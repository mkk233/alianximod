# fire_seed_sow_v3_archive_sync 归档与写表回执

- batchId：`待制作-20261031`
- taskId：`fire_seed_sow_v3_archive_sync`
- attemptId：`20261115-sync-seed-sow-v3-02`
- stage：`delivery`
- 操作角色：共享文档与制作表的唯一写者
- 可观察身份：`CODEX_SESSION_ID=01a1ce06-b681-7c90-b11d-260d3c169911`，`CODEX_THREAD_ID=01a1df7c-583f-7f10-8d2f-1f1d34696a55`
- 请求模型 / 后端模型：宿主未返回，记未知
- 真实直接父代理：当前主会话，UUID 未知，不编造。父方接收时间由主会话收到本 final 后填写，本回执不代填。
- 结果：`fire_seed_sow` 已从制作中第 7 行迁入待验收第 25 行。表格同步已完成。尚未游戏实测，不能迁入已完成。
- 额度：设计 `2/2`、实现 `3/3`。本阶段额度已用完，返修结果未追加复审，未重置计数。本单只核对已有交付证据，不正式复审。
- 下属：无。未再派生。
- 编译：本单未运行 `gradlew`。

## 第 8 节核对

五项同时满足，依据是重读后的已有回执和当前文件：

- 功能实现已有完成回执。本次只读核对 `ModTechniques.java:307-309` 的技术卡文案、`BatchVisualTexture.SEED`、`BatchTriggerVisuals.seed`、`CameraFacingRegistry` 的 `SEED -> CAMERA_PLANE`，以及 `CameraFacingClient.java:43-45` 的 `0.08` 平移。未复核正确性，未改 Java。
- 独立正式美术已交付。2026-11-11 二维确认版仍在 `docs/资源库/特效/火焰/种火_上窜_v3/`。2026-11-15 同版模型补档在 `docs/资源库/过程归档/种火_上窜熄灭_v3_模型补档_20261115/`，回执为 `seed-sow-v3-model-repair-20261115.md`。
- 用户 2026-11-11 原话：“按照这版接入”。本次模型补档没有改变运行参数，不提出新版本，不重复要求同版视觉确认。
- 运行贴图 `assets/xueximod/textures/effect/seed_sow_v3/seed_sow.png` 为 2776 字节，SHA256 `A50EC7ED6A8434F50987A86036201DABFF3AB8A70CFF9D82A31CAC9F12B03EEE`，与确认版竖条一致。`.mcmeta` 为 64 字节，SHA256 `B2CAA414505E1C556CF68460294471D97198A50F3D69ED1FD9B717510C0D6F4A`。`.bbmodel` 留在示意归档，没有复制进 `src`。
- 现行 `src/main` 在文档写入前重算为 1456 个文件、13766184 字节，树 SHA256 `351eb181c4eb75b138b9370a8b2fce8caeaba71026d408a240c627f62bf408d4`。这与 2026-11-15 03:25:54.673–03:26:22.708、退出码 0 的 `compileJava` 输入相同。本次未改 `src/main`，所以既有编译仍覆盖现行字节。`compileJava` 不证明贴图、`.mcmeta` 或 Blockbench 已在 Forge 中运行。

配置沿用已有记录：`seedCount` 默认 `10`，范围 `0..100`；`manaCost` 默认 `2`；`cooldownTicks` 默认 `5`。本次未改配置代码。

## 美术事实勘误

- 真实美术代理：`01a1df75-df60-73c3-b0c0-dd95a7ef14e8`（Helmholtz）。请求链为 Carbon `Unknown model`、Argon `Unknown model`，再按流程第 11.1 节降到 `gpt-6-astra` `max`。父方已收到 final 并 close。
- `visual-handoff.md`：10769 字节，SHA256 `74B64019616063634A3B6E8D61F2CDDBDA1D37B546BD00A9CA188E398C037F39`。
- `seed-sow-v3-model-repair-20261115.md`：6119 字节，SHA256 `1A9F3DFD4D3C2AEABF1247D6BFC421F24598146240163C9DE5353B6003EF58FE`。
- `manifest.json`：14845 字节，SHA256 `B9031F1F6875ECDE1B2C1D3E9E62F3961F260611A1FB7C3C7411BBFF65025D98`。重读后，清单中的模型、四张视口和全部产物哈希都与磁盘一致。
- 模型仍是 804998 字节，SHA256 `CE7AC65C0406AE3A338FBB5CD7AE98E70D005A245520C7B4603245715DB9852C`。四张视口哈希未变。没有改脚本、图片或模型。

## 基线、备份与提交

- 提交前独占读取基线：SHA256 `8CFA3C2599EC08A531B3624522551791E208D2425892930BC6C1616DAD9CE938`，179938 字节。无 `EXCEL.EXE`，无 `docs/~$术式制作表.xlsx`。
- 备份：`docs/开发记录/术式批量制作/待制作-20261031/备份/seed-sow-v3-writeback-术式制作表-20261115-222248-1.xlsx`，179938 字节，SHA256 等于提交前基线。
- 需求快照：`docs/开发记录/术式批量制作/待制作-20261031/seed-sow-v3-requirements-snapshot-20261115.json`，20862 字节，SHA256 `CDBCB1B9734D0CA82227D432773C18C5725DBA82DF10E870285FD0396DDEDD41`。
- 保真脚本：`docs/开发记录/术式批量制作/待制作-20261031/seed-sow-v3-writeback-20261115.py`，20764 字节，SHA256 `2BD2D25DBB1FA2FECAFF7F277766A484C65F2FCADB1B588A589DA1BC15416854`。
- 候选：`docs/开发记录/术式批量制作/待制作-20261031/seed-sow-v3-writeback-candidate-preserved.xlsx`，180549 字节，SHA256 `1906BAABEBC490567AD9CDBAC250B17D2472264C2FB9E22BE97E264EF9175E54`。
- 提交：在独占读写句柄内按候选字节替换源表。提交后 `docs/术式制作表.xlsx` 为 180549 字节，SHA256 `1906BAABEBC490567AD9CDBAC250B17D2472264C2FB9E22BE97E264EF9175E54`。
- 关闭后重开：ZIP `testzip` 通过。已完成 37、待制作 0、制作中 22、待验收 19，总数 78，四页 `B` 唯一。`fire_seed_sow` 仅位于待验收第 25 行，制作中已无该编号。资源状态明细仍无该编号。待制作原始数据第 18 行未动。相对备份只改 `xl/worksheets/sheet3.xml`、`xl/worksheets/sheet4.xml`。

## 字段

- `N`：19 字，`命中被接受后并入本体基数，基础产种10`。这是 `ModTechniques.java:309` 的技术卡原文。`zh_cn.json` 没有覆盖这个编号。
- `O`：56 字，`assets/xueximod/textures/effect/seed_sow_v3/seed_sow.png`。
- `DV`：完整保留制作中 1090 字前缀，再追加 2026-11-15 美术进度和待验收流程标记，合计 2191 字。
- 除 `N`、`O`、`DV` 外，迁入行的单元格文本和行结构原样保留。样式、数据验证、批注、数据表、资源状态明细和待制作原始数据未改。

## 文档

- `docs/资源库/特效/火焰/种火_上窜_v3/README.md`：3533 字节，SHA256 `09DC0A59F2D360C0E854F70FEE750996466FB6087CE754AACA648614AEB23D95`。
- `docs/资源库/README.md`：33368 字节，SHA256 `B57C218FAB5594BF9244F94A1621D3E799B7E49B801E31C68D63B106EC2F69B0`。只改种火对应段。
- `docs/开发记录/术式批量制作/待制作-20261031/实测清单.md`：33885 字节，SHA256 `C0DE4A714F22692504EAE45D7EF07844311CD7C9A9225DA6D9BFCE6B5DF76ED5`。只改“## 种火”前置句。
- `docs/开发记录/术式批量制作/待制作-20261031/记录.md`：87613 字节，SHA256 `AB4C40E5729485A7C264A5BE7489D437CFFCEFA5F0DB4E917E62B56577309064`。只追加本段。追加前为 82445 字节、SHA256 `19D14B50A8F05C2E79FE9A66419ED357F1FA1774A6E3C24B9A6C96B39C08066E`。
- 历史 `seed-sow-v3-runtime-prep-20261115.md` 和 `flame-trail-spark-alignment-compile-20261115.md` 未改。

## 实测四行

- 编号 / 名称：`fire_seed_sow` / 种火
- 前置：创造栏或给予测试法杖。装配会命中的火攻击，并装上种火。运行贴图为 `assets/xueximod/textures/effect/seed_sow_v3/seed_sow.png`，含同目录 `.mcmeta`。
- 步骤：进入世界后打中一个目标，并转动镜头。另对没有目标的落点看一次。
- 预期：有目标时平面中心贴身体中心，边长等于碰撞高度，并向当前镜头拉近 0.08 格；没有目标时为 1.75 格正方形。画面朝当前客户端镜头，播一次后消失。用户反馈：（留空待填）

## 收束

写入已收束。未改 `src/`，未打包，未启动游戏，未操作美术 MCP 或桌面控制，未做 git 操作，未关闭用户 Excel。文件句柄已关闭。
