<#
.SYNOPSIS
    Creates the two release keys of owner decision 4.2 and the keystore.properties the build reads.

.DESCRIPTION
    Run by the owner, once, in an interactive console: the key is the owner's to create and no agent
    generates anything under P:\ANDROID\Tourist_credentials. Writes, into -Destination:

      tourist-app-signing.jks  the product's own key: signs the sideload (noLegal) packages and is imported
                               into Play App Signing, so Play-delivered and sideload builds carry one signature
      tourist-upload.jks       the separate upload key: signs what is uploaded to Play (store variant, watch face)
      keystore.properties      both keys' paths and passwords, in the format build-logic ReleaseSigning reads

    Both keys are RSA 4096, PKCS12, valid 10000 days (Play requires validity past 2033-10-22). Passwords
    are asked twice each and passed to keytool through the environment, never on its command line. The
    script never overwrites an existing key or properties file.

    -Rehearsal makes throwaway keys with random passwords for a pipeline dry run. They are refused anywhere
    but under the repository's temp/ (gitignored), and nothing signed with them may leave this machine.

.EXAMPLE
    pwsh -NoProfile -File tools/release/new-release-keys.ps1
    pwsh -NoProfile -File tools/release/new-release-keys.ps1 -Rehearsal -Destination temp/S0010/keys

.NOTES
    Exit codes: 0 keys written, 1 refused (a file exists, a password mismatch, a wrong destination),
    2 keytool failed.
#>
[CmdletBinding()]
param(
    [string] $Destination = 'P:\ANDROID\Tourist_credentials',
    [string] $DistinguishedName = 'CN=SZA Tourist, O=SZA',
    [switch] $Rehearsal
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot '_release.ps1')

function Stop-Refused([string] $Message) {
    Write-Error $Message -ErrorAction Continue
    exit 1
}

$target = [System.IO.Path]::GetFullPath($(if ([System.IO.Path]::IsPathRooted($Destination)) { $Destination } else { Join-Path $script:RepoRoot $Destination }))
$repoTemp = [System.IO.Path]::GetFullPath((Join-Path $script:RepoRoot 'temp')) + '\'
$insideRepo = $target.StartsWith($script:RepoRoot + '\', [StringComparison]::OrdinalIgnoreCase)
if ($Rehearsal -and -not $target.StartsWith($repoTemp, [StringComparison]::OrdinalIgnoreCase)) {
    Stop-Refused "Rehearsal keys go under $repoTemp only, not $target."
}
if (-not $Rehearsal -and $insideRepo) {
    Stop-Refused "Release keys never live inside the repository ($target); use a folder outside it."
}

$keys = @(
    @{ File = 'tourist-app-signing.jks'; Alias = 'tourist'; Prefix = ''; Role = 'app signing key' },
    @{ File = 'tourist-upload.jks'; Alias = 'tourist-upload'; Prefix = 'upload.'; Role = 'upload key' }
)
$properties = Join-Path $target 'keystore.properties'
foreach ($path in @($properties) + ($keys | ForEach-Object { Join-Path $target $_.File })) {
    if (Test-Path $path) { Stop-Refused "$path exists; this script never overwrites a key." }
}

function Read-NewPassword([string] $Role) {
    if ($Rehearsal) {
        $bytes = [byte[]]::new(24)
        [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
        return [Convert]::ToBase64String($bytes) -replace '[+/=]', 'x'
    }
    $first = Read-Host "Password for the $Role (6+ characters)" -AsSecureString
    $second = Read-Host "Repeat the password for the $Role" -AsSecureString
    $a = [System.Net.NetworkCredential]::new('', $first).Password
    $b = [System.Net.NetworkCredential]::new('', $second).Password
    if ($a -ne $b) { Stop-Refused "The two passwords for the $Role differ." }
    if ($a.Length -lt 6) { Stop-Refused "keytool needs 6 or more characters for the $Role." }
    return $a
}

$keytool = Get-JdkTool 'keytool'
New-Item -ItemType Directory -Force $target | Out-Null
$lines = @($(if ($Rehearsal) { '# REHEARSAL keys of Tourist: throwaway, nothing signed with them ever ships.' } else { '# Release keys of Tourist (build-logic ReleaseSigning). Never commit this file.' }))
foreach ($key in $keys) {
    $password = Read-NewPassword $key.Role
    $env:TOURIST_NEW_KEY_PASSWORD = $password
    try {
        & $keytool -genkeypair -keystore (Join-Path $target $key.File) -storetype PKCS12 -alias $key.Alias `
            -keyalg RSA -keysize 4096 -validity 10000 -dname $DistinguishedName `
            -storepass:env TOURIST_NEW_KEY_PASSWORD -keypass:env TOURIST_NEW_KEY_PASSWORD 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) {
            Write-Error "keytool failed to create the $($key.Role)." -ErrorAction Continue
            exit 2
        }
        $listing = & $keytool -list -v -keystore (Join-Path $target $key.File) -storetype PKCS12 `
            -storepass:env TOURIST_NEW_KEY_PASSWORD 2>&1
        $key.Sha256 = ($listing | Where-Object { $_ -match '^\s*SHA256: ' } | Select-Object -First 1) -replace '^\s*SHA256: ', ''
    } finally {
        Remove-Item Env:TOURIST_NEW_KEY_PASSWORD -ErrorAction SilentlyContinue
    }
    $lines += "$($key.Prefix)storeFile=$($key.File)"
    $lines += "$($key.Prefix)storePassword=$password"
    $lines += "$($key.Prefix)keyAlias=$($key.Alias)"
    $lines += "$($key.Prefix)keyPassword=$password"
}
Set-Content -Path $properties -Value $lines -Encoding ascii

$kind = if ($Rehearsal) { 'Rehearsal keys (throwaway, never ship anything signed with them)' } else { 'Release keys' }
Write-Output "$kind written to $target"
foreach ($key in $keys) {
    Write-Output ("  {0,-24} {1,-16} SHA-256 {2}" -f $key.File, $key.Role, $key.Sha256)
}
Write-Output "Point the build at them: `$env:TOURIST_KEYSTORE_PROPERTIES = '$properties'"
exit 0
