package com.bslx.XueXiMod.keybind;

import com.bslx.XueXiMod.XueXiMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/**
 * 按键绑定注册
 * C - 技槽2释放
 * V - 技槽3释放
 * R - 技槽4释放
 * M - 打开术管理界面
 * 右键 - 技槽1释放（通过交互事件处理）
 */
public class ModKeyBindings {
    public static final String CATEGORY = "key.categories." + XueXiMod.MOD_ID;

    public static final KeyMapping SKILL_SLOT_2 = new KeyMapping(
            "key." + XueXiMod.MOD_ID + ".skill_slot_2",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            CATEGORY
    );

    public static final KeyMapping SKILL_SLOT_3 = new KeyMapping(
            "key." + XueXiMod.MOD_ID + ".skill_slot_3",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    public static final KeyMapping SKILL_SLOT_4 = new KeyMapping(
            "key." + XueXiMod.MOD_ID + ".skill_slot_4",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );

    public static final KeyMapping OPEN_SHU_GUI = new KeyMapping(
            "key." + XueXiMod.MOD_ID + ".open_shu_gui",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            CATEGORY
    );

    public static void registerAll(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(SKILL_SLOT_2);
        event.register(SKILL_SLOT_3);
        event.register(SKILL_SLOT_4);
        event.register(OPEN_SHU_GUI);
    }
}
