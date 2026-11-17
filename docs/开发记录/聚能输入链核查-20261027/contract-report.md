# 公共契约 API（A 第一小项）

未做路由、资格、MAGIC/NONE。未改容器。未跑 Gradle。未 commit。

命名门禁：`ReleaseInput` 保持 record，本次两个文件没有新违规。脚本退出码 1，仅旧违规，未改：

- `src/main/java/com/bslx/XueXiMod/animation/runtime/OriginalHumanoid.java:165` 局部变量 `i`
- `src/main/java/com/bslx/XueXiMod/animation/runtime/OriginalHumanoid.java:202` 局部变量 `to`

## 文件

- 新增 `src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseInput.java`
- `CastFrame` 只增加字段 `public ReleaseInput releaseInput = ReleaseInput.BASE;`
- 构造不改写倍率。组件里的 NaN、±Inf、≤0 原样保存。

## 类型

```java
public record ReleaseInput(float chainEnergyMultiplier) {
    public static final ReleaseInput BASE = new ReleaseInput(1f);

    public float sizeMultiplier();
    public int scaleDuration(int baseTicks);

    public static int expiryTick(int nowTick, int durationTicks);
    public static boolean hasExpired(int nowTick, int expiryTick);
    public static long expiryGameTime(long now, long durationTicks);
}
```

没有 `energyScale`、附魔半径、操作 ID。`sizeMultiplier()` 只返回有效链聚能，不乘 `CastFrame.energyScale` 或 `radiusMultiplier`。

## sizeMultiplier

`!Float.isFinite(chainEnergyMultiplier) || chainEnergyMultiplier <= 0f` 时返回 `1f`。否则返回保存值。

覆盖 NaN、+Inf、-Inf、+0、-0 和负数。`(0, +Inf)` 内的有限正数原样返回，包含小于 1 和大于 1。

## scaleDuration

`baseTicks <= 0` 时原样返回，含 0、负数和 `Integer.MIN_VALUE`。这条结果不能送进下面三个 helper。

`baseTicks >= 1` 时：

```text
scaled = Math.ceil((double) baseTicks * (double) sizeMultiplier())
```

先转 double 再乘，再 `ceil`。然后：

| scaled | 返回 |
|---|---|
| NaN | 1 |
| ≥ `Integer.MAX_VALUE`（含 +Inf） | `Integer.MAX_VALUE` |
| ≤ 1 | 1 |
| 其余 | `(int) scaled` |

因此有限正寿命的结果闭区间是 `1 .. Integer.MAX_VALUE`。`Integer.MAX_VALUE` 是有限正寿命的上界，不是无限哨兵；乘完超出 int 时停在这个上界。

## expiryTick

```java
return nowTick + durationTicks;
```

Java int 环绕加法，不钳制，不识别哨兵。调用方只能传入已经由 `scaleDuration` 得到的有限正寿命 `1 .. Integer.MAX_VALUE`。`Integer.MAX_VALUE + 1` 环绕成 `Integer.MIN_VALUE`。

## hasExpired

```java
return nowTick - expiryTick >= 0;
```

相等视为已到期。禁止写成 `nowTick >= expiryTick`。只给逐 tick 检查的有限正寿命用；超过 2^31 tick 未检查的历史记录不能用这个差值。

## expiryGameTime

```java
if (durationTicks >= 0L && now > Long.MAX_VALUE - durationTicks) {
    return Long.MAX_VALUE;
}
return now + durationTicks;
```

`durationTicks >= 0` 时与「`now > Long.MAX_VALUE - duration` 则返回 `Long.MAX_VALUE`，否则相加」相同；这个前提避免负寿命把 `Long.MAX_VALUE - durationTicks` 自己算溢出。负寿命不在调用契约内。

长游戏时间的到期比较保持 `now >= expiry`。不要改用 int 那条差值。

三个 helper 都不接受无限哨兵。调用前寿命必须已是有限正值。
