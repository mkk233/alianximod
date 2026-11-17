package com.bslx.XueXiMod.shu.technique.release.conditional;

import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.release.prison.PrisonCommitResult;
import com.bslx.XueXiMod.shu.technique.release.prison.PrisonReservation;
import com.bslx.XueXiMod.shu.technique.release.prison.PrisonService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/**
 * 水牢只适配牢服务的选择、准备和提交。牢的状态、禁锢和结算不在这里。
 * 目标始终是施术者准星上的单个潮湿生物，不用动作撞到的那一个代替。
 */
public final class WaterPrisonEffect implements ConditionalEffect {
    public static final WaterPrisonEffect INSTANCE = new WaterPrisonEffect();
    private static final double TARGET_RANGE = 20.0D;

    private WaterPrisonEffect() {
    }

    @Override
    public boolean copiesCoalesce() {
        return true;
    }

    @Override
    public boolean supportsContact() {
        return true;
    }

    @Override
    public ConditionalResult attempt(ServerPlayer caster, UUID operationId, EnchantmentSnapshot contact) {
        if (caster == null || operationId == null) {
            return ConditionalResult.rejected("没有可禁锢的潮湿目标");
        }
        LivingEntity target = PrisonService.selectTarget(caster, TARGET_RANGE);
        if (target == null) {
            return ConditionalResult.rejected("没有可禁锢的潮湿目标");
        }
        PrisonReservation reservation = PrisonService.prepare(caster, target, operationId);
        if (reservation == null) {
            return ConditionalResult.rejected("水牢没有成立");
        }
        boolean established = false;
        String because = "";
        try {
            if (!reservation.acquired()) {
                because = reservation.reason();
                return ConditionalResult.rejected(because == null || because.isEmpty() ? "水牢没有成立" : because);
            }
            PrisonCommitResult committed = reservation.commit(contact);
            established = committed != null && committed.committed();
            if (!established) {
                because = committed == null ? "" : committed.reason();
            }
        } finally {
            reservation.close();
        }
        if (established) {
            return ConditionalResult.committed();
        }
        return ConditionalResult.rejected(because == null || because.isEmpty() ? "水牢没有成立" : because);
    }
}
