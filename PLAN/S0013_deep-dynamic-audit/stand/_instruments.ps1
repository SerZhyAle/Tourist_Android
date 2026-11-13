#requires -Version 7.0
# Instrument functions of the S0013 stand (M1, M2, M3, M7 and the logcat harvest). Dot-sourced after
# _stand.ps1. Every quantity is read from outside the app; the dumpsys shapes were confirmed on the
# phone profile (API 33) on 2026-10-25 - see INDEX.md, instrument-availability matrix.

# ---------------------------------------------------------------------------------------------------------
# M1 - start round-trip and frame pacing
# ---------------------------------------------------------------------------------------------------------

# cold: process stopped first; warm: the process lives, the task is cleared so the activity is recreated;
# hot: the activity lives behind HOME.
function Measure-Start {
    param([string] $Serial, [string] $Component, [ValidateSet('cold', 'warm', 'hot')] [string] $Kind, [string] $Extras = '')
    $pkg = ($Component -split '/')[0]
    switch ($Kind) {
        'cold' { [void](Invoke-Sh -Serial $Serial -Command "am force-stop $pkg"); Start-Sleep -Milliseconds 800; $flags = '' }
        'warm' {
            if (-not (Get-AppPid -Serial $Serial -Package $pkg)) { [void](Invoke-Sh -Serial $Serial -Command "am start -W -n $Component $Extras"); Start-Sleep -Seconds 2 }
            [void](Invoke-Sh -Serial $Serial -Command 'input keyevent KEYCODE_HOME'); Start-Sleep -Milliseconds 800
            $flags = '-f 0x10008000'
        }
        'hot' {
            if (-not (Get-AppPid -Serial $Serial -Package $pkg)) { [void](Invoke-Sh -Serial $Serial -Command "am start -W -n $Component $Extras"); Start-Sleep -Seconds 2 }
            [void](Invoke-Sh -Serial $Serial -Command 'input keyevent KEYCODE_HOME'); Start-Sleep -Milliseconds 800
            $flags = ''
        }
    }
    $ts = Get-NowMs
    $o = Invoke-Sh -Serial $Serial -Command "am start -W $flags -n $Component $Extras" -TimeoutSec 60
    $state = if ($o -match 'LaunchState:\s*(\w+)') { $Matches[1].ToLowerInvariant() } else { $null }
    $total = if ($o -match 'TotalTime:\s*(\d+)') { [int]$Matches[1] } else { $null }
    $wait = if ($o -match 'WaitTime:\s*(\d+)') { [int]$Matches[1] } else { $null }
    if ($o -match 'Status:\s*timeout') { $total = $null }
    [ordered]@{ kind = $Kind; launchState = $state; totalMs = $total; waitMs = $wait; ts = $ts }
}

function Reset-Gfx([string] $Serial, [string] $Package = $script:Package) {
    [void](Invoke-Sh -Serial $Serial -Command "dumpsys gfxinfo $Package reset" -AllowFail)
}

function Read-Gfx([string] $Serial, [string] $Package = $script:Package) {
    $o = Invoke-Sh -Serial $Serial -Command "dumpsys gfxinfo $Package" -AllowFail
    $g = [ordered]@{ frames = 0; janky = 0; jankyPct = $null; p50 = $null; p90 = $null; p95 = $null; p99 = $null }
    if ($o -match 'Total frames rendered:\s*(\d+)') { $g.frames = [int]$Matches[1] }
    if ($o -match 'Janky frames:\s*(\d+)\s*\(([\d.]+)%\)') { $g.janky = [int]$Matches[1]; $g.jankyPct = [double]$Matches[2] }
    foreach ($p in 50, 90, 95, 99) { if ($o -match "(?m)^$p(th)? percentile:\s*(\d+)ms") { $g["p$p"] = [int]$Matches[2] } }
    $g
}

# ---------------------------------------------------------------------------------------------------------
# M2 - injected input to the next presented frame
#   t0 = the stand's logcat mark written immediately before `input` (cmd input since API 31: no
#   app_process start-up); the response frame is the first frame that handled input after t0, else the
#   first frame whose vsync follows t0. Latency = that frame's completion (present time when the
#   compositor reports one) - t0. Same clock: logcat -v monotonic and framestats are both CLOCK_MONOTONIC.
# ---------------------------------------------------------------------------------------------------------

function Get-MarkMonotonicMs([string] $Serial, [string] $Mark) {
    $o = (Invoke-Adb -Serial $Serial -Arguments @('logcat', '-d', '-v', 'monotonic', '-s', 'S0013:*') -AllowFail).Out
    foreach ($line in ($o -split "`n")) {
        if ($line -match "^\s*(\d+\.\d+)\s.*\b$([regex]::Escape($Mark))\s*$") {
            return [double]::Parse($Matches[1], [Globalization.CultureInfo]::InvariantCulture) * 1000
        }
    }
    $null
}

function Get-Framestats([string] $Serial, [string] $Package) {
    $o = Invoke-Sh -Serial $Serial -Command "dumpsys gfxinfo $Package framestats" -AllowFail
    $lines = $o -split "`n"
    $header = $null; $rows = [Collections.Generic.List[object]]::new()
    foreach ($l in $lines) {
        if ($l -match '^Flags,') { $header = $l.Trim().TrimEnd(',').Split(','); continue }
        if ($header -and $l -match '^\d+,') {
            $v = $l.Trim().TrimEnd(',').Split(',')
            $row = @{}
            for ($i = 0; $i -lt [math]::Min($v.Count, $header.Count); $i++) { $row[$header[$i]] = [long]$v[$i] }
            if ($row['Flags'] -eq 0) { $rows.Add($row) }
        }
        if ($l -match '^---PROFILEDATA---' -and $rows.Count -gt 0 -and $header) { }
    }
    $rows
}

function Send-MarkedInput([string] $Serial, [string] $InputCmd) {
    $mark = 'm2-' + [guid]::NewGuid().ToString('N').Substring(0, 10)
    [void](Invoke-Sh -Serial $Serial -Command "log -t S0013 $mark; $InputCmd")
    $mark
}

function Measure-InputLatency {
    param([string] $Serial, [string] $Package = $script:Package, [Parameter(Mandatory)] [string] $InputCmd, [int] $SettleMs = 900)
    $mark = Send-MarkedInput $Serial $InputCmd
    Start-Sleep -Milliseconds $SettleMs
    Resolve-InputLatency -Serial $Serial -Package $Package -Mark $mark
}

function Resolve-InputLatency {
    param([string] $Serial, [string] $Package = $script:Package, [string] $Mark)
    $mark = $Mark
    $t0 = Get-MarkMonotonicMs -Serial $Serial -Mark $mark
    if ($null -eq $t0) { return [ordered]@{ ok = $false; reason = 'mark not found in logcat' } }
    $frames = Get-Framestats -Serial $Serial -Package $Package
    $t0ns = [long]($t0 * 1e6)
    $after = @($frames | Where-Object { $_['IntendedVsync'] -ge ($t0ns - 17000000) -and $_['FrameCompleted'] -gt $t0ns } | Sort-Object { $_['IntendedVsync'] })
    if ($after.Count -eq 0) { return [ordered]@{ ok = $false; reason = 'no frame after the input'; mark = $mark } }
    $handled = @($after | Where-Object { $_['HandleInputStart'] -gt $t0ns } | Select-Object -First 1)
    $frame = if ($handled.Count -gt 0) { $handled[0] } else { $after[0] }
    # DisplayPresentTime is 0 or -1 when the compositor did not report it; anything beyond a second past
    # completion is such a sentinel, not a present time.
    $present = if ($frame.ContainsKey('DisplayPresentTime')) { $frame['DisplayPresentTime'] } else { 0 }
    $end = if ($present -gt $frame['FrameCompleted'] -and $present -lt $frame['FrameCompleted'] + 1000000000) { $present } else { $frame['FrameCompleted'] }
    $lat = ($end - $t0ns) / 1e6
    if ($lat -lt 0 -or $lat -gt 10000) { return [ordered]@{ ok = $false; reason = "unmatched frame clock ($([math]::Round($lat)) ms)"; mark = $mark } }
    [ordered]@{
        ok = $true; mark = $mark
        latencyMs = [math]::Round($lat, 1)
        matchedBy = if ($handled.Count -gt 0) { 'HandleInputStart' } else { 'next vsync' }
    }
}

# ---------------------------------------------------------------------------------------------------------
# M3 - the periodic outside harvest. One adb round-trip per sample: a device-side script prints sections
# behind @@ markers.
# ---------------------------------------------------------------------------------------------------------

# The torch reader. `dumpsys media.camera` keeps an event log of client calls (`Torch for camera id N turned on|off
# for client PID p`), newest first. When a client dies the camera service switches its torch off and logs nothing, so
# the last entry of a force-stopped app reads `on` for ever (S0019 section 4.1, S0031 item 3). The device side tags
# each entry with the client's liveness - `/proc/<pid>` present or gone - and Measure-TorchLit reads the newest entry
# per camera, calling it lit only when it is an `on` of a client that is not tagged `dead`. An entry whose PID cannot
# be read is tagged `alive`: the reader then behaves as it did before and cannot hide a lit torch.
$script:TorchProbeCmd = 'dumpsys media.camera 2>/dev/null | grep -m 8 "Torch for camera" | while read -r l; do p=${l##*PID }; p=${p%% *}; case $p in ""|*[!0-9]*) echo "$l alive";; *) if [ -d /proc/$p ]; then echo "$l alive"; else echo "$l dead"; fi;; esac; done'

# Number of cameras whose newest torch entry is an `on` of a live client, from the output of $script:TorchProbeCmd.
function Measure-TorchLit([string] $Text) {
    $seen = @{}; $lit = 0
    foreach ($l in ($Text -split "`n")) {
        if ($l -match 'Torch for camera id (\S+) turned (on|off)') {
            $cam = $Matches[1]; $state = $Matches[2]
            if (-not $seen.ContainsKey($cam)) {
                $seen[$cam] = $true
                if ($state -eq 'on' -and $l.TrimEnd() -notmatch '\sdead$') { $lit++ }
            }
        }
    }
    $lit
}

# @@notif counts distinct notifications. The range between `Notification List:` and `Snoozed notifications:` holds the
# posted list and, on API 33, the `Enqueued Notification List:` as well; an update of one key waits 200 ms in the
# enqueued list while the posted copy is still there (S0031 item 1). Each record line is cut to its identity
# (`pkg user id tag`, the key without the uid) and de-duplicated; a line the cut does not recognise stays whole, so
# a layout change can only over-count, never hide a notification.
#
# @@torch is $script:TorchProbeCmd (the torch reader above).
function Get-SampleScript([string] $Package, [int] $Uid) {
    @"
P=`$(pidof $Package | cut -d' ' -f1)
echo "@@pid `$P"
echo "@@uptime `$(cat /proc/uptime)"
if [ -n "`$P" ]; then
  echo "@@fd `$(run-as $Package ls /proc/`$P/fd 2>/dev/null | wc -l)"
  echo "@@thr `$(ls /proc/`$P/task 2>/dev/null | wc -l)"
  echo "@@stat `$(cat /proc/`$P/stat 2>/dev/null)"
  echo "@@mem"
  dumpsys meminfo `$P | grep -E '^ +TOTAL +[0-9]|Java Heap:|Native Heap:'
fi
echo "@@du `$(run-as $Package du -sk . 2>/dev/null | cut -f1) `$(run-as $Package du -sk cache 2>/dev/null | cut -f1) `$(run-as $Package du -sk code_cache 2>/dev/null | cut -f1)"
echo "@@power"
dumpsys power | grep -E 'uid=$Uid |ws=WorkSource\{$Uid'
echo "@@sensor `$(dumpsys sensorservice | grep -c '| uid $Uid |')"
echo "@@loc `$(dumpsys location | grep -cE '^ +$Uid/$Package/[0-9A-Fa-f]+ Request\[')"
echo "@@notif `$(dumpsys notification --noredact | sed -n '/Notification List:/,/Snoozed notifications:/p' | grep 'NotificationRecord(.*pkg=$Package ' | sed 's/NotificationRecord(0x[0-9a-f]*: //; s/ importance=.*//' | sort -u | wc -l)"
echo "@@svc"
dumpsys activity services $Package | grep -E 'ServiceRecord\{|isForeground=true'
echo "@@focus `$(dumpsys audio | sed -n '/Audio Focus stack entries/,/external focus policy/p' | grep -c 'pack: $Package')"
echo "@@alarm `$(dumpsys alarm | grep -cE 'Alarm\{.* $Package\}')"
echo "@@torch"
$($script:TorchProbeCmd)
echo "@@end"
"@ -replace "`r", ''
}

function ConvertFrom-Sample([string] $Text, [int] $AppPidHint = 0) {
    $s = [ordered]@{
        ts = Get-NowMs; uptimeMs = $null; pid = $null; pssKb = $null; privDirtyKb = $null; javaHeapKb = $null; nativeHeapKb = $null
        fd = $null; threads = $null; cpuTicks = $null; dataKb = $null; cacheKb = $null
        wlOwn = 0; wlAttributed = 0; wlScreen = 0; sensors = 0; locRegs = 0; notifs = 0; services = 0; fgServices = 0
        focus = 0; alarms = 0; torchOn = 0; wlTags = ''
    }
    $section = ''
    $torchLines = [Collections.Generic.List[string]]::new()
    foreach ($raw in ($Text -split "`n")) {
        $l = $raw.TrimEnd()
        if ($l -match '^@@(\w+)\s*(.*)$') {
            $section = $Matches[1]; $v = $Matches[2].Trim()
            switch ($section) {
                'pid' { if ($v -match '^\d+$') { $s.pid = [int]$v } }
                'uptime' { if ($v -match '^([\d.]+)') { $s.uptimeMs = [long]([double]::Parse($Matches[1], [Globalization.CultureInfo]::InvariantCulture) * 1000) } }
                'fd' { if ($v -match '^\d+$' -and $s.pid) { $s.fd = [int]$v } }
                'thr' { if ($v -match '^\d+$' -and $s.pid) { $s.threads = [int]$v } }
                'stat' { $f = ($v -replace '^.*\)\s*', '') -split '\s+'; if ($f.Count -gt 13) { $s.cpuTicks = [long]$f[11] + [long]$f[12] } }
                'du' { $d = $v -split '\s+'; if ($d.Count -ge 1 -and $d[0] -match '^\d+$') { $s.dataKb = [long]$d[0]; $c = 0; foreach ($x in $d[1..($d.Count - 1)]) { if ($x -match '^\d+$') { $c += [long]$x } }; $s.cacheKb = $c } }
                'sensor' { $s.sensors = [int]$v }
                'loc' { $s.locRegs = [int]$v }
                'notif' { $s.notifs = [int]$v }
                'focus' { $s.focus = [int]$v }
                'alarm' { $s.alarms = [int]$v }
            }
            continue
        }
        switch ($section) {
            'mem' {
                if ($l -match '^\s+TOTAL\s+(\d+)\s+(\d+)') { $s.pssKb = [long]$Matches[1]; $s.privDirtyKb = [long]$Matches[2] }
                elseif ($l -match 'Java Heap:\s+(\d+)') { $s.javaHeapKb = [long]$Matches[1] }
                elseif ($l -match 'Native Heap:\s+(\d+)') { $s.nativeHeapKb = [long]$Matches[1] }
            }
            'power' {
                if ($l -match "^\s+(\w+_WAKE_LOCK|PARTIAL_WAKE_LOCK)\s+'([^']*)'") {
                    $type = $Matches[1]; $tag = $Matches[2]
                    if ($l -match 'ws=WorkSource') {
                        if ($type -match 'SCREEN|FULL') { $s.wlScreen++ } else { $s.wlAttributed++ }
                    } else { $s.wlOwn++ }
                    $s.wlTags = (@($s.wlTags, "$type/$tag") | Where-Object { $_ }) -join ';'
                }
            }
            'svc' {
                if ($l -match 'ServiceRecord\{') { $s.services++ }
                elseif ($l -match 'isForeground=true') { $s.fgServices++ }
            }
            'torch' { $torchLines.Add($l) }
        }
    }
    $s.torchOn = Measure-TorchLit ($torchLines -join "`n")
    $s
}

function Get-Sample([string] $Serial, [string] $Package, [int] $Uid) {
    $script = Get-SampleScript $Package $Uid
    $r = Invoke-Adb -Serial $Serial -Arguments @('shell', $script) -TimeoutSec 45 -AllowFail
    if ($r.Out -notmatch '@@end') { throw "INVALID: sampler round-trip incomplete on $Serial (dumpsys timeout or adb drop)" }
    ConvertFrom-Sample $r.Out
}

$script:SampleColumns = 'ts', 'uptimeMs', 'pid', 'pssKb', 'privDirtyKb', 'javaHeapKb', 'nativeHeapKb', 'fd', 'threads', 'cpuTicks',
    'dataKb', 'cacheKb', 'wlOwn', 'wlAttributed', 'wlScreen', 'sensors', 'locRegs', 'notifs', 'services', 'fgServices',
    'focus', 'alarms', 'torchOn', 'wlTags'

# Runs the sampler in a thread job until -StopFile appears or -DurationSec elapses; rows go to -CsvPath.
function Start-Sampler {
    param([string] $Serial, [string] $Package, [int] $Uid, [string] $CsvPath, [double] $IntervalSec = 2, [int] $DurationSec = 86400, [string] $StopFile)
    $lib = Join-Path $script:StandRoot '_stand.ps1'
    $ins = Join-Path $script:StandRoot '_instruments.ps1'
    Start-ThreadJob -ArgumentList $lib, $ins, $Serial, $Package, $Uid, $CsvPath, $IntervalSec, $DurationSec, $StopFile -ScriptBlock {
        param($lib, $ins, $jSerial, $jPackage, $jUid, $CsvPath, $IntervalSec, $DurationSec, $StopFile)
        # The libraries set $script:Package at this scope; the job keeps its own names so nothing is clobbered.
        . $lib; . $ins
        $cols = $script:SampleColumns
        if (-not (Test-Path $CsvPath)) { ($cols -join ',') | Set-Content -LiteralPath $CsvPath -Encoding utf8NoBOM }
        $end = (Get-Date).AddSeconds($DurationSec)
        $errors = 0
        while ((Get-Date) -lt $end -and -not ($StopFile -and (Test-Path $StopFile))) {
            $tick = Get-Date
            try {
                $s = Get-Sample -Serial $jSerial -Package $jPackage -Uid $jUid
                (($cols | ForEach-Object { $v = $s[$_]; if ($null -eq $v) { '' } else { "$v" -replace ',', ' ' } }) -join ',') | Add-Content -LiteralPath $CsvPath -Encoding utf8NoBOM
            } catch {
                $errors++
                "# sample error: $($_.Exception.Message -replace "`n", ' ')" | Add-Content -LiteralPath $CsvPath -Encoding utf8NoBOM
                if ($errors -ge 3) { break }
            }
            $left = $IntervalSec - ((Get-Date) - $tick).TotalSeconds
            if ($left -gt 0) { Start-Sleep -Milliseconds ([int]($left * 1000)) }
        }
        $errors
    }
}

function Read-Samples([string] $CsvPath) {
    if (-not (Test-Path $CsvPath)) { return @() }
    $lines = @(Get-Content -LiteralPath $CsvPath | Where-Object { $_ -and -not $_.StartsWith('#') })
    if ($lines.Count -lt 2) { return @() }
    $rows = $lines | ConvertFrom-Csv
    foreach ($r in $rows) {
        $h = [ordered]@{}
        foreach ($c in $script:SampleColumns) {
            $v = $r.$c
            $h[$c] = if ($c -eq 'wlTags') { $v } elseif ($v -eq '' -or $null -eq $v) { $null } else { [double]$v }
        }
        $h
    }
}

function Get-SamplerErrors([string] $CsvPath) {
    if (-not (Test-Path $CsvPath)) { return 0 }
    @(Get-Content -LiteralPath $CsvPath | Where-Object { $_.StartsWith('# sample error') }).Count
}

# ---------------------------------------------------------------------------------------------------------
# Logcat harvest: crashes, ANRs, app lines, the secret-class scan (decision 8.8: coordinate pairs, and
# account and token patterns)
# ---------------------------------------------------------------------------------------------------------

$script:SecretPatterns = [ordered]@{
    'coordinate-pair'  = '-?\b\d{1,2}\.\d{3,}\s*[,;/ ]\s*-?\d{1,3}\.\d{3,}\b'
    'geo-uri'          = 'geo:-?\d+\.\d+,-?\d+\.\d+'
    'lat-lon-field'    = '(?i)\b(lat|latitude|lon|lng|longitude)\s*[=:]\s*-?\d{1,3}\.\d{3,}'
    'email'            = '\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b'
    'bearer-token'     = '(?i)\bbearer\s+[A-Za-z0-9._~+/-]{12,}'
    'secret-field'     = '(?i)\b(token|secret|password|passwd|api[_-]?key|auth)\s*[=:]\s*[^\s,;]{8,}'
    'long-hex'         = '\b[0-9a-fA-F]{32,}\b'
    'data-layer-node'  = '(?i)\bnode(id)?\s*[=:]\s*[0-9a-f]{6,}\b'
}

function Get-LogcatFacts {
    param([string] $Path, [int] $Uid, [string] $Package = $script:Package, [double[]] $InjectedCoords = @(), [double] $FromMonoMs = 0, [double] $ToMonoMs = [double]::MaxValue)
    $facts = [ordered]@{ appLines = 0; firstMs = $null; lastMs = $null; crashes = 0; anrs = 0; fatal = @(); secrets = @(); exceptions = @() }
    if (Test-Path $Path) {
        $inStack = $false
        foreach ($line in [IO.File]::ReadLines($Path)) {
            if ($line -notmatch '^\s*(\d+\.\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s+([VDIWEF])\s+(.*)$') { continue }
            $mono = [double]::Parse($Matches[1], [Globalization.CultureInfo]::InvariantCulture) * 1000
            if ($mono -lt $FromMonoMs -or $mono -gt $ToMonoMs) { continue }
            $lineUid = [int]$Matches[2]; $level = $Matches[5]; $rest = $Matches[6]
            if ($rest -match '^AndroidRuntime\s*:\s*FATAL EXCEPTION' -and $lineUid -eq $Uid) { $facts.fatal += $rest }
            if ($lineUid -ne $Uid) { continue }
            $facts.appLines++
            if ($null -eq $facts.firstMs) { $facts.firstMs = $mono }
            $facts.lastMs = $mono
            if ($rest -match '(?:^|\s)([\w.$]+(?:Exception|Error))(?::|\s|$)' -and $level -in 'W', 'E', 'F') {
                $facts.exceptions += "$level $($Matches[1])"
            }
            foreach ($k in $script:SecretPatterns.Keys) {
                if ($rest -match $script:SecretPatterns[$k]) { $facts.secrets += [ordered]@{ class = $k; at = $mono; match = $Matches[0].Substring(0, [math]::Min(4, $Matches[0].Length)) + '..' } }
            }
            foreach ($c in $InjectedCoords) {
                $needle = $c.ToString('0.000', [Globalization.CultureInfo]::InvariantCulture)
                if ($rest.Contains($needle)) { $facts.secrets += [ordered]@{ class = 'injected-coordinate'; at = $mono; match = 'redacted' } }
            }
        }
    }
    $ev = $Path -replace '\.txt$', '.events.txt'
    if (Test-Path $ev) {
        foreach ($line in [IO.File]::ReadLines($ev)) {
            if ($line -match '^\s*(\d+\.\d+)\s') {
                $mono = [double]::Parse($Matches[1], [Globalization.CultureInfo]::InvariantCulture) * 1000
                if ($mono -lt $FromMonoMs -or $mono -gt $ToMonoMs) { continue }
            }
            if ($line -match "am_crash:.*\b$([regex]::Escape($Package))\b") { $facts.crashes++ }
            if ($line -match "am_anr\s*:.*\b$([regex]::Escape($Package))\b") { $facts.anrs++ }
        }
    }
    $facts.exceptions = @($facts.exceptions | Group-Object | ForEach-Object { "$($_.Name) x$($_.Count)" })
    $facts
}

# ---------------------------------------------------------------------------------------------------------
# M7 - accessibility and locale facts from a UI dump
# ---------------------------------------------------------------------------------------------------------

function Get-NodeLabel($Node) {
    $t = "$($Node.text)".Trim(); $d = "$($Node.'content-desc')".Trim()
    if ($t -or $d) { return ($t, $d | Where-Object { $_ }) -join ' | ' }
    foreach ($c in $Node.SelectNodes('.//node')) {
        $t = "$($c.text)".Trim(); $d = "$($c.'content-desc')".Trim()
        if ($t -or $d) { return ($t, $d | Where-Object { $_ }) -join ' | ' }
    }
    ''
}

# The narrowest width a drawn character can take, in sp: under the narrowest legible glyph at the smallest body
# size in use, so a label at or above it is never called clipped by the width rule (S0034).
$script:MinGlyphSp = 3

# -FontScale turns on the width rule: a text node narrower than MinGlyphSp per character of its longest word
# cannot hold its text at that scale, even when its bounds sit inside its parent.
function Get-A11yFacts {
    param([xml] $Dump, [int] $DensityDpi, [int] $MinDp, [string] $Package = $script:Package, [int] $ScreenW = 0, [int] $ScreenH = 0, [double] $FontScale = 0)
    $scale = $DensityDpi / 160.0
    $facts = [ordered]@{ interactive = 0; small = @(); unlabelled = @(); clipped = @(); rawError = @(); texts = @() }
    $root = $Dump.SelectSingleNode('//node')
    if ($ScreenW -eq 0 -and $root) { $b = ConvertFrom-Bounds $root.bounds; $ScreenW = $b.R; $ScreenH = $b.B }
    foreach ($n in $Dump.SelectNodes('//node')) {
        if ($n.package -ne $Package) { continue }
        # Scrolled out of view: the probe reports it with clipped (often inverted) bounds; a user cannot
        # see or reach it until it scrolls in, and then it is judged in that dump.
        if ($n.GetAttribute('visible-to-user') -eq 'false') { continue }
        $b = ConvertFrom-Bounds $n.bounds
        $name = if ($n.'resource-id') { ($n.'resource-id' -replace '^.*:id/', '') } else { $n.class -replace '^.*\.', '' }
        foreach ($s in @("$($n.text)", "$($n.'content-desc')")) {
            if ($s.Trim()) { $facts.texts += $s.Trim() }
            if ($s -match '(Exception|Error:|\bat [a-z]+\.[\w.$]+\(|java\.|kotlin\.)') { $facts.rawError += "${name}: $($s.Substring(0, [math]::Min(60, $s.Length)))" }
        }
        if ($n.clickable -eq 'true' -or $n.'long-clickable' -eq 'true') {
            $facts.interactive++
            $wDp = ($b.R - $b.L) / $scale; $hDp = ($b.B - $b.T) / $scale
            # A target cut by its scroll viewport's edge shows its visible part only; it is judged whole
            # in the dump where it has scrolled in.
            $vp = Get-ScrollViewport $n
            $cut = $vp -and ($b.T -le $vp.T -or $b.B -ge $vp.B -or $b.L -le $vp.L -or $b.R -ge $vp.R)
            if (($wDp -lt $MinDp -or $hDp -lt $MinDp) -and -not $cut) { $facts.small += "$name $([math]::Round($wDp))x$([math]::Round($hDp))dp" }
            if (-not (Get-NodeLabel $n)) { $facts.unlabelled += $name }
        }
        if ($b -and ("$($n.text)".Trim())) {
            $parent = $n.ParentNode
            $pb = if ($parent -and $parent.bounds) { ConvertFrom-Bounds $parent.bounds } else { $null }
            $outScreen = $b.L -lt 0 -or $b.T -lt 0 -or ($ScreenW -and $b.R -gt $ScreenW) -or ($ScreenH -and $b.B -gt $ScreenH)
            $outParent = $pb -and $parent.scrollable -ne 'true' -and ($b.L -lt $pb.L - 1 -or $b.R -gt $pb.R + 1 -or $b.T -lt $pb.T - 1 -or $b.B -gt $pb.B + 1)
            $zero = ($b.R - $b.L) -le 1 -or ($b.B - $b.T) -le 1
            $narrow = $false
            if ($FontScale -gt 0) {
                $word = (@("$($n.text)".Trim() -split '\s+' | ForEach-Object { $_.Length }) | Measure-Object -Maximum).Maximum
                $vp = Get-ScrollViewport $n
                $cutX = $vp -and ($b.L -le $vp.L -or $b.R -ge $vp.R)
                $narrow = -not $cutX -and ($b.R - $b.L) -lt $word * $script:MinGlyphSp * $FontScale * $scale
            }
            $clipName = if ($n.'resource-id') { $name } else { "$($n.text)".Trim() }
            if ($zero -or $outParent -or $narrow -or ($outScreen -and -not (Test-InScrollable $n))) { $facts.clipped += $clipName }
        }
    }
    $facts
}

# A label the large-font layout dropped: a clickable node labelled in the base dump (font 1.0) whose node at the
# same index path in the large-font dump of the same locale is fully inside its scroll viewport and has no label.
# A node touching the viewport edge is scrolling, not squeezed, and is not judged. Returns the base labels.
function Get-DroppedLabels {
    param([xml] $Base, [xml] $Large, [string] $Package = $script:Package)
    $index = @{}
    foreach ($n in $Large.SelectNodes('//node')) { if ($n.package -eq $Package) { $index[(Get-IndexPath $n)] = $n } }
    $dropped = @()
    foreach ($n in $Base.SelectNodes('//node')) {
        if ($n.package -ne $Package -or ($n.clickable -ne 'true' -and $n.'long-clickable' -ne 'true')) { continue }
        $label = Get-NodeLabel $n
        if (-not $label) { continue }
        $m = $index[(Get-IndexPath $n)]
        if (-not $m -or $m.class -ne $n.class -or $m.GetAttribute('visible-to-user') -eq 'false' -or (Get-NodeLabel $m)) { continue }
        $b = ConvertFrom-Bounds $m.bounds
        if (-not $b -or ($b.R - $b.L) -le 1 -or ($b.B - $b.T) -le 1) { continue }
        $vp = Get-ScrollViewport $m
        if ($vp -and ($b.T -le $vp.T -or $b.B -ge $vp.B -or $b.L -le $vp.L -or $b.R -ge $vp.R)) { continue }
        $dropped += $label
    }
    $dropped
}

function Get-IndexPath($Node) {
    $path = @(); $p = $Node
    while ($p -and $p.LocalName -eq 'node') { $path = @("$($p.index)") + $path; $p = $p.ParentNode }
    $path -join '/'
}

function Get-ScrollViewport($Node) {
    $p = $Node.ParentNode
    while ($p -and $p.LocalName -eq 'node') { if ($p.scrollable -eq 'true') { return (ConvertFrom-Bounds $p.bounds) }; $p = $p.ParentNode }
    $null
}

function Test-InScrollable($Node) {
    $p = $Node.ParentNode
    while ($p -and $p.LocalName -eq 'node') { if ($p.scrollable -eq 'true') { return $true }; $p = $p.ParentNode }
    $false
}

# Strings that are the same in every locale by design: the product name, units, cardinal letters, digits.
$script:LocaleInvariant = '^(Tourist|SOS|GPS|N|E|S|W|NE|NW|SE|SW|km|m|mi|ft|km/h|mph|hPa|inHg|°C|°F|%|[\d\s.,:°+\-/–—]+|[\d.,]+\s*\S{0,4})$'

# English fallback: a text shown in the RU or UK dump that is identical to the EN dump's text and carries
# at least one Latin word of three letters, and is not locale-invariant by design.
function Get-FallbackStrings([string[]] $EnTexts, [string[]] $LocTexts) {
    $en = [Collections.Generic.HashSet[string]]::new([string[]]$EnTexts)
    @($LocTexts | Where-Object { $en.Contains($_) -and $_ -match '[A-Za-z]{3,}' -and $_ -notmatch $script:LocaleInvariant } | Select-Object -Unique)
}
