# 火系六式统一编译任务（只在主会话显式派发后有效）

固定Cursor CLI --model grok-4.7-high-fast。本单只有经主会话实际启动才授予统一编译权限；文件存在本身不是开始信号。你是本批唯一编译者，禁止源码/语言/制作表/资源修改。没有独立实现复审授权，不开启其他会话或代理，不重置既有设计/实现2/2。

启动后先读本批记录.md、implement-report.md、visual-integration/remaining-visual最终反馈，以及known-limits-report.md、furnace-atomic-report.md和最后一份代码作者变更清单。主会话只有在所有相关作者完成并明确释放文件占用后才启动本单。记录三类证据：文件所有权清单；这些路径编译前LastWriteTime与hash；本批作者退出和停写声明。若缺证据或发现仍在写入，返回阻塞，不编译。用户已有改动全部保留。

固定环境：JAVA_HOME=C:/Program Files/Java/jdk-17，GRADLE_USER_HOME=C:/Users/baishang/.gradle；PATH仅按需前置该JDK/bin，不覆盖其他环境内容。不得使用隔离空缓存、不得下载wrapper或依赖、不得改Gradle配置。实际在D:/alianximod执行 .\gradlew.bat compileJava --offline --no-daemon；沿用org.gradle.jvmargs=-Xmx3G。禁止build/jar/reobf/runClient/启动游戏。

失败返回具体错误给唯一作者，禁止边编边修。通过后按作者列出的本批有意义定向检查实际执行，确保使用本次main classes与新检查输出目录，不沿用旧产物。共享底座的本次变更所真正影响的既有检查同样运行，不扩大至无关代码。此前既有SkinAttachmentChecks.java使全量compileTestJava失败20处；若本批未改这个文件，不为重跑已知无关错误反复启动全量测试，单独编译运行本批必要入口并如实说明未通过全量测试编译。不得额外生成“复审”或批量新测试来绕过额度。

为交付实测准备，可只读采集现有的六式学习/装配/手持物品前置，以及给目标准备火种的现有测试入口；不要新增技能书、获取途径、生产绑定或调试命令。若现有正常玩法没有产火种途径，或没有可直接给用户使用的测试入口，明确报告，不能编造游戏命令。将各项按“编号/名称、前置、步骤、预期”四行写在compile报告的实测部分，预期照完整N及已确认视觉，用户反馈空白。这是操作信息采集，不作额外代码正确性审查。

最终写compile-report.md及必要日志：全部实际命令全文、退出码、检查断言/范围、环境、源文件hash/mtime；确认编译后相关文件hash未变化，编译晚于最后写入。列出未跑项：游戏实际Mixin应用/持续伤害/炉心生命周期/视觉/关系/击退等。区分执行结果与正式复审，不说实现已复审通过。表格状态不由你更新。
