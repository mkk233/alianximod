package com.bslx.XueXiMod.shu.technique.release.clone.capture;

import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.BalanceSide;
import com.bslx.XueXiMod.shu.balance.ConfiguredDamage;
import com.bslx.XueXiMod.shu.balance.RawDamage;
import com.bslx.XueXiMod.shu.balance.BalanceService;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CapturedAttackSnapshot;
import com.bslx.XueXiMod.shu.technique.release.clone.core.DetachedActorFrame;
import com.bslx.XueXiMod.shu.technique.release.clone.core.VolumeVisualKind;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * 断潮的无附魔静止斩。只冻结现有配置和单段无附魔公式，不调用旧效果、旧生成或旧命中。
 */
public final class TideSeverActorAdapter implements ActorPayloadAdapter {
    public static final String TECHNIQUE_ID = "tide_sever";
    private static final int VERSION = 1;
    /** 键尚未生效时，与断潮现有读口使用的同一原始默认。键生效后不会盖过配置。 */
    private static final double INACTIVE_DEPTH = 5.0d;
    private static final double INACTIVE_HEIGHT = 3.0d;
    private static final String UNSUPPORTED_TEXT = "水幻形后仅支持一段已适配水系攻击；本版支持薄水切";
    private static final String INVALID_TEXT = "薄水切的伤害、法力或冷却无法冻结";
    private static final String GEOMETRY_TEXT = "薄水切的尺寸或寿命不是有限正数";
    private static final String MISMATCH_TEXT = "薄水切适配版本已经变化";

    @Override
    public String techniqueId() {
        return TECHNIQUE_ID;
    }

    @Override
    public ShuAttribute attribute() {
        return ShuAttribute.WATER;
    }

    @Override
    public int version() {
        return VERSION;
    }

    @Override
    public CaptureQuote freezeQuote(CaptureQuoteContext context) {
        if (context == null || context.payload() == null) {
            return CaptureQuote.refused(PayloadQuoteFault.UNSUPPORTED, UNSUPPORTED_TEXT, TECHNIQUE_ID, 0);
        }
        Technique payload = context.payload();
        String payloadId = payload.getId() == null || payload.getId().isBlank() ? TECHNIQUE_ID : payload.getId();
        if (!TECHNIQUE_ID.equals(payload.getId()) || payload.getAttribute() != ShuAttribute.WATER) {
            return CaptureQuote.refused(PayloadQuoteFault.UNSUPPORTED, UNSUPPORTED_TEXT, payloadId, 0);
        }
        ActorPayloadAdapter registered = ActorPayloadRegistry.find(TECHNIQUE_ID);
        if (registered == null || registered.version() != version()) {
            return CaptureQuote.refused(PayloadQuoteFault.ADAPTER_MISMATCH, MISMATCH_TEXT, TECHNIQUE_ID, version());
        }
        double depthBlocks;
        double heightBlocks;
        double widthBlocks;
        int lifetimeTicks;
        try {
            depthBlocks = SpecialBalance.number(TECHNIQUE_ID, BalanceKeys.BEAM_LENGTH, INACTIVE_DEPTH);
            heightBlocks = SpecialBalance.number(TECHNIQUE_ID, BalanceKeys.BOX_HEIGHT, INACTIVE_HEIGHT);
            ReleaseDefinition definition = ReleaseRegistry.get(TECHNIQUE_ID);
            if (definition == null) {
                return CaptureQuote.refused(PayloadQuoteFault.GEOMETRY, GEOMETRY_TEXT, TECHNIQUE_ID, version());
            }
            widthBlocks = definition.thickness();
            lifetimeTicks = definition.lifetimeTicks();
        } catch (RuntimeException failure) {
            return CaptureQuote.refused(PayloadQuoteFault.GEOMETRY, GEOMETRY_TEXT, TECHNIQUE_ID, version());
        }
        if (!ActorVolumeGeometry.finitePositive(depthBlocks)
                || !ActorVolumeGeometry.finitePositive(heightBlocks)
                || !ActorVolumeGeometry.finitePositive(widthBlocks)
                || lifetimeTicks <= 0) {
            return CaptureQuote.refused(PayloadQuoteFault.GEOMETRY, GEOMETRY_TEXT, TECHNIQUE_ID, version());
        }
        try {
            float magicDamage = 0.0f;
            float baseDamage = payload.getBaseDamage();
            float scaledBase = scaledBaseDamage(baseDamage, context.attributeDamageBonus());
            float rawAmount = baseDamage > 0.0f
                    ? scaledBase + context.weaponBonus() + magicDamage
                    : scaledBase + context.weaponBonus();
            ConfiguredDamage configuredDamage = BalanceService.damageContribution(
                    BalanceSide.SERVER, TECHNIQUE_ID, RawDamage.of(rawAmount));
            int configuredMana = BalanceService.manaCost(
                    BalanceSide.SERVER, TECHNIQUE_ID, BalanceKeys.MANA_COST, payload.manaCost());
            int configuredCooldownTicks = BalanceService.cooldownTicks(
                    BalanceSide.SERVER, TECHNIQUE_ID, payload.getBaseCooldown());
            if (configuredDamage == null
                    || !withinAmount(configuredDamage.amount())
                    || !withinAmount(configuredMana)
                    || !withinAmount(configuredCooldownTicks)) {
                return CaptureQuote.refused(PayloadQuoteFault.INVALID_QUOTE, INVALID_TEXT, TECHNIQUE_ID, version());
            }
            return CaptureQuote.accepted(TECHNIQUE_ID, version(), configuredDamage, configuredMana,
                    configuredCooldownTicks, depthBlocks, heightBlocks, widthBlocks, lifetimeTicks);
        } catch (RuntimeException failure) {
            return CaptureQuote.refused(PayloadQuoteFault.INVALID_QUOTE, INVALID_TEXT, TECHNIQUE_ID, version());
        }
    }

    @Override
    public ActorVolumeSpec instantiate(CapturedAttackSnapshot payload, DetachedActorFrame actorFrame,
                                       CapturedActionAuthority authority) {
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(actorFrame, "actorFrame");
        Objects.requireNonNull(authority, "authority");
        ResourceKey<Level> dimension = Objects.requireNonNull(actorFrame.dimension(), "dimension");
        UUID ownerId = Objects.requireNonNull(authority.ownerId(), "ownerId");
        UUID rootEventId = Objects.requireNonNull(authority.rootEventId(), "rootEventId");
        UUID deploymentId = Objects.requireNonNull(authority.deploymentId(), "deploymentId");
        Vec3[] axes = ActorVolumeGeometry.basis(actorFrame.lookUnit());
        boolean identityReady = TECHNIQUE_ID.equals(payload.payloadTechniqueId())
                && authority.adapterVersion() == payload.adapterVersion()
                && authority.sessionToken() == actorFrame.sessionToken()
                && Objects.equals(ownerId, payload.ownerId())
                && Objects.equals(ownerId, actorFrame.ownerId())
                && Objects.equals(rootEventId, payload.rootEventId())
                && Objects.equals(rootEventId, actorFrame.rootEventId())
                && Objects.equals(deploymentId, actorFrame.deploymentId());
        boolean shapeReady = identityReady
                && axes != null
                && payload.configuredDamage() != null
                && payload.lifetimeTicks() > 0
                && ActorVolumeGeometry.finitePositive(payload.depthBlocks())
                && ActorVolumeGeometry.finitePositive(payload.heightBlocks())
                && ActorVolumeGeometry.finitePositive(payload.widthBlocks())
                && actorFrame.eyeOrigin() != null;
        Vec3 eyeOrigin = shapeReady ? actorFrame.eyeOrigin() : Vec3.ZERO;
        Vec3 forwardUnit = shapeReady ? axes[0] : Vec3.ZERO;
        Vec3 upUnit = shapeReady ? axes[1] : Vec3.ZERO;
        Vec3 rightUnit = shapeReady ? axes[2] : Vec3.ZERO;
        ConfiguredDamage configuredDamage = shapeReady
                ? payload.configuredDamage()
                : ConfiguredDamage.retained(0.0f);
        return new ActorVolumeSpec(
                authority.sessionToken(),
                dimension,
                ownerId,
                rootEventId,
                deploymentId,
                authority.actionToken(),
                eyeOrigin,
                forwardUnit,
                upUnit,
                rightUnit,
                shapeReady ? payload.depthBlocks() : 0.0d,
                shapeReady ? payload.heightBlocks() : 0.0d,
                shapeReady ? payload.widthBlocks() : 0.0d,
                shapeReady ? payload.lifetimeTicks() : 0,
                configuredDamage,
                payload.adapterVersion(),
                VolumeVisualKind.TIDE_SLASH_FULL);
    }

    private static float scaledBaseDamage(float baseDamage, @Nullable Double attributeDamageBonus) {
        if (attributeDamageBonus == null || baseDamage <= 0.0f) {
            return baseDamage;
        }
        return (float) Math.max(0.0d, baseDamage * (1.0d + attributeDamageBonus));
    }

    private static boolean withinAmount(float amount) {
        return Float.isFinite(amount) && amount >= 0.0f && amount <= CaptureQuote.AMOUNT_LIMIT;
    }

    private static boolean withinAmount(int amount) {
        return amount >= 0 && amount <= CaptureQuote.AMOUNT_LIMIT;
    }
}
