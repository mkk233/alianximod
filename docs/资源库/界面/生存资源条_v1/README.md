# 生存资源条 v1

2026-11-10，针对生存模式中魔力、水势文字被原版生命、经验和物品名遮挡的问题重绘。创造模式没有这些原版状态元素，所以旧文字看起来正常。此版本未采用。用户随后要求改成接近经验条的细条，确认接入的是 [细条 v2](../生存资源条_细条_v2/README.md)。

Blockbench 适用性：不适用。资源只在二维 HUD 中显示，没有空间形状、朝向、厚度、遮挡、环绕、挂载、尺寸或运动姿态。

版本：resource-bars-v1

确认：待确认

二维源：

- [resource_bars_v1.aseprite](resource_bars_v1.aseprite)：208×28，RGB，1 帧。图标、槽和填充按部件横向排列。SHA-256 `b24d54d406e7ba2f1bde03aa696af83e8491b5720c541a558863e931cee9b2a8`。
- [context_v1.aseprite](context_v1.aseprite)：240×196，左上角位置对照。SHA-256 `5ea5d947c286f7ba3fbe5685cb4530836c0ebf33aea36ba74372ef72ae144dd8`。
- [states_v1.aseprite](states_v1.aseprite)：240×176，空、半满和满的状态。SHA-256 `6f83a1f2603db1b9e75af6f44ce13114b65858924c706c818a763ef6a53ab862`。

绘制脚本保留在 [过程归档](../../过程归档/生存资源条-20261110/src/draw_resource_bars.lua)。Aseprite v1.3.17 批处理实际生成这些源和导出。

二维导出：

- [resource_bars_sheet.png](resource_bars_sheet.png)，208×28，SHA-256 `198ebe28d9c7c8513a422c2373e83676b57d50cc26812f6f27b8d9aa273563e4`。
- [mana_icon.png](mana_icon.png)，9×9；[mana_track.png](mana_track.png)，91×9；[mana_fill.png](mana_fill.png)，87×5。
- [water_icon.png](water_icon.png)，9×9；[water_track.png](water_track.png)，91×11；[water_fill.png](water_fill.png)，87×5。

部件哈希：

- 魔力图标 `ac75186d6c02f078cf933ffbae3aea49723cf2dc0deac09b1d01d2c0e12ed2ac`
- 魔力槽 `7f96be13e08bca264d2bd9645cf217e510fc863cf1f9e3b12880b35a576e8289`
- 魔力填充 `3ee1bece0a613236d3040b81eec35d5fc2a698dbb1a09c7b1c1bbaef948c32cf`
- 水势图标 `6dd23a22a62ef559ef1e8b9cfc80d3627203f762315ba751d07a655ce69d37bd`
- 水势槽 `815e56f7283901c5bc5afbdcf8540abbc51a615f60251a28f0877ed036d21b0e`
- 水势填充 `d462ac2d5e461783f55d99ac797c9a9578f2bb70d8d309ce3cdad79dedc73720`

二维预览：

- [context_v1.png](context_v1.png)，SHA-256 `7d917e890d82635e3e0f625e46c66e8f88b8f7d640d38fc3fb8f1f2309409c4e`。
- [states_v1.png](states_v1.png)，SHA-256 `83c848155dad32f5d4f4f1d7a196bb900d632d837a063d3e0d2caebb81c2622d`。
- 2 倍、4 倍最近邻放大版只在过程归档的 `preview/`，方便查看，不作为游戏贴图。

.bbmodel：不适用。原因同上，没有需要搭建的空间关系。

实际预览：不适用。没有 Blockbench 场景；上列 PNG 是 Aseprite 导出的二维预览。

## 画面

魔力在上，使用金黄槽和菱形标记。水势在下，使用青色槽和水滴标记，并常驻显示。两条默认放在屏幕左上角。

水势槽上沿有两枚刻度。短刻度对准 20，长刻度对准 50，分别对应偏转门槛和让锋分水门槛。填充按当前值占上限的比例裁剪；至少 1 点时保留 1 像素，满值才铺满 87 像素。

数字不放进槽内。魔力显示 `当前/上限`，水势显示当前整数。预览中的 `62/80` 和 `55` 只演示动态文本，接入时由现有数值生成，不烤进贴图。

## 接入位置

GUI 坐标以屏幕左上为原点，单位为 GUI 缩放后的逻辑像素：

- 魔力图标：`8, 8`，9×9。
- 魔力槽：`20, 8`，91×9；填充从槽内 `+2, +2` 开始，最高 87×5。
- 魔力文字：槽右侧 4 像素，垂直居中。
- 水势图标：`8, 25`，9×9。
- 水势槽：`20, 24`，91×11；填充从槽内 `+2, +4` 开始。槽体比画布低 2 像素，上方留出刻度。
- 水势文字：槽右侧 4 像素，垂直居中。

左上角是默认位置，避开底部原版生存 HUD；但小地图和其他模组的界面位置可由玩家配置，因此不能保证所有配置都不重叠。接入时应提供位置偏移配置，并在不同 GUI 缩放和较小屏幕下检查。旧文字锚点是 `W / 2 - 91, H - 48` 和 `H - 60`，不再使用。

水势获得或消耗时，变化值显示在水势主条下方，正值用青色、消耗用旧的琥珀色，几秒后消失，避免遮挡上方魔力条。主条本身保持常驻。

## 运行资源

确认后再复制。建议目录为 `assets/xueximod/textures/gui/resource_bars_v1/`，文件对应上面的六张部件 PNG。数字继续由字体绘制。

现有入口是 `MagicHudOverlay` 和 `WaterMomentumOverlay`，目前都注册在快捷栏之上并用 `drawString`。接入时改为绘制这些部件，同时保留现有魔力合计、法杖加成和水势数值来源。

Blockbench 不能代表的运行效果：GUI 缩放、不同分辨率下的整数对齐、字体宽度，以及创造/生存元素是否绘制，都要在游戏里看。预览里的红框、心、护甲、食物和快捷栏只是位置参照，不进入运行资源。
