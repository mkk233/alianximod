# A：公共链、命中接口、32格法杖实现

阶段：已批准设计的代码实现。固定 Cursor CLI --model grok-4.7-high-fast。你为A唯一作者，当前设计2/2已用完，不再开设计复审；实现0/3，后续相关实现齐备后集中复核。本任务不是独立复审。

先读AGENTS.md、max-dispatch和统一流程；完整输入为本目录intake.json的13项N/O/DV、approved-v1.md、implementation-contract.md、facts-followup-report.md。用户明确让一把法杖一次装32个不同术式，并已临时采用当前二维v1用于本次接入。历史12槽/新增另一把法杖提案撤回；不能重置轮次。保留全部未提交改动，禁止reset/clean/checkout。

授权源码根 D:/alianximod/src/main/java/com/bslx/XueXiMod/。允许你修改该根内以下准确类名对应的文件及其直接职责内必需新类：ModItems、StaffData、StaffSkills、StaffItem、法杖装配相关Menu/Screen/Packet（含SpellAssemblyMenu及真正Screen、SkillAssemblyScreen/SkillAssemblyPacket）；ChainRole、Trajectory、ReleaseDefinition、ReleaseRegistry、ReleaseDefinitions、TechniqueChain、SkillSegmentMath、CompiledReleaseStep、CastExecutionSession、SkillUsePacket、计划缓存/编译/预算/调度；CastFrame、ReleaseBatch、TechniqueHitSpec、TechniqueHitEntity、TechniqueHitSettle、TechniqueHits、碰撞结果/结算作用域、TrajectorySampler、JavaReference；ModTechniques、Technique及最终注册接缝。现有文件路径按类定位，先把绝对路径与新增目录写 author-a-ownership.json；不要按整个包无边界修改。

你禁止修改 B 所有文件：EnchantmentDefinitions、EnchantBonus、新附魔具体实现、FireCastContext/FireSeedAccount/FireSeedState/FireSeedGrant/FireSeedProduction/FireSeedMath/FireLimits/BurnLedger/FireConsumption/FurnaceService/FurnaceMath/FireRuntimeEvents及同职责资源文件。禁止修改 C 所有文件：DomainService、DomainInstance、ContactEnchantState、ModBlocks、独立陨岩/热波/火苗/留焰/幽冥/地形/残骸实现、独立渲染类。发现跨界需求列handoff，不自己抢写。B仅并行准备资源API，C尚未派发。

本阶段完成：
1. 现有basic_staff 6→32，8列×4行清晰界面，32索引全链路保存/装配/同步/服务端合法校验，旧NBT保护，32上限；剑四条8格与缓存64不改，魔力加成等不改。不是创建第二把法杖，不把32拆四条链。
2. 合同公共链能力：地表锚点、新抛物线/喷涌、错时已有副本、明确单层父子接续编译、分支附魔隔离、根施法ID共享、复制/时序分离、完整运动冲突拒绝、<=64实际实例、费用预约与取消退款。仅新能力计划用新报价策略，旧链行为保持。
3. 命中公共接口：扫掠最早合法实体/地形事件，取消/免疫成立/确认伤害/账本等待区分；Tally一次完成回调，先父结算后子段，固定事件点与中心排除/遮挡快照，父副本与子默认数量分离；暴露共享HitClock以支持本体/火径去重。保持旧绕无敌帧路径，不给新效果加额外绕过。
4. 给B/C稳定最小生命周期/载荷/宿主接口。优先现有类扩展，不另造施法引擎；具体命名是实现常规选择，遵守合同职责。不要引用尚未交付的B/C新类使本阶段源码悬空；可用通用接口/回调由后续注册绑定。不生成会悄悄成功但无功能的空实现。13术式注册可待B/C类交付后再最终集成，本轮先把必要公共能力落实。

32UI必要语言键允许你独占 D:/alianximod/src/main/resources/assets/xueximod/lang/zh_cn.json 与en_us.json；不得改其它素材/模型/纹理，本轮新美术由C依据归档清单接入。不要写xlsx、资源库README或本批记录.md。

输出允许写本批 author-a-report.md/json、author-a-interface.md、author-a-ownership.json，准确列已实现和留给C/B集成项，不把接口准备说成13项已完整实现。handoff包含可调用签名、根ID/分支/命中/费用生命周期和需后续注册项目。对照批准行为做必要实现自查，不启动独立正式复审或测试阶段，不默认写测试，不运行Gradle/javac/build/jar/reobf/runClient或游戏。交付写待统一编译。

完成后所有写入停止，给文件清单、hash/mtime、必要检查、残留阻塞、停写声明。主会话只等最终报告，不实时读你的中间工作。
