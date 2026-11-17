package com.bslx.XueXiMod.client.action.cooperative;

import com.bslx.XueXiMod.shu.technique.release.cooperative.api.WeaponDisplaySnapshot;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 分身手中的武器只是不可交互的渲染副本。不生成物品实体，也不写装备伤害。
 * 握柄相对右臂枢轴，模型空间是 Blockbench [-1,-9,0] 翻到 Y 朝下后的 [-1,9,0] 像素。
 */
public final class CooperativeWeaponCopy {
    private static final float GRIP_ACROSS = -1.0F;
    private static final float GRIP_DOWN = 9.0F;

    private CooperativeWeaponCopy() {
    }

    public static void draw(HumanoidModel<?> model, LivingEntity living, float partialTick, PoseStack poseStack,
                            MultiBufferSource buffer, int packedLight) {
        if (model == null || model.rightArm == null || living == null || poseStack == null || buffer == null) {
            return;
        }
        WeaponDisplaySnapshot snapshot = CooperativeActionViews.weapon(living);
        if (snapshot == null) {
            return;
        }
        ItemStack stack = stackOf(snapshot);
        if (stack.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        ItemRenderer itemRenderer = minecraft.getItemRenderer();
        ModelPart arm = model.rightArm;
        float bodyYaw = Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        CooperativePose pose = CooperativeActionViews.pose(living, partialTick);
        poseStack.mulPose(Axis.YP.rotation(CooperativePoseApplier.rootSpinRadians(pose)));
        appendHeldItem(poseStack, arm, true);
        itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                living.level(),
                living.getId());
        poseStack.popPose();
    }

    /** 握柄在手臂局部空间。右侧是确认握点，左侧只镜像横向。 */
    static void appendHeldItem(PoseStack poseStack, ModelPart arm, boolean rightSide) {
        if (poseStack == null || arm == null) {
            return;
        }
        poseStack.translate(arm.x / 16.0F, arm.y / 16.0F, arm.z / 16.0F);
        if (arm.zRot != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotation(arm.zRot));
        }
        if (arm.yRot != 0.0F) {
            poseStack.mulPose(Axis.YP.rotation(arm.yRot));
        }
        if (arm.xRot != 0.0F) {
            poseStack.mulPose(Axis.XP.rotation(arm.xRot));
        }
        float across = rightSide ? GRIP_ACROSS : -GRIP_ACROSS;
        poseStack.translate(across / 16.0F, GRIP_DOWN / 16.0F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
    }

    private static ItemStack stackOf(WeaponDisplaySnapshot snapshot) {
        Item item = ForgeRegistries.ITEMS.getValue(snapshot.itemId());
        if (item == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item, snapshot.count());
        CompoundTag frozenTag = snapshot.frozenTag();
        if (frozenTag != null) {
            stack.setTag(frozenTag);
        }
        return stack;
    }
}
