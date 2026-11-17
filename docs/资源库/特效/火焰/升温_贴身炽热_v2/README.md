# 升温 · 贴身炽热 v2（确认版）

用户于2026-10-31明确确认：“升温效果很好，采用”。本目录保留该版原始像素与动画，不因燃烧的返修改变升温。

- 术式：fire_heat_up / 升温。
- 视觉：贴身暖橙亮边，手臂与躯干两侧上移热流，低密度上升余烬。
- 循环：96×112，20帧×80ms；入场：8帧×60ms，只播一次。
- heat_up-v2.aseprite、heat_up-v2-entry.aseprite：可编辑分层源。
- heat_up-v2-strip.png：完整透明纵条；back/front分别为前后两组导出；entry-strip为一次入场图集。
- heat_up-v2-preview.aseprite / .gif / .png：仅升温的独立预览，人物不在效果源里。
- manifest.json记录六个源/导出文件的SHA256；均与获批过程稿逐字节一致。

状态：2026-10-31 已接入源码，待编译/游戏验收。批准原件未改。预览不进游戏。制作表仍在制作中，未迁待验收。未编译、未游戏实测。

## 游戏引用

运行贴图在 `src/main/resources/assets/xueximod/textures/effect/furnace_v2/`。

- `heat_rear.png`、`heat_rear_tongues.png`、`heat_front_tongues.png`、`heat_embers.png`：维持 20 帧 × 80ms，从 `heat_up-v2.aseprite` 分层无损导出。
- `heat_entry_rear.png`、`heat_entry_rear_tongues.png`、`heat_entry_front_tongues.png`、`heat_entry_embers.png`：入场 8 帧 × 60ms，从 `heat_up-v2-entry.aseprite` 分层无损导出。
- 成功进入普通炉心时播一次入场，然后循环。预览、整图 `heat_up-v2-strip.png` 和人偶不加载。
- `FurnaceBodyLayer` 把前臂热流挂在左右臂，躯干侧热流和余烬挂在身体，后方热流挂在背后。`FurnaceArmFlame` 只在第一人称手臂上画局部热流。
- 冷却蒸汽仍用 `textures/effect/furnace_v1/cool.png`。旧 `furnace_v1/heat_up.png` 不再被升温路径引用，文件保留。

原制作脚本与组合预览留在过程归档/炉心升温燃烧_重设计_v2-20261031；独立预览制作脚本在过程归档/燃烧_双手附焰与火焰披风_v3-20261031/draw-fire-cape-v3.lua。
