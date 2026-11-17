$ErrorActionPreference = 'Stop'
$root = 'D:/alianximod/docs/资源库/过程归档/幽冥蓝火重配色-20261116'
$coverage = "$root/coverage"
$runtime = 'D:/alianximod/src/main/resources/assets/xueximod/textures/effect'
$expectedBaseline = 'FEB994B510E31B8AB63FB916DA1CF3A922DE59FA9BD6E496B558B7A56CEF0170'
function Get-EvidenceFile([string]$Path) {
    $item = Get-Item -LiteralPath $Path
    [ordered]@{ path = $item.FullName.Replace('\','/'); bytes = $item.Length; sha256 = (Get-FileHash -LiteralPath $item.FullName -Algorithm SHA256).Hash }
}
function Write-EvidenceJson([string]$RelativePath, $Value) {
    $target = [IO.Path]::GetFullPath("$coverage/$RelativePath")
    $allowed = [IO.Path]::GetFullPath($coverage).TrimEnd('\') + '\'
    if (-not $target.StartsWith($allowed, [StringComparison]::OrdinalIgnoreCase)) { throw 'Write outside coverage rejected' }
    $Value | ConvertTo-Json -Depth 40 | Set-Content -LiteralPath $target -Encoding utf8
}
$baselineManifest = Get-EvidenceFile "$root/manifest.json"
if ($baselineManifest.sha256 -ne $expectedBaseline) { throw 'Original v1 manifest changed; do not overwrite' }
$baseline = Get-Content -LiteralPath "$root/manifest.json" -Raw | ConvertFrom-Json
$baselineResults = @($baseline.files | ForEach-Object {
    $actual = Get-EvidenceFile "$root/$($_.path)"
    [ordered]@{ path = $_.path; sha256 = $actual.sha256; unchanged = ($actual.sha256 -eq $_.sha256 -and $actual.bytes -eq $_.bytes) }
})
if ($baselineResults.Count -ne 122 -or @($baselineResults | Where-Object { -not $_.unchanged }).Count) { throw 'Original v1 files changed; do not overwrite' }
Write-EvidenceJson 'evidence/baseline-preservation.json' ([ordered]@{
    task = 'nether-blue-visual'; attempt = 'art-coverage-02'; manifest = $baselineManifest
    expectedManifestSha256 = $expectedBaseline; manifestUnchanged = $true
    fileCount = $baselineResults.Count; allUnchanged = $true; files = $baselineResults
})
$production = Get-Content -LiteralPath "$coverage/evidence/aseprite-production.json" -Raw | ConvertFrom-Json
$verification = Get-Content -LiteralPath "$coverage/evidence/aseprite-verification.json" -Raw | ConvertFrom-Json
$blockbench = Get-Content -LiteralPath "$coverage/evidence/blockbench-reopen-verification.json" -Raw | ConvertFrom-Json
if ($production.entries.Count -ne 45 -or $production.excluded.Count -ne 8 -or $production.reusedNative.Count -ne 3) { throw 'Unexpected coverage counts' }
if ($verification.alphaDifferences -ne 0 -or $verification.silhouetteDifferences -ne 0 -or $verification.sourceExportDifferences -ne 0 -or $verification.currentInputDifferences -ne 0) { throw 'Aseprite verification failed' }
if (@($verification.exclusions | Where-Object { $_.pixelDifferences -ne 0 }).Count -gt 0) { throw 'Preservation verification failed' }
if (-not $blockbench.nativeReopen -or -not $blockbench.allProtectedUnchanged -or $blockbench.textureErrors.Count -ne 0 -or $blockbench.missingFaceTextures.Count -ne 0) { throw 'Blockbench verification failed' }
$entries = @($production.entries | ForEach-Object {
    [ordered]@{
        id = $_.id; runtimeInput = Get-EvidenceFile "$runtime/$($_.file)"
        width = $_.width; height = $_.frameHeight * $_.frames; frameHeight = $_.frameHeight; frames = $_.frames; previewMs = $_.ms
        blue = Get-EvidenceFile "$coverage/textures/$($_.id)_blue.png"
        editable = Get-EvidenceFile "$coverage/source/$($_.id)_blue.aseprite"
        original = Get-EvidenceFile "$coverage/references/$($_.id)_original.png"
        legacyComparator = Get-EvidenceFile "$coverage/textures/$($_.id)_legacy.png"
        alphaDifferences = $_.alphaDifferences; silhouetteDifferences = $_.silhouetteDifferences
        changedVisiblePixels = $_.changedVisiblePixels; preservedVisiblePixels = $_.preservedVisiblePixels
    }
})
$exclusions = @($production.excluded | ForEach-Object {
    [ordered]@{ id = $_.id; input = Get-EvidenceFile "$runtime/$($_.file)"; reference = Get-EvidenceFile "$coverage/references/preserve_$($_.id).png"; reason = $_.reason; pixelDifferences = 0 }
})
$nativeReuse = @($production.reusedNative | ForEach-Object {
    [ordered]@{ id = $_.id; resource = $_.resource; original = Get-EvidenceFile "$coverage/references/$($_.original)"; nativeSoul = Get-EvidenceFile "$coverage/references/$($_.blue)"; sameAlphaRecolor = $false; note = $_.note }
})
$model = Get-EvidenceFile "$coverage/blockbench/nether_blue_coverage_02.bbmodel"
if ($model.sha256 -ne '6711CA4889F014C2A92004E81C3D73158DC5074ADF0A628EACD5B8B89C537E97') { throw 'Saved model differs from reopened model' }
$screenshots = @(Get-ChildItem -LiteralPath "$coverage/previews" -File -Filter 'bb_*.png' | Sort-Object Name | ForEach-Object { Get-EvidenceFile $_.FullName })
if ($screenshots.Count -ne 8) { throw 'Expected eight actual BB screenshots' }
$approvedSources = @(
    'D:/alianximod/docs/资源库/特效/火焰/升温_贴身炽热_v3.1/spatial/heat_up_v3_1_spatial.bbmodel',
    'D:/alianximod/docs/资源库/特效/火焰/燃烧_空间挂载_v1/燃烧v3_空间挂载示意_v1.bbmodel',
    'D:/alianximod/docs/资源库/特效/火焰/燎里_剑阵姿态_v7/liaoli-dive-leg-array-forward.bbmodel'
) | ForEach-Object { Get-EvidenceFile $_ }
$latestUsage = @(
    [ordered]@{ previousCandidateId = 'fentian_conflagration'; currentUse = '引燃_承接焚天_v4'; input = Get-EvidenceFile 'D:/alianximod/docs/资源库/特效/火焰/引燃_承接焚天_v4/kindle_v4-strip.png' },
    [ordered]@{ previousCandidateId = 'kindle_v3_screenshot'; currentUse = '焚天_全身烈焰_v3'; input = Get-EvidenceFile 'D:/alianximod/docs/资源库/特效/火焰/焚天_全身烈焰_v3/fentian_v3-strip.png' }
)
$files = @(Get-ChildItem -LiteralPath $coverage -Recurse -File | Where-Object { $_.FullName -ne [IO.Path]::GetFullPath("$coverage/manifest.json") } | Sort-Object FullName | ForEach-Object {
    $record = Get-EvidenceFile $_.FullName
    $record.path = $record.path.Substring($coverage.Length + 1)
    $record
})
Write-EvidenceJson 'manifest.json' ([ordered]@{
    version = 1; date = '2026-11-16'; task = 'nether-blue-visual'; attempt = 'art-coverage-02'
    status = 'complete_candidate_pending_user_confirmation'; userConfirmation = $false; runtimeIntegration = $false
    parentThread = '01a1c9b3-ca26-7a33-91d2-71c2e32bb69b'
    writeRoot = $coverage; writesClosed = $true; guiWorkStopped = $true; subagents = @()
    batches = @('领域附魔底座实施-20261029','火系六式-20261029'); reviewCounters = @{ design = '2/2'; implementation = '3/3'; unchanged = $true; additionalReview = $false }
    fallback = @{ authorRole = 'Astra max art fallback'; reason = 'Parent reported actual Carbon and Argon Unknown model failures'; reprobed = $false }
    flashFacts = @{ received = $true; parentConfirmedFinalAndClosed = $true; agentId = $null; idNote = 'Not supplied in received final facts; not fabricated' }
    counts = @{ newBlueAtlases = 45; editableBlueSources = 45; exclusions = 8; nativeSoulReuse = 3; animatedComparisons = 5; actualBlockbenchScreenshots = 8; existingV1FilesPreserved = 122 }
    tools = @{ aseprite = 'Official magpie batch Lua; Aseprite 1.3.17'; liveConnected = $false; blockbench = $blockbench.application; nativeSaveAndReopen = $true }
    colorMode = @{ proposal = 'SOUL_PALETTE_TEXTURE'; palette = 'unchanged v1 piecewise luminance palette'; vertexRgb = @(255,255,255); multiplyBlueAgain = $false; legacyRgb = @(90,160,255); handoff = 'visual-handoff.md' }
    inputNetherBaseline = Get-EvidenceFile "$runtime/nether_gloom_v2/nether_gloom.png"
    overview = 'previews/coverage_overview.png'; receipt = 'receipt.md'; handoff = 'visual-handoff.md'
    blockbench = @{ model = $model; nativeReopenEvidence = 'evidence/blockbench-reopen-verification.json'; screenshots = $screenshots; sourceModels = @($approvedSources); preexistingProjectsUnchanged = 8; elements = 438; meshes = 177; textures = 128 }
    verification = @{ alphaDifferences = 0; silhouetteDifferences = 0; sourceExportDifferences = 0; currentInputDifferences = 0; excludedPixelDifferences = 0; baselinePreserved = $true }
    exclusions = $exclusions; nativeReuse = $nativeReuse; entries = $entries; approvedOriginalUsageMapping = $latestUsage
    limitations = @('BB is actual desktop spatial art preview, not in-game verification','Thrust side F10 is a texture keyframe on a standing sword mount, not a diving pose','Native soul reuse retains native shapes/alpha rather than matching ordinary fire','Legacy comparator does not assert every added runtime path previously applied that tint','No Java/runtime resources/workbooks/shared indexes/compile/game/review/integration performed')
    files = $files
})
[ordered]@{ status = 'sealed'; blueAtlases = $entries.Count; preservedV1Files = $baselineResults.Count; screenshotCount = $screenshots.Count; inventoryCount = $files.Count; modelSha256 = $model.sha256; manifest = "$coverage/manifest.json" } | ConvertTo-Json
