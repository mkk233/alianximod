# 水牢 v3 · 纯 2D 公告板验证

状态：此为比例有误的公告板初稿，已由 [等比验证 v4](../水牢公告板验证_v4_等比缩放-20261027/README.md) 修正。不能以本稿的宽高分别缩放方式接入游戏。

## 确认的设计规则

- 使用已认可的 [Aseprite 二维 v3 原动画](../水牢泡泡_2D_v3_斜向错层-20261027/README.md)，不改为体积球壳，不将浪纹变成三维流带。
- 每个水牢只提交一张四顶点面片（渲染为两个三角形），96×96、16 帧×100ms。
- 面片每次渲染复制当前客户端渲染相机的朝向；多人各客户端独立计算，不同步面片角度，也不以施法者镜头统一朝向。
- 大小适配目标碰撞箱。此验证根据碰撞箱投影与留边计算展示宽高，避免镜头前移导致画面放大。
- 面片位于目标最靠近镜头的支撑平面前方少量距离，让水纹可覆盖人物；透明中心仍可看清目标。开启深度测试、关闭深度写入。

## 预览

- [镜头环绕](water_prison_billboard_orbit.gif)：720×720、96 帧、20fps、4.8 秒。人物可见面随镜头变化，水牢贴图始终正对镜头。
- [固定视角动画](water_prison_billboard_fixed.gif)：32 帧、1.6 秒。
- [宽矮目标](water_prison_billboard_wide.gif)：32 帧、1.6 秒。
- 静态检查：[正面](water_prison_billboard_front.png)、[侧面](water_prison_billboard_side.png)、[背面](water_prison_billboard_back.png)、[斜视](water_prison_billboard_oblique.png)。

这些是三维场景中的二维公告板演示，没有实体球壳。此前体积水壳方案仅留作过程记录，不采用。

## 文件与复现

- `水牢_v3_纯2D公告板验证.bbmodel`：包含人形和默认隐藏的宽矮目标对照，每组只有一张水牢平面；原动画 PNG 已内嵌。
- `preview-billboard.js`：打开工程后通过 Blockbench MCP 执行，启用当前渲染相机朝向、平面位置与大小计算、原色透明混合及水纹播放。相机跟随回调不是 bbmodel 单文件自带。
- `build-billboard.js`：原生 Blockbench 建模脚本。
- `render-previews.js`、`export-previews.ps1`：原生逐帧截图与 GIF 编码。
- `source-validation.json`：内嵌 PNG 与原二维导出的字节一致；每个特效四顶点、一张四边面。
- `render-validation.json`：160 帧中，面片法线始终与当前相机朝向一致，平面始终在目标近侧之前。
- `multi-camera-validation.json`：交替使用两个不同相机渲染，均使用当前相机而非共享固定朝向。
- Aseprite 源文件保留在上述二维 v3 目录，未重画、未覆盖。

此轮没有修改游戏代码、编译、打包或进行多人游戏验证。墙体交界、极近距离和实际实体尺寸仍需接入后在游戏里测试；当前仅验证已明确的二维镜头朝向表现。
