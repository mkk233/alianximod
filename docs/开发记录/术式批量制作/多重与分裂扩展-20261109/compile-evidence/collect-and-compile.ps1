$ErrorActionPreference = 'Stop'
$root = 'D:\alianximod'
$evidence = $PSScriptRoot
$batch = Split-Path -Parent $evidence
$freezeFile = Get-ChildItem -LiteralPath $batch -File -Filter '*.json' |
    Where-Object { $_.Name -like '*v3*' -and $_.Length -gt 100000 } |
    Select-Object -First 1
if (-not $freezeFile) { throw 'freeze fingerprint json not found' }
$freezePath = $freezeFile.FullName

function Read-FreezeEntries {
    param([string]$Path)
    $raw = [System.IO.File]::ReadAllText($Path)
    $rx = [regex]'\"path\"\s*:\s*\"((?:\\.|[^\"\\])*)\"\s*,\s*\"sha256\"\s*:\s*\"([A-Fa-f0-9]+)\"\s*,\s*\"lastWriteTimeUtc\"\s*:\s*\"([^\"]+)\"'
    $list = New-Object System.Collections.Generic.List[object]
    foreach ($match in $rx.Matches($raw)) {
        $list.Add([pscustomobject]@{
            path = [regex]::Unescape($match.Groups[1].Value)
            sha256 = $match.Groups[2].Value.ToUpperInvariant()
            lastWriteTimeUtc = $match.Groups[3].Value
        }) | Out-Null
    }
    return $list
}

function Format-Utc([datetime]$Value) {
    return $Value.ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ss.fffffff') + 'Z'
}

function Measure-Tree {
    param($Entries)
    $missing = New-Object System.Collections.Generic.List[string]
    $hashMismatch = New-Object System.Collections.Generic.List[string]
    $timeMismatch = New-Object System.Collections.Generic.List[string]
    $rows = New-Object System.Collections.Generic.List[object]
    $maxWrite = [datetimeoffset]::MinValue
    $maxPath = ''
    $maxDeltaMs = 0.0
    foreach ($entry in $Entries) {
        if (-not (Test-Path -LiteralPath $entry.path)) {
            $missing.Add($entry.path) | Out-Null
            continue
        }
        $item = Get-Item -LiteralPath $entry.path
        $hash = (Get-FileHash -LiteralPath $entry.path -Algorithm SHA256).Hash.ToUpperInvariant()
        $diskText = Format-Utc $item.LastWriteTimeUtc
        $disk = [datetimeoffset]::Parse($diskText, [Globalization.CultureInfo]::InvariantCulture, [Globalization.DateTimeStyles]::RoundtripKind)
        if ($disk -gt $maxWrite) { $maxWrite = $disk; $maxPath = $entry.path }
        if ($hash -ne $entry.sha256) { $hashMismatch.Add($entry.path) | Out-Null }
        $stored = [datetimeoffset]::Parse($entry.lastWriteTimeUtc, [Globalization.CultureInfo]::InvariantCulture, [Globalization.DateTimeStyles]::RoundtripKind)
        $delta = [math]::Abs(($disk - $stored).TotalMilliseconds)
        if ($delta -gt $maxDeltaMs) { $maxDeltaMs = $delta }
        if ($delta -gt 1) { $timeMismatch.Add($entry.path) | Out-Null }
        $rows.Add([pscustomobject]@{
            path = $entry.path
            sha256 = $hash
            lastWriteTimeUtc = $diskText
            length = $item.Length
        }) | Out-Null
    }
    return [pscustomobject]@{
        rows = $rows
        missing = $missing
        hashMismatch = $hashMismatch
        timeMismatch = $timeMismatch
        maxWrite = $maxWrite
        maxPath = $maxPath
        maxDeltaMs = $maxDeltaMs
    }
}

function Find-Extras {
    param($Entries)
    $seen = New-Object 'System.Collections.Generic.HashSet[string]' ([StringComparer]::OrdinalIgnoreCase)
    foreach ($entry in $Entries) { [void]$seen.Add($entry.path) }
    $extra = New-Object System.Collections.Generic.List[string]
    foreach ($scanRoot in @((Join-Path $root 'src\main\java'), (Join-Path $root 'src\main\resources'))) {
        Get-ChildItem -LiteralPath $scanRoot -Recurse -File | ForEach-Object {
            if (-not $seen.Contains($_.FullName)) { $extra.Add($_.FullName) | Out-Null }
        }
    }
    foreach ($name in @('build.gradle', 'settings.gradle', 'gradle.properties')) {
        $path = Join-Path $root $name
        if ((Test-Path -LiteralPath $path) -and -not $seen.Contains($path)) { $extra.Add($path) | Out-Null }
    }
    return $extra
}

function Find-GradleProcesses {
    return @(Get-CimInstance Win32_Process | Where-Object {
        $_.ProcessId -ne $PID -and $_.CommandLine -and ($_.CommandLine -match 'gradlew|GradleWrapper|org\.gradle')
    })
}

Write-Output ("freezeFile=" + $freezeFile.Name)
$entries = Read-FreezeEntries $freezePath
if ($entries.Count -lt 1000) { throw ("freeze parse too small: " + $entries.Count) }
$freezeMax = [datetimeoffset]::MinValue
$freezeMaxPath = ''
foreach ($entry in $entries) {
    $stored = [datetimeoffset]::Parse($entry.lastWriteTimeUtc, [Globalization.CultureInfo]::InvariantCulture, [Globalization.DateTimeStyles]::RoundtripKind)
    if ($stored -gt $freezeMax) { $freezeMax = $stored; $freezeMaxPath = $entry.path }
}
Write-Output ("freezeCount=" + $entries.Count)
Write-Output ("freezeMaxWriteUtc=" + $freezeMax.ToString('yyyy-MM-ddTHH:mm:ss.fffffffZ'))
Write-Output ("freezeMaxWritePath=" + $freezeMaxPath)

$before = Measure-Tree $entries
$extra = Find-Extras $entries
$gradleBefore = Find-GradleProcesses
$beforePath = Join-Path $evidence 'before-snapshot.json'
$before.rows | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $beforePath -Encoding UTF8
Write-Output ("beforeRows=" + $before.rows.Count)
Write-Output ("beforeMaxWriteUtc=" + $before.maxWrite.ToString('yyyy-MM-ddTHH:mm:ss.fffffffZ'))
Write-Output ("beforeMaxWritePath=" + $before.maxPath)
Write-Output ("hashMismatch=" + $before.hashMismatch.Count)
Write-Output ("timeMismatch=" + $before.timeMismatch.Count)
Write-Output ("maxTimeDeltaMs=" + $before.maxDeltaMs)
Write-Output ("missing=" + $before.missing.Count)
Write-Output ("extra=" + $extra.Count)
Write-Output ("gradleProcesses=" + $gradleBefore.Count)
if ($before.hashMismatch.Count -gt 0) { $before.hashMismatch | Select-Object -First 30 | ForEach-Object { Write-Output ("HASH " + $_) } }
if ($before.timeMismatch.Count -gt 0) { $before.timeMismatch | Select-Object -First 10 | ForEach-Object { Write-Output ("TIME " + $_) } }
if ($before.missing.Count -gt 0) { $before.missing | Select-Object -First 10 | ForEach-Object { Write-Output ("MISSING " + $_) } }
if ($extra.Count -gt 0) { $extra | Select-Object -First 20 | ForEach-Object { Write-Output ("EXTRA " + $_) } }

if ($before.hashMismatch.Count -gt 0 -or $before.missing.Count -gt 0 -or $extra.Count -gt 0 -or $before.timeMismatch.Count -gt 0 -or $gradleBefore.Count -gt 0) {
    Write-Output 'BLOCKED_BEFORE_COMPILE'
    exit 2
}

$oldJava = $env:JAVA_HOME
$oldGradleHome = $env:GRADLE_USER_HOME
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:GRADLE_USER_HOME = 'C:\Users\baishang\.gradle'
$stdoutPath = Join-Path $evidence 'compileJava.stdout.txt'
$stderrPath = Join-Path $evidence 'compileJava.stderr.txt'
if (Test-Path -LiteralPath $stdoutPath) { Remove-Item -LiteralPath $stdoutPath -Force }
if (Test-Path -LiteralPath $stderrPath) { Remove-Item -LiteralPath $stderrPath -Force }
$started = [datetimeoffset]::UtcNow
Write-Output ("startedAtUtc=" + $started.ToString('yyyy-MM-ddTHH:mm:ss.fffffffZ'))
$proc = Start-Process -FilePath $env:ComSpec -ArgumentList '/d','/c','gradlew.bat compileJava --offline --no-daemon' -WorkingDirectory $root -Wait -PassThru -NoNewWindow -RedirectStandardOutput $stdoutPath -RedirectStandardError $stderrPath
$ended = [datetimeoffset]::UtcNow
$exitCode = $proc.ExitCode
if ($null -eq $oldJava) { Remove-Item Env:JAVA_HOME -ErrorAction SilentlyContinue } else { $env:JAVA_HOME = $oldJava }
if ($null -eq $oldGradleHome) { Remove-Item Env:GRADLE_USER_HOME -ErrorAction SilentlyContinue } else { $env:GRADLE_USER_HOME = $oldGradleHome }

$after = Measure-Tree $entries
$afterPath = Join-Path $evidence 'after-snapshot.json'
$after.rows | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $afterPath -Encoding UTF8
$changed = New-Object System.Collections.Generic.List[string]
$beforeMap = @{}
foreach ($row in $before.rows) { $beforeMap[$row.path] = $row.sha256 }
foreach ($row in $after.rows) {
    if (-not $beforeMap.ContainsKey($row.path) -or $beforeMap[$row.path] -ne $row.sha256) {
        $changed.Add($row.path) | Out-Null
    }
}
$meta = [pscustomobject]@{
    command = '.\gradlew.bat compileJava --offline --no-daemon'
    workingDirectory = $root
    javaHome = 'C:\Program Files\Java\jdk-17'
    gradleUserHome = 'C:\Users\baishang\.gradle'
    startedAtUtc = $started.ToString('yyyy-MM-ddTHH:mm:ss.fffffffZ')
    endedAtUtc = $ended.ToString('yyyy-MM-ddTHH:mm:ss.fffffffZ')
    exitCode = $exitCode
    freezeCount = $entries.Count
    freezeMaxWriteUtc = $freezeMax.ToString('yyyy-MM-ddTHH:mm:ss.fffffffZ')
    freezeMaxWritePath = $freezeMaxPath
    beforeMaxWriteUtc = $before.maxWrite.ToString('yyyy-MM-ddTHH:mm:ss.fffffffZ')
    beforeMaxWritePath = $before.maxPath
    afterMaxWriteUtc = $after.maxWrite.ToString('yyyy-MM-ddTHH:mm:ss.fffffffZ')
    afterHashMismatchVsFreeze = $after.hashMismatch.Count
    afterTimeMismatchVsFreeze = $after.timeMismatch.Count
    sourceBytesChangedDuringCompile = $changed.Count
    startAfterFinalSourceWrite = ($started -gt $before.maxWrite)
}
$meta | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $evidence 'compile-meta.json') -Encoding UTF8
Write-Output ("endedAtUtc=" + $meta.endedAtUtc)
Write-Output ("exitCode=" + $exitCode)
Write-Output ("startAfterFinalSourceWrite=" + $meta.startAfterFinalSourceWrite)
Write-Output ("afterHashMismatchVsFreeze=" + $after.hashMismatch.Count)
Write-Output ("afterTimeMismatchVsFreeze=" + $after.timeMismatch.Count)
Write-Output ("sourceBytesChangedDuringCompile=" + $changed.Count)
if ($changed.Count -gt 0) { $changed | Select-Object -First 30 | ForEach-Object { Write-Output ("CHANGED " + $_) } }
Write-Output 'STDOUT_TAIL'
Get-Content -LiteralPath $stdoutPath -Tail 30
Write-Output 'STDERR_TAIL'
if (Test-Path -LiteralPath $stderrPath) { Get-Content -LiteralPath $stderrPath -Tail 40 }
Write-Output 'COMPILE_SCRIPT_DONE'
