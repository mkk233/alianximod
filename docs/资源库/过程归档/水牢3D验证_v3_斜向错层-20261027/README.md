# 水牢 v3 · 斜向错层 3D 验证

状态：体积壳方向不采用。用户已明确采用纯二维、面向当前客户端镜头的单平面；改用 [公告板验证](../水牢公告板验证_v3_客户端镜头-20261027/README.md)。本目录仅保留过程记录，未接入游戏。

## 预览

- [固定斜视动画](water_prison_3d_fixed.gif)：720×720、32 帧、20fps、1.6 秒循环。
- [360° 转台](water_prison_3d_orbit.gif)：720×720、96 帧、20fps、4.8 秒，相机转一圈，水流循环三次。
- [宽矮目标对照](water_prison_3d_wide.gif)：720×720、32 帧、20fps、1.6 秒循环。
- 静态：[正面](water_prison_3d_front.png)、[侧面](water_prison_3d_side.png)、[背面](water_prison_3d_back.png)、[斜视](water_prison_3d_oblique.png)、[宽矮](water_prison_3d_wide.png)。

所有帧均由 Blockbench 的真实三维场景渲染；深蓝背景与灰色人物仅用于展示。水壳和流带本身保持不转动，动画只移动水流细纹。转台的变化来自相机移动。

## 三维结构

每个水牢为 528 顶点、416 面：闭合椭球壳 288 面（24 经向×12 纬向），三道贴壳束缚流共 128 面。正面沿用二维主长流、上方短回流、下方暗回流的排布；主流向侧后方延伸并收细，保留背面较疏的分布。

积水是球壳底部的曲面色层，没有水平地板。两种验证比例分别为半径 (13.5,20,12.5) 和 (20,14,16)，目标保持各自原比例。全体占位方块角点均在对应椭球内。

## 可编辑文件

- `水牢_斜向错层_v3_3D验证.bbmodel`：Blockbench 通用模型，包含人形与默认隐藏的宽矮目标组，五张贴图全部内嵌。
- `water_prison_3d_atlas.aseprite`：Aseprite 原生四层，256×128、16 帧×100ms。上方 256×96 是横向无缝球面水膜，下方 256×32 是水流展开纹理。
- `water_prison_3d_atlas.png`：256×2048 竖向动画图集。
- `draw-3d-atlas.lua`：实际通过 Aseprite MCP 执行的绘制脚本。
- `build-water-prison.js`：Blockbench 原生建模脚本。
- `preview-setup.js`：预览专用材质、透明排序和默认视角设置。
- `render-previews.js`、`export-previews.ps1`：原生逐帧截图与 GIF 编码。
- `validation.json`：网格、目标包围、贴图和动画帧检查结果。
- `frames-fixed/`、`frames-orbit/`、`frames-wide/`：最终原生渲染帧。
- `first-oblique.png`：厚壁增强前的过程检查图，不是最终效果。

二维 v3 原稿保持原样；三维展开贴图是用 Aseprite 另画的衍生文件，没有将整张圆形泡泡重复铺在球面。

## 复现预览

在 Blockbench 打开 bbmodel 后，通过 MCP 执行 `preview-setup.js`。脚本开启水纹播放、设置展示视角，并应用单层球壳的三档视角边缘明暗和 alpha，以及透明三角面从远到近的排序。水体开启深度测试、关闭深度写入，人物正常遮挡后方水流。

厚壁视角材质和排序是 **Blockbench 会话中的验证效果**，并非 bbmodel 单文件自带，也尚未实现于游戏渲染器。重新打开模型需要重新执行该脚本。宽矮对照可调用 `waterPrisonShow(1)`，常规人形为 `waterPrisonShow(0)`。

批量截图使用 MCP 建立的 720×720、禁用抗锯齿的 `water_prison_preview` 离屏视口。截图循环不使用后台计时器，避免后台窗口节流造成等待。最终导出已完整结束。

## 检查结果

两套球壳的每条边均恰有两张相邻面，闭合无开口；方块目标全部在壳内。固定和宽矮动画各有 16 个独特水纹帧，转台 96 帧均不同。已查看正面、斜视、侧后方及宽矮对照，未见明显透明穿帮。

本轮无游戏代码改动、编译、打包或游戏内测试。
