# 周身水环 · 像素旋冲 v4（待确认）

2026-10-27。用户参考：https://huaban.com/pins/5784587260 。已通过页面真人验证并逐帧观察原动图（240×240，48帧/3.36秒）；页面标题为“超个性像素风动效gif参考 - 游戏特效论坛 - CGJOY”，来源链接为 https://www.cgjoy.com/forum.php?mod=viewthread&tid=226079 。参考文件仅供风格分析，不属于可使用游戏资产。

本版由Aseprite MCP重新绘制水弧及展开水流。提取深色局部轮廓、硬边大色块、前端集中的亮色、尖尾碎片与快速扫过的节奏，保留水属性蓝青白配色。保留v1/v3及v2几何过程稿，未正式采用、未接入游戏。

- `surge_arc.aseprite/png`：96×32每帧，16帧，100ms/帧，四层：轮廓水体、蓝青色块、冲刷亮刃、短碎水片。PNG为96×512透明序列。
- `surge_flow.aseprite/png`：128×32每帧，16帧，断续、有尖尾的环向水流；削弱均匀闭合圆管。
- `surge_arc_echo.png`：同图副本，仅为Blockbench独立播放8帧错相余流。
- `surge_arc_preview.gif`：Aseprite导出的8倍像素动画。
- `周身水环_像素旋冲_v4.bbmodel`：嵌入纹理和原生`animation.water_guard_surge`动画。主旋弧、短余流、低调厚度环体分开可编辑。
- `water_surge_fixed.gif`：真实Blockbench固定视口，32帧/1.6秒。
- `water_surge_orbit.gif`：96帧/4.8秒环绕视角。

浪的轮廓、收束、尖端和短水片来自Aseprite逐帧绘制，Blockbench只承载弧面、厚度和空间遮挡。几何推进在一周期内加速、短停、收尾，并非全程匀速。水体临时关闭方向光着色显示原色，重开可执行`preview-water-unlit.js`恢复；这项预览显示设置不写入bbmodel。

已检查32帧互不相同、循环端点姿态与贴图帧一致，前后遮挡可见。制作脚本为`draw-surge.lua`和`build-v4.js`，导出脚本为`export-previews.ps1`。未改游戏代码，未编译、打包或测试游戏。
