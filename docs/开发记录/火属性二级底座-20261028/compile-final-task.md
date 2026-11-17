# 返修收束后的统一编译与定向检查

Cursor CLI --model grok-4.7-high-fast。继续原编译会话4422b509-65f6-42c4-9a51-788a0978a6ba。主会话在唯一整合作者最终交回并停止写源码后才会派此单。若此单已被派发，当前源码已冻结；独立复审只读，可并行。不要自己改Java/JSON/语言/构建脚本，不做额外代码复审，不spawn。

读取repair-round1-final.md与repair-r6-final.md确定新增回归检查及最终接口。R6补完只改语言和死亡消息检查，其最终结果覆盖前者自报的基础句格式化缺口。保留此前验证日志，新的写verification/final/。

必须显式使用前次成功进入Java编译的JDK17.0.6和 GRADLE_USER_HOME=C:/Users/baishang/.gradle。本会话已知其真实JDK位置，不换回sandbox home；不联网、不下载wrapper或依赖。缓存异常即报告，不通过下载规避。

在D:/alianximod串行执行：
1. .\gradlew.bat compileJava --offline --no-daemon
2. 第1步成功后 .\gradlew.bat compileTestJava --offline --no-daemon
3. 使用本次Gradle编译出的main/test classes及必要现有runtimeClasspath，运行本批6个原main检查和本轮新增必要回归main。可以复用已有export-classpath.gradle；更新缓存class路径不得冒充新编译结果。不用gradlew test冒充执行main。

不要把旧508条重复跑多次累计成新用例数。分别列明每个检查断言数和结果；新回归实际覆盖R2交错伤害账本/R3元素调用隔离/R4伤害回调schedule和discard/R5主线程门的哪些层级（纯逻辑、结构或真实集成），不虚称游戏/织入通过。

如果compileJava失败，不编compileTestJava，报告精确错误并停止修改；可完成已经准备好的独立纯检查，但必须注明不等于完整编译通过。无论成功失败，返回每条命令退出码、警告、未跑项及最终源码未修改声明。

禁止build/jar/reobf/runClient/GameTestServer、打包或游戏启动。源码写入为0，唯一允许输出为本记录目录的verification/final日志/临时classpath脚本。
