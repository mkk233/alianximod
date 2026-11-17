package com.bslx.XueXiMod.shu.technique.release.deflection.projectile;

import com.bslx.XueXiMod.shu.technique.release.deflection.AttackKey;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionEconomy;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionMarks;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionService;
import com.bslx.XueXiMod.shu.technique.release.deflection.HostKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 箭上的 ForgeData 和同一次命中才有效的待用随机。
 * 未提交的事务不写剩余伤害，也不加完整次数。湿度折扣不进标签。
 * 完整偏转写下当时的直接伤害。部分卸力写下报价里的余伤，不用费用代替伤害。
 */
public final class ArrowBook {
    public static final int VERSION = 1;
    public static final String VERSION_KEY = "xuexi_deflect_version";
    public static final String QUOTED = "xuexi_deflect_quoted";
    public static final String PARTIAL_AT = "xuexi_deflect_partial_at";
    public static final String CAPTURED = "xuexi_deflect_captured";
    public static final String SPEED = "xuexi_deflect_speed";
    public static final String NO_GRAVITY = "xuexi_deflect_nogravity";
    public static final String NO_PHYSICS = "xuexi_deflect_nophysics";
    public static final String INCOMING_X = "xuexi_deflect_in_x";
    public static final String INCOMING_Y = "xuexi_deflect_in_y";
    public static final String INCOMING_Z = "xuexi_deflect_in_z";
    public static final String EXIT_READY = "xuexi_deflect_exit_set";
    public static final String EXIT_HOST = "xuexi_deflect_exit_host";
    public static final String EXIT_GENERATION = "xuexi_deflect_exit_gen";
    public static final String CAPTURE_HOST = "xuexi_deflect_capture_host";
    public static final String CAPTURE_GENERATION = "xuexi_deflect_capture_gen";
    public static final String EXIT_X = "xuexi_deflect_exit_x";
    public static final String EXIT_Y = "xuexi_deflect_exit_y";
    public static final String EXIT_Z = "xuexi_deflect_exit_z";
    public static final String INSIDE = "xuexi_deflect_inside";

    private static final ConcurrentHashMap<UUID, Float> PENDING = new ConcurrentHashMap<>();

    private ArrowBook() {
    }

    /** 直接伤害是否已经冻结进箭的数据。 */
    public static boolean hasFrozen(CompoundTag tag) {
        return tag != null && tag.getBoolean(QUOTED) && tag.contains(DeflectionMarks.REMAINING_DIRECT);
    }

    /** 只冻结这一次直接伤害，不写捕获、不增加完整偏转次数，也不走水势交易。 */
    public static void freezeDirect(CompoundTag tag, float direct) {
        if (tag == null || hasFrozen(tag)) {
            return;
        }
        float stored = Float.isFinite(direct) ? direct : 0F;
        tag.putBoolean(QUOTED, true);
        tag.putFloat(DeflectionMarks.REMAINING_DIRECT, stored);
    }

    /** 读取冻结后的剩余直接伤害。没有记录时为 0。 */
    public static float remaining(CompoundTag tag) {
        if (tag == null || !tag.contains(DeflectionMarks.REMAINING_DIRECT)) {
            return 0f;
        }
        return tag.getFloat(DeflectionMarks.REMAINING_DIRECT);
    }

    /** 已经完成的完整偏转次数。没有记录时为 0。 */
    public static int fullCount(CompoundTag tag) {
        if (tag == null || !tag.contains(DeflectionMarks.FULL_COUNT)) {
            return 0;
        }
        return tag.getInt(DeflectionMarks.FULL_COUNT);
    }

    /** 当前飞行接触序号。没有记录时为 0。 */
    public static int contactSerial(CompoundTag tag) {
        if (tag == null || !tag.contains(DeflectionMarks.CONTACT_SERIAL)) {
            return 0;
        }
        int serial = tag.getInt(DeflectionMarks.CONTACT_SERIAL);
        return Math.max(serial, 0);
    }

    /** 箭当前是否被路线捕获。 */
    public static boolean captured(CompoundTag tag) {
        return tag != null && tag.getBoolean(CAPTURED);
    }

    /**
     * 只有提交成功并且报价真的成交才落账。失败路径保持原标签。
     */
    public static boolean applyCommit(CompoundTag tag, DeflectionEconomy.Quote quote, float direct, int serial,
                                      boolean committed) {
        if (tag == null || !committed || quote == null || !quote.engages() || !Float.isFinite(direct)) {
            return false;
        }
        tag.putInt(VERSION_KEY, VERSION);
        tag.putBoolean(QUOTED, true);
        if (quote.spendsFullAttempt()) {
            int nextCount = fullCount(tag) + 1;
            tag.putFloat(DeflectionMarks.REMAINING_DIRECT, direct);
            tag.putInt(DeflectionMarks.FULL_COUNT, nextCount);
            return true;
        }
        float left = quote.residualDirect();
        if (!Float.isFinite(left) || left < 0f) {
            left = 0f;
        }
        tag.putFloat(DeflectionMarks.REMAINING_DIRECT, left);
        tag.putInt(PARTIAL_AT, serial);
        return true;
    }

    /** 箭仍在某面水体里。第一次进入时从序号 0 开始。 */
    public static void stayContact(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        tag.putBoolean(INSIDE, true);
        if (!tag.contains(DeflectionMarks.CONTACT_SERIAL)) {
            tag.putInt(DeflectionMarks.CONTACT_SERIAL, 0);
        }
    }

    /**
     * 真正离开水体时先忘掉这一序号的防重，再把序号拨到下一个。
     * 键还被占用时不拨号，避免清掉活跃交易后又用同一个序号。
     */
    public static void leaveContact(CompoundTag tag, UUID arrowId) {
        if (tag == null || !tag.getBoolean(INSIDE) || arrowId == null) {
            return;
        }
        int serial = contactSerial(tag);
        if (!DeflectionService.retireAttack(AttackKey.projectile(arrowId, serial))) {
            return;
        }
        tag.putInt(DeflectionMarks.CONTACT_SERIAL, serial + 1);
        tag.putBoolean(INSIDE, false);
    }

    /** 弹体已经离开世界，防重和闸门已由 retireProjectile 清掉。只拨掉还开着的序号。 */
    public static void sealEnded(CompoundTag tag) {
        if (tag == null || !tag.getBoolean(INSIDE)) {
            return;
        }
        tag.putInt(DeflectionMarks.CONTACT_SERIAL, contactSerial(tag) + 1);
        tag.putBoolean(INSIDE, false);
    }

    /** 记下同一次命中才使用的待定直接伤害。 */
    public static void rememberPending(UUID arrowId, float direct) {
        if (arrowId != null && Float.isFinite(direct)) {
            PENDING.put(arrowId, direct);
        }
    }

    /** 读取还没消费的待定直接伤害。 */
    public static Float peekPending(UUID arrowId) {
        if (arrowId == null) {
            return null;
        }
        return PENDING.get(arrowId);
    }

    /** 冻结值或待定值是否应替换这次原版伤害。 */
    public static boolean replacesHurt(CompoundTag tag, UUID arrowId) {
        return hasFrozen(tag) || peekPending(arrowId) != null;
    }

    /** 这次命中用掉待定伤害。 */
    public static void consumePending(UUID arrowId) {
        if (arrowId != null) {
            PENDING.remove(arrowId);
        }
    }

    /** 拍末清掉全部待定伤害。 */
    public static void clearPending() {
        PENDING.clear();
    }

    /** 把这个生物加入箭的附带效果排除名单。 */
    public static void appendExcluded(CompoundTag tag, UUID protectedId) {
        if (tag == null || protectedId == null || excludes(tag, protectedId)) {
            return;
        }
        long[] existing = tag.getLongArray(DeflectionMarks.EXCLUDE_OWNERS);
        long[] grown = Arrays.copyOf(existing, existing.length + 2);
        grown[existing.length] = protectedId.getMostSignificantBits();
        grown[existing.length + 1] = protectedId.getLeastSignificantBits();
        tag.putLongArray(DeflectionMarks.EXCLUDE_OWNERS, grown);
    }

    /** 这个生物是否已在箭的附带效果排除名单里。 */
    public static boolean excludes(CompoundTag tag, UUID targetId) {
        if (tag == null || targetId == null || !tag.contains(DeflectionMarks.EXCLUDE_OWNERS)) {
            return false;
        }
        long[] pairs = tag.getLongArray(DeflectionMarks.EXCLUDE_OWNERS);
        long high = targetId.getMostSignificantBits();
        long low = targetId.getLeastSignificantBits();
        for (int cursor = 0; cursor + 1 < pairs.length; cursor += 2) {
            if (pairs[cursor] == high && pairs[cursor + 1] == low) {
                return true;
            }
        }
        return false;
    }

    /** 记下捕获时的速度、重力和来向。不增加完整次数。 */
    public static void writeCapture(CompoundTag tag, double speed, boolean noGravity, boolean noPhysics, Vec3 incoming,
                                    HostKey hostKey) {
        if (tag == null) {
            return;
        }
        tag.putInt(VERSION_KEY, VERSION);
        tag.putBoolean(CAPTURED, true);
        tag.putDouble(SPEED, speed);
        tag.putBoolean(NO_GRAVITY, noGravity);
        tag.putBoolean(NO_PHYSICS, noPhysics);
        if (incoming != null) {
            tag.putDouble(INCOMING_X, incoming.x);
            tag.putDouble(INCOMING_Y, incoming.y);
            tag.putDouble(INCOMING_Z, incoming.z);
        }
        if (hostKey != null) {
            tag.putUUID(CAPTURE_HOST, hostKey.hostId());
            tag.putLong(CAPTURE_GENERATION, hostKey.generation());
        }
    }

    /** 记下当前还包着箭的宿主，供离体闸门使用。 */
    public static void latchExit(CompoundTag tag, HostKey hostKey) {
        if (tag == null || hostKey == null) {
            return;
        }
        tag.putUUID(EXIT_HOST, hostKey.hostId());
        tag.putLong(EXIT_GENERATION, hostKey.generation());
    }

    /** 读取离体闸门记下的宿主。 */
    public static HostKey exitLatch(CompoundTag tag) {
        if (tag == null || !tag.hasUUID(EXIT_HOST)) {
            return null;
        }
        return new HostKey(tag.getUUID(EXIT_HOST), tag.getLong(EXIT_GENERATION));
    }

    /** 读取捕获时记下的宿主。 */
    public static HostKey captureHost(CompoundTag tag) {
        if (tag == null || !tag.hasUUID(CAPTURE_HOST)) {
            return null;
        }
        return new HostKey(tag.getUUID(CAPTURE_HOST), tag.getLong(CAPTURE_GENERATION));
    }

    /** 箭已经离开那面水体后清掉离体宿主。 */
    public static void clearExit(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        tag.remove(EXIT_HOST);
        tag.remove(EXIT_GENERATION);
    }

    /** 记下最新的放出方向。 */
    public static void writeExit(CompoundTag tag, Vec3 heading) {
        if (tag == null || heading == null) {
            return;
        }
        tag.putBoolean(EXIT_READY, true);
        tag.putDouble(EXIT_X, heading.x);
        tag.putDouble(EXIT_Y, heading.y);
        tag.putDouble(EXIT_Z, heading.z);
    }

    /** 优先读放出方向，没有时回来向。 */
    public static Vec3 exitOrIncoming(CompoundTag tag) {
        if (tag != null && tag.getBoolean(EXIT_READY)) {
            return new Vec3(tag.getDouble(EXIT_X), tag.getDouble(EXIT_Y), tag.getDouble(EXIT_Z));
        }
        if (tag != null && tag.contains(INCOMING_X)) {
            return new Vec3(tag.getDouble(INCOMING_X), tag.getDouble(INCOMING_Y), tag.getDouble(INCOMING_Z));
        }
        return new Vec3(0.0, 0.0, 1.0);
    }

    /** 捕获时保存的速度幅值。 */
    public static double savedSpeed(CompoundTag tag) {
        if (tag == null || !tag.contains(SPEED)) {
            return 0.0;
        }
        return tag.getDouble(SPEED);
    }

    /** 卸下捕获标记。不改剩余伤害和完整次数，也不再扣一次。 */
    public static void markSafeRelease(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        tag.putBoolean(CAPTURED, false);
    }
}
