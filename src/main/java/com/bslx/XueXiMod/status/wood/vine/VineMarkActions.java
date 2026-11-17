package com.bslx.XueXiMod.status.wood.vine;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/** 一次独立攻击段的身份。复制份和多次接触共用，不随枝印清空。 */
public final class VineMarkActions {
    public enum Source {
        MELEE,
        SWORD
    }

    public record Action(UUID casterId, UUID actionId, Source source, boolean spectrumEligible) {
        public Action(UUID casterId, UUID actionId, Source source) {
            this(casterId, actionId, source, false);
        }
    }

    private static final ThreadLocal<Action> OPEN = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> MELEE_SWEEP = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<UUID> SWORD_CASTER = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SWORD_SPECTRUM = ThreadLocal.withInitial(() -> false);

    private VineMarkActions() {
    }

    public static Action openMelee(ServerPlayer caster) {
        Action action = new Action(caster.getUUID(), UUID.randomUUID(), Source.MELEE,
                VineMarkSpectrum.active(caster));
        open(action);
        MELEE_SWEEP.set(false);
        return action;
    }

    public static void open(Action action) {
        if (action == null) {
            OPEN.remove();
            return;
        }
        OPEN.set(action);
    }

    public static void close(Action action) {
        if (OPEN.get() == action) {
            OPEN.remove();
            MELEE_SWEEP.remove();
        }
    }

    public static Action current() {
        return OPEN.get();
    }

    public static void armSword(ServerPlayer caster) {
        if (caster != null) {
            SWORD_CASTER.set(caster.getUUID());
            SWORD_SPECTRUM.set(VineMarkSpectrum.active(caster));
        }
    }

    public static void disarmSword() {
        SWORD_CASTER.remove();
        SWORD_SPECTRUM.remove();
    }

    public static boolean swordArmed(ServerPlayer caster) {
        return caster != null && caster.getUUID().equals(SWORD_CASTER.get());
    }

    public static boolean swordSpectrum() {
        return Boolean.TRUE.equals(SWORD_SPECTRUM.get());
    }

    public static Action frozenSword(ServerPlayer caster, long tick, long ordinal) {
        if (!swordArmed(caster)) {
            return null;
        }
        return new Action(caster.getUUID(),
                new UUID(caster.getUUID().getMostSignificantBits() ^ tick,
                        caster.getUUID().getLeastSignificantBits() ^ ordinal),
                Source.SWORD, swordSpectrum());
    }

    public static void notePrimary(LivingEntity target) {
        Action action = OPEN.get();
        if (action != null && action.source == Source.MELEE && target != null) {
            MELEE_SWEEP.set(true);
        }
    }

    public static boolean sweepArmed() {
        Action action = OPEN.get();
        return action != null && action.source == Source.MELEE && Boolean.TRUE.equals(MELEE_SWEEP.get());
    }
}
