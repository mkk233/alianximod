# 陨岩实体命中续飞：最终编译

本次对已经收束的源码执行 `compileJava`。退出码 0。不是新增正式复审，也没有进游戏。

会话编号：`c944df97-5698-4cb3-ae72-8adf3edc9b0a`。

## 编译前确认

`implementation-state.json` 已是 completed，退出码 0。`implementation-result.json` 里 `source_writes_closed` 为 true。本目录没有 repair 最终反馈。

已有终端记录都不是正在运行的命令。编译前查询 Gradle 相关进程，`gradle_busy=0`。

## 上次命名门禁被拒绝的原始反馈

实现阶段的 Shell 工具返回正文只有一行 `Rejected:`，冒号后没有文字。没有拒绝类型、分类理由、权限名称或退出码。工具反馈本身没有写成用户未授权。

被拒绝的命令依次是：

1. `powershell -NoProfile -File tools/check-name-gate.ps1`
2. 同一条命令再一次
3. `echo name-gate-start`
4. `java -version`
5. `powershell -NoProfile -File tools/check-name-gate.ps1`

工作目录都是 `D:\alianximod`。

## 这次命名检查

`tools/check-name-gate.ps1` 没有参数。这次在项目根执行 `& .\tools\check-name-gate.ps1`，退出码 1。

失败只有未改文件里的旧名字：

- `src/main/java/com/bslx/XueXiMod/animation/runtime/OriginalHumanoid.java:165` 变量 `i`
- 同文件第 202 行变量 `to`

本次陨岩改动的文件没有出现在失败列表里。本阶段没有为门禁改源码。

## 编译

工作目录：`D:\alianximod`

命令：`.\gradlew.bat compileJava --offline --no-daemon`

仅本次进程：

- `JAVA_HOME=C:\Program Files\Java\jdk-17`
- `GRADLE_USER_HOME=C:\Users\baishang\.gradle`
- `java version "17.0.6" 2023-01-17 LTS`

`gradle.properties` 原有 `org.gradle.jvmargs=-Xmx3G` 和 `org.gradle.daemon=false`，没有改。没有使用临时空缓存，没有打包，没有 `runClient`。

- 开始：`2026-11-02T00:22:13.1682455+08:00`
- 结束：`2026-11-02T00:22:49.4527498+08:00`
- 退出码：0
- Gradle 文本：`BUILD SUCCESSFUL in 35s`
- 任务：`4 actionable tasks: 2 executed, 2 up-to-date`
- `:compileJava` 实际执行，不是 UP-TO-DATE。`:downloadMcpConfig` 也执行了。`:extractSrg` 和 `:createMcpToSrg` 为 UP-TO-DATE。
- javac 实际跑过：注解处理器输出了映射和 refmap，并给出 40 条弃用警告，没有 error。

控制台把 javac 的中文诊断按错误编码显示成乱码。可读部分是 `ResourceLocation` 构造和 `ProjectileImpactEvent.setCanceled` 的 removal 警告，以及 `40` 条警告。这些警告不在本次陨岩改动文件上，也不导致失败。

## 日志

```
JAVA_HOME=C:\Program Files\Java\jdk-17
GRADLE_USER_HOME=C:\Users\baishang\.gradle
java_version:
java version "17.0.6" 2023-01-17 LTS
Java(TM) SE Runtime Environment (build 17.0.6+9-LTS-190)
Java HotSpot(TM) 64-Bit Server VM (build 17.0.6+9-LTS-190, mixed mode, sharing)
compile_start=2026-11-02T00:22:13.1682455+08:00
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.8/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build

> Configure project :
Java: 17.0.6, JVM: 17.0.6+9-LTS-190 (Oracle Corporation), Arch: amd64

> Task :downloadMcpConfig
> Task :extractSrg UP-TO-DATE
> Task :createMcpToSrg UP-TO-DATE

> Task :compileJava
[注解处理器的中文“注:”在捕获里显示为乱码。可读英文如下。]
SpongePowered MIXIN Annotation Processor Version=0.8.5
Supported obfuscation types: ObfuscationServiceMCP supports [notch] ObfuscationServiceFG3 supports [searge]
Loading searge mappings from D:\alianximod\build\createMcpToSrg\output.tsrg
Writing refmap to D:\alianximod\build\tmp\compileJava\compileJava-refmap.json
Writing searge composite mappings to D:\alianximod\build\tmp\compileJava\compileJava-mappings.tsrg
[随后是 40 条 [removal] 警告。捕获里的中文句子是乱码，英文符号可读：ResourceLocation 构造，以及 HitVolumes.java:208 的 ProjectileImpactEvent.setCanceled(boolean)。文件包括 DomainMistRainRenderer、DomainMistRingRenderer、DomainMistShaders、DomainPressureRenderer、DomainWorldRenderer、EntityRendererFix、FireSeedFlameClient、FireVisualClient、FurnaceBodyVisual、BurnSpatialMount、LiaoliRingTrail、LiaoliV6Renderer、NetherDomainWisps、PrisonSyncPacket、PrisonShellRenderer、SwordEntityRenderer、TechniqueHitRenderer、WaterBarrierRenderer、WaterPartingRenderer、WaterRibbonRenderer、WaterPrisonEffect、FireDamage、SteamBurst、HitVolumes、PrisonPressure、ResourceStatuses、FireSeedState、WetnessFlightExempt。结尾警告计数在捕获里是乱码，数字是 40。]

BUILD SUCCESSFUL in 35s
4 actionable tasks: 2 executed, 2 up-to-date
compile_end=2026-11-02T00:22:49.4527498+08:00
compile_exit=0
```

## 未实测

编译通过只说明当前源码能编过。游戏里命中实体后续飞、再次重叠不重复结算、直接撞地、超时残骸、撞块安放失败、外观连续和区块卸载都还没有实测。
