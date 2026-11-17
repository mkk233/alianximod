## 只读补查报告（给定源码范围）

**结论先行**：在给定文件里，聚能对命中实体的影响只出现在 `TechniqueHitSpec.spawnReported` 的 `CastFrame` 分支：第 143 行取帧，第 145-221 行写入。写入项包括索敌开关、结算字段、体积倍率、速度倍率和几何覆写。`lifetime`、`hitInterval`、`hitReadyAge` 在该分支内没有被读取或缩放。`WeepingCurtain`、`FieldZones`、`FieldSpawner`、`BarrierService`、`HeatBeamEffect`、`TestSustainedEffect` 都没有 import `CastFrame`；`WeepingCurtainEffect` 第 6、27、39、42-56 行直接读帧，是 `TechniqueHitSpec` 之外的第二处消费者。以下只陈述事实与待确认项。

### 一、持续实体的寿命与活动窗口

普通路径是同源的：`TechniqueHitSpec.lifetime` 第 62 行 → `spawnReported` 不修改 → `TechniqueHitEntity.applySpec` 第 367 行抄入 → `tick` 第 310-312 行按 `age >= lifetime` 到期。`age` 在第 268 行递增，`onExpire` 在第 311 行调用。

有两个提前返回会改变“活动窗口”的含义：

- 第 259-261 行：`pendingActivation` 为真时直接 return，`age` 不增，寿命暂停，直到宿主组 `releaseActivation`（第 329-338 行）。实体可以停留在世界里而不消耗寿命。
- 第 263-266 行：存在 `arcSnapshot` 时转入 `tickArcBand` 并 return，`age` 不增，第 310 行的 `lifetime` 不再生效。弧带寿命改由 `installArc` 第 731 行 `snapshot.lifeTicks()` 覆盖，活动窗口由第 743 行 `gameTime - birthTick`、第 748 行 `activeTicks`、第 767 行 `lifeTicks`、第 782 行偏转窗口分别控制。`bakeArc` 第 397-448 行没有传入 `spec.lifetime`，所以弧带实体的寿命与 `spec.lifetime` 不同源。

静帧视觉还有一套时钟：`DATA_STILL_BIRTH` 第 79-80 行、`stampStillBirth` 第 665-673 行、`visualBirth` 第 656-658 行、`DATA_STILL_FRAME_TICKS` 第 77-78 行、`DATA_STILL_CANCELLED` 第 81-82 行。客户端粒子用 `tickCount` 第 320 行，和服务器 `age` 不是同一计数。

### 二、三个术式的专用时钟

**垂丝**：`WeepingCurtainEffect` 第 28、37 行把寿命设为定义值，缺省 `WeepingSettings.LIFETIME`；实体到期仍走第 310 行。但水体填充、满流、排空由 `WeepingCurtain` 第 123 行 `FILL_TICKS`、第 127 行 `LIFETIME`、第 128 行 `DRAIN_START` 驱动，第 65 行只把 `entity.age()` 传进去，第 171 行用 `age == FILL_TICKS` 判定溅水。`WeepingCurtain.java` 全文没有读 `spec.lifetime`，所以水体相位确实绕过 `spec.lifetime`。当前 `ReleaseDefinitions` 第 22 行给 `water_weeping_arc` 的寿命是 `WeepingSettings.LIFETIME`（40），两套时钟数值暂时相同。

**热光束**：`HeatBeamEffect` 第 22 行取定义寿命（缺省 40），第 34 行抄入 `spec.lifetime`，没有第二套寿命时钟。第 35 行 `hitInterval = 10` 是命中节拍，不是寿命。第 30 行 `speed = 0.0`，第 181-183 行的速度倍率对它无效；第 31 行长度 6.0 会进入第 157-170 行的体积缩放（定义第 13 行走默认 scale 全开）。`path = FOLLOW` 第 29 行对应实体第 420-421 行的光束固定。

**TestSustained**：第 24、25 行速度与寿命取定义（缺省 0.4 / 20），第 37 行抄入，没有专用寿命时钟。第 32、39 行为 `DROP` 和 `dropThisCast`。没有设置 `hitInterval`，默认 0，配合实体第 607 行 `hitOnce`，同一个目标只命中一次。

因此：热光束与 TestSustained 的实体寿命不绕过 `spec.lifetime`；垂丝的水体相位绕过；弧带与静帧另有独立时钟。

### 三、需同源扩展的时间字段与无倍率基线

| 位置 | 字段 | 无倍率基线 | 当前是否被 `CastFrame` 改写 |
|---|---|---|---|
| `TechniqueHitSpec.java:62` | `lifetime` | 20 tick | 否 |
| `TechniqueHitSpec.java:71` | `hitInterval` | 0 | 否 |
| `TechniqueHitSpec.java:72` | `hitReadyAge` | 0 | 否 |
| `TechniqueHitSpec.java:63` | `speed` | 1.0 | 仅第 181-183 行，且 `speedMultiplier > 1.001` |
| `TechniqueHitSpec.java:75` | `modelScale` | 1.0 | 第 157-170 行，非弧带 |
| `TechniqueHitSpec.java:81-83` | `length/width/height` | 各 1.0 | 第 157-170 行，非弧带 |
| `TechniqueHitSpec.java:67-69` | `arcRadius/arcDegrees/arcHeight` | 2.0 / 120 / 2.0 | 否，弧带走第 144 行跳体积缩放 |
| `WeepingSettings.java:10-15` | `HIT_INTERVAL/LIFETIME/FILL/DRAIN/FALL` | 10 / 40 / 8 / 32 / 6.0 | 否，静态常量 |
| `WeepingCurtain.java:123,127,128,171` | 填充、排空、溅水相位 | 同上 | 否 |
| `TechniqueHitEntity.java:731,748,767,782` | `lifeTicks/activeTicks/birthTick` | 快照自带 | 否，覆盖 `spec.lifetime` |
| `TechniqueHitEntity.java:77-80,665-673` | 静帧出生与帧长 | 帧长 1 | 否 |
| `FieldZone.java:18,19,21` | `expireTick/nextUpkeepTick/upkeepPeriod` | 由 `FieldZones` 填 | 否 |
| `FieldZones.java:40,41,184,188` | 寿命/维护周期/绘制/脉冲 | 200 / 20 / 每 16 tick / 每 10 tick | 否 |
| `BarrierDefinition.java:9,12` + `:38` | 寿命/维护周期 | 80 / 20 | 否 |
| `BarrierBody.java:176,180-186` | 到期与维护 | 同上 | 否 |
| `ReleaseDefinitions.java:8-34` | 各术式寿命 | 逐条写死 | 否 |

### 四、场地与屏障的注册、生成入口和两类范围

**场地**：`FieldZones.add` 在给定源码中的唯一调用点是 `FieldSpawner.releaseAttached` 第 51 行，参数来自 `ReleaseDefinition` 第 51-55 行：`fieldRadius`、`fieldLifetimeTicks`、`fieldUpkeepCost`、`fieldUpkeepPeriod`，以及治疗、牵引、潮湿、净化、压力、侵蚀、卸力、减速、终爆。

已注册的 domain 使用者是 `WaterTechniques` 第 33-34 行 `water_mist` 和第 37-38 行 `water_deep_pressure`。但 `WaterReleaseDefinition.domain` 第 104-112 行没有写 `fieldRadius`，该字段默认 0（第 27 行），而 `FieldSpawner.releaseAttached` 第 45 行在 `fieldRadius() <= 0f` 时直接 return。因此，仅凭给定源码不能证明这两个术式会走到 `FieldZones.add`；实际消费应在未提供的 `WaterFormEffect` 中，此处保留为待确认。`FieldZones` 第 51-66 行只是清理旧场地，不是注册。

场地本体范围：`FieldZone.radius` 第 17 行；`FieldZones.coversBody` 第 344-352 行按身体包围盒到锚点的最近距离判定；脉冲查询盒第 215 行 `inflate(zone.radius)`；`FieldTouch.queryBox` 第 403 行 `radius + 1.0`。子效果半径另算：终爆第 301 行 `inflate(max(1.0, radius * 0.45))`，与本体半径不同；脉冲各效果按单份取最大（第 227-245 行），第 203 行注释明确不按场地份数叠加。

**屏障**：`WaterTechniques` 第 31-32 行注册 `water_curtain`；`BarrierDefinition.curtain` 第 37-39 行为宽 3.2、高 2.8、厚 0.25、前置 2.0、寿命 80、生成费 8、维护费 2、维护周期 20、衰减 0.5、偏转开启。`BarrierService.releasePlace` 第 63 行 `bill = spawnCost * count`，第 81 行 `now + lifetimeTicks`，第 82 行 `now + upkeepPeriod`；`BarrierBody.tick` 第 176 行到期、第 180-186 行维护。

屏障本体范围是 `BarrierService.placeAt` 第 147-157 行的 AABB，接触用 `AxisAlignedWall.touches` 第 26-39 行，查询盒第 147 行 `inflate(2.0)`，偏转登记第 100-110 行。屏障没有终爆式子半径。需要注意：`BarrierBody` 第 185 行维护周期读取 `BarrierDefinition.curtain()`，第 89-90 行衰减也读取 `curtain()`，不是实例字段；当前只注册了 `curtain`，所以尚未暴露差异，但自定义定义不会按自身周期和衰减运行。另，第 72-93 行多份墙使用同一个 `wall` 配置，`count > 1` 时是重叠同位置墙体。

### 五、本轮不随能量乘的项

伤害在 `spawnReported` 第 147 行直接取 `frame.damage`，不在体积倍率链路内；`SkillSegmentMath.segmentDamage` 第 106-116 行只叠加武器与属性加成。费用在 `BarrierService` 第 63-69 行、`FieldZones` 第 176-183 行、`SkillSegmentMath.manaCostOf` 第 180-186 行。冷却在 `SkillSegmentMath.scaleCooldown` 第 118-121 行，只吃 `cooldownReduction`。本轮均不随能量乘倍。

`MAGIC` 在给定文件出现在 `WaterTechniques` 第 47、56、70、79、88、97 行；给定源码没有 `NONE` 资格判定，故不推断边界。

### 待确认项

- `ArcBandSnapshot.lifeTicks()` 与 `activeTicks()` 的赋值源不在给定源码，弧带寿命基线无法在此闭合。
- `WaterFormEffect` 是否写入 `fieldRadius`、是否直接调用 `FieldZones.add`，不在给定源码。
- 屏障 `attenuation()` 的实际消费点不在给定源码。
- `MAGIC/NONE` 资格边界由用户确认中，本轮不假定。