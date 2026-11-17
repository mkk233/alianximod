package com.bslx.XueXiMod.shu.technique.release.entity;

/**
 * 一份命中实体自己的显示序号。值和结算序号分开，生成时抄成不可变记录。
 * 原始序号覆盖全部复制和实体倍率。布局槽只描述仍叠在一起的那一小组。
 */
public record CohortVisual(int displayIndex, int displayCount, int laneIndex, int laneCount, boolean laneSplit) {
    public static final CohortVisual SINGLE = new CohortVisual(0, 1, 0, 1, false);

    /** 结算序号仍是复制号加上生成号乘 1024。显示序号不借用这套编码。 */
    public static int settleEncoding(int copyIndex, int spawnIndex) {
        return copyIndex + spawnIndex * 1024;
    }

    /** 客户端从同步字段还原。不在这里重算哪一轴已经散开。 */
    public static CohortVisual read(int displayIndex, int displayCount, int laneIndex, int laneCount, boolean laneSplit) {
        return new CohortVisual(displayIndex, displayCount, laneIndex, laneCount, laneSplit);
    }

    /**
     * 显示总数是复制份数乘实体倍率，序号是复制号乘倍率再加本次生成号。
     * 两个散开轴都假时，布局槽就是这组全部序号。
     * 只有复制轴散开时，同一复制号里的生成号互分。
     * 只有生成轴散开、且同生成号几何相同，则按复制号互分。
     * 两轴都散开时不再加视觉槽。
     */
    public static CohortVisual capture(int copyIndex, int copyCount, int spawnIndex, int spawnCount,
                                       boolean swordVisual, boolean copyDispersed, boolean spawnDispersed) {
        int safeCopies = copyCount < 1 ? 1 : copyCount;
        int safeSpawns = spawnCount < 1 ? 1 : spawnCount;
        int safeCopy = copyIndex < 0 ? 0 : copyIndex;
        int safeSpawn = spawnIndex < 0 ? 0 : spawnIndex;
        int shownCount = saturatingProduct(safeCopies, safeSpawns);
        long rawIndex = (long) safeCopy * (long) safeSpawns + safeSpawn;
        int shownIndex = rawIndex > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) rawIndex;
        int localIndex;
        int localCount;
        if (copyDispersed && spawnDispersed) {
            localIndex = 0;
            localCount = 1;
        } else if (copyDispersed) {
            localIndex = safeSpawn;
            localCount = safeSpawns;
        } else if (spawnDispersed) {
            localIndex = safeCopy;
            localCount = safeCopies;
        } else {
            localIndex = shownIndex;
            localCount = shownCount;
        }
        boolean split = swordVisual && localCount > 1 && localIndex >= 0 && localIndex < localCount;
        return new CohortVisual(shownIndex, shownCount, localIndex, localCount, split);
    }

    /** 同步前保留原始序号。布局槽非法时只关闭分道，不改写原始序号。 */
    public CohortVisual stored() {
        int storedCount = this.displayCount < 1 ? 1 : this.displayCount;
        int storedIndex = this.displayIndex < 0 ? 0 : this.displayIndex;
        int storedLaneCount = this.laneCount < 1 ? 1 : this.laneCount;
        int storedLaneIndex = this.laneIndex < 0 ? 0 : this.laneIndex;
        boolean storedLane = this.laneSplit && storedLaneCount > 1 && storedLaneIndex < storedLaneCount;
        return new CohortVisual(storedIndex, storedCount, storedLaneIndex, storedLaneCount, storedLane);
    }

    /**
     * 绘制只看见本小组的槽号。单份、关闭分道或槽号越界都回到整张面。
     * 返回值不写回实体，原始序号留在原记录上。
     */
    public CohortVisual forDrawing() {
        CohortVisual stored = this.stored();
        if (!stored.laneSplit()) {
            return SINGLE;
        }
        return new CohortVisual(stored.laneIndex(), stored.laneCount(), stored.laneIndex(), stored.laneCount(), true);
    }

    private static int saturatingProduct(int left, int right) {
        long product = (long) left * (long) right;
        if (product > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) product;
    }
}
