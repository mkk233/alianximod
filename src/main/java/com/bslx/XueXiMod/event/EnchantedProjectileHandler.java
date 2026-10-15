package com.bslx.XueXiMod.event;

import com.bslx.XueXiMod.XueXiMod;
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

        target.setSecondsOnFire(seconds);

        AABB box = target.getBoundingBox().inflate(radius);
        List<LivingEntity> nearby = projectile.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != target && e.isAlive());
        for (LivingEntity e : nearby) {
            e.setSecondsOnFire(seconds);
        }
    }
}
