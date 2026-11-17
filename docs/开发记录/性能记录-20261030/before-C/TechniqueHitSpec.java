package com.bslx.XueXiMod.shu.technique.release.entity;

import com.bslx.XueXiMod.command.SkillEntityCount;
import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
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

import java.util.function.Consumer;

public final class TechniqueHitSpec {

    public enum Motion {
        SWING,
        PROJECTILE,
        DROP,
        FOLLOW
    }

    public enum Hitbox {
        ARC,
        OBB,
        BOX,
        SPHERE
    }

    public enum Path {
        STILL_AIM,
        STRAIGHT,
        DROP,
        FOLLOW,
        VORTEX,
        FUNCTION
    }

    public interface HitListener {
        void onHit(LivingEntity target);

        void onExpire(Vec3 position);
    }

    public interface HitListenerFactory {
        HitListener create();
    }

    public String techniqueId = "";
    public Motion motion = Motion.PROJECTILE;
    public Hitbox hitbox = Hitbox.BOX;
    public int lifetime = 20;
    public double speed = 1.0;
    public boolean lockThisCast;
    public boolean dropThisCast;
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
    public Path path;
    public String functionId = "";
    public double length = 1.0;
    public double width = 1.0;
    public double height = 1.0;
    public String texture;
    public boolean centered;
    public boolean pose3d;
    public float roll;
    public HitListener listener;
    public HitListenerFactory listenerFactory;
    public float settleDamage = Float.NaN;
    public EnchantmentResolution settleEnchantments;
    public ReleaseBatch.Tally settleTally;
    public int settleCopyIndex;
    public int settleBurnSeconds;
    /** 术式自己的火种。生成时再加帧上的点火附魔。 */
    public int seedPoints;
    /** 这一份实体在伤害被接受时挂上的合计。 */
    public int settleSeedPoints;
    public int settleControlTicks;
    public boolean settleExtinguish;
    public float settleKnockback;
    public com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess settleTide =
            com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess.NONE;
    public Vec3 localOffset;
    public boolean trap;
    public boolean burstOnImpact;
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

    public static void spawn(ServerLevel level, ServerPlayer caster, TechniqueHitSpec spec) {
        spawnReported(level, caster, spec);
    }

    /** 返回这一次调用里实际要求生成和真正进入世界的数量。 */
    public static SpawnReport spawnReported(ServerLevel level, ServerPlayer caster, TechniqueHitSpec spec) {
        CastFrame frame = CastFrame.current();
        boolean arcBand = spec.arcDraft != null;
        spec.settleSeedPoints = Math.max(0, spec.seedPoints);
        if (frame != null) {
            spec.tracking = frame.tracking && frame.trajectoryMutable;
            if (!spec.suppressRootSettle) {
                spec.settleDamage = frame.damage;
                spec.settleEnchantments = frame.enchantments;
                spec.settleTally = frame.tally;
                spec.settleCopyIndex = frame.copyIndex;
            }
            float volumeScale = frame.volumeScale();
            ReleaseDefinition definition = ReleaseRegistry.get(spec.techniqueId);
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
                spec.settleTide = frame.tideSuccess == null
                        ? com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess.NONE
                        : frame.tideSuccess;
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
                }
                if (frame.trajectoryMutable && frame.pathOverride == Trajectory.VORTEX) {
                    spec.path = Path.VORTEX;
                    spec.start = frame.origin != null ? frame.origin : caster.getEyePosition();
                    spec.aim = frame.look;
                } else if (frame.drop && frame.trajectoryMutable) {
                    spec.path = Path.DROP;
                    spec.start = frame.origin != null ? frame.origin : caster.getEyePosition();
                    spec.aim = frame.look;
                } else if (path == Path.FUNCTION) {
                    // 不可改弹道的水帘也要吃落点和交错，只搬生成位置，不改成斩击锚点。
                    Vec3 locked = frame.origin != null ? frame.origin : frame.anchor;
                    spec.start = locked != null ? locked : caster.getEyePosition();
                    if (frame.look != null) {
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
                    spec.start = frame.origin != null ? frame.origin : caster.getEyePosition();
                    if (frame.look != null) {
                        spec.aim = frame.look;
                    }
                }
            }
        }
        if (spec.localOffset != null) {
            if (spec.start != null) {
                spec.start = spec.start.add(spec.localOffset);
            } else if (spec.anchor != null) {
                spec.anchor = spec.anchor.add(spec.localOffset);
            } else {
                spec.start = caster.getEyePosition().add(spec.localOffset);
            }
        }
        int baseCopy = spec.settleCopyIndex;
        HostGroupContext hostGroup = HostGroupContext.current();
        int entityCount = hostGroup == null ? SkillEntityCount.entityCount() : hostGroup.spawnCount();
        ReleaseDefinition cohortDefinition = ReleaseRegistry.get(spec.techniqueId);
        boolean swordVisual = cohortDefinition != null && cohortDefinition.separatesCohortVisuals();
        Vec3 bakedStart = spec.start;
        Vec3 bakedAim = spec.aim;
        CastFrame placedFrame = frame;
        Vec3 sampledEye = arcBand ? caster.getEyePosition() : null;
        Vec3 sampledAim = arcBand ? caster.getLookAngle() : null;
        boolean planeSpread = radialPlaneSpread(spec.arcDraft, placedFrame);
        ReleaseInput releaseInput = ReleaseInput.BASE;
        if (frame != null && frame.releaseInput != null) {
            releaseInput = frame.releaseInput;
        }
        int instanceLifetime = instanceLifetime(spec.lifetime, releaseInput, arcBand || spec.fixedLifetime);
        int accepted = 0;
        for (int spawnIndex = 0; spawnIndex < entityCount; spawnIndex++) {
            if (spec.listenerFactory != null) {
                spec.listener = spec.listenerFactory.create();
            }
            spec.settleCopyIndex = CohortVisual.settleEncoding(baseCopy, spawnIndex);
            assignCohort(spec, placedFrame, spawnIndex, entityCount, swordVisual, planeSpread);
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
            if (arcBand) {
                spec.frozenArc = bakeArc(spec, placedFrame, sampledEye, sampledAim);
                if (spec.frozenArc == null) {
                    if (hostGroup != null) {
                        hostGroup.fail();
                    }
                    continue;
                }
            }
            if (spec.keepFollowing) {
                spec.path = Path.FOLLOW;
                spec.centered = false;
                spec.start = null;
                spec.anchor = null;
            }
            TechniqueHitEntity entity = new TechniqueHitEntity(ReleaseEntities.TECHNIQUE_HIT.get(), level);
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
            ReleaseDefinition spawnedDefinition = ReleaseRegistry.get(spec.techniqueId);
            if (spawnedDefinition != null && spawnedDefinition.pressureWetnessMinimum() > 0 && placedFrame != null) {
                WetPressure.attach(entity, placedFrame.releaseGroup, placedFrame.heavyWater,
                        spawnedDefinition.pressureWetnessMinimum());
            }
            boolean published = level.addFreshEntity(entity);
            if (!published) {
                HitPayloads.clear(entity);
                WetPressure.clear(entity);
                if (hostGroup != null) {
                    hostGroup.fail();
                }
                continue;
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
        releaseCohort(spec);
        return new SpawnReport(entityCount, accepted);
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
            copyCount = frame.copyCount < 1 ? 1 : frame.copyCount;
            copyIndex = frame.copyIndex;
        }
        return CohortVisual.capture(copyIndex, copyCount, spawnIndex, spawnCount, swordVisual,
                copyDispersed(frame) || planeSpread, spawnDispersed(frame));
    }

    /** 这一份的生成点或刀面已经按复制号分开。接触点改回同一锚点后不再算。 */
    public static boolean copyDispersed(CastFrame frame) {
        if (frame == null) {
            return false;
        }
        if (spawnDispersed(frame)) {
            return true;
        }
        return frame.cohortDispersed;
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

    public static void assignCohort(TechniqueHitSpec spec, CastFrame frame, int spawnIndex, int spawnCount, boolean swordVisual) {
        assignCohort(spec, frame, spawnIndex, spawnCount, swordVisual, false);
    }

    public static void assignCohort(TechniqueHitSpec spec, CastFrame frame, int spawnIndex, int spawnCount,
                                    boolean swordVisual, boolean planeSpread) {
        spec.cohortVisual = cohortFor(frame, spawnIndex, spawnCount, swordVisual, planeSpread);
    }

    /** 交错、落点和已经改过刀面的路径保持原轨迹。只搬锚点的覆写不算已经散开。 */
    static boolean radialCopiesAlreadySpread(CastFrame frame) {
        if (frame == null) {
            return false;
        }
        if (copyDispersed(frame)) {
            return true;
        }
        return frame.pose3d || Math.abs(frame.roll) > 1.0F;
    }

    static boolean radialPlaneSpread(ArcBandDraft draft, CastFrame frame) {
        if (draft == null || frame == null) {
            return false;
        }
        return RadialCopyPlane.spreads(draft.mode(), frame.copyCount, radialCopiesAlreadySpread(frame));
    }

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
}
