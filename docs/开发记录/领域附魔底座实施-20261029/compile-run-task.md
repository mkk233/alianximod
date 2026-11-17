# 执行最终统一编译 非只读启动

Cursor CLI --model grok-4.7-high-fast。本次以可执行写入的agent模式启动，专门允许Gradle正常构建输出/缓存和本目录compile-evidence。上一会话使用ask只读模式，因此compile-final.md报告命令未发出；该结果不是编译失败也不是编译通过。本次已有完整编译授权，不再询问用户。生产/测试源码仍禁止修改。

承接已读收束资料，先读本目录compile-final.md（前次已结束的只读预检结果）、compile-task.md（本次准确命令和范围）、source-freeze-metadata.json及记录.md末尾。不要再作源码正确性检查，不反复读取全部历史源码/报告；设计2/2、实现3/3均用完，禁止追加复审。

在 D:/alianximod 重新做必要的最小进程/源码稳定性核对，排除自身启动链。此前IDE Gradle73124已确认idle，火系57000只写制作表，来源范围明确禁止src/Gradle；若当前状态变了按实际判断。源已收束且没有实际同工作区Gradle冲突时，写compile-evidence的src/main文件哈希和本次13路径元信息，然后实际执行唯一命令：

`.\gradlew.bat compileJava --offline --no-daemon`

保持 org.gradle.jvmargs=-Xmx3G 和daemon关闭，不改配置，不打包，不运行测试或游戏，不改制作表，不提交。编译前后记录源内容是否变化、起止时间和原始退出码；输出证据保存本目录compile-evidence。代码失败只返回诊断，不边修边编。如果源码在窗口变化，就如实标此次不能作为最终编译，不自动再跑。

最终简洁返回：命令实际执行与否、退出码、是否晚于末次相关源码写入、编译前后哈希是否一致、失败诊断（如有）、未跑游戏/测试。不要用当前外层agent任务exit0冒充Gradle exit0。写入只限构建产物和明确证据，不受上一次会话的只读启动模式限制。
