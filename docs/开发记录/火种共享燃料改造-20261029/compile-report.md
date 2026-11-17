# 火种共享燃料改造统一编译报告

执行者：本轮唯一统一编译。模型 Cursor CLI grok-4.7-high-fast。本单承接设计 2 次、实现 4 次，不增加正式检查轮次，不另设测试阶段。编译与定向检查的退出码不是本版额外正式复审结论。本阶段复审额度已用完，本版未追加正式复审。

生产源码、主设计文档、制作表、旧批次记录未改。没有 `build` / `jar` / `reobf`，没有 `runClient`，没有启动游戏，没有提交。全部 Gradle、javac、检查进程已结束。

## 环境

- 工作目录：`D:\alianximod`
- `JAVA_HOME=C:\Program Files\Java\jdk-17`
- `java version "17.0.6" 2023-01-17 LTS`，`Java(TM) SE Runtime Environment (build 17.0.6+9-LTS-190)`
- `GRADLE_USER_HOME=C:\Users\baishang\.gradle`
- Wrapper：`gradle-8.8-bin.zip`。成功日志里的 Gradle 文档版本为 8.8。配置项目打印 `Java: 17.0.6`
- `gradle.properties` 原有 `org.gradle.jvmargs=-Xmx3G`、`org.gradle.daemon=false`，未改。未改 Gradle 配置，未要求下载新依赖。命令带 `--offline --no-daemon`。Gradle 8.8 仍按自己的规则临时 fork 了一个单次 Daemon，并在构建结束时停止。

## 编译前

作者列出 25 个文件。编译前没有其他 Gradle、javac 或本仓库相关 java 进程。

SHA-256 与 `implementation-evidence.json` 全部一致。最后写入时刻与作者记录的毫秒时间一致，没有晚于作者最后写入。最晚文件是 `src/main/java/com/bslx/XueXiMod/status/fire/seed/FuelShare.java`，`2026-10-29T21:22:29.161+08:00`。对照在 `verification/pre-hash-rerun.txt`。

## 命令与结果

命令全文：

```text
.\gradlew.bat compileJava --offline --no-daemon
```

工作目录 `D:\alianximod`。环境变量如上。

同一命令有两次启动。有效结果是第二次。

1. `2026-10-29T21:29:15.6869303+08:00` 启动。标准输出和标准错误用单线程管道读取，包装进程卡住。`21:34:44` 结束该包装进程，退出码 1。日志停在 `compileJava` 的过时 API 警告，没有 `error:`，没有 `BUILD SUCCESSFUL` 或 `BUILD FAILED`。这个退出码是中止卡住的包装进程，不是编译器对源码的失败结论。日志：`verification/gradle-compile-aborted-pipe.log`。这次 `compileJava` 已经把 class 写到 `build/classes/java/main`：抽查的本次主源码 class 时间是 `21:29:37`–`21:29:38`，晚于对应源码；`21:29` 之后写入的 class 共 959 个。644 个 `src/main/java` 源文件里，只有两个 `package-info.java` 没有对应 class。
2. 有效一次：`2026-10-29T21:36:10.2815393+08:00` 至 `2026-10-29T21:36:22.4530269+08:00`。标准输出和标准错误写入 `build/shared-fuel-compile-stdout.log`、`build/shared-fuel-compile-stderr.log`，再汇总到 `verification/gradle-compile.log`。退出码 0。`BUILD SUCCESSFUL in 11s`。`compileJava` 为 **UP-TO-DATE**，没有在这一次重新执行编译。四个任务里 `downloadMcpConfig` 执行了 1 个，其余 3 个 UP-TO-DATE。这是离线构建图里的既有任务，不是新依赖下载。

UP-TO-DATE 对应的就是上一轮已经生成、且新于作者源码的 `build/classes/java/main`。编译后 25 个文件 SHA-256 与写入时刻仍与作者记录一致，见 `verification/post-hash.txt`。没有版本漂移，没有混用另一份源码。

## 定向检查

没有使用旧目录 `check-classes`。检查类单独 `javac`，classpath 为本次 `build/classes/java/main` 加上 `build/classpath/runClient_minecraftClasspath.txt`（116 个已有 jar）。输出目录：`verification/check-classes`。

第一次 `javac` 把参数文件写成 UTF-8，JDK 按系统编码读到输出路径时退出码 3（`MalformedInputException`），没有生成检查类。随即用同一 classpath、同一源文件，把参数文件改成 GBK 再编一次。

成功的检查编译：

```text
javac --release 17 -encoding UTF-8
```

- 开始 `2026-10-29T21:38:35.9440586+08:00`
- 结束 `2026-10-29T21:38:38.1032479+08:00`
- 退出码 0
- 注解处理只有 Mixin 说明，没有错误
- 日志：`verification/javac-checks.log`

运行入口是下面 7 个 `main`，工作目录 `D:\alianximod`，classpath 把 `verification/check-classes` 放在 Gradle main classes 之前。没有跑 `compileTestJava`，没有改 `SkinAttachmentChecks`。

| 入口 | 起止 | 退出码 | 输出 |
| --- | --- | --- | --- |
| `com.bslx.XueXiMod.status.fire.seed.SharedFuelCheck` | 21:38:38.121–21:38:38.488 | 0 | 分配、一层两段、两层同拍充足、不足按比例、进行中补种、余额归零不复活、先后手、挂载去重、同拍不重复扣、焚天锁定、旧预付与恢复、版本区分均通过。断言 121，失败 0 |
| `com.bslx.XueXiMod.status.fire.seed.SixFireFunctionalCheck` | 21:38:38.493–21:38:38.859 | 0 | `six fire functional check passed` |
| `com.bslx.XueXiMod.status.fire.seed.FireSeedCheck` | 21:38:38.862–21:38:39.303 | 0 | 断言 258，失败 0 |
| `com.bslx.XueXiMod.shu.technique.release.fire.BurnLedgerCheck` | 21:38:39.319–21:38:39.711 | 0 | 预检、冲突、节奏、快照、恢复、燎原、燎里、焚天、旧档案输入失败均为 0。断言 106，失败 0 |
| `com.bslx.XueXiMod.shu.technique.release.fire.BurnDispatchCheck` | 21:38:39.714–21:38:40.065 | 0 | 回调挂载、回调丢弃、死亡、首段等待通过。断言 16，失败 0 |
| `com.bslx.XueXiMod.shu.technique.release.fire.FireReleaseStructureCheck` | 21:38:40.068–21:38:40.327 | 0 | 运行源码、标签失败均为 0。断言 43，失败 0 |
| `com.bslx.XueXiMod.shu.technique.release.firecast.SixFireLogicCheck` | 21:38:40.330–21:38:40.575 | 0 | `six fire logic check passed` |

作者准备的这些入口覆盖了共享扣款、同拍足额与不足分配、补种、引爆先后、重复回调、焚天锁定以及旧预付/版本区分。检查后 25 个源文件哈希和写入时刻仍然与作者记录一致。

## 未执行

- `LivingEntity`、`CompoundTag` 或实体 NBT 往返
- 无敌帧、护甲、抗火、真实 hurt
- 真实实体存档、死亡事件、玩家克隆、生物转化、牛奶、服务端主线程拒绝
- 玩家事务、实体命中、地火 tick、混链函数、一分为三复制份
- 卸载暂停的真实时钟
- 数据包加载、标签合并
- 全量 `compileTestJava`（历史被 `SkinAttachmentChecks` 旧错误挡住，本单不修该文件）
- 打包、`runClient`、启动游戏

这些缺口沿用检查程序自己的输出，没有为补覆盖另写算法或新测试。

## 收束

- 有效编译：退出码 0，`compileJava` UP-TO-DATE，源码与作者交回版本一致。
- 定向检查：7 个入口退出码都是 0。
- 没有源码修改。相关进程已结束。
- 没有真实阻塞需要交回作者改源码。
- 本结果不是额外正式复审通过。
