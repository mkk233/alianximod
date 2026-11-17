package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.item.EmbryoConversionService;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 金胚配方提前结束原版计费，只留零级预览。实际取出由专用点击路径处理。 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
    @Shadow
    @Final
    private DataSlot cost;

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void xuexiPreviewMetalEmbryo(CallbackInfo callback) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        ItemStack left = menu.getSlot(AnvilMenu.INPUT_SLOT).getItem();
        ItemStack right = menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
        if (!EmbryoConversionService.isMetalPair(left, right)) {
            return;
        }
        menu.getSlot(AnvilMenu.RESULT_SLOT).set(EmbryoConversionService.metalResult(left));
        this.cost.set(0);
        menu.repairItemCountCost = 1;
        menu.broadcastChanges();
        callback.cancel();
    }
}
