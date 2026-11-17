# 周身水环 · Aseprite翻浪 v3（验证版）

2026-10-27。用户指定浪头应由Aseprite绘制，本版以Aseprite像素动画替换此前几何尖翅。原水平薄环v1和几何过程稿v2保留。待确认，未正式采用、未接入游戏。

- `curl_wave.aseprite`：64×32、16帧、100ms/帧，四层：浪体轮廓、卷浪暗部与流线、流动白沫、浪尖飞沫。
- `curl_wave.png`：64×512透明序列；second/tail PNG为同内容副本，仅供Blockbench独立错相播放。
- `curl_wave_preview.gif`：Aseprite原生导出的8倍像素预览，16帧/1.6秒。
- `force_water.aseprite/png`：主体水流纹理，128×32每帧、16帧。
- `周身水环_Aseprite翻浪_v3.bbmodel`：可编辑模型，嵌入全部纹理及原生循环骨骼动画。
- `water_guard_fixed.gif`：固定相机，观察贴图翻卷与浪头推进，32帧/1.6秒。
- `water_guard_orbit.gif`：96帧/4.8秒，环绕检查三维空间。

主体环有厚度和8度倾斜，48段六边截面，闭合边均恰好属于两面。三个浪头是8段竖向弧面，只承载整张Aseprite透明动画；浪形轮廓、白沫与卷钩空洞均来自像素素材，已去除原几何浪头。三个动画相位分别0/5/10帧。

预览对水材质临时关闭方向光着色以展示素材原色，未改全局灯光。此项是当前Blockbench会话的显示设置；重新打开模型可执行`preview-water-unlit.js`恢复相同显示。模型与纹理动画保存在bbmodel中，播放选中的`animation.water_guard_flow`并启用纹理播放即可查看。

制作：`draw-wave.lua`经Aseprite MCP执行，`mount-aseprite-waves.js`经Blockbench MCP执行；GIF由真实Blockbench逐帧视口PNG封装，未用生成式图片。源几何制作脚本在相邻v2过程目录。检查了透明卷钩、侧面厚度、身体遮挡和循环端点。无游戏代码改动、无编译或打包、未测试游戏。
