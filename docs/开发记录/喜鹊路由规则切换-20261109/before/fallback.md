---
name: xueximod-grok-fallback
description: "XueXi Mod 执行任务遇到已确认的 Cursor 账户额度耗尽且该确认仍适用时，自动使用 Grok CLI 老猪的中转站（别名 grok-4.7，wire grok-4.7-build-fast，high）续作。不用于普通直接设计讨论。"
---

# XueXi Mod Grok 备用渠道

本技能只用于 XueXi Mod 的执行任务：实现、修复、编译或定向代码检查需要继续，而 Cursor 账户额度已经确认耗尽。玩法、机制、需求、方案或设计取舍由 Codex 主会话直接回答，不派执行渠道。

唯一合同是 `D:/alianximod/docs/统一开发与术式批量制作流程.md` 第 11 节。命令、身份、会话字段和回执以该节为准。本技能只给最短操作说明，不复制全文，不新造状态机，也不发明 CLI 旗标。

执行调度仍看 `D:/alianximod/.grok/skills/max-dispatch/SKILL.md`。本技能只补充备用渠道这一决定。

## 渠道

- 主渠道是 Cursor CLI，模型固定 `grok-4.7-high-fast`。
- 唯一备用是 Grok CLI 别名 `grok-4.7`。显示名是「老猪的中转站」，wire 模型是 `grok-4.7-build-fast`。要求同时是高推理和权限模式 `auto`。
- 启动器必须核对这些要求已经实际生效。命令行带有 `--reasoning-effort high` 或 `--permission-mode auto`，不能单独当成已经生效。不修改用户全局配置，也不在这里推测新的模型参数。
- 不要降档，不要改用 `grok-4.5`、`grok-4.6` 或其他模型，也不要省略模型。

## 何时带上账户证据

不必让每个新 task 先再请求一次 Cursor。用户当前的明确确认可以作为证据来源，调用仍要带上仍有效的证据文件。2026-11-04 的某一次确认不是永久有效。

证据要满足第 11 节的有效期、当前登录范围和未作废。启动器当前拒绝超过 14 天的期限。期限与字段以第 11 节为准。现有证据文件是 `D:/alianximod/docs/开发记录/Grok备用渠道-20261104/account-quota-current.json`。口头确认不能省略 `-AccountQuotaEvidence`。

证据到期、已作废、登录范围已变或额度已恢复时，停止沿用该文件。按第 11 节和用户的新决定处理。不启动后台监控，也不为探额度而反复请求。

这里的自动，是主会话在已有授权下安排这一次启动器调用。它不是后台守护进程，也不是失败后的无限重试。

用户确认和旧的失败记录是两件事。保留 Cursor 失败的原文和类别。不要伪造额度证据，也不要把某次网络失败改写成 quota。结果正文、工具输出或模型自报里的额度字样，不能单独当成错误通道证据。

网络错误、普通 429、认证失败、编译失败和审批拒绝保持各自的类别。已经确认的额度耗尽，也不能用来绕过某次明确的审批拒绝。

## 额度与复审

切换渠道不是复审额度用完后换一个代理继续审。设计最多 2 轮，实现最多 3 轮，由同一个 batch 共享。更换渠道、session、attempt 或 task 都不重置这组计数。该阶段额度用完后，只整理并转达执行结果，不自动追加该阶段复审。没有独立测试阶段。

直接设计讨论由 Codex 主会话回答。对已有批次的正式正确性复审，以及返修后的正式复审，仍按所属阶段计数。

## 调用

常规派发、续跑、状态和回执只用 `D:/alianximod/.grok/scripts/Invoke-CursorTask.ps1`，由 PowerShell 7 的 `pwsh` 显式调用一次。技能不提供绕过启动器的路径。

账户证据仍有效时，新 task 使用 `Start -UseQuotaFallback -AccountQuotaEvidence <证据 JSON 绝对路径>`。同一 task 已有收束的真实失败、需要交接时，使用 `Resume -UseQuotaFallback -AccountQuotaEvidence <证据 JSON 绝对路径> -ResumeReason "<具体对账>"`。写任务再加 `-WriteMode`。不要把 Cursor UUID 传给 Grok。

```powershell
pwsh -NoProfile -File D:\alianximod\.grok\scripts\Invoke-CursorTask.ps1 -Action Start -TaskFile D:\abs\task.json -PromptFile D:\abs\prompt.md -UseQuotaFallback -AccountQuotaEvidence D:\alianximod\docs\开发记录\Grok备用渠道-20261104\account-quota-current.json
pwsh -NoProfile -File D:\alianximod\.grok\scripts\Invoke-CursorTask.ps1 -Action Resume -TaskFile D:\abs\task.json -PromptFile D:\abs\prompt.md -ResumeReason "对账：旧进程与写入子进程已结束，最新真实失败已收束，账户证据仍有效。原失败类别保持不变。" -UseQuotaFallback -AccountQuotaEvidence D:\alianximod\docs\开发记录\Grok备用渠道-20261104\account-quota-current.json
```

只读与写模式、工具过滤跟随启动器合同。只读调用保持只读，不要加 `-WriteMode`。不要打开 `always-approve`、`yolo`、`bypassPermissions` 或 `force`。不要扩大信任，不要读取或抄录密钥和 base URL。

## 交接

派发或切换前，确认旧进程及其写入子进程已经结束，并交接文件所有权。同一文件同时只有一个作者。

保留未提交改动。只做剩余事项。制作表里已经迁走的行不要重领。

移交时带上 batch、task、最新已批准需求、已做与未做、编译与制作表写回状态，以及本批设计与实现额度。

Cursor 与 Grok 各用自己的 session。新的 attempt 承接父 attempt。不要把 Cursor 的 UUID 交给 Grok。普通续跑明确使用已经保存的 Grok UUID。不要使用 `continue` 或 `restore-code`。

## 等待与失败

只等最终回执，或低频查看最小 Status。不读实时思考、流式输出、工具细节或中间代码。Status 没有变化时不报心跳。

Result、模型工作报告、复审结论和制作表状态分开判断。进程退出码 0 只说明进程结束，不代表业务通过、复审通过或可以迁待验收。

Grok 调用失败时保留现场并报告真实阻塞。不要自动更换模型，不要自动切回 Cursor，也不要循环重试。

## 其余工作

主会话继续调度，并只在该阶段剩余额度内做逻辑复核。Java 与编译由当次授权的执行者完成。默认不打包、不启动游戏；游戏效果由用户实测。制作表写回，以及需要空间关系时的真实 Blockbench，仍按原流程和 `C:/Users/baishang/.codex/skills/xueximod-art-preview/SKILL.md`。
