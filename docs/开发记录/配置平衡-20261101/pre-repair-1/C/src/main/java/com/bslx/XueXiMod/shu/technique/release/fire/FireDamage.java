package com.bslx.XueXiMod.shu.technique.release.fire;

import com.bslx.XueXiMod.XueXiMod;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

import java.util.UUID;

/** 资源火伤。只调用一次 hurt，不改无敌时间，也不点燃目标。 */
public final class FireDamage {
    public static final ResourceKey<DamageType> TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(XueXiMod.MOD_ID, FireChannels.FIRE_PATH));

    private static final Logger LOGGER = LogUtils.getLogger();

    private FireDamage() {
    }

    public static boolean matches(DamageSource source) {
        return source != null && source.is(TYPE);
    }

    public static boolean strikeOnce(LivingEntity target, UUID casterId, double damage, double fireBase,
                                     boolean furnaceSettled) {
        if (target == null || target.level().isClientSide() || !target.isAlive() || target.isRemoved()) {
            return false;
        }
        if (!(target.level() instanceof ServerLevel serverLevel) || !FireLayer.acceptableDamage(damage)) {
            return false;
        }
        float amount = (float) damage;
        if (!Float.isFinite(amount) || amount <= 0.0f) {
            return false;
        }
        DamageSource source;
        try {
            source = source(serverLevel, casterId);
        } catch (RuntimeException failure) {
            LOGGER.error("fire resource strike failed for {}", target.getUUID(), failure);
            return false;
        }
        ElementDamageScope.ElementArm arm = ElementDamageScope.armFire(casterId, fireBase, furnaceSettled);
        ElementDamageScope.Boundary boundary = ElementDamageScope.mark();
        try {
            return target.hurt(source, amount);
        } catch (RuntimeException failure) {
            LOGGER.error("fire resource strike failed for {}", target.getUUID(), failure);
            return false;
        } finally {
            ElementDamageScope.restore(boundary);
            ElementDamageScope.disarm(arm);
        }
    }

    private static DamageSource source(ServerLevel serverLevel, UUID casterId) {
        Registry<DamageType> registry = serverLevel.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        Holder<DamageType> holder = registry.getHolderOrThrow(TYPE);
        ServerPlayer caster = OnlineCaster.find(serverLevel, casterId);
        if (caster == null) {
            return new DamageSource(holder);
        }
        return new DamageSource(holder, caster);
    }
}
