package com.bslx.XueXiMod.client.action.mesh;

/** 玩家体型。按显式枚举选择，不根据玩家名猜测。 */
public enum PlayerMeshKind {
    NORMAL("normal", "src/main/resources/assets/epicfight/animmodels/entity/biped.json"),
    SLIM("slim", "src/main/resources/assets/epicfight/animmodels/entity/biped_slim_arm.json");

    private final String wireName;
    private final String sourceGitPath;

    PlayerMeshKind(String wireName, String sourceGitPath) {
        this.wireName = wireName;
        this.sourceGitPath = sourceGitPath;
    }

    public String wireName() {
        return this.wireName;
    }

    public String sourceGitPath() {
        return this.sourceGitPath;
    }

    public static PlayerMeshKind fromWireName(String wireName) {
        for (PlayerMeshKind kind : values()) {
            if (kind.wireName.equals(wireName)) {
                return kind;
            }
        }
        return null;
    }
}
