固定Cursor CLI --model grok-4.7-high-fast。只由主会话在全部相关作者完成/停写、最终源码冻结后Start；若记录未给最终编译放行，不开始。读TaskFile/Prompt/AGENTS/新流程/记录.md的最新编译放行段和各最终报告。本单只编译/必要资源同步和交付证据，不写Java、不修代码、不审查业务、不改制作表或额度，不启动游戏。不reset/clean/checkout，不build/jar/reobf/runClient。

编译前仅检查是否有本workspace相关Gradle编译运行，避免并发，不打印完整java/game命令行或敏感参数、不杀无关IDE daemon/游戏。相关写入未收束就blocked交主会话，不读中间代码。记录src完整相对路径/长度/mtime/SHA256快照到compile-final-before.json。
环境使用最近成功的本机配置：进程内JAVA_HOME=C:/Program Files/Java/jdk-17，GRADLE_USER_HOME=C:/Users/baishang/.gradle，PATH前缀为该jdk/bin；不改系统/用户全局配置或gradle.properties。若环境不同先报告实情；不下载新JDK/缓存。记录java-version与已有gradle.properties Xmx3G/daemonfalse。旧水幻形报告只供环境证据，不作为本批成功依据。

运行本项目必要命名门禁tools/check-name-gate.ps1并保存日志；门禁失败停止，精确错误交回作者，不改源。
在D:/alianximod执行原命令 .\gradlew.bat compileJava --offline --no-daemon （可通过cmd.exe /d /c运行该bat）。实际exit和日志保存本批compile-final.log。记录开始/结束、是否compileJava实际执行或UP-TO-DATE、错误/警告。编译失败停止，原错误交主会话对应作者返修，不自行改Java或重复无变化编译。
本批有新确认资源，编译成功后在同一冻结窗口串行执行 .\gradlew.bat processResources --offline --no-daemon，记录独立exit和compile-final-resources.log。它只是同步运行资源，不做jar/build。若resource任务失败不得称完整可交付。
保存compile-final-after.json，比对src新增/删除/内容变化；任何变化说明不能宣称覆盖最终冻结字节，交回主会话协调，不循环擅自重编。核对新动作JSON、斩光PNG、语言等本批实际新增资源在build/resources/main与src字节一致；正式斩光PNG的src/输出SHA256应等于已确认58E741C8BED808EB4E8CC6DC77FA71FD009A9AAB0B7FEBC104ACCB68E655BFD7。也记录源动作来自正式v1的转换映射，但不重新业务审查。
仅写指定报告/日志/快照与Gradle正常输出；不读写.grok启动器运行记录。不要触碰其它批报告。所有结果记录compile-final-report.md，明确未游戏验证、未打包、未启动游戏；实际源码问题不藏在成功结尾。

最终唯一机器回执（标记各一行，字段真实，不写task/attempt/session/inputHash）：
<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"本批实际统一编译与资源同步结果","changedFiles":[],"checks":[],"notRun":["未游戏验证","未打包","未启动游戏","未新增正式复审"],"writesSettled":true,"blocker":null,"nextAction":"主会话汇总实际编译结果；表无本任务匹配已记录，不虚构迁表；交用户游戏实测"}
<<<END_CURSOR_TASK_REPORT>>>
任一失败/阻塞使用failed/blocked并说明，退出0不能替代业务回执。
