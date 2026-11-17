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

    /** 由父令牌、成员、波次和接触合成操作号。复制序号不参与。 */
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

    /**
     * 父操作号与目标编号合成子操作号。
     * 高低位仍与父异或；目标先单向折叠，父和目标对调后不会撞成同一个号。
     */
    public static UUID targetOperationId(UUID parentOperationId, UUID targetId) {
        if (parentOperationId == null || targetId == null) {
            return null;
        }
        long targetHigh = targetId.getMostSignificantBits();
        long targetLow = targetId.getLeastSignificantBits();
        long foldedHigh = targetHigh ^ Long.rotateLeft(targetLow, TARGET_ROTATION) ^ TARGET_HIGH_SALT;
        long foldedLow = targetLow ^ Long.rotateLeft(targetHigh, TARGET_ROTATION) ^ TARGET_LOW_SALT;
        return new UUID(
                parentOperationId.getMostSignificantBits() ^ foldedHigh,
                parentOperationId.getLeastSignificantBits() ^ foldedLow);
    }

    private static final long TARGET_HIGH_SALT = 0x6A09E667F3BCC909L;
    private static final long TARGET_LOW_SALT = 0xBB67AE8584CAA73BL;
    private static final int TARGET_ROTATION = 21;
}
