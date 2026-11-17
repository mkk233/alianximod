package com.bslx.XueXiMod.terrain;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 无伤残骸的实体类型。本类不自行挂到模组总线，避免和公共注册各写一份。
 */
public final class MeteoriteWreckageTypes {
    public static final String ENTITY_ID = "meteorite_wreckage";
    public static final DeferredRegister<EntityType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, XueXiMod.MOD_ID);
    public static final RegistryObject<EntityType<MeteoriteWreckageEntity>> WRECKAGE =
            TYPES.register(ENTITY_ID, () -> EntityType.Builder
                    .<MeteoriteWreckageEntity>of(MeteoriteWreckageEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .build(ENTITY_ID));

    private MeteoriteWreckageTypes() {
    }

    public static void register(IEventBus eventBus) {
        TYPES.register(eventBus);
    }
}
