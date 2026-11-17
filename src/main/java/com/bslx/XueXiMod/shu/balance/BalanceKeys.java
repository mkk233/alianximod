package com.bslx.XueXiMod.shu.balance;

import java.util.Set;

/**
 * 标准键、全局键和专项键的共用拼写。
 * 常量本身不会登记任何术式；只有声明之后才会进入目录。
 * 专项键使用小驼峰，不用下划线、中文名或制作表编号。
 */
public final class BalanceKeys {
    public static final String DAMAGE_MULTIPLIER = "damageMultiplier";
    public static final String MANA_MULTIPLIER = "manaMultiplier";
    public static final String COOLDOWN_MULTIPLIER = "cooldownMultiplier";
    public static final String MANA_COST = "manaCost";
    public static final String COOLDOWN_TICKS = "cooldownTicks";

    public static final String LIFETIME_TICKS = "lifetimeTicks";
    public static final String MOTION_SPEED = "motionSpeed";
    public static final String HIT_SIZE = "hitSize";
    public static final String THICKNESS = "thickness";
    public static final String BEAM_LENGTH = "beamLength";
    public static final String HIT_INTERVAL_TICKS = "hitIntervalTicks";
    public static final String SPAWN_MANA_COST = "spawnManaCost";
    public static final String UPKEEP_MANA_COST = "upkeepManaCost";
    public static final String UPKEEP_INTERVAL_TICKS = "upkeepIntervalTicks";
    public static final String RADIUS = "radius";
    public static final String EFFECT_RADIUS = "effectRadius";
    public static final String DAMAGE_REDUCTION = "damageReduction";
    public static final String SPEED_DECAY = "speedDecay";
    public static final String SEARCH_RANGE = "searchRange";
    public static final String SCAN_RANGE = "scanRange";
    public static final String SPREAD_RANGE = "spreadRange";
    public static final String PRISON_DURATION_TICKS = "prisonDurationTicks";
    public static final String PRESSURE_INTERVAL_TICKS = "pressureIntervalTicks";
    public static final String SEED_COUNT = "seedCount";
    public static final String SEED_INTERVAL_TICKS = "seedIntervalTicks";
    public static final String SEGMENT_DAMAGE = "segmentDamage";
    public static final String SEGMENT_INTERVAL_TICKS = "segmentIntervalTicks";
    public static final String SEGMENT_COUNT = "segmentCount";
    public static final String DURATION_TICKS = "durationTicks";
    public static final String CONTROL_DURATION_TICKS = "controlDurationTicks";
    public static final String TIDE_COUNT = "tideCount";
    public static final String DETONATE_RATIO = "detonateRatio";
    public static final String BOX_WIDTH = "boxWidth";
    public static final String BOX_HEIGHT = "boxHeight";
    public static final String BOX_DEPTH = "boxDepth";
    public static final String ATTRACTION_RADIUS = "attractionRadius";
    public static final String BODY_RADIUS = "bodyRadius";
    public static final String LIFTED_HEIGHT = "liftedHeight";
    public static final String PULL_SPEED = "pullSpeed";
    public static final String ORBIT_ANGULAR_SPEED = "orbitAngularSpeed";
    public static final String OCCUPIED_VOLUME_LIMIT = "occupiedVolumeLimit";
    public static final String LIFTED_OCCUPIED_VOLUME_LIMIT = "liftedOccupiedVolumeLimit";
    public static final String ENTRY_COST = "entryCost";
    public static final String WET_TIER_1_ENTRY_COST = "wetTier1EntryCost";
    public static final String WET_TIER_2_ENTRY_COST = "wetTier2EntryCost";
    public static final String WET_TIER_3_REFUND = "wetTier3Refund";
    public static final String WET_TIER_4_REFUND = "wetTier4Refund";
    public static final String FALLBACK_PROJECTILE_STANDARD_COST = "fallbackProjectileStandardCost";
    public static final String RISE_SPEED = "riseSpeed";
    public static final String EJECT_HORIZONTAL_SPEED = "ejectHorizontalSpeed";
    public static final String EJECT_VERTICAL_SPEED = "ejectVerticalSpeed";

    private static final Set<String> STRUCTURAL_KEYS = Set.of(
            "baseDamage",
            "projectileSpeed",
            "chainRole",
            "replicationCount",
            "multicastCount",
            "copyCount",
            "entityCount",
            "settlementOrder",
            "dedupe",
            "hitDedupe",
            "assemblyCost",
            "unlockLevel",
            "complexity",
            "levelCap",
            "manaMultiplier",
            "cooldownMultiplier",
            "geometryEnergy",
            "chainEnergyScale");

    private BalanceKeys() {
    }

    /** 是不是冷却、魔力或伤害倍率这三把标准术式键。 */
    public static boolean isStandardTechniqueKey(String parameterKey) {
        return DAMAGE_MULTIPLIER.equals(parameterKey)
                || MANA_COST.equals(parameterKey)
                || COOLDOWN_TICKS.equals(parameterKey);
    }

    /** 键必须是 3 到 48 位小驼峰，且不能拿结构机制当平衡参数。 */
    public static void checkDeclarable(String parameterKey) {
        if (parameterKey == null || parameterKey.length() < 3 || parameterKey.length() > 48) {
            throw new BalanceDeclarationException("参数键长度必须在 3 到 48 之间");
        }
        for (int index = 0; index < parameterKey.length(); index++) {
            char symbol = parameterKey.charAt(index);
            if (index == 0) {
                if (symbol < 'a' || symbol > 'z') {
                    throw new BalanceDeclarationException("参数键必须以小写字母开头: " + parameterKey);
                }
                continue;
            }
            boolean letter = (symbol >= 'a' && symbol <= 'z') || (symbol >= 'A' && symbol <= 'Z');
            boolean digit = symbol >= '0' && symbol <= '9';
            if (!letter && !digit) {
                throw new BalanceDeclarationException("参数键只能包含字母和数字: " + parameterKey);
            }
        }
        if (STRUCTURAL_KEYS.contains(parameterKey)) {
            throw new BalanceDeclarationException("结构机制不能作为平衡参数: " + parameterKey);
        }
    }
}
