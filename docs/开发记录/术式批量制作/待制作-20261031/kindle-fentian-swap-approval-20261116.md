# 引燃与焚天同版素材互换登记

本回执登记用户已明确批准的素材用途互换，并完成焚天返修退回制作中。不复审，不等待美术或实现。设计 `2/2`、实现 `3/3` 已用完。本阶段额度已用完，返修结果未追加复审。

## 身份

- batchId `待制作-20261031`，taskId `fire_kindle_v2_progress`，attemptId `20261116-record-kindle-fentian-swap-04`。角色是本轮唯一记录者和制作表写回者。
- 同一写回代理 `01a1e08b-09e0-7bb3-b2ce-d5e430f241ac`，会话 `01a1ce06-b681-7c90-b11d-260d3c169911`。本任务包没有另给请求模型或服务模型，不新报身份。直接父代理是主会话，父 UUID 未暴露。未派子代理。
- 只读 Franklin `01a1e237-8bcf-7a30-b6dd-16ed64dde350` 已 final 并关闭。其读到的表哈希就是上一轮拒绝反馈写入后的源表，不是来源不明的并发改动。

## 用户原话与批准映射

用户原话：「这个效果很好但是不适合作为燃烧的特效，因为这个火比较猛不像是初级术式这样你把现在焚天的特效先拿来给引燃用，把这个效果给焚天」。

这是同版素材用途互换并授权接入，不是等待同一素材再确认。

- 原焚天 v2 归 `fire_kindle`。批准像素 SHA256 `3AE6A95E41D92991B828EDC7491CBB40A18D503CDCF700281A50D901D6AA6A76`，计划运行路径 `assets/xueximod/textures/effect/kindle_v4/kindle.png`。
- 强烈 v3 归 `fire_fentian`。批准像素 SHA256 `EF3476EB01225D2E7478F9BBA5430F28BCF2E1D12198F3F482A634C155A04070`，计划运行路径 `assets/xueximod/textures/effect/fentian_v3/conflagration.png`。
- 两份运行文件都还没有实现回执证明已复制。本回执不冒称接入完成。

## 已派作者

- 美术 Planck `01a1e08a-817e-7b60-b465-7a8baaca1f4b` 已 resume。taskId `fire_kindle_v2_art`，attemptId `20261116-art-kindle-fentian-swap-04`，请求模型 `gpt-6-astra` `max`。降级仍沿用记录第 408、431 行：Carbon 与 Argon 为 `Unknown model`。授权目录是 `docs/资源库/特效/火焰/引燃_承接焚天_v4/`、`docs/资源库/特效/火焰/焚天_全身烈焰_v3/`、`docs/资源库/过程归档/引燃焚天_确认版互换_20261116/`，以及 `kindle-fentian-swap-art-20261116.md`。本回执未读这些进行中的文件。
- 实现 Sartre `01a1e246-a7e1-7861-acdf-2569d7b9ecee`，taskId `fire_kindle_fentian_visual_swap`，attemptId `20261116-impl-kindle-fentian-swap-01`，请求模型 `cursor/grok-4.7@500k` `high`。本轮唯一 `src` 作者，尚未获编译授权。预期回执 `kindle-fentian-swap-impl-20261116.md`。本回执未读其进行中的内容。

## 需求恢复

- `fire_kindle` 留在制作中第 8 行。N/O 逐字未动。DV 只追加本次原话和批准映射。
- `fire_fentian` 按流程第 9 节从待验收第 3 行退回制作中，现为第 28 行。整行其余单元格随行保留。
- 恢复依据是 `approved-requirements.json` SHA256 `E5C903294E9DF63A8552119BC6624EEC7521FA9636FE9F9003CE38FD0C53F922`，并与 `final-writeback-conversion.json` SHA256 `B73C3BA211EEA174D922578F310C8A2795D70A3B1431E30117DDDD14C5CCB653` 的转换前 N/O 逐字一致。N 为 594 字，SHA256 `2CB1DCCFF32F7AFE40093CF6840926A03F6C2409A22A29A63A953286D17A8190`。O 的 141 字前缀 SHA256 `33A851410DC047730E54E2F5B844907046D6F9A96B4753DA06BBA0266309E540`。
- `docs/开发记录/焚天定时燃尽-20261112/需求快照.json` 有更新的合并稿，但该轮源表被占用，没有提交。该记录自己也把 2026-10-29 的详细 N/O 定为最近已批准快照。本次不采用合并稿，不改玩法。
- O 在 141 字前缀后追加本次用户明确的素材互换。新 O 为 425 字，SHA256 `2DF457DFCBFE500DA96B2BE539A2270F5202E48D1C1CC9D3F329D9501382348D`。
- 待验收展示句和旧资源名留在转换前快照及本回执：N 为「球内有火种的目标挂上焚天。伤害在挂上时锁定，最多40刻，每段消耗10点火种」，O 为 `assets/xueximod/textures/effect/fentian_v2/conflagration.png`。DV 原 3806 字完整保留后再追加。
- 完整转换前后文本在 `docs/开发记录/术式批量制作/待制作-20261031/kindle-fentian-swap-requirements-20261116.json`，30751 字节，SHA256 `FFD2576D1CCBCF8C6F0677F20FD7AC1809227014D05F21DEB825748741ADCFE1`。

## 写表

- 表前：180915 字节，SHA256 `744A693B267A357D0CFF1D3F57598C01130B2C5559A9FAAA433C60DC2A115679`，mtime `2026-11-16 03:25:52.849`。无锁文件，无 Excel。提交前哈希未变，独占打开后立即关闭。
- 备份 `docs/开发记录/术式批量制作/待制作-20261031/备份/kindle-fentian-swap-术式制作表-20261116-105928-1.xlsx`，哈希等于表前。
- 候选 `docs/开发记录/术式批量制作/待制作-20261031/kindle-fentian-swap-candidate-20261116.xlsx`。提交后源表与候选都是 185187 字节，SHA256 `F9F953859345FB6918CDE476F4F7B8525A8C5284596C212625BEEE7D075ED23A`。源表 mtime `2026-11-16 10:59:29.103`。
- ZIP 仍是 27 个条目且顺序不变，只改两张状态表。制作中 `DV8` 追加；待验收第 3 行整行移到制作中第 28 行，并恢复 N、合并 O、追加 DV。`E` 仍是 `2|2`，`EK` 文本未改。数据验证各 20 条，表头合并各 8 条，均未改。未重算，未整本重建。
- 引燃新 DV 1870 字，SHA256 `008AE7AB1DDBDFF5EAFF8AF725B9D9942111AF077CCC9FF3A34A337C747EF238`。焚天新 DV 4329 字，SHA256 `573C7989F9A115A3253F837078F377096C014667B7D1F3520115EBCDF46CDD75`。
- 重开：已完成 37、待制作 0、制作中 23、待验收 18，合计 78。四页术式编号无重复。`fire_kindle` 只在制作中第 8 行，`fire_fentian` 只在制作中第 28 行。资源状态明细没有这两个编号。

## 未决事实

只记录，不修改，不裁决。`src/main/java/com/bslx/XueXiMod/shu/technique/release/firecast/FireConsumerPlans.java` 第 43–77 行：`skyDemand` 用整数向上取整实现 `ceil(S/R)`，`skyDamage` 为 `3 + consumed * 0.5 * (1.2^(S/10) + F)`。现行 `EK` 与 2026-10-29 批准快照仍是完整段请求 10 点、伤害 `2×(1.2^n+F)`。用户本次只授权美术互换，不据此统一或回滚公式。两套素材得到确认，不等于整项功能已经验证。

## 状态与收束

- 功能未改、未复审。美术互换已批准，正式归档和运行复制都等各自 final，当前未完成。
- 用户确认的是用途互换和接入授权，不是游戏实测通过。
- 编译未跑，游戏未启动，未迁待验收。
- 追加批次记录前，`记录.md` 为 100128 字节、464 行、SHA256 `0D68F3D326B473BC2993F80976D7B4A65A7F5A1A99C4942E9AE6DD57EFD3FEC9`。
- 下一步交回主会话。不等待美术或实现；实现与编译完成后再做最终登记。本回执停止写入。
