# fallback-adapter：集中实现第1/3轮后的定向返修

同 batch workflow-grok-fallback-20261104、原 task fallback-adapter、原 Grok session48e4111a-a131-46c2-863a-5a521d55d120。设计2/2已耗尽；集中实现第1/3轮已完成，有问题须返修，不能重置计数。本单是作者修复和必要自检，不是再次正式复审。

先读本目录 实现复核第1轮.md。其中第1–5项由你唯一修复，逐项核对触发条件，若建议与实际代码不符用具体事实说明，不能盲改。第6项个人技能由另一聊天原作者修，禁止写其目录。

还需处理本次真实统一入口暴露的接入缺陷，证据来自 .grok/runs/workflow-grok-fallback-20261104/fallback-readonly-review/attempts/a202611040128229664006：
- 真实Grok exit0/end_turn、最终报告完整，但wrapper误报tool_contract。stderr中实际是：skipped permission entry path=D:\alianximod\.claude\settings.local.json rule=permissions.allow: Skill(deep-research) reason=unknown tool prefix: Skill。这是外部兼容配置里跳过未知Skill权限项，不是本次--tools白名单含未知工具。需要精确识别真实--tools工具契约失败，不能用泛化unknown tool正则误伤无关警告，也不能把真的无效白名单放行。补对应离线正/负例，旧失败回执保留。
- 同一stderr有：reasoning_effort: model does not support effort; ignoring it ... model=grok-4.7-build-fast effort=high；以及 warning: 老猪的中转站 has no effort levels; ignoring --effort/--reasoning-effort high。此前只能证明传入high，不能证明生效。必须修正能力声明/校验，不能继续把ignored high标成已生效。

当前本机C:/Users/baishang/.grok/config.toml的[model."grok-4.7"]非秘密字段只有model=grok-4.7-build-fast、name=老猪的中转站、api_backend=responses，无reasoning_efforts或supports_reasoning_effort。主会话已用进程临时GROK_CONFIG={"model":{"grok-4.7":{"supports_reasoning_effort":true}}}做一次只回复字符串的真实探针，仍提示忽略high（本目录effort-overlay-probe.*），没有改全局文件。不要再重复该无效覆盖尝试。

先解决high实际请求：查本机CLI文档11-custom-models.md和26-config-reference.md对应模型推理能力配置。作为用户明确要求固定Grok4.7和现行High流程的必要配置修正，本轮仅额外允许在C:/Users/baishang/.grok/config.toml的[model."grok-4.7"]小节增补正确的推理档位声明；不改模型ID、别名、地址、凭据、默认模型、权限或任何其他小节。优先用确实受支持的进程级配置避免持久修改；已证伪的overlay不用再试。如果只能改用户模型能力元数据，保留原文件全部其余字节，不复制整份带凭据配置到仓库/回执/备份，不输出秘密。改动前后只记录非秘密能力字段与全文件SHA。这个窄配置修改不是允许其他全局设置变更。Grok入口固定grok-4.7/high/auto，严禁always-approve/yolo/改信任。

配置修正后允许一次最小真实CLI能力探针（不调用真实Cursor，Grok不使用工具，只回复标记）；证明不再出现忽略high的警告且同wire模型请求成功。若仍无法使CLI接受high，停在明确阻塞，不降档、不伪称已生效。新wrapper若检测到high被忽略/unsupported，应记录reasoning合同错误，不声称High；区分requested与实际可证实程度，不假装可证明中转站内部实际推理强度。MCP deny限制调用不是禁止CLI后台初始化，文档勿误称未启动MCP；若已有受支持的安全禁初始化选项可用于本进程则优先采用，不修改用户全局MCP。

允许写路径：原来六份Invoke-CursorTask.ps1、Test-CursorTask.ps1、AGENTS.md、max-dispatch/SKILL.md、统一流程第11节、本目录实现交付.md；加上上述单一全局模型小节的能力元数据，以及本目录下你新建的“high能力修正证据/”用于不含秘密的探针元数据/最终输出。主会话记录、既有回执/任务输入/失败记录不可改；业务src/资源/制作表不可改。无Gradle/游戏/打包/新模型/子代理/正式复审。

此轮原生resume限于统一入口自身确有tool_contract误分类及恢复缺陷的修复，真实留档，不伪装wrapper task/session。修复完成后普通工作必须回唯一入口。你的原生session不会被假造成已有wrapper session。

必要测试：审查1–5的每种可触发条件；真实stderr兼容Skill警告不误判/真正无效--tools仍fail closed；high忽略警告不当生效；已有全部Test-CursorTask无回归。修复完成停写返回唯一CURSOR_TASK_REPORT和清晰交付报告，列每项处理、实际high验证、剩余限制。不得改旧历史报告为成功。下一次正式复核由主会话等两个作者停写后集中第2/3轮。
