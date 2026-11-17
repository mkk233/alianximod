# 火种常驻火苗 v1 草稿

2026-10-30，由主会话通过 Aseprite MCP 的 batch 原生 Lua 绘制。尚待用户确认，不是运行时资源。

- 用户已确认所有带火种实体常驻显示：每满 10 点当前余额一朵，100 点十朵；不足 10 点不显示。
- 单帧 32×32 RGBA，8 帧，每帧 100 ms；循环火尖摇曳与小火星上升。
- `fire_seed_flame.aseprite` 为三图层可编辑源，`fire_seed_flame.png` 为原尺寸纵向图集，JSON 记录帧与标签。
- `fire_seed_flame-preview.gif` 为最近邻 6 倍放大的动画预览。
- 样式为橘红外焰、金黄内焰、浅黄焰心，透明背景；每朵保持单独轮廓便于数数。
- 生成代码见 `create-flame.lua`，只能在 Aseprite 原生 Lua 中执行，不是游戏实现。

工具兼容性记录：当前 MCP batch 在返回 sprite_info/自动保存尾部查询 `hasAssociatedFile` 时报告 API 字段不存在；原生 saveAs 已保存源文件，后续 MCP render_preview 可独立读取全部 8 帧并确认图像存在。未修改 MCP 插件或软件配置。
