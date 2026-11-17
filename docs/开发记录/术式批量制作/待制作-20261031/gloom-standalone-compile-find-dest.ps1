$ErrorActionPreference = 'Stop'
$parent = 'C:\Users\baishang\AppData\Local\Temp'
Get-ChildItem -LiteralPath $parent -Directory -Force | Where-Object { $_.Name -like 'xueximod-gloom-compile-20261031-*' } | ForEach-Object {
  Write-Output ($_.FullName + ' mtime=' + $_.LastWriteTime.ToString('yyyy-MM-ddTHH:mm:ss'))
}
$before = Join-Path $PSScriptRoot 'gloom-standalone-compile-src-before.json'
Write-Output ('before_exists=' + (Test-Path -LiteralPath $before))
$procFile = Join-Path $PSScriptRoot 'gloom-standalone-compile-processes.txt'
Write-Output ('processes_exists=' + (Test-Path -LiteralPath $procFile))
