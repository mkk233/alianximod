# 容器聚能实施报告（B）

旧重载全部委托 `ReleaseInput.BASE`。新重载只消费调用方传入的冻结输入，不读 `CastFrame`，不重扫装配，不改注册定义。倍率只乘一次：尺寸走 `sizeMultiplier()`，有限正寿命走 `scaleDuration`，到期刻走 `expiryTick` / `expiryGameTime`，存活判断走 `hasExpired`（`withinLife` 是它的否定）。非正寿命不进这两个到期助手。维护周期、费用、伤害、衰减、卸力和去重键都没有乘。

Gradle 和游戏都没有跑。`ReleaseContainerEnergyCheck` 已写好，未执行。

## 新增入口

领域

- `DomainService.tryWave(ServerPlayer, Technique, DomainDefinition, int, EnchantmentSnapshot, ReleaseInput)`
- `DomainService.tryWaveReserved(ServerPlayer, Technique, DomainDefinition, int, EnchantmentSnapshot, Vec3, String, ReleaseInput)`
- `DomainService.frozenLife(DomainDefinition, ReleaseInput)`
- `DomainService.placedGeometry(DomainGeometry, Vec3, Vec3, ReleaseInput)`
- `DomainService.enlarge(DomainGeometry, ReleaseInput)`
- `DomainService.withinLife(int, int)`
- `AxisBoxGeometry.scaledFromGround(AABB, double)`
- `DomainInstance.FrozenClock.freeze(int, long, int)`，字段 `expiresAt`、`visualEnd`
- 包内构造 `DomainInstance(..., int startedAt, int scaledLife)`

护体

- `GuardDefinition.withInput(ReleaseInput)`
- `GuardStates.schedule(ServerPlayer, GuardDefinition, int, long, ReleaseInput)`
- `GuardStates.activate(ServerPlayer, GuardDefinition, long, ReleaseInput)`
- `GuardStates.activateReserved(ServerPlayer, GuardDefinition, long, ReleaseInput)`
- `GuardStates.retune(GuardDefinition, ReleaseInput, int, int, int, int)`
- `GuardStates.expiryOf(int, int)`
- `GuardStates.withinLife(int, int)`
- `GuardStates.RefreshCarry` 字段：`derived`、`expiresAt`、`animationStart`、`nextUpkeepAt`、`revision`

屏障

- `BarrierDefinition.withInput(ReleaseInput)`
- `BarrierService.tryPlace(ServerPlayer, BarrierDefinition, int, EnchantmentSnapshot, ReleaseInput)`
- `BarrierService.tryPlaceReserved(ServerPlayer, BarrierDefinition, int, EnchantmentSnapshot, Vec3, Vec3, String, ReleaseInput)`
- `BarrierService.place(ServerPlayer, BarrierDefinition, ReleaseInput)`
- `BarrierService.placeAt(Vec3, Vec3, float, BarrierDefinition, ReleaseInput)`
- `BarrierBody.withinLife(int, int)`

场地

- `FieldSpawner.releaseFromChain(ServerPlayer, List<Technique>, int, ReleaseDefinition, int, ReleaseInput)`
- `FieldSpawner.releaseAttached(ServerPlayer, ReleaseDefinition, int, boolean, Trajectory, boolean, ReleaseInput)`
- `FieldSpawner.anchorAt(Vec3, double)`
- `FieldZones.add(ServerPlayer, Vec3, double, int, int, int, Trajectory, boolean, boolean, float, float, int, boolean, float, float, float, int, float, ReleaseInput)`
- `FieldZones.scaledRadius(double, ReleaseInput)`
- `FieldZones.scaledLifetime(int, ReleaseInput)`
- `FieldZones.burstReach(double)`
- `FieldZones.plan(double, int, ReleaseInput, int)`
- `FieldZones.withinLife(int, int)`
- `FieldZones.PlannedZone` 字段：`radius`、`lifetimeTicks`、`burstReach`、`expireTick`

## A 要改的调用

传入计划建立时冻结的 `ReleaseInput`，延迟 lambda 捕获这份快照，不要在触发时再读 `CastFrame.current`。不要预先乘尺寸或寿命，注册定义原样传入。

- `SkillUsePacket`：`DomainService.tryWave(...)`、`BarrierService.tryPlace(...)`、`GuardStates.schedule(...)`、`FieldSpawner.releaseFromChain(...)` 各加末尾 `ReleaseInput`。
- `SkillChainPlan.fireDualSequence`：两处 `DomainService.tryWave(...)`、两处 `BarrierService.tryPlace(...)`、`GuardStates.schedule(...)` 同样加末尾参数；延迟回调里用捕获值。
- `CastExecutionSession`：`tryWaveReserved(...)`、`tryPlaceReserved(...)`、`activateReserved(...)` 加末尾参数。
- `SkillSegmentRunner`：`FieldSpawner.releaseAttached(...)` 加末尾参数。

未改这些文件。旧重载仍是 BASE，所以现在不传参时范围和寿命保持原基线。

## 冻结规则

- 领域：先按接触点平移，再绕落地盒的水平中心放大宽、深、高，`minY` 不变。寿命只进实例的 `expiresAt` 和 `visualEnd`。注册定义的费用、维护周期、原生策略和 aura 不动。同步仍发实例 AABB 和 `visualEnd`。
- 护体：从本次传入的注册定义派生半径、带宽和寿命。身高比、费用、维护周期、卸力不乘。刷新替换派生定义、重绑偏转、修订加一，动画起点和维护节拍保持。普通再到二倍再到普通，每次都从注册定义重算，所以能回到 0.9 / 0.12 / 200。接触、偏转和全量同步包都读这份派生定义。
- 屏障：宽、高、厚和寿命乘一次，脚底、水平锚点和 `forward` 不变。放大后的墙再进入原来的 `planShifts`。去重键仍用原宽和原前向。
- 场地：主半径和有限正寿命乘一次。终爆半径在实例构造时用 `burstReach(主半径)` 记一次，不单独再乘能量。身体中点锚点不乘。没有场地注册者；检查用 `WaterReleaseDefinition.zone` 合成，不写入注册表。
- 非正寿命：领域和护体按原加法保留；场地仍把非正寿命回退成 200，并且不再对这个 200 乘能量。
- 正寿命到期用 `expiryTick` 的 int 环绕加法。主 tick、护体 `living`、护体 `LiveRing.live`、刷新写出的到期刻、屏障偏转宿主，都用 `hasExpired`。视觉结束用 `expiryGameTime`。压力领域渲染在 `gameTime >= visualEnd` 时停画。

## 到期补丁

- `WallDeflectionHost.lifeEnded(int, int)` 调用 `ReleaseInput.hasExpired`。`active` 用它，和 `BarrierBody.withinLife` 相反且同源。非正到期刻（0、负数、落到当前刻）仍判结束，不另造无限哨兵。
- `DomainPressureRenderer.draw` 只把 `gameTime > visualEnd` 改成 `gameTime >= visualEnd`。绘制流程未动。
- 同 tick 去重键没有加倍率。代码里预留路径的后缀是接触组 id，不是操作身份；普通释放路径的键也不带操作身份。没有两次不同操作身份同 tick 被吞的调用证据，所以没有材料交给路由组，也没有改键。

## 仍未改

- 护体包、领域包、屏障实体数据都继续发已经冻结的绝对到期刻和尺寸，没有新字段。容器里没有第二套剩余时间减法。
- `BarrierBody` 的衰减和维护周期仍读 `BarrierDefinition.curtain()`。这是原有行为，本次没有改维护计时。

## 验证边界

`src/test/java/com/bslx/XueXiMod/shu/technique/release/ReleaseContainerEnergyCheck.java` 直接调用上述生产方法，覆盖：

- 领域、护体、屏障、场地的 BASE / 2 / 4，以及非法 NaN、空输入退回 BASE。
- 寿命只调用一次 `scaleDuration`；再调用一次会得到不同结果。
- 护体普通 → 2 倍 → 普通：半径、带宽、寿命恢复，动画起点和维护节拍不变。
- 领域地面锚点、接触点平移后再放大、屏障脚底和前向。
- 到期刻与视觉结束 / 派生寿命同源；int 环绕处 `withinLife` 与 `>=` 结论相反。
- 终爆从放大后的主半径派生一次。半径 1 再乘 4 时，`burstReach` 是 1.8，不是先钳成 1 再乘 4。
- 注册定义在派生后原值仍在。

`WallHostExpiryCheck` 调用 `WallDeflectionHost.lifeEnded`，并和 `BarrierBody.withinLife` 对照：有限正寿命的前一拍和到期刻、int 环绕处不按绝对时刻误判、零和负到期刻仍结束。

未执行：这两个 main、编译、屏障里有生物时的让位预检、真实玩家身上的护体刷新、场地写入世界、客户端画面。宿主登记和主人离场也没有跑。
