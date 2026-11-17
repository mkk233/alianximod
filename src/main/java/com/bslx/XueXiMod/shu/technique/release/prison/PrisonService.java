package com.bslx.XueXiMod.shu.technique.release.prison;

import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

/** 水牢服务端入口。不引用客户端类型，也不扣魔力。 */
public final class PrisonService {
    private PrisonService() {
    }

    public static PrisonReservation prepare(ServerPlayer caster, LivingEntity target, UUID operationId) {
        return prepare(caster, target, operationId, ReleaseInput.BASE);
    }

    /** 按子操作号占用目标。失败预约也返回，这里不建牢、不扣魔力。 */
    public static PrisonReservation prepare(ServerPlayer caster, LivingEntity target, UUID operationId, ReleaseInput input) {
        return PrisonRuntime.prepare(caster, target, operationId, input);
    }

    /** 目标当前活动牢的快照。没有已提交的牢时为空。 */
    public static Optional<PrisonView> view(UUID targetId) {
        return PrisonRuntime.view(targetId);
    }

    /** 按牢编号结束。牢不存在或已经结束时为假，不退魔力。 */
    public static boolean end(UUID prisonId, EndReason reason) {
        return PrisonRuntime.end(prisonId, reason);
    }

    /** 四档及以上的活动牢抑制加强窒息。 */
    public static boolean suppressesEnhancedSuffocation(LivingEntity entity) {
        return PrisonRuntime.suppressesEnhancedSuffocation(entity);
    }

    /** 眼射线选湿目标。没有合法目标时返回空，不占用、不收费。 */
    public static LivingEntity selectTarget(ServerPlayer caster, double range) {
        return PrisonAim.select(caster, range);
    }

    /** 同一子操作号已有预约时为真。调用方不得再创建或收费。 */
    public static boolean reservationOpen(UUID operationId) {
        return PrisonRuntime.reservationOpen(operationId);
    }

    public static PrisonCommitResult commit(PrisonReservation reservation, EnchantmentSnapshot contact) {
        if (reservation == null) {
            return PrisonCommitResult.fail("missing");
        }
        return reservation.commit(contact);
    }

    /** 提交已有预约。空预约直接失败，服务本身不扣也不退魔力。 */
    public static PrisonCommitResult commit(PrisonReservation reservation, EnchantmentSnapshot contact, ReleaseInput input) {
        if (reservation == null) {
            return PrisonCommitResult.fail("missing");
        }
        return reservation.commit(contact, input);
    }
}
