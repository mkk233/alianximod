# 分身底座 2026-11-02 统一编译报告（使用本机 Gradle 缓存）

结论：已经用本机原有缓存执行一次 `.\gradlew.bat compileJava --offline --no-daemon`。`:compileJava` 实际执行，不是 UP-TO-DATE。结果是 **BUILD FAILED**，外层退出码 **1**。失败是 5 个 Java 编译错误，都在空间切割客户端文件。分身源码没有编译错误。`src/main` 前后快照 sha256 差异为 0，所以这次失败对应的就是这份源码。游戏未测，未打包，不是复审通过。

## 环境

开工时进程里的值是：`JAVA_HOME` 为空，`GRADLE_USER_HOME` 指向 `C:\Users\baishang\AppData\Local\Temp\cursor-sandbox-cache\4b6ad4d19711be11a1f1fd5edcfb931b\gradle`。

只在本次编译进程改为：

- `JAVA_HOME=C:\Program Files\Java\jdk-17`
- `GRADLE_USER_HOME=C:\Users\baishang\.gradle`
- PATH 前置 `C:\Program Files\Java\jdk-17\bin`

没有改永久环境，没有改构建文件，没有复制或删除缓存，没有改插件版本，没有在线重试。`java -version` 为 17.0.6+9-LTS-190。`gradle.properties` 原有 `-Xmx3G` 和 `daemon=false`，没有改。

本机缓存里已有：

- `C:\Users\baishang\.gradle\wrapper\dists\gradle-8.8-bin\dl7vupf4psengwqhwktix4v1`
- `org.gradle.toolchains:foojay-resolver:0.7.0` 的 pom、module、jar
- `org.gradle.toolchains.foojay-resolver-convention.gradle.plugin:0.7.0` 的 pom

日志没有再下载 Gradle 发行包。配置阶段已用 Java 17.0.6 进入 `:compileJava`。

## 开工时没有源码写入或另一路编译

空间切割 `repair-r1-retry-state.json` 已于 2026-11-02T05:35:01+08:00 完成，退出码 0。`review-r2-state.json` 已于 05:40:38 完成，退出码 0。该批 `compile-state.json` 已于 05:44:55 完成，退出码 0。`调度续跑.json` 的 `sourceWritesStopped` 为 true，`compile.authorized` 为 false，阶段是等待这次共享编译。编译开始前没有 `GradleWrapperMain` 或 `compileJava`。IntelliJ 的空闲 Gradle daemon pid 66116 仍在，无子进程，没有结束它。当时唯一的 Cursor 代理是本编译会话。

## 本次编译

- 命令：`.\gradlew.bat compileJava --offline --no-daemon`
- 工作目录：`D:\alianximod`
- 开始：2026-11-02T05:46:46.9953410+08:00
- 结束：2026-11-02T05:47:10.5509741+08:00
- 外层退出码：1
- 日志：`compile-gradle.log`
- 任务：`:downloadMcpConfig` 执行，`:extractSrg` 与 `:createMcpToSrg` 为 UP-TO-DATE，`:compileJava` 执行后 FAILED
- 日志末行：`BUILD FAILED in 23s`，`4 actionable tasks: 2 executed, 2 up-to-date`

## 编译错误

编译器汇总为 5 个错误、41 个警告。错误原文如下，本单没有改这些文件。

`SpaceCutBodyPose.java:21`

```text
错误: slim 在 PlayerModel 中是 private 访问控制
frame.rememberBody(model, model.slim, rightSleeve, leftSleeve);
```

`SpaceCutItemMount.java:61`

```text
错误: 找不到符号 变量 xBobO
错误: 找不到符号 变量 xBob
float pitchBob = Mth.lerp(partialTick, player.xBobO, player.xBob);
```

`SpaceCutItemMount.java:62`

```text
错误: 找不到符号 变量 yBobO
错误: 找不到符号 变量 yBob
float yawBob = Mth.lerp(partialTick, player.yBobO, player.yBob);
```

这 5 处都属于空间切割返修文件，交回该作者。分身 Java 没有出现在错误列表里。

警告摘要：41 条都是 `[removal]`。其中 40 条是 `ResourceLocation` 构造器已过时并标记待删除；1 条是 `HitVolumes.java:208` 的 `ProjectileImpactEvent.setCanceled(boolean)`。另有一条注释说部分输入文件使用或覆盖了已过时 API。没有把警告当成错误。

## 源码快照

- 前：`compile-source-before.json`，2026-11-02T05:46:46.9709490+08:00，1061 个文件。
- 后：`compile-source-after.json`，2026-11-02T05:47:10.7954154+08:00，1061 个文件。
- sha256 差异：**0**。
- 最新写入：`SpaceCutItemMount.java`，2026-11-02T05:32:23.0236204+08:00，早于编译开始。
- 分身目录最新写入：`ClonePositionPolicy.java`，2026-11-02T04:59:12.5969106+08:00。

差异为 0，因此这次失败编译覆盖的就是这一份 `src/main`。结束后复查没有新的源码作者，也没有新的 `compileJava`。空闲 daemon 66116 仍在。不能据此写成整个工作区以后都不会再有写入；只能说明这次编译窗口到报告前的复查为止，没有另一路作者启动。

## 命名门禁

最终快照写完之后才执行 `powershell -NoProfile -File tools/check-name-gate.ps1`。开始 2026-11-02T05:47:11.9510406+08:00，结束 05:47:14.3121107+08:00，退出码 0。`naming-gate-final.log` 全文是：

```text
源码命名可读性检查通过：未发现单字母变量或禁止缩写
```

本次输出没有本批新问题，也没有列出历史问题。先前记录过的 `OriginalHumanoid.java` 的 `i` 和 `to` 这次没有出现。这只是脚本输出，不是正确性复审，也没有为门禁改源码。

## 未做

没有第二次编译，没有修 Java，没有 build、jar、reobf、runClient，没有打包，没有启动游戏，没有独立测试，没有改制作表，没有提交，没有额外复审。attempt-1 与 attempt-2 归档没有覆盖。
