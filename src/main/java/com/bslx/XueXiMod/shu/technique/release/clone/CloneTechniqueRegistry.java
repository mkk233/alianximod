package com.bslx.XueXiMod.shu.technique.release.clone;

import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueAccess;
import com.bslx.XueXiMod.shu.TechniqueAvailability;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.shu.technique.TechniqueEffectRegistry;
import com.bslx.XueXiMod.shu.technique.release.clone.action.ClonePulseAction;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneKind;

/**
 * 三个真实开发术式。正常命令不走旧效果执行器。
 */
public final class CloneTechniqueRegistry {
    private CloneTechniqueRegistry() {
    }

    public static void registerAll() {
        TechniqueManager.register(phantomTechnique());
        TechniqueManager.register(bodyTechnique());
        TechniqueManager.register(pulseTechnique());
        registerDefense(CloneBalance.PHANTOM_ID);
        registerDefense(CloneBalance.BODY_ID);
        registerDefense(CloneBalance.PULSE_ID);
        ClonePulseAction.register();
    }

    public static CloneSpawnProfile profile(CloneKind kind) {
        if (kind == CloneKind.PHANTOM) {
            return new CloneSpawnProfile(CloneBalance.PHANTOM_ID, CloneKind.PHANTOM, CloneBalance.PULSE_ID);
        }
        if (kind == CloneKind.NORMAL) {
            return new CloneSpawnProfile(CloneBalance.BODY_ID, CloneKind.NORMAL, CloneBalance.PULSE_ID);
        }
        throw new IllegalArgumentException("分身类型不能为空");
    }

    public static Technique technique(CloneKind kind) {
        return TechniqueManager.getById(profile(kind).techniqueId());
    }

    public static Technique pulse() {
        return TechniqueManager.getById(CloneBalance.PULSE_ID);
    }

    public static String actionKey() {
        return CloneBalance.PULSE_ID;
    }

    private static void registerDefense(String techniqueId) {
        TechniqueEffectRegistry.register(techniqueId, (serverPlayer, listedTechnique, heldWeapon, castContext) -> {
            throw new IllegalStateException(TechniqueAccess.DEVELOPMENT_ENTRY_ONLY);
        });
    }

    private static Technique phantomTechnique() {
        return new Technique(
                CloneBalance.PHANTOM_ID,
                "幻影分身·基础",
                TechniqueType.MAGIC,
                CloneBalance.RAW_SPAWN_DAMAGE,
                CloneBalance.SPAWN_COOLDOWN_TICKS,
                CloneBalance.LEVEL_CAP,
                spawnDescription(),
                CloneBalance.COMPLEXITY,
                ShuAttribute.NONE,
                CloneBalance.PROJECTILE_SPEED,
                false,
                TechniqueAvailability.DEVELOPMENT_ONLY);
    }

    private static Technique bodyTechnique() {
        return new Technique(
                CloneBalance.BODY_ID,
                "普通分身",
                TechniqueType.MAGIC,
                CloneBalance.RAW_SPAWN_DAMAGE,
                CloneBalance.SPAWN_COOLDOWN_TICKS,
                CloneBalance.LEVEL_CAP,
                "单独装入伞槽 0 后释放。潜行释放撤回自己的普通分身。和其他术式放在一起会在扣费前拒绝。",
                CloneBalance.COMPLEXITY,
                ShuAttribute.NONE,
                CloneBalance.PROJECTILE_SPEED,
                false,
                TechniqueAvailability.PLAYER);
    }

    private static Technique pulseTechnique() {
        return new Technique(
                CloneBalance.PULSE_ID,
                "分身基础脉冲",
                TechniqueType.MAGIC,
                CloneBalance.RAW_PULSE_DAMAGE,
                CloneBalance.PULSE_COOLDOWN_TICKS,
                CloneBalance.LEVEL_CAP,
                pulseDescription(),
                CloneBalance.COMPLEXITY,
                ShuAttribute.NONE,
                CloneBalance.PROJECTILE_SPEED,
                false,
                TechniqueAvailability.DEVELOPMENT_ONLY);
    }

    private static String spawnDescription() {
        return "开发命令入口，默认不装配。代码默认耗魔 "
                + CloneBalance.SPAWN_MANA_COST
                + "、冷却 "
                + CloneBalance.SPAWN_COOLDOWN_TICKS
                + " 刻、寿命 "
                + CloneBalance.LIFETIME_TICKS
                + " 刻；寿命不大于 0 时没有自然到期。生命 "
                + CloneBalance.BODY_HEALTH
                + "，诱敌半径 "
                + CloneBalance.DECOY_RADIUS
                + " 格。世界配置下次开世界生效。本局实扣以命令回执为准。";
    }

    private static String pulseDescription() {
        return "开发命令入口，默认不装配。代码默认耗魔 "
                + CloneBalance.PULSE_MANA_COST
                + "、冷却 "
                + CloneBalance.PULSE_COOLDOWN_TICKS
                + " 刻、固定伤害 "
                + CloneBalance.FIXED_DAMAGE
                + "、射程 "
                + CloneBalance.PULSE_RANGE
                + " 格。调整伤害不改变默认耗魔。世界配置下次开世界生效。本局实扣以命令回执为准。";
    }

    public record CloneSpawnProfile(String techniqueId, CloneKind kind, String actionKey) {
    }
}
