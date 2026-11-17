# D：从已停写C移交的领域幽冥实现

执行固定Cursor CLI --model grok-4.7-high-fast，WriteMode；只实现本单，不能降档、不能spawn代理。主会话已确认A公共接口与B资源、C第一段全部最终停写；C续做独立攻击载体，不修改本单领域文件。此为已批准实现分工调整，不改变设计。设计2/2耗尽，实现正式复审0/3。本单不另开复审。

读取D:/alianximod/AGENTS.md和.grok/skills/max-dispatch/SKILL.md，本批D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/下intake.json完整N/O/DV、approved-v1.md、implementation-contract.md、author-a-bind-report.md、author-a-interface-v2.md、author-a-ownership.json、author-b-report/interface/ownership、author-c-stage1-report/interface/ownership、resource-manifest-v1.json/md。最新授权：basic_staff6保留，独立test_staff32；二维v1用户明确本次采用。你不改法杖，不以旧design-round2中的基础32为现行要求。

唯一所有权：D:/alianximod/src/main/java/com/bslx/XueXiMod/ 下现有DomainService、DomainInstance、ContactEnchantState、领域专用事件，以及本轮新NetherDomain前缀的领域周期/同步状态/纯视觉色调服务与其独立客户端实现。原C在第一段尚未改这些文件，现已明确移交给你；先写author-d-ownership.json列实际绝对路径并核对A/B/C所有权，无重叠后再改。现有火系客户端渲染中的纯色调接入口可由你小范围修改，必须列实际路径且不与A/C已有文件重叠；旧六式机制、几何、公式、贴图不改。C新载体渲染类正在写入，禁止读取或改它；最终给颜色查询签名，由C/A后续接。

禁止改A的通用命中/链/费用/法杖/ModTechniques/ReleaseDefinitions/全局注册/lang；禁止改B的六附魔语义与火种账本；禁止改C地形/陨岩残骸/ModBlocks/新载体与新渲染器。你可以只读已收束的A/B源码使用公开API，不另做正式审查。公共ModNetwork/模组入口/客户端总线或注册若需接线，交author-d-registration.md给A，不抢写。需要缺失A/B接口时交回具体签名与调用点，不用占位假成功。

按完整需求实现fire_nether_gloom幽冥：
1. 六新附魔载荷B已实现。幽冥只附在已明确可支持的现有领域（含灼地），不自造独立领域，不把零接触伤领域排除；承接领域原范围与时长。载荷来自真实释放帧/当前EnchantBonus.resourceLoad，根FireCastContext/castId和主人归属正确。
2. 每满20tick首笔基础5×主人当前产种倍率；用B NetherSeedArbiter/NetherOffer的公开API，对每目标全局20tick窗口从当时全部覆盖有效领域中取最高请求一次，不能先各领域单独入账再择高。所有覆盖域在仲裁时一起采样，稳定域id打破同值。用真实root/domain/window/target生命周期，不每周期伪造新的真实施法；存活燃烧能消费新余额，不能刷新已结束燃烧。
3. 无额外伤害/减速；不创建原版魂火/灵魂沙。领域内兼容火色调变蓝，离开恢复，保持伤害属性、火种归属和旧公式。客户端按领域同步或实体追踪收到真实领域范围/标志来判定颜色，不能按本地玩家近远猜全世界幽冥状态。远端多人/跨维度/领域过期清理都要有明确生命周期。
4. 用确认版manifest幽冥透明64x1024/16帧图，复制至声明运行路径、实际UV分帧并引用，不拉伸整条。只允许写本批新幽冥纹理与独立新材质定义所需文件，不覆盖旧六式资产。
5. 给C新火苗/热波/留焰/陨岩效果渲染公开最小查询接口（例如按level+worldPosition返回色调/是否幽冥，具体真实签名以你实现为准），不要猜C类名、不读取中间源码。你负责旧既有火系表现的兼容纯色调通道，需改公共入口的清单交A。对不支持的视觉通道如有真实限制要列出，不能只复制幽冥贴图就宣称全接入。
6. RootCastBindings retain/release按A v2合同，领域结束/卸载/维度清理无无限静态root残留，费用结算不负责销毁还活着的领域上下文。避免重复监听每次初始化又注册。若原领域保存策略不持久，不擅自改变全领域存档约定；描述真实边界。

先前合同已裁决这些行为，不再提新设计，不运行正式复审。必要实现定向自查可做，命名门禁只修自己新文件。**不Gradle/javac/测试/build/jar/reobf/runClient/游戏/发布，不写制作表/记录.md/资源库README，不提交。**

最终输出author-d-report.md/json、author-d-interface.md、author-d-registration.md、author-d-ownership.json：实际代码路径+hash/mtime、运行资源/hash、A统一接线清单与C颜色接口、必要自查与未跑编译/游戏、具体未完成项如有、所有写入停止。最终完整反馈而不是流式跟踪。
