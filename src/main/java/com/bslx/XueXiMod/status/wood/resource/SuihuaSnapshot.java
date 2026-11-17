package com.bslx.XueXiMod.status.wood.resource;

/**
 * 一名主人的岁华七项。总值为七项之和，不单独保存可能失配的总余额。
 * 单位是账本分辨率，不是“度”。度是 total / 60000。
 */
public record SuihuaSnapshot(int peach, int bamboo, int wisteria, int plum, int orchid, int higan, int rose) {
    public SuihuaSnapshot {
        if (peach < 0 || bamboo < 0 || wisteria < 0 || plum < 0 || orchid < 0 || higan < 0 || rose < 0) {
            throw new IllegalArgumentException("suihua component");
        }
        long sum = (long) peach + bamboo + wisteria + plum + orchid + higan + rose;
        if (sum > WoodLimits.FULL_UNITS) {
            throw new IllegalArgumentException("suihua cap");
        }
    }

    public static SuihuaSnapshot empty() {
        return new SuihuaSnapshot(0, 0, 0, 0, 0, 0, 0);
    }

    /**
     * 服务端修复。任一花谱为负则整份作废并返回 null，不保留其余正数。
     * 全为非负且超过三度时，从彼岸向桃花截掉超出的部分。
     */
    public static SuihuaSnapshot repairOrNull(int peach, int bamboo, int wisteria, int plum, int orchid, int higan, int rose) {
        if (peach < 0 || bamboo < 0 || wisteria < 0 || plum < 0 || orchid < 0 || higan < 0 || rose < 0) {
            return null;
        }
        return clamped(peach, bamboo, wisteria, plum, orchid, higan, rose);
    }

    /** 客户端显示钳制：负数当 0，再按彼岸优先的顺序截到三度。不产生服务端发奖。 */
    public static SuihuaSnapshot clamped(int peach, int bamboo, int wisteria, int plum, int orchid, int higan, int rose) {
        int[] amounts = new int[] {
                Math.max(peach, 0),
                Math.max(bamboo, 0),
                Math.max(wisteria, 0),
                Math.max(plum, 0),
                Math.max(orchid, 0),
                Math.max(higan, 0),
                Math.max(rose, 0)
        };
        long sum = 0L;
        for (int amount : amounts) {
            sum += amount;
        }
        long overflow = sum - WoodLimits.FULL_UNITS;
        for (int cursor = amounts.length - 1; cursor >= 0 && overflow > 0L; cursor--) {
            long removed = Math.min(amounts[cursor], overflow);
            amounts[cursor] -= (int) removed;
            overflow -= removed;
        }
        return new SuihuaSnapshot(amounts[0], amounts[1], amounts[2], amounts[3], amounts[4], amounts[5], amounts[6]);
    }

    public int total() {
        return peach + bamboo + wisteria + plum + orchid + higan + rose;
    }

    public int degrees() {
        return total() / WoodLimits.UNITS_PER_DEGREE;
    }

    public int amount(FlowerKind flower) {
        if (flower == null) {
            return 0;
        }
        return switch (flower) {
            case PEACH -> peach;
            case BAMBOO -> bamboo;
            case WISTERIA -> wisteria;
            case PLUM -> plum;
            case ORCHID -> orchid;
            case HIGAN -> higan;
            case ROSE -> rose;
        };
    }
}
