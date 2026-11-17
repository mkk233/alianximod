$ErrorActionPreference = 'Stop'
$postfix = $PSScriptRoot
$project = 'D:\alianximod'
$javac = 'C:\Program Files\Java\jdk-17\bin\javac.exe'
$java = 'C:\Program Files\Java\jdk-17\bin\java.exe'
$classpathFile = Join-Path $postfix 'test-runtime-classpath.txt'
$entries = New-Object System.Collections.Generic.List[string]
Get-Content -LiteralPath $classpathFile -Encoding UTF8 | ForEach-Object {
    $entry = $_.Trim()
    if ($entry.Length -gt 0 -and $entry -notlike '*\verification\*') {
        $entries.Add($entry)
    }
}
$quoted = foreach ($entry in $entries) {
    if ($entry.Contains(' ')) { '"' + $entry + '"' } else { $entry }
}
$joined = [string]::Join(';', $quoted)
$dest = Join-Path $postfix 'check-classes'
if (Test-Path -LiteralPath $dest) {
    Remove-Item -LiteralPath $dest -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $dest | Out-Null
$relativeSources = @(
    'src\test\java\com\bslx\XueXiMod\status\fire\seed\FireSeedCheck.java',
    'src\test\java\com\bslx\XueXiMod\status\fire\furnace\FurnaceMathCheck.java',
    'src\test\java\com\bslx\XueXiMod\status\fire\furnace\FurnaceStructureCheck.java',
    'src\test\java\com\bslx\XueXiMod\status\fire\furnace\FurnaceRepairCheck.java',
    'src\test\java\com\bslx\XueXiMod\shu\technique\release\fire\FireLayerCheck.java',
    'src\test\java\com\bslx\XueXiMod\shu\technique\release\fire\BurnLedgerCheck.java',
    'src\test\java\com\bslx\XueXiMod\shu\technique\release\fire\FireReleaseStructureCheck.java',
    'src\test\java\com\bslx\XueXiMod\shu\technique\release\fire\FireCooldownCheck.java',
    'src\test\java\com\bslx\XueXiMod\shu\technique\release\fire\BurnDispatchCheck.java',
    'src\test\java\com\bslx\XueXiMod\shu\technique\release\fire\FireDeathMessageCheck.java',
    'src\test\java\com\bslx\XueXiMod\shu\technique\release\fire\ElementHurtLifecycleCheck.java',
    'src\test\java\com\bslx\XueXiMod\shu\technique\release\fire\HurtBoundaryHook.java',
    'src\test\java\com\bslx\XueXiMod\shu\technique\release\fire\HurtBoundaryFixture.java',
    'src\test\java\com\bslx\XueXiMod\shu\technique\release\fire\FireHurtBytecodeCheck.java'
)
$javacArgs = New-Object System.Collections.Generic.List[string]
foreach ($token in @('--release', '17', '-encoding', 'UTF-8', '-classpath', $joined, '-d', 'check-classes')) {
    $javacArgs.Add($token)
}
foreach ($relative in $relativeSources) {
    $javacArgs.Add((Join-Path $project $relative))
}
$javacArgFile = Join-Path $postfix 'javac-args.txt'
[System.IO.File]::WriteAllLines($javacArgFile, $javacArgs, [System.Text.Encoding]::Default)
$compile = Start-Process -FilePath $javac -ArgumentList "@$javacArgFile" -WorkingDirectory $postfix -Wait -PassThru -NoNewWindow -RedirectStandardOutput (Join-Path $postfix 'check-javac.log') -RedirectStandardError (Join-Path $postfix 'check-javac.err.log')
Set-Content -LiteralPath (Join-Path $postfix 'check-javac.exit.txt') -Value $compile.ExitCode -Encoding ascii
Write-Output "JAVAC_EXIT=$($compile.ExitCode)"
if ($compile.ExitCode -ne 0) { exit $compile.ExitCode }
$runtime = 'check-classes;' + $joined
$mains = @(
    'com.bslx.XueXiMod.status.fire.seed.FireSeedCheck',
    'com.bslx.XueXiMod.status.fire.furnace.FurnaceMathCheck',
    'com.bslx.XueXiMod.status.fire.furnace.FurnaceStructureCheck',
    'com.bslx.XueXiMod.shu.technique.release.fire.FireLayerCheck',
    'com.bslx.XueXiMod.shu.technique.release.fire.BurnLedgerCheck',
    'com.bslx.XueXiMod.shu.technique.release.fire.FireReleaseStructureCheck',
    'com.bslx.XueXiMod.shu.technique.release.fire.FireCooldownCheck',
    'com.bslx.XueXiMod.shu.technique.release.fire.BurnDispatchCheck',
    'com.bslx.XueXiMod.status.fire.furnace.FurnaceRepairCheck',
    'com.bslx.XueXiMod.shu.technique.release.fire.FireDeathMessageCheck',
    'com.bslx.XueXiMod.shu.technique.release.fire.ElementHurtLifecycleCheck',
    'com.bslx.XueXiMod.shu.technique.release.fire.FireHurtBytecodeCheck'
)
$summary = New-Object System.Collections.Generic.List[string]
foreach ($main in $mains) {
    $safe = $main.Substring($main.LastIndexOf('.') + 1)
    $argFile = Join-Path $postfix "$safe-java-args.txt"
    [System.IO.File]::WriteAllLines($argFile, @('-Dfile.encoding=UTF-8', '-classpath', $runtime, $main), [System.Text.Encoding]::Default)
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo.FileName = $java
    $process.StartInfo.Arguments = "@`"$argFile`""
    $process.StartInfo.WorkingDirectory = $postfix
    $process.StartInfo.UseShellExecute = $false
    $process.StartInfo.RedirectStandardOutput = $true
    $process.StartInfo.RedirectStandardError = $true
    $process.StartInfo.StandardOutputEncoding = [System.Text.Encoding]::UTF8
    $process.StartInfo.StandardErrorEncoding = [System.Text.Encoding]::UTF8
    [void]$process.Start()
    $stdoutTask = $process.StandardOutput.ReadToEndAsync()
    $stderrTask = $process.StandardError.ReadToEndAsync()
    $finished = $process.WaitForExit(90000)
    if (-not $finished) {
        try { $process.Kill() } catch {}
        $process.WaitForExit()
        $summary.Add("$safe TIMEOUT")
        Set-Content -LiteralPath (Join-Path $postfix "$safe.exit.txt") -Value 'TIMEOUT' -Encoding ascii
        Write-Output "$safe TIMEOUT"
        continue
    }
    $stdoutTask.Wait()
    $stderrTask.Wait()
    [System.IO.File]::WriteAllText((Join-Path $postfix "$safe.out.log"), $stdoutTask.Result, [System.Text.UTF8Encoding]::new($false))
    [System.IO.File]::WriteAllText((Join-Path $postfix "$safe.err.log"), $stderrTask.Result, [System.Text.UTF8Encoding]::new($false))
    $summary.Add("$safe EXIT=$($process.ExitCode)")
    Set-Content -LiteralPath (Join-Path $postfix "$safe.exit.txt") -Value $process.ExitCode -Encoding ascii
    Write-Output "$safe EXIT=$($process.ExitCode)"
}
[System.IO.File]::WriteAllLines((Join-Path $postfix 'check-summary.txt'), $summary, [System.Text.UTF8Encoding]::new($false))
