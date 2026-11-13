# Every font-2.0 M7 dump of the 2026-10-25 runs through the old and the new reader; prints only the new entries.
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$stand = 'P:\ANDROID\Tourist_mob\PLAN\S0013_deep-dynamic-audit\stand'
. (Join-Path $stand '_stand.ps1')
. (Join-Path $stand '_instruments.ps1')
$n = 0
foreach ($p in Get-ChildItem 'P:\ANDROID\Tourist_mob\temp\S0013\runs' -Directory -Filter '20261025*' | ForEach-Object { Get-ChildItem $_.FullName -Recurse -Filter 'm7-*-2.xml' }) {
    $n++
    $density = if ($p.Directory.Name -match '-watch-') { 320 } else { 420 }
    $large = [xml](Get-Content $p.FullName -Raw)
    $basePath = $p.FullName -replace '-2\.xml$', '-1.xml'
    $old = @((Get-A11yFacts -Dump $large -DensityDpi $density -MinDp 0).clipped)
    $new = @((Get-A11yFacts -Dump $large -DensityDpi $density -MinDp 0 -FontScale 2.0).clipped)
    $dropped = if (Test-Path $basePath) { @(Get-DroppedLabels -Base ([xml](Get-Content $basePath -Raw)) -Large $large) } else { @('<no base>') }
    if ($new.Count -ne $old.Count -or $dropped.Count) {
        "{0}/{1}: old [{2}] new [{3}] dropped [{4}]" -f $p.Directory.Parent.Parent.Name, $p.Name, ($old -join ', '), ($new -join ', '), ($dropped -join ', ')
    }
}
"dumps read: $n"
