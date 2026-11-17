$ErrorActionPreference = 'Stop'
function Get-LocalStamp([datetime]$local) {
  $offset = [TimeZoneInfo]::Local.GetUtcOffset($local)
  $dto = New-Object System.DateTimeOffset $local, $offset
  return $dto.ToString('yyyy-MM-ddTHH:mm:ss.fffzzz')
}
$paths = @(
  'D:\alianximod\build\classes\java\main\com\bslx\XueXiMod\XueXiMod.class',
  'D:\alianximod\build\tmp\compileJava\previous-compilation-data.bin',
  'C:\Users\baishang\AppData\Local\Temp\xueximod-gloom-compile-20261031-232245-eaea72e5\build\classes\java\main\com\bslx\XueXiMod\XueXiMod.class',
  'C:\Users\baishang\AppData\Local\Temp\xueximod-gloom-compile-20261031-232245-eaea72e5\build\tmp\compileJava\compileJava-refmap.json'
)
foreach ($path in $paths) {
  if (-not (Test-Path -LiteralPath $path)) { Write-Output ('missing ' + $path); continue }
  $item = Get-Item -LiteralPath $path
  Write-Output ((Get-LocalStamp $item.LastWriteTime) + ' bytes=' + $item.Length + ' ' + $path)
}
