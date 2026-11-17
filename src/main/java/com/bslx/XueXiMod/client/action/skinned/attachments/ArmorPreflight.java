package com.bslx.XueXiMod.client.action.skinned.attachments;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.bslx.XueXiMod.client.action.mesh.PlayerMeshKind;
import com.bslx.XueXiMod.client.action.mesh.PlayerSkinMesh;
import com.bslx.XueXiMod.client.action.mesh.SkinDiagnostic;
import com.bslx.XueXiMod.mixin.client.skin.HumanoidArmorLayerAccess;
import com.bslx.XueXiMod.mixin.client.skin.LivingRendererLayersAccessor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;

/** 身体绘制前检查本帧原版护甲。只调用一次 Forge 护甲模型钩子，并在返回前恢复被改过的基础模型。 */
final class ArmorPreflight {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] REQUIRED_JOINTS = {
            "Head", "Chest", "Torso",
            "Arm_R", "Hand_R", "Elbow_R", "Arm_L", "Hand_L", "Elbow_L",
            "Thigh_R", "Leg_R", "Knee_R", "Thigh_L", "Leg_L", "Knee_L",
            "Tool_R", "Tool_L"
    };

    private ArmorPreflight() {
    }

    static String shapeReason(Model model) {
        if (!(model instanceof HumanoidModel<?>)) {
            return "armor-model";
        }
        return null;
    }

    static AttachmentSupport inspect(
            AbstractClientPlayer player,
            PlayerRenderer renderer,
            PlayerMeshKind kind,
            long resourceGeneration) {
        if (player == null) {
            return AttachmentSupport.of(false, "player-missing", kind, resourceGeneration, Map.of(), Map.of());
        }
        if (renderer == null) {
            return AttachmentSupport.of(false, "renderer-missing", kind, resourceGeneration, Map.of(), Map.of());
        }
        if (kind == null) {
            return AttachmentSupport.of(false, "body-kind", kind, resourceGeneration, Map.of(), Map.of());
        }
        if (resourceGeneration < 0L) {
            return AttachmentSupport.of(false, "generation", kind, resourceGeneration, Map.of(), Map.of());
        }
        BindJointTable table = BindJointTable.published(resourceGeneration);
        if (table == null) {
            return AttachmentSupport.of(false, "bind-armature-missing", kind, resourceGeneration, Map.of(), Map.of());
        }
        for (String jointName : REQUIRED_JOINTS) {
            if (!table.has(jointName)) {
                return AttachmentSupport.of(false, "bind-joint-missing", kind, resourceGeneration, Map.of(), Map.of());
            }
        }
        if (!(renderer instanceof LivingRendererLayersAccessor layers)) {
            return AttachmentSupport.of(false, "armor-layer-access", kind, resourceGeneration, Map.of(), Map.of());
        }
        HumanoidArmorLayerAccess armorAccess = null;
        for (Object layer : layers.xuexiLayers()) {
            if (layer instanceof HumanoidArmorLayerAccess access) {
                armorAccess = access;
                break;
            }
        }
        if (armorAccess == null) {
            return AttachmentSupport.of(false, "armor-layer-missing", kind, resourceGeneration, Map.of(), Map.of());
        }
        boolean anyArmor = false;
        boolean supported = true;
        String reason = "armor-clear";
        Map<EquipmentSlot, Model> hooked = new EnumMap<>(EquipmentSlot.class);
        Map<Model, PlayerSkinMesh> meshes = new IdentityHashMap<>();
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ArmorItem armorItem) || armorItem.getEquipmentSlot() != slot) {
                continue;
            }
            anyArmor = true;
            boolean innerLayer = armorAccess.xuexiUsesInnerModel(slot);
            HumanoidModel<?> base = innerLayer ? armorAccess.xuexiInnerModel() : armorAccess.xuexiOuterModel();
            List<PartRest.SavedPart> saved = captureHumanoid(base);
            copyPose(renderer, base);
            Model hookedModel = ForgeHooksClient.getArmorModel(player, stack, slot, base);
            PartRest.restore(saved);
            if (hookedModel != null) {
                hooked.put(slot, hookedModel);
            }
            String shape = shapeReason(hookedModel);
            if (shape != null) {
                supported = false;
                reason = shape;
                continue;
            }
            try {
                ArmorMeshes.ArmorExtract extract = ArmorMeshes.extract((HumanoidModel<?>) hookedModel, table, kind, innerLayer, resourceGeneration);
                meshes.put(hookedModel, extract.mesh);
                if (supported && "armor-clear".equals(reason)) {
                    reason = "armor-ready";
                }
            } catch (SkinDiagnostic diagnostic) {
                supported = false;
                reason = diagnostic.reasonCode();
            } catch (RuntimeException failure) {
                supported = false;
                reason = "armor-extract";
            }
        }
        if (!anyArmor) {
            reason = "armor-clear";
            supported = true;
        }
        return AttachmentSupport.of(supported, reason, kind, resourceGeneration, hooked, meshes);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void copyPose(PlayerRenderer renderer, HumanoidModel<?> base) {
        ((HumanoidModel) renderer.getModel()).copyPropertiesTo(base);
    }

    private static List<PartRest.SavedPart> captureHumanoid(HumanoidModel<?> model) {
        List<PartRest.SavedPart> saved = new ArrayList<>();
        saved.addAll(PartRest.capture(model.head));
        saved.addAll(PartRest.capture(model.hat));
        saved.addAll(PartRest.capture(model.body));
        saved.addAll(PartRest.capture(model.rightArm));
        saved.addAll(PartRest.capture(model.leftArm));
        saved.addAll(PartRest.capture(model.rightLeg));
        saved.addAll(PartRest.capture(model.leftLeg));
        return saved;
    }
}
