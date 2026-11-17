package com.bslx.XueXiMod.npc.daozhang;

import com.bslx.XueXiMod.gui.DaozhangMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 独立道长。关系按本实体 UUID 与玩家 UUID 保存在本实体 NBT。 */
public class DaozhangEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> DATA_CAST =
            SynchedEntityData.defineId(DaozhangEntity.class, EntityDataSerializers.INT);
    private static final String RELATIONS = "Relations";
    private static final String PLAYER = "Player";
    private static final String STATE = "State";

    private final Map<UUID, DaozhangRelation> relations = new HashMap<>();
    private int castCooldown;

    public DaozhangEntity(EntityType<? extends DaozhangEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_CAST, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new DaozhangCastGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.castCooldown > 0) {
            this.castCooldown--;
        }
    }

    public DaozhangRelation relation(UUID playerId) {
        if (playerId == null) {
            return DaozhangRelation.NEUTRAL;
        }
        return this.relations.getOrDefault(playerId, DaozhangRelation.NEUTRAL);
    }

    public void setRelation(UUID playerId, DaozhangRelation relation) {
        if (playerId == null || relation == null) {
            return;
        }
        if (relation == DaozhangRelation.NEUTRAL) {
            this.relations.remove(playerId);
        } else {
            this.relations.put(playerId, relation);
        }
    }

    public boolean becomeHostile(UUID playerId) {
        if (playerId == null || this.relation(playerId) != DaozhangRelation.NEUTRAL) {
            return false;
        }
        this.setRelation(playerId, DaozhangRelation.HOSTILE);
        this.setTarget(null);
        return true;
    }

    public boolean apprentice(UUID playerId) {
        if (playerId == null || this.relation(playerId) != DaozhangRelation.NEUTRAL) {
            return false;
        }
        this.setRelation(playerId, DaozhangRelation.APPRENTICE);
        return true;
    }

    public @Nullable ServerPlayer hostileTarget() {
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel level) || level.getServer() == null) {
            return null;
        }
        ServerPlayer nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        double range = DaozhangConfig.castRange();
        for (UUID playerId : this.relations.keySet()) {
            if (this.relations.get(playerId) != DaozhangRelation.HOSTILE) {
                continue;
            }
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player == null || player.level() != level || !player.isAlive() || player.isSpectator()) {
                continue;
            }
            double distance = player.distanceToSqr(this);
            if (distance <= range * range && distance < nearestDistance && this.hasLineOfSight(player)) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    public boolean castReady() {
        return this.castCooldown <= 0;
    }

    public void beginCast() {
        this.castCooldown = DaozhangConfig.castCooldownTicks();
        this.entityData.set(DATA_CAST, this.entityData.get(DATA_CAST) + 1);
    }

    public int castPose() {
        return this.entityData.get(DATA_CAST);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || source == null || amount <= 0.0F) {
            return super.hurt(source, amount);
        }
        UUID playerId = com.bslx.XueXiMod.shu.technique.release.relations.CombatOwnership.playerOwner(source.getEntity());
        if (playerId == null) {
            playerId = com.bslx.XueXiMod.shu.technique.release.relations.CombatOwnership.playerOwner(source.getDirectEntity());
        }
        if (playerId != null && this.relation(playerId) == DaozhangRelation.APPRENTICE) {
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && playerId != null) {
            this.becomeHostile(playerId);
        }
        return hurt;
    }

    @Override
    public boolean addEffect(MobEffectInstance effect, @Nullable Entity source) {
        if (source != null && effect != null && effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
            UUID playerId = com.bslx.XueXiMod.shu.technique.release.relations.CombatOwnership.playerOwner(source);
            if (playerId != null && this.relation(playerId) == DaozhangRelation.APPRENTICE) {
                return false;
            }
        }
        return super.addEffect(effect, source);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        UUID playerId = com.bslx.XueXiMod.shu.technique.release.relations.CombatOwnership.playerOwner(other);
        if (playerId != null && this.relation(playerId) == DaozhangRelation.APPRENTICE) {
            return true;
        }
        return super.isAlliedTo(other);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.CONSUME;
        }
        if (!DaozhangAccess.canTalk(this, serverPlayer)) {
            return InteractionResult.FAIL;
        }
        UUID[] opened = new UUID[1];
        NetworkHooks.openScreen(serverPlayer, new Provider(this, opened), buffer -> {
            UUID nonce = com.bslx.XueXiMod.network.DaozhangSessions.open(serverPlayer.getUUID(), this.getUUID());
            opened[0] = nonce;
            buffer.writeUUID(this.getUUID());
            buffer.writeUUID(nonce == null ? new UUID(0L, 0L) : nonce);
        });
        if (serverPlayer.containerMenu instanceof DaozhangMenu menu
                && opened[0] != null && menu.sessionNonce().equals(opened[0])) {
            menu.refresh(serverPlayer);
        } else if (opened[0] != null) {
            com.bslx.XueXiMod.network.DaozhangSessions.close(serverPlayer.getUUID(), opened[0]);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag list = new ListTag();
        for (Map.Entry<UUID, DaozhangRelation> entry : this.relations.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putUUID(PLAYER, entry.getKey());
            row.putString(STATE, entry.getValue().name());
            list.add(row);
        }
        tag.put(RELATIONS, list);
        tag.putInt("CastCooldown", this.castCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.relations.clear();
        ListTag list = tag.getList(RELATIONS, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag row = list.getCompound(index);
            if (!row.hasUUID(PLAYER)) {
                continue;
            }
            this.relations.put(row.getUUID(PLAYER), DaozhangRelation.fromName(row.getString(STATE)));
        }
        this.castCooldown = Math.max(0, tag.getInt("CastCooldown"));
    }

    private record Provider(DaozhangEntity daozhang, UUID[] session) implements MenuProvider {
        @Override
        public Component getDisplayName() {
            return Component.translatable("entity.xueximod.daozhang");
        }

        @Override
        public DaozhangMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inventory, Player player) {
            UUID nonce = this.session[0];
            if (nonce == null) {
                return null;
            }
            return new DaozhangMenu(containerId, inventory, this.daozhang.getUUID(), nonce);
        }
    }
}
