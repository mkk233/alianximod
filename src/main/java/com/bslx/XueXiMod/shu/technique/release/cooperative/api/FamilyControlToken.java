package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import java.util.UUID;

/** 族的取消令牌或完成令牌。两枚令牌的标识不能相同。 */
public record FamilyControlToken(UUID tokenId) {
    public FamilyControlToken {
        if (tokenId == null) {
            throw new IllegalArgumentException("控制令牌缺少标识");
        }
    }
}
