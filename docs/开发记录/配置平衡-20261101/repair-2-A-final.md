先读返修 2 的任务说明，按授权范围处理。复核写明合法 0 被当成正伤，以及占位 0 把标准耗魔也跳过了。接下来只读授权范围内的命中与报价代码。修前字节先归档，再改命中裁决和占位报价。返修 2 已按问题 1 和问题 4 收口。这一单只落实已写明的修复，没有新设计，也没有算成正式复核。未编译、未启动游戏、未改表、未提交。写入已收束，所有权已释放。

## 问题 1：0 贡献与产种分开

有限且恰好为 0、权限通过，记为 `ESTABLISHED_NO_DAMAGE`。不调用原版 `hurt`，不改无敌帧。负数、`NaN`、正无穷、负无穷，以及权限不通过（空目标、死亡、旁观、自身、无敌、`canHarmPlayer` 拒绝），记为无效。正数仍走原来的无敌帧和确认结果，只有真正打出正伤才是 `DAMAGE_ACCEPTED`。

`hurt`、`hurtRoot`、`hurtConfirmed`、`confirmTracked` 的布尔值恢复为 `grantsSeeds()`。合法 0 返回 false，不再表示可接续。`classifyRespecting` 使用同一条 0 与非法边界。

B 直接接入的签名和语义在 `docs/开发记录/配置平衡-20261101/本批命中裁决接口.md`。`classifyHit`、`classifyRoot`、`classifyConfirmed` 的重载分别对应旧的 `hurt`、`hurtRoot`、`hurtConfirmed`。另有一条带 `ConfirmedDamage` 的 `classifyConfirmed`，与 `confirmTracked` 共用跟踪；`accepted()` 仍只表示正伤被接受。

## 问题 4：原始 0 的标准耗魔

`quotedMana` 只在「定义上另有本体费用」时跳过读取。条件是基础伤害不是正数，并且 `domain()`、`barrier()` 或 `guard()` 有定义。不按术式编号分支。领域开域费仍由 `configuredBodyUnit` 读 `manaCost` 的开域单价；水幕、水环仍读 `spawnManaCost`。宿主占位保持 0，水系生成费不会再进宿主预扣。

其余原始单价为 0 的规则、动作、索敌、剑来等，走 `BalanceDisplay.mana` 的标准 `manaCost`。键未声明时仍是 0。配成正数后，金额变的是原来那一次报价、预留和退款，没有新扣费点。

## 修改位置

- `TechniqueHits.java`：布尔入口第 30–48 行；`classifyHit` 第 63–106 行；`classifyRoot` 第 109–117 行；`classifyRespecting` 第 123–133 行；`classifyConfirmed` 第 163–224 行；恰好 0 第 230–235 行；`confirmTracked` 第 266–268 行。
- `ReleaseBatch.flush`：第 410–428 行。有 flow、无 flow、尊重着陆和普通分支都保存真实裁决。产种和潮压用 `grantsSeeds()`。flow 载荷也只在正伤被接受时下发。
- `TechniqueHitSettle.java`：普通根命中第 186–191 行；flow 确认第 300–306 行。尊重着陆和扫掠仍用 `classifyRespecting`，结算已经按 `grantsSeeds()` 产种。
- `HitVolumes.java`：第 186–187 行。光束用 `continues()` 记录命中和回调，没有加产种。仓库里仍没有这条光束的生产调用方。
- `SkillSegmentMath.java`：第 254–275 行。

修前字节在 `pre-repair-2/A/`。`pre-A` 和 `pre-repair-1` 没有覆盖。

## 生产调用怎么读裁决

| 调用 | 接续 | 产种或正伤副作用 |
| --- | --- | --- |
| 命中实体，尊重着陆 | `classifyRespecting` | `ResourceSettlement`、潮压用 `grantsSeeds()` |
| 命中实体，普通根命中 | `classifyRoot`；合法 0 为成立但无正伤，命中节拍看 `continues()` | 同上，不把接续写成已接受 |
| 命中实体，flow | `classifyConfirmed` | 载荷、资源和潮压只用 `grantsSeeds()` |
| 扫掠最早目标 | `classifyRespecting` | 已按 `grantsSeeds()` |
| 批量冲刷，有 flow | 尊重着陆用 `classifyRespecting`，否则 `classifyConfirmed` | 退种、潮压、载荷只用 `grantsSeeds()`；完成回调收到真实裁决 |
| 批量冲刷，无 flow | 尊重着陆用 `classifyRespecting`，否则 `classifyRoot` | 退种和潮压只用 `grantsSeeds()` |
| 光束 | `classifyRoot.continues()` | 这条路径不产种 |

`SwordEntityMotion`、`ContactEnchantState`、`DomainContactBilling` 没有改。它们要按上述接口改接续和产种。`LiaoliEffect`、`FireBurstEffect`、`MovementDashEffect`、`FieldZones`、`LifeDrain` 不读取这个布尔值来决定接续；场地和收招只在金额已经大于 0 时调用。这次没有改它们。

## 未完成

`docs/配置平衡指南.md` 第 97 行仍写「报价大于 0 的单价」。它不在这次独占范围里，没有改。代码现在是：原始 0 也会读标准 `manaCost`；领域、屏障、护环的占位 0 除外。

没有无法交给统一编译的阻塞。`compileJava` 未跑。游戏未测。这一单不是正式复核通过。
