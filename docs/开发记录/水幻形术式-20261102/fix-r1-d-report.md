# 水幻形 D 第 1 轮返修报告

日期：2026-11-02。只修复核指出的两处。命名门禁已通过。未跑 Gradle，未进游戏。本文件写完后停止写入，不自行复审。

## 改动

### 1. 断潮画面中心

`src/main/java/com/bslx/XueXiMod/client/ActorVolumeVisual.java` 83–84 行。

`VOLUME` 的 `worldPosition` 已是服务端体积中心。`draw` 用 `snapshot.anchor()` 直接作中心，不再加 `forward * depth / 2`。盒子仍按宽、高、纵深的半尺寸和原正交基摆放，整张 UV 未改。维持和散水的脚底锚点没有动，`CloneAfterimagePlayback` 未改。

### 2. 同维主人

`src/main/java/com/bslx/XueXiMod/network/ClonePresentationS2CPacket.java` 210–241 行。

160 格内的同维玩家仍先发送。随后调用 `CloneService.findPresentationOwner(server, deploymentId)`（A 已有该只读方法，`CloneService.java` 169–186 行）。返回的主人再经 `offerViewer`：已移除、已断线、世界为空或维度与通知不同则不发。附近循环和主人补发共用同一去重键（玩家、部署、`actionToken`、阶段）。主人已在 160 格内时第二次直接跳过。发送账的保留期、4096 上限，以及 tick、登出、维度卸载、停服清理都还在。包字段和 `ClonePresentationNotice` 构造未改。没有改成全服玩家循环。

## 验证

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 0。

未执行 `gradlew compileJava`。未启动游戏。
