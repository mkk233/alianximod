package com.bslx.XueXiMod.shu.technique.release.entity;

import com.bslx.XueXiMod.command.SkillEntityCount;
import com.bslx.XueXiMod.debug.performance.PerfCounter;
import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.DropTilt;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.aim.LaunchAim;
import com.bslx.XueXiMod.shu.technique.release.batch.BatchFollow;
import com.bslx.XueXiMod.shu.technique.release.batch.BirthLayout;
import com.bslx.XueXiMod.shu.technique.release.batch.HitClock;
import com.bslx.XueXiMod.shu.technique.release.batch.ImpactPartition;
import com.bslx.XueXiMod.shu.technique.release.batch.OcclusionSnapshot;
import com.bslx.XueXiMod.shu.technique.release.bind.PublicationAttempt;
import com.bslx.XueXiMod.shu.technique.release.momentum.HeavyWater;
import com.bslx.XueXiMod.shu.technique.release.payload.HitPayloads;
import com.bslx.XueXiMod.shu.technique.release.payload.WetPressure;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBandDraft;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBandSnapshot;
import com.bslx.XueXiMod.shu.technique.release.water.ArcVec;
import com.bslx.XueXiMod.shu.technique.release.water.RadialCopyPlane;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * 一次生成用的规格和生成入口。它把释放帧抄成实体初始状态，并选择随后要用的运动与命中盒。
 * 不负责每 tick 移动、命中去重或无敌帧；那些在实体和结算里发生。
 */
public final class TechniqueHitSpec {

    /** 旧调用留下的粗分类。新路径优先看 {@link Path}。 */
    public enum Motion {
        SWING,
        PROJECTILE,
        DROP,
        FOLLOW
    }

    /** 旧的四种盒子。实体带路径或贴图时会改用按尺寸判定。 */
    public enum Hitbox {
        ARC,
        OBB,
        BOX,
        SPHERE
    }

    /** 生成时交给实体选择运动策略。链上的抛物线和喷涌还会再覆盖。 */
    public enum Path {
        STILL_AIM,
        STRAIGHT,
        DROP,
        FOLLOW,
        VORTEX,
        FUNCTION
    }

    /** 命中或过期时通知调用方。结算和账本不由监听器负责。 */
    public interface HitListener {
        void onHit(LivingEntity target);

        void onExpire(Vec3 position);
    }

    /** 多份生成时每份单独建一个监听器，避免份与份共用可变状态。 */
    public interface HitListenerFactory {
        HitListener create();
    }

    public String techniqueId = "";
    public Motion motion = Motion.PROJECTILE;
    public Hitbox hitbox = Hitbox.BOX;
    public int lifetime = 20;
    public double speed = 1.0;
    /** 这一发读取施法者已有的锁定标记。 */
    public boolean lockThisCast;
    /** 这一发改走垂落路径。显式斜落另看 {@link #dropLanding}。 */
    public boolean dropThisCast;
    /** 显式垂落这一发的固定地面锚点。为空表示仍走原来的竖直路径。 */
    public Vec3 dropLanding;
    public boolean upwellThisCast;
    /** 这一发改走施放朝向的水平直线。优先于垂落、喷涌、跟随和追踪。 */
    public boolean parallelThisCast;
    /** 施放时冻结的水平朝向。为空时才从本次瞄准投影。 */
    public Vec3 parallelHeading;
    /** 直线、垂落或涡旋可换成追踪运动。 */
    public boolean tracking;
    public double arcRadius = 2.0;
    public double arcDegrees = 120.0;
    public double arcHeight = 2.0;
    public Vec3 boxSize = new Vec3(1.0, 1.0, 1.0);
    public int hitInterval;
    /** 服务器年龄达到这个值才开始命中。0 表示生成后就能打到。 */
    public int hitReadyAge;
    public int visual;
    public float modelScale = 1.0F;
    public Vec3 start;
    public Vec3 anchor;
    public Vec3 aim;
    /** 为空时按 {@link #motion} 回退。 */
    public Path path;
    public String functionId = "";
    public double length = 1.0;
    public double width = 1.0;
    public double height = 1.0;
    public String texture;
    public boolean centered;
    public boolean pose3d;
    public boolean explicitObb;
    public float roll;
    public HitListener listener;
    public HitListenerFactory listenerFactory;
    public float settleDamage = Float.NaN;
    public EnchantmentResolution settleEnchantments;
    /** 非空时命中只记入共享账本，不在实体上当场打出伤害。 */
    public ReleaseBatch.Tally settleTally;
    public int settleCopyIndex;
    /** 无共享账本的剑术也保留生成时的动作和冻结花谱。 */
    public UUID vineActionId;
    public boolean vineSpectrumEligible;
    public boolean vineArmed;
    public int spreadCount = 1;
    /** 为真时当场结算改走尊重无敌帧的入口。 */
    public boolean respectLanding;
    /** 为真时沿上一位置到当前位置找最早接触，不再用普通命中盒扫描。 */
    public boolean earliestSweep;
    public double childRadius;
    public boolean childInstance;
    public int branchOrdinal;
    public UUID rootCastId;
    public HitClock hitClock;
    public BatchFollow batchFollow;
    public OcclusionSnapshot occlusion;
    public ImpactPartition partition;
    public boolean resourceCommit;
    public ResourceEnchantLoad resourceLoad = ResourceEnchantLoad.EMPTY;
    public int bodyScaledBase;
    /** 效果和旧附魔已经写进结算火种的固定点数。新链把它和本体基数一起提交一次。 */
    public int legacyFixedPoints;
    public int settleBurnSeconds;
    /** 术式自己的火种。生成时再加帧上的点火附魔。 */
    public int seedPoints;
    /** 这一份实体在伤害被接受时挂上的合计。 */
    public int settleSeedPoints;
    public int settleControlTicks;
    public boolean settleExtinguish;
    public float settleKnockback;
    public TideMarkSuccess settleTide = TideMarkSuccess.NONE;
    public Vec3 localOffset;
    public boolean trap;
    public boolean burstOnImpact;
    /**
     * 第一次合法实体结算后保留飞行体，并继续原来的运动，直到地形结束。
     * 默认关闭。打开时也不按术式标识分支。
     */
    public boolean retainFlightAfterEntity;
    /**
     * 为真时，一次性弹丸命中实体后仍然保留。
     * 默认关闭。只接受调用方的显式声明，不按术式标识判断。
     */
    public boolean retainAfterEntityHit;
    public boolean blocksMovement;
    /** 空表示仍走旧的盒子或弹道。只有声明了弧带的释放会填。 */
    public ArcBandDraft arcDraft;
    /** 这一份实体冻结后的弧。生成循环里逐份写入，不在份与份之间共用可变对象。 */
    public ArcBandSnapshot frozenArc;
    /** 这一次生成要抄到实体上的显示序号。循环内逐份换成新记录，结束后回到单份。 */
    public CohortVisual cohortVisual = CohortVisual.SINGLE;
    /** 静止冻结斩面的单次贴图。默认关闭，调用方显式打开，不在这里识别术式名字。 */
    public StillSheetVisual stillSheet = StillSheetVisual.NONE;
    /** 实体真正进入世界后通知调用方。不在这里识别术式名字。 */
    public Consumer<TechniqueHitEntity> publishedHook;
    /** 为真时不把本帧伤害抄进通用结算，改由命中回调自己打。 */
    public boolean suppressRootSettle;
    /** 为真时强制跟随施法者，落点指定不能把它钉成静止斩。 */
    public boolean keepFollowing;
    /** 为真时实例寿命不吃释放倍率。 */
    public boolean fixedLifetime;
    /** 为真时走廊被实心方块挡住。 */
    public boolean clipsSolids;
    /** 为真时松开、换剑或施法者倒下就结束。 */
    public boolean endsWhenReleased;
    /** 持续术式看这个槽是否仍按着。-1 表示没有槽。 */
    public int releaseSlot = -1;
    /** 这一份实体冻结的发射源。自动持续不能只留前四个标量。 */
    public Vec3 sourceOrigin;
    public Vec3 sourceDirection;
    public UUID localTargetId;
    public boolean automaticLifetime;
    public LaunchAim launchAim;

    /** 一次生成调用要求的份数和真正进入世界的份数。 */
    public static final class SpawnReport {
        private final int requested;
        private final int accepted;

        private SpawnReport(int requested, int accepted) {
            this.requested = requested;
            this.accepted = accepted;
        }

        public int requested() {
            return requested;
        }

        public int accepted() {
            return accepted;
        }

        public boolean complete() {
            return requested > 0 && accepted == requested;
        }
    }

    private static final ThreadLocal<Boolean> PLANNED_SINGLE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /** 只生成一份，不读取当前释放帧或宿主组。结束后恢复原来的单份标记。 */
    public static SpawnReport spawnPlanned(ServerLevel level, ServerPlayer caster, TechniqueHitSpec spec) {
        boolean previous = PLANNED_SINGLE.get();
        PLANNED_SINGLE.set(Boolean.TRUE);
        try {
            return spawnReported(level, caster, spec);
        } finally {
            PLANNED_SINGLE.set(previous);
        }
    }

    /** 旧入口。生成结果丢弃，失败也不会从这里抛出。 */
    public static void spawn(ServerLevel level, ServerPlayer caster, TechniqueHitSpec spec) {
        spawnReported(level, caster, spec);
    }

    /** 返回这一次调用里实际要求生成和真正进入世界的数量。 */
    public static SpawnReport spawnReported(ServerLevel level, ServerPlayer caster, TechniqueHitSpec spec) {
        long spawnStart = PerformanceRecorder.begin();
        try {
        boolean recordingNow = PerformanceRecorder.isRecording();
        boolean plannedSingle = PLANNED_SINGLE.get();
        CastFrame frame = plannedSingle ? null : CastFrame.current();
        boolean arcBand = spec.arcDraft != null;
        ReleaseDefinition definition = ReleaseRegistry.get(spec.techniqueId);
        spec.settleSeedPoints = Math.max(0, spec.seedPoints);
        if (frame != null) {
            spec.tracking = frame.tracking && (frame.trajectoryMutable || functionKeepsTracking(spec));
            if (!spec.suppressRootSettle) {
                spec.settleDamage = frame.damage;
                spec.settleEnchantments = frame.enchantments;
                spec.settleTally = frame.tally;
                spec.settleCopyIndex = frame.copyIndex;
            }
            float volumeScale = frame.volumeScale();
            boolean scaleLength = definition == null || definition.energyScalesLength();
            boolean scaleWidth = definition == null || definition.energyScalesWidth();
            boolean scaleHeight = definition == null || definition.energyScalesHeight();
            boolean scaleModel = definition == null || definition.energyScalesModel();
            if (!arcBand && Math.abs(volumeScale - 1f) > 0.001F) {
                if (scaleLength) {
                    spec.length *= volumeScale;
                }
                if (scaleWidth) {
                    spec.width *= volumeScale;
                }
                if (scaleHeight) {
                    spec.height *= volumeScale;
                }
                if (scaleModel) {
                    spec.modelScale *= volumeScale;
                }
            }
            if (!spec.suppressRootSettle) {
                spec.settleSeedPoints += Math.max(0, frame.seedPoints);
                spec.settleBurnSeconds = frame.burnSeconds;
                spec.settleControlTicks = frame.controlTicks;
                spec.settleExtinguish = frame.extinguish;
                spec.settleKnockback = frame.knockback;
                spec.settleTide = frame.tideSuccess == null ? TideMarkSuccess.NONE : frame.tideSuccess;
                com.bslx.XueXiMod.status.wood.vine.VineMarkActions.Action vineAction =
                        com.bslx.XueXiMod.status.wood.vine.VineMarkActions.current();
                if (vineAction != null && vineAction.source()
                        == com.bslx.XueXiMod.status.wood.vine.VineMarkActions.Source.SWORD) {
                    spec.vineArmed = true;
                    spec.vineActionId = vineAction.actionId();
                    spec.vineSpectrumEligible = vineAction.spectrumEligible();
                    if (spec.settleTally != null) {
                        com.bslx.XueXiMod.status.wood.vine.VineMarkRuntime.stampTally(
                                spec.settleTally, vineAction.actionId(), vineAction.spectrumEligible());
                    }
                }
            }
            if (frame.blocksMovement) {
                spec.blocksMovement = true;
            }
            if (frame.speedMultiplier > 1.001F) {
                spec.speed *= frame.speedMultiplier;
            }
            if (!arcBand && frame.overrideGeometry) {
                Path path = spec.path;
                boolean still = path == Path.STILL_AIM || spec.centered || !frame.trajectoryMutable;
                if (frame.trajectoryMutable && frame.pathOverride != null && frame.pathOverride != Trajectory.STRAIGHT) {
                    spec.dropThisCast = frame.pathOverride == Trajectory.DROP;
                    spec.upwellThisCast = frame.pathOverride == Trajectory.UPWELL;
                    spec.parallelThisCast = frame.pathOverride == Trajectory.PARALLEL;
                }
                if (frame.trajectoryMutable && frame.pathOverride == Trajectory.VORTEX) {
                    spec.path = Path.VORTEX;
                    spec.start = automaticOrigin(frame, caster);
                    spec.aim = frame.look;
                } else if (frame.drop && frame.trajectoryMutable) {
                    spec.path = Path.DROP;
                    spec.start = automaticOrigin(frame, caster);
                    spec.aim = frame.look;
                } else if (path == Path.FUNCTION) {
                    // 不可改弹道的水帘也要吃落点和交错，只搬生成位置，不改成斩击锚点。
                    // 效果已经按本轮源点排好的起点保留；规则没摆过时才回落到帧原点。
                    if (spec.start == null) {
                        Vec3 locked = frame.origin != null ? frame.origin : frame.anchor;
                        spec.start = locked != null ? locked : automaticOrigin(frame, caster);
                    }
                    if (spec.aim == null && frame.look != null) {
                        spec.aim = frame.look;
                    }
                } else if (still) {
                    spec.centered = true;
                    spec.anchor = frame.origin != null ? frame.origin : frame.anchor;
                    spec.aim = frame.planeNormal != null ? frame.planeNormal : frame.look;
                    spec.roll = frame.roll;
                    Vec3 normal = frame.planeNormal != null ? frame.planeNormal : frame.look;
                    spec.pose3d = frame.pose3d
                            || (normal != null && Math.abs(normal.y) > 0.2)
                            || Math.abs(frame.roll) > 1.0;
                } else if (path == Path.STRAIGHT || path == Path.DROP || path == Path.FOLLOW) {
                    boolean parallelKeepsOwnStart = frame.parallel && frame.trajectoryMutable && frame.origin == null;
                    if (!parallelKeepsOwnStart) {
                        spec.start = automaticOrigin(frame, caster);
                    }
                    if (frame.look != null) {
                        spec.aim = frame.look;
                    }
                }
            }
        }
        if (frame != null && frame.automaticSource() && !frame.overrideGeometry) {
            Vec3 source = frame.origin != null ? frame.origin : frame.execution.origin();
            Vec3 facing = frame.look != null ? frame.look : frame.execution.baseDirection();
            if (spec.path != Path.FUNCTION || spec.start == null) {
                spec.start = source;
            }
            if (facing != null && facing.lengthSqr() > 1.0E-8
                    && (spec.path != Path.FUNCTION || spec.aim == null)) {
                spec.aim = facing;
            }
        }
        if (spec.localOffset != null) {
            if (spec.start != null) {
                spec.start = spec.start.add(spec.localOffset);
            } else if (spec.anchor != null) {
                spec.anchor = spec.anchor.add(spec.localOffset);
            } else {
                Vec3 fallback = caster.getEyePosition();
                if (frame != null && frame.automaticSource()) {
                    fallback = frame.origin != null ? frame.origin : frame.execution.origin();
                }
                spec.start = fallback.add(spec.localOffset);
            }
        }
        clampAutomaticPoints(frame, spec);
        int baseCopy = spec.settleCopyIndex;
        HostGroupContext hostGroup = plannedSingle ? null : HostGroupContext.current();
        int entityCount = plannedSingle ? 1 : (hostGroup == null ? SkillEntityCount.entityCount() : hostGroup.spawnCount());
        boolean swordVisual = definition != null && definition.separatesCohortVisuals();
        Vec3 bakedStart = spec.start;
        Vec3 bakedAim = spec.aim;
        CastFrame placedFrame = frame;
        Vec3 sampledEye = null;
        Vec3 sampledAim = null;
        if (arcBand) {
            if (frame != null && frame.automaticSource()) {
                sampledEye = frame.origin != null ? frame.origin : frame.execution.origin();
                sampledAim = frame.look != null ? frame.look : frame.execution.baseDirection();
            } else {
                sampledEye = caster.getEyePosition();
                sampledAim = caster.getLookAngle();
            }
        }
        boolean planeSpread = radialPlaneSpread(spec.arcDraft, placedFrame);
        ReleaseInput releaseInput = ReleaseInput.BASE;
        if (frame != null && frame.releaseInput != null) {
            releaseInput = frame.releaseInput;
        }
        int instanceLifetime = instanceLifetime(spec.lifetime, releaseInput, arcBand || spec.fixedLifetime);
        int constructed = 0;
        int accepted = 0;
        for (int spawnIndex = 0; spawnIndex < entityCount; spawnIndex++) {
            if (spec.listenerFactory != null) {
                spec.listener = spec.listenerFactory.create();
            }
            spec.dropLanding = null;
            spec.start = bakedStart;
            spec.aim = bakedAim;
            spec.settleCopyIndex = CohortVisual.settleEncoding(baseCopy, spawnIndex);
            assignCohort(spec, placedFrame, spawnIndex, entityCount, swordVisual, planeSpread);
            adoptBirthOrdinal(placedFrame);
            if (placedFrame != null && placedFrame.pathOverride == Trajectory.VORTEX && placedFrame.trajectoryMutable) {
                spec.path = Path.VORTEX;
                spec.dropThisCast = false;
                spec.start = placedFrame.anchor != null ? placedFrame.anchor : placedFrame.origin;
                spec.aim = placedFrame.vortexPhase(spawnIndex);
            } else if (placedFrame != null && placedFrame.volumeHalf > 0.0 && placedFrame.trajectoryMutable && placedFrame.anchor != null) {
                spec.start = placedFrame.volumePoint(spawnIndex);
                if (spec.localOffset != null) {
                    spec.start = spec.start.add(spec.localOffset);
                }
                spec.aim = placedFrame.aimFor(spawnIndex);
                if (placedFrame.drop) {
                    spec.path = Path.DROP;
                    spec.dropThisCast = true;
                }
            }
            clampAutomaticPoints(placedFrame, spec);
            Vec3 heldOrigin = spec.start != null ? spec.start : spec.anchor;
            Vec3 heldFacing = spec.aim;
            if (arcBand) {
                spec.frozenArc = bakeArc(spec, placedFrame, sampledEye, sampledAim);
                if (spec.frozenArc == null) {
                    if (hostGroup != null) {
                        hostGroup.fail();
                    }
                    continue;
                }
            }
            if (placedFrame != null && placedFrame.parallel && placedFrame.trajectoryMutable) {
                spec.parallelThisCast = true;
                spec.dropThisCast = false;
                spec.upwellThisCast = false;
                spec.tracking = false;
                spec.keepFollowing = false;
                spec.path = Path.STRAIGHT;
                spec.aim = horizontalHeading(placedFrame.look);
                heldFacing = spec.aim;
            }
            if (spec.keepFollowing) {
                spec.path = Path.FOLLOW;
                spec.centered = false;
                spec.start = null;
                spec.anchor = null;
            }
            PublicationAttempt attempt = PublicationAttempt.current();
            boolean automaticSource = placedFrame != null && placedFrame.automaticSource();
            if ((automaticSource && attempt == null)
                    || (attempt != null && !attempt.allowsPublication())
                    || (automaticSource && !LaunchAim.spawnAllowed(placedFrame, level))) {
                if (hostGroup != null) {
                    hostGroup.fail();
                }
                continue;
            }
            attachAutomaticAim(placedFrame, spec, heldOrigin, heldFacing);
            BirthLayout.apply(spec);
            applyParallelHeading(spec);
            applyExplicitDropTilt(level, caster, placedFrame, spec, spawnIndex);
            TechniqueHitEntity entity = new TechniqueHitEntity(ReleaseEntities.TECHNIQUE_HIT.get(), level);
            constructed++;
            if (hostGroup != null) {
                entity.holdActivation();
            }
            try {
                entity.applySpec(caster, spec, instanceLifetime);
            } catch (RuntimeException failure) {
                if (hostGroup != null) {
                    hostGroup.fail();
                }
                HitPayloads.clear(entity);
                WetPressure.clear(entity);
                continue;
            }
            HitPayloads.attach(entity, placedFrame);
            if (definition != null && definition.pressureWetnessMinimum() > 0 && placedFrame != null) {
                WetPressure.attach(entity, placedFrame.releaseGroup, placedFrame.heavyWater,
                        definition.pressureWetnessMinimum());
            }
            if ((attempt != null && !attempt.allowsPublication()) || !level.addFreshEntity(entity)) {
                HitPayloads.clear(entity);
                WetPressure.clear(entity);
                if (hostGroup != null) {
                    hostGroup.fail();
                }
                continue;
            }
            if (attempt != null) {
                attempt.reportAccepted();
            }
            accepted++;
            if (spec.publishedHook != null) {
                spec.publishedHook.accept(entity);
            }
            if (hostGroup != null) {
                hostGroup.stagePublished(entity);
            } else {
                entity.stampStillBirth();
                HeavyWater.commitFirst(caster);
            }
        }
        spec.settleCopyIndex = baseCopy;
        spec.start = bakedStart;
        spec.aim = bakedAim;
        spec.frozenArc = null;
        spec.launchAim = null;
        spec.sourceOrigin = null;
        spec.sourceDirection = null;
        spec.localTargetId = null;
        spec.automaticLifetime = false;
        releaseCohort(spec);
        if (recordingNow) {
            if (entityCount > 0) {
                PerformanceRecorder.add(PerfCounter.HIT_SPAWN_REQUESTED, entityCount);
            }
            if (constructed > 0) {
                PerformanceRecorder.add(PerfCounter.HIT_SPAWN_CONSTRUCTED, constructed);
            }
            if (accepted > 0) {
                PerformanceRecorder.add(PerfCounter.HIT_SPAWN_ACCEPTED, accepted);
            }
        }
        return new SpawnReport(entityCount, accepted);
        } finally {
            PerformanceRecorder.end(PerfMetric.HIT_SPAWN, spawnStart);
        }
    }

    /**
     * 模板寿命只读一次。弧带窗口不缩放。调用方把结果写进实例，不写回模板。
     */
    public static int instanceLifetime(int templateLifetime, ReleaseInput releaseInput, boolean arcBand) {
        if (arcBand) {
            return templateLifetime;
        }
        ReleaseInput frozenInput = releaseInput == null ? ReleaseInput.BASE : releaseInput;
        return frozenInput.scaleDuration(templateLifetime);
    }

    /** 按这一帧实际采用的几何，给当前生成号做一份不可变显示记录。 */
    public static CohortVisual cohortFor(CastFrame frame, int spawnIndex, int spawnCount, boolean swordVisual) {
        return cohortFor(frame, spawnIndex, spawnCount, swordVisual, false);
    }

    /**
     * {@code planeSpread} 只写进这一份的视觉记录，不改共享帧。
     * 复制轴散开后，同一复制号里的生成倍率仍按生成号分槽。
     */
    public static CohortVisual cohortFor(CastFrame frame, int spawnIndex, int spawnCount, boolean swordVisual,
                                         boolean planeSpread) {
        int copyCount = 1;
        int copyIndex = 0;
        if (frame != null) {
            copyCount = Math.max(1, frame.copyCount);
            copyIndex = frame.copyIndex;
        }
        return CohortVisual.capture(copyIndex, copyCount, spawnIndex, spawnCount, swordVisual,
                copyDispersed(frame) || planeSpread, spawnDispersed(frame));
    }

    /** 这一份的生成点或刀面已经按复制号分开。接触点改回同一锚点后不再算。 */
    public static boolean copyDispersed(CastFrame frame) {
        return frame != null && (spawnDispersed(frame) || frame.cohortDispersed);
    }

    /** 只看生成循环里真正按生成号改位置或方向的分支，不从复制散开标志推断。 */
    public static boolean spawnDispersed(CastFrame frame) {
        if (frame == null) {
            return false;
        }
        if (frame.pathOverride == Trajectory.VORTEX && frame.trajectoryMutable) {
            return true;
        }
        return frame.volumeHalf > 0.0 && frame.trajectoryMutable && frame.anchor != null;
    }

    /** 把显示序号写进这一份规格。生成循环结束时还要单独清掉。 */
    public static void assignCohort(TechniqueHitSpec spec, CastFrame frame, int spawnIndex, int spawnCount, boolean swordVisual) {
        assignCohort(spec, frame, spawnIndex, spawnCount, swordVisual, false);
    }

    public static void assignCohort(TechniqueHitSpec spec, CastFrame frame, int spawnIndex, int spawnCount,
                                    boolean swordVisual, boolean planeSpread) {
        spec.cohortVisual = cohortFor(frame, spawnIndex, spawnCount, swordVisual, planeSpread);
    }

    /** 交错、落点和已经改过刀面的路径保持原轨迹。只搬锚点的覆写不算已经散开。 */
    static boolean radialCopiesAlreadySpread(CastFrame frame) {
        return frame != null && (copyDispersed(frame) || frame.pose3d || Math.abs(frame.roll) > 1.0F);
    }

    static boolean radialPlaneSpread(ArcBandDraft draft, CastFrame frame) {
        if (draft == null || frame == null) {
            return false;
        }
        return RadialCopyPlane.spreads(draft.mode(), frame.copyCount, radialCopiesAlreadySpread(frame));
    }

    /** 生成循环结束后回到单份显示，避免下一发沿用上一份序号。 */
    public static void releaseCohort(TechniqueHitSpec spec) {
        spec.cohortVisual = CohortVisual.SINGLE;
    }

    /** 覆写过的原点和瞄准原样进入快照。没有覆写时，眼位和朝向只用循环外的那一次采样。 */
    static ArcBandSnapshot bakeArc(TechniqueHitSpec spec, CastFrame frame, Vec3 sampledEye, Vec3 sampledAim) {
        boolean framedOrigin = frame != null && frame.overrideGeometry && frame.origin != null;
        Vec3 origin = framedOrigin ? frame.origin : (spec.start != null ? spec.start : sampledEye);
        if (origin == null) {
            return null;
        }
        if (framedOrigin && spec.localOffset != null) {
            origin = origin.add(spec.localOffset);
        }
        if (frame != null && frame.automaticSource()) {
            origin = frame.clampAutomaticSource(origin);
        }
        boolean scatterFacing = frame != null && (frame.pose3d || Math.abs(frame.roll) > 1.0F);
        Vec3 facing = scatterFacing && frame.planeNormal != null && frame.planeNormal.lengthSqr() > 1.0E-8
                ? frame.planeNormal
                : (frame != null && frame.look != null && frame.look.lengthSqr() > 1.0E-8 ? frame.look : spec.aim);
        if (facing == null || facing.lengthSqr() <= 1.0E-8) {
            facing = sampledAim;
        }
        if (facing == null) {
            return null;
        }
        double extraRoll = frame == null ? 0.0 : frame.roll;
        double scale = frame == null ? 1.0 : frame.volumeScale();
        int beat = spec.arcDraft.beat();
        ArcVec frozenOrigin = new ArcVec(origin.x, origin.y, origin.z);
        ArcVec frozenFacing = new ArcVec(facing.x, facing.y, facing.z);
        if (frame != null && frame.copyIndex > 0 && radialPlaneSpread(spec.arcDraft, frame)) {
            RadialCopyPlane.Placement placement = RadialCopyPlane.place(frame.copyIndex, frame.copyCount, beat, extraRoll);
            return ArcBandSnapshot.freezeLook(
                    frozenOrigin,
                    frozenFacing,
                    null,
                    false,
                    placement.extraRollDegrees(beat),
                    beat,
                    spec.arcDraft.baseRadius(),
                    spec.arcDraft.baseHalfWidth(),
                    scale,
                    spec.arcDraft.mode(),
                    placement.sectorStartDegrees(),
                    placement.sectorSweepDegrees());
        }
        return ArcBandSnapshot.freezeLook(
                frozenOrigin,
                frozenFacing,
                null,
                false,
                extraRoll,
                beat,
                spec.arcDraft.baseRadius(),
                spec.arcDraft.baseHalfWidth(),
                scale,
                spec.arcDraft.mode());
    }

    /**
     * 函数弹道本身不改成飞弹，但已经声明接受路径覆盖时，追踪标记要留给水帘上沿。
     * 不因此把整份宿主改成可替换弹道。
     */
    private static boolean functionKeepsTracking(TechniqueHitSpec spec) {
        if (spec == null || spec.path != Path.FUNCTION) {
            return false;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(spec.techniqueId);
        return definition != null
                && definition.trajectory() == Trajectory.FUNCTION
                && definition.acceptsPathOverride();
    }

    /**
     * 建帧前已经写入同一出生序号。这里再写一次，是为了体积重取样和垂落斜线继续读到 0、1、2。
     * 没有出生槽的旧路径不改帧。0 是第一份，不能当成没有序号。
     */
    private static void adoptBirthOrdinal(CastFrame placedFrame) {
        if (placedFrame == null) {
            return;
        }
        BirthLayout.Slot birthSlot = BirthLayout.current();
        if (birthSlot == null) {
            return;
        }
        placedFrame.copyIndex = birthSlot.copyOrdinal;
        placedFrame.copyCount = Math.max(1, birthSlot.spreadCount);
    }

    /** 平行只换方向。生成点保留，垂落、喷涌、跟随和追踪都不再叠加。 */
    private static void applyParallelHeading(TechniqueHitSpec spec) {
        if (spec == null || !spec.parallelThisCast) {
            return;
        }
        Vec3 heading = spec.parallelHeading;
        if (heading == null || heading.lengthSqr() <= 1.0E-8) {
            heading = spec.aim;
        }
        spec.aim = horizontalHeading(heading);
        spec.parallelHeading = spec.aim;
        spec.path = Path.STRAIGHT;
        spec.dropThisCast = false;
        spec.dropLanding = null;
        spec.upwellThisCast = false;
        spec.tracking = false;
        spec.keepFollowing = false;
    }

    private static Vec3 horizontalHeading(Vec3 direction) {
        if (direction == null) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        double horizontalLength = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        if (horizontalLength < 1.0E-4) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return new Vec3(direction.x / horizontalLength, 0.0, direction.z / horizontalLength);
    }

    /**
     * 只改链上显式垂落、且宿主接受这次改写的一发。未装修饰的原生竖落仍竖直。
     * 注册轨迹碰巧是 DROP 不构成例外。风暴只是复用垂落角色，弹道仍是涡旋。
     * 锚点沿改写前的竖直落柱取碰撞顶面，斜线从同一高度横移后汇向它。
     */
    private static void applyExplicitDropTilt(ServerLevel level, ServerPlayer caster, CastFrame frame,
                                              TechniqueHitSpec spec, int spawnOrdinal) {
        if (!acceptsExplicitDrop(frame, spec)) {
            return;
        }
        Vec3 columnStart = spec.start;
        if (columnStart == null) {
            columnStart = automaticOrigin(frame, caster);
        }
        DropTilt.Placement placement = DropTilt.placement(
                level, columnStart, frame.sampleSeed, frame.copyIndex, spawnOrdinal);
        if (placement == null) {
            return;
        }
        spec.dropLanding = placement.anchor();
        spec.start = placement.start();
        if (frame.automaticSource()) {
            spec.start = frame.clampAutomaticSource(spec.start);
        }
        spec.aim = DropTilt.unitToward(spec.start, spec.dropLanding);
        spec.path = Path.DROP;
        spec.dropThisCast = true;
        if (frame.automaticSource()) {
            attachAutomaticAim(frame, spec, spec.start, spec.aim);
        }
    }

    private static boolean acceptsExplicitDrop(CastFrame frame, TechniqueHitSpec spec) {
        if (frame == null || spec == null || !frame.drop) {
            return false;
        }
        if (spec.childInstance || spec.keepFollowing || spec.frozenArc != null) {
            return false;
        }
        if (frame.pathOverride == Trajectory.VORTEX || frame.pathOverride == Trajectory.UPWELL
                || frame.pathOverride == Trajectory.PARALLEL || spec.parallelThisCast) {
            return false;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(spec.techniqueId);
        if (definition == null) {
            return false;
        }
        if (definition.trajectory() == Trajectory.VORTEX || definition.trajectory() == Trajectory.UPWELL) {
            return false;
        }
        if (!definition.trajectoryMutable() && !definition.acceptsPathOverride()) {
            return false;
        }
        return definition.trajectoryMutable() || spec.dropThisCast;
    }

    private static Vec3 automaticOrigin(CastFrame frame, ServerPlayer caster) {
        if (frame.origin != null) {
            return frame.origin;
        }
        if (frame.automaticSource()) {
            return frame.execution.origin();
        }
        return caster.getEyePosition();
    }

    private static void clampAutomaticPoints(CastFrame frame, TechniqueHitSpec spec) {
        if (frame == null || !frame.automaticSource()) {
            return;
        }
        if (spec.start != null) {
            spec.start = frame.clampAutomaticSource(spec.start);
        }
        if (spec.anchor != null) {
            spec.anchor = frame.clampAutomaticSource(spec.anchor);
        }
    }

    private static void attachAutomaticAim(CastFrame frame, TechniqueHitSpec spec, Vec3 heldOrigin, Vec3 heldFacing) {
        if (frame == null || !frame.automaticSource()) {
            spec.launchAim = null;
            spec.automaticLifetime = false;
            return;
        }
        LaunchAim aim = LaunchAim.capture(frame, heldOrigin, heldFacing);
        spec.launchAim = aim;
        if (aim == null) {
            spec.automaticLifetime = false;
            return;
        }
        spec.sourceOrigin = aim.origin();
        spec.sourceDirection = aim.direction();
        spec.localTargetId = aim.localTargetId();
        spec.automaticLifetime = true;
    }
}
