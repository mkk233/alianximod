# 火系六式测试阶段准备

采样时间：2026-10-29T10:31:22+08:00。只读事实，供后续测试任务使用。

本文件不是实现复审，也不是测试正式校验。设计正式额度 2/2、实现正式额度 2/2 均不追加。测试正式校验仍是 0/2，本单不消耗。没有通过结论，没有制作表迁移，没有改 `src`、制作表、`记录.md` 或归档。没有跑 Gradle，没有执行任何检查入口。

## 1. 当前字节与已有编译

对照 `compile-freeze-main-final.json`（该冻结在 2026-10-29 04:17 前后与当时源码一致，随后 04:24 的 `compileJava --offline --no-daemon` 退出 0，当时 66 个本批路径与 889 条冻结路径均无差异）。

| 范围 | 冻结内条数 | 缺失 | 哈希或长度不同 | 冻结外新增 |
| --- | ---: | ---: | ---: | ---: |
| `src/main/java` | 639 | 1 | 1 | 1 |
| 本批游戏资源 | 15 | 0 | 0 | 0 |

本批游戏资源指 `liaoli_v6`、`liaoyuan_v2`、`fentian_v2`、`furnace_v1` 四组贴图，以及 `zh_cn.json`、`en_us.json`。这 15 个文件的当前 SHA256 与冻结一致。

差异路径：

| 路径 | 事实 |
| --- | --- |
| `src/main/java/com/bslx/XueXiMod/mixin/FireHurtMixinPlugin.java` | 仍在。当前 SHA256 `EA0A228403E5B01D5FA38AEE67EB73AB59655B77E89583FBE2F2F399C5A8B850`，1556 字节，mtime 2026-10-29T09:08:29.6649850+08:00。冻结是 `DCF5D2B52BA037B097111530DC2D61364DBA9D56FA3C33265FC73BFD3E51E00C`，1486 字节，mtime 2026-10-29 00:33:14。 |
| `src/main/java/com/bslx/XueXiMod/mixin/FireHurtBoundary.java` | 冻结有此路径，SHA256 `F43FC91FE274EB823CC93235C2F45C8B1E4A2D02AAF5D175D56D5A7FE0EDED6D`，5533 字节，mtime 2026-10-29 00:38:41。当前路径不存在。 |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/FireHurtBoundary.java` | 冻结外新增。SHA256 `3EFD234372FBFE88492C46897B9F3975A6F341F77EDC56A908D58002620DFCEC`，5554 字节，mtime 2026-10-29T09:08:29.6649850+08:00。 |

其余 637 个 `src/main/java` 文件与冻结哈希一致。未重读差异内容。

`build/classes/java/main` 与当前 `src/main/java` 的 mtime：639 个 `.java` 里，637 个对应 `.class` 的 mtime 不早于源码，0 个早于源码。没有 `.class` 的是 `client/action/skinned/attachments/package-info.java` 和 `shu/technique/release/deflection/package-info.java`。

与上述差异直接相关的 class：

| class | mtime | 字节 |
| --- | --- | --- |
| `mixin/FireHurtMixinPlugin.class` | 2026-10-29T09:09:18.1064663+08:00 | 2451 |
| `shu/technique/release/fire/FireHurtBoundary.class` | 2026-10-29T09:09:19.7999759+08:00 | 5205 |
| `mixin/FireHurtBoundary.class` | 不存在 |  |

未改字节的 `FireVisualPacket.java`、`FireVisualClient.java` 仍是编译修复时的哈希（`FA239097…`、`423AF8D0…`，mtime 均为 04:13:24），但它们的 class 已是 2026-10-29T09:09:16。04:24 那次编译写过的 class 时间已被 09:09 这一批 class 覆盖。本单没有读到 09:09 这次 class 写入的 Gradle 退出码。

哈希事实：04:24 记录在案的成功编译，其输入是冻结哈希，对不上当前 `FireHurtMixinPlugin.java`，也对不上已经不在原路径的 `mixin/FireHurtBoundary.java`。当前这两个源文件的 class mtime 晚于源文件 mtime，只说明磁盘上有更新的 class，不把 09:09 的 class 写成一次已记录的完整 `compileJava`。

已读完成报告里的停写，都早于 09:08：

- 六式实现报告停写于 2026-10-29 02:59:04。
- 已知限制报告停写于 03:21:57。
- 平面修补报告停写于 03:35:48。
- 贴图接入报告停写于 03:48:08。
- 炉心原子报告停写于 03:58:38。
- 编译修复报告停写于 04:14:02，只改了 `FireVisualPacket.waitTicks()` 和 `FireVisualClient` 的调用。
- 表作者在那次编译前已停写。

已读的完成记录里，没有覆盖 09:08 这两处字节的停写声明，也没有对应的修复反馈。火属性底座记录把 R2/R3 返修标成「未追加复审」，底座编译收口用的 `FireHurtBoundary.class` 时间是 2026-10-29 00:45:17，早于这次路径变化。

## 2. 已有八项检查的机制

这八项在 2026-10-29 04:25 对当时的 `build/classes/java/main` 各跑过一轮，javac 与 `java` 退出码都是 0。它们调用的生产源文件当前哈希仍与冻结一致。磁盘上的 main class 已在 09:09 重写。04:25 的退出码覆盖的是当时的 class，不自动覆盖 09:09 的 class。本单没有重跑。

共同运行条件：JDK 17，工作目录 `D:\alianximod`，classpath 含 `build/classes/java/main`。这八项当时没有把 Forge 客户端或服务端拉起来。源码比对用 `Files.readString`。

| 入口 | 类别 | 调用的生产函数 | 源码文本比对 | 运行依赖 |
| --- | --- | --- | --- | --- |
| `SixFireLogicCheck` | 混合 | `FireConsumerPlans.spread` / `thrust` / `sky`，`FireLayer.combine`，`GroundFireMath.inLayer` / `insideDisc`，`SphereTouch.reaches`，`MeleeCorridorMath.blocked` / `overlaps` | `SkillUsePacket`、`SelfStatusCommit`、`FentianEffect`、`GroundFireFields`、`LiaoliEffect`、`TechniqueHitSpec` | JDK + main class |
| `LiaoliV6LayoutCheck` | 混合 | `LiaoliV6Layout` 的剑柄、剑尖、镜像、扇角、翻转、火焰段、突进、气流、环和热力距离 | `LiaoliV6Renderer`、`SkillUsePacket`、`LiaoliEffect` | JDK + main class |
| `FireVisualLayoutCheck` | 混合 | `FireVisualLayout` 的半径、波次、贴附、燃烧令牌、脉冲帧、余烬、焚天缩放、贴图行、炉心循环帧、冷却帧 | `GroundFireFields`、`FentianEffect`、`SelfStatusCommit`、`SkillUsePacket`、`FurnaceEvents`、`FireVisualClient` | JDK + main class |
| `KnownLimitsCheck` | 混合 | `GroundFireMath.openTop` / `inLayer` / `insideDisc`（自造 `Span`），`FireVisualLayout.discCoordinate` / `waveVisible` / `coveredRadius`，`SelfStatusOrder.decide` | `GroundFireFields`、`StandSurface`、`FireVisualClient`、`SelfStatusCommit`、`SkillUsePacket` | JDK + main class。`openTop` 不调用 `StandSurface.columnTop` |
| `FurnaceAtomicCheck` | 混合 | `FurnaceMath.transition`，`FurnaceInstall.blocked` / `accepted` / `plan` | `FurnaceService`、`SelfStatusCommit` | JDK + main class |
| `FurnaceStructureCheck` | 源码文本 | 无 | 炉心服务、事件、运行时、效果、抗性 Mixin、效果注册、语言、存档和属性 | 读项目内文件 |
| `FurnaceRepairCheck` | 混合 | `FurnaceMath.allowsServerWork`，`Outcome.REJECT_THREAD.succeeded()` | `FurnaceService`、`FurnaceRuntime`、`ThreadGate`、`FurnaceEvents` | JDK + main class。自注没有真实服务端线程，没有写效果或属性 |
| `FireReleaseStructureCheck` | 源码文本 | 无 | 火释放包、燃烧载体、蒸汽、伤害、运行时事件、燃烧分发、三个伤害标签 | 读项目内文件。自注没有数据包加载、标签合并或真实无敌帧 |

## 3. 已有记录里点名的实现正确性未决项

六式本批没有一份完成的正式实现复审记录。下面只转写已完成报告里写明的项，不另作对错判断。

| 记录 | 已写明的状态 | 与六式的关系 | 作者修复反馈 |
| --- | --- | --- | --- |
| `火属性二级底座-20261028/review-round2.md`：R2 要求 `invulnerableTime` 的 Mixin owner 是 `LivingEntity`；R3 要求 `hurt` 异常离开时清元素帧。结论是不通过，实现额度 2/2。 | `repair-final-final.md` 写了按该方案改插件和边界。`compile-postfix-final.md` 写了编译退出 0、12 个入口 627 断言 0 失败，并写明那不是游戏内 Mixin。`记录.md` 与 `底层规格.md` 写明「返修结果未追加复审」。 | 共用火伤、冷却隔离和元素帧。燎原、燎里、焚天的伤害和炉心蒸汽走这套，不按六个术式 id 分叉。 | 有底座返修报告。没有追加复审。09:08 的插件字节和 `FireHurtBoundary` 路径变化，在已读完成记录里没有对应反馈。 |
| `implement-report.md`：半砖顶不是碰撞顶。 | `known-limits-report.md` 写明改成碰撞顶面加容差，并写明没有视线射线。批次记录写明这个「可见」解释未获额外正式复审。 | 燎原落点。 | 有作者反馈，没有后续审查结论。 |
| `implement-report.md`：炉心提交成功后扣魔力失败只记日志，效果仍算已提交。 | `known-limits-report.md` 写明预览、扣费、失败退款的顺序。`furnace-atomic-report.md` 写明 `forceAddEffect` 失败后恢复效果和热量，并写明失败恢复用的是对账后、上效果前的快照。 | 升温、燃烧、冷却。 | 有作者反馈，没有追加正式复审。 |
| `known-limits-report.md` 第 30 行：`REJECT_BLOCKED` 后效果可能仍留在玩家身上。 | 同一条被炉心原子任务接走。作者报告写了失败恢复，同时写明属性修饰符仍只把当前生命往下夹，没有蒸汽或被动治疗把生命加回去。 | 炉心三式进入失败。 | 有作者反馈，没有追加正式复审。 |
| `remaining-visual-final.md`：平面波、第一人称裁切、燃烧快照约 40 tick 后不再刷新。 | `known-limits-report.md` 写明按柱用高度块补平面。第一人称裁切和 40 tick 快照在该视觉报告里仍列为限度，后续完成报告没有写这两项已改。 | 燎原波面；燃烧的客户端快照。 | 平面有作者反馈。第一人称和 40 tick 没有后续修复反馈。 |
| `visual-integration-final.md`：第一人称和侧面厚度未进游戏核对；侧看平面四边形很薄。 | 后续完成报告没有写这项已改。 | 燎原、燎里、焚天贴图。 | 无后续修复反馈。 |
| `furnace-atomic-report.md`：进入前对账仍是原读取路径。 | 作者把它列为未改行为。 | 炉心三式。 | 无后续修复反馈。 |
| 编译日志里的 32 条待删除 API 警告。 | `compile-fix-report.md` 写明没有改这些调用。警告没有让那次构建失败。 | 全仓既有调用，编译修复只动了 `waitTicks`。 | 无。这不是一份正确性审查列出的未决缺陷。 |

`OriginalHumanoid.java` 的名字门禁旧名、`SkinAttachmentChecks.java` 的全量测试编译错误，都在本批动手前就存在。本批完成报告写明没有改 `SkinAttachmentChecks.java`。它们不是六式审查列出的实现未决项。

## 4. 流程需要与可执行入口

流程 7.1 要求对最终源码做正常、边界、失败，并覆盖实际改过的多术式组合、共享注册、链结算和状态隔离。下面只对照现有入口，不执行。

正常：

- `SixFireLogicCheck` 调用燎原、燎里、焚天的计划函数，以及层、圆盘、球体、近战走廊。
- `LiaoliV6LayoutCheck` 调用燎里几何。
- `FireVisualLayoutCheck` 调用波次、脉冲、炉心循环和冷却帧。
- `FurnaceAtomicCheck` 调用 `FurnaceMath.transition`、`FurnaceInstall.plan`。
- 底座已有、本批八项之外：`FurnaceMathCheck` 调用 `FurnaceMath.advanceOnline`；`BurnLedgerCheck` 调用燃烧账本；`FireDeathMessageCheck` 格式化死亡句。它们的最近一次记录在案的执行属于底座编译收口，早于六式源码。

边界：

- `KnownLimitsCheck` 用自造 `Span` 调 `GroundFireMath.openTop`、`inLayer`、`insideDisc`，并调用 `SelfStatusOrder.decide`。
- `SixFireLogicCheck` 含零火种空计划、圆盘边和球体边。
- `FireVisualLayoutCheck` 含低火种不进入后续脉冲。
- `FurnaceAtomicCheck` 含有限时长被拒、空恢复。

失败：

- `SelfStatusOrder.decide` 有预览拒绝、未付款、退款等结果，由 `KnownLimitsCheck` 调用。
- `FurnaceInstall.plan` 有移除和重装，由 `FurnaceAtomicCheck` 调用。
- `FurnaceMath.allowsServerWork` 的拒绝分支由 `FurnaceRepairCheck` 调用。
- 离线账本 `FireSeedAccount.grant` 的拒绝和封顶由 `FireSeedCheck` 调用。见下文火种两条。

多术式组合：八项没有调用把两个以上六式放进同一次 `ReleaseBatch` 或同一条链再结算的生产函数。`SixFireLogicCheck` 只用文本确认 `SkillUsePacket` 和 `SelfStatusCommit` 不含六个 id。缺一条执行组合结算的入口。

共享注册：八项没有调用 `ReleaseDefinitions.registerAll`、`SelfStatusDefinition.registerAll` 或 `ModTechniques`。注册表里是否出现六个 id，目前只出现在实现报告的叙述里。

链结算：`SelfStatusOrder.decide` 是顺序函数，不是 `SkillUsePacket.runSegment`。八项没有调用魔力扣减、冷却写入或按住连发。`FireReleaseStructureCheck` 只读 `FireRuntimeEvents` 文本。

状态隔离：

- 底座入口仍在：`FireLayerCheck` 调用 `ElementDamageScope.enterHurt`、`armFire`；`ElementHurtLifecycleCheck` 调用令牌的正常、早退、异常路径；`FireCooldownCheck` 调用冷却记录；`FireHurtBytecodeCheck` 读本机 `recomp.jar` 里的 `LivingEntity.hurt`，再调用 `FireHurtBoundary.wrapMappedHurt`。该检查与 `FireHurtBoundary` 同包，当前类已在 `shu.technique.release.fire`。
- 这四个入口最近一次记录在案的执行是底座 `compile-postfix-final.md`（12 入口、627 断言、0 失败），class 时间 00:45:17。当前边界源码和 class 都是 09:08–09:09，那次断言结果不覆盖当前字节。
- `BurnLedgerCheck` 调用燃烧账本的独占轨。它同样属于底座那次执行，不覆盖六式之后的字节。

火种分两条记：

- 正常游玩：`FireSeedState.grant` 没有主源码调用方，也没有命令。升温、燃烧、焚天在普通游戏里没有准备火种余额的入口。
- 离线构造：`FireSeedCheck` 直接调用 `FireSeedMath` 和 `FireSeedAccount.grant`，其注释写明不构造世界、不启动游戏。账本对象可以在没有世界的情况下造出余额。本单没有运行它。

离线入口覆盖不到、留给以后用户实测的项，来自已有报告的自注：

- 游戏进程里的 Mixin 织入。
- 4 tick 火伤和普通攻击的无敌帧交错。
- 护甲、抗火、保护附魔、牛奶对炉心的效果。
- 登出、跨维度之后的炉心和燃烧保持。
- 蒸汽击退距离。
- 第一人称裁切、侧面厚度、贴图在游戏里的样子。
- 客户端燃烧快照在约 40 tick 后是否还在。
- 实体 tick 上的地面伤害、手持剑门、装配界面、对真实玩家的魔力扣减。
- 真实实体上的关系过滤。`DamagePermission` 会碰到 Minecraft 类型；`SixFireLogicCheck` 没有调用它。

## 停写

本报告到此停写。后续测试任务另开，不由本单补测试或改源码。
