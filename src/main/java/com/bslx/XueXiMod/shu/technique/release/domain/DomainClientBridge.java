package com.bslx.XueXiMod.shu.technique.release.domain;

import net.minecraft.world.entity.Entity;

import java.util.List;

/**
 * 客户端领域快照桥。服务端只保存来源，真正的快照生命周期由客户端接入方管理。
 * 球形场地和护体各有自己的同步状态，不从这里取裁切目标。
 */
public final class DomainClientBridge {
    public interface Source {
        List<DomainBound> targets(Entity mover);
    }

    private static Source source = mover -> List.of();

    private DomainClientBridge() {
    }

    /** 接上或清空客户端快照来源。传入空时退回空列表，不扣资源。 */
    public static void bind(Source next) {
        source = next == null ? mover -> List.of() : next;
    }

    /** 取这个移动者当前可见的领域目标。来源失败时只返回它给出的结果，不在这里补偿。 */
    public static List<DomainBound> targets(Entity mover) {
        return source.targets(mover);
    }
}
