package com.bslx.XueXiMod.shu.technique.release.prison.deflection;

import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionAttack;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHost;

/**
 * 被困者本人的向外攻击。友方放行不能把这一下也放走。
 * 其他宿主保持原样。
 */
public final class PrisonStrike {
    private PrisonStrike() {
    }

    /** 这一下是不是被困者本人打向水牢壳外。 */
    public static boolean confinesActor(DeflectionHost host, DeflectionAttack attack) {
        if (!(host instanceof PrisonDeflectionHost prison) || attack == null || attack.actor() == null) {
            return false;
        }
        return prison.confines(attack.actor().getUUID());
    }
}
