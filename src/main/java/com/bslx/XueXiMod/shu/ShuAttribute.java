package com.bslx.XueXiMod.shu;

/**
 * 原胚 / 术的元素属性。原胚属性不限制能刻什么术，只影响刻印时是否匹配加成。
 */
public enum ShuAttribute {
    NONE("none"),
    WOOD("wood"),
    FIRE("fire"),
    WATER("water"),
    EARTH("earth"),
    METAL("metal");

    private final String id;

    ShuAttribute(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getTranslationKey() {
        return "shu.attribute." + id;
    }

    /** Item model predicate: none=0, wood=0.1, fire=0.2, water=0.3, earth=0.4, metal=0.5 */
    public float getPredicateValue() {
        return ordinal() * 0.1f;
    }

    public static ShuAttribute fromId(String id) {
        if (id == null || id.isEmpty()) return NONE;
        for (ShuAttribute attribute : values()) {
            if (attribute.id.equals(id)) return attribute;
        }
        return NONE;
    }
}
