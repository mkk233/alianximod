# 2026-11-04 游戏反馈返修：最终统一编译
固定Cursor CLI --model grok-4.7-high-fast，原compile-final session。你是主会话明确指定的唯一编译执行者。本单TaskFile及本Prompt在记录.md写入源码收束和放行后才Resume；未放行不得开编译。
用户明确要求处理动作/第一人称/协同距离，编译是本单必要交付，AGENTS第3节由Cursor执行。主会话不跑Gradle，源码作者无编译权；这不限制本指定编译单。
只做编译、资源同步、前后文件快照、实际命令证据；不是实现复审，不重读业务代码。设计2/2、实现3/3均用完，返修未追加复审。不改Java/运行资源/世界配置/制作表/启动器。不得kill游戏或daemon、删除缓存、安装环境、修改自动审批设置。
在D:/alianximod最小确认共享作者已停写、无Gradle/runClient锁竞争。若游戏仍占用，报告实际进程和锁，停止等待用户正常退出；不假定昨日退出授权代表今日没有运行。无占用才拍src前快照（含文件lastWriteTime和SHA256），执行命名门禁。
临时进程env使用已知Java17：JAVA_HOME=C:/Program Files/Java/jdk-17，GRADLE_USER_HOME=C:/Users/baishang/.gradle，PATH前置jdk17/bin，不改全局环境。沿原org.gradle.jvmargs=-Xmx3G、daemonfalse。
明确授权在项目根串行执行两条：
1. .\gradlew.bat compileJava --offline --no-daemon
2. 仅1成功后 .\gradlew.bat processResources --offline --no-daemon
调用短而透明，检查/命令/整理日志分开走正常自动审批。不build/jar/reobf/runClient，不启动游戏。审批若拒绝，保留实际reason原文交回；不得换工具/隐藏/绕过。
成功失败都采src后快照，变动则不能作为最终覆盖；编译失败只报告错误精确路径行号交原作者，不自己修、不无变化重跑。报告实际exit、BUILD结果、执行或UP-TO-DATE、warnings数量、编译起止时间、最后源码写入时间、前后内容变化。
成功后核对批准PNG（58E741C8BED808EB4E8CC6DC77FA71FD009A9AAB0B7FEBC104ACCB68E655BFD7）、actions/cooperative_v1两JSON及V最终报告里实际模型说明资源、适用lang资源在src/build/resources/main的SHA一致。v2是代码渲染壳，不把bbmodel或示意假人当运行模型。不得改资源以匹配核对。报告未游戏验证。
输出TaskFile指定的compile-gameplay-20261104-report.md、编译日志/资源日志/门禁日志、before/after快照，不覆盖2026-11-03成功历史。合法CURSOR_TASK_REPORT必须如实reported_done/blocked/needs_input/failed及writesSettled，失败不能算完成，成功也不代表游戏验收或新增复审。

共享编译范围追加：先读 docs/开发记录/术式批量制作/水流组合-20261103/实测返修-20261104-水流/编译交接.md。另一聊天已明确三文件停写并不再启动Gradle，授权本统一编译覆盖双方最终字节。前后快照额外列出该交接三文件SHA与约定一致、mtime，供其取实际编译证据；不做水流业务复审，也不改它们。若发现其他源码作者仍活跃或SHA偏离，先报告而非写成最终通过。
