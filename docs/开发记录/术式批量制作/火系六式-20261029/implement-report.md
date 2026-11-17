# 火系六式实现报告

设计额度 2/2、底座实现额度 2/2 原样继承，本次不重置。底座最后一次 R2/R3 返修没有再做复审。本次没有追加正式实现复审。表状态未改。获取途径未新增。

待验收拟用一句：火系六式已按释放能力接入，待统一编译后由主会话决定是否标待验收。

待统一编译。视觉待用户确认。未追加正式实现复审。

## 各项结果

| 术式 | 结果 |
| --- | --- |
| 升温 `fire_heat_up` | 逻辑已接入。未编译，未进游戏。 |
| 燃烧 `fire_burn` | 逻辑已接入。未编译，未进游戏。 |
| 冷却 `fire_cool` | 逻辑已接入。未编译，未进游戏。 |
| 燎原 `fire_liaoyuan` | 逻辑已接入。未编译，未进游戏。 |
| 燎里 `fire_liaoli` | 逻辑已接入。未编译，未进游戏。 |
| 焚天 `fire_fentian` | 逻辑已接入。未编译，未进游戏。 |

没有因架构冲突而停掉某一式。阻塞只在本阶段明确不做的编译、游戏实测和已确认视觉。

## 登记

链上只看种类、角色和能力，不按这六个编号分支。

| id | 种类 | 标签 | 弹道 | 角色 | 魔力 | 冷却 | 装配 | 伤害 | 复杂度 | 弹速 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| fire_heat_up | SELF_STATUS | TRIGGER | STRAIGHT | ACTION | 20 | 500 | 1 | 0 | 0 | 0 |
| fire_burn | SELF_STATUS | TRIGGER | STRAIGHT | ACTION | 20 | 500 | 1 | 0 | 0 | 0 |
| fire_cool | SELF_STATUS | TRIGGER | STRAIGHT | ACTION | 0 | 0 | 1 | 0 | 0 | 0 |
| fire_liaoyuan | HIT_VOLUME | TRIGGER | STRAIGHT | ATTACK | 60 | 600 | 3 | 2 | 20 | 0 |
| fire_liaoli | HIT_VOLUME | SUSTAINED | FOLLOW_LOOK | ATTACK | 40 | 300 | 3 | 6 | 20 | 0 |
| fire_fentian | HIT_VOLUME | TRIGGER | STRAIGHT | ATTACK | 100 | 1200 | 3 | 2 | 20 | 0 |

炉心三式拒绝混链，不生成命中体。动作分别是 `ENTER_NORMAL`、`ENTER_OVERDRIVE`、`COOL`。升级走 `enterOverdrive`，保留已有加成。冷却走 `cool`，只有这条主动路径放一次蒸汽。

三个消费者不产火种，也不改旧火球和爆燃。效果键是 `xueximod:fire_liaoyuan` 与 `xueximod:fire_fentian`。燎里走引爆，没有燃烧键。

## 未写进表、沿现有代码采用的值

- 炉心复杂度用构造默认 0。现有登录学习会教会复杂度 0 的术式。这是既有路径，本次没有另写技能书或发放。三式终极复杂度用 20，登录不会自动学会。
- 六式弹速都写 0。短构造的默认弹速是 1，这里不用。
- 能量倍率、链能量倍率都是 1。燎原、燎里、焚天缩放长宽高，不缩放模型。
- 炉心伤害为 0，也没有场地、领域、屏障或护体，所以不能接受附魔。三式终极基础伤害大于 0，因此可以接受附魔。
- 炉心弹道列为空。释放定义仍要有弹道，采用 `STRAIGHT`。它们不生成命中体，也不改写别人的弹道。
- 炉心三式要求重新按下。冷却是 0，若沿用按住连发，松手之前每一拍都会再预检并重复提示。
- 混链提示：自身状态说「自身状态不能和其他术式装配」。旧操纵术式的装配句仍是「无法装配：操纵术式不能和其他术式装配」，释放句仍是「这个操纵术式不能和其他术式装配」。
- 伤害许可：排除自己、死亡、旁观、已移除、无敌，以及 `canHarmPlayer` 不允许的玩家。动物和怪物可以打到。队友只在原版允许互相伤害时打到。燎原地面和焚天都用这一条。焚天不看视线。
- 燎原形状按半径 6 的水平圆盘，加上上下 3 格、层高 1。表里的方盒没有做成轴对齐盒子。
- 地表用方块格的顶面，半砖不按碰撞箱精确顶面。
- 地面火第一下在放出后 20 拍，之后每 20 拍一次。同一拍最多一下，漏掉的拍不补。火场 200 拍，不随能量倍率变长，关服不保存。施法者离线时不再打新的地面伤害，计时继续走完。已经挂上的燃烧继续走目标存档。
- 地面伤害固定 2，且标记炉心已经结算，避免再乘一次炉心。同一施法者的燎原附着存在时，这一下地面伤害跳过。
- 火种计划不吃体积倍率。圆盘半径和焚天半径、燎里走廊长宽高可以吃。燎里 40 拍用固定寿命，不吃持续时间倍率。
- 同一本 `ReleaseBatch` 懒创建一个 `FireCastContext`。复制份共用这个对象。燎原同一本账只铺一片火场。
- 落点指定可以改燎原和焚天的原点。燎里强制跟随准星，落点指定不能把它钉成静止斩。燎里也不读取锁定目标来转向。
- 走廊从眼睛沿准星伸出 3×0.6×0.6，实心方块挡住后面的目标。高度用走廊自己的 0.6，不抬成 1。
- 燎里物理用基础伤害 6，走 `hurtRoot`。通用结算里的帧伤害不抄过来，避免打两刀。命中回调之前，现有载荷仍会先套一次。附魔可以装配，刺击本身不再走那套伤害结算。火种引爆另算。复制份可以各打一刀物理，火种只消费一次。
- 命中名额在权限判断之前记下。权限不通过的目标这一下不再补打。
- 焚天用球和包围盒相交，半径默认 8，原点默认身体中心。复制份可以再扫，排他键和同一本账挡住第二份计划。段数和 1.2 的指数不吃能量倍率。
- 预览炉心时会走现有对账。损坏档被读路径修好，是底座行为。
- 炉心提交成功后若扣魔力失败，只记错误，不用结束或冷却把状态改回去。这种情况仍把这次当作已提交。
- 运行时显示名用术式构造里的中文。语言键已写入 `zh_cn.json` 和 `en_us.json`，当前界面取名不读这些键。

## 需求与资源

完整 N、O、DV 在 `docs/开发记录/术式批量制作/火系六式-20261029/input-snapshot.json` 里这六个 id 的整行，以及同目录 `input-snapshot.xlsx`。本次没有改 `docs/术式制作表.xlsx`，也没有改本目录 `记录.md`。

真实引用的只有原版粒子 `FLAME`、`CLOUD`，以及命中体已有的视觉号 4（客户端火焰粒子）。伤害类型继续用已有的 `fire_resource` 和 `steam_burst`。没有新贴图，没有把草稿拷进 `src`，没有改资源库。这些粒子和视觉号都是开发占位/视觉未确认。O 未完成。

## 实测

前置：统一编译通过，并且玩家已经学会对应术式、装进技槽。炉心复杂度 0 会被现有登录学习教会；三式终极复杂度 20，需要另行学会。燎里要手持剑。测试火种和炉心要用已有的火种与炉心状态，本次没有新的产出途径。

步骤与预期：

1. 单独释放升温。没有炉心时进入普通炉心，扣 20 魔力，冷却 500 拍。已经是普通或超频时，炉心、魔力和冷却都不变。
2. 普通炉心上释放燃烧。进入超频并保留加成，扣 20，冷却 500。已经超频则不变。
3. 有炉心时释放冷却。炉心结束，放一次蒸汽，魔力不变，冷却为 0，同一拍可以再按。没有炉心时不放蒸汽。按住不松不会连发。炉心三式不能和其他术式装在同一条链。
4. 燎原：脚下半径 6、上下 3 格内、站在实心方块上的可见一层地面。不放原版火，不改方块。无火种的目标每 20 拍受 2 点资源火伤。有火种的目标第一次接触烧掉全部火种，挂上 3+n 段、每段 `2*(1+0.1*n+F)`、间隔 20 的燃烧，并不再吃这份地面伤害。离开后燃烧继续。100 火种、炉心加成为 0 时是 13 段、每段 4。
5. 燎里：按住刺 40 拍，松开、换掉剑或死亡即停。超过 40 拍必须松开再按。走廊不穿实心块。每个目标一刀物理 6；火种大于 0 时再打 `0.36*S*(1+F)` 的即时火伤。魔力 40 只在起手扣一次，冷却 300 从起手算。没有远程剑、突进或二次爆炸。
6. 焚天：半径 8 的球内、可伤害且火种大于 0 的生物一次烧完。不看视线。n 个整段为 `2*(1.2^n+F)`，余数再加一段按余量比例缩小的尾段，间隔 4。100 点是 10 段、合计约 123.834728448（炉心加成为 0），跨度 40 拍。10 点是 1 段 2.4。50 点是 5 段合计 24.8832。新火种不放大已经挂上的计划。不改全局无敌帧。

## 自检

没有跑 Gradle，没有 `compileJava`，没有 `compileTestJava`，没有打包，没有启动游戏。

名称门禁：

```text
powershell -NoProfile -File tools/check-name-gate.ps1
```

退出码 1。只有未改动的 `OriginalHumanoid.java` 第 165 行 `i`、第 202 行 `to`。本次动过的文件没有新的命名失败。记录在 `implement-name-gate.txt`。

纯逻辑：

```text
javac --release 17 -encoding UTF-8 -d docs/开发记录/术式批量制作/火系六式-20261029/implement-check-classes <八个纯类和 SixFireLogicCheck>
java -cp <同上目录> com.bslx.XueXiMod.shu.technique.release.firecast.SixFireLogicCheck
```

`javac` 退出码 0，`java` 退出码 0，输出 `six fire logic check passed`。记录在 `implement-self-check.txt`。类文件在 `implement-check-classes`，只供这次离线检查。

这次检查只覆盖火种计划、地面圆盘、球相交、走廊纯几何，以及源码里没有按编号分支、焚天没有方块裁剪、地面没有改方块、燎里固定寿命。它不覆盖炉心服务、魔力、冷却、实体节拍和客户端画面，也不能代替编译或游戏实测。

## 文件

SHA256 与最后写入时间在本报告定稿前测得。时间是本机本地时间。

| SHA256 | 写入时间 | 文件 |
| --- | --- | --- |
| CC957D3446C118357B5C7850B8D193B6B20B49EF29E2A10368D55A190A993F03 | 2026-10-29 02:47:28 | src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseKind.java |
| E6A143AB58E39D722AB6613A00F861EFE341AFBF7BF96B1B965F89D3C592C036 | 2026-10-29 02:47:30 | src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseDefinition.java |
| FF4789D922A831598655D7EC54C0B6F16C315FCF706E5A402C19E7C4BD6C1728 | 2026-10-29 02:47:30 | src/main/java/com/bslx/XueXiMod/shu/Technique.java |
| 2979B0584625307DC454A65351990453D9CAE226CE0C885FF5BB9137675C5F03 | 2026-10-29 02:56:08 | src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseDefinitions.java |
| C33653D0F069ADA7BF89A59664FA943FF70E6348424E472B9E2D5C46145F3015 | 2026-10-29 02:48:14 | src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseBatch.java |
| 77BE9C1FC84E7A763D2CFCA756E04995E61F4AA270496D48D96367EE87377A4A | 2026-10-29 02:47:36 | src/main/java/com/bslx/XueXiMod/shu/technique/release/selfstatus/SelfStatusCast.java |
| AD2EABADB23787B078CB818BC040D1DE5E93F2C37FEDD2325BFDB889086A2A80 | 2026-10-29 02:58:10 | src/main/java/com/bslx/XueXiMod/shu/technique/release/selfstatus/SelfStatusDefinition.java |
| 2230A045962685876E402FA70CAA673B581F31BFC72D0D0AA62967D43DF4BC23 | 2026-10-29 02:47:45 | src/main/java/com/bslx/XueXiMod/shu/technique/release/selfstatus/SelfStatusCommit.java |
| 4079997E093484F8A406F23BFD57FAAE92B34F042E58B9A03ED871A6E995DA74 | 2026-10-29 02:47:55 | src/main/java/com/bslx/XueXiMod/status/fire/furnace/FurnaceService.java |
| 0CAB351062D50251D9CC228911F4D5319AD1CA209A71465AB740D799229E7AC2 | 2026-10-29 02:59:04 | src/main/java/com/bslx/XueXiMod/network/SkillSegmentMath.java |
| DC674A2FE6ADF83C66AE985C4E13F7FE8300FFFDDAFB41D6DE5AD8FD29A84336 | 2026-10-29 02:47:57 | src/main/java/com/bslx/XueXiMod/network/SkillChainPlan.java |
| 3BF4BC544CA3F3F5D27F32118DBFA34CBE7F3095798D995BF953F645137C07D3 | 2026-10-29 02:59:04 | src/main/java/com/bslx/XueXiMod/network/SkillUsePacket.java |
| 7B28AA973230323A70CB8BF1C4C06FD54E90F6134CB574DCFD9165030EBFFF2E | 2026-10-29 02:48:01 | src/main/java/com/bslx/XueXiMod/network/SkillAssemblyPacket.java |
| 083C232E4BBC780A7F2A31E7B79F29D2EFEC3BC0BD2EE76597FF3C6C3F6D7A55 | 2026-10-29 02:48:13 | src/main/java/com/bslx/XueXiMod/network/HoldRepeat.java |
| E8BE6CB2C2901079A6F29F94BACB54E20E7FD9964DD6BBC6CF3871E84378DCE8 | 2026-10-29 02:56:37 | src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitSpec.java |
| 71B188EEB8B741CF3A456049E41BDE0E5D0D55433CAE7F92A3BF9F5FF3683A2A | 2026-10-29 02:56:26 | src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitEntity.java |
| 883D4A82F5900FC880E3166B59415F03BB7D4AB3F6D3AA81BE3F9B01B25759B2 | 2026-10-29 02:56:19 | src/main/java/com/bslx/XueXiMod/shu/technique/release/hitbox/SizedBoxHitbox.java |
| F8ADBAFA536A0BA905DBE38A28CB93FDB49BF1111E8AB0781F421F734388A742 | 2026-10-29 02:49:09 | src/main/java/com/bslx/XueXiMod/shu/technique/release/hitbox/MeleeCorridor.java |
| A3FDD41AF7F210A2869AE52D9C561B77961179FD62F385C23B684464193A14D6 | 2026-10-29 02:48:58 | src/main/java/com/bslx/XueXiMod/shu/technique/release/firecast/FireConsumerPlans.java |
| 67B09BAA53AA1AA7DB1A86EB60799E812972382339F04A7D36AFE1E5A81BE196 | 2026-10-29 02:48:37 | src/main/java/com/bslx/XueXiMod/shu/technique/release/firecast/GroundFireMath.java |
| 9D13A26BD834C99AAB0323EDB58CA9B63104BC2FCA5A147BBC4B1477F88EF5B4 | 2026-10-29 02:48:38 | src/main/java/com/bslx/XueXiMod/shu/technique/release/firecast/SphereTouch.java |
| A99AC957765F0E0A14D553F4CAACD856DCEDC060FC3CE8A80B71298EFB0B4769 | 2026-10-29 02:48:46 | src/main/java/com/bslx/XueXiMod/shu/technique/release/firecast/MeleeCorridorMath.java |
| 38B5782CC5ED8459F0D3A92F810C1109C7C5FD3FCA8768FC0C78D256EF382200 | 2026-10-29 02:48:48 | src/main/java/com/bslx/XueXiMod/shu/technique/release/firecast/DamagePermission.java |
| 98B2BA50B38AD5E1402E620440607D93CB06E363352CCF68BAD33362E3D35EA5 | 2026-10-29 02:55:42 | src/main/java/com/bslx/XueXiMod/shu/technique/release/firecast/GroundFireFields.java |
| 6226AA1F990F084AE94155C94098C49B665ECADB391C7ED895F93610F7B62F3F | 2026-10-29 02:55:44 | src/main/java/com/bslx/XueXiMod/shu/technique/effects/LiaoyuanEffect.java |
| 920AECE4A9D064CD9E49C75F8040A6E962CAB004BEDA3E992A1999C628A125AD | 2026-10-29 02:56:37 | src/main/java/com/bslx/XueXiMod/shu/technique/effects/LiaoliEffect.java |
| 9F50CBB926292143CAC1CECF0254E66EE4C9DBAD09EE02956C9F4DB5E1E84B30 | 2026-10-29 02:55:54 | src/main/java/com/bslx/XueXiMod/shu/technique/effects/FentianEffect.java |
| 0232C89803D9DE48B021BFBE5764DCF38449A67534FE497D1C5754E5E1038EC4 | 2026-10-29 02:56:43 | src/main/java/com/bslx/XueXiMod/shu/ModTechniques.java |
| 13FA9CC4CBF7E4E60A1A8D9B9625D2BFB3C8627745AD0EF563B643ACD49F302A | 2026-10-29 02:56:43 | src/main/resources/assets/xueximod/lang/zh_cn.json |
| 5A6633C9DD7056DA81D83C1028BB787D06081659CB315C9920A927DDBFEBDF82 | 2026-10-29 02:56:46 | src/main/resources/assets/xueximod/lang/en_us.json |
| C19E652AC8B31087E36F2656182E5B2DD265D6F8F2FC7478BB95C4BF6804939A | 2026-10-29 02:59:38 | src/test/java/com/bslx/XueXiMod/shu/technique/release/firecast/SixFireLogicCheck.java |
| 6CAF79B884A783B52C2E2F21E6EAC7F1A2DB90A18F3D28C36E69E669AA1FA7DB | 2026-10-29 03:00:51 | docs/开发记录/术式批量制作/火系六式-20261029/implement-name-gate.txt |
| 16842D8CE73C1BFAB853AC6B297BBCF6E7FF44C37475A2C83E645A45E5038EA0 | 2026-10-29 03:00:53 | docs/开发记录/术式批量制作/火系六式-20261029/implement-self-check.txt |

本报告和 `implement-ownership.txt` 是此后的最后写入。写完即停止，不再改源码，文件句柄随写入结束释放。这两份文件的摘要在写完后测量，不回写进文件。

待统一编译。视觉待用户确认。未追加正式实现复审。
