package com.bslx.XueXiMod.shu.technique.release.field;

import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactEnchantState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** 钉在世界坐标上的场地。索敌和弹道只记在上面，不挪生成点。 */
final class FieldZone {
    final UUID ownerId;
    final ResourceKey<Level> dimension;
    final Vec3 anchor;
    final double radius;
    final int expireTick;
    int nextUpkeepTick;
    final int upkeepCost;
    final int upkeepPeriod;
    final Trajectory attachedPath;
    final boolean attachedLock;
    final boolean attachedTracking;
    final float healAmount;
    final float pullStrength;
    final int wetnessGrant;
    final boolean purify;
    final float pressurePulse;
    final float erosionPulse;
    final float unloadRatio;
    final int slowTicks;
    final float endBurstDamage;
    boolean closed;
    ContactEnchantState contact;
    int bornTick;
    ServerLevel serverLevel;
    final UUID hostId = UUID.randomUUID();

    FieldZone(UUID ownerId, ResourceKey<Level> dimension, Vec3 anchor, double radius,
              int expireTick, int nextUpkeepTick, int upkeepCost, int upkeepPeriod,
              Trajectory attachedPath, boolean attachedLock, boolean attachedTracking, float healAmount, float pullStrength,
              int wetnessGrant, boolean purify, float pressurePulse, float erosionPulse,
              float unloadRatio, int slowTicks, float endBurstDamage) {
        this.ownerId = ownerId;
        this.dimension = dimension;
        this.anchor = anchor;
        this.radius = radius;
        this.expireTick = expireTick;
        this.nextUpkeepTick = nextUpkeepTick;
        this.upkeepCost = upkeepCost;
        this.upkeepPeriod = upkeepPeriod;
        this.attachedPath = attachedPath;
        this.attachedLock = attachedLock;
        this.attachedTracking = attachedTracking;
        this.healAmount = healAmount;
        this.pullStrength = pullStrength;
        this.wetnessGrant = wetnessGrant;
        this.purify = purify;
        this.pressurePulse = pressurePulse;
        this.erosionPulse = erosionPulse;
        this.unloadRatio = unloadRatio;
        this.slowTicks = slowTicks;
        this.endBurstDamage = endBurstDamage;
    }
}
