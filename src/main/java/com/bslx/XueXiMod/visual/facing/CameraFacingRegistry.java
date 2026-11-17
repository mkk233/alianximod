package com.bslx.XueXiMod.visual.facing;

import com.bslx.XueXiMod.carrier.BatchVisualTexture;

/**
 * 持续面向玩家的唯一登记表。没有显式登记的贴图或特效返回 {@link CameraFacingTag#NONE}。
 */
public final class CameraFacingRegistry {
    private CameraFacingRegistry() {
    }

    public static CameraFacingTag tagOf(BatchVisualTexture texture) {
        if (texture == null) {
            return CameraFacingTag.NONE;
        }
        return switch (texture) {
            case SEED, KINDLE, DUST -> CameraFacingTag.CAMERA_PLANE;
            case SPARK, TRAIL, SURFACE, SHOCK, DETONATE, NONE -> CameraFacingTag.NONE;
        };
    }

    public static CameraFacingTag tagOf(CameraFacingEffectId effectId) {
        if (effectId == null) {
            return CameraFacingTag.NONE;
        }
        return switch (effectId) {
            case FENTIAN_SKY_FLAME, FIRE_SEED_ORBIT_FLAME, WATER_PRISON_SHELL -> CameraFacingTag.CAMERA_PLANE;
            case SWORD_RETURN_ALL_VORTEX -> CameraFacingTag.UPRIGHT_CAMERA_YAW;
        };
    }
}
