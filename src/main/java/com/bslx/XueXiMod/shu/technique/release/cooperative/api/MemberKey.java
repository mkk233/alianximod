package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/** 双重、错时或命中接续中的一个成员。成员序号只在所属攻击族内密集使用。 */
public record MemberKey(FamilyKey family, int memberOrdinal) {
    public MemberKey {
        if (family == null) {
            throw new IllegalArgumentException("成员缺少攻击族");
        }
        if (memberOrdinal < 0) {
            throw new IllegalArgumentException("成员序号不能为负");
        }
    }
}
