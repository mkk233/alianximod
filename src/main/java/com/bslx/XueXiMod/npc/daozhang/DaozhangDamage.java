package com.bslx.XueXiMod.npc.daozhang;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

/** 直接实体是弹体，造成者是道长。 */
public final class DaozhangDamage {
    public static final ResourceKey<DamageType> TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(XueXiMod.MOD_ID, "daozhang_spell"));

    private DaozhangDamage() {
    }

    public static DamageSource source(ServerLevel level, Entity projectile, Entity daozhang) {
        Holder<DamageType> type = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(TYPE);
        return new DamageSource(type, projectile, daozhang);
    }
}
