$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$source = Join-Path $PSScriptRoot 'namegate\NameGate.java'
$outputDir = Join-Path $projectRoot 'build\namegate'
$classFile = Join-Path $outputDir 'NameGate.class'
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

$needsCompile = -not (Test-Path $classFile)
if (-not $needsCompile) {
    $needsCompile = (Get-Item $source).LastWriteTime -gt (Get-Item $classFile).LastWriteTime
}
if ($needsCompile) {
    & javac --release 17 -encoding UTF-8 --add-modules jdk.compiler -d $outputDir $source
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

& java --add-modules jdk.compiler -cp $outputDir NameGate $projectRoot
exit $LASTEXITCODE
