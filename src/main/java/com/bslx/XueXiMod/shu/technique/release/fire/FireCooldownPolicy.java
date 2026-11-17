package com.bslx.XueXiMod.shu.technique.release.fire;

/** 资源火伤是否保留 hurt 的 else 分支本来要改写的受击记录。 */
public final class FireCooldownPolicy {
    private FireCooldownPolicy() {
    }

    public static boolean keepExistingHurt(boolean fireResource) {
        return fireResource;
    }
}
