$ErrorActionPreference = 'Stop'
function Get-LocalStamp([datetime]$local) {
  $offset = [TimeZoneInfo]::Local.GetUtcOffset($local)
  $dto = New-Object System.DateTimeOffset $local, $offset
  return $dto.ToString('yyyy-MM-ddTHH:mm:ss.fffzzz')
}
$paths = @(
  'D:\alianximod\src\main\java\com\bslx\XueXiMod\command\ModCommands.java',
  'D:\alianximod\src\main\java\com\bslx\XueXiMod\command\ShuCommands.java'
)
foreach ($path in $paths) {
  $item = Get-Item -LiteralPath $path
  $hash = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
  Write-Output ((Get-LocalStamp $item.LastWriteTime) + ' bytes=' + $item.Length + ' sha=' + $hash + ' ' + $path)
}
Start-Sleep -Seconds 2
Write-Output 'recheck'
foreach ($path in $paths) {
  $item = Get-Item -LiteralPath $path
  $hash = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
  Write-Output ((Get-LocalStamp $item.LastWriteTime) + ' bytes=' + $item.Length + ' sha=' + $hash + ' ' + $path)
}
