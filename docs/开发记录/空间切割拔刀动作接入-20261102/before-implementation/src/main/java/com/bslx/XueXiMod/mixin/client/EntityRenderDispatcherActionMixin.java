package com.bslx.XueXiMod.mixin.client;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.bslx.XueXiMod.client.action.ActionClientPlayback;
import com.bslx.XueXiMod.client.action.ActionRenderClock;
import com.bslx.XueXiMod.client.action.ModelPartSnapshot;
import com.bslx.XueXiMod.client.action.PoseStackGuard;
import com.bslx.XueXiMod.client.action.RenderPoseScope;
import com.bslx.XueXiMod.client.action.RenderSpaceCapture;
import com.bslx.XueXiMod.client.action.skinned.SkinSession;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import com.mojang.blaze3d.vertex.PoseStack;

/** 包住玩家整次渲染，覆盖 setupAnim 和全部层，结束后恢复共享模型。 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherActionMixin {
    @Redirect(
            method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"),
            require = 1)
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void xuexiRenderEntity(
            EntityRenderer renderer,
            Entity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        if (!(renderer instanceof PlayerRenderer playerRenderer) || !(entity instanceof Player player)) {
            renderer.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }
        if (xuexiBypassesPlayerAction(player)) {
            renderer.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }
        PlayerModel<?> model = playerRenderer.getModel();
        int entryDepth = PoseStackGuard.depth(poseStack);
        ModelPartSnapshot snapshot = ModelPartSnapshot.capture(model);
        ActionRenderClock.push(partialTick);
        RenderSpaceCapture.push(poseStack);
        RenderPoseScope.runThirdPerson(model, player, () -> {
            try {
                SkinSession.openThirdPerson(model, player, playerRenderer, partialTick);
                renderer.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            } finally {
                try {
                    ActionRenderClock.pop();
                    RenderSpaceCapture.pop();
                    snapshot.restore();
                } finally {
                    PoseStackGuard.restore(poseStack, entryDepth);
                }
            }
        });
    }

    /** 没有播放记录、当前栈也没握着这份采样时，不建快照和会话。外层作用域仍走原路径，避免共享模型被内层写掉。 */
    private static boolean xuexiBypassesPlayerAction(Player player) {
        if (player == null || RenderPoseScope.depth() != 0) {
            return false;
        }
        UUID ownerId = player.getUUID();
        if (RenderPoseScope.frozenSample(ownerId) != null) {
            return false;
        }
        return !ActionClientPlayback.hasPlaybackRecord(ownerId);
    }
}
