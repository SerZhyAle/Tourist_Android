#requires -Version 7.0
<#
.SYNOPSIS
    Checks the source-level unified UI invariants. Does not claim rendered conformance.+.DESCRIPTION
    Exit 0 PASS, 1 FAIL. Run before the device matrix; no Gradle or device is needed.
#>
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$main = Join-Path $root 'phone/src/main'
$issues = [Collections.Generic.List[string]]::new()
$android = 'http://schemas.android.com/apk/res/android'
$buttons = 0
$checkboxes = 0
foreach ($file in Get-ChildItem -LiteralPath (Join-Path $main 'res') -Recurse -Filter '*.xml') {
    $xml = [xml](Get-Content -LiteralPath $file.FullName -Raw)
    foreach ($node in $xml.SelectNodes('//com.google.android.material.button.MaterialButton')) {
        $buttons++
        $width = $node.GetAttribute('layout_width', $android)
        if ($width -notin @('wrap_content', '@dimen/touch_target_min_height')) {
            $issues.Add("$($file.Name): command has non-content width $width")
        }
        if ($node.GetAttribute('layout_weight', $android)) { $issues.Add("$($file.Name): weighted command") }
    }
    $checkboxes += $xml.SelectNodes('//com.sza.tourist.ui.common.widget.SettingsCheckboxRow').Count
    if ($xml.SelectNodes('//com.sza.tourist.ui.common.widget.SettingsToggleRow').Count) { $issues.Add("$($file.Name): retired switch row") }
}
if ($buttons -eq 0 -or $checkboxes -ne 4) { $issues.Add("Unexpected surface count: buttons=$buttons checkboxes=$checkboxes") }
$localeKeys = @{}
foreach ($locale in @('values', 'values-ru', 'values-uk')) {
    $xml = [xml](Get-Content -LiteralPath (Join-Path $main "res/$locale/strings.xml") -Raw)
    $keys = @($xml.resources.string | ForEach-Object { $_.name })
    $duplicates = @($keys | Group-Object | Where-Object Count -gt 1)
    if ($duplicates.Count) { $issues.Add("${locale}: duplicate string keys") }
    $localeKeys[$locale] = @($keys | Where-Object { $_ -match '^(settings_|group_state_)' } | Sort-Object)
}
foreach ($locale in @('values-ru', 'values-uk')) {
    if (Compare-Object $localeKeys.values $localeKeys[$locale]) { $issues.Add("${locale}: settings locale parity differs") }
}
$map = Get-Content -LiteralPath (Join-Path $main 'assets/help/help-map.json') -Raw | ConvertFrom-Json
if (@($map.surfaces | Where-Object id -eq 'settings').Count -ne 1) { $issues.Add('Settings help map entry missing or duplicated') }
$face = [xml](Get-Content -LiteralPath (Join-Path $root 'watchface/src/main/AndroidManifest.xml') -Raw)
if ($face.SelectNodes('//activity').Count) { $issues.Add('Watch-face configuration activity requires a new inventory review') }
foreach ($issue in $issues) { Write-Host "FAIL: $issue" }
Write-Host "Checked $buttons XML command buttons, $checkboxes migrated boolean rows, three authored locales and watch-face applicability."
Write-Host "unified-ui-source: $(if ($issues.Count) { 'FAIL' } else { 'PASS' })"
exit $(if ($issues.Count) { 1 } else { 0 })
