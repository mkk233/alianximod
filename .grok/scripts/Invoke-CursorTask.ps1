# Manual task launcher. One Start, Resume, Status, or Result.
# Requires PowerShell 7. Verified host is 7.6.5. Windows PowerShell 5.1 is not supported.
# Primary provider is Cursor, fixed model grok-4.7-high-fast.
# The only fallback is Grok CLI alias grok-4.7 (wire grok-4.7-build-fast, high).
# It starts after this task's own Cursor quota failure, or when the caller passes
# still-valid same-account quota evidence. Evidence expires and does not cross accounts.
# Does not schedule, loop, compile, review, or change batch quota.
#Requires -Version 7.0
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('Start', 'Resume', 'Status', 'Result')]
    [string]$Action,

    [Parameter(Mandatory = $true)]
    [string]$TaskFile,

    [string]$PromptFile,
    [switch]$WriteMode,
    [string]$CursorAgentPath,
    [string]$GrokAgentPath,
    [string]$AttemptId,
    [string]$ResumeReason,
    [switch]$UseQuotaFallback,
    [string]$AccountQuotaEvidence,
    [switch]$RecoverPersistentAsk
)

if ($PSVersionTable.PSVersion.Major -lt 7) {
    [Console]::Error.WriteLine('Invoke-CursorTask.ps1 需要 PowerShell 7。已验证版本是 7.6.5，不支持 Windows PowerShell 5.1。')
    exit 2
}
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$script:ExitCode = 2
$script:TaskLock = $null
$script:SessionLock = $null
$FixedModel = 'grok-4.7-high-fast'
# Exact labels for the fixed request id. Trailing format/control characters may be stripped.
# Internal spacing is significant. Do not match on High or Fast substrings.
$FixedModelLabels = @(
    'grok-4.7-high-fast',
    'Grok 4.7 256K High Fast',
    'Grok 4.7  High Fast'
)
$DefaultCursorAgent = 'C:\Users\baishang\AppData\Local\CursorAgentCodex\2026.10.22-27dcf5f\dist-package\cursor-agent.cmd'
$DefaultGrokAgent = 'C:\Users\baishang\.grok\bin\grok.exe'
$GrokModelAlias = 'grok-4.7'
$GrokWireModel = 'grok-4.7-build-fast'
$GrokReasoning = 'high'
$QuotaRuleVersion = 'cursor-weekly-limit-v1'
$QuotaErrorPattern = 'ActionRequiredError:\s*Weekly usage limit reached\.'
$PersistentAskMarker = 'Ask mode is still active'
$ReasoningIgnoredPattern = '(?i)(does not support effort|no effort levels|ignoring --effort|ignoring --reasoning-effort)'
$GrokReadTools = 'read_file,grep,list_dir'
$GrokWriteTools = 'read_file,grep,list_dir,write,search_replace,run_terminal_cmd,get_task_output,wait_tasks'
$UuidPattern = '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
$Utf8 = New-Object System.Text.UTF8Encoding $false
$Utf8Strict = New-Object System.Text.UTF8Encoding $false, $true

function Close-OwnedLocks {
    if ($script:SessionLock) {
        $script:SessionLock.Dispose()
        $script:SessionLock = $null
    }
    if ($script:TaskLock) {
        $script:TaskLock.Dispose()
        $script:TaskLock = $null
    }
}

function Write-AtomicUtf8 {
    param([string]$Path, [string]$Content)
    $parent = Split-Path -Parent $Path
    if (-not (Test-Path -LiteralPath $parent)) {
        [void][System.IO.Directory]::CreateDirectory($parent)
    }
    $temporary = Join-Path $parent ('.tmp-' + [guid]::NewGuid().ToString('n'))
    [System.IO.File]::WriteAllText($temporary, $Content, $Utf8)
    $overwrite = $false
    foreach ($method in [System.IO.File].GetMethods()) {
        if ($method.Name -eq 'Move' -and $method.GetParameters().Count -eq 3) { $overwrite = $true }
    }
    try {
        if ($overwrite) {
            [System.IO.File]::Move($temporary, $Path, $true)
        }
        elseif ([System.IO.File]::Exists($Path)) {
            $backup = $temporary + '.bak'
            [System.IO.File]::Replace($temporary, $Path, $backup)
            if ([System.IO.File]::Exists($backup)) { [System.IO.File]::Delete($backup) }
        }
        else {
            [System.IO.File]::Move($temporary, $Path)
        }
    }
    catch {
        if ([System.IO.File]::Exists($temporary)) {
            [System.IO.File]::Delete($temporary)
        }
        throw
    }
}

function Write-AtomicJson {
    param([string]$Path, $Object)
    $json = $Object | ConvertTo-Json -Depth 30
    Write-AtomicUtf8 -Path $Path -Content $json
}

function Read-Utf8Text {
    param([string]$Path, [switch]$Strict)
    $encoding = $Utf8
    if ($Strict) { $encoding = $Utf8Strict }
    return [System.IO.File]::ReadAllText($Path, $encoding)
}

function Read-JsonFile {
    param([string]$Path)
    $text = Read-Utf8Text -Path $Path -Strict
    if ([string]::IsNullOrWhiteSpace($text)) {
        throw "JSON 文件是空的: $Path"
    }
    return $text | ConvertFrom-Json
}

function Get-Sha256Bytes {
    param([byte[]]$Bytes)
    $algorithm = [System.Security.Cryptography.SHA256]::Create()
    try {
        $hash = $algorithm.ComputeHash($Bytes)
    }
    finally {
        $algorithm.Dispose()
    }
    return ([System.BitConverter]::ToString($hash)).Replace('-', '').ToLowerInvariant()
}

function Test-StableId {
    param([string]$Value)
    if ([string]::IsNullOrWhiteSpace($Value) -or $Value.Length -gt 80) { return $false }
    if ($Value -notmatch '^[A-Za-z0-9][A-Za-z0-9._-]*$') { return $false }
    if ($Value -match '\.\.') { return $false }
    $reserved = @(
        'CON', 'PRN', 'AUX', 'NUL',
        'COM1', 'COM2', 'COM3', 'COM4', 'COM5', 'COM6', 'COM7', 'COM8', 'COM9',
        'LPT1', 'LPT2', 'LPT3', 'LPT4', 'LPT5', 'LPT6', 'LPT7', 'LPT8', 'LPT9'
    )
    if ($reserved -contains $Value.ToUpperInvariant()) { return $false }
    return $true
}

function Test-ControlFree {
    param([string]$Value, [int]$MaxLength)
    if ([string]::IsNullOrWhiteSpace($Value) -or $Value.Length -gt $MaxLength) { return $false }
    if ($Value -match '[\u0000-\u001F]') { return $false }
    return $true
}

function Test-PathArgSafe {
    param([string]$Value)
    if ([string]::IsNullOrWhiteSpace($Value)) { return $false }
    if ($Value -match '[%"]|[\u0000-\u001F]') { return $false }
    return $true
}

function Get-FullDirectory {
    param([string]$Path)
    $full = [System.IO.Path]::GetFullPath($Path)
    return $full.TrimEnd('\', '/')
}

function Test-IsList {
    param($Value)
    if ($null -eq $Value) { return $false }
    if ($Value -is [string]) { return $false }
    return $Value -is [System.Collections.IEnumerable]
}

function Test-Property {
    param($Object, [string]$Name)
    if ($null -eq $Object) { return $false }
    if ($Object -is [System.Collections.IDictionary]) {
        return $Object.Contains($Name)
    }
    return $null -ne $Object.PSObject.Properties[$Name]
}

function ConvertTo-StringArray {
    param($Value)
    $items = New-Object System.Collections.Generic.List[string]
    foreach ($entry in @($Value)) {
        $items.Add([string]$entry)
    }
    # A bare return enumerates 0- and 1-element arrays into null or a string.
    return ,$items.ToArray()
}

function New-Response {
    param(
        [bool]$Ok,
        [string]$Code,
        [int]$ExitCode,
        [string]$Message,
        [string]$Lifecycle = $null,
        $Meta = $null,
        [string]$WorkStatus = $null,
        [string]$ReportStatus = $null,
        [string]$Feedback = $null,
        $Receipt = $null,
        [string]$AttemptDir = $null,
        [string]$SessionId = $null,
        [string]$Attempt = $null,
        [bool]$PendingCompile = $false,
        [string]$NextAction = $null,
        $AttemptIds = $null,
        $Paths = $null,
        [string]$Provider = $null,
        [string]$FallbackEligibility = $null,
        $FallbackUsed = $null
    )
    $script:ExitCode = $ExitCode
    return [ordered]@{
        ok                   = $Ok
        code                 = $Code
        action               = $Action
        message              = $Message
        accepted             = $false
        quotaChanged         = $false
        complete             = $false
        lifecycle            = $Lifecycle
        workStatus           = $WorkStatus
        reportStatus         = $ReportStatus
        pendingCompile       = $PendingCompile
        sessionId            = $SessionId
        provider             = $Provider
        fallbackEligibility  = $FallbackEligibility
        fallbackUsed         = $FallbackUsed
        attemptId            = $Attempt
        attemptDir           = $AttemptDir
        attemptIds           = $AttemptIds
        nextAction           = $NextAction
        paths                = $Paths
        meta                 = $Meta
        feedback             = $Feedback
        receipt              = $Receipt
    }
}

function Get-ModelLabelCore {
    param([string]$Reported)
    if ([string]::IsNullOrEmpty($Reported)) { return '' }
    $text = $Reported.TrimStart()
    while ($text.Length -gt 0) {
        $last = $text[$text.Length - 1]
        $category = [System.Globalization.CharUnicodeInfo]::GetUnicodeCategory($last)
        $trailing = $category -eq [System.Globalization.UnicodeCategory]::Format -or
            $category -eq [System.Globalization.UnicodeCategory]::Control -or
            [char]::IsWhiteSpace($last)
        if (-not $trailing) { break }
        $text = $text.Substring(0, $text.Length - 1)
    }
    return $text
}

function Test-FixedModelLabel {
    param([string]$Reported)
    $core = Get-ModelLabelCore $Reported
    foreach ($label in $FixedModelLabels) {
        if ($core -ceq $label) { return $true }
    }
    return $false
}

function Get-OrphanState {
    param($Context)
    $taskJsonPath = Join-Path $Context.taskRoot 'task.json'
    $attemptsRoot = Join-Path $Context.taskRoot 'attempts'
    if (Test-Path -LiteralPath $taskJsonPath -PathType Leaf) { return $null }
    if (-not (Test-Path -LiteralPath $attemptsRoot -PathType Container)) { return $null }
    $children = @(Get-ChildItem -LiteralPath $attemptsRoot -Force)
    if ($children.Count -eq 0) { return $null }
    $attemptPaths = New-Object System.Collections.Generic.List[string]
    foreach ($child in $children) { $attemptPaths.Add($child.FullName) }
    return [ordered]@{
        taskRoot      = $Context.taskRoot
        attemptsRoot  = $attemptsRoot
        taskJsonPath  = $taskJsonPath
        taskLockPath  = Join-Path $Context.taskRoot 'task.lock'
        attemptPaths  = $attemptPaths.ToArray()
    }
}

function New-RecoveryResponse {
    param($Orphan)
    $script:ExitCode = 2
    return New-Response -Ok $false -Code 'recovery_required' -ExitCode 2 -Lifecycle 'unknown' -Paths $Orphan `
        -Message '初始化中断：attempt 目录已在，但 task.json 尚未落盘。目录和锁都保留，没有删除或重放。' `
        -NextAction '人工对账 paths 里的目录后自行处理。本工具不会自动补会话，也不会自动清理。'
}

function New-StatusResponse {
    param(
        [bool]$Ok,
        [string]$Code,
        [int]$ExitCode,
        [string]$Message,
        [string]$Lifecycle,
        [string]$BatchId,
        [string]$TaskId,
        [string]$Attempt,
        [string]$SessionId,
        [string]$Model,
        [string]$RequestedModel,
        $ReportedModel,
        $WorkStatus,
        [string]$ReportStatus,
        $ProcessAlive,
        [bool]$NeedsHuman,
        [string]$NextAction,
        $Paths = $null,
        [string]$Provider = 'cursor',
        [bool]$FallbackUsed = $false,
        [string]$FallbackEligibility = 'ineligible'
    )
    $script:ExitCode = $ExitCode
    return [ordered]@{
        ok                  = $Ok
        code                = $Code
        action              = $Action
        message             = $Message
        accepted            = $false
        quotaChanged        = $false
        complete            = $false
        lifecycle           = $Lifecycle
        batchId             = $BatchId
        taskId              = $TaskId
        attemptId           = $Attempt
        sessionId           = $SessionId
        provider            = $Provider
        fallbackUsed        = $FallbackUsed
        fallbackEligibility = $FallbackEligibility
        model               = $Model
        requestedModel      = $RequestedModel
        reportedModel       = $ReportedModel
        workStatus          = $WorkStatus
        reportStatus        = $ReportStatus
        processAlive        = $ProcessAlive
        needsHuman          = $NeedsHuman
        nextAction          = $NextAction
        paths               = $Paths
    }
}

function Open-ExclusiveFile {
    param([string]$Path)
    $parent = Split-Path -Parent $Path
    if (-not (Test-Path -LiteralPath $parent)) {
        [void][System.IO.Directory]::CreateDirectory($parent)
    }
    try {
        return [System.IO.File]::Open(
            $Path,
            [System.IO.FileMode]::OpenOrCreate,
            [System.IO.FileAccess]::ReadWrite,
            [System.IO.FileShare]::None)
    }
    catch [System.IO.IOException] {
        return $null
    }
}

function Write-LockText {
    param($Stream, [string]$Text)
    $bytes = $Utf8.GetBytes($Text)
    $Stream.SetLength(0)
    $Stream.Write($bytes, 0, $bytes.Length)
    $Stream.Flush($true)
}

function Get-ProcessStartIso {
    param([int]$ProcessId)
    try {
        $process = Get-Process -Id $ProcessId -ErrorAction Stop
        return $process.StartTime.ToUniversalTime().ToString('o')
    }
    catch {
        return $null
    }
}

function Test-SameProcess {
    param([int]$ProcessId, [string]$RecordedStart)
    if ($ProcessId -le 0 -or [string]::IsNullOrWhiteSpace($RecordedStart)) { return $false }
    try {
        $process = Get-Process -Id $ProcessId -ErrorAction Stop
        $actual = $process.StartTime.ToUniversalTime()
        $expected = [DateTime]::Parse($RecordedStart, $null, [System.Globalization.DateTimeStyles]::RoundtripKind).ToUniversalTime()
        $delta = [Math]::Abs(($actual - $expected).TotalSeconds)
        return $delta -lt 3
    }
    catch {
        return $false
    }
}

function Get-OwnerAlive {
    param($Meta)
    if (-not (Test-Property $Meta 'pid') -or -not (Test-Property $Meta 'processStartTime')) { return $false }
    if ($null -eq $Meta.pid -or [string]::IsNullOrWhiteSpace([string]$Meta.processStartTime)) { return $false }
    return Test-SameProcess -ProcessId ([int]$Meta.pid) -RecordedStart ([string]$Meta.processStartTime)
}

function Get-CliAlive {
    param($Meta)
    if (-not (Test-Property $Meta 'cliPid') -or -not (Test-Property $Meta 'cliProcessStartTime')) { return $false }
    if ($null -eq $Meta.cliPid -or [string]::IsNullOrWhiteSpace([string]$Meta.cliProcessStartTime)) { return $false }
    return Test-SameProcess -ProcessId ([int]$Meta.cliPid) -RecordedStart ([string]$Meta.cliProcessStartTime)
}

function Resolve-CursorAgentPath {
    if (-not [string]::IsNullOrWhiteSpace($CursorAgentPath)) {
        if (-not (Test-PathArgSafe $CursorAgentPath)) {
            return New-Response -Ok $false -Code 'unsafe_argument' -ExitCode 2 -Message 'CursorAgentPath 含有 %、引号或控制字符，已拒绝调用。' -NextAction '换一个不含这些字符的 CLI 路径。'
        }
        if (-not [System.IO.Path]::IsPathRooted($CursorAgentPath) -or -not (Test-Path -LiteralPath $CursorAgentPath -PathType Leaf)) {
            return New-Response -Ok $false -Code 'cli_missing' -ExitCode 2 -Message "指定的 CLI 不存在: $CursorAgentPath" -NextAction '安装并核对 cursor-agent.cmd，或修正 -CursorAgentPath。不能改模型代替执行。'
        }
        return [ordered]@{ path = [System.IO.Path]::GetFullPath($CursorAgentPath) }
    }
    if (Test-Path -LiteralPath $DefaultCursorAgent -PathType Leaf) {
        return [ordered]@{ path = $DefaultCursorAgent }
    }
    return New-Response -Ok $false -Code 'cli_missing' -ExitCode 2 -Message "找不到固定 CLI: $DefaultCursorAgent" -NextAction '安装已核实的 cursor-agent 后再启动。不能换模型或改由本脚本代替执行。'
}

function Add-ProcessArguments {
    param($StartInfo, [string[]]$Arguments)
    $property = $StartInfo.PSObject.Properties['ArgumentList']
    if ($null -ne $property) {
        foreach ($argument in $Arguments) {
            [void]$StartInfo.ArgumentList.Add($argument)
        }
        return
    }
    $pieces = New-Object System.Collections.Generic.List[string]
    foreach ($argument in $Arguments) {
        if ($argument -notmatch '[\s"]') {
            $pieces.Add($argument)
            continue
        }
        $escaped = $argument -replace '(\\*)"', '$1$1\"' -replace '(\\+)$', '$1$1'
        $pieces.Add('"' + $escaped + '"')
    }
    $StartInfo.Arguments = ($pieces -join ' ')
}

function Invoke-CapturedProcess {
    param(
        [string]$FilePath,
        [string]$WorkingDirectory,
        [string[]]$Arguments,
        [string]$StdoutPath,
        [string]$StderrPath
    )
    foreach ($argument in $Arguments) {
        if ($argument -match '[%]|[\u0000-\u001F]') {
            throw "参数含有 % 或控制字符，已拒绝拼进 CLI。"
        }
    }
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $FilePath
    $startInfo.WorkingDirectory = $WorkingDirectory
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.CreateNoWindow = $true
    Add-ProcessArguments -StartInfo $startInfo -Arguments $Arguments
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    [void]$process.Start()
    $stdoutStream = [System.IO.File]::Create($StdoutPath)
    $stderrStream = [System.IO.File]::Create($StderrPath)
    try {
        $stdoutCopy = $process.StandardOutput.BaseStream.CopyToAsync($stdoutStream)
        $stderrCopy = $process.StandardError.BaseStream.CopyToAsync($stderrStream)
        $process.WaitForExit()
        $stdoutCopy.Wait()
        $stderrCopy.Wait()
        $started = $null
        try { $started = $process.StartTime.ToUniversalTime().ToString('o') } catch { $started = $null }
        return [ordered]@{
            exitCode         = $process.ExitCode
            pid              = $process.Id
            processStartTime = $started
        }
    }
    finally {
        $stdoutStream.Dispose()
        $stderrStream.Dispose()
        $process.Dispose()
    }
}

function Get-LaunchPlan {
    param([string]$AgentPath, [string[]]$AgentArguments)
    $extension = [System.IO.Path]::GetExtension($AgentPath).ToLowerInvariant()
    if ($extension -eq '.ps1') {
        $hostPath = (Get-Process -Id $PID).Path
        $prefix = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $AgentPath)
        return [ordered]@{ fileName = $hostPath; arguments = @($prefix + $AgentArguments) }
    }
    return [ordered]@{ fileName = $AgentPath; arguments = $AgentArguments }
}

function New-AgentArguments {
    param([string]$Workspace, [string]$SessionId, [bool]$AskMode, [string]$Prompt)
    $arguments = New-Object System.Collections.Generic.List[string]
    $arguments.Add('--print')
    $arguments.Add('--model')
    $arguments.Add($FixedModel)
    $arguments.Add('--auto-review')
    $arguments.Add('--workspace')
    $arguments.Add($Workspace)
    $arguments.Add('--output-format')
    $arguments.Add('stream-json')
    $arguments.Add('--resume')
    $arguments.Add($SessionId)
    if ($AskMode) {
        $arguments.Add('--mode')
        $arguments.Add('ask')
    }
    $arguments.Add($Prompt)
    return $arguments.ToArray()
}

function Get-CliPromptContract {
    return 'Follow their exact scope. Do not operate the launcher, run records, or batch quota. Perform compilation, code review, or sheet migration only when that exact action is explicitly authorized by both the task file and prompt file, and obey their stage, quota, and file-ownership constraints. Do not treat compilation or model-reported completion as user game acceptance. In the final answer include exactly one block marked <<<CURSOR_TASK_REPORT>>> then one JSON object then <<<END_CURSOR_TASK_REPORT>>>. Keys are workStatus, summary, changedFiles, checks, notRun, writesSettled, blocker, nextAction. workStatus must be reported_done or blocked or needs_input or failed. Do not invent task, attempt, session, or inputHash. Example: {"workStatus":"reported_done","summary":"","changedFiles":[],"checks":[],"notRun":[],"writesSettled":false,"blocker":null,"nextAction":""}'
}

function New-CliPrompt {
    param([string]$TaskPath, [string]$PromptPath)
    return "Read the complete UTF-8 task file at $TaskPath and the complete UTF-8 prompt file at $PromptPath. $(Get-CliPromptContract)"
}

function New-RecoveryCliPrompt {
    param([string]$TaskPath, [string]$PromptPath, [string]$HandoffPath, [string]$ReceiptPath)
    return "Read the complete UTF-8 task file at $TaskPath and the complete UTF-8 prompt file at $PromptPath. Also read the complete UTF-8 session recovery handoff at $HandoffPath and the predecessor receipt at $ReceiptPath. This is a new Cursor session for the same task. The previous session remained in persisted Ask mode; that read-only positioning does not apply to this session. $(Get-CliPromptContract)"
}

function New-AttemptIdentifier {
    param([string]$AttemptsDirectory)
    for ($try = 0; $try -lt 5; $try++) {
        $suffix = [guid]::NewGuid().ToString('n').Substring(0, 4)
        $identifier = 'a' + [DateTime]::UtcNow.ToString('yyyyMMddHHmmssfff') + $suffix
        $candidate = Join-Path $AttemptsDirectory $identifier
        if (-not (Test-Path -LiteralPath $candidate)) { return $identifier }
        Start-Sleep -Milliseconds 20
    }
    throw '无法分配不冲突的 attemptId。'
}

function Get-TaskContext {
    param([switch]$RequirePrompt)
    if (-not [string]::IsNullOrWhiteSpace($AttemptId) -and $Action -ne 'Result') {
        return New-Response -Ok $false -Code 'invalid_args' -ExitCode 2 -Message '-AttemptId 只用于 Result。'
    }
    if (-not [string]::IsNullOrWhiteSpace($ResumeReason) -and $Action -ne 'Resume') {
        return New-Response -Ok $false -Code 'invalid_args' -ExitCode 2 -Message '-ResumeReason 只用于 Resume。'
    }
    if ($Action -eq 'Resume' -and -not (Test-ControlFree $ResumeReason 500)) {
        return New-Response -Ok $false -Code 'invalid_args' -ExitCode 2 -Message 'Resume 必须提供 ResumeReason，说明已经对账以及为什么接续。' -NextAction '核对原进程和锁之后，用 -ResumeReason 显式接续。本工具不会自动接续，也不会重置额度。'
    }
    if (-not [System.IO.Path]::IsPathRooted($TaskFile) -or -not (Test-Path -LiteralPath $TaskFile -PathType Leaf)) {
        return New-Response -Ok $false -Code 'invalid_task' -ExitCode 2 -Message 'TaskFile 必须是已存在的绝对路径。'
    }
    if (-not (Test-PathArgSafe $TaskFile)) {
        return New-Response -Ok $false -Code 'unsafe_argument' -ExitCode 2 -Message 'TaskFile 含有 %、引号或控制字符。'
    }
    $taskFull = [System.IO.Path]::GetFullPath($TaskFile)
    $taskBytes = [System.IO.File]::ReadAllBytes($taskFull)
    if ($taskBytes.Length -gt 8MB) {
        return New-Response -Ok $false -Code 'invalid_task' -ExitCode 2 -Message 'TaskFile 超过 8MB。'
    }
    try {
        $task = Read-JsonFile -Path $taskFull
    }
    catch {
        return New-Response -Ok $false -Code 'invalid_task' -ExitCode 2 -Message ('TaskFile 不是合法 UTF-8 JSON: ' + $_.Exception.Message)
    }
    foreach ($name in @('batchId', 'taskId', 'workspace', 'stage', 'operation', 'authorization', 'inputVersion', 'recordPath', 'writePaths', 'dependencies')) {
        if (-not (Test-Property $task $name)) {
            return New-Response -Ok $false -Code 'invalid_task' -ExitCode 2 -Message "TaskFile 缺少字段 $name。"
        }
    }
    if (-not (Test-StableId ([string]$task.batchId)) -or -not (Test-StableId ([string]$task.taskId))) {
        return New-Response -Ok $false -Code 'invalid_task' -ExitCode 2 -Message 'batchId 和 taskId 必须是不含路径的 ASCII 稳定 ID。'
    }
    if (([string]$task.stage) -notin @('design', 'implementation', 'delivery')) {
        return New-Response -Ok $false -Code 'invalid_task' -ExitCode 2 -Message 'stage 只能是 design、implementation 或 delivery。'
    }
    if (-not (Test-ControlFree ([string]$task.operation) 80)) {
        return New-Response -Ok $false -Code 'invalid_task' -ExitCode 2 -Message 'operation 无效。'
    }
    foreach ($label in @('authorization', 'inputVersion', 'recordPath')) {
        if (-not (Test-ControlFree ([string]$task.$label) 2000)) {
            return New-Response -Ok $false -Code 'invalid_task' -ExitCode 2 -Message "$label 无效。"
        }
    }
    if (-not (Test-IsList $task.writePaths) -or -not (Test-IsList $task.dependencies)) {
        return New-Response -Ok $false -Code 'invalid_task' -ExitCode 2 -Message 'writePaths 和 dependencies 必须是数组。'
    }
    $writePaths = ConvertTo-StringArray $task.writePaths
    $dependencies = ConvertTo-StringArray $task.dependencies
    if ($writePaths.Count -gt 100 -or $dependencies.Count -gt 50) {
        return New-Response -Ok $false -Code 'invalid_task' -ExitCode 2 -Message 'writePaths 或 dependencies 过多。'
    }
    foreach ($writePath in $writePaths) {
        if (-not (Test-ControlFree $writePath 500)) {
            return New-Response -Ok $false -Code 'invalid_task' -ExitCode 2 -Message 'writePaths 含有空值或控制字符。'
        }
    }
    foreach ($dependency in $dependencies) {
        if (-not (Test-StableId $dependency)) {
            return New-Response -Ok $false -Code 'invalid_task' -ExitCode 2 -Message "dependencies 含有非法 taskId: $dependency"
        }
    }
    if (-not [System.IO.Path]::IsPathRooted([string]$task.workspace) -or -not (Test-PathArgSafe ([string]$task.workspace))) {
        return New-Response -Ok $false -Code 'invalid_task' -ExitCode 2 -Message 'workspace 必须是不含 % 和引号的绝对路径。'
    }
    $workspace = Get-FullDirectory ([string]$task.workspace)
    if (-not (Test-Path -LiteralPath $workspace -PathType Container)) {
        return New-Response -Ok $false -Code 'invalid_task' -ExitCode 2 -Message "workspace 不存在: $workspace"
    }
    if ($WriteMode -and $writePaths.Count -eq 0) {
        return New-Response -Ok $false -Code 'write_not_authorized' -ExitCode 2 -Message 'TaskFile 的 writePaths 为空，拒绝 -WriteMode。' -NextAction '保持默认 ask，或先在任务清单写入已授权路径。'
    }
    $runRoot = Get-FullDirectory (Join-Path $workspace '.grok\runs')
    $taskRoot = Get-FullDirectory (Join-Path (Join-Path $runRoot ([string]$task.batchId)) ([string]$task.taskId))
    $prefix = $runRoot + [System.IO.Path]::DirectorySeparatorChar
    if (-not $taskRoot.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        return New-Response -Ok $false -Code 'invalid_task' -ExitCode 2 -Message 'batchId 或 taskId 越出了运行目录。'
    }
    $promptFull = $null
    $promptBytes = $null
    if ($RequirePrompt) {
        if ([string]::IsNullOrWhiteSpace($PromptFile)) {
            return New-Response -Ok $false -Code 'invalid_args' -ExitCode 2 -Message 'Start 和 Resume 必须提供 PromptFile 绝对路径。'
        }
        if (-not [System.IO.Path]::IsPathRooted($PromptFile) -or -not (Test-Path -LiteralPath $PromptFile -PathType Leaf) -or -not (Test-PathArgSafe $PromptFile)) {
            return New-Response -Ok $false -Code 'invalid_args' -ExitCode 2 -Message 'PromptFile 必须是已存在、不含 % 和引号的绝对路径。'
        }
        $promptFull = [System.IO.Path]::GetFullPath($PromptFile)
        $promptBytes = [System.IO.File]::ReadAllBytes($promptFull)
        if ($promptBytes.Length -gt 8MB) {
            return New-Response -Ok $false -Code 'invalid_args' -ExitCode 2 -Message 'PromptFile 超过 8MB。'
        }
        try {
            $null = $Utf8Strict.GetString($promptBytes)
        }
        catch {
            return New-Response -Ok $false -Code 'invalid_args' -ExitCode 2 -Message 'PromptFile 不是合法 UTF-8。'
        }
    }
    return [ordered]@{
        context      = $true
        task         = $task
        taskFull     = $taskFull
        taskBytes    = $taskBytes
        promptFull   = $promptFull
        promptBytes  = $promptBytes
        workspace    = $workspace
        runRoot      = $runRoot
        taskRoot     = $taskRoot
        writePaths   = $writePaths
        dependencies = $dependencies
        askMode      = -not $WriteMode.IsPresent
        mode         = $(if ($WriteMode) { 'write' } else { 'ask' })
    }
}

function Get-AttemptDirectory {
    param($Context, [string]$Identifier)
    if (-not (Test-StableId $Identifier)) { return $null }
    $attemptsRoot = Join-Path $Context.taskRoot 'attempts'
    $directory = Get-FullDirectory (Join-Path $attemptsRoot $Identifier)
    $prefix = (Get-FullDirectory $attemptsRoot) + [System.IO.Path]::DirectorySeparatorChar
    if (-not $directory.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) { return $null }
    return $directory
}

function Read-AttemptMeta {
    param([string]$Directory)
    $path = Join-Path $Directory 'attempt.json'
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { return $null }
    return Read-JsonFile -Path $path
}

function Get-StoredAttemptIds {
    param($TaskDocument)
    $ids = New-Object System.Collections.Generic.List[string]
    if ((Test-Property $TaskDocument 'attempts') -and (Test-IsList $TaskDocument.attempts)) {
        foreach ($entry in @($TaskDocument.attempts)) {
            $ids.Add([string]$entry)
        }
    }
    return ,$ids.ToArray()
}

function Update-StaleAttempt {
    param([string]$Directory)
    $meta = Read-AttemptMeta -Directory $Directory
    if ($null -eq $meta) { return 'unknown' }
    $stored = [string]$meta.lifecycle
    if ($stored -ne 'running') { return $stored }
    $ownerAlive = Get-OwnerAlive $meta
    $cliAlive = Get-CliAlive $meta
    if ($ownerAlive -or $cliAlive) { return 'running' }
    $hasOwnerStamp = (Test-Property $meta 'pid') -and $null -ne $meta.pid -and (Test-Property $meta 'processStartTime') -and -not [string]::IsNullOrWhiteSpace([string]$meta.processStartTime)
    $observed = $(if ($hasOwnerStamp) { 'interrupted' } else { 'unknown' })
    $map = [ordered]@{}
    foreach ($property in $meta.PSObject.Properties) {
        $map[$property.Name] = $property.Value
    }
    $map['lifecycle'] = $observed
    if ([string]::IsNullOrWhiteSpace([string]$map['endedAt'])) {
        $map['endedAt'] = [DateTime]::UtcNow.ToString('o')
    }
    $map['blocker'] = "原进程已不在或启动时间不符，状态记为 $observed。这不是完成。需要人工对账后才能 Resume。"
    Write-AtomicJson -Path (Join-Path $Directory 'attempt.json') -Object $map
    return $observed
}

function Parse-WorkReport {
    param([string]$Text)
    $pattern = '<<<CURSOR_TASK_REPORT>>>(?<body>.*?)<<<END_CURSOR_TASK_REPORT>>>'
    $found = [regex]::Matches($Text, $pattern, [System.Text.RegularExpressions.RegexOptions]::Singleline)
    if ($found.Count -eq 0) {
        return [ordered]@{ reportStatus = 'missing'; workStatus = $null }
    }
    if ($found.Count -ne 1) {
        return [ordered]@{ reportStatus = 'invalid'; workStatus = $null; blocker = '最终回答里有多段报告标记。' }
    }
    $body = $found[0].Groups['body'].Value.Trim()
    try {
        $report = $body | ConvertFrom-Json
    }
    catch {
        return [ordered]@{ reportStatus = 'invalid'; workStatus = $null; blocker = '报告标记内不是合法 JSON。' }
    }
    $required = @('workStatus', 'summary', 'changedFiles', 'checks', 'notRun', 'writesSettled', 'blocker', 'nextAction')
    foreach ($name in $required) {
        if (-not (Test-Property $report $name)) {
            return [ordered]@{ reportStatus = 'invalid'; workStatus = $null; blocker = "报告缺少 $name。" }
        }
    }
    $status = [string]$report.workStatus
    if ($status -notin @('reported_done', 'blocked', 'needs_input', 'failed')) {
        return [ordered]@{ reportStatus = 'invalid'; workStatus = $null; blocker = 'workStatus 不在允许集合内。' }
    }
    if ($report.summary -isnot [string] -or $report.nextAction -isnot [string]) {
        return [ordered]@{ reportStatus = 'invalid'; workStatus = $null; blocker = 'summary 和 nextAction 必须是字符串。' }
    }
    $blockerValue = $null
    if ($null -eq $report.blocker) {
        $blockerValue = $null
    }
    elseif ($report.blocker -is [string]) {
        $blockerValue = [string]$report.blocker
    }
    else {
        return [ordered]@{ reportStatus = 'invalid'; workStatus = $null; blocker = 'blocker 必须是字符串或 null。' }
    }
    if (-not (Test-IsList $report.changedFiles) -or -not (Test-IsList $report.checks) -or -not (Test-IsList $report.notRun)) {
        return [ordered]@{ reportStatus = 'invalid'; workStatus = $null; blocker = 'changedFiles、checks、notRun 必须是数组。' }
    }
    if ($report.writesSettled -isnot [bool]) {
        return [ordered]@{ reportStatus = 'invalid'; workStatus = $null; blocker = 'writesSettled 必须是布尔值。' }
    }
    return [ordered]@{
        reportStatus   = 'valid'
        workStatus     = $status
        summary        = [string]$report.summary
        changedFiles   = ConvertTo-StringArray $report.changedFiles
        checks         = ConvertTo-StringArray $report.checks
        notRun         = ConvertTo-StringArray $report.notRun
        writesSettled  = [bool]$report.writesSettled
        blocker        = $blockerValue
        nextAction     = [string]$report.nextAction
    }
}

function Read-StreamOutcome {
    param([string]$StdoutPath, [string]$StderrPath, [string]$SessionId, [string]$EventsPath, [string]$ResultEventsPath, [string]$ParseErrorPath)
    Protect-LogFile -Path $StdoutPath
    Protect-LogFile -Path $StderrPath
    $notes = New-Object System.Collections.Generic.List[string]
    $text = ''
    try {
        $text = Read-Utf8Text -Path $StdoutPath -Strict
    }
    catch {
        $notes.Add('stdout 不是合法 UTF-8。')
        $text = ''
    }
    $stderrText = ''
    try {
        $stderrText = Read-Utf8Text -Path $StderrPath -Strict
    }
    catch {
        $notes.Add('stderr 不是合法 UTF-8。')
    }
    $reportedModel = $null
    $eventLines = New-Object System.Collections.Generic.List[string]
    $resultLines = New-Object System.Collections.Generic.List[string]
    $results = New-Object System.Collections.Generic.List[object]
    $errorBits = New-Object System.Collections.Generic.List[string]
    $parseErrors = New-Object System.Collections.Generic.List[string]
    foreach ($rawLine in ($text -split "`n")) {
        $line = $rawLine.TrimEnd("`r")
        if ([string]::IsNullOrWhiteSpace($line)) { continue }
        try {
            $event = $line | ConvertFrom-Json
        }
        catch {
            $parseErrors.Add($line)
            $notes.Add('stdout 含有无法解析的事件行。')
            continue
        }
        $eventLines.Add($line)
        $type = ''
        if (Test-Property $event 'type') { $type = [string]$event.type }
        if ($type -eq 'result') {
            $results.Add($event)
            $resultLines.Add($line)
        }
        elseif ($type -eq 'system' -and (Test-Property $event 'subtype') -and [string]$event.subtype -eq 'init') {
            if ((Test-Property $event 'model') -and -not [string]::IsNullOrWhiteSpace([string]$event.model)) {
                $reportedModel = [string]$event.model
                if (-not (Test-FixedModelLabel $reportedModel)) {
                    $notes.Add("模型与固定值不符: $reportedModel")
                }
            }
            if ((Test-Property $event 'session_id') -and -not [string]::IsNullOrWhiteSpace([string]$event.session_id) -and [string]$event.session_id -ne $SessionId) {
                $notes.Add('init 会话与 --resume 不一致。')
            }
        }
        elseif ($type -eq 'error') {
            $errorBits.Add($line)
        }
    }
    Write-AtomicUtf8 -Path $EventsPath -Content (($eventLines -join "`n") + $(if ($eventLines.Count -gt 0) { "`n" } else { '' }))
    Write-AtomicUtf8 -Path $ResultEventsPath -Content (($resultLines -join "`n") + $(if ($resultLines.Count -gt 0) { "`n" } else { '' }))
    Write-AtomicUtf8 -Path $ParseErrorPath -Content (($parseErrors -join "`n") + $(if ($parseErrors.Count -gt 0) { "`n" } else { '' }))
    if ($results.Count -gt 1) {
        $notes.Add('出现多条 result，不能选取其中一条当作成功。')
    }
    $resultText = $null
    $isError = $null
    if ($results.Count -eq 1) {
        $only = $results[0]
        if (-not (Test-Property $only 'session_id') -or [string]$only.session_id -ne $SessionId) {
            $notes.Add('result 会话与 --resume 不一致。')
        }
        if (-not (Test-Property $only 'is_error') -or $only.is_error -isnot [bool]) {
            $notes.Add('result 缺少布尔 is_error。')
        }
        else {
            $isError = [bool]$only.is_error
        }
        if (-not (Test-Property $only 'result') -or $only.result -isnot [string]) {
            $notes.Add('result 正文不是字符串。')
        }
        else {
            $resultText = [string]$only.result
        }
    }
    return [ordered]@{
        notes         = $notes.ToArray()
        reportedModel = $reportedModel
        resultText    = $resultText
        isError     = $isError
        resultCount = $results.Count
        stderrText  = $stderrText
        errorBits   = $errorBits.ToArray()
    }
}

function New-AttemptMeta {
    param(
        $Context,
        [string]$Identifier,
        [string]$SessionId,
        [string]$InputHash,
        [string]$TaskHash,
        [string]$PromptHash,
        [string]$Provider = 'cursor',
        [string]$ParentAttemptId = $null,
        [string]$SwitchReason = $null,
        [string]$EffectivePromptHash = $null,
        [string]$HandoffHash = $null,
        [string]$CursorSessionId = $null
    )
    $directory = Get-AttemptDirectory -Context $Context -Identifier $Identifier
    $isGrok = $Provider -eq 'grok'
    return [ordered]@{
        batchId              = [string]$Context.task.batchId
        taskId               = [string]$Context.task.taskId
        attemptId            = $Identifier
        provider             = $Provider
        sessionId            = $SessionId
        providerSessionId    = $SessionId
        cursorSessionId      = $(if ($isGrok) { $CursorSessionId } else { $SessionId })
        model                = $(if ($isGrok) { $GrokModelAlias } else { $FixedModel })
        requestedModel       = $(if ($isGrok) { $GrokModelAlias } else { $FixedModel })
        expectedWireModel    = $(if ($isGrok) { $GrokWireModel } else { $null })
        reasoningEffort      = $(if ($isGrok) { $GrokReasoning } else { $null })
        permissionMode       = $(if ($isGrok) { 'auto' } else { 'auto-review' })
        reportedModel        = $null
        modelAssurance       = $null
        stopReason           = $null
        providerRequestId    = $null
        workspace            = $Context.workspace
        mode                 = $Context.mode
        readOnly             = $Context.askMode
        stage                = [string]$Context.task.stage
        operation            = [string]$Context.task.operation
        startedAt            = [DateTime]::UtcNow.ToString('o')
        endedAt              = $null
        pid                  = $PID
        processStartTime     = Get-ProcessStartIso -ProcessId $PID
        cliPid               = $null
        cliProcessStartTime  = $null
        exitCode             = $null
        isError              = $null
        inputHash            = $InputHash
        taskHash             = $TaskHash
        promptHash           = $PromptHash
        effectivePromptHash  = $(if ([string]::IsNullOrWhiteSpace($EffectivePromptHash)) { $PromptHash } else { $EffectivePromptHash })
        handoffHash          = $HandoffHash
        parentAttemptId      = $ParentAttemptId
        switchReason         = $SwitchReason
        failureKind          = $null
        quotaEvidence        = $null
        fallbackEligibility  = 'ineligible'
        receiptPath          = Join-Path $directory 'receipt.json'
        resultPath           = Join-Path $directory 'result.txt'
        stdoutPath           = Join-Path $directory 'stdout.log'
        stderrPath           = Join-Path $directory 'stderr.log'
        lifecycle            = 'running'
        workStatus           = $null
        reportStatus         = 'pending'
        pendingCompile       = $false
        resumeReason         = $(if ($Action -eq 'Resume') { $ResumeReason } else { $null })
        authorization        = [string]$Context.task.authorization
        inputVersion         = [string]$Context.task.inputVersion
        recordPath           = [string]$Context.task.recordPath
        writePaths           = $Context.writePaths
        dependencies         = $Context.dependencies
        resultEventCount     = 0
        blocker              = $null
        accepted             = $false
        quotaChanged         = $false
    }
}

function Get-RedactedText {
    param([string]$Text)
    if ([string]::IsNullOrEmpty($Text)) { return '' }
    $value = $Text
    $value = [regex]::Replace($value, '(?i)(authorization\s*:\s*bearer\s+)\S+', '$1[redacted]')
    $value = [regex]::Replace($value, '(?i)(bearer\s+)[A-Za-z0-9._\-]{8,}', '$1[redacted]')
    $value = [regex]::Replace($value, '(?i)\b(api[_-]?key|token|secret)\s*[:=]\s*\S+', '$1=[redacted]')
    $value = [regex]::Replace($value, '(?i)\bsk-[A-Za-z0-9]{8,}\b', '[redacted]')
    return $value
}

function Protect-LogFile {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { return }
    $text = ''
    try { $text = Read-Utf8Text -Path $Path } catch { return }
    $redacted = Get-RedactedText $text
    if ($redacted -ne $text) {
        Write-AtomicUtf8 -Path $Path -Content $redacted
    }
}

function Test-Uuid {
    param([string]$Value)
    return $Value -match $UuidPattern
}

function Get-ProviderState {
    param($StoredTask)
    $provider = 'cursor'
    if ($StoredTask -and (Test-Property $StoredTask 'activeProvider')) {
        $value = [string]$StoredTask.activeProvider
        if ($value -eq 'grok' -or $value -eq 'cursor') { $provider = $value }
    }
    $cursorSession = $null
    $grokSession = $null
    if ($StoredTask -and (Test-Property $StoredTask 'sessions') -and $null -ne $StoredTask.sessions) {
        if ((Test-Property $StoredTask.sessions 'cursor') -and -not [string]::IsNullOrWhiteSpace([string]$StoredTask.sessions.cursor)) {
            $cursorSession = [string]$StoredTask.sessions.cursor
        }
        if ((Test-Property $StoredTask.sessions 'grok') -and -not [string]::IsNullOrWhiteSpace([string]$StoredTask.sessions.grok)) {
            $grokSession = [string]$StoredTask.sessions.grok
        }
    }
    if ([string]::IsNullOrWhiteSpace($cursorSession) -and $StoredTask -and (Test-Property $StoredTask 'cursorSessionId') -and -not [string]::IsNullOrWhiteSpace([string]$StoredTask.cursorSessionId)) {
        $cursorSession = [string]$StoredTask.cursorSessionId
    }
    if ([string]::IsNullOrWhiteSpace($cursorSession) -and $provider -eq 'cursor' -and $StoredTask -and (Test-Property $StoredTask 'sessionId')) {
        $cursorSession = [string]$StoredTask.sessionId
    }
    $fallbackUsed = $false
    if ($StoredTask -and (Test-Property $StoredTask 'fallbackUsed') -and $StoredTask.fallbackUsed -eq $true) { $fallbackUsed = $true }
    $intent = $null
    if ($StoredTask -and (Test-Property $StoredTask 'fallbackIntent') -and -not [string]::IsNullOrWhiteSpace([string]$StoredTask.fallbackIntent)) {
        $intent = [string]$StoredTask.fallbackIntent
    }
    $launch = 'none'
    if ($StoredTask -and (Test-Property $StoredTask 'grokLaunch') -and -not [string]::IsNullOrWhiteSpace([string]$StoredTask.grokLaunch)) {
        $launch = [string]$StoredTask.grokLaunch
    }
    $fromAttempt = $null
    if ($StoredTask -and (Test-Property $StoredTask 'fallbackFromAttemptId') -and -not [string]::IsNullOrWhiteSpace([string]$StoredTask.fallbackFromAttemptId)) {
        $fromAttempt = [string]$StoredTask.fallbackFromAttemptId
    }
    $recoveries = New-Object System.Collections.Generic.List[object]
    if ($StoredTask -and (Test-Property $StoredTask 'cursorSessionRecoveries') -and $null -ne $StoredTask.cursorSessionRecoveries -and (Test-IsList $StoredTask.cursorSessionRecoveries)) {
        foreach ($entry in @($StoredTask.cursorSessionRecoveries)) {
            if ($null -ne $entry) { $recoveries.Add($entry) }
        }
    }
    return [ordered]@{
        activeProvider        = $provider
        cursorSession         = $cursorSession
        grokSession           = $grokSession
        fallbackUsed          = $fallbackUsed
        fallbackIntent        = $intent
        grokLaunch            = $launch
        fallbackFromAttemptId = $fromAttempt
        cursorSessionRecoveries = $recoveries.ToArray()
    }
}

function New-TaskDocument {
    param($Loaded, $StoredTask, [string[]]$AttemptIds, [string]$LatestAttemptId, $ProviderState, [string]$ActiveProvider, [string]$ProviderSessionId, $AccountQuotaReceipt = $null)
    $created = [DateTime]::UtcNow.ToString('o')
    if ($StoredTask -and (Test-Property $StoredTask 'createdAt') -and -not [string]::IsNullOrWhiteSpace([string]$StoredTask.createdAt)) {
        $created = [string]$StoredTask.createdAt
    }
    $cursorSession = $ProviderState.cursorSession
    if ([string]::IsNullOrWhiteSpace([string]$cursorSession)) { $cursorSession = $null }
    $grokSession = $ProviderState.grokSession
    if ([string]::IsNullOrWhiteSpace([string]$grokSession)) { $grokSession = $null }
    $document = [ordered]@{
        schemaVersion         = 2
        batchId               = [string]$Loaded.task.batchId
        taskId                = [string]$Loaded.task.taskId
        workspace             = $Loaded.workspace
        sessionId             = $cursorSession
        cursorSessionId       = $cursorSession
        providerSessionId     = $ProviderSessionId
        activeProvider        = $ActiveProvider
        sessions              = [ordered]@{ cursor = $cursorSession; grok = $grokSession }
        model                 = $(if ($ActiveProvider -eq 'grok') { $GrokModelAlias } else { $FixedModel })
        fallbackUsed          = [bool]$ProviderState.fallbackUsed
        fallbackFromAttemptId = $ProviderState.fallbackFromAttemptId
        fallbackIntent        = $ProviderState.fallbackIntent
        grokLaunch            = [string]$ProviderState.grokLaunch
        accountQuotaEvidence  = $AccountQuotaReceipt
        createdAt             = $created
        latestAttemptId       = $LatestAttemptId
        attempts              = $AttemptIds
    }
    $recoveries = New-Object System.Collections.Generic.List[object]
    if ($ProviderState -and (Test-Property $ProviderState 'cursorSessionRecoveries') -and $null -ne $ProviderState.cursorSessionRecoveries) {
        foreach ($entry in @($ProviderState.cursorSessionRecoveries)) {
            if ($null -ne $entry) { $recoveries.Add($entry) }
        }
    }
    if ($recoveries.Count -gt 0) {
        $document.cursorSessionRecoveries = $recoveries.ToArray()
    }
    return $document
}

function Get-SessionLockPath {
    param([string]$RunRoot, [string]$Provider, [string]$SessionId)
    $leaf = $(if ($Provider -eq 'grok') { "grok-$SessionId.lock" } else { "$SessionId.lock" })
    return Join-Path (Join-Path $RunRoot '_sessions') $leaf
}

function Resolve-GrokAgentPath {
    if (-not [string]::IsNullOrWhiteSpace($GrokAgentPath)) {
        if (-not (Test-PathArgSafe $GrokAgentPath)) {
            return New-Response -Ok $false -Code 'unsafe_argument' -ExitCode 2 -Message 'GrokAgentPath 含有 %、引号或控制字符，已拒绝调用。' -NextAction '换一个不含这些字符的 CLI 路径。不能用它追加旗标或改模型。'
        }
        if (-not [System.IO.Path]::IsPathRooted($GrokAgentPath) -or -not (Test-Path -LiteralPath $GrokAgentPath -PathType Leaf)) {
            return New-Response -Ok $false -Code 'cli_missing' -ExitCode 2 -Message "指定的 Grok CLI 不存在: $GrokAgentPath" -NextAction '核对 grok.exe，或修正 -GrokAgentPath。不能改模型或换其他 provider。'
        }
        return [ordered]@{ path = [System.IO.Path]::GetFullPath($GrokAgentPath) }
    }
    if (Test-Path -LiteralPath $DefaultGrokAgent -PathType Leaf) {
        return [ordered]@{ path = $DefaultGrokAgent }
    }
    return New-Response -Ok $false -Code 'cli_missing' -ExitCode 2 -Message "找不到固定 Grok CLI: $DefaultGrokAgent" -NextAction '安装已核实的 grok.exe 后再切换。不能降档或换模型。'
}

function New-GrokArguments {
    param([string]$Workspace, [string]$SessionId, [bool]$AskMode, [string]$PromptPath, [bool]$FirstStart)
    $arguments = New-Object System.Collections.Generic.List[string]
    foreach ($pair in @(
            @('--model', $GrokModelAlias),
            @('--reasoning-effort', $GrokReasoning),
            @('--permission-mode', 'auto'),
            @('--cwd', $Workspace),
            @('--prompt-file', $PromptPath),
            @('--output-format', 'json'),
            @('--no-subagents', $null),
            @('--disable-web-search', $null),
            @('--no-auto-update', $null),
            @('--deny', 'MCPTool')
        )) {
        $arguments.Add([string]$pair[0])
        if ($null -ne $pair[1]) { $arguments.Add([string]$pair[1]) }
    }
    if ($FirstStart) {
        $arguments.Add('--session-id')
    }
    else {
        $arguments.Add('--resume')
    }
    $arguments.Add($SessionId)
    if ($AskMode) {
        $arguments.Add('--tools')
        $arguments.Add($GrokReadTools)
        foreach ($deny in @('Edit', 'Write', 'Bash')) {
            $arguments.Add('--deny')
            $arguments.Add($deny)
        }
    }
    else {
        $arguments.Add('--tools')
        $arguments.Add($GrokWriteTools)
    }
    return $arguments.ToArray()
}

function Get-QuotaMatch {
    param([string]$StderrText, [string[]]$ErrorBits)
    $stderr = [string]$StderrText
    $stderrHit = [regex]::IsMatch($stderr, $QuotaErrorPattern)
    $eventLine = $null
    foreach ($bit in @($ErrorBits)) {
        if ([regex]::IsMatch([string]$bit, $QuotaErrorPattern)) {
            $eventLine = [string]$bit
            break
        }
    }
    if (-not $stderrHit -and [string]::IsNullOrWhiteSpace($eventLine)) { return $null }
    $source = $(if ($stderrHit) { 'stderr' } else { 'error_event' })
    $haystack = $(if ($stderrHit) { $stderr } else { $eventLine })
    $match = [regex]::Match($haystack, '(?m)^.*ActionRequiredError:\s*Weekly usage limit reached\..*$')
    $excerpt = $(if ($match.Success) { $match.Value.Trim() } else { '' })
    $excerpt = Get-RedactedText $excerpt
    if ($excerpt.Length -gt 240) { $excerpt = $excerpt.Substring(0, 240) }
    return [ordered]@{
        source      = $source
        ruleVersion = $QuotaRuleVersion
        excerpt     = $excerpt
    }
}

function Get-StoredQuotaEvidence {
    param([string]$Directory)
    $stderrPath = Join-Path $Directory 'stderr.log'
    $eventsPath = Join-Path $Directory 'events.jsonl'
    $stderr = ''
    if (Test-Path -LiteralPath $stderrPath -PathType Leaf) {
        try { $stderr = Read-Utf8Text -Path $stderrPath } catch { $stderr = '' }
    }
    $bits = New-Object System.Collections.Generic.List[string]
    if (Test-Path -LiteralPath $eventsPath -PathType Leaf) {
        $events = ''
        try { $events = Read-Utf8Text -Path $eventsPath } catch { $events = '' }
        foreach ($raw in ($events -split "`n")) {
            $line = $raw.TrimEnd("`r").Trim()
            if ([string]::IsNullOrWhiteSpace($line)) { continue }
            try { $event = $line | ConvertFrom-Json } catch { continue }
            if ((Test-Property $event 'type') -and [string]$event.type -eq 'error') { $bits.Add($line) }
        }
    }
    return Get-QuotaMatch -StderrText $stderr -ErrorBits $bits.ToArray()
}

function Get-ProcessSettlement {
    param($Meta)
    if ((Get-OwnerAlive $Meta) -or (Get-CliAlive $Meta)) { return 'alive' }
    if ((Test-Property $Meta 'childPids') -and $null -ne $Meta.childPids) {
        foreach ($child in @($Meta.childPids)) {
            $idText = [string]$child
            if ([string]::IsNullOrWhiteSpace($idText)) { return 'unproven' }
            $id = 0
            if (-not [int]::TryParse($idText, [ref]$id) -or $id -le 0) { return 'unproven' }
            try {
                $null = Get-Process -Id $id -ErrorAction Stop
                return 'alive'
            }
            catch { }
        }
    }
    $hasOwner = (Test-Property $Meta 'pid') -and $null -ne $Meta.pid -and (Test-Property $Meta 'processStartTime') -and -not [string]::IsNullOrWhiteSpace([string]$Meta.processStartTime)
    $hasCli = (Test-Property $Meta 'cliPid') -and $null -ne $Meta.cliPid -and (Test-Property $Meta 'cliProcessStartTime') -and -not [string]::IsNullOrWhiteSpace([string]$Meta.cliProcessStartTime)
    if (-not $hasOwner -or -not $hasCli) { return 'unproven' }
    return 'settled'
}

function Get-JoinedScope {
    param($Value)
    if ($null -eq $Value) { return '' }
    $parts = New-Object System.Collections.Generic.List[string]
    foreach ($entry in @($Value)) {
        if ($null -eq $entry) { continue }
        $text = [string]$entry
        if ([string]::IsNullOrEmpty($text)) { continue }
        $parts.Add($text)
    }
    return ($parts.ToArray() -join "`n")
}

function Test-SameTaskScope {
    param($AttemptMeta, $Context)
    if ([string]$AttemptMeta.inputVersion -ne [string]$Context.task.inputVersion) { return $false }
    if ([string]$AttemptMeta.stage -ne [string]$Context.task.stage) { return $false }
    if ([string]$AttemptMeta.operation -ne [string]$Context.task.operation) { return $false }
    if ([string]$AttemptMeta.authorization -ne [string]$Context.task.authorization) { return $false }
    $oldPaths = $null
    if ((Test-Property $AttemptMeta 'writePaths') -and $null -ne $AttemptMeta.writePaths) { $oldPaths = $AttemptMeta.writePaths }
    $oldDependencies = $null
    if ((Test-Property $AttemptMeta 'dependencies') -and $null -ne $AttemptMeta.dependencies) { $oldDependencies = $AttemptMeta.dependencies }
    if ((Get-JoinedScope $oldPaths) -ne (Get-JoinedScope $Context.writePaths)) { return $false }
    if ((Get-JoinedScope $oldDependencies) -ne (Get-JoinedScope $Context.dependencies)) { return $false }
    return $true
}

function Get-TopLevelJson {
    param([string]$Text)
    $trimmed = [string]$Text
    if ($null -eq $trimmed) { $trimmed = '' }
    $trimmed = $trimmed.Trim()
    if ([string]::IsNullOrWhiteSpace($trimmed)) {
        return [ordered]@{ values = @(); messy = $false }
    }
    try {
        $parsed = $trimmed | ConvertFrom-Json
        if ($parsed -is [System.Array]) {
            return [ordered]@{ values = @($parsed); messy = $false }
        }
        return [ordered]@{ values = @($parsed); messy = $false }
    }
    catch {
        $values = New-Object System.Collections.Generic.List[object]
        $saw = $false
        foreach ($raw in ($trimmed -split "`n")) {
            $line = $raw.TrimEnd("`r").Trim()
            if ([string]::IsNullOrWhiteSpace($line)) { continue }
            $saw = $true
            try {
                $values.Add(($line | ConvertFrom-Json))
            }
            catch {
                return [ordered]@{ values = @(); messy = $true }
            }
        }
        if (-not $saw) { return [ordered]@{ values = @(); messy = $true } }
        return [ordered]@{ values = $values.ToArray(); messy = $false }
    }
}

function Read-GrokOutcome {
    param([string]$StdoutPath, [string]$StderrPath, [string]$SessionId, [string]$Workspace, [string]$ProviderResultPath)
    Protect-LogFile -Path $StdoutPath
    Protect-LogFile -Path $StderrPath
    $stdout = ''
    $stderr = ''
    $notes = New-Object System.Collections.Generic.List[string]
    try { $stdout = Read-Utf8Text -Path $StdoutPath -Strict } catch { $notes.Add('stdout 不是合法 UTF-8。'); $stdout = '' }
    try { $stderr = Read-Utf8Text -Path $StderrPath -Strict } catch { $notes.Add('stderr 不是合法 UTF-8。'); $stderr = '' }
    $stderr = Get-RedactedText $stderr
    $toolContract = Test-ToolAllowlistWarning -Stderr $stderr
    $reasoningIgnored = Test-ReasoningIgnored -Stderr $stderr
    $approvalRejected = $stderr -match '(?i)(Auto-review|permission required|cannot ask for approval)'
    $parsed = Get-TopLevelJson -Text $stdout
    $values = @($parsed.values)
    $result = $null
    if ($parsed.messy) {
        $values = @()
    }
    elseif ($values.Count -gt 1) {
        $notes.Add('出现多个 JSON 终局，不能选取其中一条当作成功。')
    }
    elseif ($values.Count -eq 1) {
        $result = $values[0]
    }
    $stopReason = $null
    $text = $null
    $returnedSession = $null
    $requestId = $null
    $returnedCwd = $null
    $errorObject = $false
    $modelAssurance = 'unverified'
    $reportedModel = $null
    if ($null -ne $result) {
        if ((Test-Property $result 'type') -and [string]$result.type -eq 'error') { $errorObject = $true }
        if (Test-Property $result 'error') { $errorObject = $true }
        if (Test-Property $result 'stopReason') { $stopReason = [string]$result.stopReason }
        if ((Test-Property $result 'text') -and $result.text -is [string]) { $text = [string]$result.text }
        if (Test-Property $result 'sessionId') { $returnedSession = [string]$result.sessionId }
        if (Test-Property $result 'requestId') { $requestId = [string]$result.requestId }
        if (Test-Property $result 'cwd') { $returnedCwd = [string]$result.cwd }
        if ($errorObject -eq $false) {
            if (-not [string]::IsNullOrWhiteSpace($returnedSession) -and $returnedSession -ne $SessionId) {
                $notes.Add('终局 sessionId 与本次 Grok 会话不一致。')
            }
            if (-not [string]::IsNullOrWhiteSpace($returnedCwd)) {
                $actualCwd = Get-FullDirectory $returnedCwd
                if ($actualCwd -ne $Workspace) { $notes.Add('终局 cwd 与任务 workspace 不一致。') }
            }
        }
        elseif (-not [string]::IsNullOrWhiteSpace($returnedSession) -and $returnedSession -ne $SessionId) {
            $notes.Add('错误对象的 sessionId 与本次 Grok 会话不一致。')
        }
        if ((Test-Property $result 'modelUsage') -and $null -ne $result.modelUsage) {
            $names = @($result.modelUsage.PSObject.Properties.Name)
            $foreign = @($names | Where-Object { $_ -ne $GrokWireModel })
            if ($names.Count -eq 0) {
                $modelAssurance = 'unverified'
            }
            elseif ($foreign.Count -gt 0) {
                $modelAssurance = 'mismatch'
                $reportedModel = ($names -join ',')
                $notes.Add("modelUsage 含有未批准模型: $reportedModel")
            }
            else {
                $modelAssurance = 'matched'
                $reportedModel = $GrokWireModel
            }
        }
        $stored = [ordered]@{}
        foreach ($property in $result.PSObject.Properties) {
            if ($property.Name -eq 'thought') { continue }
            $stored[$property.Name] = $property.Value
        }
        if (-not [string]::IsNullOrWhiteSpace($ProviderResultPath)) {
            Write-AtomicJson -Path $ProviderResultPath -Object $stored
        }
    }
    return [ordered]@{
        notes          = $notes.ToArray()
        stderrText     = $stderr
        result         = $result
        resultText     = $text
        stopReason     = $stopReason
        sessionId      = $returnedSession
        requestId      = $requestId
        cwd            = $returnedCwd
        errorObject    = $errorObject
        modelAssurance = $modelAssurance
        reportedModel  = $reportedModel
        toolContract      = $toolContract
        reasoningIgnored  = $reasoningIgnored
        approvalRejected  = $approvalRejected
        resultCount       = $values.Count
    }
}

function New-GrokHandoff {
    param(
        $Context,
        [string]$Directory,
        $ParentMeta,
        [string]$ParentAttemptId,
        $Evidence,
        [string]$Settlement,
        [string]$HandoffKind = 'cursor-to-grok',
        [string]$FailureKind = 'cursor_quota_exhausted',
        [string]$Note = 'Quota failure stays failed. This handoff does not invent writesSettled or a successful Cursor terminal.'
    )
    $handoff = [ordered]@{
        kind              = $HandoffKind
        parentAttemptId   = $ParentAttemptId
        parentLifecycle   = $(if ($ParentMeta) { [string]$ParentMeta.lifecycle } else { $null })
        failureKind       = $FailureKind
        quotaEvidence     = $Evidence
        resumeReason      = $ResumeReason
        inputVersion      = [string]$Context.task.inputVersion
        stage             = [string]$Context.task.stage
        operation         = [string]$Context.task.operation
        authorization     = [string]$Context.task.authorization
        writePaths        = @($Context.writePaths)
        dependencies      = @($Context.dependencies)
        mode              = $Context.mode
        processSettlement = $Settlement
        ownership         = 'launcher records writePaths only; it does not grant an OS sandbox or release authors'
        partialChanges    = 'not assessed by launcher'
        writesSettled     = $null
        note              = $Note
    }
    $path = Join-Path $Directory 'handoff.json'
    Write-AtomicJson -Path $path -Object $handoff
    $bytes = [System.IO.File]::ReadAllBytes($path)
    return [ordered]@{ path = $path; hash = (Get-Sha256Bytes -Bytes $bytes) }
}

function New-GrokPrompt {
    param([string]$TaskPath, [string]$PromptPath, [string]$HandoffPath, [string]$Directory)
    $body = @"
Read the complete UTF-8 task file at $TaskPath, the complete UTF-8 prompt file at $PromptPath, and the handoff file at $HandoffPath. Follow their exact scope. You are the Grok CLI fallback author for this same batch and task. Do not operate the launcher, run records, or batch quota. Do not treat the previous Cursor session as this session. Do not switch model, provider, or permission mode. Perform compilation, code review, or sheet migration only when that exact action is explicitly authorized by both the task file and prompt file. Do not redo completed side effects. Do not treat compilation or model-reported completion as user game acceptance. In the final answer include exactly one block marked <<<CURSOR_TASK_REPORT>>> then one JSON object then <<<END_CURSOR_TASK_REPORT>>>. Keys are workStatus, summary, changedFiles, checks, notRun, writesSettled, blocker, nextAction. workStatus must be reported_done or blocked or needs_input or failed. Do not invent task, attempt, session, or inputHash.
"@
    $path = Join-Path $Directory 'effective-prompt.md'
    Write-AtomicUtf8 -Path $path -Content $body
    Write-AtomicUtf8 -Path (Join-Path $Directory 'cli-prompt.txt') -Content $body
    $bytes = [System.IO.File]::ReadAllBytes($path)
    return [ordered]@{ path = $path; hash = (Get-Sha256Bytes -Bytes $bytes) }
}

function Test-SecretMaterial {
    param([string]$Text)
    if ([string]::IsNullOrEmpty($Text)) { return $false }
    return $Text -match '(?i)(bearer\s+\S|api[_-]?key|cookie\s*[:=]|base_url|authorization\s*:|\bsk-[A-Za-z0-9]{8,}\b|token\s*[:=])'
}

function ConvertTo-UtcInstant {
    param([string]$Text)
    if ([string]::IsNullOrWhiteSpace($Text)) { return $null }
    try {
        return [DateTime]::Parse($Text, $null, [System.Globalization.DateTimeStyles]::RoundtripKind).ToUniversalTime()
    }
    catch {
        return $null
    }
}

function Test-LauncherArguments {
    if ($RecoverPersistentAsk) {
        if ($Action -ne 'Resume') {
            return New-Response -Ok $false -Code 'invalid_args' -ExitCode 2 -Message '-RecoverPersistentAsk 只用于 Resume。普通 Start 不会借它新建会话。'
        }
        if (-not $WriteMode) {
            return New-Response -Ok $false -Code 'invalid_args' -ExitCode 2 -Message '持久化 Ask 恢复必须同时显式 -WriteMode。普通 Resume 仍只接当前会话。' -NextAction '需要恢复时同时给出 Resume、-WriteMode、-RecoverPersistentAsk 和 -ResumeReason。'
        }
        if ($UseQuotaFallback -or -not [string]::IsNullOrWhiteSpace($AccountQuotaEvidence)) {
            return New-Response -Ok $false -Code 'invalid_args' -ExitCode 2 -Message '持久化 Ask 恢复不能和额度切换一起用，也不会改 provider。'
        }
    }
    if (-not [string]::IsNullOrWhiteSpace($AccountQuotaEvidence)) {
        if ($Action -notin @('Start', 'Resume')) {
            return New-Response -Ok $false -Code 'invalid_args' -ExitCode 2 -Message '账户额度证据只用于 Start 或 Resume。'
        }
        if (-not $UseQuotaFallback) {
            return New-Response -Ok $false -Code 'invalid_args' -ExitCode 2 -Message '账户额度证据必须与 -UseQuotaFallback 一起显式使用。单独传入不会改渠道。'
        }
    }
    if ($UseQuotaFallback -and $Action -eq 'Start' -and [string]::IsNullOrWhiteSpace($AccountQuotaEvidence)) {
        return New-Response -Ok $false -Code 'invalid_args' -ExitCode 2 -Message 'Start 直接使用 Grok 必须带仍有效的账户额度证据。没有证据时 Start 仍走 Cursor。'
    }
    if ($UseQuotaFallback -and $Action -notin @('Start', 'Resume')) {
        return New-Response -Ok $false -Code 'invalid_args' -ExitCode 2 -Message '-UseQuotaFallback 只用于 Start 或 Resume。'
    }
    return $null
}

function Read-AccountQuotaEvidence {
    param([string]$Path)
    if (-not (Test-PathArgSafe $Path) -or -not [System.IO.Path]::IsPathRooted($Path) -or -not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return [ordered]@{ ok = $false; response = (New-Response -Ok $false -Code 'evidence_invalid' -ExitCode 2 -Message '账户额度证据必须是已存在、不含 % 和引号的绝对路径。没有启动渠道。') }
    }
    $bytes = [System.IO.File]::ReadAllBytes($Path)
    if ($bytes.Length -gt 262144) {
        return [ordered]@{ ok = $false; response = (New-Response -Ok $false -Code 'evidence_invalid' -ExitCode 2 -Message '账户额度证据超过 256KB。没有启动渠道。') }
    }
    $raw = ''
    try { $raw = $Utf8Strict.GetString($bytes) } catch {
        return [ordered]@{ ok = $false; response = (New-Response -Ok $false -Code 'evidence_invalid' -ExitCode 2 -Message '账户额度证据不是合法 UTF-8。没有启动渠道。') }
    }
    if (Test-SecretMaterial $raw) {
        return [ordered]@{ ok = $false; response = (New-Response -Ok $false -Code 'unsafe_evidence' -ExitCode 2 -Message '账户额度证据含有凭据或地址材料，已拒绝。没有保存该文件，也没有启动渠道。') }
    }
    try { $document = $raw | ConvertFrom-Json } catch {
        return [ordered]@{ ok = $false; response = (New-Response -Ok $false -Code 'evidence_invalid' -ExitCode 2 -Message '账户额度证据不是合法 JSON。没有启动渠道。') }
    }
    foreach ($name in @('schemaVersion', 'kind', 'accountScope', 'currentLoginScope', 'loginScopeNote', 'sourceKind', 'sourceLocator', 'confirmedAt', 'validUntil', 'invalidated')) {
        if (-not (Test-Property $document $name)) {
            return [ordered]@{ ok = $false; response = (New-Response -Ok $false -Code 'evidence_invalid' -ExitCode 2 -Message "账户额度证据缺少 $name。没有启动渠道。") }
        }
    }
    if ([int]$document.schemaVersion -ne 1 -or [string]$document.kind -ne 'cursor_account_quota') {
        return [ordered]@{ ok = $false; response = (New-Response -Ok $false -Code 'evidence_invalid' -ExitCode 2 -Message '账户额度证据的 schema 或 kind 不正确。没有启动渠道。') }
    }
    $scope = [string]$document.accountScope
    $currentScope = [string]$document.currentLoginScope
    if ($scope -notmatch '^[A-Za-z0-9][A-Za-z0-9._:@/-]{0,119}$' -or $scope -match '://' -or $currentScope -notmatch '^[A-Za-z0-9][A-Za-z0-9._:@/-]{0,119}$') {
        return [ordered]@{ ok = $false; response = (New-Response -Ok $false -Code 'evidence_invalid' -ExitCode 2 -Message 'accountScope 必须是不含地址和凭据的登录范围标签。启动器不能自动证明当前登录。') }
    }
    if ($currentScope -cne $scope) {
        return [ordered]@{ ok = $false; response = (New-Response -Ok $false -Code 'account_mismatch' -ExitCode 2 -Message '证据账户范围与主会话声明的当前登录范围不一致。不能跨账户沿用额度证据。启动器没有读取认证材料。') }
    }
    $note = [string]$document.loginScopeNote
    $locator = [string]$document.sourceLocator
    if (-not (Test-ControlFree $note 500) -or -not (Test-ControlFree $locator 500)) {
        return [ordered]@{ ok = $false; response = (New-Response -Ok $false -Code 'evidence_invalid' -ExitCode 2 -Message 'loginScopeNote 或 sourceLocator 无效。没有启动渠道。') }
    }
    $sourceKind = [string]$document.sourceKind
    if ($sourceKind -notin @('verified_attempt', 'user_confirmation')) {
        return [ordered]@{ ok = $false; response = (New-Response -Ok $false -Code 'evidence_invalid' -ExitCode 2 -Message 'sourceKind 只能是 verified_attempt 或 user_confirmation。') }
    }
    if ($document.invalidated -isnot [bool]) {
        return [ordered]@{ ok = $false; response = (New-Response -Ok $false -Code 'evidence_invalid' -ExitCode 2 -Message 'invalidated 必须是布尔值。') }
    }
    $invalidReason = $null
    if ((Test-Property $document 'invalidReason') -and $null -ne $document.invalidReason) {
        if ($document.invalidReason -isnot [string] -or -not (Test-ControlFree ([string]$document.invalidReason) 200)) {
            return [ordered]@{ ok = $false; response = (New-Response -Ok $false -Code 'evidence_invalid' -ExitCode 2 -Message 'invalidReason 无效。') }
        }
        $invalidReason = [string]$document.invalidReason
    }
    if ($document.invalidated) {
        $why = $(if ([string]::IsNullOrWhiteSpace($invalidReason)) { '未写明' } else { $invalidReason })
        return [ordered]@{ ok = $false; response = (New-Response -Ok $false -Code 'evidence_invalid' -ExitCode 2 -Message "账户额度证据已失效：$why。恢复、换登录或作废后不能新开备用。") }
    }
    $confirmed = ConvertTo-UtcInstant ([string]$document.confirmedAt)
    $validUntil = ConvertTo-UtcInstant ([string]$document.validUntil)
    if ($null -eq $confirmed -or $null -eq $validUntil) {
        return [ordered]@{ ok = $false; response = (New-Response -Ok $false -Code 'evidence_invalid' -ExitCode 2 -Message 'confirmedAt 或 validUntil 不是可解析的时间。') }
    }
    $now = [DateTime]::UtcNow
    if ($confirmed -gt $now.AddMinutes(5)) {
        return [ordered]@{ ok = $false; response = (New-Response -Ok $false -Code 'evidence_invalid' -ExitCode 2 -Message 'confirmedAt 晚于当前时间。没有启动渠道。') }
    }
    if ($validUntil -le $now) {
        return [ordered]@{ ok = $false; response = (New-Response -Ok $false -Code 'evidence_expired' -ExitCode 2 -Message '账户额度证据已过 validUntil。不能永远沿用。新任务应回到 Cursor，或由主会话重新确认。') }
    }
    if (($validUntil - $confirmed).TotalDays -le 0 -or ($validUntil - $confirmed).TotalDays -gt 14) {
        return [ordered]@{ ok = $false; response = (New-Response -Ok $false -Code 'evidence_window' -ExitCode 2 -Message '账户额度证据的有效期必须短于或等于 14 天，且晚于确认时间。') }
    }
    $verified = $false
    $excerpt = $null
    if ($sourceKind -eq 'verified_attempt') {
        if (-not [System.IO.Path]::IsPathRooted($locator) -or -not (Test-PathArgSafe $locator)) {
            return [ordered]@{ ok = $false; response = (New-Response -Ok $false -Code 'evidence_invalid' -ExitCode 2 -Message 'verified_attempt 的 sourceLocator 必须是安全的绝对路径。') }
        }
        $sourceFull = [System.IO.Path]::GetFullPath($locator)
        $sourceDirectory = $sourceFull
        if (Test-Path -LiteralPath $sourceFull -PathType Leaf) {
            $sourceDirectory = Split-Path -Parent $sourceFull
        }
        if (-not (Test-Path -LiteralPath $sourceDirectory -PathType Container)) {
            return [ordered]@{ ok = $false; response = (New-Response -Ok $false -Code 'evidence_not_quota' -ExitCode 2 -Message 'verified_attempt 指向的目录不存在。没有把它当成额度证据。') }
        }
        $found = Get-StoredQuotaEvidence -Directory $sourceDirectory
        if ($null -eq $found) {
            return [ordered]@{ ok = $false; response = (New-Response -Ok $false -Code 'evidence_not_quota' -ExitCode 2 -Message '来源 attempt 的错误通道没有已核实的周额度句子。TLS、网络或其他失败不能改写成额度。没有修改来源文件。') }
        }
        $verified = $true
        $excerpt = [string]$found.excerpt
        $locator = $sourceDirectory
    }
    $receipt = [ordered]@{
        schemaVersion            = 1
        kind                     = 'cursor_account_quota'
        accountScope             = $scope
        currentLoginScope        = $currentScope
        loginScopeNote           = $note
        sourceKind               = $sourceKind
        sourceLocator            = $locator
        confirmedAt              = $confirmed.ToString('o')
        validUntil               = $validUntil.ToString('o')
        invalidated              = $false
        invalidReason            = $null
        launcherProvesAccount    = $false
        accountIdentityCheckedBy = 'main_session'
        ruleVersion              = 'account-quota-v1'
        verifiedAttemptQuota     = $verified
        quotaExcerpt             = $excerpt
        note                     = '启动器只核验证据形状、期限和来源错误通道。它不能自动证明当前登录账户，主会话必须核对登录范围。'
    }
    return [ordered]@{ ok = $true; receipt = $receipt }
}

function Get-AttemptChannelText {
    param([string]$Directory, $Meta)
    $parts = New-Object System.Collections.Generic.List[string]
    if ($Meta -and (Test-Property $Meta 'blocker') -and -not [string]::IsNullOrWhiteSpace([string]$Meta.blocker)) {
        $parts.Add([string]$Meta.blocker)
    }
    foreach ($leaf in @('stderr.log', 'result.txt')) {
        $path = Join-Path $Directory $leaf
        if (Test-Path -LiteralPath $path -PathType Leaf) {
            try { $parts.Add((Read-Utf8Text -Path $path)) } catch { }
        }
    }
    return ($parts -join "`n")
}

function Test-ApprovalBlocked {
    param([string]$Directory, $Meta)
    $joined = Get-AttemptChannelText -Directory $Directory -Meta $Meta
    return $joined -match '(?i)(Auto-review|permission required|cannot ask for approval)'
}

function Test-UserCancelled {
    param([string]$Directory, $Meta)
    $joined = Get-AttemptChannelText -Directory $Directory -Meta $Meta
    return $joined -match '(?i)(canceled by user|cancelled by user|aborted by user)'
}

function Test-ReservedGrokUnlaunched {
    param($State)
    return [string]$State.grokLaunch -eq 'none' -and [string]$State.fallbackIntent -eq 'reserved' -and (Test-Uuid ([string]$State.grokSession))
}

function Test-ToolAllowlistWarning {
    param([string]$Stderr)
    foreach ($raw in ([string]$Stderr -split "`n")) {
        $line = $raw.Trim()
        if ($line -match '(?i)skipped permission entry') { continue }
        if ($line -match '(?i)unknown tool prefix') { continue }
        if ($line -match '(?i)(names no tool|leaves the other tools available)') { return $true }
        if ($line -match '(?i)--tools\b' -and $line -match '(?i)unknown tool') { return $true }
    }
    return $false
}

function Test-ReasoningIgnored {
    param([string]$Stderr)
    return [string]$Stderr -match $ReasoningIgnoredPattern
}

function Get-LatestStoredAttempt {
    param($Loaded, $StoredTask)
    $ids = @(Get-StoredAttemptIds $StoredTask)
    if ($ids.Count -eq 0) { return $null }
    $latest = [string]$ids[$ids.Count - 1]
    if ((Test-Property $StoredTask 'latestAttemptId') -and -not [string]::IsNullOrWhiteSpace([string]$StoredTask.latestAttemptId)) {
        $latest = [string]$StoredTask.latestAttemptId
    }
    $directory = Get-AttemptDirectory -Context $Loaded -Identifier $latest
    $meta = $null
    if ($directory) { $meta = Read-AttemptMeta -Directory $directory }
    return [ordered]@{ id = $latest; directory = $directory; meta = $meta }
}

function Get-AttemptVisibleText {
    param([string]$Directory, $Meta)
    $parts = New-Object System.Collections.Generic.List[string]
    if ($Meta -and (Test-Property $Meta 'blocker') -and $null -ne $Meta.blocker) {
        $parts.Add([string]$Meta.blocker)
    }
    foreach ($leaf in @('result.txt', 'stderr.log')) {
        $path = Join-Path $Directory $leaf
        if (Test-Path -LiteralPath $path -PathType Leaf) {
            try { $parts.Add((Read-Utf8Text -Path $path)) } catch { }
        }
    }
    $receiptPath = Join-Path $Directory 'receipt.json'
    if (Test-Path -LiteralPath $receiptPath -PathType Leaf) {
        try {
            $receipt = Read-JsonFile -Path $receiptPath
            foreach ($name in @('summary', 'blocker', 'nextAction')) {
                if ((Test-Property $receipt $name) -and $null -ne $receipt.$name) {
                    $parts.Add([string]$receipt.$name)
                }
            }
        }
        catch { }
    }
    return ($parts.ToArray() -join "`n")
}

function Test-EmptyChangedFiles {
    param($Receipt)
    if ($null -eq $Receipt -or -not (Test-Property $Receipt 'changedFiles')) { return $false }
    $changed = $Receipt.changedFiles
    if ($null -eq $changed) { return $true }
    if ($changed -is [string]) { return $false }
    return @($changed).Count -eq 0
}

function Test-RecoveryCursorOnly {
    param($ProviderState)
    if ([string]$ProviderState.activeProvider -ne 'cursor') { return $false }
    if ($ProviderState.fallbackUsed) { return $false }
    if ([string]$ProviderState.grokLaunch -eq 'starting' -or [string]$ProviderState.grokLaunch -eq 'created') { return $false }
    if (Test-ReservedGrokUnlaunched $ProviderState) { return $false }
    if (-not [string]::IsNullOrWhiteSpace([string]$ProviderState.grokSession)) { return $false }
    return $true
}

function Test-RecoveryScope {
    param($AttemptMeta, $Context)
    if ([string]$AttemptMeta.stage -ne [string]$Context.task.stage) { return $false }
    $oldPaths = $null
    if ((Test-Property $AttemptMeta 'writePaths') -and $null -ne $AttemptMeta.writePaths) { $oldPaths = $AttemptMeta.writePaths }
    $oldDependencies = $null
    if ((Test-Property $AttemptMeta 'dependencies') -and $null -ne $AttemptMeta.dependencies) { $oldDependencies = $AttemptMeta.dependencies }
    if ((Get-JoinedScope $oldPaths) -ne (Get-JoinedScope $Context.writePaths)) { return $false }
    if ((Get-JoinedScope $oldDependencies) -ne (Get-JoinedScope $Context.dependencies)) { return $false }
    return $true
}

function Invoke-RecoveryCreateChat {
    param($Loaded)
    $agent = Resolve-CursorAgentPath
    if (-not ($agent -is [System.Collections.IDictionary] -and $agent.Contains('path'))) {
        return [ordered]@{ ok = $false; response = $agent }
    }
    $stamp = [DateTime]::UtcNow.ToString('yyyyMMddHHmmssfff')
    $logDirectory = Join-Path (Join-Path $Loaded.taskRoot 'session-recovery-create') $stamp
    [void][System.IO.Directory]::CreateDirectory($logDirectory)
    $createOut = Join-Path $logDirectory 'stdout.log'
    $createErr = Join-Path $logDirectory 'stderr.log'
    $plan = Get-LaunchPlan -AgentPath $agent.path -AgentArguments @('create-chat')
    try {
        $created = Invoke-CapturedProcess -FilePath $plan.fileName -WorkingDirectory $Loaded.workspace -Arguments $plan.arguments -StdoutPath $createOut -StderrPath $createErr
    }
    catch {
        return [ordered]@{
            ok       = $false
            response = (New-Response -Ok $false -Code 'create_chat_failed' -ExitCode 4 -Message ('create-chat 没有启动。当前会话没有切换: ' + $_.Exception.Message) -NextAction '当前 Cursor 会话没有更换。可以修正 CLI 后再次显式恢复，不会自动重试。')
        }
    }
    $createText = ''
    try { $createText = (Read-Utf8Text -Path $createOut).Trim() } catch { $createText = '' }
    $lines = @($createText -split "`r?`n" | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    if ($created.exitCode -ne 0 -or $lines.Count -ne 1 -or -not (Test-Uuid ([string]$lines[0]))) {
        $detail = $createText
        if ([string]::IsNullOrWhiteSpace($detail)) {
            try { $detail = (Read-Utf8Text -Path $createErr).Trim() } catch { $detail = '' }
        }
        $detail = Get-RedactedText $detail
        $failure = [ordered]@{
            at              = [DateTime]::UtcNow.ToString('o')
            kind            = 'persistent-ask-recovery-create-failed'
            exitCode        = $created.exitCode
            output          = $detail
            sessionSwitched = $false
        }
        Write-AtomicJson -Path (Join-Path $Loaded.taskRoot ('session-recovery-failure-' + $stamp + '.json')) -Object $failure
        return [ordered]@{
            ok       = $false
            response = (New-Response -Ok $false -Code 'create_chat_failed' -ExitCode 4 -Message ('create-chat 没有返回唯一 UUID。持久化 Ask 恢复没有切换当前会话。' + $detail) -NextAction '查看 session-recovery-failure 记录。当前 Cursor 会话没有更换，也没有自动再开会话。')
        }
    }
    return [ordered]@{ ok = $true; sessionId = [string]$lines[0] }
}

function Resolve-PersistentAskRecovery {
    param($Loaded, $StoredTask, $ProviderState)
    if (-not (Test-RecoveryCursorOnly $ProviderState)) {
        return [ordered]@{
            ok       = $false
            response = (New-Response -Ok $false -Code 'cursor_only' -ExitCode 2 -Provider ([string]$ProviderState.activeProvider) -SessionId ([string]$ProviderState.cursorSession) -Message '持久化 Ask 恢复只用于当前 Cursor 会话。它不改 provider，也没有切换会话。')
        }
    }
    $latest = Get-LatestStoredAttempt -Loaded $Loaded -StoredTask $StoredTask
    if ($null -eq $latest -or $null -eq $latest.meta -or [string]::IsNullOrWhiteSpace([string]$latest.directory)) {
        return [ordered]@{
            ok       = $false
            response = (New-Response -Ok $false -Code 'persistent_ask_required' -ExitCode 2 -SessionId ([string]$ProviderState.cursorSession) -Message '没有可核对的最新 attempt。普通 Resume 没有新建会话。')
        }
    }
    $meta = $latest.meta
    $directory = [string]$latest.directory
    $life = [string]$meta.lifecycle
    if ($life -eq 'protocol_error') {
        return [ordered]@{
            ok       = $false
            response = (New-Response -Ok $false -Code 'protocol_unresolved' -ExitCode 2 -Provider 'cursor' -SessionId ([string]$ProviderState.cursorSession) -Attempt ([string]$latest.id) -Message '最新 attempt 是协议异常。持久化 Ask 恢复不能绕开它，也没有切换会话。')
        }
    }
    if (Test-ApprovalBlocked -Directory $directory -Meta $meta) {
        return [ordered]@{
            ok       = $false
            response = (New-Response -Ok $false -Code 'approval_not_bypassable' -ExitCode 2 -Provider 'cursor' -SessionId ([string]$ProviderState.cursorSession) -Attempt ([string]$latest.id) -Message '审批拒绝不能借持久化 Ask 恢复绕过。当前会话没有更换。')
        }
    }
    if (Test-UserCancelled -Directory $directory -Meta $meta) {
        return [ordered]@{
            ok       = $false
            response = (New-Response -Ok $false -Code 'cancelled' -ExitCode 2 -Provider 'cursor' -SessionId ([string]$ProviderState.cursorSession) -Attempt ([string]$latest.id) -Message '用户取消保持原失败。持久化 Ask 恢复不能把它换成新会话。')
        }
    }
    $visible = Get-AttemptVisibleText -Directory $directory -Meta $meta
    $failureKind = ''
    if ((Test-Property $meta 'failureKind') -and $null -ne $meta.failureKind) { $failureKind = [string]$meta.failureKind }
    $eligibility = ''
    if ((Test-Property $meta 'fallbackEligibility') -and $null -ne $meta.fallbackEligibility) { $eligibility = [string]$meta.fallbackEligibility }
    if ($failureKind -eq 'cursor_quota_exhausted' -or $eligibility -eq 'eligible' -or $visible -match $QuotaErrorPattern) {
        return [ordered]@{
            ok       = $false
            response = (New-Response -Ok $false -Code 'quota_not_recoverable' -ExitCode 2 -Provider 'cursor' -SessionId ([string]$ProviderState.cursorSession) -Attempt ([string]$latest.id) -FallbackEligibility $eligibility -Message '额度失败不能借持久化 Ask 恢复换会话或换 provider。')
        }
    }
    $attemptProvider = 'cursor'
    if ((Test-Property $meta 'provider') -and -not [string]::IsNullOrWhiteSpace([string]$meta.provider)) { $attemptProvider = [string]$meta.provider }
    $receiptPath = Join-Path $directory 'receipt.json'
    $receipt = $null
    if (Test-Path -LiteralPath $receiptPath -PathType Leaf) {
        try { $receipt = Read-JsonFile -Path $receiptPath } catch { $receipt = $null }
    }
    $readOnlyWrite = $false
    if ((Test-Property $meta 'readOnly') -and $meta.readOnly -is [bool]) { $readOnlyWrite = -not [bool]$meta.readOnly }
    $settledWrites = ($receipt.writesSettled -is [bool]) -and ($receipt.writesSettled -eq $true)
    $recorded = $attemptProvider -eq 'cursor' -and $life -eq 'reported' -and [string]$meta.reportStatus -eq 'valid' -and [string]$meta.workStatus -eq 'blocked' -and [string]$meta.mode -eq 'write' -and $readOnlyWrite -and $null -ne $receipt -and [string]$receipt.reportStatus -eq 'valid' -and [string]$receipt.workStatus -eq 'blocked' -and $settledWrites -and (Test-EmptyChangedFiles $receipt) -and $visible.Contains($PersistentAskMarker)
    if (-not $recorded) {
        return [ordered]@{
            ok       = $false
            response = (New-Response -Ok $false -Code 'persistent_ask_required' -ExitCode 2 -Provider 'cursor' -SessionId ([string]$ProviderState.cursorSession) -Attempt ([string]$latest.id) -Message '最新 attempt 不是已记录的持久化 Ask 阻塞。普通 Resume 没有新建会话。')
        }
    }
    $settlement = Get-ProcessSettlement $meta
    if ($settlement -ne 'settled') {
        return [ordered]@{
            ok       = $false
            response = (New-Response -Ok $false -Code 'not_settled' -ExitCode 3 -Provider 'cursor' -SessionId ([string]$ProviderState.cursorSession) -Attempt ([string]$latest.id) -Message '原进程、CLI 或记录的写入子进程尚未收束，或缺少可核对的进程戳。已拒绝恢复。')
        }
    }
    if (-not (Test-RecoveryScope -AttemptMeta $meta -Context $Loaded)) {
        return [ordered]@{
            ok       = $false
            response = (New-Response -Ok $false -Code 'scope_mismatch' -ExitCode 2 -Provider 'cursor' -SessionId ([string]$ProviderState.cursorSession) -Attempt ([string]$latest.id) -Message 'stage、writePaths 或 dependencies 与最新 attempt 不一致。恢复不改这些，也没有切换会话。')
        }
    }
    $created = Invoke-RecoveryCreateChat -Loaded $Loaded
    if (-not $created.ok) { return [ordered]@{ ok = $false; response = $created.response } }
    $oldSession = [string]$ProviderState.cursorSession
    $newSession = [string]$created.sessionId
    $record = [ordered]@{
        kind                     = 'persistent-ask'
        at                       = [DateTime]::UtcNow.ToString('o')
        previousCursorSessionId  = $oldSession
        cursorSessionId          = $newSession
        parentAttemptId          = [string]$latest.id
        reason                   = $ResumeReason
        inputVersion             = [string]$Loaded.task.inputVersion
        previousInputVersion     = [string]$meta.inputVersion
    }
    $list = New-Object System.Collections.Generic.List[object]
    foreach ($entry in @($ProviderState.cursorSessionRecoveries)) {
        if ($null -ne $entry) { $list.Add($entry) }
    }
    $list.Add($record)
    $ProviderState.cursorSession = $newSession
    $ProviderState.activeProvider = 'cursor'
    $ProviderState.cursorSessionRecoveries = $list.ToArray()
    return [ordered]@{
        ok            = $true
        providerState = $ProviderState
        recovery      = [ordered]@{
            oldSession            = $oldSession
            newSession            = $newSession
            parentAttemptId       = [string]$latest.id
            parentDirectory       = $directory
            previousInputVersion  = [string]$meta.inputVersion
            receiptPath           = $receiptPath
            reason                = $ResumeReason
        }
    }
}

function Restore-UnstartedRecovery {
    param([string]$TaskJsonPath, [string]$AttemptDirectory, [string]$Detail)
    if ($null -eq $script:PendingRecovery -or $null -eq $script:TaskJsonSnapshot -or [string]::IsNullOrWhiteSpace($TaskJsonPath)) { return }
    [System.IO.File]::WriteAllBytes($TaskJsonPath, $script:TaskJsonSnapshot)
    $note = [ordered]@{
        at                       = [DateTime]::UtcNow.ToString('o')
        kind                     = 'persistent-ask-recovery-not-started'
        previousCursorSessionId  = [string]$script:PendingRecovery.oldSession
        unusedCursorSessionId    = [string]$script:PendingRecovery.newSession
        sessionSwitched          = $false
        attemptDirectory         = $AttemptDirectory
        detail                   = (Get-RedactedText ([string]$Detail))
    }
    $root = Split-Path -Parent $TaskJsonPath
    Write-AtomicJson -Path (Join-Path $root ('session-recovery-failure-' + [DateTime]::UtcNow.ToString('yyyyMMddHHmmssfff') + '.json')) -Object $note
    $script:PendingRecovery = $null
    $script:TaskJsonSnapshot = $null
}

function Invoke-StartOrResume {
    $loaded = Get-TaskContext -RequirePrompt
    if (-not ($loaded -is [System.Collections.IDictionary] -and $loaded.Contains('context'))) { return $loaded }

    $taskJsonPath = Join-Path $loaded.taskRoot 'task.json'
    $taskLockPath = Join-Path $loaded.taskRoot 'task.lock'
    $script:TaskLock = Open-ExclusiveFile -Path $taskLockPath
    if ($null -eq $script:TaskLock) {
        return New-Response -Ok $false -Code 'lock_conflict' -ExitCode 3 -Message '同一任务正在执行。已拒绝并发，没有终止对方进程，也没有删除锁。' -NextAction '等当前进程结束，或在人工对账后使用 Resume。'
    }
    Write-LockText -Stream $script:TaskLock -Text ("pid=$PID; started=$(Get-ProcessStartIso -ProcessId $PID); action=$Action`n")

    $taskExists = Test-Path -LiteralPath $taskJsonPath -PathType Leaf
    $attemptsRoot = Join-Path $loaded.taskRoot 'attempts'
    $orphan = Get-OrphanState $loaded
    if ($null -ne $orphan) {
        return New-RecoveryResponse -Orphan $orphan
    }
    if ($Action -eq 'Start' -and $taskExists) {
        return New-Response -Ok $false -Code 'task_exists' -ExitCode 2 -Message '该任务已有记录。Start 不会覆盖旧终态。' -NextAction '核对身份、工作区和会话后，使用 -Action Resume -ResumeReason 显式接续。'
    }

    $storedTask = $null
    $sessionId = $null
    if ($Action -eq 'Resume') {
        if (-not $taskExists) {
            return New-Response -Ok $false -Code 'task_missing' -ExitCode 2 -Message '没有可 Resume 的任务。' -NextAction '首次执行使用 Start。Start 才会 create-chat。'
        }
        $storedTask = Read-JsonFile -Path $taskJsonPath
        foreach ($name in @('batchId', 'taskId', 'workspace', 'sessionId')) {
            if (-not (Test-Property $storedTask $name)) {
                return New-Response -Ok $false -Code 'identity_mismatch' -ExitCode 2 -Message "已存任务缺少 $name。"
            }
        }
        $sameIdentity = ([string]$storedTask.batchId -eq [string]$loaded.task.batchId) -and ([string]$storedTask.taskId -eq [string]$loaded.task.taskId) -and ((Get-FullDirectory ([string]$storedTask.workspace)) -eq $loaded.workspace)
        if (-not $sameIdentity) {
            return New-Response -Ok $false -Code 'identity_mismatch' -ExitCode 2 -Message 'Resume 的 batchId、taskId 或 workspace 与已存任务不一致。' -NextAction '不要换会话重开。核对 TaskFile 后再接续。额度不会因此重置。'
        }
        $sessionId = ''
        if ((Test-Property $storedTask 'sessionId') -and -not [string]::IsNullOrWhiteSpace([string]$storedTask.sessionId)) {
            $sessionId = [string]$storedTask.sessionId
            if (-not (Test-Uuid $sessionId)) {
                return New-Response -Ok $false -Code 'session_missing' -ExitCode 2 -Message '已存 Cursor 会话不是有效 UUID。Resume 不能新建会话。'
            }
        }
        else {
            $existingProvider = Get-ProviderState $storedTask
            if ($existingProvider.activeProvider -ne 'grok' -or -not (Test-Uuid ([string]$existingProvider.grokSession))) {
                return New-Response -Ok $false -Code 'session_missing' -ExitCode 2 -Message '没有可用的 Cursor 会话，也没有已创建的 Grok 会话。Resume 不伪造 Cursor UUID。'
            }
            $sessionId = [string]$existingProvider.grokSession
        }
        foreach ($previous in (Get-StoredAttemptIds $storedTask)) {
            $previousDirectory = Get-AttemptDirectory -Context $loaded -Identifier $previous
            if ($null -eq $previousDirectory) { continue }
            $observed = Update-StaleAttempt -Directory $previousDirectory
            if ($observed -eq 'running') {
                return New-Response -Ok $false -Code 'still_running' -ExitCode 3 -Message '原进程仍在运行。拒绝重复启动，没有终止该进程。' -SessionId $sessionId -NextAction '等它结束，或确认进程消失后再 Resume。'
            }
        }
    }

    $accountReceipt = $null
    $directGrokStart = $false
    if ($UseQuotaFallback -and -not [string]::IsNullOrWhiteSpace($AccountQuotaEvidence)) {
        $loadedEvidence = Read-AccountQuotaEvidence -Path $AccountQuotaEvidence
        if (-not $loadedEvidence.ok) { return $loadedEvidence.response }
        $accountReceipt = $loadedEvidence.receipt
        if ($Action -eq 'Start') { $directGrokStart = $true }
    }

    if ($Action -eq 'Start' -and -not $directGrokStart) {
        $agent = Resolve-CursorAgentPath
        if (-not ($agent -is [System.Collections.IDictionary] -and $agent.Contains('path'))) { return $agent }
        $createDirectory = Join-Path $loaded.taskRoot 'create-chat'
        if (-not (Test-Path -LiteralPath $createDirectory)) {
            [void][System.IO.Directory]::CreateDirectory($createDirectory)
        }
        $createOut = Join-Path $createDirectory 'stdout.log'
        $createErr = Join-Path $createDirectory 'stderr.log'
        $plan = Get-LaunchPlan -AgentPath $agent.path -AgentArguments @('create-chat')
        try {
            $created = Invoke-CapturedProcess -FilePath $plan.fileName -WorkingDirectory $loaded.workspace -Arguments $plan.arguments -StdoutPath $createOut -StderrPath $createErr
        }
        catch {
            return New-Response -Ok $false -Code 'create_chat_failed' -ExitCode 4 -Message ('create-chat 没有启动: ' + $_.Exception.Message) -NextAction '任务尚未建立，可以修正 CLI 后重新 Start。'
        }
        $createText = ''
        try { $createText = (Read-Utf8Text -Path $createOut).Trim() } catch { $createText = '' }
        $lines = @($createText -split "`r?`n" | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
        $uuidPattern = '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
        if ($created.exitCode -ne 0 -or $lines.Count -ne 1 -or $lines[0] -notmatch $uuidPattern) {
            $detail = $createText
            if ([string]::IsNullOrWhiteSpace($detail)) {
                try { $detail = (Read-Utf8Text -Path $createErr).Trim() } catch { $detail = '' }
            }
            $failure = [ordered]@{
                at       = [DateTime]::UtcNow.ToString('o')
                exitCode = $created.exitCode
                output   = $detail
            }
            Write-AtomicJson -Path (Join-Path $loaded.taskRoot ('start-failure-' + [DateTime]::UtcNow.ToString('yyyyMMddHHmmssfff') + '.json')) -Object $failure
            return New-Response -Ok $false -Code 'create_chat_failed' -ExitCode 4 -Message ('create-chat 没有返回唯一 UUID，任务未建立。' + $detail) -NextAction '查看 start-failure 记录。不要猜会话，修正后重新 Start。'
        }
        $sessionId = $lines[0]
        Write-AtomicUtf8 -Path (Join-Path $loaded.taskRoot 'session-create.txt') -Content $sessionId
    }

    $providerState = Get-ProviderState $storedTask
    if ($directGrokStart) {
        $providerState.cursorSession = $null
        $providerState.activeProvider = 'grok'
        $providerState.grokLaunch = 'none'
        $providerState.fallbackUsed = $false
        $providerState.fallbackIntent = 'reserved'
    }
    elseif ($Action -eq 'Start') {
        $providerState.cursorSession = $sessionId
        $providerState.activeProvider = 'cursor'
        $providerState.grokLaunch = 'none'
        $providerState.fallbackUsed = $false
    }
    elseif ([string]::IsNullOrWhiteSpace([string]$providerState.cursorSession) -and $providerState.activeProvider -eq 'cursor') {
        $providerState.cursorSession = $sessionId
    }
    $script:PendingRecovery = $null
    $script:TaskJsonSnapshot = $null
    if ($RecoverPersistentAsk) {
        $decided = Resolve-PersistentAskRecovery -Loaded $loaded -StoredTask $storedTask -ProviderState $providerState
        if (-not $decided.ok) { return $decided.response }
        $providerState = $decided.providerState
        $script:PendingRecovery = $decided.recovery
        $script:TaskJsonSnapshot = [System.IO.File]::ReadAllBytes($taskJsonPath)
    }
    $runProvider = 'cursor'
    $runSession = [string]$providerState.cursorSession
    $grokFirstStart = $false
    $parentAttemptId = $null
    $parentMeta = $null
    $quotaEvidence = $null
    $settlement = $null
    if ($directGrokStart) {
        $runSession = [guid]::NewGuid().ToString()
        $providerState.grokSession = $runSession
        $providerState.fallbackFromAttemptId = $null
        $runProvider = 'grok'
        $grokFirstStart = $true
        $settlement = 'no_prior_attempt'
        $quotaEvidence = $accountReceipt
    }
    elseif ($UseQuotaFallback -and $null -ne $accountReceipt) {
        if ($providerState.grokLaunch -eq 'starting') {
            return New-Response -Ok $false -Code 'session_uncertain' -ExitCode 2 -Provider 'grok' -SessionId $providerState.grokSession -FallbackUsed $providerState.fallbackUsed -Message 'Grok 会话启动是否成功不能确定。已停止，没有生成第二个 UUID。'
        }
        if (Test-ReservedGrokUnlaunched $providerState) {
            $runSession = [string]$providerState.grokSession
            $runProvider = 'grok'
            $grokFirstStart = $true
            $quotaEvidence = $accountReceipt
            $settlement = 'reserved_unlaunched'
        }
        elseif ($providerState.fallbackUsed -or $providerState.grokLaunch -eq 'created' -or ($providerState.activeProvider -eq 'grok' -and $providerState.grokLaunch -ne 'none')) {
            return New-Response -Ok $false -Code 'fallback_already_used' -ExitCode 2 -Provider 'grok' -SessionId $providerState.grokSession -FallbackUsed $true -FallbackEligibility 'used' -Message '该 task 已经在使用 Grok。账户证据不能再次切换，也不能把它切回 Cursor。' -NextAction '后续用普通 Resume 接续已保存的 Grok 会话。'
        }
        else {
        $attemptIdsForAccount = @(Get-StoredAttemptIds $storedTask)
        if ($attemptIdsForAccount.Count -eq 0) {
            return New-Response -Ok $false -Code 'fallback_not_eligible' -ExitCode 2 -Provider 'cursor' -Message '没有可交接的 attempt。没有伪造 Cursor 运行。'
        }
        $parentAttemptId = [string]$attemptIdsForAccount[$attemptIdsForAccount.Count - 1]
        if (-not [string]::IsNullOrWhiteSpace([string]$storedTask.latestAttemptId)) { $parentAttemptId = [string]$storedTask.latestAttemptId }
        $parentDirectory = Get-AttemptDirectory -Context $loaded -Identifier $parentAttemptId
        $parentMeta = $(if ($parentDirectory) { Read-AttemptMeta -Directory $parentDirectory } else { $null })
        if ($null -eq $parentMeta) {
            return New-Response -Ok $false -Code 'fallback_not_eligible' -ExitCode 2 -Message '找不到最新 attempt。没有修改历史文件。'
        }
        $parentLife = [string]$parentMeta.lifecycle
        if ($parentLife -eq 'protocol_error') {
            return New-Response -Ok $false -Code 'protocol_unresolved' -ExitCode 2 -Provider 'cursor' -Attempt $parentAttemptId -Message '最新 attempt 是协议异常。账户证据不能掩盖或绕开它。'
        }
        if ($parentLife -ne 'failed') {
            return New-Response -Ok $false -Code 'not_reconciled' -ExitCode 2 -Provider 'cursor' -Attempt $parentAttemptId -Message "最新 attempt 的 lifecycle 是 $parentLife。取消、缺终局、中断或仍在运行时不能用账户证据换渠道。"
        }
        if (Test-UserCancelled -Directory $parentDirectory -Meta $parentMeta) {
            return New-Response -Ok $false -Code 'cancelled' -ExitCode 2 -Provider 'cursor' -Attempt $parentAttemptId -Message '用户取消保持原失败。账户证据不能把它换成 Grok。'
        }
        if (Test-ApprovalBlocked -Directory $parentDirectory -Meta $parentMeta) {
            return New-Response -Ok $false -Code 'approval_not_bypassable' -ExitCode 2 -Provider 'cursor' -Attempt $parentAttemptId -Message '审批拒绝不能借账户额度证据换渠道绕过。原 attempt 保持原失败。'
        }
        if (-not (Test-SameTaskScope -AttemptMeta $parentMeta -Context $loaded)) {
            return New-Response -Ok $false -Code 'input_changed' -ExitCode 2 -Provider 'cursor' -Message 'TaskFile 的输入版本、授权、阶段或写路径与最新 attempt 不一致。已拒绝切换。'
        }
        $settlement = Get-ProcessSettlement $parentMeta
        if ($settlement -ne 'settled') {
            return New-Response -Ok $false -Code 'not_settled' -ExitCode 3 -Provider 'cursor' -Attempt $parentAttemptId -Message '原进程、CLI 或记录的写入子进程尚未收束。已拒绝接替。'
        }
        $runSession = [guid]::NewGuid().ToString()
        $providerState.grokSession = $runSession
        $providerState.fallbackIntent = 'reserved'
        $providerState.fallbackFromAttemptId = $parentAttemptId
        $providerState.grokLaunch = 'none'
        $providerState.fallbackUsed = $false
        $providerState.activeProvider = 'cursor'
        $runProvider = 'grok'
        $grokFirstStart = $true
        $quotaEvidence = $accountReceipt
        }
    }
    elseif ($UseQuotaFallback) {
        if ($providerState.grokLaunch -eq 'starting') {
            return New-Response -Ok $false -Code 'session_uncertain' -ExitCode 2 -Provider 'grok' -SessionId $providerState.grokSession -FallbackUsed $providerState.fallbackUsed -FallbackEligibility 'used' `
                -Message 'Grok 会话启动是否成功不能确定。已停止，没有生成第二个 UUID。' -NextAction '先对账该预留 UUID。已创建的会话只能显式 resume，不能另开会话。'
        }
        if ($providerState.fallbackUsed -or $providerState.activeProvider -eq 'grok' -or $providerState.grokLaunch -eq 'created') {
            return New-Response -Ok $false -Code 'fallback_already_used' -ExitCode 2 -Provider 'grok' -SessionId $providerState.grokSession -FallbackUsed $true -FallbackEligibility 'used' `
                -Message '该 task 已经用过一次 Cursor→Grok 切换。不会回切，也不会再切换。' -NextAction '后续用普通 Resume 接续已保存的 Grok 会话。'
        }
        $attemptIdsForFallback = @(Get-StoredAttemptIds $storedTask)
        if ($attemptIdsForFallback.Count -eq 0) {
            return New-Response -Ok $false -Code 'fallback_not_eligible' -ExitCode 2 -Provider 'cursor' -FallbackUsed $false -FallbackEligibility 'ineligible' -Message '没有可切换的 Cursor attempt。'
        }
        $parentAttemptId = [string]$attemptIdsForFallback[$attemptIdsForFallback.Count - 1]
        if (-not [string]::IsNullOrWhiteSpace([string]$storedTask.latestAttemptId)) { $parentAttemptId = [string]$storedTask.latestAttemptId }
        if (-not [string]::IsNullOrWhiteSpace([string]$providerState.fallbackFromAttemptId)) { $parentAttemptId = [string]$providerState.fallbackFromAttemptId }
        $parentDirectory = Get-AttemptDirectory -Context $loaded -Identifier $parentAttemptId
        $parentMeta = $(if ($parentDirectory) { Read-AttemptMeta -Directory $parentDirectory } else { $null })
        if ($null -eq $parentMeta) {
            return New-Response -Ok $false -Code 'fallback_not_eligible' -ExitCode 2 -Provider 'cursor' -FallbackEligibility 'ineligible' -Message '找不到作为切换依据的 Cursor attempt。'
        }
        $parentProvider = 'cursor'
        if ((Test-Property $parentMeta 'provider') -and -not [string]::IsNullOrWhiteSpace([string]$parentMeta.provider)) { $parentProvider = [string]$parentMeta.provider }
        $storedEligibility = 'ineligible'
        if (Test-Property $parentMeta 'fallbackEligibility') { $storedEligibility = [string]$parentMeta.fallbackEligibility }
        $quotaEvidence = Get-StoredQuotaEvidence -Directory $parentDirectory
        if (Test-UserCancelled -Directory $parentDirectory -Meta $parentMeta) {
            return New-Response -Ok $false -Code 'cancelled' -ExitCode 2 -Provider 'cursor' -Attempt $parentAttemptId -FallbackEligibility $storedEligibility `
                -Message '用户取消保持原失败。同任务周额度路径也不能把它换成 Grok。'
        }
        if (Test-ApprovalBlocked -Directory $parentDirectory -Meta $parentMeta) {
            return New-Response -Ok $false -Code 'approval_not_bypassable' -ExitCode 2 -Provider 'cursor' -Attempt $parentAttemptId -FallbackEligibility $storedEligibility `
                -Message '审批拒绝不能借同任务周额度证据换渠道绕过。原 attempt 保持原失败。'
        }
        if ($parentProvider -ne 'cursor' -or $storedEligibility -ne 'eligible' -or $null -eq $quotaEvidence -or [string]$parentMeta.lifecycle -ne 'failed') {
            return New-Response -Ok $false -Code 'fallback_not_eligible' -ExitCode 2 -Provider 'cursor' -SessionId $providerState.cursorSession -FallbackUsed $false -FallbackEligibility $storedEligibility `
                -Message '最新 Cursor attempt 不是已复核的额度耗尽失败。普通错误、自报额度和协议异常都不能切换。' -NextAction '保留现场。不要为了切换重复消耗 Cursor 额度。'
        }
        if (-not (Test-SameTaskScope -AttemptMeta $parentMeta -Context $loaded)) {
            return New-Response -Ok $false -Code 'input_changed' -ExitCode 2 -Provider 'cursor' -SessionId $providerState.cursorSession -FallbackEligibility 'eligible' `
                -Message 'TaskFile 的输入版本、授权、阶段或写路径与额度失败 attempt 不一致。已拒绝切换。'
        }
        $settlement = Get-ProcessSettlement $parentMeta
        if ($settlement -ne 'settled') {
            $code = $(if ($settlement -eq 'alive') { 'not_settled' } else { 'not_settled' })
            return New-Response -Ok $false -Code $code -ExitCode 3 -Provider 'cursor' -SessionId $providerState.cursorSession -Attempt $parentAttemptId -FallbackEligibility 'eligible' `
                -Message '原进程、CLI 或记录的写入子进程尚未收束，或缺少可核对的进程戳。已拒绝接替。' -NextAction '对账结束后再 Resume -UseQuotaFallback。不能把不确定运行标成完成。'
        }
        $reuse = -not [string]::IsNullOrWhiteSpace([string]$providerState.grokSession) -and [string]$providerState.fallbackIntent -eq 'reserved' -and [string]$providerState.grokLaunch -eq 'none'
        if ($reuse) {
            $runSession = [string]$providerState.grokSession
        }
        else {
            $runSession = [guid]::NewGuid().ToString()
            $providerState.grokSession = $runSession
        }
        if (-not (Test-Uuid $runSession)) {
            return New-Response -Ok $false -Code 'session_missing' -ExitCode 2 -Message '预留的 Grok 会话不是有效 UUID。没有另开会话。'
        }
        $providerState.fallbackIntent = 'reserved'
        $providerState.fallbackFromAttemptId = $parentAttemptId
        $providerState.grokLaunch = 'none'
        $providerState.fallbackUsed = $false
        $providerState.activeProvider = 'cursor'
        $runProvider = 'grok'
        $grokFirstStart = $true
    }
    elseif ($Action -eq 'Resume' -and ($providerState.activeProvider -eq 'grok' -or (Test-ReservedGrokUnlaunched $providerState))) {
        if (Test-ReservedGrokUnlaunched $providerState) {
            $runProvider = 'grok'
            $runSession = [string]$providerState.grokSession
            $grokFirstStart = $true
        }
        elseif ($providerState.grokLaunch -ne 'created' -or -not (Test-Uuid ([string]$providerState.grokSession))) {
            return New-Response -Ok $false -Code 'session_uncertain' -ExitCode 2 -Provider 'grok' -SessionId $providerState.grokSession -FallbackUsed $providerState.fallbackUsed `
                -Message 'Grok 会话还不能确定已经创建。普通 Resume 不会改用 --session-id，也不会新建 UUID。' -NextAction '先对账预留 UUID。'
        }
        else {
            $runProvider = 'grok'
            $runSession = [string]$providerState.grokSession
            $grokFirstStart = $false
        }
    }
    else {
        $runProvider = 'cursor'
        $runSession = [string]$providerState.cursorSession
    }

    $resolvedGrok = $null
    if ($runProvider -eq 'grok') {
        $resolvedGrok = Resolve-GrokAgentPath
        if (-not ($resolvedGrok -is [System.Collections.IDictionary] -and $resolvedGrok.Contains('path'))) { return $resolvedGrok }
    }

    $sessionLockPath = Get-SessionLockPath -RunRoot $loaded.runRoot -Provider $runProvider -SessionId $runSession
    $script:SessionLock = Open-ExclusiveFile -Path $sessionLockPath
    if ($null -eq $script:SessionLock) {
        return New-Response -Ok $false -Code 'lock_conflict' -ExitCode 3 -Message '同一 provider 会话正在执行。已拒绝并发，没有终止对方进程，也没有删除锁。' -SessionId $runSession -Provider $runProvider -NextAction '等该会话结束后再 Resume。'
    }
    Write-LockText -Stream $script:SessionLock -Text ("pid=$PID; provider=$runProvider; session=$runSession; action=$Action`n")

    if (-not (Test-Path -LiteralPath $attemptsRoot)) {
        [void][System.IO.Directory]::CreateDirectory($attemptsRoot)
    }
    $identifier = New-AttemptIdentifier -AttemptsDirectory $attemptsRoot
    $attemptDirectory = Get-AttemptDirectory -Context $loaded -Identifier $identifier
    [void][System.IO.Directory]::CreateDirectory($attemptDirectory)
    [System.IO.File]::WriteAllBytes((Join-Path $attemptDirectory 'task.json'), $loaded.taskBytes)
    [System.IO.File]::WriteAllBytes((Join-Path $attemptDirectory 'prompt.md'), $loaded.promptBytes)
    $taskHash = Get-Sha256Bytes -Bytes $loaded.taskBytes
    $promptHash = Get-Sha256Bytes -Bytes $loaded.promptBytes
    $combined = New-Object byte[] ($loaded.taskBytes.Length + 1 + $loaded.promptBytes.Length)
    [System.Array]::Copy($loaded.taskBytes, 0, $combined, 0, $loaded.taskBytes.Length)
    $combined[$loaded.taskBytes.Length] = 0
    [System.Array]::Copy($loaded.promptBytes, 0, $combined, $loaded.taskBytes.Length + 1, $loaded.promptBytes.Length)
    $inputHash = Get-Sha256Bytes -Bytes $combined
    $effectiveHash = $null
    $handoffHash = $null
    if ($runProvider -eq 'grok' -and $grokFirstStart) {
        $handoffKind = 'cursor-to-grok'
        $handoffFailure = 'cursor_quota_exhausted'
        $handoffNote = 'Quota failure stays failed. This handoff does not invent writesSettled or a successful Cursor terminal.'
        if ($null -ne $accountReceipt) {
            $handoffKind = $(if ($directGrokStart) { 'direct-grok-start' } else { 'account-quota-to-grok' })
            $handoffFailure = 'account_quota_reused'
            $handoffNote = 'Account evidence selects the provider only. The launcher does not prove the login identity, does not rewrite the previous attempt, and does not grant write or review permission.'
        }
        $handoff = New-GrokHandoff -Context $loaded -Directory $attemptDirectory -ParentMeta $parentMeta -ParentAttemptId $parentAttemptId -Evidence $quotaEvidence -Settlement $settlement -HandoffKind $handoffKind -FailureKind $handoffFailure -Note $handoffNote
        $effective = New-GrokPrompt -TaskPath $loaded.taskFull -PromptPath $loaded.promptFull -HandoffPath $handoff.path -Directory $attemptDirectory
        $effectiveHash = [string]$effective.hash
        $handoffHash = [string]$handoff.hash
        $meta = New-AttemptMeta -Context $loaded -Identifier $identifier -SessionId $runSession -InputHash $inputHash -TaskHash $taskHash -PromptHash $promptHash -Provider 'grok' -ParentAttemptId $parentAttemptId -SwitchReason $ResumeReason -EffectivePromptHash $effectiveHash -HandoffHash $handoffHash -CursorSessionId ([string]$providerState.cursorSession)
    }
    elseif ($runProvider -eq 'grok') {
        $handoffPath = Join-Path $attemptDirectory 'handoff.json'
        $resumeHandoff = [ordered]@{
            kind          = 'grok-resume'
            resumeReason  = $ResumeReason
            inputVersion  = [string]$loaded.task.inputVersion
            mode          = $loaded.mode
            writesSettled = $null
            note          = 'Continues the same Grok session. Does not switch provider or model.'
        }
        Write-AtomicJson -Path $handoffPath -Object $resumeHandoff
        $handoffHash = Get-Sha256Bytes -Bytes ([System.IO.File]::ReadAllBytes($handoffPath))
        $effective = New-GrokPrompt -TaskPath $loaded.taskFull -PromptPath $loaded.promptFull -HandoffPath $handoffPath -Directory $attemptDirectory
        $effectiveHash = [string]$effective.hash
        $meta = New-AttemptMeta -Context $loaded -Identifier $identifier -SessionId $runSession -InputHash $inputHash -TaskHash $taskHash -PromptHash $promptHash -Provider 'grok' -SwitchReason $ResumeReason -EffectivePromptHash $effectiveHash -HandoffHash $handoffHash -CursorSessionId ([string]$providerState.cursorSession)
    }
    else {
        $parentForCursor = $null
        $switchForCursor = $null
        if ($null -ne $script:PendingRecovery) {
            $parentForCursor = [string]$script:PendingRecovery.parentAttemptId
            $switchForCursor = $ResumeReason
        }
        $meta = New-AttemptMeta -Context $loaded -Identifier $identifier -SessionId $runSession -InputHash $inputHash -TaskHash $taskHash -PromptHash $promptHash -Provider 'cursor' -ParentAttemptId $parentForCursor -SwitchReason $switchForCursor
        if ($null -ne $script:PendingRecovery) {
            $meta.recoveredFromSessionId = [string]$script:PendingRecovery.oldSession
            $meta.recoveredFromAttemptId = [string]$script:PendingRecovery.parentAttemptId
            $meta.previousInputVersion = [string]$script:PendingRecovery.previousInputVersion
            $meta.sessionRecovery = 'persistent-ask'
        }
    }
    if ($null -ne $accountReceipt) {
        $meta.accountQuotaEvidence = $accountReceipt
        Write-AtomicJson -Path (Join-Path $attemptDirectory 'account-quota-evidence.json') -Object $accountReceipt
    }
    Write-AtomicJson -Path (Join-Path $attemptDirectory 'attempt.json') -Object $meta

    $attemptIds = New-Object System.Collections.Generic.List[string]
    if ($null -ne $storedTask) {
        foreach ($previous in (Get-StoredAttemptIds $storedTask)) { $attemptIds.Add($previous) }
    }
    $attemptIds.Add($identifier)
    $activeForDocument = $(if ($runProvider -eq 'grok' -and -not $grokFirstStart) { 'grok' } else { [string]$providerState.activeProvider })
    $visibleSession = $(if ($activeForDocument -eq 'grok') { $runSession } else { [string]$providerState.cursorSession })
    $taskDocument = New-TaskDocument -Loaded $loaded -StoredTask $storedTask -AttemptIds $attemptIds.ToArray() -LatestAttemptId $identifier -ProviderState $providerState -ActiveProvider $activeForDocument -ProviderSessionId $visibleSession -AccountQuotaReceipt $accountReceipt
    Write-AtomicJson -Path $taskJsonPath -Object $taskDocument

    if ($runProvider -eq 'grok') {
        $agent = $resolvedGrok
        $promptPath = Join-Path $attemptDirectory 'effective-prompt.md'
        $agentArguments = New-GrokArguments -Workspace $loaded.workspace -SessionId $runSession -AskMode $loaded.askMode -PromptPath $promptPath -FirstStart $grokFirstStart
    }
    else {
        $agent = Resolve-CursorAgentPath
        if (-not ($agent -is [System.Collections.IDictionary] -and $agent.Contains('path'))) {
            if ($null -ne $script:PendingRecovery) {
                Restore-UnstartedRecovery -TaskJsonPath $taskJsonPath -AttemptDirectory $attemptDirectory -Detail ([string]$agent.message)
            }
            return $agent
        }
        if ($null -ne $script:PendingRecovery) {
            $handoffPath = Join-Path $attemptDirectory 'session-recovery.json'
            $handoff = [ordered]@{
                kind                    = 'persistent-ask-session-recovery'
                previousCursorSessionId = [string]$script:PendingRecovery.oldSession
                cursorSessionId         = [string]$script:PendingRecovery.newSession
                parentAttemptId         = [string]$script:PendingRecovery.parentAttemptId
                parentReceiptPath       = [string]$script:PendingRecovery.receiptPath
                taskFile                = $loaded.taskFull
                promptFile              = $loaded.promptFull
                inputVersion            = [string]$loaded.task.inputVersion
                previousInputVersion    = [string]$script:PendingRecovery.previousInputVersion
                reason                  = [string]$script:PendingRecovery.reason
                model                   = $FixedModel
                note                    = 'New Cursor session for the same task. The previous session stayed in persisted Ask mode. That read-only positioning does not apply here. Read the current task file, prompt file, and predecessor receipt. Follow the current task authorization. This recovery does not reset batch quota, change taskId, or switch provider.'
            }
            Write-AtomicJson -Path $handoffPath -Object $handoff
            $prompt = New-RecoveryCliPrompt -TaskPath $loaded.taskFull -PromptPath $loaded.promptFull -HandoffPath $handoffPath -ReceiptPath ([string]$script:PendingRecovery.receiptPath)
        }
        else {
            $prompt = New-CliPrompt -TaskPath $loaded.taskFull -PromptPath $loaded.promptFull
        }
        Write-AtomicUtf8 -Path (Join-Path $attemptDirectory 'cli-prompt.txt') -Content $prompt
        $agentArguments = New-AgentArguments -Workspace $loaded.workspace -SessionId $runSession -AskMode $loaded.askMode -Prompt $prompt
    }
    $plan = Get-LaunchPlan -AgentPath $agent.path -AgentArguments $agentArguments
    $stdoutPath = [string]$meta.stdoutPath
    $stderrPath = [string]$meta.stderrPath
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $plan.fileName
    $startInfo.WorkingDirectory = $loaded.workspace
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.CreateNoWindow = $true
    foreach ($argument in $plan.arguments) {
        if ($argument -match '[%]|[\u0000-\u001F]') {
            $meta.lifecycle = 'failed'
            $meta.endedAt = [DateTime]::UtcNow.ToString('o')
            $meta.blocker = 'CLI 参数含有 % 或控制字符，已拒绝执行。'
            $meta.reportStatus = 'unparsed'
            Write-AtomicJson -Path (Join-Path $attemptDirectory 'attempt.json') -Object $meta
            $reportedSession = $runSession
            if ($null -ne $script:PendingRecovery) {
                $reportedSession = [string]$script:PendingRecovery.oldSession
                Restore-UnstartedRecovery -TaskJsonPath $taskJsonPath -AttemptDirectory $attemptDirectory -Detail $meta.blocker
            }
            return New-Response -Ok $false -Code 'unsafe_argument' -ExitCode 2 -Message $meta.blocker -Lifecycle 'failed' -Meta $meta -AttemptDir $attemptDirectory -SessionId $reportedSession -Attempt $identifier -Provider $runProvider
        }
    }
    Add-ProcessArguments -StartInfo $startInfo -Arguments $plan.arguments
    if ($runProvider -eq 'grok') {
        $startInfo.EnvironmentVariables['GROK_WORKFLOWS'] = '0'
        $startInfo.EnvironmentVariables['GROK_MEMORY'] = '0'
        $startInfo.EnvironmentVariables['GROK_DISABLE_AUTOUPDATER'] = '1'
        if ($grokFirstStart) {
            $providerState.grokLaunch = 'starting'
            $providerState.fallbackUsed = $true
            $providerState.activeProvider = 'grok'
            $providerState.fallbackIntent = 'starting'
            $taskDocument = New-TaskDocument -Loaded $loaded -StoredTask $storedTask -AttemptIds $attemptIds.ToArray() -LatestAttemptId $identifier -ProviderState $providerState -ActiveProvider 'grok' -ProviderSessionId $runSession -AccountQuotaReceipt $accountReceipt
            Write-AtomicJson -Path $taskJsonPath -Object $taskDocument
        }
    }
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    try {
        [void]$process.Start()
    }
    catch {
        $meta.lifecycle = 'unknown'
        $meta.endedAt = [DateTime]::UtcNow.ToString('o')
        $meta.blocker = 'CLI 进程没有确认启动。没有生成第二个会话。'
        $reportedSession = $runSession
        if ($null -ne $script:PendingRecovery) {
            $meta.blocker = 'CLI 进程没有确认启动。当前 Cursor 会话没有切换。'
            $meta.sessionSwitched = $false
            $meta.unusedCursorSessionId = [string]$script:PendingRecovery.newSession
            $meta.recoveredFromSessionId = [string]$script:PendingRecovery.oldSession
            $reportedSession = [string]$script:PendingRecovery.oldSession
        }
        Write-AtomicJson -Path (Join-Path $attemptDirectory 'attempt.json') -Object $meta
        if ($reportedSession -ne $runSession) {
            Restore-UnstartedRecovery -TaskJsonPath $taskJsonPath -AttemptDirectory $attemptDirectory -Detail $meta.blocker
        }
        return New-Response -Ok $false -Code 'session_uncertain' -ExitCode 2 -Message $meta.blocker -Lifecycle 'unknown' -Meta $meta -AttemptDir $attemptDirectory -SessionId $reportedSession -Attempt $identifier -Provider $runProvider -FallbackUsed $providerState.fallbackUsed
    }
    if ($runProvider -eq 'grok' -and $grokFirstStart) {
        $providerState.grokLaunch = 'created'
        $providerState.fallbackIntent = 'started'
        $taskDocument = New-TaskDocument -Loaded $loaded -StoredTask $storedTask -AttemptIds $attemptIds.ToArray() -LatestAttemptId $identifier -ProviderState $providerState -ActiveProvider 'grok' -ProviderSessionId $runSession -AccountQuotaReceipt $accountReceipt
        Write-AtomicJson -Path $taskJsonPath -Object $taskDocument
    }
    $meta.cliPid = $process.Id
    try { $meta.cliProcessStartTime = $process.StartTime.ToUniversalTime().ToString('o') } catch { $meta.cliProcessStartTime = $null }
    $stdoutStream = [System.IO.File]::Create($stdoutPath)
    $stderrStream = [System.IO.File]::Create($stderrPath)
    $stdoutCopy = $null
    $stderrCopy = $null
    $exitCode = -1
    try {
        $stdoutCopy = $process.StandardOutput.BaseStream.CopyToAsync($stdoutStream)
        $stderrCopy = $process.StandardError.BaseStream.CopyToAsync($stderrStream)
        Write-AtomicJson -Path (Join-Path $attemptDirectory 'attempt.json') -Object $meta
        $process.WaitForExit()
        $stdoutCopy.Wait()
        $stderrCopy.Wait()
        $exitCode = $process.ExitCode
    }
    finally {
        try {
            if (-not $process.HasExited) { $process.WaitForExit() }
            if ($null -ne $stdoutCopy) { $stdoutCopy.Wait() }
            if ($null -ne $stderrCopy) { $stderrCopy.Wait() }
        }
        catch { }
        $stdoutStream.Dispose()
        $stderrStream.Dispose()
        $process.Dispose()
    }

    $feedback = $null
    $report = [ordered]@{ reportStatus = 'unparsed'; workStatus = $null; blocker = $null; summary = $null; changedFiles = @(); checks = @(); notRun = @(); writesSettled = $null; nextAction = $null }
    $protocol = @()
    if ($runProvider -eq 'grok') {
        $grokOutcome = Read-GrokOutcome -StdoutPath $stdoutPath -StderrPath $stderrPath -SessionId $runSession -Workspace $loaded.workspace -ProviderResultPath (Join-Path $attemptDirectory 'provider-result.json')
        $meta.resultEventCount = $grokOutcome.resultCount
        $meta.reportedModel = $grokOutcome.reportedModel
        $meta.modelAssurance = $grokOutcome.modelAssurance
        $meta.stopReason = $grokOutcome.stopReason
        $meta.providerRequestId = $grokOutcome.requestId
        $meta.exitCode = $exitCode
        $meta.endedAt = [DateTime]::UtcNow.ToString('o')
        $protocol = @($grokOutcome.notes)
        $meta.reasoningRequested = $GrokReasoning
        if ($grokOutcome.reasoningIgnored) { $meta.reasoningAssurance = 'ignored' }
        else { $meta.reasoningAssurance = 'requested_unverified' }
        if ($grokOutcome.toolContract) {
            $meta.lifecycle = 'failed'
            $meta.failureKind = 'tool_contract'
            $meta.isError = $true
            $meta.reportStatus = 'unparsed'
            $meta.workStatus = $null
            $meta.blocker = 'Grok --tools 白名单出现未知工具警告。已停止，没有放宽默认工具，也没有改权限模式。'
        }
        elseif ($grokOutcome.reasoningIgnored) {
            $meta.lifecycle = 'failed'
            $meta.failureKind = 'reasoning_contract'
            $meta.isError = $true
            $meta.reportStatus = 'unparsed'
            $meta.workStatus = $null
            $meta.blocker = '已请求 reasoning high，但 CLI 报告该模型没有推理档位或忽略了 high。这不能记成 High 已生效，也不能证明中转站内部推理强度。'
        }
        elseif ($grokOutcome.approvalRejected) {
            $meta.lifecycle = 'failed'
            $meta.failureKind = 'approval_rejected'
            $meta.isError = $true
            $meta.reportStatus = 'unparsed'
            $meta.workStatus = $null
            $refusal = ''
            foreach ($raw in ([string]$grokOutcome.stderrText -split "`n")) {
                if ($raw -match '(?i)(Auto-review|permission required|cannot ask for approval)') { $refusal = $raw.Trim(); break }
            }
            if ([string]::IsNullOrWhiteSpace($refusal)) { $refusal = 'Grok 审批拒绝。退出码 0 不能把它记成 reported_done。' }
            $meta.blocker = Get-RedactedText $refusal
        }
        elseif ($protocol.Count -gt 0) {
            $meta.lifecycle = 'protocol_error'
            $meta.isError = $true
            $meta.blocker = ($protocol -join ' ')
            $meta.reportStatus = 'unparsed'
            $meta.workStatus = $null
        }
        elseif ($exitCode -ne 0 -or $grokOutcome.errorObject) {
            $meta.lifecycle = 'failed'
            $meta.isError = $true
            $meta.reportStatus = 'unparsed'
            $meta.workStatus = $null
            $stderrTrim = ([string]$grokOutcome.stderrText).Trim()
            if (-not [string]::IsNullOrWhiteSpace($stderrTrim)) {
                $meta.blocker = Get-RedactedText $stderrTrim.Substring(0, [Math]::Min(500, $stderrTrim.Length))
            }
            elseif ($grokOutcome.errorObject -and $null -ne $grokOutcome.result -and (Test-Property $grokOutcome.result 'message')) {
                $meta.blocker = Get-RedactedText ([string]$grokOutcome.result.message)
            }
            else {
                $meta.blocker = "Grok CLI 失败，exit=$exitCode。没有回切 Cursor，也没有换模型。"
            }
        }
        elseif ($null -eq $grokOutcome.result -or [string]::IsNullOrWhiteSpace([string]$grokOutcome.stopReason)) {
            $meta.lifecycle = 'missing_result'
            $meta.isError = $false
            $meta.reportStatus = 'missing'
            $meta.workStatus = $null
            $meta.blocker = '退出码为 0，但没有可识别的 Grok JSON 终局。'
        }
        elseif ([string]$grokOutcome.stopReason -ne 'end_turn') {
            $meta.lifecycle = 'failed'
            $meta.isError = $true
            $meta.reportStatus = 'unparsed'
            $meta.workStatus = $null
            $meta.blocker = "stopReason=$($grokOutcome.stopReason)。这不是完整交付。"
        }
        elseif ([string]$grokOutcome.sessionId -ne $runSession -or [string]::IsNullOrWhiteSpace([string]$grokOutcome.cwd)) {
            $meta.lifecycle = 'protocol_error'
            $meta.isError = $true
            $meta.reportStatus = 'unparsed'
            $meta.workStatus = $null
            $meta.blocker = '终局会话或 cwd 与本次 Grok 调用不一致。'
        }
        else {
            $meta.lifecycle = 'reported'
            $meta.isError = $false
            $feedback = [string]$grokOutcome.resultText
            if ($null -eq $feedback) { $feedback = '' }
            [System.IO.File]::WriteAllText([string]$meta.resultPath, $feedback, $Utf8)
            $report = Parse-WorkReport -Text $feedback
            $meta.reportStatus = [string]$report.reportStatus
            $meta.workStatus = $report.workStatus
            $meta.blocker = $report.blocker
        }
    }
    else {
    $outcome = Read-StreamOutcome -StdoutPath $stdoutPath -StderrPath $stderrPath -SessionId $runSession -EventsPath (Join-Path $attemptDirectory 'events.jsonl') -ResultEventsPath (Join-Path $attemptDirectory 'result-events.jsonl') -ParseErrorPath (Join-Path $attemptDirectory 'parse-errors.log')
    $meta.resultEventCount = $outcome.resultCount
    $meta.requestedModel = $FixedModel
    $meta.reportedModel = $outcome.reportedModel
    $meta.exitCode = $exitCode
    $meta.isError = $outcome.isError
    $meta.endedAt = [DateTime]::UtcNow.ToString('o')
    $protocol = @($outcome.notes)
    if ($protocol.Count -gt 0) {
        $meta.lifecycle = 'protocol_error'
        $meta.blocker = ($protocol -join ' ')
        $meta.reportStatus = 'unparsed'
        $meta.workStatus = $null
    }
    elseif ($exitCode -ne 0 -or $outcome.isError -eq $true) {
        $meta.lifecycle = 'failed'
        if ($null -ne $outcome.resultText) {
            $feedback = $outcome.resultText
            [System.IO.File]::WriteAllText([string]$meta.resultPath, $feedback, $Utf8)
            $report = Parse-WorkReport -Text $feedback
        }
        $meta.reportStatus = [string]$report.reportStatus
        $meta.workStatus = $report.workStatus
        $stderrTrim = ([string]$outcome.stderrText).Trim()
        if (-not [string]::IsNullOrWhiteSpace([string]$report.blocker)) {
            $meta.blocker = [string]$report.blocker
        }
        elseif (-not [string]::IsNullOrWhiteSpace($stderrTrim)) {
            $meta.blocker = $stderrTrim.Substring(0, [Math]::Min(2000, $stderrTrim.Length))
        }
        elseif ($outcome.errorBits.Count -gt 0) {
            $meta.blocker = [string]$outcome.errorBits[0]
        }
        else {
            $meta.blocker = "CLI 失败，exit=$exitCode is_error=$($outcome.isError)。没有自动重试，也没有改为请求批准。"
        }
    }
    elseif ($outcome.resultCount -ne 1 -or $null -eq $outcome.resultText) {
        $meta.lifecycle = 'missing_result'
        $meta.reportStatus = 'missing'
        $meta.workStatus = $null
        $meta.blocker = '退出码为 0，但没有唯一且完整的 result 终态。'
    }
    else {
        $meta.lifecycle = 'reported'
        $feedback = $outcome.resultText
        [System.IO.File]::WriteAllText([string]$meta.resultPath, $feedback, $Utf8)
        $report = Parse-WorkReport -Text $feedback
        $meta.reportStatus = [string]$report.reportStatus
        $meta.workStatus = $report.workStatus
        $meta.blocker = $report.blocker
    }
    $foundQuota = Get-QuotaMatch -StderrText ([string]$outcome.stderrText) -ErrorBits @($outcome.errorBits)
    $channelText = (([string]$outcome.stderrText) + "`n" + [string]$outcome.resultText)
    $channelBlocked = $channelText -match '(?i)(Auto-review|permission required|cannot ask for approval|canceled by user|cancelled by user|aborted by user)'
    if ($null -ne $foundQuota -and $meta.lifecycle -eq 'failed' -and $protocol.Count -eq 0 -and -not $channelBlocked) {
        $meta.failureKind = 'cursor_quota_exhausted'
        $meta.quotaEvidence = $foundQuota
        $meta.fallbackEligibility = 'eligible'
    }
    elseif ($null -ne $foundQuota) {
        $meta.fallbackEligibility = 'blocked_protocol'
        $meta.failureKind = $null
    }
    else {
        $meta.fallbackEligibility = 'ineligible'
    }
    }
    $pending = $false
    if ($meta.lifecycle -eq 'reported' -and $meta.reportStatus -eq 'valid' -and $meta.workStatus -eq 'reported_done' -and [string]$loaded.task.operation -eq 'implement') {
        $pending = $true
    }
    $meta.pendingCompile = $pending
    $receipt = [ordered]@{
        reportStatus   = $meta.reportStatus
        workStatus     = $meta.workStatus
        summary        = $report.summary
        changedFiles   = $report.changedFiles
        checks         = $report.checks
        notRun         = $report.notRun
        writesSettled  = $report.writesSettled
        blocker        = $(if ($null -eq $meta.blocker) { $null } else { Get-RedactedText ([string]$meta.blocker) })
        nextAction     = $report.nextAction
        pendingCompile = $pending
        accepted       = $false
        quotaChanged   = $false
        note           = 'lifecycle=reported 只表示 CLI 已结束且终态匹配。报告缺失或无效不是完成。implement 的 reported_done 仍待编译。本工具不编译、不复审、不迁表、不验收，也不改额度。'
    }
    Write-AtomicJson -Path ([string]$meta.receiptPath) -Object $receipt
    Write-AtomicJson -Path (Join-Path $attemptDirectory 'attempt.json') -Object $meta
    $ok = $meta.lifecycle -eq 'reported'
    $exit = $(if ($ok) { 0 } else { 4 })
    $message = switch ($meta.lifecycle) {
        'reported' { 'CLI 已结束并收到匹配终态。这是 reported，不是验收完成。' }
        'failed' { 'CLI 失败（退出码非 0 或 is_error=true）。自报状态不能把它改成成功。' }
        'missing_result' { '退出码为 0，但缺少完整终态。' }
        'protocol_error' { '协议异常，没有把某一条结果当成成功。' }
        default { [string]$meta.lifecycle }
    }
    $eligibility = 'ineligible'
    if (Test-Property $meta 'fallbackEligibility') { $eligibility = [string]$meta.fallbackEligibility }
    $usedNow = $false
    if ($runProvider -eq 'grok') { $usedNow = $true }
    return New-Response -Ok $ok -Code $meta.lifecycle -ExitCode $exit -Message $message -Lifecycle $meta.lifecycle -Meta $meta -WorkStatus $meta.workStatus -ReportStatus $meta.reportStatus -Feedback $feedback -Receipt $receipt -AttemptDir $attemptDirectory -SessionId $runSession -Attempt $identifier -PendingCompile $pending -NextAction $receipt.nextAction -Provider $runProvider -FallbackEligibility $eligibility -FallbackUsed $usedNow
}

function Invoke-Status {
    $loaded = Get-TaskContext
    if (-not ($loaded -is [System.Collections.IDictionary] -and $loaded.Contains('context'))) { return $loaded }
    $batchId = [string]$loaded.task.batchId
    $taskId = [string]$loaded.task.taskId
    $orphan = Get-OrphanState $loaded
    if ($null -ne $orphan) {
        return New-StatusResponse -Ok $false -Code 'recovery_required' -ExitCode 2 -Lifecycle 'unknown' `
            -BatchId $batchId -TaskId $taskId -Model $FixedModel -RequestedModel $FixedModel `
            -ProcessAlive $false -NeedsHuman $true -Paths $orphan `
            -Message '初始化中断，task.json 尚未落盘。' `
            -NextAction '人工对账 paths。不要改走 Start 或 Resume 来自动补上。'
    }
    $taskJsonPath = Join-Path $loaded.taskRoot 'task.json'
    if (-not (Test-Path -LiteralPath $taskJsonPath -PathType Leaf)) {
        return New-StatusResponse -Ok $false -Code 'task_missing' -ExitCode 2 -Lifecycle $null `
            -BatchId $batchId -TaskId $taskId -Model $FixedModel -RequestedModel $FixedModel `
            -ProcessAlive $false -NeedsHuman $false `
            -Message '没有任务记录。' -NextAction '首次执行使用 Start。'
    }
    $storedTask = Read-JsonFile -Path $taskJsonPath
    $ids = Get-StoredAttemptIds $storedTask
    $sessionId = [string]$storedTask.sessionId
    if ($ids.Count -eq 0) {
        return New-StatusResponse -Ok $true -Code 'no_attempt' -ExitCode 0 -Lifecycle 'unknown' `
            -BatchId $batchId -TaskId $taskId -SessionId $sessionId -Model $FixedModel -RequestedModel $FixedModel `
            -ProcessAlive $false -NeedsHuman $false `
            -Message '会话已建立，还没有 attempt。' -NextAction '使用 Resume 做第一次执行。'
    }
    $latest = [string]$ids[$ids.Count - 1]
    if ((Test-Property $storedTask 'latestAttemptId') -and -not [string]::IsNullOrWhiteSpace([string]$storedTask.latestAttemptId)) {
        $latest = [string]$storedTask.latestAttemptId
    }
    $directory = Get-AttemptDirectory -Context $loaded -Identifier $latest
    $meta = Read-AttemptMeta -Directory $directory
    if ($null -eq $meta) {
        return New-StatusResponse -Ok $false -Code 'unknown' -ExitCode 0 -Lifecycle 'unknown' `
            -BatchId $batchId -TaskId $taskId -Attempt $latest -SessionId $sessionId -Model $FixedModel -RequestedModel $FixedModel `
            -ProcessAlive $false -NeedsHuman $true `
            -Message '最新 attempt 没有元数据。' -NextAction '人工对账 attempt 目录。不能当成完成。'
    }
    $lifecycle = [string]$meta.lifecycle
    $processAlive = $false
    if ($lifecycle -eq 'running') {
        $taskLockPath = Join-Path $loaded.taskRoot 'task.lock'
        $probe = Open-ExclusiveFile -Path $taskLockPath
        if ($null -eq $probe) {
            $lifecycle = 'running'
            $processAlive = $true
        }
        else {
            $script:TaskLock = $probe
            try {
                $lifecycle = Update-StaleAttempt -Directory $directory
                $meta = Read-AttemptMeta -Directory $directory
                $processAlive = $lifecycle -eq 'running'
            }
            finally {
                Close-OwnedLocks
            }
        }
    }
    $work = $null
    $reportStatus = $null
    $reportedModel = $null
    $requestedModel = $FixedModel
    $model = $FixedModel
    if ($null -ne $meta) {
        if (Test-Property $meta 'workStatus') { $work = $meta.workStatus }
        if (Test-Property $meta 'reportStatus') { $reportStatus = [string]$meta.reportStatus }
        if ((Test-Property $meta 'model') -and -not [string]::IsNullOrWhiteSpace([string]$meta.model)) { $model = [string]$meta.model }
        if ((Test-Property $meta 'requestedModel') -and -not [string]::IsNullOrWhiteSpace([string]$meta.requestedModel)) { $requestedModel = [string]$meta.requestedModel }
        if (Test-Property $meta 'reportedModel') { $reportedModel = $meta.reportedModel }
        if ((Test-Property $meta 'sessionId') -and -not [string]::IsNullOrWhiteSpace([string]$meta.sessionId)) { $sessionId = [string]$meta.sessionId }
    }
    $needsHuman = $true
    if ($lifecycle -eq 'running' -or ($lifecycle -eq 'reported' -and $reportStatus -eq 'valid')) { $needsHuman = $false }
    $next = '查看 Result 取回执。这不是验收。'
    $note = '终态来自元数据。'
    if ($lifecycle -eq 'running') {
        $note = '仍在运行。'
        $next = '等待结束。不要另起 Start。'
    }
    elseif ($lifecycle -eq 'interrupted' -or $lifecycle -eq 'unknown') {
        $note = '没有可靠终态。'
        $next = '人工对账后再决定是否 Resume。不能当成完成。'
    }
    elseif ($lifecycle -eq 'failed') {
        $note = 'CLI 失败。'
        $next = '查看 Result。不能当成完成。'
    }
    elseif ($lifecycle -eq 'missing_result' -or $lifecycle -eq 'protocol_error') {
        $note = '终态不可用。'
        $next = '查看 Result。不能当成完成。'
    }
    elseif ($reportStatus -ne 'valid') {
        $note = '自报不完整。'
        $next = '查看 Result。缺报告不是完成。'
    }
    $providerState = Get-ProviderState $storedTask
    $provider = [string]$providerState.activeProvider
    if ($null -ne $meta -and (Test-Property $meta 'provider') -and -not [string]::IsNullOrWhiteSpace([string]$meta.provider)) {
        $provider = [string]$meta.provider
    }
    $eligibility = 'ineligible'
    if ($providerState.fallbackUsed -or $providerState.activeProvider -eq 'grok') {
        $eligibility = 'used'
    }
    elseif ($null -ne $meta -and (Test-Property $meta 'fallbackEligibility') -and -not [string]::IsNullOrWhiteSpace([string]$meta.fallbackEligibility)) {
        $eligibility = [string]$meta.fallbackEligibility
    }
    if ($eligibility -eq 'eligible') {
        $note = 'Cursor 额度耗尽，可以切换一次。'
        $next = '对账收束后使用 Resume -UseQuotaFallback。本状态没有启动 Grok。'
    }
    return New-StatusResponse -Ok $true -Code $lifecycle -ExitCode 0 -Message $note -Lifecycle $lifecycle `
        -BatchId $batchId -TaskId $taskId -Attempt $latest -SessionId $sessionId -Model $model `
        -RequestedModel $requestedModel -ReportedModel $reportedModel -WorkStatus $work -ReportStatus $reportStatus `
        -ProcessAlive $processAlive -NeedsHuman $needsHuman -NextAction $next `
        -Provider $provider -FallbackUsed ([bool]$providerState.fallbackUsed) -FallbackEligibility $eligibility
}

function Invoke-ResultQuery {
    $loaded = Get-TaskContext
    if (-not ($loaded -is [System.Collections.IDictionary] -and $loaded.Contains('context'))) { return $loaded }
    $orphan = Get-OrphanState $loaded
    if ($null -ne $orphan) {
        return New-RecoveryResponse -Orphan $orphan
    }
    $taskJsonPath = Join-Path $loaded.taskRoot 'task.json'
    if (-not (Test-Path -LiteralPath $taskJsonPath -PathType Leaf)) {
        return New-Response -Ok $false -Code 'task_missing' -ExitCode 2 -Message '没有任务记录。' -NextAction '首次执行使用 Start。'
    }
    $storedTask = Read-JsonFile -Path $taskJsonPath
    $ids = Get-StoredAttemptIds $storedTask
    $selected = $null
    if (-not [string]::IsNullOrWhiteSpace($AttemptId)) {
        if (-not (Test-StableId $AttemptId)) {
            return New-Response -Ok $false -Code 'invalid_args' -ExitCode 2 -Message 'AttemptId 非法。'
        }
        $selected = $AttemptId
        if ($ids -notcontains $selected) {
            return New-Response -Ok $false -Code 'attempt_missing' -ExitCode 2 -Message "没有这个 attempt: $selected"
        }
    }
    elseif ($ids.Count -gt 0) {
        $selected = [string]$ids[$ids.Count - 1]
        if ((Test-Property $storedTask 'latestAttemptId') -and -not [string]::IsNullOrWhiteSpace([string]$storedTask.latestAttemptId)) {
            $selected = [string]$storedTask.latestAttemptId
        }
    }
    if ([string]::IsNullOrWhiteSpace($selected)) {
        return New-Response -Ok $false -Code 'no_attempt' -ExitCode 2 -Message '还没有可展示的 attempt。' -SessionId ([string]$storedTask.sessionId)
    }
    $directory = Get-AttemptDirectory -Context $loaded -Identifier $selected
    if ($null -eq $directory) {
        return New-Response -Ok $false -Code 'invalid_args' -ExitCode 2 -Message 'AttemptId 越出了任务目录。'
    }
    $meta = Read-AttemptMeta -Directory $directory
    if ($null -eq $meta) {
        return New-Response -Ok $false -Code 'unknown' -ExitCode 0 -Message 'attempt 元数据缺失。' -Lifecycle 'unknown' -Attempt $selected -AttemptDir $directory
    }
    $feedback = $null
    if ((Test-Property $meta 'resultPath') -and (Test-Path -LiteralPath ([string]$meta.resultPath) -PathType Leaf) -and [string]$meta.lifecycle -ne 'protocol_error') {
        $feedback = Read-Utf8Text -Path ([string]$meta.resultPath)
    }
    $receipt = $null
    if ((Test-Property $meta 'receiptPath') -and (Test-Path -LiteralPath ([string]$meta.receiptPath) -PathType Leaf)) {
        $receipt = Read-JsonFile -Path ([string]$meta.receiptPath)
    }
    $work = $null
    $reportStatus = $null
    $pending = $false
    if (Test-Property $meta 'workStatus') { $work = $meta.workStatus }
    if (Test-Property $meta 'reportStatus') { $reportStatus = [string]$meta.reportStatus }
    if (Test-Property $meta 'pendingCompile') { $pending = [bool]$meta.pendingCompile }
    if ($null -ne $feedback) { $feedback = Get-RedactedText $feedback }
    $resultProvider = 'cursor'
    if ((Test-Property $meta 'provider') -and -not [string]::IsNullOrWhiteSpace([string]$meta.provider)) { $resultProvider = [string]$meta.provider }
    $resultState = Get-ProviderState $storedTask
    $resultEligibility = 'ineligible'
    if ($resultState.fallbackUsed -or $resultState.activeProvider -eq 'grok') { $resultEligibility = 'used' }
    elseif ((Test-Property $meta 'fallbackEligibility') -and -not [string]::IsNullOrWhiteSpace([string]$meta.fallbackEligibility)) { $resultEligibility = [string]$meta.fallbackEligibility }
    return New-Response -Ok $true -Code ([string]$meta.lifecycle) -ExitCode 0 -Message '以下是已保存的最终反馈和回执。没有重新解析运行中的原始流。' -Lifecycle ([string]$meta.lifecycle) -Meta $meta -WorkStatus $work -ReportStatus $reportStatus -Feedback $feedback -Receipt $receipt -AttemptDir $directory -SessionId ([string]$meta.sessionId) -Attempt $selected -PendingCompile $pending -AttemptIds $ids -Provider $resultProvider -FallbackEligibility $resultEligibility -FallbackUsed ([bool]$resultState.fallbackUsed)
}

$response = $null
try {
    $argumentGuard = Test-LauncherArguments
    if ($null -ne $argumentGuard) {
        $response = $argumentGuard
    }
    elseif ($Action -eq 'Start' -or $Action -eq 'Resume') {
        $response = Invoke-StartOrResume
    }
    elseif ($Action -eq 'Status') {
        $response = Invoke-Status
    }
    else {
        $response = Invoke-ResultQuery
    }
}
catch {
    $script:ExitCode = 4
    $response = [ordered]@{
        ok           = $false
        code         = 'internal_error'
        action       = $Action
        message      = $_.Exception.Message
        detail       = $_.ScriptStackTrace
        accepted     = $false
        quotaChanged = $false
        complete     = $false
        lifecycle    = $null
    }
}
finally {
    Close-OwnedLocks
}

if ($null -eq $response) {
    $response = [ordered]@{ ok = $false; code = 'internal_error'; action = $Action; message = '没有产生结果。'; accepted = $false; quotaChanged = $false; complete = $false }
    $script:ExitCode = 4
}
[Console]::OutputEncoding = $Utf8
[Console]::Out.WriteLine(($response | ConvertTo-Json -Depth 30))
exit $script:ExitCode
