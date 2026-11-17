模型：Cursor CLI --model grok-4.7-high-fast（显式固定，不换模型）。阶段：实现，无编译授权。设计第1/2轮已定稿，实现正式复核尚0/3；本单为作者实现/必要定向自检，不另开独立测试或正式复审。必须完整读 AGENTS.md、.grok/skills/max-dispatch/SKILL.md、docs/统一开发与术式批量制作流程.md、docs/分身底座设计与实现指导.md，以及本批 implementation-contract.md。按两份合同实施，不自行扩大范围或重审旧批次。

用户授权基础分身底座，先设计再实现。真实开发闭环：两类驻留分身、主动/受击基础脉冲、有限诱敌、生命周期、配置消费者、权限2命令与普通入口过滤。新视觉只复用原版模型/贴图，不制作新美术。不开放任意现有链/完整水幻形/同步配合AI；不更改旧全局平衡算法/伤害规则，不用 FakePlayer，不挪动主人，不向旧 ChainScheduler/Tally 塞分身任务。

保留所有未提交修改；不得 reset/clean/checkout、提交、改表、打包、build/jar/reobf/runClient、启动游戏、跑 gradlew 或新增独立测试。git使用单次 -c safe.directory=D:/alianximod。不要派其他代理。派发期间主会话只收最终结果，不读实时内容。

严格单文件单作者。依赖其他作者时只看已冻结 implementation-contract.md，不读取其他作者进行中的候选/中间代码来猜接口。确有合同冲突或需要范围外文件，停止那一小部分，最终报告清楚交回，不越权修共享文件。常规实现选择按合同自主完成。

报告要求：本次改动的准确路径、真实完成能力、接口依赖、配置实际消费者、必要自检与未做项、哈希与最后写入时间，明确“待统一编译”“游戏未测”；最后停止写入并声明释放。不要把候选源当成已经接入运行源码。


你是唯一整合作者 C。当前阶段只制作候选源码/资源，不激活到正式 src/main，不编译。另一聊天已报告其源码收束、编译尚待结果；本方不能改变其实际编译输入。

写入授权：
1. 冻结 implementation-contract.md 第2节所有 C 归属的新文件和既有文件，其“候选副本”写入 D:/alianximod/docs/开发记录/分身底座-20261102/候选源码/ 下，后缀完整保留 src/main/java/... 或 src/main/resources/...。
2. 本批 before-C/ 下保存 C 所有既有文件的原始字节；author-C-integration-report.md、author-C-integration-evidence.json、integration-apply-manifest.json。
3. 不写正式 D:/alianximod/src/main，不写A的core或B的action/CloneBalance候选，不改设计和冻结合同，不写工作簿。

先按冻结合同把实际路径列出：所有既有C文件先读当前真实文件、保留原始字节/sha256/mtime作为before-C基线，再复制到候选位置只改自己的候选副本。不存在的语言文件不为凑清单创造无用副本。新增候选文件若已存在且不属于你先停下报告，不能覆盖他人。当前core/action作者按同一合同并行，只依据合同写引用，不读取它们未完成的中间代码。

必须完整实现：
- TechniqueAvailability / TechniqueAccess，旧构造与旧术式默认PLAYER保持兼容、元数据不可被NBT覆盖；TechniqueManager.getAll保留全量，玩家可用目录统一筛选。
- 第9节所有获取/学习/自动学习/菜单/刻印/书/术式石/剑技与法杖装配/StaffSkills入口；旧保存/库存不破坏性清理，未知旧数据处理保持原义。SkillUsePacket顶层整链收费前拒绝，必须覆盖批量、纯附魔、多重和普通路径，并清掉被拒开发项的按住重试，不能静默丢段继续剩余链。只增加能力门，不顺手修旧学习规则。
- 三个真实Technique + CloneReleaseDefinition/CloneTechniqueRegistry，explicitManaCost与常量来自B CloneBalance，普通ACTION而非SELF_STATUS。ReleaseDefinitions挂接、ModTechniques登记与declareSpecialBalances调用、BalanceCatalog仅新增pulse宿主白名单。不要空effect假成功，不做按ID旧链runner特判。
- CloneEntityTypes独立DeferredRegister、属性、Forge实体跟踪、XueXiMod服务端和客户端隔离注册，客户端CloneRenderer只用原版人形/原版贴图，不自制美术不下载皮肤，不在dedicated server引用Minecraft客户端类。
- /shu clone spawn phantom|normal、list、respond UUID、dismiss UUID|all，在线玩家本人、权限2，使用A服务和真实结果状态；请求成功不能提前报已伤害。UUID列表/拒绝提示可读，可按合同返回跳转或copy建议但不得新增宽泛控制他人权限。
- 性能枚举只追加固定常量，不改旧序号/统计口径/CSV，不修改PerformanceRecorder；计数由A调用，C不重复统计。
- 仅新增实际用到的语言键，保留原字节尽可能最小diff。

对源代码依赖只能只读定位，不重审/扩修旧批次。需要新路径挂接时先最终报告路径原因，不能越权。合同中给出的A/B接口是冻结依赖；如果无法无损接入，记录精确签名冲突，别用反射/临时伪实现绕过。

交付必须包含待接入清单：每个候选的目标路径、候选sha、原始目标sha（新文件标missing）、是否new/modify；以后真实应用时必须先重核原目标未变，变了只定向合并本次增量，不能整文件覆盖。当前不要执行应用。报告所有完成/未完成点和停止写入，明确“候选未接入、待统一编译、游戏未测”。命名门禁扫描真实src的部分留统一接入后执行，不为门禁提前写正式src。无编译授权。
