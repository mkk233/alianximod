package com.bslx.XueXiMod.status.wood.vine;

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

/** 抽枝、移动刺伤和流血。调用方只在正金额时进入。 */
public final class VineMarkDamage {
    public static final ResourceKey<DamageType> PIERCE = key("rose_pierce");
    public static final ResourceKey<DamageType> BLEED = key("rose_bleed");

    private static final Logger LOGGER = LogUtils.getLogger();

    private VineMarkDamage() {
    }

    public static boolean pierce(LivingEntity target, UUID casterId, float amount) {
        return strike(target, casterId, amount, PIERCE);
    }

    public static boolean bleed(LivingEntity target, UUID casterId, float amount) {
        return strike(target, casterId, amount, BLEED);
    }

    public static boolean matches(DamageSource source) {
        return source != null && (source.is(PIERCE) || source.is(BLEED));
    }

    private static boolean strike(LivingEntity target, UUID casterId, float amount, ResourceKey<DamageType> type) {
        if (target == null || target.level().isClientSide() || !target.isAlive() || target.isRemoved()) {
            return false;
        }
        if (!Float.isFinite(amount) || amount <= 0.0f || !(target.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        try {
            return target.hurt(source(serverLevel, casterId, type), amount);
        } catch (RuntimeException failure) {
            LOGGER.error("rose derivative strike failed for {}", target.getUUID(), failure);
            return false;
        }
    }

    private static DamageSource source(ServerLevel level, UUID casterId, ResourceKey<DamageType> type) {
        Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        Holder<DamageType> holder = registry.getHolderOrThrow(type);
        ServerPlayer caster = find(level, casterId);
        if (caster == null) {
            return new DamageSource(holder);
        }
        return new DamageSource(holder, caster);
    }

    private static ServerPlayer find(ServerLevel level, UUID casterId) {
        if (casterId == null || level.getServer() == null) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(casterId);
    }

    private static ResourceKey<DamageType> key(String path) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(XueXiMod.MOD_ID, path));
    }
}
