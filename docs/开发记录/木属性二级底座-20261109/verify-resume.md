# 原统一验证任务续跑

保持verify-delivery原可写Cursor session、grok-4.7-high-fast。只在主会话带入A/C最终完成回执且相关写入全部收束后执行。首次验证a20261109102610151c7a4已最终failed，输出保留，不抹去失败历史。

本次主会话确认全部写入已收束：A最终a202611091041213362c99（工厂改名success，实例accepted保留）；C最终a20261109104121430efa0（检查常量/完整fixture命令、客户端MOD setup一次登记、SuihuaClientRegisterCheck），均reported_done/valid/writesSettled=true。当前无其他作者运行，正式授权补跑。C新登记入口检查的命令在最新整合交付.md，只测真实入口和注册表，不代表客户端Forge事件已实机派发。

本批实现静态复审已达3/3，不进行新一轮代码正确性审查，不调用其他会话重审。这里只执行原本授权的最终编译、修复后必要检查，并报告真实结果；发生失败仅定位编译器/检查给出的具体错误并交回作者，不自己修源码。

使用上次确认存在的本机离线缓存，当前进程GRADLE_USER_HOME=C:/Users/baishang/.gradle；JDK17在C:/Program Files/Java/jdk-17。不要改用户全局配置或settings.gradle。先确认无并发Gradle，在D:/alianximod执行原命令：`./gradlew.bat compileJava --offline --no-daemon`。

编译刷新主类后顺序补跑：
1. resources/run-resource-checks.ps1（上次被RemoveResult编译错误挡住）。
2. FireHurtBytecodeCheck，包含C最终交付列出的完整fixture/hook编译输入；补常量后需要真实java执行，不只javac。
3. KnownLimitsCheck和FireVisualLayoutCheck，上次因主编译失败未产出FireVisualLayout.class而未运行。
4. 若C提供了客户端岁华登记的可执行最小回归，运行并如实记录。游戏外环境若不能启动真实客户端MOD事件，明确限制，不启动游戏、不能把仅方法测试当成事件已经实机触发。
5. 对最新源码跑命名门禁。

未变的效果账本155断言、DamageEventSlots4项、Guard核心9项55断言已实际通过，保留首次结果，不无故重复。Guard协议项上次因游戏外Forge初始化失败UNEXECUTED，ModNetwork若未被本轮修改，无需重复同一已知环境失败，继续准确标为未验证。不要把之前的失败编译或未执行项拼成全量通过。

输出仍限TaskFile构建/检查目录与编译与检查结果.md。更新结果文档区分首次失败、这次补跑、保留的既有通过、环境无法执行、游戏未测。最终报告writesSettled=true；若完成就reported_done，明确最后返修未追加静态复审，仅报告编译和实际检查。禁止打包/runClient/游戏/源码修改/扩大审查。
