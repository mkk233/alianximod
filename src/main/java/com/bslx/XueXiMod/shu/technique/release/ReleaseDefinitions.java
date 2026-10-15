package com.bslx.XueXiMod.shu.technique.release;

public final class ReleaseDefinitions {
    private ReleaseDefinitions() {}

    public static void registerAll() {
        ReleaseRegistry.register(define("movement_dash", ReleaseKind.ACTION, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 10, 2f, 0f, 0, false, false, false, false, ChainRole.ACTION, false, false));
        ReleaseRegistry.register(define("sword_horizontal_slash", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 4, 0f, 0.5f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
        ReleaseRegistry.register(define("sword_vertical_slash", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 4, 0f, 0.5f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
        ReleaseRegistry.register(define("sword_thrust", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 4, 0f, 0.5f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
        ReleaseRegistry.register(define("magic_arcane_missile", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, true, 40, 1.5f, 0.1f, 0, true, true, false, false, ChainRole.ATTACK, true, false));
        ReleaseRegistry.register(define("test_sustained", ReleaseKind.HIT_VOLUME, ReleaseTag.SUSTAINED, Trajectory.DROP, true, 20, 0.4f, 0.1f, 0, false, true, false, false, ChainRole.ATTACK, true, false));
        ReleaseRegistry.register(define("heat_beam", ReleaseKind.HIT_VOLUME, ReleaseTag.SUSTAINED, Trajectory.FOLLOW_LOOK, false, 40, 0f, 1f, 0, true, true, false, false, ChainRole.ATTACK, true, false));
        ReleaseRegistry.register(define("universal_lock_target", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, true, false, ChainRole.SEEK_ENTITY, false, false));
        ReleaseRegistry.register(define("rule_dual_release", ReleaseKind.RULE, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 2, false, false, false, false, ChainRole.RULE_DUAL, false, false));
        ReleaseRegistry.register(define("trajectory_drop", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.DROP, false, 1, 0f, 0f, 0, false, false, false, true, ChainRole.TRAJECTORY_DROP, false, false));
        ReleaseRegistry.register(define("space_cut", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 10, 0f, 0f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
        ReleaseRegistry.register(define("rule_cast_anchor", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, true, false, ChainRole.SEEK_SPACE, false, false));
        ReleaseRegistry.register(define("rule_triple_cast", ReleaseKind.RULE, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.RULE_TRIPLE, false, false));
        ReleaseRegistry.register(define("trajectory_3d_interleave", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.TRAJECTORY_SCATTER, false, false));
        ReleaseRegistry.register(define("parasite_seed", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.DROP, true, 160, 0f, 0.5f, 0, false, true, false, false, ChainRole.ATTACK, true, false));
        ReleaseRegistry.register(define("tide_sever", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 6, 0f, 0.4f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
        ReleaseRegistry.register(define("stone_hammer", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 10, 0f, 1f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
        ReleaseRegistry.register(define("metal_needle", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, true, 15, 1.5f, 0.2f, 0, true, true, false, false, ChainRole.ATTACK, true, false));
        ReleaseRegistry.register(define("phantom_sword", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, true, 80, 2.5f, 0.5f, 0, true, true, false, false, ChainRole.ATTACK, true, false));
        ReleaseRegistry.register(define("sword_come", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 20, 0f, 0.5f, 0, false, false, false, false, ChainRole.ATTACK, false, true));
    }

    private static ReleaseDefinition define(String id, ReleaseKind kind, ReleaseTag tag, Trajectory trajectory,
                                            boolean moving, int lifetimeTicks, float speed, float thickness, int pullAfterA,
                                            boolean acceptsTargetOverride, boolean acceptsPathOverride,
                                            boolean setsTargetForNextDirectional, boolean setsDropForNextDirectional,
                                            ChainRole role, boolean trajectoryMutable, boolean manipulatesSwordEntities) {
        return new ReleaseDefinition() {
            @Override
            public String id() {
                return id;
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
                return lifetimeTicks;
            }

            @Override
            public float speed() {
                return speed;
            }

            @Override
            public float thickness() {
                return thickness;
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
        };
    }
}
