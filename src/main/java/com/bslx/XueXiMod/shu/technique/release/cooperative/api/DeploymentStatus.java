package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/** 独立部署的明确结果。除 SUCCESS 外都不留下新的身体。 */
public enum DeploymentStatus {
    SUCCESS,
    ILLEGAL_STRUCTURE,
    CARRIER_MISSING,
    QUOTA,
    COOLDOWN,
    UNSAFE_SPAWN,
    PAYMENT_FAILED,
    PUBLISH_FAILED,
    PERMISSION
}
