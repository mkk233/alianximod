package com.bslx.XueXiMod.shu.technique.release.clone.core;

import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CancelReason;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeStructureLimits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

/** 保守扫掠。不加载区块，不沿竖直方向搜索落点，也不把玩家当成可移动目标。 */
final class CloneBodySweep {
    private static final double SAMPLE_EDGE = 1.0E-7D;
    private static final double SUPPORT_EPSILON = 1.0E-3D;
    private static final double SAME_SPOT = 1.0E-8D;

    private CloneBodySweep() {
    }

    static boolean movableOrdinary(@Nullable CloneRuntimeState state) {
        if (state == null || state.life != CloneLifeState.ACTIVE) {
            return false;
        }
        if (state.spec.kind() != CloneKind.NORMAL || !state.spec.externalCooperation().enabled()) {
            return false;
        }
        if (state.spec.terminationPolicy() == TerminationPolicy.BREAK_ON_TRIGGER || state.spec.deploymentId() != null) {
            return false;
        }
        if (state.deploymentLease != null || state.cosmeticOnly || state.capturedSnapshot != null) {
            return false;
        }
        CloneEntity actor = state.actor;
        return actor != null && actor.isAlive() && !actor.isRemoved() && !actor.capturedCosmetic();
    }

    @Nullable
    static CancelReason pathProblem(ServerLevel level, CloneEntity actor, Vec3 targetFeet, double distanceLimit) {
        if (level == null || actor == null || !finite(targetFeet)) {
            return CancelReason.UNSAFE_STANCE;
        }
        Vec3 originFeet = actor.position();
        if (!finite(originFeet)) {
            return CancelReason.UNSAFE_STANCE;
        }
        double distanceSquared = originFeet.distanceToSqr(targetFeet);
        if (!Double.isFinite(distanceLimit) || distanceLimit < 0.0D
                || distanceLimit > CooperativeStructureLimits.REPOSITION_DISTANCE_CAP) {
            return CancelReason.VALIDATION_FAILED;
        }
        double allowedSquared = distanceLimit * distanceLimit;
        if (distanceLimit == 0.0D) {
            allowedSquared = SAME_SPOT;
        }
        if (distanceSquared > allowedSquared + SAMPLE_EDGE) {
            return CancelReason.ACTOR_TOO_FAR;
        }
        return stepProblem(level, actor, originFeet, targetFeet);
    }

    @Nullable
    static CancelReason stepProblem(ServerLevel level, CloneEntity actor, Vec3 originFeet, Vec3 targetFeet) {
        if (!finite(originFeet) || !finite(targetFeet)) {
            return CancelReason.UNSAFE_STANCE;
        }
        int sampleCount = CooperativeStructureLimits.SWEEP_SAMPLE_CAP;
        for (int sampleIndex = 1; sampleIndex <= sampleCount; sampleIndex++) {
            double fraction = sampleIndex / (double) sampleCount;
            Vec3 sampleFeet = originFeet.lerp(targetFeet, fraction);
            CancelReason sampleProblem = placeProblem(level, actor, sampleFeet, true);
            if (sampleProblem != null) {
                return sampleProblem;
            }
        }
        return volumeProblem(level, actor, originFeet, targetFeet);
    }

    @Nullable
    private static CancelReason volumeProblem(ServerLevel level, CloneEntity actor, Vec3 originFeet, Vec3 targetFeet) {
        AABB swept = bodyBox(actor, originFeet).minmax(bodyBox(actor, targetFeet));
        CancelReason space = spaceProblem(level, swept);
        if (space != null) {
            return space;
        }
        CancelReason blocks = blockProblem(level, swept);
        if (blocks != null) {
            return blocks;
        }
        if (crowded(level, actor, swept)) {
            return CancelReason.ACTOR_BLOCKED;
        }
        return null;
    }

    @Nullable
    private static CancelReason placeProblem(ServerLevel level, CloneEntity actor, Vec3 feet, boolean requireSupport) {
        AABB box = bodyBox(actor, feet);
        CancelReason space = spaceProblem(level, box);
        if (space != null) {
            return space;
        }
        CancelReason blocks = blockProblem(level, box);
        if (blocks != null) {
            return blocks;
        }
        if (crowded(level, actor, box)) {
            return CancelReason.ACTOR_BLOCKED;
        }
        if (requireSupport) {
            return supportProblem(level, feet);
        }
        return null;
    }

    @Nullable
    private static CancelReason spaceProblem(ServerLevel level, AABB box) {
        if (box.minY < level.getMinBuildHeight() || box.maxY > level.getMaxBuildHeight()) {
            return CancelReason.UNSAFE_STANCE;
        }
        WorldBorder border = level.getWorldBorder();
        if (!border.isWithinBounds(box.minX, box.minZ) || !border.isWithinBounds(box.maxX, box.minZ)
                || !border.isWithinBounds(box.minX, box.maxZ) || !border.isWithinBounds(box.maxX, box.maxZ)) {
            return CancelReason.UNSAFE_STANCE;
        }
        BlockPos lowCorner = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos highCorner = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        if (!level.isInWorldBounds(lowCorner) || !level.isInWorldBounds(highCorner)) {
            return CancelReason.UNSAFE_STANCE;
        }
        if (!chunksLoaded(level, box)) {
            return CancelReason.REGION_UNLOADED;
        }
        return null;
    }

    @Nullable
    private static CancelReason blockProblem(ServerLevel level, AABB box) {
        AABB collision = collisionBox(box);
        if (!chunksLoaded(level, collision)) {
            return CancelReason.REGION_UNLOADED;
        }
        int west = Mth.floor(collision.minX);
        int east = Mth.floor(collision.maxX - SAMPLE_EDGE);
        int bottom = Mth.floor(collision.minY);
        int crown = Mth.floor(collision.maxY - SAMPLE_EDGE);
        int north = Mth.floor(collision.minZ);
        int south = Mth.floor(collision.maxZ - SAMPLE_EDGE);
        if (east < west || crown < bottom || south < north) {
            return CancelReason.UNSAFE_STANCE;
        }
        for (int cursorX = west; cursorX <= east; cursorX++) {
            for (int cursorY = bottom; cursorY <= crown; cursorY++) {
                for (int cursorZ = north; cursorZ <= south; cursorZ++) {
                    BlockPos cursor = new BlockPos(cursorX, cursorY, cursorZ);
                    if (!level.hasChunkAt(cursor)) {
                        return CancelReason.REGION_UNLOADED;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (hazardous(state)) {
                        return CancelReason.UNSAFE_STANCE;
                    }
                }
            }
        }
        for (VoxelShape shape : level.getBlockCollisions(null, collision)) {
            if (!shape.isEmpty()) {
                return CancelReason.ACTOR_BLOCKED;
            }
        }
        return null;
    }

    @Nullable
    private static CancelReason supportProblem(ServerLevel level, Vec3 feet) {
        BlockPos ground = BlockPos.containing(feet.x, feet.y - SUPPORT_EPSILON, feet.z);
        if (!level.hasChunkAt(ground) || !level.isInWorldBounds(ground)) {
            return CancelReason.REGION_UNLOADED;
        }
        BlockState groundState = level.getBlockState(ground);
        if (hazardous(groundState) || !groundState.getFluidState().isEmpty()) {
            return CancelReason.UNSAFE_STANCE;
        }
        if (!groundState.isFaceSturdy(level, ground, Direction.UP)) {
            return CancelReason.UNSAFE_STANCE;
        }
        return null;
    }

    private static boolean crowded(ServerLevel level, CloneEntity actor, AABB box) {
        if (!chunksLoaded(level, box)) {
            return true;
        }
        return !level.getEntities(actor, box, other -> crowds(actor, other)).isEmpty();
    }

    private static boolean crowds(CloneEntity actor, Entity other) {
        if (other == null || other == actor || other.isRemoved() || other.isSpectator() || !other.isAlive()) {
            return false;
        }
        if (other instanceof LivingEntity) {
            return true;
        }
        return other.canBeCollidedWith();
    }

    private static boolean chunksLoaded(ServerLevel level, AABB box) {
        int west = Mth.floor(box.minX);
        int east = Mth.floor(box.maxX - SAMPLE_EDGE);
        int bottom = Mth.floor(box.minY);
        int crown = Mth.floor(box.maxY - SAMPLE_EDGE);
        int north = Mth.floor(box.minZ);
        int south = Mth.floor(box.maxZ - SAMPLE_EDGE);
        for (int cursorX = west; cursorX <= east; cursorX++) {
            for (int cursorZ = north; cursorZ <= south; cursorZ++) {
                BlockPos lower = new BlockPos(cursorX, bottom, cursorZ);
                BlockPos upper = new BlockPos(cursorX, crown, cursorZ);
                if (!level.hasChunkAt(lower) || !level.hasChunkAt(upper)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static AABB bodyBox(CloneEntity actor, Vec3 feet) {
        double shiftX = feet.x - actor.getX();
        double shiftY = feet.y - actor.getY();
        double shiftZ = feet.z - actor.getZ();
        return actor.getBoundingBox().move(shiftX, shiftY, shiftZ);
    }

    private static AABB collisionBox(AABB box) {
        AABB shrunk = box.deflate(SAMPLE_EDGE);
        if (shrunk.minX >= shrunk.maxX || shrunk.minY >= shrunk.maxY || shrunk.minZ >= shrunk.maxZ) {
            return box;
        }
        return shrunk;
    }

    private static boolean hazardous(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (!state.getFluidState().isEmpty()) {
            return true;
        }
        if (state.is(BlockTags.FIRE) || state.is(BlockTags.CAMPFIRES)) {
            return true;
        }
        return state.is(Blocks.LAVA)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.WITHER_ROSE)
                || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.POINTED_DRIPSTONE);
    }

    private static boolean finite(@Nullable Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }

    /** 源或目的站位的完整身体盒、边界、支撑和近身实体。不扫中间通道。 */
    @Nullable
    static CancelReason endpointProblem(ServerLevel level, CloneEntity actor, Vec3 feet) {
        if (level == null || actor == null || !finite(feet)) {
            return CancelReason.UNSAFE_STANCE;
        }
        return placeProblem(level, actor, feet, true);
    }

    static boolean crowdsBody(CloneEntity actor, Entity other) {
        return crowds(actor, other);
    }

    static boolean hazardousBlock(BlockState state) {
        return state != null && hazardous(state);
    }

    /**
     * 把身体盒和脚下支撑格并进去重集合。
     * 返回新加入的格子数；并入后会超过 cap 时不修改集合并返回 -1。
     */
    static int absorbFootprint(CloneEntity actor, Vec3 feet, Set<Long> cells, int cap) {
        if (actor == null || !finite(feet) || cells == null || cap < 1) {
            return -1;
        }
        AABB box = collisionBox(bodyBox(actor, feet));
        int west = Mth.floor(box.minX);
        int east = Mth.floor(box.maxX - SAMPLE_EDGE);
        int bottom = Mth.floor(box.minY);
        int crown = Mth.floor(box.maxY - SAMPLE_EDGE);
        int north = Mth.floor(box.minZ);
        int south = Mth.floor(box.maxZ - SAMPLE_EDGE);
        if (east - west > 8 || crown - bottom > 8 || south - north > 8) {
            return -1;
        }
        LinkedHashSet<Long> fresh = new LinkedHashSet<>();
        for (int cursorX = west; cursorX <= east; cursorX++) {
            for (int cursorY = bottom; cursorY <= crown; cursorY++) {
                for (int cursorZ = north; cursorZ <= south; cursorZ++) {
                    if (!rememberCell(cells, fresh, BlockPos.asLong(cursorX, cursorY, cursorZ), cap)) {
                        return -1;
                    }
                }
            }
        }
        BlockPos ground = BlockPos.containing(feet.x, feet.y - SUPPORT_EPSILON, feet.z);
        if (!rememberCell(cells, fresh, ground.asLong(), cap)) {
            return -1;
        }
        cells.addAll(fresh);
        return fresh.size();
    }

    private static boolean rememberCell(Set<Long> cells, Set<Long> fresh, long packed, int cap) {
        if (cells.contains(packed) || fresh.contains(packed)) {
            return true;
        }
        if ((long) cells.size() + fresh.size() + 1L > cap) {
            return false;
        }
        fresh.add(packed);
        return true;
    }
}
