package com.bslx.XueXiMod.combat.counter;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

/**
 * 中性反击来源。数据在 {@code data/xueximod/damage_type/counterattack.json}，不绕过无敌。
 * 调用标记打开时，这一线程上的任意来源都视为反击，避免嵌套 hurt 再消耗迎击窗口。
 */
public final class CounterDamage {
    public static final ResourceKey<DamageType> TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(XueXiMod.MOD_ID, "counterattack"));

    private static final ThreadLocal<int[]> MARK_DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    private CounterDamage() {
    }

    public static boolean isCounter(DamageSource source) {
        if (markActive()) {
            return true;
        }
        return source != null && source.is(TYPE);
    }

    public static boolean markActive() {
        return MARK_DEPTH.get()[0] > 0;
    }

    public static Mark open() {
        int[] depth = MARK_DEPTH.get();
        depth[0] += 1;
        return new Mark(depth[0]);
    }

    public static void close(Mark mark) {
        if (mark == null || !mark.open) {
            return;
        }
        mark.open = false;
        int[] depth = MARK_DEPTH.get();
        if (depth[0] == mark.depth) {
            depth[0] -= 1;
        }
    }

    /** 只清当前线程的反击标记。已经发出的 {@link Mark} 再关闭时对不上深度，不会把深度减成负数。 */
    static void clearCurrentThread() {
        MARK_DEPTH.get()[0] = 0;
    }

    public static final class Mark {
        private final int depth;
        private boolean open = true;

        private Mark(int depth) {
            this.depth = depth;
        }
    }
}
