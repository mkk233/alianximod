package com.bslx.XueXiMod.status;

import com.bslx.XueXiMod.shu.technique.release.domain.motion.VolumeMotionRuntime;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/** 三级缓慢下降，四级快速坠落。速度夹紧不改摔伤；恼鬼和末影龙只裁追加的竖直段。 */
public final class WetnessFlight {
    public static final double SLOW_LIMIT = -0.08D;
    public static final double FAST_LIMIT = -0.30D;

    private static final ThreadLocal<LivingEntity> TRAVEL_GUARD = new ThreadLocal<>();
    private static final List<Probe> extraProbes = new ArrayList<>();
    private static final WeakHashMap<EnderDragon, Double> DRAGON_EXTRA = new WeakHashMap<>();

    private WetnessFlight() {}

    /** 实体类本身不能改时，用这个加入飞行判断。不读取免摔伤标签。 */
    public static void register(Probe probe) {
        if (probe != null) {
            extraProbes.add(probe);
        }
    }

    /** 三档竖直速度不超过 {@link #SLOW_LIMIT}，四档不超过 {@link #FAST_LIMIT}。低于上限的不改。 */
    public static double limitedVertical(int tier, double vertical) {
        double ceiling;
        if (tier >= 4) {
            ceiling = FAST_LIMIT;
        } else if (tier == 3) {
            ceiling = SLOW_LIMIT;
        } else {
            return vertical;
        }
        if (vertical > ceiling) {
            return ceiling;
        }
        return vertical;
    }

    /** 相对原竖直速度，潮湿还要再下降的量。已经更快时为 0。 */
    public static double extraDown(int tier, double vanillaVertical) {
        return limitedVertical(tier, vanillaVertical) - vanillaVertical;
    }

    /** 乘客、落地、实际流体不下降。玩家身份和飞行类别不再作为门槛。 */
    public static boolean descentEligible(
            boolean human,
            boolean passenger,
            boolean grounded,
            boolean submerged,
            boolean flyingBody,
            boolean flyingNow,
            boolean explicitFlight) {
        return !passenger && !grounded && !submerged;
    }

    /** 这个实体的类型在不在下降黑名单里。 */
    public static boolean isDescentExempt(LivingEntity entity) {
        return WetnessFlightExempt.isExempt(entity);
    }

    /** 是不是真的在水、岩浆或其他流体里。 */
    public static boolean actualFluid(LivingEntity entity) {
        return entity.isInWater() || entity.isInLava() || entity.isInFluidType();
    }

    /** 旅行前夹一次竖直速度。同一实体重入时不再夹。 */
    public static void beforeTravel(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        LivingEntity active = TRAVEL_GUARD.get();
        if (active == entity) {
            return;
        }
        TRAVEL_GUARD.set(entity);
        try {
            clampDescent(entity);
        } finally {
            TRAVEL_GUARD.set(active);
        }
    }

    /** 恼鬼自己的移动再补一段下降，并按碰撞裁短。有效上升气流时改成至少上升到气流意图。 */
    public static Vec3 adjustVexMove(LivingEntity entity, MoverType type, Vec3 movement) {
        if (!(entity instanceof Vex) || movement == null || type != MoverType.SELF || entity.isPassenger()) {
            return movement;
        }
        if (VolumeMotionRuntime.hasEffectiveUpdraft(entity)) {
            double rise = VolumeMotionRuntime.riseIntent(entity);
            if (movement.y < rise) {
                return new Vec3(movement.x, rise, movement.z);
            }
            return movement;
        }
        if (!canAddExtra(entity)) {
            return movement;
        }
        double extra = extraDown(WetnessTiers.of(entity).tier(), movement.y);
        if (extra == 0.0D) {
            return movement;
        }
        AABB finished = entity.getBoundingBox().move(movement);
        double allowed = WetnessFlightClip.clipExtra(
                finished,
                extra,
                entity.level().getBlockCollisions(entity, finished.expandTowards(0.0D, extra, 0.0D)));
        if (allowed == 0.0D) {
            return movement;
        }
        return new Vec3(movement.x, movement.y + allowed, movement.z);
    }

    /** 龙这一步开始时清掉上次记下的追加下降。 */
    public static void beginDragonStep(EnderDragon dragon) {
        if (dragon == null || dragon.level().isClientSide()) {
            return;
        }
        DRAGON_EXTRA.remove(dragon);
    }

    /** 只记下龙这一步还要追加的下降，不在这里改龙的移动。 */
    public static Vec3 adjustDragonMove(EnderDragon dragon, MoverType type, Vec3 movement) {
        if (dragon == null || dragon.level().isClientSide()) {
            return movement;
        }
        if (movement == null) {
            return movement;
        }
        if (VolumeMotionRuntime.hasEffectiveUpdraft(dragon)) {
            DRAGON_EXTRA.put(dragon, 0.0D);
            return movement;
        }
        if (type != MoverType.SELF || !canAddExtra(dragon)) {
            DRAGON_EXTRA.put(dragon, 0.0D);
            return movement;
        }
        double extra = extraDown(WetnessTiers.of(dragon).tier(), movement.y);
        DRAGON_EXTRA.put(dragon, extra);
        return movement;
    }

    /** 用头、颈、身体里最紧的碰撞裁短龙的追加下降，再把同一段加到龙和各部件上。 */
    public static AABB correctDragonDescent(
            EnderDragon dragon,
            EnderDragonPart head,
            EnderDragonPart neck,
            EnderDragonPart body,
            AABB preparedHead) {
        AABB unchanged = preparedHead;
        if (unchanged == null && head != null) {
            unchanged = head.getBoundingBox();
        }
        if (dragon == null || dragon.level().isClientSide()) {
            return unchanged;
        }
        if (VolumeMotionRuntime.hasEffectiveUpdraft(dragon)) {
            DRAGON_EXTRA.remove(dragon);
            if (head == null) {
                return unchanged;
            }
            return head.getBoundingBox();
        }
        Double stored = DRAGON_EXTRA.remove(dragon);
        if (isDescentExempt(dragon)) {
            return unchanged;
        }
        if (stored == null || stored >= 0.0D) {
            return unchanged;
        }
        double extra = stored;
        double allowedHead = clipDragonPart(dragon, head, extra);
        double allowedNeck = clipDragonPart(dragon, neck, extra);
        double allowedBody = clipDragonPart(dragon, body, extra);
        double allowed = WetnessFlightClip.strictestExtra(extra, allowedHead, allowedNeck, allowedBody);
        if (allowed == 0.0D) {
            return unchanged;
        }
        addVertical(dragon, allowed);
        EnderDragonPart[] parts = dragon.getSubEntities();
        if (parts != null) {
            for (EnderDragonPart part : parts) {
                if (part != null) {
                    addVertical(part, allowed);
                }
            }
        }
        if (head == null) {
            return unchanged;
        }
        return head.getBoundingBox();
    }

    private static void clampDescent(LivingEntity entity) {
        if (entity.level().isClientSide() || entity instanceof Vex || entity instanceof EnderDragon) {
            return;
        }
        if (VolumeMotionRuntime.hasEffectiveUpdraft(entity)) {
            return;
        }
        if (entity instanceof Bat bat && bat.isResting()) {
            return;
        }
        if (!entity.isAlive() || isDescentExempt(entity)) {
            return;
        }
        if (!descentEligible(
                false,
                entity.isPassenger(),
                entity.onGround(),
                actualFluid(entity),
                false,
                false,
                false)) {
            return;
        }
        int tier = WetnessTiers.of(entity).tier();
        if (tier < 3) {
            return;
        }
        Vec3 motion = entity.getDeltaMovement();
        double nextVertical = limitedVertical(tier, motion.y);
        if (nextVertical == motion.y) {
            return;
        }
        entity.setDeltaMovement(motion.x, nextVertical, motion.z);
    }

    private static boolean canAddExtra(LivingEntity entity) {
        return entity != null
                && !entity.level().isClientSide()
                && !isDescentExempt(entity)
                && !entity.isPassenger()
                && !actualFluid(entity);
    }

    private static double clipDragonPart(EnderDragon dragon, EnderDragonPart part, double extra) {
        if (part == null || dragon.level() == null) {
            return 0.0D;
        }
        AABB current = part.getBoundingBox();
        AABB swept = current.expandTowards(0.0D, extra, 0.0D);
        return WetnessFlightClip.clipExtra(current, extra, WetnessFlightClip.dragonObstacles(dragon.level(), dragon, swept));
    }

    private static void addVertical(LivingEntity dragon, double allowed) {
        dragon.setPos(dragon.getX(), dragon.getY() + allowed, dragon.getZ());
    }

    private static void addVertical(EnderDragonPart part, double allowed) {
        part.setPos(part.getX(), part.getY() + allowed, part.getZ());
    }

    @FunctionalInterface
    public interface Probe {
        boolean affected(LivingEntity entity);
    }
}
