# 崩地 v2 候选准备记录

2026-11-16。batchId `待制作-20261031`；taskId `earth_break_ground_v2_art_preview`；attemptId `20261116-record-ground-v2-preparation-01`。

直接父级为主会话 `01a1ce06-b681-7c90-b11d-260d3c169911`。记录者 `01a1e08b-09e0-7bb3-b2ce-d5e430f241ac`。模型 `cursor/grok-4.7@500k`，high；Fast 沿用客户端 `service_tier="priority"`，不声称后端已验证。

本阶段额度已用完，返修结果未追加复审。设计 2/2、实现 3/3 不变。本次不是正式复审。无下属。未写制作表，未改源码，未编译，未启动游戏。

## 表

只读。`docs/术式制作表.xlsx` 为 190802 字节，SHA256 `87F22C7DCE22FD0AB83F219C1D18938AF381D2286C106A62CEBCA1A6A56E9C75`，mtime `2026-11-16 21:22:44.657`，无锁。四页 37/0/21/20，合计 78，B 唯一。

`earth_break_ground`（崩地）在制作中第 11 行。未迁页。资源状态明细没有该编号。幽冥记录原文未改。薄水切、断潮不在本轮。

非空字段：类型附魔，属性土，基础冷却 5，等级上限 20，复杂度 3，附魔是，装配消耗 2，登记魔力 2，必须手持剑为否。弹道、链上角色、结算约束为空。

- N 236 字，SHA256 `47A9078AF2E686E9BC4C653C85CE3A9A39EF6D7ED18704FCE2A64F463B6F1176`。
- O 26 字，SHA256 `EBB5A0CF0E676AB7BB49439342079CA6BFCF8CB5EDE142CF9C1B5419B624CD37`。原文：“撞击处出现一次坑，不另起爆炸，也不额外打出一轮伤害。”
- DV 1116 字，SHA256 `15D06C6A8539A721F2EA28D75146009674B670A96B05954A7F1E2979DE567894`。原文完整保留在快照，含历史用户暂用二维原话和 `approved-v1.md` 指针。

本轮用户原话只有“下一个术式”。快照里的历史原话不改写成这次对 v2 的确认。

## 已接收事实

Flash Anscombe `01a1e493-caad-7b40-b580-604e101a1815` 已完成只读并关闭，无写入。

现行 v1 暂用二维已接入。归档 `docs/资源库/特效/大地/崩地_v1/ground_dust-strip.png` 与运行 `assets/xueximod/textures/effect/ground_dust_v1/ground_dust.png` 同为 SHA256 `0C7479F9F3DC02E3216D7EBF6A195C98730CF9841A57693C210658821F0A476A`。没有 Blockbench。资源库总 README 对崩地仍写“尚未复制”，该句已过时；本轮不改共享索引，等候选收尾再同步。

运行 16 tick，即 0.8 秒；原 Aseprite 为 16 帧 × 80 毫秒，即 1.28 秒。halfSpan 1.25，挂点在方块中心，DUST、CAMERA_PLANE、世界光照。`BreakGroundShape`（r²≤4、d 为 0 到 2、2d≤4-r²）实际 23 块，上限 32；半径 2，向下偏移 2，含撞击层共 3 层。这是现状，不是设计改动。

播放缺口：当前只看到 `BatchMeteorLanding` 第 50–58 行、`MeteoriteWreckageEntity` 第 256–266 行，在 `breakExistingOnce` 为 EXECUTED 后播放。未找到普通攻击碰撞通路。这只登记缺口，不作为新的正确性复审，也不修功能。

既有幽冥编译受无关木系错误阻塞。本轮不重跑 Gradle。

## 美术派发

候选正在制作，尚未用户确认，尚未接入，不能替换运行 v1。本轮未读美术目录或中间产物。

- `antigravity/gemini-4-carbon`，attempt `20261116-art-ground-v2-01`：Unknown model，无 agent_id，无产物。
- `antigravity/gemini-4-argon`，attempt `20261116-art-ground-v2-02`：同样 Unknown model，无 agent_id，无产物。
- 这不是额度耗尽。按流程第 11.1 节改用 `gpt-6-astra` max，attempt `20261116-art-ground-v2-03` 成功。真实代理 Feynman `01a1e49b-2cc4-7390-a605-76028c8b0f08`。不计复审，不猜测后端。

美术独占 `docs/资源库/过程归档/崩地_坑口扬尘_v2_20261116/`，独立回执 `ground-v2-art-preview-20261116.md`。范围按已批准“只有土尘、不另起爆炸”：短促坑口扬尘散尽，以及真实方块坑。交付应含 Aseprite 源、透明导出、二维 GIF、真实 Blockbench 模型和空间动态预览。

父方本轮已连接喜鹊：Aseprite 1.3.17，batch ready，live=false；Blockbench 桌面 5.2.2 / plugin 1.10.1。当前幽冥工程 UUID `bd0ab5c3-e2ff-a2d3-b619-3ba215a682d2` 保持保护，另建新工程。禁止桌面控制，不用 ImageGen，不用 headless 伪截图。记录者未再调用这些工具。

## 分列

- 功能：现行 v1 已接入；普通攻击播放通路缺口只登记，不修。
- 美术：v2 坑口扬尘候选进行中，不是完成。
- 用户确认：v2 未确认。历史二维暂用原话只保留在 DV。
- 正式接入：未做。运行仍是 v1。
- 仍留制作中。未编译，未游戏实测，不称待验收或复审通过。

需求快照 `ground-v2-requirements-snapshot-20261116.json`。写入已收束。
