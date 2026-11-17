# C 第二段最终报告

作者 C，模型 grok-4.7-high-fast。设计额度 2/2，实现正式复审 0/3。本段自查不是正式复审。未跑 Gradle、javac、打包和游戏。命名门禁退出码 1，只有未改动的 `OriginalHumanoid.java` 第 165 行 `i` 和第 202 行 `to`。本段新增和修改的文件没有新的命名失败。写到本报告后停写。

幽冥和领域已移交 D。C 没有写 `DomainService`、`DomainInstance`、`ContactEnchantState`，也没有读 D 的中间文件，没有给幽冥登记效果或色调。

## 这一段做了什么

火苗、热波、陨岩会在 `BatchCarrierBootstrap.install()` 之后成为可提交的效果。火苗 `seedPoints` 为 0，本体产种基数 2 仍由释放登记进 `bodyScaledBase`，只走 A 的一次 `ResourceHitCommit`。命中或撞块后消失，不爆炸。20 刻点燃写在 `onResolved`，因为最早扫掠不调用 `onHit`。

热波按内半径 1.5、外半径 5、总高 3 做一次中空判定，打完把击退前的速度写回去。父段的排除名单和遮挡快照原样使用。没有合法目标时不接续子段。宿主组要求发布命中体，所以另放一颗 12 刻、命中年龄极大的空实体，它不参与判定。

陨岩用释放定义上的抛物线，不另写重力。飞行体积是 1 格。第一次实体接触由 A 的扫掠打到直接目标并提交资源；C 再给半径 1.5、总高 3 里其余目标补一次，跳过已经打过的那个。撞块时没有这次扫掠伤害，C 对整个圆柱打一次，再崩地、再安放。安放成功就不再留残骸。安放失败，或空中命中、或超时，才生成第一段的无伤残骸。超时不打中心、不接续、不叫做撞地。崩地开关仍只挡破坏，不挡安放。

留焰只挂在本段生成的火苗和陨岩上，且载荷里有留焰、命中钟还在。碰撞按直径 0.5 沿整段扫，表现点间距 0.25、同时存在不超过 256。不产种、不引爆、不崩地、不放原版火。陨岩留焰打的是本体物理伤害，不是每段再来一次中心爆炸。跟踪实体比宿主多留 40 刻，期间持有根上下文，结束才释放。

地表锚点、种火、引燃、引爆、土尘都按 16 帧切 V。喷涌、错时、接续没有新贴图，只注册通过效果，避免效果表是空的时整段被拒绝。它们的弹道和延迟仍是 A 的批次。

八张竖条已复制到建议路径，SHA256 与清单一致。陨岩六面仍是 32×32 的 `meteorite_v1.png`，没有把 64×1024 外观条带贴上去。`nether_gloom` 的运行时文件如果已经在资源目录里，不是本段复制的，渲染也不引用它。

## 还没完成，不能写成完成

1. `BatchCarrierBootstrap.install()`、两个实体类型、两个渲染器、地形配置和语言都还没挂到共享入口。没调用 `install()` 时，这些术式仍然因为没有效果而被拒绝。视觉类型没登记时，留焰跟踪和条带不会生成。残骸类型没登记时，`spawnAfterSettlement` 返回 false，父实体随后仍会被 A 丢掉，这块岩没有待重试对象。区块没加载时同样不生成、不掉落、不强载。
2. 空中陨岩命中仍会由 `TechniqueHitSettle.sweepEarliest` 接续子段。要停掉它，必须按 `author-c-interface-v2.md` 在 `classifyRespecting` 之前调用 `intercept`，并且在 `handled()` 时不要再结算、不要再 `batchFollow`。这条还没改 A 的文件。
3. `fire_orb` 的留焰没有接。`FireOrbEffect` 不归 C。
4. 普通武器只收养根上下文，不会走 `publish`，因此武器上的留焰、崩地和陨岩残骸没有落地。
5. 喷涌运动仍看宿主定义自己的轨迹。规则覆盖不会自动换成 `UpwellMotion`，那是 A 的 `applySpec`。
6. 幽冥效果、色调和领域由 D 做。含幽冥的槽在 D 登记效果之前，执行到该槽会被拒绝。
7. 远程客户端要等 A 登记渲染器才看得到条带。跟踪距离 96，更新间隔 1。

## 资源

| 运行时路径 | SHA256 |
| --- | --- |
| textures/block/meteorite_v1.png | E8CA06D343A6E6A60082C542869FDA9BE4B9DE040904EEF59BAF52D28CBC1D3A |
| textures/effect/fire_spark_v1/fire_spark.png | 07407B8570B6EEAF9EFC764BD1365F1589A9C5427EDE4A8B1B9748D03E7DF58E |
| textures/effect/flame_trail_v1/flame_trail.png | F1B0D723B4AEA57B37FCA4928AFAF492E8F1020D864183F36EDABF379BE95284 |
| textures/effect/shock_ring_v1/shock_ring.png | 3CF9F5728C0B70EEE2CC09E61B4F6182E6254516CC7B955939907755C654BBC4 |
| textures/effect/surface_anchor_v1/surface_anchor.png | 7801A4513A63D26DAA8567F06A33834E46FD9F0AD71F5CDA3CE7F471AC39D018 |
| textures/effect/seed_sow_v1/seed_sow.png | 38CFAAE9D154AD09262680CF2053E68B2BA3D9BAF9C4FEF1769D78252D750458 |
| textures/effect/kindle_v1/kindle.png | 2490F9E2D5A8914D9A1B7080E40DD4FB0FB9188C8AA64951A88F34389F853E73 |
| textures/effect/detonate_v1/detonate.png | F3F0FCEB4A8AAECBA5EAC2B300C1FD5036B0E9EEAF1CD04B1A4F38FC1C79D858 |
| textures/effect/ground_dust_v1/ground_dust.png | 0C7479F9F3DC02E3216D7EBF6A195C98730CF9841A57693C210658821F0A476A |

条带都是 64×1024、16 帧。渲染用 `frame / 16` 到 `(frame + 1) / 16` 的 V，不把整张拉成一个面。

## 文件哈希

时间是本机写入时间。完整表也在 `author-c-report.json`。

| SHA256 | 时间 | 文件 |
| --- | --- | --- |
| 3120F848503E6BABD040D9EC8E871F253AC3B62FFE9E984D5285C5E42B3D510A | 2026-10-31 04:36:17 | carrier/BatchCarrierIds.java |
| 1E4301428AAE6BF58AF1264889A9D88AA74BD7F44B89280727E58A38EEBB08B3 | 2026-10-31 04:36:19 | carrier/BatchVisualTexture.java |
| C7456C163015D9E1F4925527D4DDA6BBCE903C81B30A3B4C69E715EC0032F18B | 2026-10-31 04:36:20 | carrier/BatchVisualShape.java |
| 37C92E67792B6937BACCC2AE330FB7273FA76033B3F0C42451905F8A0C636735 | 2026-10-31 04:38:52 | carrier/BatchCarrierVisual.java |
| 4B562B49BEACEFF94DF39A1576E6C7E87F896D27A8B643F5F82C3C9CC8A96BF8 | 2026-10-31 04:36:51 | carrier/BatchCarrierTypes.java |
| FB91CFAE57A9E085F972385544C85CC1359FD3D5E53392642006B3B6FB03D5AF | 2026-10-31 04:37:33 | carrier/BatchVolumeQuery.java |
| 588CB3B659210A4EFF9E7B6C3CA3D2B13BA66CE520E42EDE04A1E86B50C4F14C | 2026-10-31 04:37:38 | carrier/BatchTriggerVisuals.java |
| 70CFD6232BD0018F712B029F11327A15D7BFB6E9015824DDB73FF56324605B9D | 2026-10-31 04:38:00 | carrier/BatchMeteorLanding.java |
| F0FE02806D91A04CD06776ADA250BD5CC8E495CE85E2BFE80E619B477B17AC47 | 2026-10-31 04:38:02 | carrier/BatchFlightRecord.java |
| 679467039DA4D88B0881A4DBBE14480DE79DCBC9A7039229428CB6E2B49299E8 | 2026-10-31 04:39:11 | carrier/BatchFlameTrail.java |
| C28B46E3F0878BCA389FDBF9B528A9C20355FFCE42C21A7E9A3CA249C68E9364 | 2026-10-31 04:39:59 | carrier/BatchOwners.java |
| 2F44BEDCB79476BCE5FD2126FD18471B4A3BB7201B87CAC93BBCF2049DE4BB65 | 2026-10-31 04:40:01 | carrier/MeteorContactResult.java |
| FB1A706FDB43C480964E03950F795A246E4D4E32D3D4EFA743DB713DC5805D72 | 2026-10-31 04:40:01 | carrier/MeteorContactKind.java |
| 24869B6613D0BCD2462D95C19F635A5AF690FB643B6EFE3DCD0FD4F30493BC9B | 2026-10-31 04:40:49 | carrier/BatchFlightBook.java |
| A397D4CE9C42768B459F9B167E65083AB71ED1DE5AC3446DF15BE9A26DCF815D | 2026-10-31 04:40:26 | carrier/BatchCarrierHooks.java |
| 457CB58D936FCDC07C77D19E0684F35AD6FCC3CEF34F5F6BA0B75055F5D1D047 | 2026-10-31 04:40:53 | carrier/BatchSparkEffect.java |
| 511AEA3A2A35E52392D18933E6C96977A497C7D4099C549AE8E00FF23F2F4CBD | 2026-10-31 04:40:56 | carrier/BatchMeteoriteEffect.java |
| CA6A66744A3B9A12C057E0366600F315526E4F484287CF8EA4D166383F3ABA92 | 2026-10-31 04:42:44 | carrier/BatchShockRingEffect.java |
| 0ECD6F13799B3C29C9BC48532A964BE5BA91EF39AC8C60D17153D02718D78796 | 2026-10-31 04:41:13 | carrier/BatchSurfaceAnchorEffect.java |
| 9F15E1396591C01F696EC5FD82049114572459C24C4B2F5D95232B5D5242DF1C | 2026-10-31 04:41:15 | carrier/BatchRulePassEffect.java |
| EBD8E7B9F575F3CF7B32734BF822AD58451B3207E4F8179DD323837BB170500D | 2026-10-31 04:41:18 | carrier/BatchCarrierBootstrap.java |
| 251A6EF6157CB1E7F7EE226F68381E04570D7CE5E2377A2811245F84585E0975 | 2026-10-31 04:37:20 | client/BatchCarrierVisualRenderer.java |
| D68F24078B3C596C1B998610079533E438F99CA4CB933313770C0D6FB0B9E079 | 2026-10-31 04:41:51 | terrain/MeteoriteWreckageEntity.java |
| 5AF0ED780D8B349EA1DC2B1174830D6C0E137CD42EBAE6A4225D91F42C7DD4EF | 2026-10-31 04:08:44 | block/MeteoriteBlock.java |
| 1D72BCF08D0F4DF256BA69527A62F3D09FBDF65981EF1A27D9C1BB0EAF57CEA9 | 2026-10-31 04:07:25 | block/ModBlocks.java |
| 876F0FD71CB1752052CFF937ACDD651D5D91F26FF9F93019D4DC0B6EE3BD33F8 | 2026-10-31 04:08:47 | terrain/BatchTerrainBreakSwitch.java |
| 680C7CE900F58C09D5DD973D741A63C11757357D5C627865EB40B0C5DD122E6E | 2026-10-31 04:12:51 | terrain/TerrainMutationService.java |
| 857B7D30E11CCFB85E466749F9A7300255446874BA94637612F4794F48BA091C | 2026-10-31 04:08:49 | terrain/MeteoriteWreckageTypes.java |
| 3B78A19B08239BA9CF46E52A122ADBE18ED05F9EFDF676B4097E721D81D5ED65 | 2026-10-31 04:08:53 | client/MeteoriteWreckageRenderer.java |
| 66EF882D5ADBDA924A27C1E9BA75B7E56EB606B7DDB96950E85688F790ACDB4D | 2026-10-31 04:07:27 | datagen/ModBlockTagGenerator.java |
| 9CD3215ABDDDF786A7F4860A1233EC1A8B45DE5668FC3378CCA6FEE3AF036F58 | 2026-10-31 04:07:27 | datagen/loot/ModBlockLootTables.java |

第一段的方块状态、模型和战利品哈希见 json。贴图时间停在归档原文件的 03:15 或 03:17，因为复制保留了源时间，哈希已核对。

停写。
