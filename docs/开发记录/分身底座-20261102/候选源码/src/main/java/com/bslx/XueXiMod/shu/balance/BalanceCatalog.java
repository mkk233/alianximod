package com.bslx.XueXiMod.shu.balance;

import com.bslx.XueXiMod.shu.ModTechniques;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.release.clone.CloneBalance;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.technique.EnchantmentDefinitions;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 内容事件之后补标准键，再按合同调用 B、C，最后封存。
 * 计算器只读术式原始 getter，不进入 {@link BalanceService}。
 */
public final class BalanceCatalog {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> BUILTINS = new LinkedHashSet<>();
    private static final Set<String> DAMAGE_EXCLUDED = Set.of("sword_come", "sword_return_all");
    /**
     * 这些编号会形成一笔可缩放伤害，或由 B/C 按合同读取伤害倍率。
     * 零基础伤害的火术、水牢和进入公式的附魔也在这里，不靠 baseDamage 猜测。
     */
    private static final Set<String> DAMAGE_HOSTS = Set.of(
            "sword_thrust",
            "sword_horizontal_slash",
            "sword_vertical_slash",
            "movement_dash",
            "magic_arcane_missile",
            "test_sustained",
            "heat_beam",
            "space_cut",
            "parasite_seed",
            "tide_sever",
            "stone_hammer",
            "metal_needle",
            "fire_orb",
            "fire_burst",
            "water_yielding_ribbon",
            "water_weeping_arc",
            "water_blade",
            "fire_meteorite",
            "fire_shock_ring",
            "fire_spark",
            "phantom_sword",
            "water_prison",
            "fire_liaoyuan",
            "fire_fentian",
            "fire_liaoli",
            "fire_scorch_field",
            "fire_kindle",
            "fire_detonate",
            "fire_cool",
            "tide_mark",
            "vine_mark",
            "sinking_edge",
            "keen_mark",
            "magic_ignite",
            CloneBalance.PULSE_ID);

    private BalanceCatalog() {
    }

    public static void noteBuiltins(Iterable<Technique> techniques) {
        BUILTINS.clear();
        if (techniques == null) {
            return;
        }
        for (Technique technique : techniques) {
            if (technique == null || technique.getId() == null) {
                continue;
            }
            BUILTINS.add(BalanceRegistry.canonicalize(technique.getId()));
        }
    }

    public static void sealAfterContent() {
        List<String> registered = new ArrayList<>();
        for (Technique technique : TechniqueManager.getAll()) {
            if (technique == null || technique.getId() == null) {
                continue;
            }
            String techniqueId = BalanceRegistry.canonicalize(technique.getId());
            registered.add(techniqueId);
            declareStandards(technique, techniqueId);
        }
        ModTechniques.declareSpecialBalances();
        EnchantmentDefinitions.declareBalance(BalanceRegistry.current());
        BalanceRegistry.seal(registered);
    }

    private static void declareStandards(Technique technique, String techniqueId) {
        boolean builtin = BUILTINS.contains(techniqueId);
        boolean includeDamage = DAMAGE_HOSTS.contains(techniqueId) && !DAMAGE_EXCLUDED.contains(techniqueId);
        if (!builtin && technique.getBaseDamage() > 0f && !includeDamage) {
            includeDamage = true;
            LOGGER.warn("外部术式 {} 只补标准伤害倍率，专项参数尚未声明", techniqueId);
        } else if (!builtin) {
            LOGGER.warn("外部术式 {} 已补标准冷却和魔力，专项参数尚未声明", techniqueId);
        }
        if (builtin && technique.getBaseDamage() > 0f
                && !DAMAGE_HOSTS.contains(techniqueId) && !DAMAGE_EXCLUDED.contains(techniqueId)) {
            LOGGER.warn("内置术式 {} 基础伤害大于 0，但没有列入标准伤害倍率", techniqueId);
        }
        addCooldown(techniqueId);
        addMana(techniqueId);
        if (includeDamage) {
            addDamage(techniqueId);
        }
    }

    private static void addCooldown(String techniqueId) {
        if (BalanceRegistry.isDeclared(techniqueId, BalanceKeys.COOLDOWN_TICKS)) {
            LOGGER.warn("术式 {} 已有 cooldownTicks，标准声明不覆盖", techniqueId);
            return;
        }
        BalanceRegistry.completeStandard(TechniqueBalanceSpec.builder(techniqueId)
                .cooldownTicks(RawDefault.calculator(BalanceCatalog::cooldownOf))
                .build());
    }

    private static void addMana(String techniqueId) {
        if (BalanceRegistry.isDeclared(techniqueId, BalanceKeys.MANA_COST)) {
            LOGGER.warn("术式 {} 已有 manaCost，标准声明不覆盖", techniqueId);
            return;
        }
        BalanceRegistry.completeStandard(TechniqueBalanceSpec.builder(techniqueId)
                .manaCost(RawDefault.calculator(BalanceCatalog::manaOf))
                .build());
    }

    private static void addDamage(String techniqueId) {
        if (BalanceRegistry.isDeclared(techniqueId, BalanceKeys.DAMAGE_MULTIPLIER)) {
            LOGGER.warn("术式 {} 已有 damageMultiplier，标准声明不覆盖", techniqueId);
            return;
        }
        BalanceRegistry.completeStandard(TechniqueBalanceSpec.builder(techniqueId)
                .damageMultiplier()
                .build());
    }

    private static double cooldownOf(String techniqueId) {
        Technique technique = TechniqueManager.getById(techniqueId);
        if (technique == null) {
            return 0.0d;
        }
        return technique.getBaseCooldown();
    }

    private static double manaOf(String techniqueId) {
        Technique technique = TechniqueManager.getById(techniqueId);
        if (technique == null) {
            return 0.0d;
        }
        return technique.manaCost();
    }
}
