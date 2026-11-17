package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;

/**
 * 旧的逐目标接触单价。领域额外附魔已改用各附魔自己的配置价，整域每 10 刻收一次。
 * 这个常量不再参与领域结算。
 */
public final class DomainContactCharge {
    private DomainContactCharge() {
    }

    /** 旧接触账的每种附魔单价。领域节拍不再读取它。 */
    public static final int MANA_PER_BASIC_ENCHANTMENT = 1;

    /** 旧报价保留为 0，避免新的调用再按目标种类收费。 */
    public static int quote(EnchantmentSnapshot snapshot) {
        return 0;
    }
}
