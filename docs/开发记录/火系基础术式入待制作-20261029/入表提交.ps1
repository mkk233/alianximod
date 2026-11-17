$ErrorActionPreference = "Stop"
$live = "D:\alianximod\docs\术式制作表.xlsx"
$candidate = "D:\alianximod\docs\开发记录\火系基础术式入待制作-20261029\入表-candidate-preserved.xlsx"
$resultPath = "D:\alianximod\docs\开发记录\火系基础术式入待制作-20261029\入表-submit.json"
$expected = "50877EAF6C0D34DAAA49465DE447F8600073BE19348D3C90B96B2F0A1D4985E7"
$sha = [System.Security.Cryptography.SHA256]::Create()
$stream = $null
try {
    $stream = [System.IO.File]::Open($live, [System.IO.FileMode]::Open, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
    $locked = New-Object byte[] $stream.Length
    $offset = 0
    while ($offset -lt $locked.Length) {
        $count = $stream.Read($locked, $offset, $locked.Length - $offset)
        if ($count -le 0) { throw "Locked read ended early" }
        $offset += $count
    }
    $hash = ([System.BitConverter]::ToString($sha.ComputeHash($locked))).Replace("-", "")
    if ($hash -ne $expected) { throw "Live hash drifted: $hash" }
    $payload = [System.IO.File]::ReadAllBytes($candidate)
    $stream.SetLength(0)
    $stream.Write($payload, 0, $payload.Length)
    $stream.Flush()
    $submitted = $true
    $errorText = ""
} catch {
    $submitted = $false
    $errorText = $_.Exception.Message
} finally {
    if ($stream -ne $null) { $stream.Dispose() }
}
if ($submitted) {
    $file = Get-Item -LiteralPath $live
    $finalHash = (Get-FileHash -LiteralPath $live -Algorithm SHA256).Hash
    $result = [ordered]@{
        submitted = $true
        sha256 = $finalHash
        bytes = $file.Length
        mtime = $file.LastWriteTime.ToString("yyyy-MM-dd HH:mm:ss.fff")
    }
} else {
    $result = [ordered]@{ submitted = $false; error = $errorText }
}
$result | ConvertTo-Json | Set-Content -LiteralPath $resultPath -Encoding UTF8
if (-not $submitted) { exit 2 }
