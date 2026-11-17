package com.bslx.XueXiMod.shu.technique.release.selfstatus;

import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.ReleaseTag;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.status.fire.furnace.FurnaceMath;

/** 炉心三式共用的自身状态定义。链上角色是动作，拒绝混链，不生成命中体。 */
public final class SelfStatusDefinition implements ReleaseDefinition, SelfStatusCast {
    private final String releaseId;
    private final int manaCost;
    private final FurnaceMath.Action furnaceAction;

    private SelfStatusDefinition(String releaseId, int manaCost, FurnaceMath.Action furnaceAction) {
        this.releaseId = releaseId;
        this.manaCost = manaCost;
        this.furnaceAction = furnaceAction;
    }

    public static void registerAll() {
        ReleaseRegistry.register(new SelfStatusDefinition("fire_heat_up", 20, FurnaceMath.Action.ENTER_NORMAL));
        ReleaseRegistry.register(new SelfStatusDefinition("fire_burn", 20, FurnaceMath.Action.ENTER_OVERDRIVE));
        ReleaseRegistry.register(new SelfStatusDefinition("fire_cool", 0, FurnaceMath.Action.COOL));
    }

    @Override
    public String id() {
        return this.releaseId;
    }

    @Override
    public ReleaseKind kind() {
        return ReleaseKind.SELF_STATUS;
    }

    @Override
    public ReleaseTag tag() {
        return ReleaseTag.TRIGGER;
    }

    @Override
    public Trajectory trajectory() {
        return Trajectory.STRAIGHT;
    }

    @Override
    public boolean moving() {
        return false;
    }

    @Override
    public int lifetimeTicks() {
        return 1;
    }

    @Override
    public float speed() {
        return 0f;
    }

    @Override
    public float thickness() {
        return 0f;
    }

    @Override
    public ChainRole role() {
        return ChainRole.ACTION;
    }

    @Override
    public boolean trajectoryMutable() {
        return false;
    }

    @Override
    public boolean refusesMixedChain() {
        return true;
    }

    @Override
    public int explicitManaCost() {
        return this.manaCost;
    }

    @Override
    public int explicitAssemblyCost() {
        return 1;
    }

    @Override
    public boolean requiresFreshPress() {
        return true;
    }

    @Override
    public SelfStatusCast selfStatus() {
        return this;
    }

    @Override
    public FurnaceMath.Action action() {
        return this.furnaceAction;
    }
}
