# compile-02 final

- 状态：编译通过。源码在 compile-02 期间没有再改。未打包，未启动游戏，未做正式复审。
- 任务：taskId=projectile-hit-consumption，attemptId=compile-02。
- 前置：repair-import-01 已先补回 `import net.minecraft.world.entity.Entity;`，回执写完后源码哈希未变，然后才开始本编译。
- compile-01 的失败日志和回执未覆盖。

## 命令

- 工作目录：`D:\alianximod`。
- 命令：`.\gradlew.bat compileJava --offline --no-daemon`。
- 配置：`org.gradle.jvmargs=-Xmx3G`，`org.gradle.daemon=false`。本次未改配置。
- JDK：PATH 上的 `java` 为 Oracle 17.0.6+9-LTS-190。当前 shell 未设置 `JAVA_HOME`。
- 开始：2026-11-11 16:50:57.599。结束：2026-11-11 16:51:21.740。
- 退出码：0。日志写明 `BUILD SUCCESSFUL in 23s`，4 个任务中 2 个执行、2 个 UP-TO-DATE。
- 日志：`D:\alianximod\docs\开发记录\弹丸命中消耗-20261111\compile-02.log`。

## 结果

- 错误：0。compile-01 的 `BatchFlightBook.java:451` 找不到 `Entity` 没有再出现。
- 警告：53 条既有 removal/deprecation 提示，日志末尾汇总为 `53 个警告`。本次未按警告改代码。
- Gradle 为遵守 `-Xmx3G` 临时分叉了单次 Daemon，并在构建结束时停止。编译前和编译后都没有 `java.exe` 或 `javaw.exe`。

## 源码稳定

编译前后 `src\main` 都是 1345 个文件，逐行对比差异数为 0。

- 编译前清单：`compile-02-src-before.txt`，汇总 `6786FB4F5A325A9AD196315501447D56D49FB743A38D9582A9B4B3D5939F80E9`。
- 编译后清单：`compile-02-src-after.txt`，汇总同样是 `6786FB4F5A325A9AD196315501447D56D49FB743A38D9582A9B4B3D5939F80E9`。

最终七文件 SHA-256 与 repair-import-01 回执一致：

- `FAB51BEA77C563E4ADEB572A981B7E62120527E04A57944C733566F54F3F8AEC`，2026-11-11 16:39:32.218，`BatchSparkEffect.java`。
- `DA98A74B2D9C9B256410C16B2845C68E7FEC4D38E0659CA9D7EDA42ABDA4B718`，2026-11-11 16:50:09.853，`BatchFlightBook.java`。
- `C6F7B55B73C875B15536DF4AF8273F8CCB7E69CA87E7F36E7AEFC3615D5B6D04`，2026-11-11 16:41:52.197，`BatchCarrierVisual.java`。
- `384B1307B9F361D2528DD6C44CCBE0F300CB6C3E540FADA0E1D6F424BBAD25F7`，2026-11-11 16:39:18.124，`TechniqueHitSpec.java`。
- `E6A8AC90C2CA1AB94E61C5C4D4F7007A4E8BB0CDA948610019615D020345195A`，2026-11-11 16:43:19.239，`TechniqueHitEntity.java`。
- `54C4FB88E353C93FE8190DE9A15FCFB604FB9B3925930DBBD35356A92C322AEE`，2026-11-11 16:43:36.263，`TechniqueHitSettle.java`。
- `8E712A44F99C63F45CA57AB2D0CF960119604420E0EEB2EDC80709A2036CB870`，2026-11-11 16:42:32.856，`FireOrbEffect.java`。

没有下属代理。源码写入在编译前已经停止。未写制作表，未写原批次记录。

## 下一步

父方接收本回执后，交独立写回者同步记录和满足条件的表状态。
