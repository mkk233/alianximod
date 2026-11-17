package com.bslx.XueXiMod.shu.technique.release.contact;

import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierBody;
import com.bslx.XueXiMod.shu.technique.release.relations.Relations;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 通行、接触、箭衰减三条权限分开，不共用一个布尔。 */
public final class ContactPolicy {
    private ContactPolicy() {
    }

    public static boolean mayBodyPass(UUID ownerId, Entity mover) {
        if (mover == null) {
            return false;
        }
        if (mover instanceof ItemEntity || mover instanceof ExperienceOrb || mover instanceof Projectile) {
            return true;
        }
        if (mover.isSpectator() || mover.noPhysics) {
            return true;
        }
        Entity root = mover.getRootVehicle();
        List<Entity> group = new ArrayList<>();
        collect(root, group);
        for (Entity member : group) {
            if (member instanceof LivingEntity && !Relations.permits(ownerId, member)) {
                return false;
            }
        }
        return true;
    }

    public static boolean mayBodyPass(Entity mover, BarrierBody wall) {
        return wall != null && mayBodyPass(wall.ownerId(), mover);
    }

    public static boolean mayContactAffect(UUID ownerId, LivingEntity target) {
        if (target == null || !target.isAlive() || target.isSpectator()) {
            return false;
        }
        if (Relations.permits(ownerId, target) || Relations.ally(ownerId, target)) {
            return false;
        }
        return true;
    }

    public static boolean mayAttenuateProjectile(Projectile projectile) {
        return projectile != null && projectile.isAlive();
    }

    private static void collect(Entity root, List<Entity> group) {
        group.add(root);
        for (Entity passenger : root.getPassengers()) {
            collect(passenger, group);
        }
    }
}
