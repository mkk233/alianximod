package com.bslx.XueXiMod.shu.technique.release.contact;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/** 零伤害宿主的真实接触面。墙、场地和护体各自实现，不另造第二套攻击区域。 */
public interface ContactCarrier {
    UUID hostId();

    int revision();

    UUID ownerId();

    boolean active();

    boolean touches(LivingEntity target);

    ServerLevel hostLevel();

    /** 粗筛用的范围。窄判仍走 touches。 */
    AABB queryBox();
}
