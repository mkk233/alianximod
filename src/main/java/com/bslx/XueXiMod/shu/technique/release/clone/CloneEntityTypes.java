package com.bslx.XueXiMod.shu.technique.release.clone;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 分身自己的实体注册。不改旧的释放实体注册表。
 */
public final class CloneEntityTypes {
    public static final String REGISTRY_PATH = "clone_actor";
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, XueXiMod.MOD_ID);
    public static final RegistryObject<EntityType<CloneEntity>> CLONE =
            ENTITY_TYPES.register(REGISTRY_PATH, () -> EntityType.Builder
                    .<CloneEntity>of(CloneEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .noSave()
                    .build(REGISTRY_PATH));

    private CloneEntityTypes() {
    }

    public static EntityType<CloneEntity> cloneType() {
        return CLONE.get();
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
