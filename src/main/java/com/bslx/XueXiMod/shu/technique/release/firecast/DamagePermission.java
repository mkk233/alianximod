package com.bslx.XueXiMod.shu.technique.release.firecast;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import com.bslx.XueXiMod.shu.technique.release.relations.HarmPolicy;

/** 能不能打到这个目标。排除自己、旁观、无敌，以及原版不允许伤害的玩家。 */
public final class DamagePermission {
    private DamagePermission() {
    }

    public static boolean allows(ServerPlayer caster, LivingEntity target) {
        if (caster == null || target == null || !target.isAlive() || target.isSpectator() || target.isRemoved()) {
            return false;
        }
        if (target == caster || target.getUUID().equals(caster.getUUID())) {
            return false;
        }
        if (target.isInvulnerable()) {
            return false;
        }
        if (target instanceof Player playerTarget) {
            return caster.canHarmPlayer(playerTarget);
        }
        if (HarmPolicy.denies(caster, target)) {
            return false;
        }
        return true;
    }
}
