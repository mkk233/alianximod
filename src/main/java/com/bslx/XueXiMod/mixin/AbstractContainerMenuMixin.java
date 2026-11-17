package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.item.EmbryoConversionService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 金胚的普通点击和 Shift 都一次只做一个。
 * 数字键、丢弃、拖拽和中键复制不能绕过背包容量。
 */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
    private void xuexiTakeMetalEmbryo(int slotIndex, int button, ClickType clickType, Player player,
                                      CallbackInfo callback) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (!(menu instanceof AnvilMenu anvilMenu) || slotIndex != anvilMenu.getResultSlot()) {
            return;
        }
        ItemStack left = anvilMenu.getSlot(AnvilMenu.INPUT_SLOT).getItem();
        ItemStack right = anvilMenu.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
        if (!EmbryoConversionService.isMetalPair(left, right)) {
            return;
        }
        callback.cancel();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        boolean ordinaryTake = (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE)
                && (button == 0 || button == 1);
        if (!ordinaryTake || !menu.getCarried().isEmpty()) {
            return;
        }
        EmbryoConversionService.commitMetal(anvilMenu, serverPlayer);
    }
}
