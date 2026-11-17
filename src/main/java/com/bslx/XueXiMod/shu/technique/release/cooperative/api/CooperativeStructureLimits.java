package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/**
 * 本批结构限额和站位常数。这些不是术式平衡键，整合时不得改成可配置项。
 * 平衡默认值仍由后续 BalanceService 读取，不在这里再放一份会覆盖配置的伤害或费用。
 */
public final class CooperativeStructureLimits {
    public static final int UNCOMMITTED_RESERVATION_TICKS = 80;
    public static final int PREPARE_TICK_CAP = 2;
    public static final int REPOSITION_TICK_CAP = 2;
    public static final int SWEEP_SAMPLE_CAP = 8;
    public static final int STANCE_PAIR_CAP = 2;
    public static final int OWNER_BODY_LOOKUP_CAP = 4;
    public static final int COOPERATION_RESERVATION_CAP = 128;
    public static final int COOPERATION_PREPARE_PER_TICK_CAP = 32;
    public static final int ACTIVE_FAMILY_CAP = 128;
    public static final int ACTIVE_FAMILIES_PER_OWNER = 4;
    public static final int FAMILY_PLANNED_EMIT_CAP = 128;
    public static final int LIVE_PLANNED_EMIT_CAP = 256;
    public static final int PUBLISH_PER_TICK_CAP = 128;
    public static final int TALLY_TARGET_CAP = 64;
    public static final int EMIT_PER_COPY_PER_ACTOR = 1;
    public static final int ACTORS_PER_COOPERATION = 2;
    public static final int OWNER_ACTOR_ORDINAL = 0;
    public static final int COOPERATING_ACTOR_ORDINAL = 1;
    public static final long OWNER_BODY_GENERATION = 0L;
    public static final int IMMEDIATE_SLASH_RELEASE_TICK = 0;
    public static final int IMMEDIATE_SLASH_RECOVERY_TICKS = 8;
    public static final double STANCE_FORWARD_BLOCKS = 0.5D;
    public static final double STANCE_DEVIATION_BLOCKS = 1.0D;
    public static final double SELECTION_RADIUS_CAP = 48.0D;
    public static final double MOVE_LIMIT_CAP = 64.0D;
    /** 短距扫掠与远距瞬时归位的固定分界。不是平衡键，短距端口仍只用这个上限。 */
    public static final double REPOSITION_DISTANCE_CAP = 8.0D;
    public static final int REMOTE_ENDPOINT_CELL_CAP = 128;
    public static final int REMOTE_CHANNEL_CELL_CAP = 512;
    public static final int REMOTE_CHANNEL_STEP_CAP = 128;
    public static final int REMOTE_CHANNEL_CHUNK_CAP = 16;
    public static final int REMOTE_CHANNEL_ENTITY_CAP = 64;
    public static final int REMOTE_SERVER_CELL_BUDGET = 2048;
    public static final int REMOTE_SERVER_ENTITY_BUDGET = 256;
    public static final double SIDE_OFFSET_LOW = 0.75D;
    public static final double SIDE_OFFSET_HIGH = 2.0D;
    public static final int WINDUP_LOW = 1;
    public static final int WINDUP_HIGH = 20;
    public static final int RECOVERY_LOW = 1;
    public static final int RECOVERY_HIGH = 40;
    public static final double BOX_SIZE_CAP = 512.0D;
    public static final double TILT_LOW = -80.0D;
    public static final double TILT_HIGH = 80.0D;
    public static final int WEAPON_TAG_ENTRY_CAP = 64;
    public static final int WEAPON_TAG_DEPTH_CAP = 8;
    public static final int WEAPON_TAG_STRING_CAP = 256;

    private CooperativeStructureLimits() {
    }
}
