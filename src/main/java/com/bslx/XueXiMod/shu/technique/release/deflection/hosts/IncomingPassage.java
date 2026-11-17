package com.bslx.XueXiMod.shu.technique.release.deflection.hosts;

import com.bslx.XueXiMod.shu.technique.release.relations.Relations;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.UUID;

/**
 * 主人和盟友来弹放行。只转调已有 {@link Relations}，不另建盟友存档。
 * 线段查询没有弹体实体，所以放行发生在弹体批收购之前，不写进几何。
 */
public final class IncomingPassage {
    private IncomingPassage() {
    }

    /** 移动者本人或弹体射手是主人或盟友时放行。 */
    public static boolean letsPass(UUID ownerId, Entity mover) {
        if (Relations.permits(ownerId, mover) || Relations.ally(ownerId, mover)) {
            return true;
        }
        if (mover instanceof Projectile projectile) {
            Entity shooter = projectile.getOwner();
            return Relations.permits(ownerId, shooter) || Relations.ally(ownerId, shooter);
        }
        return false;
    }
}
