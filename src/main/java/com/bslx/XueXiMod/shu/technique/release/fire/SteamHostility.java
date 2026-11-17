package com.bslx.XueXiMod.shu.technique.release.fire;

/** 蒸汽打谁。队友和驯服关系先排除，再看项目里的敌对策略。 */
public final class SteamHostility {
    private SteamHostility() {
    }

    public static boolean struck(boolean self, boolean sameTeam, boolean tamedByCasterOrTeammate, boolean relationAccepts) {
        if (self || sameTeam || tamedByCasterOrTeammate) {
            return false;
        }
        return relationAccepts;
    }
}
