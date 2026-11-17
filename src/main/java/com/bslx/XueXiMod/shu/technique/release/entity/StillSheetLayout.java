package com.bslx.XueXiMod.shu.technique.release.entity;

/** 快照沿用已有分槽。尺寸用实体上的最终宽长高，不再乘第二遍倍率。 */
public final class StillSheetLayout {
    private StillSheetLayout() {
    }

    /** 用最终宽长高和显示槽取绘制窗口。 */
    public static CohortLaneLayout.FaceWindow faceWindow(double sheetWidth, double sheetLength, double sheetHeight,
                                                         CohortVisual cohort) {
        return CohortLaneLayout.sheetWindow(sheetWidth, sheetLength, sheetHeight, cohort);
    }
}
