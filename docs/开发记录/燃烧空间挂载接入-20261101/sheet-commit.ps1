$ErrorActionPreference = 'Stop'
$batch = Split-Path -Parent $PSCommandPath
$utf8 = New-Object System.Text.UTF8Encoding $false
$prep = [System.IO.File]::ReadAllText((Join-Path $batch 'sheet-prepare-result.json'), $utf8) | ConvertFrom-Json
$live = [string]$prep.live
$readme = [string]$prep.readme
$candidatePath = [string]$prep.candidate
$readmeCandidate = [string]$prep.readmeCandidate
$expected = [string]$prep.baselineSha256
$expectedCandidate = [string]$prep.candidateSha256
$expectedReadme = [string]$prep.readmeSha256
$expectedReadmeCandidate = [string]$prep.readmeCandidateSha256

function Get-ShaBytes([byte[]]$bytes) {
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        return ([BitConverter]::ToString($sha.ComputeHash($bytes))).Replace('-', '').ToUpperInvariant()
    } finally {
        $sha.Dispose()
    }
}

$outside = (Get-FileHash -LiteralPath $live -Algorithm SHA256).Hash.ToUpperInvariant()
if ($outside -ne $expected) { Write-Output ("ABORT_XLSX_CHANGED " + $outside); exit 2 }
$readmeOutside = (Get-FileHash -LiteralPath $readme -Algorithm SHA256).Hash.ToUpperInvariant()
if ($readmeOutside -ne $expectedReadme) { Write-Output ("ABORT_README_CHANGED " + $readmeOutside); exit 3 }
$candidateBytes = [System.IO.File]::ReadAllBytes($candidatePath)
if ((Get-ShaBytes $candidateBytes) -ne $expectedCandidate) { Write-Output 'ABORT_CANDIDATE_HASH'; exit 4 }
$readmeBytes = [System.IO.File]::ReadAllBytes($readmeCandidate)
if ((Get-ShaBytes $readmeBytes) -ne $expectedReadmeCandidate) { Write-Output 'ABORT_README_CANDIDATE'; exit 5 }

$liveFs = $null
$readmeFs = $null
$buffer = $null
try {
    $liveFs = New-Object System.IO.FileStream($live, [System.IO.FileMode]::Open, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
    $buffer = New-Object byte[] $liveFs.Length
    [void]$liveFs.Read($buffer, 0, $buffer.Length)
    if ((Get-ShaBytes $buffer) -ne $expected) { Write-Output 'ABORT_LOCKED_XLSX'; exit 2 }
    $readmeFs = New-Object System.IO.FileStream($readme, [System.IO.FileMode]::Open, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
    $readmeBuffer = New-Object byte[] $readmeFs.Length
    [void]$readmeFs.Read($readmeBuffer, 0, $readmeBuffer.Length)
    if ((Get-ShaBytes $readmeBuffer) -ne $expectedReadme) { Write-Output 'ABORT_LOCKED_README'; exit 3 }
    $liveFs.SetLength(0)
    $liveFs.Position = 0
    $liveFs.Write($candidateBytes, 0, $candidateBytes.Length)
    $liveFs.Flush($true)
    $readmeFs.SetLength(0)
    $readmeFs.Position = 0
    $readmeFs.Write($readmeBytes, 0, $readmeBytes.Length)
    $readmeFs.Flush($true)
} catch {
    if ($liveFs -ne $null -and $buffer -ne $null) {
        $liveFs.SetLength(0)
        $liveFs.Position = 0
        $liveFs.Write($buffer, 0, $buffer.Length)
        $liveFs.Flush($true)
    }
    Write-Output ("ABORT_LOCK " + $_.Exception.Message)
    exit 6
} finally {
    if ($readmeFs -ne $null) { $readmeFs.Dispose() }
    if ($liveFs -ne $null) { $liveFs.Dispose() }
}

$after = (Get-FileHash -LiteralPath $live -Algorithm SHA256).Hash.ToUpperInvariant()
$afterReadme = (Get-FileHash -LiteralPath $readme -Algorithm SHA256).Hash.ToUpperInvariant()
Write-Output ("xlsx_after=" + $after)
Write-Output ("readme_after=" + $afterReadme)
if ($after -ne $expectedCandidate -or $afterReadme -ne $expectedReadmeCandidate) { Write-Output 'ABORT_POST_HASH'; exit 7 }
exit 0
