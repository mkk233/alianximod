# 燃烧空间挂载：最终编译报告

模型：grok-4.7-high-fast。会话：14dc9491-4e97-4a66-a10f-b7a91f9ff90a。

## 结论

`compileJava` 失败，退出码 1。javac 没有启动。燃烧 Java 与运行时资源在本次命令前后哈希未变。正式归档 README / manifest 仍为「待最终编译」，未改批准源文件或哈希。

失败发生在 `settings.gradle` 第 13 行解析插件，责任不在本次燃烧源码。

## 执行前确认

- 已读 `implementation-report.md` 与 `implementation-result.json`：`source_writes_closed=true`，`needs_compile=true`，`blockers=[]`。
- 没有其他 Gradle / Java wrapper 进程。历史终端均已结束。
- `gradle.properties` 原有 `org.gradle.jvmargs=-Xmx3G` 与 `org.gradle.daemon=false`，未改。
- 未执行 `build`、`jar`、`reobf`、`runClient`，未启动游戏，未重编。

## 命令

- 工作目录：`D:\alianximod`
- 命令全文：`.\gradlew.bat compileJava --offline --no-daemon`
- 开始：`2026-11-01T21:32:36.7873826+08:00`
- 结束：`2026-11-01T21:32:57.4430012+08:00`
- 退出码：`1`
- 结束后进程检查：`NO_GRADLE_PROCESS`
- 完整日志：`compileJava-offline.log`
- 编译前源文件哈希：`compile-source-before.txt`。编译后这 6 个路径哈希变化数：`0`

覆盖核对的路径：

- `src/main/java/com/bslx/XueXiMod/client/furnace/BurnSpatialMount.java`
- `src/main/java/com/bslx/XueXiMod/client/furnace/BurnFirstPersonPose.java`
- `src/main/java/com/bslx/XueXiMod/client/furnace/FurnaceBodyLayer.java`
- `src/main/java/com/bslx/XueXiMod/client/furnace/FurnaceArmFlame.java`
- `src/main/java/com/bslx/XueXiMod/client/furnace/FurnaceBodyVisual.java`
- `src/main/resources/assets/xueximod/models/effect/burn_spatial_v1/mount.json`

## 原始输出

stdout：

```
Downloading https://services.gradle.org/distributions/gradle-8.8-bin.zip
.............10%.............20%.............30%.............40%.............50%.............60%..............70%.............80%.............90%.............100%

Welcome to Gradle 8.8!

Here are the highlights of this release:
 - Running Gradle on Java 22
 - Configurable Gradle daemon JVM
 - Improved IDE performance for large projects

For more details see https://docs.gradle.org/8.8/release-notes.html

To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.8/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build
```

stderr：

```
FAILURE: Build failed with an exception.

* Where:
Settings file 'D:\alianximod\settings.gradle' line: 13

* What went wrong:
Plugin [id: 'org.gradle.toolchains.foojay-resolver-convention', version: '0.7.0'] was not found in any of the following sources:

- Gradle Core Plugins (plugin is not in 'org.gradle' namespace)
- Included Builds (No included builds contain this plugin)
- Plugin Repositories (could not resolve plugin artifact 'org.gradle.toolchains.foojay-resolver-convention:org.gradle.toolchains.foojay-resolver-convention.gradle.plugin:0.7.0')
  Searched in the following repositories:
    Gradle Central Plugin Repository
    MinecraftForge(https://maven.minecraftforge.net/)
    maven(https://maven.parchmentmc.org)

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 19s
```

## 责任范围

`settings.gradle` 第 13 行声明 `id 'org.gradle.toolchains.foojay-resolver-convention' version '0.7.0'`。本机 `C:\Users\baishang\.gradle` 下没有该插件缓存。`--offline` 不能从 Gradle Central Plugin Repository、MinecraftForge、Parchment 下载它，构建在配置阶段结束。

Wrapper 在构建开始前下载了 `gradle-8.8-bin.zip`（`gradle-wrapper.properties` 的 `distributionUrl`）。这次下载成功，随后插件解析失败。日志里的 single-use Daemon 是 `--no-daemon` 下 Gradle 为遵守 `org.gradle.jvmargs` 而临时拉起的进程，构建结束已退出。`gradle.properties` 未被修改。

燃烧空间挂载的 Java、`mount.json` 和贴图没有进入编译，也没有语法错误输出。不能把这次失败记成燃烧源码编译失败，也不能用任何旧的 `compileJava` 结果代替。

## 阻塞

另发任务前，工作区无法完成指定的离线 `compileJava`。需要一次单独授权：在不改燃烧 Java、运行时贴图、制作表和资源库索引的前提下，先让 `org.gradle.toolchains.foojay-resolver-convention:0.7.0` 进入本机 Gradle 插件缓存，再对已经收束的同一份最终源码重新执行 `.\gradlew.bat compileJava --offline --no-daemon`。本任务没有做这第二次编译。

游戏画面仍未验证。
