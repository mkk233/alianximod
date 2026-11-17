package com.bslx.XueXiMod.event;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.relations.CombatOwnership;
import com.bslx.XueXiMod.shu.technique.release.relations.HarmPolicy;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public class EnchantedProjectileHandler {

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (projectile.level().isClientSide()) return;
        if (!projectile.getPersistentData().getBoolean("xueximod_fire")) return;

        if (!(event.getRayTraceResult() instanceof EntityHitResult entityHit)) return;
        Entity hit = entityHit.getEntity();
        if (!(hit instanceof LivingEntity target)) return;

        int seconds = projectile.getPersistentData().getInt("xueximod_fire_seconds");
        float radius = projectile.getPersistentData().getFloat("xueximod_fire_radius");
        java.util.UUID owner = CombatOwnership.playerOwner(projectile);
        if (HarmPolicy.denies(owner, target)) {
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
            return;
        }

        target.setSecondsOnFire(seconds);

        AABB box = target.getBoundingBox().inflate(radius);
        List<LivingEntity> nearby = projectile.level().getEntitiesOfClass(LivingEntity.class, box,
                nearbyEntity -> nearbyEntity != target && nearbyEntity.isAlive());
        for (LivingEntity nearbyEntity : nearby) {
            if (HarmPolicy.denies(owner, nearbyEntity)) {
                continue;
            }
            nearbyEntity.setSecondsOnFire(seconds);
        }
    }
}
