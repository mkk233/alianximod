# C 实现最终报告：实体、生成、碰撞与伤害性能插桩

作者：C，Cursor CLI `--model grok-4.7-high-fast`。本阶段是实现，设计裁决 1/2 已完成，实现正式复审 0/3。没有自行追加复审、独立测试、编译、打包、启动游戏、提交或改制作表。

本作者对下面八个源文件的全部写入已经结束，现释放这八个文件。A 的 `debug/performance` 公共接口和 B 的释放链（含 `ReleaseBatch`）没有修改，也没有读取他们未完成的工作。公共接口按冻结契约直接引用，没有补建。

## 备份

八个源文件在修改前复制到 `docs/开发记录/性能记录-20261030/before-C/`。备份字节与当时源文件 SHA-256 一致，之后没有再写备份。未调用 git，因此没有 `safe.directory` 命令；没有 reset、clean、checkout。

| 文件 | 备份 SHA-256 | 备份字节 |
| --- | --- | --- |
| TechniqueHitEntity.java | d04258755b7f2137efd18fc314c6c5ddc1354872b6a00b481172a0f7db68ca23 | 36773 |
| SwordEntity.java | 59a69caa5879eb6290bffc9b734c234203abdc17e8d6a63560c4f9d43d4b3e90 | 19796 |
| SwordEntityMotion.java | 052242cef481905ef05d73ae8daae68fe9d4312af86702db9bf2d4d25b7e1d8a | 13561 |
| TechniqueHitSettle.java | eb9d28579813ed27a7ef9db08a01d27c00a7f2f2d6fe3377feb8d7382225d0a2 | 10216 |
| TechniqueHitSpec.java | f3e6d4c4233f5b7624fd85cb9bf1f1de58d0790da3d6102c09d7070a9cdd9868 | 21777 |
| TechniqueHits.java | 10dd680b45c91e83150465b785634c4df04ce6fc7e1f5ec3ee0d60b82ce80f9a | 13279 |
| PhantomSwordEffect.java | aa5198ef79b05adbbf4640d62a65fbf5fd305eab769b474443bbb46a29e4ba08 | 2531 |
| HitVolumes.java | d5441b07096cee46cfc85b3cb59895a06ad9a61affb79e9f05c933a7adcefcee | 14918 |

## 修改路径与关键行号

八个文件在 `src` 下各只有一处。计时一律是显式 `long` 加 `try/finally`，调用 `PerformanceRecorder.begin()` / `end(PerfMetric, long)`。计数用 `add(PerfCounter, long)`，并由 `isRecording()` 或进入方法时取样的布尔值把门。关闭时不读业务结果的 `size()`，不额外调用原表达式。客户端分支在进入记录器之前返回。

同一枚举可以嵌套或重入，耗时都是 inclusive，不能相加当成总 CPU。

### TechniqueHitEntity.java

路径：`src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitEntity.java`

- `tick` 282–415。客户端 284–290 原样返回。服务端 292 取样，293–414 包住整个服务端体，含 `pendingActivation` 早退。`HIT_ENTITY_UPDATE` 在 410 结束；`HIT_ENTITY_UPDATES` 在业务体之后的 finally 412 加一，早退也会走到。
- `motion.tick` 三处各包 `MOTION`：tick 内 310–314；`applySpec` 的 `STILL_AIM` 554–558；`VORTEX`/`FUNCTION` 563–567。只包实际发生的那一次调用。
- 陷阱查询 332–342、普通查询 350–360、弧带查询 938–949：只包 `getEntitiesOfClass`，循环在计时外。`ENTITY_QUERY_CALLS` 与 `ENTITY_CANDIDATES` 在查询返回后各加一次。
- 普通 `contains` 363–373、弧带 `intersectsSlice` 955–965：`NARROW_PHASE`，测试数与通过数在同一次布尔结果上计数。
- 世界 `level.clip` 382–389：`BLOCK_CLIP`。
- 弧带更新留在 `HIT_ENTITY_UPDATE` 内，没有另记 `MOTION`。

### SwordEntity.java

路径：`src/main/java/com/bslx/XueXiMod/shu/technique/sword/SwordEntity.java`

- `anyOwnedBy` 194–222、`ownedBy` 224–252：`SWORD_OWNED_SCAN`。空 level/owner 在扫描前返回，不计扫描。`SWORD_OWNED_SCAN_CALLS` 与已访问个数在扫描 finally 里按本地计数一次性加入。`anyOwnedBy` 命中后立即返回，只计已访问实体。`ownedBy` 的排序在 250，位于扫描计时之外，仍只执行一次。
- `tick` 353–419。客户端 355–357 原样返回。服务端 360–418 为 `SWORD_UPDATE`，`SWORD_UPDATES` 在 finally 416。悬停、飞行、召回、风暴和插地都在这个体内。
- `returnTick` 425–460：整个方法体为 `MOTION`，含早退。`fly` 421 仍只调用 `flyStep`，飞行计时在 `SwordEntityMotion.flyStep`。

### SwordEntityMotion.java

路径：`src/main/java/com/bslx/XueXiMod/shu/technique/sword/SwordEntityMotion.java`

- `hurtAlong` 82–160。查询 85–94 为 `ENTITY_QUERY`。去重 `hit.contains` / `size >= 5` 仍在窄判定之前短路，不计为窄判定。窄判定 100–111 保持原表达式和 `&&` 短路：线段 `clip` 非空时不求值终点盒。未命中才 `continue`。通过数是表达式为假的次数。
- 主人在场仍只调用一次 `TechniqueHits.hurtRoot` 120，不在这里再包 `DAMAGE_APPLY`。主人不在时的 magic `target.hurt` 130–139 包 `DAMAGE_APPLY`，返回后才计 `DAMAGE_ATTEMPTS` / `DAMAGE_ACCEPTED`。原 `TideMarkDamageScope.finish` 仍在外层 finally。
- `flyStep` 198–264 整个方法为 `MOTION`。世界 `level.clip` 239–244 为 `BLOCK_CLIP`。实体盒 `clip` 不进入 `BLOCK_CLIP`。

### TechniqueHitSettle.java

路径：`src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitSettle.java`

- `tryHit` 38–86、`settleFlow` 170–195：`HIT_SETTLE`。`settle` 88 没有再包同一枚举。`tryHit` 调用 `settle`，持续接触再进入 `settleFlow` 时两层 inclusive 重入。
- 引爆查询 219–228：`ENTITY_QUERY`。
- 延迟反馈 lambda 内的 `broadcastEntityEvent` 125–132、`playSound` 134–143，以及 `detonate` 的 `sendParticles` 241–248：每个原调用各一次 `EXPLICIT_SEND` 和 `EXPLICIT_SEND_CALLS`。lambda 仍是原来的 `ChainScheduler.schedule`，计时发生在它实际执行时。目标已死亡则两个调用都不发生。

### TechniqueHitSpec.java

路径：`src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitSpec.java`

- `spawnReported` 162–376 整个方法为 `HIT_SPAWN`，含准备、循环、早退 `continue` 和返回。
- 计划数、构造成功数、入世界成功数用本地整数累计。`new` 成功后 316 `constructed++`。`addFreshEntity` 仍只在 336 调用一次，返回值仍只决定原来的 `accepted`。循环正常结束后 359–371 各最多 `add` 一次：`HIT_SPAWN_REQUESTED` 为 `entityCount`，`HIT_SPAWN_CONSTRUCTED` 为成功 `new` 的次数，`HIT_SPAWN_ACCEPTED` 为 `addFreshEntity` 返回 true 的次数。零或负数不加，避免向 `add` 传入负增量；零与契约里的零增量无作用一致。

### TechniqueHits.java

路径：`src/main/java/com/bslx/XueXiMod/shu/technique/release/TechniqueHits.java`

- 最内层 `hurt` 47–80 与 `hurtConfirmed` 86–107 为 `DAMAGE_APPLY`。两个 `hurtRoot` 重载和 `confirmTracked` 没有再包。
- `target.hurt` 仍各只调用一次：57、64、98。无敌时间的内层 finally 顺序不变。`TideMarkDamageScope.finish` 仍在原来的 finally。`noteDamageOutcome` 109–116 只在 `hurt` 返回后根据那一次布尔值计 `DAMAGE_ATTEMPTS` 和 `DAMAGE_ACCEPTED`。`hurtConfirmed` 的方法返回值仍是 `precise`，计数看的是 `target.hurt` 的布尔值。

### PhantomSwordEffect.java

路径：`src/main/java/com/bslx/XueXiMod/shu/technique/effects/PhantomSwordEffect.java`

- 武器不接受或不是 `ServerLevel` 的两个早退留在计时外。其后的生成体 27–60 为 `PHANTOM_SWORD_SPAWN`。
- `behind` 仍按原三元表达式每轮最多调用一次，随机次数不变。`SwordEntity.phantom` 返回后本地 `constructed++`，`addFreshEntity` 仍调用一次并保存返回值。循环正常结束后一次性加入 `SWORD_SPAWN_REQUESTED` / `SWORD_SPAWN_CONSTRUCTED` / `SWORD_SPAWN_ACCEPTED`。

### HitVolumes.java

路径：`src/main/java/com/bslx/XueXiMod/shu/technique/release/HitVolumes.java`

- 体积查询 110–121、光束查询 185–195：只包已有的 `getEntitiesOfClass`，计 `ENTITY_QUERY`、`ENTITY_QUERY_CALLS`、`ENTITY_CANDIDATES`。
- 没有改状态维护、视觉、`syncVisual`、视觉体 `addFreshEntity`，也没有给光束里的实体盒判断加窄判定。

## 指标与计数

本作者写入的对应关系如下。A 负责枚举定义；这里只按冻结名字引用。

| 枚举 | 位置 |
| --- | --- |
| HIT_ENTITY_UPDATE / HIT_ENTITY_UPDATES | 命中体服务端 tick，含等待激活 |
| SWORD_UPDATE / SWORD_UPDATES | 剑服务端 tick |
| MOTION | 命中体三处 `motion.tick`、`flyStep`、`returnTick` |
| ENTITY_QUERY / ENTITY_QUERY_CALLS / ENTITY_CANDIDATES | 七处 `getEntitiesOfClass`：陷阱、普通命中、弧带、引爆、剑 `hurtAlong`、HitVolumes 体积与光束 |
| SWORD_OWNED_SCAN / SWORD_OWNED_SCAN_CALLS / SWORD_OWNED_SCANNED | `anyOwnedBy`、`ownedBy` |
| NARROW_PHASE / NARROW_PHASE_TESTS / NARROW_PHASE_PASSED | `contains`、`intersectsSlice`、剑窄判定表达式 |
| BLOCK_CLIP | 命中体与 `flyStep` 的 `level.clip` |
| HIT_SPAWN / HIT_SPAWN_REQUESTED / HIT_SPAWN_CONSTRUCTED / HIT_SPAWN_ACCEPTED | `spawnReported` |
| PHANTOM_SWORD_SPAWN / SWORD_SPAWN_* | `PhantomSwordEffect` 生成体 |
| HIT_SETTLE | `tryHit`、`settleFlow` |
| DAMAGE_APPLY / DAMAGE_ATTEMPTS / DAMAGE_ACCEPTED | 最内层 `hurt`、`hurtConfirmed`、剑的直接 magic `hurt` |
| EXPLICIT_SEND / EXPLICIT_SEND_CALLS | `broadcastEntityEvent`、`playSound`、`sendParticles` 三个原调用 |

`EXPLICIT_SEND_CALLS` 只计这三次业务调用，不是包数、字节或延迟。`entityData.set` 和 `setPos` 没有当成发送。

## 保留的原行为

- 关闭采样时，`begin`/`end` 按契约空转；计数被 `isRecording` 挡住。玩法表达式不包在开关外面，关闭不会跳过玩法。
- 与备份对比，八个文件的 `target.hurt`、`addFreshEntity`、`getEntitiesOfClass`、`level.clip` 调用次数都没有增加。花括号数量两边各自平衡，`begin` 与 `end` 数量一致。
- 每个原查询、窄判定、`clip`、`hurt`、`addFreshEntity` 和三处发送都只求值一次。结果先放进局部变量再决定后续玩法。
- 命中去重、`pendingActivation`、弧带失败 `continue`、`applySpec` 异常 `continue`、`addFreshEntity` 返回 false、剑命中集合上限 5、无敌时间恢复和潮汐 `finish` 的原顺序都还在。
- 生成三计数在循环结束后一次性加入，循环内不调用 `add`，因此记录器计数不会插在 `new` 和 `addFreshEntity` 之间把入世界调用跳掉。
- 更新次数和 owned 扫描次数放在业务体之后的 finally，业务体先执行。`anyOwnedBy` 仍在首次命中后停止，不预先扫完。

## 未覆盖、边界与阻塞

- A 的 `PerformanceRecorder`、`PerfMetric`、`PerfCounter` 尚未由本作者创建。八个文件已经引用 `com.bslx.XueXiMod.debug.performance`。A 未落地时统一编译会因找不到这些类型失败。这是契约并行，不是实现缺口。
- `settle` 本体和引爆里非持续接触的 `settle` 没有另包 `HIT_SETTLE`。只有从 `tryHit` 进来时，外层 `tryHit` 的计时盖住非持续接触路径；`settleFlow` 自己始终有一层。
- `hurt` 的金额、目标或玩家无效早退记入 `DAMAGE_APPLY` 次数与耗时，但不记 `DAMAGE_ATTEMPTS`，因为没有 `target.hurt`。
- 业务调用若抛出，计时 finally 仍 `end`，依赖返回值的计数不加。更新次数和 owned 扫描计数在 finally 里；若那里的 `add` 再抛出，可能替换本次返回值或原来的异常。玩法体在该 `add` 之前已经执行。
- `spawnReported` 或幻剑循环中途抛出时，该次调用不刷三个生成计数；已经 `addFreshEntity` 的实体仍留在世界里，生成计时仍 `end`。
- HitVolumes 的光束实体盒判断、视觉生成和火焰粒子不在本次查询插桩之外。真剑丢出生成不在这八个文件里。`SwordVortex` 未改；它调用的 `ownedBy` 由 `SwordEntity.ownedBy` 计入。
- 网络只覆盖 `TechniqueHitSettle` 的三处业务调用。`ModNetwork`、`FireVisualRelay`、追踪包、字节和延迟都没有计。
- 命名门禁脚本 `tools/check-name-gate.ps1` 会编译检查器。本阶段禁止编译，自动审查两次拒绝执行该脚本。新增局部变量已按不少于 3 个字符、拆词后不命中禁用缩写自检。没有改未触及文件里的旧名字，也不能把本次说成门禁已通过。
- 没有超出设计、需要换模型的架构阻塞。没有降模型。

## 编译与实测

无编译授权。没有运行 `gradlew`，没有独立测试，没有改配置，没有打包，没有启动游戏。游戏效果未实测。

待三个作者写入收束后，由主会话指定的 Cursor 对最终源码执行 `.\gradlew.bat compileJava --offline --no-daemon`。本报告不表示编译已通过。

## 写入结束时的源文件

UTC 为文件系统 `LastWriteTimeUtc`。SHA-256 为文件字节。

| 文件 | 最后写入 UTC | SHA-256 | 字节 |
| --- | --- | --- | --- |
| TechniqueHitEntity.java | 2026-10-29T20:12:19Z | 098f595b9be573905eb930fe37061e09719b422cacd5cc703887576cd534d994 | 40698 |
| SwordEntity.java | 2026-10-29T20:12:22Z | 3a49c611c5aac4ce2a1eaf20d29ceaba0e48f94b7b014e9df8375265a9c3113e | 21793 |
| SwordEntityMotion.java | 2026-10-29T20:10:17Z | 569623b1af9a99289faa8f7d1de71a0cfd9e6551130599091643692dede3feb8 | 15712 |
| TechniqueHitSettle.java | 2026-10-29T20:10:22Z | c59d191555420e0f4a807d896d6307f5667f3c1385dbd2aae3fb481c8d34374e | 12350 |
| TechniqueHitSpec.java | 2026-10-29T20:11:16Z | 4d70193a86de7034b65c996679682189a834d498053bab82bc38cef6869ae4b9 | 22685 |
| TechniqueHits.java | 2026-10-29T20:10:25Z | 6c594e3df8bb09e53cc79031afa2387314f40a89ac64d35f4bb394d7e9efbc9e | 14434 |
| PhantomSwordEffect.java | 2026-10-29T20:11:15Z | 0127ef6e59840c75162afb4a69f75d4c57966703bc321d998e8d6ef50154bdfb | 3572 |
| HitVolumes.java | 2026-10-29T20:10:28Z | 18bc0232bcc0fc2c7e2da2900ff3cb4b7a13f97dd54577b675f87b8ce2f2b71c | 16089 |

本文件与 `implementation-C-evidence.json` 在上述八个源文件冻结后写入。它们的哈希不回写进自身，避免自引用变化。

本作者（C）对这八个源文件的全部写入已经结束，现释放 TechniqueHitEntity.java、SwordEntity.java、SwordEntityMotion.java、TechniqueHitSettle.java、TechniqueHitSpec.java、TechniqueHits.java、PhantomSwordEffect.java、HitVolumes.java。
