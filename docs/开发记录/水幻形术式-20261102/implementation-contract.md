# 水幻形实现合同

当前状态（2026-11-02）：下面正文是并行实现时的历史合同，原句保留，不删除。文首和后文里的「v2 未确认」「本文不接入资源」「不注册 `water_phantom`」是当时的阶段门。用户原话「这个版本好接入吧」已经批准 Steve 水线 v2.1 并授权接入，该阶段门被这次批准替代。正式登记、专项声明和确认版资源接线以 `integrate-c-report.md`、`integrate-d-report.md` 和 `交付与实测.md` 为准。这次说明不是新的合同，也不是新的复审。

日期：2026-11-02。批次：水幻形术式-20261102。本文冻结并行作者的方法、字段和调用，不改设计结论。视觉 v1 不采用。v2 未确认。本文不接入资源，不注册 `water_phantom`。

根包：`com.bslx.XueXiMod`。下文路径都相对 `src/main/java/com/bslx/XueXiMod/`。

原载体门阻塞已由设计第2/2轮裁决收口，见第9节：仅法杖上的完整声明式捕获计划可接纳其已消费的SWORD载荷。A/B/D已冻结接口不变；本次补充只归C，不重审其余合同。视觉v2仍未确认，正式注册与具体资源接线继续等待同版确认。

## 1. 作者和路径

| 作者 | 独占路径 | 现有文件也归该作者 |
|---|---|---|
| A | `shu/technique/release/clone/core/` 整个包 | 已有 `CloneService`、`CloneRuntime`、`CloneRuntimeState`、`CloneEntity`、`CloneSpec`、`CloneTriggerInput`、`CloneTriggerPolicy`、`CloneLifecycleEvents`、`CloneEndReason`、`CloneSourceScope`、`CloneResults`、`CloneDecoyPolicy`、`ClonePositionPolicy`、`CloneActorFrame`、`CloneActionRequest`、`CloneActionContext`、`CloneHandle`。实体类就在 core，不另归别人 |
| B | `shu/technique/release/clone/action/` 与新建 `shu/technique/release/clone/capture/` | `CloneActionRegistry`、`CloneAction`、`CloneActionCapabilities`、`ClonePreparedAction`、`ClonePulseAction` |
| C | 新建 `shu/technique/release/capture/` 与 `shu/technique/release/clone/water/`；公共释放入口见下 | `network/SkillUsePacket.java`、`network/SkillAssemblyPacket.java`、`gui/SpellAssemblyMenu.java`、`item/StaffSkills.java`、`shu/ModTechniques.java`、`shu/technique/release/ReleaseDefinition.java`、`shu/technique/release/ReleaseDefinitions.java`、`network/HoldRepeat.java`、`debug/performance/PerfCounter.java`、`debug/performance/PerfMetric.java` |
| D | 客户端、网络包、确认后的运行资源 | `client/CloneRenderer.java`、`network/ModNetwork.java`、`XueXiMod.java` 里的客户端挂接 |

父包 `shu/technique/release/clone/` 里的 `CloneTechniqueRegistry`、`CloneReleaseDefinition`、`CloneBalance`、`CloneEntityTypes` 这次谁也不改。B 只在已有的 `ClonePulseAction.register()` 末尾登记断潮适配器，那个方法本来就由 `CloneTechniqueRegistry.registerAll()` 调用。

一个文件只有一个作者。A 不写 B/C/D 的文件。B 不扣款、不写冷却、不 `endActor`。C 不复制 A 的付款事务，不调用 `ManaPaymentReceipt.charge`。D 不改 `CloneEntity` 的字段写入。

### 1.1 必须先落地、别人再调用的共享入口

这些不是两人同时改，而是被调用方先按本文把签名放进自己的文件：

1. C 先在 `PerfCounter`、`PerfMetric` 枚举末尾追加第 4 节的常量。`PerformanceRecorder.buildTicksCsv` 的表头保持现在这 11 列，不改 `ticks.csv`。summary 会按枚举名自动多行。
2. A 先公开 `PreparedCapture`、`CapturedAttackSnapshot`、`DetachedActorFrame`、`DeploymentLease`、`CloneService.spawnCaptured`、`CloneService.finishDeployment`、`ClonePresentation.install`。
3. B 先公开 `CaptureQuoteContext`、`CaptureQuote`、`ActorPayloadAdapter`、`ActorPayloadRegistry`、`ActorVolumeSpec`、`ActorVolumeRuntime.submit/tick/shutdown`。
4. C 在 `ReleaseDefinition` 增加 `actorCapture()` 默认 `NONE`。旧定义不用改。
5. D 实现 `ClonePresentationSink` 后调用 A 的 `install`。在此之前 A 的无操作接收器丢弃事件。
6. `ModNetwork` 现在占用包号 0–15（`ModNetwork.register` 36–51 行）。D 改这个文件时再读一次，取当时第一个未占用号；本次读到的下一个号是 16。不在别的任务里占号。

`XueXiMod` 客户端挂接只由 D 写。`ModTechniques.registerAll` 与 `declareSpecialBalances` 只由 C 写，而且第 8 节的正式调用这次不加上。

## 2. 已有签名（保持）

这些是调用边界，新代码从这里接，不改参数：

- `CloneService.spawn(ServerPlayer owner, CloneKind kind)` → `CloneResults.SpawnResult`。仍走 `developmentOnly`，仍只收开发术式。
- `CloneService.request(ServerPlayer, CloneHandle, CloneTriggerInput)` → `RequestResult`。`executeTick = tick+1`，`expireTick = tick+2`。水幻形反击不走这条。
- `CloneService.onAcceptedHurt(CloneEntity, DamageSource)` 仍要求 `actor.isAlive()`。水幻形致死不调用它。
- `CloneService.perform` 仍对 `actionKey` 做 `CloneActionRegistry.admit(..., CloneActionCapabilities.full())`，仍在提交线前 `ManaPaymentReceipt.charge`。只服务旧脉冲。
- `ManaPaymentReceipt.charge(ServerPlayer player, int cost, boolean tellPlayer)`：`cost<=0` 返回零额收据；失败返回 `null`。`refund(ServerPlayer)` 只退 `COMMITTED` 且同一玩家。
- `CastCooldown.mark(ServerPlayer player, ItemStack weapon, int slot, int untilTick)` 与 `busy(...)` 比较的是 `player.getServer().getTickCount()`，不是 `CloneRuntime.tick()`。
- `CastTiming.accepted(int startedTick, int finishedTick, int replayTick, List<Technique> fired)` 是 `network` 包内可见。分发器放在 `release/capture`，不直接构造 `CastTiming`。
- `HoldRepeat.drop(ServerPlayer player, int slot)` 同包可见。`SkillUsePacket` 直接调用。
- `SkillSegmentMath.scaleCooldown(int ticks, double cooldownReduction)`：`ticks<=0` 原样返回，否则 `max(1, round(ticks * (1 - cooldownReduction)))`。只在 `SkillUsePacket` 里调用一次。
- `SkillSegmentMath.segmentDamage`：基础伤害 `>0` 时为 `scaledBase + weaponBonus + magicDamage`，否则 `scaledBase + weaponBonus`。`scaledBase` 仅在法杖加成非空且基础伤害 `>0` 时为 `max(0, base * (1 + attributeDamageBonus))`。
- `BalanceService.manaCost(BalanceSide.SERVER, techniqueId, parameterKey, rawDefault)`、`cooldownTicks(BalanceSide.SERVER, techniqueId, rawDefault)`、`damageContribution(BalanceSide.SERVER, techniqueId, RawDamage)`、`intValue`、`number`。每笔只调用一次。
- `DamagePermission.allows(ServerPlayer caster, LivingEntity target)`。
- `TechniqueHits.classifyRespecting(LivingEntity target, ServerPlayer player, float amount, TideMarkSuccess tide)`。
- `LivingCandidateQueries.entityQuery(Level level, AABB box, Predicate<? super LivingEntity> predicate)`。
- `CloneSourceScope.open(MinecraftServer server, Origin origin, UUID rootEventId, int depth)`，用完 `close()`。`Origin.CLONE_RESPONSE` 或 `depth>0` 时 `blocksResponse()` 为真。
- `CloneEntity.rejectsIncomingAttacker`、`rejectsPulseTarget`。
- `CloneRuntime` 常量：每人 4、全服 128、每具请求 1、服务端请求队列 128、每 tick 提交 32、请求寿命 2 tick。诱敌仍是每 tick 4 次查询、每 20 tick 轮转、每具 8 个诱敌租约、每次最多看 32 个候选。
- `ChainStructureEpoch.current()` / `changed()`。`ReleaseRegistry.register` 已经会 `changed()`。
- 实体碰撞箱 `0.6F × 1.95F`，眼高 `1.74F`（`CloneEntityTypes` 21–22 行，`CloneEntity` 眼高常量）。不改注册尺寸。

`CloneActionRegistry.register(CloneAction action)` 改为只保留：

```java
public static void register(CloneAction action, CloneActionCapabilities capabilities)
```

删掉按 `instanceof ClonePulseAction` 给 `full()` 的分支。`ClonePulseAction.register()` 改为 `register(HOLDER, CloneActionCapabilities.full())`，并在同一方法末尾调用 `ActorPayloadRegistry.registerBuiltins()`。未知动作没有第二种“全假也能登记成可放”的入口。

## 3. 新建类型

名字按命名门禁：标识符至少 3 个字符；驼峰段不得使用 `arg/ctx/idx/len/pos/req/res/val/msg/num/dir/obj/cfg/err` 等门禁缩写。下面的名字已经避开。

不可变记录的组件全部 `final`。构造时复制列表。不提供改字段的方法。网络包不能反序列化成 `PreparedCapture`、`CapturedAttackSnapshot` 或 `DeploymentLease`。

### 3.1 A，`clone/core`

```java
public enum CloneSpawnAuthority {
    DEVELOPMENT_COMMAND,
    PLAYER_CAPTURED_CAST
}

public enum PresentationKey {
    LEGACY_HUMANOID,
    WATER_OUTLINE
}

public enum TerminationPolicy {
    BUDGET,
    BREAK_ON_TRIGGER
}
```

`CloneSpec.TriggerMode` 增加 `CONTACT_OR_HURT`。旧值 `MANUAL_AND_HURT` 保留。行为看 `TriggerMode` 和 `TerminationPolicy`，不看 `CloneKind`。

`CloneSpec` 在现有 10 个组件之后追加，且只追加这些：

```java
TerminationPolicy terminationPolicy,
PresentationKey presentationKey,
int presentationRevision,
@Nullable UUID deploymentId
```

紧凑构造继续拒绝空术式、空动作键、非有限生命和诱敌半径。`presentationKey == null` 或 `terminationPolicy == null` 同样拒绝。`deploymentId == null` 只允许旧开发规格。

`specFor` 给旧幻影和旧普通分身补：`TerminationPolicy.BUDGET`、`PresentationKey.LEGACY_HUMANOID`、`presentationRevision = 0`、`deploymentId = null`、`TriggerMode.MANUAL_AND_HURT`。不因为 `kind == PHANTOM` 改成碰一下就散。

```java
public record CapturedAttackSnapshot(
        String payloadTechniqueId,
        int adapterVersion,
        long structureEpoch,
        UUID ownerId,
        UUID rootEventId,
        ConfiguredDamage configuredDamage,
        double depthBlocks,
        double heightBlocks,
        double widthBlocks,
        int lifetimeTicks,
        int configuredPayloadMana,
        int configuredPayloadCooldownTicks) {}

public record DetachedActorFrame(
        long sessionToken,
        ResourceKey<Level> dimension,
        UUID ownerId,
        UUID rootEventId,
        UUID deploymentId,
        Vec3 feetPosition,
        Vec3 eyeOrigin,
        Vec3 lookUnit,
        long frozenGameTick) {}

public record PreparedCapture(
        CloneSpawnAuthority authority,
        UUID ownerId,
        ResourceKey<Level> dimension,
        boolean immediate,
        Vec3 feetPosition,
        Vec3 eyeOrigin,
        Vec3 lookUnit,
        int totalMana,
        int residenceTicks,
        double decoyRadiusBlocks,
        double bodyHealth,
        CapturedAttackSnapshot snapshot,
        long structureEpoch) {}

public record CapturedResponseTicket(
        long sessionToken,
        UUID ownerId,
        UUID rootEventId,
        UUID deploymentId,
        long actionToken,
        DetachedActorFrame actorFrame,
        CapturedAttackSnapshot payload,
        long executeTick,
        long expireTick,
        boolean manualFeedback) {}
```

记录约束：

- 快照不含 `Skill`、`CastFrame`、回调、活目标、`Tally`、主手 `ItemStack`、领域状态。
- `DetachedActorFrame` 不含 `CloneEntity`、`ServerLevel`、`CloneRuntimeState`。
- `PreparedCapture.authority` 必须是 `PLAYER_CAPTURED_CAST`。`ownerId` 与快照里的主人相同。`totalMana >= 0`。`bodyHealth` 水幻形固定 `1.0`，由 C 写入，A 不再读平衡。`residenceTicks` 是已经读好的配置寿命，`0` 表示这次加载会话内不到期。`immediate == true` 时 `decoyRadiusBlocks` 必须是 `0`。
- 票不含租约对象，只含 `deploymentId`。租约在 A 的表里。
- 上述记录都不写 NBT，不从存档恢复。

```java
public final class DeploymentLease {
    public UUID deploymentId();
    public UUID ownerId();
    public long sessionToken();
    public Phase phase();
    public boolean released();

    public enum Phase { BODY, TICKET, VOLUME, RELEASED }
}
```

构造包内可见。外面拿不到第二份。`phase()` 只读。转移方法包内可见，顺序只允许 `BODY → TICKET → VOLUME → RELEASED`。`release()` 幂等，第二次返回 `false`。不允许从 `RELEASED` 回到前面，不允许同时处于两相。

```java
public final class CapturedResponseRuntime {
    public static void tick(MinecraftServer server, int commitBudget);
    public static void cancelOwned(UUID ownerId, CaptureEndReason reason);
    public static void cancelDimension(ResourceKey<Level> dimension, CaptureEndReason reason);
    public static void shutdown(CaptureEndReason reason);
    public static boolean idle();
}

public enum CaptureEndReason {
    VOLUME_FINISHED,
    TICKET_EXPIRED,
    OWNER_LOGOUT,
    OWNER_DEATH,
    OWNER_DIMENSION,
    AREA_UNLOADED,
    WORLD_UNLOAD,
    SERVER_STOP,
    ADAPTER_MISMATCH,
    INVALID_GEOMETRY,
    FAILED_SUBMIT
}
```

`TRIGGERED_TRANSFER` 不是结束原因，只是租约进了 `TICKET`。

```java
public enum CaptureTriggerKind { CONTACT, ACCEPTED_HURT, IMMEDIATE }

public static CloneResults.SpawnResult CloneService.spawnCaptured(ServerPlayer owner, PreparedCapture prepared);
public static boolean CloneService.breakForResponse(CloneHandle handle, CaptureTriggerKind kind, @Nullable UUID subjectId, Vec3 frozenLook);
public static void CloneService.finishDeployment(DeploymentLease lease, CaptureEndReason reason);
```

`spawnCaptured` 不调用 `developmentOnly`，不调用 `CloneRuntime.armSpawnCooldown`，不调用 `CloneBalance.readSpawn`。失败用现有 `CloneResults.RejectReason`：`WRONG_SERVER`、`OWNER_UNAVAILABLE`、`CAP_REACHED`、`INVALID_GEOMETRY`、`INVALID_QUOTE`、`INSUFFICIENT_MANA`。成功时 `SpawnResult.accepted==true`，`chargedMana==prepared.totalMana()`。

`breakForResponse` 只给 A 内部的接触和受击调用。它不收费。成功才把租约从 `BODY` 转到 `TICKET` 并登记票。失败不改身体。

`finishDeployment` 检查当前服务器线程、`lease.sessionToken()` 等于 `CloneRuntime.sessionToken()`、租约未释放。不符则返回，不退款，不重新生成身体。相符则 `release()` 一次，并让 B 的体积停掉。幂等。

```java
public interface ClonePresentationSink {
    void accept(ClonePresentationNotice notice);
}

public record ClonePresentationNotice(
        ResourceKey<Level> dimension,
        long sessionToken,
        UUID deploymentId,
        long actionToken,
        long eventSerial,
        PresentationKey presentationKey,
        int presentationRevision,
        Vec3 worldPosition,
        Vec3 lookUnit,
        NoticePhase phase,
        int durationTicks,
        VolumeVisualKind volumeVisual) {}

public enum NoticePhase { MAINTAIN, SCATTER, VOLUME }
public enum VolumeVisualKind { NONE, TIDE_SLASH_FULL }

public final class ClonePresentation {
    public static void install(ClonePresentationSink sink);
    public static void publish(ClonePresentationNotice notice);
    public static boolean installed();
}
```

默认接收器是 A 包内的 `NoopClonePresentation`，`accept` 什么都不做。`install` 只能成功一次，第二次抛 `IllegalStateException`。`publish` 在未安装时调用无操作接收器。无操作不注册术式、不选贴图、不把外观报成已完成。通知里没有贴图路径字符串。

`CloneEntity` 增加只读访问器，同步数据只在值变化时写：

```java
public PresentationKey presentationKey();
public int presentationRevision();
public long syncedSessionToken();
```

旧实体默认 `LEGACY_HUMANOID`、修订号 `0`。水幻形写 `WATER_OUTLINE`。D 只读这些方法和通知，不写同步字段。

`CloneRuntimeState` 增加包内字段：`@Nullable DeploymentLease deploymentLease`、`@Nullable CapturedAttackSnapshot capturedSnapshot`、`boolean cosmeticOnly`。不公开给 B/C 随意改。C 通过 `spawnCaptured` 进入。

### 3.2 B，`clone/capture`

```java
public enum PayloadQuoteFault {
    UNSUPPORTED,
    INVALID_QUOTE,
    GEOMETRY,
    ADAPTER_MISMATCH
}

public record CaptureQuoteContext(
        Technique payload,
        ItemStack weaponCopy,
        float weaponBonus,
        @Nullable Double attributeDamageBonus,
        ShuAttribute embryo,
        long structureEpoch) {}

public record CaptureQuote(
        boolean accepted,
        @Nullable PayloadQuoteFault fault,
        String playerText,
        String techniqueId,
        int adapterVersion,
        @Nullable ConfiguredDamage configuredDamage,
        int configuredMana,
        int configuredCooldownTicks,
        double depthBlocks,
        double heightBlocks,
        double widthBlocks,
        int lifetimeTicks) {}

public record CapturedActionAuthority(
        long sessionToken,
        UUID ownerId,
        UUID rootEventId,
        UUID deploymentId,
        long actionToken,
        int adapterVersion) {}

public record ActorVolumeSpec(
        long sessionToken,
        ResourceKey<Level> dimension,
        UUID ownerId,
        UUID rootEventId,
        UUID deploymentId,
        long actionToken,
        Vec3 eyeOrigin,
        Vec3 forwardUnit,
        Vec3 upUnit,
        Vec3 rightUnit,
        double depthBlocks,
        double heightBlocks,
        double widthBlocks,
        int lifetimeTicks,
        ConfiguredDamage configuredDamage,
        int adapterVersion,
        VolumeVisualKind volumeVisual) {}

public interface ActorPayloadAdapter {
    String techniqueId();
    ShuAttribute attribute();
    int version();
    CaptureQuote freezeQuote(CaptureQuoteContext context);
    ActorVolumeSpec instantiate(CapturedAttackSnapshot payload, DetachedActorFrame actorFrame, CapturedActionAuthority authority);
}

public final class ActorPayloadRegistry {
    public static void register(ActorPayloadAdapter adapter);
    public static void registerBuiltins();
    public static @Nullable ActorPayloadAdapter find(String techniqueId);
}

public final class ActorVolumeRuntime {
    public static boolean submit(ActorVolumeSpec spec, DeploymentLease lease);
    public static void tick(MinecraftServer server);
    public static void shutdown(CaptureEndReason reason);
    public static boolean idle();
}

public final class ActorVolumeGeometry {
    public static Vec3[] basis(Vec3 lookUnit);
    public static AABB worldBounds(ActorVolumeSpec spec);
    public static boolean intersectsObb(ActorVolumeSpec spec, AABB targetBounds);
}
```

`freezeQuote` 不扣款、不生成、不写冷却、不读 `owner.getMainHandItem()`。上下文里没有 `ServerPlayer`。武器只用传入的副本；断潮不要求它是剑。

`instantiate` 不读 `CastFrame`。返回的规格不含 `Skill`、宿主组、`Tally`、监听器。

`submit` 要求 `lease.phase()==VOLUME`、`lease.sessionToken()==spec.sessionToken()`、两边 `deploymentId` 相同、当前是该租约的服务器线程。否则返回 `false`，不创建体积，不收费。

`basis`：`forwardUnit` 为已单位化视线。`|forwardUnit.y| > 0.999` 时用 `(1,0,0)` 做临时右轴，否则 `forwardUnit.cross(new Vec3(0,1,0))` 再单位化得到 `rightUnit`。`upUnit = rightUnit.cross(forwardUnit)` 再单位化。不读主人当前视线。体积中心是 `eyeOrigin + forwardUnit * depth/2`。半尺寸是 `width/2`、`height/2`、`depth/2`。`width` 沿 `rightUnit`，`height` 沿 `upUnit`，`depth` 沿 `forwardUnit`。粗查询只用 `worldBounds`。命中必须 `intersectsObb` 为真。

`TideSeverActorAdapter`：`techniqueId` 为 `tide_sever`，属性 `WATER`，`version` 从 `1` 起。`registerBuiltins` 只登记这一个。它不实现 `CloneAction`，不调用 `TideSeverEffect`、`TechniqueHitSpec.spawn`、`spawnReported`、`TechniqueHitSettle.tryHit`。

报价公式，各一次：

- 几何：`SpecialBalance.number("tide_sever", BalanceKeys.BEAM_LENGTH, 5.0)`、`SpecialBalance.number("tide_sever", BalanceKeys.BOX_HEIGHT, 3.0)`、`ReleaseRegistry.get("tide_sever").thickness()`、`lifetimeTicks()`。这四个就是现有效果的读口（`TideSeverEffect.execute` 43–46 行；定义内部再进 `SpecialBalance`）。
- 伤害原始额用第 2 节的 `segmentDamage` 公式，`magicDamage = 0`，不用附魔分支。然后只调用一次 `BalanceService.damageContribution(BalanceSide.SERVER, "tide_sever", RawDamage.of(rawAmount))`。不要再乘 `BalanceDisplay.damage`。
- 魔力：`Technique.manaCost()` 在断潮上是 `ceil(6*2)=12`，再只调用一次 `BalanceService.manaCost(BalanceSide.SERVER, "tide_sever", "manaCost", rawMana)`。
- 冷却：只调用一次 `BalanceService.cooldownTicks(BalanceSide.SERVER, "tide_sever", payload.getBaseCooldown())`。这里还不做法杖缩减。

尺寸或寿命不是有限正数、伤害对象为空、金额超出 `0..1_000_000`，返回 `GEOMETRY` 或 `INVALID_QUOTE`，`accepted==false`。

体积每 tick：先空表则 `tick` 立刻返回。否则 `entityQuery` 粗筛，精确 OBB，再从冻结眼原点用 `ClipContext.Block.COLLIDER`、`Fluid.NONE` 打到相交点。射线实体用仍在线的主人，不用已经 `discard` 的分身。实心阻挡则这次不伤害，下一 tick 可以再查。未加载区块不射线、不强制加载。

真正 `hurt` 前：`DamagePermission.allows` 和 `CloneEntity.rejectsPulseTarget` 都要过，并且目标不是任何 `CloneEntity`。然后 `TechniqueHits.classifyRespecting(target, owner, configuredDamage.amount(), TideMarkSuccess.NONE)`。进入 `hurt` 前占该目标的命中令牌；同一体积对同一目标最多一次，免疫也占住。尝试目标最多 64 个，满了不再增加。配置伤害为 0 仍算一次已出手，不产生正伤副作用。每次实际结算重新 `CloneSourceScope.open(..., CLONE_RESPONSE, rootEventId, 1)`，`finally` 里 `close()`。不改无敌帧，不加领域、潮印、火种、击退，不动主手，不调用 `onFirstTrigger`。

体积结束、到期、异常：调用 `CloneService.finishDeployment(lease, reason)`。不调用 `endActor`。

客户端斩击画面不属于 B。规格只带 `VolumeVisualKind.TIDE_SLASH_FULL`。现有断潮不写 `StillSheetVisual`（默认 `NONE`），`TechniqueHitRenderer.renderSheet` 用整张 UV `0..1`，尺寸来自 `CohortLaneLayout.sheetWindow`。D 按这个事实画，不发明 32 帧。

### 3.3 C，`release/capture` 与 `clone/water`

```java
public enum ActorCaptureMode { NONE, NEXT_SINGLE_ATTACK }

public enum CaptureCarrierMode { NONE, STAFF_ONLY }

public record ActorCaptureContract(
        ActorCaptureMode mode,
        ChainRole capturedRole,
        int maximumEntries,
        ShuAttribute requiredAttribute,
        boolean requireExplicitAdapter,
        boolean rejectModifiers,
        boolean rejectNesting,
        CaptureCarrierMode carrierMode,
        Set<TechniqueType> capturedTypeExceptions) {

    public static final ActorCaptureContract NONE = new ActorCaptureContract(
            ActorCaptureMode.NONE, ChainRole.ATTACK, 0, ShuAttribute.NONE, false, false, false,
            CaptureCarrierMode.NONE, Set.of());
}

public enum CaptureRefusal {
    CAPTURE_MISSING_PAYLOAD,
    CAPTURE_COMPLEX_CHAIN,
    CAPTURE_UNSUPPORTED_PAYLOAD,
    CAPTURE_UNSUPPORTED_EMBRYO,
    CAPTURE_INVALID_QUOTE,
    CAPTURE_CAP_REACHED,
    CAPTURE_OWNER_UNAVAILABLE,
    CAPTURE_GEOMETRY_INVALID,
    CAPTURE_ADAPTER_CHANGED,
    CAPTURE_TICK_LIMIT,
    CAPTURE_REQUIRES_STAFF,
    CAPTURE_INVALID_CARRIER_SLOT
}

public enum ActorCaptureStatus { NOT_APPLICABLE, REFUSED, PLAN }

public record CapturedStructurePlan(
        int captureOriginalIndex,
        List<Integer> payloadOriginalIndexes,
        List<Integer> consumedOriginalIndexes,
        List<Integer> chargedOriginalIndexes,
        int nextOriginalIndex,
        String payloadTechniqueId,
        ShuAttribute embryoCopy,
        String adapterKey,
        int adapterVersion,
        long structureEpoch) {}

public record ActorCaptureInspection(
        ActorCaptureStatus status,
        @Nullable CapturedStructurePlan plan,
        @Nullable CaptureRefusal refusal,
        String playerText) {}

public record CapturedInput(
        boolean ownerShift,
        Vec3 feetPosition,
        Vec3 eyePosition,
        Vec3 lookUnit,
        float weaponBonus,
        @Nullable Double attributeDamageBonus,
        double cooldownReduction,
        ItemStack weaponCopy,
        int slotIndex,
        boolean staffCarrier,
        int serverGameTick) {}

public record CaptureDispatchResult(
        boolean accepted,
        boolean lacksMana,
        @Nullable CaptureRefusal refusal,
        String playerText,
        @Nullable Technique captureTechnique,
        int unscaledCooldownTicks,
        @Nullable CloneHandle handle) {}

public final class ActorCaptureCompiler {
    public static ActorCaptureInspection inspect(Skill skill);
}

public final class ActorCaptureDispatcher {
    public static CaptureDispatchResult tryExecute(ServerPlayer owner, Skill skill,
            CapturedStructurePlan plan, CapturedInput input);
}
```

`ReleaseDefinition` 新增：

```java
default ActorCaptureContract actorCapture() {
    return ActorCaptureContract.NONE;
}
```

`inspect` 看原始 `skill.getTechniques()` 和 `skill.getEmbryo(index)`。任一格的 `actorCapture().mode()` 不是 `NONE` 才继续，否则立刻 `NOT_APPLICABLE`，文本为空。本版要恰好两格：第 0 格是捕获，第 1 格是 `ActorPayloadRegistry.find` 能找到的攻击，两边原胚都是 `ShuAttribute.NONE`，第 1 格没有附魔下标，没有第三格。否则 `REFUSED`。计划字段固定为：`captureOriginalIndex=0`，`payloadOriginalIndexes=[1]`，`consumedOriginalIndexes=[0,1]`，`chargedOriginalIndexes=[0,1]`，`nextOriginalIndex=2`，`structureEpoch=ChainStructureEpoch.current()`。计划不含主人、位置、费用、武器。

结构拒绝的玩家文字用：「水幻形后仅支持一段已适配水系攻击；本版支持断潮」。缺原胚以外的半链在装配说明里用「缺少支持的下一攻击」。非 `NONE` 原胚用 `CAPTURE_UNSUPPORTED_EMBRYO`，文字说明原胚会改变这一发，本版不执行。

`WaterPhantomDefinition` 实现 `id()=="water_phantom"`，`kind()==ACTION`，`tag()==TRIGGER`，`moving()==false`，`role()==ACTION`，`explicitManaCost()==10`，`requiresFreshPress()==true`，`lifetimeTicks()` 固定返回 `1`（释放接口占位，和现有分身定义一样）。`actorCapture()` 返回 `NEXT_SINGLE_ATTACK`、捕获角色 `ATTACK`、最多 1 条、属性 `WATER`、要求显式适配器、拒绝修饰和嵌套；`carrierMode=STAFF_ONLY`，`capturedTypeExceptions=Set.of(TechniqueType.SWORD)`。集合构造时复制为不可变；该例外仅作用于完整计划已消费的载荷下标，不作用于宿主、任意SWORD或独立施放。`ChainRole` 不靠术式编号分支。

第2轮只给C新增上述两个契约组件与第9节纯载体检查接口，不改A/B记录、报价、lease或体积签名。`CapturedInput.staffCarrier` 已存在，继续用实际法杖状态填它，不另造A/B载体枚举。

`WaterPhantomBalance` 提供 `declareSpecials()` 和 `read(BalanceSide)`，但本次没有任何调用方。声明内容见第 8 节。`read` 在键未封存时抛 `IllegalStateException`，禁止返回一套假的“已经生效”数字。

### 3.4 D

```java
public final class ClonePresentationS2CPacket {
    public static void register(SimpleChannel channel, int messageId);
}
```

字段与 `ClonePresentationNotice` 相同，外加接收者只在服务端发送。编码使用枚举序号，不写贴图路径，不写任意资源字符串。`handle` 只 `enqueueWork` 到客户端线程，校验会话、维度、有限坐标（绝对值不超过 `3.0e7`）、时长 `0..200`、已知枚举。失败丢弃。不回调服务器。

`CloneAfterimagePlayback` 保存独立快照。上限 256，超出丢掉最旧的纯视觉。实体移除只结束维持，不删已接受的散水。世界切换、维度不符、断线、对应世界卸载时清空。`deploymentId` 已有终止标记时，迟到的维持包丢弃。终止标记保留到视觉时长和跟踪窗口里较长的那个结束。

`ActorVolumeVisual` 只消费 `TIDE_SLASH_FULL`：客户端本地使用现有资源 `xueximod:textures/entity/xueximod/horizontal_slash.png`，整张 UV，世界尺寸与 `ActorVolumeGeometry` 的中心和半尺寸一致。不创建 `TechniqueHitEntity`。

`CloneRenderer`：`presentationKey()==LEGACY_HUMANOID` 保持现在的僵尸模型和 `textures/entity/zombie/zombie.png`。`WATER_OUTLINE` 直接返回，不画僵尸，不读 v1/v2 候选贴图。具体 Steve 水线模型等到确认交接再写。

## 4. 调用协议

### 4.1 C → B 报价，C → A 生成，C 写冷却

`SkillUsePacket.executeSkill` 在现有 244 行准入通过、248–249 行武器加成算完之后，`BatchReleaseCompiler.compile`（现 250 行）之前：

其之前的 `StaffSkills.castRefusal` 和 `offerNow` 法杖SWORD门先按第9节取得完整计划的局部载荷例外，不能仍无条件拒绝。`executeSkill`及分发器仍核对实际`staffCarrier`；未通过载体检查的PLAN不调用B报价或A生成。

1. 用当刻服务器状态做 `CapturedInput`：`owner.isShiftKeyDown()`、`owner.position()`、`owner.getEyePosition()`、单位化 `owner.getLookAngle()`、已经算好的 `weaponBonus` 和 `attributeDamageBonus`、`cooldownReduction`、`weapon.copy()`、槽位、是否法杖、`getTickCount()`。不从客户端包读潜行或视线。
2. `inspect(skill)`。
3. `NOT_APPLICABLE`：什么都不做，继续现有 `compile`。
4. `REFUSED`：`HoldRepeat.drop(player, slot)`，`tell` 拒绝文字，返回 `CastTiming.rejected()`。不报价，不扣款，不推进游标。
5. `PLAN`：`tryExecute`。无论成败都 `HoldRepeat.drop`。不再回到主人循环，因此第二格不会进 `pressCommitCost`。

`tryExecute` 顺序：

1. 同一主人、同一个 `serverGameTick` 已经成功部署过，返回 `CAPTURE_TICK_LIMIT`，文字「这一刻已经部署过水幻形」。只记录成功。
2. 主人不可用，返回 `CAPTURE_OWNER_UNAVAILABLE`。
3. `adapter.freezeQuote`。失败按 `PayloadQuoteFault` 映射为 `CAPTURE_UNSUPPORTED_PAYLOAD`、`CAPTURE_INVALID_QUOTE`、`CAPTURE_GEOMETRY_INVALID`、`CAPTURE_ADAPTER_CHANGED`。
4. 生成费用 `BalanceService.manaCost(SERVER, "water_phantom", "manaCost", 10)` 一次。与载荷魔力做 `long` 相加，和超出 `Integer` 非负范围则 `CAPTURE_INVALID_QUOTE`。不要把合计再乘全局魔力。
5. 冷却用 `BalanceService.cooldownTicks(SERVER, "water_phantom", 20)` 一次，加载荷冷却，`long` 检查。结果放进 `unscaledCooldownTicks`。这里不调用 `scaleCooldown`，不调用 `CastCooldown.mark`。
6. 寿命 `BalanceService.intValue(SERVER, "water_phantom", "lifetimeTicks")`，诱敌 `BalanceService.number(SERVER, "water_phantom", "decoyRadius")`。立即模式诱敌写成 `0`。身体生命写成 `1.0`。
7. 组装 `CapturedAttackSnapshot` 和 `PreparedCapture`，调用 `spawnCaptured`。
8. A 返回失败：`INSUFFICIENT_MANA` 映射为 `lacksMana==true`；`CAP_REACHED` → `CAPTURE_CAP_REACHED`；`OWNER_UNAVAILABLE` → `CAPTURE_OWNER_UNAVAILABLE`；`INVALID_GEOMETRY` → `CAPTURE_GEOMETRY_INVALID`；`INVALID_QUOTE` → `CAPTURE_INVALID_QUOTE`。不记本 tick 已部署。
9. A 返回成功：记下本 tick，`accepted==true`，`captureTechnique` 只有水幻形这一条。

回到 `SkillUsePacket`：失败且 `lacksMana` 返回 `CastTiming.insufficient()`；其他失败 `rejected()`。成功时：

```java
int scaled = SkillSegmentMath.scaleCooldown(result.unscaledCooldownTicks(), cooldownReduction);
int nowTick = player.getServer().getTickCount();
int replayTick = scaled <= 0 ? nowTick : nowTick + scaled;
return CastTiming.accepted(nowTick, nowTick, replayTick, List.of(result.captureTechnique()));
```

现有 `offerNow` 在 `accepted && !settlesLater` 时调用 `CastCooldown.mark`，并只对 `fired()` 做一次使用统计。`fired()` 只有水幻形，断潮不加第二遍。失败不走到 `mark`。反击不再 `mark`。

A 内部 `spawnCaptured` 的付款是唯一的 `ManaPaymentReceipt.charge(owner, prepared.totalMana(), true)`。`null` 就是魔力不足。发布前失败按现有 `spawn` 的 `finally` 退一次、释放占位。发布成功不退，之后撤回、到期、离线、换维都不退。

### 4.2 A → B 执行，租约只转移一次

容量：捕获部署从 `reserve` 起占本人和全服名额。身体在 `actors` 里时就算这一份，不要再加一份。`TRIGGERED_TRANSFER` 把身体移出 `actors` 后，租约仍占同一份，直到 `release()`。旧开发分身仍在 `endActor` 时释放名额。

立即模式：`spawnCaptured` 发布成功后把 `cosmeticOnly` 设为真，同一调用里 `breakForResponse(..., IMMEDIATE, null, frozenLook)`。外观不诱敌、不碰撞、不受击、不参加接触。下一 tick 出体积，然后散。创建时站在敌人身上也不当接触。

驻留模式：身体保持 `ARMED`。接触和受击走第 7 节。成功的 `breakForResponse` 把租约转到 `TICKET`，票的 `executeTick = 当前 CloneRuntime.tick()+1`，`expireTick = executeTick+1`。然后结束身体并 `publish` 散水。

`CloneRuntime.drain` 的空转条件改为：没有身体、没有旧队列、没有主人记录、并且两个新 runtime 的 `idle()` 都为真，才返回。非空时先给旧 `pumpQueue` 与 `CapturedResponseRuntime.tick` 共用剩余的 32 次提交。旧队列不得再单独用满 32 之后让新票再来 32。

票执行时 A 检查会话、主人存活且同维、眼位区块已加载、适配器版本与 `find(id).version()` 相同、几何有限。失败则 `finishDeployment`，驻留反击不刷屏；立即模式给主人一条文字。通过后：

```java
ActorVolumeSpec spec = adapter.instantiate(snapshot, frame, authority);
lease 包内转到 VOLUME;
boolean submitted = ActorVolumeRuntime.submit(spec, lease);
```

`submitted==true` 时 `PerformanceRecorder.add(PerfCounter.CLONE_RESPONSE_COMMITS, 1)` 只加这一次。破散当 tick 不加。`submitted==false` 时 `finishDeployment(..., FAILED_SUBMIT)`，不加提交计数。

体积随后的 tick 由 A 调用 `ActorVolumeRuntime.tick`。B 不在自己的 Forge 事件里再挂一套服务器 tick。

### 4.3 B → A 结束

体积正常打完、到期、主人取消、提交失败，都只调用 `finishDeployment`。A 校验线程、会话、主人标识和租约身份。已经 `RELEASED` 则返回。不允许 B 调用 `CloneRuntime.endActor`、`CloneService.end` 或 `request`。

主人登出、主人死亡、换维、世界卸载、停服仍从现有 `CloneLifecycleEvents` 进 A。A 在原有 `endOwned` / `endDimension` / `shutdown` 里追加 `cancelOwned`、`cancelDimension`、`shutdown`。这些取消包括票和已发出的体积。`OWNER_DEATH` 不延后。

### 4.4 A → D 表现

A 在散水决定已经做成、`discard` 之前收集接收者：同维主人，加上同维、距离不超过实体跟踪范围 10 区块（160 格）的玩家。每个玩家每个 `actionToken + phase` 只发一次。`ClonePresentation.publish` 之后才 `discard`。D 未 `install` 时发布被无操作接收器丢掉，逻辑仍结束身体。

`CloneRenderer` 的 `WATER_OUTLINE` 早退由 D 写。在那之前 C 不注册术式，A 不加命令，玩家到不了这个实体。现有渲染器对一切 `CloneEntity` 仍返回僵尸贴图，所以不能提前把水幻形放进世界。

### 4.5 计数

C 追加在枚举最后：

- `PerfCounter`：`CAPTURE_PLAN_COMPILES`、`CAPTURE_DEPLOYS`、`CAPTURE_BREAKS`、`CAPTURE_RESPONSE_CANCELS`、`ACTOR_VOLUME_UPDATES`
- `PerfMetric`：`ACTOR_VOLUME_UPDATE`

调用：C 在 `inspect` 得到非 `NOT_APPLICABLE` 时加编译次数；A 在发布成功时加部署、在转票成功时加破散、在票或体积被取消时加取消；B 在体积 tick 真正更新时加 `ACTOR_VOLUME_UPDATES`，并用 `ACTOR_VOLUME_UPDATE` 包住该次更新。采样关闭时不额外 `nanoTime`。不要包一层新的 `ENTITY_QUERY`。

## 5. Tick 和生命周期落点

| 事件 | 现有方法 | 谁扩展 |
|---|---|---|
| 服务器 tick 开始 | `CloneLifecycleEvents.onServerTick` → `CloneRuntime.advanceTick` | A，不新增事件 |
| 服务器 tick 结束 | 同方法 → `CloneRuntime.drain` | A 在 `drain` 里排空旧队列、捕获票、体积、接触和诱敌 |
| 登出 | `onLogout` → `endOwned(OWNER_LOGOUT)` | A 同时 `cancelOwned` |
| 死亡 | `onDeath`：分身 `endLoaded(DEATH)`，主人 `endOwned(OWNER_DEATH)` | 分身死亡受第 7 节门控制；主人死亡不延后 |
| 玩家克隆 | `onPlayerClone` | A 只清旧主人的部署，不复制水幻形 |
| 换维 | `onDimension` → `OWNER_DIMENSION` | A 取消部署 |
| 离开世界 | `onLeave` → `endLoaded(UNLOAD)` | 见第 7 节：已转出的租约不能被这次卸载收回 |
| 世界卸载 | `onLevelUnload` → `endDimension(WORLD_UNLOAD)` | A 取消该维部署 |
| 开服 / 停服 | `onServerStarted` / `onServerStopping` | 停服 `shutdown(SERVER_STOP)`，票和体积丢掉，不留到下一局 |

接触兜底在 `drain` 里、诱敌之前，只处理 `TriggerMode.CONTACT_OR_HURT` 且不是 `cosmeticOnly` 的身体。`LivingCandidateQueries.entityQuery(level, actor.getBoundingBox(), ...)`，然后精确相交。最多处理 32 个候选，第一个通过 `rejectsPulseTarget` 的抢到 `breakForResponse(CONTACT)`。`CloneEntity.push(Entity)` 可以先走到同一方法，不能另写一套资格。地面、物品、经验球、主人、友方、宠物、其他分身不触发。

`CloneDecoyPolicy.pass` 继续读 `spec.decoyRadiusBlocks()`。半径 `0` 已经会直接返回。立即模式写 `0`。

读档：`CloneEntity.readAdditionalSaveData` 已把 `fromSave` 设真并丢弃。捕获票不写 NBT。

## 6. 配置

`BalanceRegistry.seal` 会拒绝“声明了未登记术式”（`BalanceRegistry` 128–131 行）。因此本次：

- `WaterPhantomBalance.declareSpecials()` 可以存在，`ModTechniques.declareSpecialBalances` 不调用它。
- `ModTechniques.registerAll` 不加入 `water_phantom`。
- `ReleaseDefinitions.registerAll` 不 `ReleaseRegistry.register` 水幻形定义。
- 不把它放进 `BalanceCatalog.DAMAGE_HOSTS`。它没有伤害倍率。
- 不写成 `DEVELOPMENT_ONLY` 来绕过封存。

以后确认视觉的一次整合，按第9节已经裁定的法杖路径接线，顺序只能是：先把 `Technique` 放进 `TechniqueManager`，再 `ReleaseRegistry.register`（这样 `Technique.manaCost()` 才能读到显式 `10`，而不是基础伤害 `0` 算出的 `0`），然后 `declareSpecials()`，最后才走到现有的 `seal`。定义上的 `lifetimeTicks()` 继续返回 `1`，不要用 `SpecialBalance.life`，那个下界是 `1`。专项寿命用允许 `0` 的边界。

届时声明，且只有这些键：

```java
BalanceRegistry.declareSpecial(TechniqueBalanceSpec.builder("water_phantom")
        .integerParameter(BalanceKeys.LIFETIME_TICKS, RawDefault.literal(0),
                BalanceBound.count(0, 1_200_000), BalanceUnit.TICKS,
                "没有自然到期写 0，正数从生成提交的服务器单调 tick 起算")
        .numberParameter(CloneBalance.DECOY_KEY, RawDefault.literal(8),
                BalanceBound.inclusive(0, 16), BalanceUnit.BLOCKS, "诱敌半径，0 关闭")
        .build());
```

标准 `manaCost` 默认 `10`、`cooldownTicks` 默认 `20` 由现有 `BalanceCatalog.declareStandards` 在术式已经登记后补上。消费者：

- 生成魔力：部署报价的第一笔，`BalanceService.manaCost` 一次。
- 生成冷却：合成玩家冷却的第一笔，`BalanceService.cooldownTicks` 一次，再与载荷冷却相加，再 `scaleCooldown` 一次。
- `lifetimeTicks`：生成时冻结进 `PreparedCapture.residenceTicks`，立即模式不靠它延迟发射。
- `decoyRadius`：生成时冻结进规格；`0` 关闭诱敌。

断潮继续用它已经声明的 `beamLength`、`boxHeight`、`thickness`、`lifetimeTicks`、标准魔力、冷却和伤害倍率。水幻形不再包一层伤害倍率。不新增命中去重、容量、触发类型或反击费用键。

`Technique` 以后的构造参数：`("water_phantom", "水幻形", MAGIC, 0f, 20, 1, 说明, 1, WATER, 0f, false, PLAYER)`。本次不执行这行。

## 7. 致死六步的现有落点

语义保持设计第 7 节。门只对 `TerminationPolicy.BREAK_ON_TRIGGER` 且当前租约仍是 `BODY` 的水幻形打开。普通分身和开发幻影走现在的 `CloneEntity.hurt`。

1. `CloneEntity.hurt`（现 204–231 行）。水幻形在 `super.hurt` 之前，来源已通过 `rejectsIncomingAttacker`、策略仍是 `ARMED` 时，写下包内 `HurtAttempt`：句柄、租约、眼睛、朝向、来源 UUID、事务序号。不消费额度，不发包，不排队。同一同步 `hurt` 重入则水幻形分支返回 `false`。普通分身不加这道门。
2. `super.hurt` 照常。只延后本模组的死亡收束。`CloneRuntime.endActor`（现 334–369 行）在 `reason==DEATH` 且该 `HurtAttempt` 未关闭时记下 `pendingDeath` 并返回：不设 `prepareCancelled`，不 `cancelPending`，不删 `actors`，不删主人索引。`CloneEntity.hurt` 里现有的 `endLoaded(DEATH)`（226 行）和 `CloneLifecycleEvents.onDeath`（58–59 行）都已经进 `endLoaded` → `endActor`，不要再写第三个删表口。`OWNER_DEATH` 不进这道门。
3. 1.20.1 里 `LivingEntity.die` 会置 `dead` 并广播；`remove(KILLED)` 通常要等 `tickDeath` 的 `deathTime >= 20`，不一定发生在这次 `hurt` 里。A 仍覆盖 `CloneEntity.remove(RemovalReason)`：原因是 `KILLED` 且 `HurtAttempt` 开着，只记 `pendingKilledRemoval`，不调用 `super.remove`。`die` 仍调用 `super.die`。`UNLOADED_TO_CHUNK`、`DISCARDED` 和其他原因不延期，立即取消整次部署。另外覆盖水幻形的 `tickDeath`：不要留 20 tick 尸体；若转票已经完成就立刻走第 5 步的身体清理。普通分身不覆盖这条等待。
4. `super.hurt` 返回后保存这个 `boolean`，再关闭延后门。核对句柄、租约、会话、主人在线且同维。只有返回 `true` 且来源仍符合第 7 节资格，才生成包内 `AcceptedHurtTrigger`。允许此时 `health<=0`。不调用 `onAcceptedHurt` 或 `request`。坐标用 `HurtAttempt` 里保存的，不读主人当前位置。
5. 合格触发把租约转到 `TICKET`，标记预算已用，然后按普通结束去掉身体，并执行刚才记下的移除。此后 `endActor(DEATH)` 看到租约已经不是 `BODY`，只清身体，不取消票。不合格、返回 `false`、硬取消或异常：没有票；按记下的死亡或移除把原清理做完。死亡被 Forge 取消且这次 `hurt` 返回 `false`：回到 `ARMED`。`hurt` 返回 `true` 即使死亡被取消，也是一次有效受击。
6. `finally` 清掉 `HurtAttempt` 和临时引用。异常不重试伤害，不宣称已接受。重复的 `end` / `remove` 看到已转出或已结束时只清身体。`onLeave` 在 `discard` 里会同步再进 `endLoaded(UNLOAD)`；身体结束前先记下“租约已转出”，这次 `UNLOAD` 不得把票取消。主人或世界级取消仍通过部署表取消票和体积。

接触路径不进 `hurt`。它直接 `breakForResponse(CONTACT)`，然后同样转票、撤身体、发散水。

## 8. 现在就能写，以及还要等

### A 现在写

整个 `clone/core`：新记录和租约、`spawnCaptured`、`breakForResponse`、`finishDeployment`、接触、第 7 节、`drain` 空转和共享 32 次预算、无操作表现、旧 `specFor` 的默认值。不改 `spawn(owner, kind)` 的开发门。不加命令。不读候选贴图。

### B 现在写

`register(action, capabilities)`、脉冲改为显式 `full()`、`registerBuiltins`、断潮报价和体积、几何、来源隔离。不改四个水系效果、`TechniqueHitSpec`、`TechniqueHitSettle`、`TechniqueHits`。不写客户端类。

### C 现在写

`actorCapture()` 默认值、编译器、分发器、`SkillUsePacket` 在 `compile` 之前的路由、`PerfCounter` / `PerfMetric` 末尾常量、`WaterPhantomDefinition` 与 `WaterPhantomBalance` 两个类，以及第9节所有C载体门/候选链/本捕获编辑的原子写回。无需等待视觉确认才写这些通用代码。定义未正式注册时旧链仍NOT_APPLICABLE，不能用ID硬编码让未注册内容提前出现。

### C 现在不调用

- `ModTechniques` 注册 `water_phantom`
- `ReleaseRegistry.register` 水幻形
- `WaterPhantomBalance.declareSpecials()`
- `BalanceCatalog.DAMAGE_HOSTS`

第9节已结论明确，不再等待载体选择。实际水幻形仍待视觉确认后正式注册；注册前路由对现有链是NOT_APPLICABLE，这是预期。

### D 现在写

S2C 包和校验、`ModNetwork` 的下一个空号、`ClonePresentation.install`、维持/散水快照容器、`CloneRenderer` 对 `WATER_OUTLINE` 的早退、`TIDE_SLASH_FULL` 的整张 UV。不复制 v1/v2 文件，不写 Steve 水线的框、UV 和帧。

### 确认视觉之后，按第9节一次整合

C 注册术式、定义和专项声明。D 按交接实现人形和散水，并复制已经确认的运行资源。包号和性能枚举不再另开一路去改。那次之前的编译不能当作最终编译。

## 9. 第2/2轮裁决：仅法杖的已捕获载荷类型例外

事实依据：implementation-contract-report.md与read-carrier-gates-final.md，均已完成停写。本节只解除载体门阻塞，不复审或修改A/B/D合同。

### 9.1 载体与声明

唯一正式入口是**法杖槽0**。水幻形仍MAGIC，断潮仍SWORD；断潮在此是被计划完整收走的数据载荷，不由法杖的普通剑术执行路径直接施放。不开放剑技槽、空手或其它物品。水幻形定义声明STAFF_ONLY与SWORD载荷例外，例外还必须同时满足完整结构PLAN、原下标属于payloadOriginalIndexes/consumedOriginalIndexes、显式adapter存在且能力/属性/角色/版本合格、原胚NONE、PLAYER可用。只有类型门可局部让过，其余学习、解锁、容量、原胚、持剑需求、开发能力和费用检查不免除。

普通链没有捕获能力则NOT_APPLICABLE，完全走旧载体规则。水幻形不能凭一个标签携带任意SWORD；断潮单独进法杖仍拒绝。未来新增适配器也不能自动让需要实体持剑的攻击绕过HeldSwordGate。

### 9.2 C的共享纯检查接口（拟新增）

在`shu/technique/release/capture/`新增`ActorCaptureCarrierGate.java`，不依赖A/B新方法，不读写NBT、不付款：

```java
public enum CaptureCarrierDecision { NOT_APPLICABLE, DRAFT, READY, REFUSED }

public record CaptureCarrierCheck(
        CaptureCarrierDecision decision,
        @Nullable CapturedStructurePlan plan,
        @Nullable CaptureRefusal refusal,
        String playerText) {}

public final class ActorCaptureCarrierGate {
    public static CaptureCarrierCheck inspect(Skill candidate, boolean staffCarrier,
                                              boolean permitIncompleteDraft);
    public static boolean permitsCapturedType(CaptureCarrierCheck checked,
                                              int originalIndex, Technique technique);
}
```

只从定义的actorCapture声明查规则。inspect以ActorCaptureCompiler为完整结构裁决来源；READY必须有PLAN且实际staffCarrier为true， permitsCapturedType只对READY中已消费载荷下标且类型在不可变exceptions集合里返回true。DRAFT不产生执行计划，也不豁免任何SWORD插入。载体不对返回CAPTURE_REQUIRES_STAFF，文字“该捕获术式只能装配并释放于法杖”。非法槽/未被捕获的SWORD返回CAPTURE_INVALID_CARRIER_SLOT。实际载体布尔由服务器手持物或菜单绑定载体取得，不信任包字段/NBT。

permitIncompleteDraft只用于编辑，不用于施放。允许的新增前缀草稿只有一格合法捕获宿主（NONE原胚），DRAFT提示“缺少支持的下一攻击”。空链留原规则；反序、第三格、第二个宿主、未知/未适配载荷、非法原胚一律REFUSED，不能因“编辑中”就接受。

### 9.3 装配候选与无损修改

- `SkillAssemblyPacket.put`法杖是末尾追加，slot/techniqueIndex不是法杖插入位置。先从当前完整entries（含原胚）复制，再追加待加项，验证这个候选，不能只查待加Technique。先放宿主形成DRAFT，再放支持的断潮形成READY。先放裸断潮仍按旧门拒绝，不消耗匣子条目。超过两格或换未适配攻击拒绝且原杖/匣子不变。
- `SpellAssemblyMenu.rejectReason(stack,gearIndex)`按“该gearIndex替换之后”的所有齿轮顺序取非空石，连同原胚构造完整候选。空齿轮不占逻辑链下标，与writeGear一致；不能把gearIndex直接当capturedOriginalIndex。mayPlace、实际set/替换、服务端同步都用同一判定，客户端可预览但不作最终授权。
- 学习与解锁按各条目的真实TechniqueType逐条检查；断潮仍需已有的学会/解锁条件，不能因为宿主已学就把载荷视为已学。PLAYER/原胚/容量照常。GUI没有额外法杖类型预滤，无需改筛选按钮或新增UI；用户选择“全部/剑术”能看见断潮，是否可装由候选判定决定。
- 删除始终允许用户无损拆下已有条目；删除宿主后留下的裸SWORD或反序中间态可以作为**不可释放的原样编辑内容**保存，不获得READY，不再允许据此新增非法条目。不得为了凑合法链丢弃载荷、把它转成MAGIC或自动执行。再次放回正确宿主可回READY；删除/修改成普通合法法杖链则回旧规则。纯删除由服务器对实际修改确定，不能信任客户端声称remove而同时插入条目。
- 移动/排序不增加物品，可原样保存不完整/反序的编辑状态并明确“当前捕获链不可释放”；服务器施放仍拒绝。下次新增/替换必须验证完整候选，不把历史草稿当永久类型豁免。packet的remove/move继续syncInventory，原胚与术式一同移动；不新发剑技槽同步包。

本次捕获相关编辑必须无静默消耗/丢槽：旧put的consume后add忽略false、旧writeGear清空后add失败break，不能直接拿来落新许可路径。C在StaffSkills新增`replaceEntriesExact(ItemStack staff, List<StaffSkills.Entry> candidate)`或同语义包内辅助：先在独立候选中验证容量、已知ID和全部原胚，只有完整可写时一次替换术式列表，返回明确结果；失败原术式列表字节不变，不动法杖魔力及其它NBT。调用层负责上面的编辑许可，辅助不能单独给SWORD发许可证。

“捕获相关”覆盖修改前或修改后含actorCapture声明的法杖，以及含SWORD草稿条目的法杖（含拆下宿主后的后续编辑）；因此不能在宿主移走后退回那个可能截断载荷的旧写回。这里只保护原样保存或经验证的新例外，不让孤立SWORD得到插入/施放资格。完全不含这些内容的普通法杖链不因本节被整体改写。

捕获相关put在消耗匣子前准备整份候选并验证来源/法杖版本，最终提交必须成对完成；任一失败两侧恢复到本次原状态，不能吞掉add/consume失败。捕获相关GUI写回先准备全部候选，禁止先清实际法杖再逐条add；替换失败须保持/恢复实际齿轮、鼠标携带栈及来源库存的一致状态，不能只恢复NBT而物品已经被拿走。必要事务辅助留C的SpellAssemblyMenu/StaffSkills内，覆盖该新捕获编辑路径；不借此重构旧普通链全局库存逻辑，也不宣称旧分支已修复。

未知ID或损坏原胚不能被asSkill的continue静默过滤后变成合法两格链：本捕获候选与施放先验证原始entries完整且数量/顺序对应，再构造Skill。非法数据拒绝、不消耗、不破坏性清洗。

### 9.4 两道法杖施放门与最终执行门

`StaffSkills.castRefusal(player,stack)`是更早的门（offerNow约160行），必须由C一起改。先按原规则验证原始entries的未知ID、开发可用性、学会/解锁、容量，再构造原样Skill并调用gate.inspect(...,true,false)。READY时仅豁免permitsCapturedType为真的载荷下标SWORD；DRAFT/REFUSED返回明确捕获拒绝；NOT_APPLICABLE仍执行旧SWORD拒绝。保持原返回String签名和不改NBT行为。asSkill/add不需要全局放宽，它们本来不按SWORD删条目。

`SkillUsePacket.offerNow`约196–201行第二次法杖SWORD门也调用同一纯gate，并只对已消费的载荷下标豁免。不要简单删除整段SWORD检查，或发现任何捕获术式就跳过全链检查。HeldSwordGate、TechniqueAccess、槽0、冷却忙碌等其余门保留。

`executeSkill`在BatchReleaseCompiler之前及`ActorCaptureDispatcher.tryExecute`重新验证完整PLAN、实际法杖/槽0与当前结构纪元；input.staffCarrier为false直接CAPTURE_REQUIRES_STAFF，不调用B报价/A生成。不接受从菜单缓存或客户端传来的“已允许”结果。手写NBT、移动后反序、载荷缺失/多余、能力改变都在付款/冷却/游标/使用统计之前拒绝并解除本次HoldRepeat。

### 9.5 普通剑技与普通法杖保持

不修改`TechniqueType`、`SkillCarrier.allowsMagic`、`allowsEmptyHandSwordCast`或两个术式的类型。非法杖put拒MAGIC、SpellAssemblyMenu剑技mayPlace/rejectReason拒MAGIC、非法杖writeGear同步跳过MAGIC均保持；本版合法捕获链不经过那些分支，无需为了它删除同步过滤。普通法杖独立SWORD仍拒绝，普通法术链仍按旧规则；不会把“法杖装捕获数据”扩大成“法杖能直接放所有剑术”。

### 9.6 精确C范围与收口

C现在可实施：本节gate/契约、原有compiler/dispatcher的载体验证；`item/StaffSkills.java`的castRefusal及捕获相关原子列表辅助；`network/SkillAssemblyPacket.java`法杖追加候选/事务及删除移动状态说明；`gui/SpellAssemblyMenu.java`法杖候选/写回一致性；`network/SkillUsePacket.java`两次法杖门与执行前防御。所有新增路径归C，A/B记录、租约、报价、体积以及D协议签名完全不改。

实现者定向核对：法杖宿主草稿→追加断潮→READY→一次捕获；裸断潮、反序、三格、未学/未解锁、非NONE原胚、剑技槽/空手、手写NBT均收费前拒绝；拆宿主不丢载荷/原胚，草稿不能释放；写回失败不消耗来源或截断法杖。此处只是本次新门的验收合同，不是已运行结果，不新增独立测试阶段。

结论：载体阻塞解除，C可按本文实施，不需要用户另选载体或重复确认开工。设计2/2已用完，不再自动追加设计复审；视觉v2具体确认门仍独立保留。

## 10. 依据

- 设计：`docs/水幻形术式设计与实现指导.md` 第 4–15 节；`design-decision.md`；`用户决定.md`。v1 外形作废。
- 生成、受击、结束：`CloneService` 51、155、231、414 行；`CloneEntity.hurt` 204 行；`CloneRuntime.endActor` 334 行；`CloneLifecycleEvents`。
- 收费前最后的公共口：`SkillUsePacket.executeSkill` 250 行之前。
- 封存拒绝未登记术式：`BalanceRegistry.seal` 128–131 行。
- 断潮读口和画面：`TideSeverEffect` 43–47 行；`TechniqueHitRenderer.renderSheet` 218–237 行；`stillSheet` 默认关闭。
- 包号：`ModNetwork.register` 36–51 行，0–15 已占用。
