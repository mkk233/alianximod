package com.bslx.XueXiMod.shu.technique.release.cooperative;

import com.bslx.XueXiMod.shu.technique.release.cooperative.api.AttackMemberPlan;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ChainIndexMapping;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperationOrder;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeStructureLimits;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.FamilyControlToken;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.FamilyKey;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.FrozenCarrierCredential;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.FrozenValues;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.LogicalSpawnIndex;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.PlanShareSchedule;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.PlanSlotKind;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.QuotedCooldown;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ReleaseGate;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ReleaseGateKind;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ShareRole;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ShareSlot;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 一次攻击族的不可变执行描述。
 * 不保存技能、可变 NBT、主手物品或线程局部释放帧。载体只留身份凭据，付款只留份额索引。
 * 附魔、领域和命中接续在每个攻击成员的结算封套里；规则和原下标在链映射里。
 * 这里只按一名演员拒绝超过本族体积上限的展开。两名演员超限时计划仍可建立，运行时再回退单人。
 */
public record ExecutionFamilyPlan(FamilyKey family, List<ChainIndexMapping> chainIndexes,
                                  FrozenCarrierCredential carrier, ReleaseGate gate,
                                  @Nullable CooperationOrder cooperation, List<AttackMemberPlan> attacks,
                                  List<LogicalSpawnIndex> spawns, PlanShareSchedule shares,
                                  List<QuotedCooldown> cooldownPieces, FamilyControlToken cancelToken,
                                  FamilyControlToken completionToken, Vec3 frozenAim, float frozenYaw,
                                  boolean horizontalAimFallback) {
    public ExecutionFamilyPlan {
        family = FrozenValues.present(family, "攻击族");
        carrier = FrozenValues.present(carrier, "载体凭据");
        gate = FrozenValues.present(gate, "释放门");
        shares = FrozenValues.present(shares, "份额索引");
        cancelToken = FrozenValues.present(cancelToken, "取消令牌");
        completionToken = FrozenValues.present(completionToken, "完成令牌");
        if (cancelToken.tokenId().equals(completionToken.tokenId())) {
            throw new IllegalArgumentException("取消令牌和完成令牌不能相同");
        }
        chainIndexes = FrozenValues.list(chainIndexes, "链下标映射");
        attacks = FrozenValues.list(attacks, "攻击成员");
        spawns = FrozenValues.list(spawns, "逻辑生成索引");
        cooldownPieces = FrozenValues.list(cooldownPieces, "冷却原料");
        frozenAim = FrozenValues.unit(frozenAim, "冻结瞄准");
        if (!Float.isFinite(frozenYaw)) {
            throw new IllegalArgumentException("冻结水平角必须有限");
        }
        if (chainIndexes.isEmpty() || cooldownPieces.isEmpty()) {
            throw new IllegalArgumentException("执行族缺少链映射或冷却原料");
        }
        checkMembers(chainIndexes, carrier, gate, cooperation, attacks, spawns, shares, cancelToken);
    }

    private static void checkMembers(List<ChainIndexMapping> chainIndexes, FrozenCarrierCredential carrier,
                                     ReleaseGate gate, @Nullable CooperationOrder cooperation,
                                     List<AttackMemberPlan> attacks, List<LogicalSpawnIndex> spawns,
                                     PlanShareSchedule shares, FamilyControlToken cancelToken) {
        Map<Integer, AttackMemberPlan> byOrdinal = new LinkedHashMap<>();
        Map<Integer, AttackMemberPlan> byChain = new LinkedHashMap<>();
        long plannedEmits = 0L;
        for (AttackMemberPlan member : attacks) {
            if (byOrdinal.put(member.memberOrdinal(), member) != null) {
                throw new IllegalArgumentException("攻击成员序号重复");
            }
            if (byChain.put(member.chainIndex(), member) != null) {
                throw new IllegalArgumentException("攻击成员链下标重复");
            }
            if (!member.prepared().preparation().cancelId().equals(cancelToken.tokenId())) {
                throw new IllegalArgumentException("攻击成员的取消标识必须等于取消令牌");
            }
            if (!member.prepared().preparation().settlement().carrier().sameIdentity(carrier)) {
                throw new IllegalArgumentException("攻击成员的载体凭据和族凭据不一致");
            }
            try {
                plannedEmits = Math.addExact(plannedEmits,
                        Math.multiplyExact((long) member.copyCount(), (long) member.spawnCount()));
            } catch (ArithmeticException overflow) {
                throw new IllegalArgumentException("攻击成员展开份数溢出");
            }
        }
        if (plannedEmits > CooperativeStructureLimits.FAMILY_PLANNED_EMIT_CAP) {
            throw new IllegalArgumentException("单人展开超过本族体积上限");
        }
        int timedCount = 0;
        int contactCount = 0;
        int cooperativeCount = 0;
        int immediateCount = 0;
        Set<Integer> seenChains = new HashSet<>();
        for (ChainIndexMapping mapping : chainIndexes) {
            if (!seenChains.add(mapping.chainIndex())) {
                throw new IllegalArgumentException("链下标重复");
            }
            if (mapping.memberScoped() && !byOrdinal.containsKey(mapping.memberOrdinal())) {
                throw new IllegalArgumentException("链槽指向不存在的成员");
            }
            if (mapping.slotKind() == PlanSlotKind.TIMED_ACTION) {
                timedCount += 1;
            } else if (mapping.slotKind() == PlanSlotKind.CONTACT_ACTION) {
                contactCount += 1;
            } else if (mapping.slotKind() == PlanSlotKind.COOPERATIVE_RULE) {
                cooperativeCount += 1;
            } else if (mapping.slotKind() == PlanSlotKind.IMMEDIATE_ATTACK) {
                immediateCount += 1;
                AttackMemberPlan member = byChain.get(mapping.chainIndex());
                if (member == null || !mapping.memberScoped()
                        || mapping.memberOrdinal() != member.memberOrdinal()
                        || !mapping.techniqueId().equals(member.techniqueId())) {
                    throw new IllegalArgumentException("立即攻击槽和攻击成员不一致");
                }
            }
        }
        if (timedCount > 1 || contactCount > 1 || cooperativeCount > 1 || (timedCount > 0 && contactCount > 0)) {
            throw new IllegalArgumentException("一个执行族不能嵌套两段新动作或重复协同");
        }
        if (immediateCount != attacks.size()) {
            throw new IllegalArgumentException("攻击成员和立即攻击槽数量不一致");
        }
        if (cooperation == null) {
            if (cooperativeCount != 0 || shares.cooperationSeparate()) {
                throw new IllegalArgumentException("没有协同指令时不能保留协同槽");
            }
        } else if (cooperativeCount != 1 || attacks.isEmpty()) {
            throw new IllegalArgumentException("协同必须恰好绑定一个带攻击的执行族");
        }
        int actionSlots = countRole(shares, ShareRole.ACTION);
        if (gate.kind() == ReleaseGateKind.TIMED) {
            if (timedCount != 1 || contactCount != 0 || actionSlots != 1) {
                throw new IllegalArgumentException("定时族必须有一段定时动作和一份动作份额");
            }
        } else if (gate.kind() == ReleaseGateKind.CONTACT) {
            if (contactCount != 1 || timedCount != 0 || attacks.isEmpty() || actionSlots != 1) {
                throw new IllegalArgumentException("接触族必须有一段接触动作、后续攻击和一份动作份额");
            }
        } else if (timedCount != 0 || contactCount != 0 || attacks.isEmpty() || actionSlots != 0) {
            throw new IllegalArgumentException("立即族只发布攻击，不携带动作份额");
        }
        Map<Integer, ShareSlot> attackSlots = attackSlots(shares);
        if (attackSlots.size() != attacks.size()) {
            throw new IllegalArgumentException("攻击份额和攻击成员数量不一致");
        }
        for (AttackMemberPlan member : attacks) {
            if (!attackSlots.containsKey(member.memberOrdinal())) {
                throw new IllegalArgumentException("攻击成员缺少份额");
            }
        }
        checkSpawns(attacks, spawns, byOrdinal);
    }

    private static int countRole(PlanShareSchedule shares, ShareRole role) {
        int total = 0;
        for (ShareSlot slot : shares.slots()) {
            if (slot.role() == role) {
                total += 1;
            }
        }
        return total;
    }

    private static Map<Integer, ShareSlot> attackSlots(PlanShareSchedule shares) {
        Map<Integer, ShareSlot> indexed = new LinkedHashMap<>();
        for (ShareSlot slot : shares.slots()) {
            if (slot.role() != ShareRole.ATTACK_MEMBER) {
                continue;
            }
            if (indexed.put(slot.memberOrdinal(), slot) != null) {
                throw new IllegalArgumentException("攻击份额序号重复");
            }
        }
        return indexed;
    }

    private static void checkSpawns(List<AttackMemberPlan> attacks, List<LogicalSpawnIndex> spawns,
                                    Map<Integer, AttackMemberPlan> byOrdinal) {
        Map<String, LogicalSpawnIndex> indexed = new LinkedHashMap<>();
        Map<Integer, Set<Integer>> ordinalsByMember = new LinkedHashMap<>();
        for (LogicalSpawnIndex spawn : spawns) {
            AttackMemberPlan member = byOrdinal.get(spawn.memberOrdinal());
            if (member == null || spawn.copyOrdinal() >= member.copyCount()
                    || spawn.spawnOrdinal() >= member.spawnCount()) {
                throw new IllegalArgumentException("逻辑生成索引没有落在成员展开内");
            }
            String slotKey = spawn.memberOrdinal() + ":" + spawn.copyOrdinal() + ":" + spawn.spawnOrdinal();
            if (indexed.put(slotKey, spawn) != null) {
                throw new IllegalArgumentException("逻辑生成索引重复");
            }
            Set<Integer> ordinals = ordinalsByMember.computeIfAbsent(spawn.memberOrdinal(), memberKey -> new HashSet<>());
            if (!ordinals.add(spawn.settlementOrdinal())) {
                throw new IllegalArgumentException("同一成员的结算序号重复");
            }
        }
        for (AttackMemberPlan member : attacks) {
            int expectedCount;
            try {
                expectedCount = Math.toIntExact(Math.multiplyExact((long) member.copyCount(), (long) member.spawnCount()));
            } catch (ArithmeticException overflow) {
                throw new IllegalArgumentException("攻击成员展开份数溢出");
            }
            Set<Integer> ordinals = ordinalsByMember.getOrDefault(member.memberOrdinal(), Set.of());
            if (ordinals.size() != expectedCount) {
                throw new IllegalArgumentException("结算序号没有覆盖成员展开");
            }
            for (int ordinal = 0; ordinal < expectedCount; ordinal++) {
                if (!ordinals.contains(ordinal)) {
                    throw new IllegalArgumentException("结算序号必须从 0 连续编号");
                }
            }
            for (int copyCursor = 0; copyCursor < member.copyCount(); copyCursor++) {
                for (int spawnCursor = 0; spawnCursor < member.spawnCount(); spawnCursor++) {
                    String slotKey = member.memberOrdinal() + ":" + copyCursor + ":" + spawnCursor;
                    if (!indexed.containsKey(slotKey)) {
                        throw new IllegalArgumentException("逻辑生成索引没有覆盖成员展开");
                    }
                }
            }
        }
    }
}
