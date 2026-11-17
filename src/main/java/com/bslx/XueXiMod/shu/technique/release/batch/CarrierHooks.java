package com.bslx.XueXiMod.shu.technique.release.batch;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 后续资源作者和载体作者的注册点。这里不引用尚未交付的类。
 * 没有监听器时，攻击仍走现有宿主提交；不会假装火种或地形已经发生。
 */
public final class CarrierHooks {
    public interface BirthListener {
        void onBorn(HostHandoff handoff);
    }

    public interface VerdictListener {
        void onVerdict(HostHandoff handoff, HitVerdict verdict);

        /** 带目标、载荷和根上下文的正式接缝。旧监听器不用实现。 */
        default void onResolved(ResolvedHit hit) {
        }
    }

    private static final List<BirthListener> BIRTH_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<VerdictListener> VERDICT_LISTENERS = new CopyOnWriteArrayList<>();

    private CarrierHooks() {
    }

    /** 登记出生监听。空监听忽略。 */
    public static void addBirthListener(BirthListener listener) {
        if (listener != null) {
            BIRTH_LISTENERS.add(listener);
        }
    }

    /** 登记结算监听。空监听忽略。 */
    public static void addVerdictListener(VerdictListener listener) {
        if (listener != null) {
            VERDICT_LISTENERS.add(listener);
        }
    }

    /** 按登记顺序通知出生。没有交接时不通知。 */
    public static void notifyBorn(HostHandoff handoff) {
        if (handoff == null) {
            return;
        }
        for (BirthListener listener : BIRTH_LISTENERS) {
            listener.onBorn(handoff);
        }
    }

    /** 按登记顺序通知旧的结算结果。 */
    public static void notifyVerdict(HostHandoff handoff, HitVerdict verdict) {
        if (handoff == null || verdict == null) {
            return;
        }
        for (VerdictListener listener : VERDICT_LISTENERS) {
            listener.onVerdict(handoff, verdict);
        }
    }

    /** 先走旧结算通知，再把带目标和载荷的结果交给监听器。 */
    public static void notifyResolved(ResolvedHit hit) {
        if (hit == null || hit.handoff() == null || hit.verdict() == null) {
            return;
        }
        notifyVerdict(hit.handoff(), hit.verdict());
        for (VerdictListener listener : VERDICT_LISTENERS) {
            listener.onResolved(hit);
        }
    }
}
