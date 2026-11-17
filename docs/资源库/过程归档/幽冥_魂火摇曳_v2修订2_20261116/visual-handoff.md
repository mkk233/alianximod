# 幽冥摇曳返修：制作阻塞交接

状态：**blocked / 未完成候选，不可用于同版确认或接入**。
batchId `待制作-20261031`；taskId `fire_nether_gloom_v2_domain_preview`；attemptId `20261116-art-gloom-sway-03`。

用户准确原话：「火焰特效一直在变化大小的这个效果有点蠢幽冥的火焰应该摇曳而不是变大」。

本次需求在前版完整 N/O/DV 快照、approved-v1 覆盖与最新反馈上继续，未用原始档覆盖现行需求。应取消整簇周期宽高缩放，固定基础大小、位置、根部和 yaw；通过局部火舌形状/方向错相摇曳，而非整簇硬纸板摆头。保持 49 簇、7×7×8、挂点 minY+0.15、交叉面、基础宽0.6486–0.9964格/高0.58–1.2296格，青白内核/暗青外焰及现有天空远雾方向。

## 本轮实际完成

- 已读取当前美术技能、交接模板、前版 handoff，检查适用 AGENTS；本轮是美术制作，不是复审。
- 已发现并实际查询当前喜鹊 Aseprite、Blockbench MCP。
- 新目录内仅写了 `evidence/make-sway.aseprite.lua` 制作草稿及本轮阻塞文档。该草稿拟将原 v2 像素拆为固定根部与4个独立火舌层，根部为44–63行，局部错相0/5/9/13帧；**没有成功落盘的美术源，不能把拟定参数写成已实现效果**。
- 没有更改旧候选、源码、正式资源、制作表、共享记录或索引；没有运行编译、游戏或打包。

## 原生工具事实

1. `aseprite__status`：1.3.17，batch ready，live.connected=false。
2. `blockbench__get_capabilities`：desktop5.2.2/plugin1.10.1，原项目 `0160f5df-5ae4-58e0-f638-74fa0e1dcca0`，273 cubes/65 groups/19 textures/1 animation；仅查询，未改动。
3. 只读进程检查未发现 Aseprite 进程，随后通过原生 `aseprite__launch_aseprite` 打开已有候选源，返回 connected=true；没有另启 Blockbench 或其他服务实例。
4. `aseprite__run_lua` 明确指定 live，调用新目录 Lua 制作脚本。工具返回：`The MCP server aseprite couldn't run run_lua: it didn't answer in 50s`。工具调用墙钟约81秒；这是单次工具自身等待，未自建等待循环。
5. 有界补查一：status 返回 live.connected=true，但这只能证明状态查询成功，不能证明制作操作恢复。
6. 有界补查二：live只读 Lua 查询当前文档，返回 `Aseprite disconnected before answering`。随后停止调用，不循环恢复、不改配置、不换实例、不转桌面或headless。
7. 最后只读文件检查：新目录只有 Lua 草稿，**没有 `.aseprite`、PNG 或 GIF**。Aseprite进程 PID61004，Responding=true，窗口标题为前版 `youming_v2_soulvalley_palette.aseprite - Aseprite v1.3.17`。进程响应不等于MCP操作正常；具体根因未知，未把它推定成权限弹窗、用户拒绝或额度问题。

## 未完成与后续边界

新源、原尺寸导出、二维动态预览均未完成。没有在真实 Blockbench 应用新的摇曳纹理、取消旧 scale 曲线或保存新工程，因此也没有新的真实第一人称、近景GIF、侧面PNG及复开证据。旧版仍含被用户否定的整簇缩放，不能拿旧片段充作本轮结果；不提交可看候选。

本轮主动写入已经停止；超时 Lua 请求是否在应用中仍挂起未知，未擅自关闭/重启应用取消它，不能宣称已经证实不存在晚到执行。若连接恢复后出现晚到文件，须重新核验后才能作为后续产物，不能凭文件出现自动转为完成。后续先恢复同一会话支持的 Aseprite live 调用并重新核实当前文档，再沿本任务授权续作；不需改设计或追加复审。

仍为未完成/未确认/未接入/未编译/未游戏实测，不迁待验收。**本阶段额度已用完，返修结果未追加复审。**
