我先读这份定位任务的授权范围，确认能读什么、能不能改文件。这是只读定位。我按指定引用核对剑尖、火环几何和实际结算路径，只写这份定位报告。环的起点、半径和伤害调用已经对上现有符号。正在把限定定位写成报告，不改源码。限定定位已写入 `docs/开发记录/术式批量制作/火系六式-20261029/liaoli-tip-ring-burst-locate-report.md`。12322 字节，最后写入 2026-10-30 02:55:25，SHA256 `582FC828517D9C2272C1B6A3A28E7D7C3892C64E125A72CF7E850B1367639581`。没有改 Java、贴图或制作表，没有编译、测试或提交。这不是复审结论。

# 燎里火环与低伤反馈：实施前限定定位

会话 `23ef4660-da4b-47a8-9341-8ec236ec5cc3`。本单只读相关源码，只写本文件。没有改 Java、贴图、制作表、`记录.md` 或资源库索引，没有编译、测试、启动游戏或提交。不是独立复审，不给通过或失败结论。设计 2/2、实现 3/3 承接，额度已用完，本定位不追加复审。

环的几何改动可以另单做，但要等制作表实际退回「制作中」之后。本单不核对、不改表。伤害系数仍由主会话讨论，没有任何新数值获批。下面的格数是按当前布局公式对站立姿态（俯仰 0、俯冲量 0、眼高 1.62）做的换算，用来对照现有环和剑阵，不是新设计参数，也不是测试运行结果。

已批准并继续保留的需求：后环只在有效命中且扣种前余额严格大于 50 时出现；火种常驻火苗；v7 剑阵姿态和现有六张燎里贴图；40 tick 寿命、40 魔力、300 tick 冷却、每个目标一次。输入快照 `liaoli-hit-seed-transition-snapshot.json` 的批准详细需求仍是基础 6 点物理，另加 `0.36×S×(1+F)` 即时火伤；100 点、F=0 时追加 36，合计 42（不计其他加成与减伤）。

## 1. 火环为什么不从手持剑尖长出

手持剑尖和三道环用的是同一套视线偏航基，但不是同一个点。

`LiaoliV6Renderer.render` 把环、背后剑和手持刺击画在同一次姿态里。环和手持剑都走 `Basis.fromYaw(lookTurn)`。背后八剑另走身体偏航和身体俯仰。环没有挂到 `thrustTip()` 上。

`drawThrust` 的剑尖是 `LiaoliV6Layout.thrustTip()`，即 `fromPixels(12, 20, 73)`。像素换算是 `BLOCKS_PER_PIXEL = 1.8/64`，眼高像素 58，正深度朝前。站立换算后相对眼睛：

- 横向（玩家左侧）`+0.3375` 格
- 高低 `-1.06875` 格
- 前后 `+2.053125` 格

`drawRings` 的中心是 `ringCenter(progress)`，即 `fromPixels(0, 25, -17 - 65×progress)`。进度 0 时已经在眼睛后方，不是剑尖：

- 横向 `0`
- 高低 `-0.928125` 格
- 前后 `-0.478125` 格（深度像素 −17）

进度 1 时前后变成 `-2.30625` 格（深度像素 −82），高低不变。从剑尖到进度 0 的环心，差是横向 `−0.3375`、高低 `+0.140625`、前后 `−2.53125`，直线距离约 `2.558` 格。环从出生起就在眼睛后面，再沿视线后方走，不会经过前方 2.05 格处的剑尖。

半径是另一条直线，也不从剑尖量起：

- `ringRadius(progress) = (14 + 43×progress) × 1.8/64`
- 进度 0：环线半径 `0.39375` 格
- 进度 1：环线半径 `1.603125` 格

`drawRings` 把这个半径再乘 `RING_QUAD_SCALE = 48/32`。注释写明贴图环线大约在 96 像素画面的 32 像素处，方片半边放大后环线才落在空间半径上。进度 1 的方片半边是 `2.4046875` 格。可见圆是环线，方片多出来的一圈是贴图边。

三道环共用一个 24 tick 窗口，相位差 `ringIndex / 3`：

`ringProgress = age/24 + ringIndex/3`，再取小数部分。

`rearRingVisualAge` 在年龄达到传入的周期时返回 −1。渲染传入的周期是 `RING_PERIOD = 24`。年龄刚到 24 之前：

- 第 0 环进度约 1，环线约 `1.603` 格，深度像素约 −82
- 第 1 环进度约 `1/3`，环线约 `0.797` 格，深度像素约 −39
- 第 2 环进度约 `2/3`，环线约 `1.200` 格，深度像素约 −60

所以一次触发里只有一道环走到布局函数的末端。另外两道在中途被同一个 24 tick 截止剪掉。

## 2. 末端半径和背后剑阵

站立、俯仰 0、俯冲量 0、眼高 1.62 时，按 `droppedTip`、`posed` 和 `FLAME_PAST_BLADE = (118−62)/(62−23)` 换算：

- 八个剑尖离眼睛最远约 `1.663` 格，左右极端约 `±1.0125` 格
- 火舌末端离眼睛最远约 `2.963` 格，左右极端约 `±1.901` 格
- 把这些点投到环所在的横向–竖直平面，相对进度 1 的环心：剑尖最远约 `1.073` 格，火舌末端最远约 `2.316` 格

对照：

- 末端环线 `1.603` 格大于剑尖的平面跨度 `1.073` 格
- 末端环线 `1.603` 格小于火舌末端的平面跨度 `2.316` 格，也小于火舌离眼睛的 `2.963` 格
- 末端方片半边 `2.405` 格略大于火舌的平面跨度 `2.316` 格。那是贴图卡片的边，不是环线

环心在眼睛后 `2.306` 格，剑尖大约只在眼睛后 `0.31` 格。圆盘和剑阵不在同一深度。横身俯冲时八剑改走身体俯仰并下移到腿部，环仍在视线偏航、世界上方的直立平面上，两者进一步分开。

消散不由透明度决定。`ringAlpha(1) = 0.65`，进度走完仍然可见。停画条件是 `rearRingVisualAge`：出生时刻缺省 −1，或年龄 `< −2`，或年龄 `≥` 传入周期，都返回 −1，`render` 就不再调用 `drawRings`。周期来自 `RING_PERIOD`，24 tick。`pulseRearRing` 只写世界时刻，注释写明不缩短命中体寿命。命中体自己的 40 tick、松开、换剑或死亡移除，会把整段视觉一起收掉，包括还没走完的环。同一世界时刻重复 `pulseRearRing` 不会重开一轮；更晚的另一次合格命中会改写出生时刻。

## 3. 保留现有环贴图的最小落点

不改 `rear_ring.png` 像素时，贴图接线保持：

- 路径常量 `LiaoliV6Layout.REAR_RING`（`textures/effect/liaoli_v6/rear_ring.png`）
- `RING_FRAMES = 24`，`drawRings` 里按帧取 V
- `RING_QUAD_SCALE = 48/32`，让环线继续落在 `ringRadius` 上

几何只动这些符号：

- `ringCenter`：进度 0 应对齐 `thrustTip()` 的 `(横向 12, 高低 20, 深度 73)`，再随进度把深度改向负值，也就是朝玩家身后
- `ringRadius`：末端环线要超过背后剑阵外缘之后，`rearRingVisualAge` 才停画。以站立火舌平面跨度约 `2.316` 格为当前外缘；俯冲姿态外缘会再变
- `ringProgress` 的 `ringIndex/3` 相位，或传入 `rearRingVisualAge` 的 `RING_PERIOD`：现在两道环到不了进度 1。若三道都要从剑尖长到超过剑阵，现有相位或 24 tick 截止就是挡住它的符号

`LiaoliV6Renderer.drawRings` 已经用视线偏航放置 `ringCenter`。标记改对之后，渲染函数不必另造父级。只有调用形状要改时才动它。`TechniqueHitEntity.pulseRearRing` 和 `>50` 门槛不负责环的起点。

## 4. 100 点火种时两段伤害怎么进 hurt

登记值在 `ModTechniques.FIRE_LIAOLI`：基础伤害 `6f`，冷却 `300`。`LiaoliEffect.execute` 把 `technique.getBaseDamage()` 放进监听器，并设 `suppressRootSettle = true`。因此 `TechniqueHitSpec.spawn` 不会把 `CastFrame.damage` 抄进 `settleDamage`，`TechniqueHitSettle.settle` 也不会对燎里再打一刀根结算。

一次被接受的命中，`SeedHit.onHit` 的顺序是：

1. `DamagePermission.allows` 失败则整段返回，物理和火伤都不打。
2. 目标火种 `> 0` 时调用 `FireConsumption.detonate(..., ALL_SEEDS, FireConsumerPlans::thrust)`。
3. `physical > 0` 时调用 `TechniqueHits.hurtRoot(target, player, 6f, NONE)`。
4. 成功快照的 `amount() > 50` 才 `pulseRearRing`。环不改伤害。

火伤公式在引爆调用里闭合。`thrust` 写 `BurnComponent.of(0.36 × amount, 0)`，也就是 B=`0.36×S`，A=0。`FireLayer.combine` 是 `B × (1 + A + F)`。S=100、F=0 时是 `36 × (1+0+0) = 36`。F 不为 0 时是 `36×(1+F)`，例如既有记录里的 F=0.30 得 46.8。`strikeOnce` 的第四个参数是 B，第五个参数 `furnaceSettled=true`。`FireRuntimeEvents.onHurt` 看到已结算就不再加炉心；`ElementAdjustment.fireAmount` 在已结算时原样返回。物理 6 不进入 `combine`。

两段是两次独立的 `target.hurt`：

- 火伤：`FireDamage.strikeOnce` 使用 `xueximod:fire_resource`。类型文件 `scaling` 为 `never`。该类型在 `is_fire` 和 `bypasses_cooldown` 里，本仓库没有把它放进 `bypasses_armor`。`strikeOnce` 不改无敌时间。
- 物理：`hurtRoot` 先 `DomainAuraResolver.merge`。合并只加施法者当时所在、且允许领域光环的最大一份领域附加，不读 `Attributes.ATTACK_DAMAGE`。然后 `Landing.BYPASS_INVULNERABILITY` 把 `invulnerableTime` 置 0，再 `damageSources().playerAttack(player)`。`finally` 会把无敌时间设回不小于原值；若原无敌时间 `> 10`，还会把 `lastHurt` 抬到不小于本次。这不撤销已经扣掉的血。

同 tick 的第二下不会被第一下的无敌帧吃掉。火伤类型带 `bypasses_cooldown`；物理在调用前把无敌时间清零。没有第三段：根结算被 `suppressRootSettle` 跳过。权限失败、火种为 0、或 `detonate` 返回空，才会少掉火伤。火种为 0 时仍会打物理 6。本仓库把 `fire_resource` 放进 `is_fire`；1.20.1 里带火伤标签且目标有抗火时，`LivingEntity.hurt` 会在扣血前返回。本单没有再打开原版函数体。抗火只影响火伤这一下，物理调用仍在后面。两边传入 `hurt` 的都是减免前数额。护甲、保护具体减多少，本单没有重算。

和「100 火种收割偏少」直接对得上的现成事实是：链上已经算过含武器的伤害，燎里命中没有用它。`SkillUsePacket.executeSkill` 读取 `Attributes.ATTACK_DAMAGE`，`weaponBonus = max(0, 该值 − 1)`。`SkillSegmentMath.segmentDamage` 在基础伤害大于 0、且该段没有附魔索引时，把按属性加成缩放过的基础、`weaponBonus` 和魔法伤害加在一起。这个数经 `SkillSegmentRunner` 写成 `CastFrame.damage` 和 `TechniqueCastContext.getDamage()`。`LiaoliEffect` 不读 `cast.getDamage()`，监听器固定用登记的 `6f`。`suppressRootSettle` 又让结算实体不用帧上的那份伤害。因此攻击属性、剑伤、暴击和锋利不在燎里这两次 `hurt` 里。领域光环只加在物理 6 上，而且只在施法者当时处于会给 `ROOT_TECHNIQUE` 光环的领域内。火伤段只有 `0.36×S×(1+F)`，再进原版火伤结算。没有移动速度项。

这不是把公式判错，也不批准改掉 6 或 `0.36×S×(1+F)`。

## 5. 命中时服务端能取到的运动

`SeedHit` 持有释放时的 `ServerPlayer`。`onHit` 在服务端命中回调里执行，这个玩家对象当时就是施法者。可直接读的原版量：

- `getDeltaMovement()`：当前速度，单位是格/tick，含方向
- `getX()-xo`、`getY()-yo`、`getZ()-zo`：相对上一 tick 位置的位移，同样是格/tick
- `isSprinting()`
- `getSpeed()`：移动速度属性系数，不是当前速度
- `getLookAngle()`、`getYRot()`、`getXRot()`
- `onGround()`、`isFallFlying()`、`isSwimming()`

`shu/technique` 里没有把玩家速度、冲刺或位移换成伤害的函数。搜到的 `getDeltaMovement` 用于剑实体重力、击退和个别位移效果，不用于燎里结算。本仓库是 1.20.1，没有 26.3 长矛的蓄力字段，也没有现成的格/tick 到伤害系数。上面的攻击属性换算（属性值减 1）是链上已有的另一路，单位是伤害点，不是速度。

## 6. 下一步所有权

环几何另单，且只在表实际回到「制作中」之后：

- 主文件 `LiaoliV6Layout`：`ringCenter`、`ringRadius`，以及若三道都要走完扩张则改 `ringProgress` 或 `RING_PERIOD`
- `LiaoliV6Renderer.drawRings` 仅在调用形状必须变时修改
- `TechniqueHitEntity.rearRingVisualAge` 的比较已经是「年龄 ≥ 传入周期」。周期仍由布局常量传入时，不必改实体方法
- 不改 `rear_ring.png`，不改 `>50` 门槛、`pulseRearRing` 的写时刻方式、v7 姿态和其余五张贴图

伤害和移速不在本单、也不在未获批的下一单里接线。主会话还没有批准系数。若以后单独批准，最小接线停在 `LiaoliEffect`：物理仍走现有 `hurtRoot`，火伤仍走现有 `detonate` / `strikeOnce`。速度用上面的 `ServerPlayer` 读数，本仓库没有可复用的单位转换。不要为了吃进 `cast.getDamage()` 而关掉 `suppressRootSettle`，那会把附魔、燃烧秒数、击退和潮标一起放进被跳过的根结算。不要改 `FireLayer.combine` 或全局无敌帧。本定位不给出任何新系数。

待统一编译仍未跑。游戏未实测。额度已用完，本定位不是返修复审，也不把旧的编译或检查退出码当成这次通过。
