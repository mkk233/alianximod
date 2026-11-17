package com.bslx.XueXiMod.shu.technique.release.cooperative;

import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ChainIndexMapping;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ReleaseGateKind;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 一次攻击族的结构草图。令牌、伤害和载体在真正启动时才冻结，领域每一轮都重新建计划。
 */
public final class CooperativeCastRecipe {
    private final ReleaseGateKind gateKind;
    private final int actionChainIndex;
    private final @Nullable String actionTechniqueId;
    private final boolean cooperationRequested;
    private final boolean spaceLock;
    private final boolean scatter;
    private final boolean dropPath;
    private final List<AttackSketch> attacks;
    private final List<ChainIndexMapping> chainIndexes;
    private final List<Integer> leapChainIndexes;
    private final @Nullable String refusalKey;

    private CooperativeCastRecipe(ReleaseGateKind gateKind, int actionChainIndex, @Nullable String actionTechniqueId,
                                  boolean cooperationRequested, boolean spaceLock, boolean scatter, boolean dropPath,
                                  List<AttackSketch> attacks, List<ChainIndexMapping> chainIndexes,
                                  List<Integer> leapChainIndexes, @Nullable String refusalKey) {
        this.gateKind = gateKind;
        this.actionChainIndex = actionChainIndex;
        this.actionTechniqueId = actionTechniqueId;
        this.cooperationRequested = cooperationRequested;
        this.spaceLock = spaceLock;
        this.scatter = scatter;
        this.dropPath = dropPath;
        this.attacks = attacks;
        this.chainIndexes = chainIndexes;
        this.leapChainIndexes = leapChainIndexes;
        this.refusalKey = refusalKey;
    }

    public static CooperativeCastRecipe ready(ReleaseGateKind gateKind, int actionChainIndex,
                                              @Nullable String actionTechniqueId, boolean cooperationRequested,
                                              boolean spaceLock, boolean scatter, boolean dropPath,
                                              List<AttackSketch> attacks, List<ChainIndexMapping> chainIndexes,
                                              List<Integer> leapChainIndexes) {
        return new CooperativeCastRecipe(gateKind, actionChainIndex, actionTechniqueId, cooperationRequested,
                spaceLock, scatter, dropPath, List.copyOf(attacks), List.copyOf(chainIndexes),
                List.copyOf(leapChainIndexes), null);
    }

    public static CooperativeCastRecipe refused(String refusalKey) {
        return new CooperativeCastRecipe(ReleaseGateKind.IMMEDIATE, -1, null, false, false, false, false,
                List.of(), List.of(), List.of(), refusalKey);
    }

    public boolean refused() {
        return refusalKey != null;
    }

    public @Nullable String refusalKey() {
        return refusalKey;
    }

    public ReleaseGateKind gateKind() {
        return gateKind;
    }

    public int actionChainIndex() {
        return actionChainIndex;
    }

    public @Nullable String actionTechniqueId() {
        return actionTechniqueId;
    }

    public boolean cooperationRequested() {
        return cooperationRequested;
    }

    public boolean spaceLock() {
        return spaceLock;
    }

    public boolean scatter() {
        return scatter;
    }

    public boolean dropPath() {
        return dropPath;
    }

    public List<AttackSketch> attacks() {
        return attacks;
    }

    public List<ChainIndexMapping> chainIndexes() {
        return chainIndexes;
    }

    public List<Integer> leapChainIndexes() {
        return leapChainIndexes;
    }

    public static final class AttackSketch {
        private final int chainIndex;
        private final int memberOrdinal;
        private final String techniqueId;
        private final int copyCount;
        private final int delayTicks;
        private final boolean stagger;
        private final int continueParentOrdinal;
        private final List<Integer> enchantIndexes;
        private final List<Integer> modifierIndexes;
        private final boolean entityLock;
        private final boolean upwellPath;

        public AttackSketch(int chainIndex, int memberOrdinal, String techniqueId, int copyCount, int delayTicks,
                            boolean stagger, int continueParentOrdinal, List<Integer> enchantIndexes,
                            List<Integer> modifierIndexes, boolean entityLock, boolean upwellPath) {
            this.chainIndex = chainIndex;
            this.memberOrdinal = memberOrdinal;
            this.techniqueId = techniqueId;
            this.copyCount = copyCount;
            this.delayTicks = delayTicks;
            this.stagger = stagger;
            this.continueParentOrdinal = continueParentOrdinal;
            this.enchantIndexes = List.copyOf(enchantIndexes);
            this.modifierIndexes = List.copyOf(modifierIndexes);
            this.entityLock = entityLock;
            this.upwellPath = upwellPath;
        }

        public int chainIndex() {
            return chainIndex;
        }

        public int memberOrdinal() {
            return memberOrdinal;
        }

        public String techniqueId() {
            return techniqueId;
        }

        public int copyCount() {
            return copyCount;
        }

        public int delayTicks() {
            return delayTicks;
        }

        public boolean stagger() {
            return stagger;
        }

        public int continueParentOrdinal() {
            return continueParentOrdinal;
        }

        public List<Integer> enchantIndexes() {
            return enchantIndexes;
        }

        public List<Integer> modifierIndexes() {
            return modifierIndexes;
        }

        public boolean entityLock() {
            return entityLock;
        }

        public boolean upwellPath() {
            return upwellPath;
        }
    }
}
