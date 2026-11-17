我先读取这份任务说明，确认授权范围后再按它执行。任务把我定为源码作者 A 的 A1 阶段：只建公共配置接口。我先读本目录的合同和裁决，再按已批准范围写代码。合同和裁决已经对齐。接下来对照现有术式类型和命名门禁，确定公共接口该怎么落，再只在授权目录里写。公共接口只落在 `shu/balance`。我先把新建文件清单写入 `pre-A`，再按已批准合同实现，不提前做 A2 的加载和接入。清单已记下。接下来按裁决把声明、封存、分侧快照和一次性倍率算进 `shu/balance`，不改其他源码。A1 已收束。公共平衡接口在 `shu/balance`，B/C 可以按合同声明和读取。本阶段没有接入玩法，也没有编译。

## 实际文件

没有修改既有源码。新建清单在 `docs/开发记录/配置平衡-20261101/pre-A/新建文件清单.txt`，路径清单在 `docs/开发记录/配置平衡-20261101/author-A-manifest.json`。

`src/main/java/com/bslx/XueXiMod/shu/balance/` 下 24 个类：`BalanceRegistry`、`TechniqueBalanceSpec`、`BalanceService`、`BalanceSnapshot`、`BalanceParameter`、`RawDefault`、`BalanceKeys`、`BalanceBound`、`GlobalBalance`、`RawDamage`、`ConfiguredDamage`，以及侧别、单位、警告和异常类型。`BalanceMath` 只给包内舍入使用。

API 合同：`docs/开发记录/配置平衡-20261101/API合同.md`。

## B/C 可立即使用的接口

声明专项参数：

- `TechniqueBalanceSpec.builder(techniqueId)` 后 `manaFee`、`integerParameter`、`numberParameter`
- `BalanceRegistry.declareSpecial(spec)`

不要调用 `seal`。标准键由 A2 在全部术式登记完成后调用 `fillStandard`，再 `seal`。不同键直接合并；同键只有合同相同才视为一次声明，合同不同抛出 `BalanceConflictException`。

读取：

- `BalanceService.damageContribution(techniqueId, rawAmount)` 返回 `ConfiguredDamage`。术式 `damageMultiplier` 与全局 `damageMultiplier` 各乘一次。两个倍率都是 1 时，只做与现有伤害相同的 `float` 收窄。
- `BalanceService.manaCost(techniqueId, parameterKey, rawDefault)`：已声明的魔力单价，再乘一次全局 `manaMultiplier`。
- `BalanceService.cooldownTicks(techniqueId, rawDefault)`：基础冷却，再乘一次全局 `cooldownMultiplier`。原始 0 保持 0。
- `BalanceService.number` / `intValue`：专项技术值，不乘全局倍率。未声明键抛出。魔力键和 `cooldownTicks` 必须走上面两个入口。

`RawDamage` 是缩放前的贡献。`ConfiguredDamage.plus` 与 `timesHitCount` 只做已配置金额的相加和命中份数相乘。旧存档用 `ConfiguredDamage.retained`。这三处不再读倍率。`Technique.baseDamage` 仍是结构字段，不能声明成配置键。

原始数字用 `RawDefault.literal` 引用现有常量，或用 `RawDefault.calculator`。计算器只在 `seal` 时求一次，禁止读取 `BalanceService`。已有冻结值时，读取参数里的 `rawDefault` 不覆盖它。

## A2 仍要做的安装与清理

`BalanceSnapshot.resolve` 合成快照，单个非法字段回退并写入 `BalanceWarning`。`BalanceService.install(side, snapshot)` 与 `bind(side)` 分侧保存。断线只 `clear(CLIENT)`，客户端线程保持绑定 `CLIENT`。服务端停止才 `clear(SERVER)`。合同第 12 节有完整顺序。JSON、世界文件、62 项标准登记、对外事件和玩法调用都还没接。

## 命名门禁与状态

`tools/check-name-gate.ps1` 退出码 1。本次新增文件没有违规。仅有的两处在未改动的 `animation/runtime/OriginalHumanoid.java`：165 行 `i`，202 行 `to`。本阶段没有改它们。

没有阻塞 B/C 按合同接入。待统一编译。游戏未测。本阶段写入已全部收束，所有权已释放。
