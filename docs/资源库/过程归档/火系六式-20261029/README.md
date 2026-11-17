# 火系六式二维草稿

状态：2026-10-29 六式已全部确认。正式采用燎里v6、燎原v2、敌人身上的焚天v2、炉心三式v1，均已在../../特效/火焰/对应目录正式归档。本目录保留全部历史草稿、参考与绘制脚本。

- 制作：Aseprite MCP，脚本draw-drafts.lua；每式64×64、16帧、75ms/帧。
- 顺序：liaoyuan燎原、liaoli燎里、fentian焚天、heat_up升温、overdrive燃烧、cool冷却。
- 每式保留可编辑-v1.aseprite和透明竖向-v1-strip.png。源图不含预览里的灰色人物、剑、地面。
- 六格预览：six-effects-review-v1.gif；静帧six-effects-review-v1.png；可编辑预览six-effects-review-v1.aseprite。放大采用最近邻。
- 预览仅展示二维质感，循环及75ms帧速不定义游戏时间；游戏持续、间隔、缩放依制作表N/DV和最新批准版。
- 没有任何已确认资源映射。代码开发占位不能冒充本稿已获用户批准。

## 燎里v2

- 可编辑源与透明竖条：liaoli-afterburners-aura-v2（128×96、16帧）、liaoli-afterburners-hit-v2（96×96、10帧）。维持与命中独立，游戏播放分别绑定持续和真实命中事件。
- 预览liaoli-afterburners-preview-v2.gif：2秒持剑火焰，演示期间一次命中，然后收招。灰色剑仅参照，不属于导出特效。
- 参考：Sky Striker Maneuver - Afterburners!，TCGplayer卡图https://tcgplayer-cdn.tcgplayer.com/product/167192_in_1000x1000.jpg；仅借鉴赤红分叉焰片、亮黄芯和放射尖芒，参考图不用于游戏贴图。
- 绘制脚本draw-liaoli-v2.lua。v1完整保留，未覆盖。

## 燎里v3（当前）

- 用户参考图user-sword-ring-reference.png；用户原话与语义见批次记录。
- liaoli-orbit-sword-v3：64×20，单帧剑型，Aseprite源与透明PNG。
- liaoli-orbit-stream-v3：96×32，24帧，沿局部+x方向流动；Aseprite源与透明竖条。
- liaoli-orbit-preview-v3.gif：八柄剑在人物背后及两侧扇形展开，剑尖各自向外喷出火焰流光。灰色人物仅参照。游戏需要姿态/方向接点，不以平面大图贴在玩家背后代替空间剑阵。
- 仅视觉提案，不额外增加远程伤害或独立命中体。待用户明确确认。

## 燎里v4（当前，待修改）

- 用户要求从玩家身后起火，逐渐覆盖剑柄，另做手持刺击剑左右分流；追加高清参考user-afterburners-hd-reference.png。
- liaoli-back-ignite-v4：128×48、12帧，从背后向外渐显；liaoli-back-plume-v4：128×48、24帧宽幅红焰和金色流线；liaoli-golden-sword-v4：64×20金色剑型。
- liaoli-thrust-split-v4：128×64、24帧，两条火流在剑左右分开。
- 预览liaoli-back-origin-preview-v4.gif、liaoli-thrust-split-preview-v4.gif。源文件与透明竖条均保留。
- 用户看过两套预览后要求继续修改，等待具体意见。没有任何接入批准。

## 燎里v5（当前）

- 修正v4的八剑左右斜角不对称；以玩家中线成对镜像，镜像角度与间距相同。
- 手持刺击剑两流朝玩家身后。透明分流图仍使用局部+x流动，游戏必须将该局部轴沿剑尖向玩家身后的方向放置，不能再次当成向前喷流。
- 当前预览liaoli-back-origin-preview-v5.gif、liaoli-thrust-split-preview-v5.gif，源与透明导出使用对应-v5名称。
- 未获接入确认。

## 燎里v6（已确认）

- 新增liaoli-rear-ring-v6源与96×96、24帧透明序列；三道圆环同时存在，半径增加并向玩家后方移动，错相接续。
- 玩家局部坐标中剑柄比剑尖更靠后，保留v5的镜像角度及手持剑向后分流。
- 正面liaoli-rings-front-preview-v6、斜侧面liaoli-rings-oblique-preview-v6，均有GIF/PNG/Aseprite。
- 用户已确认v6；正式文件及方向契约位于../../特效/火焰/燎里_v6/README.md。该归档包含v6实际使用的v5/v3组件副本，不将未确认历史版本混作确认稿。

## 焚天v2（已确认）

- 用户要求增强“直接烧毁一切的气势”，已重绘大幅赤红火浪与金白火柱，每4tick一次明显脉冲。
- fentian-conflagration-v2.aseprite及-strip.png：128×144、52帧、50ms/帧；聚火1–3，十次爆燃4–40，余烬41–52。
- fentian-conflagration-preview-v2.gif/png/aseprite为满火种示意。用户已确认仅作用被点燃敌人，实际程序需根据各目标投入和段数裁剪/取样，不能令低火种也播放十次伤害脉冲。

## 燎原v2（已确认）

- 地表火海新版：liaoyuan-ground-flame-v2（32×40、24帧）、liaoyuan-ground-embers-v2（64×64、24帧）、liaoyuan-spread-wave-v2（128×128、12帧），源与透明竖条。
- 预览liaoyuan-ground-sea-preview-v2，原点向外铺展，随后低矮火海和临时炭纹，真实寿命仍依制作表200tick。
- 用户在确认该预览时一并明确确认焚天v2与炉心三式v1。

批次记录：../../../开发记录/术式批量制作/火系六式-20261029/记录.md
