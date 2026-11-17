package com.bslx.XueXiMod.shu.technique.release.water;

import java.util.Arrays;

/** 直接调用生产弧带。不启动 Minecraft，因此不覆盖实体同步、宿主事务和渲染器。 */
public final class ArcBandGeometryCheck {
    public static void main(String[] arguments) {
        configuredArcPointsStayPut();
        narrowVolumeNotBroadBox();
        catchUpThenStop();
        scaleOnce();
        largeSweepStillReachesEnd();
        packRestoresSameArc();
        trueArcSurvivesChordGap();
        faceDistanceStays();
        singleTurnDomain();
        radialBladeReachesFace();
        radialSweepStaysOnItsSlice();
        radialPackKeepsMode();
        radialWorkStayBounded();
        System.out.println("弧带几何检查通过");
    }

    private static void configuredArcPointsStayPut() {
        ArcBandSnapshot arc = quarter();
        ArcVec start = ProgressArcBand.point(arc, 0.0);
        ArcVec middle = ProgressArcBand.point(arc, 0.5);
        ArcVec end = ProgressArcBand.point(arc, 1.0);
        check(near(start, new ArcVec(0.0, 0.0, 2.4)), "进度 0 应在弧起势");
        check(near(end, new ArcVec(2.4, 0.0, 0.0)), "进度 1 应在弧收势");
        check(Math.abs(middle.east() - 1.6970562748) < 1.0E-6, "进度 0.5 的东向");
        check(Math.abs(middle.south() - 1.6970562748) < 1.0E-6, "进度 0.5 的南向");
        check(near(start, ProgressArcBand.point(arc, 0.0)), "冻结后重复采样不变");
        check(ProgressArcBand.intersectsSlice(arc, new ArcBounds(0, 0, 0, 0, 0, 0), 0.4, 0.4) == false, "没有进度增长不算命中");
    }

    private static void narrowVolumeNotBroadBox() {
        ArcBandSnapshot arc = quarter();
        ArcVec middle = ProgressArcBand.point(arc, 0.0);
        ArcVec radial = ProgressArcBand.direction(arc, 0.0);
        ArcVec touch = middle.plus(radial.scale(arc.halfWidth()));
        ArcBounds edgeBox = new ArcBounds(touch.east(), touch.above() - 0.04, touch.south(),
                touch.east() + 0.5, touch.above() + 0.04, touch.south() + 0.5);
        check(ProgressArcBand.intersectsSlice(arc, edgeBox, 0.0, 1.0), "盒边碰到弧带要命中");
        ArcVec corner = middle.plus(radial.scale(0.08));
        ArcBounds fat = new ArcBounds(corner.east(), corner.above() - 0.3, corner.south(),
                corner.east() + 3.0, corner.above() + 3.0, corner.south() + 3.0);
        check(ProgressArcBand.intersectsSlice(arc, fat, 0.0, 1.0), "大盒中心在外但边缘相交仍要命中");
        ArcBounds inland = new ArcBounds(0.25, -0.1, 0.25, 0.55, 0.1, 0.55);
        ArcBounds broad = ProgressArcBand.sliceBroadBounds(arc, 0.0, 1.0);
        check(broad.overlaps(inland), "远离弧带的盒子仍落在粗包围盒里");
        check(!ProgressArcBand.intersectsSlice(arc, inland, 0.0, 1.0), "粗包围盒里面但远离弧带不能命中");
        double gap = ProgressArcBand.distanceSegmentToBounds(
                new ArcVec(-3.0, 0.0, 1.0), new ArcVec(3.0, 0.0, 1.0),
                new ArcBounds(-2.0, -2.0, 0.0, 2.0, 2.0, 0.0));
        check(Math.abs(gap - 1.0) < 1.0E-6, "平行于盒面的线段距离应是到面的距离");
    }

    private static void catchUpThenStop() {
        ProgressArcBand.SweepCursor caught = ProgressArcBand.advance(5.0 / 6.0, false, 7L, 6);
        check(caught.swept(), "从 5/6 跳到年龄 7 要补扫");
        check(Math.abs(caught.fromProgress() - 5.0 / 6.0) < 1.0E-9, "补扫起点是上次进度");
        check(Math.abs(caught.toProgress() - 1.0) < 1.0E-9, "补扫要到达进度 1");
        check(caught.damageClosed(), "补到活跃拍数后关闭伤害");
        ProgressArcBand.SweepCursor later = ProgressArcBand.advance(caught.lastProgress(), true, 8L, 6);
        check(!later.swept(), "年龄 8 不再产生新弧段");
        ProgressArcBand.SweepCursor idle = ProgressArcBand.advance(0.5, false, 3L, 6);
        check(!idle.swept(), "进度没有增长时不制造命中");
        check(arcLife() == 10, "6 拍伤害加 4 拍余流");
    }

    private static void scaleOnce() {
        double scale = 1.35 * 2.0 * 1.25;
        ArcBandSnapshot scaled = ArcBandSnapshot.freezeLook(
                new ArcVec(4.0, 1.0, -2.0), new ArcVec(0.0, 0.0, 1.0), null, false,
                0.0, 0, ArcBandSnapshot.BASE_RADIUS, ArcBandSnapshot.BASE_HALF_WIDTH, scale);
        check(scaled != null, "合成倍率要能冻出弧");
        check(Math.abs(scaled.radius() - ArcBandSnapshot.BASE_RADIUS * scale) < 1.0E-9, "半径只乘一次合成倍率");
        check(Math.abs(scaled.halfWidth() - ArcBandSnapshot.BASE_HALF_WIDTH * scale) < 1.0E-9, "半宽只乘一次合成倍率");
        ArcVec sample = ProgressArcBand.point(scaled, 0.3);
        double reach = sample.minus(scaled.origin()).length();
        check(Math.abs(reach - scaled.radius()) < 1.0E-6, "采样点落在最终半径上");
        check(scaled.beat() == 0, "本批冻结拍号是 0");
        check(Math.abs(Math.toDegrees(scaled.startAngle()) + 50.0) < 1.0E-6, "第 0 拍从 -50 度起势");
        check(Math.abs(Math.toDegrees(scaled.sweepAngle()) - 90.0) < 1.0E-6, "第 0 拍扫 90 度");
    }

    private static void largeSweepStillReachesEnd() {
        ArcBandSnapshot wide = ArcBandSnapshot.of(
                new ArcVec(0.0, 0.0, 0.0),
                new ArcVec(0.0, 0.0, 1.0),
                new ArcVec(1.0, 0.0, 0.0),
                new ArcVec(0.0, 1.0, 0.0),
                0, 20.0, 0.05, 0.0, Math.toRadians(300.0), -1L, 6, 4);
        check(wide != null, "大角度弧要能构造");
        check(ProgressArcBand.segmentCount(wide, 0.0, 1.0) > 8, "大角度不能停在 8 段");
        ArcVec end = ProgressArcBand.point(wide, 1.0);
        ArcBounds tip = new ArcBounds(end.east() - 0.02, end.above() - 0.02, end.south() - 0.02,
                end.east() + 0.02, end.above() + 0.02, end.south() + 0.02);
        check(ProgressArcBand.intersectsSlice(wide, tip, 0.0, 1.0), "末段仍要被检查");
    }

    private static void packRestoresSameArc() {
        ArcBandSnapshot frozen = ArcBandSnapshot.freezeLook(
                new ArcVec(1.5, 2.0, -0.5), new ArcVec(0.2, 0.1, 0.9), null, false,
                12.0, 0, 2.4, 0.16, 1.35);
        check(frozen != null, "斜向瞄准要能冻结");
        ArcBandSnapshot born = frozen.withBirth(80L);
        ArcBandSnapshot restored = ArcBandSnapshot.unpack(born.pack());
        check(restored != null, "同步串要能还原");
        check(restored.birthTick() == 80L, "激活时刻要跟着同步串走");
        check(restored.beat() == frozen.beat(), "拍号要还原");
        ArcVec original = ProgressArcBand.point(born, 0.5);
        ArcVec again = ProgressArcBand.point(restored, 0.5);
        check(original.minus(again).length() < 1.0E-4, "还原后的中点仍是同一条弧");
        check(ArcBandSnapshot.of(new ArcVec(0, 0, 0), new ArcVec(1, 0, 0), new ArcVec(1, 0, 0), new ArcVec(0, 1, 0),
                0, 1.0, 0.1, 0.0, 1.0, -1L, 6, 4) == null, "退化基要拒绝");
    }

    private static void trueArcSurvivesChordGap() {
        ArcBandSnapshot arc = quarterCircle(2.0, 0.1);
        int pieces = ProgressArcBand.segmentCount(arc, 0.0, 1.0);
        double along = (9.0 / 90.0) * pieces;
        check(Math.abs(along - Math.floor(along) - 0.5) < 1.0E-6, "9度探针仍在弦段中点");
        double outerAngle = Math.toRadians(9.0);
        ArcBounds circularOuter = cube(2.099 * Math.cos(outerAngle), 2.099 * Math.sin(outerAngle), 0.0, 0.0001);
        ArcBounds broad = ProgressArcBand.sliceBroadBounds(arc, 0.0, 1.0);
        check(broad.overlaps(circularOuter), "粗盒要包住 9 度真实外缘");
        check(ProgressArcBand.intersectsSlice(arc, circularOuter, 0.0, 1.0), "9度真实外缘不能被弓高漏掉");
        ArcBounds outsideCircular = cube(2.2 * Math.cos(outerAngle), 2.2 * Math.sin(outerAngle), 0.0, 0.0001);
        check(!ProgressArcBand.intersectsSlice(arc, outsideCircular, 0.0, 1.0), "2.2 半径仍在弧带外面");
        ArcBounds circularInner = cube(1.901 * Math.cos(outerAngle), 1.901 * Math.sin(outerAngle), 0.0, 0.0001);
        check(ProgressArcBand.intersectsSlice(arc, circularInner, 0.0, 1.0), "9度真实内缘要命中");
        ArcBounds hole = cube(0.0, 0.0, 0.0, 0.05);
        check(broad.overlaps(hole), "圆心落在粗盒里");
        check(!ProgressArcBand.intersectsSlice(arc, hole, 0.0, 1.0), "粗盒里的圆心不能命中");

        ArcBandSnapshot turned = ArcBandSnapshot.of(
                new ArcVec(5.0, 7.0, 11.0),
                new ArcVec(0.0, 0.0, -1.0),
                new ArcVec(0.0, 1.0, 0.0),
                new ArcVec(1.0, 0.0, 0.0),
                0, 2.0, 0.1, 0.0, Math.PI / 2.0, 0L, 6, 4);
        check(turned != null, "旋转平面要能构造");
        ArcVec turnedOuter = turned.origin().plus(ProgressArcBand.direction(turned, 9.0 / 90.0).scale(2.099));
        check(ProgressArcBand.intersectsSlice(turned, cube(turnedOuter.east(), turnedOuter.above(), turnedOuter.south(), 0.0001), 0.0, 1.0),
                "旋转平面的 9 度外缘要命中");

        ArcBandSnapshot scaled = ArcBandSnapshot.freezeLook(
                new ArcVec(0.0, 0.0, 0.0), new ArcVec(0.0, 0.0, 1.0), null, false,
                0.0, 0, 2.4, 0.16, 1.35 * 2.0);
        check(scaled != null, "大倍率弧要能冻结");
        int scaledPieces = ProgressArcBand.segmentCount(scaled, 0.0, 1.0);
        double scaledMid = 0.5 / scaledPieces;
        check(Math.abs(scaledMid * scaledPieces - Math.floor(scaledMid * scaledPieces) - 0.5) < 1.0E-6, "大倍率探针在弦段中点");
        ArcVec scaledOuter = scaled.origin().plus(ProgressArcBand.direction(scaled, scaledMid)
                .scale(scaled.radius() + scaled.halfWidth() - 0.0002));
        check(ProgressArcBand.intersectsSlice(scaled, cube(scaledOuter.east(), scaledOuter.above(), scaledOuter.south(), 0.0001), 0.0, 1.0),
                "放大后的真实外缘要命中");

        ArcBandSnapshot capped = ArcBandSnapshot.of(
                new ArcVec(0.0, 0.0, 0.0),
                new ArcVec(1.0, 0.0, 0.0),
                new ArcVec(0.0, 1.0, 0.0),
                new ArcVec(0.0, 0.0, 1.0),
                0, 2000.0, 0.1, 0.0, Math.PI * 2.0, -1L, 6, 4);
        check(capped != null, "大半径整圈要能构造");
        check(ProgressArcBand.segmentCount(capped, 0.0, 1.0) == 256, "大半径整圈顶到 256 段");
        double cappedMid = 0.5 / 256.0;
        ArcVec cappedRadial = ProgressArcBand.direction(capped, cappedMid);
        ArcVec cappedOuter = capped.origin().plus(cappedRadial.scale(capped.radius() + capped.halfWidth() - 0.00005));
        ArcBounds cappedOuterBox = cube(cappedOuter.east(), cappedOuter.above(), cappedOuter.south(), 0.0001);
        check(ProgressArcBand.sliceBroadBounds(capped, 0.0, 1.0).overlaps(cappedOuterBox), "顶段数时粗盒仍包住外缘");
        check(ProgressArcBand.intersectsSlice(capped, cappedOuterBox, 0.0, 1.0), "顶到 256 段时真实外缘仍要命中");
        ArcBounds cappedHole = cube(0.0, 0.0, 0.0, 0.5);
        check(ProgressArcBand.sliceBroadBounds(capped, 0.0, 1.0).overlaps(cappedHole), "整圈粗盒盖住圆心");
        check(!ProgressArcBand.intersectsSlice(capped, cappedHole, 0.0, 1.0), "顶段数后圆心仍然落空");
    }

    private static void singleTurnDomain() {
        ArcBandSnapshot forwardTurn = flatSweep(Math.PI * 2.0);
        ArcBandSnapshot reverseTurn = flatSweep(-Math.PI * 2.0);
        check(forwardTurn != null, "正一圈要能构造");
        check(reverseTurn != null, "负一圈要能构造");
        check(flatSweep(Math.nextUp(Math.PI * 2.0)) == null, "正一圈的下一个浮点要拒绝");
        check(flatSweep(Math.nextDown(-Math.PI * 2.0)) == null, "负一圈的下一个浮点要拒绝");
        check(flatSweep(Math.PI * 2.0 + 1.0E-6) == null, "略超过一圈要拒绝");
        check(flatSweep(-Math.PI * 2.0 - 1.0E-6) == null, "略超过负一圈要拒绝");
        check(flatSweep(1.0E6) == null, "巨大有限扫角要拒绝");
        check(flatSweep(Double.NaN) == null, "非数扫角要拒绝");
        check(flatSweep(Double.POSITIVE_INFINITY) == null, "正无穷扫角要拒绝");
        check(flatSweep(Double.NEGATIVE_INFINITY) == null, "负无穷扫角要拒绝");
        check(ArcBandDraft.of(0, Double.NaN, 0.16) == null, "草稿上的非数半径要拒绝");
        String[] tokens = forwardTurn.pack().split(" ");
        tokens[16] = "100";
        check(ArcBandSnapshot.unpack(String.join(" ", tokens)) == null, "同步串不能绕过扫角上限");
        ArcBandSnapshot restored = ArcBandSnapshot.unpack(forwardTurn.pack());
        ArcBandSnapshot reverseRestored = ArcBandSnapshot.unpack(reverseTurn.pack());
        check(restored != null && restored.sweepAngle() == forwardTurn.sweepAngle(), "正一圈打包后扫角不变");
        check(reverseRestored != null && reverseRestored.sweepAngle() == reverseTurn.sweepAngle(), "负一圈打包后扫角不变");

        ArcVec start = ProgressArcBand.point(forwardTurn, 0.0);
        ArcVec finish = ProgressArcBand.point(forwardTurn, 1.0);
        check(start.minus(finish).length() < 1.0E-6, "满圈起点和终点重合");
        int budget = ProgressArcBand.segmentCount(forwardTurn, 0.0, 1.0);
        check(budget <= 256 && budget >= 12, "一圈的分段落在 12 到 256");
        double farProgress = 0.5 + 0.5 / budget;
        ArcVec farSide = ProgressArcBand.point(forwardTurn, farProgress);
        check(start.minus(farSide).length() > 3.0, "背面探针离开重合的起终点");
        check(ProgressArcBand.intersectsSlice(forwardTurn, cube(farSide.east(), farSide.above(), farSide.south(), 0.02), 0.0, 1.0),
                "满圈背面要命中");
        ArcVec reverseFar = ProgressArcBand.point(reverseTurn, farProgress);
        check(ProgressArcBand.intersectsSlice(reverseTurn, cube(reverseFar.east(), reverseFar.above(), reverseFar.south(), 0.02), 0.0, 1.0),
                "负一圈背面要命中");

        ArcBandSnapshot reverseQuarter = flatSweep(-Math.PI / 2.0);
        ArcVec reverseTip = ProgressArcBand.point(reverseQuarter, 1.0);
        ArcVec forwardTip = ProgressArcBand.point(flatSweep(Math.PI / 2.0), 1.0);
        check(reverseTip.above() < -1.5, "负扫向转到下侧");
        check(ProgressArcBand.intersectsSlice(reverseQuarter, cube(reverseTip.east(), reverseTip.above(), reverseTip.south(), 0.02), 0.0, 1.0),
                "负扫向的末端要命中");
        check(!ProgressArcBand.intersectsSlice(reverseQuarter, cube(forwardTip.east(), forwardTip.above(), forwardTip.south(), 0.02), 0.0, 1.0),
                "负扫向不覆盖正向末端");

        ArcBandSnapshot wideTurn = ArcBandSnapshot.of(
                new ArcVec(0.0, 0.0, 0.0), new ArcVec(1.0, 0.0, 0.0), new ArcVec(0.0, 1.0, 0.0), new ArcVec(0.0, 0.0, 1.0),
                0, 2000.0, 0.1, 0.0, Math.PI * 2.0, -1L, 6, 4);
        int wideBudget = ProgressArcBand.segmentCount(wideTurn, 0.0, 1.0);
        check(wideBudget == 256, "大半径整圈停在 256 段");
        ProgressArcBand.resetWalkedChords();
        ArcBounds hole = cube(0.0, 0.0, 0.0, 0.5);
        check(!ProgressArcBand.intersectsSlice(wideTurn, hole, 0.0, 1.0), "大半径整圈的圆心落空");
        check(ProgressArcBand.walkedChords() == wideBudget * 2, "落空时粗检和窄检各走一遍分段");
        check(ProgressArcBand.walkedChords() <= 512, "没有第二层分段循环");
    }

    private static void radialBladeReachesFace() {
        ArcBounds faceBox = new ArcBounds(-0.3, 0.0, 0.1, 0.3, 1.8, 0.7);
        ArcBounds twoGrid = new ArcBounds(-0.3, 0.0, 1.7, 0.3, 1.95, 2.3);
        ArcBandSnapshot outerClose = playerBlade(0, 1.0, ArcBandMode.OUTER_ARC);
        ArcBandSnapshot outerScaled = playerBlade(0, 1.35, ArcBandMode.OUTER_ARC);
        check(outerClose.mode() == ArcBandMode.OUTER_ARC && outerScaled.mode() == ArcBandMode.OUTER_ARC, "旧工厂仍是外环");
        check(!hitsDuringActive(outerClose, faceBox), "外环贴脸仍然不中");
        check(!hitsDuringActive(outerScaled, faceBox), "放大后的外环贴脸仍然不中");
        check(hitsDuringActive(outerClose, twoGrid), "半径 2.4 的外环仍打中身前两格");
        check(!hitsDuringActive(outerScaled, twoGrid), "1.35 外环仍然够不到身前两格");

        double[] scales = new double[] {1.0, 1.35};
        for (int beatIndex = 0; beatIndex < 3; beatIndex++) {
            for (double scale : scales) {
                String label = "拍 " + beatIndex + " 倍率 " + scale;
                ArcBandSnapshot blade = playerBlade(beatIndex, scale, ArcBandMode.RADIAL_BLADE);
                check(blade != null && blade.mode() == ArcBandMode.RADIAL_BLADE, label + " 要冻成径向刃片");
                check(Math.abs(blade.radius() - ArcBandSnapshot.BASE_RADIUS * scale) < 1.0E-9, label + " 半径只乘一次");
                check(Math.abs(blade.halfWidth() - ArcBandSnapshot.BASE_HALF_WIDTH * scale) < 1.0E-9, label + " 半宽只乘一次");
                check(hitsDuringActive(blade, faceBox), label + " 贴脸要命中");
                check(hitsDuringActive(blade, twoGrid), label + " 身前两格要命中");
                check(ProgressArcBand.point(playerBlade(beatIndex, scale, ArcBandMode.OUTER_ARC), 0.37)
                        .minus(ProgressArcBand.point(blade, 0.37)).length() < 1.0E-9, label + " 外圈采样不因模式改变");

                double aheadProgress = 0.5;
                ArcVec aheadPoint = blade.origin().plus(ProgressArcBand.direction(blade, aheadProgress).scale(blade.radius() * 0.45));
                ArcBounds aheadBox = cube(aheadPoint.east(), aheadPoint.above(), aheadPoint.south(), 0.04);
                ProgressArcBand.SweepCursor firstCursor = ProgressArcBand.advance(0.0, false, 1L, blade.activeTicks());
                check(firstCursor.swept(), label + " 首拍要有扫掠");
                check(ProgressArcBand.sliceBroadBounds(blade, firstCursor.fromProgress(), firstCursor.toProgress()).overlaps(aheadBox),
                        label + " 未来角落在当前粗盒里");
                check(!ProgressArcBand.intersectsSlice(blade, aheadBox, firstCursor.fromProgress(), firstCursor.toProgress()),
                        label + " 未来角首拍不命中");
                check(hitsDuringActive(blade, aheadBox), label + " 扫过该角后要命中");

                ArcVec onBlade = blade.origin().plus(ProgressArcBand.direction(blade, aheadProgress).scale(blade.radius() * 0.45));
                ArcVec withinThickness = onBlade.plus(blade.normalAxis().scale(blade.halfWidth() * 0.35));
                ArcVec lifted = onBlade.plus(blade.normalAxis().scale(blade.halfWidth() + 0.35));
                check(ProgressArcBand.intersectsSlice(blade, cube(withinThickness.east(), withinThickness.above(), withinThickness.south(), 0.02), 0.0, 1.0),
                        label + " 半宽内要命中");
                check(!ProgressArcBand.intersectsSlice(blade, cube(lifted.east(), lifted.above(), lifted.south(), 0.02), 0.0, 1.0),
                        label + " 半宽外不命中");
                ArcVec skinPoint = blade.origin().plus(ProgressArcBand.direction(blade, aheadProgress)
                        .scale(blade.radius() + blade.halfWidth() * 0.45));
                ArcVec pastRadius = blade.origin().plus(ProgressArcBand.direction(blade, aheadProgress)
                        .scale(blade.radius() + blade.halfWidth() + 0.35));
                check(ProgressArcBand.intersectsSlice(blade, cube(skinPoint.east(), skinPoint.above(), skinPoint.south(), 0.01), 0.0, 1.0),
                        label + " 外沿皮要命中");
                check(!ProgressArcBand.intersectsSlice(blade, cube(pastRadius.east(), pastRadius.above(), pastRadius.south(), 0.02), 0.0, 1.0),
                        label + " 外半径外不命中");
            }
        }

        ArcBandSnapshot rootBlade = playerBlade(2, 1.35, ArcBandMode.RADIAL_BLADE);
        ArcBounds rootBox = cube(rootBlade.origin().east(), rootBlade.origin().above(), rootBlade.origin().south(), rootBlade.halfWidth() * 0.4);
        check(hitsDuringActive(rootBlade, rootBox), "刃根的小厚度球要命中");

        faceInteriorNotEdgeOnly();

        ArcBandSnapshot moved = ArcBandSnapshot.freezeLook(
                new ArcVec(5.0, 7.0, 11.0), new ArcVec(0.0, 0.0, 1.0), null, false,
                0.0, 2, ArcBandSnapshot.BASE_RADIUS, ArcBandSnapshot.BASE_HALF_WIDTH, 1.35, ArcBandMode.RADIAL_BLADE);
        check(moved != null, "覆写锚点要能冻结");
        ArcBounds movedFace = new ArcBounds(4.7, 5.38, 11.1, 5.3, 7.18, 11.7);
        check(hitsDuringActive(moved, movedFace), "覆写锚点上的贴脸要命中");
        check(!hitsDuringActive(moved, faceBox), "锚点移走后原来的贴脸不命中");

        ArcBandSnapshot eastBlade = ArcBandSnapshot.freezeLook(
                new ArcVec(0.0, 1.62, 0.0), new ArcVec(1.0, 0.0, 0.0), null, false,
                0.0, 2, ArcBandSnapshot.BASE_RADIUS, ArcBandSnapshot.BASE_HALF_WIDTH, 1.35, ArcBandMode.RADIAL_BLADE);
        check(eastBlade != null, "朝东的刃片要能冻结");
        ArcBounds eastBox = new ArcBounds(0.1, 0.0, -0.3, 0.7, 1.8, 0.3);
        ArcBounds rearBox = new ArcBounds(-0.9, 1.45, -0.12, -0.4, 1.8, 0.12);
        ArcBounds southBox = new ArcBounds(-0.3, 1.4, 2.0, 0.15, 1.85, 2.6);
        check(hitsDuringActive(eastBlade, eastBox), "旋转到朝东后前方要命中");
        check(!hitsDuringActive(eastBlade, rearBox), "旋转到朝东后背后不命中");
        check(!hitsDuringActive(eastBlade, southBox), "朝东时南向远处不命中");

        ArcBandSnapshot turned = ArcBandSnapshot.of(
                new ArcVec(5.0, 7.0, 11.0),
                new ArcVec(0.0, 0.0, -1.0),
                new ArcVec(0.0, 1.0, 0.0),
                new ArcVec(1.0, 0.0, 0.0),
                0, 2.4, 0.16, 0.0, Math.PI / 2.0, 0L, 6, 4, ArcBandMode.RADIAL_BLADE);
        check(turned != null, "旋转基要能构造径向刃片");
        ArcVec turnedInside = turned.origin().plus(ProgressArcBand.direction(turned, 0.5).scale(1.0));
        check(ProgressArcBand.intersectsSlice(turned, cube(turnedInside.east(), turnedInside.above(), turnedInside.south(), 0.04), 0.0, 1.0),
                "旋转平面的径向内部要命中");
        check(!ProgressArcBand.intersectsSlice(turned, faceBox, 0.0, 1.0), "旋转平面打不中无关的贴脸盒");
    }

    private static void faceInteriorNotEdgeOnly() {
        ArcBandSnapshot slice = ArcBandSnapshot.of(
                new ArcVec(0.0, 0.0, 0.0),
                new ArcVec(0.0, 0.0, 1.0),
                new ArcVec(1.0, 0.0, 0.0),
                new ArcVec(0.0, 1.0, 0.0),
                0, 2.0, 0.05, 0.0, Math.toRadians(10.0), 0L, 6, 4, ArcBandMode.RADIAL_BLADE);
        check(slice != null, "十度径向片要能构造");
        check(ProgressArcBand.segmentCount(slice, 0.0, 1.0) == 1, "十度片只成一段三角");
        ArcVec centroid = slice.origin().plus(ProgressArcBand.point(slice, 0.0)).plus(ProgressArcBand.point(slice, 1.0)).scale(1.0 / 3.0);
        check(ProgressArcBand.intersectsSlice(slice, cube(centroid.east(), centroid.above(), centroid.south(), 0.02), 0.0, 1.0),
                "三角面内部要命中");
        ArcVec liftedCentroid = centroid.plus(slice.normalAxis().scale(0.2));
        check(!ProgressArcBand.intersectsSlice(slice, cube(liftedCentroid.east(), liftedCentroid.above(), liftedCentroid.south(), 0.02), 0.0, 1.0),
                "面内部抬离平面后不命中");
    }

    private static void radialSweepStaysOnItsSlice() {
        ArcBandSnapshot blade = playerBlade(0, 1.35, ArcBandMode.RADIAL_BLADE);
        double tailProgress = 0.99;
        ArcVec tailPoint = blade.origin().plus(ProgressArcBand.direction(blade, tailProgress).scale(blade.radius() * 0.5));
        ArcBounds tailBox = cube(tailPoint.east(), tailPoint.above(), tailPoint.south(), 0.04);
        double lastProgress = 0.0;
        boolean damageClosed = false;
        for (long ageTicks = 1L; ageTicks <= 10L; ageTicks++) {
            ProgressArcBand.SweepCursor cursor = ProgressArcBand.advance(
                    lastProgress, damageClosed, ageTicks, blade.activeTicks());
            if (ageTicks < 6L) {
                check(cursor.swept(), "前五拍要扫到新弧");
                check(!cursor.damageClosed(), "前五拍不收刀");
                check(!ProgressArcBand.intersectsSlice(blade, tailBox, cursor.fromProgress(), cursor.toProgress()),
                        "尾段目标在扫到之前不命中");
            } else if (ageTicks == 6L) {
                check(cursor.swept(), "第 6 拍补上最后一段");
                check(Math.abs(cursor.fromProgress() - 5.0 / 6.0) < 1.0E-9, "第 6 拍从 5/6 接上");
                check(Math.abs(cursor.toProgress() - 1.0) < 1.0E-9, "第 6 拍扫到进度 1");
                check(cursor.damageClosed(), "第 6 拍后关闭伤害");
                check(ProgressArcBand.intersectsSlice(blade, tailBox, cursor.fromProgress(), cursor.toProgress()),
                        "第 6 拍尾段要命中");
            } else {
                check(!cursor.swept(), "第 7 到 10 拍不新增伤害");
                check(cursor.damageClosed(), "收刀后保持关闭");
            }
            lastProgress = cursor.lastProgress();
            damageClosed = cursor.damageClosed();
        }
    }

    private static void radialPackKeepsMode() {
        ArcBandSnapshot frozen = ArcBandSnapshot.freezeLook(
                new ArcVec(1.5, 2.0, -0.5), new ArcVec(0.2, 0.1, 0.9), null, false,
                12.0, 0, 2.4, 0.16, 1.35, ArcBandMode.RADIAL_BLADE);
        check(frozen != null && frozen.mode() == ArcBandMode.RADIAL_BLADE, "冻结要留下径向模式");
        ArcBandSnapshot born = frozen.withBirth(80L);
        check(born.mode() == ArcBandMode.RADIAL_BLADE, "激活时刻不能丢掉模式");
        ArcBandSnapshot restored = ArcBandSnapshot.unpack(born.pack());
        check(restored != null && restored.mode() == ArcBandMode.RADIAL_BLADE, "新同步串要还原径向模式");
        check(restored.birthTick() == 80L, "径向同步串仍带着激活时刻");
        check(restored.sweepAngle() == frozen.sweepAngle(), "径向同步后扫角不变");
        check(ArcBandSnapshot.freezeLook(
                new ArcVec(1.5, 2.0, -0.5), new ArcVec(0.2, 0.1, 0.9), null, false,
                12.0, 0, 2.4, 0.16, 1.35).mode() == ArcBandMode.OUTER_ARC, "旧冻结工厂仍是外环");

        String[] legacyTokens = frozen.pack().split(" ");
        check(legacyTokens.length == 21, "新打包带显式模式");
        String legacyPacked = String.join(" ", Arrays.copyOf(legacyTokens, 20));
        ArcBandSnapshot legacy = ArcBandSnapshot.unpack(legacyPacked);
        check(legacy != null && legacy.mode() == ArcBandMode.OUTER_ARC, "二十字段旧串按外环解");
        check(legacy.sweepAngle() == frozen.sweepAngle(), "旧串解出来扫角不变");
        String[] shortTokens = Arrays.copyOf(legacyTokens, 19);
        check(ArcBandSnapshot.unpack(String.join(" ", shortTokens)) == null, "十九字段要拒绝");
        String[] unknownTokens = frozen.pack().split(" ");
        unknownTokens[20] = "BENT";
        check(ArcBandSnapshot.unpack(String.join(" ", unknownTokens)) == null, "未知模式要拒绝");
        unknownTokens[20] = "radial_blade";
        check(ArcBandSnapshot.unpack(String.join(" ", unknownTokens)) == null, "大小写不同的模式要拒绝");
        check(ArcBandSnapshot.unpack(frozen.pack() + " EXTRA") == null, "超长同步串要拒绝");
        check(ArcBandDraft.of(0, 2.4, 0.16).mode() == ArcBandMode.OUTER_ARC, "旧草稿工厂是外环");
        check(ArcBandDraft.radialBlade(1, 2.4, 0.16).mode() == ArcBandMode.RADIAL_BLADE, "径向草稿要带模式");
        check(ArcBandDraft.of(0, 2.4, 0.16, null) == null, "空模式草稿要拒绝");
        check(ArcBandDraft.radialBlade(0, Double.NaN, 0.16) == null, "径向草稿上的非数半径要拒绝");

        ArcBandSnapshot quarterRestored = ArcBandSnapshot.unpack(quarter().pack());
        check(quarterRestored != null && quarterRestored.mode() == ArcBandMode.OUTER_ARC, "显式外环往返仍是外环");
        check(!ProgressArcBand.intersectsSlice(quarterRestored, cube(0.0, 0.0, 0.0, 0.05), 0.0, 1.0), "外环往返后圆心仍然落空");
    }

    private static void radialWorkStayBounded() {
        ArcBandSnapshot capped = ArcBandSnapshot.of(
                new ArcVec(0.0, 0.0, 0.0),
                new ArcVec(1.0, 0.0, 0.0),
                new ArcVec(0.0, 1.0, 0.0),
                new ArcVec(0.0, 0.0, 1.0),
                0, 2000.0, 0.1, 0.0, Math.PI * 2.0, -1L, 6, 4, ArcBandMode.RADIAL_BLADE);
        check(capped != null, "径向大半径整圈要能构造");
        check(ProgressArcBand.segmentCount(capped, 0.0, 1.0) == 256, "径向大半径整圈仍是 256 段");
        check(ArcBandSnapshot.of(
                new ArcVec(0.0, 0.0, 0.0), new ArcVec(1.0, 0.0, 0.0), new ArcVec(0.0, 1.0, 0.0), new ArcVec(0.0, 0.0, 1.0),
                0, 2000.0, 0.1, 0.0, Math.nextUp(Math.PI * 2.0), -1L, 6, 4, ArcBandMode.RADIAL_BLADE) == null,
                "径向也不能超过一圈");
        ArcBounds corner = cube(capped.radius(), capped.radius(), 0.0, 0.05);
        check(ProgressArcBand.sliceBroadBounds(capped, 0.0, 1.0).overlaps(corner), "整圈粗盒盖住方盒角");
        ProgressArcBand.resetWalkedChords();
        check(!ProgressArcBand.intersectsSlice(capped, corner, 0.0, 1.0), "方盒角不在径向刃片里");
        check(ProgressArcBand.walkedChords() == 512, "落空时粗检和窄检各走一遍");
        check(ProgressArcBand.walkedChords() <= 512, "径向没有第二层分段");
        check(ProgressArcBand.intersectsSlice(capped, cube(0.0, 0.0, 0.0, 0.05), 0.0, 1.0), "径向刃根要命中");
        ArcVec interiorBack = capped.origin().plus(ProgressArcBand.direction(capped, 0.5).scale(capped.radius() * 0.5));
        check(ProgressArcBand.intersectsSlice(capped, cube(interiorBack.east(), interiorBack.above(), interiorBack.south(), 0.2), 0.0, 1.0),
                "径向整圈背面内部要命中");
    }

    private static ArcBandSnapshot playerBlade(int beat, double scale, ArcBandMode bladeMode) {
        return ArcBandSnapshot.freezeLook(
                new ArcVec(0.0, 1.62, 0.0), new ArcVec(0.0, 0.0, 1.0), null, false,
                0.0, beat, ArcBandSnapshot.BASE_RADIUS, ArcBandSnapshot.BASE_HALF_WIDTH, scale, bladeMode);
    }

    private static boolean hitsDuringActive(ArcBandSnapshot snapshot, ArcBounds target) {
        double lastProgress = 0.0;
        boolean damageClosed = false;
        boolean connected = false;
        for (long ageTicks = 1L; ageTicks <= snapshot.activeTicks(); ageTicks++) {
            ProgressArcBand.SweepCursor cursor = ProgressArcBand.advance(
                    lastProgress, damageClosed, ageTicks, snapshot.activeTicks());
            if (cursor.swept() && ProgressArcBand.intersectsSlice(snapshot, target, cursor.fromProgress(), cursor.toProgress())) {
                connected = true;
            }
            lastProgress = cursor.lastProgress();
            damageClosed = cursor.damageClosed();
        }
        return connected;
    }

    private static ArcBandSnapshot flatSweep(double sweep) {
        return ArcBandSnapshot.of(
                new ArcVec(0.0, 0.0, 0.0),
                new ArcVec(1.0, 0.0, 0.0),
                new ArcVec(0.0, 1.0, 0.0),
                new ArcVec(0.0, 0.0, 1.0),
                0, 2.0, 0.1, 0.0, sweep, 0L, 6, 4);
    }

    private static void faceDistanceStays() {
        double faceGap = ProgressArcBand.distanceSegmentToBounds(
                new ArcVec(-2.0, 0.1, 0.0), new ArcVec(2.0, 0.1, 0.0),
                new ArcBounds(-1.0, -1.0, -1.0, 1.0, 0.0, 1.0));
        check(Math.abs(faceGap - 0.1) < 1.0E-9, "平行面内的最近距离是 0.1");
        double endpointGap = ProgressArcBand.distanceSegmentToBounds(
                new ArcVec(2.0, 2.0, 2.0), new ArcVec(3.0, 3.0, 3.0),
                new ArcBounds(-1.0, -1.0, -1.0, 1.0, 1.0, 1.0));
        check(Math.abs(endpointGap - Math.sqrt(3.0)) < 1.0E-6, "倾斜线段的最近处在端点");
    }

    private static ArcBandSnapshot quarterCircle(double radius, double halfWidth) {
        ArcBandSnapshot arc = ArcBandSnapshot.of(
                new ArcVec(0.0, 0.0, 0.0),
                new ArcVec(1.0, 0.0, 0.0),
                new ArcVec(0.0, 1.0, 0.0),
                new ArcVec(0.0, 0.0, 1.0),
                0, radius, halfWidth, 0.0, Math.PI / 2.0, 0L, 6, 4);
        if (arc == null) {
            throw new IllegalStateException("基准圆弧没有通过校验");
        }
        return arc;
    }

    private static ArcBounds cube(double east, double above, double south, double halfExtent) {
        return new ArcBounds(
                east - halfExtent, above - halfExtent, south - halfExtent,
                east + halfExtent, above + halfExtent, south + halfExtent);
    }

    private static ArcBandSnapshot quarter() {
        ArcBandSnapshot arc = ArcBandSnapshot.of(
                new ArcVec(0.0, 0.0, 0.0),
                new ArcVec(0.0, 0.0, 1.0),
                new ArcVec(1.0, 0.0, 0.0),
                new ArcVec(0.0, 1.0, 0.0),
                0, 2.4, 0.16, 0.0, Math.PI / 2.0, -1L, 6, 4);
        if (arc == null) {
            throw new IllegalStateException("直角弧没有通过校验");
        }
        return arc;
    }

    private static int arcLife() {
        return ArcBandSnapshot.ACTIVE_TICKS + ArcBandSnapshot.TAIL_TICKS;
    }

    private static boolean near(ArcVec actual, ArcVec expected) {
        return actual.minus(expected).length() < 1.0E-6;
    }

    private static void check(boolean passed, String label) {
        if (!passed) {
            throw new IllegalStateException(label);
        }
    }
}
