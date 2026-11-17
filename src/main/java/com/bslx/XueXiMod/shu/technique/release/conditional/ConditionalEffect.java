package com.bslx.XueXiMod.shu.technique.release.conditional;

import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * 零伤条件释放。旧术式不实现它，释放定义默认没有。
 * 成功只认 {@link #attempt} 的提交结果，不靠正伤害、领域、护体或命中实体凑回执。
 * 单价大于 0 时，预扣不含这份单价，成功目标再记到传入的父收据上。
 */
public interface ConditionalEffect {
    /** 每个成功目标另计的魔力。0 表示仍走段上的固定费用。 */
    default int unitManaCost() {
        return 0;
    }

    /** 尝试这次条件释放。只有返回已提交才算成功。 */
    ConditionalResult attempt(ServerPlayer caster, UUID operationId, EnchantmentSnapshot contact, ReleaseInput input);

    /** 父收据负责这次目标费用。不传收据时，固定价术式仍走原来的尝试。 */
    default ConditionalResult attempt(ServerPlayer caster, UUID operationId, EnchantmentSnapshot contact,
                                      ReleaseInput input, ManaPaymentReceipt receipt) {
        return this.attempt(caster, operationId, contact, input);
    }

    /** 旧调用不带链输入时按基准倍率。 */
    default ConditionalResult attempt(ServerPlayer caster, UUID operationId, EnchantmentSnapshot contact) {
        return this.attempt(caster, operationId, contact, ReleaseInput.BASE);
    }

    /** 同一意图的复制份只尝试一次，基费也不乘份数。默认关闭。 */
    default boolean copiesCoalesce() {
        return false;
    }

    /** 零伤宿主可以承接有接触伤害的附魔。默认关闭。 */
    default boolean supportsContact() {
        return false;
    }
}
