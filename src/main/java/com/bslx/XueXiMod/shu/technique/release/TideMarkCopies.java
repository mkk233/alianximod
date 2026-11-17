package com.bslx.XueXiMod.shu.technique.release;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 一份账本里已经收下的潮印条数。重复的目标、轮次、复制序号不再累加。
 * take 只拿走还没结算的增量，已经见过的复制序号留着，避免旧份重算。
 * 另一份账本各自记，不跨组去重。
 */
public final class TideMarkCopies {
    private final Map<UUID, Map<Integer, Set<Integer>>> seenRounds = new HashMap<>();
    private final Map<UUID, Integer> pending = new HashMap<>();

    public boolean accept(UUID targetId, int roundIndex, int copyIndex, int marks) {
        if (targetId == null) {
            return false;
        }
        Map<Integer, Set<Integer>> rounds = this.seenRounds.computeIfAbsent(targetId, targetKey -> new HashMap<>());
        Set<Integer> copies = rounds.computeIfAbsent(roundIndex, roundKey -> new HashSet<>());
        if (!copies.add(copyIndex)) {
            return false;
        }
        if (marks > 0) {
            this.pending.merge(targetId, marks, Integer::sum);
        }
        return true;
    }

    public int take(UUID targetId) {
        if (targetId == null) {
            return 0;
        }
        Integer marks = this.pending.remove(targetId);
        return marks == null ? 0 : marks;
    }
}
