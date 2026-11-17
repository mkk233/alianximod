package com.bslx.XueXiMod.registry;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.status.AttributeStateMobEffect;
import com.bslx.XueXiMod.status.WetnessState;
import com.bslx.XueXiMod.status.fire.furnace.FurnaceMobEffect;
import com.bslx.XueXiMod.status.wood.vine.RoseBindEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMobEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, XueXiMod.MOD_ID);

    public static final RegistryObject<MobEffect> WETNESS = EFFECTS.register("wetness",
            () -> new AttributeStateMobEffect(WetnessState.DEFINITION, 0x3A7CA5));

    public static final RegistryObject<MobEffect> FURNACE_CORE = EFFECTS.register("furnace_core",
            FurnaceMobEffect::new);

    public static final RegistryObject<MobEffect> ROSE_BIND = EFFECTS.register("rose_bind",
            RoseBindEffect::new);

    private ModMobEffects() {}

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}
