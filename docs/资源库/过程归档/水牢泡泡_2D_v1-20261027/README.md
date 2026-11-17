# 水牢泡泡 · 2D 草稿 v1

状态：待用户确认，尚未正式采用；本轮仅 2D 美术验证。

- 原生来源：Aseprite 1.3.17，MCP CLI 执行像素绘制脚本。
- 原尺寸：96×96 RGBA；16 帧，每帧 100 ms，1.6 秒循环。
- 图层：水膜、闭合薄壳、折射水纹、高光、贴膜水珠。
- 轮廓固定，内部水纹、高光与水珠局部缓慢流动；中央 alpha 约 18/255。
- water_prison_bubble_v1.aseprite：五层可编辑源文件。
- water_prison_bubble_v1.png：首帧透明原图。
- water_prison_bubble_v1_strip.png：16 帧竖向 RGBA 序列，保留半透明。
- water_prison_bubble_preview_4x.gif：深浅背景与灰色方块人展示，仅用于看水膜和动画。背景与人形不属于特效素材；GIF 已将半透明合成到展示背景。
- water_prison_bubble_preview.aseprite：独立展示文档。
- draw-water-prison.lua / validate-bubble.lua / validation.txt：绘制与检查记录。

后续流程：用户确认 2D → Blockbench 验证三维包覆和碰撞盒尺寸适配 → 用户认可后归入正式资源库及安排游戏接入。
