# 实体寿命与条件水牢

实例寿命只在生成时读一次 `CastFrame.releaseInput`，写入实体同步数据，不写回模板。垂丝填充和排空仍是 8 tick，排空起点是这一份的总寿命减 8。水牢先按原潮湿档算基准寿命，再乘同一份预约输入；壳从目标包围盒中心按同一倍率放大原外壳和原内盒。聚能成功且原剩余盖不住到期点时，只延长支撑潮湿的时长，不增加湿度量。

未改 `ReleaseInput`、`CastFrame`、网络路由、领域、护体、屏障、场地和美术。未跑 Gradle，未进游戏。

## 新增签名

```text
TechniqueHitSpec.instanceLifetime(int templateLifetime, ReleaseInput releaseInput, boolean arcBand) -> int
TechniqueHitEntity.applySpec(ServerPlayer caster, TechniqueHitSpec spec, int instanceLifetime)
TechniqueHitEntity.lifetime() -> int
```

`lifetime()` 读同步字段 `XuexiHitLifetime`。`applySpec`、NBT 读回和客户端都走这一个值。弧带安装后仍用快照自己的 `lifeTicks()` 覆盖，不缩放 6+4 窗口，也不改静帧。

```text
WeepingSettings.drainSpan() -> int
WeepingSettings.drainStartFor(int lifeTicks) -> int
WeepingCurtain.flowSegment(..., int age, int lifeTicks) -> WaterSegment
WeepingCurtain.segmentFor(..., int age, int lifeTicks) -> WaterSegment
WaterCurtainLayout.drainStart(int lifeTicks) -> int
WaterCurtainLayout.panels(double curtainWidth, double curtainHeight, int frameIndex, int drainStart) -> List<Panel>
```

旧的六参 `flowSegment` / `segmentFor` 仍用基准寿命 40。贴图 `flowFrame` 仍是每 2 tick 一帧。

```text
ConditionalEffect.attempt(ServerPlayer caster, UUID operationId, EnchantmentSnapshot contact, ReleaseInput input) -> ConditionalResult
ConditionalEffect.attempt(ServerPlayer, UUID, EnchantmentSnapshot) 默认转 BASE
WaterPrisonEffect.attempt(..., ReleaseInput input)
PrisonService.prepare(ServerPlayer, LivingEntity, UUID, ReleaseInput) -> PrisonReservation
PrisonService.commit(PrisonReservation, EnchantmentSnapshot, ReleaseInput) -> PrisonCommitResult
PrisonReservation.commit(EnchantmentSnapshot, ReleaseInput) -> PrisonCommitResult
PrisonReservation.releaseInput() -> ReleaseInput
PrisonRuntime.prepare(..., ReleaseInput) -> PrisonReservation
PrisonRuntime.commit(PrisonReservation, EnchantmentSnapshot, ReleaseInput) -> PrisonCommitResult
PrisonRuntime.bindInput(PrisonReservation existing, ReleaseInput offeredInput) -> ReleaseInput
PrisonRuntime.scaledDuration(int tier, int wetRemaining, ReleaseInput frozenInput) -> int
PrisonRuntime.supportingDuration(int prisonTicks) -> int
PrisonRuntime.replacementDuration(int wetRemaining, int prisonTicks, float sizeMultiplier) -> int
PrisonShell.expand(AABB body, double sizeMultiplier) -> AABB
PrisonShell.innerFor(AABB body, double sizeMultiplier) -> AABB
```

旧的三参 `prepare` 和两参 `commit` 仍在。`prepare` 转到 `BASE`。预约上的 `commit(contact)` 使用已经冻结的首次输入。

## A 调用方

本组只读 A 的契约，没有改 `ReleaseInput` 或 `CastFrame`。

- `TechniqueHitSpec.spawnReported` 读 `CastFrame.current().releaseInput`。字段缺省是 `BASE`。路由把链倍率写入该字段之后，生成才会放大实例寿命。
- `SkillSegmentRunner` 第 280 行仍调用三参 `attempt(player, operationId, contact)`。接口默认把它转成 `ReleaseInput.BASE`。路由要改成四参，并把同一次释放的 `ReleaseInput` 传进来。在那之前，水牢准备和提交都是基准倍率。
- 命中效果本身不用改。垂丝等效果仍把定义寿命放进模板，`spawn` 读帧上的输入，只缩放这一次实例。

`bindInput` 在已有预约时返回首次输入。同一 `operationId` 再次 `prepare` 直接交回那份预约，不换倍率。操作号、预扣和退款没动。

## 到期

正寿命的实体年龄用 `ReleaseInput.expiryTick(0, lifeTicks)` 和 `hasExpired`。小于等于 0 的寿命仍按原来的 `age >= lifeTicks`，不送进 helper。年龄已经是 `Integer.MAX_VALUE` 时先到期并返回，不再 `age++`。

水牢 `endTick` 用 `ReleaseInput.expiryGameTime(birthTick, durationTicks)`。这些长时刻比较保持 `gameTime >= endTick`，没有改成 int 差值：

- `PrisonRuntime.rebind`
- `PrisonRecord.tick` 的离区标记和在场结算
- `PrisonDeflectionHost.active`（未改文件，原本已是 `>=`）
- `PrisonClientState.purge` 与 `current`（未改文件，原本已是 `>=` / `<`）

`nextPulse` 仍是 20 tick 维护计时，没有乘聚能。统一的 `expiresAt` 安全上限仍等主控，这里没有另加一层。

## 行为边界

- 模板 `lifetime`、`hitInterval`、`hitReadyAge`、`speed` 不因这次缩放被写回。复制份共用同一次结果，不会逐份再乘。
- 体积仍走原来的 `volumeScale` 与定义轴向开关。资格由 A 负责。
- 垂丝 40 / 80 / 160 的排空起点是 32 / 72 / 152。第 8 tick 溅水。服务端水段和客户端 `drainStart` 都读 `WeepingSettings.drainStartFor`。客户端画出的高度仍是服务端已经按该寿命裁好的水段。
- 水牢一档 20、二档 100、三档剩余一半、四档剩余全部，先算出基准再 `scaleDuration`。非正基准原样返回并仍因 `<= 0` 拒绝。潮湿数值、档位、20 格选目标和单人囚犯不变。
- 聚能水牢只延长支撑潮湿的时长，不增加湿度量。`sizeMultiplier() > 1` 且原剩余盖不住牢到期时，成功提交后、创建通知和四档首脉冲之前，用 `WetnessState.replaceExact` 把时长写成牢寿命加 1 拍。加 1 是为了同 tick 药水先递减时仍盖住 `endTick`；已经是 `Integer.MAX_VALUE` 则不再加。`BASE` 不延长，一二档剩余不足时仍会提前拆牢。失败、未提交关闭、同一操作再次提交不再写药水。不按延长后的时长重算牢寿命。`onWetRemoved` 的直接编辑保护保持原样，牛奶等主动清除仍拆牢。
- 壳厚随倍率变。内盒不是把放大后的外壳再 `inflate(-0.20)`。
- 生成水的一格永久方块未改。

## 验证

检查类：

- `src/test/java/com/bslx/XueXiMod/shu/technique/release/entity/ReleaseEntityEnergyCheck.java`
- `src/test/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonEnergyCheck.java`

覆盖生产 `BASE` / `2` / `4`、模板不重复乘、弧带不缩放、垂丝 40 / 80 / 160 相位、壳的 1 / 2 / 4 倍与固定 0.20 反推否定、预约输入幂等、非正寿命保留。水牢支持期另覆盖四档 `1000×2`、三档 `1000/2×4`、一二档原剩余不足、`BASE` 不延长、非正牢寿命不延长、重试不重复加拍、到期点不被额外 1 拍推后、`Integer.MAX_VALUE` 饱和。

静态核对、没有执行：`commit` 里对真实药水调用 `replaceExact` 的顺序，`onWetRemoved` 在直接编辑时返回、牛奶等外部清除仍走 `WET_CLEARED`，预约第二次 `commit` 因已有结果不再进入运行时。这些要实体和效果回调，不能当成集成通过。

此阶段没有跑 Gradle，所以上面两组 `main` 没有执行。命名门禁在本组改动的文件上没有新违规。未改的 `OriginalHumanoid.java` 仍有两处旧名字：第 165 行 `i`，第 202 行 `to`。
