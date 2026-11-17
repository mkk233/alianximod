我先读这份任务说明，按里面授权的范围执行。任务是已批准设计的实现。我先读设计文档和相关源码，再备份将改的文件。设计已读完。接下来只读授权范围内的源文件和直接依赖，确认现有解析接线后再备份。设计要求先备份再改。我先把将改的源文件按相对路径复制到 `backup-A`，并记下哈希和时间。备份已落盘。开始按设计接入缓存计划，并让按下、接触和段执行实际消费它。`List.copyOf` 不能存放表示“不在同组”的空位。我先改掉这个存储，并接上结构选择核心。命名检查里，本次改过的文件没有新的短名问题。接下来写说明和最终记录，并计算各文件哈希。作者 A 的结构计划缓存已经落地，文档和证据已写完。全部写入已经停止，文件已释放。没有编译、没有打包、没有启动游戏、没有正式复审。

开始 `2026-10-30T02:42:39.756Z`，证据收束 `2026-10-30T02:56:44.338Z`。完整记录在 `docs/开发记录/术式链计划缓存-20261030/implementation-A-final.md`，哈希在同目录 `evidence-A.json`（`released=true`）。使用说明在 `docs/术式链计划缓存说明.md`。

## 范围

新增 `CompiledCastPlan`、`CastPlanContext`、`CastPlanCache`、`CastPlanCacheEvents`。修改 `ChainSteps`、`CompiledReleaseStep`、`SkillChainPlan`、`SkillUsePacket`、`SkillSegmentRunner`、`CastExecutionSession`、`AttackBudgetWindow`。`TechniqueChain` 未改，切开仍只有 `TechniqueChain.split`。作者 B 的文件、构建配置和制作表未改。改动前的七个源文件在 `backup-A/`，哈希与改前记录一致。

## 接线

`SkillUsePacket.executeSkill`（250 行）调用 `CastPlanCache.lookup`（61 行）。键是技能类型、有序术式 id、原胚和结构代际，相等比较走全字段。命中后步进下标来自缓存，`progressSignature` 仍是 `ChainSteps.signature` 的纯 id，冷却仍按当次法杖缩减现算，分段用该切片里同一批 `ChainSegment`（311 行），不再切开。未绑定、线程或服务器不符、定义不够格、链长超过 64、编译超出 16384 单元时返回 null，完整走原来的 `list`、`signature`、`slice` 和 `split`。没有 ThreadLocal。

实际消费的结构：

- 编译切片时调用一次 `TechniqueChain.split`（`CompiledCastPlan.compile` 34 行）。
- `CompiledReleaseStep.chooseStructure`（65 行）是双重路线和动作后单一条件路线的选择核心。按下时按原顺序重算份额、冷却和蓄力拒绝，再新建当次对象。成员下标指向同一份分段。
- `SkillChainPlan.captureModifierTemplate`（139 行）在编译时扫描修饰。命中后按起点新建 `ModifierPlan`。`skill == null` 的预扣仍用 `ReleaseInput.BASE`。
- `copiesBefore`、`releasedSegments`、`sameReleaseGroup`、`lockThisCast`、`dropThisCast`、`frozenChainInput` 在代际一致时读缓存，否则调用原方法。费用数字仍在调用时现算。
- `SkillSegmentRunner.scanRoles`（692 行）是唯一角色扫描。`runSegment`（506 行）经 `resolveRoles`（731 行）读取。蓄力重入传同一个上下文，不提前读位置或视线。
- `CastExecutionSession`（358 行）和 `AttackBudgetWindow`（494 行）保存当次上下文，接触和第二名成员把它传下去。伤害、主手和快照仍在原提交点计算。

`ChainSteps.freezeListed`（77 行）只把已有 `list` 的下标收成不可变副本。`CastPlanCacheEvents`（16 行）在服务端启动时绑定线程并换空表，停服时清空并丢掉引用。旧的无上下文重载都还在，内部传 null。`HoldRepeat` 和 `SkillFollowCast` 未改。

## 计数与边界

一次查找只记 hit、miss、bypass 之一。成功放入缓存后再记 compile。预算失败只记 bypass。`CAST_PLAN_COMPILE` 只包真实编译，`CHAIN_SPLIT` 只包真的切开，`CHAIN_ROLE_SCAN` 只包真实扫描；命中读标志，不计时。5 刻与 6 刻排程未改。代际失配后继续用手里那份分段；已冻结的当次数值不回写，延迟点改回原来的动态读取。

## 风险与未验证

作者 B 的 `ChainStructureEpoch`、`supportsStructuralPlanCache` 和四个 `CAST_PLAN_*` 计数尚未由本作者创建。这些符号落地前，工程不能编译。名称门禁扫完全部 `src` 时，未改动的 `OriginalHumanoid.java` 仍有两处旧短名（`i`、`to`），本次改过的文件没有新的短名或禁止缩写。极端预算中途放弃时，放弃的编译可能已经记过一次 `CHAIN_SPLIT`，旧路径会再切一次；玩法仍走旧路径。命中不保证每刻耗时下降。

没有运行 `gradlew`，没有 `compileJava`，没有打包，没有进游戏。
