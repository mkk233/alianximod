package com.bslx.XueXiMod.shu.technique.release.hitbox;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * 运动之后的命中判定。宽盒只用来搜候选，{@code contains} 才决定这个目标算不算进体积。
 * 不结算、不去重，也不改无敌帧或账本。
 */
public interface TechniqueHitbox {
    /** 候选搜索范围。比真正的命中体松，不能当成已经命中。 */
    AABB broadPhase(TechniqueHitEntity entity);

    /** 窄判定。返回假时调用方不会进入结算。 */
    boolean contains(TechniqueHitEntity entity, LivingEntity target);
}
