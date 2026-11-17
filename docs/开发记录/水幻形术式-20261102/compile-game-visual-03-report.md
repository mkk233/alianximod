# 水幻形用户实测返修 03：诊断编译

结果：**失败**。这是 03b 诊断版的 `compileJava`，不是缺臂修复。退出码 **1**，`BUILD FAILED in 20s`。`:compileJava` 实际执行后失败，不是 UP-TO-DATE。诊断类没有生成。缺臂仍未修复，也不能把这次称为游戏效果已经一致。

本批此前没有这组同名编译报告，所以没有另建 attempt 目录。02 的报告是更早的历史定位，不代替 03b。

## 命令与环境

- 工作目录：`D:/alianximod`
- 命令：`.\gradlew.bat compileJava --offline --no-daemon`
- 实际进程：`cmd.exe /d /c .\gradlew.bat compileJava --offline --no-daemon`
- `JAVA_HOME=C:\Program Files\Java\jdk-17`
- `GRADLE_USER_HOME=C:\Users\baishang\.gradle`
- 进程 PATH 前缀：`C:\Program Files\Java\jdk-17\bin`
- 启动前 `java -version`：17.0.6+9-LTS-190
- 构建日志 `Configure project`：Java 17.0.6，JVM 17.0.6+9-LTS-190（Oracle），amd64
- `gradle.properties` 仍是 `org.gradle.jvmargs=-Xmx3G`、`org.gradle.daemon=false`，本次未改
- 日志说明按该 JVM 设置派生一次性 Daemon，构建结束停止
- 编译前没有 `compileJava`、`processResources` 或 `GradleWrapperMain`。有一个由 IDE 拉起的空闲 `GradleDaemon`（pid 50884），命令里没有本仓库路径。没有结束它，本次用 `--no-daemon`
- 没有读取或输出游戏命令行、accessToken。没有热替换，没有向客户端发重载，没有启动游戏

## 时间

| 项 | 时间（+08:00） |
| --- | --- |
| `src` 最后写入 | 2026-11-03T00:10:30.9327496，`src/main/java/com/bslx/XueXiMod/client/WaterPhantomVisualDiagnostics.java` |
| 编译前快照 | 2026-11-03T00:14:27.6433103 |
| 命名门禁 | 2026-11-03T00:14:34.5689272 至 2026-11-03T00:14:38.4968150 |
| compileJava | 2026-11-03T00:14:47.6318431 至 2026-11-03T00:15:09.2353586 |
| 编译后快照 | 2026-11-03T00:15:34.9900745 |

编译开始晚于最后一次 `src` 写入。03b 报告写明当时没有编译。

## 命名门禁

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 **0**。

输出：源码命名可读性检查通过：未发现单字母变量或禁止缩写。

日志：`docs/开发记录/水幻形术式-20261102/compile-game-visual-03-name-gate.log`。没有为门禁改源码。

## 源码身份

- 前：`docs/开发记录/水幻形术式-20261102/compile-game-visual-03-before.json`，1264 个文件，相对路径 / 大小 / SHA256 / 最后修改时间
- 后：`docs/开发记录/水幻形术式-20261102/compile-game-visual-03-after.json`，1264 个文件
- 对比：新增 0，删除 0，大小或 SHA256 变化 0
- 源码未变。这次失败覆盖的就是这份冻结源码。没有再编译

## 编译状态

日志原文（代码页 936）：`docs/开发记录/水幻形术式-20261102/compile-game-visual-03.log`（1899 字节）。同一字节的 UTF-8 副本：`docs/开发记录/水幻形术式-20261102/compile-game-visual-03-log-utf8.txt`。

- `:downloadMcpConfig` 执行
- `:extractSrg` UP-TO-DATE
- `:createMcpToSrg` UP-TO-DATE
- `:compileJava` 实际执行后 **FAILED**
- `4 actionable tasks: 2 executed, 2 up-to-date`
- `BUILD FAILED in 20s`
- 退出码 1
- javac 汇总：**1 个错误**。日志没有警告条数

错误：

`src/main/java/com/bslx/XueXiMod/client/WaterPhantomVisualDiagnostics.java:11`  
找不到符号。符号是类 `Util`，位置是程序包 `net.minecraft.util`。  
`import net.minecraft.util.Util;`

`build/classes/java/main/com/bslx/XueXiMod/client/WaterPhantomVisualDiagnostics.class` 不存在。

## 资源输出

03b 没有改确认贴图和网格。`build/resources/main` 与 `src` 哈希相同，输出时间不落后：

| 文件 | 字节 | SHA256 | src 时间 | 输出时间 |
| --- | ---: | --- | --- | --- |
| `textures/entity/xueximod/steve_water_lines.png` | 2687 | `9e7e5b01a7e09c393d3ffa1800468bd61ae919f68cf628152b00d9c347793da5` | 15:56:44 | 2026-11-02T23:39:49 |
| `textures/entity/xueximod/water_flakes.png` | 608 | `cefdee9e3e3fb7f701e19fc9cb3438c586c4841d3a50e3f8c9399a635bd4d70c` | 15:56:44 | 2026-11-02T23:39:49 |
| `models/effect/water_phantom_steve_v21/lines.bin` | 60666 | `26ef5093f3fc9cad03272f7bdfad06df771953e161b69310b2ff7e37b89f9025` | 15:56:44 | 2026-11-02T23:39:48 |

`compileJava` 失败，授权里的 `processResources` 只在编译成功后执行，所以没有跑。也没有手改构建产物。

## 诊断日志还不能采

作者指定的标记是 `WATER_PHANTOM_VISUAL_DIAG_03B`。编译通过并完全重启开发客户端之后，才到 `D:\alianximod\run\logs\latest.log` 搜索。该文件现在在，但这次没有编出诊断类。不要用 F3+T，也不要只重进世界来等这次失败编译的标记。当前客户端里不会出现这版诊断。

## 未做

未 `processResources`、`build`、`jar`、`reobf`、发布、`runClient`，未启动或操控游戏。未改源码、资源、构建配置或制作表。未修这个编译错误，未追加正式复审。设计 2/2、实现 3/3 沿用。缺臂未修，游戏效果仍待以后的用户复测。
