package com.bslx.XueXiMod.status;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;

/** 把属性状态的精确数值放在实体持久数据里，和药水效果一起保存。 */
public final class AttributeStateData {
    private static final String ROOT = "xueximod_attribute_states";

    private AttributeStateData() {}

    /** 读一个状态的精确整数。还没有这组数据时是 0。 */
    public static int read(LivingEntity entity, String identifier) {
        CompoundTag persistent = entity.getPersistentData();
        if (!persistent.contains(ROOT, Tag.TAG_COMPOUND)) {
            return 0;
        }
        return persistent.getCompound(ROOT).getInt(identifier);
    }

    /** 写入精确整数。不是正数就删掉这一项；全部删空后连根标签一起去掉。 */
    public static void write(LivingEntity entity, String identifier, int amount) {
        CompoundTag persistent = entity.getPersistentData();
        CompoundTag states = persistent.getCompound(ROOT);
        if (amount <= 0) {
            states.remove(identifier);
        } else {
            states.putInt(identifier, amount);
        }
        if (states.isEmpty()) {
            persistent.remove(ROOT);
        } else {
            persistent.put(ROOT, states);
        }
    }
}
