package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import java.util.UUID;

/** 已落下领域的身份。不保存领域实例、世界或“当时允许攻击”的布尔值。 */
public record DomainLink(UUID instanceId, String techniqueId, UUID ownerId) {
    public DomainLink {
        if (instanceId == null || ownerId == null || techniqueId == null || techniqueId.isBlank()) {
            throw new IllegalArgumentException("领域链接不完整");
        }
    }
}
