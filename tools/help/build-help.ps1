#requires -Version 7.0
<#
.SYNOPSIS
    Builds the bundled help pages (S0014, CONTEXT-HELP rules 8 to 14) from their sources.

.DESCRIPTION
    Inputs, all committed:
      phone/src/main/assets/help/help-map.json   the surfaces, their pages and the elements in marker order
      docs/help/src/<help>.json                  the prose of one page, every shipped locale in one file
      docs/help/src/chrome.json                  the fixed words of a page
      phone/src/main/res/values*/strings.xml     the names the UI shows (rule 13: one string source)
      docs/help/shots/<locale>/<help>.json       the capture record written by capture-help.ps1:
                                                 element bounds, screenshot size, product version, date
    Outputs, render targets that are never edited by hand (canon invariant 16):
      phone/src/main/assets/help/<locale>/<help>.html and index.html

    Screenshots are written by capture-help.ps1 and are inputs here, not outputs.

    Exit codes (CHECK-VERDICT section 2): 0 built, 1 a source is inconsistent, 2 could not run.
#>
[CmdletBinding()]
param(
    [string[]]$Pages,
    [string[]]$Locales
)

$ErrorActionPreference = 'Stop'
if ($Pages) { $Pages = @($Pages | ForEach-Object { $_ -split "," }) }
if ($Locales) { $Locales = @($Locales | ForEach-Object { $_ -split "," }) }
. (Join-Path $PSScriptRoot 'lib.ps1')

$script:Problems = New-Object System.Collections.Generic.List[string]
function Add-Problem([string]$Text) { $script:Problems.Add($Text); Write-Host "  problem: $Text" -ForegroundColor Red }

$script:Ui = @{}
$script:Where = ''

function Expand-Ui {
    # {ui:string_name} -> the name the UI shows in this locale, in bold (rule 13: one string source, so a
    # renamed button is renamed in its help by the same edit).
    param([string]$Text)
    return [regex]::Replace($Text, '\{ui:([A-Za-z0-9_]+)\}', {
            param($m)
            $k = $m.Groups[1].Value
            if ($script:Ui.ContainsKey($k)) { return '**' + $script:Ui[$k] + '**' }
            Add-Problem "$($script:Where) : unknown UI string '$k'"
            return $m.Value
        })
}

function Convert-Prose {
    param([string]$Text)
    return ConvertTo-HtmlInline (Expand-Ui $Text)
}

function Convert-Plain {
    # Text for an attribute (the screenshot's alt): UI names inserted, no markup.
    param([string]$Text)
    return ConvertTo-HtmlText ((Expand-Ui $Text) -replace '\*\*', '')
}

function Format-Pct([double]$v) { return ([math]::Round($v, 2)).ToString([System.Globalization.CultureInfo]::InvariantCulture) + '%' }

function Get-Pin {
    # Where the numbered marker stands, as percentages of the screenshot: on the element, kept off the middle
    # of a wide or tall element so it does not hide what the element shows. (PowerShell names are not case
    # sensitive: the element sizes must not share a letter with the screen size.)
    param($Bounds, [double]$ScreenW, [double]$ScreenH)
    $boxW = $Bounds.r - $Bounds.l
    $boxH = $Bounds.b - $Bounds.t
    $px = if ($boxW / $ScreenW -gt 0.4) { $Bounds.r - 0.07 * $ScreenW } else { ($Bounds.l + $Bounds.r) / 2 }
    $py = if ($boxH / $ScreenH -gt 0.2) { $Bounds.t + [math]::Min($boxH / 2, 0.045 * $ScreenH) } else { ($Bounds.t + $Bounds.b) / 2 }
    $px = [math]::Min([math]::Max($px, 0.03 * $ScreenW), 0.97 * $ScreenW)
    $py = [math]::Min([math]::Max($py, 0.02 * $ScreenH), 0.98 * $ScreenH)
    return [pscustomobject]@{ X = Format-Pct (100 * $px / $ScreenW); Y = Format-Pct (100 * $py / $ScreenH) }
}
$map = Read-HelpMap
$chromeAll = Read-Json (Join-Path $script:SrcRoot 'chrome.json')
$shipped = Get-ShippedLocales
if (-not $Locales) { $Locales = $shipped }
$pageSurfaces = @($map.surfaces | Where-Object { (Get-Prop $_ 'form') -eq 'page' })
if ($Pages) { $pageSurfaces = @($pageSurfaces | Where-Object { $Pages -contains $_.help }) }

foreach ($locale in $Locales) {
    if ($shipped -notcontains $locale) { throw "Locale '$locale' is not a shipped locale ($($shipped -join ', '))." }
}

# page title (as the UI shows it) of a help id, per locale
function Get-HelpTitle($HelpId, $Strings) {
    $s = @($map.surfaces | Where-Object { (Get-Prop $_ 'help') -eq $HelpId })[0]
    if ($null -eq $s) { return $null }
    $res = Get-Prop $s 'nameRes'
    if ($res -and $Strings.ContainsKey($res)) { return $Strings[$res] }
    return $s.name
}

foreach ($locale in $Locales) {
    Write-Host "== $locale"
    $strings = Read-Strings $locale
    $script:Ui = $strings
    $chrome = $chromeAll.locales.$locale
    if ($null -eq $chrome) { Add-Problem "chrome.json has no '$locale' block"; continue }
    $product = $strings['app_name']
    $dir = if ($locale -in @('ar', 'he', 'fa', 'ur')) { 'rtl' } else { 'ltr' }
    $version = ''
    $captured = ''

    foreach ($surface in $pageSurfaces) {
        $helpId = $surface.help
        $title = Get-HelpTitle $helpId $strings
        $script:Where = "$helpId/$locale"
        $contentFile = Join-Path $script:SrcRoot "$helpId.json"
        if (-not (Test-Path $contentFile)) { Add-Problem "$helpId : no prose source docs/help/src/$helpId.json"; continue }
        $content = Read-Json $contentFile
        $block = Get-Prop (Get-Prop $content 'locales') $locale
        if ($null -eq $block) { Add-Problem "$helpId : no '$locale' prose"; continue }
        $shotFile = Join-Path $script:ShotsRoot "$locale\$helpId.json"
        if (-not (Test-Path $shotFile)) { Add-Problem "$helpId/$locale : no capture record (run capture-help.ps1)"; continue }
        $shot = Read-Json $shotFile
        if (-not $version) { $version = $shot.version; $captured = $shot.capturedOn }
        $shotW = [double]$shot.width; $shotH = [double]$shot.height

        $elements = @($surface.elements)
        $marker = @{}
        for ($i = 0; $i -lt $elements.Count; $i++) { $marker[$elements[$i].anchor] = $i + 1 }

        $kind = Get-SurfaceKind $surface
        $kicker = if ($kind -eq 'dialog') { $chrome.kickerWindow } else { $chrome.kickerScreen }
        $caption = if ($kind -eq 'dialog') { $chrome.figcaptionWindow } else { $chrome.figcaption }
        $sb = New-Object System.Text.StringBuilder
        $null = $sb.AppendLine('<!doctype html>')
        $null = $sb.AppendLine("<html lang=`"$locale`" dir=`"$dir`">")
        $null = $sb.AppendLine('<head>')
        $null = $sb.AppendLine('<meta charset="utf-8">')
        $null = $sb.AppendLine('<meta name="viewport" content="width=device-width, initial-scale=1">')
        $null = $sb.AppendLine('<meta name="referrer" content="no-referrer">')
        $null = $sb.AppendLine("<title>$(ConvertTo-HtmlText ($chrome.pageTitle + ' - ' + $title))</title>")
        $null = $sb.AppendLine('<script>(function(){try{var q=new URLSearchParams(location.search).get("theme");var t=(q==="light"||q==="dark")?q:(matchMedia("(prefers-color-scheme: light)").matches?"light":"dark");document.documentElement.setAttribute("data-theme",t);}catch(e){}})();</script>')
        $null = $sb.AppendLine('<link rel="stylesheet" href="../sza-kit.css">')
        $null = $sb.AppendLine('<link rel="stylesheet" href="../help.css">')
        $null = $sb.AppendLine('</head>')
        $null = $sb.AppendLine('<body class="help">')
        $null = $sb.AppendLine('<main class="container">')
        $null = $sb.AppendLine('<nav class="help-crumbs" aria-label="' + (ConvertTo-HtmlText $chrome.indexLink) + '">')
        $null = $sb.AppendLine('  <a href="index.html">' + (ConvertTo-HtmlText $chrome.indexLink) + '</a><span class="sep" aria-hidden="true">/</span><span>' + (ConvertTo-HtmlText $title) + '</span>')
        $null = $sb.AppendLine('</nav>')
        $null = $sb.AppendLine('<header class="hero">')
        $null = $sb.AppendLine('  <p class="kicker">' + (ConvertTo-HtmlText $kicker) + '</p>')
        $null = $sb.AppendLine('  <h1>' + (ConvertTo-HtmlText $title) + '</h1>')
        $null = $sb.AppendLine('  <p class="lead">' + (Convert-Prose $block.lead) + '</p>')
        $null = $sb.AppendLine('</header>')

        # 3. screenshot and pins
        $pins = New-Object System.Text.StringBuilder
        foreach ($e in $elements) {
            if ((Get-Prop $e 'shot' $true) -eq $false) { continue }
            $b = Get-Prop (Get-Prop $shot 'elements') $e.anchor
            if ($null -eq $b -and @(Get-Prop $shot 'missing' @()) -contains $e.anchor) { continue }   # recorded by the capture: the app does not show it here
            if ($null -eq $b) { Add-Problem "$helpId/$locale : the capture has no bounds for '$($e.anchor)' (view $($e.view))"; continue }
            $pin = Get-Pin $b $shotW $shotH
            $n = $marker[$e.anchor]
            $nameRes = Get-Prop $e 'nameRes'
            $ename = if ($nameRes -and $strings.ContainsKey($nameRes)) { $strings[$nameRes] } else { [string](Get-Prop (Get-Prop (Get-Prop $block 'elements') $e.anchor) 'name' $e.anchor) }
            $null = $pins.AppendLine("      <a class=`"pin`" style=`"--x:$($pin.X);--y:$($pin.Y)`" href=`"#$($e.anchor)`" aria-label=`"$n, $(ConvertTo-HtmlText $ename)`">$n</a>")
        }
        $imgW = [int]$shot.imageWidth; $imgH = [int]$shot.imageHeight
        $alt = Convert-Plain ([string](Get-Prop $block 'alt' $title))
        $null = $sb.AppendLine('<figure class="shot">')
        $null = $sb.AppendLine('  <div class="frame">')
        $null = $sb.AppendLine("    <img class=`"on-dark`" src=`"img/$helpId-dark.jpg`" width=`"$imgW`" height=`"$imgH`" alt=`"$alt`">")
        $null = $sb.AppendLine("    <img class=`"on-light`" src=`"img/$helpId-light.jpg`" width=`"$imgW`" height=`"$imgH`" alt=`"`">")
        $null = $sb.Append($pins.ToString())
        $null = $sb.AppendLine('  </div>')
        $null = $sb.AppendLine('  <figcaption>' + (ConvertTo-HtmlText $caption) + '</figcaption>')
        $null = $sb.AppendLine('</figure>')

        # 4. in this order
        $steps = @(Get-Prop $block 'order' @())
        if ($steps.Count -gt 7) { Add-Problem "$helpId/$locale : 'order' has $($steps.Count) steps; the contract allows at most seven" }
        $null = $sb.AppendLine('<h2>' + (ConvertTo-HtmlText $chrome.orderHeading) + '</h2>')
        $null = $sb.AppendLine('<ol class="order">')
        foreach ($step in $steps) {
            $html = Convert-Prose $step
            $html = [regex]::Replace($html, '\{(e-[a-z0-9-]+)\}', {
                    param($m)
                    $a = $m.Groups[1].Value
                    if (-not $marker.ContainsKey($a)) { Add-Problem "$helpId/$locale : 'order' names unknown element '$a'"; return $m.Value }
                    return '<a class="ref" href="#' + $a + '">' + $marker[$a] + '</a>'
                })
            $null = $sb.AppendLine("  <li>$html</li>")
        }
        $null = $sb.AppendLine('</ol>')

        # 5. the elements
        $null = $sb.AppendLine('<h2>' + (ConvertTo-HtmlText $chrome.elementsHeading) + '</h2>')
        $null = $sb.AppendLine('<div class="elements">')
        foreach ($e in $elements) {
            $prose = Get-Prop (Get-Prop $block 'elements') $e.anchor
            if ($null -eq $prose) { Add-Problem "$helpId/$locale : no prose for element '$($e.anchor)'"; continue }
            $n = $marker[$e.anchor]
            $nameRes = Get-Prop $e 'nameRes'
            $ename = if ($nameRes -and $strings.ContainsKey($nameRes)) { $strings[$nameRes] } else { [string](Get-Prop $prose 'name' '') }
            if (-not $ename) { Add-Problem "$helpId/$locale : element '$($e.anchor)' has neither a UI string nor a 'name'"; continue }
            $sometimes = ((Get-Prop $e 'shot' $true) -eq $false)
            $null = $sb.AppendLine("  <details class=`"el`" id=`"$($e.anchor)`">")
            $notShown = @(Get-Prop $shot 'missing' @()) -contains $e.anchor
            $pill = if ($sometimes) { ' <span class="pill">' + (ConvertTo-HtmlText $chrome.sometimes) + '</span>' }
            elseif ($notShown) { ' <span class="pill">' + (ConvertTo-HtmlText $chrome.notShown) + '</span>' }
            else { '' }
            $null = $sb.AppendLine("    <summary><span class=`"mk`">$n</span><span><span class=`"name`">$(ConvertTo-HtmlText $ename)</span>$pill<span class=`"one`">$(Convert-Prose $prose.one)</span></span></summary>")
            $null = $sb.AppendLine('    <div class="full">')
            $null = $sb.AppendLine('      <p>' + (Convert-Prose $prose.what) + '</p>')
            $null = $sb.AppendLine('      <dl class="facts">')
            foreach ($f in @(@('why', 'why'), @('next', 'next'), @('undo', 'undo'), @('shortcut', 'shortcut'))) {
                $text = Get-Prop $prose $f[0]
                if ($text) { $null = $sb.AppendLine('        <dt>' + (ConvertTo-HtmlText $chrome.($f[1])) + '</dt><dd>' + (Convert-Prose $text) + '</dd>') }
            }
            $null = $sb.AppendLine('      </dl>')
            $tip = Get-Prop $prose 'tip'
            if ($tip) { $null = $sb.AppendLine('      <div class="note"><span class="tag">' + (ConvertTo-HtmlText $chrome.tip) + '</span><p>' + (Convert-Prose $tip) + '</p></div>') }
            $null = $sb.AppendLine('    </div>')
            $null = $sb.AppendLine('  </details>')
        }
        $null = $sb.AppendLine('</div>')

        # 6. related: the pages this one opens or is opened from, and the index
        $null = $sb.AppendLine('<h2>' + (ConvertTo-HtmlText $chrome.relatedHeading) + '</h2>')
        $null = $sb.AppendLine('<ul class="related">')
        foreach ($r in @(Get-Prop $block 'related' @())) {
            $rh = Get-Prop $r 'help'
            if ($rh) {
                $target = @($map.surfaces | Where-Object { (Get-Prop $_ 'help') -eq $rh })[0]
                if ($null -eq $target) { Add-Problem "$helpId/$locale : related names unknown help id '$rh'"; continue }
                $rt = Get-HelpTitle $rh $strings
                $href = "$rh.html" + $(if (Get-Prop $r 'anchor') { '#' + $r.anchor } else { '' })
                $rpill = if ((Get-SurfaceKind $target) -eq 'dialog') { $chrome.pillWindow } else { $chrome.pillScreen }
                $null = $sb.AppendLine("  <li><a href=`"$href`"><b>$(ConvertTo-HtmlText $rt)</b><span class=`"pill`">$(ConvertTo-HtmlText $rpill)</span><small>$(Convert-Prose (Get-Prop $r 'note' ''))</small></a></li>")
            }
        }
        $null = $sb.AppendLine('  <li><a href="index.html"><b>' + (ConvertTo-HtmlText $chrome.indexLink) + '</b><small>' + (ConvertTo-HtmlText $chrome.indexBack) + '</small></a></li>')
        $null = $sb.AppendLine('</ul>')

        # 7. footer
        $foot = $chrome.footer.Replace('{product}', $product).Replace('{version}', $shot.version).Replace('{date}', $shot.capturedOn)
        $null = $sb.AppendLine('<footer class="help-foot">')
        $null = $sb.AppendLine('  <span>' + (ConvertTo-HtmlText $foot) + '</span>')
        $null = $sb.AppendLine('  <a href="index.html">' + (ConvertTo-HtmlText $chrome.indexLink) + '</a>')
        $support = Get-Prop (Get-Prop (Read-Json (Join-Path $PSScriptRoot '..\..\docs\help\help.config.json')) 'support') 'url'
        if ($support) { $null = $sb.AppendLine('  <a href="' + (ConvertTo-HtmlText $support) + '">' + (ConvertTo-HtmlText $chrome.support) + '</a>') }
        $null = $sb.AppendLine('</footer>')
        $null = $sb.AppendLine('</main>')
        $null = $sb.AppendLine('</body>')
        $null = $sb.AppendLine('</html>')
        Write-Utf8 (Join-Path $script:AssetHelp "$locale\$helpId.html") $sb.ToString()
        Write-Host "  $helpId.html"
    }

    # the index
    if (-not $Pages) {
        $indexContent = Read-Json (Join-Path $script:SrcRoot 'index.json')
        $script:Where = "index/$locale"
        $iblock = Get-Prop (Get-Prop $indexContent 'locales') $locale
        if ($null -eq $iblock) { Add-Problem "index/$locale : no prose"; continue }
        $sb = New-Object System.Text.StringBuilder
        $null = $sb.AppendLine('<!doctype html>')
        $null = $sb.AppendLine("<html lang=`"$locale`" dir=`"$dir`">")
        $null = $sb.AppendLine('<head>')
        $null = $sb.AppendLine('<meta charset="utf-8">')
        $null = $sb.AppendLine('<meta name="viewport" content="width=device-width, initial-scale=1">')
        $null = $sb.AppendLine('<meta name="referrer" content="no-referrer">')
        $null = $sb.AppendLine("<title>$(ConvertTo-HtmlText $chrome.pageTitle)</title>")
        $null = $sb.AppendLine('<script>(function(){try{var q=new URLSearchParams(location.search).get("theme");var t=(q==="light"||q==="dark")?q:(matchMedia("(prefers-color-scheme: light)").matches?"light":"dark");document.documentElement.setAttribute("data-theme",t);}catch(e){}})();</script>')
        $null = $sb.AppendLine('<link rel="stylesheet" href="../sza-kit.css">')
        $null = $sb.AppendLine('<link rel="stylesheet" href="../help.css">')
        $null = $sb.AppendLine('</head>')
        $null = $sb.AppendLine('<body class="help">')
        $null = $sb.AppendLine('<main class="container">')
        $null = $sb.AppendLine('<header class="hero">')
        $null = $sb.AppendLine('  <p class="kicker">' + (ConvertTo-HtmlText ($product + ' - ' + $chrome.indexKicker)) + '</p>')
        $null = $sb.AppendLine('  <h1>' + (ConvertTo-HtmlText $chrome.pageTitle) + '</h1>')
        $null = $sb.AppendLine('  <p class="lead">' + (Convert-Prose $iblock.lead) + '</p>')
        $null = $sb.AppendLine('</header>')
        foreach ($group in @(@('screen', $chrome.indexScreens), @('dialog', $chrome.indexWindows))) {
            $members = @($map.surfaces | Where-Object { (Get-Prop $_ 'form') -eq 'page' -and (Get-SurfaceKind $_) -eq $group[0] })
            if ($members.Count -eq 0) { continue }
            $null = $sb.AppendLine('<h2>' + (ConvertTo-HtmlText $group[1]) + '</h2>')
            $null = $sb.AppendLine('<ul class="related">')
            foreach ($m in $members) {
                $summary = Get-Prop (Get-Prop (Get-Prop (Read-Json (Join-Path $script:SrcRoot "$($m.help).json")) 'locales') $locale) 'summary' ''
                $pill = if ($group[0] -eq 'dialog') { $chrome.pillWindow } else { $chrome.pillScreen }
                $null = $sb.AppendLine("  <li><a href=`"$($m.help).html`"><b>$(ConvertTo-HtmlText (Get-HelpTitle $m.help $strings))</b><span class=`"pill`">$(ConvertTo-HtmlText $pill)</span><small>$(Convert-Prose $summary)</small></a></li>")
            }
            $null = $sb.AppendLine('</ul>')
        }
        $foot = $chrome.footer.Replace('{product}', $product).Replace('{version}', $version).Replace('{date}', $captured)
        $null = $sb.AppendLine('<footer class="help-foot">')
        $null = $sb.AppendLine('  <span>' + (ConvertTo-HtmlText $foot) + '</span>')
        $support = Get-Prop (Get-Prop (Read-Json (Join-Path $PSScriptRoot '..\..\docs\help\help.config.json')) 'support') 'url'
        if ($support) { $null = $sb.AppendLine('  <a href="' + (ConvertTo-HtmlText $support) + '">' + (ConvertTo-HtmlText $chrome.support) + '</a>') }
        $null = $sb.AppendLine('</footer>')
        $null = $sb.AppendLine('</main>')
        $null = $sb.AppendLine('</body>')
        $null = $sb.AppendLine('</html>')
        Write-Utf8 (Join-Path $script:AssetHelp "$locale\index.html") $sb.ToString()
        Write-Host '  index.html'
    }
}

if ($script:Problems.Count -gt 0) {
    Write-Host "help-build: FAIL ($($script:Problems.Count) problem(s))"
    exit 1
}
Write-Host "help-build: PASS ($($pageSurfaces.Count) page(s), $($Locales.Count) locale(s))"
exit 0
