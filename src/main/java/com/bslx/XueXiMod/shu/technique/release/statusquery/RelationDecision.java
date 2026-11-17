package com.bslx.XueXiMod.shu.technique.release.statusquery;

import com.bslx.XueXiMod.shu.technique.release.relations.Relations;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/** 关系分类与纯判定。盟友只走现有预留，不另建社交名单。 */
public final class RelationDecision {
    private RelationDecision() {
    }

    public enum SubjectKind {
        SELF,
        ALLY,
        OWNED,
        PLAYER,
        NEUTRAL,
        ENEMY,
        OTHER
    }

    public static boolean accepts(SubjectKind kind, boolean angryAtCaster, boolean canHarmPlayer) {
        if (kind == null) {
            return false;
        }
        return switch (kind) {
            case SELF, ALLY, OWNED, OTHER -> false;
            case PLAYER -> canHarmPlayer;
            case NEUTRAL -> angryAtCaster;
            case ENEMY -> true;
        };
    }

    public static SubjectKind classify(
            boolean sameCreature,
            boolean allied,
            boolean ownedByCaster,
            boolean player,
            boolean neutralMob,
            boolean enemyOrMonster) {
        if (sameCreature) {
            return SubjectKind.SELF;
        }
        if (allied) {
            return SubjectKind.ALLY;
        }
        if (ownedByCaster) {
            return SubjectKind.OWNED;
        }
        if (player) {
            return SubjectKind.PLAYER;
        }
        if (neutralMob) {
            return SubjectKind.NEUTRAL;
        }
        if (enemyOrMonster) {
            return SubjectKind.ENEMY;
        }
        return SubjectKind.OTHER;
    }

    public static SubjectKind classify(ServerPlayer caster, LivingEntity subject) {
        if (caster == null || subject == null) {
            return SubjectKind.OTHER;
        }
        boolean sameCreature = subject == caster || subject.getUUID().equals(caster.getUUID());
        boolean allied = Relations.ally(caster.getUUID(), subject);
        boolean owned = casterOwns(caster, subject);
        boolean player = subject instanceof Player;
        boolean neutralMob = subject instanceof NeutralMob;
        boolean enemyOrMonster = subject instanceof Enemy || subject instanceof Monster;
        return classify(sameCreature, allied, owned, player, neutralMob, enemyOrMonster);
    }

    /** 中立正在攻击：愤怒接口命中，或生物当前目标就是施术者。 */
    public static boolean angryAtCaster(LivingEntity subject, ServerPlayer caster) {
        if (subject == null || caster == null) {
            return false;
        }
        if (subject instanceof NeutralMob neutral && neutral.isAngryAt(caster)) {
            return true;
        }
        return subject instanceof Mob creature && creature.getTarget() == caster;
    }

    private static boolean casterOwns(ServerPlayer caster, LivingEntity subject) {
        if (!(subject instanceof TamableAnimal tamable)) {
            return false;
        }
        UUID ownerId = tamable.getOwnerUUID();
        return ownerId != null && ownerId.equals(caster.getUUID());
    }
}
