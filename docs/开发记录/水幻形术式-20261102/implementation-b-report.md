# 水幻形 B 实现报告

日期：2026-11-02。作者：B。阶段：实现。未开独立正式审查。

命名门禁 `powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 0。本次扫描没有报出其他文件的命名违规。未跑 Gradle。未启动游戏。待统一编译。本报告写完后停止改源码。

## 改动

只动了动作登记和新建 `clone/capture`。没有改 A 的 `clone/core`、C 的公共入口和配置注册、D 的网络与渲染，也没有改四个旧水系效果、`TechniqueHitSpec`、`TechniqueHitSettle`、`TechniqueHits`。

| 文件 | 行号 | 内容 |
|---|---|---|
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/action/CloneActionRegistry.java` | 25–33 | `register(CloneAction, CloneActionCapabilities)`。删掉 `instanceof ClonePulseAction` 自动 `full()`，也没有全拒绝能力的隐式登记 |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/action/ClonePulseAction.java` | 46–49 | 脉冲显式 `CloneActionCapabilities.full()`，末尾 `ActorPayloadRegistry.registerBuiltins()` |
| `.../clone/capture/PayloadQuoteFault.java` | 6–11 | 四个拒绝原因 |
| `.../clone/capture/CaptureQuoteContext.java` | 14–26 | 无玩家上下文；武器构造时再 `copy()` |
| `.../clone/capture/CaptureQuote.java` | 12–75 | 接受/拒绝报价。接受时伤害、魔力、冷却都在 0..1000000 |
| `.../clone/capture/CapturedActionAuthority.java` | 9–21 | 行动授权 |
| `.../clone/capture/ActorVolumeSpec.java` | 15–42 | 静止体积规格，视觉只带 `VolumeVisualKind` |
| `.../clone/capture/ActorPayloadAdapter.java` | 11–20 | 报价与实例化接口 |
| `.../clone/capture/ActorPayloadRegistry.java` | 19–45 | 显式登记、`registerBuiltins`、`find` |
| `.../clone/capture/TideSeverActorAdapter.java` | 27–183 | 仅 `tide_sever`，版本 1，属性 `WATER` |
| `.../clone/capture/ActorVolumeGeometry.java` | 21–108，110–143 | `basis` / `worldBounds` / `intersectsObb`，以及命中用的交点 |
| `.../clone/capture/ActorVolumeRuntime.java` | 56–145，147–260 | `submit` / `tick` / `shutdown` / `idle`，结算与阻挡 |

`TideSeverActorAdapter.freezeQuote` 在 54–114 行。伤害公式在 169–174 行。`instantiate` 在 116–167 行，成功时 `volumeVisual` 为 `TIDE_SLASH_FULL`。

## 合同状态

已按冻结签名落地：

- 未知动作没有登记入口。`admit` 找不到键就是 `UNSUPPORTED_ACTION`。脉冲仍声明原来的七项全能力。
- `registerBuiltins` 只登记断潮。缺月、让锋、垂丝不会进表。
- 报价不扣款、不写玩家冷却、不读 `owner.getMainHandItem()`。断潮不要求武器是剑。伤害用上下文里已经算好的 `weaponBonus` 和 `attributeDamageBonus`。
- 原始伤害按合同第 2 节的单段公式，`magicDamage = 0`，没有附魔分支。然后只调用一次 `BalanceService.damageContribution(BalanceSide.SERVER, "tide_sever", RawDamage)`。没有再乘 `BalanceDisplay.damage`。
- 魔力先取 `Technique.manaCost()`，再只调用一次 `BalanceService.manaCost(SERVER, "tide_sever", "manaCost", rawMana)`。冷却只调用一次 `BalanceService.cooldownTicks(SERVER, "tide_sever", payload.getBaseCooldown())`。这里不做法杖缩减。
- 几何读口与 `TideSeverEffect.execute` 相同：`SpecialBalance.number` 的 `beamLength`、`boxHeight`，以及 `ReleaseRegistry.get("tide_sever").thickness()` / `lifetimeTicks()`。`5.0` 和 `3.0` 只是键未生效时的原始默认，键生效后不会盖过配置。定义缺失、尺寸或寿命不是有限正数，返回 `GEOMETRY`。
- 伤害对象为空，或伤害、魔力、冷却超出 0..1000000 闭区间，返回 `INVALID_QUOTE`。登记版本对不上返回 `ADAPTER_MISMATCH`。编号或属性不是断潮/水，返回 `UNSUPPORTED`。
- 体积以冻结眼睛为近面中心，中心是 `eyeOrigin + forward * depth / 2`。半尺寸是宽/2、高/2、深/2。`|forward.y| > 0.999` 时右轴用 `(1,0,0)`，否则 `forward × (0,1,0)` 再单位化；上轴是右轴 × 前轴再单位化。不读主人当前视线。
- 粗筛用外接 AABB 的 `LivingCandidateQueries.entityQuery`。命中必须 `intersectsObb`。分离轴按三条边的支撑函数计算，竖直备用轴略有偏斜时仍是该组轴张成的体积。
- 阻挡从冻结眼睛打到体积与目标相交的点，`ClipContext.Block.COLLIDER`、`Fluid.NONE`，射线实体是仍在线的主人。未加载区块不射线、不强制加载；这条路径本 tick 不记命中。
- 真正结算前同时过 `DamagePermission.allows`、`CloneEntity.rejectsPulseTarget`，并再排除任意 `CloneEntity`。然后 `TechniqueHits.classifyRespecting(..., TideMarkSuccess.NONE)`。进入结算前占该目标令牌；同一体积对同一目标最多一次，免疫也占住。满 64 个尝试目标后不再增加。0 伤仍走这一次结算。每次结算重新 `CloneSourceScope.open(..., CLONE_RESPONSE, rootEventId, 1)`，`finally` 里 `close()`。
- 不改无敌帧，不加领域、潮印、火种、击退载荷，不动主手，不调用 `onFirstTrigger`。不调用旧 `execute`、`spawn`、`spawnReported`、`tryHit`，不读 `CastFrame` / `HostGroup` / `Tally`。
- 体积结束只调用 `CloneService.finishDeployment`。不调用 `endActor`、`CloneService.end` 或 `request`。B 不挂 Forge tick。空表时 `tick` 立即返回。
- 没有把候选贴图写入 `src`，没有注册 `water_phantom`，没有声明水幻形平衡键。

## 配置消费

断潮既有键，部署报价时各读一次，之后体积只用快照里的 `ConfiguredDamage` 和尺寸/寿命，命中不再缩放：

- `beamLength`、`boxHeight`：`SpecialBalance.number`
- `thickness`、`lifetimeTicks`：断潮 `ReleaseDefinition` 现有读口
- `damageMultiplier` 与全局伤害：包在一次 `damageContribution` 里
- `manaCost` 与全局魔力：一次 `manaCost`
- `cooldownTicks` 与全局冷却：一次 `cooldownTicks`

没有水幻形自己的伤害倍率，没有新的命中去重、容量、触发或反击费用键。

## 交给其他作者

- C：在 `PerfCounter` 末尾增加 `ACTOR_VOLUME_UPDATES`，在 `PerfMetric` 末尾增加 `ACTOR_VOLUME_UPDATE`。B 已经在体积真正更新时调用它们（`ActorVolumeRuntime.java` 104–106 行）。这两个枚举现在还不存在，统一编译会因此失败。C 负责 `freezeQuote` 的调用和冷却；B 不改 `SkillUsePacket`。
- A：核心类型和 `CloneService.finishDeployment` 已按合同存在。A 已调用 `submit`、`tick`、`idle`、`shutdown`。B 不改这些调用。租约已经 `released` 时，B 下一轮 `tick` 只摘掉体积，不再次 `finishDeployment`。`shutdown` 对尚未释放的租约才会 `finishDeployment`。
- D：规格里的视觉是 `TIDE_SLASH_FULL`。世界变换用上述中心和半尺寸，`basis` 返回顺序是前、上、右。B 不画斩击。

`SkillSegmentMath.segmentDamage` 是 `network` 包内可见，B 不能调用，也不能改那个文件。公式复制在 `TideSeverActorAdapter.scaledBaseDamage`。

## 实现时加的边界

合同没有单列这些上限，失败时都不结算、不强制加载：

- 体积外接区域超过 4096 个区块列，或坐标绝对值超过 `3.0e7`，按 `AREA_UNLOADED` 结束。
- 眼睛到相交点超过 1024 格，本 tick 不射线，也不占命中令牌。
- 体积 tick 抛出 `RuntimeException` 时，用 `FAILED_SUBMIT` 交还租约。`CaptureEndReason` 没有单独的运行异常值。
- `ActorVolumeSpec` 没有术式编号。运行时版本核对固定查 `tide_sever`。本批只有这一个适配器。
- 冷却配置若高于 100 万，报价按合同的金额闭区间拒绝。平衡目录的冷却上界是 120 万，这里没有放宽。
- 非服务器线程的 `submit` / `tick` 直接拒绝。`shutdown` 在当前服务器还在时只接受该服务器线程；服务器引用已经清空时，只接受最后一次成功 `submit` 或 `tick` 的那条线程，避免停服后静态表留到下一局。

## 未跑与阻塞

- 未执行 `gradlew compileJava`。待统一编译。
- 未启动游戏，也未做游戏内验证。
- 统一编译还要等 C 补上两个性能枚举。A 的类型签名目前与合同一致。
- 视觉 v2 未确认。没有接入候选资源，没有正式 `PLAYER` 登记。
- 合同第 9 节的装配门仍在。玩家现在仍装不上「水幻形 → 断潮」。这不在 B 的文件里。

已停止写入。
