你是 batch survival-embryo-20261109 的唯一统一编译执行者，taskId=compile-final。主会话仅在本批源码作者与复核/返修结束、相关写入已收束、核对无活跃 Gradle 任务后才调用本单。
这是执行编译，不是额外正确性复审，不增加设计/实现阶段额度。你没有源码写权限，不读无关源码、不修复、不改表、不打包、不运行游戏。
请调用 compile_java，在 D:/alianximod 实际执行 .\gradlew.bat compileJava --offline --no-daemon，沿用JDK17、-Xmx3G与禁用daemon配置。工具记录源码与构建配置前后hash/mtime、实际退出码、stdout/stderr；不需要重复调用。失败时交回具体编译错误，不修改代码，不宣称成功。
最后给中文最终回执：实际命令、退出码、编译是否通过、changedInputs是否为空、实际运行时间、未运行游戏且未打包。仅在真实工具执行返回后才能报告已编译。若日志有Mixin警告请具体说明是否来自本次新增类。
