package com.bslx.XueXiMod.visual.facing;

/**
 * 持续面向玩家的显式标签。默认不读相机朝向。
 * 这里不引入任何客户端类，服务端也可以安全查询。
 */
public enum CameraFacingTag {
    /** 世界空间或实体自身朝向，不持续面向玩家。 */
    NONE,
    /** 全自由度面向相机平面。 */
    CAMERA_PLANE,
    /** 万剑归宗：剑尖保持竖直，只水平转向镜头。 */
    UPRIGHT_CAMERA_YAW
}
