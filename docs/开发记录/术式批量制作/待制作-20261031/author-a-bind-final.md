先读这份授权说明，再按它的范围做资源绑定。上一段已经停写。这一段只接 B 的稳定接口，并登记术式和普通武器命中。先读批准合同和 B 的公开接口。B 的资源接口已经稳定。接下来登记 13 个术式，并把命中、根上下文和普通武器接到这些公开方法上。13 个术式 id 已登记，效果仍等 C。新附魔按显式价收费，命中只向 B 提交一次，根上下文与同一次释放绑定。`basic_staff` 仍是 6 槽，`test_staff` 仍是 32 槽、效率 15.0。本范围已停写，状态为待统一编译。这不是正式复审。

## 登记与效果

`ModTechniques` 和 `ReleaseDefinitions` 已登记这 13 条。等级上限 20，没有单独的战斗成长字段。六条附魔的链角色是 `NOT_APPLICABLE`，不是 `ATTACK`。印记树里地表定位进索敌，喷涌进弹道，错时和命中接续进规则；附魔仍按元素分组。

| id | 名称 | 魔力 | 冷却 | 复杂 | 装配 |
| --- | --- | --- | --- | --- | --- |
| fire_flame_trail | 留焰 | 2 | 5 | 2 | 2 |
| fire_meteorite | 陨岩 | 48 | 40 | 6 | 1 |
| fire_shock_ring | 热波 | 8 | 16 | 2 | 1 |
| rule_surface_anchor | 地表定位 | 0 | 0 | 1 | 1 |
| fire_upwell | 喷涌 | 0 | 0 | 2 | 1 |
| rule_stagger | 错时 | 0 | 0 | 2 | 1 |
| rule_hit_continue | 命中接续 | 0 | 0 | 3 | 1 |
| fire_seed_sow | 种火 | 2 | 5 | 1 | 2 |
| fire_kindle | 引燃 | 2 | 5 | 2 | 2 |
| fire_detonate | 引爆 | 2 | 5 | 2 | 2 |
| fire_nether_gloom | 幽冥 | 2 | 5 | 3 | 2 |
| earth_break_ground | 崩地 | 2 | 5 | 3 | 2 |
| fire_spark | 火苗 | 2 | 5 | 0 | 1 |

没有向 `TechniqueEffectRegistry` 注册效果，也没有写空的成功实现。陨岩、热波、火苗在 C 登记效果之前不能生成命中体，份额留在预算里。留焰几何、幽冥周期、崩地方块，以及陨石方块和实体，都等 C 的类名，本轮没有注册。

报价仍是 3×48 + 2 + 2 + 3×8 + 2 = 174。附魔按原槽各收一次，不按复制份数再乘。0 点魔法伤害走 `ResourceEnchantCatalog.explicitManaPrice`，不再收 1。同一附魔占两槽收两次，效果去重仍由 B 做。

## 法杖

`basic_staff` 仍是 `StaffData(6, 0.20, 0.10, 100, null)`。`test_staff` 仍是 `StaffData(32, 15.0, 0.50, 1000, null)`。回复公式是 `5 * (1 + efficiency)`，效率 15.0 得到 80，即基础 5 的 16 倍。本轮没有改这两条注册。

## B 调用与根生命周期

一次逻辑释放只有一个 `FireCastContext`，id 就是 `BatchCastSession.rootCastId()`。会话 `open`，结束时 `release` 一次。命中体生成时 `retain`，移除时 `release`。错时子分支在等待期间再持有一次。费用结算不提前清去重。持有数到 0 才移出表。取消且没有未完命中时，会话释放自己的那一次。

伤害真正接受后只调用一次 `ResourceHitCommit.commit`。本体基数和种火 10 先收齐，同一份 2 或 10 不再 `deposit` 或 `produce`。种火 10 在载荷的缩放基数里。旧点火 2、热光束 1、火球 3 留在 `legacyFixedPoints`。火苗本体 2 来自 `bodyScaledSeedBase()`。C 写火苗时不要再把 2 写进 `seedPoints`。没有新能力的旧点火、热光束、火球仍只走原来的 `deposit`。

`DAMAGE_ACCEPTED` 才把资源接受传为真。取消、免疫和 `PENDING` 都不是资源成功。子分支等父分支的真实完成回调，不再加固定 6 tick。错时仍是每份 +10 tick。

给 B 的剩余方法请求：无。用的是已有的 `FireCastContext(UUID, UUID)`、`explicitManaPrice`、`collect`、`commit`、`claimEffect`。

## 普通武器

纯新载荷不再因为旧伤害和旧种火点数都是 0 被丢掉。`ResourceIds` 只存术式 id，不存熔炉热量。下一次合法普通攻击被接受后提交一次。结果不是 `APPLIED` 时印记保留，不能当成成功，也不第二次收魔力。引燃热量由 B 在提交当时读取。带留焰、幽冥或崩地时会 `adopt` 根上下文，C 结束时必须 `release`。

## C 要接的签名

`RootCastBindings.open`、`adopt`、`lookup`、`retain`、`release`。`ResolvedHit` 带真实目标、主人、裁决、B 载荷、根上下文、本体基数、旧固定点数和宿主基数。`CarrierHooks.VerdictListener.onResolved` 是新增默认方法，旧监听不用改。拖尾若比命中体活得更久，C 必须在命中体释放前 `retain`，并在自己的最后一段结束时 `release`。

## 本轮文件

| sha256 | mtime | 文件 |
| --- | --- | --- |
| e8c7c19564cb1ca4edd23a077c1c61f87c6d0a052be18a0be900985b1ac7918d | 2026-10-31 04:09:21 | ModTechniques.java |
| e05ac47e3268390690abf8f2548c50cd68ea9fa738d887abd047e3fe9157e90d | 2026-10-31 04:08:48 | Technique.java |
| a7af7c28fcee573ed41e0bf44c883fd9d0ca02c82d14a463eac0c241b1754147 | 2026-10-31 04:08:50 | ImprintTreeView.java |
| 506f4fdebe59bb6e3e6c1c4230171d19e86a36265591237df09df475c6023c45 | 2026-10-31 04:12:02 | WeaponEnchant.java |
| b9470ab5d97904a47cd343eb535c90b172170c1078d07fa8ea80b9b3f7d0ade3 | 2026-10-31 04:08:46 | ChainRole.java |
| 96008b345c92440d494cda3490491e409685b2d42ab736698e69a7beab7e1295 | 2026-10-31 04:08:47 | ReleaseDefinition.java |
| 7223a394a74637d8eddb82069e27083556f3a300a5545a21694d07b4c83bbd21 | 2026-10-31 04:09:13 | ReleaseDefinitions.java |
| 2929e0014fb0f54e924773b7cbae2f78cac92491791c76e5291a2e0beea379c1 | 2026-10-31 04:10:50 | ReleaseBatch.java |
| 3f57610db45fe94b297b0b96c2944b2311657c8077c5f05277bd07f80741d0e4 | 2026-10-31 04:09:10 | ListedRelease.java |
| 71535e3cf126c5842f8dab0103f06b9c636b042c4fdaf36543f0a21bed556b01 | 2026-10-31 04:09:31 | RootCastBindings.java |
| 1fe07f6286dbf49dbeedfe1eedd579766e5d4ef12769fca555f3ed5ab8a40019 | 2026-10-31 04:09:34 | ResolvedHit.java |
| a2dc2d8ff1e8bdef01417366893ff222eebc9b441604a74a81b38a170c16cf4a | 2026-10-31 04:09:51 | BirthLayout.java |
| 868c44fa31a139d8cf195a0ed53b5efbfc8f060868fb383018c3a12772b0739b | 2026-10-31 04:09:49 | CarrierHooks.java |
| f934b1968306b7ccce41575e3f2c25bef2885dcd7053b2b1905b465f0924ac45 | 2026-10-31 04:12:30 | ResourceSettlement.java |
| 9d0a685aaf8629f8993365c5b8c0d04f48a3f3f7cdaaacf3f1a3ab8cd812b53b | 2026-10-31 04:09:52 | TechniqueHitSpec.java |
| 190feaaaf2ff1fc9b14ccca0bc13ba8d6fd3c6a38aef32d8bbc224ba412c2135 | 2026-10-31 04:09:56 | TechniqueHitEntity.java |
| 373b8f1522cfee0030235ec4c5c81949d26d1eea81bfd7c1a07f6fb2849c86e9 | 2026-10-31 04:12:31 | TechniqueHitSettle.java |
| 3d4488abd8c02e47c7b0e2d5451ab0ebd127c39b29db25a28f351dd9f1761c06 | 2026-10-31 04:08:49 | SkillSegmentMath.java |
| d9e35b6a2821833d11860feb3b6df19ab72fd17f20544b0113fa374955d58fd1 | 2026-10-31 04:11:16 | BatchCastSession.java |
| 6e7ef103b671d78de8a77eba8ab4486d70067776d9518bdc69cd376c82ee5ae3 | 2026-10-31 04:12:02 | zh_cn.json |
| c378a46e3bf8255c12633659a4f5e44ac951e041598e32d86dbe8defc272e512 | 2026-10-31 04:12:03 | en_us.json |

归属共 54 个路径，`author-a-ownership.json` 的 sha256 是 `a7a872f42e3cc7953acccacff10526fdd5393fee69ef2e43a8166c2c738b9a30`，mtime `2026-10-31 04:14:03`。

冻结后的交付文件：

| sha256 | mtime | 文件 |
| --- | --- | --- |
| 8ab36d4f9d1da3ec411df00b0d57f8ac588139931332faf9863b7c03a67c2f4b | 2026-10-31 04:14:43 | author-a-interface-v2.md |
| 0d737a44330d39dd4125f4e3720d24e10cbcf814908c8c805ec300bde88ab1e9 | 2026-10-31 04:14:57 | author-a-bind-report.md |
| 2bcadc11fa621eb0e9f689d507f7a1520bc9dad92377f916b0e7604e635b3631 | 2026-10-31 04:15:08 | author-a-bind-report.json |

## 检查与阻塞

`tools/check-name-gate.ps1` 退出码 1。本轮改过的文件没有新增违规。只剩未改的 `OriginalHumanoid.java` 第 165 行 `i`、第 202 行 `to`。没有跑 Gradle、javac、打包或游戏。

阻塞是 C 的三项攻击效果、留焰、幽冥、崩地，以及未知的陨石方块和实体类名。

## 停写

本范围写入到此停止。不再改 B、C、表、资源索引或记录。
