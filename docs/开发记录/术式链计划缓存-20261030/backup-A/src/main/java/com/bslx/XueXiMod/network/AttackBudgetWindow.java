package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.ActionWindow;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.release.momentum.ChargeGate;
import com.bslx.XueXiMod.shu.technique.release.water.CastSource;
import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/** 动作窗口上的攻击预算。窗口结束、人或会话失效时只退还没取走的份额。 */
final class AttackBudgetWindow implements ActionWindow.Sink {
    private final ServerPlayer player;
    private final ServerLevel castLevel;
    private final int castEpoch;
    private final UncommittedShare.Pool shares;
    private final Skill skill;
    private final List<Technique> techniques;
    private final List<ChainSegment> segments;
    private final SkillChainPlan.ModifierPlan plan;
    private final float weaponBonus;
    private final CastSource castSource;
    private boolean finished;

    private AttackBudgetWindow(ServerPlayer player, ServerLevel castLevel, int castEpoch, UncommittedShare.Pool shares,
                               Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                               SkillChainPlan.ModifierPlan plan, float weaponBonus, CastSource castSource) {
        this.player = player;
        this.castLevel = castLevel;
        this.castEpoch = castEpoch;
        this.shares = shares;
        this.skill = skill;
        this.techniques = techniques;
        this.segments = segments;
        this.plan = plan;
        this.weaponBonus = weaponBonus;
        this.castSource = castSource;
    }

    static AttackBudgetWindow open(ServerPlayer player, ServerLevel castLevel, int castEpoch, UncommittedShare.Pool shares,
                                   Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                                   SkillChainPlan.ModifierPlan plan, float weaponBonus, CastSource castSource) {
        return new AttackBudgetWindow(player, castLevel, castEpoch, shares, skill, techniques, segments, plan, weaponBonus, castSource);
    }

    boolean ownerPresent() {
        return player != null && player.isAlive() && !player.isRemoved() && !player.hasDisconnected()
                && player.level() == castLevel && ChargeGate.same(player.getUUID(), castEpoch);
    }

    void finish() {
        if (finished) {
            return;
        }
        finished = true;
        if (shares != null) {
            shares.close(player);
        }
    }

    @Override
    public void onContact(UUID targetId, Vec3 point, Vec3 direction) {
        if (closed() || player.getServer() == null) {
            return;
        }
        SkillChainPlan.firePaidPlan(player, skill, techniques, segments, plan, weaponBonus,
                player.getServer().getTickCount(), shares, castSource);
    }

    @Override
    public void onEnded() {
        finish();
    }

    @Override
    public void onFinished() {
        finish();
    }

    @Override
    public boolean closed() {
        return finished || !ownerPresent();
    }
}
