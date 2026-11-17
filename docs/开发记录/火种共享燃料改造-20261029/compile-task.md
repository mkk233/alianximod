# 共享燃料最终编译与必要验证

固定模型 Cursor CLI --model grok-4.7-high-fast。你是本轮唯一统一编译执行者。此任务仅在主会话确认源码作者最终交回且已停止写入后启动；不得自行对正在写入的源码编译。

读取本目录implement-report.md、implementation-evidence.json以及作者最后补充的完成报告，获取本版改动文件、最后写入时刻、检查入口与限制。不通读源码或开正式复审，不改变游戏行为。本次额度承接设计2次、实现4次，本单不增加正式检查轮次，也不另设测试阶段。

在D:/alianximod串行执行 .\gradlew.bat compileJava --offline --no-daemon。明确固定 JAVA_HOME=C:\Program Files\Java\jdk-17 和 GRADLE_USER_HOME=C:\Users\baishang\.gradle，沿用-Xmx3G及daemon关闭。不修改Gradle配置、不下载新依赖。

编译前确认没有其他Gradle或源码写入，记录作者列出文件的哈希，结束后再次核对。若发现外部写入，报告版本漂移而不混用结果。

编译通过后，执行作者交回的本次必要共享燃料定向检查入口，使用本次Gradle实际生成的main classes与新的检查输出目录，不能使用旧check-classes。如果检查类需要单独javac，按现有classpath方法执行。全量compileTestJava历史被SkinAttachmentChecks旧错误阻塞，本单不重复无关全量测试，不修该旧文件。

必要执行范围以本次作者实际准备的检查为准，覆盖共享扣款、同tick足额/不足分配、补种、引爆竞争、重复回调、焚天期限与旧存档兼容。不要为了凑覆盖自行重写一套算法或新增大量测试；缺真实实体/游戏验证直接写明。编译或相关检查失败时记录真实错误，停止并交回作者，不边编边改生产源码。

唯一允许写入本目录verification/、compile-report.md，以及正常项目build和已有Gradle缓存输出。生产源码、主设计文档、制作表、旧批次记录均只读。不build/jar/reobf、不runClient、不启动游戏、不提交。

最终反馈：命令全文、JDK/Gradle环境、起止时间、退出码、编译是否实际执行或UP-TO-DATE、源码是否晚于最后写入且前后哈希一致、定向检查入口及结果、未执行内容、真实阻塞。明确全部进程结束、无源码修改。不要把执行成功说成本版通过额外正式复审。
