你是主代理调度的DeepSeek专项只读读取代理。项目D:/alianximod；主代理已咨询唯一GPT-6-astra脑子，本轮只读事实，不写文件、不编译、不另派代理。
目标：水环已核准美术接入时确认应保留的现有存活、同步、姿态与偏转机制。
只读范围：
src/main/java/com/bslx/XueXiMod/shu/WaterTechniques.java 水环注册部分；
src/main/java/com/bslx/XueXiMod/shu/technique/release/guard/GuardStates.java；
src/main/java/com/bslx/XueXiMod/network/GuardSyncPacket.java；
src/main/java/com/bslx/XueXiMod/shu/technique/release/deflection/hosts/RingDeflectionHost.java、RingFollow.java、RingOrbitRoute.java相关几何/姿态；
docs/水环重设设计与实现指导-2026-10-22.md。
需要精确理解water_ring如何在客户端可见、基于什么owner坐标/半径/normal、何时消失，以及美术替换不能改哪些行为。
额外核对GPT初读发现的问题：服务端animationStart来自server.getTickCount，客户端level.getGameTime可能不同时基；若只改展示时钟怎样同步年龄。以及syncToTracking/remove/clear的实际调用点，允许rg这些符号定位相关注册类几行。
输出中文结构化事实清单和路径行号；不改代码，不猜未读行为。主代理会等唯一脑子裁决后让Cursor修改渲染。

当前是离线只读分析，以下已提供全部必要源文件。禁止调用任何工具，无须再查文件。若给定材料不含某个调用点，明确指出待查，不要因未提供的事实臆测。不要写文件或启动任何进程。

<source_file path="src/main/java/com/bslx/XueXiMod/shu/WaterTechniques.java">
1: package com.bslx.XueXiMod.shu;
2: 
3: import com.bslx.XueXiMod.shu.technique.TechniqueEffectRegistry;
4: import com.bslx.XueXiMod.shu.technique.effects.WaterFormEffect;
5: import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
6: import com.bslx.XueXiMod.shu.technique.release.Trajectory;
7: import com.bslx.XueXiMod.shu.technique.release.WaterReleaseDefinition;
8: import com.bslx.XueXiMod.shu.technique.release.conditional.WaterPrisonEffect;
9: import com.bslx.XueXiMod.shu.technique.release.water.WaterBeatBridge;
10: import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierDefinition;
11: import com.bslx.XueXiMod.shu.technique.release.domain.DomainDefinition;
12: import com.bslx.XueXiMod.shu.technique.release.guard.GuardDefinition;
13: 
14: /** 留下的水系成品：缺月、水幕、水雾、水环、深海重压、水牢。 */
15: public final class WaterTechniques {
16:     private static final WaterFormEffect FORM = new WaterFormEffect();
17: 
18:     private WaterTechniques() {
19:     }
20: 
21:     public static void register() {
22:         WaterBeatBridge.install();
23:         WaterReleaseDefinition blade = WaterReleaseDefinition.shot("water_blade", Trajectory.STRAIGHT, 10, 1.3f, 0.35f)
24:                 .along(3.2f).span(0.7f).energy(1.35f).wetness(6)
25:                 .swordForm();
26:         Technique bladeTechnique = new Technique("water_blade", "缺月", TechniqueType.SWORD, 7f, 14, 20,
27:                 "随挥剑扫出月牙形水流弧斩，可连续衔接", 0, ShuAttribute.WATER, blade.speed());
28:         TechniqueManager.register(bladeTechnique);
29:         ReleaseRegistry.register(blade);
30:         TechniqueEffectRegistry.register("water_blade", FORM);
31:         barrier("water_curtain", "水幕", 40, 0, "身前立起一道固定的薄水墙，穿过它的箭伤害减半",
32:                 BarrierDefinition.curtain());
33:         domain("water_mist", "雾雨", 80, 10, "固定的雨雾领域。完整进入的敌人出不去，并会变潮湿",
34:                 DomainDefinition.mistRain());
35:         guard("water_ring", "水环", 40, 0, "环绕自身的水环，受到的伤害削去两成",
36:                 GuardDefinition.personalRing());
37:         domain("water_deep_pressure", "深海重压", 80, 10, "低矮的重压领域。里面的人变慢、变潮湿，氧气消耗加快",
38:                 DomainDefinition.deepPressure());
39:         prison();
40:     }
41: 
42:     /**
43:      * 不登记水系命中效果。那条生成会打出命中体，水牢的伤害必须保持为 0。
44:      */
45:     private static void prison() {
46:         WaterReleaseDefinition definition = WaterReleaseDefinition.conditional("water_prison", 40, WaterPrisonEffect.INSTANCE);
47:         Technique technique = new Technique("water_prison", "水牢", TechniqueType.MAGIC, 0f, 400, 20,
48:                 "对准二十格内一个已经潮湿的生物，把它关进水牢。关不进去就不耗魔力。",
49:                 0, ShuAttribute.WATER, 0f);
50:         TechniqueManager.register(technique);
51:         ReleaseRegistry.register(definition);
52:     }
53: 
54:     private static void shot(String releaseId, String name, float damage, int cooldown, int complexity,
55:                              String description, WaterReleaseDefinition definition) {
56:         Technique technique = new Technique(releaseId, name, TechniqueType.MAGIC, damage, cooldown, 20,
57:                 description, complexity, ShuAttribute.WATER, definition.speed());
58:         TechniqueManager.register(technique);
59:         ReleaseRegistry.register(definition);
60:         TechniqueEffectRegistry.register(releaseId, FORM);
61:     }
62: 
63:     private static void zone(String releaseId, String name, int complexity, String description,
64:                              WaterReleaseDefinition definition) {
65:         zone(releaseId, name, 40, complexity, description, definition);
66:     }
67: 
68:     private static void zone(String releaseId, String name, int cooldown, int complexity, String description,
69:                              WaterReleaseDefinition definition) {
70:         Technique technique = new Technique(releaseId, name, TechniqueType.MAGIC, 0f, cooldown, 20,
71:                 description, complexity, ShuAttribute.WATER, 0f);
72:         TechniqueManager.register(technique);
73:         ReleaseRegistry.register(definition);
74:         TechniqueEffectRegistry.register(releaseId, FORM);
75:     }
76: 
77:     private static void domain(String releaseId, String name, int cooldown, int complexity, String description,
78:                                DomainDefinition domain) {
79:         Technique technique = new Technique(releaseId, name, TechniqueType.MAGIC, 0f, cooldown, 20,
80:                 description, complexity, ShuAttribute.WATER, 0f);
81:         TechniqueManager.register(technique);
82:         ReleaseRegistry.register(WaterReleaseDefinition.domain(releaseId, domain));
83:         TechniqueEffectRegistry.register(releaseId, FORM);
84:     }
85: 
86:     private static void barrier(String releaseId, String name, int cooldown, int complexity, String description,
87:                                 BarrierDefinition barrier) {
88:         Technique technique = new Technique(releaseId, name, TechniqueType.MAGIC, 0f, cooldown, 20,
89:                 description, complexity, ShuAttribute.WATER, 0f);
90:         TechniqueManager.register(technique);
91:         ReleaseRegistry.register(WaterReleaseDefinition.barrier(releaseId, barrier));
92:         TechniqueEffectRegistry.register(releaseId, FORM);
93:     }
94: 
95:     private static void guard(String releaseId, String name, int cooldown, int complexity, String description,
96:                               GuardDefinition guard) {
97:         Technique technique = new Technique(releaseId, name, TechniqueType.MAGIC, 0f, cooldown, 20,
98:                 description, complexity, ShuAttribute.WATER, 0f);
99:         TechniqueManager.register(technique);
100:         ReleaseRegistry.register(WaterReleaseDefinition.bodyGuard(releaseId, guard));
101:         TechniqueEffectRegistry.register(releaseId, FORM);
102:     }
103: }
104: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/guard/GuardStates.java">
1: package com.bslx.XueXiMod.shu.technique.release.guard;
2: 
3: import com.bslx.XueXiMod.network.GuardSyncPacket;
4: import com.bslx.XueXiMod.player.MagicPool;
5: import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHosts;
6: import com.bslx.XueXiMod.shu.technique.release.deflection.hosts.RingBandGeometry;
7: import com.bslx.XueXiMod.shu.technique.release.deflection.hosts.RingDeflectionHost;
8: import com.bslx.XueXiMod.shu.technique.release.deflection.hosts.RingFollow;
9: import com.bslx.XueXiMod.shu.technique.release.momentum.ChargeGate;
10: import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
11: import com.bslx.XueXiMod.shu.technique.release.contact.ContactCarrier;
12: import com.bslx.XueXiMod.shu.technique.release.contact.ContactEnchantState;
13: import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
14: import net.minecraft.server.MinecraftServer;
15: import net.minecraft.server.level.ServerLevel;
16: import net.minecraft.server.level.ServerPlayer;
17: import net.minecraft.world.entity.LivingEntity;
18: import net.minecraft.world.phys.AABB;
19: import net.minecraft.world.phys.Vec3;
20: import net.minecraft.resources.ResourceKey;
21: import net.minecraft.world.level.Level;
22: 
23: import java.util.ArrayList;
24: import java.util.Iterator;
25: import java.util.UUID;
26: import java.util.concurrent.ConcurrentHashMap;
27: 
28: /** 每人每类护体最多一个。不进场地表，也不增加场地层数。 */
29: public final class GuardStates {
30:     private static final ArrayList<BodyGuard> ACTIVE = new ArrayList<>();
31:     private static final ConcurrentHashMap<UUID, ConcurrentHashMap<Long, Integer>> WAVES = new ConcurrentHashMap<>();
32: 
33:     private GuardStates() {
34:     }
35: 
36:     /** 当下就能生成时返回是否生成成功。排到以后的，只表示已经排上。 */
37:     public static boolean schedule(ServerPlayer player, GuardDefinition guard, int fireTick, long waveKey) {
38:         if (player == null || guard == null || player.getServer() == null) {
39:             return false;
40:         }
41:         UUID playerId = player.getUUID();
42:         int epoch = ChargeGate.epoch(playerId);
43:         if (fireTick <= player.getServer().getTickCount()) {
44:             if (!ChargeGate.same(playerId, epoch) || !player.isAlive()) {
45:                 return false;
46:             }
47:             return activate(player, guard, waveKey);
48:         }
49:         ChainScheduler.schedule(player.getServer(), fireTick, () -> {
50:             if (!ChargeGate.same(playerId, epoch) || !player.isAlive()) {
51:                 return;
52:             }
53:             activate(player, guard, waveKey);
54:         });
55:         return true;
56:     }
57: 
58:     public static boolean activate(ServerPlayer player, GuardDefinition guard, long waveKey) {
59:         return activate(player, guard, waveKey, false);
60:     }
61: 
62:     /** 生成费已由父会话预留。 */
63:     public static boolean activateReserved(ServerPlayer player, GuardDefinition guard, long waveKey) {
64:         return activate(player, guard, waveKey, true);
65:     }
66: 
67:     private static boolean activate(ServerPlayer player, GuardDefinition guard, long waveKey, boolean reserved) {
68:         if (player == null || guard == null || player.getServer() == null) {
69:             return false;
70:         }
71:         if (!claimWave(player.getUUID(), waveKey, player.getServer().getTickCount())) {
72:             return true;
73:         }
74:         if (!reserved && !MagicPool.trySpend(player, guard.spawnCost(), true)) {
75:             forgetWave(player.getUUID(), waveKey);
76:             return false;
77:         }
78:         if (!reserved && guard.spawnCost() > 0) {
79:             MagicPool.showBalance(player);
80:         }
81:         int now = player.getServer().getTickCount();
82:         BodyGuard existing = find(player.getUUID(), guard.typeKey());
83:         if (existing != null) {
84:             detachDeflection(existing);
85:             existing.expiresAt = now + guard.lifetimeTicks();
86:             existing.revision += 1;
87:             existing.dimension = player.level().dimension();
88:             existing.serverLevel = player.serverLevel();
89:             attachDeflection(existing);
90:             broadcastFull(player, existing);
91:             return true;
92:         }
93:         BodyGuard created = new BodyGuard();
94:         created.ownerId = player.getUUID();
95:         created.dimension = player.level().dimension();
96:         created.serverLevel = player.serverLevel();
97:         created.guard = guard;
98:         created.expiresAt = now + guard.lifetimeTicks();
99:         created.nextUpkeepAt = now + guard.upkeepPeriod();
100:         created.animationStart = now;
101:         created.revision = 1;
102:         created.fromNormal = GuardPose.baseNormal(guard.baseTilt());
103:         created.towardNormal = created.fromNormal;
104:         created.phaseStart = now;
105:         created.phaseEnd = now;
106:         ACTIVE.add(created);
107:         attachDeflection(created);
108:         broadcastFull(player, created);
109:         return true;
110:     }
111: 
112:     public static float unloadRatio(ServerPlayer player) {
113:         BodyGuard guard = living(player);
114:         if (guard == null) {
115:             return 0f;
116:         }
117:         float ratio = guard.guard.unloadRatio();
118:         if (!Float.isFinite(ratio) || ratio <= 0f) {
119:             return 0f;
120:         }
121:         return ratio > 0.6f ? 0.6f : ratio;
122:     }
123: 
124:     public static void strike(ServerPlayer player, int poseKind, Vec3 incoming) {
125:         BodyGuard guard = living(player);
126:         if (guard == null || poseKind == GuardPose.NONE) {
127:             return;
128:         }
129:         int now = player.getServer().getTickCount();
130:         Vec3 target = poseKind == GuardPose.MELEE ? GuardPose.worldLevel() : GuardPose.leanToward(incoming);
131:         beginMove(player, guard, target, GuardPose.CAUSE_HIT, GuardPose.ENTER_TICKS, now);
132:         guard.holdUntil = now + GuardPose.ENTER_TICKS + GuardPose.HOLD_TICKS;
133:         guard.returning = false;
134:     }
135: 
136:     public static void tick(MinecraftServer server) {
137:         int now = server.getTickCount();
138:         Iterator<BodyGuard> iterator = ACTIVE.iterator();
139:         while (iterator.hasNext()) {
140:             BodyGuard guard = iterator.next();
141:             ServerPlayer owner = server.getPlayerList().getPlayer(guard.ownerId);
142:             if (owner == null || !owner.isAlive() || owner.level().dimension() != guard.dimension) {
143:                 drop(guard);
144:                 iterator.remove();
145:                 if (owner != null) {
146:                     broadcastRemove(owner, guard);
147:                 }
148:                 continue;
149:             }
150:             if (now >= guard.expiresAt) {
151:                 drop(guard);
152:                 iterator.remove();
153:                 broadcastRemove(owner, guard);
154:                 continue;
155:             }
156:             advancePose(owner, guard, now);
157:             if (now >= guard.nextUpkeepAt) {
158:                 if (!MagicPool.trySpend(owner, guard.guard.upkeepCost(), false)) {
159:                     drop(guard);
160:                     iterator.remove();
161:                     broadcastRemove(owner, guard);
162:                     continue;
163:                 }
164:                 guard.nextUpkeepAt += guard.guard.upkeepPeriod();
165:             }
166:             if ((now & 1) == 0) {
167:                 GuardPreview.look(owner, guard, now);
168:             }
169:         }
170:         forgetOldWaves(now);
171:     }
172: 
173:     public static void closeOwned(ServerPlayer player) {
174:         if (player == null) {
175:             return;
176:         }
177:         UUID playerId = player.getUUID();
178:         Iterator<BodyGuard> iterator = ACTIVE.iterator();
179:         while (iterator.hasNext()) {
180:             BodyGuard guard = iterator.next();
181:             if (!guard.ownerId.equals(playerId)) {
182:                 continue;
183:             }
184:             iterator.remove();
185:             drop(guard);
186:             broadcastRemove(player, guard);
187:         }
188:         WAVES.remove(playerId);
189:     }
190: 
191:     public static GuardSyncPacket snapshot(UUID ownerId) {
192:         for (BodyGuard guard : ACTIVE) {
193:             if (guard.ownerId.equals(ownerId)) {
194:                 return fullPacket(guard);
195:             }
196:         }
197:         return null;
198:     }
199: 
200:     static void preview(ServerPlayer owner, BodyGuard guard, Vec3 incoming, int entityId, int now) {
201:         if (guard.poseCause == GuardPose.CAUSE_HIT && now < guard.holdUntil) {
202:             return;
203:         }
204:         if (guard.previewEntityId == entityId && guard.previewDirection != null
205:                 && guard.previewDirection.distanceToSqr(incoming) < 0.04) {
206:             return;
207:         }
208:         guard.previewEntityId = entityId;
209:         guard.previewDirection = incoming;
210:         beginMove(owner, guard, GuardPose.leanToward(incoming), GuardPose.CAUSE_PREVIEW, GuardPose.ENTER_TICKS, now);
211:         guard.holdUntil = now + GuardPose.ENTER_TICKS + 4;
212:         guard.returning = false;
213:     }
214: 
215:     private static void advancePose(ServerPlayer owner, BodyGuard guard, int now) {
216:         if (guard.poseCause == GuardPose.CAUSE_NONE || now < guard.phaseEnd || now < guard.holdUntil || guard.returning) {
217:             if (guard.returning && now >= guard.phaseEnd) {
218:                 guard.poseCause = GuardPose.CAUSE_NONE;
219:                 guard.returning = false;
220:                 guard.previewEntityId = Integer.MIN_VALUE;
221:             }
222:             return;
223:         }
224:         Vec3 base = GuardPose.baseNormal(guard.guard.baseTilt());
225:         beginMove(owner, guard, base, guard.poseCause, GuardPose.RETURN_TICKS, now);
226:         guard.returning = true;
227:         guard.holdUntil = now;
228:     }
229: 
230:     private static void beginMove(ServerPlayer owner, BodyGuard guard, Vec3 target, int cause, int ticks, int now) {
231:         guard.fromNormal = guard.towardNormal == null ? GuardPose.baseNormal(guard.guard.baseTilt()) : guard.towardNormal;
232:         guard.towardNormal = target;
233:         guard.poseCause = cause;
234:         guard.phaseStart = now;
235:         guard.phaseEnd = now + Math.max(1, ticks);
236:         guard.sequence += 1;
237:         GuardSyncPacket.sendAround(owner, posePacket(guard, ticks));
238:     }
239: 
240:     private static BodyGuard living(ServerPlayer player) {
241:         if (player == null || player.getServer() == null) {
242:             return null;
243:         }
244:         BodyGuard guard = find(player.getUUID(), null);
245:         if (guard == null || player.getServer().getTickCount() >= guard.expiresAt) {
246:             return null;
247:         }
248:         if (player.level().dimension() != guard.dimension) {
249:             return null;
250:         }
251:         return guard;
252:     }
253: 
254:     private static BodyGuard find(UUID ownerId, String typeKey) {
255:         for (BodyGuard guard : ACTIVE) {
256:             if (!guard.ownerId.equals(ownerId)) {
257:                 continue;
258:             }
259:             if (typeKey == null || guard.guard.typeKey().equals(typeKey)) {
260:                 return guard;
261:             }
262:         }
263:         return null;
264:     }
265: 
266:     private static boolean claimWave(UUID playerId, long waveKey, int now) {
267:         ConcurrentHashMap<Long, Integer> waves = WAVES.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>());
268:         Integer previous = waves.putIfAbsent(waveKey, now);
269:         return previous == null;
270:     }
271: 
272:     private static void forgetWave(UUID playerId, long waveKey) {
273:         ConcurrentHashMap<Long, Integer> waves = WAVES.get(playerId);
274:         if (waves != null) {
275:             waves.remove(waveKey);
276:         }
277:     }
278: 
279:     private static void forgetOldWaves(int now) {
280:         for (ConcurrentHashMap<Long, Integer> waves : WAVES.values()) {
281:             waves.entrySet().removeIf(entry -> now - entry.getValue() > 400);
282:         }
283:     }
284: 
285:     private static void broadcastFull(ServerPlayer owner, BodyGuard guard) {
286:         GuardSyncPacket.sendAround(owner, fullPacket(guard));
287:     }
288: 
289:     public static void bindContact(ServerPlayer player, EnchantmentSnapshot snapshot) {
290:         BodyGuard guard = living(player);
291:         if (guard == null || snapshot == null) {
292:             return;
293:         }
294:         if (guard.contact != null) {
295:             guard.contact.close();
296:             guard.contact = null;
297:         }
298:         guard.contactRevision += 1;
299:         guard.contact = ContactEnchantState.open(new GuardTouch(guard), snapshot);
300:     }
301: 
302:     private static void drop(BodyGuard guard) {
303:         guard.retired = true;
304:         detachDeflection(guard);
305:         if (guard.contact != null) {
306:             guard.contact.close();
307:             guard.contact = null;
308:         }
309:     }
310: 
311:     private static void attachDeflection(BodyGuard guard) {
312:         detachDeflection(guard);
313:         if (guard.retired || guard.guard == null || !guard.guard.deflectsProjectiles() || guard.serverLevel == null) {
314:             return;
315:         }
316:         if (guard.ownerId == null || guard.hostId == null) {
317:             return;
318:         }
319:         guard.deflectionHost = new RingDeflectionHost(
320:                 new LiveRing(guard, guard.revision),
321:                 guard.ownerId,
322:                 guard.hostId,
323:                 guard.revision,
324:                 guard.serverLevel,
325:                 guard.guard.radius(),
326:                 guard.guard.band() * 0.5);
327:         DeflectionHosts.register(guard.deflectionHost);
328:     }
329: 
330:     private static void detachDeflection(BodyGuard guard) {
331:         if (guard.deflectionHost == null) {
332:             return;
333:         }
334:         DeflectionHosts.unregister(guard.deflectionHost.key());
335:         guard.deflectionHost = null;
336:     }
337: 
338:     /**
339:      * 偏转环面跟当前主人，法线用姿态插值。插值公式在 {@link RingBandGeometry#blendNormal}。
340:      * 主人不在本维度或这一代 revision 已经换掉时不再跟踪。
341:      */
342:     private static final class LiveRing implements RingFollow {
343:         private final BodyGuard guard;
344:         private final int boundRevision;
345: 
346:         private LiveRing(BodyGuard guard, int boundRevision) {
347:             this.guard = guard;
348:             this.boundRevision = boundRevision;
349:         }
350: 
351:         @Override
352:         public boolean live() {
353:             if (guard.retired || guard.revision != boundRevision || guard.serverLevel == null || guard.guard == null) {
354:                 return false;
355:             }
356:             if (guard.serverLevel.getServer() == null) {
357:                 return false;
358:             }
359:             int now = guard.serverLevel.getServer().getTickCount();
360:             if (now >= guard.expiresAt) {
361:                 return false;
362:             }
363:             ServerPlayer owner = guard.serverLevel.getServer().getPlayerList().getPlayer(guard.ownerId);
364:             return owner != null && owner.isAlive() && owner.level() == guard.serverLevel;
365:         }
366: 
367:         @Override
368:         public Vec3 center() {
369:             ServerPlayer owner = owner();
370:             if (owner == null || guard.guard == null) {
371:                 return Vec3.ZERO;
372:             }
373:             return owner.position().add(0.0, owner.getBbHeight() * guard.guard.bodyHeight(), 0.0);
374:         }
375: 
376:         @Override
377:         public Vec3 planeNormal() {
378:             int now = 0;
379:             if (guard.serverLevel != null && guard.serverLevel.getServer() != null) {
380:                 now = guard.serverLevel.getServer().getTickCount();
381:             }
382:             return RingBandGeometry.blendNormal(guard.fromNormal, guard.towardNormal, guard.phaseStart, guard.phaseEnd, now);
383:         }
384: 
385:         @Override
386:         public Vec3 ownerAnchor() {
387:             ServerPlayer owner = owner();
388:             if (owner == null) {
389:                 return center();
390:             }
391:             return owner.position();
392:         }
393: 
394:         private ServerPlayer owner() {
395:             if (guard.serverLevel == null || guard.serverLevel.getServer() == null || guard.ownerId == null) {
396:                 return null;
397:             }
398:             return guard.serverLevel.getServer().getPlayerList().getPlayer(guard.ownerId);
399:         }
400:     }
401: 
402:     private static void broadcastRemove(ServerPlayer owner, BodyGuard guard) {
403:         GuardSyncPacket.sendAround(owner, new GuardSyncPacket(GuardSyncPacket.REMOVE, guard.ownerId, guard.revision,
404:                 guard.sequence, 0, 0, 0, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0));
405:     }
406: 
407:     private static GuardSyncPacket fullPacket(BodyGuard guard) {
408:         Vec3 from = guard.fromNormal == null ? GuardPose.worldLevel() : guard.fromNormal;
409:         Vec3 toward = guard.towardNormal == null ? from : guard.towardNormal;
410:         return new GuardSyncPacket(GuardSyncPacket.FULL, guard.ownerId, guard.revision, guard.sequence,
411:                 guard.expiresAt, guard.nextUpkeepAt, guard.animationStart, guard.guard.baseTilt(),
412:                 (float) guard.guard.radius(), (float) guard.guard.band(), (float) guard.guard.bodyHeight(),
413:                 (float) from.x, (float) from.y, (float) from.z,
414:                 (float) toward.x, (float) toward.y, (float) toward.z,
415:                 Math.max(1, guard.phaseEnd - guard.phaseStart));
416:     }
417: 
418:     private static GuardSyncPacket posePacket(BodyGuard guard, int ticks) {
419:         Vec3 from = guard.fromNormal;
420:         Vec3 toward = guard.towardNormal;
421:         return new GuardSyncPacket(GuardSyncPacket.POSE, guard.ownerId, guard.revision, guard.sequence,
422:                 guard.expiresAt, guard.nextUpkeepAt, guard.animationStart, guard.guard.baseTilt(),
423:                 (float) guard.guard.radius(), (float) guard.guard.band(), (float) guard.guard.bodyHeight(),
424:                 (float) from.x, (float) from.y, (float) from.z,
425:                 (float) toward.x, (float) toward.y, (float) toward.z, ticks);
426:     }
427: 
428:     static final class BodyGuard {
429:         private UUID ownerId;
430:         private ResourceKey<Level> dimension;
431:         private GuardDefinition guard;
432:         private int expiresAt;
433:         private int nextUpkeepAt;
434:         private int animationStart;
435:         private int revision;
436:         private int sequence;
437:         private int poseCause;
438:         private int phaseStart;
439:         private int phaseEnd;
440:         private int holdUntil;
441:         private boolean returning;
442:         private Vec3 fromNormal = GuardPose.worldLevel();
443:         private Vec3 towardNormal = GuardPose.worldLevel();
444:         private Vec3 previewDirection;
445:         private int previewEntityId = Integer.MIN_VALUE;
446:         private UUID hostId = UUID.randomUUID();
447:         private ContactEnchantState contact;
448:         private int contactRevision = 1;
449:         private boolean retired;
450:         private ServerLevel serverLevel;
451:         private RingDeflectionHost deflectionHost;
452:     }
453: 
454:     private static final class GuardTouch implements ContactCarrier {
455:         private final BodyGuard guard;
456: 
457:         private GuardTouch(BodyGuard guard) {
458:             this.guard = guard;
459:         }
460: 
461:         @Override
462:         public UUID hostId() {
463:             return guard.hostId;
464:         }
465: 
466:         @Override
467:         public int revision() {
468:             return guard.contactRevision;
469:         }
470: 
471:         @Override
472:         public UUID ownerId() {
473:             return guard.ownerId;
474:         }
475: 
476:         @Override
477:         public boolean active() {
478:             return !guard.retired;
479:         }
480: 
481:         @Override
482:         public boolean touches(LivingEntity target) {
483:             if (target == null || guard.guard == null) {
484:                 return false;
485:             }
486:             ServerLevel level = hostLevel();
487:             if (level == null) {
488:                 return false;
489:             }
490:             ServerPlayer owner = level.getServer().getPlayerList().getPlayer(guard.ownerId);
491:             if (owner == null) {
492:                 return false;
493:             }
494:             Vec3 normal = guard.towardNormal == null ? GuardPose.worldLevel() : guard.towardNormal;
495:             if (normal.lengthSqr() < 1.0E-6) {
496:                 normal = GuardPose.worldLevel();
497:             }
498:             normal = normal.normalize();
499:             Vec3 center = owner.position().add(0.0, owner.getBbHeight() * guard.guard.bodyHeight(), 0.0);
500:             double radius = guard.guard.radius();
501:             double reach = guard.guard.band() * 0.5 + ContactEnchantState.SURFACE_SLOP;
502:             AABB body = target.getBoundingBox();
503:             Vec3 nearest = nearest(body, center);
504:             Vec3 relative = nearest.subtract(center);
505:             double along = relative.dot(normal);
506:             Vec3 planar = relative.subtract(normal.scale(along));
507:             double radial = planar.length();
508:             Vec3 ringPoint = radial < 1.0E-4
509:                     ? center
510:                     : center.add(planar.scale(radius / radial));
511:             Vec3 closest = nearest(body, ringPoint);
512:             return closest.distanceToSqr(ringPoint) <= reach * reach;
513:         }
514: 
515:         @Override
516:         public ServerLevel hostLevel() {
517:             return guard.serverLevel;
518:         }
519: 
520:         @Override
521:         public AABB queryBox() {
522:             ServerLevel level = hostLevel();
523:             if (level == null || guard.guard == null) {
524:                 return new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
525:             }
526:             ServerPlayer owner = level.getServer().getPlayerList().getPlayer(guard.ownerId);
527:             if (owner == null) {
528:                 return new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
529:             }
530:             double reach = guard.guard.radius() + guard.guard.band() + 2.0;
531:             return owner.getBoundingBox().inflate(reach);
532:         }
533: 
534:         private static Vec3 nearest(AABB body, Vec3 point) {
535:             double clampedX = point.x < body.minX ? body.minX : (point.x > body.maxX ? body.maxX : point.x);
536:             double clampedY = point.y < body.minY ? body.minY : (point.y > body.maxY ? body.maxY : point.y);
537:             double clampedZ = point.z < body.minZ ? body.minZ : (point.z > body.maxZ ? body.maxZ : point.z);
538:             return new Vec3(clampedX, clampedY, clampedZ);
539:         }
540:     }
541: }
542: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/network/GuardSyncPacket.java">
1: package com.bslx.XueXiMod.network;
2: 
3: import com.bslx.XueXiMod.shu.technique.release.guard.GuardStates;
4: import net.minecraft.network.FriendlyByteBuf;
5: import net.minecraft.server.level.ServerPlayer;
6: import net.minecraftforge.api.distmarker.Dist;
7: import net.minecraftforge.fml.DistExecutor;
8: import net.minecraftforge.network.NetworkEvent;
9: import net.minecraftforge.network.simple.SimpleChannel;
10: 
11: import java.util.ArrayList;
12: import java.util.List;
13: import java.util.UUID;
14: import java.util.function.Supplier;
15: 
16: /** 服务端把护体快照和姿态发给能看见这位玩家的人。坐标不逐拍发送。 */
17: public class GuardSyncPacket {
18:     public static final int FULL = 0;
19:     public static final int POSE = 1;
20:     public static final int REMOVE = 2;
21: 
22:     private final int kind;
23:     private final UUID ownerId;
24:     private final int revision;
25:     private final int sequence;
26:     private final int expiresAt;
27:     private final int upkeepAt;
28:     private final int animationStart;
29:     private final float baseTilt;
30:     private final float radius;
31:     private final float band;
32:     private final float bodyHeight;
33:     private final float fromX;
34:     private final float fromY;
35:     private final float fromZ;
36:     private final float towardX;
37:     private final float towardY;
38:     private final float towardZ;
39:     private final int poseTicks;
40: 
41:     public GuardSyncPacket(int kind, UUID ownerId, int revision, int sequence,
42:                            int expiresAt, int upkeepAt, int animationStart,
43:                            float baseTilt, float radius, float band, float bodyHeight,
44:                            float fromX, float fromY, float fromZ,
45:                            float towardX, float towardY, float towardZ, int poseTicks) {
46:         this.kind = kind;
47:         this.ownerId = ownerId;
48:         this.revision = revision;
49:         this.sequence = sequence;
50:         this.expiresAt = expiresAt;
51:         this.upkeepAt = upkeepAt;
52:         this.animationStart = animationStart;
53:         this.baseTilt = baseTilt;
54:         this.radius = radius;
55:         this.band = band;
56:         this.bodyHeight = bodyHeight;
57:         this.fromX = fromX;
58:         this.fromY = fromY;
59:         this.fromZ = fromZ;
60:         this.towardX = towardX;
61:         this.towardY = towardY;
62:         this.towardZ = towardZ;
63:         this.poseTicks = poseTicks;
64:     }
65: 
66:     public void encode(FriendlyByteBuf buffer) {
67:         buffer.writeByte(kind);
68:         buffer.writeUUID(ownerId);
69:         buffer.writeVarInt(revision);
70:         buffer.writeVarInt(sequence);
71:         buffer.writeVarInt(expiresAt);
72:         buffer.writeVarInt(upkeepAt);
73:         buffer.writeVarInt(animationStart);
74:         buffer.writeFloat(baseTilt);
75:         buffer.writeFloat(radius);
76:         buffer.writeFloat(band);
77:         buffer.writeFloat(bodyHeight);
78:         buffer.writeFloat(fromX);
79:         buffer.writeFloat(fromY);
80:         buffer.writeFloat(fromZ);
81:         buffer.writeFloat(towardX);
82:         buffer.writeFloat(towardY);
83:         buffer.writeFloat(towardZ);
84:         buffer.writeVarInt(poseTicks);
85:     }
86: 
87:     public static GuardSyncPacket decode(FriendlyByteBuf buffer) {
88:         return new GuardSyncPacket(buffer.readByte(), buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt(),
89:                 buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
90:                 buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
91:                 buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
92:                 buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
93:                 buffer.readVarInt());
94:     }
95: 
96:     public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
97:         GuardSyncPacket packet = this;
98:         contextSupplier.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientRings.accept(packet)));
99:         contextSupplier.get().setPacketHandled(true);
100:     }
101: 
102:     public static void register(SimpleChannel channel, int messageId) {
103:         channel.registerMessage(messageId, GuardSyncPacket.class, GuardSyncPacket::encode, GuardSyncPacket::decode, GuardSyncPacket::handle);
104:     }
105: 
106:     public static void sendAround(ServerPlayer owner, GuardSyncPacket packet) {
107:         if (owner == null || owner.getServer() == null) {
108:             return;
109:         }
110:         for (ServerPlayer watcher : owner.serverLevel().players()) {
111:             if (watcher == owner || watcher.distanceTo(owner) <= 128f) {
112:                 ModNetwork.sendToPlayer(watcher, packet);
113:             }
114:         }
115:     }
116: 
117:     public static void sendSnapshot(ServerPlayer watcher, ServerPlayer owner) {
118:         GuardSyncPacket packet = GuardStates.snapshot(owner.getUUID());
119:         if (packet != null && watcher != null) {
120:             ModNetwork.sendToPlayer(watcher, packet);
121:         }
122:     }
123: 
124:     public static final class ClientRings {
125:         private static final ArrayList<View> VIEWS = new ArrayList<>();
126: 
127:         private ClientRings() {
128:         }
129: 
130:         public static void accept(GuardSyncPacket packet) {
131:             if (packet.kind == REMOVE) {
132:                 VIEWS.removeIf(view -> view.ownerId.equals(packet.ownerId));
133:                 return;
134:             }
135:             View view = find(packet.ownerId);
136:             if (packet.kind == FULL) {
137:                 if (view != null && packet.revision < view.revision) {
138:                     return;
139:                 }
140:                 if (view == null) {
141:                     view = new View();
142:                     view.ownerId = packet.ownerId;
143:                     VIEWS.add(view);
144:                 }
145:                 view.revision = packet.revision;
146:                 view.sequence = packet.sequence;
147:                 view.baseTilt = packet.baseTilt;
148:                 view.radius = packet.radius;
149:                 view.band = packet.band;
150:                 view.bodyHeight = packet.bodyHeight;
151:                 view.animationStart = packet.animationStart;
152:                 applyPose(view, packet);
153:                 return;
154:             }
155:             if (view == null || packet.revision != view.revision || packet.sequence < view.sequence) {
156:                 return;
157:             }
158:             view.sequence = packet.sequence;
159:             applyPose(view, packet);
160:         }
161: 
162:         public static void clear() {
163:             VIEWS.clear();
164:         }
165: 
166:         public static List<View> all() {
167:             return VIEWS;
168:         }
169: 
170:         private static View find(UUID ownerId) {
171:             for (View view : VIEWS) {
172:                 if (view.ownerId.equals(ownerId)) {
173:                     return view;
174:                 }
175:             }
176:             return null;
177:         }
178: 
179:         private static void applyPose(View view, GuardSyncPacket packet) {
180:             view.fromX = packet.fromX;
181:             view.fromY = packet.fromY;
182:             view.fromZ = packet.fromZ;
183:             view.towardX = packet.towardX;
184:             view.towardY = packet.towardY;
185:             view.towardZ = packet.towardZ;
186:             view.poseStartMillis = System.currentTimeMillis();
187:             view.poseTicks = Math.max(1, packet.poseTicks);
188:         }
189: 
190:         public static final class View {
191:             public UUID ownerId;
192:             public int revision;
193:             public int sequence;
194:             public float baseTilt;
195:             public float radius = 0.9f;
196:             public float band = 0.12f;
197:             public float bodyHeight = 0.55f;
198:             public int animationStart;
199:             public float fromX;
200:             public float fromY = 1f;
201:             public float fromZ;
202:             public float towardX;
203:             public float towardY = 1f;
204:             public float towardZ;
205:             public long poseStartMillis;
206:             public int poseTicks = 1;
207:         }
208:     }
209: }
210: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/deflection/hosts/RingDeflectionHost.java">
1: package com.bslx.XueXiMod.shu.technique.release.deflection.hosts;
2: 
3: import com.bslx.XueXiMod.shu.technique.release.deflection.CaptureRoute;
4: import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionAttack;
5: import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHost;
6: import com.bslx.XueXiMod.shu.technique.release.deflection.HostContact;
7: import com.bslx.XueXiMod.shu.technique.release.deflection.HostKey;
8: import net.minecraft.server.level.ServerLevel;
9: import net.minecraft.world.phys.AABB;
10: import net.minecraft.world.phys.Vec3;
11: 
12: import java.util.Optional;
13: import java.util.UUID;
14: 
15: /**
16:  * 水环宿主。环心是当前主人身体高度上的点，法线是护体 from→toward 的确定插值，
17:  * 和 {@link com.bslx.XueXiMod.shu.technique.release.guard.GuardStates} 登记时用的是同一公式。
18:  * 附魔接触仍读目标法线，这里不改那条旧卸力。
19:  * 管半径是带宽的一半，不加附魔容差。圆盘内部不算盾，箭仍走这条环带。
20:  * 近战另用主人包围盒的水平外扩包络，只护当前这一代的主人，不把环心圆盘当成无敌。
21:  * 换代时用新的 generation 重新登记，旧路线看到 live 为假就退出。
22:  * 本宿主不扣水势。出口随机只在每条路线构造时向服务端要一次。
23:  */
24: public final class RingDeflectionHost implements DeflectionHost {
25:     private final RingFollow follow;
26:     private final UUID owner;
27:     private final HostKey key;
28:     private final ServerLevel serverLevel;
29:     private final double majorRadius;
30:     private final double tubeRadius;
31: 
32:     public RingDeflectionHost(RingFollow follow, UUID owner, UUID hostId, long generation, ServerLevel serverLevel,
33:                               double majorRadius, double tubeRadius) {
34:         this.follow = follow;
35:         this.owner = owner;
36:         this.key = new HostKey(hostId, generation);
37:         this.serverLevel = serverLevel;
38:         this.majorRadius = majorRadius;
39:         this.tubeRadius = tubeRadius;
40:     }
41: 
42:     @Override
43:     public UUID ownerId() {
44:         return owner;
45:     }
46: 
47:     @Override
48:     public HostKey key() {
49:         return key;
50:     }
51: 
52:     @Override
53:     public ServerLevel level() {
54:         return serverLevel;
55:     }
56: 
57:     @Override
58:     public boolean active() {
59:         return serverLevel != null && follow != null && follow.live();
60:     }
61: 
62:     @Override
63:     public Optional<HostContact> intersect(Vec3 from, Vec3 toward, double projectileRadius, long gameTick) {
64:         if (!active() || !finite(follow.center()) || !finite(follow.planeNormal())) {
65:             return Optional.empty();
66:         }
67:         Optional<RingBandGeometry.RingHit> hit = RingBandGeometry.earliest(
68:                 follow.center(), follow.planeNormal(), majorRadius, tubeRadius, from, toward, projectileRadius);
69:         if (hit.isEmpty()) {
70:             return Optional.empty();
71:         }
72:         RingBandGeometry.RingHit found = hit.get();
73:         return Optional.of(new HostContact(this, found.segmentT(), found.point(), found.unitNormal()));
74:     }
75: 
76:     @Override
77:     public Optional<HostContact> intersectMelee(DeflectionAttack attack, Vec3 from, Vec3 toward, long gameTick) {
78:         if (attack == null || attack.kind() != DeflectionAttack.Kind.MELEE || attack.victim() == null) {
79:             return Optional.empty();
80:         }
81:         return openMelee(active(), attack.victim().getUUID(), attack.victim().getBoundingBox(), from, toward);
82:     }
83: 
84:     /**
85:      * 活动代和主人身份由调用方传入的代标志、受击者决定。几何只认包络，不读环带。
86:      */
87:     Optional<HostContact> openMelee(boolean generationLive, UUID victimId, AABB ownerBody, Vec3 from, Vec3 toward) {
88:         Optional<RingMeleeGeometry.MeleeHit> hit = RingMeleeGeometry.protect(
89:                 generationLive, owner, victimId, readCenter(), majorRadius, tubeRadius, ownerBody, from, toward);
90:         if (hit.isEmpty()) {
91:             return Optional.empty();
92:         }
93:         RingMeleeGeometry.MeleeHit found = hit.get();
94:         return Optional.of(new HostContact(this, found.segmentT(), found.point(), found.unitNormal()));
95:     }
96: 
97:     @Override
98:     public boolean covers(Vec3 point, double projectileRadius, long gameTick) {
99:         if (!active() || !finite(follow.center()) || !finite(follow.planeNormal())) {
100:             return false;
101:         }
102:         return RingBandGeometry.covers(follow.center(), follow.planeNormal(), majorRadius, tubeRadius,
103:                 point, projectileRadius);
104:     }
105: 
106:     @Override
107:     public CaptureRoute route(HostContact contact, Vec3 incomingVelocity, long gameTick) {
108:         double randomUnit = 0.0;
109:         if (serverLevel != null && serverLevel.getRandom() != null) {
110:             randomUnit = serverLevel.getRandom().nextDouble();
111:         }
112:         return new RingOrbitRoute(follow, contact.point(), incomingVelocity, gameTick, randomUnit, majorRadius);
113:     }
114: 
115:     private Vec3 readCenter() {
116:         if (follow == null) {
117:             return null;
118:         }
119:         try {
120:             return follow.center();
121:         } catch (RuntimeException failure) {
122:             return null;
123:         }
124:     }
125: 
126:     private static boolean finite(Vec3 point) {
127:         return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
128:     }
129: }
130: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/deflection/hosts/RingFollow.java">
1: package com.bslx.XueXiMod.shu.technique.release.deflection.hosts;
2: 
3: import net.minecraft.world.phys.Vec3;
4: 
5: /**
6:  * 水环当前这一拍的真实姿态。环心跟主人，法线跟护体姿态。
7:  * 主人离场、死亡、换维或这一代护体失效时 {@link #live()} 为假，路线不再追。
8:  */
9: public interface RingFollow {
10:     boolean live();
11: 
12:     Vec3 center();
13: 
14:     Vec3 planeNormal();
15: 
16:     Vec3 ownerAnchor();
17: }
18: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/deflection/hosts/RingOrbitRoute.java">
1: package com.bslx.XueXiMod.shu.technique.release.deflection.hosts;
2: 
3: import com.bslx.XueXiMod.shu.technique.release.deflection.CaptureRoute;
4: import com.bslx.XueXiMod.shu.technique.release.deflection.ReleaseSample;
5: import com.bslx.XueXiMod.shu.technique.release.deflection.RouteSample;
6: import net.minecraft.world.phys.Vec3;
7: 
8: /**
9:  * 水环绕行六拍。出口角和绕行方向在构造时定死，两条路线不共享这段进度。
10:  * 环心和法线每次取样读当前主人与当前姿态；法线变化时基底只做最小旋转，角度不另抽。
11:  * {@link #emergencyRelease(long)} 在正常走完时给出径向朝外的单位方向；主人失效时给出当时位置和方向并封存。
12:  * 同一 tick 再问不会重抽随机，也不会把时钟再推一拍。速度幅值由弹体批保存。
13:  */
14: public final class RingOrbitRoute implements CaptureRoute {
15:     private final RingFollow follow;
16:     private final Vec3 contact;
17:     private final long captureTick;
18:     private final double radius;
19:     private final double entryAngle;
20:     private final double exitAngle;
21:     private final int windingSign;
22:     private final double signedTravel;
23:     private final RingBandGeometry.PlaneBasis captureBasis;
24:     private final Vec3 captureCenter;
25:     private boolean sealed;
26:     private boolean lost;
27:     private Vec3 sealedPosition;
28:     private Vec3 sealedDirection;
29: 
30:     public RingOrbitRoute(RingFollow follow, Vec3 contact, Vec3 incomingVelocity, long captureTick,
31:                           double randomUnit, double radius) {
32:         this.follow = follow;
33:         this.contact = contact == null ? Vec3.ZERO : contact;
34:         this.captureTick = captureTick;
35:         this.radius = radius > 1.0E-6 ? radius : 0.9;
36:         Vec3 center = this.contact;
37:         Vec3 normal = new Vec3(0.0, 1.0, 0.0);
38:         if (follow != null && follow.live() && finite(follow.center()) && finite(follow.planeNormal())) {
39:             center = follow.center();
40:             normal = follow.planeNormal();
41:         }
42:         this.captureCenter = center;
43:         this.captureBasis = RingBandGeometry.PlaneBasis.of(normal);
44:         this.entryAngle = RingBandGeometry.entryAngle(this.contact, center, captureBasis);
45:         Vec3 anchor = follow == null || follow.ownerAnchor() == null ? center : follow.ownerAnchor();
46:         this.exitAngle = RingBandGeometry.chooseExitAngle(entryAngle, randomUnit, center, captureBasis, this.radius, anchor);
47:         this.windingSign = RingBandGeometry.winding(entryAngle, incomingVelocity, captureBasis);
48:         this.signedTravel = RingBandGeometry.signedTravel(entryAngle, exitAngle, windingSign);
49:         if (follow == null || !follow.live()) {
50:             seal(this.contact, outward(captureBasis, exitAngle), true);
51:         }
52:     }
53: 
54:     @Override
55:     public RouteSample sample(long gameTick) {
56:         Pose pose = pose(gameTick);
57:         return new RouteSample(pose.position, pose.finished && !pose.lost);
58:     }
59: 
60:     @Override
61:     public ReleaseSample emergencyRelease(long gameTick) {
62:         Pose pose = pose(gameTick);
63:         if (pose.finished || pose.lost) {
64:             seal(pose.position, pose.unitDirection, pose.lost && !pose.finished);
65:         }
66:         Pose stable = pose(gameTick);
67:         return new ReleaseSample(stable.position, stable.unitDirection);
68:     }
69: 
70:     @Override
71:     public boolean active() {
72:         return !lost;
73:     }
74: 
75:     private Pose pose(long gameTick) {
76:         if (sealed) {
77:             return new Pose(sealedPosition, sealedDirection, !lost, lost);
78:         }
79:         long elapsed = gameTick < captureTick ? 0L : gameTick - captureTick;
80:         boolean timeDone = elapsed >= RingBandGeometry.ORBIT_TICKS;
81:         boolean living = follow != null && follow.live();
82:         if (!living) {
83:             Pose lostPose = placed(elapsed, false);
84:             seal(lostPose.position, lostPose.unitDirection, true);
85:             return new Pose(sealedPosition, sealedDirection, false, true);
86:         }
87:         if (timeDone) {
88:             Pose done = placed(RingBandGeometry.ORBIT_TICKS, true);
89:             seal(done.position, done.unitDirection, false);
90:             return new Pose(sealedPosition, sealedDirection, true, false);
91:         }
92:         return placed(elapsed, false);
93:     }
94: 
95:     private Pose placed(long elapsed, boolean finished) {
96:         double alpha = RingBandGeometry.ORBIT_TICKS <= 0
97:                 ? 1.0
98:                 : Math.min(1.0, elapsed / (double) RingBandGeometry.ORBIT_TICKS);
99:         Vec3 center = captureCenter;
100:         RingBandGeometry.PlaneBasis basis = captureBasis;
101:         Vec3 anchor = captureCenter;
102:         if (follow != null && follow.live() && finite(follow.center()) && finite(follow.planeNormal())) {
103:             center = follow.center();
104:             basis = captureBasis.rotatedTo(follow.planeNormal());
105:             if (finite(follow.ownerAnchor())) {
106:                 anchor = follow.ownerAnchor();
107:             }
108:         }
109:         double angle = entryAngle + signedTravel * alpha;
110:         Vec3 circle = center.add(basis.radial(angle, radius));
111:         Vec3 entryCircle = captureCenter.add(captureBasis.radial(entryAngle, radius));
112:         Vec3 position = circle.add(contact.subtract(entryCircle).scale(1.0 - alpha));
113:         Vec3 direction = finished ? releaseDirection(basis, anchor, position) : travelDirection(alpha, center, basis);
114:         return new Pose(position, direction, finished, false);
115:     }
116: 
117:     private Vec3 releaseDirection(RingBandGeometry.PlaneBasis basis, Vec3 anchor, Vec3 exitPoint) {
118:         Vec3 outward = outward(basis, exitAngle);
119:         if (finite(anchor) && outward.dot(anchor.subtract(exitPoint)) > 0.0) {
120:             outward = outward.scale(-1.0);
121:         }
122:         return outward;
123:     }
124: 
125:     private Vec3 travelDirection(double alpha, Vec3 center, RingBandGeometry.PlaneBasis basis) {
126:         double ahead = Math.min(1.0, alpha + 1.0 / RingBandGeometry.ORBIT_TICKS);
127:         double angle = entryAngle + signedTravel * ahead;
128:         Vec3 next = center.add(basis.radial(angle, radius));
129:         double currentAngle = entryAngle + signedTravel * alpha;
130:         Vec3 current = center.add(basis.radial(currentAngle, radius));
131:         Vec3 step = next.subtract(current);
132:         if (step.lengthSqr() < 1.0E-12) {
133:             return outward(basis, exitAngle);
134:         }
135:         return step.normalize();
136:     }
137: 
138:     private static Vec3 outward(RingBandGeometry.PlaneBasis basis, double angle) {
139:         Vec3 radial = basis.radial(angle, 1.0);
140:         if (radial.lengthSqr() < 1.0E-16) {
141:             return new Vec3(0.0, 1.0, 0.0);
142:         }
143:         return radial.normalize();
144:     }
145: 
146:     private void seal(Vec3 position, Vec3 direction, boolean markLost) {
147:         if (sealed) {
148:             return;
149:         }
150:         sealed = true;
151:         lost = markLost;
152:         sealedPosition = position == null ? contact : position;
153:         sealedDirection = direction == null || direction.lengthSqr() < 1.0E-16
154:                 ? new Vec3(0.0, 1.0, 0.0)
155:                 : direction.normalize();
156:     }
157: 
158:     private static boolean finite(Vec3 point) {
159:         return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
160:     }
161: 
162:     private record Pose(Vec3 position, Vec3 unitDirection, boolean finished, boolean lost) {
163:     }
164: }
165: 
</source_file>
