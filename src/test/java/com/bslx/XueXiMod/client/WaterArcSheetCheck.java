package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.client.WaterArcSheetMap.SheetCorner;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBandSnapshot;
import com.bslx.XueXiMod.shu.technique.release.water.ArcVec;
import com.bslx.XueXiMod.shu.technique.release.water.ProgressArcBand;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** 不启动游戏。核对水斩帧、极坐标 UV、三拍扫角，以及贴图没有被改写。 */
public final class WaterArcSheetCheck {
    public static void main(String[] arguments) throws Exception {
        ageBoundaries();
        sourceAnglesFollowHeadThenHoldPeak();
        threeBeatsKeepSnapshotSweep();
        sheetDoesNotTranslateOrScaleTwice();
        textureStaysInsideFrame();
        copiedSheetMatchesArchive();
        System.out.println("弧形水斩采样检查通过");
    }

    private static void ageBoundaries() {
        int activeTicks = ArcBandSnapshot.ACTIVE_TICKS;
        int tailTicks = ArcBandSnapshot.TAIL_TICKS;
        check(WaterArcSheetMap.frameIndex(-0.1, activeTicks, tailTicks) < 0, "负年龄不取帧");
        check(WaterArcSheetMap.frameIndex(0.0, activeTicks, tailTicks) == 0, "age 0 是帧 0");
        check(WaterArcSheetMap.frameIndex(5.9, activeTicks, tailTicks) == 5, "age 5.9 仍是帧 5");
        check(WaterArcSheetMap.frameIndex(6.0, activeTicks, tailTicks) == 6, "age 6 停在峰值帧 6");
        check(WaterArcSheetMap.frameIndex(6.2, activeTicks, tailTicks) == 6, "刚过 age 6 不跳到帧 7");
        check(WaterArcSheetMap.frameIndex(6.5, activeTicks, tailTicks) == 7, "峰值约 0.4 tick 后进入帧 7");
        check(WaterArcSheetMap.frameIndex(9.5, activeTicks, tailTicks) == 14, "消退后段是帧 14");
        check(WaterArcSheetMap.frameIndex(9.8, activeTicks, tailTicks) == 15, "末段进入透明帧 15");
        check(WaterArcSheetMap.frameIndex(9.999, activeTicks, tailTicks) == 15, "寿命前仍停在帧 15");
        check(WaterArcSheetMap.frameIndex(10.0, activeTicks, tailTicks) < 0, "age 10 不再绘制");
        check(WaterArcSheetMap.frameIndex(16.0, activeTicks, tailTicks) < 0, "超时不回到帧 0");
        int previous = 0;
        for (int stepIndex = 0; stepIndex <= 200; stepIndex++) {
            double sampleAge = stepIndex / 20.0;
            int frame = WaterArcSheetMap.frameIndex(sampleAge, activeTicks, tailTicks);
            if (sampleAge >= 10.0) {
                check(frame < 0, "满 10 tick 之后没有帧");
                continue;
            }
            check(frame >= previous && frame <= 15, "帧号只前进且不越出 0 到 15");
            previous = frame;
        }
        check(WaterArcSheetMap.fadeAlpha(6.0, activeTicks, tailTicks) == 1.0, "峰值开始时仍不淡");
        check(WaterArcSheetMap.fadeAlpha(8.0, activeTicks, tailTicks) < 1.0, "消退阶段沿用淡出");
        check(!WaterArcSheetMap.emit(beat(0, 1.0, new ArcVec(0.0, 0.0, 0.0)), 0.0, unused()), "age 0 没有弧段");
        check(!WaterArcSheetMap.emit(beat(0, 1.0, new ArcVec(0.0, 0.0, 0.0)), 10.0, unused()), "age 10 没有弧段");
        check(WaterArcSheetMap.emit(beat(0, 1.0, new ArcVec(0.0, 0.0, 0.0)), 6.0, unused()), "age 6 有弧段");
    }

    private static void sourceAnglesFollowHeadThenHoldPeak() {
        int activeTicks = ArcBandSnapshot.ACTIVE_TICKS;
        for (int frame = 0; frame < WaterArcSheetMap.HEAD_END_DEGREES.length; frame++) {
            double sampleAge = frame == 6 ? 6.0 : frame + 0.5;
            double revealed = WaterArcSheetMap.reveal(sampleAge, activeTicks);
            double tail = WaterArcSheetMap.sourceDegrees(frame, sampleAge, activeTicks, 0.0);
            double head = WaterArcSheetMap.sourceDegrees(frame, sampleAge, activeTicks, revealed);
            check(Math.abs(tail - WaterArcSheetMap.SOURCE_START_DEGREES) < 1.0E-9, "活动帧起点是 -213 度");
            check(Math.abs(head - WaterArcSheetMap.HEAD_END_DEGREES[frame]) < 1.0E-9, "浪头贴在当前扫描前沿");
        }
        double fadeAge = 7.5;
        int fadeFrame = WaterArcSheetMap.frameIndex(fadeAge, activeTicks, ArcBandSnapshot.TAIL_TICKS);
        check(fadeFrame > 6, "消退已经离开前 7 帧");
        check(Math.abs(WaterArcSheetMap.sourceDegrees(fadeFrame, fadeAge, activeTicks, 0.0) - WaterArcSheetMap.SOURCE_START_DEGREES) < 1.0E-9,
                "消退起点仍是峰值映射的 -213 度");
        check(Math.abs(WaterArcSheetMap.sourceDegrees(fadeFrame, fadeAge, activeTicks, 1.0) - WaterArcSheetMap.PEAK_END_DEGREES) < 1.0E-9,
                "消退终点停在峰值 4 度，不跟着后期浪头外扩");
    }

    private static void threeBeatsKeepSnapshotSweep() {
        for (int beatIndex = 0; beatIndex < 3; beatIndex++) {
            ArcBandSnapshot snapshot = beat(beatIndex, 1.0, new ArcVec(12.0, -4.0, 9.0));
            ArcVec start = WaterArcSheetMap.offsetAt(snapshot, 0.0, WaterArcSheetMap.RHO_MID);
            ArcVec end = WaterArcSheetMap.offsetAt(snapshot, 1.0, WaterArcSheetMap.RHO_MID);
            double startAngle = basisAngle(snapshot, start);
            double endAngle = basisAngle(snapshot, end);
            double worldSpan = endAngle - startAngle;
            check(Math.abs(startAngle - snapshot.startAngle()) < 1.0E-6, "起点方向跟快照");
            check(Math.abs(endAngle - (snapshot.startAngle() + snapshot.sweepAngle())) < 1.0E-6, "终点方向跟快照");
            check(Math.abs(worldSpan - snapshot.sweepAngle()) < 1.0E-6, "世界扫角等于快照扫角");
            check(Math.abs(Math.toDegrees(worldSpan) - 217.0) > 20.0, "没有把素材 217 度原样铺开");
            check(Math.abs(start.dot(snapshot.normalAxis())) < 1.0E-6, "起点在攻击平面内");
            check(Math.abs(end.dot(snapshot.normalAxis())) < 1.0E-6, "终点在攻击平面内");
        }
        ArcBandSnapshot positive = beat(0, 1.0, new ArcVec(0.0, 0.0, 0.0));
        ArcBandSnapshot negative = beat(1, 1.0, new ArcVec(0.0, 0.0, 0.0));
        check(positive.sweepAngle() > 0.0 && negative.sweepAngle() < 0.0, "第 0 拍正扫，第 1 拍负扫");
        check(Math.abs(Math.toDegrees(positive.sweepAngle()) - 90.0) < 1.0E-6, "第 0 拍 90 度");
        check(Math.abs(Math.toDegrees(negative.sweepAngle()) + 90.0) < 1.0E-6, "第 1 拍 -90 度");
        check(Math.abs(Math.toDegrees(beat(2, 1.0, new ArcVec(0.0, 0.0, 0.0)).sweepAngle()) - 140.0) < 1.0E-6, "第 2 拍 140 度");
    }

    private static void sheetDoesNotTranslateOrScaleTwice() {
        ArcVec origin = new ArcVec(80.0, 15.0, -40.0);
        ArcVec shifted = new ArcVec(-7.0, 3.0, 22.0);
        ArcBandSnapshot first = beat(2, 2.5, origin);
        ArcBandSnapshot second = beat(2, 2.5, shifted);
        ArcVec firstOffset = WaterArcSheetMap.offsetAt(first, 0.35, WaterArcSheetMap.RHO_MID);
        ArcVec secondOffset = WaterArcSheetMap.offsetAt(second, 0.35, WaterArcSheetMap.RHO_MID);
        check(firstOffset.minus(secondOffset).length() < 1.0E-9, "原点不同也不改顶点偏移");
        ArcVec expected = ProgressArcBand.direction(first, 0.35).scale(first.radius());
        check(firstOffset.minus(expected).length() < 1.0E-6, "中线落在最终半径上，没有再加原点");
        check(Math.abs(first.radius() - ArcBandSnapshot.BASE_RADIUS * 2.5) < 1.0E-9, "半径只含冻结时的那一次倍率");
        double lateral = (WaterArcSheetMap.RHO_HIGH - WaterArcSheetMap.RHO_MID) / WaterArcSheetMap.RHO_BODY_HALF;
        double outer = WaterArcSheetMap.worldDistance(first, WaterArcSheetMap.RHO_HIGH);
        double inner = WaterArcSheetMap.worldDistance(first, WaterArcSheetMap.RHO_LOW);
        check(Math.abs(WaterArcSheetMap.worldDistance(first, WaterArcSheetMap.RHO_MID) - first.radius()) < 1.0E-9, "极径中线就是弧带半径");
        check(Math.abs(outer - (first.radius() + first.halfWidth() * lateral)) < 1.0E-9, "外缘只再加一次半宽");
        check(outer > first.radius() + first.halfWidth(), "装饰可以略超出窄碰撞带");
        check(inner < first.radius() - first.halfWidth(), "内侧装饰同样只略超出");
        check(outer < first.radius() + first.halfWidth() * 2.2, "外溢没有变成整张方片");
    }

    private static void textureStaysInsideFrame() {
        for (int beatIndex = 0; beatIndex < 3; beatIndex++) {
            ArcBandSnapshot snapshot = beat(beatIndex, 1.0, new ArcVec(4.0, 2.0, -6.0));
            for (double sampleAge : new double[] {0.5, 6.0, 6.2, 8.0, 9.8}) {
                int frame = WaterArcSheetMap.frameIndex(sampleAge, snapshot.activeTicks(), snapshot.tailTicks());
                boolean emitted = WaterArcSheetMap.emit(snapshot, sampleAge, (startInner, startOuter, endOuter, endInner) -> {
                    inside(startInner, frame);
                    inside(startOuter, frame);
                    inside(endOuter, frame);
                    inside(endInner, frame);
                });
                check(emitted, "应画出的年龄有四边形");
            }
        }
    }

    private static void copiedSheetMatchesArchive() throws Exception {
        Path archive = Path.of("docs", "资源库", "特效", "水流", "弧形水斩_v1", "water_arc_slash.png");
        Path adopted = Path.of("src", "main", "resources", "assets", "xueximod", "textures", "entity", "xueximod", "water_arc_slash.png");
        check(Arrays.equals(Files.readAllBytes(archive), Files.readAllBytes(adopted)), "游戏贴图与归档字节一致");
        check(Files.notExists(adopted.resolveSibling("water_arc_slash.png.mcmeta")), "游戏侧不带会循环的 mcmeta");
        BufferedImage image = ImageIO.read(adopted.toFile());
        check(image.getWidth() == 64 && image.getHeight() == 1024, "竖条是 64×1024");
        check(opaqueCount(image, 6 * 64, 7 * 64) > 0, "峰值帧有不透明像素");
        check(opaqueCount(image, 15 * 64, 16 * 64) == 0, "帧 15 全透明");
    }

    private static void inside(SheetCorner corner, int frame) {
        double lower = frame / (double) WaterArcSheetMap.FRAME_COUNT;
        double upper = (frame + 1) / (double) WaterArcSheetMap.FRAME_COUNT;
        check(corner.textureU() > 0.0 && corner.textureU() < 1.0, "横向 UV 留在本帧宽度内");
        check(corner.textureV() > lower && corner.textureV() < upper, "纵向 UV 不串到邻帧");
    }

    private static double basisAngle(ArcBandSnapshot snapshot, ArcVec offset) {
        return Math.atan2(offset.dot(snapshot.crossAxis()), offset.dot(snapshot.forwardAxis()));
    }

    private static int opaqueCount(BufferedImage image, int rowStart, int rowEnd) {
        int opaquePixels = 0;
        for (int row = rowStart; row < rowEnd; row++) {
            for (int column = 0; column < image.getWidth(); column++) {
                int pixel = image.getRGB(column, row);
                if ((pixel >>> 24) != 0) {
                    opaquePixels++;
                }
            }
        }
        return opaquePixels;
    }

    private static WaterArcSheetMap.SheetVisitor unused() {
        return (startInner, startOuter, endOuter, endInner) -> {
        };
    }

    private static ArcBandSnapshot beat(int beatIndex, double scale, ArcVec origin) {
        ArcBandSnapshot snapshot = ArcBandSnapshot.freezeLook(
                origin, new ArcVec(0.0, 0.0, 1.0), null, false,
                0.0, beatIndex, ArcBandSnapshot.BASE_RADIUS, ArcBandSnapshot.BASE_HALF_WIDTH, scale);
        if (snapshot == null) {
            throw new IllegalStateException("水刃弧没有冻出来");
        }
        return snapshot;
    }

    private static void check(boolean passed, String label) {
        if (!passed) {
            throw new IllegalStateException(label);
        }
    }
}
