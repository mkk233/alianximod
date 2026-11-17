package com.bslx.XueXiMod.item;

import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.network.ShuSyncPacket;
import com.bslx.XueXiMod.player.PlayerShuData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** 右键学会一卷固定启蒙术式。已经都会时不消耗。 */
public class StudyScrollItem extends Item {
    private final String[] techniqueIds;

    public StudyScrollItem(Properties properties, String... techniqueIds) {
        super(properties);
        this.techniqueIds = techniqueIds.clone();
    }

    public String[] techniqueIds() {
        return techniqueIds.clone();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        PlayerShuData data = PlayerShuData.get(player);
        boolean missing = false;
        for (String techniqueId : techniqueIds) {
            if (!data.hasLearned(techniqueId)) {
                data.learnTechnique(techniqueId);
                missing = true;
            }
        }
        if (!missing) {
            player.displayClientMessage(Component.translatable("message.xueximod.study_scroll.known"), true);
            return InteractionResultHolder.fail(stack);
        }
        PlayerShuData.save(player, data);
        if (player instanceof ServerPlayer serverPlayer) {
            ModNetwork.sendToPlayer(serverPlayer, new ShuSyncPacket(data));
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.displayClientMessage(Component.translatable("message.xueximod.study_scroll.learned"), true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        String key = stack.getDescriptionId();
        tooltip.add(Component.translatable("tooltip." + key.substring(key.indexOf('.') + 1)));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
