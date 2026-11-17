# 统一编译与必要定向检查（收到本单才执行）

本单由主会话确认三个作者已结束且源码写入收束后派发。唯一模型 Cursor CLI --model grok-4.7-high-fast。阶段：统一编译与验证，不做正式额外代码复审，不spawn其他agent。

只读主设计及同目录 implement-seed-final.md / implement-furnace-final.md / implement-combat-final.md 的最终交回，确定本次新检查入口。此前输出不是编译通过证据。本单不修改Java/JSON/构建配置，不顺手修代码；错误交回主会话安排对应作者。若其他合法修复使本单基线改变，报告不能视为最终编译。

在 D:/alianximod 串行运行：
1. .\gradlew.bat compileJava --offline --no-daemon
2. 第1步成功后 .\gradlew.bat compileTestJava --offline --no-daemon
3. 实际执行本批新增的main Check。项目没有JUnit，gradlew test不等于执行main检查。按当前已有运行方法组装classpath；必要时在本记录目录写临时init.gradle只导出test runtimeClasspath，不改项目build.gradle。不要引入网络依赖或下载、不启动GameTestServer/游戏。

检查至少覆盖新模块纯逻辑/数据契约的已写用例；不能为了声称全通过跳过失败检查。如普通JVM不能启动Forge依赖的集成类，明确环境限制；能运行的纯逻辑与结构检查仍完成。

只允许在 docs/开发记录/火属性二级底座-20261028/verification/ 写编译日志、执行结果、必要临时classpath/helper脚本。每条命令记退出码、用例数量、失败详情。若编译失败，报告首个相关错误及其路径，不自己返修，不以当前其它历史改动为借口丢弃用户工作。

禁止build/jar/reobf、发布、runClient、游戏启动。最终简洁报告：实际跑过命令与退出码、实际执行的检查数、未跑/阻塞、未游戏验证。
