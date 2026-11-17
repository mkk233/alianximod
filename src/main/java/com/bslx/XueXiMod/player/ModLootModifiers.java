package com.bslx.XueXiMod.player;

import com.bslx.XueXiMod.XueXiMod;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModLootModifiers {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, XueXiMod.MOD_ID);

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> STUDY_CHEST =
            LOOT_MODIFIERS.register("study_chest", () -> StudyLootModifier.CODEC);

    private ModLootModifiers() {
    }

    public static void register(IEventBus eventBus) {
        LOOT_MODIFIERS.register(eventBus);
    }
}
