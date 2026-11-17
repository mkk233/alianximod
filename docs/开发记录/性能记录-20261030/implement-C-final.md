我先读任务说明和相关执行规范，确认授权范围后再动手。任务是性能插桩，只改指定的八个源文件。我先读设计裁决和实体读取记录，再按契约动手。设计裁决已读完。接下来定位这八个源文件并备份原件，再按冻结接口插桩。八个文件位置唯一。我先备份并记下原哈希，同时读需要插桩的方法。备份和原哈希已经记下。接下来只在这八个文件的服务端路径上按冻结接口插桩，客户端分支保持原样。调用次数应记在原表达式执行前，结果计数仍只用那一次返回值。我按这个顺序把查询、窄判定、伤害和显式发送的计数补齐。计数放在业务调用前，记录器一旦抛错就会跳过原来的查询或伤害。我把计数改回调用返回之后，保证原表达式仍然只执行一次。源码插桩已经收束。我正在把最终报告和证据写进指定目录。C 的写入已经结束，八个源文件现已释放。设计裁决 1/2，实现正式复审 0/3，没有自行追加复审。没有编译、没有游戏实测、没有提交、没有改制作表。

报告在 `docs/开发记录/性能记录-20261030/implementation-C-final.md`，证据在同目录 `implementation-C-evidence.json`。两份文件的 SHA-256 分别是 `72d89aebc473054876fdc30636532053f4c30617074e3dce8560d9925a9f9a50` 和 `2eedbbf2515729208a68acfa9466070dcce71fd376c2fae30fdb10a2ebff6699`。

## 做了什么

只改了这八个文件，并按冻结契约引用 `com.bslx.XueXiMod.debug.performance` 的 `PerformanceRecorder.begin()` / `end(PerfMetric, long)` / `add(PerfCounter, long)` / `isRecording()`。A 的类型没有补建。计时是显式 `long` 加 `try/finally`。关闭时不包住玩法，客户端分支在进入记录器之前原样返回。同一枚举可以嵌套，耗时都是 inclusive，不能相加。

修改前的八个源文件在 `docs/开发记录/性能记录-20261030/before-C/`。备份 SHA-256 与当时源文件一致。没有调用 git，没有 reset、clean、checkout。

| 文件 | 关键插桩 |
| --- | --- |
| TechniqueHitEntity.java | 服务端 `tick` 282–415 为 `HIT_ENTITY_UPDATE`，含 `pendingActivation`。三处 `motion.tick`（310、554、563）为 `MOTION`。陷阱、普通、弧带三处查询为 `ENTITY_QUERY`。`contains` 与 `intersectsSlice` 为 `NARROW_PHASE`。`level.clip` 382 为 `BLOCK_CLIP`。弧带留在更新计时内。 |
| SwordEntity.java | `anyOwnedBy` 194–222、`ownedBy` 224–252 为 `SWORD_OWNED_SCAN`，早退只计已访问实体，排序在计时外。服务端 `tick` 353–419 为 `SWORD_UPDATE`。`returnTick` 425–460 为 `MOTION`。 |
| SwordEntityMotion.java | `hurtAlong` 查询与原窄判定表达式；主人不在时的 magic `hurt` 132 为 `DAMAGE_APPLY`。`flyStep` 198–264 为 `MOTION`，其中 `level.clip` 239 为 `BLOCK_CLIP`。 |
| TechniqueHitSettle.java | `tryHit` 38–86、`settleFlow` 170–195 为 `HIT_SETTLE`，`settle` 不再包同一枚举。引爆查询为 `ENTITY_QUERY`。`broadcastEntityEvent`、`playSound`、`sendParticles` 各计一次 `EXPLICIT_SEND`。 |
| TechniqueHitSpec.java | `spawnReported` 162–376 为 `HIT_SPAWN`。`new` 后本地计数，`addFreshEntity` 仍只在 336 调用一次。循环正常结束后一次性加入请求、构造、入世界三个计数。 |
| TechniqueHits.java | 最内层 `hurt` 47–80 与 `hurtConfirmed` 86–107 为 `DAMAGE_APPLY`。三处 `target.hurt`（57、64、98）仍各一次，返回后才计尝试和 true。 |
| PhantomSwordEffect.java | 两个前置早退在计时外。生成体 27–60 为 `PHANTOM_SWORD_SPAWN`，三种剑生成计数在循环结束后一次性加入。 |
| HitVolumes.java | 只给体积查询 110 和光束查询 185 加 `ENTITY_QUERY`。状态和视觉未改。 |

与备份对比，`target.hurt`、`addFreshEntity`、`getEntitiesOfClass`、`level.clip` 的调用次数都没有增加。每个原表达式只求值一次。命中去重、`pendingActivation`、短路、随机、无敌时间恢复和潮汐 `finish` 的顺序仍在。

## 未覆盖与阻塞

`PerformanceRecorder`、`PerfMetric`、`PerfCounter` 由 A 提供。A 未落地时，统一编译会因找不到这些类型失败。这是并行契约，不是本作者漏建。

`settle` 本体没有再包 `HIT_SETTLE`。非持续接触路径只在从 `tryHit` 进入时被外层计时盖住。`hurt` 的无效早退记入 `DAMAGE_APPLY`，但不记 `DAMAGE_ATTEMPTS`。业务调用抛出时，计时仍 `end`，依赖返回值的计数不加。更新次数和 owned 扫描计数在 finally；若那里的 `add` 再抛出，可能替换本次返回或原异常，但玩法体已经先执行。生成循环中途抛出时，该次不刷三个生成计数，已经入世界的实体仍留在世界里。

HitVolumes 的光束实体盒、视觉生成和火焰粒子没有插。真剑丢出不在这八个文件里。`EXPLICIT_SEND` 只是三处业务调用，不是包数、字节或延迟。

命名门禁脚本会编译检查器。本阶段禁止编译，该脚本被拒绝了两次。新增局部变量已按不少于 3 个字符、不含禁用缩写自检，不能说门禁已通过。

## 源文件哈希

| 文件 | 最后写入 UTC | SHA-256 |
| --- | --- | --- |
| TechniqueHitEntity.java | 2026-10-29T20:12:19Z | 098f595b9be573905eb930fe37061e09719b422cacd5cc703887576cd534d994 |
| SwordEntity.java | 2026-10-29T20:12:22Z | 3a49c611c5aac4ce2a1eaf20d29ceaba0e48f94b7b014e9df8375265a9c3113e |
| SwordEntityMotion.java | 2026-10-29T20:10:17Z | 569623b1af9a99289faa8f7d1de71a0cfd9e6551130599091643692dede3feb8 |
| TechniqueHitSettle.java | 2026-10-29T20:10:22Z | c59d191555420e0f4a807d896d6307f5667f3c1385dbd2aae3fb481c8d34374e |
| TechniqueHitSpec.java | 2026-10-29T20:11:16Z | 4d70193a86de7034b65c996679682189a834d498053bab82bc38cef6869ae4b9 |
| TechniqueHits.java | 2026-10-29T20:10:25Z | 6c594e3df8bb09e53cc79031afa2387314f40a89ac64d35f4bb394d7e9efbc9e |
| PhantomSwordEffect.java | 2026-10-29T20:11:15Z | 0127ef6e59840c75162afb4a69f75d4c57966703bc321d998e8d6ef50154bdfb |
| HitVolumes.java | 2026-10-29T20:10:28Z | 18bc0232bcc0fc2c7e2da2900ff3cb4b7a13f97dd54577b675f87b8ce2f2b71c |

没有运行 `gradlew`，游戏效果未实测。待三个作者收束后，再由指定的 Cursor 对最终源码做统一编译。

本作者（C）对 TechniqueHitEntity.java、SwordEntity.java、SwordEntityMotion.java、TechniqueHitSettle.java、TechniqueHitSpec.java、TechniqueHits.java、PhantomSwordEffect.java、HitVolumes.java 的全部写入已经结束，现释放这八个文件。
