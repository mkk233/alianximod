package com.bslx.XueXiMod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * 技法刻印桌
 * 原胚 + 已学术 → 术式
 */
public class ImprintDeskBlock extends Block implements EntityBlock {

    public ImprintDeskBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPosition, BlockState state) {
        return new ImprintDeskBlockEntity(blockPosition, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos blockPosition,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(blockPosition) instanceof ImprintDeskBlockEntity) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                com.bslx.XueXiMod.network.ModNetwork.sendToPlayer(
                        serverPlayer,
                        new com.bslx.XueXiMod.network.ShuSyncPacket(
                                com.bslx.XueXiMod.player.PlayerShuData.get(serverPlayer)));
                NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("block.xueximod.imprint_desk");
                    }

                    @Override
                    public @Nullable AbstractContainerMenu createMenu(
                            int containerId, net.minecraft.world.entity.player.Inventory playerInventory, Player player) {
                        return new com.bslx.XueXiMod.gui.ImprintDeskMenu(
                                containerId, playerInventory, blockPosition);
                    }
                }, blockPosition);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
