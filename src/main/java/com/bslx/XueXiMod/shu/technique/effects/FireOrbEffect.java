package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.carrier.BatchFlameTrail;
import com.bslx.XueXiMod.carrier.BatchFlightRecord;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.batch.BirthLayout;
import com.bslx.XueXiMod.shu.technique.release.batch.ImpactPartition;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
import com.bslx.XueXiMod.shu.technique.release.relations.HarmPolicy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class FireOrbEffect implements TechniqueEffect {
    private static final int BURN_SECONDS = 4;
    static final double HIT_EDGE = 0.45D;

    public static void declareBalance() {
        SpecialBalance.publish(SpecialBalance.blocks(
                SpecialBalance.begin("fire_orb"), BalanceKeys.HIT_SIZE, HIT_EDGE, "火球命中边长"));
    }

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        float speed = definition != null ? definition.speed() : 1.2F;
        int lifetime = definition != null ? definition.lifetimeTicks() : 40;
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Consumer<Vec3> trigger = cast.onFirstTrigger();
        AtomicBoolean triggered = new AtomicBoolean(false);
        TechniqueHitSpec spec = new TechniqueHitSpec();
        spec.techniqueId = technique.getId();
        spec.path = TechniqueHitSpec.Path.STRAIGHT;
        double hitEdge = SpecialBalance.number("fire_orb", BalanceKeys.HIT_SIZE, HIT_EDGE);
        spec.length = hitEdge;
        spec.width = hitEdge;
        spec.height = hitEdge;
        spec.speed = speed;
        spec.lifetime = lifetime;
        spec.lockThisCast = cast.lockThisCast();
        spec.dropThisCast = cast.dropThisCast();
        spec.visual = 4;
        spec.modelScale = 1.2F;
        spec.seedPoints = 3;
        spec.retainAfterEntityHit = true;
        spec.listener = new TechniqueHitSpec.HitListener() {
            @Override
            public void onHit(LivingEntity target) {
                if (HarmPolicy.denies(player, target)) {
                    return;
                }
                target.setSecondsOnFire(BURN_SECONDS);
                if (trigger != null && triggered.compareAndSet(false, true)) {
                    trigger.accept(target.position());
                }
            }

            @Override
            public void onExpire(Vec3 position) {
                if (trigger != null && triggered.compareAndSet(false, true)) {
                    trigger.accept(position);
                }
            }
        };
        spec.publishedHook = hitEntity -> attachTrail(player, spec, hitEntity);
        TechniqueHitSpec.spawn(serverLevel, player, spec);
    }

    private static void attachTrail(ServerPlayer player, TechniqueHitSpec spec, TechniqueHitEntity hitEntity) {
        BirthLayout.Slot slot = BirthLayout.current();
        if (slot == null || slot.hitClock == null) {
            return;
        }
        BatchFlightRecord record = new BatchFlightRecord();
        record.techniqueKey = "fire_orb";
        record.hitClock = slot.hitClock;
        record.rootCastId = slot.rootCastId;
        record.ownerIdentifier = player.getUUID();
        record.resourceCommit = slot.resourceCommit;
        record.resourceLoad = slot.resourceLoad == null ? ResourceEnchantLoad.EMPTY : slot.resourceLoad;
        record.bodyScaledBase = slot.bodyScaledBase;
        record.legacyFixedPoints = Math.max(0, spec.settleSeedPoints);
        CastFrame frame = CastFrame.current();
        record.damage = frame == null ? 0.0F : frame.damage;
        record.follow = slot.follow;
        record.partition = slot.partition == null ? ImpactPartition.none() : slot.partition;
        record.occlusion = slot.occlusion;
        BatchFlameTrail.attachLoose(hitEntity, record);
    }
}
