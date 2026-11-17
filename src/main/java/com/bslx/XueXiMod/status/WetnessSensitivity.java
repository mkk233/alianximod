package com.bslx.XueXiMod.status;

import net.minecraft.world.entity.LivingEntity;

/** 四级怕水生物在没有真实水、雨或气泡时，按原版溺水伤害扣 1 点。不把生物标成在水里。 */
public final class WetnessSensitivity {
    private WetnessSensitivity() {}

    /** 服务端、存活路径走完后，给四级潮湿的怕水生物补一次溺水伤害。 */
    public static void afterAiStep(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        if (WetnessTiers.of(entity).tier() < 4) {
            return;
        }
        if (!entity.isSensitiveToWater() || entity.isInWaterRainOrBubble()) {
            return;
        }
        entity.hurt(entity.damageSources().drown(), 1.0F);
    }
}
