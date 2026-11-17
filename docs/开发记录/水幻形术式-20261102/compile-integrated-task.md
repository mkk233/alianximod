# 水幻形 v2.1 最终集成编译（唯一编译者）

仅收到主会话实际派发时执行，代表所有本批作者已返回最终反馈并停止写入、其他相关聊天已完成。固定 Cursor CLI `--model grok-4.7-high-fast`，不降档、不 spawn。

本阶段只执行已授权编译和记录结果，不做新增正式复审，不修改源码/资源/构建配置/制作表，不修编译错误。代码错误交回作者。默认不 build/jar/reobf/发布/runClient/启动游戏/提交。实现已累计3/3，不能以编译任务追加正确性复审。

工作区 `D:/alianximod`。读取 AGENTS.md 和本批最新集成、修复报告。先用最小进程检查确认没有其他 Gradle 编译进程；如有则报告并停止，不杀进程、不并发。不得把之前非视觉编译当本次最终覆盖。

必须对本次编译进程设置已有环境：
- JAVA_HOME = `C:/Program Files/Java/jdk-17`
- GRADLE_USER_HOME = `C:/Users/baishang/.gradle`
- 保持现有 org.gradle.jvmargs=-Xmx3G、daemon关闭，不修改配置。不能沿用 Cursor 临时 Gradle 缓存。

先保存 `src/` 全部文件相对路径/大小/SHA256到本批 `compile-integrated-before.json` 并记录最后写入时间。运行已有 `powershell -NoProfile -File tools/check-name-gate.ps1`，完整保存 `compile-integrated-name-gate.log` 与退出码。不得为通过门禁在本任务改源码。

在项目根执行且只执行：
`.\gradlew.bat compileJava --offline --no-daemon`

保存完整 `compile-integrated.log`、起止时间、退出码、实际命令和环境。结束后保存相同 src 范围 `compile-integrated-after.json`，对比一致性；如期间源码变化，本次不能作为最终覆盖，不自行再编译，交回主会话。

写 `compile-integrated-report.md`、`compile-integrated-evidence.json`，明确实际 compile 或 UP-TO-DATE、warning数量、name gate状态、源码是否变化、成功或准确错误文件行号。成功记录仅代表编译通过；未打包、未启动游戏、游戏效果待用户实测。失败不伪造通过，环境阻塞如实报告，不联网下载、不改缓存、不擅自换模型。

如上述目标报告已存在，请先把前次本组编译记录完整留在新的 attempt 子目录，避免覆盖证据，然后记录本次。完成后停止写入、一次性交回最终反馈。
