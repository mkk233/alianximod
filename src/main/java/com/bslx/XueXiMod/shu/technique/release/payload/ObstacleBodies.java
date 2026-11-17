package com.bslx.XueXiMod.shu.technique.release.payload;

import com.bslx.XueXiMod.shu.technique.release.entity.ReleaseEntities;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 临时实体障碍。形状跟着命中体，客户端仍画原来的模型。不改方块和流体。 */
public final class ObstacleBodies {
    private static final ConcurrentHashMap<UUID, Body> BODIES = new ConcurrentHashMap<>();

    private ObstacleBodies() {
    }

    public static void track(TechniqueHitEntity entity, double across, double wide, double tall) {
        if (entity == null) {
            return;
        }
        entity.noPhysics = false;
        entity.setNoGravity(true);
        double halfAcross = Math.max(0.4, across * 0.5);
        double halfWide = Math.max(0.4, wide * 0.5);
        double halfTall = Math.max(0.4, tall * 0.5);
        BODIES.put(entity.getUUID(), new Body(halfAcross, halfWide, halfTall));
        resize(entity, BODIES.get(entity.getUUID()));
    }

    public static boolean blocks(Entity entity) {
        return entity != null && BODIES.containsKey(entity.getUUID());
    }

    /** 凝结一类术式可以直接放一块障碍，不必另写实体。 */
    public static void place(ServerLevel level, ServerPlayer caster, Vec3 anchor, double across, double tall, int lifetimeTicks) {
        if (level == null || caster == null || anchor == null) {
            return;
        }
        TechniqueHitSpec spec = new TechniqueHitSpec();
        spec.path = TechniqueHitSpec.Path.STILL_AIM;
        spec.hitbox = TechniqueHitSpec.Hitbox.BOX;
        spec.lifetime = lifetimeTicks > 0 ? lifetimeTicks : 80;
        spec.length = across;
        spec.width = across;
        spec.height = tall;
        spec.boxSize = new Vec3(across, tall, across);
        spec.blocksMovement = true;
        spec.anchor = anchor;
        spec.start = anchor;
        spec.centered = true;
        TechniqueHitEntity entity = new TechniqueHitEntity(ReleaseEntities.TECHNIQUE_HIT.get(), level);
        entity.applySpec(caster, spec);
        entity.setPos(anchor.x, anchor.y, anchor.z);
        level.addFreshEntity(entity);
        track(entity, across, across, tall);
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Body>> iterator = BODIES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Body> entry = iterator.next();
            TechniqueHitEntity entity = find(server, entry.getKey());
            if (entity == null || entity.isRemoved()) {
                iterator.remove();
                continue;
            }
            if (!(entity.level() instanceof ServerLevel level)) {
                continue;
            }
            resize(entity, entry.getValue());
            AABB box = entity.getBoundingBox();
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box.inflate(0.05), LivingEntity::isAlive)) {
                if (living.getUUID().equals(entity.casterUuid())) {
                    continue;
                }
                Vec3 away = living.position().subtract(box.getCenter());
                if (away.lengthSqr() < 1.0E-4) {
                    away = new Vec3(1.0, 0.0, 0.0);
                }
                away = away.normalize().scale(0.35);
                living.setPos(living.getX() + away.x, living.getY(), living.getZ() + away.z);
                living.setDeltaMovement(away.x, living.getDeltaMovement().y, away.z);
                living.hurtMarked = true;
            }
        }
    }

    private static void resize(TechniqueHitEntity entity, Body body) {
        Vec3 place = entity.position();
        entity.setBoundingBox(new AABB(
                place.x - body.halfAcross, place.y, place.z - body.halfWide,
                place.x + body.halfAcross, place.y + body.halfTall, place.z + body.halfWide));
    }

    private static TechniqueHitEntity find(MinecraftServer server, UUID entityId) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity found = level.getEntity(entityId);
            if (found instanceof TechniqueHitEntity hitEntity) {
                return hitEntity;
            }
        }
        return null;
    }

    private static final class Body {
        final double halfAcross;
        final double halfWide;
        final double halfTall;

        Body(double halfAcross, double halfWide, double halfTall) {
            this.halfAcross = halfAcross;
            this.halfWide = halfWide;
            this.halfTall = halfTall;
        }
    }
}
