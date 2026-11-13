#requires -Version 7.0
# The scene engine shared by M5, M6, M7 and M10: reads the matrix from the ticket, prepares a scene on a
# live profile, runs its driver under the sampler, judges every requested cell and writes the evidence to
# the run folder. Dot-sourced after _stand.ps1, _instruments.ps1, _judges.ps1 and scenes.ps1.

$script:AllProperties = 'P1', 'P2', 'P3', 'P4', 'P5'


# Section 3.2 is the source: one row per scene, `[p]`/`[w]`/`[pair]` tags, five cells.
function Read-Matrix([string] $TicketPath = $script:TicketFile) {
    $rows = [ordered]@{}
    foreach ($line in Get-Content -LiteralPath $TicketPath) {
        if ($line -notmatch '^\|\s*([A-H]\d+)\s+(.+?)\s*\|(.+)\|\s*$') { continue }
        $id = $Matches[1]; $title = $Matches[2]; $rest = $Matches[3]
        $cells = @($rest -split '\|' | ForEach-Object { $_.Trim() })
        if ($cells.Count -ne 5) { continue }
        $tags = if ($title -match '`\[([a-z,]+)\]`') { @($Matches[1] -split ',') } else { @() }
        $devices = @()
        if ($tags -contains 'p') { $devices += 'phone' }
        if ($tags -contains 'w') { $devices += 'watch' }
        $pair = $tags -contains 'pair'
        $parsed = [ordered]@{}
        for ($i = 0; $i -lt 5; $i++) {
            $c = $cells[$i]
            $state = if ($c -match '^open\b') { 'open' } elseif ($c -match '^n/a\b') { 'n/a' } elseif ($c -match '^measured\b') { 'measured' } else { 'unknown' }
            $inst = @([regex]::Matches(($c -replace '^n/a.*$', ''), '\bM\d+\b') | ForEach-Object Value)
            $parsed[$script:AllProperties[$i]] = [ordered]@{ state = $state; instruments = ($inst -join ', '); text = $c }
        }
        $rows[$id] = [ordered]@{ id = $id; group = $id.Substring(0, 1); title = ($title -replace '\s*`\[[a-z,]+\]`', ''); devices = $devices; pair = $pair; cells = $parsed }
    }
    $rows
}

$script:PhaseScenes = @{
    '1' = 'C4', 'C5', 'C6', 'B1', 'B2', 'B3', 'F4', 'F5', 'F6'
    '2' = 'A1', 'A2', 'A3', 'A4', 'G1', 'G2', 'G3'
    '3' = 'D1', 'D2', 'D3', 'D4', 'D5', 'D6', 'E1', 'E2', 'E3'
    '4' = 'C1', 'C2', 'C3', 'C4', 'C5', 'C6', 'C7', 'C8', 'C9', 'C10'
}

function Get-ControlFor([string] $Recipe) {
    $hash = Get-RecipeHash $Recipe
    $c = Read-JsonFile (Join-Path $script:TempRoot "control\$Recipe-$hash.json")
    $c
}

function New-SceneCtx {
    param([string] $SceneId, [string] $Role, [string] $Serial, [string] $RunDir, [int] $Uid, [int] $Density, [bool] $WithM7, [int] $SoakMinutes, [string] $SoakScreen, [int] $Rep)
    $sub = if ($SoakScreen) { "$SceneId-$SoakScreen" } else { $SceneId }
    $dir = Join-Path $RunDir ("scenes\{0}-{1}-r{2}" -f $sub, $Role, $Rep)
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    @{
        SceneId = $SceneId; SubId = $sub; Role = $Role; Serial = $Serial; Uid = $Uid; Density = $Density; RunDir = $RunDir; SceneDir = $dir; Rep = $Rep
        Marks = [Collections.Generic.List[object]]::new(); Spans = [Collections.Generic.List[object]]::new()
        Starts = [Collections.Generic.List[object]]::new(); Latencies = [Collections.Generic.List[object]]::new()
        Torch = [Collections.Generic.List[object]]::new(); Holds = [Collections.Generic.List[object]]::new()
        Faults = [Collections.Generic.List[object]]::new(); Toggles = [Collections.Generic.List[object]]::new()
        Checkpoints = [Collections.Generic.List[object]]::new(); Notes = [Collections.Generic.List[string]]::new()
        Stateless = [Collections.Generic.List[string]]::new(); M7 = [Collections.Generic.List[object]]::new()
        Persist = @{}; WithM7 = $WithM7; SoakMinutes = $SoakMinutes; SoakScreen = $SoakScreen
        IdleWindow = $null; IdleBackground = $false; IdleLogLines = 0; BootCompletedAt = $null
        Sampler = $null; StopFile = (Join-Path $dir 'sampler.stop'); Csv = (Join-Path $dir 'samples.csv'); IntervalSec = 2
        OutcomeSetApproved = [bool](Get-OutcomeSet).approved
    }
}

# Every scene starts from the run's baseline: the recovery manifest restored, English, font 1.0, the
# grant set of the install, a clean logcat, the device awake on its launcher.
function Initialize-Scene($ctx, [string] $Recipe, [string] $RunId) {
    Assert-DeviceAlive $ctx.Serial
    $r = Invoke-Native -FilePath 'pwsh' -Arguments @('-NoProfile', '-File', (Join-Path $script:StandRoot 'profile.ps1'), '-Action', 'Restore', '-Recipe', $Recipe, '-RunId', $RunId) -TimeoutSec 300 -AllowFail
    if ($r.ExitCode -ne 0) { throw "INVALID: baseline restore before $($ctx.SubId) failed: $(($r.Out -split "`n" | Where-Object { $_ } | Select-Object -Last 1))" }
    [void](Sx $ctx 'settings put system font_scale 1.0; settings delete system time_12_24; cmd notification set_dnd off; dumpsys battery reset' -AllowFail)
    Set-AppLocale $ctx ''
    Set-ProbeBound $ctx.Serial $false
    if ($ctx.Role -eq 'phone') { [void](Emu $ctx ("geo fix {0} {1} 50 8" -f $script:BaseFix[0], $script:BaseFix[1])) }
    Wake-Device $ctx
    Key $ctx KEYCODE_HOME
    Clear-Logcat $ctx.Serial
    Pause-Sec 1
}

function Invoke-Scene {
    param([string] $SceneId, [string] $Role, [string] $Recipe, [string] $RunId, [string[]] $Properties, $Matrix, [bool] $WithM7, [int] $SoakMinutes, [string] $SoakScreen, [int] $Rep = 1, $KeySets)
    $runDir = Get-RunDir $RunId
    $row = $Matrix[$SceneId]
    $serial = Get-ProfileSerial $Recipe
    $uid = Get-AppUid $serial
    $density = Get-DensityDpi $serial
    $ctx = New-SceneCtx -SceneId $SceneId -Role $Role -Serial $serial -RunDir $runDir -Uid $uid -Density $density -WithM7 $WithM7 -SoakMinutes $SoakMinutes -SoakScreen $SoakScreen -Rep $Rep
    $control = Get-ControlFor $Recipe
    $cells = [ordered]@{}
    foreach ($p in $Properties) {
        $spec = $row.cells[$p]
        if ($spec.state -ne 'open' -and $spec.state -ne 'measured') { continue }
        $cells[$p] = New-Cell $ctx.SubId $Role $p $spec.instruments
    }
    if ($cells.Count -eq 0) { return @() }

    $driver = $script:Drivers[$SceneId]
    $drive = if ($driver) { $driver[$Role] } else { $null }
    $sceneInvalid = $null
    if (-not $drive) { $sceneInvalid = "no $Role driver for $SceneId" }
    $logPath = Join-Path $ctx.SceneDir 'logcat.txt'
    $gfx = $null
    $started = Get-Date
    if (-not $sceneInvalid) {
        try {
            Initialize-Scene $ctx $Recipe $RunId
            $ctx.HostT0 = Get-NowMs; $ctx.MonoT0 = Get-DeviceUptimeMs $serial
            Restart-Sampler $ctx
            Pause-Sec 4
            Reset-Gfx $serial
            Mark $ctx 'scene-start'
            & $drive $ctx
            Mark $ctx 'scene-end'
            $gfx = Read-Gfx $serial
            Pause-Sec 2
        } catch {
            $sceneInvalid = $_.Exception.Message -replace '^INVALID:\s*', ''
            Note $ctx "scene aborted: $sceneInvalid"
        } finally {
            Stop-SamplerFor $ctx
            try { Save-Logcat -Serial $serial -Path $logPath } catch { Note $ctx "logcat harvest failed: $($_.Exception.Message)" }
            try { Set-ProbeBound $serial $false; Set-FontScale $ctx 1.0; Set-AppLocale $ctx '' } catch { }
        }
    }
    $samples = @(Read-Samples $ctx.Csv)
    $samplerErrors = Get-SamplerErrors $ctx.Csv
    $logcat = if (Test-Path $logPath) { Get-LogcatFacts -Path $logPath -Uid $uid -InjectedCoords @($script:BaseFix[1], $script:BaseFix[0]) } else { $null }
    if ($ctx.IdleWindow -and $ctx.IdleWindow.to -and (Test-Path $logPath)) {
        $from = $ctx.MonoT0 + ($ctx.IdleWindow.from - $ctx.HostT0); $to = $ctx.MonoT0 + ($ctx.IdleWindow.to - $ctx.HostT0)
        $ctx.IdleLogLines = (Get-LogcatFacts -Path $logPath -Uid $uid -FromMonoMs $from -ToMonoMs $to).appLines
    }
    $keyNow = $null; $keyPrev = $null
    if (-not $sceneInvalid -and $cells.Contains('P4')) {
        try { $keyNow = Get-PrefsKeySet $ctx } catch { }
        $prevKey = "$Role/$($ctx.SubId)"
        if ($KeySets -and $KeySets.Contains($prevKey)) { $keyPrev = $KeySets[$prevKey] } elseif ($KeySets -and $null -ne $keyNow) { $KeySets[$prevKey] = $keyNow }
    }

    $results = foreach ($p in $cells.Keys) {
        $cell = $cells[$p]
        if ($sceneInvalid) { $cell.invalid.Add($sceneInvalid); [void](Complete-Cell $cell); $cell; continue }
        if ($samplerErrors -ge 3) { $cell.invalid.Add("sampler failed $samplerErrors times (probe self-check)") }
        switch ($p) {
            'P1' { [void](Invoke-JudgeP1 -Cell $cell -Ctx $ctx -Logcat $logcat -Gfx $gfx -Control $control) }
            'P2' { [void](Invoke-JudgeP2 -Cell $cell -Ctx $ctx -Logcat $logcat) }
            'P3' { [void](Invoke-JudgeP3 -Cell $cell -Ctx $ctx -Samples $samples -Logcat $logcat -Control $control -IntervalMs ($ctx.IntervalSec * 1000)) }
            'P4' { [void](Invoke-JudgeP4 -Cell $cell -Ctx $ctx -Samples $samples -Control $control -KeySetPrev $keyPrev -KeySetNow $keyNow) }
            'P5' { [void](Invoke-JudgeP5 -Cell $cell -Ctx $ctx) }
        }
        $cell
    }
    $evidence = [ordered]@{
        scene = $ctx.SubId; role = $Role; rep = $Rep; recipe = $Recipe; runId = $RunId; seconds = [math]::Round(((Get-Date) - $started).TotalSeconds)
        invalid = $sceneInvalid; notes = @($ctx.Notes); starts = @($ctx.Starts); latencies = @($ctx.Latencies); torch = @($ctx.Torch)
        faults = @($ctx.Faults); toggles = @($ctx.Toggles); holds = @($ctx.Holds); spans = @($ctx.Spans); marks = @($ctx.Marks)
        gfx = $gfx; samples = $samples.Count; samplerErrors = $samplerErrors; m7 = @($ctx.M7); stateless = @($ctx.Stateless)
        idleWindow = $ctx.IdleWindow; idleBackground = $ctx.IdleBackground; idleLogLines = $ctx.IdleLogLines; persist = $ctx.Persist; checkpoints = @($ctx.Checkpoints); density = $ctx.Density; uid = $uid; properties = @($cells.Keys); instruments = @($cells.Values | ForEach-Object { [ordered]@{ property = $_.property; instruments = $_.instruments } })
        logcat = if ($logcat) { [ordered]@{ appLines = $logcat.appLines; crashes = $logcat.crashes; anrs = $logcat.anrs; fatal = $logcat.fatal; secrets = $logcat.secrets; exceptions = $logcat.exceptions } } else { $null }
        cells = @($results)
    }
    Write-JsonFile (Join-Path $ctx.SceneDir 'evidence.json') $evidence
    @($results)
}

function Format-CellLine($Cell) {
    $word = Get-VerdictWord $Cell.code
    "S0013 $($Cell.scene) $($Cell.device) $($Cell.property): $word ($($Cell.detail))"
}
