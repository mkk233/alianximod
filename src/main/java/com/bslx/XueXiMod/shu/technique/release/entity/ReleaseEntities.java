package com.bslx.XueXiMod.shu.technique.release.entity;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.sword.SwordEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ReleaseEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, XueXiMod.MOD_ID);

    public static final RegistryObject<EntityType<TechniqueHitEntity>> TECHNIQUE_HIT =
            ENTITY_TYPES.register("technique_hit",
                    () -> EntityType.Builder.<TechniqueHitEntity>of(TechniqueHitEntity::new, MobCategory.MISC)
                            .sized(0.1f, 0.1f)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .noSave()
                            .build("technique_hit"));

    public static final RegistryObject<EntityType<SwordEntity>> SWORD =
            ENTITY_TYPES.register("sword_entity",
                    () -> EntityType.Builder.<SwordEntity>of(SwordEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("sword_entity"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

    private ReleaseEntities() {}
}
