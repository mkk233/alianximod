# 作者 A 资源接线报告

阶段：已批准实现的接线，不是新的正式复审。设计 2/2 已用完，实现复审仍是 0/3。本报告不是复审通过。

## 结论

13 个术式 id 已登记，印记树能显示四条新规则。效果实现仍等 C，本轮没有空的成功效果。新附魔魔力走 `ResourceEnchantCatalog.explicitManaPrice`，0 点魔法伤害不再收 1。同一附魔占两槽收两次，效果仍由 B 去重。

新链命中携带 B 的 `ResourceEnchantLoad`、本体基数和旧固定点数。伤害真正接受后只调用一次 `ResourceHitCommit.commit`。同一份 2 或 10 不再 deposit 或 produce。旧点火、热光束、火球在没有新能力时仍走旧 deposit。

根上下文与 `BatchCastSession.rootCastId()` 是同一次释放。费用结算不提前清去重。最后持有者结束或取消且没有未完命中时释放。普通武器的纯新载荷会保存，并在下一次合法攻击接受后提交一次；失败保留印记。

## 法杖

`basic_staff` 仍是 `StaffData(6, 0.20, 0.10, 100, null)`。`test_staff` 仍是 `StaffData(32, 15.0, 0.50, 1000, null)`。效率 15.0 是因为回复公式为 `5 * (1 + efficiency)`，15.0 得到 80，即基础 5 的 16 倍。本轮没有改这两条注册。

## 报价 174

陨岩 48、热波 8、六条新附魔各 2，都先走显式魔力价，再走原费用。示例 3×48 + 2 + 2 + 3×8 + 2 = 174 仍由原复制份数乘每槽费用，没有把附魔按复制份数再乘。未出生份额仍退回原收据的池和法杖。

## 本轮文件

| sha256 | mtime | path |
| --- | --- | --- |
| e8c7c19564cb1ca4edd23a077c1c61f87c6d0a052be18a0be900985b1ac7918d | 2026-10-31 04:09:21 | ModTechniques.java |
| e05ac47e3268390690abf8f2548c50cd68ea9fa738d887abd047e3fe9157e90d | 2026-10-31 04:08:48 | Technique.java |
| a7af7c28fcee573ed41e0bf44c883fd9d0ca02c82d14a463eac0c241b1754147 | 2026-10-31 04:08:50 | ImprintTreeView.java |
| 506f4fdebe59bb6e3e6c1c4230171d19e86a36265591237df09df475c6023c45 | 2026-10-31 04:12:02 | WeaponEnchant.java |
| b9470ab5d97904a47cd343eb535c90b172170c1078d07fa8ea80b9b3f7d0ade3 | 2026-10-31 04:08:46 | ChainRole.java |
| 96008b345c92440d494cda3490491e409685b2d42ab736698e69a7beab7e1295 | 2026-10-31 04:08:47 | ReleaseDefinition.java |
| 7223a394a74637d8eddb82069e27083556f3a300a5545a21694d07b4c83bbd21 | 2026-10-31 04:09:13 | ReleaseDefinitions.java |
| 2929e0014fb0f54e924773b7cbae2f78cac92491791c76e5291a2e0beea379c1 | 2026-10-31 04:10:50 | ReleaseBatch.java |
| 3f57610db45fe94b297b0b96c2944b2311657c8077c5f05277bd07f80741d0e4 | 2026-10-31 04:09:10 | batch/ListedRelease.java |
| 71535e3cf126c5842f8dab0103f06b9c636b042c4fdaf36543f0a21bed556b01 | 2026-10-31 04:09:31 | batch/RootCastBindings.java |
| 1fe07f6286dbf49dbeedfe1eedd579766e5d4ef12769fca555f3ed5ab8a40019 | 2026-10-31 04:09:34 | batch/ResolvedHit.java |
| a2dc2d8ff1e8bdef01417366893ff222eebc9b441604a74a81b38a170c16cf4a | 2026-10-31 04:09:51 | batch/BirthLayout.java |
| 868c44fa31a139d8cf195a0ed53b5efbfc8f060868fb383018c3a12772b0739b | 2026-10-31 04:09:49 | batch/CarrierHooks.java |
| f934b1968306b7ccce41575e3f2c25bef2885dcd7053b2b1905b465f0924ac45 | 2026-10-31 04:12:30 | entity/ResourceSettlement.java |
| 9d0a685aaf8629f8993365c5b8c0d04f48a3f3f7cdaaacf3f1a3ab8cd812b53b | 2026-10-31 04:09:52 | TechniqueHitSpec.java |
| 190feaaaf2ff1fc9b14ccca0bc13ba8d6fd3c6a38aef32d8bbc224ba412c2135 | 2026-10-31 04:09:56 | TechniqueHitEntity.java |
| 373b8f1522cfee0030235ec4c5c81949d26d1eea81bfd7c1a07f6fb2849c86e9 | 2026-10-31 04:12:31 | TechniqueHitSettle.java |
| 3d4488abd8c02e47c7b0e2d5451ab0ebd127c39b29db25a28f351dd9f1761c06 | 2026-10-31 04:08:49 | SkillSegmentMath.java |
| d9e35b6a2821833d11860feb3b6df19ab72fd17f20544b0113fa374955d58fd1 | 2026-10-31 04:11:16 | BatchCastSession.java |
| 6e7ef103b671d78de8a77eba8ab4486d70067776d9518bdc69cd376c82ee5ae3 | 2026-10-31 04:12:02 | lang/zh_cn.json |
| c378a46e3bf8255c12633659a4f5e44ac951e041598e32d86dbe8defc272e512 | 2026-10-31 04:12:03 | lang/en_us.json |

完整 54 个归属路径在 `author-a-ownership.json`。其 sha256 为 `a7a872f42e3cc7953acccacff10526fdd5393fee69ef2e43a8166c2c738b9a30`，mtime `2026-10-31 04:14:03`。

## 检查

`tools/check-name-gate.ps1` 退出码 1。本轮改过的文件没有新增违规。仅剩未改的 `OriginalHumanoid.java` 第 165 行 `i`、第 202 行 `to`。没有跑 Gradle、javac、打包或游戏。状态是待统一编译。

## 阻塞

C 尚未提供陨岩、热波、火苗效果，以及留焰、幽冥、崩地的持续实现。这三项攻击现在不能生成命中体。陨石方块和实体类名未知，没有注册。B 没有剩余方法请求。

## 停写

本范围写入到此停止。不再改 B、C、表、资源索引或记录。
