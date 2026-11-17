# 焚天双层视觉编译回执

- batchId：`待制作-20261031`
- taskId：`fire_kindle_fentian_visual_swap`
- attemptId：`20261116-compile-fentian-dual-layer-02`
- 角色：本轮唯一编译者
- 直接父级：主会话 `01a1ce06-b681-7c90-b11d-260d3c169911`
- 模型：`cursor/grok-4.7@500k`，high。工具无 tier 参数，沿用客户端 Fast `service_tier="priority"`，未声称后端独立验证。
- 本阶段额度已用完，返修结果未追加复审。

## 命令

工作目录 `D:\alianximod`，只执行一次：

`.\gradlew.bat compileJava --offline --no-daemon`

- 开始：2026-11-16 11:53:34.142
- 结束：2026-11-16 11:54:01.180
- 退出码：`0`
- 结果：`BUILD SUCCESSFUL in 26s`；`4 actionable tasks: 2 executed, 2 up-to-date`
- `compileJava` 本次实际执行，不是 `UP-TO-DATE`。另外执行的是 `downloadMcpConfig`；`extractSrg` 与 `createMcpToSrg` 为 `UP-TO-DATE`。
- 原始日志：`D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031\fentian-dual-layer-compile-20261116.log`（cmd 重定向，代码页 936）。
- 没有改 JVM 或 Gradle 配置，没有 build、jar、reobf、runClient，没有启动游戏。编译通过不是游戏内验证。

## 开编前核对

没有发现 `java.exe`、`javaw.exe` 或 `gradle` 进程。一次过宽的命令行匹配只命中检查脚本自己的 `pwsh.exe`，因为脚本正文含有 `gradlew`；开编前改按进程名复查，确认没有 Gradle。

两个触点与实现回执一致：

- `FireVisualClient.java`：2026-11-16 11:49:22.390，52998 字节，`EEE7620485A157F67FAB14C3B55E84C7BEF60067168AA2739D38111633E33A2E`
- `FireVisualLayout.java`：2026-11-16 11:48:15.569，10934 字节，`9C57329F4057CE564DC3D2B8E1856559B4F4F33CAF9FAEFAE46945E892678284`

`src/main` 编译前的原始工具输出是：

- `COUNT=1488`
- `TREE=AE3C3A58BB1680064E8E5665FD0A2A35F4804126549E6747B9A12F86562A2009`
- `STARTED=2026-11-16 11:53:04.927`
- `FINISHED=2026-11-16 11:53:05.715`
- `NEWEST=2026-11-16 11:49:22.390|\java\com\bslx\XueXiMod\client\FireVisualClient.java`

上一版回执把编译后的 `CFD8C246...` 误抄进这一节，现予更正。真实编译前聚合值是上面的 `AE3C3A58...`。

该次命令对每个普通文件计算大写 SHA256，相对路径是去掉 `D:\alianximod\src\main` 前缀后删除开头的 `\` 或 `/`，再把 `\` 改为 `/`；各行按序数排序，以 LF 连接并保留末尾 LF，最后对 UTF-8 无 BOM 字节取 SHA256。逐行文本没有保存，所以这只是命令文本，不能事后复核每一行。

## 编译后

两个触点的时间、大小和哈希都没有变。对应 class 写于本次编译窗口内，晚于源码：

- `FireVisualClient.class`：2026-11-16 11:53:56.140，44213 字节，`F2D6423CF14E3BC065756D5747DE797636327636A1D0BC93E4ADFC127009D001`
- `FireVisualLayout.class`：2026-11-16 11:54:00.338，6485 字节，`7BC6A11A926C12F5ED9C3A29B03AA3773F617A1020C060806E32D2751A80A8D7`

因此本次编译覆盖了这两个最终触点。编译结束后没有残留 Java 或 Gradle 进程。

编译刚结束时的原始工具输出是：

- `AFTER_COUNT=1488`
- `AFTER_TREE=AE3C3A58BB1680064E8E5665FD0A2A35F4804126549E6747B9A12F86562A2009`
- `AFTER_FINISHED=2026-11-16 11:54:02.025`

这次相对路径只删除开头的 `\`，其余连接方式与编译前命令相同。两个聚合值不相等。前后都没有保存逐文件清单，因此不能列出变化路径，也不能用后来的任何重算代替 11:53:05 的编译前摘要。整棵 `src/main` 在编译窗口内是否字节一致：未知。这不改变两个触点编译前后哈希未变、对应 class 已在本次窗口写出，以及命令退出码为 0 的事实。

## 警告

日志有 54 条 javac `[removal]` 警告，加上两条过时 API 注记；没有错误。警告都是既有的 `ResourceLocation` 旧构造或 `setCanceled`，不在本次新增逻辑上。其中一条指向 `FireVisualClient.java:879`，那是原来的 `sheet` 助手，不是本次新写的调用。`FireVisualLayout.java` 没有警告。按编译任务限制，没有改源码，也没有加 `-Xlint` 重编。

## 收束

没有改 `src`、美术、制作表或批次记录。本段只更正同一回执中的聚合摘要抄录，没有重新编译。没有新增测试，没有下属，没有后台任务。写入收束。下一步由父方派唯一写表者。
