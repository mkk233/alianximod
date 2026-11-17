package com.bslx.XueXiMod.shu.technique.release.conditional;

import java.util.UUID;

/**
 * 群体目标的排序。费用口袋在父收据上，不在这里另记一套账。
 */
public final class TargetPayment {
    private TargetPayment() {
    }

    /**
     * 潮湿档位高的在前。同档距离平方更近的在前，再按目标编号稳定排序。
     */
    public static int captureOrder(int leftTier, double leftDistanceSquared, UUID leftSubject,
                                   int rightTier, double rightDistanceSquared, UUID rightSubject) {
        int tierOrder = Integer.compare(rightTier, leftTier);
        if (tierOrder != 0) {
            return tierOrder;
        }
        int distanceOrder = Double.compare(leftDistanceSquared, rightDistanceSquared);
        if (distanceOrder != 0) {
            return distanceOrder;
        }
        if (leftSubject == null || rightSubject == null) {
            if (leftSubject == rightSubject) {
                return 0;
            }
            return leftSubject == null ? 1 : -1;
        }
        return leftSubject.compareTo(rightSubject);
    }
}
