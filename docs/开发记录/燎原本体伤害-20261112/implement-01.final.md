# implement-01 终态回执

- 任务：`spread-body-damage-4` / `implement-01`。关联 B：`fire_liaoyuan`。原批次：火系六式-20261029。
- 身份：本次唯一源码作者。请求模型 `cursor/grok-4.7@256k`，推理 `high`。Fast 依据流程现有 `service_tier="priority"` 客户端配置；本次工具无 tier 参数，不声称后端 Fast 已独立验证。可观察后端身份：未知。
- 直接父代理：本次 spawn 的主会话。已给聊天 ID `01a1c9b3-ca26-7a33-91d2-71c2e32bb69b`，不是代理 ID，未猜。本会话无向父级发送消息的原生工具，终态通知未发出；本文件加最终回复是完整 final。
- 额度：设计 2/2、实现 3/3 已耗尽。本次只落实已明确数值，没有正式复审，没有 Astra，没有新审查链。
- 下属 Flash：未派生。不适用。

## 改动

火场本体基础伤害由 8 改为 4。`payBodies` 仍先乘一次炉心，再经 `SpecialBalance.settleConfigured("fire_liaoyuan")`。

- `src/main/java/com/bslx/XueXiMod/shu/technique/release/firecast/GroundFireFields.java:45`：`GROUND_DAMAGE = 4.0d`。`:46` 的 `BODY_INTERVAL = 10` 未改。`:225` 仍用该常量。
- `src/main/java/com/bslx/XueXiMod/shu/ModTechniques.java:241`：登记伤害 `8f` 改为 `4f`；同行说明由「本体基础8点」改为「本体基础4点」。
- `src/main/resources/assets/xueximod/lang/zh_cn.json:80`：同上，只改本体基础点数。
- `src/main/resources/assets/xueximod/lang/en_us.json:19`：`8 base fire` 改为 `4 base fire`。

`8f` 和语言文案已同步。核对结论：它们不是 `payBodies` 的摄入点，也不另成一段已结算的伤害。

- 燎原显式魔力为 60、装配消耗为 3（`ReleaseDefinitions.java:61`，`explicitManaCost` 在 `:317`）。`Technique.manaCost` 不会退回 `baseDamage * 2`。
- `LiaoyuanEffect` 只读半径、寿命和几何，不读 `getBaseDamage()`、`cast.getDamage()` 或 `CastFrame.damage`，也不生成 `TechniqueHitSpec`。
- 通用链会把登记伤害算进 `SkillSegmentMath.configuredSegmentDamage`，再写入 `CastFrame.damage`（`SkillUsePacket.java:973`，`SkillSegmentRunner.java:669`、`:751`）。燎原效果不消费这个结果。
- 协同编译在 `CooperativeChainCompiler.java:610` 读取登记伤害；`fire_liaoyuan` 没有 `ActorAttackAdapter`，该路径拒绝后不结算。现有适配只有斜斩与横斩。
- 界面「伤害」读的是这个登记值。改成 4 后与本体基础伤害一致。`> 0` 的分支仍成立，不改魔力、装配消耗或附魔资格。

## 未改

- 节拍、半径、寿命、炉心加成式、火种附着段、焚天、其他术式。
- `FireConsumerPlans` 的火种基数 4、20 tick、每段 10 火种，以及旧 spread 展示辅助。
- `damageMultiplier` 未改成 0.5。没有新增配置键，没有改配置文件。
- 没有改制作表、原批次 `docs/开发记录/术式批量制作/` 记录，也没有 reset、clean、checkout 或 commit。
- 美术、占位和正式资源接入：本次不涉及。功能上只改已有数值。

## 检查与收束

- 命名门：`powershell -NoProfile -File tools/check-name-gate.ps1`，退出码 1。共 14 处，全部在本次未写的 `BatchCarrierVisualRenderer.java`、`NetherDomainWisps.java`、`HeatUpSpatialMount.java`。本次文件无新问题，未改这些旧问题。
- 编译：本任务无编译权限。待统一编译。未打包，未启动游戏。
- 两份语言 JSON 已用 `ConvertFrom-Json` 读通。
- 源码与语言写入已收束。没有未完成的下属代理或写入子进程。

## 文件哈希与时间

本地文件时间为 Asia/Shanghai。SHA256 为小写十六进制。

- `GroundFireFields.java`：`6589a46261adb8167cb38efa786d20f1b933f3a8245c638e44a269b16c46ae3b`，2026-11-12 14:39:46
- `ModTechniques.java`：`505523a04a403b78ce2580b5fb591fbc34f6d56496094a2a9ac4a0a46eb52948`，2026-11-12 14:39:46
- `zh_cn.json`：`8aaff432a2f00436e6b5a976f33104ebf15c6022e8836ed493d6c1cf06d9d1fd`，2026-11-12 14:39:46
- `en_us.json`：`d0a9b6c95793ddb57ba46184459edd71761770e869b103a8fcc6658f534f2b7d`，2026-11-12 14:39:46

本回执的 SHA256 与写入时间在文件关闭后单独计算，不写回本文，避免哈希自身改变。
