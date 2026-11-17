param(
    [Parameter(Mandatory = $true)][string]$Live,
    [Parameter(Mandatory = $true)][string]$Candidate,
    [Parameter(Mandatory = $true)][string]$Backup,
    [Parameter(Mandatory = $true)][string]$ResultPath,
    [Parameter(Mandatory = $true)][string]$Expected
)
$ErrorActionPreference = "Stop"
$sha = [System.Security.Cryptography.SHA256]::Create()
$stream = $null
$submitted = $false
$errorText = ""
try {
    $stream = [System.IO.File]::Open($Live, [System.IO.FileMode]::Open, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
    $locked = New-Object byte[] $stream.Length
    $offset = 0
    while ($offset -lt $locked.Length) {
        $count = $stream.Read($locked, $offset, $locked.Length - $offset)
        if ($count -le 0) { throw "Locked read ended early" }
        $offset += $count
    }
    $hash = ([System.BitConverter]::ToString($sha.ComputeHash($locked))).Replace("-", "")
    if ($hash -ne $Expected) { throw "Live hash drifted: $hash" }
    [System.IO.File]::WriteAllBytes($Backup, $locked)
    $payload = [System.IO.File]::ReadAllBytes($Candidate)
    $stream.SetLength(0)
    $stream.Write($payload, 0, $payload.Length)
    $stream.Flush()
    $submitted = $true
} catch {
    $errorText = $_.Exception.Message
} finally {
    if ($null -ne $stream) { $stream.Dispose() }
}
if ($submitted) {
    $file = Get-Item -LiteralPath $Live
    $finalHash = (Get-FileHash -LiteralPath $Live -Algorithm SHA256).Hash
    $result = [ordered]@{
        submitted = $true
        sha256 = $finalHash
        bytes = $file.Length
        mtime = $file.LastWriteTime.ToString("yyyy-MM-dd HH:mm:ss.fff")
        backup = $Backup
    }
} else {
    $result = [ordered]@{ submitted = $false; error = $errorText; backup = $Backup }
}
$result | ConvertTo-Json | Set-Content -LiteralPath $ResultPath -Encoding UTF8
if (-not $submitted) { exit 2 }
