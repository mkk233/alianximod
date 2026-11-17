package com.bslx.XueXiMod.command;

import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneHandle;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneKind;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneResults;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneService;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneTriggerInput;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * 权限 2 的本人分身命令。业务进 CloneService，这里只解析和回执。
 */
public final class CloneCommands {
    private CloneCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> branch() {
        return Commands.literal("clone")
                .requires(CloneCommands::allowsOperator)
                .then(spawn())
                .then(list())
                .then(respond())
                .then(dismiss());
    }

    private static boolean allowsOperator(CommandSourceStack source) {
        return source.hasPermission(2);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> spawn() {
        return Commands.literal("spawn")
                .requires(CloneCommands::allowsOperator)
                .then(Commands.literal("phantom").executes(commandContext -> spawn(commandContext, CloneKind.PHANTOM)))
                .then(Commands.literal("normal").executes(commandContext -> spawn(commandContext, CloneKind.NORMAL)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> list() {
        return Commands.literal("list")
                .requires(CloneCommands::allowsOperator)
                .executes(CloneCommands::listOwned);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> respond() {
        return Commands.literal("respond")
                .requires(CloneCommands::allowsOperator)
                .then(Commands.argument("actorUuid", UuidArgument.uuid())
                        .executes(CloneCommands::respondOwned));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dismiss() {
        return Commands.literal("dismiss")
                .requires(CloneCommands::allowsOperator)
                .then(Commands.literal("all").executes(CloneCommands::dismissOwnedAll))
                .then(Commands.argument("actorUuid", UuidArgument.uuid())
                        .executes(CloneCommands::dismissOwned));
    }

    private static int spawn(CommandContext<CommandSourceStack> commandContext, CloneKind kind)
            throws CommandSyntaxException {
        ServerPlayer owner = commandContext.getSource().getPlayerOrException();
        CloneResults.SpawnResult outcome = CloneService.spawn(owner, kind);
        return reply(commandContext.getSource(), outcome.accepted(), outcome.detail());
    }

    private static int listOwned(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        ServerPlayer owner = commandContext.getSource().getPlayerOrException();
        List<CloneResults.Status> rows = CloneService.list(owner);
        if (rows == null || rows.isEmpty()) {
            commandContext.getSource().sendSuccess(() -> Component.literal("没有分身"), false);
            return 1;
        }
        for (CloneResults.Status row : rows) {
            Component shown = listLine(row);
            commandContext.getSource().sendSuccess(() -> shown, false);
        }
        return rows.size();
    }

    private static int respondOwned(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        ServerPlayer owner = commandContext.getSource().getPlayerOrException();
        UUID actorId = UuidArgument.getUuid(commandContext, "actorUuid");
        Vec3 look = owner.getLookAngle();
        double lookLength = look.length();
        if (!Double.isFinite(look.x) || !Double.isFinite(look.y) || !Double.isFinite(look.z)
                || !Double.isFinite(lookLength) || lookLength < 1.0E-4D) {
            commandContext.getSource().sendFailure(Component.literal("视线无效"));
            return 0;
        }
        Vec3 manualLook = look.scale(1.0D / lookLength);
        CloneTriggerInput input = new CloneTriggerInput(
                CloneTriggerInput.Source.MANUAL, manualLook, null, 0);
        CloneHandle ownedHandle = CloneService.handleFor(owner, actorId);
        if (ownedHandle == null) {
            commandContext.getSource().sendFailure(Component.literal("不在本人索引"));
            return 0;
        }
        CloneResults.RequestResult outcome = CloneService.request(owner, ownedHandle, input);
        return reply(commandContext.getSource(), outcome.accepted(), outcome.detail());
    }

    private static int dismissOwned(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        ServerPlayer owner = commandContext.getSource().getPlayerOrException();
        UUID actorId = UuidArgument.getUuid(commandContext, "actorUuid");
        boolean removed = CloneService.dismiss(owner, actorId);
        if (removed) {
            commandContext.getSource().sendSuccess(() -> Component.literal("已撤回"), false);
            return 1;
        }
        commandContext.getSource().sendFailure(Component.literal("不在本人索引"));
        return 0;
    }

    private static int dismissOwnedAll(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        ServerPlayer owner = commandContext.getSource().getPlayerOrException();
        int removedCount = CloneService.dismissAll(owner);
        commandContext.getSource().sendSuccess(() -> Component.literal("已撤回 " + removedCount), false);
        return removedCount;
    }

    private static int reply(CommandSourceStack source, boolean accepted, String detailText) {
        String shown = detailText == null ? "" : detailText;
        if (accepted) {
            source.sendSuccess(() -> Component.literal(shown), false);
            return 1;
        }
        source.sendFailure(Component.literal(shown));
        return 0;
    }

    private static Component listLine(CloneResults.Status row) {
        String lineText = row.commandLine() == null ? "" : row.commandLine();
        String actorText = row.actorId() == null ? "" : row.actorId().toString();
        String suggestRespond = "/shu clone respond " + actorText;
        return Component.literal(lineText).withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggestRespond))
                .withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        Component.literal("点击填入 " + suggestRespond + "；撤回用 /shu clone dismiss " + actorText))));
    }
}
