package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.Technique;

import java.util.List;

/** 一次释放的绝对 tick。开始、末段开始、允许重放分开记，不共用一个整数。 */
final class CastTiming {
    static final int REJECTED = 1;
    static final int INSUFFICIENT = 2;

    private final int kind;
    private final int startedTick;
    private final int finishedTick;
    private final int replayTick;
    private final List<Technique> fired;
    private final boolean settlesLater;

    private CastTiming(int kind, int startedTick, int finishedTick, int replayTick, List<Technique> fired, boolean settlesLater) {
        this.kind = kind;
        this.startedTick = startedTick;
        this.finishedTick = finishedTick;
        this.replayTick = replayTick;
        this.fired = fired;
        this.settlesLater = settlesLater;
    }

    static CastTiming rejected() {
        return new CastTiming(REJECTED, 0, 0, 0, List.of(), false);
    }

    static CastTiming insufficient() {
        return new CastTiming(INSUFFICIENT, 0, 0, 0, List.of(), false);
    }

    static CastTiming accepted(int startedTick, int finishedTick, int replayTick, List<Technique> fired) {
        return new CastTiming(0, startedTick, finishedTick, replayTick, List.copyOf(fired), false);
    }

    /** 父会话已经接住。冷却和游标等这一发真正结束再记。 */
    static CastTiming deferred(int startedTick, List<Technique> fired) {
        return new CastTiming(0, startedTick, startedTick, startedTick, List.copyOf(fired), true);
    }

    boolean accepted() {
        return kind == 0;
    }

    boolean lacksMana() {
        return kind == INSUFFICIENT;
    }

    int startedTick() {
        return startedTick;
    }

    int finishedTick() {
        return finishedTick;
    }

    int replayTick() {
        return replayTick;
    }

    List<Technique> fired() {
        return fired;
    }

    boolean settlesLater() {
        return settlesLater;
    }
}
