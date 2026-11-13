#requires -Version 7.0
<#
.SYNOPSIS
    Builds the S0013 accessibility probe APK (stand instrument, never shipped) into temp/S0013/probe/.

.DESCRIPTION
    aapt2 compile + link, javac, d8, zipalign, apksigner with the local debug key. No Gradle: the probe is
    not a module of the product build and must never become one.

    Exit code: 0 the APK was built and signed; 1 never; 2 the build could not run (tool missing, compile
    error).
#>
[CmdletBinding(PositionalBinding = $false)]
param([int] $CompileSdk = 36)

. (Join-Path $PSScriptRoot '..\_stand.ps1')
trap { Write-Host "probe build: $($_.Exception.Message)" -ForegroundColor Red; Exit-Verdict -Subject 'probe-a11y build' -Code 2 -Detail ($_.Exception.Message -replace '^INVALID:\s*', '') }

$bt = Split-Path (Get-Aapt2Path) -Parent
$androidJar = Join-Path $script:Sdk "platforms\android-$CompileSdk\android.jar"
if (-not (Test-Path $androidJar)) { throw "INVALID: $androidJar not found" }
$out = Join-Path $script:TempRoot 'probe'
Remove-Item $out -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path "$out\classes", "$out\dex" | Out-Null

[void](Invoke-Native -FilePath (Join-Path $bt 'aapt2.exe') -Arguments @('compile', '--dir', (Join-Path $PSScriptRoot 'res'), '-o', "$out\res.zip"))
[void](Invoke-Native -FilePath (Join-Path $bt 'aapt2.exe') -Arguments @('link', '-I', $androidJar, '--manifest', (Join-Path $PSScriptRoot 'AndroidManifest.xml'), '-o', "$out\unsigned.apk", "$out\res.zip"))
$sources = @(Get-ChildItem (Join-Path $PSScriptRoot 'src') -Recurse -Filter *.java | ForEach-Object FullName)
[void](Invoke-Native -FilePath 'javac' -Arguments (@('--release', '11', '-classpath', $androidJar, '-d', "$out\classes") + $sources) -TimeoutSec 180)
$classes = @(Get-ChildItem "$out\classes" -Recurse -Filter *.class | ForEach-Object FullName)
[void](Invoke-Native -FilePath (Join-Path $bt 'd8.bat') -Arguments (@('--lib', $androidJar, '--min-api', '26', '--output', "$out\dex") + $classes) -TimeoutSec 180)

Add-Type -AssemblyName System.IO.Compression, System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::Open("$out\unsigned.apk", 'Update')
try { [void][IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, "$out\dex\classes.dex", 'classes.dex') } finally { $zip.Dispose() }

[void](Invoke-Native -FilePath (Join-Path $bt 'zipalign.exe') -Arguments @('-f', '4', "$out\unsigned.apk", "$out\aligned.apk"))
$ks = Join-Path $env:USERPROFILE '.android\debug.keystore'
[void](Invoke-Native -FilePath (Join-Path $bt 'apksigner.bat') -Arguments @('sign', '--ks', $ks, '--ks-pass', 'pass:android', '--ks-key-alias', 'androiddebugkey', '--key-pass', 'pass:android', '--out', "$out\s0013probe.apk", "$out\aligned.apk") -TimeoutSec 120)
Exit-Verdict -Subject 'probe-a11y build' -Code 0 -Detail "$out\s0013probe.apk"
