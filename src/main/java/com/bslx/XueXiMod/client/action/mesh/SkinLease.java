package com.bslx.XueXiMod.client.action.mesh;

/** 当前线程上的一层蒙皮缓冲。嵌套时必须后开的先关。 */
public final class SkinLease implements AutoCloseable {
    private final SkinWorkspacePool pool;
    private final SkinWorkspace workspace;
    private boolean open;

    SkinLease(SkinWorkspacePool pool, SkinWorkspace workspace) {
        this.pool = pool;
        this.workspace = workspace;
        this.open = true;
    }

    public Thread ownerThread() {
        return this.workspace.ownerThread;
    }

    public boolean isOpen() {
        return this.open;
    }

    public int normalEvaluations() {
        this.requireOpen();
        return this.workspace.normalEvaluations;
    }

    int deformPreparations() {
        this.requireOpen();
        return this.workspace.deformPreparations;
    }

    SkinWorkspace workspace() {
        this.requireOpen();
        return this.workspace;
    }

    @Override
    public void close() {
        if (!this.open) {
            return;
        }
        this.open = false;
        this.pool.release(this.workspace);
    }

    private void requireOpen() {
        if (!this.open) {
            throw new SkinDiagnostic("lease", "蒙皮租约已经关闭");
        }
    }
}
