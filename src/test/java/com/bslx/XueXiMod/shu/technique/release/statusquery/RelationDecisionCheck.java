package com.bslx.XueXiMod.shu.technique.release.statusquery;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 用真实分类函数和未初始化的原版类层次核对敌对规则。不构造生物，也不启动游戏。
 */
public final class RelationDecisionCheck {
    private static final Path DECISION = Path.of(
            "D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/statusquery/RelationDecision.java");
    private static final Path POLICY = Path.of(
            "D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/statusquery/RelationPolicy.java");
    private static int assertionCount;
    private static int failures;

    private RelationDecisionCheck() {
    }

    public static void main(String[] arguments) {
        section("纯判定", RelationDecisionCheck::decisions);
        section("类层次", RelationDecisionCheck::hierarchySection);
        section("策略", RelationDecisionCheck::policies);
        section("源码", RelationDecisionCheck::sourceSection);
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        System.out.println("未执行：真实实体的 isAngryAt、getTarget、自有宠物 UUID、死亡、旁观，以及 ServerPlayer.canHarmPlayer 和服务器 PvP 开关。没有启动游戏。");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void decisions() {
        check(RelationDecision.classify(true, true, true, true, true, true) == RelationDecision.SubjectKind.SELF,
                "自己优先于其余关系");
        check(RelationDecision.classify(false, true, true, true, true, true) == RelationDecision.SubjectKind.ALLY,
                "盟友优先于宠物、玩家和怪物");
        check(RelationDecision.classify(false, false, true, true, true, true) == RelationDecision.SubjectKind.OWNED,
                "自有宠物优先于中立和怪物");
        check(RelationDecision.classify(false, false, false, true, true, true) == RelationDecision.SubjectKind.PLAYER,
                "玩家优先于中立和怪物");
        check(RelationDecision.classify(false, false, false, false, true, true) == RelationDecision.SubjectKind.NEUTRAL,
                "中立优先于怪物继承");
        check(RelationDecision.classify(false, false, false, false, false, true) == RelationDecision.SubjectKind.ENEMY,
                "非中立的敌对怪物");
        check(RelationDecision.classify(false, false, false, false, false, false) == RelationDecision.SubjectKind.OTHER,
                "其余生物不是敌对");
        check(!RelationDecision.accepts(null, true, true), "空种类拒绝");
        RelationDecision.SubjectKind[] rejected = {
                RelationDecision.SubjectKind.SELF,
                RelationDecision.SubjectKind.ALLY,
                RelationDecision.SubjectKind.OWNED,
                RelationDecision.SubjectKind.OTHER
        };
        for (RelationDecision.SubjectKind kind : rejected) {
            check(!RelationDecision.accepts(kind, true, true), kind + " 即使在攻击且能伤害也拒绝");
        }
        check(!RelationDecision.accepts(RelationDecision.SubjectKind.PLAYER, true, false),
                "玩家在不能伤害时拒绝");
        check(RelationDecision.accepts(RelationDecision.SubjectKind.PLAYER, false, true),
                "允许伤害的玩家不要求正在攻击");
        check(!RelationDecision.accepts(RelationDecision.SubjectKind.NEUTRAL, false, true), "冷静中立拒绝");
        check(RelationDecision.accepts(RelationDecision.SubjectKind.NEUTRAL, true, false), "正在攻击的中立可收");
        check(RelationDecision.accepts(RelationDecision.SubjectKind.ENEMY, false, false), "敌对怪物不要求正在攻击");
        check(!RelationDecision.accepts(RelationDecision.SubjectKind.OTHER, true, true), "非盟友的其他生物不是敌对");
    }

    private static void hierarchySection() {
        try {
            ClassLoader loader = RelationDecision.class.getClassLoader();
            Class<?> neutralType = Class.forName("net.minecraft.world.entity.NeutralMob", false, loader);
            Class<?> enemyType = Class.forName("net.minecraft.world.entity.monster.Enemy", false, loader);
            Class<?> monsterType = Class.forName("net.minecraft.world.entity.monster.Monster", false, loader);
            expectKind(loader, "net.minecraft.world.entity.monster.EnderMan",
                    neutralType, enemyType, monsterType, RelationDecision.SubjectKind.NEUTRAL);
            expectKind(loader, "net.minecraft.world.entity.monster.ZombifiedPiglin",
                    neutralType, enemyType, monsterType, RelationDecision.SubjectKind.NEUTRAL);
            expectKind(loader, "net.minecraft.world.entity.animal.IronGolem",
                    neutralType, enemyType, monsterType, RelationDecision.SubjectKind.NEUTRAL);
            expectKind(loader, "net.minecraft.world.entity.animal.Bee",
                    neutralType, enemyType, monsterType, RelationDecision.SubjectKind.NEUTRAL);
            expectKind(loader, "net.minecraft.world.entity.monster.Zombie",
                    neutralType, enemyType, monsterType, RelationDecision.SubjectKind.ENEMY);
            expectKind(loader, "net.minecraft.world.entity.monster.Slime",
                    neutralType, enemyType, monsterType, RelationDecision.SubjectKind.ENEMY);
            expectKind(loader, "net.minecraft.world.entity.monster.piglin.PiglinBrute",
                    neutralType, enemyType, monsterType, RelationDecision.SubjectKind.ENEMY);
            expectKind(loader, "net.minecraft.world.entity.animal.Cow",
                    neutralType, enemyType, monsterType, RelationDecision.SubjectKind.OTHER);
        } catch (Throwable failure) {
            assertionCount++;
            failures++;
            System.out.println("失败 类层次 " + failure);
            failure.printStackTrace(System.out);
        }
    }

    private static void expectKind(
            ClassLoader loader,
            String binaryName,
            Class<?> neutralType,
            Class<?> enemyType,
            Class<?> monsterType,
            RelationDecision.SubjectKind expected) throws ClassNotFoundException {
        Class<?> creature = Class.forName(binaryName, false, loader);
        boolean neutralMob = neutralType.isAssignableFrom(creature);
        boolean enemyOrMonster = enemyType.isAssignableFrom(creature) || monsterType.isAssignableFrom(creature);
        RelationDecision.SubjectKind actual = RelationDecision.classify(
                false, false, false, false, neutralMob, enemyOrMonster);
        check(actual == expected, binaryName + " 期望 " + expected + " 实际 " + actual
                + " 中立=" + neutralMob + " 敌对=" + enemyOrMonster);
        if (actual == RelationDecision.SubjectKind.NEUTRAL) {
            check(!RelationDecision.accepts(actual, false, false), binaryName + " 冷静时不收");
            check(RelationDecision.accepts(actual, true, false), binaryName + " 正在攻击施术者时可收");
        }
        if (actual == RelationDecision.SubjectKind.ENEMY) {
            check(RelationDecision.accepts(actual, false, false), binaryName + " 不因未攻击而漏掉");
        }
        if (actual == RelationDecision.SubjectKind.OTHER) {
            check(!RelationDecision.accepts(actual, true, true), binaryName + " 不因非盟友而收");
        }
    }

    private static void policies() {
        RelationPolicy hostile = RelationPolicy.hostileCapture();
        check(!hostile.decide(RelationDecision.SubjectKind.NEUTRAL, false, true), "预设不收冷静中立");
        check(hostile.decide(RelationDecision.SubjectKind.NEUTRAL, true, false), "预设收正在攻击的中立");
        check(hostile.decide(RelationDecision.SubjectKind.ENEMY, false, false), "预设收敌对");
        check(!hostile.decide(RelationDecision.SubjectKind.PLAYER, true, false), "预设在不能伤害玩家时拒绝");
        check(hostile.decide(RelationDecision.SubjectKind.PLAYER, false, true), "预设在能伤害玩家时接收");
        check(!hostile.decide(RelationDecision.SubjectKind.ALLY, true, true), "预设不收盟友");
        check(!hostile.decide(RelationDecision.SubjectKind.OWNED, true, true), "预设不收自有宠物");
        check(!hostile.decide(RelationDecision.SubjectKind.OTHER, true, true), "预设不把其他生物当敌对");
        check(hostile.decide(RelationDecision.SubjectKind.ENEMY, false, false)
                        == RelationDecision.accepts(RelationDecision.SubjectKind.ENEMY, false, false),
                "预设绑定同一纯函数");
        RelationPolicy custom = RelationPolicy.using(
                (kind, angryAtCaster, canHarmPlayer) -> kind == RelationDecision.SubjectKind.ENEMY);
        check(!custom.decide(RelationDecision.SubjectKind.NEUTRAL, true, false), "其他策略可以不收中立");
        check(custom.decide(RelationDecision.SubjectKind.ENEMY, false, false), "其他策略仍可收敌对");
        boolean nullRejected = false;
        try {
            RelationPolicy.using(null);
        } catch (IllegalArgumentException failure) {
            nullRejected = true;
        }
        check(nullRejected, "空策略被拒绝");
    }

    private static void sourceSection() {
        try {
            String decision = Files.readString(DECISION, StandardCharsets.UTF_8);
            int selfAt = decision.indexOf("if (sameCreature)");
            int allyAt = decision.indexOf("if (allied)");
            int ownedAt = decision.indexOf("if (ownedByCaster)");
            int playerAt = decision.indexOf("if (player)");
            int neutralAt = decision.indexOf("if (neutralMob)");
            int enemyAt = decision.indexOf("if (enemyOrMonster)");
            check(selfAt >= 0 && selfAt < allyAt && allyAt < ownedAt && ownedAt < playerAt
                            && playerAt < neutralAt && neutralAt < enemyAt,
                    "分类顺序是自己、盟友、宠物、玩家、中立，然后才是敌对");
            check(decision.contains("Relations.ally"), "盟友走现有预留");
            check(decision.contains("TamableAnimal") && decision.contains("getOwnerUUID"), "自有宠物看主人标识");
            check(decision.contains("instanceof NeutralMob"), "中立用原版接口");
            check(decision.contains("instanceof Enemy") && decision.contains("instanceof Monster"),
                    "敌对同时认 Enemy 和 Monster");
            check(decision.contains("isAngryAt") && decision.contains("getTarget"), "正在攻击看愤怒或当前目标");
            check(!decision.contains("CompoundTag") && !decision.contains("HashSet"), "关系模块不另存社交名单");
            String policy = Files.readString(POLICY, StandardCharsets.UTF_8);
            check(policy.contains("RelationDecision.classify"), "策略先分类");
            check(policy.contains("RelationDecision::accepts"), "捕获预设绑定纯函数");
            check(policy.contains("canHarmPlayer"), "玩家是否可伤害走原版方法");
            check(policy.contains("isAlive") && policy.contains("isSpectator") && policy.contains("isRemoved"),
                    "死亡、旁观和已移除在策略入口排除");
            check(!policy.contains("isPvpAllowed"), "不在策略里另写一套允许开关");
        } catch (IOException failure) {
            assertionCount++;
            failures++;
            System.out.println("失败 读取关系源码 " + failure);
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
}
