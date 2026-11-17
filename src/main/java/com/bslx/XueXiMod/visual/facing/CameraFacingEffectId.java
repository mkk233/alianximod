package com.bslx.XueXiMod.visual.facing;

/**
 * 不走 {@code BatchVisualTexture} 的独立特效标识。
 * 未列入登记表时一律视为不持续面向玩家。
 */
public enum CameraFacingEffectId {
    /** 焚天天空火云和自然结束余烬。地面燎原与十字火舌不使用它。 */
    FENTIAN_SKY_FLAME,
    /** 火种余额环绕身体的小火苗。 */
    FIRE_SEED_ORBIT_FLAME,
    /** 水牢单张球壳。 */
    WATER_PRISON_SHELL,
    /** 万剑归宗漩涡环绕阶段。 */
    SWORD_RETURN_ALL_VORTEX
}
