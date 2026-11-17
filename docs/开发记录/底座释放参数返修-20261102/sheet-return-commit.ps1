$ErrorActionPreference = 'Stop'
$batch = Split-Path -Parent $PSCommandPath
$utf8 = New-Object System.Text.UTF8Encoding $false
$prep = [System.IO.File]::ReadAllText((Join-Path $batch 'sheet-return-prepare.json'), $utf8) | ConvertFrom-Json

function Get-ShaBytes([byte[]]$bytes) {
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($sha.ComputeHash($bytes))).Replace('-', '').ToUpperInvariant() }
    finally { $sha.Dispose() }
}

$live = [string]$prep.live
$expected = [string]$prep.baselineSha256
$candidatePath = [string]$prep.candidate
$expectedCandidate = [string]$prep.candidateSha256
$outside = (Get-FileHash -LiteralPath $live -Algorithm SHA256).Hash.ToUpperInvariant()
if ($outside -ne $expected) { Write-Output ("ABORT_XLSX_CHANGED " + $outside); exit 2 }
$candidateBytes = [System.IO.File]::ReadAllBytes($candidatePath)
if ((Get-ShaBytes $candidateBytes) -ne $expectedCandidate) { Write-Output 'ABORT_CANDIDATE_HASH'; exit 4 }

$stream = $null
$buffer = $null
try {
    $stream = New-Object System.IO.FileStream($live, [System.IO.FileMode]::Open, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
    $buffer = New-Object byte[] $stream.Length
    [void]$stream.Read($buffer, 0, $buffer.Length)
    if ((Get-ShaBytes $buffer) -ne $expected) { throw 'LOCKED_HASH' }
    $stream.SetLength(0)
    $stream.Position = 0
    $stream.Write($candidateBytes, 0, $candidateBytes.Length)
    $stream.Flush($true)
} catch {
    if ($stream -ne $null -and $buffer -ne $null) {
        $stream.SetLength(0)
        $stream.Position = 0
        $stream.Write($buffer, 0, $buffer.Length)
        $stream.Flush($true)
    }
    Write-Output ("ABORT_LOCK " + $_.Exception.Message)
    exit 6
} finally { if ($stream -ne $null) { $stream.Dispose() } }

$after = (Get-FileHash -LiteralPath $live -Algorithm SHA256).Hash.ToUpperInvariant()
Write-Output ("after=" + $after)
if ($after -ne $expectedCandidate) { Write-Output 'ABORT_POST_HASH'; exit 7 }
exit 0
