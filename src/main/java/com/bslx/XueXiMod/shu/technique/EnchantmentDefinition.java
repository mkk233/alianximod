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
}
