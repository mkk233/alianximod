package com.bslx.XueXiMod.status.fire.seed;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * 直接调用火种纯运算和账本。不构造世界，也不启动游戏。
 */
public final class FireSeedCheck {
    private static final Path SEED_ROOT = Path.of(
            "D:/alianximod/src/main/java/com/bslx/XueXiMod/status/fire/seed");
    private static final Path RESOURCE_STATUSES = Path.of(
            "D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/statusquery/ResourceStatuses.java");
    private static final Path EFFECT_REGISTRY = Path.of(
            "D:/alianximod/src/main/java/com/bslx/XueXiMod/registry/ModMobEffects.java");
    private static int assertionCount;
    private static int failures;

    private FireSeedCheck() {
    }

    public static void main(String[] arguments) {
        section("边界", FireSeedCheck::boundaries);
        section("生产", FireSeedCheck::production);
        section("消费", FireSeedCheck::consumption);
        section("共享", FireSeedCheck::sharedBalance);
        section("快照", FireSeedCheck::snapshots);
        section("源码", FireSeedCheck::sourceShape);
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        System.out.println("未运行：真实实体存档、死亡事件、玩家克隆、生物转化、牛奶、服务端主线程拒绝、Gradle 编译。没有游戏世界。");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void boundaries() {
        int[] samples = {0, 9, 10, 19, 99, 100};
        int[] tierList = {0, 0, 1, 1, 9, 10};
        int[] remainderList = {0, 9, 0, 9, 9, 0};
        int[] spendList = {0, 0, 10, 10, 90, 100};
        for (int index = 0; index < samples.length; index++) {
            int sample = samples[index];
            check(FireSeedMath.tiers(sample) == tierList[index], sample + " 的档位");
            check(FireSeedMath.remainder(sample) == remainderList[index], sample + " 的余数");
            check(FireSeedMath.wholeTierSpend(sample) == spendList[index], sample + " 的整档扣除");
            check(FireSeedMath.clamp(sample) == sample, sample + " 已在范围内");
        }
        check(FireSeedMath.clamp(-1) == 0, "负数钳成 0");
        check(FireSeedMath.clamp(Integer.MIN_VALUE) == 0, "下溢钳成 0");
        check(FireSeedMath.clamp(101) == 100, "101 钳成 100");
        check(FireSeedMath.clamp(Integer.MAX_VALUE) == 100, "上溢钳成 100");
        check(new FireSeedAccount(150).balance() == 100, "账户把 150 收成 100");
        check(new FireSeedAccount(-4).balance() == 0, "账户把负数收成 0");

        check(FireSeedMath.floorProduction(0, 2.2D) == 0, "基础量 0 乘过载倍率仍是 0");
        check(FireSeedMath.floorProduction(0, 1.0D) == 0, "基础量 0 不抬成 1");
        check(FireSeedMath.floorProduction(1, 1.12D) == 1, "1.12 向下取整为 1");
        check(FireSeedMath.floorProduction(3, 1.12D) == 3, "3.36 向下取整为 3");
        check(FireSeedMath.floorProduction(9, 1.12D) == 10, "10.08 向下取整为 10");
        check(FireSeedMath.floorProduction(1, 2.2D) == 2, "2.2 向下取整为 2");
        check(FireSeedMath.floorProduction(4, 2.2D) == 8, "8.8 向下取整为 8");
        check(FireSeedMath.floorProduction(1, 0.99D) == 0, "不足 1 没有保底");
        check(FireSeedMath.floorProduction(50, 2.2D) == 100, "过大的生产请求压到 100");
        check(FireSeedMath.floorProduction(-3, 2.0D) == 0, "负数基础量不生产");
        check(FireSeedMath.floorProduction(4, Double.NaN) == 0, "非数倍率不生产");
        check(FireSeedMath.floorProduction(4, -1.0D) == 0, "负数倍率不生产");
        check(FireSeedMath.floorProduction(4, Double.POSITIVE_INFINITY) == 0, "无限倍率不生产");
        check(FireSeedMath.floorProduction(Integer.MAX_VALUE, 1.0D) == 100, "巨大基础量压到上限");

        check(FireSeedMath.invested(19, Integer.MAX_VALUE) == 19, "最大请求表示全部余额");
        check(FireSeedMath.invested(19, 15) == 15, "请求 15 点就扣 15");
        check(FireSeedMath.invested(19, 100) == 19, "超过余额时只取余额");
        check(FireSeedMath.invested(19, 0) == 0, "请求 0 不扣");
        check(FireSeedMath.invested(19, -1) == 0, "负数请求不是全部消费");
        check(FireSeedMath.invested(0, Integer.MAX_VALUE) == 0, "空余额没有可投入点数");

        check(near(FireSeedArchive.BURST.yield(1), 0.04D), "引爆每档 0.04");
        check(near(FireSeedArchive.BURST.yield(10), 0.40D), "十档引爆 0.40");
        check(FireSeedArchive.SUSTAIN.yield(1) == 10.0D, "持续每档 10 tick");
        check(FireSeedMath.sustainTicks(3) == 30, "三档持续加 30 tick");
        check(FireSeedMath.sustainTicks(10) == 100, "十档持续加 100 tick");
        check(FireSeedMath.sustainTicks(0) == 0, "零档不加持续时间");
        check(FireSeedMath.sustainTicks(-2) == 0, "负档不加持续时间");
        check(near(FireSeedArchive.SPREAD.yield(1), 0.10D), "扩散每档 0.10");
        check(near(FireSeedArchive.SPREAD.yield(10), 1.0D), "十档扩散 1 格");
        check(FireSeedArchive.BURST.yield(0) == 0.0D, "零档引爆没有加成");
        check(near(FireSeedArchive.BURST.yield(FireSeedMath.tiers(19)),
                FireSeedArchive.BURST.yield(1)), "19 点只有 1 个完整档");
        check(!FireSeedMath.finiteNonNegative(Double.NaN), "非数不能进持久数据");
        check(!FireSeedMath.finiteNonNegative(-0.2D), "负数不能进持久数据");
        check(FireSeedMath.finiteNonNegative(0.0D), "0 是合法炉心加成");
    }

    private static void production() {
        UUID targetId = UUID.randomUUID();
        FireCastContext cast = new FireCastContext(UUID.randomUUID(), null);
        FireSeedAccount account = new FireSeedAccount(10);
        check(account.grant(cast, targetId, 0, 2.2D) == 0, "0 生产不增加");
        check(account.balance() == 10, "0 生产不改余额");
        check(!cast.hasProduced(targetId), "0 生产不占用机会");
        check(account.grant(cast, targetId, 4, 1.0D) == 4, "之后的正生产仍然有效");
        check(account.balance() == 14, "正生产写入余额");
        check(cast.hasProduced(targetId), "正生产记下机会");
        check(account.grant(cast, targetId, 4, 1.0D) == 0, "同一目标不能再生产");
        check(account.balance() == 14, "重复生产不改余额");

        FireSeedAccount capped = new FireSeedAccount(100);
        UUID cappedTarget = UUID.randomUUID();
        FireCastContext cappedCast = new FireCastContext(UUID.randomUUID(), UUID.randomUUID());
        check(capped.grant(cappedCast, cappedTarget, 8, 1.0D) == 0, "已满时实际增加为 0");
        check(capped.balance() == 100, "已满余额保持 100");
        check(cappedCast.hasProduced(cappedTarget), "到达上限仍记下生产机会");
        check(capped.grant(cappedCast, cappedTarget, 3, 2.2D) == 0, "满额后的再次生产被拒绝");

        FireSeedAccount almost = new FireSeedAccount(98);
        UUID almostTarget = UUID.randomUUID();
        FireCastContext almostCast = new FireCastContext(UUID.randomUUID(), UUID.randomUUID());
        check(almost.grant(almostCast, almostTarget, 5, 1.0D) == 2, "98 再加 5 只剩 2 点空位");
        check(almost.balance() == 100, "溢出丢掉，不另藏余额");
        check(almostCast.hasProduced(almostTarget), "部分溢出也占用机会");

        FireSeedAccount clean = new FireSeedAccount(20);
        UUID cleanTarget = UUID.randomUUID();
        FireCastContext cleanCast = new FireCastContext(UUID.randomUUID(), UUID.randomUUID());
        check(clean.grant(cleanCast, cleanTarget, -1, 1.0D) == 0, "负数基础量被拒绝");
        check(clean.grant(cleanCast, cleanTarget, 4, Double.NaN) == 0, "非数倍率被拒绝");
        check(clean.grant(cleanCast, cleanTarget, 4, -2.0D) == 0, "负数倍率被拒绝");
        check(clean.grant(cleanCast, cleanTarget, 4, Double.POSITIVE_INFINITY) == 0, "无限倍率被拒绝");
        check(clean.grant(null, cleanTarget, 4, 1.0D) == 0, "没有施法上下文不生产");
        check(!cleanCast.hasProduced(cleanTarget), "非法生产不占用机会");
        check(clean.balance() == 20, "非法生产不改余额");
        check(clean.grant(cleanCast, cleanTarget, 1, 0.5D) == 0, "取整为 0 不增加");
        check(!cleanCast.hasProduced(cleanTarget), "取整为 0 不占用机会");
        check(clean.grant(cleanCast, cleanTarget, 9, 1.12D) == 10, "9×1.12 取整后增加 10");
        check(clean.balance() == 30, "取整后的增加写入余额");

        UUID sharedToken = UUID.randomUUID();
        UUID casterId = UUID.randomUUID();
        FireCastContext firstBook = new FireCastContext(sharedToken, casterId);
        FireCastContext secondBook = new FireCastContext(sharedToken, casterId);
        FireSeedAccount separate = new FireSeedAccount(0);
        UUID separateTarget = UUID.randomUUID();
        check(separate.grant(firstBook, separateTarget, 10, 1.0D) == 10, "第一本账可以生产");
        check(separate.grant(secondBook, separateTarget, 10, 1.0D) == 10, "同标识的新上下文不共享去重");
        check(separate.balance() == 20, "两本账各自增加");
        check(separate.grant(firstBook, separateTarget, 10, 1.0D) == 0, "复用同一对象时第二次被拒绝");
    }

    private static void consumption() {
        UUID targetId = UUID.randomUUID();
        UUID casterId = UUID.randomUUID();
        FireCastContext cast = new FireCastContext(UUID.randomUUID(), casterId);
        FireSeedAccount account = new FireSeedAccount(19);
        Optional<FireSeedSnapshot> previewed = account.preview(cast, targetId, Integer.MAX_VALUE, 0.30D);
        check(previewed.isPresent(), "预览全部余额");
        check(previewed.orElseThrow().amount() == 19, "预览投入是 19");
        check(previewed.orElseThrow().tiers() == 1 && previewed.orElseThrow().remainder() == 9, "19 的档位和余数");
        check(account.balance() == 19, "预览不改余额");
        check(!cast.hasConsumed(targetId), "预览不标记消费");
        check(!cast.hasProduced(targetId), "预览不标记生产");

        int spend = FireSeedMath.wholeTierSpend(account.balance());
        Optional<FireSeedSnapshot> spent = account.consume(cast, targetId, spend, 0.30D);
        check(spend == 10 && spent.isPresent(), "19 点旧档案扣 10");
        FireSeedSnapshot wholeTier = spent.orElseThrow();
        check(wholeTier.amount() == 10 && wholeTier.tiers() == 1 && wholeTier.remainder() == 0, "整档快照没有把余数算进投入");
        check(wholeTier.furnaceBonus() == 0.30D, "快照记下炉心加成");
        check(wholeTier.casterId().equals(casterId), "快照记下施法者");
        check(account.balance() == 9, "余数 9 留在余额");
        check(cast.hasConsumed(targetId), "整档消费已标记");
        check(account.consume(cast, targetId, 9, 0.0D).isEmpty(), "同一施法不能再扣余数");
        check(account.balance() == 9, "拒绝的第二次消费不改余数");
        check(account.grant(cast, targetId, 5, 1.0D) == 0, "已消费后不再生产");
        check(!cast.hasProduced(targetId), "被拒绝的生产不另记一次生产");
        check(account.balance() == 9, "被拒绝的生产不改余数");
        check(account.preview(cast, targetId, Integer.MAX_VALUE, 0.0D).isEmpty(), "已消费后预览为空");

        FireCastContext later = new FireCastContext(UUID.randomUUID(), casterId);
        Optional<FireSeedSnapshot> rest = account.consume(later, targetId, 9, 0.0D);
        check(rest.isPresent() && rest.orElseThrow().amount() == 9, "新施法可以扣下余数");
        check(rest.orElseThrow().tiers() == 0 && rest.orElseThrow().remainder() == 9, "不足一档的全部消费余数就是本身");
        check(account.balance() == 0, "余数扣完后余额为 0");
        check(wholeTier.amount() == 10 && wholeTier.remainder() == 0, "后一次消费不改先前快照");

        FireSeedAccount full = new FireSeedAccount(19);
        UUID fullTarget = UUID.randomUUID();
        FireCastContext fullCast = new FireCastContext(UUID.randomUUID(), null);
        Optional<FireSeedSnapshot> everything = full.consume(fullCast, fullTarget, Integer.MAX_VALUE, 0.0D);
        check(everything.isPresent(), "明确全部消费");
        check(everything.orElseThrow().amount() == 19 && everything.orElseThrow().remainder() == 9, "全部消费带走余数");
        check(full.balance() == 0, "全部消费后没有余额");
        check(fullCast.casterId() == null && everything.orElseThrow().casterId() == null, "允许没有施法者");

        FireSeedAccount empty = new FireSeedAccount(0);
        UUID emptyTarget = UUID.randomUUID();
        FireCastContext emptyCast = new FireCastContext(UUID.randomUUID(), UUID.randomUUID());
        check(empty.consume(emptyCast, emptyTarget, Integer.MAX_VALUE, 0.0D).isEmpty(), "余额 0 不消费");
        check(!emptyCast.hasConsumed(emptyTarget), "失败的消费不占机会");
        check(empty.grant(emptyCast, emptyTarget, 10, 1.0D) == 10, "没扣成时后来仍可生产");

        FireSeedAccount partial = new FireSeedAccount(19);
        UUID partialTarget = UUID.randomUUID();
        FireCastContext partialCast = new FireCastContext(UUID.randomUUID(), UUID.randomUUID());
        check(partial.consume(partialCast, partialTarget, 0, 0.0D).isEmpty(), "请求 0 不消费");
        check(partial.consume(partialCast, partialTarget, -3, 0.0D).isEmpty(), "负数请求不消费");
        check(partial.consume(partialCast, partialTarget, 10, Double.NaN).isEmpty(), "非数炉心不消费");
        check(partial.consume(partialCast, partialTarget, 10, -0.1D).isEmpty(), "负数炉心不消费");
        check(!partialCast.hasConsumed(partialTarget), "无效计划不标记");
        check(partial.balance() == 19, "无效计划不扣款");
        Optional<FireSeedSnapshot> fifteen = partial.consume(partialCast, partialTarget, 15, 0.30D);
        check(fifteen.isPresent() && fifteen.orElseThrow().amount() == 15, "请求 15 就扣 15，不乘炉心");
        check(fifteen.orElseThrow().tiers() == 1 && fifteen.orElseThrow().remainder() == 5, "15 点是 1 档余 5");
        check(partial.balance() == 4, "扣 15 后剩 4");
        check(partial.preview(partialCast, partialTarget, 4, 0.0D).isEmpty(), "重复预览不再给出计划");

        FireSeedAccount nine = new FireSeedAccount(9);
        UUID nineTarget = UUID.randomUUID();
        FireCastContext nineCast = new FireCastContext(UUID.randomUUID(), UUID.randomUUID());
        check(FireSeedMath.wholeTierSpend(9) == 0, "9 点没有整档");
        check(nine.consume(nineCast, nineTarget, FireSeedMath.wholeTierSpend(9), 0.0D).isEmpty(), "没有整档则不消费");
        check(!nineCast.hasConsumed(nineTarget) && nine.balance() == 9, "未消费的 9 点还在");

        FireSeedAccount produced = new FireSeedAccount(0);
        UUID producedTarget = UUID.randomUUID();
        FireCastContext producedCast = new FireCastContext(UUID.randomUUID(), UUID.randomUUID());
        check(produced.grant(producedCast, producedTarget, 25, 1.0D) == 25, "先生产 25");
        Optional<FireSeedSnapshot> afterGrant = produced.consume(producedCast, producedTarget, Integer.MAX_VALUE, 0.12D);
        check(afterGrant.isPresent(), "生产之后仍可消费一次");
        check(afterGrant.orElseThrow().amount() == 25 && afterGrant.orElseThrow().tiers() == 2
                && afterGrant.orElseThrow().remainder() == 5, "25 点是 2 档余 5");
        check(produced.balance() == 0, "生产出来的点数已扣完");
        check(produced.consume(producedCast, producedTarget, Integer.MAX_VALUE, 0.0D).isEmpty(), "消费不能重复");
        check(near(FireSeedArchive.BURST.yield(afterGrant.orElseThrow().tiers()), 0.08D), "两档引爆 0.08");
        check(FireSeedMath.sustainTicks(afterGrant.orElseThrow().tiers()) == 20, "两档持续加 20 tick");
        check(near(FireSeedArchive.SPREAD.yield(afterGrant.orElseThrow().tiers()), 0.20D), "两档扩散 0.20");
    }

    private static void sharedBalance() {
        UUID targetId = UUID.randomUUID();
        FireSeedAccount shared = new FireSeedAccount(40);
        FireCastContext firstCast = new FireCastContext(UUID.randomUUID(), UUID.randomUUID());
        FireCastContext secondCast = new FireCastContext(UUID.randomUUID(), UUID.randomUUID());
        check(shared.grant(firstCast, targetId, 30, 1.0D) == 30, "先到的施法增加 30");
        check(shared.balance() == 70, "共享余额变成 70");
        check(shared.grant(secondCast, targetId, 40, 1.0D) == 30, "后到的施法只能补到 100");
        check(shared.balance() == 100, "两人共用上限");
        check(shared.grant(firstCast, targetId, 5, 1.0D) == 0, "先到者不能用同一施法再生产");
        Optional<FireSeedSnapshot> secondSpend = shared.consume(secondCast, targetId, 60, 0.0D);
        check(secondSpend.isPresent() && secondSpend.orElseThrow().amount() == 60, "后到者按当前余额扣 60");
        check(shared.balance() == 40, "共享余额剩 40");
        Optional<FireSeedSnapshot> firstSpend = shared.consume(firstCast, targetId, 50, 0.0D);
        check(firstSpend.isPresent() && firstSpend.orElseThrow().amount() == 40, "先到者后来只能扣剩余 40");
        check(shared.balance() == 0, "共享余额被按顺序用完");
        check(shared.consume(secondCast, targetId, 1, 0.0D).isEmpty(), "后到者不能重复消费");
        check(shared.grant(secondCast, targetId, 8, 1.0D) == 0, "后到者消费后不能再生产");
        check(shared.balance() == 0, "拒绝的生产没有把余额加回来");

        FireSeedAccount firstTargetAccount = new FireSeedAccount(0);
        FireSeedAccount secondTargetAccount = new FireSeedAccount(0);
        UUID firstTarget = UUID.randomUUID();
        UUID secondTarget = UUID.randomUUID();
        FireCastContext cast = new FireCastContext(UUID.randomUUID(), UUID.randomUUID());
        check(firstTargetAccount.grant(cast, firstTarget, 15, 1.0D) == 15, "第一个目标生产");
        check(secondTargetAccount.grant(cast, secondTarget, 15, 1.0D) == 15, "第二个目标独立生产");
        check(firstTargetAccount.consume(cast, firstTarget, Integer.MAX_VALUE, 0.0D).isPresent(), "只消费第一个目标");
        check(firstTargetAccount.balance() == 0 && secondTargetAccount.balance() == 15, "另一个目标余额还在");
        check(cast.hasConsumed(firstTarget) && !cast.hasConsumed(secondTarget), "消费标记按目标分开");
        check(secondTargetAccount.grant(cast, secondTarget, 5, 1.0D) == 0, "第二个目标的生产机会已经用过");
        check(firstTargetAccount.grant(cast, firstTarget, 5, 1.0D) == 0, "已消费目标不再生产");
        FireCastContext freshCast = new FireCastContext(UUID.randomUUID(), UUID.randomUUID());
        check(secondTargetAccount.consume(freshCast, secondTarget, 10, 0.0D).isPresent(), "新施法可以扣第二个目标");
        check(secondTargetAccount.balance() == 5, "第二个目标按自己的余额结算");
        check(firstTargetAccount.balance() == 0, "新施法没有动第一个目标");
    }

    private static void snapshots() {
        UUID castId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        FireSeedSnapshot left = FireSeedSnapshot.of(castId, null, targetId, 19, 0.0D);
        FireSeedSnapshot right = FireSeedSnapshot.of(castId, null, targetId, 19, 0.0D);
        check(left.equals(right), "相同输入得到相等快照");
        check(left.amount() == 19 && left.tiers() == 1 && left.remainder() == 9, "工厂按投入拆档");
        FireSeedAccount account = new FireSeedAccount(19);
        account.grant(new FireCastContext(UUID.randomUUID(), null), targetId, 1, 1.0D);
        check(left.amount() == 19 && left.remainder() == 9, "余额变化不改已生成快照");

        rejects(() -> new FireSeedSnapshot(castId, null, targetId, 19, 2, 9, 0.0D), "档位和余数不一致被拒绝");
        rejects(() -> new FireSeedSnapshot(castId, null, targetId, 0, 0, 0, 0.0D), "0 点不能成为成功快照");
        rejects(() -> new FireSeedSnapshot(castId, null, targetId, 101, 10, 1, 0.0D), "超过 100 的快照被拒绝");
        rejects(() -> new FireSeedSnapshot(null, null, targetId, 10, 1, 0, 0.0D), "没有施法标识的快照被拒绝");
        rejects(() -> new FireSeedSnapshot(castId, null, null, 10, 1, 0, 0.0D), "没有目标的快照被拒绝");
        rejects(() -> new FireSeedSnapshot(castId, null, targetId, 10, 1, 0, Double.NaN), "非数炉心快照被拒绝");
        rejects(() -> new FireSeedSnapshot(castId, null, targetId, 10, 1, 0, -1.0D), "负数炉心快照被拒绝");
        rejects(() -> new FireSeedSnapshot(castId, null, targetId, 10, 1, 0, Double.POSITIVE_INFINITY), "无限炉心快照被拒绝");
        rejects(() -> new FireCastContext(null, UUID.randomUUID()), "没有施法标识不能建账");
        check(new FireCastContext(castId, null).casterId() == null, "账本允许没有施法者");
    }

    private static void sourceShape() {
        try {
            String state = Files.readString(SEED_ROOT.resolve("FireSeedState.java"), StandardCharsets.UTF_8);
            String lifecycle = Files.readString(SEED_ROOT.resolve("FireSeedLifecycle.java"), StandardCharsets.UTF_8);
            String resources = Files.readString(RESOURCE_STATUSES, StandardCharsets.UTF_8);
            String effects = Files.readString(EFFECT_REGISTRY, StandardCharsets.UTF_8);
            String snapshot = Files.readString(SEED_ROOT.resolve("FireSeedSnapshot.java"), StandardCharsets.UTF_8);
            String account = Files.readString(SEED_ROOT.resolve("FireSeedAccount.java"), StandardCharsets.UTF_8);
            check(state.contains("isSameThread") && state.contains("isClientSide"), "事务要求服务端主线程");
            check(state.contains("AttributeStateService.read") && state.contains("AttributeStateService.write"),
                    "火种复用属性状态读写");
            check(state.contains("target.isAlive()"), "预览扣点和生产要求目标存活");
            check(!state.contains("new FireCastContext"), "状态入口不另造施法账本");
            check(!account.contains("new FireCastContext"), "纯账户不另造施法账本");
            check(countOf(state, "persist(") == 4, "扣点、按段抽取和生产会回写");
            check(snapshot.contains("public record FireSeedSnapshot"), "快照是不可变记录");
            check(lifecycle.contains("LivingDeathEvent") && lifecycle.contains("isCanceled"), "未取消的死亡才清理");
            check(lifecycle.contains("isWasDeath") && lifecycle.contains("retainLivingClone"), "死亡克隆与存活克隆分开");
            check(lifecycle.contains("LivingConversionEvent"), "存活转化补回火种");
            check(!lifecycle.contains("grant(") && !lifecycle.contains("consume("), "生命周期不产消火种");
            check(resources.contains("FIRE_SEED") && resources.contains("FireSeedState.value"), "资源表只读火种数值");
            check(resources.contains("FireSeedMath.tiers") && resources.contains("UNLIMITED_DURATION"),
                    "资源视图带档位和无限时长");
            check(!resources.contains("1000"), "火种视图不借用潮湿 50 秒");
            check(!resources.contains(".grant(") && !resources.contains("AttributeStateService.write"),
                    "资源读取不写火种");
            check(!effects.contains("fire_seed"), "火种不注册药水");
            try (Stream<Path> listing = Files.list(SEED_ROOT)) {
                for (Path sourceFile : listing.filter(candidate -> candidate.toString().endsWith(".java")).toList()) {
                    String text = Files.readString(sourceFile, StandardCharsets.UTF_8);
                    check(!text.contains("MobEffect"), sourceFile.getFileName() + " 不引用药水");
                    check(!text.contains("addEffect"), sourceFile.getFileName() + " 不添加药水");
                    check(!text.contains("ModMobEffects"), sourceFile.getFileName() + " 不改药水注册");
                    check(!text.contains("ReleaseDefinitions"), sourceFile.getFileName() + " 不绑定释放表");
                    check(!text.contains("ModTechniques"), sourceFile.getFileName() + " 不绑定术式");
                    check(!text.contains("static Map"), sourceFile.getFileName() + " 没有全局账本表");
                    check(!text.contains("燎原") && !text.contains("燎里") && !text.contains("焚天"),
                            sourceFile.getFileName() + " 不写死术式");
                }
            }
        } catch (IOException failure) {
            check(false, "读取火种源码 " + failure);
        }
    }

    private static int countOf(String text, String token) {
        int found = 0;
        int cursor = 0;
        while (cursor >= 0 && cursor < text.length()) {
            int next = text.indexOf(token, cursor);
            if (next < 0) {
                break;
            }
            found++;
            cursor = next + token.length();
        }
        return found;
    }

    private static boolean near(double actual, double expected) {
        return Double.isFinite(actual) && Math.abs(actual - expected) <= 0.0000001D;
    }

    private static void rejects(Runnable body, String detail) {
        boolean rejected = false;
        try {
            body.run();
        } catch (IllegalArgumentException failure) {
            rejected = true;
        }
        check(rejected, detail);
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
}
