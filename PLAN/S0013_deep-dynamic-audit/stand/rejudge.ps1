#requires -Version 7.0
<#
.SYNOPSIS
    Re-applies the current budget judges to scenes already run: rebuilds each scene's facts from its
    evidence.json, samples.csv and logcat harvest, judges every cell again, and writes
    cells.rejudged.jsonl beside the run's cells.jsonl. Nothing touches a device.

.DESCRIPTION
    For a judge that was corrected after a run (a stand defect, never a re-cut budget - that is the owner's),
    so the evidence already taken is not thrown away. Older evidence without the idle window is
    reconstructed from the marks: G1 from its feature span, G2 from the `background` mark to scene end.

    Exit code: 0 every re-judged cell passed; 1 at least one failed; 2 no evidence readable, or no cell
    failed and at least one could not be verified.
#>
[CmdletBinding(PositionalBinding = $false)]
param([Parameter(Mandatory)] [string[]] $RunIds)

foreach ($n in 'RunIds') { $v = Get-Variable -Name $n -ValueOnly; Set-Variable -Name $n -Value @($v | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim() } | Where-Object { $_ }) }
. (Join-Path $PSScriptRoot '_stand.ps1')
. (Join-Path $PSScriptRoot '_instruments.ps1')
. (Join-Path $PSScriptRoot '_judges.ps1')
. (Join-Path $PSScriptRoot 'scenes.ps1')
. (Join-Path $PSScriptRoot '_engine.ps1')
trap { Write-Host "rejudge: $($_.Exception.Message)" -ForegroundColor Red; Exit-Verdict -Subject 'S0013 rejudge' -Code 2 -Detail ($_.Exception.Message -replace '^INVALID:\s*', '') }

function ConvertTo-List($a) { $l = [Collections.Generic.List[object]]::new(); foreach ($x in @($a)) { if ($null -ne $x) { $l.Add($x) } }; , $l }

$matrix = Read-Matrix
$all = [Collections.Generic.List[object]]::new()
$uidCache = @{}
foreach ($rid in $RunIds) {
    $runDir = Join-Path $script:TempRoot "runs\$rid"
    $out = [Collections.Generic.List[string]]::new()
    foreach ($ev in Get-ChildItem (Join-Path $runDir 'scenes') -Recurse -Filter evidence.json -ErrorAction SilentlyContinue) {
        $e = Get-Content $ev.FullName -Raw | ConvertFrom-Json -AsHashtable
        $dir = $ev.DirectoryName
        $sid = ($e.scene -split '-')[0]
        $log = Join-Path $dir 'logcat.txt'
        $uid = $e.uid
        if (-not $uid -and (Test-Path ($log -replace '\.txt$', '.events.txt'))) {
            $m = Select-String -Path ($log -replace '\.txt$', '.events.txt') -Pattern 'am_proc_start: \[\d+,\d+,(\d+),com\.sza\.tourist,' | Select-Object -First 1
            if ($m) { $uid = [int]$m.Matches[0].Groups[1].Value }
        }
        # One install per run and role: a scene whose process never restarted borrows the uid another scene logged.
        if ($uid) { $uidCache["$rid/$($e.role)/$($e.recipe)"] = $uid } else { $uid = $uidCache["$rid/$($e.role)/$($e.recipe)"] }
        $samples = @(Read-Samples (Join-Path $dir 'samples.csv'))
        $ctx = @{
            SceneId = $sid; SubId = $e.scene; Role = $e.role; Density = ($e.density ?? 420)
            Marks = ConvertTo-List $e.marks; Spans = ConvertTo-List $e.spans; Starts = ConvertTo-List $e.starts; Latencies = ConvertTo-List $e.latencies
            Torch = ConvertTo-List $e.torch; Holds = ConvertTo-List $e.holds; Faults = ConvertTo-List $e.faults; Toggles = ConvertTo-List $e.toggles
            M7 = ConvertTo-List $e.m7; Stateless = ConvertTo-List $e.stateless; Persist = ($e.persist ?? @{})
            IdleWindow = $e.idleWindow; IdleBackground = [bool]$e.idleBackground; IdleLogLines = [int]($e.idleLogLines ?? 0)
            OutcomeSetApproved = [bool](Get-OutcomeSet).approved
        }
        $ctx.Checkpoints = if ($e.checkpoints) { ConvertTo-List $e.checkpoints } else { ConvertTo-List @(Get-ChildItem $dir -Filter 'ui-*.xml' | ForEach-Object { [ordered]@{ label = $_.BaseName; path = $_.FullName } }) }
        if (-not $ctx.IdleWindow -and $sid -eq 'G1' -and $ctx.Spans.Count) { $s = $ctx.Spans[0]; if ($s.exit) { $ctx.IdleWindow = @{ from = $s.start; to = $s.exit - 3000 } } }
        if (-not $ctx.IdleWindow -and $sid -eq 'G2') {
            $bg = $ctx.Marks | Where-Object { $_.name -eq 'background' } | Select-Object -First 1
            $end = $ctx.Marks | Where-Object { $_.name -eq 'scene-end' } | Select-Object -First 1
            if ($bg -and $end) { $ctx.IdleWindow = @{ from = $bg.ts; to = $end.ts - 4000 }; $ctx.IdleBackground = $true }
        }
        $logcat = if ($uid -and (Test-Path $log)) { Get-LogcatFacts -Path $log -Uid $uid -InjectedCoords @($script:BaseFix[1], $script:BaseFix[0]) } else { $null }
        $recipe = $e.recipe
        $control = Get-ControlFor $recipe
        foreach ($old in $e.cells) {
            $cell = New-Cell $e.scene $e.role $old.property $old.instruments
            if ($e.invalid) { $cell.invalid.Add($e.invalid); [void](Complete-Cell $cell) }
            elseif ($e.samplerErrors -ge 3 -and $old.property -in 'P3', 'P4') { $cell.invalid.Add("sampler failed $($e.samplerErrors) times"); [void](Complete-Cell $cell) }
            else {
                switch ($old.property) {
                    'P1' { [void](Invoke-JudgeP1 -Cell $cell -Ctx $ctx -Logcat $logcat -Gfx $e.gfx -Control $control) }
                    'P2' { [void](Invoke-JudgeP2 -Cell $cell -Ctx $ctx -Logcat $logcat) }
                    'P3' { [void](Invoke-JudgeP3 -Cell $cell -Ctx $ctx -Samples $samples -Logcat $logcat -Control $control -IntervalMs 2000) }
                    'P4' { [void](Invoke-JudgeP4 -Cell $cell -Ctx $ctx -Samples $samples -Control $control -KeySetPrev $null -KeySetNow $null) }
                    'P5' { [void](Invoke-JudgeP5 -Cell $cell -Ctx $ctx) }
                }
            }
            $all.Add($cell); $out.Add(($cell | ConvertTo-Json -Depth 8 -Compress))
            Write-Output (Format-CellLine $cell)
        }
    }
    foreach ($line in Get-Content (Join-Path $runDir 'cells.jsonl') -ErrorAction SilentlyContinue) {
        $c = $line | ConvertFrom-Json -AsHashtable
        if ($c.device -eq 'pair') { $out.Add($line); $all.Add($c) }
    }
    $out | Set-Content -LiteralPath (Join-Path $runDir 'cells.rejudged.jsonl') -Encoding utf8NoBOM
}
if ($all.Count -eq 0) { throw 'INVALID: no evidence in the given runs' }
$code = Merge-Codes @($all | ForEach-Object { [int]$_.code })
$failed = @($all | Where-Object { $_.code -eq 1 }).Count; $cnv = @($all | Where-Object { $_.code -eq 2 }).Count
Exit-Verdict -Subject 'S0013 rejudge' -Code $code -Detail "$($all.Count) cell(s): $($all.Count - $failed - $cnv) passed, $failed failed, $cnv could not verify"
