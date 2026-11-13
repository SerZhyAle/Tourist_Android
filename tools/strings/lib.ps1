#requires -Version 7.0
<#
.SYNOPSIS
    Library of the string-table gate (S0042): finds double-encoded (mojibake) text in Android value tables.
.DESCRIPTION
    Dot-sourced by check-string-tables.ps1 and test-string-tables.ps1; defines functions, runs nothing.

    The damage model: a writer reads a UTF-8 file through a single-byte code page (Windows PowerShell 5.1
    Get-Content without -Encoding reads the system ANSI page, cp1251 on this machine) and writes the text
    back as UTF-8. Every non-ASCII character then becomes two or three others: the degree sign C2 B0 turns
    into U+0412 U+00B0, the bullet E2 80 A2 into U+0432 U+0402 U+045E.

    Rules, one finding each:
      bom          the file starts with a UTF-8 BOM (the table convention is none; Set-Content -Encoding UTF8
                   of Windows PowerShell 5.1 writes one)
      utf8         the bytes are not valid UTF-8 (a table saved in a single-byte code page)
      replacement  U+FFFD is present (a lossy re-encode already happened)
      nul          a NUL character is present (a UTF-16 file written without a BOM)
      c1           a C1 control character U+0080..U+009F is present (a Latin-1 reading of a continuation byte)
      signature    a double-encoded typographic symbol, generated for the five readers below from a symbol
                   alphabet (degree, bullet, dashes, quotes, ellipsis, arrows, Latin-1 punctuation and
                   letters); a three-byte symbol is also caught by its first two bytes, so a sequence that
                   lost its last byte is still found
      cyrillic     three or more Cyrillic letters in a row, each read as two characters
    A signature that is only Russian and Ukrainian letters in a natural case shape is dropped by construction:
    the superscript two read through cp1251 is U+0412 U+0406, which is also the capital pair that opens the
    Ukrainian word "VIDSTAN", so a table can never be judged on it. The same holds for the superscript three
    and a few more; the same sequences read through another code page stay signatures.
#>

[Text.Encoding]::RegisterProvider([Text.CodePagesEncodingProvider]::Instance)

$script:StrictUtf8 = [Text.UTF8Encoding]::new($false, $true)
# The code pages a Windows writer of this machine can read UTF-8 through: ANSI (Cyrillic and Western),
# Central European, the OEM console page and ISO-8859-1.
$script:ReaderPages = [ordered]@{ cp1251 = 1251; cp1252 = 1252; cp1250 = 1250; cp866 = 866; latin1 = 28591 }
# The Gradle modules of CLAUDE.md section 3; every source set of each is judged.
$script:TableModules = @('phone', 'wear', 'watchface')
$script:Signatures = $null

function Format-CodePoints([string]$Text) {
    return (($Text.ToCharArray() | ForEach-Object { 'U+{0:X4}' -f [int]$_ }) -join ' ')
}

function Get-StringTableFiles {
    <# Every XML file of every values* directory of every source set of the three modules. #>
    param([Parameter(Mandatory)][string]$Root)
    foreach ($module in $script:TableModules) {
        $src = Join-Path $Root "$module/src"
        if (-not (Test-Path -LiteralPath $src -PathType Container)) { continue }
        foreach ($set in Get-ChildItem -LiteralPath $src -Directory) {
            $res = Join-Path $set.FullName 'res'
            if (-not (Test-Path -LiteralPath $res -PathType Container)) { continue }
            foreach ($dir in Get-ChildItem -LiteralPath $res -Directory -Filter 'values*') {
                Get-ChildItem -LiteralPath $dir.FullName -Filter '*.xml' -File
            }
        }
    }
}

function Initialize-StringTableSignatures {
    if ($script:Signatures) { return }
    # A sequence that is only Russian/Ukrainian letters in a natural case shape (all lower, all upper, or a
    # capital and lower case) can be an ordinary word and is never a signature. Mixed shapes such as
    # lower-upper-lower (the cp866 reading of a bullet) cannot, and stay.
    # The patterns are assembled from code points at run time, so this file stays ASCII.
    $extraUpper = -join (@(0x401, 0x406, 0x407, 0x404, 0x490) | ForEach-Object { [char]$_ })
    $extraLower = -join (@(0x451, 0x456, 0x457, 0x454, 0x491) | ForEach-Object { [char]$_ })
    $upper = "$([char]0x410)-$([char]0x42F)$extraUpper"
    $lower = "$([char]0x430)-$([char]0x44F)$extraLower"
    $naturalWord = "^(?:[$upper]+|[$upper]?[$lower]+)$"
    $unusable = "[$([char]0x80)-$([char]0x9F)$([char]0xFFFD)]"
    $symbols = [Collections.Generic.List[int]]::new()
    $ranges = @((0xA1..0xAC), (0xAE..0xFF), (0x2010..0x2027), (0x2030..0x203A), @(0x20AC, 0x2116, 0x2122),
        (0x2190..0x2193), @(0x2212, 0x2248, 0x2260, 0x2264, 0x2265, 0x25CF, 0x25B2, 0x25BC))
    foreach ($range in $ranges) { foreach ($cp in $range) { $symbols.Add($cp) } }
    $cyrillic = @(0x410..0x44F) + @(0x401, 0x451, 0x406, 0x456, 0x407, 0x457, 0x404, 0x454, 0x490, 0x491)

    # Ordinal: a plain @{} compares keys case-insensitively, and the bullet's cp1251 reading (..U+045E) would
    # collapse into the double dagger's (..U+040E).
    $symbolSigs = [Collections.Generic.Dictionary[string, string]]::new([StringComparer]::Ordinal)
    $pairRegex = [ordered]@{}
    foreach ($reader in $script:ReaderPages.Keys) {
        $enc = [Text.Encoding]::GetEncoding($script:ReaderPages[$reader])
        foreach ($cp in $symbols) {
            $bytes = [Text.Encoding]::UTF8.GetBytes([char]::ConvertFromUtf32($cp))
            $sig = $enc.GetString($bytes)
            if ($sig -notmatch $unusable -and $sig -cnotmatch $naturalWord -and -not $symbolSigs.ContainsKey($sig)) {
                $symbolSigs[$sig] = "$reader reading of U+{0:X4}" -f $cp
            }
            if ($bytes.Length -eq 3) {
                $head = $enc.GetString($bytes, 0, 2)
                if ($head -notmatch $unusable -and $head -cnotmatch $naturalWord -and -not $symbolSigs.ContainsKey($head)) {
                    $symbolSigs[$head] = "$reader reading of the first two bytes of a three-byte symbol (U+{0:X4}..)" -f $cp
                }
            }
        }
        $pairs = @($cyrillic | ForEach-Object { $enc.GetString([Text.Encoding]::UTF8.GetBytes([char]::ConvertFromUtf32($_))) } |
                Where-Object { $_ -notmatch $unusable } | Sort-Object -Unique -CaseSensitive | ForEach-Object { [regex]::Escape($_) })
        if ($pairs.Count) {
            $pairRegex[$reader] = [regex]::new("(?:$($pairs -join '|')){3,}", [Text.RegularExpressions.RegexOptions]::CultureInvariant)
        }
    }
    $ordered = @($symbolSigs.Keys | Sort-Object { $_.Length } -Descending | ForEach-Object { [regex]::Escape($_) })
    $script:Signatures = [pscustomobject]@{
        Names = $symbolSigs
        Symbol = [regex]::new(($ordered -join '|'), [Text.RegularExpressions.RegexOptions]::CultureInvariant)
        Cyrillic = $pairRegex
        C1 = [regex]::new("[$([char]0x80)-$([char]0x9F)]")
        NonAscii = [regex]::new('[^\x00-\x7F]')
    }
}

function Test-StringTableBytes {
    <# Judges the bytes of one table. Returns one object per finding: Line, Key, Rule, Detail. #>
    param([Parameter(Mandatory)][AllowEmptyCollection()][byte[]]$Bytes)
    Initialize-StringTableSignatures
    $sig = $script:Signatures
    $found = [Collections.Generic.List[object]]::new()
    $add = { param($Line, $Key, $Rule, $Detail) $found.Add([pscustomobject]@{ Line = $Line; Key = $Key; Rule = $Rule; Detail = $Detail }) }

    if ($Bytes.Length -ge 3 -and $Bytes[0] -eq 0xEF -and $Bytes[1] -eq 0xBB -and $Bytes[2] -eq 0xBF) {
        & $add 1 '' 'bom' 'UTF-8 BOM at the start of the file (Windows PowerShell 5.1 Set-Content -Encoding UTF8 writes one; the tables carry none)'
    }
    try {
        $text = $script:StrictUtf8.GetString($Bytes)
    } catch {
        $inner = $_.Exception.InnerException
        $offset = if ($inner -is [Text.DecoderFallbackException]) { $inner.Index } else { 0 }
        $line = 1 + @($Bytes[0..([Math]::Max(0, $offset - 1))] | Where-Object { $_ -eq 10 }).Count
        & $add $line '' 'utf8' "not valid UTF-8 at byte $offset (a table saved in a single-byte code page)"
        return $found
    }
    $nul = $text.IndexOf([char]0)
    if ($nul -ge 0) {
        & $add (1 + ($text.Substring(0, $nul).Split("`n").Count - 1)) '' 'nul' 'NUL character (a UTF-16 file without a BOM read as UTF-8)'
    }
    $lines = $text -split "`n"
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $text1 = $lines[$i]
        if (-not $sig.NonAscii.IsMatch($text1)) { continue }
        $n = $i + 1
        $key = [regex]::Match($text1, 'name="([^"]+)"').Groups[1].Value
        if ($text1.Contains([char]0xFFFD)) { & $add $n $key 'replacement' 'U+FFFD replacement character (a lossy re-encode already happened)' }
        if ($sig.C1.IsMatch($text1)) { & $add $n $key 'c1' 'C1 control character U+0080..U+009F (a Latin-1 reading of a UTF-8 continuation byte)' }
        foreach ($m in $sig.Symbol.Matches($text1)) {
            & $add $n $key 'signature' "double-encoded: $($sig.Names[$m.Value]) ($(Format-CodePoints $m.Value))"
        }
        foreach ($reader in $sig.Cyrillic.Keys) {
            foreach ($m in $sig.Cyrillic[$reader].Matches($text1)) {
                & $add $n $key 'cyrillic' "double-encoded Cyrillic text: $reader reading, $([int]($m.Length / 2)) letters in a row ($(Format-CodePoints $m.Value.Substring(0, 6)) ..)"
            }
        }
    }
    return $found
}
