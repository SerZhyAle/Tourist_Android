#requires -Version 7.0
<#
.SYNOPSIS
    Checks the hosted pages under site/ before they are published (S0026 W1-W6, owner's order of 2026-11-13).
.DESCRIPTION
    The page groups, locales and addresses come from tools/site/deck.json, the one list that the generator
    (tools/site/build-site.ps1) also renders from. Held here, because nothing else reads the rendered pages:
      - every site/**/*.html page belongs to a deck group, and every deck page exists;
      - each page has one <h1>, a canonical link and og:url equal to its own published address (the directory form for
        a landing), the reciprocal hreflang set (en, ru, uk and x-default), the matching html lang, a share image, and
        JSON-LD that parses and carries no aggregateRating or review;
      - every relative href and src of every page resolves to a file under site/ (a directory link to its index.html);
      - the only mailto address is the contact of SITE-FAMILY-MAP rule 3, no page loads a third-party script, font or
        analytics origin, and none carries a keywords meta or FAQPage / HowTo markup (S0012 section 2.1);
      - the text of every page holds house style (no three dots, no long dash) and the shipped-fact rule of invariant 17
        (no soon / planned / beta wording in en, ru or uk);
      - each generated landing shows the application icon and the name of the deck, and its footer grid links every
        family tool of the deck;
      - sitemap.xml lists every deck page and nothing else, and robots.txt names the sitemap;
      - site/assets/sza-kit.css has the sha256 that site/assets/PROVENANCE.txt records;
      - tools/site/build-site.ps1 -Check finds no render target stale.
    Runs on the Windows runner of .github/workflows/pages.yml and locally between releases.
.NOTES
    Exit codes: 0 PASS, 1 FAIL, 2 could not verify. The verdict line is the last line written.
#>
[CmdletBinding()]
param(
    [string]$Root = (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
)
$ErrorActionPreference = 'Stop'
$utf8 = New-Object System.Text.UTF8Encoding($false)
$site = Join-Path $Root 'site'
$deckPath = Join-Path $PSScriptRoot 'deck.json'
if (-not (Test-Path -LiteralPath $site -PathType Container)) { Write-Host "site-check: COULD NOT VERIFY (no site/ under $Root)"; exit 2 }
if (-not (Test-Path -LiteralPath $deckPath)) { Write-Host 'site-check: COULD NOT VERIFY (no tools/site/deck.json)'; exit 2 }
$deck = [IO.File]::ReadAllText($deckPath, $utf8) | ConvertFrom-Json
$base = $deck.site.base
$locKeys = @('en', 'ru', 'uk')

function Get-PageUrl([string]$Path) {
    if ($Path -eq 'index.html') { return $base }
    if ($Path.EndsWith('/index.html')) { return $base + $Path.Substring(0, $Path.Length - 'index.html'.Length) }
    return $base + $Path
}

$findings = New-Object System.Collections.Generic.List[string]
$expected = @{}   # relative path -> @{ Group; Loc }
foreach ($g in $deck.pages) { foreach ($k in $locKeys) { $expected[[string]$g.$k] = @{ Group = $g.id; Loc = $k } } }

$pages = @(Get-ChildItem -LiteralPath $site -Filter '*.html' -File -Recurse | Sort-Object FullName)
if ($pages.Count -eq 0) { Write-Host 'site-check: COULD NOT VERIFY (no pages)'; exit 2 }
$onDisk = @{}
foreach ($p in $pages) { $onDisk[[IO.Path]::GetRelativePath($site, $p.FullName).Replace('\', '/')] = $p }
foreach ($rel in $onDisk.Keys) { if (-not $expected.ContainsKey($rel)) { $findings.Add("${rel}: a page that no deck group names") } }
foreach ($rel in $expected.Keys) { if (-not $onDisk.ContainsKey($rel)) { $findings.Add("${rel}: named by the deck, missing under site/") } }

# Each hit of the canon grep is read: "as soon as" is a time word, not a promise, so it is the one exclusion; the
# Cyrillic words are whole words (скорость is a speed, not a promise).
# The control names a guide writes in bold are strings of the app in that locale (SITE-CHECKLIST section C; a renamed
# button must be renamed here). Read from the phone and watch string tables.
$resDir = @{ en = 'values'; ru = 'values-ru'; uk = 'values-uk' }
$appStrings = @{}
foreach ($k in $locKeys) {
    $set = New-Object System.Collections.Generic.HashSet[string] ([StringComparer]::OrdinalIgnoreCase)
    foreach ($mod in @('phone', 'wear')) {
        $f = Join-Path $Root "$mod/src/main/res/$($resDir[$k])/strings.xml"
        if (Test-Path -LiteralPath $f) {
            [xml]$x = [IO.File]::ReadAllText($f, $utf8)
            foreach ($s in $x.resources.string) { $v = ([string]$s.InnerText).Trim(); if ($v) { [void]$set.Add($v) } }
        }
    }
    if ($set.Count -eq 0) { Write-Host "site-check: COULD NOT VERIFY (no app strings for $k)"; exit 2 }
    $appStrings[$k] = $set
}
$hedge = '(?i)(?<!\bas )\bsoon\b|\b(planned|upcoming|not yet|in development|will be added|experimental|beta)\b|\b(скоро|планируется|ещё не|бета|незабаром|планується)\b'
$allText = New-Object System.Text.StringBuilder

foreach ($rel in ($onDisk.Keys | Sort-Object)) {
    if (-not $expected.ContainsKey($rel)) { continue }
    $group = $expected[$rel].Group
    $loc = $expected[$rel].Loc
    $html = [IO.File]::ReadAllText($onDisk[$rel].FullName, $utf8)
    $url = Get-PageUrl $rel
    if (([regex]::Matches($html, '<h1[\s>]')).Count -ne 1) { $findings.Add("${rel}: not exactly one <h1>") }
    if (-not $html.Contains("<link rel=`"canonical`" href=`"$url`">")) { $findings.Add("${rel}: canonical is not $url") }
    if (-not $html.Contains("<meta property=`"og:url`" content=`"$url`">")) { $findings.Add("${rel}: og:url is not $url") }
    if ($html -notmatch "<html lang=`"$loc`"") { $findings.Add("${rel}: html lang is not $loc") }
    $gp = $deck.pages | Where-Object { $_.id -eq $group }
    foreach ($k in $locKeys) {
        if (-not $html.Contains("<link rel=`"alternate`" hreflang=`"$k`" href=`"$(Get-PageUrl ([string]$gp.$k))`">")) { $findings.Add("${rel}: missing hreflang $k") }
    }
    if (-not $html.Contains("<link rel=`"alternate`" hreflang=`"x-default`" href=`"$(Get-PageUrl ([string]$gp.en))`">")) { $findings.Add("${rel}: missing hreflang x-default") }
    if ($html -notmatch '<meta property="og:image" content="https://[^"]+">') { $findings.Add("${rel}: no og:image") }
    # DOC-EXTERNAL-QUALITY rule 5: a title under 60 and a description under 160 characters.
    $tm = [regex]::Match($html, '<title>([^<]*)</title>')
    if (-not $tm.Success -or [System.Net.WebUtility]::HtmlDecode($tm.Groups[1].Value).Length -ge 60) { $findings.Add("${rel}: title missing or 60 characters or longer") }
    $dm = [regex]::Match($html, '<meta name="description" content="([^"]*)">')
    if (-not $dm.Success -or [System.Net.WebUtility]::HtmlDecode($dm.Groups[1].Value).Length -ge 160) { $findings.Add("${rel}: description missing or 160 characters or longer") }
    if ($group -ne 'privacy') {
        foreach ($m in [regex]::Matches($html, '<strong>([^<]+)</strong>')) {
            if (-not $appStrings[$loc].Contains([System.Net.WebUtility]::HtmlDecode($m.Groups[1].Value))) { $findings.Add("${rel}: bold name '$($m.Groups[1].Value)' is not a string of the app in $loc") }
        }
    }

    foreach ($m in [regex]::Matches($html, '<script type="application/ld\+json">([\s\S]*?)</script>')) {
        try { $null = $m.Groups[1].Value | ConvertFrom-Json }
        catch { $findings.Add("${rel}: JSON-LD does not parse") }
        if ($m.Groups[1].Value -match 'aggregateRating|"review"|"Review"') { $findings.Add("${rel}: JSON-LD carries a rating or review") }
    }
    if ($html -notmatch 'application/ld\+json') { $findings.Add("${rel}: no JSON-LD") }

    $dir = Split-Path -Parent $onDisk[$rel].FullName
    foreach ($m in [regex]::Matches($html, '\s(?:href|src)="([^"]+)"')) {
        $ref = $m.Groups[1].Value
        if ($ref -match '^(https?:|mailto:|#)') {
            if ($ref -match '^https?:' -and $ref -notmatch '^https://(serzhyale\.github\.io|sza\.od\.ua|github\.com|open-meteo\.com)(/|$)') { $findings.Add("${rel}: link to an origin the privacy page does not name: $ref") }
            continue
        }
        $target = Join-Path $dir (($ref -split '[#?]')[0])
        if (Test-Path -LiteralPath $target -PathType Container) { $target = Join-Path $target 'index.html' }
        if (-not (Test-Path -LiteralPath $target)) { $findings.Add("${rel}: $ref does not resolve under site/") }
    }
    foreach ($m in [regex]::Matches($html, 'mailto:([^"]+)"')) {
        if ($m.Groups[1].Value -ne $deck.site.mail) { $findings.Add("${rel}: mailto:$($m.Groups[1].Value) is not the contact $($deck.site.mail)") }
    }
    if ($html -match '<script[^>]+src=|<link[^>]+href="https?://fonts\.|google-analytics|googletagmanager|gtag\(|utm_') { $findings.Add("${rel}: a third-party script, font or tracking marker") }
    if ($html -match '<meta name="keywords"|FAQPage|HowTo') { $findings.Add("${rel}: keywords meta or FAQPage / HowTo markup") }

    $text = [regex]::Replace($html, '<script[\s\S]*?</script>|<style[\s\S]*?</style>|<!--[\s\S]*?-->|<[^>]+>', ' ')
    $text = [System.Net.WebUtility]::HtmlDecode($text)
    if ($text.Contains('...') -or $text.Contains([string][char]0x2026)) { $findings.Add("${rel}: three dots in the text (house style is ..)") }
    if ($text.Contains([string][char]0x2014) -or $text.Contains([string][char]0x2013)) { $findings.Add("${rel}: a long dash in the text (house style is a plain hyphen)") }
    foreach ($m in [regex]::Matches($text, $hedge)) { $findings.Add("${rel}: shipped-fact rule, hedge wording '$($m.Value)'") }
    [void]$allText.AppendLine($text)

    if ($group -eq 'landing') {
        if ($html -notmatch '<img class="product-icon" src="[^"]*assets/icon-512\.png"[^>]* alt="">') { $findings.Add("${rel}: no application icon in the identity block") }
        if ($html -notmatch "<p class=`"product-name`">$([regex]::Escape($deck.site.brand))</p>") { $findings.Add("${rel}: identity block name is not '$($deck.site.brand)'") }
        foreach ($f in $deck.family) {
            if (-not $html.Contains("<a href=`"$($f.url)`" rel=`"noopener`"><b>")) { $findings.Add("${rel}: footer grid lacks $($f.url)") }
        }
        if (([regex]::Matches($html, '<div class="tools-grid">[\s\S]*?</div>')).Count -ne 1) { $findings.Add("${rel}: not exactly one footer grid") }
    }
}

$sitemapPath = Join-Path $site 'sitemap.xml'
if (Test-Path -LiteralPath $sitemapPath) {
    $listed = @([regex]::Matches([IO.File]::ReadAllText($sitemapPath, $utf8), '<loc>([^<]+)</loc>') | ForEach-Object { $_.Groups[1].Value })
    $want = @($expected.Keys | ForEach-Object { Get-PageUrl $_ })
    foreach ($u in $want) { if ($listed -notcontains $u) { $findings.Add("sitemap.xml: $u is not listed") } }
    foreach ($u in $listed) { if ($want -notcontains $u) { $findings.Add("sitemap.xml: $u is not a page") } }
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

$generator = Join-Path $PSScriptRoot 'build-site.ps1'
$genOut = & pwsh -NoProfile -File $generator -Check -Root $Root 2>&1
$genExit = $LASTEXITCODE
if ($genExit -ne 0) {
    foreach ($line in @($genOut)) { if ("$line".Trim()) { $findings.Add("generator: $line") } }
}

foreach ($f in $findings) { Write-Host "FAIL: $f" }
if ($findings.Count) { Write-Host "site-check: FAIL ($($findings.Count) finding(s) in $($pages.Count) page(s))"; exit 1 }
Write-Host "site-check: PASS ($($pages.Count) pages, $($expected.Count) deck pages, sitemap, kit hash and generator staleness)"
exit 0
