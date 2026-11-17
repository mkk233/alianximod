# 拔刀斩 v5：左腰佩刀，右手拔斩

最新要求：刀在左手侧，用右手拔出完成挥砍。承接无刀鞘、刀尖朝背后和连续单次挥斩要求。在真实 Blockbench 制作，保留 v4。当前是实际预览，未正式采用或接入游戏。

- [正面斜视动画](iaido-front-preview.gif) / [侧面动画](iaido-side-preview.gif)
- [起手姿态](left-waist-start.png) / [侧面起手](side-waist-start.png) / [衔接慢放](iaido-transition-slow.gif)
- [可编辑 Blockbench 模型](iaido-single-v5-left-waist.bbmodel)
- [Aseprite 材质源](iaido-atlas.aseprite) / [64×64 PNG](iaido-atlas.png) / [二维预览](atlas-preview.png)
- [空间动作交接](visual-handoff.md) / [同版哈希](manifest.json)

刀在角色左侧腰部、刀尖朝后。右肩前送、右手跨过身前握柄，再前带并连续横斩。左手为空手让位。刀始终是同一把并绑定右前臂下的 SwordHand，没有左右手交换或镜像整个角色。

手部轨迹以 ActorRoot 为参考，再反算进转动的躯干，保证左腰位置不会随转肩变成右腰。肩和肘用原有 6+6 单位骨段，最大伸展 11.480419 < 12，不拉长手臂。native-mount.json 记录实际 Blockbench 起手骨骼位置和刀轴。

保持单次 1.6 秒：0–0.20 蓄势，0.20–0.48 前带，0.48–0.60 快斩，0.60–0.85 停势，0.85–1.60 落刀。GIF 是真实视口 50fps 采样，共 81 帧，加首尾观看停顿共 2.38 秒并循环；模型动画本身 once。

沿用 Aseprite 静态材质。free 格式占位人物不是已经映射到 Minecraft 的玩家骨架；未修改 Java、未编译、未打包、未启动游戏。
