# 燎原 · 用户确认v2

2026-10-29用户在查看地表火海v2与确认其他五项时回复“这五项都确认，继续接入”。确认版归档，未游戏实测。

Cursor作者报告已接入`assets/xueximod/textures/effect/liaoyuan_v2/ground_flame.png`、`ground_embers.png`、`spread_wave.png`，与本目录相应透明导出hash一致，并已补做碰撞顶面与分格火浪贴合。2026-10-29最终`compileJava --offline --no-daemon`和本批8项定向检查通过；未追加正式实现复审，未游戏实测。当前没有现成火种生成/给予入口，火种消费分支的实测前置尚缺；详情见批次记录及compile-final-report.md。

- ground-flame：liaoyuan-ground-flame-v2.aseprite及-strip.png，32×40、24帧、50ms/帧，贴地翻卷的低矮火焰。
- ground-embers：liaoyuan-ground-embers-v2.aseprite及-strip.png，64×64、24帧、50ms/帧，透明炭火纹理，不替换方块。
- spread-wave：liaoyuan-spread-wave-v2.aseprite及-strip.png，128×128、12帧、50ms/帧，从释放原点向外推进的火浪。
- liaoyuan-ground-sea-preview-v2.*是带人物/地面参照的二维预览，不得作为整图接入游戏。仅演示前几秒，不能将预览长度当作火场寿命。
- 游戏必须按真实可见地表铺展，半径与200tick寿命、上下3格/火层1格、伤害规则仍取完整N；表面纹理仅绘制，结束无真实方块变化、不放原版火。
- 被消费火种目标的附着火焰沿其既定剩余燃烧独立播放，强弱随消费快照，离开地火仍可见；不让火场消失提前终止目标已有燃烧。
- 可复用本目录火焰纹理对目标作低/高档表现，不复用焚天满火种十拍伤害节奏。
- 批次记录：../../../../开发记录/术式批量制作/火系六式-20261029/记录.md。
