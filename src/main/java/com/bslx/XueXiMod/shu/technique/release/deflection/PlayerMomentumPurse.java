package com.bslx.XueXiMod.shu.technique.release.deflection;

import com.bslx.XueXiMod.shu.technique.release.momentum.WaterMomentum;
import net.minecraft.server.level.ServerPlayer;

/** 生产账本。可用额、预留、提交和退回都走现有水势账。 */
final class PlayerMomentumPurse implements MomentumPurse {
    private final ServerPlayer player;

    PlayerMomentumPurse(ServerPlayer player) {
        this.player = player;
    }

    /** 玩家当前可用水势。 */
    @Override
    public float available() {
        return WaterMomentum.available(this.player);
    }

    /** 预留这笔水势。拒绝时不留预留。 */
    @Override
    public Object reserve(float amount) {
        WaterMomentum.Hold hold = WaterMomentum.reserve(this.player, amount);
        if (hold == null || !(amount > 0f) || !hold.accepted()) {
            WaterMomentum.release(hold);
            return null;
        }
        return hold;
    }

    /** 提交仍有效的预留。 */
    @Override
    public boolean commit(Object reservation) {
        if (!(reservation instanceof WaterMomentum.Hold hold)) {
            return false;
        }
        WaterMomentum.commit(this.player, hold);
        return true;
    }

    /** 退回还没提交的预留。 */
    @Override
    public void release(Object reservation) {
        if (reservation instanceof WaterMomentum.Hold hold) {
            WaterMomentum.release(hold);
        }
    }
}
