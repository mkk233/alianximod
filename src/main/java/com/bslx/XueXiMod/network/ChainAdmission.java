package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.Technique;

import java.util.List;

/** 装配入口可见的原始链校验。实际规则仍在链段计算里。 */
public final class ChainAdmission {
    private ChainAdmission() {
    }

    /** 这条原始链不能装配时返回原因，可以装配时返回空。 */
    public static String refusal(List<Technique> techniques) {
        return SkillSegmentMath.mixedChainRefusal(techniques);
    }

    /**
     * 已经存下的链能否写成这份候选。
     * 合法候选放行。旧非法链只允许原顺序拆除，不允许加入或改序后仍非法。
     */
    public static boolean permitsStoredChange(List<Technique> stored, List<Technique> proposed) {
        if (refusal(proposed) == null) {
            return true;
        }
        return retainsStoredOrder(stored, proposed);
    }

    private static boolean retainsStoredOrder(List<Technique> stored, List<Technique> proposed) {
        int cursor = 0;
        int storedCount = stored == null ? 0 : stored.size();
        if (proposed == null) {
            return true;
        }
        for (Technique technique : proposed) {
            if (technique == null) {
                continue;
            }
            boolean found = false;
            while (stored != null && cursor < storedCount) {
                Technique earlier = stored.get(cursor++);
                if (earlier != null && earlier.getId().equals(technique.getId())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }
}
