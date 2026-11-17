# 启动故障代码层 QA（2026-10-26）

基线不是 git HEAD。QA 改检查前的快照在任务目录 `before-qa-snapshot/`，时间 `2026-10-26T04:01:06+08:00`。负对照 class 用主控在重编译前保存的 `startup-negative-fixtures/`，manifest SHA 与文件一致。没有跑 `jar`、`build`、`check`、`runClient`、`runServer`，没有启动游戏。编译通过不是启动证明。

## A 编译

先确认 `editing-done-startup-fix.json` 的 `state` 为 `editing-done`，且 `fix-writer-status.json` 为 `finished`。

```
JAVA_HOME=C:\Program Files\Java\jdk-17
GRADLE_USER_HOME=C:\Users\baishang\.gradle
.\gradlew.bat compileJava --offline --no-daemon --init-script tools/startupqa/print-classpath.init.gradle
```

工作目录 `D:\alianximod`。Java 17.0.6。退出码 0。`BUILD SUCCESSFUL in 22s`。4 个任务里 2 个执行、2 个 UP-TO-DATE。Mixin 0.8.5 注解处理器写出 `build/tmp/compileJava/compileJava-refmap.json`。16 条是原有的 `ResourceLocation` 和 `setCanceled` 过时警告，没有扩修。

命名门禁：

```
powershell -NoProfile -File tools/check-name-gate.ps1
```

退出码 1。只有原有 `OriginalHumanoid.java:165 i` 和 `:202 to`。本次动过的测试没有新违规，没有改那两处。

## B 真实依赖、目标字节码、refmap

```
powershell -NoProfile -File tools\startupqa\run-startup-checks.ps1
```

最终一次退出码 0，`FAILED_COMMANDS 0`。日志在任务目录 `qa-logs/`。`StartupMixinGate` 退出码 0，断言 35，失败 0。

负对照 SHA256：

- `ArrowDeflectionMixin.class` `96BA2B4C0AD76CFE4D215EF2D9E62BF77E58E586F69B2759ACBA5967E603C939`
- `ArrowCaptureBook.class` `21594F6F5919BA3B01DC4AD9819D87728090C1BF2AD1ED7EEDF5B9AE9FAAF5C7`

修后 `ArrowDeflectionMixin.class` SHA256 `519EC002A41A6FBFA4766F1F7401E4CFD1459D7EDA6A0405438B5636AB025EC7`，与负对照不同。

本机 `mixin-0.8.5.jar` 的 `MixinApplicatorStandard.checkMethodVisibility` 指令仍是：`bipush 8`、`iconst_2`、`sipush 4096`、可见 `@Overwrite` 才跳过，否则抛 `non-private static method`。`applyNormalMethod` 对名字以 `<` 开头的方法跳过这次检查。负对照的公开静态 `setHeld` 和 `isHeld` 被这条规则拒绝。负对照 `ArrowCaptureBook` 有 3 处 `invokestatic ArrowDeflectionMixin.setHeld`。

修后 mixin 列表里没有会被这条规则拒绝的普通公开静态方法。识别出 6 个 Accessor 接口，含 `EntityRandomAccessor`。Accessor 符号引用 23 处被放行，没有按 mixin 包名整体禁止。包外类不再 `invokestatic` 或用其它符号引用实现 Mixin。

`ArrowDeflectionMixin` 实现公开接口 `ArrowCaptureAccess`。方法是 `xuexiSetCaptured(Z)V` 和 `xuexiIsCaptured()Z`，都带 `@Unique`。`ArrowCaptureBook` 有 3 处 `invokeinterface xuexiSetCaptured`，没有残留的 `invokestatic`。Mixin 内部实例调用 setter 1 处、getter 3 处。

`AbstractArrow` 的 `defineId` 只在 `ArrowDeflectionMixin.<clinit>` 出现一次。`XUEXI_CAPTURED` 是 private static。`defineSynchedData` 路径只 `define` 这同一个字段，该处没有第二份 `defineId`。其它实体自己的 `defineId` 不计入这一条。

`ArrowFlight.consider` 有 1 处 `fconst_0` 紧接 `rememberPending`，待定保存共 3 处。这是指令形状，不是 `consider` 已执行。

映射 recomp `forge-1.20.1-47.3.33_mapped_parchment_2023.06.26-1.20.1-recomp.jar` 计数都是 1：

- `AbstractArrow.tick` 的 `findHitEntity(Vec3,Vec3)EntityHitResult`
- `AbstractArrow.onHitEntity` 的 `RandomSource.nextInt(I)I`
- `AbstractArrow.onHitEntity` 的 `Entity.hurt(DamageSource,F)Z`
- `Mob.doHurtTarget` 的 `EnchantmentHelper.getFireAspect(LivingEntity)I`
- `Mob.doHurtTarget` 的 `Entity.hurt(DamageSource,F)Z`
- `ForgeHooks.onLivingBreathe` 的 `LivingEntity.hurt(DamageSource,F)Z`

`defineSynchedData` 和 `canHitEntity` 方法存在。

编译 class 上的注解各 1 份：`ArrowAuraMixin` 的 `ModifyArg`、`ArrowDeflectionMixin` 的 `ModifyArg`、`ArrowDeflectionMixin` 的 `Redirect`，方法都是 `onHitEntity`，目标都是 `Entity.hurt(DamageSource,F)Z`。refmap 的 `mappings` 和 `data.searge` 都有对应的 hurt、`nextInt`、`findHitEntity`、`onHitEntity`、近战火焰附魔和溺水 `LivingEntity.hurt`。

这三份注解、recomp 里的 1 次调用、refmap 条目是三件分开的事。目标存在不等于三份注入已经组合成功。

## C 生产纯 helper 和内存账

同一次脚本里，下面的 `main` 都是退出码 0。它们调用的是生产类，没有造第二套算法，也没有构造 `ItemStack`、`ServerPlayer` 或世界。

| 检查 | 退出码 | 断言 |
|---|---|---|
| `DeflectionCoreCheck` | 0 | 98 |
| `DeflectionProjectileCheck` | 0 | 63 |
| `HostGeometryCheck` | 0 | 99 |
| `CrescentIdentityCheck` | 0 | 109 |
| `TideMarkSettlementCheck` | 0 | 69 |
| `RadialCopyPlaneCheck` | 0 | 689 |
| `StartupDeflectionRegression` | 0 | 17 |
| `ChainStepContactCheck` | 0 | 10 |

合计 1154。`DeflectionCoreCheck` 的账是内存账，不是 `WaterMomentum` 玩家账。`DeflectionProjectileCheck` 里的 `ownership()` 是源码禁词扫描，不是调用链执行；同一 `main` 里的报价、次序和标签断言是直接调用生产方法。

`StartupDeflectionRegression` 直接调用 `ArrowDamageFreeze.freeze` 和 `ArrowBook`：

- 基数 0、暴击、roller 返回 0：只调用 1 次，上界 2，直接伤害 0。
- roller 返回 1：仍只调用 1 次，直接伤害 1，`quote(40, 1, 0, false)` 为 `FULL`、费用 1。
- 非暴击基数 0 不调用 roller。
- `rememberPending(0)` 可重复读到 0，`replacesHurt` 为真，`hasFrozen` 为假；`consumePending` 后消失；`clearPending` 后不留到下一次；`NaN` 不进入待定。
- `quote(40, 0, ...)` 为 `NONE`、费用 0，不是完整或部分；`applyCommit` 不写冻结标记。

`CrescentIdentityCheck` 读到缺月和垂丝的生产定义都是重水 0、重压门槛 0、潮湿 6。垂丝仍是函数弹道、命中体积、寿命 40、速度 0、厚度 0.35、攻击角色。深海重压领域还在，重水和重压门槛仍是 0。

`TideMarkSettlementCheck` 含单条潮印平伤 2、湿 5、势 2。`ChainStepContactCheck` 用生产 `ChainSteps` 和 `ContactCast`：突刺加断潮是两步；潮印加水幕同一步且接触伤害 2、拒绝文本为空；潮印加剑来同一步，但扣费前被「还不能承载」拒绝。

## D 未覆盖

上一轮整套脚本里，`StartupMixinGate` 退出码 0。当时 gate classpath 只有 ASM、Mixin 和 gate 产物，没有已有的 `compileClasspath`。`MixinService.getService` 抛 `InvocationTargetException`，原因是 `NoClassDefFoundError: com/google/common/base/Strings`。那一次只调用了 `getService`，没有调用 Transformer。没有补 Guava 去凑服务，没有 Bootstrap，没有伪造转换器。因此没有变换后的 `AbstractArrow`，启动证明仍是否。

`ArrowFlight.consider` 的 0 报价分支没有执行，因为它要 `ServerLevel` 和箭实体。`hurtAmount`、箭穿透、捕获同步、网络包、真实 `WaterMomentum` 预留提交退款都没有跑。

`onHitEntity` 上三份 hurt 注入的组合应用没有执行。逐项目标计数和注解存在都不能代替它。

## 补试：真实 Mixin 服务可用性探测

没有重跑 `compileJava`，没有重跑上面 8 组 helper。本轮改检查前的快照在任务目录 `before-qa-probe/`，时间 `2026-10-26T04:15:12+08:00`。`tools/startupqa/run-startup-checks.ps1` 的 gate 运行 classpath 改为指定的 ASM 9.7.1、asm-tree 9.7.1、mixin-0.8.5、`build/startupqa-gate`，后面再接已有 `compile-classpath.txt` 全文。本轮没有执行这个整套脚本。

工作目录 `D:\alianximod`，`JAVA_HOME=C:\Program Files\Java\jdk-17`。

```
javac --release 17 -encoding UTF-8 -cp asm-9.7.1.jar;asm-tree-9.7.1.jar -d build\startupqa-gate tools\startupqa\StartupMixinGate.java
java -Dfile.encoding=UTF-8 -cp asm-9.7.1.jar;asm-tree-9.7.1.jar;mixin-0.8.5.jar;build\startupqa-gate;<compile-classpath.txt> StartupMixinGate
```

参数文件是 `qa-logs/probe-gate-javac-args.txt` 和 `qa-logs/probe-gate-java-args.txt`。日志是 `qa-logs/probe-javac.txt` 和 `qa-logs/probe-gate.txt`。javac 退出码 0。`StartupMixinGate` 退出码 0。断言 35，失败 0。末行仍是 `启动证明 否`。

Guava 的 `Strings` 这次不再缺失。`MixinService.getService` 仍抛 `InvocationTargetException`，原因是 `ServiceNotAvailableError: No mixin host service is available. Services: INVALID[ModLauncher] and 1 other invalid services.` 同一段日志写明 `MixinServiceLaunchWrapperBootstrap` 不可用，因为 LaunchWrapper is not available；`MixinServiceModLauncherBootstrap` 不可用，因为 ModLauncher is not available。

这是真实 Mixin 服务可用性探测。探测在宿主缺失处停止。没有调用 Transformer，没有造 harness，没有 Bootstrap。真实组合注入仍未验证。
