import com.bslx.XueXiMod.client.WaterCurtainLayout;
import com.bslx.XueXiMod.client.WaterCurtainLayout.Panel;
import com.bslx.XueXiMod.client.WaterCurtainLayout.Piece;

import java.util.List;

/** 离线核对垂丝分件几何和时间。不启动游戏。 */
public final class VerifyWaterCurtain {
    private static int failures;

    public static void main(String[] args) {
        checkFrames();
        checkGatheredWidths();
        checkPartialPeriod();
        checkNarrowFallback();
        checkHeights();
        checkWidthStableAcrossFrames();
        if (failures != 0) {
            System.err.println("FAILED " + failures);
            System.exit(1);
        }
        System.out.println("waterfall-layout-ok");
    }

    private static void checkFrames() {
        for (int lived = 0; lived < 64; lived++) {
            int expected = Math.floorMod(lived / 2, WaterCurtainLayout.FRAME_COUNT);
            expect(WaterCurtainLayout.flowFrame(lived, 0.0F, 0) == expected, "tick " + lived);
            expect(WaterCurtainLayout.flowFrame(lived, 0.99F, 0) == expected, "partial " + lived);
        }
        expect(WaterCurtainLayout.flowFrame(30, 0.0F, 0) == 15, "frame15");
        expect(WaterCurtainLayout.flowFrame(31, 0.99F, 0) == 15, "frame15 hold");
        expect(WaterCurtainLayout.flowFrame(32, 0.0F, 0) == 0, "frame wrap");
        expect(WaterCurtainLayout.flowFrame(32, 0.0F, 0) == WaterCurtainLayout.flowFrame(0, 0.0F, 0), "wrap matches start");
    }

    private static void checkGatheredWidths() {
        double[] gathered = {0.5, 1.0, 2.0, 4.0};
        int[] middleTiles = {1, 4, 10, 22};
        for (int sample = 0; sample < gathered.length; sample++) {
            double curtainWidth = gathered[sample];
            List<Panel> opened = WaterCurtainLayout.panels(curtainWidth, 4.0, 0);
            List<Panel> later = WaterCurtainLayout.panels(curtainWidth, 4.0, 15);
            expect(!opened.isEmpty(), "panels " + curtainWidth);
            expect(sameAlong(opened, later), "width stable " + curtainWidth);
            expect(coverWidth(opened, curtainWidth), "cover " + curtainWidth);
            expect(countPiece(opened, Piece.THIN) == 0, "runtime " + curtainWidth);
            expect(countPiece(firstBand(opened), Piece.MIDDLE) == middleTiles[sample], "middle " + curtainWidth);
            expect(edgesHold(firstBand(opened), curtainWidth), "edges " + curtainWidth);
            expect(frameBounds(opened, 0), "v frame0 " + curtainWidth);
            expect(frameBounds(later, 15), "v frame15 " + curtainWidth);
        }
    }

    private static void checkPartialPeriod() {
        List<Panel> panels = firstBand(WaterCurtainLayout.panels(0.75, 4.0, 3));
        int fullTiles = 0;
        int croppedTiles = 0;
        for (Panel panel : panels) {
            if (panel.piece != Piece.MIDDLE) {
                continue;
            }
            if (panel.textureUEnd > 0.999) {
                fullTiles++;
            } else {
                croppedTiles++;
                expect(Math.abs(panel.textureUEnd - 0.5) < 1.0E-6, "partial u " + panel.textureUEnd);
                expect(Math.abs((panel.alongEnd - panel.alongStart) - WaterCurtainLayout.EDGE_BLOCKS * 0.5) < 1.0E-6,
                        "partial span");
            }
        }
        expect(fullTiles == 2 && croppedTiles == 1, "2.5 periods got " + fullTiles + "+" + croppedTiles);
        expect(coverWidth(panels, 0.75), "cover 0.75");
    }

    private static void checkNarrowFallback() {
        expect(Math.abs(WaterCurtainLayout.THIN_CONTENT_U_START - 25.0 / 64.0) < 1.0E-15, "envelope start");
        expect(Math.abs(WaterCurtainLayout.THIN_CONTENT_U_END - 39.0 / 64.0) < 1.0E-15, "envelope end");
        expectThinEnvelope(0.2, "width 0.2");
        expectThinEnvelope(1.0 / 3.0 - 0.01, "just under third");
        expectThinEnvelope(100.0, "over tile guard");
        List<Panel> early = WaterCurtainLayout.panels(0.2, 1.0, 0);
        List<Panel> late = WaterCurtainLayout.panels(0.2, 1.0, 15);
        expect(early.get(0).textureUStart == late.get(0).textureUStart, "envelope ignores frame");
        expect(early.get(0).textureUEnd == late.get(0).textureUEnd, "envelope end ignores frame");
        List<Panel> meeting = firstBand(WaterCurtainLayout.panels(1.0 / 3.0, 1.0, 0));
        expect(countPiece(meeting, Piece.MIDDLE) == 0, "meet without middle");
        expect(countPiece(meeting, Piece.LEFT) == 1 && countPiece(meeting, Piece.RIGHT) == 1, "meet edges");
        expect(WaterCurtainLayout.panels(0.0, 1.0, 0).isEmpty(), "zero width");
        expect(WaterCurtainLayout.panels(0.5, 0.0, 0).isEmpty(), "zero height");
        expect(WaterCurtainLayout.panels(Double.NaN, 1.0, 0).isEmpty(), "nan width");
    }

    private static void expectThinEnvelope(double curtainWidth, String label) {
        List<Panel> panels = WaterCurtainLayout.panels(curtainWidth, 1.0, 4);
        expect(countPiece(panels, Piece.THIN) == panels.size() && !panels.isEmpty(), label + " thin");
        expect(countPiece(panels, Piece.LEFT) == 0, label + " no edges");
        double opaqueShare = 12.0 / 14.0;
        double collapsedShare = 12.0 / 64.0;
        for (Panel panel : panels) {
            expect(Math.abs(panel.alongStart + curtainWidth * 0.5) < 1.0E-9, label + " start");
            expect(Math.abs(panel.alongEnd - curtainWidth * 0.5) < 1.0E-9, label + " end");
            expect(panel.textureUStart == WaterCurtainLayout.THIN_CONTENT_U_START, label + " u start");
            expect(panel.textureUEnd == WaterCurtainLayout.THIN_CONTENT_U_END, label + " u end");
            double envelope = panel.alongEnd - panel.alongStart;
            expect(Math.abs(envelope - curtainWidth) < 1.0E-9, label + " envelope fills width");
            double visibleOpaque = envelope * opaqueShare;
            expect(Math.abs(visibleOpaque - curtainWidth * opaqueShare) < 1.0E-9, label + " opaque width");
            expect(visibleOpaque > curtainWidth * collapsedShare * 2.0, label + " not hairline");
        }
    }

    private static void checkHeights() {
        List<Panel> shortCurtain = WaterCurtainLayout.panels(0.5, 0.4, 0);
        expect(distinctBottoms(shortCurtain) == 1, "short bands");
        Panel shortPanel = shortCurtain.get(0);
        expect(shortPanel.textureVEnd < 1.0 / WaterCurtainLayout.FRAME_COUNT, "short stays in frame");
        expect(shortPanel.textureVStart > 0.0, "short inset");
        double shortCover = 0.4 / WaterCurtainLayout.FRAME_BLOCKS;
        double shortSpan = (shortCover - 2.0 * WaterCurtainLayout.TEXEL_PAD) / WaterCurtainLayout.FRAME_COUNT;
        expect(Math.abs((shortPanel.textureVEnd - shortPanel.textureVStart) - shortSpan) < 1.0E-9, "short crop");

        List<Panel> tallCurtain = WaterCurtainLayout.panels(0.5, 8.0, 7);
        expect(distinctBottoms(tallCurtain) == 2, "tall bands");
        expect(frameBounds(tallCurtain, 7), "tall frame");
        double fullSpan = (1.0 - 2.0 * WaterCurtainLayout.TEXEL_PAD) / WaterCurtainLayout.FRAME_COUNT;
        Panel upper = tallCurtain.get(0);
        expect(Math.abs((upper.textureVEnd - upper.textureVStart) - fullSpan) < 1.0E-9, "upper full frame");
        Panel lower = tallCurtain.get(tallCurtain.size() - 1);
        expect(lower.textureVEnd - lower.textureVStart < fullSpan - 1.0E-6, "lower cropped");

        List<Panel> fallLimit = WaterCurtainLayout.panels(0.5, 5.5, 0);
        expect(distinctBottoms(fallLimit) == 2, "fall bands");
        double remainder = 5.5 - WaterCurtainLayout.FRAME_BLOCKS;
        double remainderCover = remainder / WaterCurtainLayout.FRAME_BLOCKS;
        Panel tail = fallLimit.get(fallLimit.size() - 1);
        double tailSpan = (remainderCover - 2.0 * WaterCurtainLayout.TEXEL_PAD) / WaterCurtainLayout.FRAME_COUNT;
        expect(Math.abs((tail.textureVEnd - tail.textureVStart) - tailSpan) < 1.0E-9, "fall tail");
        expect(Math.abs(remainderCover - 4.0 / 128.0) < 1.0E-9, "fall tail is 4 px");
    }

    private static void checkWidthStableAcrossFrames() {
        List<Panel> opened = WaterCurtainLayout.panels(1.0, 5.5, 0);
        List<Panel> closed = WaterCurtainLayout.panels(1.0, 5.5, 15);
        expect(opened.size() == closed.size(), "panel count");
        for (int panelIndex = 0; panelIndex < opened.size(); panelIndex++) {
            Panel early = opened.get(panelIndex);
            Panel late = closed.get(panelIndex);
            expect(early.piece == late.piece, "piece");
            expect(Math.abs(early.alongStart - late.alongStart) < 1.0E-12, "along start");
            expect(Math.abs(early.alongEnd - late.alongEnd) < 1.0E-12, "along end");
            expect(early.textureVStart >= 15.0 / 16.0 == false, "early not in last frame");
            expect(late.textureVStart >= 15.0 / 16.0, "late in last frame");
        }
    }

    private static List<Panel> firstBand(List<Panel> panels) {
        if (panels.isEmpty()) {
            return panels;
        }
        double top = panels.get(0).topOffset;
        java.util.ArrayList<Panel> band = new java.util.ArrayList<>();
        for (Panel panel : panels) {
            if (Math.abs(panel.topOffset - top) < 1.0E-9) {
                band.add(panel);
            }
        }
        return band;
    }

    private static boolean sameAlong(List<Panel> opened, List<Panel> later) {
        if (opened.size() != later.size()) {
            return false;
        }
        for (int panelIndex = 0; panelIndex < opened.size(); panelIndex++) {
            Panel early = opened.get(panelIndex);
            Panel late = later.get(panelIndex);
            if (early.piece != late.piece) {
                return false;
            }
            if (Math.abs(early.alongStart - late.alongStart) > 1.0E-12
                    || Math.abs(early.alongEnd - late.alongEnd) > 1.0E-12) {
                return false;
            }
        }
        return true;
    }

    private static boolean coverWidth(List<Panel> panels, double curtainWidth) {
        List<Panel> band = firstBand(panels);
        if (band.isEmpty()) {
            return false;
        }
        Panel first = band.get(0);
        Panel last = band.get(band.size() - 1);
        if (Math.abs(first.alongStart + curtainWidth * 0.5) > 1.0E-6) {
            return false;
        }
        if (Math.abs(last.alongEnd - curtainWidth * 0.5) > 1.0E-6) {
            return false;
        }
        double cursor = first.alongStart;
        for (Panel panel : band) {
            if (panel.alongStart < cursor - 1.0E-6 || panel.alongEnd < panel.alongStart) {
                return false;
            }
            if (panel.textureUStart < -1.0E-9 || panel.textureUEnd > 1.0 + 1.0E-9 || panel.textureUEnd <= panel.textureUStart) {
                return false;
            }
            cursor = panel.alongEnd;
        }
        return Math.abs(cursor - curtainWidth * 0.5) < 1.0E-6;
    }

    private static boolean edgesHold(List<Panel> band, double curtainWidth) {
        Panel left = band.get(0);
        Panel right = band.get(band.size() - 1);
        double edge = WaterCurtainLayout.EDGE_BLOCKS;
        return left.piece == Piece.LEFT
                && right.piece == Piece.RIGHT
                && Math.abs((left.alongEnd - left.alongStart) - edge) < 1.0E-6
                && Math.abs((right.alongEnd - right.alongStart) - edge) < 1.0E-6
                && Math.abs(left.alongStart + curtainWidth * 0.5) < 1.0E-6
                && Math.abs(right.alongEnd - curtainWidth * 0.5) < 1.0E-6;
    }

    private static boolean frameBounds(List<Panel> panels, int frameIndex) {
        double lower = frameIndex / (double) WaterCurtainLayout.FRAME_COUNT;
        double upper = (frameIndex + 1.0) / WaterCurtainLayout.FRAME_COUNT;
        for (Panel panel : panels) {
            if (panel.textureVStart < lower - 1.0E-12 || panel.textureVEnd > upper + 1.0E-12) {
                return false;
            }
            if (panel.textureVEnd <= panel.textureVStart) {
                return false;
            }
        }
        return true;
    }

    private static int countPiece(List<Panel> panels, Piece piece) {
        int count = 0;
        for (Panel panel : panels) {
            if (panel.piece == piece) {
                count++;
            }
        }
        return count;
    }

    private static int distinctBottoms(List<Panel> panels) {
        java.util.ArrayList<Double> seen = new java.util.ArrayList<>();
        for (Panel panel : panels) {
            boolean known = false;
            for (Double bottom : seen) {
                if (Math.abs(bottom - panel.bottomOffset) < 1.0E-9) {
                    known = true;
                    break;
                }
            }
            if (!known) {
                seen.add(panel.bottomOffset);
            }
        }
        return seen.size();
    }

    private static void expect(boolean passed, String label) {
        if (!passed) {
            failures++;
            System.err.println("FAIL " + label);
        }
    }
}
