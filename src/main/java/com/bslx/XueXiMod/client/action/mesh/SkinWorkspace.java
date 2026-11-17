package com.bslx.XueXiMod.client.action.mesh;

/** 一条渲染租约独占的原始数组。不属于任何玩家的静态共享缓冲。 */
final class SkinWorkspace {
    final Thread ownerThread;
    final float[] deformedPositions;
    final float[] deformedNormals;
    final byte[] normalReady;
    final float[] scratchPoint = new float[3];
    final float[] placedNormal = new float[3];
    final float[] wholeElements = new float[16];
    final float[] wholeNormal = new float[9];
    int positionCount;
    int normalCount;
    int normalEvaluations;
    int deformPreparations;
    boolean leased;
    boolean transformWhole;
    boolean deformPrepared;
    PlayerSkinMesh preparedMesh;
    SkinPalette preparedPalette;

    SkinWorkspace(Thread ownerThread, int positionCapacity, int normalCapacity) {
        this.ownerThread = ownerThread;
        int pairs = positionCapacity * normalCapacity;
        this.deformedPositions = new float[positionCapacity * 3];
        this.deformedNormals = new float[pairs * 3];
        this.normalReady = new byte[pairs];
    }

    boolean fits(int positionCount, int normalCount) {
        return positionCount >= 0 && normalCount >= 0
                && positionCount <= this.deformedPositions.length / 3
                && (long) positionCount * (long) normalCount <= this.normalReady.length;
    }

    void bind(int positionCount, int normalCount) {
        this.dropDeformPreparation();
        this.positionCount = positionCount;
        this.normalCount = normalCount;
        this.normalEvaluations = 0;
        this.clearNormalReady(positionCount * normalCount);
    }

    boolean reuseDeform(PlayerSkinMesh mesh, SkinPalette palette) {
        return this.leased && this.deformPrepared && this.preparedMesh == mesh && this.preparedPalette == palette;
    }

    void invalidateDeform() {
        this.deformPrepared = false;
        this.preparedMesh = null;
        this.preparedPalette = null;
    }

    void publishDeform(PlayerSkinMesh mesh, SkinPalette palette) {
        this.preparedMesh = mesh;
        this.preparedPalette = palette;
        this.deformPrepared = true;
        this.deformPreparations++;
    }

    void dropDeformPreparation() {
        this.invalidateDeform();
        this.deformPreparations = 0;
        this.clearNormalReady(this.positionCount * this.normalCount);
    }

    private void clearNormalReady(int usedPairs) {
        for (int pairIndex = 0; pairIndex < usedPairs; pairIndex++) {
            this.normalReady[pairIndex] = 0;
        }
    }
}
