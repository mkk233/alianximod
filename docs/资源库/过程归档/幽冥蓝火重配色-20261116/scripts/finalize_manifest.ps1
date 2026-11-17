$ErrorActionPreference = 'Stop'
$root = 'D:/alianximod/docs/资源库/过程归档/幽冥蓝火重配色-20261116'
$manifestPath = Join-Path $root 'manifest.json'
$previous = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
$unchangedCount = 0
foreach ($entry in $previous.files) {
    if ($entry.path -match '^(source|textures|references)/' -or $entry.path -match '^previews/(?!bb_)') {
        $actualHash = (Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $root $entry.path)).Hash
        if ($actualHash -ne $entry.sha256) { throw "Existing two-dimensional file changed: $($entry.path)" }
        $unchangedCount++
    }
}
$reopened = Get-Content -LiteralPath (Join-Path $root 'evidence/blockbench-reopen-verification.json') -Raw | ConvertFrom-Json
if (-not $reopened.allProtectedProjectsUnchanged -or -not $reopened.saved) { throw 'Blockbench save/protection verification is incomplete' }
if (@($reopened.textures | Where-Object { $_.error -ne 0 -or -not $_.complete }).Count) { throw 'Blockbench texture load failure' }
$captures = Get-Content -LiteralPath (Join-Path $root 'evidence/blockbench-captures.json') -Raw | ConvertFrom-Json
$previewPaths = @($captures.captures.path) + @($reopened.screenshot)
foreach ($previewPath in $previewPaths) {
    $pngBytes = [System.IO.File]::ReadAllBytes($previewPath)
    $pngWidth = [int64]$pngBytes[16]*16777216 + [int64]$pngBytes[17]*65536 + [int64]$pngBytes[18]*256 + $pngBytes[19]
    $pngHeight = [int64]$pngBytes[20]*16777216 + [int64]$pngBytes[21]*65536 + [int64]$pngBytes[22]*256 + $pngBytes[23]
    if ($pngWidth -ne 1280 -or $pngHeight -ne 640) { throw "Unexpected actual viewport dimensions: $previewPath" }
}
$remap = Get-Content -LiteralPath (Join-Path $root 'evidence/approved-source-remap.json') -Raw | ConvertFrom-Json
foreach ($mapping in $remap.mappings) {
    if ((Get-FileHash -Algorithm SHA256 -LiteralPath $mapping.approvedOriginal).Hash -ne $mapping.approvedOriginalSha256) {
        throw "Approved source changed: $($mapping.approvedOriginal)"
    }
}
$modelRelative = 'blockbench/nether_blue_palette_art_v1.bbmodel'
$modelHash = (Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $root $modelRelative)).Hash
if ($modelHash -ne 'E98E82EFED2B227B3957FDBC757D5AAD69B4A4E61769D78685464FA6FDB522E0') { throw 'Saved model hash changed' }
$files = @(Get-ChildItem -LiteralPath $root -File -Recurse | Where-Object { $_.FullName -ne [System.IO.Path]::GetFullPath($manifestPath) } | Sort-Object FullName | ForEach-Object {
    [ordered]@{
        path = [System.IO.Path]::GetRelativePath($root, $_.FullName).Replace('\','/')
        bytes = $_.Length
        sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $_.FullName).Hash
    }
})
$manifest = [ordered]@{
    version = 'art-v1-20261116'
    task = 'nether-blue-visual'
    attempt = 'art-v1-bb-finish-20261116'
    status = 'complete_candidate_pending_user_confirmation'
    userConfirmation = 'not_received'
    runtimeIntegration = 'not_started_for_blue_candidate'
    writes = 'closed'
    batch = @('领域附魔底座实施-20261029', '火系六式-20261029')
    reviewCounters = @{ design = '2/2'; implementation = '3/3'; addedFormalReview = $false }
    attempts = @(
        @{ id = 'art-v1-20261116'; result = 'partial_blocked_shared_blockbench'; twoDimensionalWorkCompleted = $true },
        @{ id = 'art-v1-bb-finish-20261116'; result = 'complete_candidate_pending_user_confirmation'; existingTwoDimensionalFilesUnchanged = $unchangedCount }
    )
    sharedGuiResolution = $remap.sharedGuiResolution
    approvedOriginalUsageMapping = $remap.mappings
    blockbench = @{
        model = $modelRelative
        sha256 = $modelHash
        environment = 'actual Blockbench Desktop 5.2.2 / plugin 1.10.1'
        originalProject = '803970b1-7113-95b6-9e54-ee1415c7edff'
        reopenedProject = $reopened.reopenedProject
        cubes = 80
        groups = 20
        textures = 18
        savedAndNativelyReopened = $true
        actualPreviewCount = $previewPaths.Count
        actualPreviews = @($previewPaths | ForEach-Object { [System.IO.Path]::GetRelativePath($root, $_).Replace('\','/') })
        allProtectedExternalProjectsUnchanged = $true
        captureEvidence = 'evidence/blockbench-captures.json'
        reopenEvidence = 'evidence/blockbench-reopen-verification.json'
    }
    colorMode = @{ name = 'SOUL_PALETTE_TEXTURE'; vertexRgb = @(255,255,255); applyLegacyMultiplyAgain = $false; preserveAlphaUvTimingAndRuntimeGeometry = $true }
    parentNotification = 'native parent-message interface not available; final receipt is the terminal handoff'
    files = $files
}
$manifest | ConvertTo-Json -Depth 14 | Set-Content -LiteralPath $manifestPath -Encoding utf8
[ordered]@{ status = $manifest.status; indexedFiles = $files.Count; unchangedTwoDimensionalFiles = $unchangedCount; realViewportImages = $previewPaths.Count; modelSha256 = $modelHash; manifestSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $manifestPath).Hash } | ConvertTo-Json -Compress
