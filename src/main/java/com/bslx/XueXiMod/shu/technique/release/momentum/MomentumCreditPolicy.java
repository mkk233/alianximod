package com.bslx.XueXiMod.shu.technique.release.momentum;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayDeque;

/** 只拦住这次溺水本身。嵌套进来的反击看自己的伤害来源，不跟着一起禁掉。 */
public final class MomentumCreditPolicy {
    private static final ThreadLocal<ArrayDeque<LivingEntity>> DROWNING = ThreadLocal.withInitial(ArrayDeque::new);

    private MomentumCreditPolicy() {
    }

    /** 记下这次正在结算溺水的实体。 */
    public static void pushDrown(LivingEntity entity) {
        if (entity != null) {
            DROWNING.get().push(entity);
        }
    }

    /** 退出一层溺水标记。没有标记时不动。 */
    public static void popDrown() {
        ArrayDeque<LivingEntity> stack = DROWNING.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    /** 伤害来源是溺水，且受害者就在当前溺水标记里，才拦住入账。 */
    public static boolean suppresses(LivingEntity victim, DamageSource source) {
        if (victim == null || source == null || !source.is(DamageTypeTags.IS_DROWNING)) {
            return false;
        }
        for (LivingEntity marked : DROWNING.get()) {
            if (marked == victim) {
                return true;
            }
        }
        return false;
    }
}
