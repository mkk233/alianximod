# 火系六式测试阶段执行

本文件是功能测试执行记录。不是测试正式结果核对，不消耗测试正式校验（仍为 0/2）。设计 2/2、实现 2/2 不追加。没有通过结论，没有迁移制作表，没有改生产源码、语言、游戏资源或 Gradle 配置。不符合项留在制作中，本单不自行修复。

## 入口与停写

实现报告、已知限制、平面修补、贴图接入、炉心原子和编译修复都已交回，原作者停写早于 09:08。R2/R3 返修有作者报告和底座编译收口，记录写明未追加实现复审。实现额度用完不拿来挡住六项测试，这些未追加复审的项也不改记成新的测试结论。

现行停写证据是本单启动依据里的用户原话：「修改已结束，当前没有其他源码写入」。它覆盖 `FireHurtMixinPlugin` 和已移到 `shu.technique.release.fire` 的 `FireHurtBoundary`。不退回 04:24 字节。

文件占用：本会话 `b2d5b359-42e3-4a90-9d24-b26acec6d5f8` 是本阶段唯一测试源码作者和编译执行者。只新增 `src/test/java/com/bslx/XueXiMod/status/fire/seed/SixFireFunctionalCheck.java`。表和 `记录.md` 仍由记录者写，本单没动。生产源码在编译前采样后到测试结束，654 条哈希无差异。

采样 `test-stage-pre-sources.txt` 时间 2026-10-29T10:48:53.5100907+08:00。关键源码：

| 路径 | SHA256 | mtime | 字节 |
| --- | --- | --- | --- |
| `mixin/FireHurtMixinPlugin.java` | `EA0A228403E5B01D5FA38AEE67EB73AB59655B77E89583FBE2F2F399C5A8B850` | 09:08:29.665 | 1556 |
| `shu/technique/release/fire/FireHurtBoundary.java` | `3EFD234372FBFE88492C46897B9F3975A6F341F77EDC56A908D58002620DFCEC` | 09:08:29.665 | 5554 |
| `SixFireFunctionalCheck.java` | `C5D3F14B735DE6CB2FB9D8127A291533A341762B1619C70CB73D44F32EC915B9` | 10:51:00.610 | 22301 |

`mixin/FireHurtBoundary.java` 不存在。编译结束时对应 class 仍是 09:09:18 和 09:09:19，没有被这次 Gradle 重写。

## 编译

命令：`.\gradlew.bat compileJava --offline --no-daemon`

环境：`JAVA_HOME=C:\Program Files\Java\jdk-17`，`GRADLE_USER_HOME=C:\Users\baishang\.gradle`，既有 `-Xmx3G`。工作目录 `D:\alianximod`。日志 `test-stage-gradle.log`，标准输出和错误都进该文件。

开始 2026-10-29T10:48:53.8536979+08:00，结束 10:49:07.6793155+08:00，退出码 0。Java 17.0.6。`compileJava` 为 UP-TO-DATE，BUILD SUCCESSFUL in 13s。Gradle 认定当前源码对得上已有 main class，没有把 04:24 的旧输入当成这次结果。当时有一个 09:04 起的空闲 Gradle 8.8 daemon；本命令使用 `--no-daemon`，单次进程结束时自行停止。没有边写生产源码边编译。

没有跑 `build`、`jar`、`reobf`、`runClient`，没有启动游戏。没有跑全量 `compileTestJava`。名字门禁只剩既有 `OriginalHumanoid.java` 的 `i` 和 `to`，本次测试文件没有新违规，那两处没改。

定向 javac：`javac --release 17 -encoding UTF-8`，classpath 为 `build/classes/java/main`、jetbrains annotations 24.0.1、asm 9.7.1 五件套。输出目录 `test-stage-check-classes`。第一次因没把同包的 `HurtBoundaryFixture`、`HurtBoundaryHook` 放进源列表而失败，不是断言被改写。补上后 10:50:04–10:50:06 退出 0。热量循环修正后只重编 `SixFireFunctionalCheck`，10:51:14 退出 0。

## 用例

运行 classpath 是上述检查目录加 main class、annotations 和 asm。`java -Dfile.encoding=UTF-8`。工作目录 `D:\alianximod`。汇总 `test-stage-run-summary.txt`。

新增 `SixFireFunctionalCheck` 调用 `ReleaseDefinitions.registerAll`、`ReleaseRegistry.get`、`FireSeedAccount.grant/consume`、`FireConsumerPlans`、`FireLayer.combine`、`BurnSchedule.of`、`BurnLedger.admit/advance/conflicts`、`FurnaceMath.transition/advanceOnline/seedMultiplier/allowsServerWork`。10:50:22 第一次退出 1，两条失败是测试把 `advanceOnline` 的 150 当成一次爬完；该函数一次调用只加一拍。已改成连续调用 150 次和 100 次，预期仍是 0.30 和 0.60。10:51:15–10:51:15.804 重跑退出 0。第一次失败留在 `test-stage-run-SixFireFunctionalCheck-first.err`。这不是交回原作者的生产缺陷。

已执行的功能结果：

- 六式都进了 `ReleaseRegistry`。燎原寿命 200、魔力 60、攻击、不拒绝混链、不要求持剑。燎里持续、跟随准星、寿命 40、魔力 40、厚度 0.6、要求持剑。焚天寿命 40、魔力 100、攻击。升温、燃烧、冷却是自身状态、动作、拒绝混链，魔力 20/20/0，装配消耗 1，炉心动作分别是进入普通、进入或升级过载、主动冷却。`rule_dual_release` 是双重，`rule_triple_cast` 是一分为三。这是注册数据，不是一次链结算。
- 火种账本可离线构造。0 不消费，三份计划都空。9 点全扣，档 0 余 9，燎原 3 段、间隔 20、单段 2，焚天 1 个尾段，燎里引爆 `0.36×9`。10 点燎原 4 段，焚天一段 2.4。54 点为 5 完整段加尾段，尾段等于完整段乘 4/10。100 封顶，燎原 13 段每段 4，焚天 10 段。段打完后轨道结束，推进次数等于段数乘间隔。
- 炉心 0.30 只经 `FireLayer.combine` 合入一次。满火种燎原单段 4.6，不是再加一次 0.30。燎里火伤 46.8，分量基础 36，不含物理刺击 6。焚天同层加算，十段锁定同一伤害。
- 同一余额先到者扣完 40，后到者读到 0。同一上下文不能再扣、不能再产。新上下文可以记入后续 15 点，旧上下文拿不走，下一次上下文可以扣。封顶后再产，余额停在 100。
- 燎原调用点的排他参数是 false：同施法者第二份挂不上，另一施法者可以再挂。焚天调用点是 true：第二份挂不上。
- 炉心纯转换：无炉心进入普通或过载都从热量 0 开始；已有普通不刷新；升级保留 0.30；已过载失败且热量不变；过载不能降成普通；主动冷却清档并清热量；没有炉心时冷却失败。连续 150 次在线步进到 0.30，满普通再连续 100 次过载步进到 0.60。一次调用即使传入 150 也只加一拍。普通产率从 1.12 到 1.60，过载固定 2.2。`allowsServerWork` 要求服务端且同线程。

其余入口都在 10:50:22–10:50:26 退出 0，用的是这次 main class：

| 入口 | 退出码 | 类别 | 实际 |
| --- | ---: | --- | --- |
| `FireSeedCheck` | 0 | 功能为主 | 断言 244 失败 0。火种运算和账本。 |
| `SixFireLogicCheck` | 0 | 混合 | 计划、层、圆盘、球体、走廊是函数调用；源码不含六个 id 的文本不单独当功能通过。 |
| `LiaoliV6LayoutCheck` | 0 | 混合 | 燎里几何函数通过；含源码文本。 |
| `FireVisualLayoutCheck` | 0 | 混合 | 波次、脉冲、炉心帧函数通过；含源码文本。 |
| `KnownLimitsCheck` | 0 | 混合 | `GroundFireMath.openTop` 和 `SelfStatusOrder.decide`。顺序函数不是整条玩家事务。 |
| `FurnaceAtomicCheck` | 0 | 混合 | `FurnaceMath.transition`、`FurnaceInstall`。安装规划不证明玩家事务成功。 |
| `FurnaceMathCheck` | 0 | 功能 | 断言 50 失败 0。爬坡、离线只加一拍、转换。 |
| `FurnaceRepairCheck` | 0 | 混合 | 断言 15 失败 0。自注没有真实服务端线程和属性写入。 |
| `FurnaceStructureCheck` | 0 | 源码文本 | 断言 19 失败 0。 |
| `FireReleaseStructureCheck` | 0 | 源码文本 | 断言 41 失败 0。 |
| `BurnLedgerCheck` | 0 | 功能 | 断言 121 失败 0。含燎原、燎里、焚天账本。 |
| `BurnDispatchCheck` | 0 | 功能 | 断言 16 失败 0。 |
| `FireLayerCheck` | 0 | 功能 | 断言 45 失败 0。 |
| `ElementHurtLifecycleCheck` | 0 | 功能 | 断言 16 失败 0。令牌正常、早退、异常。 |
| `FireCooldownCheck` | 0 | 混合 | 断言 16 失败 0。含钩子文本。 |
| `FireHurtBytecodeCheck` | 0 | 功能 | 断言 26 失败 0。读当前 `recomp.jar`，套上现行 `FireHurtBoundary`。不是游戏内 Mixin。包引用仍在 `release.fire`，没有改断言。 |

正常玩法仍没有 `FireSeedState.grant` 的主源码调用方，也没有命令。这和上面的离线账本构造分开记。

## 六个编号

| 编号 | 状态 | 已执行 | 缺口 |
| --- | --- | --- | --- |
| `fire_liaoyuan` | 部分覆盖 | 注册、0/9/10/54/100 的附着计划、间隔 20、炉心一次、共享账本、同施法者排他 | 地表每 20 tick 的 2 点需要实体 tick。火场铺设、离开后继续烧、视觉、正常玩法火种都未执行。 |
| `fire_liaoli` | 部分覆盖 | 注册与持剑标志、线性引爆、炉心只加在火伤 | 物理刺击 6 点和每目标一次命中需要实体。手持剑门、收招、视觉未执行。 |
| `fire_fentian` | 部分覆盖 | 注册、尾段、封顶、4 tick 间隔、十段锁定、目标排他 | 8 格选实体、离开范围后的实体燃烧、视觉、正常玩法火种未执行。 |
| `fire_heat_up` | 部分覆盖 | 注册、进入普通、重复失败不改热量、150 拍到 0.30、产率 | `FurnaceService.enterNormal` 和魔力、冷却、属性、效果写入需要玩家。 |
| `fire_burn` | 部分覆盖 | 直接过载、升级保留热量、已过载失败、再 100 拍到 0.60、产率 2.2 | 玩家身上的升级事务、属性不叠两份、生命只向下钳，未在玩家上执行。 |
| `fire_cool` | 部分覆盖 | 主动冷却清热量、无炉心失败 | 蒸汽半径、敌我过滤、击退、属性恢复需要世界。 |

没有把六项标成整项受阻。没有把 `SelfStatusOrder` 或 `FurnaceInstall` 写成整条事务成功。

共享链：`ReleaseBatch.fireContext()`、`SkillSegmentMath.mixedChainRefusal`、`SelfStatusCommit.attempt` 都要 `ServerPlayer`，本单没执行。一分为三和双重只核对了注册角色，没有跑复制份。共享 `FireCastContext` 的消费隔离已在账本上执行。源码不含术式 id 不写成组合通过。

交回原作者的生产缺陷清单：本次执行没有新的生产断言失败。已有、未追加实现复审的项照旧保留，不写成新发现：R2/R3 返修未追加复审；半砖「可见」解释未追加复审；炉心失败恢复和扣费顺序有作者反馈、未追加复审；第一人称裁切、侧面厚度、燃烧快照约 40 tick、进入前对账路径、属性只向下钳生命，作者报告里仍是限度。

## 待用户实测

游戏内 Mixin 织入；4 tick 火伤和普通攻击无敌帧；护甲、抗火、保护、牛奶；登出和跨维度；蒸汽击退距离；第一人称、侧面厚度和贴图；客户端燃烧快照；实体 tick 上的地火和刺击；手持剑、装配、真实魔力扣减；真实关系过滤；炉心属性、生命钳制和效果写入；一分为三、双重释放对同一本火种账的复制份。

## 停写

测试源码和 Gradle 到此停写。生产哈希在 2026-10-29T10:51:18.7505973+08:00 与编译前一致。等集中测试结果核对，本单不再开审查或重复测试。
