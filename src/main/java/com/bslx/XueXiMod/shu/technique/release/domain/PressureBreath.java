package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.momentum.MomentumCreditPolicy;
import com.bslx.XueXiMod.shu.technique.release.prison.PrisonService;
import com.bslx.XueXiMod.status.WetnessTiers;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.LivingBreatheEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 深压耗氧事件服务。深海领域和潮湿四级共用这一次原版呼吸；免疫直接返回，消耗已经是 0 时不放大。
 * 球形场地没有耗氧倍率，护体不改呼吸。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class PressureBreath {
    private static final float RATE_LIMIT = 2f;

    private PressureBreath() {
    }

    /** 判断实体是否免疫这份额外耗氧。空实体、旁观、创造或原版水下呼吸都返回真。 */
    public static boolean resists(LivingEntity entity) {
        if (entity == null || entity.isSpectator()) {
            return true;
        }
        if (entity instanceof Player player && player.getAbilities().invulnerable) {
            return true;
        }
        if (MobEffectUtil.hasWaterBreathing(entity)) {
            return true;
        }
        return !entity.canDrownInFluidType(ForgeMod.WATER_TYPE.get());
    }

    /** 判断敌向深压是否标记窒息。客户端、免疫或不在有效领域内都返回假，不扣资源。 */
    public static boolean marksSuffocation(LivingEntity entity) {
        // 客户端不读取服务端领域列表。
        if (entity == null || entity.level().isClientSide()) {
            return false;
        }
        if (resists(entity)) {
            return false;
        }
        for (DomainInstance domain : DomainService.active()) {
            DomainNativePolicy policy = domain.definition().nativePolicy();
            if (policy == null || policy.breathMultiplier() <= 1f) {
                continue;
            }
            if (DomainNative.hostileInside(domain, entity)) {
                return true;
            }
        }
        return false;
    }

    public record BreathMath(boolean denyBreath, int consumeAmount) {
    }

    /** 领域倍率与潮湿倍率取较大值后再封顶。两边都没有时不改消耗，也不要求窒息。 */
    public static BreathMath combineBreath(int vanillaConsume, float domainMultiplier, int wetnessMultiplier) {
        boolean domainHolds = domainMultiplier > 1f;
        boolean wetnessHolds = wetnessMultiplier > 1;
        if (!domainHolds && !wetnessHolds) {
            return new BreathMath(false, vanillaConsume);
        }
        float combined = 1f;
        if (domainHolds && domainMultiplier > combined) {
            combined = domainMultiplier;
        }
        if (wetnessHolds && wetnessMultiplier > combined) {
            combined = wetnessMultiplier;
        }
        if (combined > RATE_LIMIT) {
            combined = RATE_LIMIT;
        }
        int consumeAmount = vanillaConsume;
        if (consumeAmount > 0) {
            consumeAmount = consumeAmount * Math.round(combined);
        }
        return new BreathMath(true, consumeAmount);
    }

    @SubscribeEvent
    /** 放大一次原版呼吸消耗。没有深压或潮湿加成时不改事件；牢笼抑制时直接返回。 */
    public static void onBreathe(LivingBreatheEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        if (entity == null || resists(entity)) {
            return;
        }
        if (PrisonService.suppressesEnhancedSuffocation(entity)) {
            return;
        }
        float domainRate = 1f;
        boolean covered = false;
        for (DomainInstance domain : DomainService.active()) {
            DomainNativePolicy policy = domain.definition().nativePolicy();
            if (policy == null || policy.breathMultiplier() <= 1f) {
                continue;
            }
            if (!DomainNative.hostileInside(domain, entity)) {
                continue;
            }
            covered = true;
            if (policy.breathMultiplier() > domainRate) {
                domainRate = policy.breathMultiplier();
            }
        }
        int wetnessRate = WetnessTiers.of(entity).breathMultiplier();
        BreathMath breathMath = combineBreath(event.getConsumeAirAmount(), covered ? domainRate : 1f, wetnessRate);
        if (!breathMath.denyBreath()) {
            return;
        }
        event.setCanBreathe(false);
        event.setCanRefillAir(false);
        event.setConsumeAirAmount(breathMath.consumeAmount());
    }

    /** 为深压窒息打开溺水水势抑制。未标记时不入栈。 */
    public static void openDrownScope(LivingEntity entity) {
        if (!marksSuffocation(entity)) {
            return;
        }
        MomentumCreditPolicy.pushDrown(entity);
        ARMED.set(ARMED.get() + 1);
    }

    /** 关闭一层溺水水势抑制。没有打开过的层时不弹出。 */
    public static void closeDrownScope(LivingEntity entity) {
        int armed = ARMED.get();
        if (armed <= 0) {
            return;
        }
        ARMED.set(armed - 1);
        MomentumCreditPolicy.popDrown();
    }

    private static final ThreadLocal<Integer> ARMED = ThreadLocal.withInitial(() -> 0);
}
