我先完整读取这份任务说明，再按里面的范围和要求执行。任务是独立只读审查：先读契约和实体报告，再核对提供的 diff 与源码。契约和报告已读完。接下来按 diff 核对寿命、垂丝和水牢的生产路径。**不通过。** 有一处高严重缺陷：水牢把寿命乘长之后，潮湿药水仍按原剩余时间自然消失，牢会被旧的移除回调提前拆掉。其余核对过的寿命、垂丝、弧带、静帧、预约和壳缩放路径没有另见可复现缺陷。

## 高：放大后的水牢被潮湿到期提前结束

四档以及三档四倍时，算出来的牢寿命必然长于当前潮湿药水。药水照旧每拍减少，到点被移除后，牢走 `WET_CLEARED`，到不了 `endTick`。二档、一档在刚刷满 1000 拍时还能撑完（400、80 都短于 1000）；剩余时间已经短于放大后的寿命时，同样会被提前拆掉。

触发路径：

1. `WetnessState.grant` 把潮湿药水设成 1000 拍（`WetnessState.java` 第 11、47 行）。档位看湿度数值，四档是 76 及以上。
2. `PrisonRuntime.commit` 先按档算基准，再 `scaleDuration`（第 85–86、346–352、373–389 行）。刚施加的四档、倍率 2 得到 2000 拍，倍率 4 得到 4000 拍。三档四倍是剩余时间的一半再乘 4，约等于剩余时间的两倍。`endTick` 用 `expiryGameTime` 记下这个更长的点（第 98–99 行）。这里没有把药水持续时间拉到至少等于牢寿命。
3. 之后生物每拍照常扣药水时间。扣到 0 时原版移除效果，`PrisonLivingMixin` 第 70–72 行进入 `PrisonRuntime.onWetRemoved`（第 263–270 行）。这不是 `AttributeStateService` 的直接修改，所以不会被跳过，牢以 `WET_CLEARED` 结束。
4. 服务端牢的时长检查在服务器 tick 末尾（`PrisonLifecycle` 第 21–25 行，`PrisonRecord.tick` 第 487–488 行）。药水移除发生在生物 tick，更早。牢已经结束，不会再按 `endTick` 走到 `DURATION`。结束事件会通知客户端移除壳，所以两边一起提前结束，不是客户端多画一段。

举例：刚刷上的四档、链倍率 2，预约寿命 2000 拍，潮湿在第 1000 拍消失，牢同时拆掉。壳已经按 2 倍放大，寿命没有放大。

建议：`durationTicks > 0` 并且当前药水短于这个寿命时，用现有湿度数值把药水持续时间改到 `durationTicks`，不要改短更长的计时，也不要改湿度数值。走 `WetnessState.replaceExact`，这样中途摘药水算直接修改，不会误触发 `onWetRemoved`。牛奶等外部清除仍会移除药水并拆牢。预扣和退款不用动。

`PrisonEnergyCheck.durations` 只断言了 `scaledDuration` 的数字。它的 `main` 写明没有在潮湿生物上执行 `prepare` 会话和 `commit`，所以这条提前结束不会被测到。

## 其余核对

这些路径没有另见缺陷：

- 实例寿命在 `TechniqueHitSpec.spawnReported` 循环外只算一次（第 247、285 行），不写回 `spec.lifetime`。同一次生成的复制份共用这个值。
- 垂丝模板 40，经 `scaleDuration` 后为 40 / 80 / 160。填充仍是 8，排空起点是总寿命减 8（32 / 72 / 152）。服务端 `WeepingCurtain.fit` 用实体上的寿命裁水段，高度经实体数据同步；客户端贴图仍是 `flowFrame` 每 2 拍一帧。
- 弧带不乘寿命，安装后用快照的 6+4。静帧帧数和每帧拍数没有乘进寿命，播放长度仍是帧数乘每帧拍数。
- 正寿命用 `expiryTick(0, life)` 和 `hasExpired`；小于等于 0 不进 helper。年龄已经是 `Integer.MAX_VALUE` 时先到期，不再 `age++`。水牢 `endTick` 用 `expiryGameTime`，比较仍是 `gameTime >= endTick`。
- 同一操作号再次 `prepare` 交回原预约；`bindInput` 保留首次输入。壳从目标中心按同一倍率放大原外壳和原内盒，不是把放大后的外壳再缩 0.20。

`ReleaseEntityEnergyCheck` 直接调用了寿命、垂丝相位和贴图帧这些生产方法，但没有跑实体 tick、NBT 或客户端渲染。两组 `main` 这次都没有执行。上面实体路径是对照当前源码看的，不能把“测试没跑”写成这些路径已经通过。