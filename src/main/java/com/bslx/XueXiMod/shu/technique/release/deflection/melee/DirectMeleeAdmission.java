package com.bslx.XueXiMod.shu.technique.release.deflection.melee;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

/**
 * 直接怪物近战才进入补充入口。
 * 只认 mob_attack 和 sting，并且直接实体就是来源实体。
 * 术式的 player_attack、箭、魔法、环境和守卫者射线都不进。
 * 这里发生在原版无敌帧判断之前，不能把通过本方法当成已经过了无敌帧。
 */
public final class DirectMeleeAdmission {
    private DirectMeleeAdmission() {
    }

    public record DirectMeleeFacts(
            boolean serverSide,
            boolean canceled,
            float amount,
            boolean victimViable,
            boolean sameActor,
            boolean mobActor,
            boolean distinctPair,
            boolean physicalKind,
            boolean beamExcluded) {
        /** 不能进入补充入口的空事实。 */
        public static DirectMeleeFacts rejected() {
            return new DirectMeleeFacts(false, false, 0f, false, false, false, false, false, false);
        }
    }

    /** 事实全部满足时才允许补充入口继续。 */
    public static boolean admits(DirectMeleeFacts facts) {
        if (facts == null || facts.canceled() || !facts.serverSide() || !facts.victimViable()) {
            return false;
        }
        float amount = facts.amount();
        if (!Float.isFinite(amount) || amount <= 0f) {
            return false;
        }
        if (!facts.sameActor() || !facts.mobActor() || !facts.distinctPair() || !facts.physicalKind()) {
            return false;
        }
        return !facts.beamExcluded();
    }

    /** 从受击事件抄出准入事实。这里不报价、不扣水势。 */
    public static DirectMeleeFacts capture(LivingAttackEvent event) {
        if (event == null) {
            return DirectMeleeFacts.rejected();
        }
        LivingEntity victim = event.getEntity();
        DamageSource damageSource = event.getSource();
        float amount = event.getAmount();
        boolean canceled = event.isCanceled();
        boolean serverSide = victim != null
                && victim.level() instanceof ServerLevel serverLevel
                && !serverLevel.isClientSide();
        boolean victimViable = victim != null
                && victim.isAlive()
                && damageSource != null
                && !victim.isInvulnerableTo(damageSource);
        Entity directActor = damageSource == null ? null : damageSource.getDirectEntity();
        Entity causingActor = damageSource == null ? null : damageSource.getEntity();
        boolean sameActor = directActor != null && directActor == causingActor;
        boolean mobActor = directActor instanceof Mob && !(directActor instanceof Player);
        boolean distinctPair = directActor != null && victim != null && directActor != victim;
        boolean physicalKind = damageSource != null
                && !damageSource.is(DamageTypeTags.IS_PROJECTILE)
                && (damageSource.is(DamageTypes.MOB_ATTACK) || damageSource.is(DamageTypes.STING));
        boolean beamExcluded = NonPhysicalMeleeScope.excludesDirectMelee(victim, damageSource);
        return new DirectMeleeFacts(serverSide, canceled, amount, victimViable, sameActor, mobActor, distinctPair,
                physicalKind, beamExcluded);
    }
}
