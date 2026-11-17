# 生存资源条 · 细条 v2

2026-11-10 用户确认「按照这版接入」。条身高 5 像素，接近原版经验条；默认放在屏幕左上角。魔力在上，水势在下并常驻，水势槽标出 20 与 50。此版本取代未采用的粗条 [生存资源条 v1](../生存资源条_v1/README.md)。

Blockbench 适用性：不适用。资源只在二维 HUD 中显示，没有空间形状、朝向、厚度、遮挡、环绕、挂载、尺寸或运动姿态。

版本：resource-bars-thin-v2

确认：用户，2026-11-10，原话「按照这版接入」

二维源：

- [resource_bars_thin_v2.aseprite](resource_bars_thin_v2.aseprite)：208×28，RGB，1 帧。SHA-256 `258e8458a06ce58e7b0e6fcb92446bd78d217a6990f2c2257d80dddccacb3201`。
- [context_v1.aseprite](context_v1.aseprite)：240×196。SHA-256 `03fce03fc575d50af96604867a9405a1f387e8258602b18f60c8d3f391259af9`。
- [states_v1.aseprite](states_v1.aseprite)：240×176。SHA-256 `6c5a32c8e760e6ade22cdf31d4c6b30adb020e6e3d6a2a58df7285818a47ae92`。

绘制脚本在 [过程归档](../../过程归档/生存资源条细版-20261110/src/draw_resource_bars.lua)，由 Aseprite v1.3.17 批处理生成。

二维导出与运行部件：

- [mana_icon.png](mana_icon.png) 9×9，`ac75186d6c02f078cf933ffbae3aea49723cf2dc0deac09b1d01d2c0e12ed2ac`
- [mana_track.png](mana_track.png) 91×5，`96fba453d08710e3df1e2de92e3b1ee34c25c5af8ba60630ad0b2a9167636702`
- [mana_fill.png](mana_fill.png) 87×1，`19f192c0b180f6ef2c5ca5345021c125121e2731c5898176fa4204223e70d526`
- [water_icon.png](water_icon.png) 9×9，`6dd23a22a62ef559ef1e8b9cfc80d3627203f762315ba751d07a655ce69d37bd`
- [water_track.png](water_track.png) 91×7，`de24a2d94d8a781f175dcebd64d8114eba12dbb31eef4521116cc7c194912f1a`
- [water_fill.png](water_fill.png) 87×1，`093f81c4ac0cf13dc730bc11a07912adbd4e8bbc469c2df0468f1785fa338f63`

合图 [resource_bars_sheet.png](resource_bars_sheet.png) 只供查看，不进游戏。SHA-256 `5f22f4e9cb42f4d4be2cba773e2a27c4d0e77fa326bd046d4a75697a8edd2265`。

二维预览：[context_v1.png](context_v1.png) `9aa7823c0c29b5e60749ca2aba7ef19c9a91ab349bb464e550fac4d3ec752853`；[states_v1.png](states_v1.png) `02db4279967f69c22a102dab0862f02dbdb569f7f84bccb7fb0bdaab91f76db7`。4 倍最近邻预览留在过程归档，不作为游戏贴图。

.bbmodel：不适用。原因同上。

实际预览：不适用。没有 Blockbench 场景。

## 画面与坐标

GUI 坐标以屏幕左上为原点，单位是 GUI 缩放后的逻辑像素。默认：

- 魔力图标 `(8, 6)`，9×9。槽 `(20, 8)`，91×5。填充从槽内 `(2, 2)` 起，最多 87×1。
- 水势图标 `(8, 23)`，9×9。槽 `(20, 24)`，91×7。填充从槽内 `(2, 4)` 起，最多 87×1。槽上方 2 像素是刻度，5 像素条身从槽内 y=2 开始。
- 文字在对应槽右侧 4 像素，相对 5 像素条身垂直居中。使用游戏字体，不使用预览里的点阵数字。
- 魔力文字为 `当前/上限`。水势文字沿用现有一位小数格式，整数不带小数。
- 填充宽度：比值为 0 时不画；大于 0 时至少 1 像素；达到上限才画满 87 像素。中间按 `floor(比值 × 87 + 0.5)`。
- 水势 20、50 刻度已经画在槽贴图上，代码不要另画。

魔力仍使用现有缓存、法杖存量和法杖上限加成，上限至少为 1。水势仍使用现有通知里的结余，上限 100。两条在生存和创造模式都显示。原版 HUD 隐藏时不显示。水势变化沿用现有短暂列表：正值青色、消耗琥珀色，画在水势槽下方，不挡住魔力条。

运行目录：`assets/xueximod/textures/gui/resource_bars_v2/`，六张部件按原文件名复制。最近邻显示。预览中的红框、生命、护甲和快捷栏不进游戏。

Blockbench 不能代表的运行效果：GUI 缩放、窄屏幕、字体宽度，以及原版 HUD 隐藏。游戏效果由用户实测。
