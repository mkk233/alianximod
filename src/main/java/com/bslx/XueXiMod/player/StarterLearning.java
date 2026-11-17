package com.bslx.XueXiMod.player;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueManager;
import net.minecraft.world.entity.player.Player;

/** 新档只自动学会四门引路术。已经学会的旧档内容不删除。 */
public final class StarterLearning {
    public static final String[] STARTERS = {
            "sword_thrust",
            "movement_dash",
            "magic_arcane_missile",
            "rule_cast_anchor"
    };

    private StarterLearning() {
    }

    public static void grantMissing(Player player) {
        if (player == null) {
            return;
        }
        PlayerShuData data = PlayerShuData.get(player);
        boolean changed = false;
        for (String techniqueId : STARTERS) {
            Technique technique = TechniqueManager.getById(techniqueId);
            if (technique == null || data.hasLearned(techniqueId)) {
                continue;
            }
            data.learnTechnique(techniqueId);
            changed = true;
        }
        if (changed) {
            PlayerShuData.save(player, data);
        }
    }
}
