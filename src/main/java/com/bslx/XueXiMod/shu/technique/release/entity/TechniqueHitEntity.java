package com.bslx.XueXiMod.shu.technique.release.entity;

import com.bslx.XueXiMod.debug.DebugLog;
import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
import com.bslx.XueXiMod.shu.technique.release.hitbox.ArcHitbox;
import com.bslx.XueXiMod.shu.technique.release.hitbox.BoxHitbox;
import com.bslx.XueXiMod.shu.technique.release.hitbox.ObbHitbox;
import com.bslx.XueXiMod.shu.technique.release.hitbox.SphereHitbox;
import com.bslx.XueXiMod.shu.technique.release.hitbox.TechniqueHitbox;
import com.bslx.XueXiMod.shu.technique.release.hitbox.SizedBoxHitbox;
import com.bslx.XueXiMod.shu.technique.release.motion.AttachedSwingMotion;
import com.bslx.XueXiMod.shu.technique.release.motion.DropMotion;
import com.bslx.XueXiMod.shu.technique.release.motion.FollowCasterMotion;
import com.bslx.XueXiMod.shu.technique.release.motion.ProjectileMotion;
import com.bslx.XueXiMod.shu.technique.release.motion.StillAimMotion;
import com.bslx.XueXiMod.shu.technique.release.motion.TechniqueMotion;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TechniqueHitEntity extends Entity {
    private static final EntityDataAccessor<String> DATA_TECHNIQUE_ID =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_VISUAL =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_MODEL_SCALE =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_TEXTURE =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_LENGTH =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_WIDTH =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ROLL =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID casterUuid;
    private TechniqueMotion motion;
    private TechniqueHitbox hitbox;
    private int lifetime;
    private double speed;
    private boolean lockThisCast;
    private boolean dropThisCast;
    private boolean centered;
    private boolean pose3d;
    private boolean beam;
    private boolean pinned;
    private boolean trap;
    private boolean planted;
    private boolean detonated;
    private final ClientTrack clientTrack = new ClientTrack();
    private int settleControlTicks;
    private boolean settleExtinguish;
    private float settleKnockback;
    private double arcRadius;
    private double arcDegrees;
    private double arcHeight;
    private Vec3 boxSize = Vec3.ZERO;
    private double length = 1.0;
    private double width = 1.0;
    private double height = 1.0;
    private int hitInterval;
    @Nullable
    private TechniqueHitSpec.HitListener listener;
    private int age;
    private Vec3 origin = Vec3.ZERO;
    private Vec3 forward = new Vec3(0.0, 0.0, 1.0);
    private Vec3 previousPosition = Vec3.ZERO;
    @Nullable
    private Vec3 anchor;
    @Nullable
    private Vec3 aim;
    private final Set<UUID> hitOnce = new HashSet<>();
    private final Map<UUID, Long> lastHitTick = new HashMap<>();
    private float settleDamage = Float.NaN;
    private EnchantmentResolution settleEnchantments;
    private ReleaseBatch.Tally settleTally;
    private int settleCopyIndex;
    private int settleBurnSeconds;

    public TechniqueHitEntity(EntityType<? extends TechniqueHitEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_TECHNIQUE_ID, "");
        this.entityData.define(DATA_VISUAL, 0);
        this.entityData.define(DATA_MODEL_SCALE, 1.0F);
        this.entityData.define(DATA_TEXTURE, "");
        this.entityData.define(DATA_LENGTH, 1.0F);
        this.entityData.define(DATA_WIDTH, 1.0F);
        this.entityData.define(DATA_HEIGHT, 1.0F);
        this.entityData.define(DATA_ROLL, 0.0F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        if (this.level().isClientSide()) {
            this.clientTrack.onClientMove(this, x, y, z, yRot, xRot);
            return;
        }
        super.lerpTo(x, y, z, yRot, xRot, steps, teleport);
    }

    @Override
    public void moveTo(double x, double y, double z, float yRot, float xRot) {
        if (this.level().isClientSide()) {
            this.clientTrack.onClientMove(this, x, y, z, yRot, xRot);
            return;
        }
        super.moveTo(x, y, z, yRot, xRot);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.clientTrack.afterClientTick(this);
            this.clientDecor();
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        this.previousPosition = this.position();
        this.age++;
        if (this.motion != null) {
            this.motion.tick(this, level);
        }
        if (this.centered) {
            this.previousPosition = this.position();
        }
        if (this.trap) {
            if (this.planted && !this.detonated) {
                AABB box = this.getBoundingBox().inflate(0.3);
                List<LivingEntity> stepped = level.getEntitiesOfClass(LivingEntity.class, box, this::canHit);
                if (!stepped.isEmpty()) {
                    this.detonate(level);
                }
            }
        } else if (this.hitbox != null) {
            AABB broad = this.hitbox.broadPhase(this);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, broad, this::canHit);
            for (LivingEntity target : targets) {
                if (this.hitbox.contains(this, target)) {
                    this.tryHit(level, target);
                }
            }
        }
        if (this.age >= this.lifetime) {
            this.onExpire(level);
        }
    }

    /** 粒子只在客户端逻辑拍生成，避免渲染帧里按光束长度狂撒。 */
    private void clientDecor() {
        int visual = this.getVisual();
        if (visual == 2) {
            if ((this.tickCount & 3) != 0) {
                return;
            }
            int steps = Math.max(1, (int) Math.round(this.length()));
            Vec3 point = this.position().add(this.getViewVector(1.0F).scale(this.tickCount % (steps + 1)));
            this.level().addParticle(ParticleTypes.FLAME, point.x, point.y, point.z, 0.0, 0.0, 0.0);
        } else if (visual == 3 && (this.tickCount & 7) == 0) {
            this.level().addParticle(ParticleTypes.CRIMSON_SPORE, this.getX(), this.getY() + 0.2, this.getZ(), 0.0, 0.02, 0.0);
        }
    }

    public void applySpec(ServerPlayer caster, TechniqueHitSpec spec) {
        this.casterUuid = caster.getUUID();
        this.lifetime = spec.lifetime;
        this.speed = spec.speed;
        this.lockThisCast = spec.lockThisCast;
        this.dropThisCast = spec.dropThisCast;
        this.centered = spec.centered;
        this.pose3d = spec.pose3d;
        this.arcRadius = spec.arcRadius;
        this.arcDegrees = spec.arcDegrees;
        this.arcHeight = spec.arcHeight;
        this.boxSize = spec.boxSize == null ? Vec3.ZERO : spec.boxSize;
        this.hitInterval = spec.hitInterval;
        this.listener = spec.listener;
        this.settleDamage = spec.settleDamage;
        this.settleEnchantments = spec.settleEnchantments;
        this.settleTally = spec.settleTally;
        this.settleCopyIndex = spec.settleCopyIndex;
        this.settleBurnSeconds = spec.settleBurnSeconds;
        this.settleControlTicks = spec.settleControlTicks;
        this.settleExtinguish = spec.settleExtinguish;
        this.settleKnockback = spec.settleKnockback;
        this.trap = spec.trap;
        this.anchor = spec.anchor;
        this.aim = spec.aim;
        this.length = spec.length;
        this.width = spec.width;
        this.height = spec.height;
        this.origin = spec.start != null ? spec.start : caster.getEyePosition();
        this.forward = spec.aim != null && spec.aim.lengthSqr() > 1.0E-8
                ? spec.aim.normalize()
                : caster.getLookAngle();
        TechniqueHitSpec.Path path = spec.path;
        if (path == null) {
            path = switch (spec.motion) {
                case DROP -> TechniqueHitSpec.Path.DROP;
                case FOLLOW -> TechniqueHitSpec.Path.FOLLOW;
                case SWING -> TechniqueHitSpec.Path.STILL_AIM;
                case PROJECTILE -> TechniqueHitSpec.Path.STRAIGHT;
            };
        }
        if (spec.dropThisCast) {
            path = TechniqueHitSpec.Path.DROP;
        }
        this.beam = path == TechniqueHitSpec.Path.FOLLOW;
        this.pinned = this.beam && spec.start != null;
        this.entityData.set(DATA_TECHNIQUE_ID, spec.techniqueId == null ? "" : spec.techniqueId);
        this.entityData.set(DATA_VISUAL, spec.visual);
        this.entityData.set(DATA_MODEL_SCALE, spec.modelScale);
        this.entityData.set(DATA_TEXTURE, spec.texture == null ? "" : spec.texture);
        this.entityData.set(DATA_LENGTH, (float) spec.length);
        this.entityData.set(DATA_WIDTH, (float) spec.width);
        this.entityData.set(DATA_HEIGHT, (float) spec.height);
        this.entityData.set(DATA_ROLL, spec.roll);
        this.motion = spec.trap ? new com.bslx.XueXiMod.shu.technique.release.motion.TrapFallMotion() : createMotion(path);
        this.hitbox = spec.path == null && spec.texture == null ? createHitbox(spec.hitbox) : new SizedBoxHitbox();
        if (path == TechniqueHitSpec.Path.STILL_AIM && this.motion != null && caster.level() instanceof ServerLevel serverLevel) {
            this.motion.tick(this, serverLevel);
            this.previousPosition = this.position();
        } else if (this.centered && this.anchor != null) {
            this.placeAt(this.anchor, this.aim != null ? this.aim : this.forward);
            this.previousPosition = this.anchor;
        } else {
            this.placeAt(this.origin, this.forward);
        }
    }

    public void placeAt(Vec3 pos, Vec3 dir) {
        this.setPos(pos.x, pos.y, pos.z);
        if (dir != null && dir.lengthSqr() > 1.0E-8) {
            Vec3 d = dir.normalize();
            double horizontal = Math.sqrt(d.x * d.x + d.z * d.z);
            this.setYRot((float) (Math.atan2(d.z, d.x) * (180.0 / Math.PI)) - 90.0F);
            this.setXRot((float) (-Math.atan2(d.y, horizontal) * (180.0 / Math.PI)));
        }
    }

    @Nullable
    public ServerPlayer getCaster() {
        if (this.casterUuid == null || this.level().isClientSide()) {
            return null;
        }
        MinecraftServer server = this.level().getServer();
        if (server == null) {
            return null;
        }
        return server.getPlayerList().getPlayer(this.casterUuid);
    }

    public int age() {
        return this.age;
    }

    public int lifetime() {
        return this.lifetime;
    }

    public double speed() {
        return this.speed;
    }

    public boolean lockThisCast() {
        return this.lockThisCast;
    }

    public boolean dropThisCast() {
        return this.dropThisCast;
    }

    public boolean centered() {
        return this.centered;
    }

    public boolean pose3d() {
        return this.pose3d;
    }

    public boolean beam() {
        return this.beam;
    }

    public boolean pinned() {
        return this.pinned;
    }

    public boolean planted() {
        return this.planted;
    }

    public void setPlanted(boolean planted) {
        this.planted = planted;
    }

    public float roll() {
        return this.entityData.get(DATA_ROLL);
    }

    public double arcRadius() {
        return this.arcRadius;
    }

    public double arcDegrees() {
        return this.arcDegrees;
    }

    public double arcHeight() {
        return this.arcHeight;
    }

    public Vec3 boxSize() {
        return this.boxSize;
    }

    public Vec3 origin() {
        return this.origin;
    }

    public Vec3 forward() {
        return this.forward;
    }

    public Vec3 previousPosition() {
        return this.previousPosition;
    }

    @Nullable
    public Vec3 anchor() {
        return this.anchor;
    }

    @Nullable
    public Vec3 aim() {
        return this.aim;
    }

    public UUID casterUuid() {
        return this.casterUuid;
    }

    public void setOrigin(Vec3 origin) {
        this.origin = origin;
    }

    public void setForward(Vec3 forward) {
        this.forward = forward;
    }

    public String getTechniqueId() {
        return this.entityData.get(DATA_TECHNIQUE_ID);
    }

    public int getVisual() {
        return this.entityData.get(DATA_VISUAL);
    }

    public float getModelScale() {
        return this.entityData.get(DATA_MODEL_SCALE);
    }

    public String texture() {
        return this.entityData.get(DATA_TEXTURE);
    }

    public double length() {
        return this.level().isClientSide() ? this.entityData.get(DATA_LENGTH) : this.length;
    }

    public double width() {
        return this.level().isClientSide() ? this.entityData.get(DATA_WIDTH) : this.width;
    }

    public double height() {
        return this.level().isClientSide() ? this.entityData.get(DATA_HEIGHT) : this.height;
    }

    public double hitHeight() {
        if (this.height() <= 0.05) {
            return Math.max(this.length(), 1.0);
        }
        return Math.max(this.height(), 1.0);
    }

    private boolean canHit(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        if (entity.isSpectator()) {
            return false;
        }
        return this.casterUuid == null || !this.casterUuid.equals(entity.getUUID());
    }

    private void tryHit(ServerLevel level, LivingEntity target) {
        if (this.hitInterval <= 0) {
            if (!this.hitOnce.add(target.getUUID())) {
                return;
            }
        } else {
            long now = level.getGameTime();
            Long last = this.lastHitTick.get(target.getUUID());
            if (last != null && now - last < this.hitInterval) {
                return;
            }
            this.lastHitTick.put(target.getUUID(), now);
        }
        if (!Float.isNaN(this.settleDamage)) {
            this.settle(level, target);
        }
        if (this.listener != null) {
            try {
                this.listener.onHit(target);
            } catch (Throwable throwable) {
                DebugLog.log("释放实体命中回调异常：" + throwable);
            }
        }
    }

    private void settle(ServerLevel level, LivingEntity target) {
        if (this.settleBurnSeconds > 0) {
            target.setSecondsOnFire(this.settleBurnSeconds);
        } else if (this.settleEnchantments != null && this.settleEnchantments.has("fire")) {
            var fire = this.settleEnchantments.effect("fire");
            if (fire != null) {
                target.setSecondsOnFire(fire.durationSeconds());
            }
        }
        if (this.settleExtinguish) {
            target.clearFire();
        }
        if (this.settleControlTicks > 0) {
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, this.settleControlTicks, 4));
        }
        if (this.settleKnockback > 0.0F) {
            ServerPlayer caster = this.getCaster();
            Vec3 from = caster != null ? caster.position() : this.position();
            Vec3 delta = target.position().subtract(from);
            target.knockback(this.settleKnockback, delta.x, delta.z);
        }
        if (this.settleTally != null) {
            this.settleTally.record(target, this.settleDamage, this.settleCopyIndex);
            MinecraftServer server = level.getServer();
            if (server != null) {
                int feedbackTick = server.getTickCount() + this.settleCopyIndex * 2;
                if (feedbackTick < this.settleTally.flushTick()) {
                    ChainScheduler.schedule(server, feedbackTick, () -> {
                        if (!target.isAlive()) {
                            return;
                        }
                        level.broadcastEntityEvent(target, (byte) 2);
                        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                                SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 0.6F,
                                1.1F + 0.1F * this.settleCopyIndex);
                        Vec3 dir = this.forward;
                        ServerPlayer caster = this.getCaster();
                        if (caster != null) {
                            Vec3 delta = target.position().subtract(caster.position());
                            if (delta.lengthSqr() > 1.0E-8) {
                                dir = delta.normalize();
                            }
                        }
                        target.knockback(0.08, dir.x, dir.z);
                        target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.02, 0.0));
                    });
                }
            }
        } else {
            ServerPlayer caster = this.getCaster();
            if (caster != null) {
                target.hurt(target.level().damageSources().playerAttack(caster), this.settleDamage);
            }
        }
    }

    private void detonate(ServerLevel level) {
        this.detonated = true;
        AABB area = this.getBoundingBox().inflate(1.5);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area, this::canHit);
        for (LivingEntity target : victims) {
            this.settle(level, target);
            com.bslx.XueXiMod.shu.technique.release.LifeDrain.start(level, this.getCaster(), target, 1.0F, 5, 12);
            if (this.listener != null) {
                try {
                    this.listener.onHit(target);
                } catch (Throwable throwable) {
                    DebugLog.log("释放实体命中回调异常：" + throwable);
                }
            }
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        this.discard();
    }

    private void onExpire(ServerLevel level) {
        if (this.listener != null) {
            try {
                this.listener.onExpire(this.position());
            } catch (Throwable throwable) {
                DebugLog.log("释放实体过期回调异常：" + throwable);
            }
        }
        this.discard();
    }

    private static TechniqueMotion createMotion(TechniqueHitSpec.Path path) {
        return switch (path) {
            case STILL_AIM -> new StillAimMotion();
            case STRAIGHT -> new ProjectileMotion();
            case DROP -> new DropMotion();
            case FOLLOW -> new FollowCasterMotion();
        };
    }

    private static TechniqueHitbox createHitbox(TechniqueHitSpec.Hitbox hitbox) {
        return switch (hitbox) {
            case ARC -> new ArcHitbox();
            case OBB -> new ObbHitbox();
            case BOX -> new BoxHitbox();
            case SPHERE -> new SphereHitbox();
        };
    }
}
