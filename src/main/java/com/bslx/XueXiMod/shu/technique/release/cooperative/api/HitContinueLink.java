package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import java.util.UUID;

/**
 * 从既有子计划降低下来的命中接续。
 * 费用留在份额表，不在这里再记一笔价格。continuationId 是成功凭据的身份，可变的已结算标记留在运行时。
 */
public record HitContinueLink(int parentChainIndex, int childChainIndex, int ownCopies, boolean entityLock,
                              boolean dropPath, boolean upwellPath, boolean stagger, int lifetimeTicks,
                              double radius, UUID continuationId) {
    public HitContinueLink {
        if (parentChainIndex < 0 || childChainIndex < 0) {
            throw new IllegalArgumentException("命中接续下标不能为负");
        }
        if (ownCopies < 1 || lifetimeTicks < 1) {
            throw new IllegalArgumentException("命中接续份数和寿命至少为 1");
        }
        if (!Double.isFinite(radius) || radius <= 0.0D) {
            throw new IllegalArgumentException("命中接续半径必须是有限正数");
        }
        if (continuationId == null) {
            throw new IllegalArgumentException("命中接续缺少凭据");
        }
    }
}
