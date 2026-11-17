# 引燃原版火焰显示接入

- batchId：`待制作-20261031`
- taskId：`fire_kindle_v2_progress`
- attemptId：`20261116-impl-kindle-vanilla-01`
- 关联：`fire_kindle`
- 直接父级：主会话 `01a1ce06-b681-7c90-b11d-260d3c169911`
- 模型：`cursor/grok-4.7@500k`，high。工具无 tier，沿用客户端 Fast `service_tier="priority"`，未独立验证后端。
- 本阶段额度已用完，返修结果未追加复审。

用户原话：「引燃的效果就采用原本燃烧动画就好伤害依然用引燃本身计算特效不再单独制作」。

## 改动

- `D:/alianximod/src/main/java/com/bslx/XueXiMod/client/FireVisualClient.java`
  - 改前：2026-11-16 12:59:37.977，56508 字节，`59936F3FD13A592A9E17AD081AD7EDB4B0C448232D5B6DDD771E417025CAF402`
  - 改后：2026-11-16 14:28:55.528，53375 字节，`C4A09F2E7B62653D86FC0B62B02C89A4556EFAD1C9DCE7338B42340E0D67FE79`
  - `FireVisualClient.java:91`：`showsKindleFlame` 查询 `burnToken(entityId, KINDLE=3)`。
  - `FireVisualClient.java:153`：引燃轨道仍写入 `BURNS`，但不再建余焜。
  - `FireVisualClient.java:160`：任何 `BURN_END` 立即移除引燃轨道。
  - `FireVisualClient.java:506`：持续绘制只留燎原和焚天，引燃不再画专属面片。
  - `FireVisualClient.java:877`：登出或维度卸载仍清空全部轨道。
- `D:/alianximod/src/main/java/com/bslx/XueXiMod/mixin/client/EntityRenderDispatcherSoulFireMixin.java`
  - 改前：2026-11-16 12:36:07.635，2587 字节，`4B357BAC0323A9AEE1768F5D319520A5F979CE217AE33C71FFBC9135F76C05A5`
  - 改后：2026-11-16 14:28:55.537，3191 字节，`18DE43196BC5E3BDF9BA622B24C1B4430D96CF079F14015087745359AC5121C8`
  - `EntityRenderDispatcherSoulFireMixin.java:39`：重定向原版 `Entity.displayFireAnimation()`。
  - `EntityRenderDispatcherSoulFireMixin.java:45`：原判定为真，或引燃轨道有效，才进入同一次 `renderFlame`。
  - `EntityRenderDispatcherSoulFireMixin.java:48`：幽冥魂火换图仍作用于这一次火焰。
- `D:/alianximod/src/main/resources/xueximod.mixins.json:72` 原已登记该 Mixin，未改。

## 显示与收束

引燃有效条件是实体存活、未移除、非旁观、客户端世界存在，且轨道最近同步不超过既有 40 tick。原版真正着火仍先走 `displayFireAnimation()`，不会再画第二层。引燃本身不设置 `remainingFireTicks`、共享火焰标记或服务端实体火焰状态。

自然结束、死亡中止、目标移除都由现有 `BURN_END` 删除轨道；登出和维度卸载清空轨道。旧的 12 帧引燃余焜面片已停用，结束后不留自制收束。焚天双层及其 9 tick 自然收束未改。第一人称着火遮罩未改。

## 伤害未改

以下文件的修改时间与 SHA256 仍为开工前的值：

- `D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/FireDamage.java`：2026-11-01 16:06:50.127，`B396B79AB1B65DB7B53A6CBB824295A57432B3613D2692827FDA655029191371`。`FireDamage.java:62` 仍只调用一次资源火伤 `hurt`。
- `FireConsumption.java`：2026-11-01 16:07:05.037，`7B4039E99F7D3477367F6EBCA487967DAD660798ADEEC644EF9A6D2088E8B171`。
- `KindlePlan.java`：2026-11-01 15:25:48.449，`4C677C77376782361F70EE15E256EDF3AF73667C39B09C277D4566B379E53C03`。
- `BurnLedger.java`：2026-11-12 20:35:56.037，`68070AE950BD1909EE3F7369E085C347E976D905C6C72E5075F3C606E0E90D05`。
- `FireVisualRelay.java`：2026-11-16 11:04:00.239，`94B0A9F00D966616CD32DC9CB85E8DF47356897599DAD2DA4702D66B0A1C7E52`。

本次两个客户端文件没有 `setSecondsOnFire`、`setRemainingFireTicks` 或 `setSharedFlagOnFire`。

## 检查与收束

已跑 `powershell -NoProfile -File tools/check-name-gate.ps1`。两个本次文件无命名命中；全仓既有 289 处未修。未编译、未打包、未启动游戏，也没有游戏内验证。未写表、资源索引或批次记录，未改贴图。

源码写入已收束。无下属。下一步由父方单独授权统一编译。
