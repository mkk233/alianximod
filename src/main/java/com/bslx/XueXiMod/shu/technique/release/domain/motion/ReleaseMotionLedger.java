package com.bslx.XueXiMod.shu.technique.release.domain.motion;

import com.bslx.XueXiMod.shu.technique.release.domain.DomainInstance;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 同一释放组的运动账本。分体共用入卷、偏转、抛出和控权；组结束后丢掉，不留世界级黑名单。
 * 球形场地没有入卷账，护体不记录受控根。
 */
public final class ReleaseMotionLedger {
    private static final Map<String, ReleaseMotionLedger> OPEN = new HashMap<>();

    private final Set<UUID> settled = new HashSet<>();
    private final Set<UUID> deflected = new HashSet<>();
    private final Set<UUID> ejected = new HashSet<>();
    private final Map<UUID, UUID> controlledBy = new HashMap<>();
    private final Map<UUID, Set<UUID>> rootsByInstance = new HashMap<>();
    private final Map<UUID, Double> seatAngle = new HashMap<>();
    private final Map<UUID, Double> seatRing = new HashMap<>();

    private ReleaseMotionLedger() {
    }

    /** 确保释放组有账本。组或槽位无效时不建，不扣资源。 */
    public static void retain(DomainInstance domain) {
        if (domain == null || domain.releaseGroup() == null || domain.declarationSlot() < 0) {
            return;
        }
        OPEN.computeIfAbsent(key(domain), ignored -> new ReleaseMotionLedger());
    }

    /** 查找释放组账本。组或槽位无效时返回空。 */
    public static ReleaseMotionLedger find(DomainInstance domain) {
        if (domain == null || domain.releaseGroup() == null || domain.declarationSlot() < 0) {
            return null;
        }
        return OPEN.get(key(domain));
    }

    /** 同组没有其他活动领域时丢弃账本。仍有分体时保留控权。 */
    public static void dropIfIdle(DomainInstance domain) {
        if (domain == null || domain.releaseGroup() == null || domain.declarationSlot() < 0) {
            return;
        }
        String ledgerKey = key(domain);
        for (DomainInstance active : DomainService.active()) {
            if (active != domain && active.active() && ledgerKey.equals(key(active))) {
                return;
            }
        }
        ReleaseMotionLedger ledger = OPEN.remove(ledgerKey);
        if (ledger != null) {
            ledger.controlledBy.clear();
            ledger.rootsByInstance.clear();
            ledger.seatAngle.clear();
            ledger.seatRing.clear();
        }
    }

    private static String key(DomainInstance domain) {
        return domain.releaseGroup() + ":" + domain.declarationSlot();
    }

    public boolean settled(UUID entityId) {
        return entityId != null && settled.contains(entityId);
    }

    /** 记下实体已经完成入卷结算。空标识忽略，不重复收费。 */
    public void markSettled(UUID entityId) {
        if (entityId != null) {
            settled.add(entityId);
        }
    }

    public boolean deflected(UUID projectileId) {
        return projectileId != null && deflected.contains(projectileId);
    }

    /** 记下弹体已经偏转。空标识忽略，避免同一弹体重复收费。 */
    public void markDeflected(UUID projectileId) {
        if (projectileId != null) {
            deflected.add(projectileId);
        }
    }

    public boolean ejected(UUID entityId) {
        return entityId != null && ejected.contains(entityId);
    }

    /** 记下实体已经抛出，并释放它的控权和座位。空标识忽略。 */
    public void markEjected(UUID entityId) {
        if (entityId != null) {
            ejected.add(entityId);
            controlledBy.remove(entityId);
            forgetRoot(entityId);
        }
    }

    public boolean controlledByOther(UUID entityId, UUID instanceId) {
        UUID holder = controlledBy.get(entityId);
        return holder != null && !holder.equals(instanceId);
    }

    public boolean controls(UUID instanceId, UUID entityId) {
        return instanceId != null && instanceId.equals(controlledBy.get(entityId));
    }

    /** 认领实体控制权。任一标识为空时不记。 */
    public void claim(UUID instanceId, UUID entityId) {
        if (instanceId != null && entityId != null) {
            controlledBy.put(entityId, instanceId);
        }
    }

    /** 把根实体记到实例名下。任一标识为空时不记。 */
    public void claimRoot(UUID instanceId, UUID rootId) {
        if (instanceId != null && rootId != null) {
            rootsByInstance.computeIfAbsent(instanceId, ignored -> new HashSet<>()).add(rootId);
        }
    }

    public List<UUID> roots(UUID instanceId) {
        ArrayList<UUID> listed = new ArrayList<>();
        Set<UUID> stored = instanceId == null ? null : rootsByInstance.get(instanceId);
        if (stored != null) {
            listed.addAll(stored);
        }
        listed.sort(Comparator.naturalOrder());
        return listed;
    }

    public double seatAngle(UUID entityId) {
        Double stored = entityId == null ? null : seatAngle.get(entityId);
        return stored == null ? Double.NaN : stored;
    }

    public double seatRing(UUID entityId) {
        Double stored = entityId == null ? null : seatRing.get(entityId);
        return stored == null ? Double.NaN : stored;
    }

    /** 分配座位角和环半径。标识为空或数值非有限时不记。 */
    public void assignSeat(UUID entityId, double angle, double ring) {
        if (entityId != null && Double.isFinite(angle) && Double.isFinite(ring)) {
            seatAngle.put(entityId, angle);
            seatRing.put(entityId, ring);
        }
    }

    /** 释放一个实体的控权、座位和根登记。空标识忽略。 */
    public void releaseControl(UUID entityId) {
        if (entityId != null) {
            controlledBy.remove(entityId);
            forgetRoot(entityId);
        }
    }

    /** 释放一个实例的全部控权和座位。空实例忽略，不退水势。 */
    public void releaseInstance(UUID instanceId) {
        if (instanceId == null) {
            return;
        }
        controlledBy.entrySet().removeIf(entry -> instanceId.equals(entry.getValue()));
        Set<UUID> stored = rootsByInstance.remove(instanceId);
        if (stored != null) {
            for (UUID entityId : stored) {
                seatAngle.remove(entityId);
                seatRing.remove(entityId);
            }
        }
    }

    private void forgetRoot(UUID entityId) {
        seatAngle.remove(entityId);
        seatRing.remove(entityId);
        for (Set<UUID> stored : rootsByInstance.values()) {
            stored.remove(entityId);
        }
    }

    public boolean holdsAny(UUID instanceId) {
        return instanceId != null && controlledBy.containsValue(instanceId);
    }

    public Set<UUID> heldBy(UUID instanceId) {
        HashSet<UUID> held = new HashSet<>();
        if (instanceId == null) {
            return held;
        }
        for (Map.Entry<UUID, UUID> entry : controlledBy.entrySet()) {
            if (instanceId.equals(entry.getValue())) {
                held.add(entry.getKey());
            }
        }
        return held;
    }
}
