先读完整的编译返修说明，再按其中授权的范围执行。`slim` 是 `PlayerModel` 的私有字段；`xBob` 一组只在 `LocalPlayer` 上。按这两个公开声明改，不动剑的挂接矩阵。5 个编译错误已按 1.20.1 的实际声明改完。剑的挂接矩阵没有动。待统一编译。游戏未验证。

`PlayerModel.slim` 是 private，没有公开读取方法。第三人称改为读该玩家的 `AbstractClientPlayer.getModelName()`；玩家不在当前世界时，用 `DefaultPlayerSkin.getSkinModelName`，这是 `getModelName()` 在没有皮肤信息时的同一条回退。

`xBob`、`xBobO`、`yBob`、`yBobO` 只声明在 `LocalPlayer` 上。本地玩家仍用这四个字段，抵消顺序仍是先偏航、再俯仰。不是本地玩家时残差为 0，因为那两个 0.1 旋转只加在本地玩家的手部绘制上。

命名门禁通过。没有跑 Gradle。

| 文件 | sha256 | mtime |
| --- | --- | --- |
| `SpaceCutBodyPose.java` | `783b378fb81efcb1c0977b32271b807e16999eac7b0e91d949037bf494f5f34e` | 2026-11-02T05:52:27 |
| `SpaceCutItemMount.java` | `82a9d9a11fa804eb1f7a215e20918177d127021968665e3b9c27f5d52676382b` | 2026-11-02T05:52:29 |

下一轮编译进程使用 `JAVA_HOME=C:/Program Files/Java/jdk-17`、`GRADLE_USER_HOME=C:/Users/baishang/.gradle`，PATH 前置该 JDK 的 bin，保持离线与 `-Xmx3G`。不要用 Cursor 临时空缓存。

停止全部源码写入。
