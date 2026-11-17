param(
    [string]$TaskDir = "C:\Users\baishang\AppData\Local\CursorAgentCodex\wetness-tiers-implementation-20261026"
)
$ErrorActionPreference = "Continue"
Set-Location "D:\alianximod"
$javaHome = "C:\Program Files\Java\jdk-17"
$javac = Join-Path $javaHome "bin\javac.exe"
$java = Join-Path $javaHome "bin\java.exe"
$project = "D:\alianximod"
$logDir = Join-Path $TaskDir "final-qa-logs"
$classpathFile = Join-Path $logDir "compile-classpath.txt"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

if (-not (Test-Path $classpathFile)) {
    throw "final compile-classpath.txt is missing. Run compileJava before this script."
}

$compileClasspath = (Get-Content -Raw $classpathFile).Trim()
$mainClasses = Join-Path $project "build\classes\java\main"
$qaClasses = Join-Path $project "build\classes\java\finalqa"
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
    & $Executable "@$ArgFile" > $log 2>&1
    $exitCode = $LASTEXITCODE
    Get-Content $log | Out-Host
    Write-Host "EXIT $Name $exitCode"
    return $exitCode
}

$failed = 0
$gateDir = Join-Path $project "build\startupqa-gate"
$flightGateDir = Join-Path $project "build\startupqa-flight"
$bossGateDir = Join-Path $project "build\startupqa-boss"
New-Item -ItemType Directory -Force -Path $gateDir, $flightGateDir, $bossGateDir | Out-Null

$gateArgs = Join-Path $logDir "gate-javac.args"
Write-ArgFile $gateArgs @(
    "-encoding", "UTF-8", "--release", "17", "-cp", "$asm;$asmTree", "-d", $gateDir,
    (Join-Path $project "tools\startupqa\StartupMixinGate.java")
)
if ((Invoke-Logged "javac-StartupMixinGate" $javac $gateArgs) -ne 0) { $failed++ }

$flightArgs = Join-Path $logDir "flight-gate-javac.args"
Write-ArgFile $flightArgs @(
    "-encoding", "UTF-8", "--release", "17", "-cp", "$asm;$asmTree", "-d", $flightGateDir,
    (Join-Path $project "tools\startupqa\WetnessFlightGate.java")
)
if ((Invoke-Logged "javac-WetnessFlightGate" $javac $flightArgs) -ne 0) { $failed++ }

$bossArgs = Join-Path $logDir "boss-gate-javac.args"
Write-ArgFile $bossArgs @(
    "-encoding", "UTF-8", "--release", "17", "-cp", "$asm;$asmTree", "-d", $bossGateDir,
    (Join-Path $project "tools\startupqa\WetnessBossGate.java")
)
if ((Invoke-Logged "javac-WetnessBossGate" $javac $bossArgs) -ne 0) { $failed++ }

$testArgs = Join-Path $logDir "final-tests-javac.args"
Write-ArgFile $testArgs @(
    "-encoding", "UTF-8", "--release", "17", "-proc:none",
    "-cp", "$mainClasses;$compileClasspath",
    "-d", $qaClasses,
    (Join-Path $project "src\test\java\com\bslx\XueXiMod\status\WetnessTierCheck.java"),
    (Join-Path $project "src\test\java\com\bslx\XueXiMod\status\WetnessFlightCheck.java")
)
if ((Invoke-Logged "javac-final-tests" $javac $testArgs) -ne 0) { $failed++ }

$gateRun = Join-Path $logDir "StartupMixinGate.args"
Write-ArgFile $gateRun @(
    "-Dfile.encoding=UTF-8", "-cp",
    ($asm + ";" + $asmTree + ";" + $mixinJar + ";" + $gateDir + ";" + $compileClasspath),
    "StartupMixinGate"
)
if ((Invoke-Logged "StartupMixinGate" $java $gateRun) -ne 0) { $failed++ }

$flightRun = Join-Path $logDir "WetnessFlightGate.args"
Write-ArgFile $flightRun @(
    "-Dfile.encoding=UTF-8", "-cp", ($asm + ";" + $asmTree + ";" + $flightGateDir),
    "WetnessFlightGate"
)
if ((Invoke-Logged "WetnessFlightGate" $java $flightRun) -ne 0) { $failed++ }

$bossRun = Join-Path $logDir "WetnessBossGate.args"
Write-ArgFile $bossRun @(
    "-Dfile.encoding=UTF-8", "-cp", ($asm + ";" + $asmTree + ";" + $bossGateDir),
    "WetnessBossGate"
)
if ((Invoke-Logged "WetnessBossGate" $java $bossRun) -ne 0) { $failed++ }

$testRunCp = "$qaClasses;$mainClasses;$compileClasspath"
foreach ($pair in @(
    @("WetnessTierCheck", "com.bslx.XueXiMod.status.WetnessTierCheck"),
    @("WetnessFlightCheck", "com.bslx.XueXiMod.status.WetnessFlightCheck")
)) {
    $runFile = Join-Path $logDir ($pair[0] + ".args")
    Write-ArgFile $runFile @("-Dfile.encoding=UTF-8", "-cp", $testRunCp, $pair[1])
    if ((Invoke-Logged $pair[0] $java $runFile) -ne 0) { $failed++ }
}

Write-Host "FAILED_COMMANDS $failed"
if ($failed -ne 0) { exit 1 }
exit 0
