$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$root = 'D:\alianximod'
$batch = $PSScriptRoot
$utf8 = New-Object System.Text.UTF8Encoding $false
$javaHome = 'C:\Program Files\Java\jdk-17'
$gradleHome = 'C:\Users\baishang\.gradle'
$expected = @{
  'src/main/java/com/bslx/XueXiMod/item/StaffSkills.java' = '24571ba13e7d3f44353ad4b725f2074960f0f09f0b5d22e0923ff1e69842087c'
  'src/main/java/com/bslx/XueXiMod/shu/technique/enchant/resource/ResourceEnchantSlice.java' = '7659032a12d77dd578caa9355290959d367fc8c49222e252f42235791f411552'
  'src/main/java/com/bslx/XueXiMod/shu/technique/enchant/resource/ResourceEnchantCatalog.java' = 'ed91e0dfaa79690378aaa1c77e23d88f3351a15624124d390570c590366f993a'
}

function Get-LocalStamp([datetime]$local) {
  $offset = [TimeZoneInfo]::Local.GetUtcOffset($local)
  $dto = New-Object System.DateTimeOffset $local, $offset
  return $dto.ToString('yyyy-MM-ddTHH:mm:ss.fffzzz')
}

function Get-Rel([string]$full) {
  return $full.Substring($root.Length + 1).Replace('\', '/')
}

function Get-Inventory([string]$absoluteRoot) {
  $rows = New-Object System.Collections.Generic.List[object]
  if (-not (Test-Path -LiteralPath $absoluteRoot)) {
    return $rows
  }
  $files = @(Get-ChildItem -LiteralPath $absoluteRoot -Recurse -File -Force | Sort-Object FullName)
  foreach ($item in $files) {
    if ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw ('reparse_point ' + $item.FullName) }
    $hash = (Get-FileHash -LiteralPath $item.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    [void]$rows.Add([ordered]@{
      relativePath = (Get-Rel $item.FullName)
      bytes = [int64]$item.Length
      mtime = (Get-LocalStamp $item.LastWriteTime)
      sha256 = $hash
    })
  }
  return $rows
}

function Count-Move($rows) {
  $map = @{}
  foreach ($row in $rows) { $map[$row.relativePath] = $row.sha256 + '|' + $row.bytes + '|' + $row.mtime }
  return $map
}

$versionFile = Join-Path $env:TEMP 'isolated-final-compile-java-version.txt'
Start-Process -FilePath (Join-Path $javaHome 'bin\java.exe') -ArgumentList '-version' -Wait -NoNewWindow -RedirectStandardError $versionFile | Out-Null
$javaVersion = ([System.IO.File]::ReadAllText($versionFile) -replace '\r?\n', ' | ').Trim()

$userProfile = [Environment]::GetEnvironmentVariable('USERPROFILE', 'Process')
if ([string]::IsNullOrWhiteSpace($userProfile)) { $userProfile = $env:USERPROFILE }
$tempParent = [System.IO.Path]::GetFullPath((Join-Path $userProfile 'AppData\Local\Temp'))
$envTemp = [System.IO.Path]::GetFullPath([string]$env:TEMP)
$status = New-Object System.Collections.Generic.List[string]
$status.Add('checked_at=' + (Get-LocalStamp (Get-Date)))
$status.Add('user_profile=' + $userProfile)
$status.Add('temp_parent=' + $tempParent)
$status.Add('env_TEMP=' + $envTemp)
$status.Add('java_version=' + $javaVersion)
$status.Add('JAVA_HOME_selected=' + $javaHome)
$status.Add('GRADLE_USER_HOME_selected=' + $gradleHome)
$status.Add('process_GRADLE_USER_HOME=' + [string]$env:GRADLE_USER_HOME)
if (-not (Test-Path -LiteralPath $tempParent)) { $status.Add('action=temp_parent_missing'); [System.IO.File]::WriteAllLines((Join-Path $batch 'isolated-final-compile-status.txt'), $status, $utf8); exit 2 }

$buildText = [System.IO.File]::ReadAllText((Join-Path $root 'build.gradle'))
$settingsText = [System.IO.File]::ReadAllText((Join-Path $root 'settings.gradle'))
$absoluteBuild = ($buildText -match '(?m)^\s*buildDir\s*=') -or ($buildText -match 'D:[\\/]+alianximod') -or ($settingsText -match 'D:[\\/]+alianximod')
$status.Add('absolute_build_output_or_project_path=' + $absoluteBuild)
$status.Add('uses_layout_buildDirectory=' + ($buildText -match 'layout\.buildDirectory'))
if ($absoluteBuild) {
  $status.Add('action=blocked_absolute_build_path')
  [System.IO.File]::WriteAllLines((Join-Path $batch 'isolated-final-compile-status.txt'), $status, $utf8)
  exit 2
}

$baseline = Get-Content -LiteralPath (Join-Path $batch 'recompile-after-diagnostics-src-before.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$baseMain = @{}
foreach ($row in $baseline.srcMain.files) { $baseMain[[string]$row.relativePath] = $row }
$baseGenerated = @{}
foreach ($row in $baseline.generatedResources.files) { $baseGenerated[[string]$row.relativePath] = $row }
$mainRows = Get-Inventory (Join-Path $root 'src\main')
$generatedRows = Get-Inventory (Join-Path $root 'src\generated\resources')
$diffs = New-Object System.Collections.Generic.List[string]
$nowMain = @{}
foreach ($row in $mainRows) {
  $nowMain[$row.relativePath] = $row
  if (-not $baseMain.ContainsKey($row.relativePath)) { $diffs.Add('ADDED ' + $row.relativePath); continue }
  $old = $baseMain[$row.relativePath]
  if (([string]$old.sha256).ToLowerInvariant() -ne $row.sha256 -or [int64]$old.bytes -ne [int64]$row.bytes) {
    $diffs.Add('CHANGED ' + $row.relativePath + ' now=' + $row.sha256)
  }
}
foreach ($key in @($baseMain.Keys)) { if (-not $nowMain.ContainsKey($key)) { $diffs.Add('REMOVED ' + $key) } }
$nowGenerated = @{}
foreach ($row in $generatedRows) {
  $nowGenerated[$row.relativePath] = $row
  if (-not $baseGenerated.ContainsKey($row.relativePath)) { $diffs.Add('GENERATED_ADDED ' + $row.relativePath); continue }
  $old = $baseGenerated[$row.relativePath]
  if (([string]$old.sha256).ToLowerInvariant() -ne $row.sha256 -or [int64]$old.bytes -ne [int64]$row.bytes) {
    $diffs.Add('GENERATED_CHANGED ' + $row.relativePath)
  }
}
foreach ($key in @($baseGenerated.Keys)) { if (-not $nowGenerated.ContainsKey($key)) { $diffs.Add('GENERATED_REMOVED ' + $key) } }
$handoffOk = 0
foreach ($key in @($expected.Keys)) {
  $row = $nowMain[$key]
  $ok = $row -and $row.sha256 -eq $expected[$key]
  if ($ok) { $handoffOk++ }
  $status.Add('handoff ' + $key + ' ok=' + $ok + ' sha=' + $(if ($row) { $row.sha256 } else { 'missing' }))
}
$status.Add('baseline_diffs=' + $diffs.Count)
$status.Add('handoff_ok=' + $handoffOk)
if ($diffs.Count -gt 0) { [System.IO.File]::WriteAllLines((Join-Path $batch 'isolated-final-compile-unexpected.txt'), $diffs, $utf8) }
if ($diffs.Count -gt 0 -or $handoffOk -ne 3) {
  $status.Add('action=stopped_source_not_final')
  [System.IO.File]::WriteAllLines((Join-Path $batch 'isolated-final-compile-status.txt'), $status, $utf8)
  exit 3
}

$stamp = Count-Move (@($mainRows) + @($generatedRows))
Start-Sleep -Seconds 2
$mainRows2 = Get-Inventory (Join-Path $root 'src\main')
$generatedRows2 = Get-Inventory (Join-Path $root 'src\generated\resources')
$moved = 0
foreach ($row in (@($mainRows2) + @($generatedRows2))) {
  $key = $row.sha256 + '|' + $row.bytes + '|' + $row.mtime
  if (-not $stamp.ContainsKey($row.relativePath) -or $stamp[$row.relativePath] -ne $key) {
    $moved++
    $status.Add('still_writing ' + $row.relativePath)
  }
}
$status.Add('stability_moves=' + $moved)
if ($moved -gt 0) {
  $status.Add('action=stopped_still_writing')
  [System.IO.File]::WriteAllLines((Join-Path $batch 'isolated-final-compile-status.txt'), $status, $utf8)
  exit 3
}
$mainRows = $mainRows2
$generatedRows = $generatedRows2

$suffix = (Get-Date).ToString('HHmmss') + '-' + ([guid]::NewGuid().ToString('N').Substring(0, 8))
$destName = 'xueximod-final-compile-20261031-' + $suffix
$dest = [System.IO.Path]::GetFullPath((Join-Path $tempParent $destName))
$parentPrefix = $tempParent.TrimEnd('\') + '\'
if (-not $dest.StartsWith($parentPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
  $status.Add('action=dest_not_under_temp')
  $status.Add('dest=' + $dest)
  [System.IO.File]::WriteAllLines((Join-Path $batch 'isolated-final-compile-status.txt'), $status, $utf8)
  exit 2
}
if (Test-Path -LiteralPath $dest) {
  $status.Add('action=dest_already_exists')
  $status.Add('dest=' + $dest)
  [System.IO.File]::WriteAllLines((Join-Path $batch 'isolated-final-compile-status.txt'), $status, $utf8)
  exit 2
}
New-Item -ItemType Directory -Path $dest | Out-Null
$status.Add('dest=' + $dest)
$status.Add('dest_created=' + (Get-LocalStamp (Get-Date)))

$singleFiles = @('gradlew', 'gradlew.bat', 'build.gradle', 'settings.gradle', 'gradle.properties', 'tools\check-name-gate.ps1')
foreach ($rel in $singleFiles) {
  $from = Join-Path $root $rel
  $to = Join-Path $dest $rel
  $toParent = Split-Path -Parent $to
  if (-not (Test-Path -LiteralPath $toParent)) { New-Item -ItemType Directory -Path $toParent | Out-Null }
  Copy-Item -LiteralPath $from -Destination $to
}
New-Item -ItemType Directory -Path (Join-Path $dest 'gradle\wrapper') | Out-Null
Copy-Item -LiteralPath (Join-Path $root 'gradle\wrapper\gradle-wrapper.jar') -Destination (Join-Path $dest 'gradle\wrapper\gradle-wrapper.jar')
Copy-Item -LiteralPath (Join-Path $root 'gradle\wrapper\gradle-wrapper.properties') -Destination (Join-Path $dest 'gradle\wrapper\gradle-wrapper.properties')
Copy-Item -LiteralPath (Join-Path $root 'tools\namegate') -Destination (Join-Path $dest 'tools\namegate') -Recurse
Copy-Item -LiteralPath (Join-Path $root 'src') -Destination (Join-Path $dest 'src') -Recurse
$status.Add('copy_finished=' + (Get-LocalStamp (Get-Date)))

function Get-ProofRows([string]$baseRoot) {
  $list = New-Object System.Collections.Generic.List[object]
  $roots = @(
    @{ Path = (Join-Path $baseRoot 'src'); Kind = 'src' },
    @{ Path = (Join-Path $baseRoot 'tools\namegate'); Kind = 'namegate' }
  )
  foreach ($entry in $roots) {
    if (-not (Test-Path -LiteralPath $entry.Path)) { continue }
    Get-ChildItem -LiteralPath $entry.Path -Recurse -File -Force | Sort-Object FullName | ForEach-Object {
      $rel = $_.FullName.Substring($baseRoot.Length + 1).Replace('\', '/')
      $hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
      [void]$list.Add([ordered]@{ relativePath = $rel; bytes = [int64]$_.Length; sha256 = $hash; kind = $entry.Kind })
    }
  }
  foreach ($rel in @('gradlew','gradlew.bat','build.gradle','settings.gradle','gradle.properties','gradle/wrapper/gradle-wrapper.jar','gradle/wrapper/gradle-wrapper.properties','tools/check-name-gate.ps1')) {
    $full = Join-Path $baseRoot ($rel -replace '/','\')
    $hash = (Get-FileHash -LiteralPath $full -Algorithm SHA256).Hash.ToLowerInvariant()
    [void]$list.Add([ordered]@{ relativePath = $rel; bytes = [int64](Get-Item -LiteralPath $full).Length; sha256 = $hash; kind = 'build-config' })
  }
  return $list
}

$originProof = Get-ProofRows $root
$copyProof = Get-ProofRows $dest
$originMap = @{}
foreach ($row in $originProof) { $originMap[$row.relativePath] = $row }
$copyMap = @{}
foreach ($row in $copyProof) { $copyMap[$row.relativePath] = $row }
$mismatch = 0
$manifest = New-Object System.Collections.Generic.List[object]
foreach ($key in ($originMap.Keys | Sort-Object)) {
  $left = $originMap[$key]
  $right = $copyMap[$key]
  $same = $right -and $left.sha256 -eq $right.sha256 -and [int64]$left.bytes -eq [int64]$right.bytes
  if (-not $same) { $mismatch++ }
  [void]$manifest.Add([ordered]@{
    relativePath = $key
    origin = 'D:/alianximod/' + $key
    copy = ($dest.Replace('\','/') + '/' + $key)
    kind = $left.kind
    bytes = [int64]$left.bytes
    originSha256 = $left.sha256
    copySha256 = $(if ($right) { $right.sha256 } else { '' })
    same = [bool]$same
  })
}
foreach ($key in $copyMap.Keys) {
  if (-not $originMap.ContainsKey($key)) { $mismatch++ }
}
$status.Add('proof_files=' + $originMap.Count)
$status.Add('proof_mismatch=' + $mismatch)
$proofDoc = [ordered]@{
  takenAt = (Get-LocalStamp (Get-Date))
  originRoot = 'D:/alianximod'
  copyRoot = $dest
  mismatch = $mismatch
  files = $manifest
}
[System.IO.File]::WriteAllText((Join-Path $batch 'isolated-final-compile-copy-manifest.json'), ($proofDoc | ConvertTo-Json -Depth 5), $utf8)
$originBefore = [ordered]@{
  takenAt = (Get-LocalStamp (Get-Date))
  phase = 'origin-before-copy-verified'
  srcMain = [ordered]@{ fileCount = @($mainRows).Count; files = $mainRows }
  generatedResources = [ordered]@{ fileCount = @($generatedRows).Count; files = $generatedRows }
}
[System.IO.File]::WriteAllText((Join-Path $batch 'isolated-final-compile-origin-before.json'), ($originBefore | ConvertTo-Json -Depth 5), $utf8)
if ($mismatch -ne 0) {
  $status.Add('action=copy_hash_mismatch')
  [System.IO.File]::WriteAllLines((Join-Path $batch 'isolated-final-compile-status.txt'), $status, $utf8)
  exit 4
}

$env:JAVA_HOME = $javaHome
$env:PATH = (Join-Path $javaHome 'bin') + ';' + $env:PATH
$ngPath = Join-Path $batch 'isolated-final-compile-name-gate.txt'
$ngStart = Get-Date
Push-Location $dest
& powershell -NoProfile -File (Join-Path $dest 'tools\check-name-gate.ps1') *> $ngPath
$ngCode = $LASTEXITCODE
Pop-Location
$ngEnd = Get-Date
$ngBody = [System.IO.File]::ReadAllText($ngPath)
$ngHeader = "command=powershell -NoProfile -File tools/check-name-gate.ps1`r`nproject_root=$dest`r`nJAVA_HOME=$javaHome`r`nstart=$(Get-LocalStamp $ngStart)`r`nend=$(Get-LocalStamp $ngEnd)`r`nexit=$ngCode`r`n--- output ---`r`n"
[System.IO.File]::WriteAllText($ngPath, $ngHeader + $ngBody, $utf8)
$status.Add('namegate_exit=' + $ngCode)
$status.Add('namegate_project=' + $dest)

function Get-Sentinels {
  $items = @(
    'build\tmp\compileJava\previous-compilation-data.bin',
    '.gradle\8.8\executionHistory\executionHistory.bin',
    'build\classes\java\main\com\bslx\XueXiMod\XueXiMod.class'
  )
  $lines = New-Object System.Collections.Generic.List[string]
  foreach ($rel in $items) {
    $full = Join-Path $root $rel
    if (-not (Test-Path -LiteralPath $full)) { $lines.Add('missing ' + $rel); continue }
    $item = Get-Item -LiteralPath $full
    $lines.Add((Get-LocalStamp $item.LastWriteTime) + ' bytes=' + $item.Length + ' ' + $rel)
  }
  return $lines
}
$beforeSentinels = Get-Sentinels
foreach ($line in $beforeSentinels) { $status.Add('origin_build_before ' + $line) }

$gbk = [System.Text.Encoding]::GetEncoding(936)
$start = Get-Date
$status.Add('compile_start=' + (Get-LocalStamp $start))
$status.Add('compile_cwd=' + $dest)
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = Join-Path $dest 'gradlew.bat'
$psi.Arguments = 'compileJava --offline --no-daemon'
$psi.WorkingDirectory = $dest
$psi.UseShellExecute = $false
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.StandardOutputEncoding = $gbk
$psi.StandardErrorEncoding = $gbk
$psi.EnvironmentVariables['JAVA_HOME'] = $javaHome
$psi.EnvironmentVariables['GRADLE_USER_HOME'] = $gradleHome
$psi.EnvironmentVariables['PATH'] = (Join-Path $javaHome 'bin') + ';' + $psi.EnvironmentVariables['PATH']
$proc = New-Object System.Diagnostics.Process
$proc.StartInfo = $psi
[void]$proc.Start()
$stdoutTask = $proc.StandardOutput.ReadToEndAsync()
$stderrTask = $proc.StandardError.ReadToEndAsync()
$proc.WaitForExit()
$end = Get-Date
$code = $proc.ExitCode
$status.Add('compile_end=' + (Get-LocalStamp $end))
$status.Add('compile_exit=' + $code)
$log = New-Object System.Text.StringBuilder
[void]$log.AppendLine('working_directory=' + $dest)
[void]$log.AppendLine('gradlew=' + (Join-Path $dest 'gradlew.bat'))
[void]$log.AppendLine('command=.\gradlew.bat compileJava --offline --no-daemon')
[void]$log.AppendLine('JAVA_HOME=C:\Program Files\Java\jdk-17')
[void]$log.AppendLine('GRADLE_USER_HOME=C:\Users\baishang\.gradle')
[void]$log.AppendLine('origin_project_not_used_as_cwd=D:\alianximod')
[void]$log.AppendLine('compile_start=' + (Get-LocalStamp $start))
[void]$log.AppendLine('compile_end=' + (Get-LocalStamp $end))
[void]$log.AppendLine('exit=' + $code)
[void]$log.AppendLine('--- stdout ---')
[void]$log.AppendLine($stdoutTask.Result)
[void]$log.AppendLine('--- stderr ---')
[void]$log.AppendLine($stderrTask.Result)
[System.IO.File]::WriteAllText((Join-Path $batch 'isolated-final-compile-gradle.log'), $log.ToString(), $utf8)

$afterSentinels = Get-Sentinels
foreach ($line in $afterSentinels) { $status.Add('origin_build_after ' + $line) }
$copyClasses = Join-Path $dest 'build\classes\java\main'
$status.Add('copy_classes_exist=' + (Test-Path -LiteralPath $copyClasses))
$originBuildChanged = $false
for ($index = 0; $index -lt $beforeSentinels.Count; $index++) {
  if ($beforeSentinels[$index] -ne $afterSentinels[$index]) { $originBuildChanged = $true }
}
$status.Add('origin_build_sentinel_changed=' + $originBuildChanged)

$mainAfter = Get-Inventory (Join-Path $root 'src\main')
$generatedAfter = Get-Inventory (Join-Path $root 'src\generated\resources')
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
$originSrcMoved = Count-Content $mainRows $mainAfter
$originGeneratedMoved = Count-Content $generatedRows $generatedAfter
$copyProofAfter = Get-ProofRows $dest
$copyAfterMap = @{}
foreach ($row in $copyProofAfter) { $copyAfterMap[$row.relativePath] = $row }
$copySourceDrift = 0
foreach ($key in $originMap.Keys) {
  $left = $originMap[$key]
  if ($left.kind -eq 'build-config' -or $left.kind -eq 'src' -or $left.kind -eq 'namegate') {
    $right = $copyAfterMap[$key]
    if (-not $right -or $right.sha256 -ne $left.sha256 -or [int64]$right.bytes -ne [int64]$left.bytes) { $copySourceDrift++ }
  }
}
$status.Add('origin_src_main_diffs_during=' + $originSrcMoved)
$status.Add('origin_generated_diffs_during=' + $originGeneratedMoved)
$status.Add('copy_input_diffs_after_compile=' + $copySourceDrift)
$originAfter = [ordered]@{
  takenAt = (Get-LocalStamp (Get-Date))
  phase = 'origin-after'
  srcMainDiffsDuring = $originSrcMoved
  generatedDiffsDuring = $originGeneratedMoved
  srcMain = [ordered]@{ fileCount = @($mainAfter).Count; files = $mainAfter }
  generatedResources = [ordered]@{ fileCount = @($generatedAfter).Count; files = $generatedAfter }
}
[System.IO.File]::WriteAllText((Join-Path $batch 'isolated-final-compile-origin-after.json'), ($originAfter | ConvertTo-Json -Depth 5), $utf8)
$status.Add('action=compiled')
[System.IO.File]::WriteAllLines((Join-Path $batch 'isolated-final-compile-status.txt'), $status, $utf8)
Write-Output ('dest=' + $dest)
Write-Output ('compile_exit=' + $code)
Write-Output ('proof_mismatch=' + $mismatch)
Write-Output ('origin_src_diffs=' + $originSrcMoved)
Write-Output ('copy_input_diffs=' + $copySourceDrift)
Write-Output ('namegate_exit=' + $ngCode)
Write-Output ('origin_build_sentinel_changed=' + $originBuildChanged)
exit $code
