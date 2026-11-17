package com.bslx.XueXiMod.shu.technique.release.conditional;

import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * 零伤条件释放。旧术式不实现它，释放定义默认没有。
 * 成功只认 {@link #attempt} 的提交结果，不靠正伤害、领域、护体或命中实体凑回执。
 */
public interface ConditionalEffect {
    ConditionalResult attempt(ServerPlayer caster, UUID operationId, EnchantmentSnapshot contact);

    /** 同一意图的复制份只尝试一次，基费也不乘份数。默认关闭。 */
    default boolean copiesCoalesce() {
        return false;
    }

    /** 零伤宿主可以承接有接触伤害的附魔。默认关闭。 */
    default boolean supportsContact() {
        return false;
    }
}
