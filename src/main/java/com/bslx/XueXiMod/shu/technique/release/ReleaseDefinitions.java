package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.TechniqueBalanceSpec;
import com.bslx.XueXiMod.shu.technique.effects.FentianEffect;
import com.bslx.XueXiMod.shu.technique.effects.FireBurstEffect;
import com.bslx.XueXiMod.shu.technique.effects.FireOrbEffect;
import com.bslx.XueXiMod.shu.technique.effects.HeatBeamEffect;
import com.bslx.XueXiMod.shu.technique.effects.LiaoliEffect;
import com.bslx.XueXiMod.shu.technique.effects.LockTargetService;
import com.bslx.XueXiMod.shu.technique.effects.MagicArcaneMissileEffect;
import com.bslx.XueXiMod.shu.technique.effects.MetalNeedleEffect;
import com.bslx.XueXiMod.shu.technique.effects.MovementDashEffect;
import com.bslx.XueXiMod.shu.technique.effects.ParasiteSeedEffect;
import com.bslx.XueXiMod.shu.technique.effects.SpaceCutEffect;
import com.bslx.XueXiMod.shu.technique.effects.StoneHammerEffect;
import com.bslx.XueXiMod.shu.technique.effects.SwordComeEffect;
import com.bslx.XueXiMod.shu.technique.effects.SwordHorizontalSlashEffect;
import com.bslx.XueXiMod.shu.technique.effects.SwordThrustEffect;
import com.bslx.XueXiMod.shu.technique.effects.SwordVerticalSlashEffect;
import com.bslx.XueXiMod.shu.technique.effects.TestSustainedEffect;
import com.bslx.XueXiMod.shu.technique.effects.TideSeverEffect;
import com.bslx.XueXiMod.shu.technique.effects.WaterYieldingEffect;
import com.bslx.XueXiMod.shu.technique.effects.WeepingCurtainEffect;
import com.bslx.XueXiMod.shu.technique.release.batch.ListedRelease;
import com.bslx.XueXiMod.shu.technique.release.batch.MotionProfile;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastSpec;
import com.bslx.XueXiMod.shu.technique.release.multicast.ReplicationSpec;
import com.bslx.XueXiMod.shu.technique.release.clone.CloneReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.clone.water.WaterPhantomDefinition;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainDefinition;
import com.bslx.XueXiMod.shu.technique.release.selfstatus.SelfStatusDefinition;
import com.bslx.XueXiMod.shu.technique.sword.SwordEntity;

/** 内置释放定义登记处。启动时写入注册表；不执行链，也不解释外部自定义实现。 */
public final class ReleaseDefinitions {
    private ReleaseDefinitions() {}

    /** 按本文件顺序登记内置、自身状态、分身和列出的批次定义。重复标识由注册表覆盖。 */
    public static void registerAll() {
        ReleaseRegistry.register(define("movement_dash", ReleaseKind.ACTION, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 10, 2f, 0f, 0, false, false, false, false, ChainRole.ACTION, false, false));
        ReleaseRegistry.register(cohortSword("sword_horizontal_slash", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 4, 0f, 0.5f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
        ReleaseRegistry.register(cohortSword("sword_vertical_slash", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 4, 0f, 0.5f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
        ReleaseRegistry.register(cohortSword("sword_thrust", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 4, 0f, 0.5f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
        ReleaseRegistry.register(define("magic_arcane_missile", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, true, 40, 1.5f, 0.1f, 0, true, true, false, false, ChainRole.ATTACK, true, false));
        ReleaseRegistry.register(define("test_sustained", ReleaseKind.HIT_VOLUME, ReleaseTag.SUSTAINED, Trajectory.DROP, true, 20, 0.4f, 0.1f, 0, false, true, false, false, ChainRole.ATTACK, true, false));
        ReleaseRegistry.register(define("heat_beam", ReleaseKind.HIT_VOLUME, ReleaseTag.SUSTAINED, Trajectory.FOLLOW_LOOK, false, 40, 0f, 1f, 0, true, true, false, false, ChainRole.ATTACK, true, false));
        ReleaseRegistry.register(define("universal_lock_target", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, true, false, ChainRole.SEEK_ENTITY, false, false));
        ReleaseRegistry.register(define("rule_dual_release", ReleaseKind.RULE, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 1, false, false, false, false, ChainRole.RULE_DUAL, false, false, MulticastSpec.fixed(2), null));
        ReleaseRegistry.register(define("rule_triple_release", ReleaseKind.RULE, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 1, false, false, false, false, ChainRole.RULE_DUAL, false, false, MulticastSpec.fixed(3), null));
        ReleaseRegistry.register(define("rule_quadruple_release", ReleaseKind.RULE, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 1, false, false, false, false, ChainRole.RULE_DUAL, false, false, MulticastSpec.fixed(4), null));
        ReleaseRegistry.register(define("rule_octuple_release", ReleaseKind.RULE, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 1, false, false, false, false, ChainRole.RULE_DUAL, false, false, MulticastSpec.fixed(8), null));
        ReleaseRegistry.register(define("rule_sixteen_release", ReleaseKind.RULE, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 1, false, false, false, false, ChainRole.RULE_DUAL, false, false, MulticastSpec.fixed(16), null));
        ReleaseRegistry.register(define("rule_exhaustive_release", ReleaseKind.RULE, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 1, false, false, false, false, ChainRole.RULE_DUAL, false, false, MulticastSpec.remaining(), null));
        ReleaseRegistry.register(define("trajectory_drop", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.DROP, false, 1, 0f, 0f, 0, false, false, false, true, ChainRole.TRAJECTORY_DROP, false, false));
        ReleaseRegistry.register(define("trajectory_tracking", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.TRAJECTORY_TRACKING, false, false));
        ReleaseRegistry.register(define("trajectory_parallel", ReleaseKind.RULE, ReleaseTag.TRIGGER, Trajectory.PARALLEL, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.TRAJECTORY_PARALLEL, false, false));
        ReleaseRegistry.register(define("trajectory_storm", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.VORTEX, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.TRAJECTORY_DROP, false, false));
        ReleaseRegistry.register(define("fire_orb", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, true, 40, 1.2f, 0.4f, 0, true, true, false, false, ChainRole.ATTACK, true, false));
        ReleaseRegistry.register(define("fire_burst", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, true, 40, 0.6f, 0.8f, 0, true, true, false, false, ChainRole.ATTACK, true, false));
        ReleaseRegistry.register(cohortSword("water_yielding_ribbon", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 32, 0f, 0.55f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
        ReleaseRegistry.register(define("water_weeping_arc", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.FUNCTION, true, WeepingSettings.LIFETIME, 0f, 0.35f, 0, true, true, false, false, ChainRole.ATTACK, false, false, ChainCompatibility.ORDINARY, 1f, 1f, true, true, false, false, 0f, 0, WeepingSettings.WETNESS, false, true, true, WeepingSettings.RETAINED_MANA, -1, false, false, false, null, null, false));
        ReleaseRegistry.register(cohortSword("space_cut", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 10, 0f, 0f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
        ReleaseRegistry.register(define("rule_cast_anchor", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, true, false, ChainRole.SEEK_SPACE, false, false));
        ReleaseRegistry.register(define("rule_triple_cast", ReleaseKind.RULE, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.RULE_TRIPLE, false, false, null, ReplicationSpec.of(3)));
        ReleaseRegistry.register(define("rule_double_cast", ReleaseKind.RULE, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.RULE_TRIPLE, false, false, null, ReplicationSpec.of(2)));
        ReleaseRegistry.register(define("rule_quintuple_cast", ReleaseKind.RULE, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.RULE_TRIPLE, false, false, null, ReplicationSpec.of(5)));
        ReleaseRegistry.register(define("rule_octuple_cast", ReleaseKind.RULE, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.RULE_TRIPLE, false, false, null, ReplicationSpec.of(8)));
        ReleaseRegistry.register(define("rule_domain_bind", ReleaseKind.RULE, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.RULE_DOMAIN_BIND, false, false));
        ReleaseRegistry.register(define("rule_gather_energy", ReleaseKind.RULE, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.ATTACK, false, false, ChainCompatibility.ORDINARY, 1f, 2f));
        ReleaseRegistry.register(define("trajectory_3d_interleave", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.TRAJECTORY_SCATTER, false, false));
        ReleaseRegistry.register(define("parasite_seed", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.DROP, true, 160, 0f, 0.5f, 0, false, true, false, false, ChainRole.ATTACK, true, false));
        ReleaseRegistry.register(cohortSword("tide_sever", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 6, 0f, 0.4f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
        ReleaseRegistry.register(new WaterPhantomDefinition());
        ReleaseRegistry.register(cohortSword("stone_hammer", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 10, 0f, 1f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
        ReleaseRegistry.register(define("metal_needle", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, true, 15, 1.5f, 0.2f, 0, true, true, false, false, ChainRole.ATTACK, true, false));
        ReleaseRegistry.register(define("phantom_sword", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, true, 80, 2.5f, 0.5f, 0, true, true, false, false, ChainRole.ATTACK, true, false));
        ReleaseRegistry.register(define("sword_come", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 20, 0f, 0.5f, 0, false, false, false, false, ChainRole.ATTACK, false, true, ChainCompatibility.MANIPULATION));
        ReleaseRegistry.register(define("sword_return_all", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 40, 0f, 0.5f, 0, false, false, false, false, ChainRole.ATTACK, false, true, ChainCompatibility.MANIPULATION));
        ReleaseRegistry.register(define("fire_pick_lamp", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 4, 0f, 0f, 0, false, false, false, false, ChainRole.ATTACK, false, false, ChainCompatibility.ORDINARY, 1f, 1f, true, true, true, true, 0f, 0, 0, false, false, false, 10, 2, true, false, false, null, null, false));
        ReleaseRegistry.register(define("water_wave", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 4, 0f, 0f, 0, false, false, false, false, ChainRole.ATTACK, false, false, ChainCompatibility.ORDINARY, 1f, 1f, true, true, true, true, 0f, 0, 0, false, false, false, 8, 1, false, false, false, null, null, false));
        ReleaseRegistry.register(define("water_disperse", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 4, 0f, 0f, 0, false, false, false, false, ChainRole.ATTACK, false, false, ChainCompatibility.ORDINARY, 1f, 1f, true, true, true, true, 0f, 0, 0, false, false, false, 12, 1, false, false, false, null, null, false));
        ReleaseRegistry.register(define("phantom_sword_surge", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 1.5f, 0f, 0, false, false, false, false, ChainRole.ATTACK, false, true, ChainCompatibility.MANIPULATION, 1f, 1f, true, true, true, true, 0f, 0, 0, false, false, false, 12, 2, false, false, false, null, null, false));
        ReleaseRegistry.register(define("sword_guard", ReleaseKind.SELF_STATUS, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.EVENT_RESPONSE, false, false, ChainCompatibility.MANIPULATION, 1f, 1f, true, true, true, true, 0f, 0, 0, false, false, false, 4, 1, false, false, false, null, null, false));
        ReleaseRegistry.register(define("sword_array", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.TRAJECTORY_MANIPULATION, false, false, ChainCompatibility.MANIPULATION, 1f, 1f, true, true, true, true, 0f, 0, 0, false, false, false, 10, 1, false, false, false, null, null, false));
        ReleaseRegistry.register(define("sword_flight", ReleaseKind.SELF_STATUS, ReleaseTag.SUSTAINED, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.TRAJECTORY_MANIPULATION, false, true, ChainCompatibility.MANIPULATION, 1f, 1f, true, true, true, true, 0f, 0, 0, false, false, false, 0, 2, false, false, false, null, null, false));
        SelfStatusDefinition.registerAll();
        ReleaseRegistry.register(define("fire_liaoyuan", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 200, 0f, 0f, 0, false, false, false, false, ChainRole.ATTACK, false, false, ChainCompatibility.ORDINARY, 1f, 1f, true, true, true, false, 0f, 0, 0, false, false, false, 60, 3, false, false, false, null, null, false));
        ReleaseRegistry.register(define("fire_liaoli", ReleaseKind.HIT_VOLUME, ReleaseTag.SUSTAINED, Trajectory.FOLLOW_LOOK, false, 40, 0f, 0.6f, 0, false, false, false, false, ChainRole.ATTACK, false, false, ChainCompatibility.ORDINARY, 1f, 1f, true, true, true, false, 0f, 0, 0, false, false, false, 40, 3, true, true, true, null, null, false));
        ReleaseRegistry.register(define("fire_fentian", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 40, 0f, 0f, 0, false, false, false, false, ChainRole.ATTACK, false, false, ChainCompatibility.ORDINARY, 1f, 1f, true, true, true, false, 0f, 0, 0, false, false, false, 100, 3, false, false, false, null, null, false));
        ReleaseRegistry.register(define("fire_scorch_field", ReleaseKind.HIT_VOLUME, ReleaseTag.SUSTAINED, Trajectory.STRAIGHT, false, 100, 0f, 0f, 0, false, false, false, false, ChainRole.ATTACK, false, false, ChainCompatibility.ORDINARY, 1f, 1f, true, true, true, true, 0f, 0, 0, false, false, false, -1, -1, false, false, false, null, null, true));
        ReleaseRegistry.register(ListedRelease.meteorite());
        ReleaseRegistry.register(ListedRelease.shockRing());
        ReleaseRegistry.register(ListedRelease.spark());
        ReleaseRegistry.register(ListedRelease.surfaceAnchor());
        ReleaseRegistry.register(ListedRelease.upwell());
        ReleaseRegistry.register(ListedRelease.stagger());
        ReleaseRegistry.register(ListedRelease.hitContinue());
        ReleaseRegistry.register(ListedRelease.enchant("fire_flame_trail", ReleaseTag.SUSTAINED));
        ReleaseRegistry.register(ListedRelease.enchant("fire_seed_sow", ReleaseTag.TRIGGER));
        ReleaseRegistry.register(ListedRelease.enchant("fire_kindle", ReleaseTag.TRIGGER));
        ReleaseRegistry.register(ListedRelease.enchant("fire_detonate", ReleaseTag.TRIGGER));
        ReleaseRegistry.register(ListedRelease.gloomDomain(DomainDefinition.gloom()));
        ReleaseRegistry.register(ListedRelease.enchant("earth_break_ground", ReleaseTag.TRIGGER));
        CloneReleaseDefinition.registerAll();
    }

    /** 命中实体上的剑术斩面、水弧或水带。幻剑和召回不走这里。 */
    private static ReleaseDefinition cohortSword(String releaseId, ReleaseKind kind, ReleaseTag tag, Trajectory trajectory,
                                                  boolean moving, int lifetimeTicks, float speed, float thickness, int pullAfterA,
                                                  boolean acceptsTargetOverride, boolean acceptsPathOverride,
                                                  boolean setsTargetForNextDirectional, boolean setsDropForNextDirectional,
                                                  ChainRole role, boolean trajectoryMutable, boolean manipulatesSwordEntities) {
        return define(releaseId, kind, tag, trajectory, moving, lifetimeTicks, speed, thickness, pullAfterA,
                acceptsTargetOverride, acceptsPathOverride, setsTargetForNextDirectional, setsDropForNextDirectional,
                role, trajectoryMutable, manipulatesSwordEntities, ChainCompatibility.ORDINARY, 1f, 1f,
                true, true, true, true, 0f, 0, 0, true, false, false, -1, -1, false, false, false,
                null, null, false);
    }

    private static ReleaseDefinition define(String releaseId, ReleaseKind kind, ReleaseTag tag, Trajectory trajectory,
                                            boolean moving, int lifetimeTicks, float speed, float thickness, int pullAfterA,
                                            boolean acceptsTargetOverride, boolean acceptsPathOverride,
                                            boolean setsTargetForNextDirectional, boolean setsDropForNextDirectional,
                                            ChainRole role, boolean trajectoryMutable, boolean manipulatesSwordEntities) {
        return define(releaseId, kind, tag, trajectory, moving, lifetimeTicks, speed, thickness, pullAfterA,
                acceptsTargetOverride, acceptsPathOverride, setsTargetForNextDirectional, setsDropForNextDirectional,
                role, trajectoryMutable, manipulatesSwordEntities, null, null);
    }

    private static ReleaseDefinition define(String releaseId, ReleaseKind kind, ReleaseTag tag, Trajectory trajectory,
                                            boolean moving, int lifetimeTicks, float speed, float thickness, int pullAfterA,
                                            boolean acceptsTargetOverride, boolean acceptsPathOverride,
                                            boolean setsTargetForNextDirectional, boolean setsDropForNextDirectional,
                                            ChainRole role, boolean trajectoryMutable, boolean manipulatesSwordEntities,
                                            MulticastSpec declaredMulticast, ReplicationSpec declaredReplication) {
        return define(releaseId, kind, tag, trajectory, moving, lifetimeTicks, speed, thickness, pullAfterA,
                acceptsTargetOverride, acceptsPathOverride, setsTargetForNextDirectional, setsDropForNextDirectional,
                role, trajectoryMutable, manipulatesSwordEntities, ChainCompatibility.ORDINARY, 1f, 1f,
                true, true, true, true, 0f, 0, 0, false, false, false, -1, -1, false, false, false,
                declaredMulticast, declaredReplication, false);
    }

    private static ReleaseDefinition define(String releaseId, ReleaseKind kind, ReleaseTag tag, Trajectory trajectory,
                                            boolean moving, int lifetimeTicks, float speed, float thickness, int pullAfterA,
                                            boolean acceptsTargetOverride, boolean acceptsPathOverride,
                                            boolean setsTargetForNextDirectional, boolean setsDropForNextDirectional,
                                            ChainRole role, boolean trajectoryMutable, boolean manipulatesSwordEntities,
                                            ChainCompatibility compatibility) {
        return define(releaseId, kind, tag, trajectory, moving, lifetimeTicks, speed, thickness, pullAfterA,
                acceptsTargetOverride, acceptsPathOverride, setsTargetForNextDirectional, setsDropForNextDirectional,
                role, trajectoryMutable, manipulatesSwordEntities, compatibility, 1f, 1f,
                true, true, true, true, 0f, 0, 0, false, false, false, -1, -1, false, false, false,
                null, null, false);
    }

    private static ReleaseDefinition define(String releaseId, ReleaseKind kind, ReleaseTag tag, Trajectory trajectory,
                                            boolean moving, int lifetimeTicks, float speed, float thickness, int pullAfterA,
                                            boolean acceptsTargetOverride, boolean acceptsPathOverride,
                                            boolean setsTargetForNextDirectional, boolean setsDropForNextDirectional,
                                            ChainRole role, boolean trajectoryMutable, boolean manipulatesSwordEntities,
                                            ChainCompatibility compatibility, float energyScale, float chainEnergyScale) {
        return define(releaseId, kind, tag, trajectory, moving, lifetimeTicks, speed, thickness, pullAfterA,
                acceptsTargetOverride, acceptsPathOverride, setsTargetForNextDirectional, setsDropForNextDirectional,
                role, trajectoryMutable, manipulatesSwordEntities, compatibility, energyScale, chainEnergyScale,
                true, true, true, true, 0f, 0, 0, false, false, false, -1, -1, false, false, false,
                null, null, false);
    }

    private static ReleaseDefinition define(String releaseId, ReleaseKind kind, ReleaseTag tag, Trajectory trajectory,
                                            boolean moving, int lifetimeTicks, float speed, float thickness, int pullAfterA,
                                            boolean acceptsTargetOverride, boolean acceptsPathOverride,
                                            boolean setsTargetForNextDirectional, boolean setsDropForNextDirectional,
                                            ChainRole role, boolean trajectoryMutable, boolean manipulatesSwordEntities,
                                            ChainCompatibility compatibility, float energyScale, float chainEnergyScale,
                                            boolean scaleLength, boolean scaleWidth, boolean scaleHeight, boolean scaleModel,
                                            float heavyPerCopy, int pressureMinimum, int wetnessAdded,
                                            boolean cohortVisual, boolean persistentFlow, boolean deflectIncoming,
                                            int explicitCost, int assemblyCost, boolean heldSword,
                                            boolean freshPress, boolean endsOnRelease,
                                            MulticastSpec declaredMulticast, ReplicationSpec declaredReplication,
                                            boolean domainHostMarked) {
        return new ReleaseDefinition() {
            @Override
            public String id() {
                return releaseId;
            }

            @Override
            public ReleaseKind kind() {
                return kind;
            }

            @Override
            public ReleaseTag tag() {
                return tag;
            }

            @Override
            public Trajectory trajectory() {
                return trajectory;
            }

            @Override
            public boolean moving() {
                return moving;
            }

            @Override
            public int lifetimeTicks() {
                return SpecialBalance.integer(releaseId, BalanceKeys.LIFETIME_TICKS, lifetimeTicks);
            }

            @Override
            public float speed() {
                return SpecialBalance.decimal(releaseId, BalanceKeys.MOTION_SPEED, speed);
            }

            @Override
            public float thickness() {
                return SpecialBalance.decimal(releaseId, BalanceKeys.THICKNESS, thickness);
            }

            @Override
            public int pullAfterA() {
                return pullAfterA;
            }

            @Override
            public boolean acceptsTargetOverride() {
                return acceptsTargetOverride;
            }

            @Override
            public boolean acceptsPathOverride() {
                return acceptsPathOverride;
            }

            @Override
            public boolean setsTargetForNextDirectional() {
                return setsTargetForNextDirectional;
            }

            @Override
            public boolean setsDropForNextDirectional() {
                return setsDropForNextDirectional;
            }

            @Override
            public ChainRole role() {
                return role;
            }

            @Override
            public boolean trajectoryMutable() {
                return trajectoryMutable;
            }

            @Override
            public boolean manipulatesSwordEntities() {
                return manipulatesSwordEntities;
            }

            @Override
            public ChainCompatibility chainCompatibility() {
                return compatibility == null ? ChainCompatibility.ORDINARY : compatibility;
            }

            @Override
            public float energyScale() {
                return energyScale;
            }

            @Override
            public float chainEnergyScale() {
                return chainEnergyScale;
            }

            @Override
            public boolean energyScalesLength() {
                return scaleLength;
            }

            @Override
            public boolean energyScalesWidth() {
                return scaleWidth;
            }

            @Override
            public boolean energyScalesHeight() {
                return scaleHeight;
            }

            @Override
            public boolean energyScalesModel() {
                return scaleModel;
            }

            @Override
            public float heavyMomentumPerCopy() {
                return heavyPerCopy;
            }

            @Override
            public int pressureWetnessMinimum() {
                return pressureMinimum;
            }

            @Override
            public int wetnessGrant() {
                return wetnessAdded;
            }

            @Override
            public boolean separatesCohortVisuals() {
                return cohortVisual;
            }

            @Override
            public boolean persistentFlowSettlement() {
                return persistentFlow;
            }

            @Override
            public boolean deflectsProjectiles() {
                return deflectIncoming;
            }

            @Override
            public int explicitManaCost() {
                return explicitCost;
            }

            @Override
            public int explicitAssemblyCost() {
                return assemblyCost;
            }

            @Override
            public boolean requiresHeldSword() {
                return heldSword;
            }

            @Override
            public boolean requiresFreshPress() {
                return freshPress;
            }

            @Override
            public boolean endsWhenReleased() {
                return endsOnRelease;
            }

            @Override
            public MulticastSpec multicastSpec() {
                return declaredMulticast;
            }

            @Override
            public ReplicationSpec replication() {
                return declaredReplication;
            }

            @Override
            public boolean explicitDomainHost() {
                return domainHostMarked;
            }

            /** 只闭合构造参数的内置实现。结构 getter 返回这些纯配置。 */
            @Override
            public boolean supportsStructuralPlanCache() {
                return true;
            }
        };
    }

    /** 只声明本文件登记、并且玩法真的会读到的寿命、速度和厚度。寿命为 1 或速度为 0 的结构哨兵不登记。 */
    public static void declareSpecialBalances() {
        motion("movement_dash", true, true, false, "冲刺");
        motion("sword_horizontal_slash", true, false, false, "横斩");
        motion("sword_vertical_slash", true, false, false, "竖斩");
        motion("sword_thrust", true, false, false, "刺击");
        motion("magic_arcane_missile", true, true, false, "灵符");
        motion("test_sustained", true, true, false, "持续试射");
        motion("heat_beam", true, false, true, "热光束");
        motion("fire_orb", true, true, false, "火球");
        motion("fire_burst", true, true, true, "爆燃");
        motion("water_yielding_ribbon", true, false, false, "柔带");
        motion("water_weeping_arc", true, false, true, "垂丝");
        motion("space_cut", true, false, false, "空间切割");
        motion("parasite_seed", true, false, true, "寄生种子");
        motion("tide_sever", true, false, true, "断潮");
        motion("stone_hammer", true, false, false, "岩槌");
        motion("metal_needle", true, true, true, "金针");
        motion("phantom_sword", true, true, false, "幻剑");
        motion("fire_liaoyuan", true, false, false, "燎原");
        motion("fire_liaoli", true, false, true, "燎里");
        ListedRelease.declareSpecialBalances();
        MotionProfile.declareSpecialBalances();
        MovementDashEffect.declareBalance();
        MagicArcaneMissileEffect.declareBalance();
        TestSustainedEffect.declareBalance();
        FireOrbEffect.declareBalance();
        FireBurstEffect.declareBalance();
        HeatBeamEffect.declareBalance();
        MetalNeedleEffect.declareBalance();
        ParasiteSeedEffect.declareBalance();
        LiaoliEffect.declareBalance();
        FentianEffect.declareBalance();
        SwordHorizontalSlashEffect.declareBalance();
        SwordVerticalSlashEffect.declareBalance();
        SwordThrustEffect.declareBalance();
        SpaceCutEffect.declareBalance();
        WaterYieldingEffect.declareBalance();
        TideSeverEffect.declareBalance();
        StoneHammerEffect.declareBalance();
        WeepingCurtainEffect.declareBalance();
        LockTargetService.declareBalance();
        SwordComeEffect.declareBalance();
        SwordEntity.declareBalance();
    }

    private static void motion(String techniqueId, boolean includeLife, boolean includeSpeed, boolean includeThickness, String label) {
        ReleaseDefinition definition = ReleaseRegistry.get(techniqueId);
        if (definition == null) {
            return;
        }
        TechniqueBalanceSpec.Builder builder = SpecialBalance.begin(techniqueId);
        boolean included = false;
        if (includeLife && definition.lifetimeTicks() > 1) {
            builder = SpecialBalance.life(builder, definition.lifetimeTicks(), label + "寿命");
            included = true;
        }
        if (includeSpeed && definition.speed() > 0f) {
            builder = SpecialBalance.speed(builder, definition.speed(), label + "速度");
            included = true;
        }
        if (includeThickness && definition.thickness() > 0f) {
            builder = SpecialBalance.thickness(builder, definition.thickness(), label + "命中厚度");
            included = true;
        }
        if (included) {
            SpecialBalance.publish(builder);
        }
    }
}
