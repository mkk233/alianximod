# 水牢泡泡 v2 · 高压水壁（2D 草稿）

状态：待用户确认。保留 v1，本版尚未进入正式采用资源，也未接入游戏。

根据“没有牢笼的感觉、泡泡太轻盈”的反馈调整：
- 深蓝水壁加厚，底部增加弯月形积水，形成封闭和沉重感。
- 两条不同弧度的透明水流交错束住躯干，侧后方补短回流，脸部保持清楚。
- 减少白色高光，取消小珠滴；水壁轮廓固定，内部短纹沿束缚水流滑动，底部反向回流。

## 文件

- `water_prison_bubble_v2.aseprite`：96×96 RGBA、6 个可编辑图层、16 帧，每帧 100ms，1.6 秒循环。
- `water_prison_bubble_v2.png`：原尺寸透明首帧。
- `water_prison_bubble_v2_strip.png`：96×1536 竖向序列帧。
- `water_prison_bubble_v2_4x.png`：四倍像素预览。
- `water_prison_bubble_preview.aseprite`：深、浅两种背景与灰色目标的展示工程；背景、目标只用于展示。
- `water_prison_bubble_preview_4x.gif`：896×448 动画预览。
- `water_prison_bubble_preview_4x.png`、`preview_frame09.png`：动画检查截图。
- `draw-water-prison.lua`：通过 Aseprite MCP 执行的原生绘制与导出脚本。
- `validate-bubble.lua`、`validation.txt`：Aseprite 原生检查脚本及结果。

## 验证与范围

已在 Aseprite 内验证源文件尺寸、图层数、16 个不同动画帧、每帧时长、所有帧外轮廓一致、外部透明及 GIF 尺寸与帧数。已检查深、浅背景下首帧和中段帧的视觉效果。

本轮仅迭代 2D 素材。目标碰撞箱适配、三维承载、游戏接入均未执行；无代码改动，无编译、打包或游戏内验证。
