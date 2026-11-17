package com.bslx.XueXiMod.status.wood.resource;

/**
 * 花谱来源键。序数会写入存档并决定超限修复时的保留顺序，不能重排。
 * 这不是术式 ID，也不表示各自独立的资源余额。
 */
public enum FlowerKind {
    /** 桃花·灼华。序数 0，修复超限时最后被减。 */
    PEACH,
    /** 青竹·劲节。 */
    BAMBOO,
    /** 紫藤·垂影。 */
    WISTERIA,
    /** 寒梅·傲雪。 */
    PLUM,
    /** 幽兰·空谷。 */
    ORCHID,
    /** 彼岸·忘川。序数 5，修复超限时最先被减。 */
    HIGAN,
    /** 野蔷薇。序数 6，追加在原六项之后，修复超限时最先被减。 */
    ROSE;

    public static FlowerKind fromName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (FlowerKind kind : values()) {
            if (kind.name().equals(name)) {
                return kind;
            }
        }
        return null;
    }
}
