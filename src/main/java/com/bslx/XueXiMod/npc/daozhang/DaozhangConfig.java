package com.bslx.XueXiMod.npc.daozhang;

import net.minecraftforge.common.ForgeConfigSpec;

/** 道长自己的服务端数值。缺项回这里的默认，不读玩家武器或法力。 */
public final class DaozhangConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.DoubleValue TALISMAN_DAMAGE = BUILDER
            .comment("灵符固定伤害。")
            .defineInRange("daozhang.talismanDamage", 6.0D, 0.0D, 1000.0D);
    private static final ForgeConfigSpec.DoubleValue FIRE_DAMAGE = BUILDER
            .comment("火系弹体固定伤害。")
            .defineInRange("daozhang.fireDamage", 7.0D, 0.0D, 1000.0D);
    private static final ForgeConfigSpec.DoubleValue PROJECTILE_SPEED = BUILDER
            .comment("弹体每刻速度，单位是方块。")
            .defineInRange("daozhang.projectileSpeed", 1.15D, 0.05D, 8.0D);
    private static final ForgeConfigSpec.IntValue CAST_COOLDOWN = BUILDER
            .comment("一次施法后的冷却，单位是刻。")
            .defineInRange("daozhang.castCooldownTicks", 36, 1, 1200);
    private static final ForgeConfigSpec.DoubleValue CAST_RANGE = BUILDER
            .comment("选敌和弹体最大射程，单位是方块。")
            .defineInRange("daozhang.castRange", 18.0D, 2.0D, 64.0D);
    private static final ForgeConfigSpec.IntValue PROJECTILE_LIFE = BUILDER
            .comment("弹体寿命，单位是刻。")
            .defineInRange("daozhang.projectileLifeTicks", 28, 1, 200);
    private static final ForgeConfigSpec.IntValue FIRE_SECONDS = BUILDER
            .comment("火系命中被接受后的着火秒数。")
            .defineInRange("daozhang.fireSeconds", 3, 0, 30);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private DaozhangConfig() {
    }

    public static float talismanDamage() {
        return TALISMAN_DAMAGE.get().floatValue();
    }

    public static float fireDamage() {
        return FIRE_DAMAGE.get().floatValue();
    }

    public static double projectileSpeed() {
        return PROJECTILE_SPEED.get();
    }

    public static int castCooldownTicks() {
        return CAST_COOLDOWN.get();
    }

    public static double castRange() {
        return CAST_RANGE.get();
    }

    public static int projectileLifeTicks() {
        return PROJECTILE_LIFE.get();
    }

    public static int fireSeconds() {
        return FIRE_SECONDS.get();
    }
}
