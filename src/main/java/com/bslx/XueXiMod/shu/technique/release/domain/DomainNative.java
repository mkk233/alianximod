package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.shu.technique.release.relations.Relations;
import net.minecraft.world.entity.LivingEntity;

/**
 * 原生敌向资格服务。无状态，领域实例和几何仍由领域服务持有。
 * 球形场地按身体覆盖选目标，护体只围绕主人，都不使用这份完整包含资格。
 */
public final class DomainNative {
    private DomainNative() {
    }

    /** 判断完整身体是否在敌向领域内。不扣资源；维度、存活、旁观、体型或关系任一失败都返回假。 */
    public static boolean hostileInside(DomainInstance domain, LivingEntity living) {
        if (domain == null || !domain.active() || living == null || !living.isAlive() || living.isSpectator()) {
            return false;
        }
        if (living.level() != domain.hostLevel()) {
            return false;
        }
        if (!domain.geometry().fits(living.getBoundingBox()) || !domain.geometry().fullyContains(living.getBoundingBox())) {
            return false;
        }
        return !Relations.permits(domain.ownerId(), living) && !Relations.ally(domain.ownerId(), living);
    }
}
