---
name: max-dispatch
description: "Use for execution work such as max-parallel audit, implementation, fixes, or compile, or when the user says 用ds读取, 汇总给gpt, 拉满效率, 最大数量子agent, 通读审查, 并行改然后4.7审核. Direct design discussion in Codex is outside this skill: the Codex main session answers it and does not dispatch Cursor or start a subagent. A historical mention of DS does not change the current Cursor CLI --model grok-4.7-high-fast primary channel; a new explicit user authorization takes priority. If this task's latest Cursor run exhausted quota, or the caller passes still-valid same-account quota evidence, the launcher may use Grok CLI alias grok-4.7 once. Ordinary failures, TLS, and approval refusals do not become quota. During execution, hard decisions use one gpt-6-astra only while that stage's Codex quota remains. While that stage's Codex quota remains, the main session may do a concentrated logic review, and each formal correctness judgment, including each repair re-review, counts as one round whether it passes or not; after the quota it only dispatches, waits for final feedback, and relays the work result, without another review by Cursor, another Codex agent, or a new session, and without following live work. Extra review starts only after the user explicitly agrees. Formal review, while quota remains or extra review is approved, waits until the current stage's dispatched tasks finish and return final feedback. Design checks are at most 2 rounds and implementation checks at most 3. There is no independent test stage. After implementation and compile, sheet writeback can move a finished item to pending acceptance without another review. Sheet writeback is required before delivery."
when-to-use: "只覆盖执行。直接讨论设计由 Codex 主会话回答，不派 Cursor。用ds读取 汇总给gpt 拉满效率 最大数量子agent 通读 并行改 4.7审核。历史记录提到 DS 不改变当前 High Fast 主渠道；用户新明确授权优先。本 task 的 Cursor 周额度失败，或仍有效的同账户额度证据，可按流程第11节走 Grok CLI 别名 grok-4.7。普通失败、TLS 和审批拒绝不改写成额度。额度未用完或已获追加授权时，正式复审等当前阶段可推进任务完成并返回最终反馈后集中进行。派发后等最终反馈再复审，不实时跟进工作内容。设计校验最多 2 轮，实现校验最多 3 轮，返修复审同样累计；用完后不自动追加复审，只整理工作结果。没有独立测试阶段。交付条件满足可迁待验收，交付前要写回制作表。手动启动见流程第11节 Invoke-CursorTask.ps1，不是后台调度。新 attempt 或 session 不重置批次额度。"
user-invocable: true
argument-hint: "[read|fix] [scope]"
---

# Max Dispatch

主渠道是 Cursor CLI Grok 4.7 High Fast。用户 2026-11-04 授权的唯一备用是 Grok CLI 别名 `grok-4.7`（wire `grok-4.7-build-fast`，请求 high）。入口有两个，都按流程第 11 节：该 task 最新 Cursor 运行确认周额度耗尽，或仍有效且同一登录范围的账户额度证据。证据过期、换登录或额度恢复后，新任务回到 Cursor。已经在 Grok 上的任务继续原会话。不得自行降档或改用其他模型。

本技能只用于执行任务。用户直接探讨玩法、机制、需求、方案或设计取舍时，当前 Codex 主会话直接理解、推理、回答并提出建议，不派给 Cursor，也不以先启动子代理或执行流水线为回答前提。提到术式、待制作表或设计较复杂，不自动转执行。用户明确要求按表制作、实现、修复或编译时，才用下面的派发；主会话负责调度和既有额度内的逻辑复核。同一条消息兼有两者时，主会话直接回答设计部分，已授权且设计明确的执行部分再按原流程交 Cursor。讨论本身不自动领表、改代码、编译或改表状态；已经明确授权的执行不重复要求确认。一般设计交流不计批次校验轮次；对现有批次的正式复审和返修后的正式复审仍按阶段额度计数，不能改称设计讨论来避开上限。额度用完后不自动追加复审。没有独立测试阶段。设计最多 2 轮，实现最多 3 轮。完整规则见 `docs/统一开发与术式批量制作流程.md`。

下面两条只约束执行任务，并先于执行流水线：

1. **并行优先。** 执行任务中主会话只调度。该阶段 Codex 额度未用完时，可按已记录轮次集中做逻辑复核，每次正式正确性判断算该阶段一轮；额度用完后，当次不再自动追加该阶段复审，主会话只派发、等待最终反馈并转达 Cursor 的工作结果，不实时跟进其工作内容，不重新判断正确性，也不交给 Cursor 或其他会话继续复审。不必用满该阶段上限。不通读整仓，不直接实现 Java，不直接跑 `gradlew`。
2. **难度升高时先看额度。** 本条只用于执行任务。直接讨论设计由 Codex 主会话直接回答，不先问 `gpt-6-astra`，也不交给 Cursor。执行任务的计数规则优先。执行中的新需求比查一个符号、读一个文件更难，或明显难时，先看该阶段 Codex 额度；未用完才只问 `gpt-6-astra`，不要先自己试。额度用完后，不再请脑子或其他子代理重审，也不自动把该阶段复审交给 Cursor。额度内问完再交 Cursor CLI 落地。额度未用完或已获追加授权时，当前阶段可推进的 Cursor 任务完成并返回最终反馈、写入收束后集中复核。

Cursor 的实现、修复、编译和定向代码检查**不是** `spawn_subagent`。主渠道命令行显式带 `--model grok-4.7-high-fast`。禁止 Low、`grok-4.5`、省略模型、自动换模型。该 task 自己的 Cursor 周额度失败，或主会话显式带上仍有效且同一登录范围的账户额度证据时，才按流程第 11 节走 Grok CLI。新 task 可以因此直接 Start。证据有期限，不跨账户。恢复或到期后新 task 回到 Cursor；已经在 Grok 上的 task 继续原会话。普通失败、TLS 和审批拒绝都不是额度，也不能被改写成额度。备用失败时停下来报告真实阻塞，不擅自降档，主会话不接管实现。

真正 spawn 子代理时，每次都必须写 `model`。该阶段额度未用完时，困难裁决的 model 参数是 `gpt-6-astra`，只开一个。额度用完后，Codex 不再为同阶段校验 spawn 脑子或其他子代理，也不自动 spawn Cursor 继续复审。省略 model 会继承主会话。禁止 `grok-4.5`。

## 模型

| 活 | 派法 |
|---|---|
| 直接讨论设计 | 当前 Codex 主会话直接回答；不派 Cursor，不先开子代理或执行流水线 |
| 实际美术制作或修改 | 先读取 `~/.codex/skills/xueximod-art-preview/SKILL.md`（`C:/Users/baishang/.codex/skills/xueximod-art-preview/SKILL.md`），再按 `docs/统一开发与术式批量制作流程.md` 第 5 节执行。同一 MCP 实例串行。主会话可见而 Cursor 不见时，由主会话做美术 MCP，仍要真实 Blockbench 和同版本确认。主会话不因此实现 Java。直接讨论设计不走这条。判定无 MCP 或转桌面之前，主会话先按美术技能「发现与连接」检查；首次空目录先查就绪，启动或恢复后重新发现验证，CLI 不可见不能代替主会话检查 |
| 代码实现、修复、编译、定向代码检查 | 主渠道 Cursor CLI `--model grok-4.7-high-fast`。本 task 的周额度失败，或仍有效的同账户额度证据，按流程第 11 节走 Grok CLI 别名 `grok-4.7`。实现交付写待统一编译。默认不派测试 |
| 制作表写回 | 开批指定的唯一 Cursor 写回执行者。先留待制作原始数据，再迁四张状态页。源码实现者不改表。主会话不亲自写回 |
| 读代码 | 同一个 Cursor 模型；主会话只查单个符号或读文档 |
| 执行任务中、额度内的困难 / 架构裁决 | spawn，且 `model` 为 `gpt-6-astra`，只开一个；额度用完后不自动改交 Cursor 复审，无法在已批准设计内执行则报告阻塞 |
| 阶段内集中只读复核 | 仅额度未用完或用户已明确同意追加时，独立 Cursor CLI 只读会话，同一 `--model grok-4.7-high-fast`；阶段内集中，不强制每术式各开会话。额度用完且未授权则不启动 |
| 主会话逻辑复核或汇总 | 额度未用完：按已记录轮次集中做逻辑复核，每次正式正确性判断算该阶段一轮，返修复审同样计数；额度用完：只派发、等待最终反馈并转达 Cursor 工作结果，不自动追加复审，不实时跟进工作内容，不重新判断正确性。不必用满该阶段上限 |
| 禁止 | Low、`grok-4.5`、省略或自动换模型、降档、主会话直接实现或编译、把 DeepSeek 当本项目实现者 |

## 手动启动

派发和续跑用 `.grok/scripts/Invoke-CursorTask.ps1`。契约在 `docs/统一开发与术式批量制作流程.md` 第 11 节，这里不另写一套状态。备用渠道的操作入口是个人技能 `C:/Users/baishang/.codex/skills/xueximod-grok-fallback/SKILL.md`，只说明何时走这一次切换；本技能不复制其步骤，也不把它当成已经验收。引用就位时该文件仍由另一聊天制作。先指定唯一文件作者，且依赖已有完成回执，再派发。`Start` 只用于该 task 尚无运行记录时：先 `create-chat`，按实际返回取 UUID，再 `--resume` 这个 UUID。范围内返修沿用原 task 与当前 provider 的会话。`Resume` 前由主会话对账，并写 `-ResumeReason`。Cursor 每次执行都 `--resume` 指定的 Cursor 会话。普通 Resume 不新建 Cursor 会话。固定 CLI 的 `--mode` 只有 `plan` 和 `ask`，不能把已持久化的 Ask 切成写模式。只有主会话显式 `Resume -WriteMode -RecoverPersistentAsk`，且最新失败确属已记录的持久化 Ask 阻塞、进程已收束时，才用既有 `create-chat` 的真实 UUID 恢复同一 task 的当前 Cursor 会话；旧会话和旧 attempt 保留，batchId 额度不重置。审批拒绝、协议异常、取消、额度切换、活进程和未收束写入不能借这个入口绕过。本 task 额度耗尽且对账收束后，才 `Resume -UseQuotaFallback` 一次。同一账户仍有效的证据用 `-AccountQuotaEvidence` 显式带上，新 task 可直接 Start Grok，尚未切换且最新失败已收束、又不是审批拒绝的 task 也可切换一次。之后只 resume 已保存的 Grok 会话。不用 `--continue`。启动器不证明登录身份，也不后台守着额度。Status 只返回最小状态，不返回 raw 或完整 meta；完整回执用 Result。raw 只在故障或用户要求时人工查看。初始化中断若留下 attempt 但缺少 task.json，由人写恢复记录；Start 与 Resume 不互相循环提示，也不自动清理或重跑。`stage=delivery` 不另开复审额度。lifecycle 的 `reported`、模型自报的 `workStatus`，和制作表四页分开。脚本不验证业务权限、依赖对错、复审额度或全局文件锁。

## 流水线（不可跳步）

1. **切块** — 按文件切开。同一文件只能一个作者。并发看当时可用槽位，不写死人数。公共注册器、索引和表由一个整合者改。
2. **读** — 需要成片通读时用 Cursor CLI 同一模型，只读，路径写死。主会话不通读整仓。摘要被截断就在同一会话补全，不要另开一个凭印象猜。
3. **等齐再汇总** — 主会话压成事实清单（路径、行号、现象）。额度内的困难问题**只开一个** `gpt-6-astra`。该阶段额度用完后不再请它重审。额度内的困难问题没拍板前不要改代码。
4. **改** — 只改用户已授权范围内的项；已批准设计与素材可按批量授权推进。没有否决不等于授权，不能越过未确认视觉节点。额度内的困难项必须已经由 `gpt-6-astra` 拍板；额度用完后，已授权的实现仍由 Cursor High Fast 做，但不再自动追加该阶段复审。实现用 Cursor CLI `--model grok-4.7-high-fast`。互斥文件可以并行。用户没让删的系统不要删。已有未提交改动保留，禁止 reset、clean、checkout 覆盖。
5. **当前阶段收束后集中审** — 仅当该阶段额度未用完或用户已明确同意追加时，本阶段已派发且可推进的 Cursor 任务全部完成或交接并返回最终反馈、相关写入收束后，再集中只读复核。额度耗尽且未获追加授权时不启动本步复审，只转达工作结果。模型仍是 `--model grok-4.7-high-fast`。可用独立只读会话，阶段内集中进行，不强制每术式各开会话。只等当前阶段，不等还没开始的实现。阻塞项记下原因和下一步并单列；可推进项形成 ready 子批集中审，拆子批不重置轮次。未通过则只返修对应项。该阶段 Codex 额度未用完时，主会话可按已记录轮次集中做逻辑复核，每次正式正确性判断算该阶段一轮；额度用完后只派发、等待最终反馈并转达 Cursor 的工作结果，不自动追加该阶段复审。不必用满该阶段上限。没跑的验证必须明说。必要的阻塞或风险通报可以立即止损，这不算按项开复审。额度未用完或已获追加授权时，派发后等待完成并返回最终反馈再复审。执行期间不读取思考过程、流式输出、工具调用详情、中间代码，不重复扫描 diff，不边执行边审查。必须轮询时只区分运行中、已完成、失败、需要输入，不拉取工作内容；优先完成通知或等待工具。失败、阻塞、需要用户输入或用户要求查看进度时，才按需读取必要信息。文中的跟踪和进度只指这件事。完整规则见 `docs/统一开发与术式批量制作流程.md`。
6. **编译** — 相关源码还在写入时不启动统一编译。同一工作区 Gradle 串行。最终编译必须对着已经收束的最终源码。由 Cursor 在项目根执行 `.\gradlew.bat compileJava --offline --no-daemon`（`org.gradle.jvmargs=-Xmx3G`，daemon 关着）。默认不打包、不 `runClient`、不启动游戏。游戏效果由用户实测。编译失败不能迁待验收。编译通过后进入写回，不另派独立测试。
7. **写回制作表** — 开批指定的写回执行者先把首次待制作原始行留进「待制作原始数据」，再把四张状态页实际迁走：领取后进制作中；实现完成、写入收束且编译等交付条件满足后进待验收，不因没有额外复审而卡住；用户游戏实测通过后进已完成。只写进度文档不算完成。然后才交付用户实测。

批量规则不在这里展开，避免和正文各写一套状态。入口是 `docs/统一开发与术式批量制作流程.md` 和 `docs/术式制作表.xlsx` 的「待制作」页。队列为空不从「已完成」抓任务。`N`、`O`、`DV` 的语义、快照和写回顺序只以该流程为准。

## 校验额度

设计最多 2 轮、实现最多 3 轮，分别计数，不必用满。没有独立测试阶段。Codex 主会话和全部 Codex 子代理共享本批该阶段计数。不按文件、术式、ready 子批、新会话、新代理、拆项、改名或重启重置；返修和版本变化仍承接该阶段。多次返修后的每次正式复审各占一轮，无论通过与否都计数。设计例：初审 1/2，返修后复审 2/2，其后不能自动开第三次。实现例：初审 1/3，第一次返修后复审 2/3，再次返修后复审 3/3，其后不能自动开第四次。一次正式正确性或验收条件审查并给出结论算一轮，同轮的读取和汇总仍是这一轮。领取、读输入、进度、协调、阻塞通报和转达 Cursor 结果不算轮次，但不得借此重复正确性审查。计数规则优先于无条件先问脑子。涉及正式校验判断的脑子裁决，调用前先看实际阶段额度，并按该阶段计数：开工前的方案裁决算设计，代码复核算实现，不因脑子身份一律算设计。纯调度和信息采集不算校验。父子代理各自重审不能并成一轮。该阶段额度用完后，当次不再自动追加该阶段复审，只派发、等待最终反馈并转达 Cursor 的工作结果。不得交给 Cursor、另一 Codex 子代理或新会话继续复审，也不能把复审说成咨询、定向检查、设计讨论或改阶段名。已授权的 Cursor 返修、实现、编译和必要定向检查可以完成，用户实测不受复审额度限制。实现完成、写入收束且编译等交付条件满足时可以迁待验收，不因没有额外复审或独立测试卡住。额度耗尽不是完成，不能掩盖编译失败或实现未完成，也不能写成已经新增复审通过。追加复审须用户明确同意，并记录授权与累计轮次，不重置原计数；默认执行者仍是 Cursor Grok 4.7 High Fast，用户明确选择 Codex 时按该授权执行。交付区分 Cursor 报告的结果与已做复审的结论，额度用完且未追加时记录「本阶段额度已用完，返修结果未追加复审」。一般设计交流不计上述轮次；对现有批次的正式正确性或验收复审，以及返修后的正式复审，仍计所属阶段额度，不能改称设计讨论来避开上限。完整追加与记录规则见 `docs/统一开发与术式批量制作流程.md`。

## 任务说明

- 范围：允许读或改的绝对路径
- 目标：一条结论，不要整仓
- 约束：禁止改范围外文件。Cursor 任务写明 `--model grok-4.7-high-fast`
- 产出：路径、行号、做了什么或发现什么
- 本阶段：只读定位、实现、只读独立审查、统一编译与验证、制作表写回，按流程择一写明。默认不含独立测试。编译通过后还要写回，不能停在编译
- 复审时点：该阶段额度未用完或已获追加授权时，当前阶段可推进任务完成并返回最终反馈后集中复审；否则只交回工作结果，不自动复审。执行期间不跟进工作内容。本单不单独开逐项正式复审。完整等待与额度规则见流程正文
- 输入：详细 `N`、`O`、`DV` 与最新已批准需求版本；待验收的一句话和资源名不能代替详细需求
- 文件所有权：写明本任务的文件所有者和允许写的路径
- 编译授权默认无；只有被指定的编译执行者，在主会话确认相关写入已经收束后才编译
- 只读审查不写文件。编译发现问题交回作者，不边改边编译

## 红线

- 主会话通读整仓、直接实现 Java、直接跑 `gradlew`
- 用 DeepSeek、Low、`grok-4.5` 或别的模型代替主渠道 `grok-4.7-high-fast` 做实现、编译或定向检查；普通失败时换渠道，或在额度耗尽后改用 `grok-4.6` / `grok-4.5`
- spawn 时省略 model，或写成 `grok-4.5`
- 模型不可用时擅自降档，或改由主会话接管实现
- 该阶段额度未用完时，困难项还没有 `gpt-6-astra` 裁决就开始改
- 该阶段 Codex 额度用完后，仍请脑子或其他子代理重审，自动交给 Cursor 或其他会话继续复审，或把重审说成咨询、定向检查、设计讨论、改阶段名
- 两个作者写同一文件
- 每项结束立刻开正式复审
- 等尚未开始的后续阶段，才复核当前阶段
- 用返修、版本变化、新会话、新代理、拆项、改名或重启把 Codex 的阶段校验计数清零
- 用 `--continue`、续最近会话，或把缺结果的中断当成成功后盲目重跑
- 把 lifecycle 的 `reported` 或模型自报完成当成复审通过、待验收或表迁移回执
- 额度用完后自动追加同阶段复审，包括交给 Cursor、另一 Codex 子代理或新会话
- 写入未收束就做统一编译，或最终编译没有覆盖最终源码
- 编译通过后不写回制作表，或不把四张状态页按实际阶段迁走
- 把已取消的独立测试重新设成待验收门槛
- 把额度用完当成实现完成，或写成已经新增复审通过
- 从「待制作原始数据」领任务、当作验收依据，或用它回滚现行需求
- 默认打包或启动游戏
- 待制作队列为空时去抓已完成
- 把截断报告当成完整结果
- 派发后实时跟进 Cursor 的思考过程、流式输出、工具调用、中间代码或重复 diff。跟踪只指等待最终反馈和低频最小状态检查
- 实际制作或修改有空间关系的美术时，未先读取 `xueximod-art-preview`，或因为贴图、粒子是二维就跳过真实 Blockbench

| 借口 | 现实 |
|---|---|
| 模块不多，我自己改更快 | 实现、编译和表写回交给 Cursor CLI High Fast；主会话不亲自写回 |
| 额度用完就不能进待验收 | 实现完成、写入收束且编译等交付条件满足就可以迁待验收，并写明未追加复审；编译失败或实现未完成不能迁 |
| 先改着，GPT 回头看 | 额度未用完的困难项，没有裁决就没有改动清单；额度用完则停止该阶段复审并转达工作结果，不自动交 Cursor 或再叫脑子 |
| 每项结束立刻审，或等后续阶段全部做完才审 | 额度未用完或已获追加授权时，只等当前阶段可推进任务完成并返回最终反馈，再集中复审；阻塞项单列 |
| 派发后要实时看思考、流式输出和 diff 才算跟踪 | 额度未用完或已获追加授权时，等到最终反馈再复审；轮询只看运行中、已完成、失败、需要输入 |
| 额度用完就让 Cursor 接着复审 | 只转达工作结果；追加复审要用户明确同意，沉默或继续返修不是授权 |
| 集中审就可以少审掉缺陷 | 集中的是时点；未满足验收不能标通过 |
| 新会话、返修或拆子批可把校验轮次清零 | 设计最多 2 轮、实现最多 3 轮，返修复审同样累计，本批该阶段共享计数挂在 batchId |
| 新 attempt 或换 session 可以重计额度 | 原 batchId 的设计 2 轮、实现 3 轮继续累计 |
| 不写 model，配置会钉住某个模型 | spawn 省略会继承主会话；Cursor 必须写 `--model grok-4.7-high-fast` |
| 历史记录提到 DS | 不改变当前 High Fast 分工；用户新明确授权优先 |
| High Fast 不可用，换近邻模型 | 不降档，报告阻塞并停下 |
