$ErrorActionPreference = 'Stop'
$root = 'D:\alianximod'
$batch = Join-Path $root 'docs\开发记录\分身底座-20261102'
$beforeApply = Join-Path $batch 'before-apply'
$logPath = Join-Path $batch '_apply-byte-log.json'
$problems = New-Object System.Collections.Generic.List[string]

function Sha([string]$path) {
    return (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
}

function Stamp([string]$path) {
    $item = Get-Item -LiteralPath $path
    return $item.LastWriteTime.ToString('yyyy-MM-ddTHH:mm:ss.fff')
}

$gradle = @()
try {
    $gradle = @(Get-CimInstance Win32_Process | Where-Object {
        $commandLine = [string]$_.CommandLine
        $commandLine -match 'GradleWrapperMain|gradlew'
    })
} catch {
    $problems.Add('gradle_probe_failed: ' + $_.Exception.Message)
}
if ($gradle.Count -gt 0) {
    $problems.Add('gradle_busy count=' + $gradle.Count)
}

$manifest = Get-Content -LiteralPath (Join-Path $batch 'integration-apply-manifest.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$evidenceA = Get-Content -LiteralPath (Join-Path $batch 'author-A-core-evidence.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$evidenceB = Get-Content -LiteralPath (Join-Path $batch 'author-B-action-evidence.json') -Raw -Encoding UTF8 | ConvertFrom-Json

$records = New-Object System.Collections.Generic.List[object]

foreach ($entry in $manifest.entries) {
    $targetPath = Join-Path $root ($entry.target -replace '/', '\')
    $candidatePath = Join-Path $root ($entry.candidate -replace '/', '\')
    if (-not (Test-Path -LiteralPath $candidatePath)) {
        $problems.Add('missing_candidate ' + $entry.target)
        continue
    }
    $candidateHash = Sha $candidatePath
    if ($candidateHash -ne $entry.candidateSha256) {
        $problems.Add('candidate_hash_mismatch ' + $entry.target + ' actual=' + $candidateHash)
    }
    $exists = Test-Path -LiteralPath $targetPath
    if ($entry.kind -eq 'modify') {
        if (-not $exists) {
            $problems.Add('modify_target_missing ' + $entry.target)
            continue
        }
        $currentHash = Sha $targetPath
        if ($currentHash -ne $entry.originalSha256) {
            $problems.Add('target_changed ' + $entry.target + ' current=' + $currentHash)
        }
        $records.Add([ordered]@{
            target = $entry.target
            kind = 'modify'
            owner = 'C'
            beforeSha256 = $currentHash
            beforeMtime = (Stamp $targetPath)
            candidateSha256 = $candidateHash
        })
    } else {
        if ($exists) {
            $currentHash = Sha $targetPath
            if ($currentHash -ne $candidateHash) {
                $problems.Add('new_target_exists_different ' + $entry.target + ' current=' + $currentHash)
            }
            $records.Add([ordered]@{
                target = $entry.target
                kind = 'new'
                owner = 'C'
                beforeSha256 = $currentHash
                beforeMtime = (Stamp $targetPath)
                candidateSha256 = $candidateHash
            })
        } else {
            $records.Add([ordered]@{
                target = $entry.target
                kind = 'new'
                owner = 'C'
                beforeSha256 = 'missing'
                beforeMtime = ''
                candidateSha256 = $candidateHash
            })
        }
    }
}

$authorFiles = @()
foreach ($file in $evidenceA.files) {
    if ($file.path -like '*.java') { $authorFiles += [pscustomobject]@{ owner = 'A'; path = $file.path; sha256 = $file.sha256.ToLowerInvariant() } }
}
foreach ($file in $evidenceB.files) {
    if ($file.path -like '*.java') { $authorFiles += [pscustomobject]@{ owner = 'B'; path = $file.path; sha256 = $file.sha256.ToLowerInvariant() } }
}

foreach ($file in $authorFiles) {
    $candidatePath = if ([System.IO.Path]::IsPathRooted($file.path)) { $file.path } else { Join-Path $root ($file.path -replace '/', '\') }
    $relative = $candidatePath.Substring($root.Length).TrimStart('\')
    $targetRelative = $relative.Substring('docs\开发记录\分身底座-20261102\候选源码\'.Length)
    $targetPath = Join-Path $root $targetRelative
    if (-not (Test-Path -LiteralPath $candidatePath)) {
        $problems.Add('missing_author_candidate ' + $relative)
        continue
    }
    $candidateHash = Sha $candidatePath
    if ($candidateHash -ne $file.sha256) {
        $problems.Add('author_hash_mismatch ' + $targetRelative + ' actual=' + $candidateHash + ' expected=' + $file.sha256)
    }
    if (Test-Path -LiteralPath $targetPath) {
        $currentHash = Sha $targetPath
        if ($currentHash -ne $candidateHash) {
            $problems.Add('author_target_exists_different ' + $targetRelative + ' current=' + $currentHash)
        }
        $records.Add([ordered]@{
            target = ($targetRelative -replace '\\', '/')
            kind = 'new'
            owner = $file.owner
            beforeSha256 = $currentHash
            beforeMtime = (Stamp $targetPath)
            candidateSha256 = $candidateHash
        })
    } else {
        $records.Add([ordered]@{
            target = ($targetRelative -replace '\\', '/')
            kind = 'new'
            owner = $file.owner
            beforeSha256 = 'missing'
            beforeMtime = ''
            candidateSha256 = $candidateHash
        })
    }
}

if ($problems.Count -gt 0) {
    [ordered]@{ status = 'blocked'; problems = @($problems); records = @($records) } | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $logPath -Encoding UTF8
    Write-Output ('BLOCKED ' + $problems.Count)
    $problems | ForEach-Object { Write-Output $_ }
    exit 3
}

New-Item -ItemType Directory -Force -Path $beforeApply | Out-Null
foreach ($record in $records) {
    if ($record.beforeSha256 -eq 'missing') { continue }
    $sourcePath = Join-Path $root ($record.target -replace '/', '\')
    $destPath = Join-Path $beforeApply ($record.target -replace '/', '\')
    $destDir = Split-Path -Parent $destPath
    New-Item -ItemType Directory -Force -Path $destDir | Out-Null
    Copy-Item -LiteralPath $sourcePath -Destination $destPath -Force
}

foreach ($record in $records) {
    $targetPath = Join-Path $root ($record.target -replace '/', '\')
    $candidateRelative = 'docs/开发记录/分身底座-20261102/候选源码/' + $record.target
    $candidatePath = Join-Path $root ($candidateRelative -replace '/', '\')
    $targetDir = Split-Path -Parent $targetPath
    New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
    Copy-Item -LiteralPath $candidatePath -Destination $targetPath -Force
    $afterHash = Sha $targetPath
    if ($afterHash -ne $record.candidateSha256) {
        $problems.Add('copy_hash_mismatch ' + $record.target + ' actual=' + $afterHash)
    }
    $record['afterSha256'] = $afterHash
    $record['afterMtime'] = Stamp $targetPath
}

[ordered]@{ status = $(if ($problems.Count -gt 0) { 'copy_mismatch' } else { 'copied' }); problems = @($problems); records = @($records) } | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $logPath -Encoding UTF8
if ($problems.Count -gt 0) {
    Write-Output ('COPY_MISMATCH ' + $problems.Count)
    exit 4
}
Write-Output ('COPIED ' + $records.Count)
exit 0
