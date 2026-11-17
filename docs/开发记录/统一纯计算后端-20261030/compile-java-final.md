我先读完整的 Java 编译任务说明，再按其中的授权范围执行。这是最终 Java 统一编译。我先读所有权、冻结清单和最新 native 结果指针，确认输入没变、也没有别的 Gradle 在跑。最新结果是第二次 native 构建，DLL 已编出。ABI 末尾的调用状态已经同步过。我先核对 712 项冻结和有没有正在跑的 Gradle，通过后再在同一进程里离线编译。编译失败，退出码 1，冻结清单没有变化。我只从日志里抽出那 4 个错误的路径和行号，不改源码。`compileJava` 实际执行后失败，4 个错误都在 `SkillChainPlan.java`，冻结输入没有变。我只把这次失败和已有的 Windows DLL 结果写进两份说明，不改源码。`compileJava` 实际执行后失败，退出码 1。不能记为本版 Java 通过。Gradle 槽和文档写入权已释放，`released=true`。

设计 2/2、实现 3/3 已用完。未追加正式复审。没有 build、jar、reobf、runClient、测试、游戏或性能实验，没有加载 DLL。

## 命令与环境

工作目录 `D:\alianximod`。同一 PowerShell 进程设置 `JAVA_HOME=C:\Program Files\Java\jdk-17`、`GRADLE_USER_HOME=C:\Users\baishang\.gradle`，并把 JDK `bin` 放在 PATH 前缀。`gradle.properties` 原有 `org.gradle.jvmargs=-Xmx3G` 与 `org.gradle.daemon=false`，没有改。stdout/stderr 由 `cmd.exe /d /c` 重定向到文件，没有用 PowerShell 管道。

```text
.\gradlew.bat compileJava --offline --no-daemon
```

开始 `2026-10-30T08:47:04.5781116Z`，结束 `2026-10-30T08:47:27.2744045Z`，退出码 1。Java 是 17.0.6+9-LTS-190，64 位。日志写明 Gradle 为这次构建派生单次 Daemon，并在结束时停掉。`:compileJava` 有执行记录，随后 `FAILED`，不是 UP-TO-DATE。`BUILD FAILED in 21s`。审批没有拒绝。

编译前 712 项冻结全部一致，最新输入写入 `2026-10-30T08:36:18.6624943Z`，开始时间晚于它。没有活跃 `GradleWrapperMain`。有 1 个空闲 Daemon，按任务说明不把它当忙。编译后 712 项路径、长度和 UTC ticks 仍一致。没有另做 SHA256。

## 诊断

4 个错误、34 条过时 API 警告。失败原因是错误，不是警告。

`SkillChainPlan.java` 第 1121 行：

```text
private static MulticastExpansion expansionAtRule(List<ChainSegment> segments, int ruleIndex, List<Technique> techniques)
```

第 367、909、1057、1107 行调用写成 `expansionAtRule(segments, techniques, ruleIndex 或 plan.dualRuleSegment)`。`List<Technique>` 落到了 `int` 参数上。交回多重释放作者（`01a15fdc-4130-7c92-bece-27530e8c54be`）。纯计算 A/B 的文件不在这 4 个错误里。没有改 Java、C 或构建配置。细节见 `docs/开发记录/统一纯计算后端-20261030/compile-failure-diagnosis.md`，完整输出见同目录 `compile.log`。

## native 与文档

最新指针 `native-current-result.json` 指向 `native-attempt-2-final.md`。Windows x64 DLL 已在第二次构建生成：`native/compute/build/xueximod_compute.dll`，26112 字节，dumpbin 为 x64，7 个 JNI 导出。`native-compile-final.md` 仍是第一次退出码 6 的历史，没有覆盖。Java 失败不能写成 DLL 已在进程里可用。

`abi-contract.md` 末尾已经是 `count=1` 的单次分发记录，并写明尚未运行加载。本单没有改这份契约。

两份说明已按真实结果改了验证状态，没有改接口契约：

- `docs/纯计算后端使用说明.md`
- `native/compute/README.md`

尚未 JNI 加载、数值对照、游戏或性能测试。Linux / macOS 未编译。未追加复审，不声称 TPS 收益。多重释放会话应采用这次失败的统一编译结果。
