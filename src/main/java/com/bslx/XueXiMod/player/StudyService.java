package com.bslx.XueXiMod.player;

import com.bslx.XueXiMod.item.ShuEmbryoItem;
import com.bslx.XueXiMod.item.StudySlipItem;
import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.network.ShuSyncPacket;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 刻印台参悟。父亲、阶段简和属性原胚都在服务端再查一次。 */
public final class StudyService {
    private StudyService() {
    }

    public static boolean study(Player player, String techniqueId, ItemStack embryo) {
        if (player == null || player.level().isClientSide) {
            return false;
        }
        Technique technique = TechniqueManager.getById(techniqueId);
        PlayerShuData data = PlayerShuData.get(player);
        LearningGraph.Node node = LearningGraph.node(techniqueId);
        if (technique == null || node == null || !LearningGraph.canStudy(data, techniqueId)) {
            player.displayClientMessage(Component.translatable("message.xueximod.study.unavailable"), true);
            return false;
        }
        if (embryo.isEmpty() || !(embryo.getItem() instanceof ShuEmbryoItem)
                || ShuEmbryoItem.getAttribute(embryo) != LearningGraph.embryo(technique)) {
            player.displayClientMessage(Component.translatable("message.xueximod.study.embryo"), true);
            return false;
        }
        Item slip = LearningGraph.slip(node.stage());
        Inventory inventory = player.getInventory();
        int slipSlot = find(inventory, slip);
        if (slipSlot < 0) {
            player.displayClientMessage(Component.translatable("message.xueximod.study.slip"), true);
            return false;
        }
        data.learnTechnique(techniqueId);
        PlayerShuData.save(player, data);
        if (player instanceof ServerPlayer serverPlayer) {
            ModNetwork.sendToPlayer(serverPlayer, new ShuSyncPacket(data));
        }
        embryo.shrink(1);
        inventory.getItem(slipSlot).shrink(1);
        inventory.setChanged();
        player.displayClientMessage(
                Component.translatable("message.xueximod.study.learned", technique.getName()), true);
        return true;
    }

    private static int find(Inventory inventory, Item item) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof StudySlipItem && stack.is(item)) {
                return slot;
            }
        }
        return -1;
    }
}
