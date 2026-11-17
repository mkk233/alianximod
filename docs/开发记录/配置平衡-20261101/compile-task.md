固定 Cursor CLI --model grok-4.7-high-fast。本阶段：唯一最终编译执行者。只有主会话显式派发本单且本目录freeze.json和记录.md已有作者最终释放及编译授权时才生效。本单不是新正确性复审，不读全仓源码，不派代理，不修改源码/制作表/规则/配置/Gradle配置，不运行测试、build/jar/reobf/runClient或游戏，不提交。
工作区D:/alianximod。读取本批记录.md、freeze.json及最新作者/审查交付记录确认相关源码作者全部最终收束。只做必要串行检查，若本工作区已有Gradle运行则返回等待阻塞，不并发、不终止别人进程。
使用项目现有JDK17以及真实既有Gradle缓存。已知此前沙箱把GRADLE_USER_HOME重定向到空缓存导致离线解析失败；本机缓存是C:/Users/baishang/.gradle，可仅在本次进程显式设该路径，不改项目/全局配置，不重新下载工具。JAVA_HOME若不是可用JDK17，可只定位本机已有JDK17用于本进程，不安装或切项目目标版本。沿用org.gradle.jvmargs=-Xmx3G、daemon关闭。
授权执行的命令仅：
.\gradlew.bat compileJava --offline --no-daemon
日志保存本目录compileJava-offline.log，记录开始/结束时间、真实退出码及是否BUILD SUCCESSFUL。编译失败立即把完整必要错误交回，不边编边修；有历史警告如实区分。允许Gradle正常build输出和既有缓存写入。
最终stdout简明：实际命令/环境、本次真实退出码、通过或失败、日志路径，是否晚于最终源码冻结；未打包、游戏未测。不要声称命名门禁全仓通过；已知OriginalHumanoid未改文件两处旧命名问题不在本编译任务修复。

