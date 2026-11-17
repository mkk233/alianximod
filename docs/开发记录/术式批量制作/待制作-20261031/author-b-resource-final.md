我先读任务规格和相关流程，确认这次授权范围后再动手。任务是 B 的资源 API 与六个附魔第一阶段。我先读本批合同和现有源码边界，再按授权范围写实现。合同已读完。接下来只读 B 独占的现有火种、附魔和炉心接缝，不碰 A 正在写的公共链。边界已经明确：旧点火、光束和火球的入账保持原样，新的六种附魔走独立载荷。我先写下文件所有权，再只改 B 这一侧的资源和附魔代码。命名检查里，本次改过的文件没有新的短名问题。接下来写接口说明和最终报告。B 的第一阶段资源实现已经写完并停止写入。这是实现，不是正式复审。设计额度 2/2 已用完，实现复审仍是 0/3。模组没有编译。

## 结论

留焰、种火、引燃、引爆、幽冥、崩地已登记为独立载荷。魔力价格是 2，直接伤害是 0，效果键不是旧的 `fire`。旧五元素的伤害、燃烧、击退、潮印和固定 2 点火种仍按原来的效果键计算。

同一次确认命中里，火苗本体 2 和种火 10 先加成 12，再乘 `seedMultiplier` 并向下取整一次。倍率 1.12 时请求是 13。旧点火的 2、热光束的 1、火球的 3 作为固定点数加在取整之后，最后一起钳到余额 100。这次使用调用方传入的根施法上下文，不另建上下文。请求大于 0 时，即使余额已满、增加量为 0，也会占掉该目标这一次产种机会。

## 已实现

- `ResourceHitCommit.commit` 只在伤害已接受时执行，顺序是合并产种、挂引燃、引爆。未接受返回 `NOT_ACCEPTED`，不改余额、不挂层、不引爆，也不扣魔力。
- 引燃按 `n = floor(余额 / 10)` 启动，不足 1 档不挂。期限是 `40 + 10n` tick，首跳等待完整 10 tick，段数对准到期当刻。伤害是 `2 × (1 + 启动时的 fireBonus)`，资源增量是 0。满 8 层在扣燃料前拒绝。同一施法、目标和 `fire_kindle` 成功挂上后不能再挂。
- 余数轮换在 `RemainderRotation`。本拍只要还有旧共享层一起到期，`BurnLedger.pump` 仍走原来的 `FuelShare.divide`。`settleOpening` 也未改。
- 引爆用 `0.2 × 当前余额 × (1 + 当时 fireBonus)`，只扣当前共享余额，不写旧的 `hasConsumed`。火伤帧标记炉心已结算，`FireRuntimeEvents` 未改。
- 幽冥由 `NetherSeedArbiter.grant` 在一个窗口内只入账最高请求，同值取领域标识字典序较小者。留焰参数在 `TrailContract`。崩地浅碗由 `BreakGroundShape.offsets()` 给出 23 个相对方块，上限 32，没有改世界。

## 待接线

作者 A 需要登记这六个附魔，并把报价改到 `ResourceEnchantCatalog.explicitManaPrice`。现有附魔报价仍按 `magicDamage` 计算，新定义的直接伤害是 0，不改报价就会收成 1。命中结算要把 `EnchantBonus.resourceLoad` 和根上下文送进一次 `commit`。火苗的 2 走新基数，旧点火、光束、火球走固定点数，不能再对同一份点数调用 `deposit` 或 `produce`。`WeaponEnchant` 未改；纯新载荷现在会被旧印记入口丢掉，需要由 A 接到现有武器账上，失败不能当成成功。

作者 C 按 `TrailContract`、`NetherContract` 和 `BreakGroundShape` 做火径、领域周期、蓝火和实际破块。幽冥不要走要求接触伤害大于 0 的旧采样。

炉心倍率和热量没有改。游戏里现在还不能放出这六个附魔。

## 检查

`tools/check-name-gate.ps1` 退出码 1。本次写过的文件没有短名或禁止缩写。剩下两处在未改的 `OriginalHumanoid.java` 第 165 行 `i`、第 202 行 `to`，没有顺手改。名称门禁只解析源码。`compileJava --offline --no-daemon` 没有运行，待统一编译。

记录：

- `author-b-interface.md`：`F852969F5BF66F70AE84001F1164D000AB9717411223918B1A69E0E52C410EA7`，2026-10-31 03:40:55
- `author-b-report.md`：`B99714292A99832DFC5AD80D3F27220A90C0ABB623B1F5C74E8553866EF6BD6E`，2026-10-31 03:41:21
- `author-b-report.json`：`B8CBF65288BAAE4BA822E8FE59D3EFEAF9EA6A8ACC35927DBD89F13E88FE0F82`，2026-10-31 03:41:55
- `author-b-ownership.json`：`C68E73E656D52CB4E8B5A456F262A7579DC338704FFFAE67D71B7077042E602B`，2026-10-31 03:38:18

28 个源码文件的 SHA-256 和修改时间在 `author-b-report.md` 与 `author-b-report.json` 中。写入已停止。
