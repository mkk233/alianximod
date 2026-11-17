package com.bslx.XueXiMod.shu.technique.release.selfstatus;

import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ChainCompatibility;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.ReleaseTag;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.status.fire.furnace.FurnaceMath;
import net.minecraft.server.level.ServerPlayer;

/** 炉心三式共用的自身状态定义。链上角色是动作，显式属于真实自身状态，不生成命中体。 */
public final class SelfStatusDefinition implements ReleaseDefinition, SelfStatusCast {
    private final String releaseId;
    private final int manaCost;
    private final FurnaceSelfStatusCast furnaceCast;

    private SelfStatusDefinition(String releaseId, int manaCost, FurnaceSelfStatusCast furnaceCast) {
        this.releaseId = releaseId;
        this.manaCost = manaCost;
        this.furnaceCast = furnaceCast;
    }

    public static void registerAll() {
        ReleaseRegistry.register(new SelfStatusDefinition("fire_heat_up", 20, FurnaceSelfStatusCast.enterNormal()));
        ReleaseRegistry.register(new SelfStatusDefinition("fire_burn", 20, FurnaceSelfStatusCast.enterOverdrive()));
        ReleaseRegistry.register(new SelfStatusDefinition("fire_cool", 0, FurnaceSelfStatusCast.cool()));
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
    public ChainCompatibility chainCompatibility() {
        return ChainCompatibility.SELF_STATE;
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
    public StatusPreparation prepare(ServerPlayer player) {
        return this.furnaceCast.prepare(player);
    }

    @Override
    public FurnaceMath.Action action() {
        return this.furnaceCast.action();
    }

    /**
     * 本类是 final，没有未知子类可改写 getter。字段在构造时写完，getter 只读这些配置。
     */
    @Override
    public boolean supportsStructuralPlanCache() {
        return true;
    }
}
