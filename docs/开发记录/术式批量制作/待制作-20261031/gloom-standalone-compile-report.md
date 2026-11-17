# 幽冥独立释放返修最终编译

阶段：必要机械编译。设计额度 2/2，实现正式额度 3/3。没有追加正确性复审，没有读源码作逻辑判断，没有改 Java、资源、构建配置或制作表。

结论：**compileJava 成功，退出码 0。** 编译发生在用户 Temp 里的独立副本，没有在 `D:\alianximod` 启动 Gradle。原项目源码和构建配置在编译前后字节一致。原项目 `build` 里的 `XueXiMod.class` 仍是 13:03 的文件，这次没有更新正在运行的客户端。成功只表示副本编译通过，不是游戏验收。

## 1. 交回与停写

D 的 `gloom-standalone-repair-report` 声明 `stopWrite=true`，未跑编译。磁盘 SHA256 与交回的 10 个文件全部一致。

| 文件 | 磁盘 mtime | SHA256 |
| --- | --- | --- |
| `DomainDefinition.java` | 2026-10-31T23:09:08.656+08:00 | `510795563e269bb818542843fb1553128c335ad8ce059299a1ae55c87c430bf2` |
| `DomainVisual.java` | 2026-10-31T23:09:06.220+08:00 | `7390246650775c087985aa7ddd808fda5ec95c64e842fc3bb99e28c5b60b705e` |
| `DomainWorldRenderer.java` | 2026-10-31T23:09:08.796+08:00 | `0ae28490d170251fb9647fdf284417086b6dbe6df9de36135086acdb53395308` |
| `ListedRelease.java` | 2026-10-31T23:09:44.302+08:00 | `9c16080d201c6b743e915bb3c490ec157d8fe12ec5d665686e60261082c65828` |
| `ReleaseDefinitions.java` | 2026-10-31T23:09:09.009+08:00 | `1786fb0983448512c43dc046e2ccafe187628b031740471b4f8a90055ae1df53` |
| `BatchReleaseCompiler.java` | 2026-10-31T23:09:37.115+08:00 | `10599f046c00aa3b27d3d9c39cc22f2a23b5c1d69545600fffbbc023f776f10d` |
| `SkillUsePacket.java` | 2026-10-31T23:09:19.048+08:00 | `f5adcca9a85127a9dee8c481e5f76fbceb53775ecaa739946be9b0a99e87a7f4` |
| `DomainWave.java` | 2026-10-31T23:09:47.139+08:00 | `6a3ef42e988efc54dc57ec328df18b41901bef50ad51c5bd3ecfbc439b22d1b2` |
| `GloomDomainEffect.java` | 2026-10-31T23:09:50.016+08:00 | `d68ce2c54ea1fbc93950ae50f12d8ed2f70bd2d95fcff953d83a2aa9df9bd3ab` |
| `ModTechniques.java` | 2026-10-31T23:09:10.299+08:00 | `34d8252774bc8b27114fdad9712ef41c961ae1c5bdd03cdfb46280a74a19868c` |

当前基线在交回核对之后重新采集，没有用旧快照差异停编。2 秒稳定性窗口内，`src/main`、`src/generated/resources` 和 7 个构建配置文件的路径、字节、修改时间、SHA256 都没有变化。编译期间原项目这三类输入的内容差异为 0。

## 2. 为什么不在原项目编译

23:22 原项目没有 `GradleWrapperMain` 任务。有一个 Forge 客户端占着原 Gradle：客户端 pid 16680，父进程是 GradleDaemon pid 37744，再上一级 pid 6140。客户端命令同时包含 `alianximod` 与 Forge 客户端启动特征。这个 daemon 有子进程，不是空闲 daemon。没有结束客户端、daemon 或 IDEA。

`build.gradle` / `settings.gradle` 没有把 `buildDir` 指到绝对路径，也没有写死 `D:\alianximod`。Mixin 输出使用 `layout.buildDirectory`。因此按隔离方案在用户 Temp 新建副本。未注释的 `flatDir` 和 `libs` 依赖不存在；`outputs.files(...)` 只是 Mixin 任务输出，依赖来自用户 Gradle 缓存。

## 3. 命令

- 命令：`.\gradlew.bat compileJava --offline --no-daemon`
- 工作目录：`C:\Users\baishang\AppData\Local\Temp\xueximod-gloom-compile-20261031-232245-eaea72e5`
- 使用的是该目录自己的 `gradlew.bat`
- `JAVA_HOME=C:\Program Files\Java\jdk-17`（17.0.6+9-LTS-190）
- `GRADLE_USER_HOME=C:\Users\baishang\.gradle`
- 副本里的 `gradle.properties` 仍是 `org.gradle.jvmargs=-Xmx3G`、`org.gradle.daemon=false`
- 外壳注入的沙箱 Gradle 目录没有传给这次构建
- 开始：2026-10-31T23:22:48.348+08:00
- 结束：2026-10-31T23:23:26.701+08:00
- 退出码：0
- 日志：`BUILD SUCCESSFUL in 37s`，4 个任务都执行了
- 编译器：0 个错误，38 个 removal 警告。警告是既有 API 过时提示，没有因此改源码

完整日志：`gloom-standalone-compile-gradle.log`。

## 4. 输入一致

编译前逐文件证明原项目与副本 1027 个输入同字节、同 SHA256，mismatch 为 0。构成是 `src/main` 955 个文件、10703358 字节，`src/generated/resources` 65 个文件、26708 字节，再加上 `gradlew`、`gradlew.bat`、`build.gradle`、`settings.gradle`、`gradle.properties` 和两份 wrapper 文件。没有复制 `.git`、`.idea`、`.gradle`、`build`、`run` 或存档。

| 清单 | 时间 | 结果 |
| --- | --- | --- |
| `gloom-standalone-compile-src-before.json` | 2026-10-31T23:22:44.588+08:00 | 稳定基线 |
| `gloom-standalone-compile-copy-manifest.json` | 2026-10-31T23:22:48.341+08:00 | 原项目与副本 mismatch 0 |
| `gloom-standalone-compile-src-after.json` | 2026-10-31T23:23:30.212+08:00 | 原项目仍是 955 个文件、10703358 字节 |

编译后副本输入相对编译前基线的差异也是 0。

原项目 `build\classes\java\main\com\bslx\XueXiMod\XueXiMod.class` 的修改时间是 2026-10-31T13:03:41.675+08:00。副本里的同名 class 是 2026-10-31T23:23:25.025+08:00。refmap 写在副本的 `build\tmp\compileJava\compileJava-refmap.json`。这次编译没有更新原项目 build，也没有更新正在运行的客户端。

命名门禁本轮没有重跑。D 的报告已经记录退出码 1，只有 `OriginalHumanoid.java` 第 165 行 `i` 和第 202 行 `to`。那两处不在本次交回文件里，没有扩大修复。

## 5. 未使用的临时目录

第一次准备副本时，目录 `C:\Users\baishang\AppData\Local\Temp\xueximod-gloom-compile-20261031-232126-b87085bb` 已创建，但清单写入中断，没有在那里执行 Gradle。成功的编译只用 23:22:45 新建的 `xueximod-gloom-compile-20261031-232245-eaea72e5`。两个目录都留在用户 Temp，没有递归删除。

## 6. 未做

没有 build、jar、reobf、datagen、runClient、测试、游戏或发布。没有提交。没有迁表。没有追加复审。没有改源码。13 项仍未迁。本执行者到此停止。
