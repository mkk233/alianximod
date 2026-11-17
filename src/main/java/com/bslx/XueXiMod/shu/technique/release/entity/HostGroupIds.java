package com.bslx.XueXiMod.shu.technique.release.entity;

import java.util.UUID;

/** 宿主组编号。同一宿主稳定，不同宿主不共用。不用汇总、实体编号、复制序号或重水组。 */
public final class HostGroupIds {
    private static final long MIX = 0x9E3779B97F4A7C15L;

    private HostGroupIds() {
    }

    /** 用接触组和成员序号生成稳定宿主号。 */
    public static UUID sessionHost(UUID contactGroup, int memberIndex) {
        long salt = (memberIndex + 1L) * MIX;
        return new UUID(contactGroup.getMostSignificantBits() ^ salt,
                contactGroup.getLeastSignificantBits() ^ (salt << 1));
    }

    /** 普通释放的宿主号。父令牌、成员序号和波次决定；复制份不进编号。 */
    public static UUID castHost(UUID castToken, int memberIndex, int volley) {
        long memberSalt = (memberIndex + 1L) * MIX;
        long volleySalt = (volley + 1L) * MIX;
        return new UUID(castToken.getMostSignificantBits() ^ memberSalt,
                castToken.getLeastSignificantBits() ^ volleySalt);
    }
}
