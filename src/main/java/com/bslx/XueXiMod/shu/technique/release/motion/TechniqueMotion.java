package com.bslx.XueXiMod.shu.technique.release.motion;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.server.level.ServerLevel;

/**
 * 每 tick 的运动策略。只改命中体位置和朝向。
 * 不选命中盒，不结算，也不改无敌帧或账本。
 */
public interface TechniqueMotion {
    /** 服务端每拍调用一次。实现方不得在这里判定命中。 */
    void tick(TechniqueHitEntity entity, ServerLevel level);
}
