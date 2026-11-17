# 引燃 v4：承接用户指定的原焚天 v2

状态：`approved_for_recipient_integration`。接收术式：`fire_kindle`。
batchId=`待制作-20261031`；taskId=`fire_kindle_v2_art`；attemptId=`20261116-art-kindle-fentian-swap-04`。

用户已明确指定“把现在焚天的特效先拿来给引燃用”。完整逐字回复与双向批准映射：
`D:/alianximod/docs/资源库/过程归档/引燃焚天_确认版互换_20261116/approval-mapping.md`。
不再等待同一素材批准；这不是新候选或新绘制。

- `kindle_v4-strip.png`：从旧焚天 `fentian-conflagration-v2-strip.png` 按字节复制，128×7488，单帧 128×144，共 52 帧；SHA256 `3AE6A95E41D92991B828EDC7491CBB40A18D503CDCF700281A50D901D6AA6A76`。
- `kindle_v4.aseprite`：同版旧焚天源按字节复制；SHA256 `8A9E64C672466E5C1F764860307EF6E24E66A5545A71D725172DDD1BF52F6A11`。源标签仍是 gather F01–03、ten_pulses F04–40、embers F41–52，每帧 50ms，保持历史内容不改名重存。
- 数组 0 起算：主视觉索引 0–39，尾焰 40–51；按 4 帧视觉块看，块 k 的源索引为 `4k..4k+3`，k=0..9。这不是引燃 4 tick 伤害时钟。
- 目标附着：高 `max(1.8,H)*1.35*scale`，宽 `height*128/144`，中心 `entityY+.45H`，CAMERA_PLANE。参照 H=1.8、scale=1：高 2.43、宽 2.16、中心高 .81 格。不额外套用焚天新稿的支撑面偏移。
- 播放必须由接收者引燃真实 10 tick 段驱动，不照搬原焚天 4 tick 节奏，不把原 52×50ms 的源时长当燃烧持续时间；不改变伤害。逐段取样/段间保持和终止尾焰接线以 Grok 接入回执为准。

拟运行资源：`D:/alianximod/src/main/resources/assets/xueximod/textures/effect/kindle_v4/kindle.png`。本作者没有写运行资源或 Java。
同版真实空间预览、准确坐标、遮挡限度、模型、逐文件哈希：
`D:/alianximod/docs/资源库/过程归档/引燃焚天_确认版互换_20261116/visual-handoff.md` 和同目录 `manifest.json`。
正面与斜视图左侧为本引燃；冻结侧距图右后方为本引燃。

资源用途确认不等于接入完成、编译通过或游戏实测。旧焚天 v2 文件未改；不覆盖旧引燃稿。本阶段额度已用完，返修结果未追加复审。

## 2026-11-16 后续：不再作为引燃现行显示

用户随后指定引燃改用原版燃烧动画，不再单独制作特效。本目录和其中像素保留为历史，未被删除或改写。现行运行显示是原版 `minecraft:block/fire_0` 与 `minecraft:block/fire_1`；幽冥内沿用原版魂火精灵。本目录不再是现行引燃贴图。详见 `docs/开发记录/术式批量制作/待制作-20261031/kindle-vanilla-final-sync-20261116.md`。本阶段额度已用完，返修结果未追加复审。
