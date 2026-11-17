package com.bslx.XueXiMod.shu.technique.release.water;

/**
 * 当前角段的径向三角片到目标盒的欧氏间隙。
 * 面内部、盒棱刺入和共面重叠都算，不把命中收成三条边。
 */
final class RadialBladeVolume {
    private static final int[][] BOUND_EDGES = new int[][] {
            {0, 1}, {2, 3}, {4, 5}, {6, 7},
            {0, 2}, {1, 3}, {4, 6}, {5, 7},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    private RadialBladeVolume() {
    }

    static double gapToBounds(ArcVec first, ArcVec second, ArcVec third, ArcBounds target) {
        if (triangleCuts(first, second, third, target)) {
            return 0.0;
        }
        double best = ProgressArcBand.distanceSegmentToBounds(first, second, target);
        best = Math.min(best, ProgressArcBand.distanceSegmentToBounds(second, third, target));
        best = Math.min(best, ProgressArcBand.distanceSegmentToBounds(third, first, target));
        ArcVec[] corners = cornersOf(target);
        for (int cornerIndex = 0; cornerIndex < corners.length; cornerIndex++) {
            best = Math.min(best, distancePointToTriangle(corners[cornerIndex], first, second, third));
        }
        for (int edgeIndex = 0; edgeIndex < BOUND_EDGES.length; edgeIndex++) {
            best = Math.min(best, distanceSegmentToTriangle(
                    corners[BOUND_EDGES[edgeIndex][0]], corners[BOUND_EDGES[edgeIndex][1]], first, second, third));
        }
        return best;
    }

    private static boolean triangleCuts(ArcVec first, ArcVec second, ArcVec third, ArcBounds target) {
        if (ProgressArcBand.segmentHits(first, second, target)
                || ProgressArcBand.segmentHits(second, third, target)
                || ProgressArcBand.segmentHits(third, first, target)) {
            return true;
        }
        ArcVec[] corners = cornersOf(target);
        for (int edgeIndex = 0; edgeIndex < BOUND_EDGES.length; edgeIndex++) {
            if (segmentHitsTriangle(corners[BOUND_EDGES[edgeIndex][0]], corners[BOUND_EDGES[edgeIndex][1]], first, second, third)) {
                return true;
            }
        }
        return false;
    }

    private static ArcVec[] cornersOf(ArcBounds target) {
        double lowEast = target.lowEast();
        double highEast = target.highEast();
        double lowAbove = target.lowAbove();
        double highAbove = target.highAbove();
        double lowSouth = target.lowSouth();
        double highSouth = target.highSouth();
        return new ArcVec[] {
                new ArcVec(lowEast, lowAbove, lowSouth),
                new ArcVec(highEast, lowAbove, lowSouth),
                new ArcVec(lowEast, highAbove, lowSouth),
                new ArcVec(highEast, highAbove, lowSouth),
                new ArcVec(lowEast, lowAbove, highSouth),
                new ArcVec(highEast, lowAbove, highSouth),
                new ArcVec(lowEast, highAbove, highSouth),
                new ArcVec(highEast, highAbove, highSouth)
        };
    }

    private static double distanceSegmentToTriangle(ArcVec start, ArcVec end, ArcVec first, ArcVec second, ArcVec third) {
        double best = distancePointToTriangle(start, first, second, third);
        best = Math.min(best, distancePointToTriangle(end, first, second, third));
        best = Math.min(best, ProgressArcBand.segmentSegmentDistance(start, end, first, second));
        best = Math.min(best, ProgressArcBand.segmentSegmentDistance(start, end, second, third));
        best = Math.min(best, ProgressArcBand.segmentSegmentDistance(start, end, third, first));
        return Math.min(best, faceGap(start, end, first, second, third));
    }

    /** 线段投影落在三角面内时，平面间隙就是这段的面内距离。 */
    private static double faceGap(ArcVec start, ArcVec end, ArcVec first, ArcVec second, ArcVec third) {
        ArcVec edgeFirst = second.minus(first);
        ArcVec edgeSecond = third.minus(first);
        ArcVec normal = edgeFirst.cross(edgeSecond);
        double normalLength = normal.length();
        if (normalLength < 1.0E-12) {
            return Double.POSITIVE_INFINITY;
        }
        ArcVec unitNormal = normal.scale(1.0 / normalLength);
        double startGap = unitNormal.dot(start.minus(first));
        double endGap = unitNormal.dot(end.minus(first));
        ArcVec projectedStart = start.minus(unitNormal.scale(startGap));
        ArcVec projectedEnd = end.minus(unitNormal.scale(endGap));
        double[] window = new double[] {0.0, 1.0};
        if (!clipProjectedSegment(projectedStart, projectedEnd, first, second, third, window)) {
            return Double.POSITIVE_INFINITY;
        }
        double enterGap = startGap + (endGap - startGap) * window[0];
        double leaveGap = startGap + (endGap - startGap) * window[1];
        if (enterGap * leaveGap <= 0.0) {
            return 0.0;
        }
        return Math.min(Math.abs(enterGap), Math.abs(leaveGap));
    }

    private static boolean clipProjectedSegment(ArcVec start, ArcVec end, ArcVec first, ArcVec second, ArcVec third, double[] window) {
        double[] startWeights = barycentric(start, first, second, third);
        double[] endWeights = barycentric(end, first, second, third);
        if (startWeights == null || endWeights == null) {
            return false;
        }
        if (!clipHalfSpace(startWeights[0], endWeights[0], window)) {
            return false;
        }
        if (!clipHalfSpace(startWeights[1], endWeights[1], window)) {
            return false;
        }
        double startFace = 1.0 - startWeights[0] - startWeights[1];
        double endFace = 1.0 - endWeights[0] - endWeights[1];
        return clipHalfSpace(startFace, endFace, window);
    }

    private static boolean clipHalfSpace(double startValue, double endValue, double[] window) {
        double valueDelta = endValue - startValue;
        if (Math.abs(valueDelta) <= 1.0E-14) {
            return startValue >= -1.0E-8;
        }
        double crossing = -startValue / valueDelta;
        if (valueDelta > 0.0) {
            if (crossing > window[0]) {
                window[0] = crossing;
            }
        } else if (crossing < window[1]) {
            window[1] = crossing;
        }
        return window[0] <= window[1] + 1.0E-9;
    }

    private static double[] barycentric(ArcVec point, ArcVec first, ArcVec second, ArcVec third) {
        ArcVec edgeFirst = second.minus(first);
        ArcVec edgeSecond = third.minus(first);
        ArcVec offset = point.minus(first);
        double firstFirst = edgeFirst.dot(edgeFirst);
        double firstSecond = edgeFirst.dot(edgeSecond);
        double secondSecond = edgeSecond.dot(edgeSecond);
        double determinant = firstFirst * secondSecond - firstSecond * firstSecond;
        if (Math.abs(determinant) < 1.0E-18) {
            return null;
        }
        double inverse = 1.0 / determinant;
        double firstWeight = edgeFirst.dot(offset);
        double secondWeight = edgeSecond.dot(offset);
        double alongFirst = (secondSecond * firstWeight - firstSecond * secondWeight) * inverse;
        double alongSecond = (firstFirst * secondWeight - firstSecond * firstWeight) * inverse;
        return new double[] {alongFirst, alongSecond};
    }

    private static boolean segmentHitsTriangle(ArcVec start, ArcVec end, ArcVec first, ArcVec second, ArcVec third) {
        ArcVec edgeFirst = second.minus(first);
        ArcVec edgeSecond = third.minus(first);
        ArcVec normal = edgeFirst.cross(edgeSecond);
        double normalLength = normal.length();
        if (normalLength < 1.0E-12) {
            return false;
        }
        ArcVec unitNormal = normal.scale(1.0 / normalLength);
        double startGap = unitNormal.dot(start.minus(first));
        double endGap = unitNormal.dot(end.minus(first));
        double gapSlack = 1.0E-8;
        if (startGap > gapSlack && endGap > gapSlack) {
            return false;
        }
        if (startGap < -gapSlack && endGap < -gapSlack) {
            return false;
        }
        double gapDelta = endGap - startGap;
        if (Math.abs(gapDelta) <= 1.0E-12) {
            return coplanarSegmentHits(start, end, first, second, third);
        }
        double parameter = -startGap / gapDelta;
        if (parameter < -1.0E-8 || parameter > 1.0 + 1.0E-8) {
            return false;
        }
        double clamped = Math.max(0.0, Math.min(1.0, parameter));
        ArcVec hitPoint = start.plus(end.minus(start).scale(clamped));
        return pointInsideTriangle(hitPoint, first, second, third);
    }

    private static boolean coplanarSegmentHits(ArcVec start, ArcVec end, ArcVec first, ArcVec second, ArcVec third) {
        if (pointInsideTriangle(start, first, second, third) || pointInsideTriangle(end, first, second, third)) {
            return true;
        }
        double touch = 1.0E-6;
        return ProgressArcBand.segmentSegmentDistance(start, end, first, second) <= touch
                || ProgressArcBand.segmentSegmentDistance(start, end, second, third) <= touch
                || ProgressArcBand.segmentSegmentDistance(start, end, third, first) <= touch;
    }

    private static boolean pointInsideTriangle(ArcVec point, ArcVec first, ArcVec second, ArcVec third) {
        double[] weights = barycentric(point, first, second, third);
        if (weights == null) {
            return false;
        }
        double slack = 1.0E-8;
        return weights[0] >= -slack && weights[1] >= -slack && weights[0] + weights[1] <= 1.0 + slack;
    }

    private static double distancePointToTriangle(ArcVec point, ArcVec first, ArcVec second, ArcVec third) {
        ArcVec edgeFirst = second.minus(first);
        ArcVec edgeSecond = third.minus(first);
        ArcVec fromFirst = point.minus(first);
        double firstDot = edgeFirst.dot(fromFirst);
        double secondDot = edgeSecond.dot(fromFirst);
        if (firstDot <= 0.0 && secondDot <= 0.0) {
            return fromFirst.length();
        }
        ArcVec fromSecond = point.minus(second);
        double thirdDot = edgeFirst.dot(fromSecond);
        double fourthDot = edgeSecond.dot(fromSecond);
        if (thirdDot >= 0.0 && fourthDot <= thirdDot) {
            return fromSecond.length();
        }
        double edgeRegion = firstDot * fourthDot - thirdDot * secondDot;
        if (edgeRegion <= 0.0 && firstDot >= 0.0 && thirdDot <= 0.0) {
            double edgeSpan = firstDot - thirdDot;
            double along = Math.abs(edgeSpan) < 1.0E-18 ? 0.0 : firstDot / edgeSpan;
            return point.minus(first.plus(edgeFirst.scale(along))).length();
        }
        ArcVec fromThird = point.minus(third);
        double fifthDot = edgeFirst.dot(fromThird);
        double sixthDot = edgeSecond.dot(fromThird);
        if (sixthDot >= 0.0 && fifthDot <= sixthDot) {
            return fromThird.length();
        }
        double secondRegion = fifthDot * secondDot - firstDot * sixthDot;
        if (secondRegion <= 0.0 && secondDot >= 0.0 && sixthDot <= 0.0) {
            double edgeSpan = secondDot - sixthDot;
            double along = Math.abs(edgeSpan) < 1.0E-18 ? 0.0 : secondDot / edgeSpan;
            return point.minus(first.plus(edgeSecond.scale(along))).length();
        }
        double thirdRegion = thirdDot * sixthDot - fifthDot * fourthDot;
        double firstSide = fourthDot - thirdDot;
        double secondSide = fifthDot - sixthDot;
        if (thirdRegion <= 0.0 && firstSide >= 0.0 && secondSide >= 0.0) {
            double edgeSpan = firstSide + secondSide;
            double along = Math.abs(edgeSpan) < 1.0E-18 ? 0.0 : firstSide / edgeSpan;
            return point.minus(second.plus(third.minus(second).scale(along))).length();
        }
        double regionSum = edgeRegion + secondRegion + thirdRegion;
        if (Math.abs(regionSum) < 1.0E-18) {
            return Math.min(distancePointToSegment(point, first, second),
                    Math.min(distancePointToSegment(point, second, third), distancePointToSegment(point, third, first)));
        }
        double inverse = 1.0 / regionSum;
        double alongFirst = secondRegion * inverse;
        double alongSecond = edgeRegion * inverse;
        ArcVec closest = first.plus(edgeFirst.scale(alongFirst)).plus(edgeSecond.scale(alongSecond));
        return point.minus(closest).length();
    }

    private static double distancePointToSegment(ArcVec point, ArcVec start, ArcVec end) {
        ArcVec span = end.minus(start);
        double lengthSquared = span.lengthSquared();
        double along = 0.0;
        if (lengthSquared > 1.0E-18) {
            along = point.minus(start).dot(span) / lengthSquared;
            if (along < 0.0) {
                along = 0.0;
            } else if (along > 1.0) {
                along = 1.0;
            }
        }
        return point.minus(start.plus(span.scale(along))).length();
    }
}
