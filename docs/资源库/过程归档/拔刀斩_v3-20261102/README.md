# 拔刀斩 v3：无刀鞘，腰间佩刀

用户认为 v2 动作很好，要求 MC 没有刀鞘，因此刀直接别在腰上。本版在真实 Blockbench 制作，保留 v2 文件。状态：实际预览待用户评价，未正式归档采用、未接入游戏。

- [正面动画](iaido-front-preview.gif) / [侧面动画](iaido-side-preview.gif) / [衔接段慢放](iaido-transition-slow.gif)
- [可编辑模型](iaido-single-v3-waist.bbmodel) / [腰间起手截图](waist-start.png)
- [Aseprite 源](iaido-atlas.aseprite) / [贴图](iaido-atlas.png) / [像素预览](atlas-preview.png)
- [空间交接](visual-handoff.md) / [文件哈希](manifest.json)

移除五个刀鞘方块及 Scabbard 骨骼，刀身直接可见。右手握柄位置降至腰带外侧；起手从腰侧提起，随后使用 v2 已认可的连续抽斩轨迹。左手取消退鞘动作，改为身侧让位和回落。

单次动作仍为 1.6 秒：0–0.08 秒腰间握柄，0.08–0.20 秒提刀，0.20–0.48 秒带刀，0.48–0.60 秒连续快斩，0.60–0.85 秒停势，0.85–1.60 秒落刀。0.20 秒之后右手、刀轴和身体时序沿用 v2。

模型动画 once，161 个时间采样点、间隔 0.01 秒。真实视口 GIF 共 81 帧、0.02 秒采样；首尾观看停顿为 0.20/0.60 秒，总 2.38 秒并循环。慢放取 0.32–0.68 秒片段、四分之一速度。

沿用 v2 的 Aseprite 64×64 静态图集；只改模型和动作，不改贴图。free 格式假人用于示意，未映射 Minecraft 玩家骨架或第一人称。未改 Java、未编译、未打包、未运行游戏。
