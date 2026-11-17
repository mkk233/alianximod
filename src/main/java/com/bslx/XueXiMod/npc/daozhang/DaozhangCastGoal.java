package com.bslx.XueXiMod.npc.daozhang;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** 看得到敌对玩家才起手。目标失效、离开射程或不再敌对就停。 */
public class DaozhangCastGoal extends Goal {
    private static final int RELEASE_TICK = 8;
    private static final int RECOVER_TICK = 16;
    private final DaozhangEntity daozhang;
    private ServerPlayer target;
    private int windup;
    private int casts;

    public DaozhangCastGoal(DaozhangEntity daozhang) {
        this.daozhang = daozhang;
        this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.daozhang.castReady()) {
            return false;
        }
        ServerPlayer hostile = this.daozhang.hostileTarget();
        if (hostile == null || !this.daozhang.hasLineOfSight(hostile)) {
            return false;
        }
        this.target = hostile;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null
                && this.windup < RECOVER_TICK
                && this.target.isAlive()
                && !this.target.isSpectator()
                && this.daozhang.relation(this.target.getUUID()) == DaozhangRelation.HOSTILE
                && this.daozhang.distanceTo(this.target) <= DaozhangConfig.castRange()
                && this.daozhang.hasLineOfSight(this.target);
    }

    @Override
    public void start() {
        this.windup = 0;
    }

    @Override
    public void stop() {
        this.target = null;
        this.windup = 0;
        this.daozhang.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.target == null) {
            return;
        }
        this.daozhang.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
        this.daozhang.getNavigation().moveTo(this.target, 0.75D);
        this.windup++;
        if (this.windup == RELEASE_TICK) {
            DaozhangSpellProjectile.Kind kind = this.casts++ % 2 == 0
                    ? DaozhangSpellProjectile.Kind.TALISMAN
                    : DaozhangSpellProjectile.Kind.FIRE;
            DaozhangSpellProjectile.shoot(this.daozhang, this.target, kind);
            this.daozhang.beginCast();
        }
    }
}
