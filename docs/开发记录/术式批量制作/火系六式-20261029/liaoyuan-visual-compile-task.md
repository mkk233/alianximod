# 燎原两项视觉返修：最终统一编译

Cursor CLI --model grok-4.7-high-fast，指定既有编译者b2d5b359-42e3-4a90-9d24-b26acec6d5f8。只有原源码作者liaoyuan-visual-repair-current已经最终交回并声明停写后，主会话才启动本单。先读最新AGENTS/统一流程/max-dispatch。旧liaoyuan-visual-repair任务因引用过时需求已中断撤回，不能视为完成。

范围：本次fire_liaoyuan持续火场和附着燃烧视觉返修的最终源码编译；不是独立测试、实现复审或其他术式排查。设计2/2、实现3/3已耗尽；明确本次返修未追加复审，不能用seed-source-review的旧通过覆盖新字节。

输入：liaoyuan-visual-current-report.md、liaoyuan-visual-current-evidence.json、liaoyuan-visual-intake-report.md/json、liaoyuan-visual-return-report.md和记录.md最新所有权与停写记录。注意已经存在较新的docs/开发记录/火种共享燃料改造-20261029最终实现，旧13:08版本不是当前编译输入；以本次作者最终清单为准，并保留最新共享燃料改造。源码唯一作者1f4155a4-e537-4518-a3f7-893f09c5daff，表/记录唯一作者d6f84d0b-4ea1-4368-a576-8acc20cc2afd不写src。核对作者最终清单的实际hash/mtime、最终反馈停写声明和记录中的所有权。没有其他本批源码任务在运行。若发现实际源与交回证据不同或外部新写入，先返回具体阻塞，不能冒用旧编译。

上述收束条件满足后，授权在D:/alianximod执行 .\gradlew.bat compileJava --offline --no-daemon，沿用JDK17、-Xmx3G、daemon关闭，Gradle全工作区串行。不可改生产/测试源码，不边编边修；失败交回原作者。禁止build/jar/reobf/runClient、启动或操作游戏、提交。不新增/运行独立测试任务；本次如有资源元数据修改，可做格式解析与引用文件存在检查，不称为游戏验证或功能测试。

允许本批liaoyuan-visual-compile-report.md、liaoyuan-visual-compile-results.json、liaoyuan-visual-gradle.log、最终源码hash清单及Gradle正常输出。不得改表、记录.md、资源库索引或既有报告。

最终交回命令全文、开始结束时间、退出码、日志、是否晚于最后源写入、前后hash稳定性、检查和未跑验证。游戏效果由用户实测，本单不能声称现象已经游戏复现/修复验证。最后确认本次Gradle活动和写入停止，等唯一表作者安全写回。
