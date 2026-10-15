package com.bslx.XueXiMod.command;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 测试命令
 * /shu give   - 给予空白原胚和木属性原胚
 * /shu stone <id> - 用空白原胚刻指定术，给予术式
 * /shu list   - 列出所有已注册的术
 * /shu learn <id> - 学习指定术
 * /shu reset  - 重置玩家术数据
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ShuCommands.register(event.getDispatcher());
    }
}
