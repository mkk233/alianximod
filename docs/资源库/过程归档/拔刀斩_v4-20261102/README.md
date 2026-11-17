# 拔刀斩 v4：身侧佩刀，刀尖朝后

用户纠正 v3：刀不应横在身前，应位于身侧、刀尖朝玩家背后。本版已在真实 Blockbench 按该要求修改，保留旧版。待用户评价，未接入游戏。

- [侧面动画](iaido-side-preview.gif) / [正面斜视动画](iaido-front-preview.gif)
- [侧面起手](side-waist-start.png) / [正面起手](front-waist-start.png)
- [Blockbench 模型](iaido-single-v4-side.bbmodel) / [衔接慢放](iaido-transition-slow.gif)
- [Aseprite 源](iaido-atlas.aseprite) / [原尺寸贴图](iaido-atlas.png) / [二维预览](atlas-preview.png)
- [动作空间交接](visual-handoff.md) / [文件哈希](manifest.json)

刀在人物右侧腰部，刀柄朝前，刀尖沿人物后方 +Z；没有刀鞘。右手向前带刀，随后从身侧外方连成单次横斩，左手自然让位。

单次动作保持 1.6 秒：0–0.20 秒腰侧握柄蓄势，0.20–0.48 秒向前带刀，0.48–0.60 秒连续横斩，0.60–0.85 秒停势，0.85–1.60 秒落刀。抽刀和横斩用连续速度曲线衔接，没有在两段之间停住。起始方向已改变，因此本版不是原横在腰前的运动轨迹。

模型一次播放。GIF 为真实 Blockbench 视口 50fps 采样，共 81 帧，加首尾观看停顿为 2.38 秒并循环。保留同版 Aseprite 材质、bbmodel、完整视口帧与导出脚本。未改 Java、未编译、未打包、未启动游戏。
