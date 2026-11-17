# 新十三式二维草稿 v1

2026-10-31。状态：待用户预览确认，尚未归档为确认版，未接入游戏。

制作：独立 Aseprite MCP batch，通过原生 Lua 绘制分层帧；未使用图像生成工具。每组64×64、16帧，glow/body/sparks三层。原尺寸透明竖向帧条为 `<name>-strip.png`，可编辑源为同名 `.aseprite`。review-board.gif/png为最近邻3倍预览。预览每帧80ms只用于形状/配色/变化展示，不代表批准的游戏世界尺寸或伤害节拍。

- 01 火苗：fire_spark，主体小火苗与短尾焰；不是火种常驻提示。
- 02 留焰：flame_trail，连贯路径衰减；不暗示独立攻击体。
- 03 陨岩：meteorite，岩壳与熔裂的二维方块外观稿，正式方块材质须按批准外观落地。
- 04 热波：shock_ring，空心扩散环。
- 05 地表锚点：surface_anchor，地表环与指示，非攻击贴图。
- 06 种火：seed_sow，短促火星。
- 07 引燃：kindle，目标附着火焰形状稿。
- 08 引爆：detonate，短促爆燃；实际强度按消费值缩放。
- 09 幽冥：nether_gloom，蓝色魂火；不新增减速或原版火方块。
- 10 崩地：ground_dust，仅土尘碎屑；坑形由已批准参数决定。

喷涌、错时、命中接续复用攻击载体的运动与释放时序，不另造免费攻击特效。此目录和make-drafts.lua仅属过程归档。用户确认后再复制至正式资源目录，维护资源总索引，并由指定Cursor作者接src资源。

工具兼容记录：new_sprite的返回摘要遇到当前Aseprite缺少hasAssociatedFile字段；原生Sprite:saveAs保存成功。后续使用run_lua(save=false,mutates=false)显式保存、render_preview与PNG透明像素读取完成导出检查，未修改MCP插件。10组源文件和竖向导出均已保存；fire_spark透明角alpha为0。
