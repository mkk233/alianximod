$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$root = 'D:\alianximod'
$batch = $PSScriptRoot
$utf8 = New-Object System.Text.UTF8Encoding $false
$javaHome = 'C:\Program Files\Java\jdk-17'
$gradleHome = 'C:\Users\baishang\.gradle'
$expected = @{
  'src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainDefinition.java' = '510795563e269bb818542843fb1553128c335ad8ce059299a1ae55c87c430bf2'
  'src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainVisual.java' = '7390246650775c087985aa7ddd808fda5ec95c64e842fc3bb99e28c5b60b705e'
  'src/main/java/com/bslx/XueXiMod/client/DomainWorldRenderer.java' = '0ae28490d170251fb9647fdf284417086b6dbe6df9de36135086acdb53395308'
  'src/main/java/com/bslx/XueXiMod/shu/technique/release/batch/ListedRelease.java' = '9c16080d201c6b743e915bb3c490ec157d8fe12ec5d665686e60261082c65828'
  'src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseDefinitions.java' = '1786fb0983448512c43dc046e2ccafe187628b031740471b4f8a90055ae1df53'
  'src/main/java/com/bslx/XueXiMod/network/BatchReleaseCompiler.java' = '10599f046c00aa3b27d3d9c39cc22f2a23b5c1d69545600fffbbc023f776f10d'
  'src/main/java/com/bslx/XueXiMod/network/SkillUsePacket.java' = 'f5adcca9a85127a9dee8c481e5f76fbceb53775ecaa739946be9b0a99e87a7f4'
  'src/main/java/com/bslx/XueXiMod/network/DomainWave.java' = '6a3ef42e988efc54dc57ec328df18b41901bef50ad51c5bd3ecfbc439b22d1b2'
  'src/main/java/com/bslx/XueXiMod/shu/technique/release/batch/GloomDomainEffect.java' = 'd68ce2c54ea1fbc93950ae50f12d8ed2f70bd2d95fcff953d83a2aa9df9bd3ab'
  'src/main/java/com/bslx/XueXiMod/shu/ModTechniques.java' = '34d8252774bc8b27114fdad9712ef41c961ae1c5bdd03cdfb46280a74a19868c'
}
$configRels = @(
  'gradlew',
  'gradlew.bat',
  'build.gradle',
  'settings.gradle',
  'gradle.properties',
  'gradle/wrapper/gradle-wrapper.jar',
  'gradle/wrapper/gradle-wrapper.properties'
)

function Get-LocalStamp([datetime]$local) {
  $offset = [TimeZoneInfo]::Local.GetUtcOffset($local)
  $dto = New-Object System.DateTimeOffset $local, $offset
  return $dto.ToString('yyyy-MM-ddTHH:mm:ss.fffzzz')
}

function Write-Status([System.Collections.Generic.List[string]]$lines) {
  [System.IO.File]::WriteAllLines((Join-Path $batch 'gloom-standalone-compile-status.txt'), $lines, $utf8)
}

function Get-TreeRows([string]$baseRoot, [string]$absoluteRoot) {
  $rows = New-Object System.Collections.Generic.List[object]
  if (-not (Test-Path -LiteralPath $absoluteRoot)) { return $rows }
  $prefix = $baseRoot.TrimEnd('\') + '\'
  $files = @(Get-ChildItem -LiteralPath $absoluteRoot -Recurse -File -Force | Sort-Object FullName)
  foreach ($item in $files) {
    if ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw ('reparse_point ' + $item.FullName) }
    if (-not $item.FullName.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) { throw ('outside ' + $item.FullName) }
    $rel = $item.FullName.Substring($prefix.Length).Replace('\', '/')
    $hash = (Get-FileHash -LiteralPath $item.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    [void]$rows.Add([ordered]@{
      relativePath = $rel
      bytes = [int64]$item.Length
      mtime = (Get-LocalStamp $item.LastWriteTime)
      sha256 = $hash
    })
  }
  return $rows
}

function Get-ConfigRows([string]$baseRoot) {
  $rows = New-Object System.Collections.Generic.List[object]
  foreach ($rel in $configRels) {
    $full = Join-Path $baseRoot ($rel -replace '/', '\')
    if (-not (Test-Path -LiteralPath $full)) { throw ('missing_config ' + $full) }
    $item = Get-Item -LiteralPath $full
    $hash = (Get-FileHash -LiteralPath $full -Algorithm SHA256).Hash.ToLowerInvariant()
    [void]$rows.Add([ordered]@{
      relativePath = $rel
      bytes = [int64]$item.Length
      mtime = (Get-LocalStamp $item.LastWriteTime)
      sha256 = $hash
    })
  }
  return $rows
}

function Get-StampMap($rows) {
  $map = @{}
  foreach ($row in $rows) { $map[$row.relativePath] = $row.sha256 + '|' + $row.bytes + '|' + $row.mtime }
  return $map
}

function Count-Content($beforeRows, $afterRows) {
  $map = @{}
  foreach ($row in $beforeRows) { $map[$row.relativePath] = $row.sha256 + '|' + $row.bytes }
  $diff = 0
  $seen = @{}
  foreach ($row in $afterRows) {
    $seen[$row.relativePath] = $true
    $key = $row.sha256 + '|' + $row.bytes
    if (-not $map.ContainsKey($row.relativePath) -or $map[$row.relativePath] -ne $key) { $diff++ }
  }
  foreach ($row in $beforeRows) { if (-not $seen.ContainsKey($row.relativePath)) { $diff++ } }
  return $diff
}

function Sum-Bytes($rows) {
  $total = [int64]0
  foreach ($row in @($rows)) { $total += [int64]$row.bytes }
  return $total
}

function Save-Inventory($path, $phase, $mainRows, $generatedRows, $configRows) {
  $doc = [ordered]@{
    takenAt = (Get-LocalStamp (Get-Date))
    phase = $phase
    projectRoot = 'D:/alianximod'
    srcMain = [ordered]@{ fileCount = @($mainRows).Count; bytes = (Sum-Bytes $mainRows); files = @($mainRows) }
    generatedResources = [ordered]@{ fileCount = @($generatedRows).Count; bytes = (Sum-Bytes $generatedRows); files = @($generatedRows) }
    buildConfig = [ordered]@{ fileCount = @($configRows).Count; files = @($configRows) }
  }
  [System.IO.File]::WriteAllText($path, ($doc | ConvertTo-Json -Depth 6), $utf8)
}

$versionFile = Join-Path $env:TEMP 'gloom-standalone-compile-java-version.txt'
Start-Process -FilePath (Join-Path $javaHome 'bin\java.exe') -ArgumentList '-version' -Wait -NoNewWindow -RedirectStandardError $versionFile | Out-Null
$javaVersion = ([System.IO.File]::ReadAllText($versionFile) -replace '\r?\n', ' | ').Trim()
$userProfile = [Environment]::GetEnvironmentVariable('USERPROFILE', 'Process')
if ([string]::IsNullOrWhiteSpace($userProfile)) { $userProfile = $env:USERPROFILE }
$tempParent = [System.IO.Path]::GetFullPath((Join-Path $userProfile 'AppData\Local\Temp'))
$status = New-Object System.Collections.Generic.List[string]
$status.Add('checked_at=' + (Get-LocalStamp (Get-Date)))
$status.Add('java_version=' + $javaVersion)
$status.Add('JAVA_HOME_selected=' + $javaHome)
$status.Add('GRADLE_USER_HOME_selected=' + $gradleHome)
$status.Add('process_GRADLE_USER_HOME=' + [string]$env:GRADLE_USER_HOME)
$status.Add('user_temp_parent=' + $tempParent)
$status.Add('env_TEMP=' + [string]$env:TEMP)
$propLines = @(Get-Content -LiteralPath (Join-Path $root 'gradle.properties') | Where-Object { $_ -match '^\s*org\.gradle\.(jvmargs|daemon)\s*=' })
foreach ($propLine in $propLines) { $status.Add('gradle_property=' + $propLine) }
$buildText = [System.IO.File]::ReadAllText((Join-Path $root 'build.gradle'))
$settingsText = [System.IO.File]::ReadAllText((Join-Path $root 'settings.gradle'))
$absoluteBuild = ($buildText -match '(?m)^\s*buildDir\s*=') -or ($buildText -match 'D:[\\/]+alianximod') -or ($settingsText -match 'D:[\\/]+alianximod')
$status.Add('absolute_build_output_or_project_path=' + $absoluteBuild)
$status.Add('uses_layout_buildDirectory=' + ($buildText -match 'layout\.buildDirectory'))
$activeLib = $false
foreach ($line in [System.IO.File]::ReadAllLines((Join-Path $root 'build.gradle'))) {
  $trim = $line.Trim()
  if ($trim.StartsWith('//') -or $trim.StartsWith('*') -or $trim.StartsWith('/*')) { continue }
  if ($trim -match 'flatDir|files\s*\(|\blibs/') { $activeLib = $true }
}
$status.Add('uncommented_local_lib_reference=' + $activeLib)

$mainRows = Get-TreeRows $root (Join-Path $root 'src\main')
$generatedRows = Get-TreeRows $root (Join-Path $root 'src\generated\resources')
$configRows = Get-ConfigRows $root
$mainMap = @{}
foreach ($row in $mainRows) { $mainMap[$row.relativePath] = $row }
$handoffOk = 0
$handoffMiss = New-Object System.Collections.Generic.List[string]
foreach ($key in ($expected.Keys | Sort-Object)) {
  $row = $mainMap[$key]
  $ok = $row -and $row.sha256 -eq $expected[$key]
  if ($ok) { $handoffOk++ } else { $handoffMiss.Add($key + ' disk=' + $(if ($row) { $row.sha256 } else { 'missing' }) + ' expected=' + $expected[$key]) }
  $mtime = $(if ($row) { $row.mtime } else { 'missing' })
  $status.Add('handoff ' + $key + ' ok=' + $ok + ' mtime=' + $mtime + ' sha=' + $(if ($row) { $row.sha256 } else { 'missing' }))
}
$status.Add('handoff_ok=' + $handoffOk + '/10')
if ($handoffOk -ne 10) {
  [System.IO.File]::WriteAllLines((Join-Path $batch 'gloom-standalone-compile-handoff-mismatch.txt'), $handoffMiss, $utf8)
  Save-Inventory (Join-Path $batch 'gloom-standalone-compile-src-before.json') 'observed-handoff-mismatch' $mainRows $generatedRows $configRows
  $status.Add('action=stopped_handoff_mismatch')
  Write-Status $status
  Write-Output 'action=stopped_handoff_mismatch'
  exit 3
}

$stamp = Get-StampMap (@($mainRows) + @($generatedRows) + @($configRows))
Start-Sleep -Seconds 2
$mainRows2 = Get-TreeRows $root (Join-Path $root 'src\main')
$generatedRows2 = Get-TreeRows $root (Join-Path $root 'src\generated\resources')
$configRows2 = Get-ConfigRows $root
$moved = New-Object System.Collections.Generic.List[string]
foreach ($row in (@($mainRows2) + @($generatedRows2) + @($configRows2))) {
  $key = $row.sha256 + '|' + $row.bytes + '|' + $row.mtime
  if (-not $stamp.ContainsKey($row.relativePath) -or $stamp[$row.relativePath] -ne $key) { $moved.Add($row.relativePath + ' now=' + $row.sha256 + ' mtime=' + $row.mtime) }
}
foreach ($key in @($stamp.Keys)) {
  $still = $false
  foreach ($row in (@($mainRows2) + @($generatedRows2) + @($configRows2))) { if ($row.relativePath -eq $key) { $still = $true; break } }
  if (-not $still) { $moved.Add('removed ' + $key) }
}
$status.Add('stability_moves=' + $moved.Count)
if ($moved.Count -gt 0) {
  [System.IO.File]::WriteAllLines((Join-Path $batch 'gloom-standalone-compile-still-writing.txt'), $moved, $utf8)
  $status.Add('action=stopped_still_writing')
  Write-Status $status
  Write-Output 'action=stopped_still_writing'
  exit 3
}
$mainRows = $mainRows2
$generatedRows = $generatedRows2
$configRows = $configRows2
Save-Inventory (Join-Path $batch 'gloom-standalone-compile-src-before.json') 'stable-before-compile' $mainRows $generatedRows $configRows

$procs = @(Get-CimInstance Win32_Process)
$childCount = @{}
foreach ($proc in $procs) {
  $parentId = [int]$proc.ParentProcessId
  if (-not $childCount.ContainsKey($parentId)) { $childCount[$parentId] = 0 }
  $childCount[$parentId] = $childCount[$parentId] + 1
}
$processLines = New-Object System.Collections.Generic.List[string]
$clientCount = 0
$runClientWrapperCount = 0
$otherWrapperCount = 0
$idleDaemonCount = 0
foreach ($proc in $procs) {
  $name = [string]$proc.Name
  if ($name -notmatch '^(java|javaw)(\.exe)?$') { continue }
  $cmd = [string]$proc.CommandLine
  $pidText = [string]$proc.ProcessId
  $kids = 0
  if ($childCount.ContainsKey([int]$proc.ProcessId)) { $kids = $childCount[[int]$proc.ProcessId] }
  $isClient = ($cmd -match 'forgeclientuserdev|BootstrapLauncher') -and ($cmd -match 'alianximod')
  $isWrapper = ($cmd -match 'GradleWrapperMain') -and ($cmd -match 'alianximod')
  $isRunClient = $isWrapper -and ($cmd -match 'runClient')
  $isDaemon = $cmd -match 'GradleDaemon'
  $idleDaemon = $isDaemon -and $kids -eq 0
  if ($isClient) { $clientCount++ }
  if ($isRunClient) { $runClientWrapperCount++ }
  if ($isWrapper -and -not $isRunClient) { $otherWrapperCount++ }
  if ($idleDaemon) { $idleDaemonCount++ }
  if ($isClient -or $isWrapper -or $isDaemon) {
    $snippet = $cmd
    if ($snippet.Length -gt 280) { $snippet = $snippet.Substring(0, 280) }
    $processLines.Add('pid=' + $pidText + ' parent=' + $proc.ParentProcessId + ' children=' + $kids + ' client=' + $isClient + ' wrapper=' + $isWrapper + ' runClient=' + $isRunClient + ' idleDaemon=' + $idleDaemon + ' cmd=' + $snippet)
  }
}
$status.Add('client_count=' + $clientCount)
$status.Add('runclient_wrapper_count=' + $runClientWrapperCount)
$status.Add('other_project_wrapper_count=' + $otherWrapperCount)
$status.Add('idle_daemon_count=' + $idleDaemonCount)
[System.IO.File]::WriteAllLines((Join-Path $batch 'gloom-standalone-compile-processes.txt'), $processLines, $utf8)
if ($otherWrapperCount -gt 0 -and $clientCount -eq 0 -and $runClientWrapperCount -eq 0) {
  $status.Add('action=stopped_active_gradle_task')
  Write-Status $status
  Write-Output 'action=stopped_active_gradle_task'
  exit 5
}

$mode = 'inplace'
$compileRoot = $root
if ($clientCount -gt 0 -or $runClientWrapperCount -gt 0) {
  if ($absoluteBuild) {
    $status.Add('action=blocked_absolute_build_path')
    Write-Status $status
    Write-Output 'action=blocked_absolute_build_path'
    exit 2
  }
  if (-not (Test-Path -LiteralPath $tempParent)) {
    $status.Add('action=temp_parent_missing')
    Write-Status $status
    exit 2
  }
  if ($tempParent -match 'cursor-sandbox-cache') {
    $status.Add('action=temp_parent_is_sandbox')
    Write-Status $status
    exit 2
  }
  $suffix = (Get-Date).ToString('HHmmss') + '-' + ([guid]::NewGuid().ToString('N').Substring(0, 8))
  $destName = 'xueximod-gloom-compile-20261031-' + $suffix
  $dest = [System.IO.Path]::GetFullPath((Join-Path $tempParent $destName))
  $parentPrefix = $tempParent.TrimEnd('\') + '\'
  if (-not $dest.StartsWith($parentPrefix, [System.StringComparison]::OrdinalIgnoreCase) -or $dest -match 'cursor-sandbox-cache') {
    $status.Add('action=dest_not_under_user_temp')
    $status.Add('dest=' + $dest)
    Write-Status $status
    exit 2
  }
  if (Test-Path -LiteralPath $dest) {
    $status.Add('action=dest_already_exists')
    $status.Add('dest=' + $dest)
    Write-Status $status
    exit 2
  }
  New-Item -ItemType Directory -Path $dest | Out-Null
  $status.Add('dest=' + $dest)
  $status.Add('dest_existed_before_create=False')
  foreach ($rel in @('gradlew', 'gradlew.bat', 'build.gradle', 'settings.gradle', 'gradle.properties')) {
    Copy-Item -LiteralPath (Join-Path $root $rel) -Destination (Join-Path $dest $rel)
  }
  New-Item -ItemType Directory -Path (Join-Path $dest 'gradle\wrapper') | Out-Null
  Copy-Item -LiteralPath (Join-Path $root 'gradle\wrapper\gradle-wrapper.jar') -Destination (Join-Path $dest 'gradle\wrapper\gradle-wrapper.jar')
  Copy-Item -LiteralPath (Join-Path $root 'gradle\wrapper\gradle-wrapper.properties') -Destination (Join-Path $dest 'gradle\wrapper\gradle-wrapper.properties')
  Copy-Item -LiteralPath (Join-Path $root 'src') -Destination (Join-Path $dest 'src') -Recurse
  $originProof = @{}
  foreach ($row in (@($mainRows) + @($generatedRows) + @($configRows))) { $originProof[$row.relativePath] = $row }
  $copyMain = Get-TreeRows $dest (Join-Path $dest 'src\main')
  $copyGenerated = Get-TreeRows $dest (Join-Path $dest 'src\generated\resources')
  $copyConfig = Get-ConfigRows $dest
  $mismatch = 0
  $manifest = New-Object System.Collections.Generic.List[object]
  foreach ($row in (@($copyMain) + @($copyGenerated) + @($copyConfig))) {
    $left = $originProof[$row.relativePath]
    $same = $left -and $left.sha256 -eq $row.sha256 -and [int64]$left.bytes -eq [int64]$row.bytes
    if (-not $same) { $mismatch++ }
    [void]$manifest.Add([ordered]@{ relativePath = $row.relativePath; bytes = [int64]$row.bytes; originSha256 = $(if ($left) { $left.sha256 } else { '' }); copySha256 = $row.sha256; same = [bool]$same })
  }
  $copyCount = @($copyMain).Count + @($copyGenerated).Count + @($copyConfig).Count
  if ($copyCount -ne $originProof.Count) { $mismatch += [Math]::Abs($copyCount - $originProof.Count) }
  $status.Add('proof_files=' + $originProof.Count)
  $status.Add('proof_mismatch=' + $mismatch)
  Write-Status $status
  $fileJson = ConvertTo-Json -InputObject $manifest.ToArray() -Depth 4
  $proofText = '{"takenAt":"' + (Get-LocalStamp (Get-Date)) + '","originRoot":"D:/alianximod","copyRoot":"' + ($dest.Replace('\', '/')) + '","mismatch":' + $mismatch + ',"files":' + $fileJson + '}'
  [System.IO.File]::WriteAllText((Join-Path $batch 'gloom-standalone-compile-copy-manifest.json'), $proofText, $utf8)
  if ($mismatch -ne 0) {
    $status.Add('action=copy_hash_mismatch')
    Write-Status $status
    Write-Output 'action=copy_hash_mismatch'
    exit 4
  }
  $mode = 'isolate'
  $compileRoot = $dest
}
$status.Add('compile_mode=' + $mode)
$status.Add('compile_root=' + $compileRoot)

$gbk = [System.Text.Encoding]::GetEncoding(936)
$start = Get-Date
$status.Add('compile_start=' + (Get-LocalStamp $start))
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = Join-Path $compileRoot 'gradlew.bat'
$psi.Arguments = 'compileJava --offline --no-daemon'
$psi.WorkingDirectory = $compileRoot
$psi.UseShellExecute = $false
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.StandardOutputEncoding = $gbk
$psi.StandardErrorEncoding = $gbk
$psi.EnvironmentVariables['JAVA_HOME'] = $javaHome
$psi.EnvironmentVariables['GRADLE_USER_HOME'] = $gradleHome
$psi.EnvironmentVariables['PATH'] = (Join-Path $javaHome 'bin') + ';' + $psi.EnvironmentVariables['PATH']
$compileProc = New-Object System.Diagnostics.Process
$compileProc.StartInfo = $psi
[void]$compileProc.Start()
$stdoutTask = $compileProc.StandardOutput.ReadToEndAsync()
$stderrTask = $compileProc.StandardError.ReadToEndAsync()
$compileProc.WaitForExit()
$end = Get-Date
$code = $compileProc.ExitCode
$status.Add('compile_end=' + (Get-LocalStamp $end))
$status.Add('compile_exit=' + $code)
$log = New-Object System.Text.StringBuilder
[void]$log.AppendLine('working_directory=' + $compileRoot)
[void]$log.AppendLine('gradlew=' + (Join-Path $compileRoot 'gradlew.bat'))
[void]$log.AppendLine('command=.\gradlew.bat compileJava --offline --no-daemon')
[void]$log.AppendLine('compile_mode=' + $mode)
[void]$log.AppendLine('JAVA_HOME=C:\Program Files\Java\jdk-17')
[void]$log.AppendLine('GRADLE_USER_HOME=C:\Users\baishang\.gradle')
[void]$log.AppendLine('compile_start=' + (Get-LocalStamp $start))
[void]$log.AppendLine('compile_end=' + (Get-LocalStamp $end))
[void]$log.AppendLine('exit=' + $code)
[void]$log.AppendLine('--- stdout ---')
[void]$log.AppendLine($stdoutTask.Result)
[void]$log.AppendLine('--- stderr ---')
[void]$log.AppendLine($stderrTask.Result)
[System.IO.File]::WriteAllText((Join-Path $batch 'gloom-standalone-compile-gradle.log'), $log.ToString(), $utf8)

$mainAfter = Get-TreeRows $root (Join-Path $root 'src\main')
$generatedAfter = Get-TreeRows $root (Join-Path $root 'src\generated\resources')
$configAfter = Get-ConfigRows $root
$mainDiff = Count-Content $mainRows $mainAfter
$generatedDiff = Count-Content $generatedRows $generatedAfter
$configDiff = Count-Content $configRows $configAfter
$status.Add('origin_src_main_diffs_during=' + $mainDiff)
$status.Add('origin_generated_diffs_during=' + $generatedDiff)
$status.Add('origin_config_diffs_during=' + $configDiff)
if ($mode -eq 'isolate') {
  $copyMainAfter = Get-TreeRows $compileRoot (Join-Path $compileRoot 'src\main')
  $copyGeneratedAfter = Get-TreeRows $compileRoot (Join-Path $compileRoot 'src\generated\resources')
  $copyConfigAfter = Get-ConfigRows $compileRoot
  $copyDiff = (Count-Content $mainRows $copyMainAfter) + (Count-Content $generatedRows $copyGeneratedAfter) + (Count-Content $configRows $copyConfigAfter)
  $status.Add('copy_input_diffs_after=' + $copyDiff)
  $status.Add('origin_build_not_used_as_compile_output=True')
} else {
  $status.Add('copy_input_diffs_after=not_isolated')
}
Save-Inventory (Join-Path $batch 'gloom-standalone-compile-src-after.json') 'after-compile' $mainAfter $generatedAfter $configAfter
$inputStable = ($mainDiff -eq 0 -and $generatedDiff -eq 0 -and $configDiff -eq 0)
if ($mode -eq 'isolate' -and $copyDiff -ne 0) { $inputStable = $false }
$status.Add('input_stable=' + $inputStable)
$status.Add('namegate_rerun=false')
$status.Add('namegate_cited=D report exit 1, OriginalHumanoid.java:165 i and :202 to only')
if ($code -eq 0 -and $inputStable) { $status.Add('action=compile_succeeded') }
elseif ($code -eq 0) { $status.Add('action=compile_exit_0_input_moved') }
else { $status.Add('action=compile_failed') }
Write-Status $status
Write-Output ('mode=' + $mode)
Write-Output ('compile_root=' + $compileRoot)
Write-Output ('compile_exit=' + $code)
Write-Output ('input_stable=' + $inputStable)
Write-Output ('origin_src_diffs=' + $mainDiff)
Write-Output ('origin_generated_diffs=' + $generatedDiff)
Write-Output ('origin_config_diffs=' + $configDiff)
exit $code
