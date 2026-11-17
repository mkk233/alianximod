# 水幻形候选 v1：二维制作记录

状态：过程候选，尚未获用户对实际版本的视觉确认。尚未接入游戏。主会话通过真实 Aseprite MCP batch 后端制作；Cursor 会话无 Aseprite 工具。

工具状态：Aseprite 1.3.17 / API39，现有可执行文件 D:/Aseprite/Aseprite.exe。live bridge端口占用，仅使用官方batch后端；没有改配置/安装。new_sprite保存成功但摘要序列化遇到 hasAssociatedFile 兼容错误；随后run_lua读回实际文件确认96x128/32帧，并用run_lua正常绘制、保存和export，不使用出错的摘要接口，不改MCP运行库。

water_phantom_study.aseprite：96x128 RGBA，32帧，每帧80ms，四可编辑层：water_contour、flowing_highlights、separated_droplets、ground_ripples。帧1–16为无脸、中空的水线人形维持；17开始提亮并破散；17–28线段断开、外偏与下坠；29–32落地水纹淡出。idle_outline和cast_then_disperse为分段标签。示意循环复播2.56秒，不是驻留寿命。

water_phantom_preview_dark.aseprite：同版本的暗底预览副本。water_phantom_2d.gif和water_phantom_2d_front.png均由Aseprite原生export最近邻放大4倍到384x512，前者32帧。背景色#131f2c只用于预览，不能进入游戏透明资源。

water_flow.aseprite：16x16 RGBA，16帧80ms，蓝青底与移动的浅青/近白高光。water_flow_strip.png为16x256原尺寸竖条，water_flow_strip.json带帧信息；water_flow_tile.png是第1帧，供建模贴图基准。实际Blockbench应将它贴到水线几何，不能用整个二维人形板冒充立体人形。

water_drop.aseprite / water_drop.png：16x16透明水滴，单帧，独立深蓝边/青蓝水体/近白高光。

主会话已通过render_preview实际查看人体6个关键帧及水纹4个相位。原绘制脚本在scripts/draw_water_study.lua、scripts/draw_water_materials.lua。输入路径、字节数与SHA256在visual-inputs.json。之后二维输入冻结，Blockbench唯一作者另写bbmodel、bb_*实际视口预览及visual-handoff-candidate.md。

没有正式资源库索引更新，没有复制到src/main/resources。最终视觉确认必须绑定同版二维源、真实Blockbench项目与视口、参数交接。确认前不能宣称已采用或Forge效果已通过。
