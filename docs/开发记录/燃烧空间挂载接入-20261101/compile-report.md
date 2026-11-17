# 燃烧空间挂载：最终编译报告

模型：grok-4.7-high-fast。会话：14dc9491-4e97-4a66-a10f-b7a91f9ff90a。

这是恢复本机构建缓存之后的最新一次编译。第一次失败原样留在 `compile-attempt-1-report.md`、`compile-attempt-1-result.json`、`compile-attempt-1.log`，没有改写成成功。

## 结论

`compileJava` 通过，退出码 0。javac 已经执行。`BUILD SUCCESSFUL in 35s`。燃烧源码在命令前后哈希未变。游戏画面仍未验证。

## 环境

只覆盖当前进程，没有改用户或系统环境，也没有改 `settings.gradle`、`build.gradle`、`gradle.properties`、wrapper。

- `JAVA_HOME=C:\Program Files\Java\jdk-17`（Java 17.0.6）
- `GRADLE_USER_HOME=C:\Users\baishang\.gradle`
- `org.gradle.jvmargs=-Xmx3G`、`org.gradle.daemon=false` 未改
- 本机缓存里已有 `gradle-8.8-bin`，以及 `foojay-resolver` 0.7.0 和插件标记 `org.gradle.toolchains.foojay-resolver-convention.gradle.plugin` 0.7.0
- 这次没有再下载 Gradle 发行包，也没有跑 `help` 做在线准备

## 命令

- 工作目录：`D:\alianximod`
- 命令全文：`.\gradlew.bat compileJava --offline --no-daemon`
- 开始：`2026-11-01T21:37:58.9101183+08:00`
- 结束：`2026-11-01T21:38:35.9135652+08:00`
- 退出码：`0`
- 结束后进程检查：`NO_GRADLE_PROCESS`
- stdout：`compileJava-offline-2.log`
- stderr：`compileJava-offline-2.stderr.log`（原文编码），`compileJava-offline-2.stderr.utf8.log`（按 GBK 转成 UTF-8）
- 编译前源文件哈希：`compile-attempt-2-source-before.txt`。编译后这 6 个路径哈希变化数：`0`

覆盖核对的路径：

- `src/main/java/com/bslx/XueXiMod/client/furnace/BurnSpatialMount.java`
- `src/main/java/com/bslx/XueXiMod/client/furnace/BurnFirstPersonPose.java`
- `src/main/java/com/bslx/XueXiMod/client/furnace/FurnaceBodyLayer.java`
- `src/main/java/com/bslx/XueXiMod/client/furnace/FurnaceArmFlame.java`
- `src/main/java/com/bslx/XueXiMod/client/furnace/FurnaceBodyVisual.java`
- `src/main/resources/assets/xueximod/models/effect/burn_spatial_v1/mount.json`

## 最终输出

stdout：

```
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.8/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build

> Configure project :
Java: 17.0.6, JVM: 17.0.6+9-LTS-190 (Oracle Corporation), Arch: amd64

> Task :downloadMcpConfig
> Task :extractSrg UP-TO-DATE
> Task :createMcpToSrg UP-TO-DATE

> Task :compileJava

BUILD SUCCESSFUL in 35s
4 actionable tasks: 2 executed, 2 up-to-date
```

stderr 末尾：

```
注: 某些输入文件使用或覆盖了已过时的 API。
注: 有关详细信息, 请使用 -Xlint:deprecation 重新编译。
40 个警告
```

没有 `错误:`。40 条都是既有的 `[removal]` 过时 API 警告，其中 3 条落在本次炉心文件，类型与全仓相同，没有导致失败：

- `FurnaceBodyVisual.java:311` `ResourceLocation(String,String)`
- `BurnSpatialMount.java:39` `ResourceLocation(String,String)`
- `BurnSpatialMount.java:208` `ResourceLocation(String,String)`

日志里的 single-use Daemon 是 `--no-daemon` 下 Gradle 为遵守 `org.gradle.jvmargs` 临时拉起的进程，构建结束已退出。

未打包，未 `runClient`，未启动游戏。编译通过不是游戏视觉验证。
