我先读取任务说明和执行流程，确认这次授权范围后再动手。任务是最终统一编译。我先读收束资料和进程状态，确认源码稳定后再执行唯一的 Gradle 命令。收束记录已对齐：上次编译命令没发出。我现在只核对进程和这 13 个路径是否仍稳定。进程清单里还有 IDE 的 Java。我接着只核对它是否仍在构建，以及有没有别的会话在写源码。IDE 的 Gradle 仍是空闲心跳，没有别的源码写入者。接下来写入编译前哈希，再执行唯一的 `compileJava`。13 个文件的内容和长度都对得上，时间差是 JSON 解析丢掉了小数秒。我改用原始时间再对一次，然后开始编译。Gradle 已结束，退出码是 1。我读取原始输出，只整理失败诊断，不改源码、不重跑。第一次退出码 1 来自沙箱里的空 Gradle 缓存，插件在正常缓存里是有的。我改用项目实际的 Gradle 主目录，再执行同一条编译命令。退出码是 0，但只用了 16 秒。我核对 Gradle 原文，确认是真正编译通过，而不是空跑。`.\gradlew.bat compileJava --offline --no-daemon` 已实际执行。Gradle 自己的退出码是 **0**，`BUILD SUCCESSFUL in 15s`。这是构建日志里的退出码，不是外层代理进程的退出码。

## 结果

- 开始 `2026-10-29T23:32:11.4609881+08:00`，结束 `2026-10-29T23:32:27.5716036+08:00`，耗时 16.111 秒。
- 开始时间晚于 13 个冻结路径的末次写入（`TechniqueHits.java` `23:11:01.0177388`），也晚于 `src/main` 最晚文件 `FireVisualClient.java` `23:13:25.4719538`。
- 编译前后 `src/main` 都是 769 个文件，内容哈希差异 0。13 个路径的长度、时间和 SHA-256 与 `source-freeze-metadata.json` 一致，前后哈希差异 0。
- `:compileJava` 为 `UP-TO-DATE`。这 12 个主源码和 `FireVisualClient.java` 的 class 都在 `23:18:20`–`23:18:25` 写成，晚于对应源文件，所以这次没有重新调用 javac。
- 使用 `JAVA_HOME=C:\Program Files\Java\jdk-17`（17.0.6）和 `GRADLE_USER_HOME=C:\Users\baishang\.gradle`。`gradle.properties` 仍是 `org.gradle.jvmargs=-Xmx3G`、`org.gradle.daemon=false`，没有改配置。
- 无失败诊断。

## 运行前核对

IDE 的 Gradle 进程 73124 只有注册表心跳，没有子进程。火系写回进程 57000 已不在。没有其他正在写源码的会话，也没有另一路构建。

## 同一次命令的前一次退出

第一次 Gradle 退出码是 **1**。当时 `GRADLE_USER_HOME` 被指到空的沙箱缓存，`--offline` 在 `settings.gradle` 第 13 行找不到 `org.gradle.toolchains.foojay-resolver-convention` 0.7.0。该插件在 `C:\Users\baishang\.gradle` 中存在。这次没有改到源码，不能当作最终编译。日志在 `compile-evidence/gradle-stderr-sandbox.txt`。随后用项目实际的 Gradle 主目录执行了同一条命令，以上面的退出码 0 为准。

## 未做

未改生产或测试源码，未打包，未跑测试，未启动游戏，未写制作表，未提交。`src/test` 不在 `compileJava` 范围内。证据在 `docs/开发记录/领域附魔底座实施-20261029/compile-evidence/`。
