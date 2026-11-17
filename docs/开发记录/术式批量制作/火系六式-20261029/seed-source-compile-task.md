# 火种来源：最终统一编译

Cursor CLI --model grok-4.7-high-fast。本单仅由既有编译者 b2d5b359-42e3-4a90-9d24-b26acec6d5f8 执行。须在主会话派发本单时，所有源码写入已经收束。先读当前AGENTS/max-dispatch/统一流程，现行没有独立测试阶段，不沿用历史测试任务扩展本单。

读取已交回的 seed-source-implementation-report.md、seed-source-implementation-evidence.json、seed-source-review-final.md，以及主会话补充的返修最终产物（如果存在，以最新停写版本为准）、本批记录.md 所有权记录。源码作者唯一为1f4155a4-e537-4518-a3f7-893f09c5daff，复核者只读。表/记录作者不写src。编译前记录文件所有权、实际mtime和hash、所有已派源码作者的最终停写声明；源文件若不匹配最终证据或有新的外部写入，先报告明确差异，不冒用旧证据。

编译授权：相关源码最终停写后在 D:/alianximod 执行 .\gradlew.bat compileJava --offline --no-daemon。沿用 -Xmx3G，daemon关闭、既有JDK17/Gradle缓存。整个工作区Gradle串行。禁止build/jar/reobf/runClient、启动游戏和提交。生产源码与测试源码均只读。编译失败只交回作者，不边编译边改。

本单不派独立测试，不编译/运行测试集合，不生成测试文件。可对本次改动的语言JSON做语法解析这一必要资源检查；不能把编译/JSON解析称为功能测试或游戏验证，也不追加实现复审。

允许写本批 seed-source-compile-report.md、seed-source-compile-results.json、seed-source-gradle.log、最终源文件hash清单和本单专用证据，以及Gradle正常生成的输出。禁止改制作表、记录.md、源文件、既有报告、资源库索引。编译交回后由唯一表记录者实际同步。

最终报告命令全文、开始结束时间、退出码、日志路径、是否晚于最后源码写入、最终源码前后hash稳定情况、JSON结果、未跑验证。清楚区分本轮已执行编译与审查结论；实现第3轮之后的作者返修（如果有）未追加复审，不能再审它或重置计数。最后停止所有Gradle活动和本单写入。
