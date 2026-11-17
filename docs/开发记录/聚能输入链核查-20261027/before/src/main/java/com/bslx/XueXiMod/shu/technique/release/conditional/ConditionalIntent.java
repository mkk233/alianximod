package com.bslx.XueXiMod.shu.technique.release.conditional;

import com.bslx.XueXiMod.shu.technique.release.entity.HostGroupIds;

import java.util.UUID;

/**
 * 同一次父释放里的条件意图。
 * 父令牌、成员、波次和动作接触决定身份，复制序号不进入编号。
 */
public final class ConditionalIntent {
    private ConditionalIntent() {
    }

    public static UUID operationId(UUID castToken, int memberIndex, int volley, UUID contactId) {
        if (castToken == null) {
            return null;
        }
        UUID castScoped = HostGroupIds.castHost(castToken, memberIndex, volley);
        if (contactId == null) {
            return castScoped;
        }
        return new UUID(
                castScoped.getMostSignificantBits() ^ contactId.getMostSignificantBits(),
                castScoped.getLeastSignificantBits() ^ contactId.getLeastSignificantBits());
    }
}
