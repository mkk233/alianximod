你是独立 Grok 4.7 审查员，只读审查，不修改文件，不调用子代理，不重新调查整仓。以下是本次改动相对任务开始快照的diff和当前文件。只报告可复现真实缺陷，按严重性给路径行号原因修正建议，没问题写PASS。公共路由尚待另组接入不作为本组缺陷。用户期望：能量倍率2/4放大大小和寿命，伤害/速度/费用不乘，剑术不缩放。生命周期模板只缩放一次，客户端与服务端一致，兼容原基线，水牢原预扣/退款/操作号幂等保留。审查生产路径和新测试是否真的验证该路径。

# GPT-6-astra最终公共契约补充

A独占在ReleaseInput实现：
static int expiryTick(int nowTick,int durationTicks)
static boolean hasExpired(int nowTick,int expiryTick)
static long expiryGameTime(long now,long durationTicks)

非法multiplier(非有限或<=0)回1；scaleDuration有限正base以double ceil并钳[1,Integer.MAX_VALUE]，base<=0原样。有限duration范围1..Integer.MAX_VALUE。expiryTick按Java int环绕加法now+duration；hasExpired用now-expiry>=0（int差值）；禁止now>=expiry。调用只针对有限正寿命，不能把无限哨兵送进helper。对象正常逐tick检查到期处理，不能用于超过2^31 tick未检查的历史记录。

B/C必须核对长寿命所有到期入口，包含主tick/living/live/刷新/同步剩余计算，不仅主tick；long游戏时间expiryGameTime在now>Long.MAX_VALUE-duration时返回Long.MAX_VALUE否则相加，比到期保留now>=expiry。实体age到Integer.MAX_VALUE必须先到期退出，不能递增回负。不要顺手改无关维护计时器。

垂丝私有lifetime未同步，C必须增加实例寿命同步使客户端和服务端相位一致。ArcBand剑术固定窗口不改。

# 实体寿命与条件水牢

实例寿命只在生成时读一次 `CastFrame.releaseInput`，写入实体同步数据，不写回模板。垂丝填充和排空仍是 8 tick，排空起点是这一份的总寿命减 8。水牢先按原潮湿档算基准寿命，再乘同一份预约输入；壳从目标包围盒中心按同一倍率放大原外壳和原内盒。

未改 `ReleaseInput`、`CastFrame`、网络路由、领域、护体、屏障、场地和美术。未跑 Gradle，未进游戏。

## 新增签名

```text
TechniqueHitSpec.instanceLifetime(int templateLifetime, ReleaseInput releaseInput, boolean arcBand) -> int
TechniqueHitEntity.applySpec(ServerPlayer caster, TechniqueHitSpec spec, int instanceLifetime)
TechniqueHitEntity.lifetime() -> int
```

`lifetime()` 读同步字段 `XuexiHitLifetime`。`applySpec`、NBT 读回和客户端都走这一个值。弧带安装后仍用快照自己的 `lifeTicks()` 覆盖，不缩放 6+4 窗口，也不改静帧。

```text
WeepingSettings.drainSpan() -> int
WeepingSettings.drainStartFor(int lifeTicks) -> int
WeepingCurtain.flowSegment(..., int age, int lifeTicks) -> WaterSegment
WeepingCurtain.segmentFor(..., int age, int lifeTicks) -> WaterSegment
WaterCurtainLayout.drainStart(int lifeTicks) -> int
WaterCurtainLayout.panels(double curtainWidth, double curtainHeight, int frameIndex, int drainStart) -> List<Panel>
```

旧的六参 `flowSegment` / `segmentFor` 仍用基准寿命 40。贴图 `flowFrame` 仍是每 2 tick 一帧。

```text
ConditionalEffect.attempt(ServerPlayer caster, UUID operationId, EnchantmentSnapshot contact, ReleaseInput input) -> ConditionalResult
ConditionalEffect.attempt(ServerPlayer, UUID, EnchantmentSnapshot) 默认转 BASE
WaterPrisonEffect.attempt(..., ReleaseInput input)
PrisonService.prepare(ServerPlayer, LivingEntity, UUID, ReleaseInput) -> PrisonReservation
PrisonService.commit(PrisonReservation, EnchantmentSnapshot, ReleaseInput) -> PrisonCommitResult
PrisonReservation.commit(EnchantmentSnapshot, ReleaseInput) -> PrisonCommitResult
PrisonReservation.releaseInput() -> ReleaseInput
PrisonRuntime.prepare(..., ReleaseInput) -> PrisonReservation
PrisonRuntime.commit(PrisonReservation, EnchantmentSnapshot, ReleaseInput) -> PrisonCommitResult
PrisonRuntime.bindInput(PrisonReservation existing, ReleaseInput offeredInput) -> ReleaseInput
PrisonRuntime.scaledDuration(int tier, int wetRemaining, ReleaseInput frozenInput) -> int
PrisonShell.expand(AABB body, double sizeMultiplier) -> AABB
PrisonShell.innerFor(AABB body, double sizeMultiplier) -> AABB
```

旧的三参 `prepare` 和两参 `commit` 仍在。`prepare` 转到 `BASE`。预约上的 `commit(contact)` 使用已经冻结的首次输入。

## A 调用方

本组只读 A 的契约，没有改 `ReleaseInput` 或 `CastFrame`。

- `TechniqueHitSpec.spawnReported` 读 `CastFrame.current().releaseInput`。字段缺省是 `BASE`。路由把链倍率写入该字段之后，生成才会放大实例寿命。
- `SkillSegmentRunner` 第 280 行仍调用三参 `attempt(player, operationId, contact)`。接口默认把它转成 `ReleaseInput.BASE`。路由要改成四参，并把同一次释放的 `ReleaseInput` 传进来。在那之前，水牢准备和提交都是基准倍率。
- 命中效果本身不用改。垂丝等效果仍把定义寿命放进模板，`spawn` 读帧上的输入，只缩放这一次实例。

`bindInput` 在已有预约时返回首次输入。同一 `operationId` 再次 `prepare` 直接交回那份预约，不换倍率。操作号、预扣和退款没动。

## 到期

正寿命的实体年龄用 `ReleaseInput.expiryTick(0, lifeTicks)` 和 `hasExpired`。小于等于 0 的寿命仍按原来的 `age >= lifeTicks`，不送进 helper。年龄已经是 `Integer.MAX_VALUE` 时先到期并返回，不再 `age++`。

水牢 `endTick` 用 `ReleaseInput.expiryGameTime(birthTick, durationTicks)`。这些长时刻比较保持 `gameTime >= endTick`，没有改成 int 差值：

- `PrisonRuntime.rebind`
- `PrisonRecord.tick` 的离区标记和在场结算
- `PrisonDeflectionHost.active`（未改文件，原本已是 `>=`）
- `PrisonClientState.purge` 与 `current`（未改文件，原本已是 `>=` / `<`）

`nextPulse` 仍是 20 tick 维护计时，没有乘聚能。统一的 `expiresAt` 安全上限仍等主控，这里没有另加一层。

## 行为边界

- 模板 `lifetime`、`hitInterval`、`hitReadyAge`、`speed` 不因这次缩放被写回。复制份共用同一次结果，不会逐份再乘。
- 体积仍走原来的 `volumeScale` 与定义轴向开关。资格由 A 负责。
- 垂丝 40 / 80 / 160 的排空起点是 32 / 72 / 152。第 8 tick 溅水。服务端水段和客户端 `drainStart` 都读 `WeepingSettings.drainStartFor`。客户端画出的高度仍是服务端已经按该寿命裁好的水段。
- 水牢一档 20、二档 100、三档剩余一半、四档剩余全部，先算出基准再 `scaleDuration`。非正基准原样返回并仍因 `<= 0` 拒绝。潮湿、档位、20 格选目标和单人囚犯不变。
- 壳厚随倍率变。内盒不是把放大后的外壳再 `inflate(-0.20)`。
- 生成水的一格永久方块未改。

## 验证

检查类：

- `src/test/java/com/bslx/XueXiMod/shu/technique/release/entity/ReleaseEntityEnergyCheck.java`
- `src/test/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonEnergyCheck.java`

覆盖生产 `BASE` / `2` / `4`、模板不重复乘、弧带不缩放、垂丝 40 / 80 / 160 相位、壳的 1 / 2 / 4 倍与固定 0.20 反推否定、预约输入幂等、非正寿命保留。

此阶段没有跑 Gradle，所以上面两组 `main` 没有执行。命名门禁在本组改动的文件上没有新违规。未改的 `OriginalHumanoid.java` 仍有两处旧名字：第 165 行 `i`，第 202 行 `to`。


### DIFF src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitSpec.java
diff --git "a/D:/alianximod/docs/\345\274\200\345\217\221\350\256\260\345\275\225/\350\201\232\350\203\275\350\276\223\345\205\245\351\223\276\346\240\270\346\237\245-20261027/before/src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitSpec.java" b/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitSpec.java
index 7c5b08e..2e04d9c 100644
--- "a/D:/alianximod/docs/\345\274\200\345\217\221\350\256\260\345\275\225/\350\201\232\350\203\275\350\276\223\345\205\245\351\223\276\346\240\270\346\237\245-20261027/before/src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitSpec.java"
+++ b/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitSpec.java
@@ -4,6 +4,7 @@ import com.bslx.XueXiMod.command.SkillEntityCount;
 import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
 import com.bslx.XueXiMod.shu.technique.release.CastFrame;
 import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
+import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
 import com.bslx.XueXiMod.shu.technique.release.Trajectory;
 import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
 import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
@@ -239,6 +240,11 @@ public final class TechniqueHitSpec {
         Vec3 sampledEye = arcBand ? caster.getEyePosition() : null;
         Vec3 sampledAim = arcBand ? caster.getLookAngle() : null;
         boolean planeSpread = radialPlaneSpread(spec.arcDraft, placedFrame);
+        ReleaseInput releaseInput = ReleaseInput.BASE;
+        if (frame != null && frame.releaseInput != null) {
+            releaseInput = frame.releaseInput;
+        }
+        int instanceLifetime = instanceLifetime(spec.lifetime, releaseInput, arcBand);
         int accepted = 0;
         for (int spawnIndex = 0; spawnIndex < entityCount; spawnIndex++) {
             if (spec.listenerFactory != null) {
@@ -276,7 +282,7 @@ public final class TechniqueHitSpec {
                 entity.holdActivation();
             }
             try {
-                entity.applySpec(caster, spec);
+                entity.applySpec(caster, spec, instanceLifetime);
             } catch (RuntimeException failure) {
                 if (hostGroup != null) {
                     hostGroup.fail();
@@ -319,6 +325,17 @@ public final class TechniqueHitSpec {
         return new SpawnReport(entityCount, accepted);
     }
 
+    /**
+     * 模板寿命只读一次。弧带窗口不缩放。调用方把结果写进实例，不写回模板。
+     */
+    public static int instanceLifetime(int templateLifetime, ReleaseInput releaseInput, boolean arcBand) {
+        if (arcBand) {
+            return templateLifetime;
+        }
+        ReleaseInput frozenInput = releaseInput == null ? ReleaseInput.BASE : releaseInput;
+        return frozenInput.scaleDuration(templateLifetime);
+    }
+
     /** 按这一帧实际采用的几何，给当前生成号做一份不可变显示记录。 */
     public static CohortVisual cohortFor(CastFrame frame, int spawnIndex, int spawnCount, boolean swordVisual) {
         return cohortFor(frame, spawnIndex, spawnCount, swordVisual, false);

### CURRENT src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitSpec.java
1: package com.bslx.XueXiMod.shu.technique.release.entity;
2: 
3: import com.bslx.XueXiMod.command.SkillEntityCount;
4: import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
5: import com.bslx.XueXiMod.shu.technique.release.CastFrame;
6: import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
7: import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
8: import com.bslx.XueXiMod.shu.technique.release.Trajectory;
9: import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
10: import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
11: import com.bslx.XueXiMod.shu.technique.release.momentum.HeavyWater;
12: import com.bslx.XueXiMod.shu.technique.release.payload.HitPayloads;
13: import com.bslx.XueXiMod.shu.technique.release.payload.WetPressure;
14: import com.bslx.XueXiMod.shu.technique.release.water.ArcBandDraft;
15: import com.bslx.XueXiMod.shu.technique.release.water.ArcBandSnapshot;
16: import com.bslx.XueXiMod.shu.technique.release.water.ArcVec;
17: import com.bslx.XueXiMod.shu.technique.release.water.RadialCopyPlane;
18: import net.minecraft.server.level.ServerLevel;
19: import net.minecraft.server.level.ServerPlayer;
20: import net.minecraft.world.entity.LivingEntity;
21: import net.minecraft.world.phys.Vec3;
22: 
23: import java.util.function.Consumer;
24: 
25: public final class TechniqueHitSpec {
26: 
27:     public enum Motion {
28:         SWING,
29:         PROJECTILE,
30:         DROP,
31:         FOLLOW
32:     }
33: 
34:     public enum Hitbox {
35:         ARC,
36:         OBB,
37:         BOX,
38:         SPHERE
39:     }
40: 
41:     public enum Path {
42:         STILL_AIM,
43:         STRAIGHT,
44:         DROP,
45:         FOLLOW,
46:         VORTEX,
47:         FUNCTION
48:     }
49: 
50:     public interface HitListener {
51:         void onHit(LivingEntity target);
52: 
53:         void onExpire(Vec3 position);
54:     }
55: 
56:     public interface HitListenerFactory {
57:         HitListener create();
58:     }
59: 
60:     public String techniqueId = "";
61:     public Motion motion = Motion.PROJECTILE;
62:     public Hitbox hitbox = Hitbox.BOX;
63:     public int lifetime = 20;
64:     public double speed = 1.0;
65:     public boolean lockThisCast;
66:     public boolean dropThisCast;
67:     public boolean tracking;
68:     public double arcRadius = 2.0;
69:     public double arcDegrees = 120.0;
70:     public double arcHeight = 2.0;
71:     public Vec3 boxSize = new Vec3(1.0, 1.0, 1.0);
72:     public int hitInterval;
73:     /** 服务器年龄达到这个值才开始命中。0 表示生成后就能打到。 */
74:     public int hitReadyAge;
75:     public int visual;
76:     public float modelScale = 1.0F;
77:     public Vec3 start;
78:     public Vec3 anchor;
79:     public Vec3 aim;
80:     public Path path;
81:     public String functionId = "";
82:     public double length = 1.0;
83:     public double width = 1.0;
84:     public double height = 1.0;
85:     public String texture;
86:     public boolean centered;
87:     public boolean pose3d;
88:     public float roll;
89:     public HitListener listener;
90:     public HitListenerFactory listenerFactory;
91:     public float settleDamage = Float.NaN;
92:     public EnchantmentResolution settleEnchantments;
93:     public ReleaseBatch.Tally settleTally;
94:     public int settleCopyIndex;
95:     public int settleBurnSeconds;
96:     public int settleControlTicks;
97:     public boolean settleExtinguish;
98:     public float settleKnockback;
99:     public com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess settleTide =
100:             com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess.NONE;
101:     public Vec3 localOffset;
102:     public boolean trap;
103:     public boolean burstOnImpact;
104:     public boolean blocksMovement;
105:     /** 空表示仍走旧的盒子或弹道。只有声明了弧带的释放会填。 */
106:     public ArcBandDraft arcDraft;
107:     /** 这一份实体冻结后的弧。生成循环里逐份写入，不在份与份之间共用可变对象。 */
108:     public ArcBandSnapshot frozenArc;
109:     /** 这一次生成要抄到实体上的显示序号。循环内逐份换成新记录，结束后回到单份。 */
110:     public CohortVisual cohortVisual = CohortVisual.SINGLE;
111:     /** 静止冻结斩面的单次贴图。默认关闭，调用方显式打开，不在这里识别术式名字。 */
112:     public StillSheetVisual stillSheet = StillSheetVisual.NONE;
113:     /** 实体真正进入世界后通知调用方。不在这里识别术式名字。 */
114:     public Consumer<TechniqueHitEntity> publishedHook;
115: 
116:     public static final class SpawnReport {
117:         private final int requested;
118:         private final int accepted;
119: 
120:         private SpawnReport(int requested, int accepted) {
121:             this.requested = requested;
122:             this.accepted = accepted;
123:         }
124: 
125:         public int requested() {
126:             return requested;
127:         }
128: 
129:         public int accepted() {
130:             return accepted;
131:         }
132: 
133:         public boolean complete() {
134:             return requested > 0 && accepted == requested;
135:         }
136:     }
137: 
138:     public static void spawn(ServerLevel level, ServerPlayer caster, TechniqueHitSpec spec) {
139:         spawnReported(level, caster, spec);
140:     }
141: 
142:     /** 返回这一次调用里实际要求生成和真正进入世界的数量。 */
143:     public static SpawnReport spawnReported(ServerLevel level, ServerPlayer caster, TechniqueHitSpec spec) {
144:         CastFrame frame = CastFrame.current();
145:         boolean arcBand = spec.arcDraft != null;
146:         if (frame != null) {
147:             spec.tracking = frame.tracking && frame.trajectoryMutable;
148:             spec.settleDamage = frame.damage;
149:             spec.settleEnchantments = frame.enchantments;
150:             spec.settleTally = frame.tally;
151:             spec.settleCopyIndex = frame.copyIndex;
152:             float volumeScale = frame.volumeScale();
153:             ReleaseDefinition definition = ReleaseRegistry.get(spec.techniqueId);
154:             boolean scaleLength = definition == null || definition.energyScalesLength();
155:             boolean scaleWidth = definition == null || definition.energyScalesWidth();
156:             boolean scaleHeight = definition == null || definition.energyScalesHeight();
157:             boolean scaleModel = definition == null || definition.energyScalesModel();
158:             if (!arcBand && Math.abs(volumeScale - 1f) > 0.001F) {
159:                 if (scaleLength) {
160:                     spec.length *= volumeScale;
161:                 }
162:                 if (scaleWidth) {
163:                     spec.width *= volumeScale;
164:                 }
165:                 if (scaleHeight) {
166:                     spec.height *= volumeScale;
167:                 }
168:                 if (scaleModel) {
169:                     spec.modelScale *= volumeScale;
170:                 }
171:             }
172:             spec.settleBurnSeconds = frame.burnSeconds;
173:             spec.settleControlTicks = frame.controlTicks;
174:             spec.settleExtinguish = frame.extinguish;
175:             spec.settleKnockback = frame.knockback;
176:             spec.settleTide = frame.tideSuccess == null
177:                     ? com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess.NONE
178:                     : frame.tideSuccess;
179:             if (frame.blocksMovement) {
180:                 spec.blocksMovement = true;
181:             }
182:             if (frame.speedMultiplier > 1.001F) {
183:                 spec.speed *= frame.speedMultiplier;
184:             }
185:             if (!arcBand && frame.overrideGeometry) {
186:                 Path path = spec.path;
187:                 boolean still = path == Path.STILL_AIM || spec.centered || !frame.trajectoryMutable;
188:                 if (frame.trajectoryMutable && frame.pathOverride != null && frame.pathOverride != Trajectory.STRAIGHT) {
189:                     spec.dropThisCast = frame.pathOverride == Trajectory.DROP;
190:                 }
191:                 if (frame.trajectoryMutable && frame.pathOverride == Trajectory.VORTEX) {
192:                     spec.path = Path.VORTEX;
193:                     spec.start = frame.origin != null ? frame.origin : caster.getEyePosition();
194:                     spec.aim = frame.look;
195:                 } else if (frame.drop && frame.trajectoryMutable) {
196:                     spec.path = Path.DROP;
197:                     spec.start = frame.origin != null ? frame.origin : caster.getEyePosition();
198:                     spec.aim = frame.look;
199:                 } else if (path == Path.FUNCTION) {
200:                     // 不可改弹道的水帘也要吃落点和交错，只搬生成位置，不改成斩击锚点。
201:                     Vec3 locked = frame.origin != null ? frame.origin : frame.anchor;
202:                     spec.start = locked != null ? locked : caster.getEyePosition();
203:                     if (frame.look != null) {
204:                         spec.aim = frame.look;
205:                     }
206:                 } else if (still) {
207:                     spec.centered = true;
208:                     spec.anchor = frame.origin != null ? frame.origin : frame.anchor;
209:                     spec.aim = frame.planeNormal != null ? frame.planeNormal : frame.look;
210:                     spec.roll = frame.roll;
211:                     Vec3 normal = frame.planeNormal != null ? frame.planeNormal : frame.look;
212:                     spec.pose3d = frame.pose3d
213:                             || (normal != null && Math.abs(normal.y) > 0.2)
214:                             || Math.abs(frame.roll) > 1.0;
215:                 } else if (path == Path.STRAIGHT || path == Path.DROP || path == Path.FOLLOW) {
216:                     spec.start = frame.origin != null ? frame.origin : caster.getEyePosition();
217:                     if (frame.look != null) {
218:                         spec.aim = frame.look;
219:                     }
220:                 }
221:             }
222:         }
223:         if (spec.localOffset != null) {
224:             if (spec.start != null) {
225:                 spec.start = spec.start.add(spec.localOffset);
226:             } else if (spec.anchor != null) {
227:                 spec.anchor = spec.anchor.add(spec.localOffset);
228:             } else {
229:                 spec.start = caster.getEyePosition().add(spec.localOffset);
230:             }
231:         }
232:         int baseCopy = spec.settleCopyIndex;
233:         HostGroupContext hostGroup = HostGroupContext.current();
234:         int entityCount = hostGroup == null ? SkillEntityCount.entityCount() : hostGroup.spawnCount();
235:         ReleaseDefinition cohortDefinition = ReleaseRegistry.get(spec.techniqueId);
236:         boolean swordVisual = cohortDefinition != null && cohortDefinition.separatesCohortVisuals();
237:         Vec3 bakedStart = spec.start;
238:         Vec3 bakedAim = spec.aim;
239:         CastFrame placedFrame = frame;
240:         Vec3 sampledEye = arcBand ? caster.getEyePosition() : null;
241:         Vec3 sampledAim = arcBand ? caster.getLookAngle() : null;
242:         boolean planeSpread = radialPlaneSpread(spec.arcDraft, placedFrame);
243:         ReleaseInput releaseInput = ReleaseInput.BASE;
244:         if (frame != null && frame.releaseInput != null) {
245:             releaseInput = frame.releaseInput;
246:         }
247:         int instanceLifetime = instanceLifetime(spec.lifetime, releaseInput, arcBand);
248:         int accepted = 0;
249:         for (int spawnIndex = 0; spawnIndex < entityCount; spawnIndex++) {
250:             if (spec.listenerFactory != null) {
251:                 spec.listener = spec.listenerFactory.create();
252:             }
253:             spec.settleCopyIndex = CohortVisual.settleEncoding(baseCopy, spawnIndex);
254:             assignCohort(spec, placedFrame, spawnIndex, entityCount, swordVisual, planeSpread);
255:             if (placedFrame != null && placedFrame.pathOverride == Trajectory.VORTEX && placedFrame.trajectoryMutable) {
256:                 spec.path = Path.VORTEX;
257:                 spec.dropThisCast = false;
258:                 spec.start = placedFrame.anchor != null ? placedFrame.anchor : placedFrame.origin;
259:                 spec.aim = placedFrame.vortexPhase(spawnIndex);
260:             } else if (placedFrame != null && placedFrame.volumeHalf > 0.0 && placedFrame.trajectoryMutable && placedFrame.anchor != null) {
261:                 spec.start = placedFrame.volumePoint(spawnIndex);
262:                 if (spec.localOffset != null) {
263:                     spec.start = spec.start.add(spec.localOffset);
264:                 }
265:                 spec.aim = placedFrame.aimFor(spawnIndex);
266:                 if (placedFrame.drop) {
267:                     spec.path = Path.DROP;
268:                     spec.dropThisCast = true;
269:                 }
270:             }
271:             if (arcBand) {
272:                 spec.frozenArc = bakeArc(spec, placedFrame, sampledEye, sampledAim);
273:                 if (spec.frozenArc == null) {
274:                     if (hostGroup != null) {
275:                         hostGroup.fail();
276:                     }
277:                     continue;
278:                 }
279:             }
280:             TechniqueHitEntity entity = new TechniqueHitEntity(ReleaseEntities.TECHNIQUE_HIT.get(), level);
281:             if (hostGroup != null) {
282:                 entity.holdActivation();
283:             }
284:             try {
285:                 entity.applySpec(caster, spec, instanceLifetime);
286:             } catch (RuntimeException failure) {
287:                 if (hostGroup != null) {
288:                     hostGroup.fail();
289:                 }
290:                 HitPayloads.clear(entity);
291:                 WetPressure.clear(entity);
292:                 continue;
293:             }
294:             HitPayloads.attach(entity, placedFrame);
295:             ReleaseDefinition spawnedDefinition = ReleaseRegistry.get(spec.techniqueId);
296:             if (spawnedDefinition != null && spawnedDefinition.pressureWetnessMinimum() > 0 && placedFrame != null) {
297:                 WetPressure.attach(entity, placedFrame.releaseGroup, placedFrame.heavyWater,
298:                         spawnedDefinition.pressureWetnessMinimum());
299:             }
300:             boolean published = level.addFreshEntity(entity);
301:             if (!published) {
302:                 HitPayloads.clear(entity);
303:                 WetPressure.clear(entity);
304:                 if (hostGroup != null) {
305:                     hostGroup.fail();
306:                 }
307:                 continue;
308:             }
309:             accepted++;
310:             if (spec.publishedHook != null) {
311:                 spec.publishedHook.accept(entity);
312:             }
313:             if (hostGroup != null) {
314:                 hostGroup.stagePublished(entity);
315:             } else {
316:                 entity.stampStillBirth();
317:                 HeavyWater.commitFirst(caster);
318:             }
319:         }
320:         spec.settleCopyIndex = baseCopy;
321:         spec.start = bakedStart;
322:         spec.aim = bakedAim;
323:         spec.frozenArc = null;
324:         releaseCohort(spec);
325:         return new SpawnReport(entityCount, accepted);
326:     }
327: 
328:     /**
329:      * 模板寿命只读一次。弧带窗口不缩放。调用方把结果写进实例，不写回模板。
330:      */
331:     public static int instanceLifetime(int templateLifetime, ReleaseInput releaseInput, boolean arcBand) {
332:         if (arcBand) {
333:             return templateLifetime;
334:         }
335:         ReleaseInput frozenInput = releaseInput == null ? ReleaseInput.BASE : releaseInput;
336:         return frozenInput.scaleDuration(templateLifetime);
337:     }
338: 
339:     /** 按这一帧实际采用的几何，给当前生成号做一份不可变显示记录。 */
340:     public static CohortVisual cohortFor(CastFrame frame, int spawnIndex, int spawnCount, boolean swordVisual) {
341:         return cohortFor(frame, spawnIndex, spawnCount, swordVisual, false);
342:     }
343: 
344:     /**
345:      * {@code planeSpread} 只写进这一份的视觉记录，不改共享帧。
346:      * 复制轴散开后，同一复制号里的生成倍率仍按生成号分槽。
347:      */
348:     public static CohortVisual cohortFor(CastFrame frame, int spawnIndex, int spawnCount, boolean swordVisual,
349:                                          boolean planeSpread) {
350:         int copyCount = 1;
351:         int copyIndex = 0;
352:         if (frame != null) {
353:             copyCount = frame.copyCount < 1 ? 1 : frame.copyCount;
354:             copyIndex = frame.copyIndex;
355:         }
356:         return CohortVisual.capture(copyIndex, copyCount, spawnIndex, spawnCount, swordVisual,
357:                 copyDispersed(frame) || planeSpread, spawnDispersed(frame));
358:     }
359: 
360:     /** 这一份的生成点或刀面已经按复制号分开。接触点改回同一锚点后不再算。 */
361:     public static boolean copyDispersed(CastFrame frame) {
362:         if (frame == null) {
363:             return false;
364:         }
365:         if (spawnDispersed(frame)) {
366:             return true;
367:         }
368:         return frame.cohortDispersed;
369:     }
370: 
371:     /** 只看生成循环里真正按生成号改位置或方向的分支，不从复制散开标志推断。 */
372:     public static boolean spawnDispersed(CastFrame frame) {
373:         if (frame == null) {
374:             return false;
375:         }
376:         if (frame.pathOverride == Trajectory.VORTEX && frame.trajectoryMutable) {
377:             return true;
378:         }
379:         return frame.volumeHalf > 0.0 && frame.trajectoryMutable && frame.anchor != null;
380:     }
381: 
382:     public static void assignCohort(TechniqueHitSpec spec, CastFrame frame, int spawnIndex, int spawnCount, boolean swordVisual) {
383:         assignCohort(spec, frame, spawnIndex, spawnCount, swordVisual, false);
384:     }
385: 
386:     public static void assignCohort(TechniqueHitSpec spec, CastFrame frame, int spawnIndex, int spawnCount,
387:                                     boolean swordVisual, boolean planeSpread) {
388:         spec.cohortVisual = cohortFor(frame, spawnIndex, spawnCount, swordVisual, planeSpread);
389:     }
390: 
391:     /** 交错、落点和已经改过刀面的路径保持原轨迹。只搬锚点的覆写不算已经散开。 */
392:     static boolean radialCopiesAlreadySpread(CastFrame frame) {
393:         if (frame == null) {
394:             return false;
395:         }
396:         if (copyDispersed(frame)) {
397:             return true;
398:         }
399:         return frame.pose3d || Math.abs(frame.roll) > 1.0F;
400:     }
401: 
402:     static boolean radialPlaneSpread(ArcBandDraft draft, CastFrame frame) {
403:         if (draft == null || frame == null) {
404:             return false;
405:         }
406:         return RadialCopyPlane.spreads(draft.mode(), frame.copyCount, radialCopiesAlreadySpread(frame));
407:     }
408: 
409:     public static void releaseCohort(TechniqueHitSpec spec) {
410:         spec.cohortVisual = CohortVisual.SINGLE;
411:     }
412: 
413:     /** 覆写过的原点和瞄准原样进入快照。没有覆写时，眼位和朝向只用循环外的那一次采样。 */
414:     static ArcBandSnapshot bakeArc(TechniqueHitSpec spec, CastFrame frame, Vec3 sampledEye, Vec3 sampledAim) {
415:         boolean framedOrigin = frame != null && frame.overrideGeometry && frame.origin != null;
416:         Vec3 origin = framedOrigin ? frame.origin : (spec.start != null ? spec.start : sampledEye);
417:         if (origin == null) {
418:             return null;
419:         }
420:         if (framedOrigin && spec.localOffset != null) {
421:             origin = origin.add(spec.localOffset);
422:         }
423:         boolean scatterFacing = frame != null && (frame.pose3d || Math.abs(frame.roll) > 1.0F);
424:         Vec3 facing = scatterFacing && frame.planeNormal != null && frame.planeNormal.lengthSqr() > 1.0E-8
425:                 ? frame.planeNormal
426:                 : (frame != null && frame.look != null && frame.look.lengthSqr() > 1.0E-8 ? frame.look : spec.aim);
427:         if (facing == null || facing.lengthSqr() <= 1.0E-8) {
428:             facing = sampledAim;
429:         }
430:         if (facing == null) {
431:             return null;
432:         }
433:         double extraRoll = frame == null ? 0.0 : frame.roll;
434:         double scale = frame == null ? 1.0 : frame.volumeScale();
435:         int beat = spec.arcDraft.beat();
436:         ArcVec frozenOrigin = new ArcVec(origin.x, origin.y, origin.z);
437:         ArcVec frozenFacing = new ArcVec(facing.x, facing.y, facing.z);
438:         if (frame != null && frame.copyIndex > 0 && radialPlaneSpread(spec.arcDraft, frame)) {
439:             RadialCopyPlane.Placement placement = RadialCopyPlane.place(frame.copyIndex, frame.copyCount, beat, extraRoll);
440:             return ArcBandSnapshot.freezeLook(
441:                     frozenOrigin,
442:                     frozenFacing,
443:                     null,
444:                     false,
445:                     placement.extraRollDegrees(beat),
446:                     beat,
447:                     spec.arcDraft.baseRadius(),
448:                     spec.arcDraft.baseHalfWidth(),
449:                     scale,
450:                     spec.arcDraft.mode(),
451:                     placement.sectorStartDegrees(),
452:                     placement.sectorSweepDegrees());
453:         }
454:         return ArcBandSnapshot.freezeLook(
455:                 frozenOrigin,
456:                 frozenFacing,
457:                 null,
458:                 false,
459:                 extraRoll,
460:                 beat,
461:                 spec.arcDraft.baseRadius(),
462:                 spec.arcDraft.baseHalfWidth(),
463:                 scale,
464:                 spec.arcDraft.mode());
465:     }
466: }
467: 

### DIFF src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitEntity.java
diff --git "a/D:/alianximod/docs/\345\274\200\345\217\221\350\256\260\345\275\225/\350\201\232\350\203\275\350\276\223\345\205\245\351\223\276\346\240\270\346\237\245-20261027/before/src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitEntity.java" b/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitEntity.java
index 697da2d..1da70bf 100644
--- "a/D:/alianximod/docs/\345\274\200\345\217\221\350\256\260\345\275\225/\350\201\232\350\203\275\350\276\223\345\205\245\351\223\276\346\240\270\346\237\245-20261027/before/src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitEntity.java"
+++ b/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitEntity.java
@@ -8,6 +8,7 @@ import com.bslx.XueXiMod.shu.technique.release.parting.WaterPartingService;
 import com.bslx.XueXiMod.shu.technique.release.payload.ObstacleBodies;
 import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
 import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
+import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
 import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
 import com.bslx.XueXiMod.shu.technique.release.WeepingCurtain;
 import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHosts;
@@ -19,6 +20,7 @@ import com.bslx.XueXiMod.shu.technique.release.water.ArcBounds;
 import com.bslx.XueXiMod.shu.technique.release.water.ProgressArcBand;
 import net.minecraft.core.particles.ParticleTypes;
 import net.minecraft.nbt.CompoundTag;
+import net.minecraft.nbt.Tag;
 import net.minecraft.network.syncher.EntityDataAccessor;
 import net.minecraft.network.syncher.EntityDataSerializers;
 import net.minecraft.network.syncher.SynchedEntityData;
@@ -80,6 +82,9 @@ public class TechniqueHitEntity extends Entity {
             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.LONG);
     private static final EntityDataAccessor<Boolean> DATA_STILL_CANCELLED =
             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.BOOLEAN);
+    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
+            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
+    private static final String LIFETIME_KEY = "XuexiHitLifetime";
 
     @Nullable
     private UUID casterUuid;
@@ -171,6 +176,7 @@ public class TechniqueHitEntity extends Entity {
         this.entityData.define(DATA_STILL_FRAME_TICKS, 1);
         this.entityData.define(DATA_STILL_BIRTH, StillSheetVisual.UNBORN);
         this.entityData.define(DATA_STILL_CANCELLED, false);
+        this.entityData.define(DATA_LIFETIME, 0);
     }
 
     @Override
@@ -196,10 +202,14 @@ public class TechniqueHitEntity extends Entity {
 
     @Override
     protected void readAdditionalSaveData(CompoundTag tag) {
+        if (tag.contains(LIFETIME_KEY, Tag.TAG_INT)) {
+            this.storeLifetime(tag.getInt(LIFETIME_KEY));
+        }
     }
 
     @Override
     protected void addAdditionalSaveData(CompoundTag tag) {
+        tag.putInt(LIFETIME_KEY, this.lifetime());
     }
 
     @Override
@@ -265,6 +275,10 @@ public class TechniqueHitEntity extends Entity {
             return;
         }
         this.previousPosition = this.position();
+        if (this.age == Integer.MAX_VALUE) {
+            TechniqueHitSettle.onExpire(this, level);
+            return;
+        }
         this.age++;
         if (this.motion != null) {
             this.motion.tick(this, level);
@@ -307,7 +321,15 @@ public class TechniqueHitEntity extends Entity {
                 return;
             }
         }
-        if (this.age >= this.lifetime) {
+        int lifeTicks = this.lifetime();
+        boolean lifeEnded;
+        if (lifeTicks <= 0) {
+            lifeEnded = this.age >= lifeTicks;
+        } else {
+            int expiryTick = ReleaseInput.expiryTick(0, lifeTicks);
+            lifeEnded = ReleaseInput.hasExpired(this.age, expiryTick);
+        }
+        if (lifeEnded) {
             TechniqueHitSettle.onExpire(this, level);
         }
     }
@@ -354,6 +376,10 @@ public class TechniqueHitEntity extends Entity {
     }
 
     public void applySpec(ServerPlayer caster, TechniqueHitSpec spec) {
+        this.applySpec(caster, spec, spec.lifetime);
+    }
+
+    public void applySpec(ServerPlayer caster, TechniqueHitSpec spec, int instanceLifetime) {
         CohortVisual shown = spec.cohortVisual == null ? CohortVisual.SINGLE : spec.cohortVisual.stored();
         this.entityData.set(DATA_DISPLAY_INDEX, shown.displayIndex());
         this.entityData.set(DATA_DISPLAY_COUNT, shown.displayCount());
@@ -364,7 +390,7 @@ public class TechniqueHitEntity extends Entity {
         ReleaseDefinition boundDefinition = ReleaseRegistry.get(spec.techniqueId);
         this.deflectsIncoming = boundDefinition != null && boundDefinition.deflectsProjectiles();
         this.persistentFlow = boundDefinition != null && boundDefinition.persistentFlowSettlement();
-        this.lifetime = spec.lifetime;
+        this.storeLifetime(instanceLifetime);
         this.speed = spec.speed;
         this.lockThisCast = spec.lockThisCast;
         this.dropThisCast = spec.dropThisCast;
@@ -478,7 +504,12 @@ public class TechniqueHitEntity extends Entity {
     }
 
     public int lifetime() {
-        return this.lifetime;
+        return this.entityData.get(DATA_LIFETIME);
+    }
+
+    private void storeLifetime(int instanceLifetime) {
+        this.lifetime = instanceLifetime;
+        this.entityData.set(DATA_LIFETIME, instanceLifetime);
     }
 
     public double speed() {
@@ -728,7 +759,7 @@ public class TechniqueHitEntity extends Entity {
         this.forward = new Vec3(snapshot.forwardAxis().east(), snapshot.forwardAxis().above(), snapshot.forwardAxis().south());
         this.origin = new Vec3(snapshot.origin().east(), snapshot.origin().above(), snapshot.origin().south());
         this.placeAt(this.origin, this.forward);
-        this.lifetime = snapshot.lifeTicks();
+        this.storeLifetime(snapshot.lifeTicks());
     }
 
     private void adoptArc(ArcBandSnapshot snapshot) {

### CURRENT src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitEntity.java
1: package com.bslx.XueXiMod.shu.technique.release.entity;
2: 
3: import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
4: import com.bslx.XueXiMod.shu.technique.release.hitbox.TechniqueHitbox;
5: import com.bslx.XueXiMod.shu.technique.release.hitbox.SizedBoxHitbox;
6: import com.bslx.XueXiMod.shu.technique.release.motion.TechniqueMotion;
7: import com.bslx.XueXiMod.shu.technique.release.parting.WaterPartingService;
8: import com.bslx.XueXiMod.shu.technique.release.payload.ObstacleBodies;
9: import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
10: import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
11: import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
12: import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
13: import com.bslx.XueXiMod.shu.technique.release.WeepingCurtain;
14: import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHosts;
15: import com.bslx.XueXiMod.shu.technique.release.deflection.hosts.ArcDeflectionHost;
16: import com.bslx.XueXiMod.shu.technique.release.deflection.hosts.WeepingDeflectionBinding;
17: import com.bslx.XueXiMod.shu.technique.release.water.ArcBandMode;
18: import com.bslx.XueXiMod.shu.technique.release.water.ArcBandSnapshot;
19: import com.bslx.XueXiMod.shu.technique.release.water.ArcBounds;
20: import com.bslx.XueXiMod.shu.technique.release.water.ProgressArcBand;
21: import net.minecraft.core.particles.ParticleTypes;
22: import net.minecraft.nbt.CompoundTag;
23: import net.minecraft.nbt.Tag;
24: import net.minecraft.network.syncher.EntityDataAccessor;
25: import net.minecraft.network.syncher.EntityDataSerializers;
26: import net.minecraft.network.syncher.SynchedEntityData;
27: import net.minecraft.server.MinecraftServer;
28: import net.minecraft.server.level.ServerLevel;
29: import net.minecraft.server.level.ServerPlayer;
30: import net.minecraft.world.entity.Entity;
31: import net.minecraft.world.entity.EntityType;
32: import net.minecraft.world.entity.LivingEntity;
33: import net.minecraft.world.level.ClipContext;
34: import net.minecraft.world.level.Level;
35: import net.minecraft.world.phys.AABB;
36: import net.minecraft.world.phys.BlockHitResult;
37: import net.minecraft.world.phys.HitResult;
38: import net.minecraft.world.phys.Vec3;
39: 
40: import javax.annotation.Nullable;
41: import java.util.HashMap;
42: import java.util.HashSet;
43: import java.util.List;
44: import java.util.Map;
45: import java.util.Set;
46: import java.util.UUID;
47: 
48: public class TechniqueHitEntity extends Entity {
49:     private static final EntityDataAccessor<String> DATA_TECHNIQUE_ID =
50:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.STRING);
51:     private static final EntityDataAccessor<Integer> DATA_VISUAL =
52:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
53:     private static final EntityDataAccessor<Float> DATA_MODEL_SCALE =
54:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);
55:     private static final EntityDataAccessor<String> DATA_TEXTURE =
56:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.STRING);
57:     private static final EntityDataAccessor<Float> DATA_LENGTH =
58:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);
59:     private static final EntityDataAccessor<Float> DATA_WIDTH =
60:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);
61:     private static final EntityDataAccessor<Float> DATA_HEIGHT =
62:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);
63:     private static final EntityDataAccessor<Float> DATA_ROLL =
64:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);
65:     private static final EntityDataAccessor<String> DATA_ARC_SNAPSHOT =
66:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.STRING);
67:     private static final EntityDataAccessor<Integer> DATA_DISPLAY_INDEX =
68:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
69:     private static final EntityDataAccessor<Integer> DATA_DISPLAY_COUNT =
70:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
71:     private static final EntityDataAccessor<Boolean> DATA_DISPLAY_LANE =
72:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.BOOLEAN);
73:     private static final EntityDataAccessor<Integer> DATA_LANE_INDEX =
74:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
75:     private static final EntityDataAccessor<Integer> DATA_LANE_COUNT =
76:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
77:     private static final EntityDataAccessor<Integer> DATA_STILL_FRAMES =
78:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
79:     private static final EntityDataAccessor<Integer> DATA_STILL_FRAME_TICKS =
80:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
81:     private static final EntityDataAccessor<Long> DATA_STILL_BIRTH =
82:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.LONG);
83:     private static final EntityDataAccessor<Boolean> DATA_STILL_CANCELLED =
84:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.BOOLEAN);
85:     private static final EntityDataAccessor<Integer> DATA_LIFETIME =
86:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
87:     private static final String LIFETIME_KEY = "XuexiHitLifetime";
88: 
89:     @Nullable
90:     private UUID casterUuid;
91:     private boolean deflectsIncoming;
92:     /** 持续接触结算。只从释放定义抄来，客户端不登记瀑布宿主。 */
93:     boolean persistentFlow;
94:     private ArcDeflectionHost arcDeflection;
95:     private TechniqueMotion motion;
96:     private TechniqueHitbox hitbox;
97:     private int lifetime;
98:     private double speed;
99:     private boolean lockThisCast;
100:     private boolean dropThisCast;
101:     private boolean centered;
102:     private boolean pose3d;
103:     private boolean beam;
104:     private boolean pinned;
105:     private boolean trap;
106:     boolean burstOnImpact;
107:     private boolean planted;
108:     /** 盒子高度就是当前水体，不再保底成一格。 */
109:     private boolean exactVolume;
110:     /** 宿主组尚未整组成功时不结算伤害。旧路径保持关闭。 */
111:     private boolean pendingActivation;
112:     @Nullable
113:     private ArcBandSnapshot arcSnapshot;
114:     private double lastProcessedProgress;
115:     private boolean arcDamageClosed;
116:     boolean detonated;
117:     private final ClientTrack clientTrack = new ClientTrack();
118:     int settleControlTicks;
119:     boolean settleExtinguish;
120:     float settleKnockback;
121:     private double arcRadius;
122:     private double arcDegrees;
123:     private double arcHeight;
124:     private Vec3 boxSize = Vec3.ZERO;
125:     private double length = 1.0;
126:     private double width = 1.0;
127:     private double height = 1.0;
128:     int hitInterval;
129:     int hitReadyAge;
130:     @Nullable
131:     TechniqueHitSpec.HitListener listener;
132:     private int age;
133:     /** 就绪年龄只通知一次。不在这里识别术式名字。 */
134:     private boolean partingNotified;
135:     private Vec3 origin = Vec3.ZERO;
136:     Vec3 forward = new Vec3(0.0, 0.0, 1.0);
137:     private String functionId = "";
138:     private Vec3 previousPosition = Vec3.ZERO;
139:     @Nullable
140:     private Vec3 anchor;
141:     @Nullable
142:     private Vec3 aim;
143:     final Set<UUID> hitOnce = new HashSet<>();
144:     final Map<UUID, Long> lastHitTick = new HashMap<>();
145:     float settleDamage = Float.NaN;
146:     EnchantmentResolution settleEnchantments;
147:     ReleaseBatch.Tally settleTally;
148:     int settleCopyIndex;
149:     int settleBurnSeconds;
150:     com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess settleTide =
151:             com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess.NONE;
152: 
153:     public TechniqueHitEntity(EntityType<? extends TechniqueHitEntity> type, Level level) {
154:         super(type, level);
155:         this.noPhysics = true;
156:         this.setNoGravity(true);
157:     }
158: 
159:     @Override
160:     protected void defineSynchedData() {
161:         this.entityData.define(DATA_TECHNIQUE_ID, "");
162:         this.entityData.define(DATA_VISUAL, 0);
163:         this.entityData.define(DATA_MODEL_SCALE, 1.0F);
164:         this.entityData.define(DATA_TEXTURE, "");
165:         this.entityData.define(DATA_LENGTH, 1.0F);
166:         this.entityData.define(DATA_WIDTH, 1.0F);
167:         this.entityData.define(DATA_HEIGHT, 1.0F);
168:         this.entityData.define(DATA_ROLL, 0.0F);
169:         this.entityData.define(DATA_ARC_SNAPSHOT, "");
170:         this.entityData.define(DATA_DISPLAY_INDEX, 0);
171:         this.entityData.define(DATA_DISPLAY_COUNT, 1);
172:         this.entityData.define(DATA_DISPLAY_LANE, false);
173:         this.entityData.define(DATA_LANE_INDEX, 0);
174:         this.entityData.define(DATA_LANE_COUNT, 1);
175:         this.entityData.define(DATA_STILL_FRAMES, 0);
176:         this.entityData.define(DATA_STILL_FRAME_TICKS, 1);
177:         this.entityData.define(DATA_STILL_BIRTH, StillSheetVisual.UNBORN);
178:         this.entityData.define(DATA_STILL_CANCELLED, false);
179:         this.entityData.define(DATA_LIFETIME, 0);
180:     }
181: 
182:     @Override
183:     protected AABB makeBoundingBox() {
184:         AABB arcBox = this.arcWorldBox();
185:         return arcBox != null ? arcBox : super.makeBoundingBox();
186:     }
187: 
188:     @Override
189:     public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
190:         super.onSyncedDataUpdated(key);
191:         if (DATA_ARC_SNAPSHOT.equals(key)) {
192:             ArcBandSnapshot unpacked = ArcBandSnapshot.unpack(this.entityData.get(DATA_ARC_SNAPSHOT));
193:             if (unpacked != null) {
194:                 this.arcSnapshot = unpacked;
195:                 AABB arcBox = this.arcWorldBox();
196:                 if (arcBox != null) {
197:                     this.setBoundingBox(arcBox);
198:                 }
199:             }
200:         }
201:     }
202: 
203:     @Override
204:     protected void readAdditionalSaveData(CompoundTag tag) {
205:         if (tag.contains(LIFETIME_KEY, Tag.TAG_INT)) {
206:             this.storeLifetime(tag.getInt(LIFETIME_KEY));
207:         }
208:     }
209: 
210:     @Override
211:     protected void addAdditionalSaveData(CompoundTag tag) {
212:         tag.putInt(LIFETIME_KEY, this.lifetime());
213:     }
214: 
215:     @Override
216:     public boolean isPickable() {
217:         return false;
218:     }
219: 
220:     @Override
221:     public boolean isPushable() {
222:         return false;
223:     }
224: 
225:     @Override
226:     public boolean canBeCollidedWith() {
227:         return ObstacleBodies.blocks(this);
228:     }
229: 
230:     @Override
231:     public boolean canCollideWith(Entity other) {
232:         return false;
233:     }
234: 
235:     @Override
236:     public boolean shouldBeSaved() {
237:         return false;
238:     }
239: 
240:     @Override
241:     public void lerpTo(double targetX, double targetY, double targetZ, float yaw, float pitch, int steps, boolean teleport) {
242:         if (this.level().isClientSide()) {
243:             this.clientTrack.onClientMove(this, targetX, targetY, targetZ, yaw, pitch);
244:             return;
245:         }
246:         super.lerpTo(targetX, targetY, targetZ, yaw, pitch, steps, teleport);
247:     }
248: 
249:     @Override
250:     public void moveTo(double targetX, double targetY, double targetZ, float yaw, float pitch) {
251:         if (this.level().isClientSide()) {
252:             this.clientTrack.onClientMove(this, targetX, targetY, targetZ, yaw, pitch);
253:             return;
254:         }
255:         super.moveTo(targetX, targetY, targetZ, yaw, pitch);
256:     }
257: 
258:     @Override
259:     public void tick() {
260:         super.tick();
261:         if (this.level().isClientSide()) {
262:             this.clientTrack.afterClientTick(this);
263:             if (this.hasArcBand()) {
264:                 return;
265:             }
266:             this.clientDecor();
267:             return;
268:         }
269:         if (this.pendingActivation) {
270:             return;
271:         }
272:         ServerLevel level = (ServerLevel) this.level();
273:         if (this.arcSnapshot != null) {
274:             this.tickArcBand(level);
275:             return;
276:         }
277:         this.previousPosition = this.position();
278:         if (this.age == Integer.MAX_VALUE) {
279:             TechniqueHitSettle.onExpire(this, level);
280:             return;
281:         }
282:         this.age++;
283:         if (this.motion != null) {
284:             this.motion.tick(this, level);
285:         }
286:         this.refreshWeepingDeflection();
287:         if (!this.partingNotified && this.age >= this.hitReadyAge) {
288:             this.partingNotified = true;
289:             WaterPartingService.activated(this);
290:         }
291:         if (this.centered) {
292:             this.previousPosition = this.position();
293:         }
294:         if (this.trap) {
295:             if (this.planted && !this.detonated) {
296:                 AABB box = this.getBoundingBox().inflate(0.3);
297:                 List<LivingEntity> stepped = level.getEntitiesOfClass(LivingEntity.class, box,
298:                         entity -> TechniqueHitSettle.canHit(this, entity));
299:                 if (!stepped.isEmpty()) {
300:                     TechniqueHitSettle.detonate(this, level);
301:                 }
302:             }
303:         } else if (this.hitbox != null && this.age >= this.hitReadyAge) {
304:             AABB broad = this.hitbox.broadPhase(this);
305:             List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, broad,
306:                     entity -> TechniqueHitSettle.canHit(this, entity));
307:             for (LivingEntity target : targets) {
308:                 if (this.hitbox.contains(this, target)) {
309:                     TechniqueHitSettle.tryHit(this, level, target);
310:                 }
311:             }
312:         }
313:         if (this.burstOnImpact && !this.isRemoved() && this.previousPosition != null) {
314:             BlockHitResult blockHit = level.clip(new ClipContext(
315:                     this.previousPosition, this.position(),
316:                     ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
317:             if (blockHit.getType() == HitResult.Type.BLOCK) {
318:                 Vec3 hitPoint = blockHit.getLocation();
319:                 this.setPos(hitPoint.x, hitPoint.y, hitPoint.z);
320:                 TechniqueHitSettle.onExpire(this, level);
321:                 return;
322:             }
323:         }
324:         int lifeTicks = this.lifetime();
325:         boolean lifeEnded;
326:         if (lifeTicks <= 0) {
327:             lifeEnded = this.age >= lifeTicks;
328:         } else {
329:             int expiryTick = ReleaseInput.expiryTick(0, lifeTicks);
330:             lifeEnded = ReleaseInput.hasExpired(this.age, expiryTick);
331:         }
332:         if (lifeEnded) {
333:             TechniqueHitSettle.onExpire(this, level);
334:         }
335:     }
336: 
337:     /** 粒子只在客户端逻辑拍生成，避免渲染帧里按光束长度狂撒。 */
338:     private void clientDecor() {
339:         int visual = this.getVisual();
340:         if (visual == 4) {
341:             this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY() + 0.2, this.getZ(), 0.0, 0.01, 0.0);
342:         } else if (visual == 3 && (this.tickCount & 7) == 0) {
343:             this.level().addParticle(ParticleTypes.CRIMSON_SPORE, this.getX(), this.getY() + 0.2, this.getZ(), 0.0, 0.02, 0.0);
344:         }
345:     }
346: 
347:     public void holdActivation() {
348:         this.pendingActivation = true;
349:     }
350: 
351:     public void releaseActivation() {
352:         this.pendingActivation = false;
353:         if (this.arcSnapshot != null && this.arcSnapshot.birthTick() < 0L && !this.level().isClientSide()) {
354:             this.adoptArc(this.arcSnapshot.withBirth(this.level().getGameTime()));
355:             if (this.level() instanceof ServerLevel serverLevel) {
356:                 this.refreshArcDeflection(serverLevel, 0L);
357:             }
358:         }
359:         this.stampStillBirth();
360:     }
361: 
362:     public boolean hasArcBand() {
363:         if (this.arcSnapshot != null) {
364:             return true;
365:         }
366:         String packed = this.entityData.get(DATA_ARC_SNAPSHOT);
367:         return packed != null && !packed.isEmpty();
368:     }
369: 
370:     @Nullable
371:     public ArcBandSnapshot arcBand() {
372:         if (this.arcSnapshot == null) {
373:             this.arcSnapshot = ArcBandSnapshot.unpack(this.entityData.get(DATA_ARC_SNAPSHOT));
374:         }
375:         return this.arcSnapshot;
376:     }
377: 
378:     public void applySpec(ServerPlayer caster, TechniqueHitSpec spec) {
379:         this.applySpec(caster, spec, spec.lifetime);
380:     }
381: 
382:     public void applySpec(ServerPlayer caster, TechniqueHitSpec spec, int instanceLifetime) {
383:         CohortVisual shown = spec.cohortVisual == null ? CohortVisual.SINGLE : spec.cohortVisual.stored();
384:         this.entityData.set(DATA_DISPLAY_INDEX, shown.displayIndex());
385:         this.entityData.set(DATA_DISPLAY_COUNT, shown.displayCount());
386:         this.entityData.set(DATA_DISPLAY_LANE, shown.laneSplit());
387:         this.entityData.set(DATA_LANE_INDEX, shown.laneIndex());
388:         this.entityData.set(DATA_LANE_COUNT, shown.laneCount());
389:         this.casterUuid = caster.getUUID();
390:         ReleaseDefinition boundDefinition = ReleaseRegistry.get(spec.techniqueId);
391:         this.deflectsIncoming = boundDefinition != null && boundDefinition.deflectsProjectiles();
392:         this.persistentFlow = boundDefinition != null && boundDefinition.persistentFlowSettlement();
393:         this.storeLifetime(instanceLifetime);
394:         this.speed = spec.speed;
395:         this.lockThisCast = spec.lockThisCast;
396:         this.dropThisCast = spec.dropThisCast;
397:         this.centered = spec.centered;
398:         this.pose3d = spec.pose3d;
399:         this.arcRadius = spec.arcRadius;
400:         this.arcDegrees = spec.arcDegrees;
401:         this.arcHeight = spec.arcHeight;
402:         this.boxSize = spec.boxSize == null ? Vec3.ZERO : spec.boxSize;
403:         this.hitInterval = spec.hitInterval;
404:         this.hitReadyAge = Math.max(0, spec.hitReadyAge);
405:         this.listener = spec.listener;
406:         this.settleDamage = spec.settleDamage;
407:         this.settleEnchantments = spec.settleEnchantments;
408:         this.settleTally = spec.settleTally;
409:         this.settleCopyIndex = spec.settleCopyIndex;
410:         this.settleBurnSeconds = spec.settleBurnSeconds;
411:         this.settleControlTicks = spec.settleControlTicks;
412:         this.settleExtinguish = spec.settleExtinguish;
413:         this.settleKnockback = spec.settleKnockback;
414:         this.settleTide = spec.settleTide == null
415:                 ? com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess.NONE
416:                 : spec.settleTide;
417:         this.trap = spec.trap;
418:         this.burstOnImpact = spec.burstOnImpact;
419:         if (spec.blocksMovement) ObstacleBodies.track(this, spec.length, spec.width, spec.height);
420:         this.anchor = spec.anchor;
421:         this.aim = spec.aim;
422:         this.length = spec.length;
423:         this.width = spec.width;
424:         this.height = spec.height;
425:         this.functionId = spec.functionId == null ? "" : spec.functionId;
426:         if (spec.frozenArc != null) {
427:             this.installArc(spec.frozenArc);
428:             return;
429:         }
430:         this.origin = spec.start != null ? spec.start : caster.getEyePosition();
431:         this.forward = spec.aim != null && spec.aim.lengthSqr() > 1.0E-8
432:                 ? spec.aim.normalize()
433:                 : caster.getLookAngle();
434:         TechniqueHitSpec.Path path = spec.path;
435:         if (path == null) {
436:             path = switch (spec.motion) {
437:                 case DROP -> TechniqueHitSpec.Path.DROP;
438:                 case FOLLOW -> TechniqueHitSpec.Path.FOLLOW;
439:                 case SWING -> TechniqueHitSpec.Path.STILL_AIM;
440:                 case PROJECTILE -> TechniqueHitSpec.Path.STRAIGHT;
441:             };
442:         }
443:         if (spec.dropThisCast) {
444:             path = TechniqueHitSpec.Path.DROP;
445:         }
446:         this.beam = path == TechniqueHitSpec.Path.FOLLOW;
447:         this.pinned = this.beam && spec.start != null;
448:         this.entityData.set(DATA_TECHNIQUE_ID, spec.techniqueId == null ? "" : spec.techniqueId);
449:         this.entityData.set(DATA_VISUAL, spec.visual);
450:         this.entityData.set(DATA_MODEL_SCALE, spec.modelScale);
451:         this.entityData.set(DATA_TEXTURE, spec.texture == null ? "" : spec.texture);
452:         StillSheetVisual stillSheet = spec.stillSheet == null ? StillSheetVisual.NONE : spec.stillSheet;
453:         this.entityData.set(DATA_STILL_FRAMES, stillSheet.enabled() ? stillSheet.frameCount() : 0);
454:         this.entityData.set(DATA_STILL_FRAME_TICKS, stillSheet.enabled() ? stillSheet.ticksPerFrame() : 1);
455:         this.entityData.set(DATA_LENGTH, (float) spec.length);
456:         this.entityData.set(DATA_WIDTH, (float) spec.width);
457:         this.entityData.set(DATA_HEIGHT, (float) spec.height);
458:         this.entityData.set(DATA_ROLL, spec.roll);
459:         this.motion = spec.trap ? new com.bslx.XueXiMod.shu.technique.release.motion.TrapFallMotion() : TechniqueHitKinds.motion(path);
460:         if (spec.tracking && !spec.trap && (path == TechniqueHitSpec.Path.STRAIGHT
461:                 || path == TechniqueHitSpec.Path.DROP || path == TechniqueHitSpec.Path.VORTEX)) {
462:             this.motion = new com.bslx.XueXiMod.shu.technique.release.motion.TrackingMotion(path);
463:         }
464:         this.hitbox = spec.path == null && spec.texture == null ? TechniqueHitKinds.hitbox(spec.hitbox) : new SizedBoxHitbox();
465:         if (path == TechniqueHitSpec.Path.STILL_AIM && this.motion != null && caster.level() instanceof ServerLevel serverLevel) {
466:             this.motion.tick(this, serverLevel);
467:             this.previousPosition = this.position();
468:         } else if ((path == TechniqueHitSpec.Path.VORTEX || path == TechniqueHitSpec.Path.FUNCTION)
469:                 && this.motion != null && caster.level() instanceof ServerLevel vortexLevel) {
470:             this.motion.tick(this, vortexLevel);
471:             this.previousPosition = this.position();
472:         } else if (this.centered && this.anchor != null) {
473:             this.placeAt(this.anchor, this.aim != null ? this.aim : this.forward);
474:             this.previousPosition = this.anchor;
475:         } else {
476:             this.placeAt(this.origin, this.forward);
477:         }
478:     }
479: 
480:     public void placeAt(Vec3 position, Vec3 direction) {
481:         this.setPos(position.x, position.y, position.z);
482:         if (direction != null && direction.lengthSqr() > 1.0E-8) {
483:             Vec3 unitDirection = direction.normalize();
484:             double horizontal = Math.sqrt(unitDirection.x * unitDirection.x + unitDirection.z * unitDirection.z);
485:             this.setYRot((float) (Math.atan2(unitDirection.z, unitDirection.x) * (180.0 / Math.PI)) - 90.0F);
486:             this.setXRot((float) (-Math.atan2(unitDirection.y, horizontal) * (180.0 / Math.PI)));
487:         }
488:     }
489: 
490:     @Nullable
491:     public ServerPlayer getCaster() {
492:         if (this.casterUuid == null || this.level().isClientSide()) {
493:             return null;
494:         }
495:         MinecraftServer server = this.level().getServer();
496:         if (server == null) {
497:             return null;
498:         }
499:         return server.getPlayerList().getPlayer(this.casterUuid);
500:     }
501: 
502:     public int age() {
503:         return this.age;
504:     }
505: 
506:     public int lifetime() {
507:         return this.entityData.get(DATA_LIFETIME);
508:     }
509: 
510:     private void storeLifetime(int instanceLifetime) {
511:         this.lifetime = instanceLifetime;
512:         this.entityData.set(DATA_LIFETIME, instanceLifetime);
513:     }
514: 
515:     public double speed() {
516:         return this.speed;
517:     }
518: 
519:     public boolean lockThisCast() {
520:         return this.lockThisCast;
521:     }
522: 
523:     public boolean dropThisCast() {
524:         return this.dropThisCast;
525:     }
526: 
527:     public boolean centered() {
528:         return this.centered;
529:     }
530: 
531:     public boolean pose3d() {
532:         return this.pose3d;
533:     }
534: 
535:     public boolean beam() {
536:         return this.beam;
537:     }
538: 
539:     public boolean pinned() {
540:         return this.pinned;
541:     }
542: 
543:     public boolean planted() {
544:         return this.planted;
545:     }
546: 
547:     public void setPlanted(boolean planted) {
548:         this.planted = planted;
549:     }
550: 
551:     public float roll() {
552:         return this.entityData.get(DATA_ROLL);
553:     }
554: 
555:     public double arcRadius() {
556:         return this.arcRadius;
557:     }
558: 
559:     public double arcDegrees() {
560:         return this.arcDegrees;
561:     }
562: 
563:     public double arcHeight() {
564:         return this.arcHeight;
565:     }
566: 
567:     public Vec3 boxSize() {
568:         return this.boxSize;
569:     }
570: 
571:     public Vec3 origin() {
572:         return this.origin;
573:     }
574: 
575:     public Vec3 forward() {
576:         return this.forward;
577:     }
578: 
579:     public String functionId() {
580:         return this.functionId;
581:     }
582: 
583:     public Vec3 previousPosition() {
584:         return this.previousPosition;
585:     }
586: 
587:     @Nullable
588:     public Vec3 anchor() {
589:         return this.anchor;
590:     }
591: 
592:     @Nullable
593:     public Vec3 aim() {
594:         return this.aim;
595:     }
596: 
597:     public UUID casterUuid() {
598:         return this.casterUuid;
599:     }
600: 
601:     public boolean deflectsProjectiles() {
602:         return this.deflectsIncoming;
603:     }
604: 
605:     @Override
606:     public void remove(RemovalReason reason) {
607:         this.releaseWeepingDeflection();
608:         this.closeArcDeflection();
609:         super.remove(reason);
610:     }
611: 
612:     /** 运动之后、命中之前。空水段也要更新，旧宿主才能马上停。 */
613:     private void refreshWeepingDeflection() {
614:         if (this.level().isClientSide() || !this.persistentFlow) {
615:             return;
616:         }
617:         WeepingDeflectionBinding.update(this, WeepingCurtain.active(this));
618:     }
619: 
620:     private void releaseWeepingDeflection() {
621:         if (this.level().isClientSide() || !this.persistentFlow) {
622:             return;
623:         }
624:         WeepingDeflectionBinding.remove(this);
625:         WeepingCurtain.forget(this);
626:     }
627: 
628:     public void setOrigin(Vec3 origin) {
629:         this.origin = origin;
630:     }
631: 
632:     public void setForward(Vec3 forward) {
633:         this.forward = forward;
634:     }
635: 
636:     public boolean canTrack(LivingEntity target) {
637:         return TechniqueHitSettle.canHit(this, target)
638:                 && (this.hitInterval > 0 || !this.hitOnce.contains(target.getUUID()));
639:     }
640: 
641:     public String getTechniqueId() {
642:         return this.entityData.get(DATA_TECHNIQUE_ID);
643:     }
644: 
645:     public int getVisual() {
646:         return this.entityData.get(DATA_VISUAL);
647:     }
648: 
649:     public int displayCopyIndex() {
650:         return this.entityData.get(DATA_DISPLAY_INDEX);
651:     }
652: 
653:     public int displayCopyCount() {
654:         return this.entityData.get(DATA_DISPLAY_COUNT);
655:     }
656: 
657:     public boolean displayLaneSplit() {
658:         return this.entityData.get(DATA_DISPLAY_LANE);
659:     }
660: 
661:     public int displayLaneIndex() {
662:         return this.entityData.get(DATA_LANE_INDEX);
663:     }
664: 
665:     public int displayLaneCount() {
666:         return this.entityData.get(DATA_LANE_COUNT);
667:     }
668: 
669:     public CohortVisual cohortVisual() {
670:         return CohortVisual.read(this.displayCopyIndex(), this.displayCopyCount(),
671:                 this.displayLaneIndex(), this.displayLaneCount(), this.displayLaneSplit());
672:     }
673: 
674:     public float getModelScale() {
675:         return this.entityData.get(DATA_MODEL_SCALE);
676:     }
677: 
678:     public String texture() {
679:         return this.entityData.get(DATA_TEXTURE);
680:     }
681: 
682:     public StillSheetVisual stillSheet() {
683:         return StillSheetVisual.once(this.texture(), this.entityData.get(DATA_STILL_FRAMES),
684:                 this.entityData.get(DATA_STILL_FRAME_TICKS));
685:     }
686: 
687:     public long visualBirth() {
688:         return this.entityData.get(DATA_STILL_BIRTH);
689:     }
690: 
691:     public boolean stillSheetCancelled() {
692:         return this.entityData.get(DATA_STILL_CANCELLED);
693:     }
694: 
695:     /** 正式激活时写一次出生时间。暂挂和 applySpec 都不调用。 */
696:     public void stampStillBirth() {
697:         if (this.level().isClientSide() || !this.stillSheet().enabled()) {
698:             return;
699:         }
700:         if (this.entityData.get(DATA_STILL_BIRTH) >= 0L) {
701:             return;
702:         }
703:         this.entityData.set(DATA_STILL_BIRTH, this.level().getGameTime());
704:     }
705: 
706:     /** 显式取消视觉。命中到期移除不走这里。 */
707:     public void cancelStillSheet() {
708:         if (this.level().isClientSide()) {
709:             return;
710:         }
711:         this.entityData.set(DATA_STILL_CANCELLED, true);
712:     }
713: 
714:     public double length() {
715:         return this.level().isClientSide() ? this.entityData.get(DATA_LENGTH) : this.length;
716:     }
717: 
718:     public double width() {
719:         return this.level().isClientSide() ? this.entityData.get(DATA_WIDTH) : this.width;
720:     }
721: 
722:     public double height() {
723:         return this.level().isClientSide() ? this.entityData.get(DATA_HEIGHT) : this.height;
724:     }
725: 
726:     public boolean exactVolume() {
727:         return this.exactVolume;
728:     }
729: 
730:     public double hitHeight() {
731:         if (this.exactVolume) {
732:             return this.height() > 0.0 ? this.height() : 0.0;
733:         }
734:         if (this.height() <= 0.05) {
735:             return Math.max(this.length(), 1.0);
736:         }
737:         return Math.max(this.height(), 1.0);
738:     }
739: 
740:     public void resizeVolume(double along, double across, double vertical) {
741:         this.exactVolume = true;
742:         this.length = along;
743:         this.width = across;
744:         this.height = vertical;
745:         this.entityData.set(DATA_LENGTH, (float) along);
746:         this.entityData.set(DATA_WIDTH, (float) across);
747:         this.entityData.set(DATA_HEIGHT, (float) vertical);
748:         double reach = Math.max(along, Math.max(across, vertical)) * 0.5;
749:         this.setBoundingBox(new AABB(
750:                 this.getX() - reach, this.getY() - reach, this.getZ() - reach,
751:                 this.getX() + reach, this.getY() + reach, this.getZ() + reach));
752:     }
753: 
754:     private void installArc(ArcBandSnapshot snapshot) {
755:         this.motion = null;
756:         this.hitbox = null;
757:         this.centered = false;
758:         this.adoptArc(snapshot);
759:         this.forward = new Vec3(snapshot.forwardAxis().east(), snapshot.forwardAxis().above(), snapshot.forwardAxis().south());
760:         this.origin = new Vec3(snapshot.origin().east(), snapshot.origin().above(), snapshot.origin().south());
761:         this.placeAt(this.origin, this.forward);
762:         this.storeLifetime(snapshot.lifeTicks());
763:     }
764: 
765:     private void adoptArc(ArcBandSnapshot snapshot) {
766:         this.arcSnapshot = snapshot;
767:         this.entityData.set(DATA_ARC_SNAPSHOT, snapshot.pack());
768:     }
769: 
770:     private void tickArcBand(ServerLevel level) {
771:         if (this.arcSnapshot.birthTick() < 0L) {
772:             return;
773:         }
774:         long ageTicks = level.getGameTime() - this.arcSnapshot.birthTick();
775:         if (ageTicks < 0L) {
776:             ageTicks = 0L;
777:         }
778:         ProgressArcBand.SweepCursor cursor = ProgressArcBand.advance(
779:                 this.lastProcessedProgress, this.arcDamageClosed, ageTicks, this.arcSnapshot.activeTicks());
780:         if (cursor.swept()) {
781:             ArcBounds broad = ProgressArcBand.sliceBroadBounds(this.arcSnapshot, cursor.fromProgress(), cursor.toProgress());
782:             AABB searchBox = new AABB(broad.lowEast(), broad.lowAbove(), broad.lowSouth(),
783:                     broad.highEast(), broad.highAbove(), broad.highSouth());
784:             List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, searchBox,
785:                     entity -> TechniqueHitSettle.canHit(this, entity));
786:             for (LivingEntity target : targets) {
787:                 AABB targetBox = target.getBoundingBox();
788:                 ArcBounds targetBounds = new ArcBounds(targetBox.minX, targetBox.minY, targetBox.minZ,
789:                         targetBox.maxX, targetBox.maxY, targetBox.maxZ);
790:                 if (ProgressArcBand.intersectsSlice(this.arcSnapshot, targetBounds, cursor.fromProgress(), cursor.toProgress())) {
791:                     TechniqueHitSettle.tryHit(this, level, target);
792:                 }
793:             }
794:         }
795:         this.lastProcessedProgress = cursor.lastProgress();
796:         this.arcDamageClosed = cursor.damageClosed();
797:         this.refreshArcDeflection(level, ageTicks);
798:         if (ageTicks >= this.arcSnapshot.lifeTicks()) {
799:             TechniqueHitSettle.onExpire(this, level);
800:         }
801:     }
802: 
803:     private void refreshArcDeflection(ServerLevel level, long ageTicks) {
804:         if (!this.deflectsIncoming || this.level().isClientSide()) {
805:             this.closeArcDeflection();
806:             return;
807:         }
808:         ArcBandSnapshot snapshot = this.arcSnapshot;
809:         boolean open = snapshot != null
810:                 && snapshot.mode() == ArcBandMode.RADIAL_BLADE
811:                 && snapshot.birthTick() >= 0L
812:                 && ageTicks >= 0L
813:                 && ageTicks <= snapshot.activeTicks()
814:                 && this.isAlive()
815:                 && !this.isRemoved()
816:                 && this.casterUuid != null;
817:         if (!open) {
818:             this.closeArcDeflection();
819:             return;
820:         }
821:         if (this.arcDeflection == null) {
822:             this.arcDeflection = ArcDeflectionHost.bind(this);
823:             if (this.arcDeflection != null) {
824:                 DeflectionHosts.register(this.arcDeflection);
825:             }
826:         }
827:     }
828: 
829:     private void closeArcDeflection() {
830:         if (this.arcDeflection == null) {
831:             return;
832:         }
833:         DeflectionHosts.unregister(this.arcDeflection.key());
834:         this.arcDeflection = null;
835:     }
836: 
837:     @Nullable
838:     private AABB arcWorldBox() {
839:         ArcBandSnapshot snapshot = this.arcBand();
840:         if (snapshot == null) {
841:             return null;
842:         }
843:         ArcBounds broad = ProgressArcBand.sliceBroadBounds(snapshot, 0.0, 1.0);
844:         return new AABB(broad.lowEast(), broad.lowAbove(), broad.lowSouth(),
845:                 broad.highEast(), broad.highAbove(), broad.highSouth());
846:     }
847: }
848: 

### DIFF src/main/java/com/bslx/XueXiMod/shu/technique/release/WeepingSettings.java
diff --git "a/D:/alianximod/docs/\345\274\200\345\217\221\350\256\260\345\275\225/\350\201\232\350\203\275\350\276\223\345\205\245\351\223\276\346\240\270\346\237\245-20261027/before/src/main/java/com/bslx/XueXiMod/shu/technique/release/WeepingSettings.java" b/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/WeepingSettings.java
index 6a8e919..7f539bc 100644
--- "a/D:/alianximod/docs/\345\274\200\345\217\221\350\256\260\345\275\225/\350\201\232\350\203\275\350\276\223\345\205\245\351\223\276\346\240\270\346\237\245-20261027/before/src/main/java/com/bslx/XueXiMod/shu/technique/release/WeepingSettings.java"
+++ b/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/WeepingSettings.java
@@ -17,4 +17,14 @@ public final class WeepingSettings {
 
     private WeepingSettings() {
     }
+
+    /** 基准寿命减去基准排空起点，排空窗口固定 8 拍。 */
+    public static int drainSpan() {
+        return LIFETIME - DRAIN_START;
+    }
+
+    /** 实例总寿命减去固定排空拍数。填充长度不随总寿命改变。 */
+    public static int drainStartFor(int lifeTicks) {
+        return lifeTicks - drainSpan();
+    }
 }

### CURRENT src/main/java/com/bslx/XueXiMod/shu/technique/release/WeepingSettings.java
1: package com.bslx.XueXiMod.shu.technique.release;
2: 
3: /**
4:  * 垂丝这一轮的试调默认值。几何、结算和注册都读这里，不在各处再写一套。
5:  * 改前底伤是 5，魔力按 {@code ceil(底伤 * 2)} 得到 10。底伤改成 2 之后仍显式保留 10，冷却仍是 20。
6:  */
7: public final class WeepingSettings {
8:     public static final float BASE_DAMAGE = 2.0f;
9:     public static final int WETNESS = 3;
10:     public static final int HIT_INTERVAL = 10;
11:     public static final int LIFETIME = 40;
12:     public static final int FILL_TICKS = 8;
13:     public static final int DRAIN_START = 32;
14:     /** 水帘竖直方向允许的最大下落距离。 */
15:     public static final double FALL_DISTANCE_LIMIT = 6.0;
16:     public static final int RETAINED_MANA = 10;
17: 
18:     private WeepingSettings() {
19:     }
20: 
21:     /** 基准寿命减去基准排空起点，排空窗口固定 8 拍。 */
22:     public static int drainSpan() {
23:         return LIFETIME - DRAIN_START;
24:     }
25: 
26:     /** 实例总寿命减去固定排空拍数。填充长度不随总寿命改变。 */
27:     public static int drainStartFor(int lifeTicks) {
28:         return lifeTicks - drainSpan();
29:     }
30: }
31: 

### DIFF src/main/java/com/bslx/XueXiMod/shu/technique/release/WeepingCurtain.java
diff --git "a/D:/alianximod/docs/\345\274\200\345\217\221\350\256\260\345\275\225/\350\201\232\350\203\275\350\276\223\345\205\245\351\223\276\346\240\270\346\237\245-20261027/before/src/main/java/com/bslx/XueXiMod/shu/technique/release/WeepingCurtain.java" b/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/WeepingCurtain.java
index 0b23ff0..9d0b877 100644
--- "a/D:/alianximod/docs/\345\274\200\345\217\221\350\256\260\345\275\225/\350\201\232\350\203\275\350\276\223\345\205\245\351\223\276\346\240\270\346\237\245-20261027/before/src/main/java/com/bslx/XueXiMod/shu/technique/release/WeepingCurtain.java"
+++ b/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/WeepingCurtain.java
@@ -62,7 +62,7 @@ public final class WeepingCurtain {
         double across = sheetAcross(entity.width());
         double thick = sheetThick(entity.width());
         FootClip clip = probeTerrain(level, lip, entity.forward(), across, thick);
-        WaterSegment segment = segmentFor(clip, lip, entity.forward(), across, thick, entity.age());
+        WaterSegment segment = segmentFor(clip, lip, entity.forward(), across, thick, entity.age(), entity.lifetime());
         CACHED_SEGMENTS.put(entity, segment);
         entity.placeAt(segment.center(), segment.ahead());
         entity.resizeVolume(segment.thick(), segment.across(), segment.activeHeight());
@@ -116,16 +116,21 @@ public final class WeepingCurtain {
         return BASE_THICK * crossScale;
     }
 
-    /** 已知足端后的相位。不探地。 */
+    /** 已知足端后的相位。不探地。缺省寿命仍是基准 40 拍。 */
     public static WaterSegment flowSegment(Vec3 lip, Vec3 look, double footDrop, boolean floorFoot,
                                            double across, double thick, int age) {
+        return flowSegment(lip, look, footDrop, floorFoot, across, thick, age, WeepingSettings.LIFETIME);
+    }
+
+    /** 已知足端后的相位。排空起点是这一份实例的总寿命减固定排空拍数。 */
+    public static WaterSegment flowSegment(Vec3 lip, Vec3 look, double footDrop, boolean floorFoot,
+                                           double across, double thick, int age, int lifeTicks) {
         Vec3 mouth = lip == null ? Vec3.ZERO : lip;
         int fillTicks = WeepingSettings.FILL_TICKS;
         if (fillTicks < 1) {
             fillTicks = 1;
         }
-        int lifeTicks = WeepingSettings.LIFETIME;
-        int drainStart = WeepingSettings.DRAIN_START;
+        int drainStart = WeepingSettings.drainStartFor(lifeTicks);
         int drainWindow = lifeTicks - drainStart;
         if (drainWindow < 1) {
             drainWindow = 1;
@@ -152,12 +157,18 @@ public final class WeepingCurtain {
         return new WaterSegment(mouth, center, look, upperDrop, lowerDrop, safeFoot, across, thick, flowing, floorFoot);
     }
 
-    /** 源被堵住或预算不够时不给水体。其余走相位。 */
+    /** 源被堵住或预算不够时不给水体。其余走基准寿命相位。 */
     public static WaterSegment segmentFor(FootClip clip, Vec3 lip, Vec3 look, double across, double thick, int age) {
+        return segmentFor(clip, lip, look, across, thick, age, WeepingSettings.LIFETIME);
+    }
+
+    /** 源被堵住或预算不够时不给水体。其余按实例寿命走相位。 */
+    public static WaterSegment segmentFor(FootClip clip, Vec3 lip, Vec3 look, double across, double thick, int age,
+                                          int lifeTicks) {
         if (clip == null || clip.sourceInside() || clip.overBudget() || !Double.isFinite(clip.footDrop())) {
             return WaterSegment.absent(lip, look, across, thick);
         }
-        return flowSegment(lip, look, clip.footDrop(), clip.floorFoot(), across, thick, age);
+        return flowSegment(lip, look, clip.footDrop(), clip.floorFoot(), across, thick, age, lifeTicks);
     }
 
     /** 只有水头到达真实朝上地面时才溅水。 */

### CURRENT src/main/java/com/bslx/XueXiMod/shu/technique/release/WeepingCurtain.java
1: package com.bslx.XueXiMod.shu.technique.release;
2: 
3: import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
4: import net.minecraft.core.BlockPos;
5: import net.minecraft.core.particles.ParticleTypes;
6: import net.minecraft.server.level.ServerLevel;
7: import net.minecraft.util.Mth;
8: import net.minecraft.world.level.block.state.BlockState;
9: import net.minecraft.world.phys.AABB;
10: import net.minecraft.world.phys.Vec3;
11: import net.minecraft.world.phys.shapes.CollisionContext;
12: import net.minecraft.world.phys.shapes.VoxelShape;
13: 
14: import java.util.ArrayList;
15: import java.util.Collections;
16: import java.util.List;
17: import java.util.Map;
18: import java.util.WeakHashMap;
19: 
20: /**
21:  * 垂丝这一帘水。唇口固定在源点正上方，整段沿世界竖直轴下落。
22:  * 起流从上沿向下长，满流保持整段，排空时上沿往下收。本拍结果留给 {@link #active}。
23:  */
24: public final class WeepingCurtain {
25:     public static final String FUNCTION_ID = "water_weeping";
26:     /** 出水口在释放点上方。水从这里往下倒。 */
27:     public static final double LIP_LIFT = 4.0;
28:     /**
29:      * 足迹覆盖到的方块格上限。超出就整段留空并记下这个边界，不把采样间距拉大。
30:      */
31:     public static final int FOOTPRINT_CELL_BUDGET = 4096;
32: 
33:     private static final double REFERENCE_WIDTH = 0.5;
34:     private static final double BASE_THICK = 0.35;
35:     private static final double NARROW_SCALE = 0.2;
36:     private static final double PLANE_SLOP = 1.0E-4;
37:     private static final double SHAPE_SLOP = 1.0E-4;
38:     private static final double EDGE_SLOP = 1.0E-7;
39: 
40:     /** 弱键只留水段值，不把世界留在缓存里。 */
41:     private static final Map<TechniqueHitEntity, WaterSegment> CACHED_SEGMENTS =
42:             Collections.synchronizedMap(new WeakHashMap<>());
43:     private static volatile long notedCells;
44:     private static volatile boolean notedOverBudget;
45: 
46:     private WeepingCurtain() {
47:     }
48: 
49:     public static void register() {
50:         TrajectorySampler.registerVolume(FUNCTION_ID, WeepingCurtain::fit);
51:     }
52: 
53:     /**
54:      * 按这一拍的实体源点和朝向写下水段。
55:      * {@code groundPoint} 是旧弹道在释放点正下方的射线，实体这时已经被挪走，不用它改足端。
56:      */
57:     public static void fit(TechniqueHitEntity entity, ServerLevel level, Vec3 groundPoint) {
58:         if (entity == null) {
59:             return;
60:         }
61:         Vec3 lip = lipOf(entity.origin());
62:         double across = sheetAcross(entity.width());
63:         double thick = sheetThick(entity.width());
64:         FootClip clip = probeTerrain(level, lip, entity.forward(), across, thick);
65:         WaterSegment segment = segmentFor(clip, lip, entity.forward(), across, thick, entity.age(), entity.lifetime());
66:         CACHED_SEGMENTS.put(entity, segment);
67:         entity.placeAt(segment.center(), segment.ahead());
68:         entity.resizeVolume(segment.thick(), segment.across(), segment.activeHeight());
69:         if (level != null && splashes(entity.age(), clip, segment)) {
70:             Vec3 splashAt = footPoint(segment);
71:             level.sendParticles(ParticleTypes.SPLASH, splashAt.x, splashAt.y, splashAt.z, 6, 0.15, 0.05, 0.15, 0.02);
72:         }
73:     }
74: 
75:     /** 返回本拍 {@link #fit} 已经算好的水段。没有缓存时给有限空段，不再探地。 */
76:     public static WaterSegment active(TechniqueHitEntity entity) {
77:         if (entity == null) {
78:             return WaterSegment.EMPTY;
79:         }
80:         WaterSegment cached = CACHED_SEGMENTS.get(entity);
81:         if (cached == null) {
82:             return WaterSegment.EMPTY;
83:         }
84:         return cached;
85:     }
86: 
87:     /** 实体移除时丢掉缓存。 */
88:     public static void forget(TechniqueHitEntity entity) {
89:         if (entity == null) {
90:             return;
91:         }
92:         CACHED_SEGMENTS.remove(entity);
93:     }
94: 
95:     public static Vec3 lipOf(Vec3 origin) {
96:         if (origin == null || !Double.isFinite(origin.x) || !Double.isFinite(origin.y) || !Double.isFinite(origin.z)) {
97:             return new Vec3(0.0, LIP_LIFT, 0.0);
98:         }
99:         return new Vec3(origin.x, origin.y + LIP_LIFT, origin.z);
100:     }
101: 
102:     /** 当前实体宽度，含聚能。非法宽度回到基准 0.5。 */
103:     public static double sheetAcross(double entityWidth) {
104:         if (!Double.isFinite(entityWidth) || entityWidth <= 0.0) {
105:             return REFERENCE_WIDTH;
106:         }
107:         return entityWidth;
108:     }
109: 
110:     /** 厚度仍按宽度相对 0.5 缩放。过窄或非法时用基准厚度，不把宽度一起改掉。 */
111:     public static double sheetThick(double entityWidth) {
112:         double crossScale = entityWidth / REFERENCE_WIDTH;
113:         if (!Double.isFinite(crossScale) || crossScale < NARROW_SCALE) {
114:             crossScale = 1.0;
115:         }
116:         return BASE_THICK * crossScale;
117:     }
118: 
119:     /** 已知足端后的相位。不探地。缺省寿命仍是基准 40 拍。 */
120:     public static WaterSegment flowSegment(Vec3 lip, Vec3 look, double footDrop, boolean floorFoot,
121:                                            double across, double thick, int age) {
122:         return flowSegment(lip, look, footDrop, floorFoot, across, thick, age, WeepingSettings.LIFETIME);
123:     }
124: 
125:     /** 已知足端后的相位。排空起点是这一份实例的总寿命减固定排空拍数。 */
126:     public static WaterSegment flowSegment(Vec3 lip, Vec3 look, double footDrop, boolean floorFoot,
127:                                            double across, double thick, int age, int lifeTicks) {
128:         Vec3 mouth = lip == null ? Vec3.ZERO : lip;
129:         int fillTicks = WeepingSettings.FILL_TICKS;
130:         if (fillTicks < 1) {
131:             fillTicks = 1;
132:         }
133:         int drainStart = WeepingSettings.drainStartFor(lifeTicks);
134:         int drainWindow = lifeTicks - drainStart;
135:         if (drainWindow < 1) {
136:             drainWindow = 1;
137:         }
138:         int lived = Math.max(age, 0);
139:         double head = lived >= fillTicks ? 1.0 : lived / (double) fillTicks;
140:         double tail = lived <= drainStart ? 0.0 : (lived - drainStart) / (double) drainWindow;
141:         if (tail > 1.0) {
142:             tail = 1.0;
143:         }
144:         if (tail > head) {
145:             tail = head;
146:         }
147:         double safeFoot = footDrop;
148:         if (!Double.isFinite(safeFoot) || safeFoot < 0.0) {
149:             safeFoot = 0.0;
150:         }
151:         double upperDrop = safeFoot * tail;
152:         double lowerDrop = safeFoot * head;
153:         double openSpan = lowerDrop - upperDrop;
154:         boolean flowing = openSpan > WaterSegment.PRESENT_GAP;
155:         double middleDrop = (upperDrop + lowerDrop) * 0.5;
156:         Vec3 center = new Vec3(mouth.x, mouth.y - middleDrop, mouth.z);
157:         return new WaterSegment(mouth, center, look, upperDrop, lowerDrop, safeFoot, across, thick, flowing, floorFoot);
158:     }
159: 
160:     /** 源被堵住或预算不够时不给水体。其余走基准寿命相位。 */
161:     public static WaterSegment segmentFor(FootClip clip, Vec3 lip, Vec3 look, double across, double thick, int age) {
162:         return segmentFor(clip, lip, look, across, thick, age, WeepingSettings.LIFETIME);
163:     }
164: 
165:     /** 源被堵住或预算不够时不给水体。其余按实例寿命走相位。 */
166:     public static WaterSegment segmentFor(FootClip clip, Vec3 lip, Vec3 look, double across, double thick, int age,
167:                                           int lifeTicks) {
168:         if (clip == null || clip.sourceInside() || clip.overBudget() || !Double.isFinite(clip.footDrop())) {
169:             return WaterSegment.absent(lip, look, across, thick);
170:         }
171:         return flowSegment(lip, look, clip.footDrop(), clip.floorFoot(), across, thick, age, lifeTicks);
172:     }
173: 
174:     /** 只有水头到达真实朝上地面时才溅水。 */
175:     public static boolean splashes(int age, FootClip clip, WaterSegment segment) {
176:         return clip != null
177:                 && segment != null
178:                 && !clip.sourceInside()
179:                 && !clip.overBudget()
180:                 && clip.floorFoot()
181:                 && segment.present()
182:                 && age == WeepingSettings.FILL_TICKS;
183:     }
184: 
185:     /** 足端在唇口正下方，不往前倾。 */
186:     public static Vec3 footPoint(WaterSegment segment) {
187:         if (segment == null) {
188:             return Vec3.ZERO;
189:         }
190:         return new Vec3(segment.lip().x, segment.lip().y - segment.footDrop(), segment.lip().z);
191:     }
192: 
193:     public static long footprintCellCount(Vec3 lip, Vec3 look, double across, double thick) {
194:         PreparedColumn prepared = prepare(lip, look, across, thick);
195:         if (prepared == null) {
196:             return 0L;
197:         }
198:         return prepared.cellCount();
199:     }
200: 
201:     public static long notedFootprintCells() {
202:         return notedCells;
203:     }
204: 
205:     public static boolean footprintBudgetRejected() {
206:         return notedOverBudget;
207:     }
208: 
209:     /** 用已经取到的碰撞盒裁切。盒子要是世界坐标。超预算时不读这些盒子。 */
210:     public static FootClip clipFootprint(Vec3 lip, Vec3 look, double across, double thick, List<AABB> boxes) {
211:         PreparedColumn prepared = prepare(lip, look, across, thick);
212:         if (prepared == null) {
213:             noteFootprint(0L, true);
214:             return FootClip.budgetStop(0L);
215:         }
216:         if (prepared.cellCount() > FOOTPRINT_CELL_BUDGET) {
217:             noteFootprint(prepared.cellCount(), true);
218:             return FootClip.budgetStop(prepared.cellCount());
219:         }
220:         noteFootprint(prepared.cellCount(), false);
221:         return clipBoxes(prepared, boxes);
222:     }
223: 
224:     /**
225:      * 从世界读取碰撞形状后再裁切。形状以方块自己的格为原点，这里平移一次。
226:      * 只读方块，不改地形。没有世界或格子超预算时不扫描。
227:      */
228:     public static FootClip probeTerrain(ServerLevel level, Vec3 lip, Vec3 look, double across, double thick) {
229:         PreparedColumn prepared = prepare(lip, look, across, thick);
230:         if (prepared == null || level == null || prepared.cellCount() > FOOTPRINT_CELL_BUDGET) {
231:             long cellCount = prepared == null ? 0L : prepared.cellCount();
232:             noteFootprint(cellCount, true);
233:             return FootClip.budgetStop(cellCount);
234:         }
235:         noteFootprint(prepared.cellCount(), false);
236:         return clipBoxes(prepared, gatherTerrain(level, prepared.range()));
237:     }
238: 
239:     private static PreparedColumn prepare(Vec3 lip, Vec3 look, double across, double thick) {
240:         double limit = WeepingSettings.FALL_DISTANCE_LIMIT;
241:         if (!Double.isFinite(limit) || limit <= 0.0) {
242:             return null;
243:         }
244:         Vec3 mouth = lip;
245:         if (mouth == null || !Double.isFinite(mouth.x) || !Double.isFinite(mouth.y) || !Double.isFinite(mouth.z)) {
246:             mouth = Vec3.ZERO;
247:         }
248:         double fullAcross = across;
249:         double fullThick = thick;
250:         if (!Double.isFinite(fullAcross) || fullAcross < 0.0) {
251:             fullAcross = 0.0;
252:         }
253:         if (!Double.isFinite(fullThick) || fullThick < 0.0) {
254:             fullThick = 0.0;
255:         }
256:         FlatFrame frame = FlatFrame.of(look);
257:         AABB bounds = columnBounds(mouth, frame, fullAcross * 0.5, fullThick * 0.5, limit);
258:         BlockRange range = BlockRange.of(bounds);
259:         return new PreparedColumn(mouth, frame, fullAcross * 0.5, fullThick * 0.5, limit, range, range.cellCount());
260:     }
261: 
262:     private static FootClip clipBoxes(PreparedColumn prepared, List<AABB> boxes) {
263:         if (boxes == null || boxes.isEmpty()) {
264:             return FootClip.openAir(prepared.dropLimit(), prepared.cellCount());
265:         }
266:         boolean buried = false;
267:         boolean foundTop = false;
268:         double highestTop = Double.NEGATIVE_INFINITY;
269:         for (AABB box : boxes) {
270:             if (!boxUsable(box) || !overlapsFootprint(prepared, box)) {
271:                 continue;
272:             }
273:             if (box.minY < prepared.mouth().y - PLANE_SLOP && box.maxY > prepared.mouth().y + PLANE_SLOP) {
274:                 buried = true;
275:                 break;
276:             }
277:             if (box.maxY <= prepared.mouth().y + PLANE_SLOP
278:                     && box.maxY > prepared.mouth().y - prepared.dropLimit() + PLANE_SLOP) {
279:                 if (!foundTop || box.maxY > highestTop) {
280:                     highestTop = box.maxY;
281:                     foundTop = true;
282:                 }
283:             }
284:         }
285:         if (buried) {
286:             return FootClip.blockedSource(prepared.cellCount());
287:         }
288:         if (!foundTop) {
289:             return FootClip.openAir(prepared.dropLimit(), prepared.cellCount());
290:         }
291:         double drop = prepared.mouth().y - highestTop;
292:         if (!Double.isFinite(drop) || drop < 0.0) {
293:             drop = 0.0;
294:         }
295:         if (drop > prepared.dropLimit()) {
296:             return FootClip.openAir(prepared.dropLimit(), prepared.cellCount());
297:         }
298:         return new FootClip(drop, true, false, false, prepared.cellCount());
299:     }
300: 
301:     private static boolean overlapsFootprint(PreparedColumn prepared, AABB box) {
302:         Vec3 ahead = prepared.frame().ahead();
303:         Vec3 right = prepared.frame().right();
304:         double centerX = (box.minX + box.maxX) * 0.5;
305:         double centerZ = (box.minZ + box.maxZ) * 0.5;
306:         double halfBoxX = (box.maxX - box.minX) * 0.5;
307:         double halfBoxZ = (box.maxZ - box.minZ) * 0.5;
308:         double offsetX = centerX - prepared.mouth().x;
309:         double offsetZ = centerZ - prepared.mouth().z;
310:         double curtainRadiusX = Math.abs(ahead.x) * prepared.halfThick() + Math.abs(right.x) * prepared.halfAcross();
311:         if (Math.abs(offsetX) > halfBoxX + curtainRadiusX - SHAPE_SLOP) {
312:             return false;
313:         }
314:         double curtainRadiusZ = Math.abs(ahead.z) * prepared.halfThick() + Math.abs(right.z) * prepared.halfAcross();
315:         if (Math.abs(offsetZ) > halfBoxZ + curtainRadiusZ - SHAPE_SLOP) {
316:             return false;
317:         }
318:         double aheadOffset = offsetX * ahead.x + offsetZ * ahead.z;
319:         double boxOnAhead = halfBoxX * Math.abs(ahead.x) + halfBoxZ * Math.abs(ahead.z);
320:         if (Math.abs(aheadOffset) > prepared.halfThick() + boxOnAhead - SHAPE_SLOP) {
321:             return false;
322:         }
323:         double rightOffset = offsetX * right.x + offsetZ * right.z;
324:         double boxOnRight = halfBoxX * Math.abs(right.x) + halfBoxZ * Math.abs(right.z);
325:         if (Math.abs(rightOffset) > prepared.halfAcross() + boxOnRight - SHAPE_SLOP) {
326:             return false;
327:         }
328:         return true;
329:     }
330: 
331:     private static List<AABB> gatherTerrain(ServerLevel level, BlockRange range) {
332:         List<AABB> gathered = new ArrayList<>();
333:         BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
334:         CollisionContext emptyContext = CollisionContext.empty();
335:         for (int columnX = range.westEdge(); columnX <= range.eastEdge(); columnX++) {
336:             for (int layerY = range.lowEdge(); layerY <= range.highEdge(); layerY++) {
337:                 for (int columnZ = range.northEdge(); columnZ <= range.southEdge(); columnZ++) {
338:                     cursor.set(columnX, layerY, columnZ);
339:                     BlockState state = level.getBlockState(cursor);
340:                     if (state.isAir()) {
341:                         continue;
342:                     }
343:                     VoxelShape shape = state.getCollisionShape(level, cursor, emptyContext);
344:                     if (shape == null || shape.isEmpty()) {
345:                         continue;
346:                     }
347:                     VoxelShape moved = shape.move(columnX, layerY, columnZ);
348:                     for (AABB part : moved.toAabbs()) {
349:                         if (boxUsable(part)) {
350:                             gathered.add(part);
351:                         }
352:                     }
353:                 }
354:             }
355:         }
356:         return gathered;
357:     }
358: 
359:     private static AABB columnBounds(Vec3 mouth, FlatFrame frame, double halfAcross, double halfThick, double dropLimit) {
360:         Vec3 ahead = frame.ahead();
361:         Vec3 right = frame.right();
362:         double west = mouth.x;
363:         double east = mouth.x;
364:         double north = mouth.z;
365:         double south = mouth.z;
366:         double[] cornerAlong = new double[] {halfThick, halfThick, -halfThick, -halfThick};
367:         double[] cornerAcross = new double[] {halfAcross, -halfAcross, halfAcross, -halfAcross};
368:         for (int cornerIndex = 0; cornerIndex < cornerAlong.length; cornerIndex++) {
369:             double sampleX = mouth.x + ahead.x * cornerAlong[cornerIndex] + right.x * cornerAcross[cornerIndex];
370:             double sampleZ = mouth.z + ahead.z * cornerAlong[cornerIndex] + right.z * cornerAcross[cornerIndex];
371:             west = Math.min(west, sampleX);
372:             east = Math.max(east, sampleX);
373:             north = Math.min(north, sampleZ);
374:             south = Math.max(south, sampleZ);
375:         }
376:         return new AABB(west, mouth.y - dropLimit, north, east, mouth.y, south);
377:     }
378: 
379:     private static boolean boxUsable(AABB box) {
380:         return box != null
381:                 && Double.isFinite(box.minX) && Double.isFinite(box.minY) && Double.isFinite(box.minZ)
382:                 && Double.isFinite(box.maxX) && Double.isFinite(box.maxY) && Double.isFinite(box.maxZ)
383:                 && box.maxX >= box.minX && box.maxY >= box.minY && box.maxZ >= box.minZ;
384:     }
385: 
386:     private static void noteFootprint(long cellCount, boolean rejected) {
387:         notedCells = cellCount;
388:         notedOverBudget = rejected;
389:     }
390: 
391:     /**
392:      * 一列水的裁切结果。{@code floorFoot} 只在落到朝上的盒顶，并且落差小于最大落差时为真。
393:      */
394:     public record FootClip(double footDrop, boolean floorFoot, boolean sourceInside, boolean overBudget, long cellCount) {
395:         public static FootClip openAir(double footDrop, long cellCount) {
396:             double drop = footDrop;
397:             if (!Double.isFinite(drop) || drop < 0.0) {
398:                 drop = 0.0;
399:             }
400:             return new FootClip(drop, false, false, false, cellCount);
401:         }
402: 
403:         public static FootClip blockedSource(long cellCount) {
404:             return new FootClip(0.0, false, true, false, cellCount);
405:         }
406: 
407:         public static FootClip budgetStop(long cellCount) {
408:             return new FootClip(0.0, false, false, true, cellCount);
409:         }
410:     }
411: 
412:     private record FlatFrame(Vec3 ahead, Vec3 right) {
413:         private static FlatFrame of(Vec3 look) {
414:             Vec3 ahead = WaterSegment.horizontalAhead(look);
415:             return new FlatFrame(ahead, new Vec3(ahead.z, 0.0, -ahead.x));
416:         }
417:     }
418: 
419:     private record PreparedColumn(Vec3 mouth, FlatFrame frame, double halfAcross, double halfThick,
420:                                   double dropLimit, BlockRange range, long cellCount) {
421:     }
422: 
423:     private record BlockRange(int westEdge, int eastEdge, int lowEdge, int highEdge,
424:                               int northEdge, int southEdge, long cellCount) {
425:         private static BlockRange of(AABB bounds) {
426:             int westEdge = Mth.floor(bounds.minX);
427:             int eastEdge = Mth.floor(bounds.maxX - EDGE_SLOP);
428:             int lowEdge = Mth.floor(bounds.minY);
429:             int highEdge = Mth.floor(bounds.maxY - EDGE_SLOP);
430:             int northEdge = Mth.floor(bounds.minZ);
431:             int southEdge = Mth.floor(bounds.maxZ - EDGE_SLOP);
432:             if (eastEdge < westEdge) {
433:                 eastEdge = westEdge;
434:             }
435:             if (highEdge < lowEdge) {
436:                 highEdge = lowEdge;
437:             }
438:             if (southEdge < northEdge) {
439:                 southEdge = northEdge;
440:             }
441:             long spanX = (long) eastEdge - westEdge + 1L;
442:             long spanY = (long) highEdge - lowEdge + 1L;
443:             long spanZ = (long) southEdge - northEdge + 1L;
444:             long cellCount;
445:             if (spanX > FOOTPRINT_CELL_BUDGET || spanY > FOOTPRINT_CELL_BUDGET || spanZ > FOOTPRINT_CELL_BUDGET) {
446:                 cellCount = (long) FOOTPRINT_CELL_BUDGET + 1L;
447:             } else {
448:                 cellCount = spanX * spanY * spanZ;
449:             }
450:             return new BlockRange(westEdge, eastEdge, lowEdge, highEdge, northEdge, southEdge, cellCount);
451:         }
452:     }
453: }
454: 

### DIFF src/main/java/com/bslx/XueXiMod/client/WaterCurtainLayout.java
baseline missing
### CURRENT src/main/java/com/bslx/XueXiMod/client/WaterCurtainLayout.java
1: package com.bslx.XueXiMod.client;
2: 
3: import com.bslx.XueXiMod.shu.technique.release.WeepingSettings;
4: 
5: import java.util.ArrayList;
6: import java.util.List;
7: 
8: /**
9:  * 垂丝瀑布分件。调用方传入已经结算的水帘宽和高，这里不再乘聚能。
10:  * 横向 24 像素对应 1 格：4 像素边缘各 1/6 格，中段按 4 像素周期重复，末段裁几何和 U。
11:  * 纵向同样保持 24 像素每格，每段只采样当前帧，不把 128 像素拉满整条水帘。
12:  */
13: public final class WaterCurtainLayout {
14:     public static final int FRAME_COUNT = 16;
15:     public static final double PIXELS_PER_BLOCK = 24.0;
16:     public static final double EDGE_BLOCKS = 4.0 / PIXELS_PER_BLOCK;
17:     public static final double FRAME_BLOCKS = 128.0 / PIXELS_PER_BLOCK;
18:     /** 半个纹素，避免最近邻踩到相邻动画帧。 */
19:     public static final double TEXEL_PAD = 0.5 / 128.0;
20:     /**
21:      * thin 画布宽 64。全部 16 帧的不透明像素都在 x=25..38，左右留白不参与宽度。
22:      * 固定取到 39，给水流在这 14 列里摆动，不按单帧再收紧。
23:      */
24:     public static final double THIN_CONTENT_U_START = 25.0 / 64.0;
25:     public static final double THIN_CONTENT_U_END = 39.0 / 64.0;
26:     private static final int TILE_GUARD = 512;
27:     private static final int BAND_GUARD = 64;
28:     private static final double FIT_SLOP = 1.0E-4;
29: 
30:     public enum Piece {
31:         LEFT,
32:         MIDDLE,
33:         RIGHT,
34:         THIN
35:     }
36: 
37:     public static final class Panel {
38:         public final Piece piece;
39:         public final double alongStart;
40:         public final double alongEnd;
41:         public final double topOffset;
42:         public final double bottomOffset;
43:         public final double textureUStart;
44:         public final double textureUEnd;
45:         public final double textureVStart;
46:         public final double textureVEnd;
47: 
48:         private Panel(Piece piece, double alongStart, double alongEnd,
49:                       double topOffset, double bottomOffset,
50:                       double textureUStart, double textureUEnd,
51:                       double textureVStart, double textureVEnd) {
52:             this.piece = piece;
53:             this.alongStart = alongStart;
54:             this.alongEnd = alongEnd;
55:             this.topOffset = topOffset;
56:             this.bottomOffset = bottomOffset;
57:             this.textureUStart = textureUStart;
58:             this.textureUEnd = textureUEnd;
59:             this.textureVStart = textureVStart;
60:             this.textureVEnd = textureVEnd;
61:         }
62:     }
63: 
64:     private WaterCurtainLayout() {
65:     }
66: 
67:     /** 与服务端水体同一排空起点。贴图仍由 {@link #flowFrame} 每 2 tick 推进。 */
68:     public static int drainStart(int lifeTicks) {
69:         return WeepingSettings.drainStartFor(lifeTicks);
70:     }
71: 
72:     /**
73:      * 高度已经是服务端按同一寿命算出的水段。这里只拒绝没有排空窗口的哨兵，不改分件。
74:      */
75:     public static List<Panel> panels(double curtainWidth, double curtainHeight, int frameIndex, int drainStart) {
76:         if (drainStart > Integer.MAX_VALUE - WeepingSettings.FILL_TICKS) {
77:             return List.of();
78:         }
79:         return panels(curtainWidth, curtainHeight, frameIndex);
80:     }
81: 
82:     /** 16 帧，每 2 tick 一帧。相位只平移时间，不读取宽度，也不随寿命拉长。 */
83:     public static int flowFrame(int livedTicks, float partialTicks, int phaseTicks) {
84:         double partial = partialTicks;
85:         if (!Float.isFinite(partialTicks)) {
86:             partial = 0.0;
87:         }
88:         double elapsed = livedTicks + partial + phaseTicks;
89:         if (!Double.isFinite(elapsed)) {
90:             return 0;
91:         }
92:         int stepped = (int) Math.floor(elapsed / 2.0);
93:         return Math.floorMod(stepped, FRAME_COUNT);
94:     }
95: 
96:     public static List<Panel> panels(double curtainWidth, double curtainHeight, int frameIndex) {
97:         if (!Double.isFinite(curtainWidth) || !Double.isFinite(curtainHeight)) {
98:             return List.of();
99:         }
100:         if (curtainWidth <= FIT_SLOP || curtainHeight <= FIT_SLOP) {
101:             return List.of();
102:         }
103:         List<Span> spans = horizontalSpans(curtainWidth);
104:         List<Band> bands = verticalBands(curtainHeight, frameIndex);
105:         ArrayList<Panel> panels = new ArrayList<>(spans.size() * bands.size());
106:         for (Band band : bands) {
107:             for (Span span : spans) {
108:                 panels.add(new Panel(
109:                         span.piece, span.alongStart, span.alongEnd,
110:                         band.topOffset, band.bottomOffset,
111:                         span.textureUStart, span.textureUEnd,
112:                         band.textureVStart, band.textureVEnd));
113:             }
114:         }
115:         return panels;
116:     }
117: 
118:     private static List<Span> horizontalSpans(double curtainWidth) {
119:         double halfAcross = curtainWidth * 0.5;
120:         double middleSpan = curtainWidth - 2.0 * EDGE_BLOCKS;
121:         if (!(middleSpan >= -FIT_SLOP)) {
122:             return thinFallback(halfAcross);
123:         }
124:         double periods = middleSpan / EDGE_BLOCKS;
125:         int wholeTiles = (int) Math.floor(periods + FIT_SLOP);
126:         if (wholeTiles < 0 || wholeTiles > TILE_GUARD) {
127:             return thinFallback(halfAcross);
128:         }
129:         double leftover = middleSpan - wholeTiles * EDGE_BLOCKS;
130:         if (leftover < FIT_SLOP) {
131:             leftover = 0.0;
132:         }
133:         double leftInner = -halfAcross + EDGE_BLOCKS;
134:         double rightInner = halfAcross - EDGE_BLOCKS;
135:         if (rightInner < leftInner) {
136:             double meet = (leftInner + rightInner) * 0.5;
137:             leftInner = meet;
138:             rightInner = meet;
139:         }
140:         ArrayList<Span> spans = new ArrayList<>();
141:         spans.add(new Span(Piece.LEFT, -halfAcross, leftInner, 0.0, 1.0));
142:         double cursor = leftInner;
143:         for (int tileIndex = 0; tileIndex < wholeTiles; tileIndex++) {
144:             double tileEnd = cursor + EDGE_BLOCKS;
145:             boolean closing = tileIndex == wholeTiles - 1 && leftover == 0.0;
146:             if (closing || tileEnd > rightInner) {
147:                 tileEnd = rightInner;
148:             }
149:             spans.add(new Span(Piece.MIDDLE, cursor, tileEnd, 0.0, 1.0));
150:             cursor = tileEnd;
151:         }
152:         if (leftover > 0.0) {
153:             double textureEnd = leftover / EDGE_BLOCKS;
154:             if (textureEnd > 1.0) {
155:                 textureEnd = 1.0;
156:             }
157:             spans.add(new Span(Piece.MIDDLE, cursor, rightInner, 0.0, textureEnd));
158:         }
159:         spans.add(new Span(Piece.RIGHT, rightInner, halfAcross, 0.0, 1.0));
160:         return spans;
161:     }
162: 
163:     private static List<Span> thinFallback(double halfAcross) {
164:         return List.of(new Span(Piece.THIN, -halfAcross, halfAcross, THIN_CONTENT_U_START, THIN_CONTENT_U_END));
165:     }
166: 
167:     private static List<Band> verticalBands(double curtainHeight, int frameIndex) {
168:         int frameSlot = Math.floorMod(frameIndex, FRAME_COUNT);
169:         double halfTall = curtainHeight * 0.5;
170:         double lowerEdge = -halfTall;
171:         double cursor = halfTall;
172:         ArrayList<Band> bands = new ArrayList<>();
173:         int placed = 0;
174:         while (cursor - lowerEdge > FIT_SLOP && placed < BAND_GUARD) {
175:             double room = cursor - lowerEdge;
176:             double bandSpan;
177:             double localCover;
178:             if (room >= FRAME_BLOCKS - FIT_SLOP) {
179:                 if (Math.abs(room - FRAME_BLOCKS) <= FIT_SLOP) {
180:                     bandSpan = room;
181:                 } else {
182:                     bandSpan = FRAME_BLOCKS;
183:                 }
184:                 localCover = 1.0;
185:             } else {
186:                 bandSpan = room;
187:                 localCover = bandSpan / FRAME_BLOCKS;
188:             }
189:             double localTop = TEXEL_PAD;
190:             double localBottom = localCover - TEXEL_PAD;
191:             if (localBottom <= localTop) {
192:                 localTop = TEXEL_PAD;
193:                 localBottom = Math.min(1.0 - TEXEL_PAD, TEXEL_PAD + Math.max(localCover, TEXEL_PAD));
194:             }
195:             double textureVStart = (frameSlot + localTop) / FRAME_COUNT;
196:             double textureVEnd = (frameSlot + localBottom) / FRAME_COUNT;
197:             double frameCeiling = (frameSlot + 1.0) / FRAME_COUNT;
198:             if (textureVEnd > frameCeiling) {
199:                 textureVEnd = frameCeiling - TEXEL_PAD / FRAME_COUNT;
200:             }
201:             bands.add(new Band(cursor, cursor - bandSpan, textureVStart, textureVEnd));
202:             cursor -= bandSpan;
203:             placed++;
204:         }
205:         return bands;
206:     }
207: 
208:     private static final class Span {
209:         private final Piece piece;
210:         private final double alongStart;
211:         private final double alongEnd;
212:         private final double textureUStart;
213:         private final double textureUEnd;
214: 
215:         private Span(Piece piece, double alongStart, double alongEnd, double textureUStart, double textureUEnd) {
216:             this.piece = piece;
217:             this.alongStart = alongStart;
218:             this.alongEnd = alongEnd;
219:             this.textureUStart = textureUStart;
220:             this.textureUEnd = textureUEnd;
221:         }
222:     }
223: 
224:     private static final class Band {
225:         private final double topOffset;
226:         private final double bottomOffset;
227:         private final double textureVStart;
228:         private final double textureVEnd;
229: 
230:         private Band(double topOffset, double bottomOffset, double textureVStart, double textureVEnd) {
231:             this.topOffset = topOffset;
232:             this.bottomOffset = bottomOffset;
233:             this.textureVStart = textureVStart;
234:             this.textureVEnd = textureVEnd;
235:         }
236:     }
237: }
238: 

### DIFF src/main/java/com/bslx/XueXiMod/client/WaterCurtainRenderer.java
baseline missing
### CURRENT src/main/java/com/bslx/XueXiMod/client/WaterCurtainRenderer.java
1: package com.bslx.XueXiMod.client;
2: 
3: import com.bslx.XueXiMod.XueXiMod;
4: import com.bslx.XueXiMod.client.WaterCurtainLayout.Panel;
5: import com.bslx.XueXiMod.client.WaterCurtainLayout.Piece;
6: import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
7: import com.mojang.blaze3d.vertex.DefaultVertexFormat;
8: import com.mojang.blaze3d.vertex.PoseStack;
9: import com.mojang.blaze3d.vertex.VertexConsumer;
10: import com.mojang.blaze3d.vertex.VertexFormat;
11: import net.minecraft.client.renderer.LightTexture;
12: import net.minecraft.client.renderer.MultiBufferSource;
13: import net.minecraft.client.renderer.RenderStateShard;
14: import net.minecraft.client.renderer.RenderType;
15: import net.minecraft.client.renderer.texture.OverlayTexture;
16: import net.minecraft.resources.ResourceLocation;
17: import net.minecraft.util.Mth;
18: import org.joml.Matrix4f;
19: 
20: import java.util.List;
21: 
22: /**
23:  * 垂丝主面。宽度和高只读已经同步的盒子，不再乘聚能，也不画厚度方向的交叉片。
24:  * 一份面配 NO_CULL，正反都看得见。碰撞厚度仍由水段逻辑维护。
25:  */
26: public final class WaterCurtainRenderer extends RenderStateShard {
27:     private static final ResourceLocation LEFT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
28:             XueXiMod.MOD_ID, "textures/entity/xueximod/waterfall_runtime_left.png");
29:     private static final ResourceLocation MIDDLE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
30:             XueXiMod.MOD_ID, "textures/entity/xueximod/waterfall_runtime_middle.png");
31:     private static final ResourceLocation RIGHT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
32:             XueXiMod.MOD_ID, "textures/entity/xueximod/waterfall_runtime_right.png");
33:     private static final ResourceLocation THIN_TEXTURE = ResourceLocation.fromNamespaceAndPath(
34:             XueXiMod.MOD_ID, "textures/entity/xueximod/waterfall_thin.png");
35:     private static final RenderType LEFT_MESH = curtainMesh("xueximod_waterfall_runtime_left", LEFT_TEXTURE);
36:     private static final RenderType MIDDLE_MESH = curtainMesh("xueximod_waterfall_runtime_middle", MIDDLE_TEXTURE);
37:     private static final RenderType RIGHT_MESH = curtainMesh("xueximod_waterfall_runtime_right", RIGHT_TEXTURE);
38:     private static final RenderType THIN_MESH = curtainMesh("xueximod_waterfall_thin", THIN_TEXTURE);
39:     private static final int HEAVY_RED = 210;
40:     private static final int HEAVY_GREEN = 232;
41:     private static final int HEAVY_BLUE = 255;
42: 
43:     private WaterCurtainRenderer() {
44:         super("xueximod_waterfall_curtain", () -> {
45:         }, () -> {
46:         });
47:     }
48: 
49:     public static void render(TechniqueHitEntity entity, boolean heavy, float partialTicks, PoseStack pose, MultiBufferSource buffer) {
50:         double curtainWidth = entity.width();
51:         double curtainHeight = entity.height();
52:         int phaseTicks = Math.floorMod(entity.getId(), WaterCurtainLayout.FRAME_COUNT);
53:         int frameIndex = WaterCurtainLayout.flowFrame(entity.tickCount, partialTicks, phaseTicks);
54:         int drainStart = WaterCurtainLayout.drainStart(entity.lifetime());
55:         List<Panel> panels = WaterCurtainLayout.panels(curtainWidth, curtainHeight, frameIndex, drainStart);
56:         if (panels.isEmpty()) {
57:             return;
58:         }
59:         float turn = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot()) * ((float) Math.PI / 180.0F);
60:         float aheadX = -Mth.sin(turn);
61:         float aheadZ = Mth.cos(turn);
62:         float rightX = aheadZ;
63:         float rightZ = -aheadX;
64:         float normalX = -aheadX;
65:         float normalZ = -aheadZ;
66:         int colorRed = heavy ? HEAVY_RED : 255;
67:         int colorGreen = heavy ? HEAVY_GREEN : 255;
68:         int colorBlue = heavy ? HEAVY_BLUE : 255;
69:         Matrix4f matrix = pose.last().pose();
70:         int packedLight = LightTexture.FULL_BRIGHT;
71:         for (Panel panel : panels) {
72:             VertexConsumer consumer = buffer.getBuffer(meshFor(panel.piece));
73:             submit(consumer, matrix, packedLight, panel, rightX, rightZ, normalX, normalZ,
74:                     colorRed, colorGreen, colorBlue);
75:         }
76:     }
77: 
78:     private static RenderType meshFor(Piece piece) {
79:         return switch (piece) {
80:             case LEFT -> LEFT_MESH;
81:             case MIDDLE -> MIDDLE_MESH;
82:             case RIGHT -> RIGHT_MESH;
83:             case THIN -> THIN_MESH;
84:         };
85:     }
86: 
87:     private static RenderType curtainMesh(String meshName, ResourceLocation texture) {
88:         return RenderType.create(
89:                 meshName,
90:                 DefaultVertexFormat.NEW_ENTITY,
91:                 VertexFormat.Mode.QUADS,
92:                 65536,
93:                 true,
94:                 true,
95:                 RenderType.CompositeState.builder()
96:                         .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
97:                         .setTextureState(new TextureStateShard(texture, false, false))
98:                         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
99:                         .setCullState(NO_CULL)
100:                         .setLightmapState(LIGHTMAP)
101:                         .setOverlayState(OVERLAY)
102:                         .setDepthTestState(LEQUAL_DEPTH_TEST)
103:                         .setWriteMaskState(COLOR_WRITE)
104:                         .createCompositeState(true));
105:     }
106: 
107:     private static void submit(VertexConsumer consumer, Matrix4f matrix, int packedLight, Panel panel,
108:                                float rightX, float rightZ, float normalX, float normalZ,
109:                                int colorRed, int colorGreen, int colorBlue) {
110:         vertex(consumer, matrix, packedLight, panel.alongStart, panel.topOffset, rightX, rightZ,
111:                 panel.textureUStart, panel.textureVStart, normalX, normalZ, colorRed, colorGreen, colorBlue);
112:         vertex(consumer, matrix, packedLight, panel.alongEnd, panel.topOffset, rightX, rightZ,
113:                 panel.textureUEnd, panel.textureVStart, normalX, normalZ, colorRed, colorGreen, colorBlue);
114:         vertex(consumer, matrix, packedLight, panel.alongEnd, panel.bottomOffset, rightX, rightZ,
115:                 panel.textureUEnd, panel.textureVEnd, normalX, normalZ, colorRed, colorGreen, colorBlue);
116:         vertex(consumer, matrix, packedLight, panel.alongStart, panel.bottomOffset, rightX, rightZ,
117:                 panel.textureUStart, panel.textureVEnd, normalX, normalZ, colorRed, colorGreen, colorBlue);
118:     }
119: 
120:     /**
121:      * 顶点是实体中心的局部偏移。实体渲染调度器已经用 double 做过实体坐标减相机，
122:      * 再把这个小差值写入姿态。这里只在相乘之后转 float，不把世界坐标直接转 float。
123:      */
124:     private static void vertex(VertexConsumer consumer, Matrix4f matrix, int packedLight,
125:                                double along, double vertical, float rightX, float rightZ,
126:                                double textureU, double textureV,
127:                                float normalX, float normalZ,
128:                                int colorRed, int colorGreen, int colorBlue) {
129:         float pointX = (float) (along * rightX);
130:         float pointY = (float) vertical;
131:         float pointZ = (float) (along * rightZ);
132:         consumer.vertex(matrix, pointX, pointY, pointZ)
133:                 .color(colorRed, colorGreen, colorBlue, 255)
134:                 .uv((float) textureU, (float) textureV)
135:                 .overlayCoords(OverlayTexture.NO_OVERLAY)
136:                 .uv2(packedLight)
137:                 .normal(normalX, 0.0F, normalZ)
138:                 .endVertex();
139:     }
140: }
141: 

### CHECK
package com.bslx.XueXiMod.shu.technique.release.entity;

import com.bslx.XueXiMod.client.WaterCurtainLayout;
import com.bslx.XueXiMod.client.WaterCurtainLayout.Panel;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.WaterSegment;
import com.bslx.XueXiMod.shu.technique.release.WeepingCurtain;
import com.bslx.XueXiMod.shu.technique.release.WeepingSettings;

import java.util.List;

/**
 * 命中实例寿命和垂丝相位。直接调用生产方法，不复制公式，不生成实体。
 */
public final class ReleaseEntityEnergyCheck {
    private static final double FOOT = 5.62D;
    private static int assertionCount;
    private static int failures;

    private ReleaseEntityEnergyCheck() {
    }

    public static void main(String[] arguments) {
        productionLifetimes();
        templateStaysPut();
        weepingPhases();
        curtainClock();
        System.out.println("EXECUTED entity-lifetime-weeping-phase");
        System.out.println("未执行 实体 tick、NBT 往返和客户端渲染：需要世界或实体");
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void productionLifetimes() {
        ReleaseInput baseline = ReleaseInput.BASE;
        ReleaseInput doubled = new ReleaseInput(2f);
        ReleaseInput quadrupled = new ReleaseInput(4f);
        check(TechniqueHitSpec.instanceLifetime(40, baseline, false) == 40, "基准 40 被改短或拉长");
        check(TechniqueHitSpec.instanceLifetime(40, doubled, false) == 80, "两倍没有把 40 延长到 80");
        check(TechniqueHitSpec.instanceLifetime(40, quadrupled, false) == 160, "四倍没有把 40 延长到 160");
        check(TechniqueHitSpec.instanceLifetime(20, baseline, false) == 20, "默认 20 没有保持");
        check(TechniqueHitSpec.instanceLifetime(20, doubled, false) == 40, "两倍没有把 20 延长到 40");
        check(TechniqueHitSpec.instanceLifetime(20, quadrupled, false) == 80, "四倍没有把 20 延长到 80");
        check(TechniqueHitSpec.instanceLifetime(40, quadrupled, true) == 40, "弧带窗口被寿命倍率改了");
        check(TechniqueHitSpec.instanceLifetime(0, quadrupled, false) == 0, "零寿命被抬成正数");
        check(TechniqueHitSpec.instanceLifetime(-4, quadrupled, false) == -4, "负寿命被改写");
    }

    private static void templateStaysPut() {
        TechniqueHitSpec template = new TechniqueHitSpec();
        template.lifetime = 40;
        template.hitInterval = 10;
        template.hitReadyAge = 3;
        template.speed = 1.25D;
        ReleaseInput doubled = new ReleaseInput(2f);
        int firstPass = TechniqueHitSpec.instanceLifetime(template.lifetime, doubled, false);
        int secondPass = TechniqueHitSpec.instanceLifetime(template.lifetime, doubled, false);
        check(firstPass == 80 && secondPass == 80, "同一模板第二次被乘成 160，实际 " + firstPass + " " + secondPass);
        check(template.lifetime == 40, "模板寿命被写回");
        check(template.hitInterval == 10 && template.hitReadyAge == 3, "命中间隔或就绪年龄被带进寿命缩放");
        check(Math.abs(template.speed - 1.25D) < 1.0E-9D, "速度被寿命缩放改写");
    }

    private static void weepingPhases() {
        check(WeepingSettings.FILL_TICKS == 8, "填充不再是 8");
        check(WeepingSettings.drainSpan() == 8, "排空窗口不再是 8");
        check(WeepingSettings.LIFETIME == 40 && WeepingSettings.DRAIN_START == 32, "基准寿命或基准排空起点变了");
        checkPhase(40, 32);
        checkPhase(80, 72);
        checkPhase(160, 152);
        net.minecraft.world.phys.Vec3 lip = new net.minecraft.world.phys.Vec3(3.0D, 10.0D, 4.0D);
        net.minecraft.world.phys.Vec3 look = new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D);
        WaterSegment legacy = WeepingCurtain.flowSegment(lip, look, FOOT, true, 0.5D, 0.35D, 36);
        WaterSegment explicit = WeepingCurtain.flowSegment(lip, look, FOOT, true, 0.5D, 0.35D, 36, 40);
        check(Math.abs(legacy.upperDrop() - explicit.upperDrop()) < 1.0E-9D, "缺省相位不再等于寿命 40");
        WaterSegment held = WeepingCurtain.flowSegment(lip, look, FOOT, true, 0.5D, 0.35D, 40, 80);
        check(held.present() && Math.abs(held.upperDrop()) < 1.0E-9D, "80 拍寿命在第 40 拍已经开始排空");
        WaterSegment lateHold = WeepingCurtain.flowSegment(lip, look, FOOT, true, 0.5D, 0.35D, 80, 160);
        check(lateHold.present() && Math.abs(lateHold.upperDrop()) < 1.0E-9D, "160 拍寿命在第 80 拍已经开始排空");
        WeepingCurtain.FootClip landed = new WeepingCurtain.FootClip(FOOT, true, false, false, 1L);
        WaterSegment full = WeepingCurtain.flowSegment(lip, look, FOOT, true, 0.5D, 0.35D, 8, 160);
        check(WeepingCurtain.splashes(8, landed, full), "溅水没有留在第 8 拍");
        check(!WeepingCurtain.splashes(16, landed, full), "长寿命把溅水推迟了");
    }

    private static void checkPhase(int lifeTicks, int drainStart) {
        net.minecraft.world.phys.Vec3 lip = new net.minecraft.world.phys.Vec3(3.0D, 10.0D, 4.0D);
        net.minecraft.world.phys.Vec3 look = new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D);
        check(WeepingSettings.drainStartFor(lifeTicks) == drainStart, lifeTicks + " 的排空起点不是 " + drainStart);
        check(WaterCurtainLayout.drainStart(lifeTicks) == drainStart, "客户端排空起点和水体不一致");
        WaterSegment closed = flowing(lip, look, 0, lifeTicks);
        check(!closed.present(), lifeTicks + " 在 0 拍就有水");
        WaterSegment growing = flowing(lip, look, 4, lifeTicks);
        check(Math.abs(growing.upperDrop()) < 1.0E-9D, lifeTicks + " 填充时上沿离开了唇口");
        check(Math.abs(growing.lowerDrop() - FOOT * 0.5D) < 1.0E-9D, lifeTicks + " 第 4 拍没有走到一半");
        WaterSegment full = flowing(lip, look, 8, lifeTicks);
        check(full.present() && Math.abs(full.lowerDrop() - FOOT) < 1.0E-9D && Math.abs(full.upperDrop()) < 1.0E-9D,
                lifeTicks + " 第 8 拍不是满流");
        WaterSegment held = flowing(lip, look, drainStart, lifeTicks);
        check(held.present() && Math.abs(held.upperDrop()) < 1.0E-9D && Math.abs(held.lowerDrop() - FOOT) < 1.0E-9D,
                lifeTicks + " 在排空起点已经收水");
        WaterSegment draining = flowing(lip, look, drainStart + 4, lifeTicks);
        check(draining.present() && Math.abs(draining.upperDrop() - FOOT * 0.5D) < 1.0E-9D,
                lifeTicks + " 排空过半时上沿不对");
        check(Math.abs(draining.lowerDrop() - FOOT) < 1.0E-9D, lifeTicks + " 排空时下沿离开了足端");
        WaterSegment ended = flowing(lip, look, lifeTicks, lifeTicks);
        check(!ended.present(), lifeTicks + " 到期后还留着水");
    }

    private static WaterSegment flowing(net.minecraft.world.phys.Vec3 lip, net.minecraft.world.phys.Vec3 look,
                                        int age, int lifeTicks) {
        return WeepingCurtain.flowSegment(lip, look, FOOT, true, 0.5D, 0.35D, age, lifeTicks);
    }

    private static void curtainClock() {
        check(WaterCurtainLayout.flowFrame(0, 0.0F, 0) == 0, "0 拍贴图不是第 0 帧");
        check(WaterCurtainLayout.flowFrame(2, 0.0F, 0) == 1, "贴图不再是每 2 拍一帧");
        check(WaterCurtainLayout.flowFrame(4, 0.0F, 0) == 2, "第 4 拍没有落到第 2 帧");
        int drainStart = WaterCurtainLayout.drainStart(160);
        List<Panel> phased = WaterCurtainLayout.panels(1.0D, 2.0D, 0, drainStart);
        List<Panel> plain = WaterCurtainLayout.panels(1.0D, 2.0D, 0);
        check(!phased.isEmpty() && phased.size() == plain.size(), "正常排空起点把水帘分件清空了");
        check(WaterCurtainLayout.panels(1.0D, 2.0D, 0, Integer.MAX_VALUE).isEmpty(), "哨兵排空起点仍在画分件");
    }

    private static void check(boolean passed, String message) {
        assertionCount++;
        if (!passed) {
            failures++;
            System.out.println("失败 " + message);
        }
    }
}
