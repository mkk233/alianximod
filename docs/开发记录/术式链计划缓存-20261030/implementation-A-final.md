# 作者 A 实现终稿

开始 UTC：2026-10-30T02:42:39.756Z。本文写完后的文件哈希见同目录 `evidence-A.json`。本文件自身的 SHA256 也记在那份证据里，不写进正文，避免自引用。

已停止继续改源码。没有编译、没有打包、没有启动游戏、没有测试、没有 git 提交。没有启动正式复审。

## 范围

新增：

- `src/main/java/com/bslx/XueXiMod/network/CompiledCastPlan.java`
- `src/main/java/com/bslx/XueXiMod/network/CastPlanContext.java`
- `src/main/java/com/bslx/XueXiMod/network/CastPlanCache.java`
- `src/main/java/com/bslx/XueXiMod/network/CastPlanCacheEvents.java`

修改：

- `src/main/java/com/bslx/XueXiMod/network/ChainSteps.java`
- `src/main/java/com/bslx/XueXiMod/network/CompiledReleaseStep.java`
- `src/main/java/com/bslx/XueXiMod/network/SkillChainPlan.java`
- `src/main/java/com/bslx/XueXiMod/network/SkillUsePacket.java`
- `src/main/java/com/bslx/XueXiMod/network/SkillSegmentRunner.java`
- `src/main/java/com/bslx/XueXiMod/network/CastExecutionSession.java`
- `src/main/java/com/bslx/XueXiMod/network/AttackBudgetWindow.java`

文档：`docs/术式链计划缓存说明.md`。备份在本目录 `backup-A/`，按项目相对路径存放，内容是改动前的七个已有源文件。

`TechniqueChain.java` 没有改。切开仍然只有 `TechniqueChain.split`，编译切片时调用它，命中不再调用。没有改作者 B 的文件，也没有改构建配置、制作表或其他源码。

## 接线

查找入口是 `CastPlanCache.lookup`（`CastPlanCache.java` 61 行）。`SkillUsePacket.executeSkill` 在原结构解析处调用它（`SkillUsePacket.java` 250 行）。键是技能类型、有序术式 id、原胚和 `ChainStructureEpoch.current()`。相等比较走字段全量比较，不只比哈希。

命中时：

- 步进表用缓存里的下标。`progressSignature` 来自 `ChainSteps.signature`，仍然只有 id。游标仍由 `ChainProgress.current` 读取和之后的 `advance` 推进。冷却仍是 `ChainSteps.cooldown` 乘上这次的法杖缩减。
- 当次技能仍用 `ChainSteps.slice` 从当前技能切出来（`CompiledCastPlan.sliceSkill`）。
- 分段直接用该切片里 `List.copyOf` 包住的那一份 `ChainSegment`（`SkillUsePacket.java` 311 行），不再 `split`。
- `CastPlanContext` 持有这次的技能和这一份切片（`CastPlanContext.java` 26 行 `canReuseStructure`）。

未命中、无资格、超长、超预算、未绑定或线程不对时，`lookup` 返回 null，随后完整走原来的 `ChainSteps.list`、`signature`、`slice` 和 `TechniqueChain.split`。没有用 ThreadLocal。

`ChainSteps.freezeListed`（77 行）只把 `list` 的下标收成不可变副本，供编译保存。算法仍是原来的 `list`。

### 被实际消费的结构

1. 步进下标和进度签名，按下时使用。
2. `TechniqueChain.split` 的结果，只在 `CompiledCastPlan.compile`（34 行）里每个切片调用一次。
3. `CompiledReleaseStep.chooseStructure`（65 行）是双重路线和动作后单一条件路线的唯一选择核心。缓存保存成员下标和链输入。`compile`（50、55 行）在按下时按原顺序重算份额、整链冷却、动作冷却和蓄力拒绝，再 `new` 当次对象。`members` 改为 `List.copyOf`，成员对象来自同一份 `segments`。
4. `SkillChainPlan.scanModifiers` 是修饰扫描核心。`captureModifierTemplate`（139 行）在编译时调用它。命中后 `collectModifiers` 按起点新建 `ModifierPlan` 并复制字段。`skill == null` 的预扣路径用 `ReleaseInput.BASE`，不用真实技能倍率。
5. `copiesBefore`、`releasedSegments`、`sameReleaseGroup`、`lockThisCast`、`dropThisCast`、`frozenChainInput` 保留原方法，并增加可选上下文。代际一致时读同一切片的下标或标志；否则调用原方法。数字费用函数仍在调用时现算。
6. `SkillSegmentRunner.scanRoles`（692 行）是唯一角色扫描。`resolveRoles`（731 行）在代际一致时直接取标志，否则扫描。`runSegment`（506 行）走这个分支。蓄力重入把同一个 `CastPlanContext` 传回去，不提前读位置或视线。
7. `CastExecutionSession` 和 `AttackBudgetWindow` 保存当次上下文。接触、第二名成员和 `firePaidPlan` 把上下文传给 `SkillChainPlan` 与 `SkillSegmentRunner`。伤害、主手、快照和份额仍在原来的提交点计算。

`HoldRepeat` 未改。它再次 `offer`，会得到新的当次上下文。`SkillFollowCast` 未改，继续走传入 null 的旧重载。

## 计数

一次 `lookup` 只记 `CAST_PLAN_HITS`、`CAST_PLAN_MISSES`、`CAST_PLAN_BYPASSES` 之一。成功放入缓存后另记 `CAST_PLAN_COMPILES`（`CastPlanCache.java` 79、95 行附近）。预算失败只记 bypass。编译异常不捕获。

`PerfMetric.CAST_PLAN_COMPILE` 包住真实编译（87 行）。`CHAIN_SPLIT` 仍只包 `TechniqueChain.split`。`CHAIN_ROLE_SCAN` 移到 `scanRoles`，编译扫描和失配回退都计时，命中读标志不计时。`PerfMetric.java` 的注释属于作者 B，本作者没有改。

## 生命周期

`CastPlanCacheEvents` 在 `ServerStarted` 绑定服务器与当前线程并换一张空表，在 `ServerStopping` 清空并丢掉引用。代际变化在下一次本线程查找时清表，不在注册线程上改表。链长上限 64，条目上限 64，单元上限 16384，都在分配保存用的大列表之前检查。超限回旧路径，不拒绝释放。

服务绑定依赖作者 B 的：

- `com.bslx.XueXiMod.shu.technique.ChainStructureEpoch.current()`
- `ReleaseDefinition.supportsStructuralPlanCache()`
- `PerfCounter.CAST_PLAN_HITS` / `MISSES` / `COMPILES` / `BYPASSES`
- `PerfMetric.CAST_PLAN_COMPILE`

这些类型本作者没有创建，也没有改用临时名字。

## 旧语义与回退

无上下文的 `collectModifiers`、`copiesBefore`、`releasedSegments`、`frozenChainInput`、`lockThisCast`、`dropThisCast`、`firePlan`、`buildSegmentRunner`、`commitNow`、`commitHostGroup`、`playAction`、`runSegment` 都还在，内部传 null。预扣仍用空技能得到基准链输入。费用乘法、求和、冷却的 `scaleCooldown` 和 `ChainSteps.cooldown` 仍在原分支调用。5 刻与 6 刻排程未改。

代际失配后，上下文继续拿构造时的切片。已放进 `ModifierPlan` 或 `CompiledReleaseStep` 的当次数值不回写。段执行和延迟提交点若发现代际不一致，在同一份分段上调用原来的动态函数。

## 尚未完成与风险

- 作者 B 的代际、资格方法和四个计数若尚未落地，工程还不能编译。这是约定的并行边界，不是改用别的签名。
- 名称门禁扫完全部 `src` 时，未改动的 `animation/runtime/OriginalHumanoid.java` 仍有两处旧短名（`i`、`to`）。本次改过的文件没有新的短名或禁止缩写。没有顺手改那个动画文件。
- 预算在极端上界上守门。正常 64 长链按当前记账低于 16384。若某次编译中途超预算，放弃的编译可能已经记过 `CHAIN_SPLIT`，旧路径会再切一次。玩法仍走旧路径。
- 缓存命中不保证每刻耗时下降。
- `SkillFollowCast` 在仓库里没有调用点，仍是旧路径。

## 未运行的验证

没有运行 `gradlew`，没有 `compileJava`，没有打包，没有启动客户端，没有游戏实测，没有单元测试。名称门禁已跑，结果见上。

## 备份

路径均相对于 `docs/开发记录/术式链计划缓存-20261030/backup-A/`。哈希是备份落盘时的 SHA256，时间是当时文件的 UTC 修改时间。

| 文件 | SHA256 | UTC mtime |
| --- | --- | --- |
| `src/main/java/com/bslx/XueXiMod/network/ChainSteps.java` | `D131D8E8A10A8A2298DD63E919EBC667B2FDF2922DAC69785C08CC97FB652CDC` | 2026-10-23T16:11:15.337Z |
| `src/main/java/com/bslx/XueXiMod/network/CompiledReleaseStep.java` | `430A6BD68959697233F1317FBD0D57EACB972D1D041547B0EFD1AF59EBE9C62D` | 2026-10-28T10:28:51.206Z |
| `src/main/java/com/bslx/XueXiMod/network/SkillChainPlan.java` | `DC674A2FE6ADF83C66AE985C4E13F7FE8300FFFDDAFB41D6DE5AD8FD29A84336` | 2026-10-28T18:47:57.901Z |
| `src/main/java/com/bslx/XueXiMod/network/SkillUsePacket.java` | `CD687E039956C02BE1142949844E5D6B8B0C34F33BA6EF9BC7149DFB47317EB2` | 2026-10-29T20:03:53.814Z |
| `src/main/java/com/bslx/XueXiMod/network/SkillSegmentRunner.java` | `334F936B45C2D44DC628C574CFEB5A5B0D8E1C000B7C89B0AB096E447DE8AA27` | 2026-10-29T20:04:33.714Z |
| `src/main/java/com/bslx/XueXiMod/network/CastExecutionSession.java` | `F5596A56DC07140130F8A0F2F966AC082805BEF5361565A2B6BB259766698B0A` | 2026-10-28T10:28:56.690Z |
| `src/main/java/com/bslx/XueXiMod/network/AttackBudgetWindow.java` | `181A33A06A28B734E998AB574977917396C9AB5A09FB8716A3F9DBA55108F841` | 2026-10-23T21:58:56.371Z |

## 最终文件

| 文件 | SHA256 | UTC mtime |
| --- | --- | --- |
| `src/main/java/com/bslx/XueXiMod/network/ChainSteps.java` | `3619111E65C2E3488F1B3A3511CC006468A1D5B470D38CF45528B3C693E9650C` | 2026-10-30T02:44:58.634Z |
| `src/main/java/com/bslx/XueXiMod/network/CompiledReleaseStep.java` | `FCFC3882CCE28C0EAB008AB1A5314FCD0BA6B3925DBC81F3064496B87AA90DB2` | 2026-10-30T02:46:21.514Z |
| `src/main/java/com/bslx/XueXiMod/network/SkillChainPlan.java` | `7B399681F7E2525EFFF2C23884BCDE1A6A434EF2553CAFD52E31AC319019E287` | 2026-10-30T02:48:01.764Z |
| `src/main/java/com/bslx/XueXiMod/network/SkillUsePacket.java` | `A17D7A0D85D875AD4D7C388EA42CF939EF40269E35CFCB9A0B73D6113583D498` | 2026-10-30T02:50:51.877Z |
| `src/main/java/com/bslx/XueXiMod/network/SkillSegmentRunner.java` | `54F1649A6C316378CF74555C5EDB2043B9C8DED4FD1687D3C23759C6856A8DC4` | 2026-10-30T02:48:56.646Z |
| `src/main/java/com/bslx/XueXiMod/network/CastExecutionSession.java` | `3C400A2B378D36C0E476E4F22892E87A0418AACEB3F217E27E38492C04DB6055` | 2026-10-30T02:51:20.186Z |
| `src/main/java/com/bslx/XueXiMod/network/AttackBudgetWindow.java` | `89B989FA59363064A73A10D3B039D5C40DD41E75A899344AC3387DCB3E8742C4` | 2026-10-30T02:51:12.436Z |
| `src/main/java/com/bslx/XueXiMod/network/CompiledCastPlan.java` | `0A61341EFE5238EC8502966B63268FB548B15BBAAB7F68651969BAFCDA3B9DE1` | 2026-10-30T02:46:03.060Z |
| `src/main/java/com/bslx/XueXiMod/network/CastPlanContext.java` | `94B1CF0E0DAF8A0A3180521DB8689BCC022EA2F397F0EC2B952466F86AC3D87F` | 2026-10-30T02:45:03.434Z |
| `src/main/java/com/bslx/XueXiMod/network/CastPlanCache.java` | `73B71269FD3DC1A1CE765F6375A328F847F4C28664C5A5D1E651DDBE56B85D0A` | 2026-10-30T02:45:14.979Z |
| `src/main/java/com/bslx/XueXiMod/network/CastPlanCacheEvents.java` | `B8723B32E4CD22487A6D9A86C44EEE231F30A6FC2F0F3D7D60B591F0075C8B94` | 2026-10-30T02:44:59.782Z |
| `docs/术式链计划缓存说明.md` | `4387386ADA38AB9E8CC64BD0268D9D834E065FE1B7C42A5566DA61590916413E` | 2026-10-30T02:55:51.167Z |
