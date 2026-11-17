# 水幻形非视觉阶段统一编译

结果：**失败**。`compileJava` 实际执行后失败，退出码 **1**。这不是 UP-TO-DATE，也不能称为非视觉阶段编译通过、完整术式交付或游戏实测通过。

源码在编译前后一致（1256 个文件，SHA256 差异 0）。这次失败覆盖的就是这次冻结的 `src`。没有重编，没有改源码。

## 命令与环境

- 工作目录：`D:/alianximod`
- 命令：`.\gradlew.bat compileJava --offline --no-daemon`
- 实际进程：`cmd.exe /d /c .\gradlew.bat compileJava --offline --no-daemon`，标准输出和标准错误写入日志
- `JAVA_HOME=C:\Program Files\Java\jdk-17`
- `GRADLE_USER_HOME=C:\Users\baishang\.gradle`
- 进程 PATH 前缀：`C:\Program Files\Java\jdk-17\bin`
- 启动前 `java -version`：17.0.6+9-LTS-190
- 构建日志内 `Configure project`：Java 17.0.6，JVM 17.0.6+9-LTS-190（Oracle），amd64
- `gradle.properties` 仍是 `org.gradle.jvmargs=-Xmx3G`、`org.gradle.daemon=false`，本次未改
- 日志首行说明按该 JVM 设置派生一次性 Daemon，构建结束停止。Gradle 文档链接指向 8.8
- 编译前没有 `java.exe` / `gradle.exe`。JDK 17 与用户 Gradle 缓存目录都存在。离线配置能进入 `:compileJava`，没有依赖下载阻塞

## 时间

| 项 | 时间（+08:00） |
| --- | --- |
| `src` 最后写入 | 2026-11-02T12:41:59.6624543，`src/main/java/com/bslx/XueXiMod/client/CloneAfterimagePlayback.java` |
| 编译前快照 | 2026-11-02T12:44:34.6104072 |
| 命名门禁 | 2026-11-02T12:44:41.5953563 至 2026-11-02T12:44:44.2179240 |
| compileJava | 2026-11-02T12:45:03.8173112 至 2026-11-02T12:45:30.4777888 |
| 编译后快照 | 2026-11-02T12:45:55.3538572 |

编译开始晚于最后一次 `src` 写入。

## 命名门禁

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 **0**。

输出：源码命名可读性检查通过：未发现单字母变量或禁止缩写。

日志：`docs/开发记录/水幻形术式-20261102/compile-nonvisual-name-gate.log`。门禁通过不改变编译失败。没有因门禁改源码。

## 源码身份

- 前：`docs/开发记录/水幻形术式-20261102/compile-nonvisual-before.json`，1256 个文件，路径 / 大小 / SHA256
- 后：`docs/开发记录/水幻形术式-20261102/compile-nonvisual-after.json`，1256 个文件
- 对比：新增 0，删除 0，大小或 SHA256 变化 0
- 最后写入路径在前后快照中相同

派发说明本阶段相关作者已停写。批次记录写明 A/B/C/D 已停写，且编译前已协调的相关聊天为 idle。本次编译者没有再改这些文件。

## 编译状态

日志原文（控制台代码页 936）：`docs/开发记录/水幻形术式-20261102/compile-nonvisual.log`（16096 字节）。同一字节按代码页 936 转成 UTF-8 的可读副本：`docs/开发记录/水幻形术式-20261102/compile-nonvisual-log-utf8.txt`。

- `:downloadMcpConfig` 执行
- `:extractSrg` UP-TO-DATE
- `:createMcpToSrg` UP-TO-DATE
- `:compileJava` 实际执行后 **FAILED**
- `4 actionable tasks: 2 executed, 2 up-to-date`
- `BUILD FAILED in 25s`
- 退出码 1
- javac 汇总：**4 个错误，41 个警告**

## 错误

1. `src/main/java/com/bslx/XueXiMod/network/SkillUsePacket.java:632`  
   已在方法 `executeSkill(ServerPlayer,Skill,ItemStack,int,double,Double)` 中定义了变量 `captured`。  
   `SkillChainPlan.ModifierPlan captured = plan;`

2. `src/main/java/com/bslx/XueXiMod/shu/technique/release/capture/ActorCaptureDispatcher.java:152`  
   找不到符号 `UUID`。位置：类 `ActorCaptureDispatcher`。  
   `UUID.randomUUID()`

3. `src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/ClonePresentation.java:78`  
   模式类型 `ServerLevel` 是表达式类型 `ServerLevel` 的子类型。  
   `if (!(server.getLevel(state.dimension) instanceof ServerLevel level))`

4. `src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/ClonePresentation.java:115`  
   模式类型 `ServerLevel` 是表达式类型 `ServerLevel` 的子类型。  
   `if (!(server.getLevel(dimension) instanceof ServerLevel level))`

41 条警告是既有过时 API 的 removal 警告，随失败汇总一起出现。本次没有把警告当通过。

## 未做

未 `build`、`jar`、`reobf`、发布、`runClient`，未启动游戏。未改源码、资源、构建配置或制作表。未修这 4 个错误，未再次编译，未做额外复审。

用户尚未确认 Steve v2 水线外观。本阶段范围内，术式正式 `PLAYER` 注册、专项声明和具体外观仍未接线。即使编译通过，也只够称为非视觉阶段编译通过；这次没有通过。
