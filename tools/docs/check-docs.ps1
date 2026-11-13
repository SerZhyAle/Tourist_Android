#requires -Version 7.0
<#
.SYNOPSIS
    Checks the internal documentation corpus against DOC-INTERNAL-QUALITY rules 1-3, 5 and 6 (S0029).
.DESCRIPTION
    -Check registry  rules 1-2: every record of every registry carries the required fields, every declared path
                     matches a file, every corpus file is declared by exactly one record or ignored with a reason,
                     and a tracked registry names no git-ignored file while a local one names only ignored files
                     (0.11 item K). A registry is tracked or local by git check-ignore --no-index of its own file.
    -Check links     rule 3: relative Markdown links resolve to a file or folder, and a #fragment to a heading of
                     the target. Links inside a declared archive are not judged (0.11 item Q); links into it are.
    -Check style     rule 5: no "..." or U+2026, no em or en dash in Markdown prose; code is removed first.
    -Check assets    rule 6: image links, image paths written in code and numbered screenshot bookmarks resolve to
                     files; an asset cited from a tracked document is not git-ignored.
    -Check all       the four in one process plus the harness document_registry/validate over the tracked registry,
                     each verdict line quoted, then the aggregate line (CHECK-VERDICT rule 14).

    Configuration: tools/docs/docs.config.json. Accepted debt: the set baseline it names, one identifier per line,
    sorted (CHECK-BASELINE). It may only fall: a finding it does not list fails, and an identifier no finding
    raises any more fails until it is removed. -UpdateBaseline rewrites the entries of the checks run from the
    current findings and prints every identifier it accepts.

    Exit codes (CHECK-VERDICT): 0 PASS, 1 FAIL, 2 COULD NOT VERIFY (unreadable configuration or registry, no git
    work tree, the harness missing). The verdict line is the last line written.
#>
[CmdletBinding()]
param(
    [ValidateSet('registry', 'links', 'style', 'assets', 'all')][string]$Check = 'all',
    [string]$Root,
    [switch]$UpdateBaseline,
    [switch]$NoHarness
)
$ErrorActionPreference = 'Stop'
if (-not $Root) { $Root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot) }
$Root = (Resolve-Path -LiteralPath $Root).Path.TrimEnd('\', '/')
$imageExt = '\.(png|jpe?g|gif|svg|webp)$'
$required = @('id', 'title', 'category', 'audience', 'paths', 'published', 'indexable', 'product_areas', 'update_triggers', 'generated')

function Get-Rel([string]$full) { $full.Substring($Root.Length).TrimStart('\', '/').Replace('\', '/') }

function ConvertTo-GlobRegex([string]$glob) {
    $sb = [Text.StringBuilder]::new('^')
    for ($i = 0; $i -lt $glob.Length; $i++) {
        $c = $glob[$i]
        if ($c -eq '*' -and $i + 1 -lt $glob.Length -and $glob[$i + 1] -eq '*') {
            $i++
            if ($i + 1 -lt $glob.Length -and $glob[$i + 1] -eq '/') { $i++; [void]$sb.Append('(?:.*/)?') } else { [void]$sb.Append('.*') }
        }
        elseif ($c -eq '*') { [void]$sb.Append('[^/]*') }
        elseif ($c -eq '?') { [void]$sb.Append('[^/]') }
        elseif ($c -eq '[') { $end = $glob.IndexOf(']', $i); [void]$sb.Append($glob.Substring($i, $end - $i + 1)); $i = $end }
        else { [void]$sb.Append([regex]::Escape([string]$c)) }
    }
    [void]$sb.Append('$')
    [regex]::new($sb.ToString(), 'IgnoreCase')
}

function Get-FilesUnder([string]$rel) {
    $full = Join-Path $Root $rel
    if (-not (Test-Path -LiteralPath $full -PathType Container)) { return @() }
    @(Get-ChildItem -LiteralPath $full -Recurse -File -Force | ForEach-Object { Get-Rel $_.FullName })
}

function Get-CorpusFiles {
    $set = [Collections.Generic.SortedSet[string]]::new([StringComparer]::Ordinal)
    foreach ($folder in @($config.corpus.folders)) { foreach ($f in (Get-FilesUnder $folder)) { [void]$set.Add($f) } }
    foreach ($pattern in @($config.corpus.files)) {
        $static = @($pattern -split '/')
        $base = @(); foreach ($seg in $static) { if ($seg -match '[*?\[]') { break }; $base += $seg }
        if ($base.Count -eq $static.Count) {
            if (Test-Path -LiteralPath (Join-Path $Root $pattern) -PathType Leaf) { [void]$set.Add($pattern) }
            continue
        }
        $rx = ConvertTo-GlobRegex $pattern
        foreach ($f in (Get-FilesUnder ($base -join '/'))) { if ($rx.IsMatch($f)) { [void]$set.Add($f) } }
    }
    @($set)
}

$script:ignoredCache = @{}
function Test-Ignored([string[]]$paths) {
    # git check-ignore --no-index judges the .gitignore rules alone, so the answer does not move with what
    # happens to be committed. Paths go as arguments in chunks: piped stdin reaches git with CR line ends on
    # Windows, and a path with a trailing CR matches no rule.
    $todo = @($paths | Where-Object { $_ -and -not $script:ignoredCache.ContainsKey($_) } | Select-Object -Unique)
    for ($i = 0; $i -lt $todo.Count; $i += 100) {
        $chunk = @($todo[$i..([Math]::Min($i + 99, $todo.Count - 1))])
        $out = @(& git -C $Root check-ignore --no-index -- @chunk 2>$null)
        if ($LASTEXITCODE -gt 1) { throw "git check-ignore failed in $Root (exit $LASTEXITCODE)" }
        $hit = [Collections.Generic.HashSet[string]]::new([string[]]@($out | ForEach-Object { $_.Replace('\', '/') }))
        foreach ($p in $chunk) { $script:ignoredCache[$p] = $hit.Contains($p) }
    }
}

function Get-MarkdownMask([string]$text) {
    # Fenced blocks and inline code become spaces of the same length, so offsets and line numbers still map.
    $chars = $text.ToCharArray()
    $lines = [regex]::Matches($text, '[^\n]*\n?')
    $fence = $null
    foreach ($m in $lines) {
        if ($m.Length -eq 0) { continue }
        $line = $m.Value
        $open = [regex]::Match($line, '^\s{0,3}(`{3,}|~{3,})')
        $blank = $false
        if ($fence) {
            $blank = $true
            if ($open.Success -and $open.Groups[1].Value[0] -eq $fence[0] -and $open.Groups[1].Value.Length -ge $fence.Length) { $fence = $null }
        }
        elseif ($open.Success) { $fence = $open.Groups[1].Value; $blank = $true }
        if ($blank) { for ($i = $m.Index; $i -lt $m.Index + $m.Length; $i++) { if ($chars[$i] -ne "`n") { $chars[$i] = ' ' } } }
    }
    $masked = [string]::new($chars)
    foreach ($m in [regex]::Matches($masked, '(?<!`)(`+)(?!`)((?:(?!\n[ \t]*\n)[\s\S])+?)(?<!`)\1(?!`)')) {
        for ($i = $m.Index; $i -lt $m.Index + $m.Length; $i++) { if ($chars[$i] -ne "`n") { $chars[$i] = ' ' } }
    }
    [string]::new($chars)
}

function Get-LineAt([string]$text, [int]$index) { ([regex]::Matches($text.Substring(0, $index), "`n")).Count + 1 }

$script:docCache = @{}
function Get-Doc([string]$rel) {
    if (-not $script:docCache.ContainsKey($rel)) {
        $raw = [IO.File]::ReadAllText((Join-Path $Root $rel))
        $script:docCache[$rel] = [pscustomobject]@{ Raw = $raw; Masked = (Get-MarkdownMask $raw) }
    }
    $script:docCache[$rel]
}

function Get-Slug([string]$heading) {
    $t = [regex]::Replace($heading, '!?\[([^\]]*)\]\([^)]*\)', '$1')
    $t = $t -replace '<[^>]+>', '' -replace '[`*]', ''
    $t = $t.Trim().ToLowerInvariant()
    $t = [regex]::Replace($t, '[^\p{L}\p{M}\p{N} _-]', '')
    $t -replace ' ', '-'
}

$script:anchorCache = @{}
function Get-Anchors([string]$rel) {
    if (-not $script:anchorCache.ContainsKey($rel)) {
        $doc = Get-Doc $rel
        $set = [Collections.Generic.HashSet[string]]::new()
        $count = @{}
        foreach ($m in [regex]::Matches($doc.Masked, '(?m)^ {0,3}#{1,6}[ \t]+(.+?)[ \t#]*\r?$')) {
            # The heading text is read from the raw file: inline code in a heading still names its anchor.
            $text = $doc.Raw.Substring($m.Groups[1].Index, $m.Groups[1].Length)
            $slug = Get-Slug $text
            if ($count.ContainsKey($slug)) { $count[$slug]++; [void]$set.Add("$slug-$($count[$slug])") } else { $count[$slug] = 0; [void]$set.Add($slug) }
        }
        foreach ($m in [regex]::Matches($doc.Raw, '<a\s+(?:name|id)="([^"]+)"')) { [void]$set.Add($m.Groups[1].Value) }
        $script:anchorCache[$rel] = $set
    }
    $script:anchorCache[$rel]
}

function Get-Links([string]$rel) {
    $doc = Get-Doc $rel
    $found = [Collections.Generic.List[object]]::new()
    foreach ($m in [regex]::Matches($doc.Masked, '(!?)\[(?:[^\[\]]|\[[^\]]*\])*\]\(\s*<?([^)\s>]*)>?(?:\s+"[^"]*")?\s*\)')) {
        $found.Add([pscustomobject]@{ Target = $m.Groups[2].Value; Line = (Get-LineAt $doc.Raw $m.Index) })
    }
    foreach ($m in [regex]::Matches($doc.Masked, '(?m)^ {0,3}\[(?!\^)[^\]]+\]:\s*<?(\S+?)>?(?:\s|$)')) {
        $found.Add([pscustomobject]@{ Target = $m.Groups[1].Value; Line = (Get-LineAt $doc.Raw $m.Index) })
    }
    $found
}

function Resolve-Target([string]$fromRel, [string]$path) {
    # Returns the root-relative path, or $null when it leaves the repository.
    $path = [Uri]::UnescapeDataString($path)
    $joined = if ($path.StartsWith('/')) { $path.TrimStart('/') } else {
        $dir = if ($fromRel) { Split-Path -Parent $fromRel } else { '' }
        if ($dir) { "$dir/$path" } else { $path }
    }
    $parts = [Collections.Generic.List[string]]::new()
    foreach ($seg in ($joined.Replace('\', '/') -split '/')) {
        if ($seg -eq '' -or $seg -eq '.') { continue }
        if ($seg -eq '..') { if ($parts.Count -eq 0) { return $null }; $parts.RemoveAt($parts.Count - 1); continue }
        $parts.Add($seg)
    }
    $parts -join '/'
}

function New-Result([string]$name) {
    [pscustomobject]@{ Name = $name; Findings = [Collections.Generic.List[object]]::new(); Counts = ''; Error = $null }
}
function Add-Finding($result, [string]$id, [string]$message) { $result.Findings.Add([pscustomobject]@{ Id = "$($result.Name) $id"; Message = $message }) }

function Read-Registries {
    $regs = [Collections.Generic.List[object]]::new()
    foreach ($regRel in @($config.registries)) {
        $full = Join-Path $Root $regRel
        if (-not (Test-Path -LiteralPath $full -PathType Leaf)) { throw "registry not found: $regRel" }
        $records = [Collections.Generic.List[object]]::new()
        $n = 0
        foreach ($line in [IO.File]::ReadAllLines($full)) {
            $n++
            if (-not $line.Trim()) { continue }
            try { $records.Add(($line | ConvertFrom-Json)) } catch { throw "${regRel} line ${n}: invalid JSON" }
        }
        $regs.Add([pscustomobject]@{ Path = $regRel; Records = $records })
    }
    $regs
}

function Get-ArchiveRegexes {
    $list = @()
    foreach ($reg in $script:registries) {
        foreach ($rec in $reg.Records) { if ($rec.archive -eq $true) { foreach ($p in @($rec.paths)) { $list += ConvertTo-GlobRegex $p } } }
    }
    $list
}
function Test-Archived([string]$rel) { foreach ($rx in $script:archiveRx) { if ($rx.IsMatch($rel)) { return $true } }; $false }

# ------------------------------------------------------------------ registry (rules 1-2)
function Invoke-RegistryCheck {
    $r = New-Result 'docs-registry'
    $owner = @{}
    $ids = @{}
    $recordCount = 0
    $kinds = @()
    $ignoreSet = @{}
    foreach ($entry in @($config.ignore)) {
        $words = @(([string]$entry.reason -split '\s+') | Where-Object { $_ })
        if (-not $entry.path -or $words.Count -lt 4) { Add-Finding $r "ignore $($entry.path)" "ignore entry needs a path and a reason of at least four words"; continue }
        $ignoreSet[[string]$entry.path] = $true
        if ($script:corpus -notcontains $entry.path) { Add-Finding $r "ignore-stale $($entry.path)" "ignore list names $($entry.path), which is not a corpus file" }
    }
    Test-Ignored (@($script:corpus) + @($script:registries | ForEach-Object Path))
    foreach ($reg in $script:registries) {
        $local = $script:ignoredCache[$reg.Path]
        $kinds += "$($reg.Path) $(if ($local) { 'local' } else { 'tracked' })"
        foreach ($rec in $reg.Records) {
            $recordCount++
            $rid = [string]$rec.id
            foreach ($field in $required) { if ($rec.PSObject.Properties.Name -notcontains $field) { Add-Finding $r "field $rid $field" "$($reg.Path): record '$rid' lacks '$field'" } }
            if ($rid -notmatch '^[a-z0-9]+(?:-[a-z0-9]+)*$') { Add-Finding $r "id $rid" "$($reg.Path): invalid id '$rid'" }
            if ($ids.ContainsKey($rid)) { Add-Finding $r "duplicate $rid" "record id '$rid' appears in $($ids[$rid]) and $($reg.Path)" } else { $ids[$rid] = $reg.Path }
            if ($null -ne $rec.archive -and $rec.archive -isnot [bool]) { Add-Finding $r "archive $rid" "$($reg.Path): '$rid' archive must be true or false" }
            if ($rec.generated -isnot [bool]) { Add-Finding $r "generated $rid" "$($reg.Path): '$rid' generated (the role: true render, false source) must be true or false" }
            foreach ($p in @($rec.paths)) {
                $p = [string]$p
                if ($p -match '(^|/)\.\.(/|$)' -or [IO.Path]::IsPathRooted($p)) { Add-Finding $r "escape $rid $p" "${rid}: path leaves the repository: $p"; continue }
                $rx = ConvertTo-GlobRegex $p
                $hits = @($script:corpus | Where-Object { $rx.IsMatch($_) })
                if (-not $hits.Count) { Add-Finding $r "missing $rid $p" "${rid}: no corpus file matches $p (registry to disk)"; continue }
                foreach ($h in $hits) {
                    if ($owner.ContainsKey($h)) {
                        if ($owner[$h] -ne $rid) { Add-Finding $r "twice $h" "$h is declared by '$($owner[$h])' and '$rid' - a document has one home" }
                        continue
                    }
                    $owner[$h] = $rid
                    if ($script:ignoredCache[$h] -ne $local) {
                        $what = if ($local) { 'tracked file in the local' } else { 'git-ignored file in the tracked' }
                        Add-Finding $r "split $h" "$h is a $what registry $($reg.Path) (DOC-INTERNAL-QUALITY 0.11 item K)"
                    }
                }
            }
        }
    }
    foreach ($f in $script:corpus) {
        if (-not $owner.ContainsKey($f) -and -not $ignoreSet.ContainsKey($f)) {
            Add-Finding $r "undeclared $f" "$f is in the corpus and in no registry - add it to a record, or to the ignore list with a reason (disk to registry)"
        }
    }
    $r.Counts = "$($script:registries.Count) registries ($($kinds -join ', ')), $recordCount records, $($owner.Count) files declared, $($ignoreSet.Count) ignored"
    $r
}

# ------------------------------------------------------------------ links (rule 3)
function Invoke-LinksCheck {
    $r = New-Result 'docs-links'
    $links = 0; $anchors = 0; $files = 0
    foreach ($f in ($script:corpus | Where-Object { $_ -match '\.md$' })) {
        if (Test-Archived $f) { continue }
        $files++
        foreach ($l in (Get-Links $f)) {
            $t = $l.Target
            if (-not $t -or $t -match '^[a-zA-Z][a-zA-Z0-9+.-]*:' -or $t.StartsWith('//')) { continue }
            $path, $frag = $t -split '#', 2
            if ($path -match $imageExt) { continue }
            $links++
            $target = if ($path) { Resolve-Target $f $path } else { $f }
            if ($null -eq $target) { Add-Finding $r "$f -> $t" "${f}:$($l.Line): link leaves the repository: $t"; continue }
            $full = Join-Path $Root $target
            if (-not (Test-Path -LiteralPath $full)) { Add-Finding $r "$f -> $t" "${f}:$($l.Line): link target does not exist: $t"; continue }
            if ($frag -and $target -match '\.md$' -and (Test-Path -LiteralPath $full -PathType Leaf)) {
                $anchors++
                if (-not (Get-Anchors $target).Contains($frag)) { Add-Finding $r "$f -> $t" "${f}:$($l.Line): no heading '#$frag' in $target" }
            }
        }
    }
    $r.Counts = "$links link(s), $anchors anchor(s) in $files file(s)"
    $r
}

# ------------------------------------------------------------------ style (rule 5)
function Invoke-StyleCheck {
    $r = New-Result 'docs-style'
    $files = 0
    $kinds = [ordered]@{ 'ellipsis' = '\.{3,}|…'; 'em-dash' = '—'; 'en-dash' = '–' }
    foreach ($f in ($script:corpus | Where-Object { $_ -match '\.md$' })) {
        $files++
        $doc = Get-Doc $f
        # Link targets and autolinks are addresses, not prose.
        $prose = [regex]::Replace($doc.Masked, '\]\([^)]*\)|<https?://[^>]*>|https?://\S+', { param($m) ' ' * $m.Length })
        foreach ($kind in $kinds.Keys) {
            foreach ($m in [regex]::Matches($prose, $kinds[$kind])) {
                $line = Get-LineAt $doc.Raw $m.Index
                $text = ($doc.Raw -split "`n")[$line - 1].Trim()
                if ($text.Length -gt 80) { $text = $text.Substring(0, 80) }
                Add-Finding $r "$f $kind $text" "${f}:${line}: $kind in prose (house style: '..' and a plain hyphen)"
            }
        }
    }
    $r.Counts = "$files file(s)"
    $r
}

# ------------------------------------------------------------------ assets (rule 6)
function Invoke-AssetsCheck {
    $r = New-Result 'docs-assets'
    $cites = [Collections.Generic.List[object]]::new()
    foreach ($f in ($script:corpus | Where-Object { $_ -match '\.md$' })) {
        $doc = Get-Doc $f
        foreach ($l in (Get-Links $f)) {
            $path = ($l.Target -split '#', 2)[0]
            if ($path -notmatch $imageExt -or $path -match '^[a-zA-Z][a-zA-Z0-9+.-]*:' -or $path.StartsWith('//')) { continue }
            $cites.Add([pscustomobject]@{ From = $f; Line = $l.Line; Text = $path; Target = (Resolve-Target $f $path) })
        }
        foreach ($m in [regex]::Matches($doc.Raw, '`([^`\s<>{}*]+/[^`\s<>{}*]+)`')) {
            $p = $m.Groups[1].Value
            if ($p -notmatch $imageExt) { continue }
            $target = Resolve-Target '' $p
            if ($target -and -not (Test-Path -LiteralPath (Join-Path $Root $target))) {
                $near = Resolve-Target $f $p
                if ($near -and (Test-Path -LiteralPath (Join-Path $Root $near))) { $target = $near }
            }
            $cites.Add([pscustomobject]@{ From = $f; Line = (Get-LineAt $doc.Raw $m.Index); Text = $p; Target = $target })
        }
        # A numbered screenshot bookmark names files NN_<what>.<image> in the folder that carries the ticket's name.
        foreach ($m in [regex]::Matches($doc.Raw, '(?i)\bscreenshots?\b((?:\s*(?:,|and|-)?\s*`\d{2}`)+)')) {
            $folder = $f -replace '\.md$', ''
            $line = Get-LineAt $doc.Raw $m.Index
            $numbers = [Collections.Generic.List[int]]::new()
            foreach ($t in [regex]::Matches($m.Groups[1].Value, '(-)?\s*`(\d{2})`')) {
                $n = [int]$t.Groups[2].Value
                if ($t.Groups[1].Success -and $numbers.Count) { for ($k = $numbers[$numbers.Count - 1] + 1; $k -le $n; $k++) { $numbers.Add($k) } } else { $numbers.Add($n) }
            }
            foreach ($n in $numbers) {
                $nn = '{0:D2}' -f $n
                $dir = Join-Path $Root $folder
                $hit = if (Test-Path -LiteralPath $dir -PathType Container) {
                    @(Get-ChildItem -LiteralPath $dir -File | Where-Object { $_.Name -match "^${nn}_.*$imageExt" }) | Select-Object -First 1
                }
                $cites.Add([pscustomobject]@{ From = $f; Line = $line; Text = "screenshot $nn in $folder/"; Target = $(if ($hit) { Get-Rel $hit.FullName } else { "$folder/${nn}_*" }) })
            }
        }
    }
    $known = @($cites | Where-Object { $_.Target -and (Test-Path -LiteralPath (Join-Path $Root $_.Target) -PathType Leaf) })
    Test-Ignored (@($known | ForEach-Object Target) + @($cites | ForEach-Object From))
    foreach ($c in $cites) {
        if (-not $c.Target -or -not (Test-Path -LiteralPath (Join-Path $Root $c.Target) -PathType Leaf)) {
            Add-Finding $r "$($c.From) -> $($c.Text)" "$($c.From):$($c.Line): cited asset does not resolve: $($c.Text)"
            continue
        }
        if (-not $script:ignoredCache[$c.From] -and $script:ignoredCache[$c.Target]) {
            Add-Finding $r "$($c.From) -> $($c.Text)" "$($c.From):$($c.Line): tracked document cites a git-ignored asset: $($c.Target)"
        }
    }
    $r.Counts = "$($cites.Count) citation(s)"
    $r
}

# ------------------------------------------------------------------ verdicts and baseline
function Read-Baseline {
    $full = Join-Path $Root $config.baseline
    if (-not (Test-Path -LiteralPath $full -PathType Leaf)) { return @() }
    @([IO.File]::ReadAllLines($full) | Where-Object { $_.Trim() -and -not $_.StartsWith('#') })
}

function Complete-Result($r, [string[]]$baseline) {
    if ($r.Error) {
        Write-Host "$($r.Name): COULD NOT VERIFY ($($r.Error))"
        return 2
    }
    $mine = @($baseline | Where-Object { $_ -and $_.StartsWith("$($r.Name) ") })
    $ids = @($r.Findings | ForEach-Object Id | Select-Object -Unique)
    $new = @($r.Findings | Where-Object { $mine -notcontains $_.Id })
    $stale = @($mine | Where-Object { $ids -notcontains $_ })
    $accepted = @($ids | Where-Object { $mine -contains $_ }).Count
    foreach ($f in $new) { Write-Host "FAIL: $($f.Message)" }
    foreach ($s in $stale) { Write-Host "FAIL: baseline entry raised by no finding any more, remove it: $s" }
    $bad = $new.Count + $stale.Count
    $tail = if ($accepted) { ", $accepted baselined" } else { '' }
    if ($bad) { Write-Host "$($r.Name): FAIL ($bad finding(s); $($r.Counts)$tail)"; return 1 }
    Write-Host "$($r.Name): PASS ($($r.Counts)$tail)"
    0
}

$names = if ($Check -eq 'all') { @('registry', 'links', 'style', 'assets') } else { @($Check) }
$subject = if ($Check -eq 'all') { 'docs-check' } else { "docs-$Check" }
try {
    $config = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'docs.config.json') -Raw | ConvertFrom-Json
    $null = & git -C $Root rev-parse --is-inside-work-tree 2>$null
    if ($LASTEXITCODE -ne 0) { throw "not a git work tree: $Root" }
    $script:corpus = Get-CorpusFiles
    $script:registries = Read-Registries
    $script:archiveRx = Get-ArchiveRegexes
    $baseline = Read-Baseline
}
catch {
    Write-Host "$subject could not start: $($_.Exception.Message)"
    Write-Host "${subject}: COULD NOT VERIFY ($($_.Exception.Message))"
    exit 2
}

$results = foreach ($n in $names) {
    try {
        switch ($n) {
            'registry' { Invoke-RegistryCheck }
            'links' { Invoke-LinksCheck }
            'style' { Invoke-StyleCheck }
            'assets' { Invoke-AssetsCheck }
        }
    }
    catch { $e = New-Result "docs-$n"; $e.Error = $_.Exception.Message; $e }
}

if ($UpdateBaseline) {
    $keep = @($baseline | Where-Object { $line = $_; $line -and -not ($results | Where-Object { $line.StartsWith("$($_.Name) ") }) })
    $add = @($results | Where-Object { -not $_.Error } | ForEach-Object { $_.Findings } | ForEach-Object Id | Select-Object -Unique)
    foreach ($a in ($add | Where-Object { $baseline -notcontains $_ })) { Write-Host "accept: $a" }
    $header = @(
        '# CHECK-BASELINE set baseline of tools/docs/check-docs.ps1 (DOC-INTERNAL-QUALITY, S0029): one finding identifier',
        '# per line, sorted. It may only fall - a finding not listed fails, a listed identifier no finding raises fails until',
        '# it is removed. Rewritten only by check-docs.ps1 -UpdateBaseline, which prints every identifier it accepts.')
    $body = @(@($keep) + @($add) | Sort-Object -Unique -CaseSensitive)
    [IO.File]::WriteAllLines((Join-Path $Root $config.baseline), [string[]]($header + $body))
    $baseline = $body
}

$codes = @{}
foreach ($r in $results) { $codes[$r.Name] = Complete-Result $r $baseline }

if ($Check -ne 'all') { exit @($codes.Values)[0] }

if ($NoHarness) {
    Write-Host 'skipped: harness document_registry/validate (-NoHarness)'
}
else {
    # CHECK-VERDICT rule 15: the harness validator's 0/1/2 are translated on purpose to PASS/FAIL/COULD NOT VERIFY.
    $out = @(& pwsh -NoProfile -File (Join-Path $Root 'tools/harness.ps1') document_registry/validate -RepoRoot $Root 2>&1)
    $hx = $LASTEXITCODE
    foreach ($o in $out) { Write-Host "  $o" }
    $word, $code = switch ($hx) { 0 { 'PASS', 0 } 1 { 'FAIL', 1 } default { 'COULD NOT VERIFY', 2 } }
    Write-Host "docs-harness-registry: $word (document_registry/validate exit $hx)"
    $codes['docs-harness-registry'] = $code
}

$failed = @($codes.Keys | Where-Object { $codes[$_] -eq 1 } | Sort-Object)
$unverified = @($codes.Keys | Where-Object { $codes[$_] -eq 2 } | Sort-Object)
if ($failed.Count) { Write-Host "docs-check: FAIL ($($failed.Count): $($failed -join ', '))"; exit 1 }
if ($unverified.Count) { Write-Host "docs-check: COULD NOT VERIFY ($($unverified.Count): $($unverified -join ', '))"; exit 2 }
Write-Host "docs-check: PASS ($($codes.Count) checks)"
exit 0
