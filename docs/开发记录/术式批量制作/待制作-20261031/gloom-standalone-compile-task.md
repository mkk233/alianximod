# 幽冥独立释放返修最终编译

仅在主会话拿到D的gloom-standalone-repair-report.md/json最终停写反馈后派发。继续唯一编译者68ccd641-cc64-4259-8e72-633942ec39a7，显式Cursor CLI --model grok-4.7-high-fast，WriteMode。设计2/2实现3/3耗尽，只有必要机械编译，无正式复审，不读源码作逻辑判断、不spawn、不修源码。

此前整批收尾被用户明确终止，隔离续编译未执行，13项未迁。此次用户明确授权仅修幽冥独立释放，D是唯一修复源码作者，已停写，A/B/C停止。用户已明确“修改已结束，当前没有源码写入”，13:03的命令源码改动已确认完结，不再将它作为未知占用或重复问用户。旧源码hash是历史；本次以D最终交回和实际当前完整源/配置采新基线，保留用户全部外部改动。主会话附新其他聊天状态信息。只对当前写者/mtime/文件版本/构建环境作机械采集，不用旧快照差异制造新的不必要停写阻塞。

读取AGENTS/调度技能/统一流程及gloom-standalone-repair-report/ownership。校验D交回hash与磁盘相符，记录其mtime，确认本批作者均停写、没有实际活动的外部源码作者；采当前src/main、src/generated/resources及实际构建配置完整逐文件输入清单。若D交回后源码正在变化/有实际作者占用则如实停编，不假报稳定。

编译授权为 `.\gradlew.bat compileJava --offline --no-daemon`，现有org.gradle.jvmargs=-Xmx3G和daemon=false，JDK17 C:/Program Files/Java/jdk-17、GRADLE_USER_HOME C:/Users/baishang/.gradle（仅本命令环境，避免Cursor注入临时空缓存）。同一真实项目Gradle必须串行。先机械判断当前原D:/alianximod是否有活动Gradle任务或运行客户端，不把没有任务/子进程的空闲daemon误当作阻塞。

原项目没有活动Gradle任务时，在D:/alianximod根执行所需compileJava。若仍有IDE runClient/游戏占着原Gradle，沿已授权isolated-final-compile-task.md的环境隔离方案，在用户Temp新建唯一不存在的独立项目目录，复制全部真实构建输入（Java/资源/generated资源/gradle文件/必要本地依赖）同字节编译。先检查构建输出不写回原目录，原项目和副本hash相同；编译前后原输入不变且和副本一致。当前旧日期快照差异不阻塞采新最终基线。不要结束用户游戏，不要要求再次关客户端，不改原配置、不复制活跃.gradle/build/run/存档/git/idea，不递归删除/移动，不联网补依赖、不强行解缓存锁。临时目录确认绝对路径在用户Temp内，真实cwd记入报告；隔离时不能声称原build已更新。

只运行compileJava，不build/jar/reobf/datagen/runClient/测试/游戏/发布/提交。编译失败仅交回完整编译器诊断与归属，不边编边改。命名门禁按D报告如需运行可执行，历史OriginalHumanoid旧违规不扩大修复。本轮不额外代码审查。编译后输入稳定性机械核对必须覆盖最终字节，若有新变动不得套用成功。

允许写本批gloom-standalone-compile-*证据文件、编译所需build/.gradle输出（若隔离仅副本输出）。不写Java/资源配置/工作簿，不改原记录。输出gloom-standalone-compile-report.md/json、独立完整日志与输入前后清单，写真实命令、cwd、开始结束/退出码、源最终mtime与hash一致性、已做/未做/阻塞、未游戏/打包。真实退出码0方写成功，成功也只是编译不代表游戏验收。最后停止写入回最终反馈。
