package com.bslx.XueXiMod.client.action.skinned;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.client.action.mesh.PlayerSkinMesh;
import com.bslx.XueXiMod.client.action.mesh.SkinDiagnostic;
import com.bslx.XueXiMod.client.action.mesh.SkinPalette;

/**
 * 一帧已经准备好的蒙皮数据。网格和调色板由调用方给出，这里不重新求骨架，也不打开租约。
 * <p>
 * alignment 位于 sourceToView 的输出空间，构造时复制。sourceToView 把源模型空间送到该绘制空间，同样复制。
 * 第三人称的 sourceToView 必须是身体映射 H。第一人称必须由调用方传入另一张视图映射，本帧不保存 H。
 * meshPlacement 是 A 乘 sourceToView 一次的结果，坐标系与 sourceToView 的输出相同，不含 Pvanilla。
 * 顶点仍用调色板的蒙皮矩阵，附件仍用骨全局。公开矩阵方法每次返回新副本。
 */
public final class ImmutableSkinFrame {
    private final RenderPlan plan;
    private final Clock clock;
    private final PlayerSkinMesh mesh;
    private final SkinPalette palette;
    private final OpenMatrix4f alignment;
    private final OpenMatrix4f sourceToView;
    private final OpenMatrix4f meshPlacement;

    private ImmutableSkinFrame(
            RenderPlan plan,
            Clock clock,
            PlayerSkinMesh mesh,
            SkinPalette palette,
            OpenMatrix4f alignment,
            OpenMatrix4f sourceToView,
            OpenMatrix4f meshPlacement) {
        this.plan = plan;
        this.clock = clock;
        this.mesh = mesh;
        this.palette = palette;
        this.alignment = alignment;
        this.sourceToView = sourceToView;
        this.meshPlacement = meshPlacement;
    }

    public static ImmutableSkinFrame captureThirdPerson(
            RenderPlan plan,
            PlayerSkinMesh mesh,
            SkinPalette palette,
            OpenMatrix4f alignment,
            Clock clock) {
        if (plan == null) {
            throw new SkinDiagnostic("plan-missing", "绘制计划缺失");
        }
        if (plan.perspective() != RenderPlan.Perspective.THIRD_PERSON) {
            throw new SkinDiagnostic("perspective", "身体映射只属于第三人称计划");
        }
        return capture(plan, mesh, palette, alignment, AttachmentTransform.sourceToBody(), clock);
    }

    public static ImmutableSkinFrame capture(
            RenderPlan plan,
            PlayerSkinMesh mesh,
            SkinPalette palette,
            OpenMatrix4f alignment,
            OpenMatrix4f sourceToView,
            Clock clock) {
        if (plan == null) {
            throw new SkinDiagnostic("plan-missing", "绘制计划缺失");
        }
        if (plan.mode() != RenderPlan.Mode.SKINNED) {
            throw new SkinDiagnostic("frame-mode", "只有蒙皮模式才建立帧");
        }
        if (mesh == null) {
            throw new SkinDiagnostic("mesh-missing", "蒙皮网格缺失");
        }
        if (palette == null) {
            throw new SkinDiagnostic("palette-missing", "调色板缺失");
        }
        if (clock == null) {
            throw new SkinDiagnostic("clock-missing", "身份时间缺失");
        }
        if (alignment == null) {
            throw new SkinDiagnostic("alignment-missing", "对齐矩阵缺失");
        }
        if (sourceToView == null) {
            throw new SkinDiagnostic("view-missing", "视图映射缺失");
        }
        OpenMatrix4f placement = AttachmentTransform.composePlacement(alignment, sourceToView);
        boolean bodyMap = sameElements(sourceToView, AttachmentTransform.sourceToBody());
        if (plan.perspective() == RenderPlan.Perspective.THIRD_PERSON && !bodyMap) {
            throw new SkinDiagnostic("body-map", "第三人称帧的视图映射必须是身体映射");
        }
        if (plan.perspective() == RenderPlan.Perspective.FIRST_PERSON && bodyMap) {
            throw new SkinDiagnostic("first-person-body-map", "第一人称不能把身体映射当作相机变换");
        }
        return new ImmutableSkinFrame(plan, clock, mesh, palette, new OpenMatrix4f(alignment), new OpenMatrix4f(sourceToView), placement);
    }

    public RenderPlan plan() {
        return this.plan;
    }

    public Clock clock() {
        return this.clock;
    }

    public PlayerSkinMesh mesh() {
        return this.mesh;
    }

    /** 调用方给出的那一份调色板。矩阵读取仍返回副本。 */
    public SkinPalette palette() {
        return this.palette;
    }

    /** 绘制空间里的对齐。第三人称是身体模型空间，第一人称是调用方视图空间。 */
    public OpenMatrix4f copyAlignment() {
        return new OpenMatrix4f(this.alignment);
    }

    /** 源模型空间到本帧绘制空间。第三人称等于身体映射，第一人称是显式视图映射。 */
    public OpenMatrix4f copySourceToView() {
        return new OpenMatrix4f(this.sourceToView);
    }

    /** 第三人称身体映射的副本。第一人称帧没有这张矩阵。 */
    public OpenMatrix4f copyBodyMap() {
        if (this.plan.perspective() != RenderPlan.Perspective.THIRD_PERSON) {
            throw new SkinDiagnostic("first-person-body-map", "第一人称帧没有身体映射");
        }
        return this.copySourceToView();
    }

    /** A 乘 sourceToView 一次。不含骨全局，也不含 Pvanilla。 */
    public OpenMatrix4f copyMeshPlacement() {
        return new OpenMatrix4f(this.meshPlacement);
    }

    public int jointCount() {
        return this.palette.jointCount();
    }

    /** 源模型空间的骨全局，没有乘 inverse bind。 */
    public OpenMatrix4f copyGlobalFrame(int jointId) {
        return this.palette.copyGlobalFrame(jointId);
    }

    /** 源模型空间的骨全局，没有乘 inverse bind。 */
    public OpenMatrix4f copyGlobalFrame(String jointName) {
        return this.palette.copyGlobalFrame(jointName);
    }

    /** 顶点蒙皮矩阵。附件不要用它。 */
    public OpenMatrix4f copySkinMatrix(int jointId) {
        return this.palette.copySkinMatrix(jointId);
    }

    public OpenMatrix4f copyToolRightFrame() {
        return this.palette.copyToolRightFrame();
    }

    public OpenMatrix4f copyToolLeftFrame() {
        return this.palette.copyToolLeftFrame();
    }

    public OpenMatrix4f copyHandRightFrame() {
        return this.palette.copyHandRightFrame();
    }

    public OpenMatrix4f copyHandLeftFrame() {
        return this.palette.copyHandLeftFrame();
    }

    public OpenMatrix4f copyHeadFrame() {
        return this.palette.copyHeadFrame();
    }

    public OpenMatrix4f copyChestFrame() {
        return this.palette.copyChestFrame();
    }

    private static boolean sameElements(OpenMatrix4f left, OpenMatrix4f right) {
        return left.m00 == right.m00 && left.m01 == right.m01 && left.m02 == right.m02 && left.m03 == right.m03
                && left.m10 == right.m10 && left.m11 == right.m11 && left.m12 == right.m12 && left.m13 == right.m13
                && left.m20 == right.m20 && left.m21 == right.m21 && left.m22 == right.m22 && left.m23 == right.m23
                && left.m30 == right.m30 && left.m31 == right.m31 && left.m32 == right.m32 && left.m33 == right.m33;
    }

    /** 调用方已经确定的动作身份和时钟。不含姿态。 */
    public static final class Clock {
        private final String actionName;
        private final long startGameTick;
        private final float elapsedSeconds;
        private final float normalizedProgress;
        private final int revision;

        private Clock(String actionName, long startGameTick, float elapsedSeconds, float normalizedProgress, int revision) {
            this.actionName = actionName;
            this.startGameTick = startGameTick;
            this.elapsedSeconds = elapsedSeconds;
            this.normalizedProgress = normalizedProgress;
            this.revision = revision;
        }

        public static Clock of(String actionName, long startGameTick, float elapsedSeconds, float normalizedProgress, int revision) {
            if (actionName == null || actionName.isBlank()) {
                throw new SkinDiagnostic("action-name", "动作身份缺失");
            }
            if (!Float.isFinite(elapsedSeconds) || elapsedSeconds < 0.0F) {
                throw new SkinDiagnostic("elapsed", "已过时间不是非负有限数");
            }
            if (!Float.isFinite(normalizedProgress) || normalizedProgress < 0.0F || normalizedProgress > 1.0F) {
                throw new SkinDiagnostic("progress", "归一化进度超出范围");
            }
            if (revision < 0) {
                throw new SkinDiagnostic("revision", "修订号不能为负");
            }
            return new Clock(actionName, startGameTick, elapsedSeconds, normalizedProgress, revision);
        }

        public String actionName() {
            return this.actionName;
        }

        public long startGameTick() {
            return this.startGameTick;
        }

        public float elapsedSeconds() {
            return this.elapsedSeconds;
        }

        public float normalizedProgress() {
            return this.normalizedProgress;
        }

        public int revision() {
            return this.revision;
        }
    }
}
