# 水幻形非视觉阶段统一编译（指定唯一编译者）

仅收到主会话派发时执行，代表本阶段相关作者已停写且当前集中复核/指定返修已交回。固定 Cursor CLI `--model grok-4.7-high-fast`，不降档、不spawn。不改源码、资源、构建配置或表，不修编译错误、不触发额外复审。

目标：验证当前非视觉阶段最终源码可以compileJava，记录准确证据。用户尚未确认Steve v2水线外观时，术式正式PLAYER注册、专项声明与具体外观仍待接线；本次通过只能叫非视觉阶段编译通过，不能叫完整术式交付或游戏实测通过。

先读AGENTS.md/现行流程与本批编译环境记录。项目根 D:/alianximod。同工作区Gradle串行，发现已有相关Gradle编译进程则报告并停止，不杀进程不并发。默认不build、jar、reobf、发布、runClient或游戏。

必须在本次编译进程设置真实已有环境（不能沿用Cursor临时Gradle缓存）：
- JAVA_HOME = C:/Program Files/Java/jdk-17
- GRADLE_USER_HOME = C:/Users/baishang/.gradle
- 当前org.gradle.jvmargs=-Xmx3G，daemon关闭；不改这些配置。

先保存src下所有文件的路径/大小/SHA256为本批 `compile-nonvisual-before.json`；记录最后源码写入时间。运行 `powershell -NoProfile -File tools/check-name-gate.ps1`，保存门禁日志和退出码；不因为门禁问题改作者源码。

然后在项目根执行且只执行：
`.\gradlew.bat compileJava --offline --no-daemon`

保存完整输出 `compile-nonvisual.log`、起止时间、退出码、实际执行命令与环境。结束后保存src同范围 `compile-nonvisual-after.json`，对比是否一致。源码有变化则本次不作为最终覆盖，不自行重新编译；交回主会话协调。

写 `compile-nonvisual-report.md` 和 `compile-nonvisual-evidence.json`，记录真实成功/失败、UP-TO-DATE或实际compile状态、warning数量如可得、失败的准确文件行号、src身份一致性、未打包/未游戏/未正式注册与资源接线。不伪造通过，不用旧底座的编译结果代替。若环境阻塞如实返回，不联网下载或改缓存配置。任务结束后停止写入，最终一次性交回结果。
