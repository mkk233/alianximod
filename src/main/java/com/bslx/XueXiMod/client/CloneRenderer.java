package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.client.action.cooperative.CooperativeActionViews;
import com.bslx.XueXiMod.client.action.cooperative.CooperativePartGuard;
import com.bslx.XueXiMod.client.action.cooperative.CooperativeWeaponCopy;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneEntity;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneLifeState;
import com.bslx.XueXiMod.shu.technique.release.clone.core.PresentationKey;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 旧分身继续用原版僵尸人体和僵尸贴图。
 * WATER_OUTLINE 画确认版 Steve 水线，不画僵尸。
 * 原版 ZombieModel 的类型上界是 Zombie，不能声明成 CloneEntity 的模型参数。
 */
public class CloneRenderer extends LivingEntityRenderer<CloneEntity, HumanoidModel<CloneEntity>> {
    private static final ResourceLocation VANILLA_ZOMBIE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/zombie/zombie.png");
    private final Map<UUID, Long> seenActionSequence = new HashMap<>();

    public CloneRenderer(EntityRendererProvider.Context rendererContext) {
        super(rendererContext, new HumanoidModel<>(rendererContext.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public boolean shouldRender(CloneEntity cloneActor, Frustum camera, double cameraX, double cameraY,
                                double cameraZ) {
        if (waterHidden(cloneActor)) {
            return false;
        }
        return super.shouldRender(cloneActor, camera, cameraX, cameraY, cameraZ);
    }

    @Override
    public void render(CloneEntity cloneActor, float bodyYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        if (cloneActor != null && cloneActor.presentationKey() == PresentationKey.WATER_OUTLINE) {
            if (waterHidden(cloneActor) || poseStack == null || bufferSource == null) {
                return;
            }
            float frozenYaw = Mth.rotLerp(partialTick, cloneActor.yBodyRotO, cloneActor.yBodyRot);
            float elapsedSeconds = (cloneActor.tickCount + partialTick) / 20.0F;
            int brightLight = packedLight | LightTexture.FULL_BRIGHT;
            poseStack.pushPose();
            SteveWaterLineVisual.alignVanillaSteve(poseStack, frozenYaw);
            SteveWaterLineVisual.drawPhase(poseStack, bufferSource, brightLight,
                    SteveWaterLineVisual.residencePhase(elapsedSeconds));
            poseStack.popPose();
            return;
        }
        if (cloneActor != null) {
            noteSequence(cloneActor);
        }
        CooperativePartGuard.open(this.model);
        try {
            super.render(cloneActor, bodyYaw, partialTick, poseStack, bufferSource, packedLight);
            CooperativeWeaponCopy.draw(this.model, cloneActor, partialTick, poseStack, bufferSource, packedLight);
        } finally {
            CooperativePartGuard.close();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(CloneEntity cloneActor) {
        return VANILLA_ZOMBIE_TEXTURE;
    }

    private static boolean waterHidden(CloneEntity cloneActor) {
        if (cloneActor == null || cloneActor.presentationKey() != PresentationKey.WATER_OUTLINE) {
            return false;
        }
        return cloneActor.lifeState() != CloneLifeState.ACTIVE
                || CloneAfterimagePlayback.scatterHidesBody(cloneActor.presentationDeploymentId());
    }

    private void noteSequence(CloneEntity cloneActor) {
        UUID actorId = cloneActor.getUUID();
        if (cloneActor.isRemoved()) {
            seenActionSequence.remove(actorId);
            return;
        }
        long sequence = cloneActor.actionSequence();
        Long seen = seenActionSequence.get(actorId);
        if (seen != null && seen.longValue() != sequence && !CooperativeActionViews.holds(cloneActor)) {
            cloneActor.swing(InteractionHand.MAIN_HAND);
        }
        if (seen == null || seen.longValue() != sequence) {
            seenActionSequence.put(actorId, sequence);
        }
    }
}
