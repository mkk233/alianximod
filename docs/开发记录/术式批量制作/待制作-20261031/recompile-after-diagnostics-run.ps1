$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$root = 'D:\alianximod'
$batch = $PSScriptRoot
$utf8 = New-Object System.Text.UTF8Encoding $false
$javaHome = 'C:\Program Files\Java\jdk-17'
$gradleHome = 'C:\Users\baishang\.gradle'
Set-Location -LiteralPath $root

function Get-LocalStamp([datetime]$local) {
  $offset = [TimeZoneInfo]::Local.GetUtcOffset($local)
  $dto = New-Object System.DateTimeOffset $local, $offset
  return $dto.ToString('yyyy-MM-ddTHH:mm:ss.fffzzz')
}

function Get-TreeInventory([string]$absoluteRoot) {
  $files = @()
  if (Test-Path -LiteralPath $absoluteRoot) {
    $files = @(Get-ChildItem -LiteralPath $absoluteRoot -Recurse -File -Force | Sort-Object FullName)
  }
  $rows = New-Object System.Collections.Generic.List[object]
  $total = [int64]0
  foreach ($item in $files) {
    $rel = $item.FullName.Substring($root.Length + 1).Replace('\', '/')
    $hash = (Get-FileHash -LiteralPath $item.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    $total += [int64]$item.Length
    [void]$rows.Add([ordered]@{
      relativePath = $rel
      bytes = [int64]$item.Length
      mtime = (Get-LocalStamp $item.LastWriteTime)
      sha256 = $hash
    })
  }
  return [pscustomobject]@{ FileCount = $rows.Count; TotalBytes = $total; Rows = $rows }
}

function Get-BlockingProcesses {
  $list = New-Object System.Collections.Generic.List[string]
  Get-CimInstance Win32_Process | ForEach-Object {
    $cmd = [string]$_.CommandLine
    $name = [string]$_.Name
    if ($name -notmatch '^(java|javaw)(\.exe)?$') { return }
    $client = ($cmd -match 'forgeclientuserdev|BootstrapLauncher') -and ($cmd -match 'alianximod')
    $wrapper = $cmd -match 'GradleWrapperMain'
    $projectGradle = ($cmd -match 'alianximod') -and ($cmd -match 'GradleWrapperMain|org\.gradle\.launcher|compileJava')
    if ($client -or $wrapper -or $projectGradle) {
      $short = $cmd
      if ($short.Length -gt 240) { $short = $short.Substring(0, 240) }
      [void]$list.Add(('pid=' + $_.ProcessId + ' parent=' + $_.ParentProcessId + ' name=' + $name + ' cmd=' + $short))
    }
  }
  return $list
}

function Count-ContentDiff($beforeRows, $afterRows) {
  $map = @{}
  foreach ($row in $beforeRows) { $map[$row.relativePath] = ([string]$row.sha256) + '|' + ([string]$row.bytes) }
  $diff = 0
  $seen = @{}
  foreach ($row in $afterRows) {
    $seen[$row.relativePath] = $true
    $key = ([string]$row.sha256) + '|' + ([string]$row.bytes)
    if (-not $map.ContainsKey($row.relativePath) -or $map[$row.relativePath] -ne $key) { $diff++ }
  }
  foreach ($row in $beforeRows) {
    if (-not $seen.ContainsKey($row.relativePath)) { $diff++ }
  }
  return $diff
}

$expected = @{
  'src/main/java/com/bslx/XueXiMod/item/StaffSkills.java' = '24571ba13e7d3f44353ad4b725f2074960f0f09f0b5d22e0923ff1e69842087c'
  'src/main/java/com/bslx/XueXiMod/shu/technique/enchant/resource/ResourceEnchantSlice.java' = '7659032a12d77dd578caa9355290959d367fc8c49222e252f42235791f411552'
  'src/main/java/com/bslx/XueXiMod/shu/technique/enchant/resource/ResourceEnchantCatalog.java' = 'ed91e0dfaa79690378aaa1c77e23d88f3351a15624124d390570c590366f993a'
}
$unchangedCheck = @{
  'src/main/java/com/bslx/XueXiMod/shu/technique/EnchantmentDefinitions.java' = '3010454bf5fc38fd93b617d41dfef4ce3a589c19e6cab71de5f7baa844866272'
  'src/main/java/com/bslx/XueXiMod/shu/technique/enchant/resource/ResourceEnchantLoad.java' = '0b44f14f6ea429a88460a8eba68bdf57a254936066187de450bb5eb4c88e3eef'
}

$versionFile = Join-Path $env:TEMP 'recompile-after-diagnostics-java-version.txt'
Start-Process -FilePath (Join-Path $javaHome 'bin\java.exe') -ArgumentList '-version' -Wait -NoNewWindow -RedirectStandardError $versionFile | Out-Null
$javaVersion = [System.IO.File]::ReadAllText($versionFile).Trim()

$daemon = Get-CimInstance Win32_Process -Filter "ProcessId=12896"
$daemonState = 'gone'
if ($daemon) {
  $kids = @(Get-CimInstance Win32_Process | Where-Object { $_.ParentProcessId -eq 12896 })
  $daemonState = 'alive_children=' + $kids.Count
}
$blocking = Get-BlockingProcesses
$status = New-Object System.Collections.Generic.List[string]
$status.Add('checked_at=' + (Get-LocalStamp (Get-Date)))
$status.Add('daemon_12896=' + $daemonState)
$status.Add('blocking_count=' + $blocking.Count)
foreach ($line in $blocking) { $status.Add('blocking ' + $line) }
$status.Add('JAVA_HOME_selected=' + $javaHome)
$status.Add('java_version=' + ($javaVersion -replace '\r?\n', ' | '))
$status.Add('GRADLE_USER_HOME_selected=' + $gradleHome)
$status.Add('process_GRADLE_USER_HOME=' + [string]$env:GRADLE_USER_HOME)
$status.Add('wrapper88=' + (Test-Path -LiteralPath (Join-Path $gradleHome 'wrapper\dists\gradle-8.8-bin')))
$status.Add('caches88=' + (Test-Path -LiteralPath (Join-Path $gradleHome 'caches\8.8')))
$props = Get-Content -LiteralPath (Join-Path $root 'gradle.properties') -Encoding UTF8
$status.Add('jvmargs=' + (($props | Where-Object { $_ -match '^org\.gradle\.jvmargs=' } | Select-Object -First 1)))
$status.Add('daemon_prop=' + (($props | Where-Object { $_ -match '^org\.gradle\.daemon=' } | Select-Object -First 1)))

$baselinePath = Join-Path $batch 'compile-after-client-close-src-after.json'
$baseline = Get-Content -LiteralPath $baselinePath -Raw -Encoding UTF8 | ConvertFrom-Json
$baseMain = @{}
foreach ($row in $baseline.srcMain.files) { $baseMain[[string]$row.relativePath] = $row }
$baseGenerated = @{}
foreach ($row in $baseline.generatedResources.files) { $baseGenerated[[string]$row.relativePath] = $row }

$main = Get-TreeInventory (Join-Path $root 'src\main')
$generated = Get-TreeInventory (Join-Path $root 'src\generated\resources')
$status.Add('src_main_files=' + $main.FileCount)
$status.Add('src_main_bytes=' + $main.TotalBytes)
$status.Add('generated_files=' + $generated.FileCount)
$status.Add('generated_bytes=' + $generated.TotalBytes)

$changeLines = New-Object System.Collections.Generic.List[string]
$unexpected = 0
$expectedHits = @{}
$nowMain = @{}
foreach ($row in $main.Rows) {
  $nowMain[$row.relativePath] = $row
  $known = $expected.ContainsKey($row.relativePath)
  if (-not $baseMain.ContainsKey($row.relativePath)) {
    $unexpected++
    $changeLines.Add('UNEXPECTED_ADDED ' + $row.sha256 + ' ' + $row.mtime + ' ' + $row.relativePath)
    continue
  }
  $old = $baseMain[$row.relativePath]
  $same = ([string]$old.sha256).ToLowerInvariant() -eq $row.sha256 -and [int64]$old.bytes -eq [int64]$row.bytes
  if ($same -and $known) {
    $unexpected++
    $changeLines.Add('EXPECTED_NOT_CHANGED current=' + $row.sha256 + ' handoff=' + $expected[$row.relativePath] + ' ' + $row.relativePath)
    continue
  }
  if ($same) { continue }
  if ($known -and $row.sha256 -eq $expected[$row.relativePath]) {
    $expectedHits[$row.relativePath] = $true
    $changeLines.Add('EXPECTED ' + $old.sha256 + ' -> ' + $row.sha256 + ' bytes ' + $old.bytes + ' -> ' + $row.bytes + ' mtime ' + $row.mtime + ' ' + $row.relativePath)
    continue
  }
  $unexpected++
  $tag = $(if ($known) { 'HANDOFF_MISMATCH' } else { 'UNEXPECTED' })
  $changeLines.Add($tag + ' baseline=' + $old.sha256 + ' now=' + $row.sha256 + ' mtime=' + $row.mtime + ' ' + $row.relativePath)
}
foreach ($key in @($baseMain.Keys)) {
  if (-not $nowMain.ContainsKey($key)) {
    $unexpected++
    $changeLines.Add('UNEXPECTED_REMOVED ' + $key)
  }
}
foreach ($key in @($expected.Keys)) {
  if (-not $expectedHits.ContainsKey($key)) {
    if (-not ($changeLines -match [regex]::Escape($key))) {
      $unexpected++
      $changeLines.Add('EXPECTED_MISSING ' + $key)
    }
  }
}
foreach ($key in @($unchangedCheck.Keys)) {
  $row = $nowMain[$key]
  $hashOk = $row -and $row.sha256 -eq $unchangedCheck[$key]
  $baseOk = $baseMain.ContainsKey($key) -and ([string]$baseMain[$key].sha256).ToLowerInvariant() -eq $row.sha256
  $status.Add('unchanged_check ' + $key + ' matches_b_record=' + $hashOk + ' matches_baseline=' + $baseOk)
  if (-not $hashOk -or -not $baseOk) { $unexpected++ }
}

$generatedUnexpected = 0
$nowGenerated = @{}
foreach ($row in $generated.Rows) {
  $nowGenerated[$row.relativePath] = $row
  if (-not $baseGenerated.ContainsKey($row.relativePath)) {
    $generatedUnexpected++
    $changeLines.Add('GENERATED_ADDED ' + $row.sha256 + ' ' + $row.relativePath)
    continue
  }
  $old = $baseGenerated[$row.relativePath]
  if (([string]$old.sha256).ToLowerInvariant() -ne $row.sha256 -or [int64]$old.bytes -ne [int64]$row.bytes) {
    $generatedUnexpected++
    $changeLines.Add('GENERATED_CHANGED baseline=' + $old.sha256 + ' now=' + $row.sha256 + ' ' + $row.relativePath)
  }
}
foreach ($key in @($baseGenerated.Keys)) {
  if (-not $nowGenerated.ContainsKey($key)) {
    $generatedUnexpected++
    $changeLines.Add('GENERATED_REMOVED ' + $key)
  }
}
$status.Add('expected_changes=' + $expectedHits.Count)
$status.Add('unexpected_src_main=' + $unexpected)
$status.Add('unexpected_generated=' + $generatedUnexpected)
[System.IO.File]::WriteAllLines((Join-Path $batch 'recompile-after-diagnostics-changeset.txt'), $changeLines, $utf8)

$beforeDoc = [ordered]@{
  takenAt = (Get-LocalStamp (Get-Date))
  phase = 'before'
  baseline = 'compile-after-client-close-src-after.json'
  expectedChangeCount = $expectedHits.Count
  unexpectedSrcMain = $unexpected
  unexpectedGenerated = $generatedUnexpected
  srcMain = [ordered]@{ fileCount = $main.FileCount; totalBytes = $main.TotalBytes; files = $main.Rows }
  generatedResources = [ordered]@{ fileCount = $generated.FileCount; totalBytes = $generated.TotalBytes; files = $generated.Rows }
}
[System.IO.File]::WriteAllText((Join-Path $batch 'recompile-after-diagnostics-src-before.json'), ($beforeDoc | ConvertTo-Json -Depth 6), $utf8)

if ($blocking.Count -gt 0) {
  $status.Add('action=stopped_busy')
  [System.IO.File]::WriteAllLines((Join-Path $batch 'recompile-after-diagnostics-status.txt'), $status, $utf8)
  exit 3
}
if ($unexpected -gt 0 -or $generatedUnexpected -gt 0 -or $expectedHits.Count -ne 3) {
  $status.Add('action=stopped_unexplained_or_incomplete_change')
  [System.IO.File]::WriteAllLines((Join-Path $batch 'recompile-after-diagnostics-status.txt'), $status, $utf8)
  exit 4
}

$stamp = @{}
foreach ($row in $main.Rows) { $stamp[$row.relativePath] = $row.sha256 + '|' + $row.bytes + '|' + $row.mtime }
Start-Sleep -Seconds 2
$mainRecheck = Get-TreeInventory (Join-Path $root 'src\main')
$moved = 0
foreach ($row in $mainRecheck.Rows) {
  $key = $row.sha256 + '|' + $row.bytes + '|' + $row.mtime
  if (-not $stamp.ContainsKey($row.relativePath) -or $stamp[$row.relativePath] -ne $key) {
    $moved++
    $status.Add('still_writing ' + $row.relativePath + ' mtime=' + $row.mtime + ' sha=' + $row.sha256)
  }
}
if ($mainRecheck.Rows.Count -ne $main.Rows.Count) { $moved++ }
$status.Add('stability_moves=' + $moved)
if ($moved -gt 0) {
  $status.Add('action=stopped_still_writing')
  [System.IO.File]::WriteAllLines((Join-Path $batch 'recompile-after-diagnostics-status.txt'), $status, $utf8)
  exit 5
}

$env:JAVA_HOME = $javaHome
$env:PATH = (Join-Path $javaHome 'bin') + ';' + $env:PATH
$ngPath = Join-Path $batch 'recompile-after-diagnostics-name-gate.txt'
$ngStart = Get-Date
& powershell -NoProfile -File (Join-Path $root 'tools\check-name-gate.ps1') *> $ngPath
$ngCode = $LASTEXITCODE
$ngEnd = Get-Date
$ngBody = [System.IO.File]::ReadAllText($ngPath)
$ngHeader = "command=powershell -NoProfile -File tools/check-name-gate.ps1`r`nJAVA_HOME=$javaHome`r`nstart=$(Get-LocalStamp $ngStart)`r`nend=$(Get-LocalStamp $ngEnd)`r`nexit=$ngCode`r`n--- output ---`r`n"
[System.IO.File]::WriteAllText($ngPath, $ngHeader + $ngBody, $utf8)
$status.Add('namegate_exit=' + $ngCode)
$status.Add('namegate_start=' + (Get-LocalStamp $ngStart))
$status.Add('namegate_end=' + (Get-LocalStamp $ngEnd))

$blockingNow = Get-BlockingProcesses
if ($blockingNow.Count -gt 0) {
  foreach ($line in $blockingNow) { $status.Add('blocking_now ' + $line) }
  $status.Add('action=stopped_busy_before_compile')
  [System.IO.File]::WriteAllLines((Join-Path $batch 'recompile-after-diagnostics-status.txt'), $status, $utf8)
  exit 3
}

$gbk = [System.Text.Encoding]::GetEncoding(936)
$start = Get-Date
$status.Add('compile_start=' + (Get-LocalStamp $start))
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = Join-Path $root 'gradlew.bat'
$psi.Arguments = 'compileJava --offline --no-daemon'
$psi.WorkingDirectory = $root
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
[void]$log.AppendLine('working_directory=D:\alianximod')
[void]$log.AppendLine('command=.\gradlew.bat compileJava --offline --no-daemon')
[void]$log.AppendLine('JAVA_HOME=C:\Program Files\Java\jdk-17')
[void]$log.AppendLine('GRADLE_USER_HOME=C:\Users\baishang\.gradle')
[void]$log.AppendLine('gradle.properties org.gradle.jvmargs=-Xmx3G')
[void]$log.AppendLine('gradle.properties org.gradle.daemon=false')
[void]$log.AppendLine('compile_start=' + (Get-LocalStamp $start))
[void]$log.AppendLine('compile_end=' + (Get-LocalStamp $end))
[void]$log.AppendLine('exit=' + $code)
[void]$log.AppendLine('--- stdout ---')
[void]$log.AppendLine($stdoutTask.Result)
[void]$log.AppendLine('--- stderr ---')
[void]$log.AppendLine($stderrTask.Result)
[System.IO.File]::WriteAllText((Join-Path $batch 'recompile-after-diagnostics-gradle.log'), $log.ToString(), $utf8)

$mainAfter = Get-TreeInventory (Join-Path $root 'src\main')
$generatedAfter = Get-TreeInventory (Join-Path $root 'src\generated\resources')
$mainDuring = Count-ContentDiff $main.Rows $mainAfter.Rows
$generatedDuring = Count-ContentDiff $generated.Rows $generatedAfter.Rows
$status.Add('src_main_content_diffs_during=' + $mainDuring)
$status.Add('generated_content_diffs_during=' + $generatedDuring)
$afterDoc = [ordered]@{
  takenAt = (Get-LocalStamp (Get-Date))
  phase = 'after'
  srcMain = [ordered]@{
    fileCount = $mainAfter.FileCount
    totalBytes = $mainAfter.TotalBytes
    contentDiffsDuringCompile = $mainDuring
    sameAsBefore = ($mainDuring -eq 0)
    files = $mainAfter.Rows
  }
  generatedResources = [ordered]@{
    fileCount = $generatedAfter.FileCount
    totalBytes = $generatedAfter.TotalBytes
    contentDiffsDuringCompile = $generatedDuring
    sameAsBefore = ($generatedDuring -eq 0)
    files = $generatedAfter.Rows
  }
}
[System.IO.File]::WriteAllText((Join-Path $batch 'recompile-after-diagnostics-src-after.json'), ($afterDoc | ConvertTo-Json -Depth 6), $utf8)
$status.Add('action=compiled')
[System.IO.File]::WriteAllLines((Join-Path $batch 'recompile-after-diagnostics-status.txt'), $status, $utf8)
Write-Output ('compile_exit=' + $code)
Write-Output ('expected=' + $expectedHits.Count)
Write-Output ('unexpected=' + $unexpected)
Write-Output ('generated_unexpected=' + $generatedUnexpected)
Write-Output ('during_main=' + $mainDuring)
Write-Output ('during_generated=' + $generatedDuring)
Write-Output ('namegate_exit=' + $ngCode)
exit $code
