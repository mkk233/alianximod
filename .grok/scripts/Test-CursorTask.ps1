#Requires -Version 7.0
# Offline process tests for Invoke-CursorTask.ps1. Requires PowerShell 7.
# Uses a local stub. This is not a real Cursor CLI end-to-end run.
$ErrorActionPreference = 'Stop'
$script:Passed = $false
$script:Step = 'init'
$script:Jobs = New-Object System.Collections.Generic.List[System.Diagnostics.Process]
$HostExe = (Get-Process -Id $PID).Path
$Launcher = Join-Path $PSScriptRoot 'Invoke-CursorTask.ps1'
$Root = Join-Path $env:TEMP 'cursor-task 测试'
if (-not [string]::IsNullOrWhiteSpace($env:CURSOR_TASK_TEST_ROOT)) {
    $Root = $env:CURSOR_TASK_TEST_ROOT
}
$Workspace = Join-Path $Root '工作区'
$InputDir = Join-Path $Root '输入 文件'
$OtherWorkspace = Join-Path $Root '其他工作区'
$Log = Join-Path $Root 'stub-log.jsonl'
$Batch = 'batchManual'
$Utf8 = New-Object System.Text.UTF8Encoding $false

function Step([string]$Name) {
    $script:Step = $Name
    [Console]::Out.WriteLine('STEP ' + $Name)
}

function Assert-JsonArray {
    param($Value, [int]$ExpectedCount, [string]$Label)
    $isArray = ($null -ne $Value) -and ($Value -isnot [string]) -and ($Value -is [System.Array])
    Assert-True $isArray "$Label is not an array"
    Assert-True (@($Value).Count -eq $ExpectedCount) "$Label count is not $ExpectedCount"
}

function Assert-True($Condition, [string]$Message) {
    if (-not $Condition) {
        throw ($script:Step + ': ' + $Message)
    }
}

function Same-Path([string]$Left, [string]$Right) {
    $leftFull = [System.IO.Path]::GetFullPath($Left).TrimEnd('\')
    $rightFull = [System.IO.Path]::GetFullPath($Right).TrimEnd('\')
    return $leftFull.Equals($rightFull, [System.StringComparison]::OrdinalIgnoreCase)
}

function Write-Utf8([string]$Path, [string]$Content) {
    $parent = Split-Path -Parent $Path
    if (-not (Test-Path -LiteralPath $parent)) {
        [void][System.IO.Directory]::CreateDirectory($parent)
    }
    [System.IO.File]::WriteAllText($Path, $Content, $Utf8)
}

function Read-JsonText([string]$Path) {
    return ([System.IO.File]::ReadAllText($Path, $Utf8) | ConvertFrom-Json)
}

function New-TaskFile {
    param([string]$TaskId, [string]$Operation, [string[]]$WritePaths, [string[]]$Dependencies)
    $taskPath = Join-Path $InputDir ($TaskId + '.json')
    $promptPath = Join-Path $InputDir ($TaskId + ' 提示.md')
    $paths = @()
    if ($null -ne $WritePaths) { $paths = $WritePaths }
    $dependencyIds = @()
    if ($null -ne $Dependencies) { $dependencyIds = $Dependencies }
    $document = [ordered]@{
        batchId       = $Batch
        taskId        = $TaskId
        workspace     = $Workspace
        stage         = 'implementation'
        operation     = $Operation
        authorization = '用户授权离线启动器测试'
        inputVersion  = 'implement-task-20261103'
        recordPath    = 'docs/记录.md'
        writePaths    = $paths
        dependencies  = $dependencyIds
    }
    Write-Utf8 -Path $taskPath -Content ($document | ConvertTo-Json -Depth 5)
    Write-Utf8 -Path $promptPath -Content ("提示正文 " + $TaskId + "`nOK_PROMPT_BODY_不应进参数`n")
    return [pscustomobject]@{ Task = $taskPath; Prompt = $promptPath; TaskId = $TaskId }
}

function Get-LogEntries {
    if (-not (Test-Path -LiteralPath $Log)) { return @() }
    $lines = [System.IO.File]::ReadAllLines($Log, $Utf8)
    $entries = New-Object System.Collections.Generic.List[object]
    foreach ($line in $lines) {
        if ([string]::IsNullOrWhiteSpace($line)) { continue }
        $entries.Add(($line | ConvertFrom-Json))
    }
    return $entries.ToArray()
}

function Get-ArgList($Entry) {
    if ($null -eq $Entry.args) { return ,@() }
    if ($Entry.args -is [string]) { return ,@([string]$Entry.args) }
    return ,@($Entry.args)
}

function Get-EntriesSince([int]$Start) {
    $all = @(Get-LogEntries)
    if ($Start -ge $all.Count) { return }
    foreach ($entry in @($all[$Start..($all.Count - 1)])) {
        Write-Output $entry
    }
}

function New-ProcessStart([string[]]$Arguments) {
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $HostExe
    $startInfo.WorkingDirectory = $Workspace
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.CreateNoWindow = $true
    $startInfo.StandardOutputEncoding = $Utf8
    $startInfo.StandardErrorEncoding = $Utf8
    $startInfo.ArgumentList.Add('-NoProfile')
    $startInfo.ArgumentList.Add('-ExecutionPolicy')
    $startInfo.ArgumentList.Add('Bypass')
    $startInfo.ArgumentList.Add('-File')
    $startInfo.ArgumentList.Add($Launcher)
    foreach ($argument in $Arguments) { $startInfo.ArgumentList.Add($argument) }
    return $startInfo
}

function Set-StubEnv([hashtable]$EnvMap) {
    $saved = @{}
    foreach ($key in $EnvMap.Keys) {
        $saved[$key] = [Environment]::GetEnvironmentVariable([string]$key, 'Process')
        [Environment]::SetEnvironmentVariable([string]$key, [string]$EnvMap[$key], 'Process')
    }
    return $saved
}

function Restore-StubEnv([hashtable]$Saved) {
    foreach ($key in $Saved.Keys) {
        [Environment]::SetEnvironmentVariable([string]$key, $Saved[$key], 'Process')
    }
}

function Invoke-Entry {
    param([string[]]$Arguments, [hashtable]$EnvMap)
    $saved = Set-StubEnv $EnvMap
    $startInfo = New-ProcessStart $Arguments
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    try {
        [void]$process.Start()
        $stdoutTask = $process.StandardOutput.ReadToEndAsync()
        $stderrTask = $process.StandardError.ReadToEndAsync()
        $process.WaitForExit()
        $stdoutTask.Wait()
        $stderrTask.Wait()
        $stdout = $stdoutTask.Result
        $stderr = $stderrTask.Result
        $parsed = $null
        try { $parsed = $stdout | ConvertFrom-Json } catch { $parsed = $null }
        return [pscustomobject]@{
            ExitCode = $process.ExitCode
            Stdout   = $stdout
            Stderr   = $stderr
            Json     = $parsed
        }
    }
    finally {
        $process.Dispose()
        Restore-StubEnv $saved
    }
}

function Start-Entry {
    param([string[]]$Arguments, [hashtable]$EnvMap)
    $saved = Set-StubEnv $EnvMap
    $startInfo = New-ProcessStart $Arguments
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    [void]$process.Start()
    Restore-StubEnv $saved
    $script:Jobs.Add($process)
    $stdoutTask = $process.StandardOutput.ReadToEndAsync()
    $stderrTask = $process.StandardError.ReadToEndAsync()
    return [pscustomobject]@{ Process = $process; StdoutTask = $stdoutTask; StderrTask = $stderrTask }
}

function Stop-JobTree([int]$ProcessId) {
    & "$env:SystemRoot\System32\taskkill.exe" /PID $ProcessId /T /F 1>$null 2>$null
}

function Wait-Meta([string]$TaskId, [int]$Seconds) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    do {
        $meta = Read-LatestMeta $TaskId
        if ($null -ne $meta -and $null -ne $meta.cliPid) { return $meta }
        Start-Sleep -Milliseconds 200
    } while ((Get-Date) -lt $deadline)
    return $null
}

function Read-LatestMeta([string]$TaskId) {
    $taskJson = Join-Path $Workspace (".grok\runs\$Batch\$TaskId\task.json")
    if (-not (Test-Path -LiteralPath $taskJson)) { return $null }
    $task = Read-JsonText $taskJson
    if ([string]::IsNullOrWhiteSpace([string]$task.latestAttemptId)) { return $null }
    $attemptPath = Join-Path $Workspace (".grok\runs\$Batch\$TaskId\attempts\$($task.latestAttemptId)\attempt.json")
    if (-not (Test-Path -LiteralPath $attemptPath)) { return $null }
    return Read-JsonText $attemptPath
}

function Get-AttemptCount([string]$TaskId) {
    $dir = Join-Path $Workspace (".grok\runs\$Batch\$TaskId\attempts")
    if (-not (Test-Path -LiteralPath $dir)) { return 0 }
    return @(Get-ChildItem -LiteralPath $dir -Directory).Count
}

function Assert-AgentContract($Entry, [string]$SessionId, [bool]$AskMode) {
    $values = Get-ArgList $Entry
    $joined = $values -join "`n"
    Assert-True ($values[0] -eq '--print') 'agent args do not start with --print'
    Assert-True ($joined -notmatch 'OK_PROMPT_BODY_不应进参数') 'prompt body was passed on the command line'
    foreach ($banned in @('--force', '--yolo', '--continue', '--trust', '--add-dir', '--stream-partial-output')) {
        Assert-True ($values -notcontains $banned) "banned flag $banned"
    }
    Assert-True (($values | Where-Object { $_ -eq '--model' }).Count -eq 1) 'model flag count'
    $modelIndex = [array]::IndexOf($values, '--model')
    Assert-True ($values[$modelIndex + 1] -eq 'grok-4.7-high-fast') 'model is not the fixed model'
    Assert-True ($values -contains '--auto-review') 'missing auto-review'
    Assert-True ($values -contains '--output-format') 'missing output format'
    Assert-True ($values -contains 'stream-json') 'missing stream-json'
    $resumeIndex = [array]::IndexOf($values, '--resume')
    Assert-True ($resumeIndex -ge 0 -and $values[$resumeIndex + 1] -eq $SessionId) 'resume session mismatch'
    $workspaceIndex = [array]::IndexOf($values, '--workspace')
    Assert-True ($workspaceIndex -ge 0 -and (Same-Path $values[$workspaceIndex + 1] $Workspace)) 'workspace mismatch'
    Assert-True (Same-Path ([string]$Entry.cwd) $Workspace) 'process cwd mismatch'
    if ($AskMode) {
        $modeIndex = [array]::IndexOf($values, '--mode')
        Assert-True ($modeIndex -ge 0 -and $values[$modeIndex + 1] -eq 'ask') 'ask mode missing'
    }
    else {
        Assert-True ($values -notcontains '--mode') 'write mode still passed --mode'
    }
}

function New-BaseArgs($Pair) {
    return @(
        '-TaskFile', $Pair.Task,
        '-PromptFile', $Pair.Prompt,
        '-CursorAgentPath', $Stub
    )
}

function New-FallbackArgs($Pair) {
    return @(
        '-TaskFile', $Pair.Task,
        '-PromptFile', $Pair.Prompt,
        '-CursorAgentPath', $Stub,
        '-GrokAgentPath', $script:GrokStub
    )
}

function Get-GrokEntries([object[]]$Entries) {
    $matched = New-Object System.Collections.Generic.List[object]
    foreach ($entry in @($Entries)) {
        $values = Get-ArgList $entry
        if ($values -contains '--prompt-file') { $matched.Add($entry) }
    }
    return $matched.ToArray()
}

function Assert-GrokContract($Entry, [string]$SessionId, [bool]$AskMode, [bool]$FirstStart) {
    $values = Get-ArgList $Entry
    $joined = $values -join "`n"
    Assert-True ($joined -notmatch 'OK_PROMPT_BODY_不应进参数') 'grok received the prompt body on the command line'
    Assert-True ($joined -notmatch 'SECRET_TOKEN_VALUE') 'grok args contained a secret'
    foreach ($banned in @('--force', '--yolo', '--continue', '--always-approve', '--restore-code', '--bypassPermissions', 'bypassPermissions', '--auto-review', '--print', '--mode', 'grok-4.5', 'grok-4.6', 'grok-4.7-high-fast')) {
        Assert-True ($values -notcontains $banned) "banned grok flag $banned"
    }
    Assert-True (($values | Where-Object { $_ -eq '--model' }).Count -eq 1) 'grok model flag count'
    $modelIndex = [array]::IndexOf($values, '--model')
    Assert-True ($values[$modelIndex + 1] -eq 'grok-4.7') 'grok alias is not grok-4.7'
    $effortIndex = [array]::IndexOf($values, '--reasoning-effort')
    Assert-True ($effortIndex -ge 0 -and $values[$effortIndex + 1] -eq 'high') 'grok reasoning is not high'
    $permissionIndex = [array]::IndexOf($values, '--permission-mode')
    Assert-True ($permissionIndex -ge 0 -and $values[$permissionIndex + 1] -eq 'auto') 'grok permission mode is not auto'
    Assert-True ($values -contains '--output-format' -and $values -contains 'json') 'grok output format is not json'
    Assert-True ($values -contains '--no-subagents' -and $values -contains '--disable-web-search' -and $values -contains '--no-auto-update') 'grok safety flags missing'
    Assert-True ($values -contains '--deny' -and $values -contains 'MCPTool') 'MCP deny missing'
    $sessionFlag = $(if ($FirstStart) { '--session-id' } else { '--resume' })
    $otherFlag = $(if ($FirstStart) { '--resume' } else { '--session-id' })
    $sessionIndex = [array]::IndexOf($values, $sessionFlag)
    Assert-True ($sessionIndex -ge 0 -and $values[$sessionIndex + 1] -eq $SessionId) 'grok session flag mismatch'
    Assert-True ($values -notcontains $otherFlag) 'grok used both session flags'
    $cwdIndex = [array]::IndexOf($values, '--cwd')
    Assert-True ($cwdIndex -ge 0 -and (Same-Path $values[$cwdIndex + 1] $Workspace)) 'grok cwd mismatch'
    Assert-True (([string]$Entry.envWorkflows) -eq '0' -and ([string]$Entry.envMemory) -eq '0' -and ([string]$Entry.envUpdater) -eq '1') 'grok child environment was not pinned'
    if ($AskMode) {
        $toolsIndex = [array]::IndexOf($values, '--tools')
        Assert-True ($toolsIndex -ge 0 -and $values[$toolsIndex + 1] -eq 'read_file,grep,list_dir') 'read-only tools mismatch'
        foreach ($denied in @('Edit', 'Write', 'Bash')) {
            Assert-True ($values -contains $denied) "read-only deny missing $denied"
        }
        Assert-True ($values -notcontains 'run_terminal_cmd') 'read-only grok still has shell'
        Assert-True ($joined -notmatch 'search_replace') 'read-only grok still has edit'
    }
    else {
        $toolsIndex = [array]::IndexOf($values, '--tools')
        Assert-True ($toolsIndex -ge 0 -and $values[$toolsIndex + 1] -eq 'read_file,grep,list_dir,write,search_replace,run_terminal_cmd,get_task_output,wait_tasks') 'write tools mismatch'
        Assert-True ($values -notcontains 'bypassPermissions') 'write mode widened permissions'
    }
}

function Get-FileHashSha([string]$Path) {
    $algorithm = [System.Security.Cryptography.SHA256]::Create()
    $stream = [System.IO.File]::Open($Path, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::ReadWrite)
    try { return ([System.BitConverter]::ToString($algorithm.ComputeHash($stream))).Replace('-', '') }
    finally { $stream.Dispose(); $algorithm.Dispose() }
}

if (Test-Path -LiteralPath $Root) { Remove-Item -LiteralPath $Root -Recurse -Force }
[void][System.IO.Directory]::CreateDirectory($Workspace)
[void][System.IO.Directory]::CreateDirectory($InputDir)
[void][System.IO.Directory]::CreateDirectory($OtherWorkspace)
Write-Utf8 -Path $Log -Content ''
[Environment]::SetEnvironmentVariable('CURSOR_TASK_STUB_LOG', $Log, 'Process')

$Stub = Join-Path $Root 'stub agent.ps1'
$stubSource = @'
$received = @($args)
$logPath = $env:CURSOR_TASK_STUB_LOG
$scenario = $env:CURSOR_TASK_STUB_SCENARIO
if ([string]::IsNullOrWhiteSpace($scenario)) { $scenario = 'ok' }
$entry = [ordered]@{
    args = @($received)
    cwd = [System.IO.Directory]::GetCurrentDirectory()
    scenario = $scenario
}
$line = ($entry | ConvertTo-Json -Compress -Depth 6) + "`n"
$stream = [System.IO.File]::Open($logPath, [System.IO.FileMode]::Append, [System.IO.FileAccess]::Write, [System.IO.FileShare]::ReadWrite)
try {
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($line)
    $stream.Write($bytes, 0, $bytes.Length)
}
finally { $stream.Dispose() }

function Write-OutLine([string]$Text) {
    $payload = [System.Text.Encoding]::UTF8.GetBytes($Text + "`n")
    $output = [Console]::OpenStandardOutput()
    $output.Write($payload, 0, $payload.Length)
    $output.Flush()
}
function Write-ErrLine([string]$Text) {
    $payload = [System.Text.Encoding]::UTF8.GetBytes($Text + "`n")
    $errorStream = [Console]::OpenStandardError()
    $errorStream.Write($payload, 0, $payload.Length)
    $errorStream.Flush()
}
function Write-Event($Object) {
    Write-OutLine (($Object | ConvertTo-Json -Compress -Depth 8))
}
function New-Report([string]$Status, [string]$Summary) {
    $json = '{"workStatus":"' + $Status + '","summary":"' + $Summary + '","changedFiles":[],"checks":["offline"],"notRun":["game"],"writesSettled":true,"blocker":"","nextAction":"none"}'
    return "<<<CURSOR_TASK_REPORT>>>" + "`n" + $json + "`n" + "<<<END_CURSOR_TASK_REPORT>>>"
}

if ($received.Count -ge 1 -and $received[0] -eq 'create-chat') {
    if ($scenario -eq 'create_fail') {
        Write-ErrLine 'create-chat failed'
        exit 2
    }
    Write-OutLine ([guid]::NewGuid().ToString())
    exit 0
}

$session = ''
for ($index = 0; $index -lt $received.Count; $index++) {
    if ($received[$index] -eq '--resume' -and ($index + 1) -lt $received.Count) { $session = [string]$received[$index + 1] }
}
if ($scenario -eq 'sleep') {
    $delay = 12000
    if ($env:CURSOR_TASK_STUB_SLEEP_MS) { $delay = [int]$env:CURSOR_TASK_STUB_SLEEP_MS }
    Start-Sleep -Milliseconds $delay
}
$initModel = 'grok-4.7-high-fast'
if ($scenario -eq 'model_mismatch') { $initModel = 'grok-4.5' }
elseif ($scenario -eq 'display_model') { $initModel = 'Grok 4.7 256K High Fast' }
elseif ($scenario -eq 'display_list') { $initModel = "Grok 4.7  High Fast$([char]0x200B)" }
elseif ($scenario -eq 'model_low') { $initModel = 'Grok 4.7 Low' }
elseif ($scenario -eq 'model_one_space') { $initModel = 'Grok 4.7 High Fast' }
Write-Event ([ordered]@{ type = 'system'; subtype = 'init'; model = $initModel; session_id = $session })
if ($scenario -eq 'ok' -or $scenario -eq 'sleep') {
    Write-Event ([ordered]@{ type = 'tool_call'; name = 'secret_tool_body' })
    Write-Event ([ordered]@{ type = 'assistant'; text = 'secret_thinking_body' })
}
if ($scenario -eq 'bad_json') { Write-OutLine 'NOT_JSON_LINE' }
if ($scenario -eq 'no_result') { exit 0 }

$resultSession = $session
if ($scenario -eq 'session_mismatch') { $resultSession = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa' }
$isError = $false
$exitCode = 0
$emitResult = $true
$resultText = 'TOKEN_STREAM_SHOULD_NOT_APPEAR_IN_STATUS' + "`n" + (New-Report 'reported_done' 'done')
switch ($scenario) {
    'is_error' { $isError = $true; $resultText = New-Report 'reported_done' 'error-report' }
    'exit_nonzero' { $exitCode = 7 }
    'blocked' { $resultText = New-Report 'blocked' 'need-check' }
    'persistent_ask' { $resultText = "<<<CURSOR_TASK_REPORT>>>`n" + '{"workStatus":"blocked","summary":"Ask mode is still active","changedFiles":[],"checks":["offline"],"notRun":["game"],"writesSettled":true,"blocker":"Ask mode is still active","nextAction":"recover"}' + "`n<<<END_CURSOR_TASK_REPORT>>>" }
    'needs_input' { $resultText = New-Report 'needs_input' 'need-user' }
    'plain' { $resultText = 'PLAIN_RESULT_ONLY' }
    'blocker_null' { $resultText = "<<<CURSOR_TASK_REPORT>>>`n" + '{"workStatus":"reported_done","summary":"null-blocker","changedFiles":[],"checks":["offline"],"notRun":["game"],"writesSettled":true,"blocker":null,"nextAction":"none"}' + "`n<<<END_CURSOR_TASK_REPORT>>>" }
    'arrays_empty' { $resultText = "<<<CURSOR_TASK_REPORT>>>`n" + '{"workStatus":"reported_done","summary":"empty","changedFiles":[],"checks":[],"notRun":[],"writesSettled":false,"blocker":null,"nextAction":"none"}' + "`n<<<END_CURSOR_TASK_REPORT>>>" }
    'arrays_one' { $resultText = "<<<CURSOR_TASK_REPORT>>>`n" + '{"workStatus":"reported_done","summary":"one","changedFiles":["file-a"],"checks":["check-a"],"notRun":["skip-a"],"writesSettled":true,"blocker":"one-blocker","nextAction":"wait"}' + "`n<<<END_CURSOR_TASK_REPORT>>>" }
    'arrays_many' { $resultText = "<<<CURSOR_TASK_REPORT>>>`n" + '{"workStatus":"reported_done","summary":"many","changedFiles":["file-a","file-b"],"checks":["check-a","check-b","check-c"],"notRun":["skip-a","skip-b"],"writesSettled":false,"blocker":null,"nextAction":"next"}' + "`n<<<END_CURSOR_TASK_REPORT>>>" }
    'arrays_scalar' { $resultText = "<<<CURSOR_TASK_REPORT>>>`n" + '{"workStatus":"reported_done","summary":"scalar","changedFiles":"file-a","checks":["ok"],"notRun":[],"writesSettled":false,"blocker":null,"nextAction":"none"}' + "`n<<<END_CURSOR_TASK_REPORT>>>" }
    'bad_report' { $resultText = "<<<CURSOR_TASK_REPORT>>>{<<<END_CURSOR_TASK_REPORT>>>" }
    'two_reports' { $resultText = (New-Report 'reported_done' 'one') + (New-Report 'blocked' 'two') }
    'auto_review' { $isError = $true; $exitCode = 1; $resultText = 'Auto-review rejected the tool call'; Write-ErrLine 'Auto-review rejected the tool call' }
    'quota_stderr' {
        Write-ErrLine 'ActionRequiredError: Weekly usage limit reached. It resets in 5 days. Buy credits to keep going now.'
        Write-ErrLine 'Authorization: Bearer SECRET_TOKEN_VALUE'
        $exitCode = 1
        $emitResult = $false
    }
    'quota_error_event' {
        Write-Event ([ordered]@{ type = 'error'; message = 'ActionRequiredError: Weekly usage limit reached. It resets later.' })
        $exitCode = 1
        $emitResult = $false
    }
    'quota_in_result' { $resultText = 'The model says ActionRequiredError: Weekly usage limit reached. It resets in 2 days. Buy credits to keep going now.' }
    'quota_in_tool' { Write-Event ([ordered]@{ type = 'tool_call'; name = 'read'; output = 'ActionRequiredError: Weekly usage limit reached. It resets in 2 days.' }) }
    'rate_limit' { Write-ErrLine '429 Too Many Requests'; $exitCode = 1; $emitResult = $false }
    'network' { Write-ErrLine 'connection timed out'; $exitCode = 1; $emitResult = $false }
    'tls' { Write-ErrLine 'TLS handshake failed: connection reset'; $exitCode = 1; $emitResult = $false }
    'approval_result' { $exitCode = 1; $resultText = 'Auto-review rejected the tool call' }
    'quota_and_approval' { Write-ErrLine 'ActionRequiredError: Weekly usage limit reached. It resets in 2 days.'; Write-ErrLine 'Auto-review rejected the tool call'; $exitCode = 1; $emitResult = $false }
    'canceled' { Write-ErrLine 'canceled by user'; $exitCode = 1; $emitResult = $false }
    'auth_fail' { Write-ErrLine 'authentication failed'; $exitCode = 1; $emitResult = $false }
    'compile_fail' { Write-ErrLine 'compileJava FAILED'; $exitCode = 1; $emitResult = $false }
    'quota_protocol' {
        Write-ErrLine 'ActionRequiredError: Weekly usage limit reached. It resets in 1 day.'
        Write-OutLine 'NOT_JSON_LINE'
        $exitCode = 1
        $emitResult = $false
    }
    'two_results' {
        Write-Event ([ordered]@{ type = 'result'; subtype = 'success'; is_error = $false; session_id = $resultSession; result = (New-Report 'blocked' 'first-result') })
        Write-Event ([ordered]@{ type = 'result'; subtype = 'success'; is_error = $false; session_id = $resultSession; result = (New-Report 'reported_done' 'second-result') })
        exit 0
    }
}
if ($emitResult -and $scenario -ne 'two_results') {
    Write-Event ([ordered]@{ type = 'result'; subtype = 'success'; is_error = $isError; session_id = $resultSession; result = $resultText })
}
exit $exitCode
'@
Write-Utf8 -Path $Stub -Content $stubSource
$script:GrokStub = Join-Path $Root 'grok-stub.ps1'
$grokStubSource = @'
$received = @($args)
$logPath = $env:CURSOR_TASK_STUB_LOG
$scenario = $env:CURSOR_TASK_STUB_SCENARIO
if ([string]::IsNullOrWhiteSpace($scenario)) { $scenario = 'ok' }
$entry = [ordered]@{
    args = @($received)
    cwd = [System.IO.Directory]::GetCurrentDirectory()
    scenario = $scenario
    envWorkflows = [string]$env:GROK_WORKFLOWS
    envMemory = [string]$env:GROK_MEMORY
    envUpdater = [string]$env:GROK_DISABLE_AUTOUPDATER
}
$line = ($entry | ConvertTo-Json -Compress -Depth 6) + "`n"
$stream = [System.IO.File]::Open($logPath, [System.IO.FileMode]::Append, [System.IO.FileAccess]::Write, [System.IO.FileShare]::ReadWrite)
try {
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($line)
    $stream.Write($bytes, 0, $bytes.Length)
}
finally { $stream.Dispose() }
function Write-OutLine([string]$Text) {
    $payload = [System.Text.Encoding]::UTF8.GetBytes($Text + "`n")
    $output = [Console]::OpenStandardOutput()
    $output.Write($payload, 0, $payload.Length)
    $output.Flush()
}
function Write-ErrLine([string]$Text) {
    $payload = [System.Text.Encoding]::UTF8.GetBytes($Text + "`n")
    $errorStream = [Console]::OpenStandardError()
    $errorStream.Write($payload, 0, $payload.Length)
    $errorStream.Flush()
}
function New-Report([string]$Status, [bool]$Settled) {
    $settledText = $(if ($Settled) { 'true' } else { 'false' })
    $json = '{"workStatus":"' + $Status + '","summary":"grok","changedFiles":[],"checks":["offline"],"notRun":["game"],"writesSettled":' + $settledText + ',"blocker":null,"nextAction":"none"}'
    return "<<<CURSOR_TASK_REPORT>>>" + "`n" + $json + "`n" + "<<<END_CURSOR_TASK_REPORT>>>"
}
if ($received.Count -ge 1 -and $received[0] -eq 'create-chat') { Write-ErrLine 'grok stub refuses create-chat'; exit 9 }
$session = ''
$first = $false
$cwd = ''
for ($index = 0; $index -lt $received.Count; $index++) {
    if ($received[$index] -eq '--session-id' -and ($index + 1) -lt $received.Count) { $session = [string]$received[$index + 1]; $first = $true }
    if ($received[$index] -eq '--resume' -and ($index + 1) -lt $received.Count) { $session = [string]$received[$index + 1] }
    if ($received[$index] -eq '--cwd' -and ($index + 1) -lt $received.Count) { $cwd = [string]$received[$index + 1] }
}
if ($scenario -eq 'sleep' -or $scenario -eq 'grok_sleep') {
    $delay = 12000
    if ($env:CURSOR_TASK_STUB_SLEEP_MS) { $delay = [int]$env:CURSOR_TASK_STUB_SLEEP_MS }
    Start-Sleep -Milliseconds $delay
}
$stop = 'end_turn'
$text = New-Report 'reported_done' $true
$exitCode = 0
$errorObject = $false
$modelUsage = '{"grok-4.7-build-fast":{"modelCalls":1}}'
switch ($scenario) {
    'grok_error' { $errorObject = $true; $exitCode = 1 }
    'grok_missing' { Write-OutLine ''; exit 0 }
    'grok_truncated' { Write-OutLine '{"text":"partial"'; exit 0 }
    'grok_conflict' {
        Write-OutLine (([ordered]@{ text = 'one'; stopReason = 'end_turn'; sessionId = $session; requestId = 'r1'; cwd = $cwd } | ConvertTo-Json -Compress))
        Write-OutLine (([ordered]@{ text = 'two'; stopReason = 'end_turn'; sessionId = $session; requestId = 'r2'; cwd = $cwd } | ConvertTo-Json -Compress))
        exit 0
    }
    'grok_bad_session' { $session = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb' }
    'grok_bad_cwd' { $cwd = 'D:\not-the-workspace' }
    'grok_max_tokens' { $stop = 'max_tokens'; $text = 'cut off' }
    'grok_no_report' { $text = 'PLAIN_GROK_TEXT' }
    'grok_two_reports' { $text = (New-Report 'reported_done' $true) + (New-Report 'blocked' $true) }
    'grok_unsettled' { $text = New-Report 'reported_done' $false }
    'grok_unknown_tool' { Write-ErrLine "warning: unknown tool nope names no tool and leaves the other tools available" }
    'grok_wrong_model' { $modelUsage = '{"grok-4.6":{"modelCalls":1}}' }
    'grok_no_usage' { $modelUsage = '' }
    'grok_secret' { Write-ErrLine 'Authorization: Bearer SECRET_TOKEN_VALUE' }
    'grok_permission' { Write-ErrLine 'permission required: Edit calc.py was not run, as a headless run cannot ask for approval' }
    'grok_skill_warn' { Write-ErrLine 'skipped permission entry path=D:\alianximod\.claude\settings.local.json rule=permissions.allow: Skill(deep-research) reason=unknown tool prefix: Skill' }
    'grok_effort_ignored' { Write-ErrLine 'warning: 老猪的中转站 has no effort levels; ignoring --effort/--reasoning-effort high' }
}
if ($errorObject) {
    Write-OutLine '{"type":"error","message":"boom"}'
    exit $exitCode
}
$object = [ordered]@{
    text = $text
    stopReason = $stop
    sessionId = $session
    requestId = 'req-offline'
    cwd = $cwd
}
if (-not [string]::IsNullOrWhiteSpace($modelUsage)) {
    $object.modelUsage = ($modelUsage | ConvertFrom-Json)
}
Write-OutLine ($object | ConvertTo-Json -Compress -Depth 6)
exit $exitCode
'@
Write-Utf8 -Path $script:GrokStub -Content $grokStubSource
$script:OriginalAttemptHash = Get-FileHashSha 'D:\alianximod\.grok\runs\workflow-collab-20261103\cursor-smoke\attempts\a20261102164454600850b\attempt.json'
$script:QuotaEvidenceHash = Get-FileHashSha 'D:\alianximod\.grok\runs\cooperative-slash-20261103\compile-final\attempts\a2026110318480502362ed\attempt.json'
$script:QuotaStderrHash = Get-FileHashSha 'D:\alianximod\.grok\runs\cooperative-slash-20261103\compile-final\attempts\a2026110318480502362ed\stderr.log'

try {
    Step 'gitignore'
    $ignore = [System.IO.File]::ReadAllText('D:\alianximod\.gitignore', $Utf8)
    Assert-True ($ignore.Contains('dev-authorized/epicfight/')) 'existing gitignore content missing'
    Assert-True ($ignore.Contains(".grok/runs/")) 'runs ignore missing'

    Step 'reject-traversal'
    $badPath = Join-Path $InputDir 'bad.json'
    Write-Utf8 -Path $badPath -Content '{"batchId":"../evil","taskId":"taskOk","workspace":"C:\\Windows","stage":"implementation","operation":"read","authorization":"x","inputVersion":"v","recordPath":"r","writePaths":[],"dependencies":[]}'
    $badPrompt = Join-Path $InputDir 'bad.md'
    Write-Utf8 -Path $badPrompt -Content 'x'
    $bad = Invoke-Entry -Arguments @('-Action','Start','-TaskFile',$badPath,'-PromptFile',$badPrompt,'-CursorAgentPath',$Stub) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($bad.ExitCode -eq 2 -and $bad.Json.code -eq 'invalid_task') ('traversal response ' + $bad.Stdout)
    Assert-True (-not (Test-Path -LiteralPath (Join-Path $Workspace '.grok\runs\evil'))) 'traversal created a directory'

    Step 'reject-write-mode'
    $empty = New-TaskFile -TaskId 'taskEmptyWrite' -Operation 'implement'
    $denied = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $empty) + @('-WriteMode')) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($denied.ExitCode -eq 2 -and $denied.Json.code -eq 'write_not_authorized') $denied.Stdout
    Assert-True ((Get-AttemptCount 'taskEmptyWrite') -eq 0) 'write rejection created an attempt'

    Step 'happy-path'
    $mark = @(Get-LogEntries).Count
    $ok = New-TaskFile -TaskId 'taskOk' -Operation 'implement'
    $started = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $ok)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($null -ne $started.Json) ('unparsed ' + $started.Stdout + $started.Stderr)
    Assert-True ($started.ExitCode -eq 0 -and $started.Json.ok -eq $true -and $started.Json.lifecycle -eq 'reported') $started.Stdout
    Assert-True ($started.Json.workStatus -eq 'reported_done' -and $started.Json.reportStatus -eq 'valid') $started.Stdout
    Assert-True ($started.Json.pendingCompile -eq $true -and $started.Json.accepted -eq $false -and $started.Json.complete -eq $false -and $started.Json.quotaChanged -eq $false) $started.Stdout
    Assert-True ($started.Json.feedback.Contains('TOKEN_STREAM_SHOULD_NOT_APPEAR_IN_STATUS')) 'feedback missing'
    Assert-True (-not $started.Stdout.Contains('secret_tool_body') -and -not $started.Stdout.Contains('secret_thinking_body')) 'tool or thinking leaked to the console'
    $session = [string]$started.Json.sessionId
    Assert-True ($session -match '^[0-9a-fA-F-]{36}$') 'session was not a uuid'
    $events = [System.IO.File]::ReadAllText((Join-Path $started.Json.attemptDir 'events.jsonl'), $Utf8)
    Assert-True ($events.Contains('secret_tool_body') -and $events.Contains('secret_thinking_body')) 'diagnostic events were not saved'
    $promptSent = [System.IO.File]::ReadAllText((Join-Path $started.Json.attemptDir 'cli-prompt.txt'), $Utf8)
    Assert-True ($promptSent.Contains($ok.Task) -and $promptSent.Contains($ok.Prompt)) 'cli prompt missing file paths'
    Assert-True (-not $promptSent.Contains('OK_PROMPT_BODY_不应进参数')) 'cli prompt included the prompt body'
    foreach ($name in @('batchId','taskId','attemptId','sessionId','model','workspace','mode','startedAt','endedAt','pid','processStartTime','exitCode','isError','inputHash','receiptPath')) {
        Assert-True ($null -ne $started.Json.meta.$name -and [string]$started.Json.meta.$name -ne '') ("missing meta " + $name)
    }
    Assert-True ($started.Json.meta.model -eq 'grok-4.7-high-fast' -and $started.Json.meta.mode -eq 'ask') 'mode or model meta mismatch'
    $disk = Read-JsonText (Join-Path $started.Json.attemptDir 'attempt.json')
    Assert-True ($disk.lifecycle -eq 'reported' -and $disk.inputHash -eq $started.Json.meta.inputHash -and $disk.endedAt) 'disk meta dropped fields'
    $fresh = Get-EntriesSince $mark
    $agents = @($fresh | Where-Object { (Get-ArgList $_)[0] -eq '--print' })
    $creates = @($fresh | Where-Object { (Get-ArgList $_)[0] -eq 'create-chat' })
    Assert-True ($creates.Count -eq 1 -and $agents.Count -eq 1) 'start did not create exactly one session and one run'
    Assert-AgentContract $agents[0] $session $true
    Assert-True (Test-Path -LiteralPath (Join-Path $Workspace ".grok\runs\$Batch\taskOk\task.lock")) 'task lock file was deleted'
    Assert-True (Test-Path -LiteralPath (Join-Path $Workspace ".grok\runs\_sessions\$session.lock")) 'session lock file was deleted'

    Step 'status-and-result'
    $status = Invoke-Entry -Arguments @('-Action','Status','-TaskFile',$ok.Task,'-CursorAgentPath',$Stub) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($status.ExitCode -eq 0 -and $status.Json.lifecycle -eq 'reported' -and $status.Json.workStatus -eq 'reported_done' -and $status.Json.reportStatus -eq 'valid') $status.Stdout
    Assert-True ($status.Json.sessionId -eq $session -and $status.Json.attemptId -eq $started.Json.attemptId -and $status.Json.needsHuman -eq $false -and $status.Json.processAlive -eq $false) $status.Stdout
    Assert-True (-not $status.Stdout.Contains('TOKEN_STREAM_SHOULD_NOT_APPEAR_IN_STATUS')) 'status read the raw result'
    Assert-True (-not $status.Stdout.Contains('secret_tool_body')) 'status printed tool output'
    Assert-True (-not $status.Stdout.Contains([string]$started.Json.meta.inputHash)) 'status included the input hash'
    Assert-True (-not $status.Stdout.Contains('用户授权离线启动器测试')) 'status included authorization'
    Assert-True ($status.Json.PSObject.Properties.Name -notcontains 'meta' -and $status.Json.PSObject.Properties.Name -notcontains 'feedback') 'status still carries full evidence'
    $statusDisk = Read-JsonText (Join-Path $started.Json.attemptDir 'attempt.json')
    Assert-True ($statusDisk.inputHash -eq $started.Json.meta.inputHash -and $statusDisk.authorization.Contains('用户授权')) 'status rewrite dropped persisted fields'
    $result = Invoke-Entry -Arguments @('-Action','Result','-TaskFile',$ok.Task,'-CursorAgentPath',$Stub) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($result.Json.feedback.Contains('TOKEN_STREAM_SHOULD_NOT_APPEAR_IN_STATUS')) 'result missing feedback'
    Assert-True ($result.Json.receipt.workStatus -eq 'reported_done' -and $result.Json.receipt.pendingCompile -eq $true) 'result receipt missing'

    Step 'second-start-rejected'
    $before = Get-FileHashSha (Join-Path $started.Json.attemptDir 'attempt.json')
    $again = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $ok)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($again.ExitCode -eq 2 -and $again.Json.code -eq 'task_exists') $again.Stdout
    Assert-True ((Get-AttemptCount 'taskOk') -eq 1) 'second start overwrote attempts'
    Assert-True ((Get-FileHashSha (Join-Path $started.Json.attemptDir 'attempt.json')) -eq $before) 'second start rewrote the old attempt'

    Step 'missing-result'
    $missing = New-TaskFile -TaskId 'taskMissing' -Operation 'implement'
    $missingRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $missing)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'no_result' }
    Assert-True ($missingRun.ExitCode -eq 4 -and $missingRun.Json.lifecycle -eq 'missing_result' -and $missingRun.Json.ok -eq $false) $missingRun.Stdout

    Step 'is-error-then-resume'
    $mark = @(Get-LogEntries).Count
    $failed = New-TaskFile -TaskId 'taskFail' -Operation 'implement'
    $failedRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $failed)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'is_error' }
    Assert-True ($failedRun.ExitCode -eq 4 -and $failedRun.Json.lifecycle -eq 'failed' -and $failedRun.Json.meta.isError -eq $true -and $failedRun.Json.meta.exitCode -eq 0) $failedRun.Stdout
    Assert-True ($failedRun.Json.accepted -eq $false -and $failedRun.Json.ok -eq $false) 'is_error was treated as success'
    $failedStatus = Invoke-Entry -Arguments @('-Action','Status','-TaskFile',$failed.Task,'-CursorAgentPath',$Stub) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($failedStatus.Json.lifecycle -eq 'failed') $failedStatus.Stdout
    Assert-True (-not $failedStatus.Stdout.Contains('error-report')) 'status inspected the raw result'
    $firstAttempt = $failedRun.Json.attemptId
    $firstHash = Get-FileHashSha (Join-Path $failedRun.Json.attemptDir 'attempt.json')
    $firstPromptHash = Get-FileHashSha (Join-Path $failedRun.Json.attemptDir 'prompt.md')
    Write-Utf8 -Path $failed.Prompt -Content "BODY_V2`nOK_PROMPT_BODY_不应进参数`n"
    $reason = '已核对进程不在，继续同一会话 REASON42'
    $resumed = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason',$reason) + (New-BaseArgs $failed)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($resumed.ExitCode -eq 0 -and $resumed.Json.lifecycle -eq 'reported' -and $resumed.Json.sessionId -eq $failedRun.Json.sessionId) $resumed.Stdout
    Assert-True ($resumed.Json.attemptId -ne $firstAttempt) 'resume reused the old attempt directory'
    Assert-True ((Get-FileHashSha (Join-Path $failedRun.Json.attemptDir 'attempt.json')) -eq $firstHash) 'resume overwrote the failed attempt'
    Assert-True ((Get-FileHashSha (Join-Path $failedRun.Json.attemptDir 'prompt.md')) -eq $firstPromptHash) 'resume overwrote the old prompt snapshot'
    $secondPrompt = [System.IO.File]::ReadAllText((Join-Path $resumed.Json.attemptDir 'prompt.md'), $Utf8)
    Assert-True ($secondPrompt.Contains('BODY_V2')) 'new attempt missed the new prompt snapshot'
    $resumedPrompt = [System.IO.File]::ReadAllText((Join-Path $resumed.Json.attemptDir 'cli-prompt.txt'), $Utf8)
    Assert-True (-not $resumedPrompt.Contains('REASON42') -and -not $resumedPrompt.Contains('BODY_V2')) 'resume reason or prompt body leaked into the CLI prompt'
    $after = Get-EntriesSince $mark
    Assert-True (@($after | Where-Object { (Get-ArgList $_)[0] -eq 'create-chat' }).Count -eq 1) 'resume created another session'
    $resumeAgents = @($after | Where-Object { (Get-ArgList $_)[0] -eq '--print' })
    Assert-True ($resumeAgents.Count -eq 2) 'expected the failed run and the resume run'
    Assert-True ((Get-ArgList $resumeAgents[1]) -join "`n" -notmatch 'REASON42') 'resume reason was passed to the stub'
    $oldResult = Invoke-Entry -Arguments @('-Action','Result','-TaskFile',$failed.Task,'-AttemptId',$firstAttempt,'-CursorAgentPath',$Stub) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($oldResult.Json.feedback.Contains('error-report') -and -not $oldResult.Json.feedback.Contains('TOKEN_STREAM_SHOULD_NOT_APPEAR_IN_STATUS')) 'historical result was overwritten by the latest'
    $latestResult = Invoke-Entry -Arguments @('-Action','Result','-TaskFile',$failed.Task,'-CursorAgentPath',$Stub) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($latestResult.Json.attemptId -eq $resumed.Json.attemptId -and $latestResult.Json.feedback.Contains('TOKEN_STREAM_SHOULD_NOT_APPEAR_IN_STATUS')) 'default result was not the latest'

    Step 'exit-nonzero'
    $exitTask = New-TaskFile -TaskId 'taskExit' -Operation 'implement'
    $exitRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $exitTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'exit_nonzero' }
    Assert-True ($exitRun.ExitCode -eq 4 -and $exitRun.Json.lifecycle -eq 'failed' -and $exitRun.Json.meta.exitCode -eq 7) $exitRun.Stdout

    Step 'protocol-conflicts'
    foreach ($pair in @(
            @{ Id = 'taskTwo'; Scenario = 'two_results' },
            @{ Id = 'taskBadJson'; Scenario = 'bad_json' },
            @{ Id = 'taskSession'; Scenario = 'session_mismatch' },
            @{ Id = 'taskModel'; Scenario = 'model_mismatch' },
            @{ Id = 'taskModelLow'; Scenario = 'model_low' },
            @{ Id = 'taskModelSpace'; Scenario = 'model_one_space' }
        )) {
        $item = New-TaskFile -TaskId $pair.Id -Operation 'read'
        $run = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $item)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = $pair.Scenario }
        Assert-True ($run.ExitCode -eq 4 -and $run.Json.lifecycle -eq 'protocol_error' -and $run.Json.ok -eq $false) ($pair.Id + ' ' + $run.Stdout)
        Assert-True ([string]::IsNullOrWhiteSpace([string]$run.Json.workStatus)) ($pair.Id + ' selected a work status')
        Assert-True ([string]::IsNullOrWhiteSpace([string]$run.Json.feedback)) ($pair.Id + ' printed a chosen result')
    }

    Step 'report-status'
    $plainTask = New-TaskFile -TaskId 'taskPlain' -Operation 'read'
    $plainRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $plainTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'plain' }
    Assert-True ($plainRun.ExitCode -eq 0 -and $plainRun.Json.lifecycle -eq 'reported' -and $plainRun.Json.reportStatus -eq 'missing' -and [string]::IsNullOrWhiteSpace([string]$plainRun.Json.workStatus)) $plainRun.Stdout
    $badReportTask = New-TaskFile -TaskId 'taskBadReport' -Operation 'read'
    $badReport = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $badReportTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'bad_report' }
    Assert-True ($badReport.Json.lifecycle -eq 'reported' -and $badReport.Json.reportStatus -eq 'invalid') $badReport.Stdout
    $twoReportTask = New-TaskFile -TaskId 'taskTwoReports' -Operation 'read'
    $twoReport = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $twoReportTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'two_reports' }
    Assert-True ($twoReport.Json.reportStatus -eq 'invalid' -and $twoReport.Json.accepted -eq $false) $twoReport.Stdout
    $nullBlockerTask = New-TaskFile -TaskId 'taskNullBlocker' -Operation 'read'
    $nullBlocker = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $nullBlockerTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'blocker_null' }
    Assert-True ($nullBlocker.ExitCode -eq 0 -and $nullBlocker.Json.reportStatus -eq 'valid' -and $null -eq $nullBlocker.Json.receipt.blocker -and $nullBlocker.Json.receipt.writesSettled -eq $true) $nullBlocker.Stdout

    Step 'array-shapes'
    $emptyTask = New-TaskFile -TaskId 'taskArrayEmpty' -Operation 'read'
    $emptyRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $emptyTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'arrays_empty' }
    Assert-True ($emptyRun.ExitCode -eq 0 -and $emptyRun.Json.reportStatus -eq 'valid') $emptyRun.Stdout
    $emptyDir = [string]$emptyRun.Json.attemptDir
    $emptyMeta = Read-JsonText (Join-Path $emptyDir 'attempt.json')
    $emptyReceipt = Read-JsonText (Join-Path $emptyDir 'receipt.json')
    Assert-JsonArray $emptyMeta.writePaths 0 'empty disk writePaths'
    Assert-JsonArray $emptyMeta.dependencies 0 'empty disk dependencies'
    Assert-JsonArray $emptyReceipt.changedFiles 0 'empty disk changedFiles'
    Assert-JsonArray $emptyReceipt.checks 0 'empty disk checks'
    Assert-JsonArray $emptyReceipt.notRun 0 'empty disk notRun'
    Assert-True ($emptyReceipt.writesSettled -is [bool] -and $emptyReceipt.writesSettled -eq $false -and $null -eq $emptyReceipt.blocker) 'empty disk scalars changed'
    $emptyResult = Invoke-Entry -Arguments @('-Action','Result','-TaskFile',$emptyTask.Task,'-CursorAgentPath',$Stub) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'arrays_empty' }
    Assert-True ($emptyResult.ExitCode -eq 0) $emptyResult.Stdout
    Assert-JsonArray $emptyResult.Json.meta.writePaths 0 'empty result writePaths'
    Assert-JsonArray $emptyResult.Json.meta.dependencies 0 'empty result dependencies'
    Assert-JsonArray $emptyResult.Json.receipt.changedFiles 0 'empty result changedFiles'
    Assert-JsonArray $emptyResult.Json.receipt.checks 0 'empty result checks'
    Assert-JsonArray $emptyResult.Json.receipt.notRun 0 'empty result notRun'
    Assert-JsonArray $emptyResult.Json.attemptIds 1 'empty result attemptIds'
    Assert-True ($emptyResult.Json.receipt.writesSettled -is [bool] -and $null -eq $emptyResult.Json.receipt.blocker) 'empty result scalars changed'

    $oneTask = New-TaskFile -TaskId 'taskArrayOne' -Operation 'read' -WritePaths @('src/One.java') -Dependencies @('depOne')
    $oneRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $oneTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'arrays_one' }
    Assert-True ($oneRun.ExitCode -eq 0 -and $oneRun.Json.reportStatus -eq 'valid') $oneRun.Stdout
    $oneDir = [string]$oneRun.Json.attemptDir
    $oneMeta = Read-JsonText (Join-Path $oneDir 'attempt.json')
    $oneReceipt = Read-JsonText (Join-Path $oneDir 'receipt.json')
    Assert-JsonArray $oneMeta.writePaths 1 'one disk writePaths'
    Assert-JsonArray $oneMeta.dependencies 1 'one disk dependencies'
    Assert-JsonArray $oneReceipt.changedFiles 1 'one disk changedFiles'
    Assert-JsonArray $oneReceipt.checks 1 'one disk checks'
    Assert-JsonArray $oneReceipt.notRun 1 'one disk notRun'
    Assert-True ($oneReceipt.writesSettled -is [bool] -and $oneReceipt.writesSettled -eq $true -and $oneReceipt.blocker -is [string] -and $oneReceipt.blocker -eq 'one-blocker') 'one disk scalars changed'
    $oneResult = Invoke-Entry -Arguments @('-Action','Result','-TaskFile',$oneTask.Task,'-CursorAgentPath',$Stub) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'arrays_one' }
    Assert-JsonArray $oneResult.Json.meta.writePaths 1 'one result writePaths'
    Assert-JsonArray $oneResult.Json.meta.dependencies 1 'one result dependencies'
    Assert-JsonArray $oneResult.Json.receipt.changedFiles 1 'one result changedFiles'
    Assert-JsonArray $oneResult.Json.receipt.checks 1 'one result checks'
    Assert-JsonArray $oneResult.Json.receipt.notRun 1 'one result notRun'
    Assert-JsonArray $oneResult.Json.attemptIds 1 'one result attemptIds'
    Assert-True ([string]$oneResult.Json.receipt.checks[0] -eq 'check-a' -and [string]$oneResult.Json.meta.writePaths[0] -eq 'src/One.java') 'one result values changed'
    $oneResume = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','再记一次数组') + (New-BaseArgs $oneTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'arrays_one' }
    Assert-True ($oneResume.ExitCode -eq 0) $oneResume.Stdout
    $oneAgain = Invoke-Entry -Arguments @('-Action','Result','-TaskFile',$oneTask.Task,'-CursorAgentPath',$Stub) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'arrays_one' }
    Assert-JsonArray $oneAgain.Json.attemptIds 2 'resumed result attemptIds'
    $oneKept = Read-JsonText (Join-Path $oneDir 'receipt.json')
    Assert-JsonArray $oneKept.checks 1 'first one-element receipt was rewritten'

    $manyTask = New-TaskFile -TaskId 'taskArrayMany' -Operation 'read' -WritePaths @('src/A.java', 'src/B.java') -Dependencies @('depA', 'depB')
    $manyRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $manyTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'arrays_many' }
    Assert-True ($manyRun.ExitCode -eq 0 -and $manyRun.Json.reportStatus -eq 'valid') $manyRun.Stdout
    $manyDir = [string]$manyRun.Json.attemptDir
    $manyMeta = Read-JsonText (Join-Path $manyDir 'attempt.json')
    $manyReceipt = Read-JsonText (Join-Path $manyDir 'receipt.json')
    Assert-JsonArray $manyMeta.writePaths 2 'many disk writePaths'
    Assert-JsonArray $manyMeta.dependencies 2 'many disk dependencies'
    Assert-JsonArray $manyReceipt.changedFiles 2 'many disk changedFiles'
    Assert-JsonArray $manyReceipt.checks 3 'many disk checks'
    Assert-JsonArray $manyReceipt.notRun 2 'many disk notRun'
    Assert-True ($manyReceipt.writesSettled -is [bool] -and $null -eq $manyReceipt.blocker) 'many disk scalars changed'
    $manyResult = Invoke-Entry -Arguments @('-Action','Result','-TaskFile',$manyTask.Task,'-CursorAgentPath',$Stub) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'arrays_many' }
    Assert-JsonArray $manyResult.Json.meta.writePaths 2 'many result writePaths'
    Assert-JsonArray $manyResult.Json.meta.dependencies 2 'many result dependencies'
    Assert-JsonArray $manyResult.Json.receipt.changedFiles 2 'many result changedFiles'
    Assert-JsonArray $manyResult.Json.receipt.checks 3 'many result checks'
    Assert-JsonArray $manyResult.Json.receipt.notRun 2 'many result notRun'
    Assert-JsonArray $manyResult.Json.attemptIds 1 'many result attemptIds'

    $scalarTask = New-TaskFile -TaskId 'taskArrayScalar' -Operation 'read' -WritePaths @('src/One.java')
    $scalarRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $scalarTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'arrays_scalar' }
    Assert-True ($scalarRun.ExitCode -eq 0 -and $scalarRun.Json.reportStatus -eq 'invalid') $scalarRun.Stdout
    $scalarMeta = Read-JsonText (Join-Path ([string]$scalarRun.Json.attemptDir) 'attempt.json')
    Assert-JsonArray $scalarMeta.writePaths 1 'scalar task writePaths'
    Assert-True ($scalarRun.Json.receipt.changedFiles -isnot [System.Array]) 'scalar changedFiles was turned into an array'

    Step 'display-model'
    $displayTask = New-TaskFile -TaskId 'taskDisplay' -Operation 'read'
    $display = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $displayTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'display_model' }
    Assert-True ($display.ExitCode -eq 0 -and $display.Json.lifecycle -eq 'reported' -and $display.Json.meta.model -eq 'grok-4.7-high-fast') $display.Stdout
    Assert-True ($display.Json.meta.requestedModel -eq 'grok-4.7-high-fast' -and $display.Json.meta.reportedModel -eq 'Grok 4.7 256K High Fast') $display.Stdout
    $listTask = New-TaskFile -TaskId 'taskDisplayList' -Operation 'read'
    $listRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $listTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'display_list' }
    Assert-True ($listRun.ExitCode -eq 0 -and $listRun.Json.lifecycle -eq 'reported' -and $listRun.Json.meta.model -eq 'grok-4.7-high-fast') $listRun.Stdout
    Assert-True ([string]$listRun.Json.meta.reportedModel.StartsWith('Grok 4.7  High Fast') -and [string]$listRun.Json.meta.reportedModel -ne 'Grok 4.7 High Fast') $listRun.Stdout

    Step 'orphan-init'
    $orphan = New-TaskFile -TaskId 'taskOrphan' -Operation 'read'
    $orphanAttempt = Join-Path $Workspace ".grok\runs\$Batch\taskOrphan\attempts\aSeedOrphan"
    [void][System.IO.Directory]::CreateDirectory($orphanAttempt)
    Write-Utf8 -Path (Join-Path $orphanAttempt 'attempt.json') -Content '{"lifecycle":"running"}'
    $orphanMark = @(Get-LogEntries).Count
    $orphanStart = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $orphan)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($orphanStart.ExitCode -eq 2 -and $orphanStart.Json.code -eq 'recovery_required') $orphanStart.Stdout
    Assert-True ((-not $orphanStart.Json.message.Contains('使用 -Action Resume')) -and (-not $orphanStart.Json.nextAction.Contains('使用 Start'))) $orphanStart.Stdout
    $orphanResume = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','看到半截目录') + (New-BaseArgs $orphan)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($orphanResume.ExitCode -eq 2 -and $orphanResume.Json.code -eq 'recovery_required') $orphanResume.Stdout
    Assert-True ((-not $orphanResume.Json.nextAction.Contains('使用 Start')) -and (-not $orphanResume.Json.message.Contains('create-chat'))) $orphanResume.Stdout
    Assert-True ((Get-AttemptCount 'taskOrphan') -eq 1) 'orphan handling created or deleted an attempt'
    Assert-True (-not (Test-Path -LiteralPath (Join-Path $Workspace ".grok\runs\$Batch\taskOrphan\task.json"))) 'orphan handling wrote task.json'
    Assert-True (Test-Path -LiteralPath (Join-Path $orphanAttempt 'attempt.json')) 'orphan attempt file was deleted'
    Assert-True (@(Get-EntriesSince $orphanMark).Count -eq 0) 'orphan handling launched the CLI'
    $orphanStatus = Invoke-Entry -Arguments @('-Action','Status','-TaskFile',$orphan.Task,'-CursorAgentPath',$Stub) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($orphanStatus.Json.code -eq 'recovery_required' -and $orphanStatus.Json.needsHuman -eq $true) $orphanStatus.Stdout
    Assert-True ([string]$orphanStatus.Json.paths.attemptPaths -match 'aSeedOrphan') $orphanStatus.Stdout

    Step 'blocked-and-needs-input'
    $blockedTask = New-TaskFile -TaskId 'taskBlocked' -Operation 'implement'
    $blockedRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $blockedTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'blocked' }
    Assert-True ($blockedRun.ExitCode -eq 0 -and $blockedRun.Json.lifecycle -eq 'reported' -and $blockedRun.Json.workStatus -eq 'blocked' -and $blockedRun.Json.pendingCompile -eq $false) $blockedRun.Stdout
    $needsTask = New-TaskFile -TaskId 'taskNeeds' -Operation 'read'
    $needsRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $needsTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'needs_input' }
    Assert-True ($needsRun.ExitCode -eq 0 -and $needsRun.Json.workStatus -eq 'needs_input' -and $needsRun.Json.lifecycle -eq 'reported') $needsRun.Stdout

    Step 'write-mode'
    $mark = @(Get-LogEntries).Count
    $writeTask = New-TaskFile -TaskId 'taskWrite' -Operation 'implement' -WritePaths @('src/Example.java')
    $writeRun = Invoke-Entry -Arguments (@('-Action','Start','-WriteMode') + (New-BaseArgs $writeTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($writeRun.ExitCode -eq 0 -and $writeRun.Json.meta.mode -eq 'write') $writeRun.Stdout
    $writeAgent = @(Get-EntriesSince $mark | Where-Object { (Get-ArgList $_)[0] -eq '--print' })
    Assert-True ($writeAgent.Count -eq 1) 'write mode did not run'
    Assert-AgentContract $writeAgent[0] ([string]$writeRun.Json.sessionId) $false

    Step 'create-chat-failure'
    $createTask = New-TaskFile -TaskId 'taskCreate' -Operation 'read'
    $createRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $createTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'create_fail' }
    Assert-True ($createRun.ExitCode -eq 4 -and $createRun.Json.code -eq 'create_chat_failed') $createRun.Stdout
    Assert-True (-not (Test-Path -LiteralPath (Join-Path $Workspace ".grok\runs\$Batch\taskCreate\task.json"))) 'failed create-chat still committed the task'
    $createRetry = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $createTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'plain' }
    Assert-True ($createRetry.ExitCode -eq 0 -and $createRetry.Json.lifecycle -eq 'reported') $createRetry.Stdout

    Step 'auto-review'
    $mark = @(Get-LogEntries).Count
    $reviewTask = New-TaskFile -TaskId 'taskReview' -Operation 'implement'
    $reviewRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $reviewTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'auto_review' }
    Assert-True ($reviewRun.ExitCode -eq 4 -and $reviewRun.Json.lifecycle -eq 'failed') $reviewRun.Stdout
    Assert-True ([string]$reviewRun.Json.receipt.blocker -match 'Auto-review') $reviewRun.Stdout
    $reviewAgents = @(Get-EntriesSince $mark | Where-Object { (Get-ArgList $_)[0] -eq '--print' })
    Assert-True ($reviewAgents.Count -eq 1) 'auto-review was retried'

    Step 'resume-preconditions'
    $noReason = Invoke-Entry -Arguments (@('-Action','Resume') + (New-BaseArgs $ok)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($noReason.ExitCode -eq 2 -and $noReason.Json.code -eq 'invalid_args') $noReason.Stdout
    Assert-True ((Get-AttemptCount 'taskOk') -eq 1) 'resume without reason created an attempt'
    $identityTask = New-TaskFile -TaskId 'taskIdentity' -Operation 'read'
    $identityRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $identityTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'plain' }
    Assert-True ($identityRun.ExitCode -eq 0) $identityRun.Stdout
    $identityJson = Join-Path $Workspace ".grok\runs\$Batch\taskIdentity\task.json"
    $identityDoc = Read-JsonText $identityJson
    $identityDoc.workspace = $OtherWorkspace
    Write-Utf8 -Path $identityJson -Content ($identityDoc | ConvertTo-Json -Depth 5)
    $mismatch = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','核对身份') + (New-BaseArgs $identityTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($mismatch.ExitCode -eq 2 -and $mismatch.Json.code -eq 'identity_mismatch') $mismatch.Stdout
    Assert-True ((Get-AttemptCount 'taskIdentity') -eq 1) 'identity mismatch created an attempt'
    $badAttempt = Invoke-Entry -Arguments @('-Action','Result','-TaskFile',$ok.Task,'-AttemptId','..\..\outside','-CursorAgentPath',$Stub) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($badAttempt.ExitCode -eq 2 -and $badAttempt.Json.code -eq 'invalid_args') $badAttempt.Stdout

    Step 'session-lock'
    $lockTask = New-TaskFile -TaskId 'taskLock' -Operation 'read'
    $lockRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $lockTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'plain' }
    Assert-True ($lockRun.ExitCode -eq 0) $lockRun.Stdout
    $mark = @(Get-LogEntries).Count
    $sessionLock = Join-Path $Workspace (".grok\runs\_sessions\$($lockRun.Json.sessionId).lock")
    $held = [System.IO.File]::Open($sessionLock, [System.IO.FileMode]::Open, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
    try {
        $conflict = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','锁仍被占用') + (New-BaseArgs $lockTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
        Assert-True ($conflict.ExitCode -eq 3 -and $conflict.Json.code -eq 'lock_conflict') $conflict.Stdout
        Assert-True ((Get-AttemptCount 'taskLock') -eq 1) 'session conflict created an attempt'
        Assert-True (@(Get-EntriesSince $mark).Count -eq 0) 'session conflict still launched the CLI'
    }
    finally { $held.Dispose() }
    $lockResume = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','锁已释放') + (New-BaseArgs $lockTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'plain' }
    Assert-True ($lockResume.ExitCode -eq 0 -and $lockResume.Json.sessionId -eq $lockRun.Json.sessionId) $lockResume.Stdout

    Step 'concurrent-task'
    $sleepTask = New-TaskFile -TaskId 'taskSleep' -Operation 'read'
    $background = Start-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $sleepTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'sleep'; CURSOR_TASK_STUB_SLEEP_MS = '12000' }
    $running = Wait-Meta 'taskSleep' 20
    Assert-True ($null -ne $running -and -not $background.Process.HasExited) 'background start did not reach the CLI'
    $overlap = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $sleepTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($overlap.ExitCode -eq 3 -and $overlap.Json.code -eq 'lock_conflict') $overlap.Stdout
    Assert-True (-not $background.Process.HasExited) 'conflict killed the running process'
    Stop-JobTree $background.Process.Id
    $background.Process.WaitForExit(10000) | Out-Null

    Step 'interrupted'
    $interruptTask = New-TaskFile -TaskId 'taskInterrupt' -Operation 'read'
    $interruptJob = Start-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $interruptTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'sleep'; CURSOR_TASK_STUB_SLEEP_MS = '20000' }
    $interruptMeta = Wait-Meta 'taskInterrupt' 20
    Assert-True ($null -ne $interruptMeta) 'interrupt fixture did not record a pid'
    $owner = [int]$interruptMeta.pid
    $cli = [int]$interruptMeta.cliPid
    Stop-JobTree $owner
    if ($cli -gt 0) { Stop-JobTree $cli }
    $interruptJob.Process.WaitForExit(10000) | Out-Null
    $seen = $null
    foreach ($try in 1..20) {
        $seen = Invoke-Entry -Arguments @('-Action','Status','-TaskFile',$interruptTask.Task,'-CursorAgentPath',$Stub) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
        if ($seen.Json.lifecycle -eq 'interrupted') { break }
        Start-Sleep -Milliseconds 250
    }
    Assert-True ($seen.Json.lifecycle -eq 'interrupted' -and $seen.Json.accepted -eq $false -and $seen.Json.complete -eq $false -and $seen.Json.needsHuman -eq $true -and $seen.Json.processAlive -eq $false) $seen.Stdout
    Assert-True (-not [string]::IsNullOrWhiteSpace([string]$seen.Json.sessionId) -and $seen.Json.PSObject.Properties.Name -notcontains 'meta') 'interrupted status still returned full meta'
    $stored = Read-LatestMeta 'taskInterrupt'
    Assert-True ($stored.lifecycle -eq 'interrupted' -and $stored.inputHash -eq $interruptMeta.inputHash) 'interrupted meta lost the original hash'

    Step 'fallback-rejected-on-start'
    $fallbackStart = New-TaskFile -TaskId 'taskFallbackStart' -Operation 'read'
    $rejectedStart = Invoke-Entry -Arguments (@('-Action','Start','-UseQuotaFallback') + (New-FallbackArgs $fallbackStart)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($rejectedStart.ExitCode -eq 2 -and $rejectedStart.Json.code -eq 'invalid_args') $rejectedStart.Stdout
    Assert-True ((Get-AttemptCount 'taskFallbackStart') -eq 0) 'rejected fallback start created an attempt'

    Step 'quota-and-false-positives'
    $falseScenarios = @(
        @{ Id = 'taskRate'; Scenario = 'rate_limit'; Life = 'failed' },
        @{ Id = 'taskNetwork'; Scenario = 'network'; Life = 'failed' },
        @{ Id = 'taskAuth'; Scenario = 'auth_fail'; Life = 'failed' },
        @{ Id = 'taskCompile'; Scenario = 'compile_fail'; Life = 'failed' },
        @{ Id = 'taskApproval'; Scenario = 'auto_review'; Life = 'failed' },
        @{ Id = 'taskSelfReport'; Scenario = 'quota_in_result'; Life = 'reported' },
        @{ Id = 'taskToolQuote'; Scenario = 'quota_in_tool'; Life = 'reported' },
        @{ Id = 'taskQuotaProtocol'; Scenario = 'quota_protocol'; Life = 'protocol_error' }
    )
    foreach ($falseCase in $falseScenarios) {
        $pair = New-TaskFile -TaskId $falseCase.Id -Operation 'read'
        $mark = @(Get-LogEntries).Count
        $run = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $pair)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = $falseCase.Scenario }
        Assert-True ($run.Json.lifecycle -eq $falseCase.Life) ($falseCase.Id + ' ' + $run.Stdout)
        Assert-True ([string]$run.Json.fallbackEligibility -ne 'eligible') ($falseCase.Id + ' became eligible ' + $run.Stdout)
        $denied = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','false positive must stay','-UseQuotaFallback') + (New-FallbackArgs $pair)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
        $expectedDeny = $(if ($falseCase.Scenario -eq 'auto_review') { 'approval_not_bypassable' } else { 'fallback_not_eligible' })
        Assert-True ($denied.Json.code -eq $expectedDeny) ($falseCase.Id + ' switched ' + $denied.Stdout)
        Assert-True (@(Get-GrokEntries @(Get-EntriesSince $mark)).Count -eq 0) ($falseCase.Id + ' launched grok')
    }

    Step 'quota-switch-once'
    $quotaTask = New-TaskFile -TaskId 'taskQuota' -Operation 'read' -WritePaths @('D:/alianximod/.grok/scripts') -Dependencies @('design-doc')
    $quotaMark = @(Get-LogEntries).Count
    $parentEnvBefore = [Environment]::GetEnvironmentVariable('GROK_WORKFLOWS', 'Process')
    [Environment]::SetEnvironmentVariable('GROK_WORKFLOWS', 'parent-keep', 'Process')
    $quotaRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $quotaTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'quota_stderr' }
    Assert-True ($quotaRun.ExitCode -eq 4 -and $quotaRun.Json.lifecycle -eq 'failed' -and $quotaRun.Json.ok -eq $false) $quotaRun.Stdout
    Assert-True ($quotaRun.Json.fallbackEligibility -eq 'eligible' -and $quotaRun.Json.fallbackUsed -eq $false) $quotaRun.Stdout
    Assert-True ([string]$quotaRun.Json.meta.failureKind -eq 'cursor_quota_exhausted') $quotaRun.Stdout
    Assert-True (-not $quotaRun.Stdout.Contains('SECRET_TOKEN_VALUE')) 'quota response leaked a secret'
    $quotaStderr = [System.IO.File]::ReadAllText((Join-Path $quotaRun.Json.attemptDir 'stderr.log'), $Utf8)
    Assert-True ($quotaStderr.Contains('[redacted]') -and -not $quotaStderr.Contains('SECRET_TOKEN_VALUE')) 'stderr kept a secret'
    $parentAttemptHash = Get-FileHashSha (Join-Path $quotaRun.Json.attemptDir 'attempt.json')
    $quotaStatus = Invoke-Entry -Arguments @('-Action','Status','-TaskFile',$quotaTask.Task,'-CursorAgentPath',$Stub) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($quotaStatus.Json.fallbackEligibility -eq 'eligible' -and $quotaStatus.Json.provider -eq 'cursor' -and $quotaStatus.Json.fallbackUsed -eq $false) $quotaStatus.Stdout
    Assert-True ($quotaStatus.Json.PSObject.Properties.Name -notcontains 'meta' -and -not $quotaStatus.Stdout.Contains('SECRET_TOKEN_VALUE') -and -not $quotaStatus.Stdout.Contains('stderr.log')) 'status leaked raw quota evidence'
    $cursorSession = [string]$quotaRun.Json.sessionId
    $switched = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','对账：Cursor 周额度失败进程已结束，输入未变','-UseQuotaFallback') + (New-FallbackArgs $quotaTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'grok_secret' }
    Assert-True ([Environment]::GetEnvironmentVariable('GROK_WORKFLOWS', 'Process') -eq 'parent-keep') 'parent env was changed'
    [Environment]::SetEnvironmentVariable('GROK_WORKFLOWS', $parentEnvBefore, 'Process')
    Assert-True ($switched.ExitCode -eq 0 -and $switched.Json.lifecycle -eq 'reported' -and $switched.Json.provider -eq 'grok') $switched.Stdout
    Assert-True ($switched.Json.sessionId -ne $cursorSession) 'grok reused the cursor session'
    Assert-True (-not $switched.Stdout.Contains('SECRET_TOKEN_VALUE')) 'grok response leaked a secret'
    $grokCalls = @(Get-GrokEntries @(Get-EntriesSince $quotaMark))
    Assert-True ($grokCalls.Count -eq 1) 'first switch did not call grok once'
    Assert-GrokContract $grokCalls[0] ([string]$switched.Json.sessionId) $true $true
    $afterFirstGrok = @(Get-LogEntries).Count
    $storedTask = Read-JsonText (Join-Path $Workspace ".grok\runs\$Batch\taskQuota\task.json")
    Assert-True ([string]$storedTask.sessionId -eq $cursorSession -and [string]$storedTask.cursorSessionId -eq $cursorSession) 'cursor session field was overwritten'
    Assert-True ([string]$storedTask.providerSessionId -eq [string]$switched.Json.sessionId -and [string]$storedTask.sessions.grok -eq [string]$switched.Json.sessionId) 'provider session was not stored'
    Assert-True ([string]$storedTask.sessions.cursor -eq $cursorSession -and $storedTask.fallbackUsed -eq $true) 'fallback flag missing'
    Assert-True ((Get-FileHashSha (Join-Path $quotaRun.Json.attemptDir 'attempt.json')) -eq $parentAttemptHash) 'quota attempt was rewritten'
    $parentMeta = Read-JsonText (Join-Path $quotaRun.Json.attemptDir 'attempt.json')
    Assert-True ([string]$parentMeta.lifecycle -eq 'failed' -and [string]$parentMeta.failureKind -eq 'cursor_quota_exhausted') 'quota failure was changed into success'
    $grokMeta = Read-LatestMeta 'taskQuota'
    Assert-True ([string]$grokMeta.provider -eq 'grok' -and [string]$grokMeta.model -eq 'grok-4.7' -and [string]$grokMeta.expectedWireModel -eq 'grok-4.7-build-fast' -and [string]$grokMeta.reasoningEffort -eq 'high' -and [string]$grokMeta.permissionMode -eq 'auto') 'grok attempt pin mismatch'
    Assert-True ([string]$grokMeta.cursorSessionId -eq $cursorSession -and [string]$grokMeta.providerSessionId -eq [string]$switched.Json.sessionId) 'attempt session mapping mismatch'
    Assert-True ([string]$grokMeta.effectivePromptHash -ne [string]$grokMeta.promptHash -and -not [string]::IsNullOrWhiteSpace([string]$grokMeta.handoffHash)) 'handoff hash missing'
    $handoffText = [System.IO.File]::ReadAllText((Join-Path $switched.Json.attemptDir 'handoff.json'), $Utf8)
    Assert-True ($handoffText.Contains('cursor_quota_exhausted') -and -not $handoffText.Contains('SECRET_TOKEN_VALUE') -and $handoffText.Contains('"writesSettled": null')) 'handoff was not independent or leaked a secret'
    $secondSwitch = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','second switch must fail','-UseQuotaFallback') + (New-FallbackArgs $quotaTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($secondSwitch.ExitCode -eq 2 -and $secondSwitch.Json.code -eq 'fallback_already_used') $secondSwitch.Stdout
    Assert-True (@(Get-GrokEntries @(Get-EntriesSince $afterFirstGrok)).Count -eq 0) 'second switch launched another grok'
    $afterSecondSwitch = @(Get-LogEntries).Count
    $continued = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','continue same grok session') + (New-FallbackArgs $quotaTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'grok_no_report' }
    Assert-True ($continued.ExitCode -eq 0 -and $continued.Json.sessionId -eq $switched.Json.sessionId -and $continued.Json.reportStatus -eq 'missing') $continued.Stdout
    $resumeCalls = @(Get-GrokEntries @(Get-EntriesSince $afterSecondSwitch))
    Assert-True ($resumeCalls.Count -ge 1) 'grok resume did not run'
    Assert-GrokContract $resumeCalls[$resumeCalls.Count - 1] ([string]$switched.Json.sessionId) $true $false
    $oldQuotaResult = Invoke-Entry -Arguments @('-Action','Result','-TaskFile',$quotaTask.Task,'-AttemptId',$quotaRun.Json.attemptId,'-CursorAgentPath',$Stub) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($oldQuotaResult.Json.lifecycle -eq 'failed' -and -not $oldQuotaResult.Stdout.Contains('SECRET_TOKEN_VALUE')) 'historical quota result changed'

    Step 'quota-error-event-and-write-mode'
    $eventTask = New-TaskFile -TaskId 'taskQuotaEvent' -Operation 'implement' -WritePaths @('D:/alianximod/.grok/scripts')
    $eventRun = Invoke-Entry -Arguments (@('-Action','Start','-WriteMode') + (New-BaseArgs $eventTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'quota_error_event' }
    Assert-True ($eventRun.Json.lifecycle -eq 'failed' -and $eventRun.Json.fallbackEligibility -eq 'eligible') $eventRun.Stdout
    $eventMark = @(Get-LogEntries).Count
    $readFallback = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','write task without WriteMode stays read only','-UseQuotaFallback') + (New-FallbackArgs $eventTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($readFallback.Json.provider -eq 'grok' -and $readFallback.Json.meta.mode -eq 'ask') $readFallback.Stdout
    Assert-GrokContract (Get-GrokEntries @(Get-EntriesSince $eventMark))[0] ([string]$readFallback.Json.sessionId) $true $true
    $writeTask = New-TaskFile -TaskId 'taskQuotaWrite' -Operation 'implement' -WritePaths @('D:/alianximod/.grok/scripts')
    $null = Invoke-Entry -Arguments (@('-Action','Start','-WriteMode') + (New-BaseArgs $writeTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'quota_stderr' }
    $writeMark = @(Get-LogEntries).Count
    $writeFallback = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','explicit write fallback','-UseQuotaFallback','-WriteMode') + (New-FallbackArgs $writeTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'grok_unsettled' }
    Assert-True ($writeFallback.ExitCode -eq 0 -and $writeFallback.Json.lifecycle -eq 'reported' -and $writeFallback.Json.receipt.writesSettled -eq $false -and $writeFallback.Json.workStatus -eq 'reported_done') $writeFallback.Stdout
    Assert-GrokContract (Get-GrokEntries @(Get-EntriesSince $writeMark))[0] ([string]$writeFallback.Json.sessionId) $false $true

    Step 'fallback-guards'
    $guardTask = New-TaskFile -TaskId 'taskGuard' -Operation 'read'
    $guardRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $guardTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'quota_stderr' }
    $guardMetaPath = Join-Path $guardRun.Json.attemptDir 'attempt.json'
    $guardMeta = Read-JsonText $guardMetaPath
    $guardMap = [ordered]@{}
    foreach ($property in $guardMeta.PSObject.Properties) { $guardMap[$property.Name] = $property.Value }
    $guardMap['childPids'] = @($PID)
    Write-Utf8 -Path $guardMetaPath -Content ($guardMap | ConvertTo-Json -Depth 8)
    $guardMark = @(Get-LogEntries).Count
    $aliveGuard = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','child still alive','-UseQuotaFallback') + (New-FallbackArgs $guardTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($aliveGuard.ExitCode -eq 3 -and $aliveGuard.Json.code -eq 'not_settled') $aliveGuard.Stdout
    Assert-True (@(Get-GrokEntries @(Get-EntriesSince $guardMark)).Count -eq 0) 'unsettled guard launched grok'
    $guardMap['childPids'] = @()
    $guardMap.Remove('pid')
    $guardMap.Remove('processStartTime')
    Write-Utf8 -Path $guardMetaPath -Content ($guardMap | ConvertTo-Json -Depth 8)
    $unproven = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','missing process stamp','-UseQuotaFallback') + (New-FallbackArgs $guardTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($unproven.Json.code -eq 'not_settled') $unproven.Stdout

    $inputTask = New-TaskFile -TaskId 'taskInput' -Operation 'read'
    $null = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $inputTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'quota_stderr' }
    $inputJson = Read-JsonText $inputTask.Task
    $inputMap = [ordered]@{}
    foreach ($property in $inputJson.PSObject.Properties) { $inputMap[$property.Name] = $property.Value }
    $inputMap['inputVersion'] = 'changed-input'
    Write-Utf8 -Path $inputTask.Task -Content ($inputMap | ConvertTo-Json -Depth 6)
    $changed = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','input changed','-UseQuotaFallback') + (New-FallbackArgs $inputTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($changed.Json.code -eq 'input_changed') $changed.Stdout

    $lockTask = New-TaskFile -TaskId 'taskFallbackLock' -Operation 'read'
    $null = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $lockTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'quota_stderr' }
    $heldLock = [System.IO.File]::Open((Join-Path $Workspace ".grok\runs\$Batch\taskFallbackLock\task.lock"), [System.IO.FileMode]::OpenOrCreate, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
    try {
        $locked = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','lock held','-UseQuotaFallback') + (New-FallbackArgs $lockTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
        Assert-True ($locked.ExitCode -eq 3 -and $locked.Json.code -eq 'lock_conflict') $locked.Stdout
    }
    finally { $heldLock.Dispose() }

    $uncertainTask = New-TaskFile -TaskId 'taskUncertain' -Operation 'read'
    $uncertainRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $uncertainTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'quota_stderr' }
    $reservedId = 'dddddddd-dddd-dddd-dddd-dddddddddddd'
    $uncertainPath = Join-Path $Workspace ".grok\runs\$Batch\taskUncertain\task.json"
    $uncertainJson = Read-JsonText $uncertainPath
    $uncertainMap = [ordered]@{}
    foreach ($property in $uncertainJson.PSObject.Properties) { $uncertainMap[$property.Name] = $property.Value }
    $uncertainMap['grokLaunch'] = 'starting'
    $uncertainMap['fallbackIntent'] = 'starting'
    $uncertainMap['fallbackUsed'] = $false
    $uncertainMap['activeProvider'] = 'cursor'
    $uncertainMap['sessions'] = [ordered]@{ cursor = [string]$uncertainRun.Json.sessionId; grok = $reservedId }
    Write-Utf8 -Path $uncertainPath -Content ($uncertainMap | ConvertTo-Json -Depth 8)
    $uncertainMark = @(Get-LogEntries).Count
    $uncertainResult = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','uncertain launch','-UseQuotaFallback') + (New-FallbackArgs $uncertainTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($uncertainResult.Json.code -eq 'session_uncertain') $uncertainResult.Stdout
    Assert-True (@(Get-GrokEntries @(Get-EntriesSince $uncertainMark)).Count -eq 0) 'uncertain state launched a second session'
    $uncertainAfter = Read-JsonText $uncertainPath
    Assert-True ([string]$uncertainAfter.sessions.grok -eq $reservedId) 'uncertain state replaced the reserved uuid'

    $reservedTask = New-TaskFile -TaskId 'taskReserved' -Operation 'read'
    $reservedRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $reservedTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'quota_stderr' }
    $knownGrok = 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee'
    $reservedPath = Join-Path $Workspace ".grok\runs\$Batch\taskReserved\task.json"
    $reservedJson = Read-JsonText $reservedPath
    $reservedMap = [ordered]@{}
    foreach ($property in $reservedJson.PSObject.Properties) { $reservedMap[$property.Name] = $property.Value }
    $reservedMap['grokLaunch'] = 'none'
    $reservedMap['fallbackIntent'] = 'reserved'
    $reservedMap['fallbackUsed'] = $false
    $reservedMap['activeProvider'] = 'cursor'
    $reservedMap['sessions'] = [ordered]@{ cursor = [string]$reservedRun.Json.sessionId; grok = $knownGrok }
    Write-Utf8 -Path $reservedPath -Content ($reservedMap | ConvertTo-Json -Depth 8)
    $reservedFallback = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','reuse reserved uuid','-UseQuotaFallback') + (New-FallbackArgs $reservedTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($reservedFallback.ExitCode -eq 0 -and $reservedFallback.Json.sessionId -eq $knownGrok) $reservedFallback.Stdout
    $reservedCalls = @(Get-GrokEntries @(Get-EntriesSince $uncertainMark))
    $matchedReserved = @($reservedCalls | Where-Object { ((Get-ArgList $_) -join ' ') -match [regex]::Escape($knownGrok) -and (Get-ArgList $_) -contains '--session-id' })
    Assert-True ($matchedReserved.Count -eq 1) 'reserved uuid was not reused exactly once'

    Step 'grok-outcomes'
    $outcomeCases = @(
        @{ Id = 'taskGrokError'; Scenario = 'grok_error'; Life = 'failed'; Exit = 4 },
        @{ Id = 'taskGrokMissing'; Scenario = 'grok_missing'; Life = 'missing_result'; Exit = 4 },
        @{ Id = 'taskGrokTrunc'; Scenario = 'grok_truncated'; Life = 'missing_result'; Exit = 4 },
        @{ Id = 'taskGrokConflict'; Scenario = 'grok_conflict'; Life = 'protocol_error'; Exit = 4 },
        @{ Id = 'taskGrokSession'; Scenario = 'grok_bad_session'; Life = 'protocol_error'; Exit = 4 },
        @{ Id = 'taskGrokCwd'; Scenario = 'grok_bad_cwd'; Life = 'protocol_error'; Exit = 4 },
        @{ Id = 'taskGrokMax'; Scenario = 'grok_max_tokens'; Life = 'failed'; Exit = 4 },
        @{ Id = 'taskGrokPlain'; Scenario = 'grok_no_report'; Life = 'reported'; Exit = 0 },
        @{ Id = 'taskGrokTwo'; Scenario = 'grok_two_reports'; Life = 'reported'; Exit = 0 },
        @{ Id = 'taskGrokTool'; Scenario = 'grok_unknown_tool'; Life = 'failed'; Exit = 4 },
        @{ Id = 'taskGrokModel'; Scenario = 'grok_wrong_model'; Life = 'protocol_error'; Exit = 4 },
        @{ Id = 'taskGrokUsage'; Scenario = 'grok_no_usage'; Life = 'reported'; Exit = 0 }
    )
    foreach ($outcomeCase in $outcomeCases) {
        $pair = New-TaskFile -TaskId $outcomeCase.Id -Operation 'read'
        $null = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $pair)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'quota_stderr' }
        $run = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','outcome','-UseQuotaFallback') + (New-FallbackArgs $pair)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = $outcomeCase.Scenario }
        Assert-True ($run.ExitCode -eq $outcomeCase.Exit -and $run.Json.lifecycle -eq $outcomeCase.Life) ($outcomeCase.Id + ' ' + $run.Stdout)
        if ($outcomeCase.Scenario -eq 'grok_max_tokens') { Assert-True ([string]$run.Json.meta.stopReason -eq 'max_tokens' -and [string]$run.Json.workStatus -ne 'reported_done') $run.Stdout }
        if ($outcomeCase.Scenario -eq 'grok_unknown_tool') { Assert-True ([string]$run.Json.meta.failureKind -eq 'tool_contract') $run.Stdout }
        if ($outcomeCase.Scenario -eq 'grok_wrong_model') { Assert-True ([string]$run.Json.meta.reportedModel -match 'grok-4.6' -and $run.Json.lifecycle -eq 'protocol_error') $run.Stdout }
        if ($outcomeCase.Scenario -eq 'grok_no_usage') { Assert-True ([string]$run.Json.meta.modelAssurance -eq 'unverified' -and [string]::IsNullOrWhiteSpace([string]$run.Json.meta.reportedModel)) $run.Stdout }
        if ($outcomeCase.Scenario -eq 'grok_two_reports') { Assert-True ($run.Json.reportStatus -eq 'invalid') $run.Stdout }
        if ($outcomeCase.Scenario -eq 'grok_no_report') { Assert-True ($run.Json.reportStatus -eq 'missing' -and [string]::IsNullOrWhiteSpace([string]$run.Json.workStatus)) $run.Stdout }
    }

    Step 'grok-interrupt-and-race'
    $raceTask = New-TaskFile -TaskId 'taskRace' -Operation 'read'
    $null = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $raceTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'quota_stderr' }
    $raceMark = @(Get-LogEntries).Count
    $raceJob = Start-Entry -Arguments (@('-Action','Resume','-ResumeReason','race','-UseQuotaFallback') + (New-FallbackArgs $raceTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'grok_sleep'; CURSOR_TASK_STUB_SLEEP_MS = '12000' }
    $raceMeta = $null
    $raceDeadline = (Get-Date).AddSeconds(20)
    do {
        $candidate = Read-LatestMeta 'taskRace'
        if ($null -ne $candidate -and [string]$candidate.provider -eq 'grok' -and $null -ne $candidate.cliPid) { $raceMeta = $candidate; break }
        Start-Sleep -Milliseconds 200
    } while ((Get-Date) -lt $raceDeadline)
    Assert-True ($null -ne $raceMeta -and -not $raceJob.Process.HasExited) 'race fallback did not reach grok'
    $raceOverlap = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','race overlap','-UseQuotaFallback') + (New-FallbackArgs $raceTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($raceOverlap.ExitCode -eq 3 -and $raceOverlap.Json.code -eq 'lock_conflict') $raceOverlap.Stdout
    Assert-True (@(Get-GrokEntries @(Get-EntriesSince $raceMark)).Count -eq 1) 'race started two grok sessions'
    Stop-JobTree $raceJob.Process.Id
    $raceJob.Process.WaitForExit(10000) | Out-Null

    $interruptFallback = New-TaskFile -TaskId 'taskGrokInterrupt' -Operation 'read'
    $null = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $interruptFallback)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'quota_stderr' }
    $interruptJob = Start-Entry -Arguments (@('-Action','Resume','-ResumeReason','interrupt grok','-UseQuotaFallback') + (New-FallbackArgs $interruptFallback)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'grok_sleep'; CURSOR_TASK_STUB_SLEEP_MS = '20000' }
    $interruptGrok = $null
    $interruptDeadline = (Get-Date).AddSeconds(20)
    do {
        $candidate = Read-LatestMeta 'taskGrokInterrupt'
        if ($null -ne $candidate -and [string]$candidate.provider -eq 'grok' -and $null -ne $candidate.cliPid) { $interruptGrok = $candidate; break }
        Start-Sleep -Milliseconds 200
    } while ((Get-Date) -lt $interruptDeadline)
    Assert-True ($null -ne $interruptGrok) 'grok interrupt did not record a pid'
    Stop-JobTree ([int]$interruptGrok.pid)
    if ([int]$interruptGrok.cliPid -gt 0) { Stop-JobTree ([int]$interruptGrok.cliPid) }
    $interruptJob.Process.WaitForExit(10000) | Out-Null
    $interruptSeen = $null
    foreach ($try in 1..20) {
        $interruptSeen = Invoke-Entry -Arguments @('-Action','Status','-TaskFile',$interruptFallback.Task,'-CursorAgentPath',$Stub) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
        if ($interruptSeen.Json.lifecycle -eq 'interrupted') { break }
        Start-Sleep -Milliseconds 250
    }
    Assert-True ($interruptSeen.Json.lifecycle -eq 'interrupted' -and $interruptSeen.Json.accepted -eq $false -and $interruptSeen.Json.complete -eq $false) $interruptSeen.Stdout

    Step 'account-evidence'
    $fixtureDir = Join-Path $Workspace '.grok\runs\account-evidence-fixture\simulated-quota'
    [void][System.IO.Directory]::CreateDirectory($fixtureDir)
    Write-Utf8 -Path (Join-Path $fixtureDir 'stderr.log') -Content "ActionRequiredError: Weekly usage limit reached. It resets in 2 days. Buy credits to keep going now.`n"
    Write-Utf8 -Path (Join-Path $fixtureDir 'events.jsonl') -Content ''
    $script:QuotaFixture = $fixtureDir
    function New-AccountEvidence([string]$Name, [hashtable]$Overrides) {
        $document = [ordered]@{
            schemaVersion = 1
            kind = 'cursor_account_quota'
            accountScope = 'cursor-login:local-default'
            currentLoginScope = 'cursor-login:local-default'
            loginScopeNote = '主会话已核对当前登录范围与证据相同。启动器不能自动证明账户身份。'
            sourceKind = 'verified_attempt'
            sourceLocator = $script:QuotaFixture
            confirmedAt = [DateTime]::UtcNow.AddHours(-2).ToString('o')
            validUntil = [DateTime]::UtcNow.AddDays(2).ToString('o')
            invalidated = $false
            invalidReason = $null
        }
        if ($null -ne $Overrides) {
            foreach ($key in $Overrides.Keys) { $document[$key] = $Overrides[$key] }
        }
        $path = Join-Path $InputDir ($Name + '.json')
        Write-Utf8 -Path $path -Content ($document | ConvertTo-Json -Depth 6)
        return $path
    }
    $validEvidence = New-AccountEvidence 'evidence-valid' $null
    $directTask = New-TaskFile -TaskId 'taskDirectGrok' -Operation 'read'
    $directMark = @(Get-LogEntries).Count
    $direct = Invoke-Entry -Arguments (@('-Action','Start','-UseQuotaFallback','-AccountQuotaEvidence',$validEvidence) + (New-FallbackArgs $directTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($direct.ExitCode -eq 0 -and $direct.Json.provider -eq 'grok' -and $direct.Json.lifecycle -eq 'reported') $direct.Stdout
    Assert-True (@(@(Get-EntriesSince $directMark) | Where-Object { (Get-ArgList $_) -contains 'create-chat' }).Count -eq 0) 'direct grok start called create-chat'
    $directCalls = @(Get-GrokEntries @(Get-EntriesSince $directMark))
    Assert-True ($directCalls.Count -eq 1) 'direct grok start did not call grok once'
    Assert-GrokContract $directCalls[0] ([string]$direct.Json.sessionId) $true $true
    $directState = Read-JsonText (Join-Path $Workspace ".grok\runs\$Batch\taskDirectGrok\task.json")
    Assert-True ([string]::IsNullOrWhiteSpace([string]$directState.cursorSessionId) -and [string]::IsNullOrWhiteSpace([string]$directState.sessionId)) 'direct start stored a cursor session'
    Assert-True ([string]$directState.providerSessionId -eq [string]$direct.Json.sessionId -and [string]$directState.sessions.grok -eq [string]$direct.Json.sessionId -and [string]::IsNullOrWhiteSpace([string]$directState.sessions.cursor)) 'direct start session map is wrong'
    Assert-True ($directState.fallbackUsed -eq $true -and [string]$directState.activeProvider -eq 'grok') 'direct start provider state is wrong'
    $directReceipt = [System.IO.File]::ReadAllText((Join-Path $direct.Json.attemptDir 'account-quota-evidence.json'), $Utf8)
    Assert-True ($directReceipt.Contains('"launcherProvesAccount": false') -and $directReceipt.Contains('account-quota-v1') -and -not $directReceipt.Contains('SECRET_TOKEN_VALUE')) 'account receipt missing the non-proof marker'
    $directResumeMark = @(Get-LogEntries).Count
    $directResume = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','continue direct grok') + (New-FallbackArgs $directTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'grok_no_report' }
    Assert-True ($directResume.ExitCode -eq 0 -and $directResume.Json.sessionId -eq $direct.Json.sessionId -and $directResume.Json.provider -eq 'grok') $directResume.Stdout
    $directResumeCalls = @(Get-GrokEntries @(Get-EntriesSince $directResumeMark))
    Assert-True ($directResumeCalls.Count -eq 1) 'direct grok resume did not stay on one call'
    Assert-GrokContract $directResumeCalls[0] ([string]$direct.Json.sessionId) $true $false

    $userEvidence = New-AccountEvidence 'evidence-user' @{ sourceKind = 'user_confirmation'; sourceLocator = 'chat:01a17b3e-040c-79b0-a0eb-0c62559eccf6/message:01a1a41f-4621-7f72-91ee-3943dd3c5d70' }
    $userTask = New-TaskFile -TaskId 'taskUserEvidence' -Operation 'implement' -WritePaths @('D:/alianximod/.grok/scripts')
    $userMark = @(Get-LogEntries).Count
    $userRun = Invoke-Entry -Arguments (@('-Action','Start','-WriteMode','-UseQuotaFallback','-AccountQuotaEvidence',$userEvidence) + (New-FallbackArgs $userTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($userRun.ExitCode -eq 0 -and $userRun.Json.meta.mode -eq 'write') $userRun.Stdout
    Assert-GrokContract (Get-GrokEntries @(Get-EntriesSince $userMark))[0] ([string]$userRun.Json.sessionId) $false $true

    $tlsTask = New-TaskFile -TaskId 'taskTls' -Operation 'read'
    $tlsRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $tlsTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'tls' }
    Assert-True ($tlsRun.Json.lifecycle -eq 'failed' -and [string]$tlsRun.Json.fallbackEligibility -ne 'eligible') $tlsRun.Stdout
    $tlsHash = Get-FileHashSha (Join-Path $tlsRun.Json.attemptDir 'attempt.json')
    $tlsMark = @(Get-LogEntries).Count
    $tlsSwitch = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','TLS is not quota; account evidence is separate','-UseQuotaFallback','-AccountQuotaEvidence',$validEvidence) + (New-FallbackArgs $tlsTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($tlsSwitch.ExitCode -eq 0 -and $tlsSwitch.Json.provider -eq 'grok') $tlsSwitch.Stdout
    Assert-True ((Get-FileHashSha (Join-Path $tlsRun.Json.attemptDir 'attempt.json')) -eq $tlsHash) 'tls attempt was rewritten'
    $tlsMeta = Read-JsonText (Join-Path $tlsRun.Json.attemptDir 'attempt.json')
    Assert-True ([string]$tlsMeta.lifecycle -eq 'failed' -and [string]$tlsMeta.failureKind -ne 'cursor_quota_exhausted') 'tls failure was relabeled as quota'
    Assert-True (@(Get-GrokEntries @(Get-EntriesSince $tlsMark)).Count -eq 1) 'tls account switch did not call grok once'
    Assert-GrokContract (Get-GrokEntries @(Get-EntriesSince $tlsMark))[0] ([string]$tlsSwitch.Json.sessionId) $true $true

    $expired = New-AccountEvidence 'evidence-expired' @{ confirmedAt = [DateTime]::UtcNow.AddDays(-3).ToString('o'); validUntil = [DateTime]::UtcNow.AddHours(-1).ToString('o') }
    $expiredTask = New-TaskFile -TaskId 'taskExpired' -Operation 'read'
    $expiredRun = Invoke-Entry -Arguments (@('-Action','Start','-UseQuotaFallback','-AccountQuotaEvidence',$expired) + (New-FallbackArgs $expiredTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($expiredRun.Json.code -eq 'evidence_expired' -and (Get-AttemptCount 'taskExpired') -eq 0) $expiredRun.Stdout
    $otherAccount = New-AccountEvidence 'evidence-other' @{ currentLoginScope = 'cursor-login:other-account' }
    $otherTask = New-TaskFile -TaskId 'taskOtherAccount' -Operation 'read'
    $otherRun = Invoke-Entry -Arguments (@('-Action','Start','-UseQuotaFallback','-AccountQuotaEvidence',$otherAccount) + (New-FallbackArgs $otherTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($otherRun.Json.code -eq 'account_mismatch' -and (Get-AttemptCount 'taskOtherAccount') -eq 0) $otherRun.Stdout
    $recovered = New-AccountEvidence 'evidence-recovered' @{ invalidated = $true; invalidReason = 'quota_recovered' }
    $recoveredTask = New-TaskFile -TaskId 'taskRecovered' -Operation 'read'
    $recoveredRun = Invoke-Entry -Arguments (@('-Action','Start','-UseQuotaFallback','-AccountQuotaEvidence',$recovered) + (New-FallbackArgs $recoveredTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($recoveredRun.Json.code -eq 'evidence_invalid' -and (Get-AttemptCount 'taskRecovered') -eq 0) $recoveredRun.Stdout
    $backToCursor = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $recoveredTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'plain' }
    Assert-True ($backToCursor.ExitCode -eq 0 -and $backToCursor.Json.provider -eq 'cursor') $backToCursor.Stdout
    $cursorCalls = @(@(Get-EntriesSince $directMark) | Where-Object { (Get-ArgList $_) -contains 'create-chat' -and [string]$_.scenario -eq 'plain' })
    Assert-True ($cursorCalls.Count -ge 1) 'recovered account did not return a new task to cursor'

    $secretPath = Join-Path $InputDir 'evidence-secret.json'
    Write-Utf8 -Path $secretPath -Content '{"schemaVersion":1,"kind":"cursor_account_quota","accountScope":"cursor-login:local-default","currentLoginScope":"cursor-login:local-default","loginScopeNote":"x","sourceKind":"user_confirmation","sourceLocator":"chat:secret","confirmedAt":"2026-11-04T00:00:00Z","validUntil":"2026-11-06T00:00:00Z","invalidated":false,"invalidReason":null,"leak":"Authorization: Bearer SECRET_TOKEN_VALUE"}'
    $secretTask = New-TaskFile -TaskId 'taskSecretEvidence' -Operation 'read'
    $secretRun = Invoke-Entry -Arguments (@('-Action','Start','-UseQuotaFallback','-AccountQuotaEvidence',$secretPath) + (New-FallbackArgs $secretTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($secretRun.Json.code -eq 'unsafe_evidence' -and -not $secretRun.Stdout.Contains('SECRET_TOKEN_VALUE') -and (Get-AttemptCount 'taskSecretEvidence') -eq 0) $secretRun.Stdout

    $approvalTask = New-TaskFile -TaskId 'taskApprovalBypass' -Operation 'read'
    $approvalRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $approvalTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'auto_review' }
    $approvalHash = Get-FileHashSha (Join-Path $approvalRun.Json.attemptDir 'attempt.json')
    $approvalMark = @(Get-LogEntries).Count
    $approvalSwitch = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','approval must stay','-UseQuotaFallback','-AccountQuotaEvidence',$validEvidence) + (New-FallbackArgs $approvalTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($approvalSwitch.Json.code -eq 'approval_not_bypassable') $approvalSwitch.Stdout
    Assert-True ((Get-FileHashSha (Join-Path $approvalRun.Json.attemptDir 'attempt.json')) -eq $approvalHash) 'approval attempt was rewritten'
    Assert-True (@(Get-GrokEntries @(Get-EntriesSince $approvalMark)).Count -eq 0) 'approval bypass launched grok'

    $protocolTask = New-TaskFile -TaskId 'taskProtocolEvidence' -Operation 'read'
    $null = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $protocolTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'quota_protocol' }
    $protocolSwitch = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','protocol stays','-UseQuotaFallback','-AccountQuotaEvidence',$validEvidence) + (New-FallbackArgs $protocolTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($protocolSwitch.Json.code -eq 'protocol_unresolved') $protocolSwitch.Stdout

    Step 'repair-round1'
    $approvalTextTask = New-TaskFile -TaskId 'taskApprovalText' -Operation 'read'
    $approvalTextRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $approvalTextTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'approval_result' }
    $approvalTextHash = Get-FileHashSha (Join-Path $approvalTextRun.Json.attemptDir 'attempt.json')
    $approvalTextMark = @(Get-LogEntries).Count
    $approvalTextSwitch = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','result text approval','-UseQuotaFallback','-AccountQuotaEvidence',$validEvidence) + (New-FallbackArgs $approvalTextTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($approvalTextSwitch.Json.code -eq 'approval_not_bypassable') $approvalTextSwitch.Stdout
    Assert-True ((Get-FileHashSha (Join-Path $approvalTextRun.Json.attemptDir 'attempt.json')) -eq $approvalTextHash) 'approval result attempt was rewritten'
    Assert-True (@(Get-GrokEntries @(Get-EntriesSince $approvalTextMark)).Count -eq 0) 'result-only approval launched grok'

    $mixedTask = New-TaskFile -TaskId 'taskQuotaApproval' -Operation 'read'
    $mixedRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $mixedTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'quota_and_approval' }
    Assert-True ([string]$mixedRun.Json.fallbackEligibility -ne 'eligible') $mixedRun.Stdout
    $mixedHash = Get-FileHashSha (Join-Path $mixedRun.Json.attemptDir 'attempt.json')
    $mixedMark = @(Get-LogEntries).Count
    $mixedSwitch = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','quota plus approval','-UseQuotaFallback') + (New-FallbackArgs $mixedTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($mixedSwitch.Json.code -eq 'approval_not_bypassable') $mixedSwitch.Stdout
    Assert-True ((Get-FileHashSha (Join-Path $mixedRun.Json.attemptDir 'attempt.json')) -eq $mixedHash) 'mixed quota attempt was rewritten'
    Assert-True (@(Get-GrokEntries @(Get-EntriesSince $mixedMark)).Count -eq 0) 'mixed approval launched grok'

    $cancelTask = New-TaskFile -TaskId 'taskCanceled' -Operation 'read'
    $cancelRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $cancelTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'canceled' }
    $cancelHash = Get-FileHashSha (Join-Path $cancelRun.Json.attemptDir 'attempt.json')
    $cancelMark = @(Get-LogEntries).Count
    $cancelSwitch = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','cancel stays','-UseQuotaFallback','-AccountQuotaEvidence',$validEvidence) + (New-FallbackArgs $cancelTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($cancelSwitch.Json.code -eq 'cancelled') $cancelSwitch.Stdout
    Assert-True ((Get-FileHashSha (Join-Path $cancelRun.Json.attemptDir 'attempt.json')) -eq $cancelHash) 'cancel attempt was rewritten'
    Assert-True (@(Get-GrokEntries @(Get-EntriesSince $cancelMark)).Count -eq 0) 'cancel launched grok'

    $permissionTask = New-TaskFile -TaskId 'taskGrokPermission' -Operation 'read'
    $null = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $permissionTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'quota_stderr' }
    $permissionRun = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','grok approval exit 0','-UseQuotaFallback') + (New-FallbackArgs $permissionTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'grok_permission' }
    Assert-True ($permissionRun.Json.lifecycle -eq 'failed' -and [string]$permissionRun.Json.meta.failureKind -eq 'approval_rejected' -and [string]$permissionRun.Json.workStatus -ne 'reported_done') $permissionRun.Stdout
    Assert-True ([string]$permissionRun.Json.meta.blocker -match 'permission required') $permissionRun.Stdout

    $skillTask = New-TaskFile -TaskId 'taskSkillWarn' -Operation 'read'
    $null = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $skillTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'quota_stderr' }
    $skillRun = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','skill prefix is not tools','-UseQuotaFallback') + (New-FallbackArgs $skillTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'grok_skill_warn' }
    Assert-True ($skillRun.ExitCode -eq 0 -and $skillRun.Json.lifecycle -eq 'reported' -and [string]$skillRun.Json.meta.failureKind -ne 'tool_contract') $skillRun.Stdout

    $effortTask = New-TaskFile -TaskId 'taskEffortIgnored' -Operation 'read'
    $null = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $effortTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'quota_stderr' }
    $effortRun = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','ignored high','-UseQuotaFallback') + (New-FallbackArgs $effortTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'grok_effort_ignored' }
    Assert-True ($effortRun.Json.lifecycle -eq 'failed' -and [string]$effortRun.Json.meta.failureKind -eq 'reasoning_contract' -and [string]$effortRun.Json.meta.reasoningAssurance -eq 'ignored' -and [string]$effortRun.Json.meta.reasoningRequested -eq 'high') $effortRun.Stdout

    $missingGrok = Join-Path $Root 'missing-grok.exe'
    $missingTask = New-TaskFile -TaskId 'taskMissingGrok' -Operation 'read'
    $missingRun = Invoke-Entry -Arguments (@('-Action','Start','-UseQuotaFallback','-AccountQuotaEvidence',$validEvidence,'-GrokAgentPath',$missingGrok,'-TaskFile',$missingTask.Task,'-PromptFile',$missingTask.Prompt,'-CursorAgentPath',$Stub)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($missingRun.Json.code -eq 'cli_missing' -and -not (Test-Path -LiteralPath (Join-Path $Workspace ".grok\runs\$Batch\taskMissingGrok\task.json"))) $missingRun.Stdout

    $reservedId = 'ffffffff-ffff-ffff-ffff-ffffffffffff'
    $reservedRoot = Join-Path $Workspace ".grok\runs\$Batch\taskReservedRetry"
    [void][System.IO.Directory]::CreateDirectory($reservedRoot)
    $reservedTaskFile = New-TaskFile -TaskId 'taskReservedRetry' -Operation 'read'
    $reservedDoc = [ordered]@{
        schemaVersion = 2
        batchId = $Batch
        taskId = 'taskReservedRetry'
        workspace = $Workspace
        sessionId = $null
        cursorSessionId = $null
        providerSessionId = $reservedId
        activeProvider = 'grok'
        sessions = [ordered]@{ cursor = $null; grok = $reservedId }
        model = 'grok-4.7'
        fallbackUsed = $false
        fallbackFromAttemptId = $null
        fallbackIntent = 'reserved'
        grokLaunch = 'none'
        createdAt = '2026-11-04T00:00:00.0000000Z'
        latestAttemptId = $null
        attempts = @()
    }
    Write-Utf8 -Path (Join-Path $reservedRoot 'task.json') -Content ($reservedDoc | ConvertTo-Json -Depth 6)
    $reservedMark = @(Get-LogEntries).Count
    $reservedRetry = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','reuse reserved uuid') + (New-FallbackArgs $reservedTaskFile)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($reservedRetry.ExitCode -eq 0 -and $reservedRetry.Json.sessionId -eq $reservedId) $reservedRetry.Stdout
    $reservedCalls = @(Get-GrokEntries @(Get-EntriesSince $reservedMark))
    Assert-True ($reservedCalls.Count -eq 1) 'reserved retry did not reuse one session'
    Assert-GrokContract $reservedCalls[0] $reservedId $true $true

    Step 'legacy-task'
    $legacySession = 'cccccccc-cccc-cccc-cccc-cccccccccccc'
    $legacyRoot = Join-Path $Workspace ".grok\runs\$Batch\taskLegacy"
    $legacyAttempt = Join-Path $legacyRoot 'attempts\aLegacy0001'
    [void][System.IO.Directory]::CreateDirectory($legacyAttempt)
    $legacyTaskDoc = [ordered]@{
        batchId = $Batch
        taskId = 'taskLegacy'
        workspace = $Workspace
        sessionId = $legacySession
        model = 'grok-4.7-high-fast'
        createdAt = '2026-11-03T00:00:00.0000000Z'
        latestAttemptId = 'aLegacy0001'
        attempts = @('aLegacy0001')
    }
    $legacyAttemptDoc = [ordered]@{
        batchId = $Batch
        taskId = 'taskLegacy'
        attemptId = 'aLegacy0001'
        sessionId = $legacySession
        model = 'grok-4.7-high-fast'
        requestedModel = 'grok-4.7-high-fast'
        reportedModel = 'Grok 4.7 256K High Fast'
        workspace = $Workspace
        mode = 'ask'
        stage = 'implementation'
        operation = 'read'
        startedAt = '2026-11-03T00:00:00.0000000Z'
        endedAt = '2026-11-03T00:00:01.0000000Z'
        pid = 1
        processStartTime = '2026-11-03T00:00:00.0000000Z'
        cliPid = 2
        cliProcessStartTime = '2026-11-03T00:00:00.0000000Z'
        exitCode = 0
        isError = $false
        inputHash = 'legacy-hash'
        taskHash = 'legacy-task'
        promptHash = 'legacy-prompt'
        lifecycle = 'reported'
        workStatus = 'reported_done'
        reportStatus = 'valid'
        pendingCompile = $false
        authorization = '用户授权离线启动器测试'
        inputVersion = 'implement-task-20261103'
        recordPath = 'docs/记录.md'
        writePaths = @()
        dependencies = @()
        accepted = $false
        quotaChanged = $false
    }
    Write-Utf8 -Path (Join-Path $legacyRoot 'task.json') -Content ($legacyTaskDoc | ConvertTo-Json -Depth 6)
    $legacyAttemptPath = Join-Path $legacyAttempt 'attempt.json'
    Write-Utf8 -Path $legacyAttemptPath -Content ($legacyAttemptDoc | ConvertTo-Json -Depth 6)
    $legacyHash = Get-FileHashSha $legacyAttemptPath
    $legacyPair = [pscustomobject]@{ Task = (Join-Path $InputDir 'taskLegacy.json'); Prompt = (Join-Path $InputDir 'taskLegacy 提示.md'); TaskId = 'taskLegacy' }
    Write-Utf8 -Path $legacyPair.Task -Content (([ordered]@{
        batchId = $Batch; taskId = 'taskLegacy'; workspace = $Workspace; stage = 'implementation'; operation = 'read'
        authorization = '用户授权离线启动器测试'; inputVersion = 'implement-task-20261103'; recordPath = 'docs/记录.md'
        writePaths = @(); dependencies = @()
    }) | ConvertTo-Json -Depth 5)
    Write-Utf8 -Path $legacyPair.Prompt -Content "提示正文 taskLegacy`nOK_PROMPT_BODY_不应进参数`n"
    $legacyStatus = Invoke-Entry -Arguments @('-Action','Status','-TaskFile',$legacyPair.Task,'-CursorAgentPath',$Stub) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($legacyStatus.ExitCode -eq 0 -and $legacyStatus.Json.lifecycle -eq 'reported' -and $legacyStatus.Json.sessionId -eq $legacySession -and $legacyStatus.Json.provider -eq 'cursor') $legacyStatus.Stdout
    Assert-True ((Get-FileHashSha $legacyAttemptPath) -eq $legacyHash) 'status migrated the legacy attempt'
    $legacyResume = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','legacy cursor resume') + (New-BaseArgs $legacyPair)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'plain' }
    Assert-True ($legacyResume.ExitCode -eq 0 -and $legacyResume.Json.sessionId -eq $legacySession -and $legacyResume.Json.provider -eq 'cursor') $legacyResume.Stdout
    Assert-True ((Get-FileHashSha $legacyAttemptPath) -eq $legacyHash) 'resume migrated the legacy attempt'
    $legacyAgents = @(@(Get-EntriesSince $raceMark) | Where-Object { (Get-ArgList $_) -contains '--resume' -and (Get-ArgList $_) -contains $legacySession -and (Get-ArgList $_) -contains '--auto-review' })
    Assert-True ($legacyAgents.Count -ge 1) 'legacy resume did not stay on cursor'
    Assert-AgentContract $legacyAgents[$legacyAgents.Count - 1] $legacySession $true

    Step 'persistent-ask-recovery'
    $plainTask = New-TaskFile -TaskId 'taskAskPlain' -Operation 'implement' -WritePaths @('src/Example.java') -Dependencies @('sheet-owner')
    $plainStart = Invoke-Entry -Arguments (@('-Action','Start','-WriteMode') + (New-BaseArgs $plainTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'persistent_ask' }
    Assert-True ($plainStart.ExitCode -eq 0 -and $plainStart.Json.lifecycle -eq 'reported' -and $plainStart.Json.workStatus -eq 'blocked' -and $plainStart.Json.meta.mode -eq 'write' -and $plainStart.Json.meta.readOnly -eq $false) $plainStart.Stdout
    $plainSession = [string]$plainStart.Json.sessionId
    $plainMark = @(Get-LogEntries).Count
    $plainResume = Invoke-Entry -Arguments (@('-Action','Resume','-WriteMode','-ResumeReason','普通写续跑不新建会话') + (New-BaseArgs $plainTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($plainResume.ExitCode -eq 0 -and $plainResume.Json.sessionId -eq $plainSession) $plainResume.Stdout
    Assert-True (@(@(Get-EntriesSince $plainMark) | Where-Object { (Get-ArgList $_)[0] -eq 'create-chat' }).Count -eq 0) 'plain write resume created a session'
    $plainAgents = @(@(Get-EntriesSince $plainMark) | Where-Object { (Get-ArgList $_)[0] -eq '--print' })
    Assert-True ($plainAgents.Count -eq 1) 'plain write resume did not run'
    Assert-AgentContract $plainAgents[0] $plainSession $false
    $askOnly = Invoke-Entry -Arguments (@('-Action','Resume','-ResumeReason','普通只读续跑保持ask') + (New-BaseArgs $plainTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($askOnly.ExitCode -eq 0 -and $askOnly.Json.sessionId -eq $plainSession -and $askOnly.Json.meta.mode -eq 'ask') $askOnly.Stdout
    $askAgents = @(@(Get-LogEntries) | Where-Object { (Get-ArgList $_) -contains '--resume' -and (Get-ArgList $_) -contains $plainSession -and (Get-ArgList $_)[0] -eq '--print' })
    Assert-AgentContract $askAgents[$askAgents.Count - 1] $plainSession $true
    $staleRecover = Invoke-Entry -Arguments (@('-Action','Resume','-WriteMode','-RecoverPersistentAsk','-ResumeReason','只能看最新attempt') + (New-BaseArgs $plainTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($staleRecover.ExitCode -eq 2 -and $staleRecover.Json.code -eq 'persistent_ask_required' -and $staleRecover.Json.sessionId -eq $plainSession) $staleRecover.Stdout
    $askTask = New-TaskFile -TaskId 'taskAskRecover' -Operation 'implement' -WritePaths @('src/Example.java') -Dependencies @('sheet-owner')
    $askStart = Invoke-Entry -Arguments (@('-Action','Start','-WriteMode') + (New-BaseArgs $askTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'persistent_ask' }
    Assert-True ($askStart.ExitCode -eq 0 -and $askStart.Json.workStatus -eq 'blocked' -and $askStart.Json.meta.mode -eq 'write') $askStart.Stdout
    $askSession = [string]$askStart.Json.sessionId
    $askAttempt = [string]$askStart.Json.attemptId
    $askAttemptPath = Join-Path $askStart.Json.attemptDir 'attempt.json'
    $askAttemptHash = Get-FileHashSha $askAttemptPath
    $askCreateText = [System.IO.File]::ReadAllText((Join-Path $Workspace ".grok\runs\$Batch\taskAskRecover\session-create.txt"), $Utf8).Trim()
    Assert-True ($askCreateText -eq $askSession) 'original session-create text changed before recovery'
    $beforeRecoverCount = Get-AttemptCount 'taskAskRecover'
    $recoverMark = @(Get-LogEntries).Count
    $recovered = Invoke-Entry -Arguments (@('-Action','Resume','-WriteMode','-RecoverPersistentAsk','-ResumeReason','对账：持久化Ask阻塞已收束，显式恢复同一task') + (New-BaseArgs $askTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'persistent_ask' }
    Assert-True ($recovered.ExitCode -eq 0 -and $recovered.Json.lifecycle -eq 'reported' -and $recovered.Json.provider -eq 'cursor') $recovered.Stdout
    $newSession = [string]$recovered.Json.sessionId
    Assert-True ($newSession -match '^[0-9a-fA-F-]{36}$' -and $newSession -ne $askSession) 'recovery did not create a distinct session'
    Assert-True ((Get-FileHashSha $askAttemptPath) -eq $askAttemptHash) 'recovery rewrote the old attempt'
    Assert-True ([System.IO.File]::ReadAllText((Join-Path $Workspace ".grok\runs\$Batch\taskAskRecover\session-create.txt"), $Utf8).Trim() -eq $askSession) 'recovery overwrote session-create.txt'
    $recoverDoc = Read-JsonText (Join-Path $Workspace ".grok\runs\$Batch\taskAskRecover\task.json")
    Assert-True ([string]$recoverDoc.batchId -eq $Batch -and [string]$recoverDoc.taskId -eq 'taskAskRecover' -and [string]$recoverDoc.cursorSessionId -eq $newSession -and [string]$recoverDoc.model -eq 'grok-4.7-high-fast') 'recovery changed task identity or model'
    Assert-True (@($recoverDoc.attempts) -contains $askAttempt) 'recovery dropped the old attempt'
    $recoveryRecord = $recoverDoc.cursorSessionRecoveries
    if ($recoveryRecord -isnot [System.Array]) { $recoveryRecord = @($recoveryRecord) }
    Assert-True ($recoveryRecord.Count -ge 1 -and [string]$recoveryRecord[0].previousCursorSessionId -eq $askSession -and [string]$recoveryRecord[0].cursorSessionId -eq $newSession) 'recovery record lost old or new session'
    Assert-True ([string]$recovered.Json.meta.recoveredFromSessionId -eq $askSession -and [string]$recovered.Json.meta.sessionRecovery -eq 'persistent-ask') $recovered.Stdout
    $recoverAgents = @(@(Get-EntriesSince $recoverMark) | Where-Object { (Get-ArgList $_)[0] -eq '--print' })
    $recoverCreates = @(@(Get-EntriesSince $recoverMark) | Where-Object { (Get-ArgList $_)[0] -eq 'create-chat' })
    Assert-True ($recoverCreates.Count -eq 1 -and $recoverAgents.Count -eq 1) 'recovery did not use one create-chat and one run'
    Assert-AgentContract $recoverAgents[0] $newSession $false
    $recoverPrompt = [System.IO.File]::ReadAllText((Join-Path $recovered.Json.attemptDir 'cli-prompt.txt'), $Utf8)
    $recoverHandoffPath = Join-Path $recovered.Json.attemptDir 'session-recovery.json'
    Assert-True ($recoverPrompt.Contains($askTask.Task) -and $recoverPrompt.Contains($askTask.Prompt) -and $recoverPrompt.Contains($recoverHandoffPath)) 'recovery prompt missed current inputs'
    Assert-True ($recoverPrompt.Contains('does not apply') -and -not $recoverPrompt.Contains('ignore previous') -and -not $recoverPrompt.Contains('--mode ask')) 'recovery prompt carried a read-only bypass'
    $recoverHandoff = Read-JsonText $recoverHandoffPath
    Assert-True ([string]$recoverHandoff.previousCursorSessionId -eq $askSession -and [string]$recoverHandoff.cursorSessionId -eq $newSession -and [string]$recoverHandoff.reason -eq '对账：持久化Ask阻塞已收束，显式恢复同一task') 'handoff missed session lineage'
    Assert-True ($recoverPrompt.Contains([string]$recoverHandoff.parentReceiptPath)) 'recovery prompt did not name the predecessor receipt'
    $continueMark = @(Get-LogEntries).Count
    $continued = Invoke-Entry -Arguments (@('-Action','Resume','-WriteMode','-ResumeReason','恢复后的当前会话继续') + (New-BaseArgs $askTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($continued.ExitCode -eq 0 -and $continued.Json.sessionId -eq $newSession) $continued.Stdout
    Assert-True (@(@(Get-EntriesSince $continueMark) | Where-Object { (Get-ArgList $_)[0] -eq 'create-chat' }).Count -eq 0) 'ordinary resume after recovery created another session'
    Assert-True ((Get-FileHashSha $askAttemptPath) -eq $askAttemptHash) 'later resume rewrote the old attempt'
    Assert-True ((Get-AttemptCount 'taskAskRecover') -gt $beforeRecoverCount) 'recovery did not keep the old attempts on disk'

    Step 'persistent-ask-rejects'
    $flagTask = New-TaskFile -TaskId 'taskAskFlags' -Operation 'implement' -WritePaths @('src/Example.java')
    $flagStart = Invoke-Entry -Arguments (@('-Action','Start','-WriteMode','-RecoverPersistentAsk') + (New-BaseArgs $flagTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($flagStart.ExitCode -eq 2 -and $flagStart.Json.code -eq 'invalid_args') $flagStart.Stdout
    Assert-True (-not (Test-Path -LiteralPath (Join-Path $Workspace ".grok\runs\$Batch\taskAskFlags\task.json"))) 'Start with recovery created a task'
    $noWrite = Invoke-Entry -Arguments (@('-Action','Resume','-RecoverPersistentAsk','-ResumeReason','缺少写模式') + (New-BaseArgs $askTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($noWrite.ExitCode -eq 2 -and $noWrite.Json.code -eq 'invalid_args') $noWrite.Stdout
    $withQuota = Invoke-Entry -Arguments (@('-Action','Resume','-WriteMode','-RecoverPersistentAsk','-UseQuotaFallback','-ResumeReason','不能同时换渠道') + (New-FallbackArgs $askTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($withQuota.ExitCode -eq 2 -and $withQuota.Json.code -eq 'invalid_args') $withQuota.Stdout
    Assert-True ((Get-FileHashSha $askAttemptPath) -eq $askAttemptHash) 'rejected flags rewrote the old attempt'
    $shapeTask = New-TaskFile -TaskId 'taskAskShape' -Operation 'implement' -WritePaths @('src/Example.java')
    $shapeRun = Invoke-Entry -Arguments (@('-Action','Start','-WriteMode') + (New-BaseArgs $shapeTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'blocked' }
    Assert-True ($shapeRun.ExitCode -eq 0) $shapeRun.Stdout
    $shapeSession = [string]$shapeRun.Json.sessionId
    $shapeMark = @(Get-LogEntries).Count
    $shapeRecover = Invoke-Entry -Arguments (@('-Action','Resume','-WriteMode','-RecoverPersistentAsk','-ResumeReason','普通blocked不是持久化Ask') + (New-BaseArgs $shapeTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($shapeRecover.ExitCode -eq 2 -and $shapeRecover.Json.code -eq 'persistent_ask_required' -and $shapeRecover.Json.sessionId -eq $shapeSession) $shapeRecover.Stdout
    Assert-True (@(@(Get-EntriesSince $shapeMark) | Where-Object { (Get-ArgList $_)[0] -eq 'create-chat' }).Count -eq 0) 'shape mismatch still created a session'
    $readTask = New-TaskFile -TaskId 'taskAskRead' -Operation 'read' -WritePaths @('src/Example.java')
    $readRun = Invoke-Entry -Arguments (@('-Action','Start') + (New-BaseArgs $readTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'persistent_ask' }
    Assert-True ($readRun.ExitCode -eq 0 -and $readRun.Json.meta.mode -eq 'ask') $readRun.Stdout
    $readRecover = Invoke-Entry -Arguments (@('-Action','Resume','-WriteMode','-RecoverPersistentAsk','-ResumeReason','只读会话里的句子不能恢复') + (New-BaseArgs $readTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($readRecover.ExitCode -eq 2 -and $readRecover.Json.code -eq 'persistent_ask_required' -and $readRecover.Json.sessionId -eq $readRun.Json.sessionId) $readRecover.Stdout
    $emptyTask = New-TaskFile -TaskId 'taskAskEmpty' -Operation 'implement' -WritePaths @('src/Example.java')
    $emptyRun = Invoke-Entry -Arguments (@('-Action','Start','-WriteMode') + (New-BaseArgs $emptyTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'persistent_ask' }
    Assert-True ($emptyRun.ExitCode -eq 0) $emptyRun.Stdout
    $emptyDoc = Read-JsonText $emptyTask.Task
    $emptyDoc.writePaths = @()
    Write-Utf8 -Path $emptyTask.Task -Content ($emptyDoc | ConvertTo-Json -Depth 6)
    $emptyRecover = Invoke-Entry -Arguments (@('-Action','Resume','-WriteMode','-RecoverPersistentAsk','-ResumeReason','空写路径') + (New-BaseArgs $emptyTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($emptyRecover.ExitCode -eq 2 -and $emptyRecover.Json.code -eq 'write_not_authorized') $emptyRecover.Stdout
    $scopeTask = New-TaskFile -TaskId 'taskAskScope' -Operation 'implement' -WritePaths @('src/Example.java') -Dependencies @('sheet-owner')
    $scopeRun = Invoke-Entry -Arguments (@('-Action','Start','-WriteMode') + (New-BaseArgs $scopeTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'persistent_ask' }
    Assert-True ($scopeRun.ExitCode -eq 0) $scopeRun.Stdout
    $scopeDoc = Read-JsonText $scopeTask.Task
    $scopeDoc.stage = 'delivery'
    Write-Utf8 -Path $scopeTask.Task -Content ($scopeDoc | ConvertTo-Json -Depth 6)
    $scopeRecover = Invoke-Entry -Arguments (@('-Action','Resume','-WriteMode','-RecoverPersistentAsk','-ResumeReason','阶段不能变') + (New-BaseArgs $scopeTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($scopeRecover.ExitCode -eq 2 -and $scopeRecover.Json.code -eq 'scope_mismatch' -and $scopeRecover.Json.sessionId -eq $scopeRun.Json.sessionId) $scopeRecover.Stdout
    $versionTask = New-TaskFile -TaskId 'taskAskVersion' -Operation 'implement' -WritePaths @('src/Example.java')
    $versionRun = Invoke-Entry -Arguments (@('-Action','Start','-WriteMode') + (New-BaseArgs $versionTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'persistent_ask' }
    Assert-True ($versionRun.ExitCode -eq 0) $versionRun.Stdout
    $versionDoc = Read-JsonText $versionTask.Task
    $versionDoc.inputVersion = 'current-approved-input'
    Write-Utf8 -Path $versionTask.Task -Content ($versionDoc | ConvertTo-Json -Depth 6)
    $versionRecover = Invoke-Entry -Arguments (@('-Action','Resume','-WriteMode','-RecoverPersistentAsk','-ResumeReason','当前批准输入可以不同') + (New-BaseArgs $versionTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($versionRecover.ExitCode -eq 0 -and $versionRecover.Json.sessionId -ne $versionRun.Json.sessionId) $versionRecover.Stdout
    Assert-True ([string]$versionRecover.Json.meta.previousInputVersion -eq 'implement-task-20261103' -and [string]$versionRecover.Json.meta.inputVersion -eq 'current-approved-input') $versionRecover.Stdout
    $settleTask = New-TaskFile -TaskId 'taskAskSettle' -Operation 'implement' -WritePaths @('src/Example.java')
    $settleRun = Invoke-Entry -Arguments (@('-Action','Start','-WriteMode') + (New-BaseArgs $settleTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'persistent_ask' }
    Assert-True ($settleRun.ExitCode -eq 0) $settleRun.Stdout
    $settleMetaPath = Join-Path $settleRun.Json.attemptDir 'attempt.json'
    $settleMeta = Read-JsonText $settleMetaPath
    $settleMeta.cliProcessStartTime = $null
    Write-Utf8 -Path $settleMetaPath -Content ($settleMeta | ConvertTo-Json -Depth 8)
    $settleMark = @(Get-LogEntries).Count
    $settleRecover = Invoke-Entry -Arguments (@('-Action','Resume','-WriteMode','-RecoverPersistentAsk','-ResumeReason','进程戳不足') + (New-BaseArgs $settleTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($settleRecover.ExitCode -eq 3 -and $settleRecover.Json.code -eq 'not_settled' -and $settleRecover.Json.sessionId -eq $settleRun.Json.sessionId) $settleRecover.Stdout
    Assert-True (@(@(Get-EntriesSince $settleMark) | Where-Object { (Get-ArgList $_)[0] -eq 'create-chat' }).Count -eq 0) 'unsettled recovery created a session'
    $liveTask = New-TaskFile -TaskId 'taskAskLive' -Operation 'implement' -WritePaths @('src/Example.java')
    $liveRun = Invoke-Entry -Arguments (@('-Action','Start','-WriteMode') + (New-BaseArgs $liveTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'persistent_ask' }
    Assert-True ($liveRun.ExitCode -eq 0) $liveRun.Stdout
    $liveLock = Join-Path $Workspace ".grok\runs\$Batch\taskAskLive\task.lock"
    $liveHeld = [System.IO.File]::Open($liveLock, [System.IO.FileMode]::Open, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
    try {
        $liveMark = @(Get-LogEntries).Count
        $liveRecover = Invoke-Entry -Arguments (@('-Action','Resume','-WriteMode','-RecoverPersistentAsk','-ResumeReason','活进程不能恢复') + (New-BaseArgs $liveTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
        Assert-True ($liveRecover.ExitCode -eq 3 -and $liveRecover.Json.code -eq 'lock_conflict') $liveRecover.Stdout
        Assert-True (@(@(Get-EntriesSince $liveMark) | Where-Object { (Get-ArgList $_)[0] -eq 'create-chat' }).Count -eq 0) 'live lock still created a session'
        Assert-True ((Get-AttemptCount 'taskAskLive') -eq 1) 'live lock created an attempt'
        $liveDoc = Read-JsonText (Join-Path $Workspace ".grok\runs\$Batch\taskAskLive\task.json")
        Assert-True ([string]$liveDoc.cursorSessionId -eq [string]$liveRun.Json.sessionId) 'live lock switched the session'
    }
    finally { $liveHeld.Dispose() }
    $approvalTask = New-TaskFile -TaskId 'taskAskApproval' -Operation 'implement' -WritePaths @('src/Example.java')
    $approvalRun = Invoke-Entry -Arguments (@('-Action','Start','-WriteMode') + (New-BaseArgs $approvalTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'auto_review' }
    Assert-True ($approvalRun.ExitCode -eq 4) $approvalRun.Stdout
    $approvalRecover = Invoke-Entry -Arguments (@('-Action','Resume','-WriteMode','-RecoverPersistentAsk','-ResumeReason','审批不能绕过') + (New-BaseArgs $approvalTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($approvalRecover.ExitCode -eq 2 -and $approvalRecover.Json.code -eq 'approval_not_bypassable' -and $approvalRecover.Json.sessionId -eq $approvalRun.Json.sessionId) $approvalRecover.Stdout
    $protocolTask = New-TaskFile -TaskId 'taskAskProtocol' -Operation 'implement' -WritePaths @('src/Example.java')
    $protocolRun = Invoke-Entry -Arguments (@('-Action','Start','-WriteMode') + (New-BaseArgs $protocolTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'bad_json' }
    Assert-True ($protocolRun.ExitCode -eq 4 -and $protocolRun.Json.lifecycle -eq 'protocol_error') $protocolRun.Stdout
    $protocolRecover = Invoke-Entry -Arguments (@('-Action','Resume','-WriteMode','-RecoverPersistentAsk','-ResumeReason','协议异常不能恢复') + (New-BaseArgs $protocolTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($protocolRecover.ExitCode -eq 2 -and $protocolRecover.Json.code -eq 'protocol_unresolved') $protocolRecover.Stdout
    $cancelTask = New-TaskFile -TaskId 'taskAskCancel' -Operation 'implement' -WritePaths @('src/Example.java')
    $cancelRun = Invoke-Entry -Arguments (@('-Action','Start','-WriteMode') + (New-BaseArgs $cancelTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'canceled' }
    Assert-True ($cancelRun.ExitCode -eq 4) $cancelRun.Stdout
    $cancelRecover = Invoke-Entry -Arguments (@('-Action','Resume','-WriteMode','-RecoverPersistentAsk','-ResumeReason','取消不能恢复') + (New-BaseArgs $cancelTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($cancelRecover.ExitCode -eq 2 -and $cancelRecover.Json.code -eq 'cancelled' -and $cancelRecover.Json.sessionId -eq $cancelRun.Json.sessionId) $cancelRecover.Stdout
    $quotaTaskAsk = New-TaskFile -TaskId 'taskAskQuota' -Operation 'implement' -WritePaths @('src/Example.java')
    $quotaAskRun = Invoke-Entry -Arguments (@('-Action','Start','-WriteMode') + (New-BaseArgs $quotaTaskAsk)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'quota_stderr' }
    Assert-True ($quotaAskRun.ExitCode -eq 4 -and $quotaAskRun.Json.fallbackEligibility -eq 'eligible') $quotaAskRun.Stdout
    $quotaRecover = Invoke-Entry -Arguments (@('-Action','Resume','-WriteMode','-RecoverPersistentAsk','-ResumeReason','额度不能借恢复换会话') + (New-BaseArgs $quotaTaskAsk)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($quotaRecover.ExitCode -eq 2 -and $quotaRecover.Json.code -eq 'quota_not_recoverable' -and $quotaRecover.Json.sessionId -eq $quotaAskRun.Json.sessionId) $quotaRecover.Stdout
    $grokDocPath = Join-Path $Workspace ".grok\runs\$Batch\taskAskShape\task.json"
    $grokDoc = Read-JsonText $grokDocPath
    $grokDoc.activeProvider = 'grok'
    $grokDoc.fallbackUsed = $true
    $grokDoc.sessions.grok = 'cccccccc-cccc-cccc-cccc-cccccccccccc'
    Write-Utf8 -Path $grokDocPath -Content ($grokDoc | ConvertTo-Json -Depth 8)
    $grokRecover = Invoke-Entry -Arguments (@('-Action','Resume','-WriteMode','-RecoverPersistentAsk','-ResumeReason','Grok不能借这个入口') + (New-BaseArgs $shapeTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'ok' }
    Assert-True ($grokRecover.ExitCode -eq 2 -and $grokRecover.Json.code -eq 'cursor_only') $grokRecover.Stdout
    $failTask = New-TaskFile -TaskId 'taskAskCreateFail' -Operation 'implement' -WritePaths @('src/Example.java')
    $failRun = Invoke-Entry -Arguments (@('-Action','Start','-WriteMode') + (New-BaseArgs $failTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'persistent_ask' }
    Assert-True ($failRun.ExitCode -eq 0) $failRun.Stdout
    $failCount = Get-AttemptCount 'taskAskCreateFail'
    $failRecover = Invoke-Entry -Arguments (@('-Action','Resume','-WriteMode','-RecoverPersistentAsk','-ResumeReason','create-chat失败不能换会话') + (New-BaseArgs $failTask)) -EnvMap @{ CURSOR_TASK_STUB_SCENARIO = 'create_fail' }
    Assert-True ($failRecover.ExitCode -eq 4 -and $failRecover.Json.code -eq 'create_chat_failed') $failRecover.Stdout
    $failDoc = Read-JsonText (Join-Path $Workspace ".grok\runs\$Batch\taskAskCreateFail\task.json")
    Assert-True ([string]$failDoc.cursorSessionId -eq [string]$failRun.Json.sessionId) 'failed create-chat switched the session'
    Assert-True ((Get-AttemptCount 'taskAskCreateFail') -eq $failCount) 'failed create-chat added an attempt'
    Assert-True (@(Get-ChildItem -LiteralPath (Join-Path $Workspace ".grok\runs\$Batch\taskAskCreateFail") -Filter 'session-recovery-failure-*.json').Count -ge 1) 'failed recovery left no sidecar'

    $originalAttempt = 'D:\alianximod\.grok\runs\workflow-collab-20261103\cursor-smoke\attempts\a20261102164454600850b\attempt.json'
    Assert-True ((Get-FileHashSha $originalAttempt) -eq $script:OriginalAttemptHash) 'original smoke attempt was overwritten'
    Assert-True ((Get-FileHashSha 'D:\alianximod\.grok\runs\cooperative-slash-20261103\compile-final\attempts\a2026110318480502362ed\attempt.json') -eq $script:QuotaEvidenceHash) 'historical quota attempt was modified'
    Assert-True ((Get-FileHashSha 'D:\alianximod\.grok\runs\cooperative-slash-20261103\compile-final\attempts\a2026110318480502362ed\stderr.log') -eq $script:QuotaStderrHash) 'historical quota stderr was modified'
    $script:Passed = $true
    [Console]::Out.WriteLine('PASS')
}
catch {
    [Console]::Out.WriteLine('FAIL ' + $_.Exception.Message)
    [Console]::Out.WriteLine($_.ScriptStackTrace)
}
finally {
    foreach ($job in $script:Jobs) {
        try { if (-not $job.HasExited) { Stop-JobTree $job.Id } } catch { }
    }
    if ($script:Passed) {
        if ([string]::IsNullOrWhiteSpace($env:CURSOR_TASK_TEST_ROOT) -and (Test-Path -LiteralPath $Root)) {
            Remove-Item -LiteralPath $Root -Recurse -Force
        }
    }
    else {
        [Console]::Out.WriteLine('LEFT ' + $Root)
    }
}
if (-not $script:Passed) { exit 1 }
exit 0
