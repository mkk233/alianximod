# 公共执行约束与接口归属

实现模型只能 Cursor CLI --model grok-4.7-high-fast。当前阶段实现，设计正式额度沿用2/2，不启动新设计复审，不spawn子代理。实现阶段复审尚未开始。必要定位和作者自检不另开正式复审。用户明确授权底层文档+底层代码，不做术式。

权威规格 D:/alianximod/docs/火属性二级玩法设计.md。既有未提交修改全部保留，不reset/clean/checkout、不提交。不得改ModTechniques、ReleaseDefinitions、表格、技能书、具体术式、AGENTS、流程、构建配置。不得运行Gradle、打包或启动游戏。本批三作者并行，等主会话通知收束后才由指定会话编译。不要读取其它任务实时日志/中间文件来审查。只为编写本模块可查接口；最终完整交回。

固定包/API契约（可增加重载，不能默默改下面公共接口）：

资源作者A，包 com.bslx.XueXiMod.status.fire.seed：
- FireSeedState.value(LivingEntity): int，资源注册名 xueximod:fire_seed。
- FireCastContext(UUID castId, UUID casterId)：每次逻辑施法一个共享账本对象，复制体/延迟回调用同对象；允许casterId为空表示无施法者。提供castId()/casterId()。只持有UUID账本，不长存实体引用/全局永久map；上下文随施法释放回收。new上下文不能在同逻辑施法中重造。持久燃烧保存已扣快照，恢复时不重新消费。
- FireSeedSnapshot(UUID castId, UUID casterId, UUID targetId, int amount, int tiers, int remainder, double furnaceBonus)：不可变，严格校验。
- FireSeedState.preview(LivingEntity target, FireCastContext cast, int requested, double furnaceBonus): Optional<FireSeedSnapshot>。requested=Integer.MAX_VALUE表示全部，其他非负按min余额；不写数据或账本。
- FireSeedState.consume(LivingEntity target, FireCastContext cast, int requested, double furnaceBonus): Optional<FireSeedSnapshot>，只成功且amount>0时标记消费；防重复。
- FireSeedState.grant(LivingEntity target, FireCastContext cast, int baseAmount, double productionMultiplier): int，返回实际增加；先消费后生产被拒绝；0不变1、取floor，含上限去重。
- FireSeedMath提供纯运算clamp/tier/whole-tier消费与原BURST/SUSTAIN/SPREAD收益，可另起枚举。全部事务只服务端主线程，预览/扣款受同线程约束。
- A负责实体死亡清理与玩家死亡克隆清理资源；存活跨维/clone保留有需要明确处理，不覆盖其它持久键。

炉心作者B，包 com.bslx.XueXiMod.status.fire.furnace：
- FurnaceService.fireBonus(LivingEntity): double，无状态0。
- FurnaceService.seedMultiplier(LivingEntity): double，无状态1。
- FurnaceService.active(LivingEntity): boolean，仅玩家。
- enterNormal/enterOverdrive/endPassive/cool：具体返回结果类由B决定并最终报告；cool成功调用C的SteamBurst.release(Player)。B不实现蒸汽伤害。
- 自己独立事件订阅管理爬坡、属性/治疗、NBT、牛奶、生命周期、抗性提升定向mixin；B不处理元素水冰易伤和对外火伤，它们由C在伤害事件中调用B的query。
- 增加 FurnaceService.waterIceMultiplier(LivingEntity): double，无状态1、普通1.4、过载1.7，提供C。
- B独占ModMobEffects.java、xueximod.mixins.json、zh_cn.json/en_us.json（只增炉心状态语言），及新增炉心mixin文件。

火伤作者C，包 com.bslx.XueXiMod.shu.technique.release.fire：
- FireDamage统一专用火伤与作用域；SteamBurst.release(Player)固定2、半径3、友敌过滤、正常防御。
- 通过FurnaceService的query在事件接入明确元素来源；已结算炉心的专用伤害作用域防二次加成。不能从术式id/伤害量/普通playerAttack猜火水冰。当前旧攻击无可靠元素标识的部分不改写术式，通过显式元素上下文供后续接入，并在报告/文档明确此限制。
- 消费燃烧/引爆服务使用A的preview→构造验证→consume→挂载，主线程顺序原子。冲突预检，按目标保存计划NBT，恢复不重复消费。接受参数/不可变计划，不能注册具体术式或写死三式id。
- C独占新增 damage_type JSON、新增对应的 damage_type 标签文件与新增火伤事件。不得动水牢原有bypass tags，不清改invulnerableTime；使用Minecraft/Forge的定向绕过hurt cooldown机制（查本地源码证据），不绕过armor/effects/resistance/enchantments。
- 不对普通playerAttack全面改元素；如已有可靠元素上下文可以安全接入，在最终报告给证据。缺少可靠标识以显式新接口满足底层，不偷偷重写技能算法。

三个作者只写各自目录和上述既有文件，对应src/test/java同包归属。新增其它必要文件先在自己包内解决，跨所有权则只最终报告建议交整合者。不改公共规格文档；发现不可实现要求/接口矛盾立即报告实质阻塞。

测试采用项目main Check风格，写有意义的状态机/公式/消费计划检查和必要源码结构检查。作者阶段不运行任何Gradle（所有source writes需先收束）。可以用轻量独立javac只验证自己的纯Java模块，但勿据此声称Forge集成通过。最终报告改动文件、API、未满足条款、未测点，不做全仓审查。
