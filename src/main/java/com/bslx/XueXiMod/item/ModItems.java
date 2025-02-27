package com.bslx.XueXiMod.item;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    //作用未知
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, XueXiMod.MOD_ID);
    //注册物品，示例术原胚
    public static final RegistryObject<Item> shu_embryo =ITEMS.register("shu_embryo", () -> new Item(new Item.Properties()));

    //物品注册表，用于加载注册物品
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
