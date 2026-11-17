package com.bslx.XueXiMod.client.action.mesh;

/** 通用上限。真实顶点数只要落在上限内即可，不把某一份网格的计数写成唯一合法值。 */
public final class MeshLimits {
    public static final int FILE_BYTE_LIMIT = 8_000_000;
    public static final int POSITION_LIMIT = 4_096;
    public static final int NORMAL_LIMIT = 256;
    public static final int TEXTURE_LIMIT = 4_096;
    public static final int WEIGHT_LIMIT = 4_096;
    public static final int INFLUENCE_LIMIT = 8_192;
    public static final int INFLUENCES_PER_POSITION_LIMIT = 4;
    public static final int TRIANGLE_LIMIT = 8_192;
    public static final int NORMAL_PAIR_LIMIT = 65_536;
    public static final int JOINT_LIMIT = 20;
    public static final float WEIGHT_SUM_TOLERANCE = 0.001F;
    public static final String UPSTREAM_COMMIT = "9d25f19c121c5e51b1db9efe01833a563706eeee";
    public static final String RIG_RELATIVE_PATH = "rig/biped-armature.json";
    public static final String AUTHORIZATION = "local-dev-only player mesh and skin weights";
    public static final String[] JOINT_NAMES = {
            "Root", "Thigh_R", "Leg_R", "Knee_R", "Thigh_L", "Leg_L", "Knee_L", "Torso", "Chest", "Head",
            "Shoulder_R", "Arm_R", "Hand_R", "Tool_R", "Elbow_R", "Shoulder_L", "Arm_L", "Hand_L", "Tool_L", "Elbow_L"
    };

    private MeshLimits() {
    }
}
