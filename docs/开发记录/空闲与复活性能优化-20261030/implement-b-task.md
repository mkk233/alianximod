# Cursor 实现 B：动作与蒙皮空闲路径

固定 Cursor CLI --model grok-4.7-high-fast。你已是被授权实现者，不再派发代理。先读 AGENTS.md、.grok/skills/max-dispatch/SKILL.md 和本目录记录.md。设计1/2已批准，实现0/3，当前是实现而非独立复审。不创建/运行测试，不编译、不打包、不启动或操控游戏、不提交。大量既有修改必须原样保留，禁止reset/clean/checkout。Git用单次 -c safe.directory=D:/alianximod。
本项目根 D:/alianximod。唯一可写源码为 src/main/java/com/bslx/XueXiMod/ 下这些具体类（用rg --files核对其准确包路径）：EntityRenderDispatcherActionMixin.java、ItemInHandRendererActionMixin.java、HumanoidAttackTimeMixin.java、HumanoidArmorSkinMixin.java、StuckLayerScopeMixin.java、SkinAttachmentBridge.java；仅查询所需时可改 ActionClientPlayback.java、ActionRenderQueries.java。不重写或修改 SkinSession.java、RenderPoseScope.java、姿态体系或其他源码。可读取这些入口的直接依赖以落实设计前提。输入read-client-final.md第3/4/5项。
已批准设计：
1. 加不创建姿态/会话的保守查询：UUID根本没有播放记录才允许绕过动作专用快照、采样、会话，原版调用一次。已有记录保留全部旧路径，包括淡出、过期清理，不另造“活动”规则，不把已过期但未清的记录当不存在。
2. 先确认没有独立于动作的蒙皮需求。不能证明的入口保留原状并报告，不用无动作直接跳过所有蒙皮。
3. 护甲、箭矢包装只在当前实体、当前渲染作用域确实需要蒙皮时创建；不能只看“全局任意session存在”。保留嵌套渲染的隔离和恢复，即使外层有蒙皮内层无动作也不能错误继承。不能证明安全的包装保留原路径。
4. 所有快路保持原调用次数/参数、异常恢复、模型状态、手部接管挥砍规则、原版护甲染色/纹饰/附魔光，不改变任何实际动作外观/时序。
5. 没有动作的attackTime读取不创建Pose。当前作用域已持有采样时不得跳过其语义。
实现前将将修改现存文件当前字节备份到本目录before-b/下保留包路径，这是此次增量基线而非git HEAD。仅你写before-b、implement-b-report.md、implement-b-evidence.json和name-gate-b.log，不写记录.md。其他作者在改StillSheetPlayback/Cache与DomainMotion/ClientState/Bridge，另一聊天在改火种/燎里，严禁写这些。
做必要自查和 powershell -NoProfile -File tools/check-name-gate.ps1，只修自己文件命名，保留其他历史问题。最后完整报告具体修改文件与行号、前后SHA256和最后写入时间、实现前提的源码证据、快路条件、保留/跳过的包装及原因、是否有未完成；声明全部源码停止写入并释放占用、待统一编译、未游戏验证。不要估计帧数收益。前提不成立则停对应改动，不扩大设计。
