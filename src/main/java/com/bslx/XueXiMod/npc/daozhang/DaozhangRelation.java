package com.bslx.XueXiMod.npc.daozhang;

/** 一位道长对一位玩家的关系。缺省记录按中立。 */
public enum DaozhangRelation {
    NEUTRAL,
    APPRENTICE,
    HOSTILE;

    public static DaozhangRelation fromName(String name) {
        if (name == null || name.isBlank()) {
            return NEUTRAL;
        }
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return NEUTRAL;
        }
    }
}
