沿impl-visual原task/session修复实际编译错误，固定Cursor CLI --model grok-4.7-high-fast。不spawn、不换模型。先读TaskFile、本Prompt、记录.md最新编译结果。你是V原客户端作者，本单不是新的正式审查。

用户授权继续编译，指定compiler已真实执行compileJava，exit1；日志在本批compile-failed-01/compile-final.log，报告同目录compile-final-report.md。javac唯一当前错误：CooperativePoseApplier.java:4导入com.bslx.XueXiMod.client.action.MatrixFrames，该类包内final，子包不能访问。请根据实际使用最小修正：若导入未用则移除；若确需共享能力，按现有方法职责做最小可见性/调用调整。新增允许你修改client/action/MatrixFrames.java，仅用于此真实访问错误，不改原空间切割行为，不复制无必要的另一套数学工具。

原V写路径仍由你独占，I/C/E及编译者均停写。禁止改I公共API、分身核心或攻击适配器。禁止调整确认版关键帧/坐标/贴图/水幻形skin→flakes顺序。没有美术修改授权，不制作新视觉版。已有未提交改动保留，禁止reset/clean/checkout。

本批设计1/2、实现3/3已用完，本单只按javac错误定向修正，不复审，不改额度/主记录/启动器/制作表。你没有Gradle/打包/游戏授权，修完停止，由指定compile-final对最终源码重新编译。

写本批visual-compile-fix-01-delivery.md说明实际更改与未跑项，可运行必要命名门禁。原compile-failed-01归档不可改。

最终完整机器JSON：
<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"实际MatrixFrames可见性编译修正","changedFiles":[],"checks":[],"notRun":["未统一编译","未游戏验证","本阶段额度已用完，返修结果未追加复审"],"writesSettled":true,"blocker":null,"nextAction":"主会话仅转达作者结果，写入收束后原compile-final继续编译"}
<<<END_CURSOR_TASK_REPORT>>>
