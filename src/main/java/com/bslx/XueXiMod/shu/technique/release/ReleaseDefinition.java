package com.bslx.XueXiMod.shu.technique.release;

public interface ReleaseDefinition {
    String id();

    ReleaseKind kind();

    ReleaseTag tag();

    Trajectory trajectory();

    boolean moving();

    int lifetimeTicks();

    float speed();

    float thickness();

    default int pullAfterA() {
        return 0;
    }

    default boolean acceptsTargetOverride() {
        return false;
    }

    default boolean acceptsPathOverride() {
        return false;
    }

    default boolean setsTargetForNextDirectional() {
        return false;
    }

    default boolean setsDropForNextDirectional() {
        return false;
    }

    default ChainRole role() {
        return ChainRole.ATTACK;
    }

    /** 弹道修改能否改掉这条攻击原来的路径。不能改的只允许索敌改生成位置。 */
    default boolean trajectoryMutable() {
        return false;
    }

    /**
     * 是否为「操纵世界剑实体」类术式（幻剑术、后续召回/御剑等）。
     * 装配链含此类术式时：手持剑可放；空手仅当世界中存在该玩家的剑实体时可放。
     */
    default boolean manipulatesSwordEntities() {
        return false;
    }
}