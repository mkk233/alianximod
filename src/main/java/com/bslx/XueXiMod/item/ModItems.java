package com.bslx.XueXiMod.item;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.item.custom.teShuItem;
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
    public static final RegistryObject<Item> shu_raw =ITEMS.register("shu_raw", () -> new Item(new Item.Properties()));


    public static final RegistryObject<Item> ore_sousuo =ITEMS.register("ore_sousuo", () -> new teShuItem(new Item.Properties()));
//    durability(9999)用于定义耐久
//    public static final RegistryObject<Item> ore_sousuo =ITEMS.register("ore_sousuo", () -> new teShuItem(new Item.Properties().durability(9999)));

    //物品注册表，用于加载注册物品
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
