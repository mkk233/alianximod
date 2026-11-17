# 分身底座设计与实现指导

> 实施更新（2026-11-02）：首版已按本文落地，经两轮集中实现复核，最终 compileJava 与命名门禁通过，游戏未测、未打包。本文保留设计定稿时的历史时态和接口建议；实际接入、命令、边界与最终证据见[首版交付与实测](D:/alianximod/docs/分身底座首版交付与实测.md)。最终阶段计数为设计 1/2、实现 2/3。

批次：分身底座-20261102。版本：v1，2026-11-02。设计裁决者：本批唯一 gpt-6-astra。设计第 1/2 轮，实现尚为 0/3。

本文是本次 Cursor 实施合同，不是已经实现或游戏验收通过的说明。本文中的新类、新方法、新枚举均为拟新增；实际工程包路径由实现者在所指定范围内按现有风格落实并交回。既有接口事实来自本批 Cursor 最终只读报告，不以本文的拟新增签名冒充既有 API。

## 1. 本次交付与边界

本次要做出同一底座上的两种真正可生成、可受击、可响应、可结束的驻留分身。幻影最多成功提交一次响应；普通分身可在冷却后多次响应。存在期限、生命、触发、响应次数和结束策略分别建模，不能用一个 lifetime 或一次受击回调兼任全部状态。

**首版入口是权限等级 2 的开发命令，不是已经接入普通玩家装配栏的完整分身术式。** 命令进入真实服务，使用真实登记的基础 Technique 和现有平衡配置、玩家魔力、权限及伤害裁决。基础功能必须实际运行，不能交付只有接口、占位 Dormant 或空消费者的框架。首版不制作完整水幻形，不播放视频连招，不做自动跟随/协同寻路，不实现完全同步规则，也不宣称任意现有术式已经能由分身释放。

复用原版人形模型和原版贴图作基础验证，名字标明“幻影分身·基础”或“普通分身·基础”。不制作、改绘或归档新美术，不做玩家皮肤下载，也不复用 PlayerRenderer 强塞非玩家实体。可用原版 HumanoidModel/ZombieModel 及其原版贴图；两类先以名称和服务端状态区分，外观不冒充水幻形最终效果。若实施确实转向新美术，暂停那部分，另走 xueximod-art-preview；不能把制作新模型藏在基础渲染代码里。

不领取或迁移制作表。保留所有未提交改动。不顺手修复配置、候选缓存、纯计算后端、旧调度器等其他批次。相关文档的“未游戏实测”“额度用尽后未追加复审”等状态全部保留。

## 2. 已确认事实与总裁决

施术包、TechniqueEffect、SkillSegmentRunner、ReleaseBatch.Tally 仍依赖 ServerPlayer。commitNow 只覆盖接触几何；默认锁定、场地影响、主手检查、自疗、动作位移及若干剑机制仍读或修改玩家。BatchCastSession 内部与全部效果并未逐个证明 actor 安全。因此首版不得用 commitNow、buildSegmentRunnerAt 或 DOMAIN_AUTOMATIC 冒充完整分身施术接口。

不得构造 FakePlayer，不得临时挪动或旋转主人，不得让真实玩家充当分身的可移动身体。旧 phantom 包是无业务调用的休眠占位，不改造它来伪称已经兼容；本批用独立 clone 包，不删除旧包。phantom_sword 及剑实体的 phantom 标记不属于本底座。

首版只放行自有的 actor-safe 基础脉冲响应：从分身眼睛出发，向分身当次方向选择一个目标，完成一次正常玩家归属伤害。没有弹丸、附魔、火种、潮印、吸血、武器份额、领域附加、反弹、真剑、宿主组或延迟账本。其伤害结算复用共享权限/伤害裁决，不能直接改血、清无敌帧或伪造成功回执。

ChainScheduler 当前无跨服隔离和容量限制。本批不向它投递任何分身任务，不复用 ReleaseBatch.Tally，不把修复全仓调度器作为本批前置。本批队列、资源收据、冷却和索引都绑定 MinecraftServer 对象身份与一次启动的 session token。

## 3. 可操作闭环

拟新增命令统一在 `/shu clone` 下，所有子命令 requires(2)，必须是在线 ServerPlayer 发起，不接受替别人指定 owner：

- `spawn phantom`、`spawn normal`：在主人当前脚下生成一具驻留分身，朝向取该次服务器看到的主人视线；忽略与主人重叠的实体碰撞，但不能卡进实心方块、越过世界边界或生成在未加载区域。返回完整 actor UUID、类型、费用和剩余响应信息。
- `list`：只读本人的索引，列出本人的 UUID、位置、生命、类型、状态、剩余响应和剩余冷却；不扫世界，不访问别人的清单。
- `respond <actorUuid>`：仅控制本人、同维、仍加载的分身。方向取命令处理当刻主人视线的向量，**原点取分身眼睛**；这是明确的“主动瞄准指令”，不是默认锁定主人目标。分身在该方向的射程内找一个合法目标，阻挡射线截断；没有目标则拒绝，不花魔力、不消费次数。
- `dismiss <actorUuid>`、`dismiss all`：本人范围内幂等撤回，不退款，不作用于旧幻剑或其他实体。

两种分身默认同时具备主动请求与受击反击触发。受击反击只针对本次已被实际接受的伤害来源：分身受伤后仍存活，且来源能解出当前维度的存活 LivingEntity，才尝试预约一次反击。环境伤害照常扣血，不凭空选敌；致死伤先结束，不发临终反击。受击时无目标、目标出射程/被挡、无魔力、冷却中或已有请求，只拒绝这一回，不积攒攻击债务。

respond 命令当场只报告“请求已接受”或即时拒绝，不提前宣称已释放；下一 tick 执行时的拒绝向主动发起者反馈一次。主动方向在请求时冻结，反击目标只存 UUID、执行时重新取其中心；两者原点都在执行当刻从 actor 取得。提交时仅转动 actor 自身朝向以配合显示，绝不转动主人。

两类均有正常生命；“幻影”不自动等于无敌。普通分身不会因为一次非致死受击或一次响应自动消失。幻影响应一旦提交，就耗尽其一次额度，动作结束后销毁；是否真正扣到目标生命不决定退款或恢复次数。未提交的拒绝不消费额度。首版参数是待游戏调优的设计默认，不是从视频推导的事实。

首版可被敌对生物攻击；有限的基础仇恨吸引也必须落地，详见第 8 节。不能只把实体做成可 hurt，却称为已能吸引仇恨。

## 4. 身份与数据合同

拟新增包建议为 `com.bslx.XueXiMod.shu.technique.release.clone`，客户端渲染单独放客户端包。

`CloneKind`：PHANTOM、NORMAL。`CloneLifeState`：ACTIVE、ENDING、ENDED。`CloneResponseState`：READY、RESERVED、COMMITTING、COOLDOWN、SPENT。生命状态和响应状态独立，不用 COOLDOWN 代表“实体还活着”。

`CloneSpec` 为不可变服务器规格，至少含真实来源 techniqueId、kind、LifetimePolicy、maxHealth、ResponseBudget、endAfterBudgetSpent、actionKey、触发策略、站位策略、仇恨策略及相关配置值。PHANTOM 的预算为 ONE，NORMAL 为 REPEAT；次数是结构机制，不开放 copyCount 等配置。首版两者均 STATIONARY，不保存玩家实体引用。

`CloneHandle` 至少含 sessionToken、dimension、actorUuid、actorGeneration。actorGeneration 用来防止撤回后、读档异常或 UUID 碰撞把旧请求送到新实体。外部不能拿裸实体编号当永久句柄。

`CloneActorFrame` 是当次动作快照：ServerLevel 身份、actor UUID/generation、owner UUID、actorPosition、eyeOrigin、单位 look、可选目标 UUID、actionSequence。世界引用只活在当前服务器会话/短事务中；不能进全局结构缓存。主动请求方向与反击目标分别标识来源，不共用主人手动锁定缓存。

`CloneActionRequest`：handle、rootEventId、source（MANUAL/HURT）、sourceDepth、actionKey、请求序号、requestedTick、executeTick、expireTick、方向或攻击者 UUID。只由服务器构造，最多活 2 tick。首版 executeTick 为下一 tick，给受击回调提供重入隔离；不会从存档恢复。

`CloneActionContext`：在线主人、actorFrame、根事件与动作 token、当次 ConfiguredDamage、魔力收据及取消令牌。owner 负责归属和资源；actor 负责身体/几何；action token 负责一次提交；rootEventId 负责来源传播；这些不能混成主人 UUID。

`CloneRuntime` 是每个 MinecraftServer 身份独立的运行态，持有 owner→actor 的索引、actor→运行态、受限请求队列、短期冷却记录。不能用只有 UUID 的进程级静态 map 跨服共用。始终在服务器线程访问，无需并发容器掩盖线程错误。

## 5. 拟新增类与方法职责

以下为需要实现的合同签名，可按工程风格改名称，不可删掉其语义：

```java
CloneSpawnResult CloneService.spawn(ServerPlayer owner, CloneKind kind);
CloneRequestResult CloneService.request(ServerPlayer owner, CloneHandle actor,
                                       CloneTriggerInput input);
void CloneService.onAcceptedHurt(CloneEntity actor, DamageSource source);
boolean CloneService.dismiss(ServerPlayer owner, UUID actorId);
void CloneService.end(CloneHandle actor, CloneEndReason reason);
CloneStatus CloneService.status(ServerPlayer owner, UUID actorId);

ClonePreparedAction CloneAction.prepare(CloneActionContext context);
CloneActionResult ClonePreparedAction.commit();
void ClonePreparedAction.cancelBeforeCommit();

CloneActorFrame ClonePositionPolicy.frame(CloneEntity actor, CloneTriggerInput input);
boolean CloneTriggerPolicy.accept(CloneTriggerInput input, CloneRuntimeState state);
CloneAdmission CloneActionRegistry.admit(String actionKey, CloneActionCapabilities needs);
```

`prepare` 不产生伤害、不生成世界实体、不扣费、不改次数。它在执行当刻重新验证身份、世界、存活、方向、射程、阻挡与权限，并冻结这一次报价和目标。`commit` 不再二次读伤害倍率或重新扣款，最多进入一次不可逆动作；`cancelBeforeCommit` 幂等，只作用于还没有越过提交线的收据。

`CloneEntity` 建议继承 PathfinderMob，空 goal/target goal、无自主寻路/攻击、无自然刷怪、无拾取/背包/装备掉落/经验/繁殖/转换。使用原版生命和受伤机制；禁止调用 owner.attack、玩家挥刀附魔路径、owner.setDeltaMovement。实体的重力/环境受伤保留，STATIONARY 表示无主动移动，非锁死坐标或无敌。受到击退后可以离开出生点，未来移动策略才负责归位。

受击入口也要保护 owner 权限：来源为 owner 本人或任意 CloneEntity，或正处于 CloneSourceScope 的响应来源时拒绝；来源为别的 ServerPlayer 时，要求其能伤害 owner（canHarmPlayer），不得通过攻击分身间接绕过队伍保护。非玩家且与 owner 同队/属于 owner 的可识别驯养物拒绝，其他生物与环境伤沿用原版伤害。先执行这些检查，再调用原版 super.hurt；仅其接受且 actor 仍活着时通知 onAcceptedHurt，不在原版判定之前排反击。玩家允许互伤并不意味着响应可以跳过同样的出伤权限。

`ClonePulseAction` 是第一版唯一已登记 action；它不接受 Skill 或任意 TechniqueEffect。`CloneCommands` 只做命令解析及身份获取，业务全部进 CloneService。`CloneLifecycleEvents` 管启动、停止、世界卸载、离线、死亡、换维和实体移除。`CloneBalance` 只声明/读取真实基础 Technique 的字段。`CloneRenderer` 只消费实体跟踪状态。

拒绝结果必须有稳定 reason code 和可读说明：WRONG_OWNER、WRONG_SERVER、WRONG_DIMENSION、ACTOR_GONE、OWNER_UNAVAILABLE、BUSY、COOLDOWN、BUDGET_SPENT、QUEUE_FULL、CAP_REACHED、NO_TARGET、BLOCKED、FORBIDDEN_TARGET、INSUFFICIENT_MANA、UNSUPPORTED_ACTION、UNSUPPORTED_CHAIN、DEVELOPMENT_ENTRY_ONLY、INVALID_GEOMETRY、INVALID_QUOTE。手动命令反馈一次；受击自动触发不每 tick 刷聊天，供状态/采样计数查询。配置转换得到超限/非有限金额时在扣款之前 INVALID_QUOTE，不能让数值回绕成免费。

## 6. 一次响应、扣费和失败的原子边界

所有状态变更在服务器线程串行进行。“原子”指一个动作的收据/状态契约，不是为实体加 synchronized。进入任意可能回调外部代码的位置前先写好重入门。

1. 请求到达先检查 handle、owner、生命周期、响应状态、冷却、次数和硬容量；READY→RESERVED，挂一条请求。此时不扣钱、不减一次额度。每个 actor 最多一条；同 tick 主动/受击冲突采用先被接受者，后到的返回 BUSY，不覆盖先到者。
2. 下一 tick 从独立有界队列取出，在 expireTick 前执行。重新验证 owner/actor 同世界且存活、代次、目标资格、距离、视线、能力准入。任一失败取消预约，恢复 READY（若实体已终止则不恢复），不记冷却，不扣钱。
3. prepare 得到有限非负的已配置伤害、整数魔力价格和已配置冷却。先检查全部报价范围和权限，再 `ManaPaymentReceipt.charge(owner, quotedCost, tellPlayer)`；返回 null 即扣款失败，恢复 READY，不消费次数。成功保存这张真实收据，不拿 MagicPool.lastReceipt 的全局最近值代替它。
4. 把状态置 COMMITTING、占用本次 action token、对 PHANTOM 标记预算已花、写冷却起点，然后才进入世界伤害调用。这个边界之后，命中被无敌帧/事件取消、实际为 0、目标状态在回调中变化，均视为已出手，不退款、不恢复次数。不能通过重复伤害事件白嫖重发。
5. commit 结束：NORMAL→COOLDOWN，到期回 READY；PHANTOM→SPENT→ENDING→ENDED。配置冷却乘全局后为 0 仍不得一 tick 无限连发：每 actor 每服务器 tick 最多提交一次，是调度硬边界，不是额外收费冷却。
6. 提交线之前的失败，调用该张 ManaPaymentReceipt.refund(owner) 退未提交原额且最多一次；不得按新的配置重算。退款沿用原玩家池/原身份法杖和容量规则，满额部分丢弃，原杖已不在身上时其份额不改退玩家池；不以 MagicPool.restore 冒充跨来源退款。提交线之后遇异常保守视为已提交，finally 释放预约与作用域、完成额度/结束策略，不退款；记录可定位的一次错误，业务异常不伪装为成功。不得盲目重试一个可能已经造成伤害的动作。

生成也有事务：先检查位置、所有硬容量、owner 状态和共享生成冷却；预留一个容量槽，报价并扣费，再 addFreshEntity。加入失败则撤销槽位/索引，按原额退款，不记冷却。成功加入与索引发布完成后才算生成提交；后来的正常死亡、撤回、卸载或离线不退款。加入回调的重入由预留 ticket 拦住，不能两次生成各自认为还有最后一个槽。

所有冷却/过期使用本次 Runtime 的单调 long tick 计数，不使用可被 /time set 改动的世界时间，也不把服务器 int tick 回绕后的负数当无限寿命。ENDING/ENDED 优先于任何响应 finally；外部伤害回调若令主人死亡或实体被移除，finally 只清理，不把实体恢复成 READY/COOLDOWN。提交线前回调使句柄失效则取消；线后失效按已提交结束，不二次退款。

生成冷却保存在 owner 的分身服务作用域中，两种生成共用一条截止时间；成功时用本次来源 Technique 的 cooldownTicks。响应冷却属于各 actor，不借用玩家原施术链 CastCooldown；魔力通过主人同一个 MagicPool 的正式收据支付，沿用现有先法杖储量、后玩家池的顺序。首版不读取主手来计算伤害/资格、不加武器伤害、不用法杖减冷却、不授予术式经验；收费器内部读取法杖是已有资金来源规则，不是复制装备或主手附魔。不影响玩家正常装配术式的游标/按住状态。

## 7. 基础脉冲的几何、权限与账本边界

基础脉冲是瞬时单目标响应，默认原始固定伤害 2、射程 8 格。主动取主人提供的服务器视线方向，反击朝当前攻击者中心。二者都从 actor eyeOrigin 做方块阻挡和实体 AABB 相交；手动按最近射线交点选一个合法目标，距离相同用 UUID 排序作稳定决胜，不能取主人眼睛附近的目标。

所有目标必须存活、非旁观、非主人、非任意 CloneEntity，且通过现有 DamagePermission。其他玩家还要通过主人的 PvP/team 许可；不能靠分身绕过主人不能伤害的人。对同主人的宠物按既有权限规则，并在新入口显式排除主人及其可判定的驯养物。空间查询用 LivingCandidateQueries，之后仍实时验证目标，缓存不能保存允许攻击的结论。

金额仅由 `BalanceService.damageContribution("clone_response_pulse", RawDamage.of(rawFixedDamage))` 形成一次。它不把 ConfiguredDamage.amount 再当 RawDamage，不再乘全局倍率，不借宿主倍率，也不混入主人攻击属性、法杖属性或场地加成。合法配置 0 是一次有效响应，可出动作/消耗预算但不强造正伤副作用。

第一版没有多份伤害汇总，没有 ReleaseBatch，也没有 Tally。每 actor、每 action 都独立 token，单个目标最多一个伤害调用；不能共享主人级 Tally，也不能把两个普通分身打同一目标的命中合并。保留原版无敌帧，两具分身短时间同时出手不保证伤害相加，这是首版明确行为。

伤害归属是在线 owner 的 playerAttack；几何和定向效果不能根据 owner.position 再计算。第一版不额外施加自定义击退，原版 hurt 自己的攻击者/仇恨/击退表现仍可能依据 owner，这一点不能宣称已变成完整 actor 伤害源。真实调用固定为：先 `DamagePermission.allows(owner, target)` 及本底座附加友方检查，再 `TechniqueHits.classifyRespecting(target, owner, configured.amount(), TideMarkSuccess.NONE)`。不用 classifyRoot：它自动加领域且绕无敌帧；也不使用其布尔包装来决定退款，因为布尔只表示 grantsSeeds。返回 HitVerdict 仅用于记录命中结果，不回滚已经提交的动作、不发火种。NONE 的现有潮印观察作用域没有潮湿/水势发放。第一版 DamageSource 的 direct entity 与 causing entity 都仍是 owner；以后要完整 actor direct source 时另加有明确契约的共享伤害重载，不在本批改所有旧出口。

## 8. 有限仇恨吸引

`CloneDecoyPolicy` 首版启用最低限度诱敌：只考虑同维、已加载、8 格内、可见且符合现有攻击许可的原版 Enemy 类 Mob；不拉中立生物，不接管 boss，不改其他玩家仇恨。仅当该 Mob 当前无目标，或目标正是 owner，才可 setTarget(actor)；已经追别人或另一具分身的保留。每 actor 同时最多记 8 个被其设置过目标的 Mob UUID，每次至多检查 32 个候选，最多新吸引 8 个，超过即截断。对常用僵尸/骷髅的实际目标保持行为由用户测，不能宣称适配一切模组 AI。

首版白名单收窄为原版僵尸族与骷髅族的常规 Mob（按现有原版基类判定），并同时要求 Enemy；不靠遍历注册表猜所有模组怪物，更不通过 boss 血条猜实体种类。未来其他怪物要显式增加适配。这一收窄保证本版诱敌有可测消费者，也避免临时发明全模组 boss 判断。

每 20 tick 轮到一次查询，按 actor 序号错开；全服每 tick 至多 4 次此类查询，按轮转游标公平处理，不能每 tick 全体扫一次。使用 LivingCandidateQueries.entityQuery；包围盒仅限配置半径，不做 getAllEntities。候选返回后再次查 Mob.canAttack、视线、目标和生命周期。配置半径为 0 时完全跳过诱敌查询，可关闭这个行为。

结束时，只遍历这具分身记录的至多 8 个 UUID；实体仍加载且其 current target 仍等于 actor 才清空。不能恢复很久以前的目标、替 AI 扫描新目标或清掉已经转移到别人的仇恨。索引不长期持有这些 Mob 强引用。敌人自己改变目标后移除相应租约，之后可由 AI 自行寻敌。

候选复用 helper 内部真实查询的列表大小仍受世界实体密度影响；上述约束保证调用频率和业务处理数量有界，不承诺恶意堆叠环境下底层世界查询耗时恒定，不伪称缓存 256 上限就是世界查询返回上限。

## 9. 能力准入与旧链兼容

新增 CloneActionCapabilities 描述 actor 原点、actor 方向、owner 资源、无主人身体副作用、无主人主手写入、无递归广播、无未隔离延迟任务等能力。默认未知 action 拒绝。首版仅 ClonePulseAction 全部满足；扩展必须显式注册适配器，不能凭 TechniqueType、伤害大于 0 或名字相似自动放行。

现有玩家链保持原行为。基础服务 Technique 不得进入普通装配执行，准入必须在报价/扣魔/冷却/使用计数之前完成；不能在 effect.execute 末尾才拒绝。开发目录能力不是 ChainRole，不改变旧链索敌→弹道→规则→攻击的结算顺序。按通用能力过滤，不写三个 ID 的 switch。即使旧存档、手写 NBT、外部插件把它塞进 Skill，也要整链早拒绝，不能只跳过该段后让其余段执行。

补查已确认没有现成的隐藏能力，因此本批必须最小新增 `TechniqueAvailability`（PLAYER、DEVELOPMENT_ONLY）及统一 `TechniqueAccess`。Technique 旧构造保持兼容，默认 PLAYER；三个新 Technique 用显式 DEVELOPMENT_ONLY，不能通过复杂度特别大、空效果或两个已移除术式 ID 的旧特判实现隐藏。TechniqueManager.getAll() 保持全部注册项供 BalanceCatalog 封存/同步；新增玩家可用目录方法，给显示、学习与获得路径使用。该能力在注册后不可变，客户端与服务端共同拥有，不能由客户端包/Skill NBT覆盖。

本次必须覆盖如下入口，普通术式只增加“非 DEVELOPMENT_ONLY”检查，不顺手改旧学习/解锁机制：

- PlayerConnectionHandler 的复杂度 0 自动学习使用玩家目录；PlayerShuData.learnTechnique 对已登记且 DEVELOPMENT_ONLY 的条目拒绝写入（保持其原有签名，外层提供提示），以覆盖技能书等间接学习。读旧 NBT 不需要破坏性清洗，但这些历史条目不获执行资格。
- ShuCommands 的 list、learn 单个/全部、reset 的全学会、stone、book write：列表过滤；显式指定开发项返回 DEVELOPMENT_ENTRY_ONLY，不给空石/空书、不修改学习状态。`/shu clone` 是单独受权入口，不能通过管理员 `/shu learn` 绕过开发能力。
- ImprintDeskMenu（含客户端无学习缓存时的复杂度 0 退回）、ShuManagementMenu 使用玩家目录；ImprintActions 服务端刻印先校验能力。
- SkillAssemblyPacket 两条服务端装配分支、SpellAssemblyMenu.rejectReason/mayPlace 均统一拒绝开发项；法杖 StaffSkills.castRefusal 同样检查能力。库存/收纳匣可以保留无法使用的老字符串，不为隐藏能力删用户物品；插入装配和执行时明确拒绝。
- SkillUsePacket.offerNow 在 skill 已解析后、HeldSwordGate/冷却忙碌处理和任何使用计数之前检查整链；executeSkill 顶部再次防御检查，必须位于 BatchReleaseCompiler/BatchCastSession、纯附魔、CastExecutionSession、普通预扣四种分支之前。失败不推进游标、不写学习/升级、不保留按住重试。Skill.fromNbt 保留登记 ID 的行为可保留，由执行准入拒绝，不能静默剔除坏段后放行链的剩余部分。

真正给命令使用的入口登记 `CloneTechniqueRegistry`，把三个实际 Technique 分别映射到生成 profile/基础 action；服务据登记对象消费，不根据术式 id 在旧链 runner 里分叉。首版不通过旧 TechniqueEffect 执行业务；若给旧 TechniqueEffectRegistry 同时登记防御适配，该适配应明确抛出开发入口误用错误，不能空 lambda 假成功。唯一正常执行路径是已验证的 CloneService 与 CloneTechniqueRegistry 所登记的真实消费者。创建者只能登记上述 actor-safe 动作，插件注册一个名称不会自动取得资格。

首版兼容范围逐项如下：

- 普通玩家原有装配施术：保留，不订阅其全部释放进行同步广播。
- 两类驻留分身＋基础脉冲：实际支持，独立资源收据/动作次数。
- 任意现有攻击、附魔链：拒绝 UNSUPPORTED_CHAIN；没有全部 effect 的 actor 审计证据。
- 锁定/落点/追踪/垂落/交错：暂不接入分身脉冲；其主人几何不能直接复用。首版自己的单目标射线不修改玩家标记。
- 一分为三/双重/多重/步进/命中接续/宿主/领域自动：拒绝；需要每动作独立调度、Tally 与取消边界后再开。
- 冲刺/自疗/自身状态：拒绝；现有 owner 身体副作用未分离。
- 主手附魔、火种、潮印：拒绝；不复制资源、不会消耗或写入主人印记。
- 真剑、剑来、幻剑：拒绝；旧 sword_entity 不是分身 actor。
- FakePlayer、跨维响应、离线 owner：拒绝，不改为无主 magic 伤害继续打。

服务接口可以返回明确拒绝；不能暴露一个看似成功的空 adapter。后续完整水幻形及同步术式需要单独授权与能力适配，不由本次名单自动扩大范围。

## 10. 配置合同

沿用 schemaVersion 1 的 techniques 目录；不新增伪造 service ID，不修改全局倍率集合，不新建平行平衡文件。真实注册三个基础 Technique：`clone_phantom`（生成幻影）、`clone_body`（生成普通分身）、`clone_response_pulse`（真正响应动作）。它们仅供开发服务入口使用，但都必须有真实消费者和明确的开发入口标记；不允许注册“占位术式”只骗过 seal。

`clone_phantom` 与 `clone_body` 的字段分别声明，默认相同：

- `manaCost`：10 魔力，整数 0..1000000；生成事务报价使用 BalanceService.manaCost，只乘一次 global.manaMultiplier。由标准声明生成，不在专项重复。
- `cooldownTicks`：20 tick；沿用标准冷却边界和全局缩放规则，默认非 0 时字段 0 非法；生成成功时写 owner 共享生成冷却。标准声明，不重复。
- `lifetimeTicks`：0 tick，整数 0..1200000；0 表示没有自然到期，正数从生成提交的服务器 tick 起算；读取一次形成 LifetimePolicy。这里的 0 是专门声明的无限期语义，不能误用只许正 tick 的边界工厂。
- `maxHealth`：20 生命值，有限数 1..1024；实体生成设置最大生命及当前生命，后续不每 tick 读配置重置。声明 numberParameter、BalanceBound.inclusive(1, 1024)、BalanceUnit.COUNT，描述明确“生命值（非实体个数）”；不假称已有 HEALTH 单位。
- `decoyRadius`：8 格，有限数 0..16；0 禁用诱敌。生成时冻结，实际被第 8 节空间查询消费。该上限小于公共尺寸最大值，是本功能的局部预算边界。

`clone_response_pulse`：

- `manaCost`：2 魔力，整数 0..1000000；每次实际提交前由主人支付一次，自动反击同价；拒绝不收费。
- `cooldownTicks`：20 tick；标准规则，成功提交时设置该 actor 响应冷却，不因 miss 退回。
- `damageMultiplier`：1，范围 0..1000；这是有真实固定伤害的基础 Technique，必须进入内置有伤害贡献声明路径；全局伤害倍率仍只一次。
- `fixedDamage`：2 生命值，有限数 0..1000000；是原公式输入，实际出手时读取后传 RawDamage.of，再作一次 damageContribution。原始 Technique.baseDamage 仍为 2，用于结构声明/说明，不能根据已经配置的 0 撤销攻击身份。魔力默认保持 2，不随 fixedDamage 或倍率重算。
- `range`：8 格，有限数 0.1..32；prepare 时读取，同一次动作的候选盒、方块射线和距离检查使用同一个快照。

所有字段代码默认明确且只有一个定义来源，注册前不读取有效配置。专项从现有 ModTechniques.declareSpecialBalances 接入，标准费用/冷却/伤害不重复声明。lifetimeTicks 用 integerParameter + BalanceBound.count(0, 1200000) + BalanceUnit.TICKS；decoyRadius 用 numberParameter + inclusive(0,16) + BLOCKS；fixedDamage 用 numberParameter + inclusive(0,1000000) + COUNT（描述为生命伤害）；range 用 numberParameter + inclusive(0.1,32) + BLOCKS。真实 builder 允许自定义字符串键。新字段只是参数声明，不修改 BalanceService 算法。客户端显示用同步后的同侧快照，命令 status 用服务端快照；基础 Technique 的说明明确入口、默认不装配和本局有效值。

无新增总分身数量、队列容量、复制次数、轮询节拍、去重策略、允许链类别或 ChainRole 配置。它们是安全/结构约束，留代码常量。可调寿命/生命/诱敌半径属于真实生成 Technique 的行为，非术式通用服务参数不塞入这三个 ID 冒充消费者。

世界加载后配置不热重载；改磁盘下次开世界生效，缺项回代码默认，非法字段回自身默认，损坏原文件保留。实体生成参数按生成快照固定，响应参数按提交时快照固定，均不将已配置金额二次换算。世界已有 JSON 不自动写回/补键，旧覆盖值不得丢失。命令创建成本和响应伤害的默认数只是本次新功能默认，不修改任何旧术式默认。

## 11. 生命周期、NBT 与服务器隔离

第一版选择**会话内长期存在、非持久化分身**。默认 0 自然寿命可一直驻留，只要 owner 在线存活同维且实体所在区域保持加载。这里“长期存在”不等于跨退出/读档恢复。不给区块 ticket，不强制加载，不为了维持分身跟随玩家跨维。

实体类型 noSave，且防止无有效生成 ticket 的原版 `/summon`、结构/NBT 注入创建可工作分身。读到未知版本、缺 owner、无 sessionToken 或无来源规格时立即安全 discard；不根据 NBT 恢复剩余次数、队列、冷却、扣款收据或魔力。需要实现原版抽象读写方法时，只写最小版本/诊断信息，读取结果不能进入 ACTIVE。不要保存 ServerPlayer、ServerLevel、目标活引用。

以下都走同一个幂等 end：自然到期、生命归零、主动撤回、一次额度耗尽、owner 登出/死亡/换维、实体区块卸载、世界卸载、服务器停止。end 先标 ENDING 并取消 generation token，使请求失效；撤掉索引/队列/仇恨租约，再 discard，最后 ENDED。重复 remove/leave/death 回调不退款、不重复记数。

通过实体加入/移除及服务显式生成/结束维护 owner 索引，拒绝重复 UUID；不靠扫描 level.getAllEntities 找回来。正常生成前先预留 ticket，加入回调核对它，发布失败整笔撤销。按玩家 UUID 查玩家列表仅用于当前会话即时验证；不会恢复上一个 MinecraftServer 对象上的 actor。

生命周期事件即时清理，另外每 actor 错开每 20 tick 的 owner 兜底验证，处理第三方遗漏事件；真正执行响应时每次重验。服务停止先停止接受请求，取消队列，结束所有本批实体并清引用；启动总是新 sessionToken 和空容器。停服后任何遗留回调见 session 不同立刻返回，不在新服扣钱、伤害或退款。

owner 死亡的 PlayerEvent.Clone 与“战斗分身”不同；该事件只清老 owner 的运行态，不复制分身。切换维度导致 owner 对象变化，也不得借 UUID 一致把旧实体认领回来。首版不记录跨服退款债务，因为未提交收据只活在同一个同步服务器事务中，队列本身从不预扣款。

## 12. 线程、同步与无递归

服务、索引、钱、目标选择和 hurt 一律服务器线程。异步线程调用服务直接拒绝/断言错误，不借 ConcurrentHashMap 允许跨线程读取世界。客户端无选择目标、扣费、伤害或自动反击权力。

首版不新增 C2S 自定义包：命令本身已由服务端验证权限及 sender。实体采用 Forge 原生实体生成/跟踪机制，注册 attributes 和 renderer；仅同步 kind、owner UUID（需要时）、生命/响应展示状态及动作序号。变化时才 set 同步字段，不每 tick 发送完整状态。动作序号变化触发一次原版挥手/受击表现；客户端表现绝不反过来触发服务器动作。

CloneSourceScope 只包围本次同步脉冲提交，带 origin=CLONE_RESPONSE、rootEventId、depth=1；try/finally 严格恢复嵌套前作用域。触发器拒绝 CLONE_RESPONSE 或 depth>0 来源，且所有分身响应禁止以任何 CloneEntity 为目标。首版没有延迟子攻击，所以不会在异步回调里丢失来源标签。不能只做“当前 tick 已触发”来冒充无递归保证。

不监听 owner 的所有出招广播，不从分身响应调用 SkillUsePacket.offer，不调用 HoldRepeat，不发新的同步事件给其他分身。后续同步模式只能消费 origin=PLAYER_MANUAL 的根事件，再 fan-out 成同 root、depth=1 的有限动作；子动作永远不能再 fan-out。

## 13. 数量、队列、空闲路径与现有优化

结构硬上限：每 owner 最多 4 具，两类型合计；每 server 最多 128 具；每 actor 最多 1 条未执行请求；服务器请求队列最多 128 条；每 tick 最多提交 32 个响应，FIFO 公平处理，超过 2 tick 未执行则取消预约不收费；每 actor 每 tick 最多一次提交。生成不自动淘汰旧分身，超限明确拒绝。

仇恨租约每 actor 8 条，全服诱敌每 tick 4 次，owner 运行记录最多 1024 条。owner 冷却到期且无 actor/请求时移除空记录；离线和停服清理。若 owner 记录上限仍满，新建明确拒绝，不淘汰有活跃事务的主人。动作递增序号为 long，并在理论溢出前安全结束 actor，不接受回绕重放。

零实例且无冷却记录/队列时 Runtime tick 首行空判断返回，不查询玩家列表、不扫世界、不分配候选表。不为每个玩家注册永久轮询器。驻留实体仍有原版 Living tick，不能声称“完全不 tick”或“零开销”；本批自己的额外工作仅为短状态判断和错开的周期检查。不得在空闲路径反复拷贝技能、读主手 NBT、编译链或构造轨迹数组。

候选查询：响应 targetQuery、诱敌 entityQuery，使用同 tick 同 ServerLevel helper，后续权限/距离/视线自己再验。Living 分身移动会触发原有候选缓存失效，不能为追求命中率绕过 setPosRaw/setBoundingBox 等既有失效路径。调用方不再次包 ENTITY_QUERY/TARGET_QUERY 计时。

结构计划缓存：首版自有响应没有 Skill 链，不调用包内 CastPlanCache，也不复制一个分身结构缓存。新增基础开发定义默认不声明 supportsStructuralPlanCache；只有以后真正接入了结构规则才考虑。不得把 owner 当前目标、武器、费用、位置、actor 状态或配置最终金额塞进全局结构缓存。既有 capabilityStamp/ChainStructureEpoch 规则保留；若新增影响可执行性的定义能力，使用现有注册变更纪元，并在每次执行仍检查能力，不缓存授权结论。

纯计算：基础射线和原版碰撞保持 Java；不接新的 JNI，不凑 8 条批处理、不延迟动作等待批量。不增加 compute backend 的模组配置，保留现有 JVM 参数机制及 Java 回退。以后复杂运动只把纯数值送入已有 compute 边界，目标、世界查询、随机和权限仍留服务器原时点。

性能记录：新增 CLONE_TICK、CLONE_RESPONSE 业务耗时，CLONE_SPAWNS、CLONE_ENDS、CLONE_RESPONSE_REQUESTS、CLONE_RESPONSE_COMMITS、CLONE_RESPONSE_REJECTIONS、CLONE_DECOY_PASSES 计数，全部走现有 begin/end/add 门控及服务端采样线程规则。状态查询的 loaded count 用 map.size，不把更新次数叫实体数。先只扩 summary 的枚举汇总，不修改 ticks.csv 既有列合同。关闭采样不读 nanoTime、不为统计构造列表，不加每 tick 日志；耗时为 inclusive，不能相加当总 CPU。不承诺 FPS/TPS 收益。

## 14. 实施切块、所有权与交付顺序

主会话派发前登记唯一作者及具体绝对路径。推荐并行独立新文件，整合共享文件串行：

1. 核心作者 A：新 clone 服务包的实体、规格、状态、索引、请求、生命周期及事务。以本文合同为依赖，不修改 ModTechniques/SkillUsePacket/渲染注册等共享文件。
2. 响应作者 B：ClonePulseAction、能力准入与配置规格新文件。与 A 共享接口先冻结；不能两人改同一个接口文件。只新增自己的文件，必要 shared damage 适配由整合者统一落点。
3. 整合作者 C：真实 Technique 注册、开发入口通用能力及过滤、命令挂接、实体/属性/客户端 renderer 注册、语言提示、性能枚举。公共文件一律只由 C 写，不与其他聊天同时占用。原版渲染类可由 C 独写，也可明确单独移交，不默认共享。

在 A/B/C 之前主会话还须确认其他同工作区聊天的写入范围；已有活动聊天与旧调查时间戳不能证明工作区冻结。不因并行优先而同时修改 ModTechniques、XueXiMod、公共配置目录或语言文件。

先登记 DTO/合同及文件所有权，再实现核心和单动作，再整合可操作命令及能力过滤。若并行接口依赖无法明确，宁可按依赖串行，不交付空返回冒充接口已满足。每路 Cursor 显式 `--model grok-4.7-high-fast`，不降档、不由主会话接管 Java。

每位作者报告改动路径、真实行为、配置字段及消费者、未做事项、阻塞和停止写入。改 Java 按流程运行命名门禁，只处理本次动过文件的报告；不清理无关旧问题。作者没有统一编译授权，不自行 build/jar/runClient，不改表，不提交。

额度内正式实现复核必须等本阶段相关作者全部最终反馈并停止写入后集中做，本批最多 3 轮，实现返修复审仍累计。设计本轮计 1/2，不为补充事实调查另加轮。额度用完不自动让 Cursor 或别的会话继续复审，也不把代码编译通过写成复审通过。

最终编译由主会话指定的 Cursor 单一执行者，在所有相关写入收束后运行 `.\gradlew.bat compileJava --offline --no-daemon`。记录作者释放、最后写入时间、共享文件归属及其他聊天是否仍占用；可在前后对 src/main 快照核对编译确实覆盖最终字节。任何相关写入再次发生，旧编译不能冒充最终编译。失败交回作者后重收束再编译，不边编边改。默认不建独立测试阶段、不生成测试任务、不打包、不启动游戏。

完成条件：两类型、一个真实响应、一次/多次语义、基础诱敌、生命周期、能力过滤、实配消费者、实际命令、渲染和最终 compileJava 均有交付结果；不以无消费者的扩展接口代替。不要求尚未授权的完整水幻形、普通玩家装配、同步 AI 或新美术成为本批前置。

### 14.1 文件所有权清单

所有路径以 `D:/alianximod/` 为根。下列为派发前应登记的精确分组；这是设计授权建议，实际取得共享文件写入窗口仍由主会话协调。未列出的共享文件不得作者自行扩写，确需补挂接时先向主会话报告路径/原因，由同一个整合者认领，不另开设计审核来处理纯路径协调。

A 独占新目录 `src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/`，至少包括：CloneKind.java、CloneLifeState.java、CloneResponseState.java、CloneEndReason.java、CloneSpec.java、CloneHandle.java、CloneActorFrame.java、CloneTriggerInput.java、CloneActionRequest.java、CloneActionContext.java、CloneResults.java、CloneRuntime.java、CloneRuntimeState.java、CloneService.java、CloneEntity.java、CloneLifecycleEvents.java、CloneDecoyPolicy.java、ClonePositionPolicy.java、CloneTriggerPolicy.java、CloneSourceScope.java。小结果类型可归在 CloneResults 的嵌套 record/enum，避免同一文件多作者。A 不改其他组。

B 独占新目录 `src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/action/`（CloneAction.java、ClonePreparedAction.java、CloneActionResult.java、CloneActionCapabilities.java、CloneActionRegistry.java、ClonePulseAction.java），以及 `src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/CloneBalance.java`。B 在该文件提供 declareSpecials/读取规格的普通方法，C 只调用，不修改它。A/B 若要变更本文 DTO 字段，以先交接接口再改消费者方式串行，不能同写。

C 独占新文件：`src/main/java/com/bslx/XueXiMod/shu/TechniqueAvailability.java`、同目录 TechniqueAccess.java；`src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/CloneTechniqueRegistry.java`、CloneReleaseDefinition.java、CloneEntityTypes.java；`src/main/java/com/bslx/XueXiMod/command/CloneCommands.java`；`src/main/java/com/bslx/XueXiMod/client/renderer/CloneRenderer.java`（若仓库客户端目录约定不同，由 C 一次选定并报告，不让 A/B并写）。

C 独占既有共享文件：

- `src/main/java/com/bslx/XueXiMod/XueXiMod.java`。
- `src/main/java/com/bslx/XueXiMod/shu/Technique.java`、TechniqueManager.java、ModTechniques.java。
- `src/main/java/com/bslx/XueXiMod/shu/balance/BalanceCatalog.java`（仅把新基础脉冲加入有伤害宿主声明，不重审/重写旧配置算法）。
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseDefinitions.java`（新定义挂接）。
- `src/main/java/com/bslx/XueXiMod/command/ShuCommands.java`。
- `src/main/java/com/bslx/XueXiMod/event/PlayerConnectionHandler.java`。
- `src/main/java/com/bslx/XueXiMod/gui/ImprintActions.java`、ImprintDeskMenu.java、ShuManagementMenu.java、SpellAssemblyMenu.java。
- `src/main/java/com/bslx/XueXiMod/network/SkillAssemblyPacket.java`、SkillUsePacket.java。
- `src/main/java/com/bslx/XueXiMod/item/StaffSkills.java`。
- `src/main/java/com/bslx/XueXiMod/debug/performance/PerfMetric.java`、PerfCounter.java。
- `src/main/resources/assets/xueximod/lang/zh_cn.json`、`en_us.json`（仅新增本批真实提示，保留原键；若资源不存在不为凑清单新建无用翻译）。

C 同时独占 `src/main/java/com/bslx/XueXiMod/player/PlayerShuData.java`；`src/main/java/com/bslx/XueXiMod/gui/ImprintDeskScreen.java`、ShuManagementScreen.java 中若存在直接 getAll/复杂度 0 fallback，也只由 C 接到统一玩家目录。文件位置已通过文件名清单定位，设计者没有读取源码。它们属于第 9 节已裁决的过滤，不归 A/B。原有 TechniqueHits、DamagePermission、MagicPool、ManaPaymentReceipt、ChainScheduler、CastPlanCache、纯计算 JNI、候选查询实现和旧 phantom 包均不在本批默认写入范围；只调用既有 API。CloneEntityTypes 新建本批自己的 DeferredRegister 并由 XueXiMod 挂接，不需要为此改旧 ReleaseEntities。

## 15. 用户游戏实测清单（尚未执行）

前置：由用户自行使用最终源码对应的开发实例，命令权限 2，主人有足够魔力；不要求手持剑/法杖。`/shu clone list` 可取得 UUID。命令开发入口不授予普通玩家新解锁内容。

1. 分别 spawn phantom、spawn normal；应在原地看到两具标明类型的原版人形验证模型。主人移开后它们不跟随、不搬动主人；等待超过一次响应冷却仍存在。无自然期限默认不会自动变成数秒消失的攻击体。
2. 让主人与分身相距数格，主人朝一个从分身可见的目标执行 respond。伤害从分身眼睛的射线选敌，主人位置前的另一目标不应误中；隔墙拒绝/找不到合法目标时不耗魔、不耗次数。脉冲一次只选择一个目标。
3. 幻影有效提交一次后消失；重复 UUID 请求返回 ACTOR_GONE。普通分身有效提交后仍在，冷却内请求被拒，冷却后可再次出手。配置伤害为 0 后重开世界仍应消费一次响应而不打出正伤。
4. 让僵尸在诱敌范围内原本追主人或无目标，应可转向分身。其他玩家正在被追时不应强行抢仇恨；中立生物不应普遍被激怒。撤回分身后不应留下由本服务强绑的失效目标。
5. 分身被敌人非致死命中：下一 tick 尝试一次反击；无魔力或冷却时不存队列等以后补打。普通分身不会因这一次非致死命中自动消失；致死伤害直接结束且无临终反击。主人攻击本人分身按保护规则不触发反击。
6. 同 tick 主动与受击竞争，同一幻影最多一个响应；两具普通分身各有自己的冷却/次数。短时间打同一目标保留原版无敌帧，不期望强行相加，不消耗/写入主人主手附魔，不产生火种/潮印。
7. 换维、登出、死亡、卸载分身所在区块、退出重开世界、同 JVM 进入另一世界：旧分身和请求全部失效，不重生、不离线打人、不在新服扣费，不扫描全实体恢复。
8. 连续生成到每人 4 具，再生成明确拒绝且不扣费。撤回一具后容量恢复；撤回 all 后 list 为空。普通旧术式与旧幻剑仍按原入口工作。
9. 修改世界 serverconfig 的上述真实字段，重开世界：生成生命/期限/诱敌半径、响应费用/冷却/射程/伤害分别生效；单改伤害不改变费用。全局和本术式 damageMultiplier 同时为 2 时，原始固定 2 应形成 8 的配置金额，不是再次乘成 16/32；实际扣血还受原版减伤/无敌帧影响，选无额外减伤目标比较。
10. 开发 Technique 不可通过普通 learn/give/装配形成可收费的空术式；人为构造含它的 Skill 应在收费、冷却和游标修改前整体拒绝，并给明确提示。普通旧 Technique 不受此过滤影响。
11. 若用户要看性能，自行 `/shuperf start 200`，分别零分身与若干分身场景，stop/export；新增计数按含义出现，不把观测差值宣称为普遍性能收益。未启采样不刷逐 tick 日志。

每项反馈留待用户填写。编译、静态审查、原版模型复用都不能代替上述运行期验证，尤其实体跟踪、Mob 目标保持、权限事件和同 JVM 重开边界。

## 16. 后续扩展路线（本次只保留接口）

后续第一步是 actor-safe 单术式适配：把几何提供者、身体动作接收者、资源 owner、伤害归属、主手只读快照和可写状态分别列能力，逐个证明后登记。默认旧效果拒绝，不能一次宣布所有 ATTACK 兼容。需要多份/延迟攻击时，新增按 session/actor/action 隔离且可取消的调度与账本；不能把当前无隔离 ChainScheduler 原样搬进来。

完全同步规则将来消费玩家根事件，先做各分身能力与资源预算准入，再产生有限子动作。配合规则将来可选择“驻留施术”“移动”“背向”“横纵交叉”等站位/动作策略。二者共用当前的触发、动作和站位接口，互不意味着复制主人位置、伤害或资源。

持久化是另一项明确能力：需要版本化 NBT、离线 owner 策略、区块重新加入时索引与数量占用、冻结配置值、动作恢复禁令和所有权代次。首版 noSave 不是漏做恢复；未经新设计不能打开 save 然后指望索引自行补齐。完整水幻形再定义受击结束/反击先后、吸引范围和表现，不从本基础的 20 生命默认推导最终术式平衡。

## 17. 最终事实收口与实施裁决

必读依据为本批 read-casting-final.md、read-performance-config-final.md、read-damage-followup-final.md、read-config-followup-final.md、read-availability-final.md、read-registration-followup-final.md，以及配置平衡、候选查询、术式链计划缓存、纯计算后端、性能记录说明和统一流程。六份只读报告均已完成并停写；全部补查是本次设计第 1 轮内的事实采集，没有另开正式复审。

共享伤害、费用收据、开发入口过滤、真实配置工厂的事实已经收口在第 6、7、9、10 节。当前没有阻止按本文开始实现的架构事实缺口；仍须主会话取得同工作区共享文件写入窗口。设计允许实施本文边界内的闭环，不批准任何旧任意链进入 actor，也不对旧批次未决作新的正确性结论。

真实登记的最低合同已由最后一份补查确认：

- 新建 CloneReleaseDefinition，提供 id/kind/tag/trajectory/moving/lifetimeTicks/speed/thickness 八个必要方法，显式 role 和 explicitManaCost，别依赖 role 默认 ATTACK、费用默认 -1。不调用 ReleaseDefinitions 的私有 define，不冒用 SELF_STATUS，后者会进入炉心提交。
- 两个生成定义 kind=ACTION、role=ACTION；基础脉冲定义 kind=ACTION、role=ATTACK（瞬时动作，不生成旧 HIT_VOLUME）。kind/role 只描述真实动作分类，不授权普通链执行，开发能力门仍整链拒绝。moving=false；speed=0；定义的 lifetimeTicks/thickness 只是满足接口的合法静态值，不拿它们当分身实体期限或新增无消费者键。tag/trajectory 取仓库已存在的适合非移动 ACTION 的合法枚举，具体符号由 Cursor 按接口落实；不新增弹道算法或改旧枚举语义。
- 两生成 explicitManaCost=10，脉冲 explicitManaCost=2。Technique 原始基础冷却均 20；原始伤害分别 0、0、2。用真实 TechniqueType.MAGIC，现有通用/无特殊元素语义的属性，等级上限和复杂度采用最小合法值；复杂度不承担隐藏职责。不得因原始伤害 0 就把生成费用回退为免费。
- TechniqueManager.register 三个真实 Technique；ReleaseDefinitions.registerAll 挂接 CloneTechniqueRegistry 的释放定义登记；ModTechniques.declareSpecialBalances 调 CloneBalance；封存时三个 id 均存在，clone_response_pulse 加入 BalanceCatalog 内置伤害宿主集合，标准键不专项重复。getAll 仍全量，客户端/服务端能力一致。
- 本版真实执行注册是 CloneTechniqueRegistry 的生成规格和动作消费者；旧 TechniqueEffectRegistry 是玩家链的 void execute(ServerPlayer, Technique, ItemStack, TechniqueCastContext) 接口，不适合承载分身 actor，不能通过一个空 lambda 假装已接入。旧 effect 不登记也不影响本版命令消费者；若当前整合登记流程确需防御条目，只登记明确拒绝误用的适配，不让它变成收费后的正常执行入口。

报告确认当前共享伤害源只能是 owner 的 playerAttack，直接实体也为 owner。本文刻意保留该限制及原版受击表现，没有宣称已经实现 actor 直接伤害源、全链同步或水幻形正式玩法。其他聊天正在处理的旧链落点/份数/共享锚点等问题不在本次审查或修复范围，不能被本次“兼容底座”结论覆盖。

本设计轮结论：按本文实施。尚未实现，未编译，未启动游戏，未新增独立测试阶段。下一步由主会话按文件所有权及跨聊天写入窗口派 Cursor High Fast；常规实现不需要用户再确认一次。

