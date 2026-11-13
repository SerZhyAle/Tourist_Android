# Direct call of the S0034 reader change on the 2026-10-25 C2 watch dumps.
param([string] $Dir = 'P:\ANDROID\Tourist_mob\temp\S0013\runs\20261025-p5-watch\scenes\C2-watch-r1', [int] $Density = 320)
$stand = 'P:\ANDROID\Tourist_mob\PLAN\S0013_deep-dynamic-audit\stand'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
. (Join-Path $stand '_stand.ps1')
. (Join-Path $stand '_instruments.ps1')
foreach ($loc in 'en-US', 'ru-RU', 'uk-UA') {
    $base = [xml](Get-Content (Join-Path $Dir "m7-compass-after-storm-$loc-1.xml") -Raw)
    $large = [xml](Get-Content (Join-Path $Dir "m7-compass-after-storm-$loc-2.xml") -Raw)
    $old = (Get-A11yFacts -Dump $large -DensityDpi $Density -MinDp 40).clipped
    $f = Get-A11yFacts -Dump $large -DensityDpi $Density -MinDp 40 -FontScale 2.0
    $dropped = @(Get-DroppedLabels -Base $base -Large $large)
    "$loc width-off [$($old -join ', ')] width-on [$($f.clipped -join ', ')] dropped [$($dropped -join ', ')]"
}
