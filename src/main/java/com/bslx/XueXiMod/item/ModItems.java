package com.bslx.XueXiMod.item;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.player.LearningGraph;
import com.bslx.XueXiMod.npc.daozhang.DaozhangTypes;
import com.bslx.XueXiMod.item.custom.FuelItem;
import com.bslx.XueXiMod.item.custom.TeShuItem;
import com.bslx.XueXiMod.skill.SkillType;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
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
    /** 32 槽测试载体。回魔效率 15 使 1+效率 等于基础的 16 倍。冷却缩短 0.50。不改基础法杖。 */
    public static final RegistryObject<Item> test_staff = ITEMS.register("test_staff",
            () -> new StaffItem(new StaffData(32, 15.0, 0.50, 1000, null), new Item.Properties()));
    public static final RegistryObject<Item> shu_food = ITEMS.register("shu_food", () -> new Item(new Item.Properties().food(FoodItem.food)));
    public static final RegistryObject<Item> shu_canzha = ITEMS.register("shu_canzha", () -> new FuelItem(new Item.Properties(), 4000));
    public static final RegistryObject<Item> SKILL_BOOK_SWORD = ITEMS.register("skill_book_sword",
            () -> new SkillBookItem(new Item.Properties().stacksTo(1), SkillType.SWORD));
    public static final RegistryObject<Item> SKILL_BOOK_MAGIC = ITEMS.register("skill_book_magic",
            () -> new SkillBookItem(new Item.Properties().stacksTo(1), SkillType.MAGIC));
    public static final RegistryObject<Item> STUDY_SLIP_MORTAL = ITEMS.register("study_slip_mortal",
            () -> new StudySlipItem(new Item.Properties().stacksTo(64), LearningGraph.Stage.MORTAL));
    public static final RegistryObject<Item> STUDY_SLIP_MYSTIC = ITEMS.register("study_slip_mystic",
            () -> new StudySlipItem(new Item.Properties().stacksTo(64), LearningGraph.Stage.MYSTIC));
    public static final RegistryObject<Item> STUDY_SLIP_TRANSCENDENT = ITEMS.register("study_slip_transcendent",
            () -> new StudySlipItem(new Item.Properties().stacksTo(64), LearningGraph.Stage.TRANSCENDENT));
    public static final RegistryObject<Item> STUDY_SCROLL_SWORD = ITEMS.register("study_scroll_sword",
            () -> new StudyScrollItem(new Item.Properties().stacksTo(1),
                    "sword_thrust", "sword_horizontal_slash", "sword_vertical_slash",
                    "sword_guard", "movement_dash", "movement_leap"));
    public static final RegistryObject<Item> STUDY_SCROLL_WATER = ITEMS.register("study_scroll_water",
            () -> new StudyScrollItem(new Item.Properties().stacksTo(1),
                    "water_weeping_arc", "tide_mark", "water_curtain", "water_wave"));
    public static final RegistryObject<Item> STUDY_SCROLL_FIRE = ITEMS.register("study_scroll_fire",
            () -> new StudyScrollItem(new Item.Properties().stacksTo(1),
                    "fire_orb", "fire_spark", "fire_seed_sow", "fire_kindle"));
    public static final RegistryObject<Item> DAOZHANG_SPAWN_EGG = ITEMS.register("daozhang_spawn_egg",
            () -> new ForgeSpawnEggItem(DaozhangTypes.DAOZHANG, 0x6E6250, 0xE8D7AE, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
