# 火系六式已知限制续做

同一作者，在 remaining-visual 停写后继续。只处理实现报告里已经写明的两处限制。设计 2/2、实现 2/2 不重置。未追加正式实现复审，待统一编译。下面的命令是作者自检，不是复审通过。

## 做了

燎原的可站立顶面改由碰撞箱决定，伤害、火海和火浪用同一个顶面。

- `GroundFireMath.openTop` 在采样点落进的碰撞范围里，取最高的、上方 0.0001 不再落入另一块碰撞箱、并且与原点垂直差不超过 3 的顶面。没有这样的顶面就返回非数字。
- `StandSurface.columnTop` 读取该列 `originY±3` 再向下多一格的 `getCollisionShape`，把箱体换成世界坐标后交给上面的纯函数。服务端 `GroundFireFields.standsInFire` 和客户端 `FireVisualClient.drawGround` 都走它。
- 下半砖顶面是 0.5，整块是 1，被盖住的下层顶面让给上面那层。台阶低阶采样是 0.5，高阶采样是 1。圆盘边界仍含在半径上，层高仍是 1，上界不含。
- 火浪不再在原点铺一张半径那么大的平盘。火浪可见的 12 刻里，圆心落在完整半径内的每一列，在该列碰撞顶面上贴一块一格宽的横片，贴图坐标按整张圆盘切进当前帧。前几帧圆外本来就是透明的，外圈格子因此仍是空的。火海仍只用已经扩散到的半径，余烬和火焰也放在同一个顶面上，火焰高度仍是 0.78。不改方块，不放原版火。

视线按外露碰撞顶面处理：顶面上方留有站立空隙，并且落差在 3 格内，才算可见地表。没有从施法者眼睛或火场原点再打一条方块射线，也没有改用 `BlockOcclusion`。贴地射线会先扎进脚下方块，把下坡误判成挡住。火场仍覆盖圆盘内、包括施法者身后的这些外露顶面。焚天仍然不做方块遮挡检测。

炉心改为先预检、再扣款、炉心没成功就退款。只有两边都成功才返回已提交。

- 预检不成功：不扣款，返回拒绝。
- 付不起，或 `ManaPaymentReceipt.charge` 没有拿到收据：不改炉心，返回魔力不足。
- 扣款之后 `commit` 不成功，或提交过程抛出运行时异常：按这笔收据退回原法杖和玩家池，返回拒绝。
- 预检、扣款、炉心都成功：才显示余额、发一次视觉提示，并返回已提交。
- `SkillUsePacket` 仍只在已提交时写入冷却。失败不进冷却。这条路径没有术式 id 分支。
- 零费冷却不调用扣款。冷却成功才进入 `FurnaceService.cool`，蒸汽只在那里、并且在被动结束已经成功之后放一次。冷却预检或提交失败时，蒸汽还没放，收据为空，返回拒绝。
- 普通升级仍是 `enterOverdrive`。`FurnaceMath` 从普通档升到过载时保留原来的热量，提示用过载循环，不重播升温。

## 没做

- 没有跑 Gradle，没有 `compileJava`，没有打包，没有启动游戏。离线 `javac` 不能代替统一编译。
- 没有改制作表、`记录.md`、`implement-report.md`、归档贴图、Gradle 配置，也没有改这次范围以外的源码。
- `FurnaceService.enter` 仍可能在 `forceAddEffect` 之后返回 `REJECT_BLOCKED`。这次会退回魔力并返回拒绝，不进冷却；现有服务没有一条不调用 `cool`、被动结束或 `removeEffect` 的静默撤销。那条撤销会放蒸汽、丢掉热量或回血，所以没有用它把效果摘掉。效果已经加上、随后却被服务判失败时，效果会留下。这是现有接口还没解决的部分。
- 名称门禁仍报 `OriginalHumanoid.java` 第 165 行 `i`、第 202 行 `to`。这两处不是本次文件，没有改。

## 仍保持的既有约定

炉心复杂度 0，登录自动学会；大招复杂度 20，不自动学会。六式弹速都是 0。能量倍率是 1。炉心走直线释放，但没有命中实体，伤害 0，所以不附魔；大招伤害大于 0，可以附魔。炉心要重新按一次，不按住连发。混链提示不变。伤害仍过 `DamagePermission`。燎原是水平圆盘，不是方块盒。第一轮地面脉冲在开启后 20 刻，漏掉的脉冲不补。地面伤害按炉心结算记过，不再乘一次。种子方案不吃体积缩放。每一批释放一个 `FireCastContext`。落点可以挪动燎原和焚天的原点。燎里跟准星，寿命固定 40 刻，走廊 3×0.6×0.6，物理伤害 6。焚天球半径 8，指数 1.2，不随能量放大，不做视线，100 种子约 123.834728448，分 40 刻。施法者下线后不再结算新的地面伤害，目标身上已经挂上的燃烧继续。燎原半径 6、寿命 200、垂直容差 3、火层 1、地面伤害 2，这些数字没有改。

## 改动文件

| SHA256 | 最后写入 | 文件 |
| --- | --- | --- |
| `05D914C9D247EB321EA9CBA3077E4FA16021F627FDE9D5DD977481EB7041E947` | 2026-10-29 03:48:09 | `src/main/java/com/bslx/XueXiMod/shu/technique/release/firecast/GroundFireMath.java` |
| `5A771137074D35C654014DE7930219C2A8904B8F9246AE258E4DD82398486C60` | 2026-10-29 03:47:43 | `src/main/java/com/bslx/XueXiMod/shu/technique/release/firecast/StandSurface.java` |
| `634F7711B31B7741F946D84DA18ED579CD6A4D5E6F9A913FD65CDD8BD54528B5` | 2026-10-29 03:47:27 | `src/main/java/com/bslx/XueXiMod/shu/technique/release/firecast/GroundFireFields.java` |
| `B887B15FD4FE3C86ACA814A283B943E69A316DF3768CAE05C613154640B1B1FC` | 2026-10-29 03:47:29 | `src/main/java/com/bslx/XueXiMod/client/FireVisualClient.java` |
| `4F77D25629780F2B9A1355232D94A4EF75E91EAB019467BD06DE1B92BBD13B3E` | 2026-10-29 03:43:53 | `src/main/java/com/bslx/XueXiMod/visual/fire/FireVisualLayout.java` |
| `55452DA511795C188443D39A120EFED5FA4C126D01C71247EE85C7CBE450B6ED` | 2026-10-29 03:47:43 | `src/main/java/com/bslx/XueXiMod/shu/technique/release/selfstatus/SelfStatusCommit.java` |
| `CBB455B44CC89C54430D0D2C200B8D8ECA5DA35714B08E260A6A8082DD80DEEA` | 2026-10-29 03:43:34 | `src/main/java/com/bslx/XueXiMod/shu/technique/release/selfstatus/SelfStatusOrder.java` |
| `21DD664C3BD6D130FE7777AAE17BE01B1BCC009290B3ED0958C9176C0B307E0C` | 2026-10-29 03:48:38 | `src/test/java/com/bslx/XueXiMod/shu/technique/release/firecast/KnownLimitsCheck.java` |
| `F1AD5B78B7B20B64A37EE15088A6CD574F4C55EC2056ED0A0BC1B5F2562A7F48` | 2026-10-29 03:45:52 | `src/test/java/com/bslx/XueXiMod/visual/fire/FireVisualLayoutCheck.java` |
| `85AA1CB5CE985389CC8E6FF1DBDF798BD16245B49E42CD26BA9D238A0787BA50` | 2026-10-29 03:50:02 | `docs/开发记录/术式批量制作/火系六式-20261029/known-limits-name-gate.txt` |
| `CB9E80E682EFD8A88EAA2DBB6D9E8C260E22CF1DAFDFDBE433389B6E90159D2F` | 2026-10-29 03:50:04 | `docs/开发记录/术式批量制作/火系六式-20261029/known-limits-self-check.txt` |

`FireVisualLayoutCheck` 原先要求 `solid(feetBlock)`。那是方块格顶面的旧断言，已改成要求 `StandSurface.columnTop`。其余视觉断言没改。

## 自检

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 1。只有 `OriginalHumanoid.java:165 i` 和 `:202 to`。本次改过的文件没有新的命名违规。

离线 `javac --release 17 -encoding UTF-8` 退出码 0。类输出在 `docs/开发记录/术式批量制作/火系六式-20261029/known-limits-check-classes`，不是游戏资源。

`KnownLimitsCheck` 退出码 0，输出 `known limits check passed`。它实际执行了半砖、整块、覆盖、台阶、容差、圆盘、层高和四种提交顺序，并核对扣款在改炉心之前、失败退款、成功才已提交、尝试路径不调用冷却或被动结束。

`FireVisualLayoutCheck` 退出码 0，输出 `fire visual layout check passed`。

没有跑 `gradlew`。

## 停写

本报告写完后，这次授权范围内的写入已经停止，文件占用已经释放。未追加正式实现复审，待统一编译。
