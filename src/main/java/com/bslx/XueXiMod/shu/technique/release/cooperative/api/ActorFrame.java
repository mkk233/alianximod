package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 出刀时的一名演员。玩家序号是 {@link CooperativeStructureLimits#OWNER_ACTOR_ORDINAL}，
 * 协同分身是 {@link CooperativeStructureLimits#COOPERATING_ACTOR_ORDINAL}。
 * 这不是脱离身体的捕获帧，也不保存玩家或分身实体引用。
 */
public record ActorFrame(UUID actorId, int actorOrdinal, Vec3 feet, Vec3 eye, Vec3 aim, float visualYaw,
                         boolean mirrored) {
    public ActorFrame {
        if (actorId == null) {
            throw new IllegalArgumentException("演员缺少身份");
        }
        if (actorOrdinal < 0) {
            throw new IllegalArgumentException("演员序号不能为负");
        }
        feet = FrozenValues.finite(feet, "演员脚点");
        eye = FrozenValues.finite(eye, "演员眼点");
        aim = FrozenValues.unit(aim, "演员瞄准");
        if (!Float.isFinite(visualYaw)) {
            throw new IllegalArgumentException("演员表现朝向必须有限");
        }
    }
}
