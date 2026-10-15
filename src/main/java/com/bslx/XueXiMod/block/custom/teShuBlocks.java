package com.bslx.XueXiMod.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 特殊交互方块 - 右键播放音效的演示方块
 *
 * 核心职责：
 * - 右键交互时播放 Didgeridoo 音效（SoundEvents.NOTE_BLOCK_DIDGERIDOO）
 * - 物品形态显示自定义 tooltip（tooltip.block.custom.teShuBlocks）
 *
 * 用途：用于 zhu_block 注册，作为自定义方块交互的示例
 */
public class TeShuBlocks extends Block {

    public TeShuBlocks(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        pLevel.playSound(null, pPos, SoundEvents.NOTE_BLOCK_DIDGERIDOO.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable BlockGetter pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
        pTooltip.add(Component.translatable("tooltip.block.custom.teShuBlocks"));
        super.appendHoverText(pStack, pLevel, pTooltip, pFlag);
    }
}
