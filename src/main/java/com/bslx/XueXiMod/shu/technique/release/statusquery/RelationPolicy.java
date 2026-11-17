package com.bslx.XueXiMod.shu.technique.release.statusquery;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** 可复用的关系策略。群体捕获使用同一套分类，再套预设纯函数。 */
public final class RelationPolicy {
    private static final RelationPolicy HOSTILE_CAPTURE = new RelationPolicy(RelationDecision::accepts);

    private final Gate gate;

    private RelationPolicy(Gate gate) {
        this.gate = gate;
    }

    public static RelationPolicy hostileCapture() {
        return HOSTILE_CAPTURE;
    }

    public static RelationPolicy using(Gate gate) {
        if (gate == null) {
            throw new IllegalArgumentException("gate");
        }
        return new RelationPolicy(gate);
    }

    public boolean accepts(ServerPlayer caster, LivingEntity subject) {
        if (caster == null || subject == null || !subject.isAlive() || subject.isSpectator() || subject.isRemoved()) {
            return false;
        }
        if (subject == caster || subject.getUUID().equals(caster.getUUID())) {
            return false;
        }
        RelationDecision.SubjectKind kind = RelationDecision.classify(caster, subject);
        boolean angryAtCaster = RelationDecision.angryAtCaster(subject, caster);
        boolean canHarmPlayer = false;
        if (kind == RelationDecision.SubjectKind.PLAYER && subject instanceof Player targetPlayer) {
            canHarmPlayer = caster.canHarmPlayer(targetPlayer);
        }
        return decide(kind, angryAtCaster, canHarmPlayer);
    }

    boolean decide(RelationDecision.SubjectKind kind, boolean angryAtCaster, boolean canHarmPlayer) {
        return this.gate.accepts(kind, angryAtCaster, canHarmPlayer);
    }

    @FunctionalInterface
    public interface Gate {
        boolean accepts(RelationDecision.SubjectKind kind, boolean angryAtCaster, boolean canHarmPlayer);
    }
}
