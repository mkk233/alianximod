package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import java.util.List;

/**
 * 一个族的份额索引。协同槽最多一个，并且和修饰总价分开。
 * cooperationSeparate 为 true 当且仅当存在协同槽。
 */
public record PlanShareSchedule(List<ShareSlot> slots, boolean cooperationSeparate) {
    public PlanShareSchedule {
        slots = FrozenValues.list(slots, "份额索引");
        int actionSlots = 0;
        int cooperationSlots = 0;
        for (ShareSlot slot : slots) {
            if (slot.role() == ShareRole.ACTION) {
                actionSlots += 1;
            } else if (slot.role() == ShareRole.COOPERATION) {
                cooperationSlots += 1;
            }
        }
        if (actionSlots > 1 || cooperationSlots > 1) {
            throw new IllegalArgumentException("动作或协同份额最多一份");
        }
        if (cooperationSeparate != (cooperationSlots == 1)) {
            throw new IllegalArgumentException("协同份额的分离标记和槽位不一致");
        }
    }
}
