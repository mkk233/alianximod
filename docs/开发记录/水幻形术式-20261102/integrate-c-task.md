# 水幻形v2.1确认后的最终接入
用户最新：“这个版本好接入吧”。这是承接上一轮明确的v2.1散水预览确认，已批准采用并接入，不再要求常规开工或视觉重复确认。
固定Cursor CLI `--model grok-4.7-high-fast`，不降档、不spawn。设计2/2已用完，不新开设计复审；实现当前2/3，待本轮所有写入收束后集中第3轮。你本次实施，不自行开独立正式审查。不改无关系统，不重做已裁决机制。已有未提交修改全部保留，禁止reset/clean/checkout覆盖。
完整读AGENTS、max-dispatch、现行开发流程及配置指南，本批用户决定、implementation-contract、design-round-2-carrier、详细设计。阅读相关implementation报告时注意它们是初次记录，**以当前修复源码以及fix-r1-a/c/d、review-nonvisual-r2、fix-r2-d、fix-compile-a/c和compile-nonvisual-report为最新状态**，不要恢复旧bug。非视觉代码已compileJava通过，当前没有源码作者仍在写。
不运行Gradle、build/jar/reobf、runClient或游戏，不改制作表，不联系其它聊天。默认最终统一编译由另一个指定Cursor在你交回后执行。可运行命名门禁，仅修自己修改的文件。最终一次性报告准确路径/行为/配置与运行资源映射/未跑项/真实阻塞/停止写入；只等最终反馈。

## C作者：正式术式登记、配置声明与玩家说明
唯一源码写入：shu/ModTechniques.java、shu/technique/release/ReleaseDefinitions.java及必要同包登记文件、clone/water/WaterPhantomDefinition.java与WaterPhantomBalance.java，以及 assets/xueximod/lang/ 下必要语言文件。如现有正式目录/可学/收纳索引在明确另外文件，只能登记本术式必要接线并在报告列出，不扩改UI或旧术式。你不改D的client/网络/CloneEntity/纹理/模型文件，不并写资源库README。报告本批 integrate-c-report.md；用户操作文档本批 player-usage.md 归你。
目标：现在真正启用 water_phantom，保持MAGIC/WATER、PLAYER、ACTION/ACTION、TRIGGER、默认伤害0、冷却20、显式mana10、level1/complexity1，按已冻结定义登记捕获能力。使用现有通用登记路径，不写新的按ID施法分支，不把旧clone_phantom/body/pulse改PLAYER。
先登记TechniqueManager的真实术式，再登记WaterPhantomDefinition，使explicitManaCost=10真实可读；专项声明lifetimeTicks默认0与decoyRadius默认8，必须在BalanceRegistry.seal之前真实调用。不要把宿主加入DAMAGE_HOSTS、不要复制攻击伤害倍率、不重写世界已有配置。缺配置回默认，已声明键被既有dispatcher真实消费。静态初始化顺序及adapter注册先后要可运行，不能只注册id但能力/报价未就绪。
玩家可通过既有学习/术式目录/装配取得本PLAYER术式。显示中文“水幻形”和准确简述：法杖槽0装“水幻形→断潮”，普通释放留置、潜行释放立即响应；默认部署魔力10+断潮12一次，合计冷却20+20；其它未适配载荷收费前拒绝。不要夸大为全部水系支持或任意复杂链。已有两格捕获门、原胚/载体限制、冷却/失败退款等按当前实现保持，不重构。
D在独立实现确认版可见表现，期间不得编译或把这半边注册当作已交付。报告确认正式登记/释放定义/专项声明/客户端可见文案的具体调用位置，player-usage.md用实际游戏入口，不编造测试命令。

