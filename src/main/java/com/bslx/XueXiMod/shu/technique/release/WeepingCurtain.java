package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 垂丝这一帘水。上沿由弹道单独记下，帘身每拍从当前上沿沿世界竖直轴下落。
 * 起流从上沿向下长，满流保持整段，排空时上沿往下收。本拍结果留给 {@link #active}。
 * 帘心可以拿去摆放命中盒，下一拍仍读记下的上沿，不把帘心当成新的出水口。
 */
public final class WeepingCurtain {
    public static final String FUNCTION_ID = "water_weeping";
    /** 出水口在释放点上方。水从这里往下倒。 */
    public static final double LIP_LIFT = 4.0;
    /**
     * 足迹覆盖到的方块格上限。超出就整段留空并记下这个边界，不把采样间距拉大。
     */
    public static final int FOOTPRINT_CELL_BUDGET = 4096;

    private static final double REFERENCE_WIDTH = 0.5;
    private static final double BASE_THICK = 0.35;
    private static final double NARROW_SCALE = 0.2;
    private static final double PLANE_SLOP = 1.0E-4;
    private static final double SHAPE_SLOP = 1.0E-4;
    private static final double EDGE_SLOP = 1.0E-7;

    /** 弱键只留水段值，不把世界留在缓存里。 */
    private static final Map<TechniqueHitEntity, WaterSegment> CACHED_SEGMENTS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<TechniqueHitEntity, WaterSegment> PREVIOUS_SEGMENTS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<TechniqueHitEntity, Vec3> LIP_ANCHORS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile long notedCells;
    private static volatile boolean notedOverBudget;

    private WeepingCurtain() {
    }

    public static void register() {
        TrajectorySampler.registerVolume(FUNCTION_ID, WeepingCurtain::fit);
    }

    /** 记下这一拍的上沿。帘心摆放不会改这个点。 */
    public static void moveLip(TechniqueHitEntity entity, Vec3 lip) {
        if (entity == null || lip == null) {
            return;
        }
        if (!Double.isFinite(lip.x) || !Double.isFinite(lip.y) || !Double.isFinite(lip.z)) {
            return;
        }
        LIP_ANCHORS.put(entity, lip);
    }

    /**
     * 按这一拍记下的上沿写下水段。没有上沿时，领域生成点不再额外抬高，其余仍用源点正上方。
     * {@code groundPoint} 是旧弹道在释放点正下方的射线，实体这时已经被挪走，不用它改足端。
     */
    public static void fit(TechniqueHitEntity entity, ServerLevel level, Vec3 groundPoint) {
        if (entity == null) {
            return;
        }
        Vec3 lip = anchoredLip(entity);
        double across = sheetAcross(entity.width());
        double thick = sheetThick(entity.width());
        FootClip clip = probeTerrain(level, lip, entity.forward(), across, thick);
        WaterSegment segment = segmentFor(clip, lip, entity.forward(), across, thick, entity.age(), entity.lifetime());
        WaterSegment prior = CACHED_SEGMENTS.get(entity);
        if (prior != null) {
            PREVIOUS_SEGMENTS.put(entity, prior);
        }
        CACHED_SEGMENTS.put(entity, segment);
        entity.placeAt(segment.center(), segment.ahead());
        entity.resizeVolume(segment.thick(), segment.across(), segment.activeHeight());
        if (level != null && splashes(entity.age(), clip, segment)) {
            Vec3 splashAt = footPoint(segment);
            level.sendParticles(ParticleTypes.SPLASH, splashAt.x, splashAt.y, splashAt.z, 6, 0.15, 0.05, 0.15, 0.02);
        }
    }

    /** 返回本拍 {@link #fit} 已经算好的水段。没有缓存时给有限空段，不再探地。 */
    public static WaterSegment active(TechniqueHitEntity entity) {
        if (entity == null) {
            return WaterSegment.EMPTY;
        }
        WaterSegment cached = CACHED_SEGMENTS.get(entity);
        if (cached == null) {
            return WaterSegment.EMPTY;
        }
        return cached;
    }

    /** 实体移除时丢掉缓存。 */
    public static void forget(TechniqueHitEntity entity) {
        if (entity == null) {
            return;
        }
        CACHED_SEGMENTS.remove(entity);
        PREVIOUS_SEGMENTS.remove(entity);
        LIP_ANCHORS.remove(entity);
    }

    /**
     * 当前帘和上一帘的世界包围盒。不是垂丝，或这一拍还没有水时返回空。
     */
    public static AABB queryBounds(TechniqueHitEntity entity) {
        if (!isCurtain(entity)) {
            return null;
        }
        AABB current = sheetBounds(CACHED_SEGMENTS.get(entity));
        AABB previous = sheetBounds(PREVIOUS_SEGMENTS.get(entity));
        if (current == null) {
            return previous;
        }
        if (previous == null) {
            return current;
        }
        return current.minmax(previous);
    }

    /**
     * 身体碰到当前帘、上一帘，或两拍之间扫过的水，都算命中。
     * 精确盒子只认目标中心时，这里补上整面水帘。
     */
    public static boolean covers(TechniqueHitEntity entity, LivingEntity target) {
        if (!isCurtain(entity) || target == null) {
            return false;
        }
        AABB body = target.getBoundingBox();
        WaterSegment current = CACHED_SEGMENTS.get(entity);
        WaterSegment previous = PREVIOUS_SEGMENTS.get(entity);
        if (sheetOverlaps(current, body) || sheetOverlaps(previous, body)) {
            return true;
        }
        if (current == null || previous == null) {
            return false;
        }
        return sweptOverlap(previous, current, body);
    }

    private static Vec3 anchoredLip(TechniqueHitEntity entity) {
        Vec3 anchored = LIP_ANCHORS.get(entity);
        if (anchored != null) {
            return anchored;
        }
        Vec3 origin = entity.origin();
        if (entity.automaticLifetime()) {
            if (origin == null || !Double.isFinite(origin.x) || !Double.isFinite(origin.y) || !Double.isFinite(origin.z)) {
                return Vec3.ZERO;
            }
            return origin;
        }
        return lipOf(origin);
    }

    public static Vec3 lipOf(Vec3 origin) {
        if (origin == null || !Double.isFinite(origin.x) || !Double.isFinite(origin.y) || !Double.isFinite(origin.z)) {
            return new Vec3(0.0, LIP_LIFT, 0.0);
        }
        return new Vec3(origin.x, origin.y + LIP_LIFT, origin.z);
    }

    /** 当前实体宽度，含聚能。非法宽度回到基准 0.5。 */
    public static double sheetAcross(double entityWidth) {
        if (!Double.isFinite(entityWidth) || entityWidth <= 0.0) {
            return REFERENCE_WIDTH;
        }
        return entityWidth;
    }

    /** 厚度仍按宽度相对 0.5 缩放。过窄或非法时用基准厚度，不把宽度一起改掉。 */
    public static double sheetThick(double entityWidth) {
        double crossScale = entityWidth / REFERENCE_WIDTH;
        if (!Double.isFinite(crossScale) || crossScale < NARROW_SCALE) {
            crossScale = 1.0;
        }
        return BASE_THICK * crossScale;
    }

    /** 已知足端后的相位。不探地。缺省寿命仍是基准 40 拍。 */
    public static WaterSegment flowSegment(Vec3 lip, Vec3 look, double footDrop, boolean floorFoot,
                                           double across, double thick, int age) {
        return flowSegment(lip, look, footDrop, floorFoot, across, thick, age, WeepingSettings.LIFETIME);
    }

    /** 已知足端后的相位。排空起点是这一份实例的总寿命减固定排空拍数。 */
    public static WaterSegment flowSegment(Vec3 lip, Vec3 look, double footDrop, boolean floorFoot,
                                           double across, double thick, int age, int lifeTicks) {
        Vec3 mouth = lip == null ? Vec3.ZERO : lip;
        int fillTicks = WeepingSettings.FILL_TICKS;
        if (fillTicks < 1) {
            fillTicks = 1;
        }
        int drainStart = WeepingSettings.drainStartFor(lifeTicks);
        int drainWindow = lifeTicks - drainStart;
        if (drainWindow < 1) {
            drainWindow = 1;
        }
        int lived = Math.max(age, 0);
        double head = lived >= fillTicks ? 1.0 : lived / (double) fillTicks;
        double tail = lived <= drainStart ? 0.0 : (lived - drainStart) / (double) drainWindow;
        if (tail > 1.0) {
            tail = 1.0;
        }
        if (tail > head) {
            tail = head;
        }
        double safeFoot = footDrop;
        if (!Double.isFinite(safeFoot) || safeFoot < 0.0) {
            safeFoot = 0.0;
        }
        double upperDrop = safeFoot * tail;
        double lowerDrop = safeFoot * head;
        double openSpan = lowerDrop - upperDrop;
        boolean flowing = openSpan > WaterSegment.PRESENT_GAP;
        double middleDrop = (upperDrop + lowerDrop) * 0.5;
        Vec3 center = new Vec3(mouth.x, mouth.y - middleDrop, mouth.z);
        return new WaterSegment(mouth, center, look, upperDrop, lowerDrop, safeFoot, across, thick, flowing, floorFoot);
    }

    /** 源被堵住或预算不够时不给水体。其余走基准寿命相位。 */
    public static WaterSegment segmentFor(FootClip clip, Vec3 lip, Vec3 look, double across, double thick, int age) {
        return segmentFor(clip, lip, look, across, thick, age, WeepingSettings.LIFETIME);
    }

    /** 源被堵住或预算不够时不给水体。其余按实例寿命走相位。 */
    public static WaterSegment segmentFor(FootClip clip, Vec3 lip, Vec3 look, double across, double thick, int age,
                                          int lifeTicks) {
        if (clip == null || clip.sourceInside() || clip.overBudget() || !Double.isFinite(clip.footDrop())) {
            return WaterSegment.absent(lip, look, across, thick);
        }
        return flowSegment(lip, look, clip.footDrop(), clip.floorFoot(), across, thick, age, lifeTicks);
    }

    /** 只有水头到达真实朝上地面时才溅水。 */
    public static boolean splashes(int age, FootClip clip, WaterSegment segment) {
        return clip != null
                && segment != null
                && !clip.sourceInside()
                && !clip.overBudget()
                && clip.floorFoot()
                && segment.present()
                && age == WeepingSettings.FILL_TICKS;
    }

    /** 足端在唇口正下方，不往前倾。 */
    public static Vec3 footPoint(WaterSegment segment) {
        if (segment == null) {
            return Vec3.ZERO;
        }
        return new Vec3(segment.lip().x, segment.lip().y - segment.footDrop(), segment.lip().z);
    }

    public static long footprintCellCount(Vec3 lip, Vec3 look, double across, double thick) {
        PreparedColumn prepared = prepare(lip, look, across, thick);
        if (prepared == null) {
            return 0L;
        }
        return prepared.cellCount();
    }

    public static long notedFootprintCells() {
        return notedCells;
    }

    public static boolean footprintBudgetRejected() {
        return notedOverBudget;
    }

    /** 用已经取到的碰撞盒裁切。盒子要是世界坐标。超预算时不读这些盒子。 */
    public static FootClip clipFootprint(Vec3 lip, Vec3 look, double across, double thick, List<AABB> boxes) {
        PreparedColumn prepared = prepare(lip, look, across, thick);
        if (prepared == null) {
            noteFootprint(0L, true);
            return FootClip.budgetStop(0L);
        }
        if (prepared.cellCount() > FOOTPRINT_CELL_BUDGET) {
            noteFootprint(prepared.cellCount(), true);
            return FootClip.budgetStop(prepared.cellCount());
        }
        noteFootprint(prepared.cellCount(), false);
        return clipBoxes(prepared, boxes);
    }

    /**
     * 从世界读取碰撞形状后再裁切。形状以方块自己的格为原点，这里平移一次。
     * 只读方块，不改地形。没有世界或格子超预算时不扫描。
     */
    public static FootClip probeTerrain(ServerLevel level, Vec3 lip, Vec3 look, double across, double thick) {
        PreparedColumn prepared = prepare(lip, look, across, thick);
        if (prepared == null || level == null || prepared.cellCount() > FOOTPRINT_CELL_BUDGET) {
            long cellCount = prepared == null ? 0L : prepared.cellCount();
            noteFootprint(cellCount, true);
            return FootClip.budgetStop(cellCount);
        }
        noteFootprint(prepared.cellCount(), false);
        return clipBoxes(prepared, gatherTerrain(level, prepared.range()));
    }

    private static PreparedColumn prepare(Vec3 lip, Vec3 look, double across, double thick) {
        double limit = WeepingSettings.FALL_DISTANCE_LIMIT;
        if (!Double.isFinite(limit) || limit <= 0.0) {
            return null;
        }
        Vec3 mouth = lip;
        if (mouth == null || !Double.isFinite(mouth.x) || !Double.isFinite(mouth.y) || !Double.isFinite(mouth.z)) {
            mouth = Vec3.ZERO;
        }
        double fullAcross = across;
        double fullThick = thick;
        if (!Double.isFinite(fullAcross) || fullAcross < 0.0) {
            fullAcross = 0.0;
        }
        if (!Double.isFinite(fullThick) || fullThick < 0.0) {
            fullThick = 0.0;
        }
        FlatFrame frame = FlatFrame.of(look);
        AABB bounds = columnBounds(mouth, frame, fullAcross * 0.5, fullThick * 0.5, limit);
        BlockRange range = BlockRange.of(bounds);
        return new PreparedColumn(mouth, frame, fullAcross * 0.5, fullThick * 0.5, limit, range, range.cellCount());
    }

    private static FootClip clipBoxes(PreparedColumn prepared, List<AABB> boxes) {
        if (boxes == null || boxes.isEmpty()) {
            return FootClip.openAir(prepared.dropLimit(), prepared.cellCount());
        }
        boolean buried = false;
        boolean foundTop = false;
        double highestTop = Double.NEGATIVE_INFINITY;
        for (AABB box : boxes) {
            if (!boxUsable(box) || !overlapsFootprint(prepared, box)) {
                continue;
            }
            if (box.minY < prepared.mouth().y - PLANE_SLOP && box.maxY > prepared.mouth().y + PLANE_SLOP) {
                buried = true;
                break;
            }
            if (box.maxY <= prepared.mouth().y + PLANE_SLOP
                    && box.maxY > prepared.mouth().y - prepared.dropLimit() + PLANE_SLOP) {
                if (!foundTop || box.maxY > highestTop) {
                    highestTop = box.maxY;
                    foundTop = true;
                }
            }
        }
        if (buried) {
            return FootClip.blockedSource(prepared.cellCount());
        }
        if (!foundTop) {
            return FootClip.openAir(prepared.dropLimit(), prepared.cellCount());
        }
        double drop = prepared.mouth().y - highestTop;
        if (!Double.isFinite(drop) || drop < 0.0) {
            drop = 0.0;
        }
        if (drop > prepared.dropLimit()) {
            return FootClip.openAir(prepared.dropLimit(), prepared.cellCount());
        }
        return new FootClip(drop, true, false, false, prepared.cellCount());
    }

    private static boolean overlapsFootprint(PreparedColumn prepared, AABB box) {
        Vec3 ahead = prepared.frame().ahead();
        Vec3 right = prepared.frame().right();
        double centerX = (box.minX + box.maxX) * 0.5;
        double centerZ = (box.minZ + box.maxZ) * 0.5;
        double halfBoxX = (box.maxX - box.minX) * 0.5;
        double halfBoxZ = (box.maxZ - box.minZ) * 0.5;
        double offsetX = centerX - prepared.mouth().x;
        double offsetZ = centerZ - prepared.mouth().z;
        double curtainRadiusX = Math.abs(ahead.x) * prepared.halfThick() + Math.abs(right.x) * prepared.halfAcross();
        if (Math.abs(offsetX) > halfBoxX + curtainRadiusX - SHAPE_SLOP) {
            return false;
        }
        double curtainRadiusZ = Math.abs(ahead.z) * prepared.halfThick() + Math.abs(right.z) * prepared.halfAcross();
        if (Math.abs(offsetZ) > halfBoxZ + curtainRadiusZ - SHAPE_SLOP) {
            return false;
        }
        double aheadOffset = offsetX * ahead.x + offsetZ * ahead.z;
        double boxOnAhead = halfBoxX * Math.abs(ahead.x) + halfBoxZ * Math.abs(ahead.z);
        if (Math.abs(aheadOffset) > prepared.halfThick() + boxOnAhead - SHAPE_SLOP) {
            return false;
        }
        double rightOffset = offsetX * right.x + offsetZ * right.z;
        double boxOnRight = halfBoxX * Math.abs(right.x) + halfBoxZ * Math.abs(right.z);
        if (Math.abs(rightOffset) > prepared.halfAcross() + boxOnRight - SHAPE_SLOP) {
            return false;
        }
        return true;
    }

    private static List<AABB> gatherTerrain(ServerLevel level, BlockRange range) {
        List<AABB> gathered = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        CollisionContext emptyContext = CollisionContext.empty();
        for (int columnX = range.westEdge(); columnX <= range.eastEdge(); columnX++) {
            for (int layerY = range.lowEdge(); layerY <= range.highEdge(); layerY++) {
                for (int columnZ = range.northEdge(); columnZ <= range.southEdge(); columnZ++) {
                    cursor.set(columnX, layerY, columnZ);
                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir()) {
                        continue;
                    }
                    VoxelShape shape = state.getCollisionShape(level, cursor, emptyContext);
                    if (shape == null || shape.isEmpty()) {
                        continue;
                    }
                    VoxelShape moved = shape.move(columnX, layerY, columnZ);
                    for (AABB part : moved.toAabbs()) {
                        if (boxUsable(part)) {
                            gathered.add(part);
                        }
                    }
                }
            }
        }
        return gathered;
    }

    private static AABB columnBounds(Vec3 mouth, FlatFrame frame, double halfAcross, double halfThick, double dropLimit) {
        Vec3 ahead = frame.ahead();
        Vec3 right = frame.right();
        double west = mouth.x;
        double east = mouth.x;
        double north = mouth.z;
        double south = mouth.z;
        double[] cornerAlong = new double[] {halfThick, halfThick, -halfThick, -halfThick};
        double[] cornerAcross = new double[] {halfAcross, -halfAcross, halfAcross, -halfAcross};
        for (int cornerIndex = 0; cornerIndex < cornerAlong.length; cornerIndex++) {
            double sampleX = mouth.x + ahead.x * cornerAlong[cornerIndex] + right.x * cornerAcross[cornerIndex];
            double sampleZ = mouth.z + ahead.z * cornerAlong[cornerIndex] + right.z * cornerAcross[cornerIndex];
            west = Math.min(west, sampleX);
            east = Math.max(east, sampleX);
            north = Math.min(north, sampleZ);
            south = Math.max(south, sampleZ);
        }
        return new AABB(west, mouth.y - dropLimit, north, east, mouth.y, south);
    }

    private static boolean boxUsable(AABB box) {
        return box != null
                && Double.isFinite(box.minX) && Double.isFinite(box.minY) && Double.isFinite(box.minZ)
                && Double.isFinite(box.maxX) && Double.isFinite(box.maxY) && Double.isFinite(box.maxZ)
                && box.maxX >= box.minX && box.maxY >= box.minY && box.maxZ >= box.minZ;
    }

    private static boolean isCurtain(TechniqueHitEntity entity) {
        return entity != null && FUNCTION_ID.equals(entity.functionId());
    }

    private static AABB sheetBounds(WaterSegment segment) {
        if (segment == null || !segment.present()) {
            return null;
        }
        Vec3 center = segment.center();
        Vec3 ahead = segment.ahead();
        Vec3 right = new Vec3(ahead.z, 0.0, -ahead.x);
        double halfThick = segment.thick() * 0.5;
        double halfAcross = segment.across() * 0.5;
        double halfHeight = segment.activeHeight() * 0.5;
        double reachEast = halfThick * Math.abs(ahead.x) + halfAcross * Math.abs(right.x);
        double reachSouth = halfThick * Math.abs(ahead.z) + halfAcross * Math.abs(right.z);
        return new AABB(
                center.x - reachEast, center.y - halfHeight, center.z - reachSouth,
                center.x + reachEast, center.y + halfHeight, center.z + reachSouth);
    }

    private static boolean sheetOverlaps(WaterSegment segment, AABB body) {
        if (segment == null || !segment.present() || body == null) {
            return false;
        }
        Vec3 center = segment.center();
        double halfHeight = segment.activeHeight() * 0.5;
        double sheetLow = center.y - halfHeight;
        double sheetHigh = center.y + halfHeight;
        if (body.maxY < sheetLow || body.minY > sheetHigh) {
            return false;
        }
        Vec3 ahead = segment.ahead();
        Vec3 right = new Vec3(ahead.z, 0.0, -ahead.x);
        double halfThick = segment.thick() * 0.5;
        double halfAcross = segment.across() * 0.5;
        double centerEast = (body.minX + body.maxX) * 0.5;
        double centerSouth = (body.minZ + body.maxZ) * 0.5;
        double halfEast = (body.maxX - body.minX) * 0.5;
        double halfSouth = (body.maxZ - body.minZ) * 0.5;
        double offsetEast = centerEast - center.x;
        double offsetSouth = centerSouth - center.z;
        double aheadGap = Math.abs(offsetEast * ahead.x + offsetSouth * ahead.z);
        double aheadReach = halfEast * Math.abs(ahead.x) + halfSouth * Math.abs(ahead.z);
        if (aheadGap > halfThick + aheadReach) {
            return false;
        }
        double rightGap = Math.abs(offsetEast * right.x + offsetSouth * right.z);
        double rightReach = halfEast * Math.abs(right.x) + halfSouth * Math.abs(right.z);
        if (rightGap > halfAcross + rightReach) {
            return false;
        }
        double sheetOnEast = halfThick * Math.abs(ahead.x) + halfAcross * Math.abs(right.x);
        if (Math.abs(offsetEast) > sheetOnEast + halfEast) {
            return false;
        }
        double sheetOnSouth = halfThick * Math.abs(ahead.z) + halfAcross * Math.abs(right.z);
        return Math.abs(offsetSouth) <= sheetOnSouth + halfSouth;
    }

    private static boolean sweptOverlap(WaterSegment previous, WaterSegment current, AABB body) {
        Vec3 from = previous.lip();
        Vec3 toward = current.lip();
        if (from.distanceToSqr(toward) < 1.0E-8) {
            return false;
        }
        double span = from.distanceTo(toward);
        double step = Math.max(current.thick(), 0.25);
        int sampleCount = (int) Math.ceil(span / step);
        if (sampleCount < 2) {
            return false;
        }
        if (sampleCount > 8) {
            sampleCount = 8;
        }
        for (int sampleIndex = 1; sampleIndex < sampleCount; sampleIndex++) {
            double blend = sampleIndex / (double) sampleCount;
            if (sheetOverlaps(blendSegment(previous, current, blend), body)) {
                return true;
            }
        }
        return false;
    }

    private static WaterSegment blendSegment(WaterSegment from, WaterSegment toward, double blend) {
        Vec3 mouth = lerpPoint(from.lip(), toward.lip(), blend);
        Vec3 center = lerpPoint(from.center(), toward.center(), blend);
        Vec3 facing = lerpPoint(from.ahead(), toward.ahead(), blend);
        double upperDrop = from.upperDrop() + (toward.upperDrop() - from.upperDrop()) * blend;
        double lowerDrop = from.lowerDrop() + (toward.lowerDrop() - from.lowerDrop()) * blend;
        double footDrop = Math.max(from.footDrop(), toward.footDrop());
        double across = Math.max(from.across(), toward.across());
        double thick = Math.max(from.thick(), toward.thick());
        return new WaterSegment(mouth, center, facing, upperDrop, lowerDrop, footDrop, across, thick,
                from.present() || toward.present(), from.floorFoot() || toward.floorFoot());
    }

    private static Vec3 lerpPoint(Vec3 from, Vec3 toward, double blend) {
        return new Vec3(
                from.x + (toward.x - from.x) * blend,
                from.y + (toward.y - from.y) * blend,
                from.z + (toward.z - from.z) * blend);
    }

    private static void noteFootprint(long cellCount, boolean rejected) {
        notedCells = cellCount;
        notedOverBudget = rejected;
    }

    /**
     * 一列水的裁切结果。{@code floorFoot} 只在落到朝上的盒顶，并且落差小于最大落差时为真。
     */
    public record FootClip(double footDrop, boolean floorFoot, boolean sourceInside, boolean overBudget, long cellCount) {
        public static FootClip openAir(double footDrop, long cellCount) {
            double drop = footDrop;
            if (!Double.isFinite(drop) || drop < 0.0) {
                drop = 0.0;
            }
            return new FootClip(drop, false, false, false, cellCount);
        }

        public static FootClip blockedSource(long cellCount) {
            return new FootClip(0.0, false, true, false, cellCount);
        }

        public static FootClip budgetStop(long cellCount) {
            return new FootClip(0.0, false, false, true, cellCount);
        }
    }

    private record FlatFrame(Vec3 ahead, Vec3 right) {
        private static FlatFrame of(Vec3 look) {
            Vec3 ahead = WaterSegment.horizontalAhead(look);
            return new FlatFrame(ahead, new Vec3(ahead.z, 0.0, -ahead.x));
        }
    }

    private record PreparedColumn(Vec3 mouth, FlatFrame frame, double halfAcross, double halfThick,
                                  double dropLimit, BlockRange range, long cellCount) {
    }

    private record BlockRange(int westEdge, int eastEdge, int lowEdge, int highEdge,
                              int northEdge, int southEdge, long cellCount) {
        private static BlockRange of(AABB bounds) {
            int westEdge = Mth.floor(bounds.minX);
            int eastEdge = Mth.floor(bounds.maxX - EDGE_SLOP);
            int lowEdge = Mth.floor(bounds.minY);
            int highEdge = Mth.floor(bounds.maxY - EDGE_SLOP);
            int northEdge = Mth.floor(bounds.minZ);
            int southEdge = Mth.floor(bounds.maxZ - EDGE_SLOP);
            if (eastEdge < westEdge) {
                eastEdge = westEdge;
            }
            if (highEdge < lowEdge) {
                highEdge = lowEdge;
            }
            if (southEdge < northEdge) {
                southEdge = northEdge;
            }
            long spanX = (long) eastEdge - westEdge + 1L;
            long spanY = (long) highEdge - lowEdge + 1L;
            long spanZ = (long) southEdge - northEdge + 1L;
            long cellCount;
            if (spanX > FOOTPRINT_CELL_BUDGET || spanY > FOOTPRINT_CELL_BUDGET || spanZ > FOOTPRINT_CELL_BUDGET) {
                cellCount = (long) FOOTPRINT_CELL_BUDGET + 1L;
            } else {
                cellCount = spanX * spanY * spanZ;
            }
            return new BlockRange(westEdge, eastEdge, lowEdge, highEdge, northEdge, southEdge, cellCount);
        }
    }
}
