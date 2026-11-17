package com.bslx.XueXiMod.shu.technique.release.firecast;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 灼地短期去重账。只活在这次服务器进程里，不写 NBT，不持有实体。
 * 首次键是施法者、一次真实释放的共享令牌、目标。周期键是施法者与目标，只服务灼地。
 * 令牌用同一次 {@code ReleaseBatch.fireContext().castId()}。宿主出生时抄下这枚 UUID，
 * 并 {@link #retainRelease(UUID)} 一次；宿主结束只 {@link #releaseHost(UUID)}。
 * 首次与周期分开。首次成功的那个宿主，本 tick 不要再 {@link #admitPeriod}。
 * 入账走 {@link com.bslx.XueXiMod.status.fire.seed.FireSeedProduction#produce}，
 * 不要把共享释放上下文交给 {@code FireSeedState.grant}。
 * 时间用 {@code MinecraftServer.getTickCount()}。
 */
public final class ScorchSeedLedger {
    public static final int FIRST_BASE = 20;
    public static final int PERIOD_BASE = 15;
    public static final int PERIOD_GAP = 20;

    private static final Object LEDGER_LOCK = new Object();
    private static final Set<FirstKey> FIRST_CLAIMS = new HashSet<>();
    private static final Map<UUID, Integer> RELEASE_HOLDS = new HashMap<>();
    private static final Map<PeriodKey, Integer> PERIOD_GATES = new HashMap<>();

    private ScorchSeedLedger() {
    }

    /** 场龄 20、40、60、80 才是周期候选。100 是结束拍，不是候选。 */
    public static boolean periodCandidate(int fieldAge) {
        return fieldAge == 20 || fieldAge == 40 || fieldAge == 60 || fieldAge == 80;
    }

    /** 每个宿主出生调用一次。返回调用后的持有数。 */
    public static int retainRelease(UUID releaseToken) {
        if (releaseToken == null) {
            return 0;
        }
        synchronized (LEDGER_LOCK) {
            Integer count = RELEASE_HOLDS.get(releaseToken);
            int current = count == null ? 0 : count.intValue();
            int raised = current >= Integer.MAX_VALUE ? Integer.MAX_VALUE : current + 1;
            RELEASE_HOLDS.put(releaseToken, raised);
            return raised;
        }
    }

    /**
     * 每个宿主结束调用一次，与 {@link #retainRelease(UUID)} 成对。
     * 不删除首次记录，也不动周期门限。返回调用后的持有数。
     */
    public static int releaseHost(UUID releaseToken) {
        if (releaseToken == null) {
            return 0;
        }
        synchronized (LEDGER_LOCK) {
            Integer count = RELEASE_HOLDS.get(releaseToken);
            if (count == null || count.intValue() <= 0) {
                return 0;
            }
            int lowered = count.intValue() - 1;
            RELEASE_HOLDS.put(releaseToken, lowered);
            return lowered;
        }
    }

    public static int holdCount(UUID releaseToken) {
        if (releaseToken == null) {
            return 0;
        }
        synchronized (LEDGER_LOCK) {
            Integer count = RELEASE_HOLDS.get(releaseToken);
            if (count == null || count.intValue() <= 0) {
                return 0;
            }
            return count.intValue();
        }
    }

    /**
     * 先记下首次资格再返回。重复调用返回 false。
     * 之后伤害失败或实际新增 0，都不能靠再调一次重领。
     * 不读、不写周期门限。
     */
    public static boolean claimFirst(UUID casterId, UUID releaseToken, UUID targetId) {
        if (casterId == null || releaseToken == null || targetId == null) {
            return false;
        }
        synchronized (LEDGER_LOCK) {
            return FIRST_CLAIMS.add(new FirstKey(casterId, releaseToken, targetId));
        }
    }

    /**
     * 只查看共享周期门限，不消耗。
     * 从未获准，或距上次获准已满 {@link #PERIOD_GAP} 个服务端 tick 时为 true。
     */
    public static boolean periodOpen(UUID casterId, UUID targetId, int serverTick) {
        if (casterId == null || targetId == null || serverTick < 0) {
            return false;
        }
        synchronized (LEDGER_LOCK) {
            Integer lastTick = PERIOD_GATES.get(new PeriodKey(casterId, targetId));
            return lastTick == null || gapOpen(lastTick.intValue(), serverTick);
        }
    }

    /**
     * 场龄是候选并且门限开着时，记下这次获准并返回 true。
     * 同 tick 的第二次返回 false。余额随后实际新增 0 时，这次获准仍然算数。
     * 不读、不写首次账。场龄不是候选时不写门限。
     */
    public static boolean admitPeriod(UUID casterId, UUID targetId, int fieldAge, int serverTick) {
        if (casterId == null || targetId == null || serverTick < 0 || !periodCandidate(fieldAge)) {
            return false;
        }
        synchronized (LEDGER_LOCK) {
            PeriodKey gateKey = new PeriodKey(casterId, targetId);
            Integer lastTick = PERIOD_GATES.get(gateKey);
            if (lastTick != null && !gapOpen(lastTick.intValue(), serverTick)) {
                return false;
            }
            PERIOD_GATES.put(gateKey, serverTick);
            return true;
        }
    }

    /**
     * 这枚令牌的持有数已经到 0 时，删掉它名下的首次记录。
     * 仍有宿主、或从未 {@link #retainRelease(UUID)} 时保持原样并返回 false。
     * 不动周期门限，也不动火种余额。
     */
    public static boolean discardRelease(UUID releaseToken) {
        if (releaseToken == null) {
            return false;
        }
        synchronized (LEDGER_LOCK) {
            Integer count = RELEASE_HOLDS.get(releaseToken);
            if (count == null || count.intValue() != 0) {
                return false;
            }
            FIRST_CLAIMS.removeIf(claim -> releaseToken.equals(claim.releaseToken()));
            RELEASE_HOLDS.remove(releaseToken);
            return true;
        }
    }

    /**
     * 距上次获准已满 {@link #PERIOD_GAP} 个服务端 tick 时删掉这一对的周期门限。
     * 窗口未满则保留并返回 false。不碰首次账。
     */
    public static boolean discardPeriod(UUID casterId, UUID targetId, int serverTick) {
        if (casterId == null || targetId == null || serverTick < 0) {
            return false;
        }
        synchronized (LEDGER_LOCK) {
            PeriodKey gateKey = new PeriodKey(casterId, targetId);
            Integer lastTick = PERIOD_GATES.get(gateKey);
            if (lastTick == null || !gapOpen(lastTick.intValue(), serverTick)) {
                return false;
            }
            PERIOD_GATES.remove(gateKey);
            return true;
        }
    }

    /** 删掉所有已满保留窗口的周期门限。窗口未满的留下。返回删掉的条数。 */
    public static int discardMaturePeriods(int serverTick) {
        if (serverTick < 0) {
            return 0;
        }
        synchronized (LEDGER_LOCK) {
            int removed = 0;
            Iterator<Map.Entry<PeriodKey, Integer>> entries = PERIOD_GATES.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<PeriodKey, Integer> gate = entries.next();
                Integer lastTick = gate.getValue();
                if (lastTick != null && gapOpen(lastTick.intValue(), serverTick)) {
                    entries.remove();
                    removed = removed + 1;
                }
            }
            return removed;
        }
    }

    /** 停服或卸载时清这三张表。不读、不写火种余额和燃烧。 */
    public static void clearTransient() {
        synchronized (LEDGER_LOCK) {
            FIRST_CLAIMS.clear();
            RELEASE_HOLDS.clear();
            PERIOD_GATES.clear();
        }
    }

    private static boolean gapOpen(int lastTick, int serverTick) {
        if (serverTick < lastTick) {
            return false;
        }
        long elapsed = (long) serverTick - (long) lastTick;
        return elapsed >= PERIOD_GAP;
    }

    private record FirstKey(UUID casterId, UUID releaseToken, UUID targetId) {
    }

    private record PeriodKey(UUID casterId, UUID targetId) {
    }
}
