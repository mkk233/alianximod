# 幽冥 · 青色魂火 v2（二维像素草稿）

用户2026-11-11要求：幽冥特效重新画一版，突出青色火焰，参考灵魂沙峡谷，更贴合 MC。当前交付仅二维草稿，尚未视觉确认；没有正式采用、替换运行素材、编译或启动游戏。

## 输入与范围

主会话只查幽冥这一符号与单项需求。制作表制作中第13行 B=fire_nether_gloom；N/O要求领域中不断冒出蓝色魂火，依附领域载体，范围/时长继承载体。不给减速，不放置灵魂沙或原版魂火。本次仅重画视觉，不改术式逻辑。旧正式幽冥_v1保留。

历史批次待制作-20261031，表DV记设计2/2、实现3/3，均已用完。本次无新增正式正确性复审；制作时查看像素预览及用户选稿不计新增复审，也不声明用户已确认。

## 素材

- youming_cyan_soul_v2.aseprite：4层可编辑源，64×64，16帧，每帧80ms，循环1280ms。
- youming_cyan_soul_v2_strip.png：64×1024，帧从上往下，透明背景，未复制到游戏。
- youming_cyan_soul_v2.gif：最近邻4倍透明动画，展示像素与动作。
- youming_cyan_soul_v2_preview.gif/png：深/浅背景对照，最近邻3倍；背景和基线仅辅助，不进游戏，不是Blockbench截图。
- draw_soul_fire_v2.lua、preview_board.lua：经Aseprite MCP run_lua执行的绘制/预览脚本。没有ImageGen或外部光栅图片编辑。
- manifest.json：同版本文件hash。实际世界尺寸、朝向、布设密度待真实Blockbench验证，1280ms只是贴图循环不是领域持续时长。

观感：青色为主、浅青焰芯、深青少量边缘；2×2粗像素块；5簇高低错开的火舌，焰芯分段上流，7组错相上升火星。无模糊泛光，无橙红火焰。
参考：https://www.minecraft.net/en-us/article/around-block--soul-sand-valley 。参考其灵魂火方向，源像素为本次Aseprite绘制。

## 工具与阻塞

Aseprite 1.3.17 MCP批处理已真实绘制并render_preview查看。live桥41235被另一个实例占用，未关闭或重启用户实例。open_sprite元数据方法因hasAssociatedFile不兼容报错，改用同一MCP run_lua、render_preview和export成功；原文件未修改。

原生只读代理 /root/youming_art_facts，请求deepseek/deepseek-flash low，直接父级/root。返回400 task was sealed；未实际执行读取、无写入或子代理，结果已接收。未换模型/账户/旧CLI绕过。当前表只由主会话按单文档读取例外核对单行，未写回；原阶段计数未重置。

Blockbench首次工具目录为空且进程未运行。用户打开后，确认真实Blockbench.exe窗口已运行；再发现两次仍没有任何Blockbench工具。当前没有已暴露的MCP重连接口，连接层状态未知，不能认定服务本身坏掉。未使用桌面、原始HTTP、新桥、headless模型或手写bbmodel冒充真实预览。尚无bbmodel和3D视口预览。

遵循xueximod-art-preview：有空间关系必须真实Blockbench；仅MCP限制下连接阻塞时二维可继续。恢复本聊天的Blockbench工具后，应重发现并验证，再用本版本做领域火簇实际空间示意。用户确认同版本后才可归档采用和交原生Grok接入。制作表/正式索引本轮均未改。
