通过。`ReleaseInput` 与 `CastFrame.releaseInput` 符合已批准的公共契约，没有确定正确性缺陷。

- 记录组件不改写；`BASE` 为 `1f`。`sizeMultiplier()` 对非有限、±0 和负数返回 `1f`，有限正数原样返回。
- `scaleDuration`：`baseTicks <= 0`（含 `Integer.MIN_VALUE`）原样返回；正寿命先转 `double` 再乘再 `ceil`，`NaN → 1`，`≥ Integer.MAX_VALUE`（含 `+Inf`）→ `Integer.MAX_VALUE`，`≤ 1 → 1`，其余 `(int)` 截断。正寿命结果闭区间为 `1..Integer.MAX_VALUE`。
- `expiryTick` 是 Java `int` 环绕加法。`hasExpired` 用 `nowTick - expiryTick >= 0`，相等算到期。
- `expiryGameTime` 在 `durationTicks >= 0` 且 `now > Long.MAX_VALUE - durationTicks` 时返回 `Long.MAX_VALUE`，否则相加；`>` 使刚好落到上界时仍得到精确和。负寿命被排除在减法之外，避免 `Long.MAX_VALUE - duration` 自身溢出。

原始组件未归一、`sizeMultiplier()` 才归一，是契约要求，不构成可行动缺陷。`scaleDuration` 已走 `sizeMultiplier()`。

前置条件：倍率只读 `sizeMultiplier()`；`<= 0` 的 `scaleDuration` 结果和无限哨兵不得送进三个 helper；`hasExpired` 只用于逐 tick、跨度不超过 `2^31` 的有限正寿命；长游戏时间比较保持 `now >= expiry`。