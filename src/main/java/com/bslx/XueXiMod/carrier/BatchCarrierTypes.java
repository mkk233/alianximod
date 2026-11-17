package com.bslx.XueXiMod.carrier;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 载体视觉的实体类型。不自行挂总线，由注册清单交给 A。 */
public final class BatchCarrierTypes {
    public static final String ENTITY_ID = "batch_carrier_visual";
    public static final DeferredRegister<EntityType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, XueXiMod.MOD_ID);
    public static final RegistryObject<EntityType<BatchCarrierVisual>> VISUAL =
            TYPES.register(ENTITY_ID, () -> EntityType.Builder
                    .<BatchCarrierVisual>of(BatchCarrierVisual::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(96)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .build(ENTITY_ID));

    private BatchCarrierTypes() {
    }

    public static void register(IEventBus eventBus) {
        TYPES.register(eventBus);
    }
}
