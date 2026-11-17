我先完整读这份任务说明，再按其中授权范围执行。任务是只写接口交接文档。我先读设计、流程和本批六份只读报告，再按第 14.1 节冻结跨作者 API。设计已定稿。接下来核对现有签名和文件路径，把合同写成可直接实现的精确 API。路径和现有签名已经对上。设计里的客户端渲染目录、屏幕过滤和性能汇总方式需要按仓库实况校正，我现在把这些写进合同。接口合同已经冻结。本单只写了两份文档，没有改 `src`，没有编译，也没有实现 core。

- `D:/alianximod/docs/开发记录/分身底座-20261102/implementation-contract.md`
- `D:/alianximod/docs/开发记录/分身底座-20261102/author-A-prep-report.md`

关键约定：

- 魔力收据只由 A 持有、扣款和至多退一次。B 的 `prepare` 不含收据，也不设置 `COMMITTING`、次数或冷却。提交线之后不因未命中退款。
- `manaCost`、`cooldownTicks`、`damageMultiplier` 不在专项里重复声明。玩法读取固定走 `BalanceSide.SERVER`。默认数值只定义在 `CloneBalance`。
- 两个生成定义是 `ACTION`/`ACTION`，脉冲是 `ACTION`/`ATTACK`。`explicitManaCost` 为 10、10、2。定义上的 `lifetimeTicks()` 返回 1，只为满足接口，不是分身寿命。
- 渲染类按现有客户端包校正为 `com.bslx.XueXiMod.client.CloneRenderer`。命令挂在 `ShuCommands`。性能常量只追加在两个枚举末尾，并且只有 A 计数。

A、B、C 可以按各自文件并行写。三方都停笔之前，源码还不能当作可编译闭环。

作者 A 停止写入。
