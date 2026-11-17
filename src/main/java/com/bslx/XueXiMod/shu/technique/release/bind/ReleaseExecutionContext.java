package com.bslx.XueXiMod.shu.technique.release.bind;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.UUID;

/** 显式发射源。压栈保存外层，延迟回调用完必须弹出，不能留到下一拍。 */
public final class ReleaseExecutionContext {
    private static final ThreadLocal<ArrayDeque<ReleaseExecutionContext>> STACK = new ThreadLocal<>();
    private static final double MINIMUM_DIRECTION = 1.0E-8;

    private final ReleaseInputMode mode;
    private final ServerLevel level;
    private final ServerPlayer owner;
    private final UUID domainInstanceId;
    private final UUID roundId;
    private final Vec3 origin;
    private final Vec3 baseDirection;
    private final UUID localTargetId;
    private final ReleaseVolume volume;
    private final TargetCheck targetCheck;
    private final ItemStack weaponCopy;
    private final float weaponBonus;
    private final long roundSeed;
    private final LifetimeToken token;

    private ReleaseExecutionContext(ReleaseInputMode mode, ServerLevel level, ServerPlayer owner,
                                    UUID domainInstanceId, UUID roundId, Vec3 origin, Vec3 baseDirection,
                                    UUID localTargetId, ReleaseVolume volume, TargetCheck targetCheck,
                                    ItemStack weaponCopy, float weaponBonus, long roundSeed, LifetimeToken token) {
        this.mode = mode;
        this.level = level;
        this.owner = owner;
        this.domainInstanceId = domainInstanceId;
        this.roundId = roundId;
        this.origin = origin;
        this.baseDirection = baseDirection;
        this.localTargetId = localTargetId;
        this.volume = volume;
        this.targetCheck = targetCheck;
        this.weaponCopy = weaponCopy;
        this.weaponBonus = weaponBonus;
        this.roundSeed = roundSeed;
        this.token = token;
    }

    /** 当前线程栈顶的发射上下文。没有时为空。 */
    public static ReleaseExecutionContext current() {
        ArrayDeque<ReleaseExecutionContext> stack = STACK.get();
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.peek();
    }

    /** 压上这一层。延迟回调结束前必须配对解开。 */
    public static void bind(ReleaseExecutionContext context) {
        if (context == null) {
            throw new IllegalArgumentException("执行上下文不能为空");
        }
        ArrayDeque<ReleaseExecutionContext> stack = STACK.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            STACK.set(stack);
        }
        stack.push(context);
    }

    /** 弹出一层。栈空时清掉线程状态。 */
    public static void unbind() {
        ArrayDeque<ReleaseExecutionContext> stack = STACK.get();
        if (stack == null || stack.isEmpty()) {
            STACK.remove();
            return;
        }
        stack.pop();
        if (stack.isEmpty()) {
            STACK.remove();
        }
    }

    /** 复制武器并归一方向。缺字段时拒绝，不做成残缺上下文。 */
    public static ReleaseExecutionContext automatic(ServerLevel level, ServerPlayer owner, UUID domainInstanceId, UUID roundId,
                                                    Vec3 origin, Vec3 baseDirection, @Nullable UUID localTargetId,
                                                    ReleaseVolume volume, TargetCheck targetCheck, ItemStack weaponCopy,
                                                    float weaponBonus, long roundSeed, LifetimeToken token) {
        if (level == null || owner == null || domainInstanceId == null || roundId == null
                || origin == null || baseDirection == null || volume == null || targetCheck == null
                || weaponCopy == null || token == null || baseDirection.lengthSqr() <= MINIMUM_DIRECTION) {
            throw new IllegalArgumentException("自动释放上下文缺少世界、主人、标识、原点、方向、体积、目标检查、武器或令牌");
        }
        ItemStack frozenWeapon = weaponCopy.copy();
        Vec3 frozenDirection = baseDirection.normalize();
        return new ReleaseExecutionContext(ReleaseInputMode.DOMAIN_AUTOMATIC, level, owner, domainInstanceId, roundId,
                origin, frozenDirection, localTargetId, volume, targetCheck, frozenWeapon, weaponBonus, roundSeed, token);
    }

    public ReleaseInputMode mode() {
        return mode;
    }

    public ServerLevel level() {
        return level;
    }

    public ServerPlayer owner() {
        return owner;
    }

    public UUID domainInstanceId() {
        return domainInstanceId;
    }

    public UUID roundId() {
        return roundId;
    }

    public Vec3 origin() {
        return origin;
    }

    public Vec3 baseDirection() {
        return baseDirection;
    }

    public @Nullable UUID localTargetId() {
        return localTargetId;
    }

    public ReleaseVolume volume() {
        return volume;
    }

    public TargetCheck targetCheck() {
        return targetCheck;
    }

    public ItemStack weaponCopy() {
        return weaponCopy.copy();
    }

    public float weaponBonus() {
        return weaponBonus;
    }

    public long roundSeed() {
        return roundSeed;
    }

    public LifetimeToken token() {
        return token;
    }
}
