# 最终源码统一编译

固定Cursor CLI --model grok-4.7-high-fast，新可写会话，仅为构建输出/本目录验证证据需要写模式。你是本任务唯一编译执行者。只有主会话在全部相关源码写入收束、当前正式裁决允许编译后才派发本单。读记录.md与最新review-decision文档确认状态，不重新审代码正确性、不自动占新审查轮次。

允许在D:/alianximod根执行且只执行：
` .\gradlew.bat compileJava --offline --no-daemon `

这是明确编译授权，不是只读ask任务。禁止build/jar/reobf/test/runClient、游戏、网络拉取、源码修复、改制作表、git提交/reset/clean/checkout、改审批策略、改模型。org.gradle.jvmargs=-Xmx3G和daemon关闭的项目配置保持。编译失败只返回诊断，交原文件作者修，不能边编边改。

先做最小只读环境/占用检查：没有其他源码写作者或活跃Gradle编译，后台IDE/daemon存在不直接等于正在构建。与外部候选查询任务已协调，该任务A/B全部released并让出编译槽，本次不替其追加正确性审查。检查必要时只看进程/任务说明/结束元数据，不读取其他任务实时输出。若有新的未交接源码作者或构建冲突，停止并准确报告，不能抢跑。

保存开始/结束时间、真实命令、Gradle退出码与完整stdout/stderr到compile-evidence/，并报告Java与Gradle用户缓存位置。上次项目可用本机路径是JAVA_HOME=C:/Program Files/Java/jdk-17、GRADLE_USER_HOME=C:/Users/baishang/.gradle；若当前进程错误地落到空沙箱cache而offline缺已有插件，可在本次进程内使用这些已存在正确环境重试同一命令，必须记录首轮失败和恢复，不能改项目配置或下载。不要绕过审批拒绝：若具体Shell调用被Rejected，保留反馈并停止被拒动作，不换壳或工具执行同一拒绝命令、不加--force。

在获准Shell范围内记录src/main最终源码文件数量和前后SHA快照，确认编译期间源码无变化；至少记录本任务各evidence列出的所有源码路径。身份核验是为确认最终编译覆盖的源码，不是新代码审查。无法获得快照如实报告，不编造。若compileJava为UP-TO-DATE明确注明，不说刚重新javac。失败与成功必须区分CLI外层退出和真实Gradle退出。

命名门前两作者已经跑过，当前已知仅OriginalHumanoid的旧i/to两处，不重复修或拓展检查。本单不独立测试。

若错误落在候选查询新增服务/mixin，请记录确切文件/行/编译输出并保留现场；另一会话会接回其原作者，本任务编译者不得擅改。若是本次绑定接口错误也只报告。

结束写compile-final-report.md和compile-evidence/result.json：实际跑/没跑、每次命令退出码、最终源码身份/变化、可用结果、未验证游戏及Mixin运行、编译槽已释放。没有执行成功不能宣称编译通过。所有源码始终只读，本单仅写构建产物与本目录证据。
