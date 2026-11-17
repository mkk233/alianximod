package com.bslx.XueXiMod.client.action.skinned;

import org.slf4j.Logger;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.math.Vec3f;
import com.bslx.XueXiMod.client.action.mesh.PlayerMeshKind;
import com.bslx.XueXiMod.client.action.mesh.PlayerSkinMesh;
import com.mojang.logging.LogUtils;

/**
 * 第一人称手的刚性配准。源端面三点对上原版手臂 cube 的同三点，再乘本帧 equip 链。
 * 比例保持 1。配不准就返回空，不用非均匀缩放把误差藏起来。
 */
public final class HandFrameCalibration {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float SHOULDER_Y = 1.500045F;
    private static final float WRIST_Y = 0.750045F;
    private static final float FIT_LIMIT = 2.0E-3F;
    /** mesh113 已核实的 normal / slim 源文件版本。超出则不用这组固定地标。 */
    public static final String PINNED_NORMAL_SHA256 = "021f5a9b2c307f9215510efb507d9f3d8694ba21fa1db8bbee2e6551a0d06d83";
    public static final String PINNED_SLIM_SHA256 = "8a6aaeb7ff726189559db5df9f592eebd8c55530f89be6ae5745ff12d41d0626";
    private static final OpenMatrix4f[] MODEL_FRAMES = new OpenMatrix4f[4];
    private static final boolean[] FIT_REPORTED = new boolean[4];
    private static long cachedGeneration = Long.MIN_VALUE;
    private static String lastSupportText = "";

    private HandFrameCalibration() {
    }

    public static void invalidate() {
        cachedGeneration = Long.MIN_VALUE;
        for (int slot = 0; slot < MODEL_FRAMES.length; slot++) {
            MODEL_FRAMES[slot] = null;
        }
    }

    /**
     * 选择手架前核对网格种类、源路径和已验证哈希。空表示仍用现有地标；非空则调用方提前退回。
     */
    public static String supportDiagnostic(PlayerSkinMesh mesh, PlayerMeshKind selectedKind) {
        String reason = unsupportedReason(mesh, selectedKind);
        if (reason != null && !reason.equals(lastSupportText)) {
            lastSupportText = reason;
            LOGGER.warn(reason);
        }
        return reason;
    }

    public static OpenMatrix4f modelFrame(PlayerMeshKind kind, boolean rightSide) {
        int slot = slot(kind, rightSide);
        if (slot < 0) {
            return null;
        }
        long generation = SkinAssets.current().resourceGeneration();
        if (cachedGeneration != generation) {
            invalidate();
            cachedGeneration = generation;
        }
        OpenMatrix4f cached = MODEL_FRAMES[slot];
        if (cached != null) {
            return new OpenMatrix4f(cached);
        }
        OpenMatrix4f fitted = fit(kind, rightSide);
        if (fitted == null) {
            if (!FIT_REPORTED[slot]) {
                FIT_REPORTED[slot] = true;
                LOGGER.warn("第一人称 {} {} 手无法在 {} 内配成行列式为正的刚性手架，该手保持原绘制", kind.wireName(), rightSide ? "右" : "左", FIT_LIMIT);
            }
            return null;
        }
        MODEL_FRAMES[slot] = new OpenMatrix4f(fitted);
        return fitted;
    }

    public static OpenMatrix4f handFrame(PlayerMeshKind kind, boolean rightSide, float equip) {
        OpenMatrix4f model = modelFrame(kind, rightSide);
        if (model == null) {
            return null;
        }
        return OpenMatrix4f.mul(armChain(rightSide ? 1.0F : -1.0F, equip), model, null);
    }

    /** renderPlayerArm 在 swing 为 0 时、renderHand 之前的入口局部链。 */
    public static OpenMatrix4f armChain(float sideSign, float equip) {
        float safeEquip = Float.isFinite(equip) ? equip : 0.0F;
        OpenMatrix4f firstShift = OpenMatrix4f.createTranslation(sideSign * 0.64000005F, -0.6F - 0.6F * safeEquip, -0.71999997F);
        OpenMatrix4f yawOut = rotation(0.0F, 1.0F, 0.0F, sideSign * 45.0F);
        OpenMatrix4f midShift = OpenMatrix4f.createTranslation(-sideSign, 3.6F, 3.5F);
        OpenMatrix4f roll = rotation(0.0F, 0.0F, 1.0F, sideSign * 120.0F);
        OpenMatrix4f pitch = rotation(1.0F, 0.0F, 0.0F, 200.0F);
        OpenMatrix4f yawIn = rotation(0.0F, 1.0F, 0.0F, sideSign * -135.0F);
        OpenMatrix4f lastShift = OpenMatrix4f.createTranslation(sideSign * 5.6F, 0.0F, 0.0F);
        return product(firstShift, yawOut, midShift, roll, pitch, yawIn, lastShift);
    }

    /** generic 持剑在 swing 为 0、ItemRenderer 之前的入口局部平移。不含 display。 */
    public static OpenMatrix4f itemAnchor(float sideSign, float equip) {
        float safeEquip = Float.isFinite(equip) ? equip : 0.0F;
        return OpenMatrix4f.createTranslation(sideSign * 0.56F, -0.52F - 0.6F * safeEquip, -0.72F);
    }

    public static OpenMatrix4f around(OpenMatrix4f spin, float centerX, float centerY, float centerZ) {
        OpenMatrix4f usable = spin == null ? new OpenMatrix4f() : spin;
        OpenMatrix4f toward = OpenMatrix4f.createTranslation(centerX, centerY, centerZ);
        OpenMatrix4f away = OpenMatrix4f.createTranslation(-centerX, -centerY, -centerZ);
        return OpenMatrix4f.mul(toward, OpenMatrix4f.mul(usable, away, null), null);
    }

    public static Vec3f sourceShoulder(PlayerMeshKind kind, boolean rightSide) {
        return new Vec3f(sourceCenter(kind, rightSide), SHOULDER_Y, 0.0F);
    }

    public static Vec3f sourceWrist(PlayerMeshKind kind, boolean rightSide) {
        return new Vec3f(sourceCenter(kind, rightSide), WRIST_Y, 0.0F);
    }

    public static Vec3f vanillaShoulder(PlayerMeshKind kind, boolean rightSide) {
        return vanillaPoint(kind, rightSide, true);
    }

    public static Vec3f vanillaWrist(PlayerMeshKind kind, boolean rightSide) {
        return vanillaPoint(kind, rightSide, false);
    }

    private static String unsupportedReason(PlayerSkinMesh mesh, PlayerMeshKind selectedKind) {
        if (mesh == null || selectedKind == null || mesh.kind() != selectedKind) {
            return "手架校准缺少已验证网格，当前选择不在支持范围内";
        }
        String pinnedHash = selectedKind == PlayerMeshKind.SLIM ? PINNED_SLIM_SHA256 : PINNED_NORMAL_SHA256;
        String sourcePath = mesh.sourceGitPath();
        String sourceHash = mesh.sourceSha256();
        boolean pathMatches = selectedKind.sourceGitPath().equals(sourcePath);
        boolean hashMatches = sourceHash != null && pinnedHash.equalsIgnoreCase(sourceHash);
        if (pathMatches && hashMatches) {
            return null;
        }
        return "手架校准只接受已验证网格版本，当前来源不在支持范围内："
                + selectedKind.wireName() + " " + sourcePath + " " + sourceHash;
    }

    private static OpenMatrix4f fit(PlayerMeshKind kind, boolean rightSide) {
        Vec3f sourceShoulder = sourceShoulder(kind, rightSide);
        Vec3f sourceWrist = sourceWrist(kind, rightSide);
        Vec3f sourceOuter = sourceOuter(kind, rightSide);
        Vec3f targetShoulder = vanillaShoulder(kind, rightSide);
        Vec3f targetWrist = vanillaWrist(kind, rightSide);
        Vec3f targetOuter = vanillaOuter(kind, rightSide);
        if (Math.abs(distance(sourceShoulder, sourceWrist) - distance(targetShoulder, targetWrist)) > FIT_LIMIT) {
            return null;
        }
        if (Math.abs(distance(sourceShoulder, sourceOuter) - distance(targetShoulder, targetOuter)) > FIT_LIMIT) {
            return null;
        }
        OpenMatrix4f sourceBasis = basis(sourceShoulder, sourceWrist, sourceOuter);
        OpenMatrix4f targetBasis = basis(targetShoulder, targetWrist, targetOuter);
        if (sourceBasis == null || targetBasis == null) {
            return null;
        }
        OpenMatrix4f inverseSource;
        OpenMatrix4f spin;
        try {
            inverseSource = OpenMatrix4f.invert(sourceBasis, null);
            if (inverseSource == null) {
                return null;
            }
            spin = OpenMatrix4f.mul(targetBasis, inverseSource, null);
        } catch (IllegalArgumentException failure) {
            return null;
        }
        if (linearDeterminant(spin) <= 0.5F) {
            return null;
        }
        Vec3f spunShoulder = OpenMatrix4f.transform3v(spin, sourceShoulder, null);
        OpenMatrix4f fitted = new OpenMatrix4f(spin);
        fitted.m30 = targetShoulder.coordinateX - spunShoulder.coordinateX;
        fitted.m31 = targetShoulder.coordinateY - spunShoulder.coordinateY;
        fitted.m32 = targetShoulder.coordinateZ - spunShoulder.coordinateZ;
        if (!fitted.hasFiniteElements()) {
            return null;
        }
        if (gap(fitted, sourceShoulder, targetShoulder) > FIT_LIMIT
                || gap(fitted, sourceWrist, targetWrist) > FIT_LIMIT
                || gap(fitted, sourceOuter, targetOuter) > FIT_LIMIT) {
            return null;
        }
        return fitted;
    }

    private static Vec3f vanillaPoint(PlayerMeshKind kind, boolean rightSide, boolean shoulder) {
        float acrossPixels;
        float upwardPixels;
        if (kind == PlayerMeshKind.SLIM) {
            acrossPixels = rightSide ? -5.5F : 5.5F;
            upwardPixels = shoulder ? 0.5F : 12.5F;
        } else {
            acrossPixels = rightSide ? -6.0F : 6.0F;
            upwardPixels = shoulder ? 0.0F : 12.0F;
        }
        return new Vec3f(acrossPixels / 16.0F, upwardPixels / 16.0F, 0.0F);
    }

    private static Vec3f vanillaOuter(PlayerMeshKind kind, boolean rightSide) {
        Vec3f shoulder = vanillaPoint(kind, rightSide, true);
        float outward = kind == PlayerMeshKind.SLIM ? 1.5F / 16.0F : 2.0F / 16.0F;
        float across = rightSide ? shoulder.coordinateX - outward : shoulder.coordinateX + outward;
        return new Vec3f(across, shoulder.coordinateY, shoulder.coordinateZ);
    }

    private static Vec3f sourceOuter(PlayerMeshKind kind, boolean rightSide) {
        float center = sourceCenter(kind, rightSide);
        float outward = kind == PlayerMeshKind.SLIM ? 0.0937505F : 0.125F;
        float across = rightSide ? center + outward : center - outward;
        return new Vec3f(across, SHOULDER_Y, 0.0F);
    }

    private static float sourceCenter(PlayerMeshKind kind, boolean rightSide) {
        float center = kind == PlayerMeshKind.SLIM ? 0.3437505F : 0.375F;
        return rightSide ? center : -center;
    }

    private static OpenMatrix4f basis(Vec3f shoulder, Vec3f wrist, Vec3f outer) {
        Vec3f length = normalize(wrist.coordinateX - shoulder.coordinateX, wrist.coordinateY - shoulder.coordinateY, wrist.coordinateZ - shoulder.coordinateZ);
        Vec3f wide = normalize(outer.coordinateX - shoulder.coordinateX, outer.coordinateY - shoulder.coordinateY, outer.coordinateZ - shoulder.coordinateZ);
        if (length == null || wide == null) {
            return null;
        }
        float along = length.coordinateX * wide.coordinateX + length.coordinateY * wide.coordinateY + length.coordinateZ * wide.coordinateZ;
        wide = normalize(
                wide.coordinateX - length.coordinateX * along,
                wide.coordinateY - length.coordinateY * along,
                wide.coordinateZ - length.coordinateZ * along);
        if (wide == null) {
            return null;
        }
        Vec3f third = normalize(
                length.coordinateY * wide.coordinateZ - length.coordinateZ * wide.coordinateY,
                length.coordinateZ * wide.coordinateX - length.coordinateX * wide.coordinateZ,
                length.coordinateX * wide.coordinateY - length.coordinateY * wide.coordinateX);
        if (third == null) {
            return null;
        }
        OpenMatrix4f matrix = new OpenMatrix4f();
        matrix.m00 = length.coordinateX;
        matrix.m01 = length.coordinateY;
        matrix.m02 = length.coordinateZ;
        matrix.m10 = wide.coordinateX;
        matrix.m11 = wide.coordinateY;
        matrix.m12 = wide.coordinateZ;
        matrix.m20 = third.coordinateX;
        matrix.m21 = third.coordinateY;
        matrix.m22 = third.coordinateZ;
        return matrix;
    }

    private static OpenMatrix4f rotation(float axisX, float axisY, float axisZ, float degrees) {
        return OpenMatrix4f.createRotatorDeg(degrees, new Vec3f(axisX, axisY, axisZ));
    }

    private static OpenMatrix4f product(OpenMatrix4f... factors) {
        OpenMatrix4f combined = new OpenMatrix4f();
        for (int factorIndex = factors.length - 1; factorIndex >= 0; factorIndex--) {
            combined = OpenMatrix4f.mul(factors[factorIndex], combined, null);
        }
        return combined;
    }

    private static int slot(PlayerMeshKind kind, boolean rightSide) {
        if (kind == PlayerMeshKind.NORMAL) {
            return rightSide ? 0 : 1;
        }
        if (kind == PlayerMeshKind.SLIM) {
            return rightSide ? 2 : 3;
        }
        return -1;
    }

    private static float linearDeterminant(OpenMatrix4f matrix) {
        float partA = matrix.m11 * matrix.m22 - matrix.m12 * matrix.m21;
        float partB = matrix.m10 * matrix.m22 - matrix.m12 * matrix.m20;
        float partC = matrix.m10 * matrix.m21 - matrix.m11 * matrix.m20;
        return matrix.m00 * partA - matrix.m01 * partB + matrix.m02 * partC;
    }

    private static float gap(OpenMatrix4f matrix, Vec3f source, Vec3f target) {
        Vec3f moved = OpenMatrix4f.transform3v(matrix, source, null);
        return distance(moved, target);
    }

    private static float distance(Vec3f left, Vec3f right) {
        float across = left.coordinateX - right.coordinateX;
        float upward = left.coordinateY - right.coordinateY;
        float ahead = left.coordinateZ - right.coordinateZ;
        return (float) Math.sqrt(across * across + upward * upward + ahead * ahead);
    }

    private static Vec3f normalize(float across, float upward, float ahead) {
        float lengthSquared = across * across + upward * upward + ahead * ahead;
        if (!Float.isFinite(lengthSquared) || lengthSquared < 1.0E-12F) {
            return null;
        }
        float inverseLength = 1.0F / (float) Math.sqrt(lengthSquared);
        return new Vec3f(across * inverseLength, upward * inverseLength, ahead * inverseLength);
    }
}
