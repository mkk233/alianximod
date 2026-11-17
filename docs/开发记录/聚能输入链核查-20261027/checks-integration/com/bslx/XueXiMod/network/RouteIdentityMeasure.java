package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.ReleaseTag;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.skill.SkillType;

import java.util.ArrayList;
import java.util.List;

/** 只打印两条失败断言的实际倍率。不改生产代码。 */
public final class RouteIdentityMeasure {
    private RouteIdentityMeasure() {
    }

    public static void main(String[] programArguments) {
        Probe gather = register("measure_gather", ChainRole.ATTACK, 1f, 2f);
        Probe magicAttack = register("measure_magic", ChainRole.ATTACK, 1f, 1f);
        Probe secondMagic = register("measure_magic_b", ChainRole.ATTACK, 1f, 1f);
        Skill plain = skill(SkillType.MAGIC, technique(magicAttack, TechniqueType.MAGIC, ShuAttribute.WATER, 4f));
        ReleaseInput baseInput = SkillChainPlan.frozenChainInput(plain, plain.getTechniques(), 0);
        System.out.println("baseMultiplier=" + baseInput.chainEnergyMultiplier());
        System.out.println("baseIdentity=" + (baseInput == ReleaseInput.BASE));
        Skill chain = skill(SkillType.MAGIC,
                technique(gather, TechniqueType.UNIVERSAL, ShuAttribute.NONE, 0f),
                technique(magicAttack, TechniqueType.MAGIC, ShuAttribute.WATER, 4f),
                technique(secondMagic, TechniqueType.MAGIC, ShuAttribute.WATER, 4f));
        List<ChainSegment> planned = segments(3);
        SkillChainPlan.ModifierPlan firstPlan = SkillChainPlan.collectModifiers(chain, planned, chain.getTechniques(), 0);
        SkillChainPlan.ModifierPlan secondPlan = SkillChainPlan.collectModifiers(
                chain, planned, chain.getTechniques(), firstPlan.resume);
        System.out.println("firstMultiplier=" + firstPlan.attackInput().chainEnergyMultiplier());
        System.out.println("secondMultiplier=" + secondPlan.attackInput().chainEnergyMultiplier());
        System.out.println("secondIdentity=" + (secondPlan.attackInput() == ReleaseInput.BASE));
        System.out.println("secondSize=" + secondPlan.attackInput().sizeMultiplier());
    }

    private static Probe register(String identifier, ChainRole chainRole, float ownEnergy, float chainEnergy) {
        Probe definition = new Probe(identifier, chainRole, ownEnergy, chainEnergy);
        ReleaseRegistry.register(definition);
        return definition;
    }

    private static Technique technique(Probe definition, TechniqueType techniqueType, ShuAttribute attribute, float damage) {
        return new Technique(definition.id(), definition.id(), techniqueType, damage, 20, 1, "probe", 0, attribute, 1f);
    }

    private static Skill skill(SkillType skillType, Technique... members) {
        return new Skill("probe", skillType, List.of(members));
    }

    private static List<ChainSegment> segments(int memberCount) {
        List<ChainSegment> planned = new ArrayList<>();
        for (int segmentIndex = 0; segmentIndex < memberCount; segmentIndex++) {
            planned.add(new ChainSegment(segmentIndex, List.of()));
        }
        return planned;
    }

    private static final class Probe implements ReleaseDefinition {
        private final String identifier;
        private final ChainRole chainRole;
        private final float ownEnergy;
        private final float chainEnergy;

        private Probe(String identifier, ChainRole chainRole, float ownEnergy, float chainEnergy) {
            this.identifier = identifier;
            this.chainRole = chainRole;
            this.ownEnergy = ownEnergy;
            this.chainEnergy = chainEnergy;
        }

        @Override
        public String id() {
            return identifier;
        }

        @Override
        public ReleaseKind kind() {
            return ReleaseKind.HIT_VOLUME;
        }

        @Override
        public ReleaseTag tag() {
            return ReleaseTag.SUSTAINED;
        }

        @Override
        public Trajectory trajectory() {
            return Trajectory.STRAIGHT;
        }

        @Override
        public boolean moving() {
            return false;
        }

        @Override
        public int lifetimeTicks() {
            return 20;
        }

        @Override
        public float speed() {
            return 1f;
        }

        @Override
        public float thickness() {
            return 0f;
        }

        @Override
        public ChainRole role() {
            return chainRole;
        }

        @Override
        public float energyScale() {
            return ownEnergy;
        }

        @Override
        public float chainEnergyScale() {
            return chainEnergy;
        }

        @Override
        public int pullAfterA() {
            return 0;
        }
    }
}
