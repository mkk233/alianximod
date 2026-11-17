package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 已经解析的附魔、领域、接续和载体凭据。
 * 攻击适配和分身核心只整包传递，不从这里重算伤害、不读取技能或主手。
 * 链能量使用既有 {@link ReleaseInput}，传递后不得再乘一次倍率。
 */
public final class SettlementEnvelope {
    private final @Nullable EnchantmentSnapshot enchantments;
    private final ReleaseInput chainEnergy;
    private final @Nullable DomainLink domain;
    private final @Nullable HitContinueLink hitContinue;
    private final FrozenCarrierCredential carrier;
    private final List<Integer> enchantChainIndexes;
    private final List<Integer> modifierChainIndexes;

    public SettlementEnvelope(@Nullable EnchantmentSnapshot enchantments, ReleaseInput chainEnergy,
                              @Nullable DomainLink domain, @Nullable HitContinueLink hitContinue,
                              FrozenCarrierCredential carrier, List<Integer> enchantChainIndexes,
                              List<Integer> modifierChainIndexes) {
        this.enchantments = enchantments;
        this.chainEnergy = chainEnergy == null ? ReleaseInput.BASE : chainEnergy;
        this.domain = domain;
        this.hitContinue = hitContinue;
        this.carrier = FrozenValues.present(carrier, "结算载体");
        this.enchantChainIndexes = FrozenValues.list(enchantChainIndexes, "附魔下标");
        this.modifierChainIndexes = FrozenValues.list(modifierChainIndexes, "修饰下标");
    }

    public @Nullable EnchantmentSnapshot enchantments() {
        return enchantments;
    }

    public ReleaseInput chainEnergy() {
        return chainEnergy;
    }

    public @Nullable DomainLink domain() {
        return domain;
    }

    public @Nullable HitContinueLink hitContinue() {
        return hitContinue;
    }

    public FrozenCarrierCredential carrier() {
        return carrier;
    }

    public List<Integer> enchantChainIndexes() {
        return enchantChainIndexes;
    }

    public List<Integer> modifierChainIndexes() {
        return modifierChainIndexes;
    }
}
