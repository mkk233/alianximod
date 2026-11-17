# 协同回旋斩击：游戏内回旋和挥砍姿态返修

用户 2026-11-04 实测：分身已经协同，但没有正确回旋，确认的挥砍姿态也没有出现。这次只修这条链路。没有统一编译，没有游戏验证，没有追加实现复审。斩光空间参数、水幻形绘制顺序和空间切割接入没有改。

## 根因

两处客户端链路会把已加载的确认曲线变成看不见的姿态。

1. 根偏航被矩阵反解折掉。`spin.json` 的玩家根偏航是 0、-45、160、360、380、360 度，镜像轨是相反数。`CooperativePoseApplier` 把根偏航乘进每个骨骼后再用 `VanillaEuler.from` 反解。反解的偏航来自 `asin`，只覆盖大约 ±90 度；整圈旋转的矩阵是单位矩阵，结果变回 0。`from` 对合法矩阵不抛异常，原来的加法兜底不会走到。采样本身没有把 360 收成 0。

2. 动作一开始就被判成结束。`FamilyExecutionService` 把 `MinecraftServer.getTickCount()` 写进 `ActionPlayback.serverStartTick`（回旋用 `runningTick`，斜斩用同一次开机刻）。客户端用 `Level.getGameTime()` 相减。开机刻从本次服务器启动算，世界年龄跟着存档走。存档已经跑过一段之后，差值立刻大于前摇加收势，`pose` 返回空，玩家和分身都不会写入骨骼。协同位移不走这条姿态，所以分身仍会一起动。

斜斩单独播放时根偏航是 0，第 1 点不会单独抹掉出刀。第 2 点会把斜斩和回旋一起掐掉。回旋进行中，服务端和客户端都会拒绝用斜斩替换尚未结束的回旋；回旋曲线里的手臂关键帧就是这段挥砍，时机没有改。

第一人称原先只在原版手持物上叠一个四元数。手持剑时原版不画手臂，出刀和收势没有肢体。

## 更改

- `CooperativePoseApplier`：骨骼只写 Blockbench 局部欧拉角和绑定枢轴。根偏航与朝向差合成一个未归一弧度，在身体 `renderToBuffer` 前绕模型竖直轴旋转。不写实体位置、实体朝向或相机。
- `LivingEntityRendererBodyMixin`：只多这一次父级旋转。空间切割追加和水幻形提前返回没有调换。
- `CooperativeWeaponCopy`：分身武器副本使用同一个父级根偏航，握柄仍是确认的 `[-1, 9, 0]` 像素。
- `CooperativeActionViews`：收包世界时间与开始刻相差不超过 200 刻时仍按服务器开始刻快进；超出则从首次收包时刻播放，同一序号的追踪包不重置起点。
- `CooperativeFirstPerson`：主手这次绘制左右臂、袖套和握柄。根偏航与手臂欧拉角和第三人称同一套。相机和玩家位置不动。
- `ItemInHandRendererActionMixin`：动作持有期间改走上述第一人称绘制，不再只清挥砍进度。

运行 JSON 和斩光贴图没有改。`normal` 给玩家，`mirrored` 给镜像分身，两条都进入 `CooperativeActionTracks.sample`。

## 资源映射

| 动作 | 运行文件 | 采样 |
| --- | --- | --- |
| `xueximod:oblique_slash` | `assets/xueximod/actions/cooperative_v1/oblique_slash.json` | 前摇 0 秒，收势 0.40 秒；`arm_r` 从 (65, 0, -75) 收到 (0, 0, -5) |
| `xueximod:spin` | `assets/xueximod/actions/cooperative_v1/spin.json` | 根偏航保留 360 与 380；镜像根偏航为相反数 |

## 定向自检

- 根偏航不再经过 `VanillaEuler.from`。360 度与 380 度按原角度做父级旋转。
- 玩家轨和镜像轨仍按 `mirrored` 选择。
- 第一人称主手绘制出刀臂和收势臂，物品挂在对应握柄。
- 钟差过大时不再瞬间过期。钟一致时仍用开始刻快进。
- `powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 0。

未统一编译。未游戏验证。本阶段实现额度已用完，这次返修没有追加复审。

## 跨作者待办

视觉侧已经能在钟不一致时从收包时刻播放。迟到包的快进仍要求开始刻和世界年龄是同一把钟，这一处在共享执行里，本单没有改。

- 符号：`FamilyExecutionService` 内 `runningTick = player.getServer().getTickCount()`，`startSpin` 把它写入 `ActionPlayback`，`playSlash` 写入 `player.getServer().getTickCount()`。
- 参数：`ActionPlayback.serverStartTick`。
- 需要的改动：回旋和斜斩都改为该玩家所在 `ServerLevel.getGameTime()`。
- 证据：客户端 `CooperativeActionViews` 用 `Level.getGameTime()` 计算经过刻。开机刻在重启后归零，世界年龄不归零。
