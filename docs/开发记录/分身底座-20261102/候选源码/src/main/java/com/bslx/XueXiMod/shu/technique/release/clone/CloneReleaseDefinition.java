package com.bslx.XueXiMod.shu.technique.release.clone;

import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.ReleaseTag;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;

/**
 * 三个开发动作的释放定义。这里的存活刻度只为满足接口，分身寿命另读规格。
 */
public final class CloneReleaseDefinition implements ReleaseDefinition {
    private final String techniqueId;
    private final ChainRole chainRole;
    private final int manaCost;

    private CloneReleaseDefinition(String techniqueId, ChainRole chainRole, int manaCost) {
        this.techniqueId = techniqueId;
        this.chainRole = chainRole;
        this.manaCost = manaCost;
    }

    public static void registerAll() {
        ReleaseRegistry.register(new CloneReleaseDefinition(
                CloneBalance.PHANTOM_ID, ChainRole.ACTION, CloneBalance.SPAWN_MANA_COST));
        ReleaseRegistry.register(new CloneReleaseDefinition(
                CloneBalance.BODY_ID, ChainRole.ACTION, CloneBalance.SPAWN_MANA_COST));
        ReleaseRegistry.register(new CloneReleaseDefinition(
                CloneBalance.PULSE_ID, ChainRole.ATTACK, CloneBalance.PULSE_MANA_COST));
    }

    @Override
    public String id() {
        return techniqueId;
    }

    @Override
    public ReleaseKind kind() {
        return ReleaseKind.ACTION;
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
        return 0F;
    }

    @Override
    public float thickness() {
        return 0F;
    }

    @Override
    public ChainRole role() {
        return chainRole;
    }

    @Override
    public int explicitManaCost() {
        return manaCost;
    }
}
