# 燃烧 · 双手附焰与火焰披风 v3（确认版）

2026-10-31用户反馈原文：“升温效果很好，采用，燃烧效果不好我希望燃烧效果是给手上附加火焰看上去像是身体着火了，并多出一个火焰披风”。用户随后明确确认：“这个版本可以，采用”。本目录为该版正式确认资源；升温v2已单独正式归档。

## 视觉

- 双手：以两只手为火焰根部，黄白亮芯、橙黄内焰与橙红外焰，火舌向上分叉并脱落火星。
- 身体：肩侧、胸腹、腰腿上附着火舌，表现角色本身在燃烧。
- 披风：从肩背向下延伸，橙红火流沿披风流动，左右摆动，底部有不规则缺口和火舌；与手火、身体火焰独立分层。
- 预览左侧正面（手/身体），右侧背面（披风）。都是二维Aseprite效果稿，不是游戏实拍或三维验证。

## 可编辑资源

- overdrive-v3.aseprite：96×112，20帧×80ms，五层分别为披风、后方身体火焰、贴身火焰、双手火焰、火星。
- overdrive-v3-{cape,rear-fire,body-fire,hand-fire,embers}.aseprite：各组件独立可编辑源。
- 同名*-strip.png：各组件透明原尺寸纵向图集；combined-strip为整图，96×2240。
- overdrive-v3-entry.aseprite / entry-strip.png：8帧×60ms一次入场。披风从肩背展开、手部同时点燃；从普通升级只播本入场，不重播升温。
- overdrive-v3-front-back-preview.aseprite / .gif / .png：正背双视图，48帧、3.68秒演示，含一次入场和两次维持循环，4倍最近邻预览。
- manifest.json：17个源文件、原尺寸导出和预览的大小与SHA256；全部与获批过程稿逐字节一致。
- 绘制脚本仍在[过程目录](../../../过程归档/燃烧_双手附焰与火焰披风_v3-20261031/draw-fire-cape-v3.lua)，未使用ImageGen。

状态：2026-10-31 已接入源码，待编译/游戏验收。批准原件未改。正背预览和人物不进游戏。制作表仍在制作中，未迁待验收。未编译、未游戏实测。

## 游戏引用

运行贴图在 `src/main/resources/assets/xueximod/textures/effect/furnace_v2/`。

- `burn_cape.png`、`burn_rear_fire.png`、`burn_body_fire.png`、`burn_hand_fire.png`、`burn_embers.png`：与本目录同名 `*-strip.png` 逐字节相同，维持 20 帧 × 80ms。
- `burn_entry_cape.png`、`burn_entry_rear_fire.png`、`burn_entry_body_fire.png`、`burn_entry_hand_fire.png`、`burn_entry_embers.png`：从 `overdrive-v3-entry.aseprite` 分层无损导出，8 帧 × 60ms。
- 直接进入过载或从普通升级都只播燃烧入场，不重播升温。合成条、正背预览和过程归档里的空 `overdrive-v3-combined.aseprite` 不加载。
- `FurnaceBodyLayer`：手火跟随左右臂，肩胸与腿部火焰挂在躯干和腿，披风是贴在背后的竖面，余烬挂在身体前后。`FurnaceArmFlame` 只在第一人称手上画局部火。
- 冷却蒸汽仍用 `textures/effect/furnace_v1/cool.png`。旧 `furnace_v1/overdrive.png` 不再被燃烧路径引用，文件保留。
