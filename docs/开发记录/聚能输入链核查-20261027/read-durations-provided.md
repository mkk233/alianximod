你是deepseek-flash专项只读补查，唯一GPT-6-astra要求以下事实。不调用工具、不改代码、不编译、不派代理；只按给定完整带行号源码核实，1000-1800字中文路径行号报告。核心聚能已确认只进入实体CastFrame；公共输入快照后续由brain裁决，此轮不要自行给架构修法。用户MAGIC/NONE资格边界正在确认，不自行假定。
补查持续实体：TechniqueHitSpec复制/配置到Entity寿命与活动窗口，WeepingCurtain/热光束/TestSustained的专用时钟是否绕过spec.lifetime；列全部要同源扩展的时间字段及无倍率基线。场地实际已注册使用者和屏障尺寸/寿命生成消费入口，必须分开本体范围与子效果半径，费用/冷却/伤害不在此轮随能量乘倍。

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitSpec.java">
1: package com.bslx.XueXiMod.shu.technique.release.entity;
2: 
3: import com.bslx.XueXiMod.command.SkillEntityCount;
4: import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
5: import com.bslx.XueXiMod.shu.technique.release.CastFrame;
6: import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
7: import com.bslx.XueXiMod.shu.technique.release.Trajectory;
8: import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
9: import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
10: import com.bslx.XueXiMod.shu.technique.release.momentum.HeavyWater;
11: import com.bslx.XueXiMod.shu.technique.release.payload.HitPayloads;
12: import com.bslx.XueXiMod.shu.technique.release.payload.WetPressure;
13: import com.bslx.XueXiMod.shu.technique.release.water.ArcBandDraft;
14: import com.bslx.XueXiMod.shu.technique.release.water.ArcBandSnapshot;
15: import com.bslx.XueXiMod.shu.technique.release.water.ArcVec;
16: import com.bslx.XueXiMod.shu.technique.release.water.RadialCopyPlane;
17: import net.minecraft.server.level.ServerLevel;
18: import net.minecraft.server.level.ServerPlayer;
19: import net.minecraft.world.entity.LivingEntity;
20: import net.minecraft.world.phys.Vec3;
21: 
22: import java.util.function.Consumer;
23: 
24: public final class TechniqueHitSpec {
25: 
26:     public enum Motion {
27:         SWING,
28:         PROJECTILE,
29:         DROP,
30:         FOLLOW
31:     }
32: 
33:     public enum Hitbox {
34:         ARC,
35:         OBB,
36:         BOX,
37:         SPHERE
38:     }
39: 
40:     public enum Path {
41:         STILL_AIM,
42:         STRAIGHT,
43:         DROP,
44:         FOLLOW,
45:         VORTEX,
46:         FUNCTION
47:     }
48: 
49:     public interface HitListener {
50:         void onHit(LivingEntity target);
51: 
52:         void onExpire(Vec3 position);
53:     }
54: 
55:     public interface HitListenerFactory {
56:         HitListener create();
57:     }
58: 
59:     public String techniqueId = "";
60:     public Motion motion = Motion.PROJECTILE;
61:     public Hitbox hitbox = Hitbox.BOX;
62:     public int lifetime = 20;
63:     public double speed = 1.0;
64:     public boolean lockThisCast;
65:     public boolean dropThisCast;
66:     public boolean tracking;
67:     public double arcRadius = 2.0;
68:     public double arcDegrees = 120.0;
69:     public double arcHeight = 2.0;
70:     public Vec3 boxSize = new Vec3(1.0, 1.0, 1.0);
71:     public int hitInterval;
72:     /** 服务器年龄达到这个值才开始命中。0 表示生成后就能打到。 */
73:     public int hitReadyAge;
74:     public int visual;
75:     public float modelScale = 1.0F;
76:     public Vec3 start;
77:     public Vec3 anchor;
78:     public Vec3 aim;
79:     public Path path;
80:     public String functionId = "";
81:     public double length = 1.0;
82:     public double width = 1.0;
83:     public double height = 1.0;
84:     public String texture;
85:     public boolean centered;
86:     public boolean pose3d;
87:     public float roll;
88:     public HitListener listener;
89:     public HitListenerFactory listenerFactory;
90:     public float settleDamage = Float.NaN;
91:     public EnchantmentResolution settleEnchantments;
92:     public ReleaseBatch.Tally settleTally;
93:     public int settleCopyIndex;
94:     public int settleBurnSeconds;
95:     public int settleControlTicks;
96:     public boolean settleExtinguish;
97:     public float settleKnockback;
98:     public com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess settleTide =
99:             com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess.NONE;
100:     public Vec3 localOffset;
101:     public boolean trap;
102:     public boolean burstOnImpact;
103:     public boolean blocksMovement;
104:     /** 空表示仍走旧的盒子或弹道。只有声明了弧带的释放会填。 */
105:     public ArcBandDraft arcDraft;
106:     /** 这一份实体冻结后的弧。生成循环里逐份写入，不在份与份之间共用可变对象。 */
107:     public ArcBandSnapshot frozenArc;
108:     /** 这一次生成要抄到实体上的显示序号。循环内逐份换成新记录，结束后回到单份。 */
109:     public CohortVisual cohortVisual = CohortVisual.SINGLE;
110:     /** 静止冻结斩面的单次贴图。默认关闭，调用方显式打开，不在这里识别术式名字。 */
111:     public StillSheetVisual stillSheet = StillSheetVisual.NONE;
112:     /** 实体真正进入世界后通知调用方。不在这里识别术式名字。 */
113:     public Consumer<TechniqueHitEntity> publishedHook;
114: 
115:     public static final class SpawnReport {
116:         private final int requested;
117:         private final int accepted;
118: 
119:         private SpawnReport(int requested, int accepted) {
120:             this.requested = requested;
121:             this.accepted = accepted;
122:         }
123: 
124:         public int requested() {
125:             return requested;
126:         }
127: 
128:         public int accepted() {
129:             return accepted;
130:         }
131: 
132:         public boolean complete() {
133:             return requested > 0 && accepted == requested;
134:         }
135:     }
136: 
137:     public static void spawn(ServerLevel level, ServerPlayer caster, TechniqueHitSpec spec) {
138:         spawnReported(level, caster, spec);
139:     }
140: 
141:     /** 返回这一次调用里实际要求生成和真正进入世界的数量。 */
142:     public static SpawnReport spawnReported(ServerLevel level, ServerPlayer caster, TechniqueHitSpec spec) {
143:         CastFrame frame = CastFrame.current();
144:         boolean arcBand = spec.arcDraft != null;
145:         if (frame != null) {
146:             spec.tracking = frame.tracking && frame.trajectoryMutable;
147:             spec.settleDamage = frame.damage;
148:             spec.settleEnchantments = frame.enchantments;
149:             spec.settleTally = frame.tally;
150:             spec.settleCopyIndex = frame.copyIndex;
151:             float volumeScale = frame.volumeScale();
152:             ReleaseDefinition definition = ReleaseRegistry.get(spec.techniqueId);
153:             boolean scaleLength = definition == null || definition.energyScalesLength();
154:             boolean scaleWidth = definition == null || definition.energyScalesWidth();
155:             boolean scaleHeight = definition == null || definition.energyScalesHeight();
156:             boolean scaleModel = definition == null || definition.energyScalesModel();
157:             if (!arcBand && Math.abs(volumeScale - 1f) > 0.001F) {
158:                 if (scaleLength) {
159:                     spec.length *= volumeScale;
160:                 }
161:                 if (scaleWidth) {
162:                     spec.width *= volumeScale;
163:                 }
164:                 if (scaleHeight) {
165:                     spec.height *= volumeScale;
166:                 }
167:                 if (scaleModel) {
168:                     spec.modelScale *= volumeScale;
169:                 }
170:             }
171:             spec.settleBurnSeconds = frame.burnSeconds;
172:             spec.settleControlTicks = frame.controlTicks;
173:             spec.settleExtinguish = frame.extinguish;
174:             spec.settleKnockback = frame.knockback;
175:             spec.settleTide = frame.tideSuccess == null
176:                     ? com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess.NONE
177:                     : frame.tideSuccess;
178:             if (frame.blocksMovement) {
179:                 spec.blocksMovement = true;
180:             }
181:             if (frame.speedMultiplier > 1.001F) {
182:                 spec.speed *= frame.speedMultiplier;
183:             }
184:             if (!arcBand && frame.overrideGeometry) {
185:                 Path path = spec.path;
186:                 boolean still = path == Path.STILL_AIM || spec.centered || !frame.trajectoryMutable;
187:                 if (frame.trajectoryMutable && frame.pathOverride != null && frame.pathOverride != Trajectory.STRAIGHT) {
188:                     spec.dropThisCast = frame.pathOverride == Trajectory.DROP;
189:                 }
190:                 if (frame.trajectoryMutable && frame.pathOverride == Trajectory.VORTEX) {
191:                     spec.path = Path.VORTEX;
192:                     spec.start = frame.origin != null ? frame.origin : caster.getEyePosition();
193:                     spec.aim = frame.look;
194:                 } else if (frame.drop && frame.trajectoryMutable) {
195:                     spec.path = Path.DROP;
196:                     spec.start = frame.origin != null ? frame.origin : caster.getEyePosition();
197:                     spec.aim = frame.look;
198:                 } else if (path == Path.FUNCTION) {
199:                     // 不可改弹道的水帘也要吃落点和交错，只搬生成位置，不改成斩击锚点。
200:                     Vec3 locked = frame.origin != null ? frame.origin : frame.anchor;
201:                     spec.start = locked != null ? locked : caster.getEyePosition();
202:                     if (frame.look != null) {
203:                         spec.aim = frame.look;
204:                     }
205:                 } else if (still) {
206:                     spec.centered = true;
207:                     spec.anchor = frame.origin != null ? frame.origin : frame.anchor;
208:                     spec.aim = frame.planeNormal != null ? frame.planeNormal : frame.look;
209:                     spec.roll = frame.roll;
210:                     Vec3 normal = frame.planeNormal != null ? frame.planeNormal : frame.look;
211:                     spec.pose3d = frame.pose3d
212:                             || (normal != null && Math.abs(normal.y) > 0.2)
213:                             || Math.abs(frame.roll) > 1.0;
214:                 } else if (path == Path.STRAIGHT || path == Path.DROP || path == Path.FOLLOW) {
215:                     spec.start = frame.origin != null ? frame.origin : caster.getEyePosition();
216:                     if (frame.look != null) {
217:                         spec.aim = frame.look;
218:                     }
219:                 }
220:             }
221:         }
222:         if (spec.localOffset != null) {
223:             if (spec.start != null) {
224:                 spec.start = spec.start.add(spec.localOffset);
225:             } else if (spec.anchor != null) {
226:                 spec.anchor = spec.anchor.add(spec.localOffset);
227:             } else {
228:                 spec.start = caster.getEyePosition().add(spec.localOffset);
229:             }
230:         }
231:         int baseCopy = spec.settleCopyIndex;
232:         HostGroupContext hostGroup = HostGroupContext.current();
233:         int entityCount = hostGroup == null ? SkillEntityCount.entityCount() : hostGroup.spawnCount();
234:         ReleaseDefinition cohortDefinition = ReleaseRegistry.get(spec.techniqueId);
235:         boolean swordVisual = cohortDefinition != null && cohortDefinition.separatesCohortVisuals();
236:         Vec3 bakedStart = spec.start;
237:         Vec3 bakedAim = spec.aim;
238:         CastFrame placedFrame = frame;
239:         Vec3 sampledEye = arcBand ? caster.getEyePosition() : null;
240:         Vec3 sampledAim = arcBand ? caster.getLookAngle() : null;
241:         boolean planeSpread = radialPlaneSpread(spec.arcDraft, placedFrame);
242:         int accepted = 0;
243:         for (int spawnIndex = 0; spawnIndex < entityCount; spawnIndex++) {
244:             if (spec.listenerFactory != null) {
245:                 spec.listener = spec.listenerFactory.create();
246:             }
247:             spec.settleCopyIndex = CohortVisual.settleEncoding(baseCopy, spawnIndex);
248:             assignCohort(spec, placedFrame, spawnIndex, entityCount, swordVisual, planeSpread);
249:             if (placedFrame != null && placedFrame.pathOverride == Trajectory.VORTEX && placedFrame.trajectoryMutable) {
250:                 spec.path = Path.VORTEX;
251:                 spec.dropThisCast = false;
252:                 spec.start = placedFrame.anchor != null ? placedFrame.anchor : placedFrame.origin;
253:                 spec.aim = placedFrame.vortexPhase(spawnIndex);
254:             } else if (placedFrame != null && placedFrame.volumeHalf > 0.0 && placedFrame.trajectoryMutable && placedFrame.anchor != null) {
255:                 spec.start = placedFrame.volumePoint(spawnIndex);
256:                 if (spec.localOffset != null) {
257:                     spec.start = spec.start.add(spec.localOffset);
258:                 }
259:                 spec.aim = placedFrame.aimFor(spawnIndex);
260:                 if (placedFrame.drop) {
261:                     spec.path = Path.DROP;
262:                     spec.dropThisCast = true;
263:                 }
264:             }
265:             if (arcBand) {
266:                 spec.frozenArc = bakeArc(spec, placedFrame, sampledEye, sampledAim);
267:                 if (spec.frozenArc == null) {
268:                     if (hostGroup != null) {
269:                         hostGroup.fail();
270:                     }
271:                     continue;
272:                 }
273:             }
274:             TechniqueHitEntity entity = new TechniqueHitEntity(ReleaseEntities.TECHNIQUE_HIT.get(), level);
275:             if (hostGroup != null) {
276:                 entity.holdActivation();
277:             }
278:             try {
279:                 entity.applySpec(caster, spec);
280:             } catch (RuntimeException failure) {
281:                 if (hostGroup != null) {
282:                     hostGroup.fail();
283:                 }
284:                 HitPayloads.clear(entity);
285:                 WetPressure.clear(entity);
286:                 continue;
287:             }
288:             HitPayloads.attach(entity, placedFrame);
289:             ReleaseDefinition spawnedDefinition = ReleaseRegistry.get(spec.techniqueId);
290:             if (spawnedDefinition != null && spawnedDefinition.pressureWetnessMinimum() > 0 && placedFrame != null) {
291:                 WetPressure.attach(entity, placedFrame.releaseGroup, placedFrame.heavyWater,
292:                         spawnedDefinition.pressureWetnessMinimum());
293:             }
294:             boolean published = level.addFreshEntity(entity);
295:             if (!published) {
296:                 HitPayloads.clear(entity);
297:                 WetPressure.clear(entity);
298:                 if (hostGroup != null) {
299:                     hostGroup.fail();
300:                 }
301:                 continue;
302:             }
303:             accepted++;
304:             if (spec.publishedHook != null) {
305:                 spec.publishedHook.accept(entity);
306:             }
307:             if (hostGroup != null) {
308:                 hostGroup.stagePublished(entity);
309:             } else {
310:                 entity.stampStillBirth();
311:                 HeavyWater.commitFirst(caster);
312:             }
313:         }
314:         spec.settleCopyIndex = baseCopy;
315:         spec.start = bakedStart;
316:         spec.aim = bakedAim;
317:         spec.frozenArc = null;
318:         releaseCohort(spec);
319:         return new SpawnReport(entityCount, accepted);
320:     }
321: 
322:     /** 按这一帧实际采用的几何，给当前生成号做一份不可变显示记录。 */
323:     public static CohortVisual cohortFor(CastFrame frame, int spawnIndex, int spawnCount, boolean swordVisual) {
324:         return cohortFor(frame, spawnIndex, spawnCount, swordVisual, false);
325:     }
326: 
327:     /**
328:      * {@code planeSpread} 只写进这一份的视觉记录，不改共享帧。
329:      * 复制轴散开后，同一复制号里的生成倍率仍按生成号分槽。
330:      */
331:     public static CohortVisual cohortFor(CastFrame frame, int spawnIndex, int spawnCount, boolean swordVisual,
332:                                          boolean planeSpread) {
333:         int copyCount = 1;
334:         int copyIndex = 0;
335:         if (frame != null) {
336:             copyCount = frame.copyCount < 1 ? 1 : frame.copyCount;
337:             copyIndex = frame.copyIndex;
338:         }
339:         return CohortVisual.capture(copyIndex, copyCount, spawnIndex, spawnCount, swordVisual,
340:                 copyDispersed(frame) || planeSpread, spawnDispersed(frame));
341:     }
342: 
343:     /** 这一份的生成点或刀面已经按复制号分开。接触点改回同一锚点后不再算。 */
344:     public static boolean copyDispersed(CastFrame frame) {
345:         if (frame == null) {
346:             return false;
347:         }
348:         if (spawnDispersed(frame)) {
349:             return true;
350:         }
351:         return frame.cohortDispersed;
352:     }
353: 
354:     /** 只看生成循环里真正按生成号改位置或方向的分支，不从复制散开标志推断。 */
355:     public static boolean spawnDispersed(CastFrame frame) {
356:         if (frame == null) {
357:             return false;
358:         }
359:         if (frame.pathOverride == Trajectory.VORTEX && frame.trajectoryMutable) {
360:             return true;
361:         }
362:         return frame.volumeHalf > 0.0 && frame.trajectoryMutable && frame.anchor != null;
363:     }
364: 
365:     public static void assignCohort(TechniqueHitSpec spec, CastFrame frame, int spawnIndex, int spawnCount, boolean swordVisual) {
366:         assignCohort(spec, frame, spawnIndex, spawnCount, swordVisual, false);
367:     }
368: 
369:     public static void assignCohort(TechniqueHitSpec spec, CastFrame frame, int spawnIndex, int spawnCount,
370:                                     boolean swordVisual, boolean planeSpread) {
371:         spec.cohortVisual = cohortFor(frame, spawnIndex, spawnCount, swordVisual, planeSpread);
372:     }
373: 
374:     /** 交错、落点和已经改过刀面的路径保持原轨迹。只搬锚点的覆写不算已经散开。 */
375:     static boolean radialCopiesAlreadySpread(CastFrame frame) {
376:         if (frame == null) {
377:             return false;
378:         }
379:         if (copyDispersed(frame)) {
380:             return true;
381:         }
382:         return frame.pose3d || Math.abs(frame.roll) > 1.0F;
383:     }
384: 
385:     static boolean radialPlaneSpread(ArcBandDraft draft, CastFrame frame) {
386:         if (draft == null || frame == null) {
387:             return false;
388:         }
389:         return RadialCopyPlane.spreads(draft.mode(), frame.copyCount, radialCopiesAlreadySpread(frame));
390:     }
391: 
392:     public static void releaseCohort(TechniqueHitSpec spec) {
393:         spec.cohortVisual = CohortVisual.SINGLE;
394:     }
395: 
396:     /** 覆写过的原点和瞄准原样进入快照。没有覆写时，眼位和朝向只用循环外的那一次采样。 */
397:     static ArcBandSnapshot bakeArc(TechniqueHitSpec spec, CastFrame frame, Vec3 sampledEye, Vec3 sampledAim) {
398:         boolean framedOrigin = frame != null && frame.overrideGeometry && frame.origin != null;
399:         Vec3 origin = framedOrigin ? frame.origin : (spec.start != null ? spec.start : sampledEye);
400:         if (origin == null) {
401:             return null;
402:         }
403:         if (framedOrigin && spec.localOffset != null) {
404:             origin = origin.add(spec.localOffset);
405:         }
406:         boolean scatterFacing = frame != null && (frame.pose3d || Math.abs(frame.roll) > 1.0F);
407:         Vec3 facing = scatterFacing && frame.planeNormal != null && frame.planeNormal.lengthSqr() > 1.0E-8
408:                 ? frame.planeNormal
409:                 : (frame != null && frame.look != null && frame.look.lengthSqr() > 1.0E-8 ? frame.look : spec.aim);
410:         if (facing == null || facing.lengthSqr() <= 1.0E-8) {
411:             facing = sampledAim;
412:         }
413:         if (facing == null) {
414:             return null;
415:         }
416:         double extraRoll = frame == null ? 0.0 : frame.roll;
417:         double scale = frame == null ? 1.0 : frame.volumeScale();
418:         int beat = spec.arcDraft.beat();
419:         ArcVec frozenOrigin = new ArcVec(origin.x, origin.y, origin.z);
420:         ArcVec frozenFacing = new ArcVec(facing.x, facing.y, facing.z);
421:         if (frame != null && frame.copyIndex > 0 && radialPlaneSpread(spec.arcDraft, frame)) {
422:             RadialCopyPlane.Placement placement = RadialCopyPlane.place(frame.copyIndex, frame.copyCount, beat, extraRoll);
423:             return ArcBandSnapshot.freezeLook(
424:                     frozenOrigin,
425:                     frozenFacing,
426:                     null,
427:                     false,
428:                     placement.extraRollDegrees(beat),
429:                     beat,
430:                     spec.arcDraft.baseRadius(),
431:                     spec.arcDraft.baseHalfWidth(),
432:                     scale,
433:                     spec.arcDraft.mode(),
434:                     placement.sectorStartDegrees(),
435:                     placement.sectorSweepDegrees());
436:         }
437:         return ArcBandSnapshot.freezeLook(
438:                 frozenOrigin,
439:                 frozenFacing,
440:                 null,
441:                 false,
442:                 extraRoll,
443:                 beat,
444:                 spec.arcDraft.baseRadius(),
445:                 spec.arcDraft.baseHalfWidth(),
446:                 scale,
447:                 spec.arcDraft.mode());
448:     }
449: }
450: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitEntity.java">
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
11: import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
12: import com.bslx.XueXiMod.shu.technique.release.WeepingCurtain;
13: import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHosts;
14: import com.bslx.XueXiMod.shu.technique.release.deflection.hosts.ArcDeflectionHost;
15: import com.bslx.XueXiMod.shu.technique.release.deflection.hosts.WeepingDeflectionBinding;
16: import com.bslx.XueXiMod.shu.technique.release.water.ArcBandMode;
17: import com.bslx.XueXiMod.shu.technique.release.water.ArcBandSnapshot;
18: import com.bslx.XueXiMod.shu.technique.release.water.ArcBounds;
19: import com.bslx.XueXiMod.shu.technique.release.water.ProgressArcBand;
20: import net.minecraft.core.particles.ParticleTypes;
21: import net.minecraft.nbt.CompoundTag;
22: import net.minecraft.network.syncher.EntityDataAccessor;
23: import net.minecraft.network.syncher.EntityDataSerializers;
24: import net.minecraft.network.syncher.SynchedEntityData;
25: import net.minecraft.server.MinecraftServer;
26: import net.minecraft.server.level.ServerLevel;
27: import net.minecraft.server.level.ServerPlayer;
28: import net.minecraft.world.entity.Entity;
29: import net.minecraft.world.entity.EntityType;
30: import net.minecraft.world.entity.LivingEntity;
31: import net.minecraft.world.level.ClipContext;
32: import net.minecraft.world.level.Level;
33: import net.minecraft.world.phys.AABB;
34: import net.minecraft.world.phys.BlockHitResult;
35: import net.minecraft.world.phys.HitResult;
36: import net.minecraft.world.phys.Vec3;
37: 
38: import javax.annotation.Nullable;
39: import java.util.HashMap;
40: import java.util.HashSet;
41: import java.util.List;
42: import java.util.Map;
43: import java.util.Set;
44: import java.util.UUID;
45: 
46: public class TechniqueHitEntity extends Entity {
47:     private static final EntityDataAccessor<String> DATA_TECHNIQUE_ID =
48:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.STRING);
49:     private static final EntityDataAccessor<Integer> DATA_VISUAL =
50:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
51:     private static final EntityDataAccessor<Float> DATA_MODEL_SCALE =
52:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);
53:     private static final EntityDataAccessor<String> DATA_TEXTURE =
54:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.STRING);
55:     private static final EntityDataAccessor<Float> DATA_LENGTH =
56:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);
57:     private static final EntityDataAccessor<Float> DATA_WIDTH =
58:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);
59:     private static final EntityDataAccessor<Float> DATA_HEIGHT =
60:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);
61:     private static final EntityDataAccessor<Float> DATA_ROLL =
62:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);
63:     private static final EntityDataAccessor<String> DATA_ARC_SNAPSHOT =
64:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.STRING);
65:     private static final EntityDataAccessor<Integer> DATA_DISPLAY_INDEX =
66:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
67:     private static final EntityDataAccessor<Integer> DATA_DISPLAY_COUNT =
68:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
69:     private static final EntityDataAccessor<Boolean> DATA_DISPLAY_LANE =
70:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.BOOLEAN);
71:     private static final EntityDataAccessor<Integer> DATA_LANE_INDEX =
72:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
73:     private static final EntityDataAccessor<Integer> DATA_LANE_COUNT =
74:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
75:     private static final EntityDataAccessor<Integer> DATA_STILL_FRAMES =
76:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
77:     private static final EntityDataAccessor<Integer> DATA_STILL_FRAME_TICKS =
78:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
79:     private static final EntityDataAccessor<Long> DATA_STILL_BIRTH =
80:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.LONG);
81:     private static final EntityDataAccessor<Boolean> DATA_STILL_CANCELLED =
82:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.BOOLEAN);
83: 
84:     @Nullable
85:     private UUID casterUuid;
86:     private boolean deflectsIncoming;
87:     /** 持续接触结算。只从释放定义抄来，客户端不登记瀑布宿主。 */
88:     boolean persistentFlow;
89:     private ArcDeflectionHost arcDeflection;
90:     private TechniqueMotion motion;
91:     private TechniqueHitbox hitbox;
92:     private int lifetime;
93:     private double speed;
94:     private boolean lockThisCast;
95:     private boolean dropThisCast;
96:     private boolean centered;
97:     private boolean pose3d;
98:     private boolean beam;
99:     private boolean pinned;
100:     private boolean trap;
101:     boolean burstOnImpact;
102:     private boolean planted;
103:     /** 盒子高度就是当前水体，不再保底成一格。 */
104:     private boolean exactVolume;
105:     /** 宿主组尚未整组成功时不结算伤害。旧路径保持关闭。 */
106:     private boolean pendingActivation;
107:     @Nullable
108:     private ArcBandSnapshot arcSnapshot;
109:     private double lastProcessedProgress;
110:     private boolean arcDamageClosed;
111:     boolean detonated;
112:     private final ClientTrack clientTrack = new ClientTrack();
113:     int settleControlTicks;
114:     boolean settleExtinguish;
115:     float settleKnockback;
116:     private double arcRadius;
117:     private double arcDegrees;
118:     private double arcHeight;
119:     private Vec3 boxSize = Vec3.ZERO;
120:     private double length = 1.0;
121:     private double width = 1.0;
122:     private double height = 1.0;
123:     int hitInterval;
124:     int hitReadyAge;
125:     @Nullable
126:     TechniqueHitSpec.HitListener listener;
127:     private int age;
128:     /** 就绪年龄只通知一次。不在这里识别术式名字。 */
129:     private boolean partingNotified;
130:     private Vec3 origin = Vec3.ZERO;
131:     Vec3 forward = new Vec3(0.0, 0.0, 1.0);
132:     private String functionId = "";
133:     private Vec3 previousPosition = Vec3.ZERO;
134:     @Nullable
135:     private Vec3 anchor;
136:     @Nullable
137:     private Vec3 aim;
138:     final Set<UUID> hitOnce = new HashSet<>();
139:     final Map<UUID, Long> lastHitTick = new HashMap<>();
140:     float settleDamage = Float.NaN;
141:     EnchantmentResolution settleEnchantments;
142:     ReleaseBatch.Tally settleTally;
143:     int settleCopyIndex;
144:     int settleBurnSeconds;
145:     com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess settleTide =
146:             com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess.NONE;
147: 
148:     public TechniqueHitEntity(EntityType<? extends TechniqueHitEntity> type, Level level) {
149:         super(type, level);
150:         this.noPhysics = true;
151:         this.setNoGravity(true);
152:     }
153: 
154:     @Override
155:     protected void defineSynchedData() {
156:         this.entityData.define(DATA_TECHNIQUE_ID, "");
157:         this.entityData.define(DATA_VISUAL, 0);
158:         this.entityData.define(DATA_MODEL_SCALE, 1.0F);
159:         this.entityData.define(DATA_TEXTURE, "");
160:         this.entityData.define(DATA_LENGTH, 1.0F);
161:         this.entityData.define(DATA_WIDTH, 1.0F);
162:         this.entityData.define(DATA_HEIGHT, 1.0F);
163:         this.entityData.define(DATA_ROLL, 0.0F);
164:         this.entityData.define(DATA_ARC_SNAPSHOT, "");
165:         this.entityData.define(DATA_DISPLAY_INDEX, 0);
166:         this.entityData.define(DATA_DISPLAY_COUNT, 1);
167:         this.entityData.define(DATA_DISPLAY_LANE, false);
168:         this.entityData.define(DATA_LANE_INDEX, 0);
169:         this.entityData.define(DATA_LANE_COUNT, 1);
170:         this.entityData.define(DATA_STILL_FRAMES, 0);
171:         this.entityData.define(DATA_STILL_FRAME_TICKS, 1);
172:         this.entityData.define(DATA_STILL_BIRTH, StillSheetVisual.UNBORN);
173:         this.entityData.define(DATA_STILL_CANCELLED, false);
174:     }
175: 
176:     @Override
177:     protected AABB makeBoundingBox() {
178:         AABB arcBox = this.arcWorldBox();
179:         return arcBox != null ? arcBox : super.makeBoundingBox();
180:     }
181: 
182:     @Override
183:     public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
184:         super.onSyncedDataUpdated(key);
185:         if (DATA_ARC_SNAPSHOT.equals(key)) {
186:             ArcBandSnapshot unpacked = ArcBandSnapshot.unpack(this.entityData.get(DATA_ARC_SNAPSHOT));
187:             if (unpacked != null) {
188:                 this.arcSnapshot = unpacked;
189:                 AABB arcBox = this.arcWorldBox();
190:                 if (arcBox != null) {
191:                     this.setBoundingBox(arcBox);
192:                 }
193:             }
194:         }
195:     }
196: 
197:     @Override
198:     protected void readAdditionalSaveData(CompoundTag tag) {
199:     }
200: 
201:     @Override
202:     protected void addAdditionalSaveData(CompoundTag tag) {
203:     }
204: 
205:     @Override
206:     public boolean isPickable() {
207:         return false;
208:     }
209: 
210:     @Override
211:     public boolean isPushable() {
212:         return false;
213:     }
214: 
215:     @Override
216:     public boolean canBeCollidedWith() {
217:         return ObstacleBodies.blocks(this);
218:     }
219: 
220:     @Override
221:     public boolean canCollideWith(Entity other) {
222:         return false;
223:     }
224: 
225:     @Override
226:     public boolean shouldBeSaved() {
227:         return false;
228:     }
229: 
230:     @Override
231:     public void lerpTo(double targetX, double targetY, double targetZ, float yaw, float pitch, int steps, boolean teleport) {
232:         if (this.level().isClientSide()) {
233:             this.clientTrack.onClientMove(this, targetX, targetY, targetZ, yaw, pitch);
234:             return;
235:         }
236:         super.lerpTo(targetX, targetY, targetZ, yaw, pitch, steps, teleport);
237:     }
238: 
239:     @Override
240:     public void moveTo(double targetX, double targetY, double targetZ, float yaw, float pitch) {
241:         if (this.level().isClientSide()) {
242:             this.clientTrack.onClientMove(this, targetX, targetY, targetZ, yaw, pitch);
243:             return;
244:         }
245:         super.moveTo(targetX, targetY, targetZ, yaw, pitch);
246:     }
247: 
248:     @Override
249:     public void tick() {
250:         super.tick();
251:         if (this.level().isClientSide()) {
252:             this.clientTrack.afterClientTick(this);
253:             if (this.hasArcBand()) {
254:                 return;
255:             }
256:             this.clientDecor();
257:             return;
258:         }
259:         if (this.pendingActivation) {
260:             return;
261:         }
262:         ServerLevel level = (ServerLevel) this.level();
263:         if (this.arcSnapshot != null) {
264:             this.tickArcBand(level);
265:             return;
266:         }
267:         this.previousPosition = this.position();
268:         this.age++;
269:         if (this.motion != null) {
270:             this.motion.tick(this, level);
271:         }
272:         this.refreshWeepingDeflection();
273:         if (!this.partingNotified && this.age >= this.hitReadyAge) {
274:             this.partingNotified = true;
275:             WaterPartingService.activated(this);
276:         }
277:         if (this.centered) {
278:             this.previousPosition = this.position();
279:         }
280:         if (this.trap) {
281:             if (this.planted && !this.detonated) {
282:                 AABB box = this.getBoundingBox().inflate(0.3);
283:                 List<LivingEntity> stepped = level.getEntitiesOfClass(LivingEntity.class, box,
284:                         entity -> TechniqueHitSettle.canHit(this, entity));
285:                 if (!stepped.isEmpty()) {
286:                     TechniqueHitSettle.detonate(this, level);
287:                 }
288:             }
289:         } else if (this.hitbox != null && this.age >= this.hitReadyAge) {
290:             AABB broad = this.hitbox.broadPhase(this);
291:             List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, broad,
292:                     entity -> TechniqueHitSettle.canHit(this, entity));
293:             for (LivingEntity target : targets) {
294:                 if (this.hitbox.contains(this, target)) {
295:                     TechniqueHitSettle.tryHit(this, level, target);
296:                 }
297:             }
298:         }
299:         if (this.burstOnImpact && !this.isRemoved() && this.previousPosition != null) {
300:             BlockHitResult blockHit = level.clip(new ClipContext(
301:                     this.previousPosition, this.position(),
302:                     ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
303:             if (blockHit.getType() == HitResult.Type.BLOCK) {
304:                 Vec3 hitPoint = blockHit.getLocation();
305:                 this.setPos(hitPoint.x, hitPoint.y, hitPoint.z);
306:                 TechniqueHitSettle.onExpire(this, level);
307:                 return;
308:             }
309:         }
310:         if (this.age >= this.lifetime) {
311:             TechniqueHitSettle.onExpire(this, level);
312:         }
313:     }
314: 
315:     /** 粒子只在客户端逻辑拍生成，避免渲染帧里按光束长度狂撒。 */
316:     private void clientDecor() {
317:         int visual = this.getVisual();
318:         if (visual == 4) {
319:             this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY() + 0.2, this.getZ(), 0.0, 0.01, 0.0);
320:         } else if (visual == 3 && (this.tickCount & 7) == 0) {
321:             this.level().addParticle(ParticleTypes.CRIMSON_SPORE, this.getX(), this.getY() + 0.2, this.getZ(), 0.0, 0.02, 0.0);
322:         }
323:     }
324: 
325:     public void holdActivation() {
326:         this.pendingActivation = true;
327:     }
328: 
329:     public void releaseActivation() {
330:         this.pendingActivation = false;
331:         if (this.arcSnapshot != null && this.arcSnapshot.birthTick() < 0L && !this.level().isClientSide()) {
332:             this.adoptArc(this.arcSnapshot.withBirth(this.level().getGameTime()));
333:             if (this.level() instanceof ServerLevel serverLevel) {
334:                 this.refreshArcDeflection(serverLevel, 0L);
335:             }
336:         }
337:         this.stampStillBirth();
338:     }
339: 
340:     public boolean hasArcBand() {
341:         if (this.arcSnapshot != null) {
342:             return true;
343:         }
344:         String packed = this.entityData.get(DATA_ARC_SNAPSHOT);
345:         return packed != null && !packed.isEmpty();
346:     }
347: 
348:     @Nullable
349:     public ArcBandSnapshot arcBand() {
350:         if (this.arcSnapshot == null) {
351:             this.arcSnapshot = ArcBandSnapshot.unpack(this.entityData.get(DATA_ARC_SNAPSHOT));
352:         }
353:         return this.arcSnapshot;
354:     }
355: 
356:     public void applySpec(ServerPlayer caster, TechniqueHitSpec spec) {
357:         CohortVisual shown = spec.cohortVisual == null ? CohortVisual.SINGLE : spec.cohortVisual.stored();
358:         this.entityData.set(DATA_DISPLAY_INDEX, shown.displayIndex());
359:         this.entityData.set(DATA_DISPLAY_COUNT, shown.displayCount());
360:         this.entityData.set(DATA_DISPLAY_LANE, shown.laneSplit());
361:         this.entityData.set(DATA_LANE_INDEX, shown.laneIndex());
362:         this.entityData.set(DATA_LANE_COUNT, shown.laneCount());
363:         this.casterUuid = caster.getUUID();
364:         ReleaseDefinition boundDefinition = ReleaseRegistry.get(spec.techniqueId);
365:         this.deflectsIncoming = boundDefinition != null && boundDefinition.deflectsProjectiles();
366:         this.persistentFlow = boundDefinition != null && boundDefinition.persistentFlowSettlement();
367:         this.lifetime = spec.lifetime;
368:         this.speed = spec.speed;
369:         this.lockThisCast = spec.lockThisCast;
370:         this.dropThisCast = spec.dropThisCast;
371:         this.centered = spec.centered;
372:         this.pose3d = spec.pose3d;
373:         this.arcRadius = spec.arcRadius;
374:         this.arcDegrees = spec.arcDegrees;
375:         this.arcHeight = spec.arcHeight;
376:         this.boxSize = spec.boxSize == null ? Vec3.ZERO : spec.boxSize;
377:         this.hitInterval = spec.hitInterval;
378:         this.hitReadyAge = Math.max(0, spec.hitReadyAge);
379:         this.listener = spec.listener;
380:         this.settleDamage = spec.settleDamage;
381:         this.settleEnchantments = spec.settleEnchantments;
382:         this.settleTally = spec.settleTally;
383:         this.settleCopyIndex = spec.settleCopyIndex;
384:         this.settleBurnSeconds = spec.settleBurnSeconds;
385:         this.settleControlTicks = spec.settleControlTicks;
386:         this.settleExtinguish = spec.settleExtinguish;
387:         this.settleKnockback = spec.settleKnockback;
388:         this.settleTide = spec.settleTide == null
389:                 ? com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess.NONE
390:                 : spec.settleTide;
391:         this.trap = spec.trap;
392:         this.burstOnImpact = spec.burstOnImpact;
393:         if (spec.blocksMovement) ObstacleBodies.track(this, spec.length, spec.width, spec.height);
394:         this.anchor = spec.anchor;
395:         this.aim = spec.aim;
396:         this.length = spec.length;
397:         this.width = spec.width;
398:         this.height = spec.height;
399:         this.functionId = spec.functionId == null ? "" : spec.functionId;
400:         if (spec.frozenArc != null) {
401:             this.installArc(spec.frozenArc);
402:             return;
403:         }
404:         this.origin = spec.start != null ? spec.start : caster.getEyePosition();
405:         this.forward = spec.aim != null && spec.aim.lengthSqr() > 1.0E-8
406:                 ? spec.aim.normalize()
407:                 : caster.getLookAngle();
408:         TechniqueHitSpec.Path path = spec.path;
409:         if (path == null) {
410:             path = switch (spec.motion) {
411:                 case DROP -> TechniqueHitSpec.Path.DROP;
412:                 case FOLLOW -> TechniqueHitSpec.Path.FOLLOW;
413:                 case SWING -> TechniqueHitSpec.Path.STILL_AIM;
414:                 case PROJECTILE -> TechniqueHitSpec.Path.STRAIGHT;
415:             };
416:         }
417:         if (spec.dropThisCast) {
418:             path = TechniqueHitSpec.Path.DROP;
419:         }
420:         this.beam = path == TechniqueHitSpec.Path.FOLLOW;
421:         this.pinned = this.beam && spec.start != null;
422:         this.entityData.set(DATA_TECHNIQUE_ID, spec.techniqueId == null ? "" : spec.techniqueId);
423:         this.entityData.set(DATA_VISUAL, spec.visual);
424:         this.entityData.set(DATA_MODEL_SCALE, spec.modelScale);
425:         this.entityData.set(DATA_TEXTURE, spec.texture == null ? "" : spec.texture);
426:         StillSheetVisual stillSheet = spec.stillSheet == null ? StillSheetVisual.NONE : spec.stillSheet;
427:         this.entityData.set(DATA_STILL_FRAMES, stillSheet.enabled() ? stillSheet.frameCount() : 0);
428:         this.entityData.set(DATA_STILL_FRAME_TICKS, stillSheet.enabled() ? stillSheet.ticksPerFrame() : 1);
429:         this.entityData.set(DATA_LENGTH, (float) spec.length);
430:         this.entityData.set(DATA_WIDTH, (float) spec.width);
431:         this.entityData.set(DATA_HEIGHT, (float) spec.height);
432:         this.entityData.set(DATA_ROLL, spec.roll);
433:         this.motion = spec.trap ? new com.bslx.XueXiMod.shu.technique.release.motion.TrapFallMotion() : TechniqueHitKinds.motion(path);
434:         if (spec.tracking && !spec.trap && (path == TechniqueHitSpec.Path.STRAIGHT
435:                 || path == TechniqueHitSpec.Path.DROP || path == TechniqueHitSpec.Path.VORTEX)) {
436:             this.motion = new com.bslx.XueXiMod.shu.technique.release.motion.TrackingMotion(path);
437:         }
438:         this.hitbox = spec.path == null && spec.texture == null ? TechniqueHitKinds.hitbox(spec.hitbox) : new SizedBoxHitbox();
439:         if (path == TechniqueHitSpec.Path.STILL_AIM && this.motion != null && caster.level() instanceof ServerLevel serverLevel) {
440:             this.motion.tick(this, serverLevel);
441:             this.previousPosition = this.position();
442:         } else if ((path == TechniqueHitSpec.Path.VORTEX || path == TechniqueHitSpec.Path.FUNCTION)
443:                 && this.motion != null && caster.level() instanceof ServerLevel vortexLevel) {
444:             this.motion.tick(this, vortexLevel);
445:             this.previousPosition = this.position();
446:         } else if (this.centered && this.anchor != null) {
447:             this.placeAt(this.anchor, this.aim != null ? this.aim : this.forward);
448:             this.previousPosition = this.anchor;
449:         } else {
450:             this.placeAt(this.origin, this.forward);
451:         }
452:     }
453: 
454:     public void placeAt(Vec3 position, Vec3 direction) {
455:         this.setPos(position.x, position.y, position.z);
456:         if (direction != null && direction.lengthSqr() > 1.0E-8) {
457:             Vec3 unitDirection = direction.normalize();
458:             double horizontal = Math.sqrt(unitDirection.x * unitDirection.x + unitDirection.z * unitDirection.z);
459:             this.setYRot((float) (Math.atan2(unitDirection.z, unitDirection.x) * (180.0 / Math.PI)) - 90.0F);
460:             this.setXRot((float) (-Math.atan2(unitDirection.y, horizontal) * (180.0 / Math.PI)));
461:         }
462:     }
463: 
464:     @Nullable
465:     public ServerPlayer getCaster() {
466:         if (this.casterUuid == null || this.level().isClientSide()) {
467:             return null;
468:         }
469:         MinecraftServer server = this.level().getServer();
470:         if (server == null) {
471:             return null;
472:         }
473:         return server.getPlayerList().getPlayer(this.casterUuid);
474:     }
475: 
476:     public int age() {
477:         return this.age;
478:     }
479: 
480:     public int lifetime() {
481:         return this.lifetime;
482:     }
483: 
484:     public double speed() {
485:         return this.speed;
486:     }
487: 
488:     public boolean lockThisCast() {
489:         return this.lockThisCast;
490:     }
491: 
492:     public boolean dropThisCast() {
493:         return this.dropThisCast;
494:     }
495: 
496:     public boolean centered() {
497:         return this.centered;
498:     }
499: 
500:     public boolean pose3d() {
501:         return this.pose3d;
502:     }
503: 
504:     public boolean beam() {
505:         return this.beam;
506:     }
507: 
508:     public boolean pinned() {
509:         return this.pinned;
510:     }
511: 
512:     public boolean planted() {
513:         return this.planted;
514:     }
515: 
516:     public void setPlanted(boolean planted) {
517:         this.planted = planted;
518:     }
519: 
520:     public float roll() {
521:         return this.entityData.get(DATA_ROLL);
522:     }
523: 
524:     public double arcRadius() {
525:         return this.arcRadius;
526:     }
527: 
528:     public double arcDegrees() {
529:         return this.arcDegrees;
530:     }
531: 
532:     public double arcHeight() {
533:         return this.arcHeight;
534:     }
535: 
536:     public Vec3 boxSize() {
537:         return this.boxSize;
538:     }
539: 
540:     public Vec3 origin() {
541:         return this.origin;
542:     }
543: 
544:     public Vec3 forward() {
545:         return this.forward;
546:     }
547: 
548:     public String functionId() {
549:         return this.functionId;
550:     }
551: 
552:     public Vec3 previousPosition() {
553:         return this.previousPosition;
554:     }
555: 
556:     @Nullable
557:     public Vec3 anchor() {
558:         return this.anchor;
559:     }
560: 
561:     @Nullable
562:     public Vec3 aim() {
563:         return this.aim;
564:     }
565: 
566:     public UUID casterUuid() {
567:         return this.casterUuid;
568:     }
569: 
570:     public boolean deflectsProjectiles() {
571:         return this.deflectsIncoming;
572:     }
573: 
574:     @Override
575:     public void remove(RemovalReason reason) {
576:         this.releaseWeepingDeflection();
577:         this.closeArcDeflection();
578:         super.remove(reason);
579:     }
580: 
581:     /** 运动之后、命中之前。空水段也要更新，旧宿主才能马上停。 */
582:     private void refreshWeepingDeflection() {
583:         if (this.level().isClientSide() || !this.persistentFlow) {
584:             return;
585:         }
586:         WeepingDeflectionBinding.update(this, WeepingCurtain.active(this));
587:     }
588: 
589:     private void releaseWeepingDeflection() {
590:         if (this.level().isClientSide() || !this.persistentFlow) {
591:             return;
592:         }
593:         WeepingDeflectionBinding.remove(this);
594:         WeepingCurtain.forget(this);
595:     }
596: 
597:     public void setOrigin(Vec3 origin) {
598:         this.origin = origin;
599:     }
600: 
601:     public void setForward(Vec3 forward) {
602:         this.forward = forward;
603:     }
604: 
605:     public boolean canTrack(LivingEntity target) {
606:         return TechniqueHitSettle.canHit(this, target)
607:                 && (this.hitInterval > 0 || !this.hitOnce.contains(target.getUUID()));
608:     }
609: 
610:     public String getTechniqueId() {
611:         return this.entityData.get(DATA_TECHNIQUE_ID);
612:     }
613: 
614:     public int getVisual() {
615:         return this.entityData.get(DATA_VISUAL);
616:     }
617: 
618:     public int displayCopyIndex() {
619:         return this.entityData.get(DATA_DISPLAY_INDEX);
620:     }
621: 
622:     public int displayCopyCount() {
623:         return this.entityData.get(DATA_DISPLAY_COUNT);
624:     }
625: 
626:     public boolean displayLaneSplit() {
627:         return this.entityData.get(DATA_DISPLAY_LANE);
628:     }
629: 
630:     public int displayLaneIndex() {
631:         return this.entityData.get(DATA_LANE_INDEX);
632:     }
633: 
634:     public int displayLaneCount() {
635:         return this.entityData.get(DATA_LANE_COUNT);
636:     }
637: 
638:     public CohortVisual cohortVisual() {
639:         return CohortVisual.read(this.displayCopyIndex(), this.displayCopyCount(),
640:                 this.displayLaneIndex(), this.displayLaneCount(), this.displayLaneSplit());
641:     }
642: 
643:     public float getModelScale() {
644:         return this.entityData.get(DATA_MODEL_SCALE);
645:     }
646: 
647:     public String texture() {
648:         return this.entityData.get(DATA_TEXTURE);
649:     }
650: 
651:     public StillSheetVisual stillSheet() {
652:         return StillSheetVisual.once(this.texture(), this.entityData.get(DATA_STILL_FRAMES),
653:                 this.entityData.get(DATA_STILL_FRAME_TICKS));
654:     }
655: 
656:     public long visualBirth() {
657:         return this.entityData.get(DATA_STILL_BIRTH);
658:     }
659: 
660:     public boolean stillSheetCancelled() {
661:         return this.entityData.get(DATA_STILL_CANCELLED);
662:     }
663: 
664:     /** 正式激活时写一次出生时间。暂挂和 applySpec 都不调用。 */
665:     public void stampStillBirth() {
666:         if (this.level().isClientSide() || !this.stillSheet().enabled()) {
667:             return;
668:         }
669:         if (this.entityData.get(DATA_STILL_BIRTH) >= 0L) {
670:             return;
671:         }
672:         this.entityData.set(DATA_STILL_BIRTH, this.level().getGameTime());
673:     }
674: 
675:     /** 显式取消视觉。命中到期移除不走这里。 */
676:     public void cancelStillSheet() {
677:         if (this.level().isClientSide()) {
678:             return;
679:         }
680:         this.entityData.set(DATA_STILL_CANCELLED, true);
681:     }
682: 
683:     public double length() {
684:         return this.level().isClientSide() ? this.entityData.get(DATA_LENGTH) : this.length;
685:     }
686: 
687:     public double width() {
688:         return this.level().isClientSide() ? this.entityData.get(DATA_WIDTH) : this.width;
689:     }
690: 
691:     public double height() {
692:         return this.level().isClientSide() ? this.entityData.get(DATA_HEIGHT) : this.height;
693:     }
694: 
695:     public boolean exactVolume() {
696:         return this.exactVolume;
697:     }
698: 
699:     public double hitHeight() {
700:         if (this.exactVolume) {
701:             return this.height() > 0.0 ? this.height() : 0.0;
702:         }
703:         if (this.height() <= 0.05) {
704:             return Math.max(this.length(), 1.0);
705:         }
706:         return Math.max(this.height(), 1.0);
707:     }
708: 
709:     public void resizeVolume(double along, double across, double vertical) {
710:         this.exactVolume = true;
711:         this.length = along;
712:         this.width = across;
713:         this.height = vertical;
714:         this.entityData.set(DATA_LENGTH, (float) along);
715:         this.entityData.set(DATA_WIDTH, (float) across);
716:         this.entityData.set(DATA_HEIGHT, (float) vertical);
717:         double reach = Math.max(along, Math.max(across, vertical)) * 0.5;
718:         this.setBoundingBox(new AABB(
719:                 this.getX() - reach, this.getY() - reach, this.getZ() - reach,
720:                 this.getX() + reach, this.getY() + reach, this.getZ() + reach));
721:     }
722: 
723:     private void installArc(ArcBandSnapshot snapshot) {
724:         this.motion = null;
725:         this.hitbox = null;
726:         this.centered = false;
727:         this.adoptArc(snapshot);
728:         this.forward = new Vec3(snapshot.forwardAxis().east(), snapshot.forwardAxis().above(), snapshot.forwardAxis().south());
729:         this.origin = new Vec3(snapshot.origin().east(), snapshot.origin().above(), snapshot.origin().south());
730:         this.placeAt(this.origin, this.forward);
731:         this.lifetime = snapshot.lifeTicks();
732:     }
733: 
734:     private void adoptArc(ArcBandSnapshot snapshot) {
735:         this.arcSnapshot = snapshot;
736:         this.entityData.set(DATA_ARC_SNAPSHOT, snapshot.pack());
737:     }
738: 
739:     private void tickArcBand(ServerLevel level) {
740:         if (this.arcSnapshot.birthTick() < 0L) {
741:             return;
742:         }
743:         long ageTicks = level.getGameTime() - this.arcSnapshot.birthTick();
744:         if (ageTicks < 0L) {
745:             ageTicks = 0L;
746:         }
747:         ProgressArcBand.SweepCursor cursor = ProgressArcBand.advance(
748:                 this.lastProcessedProgress, this.arcDamageClosed, ageTicks, this.arcSnapshot.activeTicks());
749:         if (cursor.swept()) {
750:             ArcBounds broad = ProgressArcBand.sliceBroadBounds(this.arcSnapshot, cursor.fromProgress(), cursor.toProgress());
751:             AABB searchBox = new AABB(broad.lowEast(), broad.lowAbove(), broad.lowSouth(),
752:                     broad.highEast(), broad.highAbove(), broad.highSouth());
753:             List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, searchBox,
754:                     entity -> TechniqueHitSettle.canHit(this, entity));
755:             for (LivingEntity target : targets) {
756:                 AABB targetBox = target.getBoundingBox();
757:                 ArcBounds targetBounds = new ArcBounds(targetBox.minX, targetBox.minY, targetBox.minZ,
758:                         targetBox.maxX, targetBox.maxY, targetBox.maxZ);
759:                 if (ProgressArcBand.intersectsSlice(this.arcSnapshot, targetBounds, cursor.fromProgress(), cursor.toProgress())) {
760:                     TechniqueHitSettle.tryHit(this, level, target);
761:                 }
762:             }
763:         }
764:         this.lastProcessedProgress = cursor.lastProgress();
765:         this.arcDamageClosed = cursor.damageClosed();
766:         this.refreshArcDeflection(level, ageTicks);
767:         if (ageTicks >= this.arcSnapshot.lifeTicks()) {
768:             TechniqueHitSettle.onExpire(this, level);
769:         }
770:     }
771: 
772:     private void refreshArcDeflection(ServerLevel level, long ageTicks) {
773:         if (!this.deflectsIncoming || this.level().isClientSide()) {
774:             this.closeArcDeflection();
775:             return;
776:         }
777:         ArcBandSnapshot snapshot = this.arcSnapshot;
778:         boolean open = snapshot != null
779:                 && snapshot.mode() == ArcBandMode.RADIAL_BLADE
780:                 && snapshot.birthTick() >= 0L
781:                 && ageTicks >= 0L
782:                 && ageTicks <= snapshot.activeTicks()
783:                 && this.isAlive()
784:                 && !this.isRemoved()
785:                 && this.casterUuid != null;
786:         if (!open) {
787:             this.closeArcDeflection();
788:             return;
789:         }
790:         if (this.arcDeflection == null) {
791:             this.arcDeflection = ArcDeflectionHost.bind(this);
792:             if (this.arcDeflection != null) {
793:                 DeflectionHosts.register(this.arcDeflection);
794:             }
795:         }
796:     }
797: 
798:     private void closeArcDeflection() {
799:         if (this.arcDeflection == null) {
800:             return;
801:         }
802:         DeflectionHosts.unregister(this.arcDeflection.key());
803:         this.arcDeflection = null;
804:     }
805: 
806:     @Nullable
807:     private AABB arcWorldBox() {
808:         ArcBandSnapshot snapshot = this.arcBand();
809:         if (snapshot == null) {
810:             return null;
811:         }
812:         ArcBounds broad = ProgressArcBand.sliceBroadBounds(snapshot, 0.0, 1.0);
813:         return new AABB(broad.lowEast(), broad.lowAbove(), broad.lowSouth(),
814:                 broad.highEast(), broad.highAbove(), broad.highSouth());
815:     }
816: }
817: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/WeepingCurtain.java">
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
65:         WaterSegment segment = segmentFor(clip, lip, entity.forward(), across, thick, entity.age());
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
119:     /** 已知足端后的相位。不探地。 */
120:     public static WaterSegment flowSegment(Vec3 lip, Vec3 look, double footDrop, boolean floorFoot,
121:                                            double across, double thick, int age) {
122:         Vec3 mouth = lip == null ? Vec3.ZERO : lip;
123:         int fillTicks = WeepingSettings.FILL_TICKS;
124:         if (fillTicks < 1) {
125:             fillTicks = 1;
126:         }
127:         int lifeTicks = WeepingSettings.LIFETIME;
128:         int drainStart = WeepingSettings.DRAIN_START;
129:         int drainWindow = lifeTicks - drainStart;
130:         if (drainWindow < 1) {
131:             drainWindow = 1;
132:         }
133:         int lived = Math.max(age, 0);
134:         double head = lived >= fillTicks ? 1.0 : lived / (double) fillTicks;
135:         double tail = lived <= drainStart ? 0.0 : (lived - drainStart) / (double) drainWindow;
136:         if (tail > 1.0) {
137:             tail = 1.0;
138:         }
139:         if (tail > head) {
140:             tail = head;
141:         }
142:         double safeFoot = footDrop;
143:         if (!Double.isFinite(safeFoot) || safeFoot < 0.0) {
144:             safeFoot = 0.0;
145:         }
146:         double upperDrop = safeFoot * tail;
147:         double lowerDrop = safeFoot * head;
148:         double openSpan = lowerDrop - upperDrop;
149:         boolean flowing = openSpan > WaterSegment.PRESENT_GAP;
150:         double middleDrop = (upperDrop + lowerDrop) * 0.5;
151:         Vec3 center = new Vec3(mouth.x, mouth.y - middleDrop, mouth.z);
152:         return new WaterSegment(mouth, center, look, upperDrop, lowerDrop, safeFoot, across, thick, flowing, floorFoot);
153:     }
154: 
155:     /** 源被堵住或预算不够时不给水体。其余走相位。 */
156:     public static WaterSegment segmentFor(FootClip clip, Vec3 lip, Vec3 look, double across, double thick, int age) {
157:         if (clip == null || clip.sourceInside() || clip.overBudget() || !Double.isFinite(clip.footDrop())) {
158:             return WaterSegment.absent(lip, look, across, thick);
159:         }
160:         return flowSegment(lip, look, clip.footDrop(), clip.floorFoot(), across, thick, age);
161:     }
162: 
163:     /** 只有水头到达真实朝上地面时才溅水。 */
164:     public static boolean splashes(int age, FootClip clip, WaterSegment segment) {
165:         return clip != null
166:                 && segment != null
167:                 && !clip.sourceInside()
168:                 && !clip.overBudget()
169:                 && clip.floorFoot()
170:                 && segment.present()
171:                 && age == WeepingSettings.FILL_TICKS;
172:     }
173: 
174:     /** 足端在唇口正下方，不往前倾。 */
175:     public static Vec3 footPoint(WaterSegment segment) {
176:         if (segment == null) {
177:             return Vec3.ZERO;
178:         }
179:         return new Vec3(segment.lip().x, segment.lip().y - segment.footDrop(), segment.lip().z);
180:     }
181: 
182:     public static long footprintCellCount(Vec3 lip, Vec3 look, double across, double thick) {
183:         PreparedColumn prepared = prepare(lip, look, across, thick);
184:         if (prepared == null) {
185:             return 0L;
186:         }
187:         return prepared.cellCount();
188:     }
189: 
190:     public static long notedFootprintCells() {
191:         return notedCells;
192:     }
193: 
194:     public static boolean footprintBudgetRejected() {
195:         return notedOverBudget;
196:     }
197: 
198:     /** 用已经取到的碰撞盒裁切。盒子要是世界坐标。超预算时不读这些盒子。 */
199:     public static FootClip clipFootprint(Vec3 lip, Vec3 look, double across, double thick, List<AABB> boxes) {
200:         PreparedColumn prepared = prepare(lip, look, across, thick);
201:         if (prepared == null) {
202:             noteFootprint(0L, true);
203:             return FootClip.budgetStop(0L);
204:         }
205:         if (prepared.cellCount() > FOOTPRINT_CELL_BUDGET) {
206:             noteFootprint(prepared.cellCount(), true);
207:             return FootClip.budgetStop(prepared.cellCount());
208:         }
209:         noteFootprint(prepared.cellCount(), false);
210:         return clipBoxes(prepared, boxes);
211:     }
212: 
213:     /**
214:      * 从世界读取碰撞形状后再裁切。形状以方块自己的格为原点，这里平移一次。
215:      * 只读方块，不改地形。没有世界或格子超预算时不扫描。
216:      */
217:     public static FootClip probeTerrain(ServerLevel level, Vec3 lip, Vec3 look, double across, double thick) {
218:         PreparedColumn prepared = prepare(lip, look, across, thick);
219:         if (prepared == null || level == null || prepared.cellCount() > FOOTPRINT_CELL_BUDGET) {
220:             long cellCount = prepared == null ? 0L : prepared.cellCount();
221:             noteFootprint(cellCount, true);
222:             return FootClip.budgetStop(cellCount);
223:         }
224:         noteFootprint(prepared.cellCount(), false);
225:         return clipBoxes(prepared, gatherTerrain(level, prepared.range()));
226:     }
227: 
228:     private static PreparedColumn prepare(Vec3 lip, Vec3 look, double across, double thick) {
229:         double limit = WeepingSettings.FALL_DISTANCE_LIMIT;
230:         if (!Double.isFinite(limit) || limit <= 0.0) {
231:             return null;
232:         }
233:         Vec3 mouth = lip;
234:         if (mouth == null || !Double.isFinite(mouth.x) || !Double.isFinite(mouth.y) || !Double.isFinite(mouth.z)) {
235:             mouth = Vec3.ZERO;
236:         }
237:         double fullAcross = across;
238:         double fullThick = thick;
239:         if (!Double.isFinite(fullAcross) || fullAcross < 0.0) {
240:             fullAcross = 0.0;
241:         }
242:         if (!Double.isFinite(fullThick) || fullThick < 0.0) {
243:             fullThick = 0.0;
244:         }
245:         FlatFrame frame = FlatFrame.of(look);
246:         AABB bounds = columnBounds(mouth, frame, fullAcross * 0.5, fullThick * 0.5, limit);
247:         BlockRange range = BlockRange.of(bounds);
248:         return new PreparedColumn(mouth, frame, fullAcross * 0.5, fullThick * 0.5, limit, range, range.cellCount());
249:     }
250: 
251:     private static FootClip clipBoxes(PreparedColumn prepared, List<AABB> boxes) {
252:         if (boxes == null || boxes.isEmpty()) {
253:             return FootClip.openAir(prepared.dropLimit(), prepared.cellCount());
254:         }
255:         boolean buried = false;
256:         boolean foundTop = false;
257:         double highestTop = Double.NEGATIVE_INFINITY;
258:         for (AABB box : boxes) {
259:             if (!boxUsable(box) || !overlapsFootprint(prepared, box)) {
260:                 continue;
261:             }
262:             if (box.minY < prepared.mouth().y - PLANE_SLOP && box.maxY > prepared.mouth().y + PLANE_SLOP) {
263:                 buried = true;
264:                 break;
265:             }
266:             if (box.maxY <= prepared.mouth().y + PLANE_SLOP
267:                     && box.maxY > prepared.mouth().y - prepared.dropLimit() + PLANE_SLOP) {
268:                 if (!foundTop || box.maxY > highestTop) {
269:                     highestTop = box.maxY;
270:                     foundTop = true;
271:                 }
272:             }
273:         }
274:         if (buried) {
275:             return FootClip.blockedSource(prepared.cellCount());
276:         }
277:         if (!foundTop) {
278:             return FootClip.openAir(prepared.dropLimit(), prepared.cellCount());
279:         }
280:         double drop = prepared.mouth().y - highestTop;
281:         if (!Double.isFinite(drop) || drop < 0.0) {
282:             drop = 0.0;
283:         }
284:         if (drop > prepared.dropLimit()) {
285:             return FootClip.openAir(prepared.dropLimit(), prepared.cellCount());
286:         }
287:         return new FootClip(drop, true, false, false, prepared.cellCount());
288:     }
289: 
290:     private static boolean overlapsFootprint(PreparedColumn prepared, AABB box) {
291:         Vec3 ahead = prepared.frame().ahead();
292:         Vec3 right = prepared.frame().right();
293:         double centerX = (box.minX + box.maxX) * 0.5;
294:         double centerZ = (box.minZ + box.maxZ) * 0.5;
295:         double halfBoxX = (box.maxX - box.minX) * 0.5;
296:         double halfBoxZ = (box.maxZ - box.minZ) * 0.5;
297:         double offsetX = centerX - prepared.mouth().x;
298:         double offsetZ = centerZ - prepared.mouth().z;
299:         double curtainRadiusX = Math.abs(ahead.x) * prepared.halfThick() + Math.abs(right.x) * prepared.halfAcross();
300:         if (Math.abs(offsetX) > halfBoxX + curtainRadiusX - SHAPE_SLOP) {
301:             return false;
302:         }
303:         double curtainRadiusZ = Math.abs(ahead.z) * prepared.halfThick() + Math.abs(right.z) * prepared.halfAcross();
304:         if (Math.abs(offsetZ) > halfBoxZ + curtainRadiusZ - SHAPE_SLOP) {
305:             return false;
306:         }
307:         double aheadOffset = offsetX * ahead.x + offsetZ * ahead.z;
308:         double boxOnAhead = halfBoxX * Math.abs(ahead.x) + halfBoxZ * Math.abs(ahead.z);
309:         if (Math.abs(aheadOffset) > prepared.halfThick() + boxOnAhead - SHAPE_SLOP) {
310:             return false;
311:         }
312:         double rightOffset = offsetX * right.x + offsetZ * right.z;
313:         double boxOnRight = halfBoxX * Math.abs(right.x) + halfBoxZ * Math.abs(right.z);
314:         if (Math.abs(rightOffset) > prepared.halfAcross() + boxOnRight - SHAPE_SLOP) {
315:             return false;
316:         }
317:         return true;
318:     }
319: 
320:     private static List<AABB> gatherTerrain(ServerLevel level, BlockRange range) {
321:         List<AABB> gathered = new ArrayList<>();
322:         BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
323:         CollisionContext emptyContext = CollisionContext.empty();
324:         for (int columnX = range.westEdge(); columnX <= range.eastEdge(); columnX++) {
325:             for (int layerY = range.lowEdge(); layerY <= range.highEdge(); layerY++) {
326:                 for (int columnZ = range.northEdge(); columnZ <= range.southEdge(); columnZ++) {
327:                     cursor.set(columnX, layerY, columnZ);
328:                     BlockState state = level.getBlockState(cursor);
329:                     if (state.isAir()) {
330:                         continue;
331:                     }
332:                     VoxelShape shape = state.getCollisionShape(level, cursor, emptyContext);
333:                     if (shape == null || shape.isEmpty()) {
334:                         continue;
335:                     }
336:                     VoxelShape moved = shape.move(columnX, layerY, columnZ);
337:                     for (AABB part : moved.toAabbs()) {
338:                         if (boxUsable(part)) {
339:                             gathered.add(part);
340:                         }
341:                     }
342:                 }
343:             }
344:         }
345:         return gathered;
346:     }
347: 
348:     private static AABB columnBounds(Vec3 mouth, FlatFrame frame, double halfAcross, double halfThick, double dropLimit) {
349:         Vec3 ahead = frame.ahead();
350:         Vec3 right = frame.right();
351:         double west = mouth.x;
352:         double east = mouth.x;
353:         double north = mouth.z;
354:         double south = mouth.z;
355:         double[] cornerAlong = new double[] {halfThick, halfThick, -halfThick, -halfThick};
356:         double[] cornerAcross = new double[] {halfAcross, -halfAcross, halfAcross, -halfAcross};
357:         for (int cornerIndex = 0; cornerIndex < cornerAlong.length; cornerIndex++) {
358:             double sampleX = mouth.x + ahead.x * cornerAlong[cornerIndex] + right.x * cornerAcross[cornerIndex];
359:             double sampleZ = mouth.z + ahead.z * cornerAlong[cornerIndex] + right.z * cornerAcross[cornerIndex];
360:             west = Math.min(west, sampleX);
361:             east = Math.max(east, sampleX);
362:             north = Math.min(north, sampleZ);
363:             south = Math.max(south, sampleZ);
364:         }
365:         return new AABB(west, mouth.y - dropLimit, north, east, mouth.y, south);
366:     }
367: 
368:     private static boolean boxUsable(AABB box) {
369:         return box != null
370:                 && Double.isFinite(box.minX) && Double.isFinite(box.minY) && Double.isFinite(box.minZ)
371:                 && Double.isFinite(box.maxX) && Double.isFinite(box.maxY) && Double.isFinite(box.maxZ)
372:                 && box.maxX >= box.minX && box.maxY >= box.minY && box.maxZ >= box.minZ;
373:     }
374: 
375:     private static void noteFootprint(long cellCount, boolean rejected) {
376:         notedCells = cellCount;
377:         notedOverBudget = rejected;
378:     }
379: 
380:     /**
381:      * 一列水的裁切结果。{@code floorFoot} 只在落到朝上的盒顶，并且落差小于最大落差时为真。
382:      */
383:     public record FootClip(double footDrop, boolean floorFoot, boolean sourceInside, boolean overBudget, long cellCount) {
384:         public static FootClip openAir(double footDrop, long cellCount) {
385:             double drop = footDrop;
386:             if (!Double.isFinite(drop) || drop < 0.0) {
387:                 drop = 0.0;
388:             }
389:             return new FootClip(drop, false, false, false, cellCount);
390:         }
391: 
392:         public static FootClip blockedSource(long cellCount) {
393:             return new FootClip(0.0, false, true, false, cellCount);
394:         }
395: 
396:         public static FootClip budgetStop(long cellCount) {
397:             return new FootClip(0.0, false, false, true, cellCount);
398:         }
399:     }
400: 
401:     private record FlatFrame(Vec3 ahead, Vec3 right) {
402:         private static FlatFrame of(Vec3 look) {
403:             Vec3 ahead = WaterSegment.horizontalAhead(look);
404:             return new FlatFrame(ahead, new Vec3(ahead.z, 0.0, -ahead.x));
405:         }
406:     }
407: 
408:     private record PreparedColumn(Vec3 mouth, FlatFrame frame, double halfAcross, double halfThick,
409:                                   double dropLimit, BlockRange range, long cellCount) {
410:     }
411: 
412:     private record BlockRange(int westEdge, int eastEdge, int lowEdge, int highEdge,
413:                               int northEdge, int southEdge, long cellCount) {
414:         private static BlockRange of(AABB bounds) {
415:             int westEdge = Mth.floor(bounds.minX);
416:             int eastEdge = Mth.floor(bounds.maxX - EDGE_SLOP);
417:             int lowEdge = Mth.floor(bounds.minY);
418:             int highEdge = Mth.floor(bounds.maxY - EDGE_SLOP);
419:             int northEdge = Mth.floor(bounds.minZ);
420:             int southEdge = Mth.floor(bounds.maxZ - EDGE_SLOP);
421:             if (eastEdge < westEdge) {
422:                 eastEdge = westEdge;
423:             }
424:             if (highEdge < lowEdge) {
425:                 highEdge = lowEdge;
426:             }
427:             if (southEdge < northEdge) {
428:                 southEdge = northEdge;
429:             }
430:             long spanX = (long) eastEdge - westEdge + 1L;
431:             long spanY = (long) highEdge - lowEdge + 1L;
432:             long spanZ = (long) southEdge - northEdge + 1L;
433:             long cellCount;
434:             if (spanX > FOOTPRINT_CELL_BUDGET || spanY > FOOTPRINT_CELL_BUDGET || spanZ > FOOTPRINT_CELL_BUDGET) {
435:                 cellCount = (long) FOOTPRINT_CELL_BUDGET + 1L;
436:             } else {
437:                 cellCount = spanX * spanY * spanZ;
438:             }
439:             return new BlockRange(westEdge, eastEdge, lowEdge, highEdge, northEdge, southEdge, cellCount);
440:         }
441:     }
442: }
443: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/WeepingSettings.java">
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
20: }
21: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/effects/HeatBeamEffect.java">
1: package com.bslx.XueXiMod.shu.technique.effects;
2: 
3: import com.bslx.XueXiMod.shu.Technique;
4: import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
5: import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
6: import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
7: import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
8: import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
9: import net.minecraft.server.level.ServerLevel;
10: import net.minecraft.server.level.ServerPlayer;
11: import net.minecraft.world.entity.LivingEntity;
12: import net.minecraft.world.item.ItemStack;
13: import net.minecraft.world.phys.Vec3;
14: 
15: import java.util.concurrent.atomic.AtomicBoolean;
16: import java.util.function.Consumer;
17: 
18: public class HeatBeamEffect implements TechniqueEffect {
19:     @Override
20:     public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
21:         ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
22:         int lifetime = definition != null ? definition.lifetimeTicks() : 40;
23: 
24:         if (player.level() instanceof ServerLevel serverLevel) {
25:             Consumer<Vec3> trigger = cast.onFirstTrigger();
26:             AtomicBoolean triggered = new AtomicBoolean(false);
27:             TechniqueHitSpec spec = new TechniqueHitSpec();
28:             spec.techniqueId = technique.getId();
29:             spec.path = TechniqueHitSpec.Path.FOLLOW;
30:             spec.speed = 0.0;
31:             spec.length = 6.0;
32:             spec.width = 1.0;
33:             spec.height = 1.0;
34:             spec.lifetime = lifetime;
35:             spec.hitInterval = 10;
36:             spec.lockThisCast = cast.lockThisCast();
37:             spec.dropThisCast = cast.dropThisCast();
38:             spec.visual = 2;
39:             spec.listener = new TechniqueHitSpec.HitListener() {
40:                 @Override
41:                 public void onHit(LivingEntity target) {
42:                     if (trigger != null && triggered.compareAndSet(false, true)) {
43:                         trigger.accept(target.position());
44:                     }
45:                 }
46: 
47:                 @Override
48:                 public void onExpire(Vec3 position) {
49:                     if (trigger != null && triggered.compareAndSet(false, true)) {
50:                         trigger.accept(position);
51:                     }
52:                 }
53:             };
54:             TechniqueHitSpec.spawn(serverLevel, player, spec);
55:         }
56:     }
57: }
58: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/effects/TestSustainedEffect.java">
1: package com.bslx.XueXiMod.shu.technique.effects;
2: 
3: import com.bslx.XueXiMod.shu.Technique;
4: import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
5: import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
6: import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
7: import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
8: import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
9: import net.minecraft.server.level.ServerLevel;
10: import net.minecraft.server.level.ServerPlayer;
11: import net.minecraft.world.entity.LivingEntity;
12: import net.minecraft.world.item.ItemStack;
13: import net.minecraft.world.phys.Vec3;
14: 
15: import java.util.concurrent.atomic.AtomicBoolean;
16: import java.util.function.Consumer;
17: 
18: public class TestSustainedEffect implements TechniqueEffect {
19:     @Override
20:     public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
21:         float damage = cast.getDamage();
22: 
23:         ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
24:         float speed = definition != null ? definition.speed() : 0.4f;
25:         int lifetime = definition != null ? definition.lifetimeTicks() : 20;
26: 
27:         if (player.level() instanceof ServerLevel serverLevel) {
28:             Consumer<Vec3> trigger = cast.onFirstTrigger();
29:             AtomicBoolean triggered = new AtomicBoolean(false);
30: 
31:             TechniqueHitSpec spec = new TechniqueHitSpec();
32:             spec.path = TechniqueHitSpec.Path.DROP;
33:             spec.length = 0.3;
34:             spec.width = 0.3;
35:             spec.height = 0.3;
36:             spec.speed = speed;
37:             spec.lifetime = lifetime;
38:             spec.lockThisCast = false;
39:             spec.dropThisCast = true;
40:             spec.visual = 1;
41:             spec.techniqueId = technique.getId();
42:             spec.listener = new TechniqueHitSpec.HitListener() {
43:                 @Override
44:                 public void onHit(LivingEntity target) {
45:                     if (trigger != null && triggered.compareAndSet(false, true)) {
46:                         trigger.accept(target.position());
47:                     }
48:                 }
49: 
50:                 @Override
51:                 public void onExpire(Vec3 position) {
52:                     if (trigger != null && triggered.compareAndSet(false, true)) {
53:                         trigger.accept(position);
54:                     }
55:                 }
56:             };
57:             TechniqueHitSpec.spawn(serverLevel, player, spec);
58:         }
59:     }
60: }
61: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/effects/WeepingCurtainEffect.java">
1: package com.bslx.XueXiMod.shu.technique.effects;
2: 
3: import com.bslx.XueXiMod.shu.Technique;
4: import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
5: import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
6: import com.bslx.XueXiMod.shu.technique.release.CastFrame;
7: import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
8: import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
9: import com.bslx.XueXiMod.shu.technique.release.WeepingCurtain;
10: import com.bslx.XueXiMod.shu.technique.release.WeepingSettings;
11: import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
12: import net.minecraft.server.level.ServerLevel;
13: import net.minecraft.server.level.ServerPlayer;
14: import net.minecraft.world.item.ItemStack;
15: import net.minecraft.world.phys.Vec3;
16: 
17: /** 垂丝：一份一个盒子，沿着当前水帘移动。水花没有单独的命中体。 */
18: public final class WeepingCurtainEffect implements TechniqueEffect {
19:     private static final double STRIP_STEP = 0.72;
20: 
21:     @Override
22:     public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
23:         if (!(player.level() instanceof ServerLevel serverLevel)) {
24:             return;
25:         }
26:         ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
27:         CastFrame frame = CastFrame.current();
28:         int lifetime = definition != null ? definition.lifetimeTicks() : WeepingSettings.LIFETIME;
29:         TechniqueHitSpec spec = new TechniqueHitSpec();
30:         spec.techniqueId = technique.getId();
31:         spec.path = TechniqueHitSpec.Path.FUNCTION;
32:         spec.functionId = WeepingCurtain.FUNCTION_ID;
33:         spec.length = 0.35;
34:         spec.width = 0.5;
35:         spec.height = 0.4;
36:         spec.speed = 0.0;
37:         spec.lifetime = lifetime;
38:         spec.hitInterval = WeepingSettings.HIT_INTERVAL;
39:         spec.visual = frame != null && frame.heavyWater ? 8 : 7;
40:         spec.lockThisCast = cast.lockThisCast();
41:         spec.dropThisCast = cast.dropThisCast();
42:         if (frame == null || !frame.overrideGeometry) {
43:             Vec3 look = player.getLookAngle();
44:             Vec3 ahead = new Vec3(look.x, 0.0, look.z);
45:             if (ahead.lengthSqr() < 1.0E-6) {
46:                 ahead = new Vec3(0.0, 0.0, 1.0);
47:             } else {
48:                 ahead = ahead.normalize();
49:             }
50:             Vec3 right = new Vec3(ahead.z, 0.0, -ahead.x);
51:             int index = frame == null ? 0 : frame.copyIndex;
52:             int total = frame == null ? 1 : Math.max(1, frame.copyCount);
53:             double side = (index - (total - 1) * 0.5) * STRIP_STEP;
54:             spec.start = player.getEyePosition().add(ahead.scale(2.2)).add(right.scale(side));
55:             spec.aim = ahead;
56:         }
57:         TechniqueHitSpec.spawn(serverLevel, player, spec);
58:     }
59: }
60: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/field/FieldSpawner.java">
1: package com.bslx.XueXiMod.shu.technique.release.field;
2: 
3: import com.bslx.XueXiMod.shu.Technique;
4: import com.bslx.XueXiMod.shu.technique.release.ChainRole;
5: import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
6: import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
7: import com.bslx.XueXiMod.shu.technique.release.Trajectory;
8: import net.minecraft.server.level.ServerPlayer;
9: import net.minecraft.world.phys.Vec3;
10: 
11: import java.util.List;
12: 
13: /** 场地生成在身体中点。链上的索敌和弹道只附加，不改这个点。 */
14: public final class FieldSpawner {
15:     private FieldSpawner() {
16:     }
17: 
18:     public static void releaseFromChain(ServerPlayer player, List<Technique> techniques, int actionIndex,
19:                                         ReleaseDefinition definition, int copies) {
20:         boolean attachedLock = false;
21:         boolean attachedTracking = false;
22:         Trajectory attachedPath = null;
23:         for (int earlierIndex = actionIndex - 1; earlierIndex >= 0; earlierIndex--) {
24:             Technique earlier = techniques.get(earlierIndex);
25:             if (earlier.getBaseDamage() > 0 && !earlier.isEnchantment()) {
26:                 break;
27:             }
28:             ChainRole role = roleOf(earlier);
29:             if (role == ChainRole.SEEK_ENTITY) {
30:                 attachedLock = true;
31:             } else if (role == ChainRole.TRAJECTORY_TRACKING) {
32:                 attachedTracking = true;
33:             } else if (role == ChainRole.TRAJECTORY_DROP && attachedPath == null) {
34:                 ReleaseDefinition pathDefinition = ReleaseRegistry.get(earlier.getId());
35:                 if (pathDefinition != null) {
36:                     attachedPath = pathDefinition.trajectory();
37:                 }
38:             }
39:         }
40:         releaseAttached(player, definition, copies, attachedLock, attachedPath, attachedTracking);
41:     }
42: 
43:     public static void releaseAttached(ServerPlayer player, ReleaseDefinition definition, int copies,
44:                                        boolean attachedLock, Trajectory attachedPath, boolean attachedTracking) {
45:         if (player == null || definition == null || definition.fieldRadius() <= 0f || copies <= 0) {
46:             return;
47:         }
48:         Vec3 anchor = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
49:         int made = 0;
50:         while (made < copies) {
51:             FieldZones.add(player, anchor, definition.fieldRadius(), definition.fieldLifetimeTicks(),
52:                     definition.fieldUpkeepCost(), definition.fieldUpkeepPeriod(), attachedPath, attachedLock, attachedTracking,
53:                     definition.healAmount(), definition.pullStrength(), definition.wetnessGrant(),
54:                     definition.purifyOnHit(), definition.pressurePulse(), definition.erosionPulse(),
55:                     definition.unloadRatio(), definition.slowTicks(), definition.endBurstDamage());
56:             made += 1;
57:         }
58:     }
59: 
60:     private static ChainRole roleOf(Technique technique) {
61:         ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
62:         if (definition == null) {
63:             return ChainRole.ATTACK;
64:         }
65:         return definition.role();
66:     }
67: }
68: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/field/FieldZone.java">
1: package com.bslx.XueXiMod.shu.technique.release.field;
2: 
3: import com.bslx.XueXiMod.shu.technique.release.Trajectory;
4: import com.bslx.XueXiMod.shu.technique.release.contact.ContactEnchantState;
5: import net.minecraft.server.level.ServerLevel;
6: import net.minecraft.resources.ResourceKey;
7: import net.minecraft.world.level.Level;
8: import net.minecraft.world.phys.Vec3;
9: 
10: import java.util.UUID;
11: 
12: /** 钉在世界坐标上的场地。索敌和弹道只记在上面，不挪生成点。 */
13: final class FieldZone {
14:     final UUID ownerId;
15:     final ResourceKey<Level> dimension;
16:     final Vec3 anchor;
17:     final double radius;
18:     final int expireTick;
19:     int nextUpkeepTick;
20:     final int upkeepCost;
21:     final int upkeepPeriod;
22:     final Trajectory attachedPath;
23:     final boolean attachedLock;
24:     final boolean attachedTracking;
25:     final float healAmount;
26:     final float pullStrength;
27:     final int wetnessGrant;
28:     final boolean purify;
29:     final float pressurePulse;
30:     final float erosionPulse;
31:     final float unloadRatio;
32:     final int slowTicks;
33:     final float endBurstDamage;
34:     boolean closed;
35:     ContactEnchantState contact;
36:     int bornTick;
37:     ServerLevel serverLevel;
38:     final UUID hostId = UUID.randomUUID();
39: 
40:     FieldZone(UUID ownerId, ResourceKey<Level> dimension, Vec3 anchor, double radius,
41:               int expireTick, int nextUpkeepTick, int upkeepCost, int upkeepPeriod,
42:               Trajectory attachedPath, boolean attachedLock, boolean attachedTracking, float healAmount, float pullStrength,
43:               int wetnessGrant, boolean purify, float pressurePulse, float erosionPulse,
44:               float unloadRatio, int slowTicks, float endBurstDamage) {
45:         this.ownerId = ownerId;
46:         this.dimension = dimension;
47:         this.anchor = anchor;
48:         this.radius = radius;
49:         this.expireTick = expireTick;
50:         this.nextUpkeepTick = nextUpkeepTick;
51:         this.upkeepCost = upkeepCost;
52:         this.upkeepPeriod = upkeepPeriod;
53:         this.attachedPath = attachedPath;
54:         this.attachedLock = attachedLock;
55:         this.attachedTracking = attachedTracking;
56:         this.healAmount = healAmount;
57:         this.pullStrength = pullStrength;
58:         this.wetnessGrant = wetnessGrant;
59:         this.purify = purify;
60:         this.pressurePulse = pressurePulse;
61:         this.erosionPulse = erosionPulse;
62:         this.unloadRatio = unloadRatio;
63:         this.slowTicks = slowTicks;
64:         this.endBurstDamage = endBurstDamage;
65:     }
66: }
67: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/field/FieldZones.java">
1: package com.bslx.XueXiMod.shu.technique.release.field;
2: 
3: import com.bslx.XueXiMod.player.MagicPool;
4: import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
5: import com.bslx.XueXiMod.shu.technique.release.contact.ContactCarrier;
6: import com.bslx.XueXiMod.shu.technique.release.contact.ContactEnchantState;
7: import com.bslx.XueXiMod.shu.technique.release.TechniqueHits;
8: import com.bslx.XueXiMod.shu.technique.release.Trajectory;
9: import com.bslx.XueXiMod.status.WetnessState;
10: import net.minecraft.world.effect.MobEffectInstance;
11: import net.minecraft.world.effect.MobEffects;
12: import net.minecraft.core.particles.ParticleTypes;
13: import net.minecraft.server.MinecraftServer;
14: import net.minecraft.server.level.ServerLevel;
15: import net.minecraft.server.level.ServerPlayer;
16: import net.minecraft.world.entity.LivingEntity;
17: import net.minecraft.world.phys.AABB;
18: import net.minecraft.world.phys.Vec3;
19: 
20: import java.util.ArrayList;
21: import java.util.Iterator;
22: import java.util.UUID;
23: 
24: /** 服务端场地表。重叠允许。维护费失败、死亡、下线或换维度就拆掉。 */
25: public final class FieldZones {
26:     private static final ArrayList<FieldZone> ZONES = new ArrayList<>();
27: 
28:     private FieldZones() {
29:     }
30: 
31:     public static void add(ServerPlayer owner, Vec3 anchor, double radius, int lifetimeTicks,
32:                            int upkeepCost, int upkeepPeriod, Trajectory attachedPath, boolean attachedLock, boolean attachedTracking,
33:                            float healAmount, float pullStrength, int wetnessGrant, boolean purify,
34:                            float pressurePulse, float erosionPulse, float unloadRatio, int slowTicks,
35:                            float endBurstDamage) {
36:         if (owner == null || anchor == null || radius <= 0.0 || !(owner.level() instanceof ServerLevel level)) {
37:             return;
38:         }
39:         int now = level.getServer().getTickCount();
40:         int life = lifetimeTicks > 0 ? lifetimeTicks : 200;
41:         int period = upkeepPeriod > 0 ? upkeepPeriod : 20;
42:         FieldZone zone = new FieldZone(owner.getUUID(), level.dimension(), anchor, radius, now + life,
43:                 now + period, Math.max(0, upkeepCost), period, attachedPath, attachedLock, attachedTracking,
44:                 healAmount, pullStrength, wetnessGrant, purify, pressurePulse, erosionPulse,
45:                 unloadRatio, slowTicks, endBurstDamage);
46:         zone.bornTick = now;
47:         zone.serverLevel = level;
48:         ZONES.add(zone);
49:     }
50: 
51:     public static void retireLegacyMist() {
52:         for (FieldZone zone : new ArrayList<>(ZONES)) {
53:             if (!zone.closed && Math.abs(zone.radius - 4.5) < 0.05 && zone.wetnessGrant == 2 && zone.upkeepCost == 1) {
54:                 retire(zone);
55:             }
56:         }
57:     }
58: 
59:     public static void retireLegacyPressure() {
60:         for (FieldZone zone : new ArrayList<>(ZONES)) {
61:             if (!zone.closed && Math.abs(zone.radius - 5.0) < 0.05 && zone.wetnessGrant == 4
62:                     && zone.upkeepCost == 4 && zone.pressurePulse > 0f && zone.endBurstDamage > 0f) {
63:                 retire(zone);
64:             }
65:         }
66:     }
67: 
68:     public static void bindBorn(java.util.UUID ownerId, EnchantmentSnapshot snapshot, int bornTick) {
69:         for (FieldZone zone : ZONES) {
70:             if (zone.closed || zone.contact != null || !zone.ownerId.equals(ownerId) || zone.bornTick != bornTick) {
71:                 continue;
72:             }
73:             zone.contact = ContactEnchantState.open(new FieldTouch(zone), snapshot);
74:         }
75:     }
76: 
77:     public static void closeOwned(UUID ownerId) {
78:         for (FieldZone zone : ZONES) {
79:             if (zone.ownerId.equals(ownerId)) {
80:                 retire(zone);
81:             }
82:         }
83:     }
84: 
85:     public static FieldInfluence influence(ServerPlayer player) {
86:         if (player == null) {
87:             return FieldInfluence.NONE;
88:         }
89:         int occupied = 0;
90:         Trajectory attachedPath = null;
91:         boolean attachedLock = false;
92:         boolean attachedTracking = false;
93:         float healAmount = 0f;
94:         float pullStrength = 0f;
95:         int wetnessGrant = 0;
96:         boolean purify = false;
97:         Vec3 pullAnchor = null;
98:         for (FieldZone zone : ZONES) {
99:             if (!covers(zone, player)) {
100:                 continue;
101:             }
102:             occupied += 1;
103:             attachedTracking |= zone.attachedTracking;
104:             if (attachedPath == null && zone.attachedPath != null) {
105:                 attachedPath = zone.attachedPath;
106:             }
107:             if (!attachedLock && zone.attachedLock) {
108:                 attachedLock = true;
109:             }
110:             if (healAmount <= 0f && zone.healAmount > 0f) {
111:                 healAmount = zone.healAmount;
112:             }
113:             if (pullStrength <= 0f && zone.pullStrength > 0f) {
114:                 pullStrength = zone.pullStrength;
115:                 pullAnchor = zone.anchor;
116:             }
117:             if (wetnessGrant <= 0 && zone.wetnessGrant > 0) {
118:                 wetnessGrant = zone.wetnessGrant;
119:             }
120:             if (!purify && zone.purify) {
121:                 purify = true;
122:             }
123:         }
124:         if (occupied <= 0) {
125:             return FieldInfluence.NONE;
126:         }
127:         return new FieldInfluence(occupied, attachedPath, attachedLock, attachedTracking, healAmount, pullStrength,
128:                 wetnessGrant, purify, pullAnchor);
129:     }
130: 
131:     /** 多块场地的卸力不叠加，取最大的一份。 */
132:     public static float unloadRatio(ServerPlayer player) {
133:         float ratio = 0f;
134:         if (player == null) {
135:             return 0f;
136:         }
137:         for (FieldZone zone : ZONES) {
138:             if (zone.unloadRatio > ratio && covers(zone, player)) {
139:                 ratio = zone.unloadRatio;
140:             }
141:         }
142:         if (ratio < 0f) {
143:             return 0f;
144:         }
145:         return ratio > 0.6f ? 0.6f : ratio;
146:     }
147: 
148:     public static void tick(MinecraftServer server) {
149:         int now = server.getTickCount();
150:         Iterator<FieldZone> iterator = ZONES.iterator();
151:         while (iterator.hasNext()) {
152:             FieldZone zone = iterator.next();
153:             if (zone.closed) {
154:                 retire(zone);
155:                 iterator.remove();
156:                 continue;
157:             }
158:             if (now >= zone.expireTick) {
159:                 endBurst(server, zone);
160:                 retire(zone);
161:                 iterator.remove();
162:                 continue;
163:             }
164:             ServerPlayer owner = server.getPlayerList().getPlayer(zone.ownerId);
165:             if (owner == null || !owner.isAlive() || owner.level().dimension() != zone.dimension) {
166:                 retire(zone);
167:                 iterator.remove();
168:                 continue;
169:             }
170:             ServerLevel level = server.getLevel(zone.dimension);
171:             if (level == null) {
172:                 retire(zone);
173:                 iterator.remove();
174:                 continue;
175:             }
176:             if (now >= zone.nextUpkeepTick) {
177:                 if (!MagicPool.trySpend(owner, zone.upkeepCost, false)) {
178:                     retire(zone);
179:                     iterator.remove();
180:                     continue;
181:                 }
182:                 zone.nextUpkeepTick = now + zone.upkeepPeriod;
183:             }
184:             if ((now & 15) == 0) {
185:                 paint(level, zone);
186:             }
187:         }
188:         if (now % 10 == 0) {
189:             pulse(server);
190:         }
191:     }
192: 
193:     private static void paint(ServerLevel level, FieldZone zone) {
194:         double radius = zone.radius;
195:         for (int step = 0; step < 8; step++) {
196:             double angle = step * Math.PI / 4.0;
197:             double pointX = zone.anchor.x + Math.cos(angle) * radius;
198:             double pointZ = zone.anchor.z + Math.sin(angle) * radius;
199:             level.sendParticles(ParticleTypes.ENCHANT, pointX, zone.anchor.y + 0.2, pointZ, 1, 0.0, 0.05, 0.0, 0.0);
200:         }
201:     }
202: 
203:     /** 压强、侵蚀、灭火各按一份生效，不按场地份数乘，也不把多块场地的数值加在一起。 */
204:     private static void pulse(MinecraftServer server) {
205:         ArrayList<BodyPulse> pending = new ArrayList<>();
206:         for (FieldZone zone : ZONES) {
207:             if (zone.closed || !hasPulse(zone)) {
208:                 continue;
209:             }
210:             ServerLevel level = server.getLevel(zone.dimension);
211:             ServerPlayer owner = server.getPlayerList().getPlayer(zone.ownerId);
212:             if (level == null || owner == null) {
213:                 continue;
214:             }
215:             AABB box = new AABB(zone.anchor, zone.anchor).inflate(zone.radius);
216:             for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
217:                 if (living.getUUID().equals(zone.ownerId) || living.isSpectator()) {
218:                     continue;
219:                 }
220:                 if (!coversBody(zone.anchor, zone.radius, living.getBoundingBox())) {
221:                     continue;
222:                 }
223:                 BodyPulse current = bodyPulse(pending, living);
224:                 if (current.owner == null) {
225:                     current.owner = owner;
226:                 }
227:                 if (current.pressure <= 0f && zone.pressurePulse > 0f) {
228:                     current.pressure = zone.pressurePulse;
229:                 }
230:                 if (current.erosion <= 0f && zone.erosionPulse > 0f) {
231:                     current.erosion = zone.erosionPulse;
232:                 }
233:                 if (!current.purify && zone.purify) {
234:                     current.purify = true;
235:                 }
236:                 if (current.wetnessGrant <= 0 && zone.wetnessGrant > 0) {
237:                     current.wetnessGrant = zone.wetnessGrant;
238:                 }
239:                 if (current.pullStrength <= 0f && zone.pullStrength > 0f) {
240:                     current.pullStrength = zone.pullStrength;
241:                     current.pullAnchor = zone.anchor;
242:                 }
243:                 if (current.slowTicks <= 0 && zone.slowTicks > 0) {
244:                     current.slowTicks = zone.slowTicks;
245:                 }
246:             }
247:         }
248:         for (BodyPulse current : pending) {
249:             if (current.owner == null) {
250:                 continue;
251:             }
252:             if (current.purify) {
253:                 current.living.clearFire();
254:             }
255:             if (current.pressure > 0f) {
256:                 TechniqueHits.hurt(current.living, current.owner, current.pressure);
257:             }
258:             if (current.erosion > 0f) {
259:                 TechniqueHits.hurt(current.living, current.owner, current.erosion);
260:             }
261:             if (current.wetnessGrant > 0) {
262:                 WetnessState.grant(current.living, current.wetnessGrant);
263:             }
264:             if (current.slowTicks > 0) {
265:                 current.living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, current.slowTicks, 1));
266:             }
267:             if (current.pullStrength > 0f && current.pullAnchor != null) {
268:                 Vec3 delta = current.pullAnchor.subtract(current.living.position());
269:                 if (delta.lengthSqr() > 0.04) {
270:                     Vec3 pull = delta.normalize().scale(current.pullStrength);
271:                     current.living.setDeltaMovement(current.living.getDeltaMovement().add(pull.x, pull.y * 0.2, pull.z));
272:                     current.living.hurtMarked = true;
273:                 }
274:             }
275:         }
276:     }
277: 
278:     private static boolean hasPulse(FieldZone zone) {
279:         return zone.pressurePulse > 0f || zone.erosionPulse > 0f || zone.purify || zone.wetnessGrant > 0
280:                 || zone.pullStrength > 0f || zone.slowTicks > 0;
281:     }
282: 
283:     /** 只有正常到期才收招。死亡、下线和维持失败只拆场地。 */
284:     private static void retire(FieldZone zone) {
285:         zone.closed = true;
286:         if (zone.contact != null) {
287:             zone.contact.close();
288:             zone.contact = null;
289:         }
290:     }
291: 
292:     private static void endBurst(MinecraftServer server, FieldZone zone) {
293:         if (zone.endBurstDamage <= 0f) {
294:             return;
295:         }
296:         ServerLevel level = server.getLevel(zone.dimension);
297:         ServerPlayer owner = server.getPlayerList().getPlayer(zone.ownerId);
298:         if (level == null || owner == null) {
299:             return;
300:         }
301:         AABB box = new AABB(zone.anchor, zone.anchor).inflate(Math.max(1.0, zone.radius * 0.45));
302:         for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
303:             if (living.getUUID().equals(zone.ownerId) || living.isSpectator()) {
304:                 continue;
305:             }
306:             TechniqueHits.hurt(living, owner, zone.endBurstDamage);
307:         }
308:     }
309: 
310:     private static BodyPulse bodyPulse(ArrayList<BodyPulse> pending, LivingEntity living) {
311:         for (BodyPulse current : pending) {
312:             if (current.living == living) {
313:                 return current;
314:             }
315:         }
316:         BodyPulse created = new BodyPulse(living);
317:         pending.add(created);
318:         return created;
319:     }
320: 
321:     private static final class BodyPulse {
322:         final LivingEntity living;
323:         ServerPlayer owner;
324:         float pressure;
325:         float erosion;
326:         boolean purify;
327:         int wetnessGrant;
328:         float pullStrength;
329:         Vec3 pullAnchor;
330:         int slowTicks;
331: 
332:         BodyPulse(LivingEntity living) {
333:             this.living = living;
334:         }
335:     }
336: 
337:     static boolean covers(FieldZone zone, ServerPlayer player) {
338:         if (zone.closed || player.level().dimension() != zone.dimension) {
339:             return false;
340:         }
341:         return coversBody(zone.anchor, zone.radius, player.getBoundingBox());
342:     }
343: 
344:     private static boolean coversBody(Vec3 anchor, double radius, AABB body) {
345:         double closestX = clampTo(anchor.x, body.minX, body.maxX);
346:         double closestY = clampTo(anchor.y, body.minY, body.maxY);
347:         double closestZ = clampTo(anchor.z, body.minZ, body.maxZ);
348:         double deltaX = anchor.x - closestX;
349:         double deltaY = anchor.y - closestY;
350:         double deltaZ = anchor.z - closestZ;
351:         return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ <= radius * radius;
352:     }
353: 
354:     private static double clampTo(double amount, double lower, double upper) {
355:         if (amount < lower) {
356:             return lower;
357:         }
358:         if (amount > upper) {
359:             return upper;
360:         }
361:         return amount;
362:     }
363: 
364:     private static final class FieldTouch implements ContactCarrier {
365:         private final FieldZone zone;
366: 
367:         private FieldTouch(FieldZone zone) {
368:             this.zone = zone;
369:         }
370: 
371:         @Override
372:         public java.util.UUID hostId() {
373:             return zone.hostId;
374:         }
375: 
376:         @Override
377:         public int revision() {
378:             return 1;
379:         }
380: 
381:         @Override
382:         public java.util.UUID ownerId() {
383:             return zone.ownerId;
384:         }
385: 
386:         @Override
387:         public boolean active() {
388:             return !zone.closed && zone.serverLevel != null;
389:         }
390: 
391:         @Override
392:         public boolean touches(LivingEntity target) {
393:             return target != null && coversBody(zone.anchor, zone.radius, target.getBoundingBox());
394:         }
395: 
396:         @Override
397:         public ServerLevel hostLevel() {
398:             return zone.serverLevel;
399:         }
400: 
401:         @Override
402:         public AABB queryBox() {
403:             return new AABB(zone.anchor, zone.anchor).inflate(zone.radius + 1.0);
404:         }
405:     }
406: }
407: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/barrier/BarrierDefinition.java">
1: package com.bslx.XueXiMod.shu.technique.release.barrier;
2: 
3: /** 固定薄墙的声明。水幕用它，不再走球形场地。 */
4: public final class BarrierDefinition {
5:     private final double width;
6:     private final double height;
7:     private final double thickness;
8:     private final double forward;
9:     private final int lifetimeTicks;
10:     private final int spawnCost;
11:     private final int upkeepCost;
12:     private final int upkeepPeriod;
13:     private final float attenuation;
14:     private final boolean deflectsProjectiles;
15: 
16:     public BarrierDefinition(double width, double height, double thickness, double forward,
17:                              int lifetimeTicks, int spawnCost, int upkeepCost, int upkeepPeriod, float attenuation) {
18:         this(width, height, thickness, forward, lifetimeTicks, spawnCost, upkeepCost, upkeepPeriod, attenuation, false);
19:     }
20: 
21:     public BarrierDefinition(double width, double height, double thickness, double forward,
22:                              int lifetimeTicks, int spawnCost, int upkeepCost, int upkeepPeriod, float attenuation,
23:                              boolean deflectsProjectiles) {
24:         this.width = width;
25:         this.height = height;
26:         this.thickness = thickness;
27:         this.forward = forward;
28:         this.lifetimeTicks = lifetimeTicks;
29:         this.spawnCost = spawnCost;
30:         this.upkeepCost = upkeepCost;
31:         this.upkeepPeriod = upkeepPeriod;
32:         this.attenuation = attenuation;
33:         this.deflectsProjectiles = deflectsProjectiles;
34:     }
35: 
36:     /** 水幕显式承接来袭弹。其它薄墙构造保持关闭。 */
37:     public static BarrierDefinition curtain() {
38:         return new BarrierDefinition(3.2, 2.8, 0.25, 2.0, 80, 8, 2, 20, 0.5f, true);
39:     }
40: 
41:     public double width() {
42:         return width;
43:     }
44: 
45:     public double height() {
46:         return height;
47:     }
48: 
49:     public double thickness() {
50:         return thickness;
51:     }
52: 
53:     public double forward() {
54:         return forward;
55:     }
56: 
57:     public int lifetimeTicks() {
58:         return lifetimeTicks;
59:     }
60: 
61:     public int spawnCost() {
62:         return spawnCost;
63:     }
64: 
65:     public int upkeepCost() {
66:         return upkeepCost;
67:     }
68: 
69:     public int upkeepPeriod() {
70:         return upkeepPeriod;
71:     }
72: 
73:     public float attenuation() {
74:         return attenuation;
75:     }
76: 
77:     public boolean deflectsProjectiles() {
78:         return deflectsProjectiles;
79:     }
80: }
81: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/barrier/BarrierService.java">
1: package com.bslx.XueXiMod.shu.technique.release.barrier;
2: 
3: import com.bslx.XueXiMod.player.MagicPool;
4: import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
5: import com.bslx.XueXiMod.shu.technique.release.contact.ContactPolicy;
6: import com.bslx.XueXiMod.shu.technique.release.entity.ReleaseEntities;
7: import net.minecraft.server.level.ServerLevel;
8: import net.minecraft.server.level.ServerPlayer;
9: import net.minecraft.world.entity.Entity;
10: import net.minecraft.world.entity.LivingEntity;
11: import net.minecraft.world.level.Level;
12: import net.minecraft.world.phys.AABB;
13: import net.minecraft.world.phys.Vec3;
14: 
15: import java.util.ArrayList;
16: import java.util.List;
17: 
18: /** 同一波次先预检再一起放墙。任何一面放不下，这一波不移动、不扣生成费。 */
19: public final class BarrierService {
20:     private static final ArrayList<BarrierBody> ACTIVE = new ArrayList<>();
21:     private static final java.util.HashSet<String> PLACED_THIS_TICK = new java.util.HashSet<>();
22:     private static int placedTick = Integer.MIN_VALUE;
23: 
24:     private BarrierService() {
25:     }
26: 
27:     public static boolean tryPlace(ServerPlayer player, BarrierDefinition definition, int copies, EnchantmentSnapshot snapshot) {
28:         return releasePlace(player, definition, copies, snapshot, null, null, false, null);
29:     }
30: 
31:     /** 生成费已由父会话预留。墙放在接触点，朝向用冻结方向。 */
32:     public static boolean tryPlaceReserved(ServerPlayer player, BarrierDefinition definition, int copies,
33:                                            EnchantmentSnapshot snapshot, Vec3 feet, Vec3 look, String claimSuffix) {
34:         return releasePlace(player, definition, copies, snapshot, feet, look, true, claimSuffix);
35:     }
36: 
37:     private static boolean releasePlace(ServerPlayer player, BarrierDefinition definition, int copies,
38:                                         EnchantmentSnapshot snapshot, Vec3 feet, Vec3 look, boolean reserved, String claimSuffix) {
39:         if (player == null || definition == null || !(player.level() instanceof ServerLevel level)) {
40:             return false;
41:         }
42:         int count = Math.max(1, copies);
43:         int nowTick = level.getServer().getTickCount();
44:         if (placedTick != nowTick) {
45:             PLACED_THIS_TICK.clear();
46:             placedTick = nowTick;
47:         }
48:         String claim = player.getUUID() + ":" + definition.width() + ":" + definition.forward();
49:         if (claimSuffix != null) {
50:             claim = claim + ":" + claimSuffix;
51:         }
52:         if (PLACED_THIS_TICK.contains(claim)) {
53:             return true;
54:         }
55:         AxisAlignedWall wall = feet == null || look == null
56:                 ? place(player, definition)
57:                 : placeAt(feet, look, player.getYRot(), definition);
58:         List<Shift> shifts = new ArrayList<>();
59:         if (!planShifts(level, wall, player, shifts)) {
60:             player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c水墙里没有安全的让出位置，这次没有释放"));
61:             return false;
62:         }
63:         int bill = definition.spawnCost() * count;
64:         if (!reserved && !MagicPool.trySpend(player, bill)) {
65:             return false;
66:         }
67:         if (!reserved && bill > 0) {
68:             MagicPool.showBalance(player);
69:         }
70:         int now = level.getServer().getTickCount();
71:         List<BarrierBody> spawned = new ArrayList<>();
72:         for (int copy = 0; copy < count; copy++) {
73:             BarrierBody body = ReleaseEntities.BARRIER.get().create(level);
74:             if (body == null) {
75:                 rollback(spawned);
76:                 if (!reserved) {
77:                     MagicPool.restore(player, bill);
78:                 }
79:                 return false;
80:             }
81:             body.configure(player.getUUID(), wall, now + definition.lifetimeTicks(),
82:                     now + definition.upkeepPeriod(), definition.upkeepCost(), snapshot, definition.deflectsProjectiles());
83:             if (!level.addFreshEntity(body)) {
84:                 rollback(spawned);
85:                 if (!reserved) {
86:                     MagicPool.restore(player, bill);
87:                 }
88:                 return false;
89:             }
90:             body.openDeflection();
91:             spawned.add(body);
92:             ACTIVE.add(body);
93:         }
94:         for (Shift shift : shifts) {
95:             Vec3 delta = shift.destination.subtract(shift.root.position());
96:             shift.root.setPos(shift.destination.x, shift.destination.y, shift.destination.z);
97:             nudgePassengers(shift.root, delta);
98:         }
99:         PLACED_THIS_TICK.add(claim);
100:         return true;
101:     }
102: 
103:     public static void closeOwned(ServerPlayer player) {
104:         if (player == null) {
105:             return;
106:         }
107:         for (int index = ACTIVE.size() - 1; index >= 0; index--) {
108:             BarrierBody body = ACTIVE.get(index);
109:             if (player.getUUID().equals(body.ownerId())) {
110:                 body.discard();
111:             }
112:         }
113:     }
114: 
115:     public static void forget(BarrierBody body) {
116:         ACTIVE.remove(body);
117:     }
118: 
119:     public static List<BarrierBody> active() {
120:         return ACTIVE;
121:     }
122: 
123:     public static AxisAlignedWall place(ServerPlayer player, BarrierDefinition definition) {
124:         return placeAt(player.position(), player.getLookAngle(), player.getYRot(), definition);
125:     }
126: 
127:     public static AxisAlignedWall placeAt(Vec3 feet, Vec3 look, float bodyYaw, BarrierDefinition definition) {
128:         double flatReach = Math.sqrt(look.x * look.x + look.z * look.z);
129:         double faceX;
130:         double faceZ;
131:         if (flatReach < 0.2) {
132:             double radians = Math.toRadians(bodyYaw);
133:             faceX = -Math.sin(radians);
134:             faceZ = Math.cos(radians);
135:         } else {
136:             faceX = look.x / flatReach;
137:             faceZ = look.z / flatReach;
138:         }
139:         boolean thinX = Math.abs(faceX) >= Math.abs(faceZ);
140:         if (thinX) {
141:             faceX = Math.signum(faceX == 0.0 ? 1.0 : faceX);
142:             faceZ = 0.0;
143:         } else {
144:             faceZ = Math.signum(faceZ == 0.0 ? 1.0 : faceZ);
145:             faceX = 0.0;
146:         }
147:         double centerX = feet.x + faceX * definition.forward();
148:         double centerZ = feet.z + faceZ * definition.forward();
149:         double bottom = feet.y;
150:         double halfWidth = definition.width() * 0.5;
151:         double halfThick = definition.thickness() * 0.5;
152:         AABB box = thinX
153:                 ? new AABB(centerX - halfThick, bottom, centerZ - halfWidth,
154:                 centerX + halfThick, bottom + definition.height(), centerZ + halfWidth)
155:                 : new AABB(centerX - halfWidth, bottom, centerZ - halfThick,
156:                 centerX + halfWidth, bottom + definition.height(), centerZ + halfThick);
157:         return new AxisAlignedWall(box);
158:     }
159: 
160:     private static boolean planShifts(ServerLevel level, AxisAlignedWall wall, ServerPlayer caster, List<Shift> shifts) {
161:         AABB area = wall.box().inflate(1.0);
162:         for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, Entity::isAlive)) {
163:             Entity root = living.getRootVehicle();
164:             if (!root.getBoundingBox().intersects(wall.box())) {
165:                 continue;
166:             }
167:             if (ContactPolicy.mayBodyPass(caster.getUUID(), root)) {
168:                 continue;
169:             }
170:             if (alreadyShifted(shifts, root)) {
171:                 continue;
172:             }
173:             Vec3 destination = escape(level, wall.box(), root);
174:             if (destination == null) {
175:                 return false;
176:             }
177:             shifts.add(new Shift(root, destination));
178:         }
179:         return true;
180:     }
181: 
182:     private static boolean alreadyShifted(List<Shift> shifts, Entity root) {
183:         for (Shift shift : shifts) {
184:             if (shift.root == root) {
185:                 return true;
186:             }
187:         }
188:         return false;
189:     }
190: 
191:     private static Vec3 escape(ServerLevel level, AABB wall, Entity root) {
192:         AABB body = root.getBoundingBox();
193:         double[] offsets = new double[] {
194:                 wall.maxX - body.minX + 0.05, -(body.maxX - wall.minX + 0.05),
195:                 wall.maxZ - body.minZ + 0.05, -(body.maxZ - wall.minZ + 0.05),
196:                 wall.maxY - body.minY + 0.05, -(body.maxY - wall.minY + 0.05)
197:         };
198:         int[] axis = new int[] {0, 0, 2, 2, 1, 1};
199:         int best = -1;
200:         double bestDistance = Double.MAX_VALUE;
201:         for (int index = 0; index < offsets.length; index++) {
202:             double distance = Math.abs(offsets[index]);
203:             if (distance >= bestDistance) {
204:                 continue;
205:             }
206:             Vec3 moved = move(root.position(), axis[index], offsets[index]);
207:             AABB destination = body.move(moved.x - root.getX(), moved.y - root.getY(), moved.z - root.getZ());
208:             if (destination.minY < level.getMinBuildHeight()) {
209:                 continue;
210:             }
211:             if (!level.noCollision(root, destination) || destination.intersects(wall) || hitsActiveWall(destination)) {
212:                 continue;
213:             }
214:             best = index;
215:             bestDistance = distance;
216:         }
217:         if (best < 0) {
218:             return null;
219:         }
220:         return move(root.position(), axis[best], offsets[best]);
221:     }
222: 
223:     private static boolean hitsActiveWall(AABB destination) {
224:         for (BarrierBody body : ACTIVE) {
225:             if (body.active() && destination.intersects(body.wall().box())) {
226:                 return true;
227:             }
228:         }
229:         return false;
230:     }
231: 
232:     private static Vec3 move(Vec3 origin, int axis, double offset) {
233:         if (axis == 0) {
234:             return origin.add(offset, 0.0, 0.0);
235:         }
236:         if (axis == 1) {
237:             return origin.add(0.0, offset, 0.0);
238:         }
239:         return origin.add(0.0, 0.0, offset);
240:     }
241: 
242:     private static void nudgePassengers(Entity root, Vec3 delta) {
243:         for (Entity passenger : root.getPassengers()) {
244:             passenger.setPos(passenger.getX() + delta.x, passenger.getY() + delta.y, passenger.getZ() + delta.z);
245:             nudgePassengers(passenger, delta);
246:         }
247:     }
248: 
249:     private static void rollback(List<BarrierBody> spawned) {
250:         for (BarrierBody body : spawned) {
251:             body.discard();
252:         }
253:     }
254: 
255:     private static final class Shift {
256:         final Entity root;
257:         final Vec3 destination;
258: 
259:         Shift(Entity root, Vec3 destination) {
260:             this.root = root;
261:             this.destination = destination;
262:         }
263:     }
264: }
265: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/barrier/AxisAlignedWall.java">
1: package com.bslx.XueXiMod.shu.technique.release.barrier;
2: 
3: import net.minecraft.world.entity.LivingEntity;
4: import net.minecraft.world.phys.AABB;
5: import net.minecraft.world.phys.Vec3;
6: 
7: /** 吸附世界四轴的薄墙。渲染、阻挡和接触都读这一份盒子。 */
8: public final class AxisAlignedWall {
9:     public static final double CROSS_EPSILON = 1.0E-6;
10:     private final AABB box;
11:     private final Vec3 center;
12: 
13:     public AxisAlignedWall(AABB box) {
14:         this.box = box;
15:         this.center = new Vec3((box.minX + box.maxX) * 0.5, (box.minY + box.maxY) * 0.5, (box.minZ + box.maxZ) * 0.5);
16:     }
17: 
18:     public AABB box() {
19:         return box;
20:     }
21: 
22:     public Vec3 center() {
23:         return center;
24:     }
25: 
26:     public boolean touches(LivingEntity target, double slop) {
27:         if (target == null) {
28:             return false;
29:         }
30:         AABB body = target.getBoundingBox();
31:         AABB swept = body;
32:         double shiftX = target.xOld - target.getX();
33:         double shiftY = target.yOld - target.getY();
34:         double shiftZ = target.zOld - target.getZ();
35:         if (shiftX * shiftX + shiftY * shiftY + shiftZ * shiftZ > CROSS_EPSILON) {
36:             swept = body.minmax(body.move(shiftX, shiftY, shiftZ));
37:         }
38:         return swept.inflate(slop).intersects(box) && distance(body, box) <= slop;
39:     }
40: 
41:     /** 运动段在墙内的长度。外表面擦过接近 0，不算穿越。 */
42:     public double interiorLength(Vec3 from, Vec3 finish) {
43:         if (from == null || finish == null) {
44:             return 0.0;
45:         }
46:         double spanX = finish.x - from.x;
47:         double spanY = finish.y - from.y;
48:         double spanZ = finish.z - from.z;
49:         double travel = Math.sqrt(spanX * spanX + spanY * spanY + spanZ * spanZ);
50:         if (travel <= CROSS_EPSILON) {
51:             return 0.0;
52:         }
53:         double[] window = new double[] {0.0, 1.0};
54:         if (!clip(from.x, spanX, box.minX, box.maxX, window)
55:                 || !clip(from.y, spanY, box.minY, box.maxY, window)
56:                 || !clip(from.z, spanZ, box.minZ, box.maxZ, window)) {
57:             return 0.0;
58:         }
59:         return travel * Math.max(0.0, window[1] - window[0]);
60:     }
61: 
62:     public double entryFraction(Vec3 from, Vec3 finish) {
63:         if (interiorLength(from, finish) <= CROSS_EPSILON) {
64:             return Double.MAX_VALUE;
65:         }
66:         double spanX = finish.x - from.x;
67:         double spanY = finish.y - from.y;
68:         double spanZ = finish.z - from.z;
69:         double[] window = new double[] {0.0, 1.0};
70:         if (!clip(from.x, spanX, box.minX, box.maxX, window)
71:                 || !clip(from.y, spanY, box.minY, box.maxY, window)
72:                 || !clip(from.z, spanZ, box.minZ, box.maxZ, window)) {
73:             return Double.MAX_VALUE;
74:         }
75:         return window[0];
76:     }
77: 
78:     private static boolean clip(double origin, double span, double lower, double upper, double[] window) {
79:         if (Math.abs(span) <= CROSS_EPSILON) {
80:             return origin >= lower && origin <= upper;
81:         }
82:         double enter = (lower - origin) / span;
83:         double leave = (upper - origin) / span;
84:         if (enter > leave) {
85:             double swap = enter;
86:             enter = leave;
87:             leave = swap;
88:         }
89:         if (enter > window[0]) {
90:             window[0] = enter;
91:         }
92:         if (leave < window[1]) {
93:             window[1] = leave;
94:         }
95:         return window[1] > window[0];
96:     }
97: 
98:     private static double distance(AABB body, AABB wall) {
99:         double deltaX = gap(body.minX, body.maxX, wall.minX, wall.maxX);
100:         double deltaY = gap(body.minY, body.maxY, wall.minY, wall.maxY);
101:         double deltaZ = gap(body.minZ, body.maxZ, wall.minZ, wall.maxZ);
102:         return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
103:     }
104: 
105:     private static double gap(double bodyLow, double bodyHigh, double wallLow, double wallHigh) {
106:         if (bodyHigh < wallLow) {
107:             return wallLow - bodyHigh;
108:         }
109:         if (wallHigh < bodyLow) {
110:             return bodyLow - wallHigh;
111:         }
112:         return 0.0;
113:     }
114: }
115: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/barrier/BarrierBody.java">
1: package com.bslx.XueXiMod.shu.technique.release.barrier;
2: 
3: import com.bslx.XueXiMod.player.MagicPool;
4: import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
5: import com.bslx.XueXiMod.shu.technique.release.contact.ContactCarrier;
6: import com.bslx.XueXiMod.shu.technique.release.contact.ContactEnchantState;
7: import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHosts;
8: import com.bslx.XueXiMod.shu.technique.release.deflection.hosts.WallDeflectionHost;
9: import net.minecraft.nbt.CompoundTag;
10: import net.minecraft.network.protocol.Packet;
11: import net.minecraft.network.protocol.game.ClientGamePacketListener;
12: import net.minecraft.network.syncher.EntityDataAccessor;
13: import net.minecraft.network.syncher.EntityDataSerializers;
14: import net.minecraft.network.syncher.SynchedEntityData;
15: import net.minecraft.server.level.ServerLevel;
16: import net.minecraft.server.level.ServerPlayer;
17: import net.minecraft.world.entity.Entity;
18: import net.minecraft.world.entity.EntityType;
19: import net.minecraft.world.entity.LivingEntity;
20: import net.minecraft.world.level.Level;
21: import net.minecraft.world.phys.AABB;
22: import net.minecraft.world.phys.Vec3;
23: import net.minecraftforge.network.NetworkHooks;
24: 
25: import java.util.Optional;
26: import java.util.UUID;
27: 
28: /** 固定在施法位置的薄墙实体。主人走动不会带走它。 */
29: public class BarrierBody extends Entity implements ContactCarrier {
30:     private static final EntityDataAccessor<Optional<UUID>> OWNER =
31:             SynchedEntityData.defineId(BarrierBody.class, EntityDataSerializers.OPTIONAL_UUID);
32:     private static final EntityDataAccessor<Integer> THIN_AXIS =
33:             SynchedEntityData.defineId(BarrierBody.class, EntityDataSerializers.INT);
34:     private static final EntityDataAccessor<Float> WALL_WIDTH =
35:             SynchedEntityData.defineId(BarrierBody.class, EntityDataSerializers.FLOAT);
36:     private static final EntityDataAccessor<Float> WALL_HEIGHT =
37:             SynchedEntityData.defineId(BarrierBody.class, EntityDataSerializers.FLOAT);
38:     private static final EntityDataAccessor<Float> WALL_THICK =
39:             SynchedEntityData.defineId(BarrierBody.class, EntityDataSerializers.FLOAT);
40: 
41:     private AxisAlignedWall wall = new AxisAlignedWall(new AABB(0, 0, 0, 1, 1, 1));
42:     private int expireTick;
43:     private int nextUpkeepTick;
44:     private int upkeepCost;
45:     private ContactEnchantState contact;
46:     private int contactRevision = 1;
47:     private boolean deflectIncoming;
48:     private long deflectionGeneration = 1L;
49:     private WallDeflectionHost deflectionHost;
50: 
51:     public BarrierBody(EntityType<? extends BarrierBody> type, Level level) {
52:         super(type, level);
53:         this.noPhysics = false;
54:         this.setInvulnerable(true);
55:         this.setNoGravity(true);
56:     }
57: 
58:     public void configure(UUID ownerId, AxisAlignedWall placed, int expireTick, int nextUpkeepTick, int upkeepCost,
59:                           EnchantmentSnapshot snapshot) {
60:         configure(ownerId, placed, expireTick, nextUpkeepTick, upkeepCost, snapshot, false);
61:     }
62: 
63:     public void configure(UUID ownerId, AxisAlignedWall placed, int expireTick, int nextUpkeepTick, int upkeepCost,
64:                           EnchantmentSnapshot snapshot, boolean deflectIncoming) {
65:         this.wall = placed;
66:         this.expireTick = expireTick;
67:         this.nextUpkeepTick = nextUpkeepTick;
68:         this.upkeepCost = upkeepCost;
69:         this.deflectIncoming = deflectIncoming;
70:         this.entityData.set(OWNER, Optional.ofNullable(ownerId));
71:         Vec3 center = placed.center();
72:         this.setPos(center.x, center.y, center.z);
73:         this.setBoundingBox(placed.box());
74:         writeExtents(placed.box());
75:         if (snapshot != null && this.level() instanceof ServerLevel) {
76:             this.contact = ContactEnchantState.open(this, snapshot);
77:         }
78:     }
79: 
80:     public UUID ownerId() {
81:         return this.entityData.get(OWNER).orElse(null);
82:     }
83: 
84:     public AxisAlignedWall wall() {
85:         return wall;
86:     }
87: 
88:     public float attenuation() {
89:         return BarrierDefinition.curtain().attenuation();
90:     }
91: 
92:     public int expireTick() {
93:         return expireTick;
94:     }
95: 
96:     public boolean deflectsProjectiles() {
97:         return deflectIncoming;
98:     }
99: 
100:     /** 墙已经进世界之后再登记。放不下的那面不要进注册表。 */
101:     public void openDeflection() {
102:         if (!this.deflectIncoming || this.deflectionHost != null || this.level().isClientSide || ownerId() == null) {
103:             return;
104:         }
105:         if (!(this.level() instanceof ServerLevel)) {
106:             return;
107:         }
108:         this.deflectionHost = new WallDeflectionHost(this, this.deflectionGeneration);
109:         DeflectionHosts.register(this.deflectionHost);
110:     }
111: 
112:     public void closeDeflection() {
113:         if (this.deflectionHost == null) {
114:             return;
115:         }
116:         DeflectionHosts.unregister(this.deflectionHost.key());
117:         this.deflectionHost = null;
118:     }
119: 
120:     @Override
121:     public UUID hostId() {
122:         return getUUID();
123:     }
124: 
125:     @Override
126:     public int revision() {
127:         return contactRevision;
128:     }
129: 
130:     @Override
131:     public boolean active() {
132:         return isAlive() && !isRemoved();
133:     }
134: 
135:     @Override
136:     public boolean touches(LivingEntity target) {
137:         return wall.touches(target, ContactEnchantState.SURFACE_SLOP);
138:     }
139: 
140:     @Override
141:     public ServerLevel hostLevel() {
142:         return this.level() instanceof ServerLevel serverLevel ? serverLevel : null;
143:     }
144: 
145:     @Override
146:     public AABB queryBox() {
147:         return wall.box().inflate(2.0);
148:     }
149: 
150:     @Override
151:     protected void defineSynchedData() {
152:         this.entityData.define(OWNER, Optional.empty());
153:         this.entityData.define(THIN_AXIS, 0);
154:         this.entityData.define(WALL_WIDTH, 3.2f);
155:         this.entityData.define(WALL_HEIGHT, 2.8f);
156:         this.entityData.define(WALL_THICK, 0.25f);
157:     }
158: 
159:     @Override
160:     public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
161:         super.onSyncedDataUpdated(accessor);
162:         refreshWall();
163:     }
164: 
165:     @Override
166:     public void tick() {
167:         super.tick();
168:         refreshWall();
169:         if (this.level().isClientSide) {
170:             return;
171:         }
172:         ServerLevel serverLevel = (ServerLevel) this.level();
173:         int now = serverLevel.getServer().getTickCount();
174:         UUID owner = ownerId();
175:         ServerPlayer player = owner == null ? null : serverLevel.getServer().getPlayerList().getPlayer(owner);
176:         if (player == null || !player.isAlive() || player.level() != serverLevel || now >= expireTick) {
177:             discard();
178:             return;
179:         }
180:         if (now >= nextUpkeepTick) {
181:             if (!MagicPool.trySpend(player, upkeepCost, false)) {
182:                 discard();
183:                 return;
184:             }
185:             nextUpkeepTick = now + BarrierDefinition.curtain().upkeepPeriod();
186:         }
187:     }
188: 
189:     @Override
190:     public void remove(RemovalReason reason) {
191:         closeDeflection();
192:         if (contact != null) {
193:             contact.close();
194:             contact = null;
195:         }
196:         BarrierService.forget(this);
197:         super.remove(reason);
198:     }
199: 
200:     @Override
201:     public boolean canBeCollidedWith() {
202:         return true;
203:     }
204: 
205:     @Override
206:     public boolean isPickable() {
207:         return false;
208:     }
209: 
210:     @Override
211:     public boolean isPushable() {
212:         return false;
213:     }
214: 
215:     @Override
216:     public boolean canBeHitByProjectile() {
217:         return false;
218:     }
219: 
220:     @Override
221:     protected void readAdditionalSaveData(CompoundTag tag) {
222:     }
223: 
224:     @Override
225:     protected void addAdditionalSaveData(CompoundTag tag) {
226:     }
227: 
228:     @Override
229:     public Packet<ClientGamePacketListener> getAddEntityPacket() {
230:         return NetworkHooks.getEntitySpawningPacket(this);
231:     }
232: 
233:     private void writeExtents(AABB box) {
234:         boolean thinX = (box.maxX - box.minX) < (box.maxZ - box.minZ);
235:         this.entityData.set(THIN_AXIS, thinX ? 0 : 1);
236:         this.entityData.set(WALL_WIDTH, (float) (thinX ? box.maxZ - box.minZ : box.maxX - box.minX));
237:         this.entityData.set(WALL_HEIGHT, (float) (box.maxY - box.minY));
238:         this.entityData.set(WALL_THICK, (float) (thinX ? box.maxX - box.minX : box.maxZ - box.minZ));
239:     }
240: 
241:     private void refreshWall() {
242:         if (!this.level().isClientSide && wall != null) {
243:             this.setBoundingBox(wall.box());
244:             return;
245:         }
246:         float width = this.entityData.get(WALL_WIDTH);
247:         float height = this.entityData.get(WALL_HEIGHT);
248:         float thick = this.entityData.get(WALL_THICK);
249:         boolean thinX = this.entityData.get(THIN_AXIS) == 0;
250:         double halfWidth = width * 0.5;
251:         double halfHeight = height * 0.5;
252:         double halfThick = thick * 0.5;
253:         AABB rebuilt = thinX
254:                 ? new AABB(getX() - halfThick, getY() - halfHeight, getZ() - halfWidth,
255:                 getX() + halfThick, getY() + halfHeight, getZ() + halfWidth)
256:                 : new AABB(getX() - halfWidth, getY() - halfHeight, getZ() - halfThick,
257:                 getX() + halfWidth, getY() + halfHeight, getZ() + halfThick);
258:         this.wall = new AxisAlignedWall(rebuilt);
259:         this.setBoundingBox(rebuilt);
260:     }
261: }
262: 
</source_file>

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

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseDefinitions.java">
1: package com.bslx.XueXiMod.shu.technique.release;
2: 
3: public final class ReleaseDefinitions {
4:     private ReleaseDefinitions() {}
5: 
6:     public static void registerAll() {
7:         ReleaseRegistry.register(define("movement_dash", ReleaseKind.ACTION, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 10, 2f, 0f, 0, false, false, false, false, ChainRole.ACTION, false, false));
8:         ReleaseRegistry.register(cohortSword("sword_horizontal_slash", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 4, 0f, 0.5f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
9:         ReleaseRegistry.register(cohortSword("sword_vertical_slash", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 4, 0f, 0.5f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
10:         ReleaseRegistry.register(cohortSword("sword_thrust", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 4, 0f, 0.5f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
11:         ReleaseRegistry.register(define("magic_arcane_missile", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, true, 40, 1.5f, 0.1f, 0, true, true, false, false, ChainRole.ATTACK, true, false));
12:         ReleaseRegistry.register(define("test_sustained", ReleaseKind.HIT_VOLUME, ReleaseTag.SUSTAINED, Trajectory.DROP, true, 20, 0.4f, 0.1f, 0, false, true, false, false, ChainRole.ATTACK, true, false));
13:         ReleaseRegistry.register(define("heat_beam", ReleaseKind.HIT_VOLUME, ReleaseTag.SUSTAINED, Trajectory.FOLLOW_LOOK, false, 40, 0f, 1f, 0, true, true, false, false, ChainRole.ATTACK, true, false));
14:         ReleaseRegistry.register(define("universal_lock_target", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, true, false, ChainRole.SEEK_ENTITY, false, false));
15:         ReleaseRegistry.register(define("rule_dual_release", ReleaseKind.RULE, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 1, false, false, false, false, ChainRole.RULE_DUAL, false, false));
16:         ReleaseRegistry.register(define("trajectory_drop", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.DROP, false, 1, 0f, 0f, 0, false, false, false, true, ChainRole.TRAJECTORY_DROP, false, false));
17:         ReleaseRegistry.register(define("trajectory_tracking", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.TRAJECTORY_TRACKING, false, false));
18:         ReleaseRegistry.register(define("trajectory_storm", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.VORTEX, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.TRAJECTORY_DROP, false, false));
19:         ReleaseRegistry.register(define("fire_orb", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, true, 40, 1.2f, 0.4f, 0, true, true, false, false, ChainRole.ATTACK, true, false));
20:         ReleaseRegistry.register(define("fire_burst", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, true, 40, 0.6f, 0.8f, 0, true, true, false, false, ChainRole.ATTACK, true, false));
21:         ReleaseRegistry.register(cohortSword("water_yielding_ribbon", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 32, 0f, 0.55f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
22:         ReleaseRegistry.register(define("water_weeping_arc", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.FUNCTION, true, WeepingSettings.LIFETIME, 0f, 0.35f, 0, true, true, false, false, ChainRole.ATTACK, false, false, false, 1f, 1f, true, true, false, false, 0f, 0, WeepingSettings.WETNESS, false, true, true, WeepingSettings.RETAINED_MANA));
23:         ReleaseRegistry.register(cohortSword("space_cut", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 10, 0f, 0f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
24:         ReleaseRegistry.register(define("rule_cast_anchor", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, true, false, ChainRole.SEEK_SPACE, false, false));
25:         ReleaseRegistry.register(define("rule_triple_cast", ReleaseKind.RULE, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.RULE_TRIPLE, false, false));
26:         ReleaseRegistry.register(define("rule_gather_energy", ReleaseKind.RULE, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.ATTACK, false, false, false, 1f, 2f));
27:         ReleaseRegistry.register(define("trajectory_3d_interleave", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.TRAJECTORY_SCATTER, false, false));
28:         ReleaseRegistry.register(define("parasite_seed", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.DROP, true, 160, 0f, 0.5f, 0, false, true, false, false, ChainRole.ATTACK, true, false));
29:         ReleaseRegistry.register(cohortSword("tide_sever", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 6, 0f, 0.4f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
30:         ReleaseRegistry.register(cohortSword("stone_hammer", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 10, 0f, 1f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
31:         ReleaseRegistry.register(define("metal_needle", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, true, 15, 1.5f, 0.2f, 0, true, true, false, false, ChainRole.ATTACK, true, false));
32:         ReleaseRegistry.register(define("phantom_sword", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, true, 80, 2.5f, 0.5f, 0, true, true, false, false, ChainRole.ATTACK, true, false));
33:         ReleaseRegistry.register(define("sword_come", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 20, 0f, 0.5f, 0, false, false, false, false, ChainRole.ATTACK, false, true, true));
34:         ReleaseRegistry.register(define("sword_return_all", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 40, 0f, 0.5f, 0, false, false, false, false, ChainRole.ATTACK, false, true, true));
35:     }
36: 
37:     /** 命中实体上的剑术斩面、水弧或水带。幻剑和召回不走这里。 */
38:     private static ReleaseDefinition cohortSword(String releaseId, ReleaseKind kind, ReleaseTag tag, Trajectory trajectory,
39:                                                   boolean moving, int lifetimeTicks, float speed, float thickness, int pullAfterA,
40:                                                   boolean acceptsTargetOverride, boolean acceptsPathOverride,
41:                                                   boolean setsTargetForNextDirectional, boolean setsDropForNextDirectional,
42:                                                   ChainRole role, boolean trajectoryMutable, boolean manipulatesSwordEntities) {
43:         return define(releaseId, kind, tag, trajectory, moving, lifetimeTicks, speed, thickness, pullAfterA,
44:                 acceptsTargetOverride, acceptsPathOverride, setsTargetForNextDirectional, setsDropForNextDirectional,
45:                 role, trajectoryMutable, manipulatesSwordEntities, false, 1f, 1f,
46:                 true, true, true, true, 0f, 0, 0, true, false, false, -1);
47:     }
48: 
49:     private static ReleaseDefinition define(String releaseId, ReleaseKind kind, ReleaseTag tag, Trajectory trajectory,
50:                                             boolean moving, int lifetimeTicks, float speed, float thickness, int pullAfterA,
51:                                             boolean acceptsTargetOverride, boolean acceptsPathOverride,
52:                                             boolean setsTargetForNextDirectional, boolean setsDropForNextDirectional,
53:                                             ChainRole role, boolean trajectoryMutable, boolean manipulatesSwordEntities) {
54:         return define(releaseId, kind, tag, trajectory, moving, lifetimeTicks, speed, thickness, pullAfterA,
55:                 acceptsTargetOverride, acceptsPathOverride, setsTargetForNextDirectional, setsDropForNextDirectional,
56:                 role, trajectoryMutable, manipulatesSwordEntities, false, 1f);
57:     }
58: 
59:     private static ReleaseDefinition define(String releaseId, ReleaseKind kind, ReleaseTag tag, Trajectory trajectory,
60:                                             boolean moving, int lifetimeTicks, float speed, float thickness, int pullAfterA,
61:                                             boolean acceptsTargetOverride, boolean acceptsPathOverride,
62:                                             boolean setsTargetForNextDirectional, boolean setsDropForNextDirectional,
63:                                             ChainRole role, boolean trajectoryMutable, boolean manipulatesSwordEntities,
64:                                             boolean refusesMixedChain) {
65:         return define(releaseId, kind, tag, trajectory, moving, lifetimeTicks, speed, thickness, pullAfterA,
66:                 acceptsTargetOverride, acceptsPathOverride, setsTargetForNextDirectional, setsDropForNextDirectional,
67:                 role, trajectoryMutable, manipulatesSwordEntities, refusesMixedChain, 1f, 1f,
68:                 true, true, true, true, 0f, 0, 0, false, false, false, -1);
69:     }
70: 
71:     private static ReleaseDefinition define(String releaseId, ReleaseKind kind, ReleaseTag tag, Trajectory trajectory,
72:                                             boolean moving, int lifetimeTicks, float speed, float thickness, int pullAfterA,
73:                                             boolean acceptsTargetOverride, boolean acceptsPathOverride,
74:                                             boolean setsTargetForNextDirectional, boolean setsDropForNextDirectional,
75:                                             ChainRole role, boolean trajectoryMutable, boolean manipulatesSwordEntities,
76:                                             boolean refusesMixedChain, float energyScale) {
77:         return define(releaseId, kind, tag, trajectory, moving, lifetimeTicks, speed, thickness, pullAfterA,
78:                 acceptsTargetOverride, acceptsPathOverride, setsTargetForNextDirectional, setsDropForNextDirectional,
79:                 role, trajectoryMutable, manipulatesSwordEntities, refusesMixedChain, energyScale, 1f,
80:                 true, true, true, true, 0f, 0, 0, false, false, false, -1);
81:     }
82: 
83:     private static ReleaseDefinition define(String releaseId, ReleaseKind kind, ReleaseTag tag, Trajectory trajectory,
84:                                             boolean moving, int lifetimeTicks, float speed, float thickness, int pullAfterA,
85:                                             boolean acceptsTargetOverride, boolean acceptsPathOverride,
86:                                             boolean setsTargetForNextDirectional, boolean setsDropForNextDirectional,
87:                                             ChainRole role, boolean trajectoryMutable, boolean manipulatesSwordEntities,
88:                                             boolean refusesMixedChain, float energyScale, float chainEnergyScale) {
89:         return define(releaseId, kind, tag, trajectory, moving, lifetimeTicks, speed, thickness, pullAfterA,
90:                 acceptsTargetOverride, acceptsPathOverride, setsTargetForNextDirectional, setsDropForNextDirectional,
91:                 role, trajectoryMutable, manipulatesSwordEntities, refusesMixedChain, energyScale, chainEnergyScale,
92:                 true, true, true, true, 0f, 0, 0, false, false, false, -1);
93:     }
94: 
95:     private static ReleaseDefinition define(String releaseId, ReleaseKind kind, ReleaseTag tag, Trajectory trajectory,
96:                                             boolean moving, int lifetimeTicks, float speed, float thickness, int pullAfterA,
97:                                             boolean acceptsTargetOverride, boolean acceptsPathOverride,
98:                                             boolean setsTargetForNextDirectional, boolean setsDropForNextDirectional,
99:                                             ChainRole role, boolean trajectoryMutable, boolean manipulatesSwordEntities,
100:                                             boolean refusesMixedChain, float energyScale, float chainEnergyScale,
101:                                             boolean scaleLength, boolean scaleWidth, boolean scaleHeight, boolean scaleModel,
102:                                             float heavyPerCopy, int pressureMinimum, int wetnessAdded,
103:                                             boolean cohortVisual, boolean persistentFlow, boolean deflectIncoming,
104:                                             int explicitCost) {
105:         return new ReleaseDefinition() {
106:             @Override
107:             public String id() {
108:                 return releaseId;
109:             }
110: 
111:             @Override
112:             public ReleaseKind kind() {
113:                 return kind;
114:             }
115: 
116:             @Override
117:             public ReleaseTag tag() {
118:                 return tag;
119:             }
120: 
121:             @Override
122:             public Trajectory trajectory() {
123:                 return trajectory;
124:             }
125: 
126:             @Override
127:             public boolean moving() {
128:                 return moving;
129:             }
130: 
131:             @Override
132:             public int lifetimeTicks() {
133:                 return lifetimeTicks;
134:             }
135: 
136:             @Override
137:             public float speed() {
138:                 return speed;
139:             }
140: 
141:             @Override
142:             public float thickness() {
143:                 return thickness;
144:             }
145: 
146:             @Override
147:             public int pullAfterA() {
148:                 return pullAfterA;
149:             }
150: 
151:             @Override
152:             public boolean acceptsTargetOverride() {
153:                 return acceptsTargetOverride;
154:             }
155: 
156:             @Override
157:             public boolean acceptsPathOverride() {
158:                 return acceptsPathOverride;
159:             }
160: 
161:             @Override
162:             public boolean setsTargetForNextDirectional() {
163:                 return setsTargetForNextDirectional;
164:             }
165: 
166:             @Override
167:             public boolean setsDropForNextDirectional() {
168:                 return setsDropForNextDirectional;
169:             }
170: 
171:             @Override
172:             public ChainRole role() {
173:                 return role;
174:             }
175: 
176:             @Override
177:             public boolean trajectoryMutable() {
178:                 return trajectoryMutable;
179:             }
180: 
181:             @Override
182:             public boolean manipulatesSwordEntities() {
183:                 return manipulatesSwordEntities;
184:             }
185: 
186:             @Override
187:             public boolean refusesMixedChain() {
188:                 return refusesMixedChain;
189:             }
190: 
191:             @Override
192:             public float energyScale() {
193:                 return energyScale;
194:             }
195: 
196:             @Override
197:             public float chainEnergyScale() {
198:                 return chainEnergyScale;
199:             }
200: 
201:             @Override
202:             public boolean energyScalesLength() {
203:                 return scaleLength;
204:             }
205: 
206:             @Override
207:             public boolean energyScalesWidth() {
208:                 return scaleWidth;
209:             }
210: 
211:             @Override
212:             public boolean energyScalesHeight() {
213:                 return scaleHeight;
214:             }
215: 
216:             @Override
217:             public boolean energyScalesModel() {
218:                 return scaleModel;
219:             }
220: 
221:             @Override
222:             public float heavyMomentumPerCopy() {
223:                 return heavyPerCopy;
224:             }
225: 
226:             @Override
227:             public int pressureWetnessMinimum() {
228:                 return pressureMinimum;
229:             }
230: 
231:             @Override
232:             public int wetnessGrant() {
233:                 return wetnessAdded;
234:             }
235: 
236:             @Override
237:             public boolean separatesCohortVisuals() {
238:                 return cohortVisual;
239:             }
240: 
241:             @Override
242:             public boolean persistentFlowSettlement() {
243:                 return persistentFlow;
244:             }
245: 
246:             @Override
247:             public boolean deflectsProjectiles() {
248:                 return deflectIncoming;
249:             }
250: 
251:             @Override
252:             public int explicitManaCost() {
253:                 return explicitCost;
254:             }
255:         };
256:     }
257: }
258: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/WaterReleaseDefinition.java">
1: package com.bslx.XueXiMod.shu.technique.release;
2: 
3: import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierDefinition;
4: import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalEffect;
5: import com.bslx.XueXiMod.shu.technique.release.domain.DomainDefinition;
6: import com.bslx.XueXiMod.shu.technique.release.guard.GuardDefinition;
7: 
8: /**
9:  * 水系成品的释放数据。结算不看术式编号，只读这些字段。
10:  */
11: public final class WaterReleaseDefinition implements ReleaseDefinition {
12:     private final String releaseId;
13:     private final Trajectory trajectory;
14:     private final boolean moving;
15:     private final int lifetimeTicks;
16:     private final float speed;
17:     private final float thickness;
18:     private boolean trajectoryMutable;
19:     private boolean acceptInitialAim;
20:     private boolean heldSword;
21:     private boolean hostGroupSubmit;
22:     private boolean arcBand;
23:     private boolean beatCycle;
24:     private boolean cohortVisual;
25:     private float energyScale = 1f;
26:     private int explicitCost = -1;
27:     private float fieldRadius;
28:     private int upkeepCost;
29:     private int fieldLife = 80;
30:     private float pullStrength;
31:     private int wetnessGrant;
32:     private float heavyPerCopy;
33:     private int pressureMinimum;
34:     private boolean purify;
35:     private boolean blocksMovement;
36:     private float pressurePulse;
37:     private float knockbackStrength;
38:     private float liftStrength;
39:     private float unloadRatio;
40:     private int slowTicks;
41:     private float endBurstDamage;
42:     private int bodyVisual = 5;
43:     private String curveFunction = "";
44:     private ReleaseShape shape = ReleaseShape.BOX;
45:     private float spanScale = 1f;
46:     private float lengthScale = 1f;
47:     private GuardDefinition guard;
48:     private BarrierDefinition barrier;
49:     private DomainDefinition domain;
50:     private ConditionalEffect conditionalEffect;
51:     private boolean deflectsIncoming;
52: 
53:     private WaterReleaseDefinition(String releaseId, Trajectory trajectory, boolean moving, int lifetimeTicks,
54:                                    float speed, float thickness, boolean trajectoryMutable) {
55:         this.releaseId = releaseId;
56:         this.trajectory = trajectory;
57:         this.moving = moving;
58:         this.lifetimeTicks = lifetimeTicks;
59:         this.speed = speed;
60:         this.thickness = thickness;
61:         this.trajectoryMutable = trajectoryMutable;
62:         this.acceptInitialAim = trajectoryMutable;
63:         this.heldSword = false;
64:     }
65: 
66:     /** 弹道锁定，仍接受出手瞬间的目标方向，并且必须手持剑。不改其他水系射击的默认值。 */
67:     public WaterReleaseDefinition swordForm() {
68:         this.trajectoryMutable = false;
69:         this.acceptInitialAim = true;
70:         this.heldSword = true;
71:         this.hostGroupSubmit = true;
72:         this.arcBand = true;
73:         this.beatCycle = true;
74:         this.cohortVisual = true;
75:         this.deflectsIncoming = true;
76:         return this;
77:     }
78: 
79:     public static WaterReleaseDefinition shot(String releaseId, Trajectory trajectory, int lifetimeTicks,
80:                                               float speed, float thickness) {
81:         return new WaterReleaseDefinition(releaseId, trajectory, true, lifetimeTicks, speed, thickness, true);
82:     }
83: 
84:     public static WaterReleaseDefinition zone(String releaseId, float radius, int lifetimeTicks, int manaCost, int upkeepCost) {
85:         WaterReleaseDefinition definition = new WaterReleaseDefinition(releaseId, Trajectory.STRAIGHT, false, lifetimeTicks, 0f, radius, false);
86:         definition.fieldRadius = radius;
87:         definition.fieldLife = lifetimeTicks;
88:         definition.explicitCost = manaCost;
89:         definition.upkeepCost = upkeepCost;
90:         definition.bodyVisual = 1;
91:         return definition;
92:     }
93: 
94:     public static WaterReleaseDefinition barrier(String releaseId, BarrierDefinition barrier) {
95:         WaterReleaseDefinition definition = new WaterReleaseDefinition(releaseId, Trajectory.STRAIGHT, false,
96:                 barrier.lifetimeTicks(), 0f, 0f, false);
97:         definition.explicitCost = 0;
98:         definition.barrier = barrier;
99:         definition.fieldLife = barrier.lifetimeTicks();
100:         definition.upkeepCost = barrier.upkeepCost();
101:         return definition;
102:     }
103: 
104:     public static WaterReleaseDefinition domain(String releaseId, DomainDefinition domain) {
105:         WaterReleaseDefinition definition = new WaterReleaseDefinition(releaseId, Trajectory.STRAIGHT, false,
106:                 domain.lifetimeTicks(), 0f, 0f, false);
107:         definition.explicitCost = 0;
108:         definition.domain = domain;
109:         definition.fieldLife = domain.lifetimeTicks();
110:         definition.upkeepCost = domain.upkeepCost();
111:         return definition;
112:     }
113: 
114:     public static WaterReleaseDefinition bodyGuard(String releaseId, GuardDefinition guard) {
115:         WaterReleaseDefinition definition = new WaterReleaseDefinition(releaseId, Trajectory.STRAIGHT, false,
116:                 guard.lifetimeTicks(), 0f, 0f, false);
117:         definition.explicitCost = 0;
118:         definition.guard = guard;
119:         return definition;
120:     }
121: 
122:     /** 零伤条件释放。显式魔力只在非负时写下，不顺手变成领域、屏障或护体。 */
123:     public static WaterReleaseDefinition conditional(String releaseId, int manaCost, ConditionalEffect effect) {
124:         WaterReleaseDefinition definition = new WaterReleaseDefinition(releaseId, Trajectory.STRAIGHT, false,
125:                 1, 0f, 0f, false);
126:         if (manaCost >= 0) {
127:             definition.explicitCost = manaCost;
128:         }
129:         definition.conditionalEffect = effect;
130:         return definition;
131:     }
132: 
133:     public WaterReleaseDefinition energy(float scale) {
134:         this.energyScale = scale;
135:         return this;
136:     }
137: 
138:     public WaterReleaseDefinition wetness(int amount) {
139:         this.wetnessGrant = amount;
140:         return this;
141:     }
142: 
143:     /** 每一份有效攻击预留的重水。非有限或负数留下原值。0 表示不走重水。 */
144:     public WaterReleaseDefinition heavyMomentum(float perCopy) {
145:         if (Float.isFinite(perCopy) && perCopy >= 0f) {
146:             this.heavyPerCopy = perCopy;
147:         }
148:         return this;
149:     }
150: 
151:     /** 重压的潮湿门槛，只接受 0 到 100。越界留下原值。0 表示不挂重压。 */
152:     public WaterReleaseDefinition pressureWetness(int minimum) {
153:         if (minimum >= 0 && minimum <= 100) {
154:             this.pressureMinimum = minimum;
155:         }
156:         return this;
157:     }
158: 
159:     public WaterReleaseDefinition pull(float strength) {
160:         this.pullStrength = strength;
161:         return this;
162:     }
163: 
164:     public WaterReleaseDefinition pressure(float amount) {
165:         this.pressurePulse = amount;
166:         return this;
167:     }
168: 
169:     public WaterReleaseDefinition knock(float strength) {
170:         this.knockbackStrength = strength;
171:         return this;
172:     }
173: 
174:     public WaterReleaseDefinition lift(float strength) {
175:         this.liftStrength = strength;
176:         return this;
177:     }
178: 
179:     public WaterReleaseDefinition unload(float ratio) {
180:         this.unloadRatio = ratio;
181:         return this;
182:     }
183: 
184:     public WaterReleaseDefinition slow(int ticks) {
185:         this.slowTicks = ticks;
186:         return this;
187:     }
188: 
189:     public WaterReleaseDefinition burst(float damage) {
190:         this.endBurstDamage = damage;
191:         return this;
192:     }
193: 
194:     public WaterReleaseDefinition purify() {
195:         this.purify = true;
196:         return this;
197:     }
198: 
199:     public WaterReleaseDefinition wall() {
200:         this.blocksMovement = true;
201:         return this;
202:     }
203: 
204:     public WaterReleaseDefinition visual(int visual) {
205:         this.bodyVisual = visual;
206:         return this;
207:     }
208: 
209:     public WaterReleaseDefinition curve(String functionId) {
210:         this.curveFunction = functionId == null ? "" : functionId;
211:         return this;
212:     }
213: 
214:     public WaterReleaseDefinition shape(ReleaseShape shape) {
215:         this.shape = shape;
216:         return this;
217:     }
218: 
219:     public WaterReleaseDefinition span(float scale) {
220:         this.spanScale = scale;
221:         return this;
222:     }
223: 
224:     public WaterReleaseDefinition along(float scale) {
225:         this.lengthScale = scale;
226:         return this;
227:     }
228: 
229:     @Override
230:     public String id() {
231:         return releaseId;
232:     }
233: 
234:     @Override
235:     public ReleaseKind kind() {
236:         return ReleaseKind.HIT_VOLUME;
237:     }
238: 
239:     @Override
240:     public ReleaseTag tag() {
241:         return ReleaseTag.TRIGGER;
242:     }
243: 
244:     @Override
245:     public Trajectory trajectory() {
246:         return trajectory;
247:     }
248: 
249:     @Override
250:     public boolean moving() {
251:         return moving;
252:     }
253: 
254:     @Override
255:     public int lifetimeTicks() {
256:         return lifetimeTicks;
257:     }
258: 
259:     @Override
260:     public float speed() {
261:         return speed;
262:     }
263: 
264:     @Override
265:     public float thickness() {
266:         return thickness;
267:     }
268: 
269:     @Override
270:     public boolean acceptsTargetOverride() {
271:         return trajectoryMutable;
272:     }
273: 
274:     @Override
275:     public boolean acceptsInitialAim() {
276:         return acceptInitialAim;
277:     }
278: 
279:     @Override
280:     public boolean requiresHeldSword() {
281:         return heldSword;
282:     }
283: 
284:     @Override
285:     public boolean submitsHostGroup() {
286:         return hostGroupSubmit;
287:     }
288: 
289:     @Override
290:     public boolean usesArcBand() {
291:         return arcBand;
292:     }
293: 
294:     @Override
295:     public boolean tracksBeatCycle() {
296:         return beatCycle;
297:     }
298: 
299:     @Override
300:     public boolean deflectsProjectiles() {
301:         if (deflectsIncoming) {
302:             return true;
303:         }
304:         if (barrier != null && barrier.deflectsProjectiles()) {
305:             return true;
306:         }
307:         return guard != null && guard.deflectsProjectiles();
308:     }
309: 
310:     @Override
311:     public boolean separatesCohortVisuals() {
312:         return cohortVisual;
313:     }
314: 
315:     @Override
316:     public boolean acceptsPathOverride() {
317:         return trajectoryMutable;
318:     }
319: 
320:     @Override
321:     public ChainRole role() {
322:         return ChainRole.ATTACK;
323:     }
324: 
325:     @Override
326:     public boolean trajectoryMutable() {
327:         return trajectoryMutable;
328:     }
329: 
330:     @Override
331:     public float energyScale() {
332:         return energyScale;
333:     }
334: 
335:     @Override
336:     public int explicitManaCost() {
337:         return explicitCost;
338:     }
339: 
340:     @Override
341:     public float fieldRadius() {
342:         return fieldRadius;
343:     }
344: 
345:     @Override
346:     public GuardDefinition guard() {
347:         return guard;
348:     }
349: 
350:     @Override
351:     public BarrierDefinition barrier() {
352:         return barrier;
353:     }
354: 
355:     @Override
356:     public DomainDefinition domain() {
357:         return domain;
358:     }
359: 
360:     @Override
361:     public ConditionalEffect conditionalEffect() {
362:         return conditionalEffect;
363:     }
364: 
365:     @Override
366:     public int fieldUpkeepCost() {
367:         return upkeepCost;
368:     }
369: 
370:     @Override
371:     public int fieldLifetimeTicks() {
372:         return fieldLife;
373:     }
374: 
375:     @Override
376:     public float pullStrength() {
377:         return pullStrength;
378:     }
379: 
380:     @Override
381:     public int wetnessGrant() {
382:         return wetnessGrant;
383:     }
384: 
385:     @Override
386:     public float heavyMomentumPerCopy() {
387:         return heavyPerCopy;
388:     }
389: 
390:     @Override
391:     public int pressureWetnessMinimum() {
392:         return pressureMinimum;
393:     }
394: 
395:     @Override
396:     public boolean purifyOnHit() {
397:         return purify;
398:     }
399: 
400:     @Override
401:     public boolean blocksMovement() {
402:         return blocksMovement;
403:     }
404: 
405:     @Override
406:     public float pressurePulse() {
407:         return pressurePulse;
408:     }
409: 
410:     @Override
411:     public float knockbackStrength() {
412:         return knockbackStrength;
413:     }
414: 
415:     @Override
416:     public float liftStrength() {
417:         return liftStrength;
418:     }
419: 
420:     @Override
421:     public float unloadRatio() {
422:         return unloadRatio;
423:     }
424: 
425:     @Override
426:     public int slowTicks() {
427:         return slowTicks;
428:     }
429: 
430:     @Override
431:     public float endBurstDamage() {
432:         return endBurstDamage;
433:     }
434: 
435:     @Override
436:     public int bodyVisual() {
437:         return bodyVisual;
438:     }
439: 
440:     @Override
441:     public String functionId() {
442:         return curveFunction;
443:     }
444: 
445:     @Override
446:     public ReleaseShape shape() {
447:         return shape;
448:     }
449: 
450:     @Override
451:     public float spanScale() {
452:         return spanScale;
453:     }
454: 
455:     @Override
456:     public float lengthScale() {
457:         return lengthScale;
458:     }
459: }
460: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/network/SkillSegmentMath.java">
1: package com.bslx.XueXiMod.network;
2: 
3: import com.bslx.XueXiMod.debug.DebugLog;
4: import com.bslx.XueXiMod.shu.ElementReaction;
5: import com.bslx.XueXiMod.shu.Technique;
6: import com.bslx.XueXiMod.shu.technique.EnchantmentAccumulator;
7: import com.bslx.XueXiMod.shu.technique.EnchantmentDefinition;
8: import com.bslx.XueXiMod.shu.technique.EnchantmentRegistry;
9: import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
10: import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
11: import com.bslx.XueXiMod.shu.technique.release.ChainRole;
12: import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
13: import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
14: import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
15: import com.bslx.XueXiMod.shu.technique.release.ReleaseTag;
16: import com.bslx.XueXiMod.shu.technique.sword.SwordEntity;
17: import com.bslx.XueXiMod.skill.Skill;
18: import net.minecraft.server.level.ServerLevel;
19: import net.minecraft.server.level.ServerPlayer;
20: 
21: import java.util.ArrayList;
22: import java.util.List;
23: 
24: final class SkillSegmentMath {
25:     /** 装配链是否含「操纵世界剑实体」类术式（由 ReleaseDefinition 登记，不写死 id）。 */
26:     static boolean hasSwordManipulation(List<Technique> techniques) {
27:         for (Technique technique : techniques) {
28:             ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
29:             if (definition != null && definition.manipulatesSwordEntities()) {
30:                 return true;
31:             }
32:         }
33:         return false;
34:     }
35: 
36:     static boolean refusesMixedChain(List<Technique> techniques) {
37:         int effectiveCount = 0;
38:         boolean refuses = false;
39:         for (Technique technique : techniques) {
40:             if (technique == null || isRemovedRule(technique.getId())) continue;
41:             effectiveCount++;
42:             ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
43:             refuses |= definition != null && (definition.manipulatesSwordEntities() || definition.refusesMixedChain());
44:         }
45:         return effectiveCount > 1 && refuses;
46:     }
47: 
48:     static boolean hasPlantedWorldSword(ServerPlayer player) {
49:         if (!(player.level() instanceof ServerLevel level)) {
50:             return false;
51:         }
52:         return SwordEntity.anyOwnedBy(level, player.getUUID());
53:     }
54: 
55:     /** 空手可放：链上有操纵类术式，且维度里存在该玩家拥有的存活剑实体。 */
56:     static boolean allowsEmptyHandSwordCast(ServerPlayer player, Skill skill) {
57:         return skill != null
58:                 && hasSwordManipulation(skill.getTechniques())
59:                 && hasPlantedWorldSword(player);
60:     }
61: 
62:     static ChainRole roleOf(Technique technique) {
63:         ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
64:         if (definition == null) {
65:             return ChainRole.ATTACK;
66:         }
67:         return definition.role();
68:     }
69: 
70:     static boolean isChainModifier(ChainRole role) {
71:         return role == ChainRole.SEEK_SPACE
72:                 || role == ChainRole.SEEK_ENTITY
73:                 || role == ChainRole.TRAJECTORY_TRACKING || role == ChainRole.TRAJECTORY_DROP
74:                 || role == ChainRole.TRAJECTORY_SCATTER
75:                 || role == ChainRole.RULE_TRIPLE
76:                 || role == ChainRole.RULE_DUAL;
77:     }
78: 
79:     static boolean isChainModifier(Technique technique) {
80:         if (technique == null) {
81:             return false;
82:         }
83:         if (isChainModifier(roleOf(technique))) {
84:             return true;
85:         }
86:         ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
87:         if (definition == null) {
88:             return false;
89:         }
90:         float supplied = definition.chainEnergyScale();
91:         return Float.isFinite(supplied) && Math.abs(supplied - 1f) > 0.001f;
92:     }
93: 
94:     static boolean isRemovedRule(String techniqueId) {
95:         return "rule_interleave".equals(techniqueId) || "rule_damage_tally".equals(techniqueId);
96:     }
97: 
98:     static int baseCooldownSum(List<ChainSegment> segments, List<Technique> techniques) {
99:         int sum = 0;
100:         for (ChainSegment segment : segments) {
101:             sum += techniques.get(segment.actionIndex()).getBaseCooldown();
102:         }
103:         return sum;
104:     }
105: 
106:     static float segmentDamage(Technique action, ChainSegment segment, float weaponBonus, int magicDamage, Double attributeDamageBonus) {
107:         float base = scaledBaseDamage(action.getBaseDamage(), attributeDamageBonus);
108:         return action.getBaseDamage() > 0
109:                 ? base + weaponBonus + magicDamage
110:                 : base + weaponBonus;
111:     }
112: 
113:     static float scaledBaseDamage(float baseDamage, Double attributeDamageBonus) {
114:         if (attributeDamageBonus == null || baseDamage <= 0) return baseDamage;
115:         return (float) Math.max(0, baseDamage * (1 + attributeDamageBonus));
116:     }
117: 
118:     static int scaleCooldown(int ticks, double cooldownReduction) {
119:         if (ticks <= 0) return ticks;
120:         return Math.max(1, (int) Math.round(ticks * (1 - cooldownReduction)));
121:     }
122: 
123:     static ReleaseKind releaseKind(String techniqueId) {
124:         ReleaseDefinition definition = ReleaseRegistry.get(techniqueId);
125:         return definition != null ? definition.kind() : null;
126:     }
127: 
128:     static boolean isTriggerRelease(String techniqueId) {
129:         ReleaseDefinition definition = ReleaseRegistry.get(techniqueId);
130:         return definition != null && definition.tag() == ReleaseTag.TRIGGER;
131:     }
132: 
133:     static boolean manipulatesSwordEntities(String techniqueId) {
134:         ReleaseDefinition definition = ReleaseRegistry.get(techniqueId);
135:         return definition != null && definition.manipulatesSwordEntities();
136:     }
137: 
138:     static EnchantmentResolution resolveSegmentEnchantments(Skill skill, ChainSegment segment) {
139:         EnchantmentResolution resolution = EnchantmentResolution.empty();
140:         List<Technique> techniques = skill.getTechniques();
141:         for (int index : segment.enchantIndexes()) {
142:             Technique technique = techniques.get(index);
143:             EnchantmentDefinition definition = EnchantmentRegistry.get(technique.getId());
144:             if (definition == null) {
145:                 DebugLog.log("附魔未登记：" + technique.getId());
146:                 continue;
147:             }
148:             ElementReaction reaction = ElementReaction.of(skill.getEmbryo(index), definition.element());
149:             DebugLog.log("附魔 " + technique.getId() + " 原胚=" + skill.getEmbryo(index).getId()
150:                     + " " + reaction.describe());
151:             EnchantmentAccumulator current = resolution.effect(definition.createAccumulator().effectId());
152:             resolution = resolution.with(definition.fold(reaction, current));
153:         }
154:         return resolution;
155:     }
156: 
157:     static int segmentMagicDamage(Skill skill, ChainSegment segment) {
158:         int total = 0;
159:         List<Technique> techniques = skill.getTechniques();
160:         for (int index : segment.enchantIndexes()) {
161:             Technique technique = techniques.get(index);
162:             EnchantmentDefinition definition = EnchantmentRegistry.get(technique.getId());
163:             if (definition == null) {
164:                 continue;
165:             }
166:             ElementReaction reaction = ElementReaction.of(skill.getEmbryo(index), definition.element());
167:             total += definition.magicDamage(reaction);
168:         }
169:         return total;
170:     }
171: 
172:     static int segmentManaCost(List<Technique> techniques, ChainSegment segment) {
173:         int cost = manaCostOf(techniques.get(segment.actionIndex()));
174:         for (int index : segment.enchantIndexes()) {
175:             cost += manaCostOf(techniques.get(index));
176:         }
177:         return cost;
178:     }
179: 
180:     private static int manaCostOf(Technique technique) {
181:         ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
182:         if (definition != null && definition.explicitManaCost() >= 0) {
183:             return definition.explicitManaCost();
184:         }
185:         return technique.manaCost();
186:     }
187: 
188:     static boolean sharesDualRelease(List<Technique> techniques, int leftIndex, int rightIndex) {
189:         List<Integer> playable = new ArrayList<>();
190:         for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
191:             if (!techniques.get(techniqueIndex).isEnchantment()) {
192:                 playable.add(techniqueIndex);
193:             }
194:         }
195:         for (int playableIndex = 0; playableIndex < playable.size(); playableIndex++) {
196:             int techniqueIndex = playable.get(playableIndex);
197:             ReleaseDefinition definition = ReleaseRegistry.get(techniques.get(techniqueIndex).getId());
198:             if (definition == null || definition.role() != ChainRole.RULE_DUAL) {
199:                 continue;
200:             }
201:             int pull = definition.pullAfterA();
202:             int available = Math.max(0, playable.size() - (playableIndex + 2));
203:             int follow = pull < 0 ? available : Math.min(pull, available);
204:             int lastPlayable = playableIndex + 1 + follow;
205:             boolean leftFound = false;
206:             boolean rightFound = false;
207:             for (int member = playableIndex + 1; member <= lastPlayable && member < playable.size(); member++) {
208:                 int memberIndex = playable.get(member);
209:                 if (memberIndex == leftIndex) {
210:                     leftFound = true;
211:                 }
212:                 if (memberIndex == rightIndex) {
213:                     rightFound = true;
214:                 }
215:             }
216:             if (leftFound && rightFound) {
217:                 return true;
218:             }
219:         }
220:         return false;
221:     }
222: }
223: 
</source_file>
