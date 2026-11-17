package com.bslx.XueXiMod.shu.technique.release.water;

/**
 * 径向刃片在同一宿主拍上的多份斩面。
 * 只按复制份数选择总滚转和对应扇区，不推进拍号，也不把实体倍率当成新的一份。
 */
public final class RadialCopyPlane {
    private RadialCopyPlane() {
    }

    /** 单份、非径向、或复制轴已经按既有路径散开时，不再加这一轮斩面。 */
    public static boolean spreads(ArcBandMode mode, int copyCount, boolean alreadySpread) {
        int safeCount = copyCount < 1 ? 1 : copyCount;
        return mode == ArcBandMode.RADIAL_BLADE && safeCount > 1 && !alreadySpread;
    }

    /**
     * 这一份的总滚转和平面内扇区。0 号份保留宿主拍的滚转与扇区。
     * {@code hostExtraRoll} 只属于宿主原来的附加滚转，不加到其它份的总滚转上。
     */
    public static Placement place(int copyIndex, int copyCount, int hostBeat, double hostExtraRoll) {
        int safeCount = copyCount < 1 ? 1 : copyCount;
        int safeCopy = copyIndex < 0 ? 0 : copyIndex;
        double hostRoll = ArcBandSnapshot.rollDegrees(hostBeat) + hostExtraRoll;
        if (safeCopy == 0 || safeCount <= 1) {
            return hostPlacement(hostBeat, hostRoll);
        }
        if (safeCount == 2) {
            return templatePlacement(Math.floorMod(Math.floorMod(hostBeat, 3) + 1, 3));
        }
        if (safeCount == 3) {
            return templatePlacement(Math.floorMod(Math.floorMod(hostBeat, 3) + safeCopy, 3));
        }
        return signedPlacement(wideRoll(safeCopy, safeCount, hostRoll));
    }

    public record Placement(double totalRollDegrees, double sectorStartDegrees, double sectorSweepDegrees,
                             boolean hostSector) {
        /** 冻结入口还会加上宿主拍滚转，所以附加角先减掉这一拍。 */
        public double extraRollDegrees(int hostBeat) {
            return this.totalRollDegrees - ArcBandSnapshot.rollDegrees(hostBeat);
        }
    }

    private static Placement hostPlacement(int hostBeat, double hostRoll) {
        return new Placement(hostRoll, ArcBandSnapshot.startDegrees(hostBeat), ArcBandSnapshot.sweepDegrees(hostBeat), true);
    }

    private static Placement templatePlacement(int template) {
        return new Placement(ArcBandSnapshot.rollDegrees(template), ArcBandSnapshot.startDegrees(template),
                ArcBandSnapshot.sweepDegrees(template), false);
    }

    private static Placement signedPlacement(double totalRoll) {
        int template = sweepTemplate(totalRoll);
        return new Placement(totalRoll, ArcBandSnapshot.startDegrees(template), ArcBandSnapshot.sweepDegrees(template), false);
    }

    /** 正滚转用左斜扇区，负滚转用右斜，恰好为零用水平。 */
    private static int sweepTemplate(double totalRoll) {
        if (totalRoll > 1.0E-9) {
            return 0;
        }
        if (totalRoll < -1.0E-9) {
            return 1;
        }
        return 2;
    }

    private static double wideRoll(int copyIndex, int copyCount, double hostRoll) {
        int kept = nearestSlot(hostRoll, copyCount);
        int passed = 0;
        for (int slot = 0; slot < copyCount; slot++) {
            if (slot == kept) {
                continue;
            }
            if (passed == copyIndex - 1) {
                return spacedRoll(slot, copyCount);
            }
            passed++;
        }
        return spacedRoll(copyCount - 1, copyCount);
    }

    /** 距离相同时取较低槽。这个槽的生成角会换成宿主原滚转，不留给其它份。 */
    private static int nearestSlot(double hostRoll, int copyCount) {
        double stepDegrees = 160.0 / (copyCount - 1);
        double ideal = (hostRoll + 80.0) / stepDegrees;
        int lower = (int) Math.floor(ideal);
        if (lower < 0) {
            return 0;
        }
        if (lower >= copyCount - 1) {
            return copyCount - 1;
        }
        double lowerRoll = spacedRoll(lower, copyCount);
        double upperRoll = spacedRoll(lower + 1, copyCount);
        double lowerGap = Math.abs(lowerRoll - hostRoll);
        double upperGap = Math.abs(upperRoll - hostRoll);
        if (upperGap < lowerGap) {
            return lower + 1;
        }
        return lower;
    }

    private static double spacedRoll(int slot, int copyCount) {
        if ((copyCount % 2) == 1 && slot == (copyCount - 1) / 2) {
            return 0.0;
        }
        return -80.0 + slot * (160.0 / (copyCount - 1));
    }
}
