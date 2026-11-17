# I 原作者：远距核心收束后的最终配置/接线与指定世界调整
固定Cursor CLI --model grok-4.7-high-fast，原task/session；设计2/2、实现3/3耗尽，本单是已授权实现整合和必要自检，不作正式复审，不通读无关代码。主会话只在C core有效最终回执后派本单。
输入design-round-2-gameplay-feedback.md、integration-range-20261104-delivery.md、clone-range-20261104-delivery.md、integration-clock-20261104-delivery.md、visual-gameplay-repair-20261104-delivery.md、visual-wedge-v2-delivery.md。其中动作时间源已由你修，V无额外跨作者时钟待办。
C核心现在已按公共接口落地，在不改C文件的前提下完成确有需要的I接线。保持唯一tryReserve/不重复归位；同一端口实例已应实现REMOTE_RECALL。若C报告必须另改文件而不在你的TaskFile，精确交回，禁止跨作者改。
开放CooperativeContent.java中现有rule_cooperative稳定键：selectionRadius默认24、范围0..48；moveLimit默认32、范围0..64；sideOffset仍1.25/0.75..2。现有配置消费者在network/CooperativeChainCompiler，只有必要时修改；不得改变原单次费用、冷却、命中结算、复制规则或表。重要值必须实际消费，不能仅改文档。缺键新默认，其他世界显式旧8/4保留，schema不变。
用户明确给出本次测试世界：D:/alianximod/run/saves/新的世界。主会话已确认世界目录和serverconfig/xueximod-balance.json存在。仅授权这个文件rule_cooperative下selectionRadius/moveLimit两键设24/32；不修改其他世界/模板。先解析并记录当前值、哈希、mtime；复制独立时间戳备份到本批world-config-backup/；窄改或新增两键，其他对象/字段/数值必须保持，重新解析核对非目标字段语义等价。提交前原文件若已被别人改则停止交回而非覆盖。不要抹掉未知键。若当前值本来已24/32就无需重写。记录真实备份和前后SHA，生效为重开世界/服务器，本任务不启动游戏或重启。
更新docs/配置平衡指南.md中协同段真实默认/允许范围/消费者/生效时机，不重写水流和其它术式内容。更新本批游戏实测说明.md与integration-delivery.md，将旧8/4和待确认新视觉状态明确标成历史，写当前四项返修行为/用户世界配置结果；不要声称游戏效果已验证。既有资源真实映射沿V报告，原v1人物动作+v2薄壳。
交付integration-gameplay-final-20261104-delivery.md：实际代码/配置变更、给C/V剩余必须待办（无则明确无）、明确编译未跑、命名门禁、原世界备份/两键前后/哈希/未变其余字段、用户实测重点。禁止Gradle/build/jar/reobf/runClient、游戏、提交、制作表、.grok启动器运行记录修改。
最终合法CURSOR_TASK_REPORT，reported_done只表示本实现阶段完成；writesSettled=true后由原指定compiler统一执行，不自己附加任何复审或编译。

交付实测入口补充：本次Java变更需要用户重新启动开发客户端后进入「新的世界」；只按F3+T不能加载Java修复。配置本身按既有规则重开世界生效。不要自行启动游戏。
