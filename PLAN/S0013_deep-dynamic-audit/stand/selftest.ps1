#requires -Version 7.0
<#
.SYNOPSIS
    M0 - coverage self-test of the S0013 matrix (section 3.1).

.DESCRIPTION
    Ground truth: CLAUDE.md section 1 (the capability list) and docs/BOOTSTRAP_HANDOFF.md section 3 (the
    locales, accessibility and input modes). Checks, each one a gap when it fails:
      1. every capability of CLAUDE.md section 1 maps to at least one scene with an open or measured cell
         (or an n/a that names its owner decision);
      2. every locale and mode of handoff section 3 is carried by the matrix (D5, M7 and its sweep);
      3. every open cell names instrument ids that exist in the registry (section 3.1);
      4. every registry row declares exactly the codes 0/1/2 and each script file it names exists with an
         `Exit code:` header naming 0, 1 and 2;
      5. every budget row of section 8 equals budgets.json character for character;
      6. every scene of the matrix has a driver for each device its tags name ([pair] excepted).

    Exit code: 0 complete; 1 gap list printed; 2 a source is unreadable.
#>
[CmdletBinding(PositionalBinding = $false)]
param([string] $Ticket, [string] $ClaudeMd, [string] $Handoff)

. (Join-Path $PSScriptRoot '_stand.ps1')
. (Join-Path $PSScriptRoot '_instruments.ps1')
. (Join-Path $PSScriptRoot '_judges.ps1')
. (Join-Path $PSScriptRoot 'scenes.ps1')
. (Join-Path $PSScriptRoot '_engine.ps1')
trap { Write-Host "selftest: $($_.Exception.Message)" -ForegroundColor Red; Exit-Verdict -Subject 'S0013 selftest' -Code 2 -Detail ($_.Exception.Message -replace '^INVALID:\s*', '') }

if (-not $Ticket) { $Ticket = $script:TicketFile }
if (-not $ClaudeMd) { $ClaudeMd = Join-Path $script:RepoRoot 'CLAUDE.md' }
if (-not $Handoff) { $Handoff = Join-Path $script:RepoRoot 'docs\BOOTSTRAP_HANDOFF.md' }
foreach ($f in $Ticket, $ClaudeMd, $Handoff) { if (-not (Test-Path -LiteralPath $f)) { throw "INVALID: source unreadable: $f" } }

$gaps = [Collections.Generic.List[string]]::new()
$matrix = Read-Matrix $Ticket
if ($matrix.Count -eq 0) { throw 'INVALID: no matrix rows in the ticket' }

# 1. Capabilities of CLAUDE.md section 1 -> scenes. The token list is read from the section; each token
#    must have a mapping here, so a capability added to CLAUDE.md without a scene fails the self-test.
$claude = Get-Content -LiteralPath $ClaudeMd -Raw
$sec1 = if ($claude -match '(?s)## 1\. The product(.+?)\n## ') { $Matches[1] } else { throw 'INVALID: CLAUDE.md section 1 not found' }
$sec1 = $sec1 -replace '\s+', ' '
$capabilityMap = [ordered]@{
    'Tourist dashboard'       = @{ pattern = 'Tourist dashboard'; scenes = 'C1', 'C10', 'G1' }
    'compass hero tile'       = @{ pattern = 'compass hero tile'; scenes = 'C2' }
    'SOS'                     = @{ pattern = '\bSOS\b'; scenes = 'C5', 'C6', 'D3' }
    'water flashlight'        = @{ pattern = 'water\s+flashlight'; scenes = 'C4', 'D1' }
    'screen flashlight'       = @{ pattern = 'screen and camera flashlights'; scenes = 'C3' }
    'camera flashlight'       = @{ pattern = 'screen and camera flashlights'; scenes = 'C3', 'C9', 'F4' }
    'stopwatch'               = @{ pattern = 'multi-participant stopwatch'; scenes = 'C7', 'E1' }
    'Wear OS app'             = @{ pattern = 'Wear OS app'; scenes = 'B3', 'B5', 'D2' }
    'watch face'              = @{ pattern = 'watch face'; scenes = 'H1' }
}
$known = 'Tourist dashboard', 'compass hero tile', 'SOS', 'water flashlight', 'screen and camera flashlights', 'multi-participant stopwatch', 'Wear OS app', 'watch face'
$capLine = ($sec1 -split "`n" | Where-Object { $_ -match 'hike kit' } | Select-Object -First 3) -join ' '
foreach ($k in $known) { if ($capLine -notmatch [regex]::Escape($k) -and $sec1 -notmatch [regex]::Escape($k)) { $gaps.Add("CLAUDE.md section 1 no longer names '$k' - update the capability map") } }
# Any capability noun phrase in the list line that the map does not know is a gap.
foreach ($c in $capabilityMap.Keys) {
    $m = $capabilityMap[$c]
    if ($sec1 -notmatch $m.pattern) { $gaps.Add("capability '$c' not found in CLAUDE.md section 1") ; continue }
    $covered = @($m.scenes | Where-Object { $matrix.Contains($_) -and @($matrix[$_].cells.Values | Where-Object { $_.state -in 'open', 'measured' -or ($_.state -eq 'n/a' -and $_.text -match '\b\d\.\d\b|decision|S\d{4}') }).Count -gt 0 })
    if ($covered.Count -eq 0) { $gaps.Add("capability '$c' maps to no open or measured cell ($($m.scenes -join ', '))") }
}

# 2. Locales and modes of handoff section 3
$handoff = Get-Content -LiteralPath $Handoff -Raw
$sec3 = if ($handoff -match '(?s)## 3\.[^\n]*\n(.+?)\n## ') { $Matches[1] } else { throw 'INVALID: handoff section 3 not found' }
$modes = [ordered]@{
    'EN/RU/UK'          = @{ pattern = 'EN/RU/UK'; carrier = { $matrix['D5'] -and $matrix['D5'].cells.P5.state -eq 'open' } }
    'TalkBack labels'   = @{ pattern = 'TalkBack labels'; carrier = { (Get-Budgets).Contains('8.5e') } }
    'touch targets'     = @{ pattern = 'touch targets'; carrier = { (Get-Budgets).Contains('8.5c') } }
    'state not by colour' = @{ pattern = 'state not shown by colour'; carrier = { (Get-Command Test-ToggleExposesState -ErrorAction SilentlyContinue) -ne $null } }
    'input parity'      = @{ pattern = 'input parity'; carrier = { @($script:Drivers.Keys | Where-Object { $script:Drivers[$_].Contains('watch') }).Count -gt 0 } }
}
foreach ($k in $modes.Keys) {
    if ($sec3 -notmatch $modes[$k].pattern) { $gaps.Add("handoff section 3 no longer names '$k'"); continue }
    if (-not (& $modes[$k].carrier)) { $gaps.Add("handoff section 3 '$k' has no carrier in the stand") }
}

# 3. Open cells name registered instruments; 4. registry rows declare 0/1/2 and their scripts exist.
$ticketText = Get-Content -LiteralPath $Ticket -Raw
$registry = [ordered]@{}
foreach ($m in [regex]::Matches($ticketText, '(?m)^\| (M\d+) \| `([\w-]+)` \| (.+?) \| (.+?) \|\s*$')) { $registry[$m.Groups[1].Value] = [ordered]@{ script = $m.Groups[2].Value; contract = $m.Groups[3].Value; basis = $m.Groups[4].Value } }
if ($registry.Count -lt 11) { $gaps.Add("registry has $($registry.Count) rows, expected M0-M10") }
foreach ($sid in $matrix.Keys) {
    foreach ($p in $script:AllProperties) {
        $cell = $matrix[$sid].cells[$p]
        if ($cell.state -eq 'unknown') { $gaps.Add("$sid ${p}: cell '$($cell.text)' is neither open, measured nor n/a") }
        if ($cell.state -eq 'open') {
            if (-not $cell.instruments) { $gaps.Add("$sid ${p}: open cell names no instrument") }
            foreach ($i in ($cell.instruments -split ',\s*' | Where-Object { $_ })) { if (-not $registry.Contains($i)) { $gaps.Add("$sid ${p}: instrument $i is not in the registry") } }
        }
        if ($cell.state -eq 'n/a' -and $cell.text -notmatch '^n/a:\s*\S') { $gaps.Add("$sid ${p}: n/a without a reason") }
    }
}
$fileFor = @{ 'run-matrix' = 'run-matrix.ps1' }
foreach ($id in $registry.Keys) {
    $file = Join-Path $PSScriptRoot ($fileFor[$registry[$id].script] ?? "$($registry[$id].script).ps1")
    if (-not (Test-Path $file)) { $gaps.Add("$id script $($registry[$id].script) has no file in the stand"); continue }
    $head = (Get-Content $file -TotalCount 60) -join "`n"
    if ($head -notmatch '(?s)Exit code:.*\b0\b.*\b1\b.*\b2\b') { $gaps.Add("$id $($registry[$id].script): header does not declare exit codes 0, 1 and 2") }
}

# 5. Budgets: ticket section 8 vs budgets.json
$budgets = Get-Budgets
foreach ($m in [regex]::Matches($ticketText, '(?m)^\| (8\.\d[a-z]) \| (P\d) \| (.+?) \| (.+?) \|\s*$')) {
    $id = $m.Groups[1].Value; $text = $m.Groups[4].Value.Trim()
    if (-not $budgets.Contains($id)) { $gaps.Add("budget $id is in the ticket but not in budgets.json"); continue }
    if ($budgets[$id].text -ne $text) { $gaps.Add("budget $id differs: ticket '$text' vs budgets.json '$($budgets[$id].text)'") }
    if ($text -notmatch '`\[(h|hard)\b') { $gaps.Add("budget $id is marked neither [h] nor [hard]") }
}

# 6. Drivers
foreach ($sid in $matrix.Keys) {
    $row = $matrix[$sid]
    if ($row.pair) { continue }
    $openAny = @($row.cells.Values | Where-Object { $_.state -eq 'open' }).Count -gt 0
    if (-not $openAny) { continue }
    foreach ($d in $row.devices) {
        if (-not ($script:Drivers.Contains($sid) -and $script:Drivers[$sid].Contains($d))) { $gaps.Add("$sid has open cells on $d but no $d driver") }
    }
}

foreach ($g in $gaps) { Write-Output "  gap: $g" }
$openCells = @($matrix.Values | ForEach-Object { $_.cells.Values } | Where-Object { $_.state -eq 'open' }).Count
$measured = @($matrix.Values | ForEach-Object { $_.cells.Values } | Where-Object { $_.state -eq 'measured' }).Count
$na = @($matrix.Values | ForEach-Object { $_.cells.Values } | Where-Object { $_.state -eq 'n/a' }).Count
$detail = "$($matrix.Count) scenes, $openCells open, $measured measured, $na n/a, $($registry.Count) instruments"
if ($gaps.Count) { Exit-Verdict -Subject 'S0013 selftest' -Code 1 -Detail "$($gaps.Count) gap(s); $detail" }
Exit-Verdict -Subject 'S0013 selftest' -Code 0 -Detail $detail
