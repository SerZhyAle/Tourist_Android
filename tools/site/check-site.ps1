#requires -Version 7.0
<#
.SYNOPSIS
    Checks the hosted pages under site/ before they are published (S0026 W3, owner's order of 2026-11-13).
.DESCRIPTION
    The privacy pages are hand-written HTML; nothing renders them, so this gate holds what a renderer would:
      - each site/*.html page has one <h1>, a canonical link equal to its own published address, the reciprocal
        hreflang set (en, ru, uk and x-default) and the matching og:url;
      - every relative href and src of every page resolves to a file under site/;
      - sitemap.xml lists every page and nothing else, and robots.txt names the sitemap;
      - site/assets/sza-kit.css has the sha256 that site/assets/PROVENANCE.txt records (the family kit is copied byte
        for byte and never edited);
      - no page carries "..." or U+2026 in its text (house style).
    Runs on the Windows runner of .github/workflows/pages.yml and locally between releases.
.NOTES
    Exit codes: 0 PASS, 1 FAIL, 2 could not verify. The verdict line is the last line written.
#>
[CmdletBinding()]
param(
    [string]$Root = (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
)
$ErrorActionPreference = 'Stop'
$base = 'https://serzhyale.github.io/Tourist_Android/'
$site = Join-Path $Root 'site'
if (-not (Test-Path -LiteralPath $site -PathType Container)) { Write-Host "site-check: COULD NOT VERIFY (no site/ under $Root)"; exit 2 }

$findings = New-Object System.Collections.Generic.List[string]
$utf8 = New-Object System.Text.UTF8Encoding($false)
$pages = @(Get-ChildItem -LiteralPath $site -Filter '*.html' -File | Sort-Object Name)
if ($pages.Count -eq 0) { Write-Host 'site-check: COULD NOT VERIFY (no pages)'; exit 2 }
$locales = @{ 'privacy.html' = 'en'; 'privacy-ru.html' = 'ru'; 'privacy-uk.html' = 'uk' }

foreach ($page in $pages) {
    $name = $page.Name
    $html = [IO.File]::ReadAllText($page.FullName, $utf8)
    $url = $base + $name
    if (([regex]::Matches($html, '<h1[\s>]')).Count -ne 1) { $findings.Add("$name`: not exactly one <h1>") }
    if ($html -notmatch [regex]::Escape("<link rel=`"canonical`" href=`"$url`">")) { $findings.Add("$name`: canonical is not $url") }
    if ($html -notmatch [regex]::Escape("<meta property=`"og:url`" content=`"$url`">")) { $findings.Add("$name`: og:url is not $url") }
    if ($locales.ContainsKey($name)) {
        foreach ($pair in @(@('en', 'privacy.html'), @('ru', 'privacy-ru.html'), @('uk', 'privacy-uk.html'), @('x-default', 'privacy.html'))) {
            $link = "<link rel=`"alternate`" hreflang=`"$($pair[0])`" href=`"$base$($pair[1])`">"
            if (-not $html.Contains($link)) { $findings.Add("$name`: missing hreflang $($pair[0])") }
        }
        if ($html -notmatch "<html lang=`"$($locales[$name])`"") { $findings.Add("$name`: html lang is not $($locales[$name])") }
    }
    foreach ($m in [regex]::Matches($html, '\s(?:href|src)="([^"]+)"')) {
        $ref = $m.Groups[1].Value
        if ($ref -match '^(https?:|mailto:|#)') { continue }
        $path = Join-Path $site (($ref -split '[#?]')[0])
        if (-not (Test-Path -LiteralPath $path)) { $findings.Add("$name`: $ref does not resolve under site/") }
    }
    $text = [regex]::Replace($html, '<script[\s\S]*?</script>|<style[\s\S]*?</style>|<[^>]+>', ' ')
    if ($text.Contains('...') -or $text.Contains([string][char]0x2026)) { $findings.Add("$name`: three dots in the text (house style is ..)") }
}

$sitemapPath = Join-Path $site 'sitemap.xml'
if (Test-Path -LiteralPath $sitemapPath) {
    $listed = @([regex]::Matches([IO.File]::ReadAllText($sitemapPath, $utf8), '<loc>([^<]+)</loc>') | ForEach-Object { $_.Groups[1].Value })
    $expected = @($pages | ForEach-Object { $base + $_.Name })
    foreach ($u in $expected) { if ($listed -notcontains $u) { $findings.Add("sitemap.xml: $u is not listed") } }
    foreach ($u in $listed) { if ($expected -notcontains $u) { $findings.Add("sitemap.xml: $u is not a page") } }
}
else { $findings.Add('sitemap.xml: missing') }
$robotsPath = Join-Path $site 'robots.txt'
if (-not (Test-Path -LiteralPath $robotsPath) -or -not ([IO.File]::ReadAllText($robotsPath, $utf8).Contains("Sitemap: ${base}sitemap.xml"))) {
    $findings.Add('robots.txt: missing or does not name the sitemap')
}

$kit = Join-Path $site 'assets/sza-kit.css'
$provenance = Join-Path $site 'assets/PROVENANCE.txt'
if ((Test-Path -LiteralPath $kit) -and (Test-Path -LiteralPath $provenance)) {
    $recorded = [regex]::Match([IO.File]::ReadAllText($provenance, $utf8), 'sza-kit\.css sha256 ([0-9a-f]{64})').Groups[1].Value
    $actual = (Get-FileHash -LiteralPath $kit -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($recorded -ne $actual) { $findings.Add("assets/sza-kit.css: sha256 $actual, PROVENANCE.txt records '$recorded'") }
}
else { $findings.Add('assets/sza-kit.css or assets/PROVENANCE.txt: missing') }

foreach ($f in $findings) { Write-Host "FAIL: $f" }
if ($findings.Count) { Write-Host "site-check: FAIL ($($findings.Count) finding(s) in $($pages.Count) page(s))"; exit 1 }
Write-Host "site-check: PASS ($($pages.Count) page(s), sitemap, robots, kit hash)"
exit 0
