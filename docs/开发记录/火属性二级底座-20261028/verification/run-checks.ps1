$ErrorActionPreference = 'Stop'
$verify = $PSScriptRoot
$project = 'D:\alianximod'
$javac = 'C:\Program Files\Java\jdk-17\bin\javac.exe'
$java = 'C:\Program Files\Java\jdk-17\bin\java.exe'
$classpathFile = Join-Path $verify 'test-runtime-classpath.txt'
$entries = New-Object System.Collections.Generic.List[string]
Get-Content -LiteralPath $classpathFile -Encoding UTF8 | ForEach-Object {
    $entry = $_.Trim()
    if ($entry.Length -gt 0) {
        $entries.Add($entry)
    }
}
$quoted = foreach ($entry in $entries) {
    if ($entry.Contains(' ') -or $entry.Contains(';')) {
        '"' + $entry + '"'
    } else {
        $entry
    }
}
$joined = [string]::Join(';', $quoted)
$dest = Join-Path $verify 'check-classes'
if (Test-Path $dest) {
    Remove-Item -LiteralPath $dest -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $dest | Out-Null

$relativeSources = @(
    'src\main\java\com\bslx\XueXiMod\status\fire\seed\FireSeedMath.java',
    'src\main\java\com\bslx\XueXiMod\status\fire\seed\FireSeedAccount.java',
    'src\main\java\com\bslx\XueXiMod\status\fire\seed\FireSeedArchive.java',
    'src\main\java\com\bslx\XueXiMod\status\fire\seed\FireSeedSnapshot.java',
    'src\main\java\com\bslx\XueXiMod\status\fire\seed\FireCastContext.java',
    'src\test\java\com\bslx\XueXiMod\status\fire\seed\FireSeedCheck.java',
    'src\test\java\com\bslx\XueXiMod\status\fire\furnace\FurnaceMathCheck.java',
    'src\test\java\com\bslx\XueXiMod\status\fire\furnace\FurnaceStructureCheck.java',
    'src\test\java\com\bslx\XueXiMod\shu\technique\release\fire\FireLayerCheck.java',
    'src\test\java\com\bslx\XueXiMod\shu\technique\release\fire\BurnLedgerCheck.java',
    'src\test\java\com\bslx\XueXiMod\shu\technique\release\fire\FireReleaseStructureCheck.java'
)
$javacArgs = New-Object System.Collections.Generic.List[string]
$javacArgs.Add('--release')
$javacArgs.Add('17')
$javacArgs.Add('-encoding')
$javacArgs.Add('UTF-8')
$javacArgs.Add('-classpath')
$javacArgs.Add($joined)
$javacArgs.Add('-d')
$javacArgs.Add('check-classes')
foreach ($relative in $relativeSources) {
    $javacArgs.Add((Join-Path $project $relative))
}
$javacArgFile = Join-Path $verify 'javac-args.txt'
[System.IO.File]::WriteAllLines($javacArgFile, $javacArgs, [System.Text.Encoding]::Default)
$compileLog = Join-Path $verify 'check-javac.log'
$compileErr = Join-Path $verify 'check-javac.err.log'
$compile = Start-Process -FilePath $javac -ArgumentList "@$javacArgFile" -WorkingDirectory $verify -Wait -PassThru -NoNewWindow -RedirectStandardOutput $compileLog -RedirectStandardError $compileErr
Set-Content -LiteralPath (Join-Path $verify 'check-javac.exit.txt') -Value $compile.ExitCode -Encoding ascii
Write-Output "JAVAC_EXIT=$($compile.ExitCode)"
if ($compile.ExitCode -ne 0) {
    exit $compile.ExitCode
}

$runtime = 'check-classes;' + $joined
$mains = @(
    'com.bslx.XueXiMod.status.fire.seed.FireSeedCheck',
    'com.bslx.XueXiMod.status.fire.furnace.FurnaceMathCheck',
    'com.bslx.XueXiMod.status.fire.furnace.FurnaceStructureCheck',
    'com.bslx.XueXiMod.shu.technique.release.fire.FireLayerCheck',
    'com.bslx.XueXiMod.shu.technique.release.fire.BurnLedgerCheck',
    'com.bslx.XueXiMod.shu.technique.release.fire.FireReleaseStructureCheck'
)
$summary = New-Object System.Collections.Generic.List[string]
foreach ($main in $mains) {
    $safe = $main.Substring($main.LastIndexOf('.') + 1)
    $argFile = Join-Path $verify "$safe-java-args.txt"
    $javaArgs = @(
        '-Dfile.encoding=UTF-8',
        '-classpath',
        $runtime,
        $main
    )
    [System.IO.File]::WriteAllLines($argFile, $javaArgs, [System.Text.Encoding]::Default)
    $outLog = Join-Path $verify "$safe.out.log"
    $errLog = Join-Path $verify "$safe.err.log"
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo.FileName = $java
    $process.StartInfo.Arguments = "@`"$argFile`""
    $process.StartInfo.WorkingDirectory = $verify
    $process.StartInfo.UseShellExecute = $false
    $process.StartInfo.RedirectStandardOutput = $true
    $process.StartInfo.RedirectStandardError = $true
    $process.StartInfo.StandardOutputEncoding = [System.Text.Encoding]::UTF8
    $process.StartInfo.StandardErrorEncoding = [System.Text.Encoding]::UTF8
    [void]$process.Start()
    $stdoutTask = $process.StandardOutput.ReadToEndAsync()
    $stderrTask = $process.StandardError.ReadToEndAsync()
    $finished = $process.WaitForExit(60000)
    if (-not $finished) {
        try { $process.Kill() } catch {}
        $process.WaitForExit()
        $summary.Add("$safe TIMEOUT")
        Set-Content -LiteralPath (Join-Path $verify "$safe.exit.txt") -Value 'TIMEOUT' -Encoding ascii
        continue
    }
    $stdoutTask.Wait()
    $stderrTask.Wait()
    [System.IO.File]::WriteAllText($outLog, $stdoutTask.Result, [System.Text.UTF8Encoding]::new($false))
    [System.IO.File]::WriteAllText($errLog, $stderrTask.Result, [System.Text.UTF8Encoding]::new($false))
    $summary.Add("$safe EXIT=$($process.ExitCode)")
    Set-Content -LiteralPath (Join-Path $verify "$safe.exit.txt") -Value $process.ExitCode -Encoding ascii
    Write-Output "$safe EXIT=$($process.ExitCode)"
}
Set-Content -LiteralPath (Join-Path $verify 'check-summary.txt') -Value $summary -Encoding utf8
