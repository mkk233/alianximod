# 幽冥·魂谷摇曳 v2（已采用，运行已接入，编译未通过）

2026-11-16 用户原话：「按这版接入」。确认对象是过程目录 [幽冥_魂火摇曳_v2修订2_batch_20261116](../../../过程归档/幽冥_魂火摇曳_v2修订2_batch_20261116/) 的 `close-sway-3p2s.gif` 与 `inside-sway-3p2s.gif`，并绑定同版 Aseprite、原尺寸 PNG、bbmodel 和交接。这是正式采用和接入授权，不再标为未确认。

接入范围包括固定根部的局部火舌摇曳，以及此前已展示的领域内魂谷天空和远雾氛围。游戏效果尚未实测。

## 本目录

- 可编辑源 `youming_v2_sway_fixed_root.aseprite`，SHA256 `9D7D6867771F216A3CA7552844FB66D15D764DF9A3FAE4737114FF39C5000032`。64×64、16 帧、5 层、100ms。底部 20 行根部固定，上部局部火舌错相摇曳。
- 原尺寸条 `textures/youming_v2_sway_strip.png`，SHA256 `7699297D207AC3CCACEC25E9CF7226C5B3765C0E16F620F3E1D717C1E23AD0FB`。64×1024。
- 二维预览 `sway-2d-native.gif`、`sway-2d-4x.gif`。
- 真实 Blockbench 工程 `youming-v2-fixed-root-sway-candidate.bbmodel`，SHA256 `350EF6D81BC057E9C577B09F04F42225009DF19EA2B59CE0C4AD4B52DD7309C9`。按字节复制，没有改路径或重画。
- 关键视口 `01-inside-sway.png`、`02-close-fixed-root.png`、`03-side-person-height.png`。
- 确认动画 `close-sway-3p2s.gif`（`A8C514EAE8E8E862FC154C472CE2E2246FF518695C75100F72C5F06488631CB8`）、`inside-sway-3p2s.gif`（`B92B910CE7DCE0116EA75F670F44446812126273EC05E37DFF7E111769E22308`）。
- 交接 `visual-handoff.md` 按字节复制。过程 manifest 的逐字节副本是 `来源-manifest.json`，SHA256 `4841D6409142A91D625B934C241AA4C1202023EFE6EA24E0DAFB419B1601168B`。本目录 `manifest.json` 只是正式路径索引，不替代该哈希。

模型内的相对贴图路径仍指向原过程目录的上一层：`../幽冥_v2领域定稿预览_20261116/textures/nether_gloom_v2_runtime.png` 和 `../幽冥_魂谷天空危险魂火_v2修订_20261116/textures/youming_v2_soulvalley_strip.png`。原过程目录没有移动，从原目录打开时这些回链仍有效。从本正式目录打开时，相对路径不会指向这里；本目录另存同字节原尺寸条。交接里指向 `evidence/`、`frames/` 的相对链接也以原过程目录为准。


## 运行与未决

2026-11-16 接入已收束。运行贴图 `assets/xueximod/textures/effect/nether_gloom_v2/nether_gloom.png` 为 5310 字节，SHA256 `7699297D207AC3CCACEC25E9CF7226C5B3765C0E16F620F3E1D717C1E23AD0FB`，与本目录原尺寸条逐字节相同。`nether_gloom.png.mcmeta` 为 `blur=false`、`clamp=true`。运行地火取消整簇宽高脉冲，固定根部，上部火舌错相摇曳；真实切片内的天空和远雾按 0.6 秒进入、0.8 秒退出混合。水、熔岩和粉雪不混色。地火到视觉结束仍立刻停画，没有火焰淡出；雾的近远距离仍是原版，没有新的雾密度。

`compileJava --offline --no-daemon` 于 2026-11-16 21:15:12.347–21:15:38.292 执行一次，退出码 1。两个错误都在无关的 `RoseVineProjectile.java:73` 与 `:109`，本项源码未改。这次不能当成编译通过，也不能当成成功覆盖最终源码。未启动游戏，未游戏实测。

产种周期仍未决：`approved-v1.md` 第 17、71 行，`gloom-standalone-repair-report.md` 第 87 行，`gloom-standalone-repair-final.md` 第 89 行批准仍是每 20 tick 基础 5。先前只读事实记录现行代码和两份存档配置为 5，批次 Markdown 没有更晚把 20 改成 5 的批准。本次视觉接入不解决这个差异，也不改玩法。

旧过程候选和 [幽冥_v1](../幽冥_v1/README.md) 保留为历史。本阶段额度已用完，返修结果未追加复审。
