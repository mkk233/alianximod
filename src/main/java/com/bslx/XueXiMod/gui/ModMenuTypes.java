package com.bslx.XueXiMod.gui;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 菜单类型注册
 */
public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, XueXiMod.MOD_ID);

    public static final RegistryObject<MenuType<ImprintDeskMenu>> IMPRINT_DESK =
            registerMenuType("imprint_desk", ImprintDeskMenu::new);

    public static final RegistryObject<MenuType<ShuManagementMenu>> SHU_MANAGEMENT =
            registerMenuType("shu_management", ShuManagementMenu::new);

    public static final RegistryObject<MenuType<SkillAssemblyMenu>> SKILL_ASSEMBLY =
            registerMenuType("skill_assembly", SkillAssemblyMenu::new);

    public static final RegistryObject<MenuType<SpellStorageMenu>> SPELL_STORAGE =
            registerMenuType("spell_storage", (containerId, inventory, buffer) -> new SpellStorageMenu(containerId, inventory));

    public static final RegistryObject<MenuType<SpellAssemblyMenu>> SPELL_ASSEMBLY =
            registerMenuType("spell_assembly", (containerId, inventory, buffer) -> new SpellAssemblyMenu(containerId, inventory));

    public static final RegistryObject<MenuType<DaozhangMenu>> DAOZHANG =
            registerMenuType("daozhang", DaozhangMenu::new);

    private static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> registerMenuType(
            String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IForgeMenuType.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
