package com.bslx.XueXiMod.client.action.mesh;

import java.util.ArrayDeque;

/** 每个线程自己的租约池。嵌套绘制拿不同缓冲，避免互相覆盖。 */
public final class SkinWorkspacePool {
    private static final int POOL_LIMIT = 8;
    private static final ThreadLocal<SkinWorkspacePool> LOCAL = ThreadLocal.withInitial(SkinWorkspacePool::new);

    private final Thread ownerThread = Thread.currentThread();
    private final ArrayDeque<SkinWorkspace> freeWorkspaces = new ArrayDeque<>();
    private final ArrayDeque<SkinWorkspace> activeWorkspaces = new ArrayDeque<>();

    private SkinWorkspacePool() {
    }

    public static SkinWorkspacePool currentThread() {
        return LOCAL.get();
    }

    public SkinLease open(PlayerSkinMesh mesh) {
        if (mesh == null) {
            throw new SkinDiagnostic("mesh-missing", "蒙皮网格缺失");
        }
        if (Thread.currentThread() != this.ownerThread) {
            throw new SkinDiagnostic("thread", "蒙皮缓冲池属于另一条线程");
        }
        SkinWorkspace workspace = this.take(mesh.positionCount(), mesh.normalCount());
        workspace.bind(mesh.positionCount(), mesh.normalCount());
        workspace.leased = true;
        this.activeWorkspaces.addLast(workspace);
        return new SkinLease(this, workspace);
    }

    void release(SkinWorkspace workspace) {
        if (Thread.currentThread() != this.ownerThread) {
            throw new SkinDiagnostic("thread", "不能在别的线程归还蒙皮缓冲");
        }
        if (this.activeWorkspaces.peekLast() != workspace) {
            throw new SkinDiagnostic("lease-order", "嵌套的蒙皮缓冲必须后开先关");
        }
        this.activeWorkspaces.removeLast();
        workspace.dropDeformPreparation();
        workspace.leased = false;
        if (this.freeWorkspaces.size() < POOL_LIMIT) {
            this.freeWorkspaces.addLast(workspace);
        }
    }

    private SkinWorkspace take(int positionCount, int normalCount) {
        SkinWorkspace matched = null;
        int scanned = this.freeWorkspaces.size();
        for (int scanIndex = 0; scanIndex < scanned; scanIndex++) {
            SkinWorkspace candidate = this.freeWorkspaces.removeFirst();
            if (matched == null && candidate.fits(positionCount, normalCount)) {
                matched = candidate;
            } else {
                this.freeWorkspaces.addLast(candidate);
            }
        }
        if (matched != null) {
            return matched;
        }
        if (this.activeWorkspaces.size() >= POOL_LIMIT) {
            throw new SkinDiagnostic("workspace-limit", "嵌套的蒙皮缓冲超过上限");
        }
        int positionCapacity = positionCount < 1 ? 1 : positionCount;
        int normalCapacity = normalCount < 1 ? 1 : normalCount;
        return new SkinWorkspace(this.ownerThread, positionCapacity, normalCapacity);
    }
}
