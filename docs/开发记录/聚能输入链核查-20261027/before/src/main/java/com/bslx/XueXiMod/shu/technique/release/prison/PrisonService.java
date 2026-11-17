package com.bslx.XueXiMod.shu.technique.release.prison;

import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

/** 水牢服务端入口。不引用客户端类型，也不扣魔力。 */
public final class PrisonService {
    private PrisonService() {
    }

    public static PrisonReservation prepare(ServerPlayer caster, LivingEntity target, UUID operationId) {
        return PrisonRuntime.prepare(caster, target, operationId);
    }

    public static Optional<PrisonView> view(UUID targetId) {
        return PrisonRuntime.view(targetId);
    }

    public static boolean end(UUID prisonId, EndReason reason) {
        return PrisonRuntime.end(prisonId, reason);
    }

    public static boolean suppressesEnhancedSuffocation(LivingEntity entity) {
        return PrisonRuntime.suppressesEnhancedSuffocation(entity);
    }

    public static LivingEntity selectTarget(ServerPlayer caster, double range) {
        return PrisonAim.select(caster, range);
    }

    public static PrisonCommitResult commit(PrisonReservation reservation, EnchantmentSnapshot contact) {
        if (reservation == null) {
            return PrisonCommitResult.fail("missing");
        }
        return reservation.commit(contact);
    }
}
