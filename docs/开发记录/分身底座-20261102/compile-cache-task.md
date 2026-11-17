模型 Cursor CLI --model grok-4.7-high-fast。单一编译者，遵守 compile-task.md。当前授权在真实源码作者停写窗口执行一次最终编译，不得边写边编。

环境事实：docs/开发记录/底座释放参数返修-20261102/compile-report.md 记录 03:55 成功离线编译明确设置 JAVA_HOME=C:/Program Files/Java/jdk-17 和 GRADLE_USER_HOME=C:/Users/baishang/.gradle。docs/开发记录/燃烧空间挂载接入-20261101/compile-environment-report.md 曾记录 Cursor 默认临时 cursor-sandbox-cache 的 GRADLE_USER_HOME 导致同一 foojay 0.7.0 缺失；复用本机原有 .gradle 缓存后成功。

请只读核实当前 JAVA_HOME、GRADLE_USER_HOME，以及上述已有缓存中 Gradle 8.8、foojay 0.7.0 的存在；这属于本次构建环境，不是无关终端历史。仅当前编译进程设 JAVA_HOME=C:/Program Files/Java/jdk-17、GRADLE_USER_HOME=C:/Users/baishang/.gradle，并 PATH 前置该 JDK/bin。不得修改永久环境/构建文件，不复制/删缓存，不改仓库/插件版本，不安装下载依赖，不改在线重试，不读取先前被拒绝的终端历史。

先确认空间切割 repair-r1-retry（以及其后继任务）已停写、没有另一个 GradleWrapperMain/compileJava、自己仍是唯一编译者。只读复核本身不占写锁，但若实际返修作者尚在运行，不编译；报告占用返回。允许读该批调度/状态文件与进程元数据，不读他们实时工具流/中间源码。全部实际源码作者停写后保存 src/main 前快照，再在 D:/alianximod 运行一次 .\gradlew.bat compileJava --offline --no-daemon。保留 Xmx3G 和 daemon=false。编译结束保存后快照并记录是否有新写入，差异为 0 才能说明覆盖该次源码。若另一路新的作者启动，指出事实，不能宣称全工作区已彻底收束。

真实 Java 编译错误要给准确文件/行号/消息，但本单不修源码；返回原作者。最终写 compile-report.md/evidence.json、完整 compile-gradle.log、两份源码快照。先前 attempt-1/attempt-2 已归档，不覆盖。命名门禁只在最终快照执行并保存 naming-gate-final.log。无游戏、无打包、无测试、无表格、无提交、无额外复审。本单收到明确派发后才执行。
