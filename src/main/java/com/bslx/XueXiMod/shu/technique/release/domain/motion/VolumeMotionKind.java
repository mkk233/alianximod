package com.bslx.XueXiMod.shu.technique.release.domain.motion;

/**
 * 体积运动种类合同。只区分无运动、旋流宿主和上升气流，不持有运行状态。
 * 球形场地不挂这三种体积运动，护体只维护随身环。
 */
public enum VolumeMotionKind {
    NONE,
    VORTEX_HOST,
    UPDRAFT
}
