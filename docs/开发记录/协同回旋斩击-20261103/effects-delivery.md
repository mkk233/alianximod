# 协同、回旋、斩击：攻击与动作交付

作者 E。只新增 `cooperative/attack`、`cooperative/action` 和本文件。没有改公共接口、旧横劈效果、命中规格、账本、共享注册、客户端、制作表或批次记录。没有调用 `ActorAttackAdapterRegistry.register`。没有复制贴图或动作曲线。

命名门禁 `tools/check-name-gate.ps1` 退出码 0。没有跑 `compileJava`，没有游戏验证，没有正式实现复审。

## 完成

- 斜斩适配 `ObliqueSlashAdapter`、横劈适配 `HorizontalSlashActorAdapter`。`prepare` / `emit` 只读准备记录、冻结刀向、已解副本和演员帧。每演员每个逻辑副本返回恰好一个 `PreparedEmit`。已配置伤害原样留在准备记录里，金额为 0 也仍给出发射，链角色不因此改变。
- 形状在 `SlashVolumeLayout`。先用已解锚点加局部偏移，再加演员眼点相对玩家基准眼点的差。刀向来自冻结瞄准，不用副本上的刀面法线或滚转。
- 回旋时间线 `SpinTimelineDefinition`：默认 6+6 刻、`IN_PLACE`、自身伤害 0。没有客户端关键帧，不写玩家视角，不在效果里扣费或冷却。
- 斜斩和回旋的术式、释放定义、能力工厂和专项声明已交出。旧效果入口会抛异常，避免被旧段循环当成已经打出。

## 未完成，交给 I

公共包没有可定位的 `FamilyExecutionRuntime`。`ExecutionFamilyPlan` 也只能由 I 降低。因此 `ObliqueSlashEffect.execute`（约第 137 行）和 `SpinActionEffect.execute`（约第 64 行）拒绝执行，不生成体积、不结算、不扣费。这不是成功的空实现。I 不要把这两个效果交给旧的 `TechniqueEffect.execute`。

本目录没有注册术式、释放、适配器、伤害宿主或语言行，也没有把 `ChainRole.RULE_COOPERATIVE` 写进枚举。

## I 要调用的工厂

每次登记适配器都使用下面返回的同一个实例。换实例会被注册表拒绝。

| 用途 | 签名 | 位置 |
| --- | --- | --- |
| 斜斩适配器 | `ActorAttackAdapter ObliqueSlashEffect.adapter()` | `action/ObliqueSlashEffect.java` 第 86 行，实例在 `ObliqueSlashAdapter.adapter()` 第 26 行 |
| 横劈适配器 | `ActorAttackAdapter HorizontalSlashActorAdapter.adapter()` | `attack/HorizontalSlashActorAdapter.java` 第 39 行 |
| 斜斩术式 | `Technique ObliqueSlashEffect.technique()` | 第 62 行 |
| 斜斩释放 | `ReleaseDefinition ObliqueSlashEffect.releaseDefinition()` | 第 78 行 |
| 斜斩能力 | `TechniqueExecutionCapabilities ObliqueSlashEffect.capabilities()` | 第 82 行，即 `explicitImmediateAttack()` |
| 斜斩立即门 | `ReleaseGate ObliqueSlashEffect.immediateGate()` | 第 95 行。出刀 0、收势 8、`IN_PLACE`，不是平衡键 |
| 斜斩表现标识 | `ResourceLocation ObliqueSlashEffect.presentationId()` | 第 90 行，`xueximod:oblique_slash`。只是标识，没有资源加载 |
| 斜斩体积读取 | `AdapterVolumeSpec ObliqueSlashEffect.serverVolume()` | 第 107 行 |
| 斜斩专项声明 | `void ObliqueSlashEffect.declareSpecialBalance()` | 第 121 行 |
| 斜斩效果对象 | `ObliqueSlashEffect ObliqueSlashEffect.effect()` | 第 58 行。只用于注册类型；不要调用 `execute` |
| 横劈体积读取 | `AdapterVolumeSpec HorizontalSlashActorAdapter.serverVolume()` | 第 47 行。不新声明横劈键 |
| 回旋术式 | `Technique SpinActionEffect.technique()` | `action/SpinActionEffect.java` 第 39 行 |
| 回旋释放 | `ReleaseDefinition SpinActionEffect.releaseDefinition()` | 第 55 行 |
| 回旋能力 | `TechniqueExecutionCapabilities SpinActionEffect.capabilities()` | 第 59 行，即 `timedRelease()` |
| 回旋时间线 | `TimelineSpec SpinTimelineDefinition.serverTimeline()` | `action/SpinTimelineDefinition.java` 第 35 行 |
| 回旋定时门 | `ReleaseGate SpinTimelineDefinition.timedGate()` | 第 42 行 |
| 回旋动作标识 | `ResourceLocation SpinTimelineDefinition.actionId()` | 第 30 行，`xueximod:spin` |
| 回旋专项声明 | `void SpinTimelineDefinition.declareSpecialBalance()` | 第 47 行 |
| 回旋效果对象 | `SpinActionEffect SpinActionEffect.effect()` | 第 35 行。不要调用 `execute` |

横劈的释放定义和能力保持原样：`TechniqueExecutionCapabilities.none()`。只有带新前缀的族才 `register(CooperativeTechniqueIds.HORIZONTAL_SLASH, HorizontalSlashActorAdapter.adapter())`。

建议登记：

```java
ActorAttackAdapterRegistry.register(
        CooperativeTechniqueIds.OBLIQUE_SLASH, ObliqueSlashEffect.adapter());
ActorAttackAdapterRegistry.register(
        CooperativeTechniqueIds.HORIZONTAL_SLASH, HorizontalSlashActorAdapter.adapter());
```

`sword_oblique_slash` 要加入 `BalanceCatalog` 的内置伤害宿主，标准 `damageMultiplier` 才会在封存时补上。`movement_spin` 底伤 0，不要补伤害倍率。两项都不要在专项里再声明 `manaCost`、`cooldownTicks` 或 `damageMultiplier`。

封存前要先登记术式和释放定义。显式魔力写在释放定义上：斜斩 4，回旋 2。`Technique.manaCost()` 会读到它们。术式卡上的弹速沿用其它剑技和动作的 1；命中体速度是释放定义的 0，斜斩不声明 `motionSpeed`。

`declareSpecialBalance` 各调用一次，放进 I 负责的专项汇总，且必须在 `BalanceRegistry.seal` 之前。不要对这两项再跑 `ReleaseDefinitions` 的通用 `motion(...)`。斜斩的 `lifetimeTicks` 已经由专项声明；通用运动声明会再登记一次并失败。回旋释放定义的寿命固定为 1，只表示没有命中体，不能当成第二套前摇或收势。

## 几何

局部原点相对眼点。`emit` 才变成这名演员的世界中心。

斜斩，`SlashVolumeLayout.oblique`：

- 纵深轴是 `AttackPreparation.frozenDirection`。水平分量长度平方小于 `1e-8` 时，改用回退偏航的水平前向。Minecraft 偏航：`x = -sin(yaw)`，`z = cos(yaw)`。
- `prepare` 没有演员，回退偏航用 0。`emit` 在退化时用 `ActorFrame.visualYaw`。I 应在构建准备记录之前，用族上的冻结水平朝向把近垂直瞄准收成水平单位向量，这样准备形状和发射刀向一致。
- 宽度的零倾角是水平右向 `worldUp × forward`。法线绕瞄准轴转过带符号倾角。正倾角把右向抬向该轴的上方向。镜像只把倾角取反。
- 中心偏移是常数 `SlashVolumeLayout.CENTER_AHEAD_BLOCKS`（1.5），沿上面的纵深轴，不随 `boxDepth` 改变。默认纵深 3 时，近端恰在眼点。
- `direction` 是纵深轴，`bladeNormal` 是厚度轴，`roll` 是带符号度数。宽向可由 `bladeNormal × direction` 还原。`pose3d` 为 true，只表示这是倾斜刀面数据。
- 不要把 `bladeNormal` 塞进旧 `SizedBoxHitbox.intersectsTiltedPlane`。那个测试把实体前向当法线，并且不用厚度。提交桥应按原点、纵深轴、法线和三边长放置，原点已经是盒子中心，不能再加半个纵深。

横劈，`SlashVolumeLayout.horizontal`：

- 纵深轴压平。水平长度小于 `1e-4` 时朝正南 `(0,0,1)`，与旧 `StillAimMotion` 相同，不用演员偏航。
- 中心在眼点前方 `boxDepth / 2`。默认纵深 3 时也是 1.5，但配置改纵深后中心跟着半深走，这是旧锚点，不是斜斩的固定 1.5。
- 法线是世界上方，滚转 0，`pose3d` 为 false。镜像不改变这个对称盒子。
- 厚度不是新键。旧效果把 `spec.height` 写成 0，碰撞竖向因此是 `max(纵深, 1)`。`serverVolume` 第 53 行把这个数写入 `boxThickness`。

落点，`SlashVolumeLayout.placeActor` 第 79–82 行：

```text
actorCenter = solved.origin + localShape.origin + (actor.eye - solved.playerEye)
```

`ResolvedCopyGeometry.origin` 必须是链落点或交错已经解完的锚点，用来代替施法时的玩家眼点，不是盒子中心。没有修饰时它等于当时的玩家眼点。`playerEye` 是那次求解用的基准眼点。出刀帧上的 `eye` 是当前身体眼点。视角变化只影响帧里的眼点差，不改已经准备好的刀向。

`overrideGeometry` 不在这里再做一次变换。I 把求解结果写入 `origin` 即可。轨迹不可变，所以副本上的刀面法线和滚转被忽略。

## 配置

读取都走 `ServerSpecialRead`：未封存或键未声明时用代码默认；封存后只调用 `BalanceService.number/intValue(BalanceSide.SERVER, ...)`。

玩法消费时机：构建 `AttackPreparation` 时调用一次 `serverVolume()`，构建定时门时调用一次 `serverTimeline()`，之后发射和门只用这份冻结值。释放定义上的斜斩寿命和厚度是同一套服务端键的登记视图，会把厚度收成 float；提交几何以 `AdapterVolumeSpec` 的 double 为准。

| 术式 | 键 | 默认 | 单位 | 范围 | 消费者 |
| --- | --- | --- | --- | --- | --- |
| `sword_oblique_slash` | `boxDepth` | 3 | `BLOCKS` | 现有正尺寸，大于 0 到 512 | 纵深 |
| 同上 | `boxWidth` | 5 | `BLOCKS` | 同上 | 刀宽 |
| 同上 | `boxThickness` | 0.5 | `BLOCKS` | 同上 | 厚度 |
| 同上 | `lifetimeTicks` | 4 | `TICKS` | 现有正刻，1 到 1200000 | 发射寿命 |
| 同上 | `slashTiltDegrees` | 35 | `MULTIPLIER` | 闭区间 -80 到 80 | 绕瞄准轴的倾角。没有角度枚举，描述写明是度 |
| `movement_spin` | `windupTicks` | 6 | `TICKS` | 闭区间 1 到 20 | 定时门前摇，`releaseTick()` |
| 同上 | `recoveryTicks` | 6 | `TICKS` | 闭区间 1 到 40 | 定时门收势 |
| `sword_horizontal_slash` | 已有 `boxDepth` | 3 | 沿旧声明 | 沿旧声明 | 横劈适配纵深 |
| 同上 | 已有 `boxWidth` | 5 | 沿旧声明 | 沿旧声明 | 横劈适配刀宽 |
| 同上 | 已有 `lifetimeTicks` | 4 | 沿旧声明 | 沿旧声明 | 横劈适配寿命 |

斜斩标准键仍是魔力 4、冷却 20、伤害倍率 1，由封存补，不在专项重复。回旋标准键是魔力 2、冷却 10，没有伤害倍率。回旋不声明寿命、速度或厚度。单独斜斩不依赖回旋或分身；回旋没有攻击时也不由本效果生成体积。

## 风险

- 斜斩中心固定在瞄准轴前方 1.5。`boxDepth` 变大时盒子会向眼后延伸。默认深 3 时近端仍在眼点。
- 近垂直瞄准如果没有事先收成水平刀向，`prepare` 用偏航 0，`emit` 用演员表现偏航，两份刀向会不同。
- 横劈近垂直时朝正南，这是旧运动的行为。
- `pose3d == true` 不能直接复用旧倾斜薄片测试，否则厚度 0.5 不会生效。
- 效果入口会抛 `IllegalStateException`。旧段循环如果调用它，这一发会失败，而不是静默打出。
- 伤害宿主没进 `BalanceCatalog` 之前，内置斜斩会在封存时警告缺少标准伤害倍率。那一行只能由 I 加。
