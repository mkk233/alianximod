package com.bslx.XueXiMod.player;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.block.ModBlocks;
import com.bslx.XueXiMod.item.ModItems;
import com.bslx.XueXiMod.item.ShuEmbryoItem;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.google.common.collect.ImmutableSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 授术先生。工作点沿用现有刻印台，不新增结构。 */
public final class ModVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(ForgeRegistries.POI_TYPES, XueXiMod.MOD_ID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, XueXiMod.MOD_ID);

    public static final RegistryObject<PoiType> IMPRINT_POI = POI_TYPES.register("imprint_desk",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.IMPRINT_DESK.get().getStateDefinition().getPossibleStates()),
                    1, 1));
    public static final RegistryObject<VillagerProfession> STUDY_MASTER = PROFESSIONS.register("study_master",
            () -> new VillagerProfession("study_master", holder -> holder.is(IMPRINT_POI.getKey()),
                    holder -> holder.is(IMPRINT_POI.getKey()), ImmutableSet.of(), ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_LIBRARIAN));

    private ModVillagers() {
    }

    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
        PROFESSIONS.register(eventBus);
    }

    @Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
    public static final class Trades {
        private Trades() {
        }

        @SubscribeEvent
        public static void register(VillagerTradesEvent event) {
            if (!event.getType().equals(STUDY_MASTER.get())) {
                return;
            }
            var trades = event.getTrades();
            trades.get(1).add((trader, random) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD), ShuEmbryoItem.create(ShuAttribute.NONE), 12, 2, 0.05f));
            trades.get(1).add((trader, random) -> new net.minecraft.world.item.trading.MerchantOffer(
                    new ItemStack(ModItems.shu_canzha.get()), new ItemStack(Items.EMERALD), 16, 2, 0.05f));
            trades.get(2).add(sale(ModItems.STUDY_SCROLL_SWORD.get(), 8, 12, 5));
            trades.get(2).add(sale(ModItems.STUDY_SCROLL_WATER.get(), 8, 12, 5));
            trades.get(2).add(sale(ModItems.STUDY_SCROLL_FIRE.get(), 8, 12, 5));
            trades.get(3).add(sale(ModItems.STUDY_SLIP_MORTAL.get(), 5, 12, 10));
            trades.get(4).add(sale(ModItems.STUDY_SLIP_MYSTIC.get(), 12, 8, 15));
            trades.get(5).add(sale(ModItems.STUDY_SLIP_TRANSCENDENT.get(), 32, 4, 30));
        }

        private static VillagerTrades.ItemListing sale(
                net.minecraft.world.item.Item item, int emeralds, int maxUses, int experience) {
            return (trader, random) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, emeralds), new ItemStack(item), maxUses, experience, 0.05f);
        }
    }
}
