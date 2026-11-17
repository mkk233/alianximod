本次只读定位结论：**最强根因线索是铁傀儡未满足水牢的“敌对目标”资格。现行规则下，潮湿Ⅳ本身不足以被捕获；未对施术者愤怒、且当前攻击目标不是施术者的中立生物会被排除。**这与现行群体水牢需求一致。现有日志确认了失败提示，但没有记录铁傀儡当时的关系、精确湿度与距离，因此尚不能把该线索定为本次唯一根因。

继续沿用 `water-vortex-20261103 / water-prison-target-repair / read-01`。全程仅使用只读工具，未修改源码、配置或表，未编译、未操作游戏、未启动独立审查；设计 **2/2**、实现 **3/3** 保持不变。此前工具中断是本地配置扩展名读取问题，已补读成功。

**提示与实际失败位置**

源码中的确切提示是 **“没有可禁锢的潮湿目标”**，比反馈转述少一个“以”字。[latest.log](/D:/alianximod/run/logs/latest.log) 中，2026-11-09 **22:31:26.756 至 22:31:49.203** 多次出现该提示。

调用入口为 [SkillSegmentRunner.java:470](/D:/alianximod/src/main/java/com/bslx/XueXiMod/network/SkillSegmentRunner.java:470)，调用条件效果的 `attempt(...)`。该提示只在 [WaterPrisonEffect.java:81](/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/conditional/WaterPrisonEffect.java:81) 的三个分支返回：

- 第 **81–82** 行：施术者或操作编号为空。
- 第 **93–94** 行：扫描结果为空。
- 第 **97–99** 行：剔除空候选/空 UUID 后为空。

正常扫描已保证候选及 UUID 有效，因此**扫描结果为空是正常调用下的主要解释**。这句提示发生在逐目标付款、预约和创建水牢之前；不能据此判断为魔力不足、已有牢或牢壳放不下。

**目标必须同时满足的条件**

| 条件 | 当前实现及对应位置 | 对此次反馈的意义 |
|---|---|---|
| 施术者有效 | [StatusScan.java:27](/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/statusquery/StatusScan.java:27)：存活、非旁观、未移除，处于服务端世界，且在服务器线程执行 | 无效时直接返回空列表；日志未提供这类异常证据 |
| 目标有效 | 同文件第 **43–54、122–134** 行：同世界、存活、非旁观、未移除，排除施术者自己，按 UUID 去重 | 没有铁傀儡种类黑名单 |
| 距离 | 同文件第 **37、62–65** 行：施术者脚底位置到目标真实包围盒最近点的三维距离；默认半径 **12**，包含边界 | 不是目标中心距离，也不是只算水平距离 |
| 周身扫描 | [WaterPrisonEffect.java:87](/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/conditional/WaterPrisonEffect.java:87) 请求全圆扫描；`StatusScan` 第 **67** 行因此跳过朝向限制 | 身后目标可以入选，不要求准星瞄准 |
| 敌对关系 | [RelationDecision.java:30](/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/statusquery/RelationDecision.java:30)：敌对怪物接受；中立生物须满足愤怒条件；玩家须允许伤害；自己、盟友、自有驯服物和 OTHER 拒绝 | **最强嫌疑项** |
| 方块遮挡 | [BlockOcclusion.java:48](/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/statusquery/BlockOcclusion.java:48)：从施术者眼睛射向上述最近点，方块采用 `COLLIDER`，流体采用 `NONE`，只接受 `MISS` | 其他铁傀儡和水不会遮挡；方块会 |
| 潮湿条件 | `WaterPrisonEffect` 第 **89** 行要求资源值 ≥1 **且**同名效果有效；[StatusQuery.java:97](/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/statusquery/StatusQuery.java:97)、第 **100–115** 行分别检查数值与效果剩余时间 | 并不要求潮湿Ⅳ，一档也可入选；仅有药水显示不够 |

扫描实际顺序是**距离 → 关系 → 遮挡 → 潮湿**，见 `StatusScan` 第 **62–80** 行。关系被拒绝时，甚至不会继续检查潮湿。

**铁傀儡友敌筛选：代码能证明什么**

[RelationDecision.java:78](/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/statusquery/RelationDecision.java:78) 按 `instanceof NeutralMob` 分类；中立分类优先于 `Enemy/Monster`。第 **84–91** 行的通过条件严格为：

```java
neutral.isAngryAt(caster)
    || (subject instanceof Mob creature && creature.getTarget() == caster)
```

按 Minecraft 1.20.1 普通铁傀儡的中立类型，它走此分支。因此：

- 只是被叠湿、站在附近，不能证明它对施术者敌对。
- 正在攻击别的生物，也不满足“攻击施术者”。
- 没有“铁傀儡天然算玩家盟友”的特殊判断；[Relations.java:16](/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/relations/Relations.java:16) 的 `ally()` 当前恒为 `false`。

**可证明的是上述拒绝规则；未证明的是这几只铁傀儡施放当刻是否满足愤怒条件。**本次未读取依赖中的原版铁傀儡类，也未取得运行实体快照。创造模式、玩家建造的铁傀儡、无 AI 等因素只能列作需要确认的现场背景，不能直接定因。

**“潮湿4”与后端数值是否一致**

正常术式施湿路径中，两者对应一致，但含义不同：

| 显示档位 | 后端精确湿度 | 药水 amplifier |
|---|---:|---:|
| Ⅰ | 1–25 | 0 |
| Ⅱ | 26–50 | 1 |
| Ⅲ | 51–75 | 2 |
| Ⅳ | 76–100 | 3 |

依据为 [WetnessState.java:15](/D:/alianximod/src/main/java/com/bslx/XueXiMod/status/WetnessState.java:15)。第 **35–47** 行先更新精确值，再添加 `tier - 1` 的效果，正常施加刷新至 **1000 tick**。

水牢读取的是 [ResourceStatuses.java:92](/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/statusquery/ResourceStatuses.java:92) 返回的 `WetnessState.read(subject)`。精确值存于实体持久数据 **`xueximod_attribute_states.wetness`**，见 [AttributeStateData.java:9](/D:/alianximod/src/main/java/com/bslx/XueXiMod/status/AttributeStateData.java:9)，不会从 amplifier 反推。

因此，**显示“潮湿Ⅳ”不能单独证明当前后端值确为 76–100**。直接命令或外部方式仅挂药水而未写资源值，代码上可以形成“有效果、数值为零”的状态，水牢将拒绝。对齐逻辑在 [AttributeStateService.java:39](/D:/alianximod/src/main/java/com/bslx/XueXiMod/status/AttributeStateService.java:39)，已找到的调用是实体加入世界时的 [AttributeStateEvents.java:18](/D:/alianximod/src/main/java/com/bslx/XueXiMod/status/AttributeStateEvents.java:18)，不能据此声称每次施放前必然对齐。

现场配置 [jade/plugins.json:54](/D:/alianximod/run/config/jade/plugins.json:54) 启用了 `potion_effects`，日志也确认加载了 Jade。**显示来自 Jade 药水面板是合理推测，尚未确认用户看到的具体界面；本次未读 Jade 外部实现。**日志没有精确湿度，无法判定此次是否发生显示与资源不一致。

**距离、空间继承与体积排查结果**

- **扫描中心固定为施术者当前位置。**`StatusScan` 第 **37** 行直接读取 `caster.position()`；本次水牢调用没有传入前段落点、命中位置或空间覆盖。前段在远处制造潮湿，不会让水牢改从那个位置扫描。
- **聚能不扩大扫描半径。**`WaterPrisonEffect` 第 **88** 行只读取平衡参数 `scanRange`；`ReleaseInput` 在成牢阶段用于寿命和壳尺寸，见 [PrisonRuntime.java:90](/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonRuntime.java:90)。不能按聚能后的视觉大小推算捕获范围。
- **没有水牢目标体积上限、人数容量或“铁傀儡太大”的判定。**成牢时按目标真实包围盒外扩 **0.20**，再缩放，见 [PrisonShell.java:20](/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonShell.java:20)。旋流/水龙卷的体积容量不能套到水牢。
- **遮挡仍有现场不确定性。**射线终点是脚底最近点，并非头部或身体中心；看得见上半身，不能保证该射线无碰撞。地形、台阶或边缘遮挡是否影响本次，需要当刻位置与碰撞命中证据；目前不能认定算法有错。
- 已读 [xueximod-common.toml:11](/D:/alianximod/run/config/xueximod-common.toml:11)，其中潮湿配置只涉及下降豁免，没有水牢目标或范围设置。真正平衡文件位于**当前世界的 `serverconfig/xueximod-balance.json`**，见 [BalanceDocuments.java:35](/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/balance/BalanceDocuments.java:35)。它不在本次指定的 `run/config` 范围内，本次未读；**12 格是默认值，尚未核实当前世界生效值。**

**现行需求与用户认知的冲突**

现行依据是 [群体水牢与状态索敌记录.md:14](/D:/alianximod/docs/开发记录/群体水牢与状态索敌-2026-10-28/记录.md:14)，明确写明：

> 捕获潮湿的敌对怪物、正在攻击施术者的中立生物，以及允许 PvP 的非盟友玩家。不能把所有非盟友均视作敌对。

同文件第 **92** 行进一步明确“未敌对中立者……不捕获”。当前注册说明 [WaterTechniques.java:54](/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/WaterTechniques.java:54) 也写的是十二格内潮湿的敌对目标。

10 月 26 日旧指导中的“二十格准星单目标”已被这份群体需求替代，不能作为当前选目标规则。

**如果用户预期是“附近所有潮湿铁傀儡均可禁锢，不论是否攻击自己”，就与现行需求冲突。放开中立筛选属于玩法变更，本次不建议按故障修复直接实施。**

**最小修复建议与文件清单**

目前还没有证据支持修改捕获逻辑。建议按以下最小范围处理：

| 后续证据/目标 | 最小候选文件 | 建议 |
|---|---|---|
| 确认铁傀儡未敌对，保持现行玩法 | `WaterPrisonEffect.java`；可选 `WaterTechniques.java` | 改善笼统提示，例如“附近没有符合距离、遮挡和敌对条件的潮湿目标”；说明中明确中立生物须攻击自己。无需改关系逻辑 |
| 仍无法区分拒绝原因 | `StatusScan.java`，必要时配合 `WaterPrisonEffect.java` | 仅在调试开关下记录本次扫描各筛选阶段数量及候选拒绝原因，避免持续逐 tick 刷日志 |
| 证实有效果但精确湿度为零 | 实际施湿入口，届时再定文件 | 修复产生不一致的入口，使其使用正确资源 API；不建议删除水牢的资源检查或从Ⅳ级臆造精确值 |
| 证实敌对、湿度及距离均合格，却被遮挡排除 | `BlockOcclusion.java` | 根据实际碰撞证据定向修复；当前不足以支持改射线或取消遮挡 |

`RelationDecision`/`RelationPolicy` 已被其他玩法复用，**不要为了让测试铁傀儡入选，直接放宽公共策略**。

要确认本次根因，最少还缺**一次失败施放同刻、同一只铁傀儡**的以下证据：

1. 实体 UUID/类型、`NeutralMob.isAngryAt(caster)` 与 `Mob.getTarget()`；玩家模式及铁傀儡是否玩家建造、是否无 AI。
2. `xueximod_attribute_states.wetness`、潮湿效果 amplifier/剩余 tick，以及潮湿来自术式还是命令。
3. 玩家位置/眼睛位置、目标包围盒、最近点距离、遮挡射线命中的方块。
4. 当前世界实际生效的 `water_prison.scanRange`，以及正在运行版本与所读源码的对应关系。

现有日志只记录了失败文案，没有这些字段；仅反复读取同一日志无法进一步定因。

**发现的已有任务关联**

- 当前：`water-vortex-20261103 / water-prison-target-repair / read-01`，属于本次实测返修定位，沿用 **2/2、3/3**。
- 群体捕获的直接来源：`status-targeting-prison-implementation-20261028`，记录于[群体水牢记录.md:40](/D:/alianximod/docs/开发记录/群体水牢与状态索敌-2026-10-28/记录.md:40)；`author-a` 负责状态查询/关系，`author-b` 负责群体水牢接线与付款。这是后续定位时最直接的原作者归属。
- [聚能输入链核查-20261027](/D:/alianximod/docs/开发记录/聚能输入链核查-20261027/entities-report.md:3)：关联壳尺寸、寿命缩放和潮湿支撑期。
- [水牢特效接入-20261027](/D:/alianximod/docs/开发记录/水牢特效接入-20261027/记录.md:31)：关联客户端公告板及纹理，没有目标筛选职责。
- [配置平衡-20261101](/D:/alianximod/docs/开发记录/配置平衡-20261101/author-B2-write-final.md:15)：关联水牢扫描范围与时长配置。

以上历史记录仅用于需求与实现溯源，没有借用、重置或新增其复审额度。