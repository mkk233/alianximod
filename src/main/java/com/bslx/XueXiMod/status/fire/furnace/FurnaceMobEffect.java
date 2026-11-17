package com.bslx.XueXiMod.status.fire.furnace;

import com.bslx.XueXiMod.mixin.FurnaceEffectAccess;
import com.bslx.XueXiMod.registry.ModMobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 无限炉心。牛奶走治愈物列表，默认是奶桶；这里始终给空列表。
 * 命令清除走 {@code removeEffect}，不看治愈物。
 */
public final class FurnaceMobEffect extends MobEffect {
    public FurnaceMobEffect() {
        super(MobEffectCategory.HARMFUL, 0xE25822);
    }

    /** 当前注册的炉心效果。 */
    public static MobEffect effect() {
        return ModMobEffects.FURNACE_CORE.get();
    }

    /** 这个实例是不是炉心。 */
    public static boolean isFurnace(MobEffectInstance instance) {
        return instance != null && instance.getEffect() instanceof FurnaceMobEffect;
    }

    /** 做一份合法档位的无限炉心。 */
    public static MobEffectInstance infinite(int amplifier) {
        MobEffectInstance instance = new MobEffectInstance(
                effect(),
                MobEffectInstance.INFINITE_DURATION,
                FurnaceMath.legalAmplifier(amplifier),
                false,
                true,
                true);
        normalize(instance);
        return instance;
    }

    /** 强制无限、合法档，并丢掉隐藏效果，避免高档到期后被低档悄悄换回来。 */
    public static boolean normalize(MobEffectInstance instance) {
        if (!isFurnace(instance)) {
            return false;
        }
        FurnaceEffectAccess access = (FurnaceEffectAccess) (Object) instance;
        access.xuexiSetHiddenEffect(null);
        if (!instance.isInfiniteDuration()) {
            access.xuexiSetDuration(MobEffectInstance.INFINITE_DURATION);
        }
        instance.setCurativeItems(new ArrayList<>());
        int legal = FurnaceMath.legalAmplifier(instance.getAmplifier());
        if (instance.getAmplifier() != legal) {
            access.xuexiSetAmplifier(legal);
            return true;
        }
        return false;
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return new ArrayList<>();
    }

    /** 效果挂上时按档位挂炉心属性。 */
    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        super.addAttributeModifiers(entity, attributes, amplifier);
        FurnaceAttributes.attach(entity, amplifier);
    }

    /** 效果去掉时摘掉炉心属性。 */
    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        FurnaceAttributes.detach(entity);
        super.removeAttributeModifiers(entity, attributes, amplifier);
    }
}
