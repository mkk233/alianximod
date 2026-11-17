package com.bslx.XueXiMod.carrier;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;

import java.util.UUID;

/** 伤害结算用的在线主人。假人和旁观者不在这里顶上。 */
public final class BatchOwners {
    private BatchOwners() {
    }

    public static ServerPlayer online(ServerLevel level, UUID ownerIdentifier) {
        if (level == null || ownerIdentifier == null || level.getServer() == null) {
            return null;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerIdentifier);
        if (owner == null || owner.isRemoved() || !owner.isAlive() || owner instanceof FakePlayer) {
            return null;
        }
        if (owner.serverLevel() != level) {
            return null;
        }
        return owner;
    }
}
