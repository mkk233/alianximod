# 水幻形缺臂实际修复04：最终统一编译

固定 Cursor CLI --model grok-4.7-high-fast，原编译者6f4471df-b73a-4e09-84cb-54a4bc7c55b3。不spawn，不进行新增正式复审，不读源码重审，不改源码/资源/构建配置/制作表。沿用设计2/2、实现3/3，本阶段额度已用完，返修结果未追加复审。

本单实际派发表示D作者04b已交回最终报告并停写，主会话已确认另外两相关聊天idle/completed。先读 fix-game-visual-04b-report.md。本次是真实修复而非03诊断版：作者报告提前取水花Consumer导致共享Builder切材质，皮肤被画上水花贴图，手臂透明、同侧腿浅色；已按先drawSkin再getBuffer水花并drawFlakes修正，删临时诊断类和钩子。主会话未新增正式复审，不把编译通过称为游戏实测通过。

只编译最终冻结源、运行现有namegate、整理输出证据。先最小进程检查没有其它Gradle，若存在则停止报告，不杀进程不并发。不build/jar/reobf/发布/runClient，不启动/控制游戏。JAVA_HOME=C:/Program Files/Java/jdk-17，GRADLE_USER_HOME=C:/Users/baishang/.gradle，保持org.gradle.jvmargs=-Xmx3G和daemon关闭。不下载、不改全局配置、不用空缓存。

保存 compile-game-visual-04-before.json：src全部文件相对路径/大小/SHA256/LastWriteTime。跑 powershell -NoProfile -File tools/check-name-gate.ps1 保存 compile-game-visual-04-name-gate.log 和exit。

项目根 D:/alianximod 执行：
.\gradlew.bat compileJava --offline --no-daemon

保存 compile-game-visual-04.log 的完整输出、起止时间、exit、实际compile或UP-TO-DATE。之后保存同范围after快照。任何源码中途变化则不能作为最终覆盖，交回不擅自重编。编译失败也直接交回作者，不自己修。

检查build/classes最新修复相关类时间，已删除的WaterPhantomVisualDiagnostics是否仍残留输出，只报告，不手动删除构建产物。检查实际build/resources/main水幻形PNG和lines.bin对src哈希一致；若输出落后，本单授权compileJava成功后串行 processResources --offline --no-daemon，另留日志并重新快照。相同则不跑额外资源任务。

写 compile-game-visual-04-report.md、compile-game-visual-04-evidence.json：实际成功/失败、错误/警告、namegate、源快照变化、资源输出、诊断class输出清理状态、未游戏实测/未追加正式复审/未打包。保留所有03诊断及失败历史，不覆盖。

本单独占本批 交付与实测.md 文首状态补充（其它作者已停止）：根据04b最终报告和本次编译结果更新为“缺臂代码修复已交回、最终编译结果、待用户一次性游戏验证”，说明根因和诊断已移除，历史保留。只有编译成功才给出重启后检查双臂、双腿颜色及散水的最短步骤；失败不得叫用户验证。Java变化需完整重启当前开发客户端，F3+T/重进世界不能替换类。不自行操作游戏。

结束停写并一次性交回最终反馈。本批未匹配制作表行，表格本单仍不适用，不新增或迁移行。
