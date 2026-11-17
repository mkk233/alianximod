先读最终集成续作的授权范围，再按它接线并交最终报告。C 的载体清单已经收束。接下来只在 A 的公共入口接上总线、命中缝、火球留焰、喷涌改写和幽冥提交，不改 C/D 的实现。最终接线已停写。这是继续实现，不是正式复审。设计额度 2/2 已用完，实现正式复审仍是 0/3。没有运行 Gradle、javac、打包、游戏或独立测试。编译状态：待统一编译。

D 的灼地桥尚未交回。本轮没有读它的中间源码。该结果不挡住本单。

`basic_staff` 仍是 `StaffData(6, 0.20, 0.10, 100, null)`。`test_staff` 仍是 `StaffData(32, 15.0, 0.50, 1000, null)`。

命名门禁退出码 1。本轮文件没有新违规。未改的 `OriginalHumanoid.java:165` 的 `i` 和 `:202` 的 `to` 仍在清单里。

## 真实注册和调用

- `XueXiMod.registerContent` 在残骸登记之后调用一次 `BatchCarrierTypes.register`。残骸类型、地形开关和陨岩物品不重复登记。
- `commonSetup` 在 `ModTechniques.registerAll()` 之后调用一次 `BatchCarrierBootstrap.install()`。它登记火苗、热波、陨岩、地表、喷涌、错时、接续、留焰、种火、引燃、引爆、崩地，不登记幽冥。
- 客户端渲染事件把 `BatchCarrierTypes.VISUAL` 交给 `BatchCarrierVisualRenderer`。`EntityRendererFix` 按 `batch_carrier_visual` 重绑。语言键为「载体显形 / Carrier Visual」。该实体不进创造标签。
- `TechniqueHitSettle.sweepEarliest` 在分类前调用 `BatchCarrierHooks.intercept`。载体已接住的接触不再走 A 的直接分类、资源提交和湿压。实体接触由 C 扩圈；A 只在裁决可接续、且不是取消或未决时调用一次 `batchFollow`。地形按 C 的 `continueChildren` 决定是否接续，然后结束。寿命走超时拦截，不当成落地。方块截断走地形拦截。
- `FireOrbEffect` 只增加 `publishedHook`，在出生槽和命中钟还在时调用 `BatchFlameTrail.attachLoose`。产种点仍是 3，燃烧仍是 4 秒，不新增爆炸。
- `GloomDomainEffect` 只把施法者已经打开的领域交给 `NetherDomainCycle.attach`。没有活领域就不生成攻击体，也不新开领域。
- `WeaponEnchant.commitResource` 不再 `adopt`。栈上的施法上下文只交给 `ResourceHitCommit`。失败仍保留印记，不二次扣魔力。

## 十三项

1. 留焰：沿载体留下不产种的火焰轨迹。贴图 `textures/effect/flame_trail_v1/flame_trail.png`，只挂火苗、火球、陨岩。
2. 陨岩：默认抛物线，中心冲击后留下岩体。飞行和残骸用 `textures/block/meteorite_v1.png`，不用火焰面片。
3. 热波：环体判定一次。贴图 `textures/effect/shock_ring_v1/shock_ring.png`。
4. 地表定位：整组放到第一个可见承托面，失败不扣费。贴图 `textures/effect/surface_anchor_v1/surface_anchor.png`。
5. 喷涌：可变宿主改为竖直或 25 度锥。初速 1，重力 0.08，运动不超过 100 tick，水平 32 格，仍受宿主寿命限制。同分支再遇垂落、追踪、抛物线或涡旋则拒绝。没有新贴图。
6. 错时：同一组按间隔错开出生。没有新贴图。
7. 命中接续：父结果完成后在冻结点发射一次子分支。没有新贴图。
8. 种火：命中被接受后把基础 10 并进本体基数。贴图 `textures/effect/seed_sow_v1/seed_sow.png`。
9. 引燃：按当时余额挂引燃，不附加直接伤害。贴图 `textures/effect/kindle_v1/kindle.png`。
10. 引爆：按当时余额引爆一次，不附加直接伤害。贴图 `textures/effect/detonate_v1/detonate.png`。
11. 幽冥：挂到已经打开的领域上产种，不新开领域，也不生成攻击体。表现条是 D 的资源，不在 A 这边。
12. 崩地：在成立的落点挖浅碗，不附加直接伤害。贴图 `textures/effect/ground_dust_v1/ground_dust.png`。武器路径不持有崩地租约。
13. 火苗：直线火命中体，本体基数 2。贴图 `textures/effect/fire_spark_v1/fire_spark.png`。

色调由 C 在绘制时查询。A 不把颜色写进渲染器。

## 剩余请求

- B：无。
- C：`BatchFlightBook` 的实体拦截在第一下被取消时不要扩圈，也不要记成已处理。A 没有改这个文件。
- D：仍要 `ContactEnchantState.open(ContactCarrier, EnchantmentSnapshot, ResourceEnchantLoad, UUID)`，并由 `SurfaceFireHosts.open` 传入同一载荷和根，不新建施法上下文。灼地桥尚未交回。

## 停写后的交付哈希

- `author-a-ownership.json`：`a999c98faec951fc1b0c92653e3288d54389903fda12b043c857fc5159fe1f55`，2026-10-31 05:02:59，62 个文件，阶段 `final-integration`
- `author-a-interface-final.md`：`dbebb01e7dfdd912f65f57b01e5ecd42ff3afced0dc738491ccd81365b72da9d`，2026-10-31 05:03:14
- `author-a-final-integration-report.md`：`bbe74a62e1582132c7dea15dcdd0e55b7bf7267c8e890095acdd39e54a56ac02`，2026-10-31 05:03:28
- `author-a-final-integration-report.json`：`a12870c8a216f8c214798191741108af9e34340e95762e746ddb9f3d454a10a0`，2026-10-31 05:03:30
