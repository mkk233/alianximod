package com.bslx.XueXiMod.shu.technique.release.momentum;

import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierDefinition;
import com.bslx.XueXiMod.shu.technique.release.deflection.MitigationCreditScope;
import com.bslx.XueXiMod.shu.technique.release.guard.GuardDefinition;
import com.bslx.XueXiMod.shu.technique.release.payload.WetPressure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 调用仍能在没有玩家时执行的旧停用入口，并核对潮印固定 2/5/2。进入重水要有玩家，这里不伪造。
 */
public final class WetnessLegacyCheck {
    private static int assertionCount;
    private static int failures;

    private WetnessLegacyCheck() {
    }

    public static void main(String[] arguments) {
        section("重压空调用", WetnessLegacyCheck::pressure);
        section("重水空会话", WetnessLegacyCheck::heavy);
        section("潮印和基础减伤常数", WetnessLegacyCheck::marks);
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        System.out.println("未运行：HeavyWater.enter 需要 ServerPlayer。深海领域实体、旧预留释放和重压控制都没有生物。");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void pressure() {
        WetPressure.attach(null, null, true, 8);
        WetPressure.note(null, null, null);
        WetPressure.afterDamage(null, null, true);
        WetPressure.afterFlushed(null, null, true);
        WetPressure.clear(null);
        requireVoid("attach", Entity.class, UUID.class, boolean.class, int.class);
        requireVoid("note", UUID.class, com.bslx.XueXiMod.shu.technique.release.ReleaseBatch.Tally.class, LivingEntity.class);
        requireVoid("afterDamage", UUID.class, LivingEntity.class, boolean.class);
        requireVoid("afterFlushed", com.bslx.XueXiMod.shu.technique.release.ReleaseBatch.Tally.class, LivingEntity.class, boolean.class);
        requireVoid("clear", Entity.class);
    }

    private static void requireVoid(String methodName, Class<?>... parameterTypes) {
        try {
            Method method = WetPressure.class.getDeclaredMethod(methodName, parameterTypes);
            check(method.getReturnType() == void.class, methodName + " 仍是空返回的原签名");
        } catch (NoSuchMethodException failure) {
            check(false, methodName + " 签名不在：" + failure.getMessage());
        }
    }

    private static void heavy() {
        check(HeavyWater.current() == null, "当前没有重水会话");
        HeavyWater.commitFirst(null);
        HeavyWater.finish(null);
        HeavyWater.clearCurrent();
        check(HeavyWater.current() == null, "空提交、空结束和清当前之后仍没有会话");
    }

    private static void marks() {
        TideMarkSuccess one = TideMarkSuccess.of(1, new UUID(3L, 5L));
        check(one.flatDamage() == 2f && one.wetness() == 5 && one.momentum() == 2,
                "一条潮印仍是伤 2、湿 5、水势 2，实际伤 " + one.flatDamage()
                        + " 湿 " + one.wetness() + " 势 " + one.momentum());
        TideMarkSuccess three = TideMarkSuccess.of(3, new UUID(3L, 5L));
        check(three.flatDamage() == 6f && three.wetness() == 15 && three.momentum() == 6,
                "三条潮印按条数乘，不另加潮湿奖励");
        check(TideMarkSuccess.of(0, new UUID(1L, 1L)) == TideMarkSuccess.NONE, "零条潮印仍是空贡献");
        check(BarrierDefinition.curtain().attenuation() == 0.5f, "水幕衰减仍是 0.5");
        check(GuardDefinition.personalRing().unloadRatio() == 0.20f, "水环卸力仍是 0.20");
        check(!MitigationCreditScope.allowsMitigatedCredit(2f, false, true), "偏转部分卸力不把基础减伤记回水势");
        check(!MitigationCreditScope.allowsMitigatedCredit(2f, true, false), "溺水抑制仍关掉减伤回势");
        check(MitigationCreditScope.allowsMitigatedCredit(2f, false, false), "普通减伤路径仍可入账");
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
