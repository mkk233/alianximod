继续同一 burn-spatial-implementation 任务及会话。当前阶段仅统一串行编译，不修改任何源码或美术资源，不重新做代码复审。本任务修复阶段与制作表前置已有效最终返回且停写，具体回执由主会话记录。保留所有工作区改动。

先确认自己的 implementation-report.md 回执和源码停写；检查共享工作区没有相关 Java 写入者、没有正在占用本工作区的 Gradle/runClient。若不能确认安全串行或仍有写入/构建，报告 blocked，不杀用户进程、不并行编译。采集 src/main/java 及此次相关源码/资源的文件数/hash/mtime作为稳定基线，结束再比对。不得把仅相同总数当作内容没变。无需追加功能测试或其他复审。

仅以本机进程环境执行：JAVA_HOME=C:/Program Files/Java/jdk-17；GRADLE_USER_HOME=C:/Users/baishang/.gradle；在当前子进程 PATH 前加 JDK bin。不要改持久环境/gradle配置；不要使用 Cursor 临时 .gradle 缓存，已知那里缺 foojay。

在 D:/alianximod 运行唯一授权命令：.\gradlew.bat compileJava --offline --no-daemon。使用现有 org.gradle.jvmargs=-Xmx3G。禁止 build/jar/reobf/runClient/启动游戏/联网拉取依赖。

源码修改在上个阶段已停止；本阶段允许 Gradle 正常编译产物和缓存写入，以及本提示同目录 compile-report.md 与 编译证据/。保留命令输出、实际退出码、任务结论、起止时间、源码前后 hash/mtime 核对与共享工作区串行依据。若失败只返回具体原因，不在编译阶段修代码或改资源。若 UP-TO-DATE 如实记录不要伪称实际重编。

不要改表格、资源库索引或主会话记录。明确游戏未测、未打包。最后必须返回：
<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"实际编译结果","changedFiles":[],"checks":[],"notRun":[],"writesSettled":true,"blocker":null,"nextAction":"表格写回者按有效编译结果迁待验收；用户游戏实测"}
<<<END_CURSOR_TASK_REPORT>>>
失败/阻塞则使用 failed/blocked，不宣称成功。
