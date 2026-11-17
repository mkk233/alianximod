package com.bslx.XueXiMod.shu.technique;

import com.bslx.XueXiMod.shu.technique.release.relations.Relations;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** 动作报告合法接触和窗口结束。调用方不猜固定结束 tick。 */
public final class ActionWindow {
    private ActionWindow() {
    }

    public interface Sink {
        void onContact(UUID targetId, Vec3 point, Vec3 direction);

        void onEnded();

        /** 窗口真正结束。默认什么都不做，原有动作保持原样。 */
        default void onFinished() {
        }

        boolean closed();
    }

    /** 接收组时再确认一次。盟友仍始终为否，不从宠物推断。 */
    public static boolean hostileTo(ServerPlayer owner, LivingEntity target) {
        if (owner == null || target == null || target == owner || !target.isAlive() || target.isSpectator()) {
            return false;
        }
        UUID ownerId = owner.getUUID();
        if (Relations.permits(ownerId, target) || Relations.ally(ownerId, target)) {
            return false;
        }
        return true;
    }
}
