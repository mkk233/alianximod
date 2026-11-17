# 分身底座实现合同（作者 A 冻结）

批次：分身底座-20261102。依据：`docs/分身底座设计与实现指导.md` v1（设计 1/2，实现 0/3）。本文只冻结跨作者 API 与文件所有权。不改生命、费用、触发、次数、冷却和容量语义。未写 Java，未编译。

根路径：`D:/alianximod/`。下面出现的拟新增类型都还没有源码。调用既有 API 时以本文签名为准。

## 1. 路径校正

设计第 14.1 节的逻辑分组保持不变。下列位置按仓库实况校正，不扩大行为：

| 项 | 设计写法 | 冻结路径 |
|---|---|---|
| 渲染类 | `client/renderer/CloneRenderer.java` | `src/main/java/com/bslx/XueXiMod/client/CloneRenderer.java`，包名 `com.bslx.XueXiMod.client`。现有 `SwordEntityRenderer`、`TechniqueHitRenderer`、`WaterBarrierRenderer` 都在这个包，没有 `client/renderer` |
| 命令挂接 | 命令归 C | 只改 `ShuCommands.register`。`ModCommands.java` 已经调用它，本批不写 `ModCommands.java` |
| 刻印/术管理界面 | 若屏幕直接 `getAll` 或复杂度 0 退回，归 C | 两份屏幕都不调用 `TechniqueManager.getAll`。复杂度 0 退回在 `ImprintDeskMenu.getLearnedTechniques`（约 79–84 行）。本批不写 `ImprintDeskScreen.java`、`ShuManagementScreen.java` |
| 性能汇总 | C 写 `PerfMetric` 与 `PerfCounter` | 不写 `PerformanceRecorder.java`。`buildSummary` 已按 `values()` 输出；`ticks.csv` 列是硬编码的 11 列。只在两个枚举末尾追加常量 |
| 旧幻影包 | 不改造 | `src/main/java/com/bslx/XueXiMod/shu/technique/release/phantom/` 四份文件不读不写 |

`ReleaseEntities.java`、候选查询、`ChainScheduler`、`MagicPool`、`ManaPaymentReceipt`、`TechniqueHits`、`DamagePermission`、`CastPlanCache`、纯计算 JNI 都不在本批写入范围。

## 2. 文件所有权

A 独占目录 `src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/`，包名 `com.bslx.XueXiMod.shu.technique.release.clone.core`。只许这些文件：

`CloneKind.java`、`CloneLifeState.java`、`CloneResponseState.java`、`CloneEndReason.java`、`CloneSpec.java`、`CloneHandle.java`、`CloneActorFrame.java`、`CloneTriggerInput.java`、`CloneActionRequest.java`、`CloneActionContext.java`、`CloneResults.java`、`CloneRuntime.java`、`CloneRuntimeState.java`、`CloneService.java`、`CloneEntity.java`、`CloneLifecycleEvents.java`、`CloneDecoyPolicy.java`、`ClonePositionPolicy.java`、`CloneTriggerPolicy.java`、`CloneSourceScope.java`。

B 独占：

- 目录 `src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/action/`，包名 `...clone.action`：`CloneAction.java`、`ClonePreparedAction.java`、`CloneActionResult.java`、`CloneActionCapabilities.java`、`CloneActionRegistry.java`、`ClonePulseAction.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/CloneBalance.java`，包名 `com.bslx.XueXiMod.shu.technique.release.clone`

C 独占新文件：

- `src/main/java/com/bslx/XueXiMod/shu/TechniqueAvailability.java`
- `src/main/java/com/bslx/XueXiMod/shu/TechniqueAccess.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/CloneTechniqueRegistry.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/CloneReleaseDefinition.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/CloneEntityTypes.java`
- `src/main/java/com/bslx/XueXiMod/command/CloneCommands.java`
- `src/main/java/com/bslx/XueXiMod/client/CloneRenderer.java`

C 独占既有文件：`XueXiMod.java`、`shu/Technique.java`、`shu/TechniqueManager.java`、`shu/ModTechniques.java`、`shu/balance/BalanceCatalog.java`、`shu/technique/release/ReleaseDefinitions.java`、`command/ShuCommands.java`、`event/PlayerConnectionHandler.java`、`gui/ImprintActions.java`、`gui/ImprintDeskMenu.java`、`gui/ShuManagementMenu.java`、`gui/SpellAssemblyMenu.java`、`network/SkillAssemblyPacket.java`、`network/SkillUsePacket.java`、`item/StaffSkills.java`、`debug/performance/PerfMetric.java`、`debug/performance/PerfCounter.java`、`player/PlayerShuData.java`、`src/main/resources/assets/xueximod/lang/zh_cn.json`、`src/main/resources/assets/xueximod/lang/en_us.json`。

同一文件只有一个作者。A/B 不改共享文件。结果类型嵌在已列出的文件里，不再加文件。

新标识符要能通过命名门禁：局部变量、参数、字段按驼峰和下划线拆段后，任一段不得短于 3，也不得是 `arg` `args` `arr` `avg` `cb` `cfg` `cnt` `cond` `conds` `ctx` `dir` `doc` `dto` `env` `err` `fn` `idx` `len` `loc` `max` `min` `msg` `num` `obj` `opts` `params` `pos` `proj` `req` `res` `resp` `rot` `str` `svc` `tx` `val` `vel`。配置键字符串 `"maxHealth"` 保留。Java 字段写 `bodyHealth`，不要写 `maxHealth`。不要用 `pos`、`dir`、`ctx`。

## 3. 稳定 ID 与数值单一来源

数值常量只定义在 `CloneBalance`。C 构造 `Technique` 和 `CloneReleaseDefinition` 时引用这些常量。A 的属性默认值和生成报价也引用它们。A/B/C 都不要再写一份 `10`、`20`、`2`、`8`。

```java
public static final String PHANTOM_ID = "clone_phantom";
public static final String BODY_ID = "clone_body";
public static final String PULSE_ID = "clone_response_pulse";
public static final String MANA_KEY = "manaCost";
public static final String LIFETIME_KEY = "lifetimeTicks";
public static final String HEALTH_KEY = "maxHealth";
public static final String DECOY_KEY = "decoyRadius";
public static final String FIXED_DAMAGE_KEY = "fixedDamage";
public static final String RANGE_KEY = "range";

public static final int SPAWN_MANA_COST = 10;
public static final int SPAWN_COOLDOWN_TICKS = 20;
public static final int LIFETIME_TICKS = 0;
public static final double BODY_HEALTH = 20.0d;
public static final double DECOY_RADIUS = 8.0d;
public static final int PULSE_MANA_COST = 2;
public static final int PULSE_COOLDOWN_TICKS = 20;
public static final double PULSE_DAMAGE_MULTIPLIER = 1.0d;
public static final double FIXED_DAMAGE = 2.0d;
public static final double PULSE_RANGE = 8.0d;
public static final float RAW_SPAWN_DAMAGE = 0.0f;
public static final float RAW_PULSE_DAMAGE = 2.0f;
public static final int LEVEL_CAP = 1;
public static final int COMPLEXITY = 0;
public static final float PROJECTILE_SPEED = 0.0f;
```

`PULSE_DAMAGE_MULTIPLIER` 只说明标准声明的代码默认是 1。B 不再声明 `damageMultiplier`。C 把 `PULSE_ID` 放进 `BalanceCatalog` 的 `DAMAGE_HOSTS`，让现有 `addDamage` 走 `.damageMultiplier()`。两个生成 ID 不放进 `DAMAGE_HOSTS`。

结构容量只放在 `CloneRuntime`，不是配置，B/C 不另抄：

```java
public static final int OWNER_ACTOR_CAP = 4;
public static final int SERVER_ACTOR_CAP = 128;
public static final int ACTOR_REQUEST_CAP = 1;
public static final int SERVER_REQUEST_CAP = 128;
public static final int COMMITS_PER_TICK = 32;
public static final int REQUEST_LIFE_TICKS = 2;
public static final int DECOY_LEASE_CAP = 8;
public static final int DECOY_CANDIDATE_CAP = 32;
public static final int DECOY_ATTRACT_CAP = 8;
public static final int DECOY_QUERIES_PER_TICK = 4;
public static final int PERIOD_TICKS = 20;
public static final int OWNER_RECORD_CAP = 1024;
```

显示名只此两句，由 A 在生成提交后 `setCustomName` 并 `setCustomNameVisible(true)`：幻影为 `幻影分身·基础`，普通为 `普通分身·基础`。

## 4. A 的类型与服务签名

### 4.1 枚举

```java
public enum CloneKind { PHANTOM, NORMAL }

public enum CloneLifeState { ACTIVE, ENDING, ENDED }

public enum CloneResponseState { READY, RESERVED, COMMITTING, COOLDOWN, SPENT }

public enum CloneEndReason {
    EXPIRE, DEATH, DISMISS, BUDGET_SPENT, OWNER_LOGOUT, OWNER_DEATH,
    OWNER_DIMENSION, UNLOAD, WORLD_UNLOAD, SERVER_STOP, SEQUENCE_GUARD
}
```

`SEQUENCE_GUARD` 只用于动作序号在 `Long.MAX_VALUE` 之前的保护结束。其余值对应设计第 11 节的结束原因。生命状态和响应状态分开写，禁止用 `COOLDOWN` 表示实体还活着。

### 4.2 规格、句柄、帧、触发、请求

`CloneSpec.java` 内嵌预算、寿命、站位、触发模式。首版没有第二种站位或触发模式。

```java
public record LifetimePolicy(int spanTicks) {
    public boolean expires() { return spanTicks > 0; }
}

public enum ResponseBudget { ONE, REPEAT }

public enum Station { STATIONARY }

public enum TriggerMode { MANUAL_AND_HURT }

public record CloneSpec(
        String techniqueId,
        CloneKind kind,
        LifetimePolicy lifetime,
        double bodyHealth,
        ResponseBudget budget,
        boolean endAfterBudgetSpent,
        String actionKey,
        TriggerMode triggerMode,
        Station station,
        double decoyRadiusBlocks) {}
```

幻影：`budget=ONE`，`endAfterBudgetSpent=true`，`techniqueId=PHANTOM_ID`。普通：`REPEAT`，`endAfterBudgetSpent=false`，`techniqueId=BODY_ID`。两者 `actionKey=PULSE_ID`，`triggerMode=MANUAL_AND_HURT`，`station=STATIONARY`。不保存 `ServerPlayer`。次数和 `copyCount` 不是配置。

```java
public record CloneHandle(
        long sessionToken,
        ResourceKey<Level> dimension,
        UUID actorId,
        long actorGeneration) {}
```

`actorGeneration` 从 1 起。0 表示未生成或已作废。外部不拿实体网络编号当句柄。

```java
public record CloneActorFrame(
        ServerLevel level,
        UUID actorId,
        long actorGeneration,
        UUID ownerId,
        Vec3 actorPosition,
        Vec3 eyeOrigin,
        Vec3 lookUnit,
        @Nullable UUID targetId,
        long actionSequence,
        AimSource aimSource) {
    public enum AimSource { MANUAL_DIRECTION, HURT_TARGET }
}
```

`lookUnit` 必须是有限单位向量。`level` 只活在本次服务器事务里，禁止放进静态缓存或 NBT。主动方向与反击目标用 `aimSource` 区分，不读主人的锁定缓存，也不写它。

```java
public record CloneTriggerInput(
        Source source,
        @Nullable Vec3 manualLook,
        @Nullable UUID attackerId,
        int sourceDepth) {
    public enum Source { MANUAL, HURT }
}
```

`MANUAL` 必须带请求当刻已冻结的单位视线，`attackerId` 为空。`HURT` 必须带攻击者 UUID，`manualLook` 为空。`sourceDepth` 首版主动与受击都是 0。

```java
public record CloneActionRequest(
        CloneHandle handle,
        UUID rootEventId,
        CloneTriggerInput.Source source,
        int sourceDepth,
        String actionKey,
        long requestSerial,
        long requestedTick,
        long executeTick,
        long expireTick,
        @Nullable Vec3 manualLook,
        @Nullable UUID attackerId) {}
```

只由服务器在服务器线程构造。`executeTick = requestedTick + 1`。`expireTick = requestedTick + REQUEST_LIFE_TICKS`。不写进存档。

### 4.3 动作上下文

准备发生在扣款之前，所以准备入参里没有收据，也没有已配置伤害。收据只留在 A。这是所有权拆分，不改变扣款时点。

```java
public record CloneActionContext(
        ServerPlayer owner,
        CloneActorFrame actorFrame,
        UUID rootEventId,
        long actionToken,
        CancelToken cancelToken) {
    public interface CancelToken {
        boolean cancelled();
    }
}
```

`CancelToken` 的取消方法不公开。只有 `CloneService` 能取消。B 只读 `cancelled()`。`actionToken` 由 A 分配；B 不另发一个 token。

### 4.4 服务结果

全部嵌在 `CloneResults.java`。`REPEAT_CHARGES` 表示普通分身没有递减次数。幻影提交前剩余 1，提交线之后剩余 0。

```java
public final class CloneResults {
    public static final int REPEAT_CHARGES = -1;

    public enum RejectReason {
        WRONG_OWNER, WRONG_SERVER, WRONG_DIMENSION, ACTOR_GONE, OWNER_UNAVAILABLE,
        BUSY, COOLDOWN, BUDGET_SPENT, QUEUE_FULL, CAP_REACHED, NO_TARGET, BLOCKED,
        FORBIDDEN_TARGET, INSUFFICIENT_MANA, UNSUPPORTED_ACTION, UNSUPPORTED_CHAIN,
        DEVELOPMENT_ENTRY_ONLY, INVALID_GEOMETRY, INVALID_QUOTE
    }

    public record SpawnResult(boolean accepted, @Nullable RejectReason reason, String detail,
            @Nullable CloneHandle handle, @Nullable CloneKind kind, int chargedMana,
            int responsesRemaining, int cooldownTicksRemaining) {}

    public record RequestResult(boolean accepted, @Nullable RejectReason reason, String detail,
            boolean scheduled) {}

    public record Status(UUID actorId, CloneKind kind, CloneLifeState life,
            CloneResponseState response, Vec3 actorPosition, double healthNow, double bodyHealth,
            int responsesRemaining, int cooldownTicksRemaining) {
        public String commandLine();
    }
}
```

`detail` 是给玩家看的一句中文。C 原样发送，不另做原因表。成功的 `SpawnResult.detail` 必须含 actor UUID、类型、本次实扣魔力、剩余响应。`commandLine()` 格式固定为：

`uuid=<actorId> pos=<x> <y> <z> health=<healthNow>/<bodyHealth> kind=<kind> life=<life> response=<response> charges=<responsesRemaining 或 repeat> cooldown=<cooldownTicksRemaining>`

`responsesRemaining == REPEAT_CHARGES` 时打印 `repeat`。

### 4.5 CloneService

```java
public final class CloneService {
    public static SpawnResult spawn(ServerPlayer owner, CloneKind kind);
    public static RequestResult request(ServerPlayer owner, CloneHandle handle, CloneTriggerInput input);
    public static void onAcceptedHurt(CloneEntity actor, DamageSource source);
    public static boolean dismiss(ServerPlayer owner, UUID actorId);
    public static int dismissAll(ServerPlayer owner);
    public static void end(CloneHandle handle, CloneEndReason reason);
    public static @Nullable Status status(ServerPlayer owner, UUID actorId);
    public static List<Status> list(ServerPlayer owner);
}
```

`list` 与 `dismissAll` 是第 3 节命令的消费者，补在第 5 节签名旁边。`list` 只读该主人索引，不扫世界。`dismiss` 对非本人、已结束、未知 UUID 返回 false，不退款。`dismissAll` 返回本次实际结束的数量，重复调用返回 0。

`spawn` 不接收坐标。落点是主人当前脚下，水平朝向取这次服务器看到的主人视线。忽略与主人重叠的实体碰撞；实心方块、世界边界外、未加载区域拒绝，原因 `INVALID_GEOMETRY`，不扣费。

`request` 的主动视线以入参 `manualLook` 为准，执行时不再读主人眼睛。命令当场只表示已排队或当场拒绝。主动请求在下一 tick 执行失败时，由 A 向仍在线的发起者发一次 `detail`。受击失败不发聊天。

每个公开方法先要求 `owner.getServer()` 非空且 `isSameThread()`。否则返回 `WRONG_SERVER`，不读世界。不新建线程，不用并发 map 掩盖跨线程。

### 4.6 实体、策略、作用域

```java
public class CloneEntity extends PathfinderMob {
    public CloneEntity(EntityType<? extends CloneEntity> entityType, Level level);
    public static AttributeSupplier.Builder createAttributes();
    public CloneKind kind();
    public UUID ownerId();
    public CloneLifeState lifeState();
    public CloneResponseState responseState();
    public long actionSequence();
    public static boolean rejectsIncomingAttacker(ServerPlayer owner, @Nullable Entity attacker);
    public static boolean rejectsPulseTarget(ServerPlayer owner, LivingEntity target);
    @Override public boolean hurt(DamageSource source, float incoming);
    @Override public void readAdditionalSaveData(CompoundTag tag);
    @Override public void addAdditionalSaveData(CompoundTag tag);
}
```

`createAttributes()` 使用 `PathfinderMob.createMobAttributes()`，加上 `MAX_HEALTH = CloneBalance.BODY_HEALTH`、`MOVEMENT_SPEED = 0`、`ATTACK_DAMAGE = 0`。生成提交时按当次 `CloneSpec.bodyHealth` 设置最大生命和当前生命，之后不每 tick 回写配置。`getEyeHeight` 固定 `1.74F`。空 goal、空 target goal。无自然刷怪、无拾取、无装备掉落、无经验、无繁殖。重力与环境伤害保留。击退可以离开出生点。禁止 `owner.attack`、`owner.setDeltaMovement`、挥刀附魔路径。

同步数据只在变化时写入：kind、owner UUID、生命状态、响应状态、动作序号、代次、session token。不每 tick 整包重发。

出生票嵌在 `CloneEntity` 里，包外不可伪造。`CloneService` 在 `addFreshEntity` 之前绑定票。没有票、session 不符、缺主人或缺规格时，加入后立刻 `discard`，不进入 `ACTIVE`。`readAdditionalSaveData` 只记版本诊断，读完不能变成 `ACTIVE`。需要实现的写出方法只写最小版本，不写次数、队列、冷却、收据、`ServerPlayer` 或 `ServerLevel`。

`hurt` 顺序：

1. `CloneSourceScope` 当前来源是 `CLONE_RESPONSE` 或深度大于 0：直接 false。
2. `rejectsIncomingAttacker`：主人自己、任意 `CloneEntity`、不能 `canHarmPlayer(owner)` 的其他 `ServerPlayer`、主人的可识别驯养物、与主人同队的非玩家，直接 false。环境伤害和其他生物不在这里拒绝。
3. 然后才 `super.hurt`。
4. 返回 true 且仍存活：`CloneService.onAcceptedHurt`。返回 true 但已死：`end(DEATH)`，不反击。返回 false：不反击。

`rejectsPulseTarget`：死亡、旁观、已移除、主人、任意 `CloneEntity`、`DamagePermission.allows` 为假、主人的驯养物、与主人同队的非玩家，返回 true（表示拒绝）。B 的准备和提交都调用它，不另写一套。

```java
public final class ClonePositionPolicy {
    public static CloneActorFrame frame(CloneEntity actor, CloneTriggerInput input);
}
public final class CloneTriggerPolicy {
    public static boolean accept(CloneTriggerInput input, CloneRuntimeState state);
}
```

`frame` 的眼睛取执行当刻 `actor.getEyePosition()`。主动用冻结的 `manualLook`。反击先按 UUID 在 actor 当前维度重新找存活实体，再朝其包围盒中心；找不到则让准备失败，不在这里选另一个敌人。`accept` 拒绝 `CLONE_RESPONSE`、深度大于 0、非 `READY`、冷却中、幻影额度已花、已有预约。

```java
public final class CloneSourceScope implements AutoCloseable {
    public enum Origin { PLAYER_MANUAL, CLONE_RESPONSE }
    public static CloneSourceScope open(MinecraftServer server, Origin origin, UUID rootEventId, int depth);
    public void close();
    public static @Nullable Origin origin();
    public static int depth();
    public static @Nullable UUID rootEventId();
}
```

只用服务器线程上的单层静态嵌套，禁止 `ThreadLocal` 让异步线程看见来源。`close` 恢复打开前的层。首版只在脉冲提交线之后、`commit()` 前后打开 `CLONE_RESPONSE`、深度 1。

`CloneRuntime.get(MinecraftServer)` 按服务器对象身份取运行态，启动时新 `sessionToken` 和空容器，停止后丢弃。单调 `long tick()` 每服务器 tick 加一，不用 `Level.getGameTime()`，也不用会变成负数的 `getTickCount()`。

`CloneLifecycleEvents` 自己用 `@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)` 听游戏总线：服务器启动、停止、登出、死亡、`PlayerEvent.Clone`、换维、实体离开、世界卸载、`ServerTickEvent`。不要求 C 在 `XueXiMod` 再登记一份监听。死亡克隆只清老主人，不复制分身。

tick：`START` 先把单调 tick 加一。`END` 再处理到期、队列和诱敌。队列空、没有冷却记录、没有 actor 时，`END` 第一行返回。请求在 `requestedTick` 的下一个 `END` 才可执行；`currentTick > expireTick` 时取消预约，不扣费。每个 `END` 最多提交 `COMMITS_PER_TICK` 条，FIFO。

## 5. A 调用 B，以及收据所有权

B 不调用 `ManaPaymentReceipt.charge`、`refund`、`refundShare`，不读 `MagicPool.lastReceipt`，不改 `CloneResponseState`，不写冷却，不标记幻影额度，不调用 `CloneService.end`。

A 在执行 tick、结构校验通过后：

1. `CloneActionRegistry.admit(actionKey, CloneActionCapabilities.FULL)`。不是 `ADMITTED` 则取消预约。未知键映射 `UNSUPPORTED_ACTION`，能力不足映射 `UNSUPPORTED_CHAIN`。
2. `ClonePositionPolicy.frame(...)` 得到当次帧。几何不合法则 `INVALID_GEOMETRY`，不调用准备。
3. `CloneActionRegistry.get(actionKey).prepare(context)`。
4. 准备结果 `accepted()==false`：按其余下的 `RejectReason` 取消预约，恢复 `READY`（实体已终止则不恢复），不记冷却，不扣费。不把这一步当成提交。
5. A 再查报价：魔力为 0..1000000 的整数，冷却非负，伤害有限且非负，射程有限。`BalanceValueException`、`BalanceSideException`、`BalanceDeclarationException` 或非有限数都是 `INVALID_QUOTE`，发生在扣款之前。
6. `ManaPaymentReceipt.charge(owner, quotedMana, tellPlayer)`。`tellPlayer` 仅主动请求为 true。返回 null：`INSUFFICIENT_MANA`，恢复 `READY`，不消费次数。返回的对象是这张收据，禁止再取 `lastReceipt`。魔力 0 时既有 `charge` 返回零收据而不是 null，这是成功。
7. 跨过提交线，只由 A 写入：状态 `COMMITTING`，占用 `actionToken`，幻影标记额度已花，写下该 actor 的冷却起点和准备冻结的冷却长度，动作序号加一。然后 `CloneSourceScope.open(..., CLONE_RESPONSE, rootEventId, 1)`，调用 `prepared.commit()`。
8. `commit` 正常返回后：普通分身 `COOLDOWN`，到点回 `READY`；幻影 `SPENT`，再 `end(BUDGET_SPENT)`。配置冷却乘全局后为 0 时，仍保持 `COOLDOWN` 到下一个单调 tick 才回 `READY`。这是每 actor 每 tick 一次提交，不是另一笔费用。
9. 提交线之前的失败，包括准备后、扣款后、进入第 7 步之前：先 `prepared.cancelBeforeCommit()`，再由 A 对这张收据 `refund(owner)` 一次。A 用收据上的一次性标记防止第二次 `refund`。
10. 提交线之后抛错：不退款，不恢复次数。`finally` 关掉作用域，按已提交完成额度或结束，并记一条可定位错误。禁止重试这次伤害。

`cancelBeforeCommit` 只清 B 自己冻结的目标和报价，必须幂等，不碰钱。

`commit` 的 `HitVerdict` 只记录。无敌帧、事件取消、0 点、目标中途消失都不退款、不恢复次数。禁止用 `HitVerdict.grantsSeeds()` 或布尔包装决定退款。

生成事务同样只有 A 持有收据：查位置、容量、主人、共享生成冷却；预留槽；`CloneBalance.readSpawn(BalanceSide.SERVER, kind)`；校验；`charge`；`addFreshEntity`；失败则撤销槽和索引并 `refund` 一次，不写冷却。索引发布成功才写两种生成共用的主人冷却，时长是这次来源术式的 `cooldownTicks`。此后死亡、撤回、卸载、离线不退款。

响应冷却在各 actor 上，不写 `CastCooldown`。生成冷却在主人的分身运行态里。两者都用 `CloneRuntime.tick()`。

## 6. B 的动作与平衡签名

```java
public interface CloneAction {
    String actionKey();
    ClonePreparedAction prepare(CloneActionContext actionContext);
}

public interface ClonePreparedAction {
    boolean accepted();
    @Nullable CloneResults.RejectReason reason();
    String detail();
    @Nullable ConfiguredDamage configuredDamage();
    int quotedMana();
    int quotedCooldownTicks();
    @Nullable UUID targetId();
    CloneActionResult commit();
    void cancelBeforeCommit();
}

public record CloneActionResult(@Nullable HitVerdict verdict, boolean damageCalled) {}
```

`accepted()==false` 时，伤害、魔力和目标为空，`damageCalled` 不允许出现。A 保证不调用未接受对象的 `commit()`。B 若被误调，抛 `IllegalStateException`，仍不扣费。

```java
public record CloneActionCapabilities(
        boolean actorOrigin,
        boolean actorDirection,
        boolean ownerResources,
        boolean ownerBodyUntouched,
        boolean ownerHandUntouched,
        boolean broadcastSuppressed,
        boolean delayIsolated) {
    public static CloneActionCapabilities full();
    public boolean covers(CloneActionCapabilities required);
}

public final class CloneActionRegistry {
    public enum Admission { ADMITTED, UNSUPPORTED_ACTION, UNSUPPORTED_CHAIN }
    public static void register(CloneAction action);
    public static @Nullable CloneAction get(String actionKey);
    public static Admission admit(String actionKey, CloneActionCapabilities required);
}
```

`register` 同一键第二次抛 `IllegalStateException`。未知键 `UNSUPPORTED_ACTION`。已登记但 `covers` 失败 `UNSUPPORTED_CHAIN`。不要按三个术式 ID 写 switch。

```java
public final class ClonePulseAction implements CloneAction {
    public static void register();
    public String actionKey();
    public ClonePreparedAction prepare(CloneActionContext actionContext);
}
```

`actionKey()` 返回 `CloneBalance.PULSE_ID`。`register()` 只由 C 在 `CloneTechniqueRegistry.registerAll()` 里调用一次。不要用静态初始化自己登记。能力七项全真。它不接受 `Skill` 或 `TechniqueEffect`。

准备在执行当刻做完这些事，且不做伤害、不生成实体、不扣费、不改次数：

- 主人与 actor 仍在帧里的那个 `ServerLevel`，都存活，代次相符。
- 用 `CloneBalance.readPulse(BalanceSide.SERVER)` 取这一次报价。射程、候选盒、方块射线、距离用同一个 `rangeBlocks`。
- 从 `eyeOrigin` 沿冻结方向做方块阻挡。主动在射程内用 `LivingCandidateQueries.targetQuery` 找一个合法目标，取最近射线交点，距离相同用 UUID 字典序。反击只验证冻结的攻击者 UUID 仍在射程、可见、合法。
- 目标再过 `CloneEntity.rejectsPulseTarget`。失败分别是 `NO_TARGET`、`BLOCKED`、`FORBIDDEN_TARGET`。
- 不读主人主手，不加武器、法杖、场地或宿主倍率。

`targetQuery` 的谓词只做粗筛。允许结论必须在查询之后按实时实体再判。不要再包 `PerfMetric.TARGET_QUERY`、`ENTITY_QUERY` 或 `CANDIDATE_QUERY_LOOKUP`。

提交只做：

```java
DamagePermission.allows(owner, target);
CloneEntity.rejectsPulseTarget(owner, target);
TechniqueHits.classifyRespecting(target, owner, configuredDamage.amount(), TideMarkSuccess.NONE);
```

目标已经消失则 `damageCalled=false`，仍然算已出手。禁止 `classifyRoot`、`classifyHit` 的默认绕帧、`ReleaseBatch`、`Tally`、`ChainScheduler`、`FireSeedGrant`、潮印发放、自定义击退。`DamageSource` 仍是主人的 `playerAttack`。直接实体和致伤实体都是主人。不要改 `TechniqueHits`。

### 6.1 CloneBalance

```java
public final class CloneBalance {
    public static void declareSpecials();
    public static CloneSpawnQuote readSpawn(BalanceSide side, CloneKind kind);
    public static ClonePulseQuote readPulse(BalanceSide side);

    public record CloneSpawnQuote(String techniqueId, CloneKind kind, int manaCost,
            int cooldownTicks, int lifetimeTicks, double bodyHealth, double decoyRadiusBlocks) {}

    public record ClonePulseQuote(String techniqueId, ConfiguredDamage configuredDamage,
            int manaCost, int cooldownTicks, double rangeBlocks, double rawFixedDamage) {}
}
```

`side != BalanceSide.SERVER` 时立即抛 `IllegalArgumentException`，不读客户端快照。禁止调用没有 `BalanceSide` 的 `BalanceService` 重载，也禁止经 `SpecialBalance` 转到 `current()`。

`declareSpecials` 只调用 `BalanceRegistry.declareSpecial(TechniqueBalanceSpec)`。每个生成 ID 恰好三个专项：

- `integerParameter(LIFETIME_KEY, RawDefault.literal(LIFETIME_TICKS), BalanceBound.count(0, 1_200_000), BalanceUnit.TICKS, "没有自然到期写 0，正数从生成提交的服务器单调 tick 起算")`
- `numberParameter(HEALTH_KEY, RawDefault.literal(BODY_HEALTH), BalanceBound.inclusive(1, 1024), BalanceUnit.COUNT, "生命值（非实体个数）")`
- `numberParameter(DECOY_KEY, RawDefault.literal(DECOY_RADIUS), BalanceBound.inclusive(0, 16), BalanceUnit.BLOCKS, "诱敌半径，0 关闭")`

脉冲 ID 恰好两个专项：

- `numberParameter(FIXED_DAMAGE_KEY, RawDefault.literal(FIXED_DAMAGE), BalanceBound.inclusive(0, 1_000_000), BalanceUnit.COUNT, "生命伤害")`
- `numberParameter(RANGE_KEY, RawDefault.literal(PULSE_RANGE), BalanceBound.inclusive(0.1, 32), BalanceUnit.BLOCKS, "脉冲射程")`

不声明 `manaCost`、`cooldownTicks`、`damageMultiplier`。不调用 `BalanceService`。C 只在 `ModTechniques.declareSpecialBalances()` 里调用一次 `declareSpecials()`，不修改 `CloneBalance`。

读取：

```java
int mana = BalanceService.manaCost(BalanceSide.SERVER, techniqueId, MANA_KEY, defaultMana);
int cooldown = BalanceService.cooldownTicks(BalanceSide.SERVER, techniqueId, defaultCooldown);
int lifetime = BalanceService.intValue(BalanceSide.SERVER, techniqueId, LIFETIME_KEY);
double health = BalanceService.number(BalanceSide.SERVER, techniqueId, HEALTH_KEY);
double decoy = BalanceService.number(BalanceSide.SERVER, techniqueId, DECOY_KEY);

double rawFixed = BalanceService.number(BalanceSide.SERVER, PULSE_ID, FIXED_DAMAGE_KEY);
ConfiguredDamage damage = BalanceService.damageContribution(
        BalanceSide.SERVER, PULSE_ID, RawDamage.of(rawFixed));
double range = BalanceService.number(BalanceSide.SERVER, PULSE_ID, RANGE_KEY);
```

`RawDamage.of` 的输入是刚读出的 `fixedDamage`，不是 `ConfiguredDamage.amount()`。全局伤害倍率只在 `damageContribution` 里乘一次。不要再乘 `PULSE_DAMAGE_MULTIPLIER` 或主人攻击属性。

A 只在生成事务调用 `readSpawn`。B 只在每次 `prepare` 调用 `readPulse`。生成参数随后冻进 `CloneSpec`。响应参数只冻进这一次 `ClonePreparedAction`。

## 7. A 调用 C，C 调用 A

### 7.1 实体类型

```java
public final class CloneEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES;
    public static final RegistryObject<EntityType<CloneEntity>> CLONE;
    public static final String REGISTRY_PATH = "clone_actor";
    public static EntityType<CloneEntity> cloneType();
    public static void register(IEventBus eventBus);
}
```

注册体：

```java
EntityType.Builder.of(CloneEntity::new, MobCategory.MISC)
        .sized(0.6F, 1.95F)
        .clientTrackingRange(10)
        .updateInterval(3)
        .noSave()
        .build(REGISTRY_PATH)
```

无刷怪蛋，无生物群系生成。C 在 `XueXiMod.registerContent` 里调用 `CloneEntityTypes.register(modEventBus)`，不改 `ReleaseEntities`。C 在模组总线增加属性事件，只此一行业务：

```java
event.put(CloneEntityTypes.cloneType(), CloneEntity.createAttributes().build());
```

A 生成时只用 `CloneEntityTypes.cloneType()`，不自己 `DeferredRegister`。

### 7.2 三个术式与来源

```java
public final class CloneTechniqueRegistry {
    public static void registerAll();
    public static CloneSpawnProfile profile(CloneKind kind);
    public static Technique technique(CloneKind kind);
    public static Technique pulse();
    public static String actionKey();

    public record CloneSpawnProfile(String techniqueId, CloneKind kind, String actionKey) {}
}
```

`registerAll` 的唯一调用点是 `ModTechniques.registerAll`，并且要在 `ReleaseDefinitions.registerAll` 之前完成三件套的 `TechniqueManager.register`。释放定义由 `ReleaseDefinitions.registerAll` 调用 `CloneReleaseDefinition.registerAll()`。`declareSpecials` 不在 `registerAll` 里。

`profile` 只按 `CloneKind` 返回，不让 A 比较术式 ID。`actionKey()` 是 `PULSE_ID`。A 在生成前确认 `technique(kind).availability()` 和 `pulse().availability()` 都是 `DEVELOPMENT_ONLY`，否则 `DEVELOPMENT_ENTRY_ONLY`，不扣费。

C 使用这条既有构造，不要改旧构造的参数顺序：

```java
new Technique(id, name, TechniqueType.MAGIC,
        rawDamage, cooldownTicks, CloneBalance.LEVEL_CAP, description,
        CloneBalance.COMPLEXITY, ShuAttribute.NONE, CloneBalance.PROJECTILE_SPEED,
        false, TechniqueAvailability.DEVELOPMENT_ONLY)
```

| ID | 名称 | rawDamage | cooldown | explicitManaCost |
|---|---|---|---|---|
| `clone_phantom` | 幻影分身·基础 | `RAW_SPAWN_DAMAGE` | `SPAWN_COOLDOWN_TICKS` | `SPAWN_MANA_COST` |
| `clone_body` | 普通分身·基础 | `RAW_SPAWN_DAMAGE` | `SPAWN_COOLDOWN_TICKS` | `SPAWN_MANA_COST` |
| `clone_response_pulse` | 分身基础脉冲 | `RAW_PULSE_DAMAGE` | `PULSE_COOLDOWN_TICKS` | `PULSE_MANA_COST` |

说明文字写明开发命令入口、默认不装配、上表代码默认，以及世界配置下次开世界生效。脉冲说明写明调整伤害不改变默认耗魔。注册期不调用 `BalanceService`。本局实扣数以命令回执为准。

```java
public final class CloneReleaseDefinition implements ReleaseDefinition {
    public static void registerAll();
    public String id();
    public ReleaseKind kind();
    public ReleaseTag tag();
    public Trajectory trajectory();
    public boolean moving();
    public int lifetimeTicks();
    public float speed();
    public float thickness();
    public ChainRole role();
    public int explicitManaCost();
}
```

三份定义都是 `ReleaseKind.ACTION`、`ReleaseTag.TRIGGER`、`Trajectory.STRAIGHT`、`moving()==false`、`lifetimeTicks()==1`、`speed()==0F`、`thickness()==0F`。这里的 1 只为满足接口，不是分身寿命；分身寿命只读 `CloneSpec.lifetime`。不要调用 `ReleaseDefinitions` 的私有 `define`。不要声明 `selfStatus()`。`supportsStructuralPlanCache()` 保持接口默认 false。

生成两份 `role()==ChainRole.ACTION`。脉冲 `role()==ChainRole.ATTACK`。`explicitManaCost()` 使用第 3 节常量，禁止保持默认 `-1`，否则生成费会变成 0，脉冲费会按伤害推成 4。`explicitAssemblyCost()` 保持默认 `-1`。不因此获得玩家装配资格。

`CloneTechniqueRegistry.registerAll` 同时为三个 ID 登记防御效果：

```java
TechniqueEffectRegistry.register(id, (player, technique, weapon, cast) -> {
    throw new IllegalStateException(TechniqueAccess.DEVELOPMENT_ENTRY_ONLY);
});
```

这不是成功消费者。正常命令路径不进入 `TechniqueEffect.execute`。不要登记空 lambda。

### 7.3 命令

```java
public final class CloneCommands {
    public static LiteralArgumentBuilder<CommandSourceStack> branch();
}
```

`ShuCommands.register` 在现有 `literal("shu")` 上追加 `.then(CloneCommands.branch())`。`branch` 以及 `spawn`、`list`、`respond`、`dismiss` 各自 `.requires(source -> source.hasPermission(2))`。发起者必须是 `getPlayerOrException()` 得到的在线 `ServerPlayer`。没有替他人指定主人的参数。

| 命令 | 调用 | 回执 |
|---|---|---|
| `/shu clone spawn phantom` | `CloneService.spawn(owner, PHANTOM)` | 发送 `detail` |
| `/shu clone spawn normal` | `spawn(owner, NORMAL)` | 同上 |
| `/shu clone list` | `list(owner)` | 逐行 `commandLine()`，空则一句「没有分身」 |
| `/shu clone respond <actorUuid>` | `request(owner, handle, MANUAL)` | 只发送当场 `detail`。延迟失败由 A 另发一次 |
| `/shu clone dismiss <actorUuid>` | `dismiss` | true：「已撤回」；false：「不在本人索引」 |
| `/shu clone dismiss all` | `dismissAll` | 发送结束数量 |

`respond` 的视线是命令执行当刻 `owner.getLookAngle()` 归一化后放进 `CloneTriggerInput`。句柄的 `sessionToken`、维度和代次由 A 按该 UUID 的本人索引补齐；C 不自己拼一个代次为 0 的句柄去探活。C 不调用 `onAcceptedHurt`、`end` 或 `commit`。

### 7.4 渲染

```java
public class CloneRenderer extends LivingEntityRenderer<CloneEntity, ZombieModel<CloneEntity>> {
    public CloneRenderer(EntityRendererProvider.Context rendererContext);
}
```

模型是 `ZombieModel`，贴图是原版 `textures/entity/zombie/zombie.png`。两种分身同一模型。不使用 `PlayerRenderer`，不读取玩家皮肤。C 在 `XueXiMod` 现有 `onRegisterEntityRenderers` 里：

```java
event.registerEntityRenderer(CloneEntityTypes.cloneType(), CloneRenderer::new);
```

客户端记住上次 `actionSequence()`。序号变化时调用一次 `entity.swing(InteractionHand.MAIN_HAND)`。不发包，不调用 `CloneService`。

语言文件只加实体键：`entity.xueximod.clone_actor`，中文「分身」，英文「Clone」。命令句用 A 的 `detail` 和 `commandLine()`。开发入口拒绝句用下一节常量。不新增空键。

### 7.5 开发入口

```java
public enum TechniqueAvailability { PLAYER, DEVELOPMENT_ONLY }

public final class TechniqueAccess {
    public static final String DEVELOPMENT_ENTRY_ONLY = "DEVELOPMENT_ENTRY_ONLY";
    public static final String DENIAL_TEXT = "该术式只属于开发入口";
    public static boolean allowsPlayerUse(Technique technique);
    public static List<Technique> playerCatalog();
    public static boolean skillAllowed(Skill skill);
}
```

`Technique` 新增最后一参的重载，旧四条构造原样转入 `PLAYER`。新增 `availability()`。不改 `learnTechnique(String)` 的签名；方法体内若已登记且为 `DEVELOPMENT_ONLY` 则直接返回，不写入。外层提示用 `DENIAL_TEXT`。

`TechniqueManager.playerCatalog()` 委托 `TechniqueAccess.playerCatalog()`。`getAll()` 仍返回全部，供 `BalanceCatalog.noteBuiltins` 和封存。`getByType`、`getSwordCompatible` 若被显示或装配使用，也要滤掉开发项；封存路径继续用 `getAll()`。

`skillAllowed` 在任一段已登记且 `DEVELOPMENT_ONLY` 时返回 false。未登记段保持 `Skill.fromNbt` 的现有丢弃行为，不在这里静默删掉已登记段。

C 把下列入口改到玩家目录或 `skillAllowed`，普通术式只多这一次判断：

- `PlayerConnectionHandler` 复杂度 0 自动学习改为遍历 `playerCatalog()`。
- `ShuCommands` 的 list、learn 单个、learn all、reset 全学会、stone、book write：列表用玩家目录。显式点名开发项时回复 `DENIAL_TEXT`，不发空石、不发空书、不改学习状态。`/shu learn` 不能绕过。
- `ImprintDeskMenu` 两处 `getAll`，含客户端缓存为空时的复杂度 0 退回；退回也先限制在 `playerCatalog()`。`ImprintActions.imprint` 先 `allowsPlayerUse`。
- `ShuManagementMenu.getTechniquesInCategory` 使用玩家目录。
- `SkillAssemblyPacket` 两条服务端装配、`SpellAssemblyMenu.rejectReason` 与 `mayPlace`、`StaffSkills.castRefusal`。
- `SkillUsePacket.offerNow`：技能已解析后，`HeldSwordGate`、冷却忙碌和任何使用计数之前，整链 `skillAllowed`。`executeSkill` 顶部再查一次，位置在 `BatchReleaseCompiler` / `BatchCastSession`、纯附魔、`CastExecutionSession`、普通预扣之前。失败不推进游标、不写学习或升级、不保留按住重试。

这些拒绝使用 `DENIAL_TEXT`，原因码字符串是 `DEVELOPMENT_ENTRY_ONLY`。不要按三个 ID 写 switch。

## 8. 性能常量与调用点

C 只在枚举末尾追加，不重排旧常量，不改 `PerformanceRecorder`，不改 `ticks.csv` 表头。

`PerfMetric` 末尾，紧跟 `COMPUTE_BACKEND`：

```java
CLONE_TICK,
CLONE_RESPONSE
```

`PerfCounter` 末尾，紧跟 `COMPUTE_JAVA_FALLBACKS`：

```java
CLONE_SPAWNS,
CLONE_ENDS,
CLONE_RESPONSE_REQUESTS,
CLONE_RESPONSE_COMMITS,
CLONE_RESPONSE_REJECTIONS,
CLONE_DECOY_PASSES
```

既有调用形态不变：

```java
long started = PerformanceRecorder.begin();
PerformanceRecorder.end(PerfMetric.CLONE_TICK, started);
PerformanceRecorder.add(PerfCounter.CLONE_SPAWNS, 1L);
```

只有 A 调用。B 和 C 不调用，避免双计。

| 常量 | 位置 |
|---|---|
| `CLONE_TICK` | `CloneRuntime` 的 `END` 处理整体。空闲首行返回时不 `begin` |
| `CLONE_RESPONSE` | 从取出一条到期请求到该条 `finally`，包住准备、扣款和提交 |
| `CLONE_SPAWNS` | 索引发布成功后加 1。失败不加 |
| `CLONE_ENDS` | 首次进入 `ENDED` 加 1。重复 `end` 不加 |
| `CLONE_RESPONSE_REQUESTS` | `READY` 变为 `RESERVED` 时加 1。当场拒绝不加 |
| `CLONE_RESPONSE_COMMITS` | 跨过提交线时加 1 |
| `CLONE_RESPONSE_REJECTIONS` | 每条被拒绝的主动或受击请求加 1，含排队后取消。同一条只加一次 |
| `CLONE_DECOY_PASSES` | 实际执行一次诱敌查询加 1。半径 0、未轮到、超过每 tick 4 次都不加 |

不要把 `map.size()` 记成这些计数。关闭采样时不读 `nanoTime`。不要为计数再执行一次业务调用。

## 9. 诱敌与其他 A 内部责任

`CloneDecoyPolicy` 只由 A 的 tick 调用。半径按该 actor 生成时冻结的 `decoyRadiusBlocks`。0 则整段跳过。查询用 `LivingCandidateQueries.entityQuery`，盒子不超过该半径。候选必须同时是 `Enemy`，并且是 `Zombie` 或 `AbstractSkeleton`。不遍历注册表，不按 boss 血条猜测。

每 20 个单调 tick 一轮，按 actor 序号错开。全服每 tick 最多 4 次查询。每次最多看 32 个候选、新吸引 8 个。只对当前无目标、或目标正是主人的个体 `setTarget(actor)`。每 actor 最多记 8 个 UUID，不持有实体强引用。结束时只清这 8 个里仍把目标指回该 actor 的个体。

查询之后再核 `Mob.canAttack`、视线、目标和生命周期。不要再包查询计时。

## 10. 并行

可以按 A/B/C 同时写。三组路径没有交集。

调用环只存在于已冻结的签名上：A 调用 B 的准备/提交/报价和 C 的 `cloneType`、`profile`、`actionKey`；B 调用 A 的上下文、帧、实体目标拒绝和 `RejectReason`；C 调用 A 的 `CloneService` 与 `createAttributes`，并调用 B 的 `declareSpecials`、常量、`ClonePulseAction.register`。谁也不写对方的文件。

三方都停笔之前，源码还不能组成可编译闭环。本单不授权编译，也不提前写 A 的实现。

## 11. 不在本次合同里改的行为

不改旧链结算顺序，不把这三个 ID 放进玩家装配后的成功施放，不接入任意旧效果、FakePlayer、`commitNow`、`ChainScheduler`、共享 `Tally`。不持久化分身。不制作新模型。不改表，不打包，不启动游戏。
