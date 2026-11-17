$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$root = 'D:\alianximod'
$batch = $PSScriptRoot
$utf8 = New-Object System.Text.UTF8Encoding $false
$expectedMain = '5e3feb75da9a5de7b47551eebe6a77f99e6012cd0da3f9f6e703241eedba978a'
$javaHome = 'C:\Program Files\Java\jdk-17'
$gradleHome = 'C:\Users\baishang\.gradle'
Set-Location -LiteralPath $root

function Get-LocalStamp([datetime]$local) {
  $offset = [TimeZoneInfo]::Local.GetUtcOffset($local)
  $dto = New-Object System.DateTimeOffset $local, $offset
  return $dto.ToString('yyyy-MM-ddTHH:mm:ss.fffzzz')
}

function Get-TreeInventory([string]$absoluteRoot, [string]$label) {
  $files = @()
  if (Test-Path -LiteralPath $absoluteRoot) {
    $files = @(Get-ChildItem -LiteralPath $absoluteRoot -Recurse -File -Force | Sort-Object FullName)
  }
  $rows = New-Object System.Collections.Generic.List[object]
  $tree = New-Object System.Text.StringBuilder
  $total = [int64]0
  foreach ($item in $files) {
    $rel = $item.FullName.Substring($root.Length + 1).Replace('\', '/')
    $hash = (Get-FileHash -LiteralPath $item.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    $stamp = Get-LocalStamp $item.LastWriteTime
    $total += [int64]$item.Length
    [void]$rows.Add([ordered]@{
      relativePath = $rel
      bytes = [int64]$item.Length
      mtime = $stamp
      sha256 = $hash
    })
    [void]$tree.Append($rel).Append("`t").Append($item.Length).Append("`t").Append($hash).Append("`n")
  }
  $treeHash = [System.BitConverter]::ToString([System.Security.Cryptography.SHA256]::Create().ComputeHash($utf8.GetBytes($tree.ToString()))).Replace('-', '').ToLowerInvariant()
  return [pscustomobject]@{
    Label = $label
    Root = $absoluteRoot
    FileCount = $rows.Count
    TotalBytes = $total
    TreeSha256 = $treeHash
    Rows = $rows
  }
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

$daemon = Get-CimInstance Win32_Process -Filter "ProcessId=12896"
$daemonState = 'gone'
if ($daemon) {
  $kids = @(Get-CimInstance Win32_Process | Where-Object { $_.ParentProcessId -eq 12896 })
  $daemonState = 'alive_children=' + $kids.Count
}
$client = Get-CimInstance Win32_Process -Filter "ProcessId=45212"
$clientState = $(if ($client) { 'alive' } else { 'gone' })
$blocking = Get-BlockingProcesses

$status = New-Object System.Collections.Generic.List[string]
$status.Add('checked_at=' + (Get-LocalStamp (Get-Date)))
$status.Add('client_45212=' + $clientState)
$status.Add('daemon_12896=' + $daemonState)
$status.Add('blocking_count=' + $blocking.Count)
foreach ($line in $blocking) { $status.Add('blocking ' + $line) }
$status.Add('JAVA_HOME_selected=' + $javaHome)
$status.Add('java_exists=' + (Test-Path -LiteralPath (Join-Path $javaHome 'bin\java.exe')))
$versionFile = Join-Path $env:TEMP 'compile-after-client-close-java-version.txt'
Start-Process -FilePath (Join-Path $javaHome 'bin\java.exe') -ArgumentList '-version' -Wait -NoNewWindow -RedirectStandardError $versionFile | Out-Null
$javaVersion = [System.IO.File]::ReadAllText($versionFile).Trim()
$status.Add('java_version=' + ($javaVersion -replace '\r?\n', ' | '))
$status.Add('GRADLE_USER_HOME_selected=' + $gradleHome)
$status.Add('process_GRADLE_USER_HOME=' + [string]$env:GRADLE_USER_HOME)
$status.Add('wrapper88=' + (Test-Path -LiteralPath (Join-Path $gradleHome 'wrapper\dists\gradle-8.8-bin')))
$status.Add('caches88=' + (Test-Path -LiteralPath (Join-Path $gradleHome 'caches\8.8')))
$props = Get-Content -LiteralPath (Join-Path $root 'gradle.properties') -Encoding UTF8
$status.Add('jvmargs=' + (($props | Where-Object { $_ -match '^org\.gradle\.jvmargs=' } | Select-Object -First 1)))
$status.Add('daemon_prop=' + (($props | Where-Object { $_ -match '^org\.gradle\.daemon=' } | Select-Object -First 1)))

$main = Get-TreeInventory (Join-Path $root 'src\main') 'src/main'
$generated = Get-TreeInventory (Join-Path $root 'src\generated\resources') 'src/generated/resources'
$status.Add('src_main_files=' + $main.FileCount)
$status.Add('src_main_bytes=' + $main.TotalBytes)
$status.Add('src_main_tree=' + $main.TreeSha256)
$status.Add('src_main_matches_original=' + ($main.TreeSha256 -eq $expectedMain))
$status.Add('generated_files=' + $generated.FileCount)
$status.Add('generated_bytes=' + $generated.TotalBytes)
$status.Add('generated_tree=' + $generated.TreeSha256)

$priorPath = Join-Path $batch 'final-compile-src-before.json'
$prior = Get-Content -LiteralPath $priorPath -Raw -Encoding UTF8 | ConvertFrom-Json
$priorMap = @{}
foreach ($row in $prior.files) { $priorMap[$row.relativePath] = $row }
$diffLines = New-Object System.Collections.Generic.List[string]
$nowMap = @{}
foreach ($row in $main.Rows) {
  $nowMap[$row.relativePath] = $row
  if (-not $priorMap.ContainsKey($row.relativePath)) {
    $diffLines.Add('ADDED ' + $row.mtime + ' ' + $row.sha256 + ' ' + $row.relativePath)
    continue
  }
  $old = $priorMap[$row.relativePath]
  if ($old.sha256 -ne $row.sha256 -or [int64]$old.bytes -ne [int64]$row.bytes) {
    $diffLines.Add('CHANGED now=' + $row.mtime + ' ' + $row.sha256 + ' was=' + $old.mtime + ' ' + $old.sha256 + ' ' + $row.relativePath)
  }
}
foreach ($key in $priorMap.Keys) {
  if (-not $nowMap.ContainsKey($key)) { $diffLines.Add('REMOVED ' + $key) }
}
$status.Add('src_main_file_diffs=' + $diffLines.Count)
if ($diffLines.Count -gt 0) {
  [System.IO.File]::WriteAllLines((Join-Path $batch 'compile-after-client-close-src-diff.txt'), $diffLines, $utf8)
}
$priorTree = New-Object System.Text.StringBuilder
foreach ($row in $prior.files) {
  [void]$priorTree.Append([string]$row.relativePath).Append("`t").Append([string]$row.bytes).Append("`t").Append(([string]$row.sha256).ToLowerInvariant()).Append("`n")
}
$reproduced = [System.BitConverter]::ToString([System.Security.Cryptography.SHA256]::Create().ComputeHash($utf8.GetBytes($priorTree.ToString()))).Replace('-', '').ToLowerInvariant()
$status.Add('original_tree_reproduced=' + $reproduced)
$status.Add('content_matches_original_inventory=' + ($diffLines.Count -eq 0 -and $reproduced -eq $expectedMain))

$watch = @(
  'src/generated/resources/data/xueximod/loot_tables/blocks/meteorite.json|fa416660e970f12c51eed7b62d9501f49b1ef25f39ab6102bca8e7235b16a830',
  'src/generated/resources/data/minecraft/tags/blocks/mineable/pickaxe.json|af699604b9969fddd4271faa636d134faedc2165a346510501d037726b178d0b',
  'src/generated/resources/data/minecraft/tags/blocks/needs_stone_tool.json|1d2581e7080321aef9e703bc4be2132a99c9ed1b2229df0001bed41b60cb4a79'
)
$generatedChanged = $false
foreach ($item in $watch) {
  $parts = $item.Split('|')
  $full = Join-Path $root ($parts[0] -replace '/', '\')
  $hash = (Get-FileHash -LiteralPath $full -Algorithm SHA256).Hash.ToLowerInvariant()
  $same = $hash -eq $parts[1]
  if (-not $same) { $generatedChanged = $true }
  $status.Add('prior_json ' + $parts[0] + ' same=' + $same + ' now=' + $hash)
}
$status.Add('prior_generated_json_changed=' + $generatedChanged)

$beforeDoc = [ordered]@{
  takenAt = (Get-LocalStamp (Get-Date))
  phase = 'before'
  sourceSets = @(
    'src/main/java',
    'src/main/resources',
    'src/generated/resources'
  )
  srcMain = [ordered]@{
    fileCount = $main.FileCount
    totalBytes = $main.TotalBytes
    treeSha256 = $reproduced
    scanTreeSha256 = $main.TreeSha256
    matchesOriginal = ($diffLines.Count -eq 0 -and $reproduced -eq $expectedMain)
    files = $main.Rows
  }
  generatedResources = [ordered]@{
    fileCount = $generated.FileCount
    totalBytes = $generated.TotalBytes
    treeSha256 = $generated.TreeSha256
    files = $generated.Rows
  }
}
[System.IO.File]::WriteAllText((Join-Path $batch 'compile-after-client-close-src-before.json'), ($beforeDoc | ConvertTo-Json -Depth 6), $utf8)

if ($blocking.Count -gt 0 -or $clientState -eq 'alive') {
  $status.Add('action=stopped_busy')
  [System.IO.File]::WriteAllLines((Join-Path $batch 'compile-after-client-close-status.txt'), $status, $utf8)
  exit 3
}
if ($diffLines.Count -gt 0 -or $reproduced -ne $expectedMain) {
  $status.Add('action=stopped_source_drift')
  [System.IO.File]::WriteAllLines((Join-Path $batch 'compile-after-client-close-status.txt'), $status, $utf8)
  exit 4
}

$blockingNow = Get-BlockingProcesses
if ($blockingNow.Count -gt 0) {
  $status.Add('action=stopped_busy_immediately_before_compile')
  foreach ($line in $blockingNow) { $status.Add('blocking_now ' + $line) }
  [System.IO.File]::WriteAllLines((Join-Path $batch 'compile-after-client-close-status.txt'), $status, $utf8)
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
$stdout = $stdoutTask.Result
$stderr = $stderrTask.Result
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
[void]$log.AppendLine($stdout)
[void]$log.AppendLine('--- stderr ---')
[void]$log.AppendLine($stderr)
[System.IO.File]::WriteAllText((Join-Path $batch 'compile-after-client-close-gradle.log'), $log.ToString(), $utf8)

$mainAfter = Get-TreeInventory (Join-Path $root 'src\main') 'src/main'
$generatedAfter = Get-TreeInventory (Join-Path $root 'src\generated\resources') 'src/generated/resources'
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
$mainDuring = Count-ContentDiff $main.Rows $mainAfter.Rows
$generatedDuring = Count-ContentDiff $generated.Rows $generatedAfter.Rows
$status.Add('src_main_after_scan=' + $mainAfter.TreeSha256)
$status.Add('src_main_scan_same=' + ($mainAfter.TreeSha256 -eq $main.TreeSha256))
$status.Add('src_main_content_diffs_during=' + $mainDuring)
$status.Add('generated_after=' + $generatedAfter.TreeSha256)
$status.Add('generated_same_during_compile=' + ($generatedAfter.TreeSha256 -eq $generated.TreeSha256))
$status.Add('generated_content_diffs_during=' + $generatedDuring)
$afterDoc = [ordered]@{
  takenAt = (Get-LocalStamp (Get-Date))
  phase = 'after'
  srcMain = [ordered]@{
    fileCount = $mainAfter.FileCount
    totalBytes = $mainAfter.TotalBytes
    treeSha256 = $(if ($mainDuring -eq 0) { $reproduced } else { $mainAfter.TreeSha256 })
    scanTreeSha256 = $mainAfter.TreeSha256
    contentDiffsDuringCompile = $mainDuring
    sameAsBefore = ($mainDuring -eq 0)
    files = $mainAfter.Rows
  }
  generatedResources = [ordered]@{
    fileCount = $generatedAfter.FileCount
    totalBytes = $generatedAfter.TotalBytes
    treeSha256 = $generatedAfter.TreeSha256
    contentDiffsDuringCompile = $generatedDuring
    sameAsBefore = ($generatedDuring -eq 0)
    files = $generatedAfter.Rows
  }
}
[System.IO.File]::WriteAllText((Join-Path $batch 'compile-after-client-close-src-after.json'), ($afterDoc | ConvertTo-Json -Depth 6), $utf8)
$status.Add('action=compiled')
[System.IO.File]::WriteAllLines((Join-Path $batch 'compile-after-client-close-status.txt'), $status, $utf8)
Write-Output ('compile_exit=' + $code)
Write-Output ('src_main=' + $main.TreeSha256)
Write-Output ('src_main_after=' + $mainAfter.TreeSha256)
Write-Output ('generated=' + $generated.TreeSha256)
Write-Output ('generated_after=' + $generatedAfter.TreeSha256)
exit $code
