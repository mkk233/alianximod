package com.bslx.XueXiMod.shu.technique.release.parting;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

/**
 * 激活时冻结的斩击。之后不跟随镜头。通道全宽固定 3，与攻击宽无关。
 */
public final class PartingGeometrySnapshot {
    static final double CHANNEL_HALF_WIDTH = 1.5;

    private final Vec3 center;
    private final Vec3 horizontalForward;
    private final double halfLength;
    private final double lowerCut;
    private final double upperCut;

    public PartingGeometrySnapshot(Vec3 center, Vec3 horizontalForward, double halfLength, double lowerCut, double upperCut) {
        this.center = center;
        this.horizontalForward = horizontalForward;
        this.halfLength = halfLength;
        this.lowerCut = lowerCut;
        this.upperCut = upperCut;
    }

    public static PartingGeometrySnapshot capture(TechniqueHitEntity hitEntity) {
        Vec3 facing = hitEntity.forward() == null ? new Vec3(0.0, 0.0, 1.0) : hitEntity.forward();
        double horizontal = Math.sqrt(facing.x * facing.x + facing.z * facing.z);
        Vec3 flat = horizontal < 1.0E-4
                ? new Vec3(0.0, 0.0, 1.0)
                : new Vec3(facing.x / horizontal, 0.0, facing.z / horizontal);
        Vec3 center = hitEntity.position();
        double slashLength = Math.max(hitEntity.length(), 0.0);
        double slashHeight = Math.max(hitEntity.height(), 0.0);
        return new PartingGeometrySnapshot(center, flat, slashLength * 0.5, center.y - slashHeight * 0.5, center.y + slashHeight * 0.5);
    }

    public Vec3 center() {
        return center;
    }

    public Vec3 horizontalForward() {
        return horizontalForward;
    }

    public double halfLength() {
        return halfLength;
    }

    public double lowerCut() {
        return lowerCut;
    }

    public double upperCut() {
        return upperCut;
    }

    /** 方块水平中心落在斩击有限矩形内。全宽 3。 */
    public boolean containsColumn(int blockX, int blockZ) {
        double centerX = blockX + 0.5;
        double centerZ = blockZ + 0.5;
        double offsetX = centerX - center.x;
        double offsetZ = centerZ - center.z;
        double along = offsetX * horizontalForward.x + offsetZ * horizontalForward.z;
        double across = offsetX * -horizontalForward.z + offsetZ * horizontalForward.x;
        return Math.abs(along) <= halfLength + 1.0E-4 && Math.abs(across) <= CHANNEL_HALF_WIDTH + 1.0E-4;
    }

    public boolean overlapsColumn(BlockPos cell) {
        return containsColumn(cell.getX(), cell.getZ());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("centerX", center.x);
        tag.putDouble("centerY", center.y);
        tag.putDouble("centerZ", center.z);
        tag.putDouble("forwardX", horizontalForward.x);
        tag.putDouble("forwardZ", horizontalForward.z);
        tag.putDouble("halfLength", halfLength);
        tag.putDouble("lowerCut", lowerCut);
        tag.putDouble("upperCut", upperCut);
        return tag;
    }

    public static PartingGeometrySnapshot load(CompoundTag tag) {
        Vec3 center = new Vec3(tag.getDouble("centerX"), tag.getDouble("centerY"), tag.getDouble("centerZ"));
        Vec3 forward = new Vec3(tag.getDouble("forwardX"), 0.0, tag.getDouble("forwardZ"));
        if (forward.lengthSqr() < 1.0E-6) {
            forward = new Vec3(0.0, 0.0, 1.0);
        } else {
            forward = forward.normalize();
        }
        return new PartingGeometrySnapshot(center, forward, tag.getDouble("halfLength"), tag.getDouble("lowerCut"), tag.getDouble("upperCut"));
    }
}
