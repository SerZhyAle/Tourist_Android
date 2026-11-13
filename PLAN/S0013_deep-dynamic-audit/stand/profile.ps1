#requires -Version 7.0
<#
.SYNOPSIS
    M9 - the disposable profile lifecycle and the backup gate (S0013 section 6).

.DESCRIPTION
    Actions:
      Create    create the AVD from the committed recipe (never an image) and boot it headless
      Install   install the variant's debug APK with its grants and read the version back
      Backup    capture the app's /data/data tree (run-as tar), external-storage additions, the grant set
                and the reachable settings; write and verify the recovery manifest
      Verify    re-hash every manifest entry - the gate a runner calls before its first scene
      Restore   put the captured state back (runner finally, also after a runner crash)
      Destroy   kill the emulator and delete the AVD
      Status    print the live profiles
      PairCheck the pairing health check of [pair] scenes (section 12)

    Exit code: 0 the action completed (Verify: the manifest exists and every checksum agrees);
    1 never - a profile action judges no product behaviour; 2 the action could not complete - a missing or
    disagreeing manifest refuses the run (the backup gate), a boot timeout, adb failure, no pair.
#>
[CmdletBinding(PositionalBinding = $false)]
param(
    [Parameter(Mandatory)] [ValidateSet('Create', 'Install', 'Backup', 'Verify', 'Restore', 'Destroy', 'Status', 'PairCheck')]
    [string] $Action,
    [string] $Recipe = 'phone',
    [ValidateSet('noLegal', 'store')] [string] $Variant = 'noLegal',
    [string] $RunId,
    [int] $BootTimeoutSec = 420
)

. (Join-Path $PSScriptRoot '_stand.ps1')
trap { Write-Host "profile: $($_.Exception.Message)" -ForegroundColor Red; Exit-Verdict -Subject "profile $Action $Recipe" -Code 2 -Detail ($_.Exception.Message -replace '^INVALID:\s*', '') }

# profiles.json is shared by parallel runs (a phone run and a watch run): every write re-reads the file and
# changes only its own entry, under a machine-wide mutex.
function Set-ProfileEntry([string] $Name, $Entry) {
    $m = [Threading.Mutex]::new($false, 'Local\S0013-profiles')
    [void]$m.WaitOne(30000)
    try {
        $all = Get-ActiveProfiles
        if ($null -eq $Entry) { $all.Remove($Name) } else { $all[$Name] = $Entry }
        Write-JsonFile (Join-Path $script:TempRoot 'profiles.json') $all
    } finally { $m.ReleaseMutex(); $m.Dispose() }
}

function Wait-Boot([string] $Serial, [int] $TimeoutSec) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $r = Invoke-Adb -Serial $Serial -Arguments @('shell', 'getprop sys.boot_completed') -TimeoutSec 15 -AllowFail
        if ($r.ExitCode -eq 0 -and $r.Out.Trim() -eq '1') {
            # The package manager answers a little after boot_completed on a cold first boot.
            for ($i = 0; $i -lt 30; $i++) {
                $pm = Invoke-Adb -Serial $Serial -Arguments @('shell', 'pm path android') -TimeoutSec 15 -AllowFail
                if ($pm.Out -match 'package:') { return }
                Start-Sleep -Seconds 2
            }
        }
        Start-Sleep -Seconds 3
    }
    throw "INVALID: $Serial did not finish booting in ${TimeoutSec}s"
}

function New-Profile([string] $Name) {
    $recipes = Get-Recipes
    $r = $recipes[$Name]
    if (-not $r) { throw "INVALID: unknown recipe '$Name'" }
    $profiles = Get-ActiveProfiles
    if ($profiles[$Name]) { throw "INVALID: profile '$Name' is already live on $($profiles[$Name].serial); destroy it first" }
    $serial = "emulator-$($r.port)"
    $busy = (Invoke-Adb -Arguments @('devices')).Out
    if ($busy -match [regex]::Escape($serial)) { throw "INVALID: port $($r.port) is taken by a running emulator that is not this profile" }

    $create = Invoke-Native -FilePath $script:AvdManagerPath -Arguments @("create", "avd", "-n", $r.avd, "-k", $r.image, "-d", $r.device, "--force") -StdinText "no`n" -TimeoutSec 180 -AllowFail
    if ($create.ExitCode -ne 0) { throw "INVALID: avdmanager create failed: $($create.Err.Trim())" }
    $avdDir = Join-Path $env:USERPROFILE ".android\avd\$($r.avd).avd"
    $cfgPath = Join-Path $avdDir 'config.ini'
    $cfg = [ordered]@{}
    foreach ($line in Get-Content $cfgPath) { if ($line -match '^([^=]+)=(.*)$') { $cfg[$Matches[1].Trim()] = $Matches[2] } }
    foreach ($k in $r.config.Keys) { $cfg[$k] = $r.config[$k] }
    ($cfg.Keys | ForEach-Object { "$_=$($cfg[$_])" }) | Set-Content -LiteralPath $cfgPath -Encoding ascii

    $emuArgs = @('-avd', $r.avd, '-port', "$($r.port)") + @($recipes.emulatorFlags)
    New-Item -ItemType Directory -Force -Path $script:TempRoot | Out-Null
    # Shell-executed so the emulator inherits no handle of this process: an inherited stdout pipe would
    # keep the caller reading this script's output forever.
    $psi = [Diagnostics.ProcessStartInfo]::new($script:EmulatorPath)
    foreach ($a in $emuArgs) { $psi.ArgumentList.Add($a) }
    $psi.UseShellExecute = $true
    $psi.WindowStyle = [Diagnostics.ProcessWindowStyle]::Hidden
    $proc = [Diagnostics.Process]::Start($psi)
    Set-ProfileEntry $Name ([ordered]@{ serial = $serial; avd = $r.avd; port = $r.port; pid = $proc.Id; created = (Get-Date).ToString('s'); recipeHash = (Get-RecipeHash $Name) })
    Start-Sleep -Seconds 5
    [void](Invoke-Adb -Arguments @('wait-for-device') -Serial $serial -TimeoutSec $BootTimeoutSec -AllowFail)
    Wait-Boot $serial $BootTimeoutSec
    foreach ($k in $r.settings.Keys) {
        $ns, $key = $k -split ' ', 2
        [void](Invoke-Sh -Serial $serial -Command "settings put $ns $key $($r.settings[$k])")
    }
    [void](Invoke-Sh -Serial $serial -Command 'input keyevent KEYCODE_WAKEUP; wm dismiss-keyguard' -AllowFail)
    $serial
}

function Install-Variant([string] $Name, [string] $Var) {
    $r = (Get-Recipes)[$Name]
    $serial = Get-ProfileSerial $Name
    $apk = Join-Path $script:RepoRoot $r.apk[$Var]
    $id = Get-ApkIdentity $apk
    [void](Invoke-Adb -Serial $serial -Arguments @('install', '-r', '-g', '-t', $apk) -TimeoutSec 240)
    $readback = Assert-BuildUnderTest -Serial $serial -Identity $id
    Install-Probe $serial
    if ($r.Contains("extraPackages")) {
        foreach ($k in $r.extraPackages.Keys) {
            $extra = Join-Path $script:RepoRoot $r.extraPackages[$k]
            if (Test-Path $extra) { [void](Invoke-Adb -Serial $serial -Arguments @('install', '-r', '-t', $extra) -TimeoutSec 240) }
        }
    }
    $entry = (Get-ActiveProfiles)[$Name]
    $entry.variant = $Var
    $entry.build = $id
    Set-ProfileEntry $Name $entry
    $readback
}

function Get-GrantSet([string] $Serial) {
    $d = Invoke-Sh -Serial $Serial -Command "dumpsys package $script:Package"
    $set = [ordered]@{}
    foreach ($m in [regex]::Matches($d, '(android\.permission\.[A-Z_]+): granted=(true|false)')) { $set[$m.Groups[1].Value] = ($m.Groups[2].Value -eq 'true') }
    $set
}

function Backup-Profile([string] $Name, [string] $Id) {
    $serial = Get-ProfileSerial $Name
    $runDir = Get-RunDir $Id
    $bdir = Join-Path $runDir "backup-$Name"
    New-Item -ItemType Directory -Force -Path $bdir | Out-Null
    [void](Invoke-Sh -Serial $serial -Command "am force-stop $script:Package")
    $tar = Join-Path $bdir 'data.tar'
    [void](Invoke-Adb -Serial $serial -Arguments @('exec-out', "run-as $script:Package tar -cf - -C /data/data/$script:Package --exclude=./cache --exclude=./code_cache ." ) -OutFile $tar -TimeoutSec 120)
    if ((Get-Item $tar).Length -lt 1024) { throw "INVALID: run-as tar of /data/data/$script:Package produced no archive (is the build debuggable?)" }
    $files = Invoke-RunAs -Serial $serial -Command 'find . -type f ! -path "./cache/*" ! -path "./code_cache/*" -exec sha256sum {} +' -AllowFail
    Set-Content -LiteralPath (Join-Path $bdir 'data.files.txt') -Value $files -Encoding utf8NoBOM
    $ext = Invoke-Sh -Serial $serial -Command "ls -laR /sdcard/Android/data/$script:Package 2>/dev/null; ls -la /sdcard/Download /sdcard/Pictures /sdcard/Music 2>/dev/null" -AllowFail
    Set-Content -LiteralPath (Join-Path $bdir 'external.txt') -Value $ext -Encoding utf8NoBOM
    Write-JsonFile (Join-Path $bdir 'grants.json') (Get-GrantSet $serial)
    $settings = [ordered]@{}
    foreach ($k in (Get-Recipes).settingsKeysCaptured) {
        $ns, $key = $k -split ' ', 2
        $settings[$k] = (Invoke-Sh -Serial $serial -Command "settings get $ns $key").Trim()
    }
    $settings['app-locales'] = (Invoke-Sh -Serial $serial -Command "cmd locale get-app-locales $script:Package" -AllowFail).Trim()
    Write-JsonFile (Join-Path $bdir 'settings.json') $settings

    $entries = foreach ($f in Get-ChildItem $bdir -File | Where-Object Name -ne 'manifest.json') {
        [ordered]@{ path = $f.Name; size = $f.Length; sha256 = (Get-FileHash $f.FullName -Algorithm SHA256).Hash.ToLowerInvariant() }
    }
    $manifest = [ordered]@{
        runId = $Id; recipe = $Name; serial = $serial; package = $script:Package; taken = (Get-Date).ToString('s')
        entries = @($entries)
        restore = @(
            "am force-stop $script:Package",
            "run-as $($script:Package): remove every entry of /data/data/$script:Package except cache and code_cache, then untar data.tar there",
            'grant/revoke each runtime permission to match grants.json',
            'settings put each key of settings.json; cmd locale set-app-locales to the captured value'
        )
    }
    Write-JsonFile (Join-Path $bdir 'manifest.json') $manifest
    Test-Backup $Name $Id
}

function Test-Backup([string] $Name, [string] $Id) {
    if (-not $Id) { throw 'INVALID: backup gate: no run id given' }
    $bdir = Join-Path (Get-RunDir $Id) "backup-$Name"
    $m = Read-JsonFile (Join-Path $bdir 'manifest.json')
    if (-not $m) { throw "INVALID: backup gate: no recovery manifest for $Name in run $Id - refusing to start" }
    foreach ($e in $m.entries) {
        $p = Join-Path $bdir $e.path
        if (-not (Test-Path $p)) { throw "INVALID: backup gate: $($e.path) is missing" }
        $h = (Get-FileHash $p -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($h -ne $e.sha256) { throw "INVALID: backup gate: checksum of $($e.path) disagrees with the manifest" }
    }
    "$($m.entries.Count) entries verified"
}

function Restore-Profile([string] $Name, [string] $Id) {
    [void](Test-Backup $Name $Id)
    $serial = Get-ProfileSerial $Name
    $bdir = Join-Path (Get-RunDir $Id) "backup-$Name"
    [void](Invoke-Sh -Serial $serial -Command "am force-stop $script:Package")
    [void](Invoke-Adb -Serial $serial -Arguments @('push', (Join-Path $bdir 'data.tar'), '/data/local/tmp/s0013-restore.tar') -TimeoutSec 120)
    [void](Invoke-Sh -Serial $serial -Command 'chmod 644 /data/local/tmp/s0013-restore.tar')
    [void](Invoke-RunAs -Serial $serial -Command 'for e in $(ls -A); do case $e in cache|code_cache) ;; *) rm -rf "$e";; esac; done; tar -xf /data/local/tmp/s0013-restore.tar -C .')
    [void](Invoke-Sh -Serial $serial -Command 'rm -f /data/local/tmp/s0013-restore.tar')
    $grants = Read-JsonFile (Join-Path $bdir 'grants.json')
    $now = Get-GrantSet $serial
    foreach ($perm in $grants.Keys) {
        if ($now[$perm] -eq $grants[$perm]) { continue }
        $verb = if ($grants[$perm]) { 'grant' } else { 'revoke' }
        [void](Invoke-Sh -Serial $serial -Command "pm $verb $script:Package $perm" -AllowFail)
    }
    $settings = Read-JsonFile (Join-Path $bdir 'settings.json')
    foreach ($k in $settings.Keys) {
        if ($k -eq 'app-locales') {
            $loc = if ($settings[$k] -match 'Locales:\s*\[([^\]]*)\]') { $Matches[1] } else { '' }
            [void](Invoke-Sh -Serial $serial -Command "cmd locale set-app-locales $script:Package --locales '$loc'" -AllowFail)
            continue
        }
        $ns, $key = $k -split ' ', 2
        $v = $settings[$k]
        if ($v -eq 'null') { [void](Invoke-Sh -Serial $serial -Command "settings delete $ns $key" -AllowFail) }
        else { [void](Invoke-Sh -Serial $serial -Command "settings put $ns $key '$v'" -AllowFail) }
    }
    $after = Invoke-RunAs -Serial $serial -Command 'find . -type f ! -path "./cache/*" ! -path "./code_cache/*" -exec sha256sum {} +' -AllowFail
    $before = Get-Content -LiteralPath (Join-Path $bdir 'data.files.txt') -Raw
    $norm = { param($t) (($t -split "`n") | ForEach-Object { $_.Trim() } | Where-Object { $_ } | Sort-Object) -join "`n" }
    if ((& $norm $after) -ne (& $norm $before)) { throw "INVALID: restore readback: the data tree of $Name does not hash to the backup" }
    'restored and read back'
}

function Remove-Profile([string] $Name) {
    $profiles = Get-ActiveProfiles
    $p = $profiles[$Name]
    $r = (Get-Recipes)[$Name]
    if ($p) {
        [void](Invoke-Adb -Serial $p.serial -Arguments @('emu', 'kill') -AllowFail -TimeoutSec 20)
        Start-Sleep -Seconds 4
        try { Stop-Process -Id $p.pid -Force -ErrorAction Stop } catch { }
    }
    $del = Invoke-Native -FilePath $script:AvdManagerPath -Arguments @("delete", "avd", "-n", $r.avd) -TimeoutSec 120 -AllowFail
    Set-ProfileEntry $Name $null
    "destroyed ($($del.ExitCode))"
}

function Test-Pair {
    $profiles = Get-ActiveProfiles
    $phone = $profiles['phone']; $watch = $profiles['wear']
    if (-not $phone -or -not $watch) { throw 'INVALID: pairing health: the phone and the watch profiles are not both live' }
    $companion = Invoke-Sh -Serial $phone.serial -Command 'pm list packages com.google.android.wearable.app' -AllowFail
    if ($companion -notmatch 'package:') {
        throw 'INVALID: pairing health: the phone image carries no Wear OS companion app (no Google sign-in on the emulator) - the pair cannot be built'
    }
    $nodes = Invoke-Sh -Serial $watch.serial -Command 'dumpsys activity service WearableService | grep -i -E "connected|node" | head -n 20' -AllowFail
    if ($nodes -notmatch '(?i)isConnected=true|connected: true') { throw 'INVALID: pairing health: the watch reports no connected node' }
    'pair healthy'
}

switch ($Action) {
    'Create' { $s = New-Profile $Recipe; Exit-Verdict -Subject "profile Create $Recipe" -Code 0 -Detail "$s booted" }
    'Install' { $rb = Install-Variant $Recipe $Variant; Exit-Verdict -Subject "profile Install $Recipe" -Code 0 -Detail "$Variant $($rb.versionName) ($($rb.versionCode)) sha256 $($rb.sha256.Substring(0, 12))" }
    'Backup' { $d = Backup-Profile $Recipe $RunId; Exit-Verdict -Subject "profile Backup $Recipe" -Code 0 -Detail $d }
    'Verify' { $d = Test-Backup $Recipe $RunId; Exit-Verdict -Subject "profile Verify $Recipe" -Code 0 -Detail $d }
    'Restore' { $d = Restore-Profile $Recipe $RunId; Exit-Verdict -Subject "profile Restore $Recipe" -Code 0 -Detail $d }
    'Destroy' { $d = Remove-Profile $Recipe; Exit-Verdict -Subject "profile Destroy $Recipe" -Code 0 -Detail $d }
    'Status' { Get-ActiveProfiles | ConvertTo-Json -Depth 6; Exit-Verdict -Subject 'profile Status' -Code 0 }
    'PairCheck' { $d = Test-Pair; Exit-Verdict -Subject 'profile PairCheck' -Code 0 -Detail $d }
}
