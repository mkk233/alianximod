package com.bslx.XueXiMod.shu.technique.release.deflection.projectile;

/**
 * 水体必须严格早于方块和合法实体。已经处理过的前段不能再被后面的水追认。
 * 无效参数和已处理前段只跳过；方块终点和当前合法实体才是停下的障碍。
 */
public final class ArrowSegmentOrder {
    public static final double EARLIER_GAP = 1.0E-6;
    public static final int PLACE_SKIP = 1;
    public static final int PLACE_HANDLED = 2;
    public static final int PLACE_OBSTACLE = 3;
    public static final int PLACE_OPEN = 4;

    private ArrowSegmentOrder() {
    }

    /**
     * 按无效、已处理前段、真实障碍的顺序分类。已处理前段不是障碍。
     */
    public static int classify(double waterT, boolean blockLimited, double entityT, boolean entityHit,
                               double alreadyHandled) {
        if (!Double.isFinite(waterT) || waterT < 0.0 || waterT > 1.0) {
            return PLACE_SKIP;
        }
        if (waterT <= alreadyHandled + EARLIER_GAP) {
            return PLACE_HANDLED;
        }
        if (blockLimited && waterT >= 1.0 - EARLIER_GAP) {
            return PLACE_OBSTACLE;
        }
        if (entityHit && Double.isFinite(entityT) && waterT >= entityT - EARLIER_GAP) {
            return PLACE_OBSTACLE;
        }
        return PLACE_OPEN;
    }

    /**
     * 同一条已处理界限贯穿全部候选，跳过前段时不把界限改成后面的实体。
     * 遇到障碍就停，障碍后面的水不进入可考虑名单。
     */
    public static Passage passage(double[] waterTimes, boolean blockLimited, double entityT, boolean entityHit,
                                  double alreadyHandled) {
        if (waterTimes == null) {
            return new Passage(new int[0], false);
        }
        int[] chosen = new int[waterTimes.length];
        int chosenCount = 0;
        boolean blocked = false;
        for (int cursor = 0; cursor < waterTimes.length; cursor++) {
            int verdict = classify(waterTimes[cursor], blockLimited, entityT, entityHit, alreadyHandled);
            if (verdict == PLACE_SKIP || verdict == PLACE_HANDLED) {
                continue;
            }
            if (verdict != PLACE_OPEN) {
                blocked = true;
                break;
            }
            chosen[chosenCount] = cursor;
            chosenCount++;
        }
        int[] admitted = new int[chosenCount];
        System.arraycopy(chosen, 0, admitted, 0, chosenCount);
        return new Passage(admitted, blocked);
    }

    /** 命中点在飞行线段上的参数。 */
    public static double parameter(double fromX, double fromY, double fromZ,
                                   double towardX, double towardY, double towardZ,
                                   double pointX, double pointY, double pointZ) {
        double spanX = towardX - fromX;
        double spanY = towardY - fromY;
        double spanZ = towardZ - fromZ;
        double scale = spanX * spanX + spanY * spanY + spanZ * spanZ;
        if (scale < 1.0E-12) {
            return 0.0;
        }
        return ((pointX - fromX) * spanX + (pointY - fromY) * spanY + (pointZ - fromZ) * spanZ) / scale;
    }

    /** 水体是否严格早于障碍，并且不在已处理前段里。 */
    public static boolean waterEarlier(double waterT, boolean blockLimited, double entityT, boolean entityHit,
                                       double alreadyHandled) {
        return classify(waterT, blockLimited, entityT, entityHit, alreadyHandled) == PLACE_OPEN;
    }

    public static final class Passage {
        private final int[] admitted;
        private final boolean blocked;

        private Passage(int[] admitted, boolean blocked) {
            this.admitted = admitted;
            this.blocked = blocked;
        }

        /** 可以继续考虑的水体下标。 */
        public int[] admitted() {
            return this.admitted.clone();
        }

        /** 后面是否被方块或合法实体挡住。 */
        public boolean blocked() {
            return this.blocked;
        }
    }
}
