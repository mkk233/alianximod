package com.bslx.XueXiMod.shu.technique.release.domain;

/**
 * 根攻击种类合同。只给附加伤害判断这一击该不该拿领域供给，不持有实例，也没有生命周期。
 * 球形场地和护体不读这份种类。
 */
public final class AttackEnchantmentContext {
    public enum Kind {
        ROOT_MELEE,
        ROOT_ARROW,
        ROOT_TECHNIQUE,
        CONTACT,
        NATIVE_DOT,
        STATUS_DERIVED
    }

    private AttackEnchantmentContext() {
    }

    /** 近战、射箭和术式这三种根攻击才可以取领域附加伤害。不扣资源，其余种类直接拒绝。 */
    public static boolean grantsAura(Kind kind) {
        return kind == Kind.ROOT_MELEE || kind == Kind.ROOT_ARROW || kind == Kind.ROOT_TECHNIQUE;
    }
}
