package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastSpec;

import java.util.List;

/**
 * 剑术是否带上固定数量至少为 8 的多重声明。
 * 只读这一条术式或这一条链的多重声明，不拼四条入口，也不写玩家状态。
 */
public final class SwordMulticastGate {
    public static final String LIMIT_KEY = "gui.xueximod.sword.multicast_limit";
    private static final int RESTRICTED_FIXED_COUNT = 8;

    private SwordMulticastGate() {
    }

    public static boolean restrictsSword(Technique technique) {
        if (technique == null) {
            return false;
        }
        return restrictsDeclared(ReleaseRegistry.get(technique.getId()));
    }

    public static boolean restrictsSwordChain(List<Technique> techniques) {
        if (techniques == null) {
            return false;
        }
        for (Technique listed : techniques) {
            if (restrictsSword(listed)) {
                return true;
            }
        }
        return false;
    }

    private static boolean restrictsDeclared(ReleaseDefinition definition) {
        if (definition == null) {
            return false;
        }
        MulticastSpec declared = definition.multicastSpec();
        if (declared == null || declared.kind() != MulticastSpec.Kind.FIXED) {
            return false;
        }
        return declared.fixedCount() >= RESTRICTED_FIXED_COUNT;
    }
}
