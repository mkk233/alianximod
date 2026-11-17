# 水幻形术式设计与实现指导

当前结果（2026-11-02，只整理已交回的报告，不是新的复审）：用户已用「这个版本好接入吧」批准 Steve 水线 v2.1 并授权接入。正式术式、专项声明和确认版水形资源已经接线。集成复核第 3/3 轮结论为通过。随后的 `compileJava` 退出码 0，编译前后 `src` 哈希无差异。未打包，未进游戏。状态是实现交付、待用户实测，不是已经游戏验收。玩家说明、资源映射和实测清单见 `docs/开发记录/水幻形术式-20261102/交付与实测.md`。

名称对照（2026-11-03，仅改名，不是新的复审）：正文里称为「断潮」的水幻形载荷是 `tide_sever`，现显示名薄水切，仍可装配和释放。玩家现在看见的「断潮」是 `water_yielding_ribbon`，尚未适配水幻形。下文设计正文不改。

下面正文是冻结的详细设计，不另改玩法。文中「v2 尚未确认」「先不要注册」「水形尚未接入」是确认前的阶段门，已被这次批准接入替代。设计额度 2/2，实现复核 3/3；该轮之后没有返修报告，额度已用完，没有追加复审。

视觉更正（历史指令，仍然有效）：v1 圆润人形已否决；改在真实 Blockbench 中直接绘制标准 Steve 六方块模型的透明水线材质。过程目录为 `docs/资源库/过程归档/水幻形_Steve水线_v2-20261102/`。用户明确允许跳过二维先行；下面关于 v1、96×128/32 帧和必须先 Aseprite 的描述仅为历史输入，不得作为实现尺寸、轮廓或流程约束。v2 造型和材质已认可；v2.1 只改散水观感，并已按用户原话采用。逻辑设计不因此改写。

批次：水幻形术式-20261102。版本：v1＋载体门第 2 轮补充，视觉采用 v2.1。设计累计 2/2。日期：2026-11-02。第 1 轮裁决玩法与底座，第 2 轮仅解决 MAGIC 宿主与 SWORD 载荷的装配/施放门，不重审伤害、配置、付款、生命周期或视觉。正文是设计，不是游戏通过证明。

本次是用户新授权的正式术式、装配与视觉接入。此前分身底座的设计 1/2、实现 2/3 是原批历史，不重置、不借本批追加审查旧范围。这里只改变新水幻形实际依赖的通用能力，保留开发分身和普通分身的原语义。

## 1. 输入和制作表结论

用户要求：原地留下水线条勾勒的人形；碰到时散成水并完成反击；或者释放时直接释放某一术式后散架。用户明确选择“从装配链指定，先支持适配过的水系攻击”。不能改成固定缺月，也不能让未适配攻击收费后空放。

制作表已由指定 Cursor 只读解出。四状态页共 60 行，待制作只有表头，没有“水幻形”或 water_phantom/water_mimic/water_illusion；旧已完成 phantom_sword 与本任务无关。源表 SHA256 为 `06a616fbf4484b38d101be875d6f6d0293676052f0f65747e74893ad79c3b179`。本次以用户原话和用户决定.md 为正式输入，不捏造 B/N/O/DV，不领取已完成幻剑，不新增或迁移表行。工作簿保持不变；以后用户要求登记时再按当时真实表状态处理。

逻辑依据是本批 read-facts-final.md、read-payload-lifecycle-final.md、read-chain-effects-final.md、read-sheet-report.md，以及分身底座首版交付与实测、implementation-contract 和两轮复核报告。上述事实调查全部完成停写后才作本轮裁决。设计者不直接读源码内容、不实现 Java、不编译、不操作美术 MCP。

## 2. 本次必须完成的玩家闭环

正式稳定 ID：`water_phantom`，中文“水幻形”，TechniqueType.MAGIC，ShuAttribute.WATER，TechniqueAvailability.PLAYER。原始伤害 0、原始冷却 20 tick、显式魔力 10、等级上限 1、复杂度 1；原始伤害为 0 不代表生成免费。其 ReleaseDefinition 为 ACTION/ACTION（kind/role），TRIGGER，非移动；通过新增的声明式捕获能力识别，不在旧结算器散落按 ID 的分支。

第一版可装配的完整链只有两个有效条目：`水幻形 → 断潮`。第二格由玩家装配，服务端从链读取并按动作适配器登记校验，不能在服务里偷偷替换成断潮。当前唯一区别是只有断潮完成适配；换入其它攻击就明确拒绝。后续增加适配器不需要修改水幻形的取下一格算法。

第2轮载体裁决：该链只装在**法杖槽0**。水幻形仍为MAGIC，断潮仍为SWORD；后者只作为完整捕获计划已消费的载荷获得局部类型例外，不是法杖直接施放剑术。剑技槽、空手和其它载体不开放。使用现有法杖装配界面先放水幻形，再放断潮；需要看见断潮时选择现有“全部”或“剑术”过滤，不新增UI。

两模式使用现有按键与原版潜行，不增加菜单或额外热键：

- 不潜行释放：原地部署，保持水线人形，等待合格接触或有效受击；本轮不限自然寿命的默认由配置 0 表达。
- 潜行释放：在原地形成同一施术起点，下一服务器 tick 发出所捕获攻击，然后立即散水；不等待接触。攻击方向取释放被接受时的服务器视线，不在延迟执行时重读潜行或主人的视线。

每次输入只部署一次。此术式不支持按住自动重复：服务端识别捕获计划后消费当前一次并解除对应 HoldRepeat 状态，后续必须松开再按。不要因为全局冷却为 0 就在一个长按中排出许多水幻形。普通旧术式的按住行为不改。

释放有真实成本、玩家冷却、容量与权限。水幻形不授予主人无敌，不移动主人，不触发主人挥剑，不替主人挡任意区域攻击。人形只负责自己的接触/受击与诱敌；本次不做跟随、同步连招、移动站位、地形造水、潮湿、火种、附魔、反弹或完整水系派生反应。

## 3. 首批准入清单与攻击语义

本次只适配 `tide_sever`（断潮）的**无附魔、无修饰、单发静止斩击**。使用其真实稳定 ID、当前配置、既有水斩贴图与原始攻击公式；攻击体积原点来自水幻形，归属来自主人。不调用 TideSeverEffect.execute 后临时移动主人或覆盖旧 CastFrame。

断潮当前基础参数：原始伤害 6、原始魔力 12、冷却 20、寿命 6 tick、纵深 5、竖高 3、厚度 0.4。声明了的尺寸/寿命从断潮现有配置读口读取，不能另抄一套永久常量覆盖配置。它不要求持剑。原始攻击额按现有单段无附魔公式，在部署时取当前攻击属性/该次法杖加成；没有附魔和额外水系载荷。最后只按 tide_sever 乘一次术式及全局伤害倍率。

第一版拒绝以下内容，拒绝发生在所有收费之前：

- 缺月 water_blade（含旧别名规范后的同一术式）：依赖主手、宿主拍序、弧带烘焙、分道视觉和弹反；本次没有完整适配这些语义。
- 让锋 water_yielding_ribbon、垂丝 water_weeping_arc：其服务/运动/维持语义未迁到 actor。
- 水幕、水环、雾雨、深海重压、水牢：不是本版的单发攻击载荷，不因 role 恰好是 ATTACK 就放行。
- 潮印及所有附魔，索敌、弹道修饰、一分为三、双重、多重、步进、领域绑定、宿主、命中接续、自身动作、任意前缀或后缀、多个水幻形、未知或未登记的适配器。

原版玩家单独释放断潮/缺月/让锋/垂丝保持原有行为，本批不复审其旧路径。分身断潮明确保留正常无敌帧、不添加领域伤害、不带旧链副作用；不会声称与原主人链里绕帧/领域加成的组合完全等价。这是新 actor 适配的明确边界，不能暗中调用 classifyRoot 恢复旧副作用。

## 4. 通用捕获能力、编译与消费索引

新增声明式能力，建议名 `ActorCaptureContract`，由 ReleaseDefinition 默认返回 NONE，水幻形的定义声明 NEXT_SINGLE_ATTACK。内容包括：捕获的角色、最大条目 1、允许的属性 WATER、要求显式 actor 适配器、禁止修饰/嵌套、入口模式选择器、一次部署策略和可用载体。不可通过 NBT 或客户端包伪造这些能力。

新增纯结构编译器 `ActorCaptureCompiler.inspect(Skill)`，返回 NOT_APPLICABLE、REFUSED、PLAN 三种结果。普通链没有此能力时立刻 NOT_APPLICABLE，完全留给现有路径。只要链任一条目声明捕获能力，就先检查原始整链；本版必须恰好两格，第一格捕获、第二格为适配表允许的普通攻击。未登记、缺失、原胚/修饰超出本版能力或结构多余时整体拒绝，不能跳过坏格继续放剩余部分。

`CapturedStructurePlan` 至少保存：captureOriginalIndex=0、payloadOriginalIndexes=[1]、consumedOriginalIndexes=[0,1]、chargedOriginalIndexes=[0,1]、nextOriginalIndex=2、payloadTechniqueId、原胚不可变副本、适配器键/版本、结构纪元。索引按原始链，不能用移除后的下标判断计费。此结构对象不含主人、位置、目标、武器、费用或伤害，不放动态数据进全局计划缓存。

统一路由必须位于 SkillUsePacket.executeSkill 内、BatchReleaseCompiler.compile 之前（本次调查约第 250 行），也在 BindingLayout、ChainProgress、双重和普通 pressCommitCost 之前。TechniqueAccess和持剑/容量等门保留；法杖类型门仅按第4.1节声明式已消费载荷作局部例外，不能仍无条件挡住SWORD断潮。编译器判 PLAN 后交给通用 `ActorCaptureDispatcher`，成功或拒绝都返回最终结果，**不再落回原主人循环**。这样被捕获的第二格不会被 pressCommitCost 再报价，也不会由 playAction 或旧命中回调再放一次。

本版是整链被该计划完全消费，nextOriginalIndex=2 等于末尾。无需推进旧步进游标，不把它留在第二格待下次执行。若未来允许前后缀，才实现投影与返回游标；本次多余内容直接拒绝，不套领域标记的 BoundReleasePlan，也不借其每轮收费机制。

在装配菜单/SkillAssemblyPacket 的服务端校验中使用相同结构编译器给出说明；装配编辑中的未完成半链可保存时明确显示“缺少支持的下一攻击”，但任何释放入口都拒绝且不收费。不得只做客户端提示。原胚若会改变攻击结构/几何能量，第一版拒绝非默认原胚，不把玩家选项静默忽略。

捕获计划本版不进入 CastPlanCache，不声明 supportsStructuralPlanCache；两格编译有常数上限。新能力注册按现有 ChainStructureEpoch 变更。未来缓存只能存结构和能力戳，不能存已配置金额或目标许可。

### 4.1 第2轮补充：法杖载体准入及草稿

最终事实见本批implementation-contract-report.md与read-carrier-gates-final.md：断潮真实类型是SWORD；StaffSkills.castRefusal在asSkill之前拒绝它，offerNow还有第二道SWORD门；法杖put及菜单mayPlace也拒绝。存取本身不按SWORD删条目。因此只补“执行到executeSkill之后的捕获”仍然不可用。第2轮对此作最小裁决，A/B已冻结的DTO、报价、租约、体积签名不变。

C在ActorCaptureContract追加不可变carrierMode=STAFF_ONLY与capturedTypeExceptions={SWORD}；NONE契约默认为NONE/空集合。新增纯ActorCaptureCarrierGate，复用完整结构编译结果。只有实际法杖、完整PLAN、对应原下标被payloadOriginalIndexes与consumedOriginalIndexes收走、显式适配器合格且类型在例外集合中，才豁免该一格的类型拒绝。不按water_phantom/tide_sever ID开门，不改TechniqueType或SkillCarrier全局规则。

编辑和执行分开：一格合法捕获宿主可作为DRAFT保存，提示缺少下一攻击；DRAFT不能释放，也不授权放任意SWORD。法杖packet追加按当前完整entries加新条目建立候选；GUI按替换gearIndex之后的非空齿轮紧凑顺序建立候选，原胚同行保存，空齿轮不冒充逻辑下标。插入/替换只有合法前缀DRAFT或完整READY能走捕获例外；反序/多格/未知/未适配/非NONE原胚拒绝且不消耗。

拆下或移动已有条目允许无损编辑：暂存缺宿主的载荷或反序只是不可施放的草稿，不能默默丢弃原胚/载荷，不给永久类型豁免。每次新增/替换重新校验完整候选；每次施放重新完整校验，不能信任菜单缓存。纯删除/移动由服务器实际操作确定，不信任客户端声称“删除”却新增内容。普通链转回合法法杖内容后走原规则。

学习/解锁按每条真实类型分别检查，断潮不是因为被水幻形捕获就自动学会；开发可用性、容量、原胚、HeldSwordGate等仍生效。原始entries中的未知ID不得先被asSkill丢弃，再把剩余两格判为合法。

C同时修改StaffSkills.castRefusal与offerNow第二道法杖SWORD门，让它们读取同一个gate并仅豁免已消费下标；executeSkill/dispatcher仍在Batch/报价之前防御核对实际法杖、槽0和当前结构纪元。所有半链/非法组合/手写NBT均收费、冷却、游标、统计之前拒绝并解除本次按住。普通法杖裸SWORD仍拒绝，普通法术链不受影响。

法杖捕获相关写回不能照搬“先consume再add且忽略false”或“清空后add失败break”。C为新路径先建立完整候选，验证后原子写入整个术式列表，失败不改原杖、来源库存、齿轮或携带栈，不截断后续条目，不覆盖法杖魔力及其他NBT。详细事务和精确文件见implementation-contract第9节。此要求仅覆盖本捕获准入所涉及编辑，不扩修旧普通链库存逻辑。

非法杖put拒MAGIC、剑技菜单拒MAGIC、剑技同步跳过MAGIC均保留；本版不经过这些门，不为混装删除它们。法杖GUI没有隐藏SWORD的额外类型预滤，不改现有过滤按钮。唯一正式载体是法杖，不再留“法技/法杖任选”的模糊入口。

## 5. 冻结时点、成本与冷却

在服务端本次输入通过静态结构准入后，冻结模式、脚底位置、眼睛原点、视线，以及所选攻击的武器/属性/配置快照。武器只复制用于当次合法报价和伤害公式，不保存活主手引用，不把整份玩家对象放进载荷。断潮本版不要求后续仍持同一物品；主人离开原位、换手或后来获得增益不会改变已部署攻击。

`CapturedAttackSnapshot` 保存已配置 `ConfiguredDamage`、几何尺寸、生命周期、贴图/表现样式键、原始与已配置费用记录、适配器版本/能力戳、owner UUID、根事件 UUID。不能保存 Skill、CastFrame、回调闭包、当前目标活引用、Tally 或主人的领域状态。本版不热重载；即使未来允许结构注册更新，已部署快照只使用本次支持的稳定适配器版本，失效时安全取消，不换成另一种攻击。

部署报价由两笔真实单价组成：water_phantom 的 manaCost，加上被捕获攻击的标准单价。每笔分别走 BalanceService/既有 BalanceDisplay 的一次全局魔力处理，合计用 long 检查后收窄到付款接口可表示的非负 int；溢出拒绝，不夹成免费。不要把合计再次乘全局倍率，也不收 clone_phantom 或 clone_response_pulse 的费用。

一次 `ManaPaymentReceipt.charge` 支付合计。预留容量、冻结规格、检查坐标/世界/主人/冷却在付款前；创建失败或发布前异常按同一收据原额 refund 一次，撤销索引和容量。**成功部署即两笔均已消费**；此后未触发、撤回、自然到期、目标离开、主人死亡/离线/换维或世界卸载都不退款。触发/反击不再报价或扣费，不因魔力见底而无法出手，也不在反击失败时产生资金债务。

玩家冷却在成功部署时一次写入，值为生成术式配置冷却加载荷配置冷却，然后只应用一次本次载体已有冷却缩减规则。默认合计 40 tick。先以 long 校验，不能回绕。使用现有玩家 CastCooldown 对应的单次冷却接口，不能只设置分身服务的开发生成冷却；实现者记录实际调用签名。失败不装冷却，反击不再重置玩家冷却。每玩家每服务器 tick 至多接受一次捕获部署，作为调度硬边界；全局冷却为 0 也不突破。

成功才执行玩家原有“本次释放成功”的使用统计/学习处理一次；不在载荷触发时再次计使用，也不让主人两格各获得一遍重复整链统计。若已有统计函数以整链处理，沿用其一次成功调用即可。失败不推进游标、不学会、不耗用物品印记。

## 6. 生成与旧底座的兼容方式

现有 `CloneService.spawn(owner, kind)` 保留，仍是开发入口且固定基础脉冲；不把开发术式改为 PLAYER，也不删它的防御检查。新增正式来源的 `spawnCaptured(owner, PreparedCapture)`，内部共享安全的容量预留、实体票据、索引、发布及生命周期原语，入口授权由类型化 `CloneSpawnAuthority` 决定。

Authority 至少区分 DEVELOPMENT_COMMAND 与 PLAYER_CAPTURED_CAST：开发来源验证原三个 DEVELOPMENT_ONLY；玩家来源验证本次捕获定义 PLAYER、真实计划与指定 adapter，不能仅因为 actionKey 是某字符串就放行。`CloneActionRegistry.register` 改为显式动作加能力描述，去掉 `instanceof ClonePulseAction` 才有 full 的硬编码。旧脉冲显式登记原七项能力；新动作登记实际具备的 actor 原点/方向、主人资源、身体/主手不改、无广播、延迟隔离能力。默认未知能力全拒绝，不信任运行期输入自报 full。

给 CloneSpec 增加独立策略：trigger policy、termination policy、presentation key、可选 captured payload/lease 引用。旧构造或旧 profile 默认保持 MANUAL_AND_HURT、普通预算和僵尸验证表现；水幻形采用 PHANTOM、ONE、BREAK_ON_TRIGGER、WATER_OUTLINE，以及自己的捕获部署策略。不要因为 kind==PHANTOM 就把旧开发幻影也改成碰一下必散或致死反击。

水幻形原地驻留、无自动跟随、不主动走动，使用当前 clone_actor 0.6×1.95 的基础身体/眼高合同。owner 重叠允许；实心方块、世界边界外、未加载位置拒绝。保留必要环境物理，不把“原地”实现成搬主人或跨区块传送。诱敌沿用已有有界原版僵尸/骷髅策略，半径来自 water_phantom 自己配置，不扩大到所有 Mob。

## 7. 接触、受击、致死与破散顺序

“接触”本版指与水幻形实际身体 AABB 重叠的、主人可以攻击的非友方 LivingEntity。主人、任意 CloneEntity、旁观/死亡/移除/无敌实体、不能 PvP 互伤的玩家、主人的可识别宠物、与主人同队的非玩家都不触发。方块、地面、水流、掉落物和经验球不触发反击；否则原地出生就会被地面或主人消耗。

接触只做相交，不自动追踪远处敌人。用 LivingCandidateQueries 查询本体 AABB，再精确相交和实时权限检查；每次至多检查 32 个候选，选择第一个合法者，同一 tick 多人接触只认成功抢到触发锁的一个。碰撞回调可作为加速入口，但仍须相同资格检查；周期本体重叠检查是保证已重叠/瞬移进来的实体能触发的兜底，不靠玩家专属 playerTouch 处理所有生物。

受击来源为合法敌方 LivingEntity 或其可归属的投射物时，也可触发。要求本次 `super.hurt` 真正接受，取消/免疫返回 false 不触发。有效受击包含本来致死的一击；环境致死则只结束，不向空处补一次“反击”。有效反击在逻辑上不依赖剩余生命，所以不能沿用现有“已死就 end(DEATH) 不反击”的固定分支。

致死实现采用水幻形专属的局部受击事务：进入 super.hurt 前记录可用来源、位置/朝向和嵌套深度，设置仅对此次调用生效的死亡收束延后门；die/remove/生命周期回调遇该门只记录终止原因，不立即撤销捕获票。super.hurt 返回后，在 finally 收口：若接受且合格敌方来源且 owner/世界仍有效，则抢占 BREAK_ON_TRIGGER；否则按实际死亡/移除原因正常结束。owner 离线/死亡/换维及服务器/世界停止的取消优先，不能被“致死反击”复活票据。门在异常路径必清，普通分身和开发幻影不走此门；不得重放 super.hurt 或伪造已接受伤害。

具体事件先后与最小落点固定如下，归核心作者A，不能让各入口自行猜测：

1. CloneEntity.hurt 首先照旧检查来源scope、主人/友方和当前策略。水幻形尚ARMED且来源合格时，在调用super.hurt前创建一次包内 HurtAttempt；保存当前handle、已存在的deployment lease、眼睛/朝向、来源UUID和伤害事务序号。只是预备，不能在这里消费额度、发包或排反击。若同一实体正在这段同步hurt内重入，水幻形分支直接false，不再嵌套建立票；普通分身不加这条新门。
2. super.hurt 及原版/Forge的死亡判定照常执行，不取消Forge事件来制造命中。**只延后本模组的DEATH收束**：CloneRuntime.endActor 在reason==DEATH且该HurtAttempt未关闭时记录pendingDeath并返回，不取消pending/lease、不删actors/owner索引、不置prepareCancelled。CloneEntity现有hurt里的endLoaded(DEATH)同样通过这一个入口；CloneLifecycleEvents若有分身死亡清理也只走此口，不另行删表。主人自身死亡的OWNER_DEATH绝不延后。
3. 如果原版在该同步调用内要求entity.remove(KILLED)，CloneEntity对这个明确的KILLED原因记录pendingKilledRemoval，暂不调用super.remove；die本身仍正常调用父实现让Forge有机会接受/取消死亡。UNLOADED_TO_CHUNK、世界卸载、服务停止、主动dismiss、DISCARDED及其他外部硬移除不延期，立即取消整个部署；不能把任意remove都当致死反击保留。此延后只覆盖这一段同步方法调用，不留一具死亡实体跨tick等待。
4. super.hurt正常返回后，先保存它的真实boolean结果，再关闭HurtAttempt的延后门。再次核对原handle/lease仍属于本次ACTIVE部署、没有硬取消、owner在线存活同维且session相同。只有返回true且来源通过第7节资格时生成包内一次性的 AcceptedHurtTrigger；该触发允许actor此刻health<=0，不再调用要求actor.isAlive的旧request/frame入口。使用保存的身体坐标，不重新读取主人坐标。
5. 合格AcceptedHurtTrigger原子转移lease到独立票，标记一次预算已用，接着正常end身体并执行待处理移除。此时DEATH清理不能再取消已经按TRIGGERED_TRANSFER转出的票。若不合格/返回false/硬取消/异常，则无票；按记录的死亡/移除要求完成原清理。若死亡事件被取消、actor仍活且这次hurt本身返回false，则恢复原ARMED状态，不凭pendingDeath判为有效受击；若hurt返回true，即使死亡被取消仍是一击有效受击，可破散一次。
6. finally始终清掉门和临时引用。超出预期的异常不得重试伤害或宣称accepted；只完成清理并交回可定位错误。迟到重复DEATH/end/remove见部署已转移或结束只幂等清身体，不能夺回已转出的lease；owner/world级取消仍通过deployment索引取消票或活动体积。

这一顺序的“延期”只保护原有部署直到知道本次hurt是否被接受，不恢复生命、不阻止主人死亡、不按猜测的伤害数值预判致死。A在交付中逐一列出hurt、endActor、remove和生命周期落点，说明谁掌握唯一门，不能只写“致死已处理”。

共同原子顺序：ARMED → TRIGGERED（一次性锁，消费响应预算）→冻结分身眼睛和目标方向→将部署租约转交独立行动票→禁用身体的碰撞、诱敌、受击和继续触发→从活体索引摘掉并 discard→发布散水视觉。接触/受击路径在下一 tick 执行那张行动票，不让身体继续挡伤害直到动画结束。

立即模式的顺序是：部署成功→立即建立同一张独立行动票，实体进入仅维持外观的短暂状态（不诱敌、不碰撞、不受击、不再监听接触）→下一 tick 发出攻击→该外观实体结束并散水。它不是一具仍能挡伤害的临时无敌身体，也不会给予主人无敌。该路径不因为创建时与敌人重叠而抢跑为接触反击。发射失败也结束并散水，不能变成永久留置人形；owner/世界取消同样移除外观。若客户端尚未来得及跟踪实体，独立世界坐标事件照样呈现出招及散水。

冻结目标方向而非活目标许可：触发时以合法接触者/攻击者中心计算方向；重合导致向量退化时用生成时冻结视线。行动执行时目标死亡/离开不改选别的敌人，仍可沿冻结方向发出一次静止斩；实际命中在自己的体积内重新判断合法目标，不保证命中原攻击者。不能称“反击”就绕墙、跨维或强制对已死目标结算。

## 8. 破散后的独立行动票与失败语义

当前 endActor 会取消 pending、prepareCancelled 和索引；本次不能把旧 request 指向已 discard 的实体。新增 `CapturedResponseTicket`，含 server identity/session、owner UUID、rootEventId、deploymentId、actionToken、不可变 actor frame、payload snapshot、执行 tick、过期 tick、独立 owner/world 取消令牌及容量 lease。它不持有 CloneEntity/CloneRuntimeState，不借已结束 actor 的取消 token。

票在当前 tick 原子登记成功后才能拆身体；每具只一张。生成时已预留这一份转换容量，触发不能因临时队列满而保留一个已消耗但未散的身体。Runtime 区分“身体结束”与“部署整体取消”：TRIGGERED_TRANSFER 只结束身体并转移 lease；DISMISS、OWNER_*、UNLOAD、WORLD_UNLOAD、SERVER_STOP 等取消整个部署，连独立票和已发射体积一起清理。普通旧 end 仍取消其全部请求。

独立票执行时间为触发后的下一服务器 tick，最多存活 2 tick；独立 FIFO 与本批/既有响应共享每 tick 32 次提交预算。超时、无 owner、owner 不存活、维度改变、区块不可用、适配器版本失效、非法几何或异常：票终止，释放 lease；身体已经散就不恢复，也不退款，不重试可能已发出的一次攻击。手动立即模式失败给主人一条清晰反馈；被动反击失败可合并提示/计数，禁止逐 tick 刷屏。

正常但打空、伤害为 0、目标被免疫或事件取消，都是已经完成的一次出手，不恢复人形或次数。生成本身失败与部署成功后失效必须区分；前者退款，后者已花费。

所有路径使用当前服务器会话的单调 long tick 和不可复用动作 token。过期/重复触发/重复包/重入不会再生成第二个攻击体积。服务器停止后票据一律丢弃，不在同 JVM 下一局继续执行。

## 9. 断潮 actor 适配：隔离的静止体积，不伪装旧 spawn

事实已经确认：旧 TechniqueHitSpec.spawnReported 即使 suppressRootSettle，仍受 CastFrame 几何/体积、宿主组份数、湿压等影响；旧 tryHit 没有统一 DamagePermission，默认 classifyRoot 加领域并绕帧。故本版**不调用旧 TideSeverEffect.execute、TechniqueHitSpec.spawn/spawnReported、TechniqueHitSettle.tryHit**，也不通过两个布尔开关、清空线程局部或临时挪玩家来伪装 actor 安全。

新增通用静止体积执行器 `ActorVolumeRuntime`，首个消费者 `TideSeverActorAdapter`。这不是又一套任意术式链：它只接不可变 `ActorVolumeSpec`，含显式眼睛原点、正交基、深/宽/高、寿命、ConfiguredDamage、owner/session/lease、视觉样式和一次命中策略。没有 Skill、CastFrame、HostGroup、Tally、附魔、任意 listener、onFirstTrigger 或接续回调字段；接口上就不能带入旧链上下文。

几何合同：以 actor eyeOrigin 为靠近施术者的一面中心，局部 forward 取冻结 look，up/right 用稳定正交基；体积中心为 eyeOrigin + forward×depth/2，半尺寸为 width/2、height/2、depth/2。当 look 接近竖直时用固定备用轴构造正交基，不读主人视线。默认 depth=5、height=3、width=0.4；这明确描述本次 actor 版断潮静止斩的范围，不能无意变成主人眼睛中心。

ActorVolumeRuntime 使用现有候选查询做世界 AABB 粗筛，之后用精确的 OBB 与目标 AABB 相交（可提取已有无副作用几何辅助；若没有，按标准分离轴法实现，不能把粗 AABB 当实际斩击体积）。静止体积不移动、不跟随 owner、不使用 JNI 新接口。每 tick 查询直到冻结寿命到期，目标每次都实时检查，不缓存许可。

阻挡：对每个精确相交候选，从冻结眼睛原点向其与体积相交的目标点做原版方块阻挡检查，遇实心阻挡不伤害；不跨未加载区块做射线或强制加载。无有效路径的目标不记已命中，下个 tick 可重新验证。体积位置始终固定，受击方移动会由候选复用已有 Living 失效钩子正确刷新。

每次准备实际伤害前调用现有 DamagePermission.allows(owner,target) 和底座目标保护，再 `TechniqueHits.classifyRespecting(target, owner, configured.amount(), TideMarkSuccess.NONE)`。一次体积对同一目标最多尝试一次 hurt；在进入 hurt 前占命中 token，防同步重入，免疫/取消也不在随后 6 tick 反复刷同一目标。配置 0 仍按权限成立但不产正伤副作用。每体积最多记录 64 个实际尝试目标；满额后不再增加，作为固定安全上限，不开放复制/去重配置。

不清 invulnerableTime、不做领域合并、不附火种/潮印/自疗/湿压/击退载荷、不操作主人主手、不调用 onFirstTrigger 或旧 ChainScheduler。每个部署/动作/体积独立去重和 token，绝不共享主人 Tally。伤害归属仍 playerAttack(owner)，直接实体也仍是 owner；新体积只提供 actor 几何，不宣称完整 actor DamageSource 已实现。原版 hurt 与其他合法事件的受伤表现仍存在。

每次提交或后续 tick 实际 hurt 时都重开 CloneSourceScope(origin=CLONE_RESPONSE, rootEventId, depth=1)，finally 恢复。不能只在生成那一 tick 标来源，随后命中丢失标记。所有 CloneEntity 都拒绝成为此体积的目标，触发器也拒绝响应来源，避免两个水幻形或普通分身递归互触发。

断潮视觉用独立只读快照复用 `textures/entity/xueximod/horizontal_slash.png` 及其现行帧布局，不创建服务端旧 TechniqueHitEntity 来“借画面”。可以提取旧渲染的纯绘制辅助，不能为复用画面重新接上旧命中业务。新静止体积的世界变换必须与上述精确判定一致；需要视觉作者确认的是新人形/散水，本段复用既有断潮素材，不改绘该素材。

## 10. 容量、线程、性能和生命周期

沿用每 owner 4、每 server 128 的分身部署硬容量，**水幻形的一份容量贯穿驻留身体→独立票→响应体积，直到整次动作结束才释放**。不在身体散掉时立刻释放名额，让 0 冷却能堆无限寿命斩击。每份最多一张票、一份静止体积；没有目标复制或每候选生成实体。现有普通分身与开发入口仍按它们原生命周期占/还名额。

待执行票总数不超过占用部署数；每 tick 共用 32 次响应提交上限。活动水幻形最多 128，接触查询每个活体每 tick 最多一次且最多处理 32 候选；体积查询最多每体积每 tick 一次，命中账最多 64 项。候选 helper 的底层返回量仍受区域实体密度影响，不能把业务处理上限写成世界查询绝对耗时上限。诱敌保留现有每 tick 4 次、每20tick轮转、每具8租约等上限，不另起全实体扫描。

所有服务、索引、付款、票、体积和结束在同一服务器线程。异步线程拒绝操作，不以 ConcurrentHashMap 允许跨线程读世界。零水幻形/票/体积时新增 Runtime 首行空返回，不为所有在线玩家轮询、不复制技能、不读主手 NBT、不打逐 tick 日志。

owner 登出/死亡/换维，身体区块卸载，体积相关区域不可用，世界卸载，服务器停止：结束对应部署及其票/体积，丢引用、去索引；不强制加载、不扫描 getAllEntities 恢复、不以 UUID 相同认领新服 actor。出生仍用票据/noSave；不保存抓取链或长期资金收据，不读档恢复响应。死亡克隆事件仅清旧主人，不复制水幻形。

性能沿用 PerformanceRecorder 门控。建议增加 CAPTURE_PLAN_COMPILES、CAPTURE_DEPLOYS、CAPTURE_BREAKS、CAPTURE_RESPONSE_CANCELS、ACTOR_VOLUME_UPDATES 等计数及 ACTOR_VOLUME_UPDATE 耗时；可精简名字，但含义要交付清楚。已有 CLONE_RESPONSE_COMMITS 只在新行动真正提交时一次，不在破散和下一 tick 各记一遍。关闭采样不 nanoTime/构造计数列表；不再包一层 ENTITY_QUERY/TARGET_QUERY。summary 扩枚举，ticks.csv 保持旧列合同。没有性能收益或游戏通过声明。

## 11. 配置合同

water_phantom 进入真实 PLAYER Technique 目录，封存时必须存在；不复用开发 clone_phantom 的配置。只声明有真实消费者的键：

- 标准 manaCost 默认10，单位魔力，沿用0..1000000单价边界和全局魔力；消费者为部署总报价中的生成一项。
- 标准 cooldownTicks 默认20，单位tick，沿用当前标准冷却边界/全局倍率及默认正值时字段0非法规则；消费者为部署合成玩家冷却中的生成一项。
- lifetimeTicks 默认0，整数0..1200000、单位TICKS，使用允许0的 count/inclusive边界；0为当前加载会话内无自然到期，正数为驻留期限。立即模式不靠这个值推迟发射。
- decoyRadius 默认8，数值0..16、单位BLOCKS；0关闭诱敌，生成时冻结进入既有有限诱敌策略。

本版水幻形没有自己的正伤，**不声明 damageMultiplier**；被捕获断潮按 tide_sever 的现有 damageMultiplier/几何/寿命/魔力/冷却读取一次。不能用水幻形倍率再包一层，不能提供一个没人消费的复制次数或反击费用键。

不开放触发类型、模式选择、预算次数、角色/捕获数量、命中去重、容量、轮询频率、展示帧率、碰撞体大小为平衡键。模式来自玩家输入；接触大小与既有实体身体一致，视觉尺寸按确认版交接。水幻形受一次有效攻击就破散，无需另开一个让人误以为能多承伤的 maxHealth 键；内部生命用最小正常值1，环境死亡正常结束，敌方致死按第7节事务反击。

缺配置回默认，非法值按既有解析回自身默认，世界文件保留未知覆盖，不重写旧配置。世界重开才生效，客户端目录/说明用已同步的有效值。伤害和几何冻结之后不在后续命中再缩放，也不因主人换手/后续属性变化而重算。数值默认是本次设计值，未游戏调优。

## 12. 人形与散水的客户端合同

用户已否决v1圆润人形。当前候选为 `docs/资源库/过程归档/水幻形_Steve水线_v2-20261102/`：直接在真实Blockbench中绘制标准Steve六方体的透明水线材质与破散动画。用户明确允许跳过二维先行。本候选**不是已确认正式资源**，具体贴图、碎片、帧速和运行映射以修整完成后同版交接及用户确认结果为准，不继承v1像素尺寸和帧数。

CloneSpec/synced data 新增 presentation key 与表现版本，只在改变时同步。旧开发分身仍用原版人形；water_phantom 使用 WATER_OUTLINE，渲染不借僵尸验证皮肤冒充正式视觉。正式维持帧、破散帧、透明混合、面朝方向、世界尺寸、地面水纹、偏移、帧速均从用户确认后的 visual-handoff 读明，不把贴图像素、32模型单位或碰撞高度相互直接等同。

新增 S2C 视觉事件（仅服务端发送）包含维度、当前连接/服务端session、deploymentId、actionToken、单调事件序号、表现样式/版本、冻结世界位置和朝向、阶段以及有限时长。不能只发送已移除实体网络号。广播给当前追踪者及同维owner，拆身体前取好接收者；立即模式实体还未追踪时也须给附近可见观察者发送独立世界坐标事件。每观察者最多一份，同一个token/阶段去重。

新包号由整合者在当时真实 ModNetwork 表里取未占用值，不能盲写当前调查时的16；协议兼容要求按现有通道策略处理。客户端包入站校验方向、字段长度、有限坐标/时长、维度和允许的presentation枚举，不接受任意贴图路径；只 enqueue 到客户端线程，不回调服务器业务。

客户端 `CloneAfterimagePlayback` 保存独立快照，借鉴 StillSheetSnapshot/StillSheetCache 的清理合同，**不直接把 CloneEntity 塞进只认 TechniqueHitEntity 的旧缓存**。身体remove只结束维持候选，不删除已接受的破散快照；按确认时长播完后清除。世界对象切换/维度不符/断线/对应世界卸载清空，session/序号不符丢弃；容量硬上限256、超限淘汰最旧纯视觉，不能反过来改服务端伤害。

破散事件先于实体消失包或晚到都应可独立播放，不要求客户端曾见过实体。迟到的旧维持生成/同步若 deployment 已终止，不得再显出人形；保留有界短期终止标记直到最大跟踪/视觉窗口结束，并按世界清理。不能无限保存每个已结束 UUID。正在驻留的实体正常 start-tracking 可见维持；已经结束的破散不为新进范围玩家跨时补播。

接触/受击：逻辑身体立即结束，散水可以继续若干视觉帧，反击下一tick发出，不等待动画末帧。立即模式：出攻击的同一tick再发散水。视觉事件只表达已发生结果；掉包/帧率低不改变额度、费用或伤害。不得为让动画播完留一具仍可挡路/受击/反击的服务端尸体。

## 13. 美术确认与资源接入门槛

按用户最新指令继续真实 Blockbench 直接绘制材质与动画预览，不等本文或代码编译才展示。依据 xueximod-art-preview 的“等用户确认这一实际版本”和“未经确认不得列为正式采用版本，也不得送去运行接入”，本设计不替用户批准候选v2，不阻止无资源依赖的通用代码实施。

用户确认应绑定同一版本的Blockbench可编辑源、原尺寸透明材质导出、.bbmodel、真实多视角/关键帧预览和 visual-handoff。本次按用户要求直接在3D模型上绘制，不要求另造.aseprite作为门槛。交接写清单位换算、轴向、原点、层级、身体/散水尺寸、局部世界坐标、透明与遮挡、UV/帧布局、循环及阶段切换。预览速度不是游戏施法时长。假人/地面参照/网格只留示意，不放运行资源。

确认后归档到 `docs/资源库/特效/水流/水幻形_<确认版本>/`，保留过程稿，资源库索引由唯一作者更新。运行路径建议 `textures/effect/water_phantom/<确认版本>/`；具体文件名按最终导出映射。Cursor 只能复制确认版，核对引用和必要哈希，不从过程目录偷偷选一版。未确认时可以完成纯代码，正式视觉接入/整项待验收保持未完成，明确原因而非用僵尸皮肤交付正式水幻形。

## 14. 拟新增类型与实施合同

以下是拟新增或拟扩展合同，不是声称仓库已有这些签名。可按命名门禁展开名称，但语义不可省略：

```java
ActorCaptureInspection ActorCaptureCompiler.inspect(Skill skill);
CaptureDispatchResult ActorCaptureDispatcher.tryExecute(ServerPlayer owner,
        Skill skill, CapturedStructurePlan plan, CapturedInput input);
CaptureQuote ActorPayloadAdapter.freezeQuote(CaptureQuoteContext context);
ActorVolumeSpec ActorPayloadAdapter.instantiate(CapturedAttackSnapshot payload,
        DetachedActorFrame actorFrame, CapturedActionAuthority authority);
SpawnResult CloneService.spawnCaptured(ServerPlayer owner, PreparedCapture prepared);
boolean CloneService.breakForResponse(CloneHandle handle, CaptureTrigger trigger);
void CapturedResponseRuntime.cancelOwned(UUID ownerId, CaptureEndReason reason);
void ActorVolumeRuntime.submit(ActorVolumeSpec spec, DeploymentLease lease);
```

CaptureQuoteContext 是当次只读服务器报价输入，freezeQuote 不扣款、不生成、不写主手。它接收从本次释放已经解析的载体类型、法杖属性加成、冷却缩减和武器副本，不能在稍后触发时重新猜主手。PreparedCapture 是通过静态准入/报价的不可变计划，不能由网络直接反序列化；付款与发布只归生成事务作者。DetachedActorFrame 只带世界会话、位置、方向、owner与根事件，不带已被discard的entity引用。DeploymentLease 在身体/票/体积之间单向转移，release幂等，不能复制两份。

ActorPayloadAdapter 注册时显式声明支持的 Technique ID/属性/结构能力/版本和需要的门槛；检查来源从登记表查询，不在水幻形或SkillUsePacket里写 tide_sever 特判。首次只有 TideSeverActorAdapter。适配器不能通过反射直接拿旧effect；能力不符返回稳定拒绝码。部署保存的适配器版本与每次执行的登记版本不同就取消，不把这种变更命名为配置热重载，不重新跑旧链。

拒绝至少包括：CAPTURE_MISSING_PAYLOAD、CAPTURE_COMPLEX_CHAIN、CAPTURE_UNSUPPORTED_PAYLOAD、CAPTURE_UNSUPPORTED_EMBRYO、CAPTURE_INVALID_QUOTE、CAPTURE_CAP_REACHED、CAPTURE_OWNER_UNAVAILABLE、CAPTURE_GEOMETRY_INVALID、CAPTURE_ADAPTER_CHANGED。输出中文解释如“水幻形后仅支持一段已适配水系攻击；本版支持断潮”，不只返回内部码。

## 15. 文件所有权与实施顺序

根目录 `D:/alianximod/`。派发前按本清单将每份文件交给唯一 Cursor 作者；共享文件不得与其他聊天并写。推荐先冻结DTO合同，再并行独立包，最后整合；每个作者显式 `--model grok-4.7-high-fast`。

A：分身状态/租约/票/接触受击作者。独占 `shu/technique/release/clone/core/` 中 CloneService、CloneRuntime、CloneRuntimeState、CloneEntity、CloneSpec、CloneTriggerInput、CloneTriggerPolicy、CloneLifecycleEvents、CloneEndReason，以及同包新增 DeploymentLease、CapturedResponseTicket、CapturedResponseRuntime、CapturedAttackSnapshot、DetachedActorFrame、CloneSpawnAuthority 等。旧默认语义由A保持，不改B/C文件。A负责一次收费/退款/发布、生命周期、容量转移和致死事务；不得自行接旧spawnReported。

B：actor攻击作者。独占 `shu/technique/release/clone/action/` 的 CloneActionRegistry/能力接口最小扩展，以及新增 `shu/technique/release/clone/capture/` 内 ActorPayloadAdapter、ActorPayloadRegistry、TideSeverActorAdapter、CaptureQuoteContext、CaptureQuote、ActorVolumeSpec、ActorVolumeRuntime、ActorVolumeGeometry 等纯载荷/体积文件。B负责显式报价与已配置伤害、几何、目标权限/去重、每tick来源隔离；不扣钱、不写玩家冷却、不结束身体。不改旧四个水系效果、TechniqueHitSpec、TechniqueHitSettle、TechniqueHits。

C：装配与公共注册整合作者。独占新 ActorCaptureContract/Compiler/Inspection/StructurePlan/Dispatcher/Input及ActorCaptureCarrierGate（放 `shu/technique/release/capture/`），新 WaterPhantomDefinition/Balance/注册类（放 `shu/technique/release/clone/water/`）；既有 `network/SkillUsePacket.java`、`network/SkillAssemblyPacket.java`、`gui/SpellAssemblyMenu.java`、`item/StaffSkills.java`、`shu/ModTechniques.java`、`shu/technique/release/ReleaseDefinition.java`、ReleaseDefinitions.java、必要的 HoldRepeat 接口及玩家冷却挂接。C只调用A生成和B冻结报价，不重复收费。第2轮新增的StaffSkills及载体门只归C，A/B记录、租约、报价、体积签名不变。实际包路径若与目录风格不同先登记后使用，不同时让A/B改同一接口。水幻形未拥有伤害倍率，不为它改 BalanceCatalog.DAMAGE_HOSTS。

D：客户端/网络/确认资源接入作者，可在槽位不足时由C完成但不能同时写共享文件。独占 `client/CloneRenderer.java`、新 CloneAfterimagePlayback/Snapshot/ActorVolumeVisual 类、新 S2C 包、ModNetwork 注册、XueXiMod 客户端挂接、必要语言文件与最终确认的运行资源。D与A约定只读表现字段接口，由A唯一修改CloneEntity。PerfMetric/PerfCounter 仅C统一追加。资源库README仅指定艺术归档/整合者写。

艺术作者：已指定Cursor独占当前Blockbench文档与v2候选过程文件，直接在3D模型上绘制；主会话不并发操作该MCP。v1 Aseprite过程稿保留但不采用，v2无需新建Aseprite。艺术作者不写Java。设计文档和design-decision仅本设计者写，交回后停止写入，后续由主会话重新协调文档所有权。制作表不在任何作者写入授权中。

实现顺序：声明/DTO与能力注册→捕获编译/报价→租约/生成与接触/致死→独立体积→客户端事件/渲染→确认版资源接入。无视觉确认依赖的代码可先做；确认前不把候选资源拷进src。A/B互用的接口先由所属作者冻结，其他作者只改自己的消费者。协议/公共注册统一整合，不在多路任务里各自占包号。

### 15.1 视觉确认前可独立执行的明确范围

- A组上述core文件全部可实施：新增来源/表现**标识**、捕获载荷字段、付款、租约、独立票、接触资格、致死事务、生命周期和旧默认兼容。标识不包含候选贴图路径、未经确认的世界视觉尺寸或帧速。A只暴露只读presentation/token接口，不编辑渲染资源。
- B组上述适配注册/能力、断潮冻结报价、ActorVolumeSpec/Runtime/Geometry全部可实施。断潮沿用已存在的正式资源及其当前取样合同，不重画素材。没有水幻形实际版本依赖。
- C组ActorCaptureContract/Compiler/Inspection/StructurePlan/Dispatcher/Input、新WaterPhantomDefinition/Balance类、SkillUsePacket统一提前路由、装配校验、HoldRepeat/冷却接口可实施。新能力对未声明的旧链返回NOT_APPLICABLE。**water_phantom的真实注册调用与专项声明调用最后接线**：具体视觉确认与资源映射未完成前，先完成类与接口，不把半成品正式术式挂进ModTechniques公共目录或封存目录，避免玩家取得没有正式外观的术式；也不为它临时伪造DEVELOPMENT_ONLY状态。公共底座已有旧注册不受影响。
- D组可先实施S2C数据schema/校验、当前未占用包号登记、纯客户端世界绑定/去重/终止标记/容量清理、独立快照容器，以及只读presentation接口消费。可以提取复用断潮的既有纯渲染；不能写死候选v1的贴图、32帧帧速、平面布局或空间尺寸。新人形/散水具体RenderType、UV布局、维持/破散帧配置、模型或公告板朝向、地面水纹挂载，等待同版交接再实现/接线。

确认后专门的一次整合任务负责：正式资源归档与哈希→按交接实现新人形/散水具体渲染→复制运行资源并核对引用→挂接water_phantom的PLAYER注册、ReleaseDefinition和Balance专项声明→检查客户端目录/提示→所有相关写入收束后的最终编译。不能先声明未注册术式的平衡键骗过seal，也不能注册正式术式但暂用僵尸或不可见表现冒充交付。

因此等待视觉只挡上面明确列出的版本相关接线，A/B与C/D的通用代码不停止。本阶段这些基础改动收束后可以按额度集中复核；拆出视觉接线不重置实现轮次。最终整项交付仍必须覆盖实际已确认资源接入后的源码，不能拿接线前的编译充数。

## 16. 收束、复核、编译与交付

本批设计已累计2/2，写本文时实现复核尚未开始，所以这里记为 0/3。当前轮次以文首为准：实现复核 3/3 已用完。第2轮只裁决新查实的载体门阻塞，设计额度已用完，不再自动追加该阶段复审；不能以新会话、实施咨询或Cursor复审绕过。实现复核等该阶段已派发可推进作者最终反馈且停写后集中进行，承接本批实现额度，返修复审累计最多3轮，不重审旧批次未决。

实现作者交回实际文件、已完成行为、配置字段/消费者、运行资源映射、停止写入和未跑项目。Java修改按流程跑命名门禁；编译仅由主会话指定的Cursor在相关作者和其他聊天共享写入收束后运行 `.\gradlew.bat compileJava --offline --no-daemon`。记录最后相关写入、命令、起止、退出码和源码身份；编译后有相关写入则旧结果不能冒充最终覆盖。不打包、不启动游戏、不建独立测试阶段。

本批完成交付至少包括：玩家可获取装配正式水幻形；两模式可操作；相邻格捕获与收费前拒绝；断潮actor实际体积伤害；接触/有效致死受击一次破散反击；失效不恢复身体/不递归；无二次费用；配置实消费；客户端独立散水；用户确认资源已真实接入；最终编译。缺任何一项如实保留，不把基础脉冲或旧僵尸验证模型当完成。

## 17. 用户实测（设计时列出的检查，游戏里仍未执行）

给用户的短清单和「预期 / 已验证」分界在 `docs/开发记录/水幻形术式-20261102/交付与实测.md`。下面这些仍是设计时的检查项，静态复核和编译都没有代替游戏实测。

前置：用户自行启动最终代码/资源对应开发实例，分别满足水幻形和断潮现有学习/解锁条件；在法杖槽0先装水幻形、再装断潮，有足够魔力。默认不要求主手剑，正式载体只用法杖；剑技槽/空手不支持。记录确认视觉版本，测试命令可用现有管理工具提供魔力，但不以开发clone命令代替正式入口。

1. 正常释放后主人移开，原地为确认版水线人形，不跟主人走；释放时扣默认10+12魔力一次、写默认40tick合成冷却（有配置/载体缩减则按本局有效值）。停留期间不维护扣费。
2. 潜行释放，冻结当前视线；立即松潜行并转头，下一tick仍从原地沿原方向出断潮，再散水。长按不会连续生成；没有一次在主人眼前重复放出的断潮。
3. 合法敌人走入身体、投射物命中、敌方非致死攻击、敌方致死攻击分别验证：身体立即不能再挡路/受击，只产生一次反击；散水可在身体移除后继续。多人同tick接触、接触与伤害同tick只一次。
4. 主人走过、友方/宠物走过、物品落入、站在地面/水中不误触。受击被事件取消或无敌拒绝不破散反击；环境致死只结束。玩家PvP禁用/队伍保护不能靠水幻形绕过。
5. 部署后换手、耗尽魔力、改变视线/属性；反击费用不再扣，伤害/几何仍用部署快照。目标在触发后逃走可以打空，仍散且不退款。隔墙目标不受体积伤害。
6. 两具同时反击保留原版无敌帧，不强行汇总。无潮印/火种/主手附魔被凭空发放或消耗，分身间不递归，主人未被临时移动或旋转。
7. 换成缺月/让锋/垂丝/附魔/双重/三格链/只有水幻形：均在付款、冷却、游标前明确拒绝。单独释放这些旧术式仍走原有入口。
8. 生成失败不扣费；成功部署后撤回/到期/离线/换维不退款且不遗留反击。触发后、执行前主人离线或死亡，动作取消，人形不回来。退出重开/同JVM换世界没有旧票或旧散水。
9. 容量满明确拒绝；触发后的短票/活动断潮仍占同一部署额度，等攻击结束才释放，不产生双占或提前无限释放。
10. 修改water_phantom生成费/寿命/诱敌半径、tide_sever攻击费用/伤害/尺寸/寿命，重开世界分别生效。单改伤害不改费用，全局与局部倍率各一次；0伤仍一次出手/散水。
11. 新观察者正常看到驻留人形；破散即便实体已经移除仍播完；切维/断线不残影，事件次序变化不让旧人形复活。原开发普通分身成功一次后再失败仍能再次请求，原底座语义未回退。

12. 第2轮载体门：法杖单宿主草稿不能释放，追加已适配断潮后可释放；裸断潮仍不能直接放法杖。拆宿主、移动反序、再恢复正确次序，载荷/原胚不丢，非法中间态不收费。剑技槽依旧拒MAGIC，普通法杖链和普通剑技规则保持。捕获相关写回失败不得吞来源或只写前半条链。

用户反馈留待填写。静态复核和compileJava不证明原版AI、hurt/死亡事件时序、网络视觉次序或实际观感已经正确。

## 18. 本轮结论与剩余事项

按本文范围开始代码实施，无需重复确认常规开工。首批只支持真正适配的断潮，其他水系攻击明确拒绝；后续逐个适配再扩表，不把未适配的旧effects直接放进能力注册。

确认前写下的「视觉尚未选定、不得接入」已经结束。用户 2026-11-02 原话「这个版本好接入吧」采用的是 Steve 水线 v2.1。正式登记、专项声明和该版运行资源已由接入报告交回；最终编译见 `compile-integrated-report.md`。游戏实测仍未做，完整验收不以本文自称完成。当前证据以文首和 `交付与实测.md` 为准。




