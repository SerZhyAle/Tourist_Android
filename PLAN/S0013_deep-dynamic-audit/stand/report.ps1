#requires -Version 7.0
<#
.SYNOPSIS
    Folds the cells of one or more run-matrix runs into the matrix of S0013 section 3.2 and a findings list,
    as Markdown for the ticket. Reads temp/S0013/runs/<run-id>/cells.jsonl and the scenes' evidence.json.

.DESCRIPTION
    Per cell: the merged verdict of every device and sub-scene (CHECK-VERDICT 2.14 precedence), written as
    `measured <WORD> (<run ids>)`. A later run of the same cell replaces an earlier one only for the
    properties it measured. Findings are listed with budget, value, limit, proposed severity and rank:
    R for one run, T when the same cell failed the same budget in three runs.

    Exit code: 0 the report was written; 1 never; 2 no readable run.
#>
[CmdletBinding(PositionalBinding = $false)]
param([Parameter(Mandatory)] [string[]] $RunIds, [string] $Out, [switch] $UpdateTicket)

foreach ($n in 'RunIds') { $v = Get-Variable -Name $n -ValueOnly; Set-Variable -Name $n -Value @($v | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim() } | Where-Object { $_ }) }
. (Join-Path $PSScriptRoot '_stand.ps1')
. (Join-Path $PSScriptRoot '_instruments.ps1')
. (Join-Path $PSScriptRoot '_judges.ps1')
. (Join-Path $PSScriptRoot 'scenes.ps1')
. (Join-Path $PSScriptRoot '_engine.ps1')
trap { Write-Host "report: $($_.Exception.Message)" -ForegroundColor Red; Exit-Verdict -Subject 'S0013 report' -Code 2 -Detail ($_.Exception.Message -replace '^INVALID:\s*', '') }

$matrix = Read-Matrix
$cells = [ordered]@{}
foreach ($rid in $RunIds) {
    $f = Join-Path $script:TempRoot "runs\$rid\cells.rejudged.jsonl"
    if (-not (Test-Path $f)) { $f = Join-Path $script:TempRoot "runs\$rid\cells.jsonl" }
    if (-not (Test-Path $f)) { throw "INVALID: no cells.jsonl in run $rid" }
    # A later run of a cell (same scene, device, property) replaces the earlier runs of it.
    $thisRun = [ordered]@{}
    foreach ($line in Get-Content $f) {
        if (-not $line.Trim()) { continue }
        $c = $line | ConvertFrom-Json -AsHashtable
        $base = ($c.scene -split '-')[0]
        $key = "$base|$($c.property)|$($c.device)|$($c.scene)"
        if (-not $thisRun.Contains($key)) { $thisRun[$key] = [Collections.Generic.List[object]]::new() }
        $c['runId'] = $rid
        $thisRun[$key].Add($c)
    }
    foreach ($k in $thisRun.Keys) { $cells[$k] = $thisRun[$k] }
}
# Fold to scene|property for the matrix.
$byCell = [ordered]@{}
foreach ($k in $cells.Keys) {
    $p = $k -split '\|'
    $mk = "$($p[0])|$($p[1])"
    if (-not $byCell.Contains($mk)) { $byCell[$mk] = [Collections.Generic.List[object]]::new() }
    foreach ($c in $cells[$k]) { $byCell[$mk].Add($c) }
}
$cells = $byCell
if ($cells.Count -eq 0) { throw 'INVALID: the runs hold no cells' }

$rowsOut = [ordered]@{}
$sb = [Text.StringBuilder]::new()
[void]$sb.AppendLine("Runs: $($RunIds -join ', ')")
[void]$sb.AppendLine()
foreach ($g in 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H') {
    [void]$sb.AppendLine("**$g**")
    [void]$sb.AppendLine()
    [void]$sb.AppendLine('| Scene | P1 | P2 | P3 | P4 | P5 |')
    [void]$sb.AppendLine('| --- | --- | --- | --- | --- | --- |')
    foreach ($sid in @($matrix.Keys | Where-Object { $_ -like "$g*" })) {
        $row = $matrix[$sid]
        $parts = foreach ($p in $script:AllProperties) {
            $spec = $row.cells[$p]
            $list = $cells["$sid|$p"]
            if (-not $list) { $spec.text; continue }
            $code = Merge-Codes @($list | ForEach-Object { [int]$_.code })
            $word = Get-VerdictWord $code
            $devs = ($list | ForEach-Object { $_.device } | Select-Object -Unique) -join '+'
            $runs = ($list | ForEach-Object { $_.runId -replace '^\d{8}-', '' } | Select-Object -Unique) -join ', '
            $inst = if ($spec.instruments) { "$($spec.instruments): " } else { '' }
            "measured $inst$word ($devs; $runs)"
        }
        $rowsOut[$sid] = $parts
        [void]$sb.AppendLine("| $sid | $($parts -join ' | ') |")
    }
    [void]$sb.AppendLine()
}

[void]$sb.AppendLine('**Findings** (budget - cell - measured value / limit - proposed severity - rank)')
[void]$sb.AppendLine()
$n = 0
foreach ($key in $cells.Keys) {
    foreach ($c in $cells[$key]) {
        foreach ($f in $c.findings) {
            $same = @($cells[$key] | Where-Object { @($_.findings | Where-Object { $_.budget -eq $f.budget -and $_.metric -eq $f.metric }).Count -gt 0 })
            $rank = if ($same.Count -ge 3) { 'T' } else { 'R' }
            $n++
            [void]$sb.AppendLine("- $($f.budget) - $($c.scene) $($c.device) $($c.property) - $($f.metric): $($f.value) / $($f.limit) - $($f.severity) - $rank (run $($c.runId))$(if ($f.note) { " - $($f.note)" })")
        }
    }
}
if ($n -eq 0) { [void]$sb.AppendLine('- none') }
[void]$sb.AppendLine()
[void]$sb.AppendLine('**Could not verify** (cell - reason)')
[void]$sb.AppendLine()
foreach ($key in $cells.Keys) {
    foreach ($c in $cells[$key] | Where-Object { $_.code -eq 2 }) {
        [void]$sb.AppendLine("- $($c.scene) $($c.device) $($c.property) - $($c.detail)")
    }
}
$text = $sb.ToString()
if ($UpdateTicket) {
    # Section 3.2 rows are rewritten in place: the scene title and tags stay, each measured cell keeps its
    # instrument ids so the matrix stays readable by M0 and run-matrix.
    $lines = Get-Content -LiteralPath $script:TicketFile
    $new = foreach ($l in $lines) {
        if ($l -match '^\| ([A-H]\d+) (.+?) \|(.+)\|\s*$' -and $rowsOut.Contains($Matches[1]) -and @($Matches[3] -split '\|').Count -eq 5) { "| $($Matches[1]) $($Matches[2]) | $($rowsOut[$Matches[1]] -join ' | ') |" } else { $l }
    }
    Set-Content -LiteralPath $script:TicketFile -Value $new -Encoding utf8NoBOM
}
if ($Out) { Set-Content -LiteralPath $Out -Value $text -Encoding utf8NoBOM } else { Write-Output $text }
Exit-Verdict -Subject 'S0013 report' -Code 0 -Detail "$($cells.Count) cell(s), $n finding(s)$(if ($Out) { "; $Out" })"
