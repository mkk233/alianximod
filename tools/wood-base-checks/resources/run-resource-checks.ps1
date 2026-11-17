$ErrorActionPreference = 'Stop'
$project = 'D:\alianximod'
if ($args.Count -gt 0 -and $args[0]) {
    $project = $args[0]
}
$source = Join-Path $project 'src\main\java\com\bslx\XueXiMod\status\wood\resource'
$names = @(
    'WoodLimits.java',
    'FlowerKind.java',
    'ResourceOrigin.java',
    'RemovalReason.java',
    'WoodReject.java',
    'WoodInterval.java',
    'SuihuaSnapshot.java',
    'ProductionTicket.java',
    'FragmentHandle.java',
    'FragmentTake.java',
    'GroundFragmentView.java',
    'GroundPieceTake.java',
    'GroundConsumePreview.java',
    'GroundConsumeReceipt.java',
    'GroundQueryResult.java',
    'CreateResult.java',
    'SplitResult.java',
    'RemoveResult.java',
    'SuihuaBalanceSink.java',
    'WoodLedger.java'
)
$paths = New-Object System.Collections.Generic.List[string]
foreach ($name in $names) {
    $paths.Add((Join-Path $source $name))
}
$paths.Add((Join-Path $source 'client\SuihuaClientCache.java'))
$paths.Add((Join-Path $PSScriptRoot 'WoodResourceLedgerCheck.java'))
$out = Join-Path $project 'build\wood-base-checks\resources'
New-Item -ItemType Directory -Force -Path $out | Out-Null
& javac --release 17 -encoding UTF-8 -d $out @paths
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
& java -cp $out com.bslx.XueXiMod.status.wood.resource.WoodResourceLedgerCheck $project
exit $LASTEXITCODE
