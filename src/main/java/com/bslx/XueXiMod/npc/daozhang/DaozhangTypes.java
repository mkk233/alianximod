package com.bslx.XueXiMod.npc.daozhang;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 道长和道长弹体。正常保存，不使用分身的 noSave。 */
public final class DaozhangTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, XueXiMod.MOD_ID);

    public static final RegistryObject<EntityType<DaozhangEntity>> DAOZHANG =
            ENTITY_TYPES.register("daozhang", () -> EntityType.Builder
                    .<DaozhangEntity>of(DaozhangEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build("daozhang"));

    public static final RegistryObject<EntityType<DaozhangSpellProjectile>> SPELL =
            ENTITY_TYPES.register("daozhang_spell", () -> EntityType.Builder
                    .<DaozhangSpellProjectile>of(DaozhangSpellProjectile::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("daozhang_spell"));

    private DaozhangTypes() {
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
