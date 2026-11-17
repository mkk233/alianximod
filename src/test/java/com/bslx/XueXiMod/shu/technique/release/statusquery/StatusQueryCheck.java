package com.bslx.XueXiMod.shu.technique.release.statusquery;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * 调用真实查询、注册、距离和快照函数。不构造世界，也不启动游戏。
 */
public final class StatusQueryCheck {
    private static final Path SOURCE_ROOT = Path.of(
            "D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/statusquery");
    private static int assertionCount;
    private static int failures;
    private static boolean registrySkipped;

    private StatusQueryCheck() {
    }

    public static void main(String[] arguments) {
        section("查询", StatusQueryCheck::queries);
        section("几何", StatusQueryCheck::geometry);
        section("快照", StatusQueryCheck::snapshots);
        section("源码", StatusQueryCheck::sourceSection);
        registryProbe();
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        System.out.println("未执行：真实世界扫描、方块射线、末影龙部位实例、实体上的湿度正数与药水实例。没有启动游戏。");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void queries() {
        ResourceLocation unknown = new ResourceLocation("xueximod", "missing_status");
        ResourceLocation probeKey = new ResourceLocation("xueximod", "status_query_probe");
        check(ResourceStatuses.find(unknown).isEmpty(), "未知资源不存在");
        check(!StatusQuery.resourceMatches(null, unknown, 0), "未知资源即使下限为 0 也不命中");
        check(!StatusQuery.resourceMatches(null, ResourceStatuses.WETNESS, 0), "没有目标时不把湿度 0 当成命中");

        Optional<ResourceStatus> wetness = ResourceStatuses.find(ResourceStatuses.WETNESS);
        check(wetness.isPresent(), "内建湿度已登记");
        ResourceView emptyWetness = wetness.orElseThrow().read(null);
        check(emptyWetness.value() == 0 && emptyWetness.tier() == 0 && emptyWetness.durationTicks() == 0,
                "空目标湿度只读为零");
        check(!StatusQuery.matchesView(ResourceStatuses.WETNESS, 1, emptyWetness), "湿度 0 不满足至少 1");
        check(StatusQuery.matchesView(ResourceStatuses.WETNESS, 0, emptyWetness), "湿度 0 满足下限 0");

        ResourceView numeric = new ResourceView(probeKey, 1, 4, 9);
        check(StatusQuery.matchesView(probeKey, 1, numeric), "比较的是数值");
        ResourceView tierOnly = new ResourceView(probeKey, 0, 4, 9);
        check(!StatusQuery.matchesView(probeKey, 1, tierOnly), "档位高但数值不足不命中");
        check(!StatusQuery.matchesView(unknown, 1, numeric), "标识不同不命中");
        check(!StatusQuery.matchesView(probeKey, 1, null), "空视图不命中");
        check(!StatusQuery.matchesView(probeKey, -1, numeric), "非法下限不命中");

        if (ResourceStatuses.find(probeKey).isEmpty()) {
            ResourceStatuses.register(new ProbeStatus(probeKey, 40, 3, 20));
        }
        Optional<ResourceStatus> probe = ResourceStatuses.find(probeKey);
        check(probe.isPresent(), "自定义资源可以注册");
        ResourceView probed = probe.orElseThrow().read(null);
        check(StatusQuery.matchesView(probeKey, 40, probed), "自定义资源达到下限");
        check(!StatusQuery.matchesView(probeKey, 41, probed), "自定义资源低于下限不命中");
        check(probed.tier() == 3 && probed.durationTicks() == 20, "自定义资源保留档位和时长");
        boolean duplicateRejected = false;
        try {
            ResourceStatuses.register(new ProbeStatus(probeKey, 1, 1, 1));
        } catch (IllegalStateException failure) {
            duplicateRejected = true;
        }
        check(duplicateRejected, "同一标识不能重复注册");
        boolean nullRejected = false;
        try {
            ResourceStatuses.register(null);
        } catch (IllegalArgumentException failure) {
            nullRejected = true;
        }
        check(nullRejected, "空资源提供者被拒绝");

        Map<ResourceLocation, ResourceView> views = ResourceStatuses.readAll(null);
        check(views.get(ResourceStatuses.WETNESS).value() == 0, "快照含只读湿度");
        check(views.get(probeKey).value() == 40, "快照含已注册的自定义资源");
        check(StatusQuery.activeEffects(null).isEmpty(), "没有目标时不读药水");

        check(!StatusQuery.effectCounts(false, true, 10, false), "未注册药水不命中");
        check(!StatusQuery.effectCounts(true, false, 10, false), "没有实例不命中");
        check(!StatusQuery.effectCounts(true, true, 0, false), "剩余时间为 0 且非永久不命中");
        check(StatusQuery.effectCounts(true, true, 1, false), "剩余时间大于 0 才有效");
        check(StatusQuery.effectCounts(true, true, -1, true), "永久药水有效");
        check(!StatusQuery.effectCounts(true, true, -1, false), "负时长但未标永久不命中");
        check(StatusQuery.effectCounts(true, true, 0, true), "永久标记即使时长为 0 也有效");
        check(!StatusQuery.effectCounts(null, null), "空药水标识不命中");

        check(!StatusQuery.combine(), "空组合不命中");
        check(StatusQuery.combine(true, true), "两项都成立才命中");
        check(!StatusQuery.combine(true, false), "任一项失败则组合失败");
        StatusQuery missing = StatusQuery.resourceAtLeast(unknown, 1);
        check(!StatusQuery.and().matches(null), "没有条件的组合查询不命中");
        check(!StatusQuery.and(missing, missing).matches(null), "未知资源组合不命中");

        boolean negativeRejected = false;
        try {
            StatusQuery.resourceAtLeast(ResourceStatuses.WETNESS, -1);
        } catch (IllegalArgumentException failure) {
            negativeRejected = true;
        }
        check(negativeRejected, "负数下限被拒绝");
        boolean nullKeyRejected = false;
        try {
            StatusQuery.resourceAtLeast(null, 1);
        } catch (IllegalArgumentException failure) {
            nullKeyRejected = true;
        }
        check(nullKeyRejected, "空资源标识被拒绝");
        boolean nullEffectRejected = false;
        try {
            StatusQuery.effectActive(null);
        } catch (IllegalArgumentException failure) {
            nullEffectRejected = true;
        }
        check(nullEffectRejected, "空药水标识被拒绝");
        boolean nullQueryRejected = false;
        try {
            StatusQuery.and(missing, null);
        } catch (IllegalArgumentException failure) {
            nullQueryRejected = true;
        }
        check(nullQueryRejected, "组合里的空条件被拒绝");
    }

    private static void geometry() {
        AABB bounds = new AABB(0.0D, 0.0D, 0.0D, 2.0D, 2.0D, 2.0D);
        Vec3 inside = BlockOcclusion.closestPoint(new Vec3(1.0D, 1.0D, 1.0D), bounds);
        check(inside.x == 1.0D && inside.y == 1.0D && inside.z == 1.0D, "盒内点的最近点是自身");
        Vec3 outsideOrigin = new Vec3(5.0D, -2.0D, 1.0D);
        Vec3 outside = BlockOcclusion.closestPoint(outsideOrigin, bounds);
        check(outside.x == 2.0D && outside.y == 0.0D && outside.z == 1.0D, "盒外点夹到表面上");
        check(outsideOrigin.distanceToSqr(outside) == 13.0D, "夹取后的距离平方");

        Vec3 origin = Vec3.ZERO;
        AABB border = new AABB(12.0D, 0.0D, 0.0D, 12.0D, 1.0D, 1.0D);
        Vec3 borderPoint = BlockOcclusion.closestPoint(origin, border);
        double borderDistance = origin.distanceToSqr(borderPoint);
        check(borderDistance == 144.0D, "12 格边界距离平方 " + borderDistance);
        check(BlockOcclusion.withinRadius(borderDistance, 12.0D), "刚好 12 格纳入");
        AABB beyond = new AABB(12.01D, 0.0D, 0.0D, 13.0D, 1.0D, 1.0D);
        double beyondDistance = origin.distanceToSqr(BlockOcclusion.closestPoint(origin, beyond));
        check(!BlockOcclusion.withinRadius(beyondDistance, 12.0D), "超过 12 格排除");
        check(!BlockOcclusion.withinRadius(1.0D, 0.0D), "非正半径排除");
        check(!BlockOcclusion.withinRadius(Double.NaN, 12.0D), "非数距离排除");
        check(!BlockOcclusion.withinRadius(Double.POSITIVE_INFINITY, 12.0D), "无限距离排除");

        Vec3 eye = new Vec3(0.0D, 1.6D, 0.0D);
        Vec3 look = new Vec3(0.0D, 0.0D, 1.0D);
        check(BlockOcclusion.inFront(eye, new Vec3(0.0D, 1.6D, 4.0D), look), "眼前点积为正");
        check(!BlockOcclusion.inFront(eye, new Vec3(0.0D, 1.6D, -1.0D), look), "背后点积不为正");
        check(!BlockOcclusion.inFront(eye, eye, look), "零向量不算在前方");
        boolean boundsRejected = false;
        try {
            BlockOcclusion.closestPoint(null, bounds);
        } catch (IllegalArgumentException failure) {
            boundsRejected = true;
        }
        check(boundsRejected, "空起点被拒绝");
    }

    private static void snapshots() {
        ResourceLocation probeKey = new ResourceLocation("xueximod", "status_query_probe");
        ResourceLocation otherKey = new ResourceLocation("xueximod", "status_query_other");
        Map<ResourceLocation, ResourceView> source = new HashMap<>();
        source.put(probeKey, new ResourceView(probeKey, 3, 1, 8));
        Map<ResourceLocation, ResourceView> frozen = StatusCandidate.freezeResources(source);
        source.clear();
        check(frozen.size() == 1 && frozen.get(probeKey).value() == 3, "资源快照不跟随原映射变化");
        boolean mapRejected = false;
        try {
            frozen.put(otherKey, new ResourceView(otherKey, 1, 1, 1));
        } catch (UnsupportedOperationException failure) {
            mapRejected = true;
        }
        check(mapRejected, "资源快照不可改");
        check(StatusCandidate.freezeResources(null).isEmpty(), "空资源快照");
        check(StatusCandidate.freezeEffects(null).isEmpty(), "空药水快照");

        UUID earlier = new UUID(0L, 1L);
        UUID later = new UUID(0L, 2L);
        check(StatusScan.snapshotOrder(1.0D, later, 2.0D, earlier) < 0, "更近的在前");
        check(StatusScan.snapshotOrder(2.0D, earlier, 2.0D, later) < 0, "同距按 UUID 升序");
        check(StatusScan.snapshotOrder(2.0D, later, 2.0D, earlier) > 0, "较大 UUID 在后");

        StatusQuery query = StatusQuery.resourceAtLeast(new ResourceLocation("xueximod", "missing_status"), 1);
        RelationPolicy policy = RelationPolicy.hostileCapture();
        StatusScanRequest request = new StatusScanRequest(12.0D, query, policy, true, true);
        check(request.radius() == 12.0D && request.blockOcclusion() && request.fullCircle(),
                "请求保留半径 12、遮挡和全圆");
        StatusScanRequest facing = new StatusScanRequest(4.0D, query, policy, false, false);
        check(!facing.blockOcclusion() && !facing.fullCircle(), "遮挡和全圆可以关掉");
        List<StatusCandidate> none = StatusScan.collect(null, request);
        check(none.isEmpty(), "没有施术者时不扫描");
        check(StatusScan.collect(null, null).isEmpty(), "空请求不扫描");
        boolean mutationRejected = false;
        try {
            none.add(null);
        } catch (UnsupportedOperationException failure) {
            mutationRejected = true;
        }
        check(mutationRejected, "扫描结果不可改");
        expectRequestRejected(0.0D, query, policy, "零半径被拒绝");
        expectRequestRejected(-2.0D, query, policy, "负半径被拒绝");
        expectRequestRejected(Double.NaN, query, policy, "非数半径被拒绝");
        expectRequestRejected(Double.POSITIVE_INFINITY, query, policy, "无限半径被拒绝");
        boolean nullQueryRejected = false;
        try {
            new StatusScanRequest(12.0D, null, policy, true, true);
        } catch (IllegalArgumentException failure) {
            nullQueryRejected = true;
        }
        check(nullQueryRejected, "空查询被拒绝");
        boolean nullPolicyRejected = false;
        try {
            new StatusScanRequest(12.0D, query, null, true, true);
        } catch (IllegalArgumentException failure) {
            nullPolicyRejected = true;
        }
        check(nullPolicyRejected, "空关系策略被拒绝");
    }

    private static void expectRequestRejected(double radius, StatusQuery query, RelationPolicy policy, String detail) {
        boolean rejected = false;
        try {
            new StatusScanRequest(radius, query, policy, true, true);
        } catch (IllegalArgumentException failure) {
            rejected = true;
        }
        check(rejected, detail);
    }

    private static void sourceSection() {
        try {
            purity();
        } catch (IOException failure) {
            assertionCount++;
            failures++;
            System.out.println("失败 读取源码 " + failure);
        }
    }

    private static void purity() throws IOException {
        String[] forbidden = {
                ".grant(",
                ".reconcile(",
                ".replaceExact(",
                ".replacePreservingDuration(",
                ".replace(",
                "AttributeStateData.write",
                "AttributeStateService.write",
                "addEffect",
                "removeEffect",
                "getPersistentData",
                "ModMobEffects",
                "CompoundTag",
                "water_prison"
        };
        try (Stream<Path> walk = Files.list(SOURCE_ROOT)) {
            for (Path file : walk.filter(path -> path.toString().endsWith(".java")).toList()) {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                for (String token : forbidden) {
                    check(!text.contains(token), file.getFileName() + " 含有写状态或越界标记 " + token);
                }
            }
        }
        String resources = Files.readString(SOURCE_ROOT.resolve("ResourceStatuses.java"), StandardCharsets.UTF_8);
        check(resources.contains("WetnessState.read"), "湿度走只读数值");
        check(resources.contains("WetnessState.tierOf"), "湿度走只读分档");
        check(resources.contains("WetnessState.remainingDuration"), "湿度走剩余时间");
        String query = Files.readString(SOURCE_ROOT.resolve("StatusQuery.java"), StandardCharsets.UTF_8);
        int containsAt = query.indexOf("containsKey");
        int valueAt = query.indexOf("getValue");
        check(containsAt >= 0 && valueAt > containsAt, "药水先确认注册表再取值");
        String occlusion = Files.readString(SOURCE_ROOT.resolve("BlockOcclusion.java"), StandardCharsets.UTF_8);
        check(occlusion.contains("ClipContext.Block.COLLIDER"), "遮挡使用碰撞箱");
        check(occlusion.contains("ClipContext.Fluid.NONE"), "流体不挡");
        check(!occlusion.contains("EntityHitResult"), "实体不参与遮挡");
        String scan = Files.readString(SOURCE_ROOT.resolve("StatusScan.java"), StandardCharsets.UTF_8);
        check(scan.contains("parentMob"), "龙部位收成主体");
        check(scan.contains("putIfAbsent"), "主体按 UUID 去重");
        check(scan.contains("List.copyOf"), "结果列表是快照副本");
        check(scan.contains("isSameThread"), "只在服务端线程收集");
        int relationAt = scan.indexOf("relation().accepts");
        int occlusionAt = scan.indexOf("clearOfBlocks");
        int queryAt = scan.indexOf("query().matches");
        check(relationAt >= 0 && relationAt < occlusionAt && occlusionAt < queryAt, "先关系、再遮挡、最后读状态");
        check(scan.contains("fullCircle") && scan.contains("inFront"), "非全圆才看向前点积");
    }

    private static void registryProbe() {
        ResourceLocation unknownEffect = new ResourceLocation("xueximod", "missing_effect");
        try {
            boolean counted = StatusQuery.effectCounts(null, unknownEffect);
            check(!counted, "未知药水标识不命中");
            check(!StatusQuery.effectActive(unknownEffect).matches(null), "未知药水查询不命中");
        } catch (Throwable failure) {
            registrySkipped = true;
            System.out.println("未执行：药水注册表查询 " + failure.getClass().getName());
        }
        if (registrySkipped) {
            System.out.println("未执行：Forge 药水注册表未启动，未知标识的运行时拒绝留到统一测试。");
        }
    }

    private static void section(String name, Runnable body) {
        try {
            body.run();
        } catch (RuntimeException failure) {
            assertionCount++;
            failures++;
            System.out.println("失败[" + assertionCount + "] " + name + " 抛出 " + failure);
            failure.printStackTrace(System.out);
        }
    }

    private static void check(boolean condition, String detail) {
        assertionCount++;
        if (!condition) {
            failures++;
            System.out.println("失败[" + assertionCount + "] " + detail);
        }
    }

    private static final class ProbeStatus implements ResourceStatus {
        private final ResourceLocation identifier;
        private final int amount;
        private final int tier;
        private final int durationTicks;

        private ProbeStatus(ResourceLocation identifier, int amount, int tier, int durationTicks) {
            this.identifier = identifier;
            this.amount = amount;
            this.tier = tier;
            this.durationTicks = durationTicks;
        }

        @Override
        public ResourceLocation identifier() {
            return this.identifier;
        }

        @Override
        public ResourceView read(LivingEntity subject) {
            return new ResourceView(this.identifier, this.amount, this.tier, this.durationTicks);
        }
    }
}
