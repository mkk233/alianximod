package com.bslx.XueXiMod.shu.technique.release.bind;

import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.UUID;

/** 每次解析都重验完整域内资格。不读玩家锁定标记。 */
public interface TargetCheck {
    /** 这个目标仍在域内且关系合格时返回它，否则为空。 */
    @Nullable LivingEntity stillValid(@Nullable UUID targetId);
}
