package com.bslx.XueXiMod.shu.technique.release.fire;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/** 按保存的 UUID 找在线施法者。对不上就不返回，避免把伤害记到别的玩家头上。 */
public final class OnlineCaster {
    private OnlineCaster() {
    }

    public static ServerPlayer find(ServerLevel serverLevel, UUID casterId) {
        if (serverLevel == null || casterId == null) {
            return null;
        }
        ServerPlayer caster = serverLevel.getServer().getPlayerList().getPlayer(casterId);
        if (caster == null || !casterId.equals(caster.getUUID())) {
            return null;
        }
        return caster;
    }

    public static ServerPlayer find(LivingEntity host, UUID casterId) {
        if (host == null || !(host.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return find(serverLevel, casterId);
    }
}
