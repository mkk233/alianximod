# C 最终报告：领域绑定周期运行

作者 C。模型 grok-4.7-high-fast。本阶段是实现。设计 2/2 已用尽，本单不追加设计审查。实现复核仍是 0/3，本单不代替集中复核。

`released=true`

本单完成契约第 5 节里 C 的运行职责。绑定包由 A 按正式契约提供，这里直接调用那些公开类型，没有安装执行桥，也没有为提前编译写占位类。

## 周期

`DomainBindingRuntime.PERIOD_TICKS` 为 20。带冻结计划的实例在构造时把下一拍写成 `startedAt + 20`。开域那一拍不发射。

到期比较用 `now - beat >= 0`，和 `ReleaseInput.hasExpired` 同一套 int 差值。两个条件都到达才发射：首拍 `startedAt + 20`，以及实例上保存的下一拍。`now >= expiresAt`（同样走 `hasExpired`）的实例不发射。主人不在、已死亡，或所在维度不是宿主维度时，这一拍不发射，也不改写下一拍。

发射前先把下一拍推进到严格晚于当前刻的未来节拍：`first + (floor((now - first) / 20) + 1) * 20`。计算结果超过 `Integer.MAX_VALUE` 时停在该上界。时刻跳过多个节拍时只发射当前这一次，不补发中间欠轮。

`DomainService.tick` 先按原条件倒序关闭，再跑 `DomainPulses.fire`，然后对当时的 `ACTIVE` 做 `List.copyOf`。运行时只遍历这份快照里带 `boundPlan()` 的实例，按主人 UUID、再按实例 UUID 排序。排序用 `UUID.compareTo`。执行过程不直接遍历活表。

每一轮使用自己的 `roundId`、`ReleaseExecutionContext`、`LifetimeToken`、`ReleaseVolume`、`TargetCheck` 和 `RoundHandle`。种子是实例 UUID 高位异或低位左旋 17 位，再异或 `dueBeat * 0x9E3779B97F4A7C15`。`RandomSource.create(roundSeed)` 之后调用 `geometry.samplePoint`。没有可用单位方向时，同一个随机源再取均匀球面方向：`vertical = nextDouble() * 2 - 1`，`azimuth = nextDouble() * 2π`，水平半径为 `sqrt(1 - vertical²)`。

本单不调用 `BoundReleases.install`，也不调用 `BindingLayout.installCompiler`。

## 关闭

`DomainInstance.close` 先把 `active` 写成 false，再取消该实例全部未完成 `RoundHandle`，然后关掉接触状态。取消前先把集合复制出来并清空，已 `done` 的句柄跳过。某个 `cancel` 抛出时，其余句柄仍会取消，最后再抛出第一份异常。

句柄只在 `done() == false` 时进入集合。领域 tick 对快照里的每个绑定实例调用 `sweepFinishedRounds`，当拍就去掉已经结束的句柄，不等到下一次 20 刻发射。`close`、以及原有的到期、死亡、下线、跨维路径，都同步走到 `closeAt` → `close`，不等下一次调度 tick。

`closeAt` 在 `close` 返回后按对象身份从活表移除，并广播一次关闭。`close` 抛出时，移除和这一次广播仍会执行。重复进入 `closeAt` 时不再发第二份关闭包。

这里没有删除已经生成的攻击实体。`TargetCheck` 只保存几何、宿主世界和主人 UUID，不读取 `active`，也不持有 `LifetimeToken`。关域后，已生成攻击仍可按这一份局部检查复核目标。

`LifetimeToken.valid(now)` 同时要求：领域 `active`、`ReleaseInput.hasExpired(now, expiresAt)` 为假、宿主世界仍能取到服务器、主人能由 UUID 找到、主人 `isAlive`、主人当前维度就是这份宿主世界。

## 目标

候选查询使用 `LivingCandidateQueries.targetQuery`，查询盒是 `geometry.bounds()`。最终资格在 `acceptsTarget` 再判一次：存活、非旁观、实体所在世界就是冻结的宿主世界、UUID 不是主人、`geometry.fullyContains(包围盒)`，并且 `Relations.permits(ownerId, candidate)` 与 `Relations.ally(ownerId, candidate)` 都为假。关系调用的就是领域原有的 `Relations`，本单没有改 `Relations.ally` 的实现。

距离是采样点到包围盒中心的平方距离。距离不是有限数的候选跳过。距离相等时留下 `UUID.compareTo` 更小的一个。

有目标时，基础方向是采样点指向该包围盒中心的单位向量。该方向不可用时，改用上述球面单位向量，目标 UUID 仍保留。没有目标时，目标 UUID 为空，方向就是这个球面单位向量。

`stillValid` 在冻结宿主世界里按 UUID 取实体，再跑同一套 `acceptsTarget`。空 UUID、非生物、或资格不再成立时返回空。

本单不读、不写玩家锁定标记，不调用按键包、准星、锁定服务或槽位。

## 费用衔接

无绑定的 `tryWave` / `tryWaveReserved` 仍转入 `plan == null`。去重键仍是 `主人UUID:效果键:initialCost:lifetimeTicks`，只有传入的 `claimSuffix != null` 时才在后面追加 `:` 和后缀。

`plan != null` 且后缀为 null 或空串时，不放置并返回 false，不发未学会或魔力不足提示，也不写去重键。同一声明后缀在同一刻命中已有键时，仍直接返回 true。一次调用里的 `copies` 仍按原循环各放一份，每一份保存同一个 `BoundReleasePlan` 引用。

未预留开域仍按 `initialCost * copies` 做 `MagicPool.canAfford` 和 `ManaPaymentReceipt.charge`。预留路径仍不在领域服务里扣这笔起手费。周期一轮的子计划费用由 `BoundReleases.executeAutomatic` 负责，C 不再 `charge`。

武器副本是当前主手的 `copy()`。主手引用仍是这一件时，加成取 `max(0, 攻击伤害属性 - 1)`。属性缺失或读数不是有限数时，这一轮加成是 0。

## 已完成的注册语义

本单没有改 `ChainRole`、`ModTechniques`、`ReleaseDefinitions`、`DomainGeometry`、`AxisBoxGeometry`。注册和 `samplePoint` / `clampPoint` 仍是 `implementation-C-registration-final.md` 中的结果。本单核对这三个符号还在：

- `ChainRole.RULE_DOMAIN_BIND`
- `ModTechniques` 的 `rule_domain_bind` / 「领域绑定」
- `ReleaseDefinitions` 里 `rule_domain_bind` 的 `ReleaseKind.RULE`、`ChainRole.RULE_DOMAIN_BIND` 登记

没有新增 `ReleaseDefinition` 能力声明。没有改 `DomainPulses`、水系定义、`ReleaseBaseEvents`、界面、资源、接触计费或 mixin。

## 改动文件

| 文件 | 改动 | 改前备份 |
|---|---|---|
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainInstance.java` | 冻结计划、下一拍、未完成句柄、关闭时取消 | `backup-C-runtime/` 下同相对路径 |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainService.java` | 绑定重载与声明后缀、快照后调度、关闭收尾 | `backup-C-runtime/` 下同相对路径 |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainBindingRuntime.java` | 新增周期运行 | 无改前文件 |

备份是本会话读到的改前全文，经文件工具按 UTF-8 写入。没有和原字节核对换行。SHA-256、mtime、字节长度没有文件系统读数，记为 null。证据：`docs/开发记录/领域绑定规则-20261030/evidence-C-runtime.json`。

## 未验证

- 待统一编译。本单没有跑 `compileJava`，没有跑 Gradle，没有打包，没有启动游戏。绑定包若尚未由 A 落入工作区，当前引用要等 A 的公开类型一起编译。
- 命名门没有跑成。本会话对 `powershell -NoProfile -File tools/check-name-gate.ps1` 的调用立即返回 `Rejected`，没有输出。没有改审批，也没有换命令再试。
- 没有游戏内核对 20 刻节拍、跳刻、关域退款、选敌或起手费。
- `executeAutomatic` 或 `cancel` 若抛出，当前节拍已经推进的实例要等到下一个未来节拍；同一次排序里尚未执行的实例留到下一 tick。已经生成的攻击是否保留，取决于 A 的 `cancel` 只退未提交份额。

主会话可以在相关写入收束后做统一编译。本报告不表示 A、B 的执行桥、瞄准和实体适配已经接入。
