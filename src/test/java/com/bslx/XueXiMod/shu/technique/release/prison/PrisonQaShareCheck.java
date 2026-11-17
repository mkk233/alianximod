package com.bslx.XueXiMod.shu.technique.release.prison;

import com.bslx.XueXiMod.network.UncommittedShare;
import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.WaterReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalEffects;
import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalIntent;
import com.bslx.XueXiMod.shu.technique.release.conditional.WaterPrisonEffect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.UUID;

/**
 * 直接调用费用、复制合并和收据份额。不构造玩家，也不复制扣款公式。
 */
public final class PrisonQaShareCheck {
    private static int assertionCount;
    private static int failures;

    private PrisonQaShareCheck() {
    }

    public static void main(String[] arguments) {
        price();
        receipt();
        System.out.println("未执行 把退款打回玩家、准备占用和父会话锁：需要玩家或世界");
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void price() {
        WaterReleaseDefinition definition = WaterReleaseDefinition.conditional("water_prison", 40, WaterPrisonEffect.INSTANCE);
        check(definition.explicitManaCost() == 40, "显式魔力不是 40，实际 " + definition.explicitManaCost());
        check(ConditionalEffects.copiesCoalesce(definition), "水牢复制没有合并成一次");
        check(WaterPrisonEffect.INSTANCE.copiesCoalesce(), "条件效果自己没有合并复制");
        check(WaterPrisonEffect.INSTANCE.supportsContact(), "水牢没有沿用接触附魔");
        ReleaseRegistry.register(definition);
        Technique technique = new Technique("water_prison", "水牢", TechniqueType.MAGIC, 0f, 400, 20,
                "对准潮湿目标", 0, ShuAttribute.WATER, 0f);
        check(technique.getBaseDamage() == 0f, "水牢基础伤害不是 0");
        check(technique.getBaseCooldown() == 400, "水牢基础冷却不是 400，实际 " + technique.getBaseCooldown());
        check(technique.manaCost() == 40, "登记后的魔力不是 40，实际 " + technique.manaCost());
        int priced = pricedCopies(technique, 3);
        if (priced >= 0) {
            check(priced == 1, "三份复制计价不是 1，实际 " + priced);
        }
        UUID castToken = UUID.randomUUID();
        UUID first = ConditionalIntent.operationId(castToken, 2, 0, null);
        UUID second = ConditionalIntent.operationId(castToken, 2, 0, null);
        check(first != null && first.equals(second), "同一父释放的条件意图不稳定");
        check(operationParameterCount() == 4, "条件意图编号吃进了复制序号");
        System.out.println("EXECUTED price-copies-operation");
    }

    private static void receipt() {
        Method committed = findCommitted();
        if (committed == null) {
            check(false, "ADAPTATION_PENDING ManaPaymentReceipt 没有 committed(UUID,UUID,int,int)");
            return;
        }
        try {
            UUID payer = UUID.randomUUID();
            UUID staff = UUID.randomUUID();
            Object payment = committed.invoke(null, payer, staff, 40, 40);
            int before = refundable(payment);
            check(before == 80, "收据可退余额不是 80，实际 " + before);
            UncommittedShare settled = UncommittedShare.open((ManaPaymentReceipt) payment, 40);
            UncommittedShare sibling = UncommittedShare.open((ManaPaymentReceipt) payment, 40);
            settled.commitRetained();
            int afterCommit = refundable(payment);
            check(afterCommit == 40, "划走 40 后可退余额不是 40，实际 " + afterCommit);
            settled.commitRetained();
            check(refundable(payment) == afterCommit, "同一份额第二次划走又减了一笔");
            sibling.cancel(null);
            check(refundable(payment) == afterCommit, "未提交份额的取消把已提交的 40 退回了收据");
            withdrawGroup(payment);
            System.out.println("EXECUTED receipt-share");
        } catch (InvocationTargetException failure) {
            check(false, "收据调用抛出 " + failure.getCause());
        } catch (NoSuchFieldException failure) {
            check(false, "ADAPTATION_PENDING 收据字段不是 leftStaff/leftPlayer");
        } catch (ReflectiveOperationException failure) {
            check(false, "收据反射失败 " + failure.getClass().getSimpleName() + " " + failure.getMessage());
        }
    }

    private static void withdrawGroup(Object payment) throws ReflectiveOperationException {
        Class<?> budgetClass;
        try {
            budgetClass = Class.forName("com.bslx.XueXiMod.network.CastExecutionSession$GroupBudget");
        } catch (ClassNotFoundException failure) {
            check(false, "ADAPTATION_PENDING 没有 GroupBudget");
            return;
        }
        Constructor<?> creator = null;
        for (Constructor<?> candidate : budgetClass.getDeclaredConstructors()) {
            Class<?>[] parameters = candidate.getParameterTypes();
            if (parameters.length == 2 && parameters[0] == ManaPaymentReceipt.class && parameters[1] == int.class) {
                creator = candidate;
            }
        }
        Method commit = null;
        for (Method method : budgetClass.getDeclaredMethods()) {
            if ("commit".equals(method.getName()) && method.getParameterCount() == 1
                    && method.getParameterTypes()[0] == int.class) {
                commit = method;
            }
        }
        if (creator == null || commit == null) {
            check(false, "ADAPTATION_PENDING GroupBudget 没有 (收据,int) 构造或 commit(int)");
            return;
        }
        creator.setAccessible(true);
        commit.setAccessible(true);
        Method again = findCommitted();
        if (again == null) {
            check(false, "ADAPTATION_PENDING 第二次找不到 committed");
            return;
        }
        Object fresh = again.invoke(null, UUID.randomUUID(), UUID.randomUUID(), 40, 40);
        int before = refundable(fresh);
        Object budget = creator.newInstance(fresh, before);
        commit.invoke(budget, 40);
        check(refundable(fresh) == before - 40, "动作预算提交没有从父收据划走 40，剩余 " + refundable(fresh));
    }

    private static int pricedCopies(Technique technique, int copyCount) {
        try {
            Class<?> plan = Class.forName("com.bslx.XueXiMod.network.SkillChainPlan");
            Method chosen = null;
            for (Method method : plan.getDeclaredMethods()) {
                if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != int.class) {
                    continue;
                }
                Class<?>[] parameters = method.getParameterTypes();
                if (parameters.length == 2 && parameters[0] == Technique.class && parameters[1] == int.class
                        && "pricedcopies".equals(method.getName().toLowerCase())) {
                    chosen = method;
                }
            }
            if (chosen == null) {
                check(false, "ADAPTATION_PENDING SkillChainPlan 没有 pricedCopies(Technique,int)");
                return -1;
            }
            chosen.setAccessible(true);
            Object returned = chosen.invoke(null, technique, copyCount);
            return returned instanceof Integer boxed ? boxed : -1;
        } catch (ClassNotFoundException failure) {
            check(false, "ADAPTATION_PENDING 没有 SkillChainPlan");
            return -1;
        } catch (ReflectiveOperationException failure) {
            check(false, "复制计价反射失败 " + failure.getClass().getSimpleName());
            return -1;
        }
    }

    private static int operationParameterCount() {
        for (Method method : ConditionalIntent.class.getDeclaredMethods()) {
            if ("operationId".equals(method.getName())) {
                return method.getParameterCount();
            }
        }
        return -1;
    }

    private static Method findCommitted() {
        for (Method method : ManaPaymentReceipt.class.getDeclaredMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if ("committed".equals(method.getName()) && parameters.length == 4
                    && parameters[0] == UUID.class && parameters[1] == UUID.class
                    && parameters[2] == int.class && parameters[3] == int.class) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static int refundable(Object payment) throws ReflectiveOperationException {
        Field staffLeft = ManaPaymentReceipt.class.getDeclaredField("leftStaff");
        Field playerLeft = ManaPaymentReceipt.class.getDeclaredField("leftPlayer");
        staffLeft.setAccessible(true);
        playerLeft.setAccessible(true);
        return staffLeft.getInt(payment) + playerLeft.getInt(payment);
    }

    private static void check(boolean passed, String message) {
        assertionCount++;
        if (!passed) {
            failures++;
            System.out.println("失败 " + message);
        }
    }
}
