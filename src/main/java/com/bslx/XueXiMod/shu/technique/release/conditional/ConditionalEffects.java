package com.bslx.XueXiMod.shu.technique.release.conditional;

import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;

/** 读取条件释放能力。没有该能力时两个开关都是关。 */
public final class ConditionalEffects {
    private ConditionalEffects() {
    }

    /** 这个释放是否把复制份合并成一次条件尝试。 */
    public static boolean copiesCoalesce(ReleaseDefinition definition) {
        ConditionalEffect effect = definition == null ? null : definition.conditionalEffect();
        return effect != null && effect.copiesCoalesce();
    }

    /** 这个零伤宿主是否允许接触附魔。 */
    public static boolean supportsContact(ReleaseDefinition definition) {
        ConditionalEffect effect = definition == null ? null : definition.conditionalEffect();
        return effect != null && effect.supportsContact();
    }
}
