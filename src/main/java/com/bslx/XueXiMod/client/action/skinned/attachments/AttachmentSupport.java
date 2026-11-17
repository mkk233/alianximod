package com.bslx.XueXiMod.client.action.skinned.attachments;

import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;

import com.bslx.XueXiMod.client.action.mesh.PlayerMeshKind;
import com.bslx.XueXiMod.client.action.mesh.PlayerSkinMesh;
import com.bslx.XueXiMod.client.action.mesh.SkinPalette;

import net.minecraft.client.model.Model;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * 一次第三人称预检结果。主路只读 {@link #supported()} 和 {@link #reason()}。
 * 护甲模型和网格只留在本包，供同一帧的层使用，不跨帧保存。
 */
public final class AttachmentSupport {
    private final boolean supported;
    private final String reason;
    private final PlayerMeshKind kind;
    private final long resourceGeneration;
    private final Map<EquipmentSlot, Model> hookedModels;
    private final Map<Model, PlayerSkinMesh> armorMeshes;

    private AttachmentSupport(
            boolean supported,
            String reason,
            PlayerMeshKind kind,
            long resourceGeneration,
            Map<EquipmentSlot, Model> hookedModels,
            Map<Model, PlayerSkinMesh> armorMeshes) {
        this.supported = supported;
        this.reason = reason;
        this.kind = kind;
        this.resourceGeneration = resourceGeneration;
        this.hookedModels = hookedModels;
        this.armorMeshes = armorMeshes;
    }

    static AttachmentSupport of(
            boolean supported,
            String reason,
            PlayerMeshKind kind,
            long resourceGeneration,
            Map<EquipmentSlot, Model> hookedModels,
            Map<Model, PlayerSkinMesh> armorMeshes) {
        EnumMap<EquipmentSlot, Model> modelCopy = new EnumMap<>(EquipmentSlot.class);
        if (hookedModels != null) {
            modelCopy.putAll(hookedModels);
        }
        Map<Model, PlayerSkinMesh> meshCopy = new IdentityHashMap<>();
        if (armorMeshes != null) {
            meshCopy.putAll(armorMeshes);
        }
        return new AttachmentSupport(supported, reason, kind, resourceGeneration, modelCopy, meshCopy);
    }

    public boolean supported() {
        return this.supported;
    }

    public String reason() {
        return this.reason;
    }

    PlayerMeshKind kind() {
        return this.kind;
    }

    long resourceGeneration() {
        return this.resourceGeneration;
    }

    Model hookedModel(EquipmentSlot slot) {
        if (slot == null) {
            return null;
        }
        return this.hookedModels.get(slot);
    }

    PlayerSkinMesh armorMesh(Model model) {
        if (model == null) {
            return null;
        }
        return this.armorMeshes.get(model);
    }

    /**
     * 预检已经成功时，再用实际调色板核对护甲网格。
     * 返回空表示没有新的调色板问题；不支持的预检仍看 {@link #reason()}。
     */
    String paletteReason(SkinPalette palette) {
        if (!this.supported) {
            return null;
        }
        if (palette == null) {
            return "palette-missing";
        }
        for (PlayerSkinMesh mesh : this.armorMeshes.values()) {
            if (mesh != null && mesh.jointLimit() > palette.jointCount()) {
                return "palette";
            }
        }
        return null;
    }

    static Map<EquipmentSlot, Model> emptyModels() {
        return Collections.emptyMap();
    }
}
