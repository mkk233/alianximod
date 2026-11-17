# B：新火种资源API与六个附魔实现第一阶段

固定 Cursor CLI --model grok-4.7-high-fast。阶段实现，非正式复审。设计2/2已耗尽，实施按已批准合同不再重做设计；实现0/3后续集中。用户已授权32槽并采用其余首版参数，二维v1暂定本次采用版。

读取AGENTS、max-dispatch、统一流程，以及本批 intake.json、approved-v1.md、implementation-contract.md、source-intake-report.md和facts-followup-report.md。保持旧六式公式和旧点火/光束/火球原有基数/倍率；本批6个新附魔是留焰、种火、引燃、引爆、幽冥、崩地。

源码根 D:/alianximod/src/main/java/com/bslx/XueXiMod/，你仅独占以下类名对应实际路径及这些职责所需新类：EnchantmentDefinitions、EnchantBonus、新的附魔载荷/定义/效果类；FireCastContext、FireSeedAccount、FireSeedState、FireSeedGrant、FireSeedProduction、FireSeedMath、FireLimits、BurnLedger、FireConsumption；FurnaceService/FurnaceMath仅必要只读接缝不得改倍率；FireRuntimeEvents、火伤帧去重接缝。先写author-b-ownership.json明确绝对路径，不把整个release包视为授权。

不得写A的CastFrame/ReleaseBatch/TechniqueHitSpec/Entity/Settle/TechniqueHits、链/注册/法杖/调度/费用文件，尤其ModTechniques/Technique/ReleaseDefinitions都归A。不得写C的DomainService/DomainInstance/ContactEnchantState/ModBlocks或宿主/地形/渲染。公共接口尚由A实现，你先基于现有稳定类型完成自己的纯资源/载荷API，避免读取A中间源码或猜其将来的签名；A交接后再做调用适配。跨界需求只交清单，不抢写。

实现目标：
- 明确类型载荷区分收费2与伤害0；六个新附魔不误继承旧fire固定2点火种/原版着火/直接附加伤害。保留既有五元素附魔行为。
- 新命中产种合并事务：本体新基数与种火10先合计、乘seedMultiplier取整一次，旧固定贡献保持语义，最后入同一余额上限100；根castId每目标一次且满额也占机会。不得各自newContext重复入账，也不丢掉2+10任一份。
- 新引燃按照批准段参数、冻结F、+10tick首跳、8层、到期末跳、公平共享分配与余数轮换、燃料归零终止、同castId+target+effectKey永不重复挂层；旧效果公式与原分配策略不顺手改动。
- 新引爆0.2*S*(1+F)仅消费当前余额，独立去重；产种/引燃/引爆不使用跨效果消费后禁产种总锁。共享余额不是私有燃料。显式炉心已结算标记避免通用火伤重复F；F用fireBonus，产种用seedMultiplier。
- 新附魔独立装配可挂到下一次合法普通武器攻击，收费一次且失败不伪造成功；实现需要A公共接缝时提供注册/调用清单，不凭空复制武器账本。
- 幽冥/留焰/崩地先交载荷与宿主能力合同，C负责领域周期/轨迹判定/地形实际执行；不重复实现第二套语义。幽冥窗口产种仲裁API可归B，Domain接线C做。

不改xlsx/资源库/本批记录.md，不改src资源或语言，不运行Gradle/javac/测试/build/jar/reobf/runClient，不游戏。保留所有脏改动，不reset/clean/checkout。必要的实现自查不宣称独立复审通过。

只允许写本批author-b-report.md/json、author-b-interface.md、author-b-ownership.json。交付稳定可调用签名、既有入口兼容说明、A/C需要接线的项，准确区分资源实现与待宿主集成。报告修改hash/mtime、检查和待统一编译，明确停止写入。
