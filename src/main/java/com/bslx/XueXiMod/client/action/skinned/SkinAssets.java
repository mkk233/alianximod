package com.bslx.XueXiMod.client.action.skinned;

import java.nio.file.Path;

import org.slf4j.Logger;

import com.bslx.XueXiMod.animation.core.Joint;
import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.model.Armature;
import com.bslx.XueXiMod.animation.runtime.ActionBootstrap;
import com.bslx.XueXiMod.animation.runtime.ActionRegistry;
import com.bslx.XueXiMod.animation.runtime.loader.DevActionLoader;
import com.bslx.XueXiMod.animation.runtime.loader.DevLoadResult;
import com.bslx.XueXiMod.client.action.mesh.MeshLimits;
import com.bslx.XueXiMod.client.action.mesh.MeshLoadResult;
import com.bslx.XueXiMod.client.action.mesh.MeshRepository;
import com.bslx.XueXiMod.client.action.mesh.PlayerMeshKind;
import com.bslx.XueXiMod.client.action.mesh.PlayerSkinMesh;
import com.mojang.logging.LogUtils;

/**
 * 一次客户端资源发布的不可变皮肤快照。
 * 重载在资源应用阶段读盘；渲染只拿当前引用，不在帧里读文件。
 */
public final class SkinAssets {
    public enum Status {
        NOT_LOADED,
        NOT_READY,
        READY,
        DISABLED,
        RIG_MISMATCH
    }

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Object GUARD = new Object();
    private static volatile SkinAssets current = initial();

    private final Status status;
    private final long resourceGeneration;
    private final MeshLoadResult meshes;
    private final RigCredential credential;
    private final Armature matchedArmature;
    private final String diagnostic;

    private SkinAssets(
            Status status,
            long resourceGeneration,
            MeshLoadResult meshes,
            RigCredential credential,
            Armature matchedArmature,
            String diagnostic) {
        this.status = status;
        this.resourceGeneration = resourceGeneration;
        this.meshes = meshes == null ? MeshLoadResult.notLoaded() : meshes;
        this.credential = credential;
        this.matchedArmature = matchedArmature;
        this.diagnostic = diagnostic == null || diagnostic.isBlank() ? "skin 未说明状态" : diagnostic;
    }

    public static SkinAssets current() {
        return current;
    }

    public Status status() {
        return this.status;
    }

    public long resourceGeneration() {
        return this.resourceGeneration;
    }

    public MeshLoadResult meshes() {
        return this.meshes;
    }

    public String diagnostic() {
        return this.diagnostic;
    }

    public boolean usable(PlayerMeshKind kind) {
        return this.status == Status.READY && this.meshes.usable(kind);
    }

    public PlayerSkinMesh mesh(PlayerMeshKind kind) {
        if (!this.usable(kind)) {
            return null;
        }
        return this.meshes.mesh(kind);
    }

    /** 这一份动作骨架是否就是已发布网格对应的 bind。不改快照。 */
    public boolean matches(Armature live) {
        return this.status == Status.READY && this.credential != null && this.credential.sameBind(live);
    }

    /** 注册表里的源骨架换了对象时重新核验。动作自己用了另一套骨架不会清掉快照。 */
    public static void recheckRegistry() {
        synchronized (GUARD) {
            SkinAssets snapshot = current;
            if (snapshot.status != Status.READY || snapshot.credential == null) {
                return;
            }
            Armature registered = ActionRegistry.sourceArmature();
            if (registered == snapshot.matchedArmature) {
                return;
            }
            if (registered != null && snapshot.credential.sameBind(registered)) {
                current = snapshot.withMatched(registered);
                return;
            }
            publishLocked(snapshot.asMismatch(snapshot.resourceGeneration + 1L, "动作骨架已变化，需要重新加载动作后才能启用 skin"));
        }
    }

    /** 资源应用阶段。成功、失败和属性关闭都发布新代次。 */
    public static void publishFromDevProperty() {
        synchronized (GUARD) {
            long generation = current.resourceGeneration + 1L;
            MeshLoadResult loaded;
            try {
                loaded = MeshRepository.reloadFromDevProperty();
            } catch (RuntimeException failure) {
                publishLocked(disabled(generation, "skin 目录读取失败：" + safeReason(failure)));
                return;
            }
            String configured = System.getProperty(ActionBootstrap.DEV_PROPERTY);
            if (configured == null || configured.isBlank() || !loaded.devEnabled()) {
                String diagnostic = loaded.summary();
                if (diagnostic == null || diagnostic.isBlank()) {
                    diagnostic = "开发目录未启用，skin 关闭";
                }
                publishLocked(disabled(generation, diagnostic));
                return;
            }
            DevLoadResult parsed;
            try {
                parsed = DevActionLoader.load(Path.of(configured));
            } catch (RuntimeException failure) {
                publishLocked(disabled(generation, "现有动作解析无法核对骨架：" + safeReason(failure)));
                return;
            }
            Armature parsedArmature = parsed == null ? null : parsed.sourceArmature();
            RigCredential credential = RigCredential.capture(parsedArmature);
            if (credential == null) {
                String summary = parsed == null ? "" : parsed.summaryText();
                String diagnostic = summary == null || summary.isBlank() ? "现有动作解析没有可核对的骨架" : summary;
                publishLocked(disabled(generation, diagnostic));
                return;
            }
            Armature registered = ActionRegistry.sourceArmature();
            if (registered == null) {
                publishLocked(new SkinAssets(
                        Status.NOT_READY,
                        generation,
                        loaded,
                        credential,
                        null,
                        "动作注册表还没有骨架，skin 等待就绪"));
                return;
            }
            if (!credential.sameBind(registered)) {
                publishLocked(disabled(generation, "已加载动作骨架和网格清单骨架不一致，skin 关闭"));
                return;
            }
            publishLocked(new SkinAssets(Status.READY, generation, loaded, credential, registered, loaded.summary()));
        }
    }

    /** 注册表晚于资源出现时只做内存核对，不重读磁盘。 */
    public static void promoteIfReady() {
        synchronized (GUARD) {
            SkinAssets snapshot = current;
            if (snapshot.status != Status.NOT_READY || snapshot.credential == null) {
                return;
            }
            Armature registered = ActionRegistry.sourceArmature();
            if (registered == null) {
                return;
            }
            if (snapshot.credential.sameBind(registered)) {
                publishLocked(new SkinAssets(
                        Status.READY,
                        snapshot.resourceGeneration + 1L,
                        snapshot.meshes,
                        snapshot.credential,
                        registered,
                        snapshot.meshes.summary()));
                return;
            }
            publishLocked(snapshot.asMismatch(
                    snapshot.resourceGeneration + 1L,
                    "动作骨架晚到但和网格清单不一致，skin 关闭"));
        }
    }

    static void restoreInitialForCheck() {
        synchronized (GUARD) {
            current = initial();
            HandFrameCalibration.invalidate();
        }
    }

    static SkinAssets replaceForCheck(SkinAssets snapshot) {
        synchronized (GUARD) {
            publishLocked(snapshot);
            return current;
        }
    }

    static SkinAssets readyForCheck(MeshLoadResult meshes, Armature armature, long generation) {
        RigCredential credential = RigCredential.capture(armature);
        return new SkinAssets(Status.READY, generation, meshes, credential, armature, "check");
    }

    private static SkinAssets initial() {
        return new SkinAssets(Status.NOT_LOADED, 0L, MeshLoadResult.notLoaded(), null, null, "skin 网格尚未加载");
    }

    private static SkinAssets disabled(long generation, String diagnostic) {
        return new SkinAssets(Status.DISABLED, generation, MeshLoadResult.disabled(diagnostic), null, null, diagnostic);
    }

    private SkinAssets asMismatch(long generation, String diagnostic) {
        return new SkinAssets(Status.RIG_MISMATCH, generation, MeshLoadResult.disabled(diagnostic), null, null, diagnostic);
    }

    private SkinAssets withMatched(Armature live) {
        return new SkinAssets(this.status, this.resourceGeneration, this.meshes, this.credential, live, this.diagnostic);
    }

    private static void publishLocked(SkinAssets snapshot) {
        current = snapshot;
        HandFrameCalibration.invalidate();
        LOGGER.info("skin 资源代次 {} 状态 {}：{}", snapshot.resourceGeneration, snapshot.status, snapshot.diagnostic);
    }

    private static String safeReason(RuntimeException failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            return failure.getClass().getSimpleName();
        }
        return message;
    }

    static final class RigCredential {
        private final OpenMatrix4f[] binds;

        private RigCredential(OpenMatrix4f[] binds) {
            this.binds = binds;
        }

        static RigCredential capture(Armature armature) {
            if (!shaped(armature)) {
                return null;
            }
            OpenMatrix4f[] binds = new OpenMatrix4f[MeshLimits.JOINT_NAMES.length];
            for (int jointIndex = 0; jointIndex < MeshLimits.JOINT_NAMES.length; jointIndex++) {
                binds[jointIndex] = new OpenMatrix4f(armature.searchJointById(jointIndex).getLocalTransform());
            }
            return new RigCredential(binds);
        }

        boolean sameBind(Armature live) {
            if (!shaped(live)) {
                return false;
            }
            for (int jointIndex = 0; jointIndex < MeshLimits.JOINT_NAMES.length; jointIndex++) {
                if (!sameMatrix(this.binds[jointIndex], live.searchJointById(jointIndex).getLocalTransform())) {
                    return false;
                }
            }
            return true;
        }

        private static boolean shaped(Armature armature) {
            if (armature == null || armature.getJointNumber() != MeshLimits.JOINT_NAMES.length) {
                return false;
            }
            for (int jointIndex = 0; jointIndex < MeshLimits.JOINT_NAMES.length; jointIndex++) {
                Joint joint = armature.searchJointById(jointIndex);
                if (joint == null || !MeshLimits.JOINT_NAMES[jointIndex].equals(joint.getName())) {
                    return false;
                }
            }
            return true;
        }

        private static boolean sameMatrix(OpenMatrix4f expected, OpenMatrix4f live) {
            return live != null
                    && expected.m00 == live.m00 && expected.m01 == live.m01 && expected.m02 == live.m02 && expected.m03 == live.m03
                    && expected.m10 == live.m10 && expected.m11 == live.m11 && expected.m12 == live.m12 && expected.m13 == live.m13
                    && expected.m20 == live.m20 && expected.m21 == live.m21 && expected.m22 == live.m22 && expected.m23 == live.m23
                    && expected.m30 == live.m30 && expected.m31 == live.m31 && expected.m32 == live.m32 && expected.m33 == live.m33;
        }
    }
}
