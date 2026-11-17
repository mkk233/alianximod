package com.bslx.XueXiMod.command;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.debug.performance.PerformanceCommands;
import com.bslx.XueXiMod.npc.daozhang.DaozhangCommands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 测试命令
 * /shu give   - 给予空白原胚和木属性原胚
 * /shu stone <id> - 用空白原胚刻指定术，给予术式
 * /shu list   - 列出所有已注册的术
 * /shu learn <id> - 学习指定术
 * /shu learn all - 学会全部需要学习的术式并升到能解锁的熟练
 * /shu reset  - 重置玩家术数据
 * /shu rank   - 查看或设置熟练等级和熟练点
 * /shu cd off - 临时关闭全部释放冷却，/shu cd on 恢复
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ShuCommands.register(event.getDispatcher());
        ActionCommands.register(event.getDispatcher());
        PerformanceCommands.register(event.getDispatcher());
        DaozhangCommands.register(event.getDispatcher());
    }
}
