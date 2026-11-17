package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import com.bslx.XueXiMod.shu.technique.release.water.CastSource;

import java.util.Objects;

/**
 * 释放当时冻结的载体身份。
 * 内部保留既有 {@link CastSource} 的引用相等令牌，用来在出刀前比对；外面拿不到主手，也不能从这里读 NBT。
 */
public final class FrozenCarrierCredential {
    private final int skillSlot;
    private final int selectedSlot;
    private final int generation;
    private final Object token;

    public FrozenCarrierCredential(CastSource source) {
        if (source == null) {
            throw new IllegalArgumentException("载体凭据不完整");
        }
        this.skillSlot = source.skillSlot();
        this.selectedSlot = source.selectedSlot();
        this.generation = source.generation();
        this.token = source.stackRef();
    }

    public int skillSlot() {
        return skillSlot;
    }

    public int selectedSlot() {
        return selectedSlot;
    }

    public int generation() {
        return generation;
    }

    public boolean matches(CastSource source) {
        return source != null
                && source.skillSlot() == skillSlot
                && source.selectedSlot() == selectedSlot
                && source.generation() == generation
                && source.stackRef() == token;
    }

    public boolean sameIdentity(FrozenCarrierCredential other) {
        return other != null
                && other.skillSlot == skillSlot
                && other.selectedSlot == selectedSlot
                && other.generation == generation
                && other.token == token;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FrozenCarrierCredential credential)) {
            return false;
        }
        return sameIdentity(credential);
    }

    @Override
    public int hashCode() {
        return Objects.hash(skillSlot, selectedSlot, generation, System.identityHashCode(token));
    }

    @Override
    public String toString() {
        return "FrozenCarrierCredential[skillSlot=" + skillSlot
                + ", selectedSlot=" + selectedSlot
                + ", generation=" + generation + "]";
    }
}
