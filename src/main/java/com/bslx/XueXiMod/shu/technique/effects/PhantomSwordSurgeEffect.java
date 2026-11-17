package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.query.LivingCandidateQueries;
import com.bslx.XueXiMod.shu.technique.release.statusquery.RelationPolicy;
import com.bslx.XueXiMod.shu.technique.sword.SwordEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 幻剑纵横：拾起玩家附近的自有剑，各自朝最近敌人直线喷涌。 */
public class PhantomSwordSurgeEffect implements TechniqueEffect {
    public static final double HORIZONTAL_REACH = 8.0D;
    public static final double VERTICAL_REACH = 8.0D;
    public static final int SWORD_LIMIT = 8;
    public static final double OUTWARD_REACH = 6.0D;
    public static final double LIFT_BLOCKS = 2.0D;
    public static final double FLIGHT_SPEED = 1.5D;
    public static final float FLIGHT_DAMAGE = 2.0F;

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 playerCenter = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        AABB swordBox = new AABB(
                playerCenter.x - HORIZONTAL_REACH, playerCenter.y - VERTICAL_REACH, playerCenter.z - HORIZONTAL_REACH,
                playerCenter.x + HORIZONTAL_REACH, playerCenter.y + VERTICAL_REACH, playerCenter.z + HORIZONTAL_REACH);
        List<SwordEntity> selected = new ArrayList<>();
        for (SwordEntity sword : level.getEntitiesOfClass(SwordEntity.class, swordBox, SwordEntity::isAlive)) {
            if (sword.surgeSelectable(player)) {
                selected.add(sword);
            }
        }
        selected.sort(Comparator
                .comparingDouble((SwordEntity sword) -> sword.distanceToSqr(player))
                .thenComparingInt(SwordEntity::getId));
        if (selected.size() > SWORD_LIMIT) {
            selected.subList(SWORD_LIMIT, selected.size()).clear();
        }
        List<LivingEntity> enemies = LivingCandidateQueries.entityQuery(level, swordBox,
                living -> RelationPolicy.hostileCapture().accepts(player, living));
        RandomSource random = player.getRandom();
        for (SwordEntity sword : selected) {
            LivingEntity nearest = nearestEnemy(sword, enemies);
            Vec3 heading = nearest == null
                    ? fallbackHeading(sword, playerCenter, random)
                    : nearest.getBoundingBox().getCenter().subtract(sword.position());
            sword.beginSurge(heading);
        }
    }

    private static LivingEntity nearestEnemy(SwordEntity sword, List<LivingEntity> enemies) {
        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        int nearestId = Integer.MAX_VALUE;
        Vec3 swordPosition = sword.position();
        for (LivingEntity enemy : enemies) {
            double distance = enemy.distanceToSqr(swordPosition);
            int enemyId = enemy.getId();
            if (nearest == null || distance < nearestDistance || (distance == nearestDistance && enemyId < nearestId)) {
                nearest = enemy;
                nearestDistance = distance;
                nearestId = enemyId;
            }
        }
        return nearest;
    }

    private static Vec3 fallbackHeading(SwordEntity sword, Vec3 playerCenter, RandomSource random) {
        Vec3 swordPosition = sword.position();
        double offsetX = swordPosition.x - playerCenter.x;
        double offsetZ = swordPosition.z - playerCenter.z;
        double horizontalDistance = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);
        if (horizontalDistance <= OUTWARD_REACH) {
            if (horizontalDistance < 1.0E-4D) {
                double angle = (sword.getId() & 7) * (Math.PI / 4.0D);
                return new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            }
            return new Vec3(offsetX / horizontalDistance, 0.0D, offsetZ / horizontalDistance);
        }
        double towardX = playerCenter.x - swordPosition.x;
        double towardY = playerCenter.y - swordPosition.y;
        double towardZ = playerCenter.z - swordPosition.z;
        double span = Math.max(1.0E-4D, Math.max(Math.abs(towardX), Math.max(Math.abs(towardY), Math.abs(towardZ))));
        double jitterX = (random.nextDouble() - 0.5D) * span;
        double jitterY = (random.nextDouble() - 0.5D) * span;
        double jitterZ = (random.nextDouble() - 0.5D) * span;
        Vec3 heading = new Vec3(towardX + jitterX, towardY + jitterY, towardZ + jitterZ);
        if (heading.dot(new Vec3(towardX, towardY, towardZ)) <= 0.0D || heading.lengthSqr() < 1.0E-8D) {
            return new Vec3(towardX, towardY, towardZ);
        }
        return heading;
    }
}
