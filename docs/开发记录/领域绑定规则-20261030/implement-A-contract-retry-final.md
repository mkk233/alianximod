任务要求把契约写进指定文件。我先核对说明是否有变化，并确认该文件现在是否已经存在。契约文件仍未落盘。当前会话不能写文件，因此没有创建 `docs/开发记录/领域绑定规则-20261030/implementation-contract.md`。源码未改，也未编译。下面就是该文件的完整正文，可直接保存后派发。没有必须改语义才能解开的接口冲突。

# 领域绑定实施契约

设计依据是 `design-decision-1.md` 第 1–6 节。设计 1/2 已定案，实现 0/3。不重做接触计费，不改 `MagicPool`、`ManaPaymentReceipt` 的资金来源。包名以现有文件为准。

依赖方向只允许 `network → bind`、`network → domain`（现状）、`domain → bind`、`aim/实体效果 → bind`。`bind` 不引用 `DomainService`、`DomainInstance`、`CastFrame`、`TechniqueHitSpec`、`LaunchAim`。禁止在 `network` 与 `domain` 之间新增互相 import。C 不引用 `network` 里的类，只调用 `bind`。

## 1. C 登记的固定身份

C 独占并实现：

- `ChainRole.RULE_DOMAIN_BIND`
- `ModTechniques`：标识 `rule_domain_bind`，名称「领域绑定」，`TechniqueType.UNIVERSAL`，无属性，基础伤害 0，效果空实现
- `ReleaseDefinitions`：`ReleaseKind.RULE`，角色 `RULE_DOMAIN_BIND`，`chainEnergyScale` 为 1，`explicitManaCost` 0，`explicitAssemblyCost` 0
- 继续用 `textures/gui/technique/other_none.png`，不新增语言文件和贴图
- `DomainGeometry` 新增，且只由 `AxisBoxGeometry` 实现：

```java
Vec3 samplePoint(net.minecraft.util.RandomSource random);
Vec3 clampPoint(Vec3 point);
```

采样和钳制都用 `bounds()`，不用 `queryBox()`。各轴独立均匀取样，并避开浮点边缘：每轴在 `[min+1.0E-4, max-1.0E-4]` 内取点；跨度不大于 `2.0E-4` 时取该轴中点。`clampPoint` 把点钳进同一闭区间。`DomainGeometry` 不实现 `bind` 的接口，避免几何包反过来依赖契约包。

## 2. A 独占的契约包

包：`com.bslx.XueXiMod.shu.technique.release.bind`。只由 A 创建这些类型。

```java
public record StructureFault(int index, String message) {}

public final class BindingLayout {
    public static BindingLayout compile(Skill rawSkill);
    public Skill projectedSkill();
    public int originalIndex(int projectedIndex);
    public int originalFromStep(List<Integer> projectedStepIndexes, int localActionIndex);
    public BoundReleasePlan planAtDomain(int originalDomainIndex); // 无绑定则为 null
    public boolean failed();
    public List<StructureFault> faults();
}

public final class BoundReleasePlan {
    public int originalDomainIndex();
    public int originalBindIndex();
    public List<BoundMember> members(); // 攻击宿主，双重时恰好两个，顺序 A 再 B
    public List<Technique> frozenTechniques();
    public List<ShuAttribute> frozenEmbryos();
    public boolean dual();
}

public final class BoundMember {
    public int localIndex();
    public int originalIndex();
    public Technique technique();
    public List<Integer> enchantLocalIndexes();
    public ReleaseInput chainInput();
}

public enum ReleaseInputMode { MANUAL, DOMAIN_AUTOMATIC }

public interface ReleaseVolume {
    Vec3 clampPoint(Vec3 point);
}

public interface TargetCheck {
    @Nullable LivingEntity stillValid(@Nullable UUID targetId);
}

public interface LifetimeToken {
    boolean valid(int now);
}

public interface RoundHandle {
    UUID roundId();
    void cancel();
    boolean cancelled();
}

public final class ReleaseExecutionContext {
    public static ReleaseExecutionContext current();
    public static void bind(ReleaseExecutionContext context);
    public static void unbind();
    public static ReleaseExecutionContext automatic(
            ServerLevel level, ServerPlayer owner, UUID domainInstanceId, UUID roundId,
            Vec3 origin, Vec3 baseDirection, @Nullable UUID localTargetId,
            ReleaseVolume volume, TargetCheck targetCheck, ItemStack weaponCopy,
            float weaponBonus, long roundSeed, LifetimeToken token);
    public ReleaseInputMode mode();
    public ServerLevel level();
    public ServerPlayer owner();
    public UUID domainInstanceId();
    public UUID roundId();
    public Vec3 origin();
    public Vec3 baseDirection();
    public @Nullable UUID localTargetId();
    public ReleaseVolume volume();
    public TargetCheck targetCheck();
    public ItemStack weaponCopy();
    public float weaponBonus();
    public long roundSeed();
    public LifetimeToken token();
}

public final class BoundReleases {
    public interface Executor {
        RoundHandle executeAutomatic(BoundReleasePlan plan, ReleaseExecutionContext context, LifetimeToken token);
    }
    public static void install(Executor executor);
    public static RoundHandle executeAutomatic(BoundReleasePlan plan, ReleaseExecutionContext context, LifetimeToken token);
}
```

`automatic` 在世界、主人、两个 UUID、原点、方向、体积、目标检查、武器副本或令牌缺失，或方向长度过小时抛 `IllegalArgumentException`。禁止改去读眼睛或准星。`localTargetId` 允许为空。`weaponCopy` 由调用方事先 `copy()`。上下文不可变。`bind` / `unbind` 只包住一次成员提交，延迟 5 tick 的回调要重新绑、用完即解。

`BoundReleases.executeAutomatic` 在尚未 `install` 时抛 `IllegalStateException`。A 在 `SkillUsePacket.register` 里安装 `network` 中的实现。C 不调用 `install`。

## 3. 捕获规则

`BindingLayout.compile` 不做世界操作，也不扣费。投影用 `new Skill(raw.getName(), raw.getType(), techniques, embryos)`，只保留没被捕获的原始项，顺序不变。

领域宿主：`domain() != null`、基础伤害不大于 0、不是附魔。紧接的下一项必须是 `rule_domain_bind` / `RULE_DOMAIN_BIND`。中间夹任何项，包括附魔，失败，文案「领域绑定必须紧接领域术式，中间不能夹附魔或其他术式」。标记不在领域后面，失败，文案「领域绑定必须紧接在领域术式之后」。没有这种标记的领域留在投影里。

从标记之后只接受基础附魔，以及现行 `SkillSegmentMath.isChainModifier` 为真的修饰。第一个停止点必须是基础伤害大于 0、非附魔、`ChainRole.ATTACK`，且不是领域、护体、屏障、条件释放、自身状态。领域、护体、屏障、动作、自身状态、条件释放、另一个绑定标记，以及其他既非修饰也非附魔的项，都不能跨过去找更远的攻击；失败文案带上该项名称：「领域绑定不能越过「名字」继续寻找攻击」。后面没有合格攻击：「领域绑定后面没有可绑定的攻击」。已移除规则：「链里有已移除的术式，请重新装配」。

默认捕获到这个攻击为止，含标记、沿途修饰和附魔。段内若有 `RULE_DUAL`，成员集合与 `SkillChainPlan.releasedSegments` 相同：跳过修饰，房间仍是 `pullAfterA + 1`，只取前两个非修饰宿主，保持先后，不因为第二个双重标记加大房间，也不收入第三个攻击。这两个宿主必须都是合格攻击，否则「双重释放后面缺少第二个攻击」。现有编译器会拒绝的重复组合，用同一句拒绝文案，不另做嵌套双重。

附魔在原始下标上定归属：领域前、按现行等宿主规则等着领域的，留在投影里归领域；标记之后到 A 之前的归 A；A 与 B 之间的归 B；B 之后的留在投影里。领域前的修饰不进子计划，子计划里的修饰不回到领域。聚能只在子计划自己的序列上计算，不乘领域几何倍率。

一个原始链可以有多段互不重叠的「领域 → 绑定 → 子链」。子计划里再出现领域宿主即结构失败。有任何一条失败就拒绝这次整次释放，不扣费，也不打开本链其他领域。`SkillUsePacket` 用现有 `tell` 出示 `faults()` 的第一条。

切步、`TechniqueChain.split`、`CastPlanCache.lookup` 和开域预扣都只看 `projectedSkill()`。查领域计划时用 `originalFromStep` 把当前步的局部下标变回原始下标，再 `planAtDomain`。被捕获的攻击不再出现在后续按键步。双重若作用在领域前面，只影响开域宿主，不把子攻击当成它的第二个普通成员。

## 4. 自动执行与手动分支

A 在 `network` 实现 `BoundReleases.Executor`，类名 `AutomaticRound`，外部不引用它。并从 `CastExecutionSession.GroupBudget` 抽出 `network.GroupBudget`，保留原方法；新增

```java
public static GroupBudget reserve(ServerPlayer owner, int cost, boolean tellPlayer);
```

按键会话继续 `tellPlayer == true`。自动轮使用 `false`。`reserve` 内部只在 `ManaPaymentReceipt.charge` 返回时保存那一份收据，之后禁止再读 `MagicPool.lastReceipt`。

`executeAutomatic`：

- 只用冻结子计划做现有报价。绑定规则本身费用为 0。双重一轮含两名成员；一分为三沿用 `copies` 与 `Tally`，报价乘份，不逐份再 `charge`。
- 余额不足则返回已结束、未排程的 `RoundHandle`，不生成，不关领域，不提示缺魔。
- 成功则预留整轮。成员提交成功就 `retainShare`；生成失败、持剑不合格或取消时尚未提交的份额 `refundShare`。已经生成再打空不退。
- 成员间隔仍是 `CastExecutionSession.STEP_GAP`（5）。这不是 20 tick 周期。第二成员回调先看 `handle.cancelled()` 和 `token.valid(now)`，不满足就空返回，不扣费、不生成。
- 不调用 `CastExecutionSession.open`，不 `CastCooldown.acquire`，不推进 `ChainProgress`。
- 持剑门对自动轮检查 `weaponCopy()`，不读当时主手。
- 异常时退掉本轮未提交份额，已 `retain` 的不退。

`RoundHandle.cancel()` 幂等，用本轮保存的主人和收据调用 `refundShare`，不重新查找玩家。法杖已不在可找到的背包里时，现有 `findStaff` 失败仍不退法杖那一部分；本功能不修这个旧限制。

手动路径在 `ReleaseExecutionContext.current()` 为空或 `MANUAL` 时保持现在的准星、锚点和 `LockTargetService`。`DOMAIN_AUTOMATIC` 时，公共执行器在调用效果之前写好 `CastFrame.overrideGeometry`、`origin`、`look`、`anchor`，中心和基础朝向取自上下文。禁止再调用 `LockTargetService.resolve`、`ReleaseBatch.crosshairCenter`、`resolveAnchor`。规则几何先做；随后 A 用 `volume.clampPoint` 写回 `origin` 和 `anchor`。垂落方向保持向下。`forAttack` 的种子用 `roundSeed ^ ((long) memberIndex << 16)`，不用玩家 UUID 和游戏时间。

同一轮两名成员共用这一轮的原点、基础方向和 `localTargetId`。每个成员提交前调用 `targetCheck.stillValid`。失效就用 `baseDirection()`，不重选目标，也不取消这轮。

## 5. C 如何喂给 A

20 tick 首发、之后每 20 tick、同 tick 按主人 UUID 再按实例 UUID 排序、先拍快照再执行，都在 C 的 `DomainBindingRuntime`。到期条件是 `now >= expiresAt` 即失效，不能只看 `active`。时刻跳过期只发当前一次，并推进到下一个未来节拍。

每轮开始时 C 做这些事，再调用 A：

1. `RandomSource.create(roundSeed)` 后调用 `geometry.samplePoint`。无目标时用同一随机源取均匀球面单位向量。
2. 目标只在真实 `bounds()` 内选择：存活、非旁观、非主人、通过现有关系过滤，距离从采样点算，等距取 UUID 较小者。不读、不写玩家锁定标记。
3. `ItemStack weaponCopy = owner.getMainHandItem().copy()`。`weaponBonus` 在主手仍是这一件时按现有公式 `max(0, 攻击伤害属性 - 1)` 算一次，传给 `automatic`。
4. `LifetimeToken.valid(now)` 同时要求领域 `active`、`now < expiresAt`、主人存活且在线、所在维度与宿主维度相同。
5. `ReleaseVolume` 用 C 自己的适配器转发 `geometry.clampPoint`，不把 `DomainGeometry` 放进 `bind`。
6. `BoundReleases.executeAutomatic(instance.boundPlan(), context, token)`。把返回的 `RoundHandle` 记在该实例上。`close`、死亡、下线和跨维时立刻对每个 handle 调用 `cancel()`，不等下一次调度 tick。

`DomainService` 保留旧重载，并新增：

```java
public static boolean tryWave(ServerPlayer player, Technique source, DomainDefinition definition,
        int copies, EnchantmentSnapshot snapshot, ReleaseInput input,
        @Nullable BoundReleasePlan plan, @Nullable String claimSuffix);

public static boolean tryWaveReserved(ServerPlayer player, Technique source, DomainDefinition definition,
        int copies, EnchantmentSnapshot snapshot, Vec3 feet, String claimSuffix,
        ReleaseInput input, @Nullable BoundReleasePlan plan);
```

旧方法转调时 `plan == null`。`plan != null` 且 `claimSuffix` 为空则不放置并返回 false。无绑定的去重键不变。有绑定时，A 传入的后缀是 `bind:<原始领域下标>:<本次释放UUID>`；同一次释放、同一领域下标共用一个后缀，多份 copies 仍由现有循环生成，每份实例保存同一个不可变 `BoundReleasePlan`。C 在 `DomainInstance` 上提供 `boundPlan()`。

A 必须改的开域传参点只有这三处：`SkillUsePacket` 的 `tryWave`、`SkillChainPlan.fireDualSequence` 的 `tryWave`、`CastExecutionSession.commitMember` 的 `tryWaveReserved`。三处都传同一份 `BindingLayout` 查出的计划。开域账单只含投影链，不含子计划未来费用。

## 6. B 的类型与字段

B 独占并新建 `com.bslx.XueXiMod.shu.technique.release.aim.LaunchAim`。A、C 都不创建它。

```java
public final class LaunchAim {
    public LaunchAim(Vec3 origin, Vec3 direction, @Nullable UUID localTargetId,
                     boolean automaticLifetime, long seed);
    public Vec3 origin();
    public Vec3 direction();
    public @Nullable UUID localTargetId();
    public boolean automaticLifetime();
    public long seed();
    public @Nullable LivingEntity findTarget(ServerLevel level); // 只按实体 id 解析，不碰玩家标记
}
```

B 在 `CastFrame` 增加字段 `public ReleaseExecutionContext execution;` 与 `public LaunchAim launchAim;`。A 只给 `execution` 赋值，并写现有的 `overrideGeometry`、`origin`、`look`、`anchor`。B 在生成时从 `execution` 构造 `LaunchAim`，写入 `launchAim`。效果若再次偏移发射点，必须再调用 `execution.volume().clampPoint`。垂丝唇口这类形状偏移不钳。

B 在 `TechniqueHitSpec` 增加：`sourceOrigin`、`sourceDirection`、`localTargetId`、`automaticLifetime`。由 B 从 `LaunchAim` 复制。自动持续看 `automaticLifetime`，禁止把槽位 `-1` 当成松手。手动释放的松手和换手逻辑不改。关域不删除已经生成的实体。

`StillAimMotion` 只有在必须拒绝空输入时才改。`TechniqueHitEntity`、`TrackingSupport` 本轮不动。
