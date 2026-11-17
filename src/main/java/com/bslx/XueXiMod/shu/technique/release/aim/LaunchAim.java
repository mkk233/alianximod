package com.bslx.XueXiMod.shu.technique.release.aim;

import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.bind.ReleaseExecutionContext;
import com.bslx.XueXiMod.shu.technique.release.bind.ReleaseInputMode;
import com.bslx.XueXiMod.shu.technique.release.bind.TargetCheck;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 一次发射冻结的源点、备用方向和局部目标。
 * 不保存领域取消令牌，已生成攻击不因关域消失。
 * 跨 tick 只经 TargetCheck 重验同一目标，不读玩家标记，也不另选域外目标。
 */
public final class LaunchAim {
    private static final Vec3 FALLBACK_AXIS = new Vec3(0.0, 0.0, 1.0);

    private final Vec3 origin;
    private final Vec3 fallbackDirection;
    private Vec3 lastLegalDirection;
    @Nullable
    private final UUID localTargetId;
    private final boolean automaticLifetime;
    private final long seed;
    @Nullable
    private final TargetCheck targetCheck;

    public LaunchAim(Vec3 origin, Vec3 direction, @Nullable UUID localTargetId,
                     boolean automaticLifetime, long seed, @Nullable TargetCheck targetCheck) {
        this(origin, direction, localTargetId, automaticLifetime, seed, targetCheck, false);
    }

    /**
     * 非自动兼容重载。自动来源必须使用带 TargetCheck 的构造，不能靠这个重载跨 tick 追踪。
     */
    public LaunchAim(Vec3 origin, Vec3 direction, @Nullable UUID localTargetId,
                     boolean automaticLifetime, long seed) {
        this(origin, direction, localTargetId, automaticLifetime, seed, null, false);
    }

    private LaunchAim(Vec3 origin, Vec3 direction, @Nullable UUID localTargetId,
                      boolean automaticLifetime, long seed, @Nullable TargetCheck targetCheck,
                      boolean allowMissingCheck) {
        if (origin == null) {
            throw new IllegalArgumentException("launch origin is required");
        }
        if (automaticLifetime && targetCheck == null && !allowMissingCheck) {
            throw new IllegalArgumentException("automatic launch requires a target check");
        }
        Vec3 unitDirection = unitDirection(direction);
        this.origin = origin;
        this.fallbackDirection = unitDirection;
        this.lastLegalDirection = unitDirection;
        this.localTargetId = localTargetId;
        this.automaticLifetime = automaticLifetime;
        this.seed = seed;
        this.targetCheck = targetCheck;
    }

    /** 读档后没有活的目标检查。只保留方向，不再按 UUID 追踪。 */
    public static LaunchAim remembered(Vec3 origin, Vec3 direction, @Nullable UUID localTargetId,
                                       boolean automaticLifetime, long seed) {
        return new LaunchAim(origin, direction, localTargetId, automaticLifetime, seed, null, true);
    }

    /** 执行模式是领域自动时才算自动来源。 */
    public static boolean automatic(@Nullable CastFrame frame) {
        return frame != null
                && frame.execution != null
                && frame.execution.mode() == ReleaseInputMode.DOMAIN_AUTOMATIC;
    }

    /** 手动来源直接放行。自动来源在每个真正生成前重验本轮令牌。 */
    public static boolean spawnAllowed(@Nullable CastFrame frame, @Nullable ServerLevel level) {
        if (!automatic(frame)) {
            return true;
        }
        if (level == null || level.getServer() == null || frame.execution.token() == null) {
            return false;
        }
        return frame.execution.token().valid(level.getServer().getTickCount());
    }

    /** 用本轮上下文做一份发射快照。源点先钳进领域真实范围。 */
    @Nullable
    public static LaunchAim capture(@Nullable CastFrame frame, @Nullable Vec3 origin, @Nullable Vec3 direction) {
        if (!automatic(frame)) {
            return null;
        }
        ReleaseExecutionContext execution = frame.execution;
        Vec3 source = origin != null ? origin : execution.origin();
        Vec3 facing = direction != null && direction.lengthSqr() > 1.0E-8
                ? direction
                : execution.baseDirection();
        source = frame.clampAutomaticSource(source);
        LaunchAim aim = new LaunchAim(source, facing, execution.localTargetId(),
                true, execution.roundSeed(), execution.targetCheck());
        frame.launchAim = aim;
        return aim;
    }

    public Vec3 origin() {
        return this.origin;
    }

    /** 最近一次仍合法的方向；还没有命中过目标时就是本轮备用方向。 */
    public Vec3 direction() {
        return this.lastLegalDirection;
    }

    /** 记下仍合法的方向，供目标失效后沿用。过短的方向不覆盖。 */
    public void rememberDirection(@Nullable Vec3 legalDirection) {
        if (legalDirection == null || legalDirection.lengthSqr() <= 1.0E-8) {
            return;
        }
        this.lastLegalDirection = legalDirection.normalize();
    }

    @Nullable
    public UUID localTargetId() {
        return this.localTargetId;
    }

    public boolean automaticLifetime() {
        return this.automaticLifetime;
    }

    public long seed() {
        return this.seed;
    }

    /**
     * 只重验冻结的那个目标。世界不一致、死亡、出界或关系不符都是 null。
     * 不重新选目标，也不读玩家锁定标记。
     */
    @Nullable
    public LivingEntity findTarget(@Nullable ServerLevel level) {
        if (level == null || this.targetCheck == null) {
            return null;
        }
        LivingEntity found = this.targetCheck.stillValid(this.localTargetId);
        if (found == null || found.isRemoved() || !found.isAlive() || found.isSpectator()) {
            return null;
        }
        if (found.level() != level) {
            return null;
        }
        return found;
    }

    private static Vec3 unitDirection(@Nullable Vec3 direction) {
        if (direction == null || direction.lengthSqr() < 1.0E-8) {
            return FALLBACK_AXIS;
        }
        return direction.normalize();
    }
}
