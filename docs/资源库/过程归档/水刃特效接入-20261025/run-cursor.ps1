param([string]$Prompt,[string]$ChatId='',[string]$LogName='implementation',[switch]$ReadOnly)
$runDir='D:\alianximod\docs\资源库\过程归档\水刃特效接入-20261025'
$launcher='C:\Users\baishang\AppData\Local\CursorAgentCodex\2026.10.22-27dcf5f\dist-package\cursor-agent.cmd'
$cliArgs=@('--print','--model','grok-4.7-high-fast','--auto-review','--workspace','D:\alianximod','--output-format','stream-json')
if($ChatId){$cliArgs+=@('--resume',$ChatId)}
if($ReadOnly){$cliArgs+=@('--mode','ask')}
$log=Join-Path $runDir ($LogName+'.jsonl')
& $launcher @cliArgs $Prompt | ForEach-Object {
 Add-Content -LiteralPath $log -Value $_ -Encoding utf8
 try{$event=$_|ConvertFrom-Json -ErrorAction Stop}catch{return}
 if($event.type -eq 'assistant' -or $event.type -eq 'result'){
  $body=if($event.type -eq 'result'){$event.result}else{($event.message.content|Where-Object type -eq 'text'|ForEach-Object text)-join "`n"}
  [pscustomobject]@{type=$event.type;session_id=$event.session_id;text=$body}|ConvertTo-Json -Compress -Depth 5
 }elseif($event.type -eq 'system'){Write-Output $_}
}
exit $LASTEXITCODE
