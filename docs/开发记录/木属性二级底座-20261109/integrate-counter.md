# 整合C：先接已收束的通用迎击

保持author-integration原可写Cursor session、固定grok-4.7-high-fast。你的上一轮a20261109091330505f869已reported_done/valid/writesSettled=true。B author-effects的a20261109091330165d77f也reported_done/valid/writesSettled=true，session 2cae2855-f675-4742-aba1-d28598279385。资源A还在写，禁止读取status/wood/resource目录或其未完成交付。

这是公共接线的独立子范围，只依赖B，主会话已调整调度顺序以并行推进，并未增加玩法或改变架构。先完整读更新的TaskFile、架构裁决第5/6节和通用效果作者交付.md。你是公共文件唯一作者，只修改TaskFile新增明确路径；禁止修改B私有counter目录、MagicPool或资源A目录。

落地B交付的hurt进入/事件/最终bool退出/异常退出、服务器tick末反击、死亡/登出/换维/服务器停止清理。利用现有FireHurtBoundary包装，保持火属性原行为和ElementDamageScope的finally；不要再造冲突的重复ASM包装。新增CounterRuntimeEvents.java作为独立Forge订阅，按交付所需优先级接事件，确保不重复注册，避免木系资源依赖。无合格伤害不消费，最终伤害取消或hurt返回false保留，吸收生命接住有效命中也算；不改无敌帧，反击递归禁止。

必须满足架构中的最终事件取消语义，不能以“同优先级后续监听器不会再取消”为正确性前提；服务端停止也需要实际清理能力。若B现有API不足以满足任一已批准合同，交回精确阻塞，不为赶进度默默放宽要求。

若需要更改B API或TaskFile外文件，不擅自扩大权限，交回精确阻塞。保留已有脏改动。不得运行Gradle、javac或测试脚本；只准备必要检查，等所有写入收束后统一验证。禁止新术式、花谱、渲染、打包或启动游戏。最终写迎击整合交付.md，说明实际钩子/事件、未验证项、changedFiles，回执writesSettled=true，待统一编译。不要改批次记录或运行元数据。
