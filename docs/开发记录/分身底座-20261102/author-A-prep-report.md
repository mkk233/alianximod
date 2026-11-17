# 作者 A 实施准备报告

批次：分身底座-20261102。角色：核心作者 A。阶段：实施准备。模型：Cursor CLI grok-4.7-high-fast。设计 1/2 已定稿，本文不是新的设计轮或实现复审。实现仍为 0/3。

## 产出

- `D:/alianximod/docs/开发记录/分身底座-20261102/implementation-contract.md`
- `D:/alianximod/docs/开发记录/分身底座-20261102/author-A-prep-report.md`

没有写 `src`、资源、制作表或其他共享文档。没有编译，没有测试，没有打包，没有启动游戏，没有提交，没有派代理。

## 已读

`AGENTS.md`、`.grok/skills/max-dispatch/SKILL.md`、`docs/统一开发与术式批量制作流程.md`、`docs/分身底座设计与实现指导.md`、本批 `design-decision.md`，以及六份已完成只读报告：`read-casting-final.md`、`read-performance-config-final.md`、`read-damage-followup-final.md`、`read-config-followup-final.md`、`read-availability-final.md`、`read-registration-followup-final.md`。

对照过的现有签名包括 `Technique` 十条与将保留的旧构造、`ReleaseDefinition` 八个必要方法、`explicitManaCost` 默认 `-1`、`role` 默认 `ATTACK`、`ManaPaymentReceipt.charge/refund`、`BalanceService` 的 `BalanceSide` 重载、`BalanceRegistry.declareSpecial`、`DamagePermission.allows`、`TechniqueHits.classifyRespecting`、`LivingCandidateQueries.entityQuery/targetQuery`、`PerformanceRecorder.begin/end/add`，以及 `PerfMetric.values()` 汇总和硬编码 `ticks.csv`。

## 关键约定

- 收据只由 A 保存、扣款和至多退一次。B 的 `prepare` 入参不含收据。`COMMITTING`、幻影额度、冷却起点只在 A 的提交线写入一次。`HitVerdict` 不决定退款。
- 标准 `manaCost`、`cooldownTicks`、`damageMultiplier` 不在 `CloneBalance.declareSpecials` 里重复声明。玩法读取固定传 `BalanceSide.SERVER`。数值默认只定义在 `CloneBalance` 常量。
- 生成定义 `ChainRole.ACTION`，脉冲定义 `ChainRole.ATTACK`，两者 `ReleaseKind.ACTION`。`explicitManaCost` 必须是 10、10、2。定义上的 `lifetimeTicks()==1` 不是实体寿命。
- 渲染类校正到 `com.bslx.XueXiMod.client.CloneRenderer`。命令挂在 `ShuCommands`，不写 `ModCommands`。两份屏幕没有目录扫描，本批不写它们。性能只追加枚举，不写 `PerformanceRecorder`。
- A 独占性能计数调用。B/C 不调用，避免双计。
- 空闲 tick、服务器线程、无新线程、无 `ChainScheduler`，按设计保持。

没有必须退回设计的架构缺口。准备入参与收据拆开，是为了落实「准备不扣费、提交线之后不退款」，没有改费用或次数。

## 并行

可以按 A/B/C 并行写各自文件。路径没有交集。三方都停笔前不能当作可编译闭环。本单不实现 core。

## 停止写入

作者 A 停止写入。后续源码实现需主会话另开授权。本文档若要再改，也需重新指定这一份文档的作者。
