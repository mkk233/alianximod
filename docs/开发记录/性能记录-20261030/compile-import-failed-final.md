# 真实 Gradle 主目录下的最终编译

规定命令已经用真实 Gradle 主目录跑完。退出码是 1。不能报告最终源码编译通过。未打包，未启动游戏，未做性能或游戏实测。

本任务继续原编译会话，模型为 Cursor CLI grok-4.7-high-fast。没有再委托代理，没有改 Java、Gradle 配置或制作表，没有下载 wrapper 或依赖，没有终止其他进程。

## 先例

已读 `docs/开发记录/领域附魔底座实施-20261029/compile-run-final.md`。第 18 行写明：同一次 `compileJava --offline --no-daemon` 第一次退出码 1，是因为 `GRADLE_USER_HOME` 指向空沙箱缓存，`settings.gradle` 第 13 行找不到 `org.gradle.toolchains.foojay-resolver-convention` 0.7.0；该插件在 `C:\Users\baishang\.gradle`。显式改用这个真实主目录后，同命令退出码 0。

## 留档与编译前

当前失败日志已另存，目标原先不存在：

- `compile-empty-home-failed.log`
- `compile-empty-home-failed-final.md`
- `compile-empty-home-failed-results.json`

同项目没有新的 Gradle 构建。PID 52180 仍只有注册表锁心跳，按授权不构成阻塞，也没有终止它。660 个冻结 Java 文件的 SHA-256、长度和 `lastWriteUtc` 在编译前全部匹配。最晚源码写入是 `2026-10-29T20:20:34.7212557Z`。作者 A、B、C 此前已释放。

## 实际环境

调用 Gradle 的 PowerShell 进程内显式设置，没有改全局配置，也没有改 `HOME`、`USERPROFILE`、`CODEX_HOME`。

进入进程时 `JAVA_HOME` 未设置，`GRADLE_USER_HOME` 是空沙箱缓存 `C:\Users\baishang\AppData\Local\Temp\cursor-sandbox-cache\b2e6ba2090d0f4540eae6b67539ab45b\gradle`。显式设置之后、启动 Gradle 之前打印的是：

- `JAVA_HOME=C:\Program Files\Java\jdk-17`
- `GRADLE_USER_HOME=C:\Users\baishang\.gradle`
- `java -version`：`17.0.6`，构建 `17.0.6+9-LTS-190`

`gradle.properties` 仍是 `org.gradle.jvmargs=-Xmx3G`、`org.gradle.daemon=false`，没有改。输出用 stdout/stderr 文件重定向捕获。

## 编译

命令：`.\gradlew.bat compileJava --offline --no-daemon`

工作目录：`D:\alianximod`

- 开始 UTC：`2026-10-29T20:54:27.2167987Z`
- 结束 UTC：`2026-10-29T20:54:49.1183349Z`
- 耗时：21.902 秒
- 退出码：1

这次开始时间晚于冻结清单里的最后一次源码写入。真实主目录生效：构建越过了 foojay 插件，配置阶段打印 `Java: 17.0.6`，并执行到 `:compileJava`。Gradle 摘要是 4 个可执行任务里 2 个执行、2 个 UP-TO-DATE，然后 `:compileJava FAILED`，`BUILD FAILED in 20s`。单次 Daemon 结束后没有留下；PID 52180 仍在。

唯一编译诊断在作者 A 的 `src/main/java/com/bslx/XueXiMod/debug/performance/PerformanceRecorder.java` 第 5 行：

```
import java.io.FileAlreadyExistsException;
```

javac 报告找不到符号，符号是类 `FileAlreadyExistsException`，位置是包 `java.io`。错误数是 1。日志里的中文诊断按 javac 的系统代码页写入，完整原文在 `compile.log`。本次没有改这个文件。

## 编译后

冻结清单再次核对：660/660 的 SHA-256、长度和 `lastWriteUtc` 仍匹配，哈希、长度、时间失败都是 0，缺失 0。最晚源码写入仍是 `2026-10-29T20:20:34.7212557Z`。命令覆盖的是冻结后的最终源码，但退出码不是 0，所以不能报告最终源码编译通过。

没有打包，没有游戏实测，没有性能测试。编译占用在本报告结束后释放。

## 交回

交回作者 A。文件 `PerformanceRecorder.java` 第 5 行，`java.io.FileAlreadyExistsException` 找不到符号。不要把上一份空沙箱缓存的 foojay 失败当成这次的结果；那次日志在 `compile-empty-home-failed.log`。
