package com.bslx.XueXiMod.player;

import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public class PlayerBoundSkills {
    private static final String BOUND_KEY = "xueximod_bound_skills";

    public static Skill get(ServerPlayer player, int slot) {
        if (slot < 0 || slot > 3) return null;
        CompoundTag root = player.getPersistentData();
        if (!root.contains(BOUND_KEY)) return null;
        CompoundTag bound = root.getCompound(BOUND_KEY);
        String key = "BoundSkill_" + slot;
        if (!bound.contains(key)) return null;
        Skill skill = Skill.fromNbt(bound.getCompound(key));
        if (skill == null || skill.getTechniques().isEmpty()) return null;
        return skill;
    }

    public static void set(ServerPlayer player, int slot, Skill skill) {
        if (slot < 0 || slot > 3) return;
        CompoundTag root = player.getPersistentData();
        CompoundTag bound = root.getCompound(BOUND_KEY);
        String key = "BoundSkill_" + slot;
        if (skill == null || skill.getTechniques().isEmpty()) {
            bound.remove(key);
        } else {
            bound.put(key, skill.toNbt());
        }
        root.put(BOUND_KEY, bound);
    }
}
