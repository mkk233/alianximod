package com.bslx.XueXiMod.shu.technique.release.prison;

import com.bslx.XueXiMod.XueXiMod;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

/** 水牢环境伤害。没有攻击者，不走虚空，也不绕过无敌帧。 */
public final class PrisonPressure {
    public static final ResourceKey<DamageType> TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(XueXiMod.MOD_ID, "water_prison_pressure"));
    public static final float AMOUNT = 4.0F;
    public static final int INTERVAL_TICKS = 20;

    private static final Logger LOGGER = LogUtils.getLogger();

    private PrisonPressure() {
    }

    public static boolean matches(DamageSource source) {
        return source != null && source.is(TYPE);
    }

    public static void pulse(LivingEntity target) {
        if (target == null || target.level().isClientSide() || !target.isAlive()) {
            return;
        }
        try {
            DamageSource source = source(target.level());
            if (target instanceof EnderDragon dragon && dragon.head != null) {
                dragon.head.hurt(source, AMOUNT);
                return;
            }
            target.hurt(source, AMOUNT);
        } catch (RuntimeException failure) {
            LOGGER.error("water prison pressure pulse failed for {}", target.getUUID(), failure);
        }
    }

    private static DamageSource source(Level level) {
        Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        return new DamageSource(registry.getHolderOrThrow(TYPE));
    }
}
