#requires -Version 7.0
<#
.SYNOPSIS
    Renders the product site pages of Tourist from the copy deck (S0026 W1, W2, W4, W5, W6).
.DESCRIPTION
    tools/site/deck.json is the only source of the landing, the guide and the support page in en, ru and uk. This script
    renders them and the sitemap into site/, copies the proof captures (docs/play/screenshots) and the share image
    (docs/github) into site/assets/, and reads the minimum platform levels from gradle/libs.versions.toml so a page can
    never state a level the build does not have. The served files are render targets: never edit them by hand, edit the
    deck or this script and run it again.
      build-site.ps1            write every render target
      build-site.ps1 -Check     write nothing; fail when a render target on disk differs from what the deck renders
    The three privacy pages are hand-written (owner's order of 2026-11-13) and are not touched; they only appear in the
    sitemap and in the footers' links. The kit (site/assets/sza-kit.css) and the page layer (site/assets/policy.css) are
    hand-maintained inputs too.
.NOTES
    Exit codes: 0 written, or up to date under -Check; 1 stale under -Check; 2 could not verify (an input is missing or a
    deck placeholder is unknown). The verdict line is the last line written.
#>
[CmdletBinding()]
param(
    [switch]$Check,
    [string]$Root = (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
)
$ErrorActionPreference = 'Stop'
$utf8 = New-Object System.Text.UTF8Encoding($false)

function Stop-Verify([string]$Message) {
    Write-Host "build-site: COULD NOT VERIFY ($Message)"
    exit 2
}

$deckPath = Join-Path $PSScriptRoot 'deck.json'
$tomlPath = Join-Path $Root 'gradle/libs.versions.toml'
foreach ($p in @($deckPath, $tomlPath)) { if (-not (Test-Path -LiteralPath $p)) { Stop-Verify "missing $p" } }
$deck = [IO.File]::ReadAllText($deckPath, $utf8) | ConvertFrom-Json
$base = $deck.site.base
$locKeys = @('en', 'ru', 'uk')
$siteDir = Join-Path $Root 'site'

# ---- minimum platform levels, read from the build ----
$toml = [IO.File]::ReadAllText($tomlPath, $utf8)
function Get-Level([string]$Name) {
    $m = [regex]::Match($toml, "(?m)^$([regex]::Escape($Name))\s*=\s*""(\d+)""")
    if (-not $m.Success) { Stop-Verify "$Name not found in libs.versions.toml" }
    return [int]$m.Groups[1].Value
}
$androidName = @{ 26 = '8.0'; 27 = '8.1'; 28 = '9'; 29 = '10'; 30 = '11'; 31 = '12'; 32 = '12L'; 33 = '13'; 34 = '14'; 35 = '15'; 36 = '16'; 37 = '17' }
function Format-Api([int]$Level, [string]$Fmt) {
    if (-not $androidName.ContainsKey($Level)) { Stop-Verify "no Android name for API level $Level" }
    return $Fmt.Replace('{0}', $androidName[$Level]).Replace('{1}', [string]$Level)
}
$levels = @{ minPhone = Get-Level 'min-sdk-phone'; minWear = Get-Level 'min-sdk-wear'; minFace = Get-Level 'min-sdk-watchface' }

# ---- addresses ----
function Get-PagePath([string]$Group, [string]$Loc) {
    $g = $deck.pages | Where-Object { $_.id -eq $Group }
    return [string]$g.$Loc
}
function Get-PageUrl([string]$Path) {
    if ($Path -eq 'index.html') { return $base }
    if ($Path.EndsWith('/index.html')) { return $base + $Path.Substring(0, $Path.Length - 'index.html'.Length) }
    return $base + $Path
}
function Get-Rel([string]$From, [string]$To) {
    $fd = Split-Path -Parent $From
    if (-not $fd) { $fd = '.' }
    return [IO.Path]::GetRelativePath($fd, $To).Replace('\', '/')
}
function Get-LandingHref([string]$From, [string]$Loc) {
    $target = if ($Loc -eq 'en') { '.' } else { $Loc }
    $r = Get-Rel $From $target
    if ($r -eq '.') { return './' }
    return "$r/"
}
function Get-GroupHref([string]$From, [string]$Group, [string]$Loc) {
    if ($Group -eq 'landing') { return Get-LandingHref $From $Loc }
    return Get-Rel $From (Get-PagePath $Group $Loc)
}
function Enc([string]$s) { return [System.Net.WebUtility]::HtmlEncode($s) }

# ---- placeholders of the deck ----
function Expand-Deck([string]$Text, [string]$From, [string]$Loc) {
    $ui = $deck.locales.$Loc.ui
    $map = @{
        'home'    = { Get-LandingHref $From $Loc }
        'guide'   = { Get-GroupHref $From 'guide' $Loc }
        'support' = { Get-GroupHref $From 'support' $Loc }
        'privacy' = { Get-GroupHref $From 'privacy' $Loc }
        'repo'    = { $deck.site.repo }
        'issues'  = { $deck.site.issues }
        'hub'     = { $deck.site.hub }
        'github'  = { $deck.site.github }
        'mail'    = { $deck.site.mail }
        'minPhone' = { Format-Api $levels.minPhone $ui.apiFmt }
        'minWear'  = { Format-Api $levels.minWear $ui.apiFmt }
        'minFace'  = { Format-Api $levels.minFace $ui.apiFmt }
    }
    return [regex]::Replace($Text, '\{\{(\w+)\}\}', {
            param($m)
            $k = $m.Groups[1].Value
            if (-not $map.ContainsKey($k)) { Stop-Verify "unknown deck placeholder {{$k}}" }
            return [string](& $map[$k])
        })
}

# ---- templates ----
$shellTpl = @'
<!doctype html>
<html lang="@@LANG@@" dir="ltr" data-theme="dark">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>@@TITLE@@</title>
  <meta name="description" content="@@DESC@@">
  <meta name="theme-color" content="#0a0f0a">
  <link rel="canonical" href="@@CANON@@">
@@ALTERNATES@@
  <meta property="og:type" content="website">
  <meta property="og:site_name" content="Tourist">
  <meta property="og:title" content="@@TITLE@@">
  <meta property="og:description" content="@@DESC@@">
  <meta property="og:url" content="@@CANON@@">
  <meta property="og:image" content="@@IMAGE@@">
  <meta property="og:image:width" content="1280">
  <meta property="og:image:height" content="640">
  <meta property="og:image:alt" content="@@IMAGEALT@@">
  <meta property="og:locale" content="@@OGLOCALE@@">
@@OGALTERNATES@@
  <meta name="twitter:card" content="summary_large_image">
  <meta name="twitter:image" content="@@IMAGE@@">
  <meta name="twitter:title" content="@@TITLE@@">
  <meta name="twitter:description" content="@@DESC@@">
  <script type="application/ld+json">@@JSONLD@@</script>
  <!-- PAGE-STYLE section 7: theme and the touch profile are resolved before the first stylesheet, so nothing flashes.
       sza-theme holds exactly dark or light and is shared with every page of this origin. -->
  <script>
(function(){try{
  var h=document.documentElement,t=localStorage.getItem('sza-theme');
  if(t!=='dark'&&t!=='light')t=matchMedia('(prefers-color-scheme: light)').matches?'light':'dark';
  h.setAttribute('data-theme',t);
  h.setAttribute('data-ui-profile',matchMedia('(pointer: coarse)').matches?'touch':'pointer');
}catch(e){}})();
  </script>
@@FORWARD@@
  <link rel="stylesheet" href="@@PREFIX@@assets/sza-kit.css">
  <link rel="stylesheet" href="@@PREFIX@@assets/policy.css">
</head>
<body>
<a class="skip" href="#main">@@SKIP@@</a>
<div class="bg-blobs"><div class="blob blob-1"></div><div class="blob blob-2"></div><div class="blob blob-3"></div></div>
@@HEADER@@
@@MAIN@@
@@FOOTER@@
<button type="button" class="to-top" id="toTop" aria-label="@@TOTOP@@"><span class="gi gi-top" aria-hidden="true"></span></button>
<script>
(function(){
  var h=document.documentElement,urls=@@URLS@@,labels=@@LABELS@@;
  var tb=document.getElementById('themeBtn'),meta=document.querySelector('meta[name="theme-color"]');
  function sync(){var l=h.getAttribute('data-theme')==='light';tb.setAttribute('aria-label',labels[l?'light':'dark']);if(meta)meta.setAttribute('content',l?'#eef3ea':'#0a0f0a')}
  sync();
  tb.addEventListener('click',function(){
    var n=h.getAttribute('data-theme')==='light'?'dark':'light';
    h.setAttribute('data-theme',n);
    try{localStorage.setItem('sza-theme',n)}catch(e){}
    sync();
  });
  Array.prototype.forEach.call(document.querySelectorAll('.seg button[data-lang]'),function(b){
    b.addEventListener('click',function(){
      var k=b.getAttribute('data-lang');
      try{localStorage.setItem('sza-lang',k)}catch(e){}
      location.href=urls[k];
    });
  });
  Array.prototype.forEach.call(document.querySelectorAll('[data-expand]'),function(b){
    b.addEventListener('click',function(){
      var open=b.getAttribute('data-expand')==='all';
      Array.prototype.forEach.call(document.querySelectorAll('details.sec'),function(d){d.open=open});
    });
  });
  function openFromHash(){
    var id=location.hash.slice(1);if(!id)return;
    var el=document.getElementById(id);
    if(el&&el.tagName==='DETAILS'){el.open=true;el.scrollIntoView()}
  }
  openFromHash();addEventListener('hashchange',openFromHash);
  var top=document.getElementById('toTop');
  addEventListener('scroll',function(){top.classList.toggle('show',scrollY>600)},{passive:true});
  top.addEventListener('click',function(){scrollTo({top:0})});
})();
</script>
</body>
</html>
'@

# PAGE-STYLE 3.7 and 3.9 item 3: the English landing at the root forwards only on an explicit choice (a valid locale
# query or a stored sza-lang other than en), never on navigator.language.
$forwardTpl = @'
  <script>
(function(){try{
  var q=(location.search.match(/[?&]lang=(ru|en|uk|ua)\b/i)||location.search.match(/[?&]l=(ru|en|uk|ua)\b/i)||[])[1];
  var l=q?q.toLowerCase():localStorage.getItem('sza-lang');
  if(l==='uk')l='ua';
  if(l==='ru'||l==='ua')location.replace(l==='ru'?'ru/':'uk/');
}catch(e){}})();
  </script>
'@

# ---- rendering ----
$outputs = [ordered]@{}
$copies = New-Object System.Collections.Generic.List[object]

function Get-Alternates([string]$Group) {
    $lines = New-Object System.Collections.Generic.List[string]
    foreach ($k in $locKeys) { $lines.Add("  <link rel=""alternate"" hreflang=""$k"" href=""$(Get-PageUrl (Get-PagePath $Group $k))"">") }
    $lines.Add("  <link rel=""alternate"" hreflang=""x-default"" href=""$(Get-PageUrl (Get-PagePath $Group 'en'))"">")
    return ($lines -join "`n")
}

function New-Header([string]$From, [string]$Loc, [string]$Group) {
    $ui = $deck.locales.$Loc.ui
    $prefix = if ($Loc -eq 'en') { '' } else { '../' }
    $nav = New-Object System.Collections.Generic.List[string]
    foreach ($g in @('guide', 'support', 'privacy')) {
        $cur = if ($g -eq $Group) { ' aria-current="page"' } else { '' }
        $nav.Add("<a href=""$(Get-GroupHref $From $g $Loc)""$cur>$(Enc $ui.$g)</a>")
    }
    $seg = New-Object System.Collections.Generic.List[string]
    foreach ($pair in @(@('ru', 'RU'), @('en', 'EN'), @('ua', 'UA'))) {
        $lk = if ($pair[0] -eq 'ua') { 'uk' } else { $pair[0] }
        $pressed = if ($lk -eq $Loc) { 'true' } else { 'false' }
        $seg.Add("    <button type=""button"" data-lang=""$($pair[0])"" aria-pressed=""$pressed"">$($pair[1])</button>")
    }
    $brandCur = if ($Group -eq 'landing') { ' aria-current="page"' } else { '' }
    $get = if ($Group -eq 'landing') { "`n  <a class=""btn btn-primary"" href=""#get"">$(Enc $ui.getIt)</a>" } else { '' }
    return @"
<header class="site-header">
  <a class="brand" href="$(Get-LandingHref $From $Loc)"$brandCur>$(Enc $deck.site.brand)<span class="dot">.</span></a>
  <nav class="site-nav" aria-label="$(Enc $ui.nav)">$($nav -join '')</nav>
  <div class="seg" role="group" aria-label="$(Enc $ui.language)">
$($seg -join "`n")
  </div>
  <button type="button" class="theme-btn" id="themeBtn" aria-label="$(Enc $ui.toLight)"><span class="gi gi-theme" aria-hidden="true"></span><span>$(Enc $ui.theme)</span></button>$get
</header>
"@
}

function New-Footer([string]$From, [string]$Loc, [bool]$Grid) {
    $ui = $deck.locales.$Loc.ui
    $s = $deck.site
    $gridHtml = ''
    if ($Grid) {
        $items = foreach ($f in $deck.family) {
            "<a href=""$($f.url)"" rel=""noopener""><b>$(Enc $f.tool)</b><span>$(Enc $f.type.$Loc)</span></a>"
        }
        $gridHtml = "  <h2 class=""footer-h"">$(Enc $ui.moreTools)</h2>`n  <div class=""tools-grid"">`n    $($items -join "`n    ")`n  </div>`n"
    }
    $nav = @(
        "<a href=""$(Get-LandingHref $From $Loc)"">$(Enc $ui.home)</a>"
        "<a href=""$(Get-GroupHref $From 'guide' $Loc)"">$(Enc $ui.guide)</a>"
        "<a href=""$(Get-GroupHref $From 'support' $Loc)"">$(Enc $ui.support)</a>"
        "<a href=""$(Get-GroupHref $From 'privacy' $Loc)"">$(Enc $ui.privacy)</a>"
    ) -join ' &middot; '
    return @"
<footer class="site-footer"><div class="container">
$gridHtml  <nav class="footer-nav" aria-label="$(Enc $ui.nav)">$nav</nav>
  <div class="footer-bottom">
    <span>&copy; $($s.year) <a href="$($s.hub)" rel="noopener">$(Enc $s.author)</a></span>
    <span><a href="$($s.hub)" rel="noopener">sza.od.ua</a> &middot; <a href="$($s.github)" rel="noopener">GitHub</a> &middot; <a href="mailto:$($s.mail)">$($s.mail)</a></span>
  </div>
</div></footer>
"@
}

function ConvertTo-LdJson($Object) { return ($Object | ConvertTo-Json -Compress -Depth 8) }

function New-Page {
    param([string]$Group, [string]$Loc, [string]$Title, [string]$Description, [string]$Main, [object]$JsonLd, [bool]$Grid, [bool]$Forward)
    $path = Get-PagePath $Group $Loc
    $L = $deck.locales.$Loc
    $ui = $L.ui
    $prefix = if ($Loc -eq 'en') { '' } else { '../' }
    $urls = [ordered]@{}
    foreach ($pair in @(@('ru', 'ru'), @('en', 'en'), @('ua', 'uk'))) { $urls[$pair[0]] = Get-GroupHref $path $Group $pair[1] }
    $og = foreach ($k in $locKeys) { if ($k -ne $Loc) { "  <meta property=""og:locale:alternate"" content=""$($deck.locales.$k.ogLocale)"">" } }
    $imageAlt = "$($deck.site.brand) - $($L.landing.kicker)"
    $map = @{
        LANG = $L.lang; TITLE = (Enc $Title); DESC = (Enc $Description); CANON = (Get-PageUrl $path)
        ALTERNATES = (Get-Alternates $Group); OGLOCALE = $L.ogLocale; OGALTERNATES = ($og -join "`n")
        IMAGE = "${base}assets/social-1280x640.png"; IMAGEALT = (Enc $imageAlt); JSONLD = (ConvertTo-LdJson $JsonLd)
        FORWARD = $(if ($Forward) { $forwardTpl.TrimEnd() } else { '' }); PREFIX = $prefix; SKIP = (Enc $ui.skip)
        HEADER = (New-Header $path $Loc $Group); MAIN = $Main; FOOTER = (New-Footer $path $Loc $Grid)
        TOTOP = (Enc $ui.toTop)
        URLS = ($urls | ConvertTo-Json -Compress)
        LABELS = ([ordered]@{ light = $ui.toLight; dark = $ui.toDark } | ConvertTo-Json -Compress)
    }
    $html = $shellTpl
    foreach ($k in $map.Keys) { $html = $html.Replace("@@$k@@", [string]$map[$k]) }
    $html = [regex]::Replace($html, '(?m)^[ \t]*\r?\n', '')
    $outputs[$path] = $html.Replace("`r`n", "`n")
}

function New-Blocks([object[]]$Blocks, [string]$From, [string]$Loc) {
    $out = New-Object System.Collections.Generic.List[string]
    foreach ($b in $Blocks) {
        switch ($b.t) {
            'h2' { $out.Add("<h2 id=""$($b.id)"">$(Expand-Deck $b.x $From $Loc)</h2>") }
            'p' { $out.Add("<p>$(Expand-Deck $b.x $From $Loc)</p>") }
            'ul' { $out.Add("<ul>`n" + ((@($b.x) | ForEach-Object { "<li>$(Expand-Deck $_ $From $Loc)</li>" }) -join "`n") + "`n</ul>") }
            'ol' { $out.Add("<ol>`n" + ((@($b.x) | ForEach-Object { "<li>$(Expand-Deck $_ $From $Loc)</li>" }) -join "`n") + "`n</ol>") }
            'note' { $out.Add("<div class=""note""><span class=""tag"">$(Enc $b.tag)</span><p>$(Expand-Deck $b.x $From $Loc)</p></div>") }
            default { Stop-Verify "unknown block type $($b.t)" }
        }
    }
    return ($out -join "`n")
}

function New-Landing([string]$Loc) {
    $group = 'landing'
    $path = Get-PagePath $group $Loc
    $L = $deck.locales.$Loc
    $ui = $L.ui
    $d = $L.landing
    $prefix = if ($Loc -eq 'en') { '' } else { '../' }
    $x = { param($t) Expand-Deck $t $path $Loc }

    $scen = (@($d.scenarios) | ForEach-Object { "      <article class=""card""><h3>$(Enc $_.t)</h3><p>$(& $x $_.x)</p></article>" }) -join "`n"
    $shots = (@($d.shots) | ForEach-Object {
            "      <li><img src=""${prefix}assets/shots/$Loc/$($_.key).png"" width=""$($_.w)"" height=""$($_.h)"" alt=""$(Enc $_.alt)"" loading=""lazy""></li>"
            $copies.Add(@{ From = "docs/play/screenshots/$($_.dir)/$Loc/$($_.src)"; To = "site/assets/shots/$Loc/$($_.key).png" })
        }) -join "`n"
    $steps = (@($d.steps) | ForEach-Object { "      <li>$(& $x $_)</li>" }) -join "`n"
    $n = 0
    $feat = (@($d.features) | ForEach-Object {
            $n++
            "    <details class=""sec"" id=""$($_.id)""><summary><span class=""secnum"">$('{0:00}' -f $n)</span> $(Enc $_.t)</summary>`n$(& $x $_.x)`n    </details>"
        }) -join "`n"
    $det = (@($d.details) | ForEach-Object {
            $n++
            "    <details class=""sec"" id=""$($_.id)""><summary><span class=""secnum"">$('{0:00}' -f $n)</span> $(Enc $_.t)</summary>`n$(& $x $_.x)`n    </details>"
        }) -join "`n"
    $expand = "    <div class=""btn-row"">`n      <button type=""button"" class=""btn btn-ghost"" data-expand=""all"">$(Enc $ui.expandAll)</button>`n      <button type=""button"" class=""btn btn-ghost"" data-expand=""none"">$(Enc $ui.collapseAll)</button>`n    </div>"

    $main = @"
<main class="container" id="main">
  <noscript><p class="lang-links"><a href="$(Get-LandingHref $path en)" hreflang="en">English</a> &middot; <a href="$(Get-LandingHref $path ru)" hreflang="ru">Русский</a> &middot; <a href="$(Get-LandingHref $path uk)" hreflang="uk">Українська</a></p></noscript>
  <section class="hero" aria-labelledby="top">
    <div class="identity">
      <img class="product-icon" src="${prefix}assets/icon-512.png" width="104" height="104" alt="">
      <div>
        <p class="product-name">$(Enc $deck.site.brand)</p>
        <p class="kicker">$(Enc $d.kicker)</p>
      </div>
    </div>
    <h1 id="top">$(Enc $d.h1)</h1>
    <p class="lead">$(Enc $d.lead)</p>
  </section>
  <section class="section" aria-label="$(Enc $d.kicker)">
    <p class="whatfor">$(Enc $d.whatfor)</p>
  </section>
  <section class="section" aria-labelledby="proof">
    <h2 id="proof">$(Enc $d.proofTitle)</h2>
    <div class="grid">
$scen
    </div>
    <ul class="shots" aria-label="$(Enc $ui.shotsLabel)">
$shots
    </ul>
  </section>
  <section class="get" id="get" aria-labelledby="get-h">
    <h2 id="get-h">$(Enc $d.getTitle)</h2>
    <p class="whatfor">$(Enc $d.getText)</p>
    <div class="btn-row channels">
      <a class="btn btn-primary" rel="noopener" href="$($deck.site.repo)">$(Enc $d.sourceLabel)</a>
      <a class="btn btn-ghost" href="$(Get-GroupHref $path 'guide' $Loc)">$(Enc $d.guidesLabel)</a>
    </div>
    <h3>$(Enc $d.stepsTitle)</h3>
    <ol class="quickstart">
$steps
    </ol>
  </section>
  <section class="section" aria-labelledby="features">
    <h2 id="features">$(Enc $d.featuresTitle)</h2>
$expand
$feat
  </section>
  <section class="section" aria-labelledby="guides">
    <h2 id="guides">$(Enc $d.guidesTitle)</h2>
$(& $x $d.guidesText)
  </section>
  <section class="section" aria-labelledby="details">
    <h2 id="details">$(Enc $d.detailsTitle)</h2>
$det
  </section>
</main>
"@

    $ld = [ordered]@{
        '@context'            = 'https://schema.org'
        '@type'               = 'MobileApplication'
        'name'                = $deck.site.brand
        'alternateName'       = $deck.site.storeName
        'description'         = $d.description
        'url'                 = (Get-PageUrl $path)
        'applicationCategory' = 'TravelApplication'
        'operatingSystem'     = 'Android, Wear OS'
        'inLanguage'          = @('en', 'ru', 'uk')
        'image'               = "${base}assets/icon-512.png"
        'author'              = [ordered]@{ '@type' = 'Person'; 'name' = $deck.site.author; 'url' = $deck.site.hub }
        'license'             = "$($deck.site.repo)/blob/main/LICENSE"
    }
    New-Page -Group $group -Loc $Loc -Title $d.title -Description $d.description -Main $main -JsonLd $ld -Grid $true -Forward ($Loc -eq 'en')
}

function New-Doc([string]$Group, [string]$Loc) {
    $path = Get-PagePath $Group $Loc
    $d = $deck.locales.$Loc.$Group
    $ui = $deck.locales.$Loc.ui
    $blocks = New-Blocks $d.blocks $path $Loc
    $h2s = @($d.blocks | Where-Object { $_.t -eq 'h2' })
    $toc = ''
    if ($h2s.Count -ge 6) {
        $items = ($h2s | ForEach-Object { "<li><a href=""#$($_.id)"">$(Expand-Deck $_.x $path $Loc)</a></li>" }) -join "`n"
        $toc = "<nav class=""toc"" aria-label=""$(Enc $ui.contents)"">`n<ul>`n$items`n</ul>`n</nav>`n"
    }
    $main = @"
<main class="container" id="main">
  <noscript><p class="lang-links"><a href="$(Get-GroupHref $path $Group 'en')" hreflang="en">English</a> &middot; <a href="$(Get-GroupHref $path $Group 'ru')" hreflang="ru">Русский</a> &middot; <a href="$(Get-GroupHref $path $Group 'uk')" hreflang="uk">Українська</a></p></noscript>
  <article class="policy-card doc">
<h1>$(Enc $d.h1)</h1>
<p class="lead">$(Enc $d.lead)</p>
$toc$blocks
  </article>
</main>
"@
    $ld = [ordered]@{
        '@context'    = 'https://schema.org'
        '@type'       = 'WebPage'
        'name'        = $d.title
        'description' = $d.description
        'url'         = (Get-PageUrl $path)
        'inLanguage'  = $Loc
        'isPartOf'    = [ordered]@{ '@type' = 'WebSite'; 'name' = $deck.site.brand; 'url' = $base }
    }
    New-Page -Group $Group -Loc $Loc -Title $d.title -Description $d.description -Main $main -JsonLd $ld -Grid $false -Forward $false
}

# ---- parity: the three locales are one set of pages written whole (DOC-EXTERNAL-QUALITY rule 3) ----
# The deck keeps the locales side by side, so the mechanical check is structural: every locale has the keys, the
# blocks (type and list length) and the ids that English has. A section added to one locale only stops the build.
function Get-Shape($Object, [string]$Prefix) {
    $lines = New-Object System.Collections.Generic.List[string]
    if ($Object -is [System.Management.Automation.PSCustomObject]) {
        foreach ($p in $Object.PSObject.Properties) { $lines.AddRange([string[]]@(Get-Shape $p.Value "$Prefix.$($p.Name)")) }
    }
    elseif ($Object -is [System.Collections.IEnumerable] -and $Object -isnot [string]) {
        $i = 0
        foreach ($e in $Object) { $lines.AddRange([string[]]@(Get-Shape $e "$Prefix[$i]")); $i++ }
        $lines.Add("$Prefix#count=$i")
    }
    elseif ($Prefix -match '\.(id|key|src|dir|w|h)$' -or ($Prefix -match '\.t$' -and "$Object" -in @('h2', 'p', 'ul', 'ol', 'note'))) { $lines.Add("$Prefix=$Object") }
    else { $lines.Add($Prefix) }
    return $lines.ToArray()
}
$enShape = Get-Shape $deck.locales.en 'L'
foreach ($k in @('ru', 'uk')) {
    $shape = Get-Shape $deck.locales.$k 'L'
    $diff = Compare-Object $enShape $shape
    if ($diff) { Stop-Verify "locale $k differs in structure from en: $(($diff | Select-Object -First 3 | ForEach-Object { $_.InputObject }) -join '; ')" }
}

foreach ($k in $locKeys) {
    New-Landing $k
    New-Doc 'guide' $k
    New-Doc 'support' $k
}
$copies.Add(@{ From = 'docs/github/social-preview-1280x640.png'; To = 'site/assets/social-1280x640.png' })

# ---- sitemap: every public page, privacy included, each with the reciprocal alternates ----
$sm = New-Object System.Collections.Generic.List[string]
$sm.Add('<?xml version="1.0" encoding="UTF-8"?>')
$sm.Add('<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xhtml="http://www.w3.org/1999/xhtml">')
foreach ($g in $deck.pages) {
    foreach ($k in $locKeys) {
        $sm.Add('  <url>')
        $sm.Add("    <loc>$(Get-PageUrl (Get-PagePath $g.id $k))</loc>")
        foreach ($a in $locKeys) { $sm.Add("    <xhtml:link rel=""alternate"" hreflang=""$a"" href=""$(Get-PageUrl (Get-PagePath $g.id $a))""/>") }
        $sm.Add("    <xhtml:link rel=""alternate"" hreflang=""x-default"" href=""$(Get-PageUrl (Get-PagePath $g.id 'en'))""/>")
        $sm.Add('  </url>')
    }
}
$sm.Add('</urlset>')
$outputs['sitemap.xml'] = ($sm -join "`n") + "`n"

# ---- write, or compare ----
$stale = New-Object System.Collections.Generic.List[string]
foreach ($rel in $outputs.Keys) {
    $full = Join-Path $siteDir $rel
    $want = [string]$outputs[$rel]
    if ($Check) {
        if (-not (Test-Path -LiteralPath $full)) { $stale.Add("$rel (missing)"); continue }
        $have = [IO.File]::ReadAllText($full, $utf8).Replace("`r`n", "`n")
        if ($have -ne $want) { $stale.Add($rel) }
    }
    else {
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $full) | Out-Null
        [IO.File]::WriteAllText($full, $want, $utf8)
    }
}
foreach ($c in $copies) {
    $src = Join-Path $Root $c.From
    $dst = Join-Path $Root $c.To
    if (-not (Test-Path -LiteralPath $src)) { Stop-Verify "missing source $($c.From)" }
    if ($Check) {
        if (-not (Test-Path -LiteralPath $dst)) { $stale.Add("$($c.To) (missing)"); continue }
        if ((Get-FileHash -LiteralPath $src -Algorithm SHA256).Hash -ne (Get-FileHash -LiteralPath $dst -Algorithm SHA256).Hash) { $stale.Add($c.To) }
    }
    else {
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $dst) | Out-Null
        Copy-Item -LiteralPath $src -Destination $dst -Force
    }
}

$total = $outputs.Count + $copies.Count
if ($Check) {
    foreach ($s in $stale) { Write-Host "STALE: $s" }
    if ($stale.Count) { Write-Host "build-site: STALE ($($stale.Count) of $total render targets differ from the deck; run tools/site/build-site.ps1)"; exit 1 }
    Write-Host "build-site: PASS ($total render targets match the deck)"
    exit 0
}
Write-Host "build-site: WROTE $total render targets under site/"
exit 0
