# 引爆三维候选进度写表回执（2026-11-16）

- batchId：`待制作-20261031`
- taskId：`fire_detonate_v2_progress`
- attemptId：`20261116-doc-sheet-detonate-3d-candidate-04`
- 记录者：`01a1e08b-09e0-7bb3-b2ce-d5e430f241ac`
- 直接父级：主会话 `01a1ce06-b681-7c90-b11d-260d3c169911`
- 模型：`cursor/grok-4.7@500k`，high；Fast 沿用客户端 priority，未验证后端 tier

## 接收

前一记录 attempt `20261116-doc-detonate-3d-feedback-03` 已由父方接收并关闭。美术 Ohm `01a1e336-50ed-78f0-8d74-7657a5a4e7b3`，attempt `20261116-art-detonate-3d-01`，已 final 并关闭，无下属。父方查阅主/侧时序、主/侧峰值、强度和二维帧板只是产物查阅，不是正式复审或用户确认。

## 候选

- 编号：`fire_detonate_v2_3d_candidate_20261116`
- 状态：`candidate_pending_user_confirmation`
- 内容：26 团不同 xyz 中心的像素云错时径向扩散，橙/蓝同形，20 tick 单次。
- 工具：真实 Aseprite 与 GUI Blockbench。模型只保存峰值可编辑姿态；动图为真实 GUI 逐帧捕获，不是原生动画轨道。17 个既有工程未改。
- 橙源 SHA256 `0C3EC19FE36D420F9D909B57AEFBB059D29739E9883D9ED77795B9EF657DBC86`，4614 字节。
- 蓝源 SHA256 `EADC6C6DD861261C65A54A0A09C678C2F36C55DF79F5E9E4ECA8B93F4A8C4F73`，4630 字节。
- 橙条 SHA256 `28666B39C8A80E0A18EB04DDAE7F7422A546B179307DB0990F7F8109ADF8D0F7`，2179 字节。
- 蓝条 SHA256 `C4B656B506B6CB721BCEF39CFED9BA99AA0AE21F2EDED7839057FC3573835856`，2193 字节。
- 模型 SHA256 `000F4370871D97886DAF2BB8411AAAB417847A62D3807E6CAD9786C4D0C26E0E`，329624 字节。
- 用户确认、运行接入、编译、游戏实测均未完成。现运行 v1 暂用保留。

## 制作表

- 提交前：188165 字节，SHA256 `2596AC4DD0F01B14A96A5C37AAC6EC658958B1772CDC47214B0C703447112E94`，无锁。
- 提交后：188379 字节，SHA256 `7E2B700C5F83C20964495BED4F7C2D6ED2E27F670FE9892D258C8D4A021F65E4`，mtime `2026-11-16 16:20:35.126`。
- 备份：`docs/开发记录/术式批量制作/待制作-20261031/备份/detonate-v2-3d-candidate-术式制作表-20261116-162035-1.xlsx`。
- 候选：`docs/开发记录/术式批量制作/待制作-20261031/候选/detonate-v2-3d-candidate-术式制作表-20261116-162035-1.xlsx`。
- 唯一变更：制作中第 9 行 `DV9`，1512 字增至 1829 字。N、O 和其他列未改，未迁页。
- 重开：已完成 37、待制作 0、制作中 22、待验收 19，合计 78，B 唯一。27 个 ZIP 条目保留。

设计 `2/2`、实现 `3/3` 已用完。本阶段额度已用完，返修结果未追加复审。索引、源码和美术目录未改。写入已收束，无下属。
