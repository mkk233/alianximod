package com.bslx.XueXiMod.block;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, XueXiMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<ImprintDeskBlockEntity>> IMPRINT_DESK =
            BLOCK_ENTITIES.register("imprint_desk",
                    () -> BlockEntityType.Builder.of(ImprintDeskBlockEntity::new,
                            ModBlocks.IMPRINT_DESK.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
