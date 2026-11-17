# 空间切割 · 左腰右拔 v5

当前使用 [握柄快拔 v6](../空间切割_握柄快拔_v6/README.md)。此目录保留原确认版；运行握点与时间问题已根据用户反馈在v6修复。

2026-11-02 用户确认并授权：“可以的，用这个版本作为空间切割术式的挥刀动作接入”。正式采用动作版本 iaido-single-v5-left-waist，已接入空间切割玩家挥刀动作。2026-11-02 05:55:18–05:55:43 最终离线 compileJava 实际执行通过；未打包，游戏视觉待用户实测。

左腰裸刀，刀尖朝后；右手跨身握柄、前带并连续横斩，左手空手让位。真实 Blockbench 原生模型为 free 格式，1.6 秒 once。运行适配已接入真实玩家皮肤、双节臂及实际持剑，衔接现有释放；示意假人、演示剑和图集不作为游戏人物加载。

- [正面动画](iaido-front-preview.gif) / [侧面动画](iaido-side-preview.gif) / [衔接慢放](iaido-transition-slow.gif)
- [可编辑 Blockbench 模型](iaido-single-v5-left-waist.bbmodel)
- [Aseprite 材质源](iaido-atlas.aseprite) / [64×64 PNG](iaido-atlas.png) / [二维预览](atlas-preview.png)
- [空间动作交接](visual-handoff.md) / [版本哈希](manifest.json)

GIF 来自真实 Blockbench 视口，81 帧、50fps 采样，带观看停顿总长2.38秒并循环；不把GIF循环或时长当成游戏施法规则。

原始过程、绘制/导出脚本保留在 ../../../过程归档/拔刀斩_v5-20261102/。现有空间切割斩击特效继续使用，新增本玩家挥刀动作。代码与编译由 Cursor Grok 4.7 High Fast 完成，默认不打包，游戏效果由用户实测。

## 运行接入与验证

运行资源是 `src/main/resources/assets/xueximod/actions/space_cut_left_waist_v5.json`，动作 `xueximod:space_cut`，1.6秒播放一次。默认或细手臂皮肤均用右手从左腰拔斩；主手设置在左也按这版右手拔刀展示。没有实际剑时不画演示刀。

原空间切割 `dimension_willow_v8` 特效、命中时机和20tick冷却保留；不新增动作硬直。同组一分为三登记一次人物动作、各份保留独立斩面。

- [运行映射说明](runtime-mapping.md) / [结构化映射](runtime-mapping.json)
- [最终编译证据](../../../../开发记录/空间切割拔刀动作接入-20261102/compile-evidence.json)：退出码0，compileJava实际执行，1200个源码/资源文件前后哈希差异0。
- [接入交付记录](../../../../开发记录/空间切割拔刀动作接入-20261102/交付.md) / [用户实测清单](../../../../开发记录/空间切割拔刀动作接入-20261102/用户实测清单.md)

自定义剑模型若覆盖默认handheld的手持旋转/平移，可能需要另做挂接适配；仅修改缩放会保留新尺寸。特殊护甲模型的双节臂不保证覆盖，第一人称按原版不显示护甲。当前未做游戏视觉验收。
