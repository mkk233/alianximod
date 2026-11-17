# 作者 A 阶段 A1 报告

模型 Cursor CLI grok-4.7-high-fast。未递归派发，未编译，未启动游戏，未改制作表和其他作者文件。设计 1/2，实现校验仍为 0/3，本阶段不做正式正确性复审。

全部写入已收束，`shu/balance` 与本阶段文档的所有权释放。

## 新增文件

没有修改既有源码。新建清单在 `docs/开发记录/配置平衡-20261101/pre-A/新建文件清单.txt`。清单在写 Java 之前落盘。完整路径见同目录 `author-A-manifest.json`。

公共代码在 `src/main/java/com/bslx/XueXiMod/shu/balance/`，共 24 个类。API 合同在 `docs/开发记录/配置平衡-20261101/API合同.md`。

## 接口与数据流

注册期只调用 `BalanceRegistry.declareSpecial` 或 A2 的 `fillStandard` / `completeStandard`，保存键、种类、原始默认来源、边界、单位和说明。`seal` 时计算器求值一次并冻结代码默认。之后 `BalanceSnapshot.resolve` 把玩家覆盖值合成不可变快照，单个非法字段回退默认并记入 `BalanceWarning`。`BalanceService.install` 按 `BalanceSide` 分侧保存。未安装时 `view` 返回封存后的代码默认快照，不读取另一侧，也不在类初始化里捕获配置。

B/C 立即可以使用：

- 声明：`TechniqueBalanceSpec.builder`、`BalanceRegistry.declareSpecial`
- 专项读取：`BalanceService.number`、`BalanceService.intValue`
- 伤害：`BalanceService.damageContribution`，输入 `RawDamage`，输出 `ConfiguredDamage`
- 费用和冷却：`BalanceService.manaCost`、`BalanceService.cooldownTicks`

A2 封存和安装使用 `fillStandard`、`seal`、`contractText`、`BalanceSnapshot.resolve` / `defaults`、`BalanceService.install` / `clear` / `bind`。细节、签名、合并规则和同步包内容都在 API 合同。

## 配置键与消费点

本阶段没有把 62 个术式登记进目录，也没有玩法调用点。消费发生在公共入口内部：

- 术式 `damageMultiplier` 与全局 `damageMultiplier` 只在 `damageContribution` 各乘一次。代码默认倍率固定为 1。两个倍率都是 1 时，结果只做与现有伤害相同的 `float` 收窄。
- 单位为 `MANA` 的已声明整数键只在 `manaCost` 中再乘一次全局 `manaMultiplier`。倍率是 1 时保持原整数。返回单价，不乘份数。
- `cooldownTicks` 只在 `cooldownTicks` 中再乘一次全局 `cooldownMultiplier`。原始 0 保持 0。倍率是 1 时保持原整数。
- `number` / `intValue` 返回专项技术值，不乘全局倍率。魔力键和 `cooldownTicks` 走这两个方法会抛出。

`Technique.baseDamage` 不在本包读取，也不能声明成配置键。伤害倍率为 0 时贡献可以是 0，类型仍是已配置攻击金额，不提供改结构角色的开关。

已配置金额用 `ConfiguredDamage.plus` 和 `timesHitCount` 继续合并；旧最终数值用 `retained`。这三处不再读倍率。

## 默认兼容与限制

未配置时全局三项都是 1，术式伤害倍率的代码默认也是 1。费用、冷却在全局倍率为 1 时不做第二轮舍入。本阶段尚未接入玩法，所以现有术式行为不变。

边界、别名、坏文件和分侧清理见 API 合同第 8 到第 12 节。减伤上端是现有护体和场地的 `0.6f`。冷却只有代码默认值为 0 时才接受配置 0。

## 命名门禁

已执行 `powershell -NoProfile -File tools/check-name-gate.ps1`，退出码 1。本次新增文件没有违规。

仅有的两处在未改动的既有文件，本阶段不改：

- `src/main/java/com/bslx/XueXiMod/animation/runtime/OriginalHumanoid.java:165` 变量 `i`
- 同文件 `:202` 变量 `to`

## 尚未完成

没有阻塞 B/C 按 API 合同接入声明和读取。

留给 A2，且本阶段故意没有做：

- JSON 加载、`defaultconfigs` 与世界 `serverconfig`、损坏文件保留
- 服务端启动和停止、客户端同步、`contractText` 比对后的安装
- 62 个术式的 `fillStandard` 与 `seal`
- `RegisterXueXiContentEvent` 上的声明委托
- `Technique`、网络、连锁、命中和界面改为调用本入口

待统一编译。游戏未测。
