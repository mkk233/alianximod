package com.bslx.XueXiMod.shu.technique;

import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.ShuAttribute;

public interface EnchantmentDefinition {
    String id();
    ShuAttribute element();
    EnchantmentAccumulator createAccumulator();
    EnchantmentAccumulator fold(ElementReaction reaction, EnchantmentAccumulator current);

    default int magicDamage(ElementReaction reaction) {
        return 0;
    }

    /**
     * 领域术式自身还能给圈内创建者的下一击附上的载荷。
     * 返回空表示只有领域本体，不给攻击附魔；普通附魔不靠这个方法变成领域。
     */
    default String attackPayloadId() {
        return "";
    }
}
