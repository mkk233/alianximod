# A：绑定结构、共享自动释放与周期预算

固定 Cursor CLI --model grok-4.7-high-fast；这是新的可写实现会话，不恢复原ask会话。只读完本目录 implementation-contract.md（最高执行契约）、design-decision-1.md、记录.md、read-chain-final.md、read-entry-facts-final.md。设计2/2已用完，禁止新设计审查；实现0/3，所有作者收束后统一审，不自行再审旧功能。

完整实现正式契约中A职责，不止接口或占位：切步前捕获/投影/原始下标，冻结子链，所有开域路径传计划，正常起手账单排除被捕获段，共享报价/预算/执行核心，独立自动轮与双重A/B延迟/取消退款，显式上下文栈，按实际生成时检查装备资格。新功能规则不能按ID特判。无绑定手动行为保持。双重捕获第二攻击，未捕获尾部保留后续按键步。自动入口不能抢按键槽冷却、不能查玩家准星/锁定标记，不调用伪按键包或裸spawn代替链执行。

独占源码范围：D:/alianximod/src/main/java/com/bslx/XueXiMod/network/ 下 ChainSteps、SkillUsePacket、SkillChainPlan、SkillSegmentMath、CompiledReleaseStep、CompiledCastPlan、CastPlanContext、CastExecutionSession、SkillSegmentRunner、TechniqueChain（用rg确认唯一真实路径）；按契约新增network的AutomaticRound/GroupBudget/结构编译器等必要共享执行类型；整个新 shu/technique/release/bind/ 包归你。ReleaseBatch.java已从外部作者释放，按需归你，必须先读取其最新版本并保留候选查询优化。未列文件只读，确有必须跨作者接点先返回具体需求，不改别人文件。

B在并行实现CastFrame.execution/launchAim、LaunchAim与HitSpec/实体；C在并行实现DomainService新重载/实例运行。按implementation-contract.md调用，不能另造占位或抢写。其源码可能尚未完成，不能自行修别人文件。你负责SkillUsePacket.register阶段安装编译/执行桥，首个按键前可用。共享预算从原GroupBudget提炼，保留原手动路径行为和费用。三处开域入口保持原释放索引与同次声明去重。

必须完整处理：A/B各自附魔、聚能边界、一分为三Tally、整轮预留不足静默跳过、成员失败只退未提交费用、异常清算一次、RoundHandle.done真实完成及cancel幂等、每个真实生成前独立检查到期（调度器先后不确定）。未来正蓄力需传相同token，当前无正蓄力攻击不新增玩法。费用仍用原收据对象，不回读LAST_RECEIPT、不改MagicPool/ManaPaymentReceipt。

实施前保存拟改文件原始内容到本目录backup-A/相对路径，保留所有现有未提交改动。不要reset/clean/checkout。若Shell因CLI允许列表被Rejected，禁止换壳/工具绕过执行同一拒绝命令；使用正常获准的原生文件读写完成源码和内容备份，不能伪造hash/mtime或命名门结果。无法保证保留原内容时停该文件返回。允许记录协调文档 implementation-A-final.md、evidence-A.json；如果无法取得hash如实填null。

禁止编译、Gradle、测试、打包、游戏、提交、制作表、改权限设置或模型。Shell允许时可运行已有命名门，拒绝则记未跑。完成所有写入再返回最终报告并released=true：源码文件/新增接口、捕获例子与开域费用隔离、实际复用路径、延迟预算和准星替代事实、未完成/阻塞、未验证项。写明待统一编译，不宣称整个功能已验证。不要读取其他作者实时日志或轮询其进度，按契约完成自己范围后返回。
