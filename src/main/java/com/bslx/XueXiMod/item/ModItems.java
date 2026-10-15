package com.bslx.XueXiMod.item;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.item.custom.FuelItem;
import com.bslx.XueXiMod.item.custom.TeShuItem;
import com.bslx.XueXiMod.skill.SkillType;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, XueXiMod.MOD_ID);

    public static final RegistryObject<Item> shu_embryo = ITEMS.register("shu_embryo",
            () -> new ShuEmbryoItem(new Item.Properties()));
    public static final RegistryObject<Item> skill_stone = ITEMS.register("skill_stone",
            () -> new SkillStoneItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> skill_storage = ITEMS.register("skill_storage",
            () -> new SkillStorageItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> shu_raw = ITEMS.register("shu_raw", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ore_sousuo = ITEMS.register("ore_sousuo", () -> new TeShuItem(new Item.Properties()));
    public static final RegistryObject<Item> basic_staff = ITEMS.register("basic_staff",
            () -> new StaffItem(new StaffData(6, 0.20, 0.10, 100, null), new Item.Properties()));
    public static final RegistryObject<Item> shu_food = ITEMS.register("shu_food", () -> new Item(new Item.Properties().food(FoodItem.food)));
    public static final RegistryObject<Item> shu_canzha = ITEMS.register("shu_canzha", () -> new FuelItem(new Item.Properties(), 4000));
    public static final RegistryObject<Item> SKILL_BOOK_SWORD = ITEMS.register("skill_book_sword",
            () -> new SkillBookItem(new Item.Properties().stacksTo(1), SkillType.SWORD));
    public static final RegistryObject<Item> SKILL_BOOK_MAGIC = ITEMS.register("skill_book_magic",
            () -> new SkillBookItem(new Item.Properties().stacksTo(1), SkillType.MAGIC));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
