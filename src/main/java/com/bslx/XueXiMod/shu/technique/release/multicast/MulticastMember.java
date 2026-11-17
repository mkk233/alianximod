package com.bslx.XueXiMod.shu.technique.release.multicast;

import java.util.List;

/**
 * 展开队列里的一个实际执行成员。只存原始下标，不存玩家、报价、目标或随机结果。
 * 同一成员上的复制份共用这个序号，不另开时间节点。
 */
public record MulticastMember(
        int ordinal,
        int hostIndex,
        boolean independentEnchant,
        List<Integer> inheritedModifierIndexes,
        List<Integer> localModifierIndexes,
        List<Integer> replicationIndexes,
        List<Integer> attachedEnchantIndexes
) {
    public MulticastMember {
        inheritedModifierIndexes = List.copyOf(inheritedModifierIndexes);
        localModifierIndexes = List.copyOf(localModifierIndexes);
        replicationIndexes = List.copyOf(replicationIndexes);
        attachedEnchantIndexes = List.copyOf(attachedEnchantIndexes);
    }
}
