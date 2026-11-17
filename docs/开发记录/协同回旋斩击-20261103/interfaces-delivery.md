# 协同、回旋、斩击：公共接口交付

阶段：实现接口层。没有业务实现，没有接入旧释放链，没有登记平衡键，没有编译。命名门禁已对本次新增 Java 做语法解析并通过。正式复审、制作表、启动器和游戏验收都未做。

公共包：`com.bslx.XueXiMod.shu.technique.release.cooperative.api`。根目录还有三份合同类型：`TechniqueExecutionCapabilities`、`ExecutionFamilyPlan`、`ActorAttackAdapterRegistry`。后续作者只实现自己的类，不改这些文件的签名。

## 1. 统一映射

消费者按下表使用，不再改名。

| 合同措辞 | 冻结类型 |
| --- | --- |
| `ActionTimelineSpec` | `api.TimelineSpec`。增加 `MovementPolicy movementPolicy`，否则回旋的原地声明没有落点 |
| `CooperationSettings.maxMove` | 组件 `moveLimit`。`max` 不能作为字段名片段 |
| `ShortReposition` 的 `maxDistance` | 参数和组件 `distanceLimit` |
| 散文中的 `familyId` | `FamilyKey` 全体字段。不另造实体 UUID，结算身份里也不含 `actorOrdinal` |
| `memberId` / `logicalCopyId` | `memberOrdinal` / `copyOrdinal` |
| `serverSession` | `UUID`。分身运行时的 `long sessionToken` 只通过 `ServerSessionLookup` 对照 |
| 共享链包中的 `SettlementEnvelope` | 本阶段放在 `api.SettlementEnvelope`。写范围不能改旧链文件；整合时不得把封套改回 `Object` |
| 合同中的类 `FamilyExecutionRuntime` | 冻结为 `api.FamilyExecutionRuntime` 接口。整合实现类固定叫 `cooperative.FamilyExecutionService`，实现该接口，不另写一套 `start/tick/cancel` |
| `DeflectionService.ReleaseGate` | 私有嵌套类，保留不动。新门是 `api.ReleaseGate` |
| `DetachedActorFrame` | 捕获用的脱离帧。协同演员用新的 `api.ActorFrame`，两者不互换 |
| `ChainRole.RULE_COOPERATIVE` | 本阶段不改枚举。协同识别用 `TechniqueExecutionCapabilities.Kind.COOPERATIVE_RULE` 和 `PlanSlotKind.COOPERATIVE_RULE`。整合时两处同时成立 |
| 平衡键 `maxRepositionDistance` | 运行时进入 `CooperationSettings.moveLimit` |
| 平衡键 `slashTiltDegrees` | 运行时进入 `AdapterVolumeSpec.tiltDegrees` |
| 平衡键 `selectionRadius` / `sideOffset` / `windupTicks` / `recoveryTicks` / 盒尺寸 / `lifetimeTicks` | 同名进入 `CooperationSettings`、`TimelineSpec`、`AdapterVolumeSpec` |

`CloneSpec` 还没有 `externalCooperation` 字段。C 以后添加的组件类型必须是 `ExternalCooperation`，名字保持 `externalCooperation`。普通分身用 `OPEN`，水幻形和开发幻影用 `CLOSED`。本阶段不改 `CloneSpec`。

## 2. 既有类型

直接引用、不包装成 `Object` 的既有类型：

- `net.minecraft.world.phys.Vec3`
- `net.minecraft.resources.ResourceKey<Level>`、`ResourceLocation`
- `net.minecraft.server.level.ServerLevel`、`ServerPlayer`
- `net.minecraft.server.MinecraftServer`
- `net.minecraft.world.entity.LivingEntity`
- `net.minecraft.world.item.ItemStack`：只在 `WeaponDisplaySnapshot.capture` 读取一次，计划不保存它
- `net.minecraft.nbt.CompoundTag`：武器快照保存副本，读取方法再复制一次
- `net.minecraftforge.registries.ForgeRegistries`
- `com.bslx.XueXiMod.XueXiMod.MOD_ID`
- `com.bslx.XueXiMod.shu.balance.ConfiguredDamage`
- `com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot`
- `com.bslx.XueXiMod.shu.technique.release.ReleaseInput`
- `com.bslx.XueXiMod.shu.technique.release.water.CastSource`
- `com.bslx.XueXiMod.shu.technique.release.clone.CloneBalance.CloneSpawnQuote`
- `com.bslx.XueXiMod.shu.technique.release.clone.core.CloneKind`
- `com.bslx.XueXiMod.shu.technique.release.clone.core.CloneHandle`
- `com.bslx.XueXiMod.shu.technique.ActionWindow.Sink`
- `com.bslx.XueXiMod.player.ManaPaymentReceipt`

故意不放进计划的可变对象：`Skill`、主手 `ItemStack`、线程局部 `CastFrame`、`DomainInstance`、`UncommittedShare`、包内 `BatchReleasePlan.ChildPlan`、命中接续的 `AtomicBoolean`。`ChildPlan` 的几何和规则标志降到 `HitContinueLink`，价格只进 `ShareSlot`。`CastSource.stackRef` 留在 `FrozenCarrierCredential` 私有字段里，没有公开读取器。

## 3. 结构常数

`D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/cooperative/api/CooperativeStructureLimits.java`

这些不是平衡键。`UNCOMMITTED_RESERVATION_TICKS=80`，`PREPARE_TICK_CAP=2`，`REPOSITION_TICK_CAP=2`，`SWEEP_SAMPLE_CAP=8`，`STANCE_PAIR_CAP=2`，`OWNER_BODY_LOOKUP_CAP=4`，`COOPERATION_RESERVATION_CAP=128`，`COOPERATION_PREPARE_PER_TICK_CAP=32`，`ACTIVE_FAMILY_CAP=128`，`ACTIVE_FAMILIES_PER_OWNER=4`，`FAMILY_PLANNED_EMIT_CAP=128`，`LIVE_PLANNED_EMIT_CAP=256`，`PUBLISH_PER_TICK_CAP=128`，`TALLY_TARGET_CAP=64`，`EMIT_PER_COPY_PER_ACTOR=1`，`ACTORS_PER_COOPERATION=2`，`OWNER_ACTOR_ORDINAL=0`，`COOPERATING_ACTOR_ORDINAL=1`，`OWNER_BODY_GENERATION=0`，`IMMEDIATE_SLASH_RELEASE_TICK=0`，`IMMEDIATE_SLASH_RECOVERY_TICKS=8`，`STANCE_FORWARD_BLOCKS=0.5`，`STANCE_DEVIATION_BLOCKS=1`。选择半径上限 16，归位距离上限 8，侧距 0.75 到 2，前摇 1 到 20，收势 1 到 40，盒尺寸上限 512，倾角 -80 到 80。武器 NBT 条目 64、深度 8、字符串 256，超出即拒绝。

稳定标识：

- `CooperativeTechniqueIds`：`sword_oblique_slash`、`movement_spin`、`rule_cooperative`、`clone_body`、`sword_horizontal_slash`
- `CooperativeActionIds`：`xueximod:oblique_slash`、`xueximod:spin`
- `CooperativeTexts`：键 `gui.xueximod.cooperative.unadapted_attack`，句子「后续攻击尚未适配该动作/协同」。语言文件整合时再写

`FrozenValues` 提供 `present`、`finite`、`unit`、`list`。`unit` 会归一化。

## 4. 能力与执行族

`D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/cooperative/TechniqueExecutionCapabilities.java`

```java
public enum Kind { TIMED_RELEASE, STANDALONE_DEPLOYMENT, EXPLICIT_ATTACK_ADAPTER, COOPERATIVE_RULE }
public static TechniqueExecutionCapabilities none()
public static TechniqueExecutionCapabilities timedRelease()
public static TechniqueExecutionCapabilities standaloneDeployment()
public static TechniqueExecutionCapabilities explicitImmediateAttack()
public static TechniqueExecutionCapabilities cooperativeRule()
public static TechniqueExecutionCapabilities of(Set<Kind> kinds, @Nullable ReleaseGateKind actionGate)
public Set<Kind> kinds()
public boolean has(Kind kind)
public @Nullable ReleaseGateKind actionGate()
public boolean isTimedRelease()
public boolean isStandaloneDeployment()
public boolean isExplicitAttackAdapter()
public boolean isCooperativeRule()
```

静态工厂与实例判断不同名。Java 不能只靠 static 或返回值区分 `timedRelease()`、`standaloneDeployment()`、`cooperativeRule()`。上面六行是最终签名：工厂仍是 `timedRelease()`、`standaloneDeployment()`、`explicitImmediateAttack()`、`cooperativeRule()`；实例判断是 `isTimedRelease()`、`isStandaloneDeployment()`、`isExplicitAttackAdapter()`、`isCooperativeRule()`。`has(Kind)` 仍然可用。

一项术式只能有一种能力。旧术式用 `none()`。斜斩用 `explicitImmediateAttack()`。回旋用 `timedRelease()`。协同规则用 `cooperativeRule()`。普通分身用 `standaloneDeployment()`。横劈保持 `none()`；它只在带新前缀的族里经注册表查找适配器。`COOPERATIVE_RULE` 是链角色落地前的识别位，避免各处比较术式字符串。

`D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/cooperative/ActorAttackAdapterRegistry.java`

```java
public static void register(String techniqueId, ActorAttackAdapter adapter)
public static Optional<ActorAttackAdapter> find(String techniqueId)
public static boolean supports(String techniqueId)
```

进程内只存无状态适配器，不是跨服执行队列。本阶段不登记斜斩或横劈。E 交出工厂，由 I 在整合时调用 `register`。同一实例可以再次登记，换实例会拒绝。

`D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/cooperative/ExecutionFamilyPlan.java`

```java
public record ExecutionFamilyPlan(FamilyKey family, List<ChainIndexMapping> chainIndexes,
    FrozenCarrierCredential carrier, ReleaseGate gate, @Nullable CooperationOrder cooperation,
    List<AttackMemberPlan> attacks, List<LogicalSpawnIndex> spawns, PlanShareSchedule shares,
    List<QuotedCooldown> cooldownPieces, FamilyControlToken cancelToken,
    FamilyControlToken completionToken, Vec3 frozenAim, float frozenYaw,
    boolean horizontalAimFallback)
```

构造时复制列表并做结构检查：链下标不重复；一个族最多一段定时动作、一段接触动作、一条协同规则，定时和接触不能叠在同一族；协同必须有后续攻击；立即族没有动作份额；定时族和接触族恰好一份动作份额；每个攻击成员恰好一份攻击份额；协同份额可缺，但标记必须和槽一致；`cancelToken.tokenId()` 与 `completionToken.tokenId()` 必须不同。每个 `AttackPreparation.cancelId` 只等于取消令牌的 `tokenId`。完成令牌单独存在，不写入 `cancelId`；成员封套里的载体必须和族载体是同一身份；逻辑生成索引按成员覆盖 `copyCount * spawnCount`，结算序号从 0 连续，不含演员维。单人展开超过 128 拒绝。双人超过 128 时计划仍允许建立，运行时回退单人。冷却原料必须有条目，但是安装时只聚合实际提交的份额。

## 5. 身份、门和时间线

路径前缀：`D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/cooperative/api/`

```java
public record FamilyKey(UUID serverSession, ResourceKey<Level> dimension, UUID ownerId, UUID castId, long epoch, int familyOrdinal)
public record MemberKey(FamilyKey family, int memberOrdinal)
public record LogicalCopyKey(MemberKey member, int copyOrdinal, int spawnOrdinal)
public record ActorFrame(UUID actorId, int actorOrdinal, Vec3 feet, Vec3 eye, Vec3 aim, float visualYaw, boolean mirrored)
public record TriggerGeometry(@Nullable Vec3 point, Vec3 direction, @Nullable UUID targetId)
public record TimelineSpec(int windupTicks, int recoveryTicks, MovementPolicy movementPolicy)
public record ReleaseGate(ReleaseGateKind kind, @Nullable TimelineSpec timeline)
public record SettlementIdentity(FamilyKey family, int memberOrdinal, int roundIndex, int copyOrdinal, int spawnOrdinal, UUID targetId)
public record SettlementLedgerRef(MemberKey member, UUID ledgerId)
public record FamilyControlToken(UUID tokenId)
```

`TimelineSpec.releaseTick()` 返回前摇。`endTickOffset()` 返回前摇加收势。立即门的时间线只能是 0 刻出刀、8 刻收势、`IN_PLACE`。接触门的时间线必须为空。定时门前摇至少 1。`TriggerGeometry` 的方向会归一化，点可以为空。

```java
public enum CancelReason { /* hard() 区分硬取消和软失败 */ }
public enum ActionLifecycle { PREPARED, RUNNING, RELEASED, FINISHED, CANCELLED }
public enum ReleaseGateKind { IMMEDIATE, CONTACT, TIMED }
public enum MovementPolicy { IN_PLACE }
public enum PlanSlotKind {
    SEEK, TRAJECTORY, RULE, COOPERATIVE_RULE, TIMED_ACTION, CONTACT_ACTION, IMMEDIATE_ATTACK, ENCHANT, OTHER
}
```

硬取消：主人死亡、离线、换维、复制，载体失效，父族取消，区域卸载，服务器停止。软失败：无演员、演员死亡、撤回、受阻、过远、站位不安全、准备超时、预算不足、适配缺失、发布失败、结构拒绝、校验失败。

```java
public interface ReleaseGateHandle {
    FamilyKey family();
    ReleaseGateKind kind();
    boolean trigger(TriggerGeometry geometry);
    void cancel(CancelReason reason);
    boolean closed();
    ActionLifecycle lifecycle();
}
public interface ContactFamilySource {
    ReleaseGateHandle openSubFamily(TriggerGeometry geometry);
    void onWindowEnded();
    void onWindowFinished();
    boolean windowClosed();
}
public interface ContactReleaseAdapter {
    ActionWindow.Sink sink(ContactFamilySource source);
}
```

一把门的 `trigger` 只成功一次。冲刺每一次合法接触调用 `openSubFamily` 取得新门，再 `trigger` 一次。不要把同一把门的重复回调当成第二次播放或扣费。

```java
public interface SettlementGuard {
    boolean active(ServerLevel level);
    boolean allows(ServerLevel level, LivingEntity target);
    void cancel(CancelReason reason);
}
```

I 在整合时把它挂到本批显式账本。每次记录和刷新都复查会话、世界、父执行、主人和 `DamagePermission`。不把某一刻的允许布尔值写进计划。取消丢掉未结算载荷。已结算伤害不倒扣。

## 6. 结算封套、载体和武器

```java
public record DomainLink(UUID instanceId, String techniqueId, UUID ownerId)
public record HitContinueLink(int parentChainIndex, int childChainIndex, int ownCopies, boolean entityLock,
    boolean dropPath, boolean upwellPath, boolean stagger, int lifetimeTicks, double radius, UUID continuationId)
public final class SettlementEnvelope {
    public SettlementEnvelope(@Nullable EnchantmentSnapshot enchantments, ReleaseInput chainEnergy,
        @Nullable DomainLink domain, @Nullable HitContinueLink hitContinue, FrozenCarrierCredential carrier,
        List<Integer> enchantChainIndexes, List<Integer> modifierChainIndexes)
    public @Nullable EnchantmentSnapshot enchantments()
    public ReleaseInput chainEnergy()
    public @Nullable DomainLink domain()
    public @Nullable HitContinueLink hitContinue()
    public FrozenCarrierCredential carrier()
    public List<Integer> enchantChainIndexes()
    public List<Integer> modifierChainIndexes()
}
```

E 和 C 整包传递封套，不从里面重算伤害。`chainEnergy` 缺省为 `ReleaseInput.BASE`，传递后不得再乘倍率。`continuationId` 是接续凭据身份；是否已经结算的可变标记留在运行时。

```java
public final class FrozenCarrierCredential {
    public FrozenCarrierCredential(CastSource source)
    public int skillSlot()
    public int selectedSlot()
    public int generation()
    public boolean matches(CastSource source)
    public boolean sameIdentity(FrozenCarrierCredential other)
}
```

```java
public final class WeaponDisplaySnapshot {
    public static final WeaponDisplaySnapshot EMPTY
    public WeaponDisplaySnapshot(ResourceLocation itemId, int count, @Nullable CompoundTag frozenTag)
    public static WeaponDisplaySnapshot capture(ItemStack stack)
    public ResourceLocation itemId()
    public int count()
    public @Nullable CompoundTag frozenTag()
}
```

`frozenTag()` 返回副本。数量 0 到 64。空快照只能是 `minecraft:air`。

## 7. 攻击准备和原子发布

```java
public record AdapterVolumeSpec(double boxDepth, double boxWidth, double boxThickness, int lifetimeTicks, double tiltDegrees)
public record AttackPreparation(String techniqueId, int chainIndex, ConfiguredDamage configuredDamage,
    AdapterVolumeSpec volume, WeaponDisplaySnapshot weapon, Vec3 frozenDirection, SettlementEnvelope settlement, UUID cancelId)
public record PreparedAttack(AttackPreparation preparation, ResourceLocation presentationId, ExplicitVolumeGeometry localShape)
public record ResolvedCopyGeometry(Vec3 origin, Vec3 bladeNormal, float roll, boolean pose3d, boolean overrideGeometry, Vec3 playerEye)
public record ExplicitVolumeGeometry(Vec3 origin, Vec3 direction, Vec3 bladeNormal, float roll, boolean pose3d,
    double boxDepth, double boxWidth, double boxThickness)
public record PreparedEmit(ExplicitVolumeGeometry geometry, int lifetimeTicks, ResourceLocation presentationId,
    LogicalCopyKey copy, int actorOrdinal)
public record PlannedVolumeSpawn(PreparedEmit emit, int settlementOrdinal, SettlementLedgerRef ledger, SettlementGuard guard)
public interface ActorAttackAdapter {
    PreparedAttack prepare(AttackPreparation input);
    List<PreparedEmit> emit(PreparedAttack attack, LogicalCopyKey copy, ActorFrame actor, ResolvedCopyGeometry geometry);
}
```

`AttackPreparation.cancelId` 等于所属 `ExecutionFamilyPlan` 的 `cancelToken.tokenId()`。`completionToken` 是另一枚令牌，构造时已拒绝与取消令牌相同，检查不会要求它等于 `cancelId`。

`prepare` 和 `emit` 不执行玩家攻击、分身表演、体积运行时或伤害。局部形状相对玩家眼点。每个演员每个逻辑副本必须返回 1 个 `PreparedEmit`，其它数量在整合构建计划时拒绝。`PlannedVolumeSpawn` 本身表示关闭隐式全局实体倍率和环境 `CastFrame` 二次变换，没有可关掉的那两个布尔开关。`localShape` 的盒尺寸必须和 `AdapterVolumeSpec` 相同。横劈读取自己的盒配置，不抄斜斩的键。

```java
public record ChainIndexMapping(int chainIndex, String techniqueId, PlanSlotKind slotKind, int memberOrdinal, boolean memberScoped)
public record AttackMemberPlan(int chainIndex, int memberOrdinal, String techniqueId, PreparedAttack prepared,
    int copyCount, int spawnCount, int delayTicks, boolean stagger, @Nullable HitContinueLink continueLink)
public record LogicalSpawnIndex(int memberOrdinal, int copyOrdinal, int spawnOrdinal, int settlementOrdinal)
public record ShareSlot(ShareRole role, int memberOrdinal, boolean familyLevel, int amount)
public record PlanShareSchedule(List<ShareSlot> slots, boolean cooperationSeparate)
public record QuotedCooldown(String techniqueId, ShareRole role, int memberOrdinal, boolean familyLevel, int cooldownTicks)
public enum ShareRole { ACTION, ATTACK_MEMBER, COOPERATION }
```

族级份额的 `memberOrdinal` 为 -1。攻击份额必须指向成员。协同槽最多一个，并且 `cooperationSeparate` 与槽是否存在一致。金额只是父收据上的索引。

```java
public record MemberPublishRequest(MemberKey member, List<PlannedVolumeSpawn> volumes, ShareSlot attackShare)
public record MemberStage(UUID stageToken, MemberKey member, int volumeCount)
public interface AtomicMemberPublisher {
    MemberStage stage(MemberPublishRequest request);
    void commit(MemberStage stage);
    void discard(MemberStage stage, CancelReason reason);
}
```

同一请求里每个逻辑副本的演员集合必须相同，并且只能是 1 名或 2 名。`stage` 只暂存并取得宿主组许可。`commit` 在同一服务端调用栈发布全部几何。`discard` 丢弃未发布暂存。不得先发布玩家体积再补一份。

## 8. 协同、部署和短距移动

```java
public record CooperationSettings(double selectionRadius, double moveLimit, double sideOffset, int prepareTicks)
public record CooperationOrder(CooperationSettings settings, long expiryTick)
public record CooperationReservation(UUID token, long bodyGeneration, FamilyKey family, @Nullable CloneHandle body,
    long expiryTick, boolean actorPresent)
public enum PreparationState { PREPARING, READY, UNAVAILABLE }
public record ExternalCooperation(boolean enabled)
public interface CooperationActors {
    CooperationReservation tryReserve(ServerPlayer owner, FamilyKey family, ActorFrame ownerFrame,
        CooperationSettings settings, long expiryTick);
    PreparationState advance(ServerLevel level, CooperationReservation reservation, ActorFrame currentOwnerFrame, long tick);
    Optional<ActorFrame> readyFrame(CooperationReservation reservation);
    boolean validate(CooperationReservation reservation, ActorFrame ownerFrame);
    void markCommitted(CooperationReservation reservation, MemberKey member);
    void finish(CooperationReservation reservation, boolean anyCommitted);
    void cancel(CooperationReservation reservation, CancelReason reason);
}
```

`tryReserve` 不返回 null。没有演员时 `actorPresent=false` 且 `body=null`，不留锁。有演员时两者同时存在。只查主人索引，最多 4 具。距离相同取右侧。站位是脚点加冻结水平前向乘 0.5，再加减 `sideOffset`。近垂直视线用施法时水平角，并在族计划里把 `horizontalAimFallback` 记为 true。`markCommitted` 只在 I 已取得全有或全无许可的同一调用栈里调用。`finish/cancel` 以 C 自己保存的提交状态决定冷却，调用方传 false 不能恢复已提交预算。`ExternalCooperation.OPEN` 和 `CLOSED` 已固定。

```java
public enum DeploymentSource { PLAYER_STANDALONE, DEVELOPMENT }
public enum StandaloneDeploymentKind { NORMAL }
public enum DeploymentStatus {
    SUCCESS, ILLEGAL_STRUCTURE, CARRIER_MISSING, QUOTA, COOLDOWN, UNSAFE_SPAWN, PAYMENT_FAILED, PUBLISH_FAILED, PERMISSION
}
public record CloneDeploymentInput(StandaloneDeploymentKind kind, CloneSpawnQuote quote,
    FrozenCarrierCredential carrier, DeploymentSource source)
public record CloneDeploymentResult(DeploymentStatus status, @Nullable CloneHandle handle, @Nullable String refusalKey)
public interface CloneStandaloneDeployment {
    CloneDeploymentResult deployStandalone(ServerPlayer owner, CloneDeploymentInput input);
    int dismissStandalone(ServerPlayer owner, StandaloneDeploymentKind kind);
}
```

输入里的 `CloneSpawnQuote` 必须已经是普通分身，并且魔力折扣只应用过一次。输入不含扣款收据。`deployStandalone` 是玩家路线唯一的扣款、发布、退款和生成冷却入口；来源不是 `PLAYER_STANDALONE` 时拒绝。实现内部再拆无副作用准备和已付费发布，旧开发生成共用底层，但不得调用这个方法绕过权限。`dismissStandalone` 只结束该主人的普通部署身体及其外部预约，返回数量，不扣钱，不重置已有生成冷却。成功结果必须带 `CloneHandle`，拒绝结果不能带。

```java
public sealed interface RepositionAttempt permits AcceptedReposition, RejectedReposition
public record AcceptedReposition(UUID planId, CloneHandle handle, Vec3 destination, double distanceLimit, int durationTicks)
public record RejectedReposition(CancelReason reason)
public enum RepositionMotion { RUNNING, ARRIVED, FAILED }
public interface ShortReposition {
    RepositionAttempt prepare(ServerLevel level, CloneHandle handle, Vec3 destination, double distanceLimit, int durationTicks);
    RepositionMotion advance(ServerLevel level, AcceptedReposition plan, long tick);
    void cancel(AcceptedReposition plan);
}
```

只移动传入的分身句柄，不接受玩家。时长 1 或 2 刻，距离 0 到 8 格。每步做保守包围盒扫掠，采样不超过 8，不加载区块。`cancel` 可重复。拒绝不是 null。

## 9. 表现和运行端口

```java
public record ActionPlayback(UUID serverSession, ResourceKey<Level> dimension, FamilyKey family,
    @Nullable MemberKey member, UUID actorId, int entityId, long actorGeneration, long sequence,
    ResourceLocation actionId, long serverStartTick, int windupTicks, int recoveryTicks, float visualYaw,
    boolean mirrored, @Nullable WeaponDisplaySnapshot weapon)
public interface ActorActionPublisher {
    void start(ActionPlayback playback);
    void stop(FamilyKey family, UUID actorId, long sequence);
}
```

`member` 为空表示族级动作，例如回旋开始。玩家世代用 0。序号从 1 起。旧序号的 `stop` 不得清掉更新的动作。这个端口不能请求出刀或伤害。服务端只发布这份数据，不引用客户端类。

```java
public interface FamilyRuntimeHandle {
    FamilyKey family();
    ActionLifecycle lifecycle();
    void cancel(CancelReason reason);
}
public interface FamilyExecutionRuntime {
    FamilyRuntimeHandle start(ExecutionFamilyPlan plan);
    void tick(ServerLevel level, long tick);
    void cancel(FamilyKey family, CancelReason reason);
    void cancelOwner(UUID serverSession, UUID ownerId, CancelReason reason);
    void cancelWorld(UUID serverSession, ResourceKey<Level> dimension, CancelReason reason);
    void stopServer(UUID serverSession, CancelReason reason);
}
public interface ServerSessionLookup {
    UUID sessionOf(MinecraftServer server);
    long cloneSessionToken(UUID serverSession);
}
public interface ShareLedgerPort {
    void bind(FamilyKey family, PlanShareSchedule schedule, ManaPaymentReceipt receipt);
    void commit(FamilyKey family, ShareRole role, int memberOrdinal, boolean familyLevel);
    void refundUncommitted(FamilyKey family, ShareRole role, int memberOrdinal, boolean familyLevel, ServerPlayer owner);
    void closeUntaken(FamilyKey family, ServerPlayer owner);
}
```

运行时实例按 `MinecraftServer` 隔离，不使用静态跨服队列。份额提交走 `UncommittedShare` 和 `ManaPaymentReceipt.retainShare`。退款走 `refundShare` 或份额池 `close`。禁止 `MagicPool.restore`。这些方法的接口本身不扣款。

## 10. 谁实现什么

C 实现 `CooperationActors`、`ShortReposition`、`CloneStandaloneDeployment`，并在自己的核心里增加 `ExternalCooperation externalCooperation`。预约状态、无副作用准备和已付费发布留在分身核心内部。C 不改水幻形捕获包、`CloneBalance`、共享注册和上述接口文件。

E 实现 `ActorAttackAdapter`：斜斩和横劈各一个。斜斩、回旋的术式定义和专项声明做成工厂交给 I。E 不调用 `ActorAttackAdapterRegistry.register`，不改旧横劈效果、命中规格、账本、共享注册或客户端。`emit` 不读玩家眼睛、主手、`CastFrame.current` 或全局宿主上下文。

V 实现 `ActorActionPublisher`，消费 `ActionPlayback`。动作资源标识只用 `CooperativeActionIds`。未确认斩光、关键帧、声音和贴图不进入 `src/main/resources`。V 不改 `CloneEntity` 的服务端字段；需要同步的数据先落在已有 DTO 上。

I 在后续整合实现 `FamilyExecutionService`、`AtomicMemberPublisher`、`ContactReleaseAdapter`、`ContactFamilySource`、`ServerSessionLookup`、`ShareLedgerPort` 和 `SettlementGuard`，并把能力、适配器、链角色、平衡键和三处入口接到既有编译器、自动轮和提交桥。三处入口仍是：含新能力的手动链、原冲刺带新斩击、领域绑定模板。接触几何必须进 `TriggerGeometry`，不能再传 null。本阶段这些旧文件都没改。

最终类的接入方式：E 的适配器工厂和术式工厂、C 的三个端口实现、V 的播放器注册工厂，都交给 I。I 只在共享初始化处调用 `ActorAttackAdapterRegistry.register` 和 `TechniqueExecutionCapabilities` 的四个工厂。没有新前缀的横劈、冲刺、跳跃和水幻形继续走原路径。

## 11. 自检和未做

签名以上面的源码为准，本次没有改旧共享源码。命名门禁退出码 0。没有跑 `compileJava`，所以类型解析还没有被编译器确认。没有游戏验证，没有接入未确认美术，没有正式实现复审，没有迁表。

接口可以闭合。没有需要改设计才能继续的缺项。余额键、语言行和 `ChainRole.RULE_COOPERATIVE` 留在 I 的整合，不在本阶段假装已经生效。
