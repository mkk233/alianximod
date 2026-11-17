package com.bslx.XueXiMod.status.fire.furnace;

/**
 * 炉心进入之后是否收下这次效果。判失败时只回到调用前的效果和热量，不把失败报成成功。
 */
final class FurnaceInstall {
    enum Plan {
        ACCEPT,
        REINSTALL_PREVIOUS,
        REMOVE_CURRENT,
        STORE_ONLY
    }

    private FurnaceInstall() {
    }

    /** 加上去的是不是请求档位的无限炉心。 */
    static boolean accepted(boolean furnace, int appliedAmplifier, boolean infinite, int requestedAmplifier) {
        return furnace && infinite && appliedAmplifier == requestedAmplifier;
    }

    /** 收下、放回旧效果、去掉刚加上的效果，或只处理运行态。 */
    static Plan plan(boolean furnace, int appliedAmplifier, boolean infinite, int requestedAmplifier,
                     boolean hadEffect, boolean sameInstance, boolean effectPresent) {
        if (accepted(furnace, appliedAmplifier, infinite, requestedAmplifier)) {
            return Plan.ACCEPT;
        }
        if (hadEffect && !sameInstance) {
            return Plan.REINSTALL_PREVIOUS;
        }
        if (!hadEffect && effectPresent) {
            return Plan.REMOVE_CURRENT;
        }
        return Plan.STORE_ONLY;
    }

    /** 判失败时调用方必须用调用前的视图，不能留下刚加上的效果或改过的热量。 */
    static View blocked(View before, View added, int requestedAmplifier) {
        if (accepted(added.present(), added.amplifier(), added.infinite(), requestedAmplifier)) {
            return added;
        }
        return before;
    }

    static final class View {
        private final boolean present;
        private final int amplifier;
        private final boolean infinite;
        private final double bonus;

        View(boolean present, int amplifier, boolean infinite, double bonus) {
            this.present = present;
            this.amplifier = amplifier;
            this.infinite = infinite;
            this.bonus = bonus;
        }

        boolean present() {
            return this.present;
        }

        int amplifier() {
            return this.amplifier;
        }

        boolean infinite() {
            return this.infinite;
        }

        double bonus() {
            return this.bonus;
        }
    }
}
