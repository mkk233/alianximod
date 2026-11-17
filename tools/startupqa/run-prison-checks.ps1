param(
    [string]$TaskDir = "C:\Users\baishang\AppData\Local\CursorAgentCodex\water-prison-implementation-20261026"
)
$ErrorActionPreference = "Continue"
Set-Location "D:\alianximod"
$javaHome = "C:\Program Files\Java\jdk-17"
$javac = Join-Path $javaHome "bin\javac.exe"
$java = Join-Path $javaHome "bin\java.exe"
$project = "D:\alianximod"
$logDir = Join-Path $TaskDir "prison-qa-logs"
$classpathFile = Join-Path $TaskDir "compile-classpath.txt"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

if (-not (Test-Path $classpathFile)) {
    throw "compile-classpath.txt is missing. Run compileJava before this script."
}

$compileClasspath = (Get-Content -Raw $classpathFile).Trim()
$mainClasses = Join-Path $project "build\classes\java\main"
if (-not (Test-Path $mainClasses)) {
    throw "build\classes\java\main is missing. compileJava must exit 0 before this script."
}

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
    & $Executable "@$ArgFile" > $log 2>&1
    $exitCode = $LASTEXITCODE
    Get-Content $log | Out-Host
    Write-Host "EXIT $Name $exitCode"
    return $exitCode
}

$failed = 0
$prisonGateDir = Join-Path $project "build\prisonqa-structure"
$gateDir = Join-Path $project "build\prisonqa-startup"
$wetnessDir = Join-Path $project "build\prisonqa-wetness"
$flightDir = Join-Path $project "build\prisonqa-flight"
$bossDir = Join-Path $project "build\prisonqa-boss"
$prisonClasses = Join-Path $project "build\classes\java\prisonqa"
$regressionClasses = Join-Path $project "build\classes\java\prison-regression"
New-Item -ItemType Directory -Force -Path $prisonGateDir, $gateDir, $wetnessDir, $flightDir, $bossDir, $prisonClasses, $regressionClasses | Out-Null

$prisonGateArgs = Join-Path $logDir "prison-gates-javac.args"
Write-ArgFile $prisonGateArgs @(
    "-encoding", "UTF-8", "--release", "17", "-cp", "$asm;$asmTree", "-d", $prisonGateDir,
    (Join-Path $project "tools\startupqa\PrisonStructureGate.java"),
    (Join-Path $project "tools\startupqa\PrisonResourceGate.java")
)
if ((Invoke-Logged "javac-prison-gates" $javac $prisonGateArgs) -ne 0) { $failed++ }

$prisonGateCp = "$asm;$asmTree;$prisonGateDir"
foreach ($gateName in @("PrisonResourceGate", "PrisonStructureGate")) {
    $runFile = Join-Path $logDir "$gateName.args"
    Write-ArgFile $runFile @("-Dfile.encoding=UTF-8", "-cp", $prisonGateCp, $gateName)
    if ((Invoke-Logged $gateName $java $runFile) -ne 0) { $failed++ }
}

$prisonTestArgs = Join-Path $logDir "prison-tests-javac.args"
Write-ArgFile $prisonTestArgs @(
    "-encoding", "UTF-8", "--release", "17", "-proc:none",
    "-cp", "$mainClasses;$compileClasspath",
    "-d", $prisonClasses,
    (Join-Path $project "src\test\java\com\bslx\XueXiMod\shu\technique\release\prison\PrisonQaDurationCheck.java"),
    (Join-Path $project "src\test\java\com\bslx\XueXiMod\shu\technique\release\prison\PrisonQaGeometryCheck.java"),
    (Join-Path $project "src\test\java\com\bslx\XueXiMod\shu\technique\release\prison\PrisonQaShareCheck.java"),
    (Join-Path $project "src\test\java\com\bslx\XueXiMod\network\prison\PrisonQaPacketCheck.java")
)
if ((Invoke-Logged "javac-prison-tests" $javac $prisonTestArgs) -ne 0) { $failed++ }

$prisonRunCp = "$prisonClasses;$mainClasses;$compileClasspath"
foreach ($pair in @(
    @("PrisonQaDurationCheck", "com.bslx.XueXiMod.shu.technique.release.prison.PrisonQaDurationCheck"),
    @("PrisonQaGeometryCheck", "com.bslx.XueXiMod.shu.technique.release.prison.PrisonQaGeometryCheck"),
    @("PrisonQaShareCheck", "com.bslx.XueXiMod.shu.technique.release.prison.PrisonQaShareCheck"),
    @("PrisonQaPacketCheck", "com.bslx.XueXiMod.network.prison.PrisonQaPacketCheck")
)) {
    $runFile = Join-Path $logDir ($pair[0] + ".args")
    Write-ArgFile $runFile @("-Dfile.encoding=UTF-8", "-cp", $prisonRunCp, $pair[1])
    if ((Invoke-Logged $pair[0] $java $runFile) -ne 0) { $failed++ }
}

$gateArgs = Join-Path $logDir "StartupMixinGate-javac.args"
Write-ArgFile $gateArgs @(
    "-encoding", "UTF-8", "--release", "17", "-cp", "$asm;$asmTree", "-d", $gateDir,
    (Join-Path $project "tools\startupqa\StartupMixinGate.java")
)
if ((Invoke-Logged "javac-StartupMixinGate" $javac $gateArgs) -ne 0) { $failed++ }

$wetnessArgs = Join-Path $logDir "WetnessStructureGate-javac.args"
Write-ArgFile $wetnessArgs @(
    "-encoding", "UTF-8", "--release", "17", "-cp", "$asm;$asmTree", "-d", $wetnessDir,
    (Join-Path $project "tools\startupqa\WetnessStructureGate.java")
)
if ((Invoke-Logged "javac-WetnessStructureGate" $javac $wetnessArgs) -ne 0) { $failed++ }

$flightArgs = Join-Path $logDir "WetnessFlightGate-javac.args"
Write-ArgFile $flightArgs @(
    "-encoding", "UTF-8", "--release", "17", "-cp", "$asm;$asmTree", "-d", $flightDir,
    (Join-Path $project "tools\startupqa\WetnessFlightGate.java")
)
if ((Invoke-Logged "javac-WetnessFlightGate" $javac $flightArgs) -ne 0) { $failed++ }

$bossArgs = Join-Path $logDir "WetnessBossGate-javac.args"
Write-ArgFile $bossArgs @(
    "-encoding", "UTF-8", "--release", "17", "-cp", "$asm;$asmTree", "-d", $bossDir,
    (Join-Path $project "tools\startupqa\WetnessBossGate.java")
)
if ((Invoke-Logged "javac-WetnessBossGate" $javac $bossArgs) -ne 0) { $failed++ }

$regressionSources = @(
    "src\test\java\com\bslx\XueXiMod\shu\technique\release\deflection\DeflectionCoreCheck.java",
    "src\test\java\com\bslx\XueXiMod\shu\technique\release\deflection\DeflectionProjectileCheck.java",
    "src\test\java\com\bslx\XueXiMod\shu\technique\release\deflection\hosts\HostGeometryCheck.java",
    "src\test\java\com\bslx\XueXiMod\player\CrescentIdentityCheck.java",
    "src\test\java\com\bslx\XueXiMod\shu\technique\enchant\TideMarkSettlementCheck.java",
    "src\test\java\com\bslx\XueXiMod\shu\technique\release\entity\RadialCopyPlaneCheck.java",
    "src\test\java\com\bslx\XueXiMod\shu\technique\release\deflection\StartupDeflectionRegression.java",
    "src\test\java\com\bslx\XueXiMod\network\ChainStepContactCheck.java",
    "src\test\java\com\bslx\XueXiMod\status\WetnessTierCheck.java",
    "src\test\java\com\bslx\XueXiMod\shu\technique\release\deflection\WetnessEconomyCheck.java",
    "src\test\java\com\bslx\XueXiMod\shu\technique\release\momentum\WetnessLegacyCheck.java",
    "src\test\java\com\bslx\XueXiMod\status\WetnessFlightCheck.java"
)
$regressionArgs = Join-Path $logDir "regression-tests-javac.args"
$regressionLines = New-Object System.Collections.Generic.List[string]
$regressionLines.Add("-encoding")
$regressionLines.Add("UTF-8")
$regressionLines.Add("--release")
$regressionLines.Add("17")
$regressionLines.Add("-proc:none")
$regressionLines.Add("-cp")
$regressionLines.Add("$mainClasses;$compileClasspath")
$regressionLines.Add("-d")
$regressionLines.Add($regressionClasses)
foreach ($relative in $regressionSources) {
    $regressionLines.Add((Join-Path $project $relative))
}
Write-ArgFile $regressionArgs $regressionLines
if ((Invoke-Logged "javac-regression-tests" $javac $regressionArgs) -ne 0) { $failed++ }

$startupCp = $asm + ";" + $asmTree + ";" + $mixinJar + ";" + $gateDir + ";" + $compileClasspath
$startupRun = Join-Path $logDir "StartupMixinGate.args"
Write-ArgFile $startupRun @("-Dfile.encoding=UTF-8", "-cp", $startupCp, "StartupMixinGate")
if ((Invoke-Logged "StartupMixinGate" $java $startupRun) -ne 0) { $failed++ }

$wetnessCp = $asm + ";" + $asmTree + ";" + $wetnessDir
$wetnessRun = Join-Path $logDir "WetnessStructureGate.args"
Write-ArgFile $wetnessRun @("-Dfile.encoding=UTF-8", "-cp", $wetnessCp, "WetnessStructureGate")
if ((Invoke-Logged "WetnessStructureGate" $java $wetnessRun) -ne 0) { $failed++ }

$flightCp = $asm + ";" + $asmTree + ";" + $flightDir
$flightRun = Join-Path $logDir "WetnessFlightGate.args"
Write-ArgFile $flightRun @("-Dfile.encoding=UTF-8", "-cp", $flightCp, "WetnessFlightGate")
if ((Invoke-Logged "WetnessFlightGate" $java $flightRun) -ne 0) { $failed++ }

$bossCp = $asm + ";" + $asmTree + ";" + $bossDir
$bossRun = Join-Path $logDir "WetnessBossGate.args"
Write-ArgFile $bossRun @("-Dfile.encoding=UTF-8", "-cp", $bossCp, "WetnessBossGate")
if ((Invoke-Logged "WetnessBossGate" $java $bossRun) -ne 0) { $failed++ }

$regressionCp = "$regressionClasses;$mainClasses;$compileClasspath"
foreach ($pair in @(
    @("DeflectionCoreCheck", "com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionCoreCheck"),
    @("DeflectionProjectileCheck", "com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionProjectileCheck"),
    @("HostGeometryCheck", "com.bslx.XueXiMod.shu.technique.release.deflection.hosts.HostGeometryCheck"),
    @("CrescentIdentityCheck", "com.bslx.XueXiMod.player.CrescentIdentityCheck"),
    @("TideMarkSettlementCheck", "com.bslx.XueXiMod.shu.technique.enchant.TideMarkSettlementCheck"),
    @("RadialCopyPlaneCheck", "com.bslx.XueXiMod.shu.technique.release.entity.RadialCopyPlaneCheck"),
    @("StartupDeflectionRegression", "com.bslx.XueXiMod.shu.technique.release.deflection.StartupDeflectionRegression"),
    @("ChainStepContactCheck", "com.bslx.XueXiMod.network.ChainStepContactCheck"),
    @("WetnessTierCheck", "com.bslx.XueXiMod.status.WetnessTierCheck"),
    @("WetnessEconomyCheck", "com.bslx.XueXiMod.shu.technique.release.deflection.WetnessEconomyCheck"),
    @("WetnessLegacyCheck", "com.bslx.XueXiMod.shu.technique.release.momentum.WetnessLegacyCheck"),
    @("WetnessFlightCheck", "com.bslx.XueXiMod.status.WetnessFlightCheck")
)) {
    $runFile = Join-Path $logDir ($pair[0] + ".args")
    Write-ArgFile $runFile @("-Dfile.encoding=UTF-8", "-cp", $regressionCp, $pair[1])
    if ((Invoke-Logged $pair[0] $java $runFile) -ne 0) { $failed++ }
}

Write-Host "FAILED_COMMANDS $failed"
Write-Host "NOT_GAME 这些检查没有启动 Minecraft，也没有做 Mixin 变换"
if ($failed -ne 0) { exit 1 }
exit 0
