package com.bslx.XueXiMod.client.action.mesh;

import com.bslx.XueXiMod.animation.core.model.Armature;
import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.runtime.ActionSampleContext;

/**
 * 复用现有采样的时钟、姿态和调用方给出的实际骨架。
 * 不新建播放器，也不改刚体回投。
 * 整网变换单独保存，不写进骨骼调色板，也不烘焙进网格数组。
 */
public final class SkinSampleView {
    private final ActionSampleContext sample;
    private final Armature actualArmature;
    private final OpenMatrix4f wholeMeshTransform;
    private final SkinPalette palette;

    private SkinSampleView(ActionSampleContext sample, Armature actualArmature, OpenMatrix4f wholeMeshTransform, SkinPalette palette) {
        this.sample = sample;
        this.actualArmature = actualArmature;
        this.wholeMeshTransform = wholeMeshTransform;
        this.palette = palette;
    }

    public static SkinSampleView bind(ActionSampleContext sample, Armature actualArmature, OpenMatrix4f wholeMeshTransform) {
        if (sample == null || !sample.active()) {
            throw new SkinDiagnostic("sample-inactive", "没有活跃采样，不建立蒙皮");
        }
        if (actualArmature == null) {
            throw new SkinDiagnostic("armature-missing", "缺少实际骨架");
        }
        String expectedName = sample.armatureName();
        if (expectedName != null && !expectedName.isBlank() && !expectedName.equals(actualArmature.toString())) {
            throw new SkinDiagnostic("armature-name", "采样骨架名和实际骨架不一致");
        }
        OpenMatrix4f copied = wholeMeshTransform == null ? new OpenMatrix4f() : new OpenMatrix4f(wholeMeshTransform);
        if (!copied.hasFiniteElements()) {
            throw new SkinDiagnostic("mesh-transform", "整网变换不是有限数");
        }
        return new SkinSampleView(sample, actualArmature, copied, CpuSkinner.preparePalette(actualArmature, sample.pose()));
    }

    public float elapsedSeconds() {
        return this.sample.elapsedSeconds();
    }

    public float normalizedProgress() {
        return this.sample.normalizedProgress();
    }

    public long startGameTick() {
        return this.sample.startGameTick();
    }

    public SkinPalette palette() {
        return this.palette;
    }

    public Armature actualArmature() {
        return this.actualArmature;
    }

    public OpenMatrix4f copyWholeMeshTransform() {
        return new OpenMatrix4f(this.wholeMeshTransform);
    }

    public int emit(PlayerSkinMesh mesh, SkinLease lease, PartVisibility visibility, int packedLight, int overlay, CornerSink sink) {
        return MeshRenderView.emit(mesh, this.palette, lease, visibility, this.wholeMeshTransform, packedLight, overlay, sink);
    }
}
