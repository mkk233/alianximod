package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.item.EmbryoConversionService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 金胚的 Shift 只能走专用事务，不能进入原版结果槽循环。 */
@Mixin(ItemCombinerMenu.class)
public abstract class ItemCombinerMenuMixin {
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void xuexiKeepMetalQuickMoveSingle(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> callback) {
        ItemCombinerMenu menu = (ItemCombinerMenu) (Object) this;
        if (!(menu instanceof AnvilMenu anvilMenu) || slotIndex != anvilMenu.getResultSlot()) {
            return;
        }
        ItemStack left = anvilMenu.getSlot(AnvilMenu.INPUT_SLOT).getItem();
        ItemStack right = anvilMenu.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
        if (EmbryoConversionService.isMetalPair(left, right)) {
            callback.setReturnValue(ItemStack.EMPTY);
        }
    }
}
