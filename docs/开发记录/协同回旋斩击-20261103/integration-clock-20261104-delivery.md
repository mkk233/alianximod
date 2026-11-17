# 动作发布时钟（2026-11-04）

只改了回旋和斜斩对外动作的开始刻。内部调度仍用服务器开机刻。没有编译，没有进游戏，没有改公共 API、分身核心、客户端或配置。

## 实际改动

`FamilyExecutionService` 里两处写入 `ActionPlayback.serverStartTick` 的值改为玩家所在 `ServerLevel.getGameTime()`：

- `playSpin` 给玩家和分身各发一次回旋。前摇和收势仍来自时间线，默认仍是 6 刻和 6 刻，没有改成别的时长。
- `playSlash` 发斜斩。出刀刻和收势刻仍是原来的 0 和 8。

`runningTick` 和 `startTick` 仍是 `MinecraftServer.getTickCount()`。释放门、延迟、交错、回旋结束前不播斜斩、接触窗口和冷却都继续用这把内部钟做差值。族 tick 的入参也是开机刻，所以这些时长的刻数含义不变。

## 同一把钟

客户端 `CooperativeActionViews` 用 `Level.getGameTime()` 减 `serverStartTick`。发布端现在写入的也是世界年龄。老存档里世界年龄大于本次开机刻时，动作不会一收到就被算成已经结束。

`CooperativeActionPublisher.finished`、登录追踪 `deliverTracking` 和开始追踪 `track` 本来就用 `ServerLevel.getGameTime()` 减 `serverStartTick`。`CooperativeActionS2CPacket` 只原样读写这个 long。发布端改钟之后，缓存到期和追踪包不用再改。没有要交给 V 的发布器待办。

客户端在钟差超过 200 刻时从收包时刻播放，那是视觉侧的临时兜底，不是这次服务端发布的替代。

## 未改

远距公共 API、`CooperativeContent` 的距离默认、配置指南和世界文件都没有动。

## 自检

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 0。

未统一编译。未游戏验证。本阶段额度已用完，没有追加复审。
