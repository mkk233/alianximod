# 焚天 v4：已授权双层原件复用

素材组合授权状态：`approved_for_recipient_integration`。
本次制作进度：`completed_art_preview`；已完成Aseprite双源复开、二维组合演示和同目标双层真实Blockbench四视图、原生保存复开。不是游戏实测。
batchId=`待制作-20261031`；taskId=`fire_kindle_v2_art`；attemptId=`20261116-art-fentian-dual-layer-05`。

背景 `background_fentian_v2-strip.png` / `background_fentian_v2.aseprite` 为旧焚天 v2 逐字节复制件。PNG 128×7488，52 帧，每帧128×144；源帧50ms。PNG SHA256 `3AE6A95E41D92991B828EDC7491CBB40A18D503CDCF700281A50D901D6AA6A76`；aseprite SHA256 `8A9E64C672466E5C1F764860307EF6E24E66A5545A71D725172DDD1BF52F6A11`。

前景 `foreground_fentian_v3-strip.png` / `foreground_fentian_v3.aseprite` 为全身烈焰 v3 逐字节复制件。PNG 48×1216，19 帧，每帧48×64；源帧50ms。PNG SHA256 `EF3476EB01225D2E7478F9BBA5430F28BCF2E1D12198F3F482A634C155A04070`；aseprite SHA256 `3EC04641BBBC573BAAABCF088DA61B6B79B7418159C2D1F6F3287D5B8B06BB42`。

不重绘、改色、改比例或合成新运行图集。运行仍引用 `textures/effect/fentian_v2/conflagration.png` 与 `textures/effect/fentian_v3/conflagration.png`，背景先、前景后。引燃 kindle_v4 保留，暂缓重新设计。

用户原话、固定参数、真实GUI证据及完整清单见 `D:/alianximod/docs/资源库/过程归档/焚天_双层叠加_v4_20261116/`。运行接入与编译由独立执行者负责，本美术归档不代替游戏实测或迁页回执。

## 2026-11-16 v4.1 后续补充

本节只补自然结束，不改写上方 v4 历史。用户原话：“这个版本很好，但是做背景的老特效没有自然熄灭动画而是直接消失和全身的燃烧效果不同步，把这个小瑕疵修一下”。新预览状态仍是 `candidate_pending_user_confirmation`，不表示用户已再次确认观感。

v4.1 没有新运行贴图。自然结束捕获最后显示的持续帧和当时 scale；同一矩形先乘持续 alpha，再乘余烬 alpha，然后画前景。tick 0–8 的持续 alpha 为 1、0.94、0.82、0.66、0.48、0.30、0.15、0.05、0，余烬 alpha 为 0、0.18、0.36、0.55、0.72、0.82、0.65、0.35、0。余烬帧为 40、41、42、44、45、46、48、50、51，前景帧为 10–18。tick 8 全透明，tick 9 一起删除。满火、几何、朝向、染色、引燃和玩法未改。

过程目录 `D:/alianximod/docs/资源库/过程归档/焚天_双层收束同步_v4_1_20261116/`。演示 `previews/burnout_9tick.gif`，SHA256 `812F7BC1283C90FD1471810453B26BE0666BD5B41E1A770D7A201BF28B97D878`。参数见同目录 `visual-handoff.md` 与 `timeline.json`。

实现回执 `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/fentian-burnout-sync-impl-final-20261116.md`。`compileJava --offline --no-daemon` 于 2026-11-16 13:01:12.049–13:01:35.663 退出码 0，树摘要 `542DAAD3AFB1B3E8197DFADA8AA0E91C48729F39C555CAB83AC363E959207F09`。尚未游戏实测，不迁待验收。本阶段额度已用完，返修结果未追加复审。
