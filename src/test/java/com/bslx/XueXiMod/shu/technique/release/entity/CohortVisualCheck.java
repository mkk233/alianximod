package com.bslx.XueXiMod.shu.technique.release.entity;

import com.bslx.XueXiMod.client.WaterArcSheetMap;
import com.bslx.XueXiMod.client.WaterArcSheetMap.SheetCorner;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinitions;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.ReleaseTag;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.WaterReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.entity.CohortLaneLayout.FaceWindow;
import com.bslx.XueXiMod.shu.technique.release.entity.CohortLaneLayout.LaneBand;
import com.bslx.XueXiMod.shu.technique.release.entity.CohortLaneLayout.LateralAxis;
import com.bslx.XueXiMod.shu.technique.release.entity.CohortLaneLayout.RibbonStrip;
import com.bslx.XueXiMod.shu.technique.release.entity.CohortLaneLayout.SeparableAxis;
import com.bslx.XueXiMod.shu.technique.release.entity.CohortLaneLayout.SlashInterval;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBandMode;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBandSnapshot;
import com.bslx.XueXiMod.shu.technique.release.water.ArcVec;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;

/** 直接调用生产显示序号和分槽。不启动 Minecraft，也不改命中体积。 */
public final class CohortVisualCheck {
    public static void main(String[] arguments) {
        metadataStaysUnique();
        reusedSpecDoesNotLeak();
        sharedAnchorStillSplits();
        dispersedGeometryDoesNotSplitAgain();
        subgroupLanes();
        declarationsStayNarrow();
        sheetLanes();
        partingAndRibbon();
        radialAndOuterArcs();
        System.out.println("剑术分道视觉检查通过");
    }

    private static void metadataStaysUnique() {
        CohortVisual single = CohortVisual.capture(0, 1, 0, 1, true, false, false);
        check(single.displayIndex() == 0 && single.displayCount() == 1 && !single.laneSplit(), "单份保持 0/1 且不分道");
        check(single.stored().equals(CohortVisual.SINGLE), "单份同步后仍是单份");
        check(single.forDrawing().equals(CohortVisual.SINGLE), "单份绘制走原面");

        HashSet<Integer> tripleIndexes = new HashSet<>();
        for (int copyOrdinal = 0; copyOrdinal < 3; copyOrdinal++) {
            CohortVisual visual = CohortVisual.capture(copyOrdinal, 3, 0, 1, true, false, false);
            check(visual.displayCount() == 3 && visual.laneSplit(), "三份剑术要分道");
            check(visual.laneCount() == 3 && visual.laneIndex() == copyOrdinal, "普通三份占满三槽");
            check(visual.displayIndex() == copyOrdinal, "三份序号就是复制号");
            check(tripleIndexes.add(visual.displayIndex()), "三份序号不重复");
            check(CohortVisual.settleEncoding(copyOrdinal, 0) == copyOrdinal, "倍率 1 时结算号仍是复制号");
        }

        HashSet<Integer> nineIndexes = new HashSet<>();
        for (int copyOrdinal = 0; copyOrdinal < 9; copyOrdinal++) {
            CohortVisual visual = CohortVisual.capture(copyOrdinal, 9, 0, 1, true, false, false);
            check(visual.displayCount() == 9 && visual.displayIndex() == copyOrdinal, "九份不压回三槽");
            check(nineIndexes.add(visual.displayIndex()), "九份序号唯一");
        }
        check(CohortVisual.capture(3, 9, 0, 1, true, false, false).displayIndex() != 0, "第九套的第四份不是第一槽");

        CastFrame frame = new CastFrame();
        frame.copyCount = 3;
        HashSet<Integer> multiplied = new HashSet<>();
        HashSet<Integer> settlements = new HashSet<>();
        for (int copyOrdinal = 0; copyOrdinal < 3; copyOrdinal++) {
            frame.copyIndex = copyOrdinal;
            for (int spawnOrdinal = 0; spawnOrdinal < 2; spawnOrdinal++) {
                CohortVisual visual = TechniqueHitSpec.cohortFor(frame, spawnOrdinal, 2, true);
                int expected = copyOrdinal * 2 + spawnOrdinal;
                check(visual.displayCount() == 6 && visual.displayIndex() == expected && visual.laneSplit(), "实体倍率 2 的显示序号连续");
                check(visual.laneCount() == 6 && visual.laneIndex() == expected, "普通三乘二占满六槽");
                check(multiplied.add(expected), "倍率组合不撞号");
                int settlement = CohortVisual.settleEncoding(copyOrdinal, spawnOrdinal);
                check(settlements.add(settlement), "结算编码也不撞号");
                if (spawnOrdinal > 0) {
                    check(settlement != expected, "显示序号不借用结算编码");
                }
            }
        }
        check(multiplied.size() == 6 && settlements.size() == 6, "三份乘倍率 2 是六条");
        check(CohortVisual.settleEncoding(2, 1) == 1026, "结算仍是复制号加 1024 乘生成号");
        check(CohortVisual.settleEncoding(1, 2) == 2049, "生成号继续占高位");

        CohortVisual magic = CohortVisual.capture(2, 3, 0, 1, false, false, false);
        check(!magic.laneSplit() && magic.displayIndex() == 2 && magic.displayCount() == 3, "法术记下序号但不分道");
        check(magic.forDrawing().equals(CohortVisual.SINGLE), "法术绘制回到整张");
        CohortVisual corrupt = new CohortVisual(9, 3, 9, 3, true);
        check(!corrupt.forDrawing().laneSplit(), "越界序号不再另画一份");
    }

    private static void reusedSpecDoesNotLeak() {
        TechniqueHitSpec spec = new TechniqueHitSpec();
        spec.settleCopyIndex = 41;
        CastFrame frame = new CastFrame();
        frame.copyCount = 3;
        CohortVisual[] kept = new CohortVisual[3];
        for (int copyOrdinal = 0; copyOrdinal < 3; copyOrdinal++) {
            frame.copyIndex = copyOrdinal;
            TechniqueHitSpec.assignCohort(spec, frame, 0, 1, true);
            kept[copyOrdinal] = spec.cohortVisual;
        }
        check(kept[0].displayIndex() == 0 && kept[1].displayIndex() == 1 && kept[2].displayIndex() == 2, "后一份不能改掉前一份记录");
        TechniqueHitSpec.releaseCohort(spec);
        check(spec.cohortVisual.equals(CohortVisual.SINGLE), "规格用完回到单份");
        check(spec.settleCopyIndex == 41, "显示记录不改结算号");
        check(kept[2].laneSplit() && kept[2].displayCount() == 3, "已经抄走的记录保持三份");
    }

    private static void sharedAnchorStillSplits() {
        CastFrame frame = new CastFrame();
        frame.copyCount = 3;
        frame.copyIndex = 2;
        frame.overrideGeometry = true;
        frame.origin = new Vec3(4.0, 1.5, 2.0);
        frame.anchor = frame.origin;
        frame.cohortDispersed = true;
        frame.adoptSharedAnchor();
        check(!TechniqueHitSpec.copyDispersed(frame) && !TechniqueHitSpec.spawnDispersed(frame), "同一个接触锚点不是已经散开");
        CohortVisual visual = TechniqueHitSpec.cohortFor(frame, 0, 1, true);
        check(visual.laneSplit() && visual.displayIndex() == 2 && visual.displayCount() == 3, "同锚点三份仍分道");
    }

    private static void dispersedGeometryDoesNotSplitAgain() {
        CastFrame frame = new CastFrame();
        frame.copyCount = 3;
        frame.copyIndex = 1;
        frame.overrideGeometry = true;
        frame.cohortDispersed = true;
        check(TechniqueHitSpec.copyDispersed(frame) && !TechniqueHitSpec.spawnDispersed(frame), "交错刀面只算复制轴散开");
        CohortVisual visual = TechniqueHitSpec.cohortFor(frame, 0, 1, true);
        check(!visual.laneSplit() && visual.displayIndex() == 1 && visual.displayCount() == 3, "已散开不再加第二层位移");

        CastFrame volume = new CastFrame();
        volume.copyCount = 3;
        volume.copyIndex = 0;
        volume.overrideGeometry = true;
        volume.trajectoryMutable = true;
        volume.volumeHalf = 1.5;
        volume.anchor = new Vec3(0.0, 1.0, 0.0);
        check(TechniqueHitSpec.copyDispersed(volume) && TechniqueHitSpec.spawnDispersed(volume), "落点体积两轴都散开");
        CohortVisual volumeVisual = TechniqueHitSpec.cohortFor(volume, 1, 2, true);
        check(!volumeVisual.laneSplit() && volumeVisual.laneCount() == 1, "体积撒点的倍率份也不再平移");
        check(volumeVisual.displayIndex() == 1 && volumeVisual.displayCount() == 6, "双轴散开仍保留原始序号");

        CastFrame untouched = new CastFrame();
        untouched.overrideGeometry = true;
        untouched.copyCount = 3;
        untouched.copyIndex = 1;
        untouched.anchor = new Vec3(8.0, 2.0, 1.0);
        check(!TechniqueHitSpec.copyDispersed(untouched) && !TechniqueHitSpec.spawnDispersed(untouched), "只搬锚点的覆写不算散开");
        check(TechniqueHitSpec.cohortFor(untouched, 0, 1, true).laneSplit(), "无法证明散开时仍分道");
    }

    private static void declarationsStayNarrow() {
        ReleaseDefinitions.registerAll();
        check(ReleaseRegistry.get("sword_horizontal_slash").separatesCohortVisuals(), "横劈参与分道");
        check(ReleaseRegistry.get("sword_vertical_slash").separatesCohortVisuals(), "竖劈参与分道");
        check(ReleaseRegistry.get("sword_thrust").separatesCohortVisuals(), "突刺参与分道");
        check(ReleaseRegistry.get("tide_sever").separatesCohortVisuals(), "断潮参与分道");
        check(ReleaseRegistry.get("stone_hammer").separatesCohortVisuals(), "岩槌参与分道");
        check(ReleaseRegistry.get("space_cut").separatesCohortVisuals(), "空间切割参与分道");
        check(ReleaseRegistry.get("water_yielding_ribbon").separatesCohortVisuals(), "让锋参与分道");
        ReleaseDefinition functionCohort = functionCohortFixture();
        check(functionCohort.trajectory() == Trajectory.FUNCTION, "函数样本仍是函数弹道");
        check(functionCohort.separatesCohortVisuals(), "函数样本参与分道");
        check(ReleaseRegistry.get(functionCohort.id()) == null, "函数样本不进正式释放表");
        check(!ReleaseRegistry.get("magic_arcane_missile").separatesCohortVisuals(), "飞弹保持关闭");
        check(!ReleaseRegistry.get("heat_beam").separatesCohortVisuals(), "热光束保持关闭");
        check(!ReleaseRegistry.get("water_weeping_arc").separatesCohortVisuals(), "垂丝保持关闭");
        check(!ReleaseRegistry.get("phantom_sword").separatesCohortVisuals(), "幻剑不造斩击分道");
        check(!ReleaseRegistry.get("sword_come").separatesCohortVisuals(), "剑来不造斩击分道");
        check(!ReleaseRegistry.get("sword_return_all").separatesCohortVisuals(), "万剑归宗不造斩击分道");
        WaterReleaseDefinition blade = WaterReleaseDefinition.shot("cohort_blade_probe", Trajectory.STRAIGHT, 10, 1.3f, 0.35f).swordForm();
        WaterReleaseDefinition magicShot = WaterReleaseDefinition.shot("cohort_magic_probe", Trajectory.STRAIGHT, 10, 1.3f, 0.35f);
        check(blade.separatesCohortVisuals(), "水刃剑形打开分道");
        check(!magicShot.separatesCohortVisuals(), "其他水系射击保持关闭");
    }

    private static void sheetLanes() {
        FaceWindow alone = CohortLaneLayout.sheetWindow(5.0, 3.0, 0.0, CohortVisual.SINGLE);
        check(near(alone.acrossLow(), -2.5F) && near(alone.acrossHigh(), 2.5F), "单份横斩宽度不变");
        check(near(alone.riseLow(), -1.5F) && near(alone.riseHigh(), 1.5F), "单份横斩高度不变");
        check(CohortLaneLayout.separableAxis(5.0, 3.0) == SeparableAxis.RISE, "横斩沿上下分");
        FaceWindow[] horizontal = new FaceWindow[3];
        for (int copyOrdinal = 0; copyOrdinal < 3; copyOrdinal++) {
            horizontal[copyOrdinal] = CohortLaneLayout.sheetWindow(5.0, 3.0, 0.0, lane(copyOrdinal, 3));
            check(near(horizontal[copyOrdinal].acrossLow(), alone.acrossLow())
                    && near(horizontal[copyOrdinal].acrossHigh(), alone.acrossHigh()), "横斩不挪刀刃方向");
            check(horizontal[copyOrdinal].riseLow() >= alone.riseLow() - 1.0E-4F
                    && horizontal[copyOrdinal].riseHigh() <= alone.riseHigh() + 1.0E-4F, "横斩槽留在原高度里");
        }
        check(clearance(horizontal[0], horizontal[1]) > 0.0F && clearance(horizontal[1], horizontal[2]) > 0.0F, "三道横斩之间有空隙");

        check(CohortLaneLayout.separableAxis(1.6, 2.0) == SeparableAxis.ACROSS, "竖斩沿左右分");
        FaceWindow verticalAlone = CohortLaneLayout.sheetWindow(1.6, 1.5, 2.0, CohortVisual.SINGLE);
        FaceWindow verticalEdge = CohortLaneLayout.sheetWindow(1.6, 1.5, 2.0, lane(1, 3));
        check(near(verticalEdge.riseLow(), verticalAlone.riseLow()) && near(verticalEdge.riseHigh(), verticalAlone.riseHigh()), "竖斩不挪刀刃方向");
        check(verticalEdge.acrossLow() > verticalAlone.acrossLow() && verticalEdge.acrossHigh() < verticalAlone.acrossHigh(), "竖斩中间槽比整张窄");

        check(CohortLaneLayout.separableAxis(1.0, 1.0) == SeparableAxis.RISE, "方形成面上下分");
        FaceWindow thrust = CohortLaneLayout.sheetWindow(1.0, 3.0, 1.0, lane(0, 3));
        FaceWindow thrustAlone = CohortLaneLayout.sheetWindow(1.0, 3.0, 1.0, CohortVisual.SINGLE);
        check(near(thrust.acrossLow(), thrustAlone.acrossLow()) && thrust.riseHigh() < thrustAlone.riseHigh(), "突刺压在上下槽里");

        check(CohortLaneLayout.separableAxis(0.4, 3.0) == SeparableAxis.ACROSS, "断潮沿厚度左右分");
        FaceWindow tide = CohortLaneLayout.sheetWindow(0.4, 5.0, 3.0, lane(0, 3));
        check(tide.acrossHigh() <= 0.2F + 1.0E-4F && tide.acrossLow() >= -0.2F - 1.0E-4F, "断潮槽不超出命中厚度");

        float[] nineRise = new float[9];
        for (int copyOrdinal = 0; copyOrdinal < 9; copyOrdinal++) {
            nineRise[copyOrdinal] = CohortLaneLayout.sheetWindow(5.0, 3.0, 0.0, lane(copyOrdinal, 9)).riseLow();
            if (copyOrdinal > 0) {
                check(nineRise[copyOrdinal] > nineRise[copyOrdinal - 1], "九份横斩槽依次上移");
            }
        }
        check(nineRise[3] != horizontal[0].riseLow(), "九份第四槽不是三份的第一槽");
    }

    private static void partingAndRibbon() {
        SlashInterval glowAlone = CohortLaneLayout.partingSlash(0.7, 3.2, 0.85, 1.6, CohortVisual.SINGLE, false);
        SlashInterval coreAlone = CohortLaneLayout.partingSlash(0.7, 3.2, 0.22, 1.6, CohortVisual.SINGLE, true);
        check(near(glowAlone.sideLow(), -0.85F) && near(glowAlone.sideHigh(), 0.85F), "单份让锋辉光半宽不变");
        check(near(coreAlone.sideLow(), -0.22F) && near(coreAlone.sideHigh(), 0.22F), "单份让锋刃芯半宽不变");
        check(near(glowAlone.riseLow(), -1.6F) && near(coreAlone.riseHigh(), 1.6F), "单份让锋高度不变");

        SlashInterval[] glow = new SlashInterval[3];
        SlashInterval[] core = new SlashInterval[3];
        for (int copyOrdinal = 0; copyOrdinal < 3; copyOrdinal++) {
            CohortVisual visual = lane(copyOrdinal, 3);
            glow[copyOrdinal] = CohortLaneLayout.partingSlash(0.7, 3.2, 0.85, 1.6, visual, false);
            core[copyOrdinal] = CohortLaneLayout.partingSlash(0.7, 3.2, 0.22, 1.6, visual, true);
            check(glow[copyOrdinal].sideLow() >= -0.35F - 1.0E-4F && glow[copyOrdinal].sideHigh() <= 0.35F + 1.0E-4F, "让锋斩光留在命中宽度里");
            check(core[copyOrdinal].sideLow() >= glow[copyOrdinal].sideLow() - 1.0E-4F
                    && core[copyOrdinal].sideHigh() <= glow[copyOrdinal].sideHigh() + 1.0E-4F, "刃芯不出自己的辉光槽");
            check(near(glow[copyOrdinal].riseLow(), -1.6F) && near(glow[copyOrdinal].riseHigh(), 1.6F), "让锋展开高度不变");
        }
        check(glow[0].sideHigh() < glow[1].sideLow() && glow[1].sideHigh() < glow[2].sideLow(), "三道让锋之间有空隙");

        ReleaseDefinition functionCohort = functionCohortFixture();
        check(functionCohort.trajectory() == Trajectory.FUNCTION, "水带样本仍是函数弹道");
        check(functionCohort.separatesCohortVisuals(), "水带样本打开分道");
        double ribbonWidth = functionCohort.thickness();
        RibbonStrip singleRibbon = CohortLaneLayout.ribbonStrip(ribbonWidth, 0.12, CohortVisual.SINGLE);
        check(!singleRibbon.separated() && near(singleRibbon.center(), 0.0) && near(singleRibbon.halfSpan(), 0.12), "单份水带半宽不变");
        RibbonStrip[] ribbons = new RibbonStrip[3];
        for (int copyOrdinal = 0; copyOrdinal < 3; copyOrdinal++) {
            ribbons[copyOrdinal] = CohortLaneLayout.ribbonStrip(ribbonWidth, 0.12, lane(copyOrdinal, 3));
            check(ribbons[copyOrdinal].separated(), "函数水带三份收窄");
            double lowEdge = ribbons[copyOrdinal].center() - ribbons[copyOrdinal].halfSpan();
            double highEdge = ribbons[copyOrdinal].center() + ribbons[copyOrdinal].halfSpan();
            check(lowEdge >= -0.175 - 1.0E-8 && highEdge <= 0.175 + 1.0E-8, "水带留在命中横截面里");
        }
        check(ribbons[0].center() + ribbons[0].halfSpan() < ribbons[1].center() - ribbons[1].halfSpan(), "水带槽之间有空隙");
        RibbonStrip ninth = CohortLaneLayout.ribbonStrip(ribbonWidth, 0.12, lane(3, 9));
        check(ninth.center() != ribbons[0].center(), "九份水带不回到第一条");

        LateralAxis upward = CohortLaneLayout.stableLateral(0.0, 1.0, 0.0);
        LateralAxis downward = CohortLaneLayout.stableLateral(0.0, -1.0, 0.0);
        double agreement = upward.east() * downward.east() + upward.above() * downward.above() + upward.south() * downward.south();
        check(agreement > 0.9, "竖直切向两侧不翻轴");
        LateralAxis ahead = CohortLaneLayout.stableLateral(0.0, 0.0, 1.0);
        LateralAxis back = CohortLaneLayout.stableLateral(0.0, 0.0, -1.0);
        double flatAgreement = ahead.east() * back.east() + ahead.above() * back.above() + ahead.south() * back.south();
        check(flatAgreement > 0.9, "水平反向也不翻轴");
    }

    private static void radialAndOuterArcs() {
        double sampleAge = 3.0;
        int activeTicks = ArcBandSnapshot.ACTIVE_TICKS;
        int tailTicks = ArcBandSnapshot.TAIL_TICKS;
        int frame = WaterArcSheetMap.frameIndex(sampleAge, activeTicks, tailTicks);
        double revealed = WaterArcSheetMap.reveal(sampleAge, activeTicks);
        check(frame == WaterArcSheetMap.frameIndex(sampleAge, activeTicks, tailTicks), "帧号不看份数");
        ArcBandSnapshot scaled = radial(0, 1.35);
        check(scaled.mode() == ArcBandMode.RADIAL_BLADE, "水刃仍是径向刃片");
        check(near(scaled.radius(), ArcBandSnapshot.BASE_RADIUS * 1.35), "1.35 倍率只冻一次");
        double plainInner = WaterArcSheetMap.placedDistance(scaled, WaterArcSheetMap.RHO_LOW, CohortVisual.SINGLE);
        double plainOuter = WaterArcSheetMap.placedDistance(scaled, WaterArcSheetMap.RHO_HIGH, CohortVisual.SINGLE);
        check(near(plainInner, WaterArcSheetMap.worldDistance(scaled, WaterArcSheetMap.RHO_LOW)), "单份径向仍走原极径");
        check(near(plainOuter, WaterArcSheetMap.worldDistance(scaled, WaterArcSheetMap.RHO_HIGH)), "单份外缘也不改");

        double[] laneInner = new double[3];
        double[] laneOuter = new double[3];
        for (int copyOrdinal = 0; copyOrdinal < 3; copyOrdinal++) {
            CohortVisual visual = lane(copyOrdinal, 3);
            laneInner[copyOrdinal] = WaterArcSheetMap.placedDistance(scaled, WaterArcSheetMap.RHO_LOW, visual);
            laneOuter[copyOrdinal] = WaterArcSheetMap.placedDistance(scaled, WaterArcSheetMap.RHO_HIGH, visual);
            check(laneInner[copyOrdinal] >= scaled.radius() * 0.35 - 1.0E-6, "径向槽不进到 0.35 倍半径以内");
            check(laneOuter[copyOrdinal] <= scaled.radius() + 1.0E-6, "径向槽不超出半径");
            check(laneOuter[copyOrdinal] > laneInner[copyOrdinal], "每条径向槽有宽度");
            ArcVec plainOffset = WaterArcSheetMap.offsetAt(scaled, 0.4, WaterArcSheetMap.RHO_MID);
            ArcVec splitOffset = WaterArcSheetMap.offsetAt(scaled, 0.4, WaterArcSheetMap.RHO_MID, visual);
            check(plainOffset.cross(splitOffset).length() < 1.0E-6, "分道不转扫角");
            SheetCorner plainCorner = WaterArcSheetMap.corner(scaled, frame, sampleAge, activeTicks, revealed * 0.5, WaterArcSheetMap.RHO_MID);
            SheetCorner splitCorner = WaterArcSheetMap.corner(scaled, frame, sampleAge, activeTicks, revealed * 0.5, WaterArcSheetMap.RHO_MID, visual);
            check(near(plainCorner.textureU(), splitCorner.textureU()) && near(plainCorner.textureV(), splitCorner.textureV()), "同一揭示角的贴图坐标不变");
        }
        check(laneOuter[0] < laneInner[1] && laneOuter[1] < laneInner[2], "三道水弧之间有径向空隙");

        double[] nineDistance = new double[9];
        for (int copyOrdinal = 0; copyOrdinal < 9; copyOrdinal++) {
            nineDistance[copyOrdinal] = WaterArcSheetMap.placedDistance(scaled, WaterArcSheetMap.RHO_MID, lane(copyOrdinal, 9));
            if (copyOrdinal > 0) {
                check(nineDistance[copyOrdinal] > nineDistance[copyOrdinal - 1], "九份径向槽不循环");
            }
        }
        check(nineDistance[3] != laneInner[0], "九份第四槽不是三条里的第一条");

        for (int beatIndex = 0; beatIndex < 3; beatIndex++) {
            ArcBandSnapshot beat = radial(beatIndex, 1.35);
            check(beat.mode() == ArcBandMode.RADIAL_BLADE, "三拍都保持径向");
            check(WaterArcSheetMap.frameIndex(sampleAge, beat.activeTicks(), beat.tailTicks()) == frame, "三拍同一年龄同一帧");
            check(near(WaterArcSheetMap.reveal(sampleAge, beat.activeTicks()), revealed), "三拍揭示进度相同");
            double beatDistance = WaterArcSheetMap.placedDistance(beat, WaterArcSheetMap.RHO_MID, lane(1, 3));
            check(near(beatDistance, WaterArcSheetMap.placedDistance(scaled, WaterArcSheetMap.RHO_MID, lane(1, 3))), "拍号不改槽半径");
        }

        int[] quadCount = new int[2];
        WaterArcSheetMap.emit(scaled, 6.0, (startInner, startOuter, endOuter, endInner) -> quadCount[0]++);
        WaterArcSheetMap.emit(scaled, 6.0, (startInner, startOuter, endOuter, endInner) -> quadCount[1]++, lane(8, 9));
        check(quadCount[0] > 0 && quadCount[0] == quadCount[1], "每实体顶点数不随份数翻倍");

        ArcBandSnapshot outer = ArcBandSnapshot.freezeLook(
                new ArcVec(0.0, 1.6, 0.0), new ArcVec(0.0, 0.0, 1.0), null, false,
                0.0, 0, ArcBandSnapshot.BASE_RADIUS, ArcBandSnapshot.BASE_HALF_WIDTH, 1.35, ArcBandMode.OUTER_ARC);
        double bandLow = Math.min(
                WaterArcSheetMap.worldDistance(outer, WaterArcSheetMap.RHO_LOW),
                WaterArcSheetMap.worldDistance(outer, WaterArcSheetMap.RHO_HIGH));
        double bandHigh = Math.max(
                WaterArcSheetMap.worldDistance(outer, WaterArcSheetMap.RHO_LOW),
                WaterArcSheetMap.worldDistance(outer, WaterArcSheetMap.RHO_HIGH));
        for (int copyOrdinal = 0; copyOrdinal < 3; copyOrdinal++) {
            double lowDistance = WaterArcSheetMap.placedDistance(outer, WaterArcSheetMap.RHO_LOW, lane(copyOrdinal, 3));
            double highDistance = WaterArcSheetMap.placedDistance(outer, WaterArcSheetMap.RHO_HIGH, lane(copyOrdinal, 3));
            check(lowDistance >= bandLow - 1.0E-6 && highDistance <= bandHigh + 1.0E-6, "旧外环只在原厚度里分槽");
        }
        check(near(WaterArcSheetMap.placedDistance(outer, WaterArcSheetMap.RHO_MID, CohortVisual.SINGLE),
                WaterArcSheetMap.worldDistance(outer, WaterArcSheetMap.RHO_MID)), "单份外环不改半径");
    }

    private static void subgroupLanes() {
        HashSet<Integer> landingPair = new HashSet<>();
        CastFrame landed = new CastFrame();
        landed.copyCount = 1;
        landed.copyIndex = 0;
        landed.cohortDispersed = true;
        landed.trajectoryMutable = false;
        for (int spawnOrdinal = 0; spawnOrdinal < 2; spawnOrdinal++) {
            CohortVisual visual = TechniqueHitSpec.cohortFor(landed, spawnOrdinal, 2, true);
            check(TechniqueHitSpec.copyDispersed(landed) && !TechniqueHitSpec.spawnDispersed(landed), "落点水刃只散复制轴");
            check(visual.displayCount() == 2 && visual.displayIndex() == spawnOrdinal, "落点一份两把仍有两条原始序号");
            check(visual.laneCount() == 2 && visual.laneIndex() == spawnOrdinal && visual.laneSplit(), "落点水刃在同一份里分两槽");
            check(visual.forDrawing().displayIndex() == spawnOrdinal && visual.forDrawing().displayCount() == 2, "绘制只吃本小组槽号");
            check(visual.stored().displayIndex() == spawnOrdinal, "同步不改写原始序号");
            check(landingPair.add(visual.displayIndex()), "落点两把原始序号不重复");
        }

        HashSet<Integer> landingSix = new HashSet<>();
        CastFrame scattered = new CastFrame();
        scattered.copyCount = 3;
        scattered.cohortDispersed = true;
        scattered.trajectoryMutable = false;
        for (int copyOrdinal = 0; copyOrdinal < 3; copyOrdinal++) {
            scattered.copyIndex = copyOrdinal;
            CohortVisual alone = TechniqueHitSpec.cohortFor(scattered, 0, 1, true);
            check(!alone.laneSplit() && alone.laneCount() == 1 && alone.displayIndex() == copyOrdinal, "已按复制散开的单把不再加槽");
            for (int spawnOrdinal = 0; spawnOrdinal < 2; spawnOrdinal++) {
                CohortVisual visual = TechniqueHitSpec.cohortFor(scattered, spawnOrdinal, 2, true);
                int expected = copyOrdinal * 2 + spawnOrdinal;
                check(visual.displayIndex() == expected && visual.displayCount() == 6, "三份乘二的原始序号仍覆盖全组");
                check(visual.laneIndex() == spawnOrdinal && visual.laneCount() == 2 && visual.laneSplit(), "每个已散开的复制号内部再分两槽");
                check(visual.forDrawing().displayCount() == 2 && visual.forDrawing().displayIndex() == spawnOrdinal, "绘制槽不按六份再切");
                check(visual.stored().displayIndex() == expected && visual.stored().laneIndex() == spawnOrdinal, "原始序号和小组槽同时留下");
                check(landingSix.add(expected), "三份乘二的原始序号唯一");
                check(CohortVisual.settleEncoding(copyOrdinal, spawnOrdinal) == copyOrdinal + spawnOrdinal * 1024, "小组槽不改结算编码");
            }
        }
        check(landingSix.size() == 6, "落点三份乘二仍是六条原始序号");

        FaceWindow subgroup = CohortLaneLayout.sheetWindow(5.0, 3.0, 0.0,
                CohortVisual.capture(1, 3, 1, 2, true, true, false));
        FaceWindow localPair = CohortLaneLayout.sheetWindow(5.0, 3.0, 0.0, lane(1, 2));
        check(near(subgroup.riseLow(), localPair.riseLow()) && near(subgroup.riseHigh(), localPair.riseHigh()), "布局按小组两槽而不是全组六槽");

        CohortVisual spawnOnly = CohortVisual.capture(1, 3, 0, 2, true, false, true);
        CohortVisual sameCopyLaterSpawn = CohortVisual.capture(1, 3, 1, 2, true, false, true);
        CohortVisual nextCopy = CohortVisual.capture(2, 3, 0, 2, true, false, true);
        check(spawnOnly.laneIndex() == 1 && spawnOnly.laneCount() == 3 && spawnOnly.displayIndex() == 2, "只散生成轴时按复制号分槽");
        check(sameCopyLaterSpawn.laneIndex() == spawnOnly.laneIndex() && sameCopyLaterSpawn.displayIndex() == 3, "同一复制号的不同生成号共用槽");
        check(nextCopy.laneIndex() == 2 && nextCopy.displayIndex() == 4 && nextCopy.laneSplit(), "同生成号的不同复制号分到不同槽");

        CastFrame vortex = new CastFrame();
        vortex.pathOverride = Trajectory.VORTEX;
        vortex.trajectoryMutable = true;
        vortex.copyCount = 3;
        vortex.copyIndex = 2;
        check(TechniqueHitSpec.spawnDispersed(vortex) && TechniqueHitSpec.copyDispersed(vortex), "龙卷风按生成号取样同时也按复制号分开");
        CohortVisual vortexVisual = TechniqueHitSpec.cohortFor(vortex, 1, 2, true);
        check(!vortexVisual.laneSplit() && vortexVisual.laneCount() == 1, "双轴都散开时不再加视觉槽");
        check(vortexVisual.displayIndex() == 5 && vortexVisual.displayCount() == 6, "龙卷风仍记下全组原始序号");
        check(vortexVisual.stored().displayIndex() == 5 && vortexVisual.forDrawing().equals(CohortVisual.SINGLE), "双轴记录同步后绘制回整张");
    }

    /** 测试内的函数分道样本。不登记进正式释放表。 */
    private static ReleaseDefinition functionCohortFixture() {
        return new ReleaseDefinition() {
            @Override
            public String id() {
                return "cohort_function_probe";
            }

            @Override
            public ReleaseKind kind() {
                return ReleaseKind.HIT_VOLUME;
            }

            @Override
            public ReleaseTag tag() {
                return ReleaseTag.TRIGGER;
            }

            @Override
            public Trajectory trajectory() {
                return Trajectory.FUNCTION;
            }

            @Override
            public boolean moving() {
                return true;
            }

            @Override
            public int lifetimeTicks() {
                return 32;
            }

            @Override
            public float speed() {
                return 0f;
            }

            @Override
            public float thickness() {
                return 0.35f;
            }

            @Override
            public boolean separatesCohortVisuals() {
                return true;
            }
        };
    }

    private static CohortVisual lane(int displayIndex, int displayCount) {
        return CohortVisual.capture(displayIndex, displayCount, 0, 1, true, false, false);
    }

    private static ArcBandSnapshot radial(int beat, double scale) {
        return ArcBandSnapshot.freezeLook(
                new ArcVec(0.0, 1.6, 0.0), new ArcVec(0.0, 0.0, 1.0), null, false,
                0.0, beat, ArcBandSnapshot.BASE_RADIUS, ArcBandSnapshot.BASE_HALF_WIDTH,
                scale, ArcBandMode.RADIAL_BLADE);
    }

    private static boolean near(double left, double right) {
        return Math.abs(left - right) < 1.0E-5;
    }

    private static void check(boolean passed, String label) {
        if (!passed) {
            throw new IllegalStateException(label);
        }
    }

    private static float clearance(FaceWindow earlier, FaceWindow later) {
        return later.riseLow() - earlier.riseHigh();
    }
}
