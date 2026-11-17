$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$outputDir = Join-Path $projectRoot 'build\wood-base-checks\effects'
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

$javac = Join-Path $env:JAVA_HOME 'bin\javac.exe'
if (-not (Test-Path $javac)) {
    $javac = 'C:\Program Files\Java\jdk-17\bin\javac.exe'
}
$java = Join-Path (Split-Path $javac) 'java.exe'

$sources = @(
    'src\main\java\com\bslx\XueXiMod\combat\counter\ManaRestoreMath.java',
    'src\main\java\com\bslx\XueXiMod\combat\counter\CounterAdmission.java',
    'src\main\java\com\bslx\XueXiMod\combat\counter\CounterArmResult.java',
    'src\main\java\com\bslx\XueXiMod\combat\counter\CounterWindowSpec.java',
    'src\main\java\com\bslx\XueXiMod\combat\counter\CounterHurtFrame.java',
    'src\main\java\com\bslx\XueXiMod\combat\counter\CounterLedger.java',
    'tools\wood-base-checks\effects\CounterEffectChecks.java'
) | ForEach-Object { Join-Path $projectRoot $_ }

& $javac --release 17 -encoding UTF-8 -d $outputDir @sources
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
& $java -cp $outputDir CounterEffectChecks
exit $LASTEXITCODE
