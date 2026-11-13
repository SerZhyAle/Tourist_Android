#requires -Version 7.0
<#
.SYNOPSIS
    M4 - growth slope with a confidence band: Theil-Sen slope and a percentile bootstrap CI over an M3
    series, judged against the M8 control band (budget 8.4a).

.DESCRIPTION
    Reads a sampler CSV, keeps the rows of the last process (a restart is a new series), and for each
    column reports the slope in % of the series median per hour with its 95% CI. Verdict per column:
    CI upper bound <= band + 1 %/h passes; CI lower bound above it fails; a CI straddling the limit cannot
    tell a leak from noise and is not a verdict (section 7).

    Exit code: 0 every column passed; 1 at least one column's whole CI lies above the limit, or the descriptor
    plateau lies above its ceiling; 2 no control band, too few points, or a CI straddling the limit.

.PARAMETER SkipMinutes
    Judges the scene's soak window only, past its first minutes, for a series that fills before it settles. The
    window is read from evidence.json beside the CSV; a CSV without one is not a verdict.
.PARAMETER FdCeilingCsv
    The samples of a continuously drawing screen: the highest fd of this series must not exceed the highest fd
    of that one's soak window.
#>
[CmdletBinding(PositionalBinding = $false)]
param([Parameter(Mandatory)] [string] $Csv, [string] $Recipe = 'phone', [string[]] $Columns = @('pssKb', 'fd', 'threads'), [double] $SkipMinutes = 0, [string] $FdCeilingCsv)

. (Join-Path $PSScriptRoot '_stand.ps1')
. (Join-Path $PSScriptRoot '_instruments.ps1')
. (Join-Path $PSScriptRoot '_judges.ps1')
. (Join-Path $PSScriptRoot 'scenes.ps1')
. (Join-Path $PSScriptRoot '_engine.ps1')
trap { Write-Host "slope: $($_.Exception.Message)" -ForegroundColor Red; Exit-Verdict -Subject 'S0013 slope' -Code 2 -Detail ($_.Exception.Message -replace '^INVALID:\s*', '') }

$samples = if ($SkipMinutes -gt 0) { @(Get-SoakTail $Csv $SkipMinutes) } else { @(Read-Samples $Csv) }
if ($samples.Count -lt 8) { throw "INVALID: $($samples.Count) samples in $Csv" }
$control = Get-ControlFor $Recipe
if (-not $control) { throw "INVALID: no control for $Recipe ($(Get-RecipeHash $Recipe)) - run control.ps1" }
$cell = New-Cell 'slope' $Recipe 'P4' 'M4'
$last = $samples | Where-Object { $_.pid } | Select-Object -Last 1
$series = @($samples | Where-Object { $last -and $_.pid -eq $last.pid })
$b = Get-Budgets
foreach ($col in $Columns) {
    $pts = @($series | Where-Object { $null -ne $_[$col] })
    $sl = Get-SlopePctPerHour -TimesMs ([double[]]($pts | ForEach-Object { $_.ts })) -Values ([double[]]($pts | ForEach-Object { $_[$col] })) -Boot 600
    if (-not $sl) { $cell.invalid.Add("${col}: too few points"); continue }
    $band = [double]$control.slope[$col].ciHighPct
    $limit = $band + $b['8.4a'].limit
    Write-Output ("  {0}: {1:0.00} %/h, CI {2:0.00}..{3:0.00}, limit {4:0.00} (n={5}, {6:0.00} h)" -f $col, $sl.pctPerHour, $sl.ciLowPct, $sl.ciHighPct, $limit, $sl.n, $sl.hours)
    if ($sl.ciHighPct -le $limit) { $cell.judged++ }
    elseif ($sl.ciLowPct -gt $limit) { $cell.judged++; Add-Finding $cell '8.4a' "$col slope %/h" ([math]::Round($sl.pctPerHour, 2)) ([math]::Round($limit, 2)) 'P1' }
    else { $cell.invalid.Add("${col}: CI straddles the limit") }
}
if ($FdCeilingCsv) {
    $plateau = Get-ColumnMax $series 'fd'
    $ceiling = Get-ColumnMax @(Get-SoakTail $FdCeilingCsv) 'fd'
    Write-Output ("  fd plateau {0}, ceiling {1}" -f $plateau, $ceiling)
    if ($null -eq $plateau -or $null -eq $ceiling) { $cell.invalid.Add('fd ceiling: no fd samples') }
    else {
        $cell.judged++
        if ($plateau -gt $ceiling) { Add-Finding $cell '8.4a' 'fd plateau above the ceiling of a drawing screen' $plateau $ceiling 'P1' }
    }
}
[void](Complete-Cell $cell)
Exit-Verdict -Subject 'S0013 slope' -Code $cell.code -Detail $cell.detail
