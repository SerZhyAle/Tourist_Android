# Port ledger core - shared by every script of tools/port-ledger (S0002, handoff section 6a item 2).
# Dot-sourced, never run directly.
#
# docs/port-ledger.jsonl is one JSON object per line, one line per ported file. Mandatory fields:
#   source        FastMediaSorter path, repo-relative, forward slashes
#   sourceSha256  SHA-256 (lower-case hex) of the source bytes at port time or last carry
#   destination   this repository's path, repo-relative, forward slashes
#   portedOn      yyyy-MM-dd
# Optional fields:
#   carriedOn     yyyy-MM-dd, set when a newer FastMediaSorter version was carried across
#   decision      { sourceSha256, note, decidedOn } - a deliberate decision NOT to carry, pinned to the
#                 source version it was made about; the next source change is drift again
#
# Invariants the whole tool keeps: the hash is always computed, never typed; a path never escapes its
# repository root; the ledger is written atomically as UTF-8 without BOM with LF endings, sorted by
# (source, destination) so a carry shows up as a one-line diff.

Set-StrictMode -Version Latest

$script:RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$script:LedgerFile = Join-Path $script:RepoRoot 'docs\port-ledger.jsonl'
$script:FmsRoot = 'P:\ANDROID\FastMediaSorter_mob_v2'
$script:RequiredFields = @('source', 'sourceSha256', 'destination', 'portedOn')

function Get-SourceRoot {
    # Explicit argument first, then the machine override, then the default on this workstation.
    param([string] $SourceRoot)
    $candidate = if ($SourceRoot) { $SourceRoot } elseif ($env:TOURIST_FMS_ROOT) { $env:TOURIST_FMS_ROOT } else { $script:FmsRoot }
    if (-not (Test-Path -LiteralPath $candidate -PathType Container)) {
        throw "FastMediaSorter root '$candidate' not found. Pass -SourceRoot or set TOURIST_FMS_ROOT."
    }
    return (Resolve-Path -LiteralPath $candidate).Path
}

function Get-DestinationRoot {
    param([string] $DestinationRoot)
    if (-not $DestinationRoot) { return $script:RepoRoot }
    if (-not (Test-Path -LiteralPath $DestinationRoot -PathType Container)) {
        throw "Destination root '$DestinationRoot' not found."
    }
    return (Resolve-Path -LiteralPath $DestinationRoot).Path
}

function Get-LedgerFile {
    param([string] $Ledger)
    if ($Ledger) { $Ledger } else { $script:LedgerFile }
}

function ConvertTo-RelativeLedgerPath {
    # Backslashes become forward slashes, './' goes away, and anything absolute or climbing out ('..')
    # is refused: a ledger row may only relate two files inside their own trees.
    param([Parameter(Mandatory)][string] $Path, [Parameter(Mandatory)][string] $Role)
    $normalized = (($Path -replace '\\', '/').Trim()) -replace '^\./', ''
    $segments = @($normalized -split '/')
    $escapesRoot = ($normalized -eq '') -or $normalized.StartsWith('/') -or
        ($normalized -match '^[A-Za-z]:') -or ($segments -contains '..')
    if ($escapesRoot) {
        throw "$Role path '$Path' must be a relative path that stays inside its repository."
    }
    return $normalized
}

function Get-FileSha256 {
    param([Parameter(Mandatory)][string] $Path)
    (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}

function Get-LedgerRows {
    # A missing or empty ledger is a valid, empty ledger: nothing has been ported yet.
    param([Parameter(Mandatory)][string] $Path)
    $rows = [System.Collections.Generic.List[hashtable]]::new()
    if (-not (Test-Path -LiteralPath $Path)) { return , $rows }
    $lineNumber = 0
    foreach ($line in [System.IO.File]::ReadAllLines($Path)) {
        $lineNumber++
        if ([string]::IsNullOrWhiteSpace($line)) { continue }
        $row = $null
        try { $row = $line | ConvertFrom-Json -AsHashtable -ErrorAction Stop }
        catch { throw "$Path line ${lineNumber} is not valid JSON: $($_.Exception.Message)" }
        foreach ($field in $script:RequiredFields) {
            if (-not $row.ContainsKey($field) -or [string]::IsNullOrWhiteSpace([string]$row[$field])) {
                throw "$Path line ${lineNumber} has no '$field'."
            }
        }
        if ($row['sourceSha256'] -notmatch '^[0-9a-f]{64}$') {
            throw "$Path line ${lineNumber}: sourceSha256 is not a lower-case hex SHA-256."
        }
        $rows.Add($row)
    }
    # The leading comma keeps the list whole on the way out: a bare return enumerates it in the
    # pipeline, and an empty ledger would arrive at the caller as $null.
    return , $rows
}

function Save-LedgerRows {
    # Field order in every row is fixed, rows sort by (source, destination), the write goes through a
    # temp file, and the encoding is UTF-8 without BOM with LF endings - a commit then carries the same
    # bytes on every machine.
    param([Parameter(Mandatory)][string] $Path, [Parameter(Mandatory)] $Rows)
    $fieldOrder = @('source', 'sourceSha256', 'destination', 'portedOn', 'carriedOn', 'decision')
    $orderedRows = @($Rows) | Sort-Object -Property { $_['source'] }, { $_['destination'] }
    $lines = foreach ($row in $orderedRows) {
        $ordered = [ordered]@{}
        foreach ($field in $fieldOrder) { if ($row.ContainsKey($field)) { $ordered[$field] = $row[$field] } }
        $ordered | ConvertTo-Json -Compress -Depth 4 -EscapeHandling Default
    }
    $content = if ($lines) { ($lines -join "`n") + "`n" } else { '' }
    $directory = Split-Path -Parent $Path
    if ($directory -and -not (Test-Path -LiteralPath $directory)) {
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
    }
    $staging = "$Path.writetmp"
    [System.IO.File]::WriteAllText($staging, $content, [System.Text.UTF8Encoding]::new($false))
    Move-Item -LiteralPath $staging -Destination $Path -Force
}

function Find-Rows {
    # Rows whose source matches, optionally narrowed to one destination (one source may port to several
    # places). The leading comma keeps the result one array - without it a zero- or one-hit result
    # unwraps on return, and the caller's .Count then counts hashtable keys instead of rows.
    param([Parameter(Mandatory)] $Rows, [Parameter(Mandatory)][string] $Source, [string] $Destination)
    return , @($Rows | Where-Object {
        $_['source'] -eq $Source -and (-not $Destination -or $_['destination'] -eq $Destination)
    })
}
