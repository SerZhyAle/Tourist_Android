#requires -Version 7.0
<#
.SYNOPSIS
    Shared helpers of the in-app help tooling (S0014, CONTEXT-HELP): paths, the help map, Android string
    resources, text escaping. Dot-sourced by build-help.ps1, capture-help.ps1 and check-help.ps1.
#>

Set-StrictMode -Version Latest

$script:RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$script:PhoneMain = Join-Path $script:RepoRoot 'phone\src\main'
$script:AssetHelp = Join-Path $script:PhoneMain 'assets\help'
$script:MapFile = Join-Path $script:AssetHelp 'help-map.json'
$script:SrcRoot = Join-Path $script:RepoRoot 'docs\help\src'
$script:ShotsRoot = Join-Path $script:RepoRoot 'docs\help\shots'
$script:BaselineFile = Join-Path $script:RepoRoot 'docs\help\baseline.json'
$script:ResRoot = Join-Path $script:PhoneMain 'res'
$script:ThemeNames = @('light', 'dark')

function Get-Prop {
    # A member of a parsed JSON object, or $Default when it is absent or null.
    param($Object, [string]$Name, $Default = $null)
    if ($null -eq $Object) { return $Default }
    $p = $Object.PSObject.Properties[$Name]
    if ($null -eq $p -or $null -eq $p.Value) { return $Default }
    return $p.Value
}

function Read-Json {
    param([Parameter(Mandatory)][string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) { throw "Missing file: $Path" }
    return Get-Content -LiteralPath $Path -Raw -Encoding utf8 | ConvertFrom-Json
}

function Write-Utf8 {
    # UTF-8 without a BOM and with LF line endings, whatever the host defaults are.
    param([Parameter(Mandatory)][string]$Path, [Parameter(Mandatory)][string]$Text)
    $dir = Split-Path -Parent $Path
    if (-not (Test-Path -LiteralPath $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    $normal = $Text -replace "`r`n", "`n"
    [System.IO.File]::WriteAllText($Path, $normal, (New-Object System.Text.UTF8Encoding($false)))
}

function Read-HelpMap { return Read-Json $script:MapFile }

function Get-SurfaceKind {
    # 'dialog' for a DialogFragment or a call-site dialog, 'screen' otherwise.
    param($Surface)
    $host_ = [string](Get-Prop $Surface 'host' '')
    if ($host_ -match '#' -or $host_ -match 'Dialog') { return 'dialog' }
    return 'screen'
}

function Get-ShippedLocales {
    # The UI's locale set: the source locale plus every values-xx directory with a strings file.
    $found = @('en')
    foreach ($d in Get-ChildItem -LiteralPath $script:ResRoot -Directory -Filter 'values-*') {
        if ($d.Name -match '^values-([a-z]{2,3})$' -and (Test-Path (Join-Path $d.FullName 'strings.xml'))) {
            $found += $Matches[1]
        }
    }
    return $found
}

function ConvertFrom-AndroidString {
    # Undoes the escapes of a strings.xml value: \' \" \n \\ \@ \? \uXXXX, and the quoting of a whole value.
    param([string]$Raw)
    $s = $Raw
    if ($s.Length -ge 2 -and $s.StartsWith('"') -and $s.EndsWith('"')) { $s = $s.Substring(1, $s.Length - 2) }
    $sb = New-Object System.Text.StringBuilder
    for ($i = 0; $i -lt $s.Length; $i++) {
        $c = $s[$i]
        if ($c -eq '\' -and $i + 1 -lt $s.Length) {
            $n = $s[$i + 1]
            switch -CaseSensitive ($n) {
                'n' { [void]$sb.Append("`n"); $i++ }
                't' { [void]$sb.Append("`t"); $i++ }
                'u' {
                    if ($i + 6 -le $s.Length -and $s.Substring($i + 2, 4) -match '^[0-9a-fA-F]{4}$') {
                        [void]$sb.Append([char][Convert]::ToInt32($s.Substring($i + 2, 4), 16)); $i += 5
                    } else { [void]$sb.Append($n); $i++ }
                }
                default { [void]$sb.Append($n); $i++ }
            }
        } else { [void]$sb.Append($c) }
    }
    return $sb.ToString()
}

function Read-Strings {
    # name -> text for a locale, the source locale underneath: what the UI shows in that language.
    param([Parameter(Mandatory)][string]$Locale)
    $table = @{}
    $files = @(Join-Path $script:ResRoot 'values\strings.xml')
    if ($Locale -ne 'en') { $files += (Join-Path $script:ResRoot "values-$Locale\strings.xml") }
    foreach ($f in $files) {
        if (-not (Test-Path -LiteralPath $f)) { continue }
        $xml = New-Object System.Xml.XmlDocument
        $xml.Load($f)
        foreach ($node in $xml.SelectNodes('/resources/string')) {
            $table[$node.GetAttribute('name')] = ConvertFrom-AndroidString $node.InnerText
        }
    }
    return $table
}

function ConvertTo-HtmlText {
    param([string]$Text)
    return [System.Net.WebUtility]::HtmlEncode($Text)
}

function ConvertTo-HtmlInline {
    # Page prose: escaped, with the two marks the sources use - **bold** and [[Key]] for a keyboard key.
    param([string]$Text)
    $h = ConvertTo-HtmlText $Text
    $h = [regex]::Replace($h, '\*\*(.+?)\*\*', '<b>$1</b>')
    $h = [regex]::Replace($h, '\[\[(.+?)\]\]', '<kbd>$1</kbd>')
    return $h
}

function Get-FileSha256 {
    param([Parameter(Mandatory)][string]$Path)
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}

function Get-LayoutsHash {
    # Include styles/tokens and the programmatically composed settings pages as rendering inputs.
    $parts = Get-ChildItem -LiteralPath $script:ResRoot -Recurse -File -Include '*.xml' |
        Where-Object { $_.Directory.Name -like 'layout*' -or $_.Directory.Name -like 'drawable*' -or $_.Directory.Name -eq 'menu' -or
            ($_.Directory.Name -like 'values*' -and $_.Name -ne 'strings.xml') } |
        Sort-Object FullName |
        ForEach-Object { $_.FullName.Substring($script:ResRoot.Length) + ':' + (Get-FileSha256 $_.FullName) }
    foreach ($source in @('SettingsActivity.kt', 'SettingsPages.kt')) {
        $file = Join-Path $script:PhoneMain "java/com/sza/tourist/ui/settings/$source"
        $parts += $source + ':' + (Get-FileSha256 $file)
    }
    $bytes = [System.Text.Encoding]::UTF8.GetBytes(($parts -join "`n"))
    return ([System.BitConverter]::ToString([System.Security.Cryptography.SHA256]::HashData($bytes)) -replace '-', '').ToLowerInvariant()
}

function Get-StringsHash {
    # The strings the page names and the screenshot's labels come from, for one locale.
    param([Parameter(Mandatory)][string]$Locale)
    $files = @(Join-Path $script:ResRoot 'values\strings.xml')
    if ($Locale -ne 'en') { $files += (Join-Path $script:ResRoot "values-$Locale\strings.xml") }
    $parts = $files | ForEach-Object { Get-FileSha256 $_ }
    $bytes = [System.Text.Encoding]::UTF8.GetBytes(($parts -join "`n"))
    return ([System.BitConverter]::ToString([System.Security.Cryptography.SHA256]::HashData($bytes)) -replace '-', '').ToLowerInvariant()
}
