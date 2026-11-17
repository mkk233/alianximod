package com.bslx.XueXiMod.shu.technique.release.relations;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Team;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

/** 只判断关系。盟友使用原版已有队伍，没有队伍就不是盟友。 */
public final class Relations {
    private Relations() {
    }

    /** 移动者就是主人时放行。 */
    public static boolean permits(UUID ownerId, Entity mover) {
        return ownerId != null && mover != null && ownerId.equals(mover.getUUID());
    }

    /** 双方都有队伍且原版队伍互相视为盟友。主人自己不算盟友，宠物和非怪物也不因此成为盟友。 */
    public static boolean ally(UUID ownerId, Entity mover) {
        if (ownerId == null || mover == null || ownerId.equals(mover.getUUID())) {
            return false;
        }
        Team moverTeam = mover.getTeam();
        if (moverTeam == null || mover.level() == null) {
            return false;
        }
        if (!(mover.level() instanceof ServerLevel level)) {
            return false;
        }
        Entity owner = level.getEntity(ownerId);
        if (owner == null) {
            return false;
        }
        Team ownerTeam = owner.getTeam();
        return ownerTeam != null && ownerTeam.isAlliedTo(moverTeam);
    }

    /** 原版队伍盟友，或道长与其徒弟。没有显式关系时不把中立单位算成盟友。 */
    public static boolean allied(UUID ownerId, Entity mover) {
        return ally(ownerId, mover) || HarmPolicy.allied(ownerId, mover);
    }
}
