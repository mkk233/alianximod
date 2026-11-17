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


### DIFF src/main/java/com/bslx/XueXiMod/shu/technique/release/conditional/ConditionalEffect.java
diff --git "a/D:/alianximod/docs/\345\274\200\345\217\221\350\256\260\345\275\225/\350\201\232\350\203\275\350\276\223\345\205\245\351\223\276\346\240\270\346\237\245-20261027/before/src/main/java/com/bslx/XueXiMod/shu/technique/release/conditional/ConditionalEffect.java" b/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/conditional/ConditionalEffect.java
index 2cf351d..0f7ff58 100644
--- "a/D:/alianximod/docs/\345\274\200\345\217\221\350\256\260\345\275\225/\350\201\232\350\203\275\350\276\223\345\205\245\351\223\276\346\240\270\346\237\245-20261027/before/src/main/java/com/bslx/XueXiMod/shu/technique/release/conditional/ConditionalEffect.java"
+++ b/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/conditional/ConditionalEffect.java
@@ -1,6 +1,7 @@
 package com.bslx.XueXiMod.shu.technique.release.conditional;
 
 import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
+import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
 import net.minecraft.server.level.ServerPlayer;
 
 import java.util.UUID;
@@ -10,7 +11,12 @@ import java.util.UUID;
  * 成功只认 {@link #attempt} 的提交结果，不靠正伤害、领域、护体或命中实体凑回执。
  */
 public interface ConditionalEffect {
-    ConditionalResult attempt(ServerPlayer caster, UUID operationId, EnchantmentSnapshot contact);
+    ConditionalResult attempt(ServerPlayer caster, UUID operationId, EnchantmentSnapshot contact, ReleaseInput input);
+
+    /** 旧调用不带链输入时按基准倍率。 */
+    default ConditionalResult attempt(ServerPlayer caster, UUID operationId, EnchantmentSnapshot contact) {
+        return this.attempt(caster, operationId, contact, ReleaseInput.BASE);
+    }
 
     /** 同一意图的复制份只尝试一次，基费也不乘份数。默认关闭。 */
     default boolean copiesCoalesce() {

### CURRENT src/main/java/com/bslx/XueXiMod/shu/technique/release/conditional/ConditionalEffect.java
1: package com.bslx.XueXiMod.shu.technique.release.conditional;
2: 
3: import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
4: import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
5: import net.minecraft.server.level.ServerPlayer;
6: 
7: import java.util.UUID;
8: 
9: /**
10:  * 零伤条件释放。旧术式不实现它，释放定义默认没有。
11:  * 成功只认 {@link #attempt} 的提交结果，不靠正伤害、领域、护体或命中实体凑回执。
12:  */
13: public interface ConditionalEffect {
14:     ConditionalResult attempt(ServerPlayer caster, UUID operationId, EnchantmentSnapshot contact, ReleaseInput input);
15: 
16:     /** 旧调用不带链输入时按基准倍率。 */
17:     default ConditionalResult attempt(ServerPlayer caster, UUID operationId, EnchantmentSnapshot contact) {
18:         return this.attempt(caster, operationId, contact, ReleaseInput.BASE);
19:     }
20: 
21:     /** 同一意图的复制份只尝试一次，基费也不乘份数。默认关闭。 */
22:     default boolean copiesCoalesce() {
23:         return false;
24:     }
25: 
26:     /** 零伤宿主可以承接有接触伤害的附魔。默认关闭。 */
27:     default boolean supportsContact() {
28:         return false;
29:     }
30: }
31: 

### DIFF src/main/java/com/bslx/XueXiMod/shu/technique/release/conditional/WaterPrisonEffect.java
diff --git "a/D:/alianximod/docs/\345\274\200\345\217\221\350\256\260\345\275\225/\350\201\232\350\203\275\350\276\223\345\205\245\351\223\276\346\240\270\346\237\245-20261027/before/src/main/java/com/bslx/XueXiMod/shu/technique/release/conditional/WaterPrisonEffect.java" b/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/conditional/WaterPrisonEffect.java
index e6e3686..bcafa4c 100644
--- "a/D:/alianximod/docs/\345\274\200\345\217\221\350\256\260\345\275\225/\350\201\232\350\203\275\350\276\223\345\205\245\351\223\276\346\240\270\346\237\245-20261027/before/src/main/java/com/bslx/XueXiMod/shu/technique/release/conditional/WaterPrisonEffect.java"
+++ b/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/conditional/WaterPrisonEffect.java
@@ -1,6 +1,7 @@
 package com.bslx.XueXiMod.shu.technique.release.conditional;
 
 import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
+import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
 import com.bslx.XueXiMod.shu.technique.release.prison.PrisonCommitResult;
 import com.bslx.XueXiMod.shu.technique.release.prison.PrisonReservation;
 import com.bslx.XueXiMod.shu.technique.release.prison.PrisonService;
@@ -31,7 +32,8 @@ public final class WaterPrisonEffect implements ConditionalEffect {
     }
 
     @Override
-    public ConditionalResult attempt(ServerPlayer caster, UUID operationId, EnchantmentSnapshot contact) {
+    public ConditionalResult attempt(ServerPlayer caster, UUID operationId, EnchantmentSnapshot contact, ReleaseInput input) {
+        ReleaseInput frozenInput = input == null ? ReleaseInput.BASE : input;
         if (caster == null || operationId == null) {
             return ConditionalResult.rejected("没有可禁锢的潮湿目标");
         }
@@ -39,7 +41,7 @@ public final class WaterPrisonEffect implements ConditionalEffect {
         if (target == null) {
             return ConditionalResult.rejected("没有可禁锢的潮湿目标");
         }
-        PrisonReservation reservation = PrisonService.prepare(caster, target, operationId);
+        PrisonReservation reservation = PrisonService.prepare(caster, target, operationId, frozenInput);
         if (reservation == null) {
             return ConditionalResult.rejected("水牢没有成立");
         }
@@ -50,7 +52,7 @@ public final class WaterPrisonEffect implements ConditionalEffect {
                 because = reservation.reason();
                 return ConditionalResult.rejected(because == null || because.isEmpty() ? "水牢没有成立" : because);
             }
-            PrisonCommitResult committed = reservation.commit(contact);
+            PrisonCommitResult committed = reservation.commit(contact, frozenInput);
             established = committed != null && committed.committed();
             if (!established) {
                 because = committed == null ? "" : committed.reason();

### CURRENT src/main/java/com/bslx/XueXiMod/shu/technique/release/conditional/WaterPrisonEffect.java
1: package com.bslx.XueXiMod.shu.technique.release.conditional;
2: 
3: import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
4: import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
5: import com.bslx.XueXiMod.shu.technique.release.prison.PrisonCommitResult;
6: import com.bslx.XueXiMod.shu.technique.release.prison.PrisonReservation;
7: import com.bslx.XueXiMod.shu.technique.release.prison.PrisonService;
8: import net.minecraft.server.level.ServerPlayer;
9: import net.minecraft.world.entity.LivingEntity;
10: 
11: import java.util.UUID;
12: 
13: /**
14:  * 水牢只适配牢服务的选择、准备和提交。牢的状态、禁锢和结算不在这里。
15:  * 目标始终是施术者准星上的单个潮湿生物，不用动作撞到的那一个代替。
16:  */
17: public final class WaterPrisonEffect implements ConditionalEffect {
18:     public static final WaterPrisonEffect INSTANCE = new WaterPrisonEffect();
19:     private static final double TARGET_RANGE = 20.0D;
20: 
21:     private WaterPrisonEffect() {
22:     }
23: 
24:     @Override
25:     public boolean copiesCoalesce() {
26:         return true;
27:     }
28: 
29:     @Override
30:     public boolean supportsContact() {
31:         return true;
32:     }
33: 
34:     @Override
35:     public ConditionalResult attempt(ServerPlayer caster, UUID operationId, EnchantmentSnapshot contact, ReleaseInput input) {
36:         ReleaseInput frozenInput = input == null ? ReleaseInput.BASE : input;
37:         if (caster == null || operationId == null) {
38:             return ConditionalResult.rejected("没有可禁锢的潮湿目标");
39:         }
40:         LivingEntity target = PrisonService.selectTarget(caster, TARGET_RANGE);
41:         if (target == null) {
42:             return ConditionalResult.rejected("没有可禁锢的潮湿目标");
43:         }
44:         PrisonReservation reservation = PrisonService.prepare(caster, target, operationId, frozenInput);
45:         if (reservation == null) {
46:             return ConditionalResult.rejected("水牢没有成立");
47:         }
48:         boolean established = false;
49:         String because = "";
50:         try {
51:             if (!reservation.acquired()) {
52:                 because = reservation.reason();
53:                 return ConditionalResult.rejected(because == null || because.isEmpty() ? "水牢没有成立" : because);
54:             }
55:             PrisonCommitResult committed = reservation.commit(contact, frozenInput);
56:             established = committed != null && committed.committed();
57:             if (!established) {
58:                 because = committed == null ? "" : committed.reason();
59:             }
60:         } finally {
61:             reservation.close();
62:         }
63:         if (established) {
64:             return ConditionalResult.committed();
65:         }
66:         return ConditionalResult.rejected(because == null || because.isEmpty() ? "水牢没有成立" : because);
67:     }
68: }
69: 

### DIFF src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonService.java
diff --git "a/D:/alianximod/docs/\345\274\200\345\217\221\350\256\260\345\275\225/\350\201\232\350\203\275\350\276\223\345\205\245\351\223\276\346\240\270\346\237\245-20261027/before/src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonService.java" b/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonService.java
index da6068e..d0fb6b2 100644
--- "a/D:/alianximod/docs/\345\274\200\345\217\221\350\256\260\345\275\225/\350\201\232\350\203\275\350\276\223\345\205\245\351\223\276\346\240\270\346\237\245-20261027/before/src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonService.java"
+++ b/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonService.java
@@ -1,6 +1,7 @@
 package com.bslx.XueXiMod.shu.technique.release.prison;
 
 import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
+import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
 import net.minecraft.server.level.ServerPlayer;
 import net.minecraft.world.entity.LivingEntity;
 
@@ -13,7 +14,11 @@ public final class PrisonService {
     }
 
     public static PrisonReservation prepare(ServerPlayer caster, LivingEntity target, UUID operationId) {
-        return PrisonRuntime.prepare(caster, target, operationId);
+        return prepare(caster, target, operationId, ReleaseInput.BASE);
+    }
+
+    public static PrisonReservation prepare(ServerPlayer caster, LivingEntity target, UUID operationId, ReleaseInput input) {
+        return PrisonRuntime.prepare(caster, target, operationId, input);
     }
 
     public static Optional<PrisonView> view(UUID targetId) {
@@ -38,4 +43,11 @@ public final class PrisonService {
         }
         return reservation.commit(contact);
     }
+
+    public static PrisonCommitResult commit(PrisonReservation reservation, EnchantmentSnapshot contact, ReleaseInput input) {
+        if (reservation == null) {
+            return PrisonCommitResult.fail("missing");
+        }
+        return reservation.commit(contact, input);
+    }
 }

### CURRENT src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonService.java
1: package com.bslx.XueXiMod.shu.technique.release.prison;
2: 
3: import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
4: import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
5: import net.minecraft.server.level.ServerPlayer;
6: import net.minecraft.world.entity.LivingEntity;
7: 
8: import java.util.Optional;
9: import java.util.UUID;
10: 
11: /** 水牢服务端入口。不引用客户端类型，也不扣魔力。 */
12: public final class PrisonService {
13:     private PrisonService() {
14:     }
15: 
16:     public static PrisonReservation prepare(ServerPlayer caster, LivingEntity target, UUID operationId) {
17:         return prepare(caster, target, operationId, ReleaseInput.BASE);
18:     }
19: 
20:     public static PrisonReservation prepare(ServerPlayer caster, LivingEntity target, UUID operationId, ReleaseInput input) {
21:         return PrisonRuntime.prepare(caster, target, operationId, input);
22:     }
23: 
24:     public static Optional<PrisonView> view(UUID targetId) {
25:         return PrisonRuntime.view(targetId);
26:     }
27: 
28:     public static boolean end(UUID prisonId, EndReason reason) {
29:         return PrisonRuntime.end(prisonId, reason);
30:     }
31: 
32:     public static boolean suppressesEnhancedSuffocation(LivingEntity entity) {
33:         return PrisonRuntime.suppressesEnhancedSuffocation(entity);
34:     }
35: 
36:     public static LivingEntity selectTarget(ServerPlayer caster, double range) {
37:         return PrisonAim.select(caster, range);
38:     }
39: 
40:     public static PrisonCommitResult commit(PrisonReservation reservation, EnchantmentSnapshot contact) {
41:         if (reservation == null) {
42:             return PrisonCommitResult.fail("missing");
43:         }
44:         return reservation.commit(contact);
45:     }
46: 
47:     public static PrisonCommitResult commit(PrisonReservation reservation, EnchantmentSnapshot contact, ReleaseInput input) {
48:         if (reservation == null) {
49:             return PrisonCommitResult.fail("missing");
50:         }
51:         return reservation.commit(contact, input);
52:     }
53: }
54: 

### DIFF src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonReservation.java
diff --git "a/D:/alianximod/docs/\345\274\200\345\217\221\350\256\260\345\275\225/\350\201\232\350\203\275\350\276\223\345\205\245\351\223\276\346\240\270\346\237\245-20261027/before/src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonReservation.java" b/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonReservation.java
index 85a5f93..2801f31 100644
--- "a/D:/alianximod/docs/\345\274\200\345\217\221\350\256\260\345\275\225/\350\201\232\350\203\275\350\276\223\345\205\245\351\223\276\346\240\270\346\237\245-20261027/before/src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonReservation.java"
+++ b/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonReservation.java
@@ -1,6 +1,7 @@
 package com.bslx.XueXiMod.shu.technique.release.prison;
 
 import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
+import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
 import net.minecraft.server.level.ServerPlayer;
 import net.minecraft.world.entity.LivingEntity;
 
@@ -13,16 +14,23 @@ public final class PrisonReservation implements AutoCloseable {
     private final LivingEntity target;
     private final boolean acquired;
     private final String reason;
+    private final ReleaseInput releaseInput;
     private boolean closed;
     private boolean committed;
     private PrisonCommitResult outcome;
 
-    PrisonReservation(UUID operationId, ServerPlayer caster, LivingEntity target, boolean acquired, String reason) {
+    PrisonReservation(UUID operationId, ServerPlayer caster, LivingEntity target, boolean acquired, String reason,
+                      ReleaseInput releaseInput) {
         this.operationId = operationId;
         this.caster = caster;
         this.target = target;
         this.acquired = acquired;
         this.reason = reason == null ? "" : reason;
+        this.releaseInput = releaseInput == null ? ReleaseInput.BASE : releaseInput;
+    }
+
+    public ReleaseInput releaseInput() {
+        return this.releaseInput;
     }
 
     public boolean acquired() {
@@ -34,6 +42,10 @@ public final class PrisonReservation implements AutoCloseable {
     }
 
     public PrisonCommitResult commit(EnchantmentSnapshot contact) {
+        return this.commit(contact, this.releaseInput);
+    }
+
+    public PrisonCommitResult commit(EnchantmentSnapshot contact, ReleaseInput offeredInput) {
         if (this.outcome != null) {
             return this.outcome;
         }
@@ -45,7 +57,7 @@ public final class PrisonReservation implements AutoCloseable {
             this.outcome = PrisonCommitResult.fail("closed");
             return this.outcome;
         }
-        this.outcome = PrisonRuntime.commit(this, contact);
+        this.outcome = PrisonRuntime.commit(this, contact, offeredInput);
         return this.outcome;
     }
 

### CURRENT src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonReservation.java
1: package com.bslx.XueXiMod.shu.technique.release.prison;
2: 
3: import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
4: import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
5: import net.minecraft.server.level.ServerPlayer;
6: import net.minecraft.world.entity.LivingEntity;
7: 
8: import java.util.UUID;
9: 
10: /** 一次释放会话里的目标占用。未提交时 close 放人；已提交时 close 不破牢。 */
11: public final class PrisonReservation implements AutoCloseable {
12:     private final UUID operationId;
13:     private final ServerPlayer caster;
14:     private final LivingEntity target;
15:     private final boolean acquired;
16:     private final String reason;
17:     private final ReleaseInput releaseInput;
18:     private boolean closed;
19:     private boolean committed;
20:     private PrisonCommitResult outcome;
21: 
22:     PrisonReservation(UUID operationId, ServerPlayer caster, LivingEntity target, boolean acquired, String reason,
23:                       ReleaseInput releaseInput) {
24:         this.operationId = operationId;
25:         this.caster = caster;
26:         this.target = target;
27:         this.acquired = acquired;
28:         this.reason = reason == null ? "" : reason;
29:         this.releaseInput = releaseInput == null ? ReleaseInput.BASE : releaseInput;
30:     }
31: 
32:     public ReleaseInput releaseInput() {
33:         return this.releaseInput;
34:     }
35: 
36:     public boolean acquired() {
37:         return this.acquired;
38:     }
39: 
40:     public String reason() {
41:         return this.reason;
42:     }
43: 
44:     public PrisonCommitResult commit(EnchantmentSnapshot contact) {
45:         return this.commit(contact, this.releaseInput);
46:     }
47: 
48:     public PrisonCommitResult commit(EnchantmentSnapshot contact, ReleaseInput offeredInput) {
49:         if (this.outcome != null) {
50:             return this.outcome;
51:         }
52:         if (!this.acquired) {
53:             this.outcome = PrisonCommitResult.fail(this.reason.isEmpty() ? "rejected" : this.reason);
54:             return this.outcome;
55:         }
56:         if (this.closed) {
57:             this.outcome = PrisonCommitResult.fail("closed");
58:             return this.outcome;
59:         }
60:         this.outcome = PrisonRuntime.commit(this, contact, offeredInput);
61:         return this.outcome;
62:     }
63: 
64:     @Override
65:     public void close() {
66:         if (this.closed) {
67:             return;
68:         }
69:         this.closed = true;
70:         PrisonRuntime.close(this);
71:     }
72: 
73:     UUID operationId() {
74:         return this.operationId;
75:     }
76: 
77:     ServerPlayer caster() {
78:         return this.caster;
79:     }
80: 
81:     LivingEntity target() {
82:         return this.target;
83:     }
84: 
85:     boolean wasCommitted() {
86:         return this.committed;
87:     }
88: 
89:     void markCommitted() {
90:         this.committed = true;
91:     }
92: }
93: 

### DIFF src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonShell.java
diff --git "a/D:/alianximod/docs/\345\274\200\345\217\221\350\256\260\345\275\225/\350\201\232\350\203\275\350\276\223\345\205\245\351\223\276\346\240\270\346\237\245-20261027/before/src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonShell.java" b/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonShell.java
index a54bb43..a2b090f 100644
--- "a/D:/alianximod/docs/\345\274\200\345\217\221\350\256\260\345\275\225/\350\201\232\350\203\275\350\276\223\345\205\245\351\223\276\346\240\270\346\237\245-20261027/before/src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonShell.java"
+++ b/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonShell.java
@@ -13,10 +13,16 @@ public final class PrisonShell {
     }
 
     public static AABB expand(AABB body) {
+        return expand(body, 1.0D);
+    }
+
+    /** 以目标包围盒中心放大原外壳。倍率不是有限正数时保持原外壳。 */
+    public static AABB expand(AABB body, double sizeMultiplier) {
         if (body == null) {
             return null;
         }
-        return body.inflate(PADDING);
+        AABB outer = body.inflate(PADDING);
+        return scaleAbout(body, outer, sizeMultiplier);
     }
 
     public static AABB innerOf(AABB outer) {
@@ -30,6 +36,32 @@ public final class PrisonShell {
         return inner;
     }
 
+    /** 原内盒按同一倍率从目标中心放大后冻结。不用固定填充从放大后的外壳反推。 */
+    public static AABB innerFor(AABB body, double sizeMultiplier) {
+        AABB inner = innerOf(expand(body, 1.0D));
+        if (body == null || inner == null) {
+            return null;
+        }
+        return scaleAbout(body, inner, sizeMultiplier);
+    }
+
+    private static AABB scaleAbout(AABB centerBox, AABB targetBox, double sizeMultiplier) {
+        double factor = sizeMultiplier;
+        if (!Double.isFinite(factor) || factor <= 0.0D) {
+            factor = 1.0D;
+        }
+        double centerX = (centerBox.minX + centerBox.maxX) * 0.5D;
+        double centerY = (centerBox.minY + centerBox.maxY) * 0.5D;
+        double centerZ = (centerBox.minZ + centerBox.maxZ) * 0.5D;
+        return new AABB(
+                centerX + (targetBox.minX - centerX) * factor,
+                centerY + (targetBox.minY - centerY) * factor,
+                centerZ + (targetBox.minZ - centerZ) * factor,
+                centerX + (targetBox.maxX - centerX) * factor,
+                centerY + (targetBox.maxY - centerY) * factor,
+                centerZ + (targetBox.maxZ - centerZ) * factor);
+    }
+
     public static boolean touches(AABB outer, AABB inner, LivingEntity candidate, UUID prisonerId) {
         if (outer == null || candidate == null || prisonerId == null || prisonerId.equals(candidate.getUUID())) {
             return false;

### CURRENT src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonShell.java
1: package com.bslx.XueXiMod.shu.technique.release.prison;
2: 
3: import net.minecraft.world.entity.LivingEntity;
4: import net.minecraft.world.phys.AABB;
5: 
6: import java.util.UUID;
7: 
8: /** 创建时把目标真实包围盒外扩 0.20 格作为壳。接触只算这层壳，不算盒子内部。 */
9: public final class PrisonShell {
10:     public static final double PADDING = 0.20D;
11: 
12:     private PrisonShell() {
13:     }
14: 
15:     public static AABB expand(AABB body) {
16:         return expand(body, 1.0D);
17:     }
18: 
19:     /** 以目标包围盒中心放大原外壳。倍率不是有限正数时保持原外壳。 */
20:     public static AABB expand(AABB body, double sizeMultiplier) {
21:         if (body == null) {
22:             return null;
23:         }
24:         AABB outer = body.inflate(PADDING);
25:         return scaleAbout(body, outer, sizeMultiplier);
26:     }
27: 
28:     public static AABB innerOf(AABB outer) {
29:         if (outer == null) {
30:             return null;
31:         }
32:         AABB inner = outer.inflate(-PADDING);
33:         if (inner.maxX <= inner.minX || inner.maxY <= inner.minY || inner.maxZ <= inner.minZ) {
34:             return null;
35:         }
36:         return inner;
37:     }
38: 
39:     /** 原内盒按同一倍率从目标中心放大后冻结。不用固定填充从放大后的外壳反推。 */
40:     public static AABB innerFor(AABB body, double sizeMultiplier) {
41:         AABB inner = innerOf(expand(body, 1.0D));
42:         if (body == null || inner == null) {
43:             return null;
44:         }
45:         return scaleAbout(body, inner, sizeMultiplier);
46:     }
47: 
48:     private static AABB scaleAbout(AABB centerBox, AABB targetBox, double sizeMultiplier) {
49:         double factor = sizeMultiplier;
50:         if (!Double.isFinite(factor) || factor <= 0.0D) {
51:             factor = 1.0D;
52:         }
53:         double centerX = (centerBox.minX + centerBox.maxX) * 0.5D;
54:         double centerY = (centerBox.minY + centerBox.maxY) * 0.5D;
55:         double centerZ = (centerBox.minZ + centerBox.maxZ) * 0.5D;
56:         return new AABB(
57:                 centerX + (targetBox.minX - centerX) * factor,
58:                 centerY + (targetBox.minY - centerY) * factor,
59:                 centerZ + (targetBox.minZ - centerZ) * factor,
60:                 centerX + (targetBox.maxX - centerX) * factor,
61:                 centerY + (targetBox.maxY - centerY) * factor,
62:                 centerZ + (targetBox.maxZ - centerZ) * factor);
63:     }
64: 
65:     public static boolean touches(AABB outer, AABB inner, LivingEntity candidate, UUID prisonerId) {
66:         if (outer == null || candidate == null || prisonerId == null || prisonerId.equals(candidate.getUUID())) {
67:             return false;
68:         }
69:         AABB body = candidate.getBoundingBox();
70:         if (body == null || !body.intersects(outer)) {
71:             return false;
72:         }
73:         if (inner == null) {
74:             return true;
75:         }
76:         return body.minX < inner.minX || body.maxX > inner.maxX
77:                 || body.minY < inner.minY || body.maxY > inner.maxY
78:                 || body.minZ < inner.minZ || body.maxZ > inner.maxZ;
79:     }
80: }
81: 

### DIFF src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonRuntime.java
diff --git "a/D:/alianximod/docs/\345\274\200\345\217\221\350\256\260\345\275\225/\350\201\232\350\203\275\350\276\223\345\205\245\351\223\276\346\240\270\346\237\245-20261027/before/src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonRuntime.java" b/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonRuntime.java
index d495d28..72a6492 100644
--- "a/D:/alianximod/docs/\345\274\200\345\217\221\350\256\260\345\275\225/\350\201\232\350\203\275\350\276\223\345\205\245\351\223\276\346\240\270\346\237\245-20261027/before/src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonRuntime.java"
+++ b/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonRuntime.java
@@ -2,6 +2,7 @@ package com.bslx.XueXiMod.shu.technique.release.prison;
 
 import com.bslx.XueXiMod.registry.ModMobEffects;
 import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
+import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
 import com.bslx.XueXiMod.shu.technique.release.contact.ContactCarrier;
 import com.bslx.XueXiMod.shu.technique.release.contact.ContactEnchantState;
 import com.bslx.XueXiMod.status.WetnessState;
@@ -40,29 +41,34 @@ public final class PrisonRuntime {
     }
 
     public static PrisonReservation prepare(ServerPlayer caster, LivingEntity target, UUID operationId) {
+        return prepare(caster, target, operationId, ReleaseInput.BASE);
+    }
+
+    public static PrisonReservation prepare(ServerPlayer caster, LivingEntity target, UUID operationId, ReleaseInput offeredInput) {
         if (operationId == null || caster == null || target == null) {
-            return rejected(null, caster, target, "missing");
+            return rejected(null, caster, target, "missing", bindInput(null, offeredInput));
         }
         if (!target.level().isClientSide()) {
             PrisonWetDue.settleLoaded(target);
         }
         PrisonReservation existing = SESSIONS.get(operationId);
+        ReleaseInput frozenInput = bindInput(existing, offeredInput);
         if (existing != null) {
             return existing;
         }
         if (caster.level().isClientSide() || target.level().isClientSide()) {
-            return rejected(operationId, caster, target, "client");
+            return rejected(operationId, caster, target, "client", frozenInput);
         }
         if (PRISON_BY_TARGET.containsKey(target.getUUID()) || PREPARED_TARGETS.containsKey(target.getUUID())) {
-            return rejected(operationId, caster, target, "occupied");
+            return rejected(operationId, caster, target, "occupied", frozenInput);
         }
-        PrisonReservation reservation = new PrisonReservation(operationId, caster, target, true, "");
+        PrisonReservation reservation = new PrisonReservation(operationId, caster, target, true, "", frozenInput);
         PREPARED_TARGETS.put(target.getUUID(), operationId);
         SESSIONS.put(operationId, reservation);
         return reservation;
     }
 
-    static PrisonCommitResult commit(PrisonReservation reservation, EnchantmentSnapshot contact) {
+    static PrisonCommitResult commit(PrisonReservation reservation, EnchantmentSnapshot contact, ReleaseInput offeredInput) {
         LivingEntity target = reservation.target();
         if (target == null || !target.isAlive() || target.isRemoved() || target.level().isClientSide()) {
             return PrisonCommitResult.fail("dead");
@@ -76,17 +82,21 @@ public final class PrisonRuntime {
         if (wet == null || current <= 0 || tier <= 0) {
             return PrisonCommitResult.fail("not_wet");
         }
-        int durationTicks = prisonTicks(tier, wet.getDuration());
+        ReleaseInput frozenInput = bindInput(reservation, offeredInput);
+        int durationTicks = scaledDuration(tier, wet.getDuration(), frozenInput);
         if (durationTicks <= 0) {
             return PrisonCommitResult.fail("duration");
         }
         dismount(target);
         Vec3 anchor = target.position();
-        AABB bounds = PrisonShell.expand(target.getBoundingBox());
+        AABB bodyBox = target.getBoundingBox();
+        double shellScale = frozenInput.sizeMultiplier();
+        AABB bounds = PrisonShell.expand(bodyBox, shellScale);
         if (anchor == null || bounds == null || reservation.caster() == null) {
             return PrisonCommitResult.fail("rejected");
         }
         long birthTick = level.getGameTime();
+        long endTick = ReleaseInput.expiryGameTime(birthTick, durationTicks);
         PrisonView view = new PrisonView(
                 UUID.randomUUID(),
                 REVISION,
@@ -97,8 +107,8 @@ public final class PrisonRuntime {
                 bounds,
                 tier,
                 birthTick,
-                birthTick + durationTicks);
-        PrisonRecord prison = new PrisonRecord(view, PrisonShell.innerOf(bounds), target, tier);
+                endTick);
+        PrisonRecord prison = new PrisonRecord(view, PrisonShell.innerFor(bodyBox, shellScale), target, tier);
         reservation.markCommitted();
         PREPARED_TARGETS.remove(target.getUUID());
         PRISONS.put(view.prisonId(), prison);
@@ -321,8 +331,30 @@ public final class PrisonRuntime {
         return PRISONS.get(prisonId);
     }
 
-    private static PrisonReservation rejected(UUID operationId, ServerPlayer caster, LivingEntity target, String reason) {
-        PrisonReservation reservation = new PrisonReservation(operationId, caster, target, false, reason);
+    /** 已有预约时保留首次输入。没有预约时采用这次提供的输入，空输入回基准。 */
+    static ReleaseInput bindInput(PrisonReservation existing, ReleaseInput offeredInput) {
+        if (existing != null && existing.releaseInput() != null) {
+            return existing.releaseInput();
+        }
+        if (offeredInput == null) {
+            return ReleaseInput.BASE;
+        }
+        return offeredInput;
+    }
+
+    /** 先按潮湿档算基准寿命，再只延长有限正寿命。非正基准原样返回。 */
+    static int scaledDuration(int tier, int wetRemaining, ReleaseInput frozenInput) {
+        int baseTicks = prisonTicks(tier, wetRemaining);
+        if (baseTicks <= 0) {
+            return baseTicks;
+        }
+        ReleaseInput safeInput = frozenInput == null ? ReleaseInput.BASE : frozenInput;
+        return safeInput.scaleDuration(baseTicks);
+    }
+
+    private static PrisonReservation rejected(UUID operationId, ServerPlayer caster, LivingEntity target, String reason,
+                                              ReleaseInput frozenInput) {
+        PrisonReservation reservation = new PrisonReservation(operationId, caster, target, false, reason, frozenInput);
         if (operationId != null) {
             SESSIONS.put(operationId, reservation);
         }

### CURRENT src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonRuntime.java
1: package com.bslx.XueXiMod.shu.technique.release.prison;
2: 
3: import com.bslx.XueXiMod.registry.ModMobEffects;
4: import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
5: import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
6: import com.bslx.XueXiMod.shu.technique.release.contact.ContactCarrier;
7: import com.bslx.XueXiMod.shu.technique.release.contact.ContactEnchantState;
8: import com.bslx.XueXiMod.status.WetnessState;
9: import com.mojang.logging.LogUtils;
10: import net.minecraft.server.MinecraftServer;
11: import net.minecraft.server.level.ServerLevel;
12: import net.minecraft.server.level.ServerPlayer;
13: import net.minecraft.world.effect.MobEffectInstance;
14: import net.minecraft.world.entity.Entity;
15: import net.minecraft.world.entity.LivingEntity;
16: import net.minecraft.world.phys.AABB;
17: import net.minecraft.world.phys.Vec3;
18: import org.slf4j.Logger;
19: 
20: import java.util.ArrayList;
21: import java.util.HashMap;
22: import java.util.IdentityHashMap;
23: import java.util.Map;
24: import java.util.Optional;
25: import java.util.UUID;
26: 
27: /** 占用、提交、结束和耗湿。操作号只留在本次预约里，关闭后丢掉。 */
28: public final class PrisonRuntime {
29:     private static final Logger LOGGER = LogUtils.getLogger();
30:     private static final int SHORT_TICKS = 20;
31:     private static final int LONG_TICKS = 100;
32:     private static final long REVISION = 1L;
33: 
34:     private static final Map<UUID, PrisonReservation> SESSIONS = new HashMap<>();
35:     private static final Map<UUID, UUID> PREPARED_TARGETS = new HashMap<>();
36:     private static final Map<UUID, PrisonRecord> PRISONS = new HashMap<>();
37:     private static final Map<UUID, UUID> PRISON_BY_TARGET = new HashMap<>();
38:     private static final IdentityHashMap<LivingEntity, PrisonRecord> BY_ENTITY = new IdentityHashMap<>();
39: 
40:     private PrisonRuntime() {
41:     }
42: 
43:     public static PrisonReservation prepare(ServerPlayer caster, LivingEntity target, UUID operationId) {
44:         return prepare(caster, target, operationId, ReleaseInput.BASE);
45:     }
46: 
47:     public static PrisonReservation prepare(ServerPlayer caster, LivingEntity target, UUID operationId, ReleaseInput offeredInput) {
48:         if (operationId == null || caster == null || target == null) {
49:             return rejected(null, caster, target, "missing", bindInput(null, offeredInput));
50:         }
51:         if (!target.level().isClientSide()) {
52:             PrisonWetDue.settleLoaded(target);
53:         }
54:         PrisonReservation existing = SESSIONS.get(operationId);
55:         ReleaseInput frozenInput = bindInput(existing, offeredInput);
56:         if (existing != null) {
57:             return existing;
58:         }
59:         if (caster.level().isClientSide() || target.level().isClientSide()) {
60:             return rejected(operationId, caster, target, "client", frozenInput);
61:         }
62:         if (PRISON_BY_TARGET.containsKey(target.getUUID()) || PREPARED_TARGETS.containsKey(target.getUUID())) {
63:             return rejected(operationId, caster, target, "occupied", frozenInput);
64:         }
65:         PrisonReservation reservation = new PrisonReservation(operationId, caster, target, true, "", frozenInput);
66:         PREPARED_TARGETS.put(target.getUUID(), operationId);
67:         SESSIONS.put(operationId, reservation);
68:         return reservation;
69:     }
70: 
71:     static PrisonCommitResult commit(PrisonReservation reservation, EnchantmentSnapshot contact, ReleaseInput offeredInput) {
72:         LivingEntity target = reservation.target();
73:         if (target == null || !target.isAlive() || target.isRemoved() || target.level().isClientSide()) {
74:             return PrisonCommitResult.fail("dead");
75:         }
76:         if (!(target.level() instanceof ServerLevel level)) {
77:             return PrisonCommitResult.fail("dimension");
78:         }
79:         MobEffectInstance wet = target.getEffect(ModMobEffects.WETNESS.get());
80:         int current = WetnessState.read(target);
81:         int tier = WetnessState.tierOf(current);
82:         if (wet == null || current <= 0 || tier <= 0) {
83:             return PrisonCommitResult.fail("not_wet");
84:         }
85:         ReleaseInput frozenInput = bindInput(reservation, offeredInput);
86:         int durationTicks = scaledDuration(tier, wet.getDuration(), frozenInput);
87:         if (durationTicks <= 0) {
88:             return PrisonCommitResult.fail("duration");
89:         }
90:         dismount(target);
91:         Vec3 anchor = target.position();
92:         AABB bodyBox = target.getBoundingBox();
93:         double shellScale = frozenInput.sizeMultiplier();
94:         AABB bounds = PrisonShell.expand(bodyBox, shellScale);
95:         if (anchor == null || bounds == null || reservation.caster() == null) {
96:             return PrisonCommitResult.fail("rejected");
97:         }
98:         long birthTick = level.getGameTime();
99:         long endTick = ReleaseInput.expiryGameTime(birthTick, durationTicks);
100:         PrisonView view = new PrisonView(
101:                 UUID.randomUUID(),
102:                 REVISION,
103:                 reservation.caster().getUUID(),
104:                 target.getUUID(),
105:                 level.dimension(),
106:                 anchor,
107:                 bounds,
108:                 tier,
109:                 birthTick,
110:                 endTick);
111:         PrisonRecord prison = new PrisonRecord(view, PrisonShell.innerFor(bodyBox, shellScale), target, tier);
112:         reservation.markCommitted();
113:         PREPARED_TARGETS.remove(target.getUUID());
114:         PRISONS.put(view.prisonId(), prison);
115:         PRISON_BY_TARGET.put(view.targetId(), view.prisonId());
116:         BY_ENTITY.put(target, prison);
117:         try {
118:             if (contact != null) {
119:                 prison.contact = ContactEnchantState.open(prison, contact);
120:             }
121:             PrisonEvents.created(view);
122:             if (prison.phase == Phase.COMMITTED && tier >= 4 && target.isAlive()) {
123:                 PrisonPressure.pulse(target);
124:             }
125:         } catch (RuntimeException failure) {
126:             LOGGER.error("water prison commit follow-up failed for {}", view.prisonId(), failure);
127:         }
128:         return PrisonCommitResult.success(view);
129:     }
130: 
131:     static void close(PrisonReservation reservation) {
132:         if (reservation.operationId() != null) {
133:             SESSIONS.remove(reservation.operationId(), reservation);
134:         }
135:         if (reservation.wasCommitted() || reservation.target() == null || reservation.operationId() == null) {
136:             return;
137:         }
138:         UUID holder = PREPARED_TARGETS.get(reservation.target().getUUID());
139:         if (reservation.operationId().equals(holder)) {
140:             PREPARED_TARGETS.remove(reservation.target().getUUID());
141:         }
142:     }
143: 
144:     public static Optional<PrisonView> view(UUID targetId) {
145:         PrisonRecord prison = prisonOf(targetId);
146:         if (prison == null || prison.phase != Phase.COMMITTED) {
147:             return Optional.empty();
148:         }
149:         return Optional.of(prison.view);
150:     }
151: 
152:     public static boolean end(UUID prisonId, EndReason reason) {
153:         if (prisonId == null) {
154:             return false;
155:         }
156:         PrisonRecord prison = PRISONS.get(prisonId);
157:         if (prison == null) {
158:             return false;
159:         }
160:         return prison.finish(reason == null ? EndReason.REMOVED : reason, prison.carrier());
161:     }
162: 
163:     public static boolean suppressesEnhancedSuffocation(LivingEntity entity) {
164:         PrisonRecord prison = find(entity);
165:         return prison != null && prison.phase == Phase.COMMITTED && prison.view.lockedTier() >= 4;
166:     }
167: 
168:     public static boolean locks(Entity entity) {
169:         return entity instanceof LivingEntity living && find(living) != null;
170:     }
171: 
172:     public static Vec3 anchor(Entity entity) {
173:         if (!(entity instanceof LivingEntity living)) {
174:             return null;
175:         }
176:         PrisonRecord prison = find(living);
177:         if (prison == null) {
178:             return null;
179:         }
180:         return prison.view.anchor();
181:     }
182: 
183:     public static boolean endTarget(LivingEntity locked, EndReason reason, LivingEntity wetCarrier) {
184:         PrisonRecord prison = find(locked);
185:         if (prison == null) {
186:             return false;
187:         }
188:         LivingEntity carrier = wetCarrier == null ? prison.carrier() : wetCarrier;
189:         return prison.finish(reason == null ? EndReason.REMOVED : reason, carrier);
190:     }
191: 
192:     public static void crossDimension(Entity original, Entity arrived) {
193:         if (!(original instanceof LivingEntity living) || arrived == null) {
194:             return;
195:         }
196:         LivingEntity carrier = arrived instanceof LivingEntity survivor && !survivor.isRemoved() ? survivor : living;
197:         endTarget(living, EndReason.DIMENSION, carrier);
198:     }
199: 
200:     public static void onConfirmedDeath(LivingEntity entity) {
201:         if (entity == null) {
202:             return;
203:         }
204:         endTarget(entity, EndReason.DEATH, entity);
205:         if (entity instanceof ServerPlayer player) {
206:             endCastBy(player, EndReason.CASTER_DEATH);
207:         }
208:     }
209: 
210:     public static void onLogout(ServerPlayer player) {
211:         if (player == null) {
212:             return;
213:         }
214:         endTarget(player, EndReason.LOGOUT, player);
215:         endCastBy(player, EndReason.CASTER_LOGOUT);
216:     }
217: 
218:     public static void onDimension(ServerPlayer player) {
219:         if (player == null) {
220:             return;
221:         }
222:         endTarget(player, EndReason.DIMENSION, player);
223:         endCastBy(player, EndReason.CASTER_DIMENSION);
224:     }
225: 
226:     /** 原版末地回主世界已经接受、玩家即将被丢掉之前的那次返回。普通换维事件不到这里。 */
227:     public static void onVanillaEndReturn(ServerPlayer player) {
228:         if (player == null) {
229:             return;
230:         }
231:         boolean wroteOwn = false;
232:         PrisonRecord own = find(player);
233:         if (own != null) {
234:             own.finish(EndReason.DIMENSION, player);
235:             wroteOwn = own.settled;
236:         }
237:         endCastBy(player, EndReason.CASTER_DIMENSION);
238:         if (wroteOwn) {
239:             PrisonWetTransfer.capture(player);
240:         }
241:     }
242: 
243:     public static void shutdown(MinecraftServer server) {
244:         if (server == null || PRISONS.isEmpty()) {
245:             return;
246:         }
247:         ArrayList<PrisonRecord> snapshot = new ArrayList<>(PRISONS.values());
248:         for (PrisonRecord prison : snapshot) {
249:             if (prison.phase != Phase.COMMITTED) {
250:                 continue;
251:             }
252:             LivingEntity carrier = prison.carrier();
253:             if (!holdsWritableWet(carrier)) {
254:                 LivingEntity loaded = findLive(server, prison.view.targetId());
255:                 if (holdsWritableWet(loaded)) {
256:                     carrier = loaded;
257:                 }
258:             }
259:             prison.finish(EndReason.REMOVED, carrier);
260:         }
261:     }
262: 
263:     public static void onWetRemoved(LivingEntity entity, MobEffectInstance instance) {
264:         if (entity == null || instance == null || com.bslx.XueXiMod.status.AttributeStateService.isDirectEdit()) {
265:             return;
266:         }
267:         if (instance.getEffect() != ModMobEffects.WETNESS.get()) {
268:             return;
269:         }
270:         endTarget(entity, EndReason.WET_CLEARED, entity);
271:     }
272: 
273:     public static void rebind(LivingEntity living) {
274:         if (living == null || living.level().isClientSide()) {
275:             return;
276:         }
277:         PrisonWetDue.settleLoaded(living);
278:         PrisonRecord prison = prisonOf(living.getUUID());
279:         if (prison == null || prison.phase != Phase.COMMITTED) {
280:             return;
281:         }
282:         long gameTime = living.level().getGameTime();
283:         if (prison.expiredWhileAway || gameTime >= prison.view.endTick()) {
284:             prison.finish(EndReason.DURATION, living);
285:             return;
286:         }
287:         prison.attach(living);
288:     }
289: 
290:     public static void tick(MinecraftServer server) {
291:         if (server == null || PRISONS.isEmpty()) {
292:             return;
293:         }
294:         ArrayList<PrisonRecord> snapshot = new ArrayList<>(PRISONS.values());
295:         for (PrisonRecord prison : snapshot) {
296:             prison.tick(server);
297:         }
298:     }
299: 
300:     private static void endCastBy(ServerPlayer player, EndReason reason) {
301:         ArrayList<PrisonRecord> owned = new ArrayList<>();
302:         for (PrisonRecord prison : PRISONS.values()) {
303:             if (player.getUUID().equals(prison.view.casterId())) {
304:                 owned.add(prison);
305:             }
306:         }
307:         for (PrisonRecord prison : owned) {
308:             prison.finish(reason, prison.carrier());
309:         }
310:     }
311: 
312:     private static PrisonRecord find(LivingEntity entity) {
313:         if (entity == null) {
314:             return null;
315:         }
316:         PrisonRecord direct = BY_ENTITY.get(entity);
317:         if (direct != null && direct.phase == Phase.COMMITTED && direct.prisoner == entity) {
318:             return direct;
319:         }
320:         return null;
321:     }
322: 
323:     private static PrisonRecord prisonOf(UUID targetId) {
324:         if (targetId == null) {
325:             return null;
326:         }
327:         UUID prisonId = PRISON_BY_TARGET.get(targetId);
328:         if (prisonId == null) {
329:             return null;
330:         }
331:         return PRISONS.get(prisonId);
332:     }
333: 
334:     /** 已有预约时保留首次输入。没有预约时采用这次提供的输入，空输入回基准。 */
335:     static ReleaseInput bindInput(PrisonReservation existing, ReleaseInput offeredInput) {
336:         if (existing != null && existing.releaseInput() != null) {
337:             return existing.releaseInput();
338:         }
339:         if (offeredInput == null) {
340:             return ReleaseInput.BASE;
341:         }
342:         return offeredInput;
343:     }
344: 
345:     /** 先按潮湿档算基准寿命，再只延长有限正寿命。非正基准原样返回。 */
346:     static int scaledDuration(int tier, int wetRemaining, ReleaseInput frozenInput) {
347:         int baseTicks = prisonTicks(tier, wetRemaining);
348:         if (baseTicks <= 0) {
349:             return baseTicks;
350:         }
351:         ReleaseInput safeInput = frozenInput == null ? ReleaseInput.BASE : frozenInput;
352:         return safeInput.scaleDuration(baseTicks);
353:     }
354: 
355:     private static PrisonReservation rejected(UUID operationId, ServerPlayer caster, LivingEntity target, String reason,
356:                                               ReleaseInput frozenInput) {
357:         PrisonReservation reservation = new PrisonReservation(operationId, caster, target, false, reason, frozenInput);
358:         if (operationId != null) {
359:             SESSIONS.put(operationId, reservation);
360:         }
361:         return reservation;
362:     }
363: 
364:     private static void dismount(LivingEntity target) {
365:         if (target.isPassenger()) {
366:             target.stopRiding();
367:         }
368:         if (target.isVehicle()) {
369:             target.ejectPassengers();
370:         }
371:     }
372: 
373:     private static int prisonTicks(int tier, int remaining) {
374:         if (tier == 1) {
375:             return SHORT_TICKS;
376:         }
377:         if (tier == 2) {
378:             return LONG_TICKS;
379:         }
380:         if (tier == 3) {
381:             if (remaining <= 0) {
382:                 return 0;
383:             }
384:             return Math.max(1, remaining / 2);
385:         }
386:         if (tier >= 4) {
387:             return remaining;
388:         }
389:         return 0;
390:     }
391: 
392:     static boolean settleCarrier(LivingEntity carrier, int lockedTier, EndReason reason) {
393:         if (!holdsWritableWet(carrier)) {
394:             return false;
395:         }
396:         if (reason == EndReason.WET_CLEARED) {
397:             WetnessState.clear(carrier);
398:             return true;
399:         }
400:         int current = WetnessState.read(carrier);
401:         if (current <= 0) {
402:             if (carrier.getEffect(ModMobEffects.WETNESS.get()) != null) {
403:                 WetnessState.clear(carrier);
404:             }
405:             return true;
406:         }
407:         if (lockedTier == 2) {
408:             WetnessState.replacePreservingDuration(carrier, current / 2);
409:             return true;
410:         }
411:         WetnessState.clear(carrier);
412:         return true;
413:     }
414: 
415:     private static boolean holdsWritableWet(LivingEntity entity) {
416:         if (entity == null || entity.level() == null || entity.level().isClientSide()) {
417:             return false;
418:         }
419:         if (!entity.isRemoved()) {
420:             return true;
421:         }
422:         return entity instanceof ServerPlayer
423:                 && entity.getRemovalReason() == Entity.RemovalReason.CHANGED_DIMENSION;
424:     }
425: 
426:     private static LivingEntity findLive(MinecraftServer server, UUID targetId) {
427:         if (server == null || targetId == null) {
428:             return null;
429:         }
430:         for (ServerLevel level : server.getAllLevels()) {
431:             Entity found = level.getEntity(targetId);
432:             if (found instanceof LivingEntity living && !living.isRemoved()) {
433:                 return living;
434:             }
435:         }
436:         return null;
437:     }
438: 
439:     private enum Phase {
440:         COMMITTED,
441:         ENDING,
442:         ENDED
443:     }
444: 
445:     private static final class PrisonRecord implements ContactCarrier {
446:         private final PrisonView view;
447:         private final AABB inner;
448:         private final int tier;
449:         private LivingEntity prisoner;
450:         private ContactEnchantState contact;
451:         private Phase phase = Phase.COMMITTED;
452:         private boolean settled;
453:         private boolean expiredWhileAway;
454:         private long nextPulse;
455: 
456:         private PrisonRecord(PrisonView view, AABB inner, LivingEntity prisoner, int tier) {
457:             this.view = view;
458:             this.inner = inner;
459:             this.prisoner = prisoner;
460:             this.tier = tier;
461:             this.nextPulse = tier >= 4 ? view.birthTick() + PrisonPressure.INTERVAL_TICKS : Long.MAX_VALUE;
462:         }
463: 
464:         private void tick(MinecraftServer server) {
465:             if (this.phase != Phase.COMMITTED) {
466:                 return;
467:             }
468:             ServerLevel level = server.getLevel(this.view.dimension());
469:             if (level == null) {
470:                 return;
471:             }
472:             long gameTime = level.getGameTime();
473:             LivingEntity live = resolve(level);
474:             if (live == null) {
475:                 if (this.prisoner != null && this.prisoner.isRemoved()) {
476:                     handleRemoval(server);
477:                     return;
478:                 }
479:                 if (gameTime >= this.view.endTick()) {
480:                     this.expiredWhileAway = true;
481:                 }
482:                 return;
483:             }
484:             if (live != this.prisoner) {
485:                 attach(live);
486:             }
487:             if (this.expiredWhileAway || gameTime >= this.view.endTick()) {
488:                 finish(EndReason.DURATION, live);
489:                 return;
490:             }
491:             if (this.tier >= 4 && gameTime >= this.nextPulse) {
492:                 this.nextPulse = gameTime + PrisonPressure.INTERVAL_TICKS;
493:                 PrisonPressure.pulse(live);
494:             }
495:         }
496: 
497:         private LivingEntity resolve(ServerLevel level) {
498:             Entity found = level.getEntity(this.view.targetId());
499:             if (found instanceof LivingEntity living && !living.isRemoved()) {
500:                 return living;
501:             }
502:             if (this.prisoner != null && !this.prisoner.isRemoved() && this.prisoner.level() == level) {
503:                 return this.prisoner;
504:             }
505:             return null;
506:         }
507: 
508:         private void handleRemoval(MinecraftServer server) {
509:             Entity.RemovalReason removal = this.prisoner.getRemovalReason();
510:             if (removal == Entity.RemovalReason.UNLOADED_TO_CHUNK) {
511:                 BY_ENTITY.remove(this.prisoner);
512:                 this.prisoner = null;
513:                 return;
514:             }
515:             if (removal == Entity.RemovalReason.CHANGED_DIMENSION) {
516:                 LivingEntity survivor = findLive(server, this.view.targetId());
517:                 finish(EndReason.DIMENSION, survivor == null ? this.prisoner : survivor);
518:                 return;
519:             }
520:             if (removal == Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
521:                 finish(EndReason.LOGOUT, this.prisoner);
522:                 return;
523:             }
524:             if (removal == Entity.RemovalReason.KILLED) {
525:                 finish(EndReason.DEATH, this.prisoner);
526:                 return;
527:             }
528:             finish(EndReason.REMOVED, this.prisoner);
529:         }
530: 
531:         private void attach(LivingEntity living) {
532:             if (this.prisoner != null && this.prisoner != living) {
533:                 BY_ENTITY.remove(this.prisoner);
534:             }
535:             this.prisoner = living;
536:             BY_ENTITY.put(living, this);
537:         }
538: 
539:         private LivingEntity carrier() {
540:             if (this.prisoner != null) {
541:                 return this.prisoner;
542:             }
543:             return null;
544:         }
545: 
546:         private boolean finish(EndReason reason, LivingEntity wetCarrier) {
547:             if (this.phase != Phase.COMMITTED) {
548:                 return false;
549:             }
550:             this.phase = Phase.ENDING;
551:             if (this.contact != null) {
552:                 this.contact.close();
553:                 this.contact = null;
554:             }
555:             if (this.prisoner != null) {
556:                 BY_ENTITY.remove(this.prisoner);
557:             }
558:             PRISONS.remove(this.view.prisonId());
559:             UUID mapped = PRISON_BY_TARGET.get(this.view.targetId());
560:             if (this.view.prisonId().equals(mapped)) {
561:                 PRISON_BY_TARGET.remove(this.view.targetId());
562:             }
563:             if (!this.settled) {
564:                 LivingEntity carrier = wetCarrier;
565:                 if (!holdsWritableWet(carrier)) {
566:                     MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
567:                     if (server != null) {
568:                         LivingEntity loaded = findLive(server, this.view.targetId());
569:                         if (holdsWritableWet(loaded)) {
570:                             carrier = loaded;
571:                         }
572:                     }
573:                 }
574:                 if (holdsWritableWet(carrier)) {
575:                     try {
576:                         if (settleCarrier(carrier, this.tier, reason)) {
577:                             this.settled = true;
578:                             PrisonWetDue.drop(this.view.prisonId(), this.view.dimension());
579:                         } else {
580:                             PrisonWetDue.keep(this.view.prisonId(), this.view.targetId(), this.view.dimension(), this.tier, reason);
581:                         }
582:                     } catch (RuntimeException failure) {
583:                         LOGGER.error("water prison wet settlement failed for {}", this.view.prisonId(), failure);
584:                         PrisonWetDue.keep(this.view.prisonId(), this.view.targetId(), this.view.dimension(), this.tier, reason);
585:                     }
586:                 } else {
587:                     PrisonWetDue.keep(this.view.prisonId(), this.view.targetId(), this.view.dimension(), this.tier, reason);
588:                 }
589:             }
590:             this.phase = Phase.ENDED;
591:             PrisonEvents.ended(this.view, reason);
592:             return true;
593:         }
594: 
595:         @Override
596:         public UUID hostId() {
597:             return this.view.prisonId();
598:         }
599: 
600:         @Override
601:         public int revision() {
602:             return (int) this.view.revision();
603:         }
604: 
605:         @Override
606:         public UUID ownerId() {
607:             return this.view.casterId();
608:         }
609: 
610:         @Override
611:         public boolean active() {
612:             return this.phase == Phase.COMMITTED;
613:         }
614: 
615:         @Override
616:         public boolean touches(LivingEntity target) {
617:             return PrisonShell.touches(this.view.bounds(), this.inner, target, this.view.targetId());
618:         }
619: 
620:         @Override
621:         public ServerLevel hostLevel() {
622:             if (this.prisoner != null && this.prisoner.level() instanceof ServerLevel level
623:                     && this.view.dimension().equals(level.dimension())) {
624:                 return level;
625:             }
626:             return null;
627:         }
628: 
629:         @Override
630:         public AABB queryBox() {
631:             return this.view.bounds();
632:         }
633:     }
634: }
635: 

### CHECK
package com.bslx.XueXiMod.shu.technique.release.prison;

import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;

import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * 水牢基准寿命、壳倍率和预约输入。直接调用生产方法，不构造生物。
 */
public final class PrisonEnergyCheck {
    private static int assertionCount;
    private static int failures;

    private PrisonEnergyCheck() {
    }

    public static void main(String[] arguments) {
        durations();
        shell();
        reservation();
        expiryAddition();
        System.out.println("EXECUTED prison-duration-shell-input");
        System.out.println("未执行 prepare 会话表和 commit 湿润生物：需要玩家和世界");
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void durations() {
        ReleaseInput baseline = ReleaseInput.BASE;
        ReleaseInput doubled = new ReleaseInput(2f);
        ReleaseInput quadrupled = new ReleaseInput(4f);
        check(PrisonRuntime.scaledDuration(1, 80, baseline) == 20, "一档基准不是 20");
        check(PrisonRuntime.scaledDuration(1, 80, doubled) == 40, "一档两倍不是 40");
        check(PrisonRuntime.scaledDuration(1, 80, quadrupled) == 80, "一档四倍不是 80");
        check(PrisonRuntime.scaledDuration(2, 1000, doubled) == 200, "二档两倍不是 200");
        check(PrisonRuntime.scaledDuration(2, 1000, quadrupled) == 400, "二档四倍不是 400");
        check(PrisonRuntime.scaledDuration(4, 80, doubled) == 160, "四档剩余 80 的两倍不是 160");
        check(PrisonRuntime.scaledDuration(4, 80, quadrupled) == 320, "四档剩余 80 的四倍不是 320");
        check(PrisonRuntime.scaledDuration(3, 5, doubled) == 4, "三档一半剩余没有先取基准再乘");
        check(PrisonRuntime.scaledDuration(3, 0, quadrupled) == 0, "三档非正剩余被抬成正数");
        check(PrisonRuntime.scaledDuration(4, -5, quadrupled) == -5, "四档负剩余被改写");
        check(PrisonRuntime.scaledDuration(0, 40, doubled) == 0, "无效档被乘出了寿命");
    }

    private static void shell() {
        AABB body = new AABB(0.0D, 64.0D, 0.0D, 1.0D, 66.0D, 1.0D);
        AABB outerOnce = PrisonShell.expand(body);
        AABB innerOnce = PrisonShell.innerOf(outerOnce);
        check(outerOnce != null && innerOnce != null, "基准壳是空的");
        if (outerOnce == null || innerOnce == null) {
            return;
        }
        AABB outerSame = PrisonShell.expand(body, 1.0D);
        AABB innerSame = PrisonShell.innerFor(body, 1.0D);
        check(sameBox(outerOnce, outerSame), "一倍外壳偏离了原 expand");
        check(sameBox(innerOnce, innerSame), "一倍内盒偏离了原 innerOf");
        expectScaled(body, outerOnce, innerOnce, 2.0D);
        expectScaled(body, outerOnce, innerOnce, 4.0D);
        AABB outerNan = PrisonShell.expand(body, Double.NaN);
        check(sameBox(outerOnce, outerNan), "非法倍率把外壳收掉了");
        check(PrisonShell.expand(null) == null && PrisonShell.expand(null, 2.0D) == null, "空外壳没有保持空");
        check(PrisonShell.innerFor(null, 2.0D) == null, "空内盒没有保持空");
    }

    private static void expectScaled(AABB body, AABB outerOnce, AABB innerOnce, double factor) {
        AABB outerScaled = PrisonShell.expand(body, factor);
        AABB innerScaled = PrisonShell.innerFor(body, factor);
        check(outerScaled != null && innerScaled != null, factor + " 倍壳是空的");
        if (outerScaled == null || innerScaled == null) {
            return;
        }
        check(sameScale(body, outerOnce, outerScaled, factor), factor + " 倍外壳没有绕目标中心放大");
        check(sameScale(body, innerOnce, innerScaled, factor), factor + " 倍内盒没有按同一倍率冻结");
        double baseGap = innerOnce.minX - outerOnce.minX;
        double scaledGap = innerScaled.minX - outerScaled.minX;
        check(Math.abs(scaledGap - baseGap * factor) < 1.0E-6D, factor + " 倍壳厚没有一起变");
        AABB reversed = outerScaled.inflate(-PrisonShell.PADDING);
        check(Math.abs(innerScaled.minX - reversed.minX) > 1.0E-4D, factor + " 倍内盒被固定 0.20 反推");
    }

    private static void reservation() {
        ReleaseInput doubled = new ReleaseInput(2f);
        ReleaseInput quadrupled = new ReleaseInput(4f);
        PrisonReservation stored = new PrisonReservation(UUID.randomUUID(), null, null, true, "", doubled);
        ReleaseInput kept = PrisonRuntime.bindInput(stored, quadrupled);
        check(kept == doubled, "重试换成了后一次倍率");
        check(stored.releaseInput() == doubled, "预约里的首次输入被替换");
        check(PrisonRuntime.bindInput(null, quadrupled) == quadrupled, "首次输入没有采用这次提供的倍率");
        check(PrisonRuntime.bindInput(null, null) == ReleaseInput.BASE, "空输入没有回到基准");
        check(stored.releaseInput().sizeMultiplier() == 2f, "保存的倍率不是 2");
    }

    private static void expiryAddition() {
        check(ReleaseInput.expiryGameTime(100L, 20L) == 120L, "有限寿命的到期点不是相加");
        check(ReleaseInput.expiryGameTime(Long.MAX_VALUE - 3L, 10L) == Long.MAX_VALUE, "长寿命相加没有停在 long 上界");
    }

    private static boolean sameScale(AABB body, AABB unscaled, AABB scaled, double factor) {
        double centerX = (body.minX + body.maxX) * 0.5D;
        double centerY = (body.minY + body.maxY) * 0.5D;
        double centerZ = (body.minZ + body.maxZ) * 0.5D;
        return near(scaled.minX, centerX + (unscaled.minX - centerX) * factor)
                && near(scaled.minY, centerY + (unscaled.minY - centerY) * factor)
                && near(scaled.minZ, centerZ + (unscaled.minZ - centerZ) * factor)
                && near(scaled.maxX, centerX + (unscaled.maxX - centerX) * factor)
                && near(scaled.maxY, centerY + (unscaled.maxY - centerY) * factor)
                && near(scaled.maxZ, centerZ + (unscaled.maxZ - centerZ) * factor);
    }

    private static boolean sameBox(AABB left, AABB right) {
        return left != null && right != null
                && near(left.minX, right.minX) && near(left.minY, right.minY) && near(left.minZ, right.minZ)
                && near(left.maxX, right.maxX) && near(left.maxY, right.maxY) && near(left.maxZ, right.maxZ);
    }

    private static boolean near(double actual, double expected) {
        return Math.abs(actual - expected) < 1.0E-6D;
    }

    private static void check(boolean passed, String message) {
        assertionCount++;
        if (!passed) {
            failures++;
            System.out.println("失败 " + message);
        }
    }
}
