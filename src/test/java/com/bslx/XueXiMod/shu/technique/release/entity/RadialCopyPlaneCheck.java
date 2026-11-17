package com.bslx.XueXiMod.shu.technique.release.entity;

import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBandDraft;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBandMode;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBandSnapshot;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBounds;
import com.bslx.XueXiMod.shu.technique.release.water.ArcVec;
import com.bslx.XueXiMod.shu.technique.release.water.ProgressArcBand;
import com.bslx.XueXiMod.shu.technique.release.water.RadialCopyPlane;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;

/** 直接调用径向复制斩面、冻结和生产碰撞。不启动游戏。 */
public final class RadialCopyPlaneCheck {
    private static final double[] HOST_ROLLS = {40.0, -40.0, 0.0};
    private static final int[][] TRIPLE_TEMPLATES = {
            {0, 1, 2},
            {1, 2, 0},
            {2, 0, 1}
    };

    public static void main(String[] arguments) {
        tripleUsesThreeTemplates();
        hostExtraStaysOnCopyZero();
        nineRollsStayUnique();
        fourRollsReplaceNearest();
        pairUsesNextTemplate();
        singleAndOuterStay();
        dispersedPathsDoNotStack();
        movedOriginStillSpreads();
        cohortKeepsSpawnLanes();
        planesHitTheirOwnSweep();
        System.out.println("缺月三向斩面检查通过");
        System.out.println("断言 " + assertionCount);
    }

    private static void tripleUsesThreeTemplates() {
        for (int beat = 0; beat < 3; beat++) {
            CastFrame shared = openFrame(0, 3);
            ArcBandSnapshot[] planes = new ArcBandSnapshot[3];
            for (int copyOrdinal = 0; copyOrdinal < 3; copyOrdinal++) {
                shared.copyIndex = copyOrdinal;
                check(!shared.cohortDispersed, "布局不能改写共享帧");
                ArcBandSnapshot baked = bakeRadial(beat, shared);
                planes[copyOrdinal] = baked;
                ArcBandSnapshot hostPlane = reference(beat, 0.0, shared);
                check(baked.beat() == beat, "三份仍写宿主拍");
                check(baked.activeTicks() == 6 && baked.tailTicks() == 4 && baked.birthTick() == -1L, "三份同一活动窗");
                check(baked.mode() == ArcBandMode.RADIAL_BLADE, "仍是径向刃");
                check(sameScale(baked, shared) && baked.origin().minus(eye()).length() < 1.0E-8, "原点和倍率不变");
                check(sameAxes(baked, hostPlane) == (copyOrdinal == 0), "只有 0 号份保持宿主斩面");
                int template = TRIPLE_TEMPLATES[beat][copyOrdinal];
                ArcBandSnapshot templatePlane = reference(template, 0.0, shared);
                check(sameAxes(baked, templatePlane), "斩面滚转对上模板");
                check(sameSector(baked, templatePlane), "扫向对上模板");
                if (copyOrdinal == 0) {
                    check(sameSnapshot(baked, hostPlane), "0 号份与旧单份冻结一致");
                } else {
                    double desired = HOST_ROLLS[template];
                    double adjusted = desired - HOST_ROLLS[beat];
                    check(sameAxes(baked, reference(beat, adjusted, shared)), "附加滚转只补足到模板总角");
                    if (HOST_ROLLS[beat] != 0.0) {
                        check(!sameAxes(baked, reference(beat, desired, shared)), "不能把总滚转再叠加到宿主拍上");
                    }
                    RadialCopyPlane.Placement placement = RadialCopyPlane.place(copyOrdinal, 3, beat, 0.0);
                    check(!placement.hostSector(), "其余份不用宿主扇区");
                    check(Math.abs(placement.totalRollDegrees() - desired) < 1.0E-9, "模板总滚转");
                    check(Math.abs(placement.extraRollDegrees(beat) - adjusted) < 1.0E-9, "附加角先减宿主拍");
                }
                ProgressArcBand.SweepCursor opened = ProgressArcBand.advance(0.0, false, 1L, baked.activeTicks());
                check(opened.swept() && Math.abs(opened.toProgress() - 1.0 / 6.0) < 1.0E-9, "三份同时从第一拍开扫");
            }
            check(!shared.cohortDispersed, "后一份仍能读到未散开的原状态");
            check(planes[0].forwardAxis().minus(planes[1].forwardAxis()).length() < 1.0E-8, "三份共用瞄准");
            check(Math.abs(planes[0].normalAxis().dot(planes[1].normalAxis())) < 0.97, "左右法线分开");
            check(Math.abs(planes[0].normalAxis().dot(planes[2].normalAxis())) < 0.97, "水平法线分开");
            check(Math.abs(planes[1].normalAxis().dot(planes[2].normalAxis())) < 0.97, "左右与水平分开");
            ArcBandSnapshot born = planes[1].withBirth(80L);
            check(born.birthTick() == 80L && sameAxes(born, planes[1]) && sameSector(born, planes[1]), "出生时刻保留最终轴和扫角");
            ArcBandSnapshot restored = ArcBandSnapshot.unpack(born.pack());
            check(restored != null && restored.birthTick() == 80L && restored.beat() == beat, "同步保留拍号和出生时刻");
            check(restored.forwardAxis().minus(planes[1].forwardAxis()).length() < 1.0E-6, "同步保留前向");
            check(restored.crossAxis().minus(planes[1].crossAxis()).length() < 1.0E-6, "同步保留侧向");
            check(restored.normalAxis().minus(planes[1].normalAxis()).length() < 1.0E-6, "同步保留法线");
            check(Math.abs(restored.startAngle() - planes[1].startAngle()) < 1.0E-8, "同步保留起角");
            check(restored.sweepAngle() == planes[1].sweepAngle() && restored.mode() == ArcBandMode.RADIAL_BLADE, "同步保留扫角和径向模式");
        }
    }

    private static void hostExtraStaysOnCopyZero() {
        CastFrame frame = openFrame(0, 3);
        frame.roll = 0.4F;
        ArcBandSnapshot copyZero = bakeRadial(2, frame);
        check(sameSnapshot(copyZero, reference(2, frame.roll, frame)), "0 号份保留宿主附加滚转和原扇区");
        check(!sameSector(copyZero, reference(0, 0.0, frame)), "宿主滚转略大于零时也不改成左斜扇区");
        frame.copyIndex = 1;
        ArcBandSnapshot shifted = bakeRadial(2, frame);
        check(sameAxes(shifted, reference(0, 0.0, frame)), "下一份不吃宿主附加滚转");
        check(sameSector(shifted, reference(0, 0.0, frame)), "下一份用自己的左斜扫向");
        check(!frame.cohortDispersed, "附加滚转检查也不写帧");

        CastFrame wide = openFrame(0, 9);
        wide.roll = 0.4F;
        ArcBandSnapshot wideZero = bakeRadial(2, wide);
        check(sameSnapshot(wideZero, reference(2, wide.roll, wide)), "九份里的 0 号仍是宿主原扇区");
        wide.copyIndex = 1;
        ArcBandSnapshot wideNext = bakeRadial(2, wide);
        check(sameAxes(wideNext, reference(2, -80.0, wide)), "靠近零的槽换成宿主角后，其余份从低到高");
        check(sameSector(wideNext, reference(1, 0.0, wide)), "负滚转用右斜扫向");
        check(!sameAxes(wideNext, reference(2, -79.6, wide)), "宿主附加角不加到其余份");
    }

    private static void nineRollsStayUnique() {
        double[][] expected = {
                {40.0, -80.0, -60.0, -40.0, -20.0, 0.0, 20.0, 60.0, 80.0},
                {-40.0, -80.0, -60.0, -20.0, 0.0, 20.0, 40.0, 60.0, 80.0},
                {0.0, -80.0, -60.0, -40.0, -20.0, 20.0, 40.0, 60.0, 80.0}
        };
        for (int beat = 0; beat < 3; beat++) {
            HashSet<Long> rolls = new HashSet<>();
            ArcBandSnapshot[] planes = new ArcBandSnapshot[9];
            double lowest = Double.POSITIVE_INFINITY;
            double highest = Double.NEGATIVE_INFINITY;
            for (int copyOrdinal = 0; copyOrdinal < 9; copyOrdinal++) {
                double desired = expected[beat][copyOrdinal];
                lowest = Math.min(lowest, desired);
                highest = Math.max(highest, desired);
                check(rolls.add(Math.round(desired * 1000.0)), "九份滚转互不相同");
                CastFrame frame = openFrame(copyOrdinal, 9);
                ArcBandSnapshot baked = bakeRadial(beat, frame);
                planes[copyOrdinal] = baked;
                check(baked.beat() == beat, "九份不推进拍号");
                check(sameScale(baked, frame), "九份倍率不变");
                if (copyOrdinal == 0) {
                    check(sameSnapshot(baked, reference(beat, 0.0, frame)), "九份的 0 号份保持宿主");
                } else {
                    check(sameAxes(baked, reference(2, desired, frame)), "九份总滚转不再按三取模重叠");
                    int template = desired > 0.0 ? 0 : desired < 0.0 ? 1 : 2;
                    check(sameSector(baked, reference(template, 0.0, frame)), "九份扫向按滚转符号选择");
                }
                check(Math.abs(planes[0].normalAxis().dot(baked.normalAxis())) < 0.999 || copyOrdinal == 0, "与 0 号份不同面");
            }
            check(rolls.size() == 9, "九个滚转都留下了");
            check(highest - lowest <= 160.0 + 1.0E-9 && lowest >= -80.0 && highest <= 80.0, "滚转落在一百六十度内");
            check(!sameAxes(planes[0], planes[3]) && !sameAxes(planes[3], planes[6]), "不按三份一组重复斩面");
            for (int leftCopy = 0; leftCopy < 9; leftCopy++) {
                for (int rightCopy = leftCopy + 1; rightCopy < 9; rightCopy++) {
                    check(Math.abs(planes[leftCopy].normalAxis().dot(planes[rightCopy].normalAxis())) < 0.97, "九份法线不共面");
                }
            }
        }
    }

    private static void fourRollsReplaceNearest() {
        double lowerSpaced = -80.0 + 160.0 / 3.0;
        double upperSpaced = -80.0 + 320.0 / 3.0;
        double[] beatZero = {40.0, -80.0, lowerSpaced, 80.0};
        assertWide(0, beatZero, upperSpaced);
        double[] beatFlat = {0.0, -80.0, upperSpaced, 80.0};
        assertWide(2, beatFlat, lowerSpaced);
    }

    private static void assertWide(int beat, double[] expected, double absent) {
        for (int copyOrdinal = 0; copyOrdinal < expected.length; copyOrdinal++) {
            CastFrame frame = openFrame(copyOrdinal, expected.length);
            ArcBandSnapshot baked = bakeRadial(beat, frame);
            check(sameAxes(baked, reference(2, expected[copyOrdinal], frame)), "等距槽替换后的总滚转");
            if (copyOrdinal == 0) {
                check(sameSector(baked, reference(beat, 0.0, frame)), "被替换的槽保留宿主扇区");
            }
            check(!sameAxes(baked, reference(2, absent, frame)), "被替换掉的生成角不再出现");
        }
    }

    private static void pairUsesNextTemplate() {
        int[][] nextTemplate = {{0, 1}, {1, 2}, {2, 0}};
        for (int beat = 0; beat < 3; beat++) {
            for (int copyOrdinal = 0; copyOrdinal < 2; copyOrdinal++) {
                CastFrame frame = openFrame(copyOrdinal, 2);
                ArcBandSnapshot baked = bakeRadial(beat, frame);
                int template = nextTemplate[beat][copyOrdinal];
                check(sameAxes(baked, reference(template, 0.0, frame)), "两份使用下一套斩面");
                check(sameSector(baked, reference(template, 0.0, frame)), "两份使用对应扫向");
                check(baked.beat() == beat, "两份不改拍号");
                if (copyOrdinal == 0) {
                    check(sameSnapshot(baked, reference(beat, 0.0, frame)), "两份时 0 号份仍是宿主");
                }
            }
        }
    }

    private static void singleAndOuterStay() {
        CastFrame single = openFrame(0, 1);
        for (int beat = 0; beat < 3; beat++) {
            check(!TechniqueHitSpec.radialPlaneSpread(radialDraft(beat), single), "单份不进入多斩面");
            check(sameSnapshot(bakeRadial(beat, single), reference(beat, 0.0, single)), "单份冻结与原来一致");
        }
        CastFrame triple = openFrame(1, 3);
        TechniqueHitSpec spec = new TechniqueHitSpec();
        spec.arcDraft = ArcBandDraft.of(0, ArcBandSnapshot.BASE_RADIUS, ArcBandSnapshot.BASE_HALF_WIDTH);
        check(!TechniqueHitSpec.radialPlaneSpread(spec.arcDraft, triple), "外环不进入径向多斩面");
        ArcBandSnapshot outer = TechniqueHitSpec.bakeArc(spec, triple, eyePosition(), aimPosition());
        ArcBandSnapshot outerHost = ArcBandSnapshot.freezeLook(
                eye(), aim(), null, false, 0.0, 0,
                ArcBandSnapshot.BASE_RADIUS, ArcBandSnapshot.BASE_HALF_WIDTH,
                triple.volumeScale(), ArcBandMode.OUTER_ARC);
        check(sameSnapshot(outer, outerHost), "外环三份仍走原来的宿主拍");
    }

    private static void dispersedPathsDoNotStack() {
        CastFrame scattered = openFrame(1, 3);
        scattered.overrideGeometry = true;
        scattered.cohortDispersed = true;
        scattered.pose3d = true;
        scattered.roll = 22.0F;
        scattered.origin = new Vec3(3.0, 1.5, -2.0);
        scattered.planeNormal = new Vec3(0.15, 0.35, 0.9).normalize();
        check(TechniqueHitSpec.radialCopiesAlreadySpread(scattered), "交错刀面算已经散开");
        check(!TechniqueHitSpec.radialPlaneSpread(radialDraft(1), scattered), "交错不再加三套斩面");
        ArcBandSnapshot baked = bakeRadial(1, scattered);
        ArcBandSnapshot expected = ArcBandSnapshot.freezeLook(
                new ArcVec(scattered.origin.x, scattered.origin.y, scattered.origin.z),
                new ArcVec(scattered.planeNormal.x, scattered.planeNormal.y, scattered.planeNormal.z),
                null, false, scattered.roll, 1,
                ArcBandSnapshot.BASE_RADIUS, ArcBandSnapshot.BASE_HALF_WIDTH,
                scattered.volumeScale(), ArcBandMode.RADIAL_BLADE);
        check(sameSnapshot(baked, expected), "交错只保留自己的一次滚转");
        check(!sameAxes(baked, reference(0, 0.0, scattered)), "交错没有被换成另一套模板");

        CastFrame landed = openFrame(2, 3);
        landed.overrideGeometry = true;
        landed.cohortDispersed = true;
        landed.origin = new Vec3(2.0, 1.62, 0.4);
        check(!TechniqueHitSpec.radialPlaneSpread(radialDraft(0), landed), "落点散开不加斩面");
        ArcBandSnapshot landedArc = bakeRadial(0, landed);
        ArcBandSnapshot landedHost = ArcBandSnapshot.freezeLook(
                new ArcVec(2.0, 1.62, 0.4), aim(), null, false, 0.0, 0,
                ArcBandSnapshot.BASE_RADIUS, ArcBandSnapshot.BASE_HALF_WIDTH,
                landed.volumeScale(), ArcBandMode.RADIAL_BLADE);
        check(sameSnapshot(landedArc, landedHost), "落点三份保持原来的同一斩面");

        CastFrame volume = openFrame(1, 3);
        volume.trajectoryMutable = true;
        volume.volumeHalf = 1.5;
        volume.anchor = new Vec3(0.0, 1.0, 0.0);
        check(TechniqueHitSpec.spawnDispersed(volume), "体积落点仍按生成号散开");
        check(!TechniqueHitSpec.radialPlaneSpread(radialDraft(0), volume), "体积落点不叠加斩面");
        check(sameSnapshot(bakeRadial(0, volume), reference(0, 0.0, volume)), "体积落点保持宿主斩面");
    }

    private static void movedOriginStillSpreads() {
        CastFrame frame = openFrame(0, 3);
        frame.overrideGeometry = true;
        frame.origin = new Vec3(2.0, 1.62, 0.4);
        check(!TechniqueHitSpec.radialCopiesAlreadySpread(frame), "只搬锚点不算已经散开");
        check(TechniqueHitSpec.radialPlaneSpread(radialDraft(0), frame), "锚点相同的三份仍换斩面");
        ArcBandSnapshot copyZero = bakeRadial(0, frame);
        frame.copyIndex = 1;
        ArcBandSnapshot copyNext = bakeRadial(0, frame);
        ArcVec moved = new ArcVec(2.0, 1.62, 0.4);
        check(copyZero.origin().minus(moved).length() < 1.0E-8, "0 号份用覆写原点");
        check(copyNext.origin().minus(moved).length() < 1.0E-8, "其余份也用同一个覆写原点");
        check(sameAxes(copyZero, reference(0, 0.0, frame)), "覆写原点不改变 0 号份斩面");
        check(sameAxes(copyNext, reference(1, 0.0, frame)), "覆写原点上的下一份仍是右斜");
        check(!sameAxes(copyZero, copyNext), "同一原点上的斩面仍然分开");
        check(!frame.cohortDispersed, "换斩面不回写散开标志");
    }

    private static void cohortKeepsSpawnLanes() {
        ArcBandDraft draft = radialDraft(0);
        CastFrame frame = openFrame(0, 3);
        boolean spread = TechniqueHitSpec.radialPlaneSpread(draft, frame);
        check(spread, "未散开的径向三份要记复制轴散开");
        for (int copyOrdinal = 0; copyOrdinal < 3; copyOrdinal++) {
            frame.copyIndex = copyOrdinal;
            CohortVisual alone = TechniqueHitSpec.cohortFor(frame, 0, 1, true, spread);
            check(alone.displayIndex() == copyOrdinal && alone.displayCount() == 3, "原始序号仍按复制号");
            check(!alone.laneSplit() && alone.forDrawing().equals(CohortVisual.SINGLE), "换面后不再径向切槽");
            for (int spawnOrdinal = 0; spawnOrdinal < 2; spawnOrdinal++) {
                CohortVisual visual = TechniqueHitSpec.cohortFor(frame, spawnOrdinal, 2, true, spread);
                int shown = copyOrdinal * 2 + spawnOrdinal;
                check(visual.displayIndex() == shown && visual.displayCount() == 6, "倍率后的原始序号连续");
                check(visual.laneIndex() == spawnOrdinal && visual.laneCount() == 2 && visual.laneSplit(), "同一面内的两份仍分槽");
                check(CohortVisual.settleEncoding(copyOrdinal, spawnOrdinal) == copyOrdinal + spawnOrdinal * 1024, "结算编码不变");
            }
        }
        check(!frame.cohortDispersed, "视觉记录不改共享帧");
        TechniqueHitSpec spec = new TechniqueHitSpec();
        spec.settleCopyIndex = 7;
        TechniqueHitSpec.assignCohort(spec, frame, 1, 2, true, spread);
        check(spec.settleCopyIndex == 7, "分道不改结算号");
        check(spec.cohortVisual.laneCount() == 2 && spec.cohortVisual.displayCount() == 6, "规格记下本面的两槽");

        CastFrame scattered = openFrame(1, 3);
        scattered.cohortDispersed = true;
        check(!TechniqueHitSpec.radialPlaneSpread(draft, scattered), "已散开的帧不再标记新斩面");
        CohortVisual scatteredVisual = TechniqueHitSpec.cohortFor(scattered, 0, 1, true, false);
        check(!scatteredVisual.laneSplit() && scatteredVisual.displayIndex() == 1, "原交错仍不二次分槽");
    }

    private static void planesHitTheirOwnSweep() {
        for (int beat = 0; beat < 3; beat++) {
            ArcBandSnapshot[] planes = new ArcBandSnapshot[3];
            for (int copyOrdinal = 0; copyOrdinal < 3; copyOrdinal++) {
                planes[copyOrdinal] = bakeRadial(beat, openFrame(copyOrdinal, 3));
            }
            for (int copyOrdinal = 0; copyOrdinal < 3; copyOrdinal++) {
                ArcBandSnapshot plane = planes[copyOrdinal];
                ArcBounds face = cube(plane.origin().east(), plane.origin().above(), plane.origin().south(), plane.halfWidth() * 0.4);
                check(hitsDuringActive(plane, face), "贴脸沿径向刃根命中");
                ArcVec nearPoint = plane.origin().plus(ProgressArcBand.direction(plane, 0.04).scale(plane.radius() * 0.22));
                ArcBounds nearBox = cube(nearPoint.east(), nearPoint.above(), nearPoint.south(), 0.05);
                check(hitsDuring(plane, nearBox, 1L), "开扫第一拍打到近处");
                ArcVec latePoint = plane.origin().plus(ProgressArcBand.direction(plane, 0.97).scale(plane.radius() * 0.62));
                ArcBounds lateBox = cube(latePoint.east(), latePoint.above(), latePoint.south(), 0.04);
                check(!hitsDuring(plane, lateBox, 1L), "尚未扫到的角首拍不命中");
                check(hitsDuring(plane, lateBox, 6L), "第 6 拍补上末段");
                ArcVec farPoint = plane.origin().plus(ProgressArcBand.direction(plane, 0.08).scale(plane.radius() * 0.78));
                ArcBounds farBox = cube(farPoint.east(), farPoint.above(), farPoint.south(), 0.05);
                check(ProgressArcBand.intersectsSlice(plane, farBox, 0.0, 1.0), "目标落在自己的斩面上");
                for (int other = 0; other < 3; other++) {
                    if (other == copyOrdinal) {
                        continue;
                    }
                    check(!ProgressArcBand.intersectsSlice(planes[other], farBox, 0.0, 1.0), "目标不落在另一张斩面");
                }
                double lastProgress = 0.0;
                boolean damageClosed = false;
                for (long ageTicks = 1L; ageTicks <= 10L; ageTicks++) {
                    ProgressArcBand.SweepCursor cursor = ProgressArcBand.advance(
                            lastProgress, damageClosed, ageTicks, plane.activeTicks());
                    if (ageTicks <= 5L) {
                        check(cursor.swept() && !cursor.damageClosed(), "前五拍继续扫掠");
                    } else if (ageTicks == 6L) {
                        check(cursor.swept() && cursor.damageClosed(), "第 6 拍收束活动窗");
                        check(ProgressArcBand.intersectsSlice(plane, lateBox, cursor.fromProgress(), cursor.toProgress()), "末段在第 6 拍的切片里");
                    } else {
                        check(!cursor.swept() && cursor.damageClosed(), "拖尾四拍不再造成伤害");
                    }
                    lastProgress = cursor.lastProgress();
                    damageClosed = cursor.damageClosed();
                }
            }
        }
    }

    private static ArcBandSnapshot bakeRadial(int beat, CastFrame frame) {
        TechniqueHitSpec spec = new TechniqueHitSpec();
        spec.arcDraft = radialDraft(beat);
        return TechniqueHitSpec.bakeArc(spec, frame, eyePosition(), aimPosition());
    }

    private static ArcBandDraft radialDraft(int beat) {
        return ArcBandDraft.radialBlade(beat, ArcBandSnapshot.BASE_RADIUS, ArcBandSnapshot.BASE_HALF_WIDTH);
    }

    private static CastFrame openFrame(int copyIndex, int copyCount) {
        CastFrame frame = new CastFrame();
        frame.copyIndex = copyIndex;
        frame.copyCount = copyCount;
        frame.look = aimPosition();
        frame.energyScale = 1.35F;
        return frame;
    }

    private static ArcBandSnapshot reference(int beat, double extraRoll, CastFrame frame) {
        return ArcBandSnapshot.freezeLook(
                eye(), aim(), null, false, extraRoll, beat,
                ArcBandSnapshot.BASE_RADIUS, ArcBandSnapshot.BASE_HALF_WIDTH,
                frame.volumeScale(), ArcBandMode.RADIAL_BLADE);
    }

    private static boolean sameScale(ArcBandSnapshot snapshot, CastFrame frame) {
        double scale = frame.volumeScale();
        return Math.abs(snapshot.radius() - ArcBandSnapshot.BASE_RADIUS * scale) < 1.0E-9
                && Math.abs(snapshot.halfWidth() - ArcBandSnapshot.BASE_HALF_WIDTH * scale) < 1.0E-9;
    }

    private static boolean sameAxes(ArcBandSnapshot left, ArcBandSnapshot right) {
        return left.forwardAxis().minus(right.forwardAxis()).length() < 1.0E-8
                && left.crossAxis().minus(right.crossAxis()).length() < 1.0E-8
                && left.normalAxis().minus(right.normalAxis()).length() < 1.0E-8;
    }

    private static boolean sameSector(ArcBandSnapshot left, ArcBandSnapshot right) {
        return Math.abs(left.startAngle() - right.startAngle()) < 1.0E-12
                && Math.abs(left.sweepAngle() - right.sweepAngle()) < 1.0E-12;
    }

    private static boolean sameSnapshot(ArcBandSnapshot left, ArcBandSnapshot right) {
        return left != null && right != null
                && left.origin().minus(right.origin()).length() < 1.0E-8
                && sameAxes(left, right)
                && sameSector(left, right)
                && left.beat() == right.beat()
                && Math.abs(left.radius() - right.radius()) < 1.0E-9
                && Math.abs(left.halfWidth() - right.halfWidth()) < 1.0E-9
                && left.activeTicks() == right.activeTicks()
                && left.tailTicks() == right.tailTicks()
                && left.birthTick() == right.birthTick()
                && left.mode() == right.mode();
    }

    private static boolean hitsDuringActive(ArcBandSnapshot snapshot, ArcBounds target) {
        boolean connected = false;
        for (long ageTicks = 1L; ageTicks <= snapshot.activeTicks(); ageTicks++) {
            if (hitsDuring(snapshot, target, ageTicks)) {
                connected = true;
            }
        }
        return connected;
    }

    private static boolean hitsDuring(ArcBandSnapshot snapshot, ArcBounds target, long ageTicks) {
        double lastProgress = 0.0;
        boolean damageClosed = false;
        for (long walked = 1L; walked <= ageTicks; walked++) {
            ProgressArcBand.SweepCursor cursor = ProgressArcBand.advance(
                    lastProgress, damageClosed, walked, snapshot.activeTicks());
            if (walked == ageTicks) {
                return cursor.swept() && ProgressArcBand.intersectsSlice(
                        snapshot, target, cursor.fromProgress(), cursor.toProgress());
            }
            lastProgress = cursor.lastProgress();
            damageClosed = cursor.damageClosed();
        }
        return false;
    }

    private static ArcBounds cube(double east, double above, double south, double halfExtent) {
        return new ArcBounds(
                east - halfExtent, above - halfExtent, south - halfExtent,
                east + halfExtent, above + halfExtent, south + halfExtent);
    }

    private static ArcVec eye() {
        return new ArcVec(0.0, 1.62, 0.0);
    }

    private static ArcVec aim() {
        return new ArcVec(0.0, 0.0, 1.0);
    }

    private static Vec3 eyePosition() {
        return new Vec3(0.0, 1.62, 0.0);
    }

    private static Vec3 aimPosition() {
        return new Vec3(0.0, 0.0, 1.0);
    }

    private static int assertionCount;

    private static void check(boolean passed, String label) {
        assertionCount++;
        if (!passed) {
            throw new IllegalStateException(label);
        }
    }
}
