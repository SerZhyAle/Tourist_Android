#requires -Version 7.0
<#
.SYNOPSIS
    The help gate (S0014, CONTEXT-HELP rule 19 and conformance rung 1 and the static half of rung 2).

.DESCRIPTION
    Counts surfaces against the help map and the map against the pages, and fails on:
      - a surface the product draws that has no row in the map, and a row that names no surface;
      - a form that is not built (layer, both), a page row without a help id, an inherit row without a
        parent, a none / inherit / ask-off row without a reason;
      - a page missing in a shipped locale, a screenshot or its capture record missing, a capture older than the
        layouts or the strings it shows (screenshot freshness), an element of the map that is not on the page
        or not in the capture;
      - a page that breaks the anatomy of rule 8, a request to any origin (an http reference outside an
        <a href>, a remote script, font or image), a missing lang, dir or referrer meta, a missing alt;
      - a link that does not resolve (file, or fragment of a page);
      - prose that promises what is not built, or breaks the house text style (.. never ..., plain hyphen);
      - a WebView that could load the network or run script, or help code that opens a connection.
    Over-claim words are advisories for the owner's wording review, not failures.

    Accepted debt lives in docs/help/baseline.json and may only fall (CHECK-BASELINE): a finding the baseline does
    not list fails; a baseline entry with no finding any more fails until it is removed.

    Exit codes (CHECK-VERDICT section 2): 0 PASS, 1 FAIL, 2 COULD NOT VERIFY. The last line printed is the verdict.
#>
[CmdletBinding()]
param(
    # Rung 2's freshness (a capture older than the layouts or strings it shows) becomes an advisory: for the unit-test
    # run, where a layout change by another ticket must not turn the build red before a device is at hand. The release
    # pre-flight runs the gate without it.
    [switch]$SkipFreshness
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib.ps1')

$findings = New-Object System.Collections.Generic.List[object]
$advisories = New-Object System.Collections.Generic.List[string]
function Add-Finding([string]$Key, [string]$Text) { $findings.Add([pscustomobject]@{ Key = $Key; Text = $Text }) }

try {
    $map = Read-HelpMap
    $locales = Get-ShippedLocales
} catch {
    Write-Host "help-check: COULD NOT VERIFY ($($_.Exception.Message))"
    exit 2
}

$surfaces = @($map.surfaces)
$androidNs = 'http://schemas.android.com/apk/res/android'
$basePackage = 'com.sza.tourist'

# ---------------------------------------------------------------- 1. surfaces enumerated from the source of truth
$enumerated = @{}   # fully qualified host -> kind
$manifest = New-Object System.Xml.XmlDocument
$manifest.Load((Join-Path $script:PhoneMain 'AndroidManifest.xml'))
function Resolve-Name([string]$n) { if ($n.StartsWith('.')) { return $basePackage + $n } else { return $n } }
foreach ($a in $manifest.SelectNodes('//activity')) { $enumerated[(Resolve-Name $a.GetAttribute('name', $androidNs))] = 'activity' }
foreach ($r in $manifest.SelectNodes('//receiver')) {
    $isWidget = $false
    foreach ($m in $r.SelectNodes('meta-data')) { if ($m.GetAttribute('name', $androidNs) -eq 'android.appwidget.provider') { $isWidget = $true } }
    if ($isWidget) { $enumerated[(Resolve-Name $r.GetAttribute('name', $androidNs))] = 'widget' }
}

$builderSites = @{}  # fully qualified top class -> count of dialog builders outside a DialogFragment
foreach ($f in Get-ChildItem -LiteralPath (Join-Path $script:PhoneMain 'java') -Recurse -Filter '*.kt') {
    $text = Get-Content -LiteralPath $f.FullName -Raw -Encoding utf8
    $pkg = if ($text -match '(?m)^package\s+([\w.]+)') { $Matches[1] } else { '' }
    $dialogClasses = [regex]::Matches($text, '(?m)^\s*(?:@\w+(?:\([^)]*\))?\s*)*class\s+(\w+)\s*(?:\([^)]*\))?\s*:\s*[^{\n]*DialogFragment\(')
    foreach ($m in $dialogClasses) { $enumerated["$pkg.$($m.Groups[1].Value)"] = 'dialog' }
    $builders = [regex]::Matches($text, '(MaterialAlertDialogBuilder|AlertDialog\.Builder)\(').Count
    if ($builders -gt 0 -and $dialogClasses.Count -eq 0) {
        $top = if ($text -match '(?m)^(?:\w+\s+)*(?:class|object)\s+(\w+)') { $Matches[1] } else { $null }
        if ($top) { $builderSites["$pkg.$top"] = $builders }
    }
}

$hostRows = @($surfaces | Where-Object { Get-Prop $_ 'host' })
$rowHosts = @($hostRows | ForEach-Object { ([string]$_.host) })
foreach ($h in $enumerated.Keys) {
    if ($rowHosts -notcontains $h) { Add-Finding "surface-without-row:$h" "The product draws $h ($($enumerated[$h])) and the help map has no row for it." }
}
foreach ($site in $builderSites.Keys) {
    $n = @($rowHosts | Where-Object { $_.StartsWith("$site#") }).Count
    if ($n -lt $builderSites[$site]) { Add-Finding "dialog-without-row:$site" "$site builds $($builderSites[$site]) dialog(s) and the map has $n row(s) for them (Class#member)." }
}
foreach ($row in $hostRows) {
    $h = [string]$row.host
    if ($h.Contains('#')) {
        $cls = $h.Split('#')[0]
        if (-not $builderSites.ContainsKey($cls)) { Add-Finding "row-without-surface:$($row.id)" "Row $($row.id) names $h, which is not a dialog call site in the source." }
    } elseif (-not $enumerated.ContainsKey($h)) {
        Add-Finding "row-without-surface:$($row.id)" "Row $($row.id) names $h, which the product no longer draws."
    }
}
$ids = @($surfaces | ForEach-Object { $_.id })
if (@($ids | Group-Object | Where-Object Count -gt 1).Count -gt 0) { Add-Finding 'duplicate-surface-id' 'Two rows of the map share an id.' }

# ---------------------------------------------------------------- 2. forms, reasons, parents
foreach ($row in $surfaces) {
    $form = Get-Prop $row 'form' ''
    switch ($form) {
        'page' {
            if (-not (Get-Prop $row 'help')) { Add-Finding "page-without-help:$($row.id)" "Row $($row.id) is a page and has no help id." }
            if (@(Get-Prop $row 'elements' @()).Count -eq 0) { Add-Finding "page-without-elements:$($row.id)" "Row $($row.id) lists no elements." }
        }
        'inherit' {
            $parent = Get-Prop $row 'parent'
            if (-not $parent -or $ids -notcontains $parent) { Add-Finding "inherit-without-parent:$($row.id)" "Row $($row.id) inherits from '$parent', which is not a row." }
            if (-not (Get-Prop $row 'reason')) { Add-Finding "no-reason:$($row.id)" "Row $($row.id) inherits and gives no reason." }
        }
        'none' { if (-not (Get-Prop $row 'reason')) { Add-Finding "no-reason:$($row.id)" "Row $($row.id) is none and gives no reason." } }
        default { Add-Finding "form-not-built:$($row.id)" "Row $($row.id) has form '$form'; this product builds page, inherit and none." }
    }
    if ((Get-Prop $row 'ask') -eq 'off' -and -not (Get-Prop $row 'reason')) { Add-Finding "no-reason:$($row.id)" "Row $($row.id) turns the ask off and gives no reason." }
}
$pageRows = @($surfaces | Where-Object { (Get-Prop $_ 'form') -eq 'page' })
$helpIds = @($pageRows | ForEach-Object { $_.help })
foreach ($row in @($surfaces | Where-Object { (Get-Prop $_ 'form') -eq 'inherit' })) {
    $p = @($surfaces | Where-Object { $_.id -eq (Get-Prop $row 'parent') })[0]
    if ($p -and (Get-Prop $p 'form') -eq 'none') { Add-Finding "inherit-from-none:$($row.id)" "Row $($row.id) inherits from $($p.id), which has no help." }
    $anchor = Get-Prop $row 'anchor'
    if ($anchor -and $p -and (Get-Prop $p 'form') -eq 'page' -and @($p.elements | Where-Object { $_.anchor -eq $anchor }).Count -eq 0) {
        Add-Finding "inherit-anchor:$($row.id)" "Row $($row.id) points at anchor '$anchor', which is not an element of $($p.id)."
    }
}

# ---------------------------------------------------------------- 3. locale parity, pages, screenshots, captures
$assetLocales = @(Get-ChildItem -LiteralPath $script:AssetHelp -Directory | ForEach-Object Name)
foreach ($l in $assetLocales) { if ($locales -notcontains $l) { Add-Finding "orphan-locale:$l" "assets/help/$l exists but the UI ships no '$l' locale." } }
foreach ($loc in $locales) {
    foreach ($key in @('help_action', 'help_title', 'help_close', 'help_page_missing')) {
        if ($loc -ne 'en' -and -not (Read-Strings $loc).ContainsKey($key)) { Add-Finding "string-parity:${loc}:$key" "values-$loc has no '$key'." }
    }
}

$layoutsNow = Get-LayoutsHash
$stringsNow = @{}
foreach ($loc in $locales) { $stringsNow[$loc] = Get-StringsHash $loc }
$titleOf = @{}
foreach ($loc in $locales) {
    $s = Read-Strings $loc
    foreach ($row in $pageRows) {
        $res = Get-Prop $row 'nameRes'
        $titleOf["$loc/$($row.help)"] = if ($res -and $s.ContainsKey($res)) { $s[$res] } else { $row.name }
    }
}

$checkedLinks = 0
foreach ($loc in $locales) {
    $dir = Join-Path $script:AssetHelp $loc
    $expected = @($helpIds) + @('index')
    foreach ($helpId in $expected) {
        $file = Join-Path $dir "$helpId.html"
        if (-not (Test-Path -LiteralPath $file)) { Add-Finding "page-missing:$loc/$helpId" "No page $loc/$helpId.html (rule 13: a page in every shipped locale)."; continue }
        $html = Get-Content -LiteralPath $file -Raw -Encoding utf8
        $tag = "$loc/$helpId"

        # rule 12: no request to any origin, no script from outside
        $withoutAnchors = [regex]::Replace($html, '<a\s[^>]*href="[^"]*"[^>]*>', '<a>')
        if ($withoutAnchors -match 'https?://') { Add-Finding "remote-reference:$tag" "$tag.html names an http(s) address outside an <a href> (rule 12)." }
        if ($html -match '<script[^>]+\ssrc=') { Add-Finding "remote-script:$tag" "$tag.html loads a script file." }
        if ($html -match '<link[^>]+href="https?:') { Add-Finding "remote-link:$tag" "$tag.html links a remote stylesheet or font." }
        if ($html -match '<form\b|<iframe\b|<embed\b|<object\b') { Add-Finding "active-content:$tag" "$tag.html embeds a form, frame or object." }
        if ($html -notmatch '<meta name="referrer" content="no-referrer">') { Add-Finding "no-referrer:$tag" "$tag.html lacks the no-referrer meta." }
        if ($html -notmatch "<html lang=`"$loc`" dir=`"(ltr|rtl)`">") { Add-Finding "lang-dir:$tag" "$tag.html lacks lang=`"$loc`" and dir." }
        if ($html -match 'http-equiv="refresh"') { Add-Finding "refresh:$tag" "$tag.html redirects by script or meta." }

        # house style and promises (the visible text; element ids and css names are not prose)
        $visible = [System.Net.WebUtility]::HtmlDecode(([regex]::Replace($html, '<script.*?</script>|<[^>]+>', ' ')))
        if ($visible -match '\.\.\.|…') { Add-Finding "style-ellipsis:$tag" "$tag.html has '...' or the ellipsis character (house style: two dots)." }
        if ($visible -match '[—–]') { Add-Finding "style-dash:$tag" "$tag.html has an em or en dash (house style: a plain hyphen)." }
        # whole words only: "скорость" (speed) is not "скоро" (soon)
        if ($visible -match '(?i)(?<!\p{L})(coming soon|planned|not yet|in development|скоро|планируется|ещё не|еще не|планується|поки що не|ще не)(?!\p{L})') {
            Add-Finding "promise:$tag" "$tag.html promises something not built ('$($Matches[0])') (invariant 17)."
        }
        if ($visible -match '(?i)waterproof|water-resistant|certified|guarantee|rescue service|emergency service|водонепроница|влагозащищ|сертифициров|гарантир|спасательн|водонепроник|вологозахищ|сертифіков|гарантує|рятувальн') {
            $advisories.Add("${tag}: review the wording of '$($Matches[0])' against the over-claim lines (S0012 2.1)")
        }

        # rule 8 anatomy and the title from the termbase
        if ($helpId -ne 'index') {
            $h1 = if ($html -match '<h1>(.*?)</h1>') { [System.Net.WebUtility]::HtmlDecode($Matches[1]) } else { $null }
            if ($h1 -ne $titleOf[$tag]) { Add-Finding "title:$tag" "$tag.html title is '$h1'; the UI shows '$($titleOf[$tag])'." }
            if ($html -notmatch '<p class="lead">') { Add-Finding "anatomy-lead:$tag" "$tag.html has no 'what this is for' lead." }
            if ($html -notmatch '<img class="on-dark"[^>]*alt="[^"]+"') { Add-Finding "alt:$tag" "$tag.html has no alt text on its screenshot." }
            if ($html -notmatch '<ol class="order">') { Add-Finding "anatomy-order:$tag" "$tag.html has no 'in this order' list." }
            $orderList = [regex]::Match($html, '<ol class="order">(.*?)</ol>', 'Singleline').Groups[1].Value
            if ([regex]::Matches($orderList, '<li>').Count -gt 7) { Add-Finding "anatomy-order-length:$tag" "$tag.html has more than seven ordered steps." }
            if ($html -notmatch '<footer class="help-foot">' -or $html -notmatch 'href="index.html"') { Add-Finding "anatomy-footer:$tag" "$tag.html lacks the footer or its link to the help index." }
            $row = @($pageRows | Where-Object { $_.help -eq $helpId })[0]
            $onPage = @([regex]::Matches($html, '<details class="el" id="([^"]+)"') | ForEach-Object { $_.Groups[1].Value })
            $inMap = @($row.elements | ForEach-Object { $_.anchor })
            foreach ($a in $inMap) { if ($onPage -notcontains $a) { Add-Finding "element-missing:$tag#$a" "$tag.html has no entry for map element '$a'." } }
            foreach ($a in $onPage) { if ($inMap -notcontains $a) { Add-Finding "element-extra:$tag#$a" "$tag.html has an entry '$a' the map does not list." } }

            # screenshots and the capture record (rule 14, freshness)
            foreach ($theme in $script:ThemeNames) {
                if (-not (Test-Path (Join-Path $dir "img\$helpId-$theme.jpg"))) { Add-Finding "shot-missing:$tag-$theme" "No screenshot $loc/img/$helpId-$theme.jpg." }
            }
            $rec = Join-Path $script:ShotsRoot "$loc\$helpId.json"
            if (-not (Test-Path $rec)) { Add-Finding "capture-missing:$tag" "No capture record for $tag." }
            else {
                $r = Read-Json $rec
                $recMissing = @(Get-Prop $r 'missing' @())
                foreach ($m in $recMissing) { Add-Finding "capture-missing:$tag#$m" "The app does not show '$m' on $tag (the capture recorded it as absent); fix the screen or keep the finding in the baseline." }
                foreach ($e in $row.elements) {
                    if ((Get-Prop $e 'shot' $true) -ne $false -and $recMissing -notcontains $e.anchor -and $null -eq (Get-Prop (Get-Prop $r 'elements') $e.anchor)) {
                        Add-Finding "capture-element:$tag#$($e.anchor)" "The capture of $tag has no bounds for '$($e.anchor)'; capture again."
                    }
                }
                if (-not $SkipFreshness -and (Get-Prop $r 'layoutsSha256') -ne $layoutsNow) { Add-Finding "stale-layouts:$tag" "The layouts changed after the capture of $tag; capture again (rung 2)." }
                if (-not $SkipFreshness -and (Get-Prop $r 'stringsSha256') -ne $stringsNow[$loc]) { Add-Finding "stale-strings:$tag" "The $loc strings changed after the capture of $tag; capture again (rung 2)." }
            }
        }

        # links resolve: files, and fragments of pages
        foreach ($m in [regex]::Matches($html, '(?:href|src)="([^"]*)"')) {
            $ref = $m.Groups[1].Value
            if ($ref -match '^(https?:|mailto:)') { continue }
            $checkedLinks++
            $path, $frag = $ref -split '#', 2
            $targetFile = if ($path) { [System.IO.Path]::GetFullPath((Join-Path $dir $path)) } else { $file }
            if (-not (Test-Path -LiteralPath $targetFile)) { Add-Finding "dead-link:$tag->$ref" "$tag.html links $ref, which does not exist."; continue }
            if ($frag -and $targetFile.EndsWith('.html')) {
                $tHtml = Get-Content -LiteralPath $targetFile -Raw -Encoding utf8
                if ($tHtml -notmatch "id=`"$([regex]::Escape($frag))`"") { Add-Finding "dead-anchor:$tag->$ref" "$tag.html links $ref, and that page has no '#$frag'." }
            }
        }
    }
}
foreach ($css in @('sza-kit.css', 'help.css')) {
    $p = Join-Path $script:AssetHelp $css
    if (-not (Test-Path $p)) { Add-Finding "css-missing:$css" "assets/help/$css is missing."; continue }
    $c = [regex]::Replace((Get-Content -LiteralPath $p -Raw -Encoding utf8), '/\*.*?\*/', '', 'Singleline')   # comments may name what they forbid
    if ($c -match '@import|url\(\s*["'']?https?:') { Add-Finding "css-remote:$css" "$css makes a request (@import or a remote url)." }
}

# ---------------------------------------------------------------- 4. prose sources: style and promises (every locale)
foreach ($f in Get-ChildItem -LiteralPath $script:SrcRoot -Filter '*.json') {
    $raw = Get-Content -LiteralPath $f.FullName -Raw -Encoding utf8
    try { $null = $raw | ConvertFrom-Json } catch { Add-Finding "bad-json:$($f.Name)" "$($f.Name) is not valid JSON."; continue }
    if ($f.Name -in @('capture.json')) { continue }
    $prose = [regex]::Replace($raw, '"\$comment"\s*:\s*"[^"]*"', '')
    if ($prose -match '\.\.\.|…') { Add-Finding "src-ellipsis:$($f.Name)" "$($f.Name) has '...' or the ellipsis character." }
    if ($prose -match '[—–]') { Add-Finding "src-dash:$($f.Name)" "$($f.Name) has an em or en dash." }
}
foreach ($row in $pageRows) {
    $f = Join-Path $script:SrcRoot "$($row.help).json"
    if (-not (Test-Path $f)) { Add-Finding "src-missing:$($row.help)" "No prose source docs/help/src/$($row.help).json."; continue }
    $c = Read-Json $f
    foreach ($loc in $locales) {
        $b = Get-Prop (Get-Prop $c 'locales') $loc
        if ($null -eq $b) { Add-Finding "src-locale:$($row.help)/$loc" "$($row.help).json has no '$loc' prose."; continue }
        foreach ($e in $row.elements) {
            if ($null -eq (Get-Prop (Get-Prop $b 'elements') $e.anchor)) { Add-Finding "src-element:$($row.help)/$loc#$($e.anchor)" "$($row.help).json ($loc) has no prose for '$($e.anchor)'." }
        }
    }
}

# ---------------------------------------------------------------- 5. the viewer and the code: no network, no script, no new permission
$helpSrc = Get-ChildItem -LiteralPath (Join-Path $script:PhoneMain 'java\com\sza\tourist\help') -Filter '*.kt'
$helpCode = ($helpSrc | ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw -Encoding utf8 }) -join "`n"
if ($helpCode -notmatch 'javaScriptEnabled\s*=\s*false') { Add-Finding 'viewer-script' 'The help viewer does not set javaScriptEnabled = false.' }
if ($helpCode -notmatch 'blockNetworkLoads\s*=\s*true') { Add-Finding 'viewer-network' 'The help viewer does not set blockNetworkLoads = true.' }
if ($helpCode -match 'okhttp|HttpURLConnection|java\.net\.URL\b|Socket\(|retrofit') { Add-Finding 'help-network-code' 'Help code opens a connection.' }
if ($helpCode -match 'addJavascriptInterface|setWebContentsDebuggingEnabled') { Add-Finding 'viewer-bridge' 'The help viewer exposes a script bridge.' }
if ($helpCode -match 'KEYCODE_F1' -and (Get-Content (Join-Path $script:PhoneMain 'AndroidManifest.xml') -Raw) -match 'F1') { Add-Finding 'f1-global' 'F1 is mentioned in the manifest; it must never be a system-wide chord.' }

# ---------------------------------------------------------------- 6. baseline (accepted debt, may only fall)
$accepted = @()
if (Test-Path $script:BaselineFile) { $accepted = @((Read-Json $script:BaselineFile).accepted | Where-Object { $_ }) }
$fatal = @($findings | Where-Object { $accepted -notcontains $_.Key })
$baselined = @($findings | Where-Object { $accepted -contains $_.Key })
$stale = @($accepted | Where-Object { $_ -notin @($findings | ForEach-Object Key) })
foreach ($s in $stale) { $fatal += [pscustomobject]@{ Key = "stale-baseline:$s"; Text = "The baseline accepts '$s' and nothing raises it any more; remove it (the baseline only falls)." } }

# ---------------------------------------------------------------- verdict
foreach ($f in $fatal) { Write-Host "FAIL  $($f.Text)" }
foreach ($b in $baselined) { $advisories.Add("accepted in the baseline: $($b.Text)") }
foreach ($a in $advisories) { Write-Host "NOTE  $a" }
$counts = "$($surfaces.Count) surfaces, $($pageRows.Count) pages x $($locales.Count) locales, $checkedLinks links, $($baselined.Count) baselined"
if ($fatal.Count -gt 0) {
    Write-Host "help-check: FAIL ($($fatal.Count) finding(s); $counts)"
    exit 1
}
if ($advisories.Count -gt 0) {
    Write-Host "help-check: PASS WITH ADVISORIES ($($advisories.Count); $counts)"
    exit 0
}
Write-Host "help-check: PASS ($counts)"
exit 0
