# 协同、回旋、斩击实现合同

版本：设计第 1 轮定稿，2026-11-03。配套裁决为 `design-round-1.md`。本文规定新增接口的目标语义，不把它们称为仓库现成 API。已存在 API 的事实以本批两组 `facts-*.final-initial.md`、`facts-*.final-more.md` 为准。实施 0/3；没有编译/游戏通过结论。本文只供本批，禁止借机重审或扩修旧批。

## 1. 术式与能力登记

- `sword_oblique_slash` / 斩击：TechniqueType.SWORD，属性 NONE，底伤 4，等级上限 20，复杂度沿剑技默认，冷却 20，显式魔力 4；PLAYER。ReleaseKind.HIT_VOLUME、TRIGGER、STRAIGHT、ChainRole.ATTACK；trajectoryMutable=false、acceptsPathOverride=false、manipulatesSwordEntities=false。命中体默认寿命 4、速度 0、厚度 0.5。伤害/附魔/规则仍由统一链准备，效果不能再次报价。定义可声明原有 cohort 视觉分离能力；不能只改 ReleaseRegistry 的 space_cut 白名单就声称动作已接通。
- `movement_spin` / 回旋：MOVEMENT，NONE，底伤 0，等级上限 1，冷却 10，显式魔力 2；PLAYER。ACTION/TRIGGER、ChainRole.ACTION，新增能力 `TIMED_RELEASE`。无自己的命中体或伤害倍率。玩家载体准入复用冲刺的 MOVEMENT 准入，不把它伪装成 SWORD/MAGIC。
- `rule_cooperative` / 协同：UNIVERSAL，NONE，底伤 0，等级上限 1，冷却 10，显式魔力 2；PLAYER。RULE/TRIGGER、ChainRole.RULE_COOPERATIVE。可被修饰收集器识别；无攻击效果、无 damageMultiplier，不复用 RULE_DUAL/RULE_TRIPLE。
- `clone_body`：保留原 ID、MAGIC、NONE、底伤 0、等级 1、复杂度 0、标准魔力 10/冷却 20 及 CloneBalance 专项；可用性改 PLAYER，名称“普通分身”。独立部署能力 `STANDALONE_DEPLOYMENT`，不能被 ACTION 收集当成带刀动作。原有释放 ACTION 登记可保持，入口优先按能力消费整项。`clone_phantom` 与 `clone_response_pulse` 可用性不改。

新增能力不靠术式字符串散落判断。I 定义并登记 `TechniqueExecutionCapabilities`：动作 gate 类型、是否独立部署、是否需要显式攻击适配。攻击支持由 `ActorAttackAdapterRegistry` 查询；外部协作身体支持由 CloneSpec 的新能力字段决定。所有旧构造器默认能力为无，不影响旧普通路径；普通 clone_body 的规格明确开启外部协作，水幻形和开发幻影明确关闭。

首批 `ActorAttackAdapterRegistry` 有两个真实适配器：新斜斩、横劈。必须支持这两个攻击在回旋、协同以及两者组合下运行，证明前缀可复用。旧横劈无新前缀时仍走自己的原路径；适配器重现其原盒尺寸和静止方向，不修改旧 SwordHorizontalSlashEffect 来“凑适配”。其它攻击（含断潮、缺月、空间切割、世界剑和飞弹）首版尚未登记此能力：含回旋或协同的执行族在任何扣费前报“后续攻击尚未适配该动作/协同”。它们不含新前缀时仍按原规则释放。

新斩击没有前缀也完整可用。其独立出刀姿态由提交桥发出，不依赖回旋。它搭配原冲刺时仍用原接触触发与原伤害职责；不得删掉冲刺原有底伤。跳跃仍执行原位移效果后接斩击，不把 movement_leap 悄悄改成 ACTION，也不承诺新增腾空期间独立时钟。不存在手持剑替身、假玩家或临时改主人位置的适配。

## 2. 普通分身的普通玩家闭环

玩家可按既有学习、术式石、法杖装配方式取得 clone_body。复杂度 0 的自动学会沿既有规则，不另设隐藏学习途径。其首版使用形式是法杖槽 0 仅这一项；服务端完整结构验证先于 BatchReleaseCompiler、纯附魔、普通预扣及 ACTION 分支。与其它项组合会明确拒绝，不能消费其它格后悄悄生成。装配可保留待编辑草稿，但完整释放必须合法；无需扩整套 UI。说明文本写清“单独装入法杖；潜行释放撤回自己的普通分身”。

独立部署路由由通用 STANDALONE_DEPLOYMENT 能力触发，不把 clone_body 字符串分支塞到多处。部署服务是唯一付款方：沿现有 CloneSpawnQuote / ManaPaymentReceipt.charge；主释放路由接受该单项后立即返回，不能再收普通 ACTION 费。先检查合法主人/载体/原物品仍在/名额/冷却/安全生成点，再收款、发布和提交一次冷却；发布失败原收据原池退款。原法杖折扣及载体来源在入口冻结并传给报价，不再在旧 spawn 内重复应用。

潜行撤回只结束该主人、具备普通部署能力的身体及其外部预约，不清除水幻形、开发幻影或其它主人的身体；0 费用、0 新冷却，已有生成冷却不重置。旧 `/clone spawn normal|phantom`、查询、撤回保留。原 `developmentOnly(kind)` 不再以“普通分身必须仍 DEV”为条件，应拆成调用者权限与身体/内部动作允许的来源能力；开发命令权限 2 仍必需。玩家 route 不能获得开发 phantom。

普通分身保留原 REPEAT、MANUAL_AND_HURT、长期驻留及内部脉冲响应。被协作预约期间，旧 manual/hurt 请求返回 BUSY，不扣钱、不消耗响应；预约释放后恢复原行为。协同成功调用的不是脉冲，因此不额外收脉冲 2 魔力、不触发第二次伤害。旧开发请求和新协作共用一个响应占用位，不能各自持一个 pending。

## 3. 统一执行描述与规则传播

I 新增不可变 `ExecutionFamilyPlan`，内容至少为：服务端会话/世界、主人 UUID、父 cast ID/epoch、原链下标映射、冻结载体凭据、动作 gate、可选协同指令、各攻击成员的适配快照与原始下标、规则/附魔/领域上下文、逻辑 copy/spawn 索引、付款份额索引、配置后基础冷却，以及取消/完成 token。不能长期保存会改变的 Skill、NBT 列表、主手 ItemStack 引用或线程局部 CastFrame。

三个入口必须接通：

1. 手动链含新斩击、TIMED_RELEASE 或 RULE_COOPERATIVE 时，BatchReleaseCompiler 必须生成支持本合同的执行计划。不能返回 null 再落旧段循环；不能通过只加入批次触发白名单而让编译器忽略 ACTION。既有不含新能力的链不因此全部重编。
2. 原冲刺带新斩击：编译器把现有接触 gate、份额池和双重关系降低为同一执行描述。接触坐标要实际传入显式几何，不能沿当前 firePaidPlan 传 null 的路径丢掉。没有新功能的冲刺链不在本批修复范围。
3. BindingStructureCompiler / BoundMember / AutomaticRound：保存该被绑定攻击的完整执行描述模板及原链下标，而非只存一个攻击索引或 copyCount=0。每个合法自动轮创建新的运行实例、token、账本，复用该轮原有资源报价；不重扣按键费用。若领域禁止动作的既有结构约束成立，沿原准入拒绝；不能把合法的协同规则默默丢弃。新斩击自身享有横劈相同的领域绑定资格。

规则收集以“本次攻击执行族”为边界，不以配置后的伤害是否大于 0 为边界。协同前缀可以跨 ACTION，索敌/弹道/一分为三/双重/附魔同时保留；有原始伤害的旧冲刺不能意外截断新描述中已经归属后续攻击的前缀。保留原索敌→弹道→规则→攻击的几何职责，不把协同当弹道。双重的成员顺序/间隔、一分为三的份数、错时 10 刻、命中接续的成功凭据依原计划降低，不另写一份只认识 copies 的 ModifierPlan。

一条 RULE_COOPERATIVE 绑定其后一个执行族。该族的双重成员可复用同一演员预约，但每个成员有自己的提交幂等键；下一独立攻击族不继承。重复协同前缀指向同一族、互相嵌套的新定时 ACTION、协同后没有攻击、无法无歧义归属的组合，统一结构检查收费前拒绝。单独回旋允许没有攻击，按独立动作收费。普通重复术式语义不因本规则改变。

命中接续以最终统一账本裁决通知一次，不以每个演员的碰撞触发两次。错时/双重调度不能把同一个 gate 回调当两次播放/扣费；它们产生计划成员而非重复执行闭包。新描述最长有效未提交预约 80 刻；合法展开的最晚提交时间超过上限则整族收费前拒绝，不截去尾部成员。运行时调度必须绑定服务器实例和世界，不能仅把闭包塞入无跨服隔离的旧静态 ChainScheduler。

## 4. ACTION 服务端时间线

I 提供通用 `ReleaseGate`，类型 IMMEDIATE / CONTACT / TIMED；TIMED 由服务端单调刻数驱动，CONTACT 由既有 ActionWindow.Sink 适配。新的 `ActionTimelineSpec(windupTicks, recoveryTicks, movementPolicy)` 是声明数据；movement_spin 的 movementPolicy 为 IN_PLACE。动画通知仅可用于音效/视觉，不能调用 release。

时间线状态：PREPARED → RUNNING → RELEASED → FINISHED；任一未提交态可 CANCELLED。每个 gate token 最多从未触发转为已触发一次。startTick+windupTicks 触发出刀，endTick=startTick+windupTicks+recoveryTicks 结束姿态。默认 6+6 刻。服务器卡顿后按服务器刻推进，不补发多次；客户端迟到快进到同一相位。

独立斩击使用 IMMEDIATE，成功提交时播单人姿态：releaseTick=0，从切入姿态起播、8 刻收势，不先播一个未纳入服务端时钟的拔刀前摇。回旋开始时播动作，出刀时不再用单人姿态覆盖同一次回旋；cooperative 镜像只改表现姿态。无攻击回旋到 releaseTick 不生成体积。CONTACT 保持冲刺原允许的接触次数及份额池规则；本合同没有把整次冲刺改成只能一次触发。每次合法接触派生独立子执行族，且每个子族内部多演员只提交一次。出刀刻是几何发布与视觉切入刻；命中记录仍可能等统一 Tally 的 6 刻刷新才执行最终 hurt，不能靠动画另打一刀弥补这段账本延迟。

双重加回旋：一次回旋 gate 在出刀点启动其已计划成员序列，不为每个演员重播整条链；成员间隔沿现有成员计划。若原链明确包含两个回旋 ACTION，则两个动作是两个族，不能合成一份动作费。出刀之前关闭时退未取份额；出刀之后动作视觉被替换，不撤销已经发布的合法攻击。

在动作开始时冻结逻辑瞄准向量与解析后的锁定/落点输入，出刀时重新读取合法玩家当前位置作为玩家身体基准；视角变化不倒转已计划刀面。旧会跟随目标/路径的攻击不在首批适配名单，不虚构它们的冻结语义。动作期间玩家可自行移动；分身最终位置与出刀时玩家对应站位偏差超过 1 格，或安全条件失效，回退单人而不强拉玩家。配置侧倾角或身体旋转绝不能写回玩家 yaw/pitch。

## 5. 协同预约、短距移动与提交

分身能力字段 `externalCooperation` 默认 false；普通 clone_body 规格 true。必须同时满足：同主人、同服务器会话/维度、ACTIVE、REPEAT、READY、无旧 pending/协作预约、身体存活且区块已加载、未处于 CloneSourceScope 响应来源。只从 owner 索引查最多 4 具；按距离平方再 UUID 排序，不扫全世界。水幻形和开发幻影跳过不提示错误；它们不是合适候选。

预留流程如下：

1. 先计算玩家单人攻击的完整可用计划、费用与实体预算。再尝试协同；单人本来不合法时不能靠分身放宽载体/手持要求。
2. 候选最近优先，逐个检查左右两个相对站位，选移动距离较短者，相同距离固定右侧。前向用冻结水平瞄准，近垂直视线回退施法时水平 yaw。站位为玩家脚点 + forward*0.5 ± right*sideOffset。分身面向共同瞄准方向，斜斩适配器允许另一演员镜像刀面倾角。
3. 预留以现有响应状态的 READY→RESERVED 为互斥线，记录 familyId、token、世代、到期刻。此时不花 REPEAT 响应、不扣脉冲费、不改变动作键，不覆盖旧 pending。可在同一个响应槽用 tagged reservation 表示旧请求/协作，不能另立未互斥的布尔锁。
4. 调用可复用 `ShortReposition`，最长两刻、最大距离默认 4 格。不是寻路：对每一实际移动步做完整身体 AABB 扫掠、方块/实体碰撞、世界边界、构建高度、已加载区块检查；目的地有完整可站立支撑，无流体/火/岩浆/仙人掌等危险块。路径不得穿实心块，也不得穿过玩家/其它生物身体。最多两站位、每段最多 8 个有界扫掠采样，且使用保守 swept AABB，不以仅检查端点代替墙体检测。禁止加载新区块或无界向上下搜索。
5. 没有安全候选、没有预算、移动途中受阻、两刻未 READY，取消协作预约并回退单人。身体停在最后一个验证安全的位置；不危险传回旧点。此类失败不花响应、不记协同冷却。不会再重找第二具分身拖慢同一次动作。
6. 分身 READY 后才开始双方同一回旋时间线；没有回旋的协同攻击最多等待这两刻准备，然后立即提交。既有主人的手动瞄准输入已冻结，准备不强制玩家相机。动作开始之后不再移动该分身，只设逻辑 face。
7. 到实际出刀点复验 owner、载体凭据、父 cast、分身世代/存活/占用、位置偏差、安全与预算。失效的协同降为单人，释放协同份额；单人本身失效取消整次未提交攻击。
8. 先构造全部未发布几何并取得宿主组生成许可，随后在同一服务端调用栈内越过提交线：发布该成员全部几何→标记成员已提交→提交该成员资源份额。第一成功协同成员同时把协作预约标记 committed；最后成员结束或取消后进入一次普通响应冷却。失败成员不再补打一份脉冲。

预约成功不等于伤害命中。一个成员要么发布玩家与分身全部计划几何，要么完全不发布；若双人预算/构造失败，先丢弃双人暂存，再尝试原单人完整成员。不能已发布玩家体积后发生错误又重新发布一份玩家体积。若无法保证 all-or-none，宿主组必须扩展“未发布暂存/最终发布”事务，而不是把异常留到下一刻弥补。

多人同时指挥同一身体时仅第一个原子预约成功。最多一具分身加入一个族，最多一个族占一具分身。REPEAT 不减为 ONE：预约内多个合法双重成员属于一次协作事务，结束后仅进一次响应冷却。冷却取部署时被冻结的原内部响应冷却；合作 rule 自身冷却按本次规则报价，不二次套倍率。族中已经有成功协同成员后不能退整笔协同费用，也不能把后面的失败重新记成从未响应。

## 6. 几何、权限、统一命中与身份

攻击适配器仅产生几何及表现候选，不调用 execute(player)、旧 clone.perform、ActorVolumeRuntime 或任何 hurt。I 的提交桥提供已冻结伤害/附魔与显式 ActorFrame；E 不读玩家眼睛、主手、CastFrame.current 或全局宿主上下文。

斜斩默认形状为定向薄盒：深 3、宽 5、厚 0.5，中心在玩家眼睛向前 1.5 格，长轴沿刀面宽向；刀面相对水平倾斜 35°。横劈适配保持既有横劈盒配置/锚点约定。共用几何求解先算每个逻辑 copy 的链落点/交错结果，再添加对应演员眼点相对于玩家眼点的平移差；方向使用同一个冻结规则方向，分身斜斩可镜像 35°。轨迹不可变仍只允许既有静止斩击的生成点变换，不变成飞弹。显式落点会移动整个协作布局，不能分别读取主人眼睛把分身侧拉回主人。

攻击始终记为合法 owner 的统一链伤害来源。付款/武器/附魔快照来自真实合法载体和手持要求，生成后换手不能增加金额；出刀前凭据失效则取消，不伪造分身手持参与费用或装备伤害计算。分身手上若显示武器，只是不可交互渲染副本，不能生成掉落物。

新增稳定身份：familyId 标识一次攻击族；memberId 标识双重/错时/接续成员；logicalCopyId 标识规则副本；spawnOrdinal 保留现有全局实体倍率；emitId 再含 actorOrdinal。结算去重键为 `(familyId, memberId, roundIndex, logicalCopyId, spawnOrdinal, targetUUID)`，**不含 actorOrdinal**。两个演员只是同一逻辑份数的两次几何发射。不能用 entity UUID 作为伤害 copy ID，不能把分身数量乘到 damage。

现有 Tally 的 int copyIndex 采用本族的密集编号映射 `(logicalCopyId, spawnOrdinal)→settlementOrdinal`，两个演员写同一个编号；避免 `copy + spawn*1024` 在大展开时碰撞。本批显式 spawn 不再调用旧隐式 SkillEntityCount 生成循环，规划时读取一次倍率并展开一次。不同伤害/附魔成员不得混在“最后一份 damage 乘份数”的同一个 FlowRoundLedger 中；按原成员结算族各用一个 Tally，统一回调向父族汇报。

即使只有一份副本，含本批显式演员执行描述也创建一个共享 Tally；所有其体积只 record，绝不并行直伤。第一次有效记录之后沿现有 6 刻刷新节奏；同轮相同逻辑副本只记一次。旧 ledger 的 seen 保留到体积与尾部结算都关闭，不能 clearSettled 后恢复对同轮的命中。配置生命周期较长也不凭空重打；命中间隔为 0 的静止斩击轮次恒为 0。

I 为 Tally 增加可选的 `SettlementGuard` / `cancel(reason)`：默认旧调用行为保持；本批每次 record/flush 都校验服务器会话、世界、父执行有效、主人仍合法及目标权限。取消丢弃待结算与允许载荷，关闭后来的回调，不靠单独去掉实体就以为已排的 flush 消失。主人离线/死亡/换维、世界卸载、服务器停服、父族撤销属于硬取消；已经结算的伤害不倒扣、不退款。

目标在命中与 flush 前都复查 DamagePermission；协作的身体几何还排除任何分身、主人驯服动物和同队，和现有 clone 目标边界一致。冻结 owner 身份/配置/载荷，不把某刻的“允许攻击”布尔值冻结到今后。整个协作 Tally 的最终伤害和载荷执行包裹 CloneSourceScope 的深度 1：阻止再触发其它 clone 响应/递归协同；不把 scope 存在线程局部跨 tick。仍由既有 owner 链 Tally 做最终结算及领域/附魔职责，不能每演员另调用 classifyRoot/classifyRespecting；也不借本批全局更改旧分类函数或无敌帧策略。命中接续收到的是统一裁决，有限 0 与正伤的副作用区别沿现有账本接口保留。

## 7. 费用、冷却和取消线

统一报价先检查整数乘加、适配、完整结构、最大展开与调度跨度；错误在任何 ManaPaymentReceipt.charge 之前返回。金额使用 BalanceService 产生的 ConfiguredDamage，传递后不得再读倍率。所有普通攻击、动作、链修饰份额仍由原父付款收据/UncommittedShare 管理；不能为第二演员重复调用 charge。

新规则的协同份额是可选组件：最多每执行族一份。已知没有可用演员则不预约这一份；若已预扣后回退单人，仅退这一份到原收据原池。资源不足以承担协同附加费但足以单人时，取消外部预约并正常单人；不足单人费用则整族拒绝，不免费攻击。实现必须把可选金额从原“修饰统一预扣”中明确拆出，不能先重复收了再期望退款抵消。

回旋动作费在动作真正 RUNNING 时提交一次；取消前尚未开始则退。后续攻击份额在各成员几何原子发布时提交；没命中不退，取消尚未发布的成员退其未取份额。动作已开始而攻击被硬取消，动作费不退、未发出的攻击份额退。协同费用在首个真正双人发布时提交一次；没有任何双人发布则全退。使用 receipt.refundShare / UncommittedShare.close，禁止 MagicPool.restore 代替原法杖退款。

基础冷却沿原技能组合聚合算法，输入只含实际已提交的动作、攻击和成功协同份额；不因两具身体调用两次聚合、不因双重多个回调重复安装整链冷却。动作费已提交而后续攻击取消，保留动作对应冷却；完全没开始不安装。自动领域轮沿原宿主冷却/轮次规则，不给玩家额外装一次手动技能冷却。普通分身部署冷却与协作响应冷却相互独立。

硬取消：主人死亡/离线/换维/复制、载体失效（未提交成员）、父族取消、区域卸载、服务器停服。软失败：只有分身死亡/被撤回/受阻/过远，未提交成员回退单人，已经发出的协作几何继续归同一合法 owner，除非父族硬取消。撤回身体不“复活”已消耗的协同份额；未提交预约立即释放。玩家自身普通受伤不自动取消回旋，除非已有父释放规则规定取消。

## 8. 新增公共接口与文件边界

Java 根为 `src/main/java/com/bslx/XueXiMod/`。以下新公共合同由 I 唯一落盘，其他作者只实现各自类，不改接口文件。名称为本批冻结目标；若现仓存在同名类，I 在接口阶段统一调整包名并交付完整映射后再派消费者，不能各作者自行改签名。

新公共包 `shu/technique/release/cooperative/api/`：

```java
record FamilyKey(UUID serverSession, ResourceKey<Level> dimension,
                 UUID ownerId, UUID castId, long epoch, int familyOrdinal) {}
record MemberKey(FamilyKey family, int memberOrdinal) {}
record LogicalCopyKey(MemberKey member, int copyOrdinal, int spawnOrdinal) {}
record ActorFrame(UUID actorId, int actorOrdinal, Vec3 feet, Vec3 eye,
                  Vec3 aim, float visualYaw, boolean mirrored) {}
record CooperationSettings(double selectionRadius, double maxMove,
                           double sideOffset, int prepareTicks) {}
record TimelineSpec(int windupTicks, int recoveryTicks) {}
record TriggerGeometry(Vec3 point, Vec3 direction, UUID targetId) {}
// point/targetId 可空；direction 必须有限且可归一化。

interface ReleaseGateHandle {
    boolean trigger(TriggerGeometry geometry); // 单 gate token CAS，重复 false
    void cancel(CancelReason reason);          // 幂等
    boolean closed();
}
interface SettlementGuard {
    boolean active(ServerLevel level);
    boolean allows(ServerLevel level, LivingEntity target);
    void cancel(CancelReason reason);
}
interface ActorAttackAdapter {
    PreparedAttack prepare(AttackPreparation input); // 纯准备，不改世界/付款
    List<PreparedEmit> emit(PreparedAttack attack, LogicalCopyKey copy,
                            ActorFrame actor, ResolvedCopyGeometry geometry);
}
```

`AttackPreparation` 由 I 创建且只读：术式稳定 ID、原链下标、配置后伤害、适配器配置尺寸/寿命、合法武器快照、冻结逻辑方向、opaque `SettlementEnvelope`（已解析附魔/领域/接续载荷）、取消标识。它不包含可变玩家或线程局部上下文。`PreparedAttack` 是上述数据的不可变结果。`ResolvedCopyGeometry` 包含原统一链求解后的原点、刀面法线、roll、pose3d、是否重写及玩家基准眼点。`PreparedEmit` 包含显式几何、寿命、表现 ID、该 logical copy 和 actorOrdinal；不含新伤害单价。首批每演员每 copy 恰好一个 emit，适配器返回其它数量时拒绝计划。

`SettlementEnvelope` 由 I 在共享链包实现，保留既有已配置载荷及资源凭据，E/C 不解析它。`PreparedEmit` 由 I 转成 TechniqueHitSpec；新增显式 planned spawn 入口必须设置强制 origin/direction、共享 tally、密集 settlementOrdinal、guard，并关闭隐式全局实体倍率与 ambient CastFrame 二次变换。不是靠“suppressRootSettle=true”两个开关宣称已隔离。

分身核心对外接口（C 实现，I 先创建接口类型；放公共 api 包）：

```java
interface CooperationActors {
    CooperationReservation tryReserve(ServerPlayer owner, FamilyKey family,
        ActorFrame ownerFrame, CooperationSettings settings, long expiryTick);
    PreparationState advance(ServerLevel level, CooperationReservation reservation,
        ActorFrame currentOwnerFrame, long tick); // PREPARING/READY/UNAVAILABLE
    Optional<ActorFrame> readyFrame(CooperationReservation reservation);
    boolean validate(CooperationReservation reservation, ActorFrame ownerFrame);
    void markCommitted(CooperationReservation reservation, MemberKey member);
    void finish(CooperationReservation reservation, boolean anyCommitted);
    void cancel(CooperationReservation reservation, CancelReason reason);
}
```

`tryReserve` 找不到返回明确的无演员 reservation（不得 null 并留下锁）；reservation 包含 token/世代/身体句柄，不允许消费者直接改 CloneRuntimeState。`markCommitted` 只在 I 已取得 all-or-none 发布许可的同一调用栈中调用，验证/提交之间不回调任意效果。失败状态不得越过发布线；first commit 后 reservation 保留到族结束。C 的 finish/cancel 根据自己保存的 committed 状态决定冷却，不能相信调用方传 false 来恢复已提交预算。

普通部署桥另外固定为 `CloneDeploymentResult deployStandalone(ServerPlayer owner, CloneDeploymentInput input)`、`int dismissStandalone(ServerPlayer owner, StandaloneDeploymentKind kind)`。I 创建 DTO：input 包含 NORMAL、已经一次应用服务端配置与合法载体折扣的 CloneSpawnQuote、冻结载体凭据、来源 PLAYER_STANDALONE；不包含外部已扣款收据。C 的 deployStandalone 是唯一 charge/发布/退款/生成冷却消费者，返回 SUCCESS/具体拒绝及可选 CloneHandle；I 的 dispatcher 不再扣款或重装冷却。C 在方法内部拆分无副作用准备与已付费发布，供旧 dev spawn 共用；旧 dev spawn 自己构造 DEVELOPMENT 来源报价，不可借该方法绕权限。dismissStandalone 只接能力支持的 NORMAL 来源身体，返回实际结束数量，不扣钱。分身核心的这些入口类型同样由 I 先冻结，C/E/V 不互相改 DTO。

可复用移动接口由 C 实现 `ShortReposition.prepare(level, handle, destination, maxDistance, durationTicks)` 返回安全计划或拒绝；`advance(level, plan, tick)` 返回 RUNNING/ARRIVED/FAILED；`cancel(plan)` 幂等。只能操作传入身体，内部以明确运动能力检查，不依赖 rule_cooperative ID。不得接受玩家作为自动位移目标。

I 的执行运行时 `FamilyExecutionRuntime.start(ExecutionFamilyPlan)` 返回 handle；`tick(ServerLevel,long)` 驱动准备、动作 gate、成员提交与尾部结算；`cancel(FamilyKey,CancelReason)` 和 `cancelOwner/cancelWorld/stopServer` 清理索引。挂到现有服务器事件，但运行时实例按 MinecraftServer 隔离，不能全局 static 队列混服。

V 提供表现端口 `ActorActionPublisher.start(ActionPlayback)`、`stop(FamilyKey,actorId,sequence)`，不调用玩法端口。`ActionPlayback` 必需 serverSession、dimension、family/member、actor UUID/实体 ID/世代、单调 sequence、动作资源 ID、serverStartTick、windup/recovery、visualYaw、mirrored、可选只读武器显示快照。C/I 只发布 DTO，不直接引用客户端类。

## 9. 文件单作者与依赖交付

I：共享集成作者，唯一拥有公共 api 新类型及 `TechniqueExecutionCapabilities`、`ExecutionFamilyPlan`、`FamilyExecutionRuntime`、计划 lowering/付款/提交桥；唯一修改 `network/SkillUsePacket.java`、`SkillChainPlan.java`、`SkillSegmentRunner.java`、`AttackBudgetWindow.java`、相关 `BatchReleaseCompiler/BatchCastSession/BatchReleasePlan`、`BindingStructureCompiler/BoundMember/AutomaticRound`、`CastFrame/TechniqueHitSpec/TechniqueHitSettle/ReleaseBatch/FlowRoundLedger/CohortVisual/HostGroupContext` 中本合同所需入口。后面部分文件实际在其既有包中，由 I 按本批事实所指现文件定位，不创建同名替代文件绕开调用点。

I 同时独占 `shu/ModTechniques.java`、`ReleaseDefinition/ReleaseDefinitions/ReleaseRegistry/ChainRole/SkillSegmentMath`、`CloneTechniqueRegistry/CloneReleaseDefinition/CloneBalance`、`BalanceCatalog` 的伤害白名单和新声明挂接，以及必要的 TechniqueAccess/StaffSkills/装配服务端校验、普通分身部署 dispatcher、命令入口与语言说明。I 独占 PerfMetric/计数器新增和任何共同初始化文件。不能让 E 或 C 为注册自己的类顺手改这些文件。

C：分身作者，独占 `shu/technique/release/clone/core/` 中 CloneService、CloneRuntime、CloneRuntimeState、CloneSpec、CloneEntity、CloneLifecycleEvents、ClonePositionPolicy、CloneSourceScope 等需要的核心改动，以及新 CloneCooperationService、ShortReposition 实现、reservation 状态。负责普通玩家部署的 unpaid prepare/paid publish 最小服务入口和现有 dev spawn 共用底层发布逻辑；I 的 dispatcher 只协调报价/路由，不再生成第二具实体。C 不改旧水幻形 capture/action 包、CloneBalance 或注册。

E：新纯攻击/动作作者，独占新 `shu/technique/release/cooperative/attack/` 下 ObliqueSlashAdapter、HorizontalSlashActorAdapter、Prepared 形状实现，新 `.../cooperative/action/` 下 SpinTimelineDefinition、SpinActionEffect、ObliqueSlashEffect。只调用 I API，提交适配器/术式定义/专项声明的注册工厂给 I 挂接。不得改旧横劈效果、TechniqueHitSpec、Tally、共享注册或客户端。专项纯声明工厂可由 E 提供，唯一调用点由 I 接入。

V：客户端作者，独占新 `client/action/cooperative/`、S2C 动作包及 ModNetwork 注册、现有 ActionRuntime/ActionVisualCatalog/OriginalHumanoid 的本批最小扩展、CloneRenderer 的普通分身动作接入、玩家姿态采样/渲染钩子与确认后的运行资源。CloneEntity 的服务端数据字段由 C 单独写，V 不改它；需要字段先按本文 DTO 交给 C。已有水幻形渲染的 skin→flakes 顺序、散水与材质不改。共同主入口如 XueXiMod 若需要挂接，由 I 唯一编辑，V 提供 registrar 工厂。

派发顺序：I 先只落可编译的接口/记录/能力定义与依赖清单，终局 valid/reported_done/停写后，C/E/V 才能依这些文件并行实现各自不重叠范围；可编译这里是接口目标，不授权中途共享 Gradle。它们分别交最终完成回执后，I 接回共享集成和注册，不在作者仍改依赖时统一编译。若共享类型需变化，停止消费者并交接口修订，不能无声改签名。

独立代码可在美术确认前完成：全部服务端、能力/装配拒绝、付款/冷却、动作协议、姿态采样框架、现有模型的临时内部调试显示。不得把未确认资源当正式 fallback 或复制进 src。正式斩光/动作关键帧/声音/贴图及其加载路径登记必须等主会话给出实际同版本预览确认。没有资源时不得把整个功能标成视觉已完成，但不阻塞 C/E/I 的逻辑作者。

## 10. 客户端动作与清理

共用一个演员动作播放模型服务玩家和普通分身；避免复制 SpaceCutSampler 的只认一个 ID 分支。新 action IDs 为 `xueximod:oblique_slash`、`xueximod:spin`；协同通过 mirrored/actorOrdinal 参数复用，不新增伤害动作 ID。默认单人斩击上身从右上向左下出刀，骨骼收势归零；具体曲线等实际视觉确认。回旋驱动身体骨骼与持刀，不改玩家视角、真实碰撞盒或头部瞄准数据。

每个 actor 只保留最新有效 sequence。新动作替换旧视觉；旧 stop 不得清新动作。跟踪玩家/分身的客户端收到 start，自己也收到；新进入 tracking 的客户端可以取当前未结束快照，按 serverStartTick 快进，不补播开头。结束、实体卸载、世代变化、换维、断线都清缓存；循环播放不得用于一次动作。动作总时长与服务器冻结数值一致。

父攻击已提交后 pose 被打断不撤销伤害；攻击未提交被取消应发 stop，网络丢包仍由结束时刻自行清理。不允许客户端包请求“已经到出刀帧”来提交伤害。斩光由攻击 emitId 驱动，只播放实际发布的几何；仅分身归位成功但最终回退单人，不应残留第二道斩光。显示武器快照大小有界，只取渲染所需物品/模型数据，不同步整个背包。

## 11. 配置合同

按现行 `schemaVersion:1` 的 `techniques.<stableId>.<flatKey>` 登记；缺字段回代码默认，不覆盖原文件，不另开热重载。服务端读取显式 BalanceSide.SERVER；客户端只收冻结动作时间/形状或使用已同步快照。标准 manaCost/cooldownTicks/damageMultiplier 不再在专项重复声明。

- sword_oblique_slash：标准 manaCost=4、cooldownTicks=20、damageMultiplier=1。专项 `boxDepth=3`、`boxWidth=5`、`boxThickness=0.5`，正尺寸、单位 BLOCKS、最大 512；`lifetimeTicks=4`，正整数刻，沿现行合法范围。`slashTiltDegrees=35`，数字闭区间 [-80,80]，角度单位（若现成 BalanceUnit 无角度，使用现有无量纲单位并在描述写“度”，不得伪称已有枚举）。prepare 时一次读取，适配几何与客户端形状真实消费；不登记未消费的 motionSpeed。
- movement_spin：标准 manaCost=2、cooldownTicks=10；无 damageMultiplier。专项 `windupTicks=6`，整数 [1,20] 刻；`recoveryTicks=6`，整数 [1,40] 刻。动作启动前冻结，服务端 gate 与同步 DTO 都使用，禁止两个时长来源。没有另一个只供动画、不影响时序的伪释放参数。
- rule_cooperative：标准 manaCost=2、cooldownTicks=10；无 damageMultiplier。专项 `selectionRadius=8`，数字 [0,16] 格；`maxRepositionDistance=4`，数字 [0,8] 格；`sideOffset=1.25`，数字 [0.75,2] 格。构造执行族时冻结，分别由 owner 索引距离过滤、ShortReposition、站位求解真实消费。0 选择半径/移动距离允许只接纳原地满足条件的身体。prepareTicks=2、候选上限、预约 TTL、队列及实体硬预算是结构限制，不伪装成术式平衡键。
- clone_body：复用已有 manaCost/cooldownTicks/lifetimeTicks/maxHealth/decoyRadius，不新增重复生成费。生命/寿命/诱敌及内部响应报价沿原部署冻结，不因合作结束重新读配置或重启寿命。

新斩击加入内置 damage host 声明；武器原始份额按既有规则配置一次，ConfiguredDamage 向下传递，E 不对结果再次 damageContribution。新斩击配置为伤害 0 仍是 ATTACK，规则资格看结构，不因 0 跳过份数和合法接续的既有语义。原横劈适配读取它自己的 boxDepth/boxWidth/lifetimeTicks，不抄新斩击键。

新计划若配置寿命/尺寸虽合法但当前生成预算不允许，应明确资源拒绝；不得偷偷把配置值截短后宣称已生效。金额/份数乘法用有界或 checked 运算，溢出拒绝在收费前。修改指南只由 I 在自己的实现交付阶段补真实已接入键，设计作者本轮不写该指南。

## 12. 性能硬边界与交付关注点

复用既有身体名额 owner 4/server 128；外部预约计入原每身体一个 pending。协作预约全服最多 128，每 tick 最多 32 个新协作准备/提交，合用现有 clone 工作预算而非再追加 32；没有预算当次回退单人，不排无界等待。每次最多 4 身体×2 站位，移动最多两刻；不走 LivingCandidateQueries 的超限全世界兜底。

新动作/族运行时全服最多 128 活动族、每主人最多 4；单族最多 128 planned emits（计规则副本×全局实体倍率×演员数），全服本批同时存活 planned emits 最多 256，每 tick 原子发布总量最多 128。预算在计划阶段检查、发布前复验，租约到最后实体和待结算关闭才归还。双人超预算而单人合法时回退单人；单人本身超预算整族拒绝，不删 copies、不把全局倍率从 4096 静默压成 1。这些是本批硬安全限额，不声称旧全仓已经有这些限制。

新体积目标记录每个成员账本最多 64 个目标，候选按距离/UUID 的确定顺序选前 64，所有演员共享该集合；不得每演员各 64 再相加。同一 actor 的动画网络只在 start/stop/tracking 补发，不每 tick 群发 pose。单族 pending Tally/监听器/网络快照有界，过期与取消清理 UUID 索引。性能统计用现有 inclusive/count 工厂，只增真实准备、提交、拒绝、回退、emit 计数，不能注册没有消费者的计时键。

实现阶段集中复核重点（在本批实现额度内，非新增测试阶段）：

1. 单独新斩击；回旋→新斩击/横劈；协同→二者；协同→回旋→二者。无分身、水幻形独存、普通分身冷却/被占用都正常单人，不扣脉冲费用。
2. 同一目标双演员命中只一份；只在分身几何内的目标实际受击；一分为三各命中仍三份；双重/错时/接续/领域模板不丢原下标、份数或载荷。配置伤害 0 与权限拒绝区分。
3. 旧普通横劈/冲刺/跳跃/水幻形不含新能力时路径未被改写；有新能力时不存在退回旧循环或 AutomaticRound 绕过描述。接触几何实际到达新斩击。
4. 发布失败、双人回退、死亡/登出/换维、分身致死/撤回、换手、满名额、墙体/危险地面/边界/未加载区块、超限展开：钱退原来源、动作/攻击/协同份额不重复，账本和预约无残留。服务端同时存在多个实例时不交叉执行。
5. 动画未加载/低帧/迟到/替换不多打少打；镜头不转；旧水幻形绘制顺序保持。未确认资产没有混入运行目录。

接口及相关源码全部写入收束后，由主会话指定固定 Cursor 串行运行 `./gradlew.bat compileJava --offline --no-daemon`；不 build/jar/reobf/runClient，不把源码中途编译当最终交付。必要的纯状态/几何/份额测试可随实现同阶段进行，但不另设测试额度。最终明确记录真实编译结果、未做游戏验证及实际资产确认版本。本文定稿停止写入，不要求用户重复批准代码开工。
