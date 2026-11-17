# 焚天自然收束退火实现回执

- batchId：`待制作-20261031`
- taskId：`fire_kindle_fentian_visual_swap`
- attemptId：`20261116-impl-fentian-burnout-sync-02`
- 角色：原实施作者
- 直接父级：主会话 `01a1ce06-b681-7c90-b11d-260d3c169911`
- 模型：`cursor/grok-4.7@500k`，high。工具无 tier 参数，沿用客户端 Fast `service_tier="priority"`，未声称后端独立验证。
- 本阶段额度已用完，返修结果未追加复审。
- 视觉合同：`docs/资源库/过程归档/焚天_双层收束同步_v4_1_20261116/visual-handoff.md` 与 `timeline.json`。新尾段仍是候选，不声称用户已看过本次预览。

## 修复

上一版把旧条第 40 帧只保留一拍，随后仍跳到只剩零星的第 41 帧，没有退火过渡。本次改为合同的双采样：自然结束时冻结最后实际显示的持续帧和当时 `skyScale`，再与余烬帧按两行 alpha 在同一背景矩形上叠加。

没有可用持续帧（结束包到达时客户端已经没有该 `SKY` 画面）时，索引回退 39，尺寸回退 `1.0`。结束包没有游标和剩余等待，不能反推当时尺寸，所以不会用结束余额重算 `skyScale`。有画面时，尺寸就是该画面正在使用的 `skyScale(segmentCount, cursor, remainder)`。

零起 tick 0–8，源帧取 `floor(elapsed + partialTick)`，两行 alpha 在相邻键之间按小数部分线性插值；第 8 tick 两者都是 0，不再向不存在的下一键插值。elapsed ≥ 9 删除背景收束。顶点 alpha 乘原贴图 alpha，RGB 仍是现行白色；幽冥仍走现有贴图切换，没有加暗、加法或改几何。

| tick | 持续 alpha | 余烬 alpha | 余烬索引 | 前景索引 |
|---:|---:|---:|---:|---:|
| 0 | 1.00 | 0.00 | 40 | 10 |
| 1 | 0.94 | 0.18 | 41 | 11 |
| 2 | 0.82 | 0.36 | 42 | 12 |
| 3 | 0.66 | 0.55 | 44 | 13 |
| 4 | 0.48 | 0.72 | 45 | 14 |
| 5 | 0.30 | 0.82 | 46 | 15 |
| 6 | 0.15 | 0.65 | 48 | 16 |
| 7 | 0.05 | 0.35 | 50 | 17 |
| 8 | 0.00 | 0.00 | 51 | 18 |

前景原路径已经是 tick 0–2 索引 10–12、tick 3–8 索引 13–18，tick 9 删除，本次没改它的选帧。绘制顺序改为持续背景、余烬背景、原前景。引燃、真实 4 tick 结算、伤害、燃料和非自然结束清理未改。

## 文件

改前：

- `src/main/java/com/bslx/XueXiMod/client/FireVisualClient.java`：2026-11-16 12:42:28.313，54187 字节，`A0897919EAA65F74D72F8D1BC8FAF05919A476EEED3B566A7AC20FF664DA8927`
- `src/main/java/com/bslx/XueXiMod/visual/fire/FireVisualLayout.java`：2026-11-16 12:42:19.491，11655 字节，`A3C4BDABDDCF81879B8C8C077CBC4661E86DD3A2D2DF14CBC0227AD121DFBCBC`

改后：

- `FireVisualClient.java`：2026-11-16 12:59:37.977，56508 字节，`59936F3FD13A592A9E17AD081AD7EDB4B0C448232D5B6DDD771E417025CAF402`
- `FireVisualLayout.java`：2026-11-16 12:59:37.969，13197 字节，`D1E8985C6AA6C751FB43C11CD9597C132B0EF8EC93EC2F1458E6402D208B67F1`

关键行：快照与尺寸在 `FireVisualClient.java:150-161`；绘制顺序在 `FireVisualClient.java:319-321`；双层提交在 `FireVisualClient.java:652-673`；顶点 alpha 在 `FireVisualClient.java:835-864`。选帧与插值在 `FireVisualLayout.java:122-185`。

## 检查与收束

已跑 `powershell -NoProfile -File tools/check-name-gate.ps1`。本次两个文件没有命名命中；脚本仍因既有问题非零退出，未修。没有改 PNG、`.mcmeta`、表、索引或批次记录。没有下属。源码写入收束后才进入本轮授权的编译。
