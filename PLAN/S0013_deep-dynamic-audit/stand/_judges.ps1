#requires -Version 7.0
# Budget judges of S0013 section 8. Each judge takes the facts a scene produced and returns a cell result:
#   code 0 every judged budget held; 1 a budget was breached (a finding); 2 nothing judgeable, or the
#   signal sits inside the control jitter band (section 7 - never a finding).
# Dot-sourced after _stand.ps1 and _instruments.ps1.

function Get-Budgets { Read-JsonFile (Join-Path $script:StandRoot 'budgets.json') }

function New-Cell([string] $Scene, [string] $Device, [string] $Property, [string] $Instruments) {
    [ordered]@{
        scene = $Scene; device = $Device; property = $Property; instruments = $Instruments
        code = 2; detail = ''; findings = [Collections.Generic.List[object]]::new()
        skips = [Collections.Generic.List[string]]::new(); invalid = [Collections.Generic.List[string]]::new()
        metrics = [ordered]@{}; judged = 0
    }
}

# Section 9: P0 for signature or harm budgets declared by the caller; P1 for ANR, crash, unreleased
# resources, leaks above the band and breaches of 2x or more; P2 below 2x. The rank is R until three runs
# (T) or a soak (L) back it; P0/P1 without T or L is reported as proposed only.
function Add-Finding {
    param($Cell, [string] $Budget, [string] $Metric, $Value, $Limit, [string] $Severity, [string] $Note = '')
    if (-not $Severity) {
        $ratio = if ($Limit -and [double]$Limit -ne 0) { [double]$Value / [double]$Limit } else { [double]::PositiveInfinity }
        $Severity = if ($ratio -ge 2) { 'P1' } else { 'P2' }
    }
    $Cell.findings.Add([ordered]@{ budget = $Budget; metric = $Metric; value = $Value; limit = $Limit; severity = $Severity; rank = 'R'; note = $Note })
}

function Complete-Cell($Cell) {
    $parts = @()
    if ($Cell.findings.Count -gt 0) {
        $Cell.code = 1
        $parts += "$($Cell.findings.Count) breach(es): " + (($Cell.findings | ForEach-Object { "$($_.budget) $($_.metric)=$($_.value) limit $($_.limit) $($_.severity)" }) -join '; ')
    } elseif ($Cell.invalid.Count -gt 0 -and $Cell.judged -eq 0) {
        $Cell.code = 2
        $parts += 'invalid: ' + ($Cell.invalid -join '; ')
    } elseif ($Cell.judged -eq 0) {
        $Cell.code = 2
        $parts += 'nothing judgeable'
    } elseif ($Cell.invalid.Count -gt 0) {
        $Cell.code = 2
        $parts += "$($Cell.judged) judged; invalid: " + ($Cell.invalid -join '; ')
    } else {
        $Cell.code = 0
        $parts += "$($Cell.judged) budget check(s) held"
    }
    if ($Cell.skips.Count -gt 0) { $parts += "$($Cell.skips.Count) skipped" }
    $Cell.detail = $parts -join ', '
    $Cell
}

# A value too close to its limit to call, given the instrument's own jitter, is not a verdict.
function Test-WithinJitter([double] $Value, [double] $Limit, $Jitter) {
    if ($null -eq $Jitter) { return $false }
    [math]::Abs($Value - $Limit) -lt [double]$Jitter
}

function Get-SpanSamples($Samples, [long] $From, [long] $To) { @($Samples | Where-Object { $_.ts -ge $From -and $_.ts -le $To }) }

# A scene's soak window past its first minutes (S0021). evidence.json beside the CSV names the window, so the walk
# to the screen and the fall after it stay out of a series judged as settled.
function Get-SoakTail([string] $CsvPath, [double] $SkipMinutes = 0) {
    $ev = Read-JsonFile (Join-Path (Split-Path $CsvPath -Parent) 'evidence.json')
    if (-not ($ev -and $ev.idleWindow -and $ev.idleWindow.to)) { throw "INVALID: no soak window in the evidence beside $CsvPath" }
    Get-SpanSamples @(Read-Samples $CsvPath) ([long]$ev.idleWindow.from + [long]($SkipMinutes * 60000)) ([long]$ev.idleWindow.to)
}

# The highest reading of a column, $null when it was never read.
function Get-ColumnMax($Samples, [string] $Column) { (@($Samples | Where-Object { $null -ne $_[$Column] } | ForEach-Object { $_[$Column] }) | Measure-Object -Maximum).Maximum }

# ---------------------------------------------------------------------------------------------------------
# P1 - responsiveness
# ---------------------------------------------------------------------------------------------------------

function Invoke-JudgeP1 {
    param($Cell, $Ctx, $Logcat, $Gfx, $Control)
    $b = Get-Budgets
    $wantM1 = $Cell.instruments -match 'M1'
    $wantM2 = $Cell.instruments -match 'M2'
    if ($wantM1) {
        foreach ($s in $Ctx.Starts) {
            if ($s.launchState -eq 'boot') { $Cell.metrics["boot_$($s.label)"] = $s.totalMs; continue }   # a boot broadcast is the platform's pace, not an app start
            if ($null -eq $s.totalMs) { $Cell.skips.Add("start $($s.kind): no TotalTime"); continue }
            $id = if ($s.kind -eq 'cold') { '8.1a' } else { '8.1b' }
            $limit = $b[$id].limit
            $jit = if ($Control -and $Control.m1 -and $Control.m1[$s.kind]) { ($Control.m1[$s.kind].hi - $Control.m1[$s.kind].lo) / 2 } else { $null }
            if (Test-WithinJitter $s.totalMs $limit $jit) { $Cell.invalid.Add("$id $($s.kind) $($s.totalMs) ms within control jitter of the limit"); continue }
            $Cell.judged++
            if ($s.totalMs -gt $limit) { Add-Finding $Cell $id "$($s.kind) start $($s.label)" $s.totalMs $limit }
        }
        $ctlJank = if ($Control -and $Control.gfx) { $Control.gfx.jankyPct } else { $null }
        if ($null -ne $ctlJank -and $ctlJank -ge $b['8.1d'].limit) {
            $Cell.invalid.Add("8.1d: the profile renders the reference itself $ctlJank% janky - the floor is above the budget")
            if ($Gfx) { $Cell.metrics['jankyPct'] = $Gfx.jankyPct }
        } elseif ($Gfx -and $Gfx.frames -ge 30 -and $null -ne $Gfx.jankyPct) {
            $Cell.judged++
            $Cell.metrics['jankyPct'] = $Gfx.jankyPct; $Cell.metrics['frames'] = $Gfx.frames
            if ($Gfx.jankyPct -gt $b['8.1d'].limit) { Add-Finding $Cell '8.1d' 'janky-frame share' $Gfx.jankyPct $b['8.1d'].limit }
        } elseif ($Gfx) { $Cell.skips.Add("8.1d: $($Gfx.frames) frames in the window (needs 30)") }
    }
    if ($wantM2) {
        $lat = @($Ctx.Latencies | Where-Object { $_.ok -and [double]$_.latencyMs -ge 0 -and [double]$_.latencyMs -le 10000 } | ForEach-Object { [double]$_.latencyMs })
        $failed = @($Ctx.Latencies | Where-Object { -not $_.ok -or [double]$_.latencyMs -gt 10000 -or [double]$_.latencyMs -lt 0 })
        if ($failed.Count) { $Cell.skips.Add("$($failed.Count) injection(s) without a matched frame") }
        if ($lat.Count -gt 0) {
            $p95 = Get-Percentile $lat 95
            $Cell.metrics['inputP95Ms'] = $p95; $Cell.metrics['inputN'] = $lat.Count
            $ctlP95 = if ($Control -and $Control.m2) { $Control.m2.p95 } else { $null }
            if ($null -ne $ctlP95 -and $ctlP95 -ge $b['8.1c'].limit) { $Cell.invalid.Add("8.1c: the control injection itself takes $ctlP95 ms at p95") }
            else {
                $Cell.judged++
                if ($p95 -gt $b['8.1c'].limit) { Add-Finding $Cell '8.1c' "input->frame p95 (n=$($lat.Count))" $p95 $b['8.1c'].limit }
            }
        } elseif (-not $wantM1) { $Cell.invalid.Add('M2: no latency sample') }
    }
    if ($Logcat) {
        $Cell.judged += 2
        if ($Logcat.anrs -gt 0) { Add-Finding $Cell '8.1e' 'ANR count' $Logcat.anrs 0 'P1' }
        if ($Logcat.crashes -gt 0 -or $Logcat.fatal.Count -gt 0) { Add-Finding $Cell '8.1f' 'crash count' ([math]::Max($Logcat.crashes, $Logcat.fatal.Count)) 0 'P1' }
    }
    Complete-Cell $Cell
}

# ---------------------------------------------------------------------------------------------------------
# P2 - wrapped errors
# ---------------------------------------------------------------------------------------------------------

function Invoke-JudgeP2 {
    param($Cell, $Ctx, $Logcat)
    $b = Get-Budgets
    if (-not $Logcat) { $Cell.invalid.Add('no logcat harvest for the scene') }
    if ($Logcat) {
        $Cell.judged++
        if ($Logcat.crashes -gt 0 -or $Logcat.fatal.Count -gt 0) {
            Add-Finding $Cell '8.2a' 'uncaught failure (crash) - outside every known outcome' ([math]::Max($Logcat.crashes, $Logcat.fatal.Count)) 0 'P1' ($Logcat.fatal -join ' / ')
        }
        $Cell.judged++
        if ($Logcat.secrets.Count -gt 0) {
            $classes = ($Logcat.secrets | Group-Object class | ForEach-Object { "$($_.Name) x$($_.Count)" }) -join ', '
            Add-Finding $Cell '8.2c' "secret-class tokens in logcat ($classes)" $Logcat.secrets.Count 0 'P1'
        }
        if ($Logcat.exceptions.Count) { $Cell.metrics['loggedExceptions'] = $Logcat.exceptions }
    }
    $raw = @()
    foreach ($cp in $Ctx.Checkpoints) {
        if (-not (Test-Path $cp.path)) { continue }
        $facts = Get-A11yFacts -Dump ([xml](Get-Content $cp.path -Raw)) -DensityDpi $Ctx.Density -MinDp 0
        $raw += @($facts.rawError | ForEach-Object { "$($cp.label): $_" })
    }
    if ($Ctx.Checkpoints.Count) {
        $Cell.judged++
        if ($raw.Count) { Add-Finding $Cell '8.2b' 'raw exception text in the UI' $raw.Count 0 'P2' ($raw -join ' / ') }
    }
    if ($Cell.instruments -match 'M5' -or $Ctx.Faults.Count) {
        $faults = @($Ctx.Faults)
        if ($faults.Count -eq 0 -and $Cell.instruments -match 'M5') { $Cell.invalid.Add('M5: no fault was injected') }
        foreach ($f in $faults) {
            if ($f.invalid) { $Cell.invalid.Add("$($f.fault): $($f.invalid)"); continue }
            $Cell.judged++
            if (-not $f.inSet) { Add-Finding $Cell '8.2a' "$($f.fault) outcome '$($f.observed)' not in the known-outcome set" 1 0 ($f.severity ?? 'P1') $f.detail }
            if ($null -ne $f.consistent) {
                $Cell.judged++
                if (-not $f.consistent) { Add-Finding $Cell '8.2d' "$($f.fault) state not consistent after the fault" 1 0 ($f.severity ?? 'P1') $f.detail }
            }
        }
        if ($faults.Count -and -not $Ctx.OutcomeSetApproved) { $Cell.skips.Add('8.2a judged against the DRAFT outcome set (decision 8.7 not yet approved) - verdict does not count') }
    }
    Complete-Cell $Cell
}

# ---------------------------------------------------------------------------------------------------------
# P3 - no system blocking
# ---------------------------------------------------------------------------------------------------------

function Invoke-JudgeP3 {
    param($Cell, $Ctx, $Samples, $Logcat, $Control, [double] $IntervalMs)
    $b = Get-Budgets
    if ($Samples.Count -lt 3) { $Cell.invalid.Add("only $($Samples.Count) sample(s)"); return (Complete-Cell $Cell) }
    # The real sampling pace (a loaded host stretches it past the nominal interval) bounds every release time.
    $gaps = @(for ($i = 1; $i -lt $Samples.Count; $i++) { $Samples[$i].ts - $Samples[$i - 1].ts }) | Sort-Object
    $realInterval = if ($gaps.Count) { [double]$gaps[[int]($gaps.Count / 2)] } else { $IntervalMs }
    $grace = [long]$realInterval + 500
    $lastTs = $Samples[-1].ts
    # Signature holds: a feature that must hold a resource through its span (SOS through screen-off).
    foreach ($h in $Ctx.Holds) {
        $span = $Ctx.Spans | Where-Object { $_.feature -eq $h.feature } | Select-Object -Last 1
        if (-not $span) { continue }
        # The window stops 4 s before the exit mark: the stop input itself (located, injected, settled) lands
        # in those seconds, and a resource released by the stop is not a resource dropped during the span.
        $win = Get-SpanSamples $Samples ($h.from ?? ($span.start + $h.settleMs)) ($h.to ?? (($span.exit ?? $lastTs) - 4000))
        if ($win.Count -eq 0) { $Cell.skips.Add("hold $($h.what): no sample in its window"); continue }
        $Cell.judged++
        $miss = @($win | Where-Object { [double]($_[$h.column] ?? 0) -lt $h.min })
        if ($miss.Count) { Add-Finding $Cell 'hold' "$($h.what) dropped in $($miss.Count) of $($win.Count) samples" $miss.Count 0 $h.severity }
    }
    foreach ($span in $Ctx.Spans) {
        if (-not $span.exit) { $Cell.skips.Add("span $($span.feature) never exited"); continue }
        $after = @($Samples | Where-Object { $_.ts -gt $span.exit })
        if ($after.Count -eq 0) { $Cell.invalid.Add("no sample after $($span.feature) exited"); continue }
        $openAfter = @($Ctx.Spans | Where-Object { $_.start -gt $span.exit -or -not $_.exit -or $_.exit -gt $span.exit })
        $quiet = @($after | Where-Object { $t = $_.ts; -not ($openAfter | Where-Object { $t -ge $_.start -and ($null -eq $_.exit -or $t -le $_.exit + $grace) }) })
        # Judge this exit only up to the moment the driver starts opening the next screen (an `open:` mark)
        # or the next feature begins.
        $next = @(@($Ctx.Spans | Where-Object { $_.start -gt $span.exit } | ForEach-Object { $_.start }) + @($Ctx.Marks | Where-Object { $_.name -like 'open:*' -and $_.ts -gt $span.exit } | ForEach-Object { $_.ts }) + @($Ctx.Starts | Where-Object { $_.ts -and $_.ts -gt $span.exit } | ForEach-Object { $_.ts }) | Sort-Object)
        if ($next.Count) { $quiet = @($quiet | Where-Object { $_.ts -lt $next[0] - 300 }) }
        if ($quiet.Count -eq 0) { $Cell.skips.Add("$($span.feature): no quiet sample between its exit and the next feature"); continue }
        # 8.3a wakelock > 5 s after exit
        $late = @($quiet | Where-Object { $_.ts -gt $span.exit + $b['8.3a'].graceMs -and (($_.wlOwn ?? 0) + ($_.wlAttributed ?? 0) + ($_.wlScreen ?? 0)) -gt 0 })
        $Cell.judged++
        if ($late.Count) { Add-Finding $Cell '8.3a' "wakelock held >5 s after $($span.feature) exit ($($late[0].wlTags))" $late.Count 0 'P1' }
        # 8.3b foreground service outside the span
        $fgs = @($quiet | Where-Object { $_.ts -gt $span.exit + $grace -and ($_.fgServices ?? 0) -gt 0 })
        $Cell.judged++
        if ($fgs.Count) { Add-Finding $Cell '8.3b' "foreground service alive after $($span.feature) exit" $fgs.Count 0 'P1' }
        # 8.3c sensor/location release time
        $held = @($quiet | Where-Object { (($_.sensors ?? 0) + ($_.locRegs ?? 0)) -gt 0 })
        $firstFree = $quiet | Where-Object { (($_.sensors ?? 0) + ($_.locRegs ?? 0)) -eq 0 } | Select-Object -First 1
        $Cell.judged++
        if ($held.Count -and -not $firstFree) { Add-Finding $Cell '8.3c' "sensor/location registrations never cleared after $($span.feature) exit" ($quiet[-1].ts - $span.exit) $b['8.3c'].limit 'P1' }
        elseif ($firstFree) {
            $release = $firstFree.ts - $span.exit
            $Cell.metrics["release_$($span.feature)_ms"] = $release
            $stillHeldLater = @($quiet | Where-Object { $_.ts -gt $firstFree.ts -and (($_.sensors ?? 0) + ($_.locRegs ?? 0)) -gt 0 })
            if ($release -gt $b['8.3c'].limit + $realInterval) { Add-Finding $Cell '8.3c' "registrations cleared $release ms after $($span.feature) exit" $release $b['8.3c'].limit }
            if ($stillHeldLater.Count) { Add-Finding $Cell '8.3c' "registrations re-appeared after $($span.feature) exit with no screen open" $stillHeldLater.Count 0 'P1' }
        }
        # 8.3e audio focus after stop
        $focus = @($quiet | Where-Object { $_.ts -gt $span.exit + $grace -and ($_.focus ?? 0) -gt 0 })
        $Cell.judged++
        if ($focus.Count) { Add-Finding $Cell '8.3e' "audio focus kept after $($span.feature) stop" $focus.Count 0 }
        # torch after its feature
        $torch = @($quiet | Where-Object { $_.ts -gt $span.exit + 2000 -and ($_.torchOn ?? 0) -gt 0 })
        if ($torch.Count) { Add-Finding $Cell '8.3d' "torch still on >2 s after $($span.feature) exit" $torch.Count 0 'P1' }
    }
    foreach ($t in $Ctx.Torch) {
        if ($null -eq $t.releaseMs) { $Cell.invalid.Add("torch release of $($t.label) not observed within $($t.timeoutMs) ms poll"); continue }
        $Cell.judged++
        $Cell.metrics["torchRelease_$($t.label)"] = $t.releaseMs
        if (Test-WithinJitter $t.releaseMs $b['8.3d'].limit $t.pollMs) { $Cell.invalid.Add("8.3d $($t.label): $($t.releaseMs) ms within one poll ($($t.pollMs) ms) of the limit"); $Cell.judged--; continue }
        if ($t.releaseMs -gt $b['8.3d'].limit) { Add-Finding $Cell '8.3d' "torch released $($t.releaseMs) ms after off ($($t.label))" $t.releaseMs $b['8.3d'].limit }
    }
    # 8.3f concurrent notifications per active feature
    if ($Ctx.Spans.Count) {
        $Cell.judged++
        foreach ($s in $Samples) {
            $active = @($Ctx.Spans | Where-Object { $s.ts -ge $_.start -and ($null -eq $_.exit -or $s.ts -le $_.exit) }).Count
            $allowed = [math]::Max(1, $active) * $b['8.3f'].limit + [int]($Ctx.Persist['notifs'] ?? 0)
            if (($s.notifs ?? 0) -gt $allowed) { Add-Finding $Cell '8.3f' "notifications $($s.notifs) with $active active feature(s)" $s.notifs $allowed 'P2'; break }
        }
    }
    # 8.3g / 8.3h - idle scenes only
    if ($Ctx.IdleWindow) {
        $from = $Ctx.IdleWindow.from; $to = $Ctx.IdleWindow.to
        $minutes = ($to - $from) / 60000.0
        if ($Logcat -and $minutes -gt 0.5) {
            $lines = $Ctx.IdleLogLines
            $rate = [math]::Round($lines / $minutes, 2)
            $Cell.metrics['idleLogLinesPerMin'] = $rate
            $Cell.judged++
            if ($rate -gt $b['8.3g'].limit) { Add-Finding $Cell '8.3g' 'idle logcat lines per minute' $rate $b['8.3g'].limit }
        }
        $win = Get-SpanSamples $Samples $from $to | Where-Object { $null -ne $_.cpuTicks }
        if (@($win).Count -ge 2) {
            $cpuMsPerMin = (($win[-1].cpuTicks - $win[0].cpuTicks) * 10) / ((($win[-1].ts - $win[0].ts)) / 60000.0)
            $Cell.metrics['idleCpuMsPerMin'] = [math]::Round($cpuMsPerMin, 1)
            if (-not $Ctx.IdleBackground) { $Cell.skips.Add("8.3h: an active screen is held - the battery proxy is judged on background idle (G2, G3)") }
            elseif ($Control -and $Control.cpu) {
                $Cell.judged++
                if ($cpuMsPerMin -gt $Control.cpu.hi) { Add-Finding $Cell '8.3h' 'idle CPU ms/min above the control band (battery proxy; the emulator battery is synthetic)' ([math]::Round($cpuMsPerMin, 1)) ([math]::Round($Control.cpu.hi, 1)) 'P2' }
            } else { $Cell.invalid.Add('8.3h: no control band') }
        } else { $Cell.skips.Add('8.3h: the process was not alive through the idle window (nothing to burn)') }
    }
    Complete-Cell $Cell
}

# ---------------------------------------------------------------------------------------------------------
# P4 - leaks
# ---------------------------------------------------------------------------------------------------------

$script:RegistrationColumns = 'sensors', 'locRegs', 'notifs', 'services', 'fgServices', 'focus', 'wlOwn', 'wlAttributed', 'torchOn', 'alarms'

function Invoke-JudgeP4 {
    param($Cell, $Ctx, $Samples, $Control, $KeySetPrev, $KeySetNow)
    $b = Get-Budgets
    if ($Samples.Count -lt 2) { $Cell.invalid.Add("only $($Samples.Count) sample(s)"); return (Complete-Cell $Cell) }
    $first = $Samples[0]; $last = $Samples[-1]
    # 8.4b registrations back to baseline
    $Cell.judged++
    $diff = @()
    foreach ($c in $script:RegistrationColumns) {
        $persist = [int]($Ctx.Persist[$c] ?? 0)
        $v0 = [double]($first[$c] ?? 0); $v1 = [double]($last[$c] ?? 0)
        if ($v1 -gt $v0 + $persist) { $diff += "$c $v0->$v1" }
    }
    if ($diff.Count) { Add-Finding $Cell '8.4b' "not back to baseline after the scene: $($diff -join ', ')" $diff.Count 0 'P1' }
    # 8.4c preferences key set
    if ($null -ne $KeySetNow) {
        if ($null -eq $KeySetPrev) { $Cell.skips.Add('8.4c: first pass - key set recorded as the baseline') }
        else {
            $Cell.judged++
            $added = @($KeySetNow | Where-Object { $_ -notin $KeySetPrev })
            if ($added.Count) { Add-Finding $Cell '8.4c' "preferences keys added after the first pass: $($added -join ', ')" $added.Count 0 'P1' }
        }
    }
    # 8.4a slopes (M4) and 8.4d data-dir growth
    if ($Cell.instruments -match 'M4') {
        # A slope means something only over a steady state: the soak window when the scene has one, never a
        # scene that walks through features (its phases would read as growth). Growth across a non-soak
        # scene is 8.4b's question.
        $steady = if ($Ctx.IdleWindow -and $Ctx.IdleWindow.to) { @(Get-SpanSamples $Samples $Ctx.IdleWindow.from $Ctx.IdleWindow.to) } else { @() }
        $pid0 = $steady | Where-Object { $_.pid } | Select-Object -Last 1
        $series = @($steady | Where-Object { $pid0 -and $_.pid -eq $pid0.pid })
        $spanMin = if ($series.Count) { ($series[-1].ts - $series[0].ts) / 60000.0 } else { 0 }
        $Cell.metrics['slopeWindowMin'] = [math]::Round($spanMin, 1)
        if ($steady.Count -eq 0) { $Cell.invalid.Add('M4: the scene has no steady-state window - 8.4a is carried by the soak cells (G), growth here by 8.4b') }
        elseif ($series.Count -lt 8 -or $spanMin -lt 5) { $Cell.invalid.Add("M4: $($series.Count) samples over $([math]::Round($spanMin, 1)) min of one process - under the 5 min a slope needs") }
        else {
            $cols = 'pssKb', 'fd', 'threads'
            foreach ($col in $cols) {
                $pts = @($series | Where-Object { $null -ne $_[$col] })
                $sl = Get-SlopePctPerHour -TimesMs ([double[]]($pts | ForEach-Object { $_.ts })) -Values ([double[]]($pts | ForEach-Object { $_[$col] }))
                if (-not $sl) { continue }
                $Cell.metrics["slope_$col"] = [ordered]@{ pctPerHour = [math]::Round($sl.pctPerHour, 2); ci = @([math]::Round($sl.ciLowPct, 2), [math]::Round($sl.ciHighPct, 2)); n = $sl.n }
                $band = if ($Control -and $Control.slope -and $Control.slope[$col]) { [double]$Control.slope[$col].ciHighPct } else { $null }
                if ($null -eq $band) { $Cell.invalid.Add("8.4a ${col}: no control band"); continue }
                $limit = $band + $b['8.4a'].limit
                if ($sl.ciHighPct -le $limit) { $Cell.judged++ }
                elseif ($sl.ciLowPct -gt $limit) {
                    $Cell.judged++
                    $sev = if ($spanMin -ge 5) { 'P1' } else { 'P2' }
                    Add-Finding $Cell '8.4a' "$col slope %/h (CI $([math]::Round($sl.ciLowPct,1))..$([math]::Round($sl.ciHighPct,1)))" ([math]::Round($sl.pctPerHour, 2)) ([math]::Round($limit, 2)) $sev
                } else { $Cell.invalid.Add("8.4a ${col}: CI $([math]::Round($sl.ciLowPct,1))..$([math]::Round($sl.ciHighPct,1)) %/h straddles the limit $([math]::Round($limit,2))") }
            }
        }
        $d = @($Samples | Where-Object { $null -ne $_.dataKb })
        if ($d.Count -ge 2 -and $spanMin -ge 5) {
            $hours = ($d[-1].ts - $d[0].ts) / 3600000.0
            $growth = (($d[-1].dataKb - ($d[-1].cacheKb ?? 0)) - ($d[0].dataKb - ($d[0].cacheKb ?? 0))) / $hours
            $Cell.metrics['dataKbPerHour'] = [math]::Round($growth, 1)
            $Cell.judged++
            if ($growth -gt $b['8.4d'].limit) { Add-Finding $Cell '8.4d' 'data-dir growth KB per soak hour' ([math]::Round($growth)) $b['8.4d'].limit }
        } elseif ($d.Count -ge 2) {
            $Cell.metrics['dataKbDelta'] = ($d[-1].dataKb - ($d[-1].cacheKb ?? 0)) - ($d[0].dataKb - ($d[0].cacheKb ?? 0))
        }
    }
    Complete-Cell $Cell
}

# ---------------------------------------------------------------------------------------------------------
# P5 - understandability and accessibility (M7 results gathered by the scene's checkpoints)
# ---------------------------------------------------------------------------------------------------------

function Invoke-JudgeP5 {
    param($Cell, $Ctx)
    $b = Get-Budgets
    $sweeps = @($Ctx.M7)
    if ($sweeps.Count -eq 0) { $Cell.invalid.Add('M7: no checkpoint swept (run with -WithM7)'); return (Complete-Cell $Cell) }
    $minDp = if ($Ctx.Role -eq 'watch') { $b['8.5c'].limitWatch } else { $b['8.5c'].limit }
    foreach ($sw in $sweeps) {
        if ($sw.invalid) { $Cell.invalid.Add("$($sw.label): $($sw.invalid)"); continue }
        $Cell.judged += 4
        if ($sw.fallback.Count) { Add-Finding $Cell '8.5a' "$($sw.label): English fallback ($(($sw.fallback | Select-Object -First 6) -join ' / '))" $sw.fallback.Count 0 'P2' }
        if ($sw.small.Count) { Add-Finding $Cell '8.5c' "$($sw.label): targets under $minDp dp ($(($sw.small | Select-Object -Unique | Select-Object -First 6) -join ', '))" @($sw.small | Select-Object -Unique).Count 0 'P2' }
        if ($sw.clipped2.Count -or $sw.unlabelled2.Count) {
            Add-Finding $Cell '8.5d' "$($sw.label): at font 2.0 clipped [$(($sw.clipped2 | Select-Object -Unique | Select-Object -First 5) -join ', ')] unlabelled [$(($sw.unlabelled2 | Select-Object -Unique) -join ', ')]" (@($sw.clipped2 | Select-Object -Unique).Count + @($sw.unlabelled2 | Select-Object -Unique).Count) 0 'P2'
        }
        if ($sw.unlabelled.Count -or $sw.stateless.Count) {
            Add-Finding $Cell '8.5e' "$($sw.label): interactive nodes without label [$(($sw.unlabelled | Select-Object -Unique) -join ', ')] or state [$(($sw.stateless | Select-Object -Unique) -join ', ')]" (@($sw.unlabelled | Select-Object -Unique).Count + @($sw.stateless | Select-Object -Unique).Count) 0 'P2'
        }
        if ($sw.dpad) { $Cell.metrics["dpad_$($sw.label)"] = $sw.dpad }
    }
    foreach ($t in $Ctx.Toggles) {
        $Cell.judged++
        if ($t.before -ne $t.after) { Add-Finding $Cell '8.5b' "toggle $($t.name) on->off does not return the state ($($t.diff))" 1 0 'P2' }
    }
    Complete-Cell $Cell
}
