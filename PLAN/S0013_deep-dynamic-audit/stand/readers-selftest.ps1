#requires -Version 7.0
<#
.SYNOPSIS
    Fixture check of the stand's readers and judges (S0031): no device, no adb.

.DESCRIPTION
    Each reader that misjudged the app on the 2026-10-25 runs is exercised on a fixture of the input it
    misread, so the fix is proven without a profile and a regression is caught between releases:
      1. the notification count (`@@notif` of the sample script): the posted list and the enqueue copy of one
         key count once; two real ids count twice; a record past `Snoozed notifications:` is not counted;
         the pre-S0031 reader, run on the same fixture, counts the copy twice (the fixture reproduces the defect);
      2. the torch reader: an `on` entry of a dead client is not a lit torch, a live or unreadable one is, the
         newest entry per camera decides; the device-side liveness tag is produced by the real
         $script:TorchProbeCmd run under bash on a live and on a dead PID;
      3. the F8-watch judge: a stopwatch reading is parsed from the screen text, and a resumed, a dropped and
         a stale-notification case are told apart;
      4. the draft outcome set lists what the F8-watch judge can answer without a finding;
      5. the D1 driver leaves a dark-torch gap before the water flashlight opens inside its span, and the F8
         watch driver measures the cold start before it opens the stopwatch again;
      6. the M7 clipping reader (S0034): a label narrower than one glyph per character of its longest word is
         clipped, and so is a label the font-2.0 layout dropped from a chip that is fully on screen; a chip at the
         edge of its scrolling list is not judged;
      7. the soak tail and the descriptor ceiling (S0021): a tail drops the fill and the fall after the screen, a
         settled tail is flat where the whole recorded window is not, a leaking tail is not flat, a plateau is told
         from its ceiling, the listing sits between the soak and leaving the lamp.
    Checks 1 and 2 need a POSIX shell on the host (`bash`); without one they cannot run. The device shell
    (toybox sh) is not the host's: the same pipelines run on a device in the release-tier stand runs.

    Exit code: 0 every fixture read as expected; 1 at least one reader or judge misjudged a fixture; 2 the
    check could not run (no bash, a library unreadable).
#>
[CmdletBinding(PositionalBinding = $false)]
param()

. (Join-Path $PSScriptRoot '_stand.ps1')
. (Join-Path $PSScriptRoot '_instruments.ps1')
. (Join-Path $PSScriptRoot '_judges.ps1')
. (Join-Path $PSScriptRoot 'scenes.ps1')
. (Join-Path $PSScriptRoot '_engine.ps1')
trap { Write-Host "readers-selftest: $($_.Exception.Message)" -ForegroundColor Red; Exit-Verdict -Subject 'S0031 readers' -Code 2 -Detail ($_.Exception.Message -replace '^INVALID:\s*', '') }

$bash = (Get-Command bash -ErrorAction SilentlyContinue | Select-Object -First 1).Source
if (-not $bash) { throw 'INVALID: no bash on the host; the shell pipelines cannot be exercised' }
$work = Join-Path $script:TempRoot 'readers-selftest'
New-Item -ItemType Directory -Path $work -Force | Out-Null

$results = [Collections.Generic.List[object]]::new()
function Check([string] $Name, [scriptblock] $Test) {
    try { $ok = [bool](& $Test); $why = '' } catch { $ok = $false; $why = $_.Exception.Message }
    $results.Add([pscustomobject]@{ name = $Name; ok = $ok; why = $why })
}

# Runs a POSIX script text in $work and returns its stdout lines.
function Invoke-Posix([string] $Name, [string] $Text) {
    [IO.File]::WriteAllText((Join-Path $work $Name), ($Text -replace "`r", ''))
    Push-Location $work
    try { @(& $bash $Name 2>&1 | ForEach-Object { "$_" }) } finally { Pop-Location }
}

# ---------------------------------------------------------------------------------------------------------
# 1. Notification count
# ---------------------------------------------------------------------------------------------------------

$pkg = 'com.sza.tourist'
function New-Record([string] $Hash, [string] $Package, [int] $Id, [string] $Text) {
    "    NotificationRecord(0x${Hash}: pkg=$Package user=UserHandle{0} id=$Id tag=null importance=3 key=0|$Package|$Id|null|10093: Notification(channel=c shortcut=null contentView=null text=$Text vis=PRIVATE))`n      uid=10093 userId=0`n      opPkg=$Package"
}
function New-Dump([string[]] $Posted, [string[]] $Enqueued, [string[]] $AfterSnoozed) {
    (@('Current Notification Manager state:', '  Notification List:') + $Posted + @('  ', '  Enqueued Notification List:') + $Enqueued + @('', '  Snoozed notifications:', '    (none)', '  Ranking Config:') + $AfterSnoozed) -join "`n"
}
$sosOld = New-Record '0a1b2c3d' $pkg 4201 'Sound and light'
$sosNew = New-Record '0c3d4e5f' $pkg 4201 'Sound only'
$other = New-Record '0b2c3d4e' 'com.android.systemui' 7 'x'
$second = New-Record '0d4e5f60' $pkg 4202 'Stopwatch'

$notifLine = @((Get-SampleScript $pkg 10093) -split "`n" | Where-Object { $_ -like 'echo "@@notif *' })
$oldLine = "echo `"@@notif `$(dumpsys notification --noredact | sed -n '/Notification List:/,/Snoozed notifications:/p' | grep -c 'NotificationRecord(.*pkg=$pkg ')`""
Check 'sample script carries exactly one @@notif line' { $notifLine.Count -eq 1 }
# One shell run for every case: the reader's own line, with `dumpsys notification --noredact` replaced by the fixture.
$notifCases = [ordered]@{
    'copy'       = @{ line = $notifLine[0]; dump = New-Dump @($sosOld, $other) @($sosNew) @() }
    'copy-old'   = @{ line = $oldLine; dump = New-Dump @($sosOld, $other) @($sosNew) @() }
    'two-ids'    = @{ line = $notifLine[0]; dump = New-Dump @($sosOld, $second) @() @() }
    'enqueued-2' = @{ line = $notifLine[0]; dump = New-Dump @($sosOld) @($sosNew, $second) @() }
    'none'       = @{ line = $notifLine[0]; dump = New-Dump @($other) @() @() }
    'snoozed'    = @{ line = $notifLine[0]; dump = New-Dump @($other) @() @($sosOld) }
}
$notifCounts = @{}
if ($notifLine.Count -eq 1) {
    $sh = foreach ($k in $notifCases.Keys) {
        [IO.File]::WriteAllText((Join-Path $work "dump-$k.txt"), ($notifCases[$k].dump -replace "`r", ''))
        "echo `"@@case $k`""
        $notifCases[$k].line -replace 'dumpsys notification --noredact', "cat dump-$k.txt"
    }
    $case = ''
    foreach ($l in (Invoke-Posix 'notif.sh' ($sh -join "`n"))) {
        if ($l -match '^@@case (\S+)') { $case = $Matches[1] }
        elseif ($l -match '^@@notif ') { $notifCounts[$case] = (ConvertFrom-Sample "$l`n@@end").notifs }
    }
}
Check 'posted + enqueue copy of one key counts once' { $notifCounts['copy'] -eq 1 }
Check 'the pre-S0031 reader counts that fixture twice (the fixture reproduces the defect)' { $notifCounts['copy-old'] -eq 2 }
Check 'two ids of the app count twice' { $notifCounts['two-ids'] -eq 2 }
Check 'a second id only in the enqueued list counts' { $notifCounts['enqueued-2'] -eq 2 }
Check 'no record of the app counts zero' { $notifCounts['none'] -eq 0 }
Check 'a record past Snoozed notifications is not counted' { $notifCounts['snoozed'] -eq 0 }

# ---------------------------------------------------------------------------------------------------------
# 2. Torch reader
# ---------------------------------------------------------------------------------------------------------

function Line([string] $Cam, [string] $State, [string] $Tag = '') { "Torch for camera id $Cam turned $State for client PID 6913$(if ($Tag) { " $Tag" })" }
Check 'an on entry of a dead client is not lit' { (Measure-TorchLit (Line 1 on dead)) -eq 0 }
Check 'an on entry of a live client is lit' { (Measure-TorchLit (Line 1 on alive)) -eq 1 }
Check 'an untagged on entry is lit (the old behaviour)' { (Measure-TorchLit (Line 1 on)) -eq 1 }
Check 'the newest entry per camera decides (off over on)' { (Measure-TorchLit ((Line 1 off alive), (Line 1 on alive) -join "`n")) -eq 0 }
Check 'the newest entry per camera decides (dead on over live on)' { (Measure-TorchLit ((Line 1 on dead), (Line 1 on alive) -join "`n")) -eq 0 }
Check 'two cameras count separately' { (Measure-TorchLit ((Line 0 on alive), (Line 1 off alive) -join "`n")) -eq 1 }
Check 'ConvertFrom-Sample reads torchOn from the tagged section' {
    $dead = ConvertFrom-Sample ("@@pid`n@@torch`n$(Line 1 on dead)`n@@end")
    $live = ConvertFrom-Sample ("@@pid`n@@torch`n$(Line 1 on alive)`n@@end")
    $dead.torchOn -eq 0 -and $live.torchOn -eq 1 -and -not $dead.Contains('torch_1')
}
Check 'TorchProbeCmd tags a live PID alive, a dead PID dead, an unreadable one alive' {
    $script = "dumpsys() { printf 'Torch for camera id 1 turned on for client PID %s\nTorch for camera id 0 turned on for client PID 999999\nTorch for camera id 2 turned on for client PID\n' `"`$`$`"; }`n$script:TorchProbeCmd`n"
    $out = Invoke-Posix 'torch.sh' $script
    $out.Count -eq 3 -and $out[0] -match ' alive$' -and $out[1] -match ' dead$' -and $out[2] -match ' alive$' -and (Measure-TorchLit ($out -join "`n")) -eq 2
}

# ---------------------------------------------------------------------------------------------------------
# 3. F8-watch judge
# ---------------------------------------------------------------------------------------------------------

Check 'stopwatch text M:SS.hh' { (ConvertFrom-StopwatchText '0:16.66') -eq 16660 -and (ConvertFrom-StopwatchText '12:07.05') -eq 727050 }
Check 'stopwatch text H:MM:SS.hh' { (ConvertFrom-StopwatchText '1:02:03.45') -eq 3723450 }
Check 'a clock, a label and an empty text are not readings' { $null -eq (ConvertFrom-StopwatchText '10:32') -and $null -eq (ConvertFrom-StopwatchText 'Laps: 8') -and $null -eq (ConvertFrom-StopwatchText '') }
Check 'a kept, still-running measurement is resumed' { (Get-StopwatchKillOutcome 7000 17000 1) -eq 'stopwatch-resumed' -and (Get-StopwatchKillOutcome 7000 7000 0) -eq 'stopwatch-resumed' }
Check 'a restarted measurement with a notification left is stale' { (Get-StopwatchKillOutcome 7000 3000 1) -eq 'stale-notification-left' }
Check 'a restarted measurement with no notification ended cleanly' { (Get-StopwatchKillOutcome 7000 3000 0) -eq 'ended-cleanly' }
Check 'a missing reading is no outcome' { $null -eq (Get-StopwatchKillOutcome $null 1000 0) -and $null -eq (Get-StopwatchKillOutcome 7000 $null 0) }

# ---------------------------------------------------------------------------------------------------------
# 4. Draft outcome set
# ---------------------------------------------------------------------------------------------------------

Check 'F8-watch lists resumed and ended-cleanly, not the stale finding' {
    $o = @((Get-OutcomeSet).outcomes.'F8-watch')
    ('stopwatch-resumed' -in $o) -and ('ended-cleanly' -in $o) -and ('stale-notification-left' -notin $o)
}

# ---------------------------------------------------------------------------------------------------------
# 5. Driver shape
# ---------------------------------------------------------------------------------------------------------

# The driver's commands, one per line, comments and blank lines dropped.
function Get-DriverLines([string] $Scene, [string] $Device) {
    [string[]]@($script:Drivers[$Scene][$Device].ToString() -split "`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ -and $_ -notlike '#*' })
}
function Find-Line([string[]] $Lines, [string] $Like, [switch] $Last) {
    $order = if ($Last) { ($Lines.Count - 1)..0 } else { 0..($Lines.Count - 1) }
    foreach ($i in $order) { if ($Lines[$i] -like $Like) { return $i } }
    -1
}
Check 'D1: a dark-torch gap, then water-storm begins, then the water flashlight opens' {
    $l = Get-DriverLines 'D1' 'phone'
    $i = Find-Line $l 'End-Feature $ctx torch-storm'
    # the quiet check needs a sample in (exit + 2 s, next span - 0.3 s); the sampler runs every 2 s
    $i -ge 0 -and $l[$i + 1] -match '^Pause-Sec ([\d.]+)$' -and [double]$Matches[1] -gt 4.3 -and $l[$i + 2] -eq 'Begin-Feature $ctx water-storm' -and $l[$i + 3] -like '$w = Tap $ctx -Id row_water_flashlight*'
}
Check 'F8 watch: the cold start is measured before the stopwatch is opened again' {
    $l = Get-DriverLines 'F8' 'watch'
    $start = Find-Line $l '$r = Measure-Start*-Kind cold*'
    $reopen = Find-Line $l 'Open-Watch $ctx STOPWATCH' -Last
    $start -ge 0 -and $reopen -gt $start -and @($l | Where-Object { $_ -like '*Get-StopwatchMs $ctx*' }).Count -eq 2
}

# ---------------------------------------------------------------------------------------------------------
# 6. M7 clipping reader (S0034)
# ---------------------------------------------------------------------------------------------------------

# A watch dump (density 320): a scrolling list holding a top chip and two reset chips, shaped as the
# 2026-10-25 C2 watch dumps. Each chip is "bounds|label bounds|label", an empty label bounds drops the text.
function New-ChipDump([string[]] $Chips) {
    $a = "package=`"$script:Package`" visible-to-user=`"true`""
    $i = 0
    $nodes = foreach ($c in $Chips) {
        $cb, $tb, $t = $c -split '\|'
        $label = if ($tb) { "<node index=`"0`" text=`"$t`" class=`"android.widget.TextView`" clickable=`"false`" $a bounds=`"$tb`" />" }
        "<node index=`"$i`" text=`"`" class=`"android.view.View`" clickable=`"true`" $a bounds=`"$cb`">$label</node>"
        $i++
    }
    [xml]"<hierarchy><node index=`"0`" text=`"`" class=`"android.widget.FrameLayout`" clickable=`"false`" $a bounds=`"[0,0][384,384]`"><node index=`"0`" text=`"`" class=`"android.view.View`" clickable=`"false`" scrollable=`"true`" $a bounds=`"[0,0][384,384]`">$($nodes -join '')</node></node></hierarchy>"
}
$base = New-ChipDump '[52,0][332,192]|[159,15][227,42]|Speed', '[46,207][182,298]|[69,241][159,264]|Reset Trip', '[193,207][338,298]|[216,228][315,277]|Reset Steps'
$narrow = New-ChipDump '[46,0][338,276]||', '[68,290][252,368]|[87,310][236,348]|Reset Trip', '[252,290][330,368]|[285,303][297,355]|Reset Steps'
$dropped = New-ChipDump '[46,0][338,276]||', '[69,295][264,372]|[88,315][261,353]|Reset Trip', '[264,295][341,372]||'
Check 'a label narrower than 3 sp per character of its longest word is clipped, a wide one is not' {
    $c = @((Get-A11yFacts -Dump $narrow -DensityDpi 320 -MinDp 40 -FontScale 2.0).clipped)
    $c.Count -eq 1 -and $c[0] -eq 'Reset Steps'
}
Check 'without a font scale the width rule is off (the pre-S0034 reader misses the narrow label)' { @((Get-A11yFacts -Dump $narrow -DensityDpi 320 -MinDp 40).clipped).Count -eq 0 }
Check 'a label the large font dropped from a whole chip is clipped, a chip scrolled to the edge is not' {
    $d = @(Get-DroppedLabels -Base $base -Large $dropped)
    $d.Count -eq 1 -and $d[0] -eq 'Reset Steps'
}
Check 'a label still present at the large font is not reported dropped' { @(Get-DroppedLabels -Base $base -Large $narrow).Count -eq 0 }

# ---------------------------------------------------------------------------------------------------------
# 7. Soak tail and descriptor ceiling (S0021)
# ---------------------------------------------------------------------------------------------------------

# A 10-minute soak sampled every 10 s, shaped as the 2026-10-25 water series: 93 before the lamp, a fill from 106 to
# 126 over 8 minutes, then either settled or still rising, and 93 again after the screen is left.
function New-SoakFixture([string] $Name, [int] $Rise, [int] $Plateau = 126) {
    $dir = Join-Path $work $Name
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
    $t0 = 1800000000000
    $rows = for ($s = -60; $s -le 660; $s += 10) {
        $fd = if ($s -lt 0 -or $s -gt 600) { 93 } elseif ($s -lt 480) { 106 + [int][math]::Floor($s / 24) } else { $Plateau + [int][math]::Floor($Rise * ($s - 480) / 60) }
        "$($t0 + $s * 1000),7,$fd"
    }
    @('ts,pid,fd') + $rows | Set-Content -LiteralPath (Join-Path $dir 'samples.csv') -Encoding utf8NoBOM
    Write-JsonFile (Join-Path $dir 'evidence.json') ([ordered]@{ idleWindow = [ordered]@{ from = $t0; to = $t0 + 600000 } })
    Join-Path $dir 'samples.csv'
}
function Get-FdSlope($Samples) { Get-SlopePctPerHour -TimesMs ([double[]]($Samples | ForEach-Object { $_.ts })) -Values ([double[]]($Samples | ForEach-Object { $_.fd })) -Boot 40 }
$settled = New-SoakFixture 'soak-settled' 0
$leaking = New-SoakFixture 'soak-leaking' 4
$drawing = New-SoakFixture 'soak-drawing' 0 127
$fdLimit = (Get-Budgets)['8.4a'].limit
$settledTail = @(Get-SoakTail $settled 8.5)
$leakingTail = @(Get-SoakTail $leaking 8.5)
Check 'a soak tail holds the settled window only: no fill, no fall after the screen' {
    $settledTail.Count -eq 10 -and (Get-ColumnMax $settledTail 'fd') -eq 126 -and @($settledTail | Where-Object { $_.fd -ne 126 }).Count -eq 0
}
Check 'a settled tail is flat; the whole soak window of the same series reads as growth (the recorded defect)' {
    (Get-FdSlope $settledTail).ciHighPct -le $fdLimit -and (Get-FdSlope @(Get-SoakTail $settled)).ciLowPct -gt $fdLimit
}
Check 'a tail still rising past the fill is not flat (the guard can fail)' { (Get-FdSlope $leakingTail).ciLowPct -gt $fdLimit }
Check 'a plateau is told from a ceiling: under it passes, above it does not' {
    $p = Get-ColumnMax $settledTail 'fd'; $c = Get-ColumnMax @(Get-SoakTail $drawing) 'fd'; $l = Get-ColumnMax $leakingTail 'fd'
    $p -eq 126 -and $c -eq 127 -and $p -le $c -and $l -gt $c
}
Check 'a CSV with no evidence beside it has no soak window' {
    $bare = Join-Path $work 'soak-bare'; New-Item -ItemType Directory -Path $bare -Force | Out-Null
    Copy-Item -LiteralPath $settled -Destination (Join-Path $bare 'samples.csv') -Force
    try { [void](Get-SoakTail (Join-Path $bare 'samples.csv') 8.5); $false } catch { $_.Exception.Message -like 'INVALID: no soak window*' }
}
Check 'G1 water: the descriptor listing is taken after the soak, before the lamp is left' {
    $l = Get-DriverLines 'G1' 'phone'
    $i = Find-Line $l 'Invoke-Soak $ctx water $min'
    $i -ge 0 -and $l[$i + 1] -eq 'Save-FdListing $ctx' -and $l[$i + 2] -like 'Key $ctx KEYCODE_VOLUME_DOWN*'
}

$failed =@($results | Where-Object { -not $_.ok })
foreach ($r in $results) { Write-Output ("  {0} {1}{2}" -f $(if ($r.ok) { 'ok  ' } else { 'FAIL' }), $r.name, $(if ($r.why) { " - $($r.why)" } else { '' })) }
if ($failed.Count) { Exit-Verdict -Subject 'S0031 readers' -Code 1 -Detail "$($failed.Count) of $($results.Count) fixtures misjudged" }
Exit-Verdict -Subject 'S0031 readers' -Code 0 -Detail "$($results.Count) fixtures read as expected"
