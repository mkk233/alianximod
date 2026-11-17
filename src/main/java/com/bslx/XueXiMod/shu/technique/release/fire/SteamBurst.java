package com.bslx.XueXiMod.shu.technique.release.fire;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.enchant.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.release.statusquery.RelationPolicy;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 主动退出炉心时的一次蒸汽。固定 2 点，不读炉心、火种或元素。 */
public final class SteamBurst {
    public static final ResourceKey<DamageType> TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(XueXiMod.MOD_ID, FireChannels.STEAM_PATH));

    private static final Logger LOGGER = LogUtils.getLogger();

    private SteamBurst() {
    }

    public static void release(Player player) {
        if (!(player instanceof ServerPlayer caster) || caster.level().isClientSide() || !caster.isAlive()) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        Vec3 center = caster.getBoundingBox().getCenter();
        double radius = SteamTuning.configuredRadius();
        AABB area = new AABB(center, center).inflate(radius);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area, SteamBurst::nearby);
        DamageSource source = source(level, caster);
        if (source == null) {
            return;
        }
        Optional<Double> settled = SpecialBalance.settleConfigured(SpecialBalance.COOL, SteamTuning.BASE_DAMAGE);
        if (settled.isEmpty() || !FireLayer.acceptableSettled(settled.get())) {
            return;
        }
        float steamDamage = settled.get().floatValue();
        boolean establishedWithoutHurt = steamDamage <= 0.0f;
        for (LivingEntity candidate : nearby) {
            if (!SteamTuning.reaches(candidate.getBoundingBox().distanceToSqr(center), radius)) {
                continue;
            }
            if (!hostile(caster, candidate)) {
                continue;
            }
            boolean landed = establishedWithoutHurt;
            if (!establishedWithoutHurt) {
                ElementDamageScope.ElementArm neutral = ElementDamageScope.armNeutral();
                ElementDamageScope.Boundary boundary = ElementDamageScope.mark();
                try {
                    landed = candidate.hurt(source, steamDamage);
                } catch (RuntimeException failure) {
                    LOGGER.error("steam burst failed for {}", candidate.getUUID(), failure);
                    continue;
                } finally {
                    ElementDamageScope.restore(boundary);
                    ElementDamageScope.disarm(neutral);
                }
            }
            if (!landed || !candidate.isAlive()) {
                continue;
            }
            push(caster, candidate);
        }
    }

    private static boolean nearby(LivingEntity candidate) {
        return candidate != null && candidate.isAlive() && !candidate.isSpectator() && !candidate.isRemoved();
    }

    private static boolean hostile(ServerPlayer caster, LivingEntity candidate) {
        boolean self = candidate == caster || candidate.getUUID().equals(caster.getUUID());
        boolean sameTeam = !self && candidate.isAlliedTo(caster);
        boolean tamed = tamedByCasterOrTeammate(caster, candidate);
        boolean relationAccepts = RelationPolicy.hostileCapture().accepts(caster, candidate);
        return SteamHostility.struck(self, sameTeam, tamed, relationAccepts);
    }

    private static boolean tamedByCasterOrTeammate(ServerPlayer caster, LivingEntity subject) {
        if (!(subject instanceof TamableAnimal tamable)) {
            return false;
        }
        UUID ownerId = tamable.getOwnerUUID();
        if (ownerId == null) {
            return false;
        }
        if (ownerId.equals(caster.getUUID())) {
            return true;
        }
        ServerPlayer owner = caster.server.getPlayerList().getPlayer(ownerId);
        return owner != null && owner.isAlliedTo(caster);
    }

    private static void push(ServerPlayer caster, LivingEntity target) {
        Vec3 origin = caster.getBoundingBox().getCenter();
        Vec3 body = target.getBoundingBox().getCenter();
        SteamVector vector = SteamVector.horizontal(origin.x, origin.z, body.x, body.z, caster.getYRot());
        target.knockback(SteamTuning.KNOCKBACK_STRENGTH, vector.towardX(), vector.towardZ());
    }

    private static DamageSource source(ServerLevel level, ServerPlayer caster) {
        try {
            Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
            Holder<DamageType> holder = registry.getHolderOrThrow(TYPE);
            return new DamageSource(holder, caster);
        } catch (RuntimeException failure) {
            LOGGER.error("steam burst damage type missing", failure);
            return null;
        }
    }
}
