param(
    [string]$TaskDir = "C:\Users\baishang\AppData\Local\CursorAgentCodex\water-deflection-startup-qa-20261026"
)
$ErrorActionPreference = "Continue"
Set-Location "D:\alianximod"
$javaHome = "C:\Program Files\Java\jdk-17"
$javac = Join-Path $javaHome "bin\javac.exe"
$java = Join-Path $javaHome "bin\java.exe"
$project = "D:\alianximod"
$task = $TaskDir
$classpathFile = Join-Path $task "compile-classpath.txt"
$logDir = Join-Path $task "qa-logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

if (-not (Test-Path $classpathFile)) {
    throw "compile-classpath.txt is missing. Run compileJava before this script."
}

$compileClasspath = (Get-Content -Raw $classpathFile).Trim()
$mainClasses = Join-Path $project "build\classes\java\main"
$qaClasses = Join-Path $project "build\classes\java\startupqa"
New-Item -ItemType Directory -Force -Path $qaClasses | Out-Null
$asm = "C:\Users\baishang\.gradle\caches\modules-2\files-2.1\org.ow2.asm\asm\9.7.1\f0ed132a49244b042cd0e15702ab9f2ce3cc8436\asm-9.7.1.jar"
$asmTree = "C:\Users\baishang\.gradle\caches\modules-2\files-2.1\org.ow2.asm\asm-tree\9.7.1\3a53139787663b139de76b627fca0084ab60d32c\asm-tree-9.7.1.jar"
$mixinJar = "C:\Users\baishang\.gradle\caches\modules-2\files-2.1\org.spongepowered\mixin\0.8.5\9d1c0c3a304ae6697ecd477218fa61b850bf57fc\mixin-0.8.5.jar"

function Write-ArgFile {
    param([string]$Path, [string[]]$Lines)
    $utf8 = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllLines($Path, $Lines, $utf8)
}

function Invoke-Logged {
    param([string]$Name, [string]$Executable, [string]$ArgFile)
    $log = Join-Path $logDir "$Name.txt"
    Write-Host "RUN $Name"
    Write-Host "$Executable @$ArgFile"
    & $Executable "@$ArgFile" > $log 2>&1
    $exitCode = $LASTEXITCODE
    Get-Content $log | Out-Host
    Write-Host "EXIT $Name $exitCode"
    return $exitCode
}

$gateArgs = Join-Path $logDir "gate-javac.txt"
Write-ArgFile $gateArgs @(
    "-encoding", "UTF-8",
    "--release", "17",
    "-cp", "$asm;$asmTree",
    "-d", (Join-Path $project "build\startupqa-gate"),
    (Join-Path $project "tools\startupqa\StartupMixinGate.java")
)
New-Item -ItemType Directory -Force -Path (Join-Path $project "build\startupqa-gate") | Out-Null
$failed = 0
if ((Invoke-Logged "javac-gate" $javac $gateArgs) -ne 0) { $failed++ }

$wetnessGateArgs = Join-Path $logDir "wetness-gate-javac.txt"
Write-ArgFile $wetnessGateArgs @(
    "-encoding", "UTF-8",
    "--release", "17",
    "-cp", "$asm;$asmTree",
    "-d", (Join-Path $project "build\startupqa-wetness"),
    (Join-Path $project "tools\startupqa\WetnessStructureGate.java")
)
New-Item -ItemType Directory -Force -Path (Join-Path $project "build\startupqa-wetness") | Out-Null
if ((Invoke-Logged "javac-wetness-gate" $javac $wetnessGateArgs) -ne 0) { $failed++ }

$testSources = @(
    "src\test\java\com\bslx\XueXiMod\player\CrescentIdentityCheck.java",
    "src\test\java\com\bslx\XueXiMod\shu\technique\enchant\TideMarkSettlementCheck.java",
    "src\test\java\com\bslx\XueXiMod\shu\technique\release\entity\RadialCopyPlaneCheck.java",
    "src\test\java\com\bslx\XueXiMod\shu\technique\release\deflection\DeflectionCoreCheck.java",
    "src\test\java\com\bslx\XueXiMod\shu\technique\release\deflection\DeflectionProjectileCheck.java",
    "src\test\java\com\bslx\XueXiMod\shu\technique\release\deflection\hosts\HostGeometryCheck.java",
    "src\test\java\com\bslx\XueXiMod\shu\technique\release\deflection\StartupDeflectionRegression.java",
    "src\test\java\com\bslx\XueXiMod\network\ChainStepContactCheck.java",
    "src\test\java\com\bslx\XueXiMod\status\WetnessTierCheck.java",
    "src\test\java\com\bslx\XueXiMod\shu\technique\release\deflection\WetnessEconomyCheck.java",
    "src\test\java\com\bslx\XueXiMod\shu\technique\release\momentum\WetnessLegacyCheck.java"
) | ForEach-Object { Join-Path $project $_ }
$testArgs = Join-Path $logDir "tests-javac.txt"
$testLines = New-Object System.Collections.Generic.List[string]
$testLines.Add("-encoding")
$testLines.Add("UTF-8")
$testLines.Add("--release")
$testLines.Add("17")
$testLines.Add("-proc:none")
$testLines.Add("-cp")
$testLines.Add("$mainClasses;$compileClasspath")
$testLines.Add("-d")
$testLines.Add($qaClasses)
foreach ($source in $testSources) { $testLines.Add($source) }
Write-ArgFile $testArgs $testLines
if ((Invoke-Logged "javac-tests" $javac $testArgs) -ne 0) { $failed++ }

$gateRun = Join-Path $logDir "gate-java.txt"
$gateClasspath = $asm + ";" + $asmTree + ";" + $mixinJar + ";" + (Join-Path $project "build\startupqa-gate") + ";" + $compileClasspath
Write-ArgFile $gateRun @(
    "-Dfile.encoding=UTF-8",
    "-cp",
    $gateClasspath,
    "StartupMixinGate"
)
if ((Invoke-Logged "StartupMixinGate" $java $gateRun) -ne 0) { $failed++ }

$wetnessRun = Join-Path $logDir "wetness-gate-java.txt"
$wetnessClasspath = $asm + ";" + $asmTree + ";" + (Join-Path $project "build\startupqa-wetness")
Write-ArgFile $wetnessRun @(
    "-Dfile.encoding=UTF-8",
    "-cp",
    $wetnessClasspath,
    "WetnessStructureGate"
)
if ((Invoke-Logged "WetnessStructureGate" $java $wetnessRun) -ne 0) { $failed++ }

$testRunCp = "$qaClasses;$mainClasses;$compileClasspath"
$mains = @(
    "DeflectionCoreCheck", "com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionCoreCheck",
    "DeflectionProjectileCheck", "com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionProjectileCheck",
    "HostGeometryCheck", "com.bslx.XueXiMod.shu.technique.release.deflection.hosts.HostGeometryCheck",
    "CrescentIdentityCheck", "com.bslx.XueXiMod.player.CrescentIdentityCheck",
    "TideMarkSettlementCheck", "com.bslx.XueXiMod.shu.technique.enchant.TideMarkSettlementCheck",
    "RadialCopyPlaneCheck", "com.bslx.XueXiMod.shu.technique.release.entity.RadialCopyPlaneCheck",
    "StartupDeflectionRegression", "com.bslx.XueXiMod.shu.technique.release.deflection.StartupDeflectionRegression",
    "ChainStepContactCheck", "com.bslx.XueXiMod.network.ChainStepContactCheck",
    "WetnessTierCheck", "com.bslx.XueXiMod.status.WetnessTierCheck",
    "WetnessEconomyCheck", "com.bslx.XueXiMod.shu.technique.release.deflection.WetnessEconomyCheck",
    "WetnessLegacyCheck", "com.bslx.XueXiMod.shu.technique.release.momentum.WetnessLegacyCheck"
)
for ($cursor = 0; $cursor -lt $mains.Length; $cursor += 2) {
    $runFile = Join-Path $logDir ($mains[$cursor] + "-java.txt")
    Write-ArgFile $runFile @("-Dfile.encoding=UTF-8", "-cp", $testRunCp, $mains[$cursor + 1])
    if ((Invoke-Logged $mains[$cursor] $java $runFile) -ne 0) { $failed++ }
}

Write-Host "FAILED_COMMANDS $failed"
if ($failed -ne 0) { exit 1 }
exit 0
