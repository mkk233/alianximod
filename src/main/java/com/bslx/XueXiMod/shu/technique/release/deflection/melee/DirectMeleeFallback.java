package com.bslx.XueXiMod.shu.technique.release.deflection.melee;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.effects.SwordGuardEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 旧的 Player.attack / Mob.doHurtTarget 没经过时，用 LivingAttackEvent 补直接怪物近战。
 * 优先级最低，并且不接收已经取消的事件。同一次事件重入不再买第二单。
 * 已有同对尝试时直接跳过，包括旧 arm 预约失败留下的空账。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class DirectMeleeFallback {
    private DirectMeleeFallback() {
    }

    enum Gate {
        /** 可以进入这次补充结算。 */
        READY,
        /** 同一个事件已经处理过。 */
        SEEN,
        /** 不是直接怪物近战。 */
        REJECTED
    }

    /** 旧攻击入口没经过时补一次直接怪物近战。提交成功才取消事件。 */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public static void onDirectMobMelee(LivingAttackEvent event) {
        if (event == null) {
            return;
        }
        if (claim(event, DirectMeleeAdmission.capture(event)) != Gate.READY) {
            return;
        }
        Entity actor = event.getSource() == null ? null : event.getSource().getDirectEntity();
        if (!(actor instanceof LivingEntity attacker)) {
            return;
        }
        try {
            if (MeleeDeflection.settleOwnedDirect(attacker, event.getEntity())
                    || SwordGuardEffect.parryDirectMobMelee(attacker, event.getEntity())) {
                event.setCanceled(true);
            }
        } catch (RuntimeException failure) {
            return;
        }
    }

    static Gate claim(Object eventIdentity, DirectMeleeAdmission.DirectMeleeFacts facts) {
        if (eventIdentity != null && DirectMeleeAttempts.seen(eventIdentity)) {
            return Gate.SEEN;
        }
        if (!DirectMeleeAdmission.admits(facts)) {
            return Gate.REJECTED;
        }
        if (eventIdentity != null) {
            DirectMeleeAttempts.mark(eventIdentity);
        }
        return Gate.READY;
    }
}
