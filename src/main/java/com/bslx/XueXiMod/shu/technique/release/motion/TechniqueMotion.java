package com.bslx.XueXiMod.shu.technique.release.motion;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.server.level.ServerLevel;

public interface TechniqueMotion {
    void tick(TechniqueHitEntity entity, ServerLevel level);
}
