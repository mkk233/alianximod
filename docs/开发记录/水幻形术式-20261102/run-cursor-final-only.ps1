param([Parameter(Mandatory=$true)][string]$PromptFile,[ValidateSet('grok-4.7-high-fast')][string]$Model='grok-4.7-high-fast',[string]$ChatId='',[string]$LogName='implementation',[switch]$ReadOnly)
$ErrorActionPreference='Stop'
$launcher='C:\Users\baishang\AppData\Local\CursorAgentCodex\2026.10.22-27dcf5f\dist-package\cursor-agent.cmd'
$cliArgs=@('--print','--model',$Model,'--auto-review','--workspace','D:\alianximod','--output-format','stream-json')
if($ChatId){$cliArgs+=@('--resume',$ChatId)}
if($ReadOnly){$cliArgs+=@('--mode','ask')}
$taskFile=(Resolve-Path -LiteralPath $PromptFile).Path.Replace('\','/')
$taskPrompt="Read the complete UTF-8 task specification at $taskFile and execute it now. This is the current user-authorized scope. Read it fully first."
$statePath=Join-Path $PSScriptRoot ($LogName+'-state.json')
$finalPath=Join-Path $PSScriptRoot ($LogName+'-final.json')
@{status='running';model=$Model;startedAt=(Get-Date).ToString('o');pid=$PID}|ConvertTo-Json|Set-Content -LiteralPath $statePath -Encoding utf8
& $launcher @cliArgs $taskPrompt 2>&1 | ForEach-Object {
 $line=$_.ToString()
 try{$event=$line|ConvertFrom-Json -ErrorAction Stop}catch{return}
 if($event.type -eq 'result'){
  $line|Set-Content -LiteralPath $finalPath -Encoding utf8
  $event.result|Set-Content -LiteralPath (Join-Path $PSScriptRoot ($LogName+'-final.md')) -Encoding utf8
  [pscustomobject]@{type='result';session_id=$event.session_id;is_error=$event.is_error;text=$event.result}|ConvertTo-Json -Compress -Depth 8
 }elseif($event.type -eq 'system' -and $event.subtype -eq 'init'){
  [pscustomobject]@{status='running';model=$event.model;session_id=$event.session_id;pid=$PID}|ConvertTo-Json|Set-Content -LiteralPath $statePath -Encoding utf8
  [pscustomobject]@{type='init';model=$event.model;session_id=$event.session_id}|ConvertTo-Json -Compress
 }
}
$cliExit=$LASTEXITCODE
@{status=if($cliExit -eq 0){'completed'}else{'failed'};exitCode=$cliExit;endedAt=(Get-Date).ToString('o')}|ConvertTo-Json|Set-Content -LiteralPath $statePath -Encoding utf8
Write-Output "Cursor exit code: $cliExit"
exit $cliExit
