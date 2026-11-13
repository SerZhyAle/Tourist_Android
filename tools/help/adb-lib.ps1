#requires -Version 7.0
<#
.SYNOPSIS
    The adb side of the help tooling (S0014): shared by capture-help.ps1 and walk-help.ps1. Dot-sourced after the
    caller defines $adb, $Serial, $pkg and $tmp. Reads the screen two ways - the inflated view tree
    (dumpsys activity top, works on a ticking screen) and the accessibility tree (uiautomator, sees dialogs).
#>

function Invoke-Adb {
    # No param block on purpose: a declared parameter would swallow the dash-words (-e, -a) of adb.
    $out = & $adb -s $Serial @args 2>&1
    return ($out | Out-String)
}

function Wait-Seconds([double]$s) { Start-Sleep -Milliseconds ([int]($s * 1000)) }

$script:Failures = New-Object System.Collections.Generic.List[string]
function Add-Failure([string]$t) { $script:Failures.Add($t); Write-Host "  FAIL: $t" -ForegroundColor Red }

$script:DumpMode = 'hierarchy'

function Seed-Prefs($Seed) {
    # Demo state a fresh install does not have (the stopwatch with four participants): written into the app's own
    # shared preferences through run-as, so the app reads it the way it reads its own.
    foreach ($file in $Seed.PSObject.Properties.Name) {
        $lines = @('<?xml version="1.0" encoding="utf-8" standalone="yes" ?>', '<map>')
        foreach ($k in $Seed.$file.PSObject.Properties.Name) { $lines += ("    <int name=""{0}"" value=""{1}"" />" -f $k, $Seed.$file.$k) }
        $lines += '</map>'
        $local = Join-Path $tmp "$file.xml"
        Write-Utf8 $local (($lines -join "`n") + "`n")
        Invoke-Adb push $local "/data/local/tmp/help-$file.xml" | Out-Null
        # one quoted word for the device shell: without the single quotes the && would split the command there
        Invoke-Adb shell run-as $pkg sh -c "'mkdir -p shared_prefs && cp /data/local/tmp/help-$file.xml shared_prefs/$file.xml'" | Out-Null
    }
}

function Grant-All {
    foreach ($perm in @('ACCESS_FINE_LOCATION', 'ACCESS_COARSE_LOCATION', 'ACTIVITY_RECOGNITION', 'POST_NOTIFICATIONS', 'CAMERA')) {
        Invoke-Adb shell pm grant $pkg "android.permission.$perm" | Out-Null
    }
}
function Get-AccessibilityNodes {
    # The window in front through uiautomator: a dialog is its own window and is not in the activity's view tree.
    # It needs a screen that holds still (the dump waits for idle), so a page that uses it stops what ticks first.
    for ($try = 0; $try -lt 10; $try++) {
        $r = Invoke-Adb shell uiautomator dump /sdcard/help-ui.xml
        if ($r -match 'dumped to') {
            $local = Join-Path $tmp 'ui.xml'
            Invoke-Adb pull /sdcard/help-ui.xml $local | Out-Null
            $xml = New-Object System.Xml.XmlDocument
            $xml.Load($local)
            $nodes = @()
            foreach ($n in $xml.SelectNodes('//node')) {
                if ($n.GetAttribute('bounds') -notmatch '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') { continue }
                $id = $n.GetAttribute('resource-id')
                if (-not $id) { continue }
                $nodes += [pscustomobject]@{ Id = $id; Text = $n.GetAttribute('text'); Desc = $n.GetAttribute('content-desc'); L = [int]$Matches[1]; T = [int]$Matches[2]; R = [int]$Matches[3]; B = [int]$Matches[4] }
            }
            return $nodes
        }
        Wait-Seconds 0.6
    }
    throw 'uiautomator could not dump the window in front'
}
function Get-UiNodes {
    # The view tree of the foreground app, with absolute bounds and visibility: `dumpsys activity top` prints the
    # inflated hierarchy ("the layout inflater", rule 14), and unlike the accessibility dump it does not wait for
    # an idle screen, so it also works on a ticking stopwatch, a strobing SOS screen and a live dashboard.
    # Line: <indent>Class{hash FLAGS FLAGS l,t-r,b #id app:id/name}; children are indented under their parent.
    if ($script:DumpMode -eq 'accessibility') { return Get-AccessibilityNodes }
    $text = Invoke-Adb shell dumpsys activity top
    $nodes = @()
    $stack = New-Object System.Collections.Generic.List[object]
    foreach ($line in ($text -split "`n")) {
        if ($line -notmatch '^(\s+)[\w.$]+\{[0-9a-f]+ (\S{8,9}) \S+ (-?\d+),(-?\d+)-(-?\d+),(-?\d+)(?: #[0-9a-f]+)?(?: (\S+):id/(\S+?))?[ }]') { continue }
        $indent = $Matches[1].Length
        $visible = $Matches[2].StartsWith('V')
        $l = [int]$Matches[3]; $tp = [int]$Matches[4]; $r = [int]$Matches[5]; $b = [int]$Matches[6]
        $entry = if ($Matches[8]) { $Matches[8] } else { '' }
        while ($stack.Count -gt 0 -and $stack[$stack.Count - 1].Indent -ge $indent) { $stack.RemoveAt($stack.Count - 1) }
        $parent = if ($stack.Count -gt 0) { $stack[$stack.Count - 1] } else { $null }
        $ox = if ($parent) { $parent.X } else { 0 }
        $oy = if ($parent) { $parent.Y } else { 0 }
        $vis = $visible -and (($null -eq $parent) -or $parent.Visible)
        $node = [pscustomobject]@{ Indent = $indent; X = $ox + $l; Y = $oy + $tp; Visible = $vis }
        $stack.Add($node)
        if ($entry -and $vis) {
            $nodes += [pscustomobject]@{ Id = "${pkg}:id/$entry"; Text = ''; Desc = ''; L = $ox + $l; T = $oy + $tp; R = $ox + $r; B = $oy + $b }
        }
    }
    if ($nodes.Count -eq 0) { throw 'the view hierarchy of the foreground app could not be read' }
    return $nodes
}
function Find-ById($Nodes, [string]$Entry) {
    return $Nodes | Where-Object { $_.Id -eq "${pkg}:id/$Entry" -and ($_.R -gt $_.L) -and ($_.B -gt $_.T) } | Select-Object -First 1
}

function Find-ByText($Nodes, [string]$Text) {
    return $Nodes | Where-Object { ($_.Text -eq $Text -or $_.Desc -eq $Text) -and ($_.R -gt $_.L) } | Select-Object -First 1
}

function Invoke-Tap($Node) {
    $x = [int](($Node.L + $Node.R) / 2); $y = [int](($Node.T + $Node.B) / 2)
    Invoke-Adb shell input tap $x $y | Out-Null
}

function Invoke-Swipe([string]$Direction) {
    # 'up' moves the content up (reveals what is below); 'down' brings the top back.
    if ($Direction -eq 'up') { Invoke-Adb shell input swipe 540 1900 540 600 350 | Out-Null }
    else { Invoke-Adb shell input swipe 540 600 540 1900 350 | Out-Null }
    Wait-Seconds 0.6
}

function Invoke-Steps($Steps, $Strings) {
    # Steps: wait, key (back), scrollTop, tapId (a resource entry), tapRes (a string resource: the UI's own text in
    # the current language). A tap may scroll to find its target and may be optional.
    foreach ($step in @($Steps)) {
        if (Get-Prop $step 'wait') { Wait-Seconds ([double]$step.wait); continue }
        if ((Get-Prop $step 'key') -eq 'back') { Invoke-Adb shell input keyevent KEYCODE_BACK | Out-Null; Wait-Seconds 1; continue }
        if (Get-Prop $step 'scrollTop') { 1..4 | ForEach-Object { Invoke-Swipe 'down' }; continue }
        if (Get-Prop $step 'dump') { $script:DumpMode = $step.dump; continue }
        if (Get-Prop $step 'density') {
            # A wider layout for a moment, to reach a control the app itself squeezes to nothing on a narrow screen in
            # a long language (the stopwatch's settings gear in ru and uk). The window is recreated; an open dialog stays.
            if ($step.density -eq 'reset') { Invoke-Adb shell wm density reset | Out-Null } else { Invoke-Adb shell wm density ([string]$step.density) | Out-Null }
            Wait-Seconds 2.5
            continue
        }
        $node = $null
        for ($attempt = 0; $attempt -lt 9; $attempt++) {
            $nodes = Get-UiNodes
            if (Get-Prop $step 'tapId') { $node = Find-ById $nodes $step.tapId }
            elseif (Get-Prop $step 'tapRes') { $node = Find-ByText $nodes $Strings[$step.tapRes] }
            if ($null -ne $node -or -not (Get-Prop $step 'scroll' $false)) { break }
            Invoke-Swipe 'up'
        }
        if ($null -eq $node) {
            if (-not [bool](Get-Prop $step 'optional' $false)) { throw "step target not found: $(($step | ConvertTo-Json -Compress))" }
            continue
        }
        Invoke-Tap $node
        Wait-Seconds 0.8
    }
}

function Assert-Screen([string]$Expect) {
    # The foreground window must be the screen the page is about: a launcher or a wrong screen in a help
    # picture is a defect nobody would notice in the pixels.
    $top = Invoke-Adb shell dumpsys activity activities
    $line = ($top -split "`n" | Where-Object { $_ -match 'topResumedActivity' } | Select-Object -First 1)
    if ($line -notmatch [regex]::Escape($Expect)) { throw "expected the screen '$Expect' in front, found: $($line.Trim())" }
}
