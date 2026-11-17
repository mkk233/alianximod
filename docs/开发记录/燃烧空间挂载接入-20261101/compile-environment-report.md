# 燃烧空间挂载：构建环境恢复

模型：grok-4.7-high-fast。会话：14dc9491-4e97-4a66-a10f-b7a91f9ff90a。

## 定位

当前进程原来的 `GRADLE_USER_HOME` 是：

`C:\Users\baishang\AppData\Local\Temp\cursor-sandbox-cache\1666b9acff5b60e1c6fe638eb4f17110\gradle`

`JAVA_HOME` 为空。PATH 上的 `java` 来自 `C:\Program Files\Common Files\Oracle\Java\javapath\java.exe`，版本仍是 17.0.6。

项目 2026-11-01 17:32 成功离线编译记录使用的是：

- `JAVA_HOME=C:\Program Files\Java\jdk-17`
- `GRADLE_USER_HOME=C:\Users\baishang\.gradle`

该缓存现在仍在。`wrapper\dists` 里有 `gradle-8.8-bin` 和 `gradle-8.10-bin`。`caches\modules-2\files-2.1` 里已有：

- `org.gradle.toolchains:foojay-resolver:0.7.0`
- `org.gradle.toolchains.foojay-resolver-convention:org.gradle.toolchains.foojay-resolver-convention.gradle.plugin:0.7.0`

第一次编译因此重新下载了 Gradle 8.8，并在配置阶段找不到 foojay 插件。那次原始证据没有改写：

- `compile-attempt-1-report.md`
- `compile-attempt-1-result.json`
- `compile-attempt-1.log`，sha256 `60523b94de1ef782a267b0ba4d0588cdd65a71256ff07e049c59c7fdbe90a8d3`

## 恢复

没有改构建配置，没有删缓存，没有换插件版本，没有使用镜像。本机既有缓存已经够用，所以没有下载，也没有运行 Gradle `help`。

只在当前进程覆盖：

- `JAVA_HOME=C:\Program Files\Java\jdk-17`
- `GRADLE_USER_HOME=C:\Users\baishang\.gradle`
- PATH 前置该 JDK 的 `bin`

恢复后确认没有其他 Gradle 进程，再执行第二次、也是环境变化后的唯一一次：

`.\gradlew.bat compileJava --offline --no-daemon`

开始 `2026-11-01T21:37:58.9101183+08:00`，结束 `2026-11-01T21:38:35.9135652+08:00`，退出码 0。`:compileJava` 实际执行，不是 `UP-TO-DATE`。6 个燃烧源路径哈希变化数 0。stderr 有 40 条 `[removal]` 警告、0 条错误。

正式归档 `docs/资源库/特效/火焰/燃烧_空间挂载_v1/` 的 README、manifest、交接状态已标为 compileJava 通过、待用户实测。批准源文件及其 manifest 哈希未改。制作表和 `docs/资源库/README.md` 未写。

游戏内画面、苗条臂、广角和真实第一人称透视仍未验证。
