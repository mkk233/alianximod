# 隔离最终编译报告

阶段：把同一收束源码复制到独立临时项目后再做必要编译。设计额度 2/2，实现正式额度 3/3。本单不追加正确性复审，不改原项目源码。此前的 `final-compile-report`、`compile-after-client-close-report`、`recompile-after-diagnostics-report` 都保留。`recompile-after-diagnostics-report` 仍然是未执行，不是成功。

结论：**隔离编译没有开始。** A、B 交回的 3 个文件哈希仍然一致，但 `src/main` 相对 12:54 的稳定快照还有 2 个未说明的内容变化。按本单不能把这份源码当作最终版本，也没有在原项目或临时副本上启动 Gradle。没有新建临时目录，没有改源码，没有迁表，没有打包，没有结束用户的游戏、IDEA 或 Gradle 进程。

## 1. 已核对的交回

只核对文件版本，没有读作者中间代码，也没有审这两处返修对不对。

| 文件 | 交回 SHA256 | 13:07 磁盘 | 结果 |
| --- | --- | --- | --- |
| `src/main/java/com/bslx/XueXiMod/item/StaffSkills.java` | `24571ba13e7d3f44353ad4b725f2074960f0f09f0b5d22e0923ff1e69842087c` | 相同 | 一致 |
| `src/main/java/com/bslx/XueXiMod/shu/technique/enchant/resource/ResourceEnchantSlice.java` | `7659032a12d77dd578caa9355290959d367fc8c49222e252f42235791f411552` | 相同 | 一致 |
| `src/main/java/com/bslx/XueXiMod/shu/technique/enchant/resource/ResourceEnchantCatalog.java` | `ed91e0dfaa79690378aaa1c77e23d88f3351a15624124d390570c590366f993a` | 相同 | 一致 |

A、B 的 compile-fix 报告都声明停写，跨作者请求为空。这两份报告没有包含下面两个变化。

## 2. 未说明变化

对照基线 `recompile-after-diagnostics-src-before.json`（2026-10-31T12:54:51.149+08:00）。`src/main` 与 `src/generated/resources` 的路径、字节数、SHA256 差异一共 2 条，都在 `src/main`。generated 没有差异。四份 `author-*-ownership.json` 都没有这两个路径。

| 文件 | 基线 | 当前 |
| --- | --- | --- |
| `src/main/java/com/bslx/XueXiMod/command/ModCommands.java` | 1024 字节，mtime 2026-10-30T04:16:35.667+08:00，`f3049296125ffc5e9327045884933742a005c07dc8696d55570b4e6fe2d47220` | 1105 字节，mtime 2026-10-31T13:03:04.078+08:00，`8d06a17320ae6e1e266b8b2af525733e0223d33ed2bd21b4cb0d563a6961bdec` |
| `src/main/java/com/bslx/XueXiMod/command/ShuCommands.java` | 28843 字节，mtime 2026-10-30T02:39:23.105+08:00，`4cb68b00808606871a5435f3269d39bdf6e4d9902e1489b540671edec643ebf8` | 31368 字节，mtime 2026-10-31T13:03:06.984+08:00，`09192a6b122f9eeec9249f83f8a12772ee8a89f3e259fef0d77db70a33cf43af` |

13:08 再读了一次，两个文件的 mtime、字节数和 SHA256 都没有变。短窗口里没有继续写入。变化本身发生在 13:03，晚于已收束快照，也不在 A、B 的 3 文件交回里，所以仍然不能当成这次隔离编译的最终输入。没有打开这两个文件做逻辑判断。

证据：`isolated-final-compile-unexpected.txt`、`isolated-final-compile-status.txt`。

## 3. 构建输出位置

只读了 `build.gradle` 和 `settings.gradle`。没有发现把 `buildDir` 指到绝对路径，也没有把 `D:\alianximod` 写进这两个文件。Mixin 和 refmap 使用 `layout.buildDirectory`，默认落在执行该构建的项目根下的 `build`。`run` 与 `run-data` 只是 `project.file(...)`，本单本来也不会跑 `runClient` 或 datagen。因此“输出会写回原项目”这一条本身没有构成阻塞。阻塞是源码已经不是收束版本，所以没有创建副本，也没有执行编译。

拟定环境已经核对，但没有用来启动 Gradle：`JAVA_HOME=C:\Program Files\Java\jdk-17`（17.0.6+9-LTS-190），`GRADLE_USER_HOME=C:\Users\baishang\.gradle`。外壳注入的沙箱 Gradle 目录没有使用。用户 Temp 父目录解析为 `C:\Users\baishang\AppData\Local\Temp`。没有创建 `xueximod-final-compile-20261031-*`。

## 4. 未执行

没有在 `D:\alianximod` 或任何临时副本执行 `compileJava`。没有开始时间、结束时间、退出码或编译日志。没有命名门禁重跑。没有复制 `.git`、`.idea`、`.gradle`、`build`、`run` 或存档。没有递归删除或移动。没有改 Java、资源、构建配置或制作表。没有结束用户进程。没有迁表，没有追加复审，没有游戏验证。

## 5. 停止

原项目在 13:03 出现 `ModCommands.java` 和 `ShuCommands.java` 两处未说明内容变化。A、B 三处交回哈希仍在。隔离编译因此没有启动，不能把当前源码称为已经覆盖的最终版本。本执行者到此停止全部操作，不在原工作区并行编译，不清理现场，不迁表。
