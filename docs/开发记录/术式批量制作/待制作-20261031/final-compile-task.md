# 最终收束源码：命名检查与离线编译

实际派发上下文2026-10-31约05:52：全部A/B/C/D实现者及独立reviewer已最终停写。实现正式额度已3/3，设计2/2，禁止再审代码正确性或追加复审。最新证据implementation-review-round3-final.md确认四项A+C返修通过、无新直接相关阻断；implementation-round2-kindle-ruling.md撤销首轮B引燃n封8误读。最终实现author-a-review1-repair-report、author-c-hitclock-repair-report、此前A final-integration/D scorch-bridge/B report都齐。05:52前主会话重新list_threads仅本聊天active，其他同仓idle/notLoaded。当前批准你作为唯一编译执行者运行本单，不改src。必须先查同工作区Gradle无并发、记录前后完整src稳定清单。本单机械编译/资源检查不构成第4轮复审。

本任务只能由主会话在A/B/C最终交回、所有源码停止写入、当前阶段正式复审和已指派返修收束后实际派发。固定Cursor CLI --model grok-4.7-high-fast。阶段是统一编译与必要验证，不是独立测试或追加正式复审，不重置设计2/2与实现累计额度。主会话会在派发时给实际累计值和最终反馈路径。

允许只读D:/alianximod源码/资源/本批最终报告/本地JDK与Gradle缓存；只允许Gradle正常编译输出、命名门禁输出以及本批compile报告与证据文件写入。禁止修改Java、资源、构建配置、Gradle版本、制作表、记录.md；编译错误交回作者，不边改边编译。不build/jar/reobf/runClient、游戏、发布或提交，不下载替换工具链、不改model、不spawn代理。

开始前最小核对：当前没有同工作区其他Gradle进程或作者任务；记录src/main所有文件相对路径/大小/mtime/SHA256的稳定清单，含本次资源；记最终作者交回时间。若有作者运行/源码变化则停止编译并报告，不能把中间源码称为最终。读取本批最终所有权和已知命名门禁历史问题，避免把未改旧问题说成新增。

1. 在项目根运行现行tools/check-name-gate.ps1，记录完整命令/退出码/结果路径。只报告本批新增和旧例外；不能自行修任何源码。
2. 使用已安装正确JDK17、本项目现行Gradle配置和已有本地缓存，在D:/alianximod执行 `.\gradlew.bat compileJava --offline --no-daemon`。org.gradle.jvmargs应现行为-Xmx3G，daemon关闭。首次环境若为已知foojay/离线缓存问题，可以只选择本机已存在且项目历史已用的正确JAVA_HOME/GRADLE_USER_HOME，不更改构建文件，不联网补包。报告确切环境与选择依据；真实缺依赖则如实报阻塞。
3. 结束后重新取同份src清单，确认前后相同。如果源码在编译中变化，结果无效须回报待重新收束，不擅自多轮重编。
4. 不运行独立测试任务。仅作交付必要机械检查：本批语言/模型/blockstate/loot/tag JSON可解析、manifest确认图片实际复制hash一致；无新源码修复，不重复另做逻辑复审。明确这些不等于游戏验证。

报告 `final-compile-report.md/json`：命令/开始结束/退出码/日志路径、JDK/Gradle缓存/内存配置、前后源码清单hash、命名门禁新增vs历史、资源机械检查、编译通过/失败/环境阻塞、未跑的游戏/打包，最后声明停止全部操作。若编译失败，提取每个错误的精确路径/行号/错误文本与A/B/C所有权，供主会话交给作者；不要猜测修改后会通过。成功也不自行迁表。
