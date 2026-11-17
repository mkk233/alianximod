package com.bslx.XueXiMod.shu.technique.release.batch;

import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** 一次宿主提交期间的出生参数。旧生成路径不安装它，行为不变。 */
public final class BirthLayout {
    public static final class Slot {
        public int copyOrdinal;
        public int spreadCount = 1;
        public boolean respectLanding;
        public boolean earliestSweep;
        public HitClock hitClock;
        public UUID rootCastId;
        public int branchOrdinal;
        public BatchFollow follow;
        public OcclusionSnapshot occlusion;
        public ImpactPartition partition = ImpactPartition.none();
        public double childRadius;
        public Vec3 forcedOrigin;
        public Vec3 forcedDirection;
        public boolean childInstance;
        public boolean resourceCommit;
        public boolean upwellThisCast;
        public boolean parallelThisCast;
        public Vec3 parallelHeading;
        public ResourceEnchantLoad resourceLoad = ResourceEnchantLoad.EMPTY;
        public int bodyScaledBase;
    }

    private static final ThreadLocal<Slot> CURRENT = new ThreadLocal<>();

    private BirthLayout() {
    }

    /** 这次宿主提交期间安装出生参数。 */
    public static void install(Slot slot) {
        CURRENT.set(slot);
    }

    /** 提交结束时卸下，避免下一拍读到。 */
    public static void clear() {
        CURRENT.remove();
    }

    public static Slot current() {
        return CURRENT.get();
    }

    /** 把当前出生参数抄进命中规格。没安装时不改规格。 */
    public static void apply(TechniqueHitSpec spec) {
        Slot slot = CURRENT.get();
        if (slot == null || spec == null) {
            return;
        }
        spec.settleCopyIndex = slot.copyOrdinal;
        spec.spreadCount = Math.max(1, slot.spreadCount);
        spec.respectLanding = slot.respectLanding;
        spec.earliestSweep = slot.earliestSweep;
        spec.hitClock = slot.hitClock;
        spec.rootCastId = slot.rootCastId;
        spec.branchOrdinal = slot.branchOrdinal;
        spec.batchFollow = slot.follow;
        spec.occlusion = slot.occlusion;
        spec.partition = slot.partition;
        spec.childRadius = slot.childRadius;
        spec.childInstance = slot.childInstance;
        if (slot.forcedOrigin != null) {
            spec.start = slot.forcedOrigin;
            spec.anchor = slot.forcedOrigin;
            spec.centered = false;
        }
        if (slot.forcedDirection != null && slot.forcedDirection.lengthSqr() > 1.0E-8) {
            spec.aim = slot.forcedDirection;
        }
        spec.resourceCommit = slot.resourceCommit;
        if (slot.upwellThisCast) {
            spec.upwellThisCast = true;
        }
        if (slot.parallelThisCast) {
            spec.parallelThisCast = true;
            if (slot.parallelHeading != null && slot.parallelHeading.lengthSqr() > 1.0E-8) {
                spec.parallelHeading = slot.parallelHeading;
            }
        }
        spec.resourceLoad = slot.resourceLoad == null ? ResourceEnchantLoad.EMPTY : slot.resourceLoad;
        spec.bodyScaledBase = slot.bodyScaledBase;
        spec.legacyFixedPoints = Math.max(0, spec.settleSeedPoints);
    }
}
