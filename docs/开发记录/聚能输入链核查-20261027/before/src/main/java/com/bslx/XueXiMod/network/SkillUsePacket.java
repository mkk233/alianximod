package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.debug.DebugLog;
import com.bslx.XueXiMod.item.SkillCarrier;
import com.bslx.XueXiMod.item.StaffData;
import com.bslx.XueXiMod.item.StaffItem;
import com.bslx.XueXiMod.item.StaffSkills;
import com.bslx.XueXiMod.player.MagicPool;
import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import com.bslx.XueXiMod.player.PlayerBoundSkills;
import com.bslx.XueXiMod.player.PlayerShuData;
import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.shu.technique.EnchantmentAccumulator;
import com.bslx.XueXiMod.shu.technique.EnchantmentDefinition;
import com.bslx.XueXiMod.shu.technique.EnchantmentRegistry;
import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.TechniqueEffectRegistry;
import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.chain.TechniqueChain;
import com.bslx.XueXiMod.shu.technique.effects.LockTargetService;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantBonus;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.enchant.WeaponEnchant;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalIntent;
import com.bslx.XueXiMod.shu.technique.release.entity.HostCast;
import com.bslx.XueXiMod.shu.technique.release.water.CastSource;
import com.bslx.XueXiMod.shu.technique.release.water.WaterBeatBridge;
import com.bslx.XueXiMod.shu.technique.release.entity.HostGroupCommit;
import com.bslx.XueXiMod.shu.technique.release.field.FieldSpawner;
import com.bslx.XueXiMod.shu.technique.release.guard.GuardStates;
import com.bslx.XueXiMod.shu.technique.release.momentum.ChargeGate;
import com.bslx.XueXiMod.shu.technique.release.momentum.WaterMomentum;
import com.bslx.XueXiMod.shu.technique.sword.SwordEntity;
import com.bslx.XueXiMod.shu.technique.release.ReleaseTag;
import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SkillUsePacket {
    private final int slotIndex;

    public SkillUsePacket(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(slotIndex);
    }

    public static SkillUsePacket decode(FriendlyByteBuf buf) {
        return new SkillUsePacket(buf.readInt());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;

            ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
            boolean staff = StaffItem.holds(mainHand);
            double cooldownReduction = 0.0;
            Double attributeDamageBonus = null;
            if (staff) {
                if (slotIndex != 0) return;
                StaffData data = ((StaffItem) mainHand.getItem()).data();
                cooldownReduction = data.cooldownReduction();
                attributeDamageBonus = data.attributeDamageBonus();
            } else {
                if (slotIndex < 0 || slotIndex > 3) return;
                if (!SkillCarrier.canRelease(mainHand)) {
                    Skill peek = PlayerBoundSkills.get(player, slotIndex);
                    if (peek == null || !SkillSegmentMath.allowsEmptyHandSwordCast(player, peek)) {
                        return;
                    }
                }
            }

            Skill skill = staff ? StaffSkills.asSkill(mainHand) : PlayerBoundSkills.get(player, slotIndex);
            if (skill == null) {
                player.sendSystemMessage(Component.literal("§c这个槽位还没有装配剑技"));
                return;
            }
            String heldSwordRefusal = HeldSwordGate.refusal(mainHand, skill.getTechniques());
            if (heldSwordRefusal != null) {
                HeldSwordGate.tellLimited(player, heldSwordRefusal);
                return;
            }
            if (staff) {
                for (Technique listed : skill.getTechniques()) {
                    if (listed != null && listed.getType() == TechniqueType.SWORD) {
                        player.sendSystemMessage(Component.literal("§c剑术不能由法杖释放"));
                        return;
                    }
                }
            }

            if (CastCooldown.busy(player, mainHand, slotIndex)) {
                DebugLog.log("释放中断：槽" + (slotIndex + 1) + " 链条尚未结束");
                return;
            }
            DebugLog.log("释放槽" + (slotIndex + 1) + " " + skill.getName());

            CastTiming timing = executeSkill(player, skill, mainHand, slotIndex, cooldownReduction, attributeDamageBonus);
            if (!timing.accepted()) {
                return;
            }
            if (!timing.settlesLater()) {
                CastCooldown.mark(player, mainHand, slotIndex, timing.replayTick());
                DebugLog.log("释放时刻 开始=" + timing.startedTick() + " 末段=" + timing.finishedTick() + " 重放=" + timing.replayTick());
            } else {
                DebugLog.log("双重释放已接受，冷却等到这一发结束");
            }

            PlayerShuData data = PlayerShuData.get(player);
            boolean leveledUp = false;
            for (Technique technique : timing.fired()) {
                data.incrementUsage(technique.getId());
                if (data.addUsageAndTryLevelUp(technique.getType(), player)) {
                    leveledUp = true;
                }
            }
            PlayerShuData.save(player, data);

            if (leveledUp) {
                com.bslx.XueXiMod.player.DomainLearning.offerReady(player);
                com.bslx.XueXiMod.network.ModNetwork.sendToPlayer(
                    player, new com.bslx.XueXiMod.network.ShuSyncPacket(data));
            }
        });
        context.get().setPacketHandled(true);
    }

    private CastTiming executeSkill(ServerPlayer player, Skill skill, ItemStack weapon, int slot,
                             double cooldownReduction, Double attributeDamageBonus) {
        double weaponDamage = player.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
        float weaponBonus = (float) Math.max(0, weaponDamage - 1.0);

        List<Technique> assembled = skill.getTechniques();
        if (SkillSegmentMath.refusesMixedChain(assembled)) {
            player.sendSystemMessage(Component.literal("§c这个操纵术式不能和其他术式装配"));
            return CastTiming.rejected();
        }
        String heldSwordRefusal = HeldSwordGate.refusal(weapon, assembled);
        if (heldSwordRefusal != null) {
            HeldSwordGate.tellLimited(player, heldSwordRefusal);
            return CastTiming.rejected();
        }
        CastSource castSource = WaterBeatBridge.capture(player, slot);

        List<List<Integer>> chainSteps = ChainSteps.list(skill);
        boolean stepped = !chainSteps.isEmpty();
        int stepCooldown = 0;
        String stepSignature = "";
        int stepCount = chainSteps.size();
        if (stepped) {
            stepSignature = ChainSteps.signature(skill);
            int cursor = ChainProgress.current(player, weapon, slot, stepSignature, stepCount);
            List<Integer> stepIndexes = chainSteps.get(cursor);
            stepCooldown = ChainSteps.cooldown(skill, stepIndexes, cooldownReduction);
            skill = ChainSteps.slice(skill, stepIndexes);
        }
        final int frozenStepCooldown = stepCooldown;

        List<Technique> techniques = skill.getTechniques();
        final Skill replaySkill = skill;
        List<ChainSegment> segments = TechniqueChain.split(skill);

        boolean hasPlayable = false;
        for (ChainSegment segment : segments) {
            Technique listed = techniques.get(segment.actionIndex());
            if (SkillSegmentMath.isRemovedRule(listed.getId()) || SkillSegmentMath.isChainModifier(listed)) {
                continue;
            }
            hasPlayable = true;
            break;
        }
        if (!stepped && segments.isEmpty() && legalEnchantChain(techniques)) {
            return imprintEnchantChain(player, skill, techniques, cooldownReduction);
        }
        boolean hasSwordManip = SkillSegmentMath.hasSwordManipulation(techniques);
        if (!hasPlayable) {
            player.sendSystemMessage(Component.literal("§c当前剑技没有可释放的伤害术式"));
            return CastTiming.rejected();
        }
        if (hasSwordManip) {
            boolean holdingSword = weapon.getItem() instanceof SwordItem;
            boolean worldSword = SkillSegmentMath.hasPlantedWorldSword(player);
            if (!holdingSword && !worldSword) {
                player.sendSystemMessage(Component.literal("§c需要手持剑"));
                return CastTiming.rejected();
            }
        }
        String contactRefusal = ContactCast.refusal(skill, segments);
        if (contactRefusal != null) {
            player.sendSystemMessage(Component.literal("§c" + contactRefusal));
            return CastTiming.rejected();
        }
        PlayerShuData casterData = PlayerShuData.get(player);
        for (Technique listed : techniques) {
            ReleaseDefinition listedDefinition = ReleaseRegistry.get(listed.getId());
            if (listedDefinition != null && listedDefinition.domain() != null && !casterData.isUnlocked(listed)) {
                player.sendSystemMessage(Component.literal("§c还没有学会释放" + listed.getName()));
                return CastTiming.rejected();
            }
        }

        CompiledReleaseStep dualPlan = CompiledReleaseStep.compile(skill, techniques, segments, cooldownReduction);
        if (dualPlan != null) {
            if (dualPlan.chargeRefusal != null) {
                player.sendSystemMessage(Component.literal("§c" + dualPlan.chargeRefusal));
                return CastTiming.rejected();
            }
            return CastExecutionSession.open(player, weapon, slot, castSource, skill, techniques, segments, dualPlan, weaponBonus,
                    stepSignature, stepCount);
        }

        final boolean conditionalChain = chainHasConditional(techniques, segments);
        final String conditionalLockKey = conditionalChain ? CastCooldown.carrierKey(player, weapon, slot) : null;
        final AtomicBoolean conditionalLockReleased = new AtomicBoolean(!conditionalChain);
        if (conditionalChain && !CastCooldown.acquire(conditionalLockKey)) {
            return CastTiming.rejected();
        }

        int cost = SkillChainPlan.pressCommitCost(techniques, segments);
        if (!MagicPool.trySpend(player, cost)) {
            releaseConditionalLock(conditionalLockKey, conditionalLockReleased);
            return CastTiming.insufficient();
        }
        ManaPaymentReceipt pressReceipt = MagicPool.lastReceipt(player.getUUID());
        if (cost > 0) {
            MagicPool.showBalance(player);
        }

        final HostCast conditionalCast;
        try {
            conditionalCast = openConditionalCast(player, techniques, segments, castSource);
        } catch (RuntimeException failure) {
            releaseConditionalLock(conditionalLockKey, conditionalLockReleased);
            throw failure;
        }
        final AtomicInteger pendingConditional = new AtomicInteger();
        final AtomicInteger conditionalCommitted = new AtomicInteger();
        final AtomicBoolean conditionalFailed = new AtomicBoolean();
        final AtomicBoolean conditionalAborted = new AtomicBoolean();
        final AtomicBoolean handedOff = new AtomicBoolean();
        final AtomicBoolean parentSettled = new AtomicBoolean();
        final AtomicInteger succeededConditionalScaled = new AtomicInteger();
        final AtomicInteger capturedLastStart = new AtomicInteger();
        final AtomicReference<Technique> capturedTail = new AtomicReference<>();
        final AtomicBoolean enchantApplied = new AtomicBoolean();
        final Set<String> recordedUsage = new HashSet<>();
        final Set<String> succeededConditionalIds = new HashSet<>();
        final List<Technique> ordinarySucceeded = new ArrayList<>();
        final List<UncommittedShare> conditionalClaims = new ArrayList<>();
        final String frozenSignature = stepSignature;
        final int openedTick = player.getServer().getTickCount();
        int lastStartTick = openedTick;
        Technique tailTechnique = null;
        int failedFromIndex = -1;
        try {
        ReleaseBatch.open(player, skill);
        int startTick = openedTick;
        lastStartTick = startTick;
        ChainSegment previous = null;
        boolean removedRuleWarned = false;
        boolean enchantImprinted = false;
        boolean stepFailed = false;

        int segmentIndex = 0;
        while (segmentIndex < segments.size()) {
            ChainSegment segment = segments.get(segmentIndex);
            Technique action = techniques.get(segment.actionIndex());

            int gap = 0;
            if (previous != null && !stepped) {
                gap = SkillSegmentMath.scaleCooldown(TechniqueChain.gapBefore(segment, previous, skill), cooldownReduction);
                startTick += gap;
            }
            lastStartTick = startTick;
            tailTechnique = action;

            if (SkillSegmentMath.isRemovedRule(action.getId())) {
                if (!removedRuleWarned) {
                    removedRuleWarned = true;
                    player.sendSystemMessage(Component.literal("§c链里有已移除的术式，请重新装配"));
                }
                segmentIndex += 1;
                previous = segment;
                continue;
            }

            if (action.isEnchantment()) {
                if (!enchantImprinted) {
                    SkillChainPlan.imprintEnchant(player, skill, techniques, 1);
                    enchantImprinted = true;
                    enchantApplied.set(true);
                }
                tailTechnique = action;
                segmentIndex += 1;
                previous = segment;
                continue;
            }

            ChainRole role = SkillSegmentMath.roleOf(action);
            ReleaseDefinition actionDef = ReleaseRegistry.get(action.getId());
            if (actionDef != null && actionDef.manipulatesSwordEntities()) {
                ChainScheduler.schedule(player.getServer(), startTick,
                        SkillSegmentRunner.buildSegmentRunner(player, action, techniques, segment.actionIndex(), weapon, 0f, false,
                                EnchantmentResolution.empty(), null, false, false));
                if (conditionalChain) {
                    rememberOrdinarySuccess(ordinarySucceeded, action);
                }
                tailTechnique = action;
                segmentIndex += 1;
                previous = segment;
                continue;
            }
            if (role == ChainRole.ACTION) {
                SkillChainPlan.ModifierPlan plan = SkillChainPlan.collectModifiers(segments, techniques, segmentIndex + 1);
                if (plan.attackSegment >= 0) {
                    SkillChainPlan.ModifierPlan captured = plan;
                    ServerLevel castLevel = player.serverLevel();
                    int castEpoch = ChargeGate.epoch(player.getUUID());
                    final Skill castSkill = skill;
                    if (captured.dualRuleSegment < 0) {
                        int attackShare = SkillSegmentMath.segmentManaCost(techniques, segments.get(captured.attackSegment));
                        int memberCount = Math.max(0, captured.volleys) * Math.max(0, captured.copies);
                        UncommittedShare.Pool reservedShares = UncommittedShare.reserve(pressReceipt, attackShare, memberCount);
                        AttackBudgetWindow window = AttackBudgetWindow.open(player, castLevel, castEpoch, reservedShares,
                                castSkill, techniques, segments, captured, weaponBonus, castSource);
                        ChainScheduler.schedule(player.getServer(), startTick, () -> {
                            if (!window.ownerPresent() || !HeldSwordGate.allows(action, player.getMainHandItem())) {
                                window.finish();
                                return;
                            }
                            if (!SkillSegmentRunner.playAction(player, action, techniques, segment.actionIndex(), weapon, window)) {
                                window.finish();
                            }
                        });
                    } else {
                        ChainScheduler.schedule(player.getServer(), startTick,
                                SkillSegmentRunner.buildSegmentRunner(player, action, techniques, segment.actionIndex(), weapon, 0f, false,
                                        EnchantmentResolution.empty(), position -> {
                                            if (!player.isAlive() || player.level() != castLevel
                                                    || !ChargeGate.same(player.getUUID(), castEpoch)) {
                                                return;
                                            }
                                            SkillChainPlan.firePlan(player, castSkill, techniques, segments, captured, weaponBonus,
                                                    player.getServer().getTickCount(), pressReceipt, castSource);
                                        },
                                        false, false));
                    }
                    if (conditionalChain) {
                        rememberOrdinarySuccess(ordinarySucceeded, action);
                    }
                    tailTechnique = action;
                    segmentIndex = plan.resume;
                    previous = segment;
                    continue;
                }
            }
            if (SkillSegmentMath.isChainModifier(action)) {
                SkillChainPlan.ModifierPlan plan = SkillChainPlan.collectModifiers(segments, techniques, segmentIndex);
                if (plan.attackSegment < 0) {
                    if (plan.dualRuleSegment >= 0) {
                        int firedUntil = SkillChainPlan.fireDualSequence(player, skill, techniques, segments, plan, weaponBonus, startTick, castSource);
                        if (firedUntil < 0) {
                            stepFailed = true;
                            failedFromIndex = segmentIndex;
                            break;
                        }
                        if (firedUntil > lastStartTick) {
                            lastStartTick = firedUntil;
                        }
                    } else if (!ContactCast.keptForContact(techniques, segments, segmentIndex)) {
                        SkillChainPlan.imprintEnchant(player, skill, techniques, plan.copies);
                        enchantImprinted = true;
                        enchantApplied.set(true);
                    }
                    segmentIndex = Math.max(plan.resume, segmentIndex + 1);
                    previous = segment;
                    continue;
                }
                    int firedUntil = SkillChainPlan.firePlan(player, skill, techniques, segments, plan, weaponBonus, startTick, pressReceipt, castSource);
                if (firedUntil < 0) {
                    stepFailed = true;
                    failedFromIndex = segmentIndex;
                    break;
                }
                if (firedUntil > lastStartTick) {
                    lastStartTick = firedUntil;
                }
                Technique closing = SkillChainPlan.closingAttack(techniques, segments, plan);
                if (closing != null) {
                    tailTechnique = closing;
                    if (conditionalChain) {
                        rememberOrdinarySuccess(ordinarySucceeded, closing);
                    }
                }
                segmentIndex = plan.resume;
                previous = segments.get(plan.attackSegment);
                continue;
            }

            if (actionDef != null && actionDef.domain() != null && action.getBaseDamage() <= 0) {
                int domainCopies = SkillChainPlan.copiesBefore(segments, techniques, segmentIndex);
                EnchantmentSnapshot domainSnapshot = ContactCast.snapshot(skill, segment, player);
                if (!com.bslx.XueXiMod.shu.technique.release.domain.DomainService.tryWave(
                        player, action, actionDef.domain(), domainCopies, domainSnapshot)) {
                    stepFailed = true;
                    failedFromIndex = segmentIndex;
                    break;
                }
                if (conditionalChain) {
                    rememberOrdinarySuccess(ordinarySucceeded, action);
                }
                tailTechnique = action;
                segmentIndex += 1;
                previous = segment;
                continue;
            }

            if (actionDef != null && actionDef.barrier() != null && action.getBaseDamage() <= 0) {
                int barrierCopies = SkillChainPlan.copiesBefore(segments, techniques, segmentIndex);
                EnchantmentSnapshot barrierSnapshot = ContactCast.snapshot(skill, segment, player);
                if (!com.bslx.XueXiMod.shu.technique.release.barrier.BarrierService.tryPlace(
                        player, actionDef.barrier(), barrierCopies, barrierSnapshot)) {
                    stepFailed = true;
                    failedFromIndex = segmentIndex;
                    break;
                }
                if (conditionalChain) {
                    rememberOrdinarySuccess(ordinarySucceeded, action);
                }
                tailTechnique = action;
                segmentIndex += 1;
                previous = segment;
                continue;
            }

            if (actionDef != null && actionDef.guard() != null && action.getBaseDamage() <= 0) {
                long waveKey = ((long) openedTick << 8) + segmentIndex;
                if (!GuardStates.schedule(player, actionDef.guard(), startTick, waveKey)) {
                    stepFailed = true;
                    failedFromIndex = segmentIndex;
                    break;
                }
                if (conditionalChain) {
                    rememberOrdinarySuccess(ordinarySucceeded, action);
                }
                EnchantmentSnapshot guardSnapshot = ContactCast.snapshot(skill, segment, player);
                if (guardSnapshot != null) {
                    GuardStates.bindContact(player, guardSnapshot);
                }
                tailTechnique = action;
                segmentIndex += 1;
                previous = segment;
                continue;
            }

            if (actionDef != null && action.getBaseDamage() <= 0f && actionDef.conditionalEffect() != null) {
                int segmentShare = SkillSegmentMath.segmentManaCost(techniques, segment);
                EnchantmentSnapshot conditionalSnapshot = ContactCast.snapshot(skill, segment, player);
                UUID operationId = conditionalCast == null
                        ? null
                        : ConditionalIntent.operationId(conditionalCast.tokenId(), segmentIndex, 0, null);
                UncommittedShare claim = UncommittedShare.open(pressReceipt, segmentShare);
                conditionalClaims.add(claim);
                final int scaledCooldown = SkillSegmentMath.scaleCooldown(action.getBaseCooldown(), cooldownReduction);
                if (startTick <= openedTick) {
                    ReleaseOutcome outcome = SkillSegmentRunner.commitConditional(player, actionDef, operationId, conditionalSnapshot);
                    if (!outcome.committed()) {
                        claim.cancel(player);
                        int laterCost = SkillChainPlan.pressCommitCost(techniques, segments, segmentIndex + 1);
                        if (laterCost > 0 && pressReceipt != null) {
                            pressReceipt.refundShare(player, laterCost);
                        }
                        if (!outcome.reason().isEmpty()) {
                            player.sendSystemMessage(Component.literal("§c" + outcome.reason()));
                        }
                        conditionalFailed.set(true);
                        break;
                    }
                    claim.commitRetained();
                    conditionalCommitted.incrementAndGet();
                    succeededConditionalIds.add(action.getId());
                    succeededConditionalScaled.updateAndGet(alreadyScaled -> alreadyScaled > scaledCooldown ? alreadyScaled : scaledCooldown);
                } else {
                    pendingConditional.incrementAndGet();
                    if (conditionalCast != null) {
                        conditionalCast.hold();
                    }
                    ServerLevel conditionalLevel = player.serverLevel();
                    int conditionalEpoch = ChargeGate.epoch(player.getUUID());
                    ReleaseDefinition scheduledDefinition = actionDef;
                    Technique scheduledTechnique = action;
                    AtomicBoolean entered = new AtomicBoolean();
                    ChainScheduler.schedule(player.getServer(), startTick, () -> {
                        if (!entered.compareAndSet(false, true)) {
                            return;
                        }
                        boolean committedNow = false;
                        try {
                            if (conditionalAborted.get() || conditionalFailed.get() || !player.isAlive()
                                    || player.level() != conditionalLevel || !ChargeGate.same(player.getUUID(), conditionalEpoch)) {
                                if (!claim.kept()) {
                                    claim.cancel(player);
                                }
                                return;
                            }
                            ReleaseOutcome outcome = SkillSegmentRunner.commitConditional(
                                    player, scheduledDefinition, operationId, conditionalSnapshot);
                            if (outcome.committed()) {
                                claim.commitRetained();
                                committedNow = true;
                                conditionalCommitted.incrementAndGet();
                                succeededConditionalIds.add(scheduledTechnique.getId());
                                succeededConditionalScaled.updateAndGet(alreadyScaled -> alreadyScaled > scaledCooldown ? alreadyScaled : scaledCooldown);
                            } else {
                                claim.cancel(player);
                                conditionalFailed.set(true);
                                if (!outcome.reason().isEmpty()) {
                                    player.sendSystemMessage(Component.literal("§c" + outcome.reason()));
                                }
                            }
                        } catch (RuntimeException failure) {
                            if (!committedNow && !claim.kept()) {
                                claim.cancel(player);
                                conditionalFailed.set(true);
                            }
                            DebugLog.log("条件释放失败：" + failure);
                        } finally {
                            try {
                                if (pendingConditional.decrementAndGet() == 0) {
                                    settleConditionalParent(player, weapon, slot, stepped, openedTick, frozenStepCooldown,
                                            capturedLastStart.get(), capturedTail.get(), cooldownReduction, replaySkill, techniques,
                                            frozenSignature, stepCount, conditionalCommitted, succeededConditionalScaled,
                                            ordinarySucceeded, succeededConditionalIds, enchantApplied, recordedUsage, parentSettled, true);
                                }
                            } finally {
                                if (pendingConditional.get() <= 0) {
                                    releaseConditionalLock(conditionalLockKey, conditionalLockReleased);
                                }
                                if (conditionalCast != null) {
                                    conditionalCast.release();
                                }
                            }
                        }
                    });
                }
                tailTechnique = action;
                segmentIndex += 1;
                previous = segment;
                continue;
            }

            if (actionDef != null && actionDef.fieldRadius() > 0f && action.getBaseDamage() <= 0) {
                int fieldCopies = SkillChainPlan.copiesBefore(segments, techniques, segmentIndex);
                int epoch = ChargeGate.epoch(player.getUUID());
                WaterMomentum.Hold hold = WaterMomentum.reserve(player, actionDef.momentumSpend());
                int fireTick = startTick + Math.max(0, actionDef.chargeTicks());
                EnchantmentSnapshot fieldSnapshot = ContactCast.snapshot(skill, segment, player);
                ChainScheduler.schedule(player.getServer(), fireTick, () -> {
                    if (!ChargeGate.same(player.getUUID(), epoch) || !player.isAlive()) {
                        WaterMomentum.release(hold);
                        return;
                    }
                    WaterMomentum.commit(player, hold);
                    int createdTick = player.getServer().getTickCount();
                    FieldSpawner.releaseFromChain(player, techniques, segment.actionIndex(), actionDef, fieldCopies);
                    if (fieldSnapshot != null) {
                        com.bslx.XueXiMod.shu.technique.release.field.FieldZones.bindBorn(player.getUUID(), fieldSnapshot, createdTick);
                    }
                    TechniqueEffect fieldEffect = TechniqueEffectRegistry.get(action.getId());
                    if (fieldEffect != null) {
                        fieldEffect.execute(player, action, weapon, new TechniqueCastContext(0f, player.getLookAngle(), player.position()));
                    }
                });
                if (conditionalChain) {
                    rememberOrdinarySuccess(ordinarySucceeded, action);
                }
                segmentIndex += 1;
                previous = segment;
                continue;
            }

            EnchantmentResolution enchantments = SkillSegmentMath.resolveSegmentEnchantments(skill, segment);
            int magicDamage = SkillSegmentMath.segmentMagicDamage(skill, segment);
            EnchantBonus bonus = EnchantBonus.resolve(skill, segment.enchantIndexes(), action.getBaseDamage(), 1);

            final boolean carries = action.getBaseDamage() > 0 && !segment.enchantIndexes().isEmpty();
            final float finalDamage = segment.enchantIndexes().isEmpty()
                    ? SkillSegmentMath.segmentDamage(action, segment, weaponBonus, magicDamage, attributeDamageBonus)
                    : bonus.carriedDamage(weaponBonus);
            if (!segment.enchantIndexes().isEmpty() && bonus.cooldownMultiplier > 0f && bonus.cooldownMultiplier < 0.999f && gap > 0) {
                int adjusted = Math.max(1, Math.round(gap * bonus.cooldownMultiplier));
                startTick += adjusted - gap;
                lastStartTick = startTick;
            }
            DebugLog.log("链段 " + action.getId() + " 魔法伤害=" + magicDamage + " 间隔=" + gap);

            final int currentTick = startTick;

            ChainSegment nextSegment = (segmentIndex + 1 < segments.size()) ? segments.get(segmentIndex + 1) : null;
            boolean chainedToTrigger = nextSegment != null
                    && SkillSegmentMath.releaseKind(action.getId()) == ReleaseKind.ACTION
                    && SkillSegmentMath.releaseKind(techniques.get(nextSegment.actionIndex()).getId()) != ReleaseKind.RULE
                    && SkillSegmentMath.isTriggerRelease(techniques.get(nextSegment.actionIndex()).getId())
                    && !SkillSegmentMath.manipulatesSwordEntities(techniques.get(nextSegment.actionIndex()).getId());

            if (chainedToTrigger) {
                Technique nextAction = techniques.get(nextSegment.actionIndex());
                EnchantmentResolution nextEnchantments = SkillSegmentMath.resolveSegmentEnchantments(skill, nextSegment);
                int nextMagicDamage = SkillSegmentMath.segmentMagicDamage(skill, nextSegment);
                EnchantBonus nextBonus = EnchantBonus.resolve(skill, nextSegment.enchantIndexes(), nextAction.getBaseDamage(), 1);
                boolean nextCarries = nextAction.getBaseDamage() > 0 && !nextSegment.enchantIndexes().isEmpty();
                float nextDamage = nextSegment.enchantIndexes().isEmpty()
                        ? SkillSegmentMath.segmentDamage(nextAction, nextSegment, weaponBonus, nextMagicDamage, attributeDamageBonus)
                        : nextBonus.carriedDamage(weaponBonus);
                DebugLog.log("链段 " + nextAction.getId() + " 由冲刺命中触发");
                Runnable runNext = () -> {
                    if (!HeldSwordGate.allows(nextAction, player.getMainHandItem())) {
                        return;
                    }
                    int nextCost = SkillSegmentMath.segmentManaCost(techniques, nextSegment);
                    if (!MagicPool.trySpend(player, nextCost)) {
                        return;
                    }
                    if (nextCost > 0) {
                        MagicPool.showBalance(player);
                    }
                    SkillSegmentRunner.buildSegmentRunner(player, nextAction, techniques, nextSegment.actionIndex(), weapon, nextDamage, nextCarries, nextEnchantments, null,
                            SkillChainPlan.lockThisCast(techniques, nextSegment.actionIndex()), SkillChainPlan.dropThisCast(techniques, segments, nextSegment.actionIndex()),
                            null, 0, 1, ReleaseBatch.current(), nextBonus, 0, castSource).run();
                };
                ChainScheduler.schedule(player.getServer(), currentTick,
                        SkillSegmentRunner.buildSegmentRunner(player, action, techniques, segment.actionIndex(), weapon, finalDamage, carries, enchantments, position -> runNext.run(),
                                SkillChainPlan.lockThisCast(techniques, segment.actionIndex()), SkillChainPlan.dropThisCast(techniques, segments, segment.actionIndex()),
                                null, 0, 1, ReleaseBatch.current(), bonus, 0, null));
                tailTechnique = action;
                segmentIndex += 2;
            } else {
                int segmentShare = SkillSegmentMath.segmentManaCost(techniques, segment);
                ReleaseDefinition segmentDefinition = ReleaseRegistry.get(action.getId());
                if (segmentDefinition != null && segmentDefinition.submitsHostGroup()) {
                    ServerLevel castLevel = player.serverLevel();
                    int castEpoch = ChargeGate.epoch(player.getUUID());
                    HostCast hostCast = HostCast.open(player.getUUID(), castSource);
                    hostCast.hold();
                    UUID hostGroup = hostCast.hostId(0, 0);
                    UncommittedShare claim = UncommittedShare.open(pressReceipt, segmentShare);
                    AtomicBoolean once = new AtomicBoolean(false);
                    AtomicBoolean released = new AtomicBoolean(false);
                    Runnable releaseCast = () -> {
                        if (released.compareAndSet(false, true)) {
                            hostCast.release();
                        }
                    };
                    try {
                        ChainScheduler.schedule(player.getServer(), currentTick, () -> {
                            if (!once.compareAndSet(false, true)) {
                                return;
                            }
                            try {
                                int nowTick = player.getServer() == null ? currentTick : player.getServer().getTickCount();
                                if (!player.isAlive() || player.level() != castLevel || !ChargeGate.same(player.getUUID(), castEpoch)
                                        || !HeldSwordGate.allows(action, player.getMainHandItem())) {
                                    HostGroupCommit.cancel(hostCast, hostGroup, nowTick, "施法已经中断");
                                    claim.cancel(player);
                                    return;
                                }
                                ReleaseOutcome outcome = SkillSegmentRunner.commitHostGroup(player, action, techniques, segment.actionIndex(), weapon,
                                        finalDamage, carries, enchantments,
                                        SkillChainPlan.lockThisCast(techniques, segment.actionIndex()),
                                        SkillChainPlan.dropThisCast(techniques, segments, segment.actionIndex()),
                                        null, 1, ReleaseBatch.current(), bonus, 0, null, null, null, null, hostCast, hostGroup, null);
                                if (outcome.committed()) {
                                    if (conditionalChain) {
                                        claim.commitRetained();
                                        rememberOrdinarySuccess(ordinarySucceeded, action);
                                    } else {
                                        claim.commit();
                                    }
                                } else {
                                    claim.cancel(player);
                                }
                            } finally {
                                releaseCast.run();
                            }
                        });
                    } catch (RuntimeException failure) {
                        releaseCast.run();
                        throw failure;
                    }
                } else {
                    Runnable keptHook = conditionalChain
                            ? () -> rememberOrdinarySuccess(ordinarySucceeded, action)
                            : null;
                    ChainScheduler.schedule(player.getServer(), currentTick, refundIfBlocked(player, action, pressReceipt, segmentShare,
                            SkillSegmentRunner.buildSegmentRunner(player, action, techniques, segment.actionIndex(), weapon, finalDamage, carries, enchantments, null,
                                    SkillChainPlan.lockThisCast(techniques, segment.actionIndex()), SkillChainPlan.dropThisCast(techniques, segments, segment.actionIndex()),
                                    null, 0, 1, ReleaseBatch.current(), bonus, 0, castSource), keptHook));
                }
                tailTechnique = action;
                segmentIndex += 1;
            }

            previous = segment;
        }

        if (stepFailed && !conditionalChain) {
            if (pressReceipt != null) {
                pressReceipt.refund(player);
            }
            return CastTiming.insufficient();
        }
        if (stepFailed) {
            conditionalAborted.set(true);
            cancelUnkept(conditionalClaims, player);
            if (pressReceipt != null && failedFromIndex >= 0) {
                int tailCost = SkillChainPlan.pressCommitCost(techniques, segments, failedFromIndex);
                if (tailCost > 0) {
                    pressReceipt.refundShare(player, tailCost);
                }
            }
        }

        capturedLastStart.set(lastStartTick);
        capturedTail.set(tailTechnique);
        if (conditionalChain) {
            if (pendingConditional.get() > 0) {
                handedOff.set(true);
                return CastTiming.rejected();
            }
            int settledReplay = settleConditionalParent(player, weapon, slot, stepped, openedTick, frozenStepCooldown,
                    lastStartTick, tailTechnique, cooldownReduction, replaySkill, techniques, frozenSignature, stepCount,
                    conditionalCommitted, succeededConditionalScaled, ordinarySucceeded, succeededConditionalIds,
                    enchantApplied, recordedUsage, parentSettled, false);
            if (settledReplay < 0) {
                return CastTiming.insufficient();
            }
            return CastTiming.accepted(openedTick, lastStartTick, settledReplay, List.of());
        }

        int replayTick = conditionalReplayTick(stepped, openedTick, stepCooldown, lastStartTick, tailTechnique, cooldownReduction);
        if (stepped) {
            ChainProgress.advance(player, weapon, slot, stepSignature, stepCount);
        }
        return CastTiming.accepted(openedTick, lastStartTick, replayTick, techniques);
        } catch (RuntimeException failure) {
            conditionalAborted.set(true);
            cancelUnkept(conditionalClaims, player);
            capturedLastStart.set(lastStartTick);
            capturedTail.set(tailTechnique);
            if (pendingConditional.get() > 0) {
                handedOff.set(true);
            } else {
                settleConditionalParent(player, weapon, slot, stepped, openedTick, frozenStepCooldown,
                        lastStartTick, tailTechnique, cooldownReduction, replaySkill, techniques, frozenSignature, stepCount,
                        conditionalCommitted, succeededConditionalScaled, ordinarySucceeded, succeededConditionalIds,
                        enchantApplied, recordedUsage, parentSettled, true);
            }
            throw failure;
        } finally {
            ReleaseBatch.unbind();
            if (conditionalCast != null) {
                conditionalCast.release();
            }
            if (!handedOff.get()) {
                releaseConditionalLock(conditionalLockKey, conditionalLockReleased);
            }
        }
    }

    private static HostCast openConditionalCast(ServerPlayer player, List<Technique> techniques,
                                                List<ChainSegment> segments, CastSource castSource) {
        for (ChainSegment listedSegment : segments) {
            Technique listedTechnique = techniques.get(listedSegment.actionIndex());
            ReleaseDefinition listedDefinition = ReleaseRegistry.get(listedTechnique.getId());
            if (listedTechnique.getBaseDamage() <= 0f && listedDefinition != null
                    && listedDefinition.conditionalEffect() != null) {
                HostCast opened = HostCast.open(player.getUUID(), castSource);
                opened.hold();
                return opened;
            }
        }
        return null;
    }

    private static int conditionalReplayTick(boolean stepped, int openedTick, int stepCooldown, int lastStartTick,
                                             Technique tailTechnique, double cooldownReduction) {
        if (stepped) {
            return openedTick + Math.max(stepCooldown, 1);
        }
        return lastStartTick + Math.max(tailCooldownOf(tailTechnique, cooldownReduction), 1);
    }

    private static int tailCooldownOf(Technique tailTechnique, double cooldownReduction) {
        if (tailTechnique == null) {
            return 0;
        }
        ReleaseDefinition tailDefinition = ReleaseRegistry.get(tailTechnique.getId());
        boolean conditionalTail = tailTechnique.getBaseDamage() <= 0f
                && tailDefinition != null && tailDefinition.conditionalEffect() != null;
        if (tailTechnique.getBaseDamage() > 0f || conditionalTail) {
            return SkillSegmentMath.scaleCooldown(tailTechnique.getBaseCooldown(), cooldownReduction);
        }
        return 0;
    }

    private static Runnable refundIfBlocked(ServerPlayer player, Technique action, ManaPaymentReceipt receipt, int share,
                                            Runnable body, Runnable keptHook) {
        ServerLevel castLevel = player.serverLevel();
        int castEpoch = ChargeGate.epoch(player.getUUID());
        UncommittedShare claim = UncommittedShare.open(receipt, share);
        AtomicBoolean once = new AtomicBoolean(false);
        return () -> {
            if (!once.compareAndSet(false, true)) {
                return;
            }
            if (!player.isAlive() || player.level() != castLevel || !ChargeGate.same(player.getUUID(), castEpoch)
                    || !HeldSwordGate.allows(action, player.getMainHandItem())) {
                claim.cancel(player);
                return;
            }
            body.run();
            if (keptHook == null) {
                claim.commit();
                return;
            }
            claim.commitRetained();
            keptHook.run();
        };
    }

    static void recordTechniqueUse(ServerPlayer player, Technique technique, Set<String> recorded) {
        if (player == null || technique == null || recorded == null || technique.getId() == null) {
            return;
        }
        if (!recorded.add(technique.getId())) {
            return;
        }
        PlayerShuData data = PlayerShuData.get(player);
        data.incrementUsage(technique.getId());
        boolean leveledUp = data.addUsageAndTryLevelUp(technique.getType(), player);
        PlayerShuData.save(player, data);
        if (leveledUp) {
            com.bslx.XueXiMod.player.DomainLearning.offerReady(player);
            com.bslx.XueXiMod.network.ModNetwork.sendToPlayer(player, new com.bslx.XueXiMod.network.ShuSyncPacket(data));
        }
    }

    private static void rememberOrdinarySuccess(List<Technique> succeeded, Technique technique) {
        if (technique == null || technique.getId() == null) {
            return;
        }
        for (Technique existing : succeeded) {
            if (technique.getId().equals(existing.getId())) {
                return;
            }
        }
        succeeded.add(technique);
    }

    private static void cancelUnkept(List<UncommittedShare> claims, ServerPlayer player) {
        for (UncommittedShare claim : claims) {
            if (!claim.kept()) {
                claim.cancel(player);
            }
        }
    }

    private static void releaseConditionalLock(String carrierKey, AtomicBoolean released) {
        if (carrierKey == null || released == null) {
            return;
        }
        if (released.compareAndSet(false, true)) {
            CastCooldown.release(carrierKey);
        }
    }

    private static boolean isZeroDamageConditional(Technique technique) {
        if (technique == null || technique.getBaseDamage() > 0f) {
            return false;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        return definition != null && definition.conditionalEffect() != null;
    }

    private static boolean chainHasConditional(List<Technique> techniques, List<ChainSegment> segments) {
        for (ChainSegment listedSegment : segments) {
            if (isZeroDamageConditional(techniques.get(listedSegment.actionIndex()))) {
                return true;
            }
        }
        return false;
    }

    private static int settleConditionalParent(ServerPlayer player, ItemStack weapon, int slot, boolean stepped,
                                               int openedTick, int frozenStepCooldown, int lastStartTick,
                                               Technique tailTechnique, double cooldownReduction, Skill replaySkill,
                                               List<Technique> techniques, String stepSignature, int stepCount,
                                               AtomicInteger conditionalCommitted, AtomicInteger succeededConditionalScaled,
                                               List<Technique> ordinarySucceeded, Set<String> succeededConditionalIds,
                                               AtomicBoolean enchantApplied, Set<String> recordedUsage,
                                               AtomicBoolean parentSettled, boolean markCooldown) {
        if (!parentSettled.compareAndSet(false, true)) {
            return -1;
        }
        boolean conditionalSuccess = conditionalCommitted.get() > 0;
        boolean ordinarySuccess = !ordinarySucceeded.isEmpty();
        if (!conditionalSuccess && !ordinarySuccess) {
            return -1;
        }
        for (Technique technique : ordinarySucceeded) {
            recordTechniqueUse(player, technique, recordedUsage);
        }
        if (enchantApplied.get()) {
            for (Technique technique : techniques) {
                if (technique.isEnchantment()) {
                    recordTechniqueUse(player, technique, recordedUsage);
                }
            }
        }
        if (conditionalSuccess) {
            for (Technique technique : techniques) {
                if (isZeroDamageConditional(technique) && succeededConditionalIds.contains(technique.getId())) {
                    recordTechniqueUse(player, technique, recordedUsage);
                }
            }
        }
        int replayTick = conditionalParentReplay(stepped, openedTick, frozenStepCooldown, lastStartTick, tailTechnique,
                cooldownReduction, replaySkill, techniques, conditionalCommitted.get(), succeededConditionalScaled.get());
        if (markCooldown) {
            CastCooldown.mark(player, weapon, slot, replayTick);
        }
        if (stepped) {
            ChainProgress.advance(player, weapon, slot, stepSignature, stepCount);
        }
        return replayTick;
    }

    private static int conditionalParentReplay(boolean stepped, int openedTick, int frozenStepCooldown, int lastStartTick,
                                               Technique tailTechnique, double cooldownReduction, Skill replaySkill,
                                               List<Technique> techniques, int conditionalCommittedCount,
                                               int succeededConditionalScaled) {
        if (stepped) {
            int cooldown = frozenStepCooldown;
            if (conditionalCommittedCount <= 0) {
                cooldown = steppedReplayCooldown(replaySkill, techniques, cooldownReduction);
            }
            return openedTick + Math.max(cooldown, 1);
        }
        int tailPart = tailCooldownOf(tailTechnique, cooldownReduction);
        if (conditionalCommittedCount <= 0 && isZeroDamageConditional(tailTechnique)) {
            tailPart = 0;
        }
        int conditionalPart = conditionalCommittedCount > 0 ? succeededConditionalScaled : 0;
        int piece = tailPart > conditionalPart ? tailPart : conditionalPart;
        return lastStartTick + Math.max(piece, 1);
    }

    private static int steppedReplayCooldown(Skill replaySkill, List<Technique> techniques, double cooldownReduction) {
        List<Integer> keptIndexes = new ArrayList<>();
        for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
            if (!isZeroDamageConditional(techniques.get(techniqueIndex))) {
                keptIndexes.add(techniqueIndex);
            }
        }
        if (keptIndexes.isEmpty()) {
            return 0;
        }
        return ChainSteps.cooldown(replaySkill, keptIndexes, cooldownReduction);
    }

    private CastTiming imprintEnchantChain(ServerPlayer player, Skill skill, List<Technique> techniques, double cooldownReduction) {
        int enchantCost = 0;
        int cooldownSum = 0;
        for (Technique technique : techniques) {
            enchantCost += technique.manaCost();
            cooldownSum += SkillSegmentMath.scaleCooldown(technique.getBaseCooldown(), cooldownReduction);
        }
        if (!MagicPool.trySpend(player, enchantCost)) {
            return CastTiming.insufficient();
        }
        if (enchantCost > 0) {
            MagicPool.showBalance(player);
        }
        SkillChainPlan.imprintEnchant(player, skill, techniques, 1);
        int nowTick = player.getServer().getTickCount();
        return CastTiming.accepted(nowTick, nowTick, nowTick + Math.max(cooldownSum, 1), techniques);
    }

    private static boolean legalEnchantChain(List<Technique> techniques) {
        if (techniques.isEmpty()) {
            return false;
        }
        for (Technique technique : techniques) {
            if (!technique.isEnchantment()) {
                return false;
            }
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            if (definition != null && definition.fieldRadius() > 0f) {
                return false;
            }
        }
        return true;
    }

    public static void register(SimpleChannel channel, int messageId) {
        channel.registerMessage(messageId,
                SkillUsePacket.class,
                SkillUsePacket::encode,
                SkillUsePacket::decode,
                SkillUsePacket::handle);
    }
}
