# 同一 fallback-adapter：修正账户级额度复用

继续你原 Grok 会话 48e4111a-a131-46c2-863a-5a521d55d120，同 batch workflow-grok-fallback-20261104、task fallback-adapter。此次是用户纠正后授权的实现修正，不是新增设计复审。设计累计2/2已用完、正式实现复审0/3，不重新计数，不请求子代理。

用户原始要求：Cursor额度用完后自动转Grok CLI老猪的中转站、继续Grok4.7、保持原流程。2026-11-04用户在授权协调的聊天01a17b3e-040c-79b0-a0eb-0c62559eccf6最新明确说：“不对啊不是应该用新流程的中转站方案代替cursor吗，因为cursor额度用完了”。主会话已用read_thread核实userMessage 01a1a41f-4621-7f72-91ee-3943dd3c5d70，已确认本账户当前额度耗尽。旧真实quota来自 .grok/runs/cooperative-slash-20261103/compile-final/attempts/a2026110318480502362ed，错误 Weekly usage limit reached，记录在本目录已核实输入/记录中。个人技能任务的三次TLS失败不是quota，必须保留原样。

现实现把资格硬限为“本task最新Cursor attempt必须quota”，导致账户已确认耗尽，新task仍得重复撞Cursor且被TLS错误卡住。这不符合用户要求。请增加有效账户级quota证据复用，修正统一入口与文档，允许新task直接由Grok启动、已有尚未切换task即使最新TLS失败也可使用独立有效账户quota证据切换。不得重写TLS失败为quota，不得伪造Cursor attempt，不得使用测试替身作生产证据，不得把所有网络/429/认证/审批错误都变成quota。

实现要求：
1. 保留旧的真实同task quota路径，增加账户证据输入与可追溯回执，来源可为已核实真实quota attempt或用户当前明确确认。记录非秘密的账户/当前登录配置范围、来源定位、确认时间、有效期、失效/恢复理由；不要记录token、key、cookie、base_url。证据必须有期限，不能永远沿用或跨不同账户；已知登录切换、明确额度恢复、到期则不得新开备用。没有可靠自动账户身份检查时明确说明主会话要核对当前登录范围，不假装脚本能自动证明；不要为取身份输出认证材料。
2. 主会话可在仍有效且同账户的证据下，对后续任务直接调用统一入口使用固定Grok，不再重复要求用户批准同一已授权切换，也不让每个task先调用耗尽的Cursor。Start直接Grok时正确建立新Grok UUID和任务状态，不生成假的Cursor UUID/attempt。Resume维持已有task和输入/权限/批次/原provider历史；每task最多切换一次，后续普通Resume沿Grok原session。
3. 审批拒绝不能借账户证据换渠道绕过；取消/活进程/未收束写入/协议异常等仍须对账，不能掩盖或绕开。账户证据只决定provider可用性，不授权任务内容，不提升读写/编译/文件权限、不重置复审额度。
4. Cursor额度恢复后新任务可回主渠道；已经转Grok的任务为上下文连续性仍沿原Grok会话，不自动乱切。限定本机/workspace现有使用方式，避免新增后台监控、后台重试或复杂服务；同一入口可显式携带有效证据即可，按用户授权自动调度不等于无人后台守护。
5. 固定模型/高推理/auto审批、只读工具约束、秘密脱敏、旧历史兼容、原状态/报告含义、同一作者和锁规则保持。禁止yolo/always-approve等绕过。

你是以下文件唯一作者，且仅允许修改：
- D:/alianximod/.grok/scripts/Invoke-CursorTask.ps1
- D:/alianximod/.grok/scripts/Test-CursorTask.ps1
- D:/alianximod/AGENTS.md
- D:/alianximod/.grok/skills/max-dispatch/SKILL.md
- D:/alianximod/docs/统一开发与术式批量制作流程.md
- D:/alianximod/docs/开发记录/Grok备用渠道-20261104/实现交付.md
不改业务src/资源/制作表/用户全局配置/主会话记录与已有回执，不创建新的模型渠道，不运行Gradle/打包/游戏。个人技能 C:/Users/baishang/.codex/skills/xueximod-grok-fallback/ 由另一聊天唯一作者制作，不得写它；让项目文档只引用该技能，合同唯一来源仍第11节。

必要离线作者自检：补新task有效证据直启Grok、TLS历史+有效独立证据切换、无效/过期/异账户证据拒绝、已知恢复失效、拒绝审批不可绕过、旧同task quota/旧task兼容、Grok普通Resume连续性、秘密与输入权限。完整既有Test-CursorTask通过后停写交回。离线fixture须明确模拟，不用真实Cursor碰quota，不消费真实Grok作作者测试；真实统一入口新路径烟测由主会话在你结束后做。不要追加独立正式复审。

交付在实现交付.md清晰给出最终实际参数与证据JSON最小例子、有效性/恢复/不同账户的处理、作者测试结果和未跑项。最终唯一<<<CURSOR_TASK_REPORT>>> JSON报告，至少workStatus/summary/changedFiles/checks/notRun/writesSettled/blocker/nextAction。此次原生resume是修正当前入口缺失的有限接入动作，完成后常规派发必须回统一入口；不伪装wrapper attempt。保持固定grok-4.7/high/auto。
