package com.bslx.XueXiMod.combat.counter;

/** 一次迎击接受哪类直接攻击。调用方传入已经结算好的选择，这里不读花谱。 */
public enum CounterAdmission {
    /** 直接实体与来源实体是同一个存活生物，且伤害类型是明确近战。 */
    MELEE,
    /** 直接实体是弹体，并且有存活的生物主人。 */
    OWNED_PROJECTILE,
    /** 近战或有主弹体，满足其一即可。 */
    DIRECT
}
