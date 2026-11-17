package com.bslx.XueXiMod.client.action.mesh;

/** 一次开发目录读取的结果。某一档失败只禁用该档，不代表动作 runtime 失败。 */
public final class MeshLoadResult {
    private final boolean devEnabled;
    private final boolean playbackAccepted;
    private final PlayerSkinMesh normalMesh;
    private final PlayerSkinMesh slimMesh;
    private final String normalDiagnostic;
    private final String slimDiagnostic;
    private final String summaryText;

    private MeshLoadResult(
            boolean devEnabled,
            PlayerSkinMesh normalMesh,
            PlayerSkinMesh slimMesh,
            String normalDiagnostic,
            String slimDiagnostic,
            String summaryText) {
        this.devEnabled = devEnabled;
        this.playbackAccepted = false;
        this.normalMesh = normalMesh;
        this.slimMesh = slimMesh;
        this.normalDiagnostic = normalDiagnostic == null ? "" : normalDiagnostic;
        this.slimDiagnostic = slimDiagnostic == null ? "" : slimDiagnostic;
        this.summaryText = summaryText == null ? "" : summaryText;
    }

    public static MeshLoadResult notLoaded() {
        return new MeshLoadResult(false, null, null, "尚未加载", "尚未加载", "skin 网格尚未加载");
    }

    public static MeshLoadResult disabled(String diagnostic) {
        String text = diagnostic == null || diagnostic.isBlank() ? "skin 后端已关闭" : diagnostic;
        return new MeshLoadResult(false, null, null, text, text, text);
    }

    public static MeshLoadResult loaded(
            PlayerSkinMesh normalMesh,
            PlayerSkinMesh slimMesh,
            String normalDiagnostic,
            String slimDiagnostic) {
        String summary = "normal=" + describe(normalMesh, normalDiagnostic)
                + " slim=" + describe(slimMesh, slimDiagnostic)
                + " playbackAccepted=false";
        return new MeshLoadResult(true, normalMesh, slimMesh, normalDiagnostic, slimDiagnostic, summary);
    }

    public boolean devEnabled() {
        return this.devEnabled;
    }

    public boolean playbackAccepted() {
        return this.playbackAccepted;
    }

    public boolean usable(PlayerMeshKind kind) {
        return this.mesh(kind) != null;
    }

    public PlayerSkinMesh mesh(PlayerMeshKind kind) {
        if (kind == PlayerMeshKind.NORMAL) {
            return this.normalMesh;
        }
        if (kind == PlayerMeshKind.SLIM) {
            return this.slimMesh;
        }
        return null;
    }

    public String diagnostic(PlayerMeshKind kind) {
        if (kind == PlayerMeshKind.SLIM) {
            return this.slimDiagnostic;
        }
        return this.normalDiagnostic;
    }

    public String summary() {
        return this.summaryText;
    }

    private static String describe(PlayerSkinMesh mesh, String diagnostic) {
        if (mesh == null) {
            return "disabled:" + diagnostic;
        }
        return "positions=" + mesh.positionCount()
                + " normals=" + mesh.normalCount()
                + " uvs=" + mesh.textureCount()
                + " influences=" + mesh.influenceCount()
                + " corners=" + mesh.cornerCount()
                + " triangles=" + mesh.triangleCount()
                + " parts=" + mesh.partCount();
    }
}
