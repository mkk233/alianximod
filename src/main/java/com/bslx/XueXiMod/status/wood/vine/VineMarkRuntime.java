package com.bslx.XueXiMod.status.wood.vine;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.registry.ModMobEffects;
import com.bslx.XueXiMod.shu.technique.release.firecast.DamagePermission;
import com.bslx.XueXiMod.shu.technique.release.relations.HarmPolicy;
import com.bslx.XueXiMod.status.wood.resource.CreateResult;
import com.bslx.XueXiMod.status.wood.resource.FlowerKind;
import com.bslx.XueXiMod.status.wood.resource.FragmentTake;
import com.bslx.XueXiMod.status.wood.resource.GroundFragmentView;
import com.bslx.XueXiMod.status.wood.resource.GroundLotSpec;
import com.bslx.XueXiMod.status.wood.resource.GroundQueryResult;
import com.bslx.XueXiMod.status.wood.resource.ProductionTicket;
import com.bslx.XueXiMod.status.wood.resource.ResourceOrigin;
import com.bslx.XueXiMod.status.wood.resource.WoodLimits;
import com.bslx.XueXiMod.status.wood.resource.WoodResourceService;
import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 缠枝花谱、枝印、束缚、刺伤、流血和落英的服务端状态。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class VineMarkRuntime {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<MarkKey, BranchMark> MARKS = new HashMap<>();
    private static final Map<UUID, Long> RESIDUALS = new HashMap<>();
    private static final Map<UUID, MovementTrack> MOVEMENT = new HashMap<>();
    private static final Map<UUID, BleedRecord> BLEEDS = new HashMap<>();
    private static final Map<ActionKey, Boolean> CONSUMED_ACTIONS = new HashMap<>();
    private static final Map<ActionKey, Boolean> ACTION_SPECTRUM = new HashMap<>();
    private static final Map<UUID, Vec3> MOVE_STARTS = new HashMap<>();

    private VineMarkRuntime() {
    }

    public static void grantCommittedSpectrum(ServerPlayer player) {
        VineMarkSpectrum.refresh(player);
    }

    public static void afterShareCommitted(ManaPaymentReceipt receipt) {
        if (receipt == null || !receipt.roseCommitOnce()) {
            return;
        }
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(receipt.ownerId());
        if (!readyCaster(player) || !VineMarkSpectrum.matches(player, receipt)) {
            return;
        }
        VineMarkSpectrum.clearPending(player);
        if (player.isShiftKeyDown()) {
            tryMobilize(player);
            return;
        }
        VineMarkSpectrum.refresh(player);
    }

    /** 旧木附魔的附加伤害、元素反应和缓慢不再生效。其他附魔仍走原路径。 */
    public static void suppressLegacyControl(ServerPlayer caster, LivingEntity target) {
    }

    public static void suppressLegacyControl(UUID casterId, LivingEntity target) {
    }

    public static boolean tryMobilize(ServerPlayer player) {
        if (!readyCaster(player) || !VineMarkSpectrum.active(player)) {
            return false;
        }
        LivingEntity target = aimedBleeding(player);
        GroundFragmentView fragment = nearestFallen(player);
        if (target == null || fragment == null) {
            return false;
        }
        int consumeUnits = Math.min(VineMarkBalance.fallenConsumeUnits(), fragment.length());
        if (consumeUnits <= 0) {
            return false;
        }
        FragmentTake take = new FragmentTake(fragment.fragmentId(), fragment.revision(), consumeUnits);
        boolean consumed = WoodResourceService.consumePrepared(player, List.of(take), preview ->
                player.getUUID().equals(preview.ownerId()) && preview.pieces().size() == 1
                        && preview.pieces().get(0).flower() == FlowerKind.ROSE).isPresent();
        if (!consumed) {
            return false;
        }
        Vec3 origin = new Vec3(fragment.coordX(), fragment.coordY(), fragment.coordZ());
        RoseVineProjectile.launch(player, origin, target.getBoundingBox().getCenter().subtract(origin));
        player.serverLevel().sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                origin.x, origin.y, origin.z, 6, 0.15d, 0.15d, 0.15d, 0.0d);
        return true;
    }

    public static void rememberAction(UUID actionId, ServerPlayer caster, boolean spectrumEligible) {
        if (actionId == null || caster == null) {
            return;
        }
        ACTION_SPECTRUM.put(new ActionKey(caster.getUUID(), actionId, new UUID(0L, 0L)), spectrumEligible);
    }

    public static void stampSword(com.bslx.XueXiMod.shu.technique.sword.SwordEntity sword,
                                  UUID actionId, boolean spectrumEligible) {
        if (sword == null) {
            return;
        }
        sword.vineActionId = actionId;
        sword.vineSpectrumEligible = spectrumEligible;
    }

    public static void stampTally(com.bslx.XueXiMod.shu.technique.release.ReleaseBatch.Tally tally,
                                  UUID actionId, boolean spectrumEligible) {
        if (tally == null) {
            return;
        }
        tally.vineActionId = actionId;
        tally.vineSpectrumEligible = spectrumEligible;
    }

    public static void offerQualifiedHit(ServerPlayer caster, LivingEntity target, UUID actionId,
                                         boolean canceled, boolean positiveDamage) {
        offerQualifiedHit(caster, target, actionId, canceled, positiveDamage, null);
    }

    public static void offerQualifiedHit(ServerPlayer caster, LivingEntity target, UUID actionId,
                                         boolean canceled, boolean positiveDamage, Boolean frozenSpectrum) {
        if (!readyCaster(caster) || target == null || actionId == null || canceled || !positiveDamage) {
            return;
        }
        if (!DamagePermission.allows(caster, target) || HarmPolicy.denies(caster, target)) {
            return;
        }
        long now = target.level().getGameTime();
        prune(now);
        ActionKey actionKey = new ActionKey(caster.getUUID(), actionId, target.getUUID());
        if (CONSUMED_ACTIONS.containsKey(actionKey)) {
            return;
        }
        CONSUMED_ACTIONS.put(actionKey, Boolean.TRUE);
        boolean eligible = resolveEligibility(caster, actionId, frozenSpectrum);
        MarkKey markKey = new MarkKey(caster.getUUID(), target.getUUID());
        BranchMark mark = currentMark(markKey, now);
        if (mark != null && mark.stage == Stage.BUD) {
            bloom(caster, target, mark, now);
            return;
        }
        if (!eligible || residualActive(target.getUUID(), now)) {
            return;
        }
        if (mark == null) {
            install(markKey, Stage.ONE, 1, now + VineMarkBalance.branchLifetimeTicks());
            tryBind(target, VineMarkBalance.controlTicks());
            ensureMovement(caster, target, now);
            return;
        }
        if (mark.stage == Stage.ONE) {
            mark.stage = Stage.TWO;
            mark.branches = 2;
            mark.expireGameTime = now + VineMarkBalance.branchLifetimeTicks();
            return;
        }
        mark.stage = Stage.BUD;
        mark.branches = 3;
        mark.expireGameTime = now + VineMarkBalance.budLifetimeTicks();
        tryBind(target, VineMarkBalance.budControlTicks());
    }

    public static void upgradeFromProjectile(ServerPlayer owner, LivingEntity target) {
        if (!readyCaster(owner) || target == null || !target.isAlive()) {
            return;
        }
        BleedRecord bleed = BLEEDS.get(target.getUUID());
        long now = target.level().getGameTime();
        if (bleed == null || bleed.level < 1 || now > bleed.expireGameTime) {
            return;
        }
        if (!DamagePermission.allows(owner, target) || HarmPolicy.denies(owner, target)) {
            return;
        }
        int previousLevel = bleed.level;
        bleed.level = Math.max(2, bleed.level);
        if (previousLevel < 2) {
            bleed.nextTick = Math.min(bleed.nextTick, now + VineMarkBalance.bleedEnhancedIntervalTicks());
        }
        bleed.generation++;
    }

    public static void captureMove(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide() || !MOVEMENT.containsKey(entity.getUUID())) {
            return;
        }
        MOVE_STARTS.put(entity.getUUID(), entity.position());
    }

    public static void finishMove(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        Vec3 start = MOVE_STARTS.remove(entity.getUUID());
        if (start == null) {
            return;
        }
        acceptMove(entity, start, entity.position(), false);
    }

    public static void resetMove(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        MovementTrack track = MOVEMENT.get(entity.getUUID());
        if (track == null) {
            return;
        }
        track.level = entity.level();
        track.lastPosition = entity.position();
        MOVE_STARTS.remove(entity.getUUID());
    }

    public static void acceptMove(LivingEntity entity, Vec3 from, Vec3 destination, boolean resetSample) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        MovementTrack track = MOVEMENT.get(entity.getUUID());
        if (track == null) {
            return;
        }
        if (resetSample || track.level != entity.level()) {
            track.level = entity.level();
            track.lastPosition = destination;
            return;
        }
        double distance = from.distanceTo(destination);
        if (!Double.isFinite(distance) || distance <= 0.0d || distance > 128.0d) {
            track.lastPosition = destination;
            return;
        }
        float rate = track.bud
                ? VineMarkBalance.movementDamagePerBranchPerBlock() * 3.0f
                : VineMarkBalance.movementDamagePerBranchPerBlock() * Math.max(1, track.pricedBranches);
        track.pendingDamage += distance * rate;
        track.lastPosition = destination;
    }

    public static void clearEntity(UUID entityId) {
        if (entityId == null) {
            return;
        }
        MOVEMENT.remove(entityId);
        BLEEDS.remove(entityId);
        RESIDUALS.remove(entityId);
        MOVE_STARTS.remove(entityId);
        MARKS.keySet().removeIf(key -> entityId.equals(key.targetId) || entityId.equals(key.casterId));
        CONSUMED_ACTIONS.keySet().removeIf(key -> entityId.equals(key.targetId) || entityId.equals(key.casterId));
        ACTION_SPECTRUM.keySet().removeIf(key -> entityId.equals(key.casterId));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().overworld() == null) {
            return;
        }
        long now = event.getServer().overworld().getGameTime();
        settleMovement(event.getServer(), now);
        settleBleeding(event.getServer(), now);
        prune(now);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !entity.hasEffect(ModMobEffects.ROSE_BIND.get())) {
            return;
        }
        entity.setJumping(false);
        entity.setSprinting(false);
        if (entity instanceof Player player) {
            player.setJumping(false);
        }
    }

    @SubscribeEvent
    public static void onBindApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance() != null
                && event.getEffectInstance().getEffect() == ModMobEffects.ROSE_BIND.get()
                && immune(event.getEntity())) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        clearEntity(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clearEntity(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        resetMove(event.getEntity());
    }

    private static void bloom(ServerPlayer caster, LivingEntity target, BranchMark mark, long now) {
        MARKS.remove(new MarkKey(caster.getUUID(), target.getUUID()));
        RESIDUALS.put(target.getUUID(), now + VineMarkBalance.residualProtectionTicks());
        VineMarkDamage.pierce(target, caster.getUUID(), VineMarkBalance.bloomFlatDamage());
        refreshBleed(target, caster.getUUID(), now);
        publishFallen(caster, target);
        MovementTrack track = MOVEMENT.get(target.getUUID());
        if (track != null) {
            track.pricedBranches = Math.max(track.pricedBranches, mark.branches);
            track.bud = true;
            track.closing = true;
        }
        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, target.getX(),
                    target.getY() + target.getBbHeight() * 0.6d, target.getZ(),
                    12, 0.25d, 0.3d, 0.25d, 0.0d);
        }
    }

    private static void publishFallen(ServerPlayer caster, LivingEntity target) {
        int units = VineMarkBalance.fallenUnits();
        long potential = (long) units * VineMarkBalance.FALLEN_POTENTIAL_PER_UNIT;
        if (potential <= 0L || potential > WoodLimits.BATCH_POTENTIAL) {
            LOGGER.warn("落英批次潜力 {} 超过上限，未发布", potential);
            return;
        }
        ProductionTicket ticket = WoodResourceService.beginProduction(caster, ResourceOrigin.NORMAL_BLOOM);
        if (ticket == null) {
            LOGGER.warn("落英票据签发失败");
            return;
        }
        GroundLotSpec lot = new GroundLotSpec(FlowerKind.ROSE, units, (int) potential,
                VineMarkBalance.fallenLifetimeTicks(), target.level().dimension(), target.position());
        CreateResult result = WoodResourceService.publish(ticket, List.of(lot));
        if (!result.accepted()) {
            LOGGER.warn("落英发布失败：{}", result.reject());
        }
    }

    private static void refreshBleed(LivingEntity target, UUID casterId, long now) {
        BleedRecord bleed = BLEEDS.get(target.getUUID());
        if (bleed == null || now >= bleed.expireGameTime) {
            BLEEDS.put(target.getUUID(), new BleedRecord(casterId, 1,
                    now + VineMarkBalance.bleedDurationTicks(),
                    now + VineMarkBalance.bleedIntervalTicks(),
                    VineMarkBalance.bleedFlatDamage(), 1));
            return;
        }
        bleed.level = Math.max(1, bleed.level);
        bleed.expireGameTime = now + VineMarkBalance.bleedDurationTicks();
        bleed.generation++;
    }

    private static void settleBleeding(MinecraftServer server, long now) {
        for (Map.Entry<UUID, BleedRecord> entry : new ArrayList<>(BLEEDS.entrySet())) {
            BleedRecord bleed = entry.getValue();
            LivingEntity target = findLiving(server, entry.getKey());
            if (target == null) {
                BLEEDS.remove(entry.getKey(), bleed);
                continue;
            }
            while (now >= bleed.nextTick && bleed.nextTick <= bleed.expireGameTime) {
                if (BLEEDS.get(entry.getKey()) != bleed) {
                    break;
                }
                long dueTick = bleed.nextTick;
                int interval = bleed.level >= 2
                        ? VineMarkBalance.bleedEnhancedIntervalTicks()
                        : VineMarkBalance.bleedIntervalTicks();
                bleed.nextTick = dueTick + interval;
                boolean paid = VineMarkDamage.bleed(target, bleed.casterId, bleed.amount);
                if (!paid || BLEEDS.get(entry.getKey()) != bleed || !target.isAlive()) {
                    break;
                }
            }
            if (BLEEDS.get(entry.getKey()) == bleed
                    && now >= bleed.expireGameTime && bleed.nextTick > bleed.expireGameTime) {
                BLEEDS.remove(entry.getKey(), bleed);
            }
        }
    }

    private static void settleMovement(MinecraftServer server, long now) {
        for (Map.Entry<UUID, MovementTrack> entry : new ArrayList<>(MOVEMENT.entrySet())) {
            MovementTrack track = entry.getValue();
            LivingEntity target = findLiving(server, entry.getKey());
            if (target == null) {
                MOVEMENT.remove(entry.getKey());
                continue;
            }
            int branches = activeBranches(entry.getKey(), now);
        if (branches > 0 && !track.closing) {
            track.pricedBranches = branches;
            track.bud = branches >= 3;
        }
            if (now < track.nextSettle) {
                continue;
            }
            if (track.pendingDamage > 0.0d) {
                VineMarkDamage.pierce(target, track.casterId, (float) track.pendingDamage);
            }
            track.pendingDamage = 0.0d;
            track.nextSettle = now + VineMarkBalance.movementIntervalTicks();
            if (branches <= 0) {
                MOVEMENT.remove(entry.getKey());
            }
        }
    }

    private static void ensureMovement(ServerPlayer caster, LivingEntity target, long now) {
        MovementTrack track = MOVEMENT.computeIfAbsent(target.getUUID(), ignored -> new MovementTrack());
        track.level = target.level();
        track.lastPosition = target.position();
        track.casterId = caster.getUUID();
        track.pricedBranches = Math.max(1, activeBranches(target.getUUID(), now));
        if (track.nextSettle <= 0L) {
            track.nextSettle = now + VineMarkBalance.movementIntervalTicks();
        }
    }

    private static boolean resolveEligibility(ServerPlayer caster, UUID actionId, Boolean frozenSpectrum) {
        if (frozenSpectrum != null) {
            return frozenSpectrum;
        }
        Boolean remembered = ACTION_SPECTRUM.get(new ActionKey(caster.getUUID(), actionId, new UUID(0L, 0L)));
        if (remembered != null) {
            return remembered;
        }
        VineMarkActions.Action open = VineMarkActions.current();
        if (open != null && actionId.equals(open.actionId()) && caster.getUUID().equals(open.casterId())) {
            return open.spectrumEligible();
        }
        return false;
    }

    private static BranchMark currentMark(MarkKey key, long now) {
        BranchMark mark = MARKS.get(key);
        if (mark == null || now >= mark.expireGameTime) {
            MARKS.remove(key);
            return null;
        }
        return mark;
    }

    private static void install(MarkKey key, Stage stage, int branches, long expireGameTime) {
        MARKS.put(key, new BranchMark(stage, branches, expireGameTime));
    }

    private static int activeBranches(UUID targetId, long now) {
        int branches = 0;
        for (Map.Entry<MarkKey, BranchMark> entry : MARKS.entrySet()) {
            if (targetId.equals(entry.getKey().targetId) && now < entry.getValue().expireGameTime) {
                branches = Math.max(branches, entry.getValue().branches);
            }
        }
        return branches;
    }

    private static boolean residualActive(UUID targetId, long now) {
        Long expireGameTime = RESIDUALS.get(targetId);
        return expireGameTime != null && now < expireGameTime;
    }

    private static void tryBind(LivingEntity target, int ticks) {
        if (immune(target) || ticks <= 0) {
            return;
        }
        boolean installed = target.addEffect(new MobEffectInstance(
                ModMobEffects.ROSE_BIND.get(), ticks, 0, false, true, true));
        if (!installed) {
            LOGGER.debug("缠枝束缚被拒绝：{}", target.getUUID());
        }
    }

    private static boolean immune(LivingEntity entity) {
        return entity instanceof EnderDragon || entity instanceof WitherBoss || entity.getType() == EntityType.WARDEN;
    }

    private static LivingEntity aimedBleeding(ServerPlayer player) {
        double range = VineMarkBalance.fallenTargetRange();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        HitResult blockHit = player.serverLevel().clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 limit = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        AABB bounds = new AABB(eye, limit).inflate(1.0d);
        for (LivingEntity candidate : player.serverLevel().getEntitiesOfClass(LivingEntity.class, bounds,
                entity -> bleedingVisible(player, entity))) {
            var clip = candidate.getBoundingBox().inflate(0.3d).clip(eye, limit);
            if (clip.isEmpty()) {
                continue;
            }
            double distance = eye.distanceToSqr(clip.get());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private static boolean bleedingVisible(ServerPlayer player, LivingEntity target) {
        BleedRecord bleed = BLEEDS.get(target.getUUID());
        long now = target.level().getGameTime();
        return bleed != null && bleed.level >= 1 && now < bleed.expireGameTime
                && DamagePermission.allows(player, target) && !HarmPolicy.denies(player, target)
                && player.hasLineOfSight(target);
    }

    private static GroundFragmentView nearestFallen(ServerPlayer player) {
        double radius = VineMarkBalance.fallenSearchRadius();
        GroundQueryResult result = WoodResourceService.query(
                player.serverLevel(), player.getUUID(), player.getBoundingBox().inflate(radius), 64);
        if (!result.accepted()) {
            return null;
        }
        GroundFragmentView nearest = null;
        double bestDistance = radius * radius;
        for (GroundFragmentView view : result.fragments()) {
            if (view.flower() != FlowerKind.ROSE || view.length() <= 0) {
                continue;
            }
            double distance = player.distanceToSqr(view.coordX(), view.coordY(), view.coordZ());
            if (distance <= bestDistance) {
                bestDistance = distance;
                nearest = view;
            }
        }
        return nearest;
    }

    private static LivingEntity findLiving(MinecraftServer server, UUID entityId) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(entityId);
            if (entity instanceof LivingEntity living && living.isAlive()) {
                return living;
            }
        }
        return null;
    }

    private static boolean readyCaster(ServerPlayer player) {
        return player != null && player.isAlive() && !player.level().isClientSide();
    }

    private static void prune(long now) {
        MARKS.entrySet().removeIf(entry -> now >= entry.getValue().expireGameTime);
        RESIDUALS.entrySet().removeIf(entry -> now >= entry.getValue());
    }

    private enum Stage {
        ONE,
        TWO,
        BUD
    }

    private record MarkKey(UUID casterId, UUID targetId) {
    }

    private record ActionKey(UUID casterId, UUID actionId, UUID targetId) {
    }

    private static final class BranchMark {
        private Stage stage;
        private int branches;
        private long expireGameTime;

        private BranchMark(Stage stage, int branches, long expireGameTime) {
            this.stage = stage;
            this.branches = branches;
            this.expireGameTime = expireGameTime;
        }
    }

    private static final class MovementTrack {
        private net.minecraft.world.level.Level level;
        private Vec3 lastPosition = Vec3.ZERO;
        private UUID casterId = new UUID(0L, 0L);
        private int pricedBranches = 1;
        private boolean bud;
        private boolean closing;
        private double pendingDamage;
        private long nextSettle;
    }

    private static final class BleedRecord {
        private final UUID casterId;
        private final float amount;
        private int level;
        private long expireGameTime;
        private long nextTick;
        private int generation;

        private BleedRecord(UUID casterId, int level, long expireGameTime, long nextTick,
                            float amount, int generation) {
            this.casterId = casterId;
            this.level = level;
            this.expireGameTime = expireGameTime;
            this.nextTick = nextTick;
            this.amount = amount;
            this.generation = generation;
        }
    }
}
