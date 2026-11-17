package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 客户端动作播放所需的全部数据。成员为空表示族级动作，例如回旋开始。
 * 玩家身体的世代使用 {@link CooperativeStructureLimits#OWNER_BODY_GENERATION}。
 * 武器只放显示快照。服务端作者不引用客户端类。
 */
public record ActionPlayback(UUID serverSession, ResourceKey<Level> dimension, FamilyKey family,
                             @Nullable MemberKey member, UUID actorId, int entityId, long actorGeneration,
                             long sequence, ResourceLocation actionId, long serverStartTick, int windupTicks,
                             int recoveryTicks, float visualYaw, boolean mirrored,
                             @Nullable WeaponDisplaySnapshot weapon) {
    public ActionPlayback {
        if (serverSession == null || dimension == null || family == null || actorId == null || actionId == null) {
            throw new IllegalArgumentException("动作播放不完整");
        }
        if (!serverSession.equals(family.serverSession()) || !dimension.equals(family.dimension())) {
            throw new IllegalArgumentException("动作播放的会话或世界和攻击族不一致");
        }
        if (member != null && !member.family().equals(family)) {
            throw new IllegalArgumentException("动作播放的成员不属于这个攻击族");
        }
        if (entityId < 0 || sequence < 1L) {
            throw new IllegalArgumentException("动作播放的实体或序号无效");
        }
        if (windupTicks < 0 || windupTicks > CooperativeStructureLimits.WINDUP_HIGH
                || recoveryTicks < CooperativeStructureLimits.RECOVERY_LOW
                || recoveryTicks > CooperativeStructureLimits.RECOVERY_HIGH) {
            throw new IllegalArgumentException("动作播放的时长超出结构范围");
        }
        if (!Float.isFinite(visualYaw)) {
            throw new IllegalArgumentException("动作播放的朝向必须有限");
        }
    }
}
