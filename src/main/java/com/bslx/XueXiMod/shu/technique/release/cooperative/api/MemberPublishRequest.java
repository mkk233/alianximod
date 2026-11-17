package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 同一调用栈内的全有或全无发布请求。
 * 先暂存，commit 才发布；discard 丢弃暂存。失败不得先发出玩家体积再补一份。
 */
public record MemberPublishRequest(MemberKey member, List<PlannedVolumeSpawn> volumes, ShareSlot attackShare) {
    public MemberPublishRequest {
        member = FrozenValues.present(member, "发布成员");
        volumes = FrozenValues.list(volumes, "待发布体积");
        attackShare = FrozenValues.present(attackShare, "攻击份额");
        if (volumes.isEmpty()) {
            throw new IllegalArgumentException("成员发布不能是空体积");
        }
        if (attackShare.role() != ShareRole.ATTACK_MEMBER || attackShare.familyLevel()
                || attackShare.memberOrdinal() != member.memberOrdinal()) {
            throw new IllegalArgumentException("攻击份额不属于这个成员");
        }
        Map<LogicalCopyKey, Set<Integer>> actorsByCopy = new LinkedHashMap<>();
        for (PlannedVolumeSpawn volume : volumes) {
            if (!volume.emit().copy().member().equals(member) || !volume.ledger().member().equals(member)) {
                throw new IllegalArgumentException("待发布体积不属于这个成员");
            }
            Set<Integer> actors = actorsByCopy.computeIfAbsent(volume.emit().copy(), copyKey -> new LinkedHashSet<>());
            if (!actors.add(volume.emit().actorOrdinal())) {
                throw new IllegalArgumentException("同一逻辑副本的演员序号重复");
            }
        }
        Set<Integer> baseline = null;
        for (Set<Integer> actors : actorsByCopy.values()) {
            if (actors.size() != 1 && actors.size() != CooperativeStructureLimits.ACTORS_PER_COOPERATION) {
                throw new IllegalArgumentException("每个逻辑副本只能有一名或两名演员");
            }
            if (baseline == null) {
                baseline = actors;
            } else if (!baseline.equals(actors)) {
                throw new IllegalArgumentException("同一成员的每个逻辑副本必须携带同一组演员");
            }
        }
    }
}
