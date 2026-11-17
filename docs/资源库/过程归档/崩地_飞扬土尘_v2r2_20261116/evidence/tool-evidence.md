# 实际工具与美术自检记录

attemptId `20261116-art-ground-v2-04`，2026-11-16；不是新正式复审。

## 连接和操作

1. 当前工具入口为 `tool_search` + 原生 MCP。续行初尝试历史 `functions.exec` 得到 `unsupported custom tool call: exec`，随后使用宿主实际提供的 `functions.exec_command`，没有假设旧工具可用。窄查询一度返回无匹配/不相关元数据，改用真实操作描述取得 schema；不把搜索结果当连接故障。
2. `mcp__magpie.aseprite__status` 实际成功：executable `D:/Aseprite/Aseprite.exe`，1.3.17 / API39，batch ready，live listening=true、connected=false。
3. `mcp__magpie.blockbench__get_capabilities` 实际成功：desktop5.2.2、win32，plugin1.10.1，起始项目 `3eaaca81-4233-01ff-c351-cb6d780f81b3 / earth_break_ground_v2_preview`，367 elements、192 groups、2 textures、1 animation。
4. `mcp__magpie.aseprite__run_lua` 首次 `dofile` 报 `attempt to index a nil value (global 'args')`，没有生成成功假报。原因是 MCP 注入的 args 在调用环境中，外部 Lua 文件全局未共享；把 `_G.args=args` 明确赋值后执行实际脚本成功。后续增加粗粒透明相位并重新原生导出。
5. `mcp__magpie.blockbench__create_project` 成功另建 free 项目 `937be075-0517-53f5-6aa9-c7f18ce35ab8`。本轮实际发现并调用喜鹊 Blockbench 的 `risky_eval`（JavaScript 原生求值工具），在真实进程创建 Group/Cube/Texture/Animation/关键帧，带 Undo 事务，未使用 headless；不沿用旧上下文中的混淆入口名。
6. 独立 `mcp__blockbench.set_camera_angle` 只用于激活当前实际视口的透视相机；后续通过已发现的 magpie 原生 JS 设置同一实际 Preview，相机不是离线渲染器。
7. 实际 `Preview.screenshot` 输出 PNG，`Timeline.setTime` + `Animator.preview` 逐帧。最初 crop:true 自动裁边使时序构图变化，改为 crop:false；中间 FOV 过窄截到辅助平台底部，扩大 FOV并重取最终所有帧。最终整体 FOV42 / 侧面 FOV38；验证 92 个最终原生帧对象都在视口内。工作取景 PNG 保留并明确不用于最终确认绑定。
8. `Codecs.project.write(Codecs.project.compile(...))` 原生保存 `.bbmodel`。`Codecs.project.load` 读取这个实际保存文件，成功创建复开项目 `1aa7c01f-c3a9-2d54-e824-b3331f4094e9`；445 elements、279 groups、3 textures、1 animation。异步纹理加载后再次原生截图，三个 error 都为 0。
9. Aseprite 实际只读复开 `ground_airborne_v2r2.aseprite`：144×48，24 帧，总 1.2 秒，三个图层名称与源一致；`save:false,mutates:false`。
10. Python/Pillow 只做既有二维导出的最近邻预览、真实原生 PNG 的 GIF 编码、GIF 解码、自检和 SHA 清单；不做三维离线场景渲染，不拼贴伪截图。

## 作者查看与数据检查

- 用 `view_image` 实际查看二维关键帧、整体发射、侧视发射/顶点/回落/尾段、俯视露坑、模型复开视口；又查看最终 GIF 解码尾段，确认低 alpha 真实消散，不以原生透明 PNG 的 RGB 预览误判为发光。
- `2d-frame-inspection.json`：末前帧最大 alpha16，末帧全透明；24 帧均不同。
- `native-capture-overall.json`、`native-capture-side.json` 保存每帧时间、可见数量、相机和实际世界矩阵采样。粗粒01有明确上升与下降，不是同一姿态截图重复。
- `gif-verification.json`：两段都是 2300 ms；整体 46 次采样→36 编码帧/33 不同画面；侧面 46→33/30。编码器合并静止相同帧，不把采样数虚报为 GIF 有效帧数。
- `final-validation.json`：23 单元、原生模型长度、Aseprite 文件帧时长、28 个既有项目名称/保存状态、115 个保护文件 SHA 均通过。所有 Python 脚本实际运行退出码 0；这不是游戏验证或编译。

## 失败性质和边界

- Carbon/Argon 的 `Unknown model` 是本任务前两次实际派发证据，沿用父方准备回执；本 attempt 未请求任何模型，不将它写成额度耗尽。
- 一次 Lua 环境变量错误已修复；没有持续工具阻塞。
- 不使用 ComputerUse，不改变 live 连接、不重启应用、不安装桥、不改全局配置。MCP 串行操作。
- 不再尝试已拒绝 native ancestor 的 `send_message_to_thread`。当前没有已暴露的原生父讯入口，完成后直接 final 向父级回传。
- 本阶段额度已用完，返修结果未追加复审。
