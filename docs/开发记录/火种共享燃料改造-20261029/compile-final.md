我先读完整份任务说明，再按里面授权的范围执行。任务是统一编译和定向检查。我先读作者交回的报告和证据，确认改动文件、最后写入时间和检查入口。作者交回已收束。我先核对检查入口和有没有别的 Gradle 或源码写入，再记哈希、编译。作者最后写入是 21:22:29。我先核对进程和这批文件的哈希，一致才开始编译。编译前哈希与作者记录一致，没有别的 Gradle 在跑。编译已开始，我先核对时间戳差异，并等编译结束。编译包装进程几乎没有占用 CPU，输出管道卡住了。我先结束这次没有结果的进程，再用文件重定向重新编译。第一次编译卡在输出管道上，进程被中止，退出码不能当成编译结果。源码哈希没变。我改用文件重定向再编一次。第二次编译退出码是 0，但 `compileJava` 显示 UP-TO-DATE。我先核对生成的 class 是否晚于这次改过的源码。改过的源码都有更新的 class，第二次 `compileJava` 确认是最新的。接下来用这次的 main classes 编译并运行作者留下的定向检查。检查都已退出 0。我把命令、环境和结果写进编译报告。有效编译退出码是 0，`compileJava` 为 UP-TO-DATE。作者交回的 7 个定向检查退出码都是 0。生产源码没有改动。这不是额外正式复审。

报告在 `docs/开发记录/火种共享燃料改造-20261029/compile-report.md`。本单承接设计 2 次、实现 4 次，不增加正式检查轮次。本阶段复审额度已用完，本版未追加正式复审。

## 环境

工作目录 `D:\alianximod`。`JAVA_HOME=C:\Program Files\Java\jdk-17`，Java 17.0.6+9-LTS-190。`GRADLE_USER_HOME=C:\Users\baishang\.gradle`。Wrapper 是 Gradle 8.8。`gradle.properties` 里原有的 `-Xmx3G` 和 `org.gradle.daemon=false` 未改。命令带 `--offline --no-daemon`。Gradle 仍临时 fork 了一个单次 Daemon，构建结束时已停止。

## 源码

编译前没有其他 Gradle 或本仓库相关的 java 进程。作者列出的 25 个文件，SHA-256 与 `implementation-evidence.json` 一致，最后写入时刻也与记录的毫秒时间一致，没有晚于作者最后写入。最晚是 `FuelShare.java` 的 `2026-10-29T21:22:29.161+08:00`。编译后、检查后再对，哈希和写入时刻仍然一致。没有版本漂移。

## 编译

命令全文：

```text
.\gradlew.bat compileJava --offline --no-daemon
```

同一命令启动了两次。有效结果是第二次。

第一次从 `2026-10-29T21:29:15.687+08:00` 开始。输出管道卡住，`21:34:44` 中止包装进程，退出码 1。日志里没有 `error:`，也没有 `BUILD SUCCESSFUL` 或 `BUILD FAILED`。这个退出码是中止卡住的包装进程，不是编译器对源码的失败。当时 `compileJava` 已经把 class 写到 `build/classes/java/main`：本次主源码的 class 在 `21:29:37`–`21:29:38`，晚于对应源码；`21:29` 之后写入的 class 共 959 个。644 个主源文件里，只有两个 `package-info.java` 没有对应 class。

有效一次：`2026-10-29T21:36:10.282+08:00` 至 `2026-10-29T21:36:22.453+08:00`，退出码 0，`BUILD SUCCESSFUL in 11s`。`compileJava` 为 **UP-TO-DATE**，这一次没有重新编译。四个任务里 `downloadMcpConfig` 执行了 1 个，其余 UP-TO-DATE。这是离线构建图里的既有任务，不是新依赖下载。UP-TO-DATE 对应的就是上一轮已经生成、且新于作者源码的 main classes。

## 定向检查

没有使用旧的 `check-classes`。检查类单独编译，classpath 是本次 `build/classes/java/main` 加上 `build/classpath/runClient_minecraftClasspath.txt`。输出在 `verification/check-classes`。

`javac --release 17 -encoding UTF-8` 第一次因参数文件用了 UTF-8、JDK 按系统编码读取而退出码 3，没有生成检查类。同一 classpath 和源文件改用 GBK 参数文件后，`21:38:35.944` 至 `21:38:38.103`，退出码 0。

7 个 `main` 都在 `D:\alianximod` 运行，新检查类放在 Gradle main classes 之前：

| 入口 | 退出码 | 结果 |
| --- | --- | --- |
| `SharedFuelCheck` | 0 | 分配、同拍足额、不足按比例、补种、余额归零、先后手、去重、同拍不重复扣、焚天锁定、旧预付与版本区分通过。断言 121，失败 0 |
| `SixFireFunctionalCheck` | 0 | passed |
| `FireSeedCheck` | 0 | 断言 258，失败 0 |
| `BurnLedgerCheck` | 0 | 断言 106，失败 0 |
| `BurnDispatchCheck` | 0 | 回调挂载、回调丢弃、死亡、首段等待通过。断言 16，失败 0 |
| `FireReleaseStructureCheck` | 0 | 断言 43，失败 0 |
| `SixFireLogicCheck` | 0 | passed |

时间从 `21:38:38.121` 到 `21:38:40.575`。没有跑全量 `compileTestJava`，没有改 `SkinAttachmentChecks`。

## 未执行

没有做 `LivingEntity`、`CompoundTag` 或实体 NBT 往返，没有做无敌帧、护甲、抗火和真实 hurt，也没有真实实体存档、死亡事件、玩家克隆、地火 tick、数据包加载或启动游戏。这些缺口来自检查程序自己的输出，没有另写一套测试。

相关进程已结束，源码未改。没有需要交回作者改源码的编译或检查失败。第一次 `compileJava` 的退出码 1 是输出管道卡住后被中止的包装进程，不是源码编译失败。随后同一命令已重新跑完：退出码 0，`compileJava` 为 UP-TO-DATE，7 个定向检查退出码都是 0。结果已写在 `docs/开发记录/火种共享燃料改造-20261029/compile-report.md`。
