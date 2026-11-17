package com.bslx.XueXiMod.shu.technique.release.deflection.melee;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayDeque;

/**
 * 只排除打开时记下的那个受害者和那一份伤害来源。
 * 换了受害者或换了来源对象，就不是这一段射线。
 * 关闭只拿掉自己的标记，乱序关闭也不会把另一层清掉。
 */
public final class NonPhysicalMeleeScope {
    private static final ThreadLocal<ArrayDeque<Mark>> MARKS = ThreadLocal.withInitial(ArrayDeque::new);

    private NonPhysicalMeleeScope() {
    }

    /** 记下这一段非物理伤害，不让它进入直接近战补充入口。 */
    public static Token open(LivingEntity victim, DamageSource damageSource) {
        return openIdentity(victim, damageSource);
    }

    /** 这对受害者和伤害来源是否正被排除。 */
    public static boolean excludesDirectMelee(LivingEntity victim, DamageSource damageSource) {
        return excludesIdentity(victim, damageSource);
    }

    static Token openIdentity(Object victimIdentity, Object sourceIdentity) {
        Mark mark = new Mark(victimIdentity, sourceIdentity);
        MARKS.get().addLast(mark);
        return new Token(mark);
    }

    static boolean excludesIdentity(Object victimIdentity, Object sourceIdentity) {
        if (victimIdentity == null || sourceIdentity == null) {
            return false;
        }
        for (Mark mark : MARKS.get()) {
            if (mark.victimIdentity == victimIdentity && mark.sourceIdentity == sourceIdentity) {
                return true;
            }
        }
        return false;
    }

    public static final class Token {
        private final Mark mark;
        private boolean closed;

        private Token(Mark mark) {
            this.mark = mark;
        }

        /** 只移除自己压入的那一层。 */
        public void close() {
            if (this.closed) {
                return;
            }
            this.closed = true;
            ArrayDeque<Mark> marks = MARKS.get();
            marks.remove(this.mark);
            if (marks.isEmpty()) {
                MARKS.remove();
            }
        }
    }

    private static final class Mark {
        private final Object victimIdentity;
        private final Object sourceIdentity;

        private Mark(Object victimIdentity, Object sourceIdentity) {
            this.victimIdentity = victimIdentity;
            this.sourceIdentity = sourceIdentity;
        }
    }
}
