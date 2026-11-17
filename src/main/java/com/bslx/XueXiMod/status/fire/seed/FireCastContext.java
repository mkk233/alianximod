package com.bslx.XueXiMod.status.fire.seed;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 一次逻辑施法的共享账本。复制体和延迟回调必须复用这个对象。
 * 去重记在对象里的目标 UUID 上，不保存实体，也不进全局表。
 * 施法者 UUID 可以为空。新建另一个上下文即使标识相同也是另一本账。
 */
public final class FireCastContext {
    private final UUID castId;
    private final UUID casterId;
    private final Set<UUID> producedTargets = new HashSet<>();
    private final Set<UUID> consumedTargets = new HashSet<>();
    /** 按目标和效果键去重。不写入 producedTargets 或 consumedTargets。 */
    private final Set<String> claimedEffects = new HashSet<>();

    public FireCastContext(UUID castId, UUID casterId) {
        if (castId == null) {
            throw new IllegalArgumentException("castId");
        }
        this.castId = castId;
        this.casterId = casterId;
    }

    public UUID castId() {
        return this.castId;
    }

    public UUID casterId() {
        return this.casterId;
    }

    boolean hasProduced(UUID targetId) {
        return targetId != null && this.producedTargets.contains(targetId);
    }

    boolean hasConsumed(UUID targetId) {
        return targetId != null && this.consumedTargets.contains(targetId);
    }

    void markProduced(UUID targetId) {
        if (targetId == null) {
            throw new IllegalArgumentException("targetId");
        }
        this.producedTargets.add(targetId);
    }

    void markConsumed(UUID targetId) {
        if (targetId == null) {
            throw new IllegalArgumentException("targetId");
        }
        this.consumedTargets.add(targetId);
    }

    public boolean effectClaimed(UUID targetId, String effectKey) {
        if (targetId == null || effectKey == null) {
            return false;
        }
        return this.claimedEffects.contains(effectToken(targetId, effectKey));
    }

    /** 第一次声明返回 true。不影响旧的生产标记和消费标记。 */
    public boolean claimEffect(UUID targetId, String effectKey) {
        if (targetId == null || effectKey == null || effectKey.isBlank()) {
            return false;
        }
        return this.claimedEffects.add(effectToken(targetId, effectKey));
    }

    private static String effectToken(UUID targetId, String effectKey) {
        return targetId.toString() + "|" + effectKey;
    }
}
