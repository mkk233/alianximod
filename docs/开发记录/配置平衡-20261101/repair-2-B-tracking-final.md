我先读这份跟踪返修的完整范围，再只改它授权的部分。正伤抛出时，已经造成受伤的接触收据会留下，不再一律退回。有限 0 仍按接触成立下发状态，不产种。这次没有做正式复核。本次写入已经释放。

正伤先扣款，再调用六参数的 `classifyConfirmed`，并传入 `ConfirmedDamage`。正常返回时，只有 `grantsSeeds()` 才留下收据、下发状态并产种。调用途中抛出时，若 `healthApplied()` 或 `absorptionReduced()` 已经为真，先 `retainShare` 记下这笔收据，随后继续抛出；收尾看到收据已留下，就不再退款。没有这些受伤标记时，仍退回这一笔。`accepted()` 仍只表示正伤被接受，不拿它在异常里决定退款。

有限 0 仍走原来的五参数裁决。`continues()` 成立才扣一次并下发状态，不产种，同一次停留不重复扣。这条路径没有接受伤跟踪。

只改了 `DomainContactBilling.java`。修前字节在 `docs/开发记录/配置平衡-20261101/pre-repair-2/B-tracking/`，原先的 `pre-B`、`pre-repair-1` 和 `pre-repair-2/B` 都没有覆盖。`author-B-manifest.json` 已记下这次衔接。`TechniqueHits` 没有改。

变量名检查仍只报出未改的 `OriginalHumanoid.java` 第 165 行和第 202 行。统一编译没有跑，游戏也没有实测。
