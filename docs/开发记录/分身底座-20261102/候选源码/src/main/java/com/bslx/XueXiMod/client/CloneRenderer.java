package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 两种分身共用原版僵尸人体层和原版僵尸贴图。不读取玩家皮肤。
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
    public void render(CloneEntity cloneActor, float bodyYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        if (cloneActor != null) {
            noteSequence(cloneActor);
        }
        super.render(cloneActor, bodyYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CloneEntity cloneActor) {
        return VANILLA_ZOMBIE_TEXTURE;
    }

    private void noteSequence(CloneEntity cloneActor) {
        UUID actorId = cloneActor.getUUID();
        if (cloneActor.isRemoved()) {
            seenActionSequence.remove(actorId);
            return;
        }
        long sequence = cloneActor.actionSequence();
        Long seen = seenActionSequence.get(actorId);
        if (seen != null && seen.longValue() != sequence) {
            cloneActor.swing(InteractionHand.MAIN_HAND);
        }
        if (seen == null || seen.longValue() != sequence) {
            seenActionSequence.put(actorId, sequence);
        }
    }
}
