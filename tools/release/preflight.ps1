<#
.SYNOPSIS
    Pre-release sweep, build half: builds every package of a release candidate from a clean source and
    writes a PASS/FAIL verdict that names the version it judged.

.DESCRIPTION
    Judges a commit (default HEAD), extracted with git archive into temp/rc/src, never the working tree:
    the tree carries other tickets' work in progress. -WorkingTree judges a snapshot of the tree instead,
    for a dry run only; such a verdict is never shippable.

    Checks, in order (each PASS, WARN, OPEN or FAIL):
      source      the judged commit, or the snapshot (OPEN: a snapshot is never shippable)
      drift       tools/port-ledger/check-drift.ps1 of the judged source: nothing uncarried
      help        tools/help/check-help.ps1, freshness included
      ui-source   tools/ui/check-unified-ui.ps1
      strings     tools/strings/check-string-tables.ps1: no double-encoded text in any string table
      docs        tools/docs/check-docs.ps1 -Check all
      docs-selftest, ledger-selftest, strings-selftest
                  tools/docs/test-docs.ps1, tools/port-ledger/test-port-ledger.ps1 and
                  tools/strings/test-string-tables.ps1
      plan       the release plan validates (WARN only: plan drift is fixed, never a blocker)
      leaks       no key, keystore or keystore.properties among the files a release carries
      keys        a release key file is found (OPEN when it holds rehearsal keys)
      build-logic the build-logic unit tests, gradlew -p build-logic :convention:test
      build      every module's unit tests and every release package, one Gradle invocation, one stamp
      packages    all ten packages exist: phone and wear (store APK + AAB, noLegal APK), watch face (APK + AAB)
      ids         application ids are the frozen anchors
      signatures  every package signed, no debug key; noLegal packages share the app signing key, Play-bound
                  packages share the upload key, and the two keys differ
      version     one versionName for all, not the sentinel, each versionCode in its family's band
      families    the PACKAGE-VERSIONING rows are registered in the catalog (OPEN until they are)
      reach       per package: minSdk, ABIs, required features against tools/release/reach-baseline.json;
                  with no baseline the candidate defines it (reach-candidate.json in the output)
      notices     a package that bundles native binaries carries THIRD-PARTY-NOTICES.txt
      contracts   every contract pointer of docs/contracts has a verified registry row (OPEN otherwise)
      privacy     a privacy page is declared (OPEN until the site hosts it); permissions listed per package
      channels    at least one channel is declared in .sza-canon.json (OPEN otherwise)

    The verdict is FAIL when any check fails, otherwise PASS. It is shippable only when it is PASS and no
    check is OPEN. The device half of the sweep (update-proof.ps1, the UI matrix, the help capture and walk) is
    listed in tools/release/README.md.

    Output: temp/release/<versionName>/ - preflight.json, preflight.md, drift.log, one log per gate,
    build-logic.log, build.log, packages/, reach-candidate.json, permissions.md.

.EXAMPLE
    pwsh -NoProfile -File tools/release/preflight.ps1
    pwsh -NoProfile -File tools/release/preflight.ps1 -Ref main -Keys P:\ANDROID\Tourist_credentials\keystore.properties

.NOTES
    Exit codes: 0 verdict PASS, 1 verdict FAIL, 2 the sweep could not run.
#>
[CmdletBinding()]
param(
    [string] $Ref = 'HEAD',
    [switch] $WorkingTree,
    [string] $Keys,
    [string] $OutRoot,
    [string] $Baseline
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot '_release.ps1')

if (-not $OutRoot) { $OutRoot = Join-Path $script:RepoRoot 'temp\release' }
if (-not $Baseline) { $Baseline = Join-Path $PSScriptRoot 'reach-baseline.json' }

$checks = [System.Collections.Generic.List[object]]::new()
function Add-Check([string] $Id, [string] $Status, [string] $Detail) {
    $checks.Add([pscustomobject]@{ id = $Id; status = $Status; detail = $Detail })
    Write-Output ("  {0,-5} {1,-11} {2}" -f $Status, $Id, $Detail)
}

$work = Join-Path $script:RepoRoot 'temp\rc'
$src = Join-Path $work 'src'
$marker = Join-Path $work '.preflight-scratch'
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$out = Join-Path $OutRoot "pending-$stamp"

function Write-Verdict([string] $Version) {
    $failed = @($checks | Where-Object status -eq 'FAIL')
    $open = @($checks | Where-Object status -eq 'OPEN')
    $verdict = if ($failed.Count) { 'FAIL' } else { 'PASS' }
    $result = [ordered]@{
        verdict = $verdict
        shippable = ($verdict -eq 'PASS' -and $open.Count -eq 0)
        version = $Version
        source = $script:SourceLabel
        judgedAt = (Get-Date).ToString('yyyy-MM-ddTHH:mm:sszzz')
        checks = $checks
    }
    $result | ConvertTo-Json -Depth 6 | Set-Content (Join-Path $out 'preflight.json') -Encoding utf8
    $md = @("# Pre-flight verdict: $verdict", '',
        "- Version judged: ``$Version``", "- Source: $($script:SourceLabel)",
        "- Shippable: $($result.shippable)$(if ($open.Count) { " ($($open.Count) open: $(($open | ForEach-Object id) -join ', '))" })",
        "- Judged at: $($result.judgedAt)", '', '| Check | Status | Detail |', '| --- | --- | --- |')
    $md += $checks | ForEach-Object { "| $($_.id) | $($_.status) | $($_.detail -replace '\|', '/') |" }
    Set-Content (Join-Path $out 'preflight.md') $md -Encoding utf8
    Write-Host ''
    Write-Host "VERDICT: $verdict for $Version (shippable: $($result.shippable)) -> $out"
    return $verdict
}

try {
    # --- source ---------------------------------------------------------------------------------
    if ((Test-Path $work) -and -not (Test-Path $marker)) {
        throw "$work exists but is not this script's scratch; move it away first."
    }
    if (Test-Path $work) { Remove-Item -Recurse -Force $work }
    New-Item -ItemType Directory -Force $src, $out | Out-Null
    Set-Content $marker 'Scratch of tools/release/preflight.ps1; deleted and recreated on every run.'

    Push-Location $script:RepoRoot
    try {
        $commit = (git rev-parse --verify "$Ref^{commit}" 2>$null)
        if ($LASTEXITCODE -ne 0 -or -not $commit) { throw "Ref '$Ref' is not a commit." }
        if ($WorkingTree) {
            $carried = @(git ls-files -co --exclude-standard)
            $script:SourceLabel = "working-tree snapshot on $($commit.Substring(0, 12))"
        } else {
            $carried = @(git ls-tree -r --name-only $commit)
            $script:SourceLabel = "commit $commit"
        }
    } finally { Pop-Location }

    if ($WorkingTree) {
        $null = robocopy $script:RepoRoot $src /E /NFL /NDL /NJH /NJS /NP `
            /XD build .gradle .kotlin .git .idea temp PLAN /XF keystore.properties *.jks *.keystore local.properties
        if ($LASTEXITCODE -ge 8) { throw "robocopy failed with $LASTEXITCODE." }
        Add-Check 'source' 'OPEN' "$($script:SourceLabel): a snapshot is a dry run, never a release candidate"
    } else {
        $tar = Join-Path $work 'src.tar'
        git -C $script:RepoRoot archive --format=tar -o $tar $commit
        if ($LASTEXITCODE -ne 0) { throw 'git archive failed.' }
        tar -xf $tar -C $src
        if ($LASTEXITCODE -ne 0) { throw 'tar could not extract the archive.' }
        Remove-Item $tar
        Add-Check 'source' 'PASS' $script:SourceLabel
    }
    Set-Content (Join-Path $src 'local.properties') ("sdk.dir=" + ((Get-SdkRoot) -replace '\\', '\\' -replace ':', '\:')) -Encoding ascii

    # --- drift ----------------------------------------------------------------------------------
    $driftScript = Join-Path $src 'tools\port-ledger\check-drift.ps1'
    $driftOut = & pwsh -NoProfile -File $driftScript 2>&1
    $driftExit = $LASTEXITCODE
    $driftOut | Set-Content (Join-Path $out 'drift.log') -Encoding utf8
    $driftLast = ($driftOut | Select-Object -Last 1) -as [string]
    Add-Check 'drift' $(if ($driftExit -eq 0) { 'PASS' } else { 'FAIL' }) "exit $driftExit - $driftLast"

    # --- gates ----------------------------------------------------------------------------------
    # Every script gate and self-test of the judged source, the fast tier included (CLAUDE.md section 7).
    # The documentation gate needs a work tree of its own: temp/rc/src sits inside this repository, whose
    # .gitignore marks every file of it ignored and so turns each tracked registry into a local one. A snapshot
    # leaves PLAN/ out, so its corpus is the tree.
    $gates = @(
        @{ id = 'help'; script = 'tools\help\check-help.ps1'; args = @() },
        @{ id = 'ui-source'; script = 'tools\ui\check-unified-ui.ps1'; args = @() },
        @{ id = 'strings'; script = 'tools\strings\check-string-tables.ps1'; args = @() },
        @{ id = 'docs'; script = 'tools\docs\check-docs.ps1'; args = @('-Check', 'all', '-Root', $(if ($WorkingTree) { $script:RepoRoot } else { $src })) },
        @{ id = 'docs-selftest'; script = 'tools\docs\test-docs.ps1'; args = @() },
        @{ id = 'ledger-selftest'; script = 'tools\port-ledger\test-port-ledger.ps1'; args = @() },
        @{ id = 'strings-selftest'; script = 'tools\strings\test-string-tables.ps1'; args = @() })
    $null = & git -C $src init -q 2>&1
    if ($LASTEXITCODE -ne 0) { throw "git init failed in $src." }
    try {
        foreach ($gate in $gates) {
            $gateArgs = $gate.args
            $gateOut = & pwsh -NoProfile -File (Join-Path $src $gate.script) @gateArgs 2>&1
            $gateExit = $LASTEXITCODE
            $gateOut | Set-Content (Join-Path $out "$($gate.id).log") -Encoding utf8
            $gateLast = ($gateOut | Select-Object -Last 1) -as [string]
            Add-Check $gate.id $(if ($gateExit -eq 0) { 'PASS' } else { 'FAIL' }) "exit $gateExit - $gateLast"
        }
    } finally {
        Remove-Item -Recurse -Force (Join-Path $src '.git')
    }

    # --- plan -----------------------------------------------------------------------------------
    $planOut = & pwsh -NoProfile -File (Join-Path $script:RepoRoot 'tools\harness.ps1') spec_catalog/release-queue -Validate 2>&1
    $planExit = $LASTEXITCODE
    Add-Check 'plan' $(if ($planExit -eq 0) { 'PASS' } else { 'WARN' }) "release-queue -Validate exit $planExit$(if ($planExit) { ' (run -Reconcile)' })"

    # --- leaks ----------------------------------------------------------------------------------
    $leakPattern = '(?i)(\.(jks|keystore|p12|pfx|pem)$)|((^|/)keystore\.properties$)|((^|/)\.env$)'
    $leaks = @($carried | Where-Object { $_ -match $leakPattern })
    Add-Check 'leaks' $(if ($leaks.Count) { 'FAIL' } else { 'PASS' }) $(if ($leaks.Count) { "carried: $($leaks -join ', ')" } else { "$($carried.Count) files carried, no signing material" })

    # --- keys -----------------------------------------------------------------------------------
    if (-not $Keys) {
        $Keys = if ($env:TOURIST_KEYSTORE_PROPERTIES) { $env:TOURIST_KEYSTORE_PROPERTIES } else { 'P:\ANDROID\Tourist_credentials\keystore.properties' }
    }
    if (-not (Test-Path $Keys)) {
        Add-Check 'keys' 'FAIL' "no release key file at $Keys; every release package would be unsigned"
        $null = Write-Verdict 'not built'
        exit 1
    }
    $Keys = (Resolve-Path $Keys).Path
    $rehearsal = [bool](Select-String -Path $Keys -Pattern '^# REHEARSAL' -Quiet)
    Add-Check 'keys' $(if ($rehearsal) { 'OPEN' } else { 'PASS' }) $(if ($rehearsal) { "rehearsal keys ($Keys): re-run with the owner's keys" } else { $Keys })

    # --- build-logic ----------------------------------------------------------------------------
    $logicLog = Join-Path $out 'build-logic.log'
    Push-Location $src
    try {
        & .\gradlew.bat --no-daemon --console=plain -p build-logic :convention:test *> $logicLog
        $logicExit = $LASTEXITCODE
    } finally { Pop-Location }
    $logicLine = (Select-String -Path $logicLog -Pattern '^BUILD (SUCCESSFUL|FAILED)' | Select-Object -Last 1).Line
    Add-Check 'build-logic' $(if ($logicExit -eq 0) { 'PASS' } else { 'FAIL' }) "gradlew -p build-logic :convention:test exit $logicExit - $logicLine"

    # --- build ----------------------------------------------------------------------------------
    $tasks = @(':phone:test', ':wear:test', ':watchface:test',
        ':phone:assembleStoreRelease', ':phone:assembleNoLegalRelease', ':phone:bundleStoreRelease',
        ':wear:assembleStoreRelease', ':wear:assembleNoLegalRelease', ':wear:bundleStoreRelease',
        ':watchface:assembleRelease', ':watchface:bundleRelease')
    $buildLog = Join-Path $out 'build.log'
    $env:TOURIST_KEYSTORE_PROPERTIES = $Keys
    Push-Location $src
    try {
        & .\gradlew.bat --no-daemon --console=plain @tasks *> $buildLog
        $buildExit = $LASTEXITCODE
    } finally {
        Pop-Location
        Remove-Item Env:TOURIST_KEYSTORE_PROPERTIES -ErrorAction SilentlyContinue
    }
    $buildLine = (Select-String -Path $buildLog -Pattern '^BUILD (SUCCESSFUL|FAILED)' | Select-Object -Last 1).Line
    Add-Check 'build' $(if ($buildExit -eq 0) { 'PASS' } else { 'FAIL' }) "gradlew exit $buildExit - $buildLine ($($tasks.Count) tasks)"
    if ($buildExit -ne 0) {
        $null = Write-Verdict 'build failed'
        exit 1
    }

    # --- packages -------------------------------------------------------------------------------
    $expected = @(
        @{ key = 'phone-store'; family = 'phone'; variant = 'store'; kind = 'apk'; dir = 'phone\build\outputs\apk\store\release'; signer = 'upload' },
        @{ key = 'phone-store'; family = 'phone'; variant = 'store'; kind = 'aab'; dir = 'phone\build\outputs\bundle\storeRelease'; signer = 'upload' },
        @{ key = 'phone-noLegal'; family = 'phone'; variant = 'noLegal'; kind = 'apk'; dir = 'phone\build\outputs\apk\noLegal\release'; signer = 'app' },
        @{ key = 'wear-store'; family = 'wear'; variant = 'store'; kind = 'apk'; dir = 'wear\build\outputs\apk\store\release'; signer = 'upload' },
        @{ key = 'wear-store'; family = 'wear'; variant = 'store'; kind = 'aab'; dir = 'wear\build\outputs\bundle\storeRelease'; signer = 'upload' },
        @{ key = 'wear-noLegal'; family = 'wear'; variant = 'noLegal'; kind = 'apk'; dir = 'wear\build\outputs\apk\noLegal\release'; signer = 'app' },
        @{ key = 'watchface'; family = 'watchface'; variant = ''; kind = 'apk'; dir = 'watchface\build\outputs\apk\release'; signer = 'upload' },
        @{ key = 'watchface'; family = 'watchface'; variant = ''; kind = 'aab'; dir = 'watchface\build\outputs\bundle\release'; signer = 'upload' }
    )
    $packages = @()
    $missing = @()
    foreach ($e in $expected) {
        $file = Get-ChildItem (Join-Path $src $e.dir) -Filter "*.$($e.kind)" -ErrorAction SilentlyContinue | Select-Object -First 1
        if (-not $file) { $missing += "$($e.key) .$($e.kind)"; continue }
        $packages += [pscustomobject]@{ spec = $e; path = $file.FullName; name = $file.Name }
    }
    Add-Check 'packages' $(if ($missing.Count) { 'FAIL' } else { 'PASS' }) $(if ($missing.Count) { "missing: $($missing -join ', ')" } else { "$($packages.Count) packages" })

    foreach ($p in $packages) {
        $p | Add-Member signer (Read-Signer $p.path)
        $p | Add-Member badging $(if ($p.spec.kind -eq 'apk') { Read-ApkBadging $p.path } else { $null })
    }
    $apks = @($packages | Where-Object { $_.spec.kind -eq 'apk' })

    # --- ids ------------------------------------------------------------------------------------
    $badIds = @($apks | Where-Object {
        $want = if ($_.spec.family -eq 'watchface') { $script:WatchFaceId } else { $script:PhoneWatchId }
        $_.badging.package -ne $want } | ForEach-Object { "$($_.spec.key)=$($_.badging.package)" })
    Add-Check 'ids' $(if ($badIds.Count) { 'FAIL' } else { 'PASS' }) $(if ($badIds.Count) { "not the frozen anchor: $($badIds -join ', ')" } else { "$($script:PhoneWatchId) (phone, wear), $($script:WatchFaceId) (face)" })

    # --- signatures -----------------------------------------------------------------------------
    $problems = @()
    $unsigned = @($packages | Where-Object { -not $_.signer } | ForEach-Object name)
    if ($unsigned.Count) { $problems += "unsigned: $($unsigned -join ', ')" }
    $debug = @($packages | Where-Object { $_.signer -and $_.signer.subject -match 'CN=Android Debug' } | ForEach-Object name)
    if ($debug.Count) { $problems += "debug key: $($debug -join ', ')" }
    $groups = @{}
    foreach ($role in 'app', 'upload') {
        $digests = @($packages | Where-Object { $_.spec.signer -eq $role -and $_.signer } | ForEach-Object { $_.signer.sha256 } | Sort-Object -Unique)
        if ($digests.Count -gt 1) { $problems += "the $role-key packages carry $($digests.Count) different certificates" }
        $groups[$role] = $digests | Select-Object -First 1
    }
    if ($groups.app -and $groups.app -eq $groups.upload) { $problems += 'the app signing key and the upload key are one key (owner decision 4.2 wants two)' }
    $sigDetail = if ($problems.Count) { $problems -join '; ' } else { "app key $($groups.app.Substring(0, 16)).. (noLegal), upload key $($groups.upload.Substring(0, 16)).. (store, face)" }
    Add-Check 'signatures' $(if ($problems.Count) { 'FAIL' } else { 'PASS' }) $sigDetail

    # --- version --------------------------------------------------------------------------------
    $names = @($apks | ForEach-Object { $_.badging.versionName } | Sort-Object -Unique)
    $version = $names | Select-Object -First 1
    $vProblems = @()
    if ($names.Count -ne 1) { $vProblems += "versionNames differ: $($names -join ', ')" }
    $instant = ConvertFrom-VersionName $version
    if (-not $instant) { $vProblems += "'$version' is not Y.YM.MDDH.Hmm" }
    elseif ($instant -eq $script:SentinelInstant) { $vProblems += "the sentinel $version (an unstamped build)" }
    else {
        foreach ($a in $apks) {
            $want = Get-ExpectedVersionCode $a.spec.family $instant
            if ($a.badging.versionCode -ne $want) { $vProblems += "$($a.spec.key) code $($a.badging.versionCode), expected $want" }
        }
    }
    $codes = ($apks | Group-Object { $_.spec.family } | ForEach-Object { "$($_.Name) $($_.Group[0].badging.versionCode)" }) -join ', '
    Add-Check 'version' $(if ($vProblems.Count) { 'FAIL' } else { 'PASS' }) $(if ($vProblems.Count) { $vProblems -join '; ' } else { "$version - $codes" })

    # The output folder is named by the version it judged.
    $final = Join-Path $OutRoot $(if ($version) { $version } else { "unversioned-$stamp" })
    if (Test-Path $final) { Remove-Item -Recurse -Force $final }
    Move-Item $out $final
    $out = $final
    $pkgDir = Join-Path $out 'packages'
    New-Item -ItemType Directory -Force $pkgDir | Out-Null
    foreach ($p in $packages) {
        $label = "tourist-$($p.spec.key)-$version.$($p.spec.kind)"
        Copy-Item $p.path (Join-Path $pkgDir $label)
        $p | Add-Member label $label
    }

    # --- catalog (families, contracts) ----------------------------------------------------------
    $claude = Get-Content (Join-Path $script:RepoRoot 'CLAUDE.md') -Raw
    $catalog = if ($claude -match 'in the catalog at `([^`]+)`') { $Matches[1] } else { $null }
    $registryFile = if ($catalog) { Join-Path $catalog '_meta\REGISTRY.md' } else { $null }
    if (-not $registryFile -or -not (Test-Path $registryFile)) {
        Add-Check 'families' 'OPEN' 'UNVERIFIED: the contracts catalog cannot be read from this machine'
        Add-Check 'contracts' 'OPEN' 'UNVERIFIED: the contracts catalog cannot be read from this machine'
    } else {
        $rows = @(Get-Content $registryFile | Where-Object { $_ -match '^\| `[A-Z0-9-]+` \| Tourist Android \|' })
        $row = $rows | Where-Object { $_ -match '^\| `PACKAGE-VERSIONING` ' } | Select-Object -First 1
        $registered = $row -and ($row -notmatch '\| pending \|') -and ($row -notmatch 'not registered')
        Add-Check 'families' $(if ($registered) { 'PASS' } else { 'OPEN' }) $(if ($registered) { 'PACKAGE-VERSIONING families registered' } else { 'PACKAGE-VERSIONING: the Tourist families are not registered in the band table yet' })

        $pointers = Get-Content (Join-Path $script:RepoRoot 'docs\contracts\README.md') | Where-Object { $_ -match '^\| \[' }
        $absent = @()
        foreach ($line in $pointers) {
            $cells = $line -split '\|'
            if ($cells.Count -lt 5 -or $cells[4] -match 'conditional') { continue }
            foreach ($id in [regex]::Matches($cells[2], '`([A-Z0-9-]+)`') | ForEach-Object { $_.Groups[1].Value }) {
                if (-not ($rows | Where-Object { $_ -match "^\| ``$id`` " })) { $absent += $id }
            }
        }
        $pending = @($rows | Where-Object { $_ -match '\| pending \|' } | ForEach-Object { ($_ -split '`')[1] })
        $cDetail = @()
        if ($absent.Count) { $cDetail += "no Tourist row: $($absent -join ', ')" }
        if ($pending.Count) { $cDetail += "verification pending: $($pending -join ', ')" }
        Add-Check 'contracts' $(if ($cDetail.Count) { 'OPEN' } else { 'PASS' }) $(if ($cDetail.Count) { $cDetail -join '; ' } else { "$($rows.Count) Tourist rows, every pointer verified" })
    }

    # --- reach ----------------------------------------------------------------------------------
    $reach = [ordered]@{}
    foreach ($a in $apks) {
        $reach[$a.spec.key] = [ordered]@{ minSdk = $a.badging.minSdk; targetSdk = $a.badging.targetSdk
            nativeCode = $a.badging.nativeCode; requiredFeatures = $a.badging.requiredFeatures }
    }
    $reachDoc = [ordered]@{ version = $version; source = $script:SourceLabel; packages = $reach }
    $reachDoc | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $out 'reach-candidate.json') -Encoding utf8
    if (Test-Path $Baseline) {
        $base = Get-Content $Baseline -Raw | ConvertFrom-Json
        $shrinks = @()
        foreach ($prop in $base.packages.PSObject.Properties) {
            $k = $prop.Name; $b = $prop.Value
            if (-not $reach.Contains($k)) { $shrinks += "$k is gone"; continue }
            $c = $reach[$k]
            if ($c.minSdk -gt $b.minSdk) { $shrinks += "$k minSdk $($b.minSdk) -> $($c.minSdk)" }
            $bAbi = @($b.nativeCode); $cAbi = @($c.nativeCode)
            if ($cAbi.Count -and -not $bAbi.Count) { $shrinks += "$k ABIs any -> $($cAbi -join ',')" }
            foreach ($abi in $bAbi) { if ($cAbi.Count -and $abi -notin $cAbi) { $shrinks += "$k drops ABI $abi" } }
            foreach ($f in @($c.requiredFeatures)) { if ($f -notin @($b.requiredFeatures)) { $shrinks += "$k now requires $f" } }
        }
        Add-Check 'reach' $(if ($shrinks.Count) { 'FAIL' } else { 'PASS' }) $(if ($shrinks.Count) { "shrinks against $(Split-Path $Baseline -Leaf) ($($base.version)): $($shrinks -join '; ')" } else { "no shrink against $($base.version), per package" })
    } else {
        $summary = ($reach.Keys | ForEach-Object { "$_ min $($reach[$_].minSdk)" }) -join ', '
        Add-Check 'reach' 'PASS' "no shipped baseline: this candidate defines it ($summary); commit reach-candidate.json as tools/release/reach-baseline.json with the first release"
    }

    # --- notices --------------------------------------------------------------------------------
    $needNotices = @()
    foreach ($a in $apks) {
        $entries = Get-ZipEntryNames $a.path
        $native = @($entries | Where-Object { $_ -match '^lib/.+\.so$' })
        if ($native.Count -and -not ($entries | Where-Object { $_ -match 'THIRD-PARTY-NOTICES\.txt$' })) { $needNotices += "$($a.spec.key) ($($native.Count) .so)" }
    }
    Add-Check 'notices' $(if ($needNotices.Count) { 'FAIL' } else { 'PASS' }) $(if ($needNotices.Count) { "native binaries without THIRD-PARTY-NOTICES.txt: $($needNotices -join ', ')" } else { 'every package that bundles a native binary carries the notices' })

    # --- privacy and channels -------------------------------------------------------------------
    $stampFile = Get-Content (Join-Path $script:RepoRoot '.sza-canon.json') -Raw | ConvertFrom-Json
    $perm = @('# Permissions per package', '', "Version $version, read from the built packages with aapt2.", '')
    foreach ($a in $apks) { $perm += "## $($a.spec.key)", ''; $perm += @($a.badging.permissions | ForEach-Object { "- ``$_``" }); $perm += '' }
    Set-Content (Join-Path $out 'permissions.md') $perm -Encoding utf8
    Add-Check 'privacy' $(if ($stampFile.privacy.page) { 'PASS' } else { 'OPEN' }) $(if ($stampFile.privacy.page) { "page $($stampFile.privacy.page); permissions.md lists what it must match" } else { 'no privacy page declared in .sza-canon.json; permissions.md lists what it must say' })
    $channelCount = @($stampFile.channels).Count
    Add-Check 'channels' $(if ($channelCount) { 'PASS' } else { 'OPEN' }) $(if ($channelCount) { "$channelCount declared" } else { 'no channel declared in .sza-canon.json' })

    $verdict = Write-Verdict $version
    exit $(if ($verdict -eq 'PASS') { 0 } else { 1 })
} catch {
    Write-Error "preflight could not run: $($_.Exception.Message)" -ErrorAction Continue
    exit 2
}
