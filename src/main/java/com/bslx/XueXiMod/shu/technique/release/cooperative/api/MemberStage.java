package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import java.util.UUID;

/** 尚未发布的成员暂存凭证。真正的体积由发布端口按这个凭证保存，不放进长期计划。 */
public record MemberStage(UUID stageToken, MemberKey member, int volumeCount) {
    public MemberStage {
        if (stageToken == null || member == null) {
            throw new IllegalArgumentException("成员暂存不完整");
        }
        if (volumeCount < 1) {
            throw new IllegalArgumentException("成员暂存不能是空体积");
        }
    }
}
