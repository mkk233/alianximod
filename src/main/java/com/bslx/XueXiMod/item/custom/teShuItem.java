package com.bslx.XueXiMod.item.custom;

import com.bslx.XueXiMod.tag.ModTags;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class teShuItem extends Item {
    public teShuItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        if (!pContext.getLevel().isClientSide()) {
            BlockPos pos = pContext.getClickedPos();
            Player player = pContext.getPlayer();
            boolean foundBlock  = false;
            //扫描Y轴
            for (int i = 0; i <=pos.getY()+64 ; i++) {
                BlockState state = pContext.getLevel().getBlockState(pos.below(i));
                if (isValueBlock(state)) {
                    outZuoBiao(pos.below(i),player,state.getBlock());   
                    foundBlock = true;
                    break;
                }
            }
            if (!foundBlock) {
                player.sendSystemMessage(Component.literal("未找到钻石矿"));
            }
        }
//      右键点击动画
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("tip.xueximod.kaungtou"));
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }

    private void outZuoBiao(BlockPos blockPos, Player player, Block block) {
        player.sendSystemMessage(Component.literal("在"+"("+blockPos.getX()+","+blockPos.getY()+","+blockPos.getZ()+
                ")找到了"+ I18n.get(block.getDescriptionId())));
    }

    private boolean isValueBlock(BlockState state) {
        return state.is(ModTags.Blocks.METAL);
    }
}
