param([Parameter(Mandatory=$true)][string]$PromptFile,[ValidateSet('grok-4.7-high-fast')][string]$Model='grok-4.7-high-fast',[string]$ChatId='',[string]$LogName='implementation',[switch]$ReadOnly)
$ErrorActionPreference='Stop'
$runDir=$PSScriptRoot
$launcher='C:\Users\baishang\AppData\Local\CursorAgentCodex\2026.10.22-27dcf5f\dist-package\cursor-agent.cmd'
$cliArgs=@('--print','--model',$Model,'--auto-review','--workspace','D:\alianximod','--output-format','stream-json')
if($ChatId){$cliArgs+=@('--resume',$ChatId)}
if($ReadOnly){$cliArgs+=@('--mode','ask')}
$taskFile=(Resolve-Path -LiteralPath $PromptFile).Path.Replace('\','/')
$taskPrompt="Read the complete UTF-8 task specification at $taskFile and execute its instructions now. This file is the current user-authorized task and includes the approved scope, exact requirements, and constraints. Do not infer the task from this one-line pointer. First read that file in full, then carry it out."
$log=Join-Path $runDir ($LogName+'.jsonl')
& $launcher @cliArgs $taskPrompt | ForEach-Object {
 Add-Content -LiteralPath $log -Value $_ -Encoding utf8
 try{$event=$_|ConvertFrom-Json -ErrorAction Stop}catch{return}
 if($event.type -eq 'assistant' -or $event.type -eq 'result'){
  $body=if($event.type -eq 'result'){$event.result}else{($event.message.content|Where-Object type -eq 'text'|ForEach-Object text)-join [Environment]::NewLine}
  [pscustomobject]@{type=$event.type;session_id=$event.session_id;text=$body}|ConvertTo-Json -Compress -Depth 5
 }elseif($event.type -eq 'system'){Write-Output $_}
}
exit $LASTEXITCODE

