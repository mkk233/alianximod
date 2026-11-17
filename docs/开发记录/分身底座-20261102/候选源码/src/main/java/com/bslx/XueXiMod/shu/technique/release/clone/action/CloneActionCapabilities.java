package com.bslx.XueXiMod.shu.technique.release.clone.action;

/**
 * 一个动作对分身身体、主人资源和调度隔离的声明。
 * 未知动作不自动获得这些能力。
 */
public record CloneActionCapabilities(
        boolean actorOrigin,
        boolean actorDirection,
        boolean ownerResources,
        boolean ownerBodyUntouched,
        boolean ownerHandUntouched,
        boolean broadcastSuppressed,
        boolean delayIsolated) {
    public static CloneActionCapabilities full() {
        return new CloneActionCapabilities(true, true, true, true, true, true, true);
    }

    /** 本动作是否覆盖调用方要求的每一项。要求为 false 的项不参与。 */
    public boolean covers(CloneActionCapabilities required) {
        if (required == null) {
            return false;
        }
        return (!required.actorOrigin || actorOrigin)
                && (!required.actorDirection || actorDirection)
                && (!required.ownerResources || ownerResources)
                && (!required.ownerBodyUntouched || ownerBodyUntouched)
                && (!required.ownerHandUntouched || ownerHandUntouched)
                && (!required.broadcastSuppressed || broadcastSuppressed)
                && (!required.delayIsolated || delayIsolated);
    }
}
