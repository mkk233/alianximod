# 水幻形 C 组实现报告

日期：2026-11-02。阶段：实现。作者只写本文件和下列 C 分区源码。实现复核 0/3，本次没有开独立正式审查。未编译，未进游戏。待统一编译。

本报告写完后 C 组停止写源码。

## 结论

整链捕获、法杖载体门和装配写回已经按合同第 3.3、4.1、6、8、9 节接上。`water_phantom` 仍未注册，专项声明没有调用方，现有旧链的 `actorCapture()` 仍是 `NONE`，结构检查返回不适用并继续原来的释放。视觉 v2 未确认，没有碰美术，没有把候选资源写入 `src`。

## 实际改动

新增：

| 路径 | 行号 | 作用 |
|---|---|---|
| `shu/technique/release/capture/ActorCaptureMode.java` | 全文 | `NONE` / `NEXT_SINGLE_ATTACK` |
| `shu/technique/release/capture/CaptureCarrierMode.java` | 全文 | `NONE` / `STAFF_ONLY` |
| `shu/technique/release/capture/ActorCaptureStatus.java` | 全文 | 不适用 / 拒绝 / 计划 |
| `shu/technique/release/capture/CaptureRefusal.java` | 全文 | 合同里的拒绝码，含法杖两码 |
| `shu/technique/release/capture/CaptureCarrierDecision.java` | 全文 | 不适用 / 草稿 / 就绪 / 拒绝 |
| `shu/technique/release/capture/ActorCaptureTexts.java` | 6–24 | 玩家中文 |
| `shu/technique/release/capture/ActorCaptureContract.java` | 12–36 | 声明记录；`NONE` 是空例外集 |
| `shu/technique/release/capture/CapturedStructurePlan.java` | 10–36 | 原始下标计划，列表在构造时复制 |
| `shu/technique/release/capture/ActorCaptureInspection.java` | 8–28 | 结构结果 |
| `shu/technique/release/capture/CapturedInput.java` | 10–29 | 服务端冻结输入，武器再复制一次 |
| `shu/technique/release/capture/CaptureDispatchResult.java` | 10–31 | 分发结果 |
| `shu/technique/release/capture/CaptureCarrierCheck.java` | 8–31 | 载体结果；只有就绪带计划 |
| `shu/technique/release/capture/ActorCaptureCompiler.java` | 23–103 | `inspect` |
| `shu/technique/release/capture/ActorCaptureCarrierGate.java` | 26–187 | 载体门和类型例外 |
| `shu/technique/release/capture/ActorCaptureDispatcher.java` | 46–176 | 报价、合计、调用生成 |
| `shu/technique/release/clone/water/WaterPhantomDefinition.java` | 20–91 | 定义类，未注册 |
| `shu/technique/release/clone/water/WaterPhantomBalance.java` | 17–55 | 声明和读取，均无调用方 |

修改：

| 路径 | 行号 | 作用 |
|---|---|---|
| `shu/technique/release/ReleaseDefinition.java` | 474–476 | 默认 `actorCapture()` 返回 `NONE` |
| `debug/performance/PerfCounter.java` | 84–93 | 枚举末尾五个捕获计数 |
| `debug/performance/PerfMetric.java` | 76–77 | 枚举末尾 `ACTOR_VOLUME_UPDATE` |
| `item/StaffSkills.java` | 115–174 | `castRefusal` 先查原始条目，再按门豁免 |
| `item/StaffSkills.java` | 263–437 | 原始条目故障、捕获编辑判断、`replaceEntriesExact`、保存提示 |
| `network/SkillUsePacket.java` | 207–226 | `offerNow` 第二道法杖剑术门 |
| `network/SkillUsePacket.java` | 269–328 | `routeCapture`，位于批量编译之前 |
| `network/SkillUsePacket.java` | 335–337 | `executeSkill` 调用上述路由 |
| `network/SkillAssemblyPacket.java` | 136–143 | 捕获相关走完整候选；普通法杖仍拒裸剑术 |
| `network/SkillAssemblyPacket.java` | 254、271 | 删除和移动后的草稿提示 |
| `network/SkillAssemblyPacket.java` | 311–440 | 成对消耗与原子替换，失败恢复两侧标签 |
| `gui/SpellAssemblyMenu.java` | 296–303 | 捕获相关齿轮写回走 `replaceEntriesExact` |
| `gui/SpellAssemblyMenu.java` | 359–367、409–487 | 替换后的非空紧凑候选 |
| `gui/SpellAssemblyMenu.java` | 741–787 | 写入失败时退回齿轮和物品 |

没有改 `ModTechniques.java`、`ReleaseDefinitions.java`、`HoldRepeat.java`、`BalanceCatalog`，也没有改 A/B/D 的文件。`HoldRepeat.drop` 原签名已够用。`ticks.csv` 的 11 列表头没有动。

命名门禁在本次源码写入之后执行：`powershell -NoProfile -File tools/check-name-gate.ps1`，退出码 0。A/B/D 若之后再写，这次结果不能当成全批最终门禁。

## 门和检查器

`ActorCaptureCompiler.inspect` 只看原始术式列表和原胚。任一格的捕获模式不是 `NONE` 才继续，否则立刻不适用，文本为空，不加编译计数。本版计划必须是第 0 格捕获、第 1 格显式适配的攻击、两边原胚 `NONE`、没有附魔、没有第三格。计划下标固定为捕获 0、载荷 `[1]`、消费 `[0,1]`、计费 `[0,1]`、下一格 2，纪元取 `ChainStructureEpoch.current()`。不按 `water_phantom` 或 `tide_sever` 编号分支；适配器来自 `ActorPayloadRegistry.find`。

`ActorCaptureCarrierGate.inspect` 以编译器为结构来源。

- 空链：不适用，调用方留旧规则。
- 没有捕获声明的链，包括单独的断潮：不适用。法杖旧剑术拒绝仍由调用方执行。
- 施放传入 `permitIncompleteDraft=false`。一格合法宿主是拒绝，不是草稿，不能放。
- 编辑传入 `true`。只有一格、无原胚、`STAFF_ONLY` 的捕获宿主能成草稿，提示「缺少支持的下一攻击」。草稿没有计划，`permitsCapturedType` 对草稿返回 false。
- 就绪必须有计划，而且实际法杖为真。载荷若是剑术，类型还必须落在宿主声明的不可变例外集里。
- 非法杖上的捕获返回 `CAPTURE_REQUIRES_STAFF`，文字「该捕获术式只能装配并释放于法杖」。
- 剑术没有被完整计划收走时返回 `CAPTURE_INVALID_CARRIER_SLOT`。
- 反序、第三格、第二个宿主、未适配载荷保持结构拒绝。非 `NONE` 原胚是 `CAPTURE_UNSUPPORTED_EMBRYO`。

`permitsCapturedType` 只在就绪、下标同时属于载荷下标和消费下标、适配器版本与属性相符、玩家可用、并且类型在唯一匹配的 `STAFF_ONLY` 宿主例外集中时返回 true。多个同类宿主无法唯一确定时不豁免。

三道施放门：

1. `StaffSkills.castRefusal`（115–174）仍是 `offerNow` 更早的门。先拒绝未知编号、开发术式、未学会或未解锁、坏槽和溢出，再原样构造技能。就绪时只跳过 `permitsCapturedType` 为真的载荷剑术；其余剑术仍返回原来的 `gui.xueximod.staff.sword`。草稿和拒绝返回捕获文字。不适用仍走旧剑术拒绝。不改法杖 NBT。未学会的剑术现在先报未学会，再轮到类型；这是合同要求的检查顺序。
2. `SkillUsePacket.offerNow`（207–226）用同一个门，`permitIncompleteDraft=false`。不因为链上出现捕获术式就跳过整段剑术检查。持剑门、槽位、冷却忙碌和学习统计都留在原处。
3. `executeSkill` 在 `BatchReleaseCompiler.compile` 之前进入 `routeCapture`（335–337，方法本体 269–328）。不适用返回空，继续旧编译，不解除普通长按。拒绝或计划都 `HoldRepeat.drop`，成功和失败都不再回到主人循环，第二格不会进原来的按次收费。计划再交给 `tryExecute`。分发器重新核对实时主手是法杖且槽位是 0、原始条目没有被 `asSkill` 滤短、计划与当前结构纪元一致、载体门仍是就绪，然后才报价。`staffCarrier` 为假时直接 `CAPTURE_REQUIRES_STAFF`，不调用 B 的 `freezeQuote`，也不调用 A 的 `spawnCaptured`。

## 装配和编辑

法杖追加先复制当前全部条目和原胚，再追加待加项，对整份候选做准入。只有草稿或就绪能走捕获例外。先放宿主是草稿，再放已适配攻击才就绪。先放裸断潮仍是旧拒绝「无法装配：剑术不能装入法杖」，不消耗匣子。超过两格、反序、未适配、非默认原胚都拒绝，法杖和匣子不变。

捕获相关追加在消耗前核对匣子里仍有同一术式和原胚。消耗和 `replaceEntriesExact` 成对提交。替换失败时两侧标签恢复到本次之前，不忽略添加失败。`replaceEntriesExact` 先在独立列表里验证容量、已知编号和原胚，通过后一次换上术式列表；失败不写。它只换技能列表，不改法杖魔力根标签，也不单独发放剑术许可证。

菜单 `rejectReason` 按「该齿轮替换之后」的非空石顺序组候选。空齿轮不占逻辑下标。`mayPlace` 和 `set` 用同一判定。剑技槽仍拒绝法术，同步仍跳过法术，筛选按钮没改。

纯删除和移动仍由服务器实际操作完成，不信任客户端同时插入。拆掉宿主后留下的剑术或反序可以原样保存，提示「当前捕获链不可释放」或草稿的「缺少支持的下一攻击」，不获得就绪，也不能据此再追加非法条目。写回失败时齿轮恢复原栈；放进空齿轮失败会把物品退回光标，替换失败则放回背包或掉落，避免只恢复标签而物品已经拿走。普通法术链仍是原来的清空再逐条添加，本任务不把那条旧路径改成已修复。

未知编号或无法识别的原胚在构造技能前拒绝。`asSkill` 仍会跳过未知编号，捕获路径不使用那条跳过结果。

## 付款、冷却和配置

分发器不调用 `ManaPaymentReceipt.charge`。唯一付款是 `CloneService.spawnCaptured`（`ActorCaptureDispatcher` 170 行）。生成前用 `long` 把宿主魔力与载荷魔力相加，冷却同样相加；超出 `int` 非负范围则 `CAPTURE_INVALID_QUOTE`，不夹成 0。合计不再乘第二遍全局倍率。

成功时 `SkillUsePacket` 只对合计冷却调用一次 `SkillSegmentMath.scaleCooldown`（316 行）。现有 `offerNow` 在接受且不延后结算时 `CastCooldown.mark`，`fired()` 只有宿主这一条，断潮不再计一次使用。失败不走到 `mark`。魔力不足返回 `CastTiming.insufficient()`，同样不冷却。同一主人同一个服务器 tick 只记录成功部署；该 tick 再放返回「这一刻已经部署过水幻形」。

潜行来自 `owner.isShiftKeyDown()`，位置和视线来自服务器玩家并在输入里单位化视线。潜行时诱敌半径写成 0，身体生命固定 1.0，不读生命配置。驻留寿命和诱敌先各读一次，立即模式不靠寿命推迟发射。

`WaterPhantomBalance` 可声明的专项只有：

- `lifetimeTicks`：整数，默认 0，范围 0..1200000，单位刻。0 表示本次加载会话内不到期。
- `decoyRadius`：小数，默认 8，范围 0..16，单位格。0 关闭诱敌。

标准 `manaCost` 默认 10、`cooldownTicks` 默认 20 不在专项里重复声明。宿主定义的 `explicitManaCost()` 返回 10。冷却原始默认用宿主术式自己的 `getBaseCooldown()`；合同里的水幻形构造是 20，登记之后这一次读取就是 `cooldownTicks(SERVER, 宿主编号, 20)`。`lifetimeTicks()` 接口仍返回 1，不拿它当身体寿命。

真实消费者在分发器 115–119 行，各一次：`BalanceService.manaCost`、`cooldownTicks`、`intValue(lifetimeTicks)`、`number(decoyRadius)`。键还没封存或没声明时捕获 `BalanceException`，变成报价拒绝，不生成、不冷却、不返回假数字。`WaterPhantomBalance.read` 在未封存时抛 `IllegalStateException`，本次没有调用它。`declareSpecials()` 本次没有调用方。没有把水幻形放进 `DAMAGE_HOSTS`，也没有写成 `DEVELOPMENT_ONLY`。

生效时机：只有将来的整合先登记术式、再登记释放定义、再 `declareSpecials()`、最后走到现有 `seal` 之后，这些读取才会在一次真实计划里生效。世界重开后才吃新配置。缺键回各自默认；本任务没有重写旧配置文件。

## 依赖和未做

- 生成、退款和发布依赖 A 已公开的 `CloneService.spawnCaptured(ServerPlayer, PreparedCapture)`。C 只调用这个签名。
- 载荷报价依赖 B 的 `ActorPayloadRegistry.find` 和 `freezeQuote`。断潮适配器要已经由 `ClonePulseAction.register` 登记，计划才认得出它。C 不登记适配器。
- 正式 `PLAYER` 术式、`ReleaseRegistry.register` 和专项声明仍等视觉确认后的整合。在那之前玩家到不了水幻形，旧链保持不适用。
- A/B/D 的文件本次没有改。他们若仍在写，统一编译要等那些写入收束。
- 没有 Gradle。没有 `compileJava`。没有打包，没有启动游戏。游戏效果未测。
- 没有新建测试。没有改制作表。没有操作美术 MCP，没有读取艺术中间稿，没有把 v1/v2 资源写入 `src`。

## 合同状态

第 2/4/5/11 节和 15.1 的 C 范围，以及第 9 节的载体门、候选链和原子写回，已按上述代码落地。第 8 节明确不调用的四项仍未调用：术式注册、释放注册、`declareSpecials()`、伤害宿主名单。

C 组源码到此停止。
