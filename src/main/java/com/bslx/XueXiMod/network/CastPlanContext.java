package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.skill.Skill;

import java.util.List;

/**
 * 一次按下的计划上下文。持有本次技能和已选切片，不把技能写回缓存。
 * 代际失配后仍使用构造时的那份切片，不再安装新图。
 */
final class CastPlanContext {
    private final Skill currentSkill;
    private final CompiledCastPlan.Slice slice;

    CastPlanContext(Skill currentSkill, CompiledCastPlan.Slice slice) {
        this.currentSkill = currentSkill;
        this.slice = slice;
    }

    Skill currentSkill() {
        return currentSkill;
    }

    boolean canReuseStructure() {
        return slice != null && slice.matchesCurrentEpoch();
    }

    List<ChainSegment> segments() {
        return slice.segments();
    }

    boolean releaseResolved() {
        return slice != null && slice.releaseResolved();
    }

    CompiledReleaseStep.StructureChoice releaseChoice() {
        return slice == null ? null : slice.releaseChoice();
    }

    SkillChainPlan.ModifierPlan modifierPlan(int from, boolean realInputs) {
        if (slice == null) {
            return null;
        }
        return slice.modifierPlan(from, realInputs);
    }

    int copiesAt(int segmentIndex) {
        if (slice == null) {
            return -1;
        }
        return slice.copiesAt(segmentIndex);
    }

    List<ChainSegment> releasedMembers(int ruleIndex) {
        if (slice == null) {
            return null;
        }
        return slice.releasedMembers(ruleIndex);
    }

    boolean knowsGroup(int actionIndex) {
        return slice != null && slice.knowsGroup(actionIndex);
    }

    List<Integer> sameGroup(int actionIndex) {
        return slice == null ? null : slice.sameGroup(actionIndex);
    }

    boolean knowsLock(int actionIndex) {
        return slice != null && slice.knowsLock(actionIndex);
    }

    boolean lockAt(int actionIndex) {
        return slice != null && slice.lockAt(actionIndex);
    }

    boolean knowsDrop(int actionIndex) {
        return slice != null && slice.knowsDrop(actionIndex);
    }

    boolean dropAt(int actionIndex) {
        return slice != null && slice.dropAt(actionIndex);
    }

    ReleaseInput frozenInput(int actionIndex) {
        if (slice == null) {
            return null;
        }
        return slice.frozenInput(actionIndex);
    }

    SkillSegmentRunner.RoleScan roleScan(int actionIndex) {
        if (slice == null) {
            return null;
        }
        return slice.roleScan(actionIndex);
    }
}
