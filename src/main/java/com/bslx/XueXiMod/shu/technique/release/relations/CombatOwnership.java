package com.bslx.XueXiMod.shu.technique.release.relations;

import com.bslx.XueXiMod.npc.daozhang.DaozhangSpellProjectile;
import com.bslx.XueXiMod.mixin.ProjectileOwnerAccess;
import com.bslx.XueXiMod.mixin.AreaEffectCloudOwnerAccess;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneEntity;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import com.bslx.XueXiMod.shu.technique.sword.SwordEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 把伤害来源收成玩家 UUID。离线 UUID 仍保留。
 * 无来源、环境或无法归到玩家时返回 null，不猜最近攻击者。
 */
@Mod.EventBusSubscriber(modid = com.bslx.XueXiMod.XueXiMod.MOD_ID)
public final class CombatOwnership {
    private static final int MAX_DEPTH = 4;
    private static final String PLAYER_OWNER = "xueximod:player_owner";
    private static final String PLAYER_ID = "Player";

    private CombatOwnership() {
    }

    public static @Nullable UUID playerOwner(@Nullable Entity entity) {
        return walk(entity, 0, new HashSet<>());
    }

    private static @Nullable UUID walk(@Nullable Entity entity, int depth, Set<UUID> seen) {
        if (entity == null || depth > MAX_DEPTH || !seen.add(entity.getUUID())) {
            return null;
        }
        if (entity instanceof Player player) {
            return player.getUUID();
        }
        if (entity instanceof CloneEntity clone) {
            return clone.ownerId();
        }
        if (entity instanceof TechniqueHitEntity hit) {
            return hit.casterUuid();
        }
        if (entity instanceof SwordEntity sword) {
            return sword.ownerUuid();
        }
        if (entity instanceof DaozhangSpellProjectile spell) {
            return null;
        }
        if (entity instanceof Projectile projectile) {
            UUID owner = resolve(projectile.getOwner(), confirmedProjectileOwner(projectile), projectile.level(), depth, seen);
            if (owner != null) {
                return owner;
            }
        }
        if (entity instanceof TraceableEntity traceable) {
            UUID remembered = entity instanceof AreaEffectCloud cloud
                    ? confirmedCloudOwner(cloud) : null;
            UUID owner = resolve(traceable.getOwner(), remembered, entity.level(), depth, seen);
            if (owner != null) {
                return owner;
            }
        }
        if (entity instanceof OwnableEntity ownable) {
            return resolve(ownable.getOwner(), ownable.getOwnerUUID(), entity.level(), depth, seen);
        }
        return null;
    }

    private static @Nullable UUID resolve(@Nullable Entity loaded, @Nullable UUID remembered,
                                          @Nullable Level level, int depth, Set<UUID> seen) {
        UUID walked = walk(loaded, depth + 1, seen);
        if (walked != null) {
            return walked;
        }
        return playerUuid(remembered, level);
    }

    /** 当前所有者变化后同步玩家标记。非玩家或未知所有者会清除旧标记。 */
    public static void transfer(Entity entity, @Nullable Entity source) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        UUID playerId = source == null ? null : playerOwner(source);
        CompoundTag data = entity.getPersistentData();
        if (playerId == null) {
            data.remove(PLAYER_OWNER);
            data.remove(PLAYER_ID);
        } else {
            data.putBoolean(PLAYER_OWNER, true);
            data.putUUID(PLAYER_ID, playerId);
        }
        if (entity instanceof AreaEffectCloud cloud) {
            ((AreaEffectCloudOwnerAccess) cloud).xuexiSetOwnerUUID(playerId);
            if (source instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
                cloud.setOwner(living);
            }
        }
    }

    private static @Nullable UUID confirmedProjectileOwner(Projectile projectile) {
        CompoundTag tag = new CompoundTag();
        ((ProjectileOwnerAccess) projectile).xuexiWriteProjectileData(tag);
        if (!tag.hasUUID("Owner")) {
            return null;
        }
        UUID ownerId = tag.getUUID("Owner");
        CompoundTag data = projectile.getPersistentData();
        if (data.getBoolean(PLAYER_OWNER) && data.hasUUID(PLAYER_ID) && ownerId.equals(data.getUUID(PLAYER_ID))) {
            return ownerId;
        }
        return playerUuid(ownerId, projectile.level());
    }

    private static @Nullable UUID confirmedCloudOwner(AreaEffectCloud cloud) {
        UUID ownerId = ((AreaEffectCloudOwnerAccess) cloud).xuexiOwnerUUID();
        if (ownerId == null) {
            return null;
        }
        CompoundTag data = cloud.getPersistentData();
        if (data.getBoolean(PLAYER_OWNER) && data.hasUUID(PLAYER_ID) && ownerId.equals(data.getUUID(PLAYER_ID))) {
            return ownerId;
        }
        return playerUuid(ownerId, cloud.level());
    }

    private static @Nullable UUID playerUuid(@Nullable UUID candidate, @Nullable Level level) {
        if (candidate == null || !(level instanceof ServerLevel server)) {
            return null;
        }
        GameProfileCache cache = server.getServer().getProfileCache();
        return cache != null && cache.get(candidate).isPresent() ? candidate : null;
    }

    @SubscribeEvent
    public static void rememberPlayerSource(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof DaozhangSpellProjectile) {
            return;
        }
        Entity owner = entity instanceof Projectile projectile ? projectile.getOwner()
                : entity instanceof TraceableEntity traceable ? traceable.getOwner()
                : entity instanceof OwnableEntity ownable ? ownable.getOwner() : null;
        if (owner == null) {
            return;
        }
        transfer(entity, owner);
    }
}
