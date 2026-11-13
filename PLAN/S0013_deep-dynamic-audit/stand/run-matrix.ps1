#requires -Version 7.0
<#
.SYNOPSIS
    M10 - walks the scene x property cells of S0013 section 3.2, runs each scene on the disposable profile
    and prints one verdict line per cell, then the aggregate.

.DESCRIPTION
    Order: profiles created from the recipes (M9), the variant installed and its version read back, the
    backup gate written and verified, the control of each recipe loaded (M8), then every selected scene on
    every device its tags name. Each scene starts from the restored baseline. In finally: the baseline is
    restored and the profiles are destroyed (unless -KeepProfiles).

    Cell lines: `S0013 <scene> <device> <P>: <WORD> (<detail>)`. The aggregate is the last line.

    Exit code: 0 every judged cell passed (CHECK-VERDICT 2.14 precedence); 1 at least one cell failed (a
    finding); 2 no cell failed and at least one could not be verified (invalid run, no driver, no pair,
    no control). Codes 3 and 4 are never emitted.

.PARAMETER Phase
    1-4 select the section 10 phase's scenes; 'all' every scene. -Scenes overrides it.
.PARAMETER WithM7
    Run the M7 locale and font sweep at each checkpoint (Phase 4; also needed for any P5 verdict).
.PARAMETER Repeat
    Runs per scene; three give the T rank of section 9.
#>
[CmdletBinding(PositionalBinding = $false)]
param(
    [string] $Phase = 'all',
    [string[]] $Scenes,
    [string[]] $Devices = @('phone', 'watch'),
    [string[]] $Properties = @('P1', 'P2', 'P3', 'P4', 'P5'),
    [ValidateSet('noLegal', 'store')] [string] $Variant = 'noLegal',
    [switch] $WithM7,
    [int] $Repeat = 1,
    [int] $SoakMinutes = 10,
    [string[]] $SoakScreens = @('dashboard', 'water', 'sos', 'stopwatch'),
    [string] $RunId,
    [switch] $ReuseProfiles,
    [switch] $KeepProfiles
)

# `pwsh -File` hands a comma list over as one string.
foreach ($n in 'Scenes', 'Devices', 'Properties', 'SoakScreens') { $v = Get-Variable -Name $n -ValueOnly -ErrorAction SilentlyContinue; if ($v) { Set-Variable -Name $n -Value @($v | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim() } | Where-Object { $_ }) } }

. (Join-Path $PSScriptRoot '_stand.ps1')
. (Join-Path $PSScriptRoot '_instruments.ps1')
. (Join-Path $PSScriptRoot '_judges.ps1')
. (Join-Path $PSScriptRoot 'scenes.ps1')
. (Join-Path $PSScriptRoot '_engine.ps1')
trap { Write-Host "run-matrix: $($_.Exception.Message)" -ForegroundColor Red; Exit-Verdict -Subject 'S0013 run-matrix' -Code 2 -Detail ($_.Exception.Message -replace '^INVALID:\s*', '') }

if (-not $RunId) { $RunId = New-RunId "matrix-$Variant" }
$runDir = Get-RunDir $RunId
$matrix = Read-Matrix
if ($matrix.Count -lt 40) { throw "INVALID: the matrix in the ticket parsed to $($matrix.Count) rows - M0 should be run" }
$selected = if ($Scenes) { $Scenes } elseif ($Phase -eq 'all') { @($matrix.Keys) } else { $script:PhaseScenes[$Phase] }
if (-not $selected) { throw "INVALID: phase '$Phase' names no scenes" }

$needPhone = $Devices -contains 'phone' -and @($selected | Where-Object { $matrix[$_].devices -contains 'phone' -or $matrix[$_].pair }).Count -gt 0
$needWatch = $Devices -contains 'watch' -and @($selected | Where-Object { $matrix[$_].devices -contains 'watch' -or $matrix[$_].pair }).Count -gt 0
$needNoBaro = $needPhone -and ($selected -contains 'F3')
$needNoBaroW = $needWatch -and ($selected -contains 'F3')
$recipes = @()
if ($needPhone) { $recipes += 'phone' }
if ($needWatch) { $recipes += 'wear' }
if ($needNoBaro) { $recipes += 'phone-nobaro' }
if ($needNoBaroW) { $recipes += 'wear-nobaro' }

function Invoke-Profile([string] $Action, [string] $Recipe) {
    $r = Invoke-Native -FilePath 'pwsh' -Arguments @('-NoProfile', '-File', (Join-Path $PSScriptRoot 'profile.ps1'), '-Action', $Action, '-Recipe', $Recipe, '-Variant', $Variant, '-RunId', $RunId) -TimeoutSec 900 -AllowFail
    $line = ($r.Out -split "`n" | Where-Object { $_.Trim() } | Select-Object -Last 1)
    Write-Output "  $($line.Trim())"
    if ($r.ExitCode -ne 0) { throw "INVALID: profile $Action $Recipe refused: $line" }
}

$manifest = [ordered]@{ runId = $RunId; variant = $Variant; started = (Get-Date).ToString('s'); scenes = $selected; devices = $Devices; properties = $Properties; withM7 = [bool]$WithM7; repeat = $Repeat; soakMinutes = $SoakMinutes; reuseProfiles = [bool]$ReuseProfiles; profiles = @{} }
$all = [Collections.Generic.List[object]]::new()
$created = @()
try {
    foreach ($rcp in $recipes) {
        $live = (Get-ActiveProfiles)[$rcp]
        if (-not ($ReuseProfiles -and $live)) {
            if ($live) { Invoke-Profile Destroy $rcp }
            Invoke-Profile Create $rcp
            $created += $rcp
        }
        Invoke-Profile Install $rcp
        Invoke-Profile Backup $rcp
        Invoke-Profile Verify $rcp
        $p = (Get-ActiveProfiles)[$rcp]
        $manifest.profiles[$rcp] = [ordered]@{ serial = $p.serial; recipeHash = $p.recipeHash; build = $p.build; control = [bool](Get-ControlFor $rcp) }
        if (-not (Get-ControlFor $rcp)) { Write-Output "  control: none for $rcp ($(Get-RecipeHash $rcp)) - band-dependent budgets will not verify; run control.ps1" }
    }
    Write-JsonFile (Join-Path $runDir 'manifest.json') $manifest

    $keySets = @{}
    foreach ($sid in $selected) {
        $row = $matrix[$sid]
        if (-not $row) { Write-Output "S0013 ${sid}: COULD NOT VERIFY (not in the matrix)"; continue }
        $roles = @($row.devices | Where-Object { $_ -in $Devices })
        if ($row.pair) { $roles = @('pair') }
        foreach ($role in $roles) {
            if ($role -eq 'pair') {
                $pc = Invoke-Native -FilePath 'pwsh' -Arguments @('-NoProfile', '-File', (Join-Path $PSScriptRoot 'profile.ps1'), '-Action', 'PairCheck') -AllowFail -TimeoutSec 120
                $reason = (($pc.Out -split "`n" | Where-Object { $_.Trim() } | Select-Object -Last 1) -replace '^.*COULD NOT VERIFY \((.*)\)\s*$', '$1').Trim()
                foreach ($p in $Properties) {
                    if ($row.cells[$p].state -ne 'open') { continue }
                    $cell = New-Cell $sid 'pair' $p $row.cells[$p].instruments
                    if ($pc.ExitCode -ne 0) { $cell.invalid.Add($reason) } else { $cell.invalid.Add('pair scenes have no driver yet') }
                    [void](Complete-Cell $cell); $all.Add($cell); Write-Output (Format-CellLine $cell)
                }
                continue
            }
            $recipe = if ($sid -eq 'F3') { if ($role -eq 'watch') { 'wear-nobaro' } else { 'phone-nobaro' } } elseif ($role -eq 'watch') { 'wear' } else { 'phone' }
            $subs = if ($sid -eq 'G1') { $SoakScreens } else { @('') }
            foreach ($sub in $subs) {
                foreach ($rep in 1..$Repeat) {
                    $cells = Invoke-Scene -SceneId $sid -Role $role -Recipe $recipe -RunId $RunId -Properties $Properties -Matrix $matrix -WithM7 ([bool]$WithM7) -SoakMinutes $SoakMinutes -SoakScreen $sub -Rep $rep -KeySets $keySets
                    foreach ($c in $cells) { $all.Add($c); Write-Output (Format-CellLine $c) }
                }
            }
        }
    }
} finally {
    ($all | ForEach-Object { $_ | ConvertTo-Json -Depth 8 -Compress }) | Set-Content -LiteralPath (Join-Path $runDir 'cells.jsonl') -Encoding utf8NoBOM
    foreach ($rcp in $recipes) {
        if (-not (Get-ActiveProfiles)[$rcp]) { continue }
        try { Invoke-Profile Restore $rcp } catch { Write-Output "  restore $rcp failed: $($_.Exception.Message)" }
        if (-not $KeepProfiles -and ($created -contains $rcp -or -not $ReuseProfiles)) { try { Invoke-Profile Destroy $rcp } catch { Write-Output "  destroy $rcp failed: $($_.Exception.Message)" } }
    }
}

$codes = @($all | ForEach-Object { $_.code })
$code = if ($codes.Count -eq 0) { 2 } else { Merge-Codes $codes }
$failed = @($all | Where-Object { $_.code -eq 1 } | ForEach-Object { "$($_.scene)/$($_.device)/$($_.property)" })
$cnv = @($all | Where-Object { $_.code -eq 2 }).Count
$passed = @($all | Where-Object { $_.code -eq 0 }).Count
$summary = [ordered]@{ runId = $RunId; code = $code; cells = $all.Count; passed = $passed; failed = $failed.Count; couldNotVerify = $cnv; finished = (Get-Date).ToString('s') }
Write-JsonFile (Join-Path $runDir 'summary.json') $summary
$detail = "$($all.Count) cell(s): $passed passed, $($failed.Count) failed, $cnv could not verify; run $RunId"
if ($failed.Count) { $detail = "$($failed.Count): $(($failed | Select-Object -First 8) -join ', ')$(if ($failed.Count -gt 8) { ', ..' }); $detail" }
Exit-Verdict -Subject 'S0013 run-matrix' -Code $code -Detail $detail
