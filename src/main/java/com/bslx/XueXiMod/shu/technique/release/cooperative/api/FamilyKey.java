package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * 一次攻击族的身份。合同里的 familyId 就是这一整组字段，不是另造的实体 UUID。
 * {@code serverSession} 是协同执行会话，不等于分身运行时的 {@code long} sessionToken。
 */
public record FamilyKey(UUID serverSession, ResourceKey<Level> dimension, UUID ownerId, UUID castId, long epoch,
                        int familyOrdinal) {
    public FamilyKey {
        if (serverSession == null || dimension == null || ownerId == null || castId == null) {
            throw new IllegalArgumentException("攻击族身份不完整");
        }
        if (familyOrdinal < 0) {
            throw new IllegalArgumentException("攻击族序号不能为负");
        }
    }
}
