package com.bslx.XueXiMod.shu.technique.release.momentum;

import com.bslx.XueXiMod.shu.technique.release.deflection.projectile.ArrowSupport;
import com.bslx.XueXiMod.status.WetnessTiers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;

/**
 * 湿目标被真正打中后的额外水势。只认这次已经成功的正伤害，按受害者当前档位给攻击玩家入账一次。
 * 不看目标是否还活着，也不在这里施湿。窒息、溺水和别的环境来源不加。
 */
public final class WetAttackReward {
    private WetAttackReward() {
    }

    /** 按受害者当前潮湿档位，给这次近战或受支持箭矢的攻击玩家记一笔水势。 */
    public static void grant(LivingEntity victim, DamageSource source) {
        ServerPlayer attacker = attackingPlayer(source);
        if (victim == null || attacker == null || attacker == victim) {
            return;
        }
        float reward = WetnessTiers.of(victim).attackReward();
        if (!Float.isFinite(reward) || reward <= 0f) {
            return;
        }
        WaterMomentum.credit(attacker, reward);
    }

    /** 只接受玩家近战，以及主人就是该玩家的受支持箭矢。溺水来源直接排除。 */
    private static ServerPlayer attackingPlayer(DamageSource source) {
        if (source == null || source.is(DamageTypeTags.IS_DROWNING)) {
            return null;
        }
        Entity cause = source.getEntity();
        if (!(cause instanceof ServerPlayer attacker)) {
            return null;
        }
        Entity direct = source.getDirectEntity();
        if (source.is(DamageTypes.PLAYER_ATTACK) && direct == attacker) {
            return attacker;
        }
        if (source.is(DamageTypes.ARROW)
                && direct instanceof AbstractArrow arrow
                && ArrowSupport.supported(arrow)
                && arrow.getOwner() == attacker) {
            return attacker;
        }
        return null;
    }
}
