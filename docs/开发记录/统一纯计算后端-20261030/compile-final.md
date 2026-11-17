# 返修后的 Java 统一编译

结论：`compileJava` 实际执行并通过，退出码 0。编译前后 712 项冻结一致，记为返修后的 Java 通过。Gradle 槽和文档写入权已释放，`released=true`。

## 命令与环境

工作目录 `D:\alianximod`。同一 PowerShell 进程设置 `JAVA_HOME=C:\Program Files\Java\jdk-17`、`GRADLE_USER_HOME=C:\Users\baishang\.gradle`，并把 JDK `bin` 放在 PATH 前缀。`gradle.properties` 原有 `-Xmx3G` 与 `daemon=false`，没有改。stdout/stderr 重定向到文件，没有用 PowerShell 管道。

```text
.\gradlew.bat compileJava --offline --no-daemon
```

开始 `2026-10-30T09:31:31.8231826Z`，结束 `2026-10-30T09:31:58.3991523Z`，退出码 0。Java 是 17.0.6+9-LTS-190。`:compileJava` 实际执行，不是 UP-TO-DATE。`BUILD SUCCESSFUL in 25s`。有 34 条过时 API 警告，没有错误。

上一成功阶段没有残留的同名编译辅助 stdout/stderr 文件需要归档。`java-attempt-2/` 里的第二次成功记录没有覆盖。

冻结捕获于 `2026-10-30T09:28:08.7578015Z`。编译前后 712 项路径、长度和 UTC ticks 一致。没有另做 SHA256。没有打包，没有加载 DLL。
