#requires -Version 7.0
<#
.SYNOPSIS
    Self-test of tools/docs/check-docs.ps1 (S0029): each check passes on a clean throw-away corpus and fails on
    the defect it exists to catch.
.DESCRIPTION
    Every case builds a fresh git work tree under temp/docs-selftest/, applies one change, runs one check in a
    child process and compares its exit code and verdict word. The tree is removed at the end.
    Exit codes (CHECK-VERDICT): 0 every case held, 1 a case did not, 2 the fixture could not be built.
#>
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$check = Join-Path $PSScriptRoot 'check-docs.ps1'
$base = Join-Path $repo 'temp/docs-selftest'
$outside = Join-Path ([IO.Path]::GetTempPath()) "docs-selftest-$PID"

function Write-Text([string]$root, [string]$rel, [string]$text) {
    $full = Join-Path $root $rel
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $full) | Out-Null
    [IO.File]::WriteAllText($full, $text)
}

function New-Fixture([string]$name, [bool]$git = $true) {
    # A tree outside git must sit outside this repository too, or rev-parse finds the repository around it.
    $root = Join-Path $(if ($git) { $base } else { $outside }) $name
    New-Item -ItemType Directory -Force -Path $root | Out-Null
    if ($git) { & git -C $root init -q 2>$null; if ($LASTEXITCODE) { throw "git init failed in $root" } }
    Write-Text $root '.gitignore' "PLAN/`n"
    Write-Text $root 'README.md' "# Fixture`n`nSee [the guide](docs/guide.md#setup) and [the index](docs/README.md).`n"
    Write-Text $root 'docs/README.md' "# Docs`n`n- [Guide](guide.md)`n"
    Write-Text $root 'docs/guide.md' "# Guide`n`n## Setup`n`nRun it - then read ``a ... b`` in code.`n"
    Write-Text $root 'docs/DOCUMENT_REGISTRY.jsonl' (@(
            '{"id":"readme","title":"Readme","category":"product","audience":"mixed","paths":["README.md"],"published":false,"indexable":false,"product_areas":["product"],"update_triggers":["release"],"generated":false}',
            '{"id":"docs","title":"Docs","category":"process","audience":"contributor","paths":["docs/*.md","docs/DOCUMENT_REGISTRY.jsonl"],"published":false,"indexable":false,"product_areas":["documentation"],"update_triggers":["documentation"],"generated":false}'
        ) -join "`n")
    Write-Text $root 'PLAN/DOCUMENT_REGISTRY.jsonl' (@(
            '{"id":"plan","title":"Plan","category":"specs","audience":"contributor","paths":["PLAN/DOCUMENT_REGISTRY.jsonl","PLAN/S[0-9][0-9][0-9][0-9]_*.md","PLAN/S[0-9][0-9][0-9][0-9]_*/**"],"published":false,"indexable":false,"product_areas":["specs"],"update_triggers":["ticket"],"generated":false}',
            '{"id":"done","title":"Done","category":"history","audience":"contributor","paths":["PLAN/DONE/**"],"published":false,"indexable":false,"product_areas":["specs"],"update_triggers":["ticket-close"],"generated":false,"archive":true}'
        ) -join "`n")
    Write-Text $root 'PLAN/S0001_demo.md' "# S0001`n`nVerified on the emulator (screenshots ``01``-``02`` in the folder).`n"
    Write-Text $root 'PLAN/S0001_demo/01_start.png' 'png'
    Write-Text $root 'PLAN/S0001_demo/02_end.png' 'png'
    Write-Text $root 'PLAN/DONE/S0000_old.md' "# Old`n`nA [moved record](gone.md) stays as written.`n"
    $root
}

$cases = @(
    @{ Name = 'clean-registry'; Check = 'registry'; Exit = 0; Word = 'PASS'; Change = {} }
    @{ Name = 'clean-links'; Check = 'links'; Exit = 0; Word = 'PASS'; Change = {} }
    @{ Name = 'clean-style'; Check = 'style'; Exit = 0; Word = 'PASS'; Change = {} }
    @{ Name = 'clean-assets'; Check = 'assets'; Exit = 0; Word = 'PASS'; Change = {} }
    @{ Name = 'clean-all'; Check = 'all'; Exit = 0; Word = 'PASS'; Change = {} }
    @{ Name = 'undeclared-file'; Check = 'registry'; Exit = 1; Word = 'FAIL'; Change = { param($r) Write-Text $r 'docs/notes.txt' 'x' } }
    @{ Name = 'declared-missing'; Check = 'registry'; Exit = 1; Word = 'FAIL'; Change = { param($r) Remove-Item -LiteralPath (Join-Path $r 'README.md') } }
    @{ Name = 'tracked-names-ignored'; Check = 'registry'; Exit = 1; Word = 'FAIL'; Change = {
            param($r) Add-Content -LiteralPath (Join-Path $r 'docs/DOCUMENT_REGISTRY.jsonl') "`n{`"id`":`"leak`",`"title`":`"Leak`",`"category`":`"x`",`"audience`":`"x`",`"paths`":[`"PLAN/S0001_demo.md`"],`"published`":false,`"indexable`":false,`"product_areas`":[`"x`"],`"update_triggers`":[`"x`"],`"generated`":false}" } }
    @{ Name = 'broken-link'; Check = 'links'; Exit = 1; Word = 'FAIL'; Change = { param($r) Add-Content -LiteralPath (Join-Path $r 'docs/README.md') '- [Gone](missing.md)' } }
    @{ Name = 'broken-anchor'; Check = 'links'; Exit = 1; Word = 'FAIL'; Change = { param($r) Add-Content -LiteralPath (Join-Path $r 'docs/README.md') '- [Nowhere](guide.md#no-such-heading)' } }
    @{ Name = 'link-into-archive'; Check = 'links'; Exit = 1; Word = 'FAIL'; Change = { param($r) Add-Content -LiteralPath (Join-Path $r 'docs/README.md') '- [Old](../PLAN/DONE/S0000_gone.md)' } }
    @{ Name = 'ellipsis-in-prose'; Check = 'style'; Exit = 1; Word = 'FAIL'; Change = { param($r) Add-Content -LiteralPath (Join-Path $r 'docs/guide.md') 'And then...' } }
    @{ Name = 'em-dash-in-prose'; Check = 'style'; Exit = 1; Word = 'FAIL'; Change = { param($r) Add-Content -LiteralPath (Join-Path $r 'docs/guide.md') "One $([char]0x2014) two" } }
    @{ Name = 'stale-baseline'; Check = 'style'; Exit = 1; Word = 'FAIL'; Change = { param($r) Write-Text $r 'tools/docs/baseline.txt' "docs-style docs/guide.md ellipsis paid off long ago`n" } }
    @{ Name = 'baselined-finding'; Check = 'style'; Exit = 0; Word = 'PASS'; Change = {
            param($r) Add-Content -LiteralPath (Join-Path $r 'docs/guide.md') 'And then...'; Write-Text $r 'tools/docs/baseline.txt' "docs-style docs/guide.md ellipsis And then...`n" } }
    @{ Name = 'missing-screenshot'; Check = 'assets'; Exit = 1; Word = 'FAIL'; Change = { param($r) Remove-Item -LiteralPath (Join-Path $r 'PLAN/S0001_demo/02_end.png') } }
    @{ Name = 'broken-image-link'; Check = 'assets'; Exit = 1; Word = 'FAIL'; Change = { param($r) Add-Content -LiteralPath (Join-Path $r 'docs/guide.md') '![Shot](shots/none.png)' } }
    @{ Name = 'tracked-cites-ignored'; Check = 'assets'; Exit = 1; Word = 'FAIL'; Change = { param($r) Add-Content -LiteralPath (Join-Path $r 'docs/guide.md') '![Shot](../PLAN/S0001_demo/01_start.png)' } }
    @{ Name = 'aggregate-fail'; Check = 'all'; Exit = 1; Word = 'FAIL'; Change = { param($r) Add-Content -LiteralPath (Join-Path $r 'docs/README.md') '- [Gone](missing.md)' } }
    @{ Name = 'invalid-registry'; Check = 'registry'; Exit = 2; Word = 'COULD NOT VERIFY'; Change = { param($r) Add-Content -LiteralPath (Join-Path $r 'PLAN/DOCUMENT_REGISTRY.jsonl') '{not json' } }
    @{ Name = 'no-git'; Check = 'links'; Exit = 2; Word = 'COULD NOT VERIFY'; Git = $false; Change = {} }
)

$failed = 0
try {
    if (Test-Path -LiteralPath $base) { Remove-Item -LiteralPath $base -Recurse -Force }
    foreach ($c in $cases) {
        $root = New-Fixture $c.Name ($c.Git -ne $false)
        & $c.Change $root
        $args_ = @('-NoProfile', '-File', $check, '-Check', $c.Check, '-Root', $root)
        if ($c.Check -eq 'all') { $args_ += '-NoHarness' }
        $out = @(& pwsh @args_ 2>&1 | ForEach-Object { "$_" })
        $code = $LASTEXITCODE
        $last = if ($out.Count) { $out[-1] } else { '' }
        $subject = if ($c.Check -eq 'all') { 'docs-check' } else { "docs-$($c.Check)" }
        $wordOk = $last -match "^${subject}: $([regex]::Escape($c.Word))( \(|$)"
        if ($code -eq $c.Exit -and $wordOk) { Write-Host "ok   $($c.Name): exit $code, $last" }
        else {
            $failed++
            Write-Host "FAIL $($c.Name): expected exit $($c.Exit) and '$subject`: $($c.Word)', got exit $code and '$last'"
            $out | Select-Object -Last 6 | ForEach-Object { Write-Host "       $_" }
        }
    }
}
catch {
    Write-Host "docs-selftest could not build its fixture: $($_.Exception.Message)"
    Write-Host "docs-selftest: COULD NOT VERIFY ($($_.Exception.Message))"
    exit 2
}
finally {
    foreach ($dir in @($base, $outside)) { if (Test-Path -LiteralPath $dir) { Remove-Item -LiteralPath $dir -Recurse -Force -ErrorAction SilentlyContinue } }
}
if ($failed) { Write-Host "docs-selftest: FAIL (run=$($cases.Count) skipped=0 failed=$failed)"; exit 1 }
Write-Host "docs-selftest: PASS (run=$($cases.Count) skipped=0)"
exit 0
