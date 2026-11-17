package com.bslx.XueXiMod.shu.technique.release.cooperative;

import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneEntity;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CancelReason;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.FamilyKey;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.SettlementGuard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** 只挂在本批账本上的结算守卫。每次记录和刷新都重查会话、世界、主人和目标权限。 */
public final class CooperativeSettlementGuard implements SettlementGuard {
    private final UUID ownerId;
    private final FamilyKey family;
    private final AtomicBoolean accepting = new AtomicBoolean(true);
    private final AtomicBoolean hardStopped = new AtomicBoolean(false);

    public CooperativeSettlementGuard(UUID ownerId, FamilyKey family) {
        if (ownerId == null || family == null) {
            throw new IllegalArgumentException("结算守卫缺少主人或攻击族");
        }
        this.ownerId = ownerId;
        this.family = family;
    }

    @Override
    public boolean active(ServerLevel level) {
        if (!accepting.get() || hardStopped.get() || level == null || level.getServer() == null) {
            return false;
        }
        if (!family.dimension().equals(level.dimension())) {
            return false;
        }
        UUID session = CooperativeRuntime.sessions().sessionOf(level.getServer());
        if (session == null || !session.equals(family.serverSession())) {
            return false;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        return owner != null && owner.isAlive() && !owner.isRemoved() && !owner.hasDisconnected()
                && owner.level() == level;
    }

    @Override
    public boolean allows(ServerLevel level, LivingEntity target) {
        if (!active(level) || target == null) {
            return false;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        return owner != null && !CloneEntity.rejectsPulseTarget(owner, target);
    }

    @Override
    public void cancel(CancelReason reason) {
        if (reason == CancelReason.CARRIER_INVALID) {
            return;
        }
        if (reason != null && reason.hard()) {
            hardStopped.set(true);
        }
        accepting.set(false);
    }
}
