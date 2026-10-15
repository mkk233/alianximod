package com.bslx.XueXiMod.shu;

public final class ElementReaction {

    public enum Kind { NONE, SAME, GENERATE }

    private final Kind kind;
    private final ShuAttribute embryo;
    private final ShuAttribute spell;

    private ElementReaction(Kind kind, ShuAttribute embryo, ShuAttribute spell) {
        this.kind = kind;
        this.embryo = embryo;
        this.spell = spell;
    }

    public static ElementReaction of(ShuAttribute embryo, ShuAttribute spell) {
        if (embryo == null || embryo == ShuAttribute.NONE
                || spell == null || spell == ShuAttribute.NONE) {
            return new ElementReaction(Kind.NONE, embryo, spell);
        }
        if (embryo == spell) {
            return new ElementReaction(Kind.SAME, embryo, spell);
        }
        if (generates(embryo, spell)) {
            return new ElementReaction(Kind.GENERATE, embryo, spell);
        }
        return new ElementReaction(Kind.NONE, embryo, spell);
    }

    private static boolean generates(ShuAttribute embryo, ShuAttribute spell) {
        return (embryo == ShuAttribute.WOOD && spell == ShuAttribute.FIRE)
                || (embryo == ShuAttribute.FIRE && spell == ShuAttribute.EARTH)
                || (embryo == ShuAttribute.EARTH && spell == ShuAttribute.METAL)
                || (embryo == ShuAttribute.METAL && spell == ShuAttribute.WATER)
                || (embryo == ShuAttribute.WATER && spell == ShuAttribute.WOOD);
    }

    public Kind kind() {
        return kind;
    }

    public float damageMultiplier() {
        return kind == Kind.SAME ? 1.30f : 1f;
    }

    public float cooldownMultiplier() {
        return kind == Kind.SAME ? 0.70f : 1f;
    }

    public float radiusMultiplier() {
        return generates(embryo, spell) && embryo == ShuAttribute.WOOD && spell == ShuAttribute.FIRE ? 1.10f : 1f;
    }

    public float durationMultiplier() {
        return generates(embryo, spell)
                && ((embryo == ShuAttribute.WOOD && spell == ShuAttribute.FIRE)
                    || (embryo == ShuAttribute.WATER && spell == ShuAttribute.WOOD)) ? 1.10f : 1f;
    }

    public float distanceMultiplier() {
        return generates(embryo, spell) && embryo == ShuAttribute.FIRE && spell == ShuAttribute.EARTH ? 1.10f : 1f;
    }

    public float searchMultiplier() {
        return generates(embryo, spell) && embryo == ShuAttribute.EARTH && spell == ShuAttribute.METAL ? 1.10f : 1f;
    }

    public float speedMultiplier() {
        return generates(embryo, spell) && embryo == ShuAttribute.METAL && spell == ShuAttribute.WATER ? 1.10f : 1f;
    }

    public String describe() {
        switch (kind) {
            case SAME:
                return "同属性：伤害 +30%，冷却 -30%";
            case GENERATE:
                if (embryo == ShuAttribute.WOOD && spell == ShuAttribute.FIRE) {
                    return "木生火：半径 +10%，持续时间 +10%";
                }
                if (embryo == ShuAttribute.FIRE && spell == ShuAttribute.EARTH) {
                    return "火生土：距离 +10%";
                }
                if (embryo == ShuAttribute.EARTH && spell == ShuAttribute.METAL) {
                    return "土生金：搜索范围 +10%";
                }
                if (embryo == ShuAttribute.METAL && spell == ShuAttribute.WATER) {
                    return "金生水：速度 +10%";
                }
                if (embryo == ShuAttribute.WATER && spell == ShuAttribute.WOOD) {
                    return "水生木：持续时间 +10%";
                }
                return "无属性反应";
            case NONE:
            default:
                return "无属性反应";
        }
    }
}
