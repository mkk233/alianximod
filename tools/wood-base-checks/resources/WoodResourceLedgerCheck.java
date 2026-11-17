package com.bslx.XueXiMod.status.wood.resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * 花瓣岁华账本的序列与不变量。不引入测试框架，也不访问 Minecraft。
 * 由 tools/wood-base-checks/resources/run-resource-checks.ps1 在统一收束后编译运行。
 */
public final class WoodResourceLedgerCheck {
    private static final String DIMENSION = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";

    private int checks;
    private int failures;

    public static void main(String[] arguments) {
        WoodResourceLedgerCheck checker = new WoodResourceLedgerCheck();
        try {
            Path root = arguments.length > 0 ? Path.of(arguments[0]) : Path.of("D:/alianximod");
            checker.run(root);
        } catch (Throwable failure) {
            failure.printStackTrace(System.out);
            System.exit(1);
        }
        System.out.println("断言 " + checker.checks + " 失败 " + checker.failures);
        System.out.println("游戏内验证 否");
        if (checker.failures != 0) {
            System.exit(1);
        }
    }

    private void run(Path root) throws IOException {
        checkBounds();
        checkIntervalIdentity();
        checkTickets();
        checkConsumeAndSplit();
        checkUltimate();
        checkFullCapOrder();
        checkConsumeFull();
        checkLifecycle();
        checkRoundTrip();
        checkBadData();
        checkStructure(root);
    }

    private void checkBounds() {
        check(WoodLimits.UNITS_PER_DEGREE == 60000, "一度不是 60000");
        check(WoodLimits.FULL_UNITS == 180000, "满额不是 180000");
        check(WoodLimits.BATCH_POTENTIAL == 60000, "批次潜力上限不是一度");
        check(FlowerKind.PEACH.ordinal() == 0 && FlowerKind.HIGAN.ordinal() == 5, "花谱序数被重排");
        check(FlowerKind.values().length == 6, "花谱不是六项");
        check(RemovalReason.values().length == 7, "移除原因数量变了");
        for (RemovalReason reason : RemovalReason.values()) {
            check(!reason.name().contains("NATURAL"), "出现了自然移除原因 " + reason.name());
        }
        SuihuaSnapshot repaired = SuihuaSnapshot.repairOrNull(100000, 100000, 0, 0, 0, 0);
        check(repaired != null && repaired.peach() == 100000 && repaired.bamboo() == 80000, "超限修复顺序不对");
        check(SuihuaSnapshot.repairOrNull(-1, 10, 0, 0, 0, 0) == null, "负数主人条目被留下了");
        check(SuihuaSnapshot.clamped(-5, 0, 0, 0, 0, 0).total() == 0, "客户端负数没有按零处理");
        boolean rejected = false;
        try {
            new SuihuaSnapshot(-1, 0, 0, 0, 0, 0);
        } catch (IllegalArgumentException exception) {
            rejected = true;
        }
        check(rejected, "非法快照被构造出来了");
    }

    private void checkIntervalIdentity() {
        check(WoodInterval.potential(1, 3, 0, 1) == 0, "1/3 第一段不是 0");
        check(WoodInterval.potential(1, 3, 1, 2) == 0, "1/3 第二段不是 0");
        check(WoodInterval.potential(1, 3, 2, 3) == 1, "1/3 第三段不是 1");
        check(WoodInterval.potential(2, 3, 0, 2) + WoodInterval.potential(2, 3, 2, 3) == 2, "2/3 分段不守恒");
        check(WoodInterval.potential(0, 5, 0, 5) == 0, "零潜力不是 0");
        check(WoodInterval.potential(60000, 1, 0, 1) == 60000, "单格满度不对");
        int wide = Integer.MAX_VALUE;
        int half = wide / 2;
        int wideSum = WoodInterval.potential(wide, wide, 0, half) + WoodInterval.potential(wide, wide, half, wide);
        check(wideSum == wide, "大整数区间不守恒");
        Random random = new Random(20261109L);
        for (int round = 0; round < 40; round++) {
            int effective = 1 + random.nextInt(400);
            int potential = random.nextInt(60001);
            int cursor = 0;
            int sum = 0;
            while (cursor < effective) {
                int step = 1 + random.nextInt(effective - cursor);
                sum += WoodInterval.potential(potential, effective, cursor, cursor + step);
                cursor += step;
            }
            check(sum == potential, "随机切分不守恒 effective=" + effective + " potential=" + potential);
        }
    }

    private void checkTickets() {
        UUID ownerId = UUID.randomUUID();
        WoodLedger ledger = WoodLedger.createEmpty();
        ProductionTicket forged = ProductionTicket.issue(UUID.randomUUID(), ownerId, ResourceOrigin.NORMAL_BLOOM, 1L, 1);
        CreateResult forgedResult = ledger.publishTicket(forged, 1, DIMENSION, 0L, List.of(sample(FlowerKind.PEACH, 4, 10, 50L, 1.0)));
        check(!forgedResult.accepted(), "伪造票据被发布了");
        check(ledger.exportImage().fragments.isEmpty(), "伪造票据留下了片段");

        ProductionTicket ticket = ledger.beginTicket(ownerId, ResourceOrigin.NORMAL_BLOOM, 3);
        check(ticket != null && ticket.serial() == 1L, "第一张票据编号不是 1");
        CreateResult late = ledger.publishTicket(ticket, 4, DIMENSION, 0L, List.of(sample(FlowerKind.PEACH, 4, 10, 50L, 1.0)));
        check(!late.accepted() && ledger.exportImage().fragments.isEmpty(), "过 tick 票据仍能发布");

        CreateResult first = publish(ledger, ownerId, ResourceOrigin.NORMAL_BLOOM, 5, sample(FlowerKind.PEACH, 4, 100, 50L, 1.0));
        check(first.accepted() && first.fragments().size() == 1, "正常发布失败");
        ProductionTicket again = ledger.beginTicket(ownerId, ResourceOrigin.NORMAL_BLOOM, 5);
        CreateResult second = ledger.publishTicket(again, 5, DIMENSION, 0L, List.of(sample(FlowerKind.PEACH, 4, 100, 50L, 1.0)));
        check(second.accepted(), "第二张新票据应该成功");
        CreateResult replay = ledger.publishTicket(again, 5, DIMENSION, 0L, List.of(sample(FlowerKind.PEACH, 4, 100, 50L, 1.0)));
        check(!replay.accepted(), "同一票据发布了两次");
        check(ledger.exportImage().batches.size() == 2, "重复发布改变了批次数量");

        CreateResult empty = publish(ledger, ownerId, ResourceOrigin.NORMAL_BLOOM, 5);
        check(empty.accepted() && empty.batchId() == null, "空发布没有消耗成一次空成功");
        check(ledger.exportImage().batches.size() == 2, "空发布留下了批次");

        CreateResult tooMuch = publish(ledger, ownerId, ResourceOrigin.NORMAL_BLOOM, 5,
                sample(FlowerKind.PEACH, 2, 30000, 50L, 1.0), sample(FlowerKind.BAMBOO, 2, 30001, 50L, 2.0));
        check(!tooMuch.accepted() && tooMuch.reject() == WoodReject.CAPACITY, "超过一度的批次没有被整批拒绝");
        check(ledger.exportImage().batches.size() == 2, "超限批次被写入了");

        WoodLedger.LotDraft[] tooManyRoots = new WoodLedger.LotDraft[WoodLimits.ROOTS_PER_BATCH + 1];
        for (int cursor = 0; cursor < tooManyRoots.length; cursor++) {
            tooManyRoots[cursor] = sample(FlowerKind.PEACH, 1, 0, 50L, 1.0);
        }
        CreateResult rootOverflow = publish(ledger, ownerId, ResourceOrigin.NORMAL_BLOOM, 5, tooManyRoots);
        check(!rootOverflow.accepted(), "257 根没有被拒绝");

        WoodLedger capped = WoodLedger.createEmpty();
        long firstSerial = -1L;
        int opened = 0;
        for (int cursor = 0; cursor < WoodLimits.OPEN_TICKETS_PER_OWNER; cursor++) {
            ProductionTicket open = capped.beginTicket(ownerId, ResourceOrigin.NORMAL_BLOOM, 1);
            check(open != null, "未发布票据提前到顶");
            if (open == null) {
                break;
            }
            if (firstSerial < 0L) {
                firstSerial = open.serial();
            }
            opened++;
        }
        check(opened == WoodLimits.OPEN_TICKETS_PER_OWNER, "未发布票据上限不是 256");
        check(capped.beginTicket(ownerId, ResourceOrigin.NORMAL_BLOOM, 1) == null, "第 257 张未发布票据被留下了");
        capped.tick(FrozenWorld.present(0L), 2);
        ProductionTicket afterSweep = capped.beginTicket(ownerId, ResourceOrigin.NORMAL_BLOOM, 2);
        check(afterSweep != null && afterSweep.serial() > firstSerial, "过 tick 后编号被复用");
        check(capped.exportImage().fragments.isEmpty(), "未发布票据变成了地面资源");
    }

    private void checkConsumeAndSplit() {
        UUID ownerId = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        WoodLedger ledger = WoodLedger.createEmpty();
        CreateResult created = publish(ledger, ownerId, ResourceOrigin.NORMAL_BLOOM, 1,
                sample(FlowerKind.PEACH, 10, 10, 100L, 1.0));
        check(created.accepted(), "分割样本发布失败");
        UUID fragmentId = created.fragments().get(0).fragmentId();
        SplitResult split = ledger.split(ownerId, fragmentId, 1, 4);
        check(split.accepted(), "合法分割失败");
        List<GroundFragmentView> views = visible(ledger, ownerId);
        check(views.size() == 2, "分割后不是两段");
        int remaining = 0;
        for (GroundFragmentView view : views) {
            remaining += view.remainingPotential();
        }
        check(remaining == 10, "分割后潜力不是 10");
        GroundFragmentView lowPiece = null;
        for (GroundFragmentView view : views) {
            if (view.fragmentId().equals(fragmentId)) {
                lowPiece = view;
            }
        }
        check(lowPiece != null && lowPiece.length() == 4 && lowPiece.revision() == 2, "低端片段没有留在原编号上");
        Optional<GroundConsumeReceipt> spent = ledger.consume(ownerId,
                List.of(new FragmentTake(fragmentId, lowPiece.revision(), 2)), preview -> true);
        check(spent.isPresent() && spent.get().potentialUnits() == 2, "前端两格潜力不是 2");
        remaining = 0;
        for (GroundFragmentView view : visible(ledger, ownerId)) {
            remaining += view.remainingPotential();
        }
        check(remaining == 8, "消费后剩余潜力不是 8");
        Optional<GroundConsumeReceipt> stale = ledger.consume(ownerId,
                List.of(new FragmentTake(fragmentId, 1, 1)), preview -> true);
        check(stale.isEmpty(), "旧修订号被接受了");
        check(sumPotential(ledger, ownerId) == 8, "旧修订号消费改了账");

        boolean[] called = new boolean[1];
        Optional<GroundConsumeReceipt> refused = ledger.consume(ownerId,
                List.of(new FragmentTake(fragmentId, 3, 1)), preview -> {
                    called[0] = true;
                    return false;
                });
        check(called[0] && refused.isEmpty() && sumPotential(ledger, ownerId) == 8, "ready 拒绝仍扣了账");
        Optional<GroundConsumeReceipt> thrown = ledger.consume(ownerId,
                List.of(new FragmentTake(fragmentId, 3, 1)), preview -> {
                    throw new IllegalStateException("stop");
                });
        check(thrown.isEmpty() && sumPotential(ledger, ownerId) == 8, "ready 抛错仍扣了账");
        int beforeReentry = sumPotential(ledger, ownerId);
        int beforeCount = visible(ledger, ownerId).size();
        Optional<GroundConsumeReceipt> reentered = ledger.consume(ownerId,
                List.of(new FragmentTake(fragmentId, 3, 1)), preview -> {
                    ledger.split(ownerId, fragmentId, preview.pieces().get(0).seenRevision(), 1);
                    return true;
                });
        GroundFragmentView afterReentry = null;
        for (GroundFragmentView view : visible(ledger, ownerId)) {
            if (view.fragmentId().equals(fragmentId)) {
                afterReentry = view;
            }
        }
        check(reentered.isEmpty() && sumPotential(ledger, ownerId) == beforeReentry
                        && visible(ledger, ownerId).size() == beforeCount
                        && afterReentry != null && afterReentry.revision() == 3,
                "ready 重入改了两次账");
        Optional<GroundConsumeReceipt> strangerTake = ledger.consume(stranger,
                List.of(new FragmentTake(fragmentId, 3, 1)), preview -> true);
        check(strangerTake.isEmpty() && sumPotential(ledger, ownerId) == beforeReentry, "别人消费了主人的片段");
        List<FragmentTake> duplicated = List.of(new FragmentTake(fragmentId, 3, 1), new FragmentTake(fragmentId, 3, 1));
        check(ledger.consume(ownerId, duplicated, preview -> true).isEmpty(), "重复片段整组没有拒绝");

        WoodLedger.TickReport awarded = ledger.tick(FrozenWorld.present(100L), 2);
        check(awarded.awardedUnits == 8, "到期入账不是剩余 8");
        check(ledger.snapshot(ownerId).peach() == 8, "岁华没有等于剩余潜力");
        check(visible(ledger, ownerId).isEmpty(), "到期后片段还在");
        WoodLedger.TickReport repeat = ledger.tick(FrozenWorld.present(1000L), 3);
        check(repeat.awardedUnits == 0 && ledger.snapshot(ownerId).peach() == 8, "重复 tick 再次发奖");
    }

    private void checkUltimate() {
        UUID ownerId = UUID.randomUUID();
        WoodLedger ledger = WoodLedger.createEmpty();
        publish(ledger, ownerId, ResourceOrigin.NORMAL_BLOOM, 1, sample(FlowerKind.PLUM, 1, 40, 0L, 1.0));
        ledger.tick(FrozenWorld.present(0L), 1);
        check(ledger.snapshot(ownerId).plum() == 40, "对照岁华没有入账");
        CreateResult ultimate = publish(ledger, ownerId, ResourceOrigin.ULTIMATE, 1,
                sample(FlowerKind.PEACH, 8, 500, 0L, 1.0));
        check(ultimate.accepted(), "终极批次被拒绝");
        GroundFragmentView view = visible(ledger, ownerId).get(0);
        check(view.origin() == ResourceOrigin.ULTIMATE && view.potentialUnits() == 0, "终极潜力没有强制为 0");
        check(view.remainingPotential() == 0, "终极区间仍有潜力");
        SplitResult split = ledger.split(ownerId, view.fragmentId(), view.revision(), 3);
        check(split.accepted(), "终极分割失败");
        for (GroundFragmentView piece : visible(ledger, ownerId)) {
            check(piece.potentialUnits() == 0 && piece.remainingPotential() == 0, "分割洗出了终极潜力");
        }
        ledger.tick(FrozenWorld.present(0L), 2);
        check(ledger.snapshot(ownerId).total() == 40, "终极凋谢发放了岁华");
        check(visible(ledger, ownerId).isEmpty(), "终极片段没有结束");
    }

    private void checkFullCapOrder() {
        UUID ownerId = UUID.randomUUID();
        WoodLedger ledger = WoodLedger.createEmpty();
        check(publish(ledger, ownerId, ResourceOrigin.NORMAL_BLOOM, 1,
                sample(FlowerKind.PEACH, 1, 60000, 5L, 1.0)).accepted(), "桃花批次失败");
        check(publish(ledger, ownerId, ResourceOrigin.NORMAL_BLOOM, 1,
                sample(FlowerKind.BAMBOO, 1, 60000, 5L, 1.0)).accepted(), "青竹批次失败");
        check(publish(ledger, ownerId, ResourceOrigin.NORMAL_BLOOM, 1,
                sample(FlowerKind.WISTERIA, 1, 59900, 5L, 1.0)).accepted(), "紫藤批次失败");
        check(publish(ledger, ownerId, ResourceOrigin.NORMAL_BLOOM, 1,
                sample(FlowerKind.HIGAN, 1, 80, 5L, 1.0),
                sample(FlowerKind.ORCHID, 1, 80, 5L, 2.0)).accepted(), "同批两根失败");
        ledger.tick(FrozenWorld.present(5L), 2);
        SuihuaSnapshot snapshot = ledger.snapshot(ownerId);
        check(snapshot.peach() == 60000 && snapshot.bamboo() == 60000 && snapshot.wisteria() == 59900,
                "满额截断没有按批次号保留前面的花谱");
        check(snapshot.higan() == 80 && snapshot.orchid() == 20 && snapshot.plum() == 0,
                "同批两根没有按根号截断");
        check(snapshot.total() == WoodLimits.FULL_UNITS, "满额合计不是三度");
        check(visible(ledger, ownerId).isEmpty(), "满额后片段被留下等以后补领");
        ledger.tick(FrozenWorld.present(5L), 3);
        check(ledger.snapshot(ownerId).equals(snapshot), "第二次 tick 改变了已满构成");
    }

    private void checkConsumeFull() {
        UUID ownerId = UUID.randomUUID();
        WoodLedger poor = WoodLedger.createEmpty();
        publish(poor, ownerId, ResourceOrigin.NORMAL_BLOOM, 1, sample(FlowerKind.PEACH, 1, 40, 0L, 1.0));
        poor.tick(FrozenWorld.present(0L), 1);
        boolean[] called = new boolean[1];
        check(poor.consumeFull(ownerId, snapshot -> {
            called[0] = true;
            return true;
        }).isEmpty(), "未满三度被清掉了");
        check(!called[0] && poor.snapshot(ownerId).peach() == 40, "未满仍调用了 ready 或改了账");

        WoodLedger ledger = fullLedger(ownerId);
        check(ledger.snapshot(ownerId).degrees() == 3, "三度样本没有满");
        boolean[] seen = new boolean[1];
        check(ledger.consumeFull(ownerId, snapshot -> {
            seen[0] = snapshot.degrees() == 3 && snapshot.total() == WoodLimits.FULL_UNITS;
            return false;
        }).isEmpty(), "ready 拒绝仍清了三度");
        check(seen[0] && ledger.snapshot(ownerId).total() == WoodLimits.FULL_UNITS, "拒绝后余额变了");
        check(ledger.consumeFull(ownerId, snapshot -> {
            throw new IllegalStateException("stop");
        }).isEmpty() && ledger.snapshot(ownerId).total() == WoodLimits.FULL_UNITS, "三度 ready 抛错仍扣账");
        check(ledger.consumeFull(ownerId, snapshot -> {
            ledger.consumeFull(ownerId, inner -> true);
            return true;
        }).isEmpty() && ledger.snapshot(ownerId).total() == WoodLimits.FULL_UNITS, "三度 ready 重入扣了账");
        Optional<SuihuaSnapshot> spent = ledger.consumeFull(ownerId, snapshot -> snapshot.total() == WoodLimits.FULL_UNITS);
        check(spent.isPresent() && spent.get().total() == WoodLimits.FULL_UNITS, "成功消费没有返回清零前快照");
        check(ledger.snapshot(ownerId).total() == 0, "成功后没有一次清零");
        check(ledger.consumeFull(ownerId, snapshot -> true).isEmpty(), "清零后还能再消费");
    }

    private void checkLifecycle() {
        UUID ownerId = UUID.randomUUID();
        WoodLedger ledger = WoodLedger.createEmpty();
        publish(ledger, ownerId, ResourceOrigin.NORMAL_BLOOM, 1, sample(FlowerKind.PEACH, 1, 40, 0L, 1.0));
        ledger.tick(FrozenWorld.present(0L), 1);
        publish(ledger, ownerId, ResourceOrigin.NORMAL_BLOOM, 1,
                sample(FlowerKind.BAMBOO, 4, 1000, 500L, 1.0),
                sample(FlowerKind.PLUM, 4, 1000, 500L, 20.0));
        publish(ledger, ownerId, ResourceOrigin.NORMAL_BLOOM, 1, nether(FlowerKind.ORCHID, 4, 1000));
        check(ledger.snapshot(ownerId).peach() == 40, "生命周期样本的既有岁华不对");
        WoodLedger.TickReport offline = ledger.tick(FrozenWorld.offline(500L), 2);
        check(offline.awardedUnits == 0 && ledger.snapshot(ownerId).peach() == 40, "主人不在线时入账了");
        check(ledger.exportImage().fragments.size() == 3, "离线 tick 把地面删了");

        GroundFragmentView bamboo = null;
        for (GroundFragmentView view : visible(ledger, ownerId)) {
            if (view.flower() == FlowerKind.BAMBOO) {
                bamboo = view;
            }
        }
        check(bamboo != null, "找不到竹片段");
        for (RemovalReason reason : RemovalReason.values()) {
            if (reason == RemovalReason.EXTERNAL) {
                continue;
            }
            RemoveResult rejected = ledger.discard(ownerId, bamboo.fragmentId(), bamboo.revision(), reason);
            check(!rejected.accepted() && rejected.reject() == WoodReject.REASON, "外部入口接受了 " + reason);
        }
        check(ledger.snapshot(ownerId).total() == 40, "错误原因改了岁华");
        check(ledger.discard(ownerId, bamboo.fragmentId(), bamboo.revision(), RemovalReason.EXTERNAL).accepted(),
                "外部丢弃失败");
        check(ledger.snapshot(ownerId).total() == 40, "外部丢弃发放了岁华");

        check(ledger.clearChunk(DIMENSION, 1, 0), "区块清理没有删掉坐标 20 的片段");
        check(ledger.snapshot(ownerId).total() == 40, "区块卸载发放了岁华");
        check(ledger.clearDimension(NETHER), "维度清理失败");
        check(ledger.snapshot(ownerId).total() == 40 && countDimension(ledger, NETHER) == 0, "维度卸载发放了岁华或没删干净");

        WoodLedger death = WoodLedger.createEmpty();
        publish(death, ownerId, ResourceOrigin.NORMAL_BLOOM, 1, sample(FlowerKind.PEACH, 1, 40, 0L, 1.0));
        death.tick(FrozenWorld.present(0L), 1);
        publish(death, ownerId, ResourceOrigin.NORMAL_BLOOM, 1, sample(FlowerKind.BAMBOO, 4, 1000, 500L, 1.0));
        check(death.clearDeath(ownerId), "死亡清理没有发生变化");
        check(death.snapshot(ownerId).total() == 0, "死亡没有清空岁华");
        check(death.exportImage().fragments.isEmpty(), "死亡留下了地面");
        death.tick(FrozenWorld.present(500L), 2);
        check(death.snapshot(ownerId).total() == 0, "死亡后又把地面补发成岁华");

        WoodLedger logout = WoodLedger.createEmpty();
        publish(logout, ownerId, ResourceOrigin.NORMAL_BLOOM, 1, sample(FlowerKind.PEACH, 1, 40, 0L, 1.0));
        logout.tick(FrozenWorld.present(0L), 1);
        publish(logout, ownerId, ResourceOrigin.NORMAL_BLOOM, 1, sample(FlowerKind.BAMBOO, 4, 1000, 500L, 1.0));
        check(logout.clearGround(ownerId), "登出没有清地面");
        check(logout.snapshot(ownerId).peach() == 40 && logout.exportImage().fragments.isEmpty(), "登出改了岁华或留下地面");
    }

    private void checkRoundTrip() {
        UUID ownerId = UUID.randomUUID();
        WoodLedger ledger = WoodLedger.createEmpty();
        publish(ledger, ownerId, ResourceOrigin.NORMAL_BLOOM, 1, sample(FlowerKind.PEACH, 6, 12, 80L, 1.0));
        GroundFragmentView view = visible(ledger, ownerId).get(0);
        check(ledger.consume(ownerId, List.of(new FragmentTake(view.fragmentId(), 1, 2)), preview -> true).isPresent(),
                "往返前的部分消费失败");
        WoodLedger.LedgerImage exported = ledger.exportImage();
        WoodLedger.LoadReport kept = WoodLedger.restore(exported, true);
        check(kept.ledger.exportImage().same(exported), "保留地面的往返丢失了区间或岁华");
        WoodLedger.LoadReport restarted = WoodLedger.restore(exported, false);
        check(restarted.discardedGround == exported.fragments.size(), "重启没有按条丢弃地面");
        check(restarted.ledger.exportImage().fragments.isEmpty(), "重启后地面还在");
        check(restarted.ledger.snapshot(ownerId).equals(ledger.snapshot(ownerId)), "重启丢失了岁华");
        restarted.ledger.tick(FrozenWorld.present(1_000_000L), 1);
        check(restarted.ledger.snapshot(ownerId).equals(ledger.snapshot(ownerId)), "重启后的 tick 补发了地面");
        ProductionTicket resumed = restarted.ledger.beginTicket(ownerId, ResourceOrigin.NORMAL_BLOOM, 1);
        check(resumed != null && resumed.serial() >= exported.nextSerial, "重启后复用了旧编号");
    }

    private void checkBadData() {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        WoodLedger.LoadReport negative = WoodLedger.restore(new WoodLedger.LedgerImage(WoodLimits.FORMAT_VERSION, 1L,
                List.of(new WoodLedger.OwnerImage(ownerId, -1, 10, 0, 0, 0, 0)),
                List.of(), List.of(), List.of()), false);
        check(negative.ledger.writable() && negative.ledger.snapshot(ownerId).total() == 0, "负数主人被当成余额");

        WoodLedger.LoadReport duplicated = WoodLedger.restore(new WoodLedger.LedgerImage(WoodLimits.FORMAT_VERSION, 1L,
                List.of(new WoodLedger.OwnerImage(ownerId, 10, 0, 0, 0, 0, 0),
                        new WoodLedger.OwnerImage(ownerId, 25, 0, 0, 0, 0, 0)),
                List.of(), List.of(), List.of()), false);
        check(duplicated.ledger.snapshot(ownerId).peach() == 10, "重复主人被加总了");

        WoodLedger.LoadReport trimmed = WoodLedger.restore(new WoodLedger.LedgerImage(WoodLimits.FORMAT_VERSION, 1L,
                List.of(new WoodLedger.OwnerImage(ownerId, 100000, 100000, 0, 0, 0, 0)),
                List.of(), List.of(), List.of()), false);
        check(trimmed.rewrite && trimmed.ledger.snapshot(ownerId).peach() == 100000
                && trimmed.ledger.snapshot(ownerId).bamboo() == 80000, "坏存档超限没有按花谱序截断");

        List<WoodLedger.OwnerImage> sealedOwners = new ArrayList<>();
        sealedOwners.add(new WoodLedger.OwnerImage(ownerId, 12, 0, 0, 0, 0, 0));
        WoodLedger.LedgerImage future = new WoodLedger.LedgerImage(99, 4L, sealedOwners, List.of(), List.of(), List.of());
        sealedOwners.clear();
        WoodLedger.LoadReport sealed = WoodLedger.restore(future, true);
        check(sealed.sealed && !sealed.ledger.writable(), "未知版本没有封存");
        check(sealed.ledger.snapshot(ownerId).total() == 0, "未知版本被解释成了余额");
        check(sealed.ledger.beginTicket(ownerId, ResourceOrigin.NORMAL_BLOOM, 1) == null, "未知版本仍能签发票据");
        check(future.owners.size() == 1 && sealed.ledger.exportImage().same(future), "未知版本原数据没有原样保留");

        UUID keptRoot = UUID.randomUUID();
        UUID overlapRoot = UUID.randomUUID();
        UUID batchKept = UUID.randomUUID();
        UUID batchOverlap = UUID.randomUUID();
        WoodLedger.LedgerImage overlap = new WoodLedger.LedgerImage(WoodLimits.FORMAT_VERSION, 5L,
                List.of(),
                List.of(batch(1L, batchKept, ownerId), batch(2L, batchOverlap, ownerId)),
                List.of(root(keptRoot, 1L, 1, FlowerKind.PEACH, 4, 7),
                        root(overlapRoot, 2L, 1, FlowerKind.BAMBOO, 10, 100)),
                List.of(fragment(UUID.randomUUID(), keptRoot, 1L, 1, 0, 4),
                        fragment(UUID.randomUUID(), overlapRoot, 2L, 1, 0, 5),
                        fragment(UUID.randomUUID(), overlapRoot, 2L, 1, 3, 8)));
        WoodLedger.LoadReport overlapLoaded = WoodLedger.restore(overlap, true);
        check(overlapLoaded.ledger.exportImage().fragments.size() == 1, "重叠区间没有整根作废");
        overlapLoaded.ledger.tick(FrozenWorld.present(1_000_000L), 1);
        check(overlapLoaded.ledger.snapshot(ownerId).peach() == 7 && overlapLoaded.ledger.snapshot(ownerId).bamboo() == 0,
                "坏区间发奖了");

        UUID sharedId = UUID.randomUUID();
        UUID singleRoot = UUID.randomUUID();
        WoodLedger.LedgerImage clones = new WoodLedger.LedgerImage(WoodLimits.FORMAT_VERSION, 3L,
                List.of(),
                List.of(batch(1L, UUID.randomUUID(), ownerId)),
                List.of(root(singleRoot, 1L, 1, FlowerKind.PEACH, 4, 20)),
                List.of(fragment(sharedId, singleRoot, 1L, 1, 0, 2), fragment(sharedId, singleRoot, 1L, 1, 2, 4)));
        WoodLedger.LoadReport cloned = WoodLedger.restore(clones, true);
        check(cloned.ledger.exportImage().fragments.isEmpty(), "重复片段编号被装进了账");
        cloned.ledger.tick(FrozenWorld.present(1_000_000L), 1);
        check(cloned.ledger.snapshot(ownerId).total() == 0, "重复编号发奖了");

        List<WoodLedger.RootImage> fatRoots = new ArrayList<>();
        List<WoodLedger.FragmentImage> fatFragments = new ArrayList<>();
        UUID fatBatch = UUID.randomUUID();
        for (int cursor = 1; cursor <= WoodLimits.ROOTS_PER_BATCH + 1; cursor++) {
            UUID rootId = UUID.randomUUID();
            fatRoots.add(root(rootId, 2L, cursor, FlowerKind.PEACH, 1, 0));
            fatFragments.add(fragment(UUID.randomUUID(), rootId, 2L, cursor, 0, 1));
        }
        UUID siblingRoot = UUID.randomUUID();
        WoodLedger.LedgerImage fat = new WoodLedger.LedgerImage(WoodLimits.FORMAT_VERSION, 3L, List.of(),
                List.of(batch(1L, UUID.randomUUID(), ownerId), batch(2L, fatBatch, ownerId)),
                joinRoots(root(siblingRoot, 1L, 1, FlowerKind.ORCHID, 1, 0), fatRoots),
                joinFragments(fragment(UUID.randomUUID(), siblingRoot, 1L, 1, 0, 1), fatFragments));
        WoodLedger.LoadReport fatLoaded = WoodLedger.restore(fat, true);
        check(fatLoaded.ledger.exportImage().fragments.size() == 1, "超限根的整批没有被拒绝");

        List<WoodLedger.BatchImage> manyBatches = new ArrayList<>();
        List<WoodLedger.RootImage> manyRoots = new ArrayList<>();
        List<WoodLedger.FragmentImage> manyFragments = new ArrayList<>();
        for (int serial = 1; serial <= WoodLimits.BATCHES_PER_OWNER + 1; serial++) {
            UUID rootId = UUID.randomUUID();
            manyBatches.add(batch(serial, UUID.randomUUID(), ownerId));
            manyRoots.add(root(rootId, serial, 1, FlowerKind.PEACH, 1, 0));
            manyFragments.add(fragment(UUID.randomUUID(), rootId, serial, 1, 0, 1));
        }
        WoodLedger.LoadReport manyLoaded = WoodLedger.restore(new WoodLedger.LedgerImage(WoodLimits.FORMAT_VERSION, 1L,
                List.of(), manyBatches, manyRoots, manyFragments), true);
        check(manyLoaded.ledger.exportImage().batches.size() == WoodLimits.BATCHES_PER_OWNER, "主人批次上限没有生效");
        boolean newestKept = false;
        for (WoodLedger.BatchImage batch : manyLoaded.ledger.exportImage().batches) {
            if (batch.serial == WoodLimits.BATCHES_PER_OWNER + 1L) {
                newestKept = true;
            }
        }
        check(!newestKept, "超限时丢掉的不是最新批次");

        WoodLedger.LoadReport burned = WoodLedger.restore(new WoodLedger.LedgerImage(WoodLimits.FORMAT_VERSION, 3L,
                List.of(), List.of(new WoodLedger.BatchImage(9L, null, null, "NORMAL_BLOOM", DIMENSION, 0L)),
                List.of(), List.of()), false);
        ProductionTicket resumed = burned.ledger.beginTicket(ownerId, ResourceOrigin.NORMAL_BLOOM, 1);
        check(resumed != null && resumed.serial() >= 10L, "坏批次编号被重新签发");

        WoodLedger.LoadReport overPotential = overPotential(ownerId, otherId);
        check(overPotential.ledger.exportImage().fragments.size() == 1, "潜力超限的批次没有整批丢掉");
        overPotential.ledger.tick(FrozenWorld.present(10L), 1);
        check(overPotential.ledger.snapshot(ownerId).peach() == 5, "超限批次的潜力被发放了");

        WoodLedger.LoadReport crowded = crowdedOwner(ownerId);
        check(crowded.ledger.exportImage().fragments.size() == WoodLimits.FRAGMENTS_PER_OWNER, "主人片段上限没有生效");
        CreateResult extra = publish(crowded.ledger, ownerId, ResourceOrigin.NORMAL_BLOOM, 1,
                sample(FlowerKind.PEACH, 1, 0, 50L, 3.0));
        check(!extra.accepted() && crowded.ledger.exportImage().fragments.size() == WoodLimits.FRAGMENTS_PER_OWNER,
                "已满片段后仍能发布");

        WoodLedger.LoadReport global = globalCap();
        int globalCount = global.ledger.exportImage().fragments.size();
        boolean extraSerialKept = false;
        for (WoodLedger.FragmentImage fragment : global.ledger.exportImage().fragments) {
            if (fragment.batchSerial == 17L) {
                extraSerialKept = true;
            }
        }
        check(globalCount == WoodLimits.FRAGMENTS_PER_SERVER && !extraSerialKept
                        && global.ledger.exportImage().owners.isEmpty(),
                "全服片段上限没有按最新批次丢弃");
    }

    private void checkStructure(Path root) throws IOException {
        Path resource = root.resolve("src/main/java/com/bslx/XueXiMod/status/wood/resource");
        String ledger = Files.readString(resource.resolve("WoodLedger.java"), StandardCharsets.UTF_8);
        check(countText(ledger, "acceptNatural(") == 2, "自然入账入口不是唯一一处");
        String saved = Files.readString(resource.resolve("WoodResourceSavedData.java"), StandardCharsets.UTF_8);
        check(saved.contains("WoodLedger.restore(image, false)"), "生产加载没有丢弃地面");
        check(saved.contains("xueximod_wood_resource"), "存档编号不对");
        String service = Files.readString(resource.resolve("WoodResourceService.java"), StandardCharsets.UTF_8);
        check(service.contains("isSameThread") && service.contains("hasChunkAt"), "服务没有校验主线程或已加载区块");
        Path view = root.resolve("src/main/java/com/bslx/XueXiMod/shu/technique/release/statusquery/ResourceView.java");
        String viewBody = Files.readString(view, StandardCharsets.UTF_8);
        check(viewBody.contains("record ResourceView(ResourceLocation identifier, int value, int tier, int durationTicks)"),
                "ResourceView 签名被改了");
        try (var walk = Files.walk(resource)) {
            for (Path file : walk.filter(path -> path.toString().endsWith(".java")).toList()) {
                String body = Files.readString(file, StandardCharsets.UTF_8);
                check(!body.contains("SubscribeEvent") && !body.contains("EventBusSubscriber")
                                && !body.contains("SimpleChannel") && !body.contains("net.minecraft.client"),
                        "资源目录出现了事件、网络或客户端类引用 " + file.getFileName());
            }
        }
        String removal = Files.readString(resource.resolve("RemovalReason.java"), StandardCharsets.UTF_8);
        check(!removal.contains("NATURAL"), "移除原因源码里出现了 NATURAL");
    }

    private CreateResult publish(WoodLedger ledger, UUID ownerId, ResourceOrigin origin, int tickCount,
                                 WoodLedger.LotDraft... drafts) {
        ProductionTicket ticket = ledger.beginTicket(ownerId, origin, tickCount);
        check(ticket != null, "签发失败");
        if (ticket == null) {
            return CreateResult.rejected(WoodReject.TICKET);
        }
        return ledger.publishTicket(ticket, tickCount, DIMENSION, 0L, Arrays.asList(drafts));
    }

    private WoodLedger fullLedger(UUID ownerId) {
        WoodLedger ledger = WoodLedger.createEmpty();
        publish(ledger, ownerId, ResourceOrigin.NORMAL_BLOOM, 1, sample(FlowerKind.PEACH, 1, 60000, 0L, 1.0));
        publish(ledger, ownerId, ResourceOrigin.NORMAL_BLOOM, 1, sample(FlowerKind.BAMBOO, 1, 60000, 0L, 1.0));
        publish(ledger, ownerId, ResourceOrigin.NORMAL_BLOOM, 1, sample(FlowerKind.WISTERIA, 1, 60000, 0L, 1.0));
        ledger.tick(FrozenWorld.present(0L), 1);
        return ledger;
    }

    private static WoodLedger.LotDraft sample(FlowerKind flower, int effective, int potential, long expire, double coordX) {
        return new WoodLedger.LotDraft(flower, effective, potential, 1L, expire, DIMENSION, coordX, 1.0, 1.0);
    }

    private static WoodLedger.LotDraft nether(FlowerKind flower, int effective, int potential) {
        return new WoodLedger.LotDraft(flower, effective, potential, 1L, 500L, NETHER, 1.0, 1.0, 1.0);
    }

    private List<GroundFragmentView> visible(WoodLedger ledger, UUID ownerId) {
        GroundQueryResult result = ledger.query(ownerId, DIMENSION, -100.0, -100.0, -100.0, 100.0, 100.0, 100.0, 1000);
        check(result.accepted(), "查询失败");
        List<GroundFragmentView> views = new ArrayList<>(result.fragments());
        GroundQueryResult nether = ledger.query(ownerId, NETHER, -100.0, -100.0, -100.0, 100.0, 100.0, 100.0, 1000);
        if (nether.accepted()) {
            views.addAll(nether.fragments());
        }
        return views;
    }

    private int sumPotential(WoodLedger ledger, UUID ownerId) {
        int sum = 0;
        for (GroundFragmentView view : visible(ledger, ownerId)) {
            sum += view.remainingPotential();
        }
        return sum;
    }

    private static int countDimension(WoodLedger ledger, String dimensionId) {
        int count = 0;
        for (WoodLedger.FragmentImage fragment : ledger.exportImage().fragments) {
            if (dimensionId.equals(fragment.dimensionId)) {
                count++;
            }
        }
        return count;
    }

    private static WoodLedger.BatchImage batch(long serial, UUID batchId, UUID ownerId) {
        return new WoodLedger.BatchImage(serial, batchId, ownerId, ResourceOrigin.NORMAL_BLOOM.name(), DIMENSION, 0L);
    }

    private static WoodLedger.RootImage root(UUID rootId, long batchSerial, int rootSerial, FlowerKind flower,
                                            int effective, int potential) {
        return new WoodLedger.RootImage(rootId, batchSerial, rootSerial, flower.name(), effective, potential, 10L);
    }

    private static WoodLedger.FragmentImage fragment(UUID fragmentId, UUID rootId, long batchSerial, int rootSerial,
                                                     int lowInclusive, int highExclusive) {
        return new WoodLedger.FragmentImage(fragmentId, rootId, batchSerial, rootSerial, DIMENSION,
                1.0, 1.0, 1.0, lowInclusive, highExclusive, 1);
    }

    private static List<WoodLedger.RootImage> joinRoots(WoodLedger.RootImage first, List<WoodLedger.RootImage> rest) {
        List<WoodLedger.RootImage> joined = new ArrayList<>();
        joined.add(first);
        joined.addAll(rest);
        return joined;
    }

    private static List<WoodLedger.FragmentImage> joinFragments(WoodLedger.FragmentImage first,
                                                               List<WoodLedger.FragmentImage> rest) {
        List<WoodLedger.FragmentImage> joined = new ArrayList<>();
        joined.add(first);
        joined.addAll(rest);
        return joined;
    }

    private static WoodLedger.LoadReport overPotential(UUID ownerId, UUID otherId) {
        UUID smallRoot = UUID.randomUUID();
        UUID leftRoot = UUID.randomUUID();
        UUID rightRoot = UUID.randomUUID();
        return WoodLedger.restore(new WoodLedger.LedgerImage(WoodLimits.FORMAT_VERSION, 4L, List.of(),
                List.of(batch(1L, UUID.randomUUID(), ownerId), batch(2L, UUID.randomUUID(), otherId)),
                List.of(root(smallRoot, 1L, 1, FlowerKind.PEACH, 1, 5),
                        root(leftRoot, 2L, 1, FlowerKind.BAMBOO, 1, 30000),
                        root(rightRoot, 2L, 2, FlowerKind.PLUM, 1, 30001)),
                List.of(fragment(UUID.randomUUID(), smallRoot, 1L, 1, 0, 1),
                        fragment(UUID.randomUUID(), leftRoot, 2L, 1, 0, 1),
                        fragment(UUID.randomUUID(), rightRoot, 2L, 2, 0, 1))), true);
    }

    private static WoodLedger.LoadReport crowdedOwner(UUID ownerId) {
        UUID rootId = UUID.randomUUID();
        int count = WoodLimits.FRAGMENTS_PER_OWNER + 1;
        List<WoodLedger.FragmentImage> fragments = new ArrayList<>();
        for (int cursor = 0; cursor < count; cursor++) {
            fragments.add(fragment(UUID.randomUUID(), rootId, 1L, 1, cursor, cursor + 1));
        }
        return WoodLedger.restore(new WoodLedger.LedgerImage(WoodLimits.FORMAT_VERSION, 20L, List.of(),
                List.of(batch(1L, UUID.randomUUID(), ownerId)),
                List.of(root(rootId, 1L, 1, FlowerKind.PEACH, count, 0)),
                fragments), true);
    }

    private static WoodLedger.LoadReport globalCap() {
        List<WoodLedger.BatchImage> batches = new ArrayList<>();
        List<WoodLedger.RootImage> roots = new ArrayList<>();
        List<WoodLedger.FragmentImage> fragments = new ArrayList<>();
        for (int ownerCursor = 1; ownerCursor <= 17; ownerCursor++) {
            UUID ownerId = UUID.randomUUID();
            UUID rootId = UUID.randomUUID();
            int count = ownerCursor == 17 ? 1 : WoodLimits.FRAGMENTS_PER_OWNER;
            batches.add(batch(ownerCursor, UUID.randomUUID(), ownerId));
            roots.add(root(rootId, ownerCursor, 1, FlowerKind.PEACH, count, 0));
            for (int piece = 0; piece < count; piece++) {
                fragments.add(fragment(UUID.randomUUID(), rootId, ownerCursor, 1, piece, piece + 1));
            }
        }
        return WoodLedger.restore(new WoodLedger.LedgerImage(WoodLimits.FORMAT_VERSION, 1000L,
                List.of(), batches, roots, fragments), true);
    }

    private static int countText(String body, String token) {
        int found = 0;
        int from = 0;
        while (from < body.length()) {
            int at = body.indexOf(token, from);
            if (at < 0) {
                return found;
            }
            found++;
            from = at + token.length();
        }
        return found;
    }

    private void check(boolean condition, String detail) {
        checks++;
        if (!condition) {
            failures++;
            System.out.println("失败 " + detail);
        }
    }

    private static final class FrozenWorld implements WoodLedger.WorldProbe {
        private final long frozenTime;
        private final boolean online;
        private final boolean alive;
        private final boolean sameDimension;
        private final boolean chunk;

        private FrozenWorld(long frozenTime, boolean online, boolean alive, boolean sameDimension, boolean chunk) {
            this.frozenTime = frozenTime;
            this.online = online;
            this.alive = alive;
            this.sameDimension = sameDimension;
            this.chunk = chunk;
        }

        private static FrozenWorld present(long frozenTime) {
            return new FrozenWorld(frozenTime, true, true, true, true);
        }

        private static FrozenWorld offline(long frozenTime) {
            return new FrozenWorld(frozenTime, false, false, false, true);
        }

        @Override
        public boolean ownerOnline(UUID ownerId) {
            return online;
        }

        @Override
        public boolean ownerAlive(UUID ownerId) {
            return alive;
        }

        @Override
        public boolean ownerInDimension(UUID ownerId, String dimensionId) {
            return sameDimension;
        }

        @Override
        public boolean chunkLoaded(String dimensionId, int chunkX, int chunkZ) {
            return chunk;
        }

        @Override
        public long gameTime(String dimensionId) {
            return frozenTime;
        }
    }
}
