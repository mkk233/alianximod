package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/** 配置读取后的一条冷却原料。安装冷却时只取实际提交的角色，不直接把整张表装两次。 */
public record QuotedCooldown(String techniqueId, ShareRole role, int memberOrdinal, boolean familyLevel,
                             int cooldownTicks) {
    public QuotedCooldown {
        if (techniqueId == null || techniqueId.isBlank() || role == null) {
            throw new IllegalArgumentException("冷却原料不完整");
        }
        if (cooldownTicks < 0) {
            throw new IllegalArgumentException("冷却刻数不能为负");
        }
        if (familyLevel) {
            if (memberOrdinal != -1) {
                throw new IllegalArgumentException("族级冷却不能带成员序号");
            }
        } else if (memberOrdinal < 0) {
            throw new IllegalArgumentException("成员冷却缺少序号");
        }
    }
}
