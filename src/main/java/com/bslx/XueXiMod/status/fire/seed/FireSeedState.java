package com.bslx.XueXiMod.status.fire.seed;

import com.bslx.XueXiMod.status.AttributeStateDefinition;
import com.bslx.XueXiMod.status.AttributeStateService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/**
 * 火种 xueximod:fire_seed。实体上的 0–100 整数，走属性状态的读写，不注册药水。
 * 预览、扣点和生产只在服务端主线程、且目标仍存活时改账。0 与无资源相同。
 * 生产倍率由调用方从炉心读出后传入；本类不改炉心热量、档位或运行态。
 * 查询时长使用无限哨兵，不借用潮湿的有限持续时间。
 * 余额变化会通知客户端显示火苗，那份副本不参与生产和消耗。
 */
public final class FireSeedState {
    public static final ResourceLocation IDENTIFIER = new ResourceLocation("xueximod", "fire_seed");
    public static final AttributeStateDefinition DEFINITION =
            new AttributeStateDefinition("fire_seed", FireSeedMath.UPPER_BOUND);
    /** 无限、无倒计时。与原版无限持续时间的哨兵相同，本资源本身没有药水。 */
    public static final int UNLIMITED_DURATION = -1;

    private FireSeedState() {
    }

    /** 服务端主线程上的当前火种。其他线程和客户端返回 0。 */
    public static int value(LivingEntity target) {
        if (!onServerMainThread(target)) {
            return 0;
        }
        return FireSeedMath.clamp(AttributeStateService.read(target, DEFINITION));
    }

    /** 按当前余额预览能投入多少。不扣点，也不占消费机会。 */
    public static Optional<FireSeedSnapshot> preview(LivingEntity target, FireCastContext cast,
                                                     int requested, double furnaceBonus) {
        if (!actionable(target)) {
            return Optional.empty();
        }
        return account(target).preview(cast, target.getUUID(), requested, furnaceBonus);
    }

    /** 直接按当前余额消费。 */
    public static Optional<FireSeedSnapshot> consume(LivingEntity target, FireCastContext cast,
                                                     int requested, double furnaceBonus) {
        return consumePrepared(target, cast, requested, furnaceBonus, snapshot -> true);
    }

    /**
     * 先用同一笔读到的余额做完校验，通过后才扣款。
     * 校验失败不写余额，也不标记消费。
     */
    public static Optional<FireSeedSnapshot> consumePrepared(LivingEntity target, FireCastContext cast,
                                                             int requested, double furnaceBonus,
                                                             SnapshotReady ready) {
        if (!actionable(target) || ready == null) {
            return Optional.empty();
        }
        int raw = AttributeStateService.read(target, DEFINITION);
        FireSeedAccount account = new FireSeedAccount(raw);
        Optional<FireSeedSnapshot> seen = account.preview(cast, target.getUUID(), requested, furnaceBonus);
        if (seen.isEmpty() || !ready.ready(seen.get())) {
            return Optional.empty();
        }
        Optional<FireSeedSnapshot> taken = account.consume(cast, target.getUUID(), requested, furnaceBonus);
        persist(target, raw, account.balance());
        return taken;
    }

    /** 从当前余额减去已经算好的燃料。不标记施法消费，也不把余额改写成调用前的旧值。 */
    public static int draw(LivingEntity target, int spent) {
        if (!actionable(target) || spent <= 0) {
            return 0;
        }
        int raw = AttributeStateService.read(target, DEFINITION);
        FireSeedAccount account = new FireSeedAccount(raw);
        int removed = account.draw(spent);
        persist(target, raw, account.balance());
        return removed;
    }

    public interface SnapshotReady {
        boolean ready(FireSeedSnapshot snapshot);
    }

    /** 按倍率产种并占掉这一施法的生产机会。倍率来自调用方读到的炉心，这里不改炉心。 */
    public static int grant(LivingEntity target, FireCastContext cast, int baseAmount, double productionMultiplier) {
        if (!actionable(target)) {
            return 0;
        }
        int raw = AttributeStateService.read(target, DEFINITION);
        FireSeedAccount account = new FireSeedAccount(raw);
        int increased = account.grant(cast, target.getUUID(), baseAmount, productionMultiplier);
        persist(target, raw, account.balance());
        return increased;
    }

    /** 合并请求已经由调用方取整。不能行动时拒绝，不占机会。 */
    static SeedMergeResult mergePrepared(LivingEntity target, FireCastContext cast, int requested) {
        if (!actionable(target) || cast == null) {
            return SeedMergeResult.rejected();
        }
        int stored = AttributeStateService.read(target, DEFINITION);
        FireSeedAccount account = new FireSeedAccount(stored);
        SeedMergeResult result = account.grantMerged(cast, target.getUUID(), requested);
        persist(target, stored, account.balance());
        return result;
    }

    /**
     * 已经取整的窗口点数。不能行动时返回 -1，调用方不得记下窗口。
     * 余额已满时返回 0，点数没有增加，但调用本身已经尝试入账。
     */
    static int depositWindow(LivingEntity target, int flooredPoints) {
        if (!actionable(target) || flooredPoints <= 0) {
            return -1;
        }
        int stored = AttributeStateService.read(target, DEFINITION);
        FireSeedAccount account = new FireSeedAccount(stored);
        int increased = account.depositUnscoped(flooredPoints);
        persist(target, stored, account.balance());
        return increased;
    }

    /** 死亡或死亡克隆：只删火种键，不动同一复合标签里的其他状态。 */
    public static void discard(LivingEntity target) {
        if (!onServerMainThread(target)) {
            return;
        }
        int stored = AttributeStateService.read(target, DEFINITION);
        AttributeStateService.write(target, DEFINITION, 0);
        if (stored != 0) {
            FireSeedVisualRelay.publish(target, 0);
        }
    }

    /**
     * 存活克隆或存活转化。只把火种整数抄到新实体。
     * 原实体可以已经离世界，只要持久数据还在对象上。
     */
    public static void retainLivingClone(LivingEntity original, LivingEntity fresh) {
        if (original == null || !onServerMainThread(fresh)) {
            return;
        }
        int kept = FireSeedMath.clamp(AttributeStateService.read(original, DEFINITION));
        AttributeStateService.write(fresh, DEFINITION, kept);
        if (kept > 0) {
            FireSeedVisualRelay.publish(fresh, kept);
        }
    }

    private static FireSeedAccount account(LivingEntity target) {
        return new FireSeedAccount(AttributeStateService.read(target, DEFINITION));
    }

    private static void persist(LivingEntity target, int raw, int updated) {
        if (raw != updated) {
            int stored = FireSeedMath.clamp(updated);
            AttributeStateService.write(target, DEFINITION, stored);
            FireSeedVisualRelay.publish(target, stored);
        }
    }

    private static boolean actionable(LivingEntity target) {
        return onServerMainThread(target) && target.isAlive() && target.getUUID() != null;
    }

    private static boolean onServerMainThread(LivingEntity target) {
        if (target == null || target.level() == null || target.level().isClientSide()) {
            return false;
        }
        MinecraftServer server = target.level().getServer();
        return server != null && server.isSameThread();
    }
}
