$ErrorActionPreference = 'Stop'
$batch = Split-Path -Parent $PSCommandPath
$utf8 = New-Object System.Text.UTF8Encoding $false
$prep = [System.IO.File]::ReadAllText((Join-Path $batch 'sheet-prepare-result.json'), $utf8) | ConvertFrom-Json

function Get-ShaBytes([byte[]]$bytes) {
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        return ([BitConverter]::ToString($sha.ComputeHash($bytes))).Replace('-', '').ToUpperInvariant()
    } finally {
        $sha.Dispose()
    }
}

function Read-Checked([string]$path, [string]$expected) {
    $bytes = [System.IO.File]::ReadAllBytes($path)
    if ((Get-ShaBytes $bytes) -ne $expected) { throw "HASH $path" }
    return $bytes
}

$pairs = @(
    @{ Path = [string]$prep.live; Expected = [string]$prep.baselineSha256; Candidate = [string]$prep.candidate; CandidateExpected = [string]$prep.candidateSha256 },
    @{ Path = [string]$prep.readme; Expected = [string]$prep.readmeSha256; Candidate = [string]$prep.readmeCandidate; CandidateExpected = [string]$prep.readmeCandidateSha256 },
    @{ Path = [string]$prep.v1Readme; Expected = [string]$prep.v1ReadmeSha256; Candidate = [string]$prep.v1Candidate; CandidateExpected = [string]$prep.v1CandidateSha256 },
    @{ Path = [string]$prep.v2Readme; Expected = [string]$prep.v2ReadmeSha256; Candidate = [string]$prep.v2ReadmeCandidate; CandidateExpected = [string]$prep.v2ReadmeCandidateSha256 },
    @{ Path = [string]$prep.v2Manifest; Expected = [string]$prep.v2ManifestSha256; Candidate = [string]$prep.v2ManifestCandidate; CandidateExpected = [string]$prep.v2ManifestCandidateSha256 }
)

foreach ($pair in $pairs) {
    $outside = (Get-FileHash -LiteralPath $pair.Path -Algorithm SHA256).Hash.ToUpperInvariant()
    if ($outside -ne $pair.Expected) { Write-Output ("ABORT_CHANGED " + $pair.Path); exit 2 }
    $candidateBytes = Read-Checked $pair.Candidate $pair.CandidateExpected
    $pair.CandidateBytes = $candidateBytes
}

$streams = @()
$originals = @()
try {
    foreach ($pair in $pairs) {
        $stream = New-Object System.IO.FileStream($pair.Path, [System.IO.FileMode]::Open, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
        $streams += $stream
        $buffer = New-Object byte[] $stream.Length
        [void]$stream.Read($buffer, 0, $buffer.Length)
        if ((Get-ShaBytes $buffer) -ne $pair.Expected) { throw "LOCKED_HASH" }
        $originals += $buffer
    }
    for ($index = 0; $index -lt $pairs.Count; $index++) {
        $streams[$index].SetLength(0)
        $streams[$index].Position = 0
        $payload = $pairs[$index].CandidateBytes
        $streams[$index].Write($payload, 0, $payload.Length)
        $streams[$index].Flush($true)
    }
} catch {
    for ($index = 0; $index -lt $originals.Count; $index++) {
        $streams[$index].SetLength(0)
        $streams[$index].Position = 0
        $streams[$index].Write($originals[$index], 0, $originals[$index].Length)
        $streams[$index].Flush($true)
    }
    Write-Output ("ABORT_LOCK " + $_.Exception.Message)
    exit 6
} finally {
    foreach ($stream in $streams) { $stream.Dispose() }
}

for ($index = 0; $index -lt $pairs.Count; $index++) {
    $after = (Get-FileHash -LiteralPath $pairs[$index].Path -Algorithm SHA256).Hash.ToUpperInvariant()
    Write-Output ("after" + $index + "=" + $after)
    if ($after -ne $pairs[$index].CandidateExpected) { Write-Output 'ABORT_POST_HASH'; exit 7 }
}
exit 0
