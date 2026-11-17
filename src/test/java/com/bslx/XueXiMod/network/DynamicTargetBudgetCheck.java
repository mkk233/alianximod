package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import com.bslx.XueXiMod.shu.ModTechniques;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.chain.TechniqueChain;
import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalIntent;
import com.bslx.XueXiMod.shu.technique.release.conditional.TargetPayment;
import com.bslx.XueXiMod.shu.technique.release.conditional.WaterPrisonEffect;
import com.bslx.XueXiMod.shu.technique.release.statusquery.RelationPolicy;
import com.bslx.XueXiMod.shu.technique.release.statusquery.StatusQuery;
import com.bslx.XueXiMod.shu.technique.release.statusquery.StatusScanRequest;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.skill.SkillType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 直接调用生产的预扣、排序、子操作号和收据口袋。
 * 不构造玩家或世界，因此不执行 StatusScan.collect、MagicPool.spendInto、牢的 prepare/commit。
 * 指定 QA 在 compileJava 退出 0 后，用 JDK 17 按 run-prison-checks.ps1 的方式编译并运行本 main：
 * javac --release 17 -proc:none -cp build/classes/java/main;compile-classpath
 * 再 java -cp 测试类;main类;compile-classpath com.bslx.XueXiMod.network.DynamicTargetBudgetCheck
 * 不启动 Minecraft，不做 Mixin。
 */
public final class DynamicTargetBudgetCheck {
    private static int assertionCount;

    private DynamicTargetBudgetCheck() {
    }

    public static void main(String[] arguments) {
        ModTechniques.registerAll();
        prisonPriceStaysOffThePrepaidBill();
        fixedPricesStayPut();
        captureOrderPrefersTierThenDistanceThenSubject();
        targetOperationStaysStableAndAsymmetric();
        receiptPocketKeepsPrefix();
        firstStaffBindStaysPut();
        standalonePoolFitsTwoPrisons();
        queryRequestMatchesPrisonContract();
        System.out.println("断言 " + assertionCount);
        System.out.println("动态目标预算检查通过");
    }

    private static void prisonPriceStaysOffThePrepaidBill() {
        Technique prison = TechniqueManager.getById("water_prison");
        check(prison != null, "水牢已登记");
        check(prison.getBaseCooldown() == 400, "水牢基础冷却仍是 400");
        check(prison.getDescription().contains("十二格") && prison.getDescription().contains("四十"), "说明写成十二格和每座四十");
        check(WaterPrisonEffect.INSTANCE.unitManaCost() == 40, "水牢单价 40");
        check(WaterPrisonEffect.CAPTURE_RADIUS == 12.0D, "水牢半径 12");
        check(WaterPrisonEffect.INSTANCE.copiesCoalesce(), "一分为三仍然合并成一次");
        check(WaterPrisonEffect.INSTANCE.supportsContact(), "水牢仍承接接触附魔");
        check(SkillSegmentMath.isConditionalRelease(prison), "水牢按条件能力识别");
        check(!SkillSegmentMath.isConditionalRelease(ModTechniques.SWORD_THRUST), "刺击不是条件释放");

        Skill alone = skillOf("单独水牢", prison);
        List<ChainSegment> aloneSegments = TechniqueChain.split(alone);
        check(SkillSegmentMath.segmentManaCost(alone.getTechniques(), aloneSegments.get(0)) == 40, "定义上的显式 40 还在");
        check(SkillChainPlan.pressCommitCost(alone.getTechniques(), aloneSegments) == 0, "单独水牢预扣不含单价");

        Skill marked = skillOf("潮印水牢", ModTechniques.TIDE_MARK, prison);
        List<ChainSegment> markedSegments = TechniqueChain.split(marked);
        check(ModTechniques.TIDE_MARK.manaCost() == 2, "潮印魔力是 2");
        check(SkillSegmentMath.segmentManaCost(marked.getTechniques(), markedSegments.get(0)) == 42, "显式单价加附魔仍是 42");
        check(SkillChainPlan.pressCommitCost(marked.getTechniques(), markedSegments) == 2, "预扣只含这一次附魔");

        Skill tripled = skillOf("三份水牢", ModTechniques.RULE_TRIPLE_CAST, prison);
        check(SkillChainPlan.pressCommitCost(tripled.getTechniques(), TechniqueChain.split(tripled)) == 0, "一分为三不把单价乘三");

        Skill dualPrisons = skillOf("双重水牢", ModTechniques.RULE_DUAL_RELEASE, prison, prison);
        check(SkillChainPlan.pressCommitCost(dualPrisons.getTechniques(), TechniqueChain.split(dualPrisons)) == 0, "双重两段水牢预扣仍不含单价");
        CompiledReleaseStep dualStep = CompiledReleaseStep.compile(dualPrisons, dualPrisons.getTechniques(),
                TechniqueChain.split(dualPrisons), 0);
        check(dualStep != null && dualStep.members.size() == 2, "双重分成两个成员");
        check(dualStep.groupBill() == 0, "双重群体账单不含单价");
        check(dualStep.memberShares.get(0) == 0 && dualStep.memberShares.get(1) == 0, "每个成员份额都不含 40");

        Skill dashPrison = skillOf("动作水牢", ModTechniques.MOVEMENT_DASH, prison);
        CompiledReleaseStep dashStep = CompiledReleaseStep.compile(dashPrison, dashPrison.getTechniques(),
                TechniqueChain.split(dashPrison), 0);
        check(dashStep != null && dashStep.actionLed, "动作加水牢走动作会话");
        check(ModTechniques.MOVEMENT_DASH.manaCost() == 4, "冲刺魔力是 4");
        check(dashStep.actionShare == 4, "动作份额仍是冲刺自己的费用");
        check(dashStep.groupBill() == 0 && dashStep.memberShares.get(0) == 0, "动作会话的群体账单不含水牢单价");
    }

    private static void fixedPricesStayPut() {
        Skill thrust = skillOf("刺击", ModTechniques.SWORD_THRUST);
        check(ModTechniques.SWORD_THRUST.manaCost() == 12, "刺击魔力是 12");
        check(SkillChainPlan.pressCommitCost(thrust.getTechniques(), TechniqueChain.split(thrust)) == 12, "固定攻击预扣不变");

        Skill tripledThrust = skillOf("三份刺击", ModTechniques.RULE_TRIPLE_CAST, ModTechniques.SWORD_THRUST);
        check(SkillChainPlan.pressCommitCost(tripledThrust.getTechniques(), TechniqueChain.split(tripledThrust)) == 36,
                "一分为三仍把固定攻击乘三");

        Technique ring = TechniqueManager.getById("water_ring");
        check(ring != null, "水环已登记");
        Skill ringAlone = skillOf("水环", ring);
        check(SkillChainPlan.pressCommitCost(ringAlone.getTechniques(), TechniqueChain.split(ringAlone)) == 0, "水环预扣仍不含生成费");
        Skill dualRing = skillOf("双重水环", ModTechniques.RULE_DUAL_RELEASE, ring, ModTechniques.SWORD_THRUST);
        CompiledReleaseStep ringStep = CompiledReleaseStep.compile(dualRing, dualRing.getTechniques(),
                TechniqueChain.split(dualRing), 0);
        check(ringStep != null, "双重水环可以编译");
        check(ringStep.memberShares.get(0) == 8, "水环生成费 8 仍在成员份额里");
        check(ringStep.memberShares.get(1) == 12, "刺击成员份额仍是 12");
        check(ringStep.groupBill() == 20, "水环和刺击的群体账单不变");
    }

    private static void captureOrderPrefersTierThenDistanceThenSubject() {
        UUID nearer = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID farther = UUID.fromString("00000000-0000-0000-0000-000000000009");
        check(TargetPayment.captureOrder(4, 25.0D, farther, 1, 1.0D, nearer) < 0, "高湿档排在前面");
        check(TargetPayment.captureOrder(2, 1.0D, farther, 2, 9.0D, nearer) < 0, "同档更近的排在前面");
        check(TargetPayment.captureOrder(2, 4.0D, nearer, 2, 4.0D, farther) < 0, "同档同距离按编号稳定排序");
        check(TargetPayment.captureOrder(3, 4.0D, nearer, 3, 4.0D, nearer) == 0, "完全相同的快照顺序不变");
    }

    private static void targetOperationStaysStableAndAsymmetric() {
        UUID parent = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID firstTarget = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID secondTarget = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID firstChild = ConditionalIntent.targetOperationId(parent, firstTarget);
        UUID again = ConditionalIntent.targetOperationId(parent, firstTarget);
        UUID swapped = ConditionalIntent.targetOperationId(firstTarget, parent);
        UUID otherChild = ConditionalIntent.targetOperationId(parent, secondTarget);
        check(firstChild != null && firstChild.equals(again), "同一父操作和目标重入得到同一个子号");
        check(!firstChild.equals(otherChild), "同父不同目标得到不同子号");
        check(!firstChild.equals(swapped), "父和目标对调不会撞号");
        check(ConditionalIntent.targetOperationId(null, firstTarget) == null, "没有父操作号就不编号");
        check(ConditionalIntent.targetOperationId(parent, null) == null, "没有目标就不编号");
    }

    private static void receiptPocketKeepsPrefix() {
        UUID payer = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UUID staffOne = UUID.fromString("55555555-5555-5555-5555-555555555555");
        UUID staffTwo = UUID.fromString("66666666-6666-6666-6666-666666666666");
        ManaPaymentReceipt chained = ManaPaymentReceipt.committed(payer, staffOne, 30, 0);
        check(!chained.canAcceptSpend(staffTwo, 40, 0), "别的法杖不能追加进这张收据");
        check(!chained.appendSpend(staffTwo, 40, 0), "拒绝追加时收据不变");
        check(chained.prepaidBalance() == 30 && chained.dynamicBalance() == 0, "冲突追加没有改到预扣或目标口袋");
        check(chained.appendSpend(null, 0, 40), "玩家池的 40 可以追加");
        check(chained.prepaidBalance() == 30 && chained.dynamicBalance() == 40, "追加进目标口袋，预扣仍是 30");
        chained.retainShare(40);
        check(chained.dynamicBalance() == 0 && chained.prepaidBalance() == 30, "成牢划走的 40 不吃预扣");
        check(chained.state() == ManaPaymentReceipt.State.COMMITTED, "预扣还在时收据不标成已退回");
        check(staffOne.equals(chained.paymentStaff()), "已有法杖身份的收据退款仍找原法杖");
    }

    private static void firstStaffBindStaysPut() {
        UUID payer = UUID.fromString("88888888-8888-8888-8888-888888888888");
        UUID staffOne = UUID.fromString("99999999-9999-9999-9999-999999999999");
        UUID staffTwo = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        ManaPaymentReceipt openReceipt = ManaPaymentReceipt.committed(payer, null, 0, 0);
        check(openReceipt.paymentStaff() == null, "零预扣收据一开始没有法杖");
        check(openReceipt.appendSpend(null, 0, 40), "只扣玩家池时还不绑定法杖");
        check(openReceipt.paymentStaff() == null, "玩家池追加不写退款法杖");
        check(openReceipt.appendSpend(staffOne, 40, 0), "第一笔法杖扣款可以绑定");
        check(staffOne.equals(openReceipt.paymentStaff()), "退款身份采用第一次的法杖");
        check(openReceipt.dynamicBalance() == 80 && openReceipt.prepaidBalance() == 0, "首次绑定只进目标口袋");
        check(openReceipt.appendSpend(staffOne, 40, 0), "同一法杖可以再追加");
        check(staffOne.equals(openReceipt.paymentStaff()), "再次追加不改法杖身份");
        check(openReceipt.dynamicBalance() == 120, "同身份追加累加目标口袋");
        check(!openReceipt.canAcceptSpend(staffTwo, 40, 0), "绑定后不能混扣另一根法杖");
        check(!openReceipt.appendSpend(staffTwo, 10, 0), "冲突追加失败");
        check(staffOne.equals(openReceipt.paymentStaff()), "冲突后身份仍是第一根法杖");
        check(openReceipt.dynamicBalance() == 120 && openReceipt.prepaidBalance() == 0, "冲突没有改余额");
    }

    private static void standalonePoolFitsTwoPrisons() {
        UUID payer = UUID.fromString("77777777-7777-7777-7777-777777777777");
        ManaPaymentReceipt alone = ManaPaymentReceipt.committed(payer, null, 0, 0);
        int freePool = 100;
        int retainedMana = 0;
        int formed = 0;
        while (freePool >= WaterPrisonEffect.SUCCESS_MANA) {
            check(alone.appendSpend(null, 0, WaterPrisonEffect.SUCCESS_MANA), "空收据可以记下这一座");
            freePool -= WaterPrisonEffect.SUCCESS_MANA;
            alone.retainShare(WaterPrisonEffect.SUCCESS_MANA);
            retainedMana += WaterPrisonEffect.SUCCESS_MANA;
            formed++;
        }
        check(formed == 2, "100 点可用魔力成 2 座");
        check(retainedMana == 80 && freePool == 20, "两座划走 80，池里剩 20");
        check(alone.prepaidBalance() == 0 && alone.dynamicBalance() == 0, "划走后收据里不留可退的目标费用");

        ManaPaymentReceipt reserved = ManaPaymentReceipt.committed(payer, null, 0, 30);
        int available = 70;
        int reservedFormed = 0;
        if (available >= 40 && reserved.appendSpend(null, 0, 40)) {
            available -= 40;
            reserved.retainShare(40);
            reservedFormed++;
        }
        check(reservedFormed == 1 && available == 30, "已保留的 30 不能再拿来成牢");
        check(available < 40, "剩下的可用魔力不够第三座");
        check(reserved.prepaidBalance() == 30 && reserved.dynamicBalance() == 0, "链上预扣没有被目标费用划走");
    }

    private static void queryRequestMatchesPrisonContract() {
        StatusQuery query = StatusQuery.and(
                StatusQuery.resourceAtLeast(WaterPrisonEffect.WETNESS, 1),
                StatusQuery.effectActive(WaterPrisonEffect.WETNESS));
        StatusScanRequest request = new StatusScanRequest(
                WaterPrisonEffect.CAPTURE_RADIUS, query, RelationPolicy.hostileCapture(), true, true);
        check(query != null && request != null, "水牢查询按半径、潮湿和敌对捕获组装");
        check("xueximod".equals(WaterPrisonEffect.WETNESS.getNamespace())
                && "wetness".equals(WaterPrisonEffect.WETNESS.getPath()), "潮湿键是 xueximod:wetness");
    }

    private static Skill skillOf(String name, Technique... parts) {
        List<Technique> techniques = List.of(parts);
        List<ShuAttribute> embryos = new ArrayList<>();
        for (int partIndex = 0; partIndex < techniques.size(); partIndex++) {
            embryos.add(ShuAttribute.NONE);
        }
        return new Skill(name, SkillType.MAGIC, techniques, embryos);
    }

    private static void check(boolean passed, String label) {
        assertionCount++;
        if (!passed) {
            throw new IllegalStateException(label);
        }
    }
}
