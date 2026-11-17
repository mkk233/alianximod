package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import com.bslx.XueXiMod.shu.technique.release.clone.CloneBalance.CloneSpawnQuote;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneKind;

/**
 * 独立部署输入。报价必须已经包含一次服务端配置和合法载体折扣。
 * 这里没有外部扣款收据；扣款、发布、退款和生成冷却只发生在部署实现内部。
 */
public record CloneDeploymentInput(StandaloneDeploymentKind kind, CloneSpawnQuote quote,
                                   FrozenCarrierCredential carrier, DeploymentSource source) {
    public CloneDeploymentInput {
        if (kind == null || quote == null || carrier == null || source == null) {
            throw new IllegalArgumentException("独立部署输入不完整");
        }
        if (kind != StandaloneDeploymentKind.NORMAL || quote.kind() != CloneKind.NORMAL) {
            throw new IllegalArgumentException("独立部署只接受普通分身");
        }
        if (!CooperativeTechniqueIds.CLONE_BODY.equals(quote.techniqueId())) {
            throw new IllegalArgumentException("独立部署报价不是普通分身");
        }
    }
}
