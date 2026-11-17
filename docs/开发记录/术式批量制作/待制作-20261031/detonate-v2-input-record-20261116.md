# 引爆候选输入记录

- 批次：`待制作-20261031`
- 任务：`fire_detonate_v2_progress`
- 尝试：`20261116-doc-detonate-input-01`
- 日期：`2026-11-16`
- 记录者：`01a1e08b-09e0-7bb3-b2ce-d5e430f241ac`
- 直接父级：主会话 `01a1ce06-b681-7c90-b11d-260d3c169911`
- 额度：设计 `2/2`、实现 `3/3`。本阶段额度已用完，返修结果未追加复审。

## 用户原话

下一个术式

这只授权继续下一项。主会话对短促火团的视觉归纳不是用户原话。

## 只读表

- 源表未写。无锁，187795 字节，SHA256 `1473D5EF076D4B8B19A59CBCC92DF3A394CDD888FB03E4B4025AC250D1F264B1`，mtime `2026-11-16 14:52:48.764`。
- 四页为已完成 37、待制作 0、制作中 22、待验收 19，合计 78，编号唯一。
- `fire_detonate` 在制作中第 9 行。资源状态明细没有该编号。
- N 337 字，O 38 字，DV 1073 字。R、S 为空。
- 单元格 SHA256：N `461AE1A12693676FF94273A432ADD5BA9307F0C93C483E7F9E2F672008A7D329`，O `34AF39A9173DD98D1DB200DB61E47E2337863414A640ACBAAA55B9146937C66F`，DV `953FC8E687BD6EF4B27C5EB0AAA226E3878A755C1C5EA71832E039FFD3E14A20`。
- 完整单元格在 `detonate-v2-input-snapshot-20261116.json`。
- O 原文：“命中点一次短促火团，强度随当时花掉的火种增加。没有余额时只有载体自己的命中。”
- N 保持消费当时共享余额，火伤为 `0.2×S×(1+F)`，只结算本次命中目标；余额为 0 时没有额外资源火伤。

## 现行 v1

运行 `textures/effect/detonate_v1/detonate.png` 与 `docs/资源库/特效/火焰/引爆_v1/detonate-strip.png` 逐字节相同。11013 字节，SHA256 `F3F0FCEB4A8AAECBA5EAC2B300C1FD5036B0E9EEAF1CD04B1A4F38FC1C79D858`，尺寸 64×1024。

索引和 v1 说明已把“尚未复制”更正为暂用已接入。正式美术未完成，不是游戏实测通过。其他资源条目未改。

## 后续接入待办

以下内容由父方转记，本轮没有重读 Java，也不构成复审。`BatchTriggerVisuals.java` 第 26、40 行只检查载入 detonate 后播放，未读取 `DetonatePlan.Outcome` 的 spent、STRUCK、EMPTY、DUPLICATE。现行是固定 16 tick、halfSpan 0.6、命中点上移 1.0、`CAMERA_PLANE` 和 `FULL_BRIGHT`。它还没有按花掉的火种改变强度，也没有让零消耗不显示。候选确认后才由 Grok 定向接入。现在不能写成已完成。

## 美术调度

- `antigravity/gemini-4-carbon` attempt `20261116-art-detonate-01` 返回 Unknown model，没有代理。
- `antigravity/gemini-4-argon` attempt `20261116-art-detonate-02` 同样返回 Unknown model，没有代理。
- 按流程第 11.1 节降到 `gpt-6-astra` max。Ohm `01a1e336-50ed-78f0-8d74-7657a5a4e7b3`，taskId `fire_detonate_v2_art`，attemptId `20261116-art-detonate-03`。
- 独占目录是 `docs/资源库/过程归档/引爆_短促火团_v2_20261116/`，回执文件是 `detonate-v2-art-preview-20261116.md`。本记录未读取其中的实时产物。
- 父方已核对 Aseprite 1.3.17 可批处理且 live=false，Blockbench 为 desktop 5.2.2、plugin 1.10.1。用户当前焚天工程保持保护。本轮没有再调用美术 MCP。
- 候选尚未获用户确认，未接入，未编译。

## 状态

引爆仍在制作中。功能需求未改，v1 只是暂用，v2 仍在制作。未游戏实测，不称正式复审通过。

本记录者未写源码、贴图和制作表。写入已收束。无下属。
