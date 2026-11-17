package com.bslx.XueXiMod.npc.daozhang;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;

/** 验收用召唤和显式保管箱绑定。不会自然生成道长或自动认领箱子。 */
public final class DaozhangCommands {
    private DaozhangCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("xuexi")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("daozhang")
                        .then(Commands.literal("summon").executes(context -> summon(context.getSource())))
                        .then(Commands.literal("bind")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .then(Commands.argument("daozhang", EntityArgument.entity())
                                                .executes(context -> bind(context.getSource(),
                                                        BlockPosArgument.getLoadedBlockPos(context, "pos"),
                                                        EntityArgument.getEntity(context, "daozhang").getUUID())))))
                        .then(Commands.literal("unbind")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(context -> unbind(context.getSource(),
                                                BlockPosArgument.getLoadedBlockPos(context, "pos")))))));
    }

    private static int summon(CommandSourceStack source) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        DaozhangEntity daozhang = DaozhangTypes.DAOZHANG.get().create(level);
        if (daozhang == null) {
            return 0;
        }
        daozhang.moveTo(source.getPosition().x, source.getPosition().y, source.getPosition().z,
                source.getRotation().y, 0.0F);
        level.addFreshEntity(daozhang);
        source.sendSuccess(() -> Component.translatable("command.xueximod.daozhang.summon", daozhang.getUUID()), true);
        return 1;
    }

    private static int bind(CommandSourceStack source, BlockPos pos, UUID owner) {
        BlockEntity blockEntity = source.getLevel().getBlockEntity(pos);
        if (!DaozhangChests.bind(blockEntity, owner)) {
            source.sendFailure(Component.translatable("command.xueximod.daozhang.bind_failed"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.xueximod.daozhang.bound", pos.toShortString(), owner), true);
        return 1;
    }

    private static int unbind(CommandSourceStack source, BlockPos pos) {
        if (!DaozhangChests.unbind(source.getLevel().getBlockEntity(pos))) {
            source.sendFailure(Component.translatable("command.xueximod.daozhang.unbind_failed"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.xueximod.daozhang.unbound", pos.toShortString()), true);
        return 1;
    }
}
