package com.bslx.XueXiMod.client.action.mesh;

/** 十二个皮肤分区。顺序就是绘制顺序。 */
public enum PlayerMeshPart {
    HEAD("head"),
    HAT("hat"),
    TORSO("torso"),
    JACKET("jacket"),
    LEFT_ARM("leftArm"),
    RIGHT_ARM("rightArm"),
    LEFT_SLEEVE("leftSleeve"),
    RIGHT_SLEEVE("rightSleeve"),
    LEFT_LEG("leftLeg"),
    RIGHT_LEG("rightLeg"),
    LEFT_PANTS("leftPants"),
    RIGHT_PANTS("rightPants");

    private final String wireName;

    PlayerMeshPart(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return this.wireName;
    }

    public static PlayerMeshPart fromWireName(String wireName) {
        for (PlayerMeshPart part : values()) {
            if (part.wireName.equals(wireName)) {
                return part;
            }
        }
        return null;
    }
}
