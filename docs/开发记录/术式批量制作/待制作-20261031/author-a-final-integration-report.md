# 作者 A 最终接线报告

本轮是继续实现，不是正式复审。设计额度 2/2 已用完，实现正式复审仍是 0/3。没有运行 Gradle、javac、打包、游戏或独立测试。编译状态：待统一编译。

D 正在独立写灼地桥。本轮没有读它的中间源码或流式日志。那个结果还没到，不挡住本单。

`basic_staff` 仍是 `StaffData(6, 0.20, 0.10, 100, null)`。`test_staff` 仍是 `StaffData(32, 15.0, 0.50, 1000, null)`。本轮没改这两份数据。

命名门禁退出码 1。本轮改过的文件没有新违规。未改的 `OriginalHumanoid.java:165` 的 `i` 和 `:202` 的 `to` 仍在清单里。

## 真实注册和调用

- `XueXiMod.registerContent`：残骸类型之后调用一次 `BatchCarrierTypes.register`。地形开关和残骸不重复登记。
- `XueXiMod.commonSetup`：`ModTechniques.registerAll()` 之后调用一次 `BatchCarrierBootstrap.install()`。
- 客户端渲染事件：`BatchCarrierTypes.VISUAL` → `BatchCarrierVisualRenderer`。`EntityRendererFix` 重绑 `batch_carrier_visual`。
- 语言：`entity.xueximod.batch_carrier_visual` 为「载体显形 / Carrier Visual」。陨岩方块和残骸语言保持原样。
- `TechniqueHitSettle.sweepEarliest` → `BatchCarrierHooks.intercept`，再决定是否 `batchFollow` 和 `onExpire`。寿命走 `expireLifetime`，方块截断走 `expireTerrain`。
- `FireOrbEffect.publishedHook` → `BatchFlameTrail.attachLoose`。
- `GloomDomainEffect` 由 `ModTechniques.registerAll` 登记，只 `NetherDomainCycle.attach` 已经打开的领域。
- `WeaponEnchant.commitResource` 不再 `adopt`。

## 十三项

玩家可见句子不写根标识。

1. 留焰：通用火附魔，沿载体留下不产种的火焰轨迹。资源是 `textures/effect/flame_trail_v1/flame_trail.png`，只挂火苗、火球、陨岩。
2. 陨岩：法术火命中体，默认抛物线，中心冲击后留下岩体。飞行和残骸用陨岩方块模型 `textures/block/meteorite_v1.png`，不用火焰面片。
3. 热波：法术火环体，判定一次。资源是 `textures/effect/shock_ring_v1/shock_ring.png`。
4. 地表定位：把整组出生放到第一个可见承托面上，失败不扣费。资源是 `textures/effect/surface_anchor_v1/surface_anchor.png`。
5. 喷涌：把可变宿主改成竖直或 25 度锥的喷涌运动。没有新贴图。
6. 错时：同一组按间隔错开出生。没有新贴图。
7. 命中接续：父结果完成后在冻结点发射一次子分支。没有新贴图。
8. 种火：命中被接受后把基础 10 并进本体基数。资源是 `textures/effect/seed_sow_v1/seed_sow.png`。
9. 引燃：按当时余额挂引燃，不附加直接伤害。资源是 `textures/effect/kindle_v1/kindle.png`。
10. 引爆：按当时余额引爆一次，不附加直接伤害。资源是 `textures/effect/detonate_v1/detonate.png`。
11. 幽冥：挂到已经打开的领域上产种，不新开领域，也不生成攻击体。表现贴图是 D 的幽冥条，不在 A 的资源里。
12. 崩地：在成立的落点挖浅碗，不附加直接伤害。资源是 `textures/effect/ground_dust_v1/ground_dust.png`。
13. 火苗：直线火命中体，本体基数 2，命中或撞块消失。资源是 `textures/effect/fire_spark_v1/fire_spark.png`。

色调由 C 在绘制时查询。A 不把颜色写进渲染器。

## 剩余请求

- B：无。
- C：`BatchFlightBook` 的实体拦截在第一下被取消时不要扩圈，也不要记成已处理，这样取消不消耗第一次机会。A 没有改这个文件。
- D：仍要 `ContactEnchantState.open(ContactCarrier, EnchantmentSnapshot, ResourceEnchantLoad, UUID)`，并由 `SurfaceFireHosts.open` 传入同一载荷和根，不新建施法上下文。灼地桥尚未交回。

## 本轮源码

| 文件 | sha256 | mtime |
| --- | --- | --- |
| XueXiMod.java | dfeba3224d5e31ab7da9b5de27a6fb748c2963c1311015daf5d2ae9e17bca511 | 2026-10-31 04:58:47 |
| EntityRendererFix.java | a1d6fda0e52b3129ceb7a78a4a30b31eb63abee2567ed01a671a2fe649a4a61c | 2026-10-31 04:58:50 |
| zh_cn.json | a8b03aea26804a8863c780f4e72c537713776a5510e7960371bf8e8e4d768d1e | 2026-10-31 04:58:51 |
| en_us.json | fba0df0fad97dc26bd154713b853dc39e6059176792a897cbafcf4c055328815 | 2026-10-31 04:58:51 |
| TechniqueHitSettle.java | dcda2da953795405a667f9fdc471b03852453457a09dd575635b36add998a5a1 | 2026-10-31 04:59:01 |
| TechniqueHitEntity.java | 261e52baaae3184ab61cc552a8d2211b26ce5da967147024b14b9a66f790c1d3 | 2026-10-31 05:01:18 |
| TechniqueHitSpec.java | 312488763d4d99ff4c0fc80a4691bf529309a16784065a1ba554fa2d6d4dd628 | 2026-10-31 04:59:24 |
| BirthLayout.java | 2abad9fc59238a542a8469e9f16cbf38543a7db540718a7e57e6779325b8cde0 | 2026-10-31 04:59:26 |
| BatchReleasePlan.java | b41c3947e68e75e5f84f49a4fcf2c5c3e67ac986f799997dbb3fa33eaa427f14 | 2026-10-31 04:59:35 |
| BatchCastSession.java | 30057ffdf005c6fe679672d41c9b8d2e9cc74c8c020893ac36328e10bf9b6625 | 2026-10-31 04:59:40 |
| BatchReleaseCompiler.java | d4a95913742a2ada5fddf76aaedc80cff525974ae11f051a56e1454ca1789f71 | 2026-10-31 04:59:42 |
| FireOrbEffect.java | 6c9ec0bdc9db59c7890d010c8c94ad47315963dba47b85f88b8c916e619fb8c4 | 2026-10-31 05:01:43 |
| WeaponEnchant.java | 7ce69faa3504503de74bc8dcd47db019749b6cc587de05e48a237724475dde9f | 2026-10-31 05:01:46 |
| GloomDomainEffect.java | 63ac793213eb2cfb49d3b759f8e1bb5d66afbca4761cbceaf54157507c26db9e | 2026-10-31 05:01:49 |
| ModTechniques.java | 62a4b5f8855871592733d06b2d4043d62f0d188c72c08a50f3c3a08998d8ea03 | 2026-10-31 05:01:50 |

所有权 62 个文件，阶段 `final-integration`。源码在本报告写下后停写。
