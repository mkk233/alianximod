# 地表定位 v2 写表回执

- batchId：`待制作-20261031`
- taskId：`rule_surface_anchor_v2_sheet_write`
- attemptId：`20261114-sheet-surface-anchor-v2-01`
- stage：`delivery`
- 操作角色：`sheet-write`
- 真实直接父代理：主会话。父方接收时间由主会话收到本 final 后填写，本回执不代填。
- 关联 B：`rule_surface_anchor`（地表定位）
- 额度：设计 `2/2`、实现 `3/3`。本阶段额度已用完，返修结果未追加复审，未重置计数。
- 编译：本单未运行 `gradlew`。

## 第 8 节核对

五项同时满足：

- 功能实现、串台修复和包围盒返修均已收束。合法播放只在 `needsSurface` 且选点成功时于落点播一次。
- 独立正式美术已交付。正式目录 `docs/资源库/特效/标记/地表定位_倒三角_v2/` 的 12 个文件与归档回执 SHA256 一致，含二维源、导出、预览和真实 Blockbench 视口。
- 用户 2026-11-14 原话：「你先继续，我忘了地面定位之前你给出的版本我还没说采用，之前那个版本的效果可以接入」。
- 运行贴图 `assets/xueximod/textures/effect/surface_anchor_v2/surface_anchor.png` 为 4741 字节，SHA256 `4434BDC7B0311E863277E9BD23761139E77489AD6B548FBB48AC202C4FB35A20`，与确认版竖条一致。`.mcmeta` 为 64 字节，SHA256 `B2CAA414505E1C556CF68460294471D97198A50F3D69ED1FD9B717510C0D6F4A`。
- 最终编译 `20261114-compile-surface-anchor-v2-02` 于 2026-11-14 04:54:01.291 开始、04:54:14.077 结束，退出码 `0`，晚于最后相关写入 04:48:11.855。`:compileJava` 为 `UP-TO-DATE`，四个目标 class 已于 04:48:47 至 04:48:48 生成。

配置兼容记录齐全：`cooldownTicks` 默认 `0 tick`，范围 `0..1200000`；`manaCost` 默认 `0`，范围 `0..1000000`。无伤害倍率，无专项键。装配消耗 `1` 不开放。视觉常量不开放。

## 基线与候选

- 共享只读基线：SHA256 `AB344CCAAFBE723AD0ED6C6DD0834EB3B264EC5184227F655FAE9FC1B0F12C26`，179499 字节，LastWriteTime `2026-11-13 13:07:13.390`。
- 源表重开：已完成 37、待制作 0、制作中 24、待验收 17，总数 78，四页 `B` 唯一。`rule_surface_anchor` 在制作中第 3 行。
- 备份：`docs/开发记录/术式批量制作/待制作-20261031/备份/surface-anchor-v2-writeback-术式制作表-20261114-050658-1.xlsx`，179499 字节，SHA256 等于基线。
- 需求快照：`docs/开发记录/术式批量制作/待制作-20261031/surface-anchor-v2-requirements-snapshot-20261114.json`，22537 字节，SHA256 `AA80DFD19C0E8F48007F56D58D259BE8438959D83BE209239A2FE84D562452BB`。
- 保真脚本：`docs/开发记录/术式批量制作/待制作-20261031/surface-anchor-v2-writeback-20261114.py`，24217 字节，SHA256 `2081A558522E96830D18F42BFDC242B445A92C6AE69907EB2A784EFF0E019A52`。
- 候选：`docs/开发记录/术式批量制作/待制作-20261031/surface-anchor-v2-writeback-candidate-preserved.xlsx`，179938 字节，SHA256 `8CFA3C2599EC08A531B3624522551791E208D2425892930BC6C1616DAD9CE938`。
- 候选重开：已完成 37、待制作 0、制作中 23、待验收 18，总数 78，四页 `B` 唯一。目标位于待验收第 24 行，制作中已无该编号。相对备份只改两张状态页 XML。

## 表格同步未完成

保真候选已构建并重开核对通过；源表当前被 `EXCEL.exe` PID `77548` 打开占用，`docs/~$术式制作表.xlsx` 存在，独占打开返回占用错误。提交前共享流哈希仍等于基线。未结束 Excel，未覆盖源表。`rule_surface_anchor` 在源表仍位于制作中第 3 行，待关闭 Excel 后在基线未变时替换提交。

源表没有新 SHA256，因为没有替换。提交后的源表哈希不适用。

## 字段

- `N`：`视线内第一个向上承托面上出现一个悬空倒三角。失败则整组不释放`
- `O`：`assets/xueximod/textures/effect/surface_anchor_v2/surface_anchor.png`
- `DV`：完整保留制作中 1284 字前缀，再追加 2026-11-14 阶段记录和待验收流程标记。
- `zh_cn.json` 没有 `technique.xueximod.rule_surface_anchor` 或 `.desc`。技术卡原文在 `ModTechniques.RULE_SURFACE_ANCHOR`，为「视线内第一个向上承托面。失败则整组不释放」。待验收 `N` 沿这句补上已接入的悬空倒三角。失败提示仍是「视线里没有可用的承托面，这次没有释放」。语言文件未改。

## 实测四行

- 编号 / 名称：`rule_surface_anchor` / 地表定位
- 前置：创造栏或给予测试法杖；把地表定位放在需要落点的术式前面。运行贴图为 `assets/xueximod/textures/effect/surface_anchor_v2/surface_anchor.png`，并有同目录 `surface_anchor.png.mcmeta`。正式归档为 `docs/资源库/特效/标记/地表定位_倒三角_v2/`。基础法杖仍是 6 槽。
- 步骤：进入世界后瞄准视线内的地面释放。再故意瞄准没有承托面的天空，确认整组被拒绝。另用未装配地表定位的幽冥空中射出点对照，确认标记不会误播。
- 预期：视线内首个向上承托面出现悬空 `0.80×0.80` 格、`Y∈[+0.04,+0.84]`、`±45°` 正交双面像素倒三角。`CameraFacingTag.NONE`，不随镜头转；纯白 `FULL_BRIGHT`，不受幽冥火焰染色。同组只对 `needsSurface` 落点播一次，不在幽冥空中射出点误播。选点失败整组不释放、不扣费、不冷却。用户反馈：（留空待填）

## 收束

已追加 `记录.md`，已更新 `实测清单.md` 的地表定位条目，其余实测条目保留。未改 `src/`、`docs/资源库/`，未打包，未启动游戏，未做 git 操作。文件句柄已关闭。

下一步：关闭 Excel 后，确认源表仍是基线 `AB344CCAAFBE723AD0ED6C6DD0834EB3B264EC5184227F655FAE9FC1B0F12C26`，再用候选副本替换源表并重开核对。
