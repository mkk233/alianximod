我先通读这份复核任务说明，确认本轮授权范围后再按它执行。第 1 轮的三个问题要逐项对照返修报告和当前代码。我先读复核记录和 A/C/D 的返修说明。三份返修报告已读完。接下来只核对这三处的当前代码：画面中心、主人补发，以及部署节流的生命周期。客户端绘制处还有一道 160 格裁剪，我要确认它会不会把已经补发的主人包再丢掉。**结论：FAIL。** 这是第 1 轮三个问题的返修复核（实现第 2/3 轮）。画面中心和部署节流已经修好。同维主人离开 160 格后，服务端会把包补出去，客户端绘制仍把这份快照丢掉，所以原来的「看不到」还在。未编译，未进游戏。Steve 水线 v2、正式 `PLAYER` 注册和专项声明仍未接线，不记为缺陷，完整术式也还不能算交付。

## 前轮问题

### 1. 已修复 — 断潮画面中心

`ActorVolumeVisual.draw` 用 `snapshot.anchor()` 做中心，不再加 `depth/2`（`ActorVolumeVisual.java` 83–91 行）。盒子仍以宽、高、纵深的一半沿原正交基摆放。服务端 `volumeCenter` 仍是眼睛沿冻结视线前移半个纵深（`CapturedResponseRuntime.java` 351–353 行），通知里的 `VOLUME` 坐标就是这个中心。

维持和散水仍把脚底写进 `worldPosition`（`CloneAfterimagePlayback.java` 290–295 行）。`CloneOutlineVisual.drawMaintained` / `drawScattered` 没有改锚点，也没有套用斩击的半纵深。

### 2. 未完全修复 — 远处主人仍看不到散水和斩击

转发本身按清单做完了。`CloneService.findPresentationOwner`（`CloneService.java` 170–187 行）只认当前服务器线程上、这一局会话里尚未释放的部署，读租约上的主人，不读身体。断线或已移除则返回空。`presentationOwnerId`（`CapturedResponseRuntime.java` 102–113 行）核对会话和部署编号。

散水和斩击发出时，部署还在表里：驻留散水在转票之后、`endActor` 之前（`CapturedCasts.java` 209–211 行）；体积通知在 `submit` 成功之后、释放之前（`CapturedResponseRuntime.java` 266–268 行）；失败散水在 `release` 之前（286–288 行）。死亡、登出、换维会把部署从这张表摘掉，不会把上一局的主人交出去。

`relay` 先发给通知维度里 160 格内的玩家，再给查到的主人补一次（`ClonePresentationS2CPacket.java` 210–217 行）。`offerViewer`（220–230 行）拒绝空玩家、已移除、断线和其他维度，并用同一把键（玩家、部署、`actionToken`、阶段）去重。附近主人不会收第二次。循环范围仍是该维度的玩家列表，不是全服。包字段和 `ClonePresentationNotice` 构造没改。

客户端把刚收下的包又按相机距离丢掉：

```441:442:src/main/java/com/bslx/XueXiMod/client/CloneAfterimagePlayback.java
            if (squared > reachSquared) {
                continue;
            }
```

`reachSquared` 来自 425–426 行的 160 格。快照时长是通知里的 20 tick（`ClonePresentation` 的 `NOTICE_WINDOW_TICKS`）。主人离开身体超过 160 格时，包能到，绘制直接跳过；20 tick 内也走不回这个范围。散水和斩击都在这道判断后面。这是原播放器里的裁剪，不是这次转发新加的，但它让前轮那个可见结果仍然存在。作者：D。

最小修复：已经收下的快照不要再按相机 160 格丢掉。服务端已经决定了接收者；附近玩家本来就在事件 160 格内。

### 3. 已修复 — 部署节流不再跨世界残留

静态 `ACCEPTED_TICK` 已删除。`CaptureAcceptanceLedger` 只保留当前 `MinecraftServer` 实例和当前 `getTickCount()` 下成功部署的主人 UUID，不留玩家对象。

同窗口里第二次成功在 `tryExecute` 开头拒绝（`ActorCaptureDispatcher.java` 49–51 行）。入账只在 `spawnCaptured` 已接受之后（168 行）。失败不写。普通链不进 `tryExecute`。服务器对象不同就不会命中，哪怕 tick 数字相同。`END` 且仍是这一窗口时清空（`CaptureAcceptanceLedger.java` 60–64 行），所以记录活不过当前这一拍的玩法处理；这一拍里的第二发仍在 `END` 之前被拒绝。停服清空（69–72 行）。登出按 UUID 移除，集合空了就放开服务器引用（75–83 行）。订阅在这个类上，没有引用客户端类。

## 新引入的问题

没有。节流账、主人查询和斩击中心没有带出新的可证明错误。

## 未跑与仍未完成

未运行 `compileJava`，未打包，未进游戏。正式注册、释放注册和 `declareSpecials()` 仍没有调用方。Steve 水线 v2 仍待确认。
