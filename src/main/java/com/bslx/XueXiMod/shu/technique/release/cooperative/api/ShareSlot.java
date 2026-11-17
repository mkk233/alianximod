package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/** 计划里的一笔份额索引。familyLevel 为 true 时 memberOrdinal 固定为 -1。金额只是索引，不是第二次扣款。 */
public record ShareSlot(ShareRole role, int memberOrdinal, boolean familyLevel, int amount) {
    public ShareSlot {
        if (role == null) {
            throw new IllegalArgumentException("份额缺少角色");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("份额金额不能为负");
        }
        if (familyLevel) {
            if (memberOrdinal != -1 || role == ShareRole.ATTACK_MEMBER) {
                throw new IllegalArgumentException("族级份额不能指向攻击成员");
            }
        } else if (memberOrdinal < 0 || role != ShareRole.ATTACK_MEMBER) {
            throw new IllegalArgumentException("成员份额只能指向攻击成员");
        }
    }
}
