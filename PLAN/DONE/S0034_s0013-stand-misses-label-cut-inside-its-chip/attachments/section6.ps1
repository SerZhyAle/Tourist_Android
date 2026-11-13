[Console]::OutputEncoding = [Text.Encoding]::UTF8
$stand = 'P:\ANDROID\Tourist_mob\PLAN\S0013_deep-dynamic-audit\stand'
. (Join-Path $stand '_stand.ps1')
. (Join-Path $stand '_instruments.ps1')
$results = [Collections.Generic.List[object]]::new()
function Check([string] $Name, [scriptblock] $Test) {
    try { $ok = [bool](& $Test); $why = '' } catch { $ok = $false; $why = $_.Exception.Message }
    $results.Add([pscustomobject]@{ name = $Name; ok = $ok; why = $why })
}
# ---------------------------------------------------------------------------------------------------------
# 6. M7 clipping reader (S0034)
# ---------------------------------------------------------------------------------------------------------

# A watch dump (density 320): a scrolling list holding a top chip and two reset chips, shaped as the
# 2026-10-25 C2 watch dumps. Each chip is "bounds|label bounds|label", an empty label bounds drops the text.
function New-ChipDump([string[]] $Chips) {
    $a = "package=`"$script:Package`" visible-to-user=`"true`""
    $i = 0
    $nodes = foreach ($c in $Chips) {
        $cb, $tb, $t = $c -split '\|'
        $label = if ($tb) { "<node index=`"0`" text=`"$t`" class=`"android.widget.TextView`" clickable=`"false`" $a bounds=`"$tb`" />" }
        "<node index=`"$i`" text=`"`" class=`"android.view.View`" clickable=`"true`" $a bounds=`"$cb`">$label</node>"
        $i++
    }
    [xml]"<hierarchy><node index=`"0`" text=`"`" class=`"android.widget.FrameLayout`" clickable=`"false`" $a bounds=`"[0,0][384,384]`"><node index=`"0`" text=`"`" class=`"android.view.View`" clickable=`"false`" scrollable=`"true`" $a bounds=`"[0,0][384,384]`">$($nodes -join '')</node></node></hierarchy>"
}
$base = New-ChipDump '[52,0][332,192]|[159,15][227,42]|Speed', '[46,207][182,298]|[69,241][159,264]|Reset Trip', '[193,207][338,298]|[216,228][315,277]|Reset Steps'
$narrow = New-ChipDump '[46,0][338,276]||', '[68,290][252,368]|[87,310][236,348]|Reset Trip', '[252,290][330,368]|[285,303][297,355]|Reset Steps'
$dropped = New-ChipDump '[46,0][338,276]||', '[69,295][264,372]|[88,315][261,353]|Reset Trip', '[264,295][341,372]||'
Check 'a label narrower than 3 sp per character of its longest word is clipped, a wide one is not' {
    $c = @((Get-A11yFacts -Dump $narrow -DensityDpi 320 -MinDp 40 -FontScale 2.0).clipped)
    $c.Count -eq 1 -and $c[0] -eq 'Reset Steps'
}
Check 'without a font scale the width rule is off (the pre-S0034 reader misses the narrow label)' { @((Get-A11yFacts -Dump $narrow -DensityDpi 320 -MinDp 40).clipped).Count -eq 0 }
Check 'a label the large font dropped from a whole chip is clipped, a chip scrolled to the edge is not' {
    $d = @(Get-DroppedLabels -Base $base -Large $dropped)
    $d.Count -eq 1 -and $d[0] -eq 'Reset Steps'
}
Check 'a label still present at the large font is not reported dropped' { @(Get-DroppedLabels -Base $base -Large $narrow).Count -eq 0 }

foreach ($r in $results) { "  {0} {1}{2}" -f $(if ($r.ok) { 'ok  ' } else { 'FAIL' }), $r.name, $(if ($r.why) { " - $($r.why)" } else { '' }) }
exit @($results | Where-Object { -not $_.ok }).Count
