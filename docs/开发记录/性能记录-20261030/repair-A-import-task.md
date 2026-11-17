# 修正唯一javac导入错误

继续作者A原会话，Cursor CLI --model grok-4.7-high-fast。这是明确步骤的单点返修，不是新设计/正式复审，不增加校验轮次。只做下面一处修改，不重新通读设计或其他作者代码，不跑命名脚本/测试/编译。

最新统一compileJava已经用真实C:/Users/baishang/.gradle和JDK17运行到javac，唯一错误是：
`D:/alianximod/src/main/java/com/bslx/XueXiMod/debug/performance/PerformanceRecorder.java:5`
当前import为`java.io.FileAlreadyExistsException`，该类应来自`java.nio.file.FileAlreadyExistsException`。

你唯一可写源文件是上述PerformanceRecorder.java；先保存修改前副本为本目录before-A-import-repair/PerformanceRecorder.java，再把这一条import改成正确包。不得改其他逻辑、格式、接口、源码、文档或制作表。不自行顺手优化，不扩大范围；若实际源已被别人修好就如实报告，不重写。

仅做定向diff确认这一处包名改变，保存本目录repair-A-import-final.md和repair-A-import-evidence.json，包含前后hash/最后写入UTC、修改位置、待统一编译、所有权释放。无编译授权，不运行gradlew，不启动游戏，不打包。完整最终返回。
