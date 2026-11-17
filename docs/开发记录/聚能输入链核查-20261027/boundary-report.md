# 释放边界

`SkillChainPlan.chainEnergyBefore` 不再用正伤害当一次释放的结尾。只改了这一个公共扫描。份数、附魔、费用和其他扫描器没动，`sharesDualRelease` 没改。

从当前成员往前看。先取更早成员的释放定义，再判断它是不是边界：

- 不是附魔
- 定义不为空
- `kind()` 是 `ReleaseKind.HIT_VOLUME`
- `role()` 是 `ChainRole.ATTACK`

是边界时，`sharesDualRelease(techniques, earlier, actionIndex)` 为真就 `continue`，同一组双重释放仍共用这次聚能；否则 `break`，更早的聚能不再传下去。不是边界就按原来的 `chainEnergyScale` 连乘。定义没注册则跳过，不截断。

聚能自己是 `ReleaseKind.RULE` 加 `ChainRole.ATTACK`，只看职责会把它误当成边界，所以必须同时看种类。动作宿主是 `ReleaseKind.ACTION` 加 `ChainRole.ACTION`，不是边界，两条聚能可以穿过它。

资格和边界分开。无属性法术、剑术不能消费聚能，但它们的定义仍是命中体积攻击，所以是边界。聚能不能跨过它们去加到后面的元素法术上。

## 顺序

往前扫，先遇到的边界生效。

- 聚能、水环、雾雨：水环 2，雾雨 1。水环挡住了更早的那次聚能。
- 聚能、正伤元素法术、雾雨：元素法术 2，雾雨 1。
- 聚能、无属性法术、元素法术：两边都是 1。无属性不消费，也挡住后面。
- 聚能、剑术、元素法术：两边都是 1。剑术同样是边界。
- 聚能、水环、聚能、雾雨：水环 2，雾雨 2。雾雨只吃到水环之后的那次聚能。
- 两条聚能、动作宿主、元素法术：元素法术 4。动作没有截断。
- 既定双重：组内两个释放成员都是 2，`sharesDualRelease` 对这一对为真，所以前一个成员不把聚能截住。组外下一名是 1，并且和组内不共享双重。编译出的双重计划仍是两份 2。

水环和雾雨用的是 `WaterReleaseDefinition.bodyGuard` / `domain`，伤害是 0，种类和职责来自这个类，不是把伤害改成正数。

## 检查

`compileJava --offline --no-daemon` 退出码 0，`compileJava-boundary.log`。`RouteReleaseInputCheck` 68 条，失败 0，退出码 0，`check-route-boundary.log`。生产类来自这次编译。实体、水牢和契约检查没有重跑。容器和墙宿主仍因缺少 ModLauncher 跑不完，没有再试 Bootstrap，没有进游戏。
