package com.bslx.XueXiMod.shu.technique.release.relations;

import com.bslx.XueXiMod.npc.daozhang.DaozhangEntity;
import com.bslx.XueXiMod.npc.daozhang.DaozhangRelation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 道长与玩家的显式关系优先。
 * 没有道长关系时，不把普通中立单位改成友方。
 */
public final class HarmPolicy {
    private HarmPolicy() {
    }

    public static boolean denies(Entity actor, LivingEntity target) {
        return denies(CombatOwnership.playerOwner(actor), target);
    }

    public static boolean denies(@Nullable UUID playerId, LivingEntity target) {
        if (playerId == null || !(target instanceof DaozhangEntity daozhang)) {
            return false;
        }
        return daozhang.relation(playerId) == DaozhangRelation.APPRENTICE;
    }

    public static boolean allied(UUID ownerId, Entity other) {
        if (ownerId == null || other == null) {
            return false;
        }
        if (other instanceof DaozhangEntity daozhang) {
            return daozhang.relation(ownerId) == DaozhangRelation.APPRENTICE;
        }
        if (other.level() instanceof ServerLevel level) {
            Entity owner = level.getEntity(ownerId);
            if (owner instanceof DaozhangEntity daozhang) {
                UUID playerId = CombatOwnership.playerOwner(other);
                return playerId != null && daozhang.relation(playerId) == DaozhangRelation.APPRENTICE;
            }
        }
        return false;
    }
}
