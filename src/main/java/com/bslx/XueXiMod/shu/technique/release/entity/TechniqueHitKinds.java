package com.bslx.XueXiMod.shu.technique.release.entity;

import com.bslx.XueXiMod.shu.technique.release.hitbox.ArcHitbox;
import com.bslx.XueXiMod.shu.technique.release.hitbox.BoxHitbox;
import com.bslx.XueXiMod.shu.technique.release.hitbox.ObbHitbox;
import com.bslx.XueXiMod.shu.technique.release.hitbox.SphereHitbox;
import com.bslx.XueXiMod.shu.technique.release.hitbox.TechniqueHitbox;
import com.bslx.XueXiMod.shu.technique.release.motion.DropMotion;
import com.bslx.XueXiMod.shu.technique.release.motion.FollowCasterMotion;
import com.bslx.XueXiMod.shu.technique.release.motion.FunctionMotion;
import com.bslx.XueXiMod.shu.technique.release.motion.ProjectileMotion;
import com.bslx.XueXiMod.shu.technique.release.motion.StillAimMotion;
import com.bslx.XueXiMod.shu.technique.release.motion.TechniqueMotion;
import com.bslx.XueXiMod.shu.technique.release.motion.VortexMotion;

/**
 * 把规格里的路径和盒子枚举换成运动与命中盒。
 * 不处理链上的抛物线、喷涌、追踪和陷阱，那些由实体装配时另选。不结算，也不改无敌帧或账本。
 */
final class TechniqueHitKinds {
    private TechniqueHitKinds() {
    }

    /** 没有单独的追踪或陷阱分支。调用方拿到对象后仍可整份换掉。 */
    static TechniqueMotion motion(TechniqueHitSpec.Path path) {
        return switch (path) {
            case STILL_AIM -> new StillAimMotion();
            case STRAIGHT -> new ProjectileMotion();
            case DROP -> new DropMotion();
            case FOLLOW -> new FollowCasterMotion();
            case VORTEX -> new VortexMotion();
            case FUNCTION -> new FunctionMotion();
        };
    }

    /** 只覆盖旧的四种盒子。带路径或贴图的实体不走这里。 */
    static TechniqueHitbox hitbox(TechniqueHitSpec.Hitbox hitbox) {
        return switch (hitbox) {
            case ARC -> new ArcHitbox();
            case OBB -> new ObbHitbox();
            case BOX -> new BoxHitbox();
            case SPHERE -> new SphereHitbox();
        };
    }
}
