$ErrorActionPreference = 'Continue'
$root = 'D:\alianximod'
$ev = Join-Path $root 'docs\开发记录\领域绑定规则-20261030\compile-evidence'
New-Item -ItemType Directory -Force -Path $ev | Out-Null
Set-Location -LiteralPath $root
$utf8 = New-Object System.Text.UTF8Encoding $false
$gbk = [System.Text.Encoding]::GetEncoding(936)

function Write-Utf8Text([string]$path, [string]$text) {
    [System.IO.File]::WriteAllText($path, $text, $utf8)
}

function Write-Utf8Lines([string]$path, $lines) {
    [System.IO.File]::WriteAllLines($path, [string[]]$lines, $utf8)
}

function Get-FileSha256([string]$path) {
    $sha = [System.Security.Cryptography.SHA256]::Create()
    $stream = [System.IO.File]::Open($path, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::ReadWrite)
    try {
        $hash = $sha.ComputeHash($stream)
        return ([System.BitConverter]::ToString($hash)).Replace('-', '').ToLowerInvariant()
    } finally {
        $stream.Dispose()
        $sha.Dispose()
    }
}

function Get-TextSha256([string]$text) {
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $hash = $sha.ComputeHash($utf8.GetBytes($text))
        return ([System.BitConverter]::ToString($hash)).Replace('-', '').ToLowerInvariant()
    } finally {
        $sha.Dispose()
    }
}

function Read-LogText([string]$path) {
    if (-not (Test-Path -LiteralPath $path)) { return '' }
    $bytes = [System.IO.File]::ReadAllBytes($path)
    if ($bytes.Length -eq 0) { return '' }
    return $gbk.GetString($bytes)
}

function Get-SourceRows {
    $javaRoot = Join-Path $root 'src\main\java'
    $files = @(Get-ChildItem -LiteralPath $javaRoot -Recurse -File | Sort-Object FullName)
    $rows = New-Object System.Collections.Generic.List[object]
    foreach ($file in $files) {
        $rel = $file.FullName.Substring($root.Length).TrimStart('\').Replace('\', '/')
        $rows.Add([pscustomobject]@{
            Path = $rel
            Length = $file.Length
            MtimeUtc = $file.LastWriteTimeUtc.ToString('o')
            Sha256 = (Get-FileSha256 $file.FullName)
        })
    }
    return $rows
}

function Format-SourceLines($rows, [string]$header) {
    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add($header)
    $javaCount = @($rows | Where-Object { $_.Path -like '*.java' }).Count
    $lines.Add('file_count=' + @($rows).Count)
    $lines.Add('java_count=' + $javaCount)
    foreach ($row in $rows) {
        $lines.Add($row.Sha256 + "`t" + $row.Length + "`t" + $row.MtimeUtc + "`t" + $row.Path)
    }
    return $lines
}

function Get-TreeIdentity($rows) {
    $body = New-Object System.Collections.Generic.List[string]
    foreach ($row in $rows) {
        $body.Add($row.Sha256 + "`t" + $row.Path)
    }
    $text = [string]::Join("`n", $body)
    return Get-TextSha256 $text
}

$realJavaHome = 'C:\Program Files\Java\jdk-17'
$realGradleHome = 'C:\Users\baishang\.gradle'
$inheritedJavaHome = [string]$env:JAVA_HOME
$inheritedGradleHome = [string]$env:GRADLE_USER_HOME
$javaOnPath = Get-Command java.exe -ErrorAction SilentlyContinue
$javaOnPathText = ''
if ($javaOnPath) { $javaOnPathText = $javaOnPath.Source }

$wrappers = @()
$cimError = ''
try {
    $wrappers = @(Get-CimInstance Win32_Process -ErrorAction Stop | Where-Object {
        $_.Name -match '^(java|javaw)(\.exe)?$' -and [string]$_.CommandLine -match 'GradleWrapperMain'
    })
} catch {
    $cimError = $_.Exception.Message
}

$sandboxDist = $false
$sandboxCaches = $false
$sandboxForge = $false
$realDist = Test-Path -LiteralPath (Join-Path $realGradleHome 'wrapper\dists\gradle-8.8-bin')
$realCaches = Test-Path -LiteralPath (Join-Path $realGradleHome 'caches')
$realForge = Test-Path -LiteralPath (Join-Path $realGradleHome 'caches\modules-2\files-2.1\net.minecraftforge.gradle')
if ($inheritedGradleHome) {
    $sandboxDist = Test-Path -LiteralPath (Join-Path $inheritedGradleHome 'wrapper\dists\gradle-8.8-bin')
    $sandboxCaches = Test-Path -LiteralPath (Join-Path $inheritedGradleHome 'caches')
    $sandboxForge = Test-Path -LiteralPath (Join-Path $inheritedGradleHome 'caches\modules-2\files-2.1\net.minecraftforge.gradle')
}

$preRows = @(Get-SourceRows)
$preWhen = (Get-Date).ToString('o')
$preLines = Format-SourceLines $preRows ("pre " + $preWhen)
$preTree = Get-TreeIdentity $preRows
$preLines.Insert(3, 'tree_sha256=' + $preTree)
Write-Utf8Lines (Join-Path $ev 'pre-sources.txt') $preLines

$batch = Join-Path $root 'docs\开发记录\领域绑定规则-20261030'
$evidencePaths = New-Object System.Collections.Generic.List[string]
Get-ChildItem -LiteralPath $batch -Filter 'evidence*.json' -File | ForEach-Object {
    $raw = [System.IO.File]::ReadAllText($_.FullName, $utf8)
    $found = [regex]::Matches($raw, 'src/main/java/[A-Za-z0-9_./-]+\.java')
    foreach ($match in $found) {
        $value = $match.Value
        if (-not $evidencePaths.Contains($value)) { $evidencePaths.Add($value) }
    }
}
$shortNames = @('GroupBudget.java', 'ReleaseExecutionContext.java', 'ReleaseBatch.java')
foreach ($short in $shortNames) {
    $hits = @($preRows | Where-Object { $_.Path.EndsWith('/' + $short) })
    foreach ($hit in $hits) {
        if (-not $evidencePaths.Contains($hit.Path)) { $evidencePaths.Add($hit.Path) }
    }
}
$evidenceLines = New-Object System.Collections.Generic.List[string]
$evidenceLines.Add('evidence-paths ' + $preWhen)
$evidenceRecords = New-Object System.Collections.Generic.List[object]
foreach ($rel in $evidencePaths) {
    $row = $preRows | Where-Object { $_.Path -eq $rel } | Select-Object -First 1
    if ($row) {
        $evidenceLines.Add('PRESENT' + "`t" + $row.Sha256 + "`t" + $row.Length + "`t" + $rel)
        $evidenceRecords.Add([pscustomobject]@{ path = $rel; present = $true; sha256 = $row.Sha256; length = $row.Length })
    } else {
        $evidenceLines.Add('MISSING' + "`t" + $rel)
        $evidenceRecords.Add([pscustomobject]@{ path = $rel; present = $false; sha256 = $null; length = $null })
    }
}
Write-Utf8Lines (Join-Path $ev 'evidence-paths.txt') $evidenceLines

function Invoke-GradleAttempt([int]$index) {
    $stdoutPath = Join-Path $ev ("attempt-$index-stdout.log")
    $stderrPath = Join-Path $ev ("attempt-$index-stderr.log")
    $start = Get-Date
    $meta = New-Object System.Collections.Generic.List[string]
    $meta.Add('index=' + $index)
    $meta.Add('launched=true')
    $meta.Add('start=' + $start.ToString('o'))
    $meta.Add('cwd=' + $root)
    $meta.Add('command=.\gradlew.bat compileJava --offline --no-daemon')
    $meta.Add('JAVA_HOME=' + [string]$env:JAVA_HOME)
    $meta.Add('GRADLE_USER_HOME=' + [string]$env:GRADLE_USER_HOME)
    $meta.Add('java_on_path=' + $javaOnPathText)
    Write-Utf8Lines (Join-Path $ev ("attempt-$index-meta.txt")) $meta
    $proc = Start-Process -FilePath $env:ComSpec -ArgumentList @('/d', '/c', 'gradlew.bat compileJava --offline --no-daemon') -WorkingDirectory $root -Wait -PassThru -RedirectStandardOutput $stdoutPath -RedirectStandardError $stderrPath
    $end = Get-Date
    $code = $proc.ExitCode
    $stdout = Read-LogText $stdoutPath
    $stderr = Read-LogText $stderrPath
    $combined = $stdout + "`n" + $stderr
    $upToDate = $combined -match 'compileJava UP-TO-DATE'
    $taskRan = ($combined -match '(?m)^> Task :compileJava\s*$') -or ($combined -match ':compileJava\s*$')
    $buildOk = $combined -match 'BUILD SUCCESSFUL'
    $buildFail = $combined -match 'BUILD FAILED'
    $meta.Add('end=' + $end.ToString('o'))
    $meta.Add('gradle_exit=' + $code)
    $meta.Add('build_successful=' + $buildOk)
    $meta.Add('build_failed=' + $buildFail)
    $meta.Add('compileJava_up_to_date=' + $upToDate)
    $meta.Add('compileJava_task_line=' + $taskRan)
    Write-Utf8Lines (Join-Path $ev ("attempt-$index-meta.txt")) $meta
    $log = New-Object System.Text.StringBuilder
    [void]$log.AppendLine('index=' + $index)
    [void]$log.AppendLine('command=.\gradlew.bat compileJava --offline --no-daemon')
    [void]$log.AppendLine('cwd=D:\alianximod')
    [void]$log.AppendLine('JAVA_HOME=' + [string]$env:JAVA_HOME)
    [void]$log.AppendLine('GRADLE_USER_HOME=' + [string]$env:GRADLE_USER_HOME)
    [void]$log.AppendLine('start=' + $start.ToString('o'))
    [void]$log.AppendLine('end=' + $end.ToString('o'))
    [void]$log.AppendLine('gradle_exit=' + $code)
    [void]$log.AppendLine('--- stdout ---')
    [void]$log.AppendLine($stdout)
    [void]$log.AppendLine('--- stderr ---')
    [void]$log.AppendLine($stderr)
    Write-Utf8Text (Join-Path $ev ("attempt-$index-combined.log")) $log.ToString()
    return [pscustomobject]@{
        index = $index
        launched = $true
        start = $start.ToString('o')
        end = $end.ToString('o')
        gradleExit = $code
        buildSuccessful = [bool]$buildOk
        buildFailed = [bool]$buildFail
        upToDate = [bool]$upToDate
        taskLine = [bool]$taskRan
        javaHome = [string]$env:JAVA_HOME
        gradleUserHome = [string]$env:GRADLE_USER_HOME
        stdoutPath = "compile-evidence/attempt-$index-stdout.log"
        stderrPath = "compile-evidence/attempt-$index-stderr.log"
        combined = $combined
    }
}

$attempts = New-Object System.Collections.Generic.List[object]
$aborted = $false
$abortReason = ''
if ($cimError) {
    $aborted = $true
    $abortReason = 'process query failed: ' + $cimError
} elseif ($wrappers.Count -gt 0) {
    $aborted = $true
    $abortReason = 'active GradleWrapperMain count=' + $wrappers.Count
}

$attempt1SkipReason = ''
if (-not $aborted) {
    $homeIsSandbox = $inheritedGradleHome -and ($inheritedGradleHome -ne $realGradleHome)
    $javaMissing = (-not $inheritedJavaHome) -and (-not $javaOnPathText)
    if ($homeIsSandbox -and -not $sandboxDist) {
        $attempt1SkipReason = 'inherited GRADLE_USER_HOME is sandbox and gradle-8.8 distribution is absent; launching gradlew would download the distribution, which this task forbids'
    } elseif ($javaMissing) {
        $attempt1SkipReason = 'JAVA_HOME is empty and java.exe is not on PATH'
    }
}

if (-not $aborted -and $attempt1SkipReason) {
    $skip = New-Object System.Collections.Generic.List[string]
    $skip.Add('index=1')
    $skip.Add('launched=false')
    $skip.Add('start=' + (Get-Date).ToString('o'))
    $skip.Add('end=' + (Get-Date).ToString('o'))
    $skip.Add('gradle_exit=')
    $skip.Add('JAVA_HOME=' + $inheritedJavaHome)
    $skip.Add('GRADLE_USER_HOME=' + $inheritedGradleHome)
    $skip.Add('java_on_path=' + $javaOnPathText)
    $skip.Add('sandbox_dist=' + $sandboxDist)
    $skip.Add('sandbox_caches=' + $sandboxCaches)
    $skip.Add('sandbox_forge_gradle=' + $sandboxForge)
    $skip.Add('reason=' + $attempt1SkipReason)
    Write-Utf8Lines (Join-Path $ev 'attempt-1-meta.txt') $skip
    Write-Utf8Text (Join-Path $ev 'attempt-1-stdout.log') ''
    Write-Utf8Text (Join-Path $ev 'attempt-1-stderr.log') $attempt1SkipReason
    $attempts.Add([pscustomobject]@{
        index = 1
        launched = $false
        start = $skip[2].Substring(6)
        end = $skip[3].Substring(4)
        gradleExit = $null
        buildSuccessful = $false
        buildFailed = $false
        upToDate = $false
        taskLine = $false
        javaHome = $inheritedJavaHome
        gradleUserHome = $inheritedGradleHome
        stdoutPath = 'compile-evidence/attempt-1-stdout.log'
        stderrPath = 'compile-evidence/attempt-1-stderr.log'
        skipReason = $attempt1SkipReason
        combined = $attempt1SkipReason
    })
} elseif (-not $aborted) {
    $attempts.Add((Invoke-GradleAttempt 1))
}

$needRecovery = $false
$recoveryReason = ''
if (-not $aborted -and $attempts.Count -gt 0) {
    $first = $attempts[0]
    if (-not $first.launched) {
        $needRecovery = $true
        $recoveryReason = $first.skipReason
    } elseif ($first.gradleExit -ne 0) {
        $text = [string]$first.combined
        $envSignals = @(
            'cursor-sandbox-cache',
            'No cached version',
            'offline mode',
            'JAVA_HOME is not set',
            'JAVA_HOME',
            'Could not determine java',
            'Plugin [id:',
            'was not found in any of the following sources',
            'Could not resolve all files',
            'No such file or directory'
        )
        foreach ($signal in $envSignals) {
            if ($text.IndexOf($signal, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
                $needRecovery = $true
                $recoveryReason = 'attempt 1 failed with environment/cache signal: ' + $signal
                break
            }
        }
        if (-not $needRecovery -and $first.gradleUserHome -and $first.gradleUserHome -ne $realGradleHome) {
            $needRecovery = $true
            $recoveryReason = 'attempt 1 failed while GRADLE_USER_HOME was not the known local cache'
        }
    }
}

if ($needRecovery -and -not $aborted) {
    $env:JAVA_HOME = $realJavaHome
    $env:GRADLE_USER_HOME = $realGradleHome
    $jdkBin = Join-Path $realJavaHome 'bin'
    if (((($env:PATH -split ';')[0])) -ne $jdkBin) {
        $env:PATH = $jdkBin + ';' + $env:PATH
    }
    $attempts.Add((Invoke-GradleAttempt 2))
}

$postRows = @(Get-SourceRows)
$postWhen = (Get-Date).ToString('o')
$postLines = Format-SourceLines $postRows ("post " + $postWhen)
$postTree = Get-TreeIdentity $postRows
$postLines.Insert(3, 'tree_sha256=' + $postTree)
Write-Utf8Lines (Join-Path $ev 'post-sources.txt') $postLines

$preMap = @{}
foreach ($row in $preRows) { $preMap[$row.Path] = $row.Sha256 }
$postMap = @{}
foreach ($row in $postRows) { $postMap[$row.Path] = $row.Sha256 }
$diffLines = New-Object System.Collections.Generic.List[string]
$changed = New-Object System.Collections.Generic.List[string]
foreach ($key in $preMap.Keys) {
    if (-not $postMap.ContainsKey($key)) {
        $diffLines.Add('REMOVED' + "`t" + $key)
        $changed.Add('REMOVED ' + $key)
    } elseif ($preMap[$key] -ne $postMap[$key]) {
        $diffLines.Add('CHANGED' + "`t" + $preMap[$key] + "`t" + $postMap[$key] + "`t" + $key)
        $changed.Add('CHANGED ' + $key)
    }
}
foreach ($key in $postMap.Keys) {
    if (-not $preMap.ContainsKey($key)) {
        $diffLines.Add('ADDED' + "`t" + $key)
        $changed.Add('ADDED ' + $key)
    }
}
if ($changed.Count -eq 0) { $diffLines.Add('UNCHANGED') }
Write-Utf8Lines (Join-Path $ev 'source-diff.txt') $diffLines

$final = $null
if ($attempts.Count -gt 0) { $final = $attempts[$attempts.Count - 1] }
$finalExit = $null
if ($final -and $null -ne $final.gradleExit) { $finalExit = [int]$final.gradleExit }
$compileRan = $false
foreach ($item in $attempts) { if ($item.launched) { $compileRan = $true } }

$facts = [pscustomobject]@{
    aborted = $aborted
    abortReason = $abortReason
    cimError = $cimError
    activeWrapperCount = @($wrappers).Count
    inheritedJavaHome = $inheritedJavaHome
    inheritedGradleUserHome = $inheritedGradleHome
    javaOnPath = $javaOnPathText
    realJavaHome = $realJavaHome
    realJavaHomeExists = (Test-Path -LiteralPath (Join-Path $realJavaHome 'bin\java.exe'))
    realGradleUserHome = $realGradleHome
    realDist = $realDist
    realCaches = $realCaches
    realForgeGradleCache = $realForge
    sandboxDist = $sandboxDist
    sandboxCaches = $sandboxCaches
    sandboxForgeGradleCache = $sandboxForge
    preWhen = $preWhen
    postWhen = $postWhen
    preFileCount = @($preRows).Count
    preJavaCount = @($preRows | Where-Object { $_.Path -like '*.java' }).Count
    postFileCount = @($postRows).Count
    postJavaCount = @($postRows | Where-Object { $_.Path -like '*.java' }).Count
    preTreeSha256 = $preTree
    postTreeSha256 = $postTree
    sourceChanged = ($changed.Count -gt 0)
    sourceChanges = @($changed)
    evidencePathCount = @($evidenceRecords).Count
    evidenceMissing = @($evidenceRecords | Where-Object { -not $_.present } | ForEach-Object { $_.path })
    evidencePaths = @($evidenceRecords)
    recoveryAttempted = $needRecovery
    recoveryReason = $recoveryReason
    compileLaunched = $compileRan
    finalGradleExit = $finalExit
    attempts = @($attempts | ForEach-Object {
        [pscustomobject]@{
            index = $_.index
            launched = $_.launched
            start = $_.start
            end = $_.end
            gradleExit = $_.gradleExit
            buildSuccessful = $_.buildSuccessful
            buildFailed = $_.buildFailed
            upToDate = $_.upToDate
            taskLine = $_.taskLine
            javaHome = $_.javaHome
            gradleUserHome = $_.gradleUserHome
            skipReason = $_.skipReason
            stdoutPath = $_.stdoutPath
            stderrPath = $_.stderrPath
        }
    })
}
Write-Utf8Text (Join-Path $ev 'facts.json') ($facts | ConvertTo-Json -Depth 6)

Write-Output ('aborted=' + $aborted)
Write-Output ('abortReason=' + $abortReason)
Write-Output ('preJavaCount=' + $facts.preJavaCount)
Write-Output ('preTree=' + $preTree)
Write-Output ('postTree=' + $postTree)
Write-Output ('sourceChanged=' + $facts.sourceChanged)
Write-Output ('recoveryAttempted=' + $needRecovery)
Write-Output ('recoveryReason=' + $recoveryReason)
Write-Output ('compileLaunched=' + $compileRan)
Write-Output ('finalGradleExit=' + $finalExit)
foreach ($item in $attempts) {
    Write-Output ('attempt=' + $item.index + ' launched=' + $item.launched + ' exit=' + $item.gradleExit + ' upToDate=' + $item.upToDate + ' buildOk=' + $item.buildSuccessful)
}
if ($aborted) { exit 4 }
if ($null -eq $finalExit) { exit 5 }
exit $finalExit
