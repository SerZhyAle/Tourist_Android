# Shared helpers of the release pipeline scripts (dot-sourced, never run directly).

Set-StrictMode -Version Latest

$script:RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path

# The phone app and the watch app share com.sza.tourist; the watch face has an id of its own.
$script:PhoneWatchId = 'com.sza.tourist'
$script:WatchFaceId = 'com.sza.tourist.watchface'

# The checked-in sentinel of PACKAGE-VERSIONING rule 7 (build-logic VersionStamp.SENTINEL_INSTANT).
$script:SentinelInstant = '2610230000'

function Get-SdkRoot {
    if ($env:ANDROID_HOME -and (Test-Path $env:ANDROID_HOME)) { return $env:ANDROID_HOME }
    $local = Join-Path $script:RepoRoot 'local.properties'
    if (Test-Path $local) {
        $line = Get-Content $local | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
        if ($line) { return ($line -replace '^sdk\.dir=', '' -replace '\\:', ':' -replace '\\\\', '\') }
    }
    $default = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    if (Test-Path $default) { return $default }
    throw 'Android SDK not found: set ANDROID_HOME or sdk.dir in local.properties.'
}

function Get-BuildTool([string] $Name) {
    $dir = Get-ChildItem (Join-Path (Get-SdkRoot) 'build-tools') -Directory |
        Sort-Object { [version]($_.Name -replace '[^0-9.].*$', '') } -Descending | Select-Object -First 1
    if (-not $dir) { throw 'No Android build-tools installed.' }
    foreach ($candidate in @("$Name.exe", "$Name.bat", $Name)) {
        $path = Join-Path $dir.FullName $candidate
        if (Test-Path $path) { return $path }
    }
    throw "build-tools $($dir.Name) has no $Name."
}

function Get-JdkTool([string] $Name) {
    if ($env:JAVA_HOME) {
        $path = Join-Path $env:JAVA_HOME "bin\$Name.exe"
        if (Test-Path $path) { return $path }
    }
    $onPath = Get-Command $Name -ErrorAction SilentlyContinue
    if ($onPath) { return $onPath.Source }
    throw "$Name not found: set JAVA_HOME to a JDK."
}

function Get-Adb { return (Join-Path (Get-SdkRoot) 'platform-tools\adb.exe') }

# versionName and versionCode as PACKAGE-VERSIONING renders them; mirrors build-logic VersionStamp.
function ConvertTo-VersionName([string] $Instant) {
    return '{0}.{1}{2}.{3}{4}{5}.{6}{7}' -f $Instant[0], $Instant[1], $Instant[2], $Instant[3],
        $Instant.Substring(4, 2), $Instant[6], $Instant[7], $Instant.Substring(8, 2)
}

function ConvertFrom-VersionName([string] $Name) {
    if ($Name -notmatch '^(\d)\.(\d{2})\.(\d{4})\.(\d{3})$') { return $null }
    return $Matches[1] + $Matches[2] + $Matches[3] + $Matches[4]
}

function Get-ExpectedVersionCode([string] $Family, [string] $Instant) {
    $minute = [int]$Instant.Substring(8, 2)
    $partition = switch ($Family) {
        'phone' { [math]::Floor($minute / 10) }
        'watchface' { [math]::Floor($minute / 10) }
        'wear' { 6 + [math]::Floor($minute / 15) }
        default { throw "Unknown package family '$Family'." }
    }
    return [int64]$Instant.Substring(0, 8) * 10 + $partition
}

# What a built APK declares, read with aapt2 from the package itself (never from the sources).
function Read-ApkBadging([string] $Apk) {
    $lines = & (Get-BuildTool 'aapt2') dump badging $Apk 2>$null
    if ($LASTEXITCODE -ne 0) { throw "aapt2 could not read $Apk." }
    $info = [ordered]@{ package = $null; versionCode = $null; versionName = $null; minSdk = $null
        targetSdk = $null; nativeCode = @(); requiredFeatures = @(); optionalFeatures = @(); permissions = @() }
    foreach ($line in $lines) {
        if ($line -match "^package: name='([^']+)' versionCode='([^']+)' versionName='([^']*)'") {
            $info.package = $Matches[1]; $info.versionCode = [int64]$Matches[2]; $info.versionName = $Matches[3]
        } elseif ($line -match "^(?:minSdkVersion|sdkVersion):'(\d+)'") {
            $info.minSdk = [int]$Matches[1]
        } elseif ($line -match "^targetSdkVersion:'(\d+)'") {
            $info.targetSdk = [int]$Matches[1]
        } elseif ($line -match '^native-code: (.*)$') {
            $info.nativeCode = @([regex]::Matches($Matches[1], "'([^']+)'") | ForEach-Object { $_.Groups[1].Value })
        } elseif ($line -match "^uses-feature(-not-required)?: name='([^']+)'") {
            if ($Matches[1]) { $info.optionalFeatures += $Matches[2] } else { $info.requiredFeatures += $Matches[2] }
        } elseif ($line -match "^uses-permission: name='([^']+)'") {
            $info.permissions += $Matches[1]
        }
    }
    $info.requiredFeatures = @($info.requiredFeatures | Sort-Object -Unique)
    $info.optionalFeatures = @($info.optionalFeatures | Sort-Object -Unique)
    $info.permissions = @($info.permissions | Sort-Object -Unique)
    return [pscustomobject]$info
}

# SHA-256 of the signing certificate and its subject; $null when the package carries no signature.
function Read-Signer([string] $Package) {
    if ($Package -like '*.apk') {
        $out = & (Get-BuildTool 'apksigner') verify --print-certs $Package 2>&1
        if ($LASTEXITCODE -ne 0) { return $null }
        $digest = ($out | Where-Object { $_ -match 'certificate SHA-256 digest: ([0-9a-f]+)' } | Select-Object -First 1)
        $subject = ($out | Where-Object { $_ -match 'certificate DN: (.+)$' } | Select-Object -First 1)
        if (-not $digest) { return $null }
        $null = $digest -match 'certificate SHA-256 digest: ([0-9a-f]+)'; $sha = $Matches[1]
        $null = $subject -match 'certificate DN: (.+)$'; $dn = $Matches[1]
        return [pscustomobject]@{ sha256 = $sha; subject = $dn }
    }
    $out = & (Get-JdkTool 'keytool') -printcert -jarfile $Package 2>&1
    $sha = $out | Where-Object { $_ -match '^\s*SHA256: ([0-9A-F:]+)' } | Select-Object -First 1
    if ($LASTEXITCODE -ne 0 -or -not $sha) { return $null }
    $null = $sha -match 'SHA256: ([0-9A-F:]+)'; $hex = ($Matches[1] -replace ':', '').ToLowerInvariant()
    $owner = $out | Where-Object { $_ -match '^\s*Owner: (.+)$' } | Select-Object -First 1
    $null = $owner -match 'Owner: (.+)$'
    return [pscustomobject]@{ sha256 = $hex; subject = $Matches[1] }
}

function Get-ZipEntryNames([string] $Path) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($Path)
    try { return @($zip.Entries | ForEach-Object FullName) } finally { $zip.Dispose() }
}
