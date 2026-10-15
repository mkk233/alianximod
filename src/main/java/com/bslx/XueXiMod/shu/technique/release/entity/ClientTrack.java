package com.bslx.XueXiMod.shu.technique.release.entity;

import net.minecraft.world.entity.Entity;

/**
 * 非生物实体不会自己消化移动包。记下包到达前的位置，tick 之后写回插值起点，
 * 渲染才能在两拍之间滑过去，而不是瞬移。
 */
public final class ClientTrack {
    private double fromX;
    private double fromY;
    private double fromZ;
    private float fromYRot;
    private float fromXRot;
    private boolean pending;

    public void onClientMove(Entity entity, double x, double y, double z, float yRot, float xRot) {
        if (!this.pending) {
            this.fromX = entity.getX();
            this.fromY = entity.getY();
            this.fromZ = entity.getZ();
            this.fromYRot = entity.getYRot();
            this.fromXRot = entity.getXRot();
            this.pending = true;
        }
        entity.setPos(x, y, z);
        entity.setYRot(yRot);
        entity.setXRot(xRot);
    }

    public void afterClientTick(Entity entity) {
        if (!this.pending) {
            return;
        }
        entity.xo = this.fromX;
        entity.yo = this.fromY;
        entity.zo = this.fromZ;
        entity.yRotO = this.fromYRot;
        entity.xRotO = this.fromXRot;
        this.pending = false;
    }
}
