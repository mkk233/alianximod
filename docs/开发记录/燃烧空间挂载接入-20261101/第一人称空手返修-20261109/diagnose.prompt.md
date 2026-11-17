# 燃烧第一人称空手偏移：定位事实

固定主渠道Cursor grok-4.7-high-fast。你是燃烧原实现职责迁入统一启动器后的唯一作者。完整读TaskFile、此提示、记录.md，以及原目录implementation-report.md/调度记录.md。只定位本次bug，不做整体验收或独立复审，不修改Java、资源、配置，不编译、不游戏。

用户实测：第三人称火焰披风和手焰完全与预览一致；第一人称在空手时火焰跑到手下方。只恢复已批准的整团火焰随手臂/拳头挂载，不改变手焰形状、大小、色彩、披风或玩法。
允许读当前src/main/java/com/bslx/XueXiMod/client/furnace/内 BurnFirstPersonPose、FurnaceArmFlame、BurnSpatialMount及其必要调用，第一人称相关mixin/动作状态和本机Forge/Minecraft 1.20.1实际代码。不要全仓通读。报告准确当前空手调用顺序、真实原版骨骼变换与复刻变换差异，说明下移具体来自哪个平移/旋转/枢轴/重复或漏用变换。区分空手主手、空副手、地图、持物及动作接管路径。不能猜一个固定上移补丁，要能使火焰跟当前真实渲染手部一致。用户未说普通持物错，不能顺手改变其视觉。

先给最小修复文件清单和具体步骤，不实施。如果只需现有姿态函数常量/分支修正，给真实源证据；若需actual hand render hook，说明为什么以及最小接口影响。确认现有ThirdPerson文件无需修改。只在本子目录写source-diagnosis.md；列出读取的本地API声明位置/可见性、调用上下文、必要作者自查方式。必要时读取正式归档docs/资源库/特效/火焰/燃烧_空间挂载_v1/交接，复用已确认模型，不重画不操控桌面。

本次不是追加正式正确性复审，不重新审查已确认第三人称。主会话不接管Java。你不能操作统一启动器、.grok/runs、批次额度、制作表或他人文件。
最终必须附一块标记JSON，标记各独占一行：
<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"实际结果","changedFiles":[],"checks":[],"notRun":[],"writesSettled":true,"blocker":null,"nextAction":"下一步"}
<<<END_CURSOR_TASK_REPORT>>>
workStatus仅reported_done/blocked/needs_input/failed；数组项为字符串，不写taskId/attemptId/session/inputHash。返回完整最终结果后停止，不流式过程汇报。