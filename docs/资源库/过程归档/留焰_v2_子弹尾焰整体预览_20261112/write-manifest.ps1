$ErrorActionPreference = 'Stop'
$directory = [System.IO.Path]::GetFullPath($PSScriptRoot)
$expected = [System.IO.Path]::GetFullPath('D:/alianximod/docs/资源库/过程归档/留焰_v2_子弹尾焰整体预览_20261112')
if ($directory -ne $expected) { throw 'Unexpected output directory' }
$receipt = 'D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/flame-trail-composite-preview-20261112.md'
$inputPaths = @(
    'D:/alianximod/docs/资源库/特效/火焰/留焰_v1/flame_trail.aseprite',
    'D:/alianximod/docs/资源库/特效/火焰/留焰_v1/flame_trail-strip.png',
    'D:/alianximod/docs/资源库/特效/火焰/火苗_飞行火舌_v3/fire_spark_v3.aseprite',
    'D:/alianximod/docs/资源库/特效/火焰/火苗_飞行火舌_v3/fire_spark_v3_strip.png'
)
function Get-ArtifactRecord([string]$FilePath) {
    $file = Get-Item -LiteralPath $FilePath
    [ordered]@{
        path = $file.FullName.Replace('\', '/')
        bytes = $file.Length
        sha256 = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash
    }
}
$previousManifestPath = Join-Path $directory 'manifest.json'
$previousManifest = Get-Content -LiteralPath $previousManifestPath -Raw -Encoding UTF8 | ConvertFrom-Json
$preservedMedia = @($previousManifest.artifacts | Where-Object { $_.path -match '\.(aseprite|png|gif)$' -and [System.IO.Path]::GetFileName($_.path) -notlike 'viewport-*' })
foreach ($record in $preservedMedia) {
    if ((Get-FileHash -LiteralPath $record.path -Algorithm SHA256).Hash -ne $record.sha256) { throw ('Existing 2D media changed: ' + $record.path) }
}
foreach ($record in $previousManifest.inputSourcesReadOnly) {
    if ((Get-FileHash -LiteralPath $record.path -Algorithm SHA256).Hash -ne $record.sha256) { throw ('Read-only source changed: ' + $record.path) }
}
$captureLog = Get-Content -LiteralPath (Join-Path $directory 'blockbench-capture-log.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$modelPath = Join-Path $directory 'flame-trail-v2-projectile-composite.bbmodel'
$savedModel = Get-Content -LiteralPath $modelPath -Raw -Encoding UTF8 | ConvertFrom-Json
if ($captureLog.projectId -ne '8a58e270-0fce-3a38-f8c5-528d850c4a39' -or $captureLog.captures.Count -ne 9) { throw 'Unexpected viewport evidence' }
if ($savedModel.elements.Count -ne 109 -or $savedModel.textures.Count -ne 98) { throw 'Unexpected saved model content' }
$manifest = [ordered]@{
    batchId = '待制作-20261031'
    taskId = 'flame_trail_art_preview'
    attemptId = '20261112-projectile-composite-3d-resume'
    twoDSourceAttemptId = '20261112-projectile-composite-feedback'
    version = '留焰_v2_子弹尾焰整体预览_20261112'
    state = '同版二维整体效果与真实三维草稿预览齐全；待本次用户视觉确认'
    designReviewRounds = '2/2 unchanged'
    implementationReviewRounds = '3/3 unchanged'
    asepriteBackend = 'native batch, not GUI'
    twoDPreservation = [ordered]@{
        comparedAgainstPreviousManifest = $true
        unchangedMediaCount = $preservedMedia.Count
        allHashesUnchanged = $true
        originalInputHashesUnchanged = $true
        twoDRegeneratedInThisAttempt = $false
    }
    inputSourcesReadOnly = @($inputPaths | ForEach-Object { Get-ArtifactRecord $_ })
    artifacts = @(Get-ChildItem -LiteralPath $directory -File | Where-Object { $_.Name -ne 'manifest.json' } | Sort-Object Name | ForEach-Object { Get-ArtifactRecord $_.FullName })
    receipt = Get-ArtifactRecord $receipt
    blockbench = [ordered]@{
        applicable = $true
        realAppConnected = $true
        version = '5.2.2'
        pluginVersion = '1.10.1'
        reusedProjectUuid = '8a58e270-0fce-3a38-f8c5-528d850c4a39'
        observedElementCount = 109
        observedGroupCount = 4
        observedTextureCount = 98
        observedAnimationCount = 0
        preparedBuildScriptExecuted = $true
        editableModel = Get-ArtifactRecord $modelPath
        savedPhaseTick = 40
        actualViewportImages = @($captureLog.captures | ForEach-Object { Get-ArtifactRecord $_.path })
        viewport = 'real connected main viewport; explicit render then canvas PNG; no offscreen or headless'
        documentVisibilityState = $captureLog.visibilityState
        blocker = $null
        previousBlockerResolved = 'Exact-name discovery once, then successful read-only and production risky_eval calls'
        otherProjectIdsPreserved = @('fe1b1c81-3962-2fbb-bc94-6a37e1b6e5ef', 'e380502f-05e4-5cab-c7a2-4ff5f7e3c7c5')
    }
    userConfirmed = $false
    runtimeIntegrated = $false
    compiled = $false
    gameTested = $false
    subagentsCreated = 0
    writesConverged = $true
    selfHashExcluded = $true
}
$manifestPath = Join-Path $directory 'manifest.json'
$serialized = $manifest | ConvertTo-Json -Depth 12
[System.IO.File]::WriteAllText($manifestPath, $serialized, [System.Text.UTF8Encoding]::new($false))
[ordered]@{
    manifest = $manifestPath
    artifactCount = $manifest.artifacts.Count
    sha256 = (Get-FileHash -LiteralPath $manifestPath -Algorithm SHA256).Hash
    state = $manifest.state
} | ConvertTo-Json
