package com.bslx.XueXiMod.client.action.cooperative;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeTechniqueIds;

import net.minecraft.resources.ResourceLocation;

/**
 * 斜斩斩光的表现标识和已确认贴图。
 * 横劈不使用这张贴图。协同规则本身也没有斩光。
 */
public final class CooperativeSlashPresentations {
    public static final ResourceLocation PRESENTATION =
            ResourceLocation.fromNamespaceAndPath(XueXiMod.MOD_ID, "oblique_slash_v1");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            XueXiMod.MOD_ID, "textures/entity/xueximod/oblique_slash_v1.png");
    public static final float AUTHORED_WINDOW_SECONDS = 0.2F;
    public static final float PHASE_END_SCALE = 0.02F;

    private CooperativeSlashPresentations() {
    }

    public static boolean matchesTechnique(String techniqueId) {
        return CooperativeTechniqueIds.OBLIQUE_SLASH.equals(techniqueId);
    }

    public static boolean matchesTexture(String textureId) {
        if (textureId == null || textureId.isEmpty()) {
            return false;
        }
        return TEXTURE.toString().equals(textureId) || PRESENTATION.toString().equals(textureId);
    }
}
