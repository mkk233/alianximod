# 水幻形非视觉阶段：仅修本次javac错误
固定Cursor CLI `--model grok-4.7-high-fast`，仅实施明确编译错误修复，不新开设计/实现复审，设计2/2、实现2/3不变。读本批 compile-attempt-1/compile-nonvisual-report.md 和实际日志定位（原日志也仍在主目录）。禁止Gradle、自行重编、打包/游戏、改表/资源/正式登记。相关作者已停写。只写指定文件和自己的报告，保留既有改动，禁止reset/clean/checkout。命名门禁可执行，只修本次文件。最终一次性交回准确变更/未编译/停止写入。

A唯一源码：clone/core/ClonePresentation.java，报告fix-compile-a-report.md。
javac在78、115行报：instanceof ServerLevel模式类型与表达式静态类型相同（JDK17）。只把这两处换成合法的局部赋值与必要条件，保留原null、服务器会话、线程/维度等语义，不删原本有意义的防御分支，不改通知字段或发布时序。不得为了编译改运行效果、A以外文件或构建语言版本。

