package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

/**
 * 领域不再按目标逐次接触收费。
 * 额外附魔改由 {@link DomainEnchantPulse} 在创建后每 10 刻整域收取一次并施加。
 */
public final class DomainContactBilling {
    private DomainContactBilling() {
    }

    /** 保留旧入口，但不再收集逐目标事件，也不报价。 */
    public static void collect(DomainInstance domain, ServerPlayer owner, EnchantmentSnapshot snapshot,
                               ServerLevel level, AABB query) {
    }

    /** 保留旧入口。没有逐目标账单可以结算。 */
    public static void settlePending() {
    }
}
