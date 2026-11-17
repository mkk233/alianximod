# 空间切割编译错误返修

只改了共享编译报出的 5 个错误。没有改剑挂接矩阵，没有跑 Gradle。待统一编译。游戏未验证。

## 细臂

`PlayerModel.slim` 在 1.20.1 是 private，类里没有公开读取方法。第三人称改为读正在绘制的玩家：`AbstractClientPlayer.getModelName()` 等于 `"slim"` 才用细臂。玩家对象不在当前世界时，用 `DefaultPlayerSkin.getSkinModelName`，这是 `getModelName()` 在没有皮肤信息时的同一条公开回退，不是写成恒 false。

第一人称原来就读 `getModelName()`，没动。

## 第一人称 bob

`xBob`、`xBobO`、`yBob`、`yBobO` 声明在 `LocalPlayer` 第 147–150 行，不是 `AbstractClientPlayer` 的字段。`ItemInHandRenderer.renderHandsWithItems` 的参数就是 `LocalPlayer`，并在第 312–315 行用这四个字段乘 0.1。

`drawFirstPerson` 先判断 `player instanceof LocalPlayer`。本地玩家用这四个字段做插值，再按原顺序先消偏航、再消俯仰，然后保留完整俯仰和身体偏航。不是本地玩家时残差为 0，因为那两个 0.1 旋转只加在本地玩家的手部绘制上，不会改用别的角假装抵消。

## 文件

| 文件 | sha256 | mtime |
| --- | --- | --- |
| `SpaceCutBodyPose.java` | `783b378fb81efcb1c0977b32271b807e16999eac7b0e91d949037bf494f5f34e` | 2026-11-02T05:52:27 |
| `SpaceCutItemMount.java` | `82a9d9a11fa804eb1f7a215e20918177d127021968665e3b9c27f5d52676382b` | 2026-11-02T05:52:29 |

命名门禁通过。运行 JSON 未改，正式资源 `runtime-mapping` 没有版本字段要改。

下一轮编译进程使用 `JAVA_HOME=C:/Program Files/Java/jdk-17`、`GRADLE_USER_HOME=C:/Users/baishang/.gradle`，PATH 前置该 JDK 的 bin，离线，`-Xmx3G`。不要用 Cursor 临时空缓存。这一阶段没有执行 Gradle。

停止全部源码写入。
