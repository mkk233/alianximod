package com.bslx.XueXiMod.shu.technique.release.deflection;

import java.util.UUID;

/**
 * 一次具体攻击或一次水体接触。
 * 箭用弹体编号加飞行接触序号，不用玩家加 tick。近战的接触号由调用方传入。
 */
public record AttackKey(Kind kind, UUID primaryId, UUID secondaryId, long contactIdentity) {
    public static final UUID NIL_PARTY = new UUID(0L, 0L);

    public enum Kind {
            /** 弹体接触。 */
        PROJECTILE,
            /** 近战接触。 */
        MELEE
    }

    public AttackKey {
        if (kind == null || primaryId == null || secondaryId == null) {
            throw new IllegalArgumentException("攻击身份不完整");
        }
    }

    /** 用弹体编号和飞行接触序号建立键。 */
    public static AttackKey projectile(UUID projectileId, int contactSerial) {
        if (projectileId == null || contactSerial < 0) {
            throw new IllegalArgumentException("箭的接触身份不完整");
        }
        return new AttackKey(Kind.PROJECTILE, projectileId, NIL_PARTY, contactSerial);
    }

    /** 用攻击者、受害者和调用方给的接触号建立键。 */
    public static AttackKey melee(UUID attackerId, UUID victimId, long contactIdentity) {
        if (attackerId == null || victimId == null) {
            throw new IllegalArgumentException("近战接触身份不完整");
        }
        return new AttackKey(Kind.MELEE, attackerId, victimId, contactIdentity);
    }

    /** 这是不是弹体接触。 */
    public boolean projectile() {
        return this.kind == Kind.PROJECTILE;
    }

    /** 弹体编号。近战键返回空。 */
    public UUID projectileId() {
        return this.projectile() ? this.primaryId : null;
    }
}
