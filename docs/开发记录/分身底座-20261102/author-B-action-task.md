模型：Cursor CLI --model grok-4.7-high-fast（显式固定，不换模型）。阶段：实现，无编译授权。设计第1/2轮已定稿，实现正式复核尚0/3；本单为作者实现/必要定向自检，不另开独立测试或正式复审。必须完整读 AGENTS.md、.grok/skills/max-dispatch/SKILL.md、docs/统一开发与术式批量制作流程.md、docs/分身底座设计与实现指导.md，以及本批 implementation-contract.md。按两份合同实施，不自行扩大范围或重审旧批次。

用户授权基础分身底座，先设计再实现。真实开发闭环：两类驻留分身、主动/受击基础脉冲、有限诱敌、生命周期、配置消费者、权限2命令与普通入口过滤。新视觉只复用原版模型/贴图，不制作新美术。不开放任意现有链/完整水幻形/同步配合AI；不更改旧全局平衡算法/伤害规则，不用 FakePlayer，不挪动主人，不向旧 ChainScheduler/Tally 塞分身任务。

保留所有未提交修改；不得 reset/clean/checkout、提交、改表、打包、build/jar/reobf/runClient、启动游戏、跑 gradlew 或新增独立测试。git使用单次 -c safe.directory=D:/alianximod。不要派其他代理。派发期间主会话只收最终结果，不读实时内容。

严格单文件单作者。依赖其他作者时只看已冻结 implementation-contract.md，不读取其他作者进行中的候选/中间代码来猜接口。确有合同冲突或需要范围外文件，停止那一小部分，最终报告清楚交回，不越权修共享文件。常规实现选择按合同自主完成。

报告要求：本次改动的准确路径、真实完成能力、接口依赖、配置实际消费者、必要自检与未做项、哈希与最后写入时间，明确“待统一编译”“游戏未测”；最后停止写入并声明释放。不要把候选源当成已经接入运行源码。

你是响应与配置作者 B。本单只写候选源码：
D:/alianximod/docs/开发记录/分身底座-20261102/候选源码/src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/action/**
D:/alianximod/docs/开发记录/分身底座-20261102/候选源码/src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/CloneBalance.java
及本批 author-B-action-report.md / author-B-action-evidence.json。真实src/main、A的core目录、C的注册/命令等均不可写。候选包名采用正式生产包。

按设计与冻结合同完整实现Action接口/准入/PreparedAction/脉冲和CloneBalance的规格声明、原始默认及服务端读数。接口都有实际消费者。
- 仅actor原点的瞬时单目标射线，手动冻结方向和受击UUID执行时解算，阻挡与范围取同一快照，实时权限/友方/owner宠物/任意CloneEntity过滤；用LivingCandidateQueries，不能用主人几何或其锁定缓存。
- 先DamagePermission及附加友方判断，再classifyRespecting(..., TideMarkSuccess.NONE)。不classifyRoot，不Tally、不产火种潮印、不武器附魔，ConfiguredDamage只形成一次，0是可提交响应。原版damage source仍owner的限制保留。
- prepare无伤害/扣费/额度副作用，具体收据与COMMITTING边界由A服务负责，不能在B再收费/退款/冷却；commit动作最多一次。
- 专项键只声明真实消费，标准伤害/魔力/冷却不重复声明；魔力和冷却专用API，显式服务端侧，默认值单源。C将调用你的declareSpecials并把pulse登记进内置伤害名单，你不改ModTechniques/BalanceCatalog。
- A与C尚在按同合同实施，不读取其正在写的中间文件，不自己补它们的类。接口问题明确交回。
本单无编译授权。命名门禁若只扫描真实src，留整合后统一执行并如实注明。
