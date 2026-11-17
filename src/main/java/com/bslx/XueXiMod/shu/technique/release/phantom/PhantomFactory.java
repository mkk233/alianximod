package com.bslx.XueXiMod.shu.technique.release.phantom;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** 生成走现有命中体。不代理伤害，也不做分身。 */
public final class PhantomFactory {
    private PhantomFactory() {
    }

    public static PhantomHandle spawn(ServerLevel level, ServerPlayer caster, TechniqueHitSpec spec) {
        if (level == null || caster == null || spec == null) {
            return Dormant.INSTANCE;
        }
        TechniqueHitSpec.spawn(level, caster, spec);
        return Dormant.INSTANCE;
    }

    private static final class Dormant implements PhantomHandle {
        static final Dormant INSTANCE = new Dormant();

        @Override
        public boolean active() {
            return false;
        }

        @Override
        public void end(PhantomEndReason reason) {
        }
    }
}
