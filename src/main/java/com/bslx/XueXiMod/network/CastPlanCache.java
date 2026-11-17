package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.debug.performance.PerfCounter;
import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.ChainStructureEpoch;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.skill.SkillType;
import net.minecraft.server.MinecraftServer;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务端线程上的结构计划缓存。未绑定、线程或服务器不符、无资格、超长或超预算都返回 null，释放仍走旧路径。
 * 一次查找只记 hit、miss、bypass 中的一个；只有完整编译并放入缓存才再记 compiles。
 */
final class CastPlanCache {
    static final int CHAIN_LIMIT = 64;
    static final int ENTRY_LIMIT = 64;
    static final int UNIT_LIMIT = 16384;

    private static MinecraftServer boundServer;
    private static Thread boundThread;
    private static long knownEpoch;
    private static Map<CastPlanKey, CompiledCastPlan> plans;

    private CastPlanCache() {
    }

    static void bindServer(MinecraftServer server) {
        boundServer = server;
        boundThread = Thread.currentThread();
        knownEpoch = ChainStructureEpoch.current();
        plans = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<CastPlanKey, CompiledCastPlan> eldestEntry) {
                return size() > ENTRY_LIMIT;
            }
        };
    }

    static void releaseServer(MinecraftServer server) {
        if (boundServer != null && server != boundServer) {
            return;
        }
        if (plans != null) {
            plans.clear();
        }
        plans = null;
        boundServer = null;
        boundThread = null;
    }

    static CompiledCastPlan lookup(MinecraftServer server, Skill currentSkill) {
        if (!boundTo(server) || currentSkill == null) {
            PerformanceRecorder.add(PerfCounter.CAST_PLAN_BYPASSES, 1L);
            return null;
        }
        long structureEpoch = ChainStructureEpoch.current();
        if (knownEpoch != structureEpoch) {
            plans.clear();
            knownEpoch = structureEpoch;
        }
        List<Technique> techniques = currentSkill.getTechniques();
        if (techniques == null || techniques.size() > CHAIN_LIMIT || !eligible(techniques)) {
            PerformanceRecorder.add(PerfCounter.CAST_PLAN_BYPASSES, 1L);
            return null;
        }
        CastPlanKey planKey = CastPlanKey.capture(currentSkill, structureEpoch);
        CompiledCastPlan found = plans.get(planKey);
        if (found != null) {
            PerformanceRecorder.add(PerfCounter.CAST_PLAN_HITS, 1L);
            return found;
        }
        long compileStart = PerformanceRecorder.begin();
        CompiledCastPlan compiled;
        try {
            compiled = CompiledCastPlan.compile(currentSkill, structureEpoch);
        } finally {
            PerformanceRecorder.end(PerfMetric.CAST_PLAN_COMPILE, compileStart);
        }
        if (compiled == null) {
            PerformanceRecorder.add(PerfCounter.CAST_PLAN_BYPASSES, 1L);
            return null;
        }
        plans.put(planKey, compiled);
        PerformanceRecorder.add(PerfCounter.CAST_PLAN_MISSES, 1L);
        PerformanceRecorder.add(PerfCounter.CAST_PLAN_COMPILES, 1L);
        return compiled;
    }

    private static boolean boundTo(MinecraftServer server) {
        return server != null
                && plans != null
                && boundServer == server
                && Thread.currentThread() == boundThread;
    }

    private static boolean eligible(List<Technique> techniques) {
        for (Technique technique : techniques) {
            if (technique == null || technique.getId() == null) {
                return false;
            }
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            if (definition != null && !definition.supportsStructuralPlanCache()) {
                return false;
            }
        }
        return true;
    }

    static final class StructureBudget {
        private int chargedUnits;

        boolean charge(int units) {
            if (units < 0 || units > UNIT_LIMIT - chargedUnits) {
                return false;
            }
            chargedUnits += units;
            return true;
        }
    }

    private static final class CastPlanKey {
        private final SkillType skillType;
        private final String[] techniqueIds;
        private final ShuAttribute[] embryoMarks;
        private final long structureEpoch;
        private final int capabilityStamp;
        private final int identityHash;

        private CastPlanKey(SkillType skillType, String[] techniqueIds, ShuAttribute[] embryoMarks,
                            long structureEpoch, int capabilityStamp, int identityHash) {
            this.skillType = skillType;
            this.techniqueIds = techniqueIds;
            this.embryoMarks = embryoMarks;
            this.structureEpoch = structureEpoch;
            this.capabilityStamp = capabilityStamp;
            this.identityHash = identityHash;
        }

        static CastPlanKey capture(Skill skill, long structureEpoch) {
            List<Technique> techniques = skill.getTechniques();
            int techniqueCount = techniques.size();
            String[] techniqueIds = new String[techniqueCount];
            ShuAttribute[] embryoMarks = new ShuAttribute[techniqueCount];
            for (int techniqueIndex = 0; techniqueIndex < techniqueCount; techniqueIndex++) {
                techniqueIds[techniqueIndex] = techniques.get(techniqueIndex).getId();
                embryoMarks[techniqueIndex] = skill.getEmbryo(techniqueIndex);
            }
            int stamp = capabilityStamp(techniques);
            int computedHash = skill.getType().hashCode();
            computedHash = 31 * computedHash + Arrays.hashCode(techniqueIds);
            computedHash = 31 * computedHash + Arrays.hashCode(embryoMarks);
            computedHash = 31 * computedHash + Long.hashCode(structureEpoch);
            computedHash = 31 * computedHash + stamp;
            return new CastPlanKey(skill.getType(), techniqueIds, embryoMarks, structureEpoch, stamp, computedHash);
        }

        private static int capabilityStamp(List<Technique> techniques) {
            int stamp = 1;
            for (Technique technique : techniques) {
                ReleaseDefinition definition = technique == null ? null : ReleaseRegistry.get(technique.getId());
                if (definition == null) {
                    stamp = 31 * stamp;
                    continue;
                }
                stamp = 31 * stamp + definition.role().ordinal();
                stamp = 31 * stamp + definition.trajectory().ordinal();
                stamp = 31 * stamp + (definition.declaresBatchQuote() ? 1 : 0);
            }
            return stamp;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof CastPlanKey otherKey)) {
                return false;
            }
            return structureEpoch == otherKey.structureEpoch
                    && capabilityStamp == otherKey.capabilityStamp
                    && skillType == otherKey.skillType
                    && Arrays.equals(techniqueIds, otherKey.techniqueIds)
                    && Arrays.equals(embryoMarks, otherKey.embryoMarks);
        }

        @Override
        public int hashCode() {
            return identityHash;
        }
    }
}
