param([Parameter(Mandatory=$true)][string]$PromptFile,[Parameter(Mandatory=$true)][string]$LogName,[Parameter(Mandatory=$true)][ValidateSet('grok-4.7-high-fast')][string]$Model,[string]$ChatId='',[switch]$WriteMode)
$ErrorActionPreference='Stop'
$runDir=$PSScriptRoot
$launcher='C:\Users\baishang\AppData\Local\CursorAgentCodex\2026.10.22-27dcf5f\dist-package\cursor-agent.cmd'
$cliArgs=@('--print','--model',$Model,'--auto-review','--workspace','D:\alianximod','--output-format','stream-json')
if(-not $WriteMode){$cliArgs+=@('--mode','ask')}
if($ChatId){$cliArgs+=@('--resume',$ChatId)}
$taskFile=(Resolve-Path -LiteralPath $PromptFile).Path.Replace('\','/')
$taskPrompt="Read the complete UTF-8 task specification at $taskFile and follow that authorized scope. Return the complete final report."
$previousErrorPreference=$ErrorActionPreference
try {
  $ErrorActionPreference='Continue'
  & $launcher @cliArgs $taskPrompt 1> (Join-Path $runDir ($LogName+'.jsonl')) 2> (Join-Path $runDir ($LogName+'.stderr.log'))
  $cursorExit=$LASTEXITCODE
} finally { $ErrorActionPreference=$previousErrorPreference }
Set-Content -LiteralPath (Join-Path $runDir ($LogName+'-exit.txt')) -Value $cursorExit -Encoding utf8
$finalEvent=Get-Content -LiteralPath (Join-Path $runDir ($LogName+'.jsonl')) | ForEach-Object {try{$event=$_ | ConvertFrom-Json -ErrorAction Stop;if($event.type -eq 'result'){$event}}catch{}} | Select-Object -Last 1
if($finalEvent){$finalEvent.result | Set-Content -LiteralPath (Join-Path $runDir ($LogName+'-final.md')) -Encoding utf8;$finalEvent | ConvertTo-Json -Depth 12 -Compress}else{Write-Output ('No final result; exit '+$cursorExit);Get-Content -LiteralPath (Join-Path $runDir ($LogName+'.stderr.log')) -Tail 25}
exit $cursorExit
