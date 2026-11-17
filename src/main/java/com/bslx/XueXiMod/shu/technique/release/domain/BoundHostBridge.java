package com.bslx.XueXiMod.shu.technique.release.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 显式绑定宿主登记服务。静态表由注册和注销维护，调度每刻只读取副本。
 * 球形场地和护体不登记到这条宿主桥。
 */
public final class BoundHostBridge {
    private static final ArrayList<HostView> EXTRA = new ArrayList<>();

    private BoundHostBridge() {
    }

    /** 用实例标识替换登记。标识为空时不登记；同标识先摘掉旧宿主，不扣资源。 */
    public static void register(HostView host) {
        if (host == null || host.instanceId() == null) {
            return;
        }
        unregister(host.instanceId());
        EXTRA.add(host);
    }

    /** 按实例标识摘掉宿主。标识为空时不改表，找不到也不补建。 */
    public static void unregister(UUID instanceId) {
        if (instanceId == null) {
            return;
        }
        EXTRA.removeIf(host -> host == null || instanceId.equals(host.instanceId()));
    }

    /** 把带绑定计划的宿主追加进本轮。目标为空或没有登记时直接返回，不执行计划。 */
    public static void appendReady(List<HostView> into) {
        if (into == null || EXTRA.isEmpty()) {
            return;
        }
        ArrayList<HostView> copy = new ArrayList<>(EXTRA);
        for (HostView host : copy) {
            if (host == null || host.boundPlan() == null || host.ownerId() == null || host.instanceId() == null) {
                continue;
            }
            into.add(host);
        }
    }
}
