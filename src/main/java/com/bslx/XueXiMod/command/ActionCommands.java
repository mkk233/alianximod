package com.bslx.XueXiMod.command;

import com.bslx.XueXiMod.animation.runtime.ActionRegistry;
import com.bslx.XueXiMod.animation.runtime.ActionRuntime;
import com.bslx.XueXiMod.animation.runtime.ActionStatus;
import com.bslx.XueXiMod.animation.runtime.WaterBladeActionMap;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** 服务端检查玩家和动作 ID。没有客户端动作包，也不会因此造成伤害。 */
public final class ActionCommands {
    private ActionCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("xuexi")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("action")
                        .then(Commands.literal("play")
                                .then(Commands.argument("actionId", ResourceLocationArgument.id())
                                        .executes(context -> play(context.getSource(), ResourceLocationArgument.getId(context, "actionId"), 1.0F, false))
                                        .then(Commands.argument("speed", FloatArgumentType.floatArg(0.05F, 20.0F))
                                                .executes(context -> play(context.getSource(), ResourceLocationArgument.getId(context, "actionId"),
                                                        FloatArgumentType.getFloat(context, "speed"), false))
                                                .then(Commands.argument("looping", BoolArgumentType.bool())
                                                        .executes(context -> play(context.getSource(), ResourceLocationArgument.getId(context, "actionId"),
                                                                FloatArgumentType.getFloat(context, "speed"), BoolArgumentType.getBool(context, "looping")))))))
                        .then(Commands.literal("list")
                                .executes(context -> list(context.getSource())))
                        .then(Commands.literal("stop")
                                .executes(context -> stop(context.getSource())))
                        .then(Commands.literal("pause")
                                .executes(context -> pause(context.getSource())))
                        .then(Commands.literal("resume")
                                .executes(context -> resume(context.getSource())))));
    }

    private static int play(CommandSourceStack source, ResourceLocation actionId, float speed, boolean looping) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException failure) {
            source.sendFailure(Component.literal("需要由玩家执行"));
            return 0;
        }
        ActionRuntime.Receipt receipt = ActionRuntime.play(player, actionId, speed, looping, player.getYRot(), null);
        if (!receipt.accepted()) {
            source.sendFailure(Component.literal(receipt.reasonText()));
            return 0;
        }
        String replaced = receipt.replacedPose() ? "已替换姿态，没有取消旧的攻击几何。" : "已开始姿态。";
        source.sendSuccess(() -> Component.literal(replaced + " " + actionId + " 序号 " + receipt.sequence()), false);
        return 1;
    }

    private static int stop(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException failure) {
            source.sendFailure(Component.literal("需要由玩家执行"));
            return 0;
        }
        ActionRuntime.Receipt receipt = ActionRuntime.stop(player);
        if (!receipt.accepted()) {
            source.sendFailure(Component.literal(receipt.reasonText()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("已取消姿态，序号 " + receipt.sequence() + "。攻击几何没有被这个命令改动。"), false);
        return 1;
    }

    private static int pause(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException failure) {
            source.sendFailure(Component.literal("需要由玩家执行"));
            return 0;
        }
        ActionRuntime.Receipt receipt = ActionRuntime.pause(player);
        if (!receipt.accepted()) {
            source.sendFailure(Component.literal(receipt.reasonText()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("已暂停姿态，序号 " + receipt.sequence()), false);
        return 1;
    }

    private static int resume(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException failure) {
            source.sendFailure(Component.literal("需要由玩家执行"));
            return 0;
        }
        ActionRuntime.Receipt receipt = ActionRuntime.resume(player);
        if (!receipt.accepted()) {
            source.sendFailure(Component.literal(receipt.reasonText()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("已恢复姿态，序号 " + receipt.sequence()), false);
        return 1;
    }

    private static int list(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(ActionRegistry.gateText()), false);
        int shown = 0;
        for (ActionStatus status : ActionRegistry.statuses()) {
            String lineText = status.reportLine();
            source.sendSuccess(() -> Component.literal(lineText), false);
            shown++;
        }
        for (int beatIndex = 0; beatIndex < 3; beatIndex++) {
            String lineText = WaterBladeActionMap.resolve(beatIndex).label();
            source.sendSuccess(() -> Component.literal(lineText), false);
        }
        int reported = shown;
        source.sendSuccess(() -> Component.literal("共列出 " + reported + " 条。翻滚只看姿态，不移动、不开无敌、不改碰撞箱。"), false);
        return reported;
    }
}
