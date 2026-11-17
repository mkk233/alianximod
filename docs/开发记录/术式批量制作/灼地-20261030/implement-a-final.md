我先读取任务说明和相关执行规范，确认这次授权范围后再动手。任务是灼地阶段 A：只做与几何无关的产种账。我先读批次事实和火种相关接口，再按已裁决方案落最小代码。批次事实已对齐。接下来只读火种账、炉心倍率和释放上下文的现有接口，确认能接上的稳定标识。接口已经对上：同一次真实释放能共用的稳定标识是 `ReleaseBatch.fireContext().castId()`，产种本身不能走那本一次性账。接下来只加去重账和独立入账入口。阶段 A 只加了两份新源码：灼地去重账和已去重产种的入账入口。几何、注册、伤害、燃烧和客户端都没动。这不是完整灼地，也没有做正式复审。本会话到此停写，编译留待统一编译。

## 改动

| 文件 | 最后写入（本地） | 大小 |
|---|---|---|
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/firecast/ScorchSeedLedger.java` | 2026-10-30 20:24:12.130 +08:00 | 8254 字节 |
| `src/main/java/com/bslx/XueXiMod/status/fire/seed/FireSeedProduction.java` | 2026-10-30 20:23:59.550 +08:00 | 1126 字节 |

现有火种账、炉心、`ReleaseBatch`、燎原、燃烧、领域、`SkillUsePacket`、注册器和客户端都没改。制作表和 `记录.md` 没写。

## 去重账

`ScorchSeedLedger` 是进程内三张表，键只有 UUID 和 `MinecraftServer.getTickCount()`。不写 NBT，不保存实体，没有 tick 监听。

- 首次基础 20、周期基础 15、门限间隔 20，在第 21–23 行。
- 候选场龄只有 20、40、60、80。`periodCandidate` 在第 34–36 行。场龄 0 和 100 都会得到 false。
- `retainRelease`（第 39–49 行）每个宿主出生加一。`releaseHost`（第 56–68 行）结束时减一，减到 0 也把 0 留在表里。这两次调用都不删首次记录，也不动周期门限。
- `claimFirst`（第 89–96 行）键是施法者 UUID、释放令牌、目标 UUID。第一次把键放进集合并返回 true，之后返回 false。它不读、不写周期门限。资格在返回前已经记下，所以随后伤害失败或实际新增 0，再调一次仍然是 false。
- `periodOpen`（第 102–110 行）只查看。没有上次获准，或距上次获准已满 20 个服务端 tick，返回 true。同 tick 的间隔是 0，返回 false。传入的 tick 比上次获准更小，也返回 false。
- `admitPeriod`（第 117–130 行）先要求场龄是候选，再占用门限。成功时写入本次服务端 tick 并返回 true。同 tick 第二个人返回 false。场龄不是候选时直接返回 false，不写门限。它不读、不写首次账。返回 true 之后，即使入账新增为 0，这次获准仍然留着。
- `discardRelease`（第 137–149 行）只在这枚令牌的持有数已经是 0 时，删掉它名下的首次记录。仍有宿主，或从未 `retainRelease`，返回 false 并保持原样。它不动周期门限和火种余额。
- `discardPeriod`（第 156–168 行）和 `discardMaturePeriods`（第 172–188 行）只删除距上次获准已满 20 tick 的周期门限。窗口未满的留下。
- `clearTransient`（第 192–198 行）只清空这三张表。

间隔判断在第 200–206 行，用 long 相减，避免两个 int 相减溢出。

## 入账入口

`FireSeedProduction.produce` 在第 18–26 行。调用方已经决定这一次可以产种。基础量小于等于 0 时返回 0，不读炉心、不写余额。

否则每次读取施法者当前的 `FurnaceService.seedMultiplier(caster)`，再建一本新的 `FireCastContext`，交给现有的 `FireSeedState.grant`。取整和上限仍走 `FireSeedMath.floorProduction` 与余额钳到 100。返回值是实际新增点数。

`FireSeedAccount.grant` 和 `markProduced` 的一次性语义保持原样。这本新上下文用完即丢，不会给 `ReleaseBatch.fireContext()` 那本共享账打上已生产。`FireSeedGrant.deposit` 没有被调用。

余额已是 100 时，`grant` 仍会在这本临时上下文上记下生产，再返回新增 0。`FireSeedState.persist` 发现余额没变，就不写属性、也不发原有火苗同步。新增大于 0 时，原有火苗同步会照旧发出；灼地「火星从地面汇向目标」还没做，下一阶段用这个返回值决定播不播。

`seedMultiplier` 走现有 `serverSnapshot`。施法者是服务端线程上的玩家时，这条读路径会先做炉心对账。它不写燃烧快照，不改其他来源的消费标记。施法者为空、不是玩家或不在服务端线程时，现有实现给出 1.0。本入口不按 UUID 去找玩家。

## 真实释放令牌

`ReleaseBatch` 没有另一枚公开批次号。同一次真实释放能拿到的稳定标识，是这本批次上 `fireContext().castId()`。

`fireContext()` 在第 51–56 行懒创建，整本批次只 `UUID.randomUUID()` 一次。类注释写明复制份共用这个对象。一次按键在 `SkillUsePacket` 里 `ReleaseBatch.open` 一次；三重复制和双重释放把同一本 `batch` 传给各份。新的一次 `open` 会得到新的 `castId`，首次奖励可以再给。

`HostCast.tokenId()` 和 `hostId(member, volley)` 是宿主回执。`HostCast.open` 在链计划、段执行和释放包里会各自打开，不能当作这次真实释放的共享令牌。宿主实体 UUID、火场区号也不能当这枚令牌。

批次在 `ThreadLocal` 里，释放包返回后不会活过火场的 100 tick。宿主出生当时就要把 `castId` 抄到宿主自己的记录里。读取 `castId()` 会建出共享火种上下文，但不会给它打生产标记。灼地入账必须走 `FireSeedProduction.produce`，不要把 `batch.fireContext()` 交给 `grant`。

## 下一阶段接入

接入点在未来的灼地宿主出生处，且当时 `ReleaseBatch` 仍绑定着这次按键。现有燎原是在效果执行里读 `ReleaseBatch.current().fireContext()`。这一阶段不改那个效果，也不改 `SkillUsePacket`。

宿主出生：

1. `releaseToken = batch.fireContext().castId()`，同时记下施法者 UUID 和 `server.getTickCount()` 作为出生 tick。
2. `ScorchSeedLedger.retainRelease(releaseToken)` 一次。

每个服务端 tick，调用方已经确认目标在低矮体积内且属于合法敌对之后：

1. `fieldAge = serverTick - birthTick`，`serverTick` 用 `getTickCount()`。
2. `claimFirst` 返回 true：调用 `produce(target, caster, FIRST_BASE)`。1 点伤害由宿主另做。伤害失败也不要撤销这次 `claimFirst`。实际新增大于 0 再播汇聚视觉。本 tick 这个宿主到此为止，不要再 `admitPeriod`。
3. `claimFirst` 返回 false，并且 `admitPeriod(casterId, targetId, fieldAge, serverTick)` 返回 true：调用 `produce(target, caster, PERIOD_BASE)`。这次不造成伤害、不发命中、不种火、不引燃、不引爆。同样只在新增大于 0 时播视觉。

`periodOpen` 可以先看门限，它不消耗资格。真正占资格的是 `admitPeriod`。

宿主结束只调用 `releaseHost`。全部宿主都已配对释放、持有数到 0、并且这枚令牌不会再有新宿主 `retainRelease` 时，才调用 `discardRelease`。某个宿主提前结束、目标离场或在火场之间切换，都不调用 `discardPeriod`。周期门限要清时，用当前 `getTickCount()` 调 `discardPeriod` 或 `discardMaturePeriods`；未满 20 tick 的记录会留下。停服或卸载调用 `clearTransient()`。

## 调用方要守的前置

- 先确认接触和敌对，再 `claimFirst` 或 `admitPeriod`。确认之前调用会把资格用掉。
- `claimFirst` 或 `admitPeriod` 返回 true 之后，立刻在目标所在的服务端主线程调用 `produce`。主线程不符时，现有 `grant` 返回 0 且不改余额，资格已经留下。
- 倍率参数传施法者，目标只作为余额的持有者。
- 每个宿主的 `retainRelease` 和 `releaseHost` 各一次。多调一次 `releaseHost` 会把仍活着的宿主的持有数提前减到 0。
- `holdCount` 在「从未 retain」和「已经全部释放」时都是 0。能否删首次记录，看 `discardRelease` 的返回值。
- 场龄由宿主用出生时的 `getTickCount()` 相减得到。没有候选的 tick 不要调用 `admitPeriod`，账里没有补发队列。
- 周期键按施法者加目标，只给灼地用。幽冥和其他来源的叠加这一阶段没有接。

从第 0 tick 起一直停留时：第 0 tick 只有首次 20；第 20、40、60、80 tick 由 `admitPeriod` 各给基础 15。首次若正好落在某个宿主的 20/40/60/80，该宿主本 tick 只走 `claimFirst`。门限没被这次首次写过，其他宿主仍可按共享门限提交自己的候选。

## 定向检查

- 对照了 `FireSeedState.grant`、`FireSeedAccount.grant`、`FireSeedMath.floorProduction`、`FurnaceService.seedMultiplier` 和 `ReleaseBatch.fireContext()` 的现有语义。
- 走读了首次不写周期门限、周期不写首次账、同 tick 只放行一次、未满 20 tick 的清理会留下记录、`clearTransient` 不触及火种余额。
- 跑了 `powershell -NoProfile -File tools/check-name-gate.ps1`。本次两个新文件没有命名违规。退出码 1 来自未改动的 `OriginalHumanoid.java` 第 165 行 `i`、第 202 行 `to`。按门禁要求，这两处旧违规没有顺手改。
- 没有跑 `gradlew`，没有测试、打包、`runClient` 或游戏。

实现校验仍是 0/3。本阶段结束只交回结果，没有单独开正式复审。
