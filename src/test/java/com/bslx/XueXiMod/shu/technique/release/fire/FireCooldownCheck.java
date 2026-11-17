package com.bslx.XueXiMod.shu.technique.release.fire;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** 资源火伤与普通攻击交错时，冷却记录按已定策略保留。不织入 Mixin，也不启动游戏。 */
public final class FireCooldownCheck {
    private static final Path ROOT = Path.of("D:/alianximod");
    private static int assertionCount;
    private static int failures;

    private FireCooldownCheck() {
    }

    public static void main(String[] arguments) throws Exception {
        section("交错", FireCooldownCheck::interleave);
        section("钩子文本", FireCooldownCheck::hookText);
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        System.out.println("未执行：Mixin 织入、真实 hurt、护甲、抗性、火焰保护。没有启动游戏。");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void interleave() {
        HurtRecord record = new HurtRecord(8.0f, 15);
        record.elseBranch(true, 1.0f);
        check(record.lastHurt == 8.0f && record.invulnerableTime == 15, "第一次资源火伤不缩小受击记录，也不刷新无敌时间");
        record.elseBranch(true, 0.5f);
        check(record.lastHurt == 8.0f && record.invulnerableTime == 15, "连续资源火伤仍保留原来的 8 点");
        check(!record.normalAttack(6.0f) && record.lastHurt == 8.0f && record.invulnerableTime == 15,
                "无敌时间内更小的普通攻击被拒绝");
        check(record.normalAttack(10.0f) && record.lastHurt == 10.0f && record.invulnerableTime == 15,
                "更大的普通攻击只更新差量记录，不把无敌时间改成 20");
        record.elseBranch(false, 3.0f);
        check(record.lastHurt == 3.0f && record.invulnerableTime == 20, "非资源火伤的 else 分支仍写入受击记录和无敌时间");
    }

    private static final class HurtRecord {
        private float lastHurt;
        private int invulnerableTime;

        private HurtRecord(float lastHurt, int invulnerableTime) {
            this.lastHurt = lastHurt;
            this.invulnerableTime = invulnerableTime;
        }

        private void elseBranch(boolean fireResource, float amount) {
            if (FireCooldownPolicy.keepExistingHurt(fireResource)) {
                return;
            }
            this.lastHurt = amount;
            this.invulnerableTime = 20;
        }

        private boolean normalAttack(float amount) {
            if (this.invulnerableTime > 10) {
                if (amount <= this.lastHurt) {
                    return false;
                }
                this.lastHurt = amount;
                return true;
            }
            this.elseBranch(false, amount);
            return true;
        }
    }

    private static void hookText() throws Exception {
        String mixin = Files.readString(ROOT.resolve("src/main/java/com/bslx/XueXiMod/mixin/FireResourceHurtMixin.java"),
                StandardCharsets.UTF_8);
        String registry = Files.readString(ROOT.resolve("src/main/resources/xueximod.mixins.json"), StandardCharsets.UTF_8);
        String cooldown = Files.readString(ROOT.resolve(
                "src/main/resources/data/minecraft/tags/damage_type/bypasses_cooldown.json"), StandardCharsets.UTF_8);
        check(mixin.contains("hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"), "只挂钩 hurt");
        check(mixin.contains("lastHurt:F") && mixin.contains("ordinal = 1"), "lastHurt 只跳过 else 的第二处写入");
        check(mixin.contains("Lnet/minecraft/world/entity/LivingEntity;invulnerableTime:I")
                        && mixin.contains("ordinal = 0"),
                "无敌时间的 owner 是 LivingEntity，序号 0");
        check(!mixin.contains("world/entity/Entity;invulnerableTime"), "不再把无敌时间写成 Entity");
        check(mixin.contains("require = 1"), "两处重定向都要求命中");
        check(mixin.contains("DamageSource source") && !mixin.contains("currentHurt"),
                "重定向直接接收本次 hurt 的 DamageSource");
        check(registry.contains("FireHurtMixinPlugin"), "hurt 边界插件已登记");
        check(!mixin.contains("static DamageSource") && !mixin.contains("method = \"handleDamageEvent\""),
                "不缓存单个来源，也不挂钩伤害结算收尾");
        check(!mixin.contains("invulnerableTime = 0"), "不把无敌时间清零");
        check(registry.contains("\"FireResourceHurtMixin\""), "新钩子已登记");
        check(cooldown.contains("\"replace\": false") && cooldown.contains("xueximod:fire_resource"), "冷却标签仍保留资源火伤");
    }

    private static void section(String title, Checked body) throws Exception {
        int before = failures;
        body.run();
        System.out.println(title + (failures == before ? " 通过" : " 失败"));
    }

    @FunctionalInterface
    private interface Checked {
        void run() throws Exception;
    }

    private static void check(boolean condition, String label) {
        assertionCount += 1;
        if (!condition) {
            failures += 1;
            System.out.println("失败 " + label);
        }
    }
}
