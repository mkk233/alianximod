package com.bslx.XueXiMod.shu.technique.release.domain.motion;

/**
 * 持续流场角色合同。只标记牵引、环绕或气泡，不保存位移，也不管理领域寿命。
 * 球形场地的牵引直接改速度，护体不产生这三种流场。
 */
public enum FlowRole {
    PULL,
    ORBIT,
    BUBBLE
}
