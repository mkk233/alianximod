package com.bslx.XueXiMod.shu.technique.release.entity;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierBody;
import com.bslx.XueXiMod.shu.technique.sword.SwordEntity;
import com.bslx.XueXiMod.status.wood.vine.RoseVineProjectile;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 释放实体注册。命中体在这里入注，生成规格之后才能进世界。
 * 同时注册屏障和剑实体。本类不负责运动、命中盒、去重或无敌帧。
 */
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

    public static final RegistryObject<EntityType<BarrierBody>> BARRIER =
            ENTITY_TYPES.register("barrier_body",
                    () -> EntityType.Builder.<BarrierBody>of(BarrierBody::new, MobCategory.MISC)
                            .sized(0.25f, 2.8f)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .noSave()
                            .build("barrier_body"));

    public static final RegistryObject<EntityType<SwordEntity>> SWORD =
            ENTITY_TYPES.register("sword_entity",
                    () -> EntityType.Builder.<SwordEntity>of(SwordEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("sword_entity"));

    public static final RegistryObject<EntityType<RoseVineProjectile>> ROSE_VINE =
            ENTITY_TYPES.register("rose_vine",
                    () -> EntityType.Builder.<RoseVineProjectile>of(RoseVineProjectile::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build("rose_vine"));

    /** 只把上面的注册表交给事件总线。 */
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

    private ReleaseEntities() {}
}
