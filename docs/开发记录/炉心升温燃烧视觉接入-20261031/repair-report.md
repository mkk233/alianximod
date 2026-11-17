# 炉心第一人称手臂返修

状态：事实阻塞，源码未改。未编译，未进游戏，未改制作表。

检查结论要求 `FurnaceArmFlame.onArm` 只订阅 `RenderArmEvent.Post`，并删掉手臂 `xRot` / `yRot` / `zRot` 的保存、清零和恢复。对照本机 Forge **1.20.1-47.3.33**（Parchment `2023.06.26-1.20.1`）后，这个类型和这次投递都不存在，所以没有按该结论改 `FurnaceArmFlame.java`。

## 定向核对

源码包：

`C:/Users/baishang/.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.20.1-47.3.33_mapped_parchment_2023.06.26-1.20.1/forge-1.20.1-47.3.33_mapped_parchment_2023.06.26-1.20.1-sources.jar`

- `net/minecraftforge/client/event/RenderArmEvent.java` 只有一个可取消事件类。文件到类结束没有 `Pre`、`Post` 内部类。
- 同目录映射 jar 与 recomp jar 里只有 `net/minecraftforge/client/event/RenderArmEvent.class`。
- `ForgeHooksClient.renderSpecificFirstPersonArm`（约第 305–308 行）只投递一次 `new RenderArmEvent(...)`。
- `PlayerRenderer.renderRightHand` / `renderLeftHand`（第 177–185 行）先调这个钩子。返回 true（事件被取消）时直接跳过手臂。返回 false 时进入 `renderHand`（第 187–198 行）。`renderHand` 里先 `setupAnim`，再把 `xRot` 写成 0，然后 `arm.render`。这里不再投递手臂事件。

因此当前监听 `RenderArmEvent` 时，每只第一人称手进一次 `onArm`，发生在 `renderHand` 之前，不会画两遍。改成 `RenderArmEvent.Post` 对这套 API 无法解析。

同一时刻手臂部件上的转角也还不是随后 `arm.render` 用的那组：`setupAnim` 和 `xRot = 0` 都在事件之后。检查结论里「删掉清零，让 `translateAndRotate` 跟上 `Post` 时的手臂转角」依赖这次不存在的后事件。只删清零会把事件当时留在部件上的转角乘进火焰，那不是手臂网格即将使用的转角。这一步没有单独做。

第一人称大手臂的持物、挥击位移在调用 `renderRightHand` / `renderLeftHand` 之前已经写在 `PoseStack` 上，例如 `ItemInHandRenderer.renderPlayerArm`（第 220 行起）和 `renderMapHand`（第 136 行起）。现有 `onArm` 仍使用 `event.getPoseStack()`，这段没有改。

## 改动范围

未改任何源码。`FurnaceArmFlame.java` 仍是实现时的字节：

- 路径：`D:/alianximod/src/main/java/com/bslx/XueXiMod/client/furnace/FurnaceArmFlame.java`
- SHA256：`79b845a283f84edfae7cd9fa8f4de9efa1e543d609a8d8ffcb61fbc167130e70`
- mtime：`2026-10-31 03:48:05`
- 字节：`4916`
- `onArm` 仍在第 31–64 行：监听 `RenderArmEvent`，保存并清零三轴转角，`pushPose`、`translateAndRotate`、`drawArm`、`popPose` 后恢复转角。左右手仍看 `event.getArm()`，苗条臂仍走 `FurnaceBodyVisual.narrowArm`。

其余升温、燃烧层、纹理、冷却蒸汽和制作表都未动。变量名门禁会调用 `javac`，本轮授权禁止 `javac`，而且没有改 Java，所以没跑。

## 缺口

`RenderArmEvent.Post` 在本 Forge 中不存在，规定的返修没有落地。双画结论与这次投递不符。清零转角是否让火焰偏离手臂网格，要等有后事件或另一次授权后再改；本轮不扩大范围。

停止写入。
