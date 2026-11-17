package com.bslx.XueXiMod.shu.technique.release.conditional;

import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;

/** 读取条件释放能力。没有该能力时两个开关都是关。 */
public final class ConditionalEffects {
    private ConditionalEffects() {
    }

    public static boolean copiesCoalesce(ReleaseDefinition definition) {
        ConditionalEffect effect = definition == null ? null : definition.conditionalEffect();
        return effect != null && effect.copiesCoalesce();
    }

    public static boolean supportsContact(ReleaseDefinition definition) {
        ConditionalEffect effect = definition == null ? null : definition.conditionalEffect();
        return effect != null && effect.supportsContact();
    }
}
