package com.bslx.XueXiMod.item.custom;

import com.bslx.XueXiMod.tag.ModTags;
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

/**
 * 矿物探测物品 - 右键方块扫描下方矿脉的特殊物品
 *
 * 核心职责：
 * - 右键点击方块时，沿 Y 轴向下扫描最多 64 格
 * - 检测 ModTags.Blocks.METAL 标签的方块（矿石）
 * - 找到后向玩家发送坐标消息，未找到则提示"未找到术蚀矿"
 * - 物品 tooltip 显示提示信息（tip.xueximod.kaungtou）
 *
 * 用于 ore_sousuo（矿物探测器）注册
 */
public class TeShuItem extends Item {
    public TeShuItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        if (!pContext.getLevel().isClientSide()) {
            BlockPos clickedPosition = pContext.getClickedPos();
            Player player = pContext.getPlayer();
            boolean foundBlock  = false;
            //扫描Y轴
            for (int segmentIndex = 0; segmentIndex <= 64; segmentIndex++) {
                BlockState state = pContext.getLevel().getBlockState(clickedPosition.below(segmentIndex));
                if (isValueBlock(state)) {
                    outZuoBiao(clickedPosition.below(segmentIndex),player,state.getBlock());   
                    foundBlock = true;
                    break;
                }
            }
            if (!foundBlock) {
                player.sendSystemMessage(Component.literal("未找到术蚀矿"));
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

    private void outZuoBiao(BlockPos position, Player player, Block block) {
        player.sendSystemMessage(Component.translatable("在 (%s,%s,%s) 找到了 %s",
                position.getX(), position.getY(), position.getZ(), Component.translatable(block.getDescriptionId())));
    }

    private boolean isValueBlock(BlockState state) {
        return state.is(ModTags.Blocks.METAL);
    }
}
