# 地表定位 v2 提交写表回执

- batchId：`待制作-20261031`
- taskId：`rule_surface_anchor_v2_sheet_write`
- attemptId：`20261114-sheet-surface-anchor-v2-02`
- stage：`delivery`
- 操作角色：`sheet-write`
- 真实代理标识：宿主返回 `CODEX_THREAD_ID=01a1d8ce-256d-7f12-8118-b8539d9c593d`，`CODEX_SESSION_ID=01a1ce06-b681-7c90-b11d-260d3c169911`。后端模型名未在环境变量中返回，记未知。
- 真实直接父代理：主会话。父方接收时间由主会话收到本 final 后填写，本回执不代填。
- 关联 B：`rule_surface_anchor`（地表定位）
- 结果：`rule_surface_anchor` 已从制作中第 3 行迁入待验收第 24 行。表格同步已完成。尚未游戏实测，不能迁入已完成。
- 额度：设计 `2/2`、实现 `3/3`。本阶段额度已用完，返修结果未追加复审，未重置计数。本单只核对已有交付回执，不正式复审。
- 下属：无。子代理标识不适用。
- 编译：本单未运行 `gradlew`。

## 第 8 节核对

五项同时满足，依据是重读后的已有回执和当前文件，不是上一作者的完成句：

- 功能实现、串台修复和包围盒返修均已收束。回执分别为 `surface-anchor-v2-integration-report-20261114.md`（7843 字节，SHA256 `46AAEBAF62C8D9AE7DC413A8E6B67F30CF005070CA7E3297996AE98D8809950F`）、`surface-marker-stray-fix-20261114.md`（3661 字节，SHA256 `C1CF618ABBA066D4B14D6B896EA5F247DADE23910B690497D7781B3A6C12F244`）、`surface-anchor-v2-integration-report-02-20261114.md`（4201 字节，SHA256 `3D953F09A210CAA9DEEDB60A6B4654C46DBC4FDD05B8FBAD664879FF74256F6E`）。
- 独立正式美术已交付。`surface-anchor-v2-art-archive-20261114.md` 为 10417 字节，SHA256 `1D0E08435BA8C03D57E40751E885BF5D83709DAC09A96572597C0603B46504DF`。正式目录 `docs/资源库/特效/标记/地表定位_倒三角_v2/` 现有 12 个文件，回执中的 12 个 SHA256 都能在该目录找到。
- 用户 2026-11-14 原话：「你先继续，我忘了地面定位之前你给出的版本我还没说采用，之前那个版本的效果可以接入」。
- 运行贴图 `assets/xueximod/textures/effect/surface_anchor_v2/surface_anchor.png` 为 4741 字节，SHA256 `4434BDC7B0311E863277E9BD23761139E77489AD6B548FBB48AC202C4FB35A20`，与确认版竖条一致。`.mcmeta` 为 64 字节，SHA256 `B2CAA414505E1C556CF68460294471D97198A50F3D69ED1FD9B717510C0D6F4A`，内容为 `blur=false`、`clamp=true`。
- 最终编译 `20261114-compile-surface-anchor-v2-02` 的回执为 11342 字节，SHA256 `9D2D881D1C015F5CFC1FF7217FC358894435BFB983672DE56808852B12D883E3`；日志为 577 字节，SHA256 `E8EAA8FD4D35AF51C23C44FAD657DF6A2A368562A7B5645B77290FAD13B5E68F`。日志写明 2026-11-14 04:54:01.291 至 04:54:14.077，`BUILD SUCCESSFUL`，`:compileJava` 为 `UP-TO-DATE`。开始时间晚于最后相关源码写入 `BatchCarrierVisual.java` 2026-11-14 04:48:11.855。六个目标源码/资源当前哈希仍等于该编译回执。

配置兼容记录齐全：`cooldownTicks` 默认 `0 tick`，范围 `0..1200000`；`manaCost` 默认 `0`，范围 `0..1000000`。无伤害倍率，无专项键。装配消耗 `1` 不开放。视觉常量不开放。

## 基线、备份与提交

- 提交前独占读取基线：SHA256 `AB344CCAAFBE723AD0ED6C6DD0834EB3B264EC5184227F655FAE9FC1B0F12C26`，179499 字节，LastWriteTime `2026-11-13 13:07:13`。无 `EXCEL.EXE`，无 `docs/~$术式制作表.xlsx`。源表哈希没有偏离任务单基线，不走方案 B。
- 本次备份：`docs/开发记录/术式批量制作/待制作-20261031/备份/surface-anchor-v2-writeback-术式制作表-20261114-144949-1.xlsx`，179499 字节，LastWriteTime `2026-11-14 14:49:49.978`，SHA256 等于提交前基线。旧备份 `surface-anchor-v2-writeback-术式制作表-20261114-050658-1.xlsx` 未改。
- 需求快照：`docs/开发记录/术式批量制作/待制作-20261031/surface-anchor-v2-requirements-snapshot-20261114.json`，22537 字节，SHA256 `AA80DFD19C0E8F48007F56D58D259BE8438959D83BE209239A2FE84D562452BB`。
- 保真脚本：`docs/开发记录/术式批量制作/待制作-20261031/surface-anchor-v2-writeback-20261114.py`，24217 字节，SHA256 `2081A558522E96830D18F42BFDC242B445A92C6AE69907EB2A784EFF0E019A52`。本单未运行该脚本，也没有用表格库重写候选。
- 候选：`docs/开发记录/术式批量制作/待制作-20261031/surface-anchor-v2-writeback-candidate-preserved.xlsx`，179938 字节，SHA256 `8CFA3C2599EC08A531B3624522551791E208D2425892930BC6C1616DAD9CE938`。提交前再次读取，哈希未变。
- 提交：在独占读写句柄内按候选字节替换源表。提交后 `docs/术式制作表.xlsx` 为 179938 字节，LastWriteTime `2026-11-14 14:49:49.994`，SHA256 `8CFA3C2599EC08A531B3624522551791E208D2425892930BC6C1616DAD9CE938`。
- 关闭后重开：ZIP `testzip` 通过。已完成 37、待制作 0、制作中 23、待验收 18，总数 78，四页 `B` 唯一。`rule_surface_anchor` 仅位于待验收第 24 行，制作中已无该编号。资源状态明细仍无该编号。待制作原始数据第 14 行未动。相对本次备份只改 `xl/worksheets/sheet3.xml`、`xl/worksheets/sheet4.xml`。

## 字段

- `N`：30 字，`视线内第一个向上承托面上出现一个悬空倒三角。失败则整组不释放`。
- `O`：68 字，`assets/xueximod/textures/effect/surface_anchor_v2/surface_anchor.png`。
- `DV`：完整保留制作中 1284 字前缀，再追加 2026-11-14 阶段记录和待验收流程标记，合计 2062 字。
- 除 `N`、`O`、`DV` 外，迁入行的单元格文本和行结构原样保留。样式、数据验证、批注、数据表、资源状态明细和待制作原始数据未改。
- 前次未提交回执把 `N` 写成 31 字、`O` 写成 60 字。本次按重开文本记录 30 字和 68 字，没有因此改单元格。

## 实测四行

- 编号 / 名称：`rule_surface_anchor` / 地表定位
- 前置：创造栏或给予测试法杖；把地表定位放在需要落点的术式前面。运行贴图为 `assets/xueximod/textures/effect/surface_anchor_v2/surface_anchor.png`，并有同目录 `surface_anchor.png.mcmeta`。正式归档为 `docs/资源库/特效/标记/地表定位_倒三角_v2/`。基础法杖仍是 6 槽。
- 步骤：进入世界后瞄准视线内的地面释放。再故意瞄准没有承托面的天空，确认整组被拒绝。另用未装配地表定位的幽冥空中射出点对照，确认标记不会误播。
- 预期：视线内首个向上承托面出现悬空 `0.80×0.80` 格、`Y∈[+0.04,+0.84]`、`±45°` 正交双面像素倒三角。`CameraFacingTag.NONE`，不随镜头转；纯白 `FULL_BRIGHT`，不受幽冥火焰染色。同组只对 `needsSurface` 落点播一次，不在幽冥空中射出点误播。选点失败整组不释放、不扣费、不冷却。用户反馈：（留空待填）

## 新需求登记

- 用户原话：三组示意按新底座接入（仅“弹道规则”单一类别单独装配显示预览，组合隐藏；地表定位例外常规显示，索敌锁定不适用）
- 紧接纠正：理解有误，是弹道规则类不是弹道类或者规则类
- 美术任务 `standalone_rule_preview_art_handoff` / `20261114-art-rule-preview-01`：原生代理 `01a1d8d1-bd3b-7b03-85c3-24c81ba8ab1f`，请求模型 `antigravity/gemini-4-carbon`，立即 `errored`，HTTP `403 Forbidden`，原文 `This service has been disabled in this account for violation of Terms of Service. Please submit an appeal to continue using this product.` 已 `close`，无素材交付。这不是额度耗尽。本次不通过改账户、路由或模型绕过该 403。不影响已经提交的地表定位 v2。
- 本段只登记上述原话、纠正和这次失败事实，不扩展含义。新代码尚未开始。除 `rule_surface_anchor` 外，没有迁移其他术式。

## 收束

已追加 `记录.md`，已写入本回执。同期美术任务的 403 只按失败事实记入新需求登记，未因此改表。未改 `src/`、`docs/资源库/`，未改 `实测清单.md`。未打包，未启动游戏，未做 git 操作，未关闭用户 Excel。文件句柄已关闭。
