#requires -Version 7.0
<#
.SYNOPSIS
    Self-test of the string-table gate (S0042): it must fail on planted damage and pass on clean text.
.DESCRIPTION
    In-process cases drive Test-StringTableBytes with text damaged the way the suspected writer damages it
    (a UTF-8 file read through a single-byte code page and written back as UTF-8), through every reader of
    lib.ps1, and with clean Russian, Ukrainian and English text that must stay unflagged. Two child-process
    cases prove the exit codes and the file discovery of check-string-tables.ps1 on throw-away trees:
    damage in wear and watchface tables, in other locales, is found; a clean tree passes; an empty one is exit 2.
    Scratch lives under the system temp directory and is removed at the end.
.NOTES
    Exit codes: 0 PASS, 1 FAIL.
#>
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib.ps1')
$checker = Join-Path $PSScriptRoot 'check-string-tables.ps1'
$utf8 = [Text.UTF8Encoding]::new($false)
$failures = [Collections.Generic.List[string]]::new()
$cases = 0

function Assert-Case([string]$Name, [bool]$Ok, [string]$Detail = '') {
    $script:cases++
    if ($Ok) { Write-Host "  PASS  $Name" } else { Write-Host "  FAIL  $Name $Detail"; $script:failures.Add($Name) }
}
function Get-Table([string]$Body) { return $utf8.GetBytes("<resources>`n$Body`n</resources>`n") }
function ConvertTo-Mojibake([string]$Text, [int]$Page) {
    # The suspected writer: UTF-8 bytes read through a single-byte page, the text written back as UTF-8.
    return $utf8.GetBytes([Text.Encoding]::GetEncoding($Page).GetString($utf8.GetBytes($Text)))
}
function Get-Rules([byte[]]$Bytes) { return @(Test-StringTableBytes -Bytes $Bytes | ForEach-Object Rule | Sort-Object -Unique) }

# This file stays ASCII: every non-ASCII character is written as hex code points, so no editor or
# Get-Content round trip can damage the test data itself. U '0412 0456' is the two characters U+0412 U+0456.
function U([string]$Hex) { return -join ($Hex -split ' ' | ForEach-Object { [char][Convert]::ToInt32($_, 16) }) }
$deg = U 'B0'
$bul = U '2022'
$clean = [ordered]@{
    'english symbols'   = "<string name=`"a`">20 ${deg}C $bul 5 $(U '2026 2013 2014 2019 201C') x $(U '201D D7 B1 B5') T $(U 'B2 B3 BD')</string>"
    # Capital V + i/I pairs read through cp1251 are U+00B2/U+00B3, the superscripts: a signature that must never exist.
    'ukrainian words'   = "<string name=`"a`">$(U '0412 0456 043D') $(U '0412 0456 0442 0435 0440') $(U '0412 0406 0414 0421 0422 0410 041D 042C') $(U '0422 0456') $(U '041D 0456 0447') $(U '0420 0456 0432 043D 043E') $(U '0420 0456 0437 0434 0432 043E') $(U '0421 0456 043C') $(U '0421 0456 043B 044C') $(U '0490 0430 043D 043E 043A') $(U '0407 0436 0430 043A') $(U '0404') $deg $bul</string>"
    'russian words'     = "<string name=`"a`">$(U '0412 0451 0440 0441 0442 043A 0430') $(U '0412 0401 0420 0421 0422 041A 0410') $(U '0412 00A0 043B 0435 0441 0443') $(U '041F 0440 0438 0432 0435 0442') $(U '0420 0430 0441 0441 0442 043E 044F 043D 0438 0435') $(U '0421 043A 043E 0440 043E 0441 0442 044C') $deg $bul</string>"
    'escaped reference' = "<string name=`"a`">&#xB0; &#x2022; $([char]92)u00B0</string>"
}
Write-Host 'Clean text stays unflagged:'
foreach ($name in $clean.Keys) {
    $rules = Get-Rules (Get-Table $clean[$name])
    Assert-Case "clean: $name" ($rules.Count -eq 0) "(rules: $($rules -join ', '))"
}
Assert-Case 'clean: an empty file' (@(Test-StringTableBytes -Bytes ([byte[]]@())).Count -eq 0)

Write-Host 'The S0041 sequences, byte for byte:'
$degreeBytes = [byte[]](0xD0, 0x92, 0xC2, 0xB0)
$bulletBytes = [byte[]](0xD0, 0xB2, 0xD0, 0x82, 0xD1, 0x9E)
$head = $utf8.GetBytes("<resources>`n<string name=`"u`">")
$tail = $utf8.GetBytes("</string>`n</resources>`n")
Assert-Case 'planted degree D0 92 C2 B0' ((Get-Rules ([byte[]]($head + $degreeBytes + $tail))) -contains 'signature')
Assert-Case 'planted bullet D0 B2 D0 82 D1 9E' ((Get-Rules ([byte[]]($head + $bulletBytes + $tail))) -contains 'signature')
$finding = @(Test-StringTableBytes -Bytes ([byte[]]($head + $degreeBytes + $tail)))[0]
Assert-Case 'finding names the line and the key' ($finding.Line -eq 2 -and $finding.Key -eq 'u') "(line $($finding.Line), key '$($finding.Key)')"

Write-Host 'Every reader of lib.ps1, every symbol of the editing alphabet:'
$symbols = @('B0', '2022', '2026', '2013', '2014', '2019', '201C', '201D', '20AC', '2122', 'AB', 'BB', 'D7', 'B1', 'B5', '2116', 'E9', 'FC', '2190', '2192', '2248' | ForEach-Object { U $_ })
foreach ($reader in $script:ReaderPages.Keys) {
    $missed = @($symbols | Where-Object { (Get-Rules (ConvertTo-Mojibake "<string name=`"a`">x ${_} y</string>" $script:ReaderPages[$reader])).Count -eq 0 })
    Assert-Case "planted via $reader ($($symbols.Count) symbols)" ($missed.Count -eq 0) "(missed: $($missed -join ' '))"
}
$lost = [byte[]]($utf8.GetBytes("<string name=`"a`">x ") + [byte[]](0xC3, 0xA2, 0xE2, 0x82, 0xAC) + $utf8.GetBytes(" y</string>"))
Assert-Case 'planted cp1252 sequence that lost its last byte (U+00E2 U+20AC)' ((Get-Rules $lost) -contains 'signature')

Write-Host 'Whole-text damage:'
$samples = @(
    @('russian', (U '0421 043A 043E 0440 043E 0441 0442 044C 0020 0438 0020 0440 0430 0441 0441 0442 043E 044F 043D 0438 0435 0020 0434 043E 0020 0446 0435 043B 0438')),
    @('ukrainian', (U '0412 0456 0442 0435 0440 0020 0442 0430 0020 0448 0432 0438 0434 043A 0456 0441 0442 044C 0020 0440 0443 0445 0443')))
foreach ($sample in $samples) {
    foreach ($reader in 'cp1251', 'cp1252', 'cp866') {
        $rules = Get-Rules (ConvertTo-Mojibake "<string name=`"a`">$($sample[1])</string>" $script:ReaderPages[$reader])
        Assert-Case "planted $($sample[0]) text via $reader" ($rules.Count -gt 0) '(no finding)'
    }
}

Write-Host 'Encoding faults:'
Assert-Case 'invalid UTF-8 (a lone B0 byte, a cp1251 file)' ((Get-Rules ([byte[]]($head + [byte[]](0x32, 0x30, 0x20, 0xB0, 0x43) + $tail))) -contains 'utf8')
Assert-Case 'U+FFFD replacement character' ((Get-Rules ([byte[]]($head + [byte[]](0xEF, 0xBF, 0xBD) + $tail))) -contains 'replacement')
Assert-Case 'C1 control character U+0081' ((Get-Rules ([byte[]]($head + [byte[]](0xC2, 0x81) + $tail))) -contains 'c1')
Assert-Case 'UTF-8 BOM' ((Get-Rules ([byte[]]([byte[]](0xEF, 0xBB, 0xBF) + (Get-Table '<string name="a">x</string>')))) -contains 'bom')
$utf16 = [Text.Encoding]::Unicode.GetBytes((Get-Table '<string name="a">x</string>'))
Assert-Case 'UTF-16 file with a BOM' ((Get-Rules ([byte[]]([byte[]](0xFF, 0xFE) + $utf16))) -contains 'utf8')
Assert-Case 'UTF-16 file without a BOM (NUL characters)' ((Get-Rules ([byte[]]$utf16)) -contains 'nul')

Write-Host 'Discovery and exit codes of check-string-tables.ps1:'
$scratch = Join-Path ([IO.Path]::GetTempPath()) "string-tables-selftest-$([guid]::NewGuid().ToString('N'))"
try {
    function New-Tree([string]$Name, [hashtable]$Overrides) {
        $root = Join-Path $scratch $Name
        foreach ($module in 'phone', 'wear', 'watchface') {
            foreach ($locale in 'values', 'values-ru', 'values-uk') {
                $dir = Join-Path $root "$module/src/main/res/$locale"
                New-Item -ItemType Directory -Force $dir | Out-Null
                $bytes = if ($Overrides.ContainsKey("$module/$locale")) { $Overrides["$module/$locale"] } else { Get-Table "<string name=`"a`">20 ${deg}C $bul</string>" }
                [IO.File]::WriteAllBytes((Join-Path $dir 'strings.xml'), $bytes)
            }
        }
        return $root
    }
    $dirty = New-Tree 'dirty' @{
        'wear/values-uk'      = [byte[]]($head + $degreeBytes + $tail)
        'watchface/values-ru' = [byte[]]($head + $bulletBytes + $tail)
    }
    $out = & pwsh -NoProfile -File $checker -Root $dirty 2>&1 | Out-String
    $exit = $LASTEXITCODE
    Assert-Case 'planted damage in wear and watchface: exit 1' ($exit -eq 1) "(exit $exit)"
    Assert-Case 'both damaged files are named' ($out -match 'wear/src/main/res/values-uk/strings\.xml:2' -and $out -match 'watchface/src/main/res/values-ru/strings\.xml:2') "`n$out"
    Assert-Case 'the verdict line says FAIL' ($out -match 'string-tables: FAIL \(2 finding') "`n$out"
    $good = New-Tree 'clean' @{}
    $out = & pwsh -NoProfile -File $checker -Root $good 2>&1 | Out-String
    $exit = $LASTEXITCODE
    Assert-Case 'clean tree of nine tables: exit 0' ($exit -eq 0 -and $out -match 'Checked 9 string table') "(exit $exit)`n$out"
    $empty = Join-Path $scratch 'empty'
    New-Item -ItemType Directory -Force $empty | Out-Null
    $null = & pwsh -NoProfile -File $checker -Root $empty 2>&1
    Assert-Case 'tree without tables: exit 2' ($LASTEXITCODE -eq 2) "(exit $LASTEXITCODE)"
    $null = & pwsh -NoProfile -File $checker -Path (Join-Path $scratch 'missing.xml') 2>&1
    Assert-Case 'missing -Path file: exit 2' ($LASTEXITCODE -eq 2) "(exit $LASTEXITCODE)"
} finally {
    if (Test-Path -LiteralPath $scratch) { Remove-Item -LiteralPath $scratch -Recurse -Force }
}

Write-Host "string-tables-selftest: $(if ($failures.Count) { "FAIL ($($failures.Count) of $cases cases: $($failures -join '; '))" } else { "PASS ($cases cases)" })"
exit $(if ($failures.Count) { 1 } else { 0 })
