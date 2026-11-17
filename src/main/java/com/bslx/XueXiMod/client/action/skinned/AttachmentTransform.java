package com.bslx.XueXiMod.client.action.skinned;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.client.action.mesh.SkinDiagnostic;
import com.bslx.XueXiMod.client.action.mesh.SkinPalette;

/**
 * 附件和整网放置的纯矩阵组合。
 * <p>
 * 第三人称身体映射 H = T(0, 1.5, 0) * diag(-1, -1, 1)，把源模型点 (x, y, z)
 * 送到原版身体模型空间 (-x, 1.5 - y, z)。比例是 1。原版脚底下移和这条抬升并不相同，
 * 差值留给调用方的 Pvanilla，本映射不吸收它。H 不是相机变换。
 * <p>
 * K = inverse(Gbind) * inverse(sourceToView) * Trest。G 是调色板里的骨全局，
 * 还没有乘 inverse bind。绘制局部是 A * sourceToView * G * K，不含 Pvanilla。
 * K 只存在于返回的矩阵里，不写资源，也不放进静态缓存。
 * 第一人称把已经准备好的 sourceToView 传进来；这里不校准地标，也不用 H 冒充相机。
 */
public final class AttachmentTransform {
    /** 源脖子高度，也是身体映射的抬升。不是原版脚底下移。 */
    public static final float BODY_MODEL_LIFT = 1.5F;

    private AttachmentTransform() {
    }

    /** 源模型空间到原版身体模型空间。只供第三人称身体使用。 */
    public static OpenMatrix4f sourceToBody() {
        OpenMatrix4f mirror = OpenMatrix4f.createScale(-1.0F, -1.0F, 1.0F);
        OpenMatrix4f lift = OpenMatrix4f.createTranslation(0.0F, BODY_MODEL_LIFT, 0.0F);
        return OpenMatrix4f.mul(lift, mirror, null);
    }

    /** 整网刚性放置 A * sourceToView。A 只乘这一次，结果不含骨全局，也不含 Pvanilla。 */
    public static OpenMatrix4f composePlacement(OpenMatrix4f alignment, OpenMatrix4f sourceToView) {
        return multiply(copyUsable(alignment, "alignment-missing"), copyUsable(sourceToView, "view-missing"));
    }

    /** 运行时校正。调用方自己决定是否把返回值留在内存。 */
    public static OpenMatrix4f runtimeCorrection(OpenMatrix4f bindGlobal, OpenMatrix4f sourceToView, OpenMatrix4f restTarget) {
        OpenMatrix4f usableBind = copyUsable(bindGlobal, "bind-missing");
        OpenMatrix4f usableView = copyUsable(sourceToView, "view-missing");
        OpenMatrix4f usableRest = copyUsable(restTarget, "rest-missing");
        OpenMatrix4f inverseView = invert(usableView);
        OpenMatrix4f inverseBind = invert(usableBind);
        return multiply(inverseBind, multiply(inverseView, usableRest));
    }

    /** 绘制局部 A * sourceToView * G * K。G 由调用方从调色板复制。 */
    public static OpenMatrix4f localDraw(
            OpenMatrix4f alignment,
            OpenMatrix4f sourceToView,
            OpenMatrix4f currentGlobal,
            OpenMatrix4f correction) {
        OpenMatrix4f placed = composePlacement(alignment, sourceToView);
        OpenMatrix4f usableGlobal = copyUsable(currentGlobal, "global-missing");
        OpenMatrix4f usableCorrection = copyUsable(correction, "correction-missing");
        return multiply(placed, multiply(usableGlobal, usableCorrection));
    }

    /** 按骨名取 G，再做绘制局部。不读取蒙皮矩阵。 */
    public static OpenMatrix4f localDrawFromPalette(
            OpenMatrix4f alignment,
            OpenMatrix4f sourceToView,
            SkinPalette palette,
            String jointName,
            OpenMatrix4f bindGlobal,
            OpenMatrix4f restTarget) {
        OpenMatrix4f currentGlobal = copyJointGlobal(palette, jointName);
        OpenMatrix4f correction = runtimeCorrection(bindGlobal, sourceToView, restTarget);
        return localDraw(alignment, sourceToView, currentGlobal, correction);
    }

    public static OpenMatrix4f copyJointGlobal(SkinPalette palette, String jointName) {
        if (palette == null) {
            throw new SkinDiagnostic("palette-missing", "调色板缺失");
        }
        if (jointName == null || jointName.isBlank()) {
            throw new SkinDiagnostic("joint-name", "关节名缺失");
        }
        return palette.copyGlobalFrame(jointName);
    }

    private static OpenMatrix4f copyUsable(OpenMatrix4f source, String missingCode) {
        if (source == null) {
            throw new SkinDiagnostic(missingCode, "矩阵缺失");
        }
        if (!source.hasFiniteElements()) {
            throw new SkinDiagnostic("non-finite", "矩阵不是有限数");
        }
        try {
            OpenMatrix4f inverted = OpenMatrix4f.invert(source, null);
            if (inverted == null || !inverted.hasFiniteElements()) {
                throw new SkinDiagnostic("not-invertible", "逆矩阵不是有限数");
            }
        } catch (SkinDiagnostic diagnostic) {
            throw diagnostic;
        } catch (IllegalArgumentException failure) {
            throw new SkinDiagnostic("not-invertible", failure.getMessage() == null ? "矩阵不可逆" : failure.getMessage());
        }
        return new OpenMatrix4f(source);
    }

    private static OpenMatrix4f invert(OpenMatrix4f source) {
        try {
            OpenMatrix4f inverted = OpenMatrix4f.invert(source, null);
            if (inverted == null || !inverted.hasFiniteElements()) {
                throw new SkinDiagnostic("not-invertible", "逆矩阵不是有限数");
            }
            return inverted;
        } catch (SkinDiagnostic diagnostic) {
            throw diagnostic;
        } catch (IllegalArgumentException failure) {
            throw new SkinDiagnostic("not-invertible", failure.getMessage() == null ? "矩阵不可逆" : failure.getMessage());
        }
    }

    private static OpenMatrix4f multiply(OpenMatrix4f outer, OpenMatrix4f inner) {
        try {
            return OpenMatrix4f.mul(outer, inner, null);
        } catch (IllegalArgumentException failure) {
            throw new SkinDiagnostic("non-finite", failure.getMessage() == null ? "矩阵乘积不是有限数" : failure.getMessage());
        }
    }
}
