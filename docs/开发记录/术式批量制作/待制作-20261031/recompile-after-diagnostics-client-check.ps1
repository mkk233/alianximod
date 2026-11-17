$ErrorActionPreference = 'Stop'
$ids = 16920, 52040
foreach ($procId in $ids) {
  $proc = Get-CimInstance Win32_Process -Filter "ProcessId=$procId"
  if (-not $proc) {
    Write-Output ("gone pid=" + $procId)
    continue
  }
  $cmd = [string]$proc.CommandLine
  $hasProject = $cmd -match 'alianximod'
  $hasClient = $cmd -match 'forgeclientuserdev|BootstrapLauncher'
  Write-Output ("alive pid=" + $proc.ProcessId + " parent=" + $proc.ParentProcessId + " name=" + $proc.Name + " creation=" + $proc.CreationDate + " project=" + $hasProject + " client=" + $hasClient)
  if ($cmd.Length -gt 420) { $cmd = $cmd.Substring(0, 420) }
  Write-Output $cmd
}
$blocking = 0
Get-CimInstance Win32_Process | ForEach-Object {
  $cmd = [string]$_.CommandLine
  $name = [string]$_.Name
  if ($name -notmatch '^(java|javaw)(\.exe)?$') { return }
  $client = ($cmd -match 'forgeclientuserdev|BootstrapLauncher') -and ($cmd -match 'alianximod')
  $wrapper = $cmd -match 'GradleWrapperMain'
  if ($client -or $wrapper) {
    $blocking++
    Write-Output ("blocking pid=" + $_.ProcessId + " parent=" + $_.ParentProcessId + " name=" + $name)
  }
}
Write-Output ("blocking_now=" + $blocking)
