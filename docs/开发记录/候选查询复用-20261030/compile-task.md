# 步骤3最终统一编译

只有主会话确认所有相关源码作者最终返回并释放写入权、冻结清单完成且正式派发本任务后才生效。固定Cursor CLI --model grok-4.7-high-fast，工作区D:/alianximod，你是唯一编译执行者。不得新增正确性复审、修改源码/配置/制作表或启动其他代理。

读本目录记录.md、最新实现/修复最终报告、compile-ownership.json、compile-source-freeze.json。核对全部主Java和本次修改的构建输入资源路径/length/UTC ticks与冻结一致，编译开始晚于最后写入。本步B说明返修的备份/取hash shell被自动审批拒绝且未给原因；不要补做该备份或重试被拒绝的取hash操作。采用当前输入的只读文件清单和最后写入时间作为冻结依据，说明没有独立最终SHA256比对，不声称字节哈希已验证。检查当前工作区没有正在执行的Gradle；idle daemon不是忙任务，如真实占用则返回来源，不擅自取消。写入未收束不得编译。

在实际运行命令的同一个PowerShell进程中显式设置：JAVA_HOME=C:/Program Files/Java/jdk-17，GRADLE_USER_HOME=C:/Users/baishang/.gradle，PATH前缀为该JDK的bin；不要只依赖外层环境。Cursor默认空隔离缓存会导致foojay插件缺失，禁止以此下载依赖或改项目配置。保留-Xmx3G和daemon关闭，不修改HOME/USERPROFILE/CODEX_HOME或全局配置。

在项目根执行唯一 `.\gradlew.bat compileJava --offline --no-daemon`，稳定stdout/stderr文件重定向，记录真实退出码、环境、开始结束UTC和完整诊断。不要用会造成PowerShell输出采集崩溃的流式管道。编译结束再核对冻结清单的文件集合、长度和UTC ticks一致，只有退出0且冻结元数据未变才能报告通过。若编译命令自身被自动审批拒绝，保留原始reason并停止该动作；不得换工具/会话执行相同被拒绝动作，不去掉auto-review/加force，不要求主会话代跑。

不跑build/jar/reobf/runClient/独立测试/游戏，不提交、不修改制作表。失败只返回实际诊断和文件行号，交原作者修复，不边编边改。写本目录compile.log、compile-results.json、compile-final.md；如有旧尝试先以唯一目录保留失败诊断，不覆盖历史。完成后最终反馈并释放编译权。明确游戏/真实性能未测，编译不代表收益。

只有实际编译成功后，允许同步docs/候选查询复用说明.md中的验证状态一小段：最终源码通过上述离线compileJava，未打包未游戏/性能实测；去掉已过时的“尚未编译”或“作者B尚未接入”开发状态，不修改缓存规则和其他说明。这只是已验证结果的文档更新，不增加正确性复审。该文档原作者完成释放后此项写入权随本编译任务转给你。失败时不写成功状态，报告中说明文档是否更新。

第3/3轮review-resume-final.md已指出唯一文档收束项：docs/候选查询复用说明.md约第65行后半还称PerfMetric注释把射线留在TARGET_QUERY且该文件未改。实际B已同步PerfMetric和性能说明，这句应改成当前注释与实际边界一致（只包真实空间查询，不含距离选择/准星方块射线/锚点选择）。在上述文档状态更新时一并修正，不修改Java或恢复旧计时范围，不新增复审。

后续状态：上述第65行已由A按doc-close-task.md完成，见doc-close-final.md，写入权已释放。你执行编译时保留已修正文句，只更新第67–69行验证状态；不重复改策略/计时说明。
