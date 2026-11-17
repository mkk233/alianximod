# 种火 v3 运行元数据修正回执（2026-11-15）

## 任务身份

- batchId：`待制作-20261031`
- taskId：`fire_seed_sow_v3_runtime_prep`
- attemptId：`20261115-impl-seed-sow-prep-01`
- stage / 角色：`implementation` / `grok-integration-and-record`
- 关联术式：`fire_seed_sow`（种火）。继续留在 `制作中!7`。本单未迁表。
- 授权：用户 2026-11-11 确认「按照这版接入」；本轮指示「可以了继续下一个术式吧」「好了MCP应该可以用了你看看」。
- 复审：设计 `2/2`、实现 `3/3` 已用完。不追加正式复审，不重置计数。本阶段额度已用完，返修结果未追加复审。

## 输入基线

- `docs/术式制作表.xlsx`：179938 字节，SHA256 `8CFA3C2599EC08A531B3624522551791E208D2425892930BC6C1616DAD9CE938`。本单未打开、未改写；写完后复核仍是这个哈希。
- 运行贴图 `src/main/resources/assets/xueximod/textures/effect/seed_sow_v3/seed_sow.png`：2776 字节，SHA256 `A50EC7ED6A8434F50987A86036201DABFF3AB8A70CFF9D82A31CAC9F12B03EEE`。本单未改。
- 模板 `src/main/resources/assets/xueximod/textures/effect/surface_anchor_v2/surface_anchor.png.mcmeta`：64 字节，SHA256 `B2CAA414505E1C556CF68460294471D97198A50F3D69ED1FD9B717510C0D6F4A`。内容为 `{"texture": {"blur": false, "clamp": true}}`，CRLF。

## 功能状态

本单未改 Java。当前源码里种火已经接上，依据是：

- `ModTechniques.java:307` 声明 `FIRE_SEED_SOW`（`fire_seed_sow`），基础冷却 5 tick；`:423` 注册。
- `ResourceHitCommit.java:22-49`：伤害被接受后合并产种，再挂引燃和引爆。
- `SeedMerge.java:22-29`：按施法者当前炉心产种倍率合并后入同一余额。
- `BatchVisualTexture.java:12`：`SEED` 指向 `textures/effect/seed_sow_v3/seed_sow.png`。
- `CameraFacingRegistry.java:17`：`SEED` 为 `CAMERA_PLANE`。
- `CameraFacingClient.java:43-45`：种火平面向当前镜头平移 `0.08`。
- `BatchTriggerVisuals.java:87-103`：以 `BatchCarrierIds.STRIP_FRAMES`（16）作为持续刻数，生成一次贴身平面。
- 已开放配置键：`seedCount` 默认 `10`，范围 `0..100`（`ResourceEnchantNumbers.SOW_BASE`，`FireLimits.SEED_CAP`，`EnchantmentDefinitions.java:111-114`）；`manaCost` 默认 `2`（`ResourceEnchantNumbers.MANA_PRICE`，标准魔力键）；`cooldownTicks` 默认 `5`（术式基础冷却，标准冷却键）。

## 美术、确认与正式接入

二维 v3 已由用户于 2026-11-11 确认「按照这版接入」。运行贴图与同目录 `seed_sow.png.mcmeta` 已就位。

缺口：真实 Blockbench `.bbmodel` 尚未补档。本单只读核对 `docs/资源库/过程归档/种火_上窜熄灭_v3-20261111/shot.js` 第 11 行仍是 `Codecs.project.write(dir+"/seed_sow_v3_preview.bbmodel")`，同目录没有 `seed_sow_v3_preview.bbmodel`。本会话未挂载 `mcp__magpie`，不能在真实 Blockbench 里补档。制作表 `制作中!DV7` 仍待补档完成后与迁页核对一并更新，故 `fire_seed_sow` 继续留在 `制作中!7`。不据此宣称第 8 节条件已齐备。

## 本轮前置子代理

本单未再派生子代理。下面按已交回的真实回执登记，本单没有复测这些进程。

- 美术代理 Rawls `01a1db1e-9161-7623-b363-e26e79feed55`。请求 `antigravity/gemini-4-carbon` `high`，`attemptId=20261115-art-seed-sow-02`。本次未再报 403，正常返回并已 `close_agent`。有界检查确认本机 `magpie-windows-amd64.exe` PID `22016` 监听 `127.0.0.1:3425`、`magpie-bun.exe` PID `29284`、`Blockbench.exe` PID `45904`/`46068` 监听 `127.0.0.1:3000` 均在运行，`Aseprite` 未运行。主会话与新建子代理的工具延迟命名空间均只有 12 项且不含 `mcp__magpie`，2 次 `tool_search` 均无匹配。零文件写入。
- 只读 Flash Averroes `01a1db1e-922a-7b33-9d72-30621b60a685`。请求 `deepseek/deepseek-flash` `low`，`attemptId=20261115-flash-seed-sow-01`。已完成 `制作中!7..11` 原文及 `fire_seed_sow` 全链路事实核对并 `close_agent`。零写入。

## 本单修改

| 文件 | 修改前 | 修改后 |
| --- | --- | --- |
| `src/main/resources/assets/xueximod/textures/effect/seed_sow_v3/seed_sow.png.mcmeta` | 不存在 | 64 字节，SHA256 `B2CAA414505E1C556CF68460294471D97198A50F3D69ED1FD9B717510C0D6F4A`，与模板逐字节相同 |
| `docs/资源库/特效/火焰/种火_上窜_v3/README.md` | 1927 字节，24 行，SHA256 `0495A8165B37D2244C0DCDDCD4D19BBAB9BD2112F77F5735E5661D108C4984A6` | 2380 字节，24 行，SHA256 `AC94A773966CB341AECB872AEF882EC622ACDB307AD5AB27E3FAC51B355D0245` |
| `docs/开发记录/术式批量制作/待制作-20261031/实测清单.md` | 33742 字节，391 行，SHA256 `B8B539EBE0C1960591BF8AA52D05BF1EB351875D7492BE53CD7243A4AAE9A7EF` | 33885 字节，391 行，SHA256 `E0BE6AFC18A62DE9AE0430622DA4EAC5AE52BFBDE6F03A9777BD598865C7D975` |
| `docs/开发记录/术式批量制作/待制作-20261031/记录.md` | 73259 字节，360 行，SHA256 `69261EB89FE8C4B01A1A3E2C13F0BE81255A36FA3CC5EAE9037A2BBB69DC3E98` | 本回执落盘后才追加。追加后的哈希不写回本文，避免和记录节循环改写 |

`seed_sow.png` 与 `docs/术式制作表.xlsx` 的哈希在修改前后都未变。实测清单只替换了「## 种火」第 209 行的旧 `seed_sow_v1` 路径，其余行未改。README 只改正文里不存在的 `.bbmodel` 死链，并在运行路径补上 `.mcmeta`；确认信息、规格和原有哈希未改。

## 编译

待统一编译（本次未改 Java 源码，前次 compileJava 退出码 0 仍覆盖当前全部 Java 源码；待种火 .bbmodel 补档及后续源码/资源收束后统一核对）。

## 下一步

待客户端新会话恢复暴露 `mcp__magpie` 后，继续 `fire_seed_sow_v3_model_repair`，补齐真实 Blockbench `.bbmodel` 与视口核验，再按流程第 8 节统一核对写表。
