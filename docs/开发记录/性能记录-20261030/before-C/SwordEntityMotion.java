package com.bslx.XueXiMod.shu.technique.sword;

import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.network.ShuSyncPacket;
import com.bslx.XueXiMod.player.PlayerShuData;
import com.bslx.XueXiMod.shu.technique.enchant.TideMarkDamageScope;
import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
import com.bslx.XueXiMod.shu.technique.release.TechniqueHits;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

final class SwordEntityMotion {
    static Vec3 approachPoint(SwordEntity sword, Vec3 eye) {
        double deltaX = sword.getX() - eye.x;
        double deltaZ = sword.getZ() - eye.z;
        double horizontalLength = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (horizontalLength < 1.0E-4) {
            double ang = (sword.getId() & 7) * (Math.PI / 4.0);
            deltaX = Math.cos(ang);
            deltaZ = Math.sin(ang);
            horizontalLength = 1.0;
        }
        return new Vec3(eye.x + deltaX / horizontalLength, eye.y, eye.z + deltaZ / horizontalLength);
    }

    /** 主人包围盒与剑的包围盒距离小于 1.2 格时回收，只认主人。 */
    static boolean tryRecoverOwner(SwordEntity sword, ServerLevel level) {
        if (!(level.getPlayerByUUID(sword.owner) instanceof ServerPlayer ownerPlayer) || !ownerPlayer.isAlive()) {
            return false;
        }
        if (sword.getBoundingBox().inflate(1.2).intersects(ownerPlayer.getBoundingBox())) {
            recover(sword, ownerPlayer);
            return true;
        }
        return false;
    }

    static void recover(SwordEntity sword, ServerPlayer player) {
        if (sword.isPhantom()) {
            PlayerShuData data = PlayerShuData.get(player);
            data.setMagic(Math.min(data.getMagicMax(), data.getMagic() + 8));
            PlayerShuData.save(player, data);
            ModNetwork.sendToPlayer(player, new ShuSyncPacket(data));
            sword.discard();
            return;
        }
        ItemStack stack = sword.swordItem().copy();
        if (player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        } else if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        sword.discard();
    }

    static void hurtAlong(SwordEntity sword, ServerLevel level, Vec3 from, Vec3 targetPoint) {
        AABB box = new AABB(from, targetPoint).inflate(0.4);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, entity -> canHit(sword, entity))) {
            if (sword.hit.contains(target.getUUID()) || sword.hit.size() >= 5) {
                continue;
            }
            if (target.getBoundingBox().clip(from, targetPoint).isEmpty() && !target.getBoundingBox().inflate(0.2).contains(targetPoint)) {
                continue;
            }
            sword.hit.add(target.getUUID());
            Player ownerPlayer = level.getPlayerByUUID(sword.owner);
            TideMarkSuccess tide = TideMarkSuccess.of(sword.tideMarks, sword.owner);
            if (ownerPlayer instanceof ServerPlayer server) {
                boolean landed = TechniqueHits.hurtRoot(target, server, sword.copiedDamage, tide);
                com.bslx.XueXiMod.shu.technique.release.payload.HitPayloads.apply(sword, target, server);
                if (landed) {
                    com.bslx.XueXiMod.status.fire.seed.FireSeedGrant.deposit(target, server, sword.seedPoints);
                }
            } else {
                TideMarkDamageScope.Token token = TideMarkDamageScope.open(target, null, tide);
                boolean landed = false;
                try {
                    landed = target.hurt(level.damageSources().magic(), sword.copiedDamage);
                } finally {
                    TideMarkDamageScope.finish(token, landed);
                }
            }
            if (sword.burnSeconds > 0) {
                target.setSecondsOnFire(sword.burnSeconds);
            }
            if (sword.extinguish) {
                target.clearFire();
            }
            if (sword.controlTicks > 0) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, sword.controlTicks, 4));
            }
            if (sword.knockback > 0f) {
                Vec3 delta = target.position().subtract(from);
                target.knockback(sword.knockback, delta.x, delta.z);
            }
        }
    }

    static boolean canHit(SwordEntity sword, LivingEntity entity) {
        return entity.isAlive() && !entity.isSpectator() && !entity.getUUID().equals(sword.owner);
    }

    /**
     * Tip follows the release direction. Center stays on the surface so the tip
     * sits about half a block inside. Shared by real and phantom swords.
     */
    static void plant(SwordEntity sword, BlockHitResult hit) {
        Vec3 into = Vec3.atLowerCornerOf(hit.getDirection().getOpposite().getNormal());
        Vec3 tipDirection = plantTipDirection(into, sword.flyDirection);
        // FIXED model center ~= mid-blade; center-to-tip ~0.5; tip embed ~0.5 => center on surface
        double tipFromCenter = 0.5;
        double tipEmbed = 0.5;
        sword.setUprightFace(false);
        Vec3 location = hit.getLocation().add(tipDirection.scale(tipEmbed - tipFromCenter));
        sword.setPos(location.x, location.y, location.z);
        aim(sword, tipDirection);
        sword.yRotO = sword.getYRot();
        sword.xRotO = sword.getXRot();
        sword.setState(SwordEntity.PLANTED);
        sword.setDeltaMovement(Vec3.ZERO);
        sword.plantedBlock = hit.getBlockPos();
        sword.realFalls = false;
    }

    /** Sword axis stays parallel to the release direction. Flip only so the tip points into the block. */
    static Vec3 plantTipDirection(Vec3 into, Vec3 incoming) {
        Vec3 inwardNormal = into.normalize();
        Vec3 tip = incoming.lengthSqr() < 1.0E-6 ? inwardNormal : incoming.normalize();
        if (tip.dot(inwardNormal) < 0.0) {
            tip = tip.scale(-1.0);
        }
        return tip;
    }

    static void flyStep(SwordEntity sword, ServerLevel level) {
        sword.flyTicks++;
        double speed = sword.isPhantom() ? 2.5 : 1.2;
        if (sword.homeToMark && sword.flyTicks >= 60) {
            sword.homeToMark = false;
        }
        if (sword.isReal() && sword.trackingEnabled && sword.flyTicks >= 60) {
            sword.trackingEnabled = false;
            sword.trackingTarget = null;
        }
        SwordVortex.GuidedStep guided = SwordVortex.steer(sword, level, speed);
        if (!sword.isPhantom() && !sword.homeToMark && !sword.trackingEnabled && sword.flyTicks >= 40) {
            sword.realFalls = true;
        }
        if (guided.acquired && !sword.realFalls && guided.stepLength <= 1.0E-8) {
            aim(sword, guided.direction);
            Vec3 here = sword.position();
            hurtAlong(sword, level, here, here);
            if (sword.isPhantom() && (sword.flyTicks >= 80 || sword.flown >= 64)) {
                sword.discard();
                return;
            }
            if (sword.getY() < level.getMinBuildHeight() - 8 && sword.isReal()) {
                returnReal(sword, level);
            }
            return;
        }
        Vec3 from = sword.position();
        Vec3 step;
        if (sword.realFalls) {
            step = sword.getDeltaMovement().add(0.0, -0.08, 0.0);
            sword.setDeltaMovement(step);
        } else if (guided.acquired) {
            step = guided.direction.scale(guided.stepLength);
        } else {
            step = sword.flyDirection.scale(speed);
        }
        Vec3 targetPoint = from.add(step);
        sword.flown += from.distanceTo(targetPoint);
        BlockHitResult block = level.clip(new ClipContext(from, targetPoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sword));
        Vec3 stop = block.getType() == HitResult.Type.BLOCK ? block.getLocation() : targetPoint;
        hurtAlong(sword, level, from, stop);
        if (block.getType() == HitResult.Type.BLOCK && level.getBlockState(block.getBlockPos()).getFluidState().isEmpty()) {
            plant(sword, block);
            return;
        }
        sword.setPos(stop.x, stop.y, stop.z);
        aim(sword, sword.realFalls ? step : sword.flyDirection);
        if (sword.isPhantom() && (sword.flyTicks >= 80 || sword.flown >= 64)) {
            sword.discard();
            return;
        }
        if (sword.getY() < level.getMinBuildHeight() - 8 && sword.isReal()) {
            returnReal(sword, level);
        }
    }

    static void aim(SwordEntity sword, Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3 normalized = direction.normalize();
        double horizontal = Math.sqrt(normalized.x * normalized.x + normalized.z * normalized.z);
        sword.setYRot((float) (Math.atan2(normalized.z, normalized.x) * (180.0 / Math.PI)) - 90.0F);
        sword.setXRot((float) (-Math.atan2(normalized.y, horizontal) * (180.0 / Math.PI)));
    }

    static void returnReal(SwordEntity sword, ServerLevel level) {
        ItemStack stack = sword.swordItem().copy();
        Player ownerPlayer = level.getPlayerByUUID(sword.owner);
        if (ownerPlayer instanceof ServerPlayer server) {
            if (!server.getInventory().add(stack)) {
                server.drop(stack, false);
            }
        } else {
            sword.spawnCarried(stack);
        }
        sword.discard();
    }

    static void dropRealItem(SwordEntity sword, ServerLevel level) {
        sword.spawnCarried(sword.swordItem().copy());
        sword.discard();
    }

    static float damageOf(ItemStack stack) {
        float damage = 1f;
        for (AttributeModifier modifier : stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                damage += (float) modifier.getAmount();
            }
        }
        damage += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, stack) * 1.25f;
        return damage;
    }

    static void saveFlight(SwordEntity sword, CompoundTag tag) {
        tag.putDouble("FlyX", sword.flyDirection.x);
        tag.putDouble("FlyY", sword.flyDirection.y);
        tag.putDouble("FlyZ", sword.flyDirection.z);
        tag.putDouble("Flown", sword.flown);
        tag.putBoolean("RealFalls", sword.realFalls);
        ListTag hits = new ListTag();
        for (UUID struck : sword.hit) {
            hits.add(StringTag.valueOf(struck.toString()));
        }
        tag.put("Hits", hits);
    }

    static void loadFlight(SwordEntity sword, CompoundTag tag) {
        boolean flying = tag.getInt("State") == SwordEntity.FLY;
        boolean directionReady = false;
        if (tag.contains("FlyX") && tag.contains("FlyY") && tag.contains("FlyZ")) {
            double directionX = tag.getDouble("FlyX");
            double directionY = tag.getDouble("FlyY");
            double directionZ = tag.getDouble("FlyZ");
            if (Double.isFinite(directionX) && Double.isFinite(directionY) && Double.isFinite(directionZ)) {
                Vec3 loaded = new Vec3(directionX, directionY, directionZ);
                if (loaded.lengthSqr() > 1.0E-8) {
                    sword.flyDirection = loaded.normalize();
                    directionReady = true;
                }
            }
        }
        if (flying && !directionReady) {
            sword.flyDirection = new Vec3(0.0, -1.0, 0.0);
            sword.realFalls = true;
            sword.setDeltaMovement(Vec3.ZERO);
        } else if (tag.contains("RealFalls")) {
            sword.realFalls = tag.getBoolean("RealFalls");
        }
        if (tag.contains("Flown")) {
            double distance = tag.getDouble("Flown");
            if (Double.isFinite(distance) && distance >= 0.0) {
                sword.flown = distance;
            }
        }
        sword.hit.clear();
        if (!tag.contains("Hits", Tag.TAG_LIST)) {
            return;
        }
        ListTag hits = tag.getList("Hits", Tag.TAG_STRING);
        for (int hitIndex = 0; hitIndex < hits.size(); hitIndex++) {
            String text = hits.getString(hitIndex);
            if (text.length() != 36) {
                continue;
            }
            try {
                sword.hit.add(UUID.fromString(text));
            } catch (IllegalArgumentException malformed) {
                continue;
            }
        }
    }
}
