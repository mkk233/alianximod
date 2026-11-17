# 周身水环 v7：双缘锯齿逆时针旋流

状态：美术验证草稿，待用户确认；未采用到游戏，未改游戏代码。旧版保持原样。

## 本版设计

按用户“多锯齿逆时针流淌溅射，贴一整圈在水环两侧”重画。两侧按内外缘布置：外缘约20簇、内缘约16簇，分叉与高度错开；尖端沿同一逆时针切线弯折。浪根连续接合，青蓝为主，浅色集中在少量尖端和脱落水滴。撤下v5的五处孤立大浪冠。

中央水体保留长弧、短流和追赶碎块；内外浪缘与主体共同旋转。Blockbench只提供平滑承载面，锯齿和分叉来自Aseprite透明贴图。

## 预览

- [固定斜视动画](water_saw_fixed.gif)：720×720，96帧，20fps，4.8秒闭环。
- [固定俯视动画](water_saw_top.gif)：检查两缘同向逆时针。
- [外缘二维动画](saw_outer_preview.gif) / [内缘二维动画](saw_inner_preview.gif)。
- [静态预览](water_saw_preview.png)。

预览由Blockbench实际三维场景逐帧截图导出，人物是验证遮挡和尺度的占位模型。GIF深色底仅用于展示；源贴图背景透明。

## 可编辑源文件和原尺寸导出

- `saw_splash_outer.aseprite` / `.png`：外缘，256×40，每帧100ms，16帧；PNG为256×640竖图集。
- `saw_splash_inner.aseprite` / `.png`：内缘，同规格，独立造型和错相。
- `broken_saw_flow.aseprite` / `.png`：主体，256×24，16帧；PNG为256×384竖图集。
- `周身水环_双缘锯齿逆时针_v7.bbmodel`：原生模型，全部贴图已内嵌。
- `draw-saw-water.lua`：实际在Aseprite执行的逐层逐帧绘制脚本。
- `mount-saw-water.js`：Blockbench载体和UV构建脚本。
- `frames-fixed/`、`frames-top/`：原生透明截图；`export-previews.ps1`将其合成预览GIF。

## 运动参数与检查

根组 `双缘水花与碎流共同逆时针` 在2.4秒内绕Y轴从0°均匀到+360°，三个水体网格共同跟随。U增大对应世界周向角减小；0.6秒时+x样点移动到-z，在俯视镜头中是逆时针。内缘不作水平镜像。

贴图1.6秒循环，只执行局部拉长、分叉、短滴脱落和回缩，没有另加整圈UV滚动；与根旋转的组合周期为4.8秒。几何、纹理在0和4.8秒状态一致，固定斜视与俯视各96个不同原生帧。详见 `validation.json` 与 `validation-native.json`。

Blockbench水体材质在本次预览中关闭自身明暗衰减，以显示Aseprite原色；该会话设置不随bbmodel保存。重开后如需复现相同色彩，运行 `preview-water-unlit.js`。人物仍使用正常明暗。

此次仅进行Aseprite/Blockbench验证；未编译、未打包、未做游戏内验证。

