package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.debug.DebugLog;
import com.bslx.XueXiMod.item.SkillCarrier;
import com.bslx.XueXiMod.item.StaffData;
import com.bslx.XueXiMod.item.StaffItem;
import com.bslx.XueXiMod.item.StaffSkills;
import com.bslx.XueXiMod.player.MagicPool;
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
import com.bslx.XueXiMod.shu.technique.enchant.WeaponEnchant;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
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
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SkillUsePacket {
    private final int slotIndex;

    private static final java.util.Map<java.util.UUID, int[]> SKILL_BUSY_UNTIL = new java.util.concurrent.ConcurrentHashMap<>();

    public SkillUsePacket(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(slotIndex);
    }

    public static SkillUsePacket decode(FriendlyByteBuf buf) {
        return new SkillUsePacket(buf.readInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
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
                    if (peek == null || !allowsEmptyHandSwordCast(player, peek)) {
                        return;
                    }
                }
            }

            Skill skill = staff ? StaffSkills.asSkill(mainHand) : PlayerBoundSkills.get(player, slotIndex);
            if (skill == null) {
                player.sendSystemMessage(Component.literal("§c这个槽位还没有装配剑技"));
                return;
            }

            if (isBusy(player, slotIndex)) {
                DebugLog.log("释放中断：槽" + (slotIndex + 1) + " 链条尚未结束");
                return;
            }
            DebugLog.log("释放槽" + (slotIndex + 1) + " " + skill.getName());

            int lastStartTick = executeSkill(player, skill, mainHand, slotIndex, cooldownReduction, attributeDamageBonus);
            if (lastStartTick == -2) {
                return;
            }
            if (lastStartTick >= 0) {
                markBusy(player, slotIndex, lastStartTick + 1);
            }

            PlayerShuData data = PlayerShuData.get(player);
            boolean leveledUp = false;
            for (var t : skill.getTechniques()) {
                data.incrementUsage(t.getId());
                if (data.addUsageAndTryLevelUp(t.getType(), player)) {
                    leveledUp = true;
                }
            }
            PlayerShuData.save(player, data);

            if (leveledUp) {
                com.bslx.XueXiMod.network.ModNetwork.sendToPlayer(
                    player, new com.bslx.XueXiMod.network.ShuSyncPacket(data));
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private boolean isBusy(ServerPlayer player, int slot) {
        int[] busy = SKILL_BUSY_UNTIL.get(player.getUUID());
        if (busy == null) return false;
        if (slot < 0 || slot >= busy.length) return false;
        return player.getServer().getTickCount() < busy[slot];
    }

    private void markBusy(ServerPlayer player, int slot, int untilTick) {
        if (slot < 0 || slot >= 4) return;
        int[] busy = SKILL_BUSY_UNTIL.computeIfAbsent(player.getUUID(), k -> new int[4]);
        if (untilTick > busy[slot]) {
            busy[slot] = untilTick;
        }
    }

    private int executeSkill(ServerPlayer player, Skill skill, ItemStack weapon, int slot,
                             double cooldownReduction, Double attributeDamageBonus) {
        double weaponDamage = player.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
        float weaponBonus = (float) Math.max(0, weaponDamage - 1.0);

        List<Technique> techniques = skill.getTechniques();
        List<ChainSegment> segments = TechniqueChain.split(skill);

        boolean hasDamageTechnique = false;
        for (ChainSegment segment : segments) {
            if (techniques.get(segment.actionIndex()).getBaseDamage() > 0) {
                hasDamageTechnique = true;
                break;
            }
        }
        boolean hasSwordManip = hasSwordManipulation(techniques);
        if (!hasDamageTechnique && !hasSwordManip) {
            player.sendSystemMessage(Component.literal("§c当前剑技没有可释放的伤害术式"));
            return -1;
        }
        if (hasSwordManip) {
            boolean holdingSword = weapon.getItem() instanceof SwordItem;
            boolean worldSword = hasPlantedWorldSword(player);
            if (!holdingSword && !worldSword) {
                player.sendSystemMessage(Component.literal("§c需要手持剑"));
                return -1;
            }
        }

        int cost = pressCommitCost(techniques, segments);
        if (!MagicPool.trySpend(player, cost)) {
            return -2;
        }
        if (cost > 0) {
            MagicPool.showBalance(player);
        }

        ReleaseBatch.open(player, skill);
        try {
        int startTick = player.getServer().getTickCount();
        int lastStartTick = startTick;
        ChainSegment previous = null;
        boolean removedRuleWarned = false;

        int i = 0;
        while (i < segments.size()) {
            ChainSegment segment = segments.get(i);
            Technique action = techniques.get(segment.actionIndex());

            int gap = 0;
            if (previous != null) {
                gap = scaleCooldown(TechniqueChain.gapBefore(segment, previous, skill), cooldownReduction);
                startTick += gap;
            }
            lastStartTick = startTick;

            if (isRemovedRule(action.getId())) {
                if (!removedRuleWarned) {
                    removedRuleWarned = true;
                    player.sendSystemMessage(Component.literal("§c链里有已移除的术式，请重新装配"));
                }
                i += 1;
                previous = segment;
                continue;
            }

            ChainRole role = roleOf(action);
            ReleaseDefinition actionDef = ReleaseRegistry.get(action.getId());
            if (actionDef != null && actionDef.manipulatesSwordEntities()) {
                ChainScheduler.schedule(player.getServer(), startTick,
                        buildSegmentRunner(player, action, techniques, segment.actionIndex(), weapon, 0f, false,
                                EnchantmentResolution.empty(), null, false, false));
                i += 1;
                previous = segment;
                continue;
            }
            if (role == ChainRole.ACTION) {
                ModifierPlan plan = collectModifiers(segments, techniques, i + 1);
                if (plan.attackSegment >= 0) {
                    ModifierPlan captured = plan;
                    ChainScheduler.schedule(player.getServer(), startTick,
                            buildSegmentRunner(player, action, techniques, segment.actionIndex(), weapon, 0f, false,
                                    EnchantmentResolution.empty(), pos -> firePlan(player, skill, techniques, segments, captured, weaponBonus),
                                    false, false));
                    i = plan.resume;
                    previous = segment;
                    continue;
                }
            }
            if (isChainModifier(role)) {
                ModifierPlan plan = collectModifiers(segments, techniques, i);
                if (plan.attackSegment < 0) {
                    imprintEnchant(player, skill, techniques, plan.copies);
                    i = Math.max(plan.resume, i + 1);
                    previous = segment;
                    continue;
                }
                firePlan(player, skill, techniques, segments, plan, weaponBonus);
                i = plan.resume;
                previous = segments.get(plan.attackSegment);
                continue;
            }

            EnchantmentResolution enchantments = resolveSegmentEnchantments(skill, segment);
            int magicDamage = segmentMagicDamage(skill, segment);
            EnchantBonus bonus = EnchantBonus.resolve(skill, segment.enchantIndexes(), action.getBaseDamage(), 1);

            final boolean carries = action.getBaseDamage() > 0 && !segment.enchantIndexes().isEmpty();
            final float finalDamage = segment.enchantIndexes().isEmpty()
                    ? segmentDamage(action, segment, weaponBonus, magicDamage, attributeDamageBonus)
                    : bonus.attackDamage + bonus.addedDamage + weaponBonus;
            if (!segment.enchantIndexes().isEmpty() && bonus.cooldownMultiplier > 0f && bonus.cooldownMultiplier < 0.999f && gap > 0) {
                int adjusted = Math.max(1, Math.round(gap * bonus.cooldownMultiplier));
                startTick += adjusted - gap;
                lastStartTick = startTick;
            }
            DebugLog.log("链段 " + action.getId() + " 魔法伤害=" + magicDamage + " 间隔=" + gap);

            final int currentTick = startTick;

            ChainSegment nextSegment = (i + 1 < segments.size()) ? segments.get(i + 1) : null;
            boolean chainedToTrigger = nextSegment != null
                    && releaseKind(action.getId()) == ReleaseKind.ACTION
                    && releaseKind(techniques.get(nextSegment.actionIndex()).getId()) != ReleaseKind.RULE
                    && isTriggerRelease(techniques.get(nextSegment.actionIndex()).getId())
                    && !manipulatesSwordEntities(techniques.get(nextSegment.actionIndex()).getId());

            if (chainedToTrigger) {
                Technique nextAction = techniques.get(nextSegment.actionIndex());
                EnchantmentResolution nextEnchantments = resolveSegmentEnchantments(skill, nextSegment);
                int nextMagicDamage = segmentMagicDamage(skill, nextSegment);
                boolean nextCarries = nextAction.getBaseDamage() > 0 && !nextSegment.enchantIndexes().isEmpty();
                float nextDamage = segmentDamage(nextAction, nextSegment, weaponBonus, nextMagicDamage, attributeDamageBonus);
                DebugLog.log("链段 " + nextAction.getId() + " 由冲刺命中触发");
                Runnable runNext = () -> {
                    int nextCost = segmentManaCost(techniques, nextSegment);
                    if (!MagicPool.trySpend(player, nextCost)) {
                        return;
                    }
                    if (nextCost > 0) {
                        MagicPool.showBalance(player);
                    }
                    buildSegmentRunner(player, nextAction, techniques, nextSegment.actionIndex(), weapon, nextDamage, nextCarries, nextEnchantments, null,
                            lockThisCast(techniques, nextSegment.actionIndex()), dropThisCast(techniques, segments, nextSegment.actionIndex())).run();
                };
                ChainScheduler.schedule(player.getServer(), currentTick,
                        buildSegmentRunner(player, action, techniques, segment.actionIndex(), weapon, finalDamage, carries, enchantments, pos -> runNext.run(),
                                lockThisCast(techniques, segment.actionIndex()), dropThisCast(techniques, segments, segment.actionIndex())));
                i += 2;
            } else {
                ChainScheduler.schedule(player.getServer(), currentTick,
                        buildSegmentRunner(player, action, techniques, segment.actionIndex(), weapon, finalDamage, carries, enchantments, null,
                                lockThisCast(techniques, segment.actionIndex()), dropThisCast(techniques, segments, segment.actionIndex()),
                                null, 0, 1, ReleaseBatch.current(), bonus, 0));
                i += 1;
            }

            previous = segment;
        }

        return lastStartTick;
        } finally {
            ReleaseBatch.unbind();
        }
    }

    private int pressCommitCost(List<Technique> techniques, List<ChainSegment> segments) {
        int cost = 0;
        int i = 0;
        while (i < segments.size()) {
            ChainSegment segment = segments.get(i);
            Technique action = techniques.get(segment.actionIndex());

            if (isRemovedRule(action.getId())) {
                i += 1;
                continue;
            }
            ChainRole role = roleOf(action);
            if (role == ChainRole.ACTION) {
                ModifierPlan plan = collectModifiers(segments, techniques, i + 1);
                cost += segmentManaCost(techniques, segment);
                if (plan.attackSegment >= 0) {
                    cost += segmentManaCost(techniques, segments.get(plan.attackSegment));
                    i = plan.resume;
                } else {
                    i += 1;
                }
                continue;
            }
            if (isChainModifier(role)) {
                ModifierPlan plan = collectModifiers(segments, techniques, i);
                if (plan.attackSegment >= 0) {
                    cost += segmentManaCost(techniques, segments.get(plan.attackSegment));
                }
                i = Math.max(plan.resume, i + 1);
                continue;
            }

            ChainSegment nextSegment = (i + 1 < segments.size()) ? segments.get(i + 1) : null;
            boolean chainedToTrigger = nextSegment != null
                    && releaseKind(action.getId()) == ReleaseKind.ACTION
                    && releaseKind(techniques.get(nextSegment.actionIndex()).getId()) != ReleaseKind.RULE
                    && isTriggerRelease(techniques.get(nextSegment.actionIndex()).getId())
                    && !manipulatesSwordEntities(techniques.get(nextSegment.actionIndex()).getId());

            if (chainedToTrigger) {
                cost += segmentManaCost(techniques, segment);
                i += 2;
                continue;
            }

            cost += segmentManaCost(techniques, segment);
            i += 1;
        }
        return cost;
    }

    private Runnable buildSegmentRunner(ServerPlayer player, Technique action, List<Technique> techniques,
                                        int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                        EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                        boolean lockThisCast, boolean dropThisCast) {
        return buildSegmentRunner(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                onFirstTrigger, lockThisCast, dropThisCast, null, 0, 1, ReleaseBatch.current(), EnchantBonus.NONE, 0);
    }

    private Runnable buildSegmentRunner(ServerPlayer player, Technique action, List<Technique> techniques,
                                        int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                        EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                        boolean lockThisCast, boolean dropThisCast, ReleaseBatch batch) {
        return buildSegmentRunner(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                onFirstTrigger, lockThisCast, dropThisCast, null, 0, 1, batch, EnchantBonus.NONE, 0);
    }

    private Runnable buildSegmentRunner(ServerPlayer player, Technique action, List<Technique> techniques,
                                        int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                        EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                        boolean lockThisCast, boolean dropThisCast,
                                        ReleaseBatch.Tally tally, int copyIndex, int copyCount, ReleaseBatch batch,
                                        EnchantBonus bonus, int volley) {
        return () -> {
            if (!player.isAlive()) {
                return;
            }
            Vec3 lookVec = player.getLookAngle();
            Vec3 pos = player.position();
            if (action.getType() == TechniqueType.SWORD) {
                player.swing(InteractionHand.MAIN_HAND, true);
            }
            runSegment(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                    onFirstTrigger, lockThisCast, dropThisCast, tally, copyIndex, copyCount, batch, lookVec, pos,
                    bonus, volley);
        };
    }

    private Runnable buildSegmentRunnerAt(ServerPlayer player, Technique action, List<Technique> techniques,
                                          int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                          EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                          Vec3 lookVec, Vec3 pos, boolean lockThisCast, boolean dropThisCast) {
        return buildSegmentRunnerAt(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                onFirstTrigger, lookVec, pos, lockThisCast, dropThisCast, null, 0, 1, ReleaseBatch.current(), EnchantBonus.NONE, 0);
    }

    private Runnable buildSegmentRunnerAt(ServerPlayer player, Technique action, List<Technique> techniques,
                                          int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                          EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                          Vec3 lookVec, Vec3 pos, boolean lockThisCast, boolean dropThisCast,
                                          ReleaseBatch batch) {
        return buildSegmentRunnerAt(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                onFirstTrigger, lookVec, pos, lockThisCast, dropThisCast, null, 0, 1, batch, EnchantBonus.NONE, 0);
    }

    private Runnable buildSegmentRunnerAt(ServerPlayer player, Technique action, List<Technique> techniques,
                                          int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                          EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                          Vec3 lookVec, Vec3 pos, boolean lockThisCast, boolean dropThisCast,
                                          ReleaseBatch.Tally tally, int copyIndex, int copyCount, ReleaseBatch batch,
                                          EnchantBonus bonus, int volley) {
        return () -> {
            if (!player.isAlive()) {
                return;
            }
            if (action.getType() == TechniqueType.SWORD) {
                player.swing(InteractionHand.MAIN_HAND, true);
            }
            runSegment(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                    onFirstTrigger, lockThisCast, dropThisCast, tally, copyIndex, copyCount, batch, lookVec, pos,
                    bonus, volley);
        };
    }

    private void runSegment(ServerPlayer player, Technique action, List<Technique> techniques, int actionIndex,
                            ItemStack weapon, float finalDamage, boolean carries,
                            EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                            boolean lockThisCast, boolean dropThisCast,
                            ReleaseBatch.Tally tally, int copyIndex, int copyCount,
                            ReleaseBatch batch, Vec3 lookVec, Vec3 pos,
                            EnchantBonus bonus, int volley) {
        TechniqueEffect effect = TechniqueEffectRegistry.get(action.getId());
        if (effect == null) {
            return;
        }
        ReleaseBatch outer = ReleaseBatch.current();
        if (batch != null) {
            ReleaseBatch.bind(batch);
        }
        try {
            EnchantmentResolution applied = carries ? enchantments : EnchantmentResolution.empty();
            boolean spaceLock = false;
            boolean entityLock = false;
            boolean scatter = false;
            boolean drop = false;
            for (int j = actionIndex - 1; j >= 0; j--) {
                Technique earlier = techniques.get(j);
                if (earlier.getBaseDamage() > 0 && !earlier.isEnchantment()) {
                    break;
                }
                switch (roleOf(earlier)) {
                    case SEEK_SPACE -> spaceLock = true;
                    case SEEK_ENTITY -> entityLock = true;
                    case TRAJECTORY_SCATTER -> scatter = true;
                    case TRAJECTORY_DROP -> drop = true;
                    default -> {
                    }
                }
            }
            if (entityLock) {
                LivingEntity locked = LockTargetService.resolve(player);
                if (locked != null) {
                    locked.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                }
            }
            ReleaseDefinition attackDefinition = ReleaseRegistry.get(action.getId());
            boolean mutable = attackDefinition != null && attackDefinition.trajectoryMutable();
            boolean frameLock = entityLock && mutable ? true : lockThisCast;
            if (spaceLock) {
                frameLock = entityLock && mutable;
            }
            Vec3 center = spaceLock && batch != null
                    ? batch.resolveAnchor(player)
                    : player.getEyePosition();
            long seed = player.getUUID().getLeastSignificantBits()
                    ^ player.level().getGameTime()
                    ^ action.getId().hashCode()
                    ^ ((long) volley << 16);
            CastFrame frame = CastFrame.forAttack(finalDamage, applied, tally, copyIndex, copyCount,
                    center, lookVec, spaceLock, scatter, drop, mutable, seed);
            frame.radiusMultiplier = bonus.radiusMultiplier;
            frame.burnSeconds = bonus.burnSeconds;
            frame.controlTicks = bonus.controlTicks;
            frame.extinguish = bonus.extinguish;
            frame.knockback = bonus.knockback;
            frame.speedMultiplier = bonus.speedMultiplier;
            CastFrame.bind(frame);
            try {
                TechniqueCastContext ctx = new TechniqueCastContext(finalDamage, lookVec, pos, applied,
                        onFirstTrigger, frameLock, dropThisCast);
                effect.execute(player, action, weapon, ctx);
            } finally {
                CastFrame.unbind();
            }
        } finally {
            if (batch != null) {
                ReleaseBatch.bind(outer);
            }
        }
    }


    /** 装配链是否含「操纵世界剑实体」类术式（由 ReleaseDefinition 登记，不写死 id）。 */
    private static boolean hasSwordManipulation(List<Technique> techniques) {
        for (Technique technique : techniques) {
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            if (definition != null && definition.manipulatesSwordEntities()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasPlantedWorldSword(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        return SwordEntity.anyOwnedBy(level, player.getUUID());
    }

    /** 空手可放：链上有操纵类术式，且维度里存在该玩家拥有的存活剑实体。 */
    private static boolean allowsEmptyHandSwordCast(ServerPlayer player, Skill skill) {
        return skill != null
                && hasSwordManipulation(skill.getTechniques())
                && hasPlantedWorldSword(player);
    }

    private ChainRole roleOf(Technique technique) {
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition == null) {
            return ChainRole.ATTACK;
        }
        return definition.role();
    }

    private boolean isChainModifier(ChainRole role) {
        return role == ChainRole.SEEK_SPACE
                || role == ChainRole.SEEK_ENTITY
                || role == ChainRole.TRAJECTORY_DROP
                || role == ChainRole.TRAJECTORY_SCATTER
                || role == ChainRole.RULE_TRIPLE
                || role == ChainRole.RULE_DUAL;
    }

    private ModifierPlan collectModifiers(List<ChainSegment> segments, List<Technique> techniques, int from) {
        ModifierPlan plan = new ModifierPlan();
        int j = from;
        while (j < segments.size()) {
            Technique technique = techniques.get(segments.get(j).actionIndex());
            if (isRemovedRule(technique.getId())) {
                j++;
                continue;
            }
            ChainRole role = roleOf(technique);
            if (technique.getBaseDamage() > 0 && !technique.isEnchantment()) {
                plan.attackSegment = j;
                j++;
                break;
            }
            if (!isChainModifier(role)) {
                break;
            }
            switch (role) {
                case SEEK_SPACE -> plan.spaceLock = true;
                case SEEK_ENTITY -> plan.entityLock = true;
                case TRAJECTORY_DROP -> plan.drop = true;
                case TRAJECTORY_SCATTER -> plan.scatter = true;
                case RULE_TRIPLE -> plan.copies *= 3;
                case RULE_DUAL -> plan.volleys *= 2;
                default -> {
                }
            }
            j++;
        }
        plan.resume = j;
        return plan;
    }

    private void firePlan(ServerPlayer player, Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                          ModifierPlan plan, float weaponBonus) {
        ChainSegment attackSeg = segments.get(plan.attackSegment);
        Technique attack = techniques.get(attackSeg.actionIndex());
        EnchantmentResolution enchantments = resolveSegmentEnchantments(skill, attackSeg);
        EnchantBonus bonus = EnchantBonus.resolve(skill, attackSeg.enchantIndexes(), attack.getBaseDamage(), 1);
        float damage = attackSeg.enchantIndexes().isEmpty()
                ? segmentDamage(attack, attackSeg, weaponBonus, segmentMagicDamage(skill, attackSeg), null)
                : bonus.attackDamage + bonus.addedDamage + weaponBonus;
        boolean carries = !attackSeg.enchantIndexes().isEmpty();
        ReleaseDefinition definition = ReleaseRegistry.get(attack.getId());
        boolean mutable = definition != null && definition.trajectoryMutable();
        ReleaseBatch batch = ReleaseBatch.current();
        int now = player.getServer().getTickCount();
        ItemStack weapon = player.getMainHandItem();
        for (int volley = 0; volley < plan.volleys; volley++) {
            ReleaseBatch.Tally tally = plan.copies > 1 && batch != null ? batch.openGroupTally() : null;
            int tick = now + volley * 10;
            for (int copy = 0; copy < plan.copies; copy++) {
                int copyIndex = copy;
                int volleyIndex = volley;
                Runnable runner = buildSegmentRunner(player, attack, techniques, attackSeg.actionIndex(), weapon,
                        damage, carries, enchantments, null,
                        plan.entityLock && mutable, plan.drop && mutable,
                        tally, copyIndex, plan.copies, batch,
                        bonus, volleyIndex);
                ChainScheduler.schedule(player.getServer(), tick, runner);
            }
        }
    }

    private void imprintEnchant(ServerPlayer player, Skill skill, List<Technique> techniques, int layers) {
        java.util.ArrayList<Integer> indexes = new java.util.ArrayList<>();
        for (int i = 0; i < techniques.size(); i++) {
            if (techniques.get(i).isEnchantment()) {
                indexes.add(i);
            }
        }
        if (indexes.isEmpty()) {
            return;
        }
        float weaponAttack = (float) player.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
        EnchantBonus bonus = EnchantBonus.resolve(skill, indexes, weaponAttack, Math.max(1, layers));
        WeaponEnchant.imprint(player, player.getMainHandItem(), bonus);
    }

    private static final class ModifierPlan {
        boolean spaceLock;
        boolean entityLock;
        boolean drop;
        boolean scatter;
        int copies = 1;
        int volleys = 1;
        int attackSegment = -1;
        int resume;
    }

    private boolean isRemovedRule(String id) {
        return "rule_interleave".equals(id) || "rule_damage_tally".equals(id);
    }

    private int findTripleAttack(List<ChainSegment> segments, int ruleIndex, List<Technique> techniques) {
        for (int j = ruleIndex + 1; j < segments.size(); j++) {
            ChainSegment seg = segments.get(j);
            Technique action = techniques.get(seg.actionIndex());
            if (releaseKind(action.getId()) == ReleaseKind.RULE) {
                DebugLog.log("rule_triple_cast 先遇到其它规则术式，停止");
                return -1;
            }
            if (action.getBaseDamage() > 0 && !action.isEnchantment()) {
                return j;
            }
        }
        return -1;
    }

    private boolean lockThisCast(List<Technique> techniques, int actionIndex) {
        ReleaseDefinition definition = ReleaseRegistry.get(techniques.get(actionIndex).getId());
        if (definition == null || !definition.acceptsTargetOverride()) {
            return false;
        }
        for (int i = actionIndex - 1; i >= 0; i--) {
            ReleaseDefinition earlier = ReleaseRegistry.get(techniques.get(i).getId());
            if (earlier == null) {
                continue;
            }
            if (earlier.setsTargetForNextDirectional()) {
                return true;
            }
            if (earlier.acceptsTargetOverride()) {
                return false;
            }
        }
        return false;
    }

    private boolean dropThisCast(List<Technique> techniques, List<ChainSegment> segments, int actionIndex) {
        ReleaseDefinition definition = ReleaseRegistry.get(techniques.get(actionIndex).getId());
        if (definition == null || !definition.acceptsPathOverride()) {
            return false;
        }
        List<Integer> sameRelease = sameReleaseGroup(segments, actionIndex, techniques);
        for (int i = actionIndex - 1; i >= 0; i--) {
            ReleaseDefinition earlier = ReleaseRegistry.get(techniques.get(i).getId());
            if (earlier == null) {
                continue;
            }
            if (earlier.setsDropForNextDirectional()) {
                return true;
            }
            if (earlier.acceptsPathOverride()) {
                if (sameRelease != null && sameRelease.contains(i)) {
                    continue;
                }
                return false;
            }
        }
        return false;
    }

    private List<Integer> sameReleaseGroup(List<ChainSegment> segments, int actionIndex, List<Technique> techniques) {
        for (int ruleIndex = 0; ruleIndex < segments.size(); ruleIndex++) {
            ChainSegment ruleSegment = segments.get(ruleIndex);
            if (releaseKind(techniques.get(ruleSegment.actionIndex()).getId()) != ReleaseKind.RULE) {
                continue;
            }
            List<ChainSegment> released = releasedSegments(segments, ruleIndex, techniques);
            boolean found = false;
            for (ChainSegment seg : released) {
                if (seg.actionIndex() == actionIndex) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                continue;
            }
            List<Integer> group = new ArrayList<>();
            for (ChainSegment seg : released) {
                group.add(seg.actionIndex());
            }
            return group;
        }
        return null;
    }

    private Consumer<Vec3> buildRuleTrigger(ServerPlayer player, Skill skill,
                                            List<Technique> techniques, ItemStack weapon,
                                            List<ChainSegment> segments, int ruleIndex, float weaponBonus, int slot,
                                            double cooldownReduction, Double attributeDamageBonus) {
        ReleaseBatch pressed = ReleaseBatch.current();
        List<ChainSegment> released = releasedSegments(segments, ruleIndex, techniques);
        if (released.isEmpty()) {
            return pos -> {};
        }
        ChainSegment aSegment = released.get(0);
        List<ChainSegment> follow = released.subList(1, released.size());

        Technique aAction = techniques.get(aSegment.actionIndex());
        EnchantmentResolution aEnchantments = resolveSegmentEnchantments(skill, aSegment);
        int aMagicDamage = segmentMagicDamage(skill, aSegment);
        boolean aCarries = aAction.getBaseDamage() > 0 && !aSegment.enchantIndexes().isEmpty();
        float aDamage = segmentDamage(aAction, aSegment, weaponBonus, aMagicDamage, attributeDamageBonus);
        int totalCooldown = scaleCooldown(baseCooldownSum(released, techniques), cooldownReduction);

        if (releaseKind(aAction.getId()) == ReleaseKind.ACTION) {
            Consumer<Vec3> followTrigger = buildFollowTrigger(player, skill, techniques, weapon, segments, follow, weaponBonus, slot, aAction, cooldownReduction, attributeDamageBonus);
            Runnable aRunner = buildSegmentRunner(player, aAction, techniques, aSegment.actionIndex(), weapon, aDamage, aCarries, aEnchantments, followTrigger,
                    lockThisCast(techniques, aSegment.actionIndex()), dropThisCast(techniques, segments, aSegment.actionIndex()));
            return parentPos -> ChainScheduler.schedule(player.getServer(), player.getServer().getTickCount(), aRunner);
        }

        return parentPos -> {
            Vec3 parent = parentPos != null ? parentPos : player.position();
            buildSegmentRunnerAt(player, aAction, techniques, aSegment.actionIndex(), weapon, aDamage, aCarries, aEnchantments, null,
                    player.getLookAngle(), parent, lockThisCast(techniques, aSegment.actionIndex()), dropThisCast(techniques, segments, aSegment.actionIndex()), pressed).run();

            int lastTriggerIndex = -1;
            for (int k = 0; k < follow.size(); k++) {
                if (isTriggerRelease(techniques.get(follow.get(k).actionIndex()).getId())) {
                    lastTriggerIndex = k;
                }
            }

            int tick = player.getServer().getTickCount();
            for (int k = 0; k < follow.size(); k++) {
                ChainSegment seg = follow.get(k);
                Technique tech = techniques.get(seg.actionIndex());
                EnchantmentResolution enchantments = resolveSegmentEnchantments(skill, seg);
                int magicDamage = segmentMagicDamage(skill, seg);
                boolean carries = tech.getBaseDamage() > 0 && !seg.enchantIndexes().isEmpty();
                float damage = segmentDamage(tech, seg, weaponBonus, magicDamage, attributeDamageBonus);

                Runnable runner;
                if (k == lastTriggerIndex) {
                    runner = () -> {
                        Vec3 look = player.getLookAngle();
                        Vec3 pos = player.position();
                        buildSegmentRunnerAt(player, tech, techniques, seg.actionIndex(), weapon, damage, carries, enchantments, null, look, pos,
                                lockThisCast(techniques, seg.actionIndex()), dropThisCast(techniques, segments, seg.actionIndex()), pressed).run();
                    };
                } else {
                    runner = () -> {
                        Vec3 look = player.getLookAngle();
                        buildSegmentRunnerAt(player, tech, techniques, seg.actionIndex(), weapon, damage, carries, enchantments, null, look, parent,
                                lockThisCast(techniques, seg.actionIndex()), dropThisCast(techniques, segments, seg.actionIndex()), pressed).run();
                    };
                }
                ChainScheduler.schedule(player.getServer(), tick, runner);
            }

            markBusy(player, slot, tick + totalCooldown);
        };
    }

    private Consumer<Vec3> buildFollowTrigger(ServerPlayer player, Skill skill,
                                              List<Technique> techniques, ItemStack weapon,
                                              List<ChainSegment> segments, List<ChainSegment> follow,
                                              float weaponBonus, int slot, Technique action,
                                              double cooldownReduction, Double attributeDamageBonus) {
        ReleaseBatch pressed = ReleaseBatch.current();
        List<ChainSegment> triggerSegments = new ArrayList<>();
        List<Technique> triggerActions = new ArrayList<>();
        List<Float> triggerDamages = new ArrayList<>();
        List<Boolean> triggerCarries = new ArrayList<>();
        List<EnchantmentResolution> triggerEnchantments = new ArrayList<>();

        List<ChainSegment> sustainedSegments = new ArrayList<>();
        List<Technique> sustainedActions = new ArrayList<>();
        List<Float> sustainedDamages = new ArrayList<>();
        List<Boolean> sustainedCarries = new ArrayList<>();
        List<EnchantmentResolution> sustainedEnchantments = new ArrayList<>();

        for (ChainSegment seg : follow) {
            Technique t = techniques.get(seg.actionIndex());
            ReleaseDefinition definition = ReleaseRegistry.get(t.getId());
            if (definition == null) {
                continue;
            }
            EnchantmentResolution enchantments = resolveSegmentEnchantments(skill, seg);
            int magicDamage = segmentMagicDamage(skill, seg);
            boolean carries = t.getBaseDamage() > 0 && !seg.enchantIndexes().isEmpty();
            float damage = segmentDamage(t, seg, weaponBonus, magicDamage, attributeDamageBonus);
            if (definition.tag() == ReleaseTag.TRIGGER) {
                triggerSegments.add(seg);
                triggerActions.add(t);
                triggerDamages.add(damage);
                triggerCarries.add(carries);
                triggerEnchantments.add(enchantments);
            } else if (definition.tag() == ReleaseTag.SUSTAINED) {
                sustainedSegments.add(seg);
                sustainedActions.add(t);
                sustainedDamages.add(damage);
                sustainedCarries.add(carries);
                sustainedEnchantments.add(enchantments);
            }
        }

        int triggerManaCostSum = 0;
        for (ChainSegment seg : triggerSegments) {
            triggerManaCostSum += segmentManaCost(techniques, seg);
        }
        int sustainedManaCostSum = 0;
        for (ChainSegment seg : sustainedSegments) {
            sustainedManaCostSum += segmentManaCost(techniques, seg);
        }
        final int triggerMana = triggerManaCostSum;
        final int sustainedMana = sustainedManaCostSum;

        int triggerCooldown = scaleCooldown(baseCooldownSum(triggerSegments, techniques), cooldownReduction);
        int sustainedCooldown = baseCooldownSum(sustainedSegments, techniques);
        int interval = scaleCooldown(Math.max(1, action.getBaseCooldown() + sustainedCooldown), cooldownReduction);
        ReleaseDefinition actionDefinition = ReleaseRegistry.get(action.getId());
        int lifetime = actionDefinition != null ? actionDefinition.lifetimeTicks() : 10;

        return pos -> {
            int tick = player.getServer().getTickCount();

            int firstCost = triggerMana + sustainedMana;
            boolean firstPaid = MagicPool.trySpend(player, firstCost);
            if (firstPaid && firstCost > 0) {
                MagicPool.showBalance(player);
            }

            if (firstPaid && !triggerSegments.isEmpty()) {
                Vec3 hitPos = pos != null ? pos : player.position();
                Vec3 look = player.getLookAngle();
                for (int k = 0; k < triggerSegments.size(); k++) {
                    Technique t = triggerActions.get(k);
                    float damage = triggerDamages.get(k);
                    boolean carries = triggerCarries.get(k);
                    EnchantmentResolution enchantments = triggerEnchantments.get(k);
                    final int index = k;
                    Runnable runner = () -> buildSegmentRunnerAt(player, t, techniques, triggerSegments.get(index).actionIndex(), weapon, damage, carries, enchantments, null,
                            look, hitPos,
                            lockThisCast(techniques, triggerSegments.get(index).actionIndex()),
                            dropThisCast(techniques, segments, triggerSegments.get(index).actionIndex()), pressed).run();
                    if (k == 0) {
                        runner.run();
                    } else {
                        ChainScheduler.schedule(player.getServer(), tick, runner);
                    }
                }
                markBusy(player, slot, tick + triggerCooldown);
            }

            if (!sustainedActions.isEmpty()) {
                Runnable round = () -> {
                    for (int k = 0; k < sustainedActions.size(); k++) {
                        buildSegmentRunner(player, sustainedActions.get(k), techniques, sustainedSegments.get(k).actionIndex(), weapon,
                                sustainedDamages.get(k), sustainedCarries.get(k),
                                sustainedEnchantments.get(k), null,
                                lockThisCast(techniques, sustainedSegments.get(k).actionIndex()), dropThisCast(techniques, segments, sustainedSegments.get(k).actionIndex()), pressed).run();
                    }
                };
                if (firstPaid) {
                    round.run();
                }
                int elapsed = interval;
                while (elapsed < lifetime) {
                    int delay = elapsed;
                    ChainScheduler.schedule(player.getServer(), tick + delay, () -> {
                        int pulseCost = sustainedMana;
                        if (!MagicPool.trySpend(player, pulseCost)) {
                            return;
                        }
                        if (pulseCost > 0) {
                            MagicPool.showBalance(player);
                        }
                        round.run();
                    });
                    elapsed += interval;
                }
            }
        };
    }

    private int ruleConsumedCount(List<ChainSegment> segments, int ruleIndex, List<Technique> techniques) {
        int count = 1;
        if (ruleIndex + 1 >= segments.size()) {
            return count;
        }
        count += 1;
        ReleaseDefinition rule = ReleaseRegistry.get(techniques.get(segments.get(ruleIndex).actionIndex()).getId());
        int pullAfterA = rule != null ? rule.pullAfterA() : 0;
        count += pulledCount(pullAfterA, segments, ruleIndex);
        return count;
    }

    private int pulledCount(int pullAfterA, List<ChainSegment> segments, int ruleIndex) {
        int available = Math.max(0, segments.size() - (ruleIndex + 2));
        return pullAfterA < 0 ? available : Math.min(pullAfterA, available);
    }

    private List<ChainSegment> releasedSegments(List<ChainSegment> segments, int ruleIndex, List<Technique> techniques) {
        List<ChainSegment> released = new ArrayList<>();
        if (ruleIndex + 1 >= segments.size()) {
            return released;
        }
        released.add(segments.get(ruleIndex + 1));
        ReleaseDefinition rule = ReleaseRegistry.get(techniques.get(segments.get(ruleIndex).actionIndex()).getId());
        int pullAfterA = rule != null ? rule.pullAfterA() : 0;
        int followCount = pulledCount(pullAfterA, segments, ruleIndex);
        int aIndex = ruleIndex + 1;
        for (int f = aIndex + 1; f < aIndex + 1 + followCount && f < segments.size(); f++) {
            released.add(segments.get(f));
        }
        return released;
    }

    private int baseCooldownSum(List<ChainSegment> segments, List<Technique> techniques) {
        int sum = 0;
        for (ChainSegment segment : segments) {
            sum += techniques.get(segment.actionIndex()).getBaseCooldown();
        }
        return sum;
    }

    private float segmentDamage(Technique action, ChainSegment segment, float weaponBonus, int magicDamage, Double attributeDamageBonus) {
        float base = scaledBaseDamage(action.getBaseDamage(), attributeDamageBonus);
        return action.getBaseDamage() > 0
                ? base + weaponBonus + magicDamage
                : base + weaponBonus;
    }

    private float scaledBaseDamage(float baseDamage, Double attributeDamageBonus) {
        if (attributeDamageBonus == null || baseDamage <= 0) return baseDamage;
        return (float) Math.max(0, baseDamage * (1 + attributeDamageBonus));
    }

    private int scaleCooldown(int ticks, double cooldownReduction) {
        if (ticks <= 0) return ticks;
        return Math.max(1, (int) Math.round(ticks * (1 - cooldownReduction)));
    }

    private ReleaseKind releaseKind(String techniqueId) {
        ReleaseDefinition definition = ReleaseRegistry.get(techniqueId);
        return definition != null ? definition.kind() : null;
    }

    private boolean isTriggerRelease(String techniqueId) {
        ReleaseDefinition definition = ReleaseRegistry.get(techniqueId);
        return definition != null && definition.tag() == ReleaseTag.TRIGGER;
    }

    private boolean manipulatesSwordEntities(String techniqueId) {
        ReleaseDefinition definition = ReleaseRegistry.get(techniqueId);
        return definition != null && definition.manipulatesSwordEntities();
    }

    private EnchantmentResolution resolveSegmentEnchantments(Skill skill, ChainSegment segment) {
        EnchantmentResolution resolution = EnchantmentResolution.empty();
        List<Technique> techniques = skill.getTechniques();
        for (int index : segment.enchantIndexes()) {
            Technique technique = techniques.get(index);
            EnchantmentDefinition definition = EnchantmentRegistry.get(technique.getId());
            if (definition == null) {
                DebugLog.log("附魔未登记：" + technique.getId());
                continue;
            }
            ElementReaction reaction = ElementReaction.of(skill.getEmbryo(index), definition.element());
            DebugLog.log("附魔 " + technique.getId() + " 原胚=" + skill.getEmbryo(index).getId()
                    + " " + reaction.describe());
            EnchantmentAccumulator current = resolution.effect(definition.createAccumulator().effectId());
            resolution = resolution.with(definition.fold(reaction, current));
        }
        return resolution;
    }

    private int segmentMagicDamage(Skill skill, ChainSegment segment) {
        int total = 0;
        List<Technique> techniques = skill.getTechniques();
        for (int index : segment.enchantIndexes()) {
            Technique technique = techniques.get(index);
            EnchantmentDefinition definition = EnchantmentRegistry.get(technique.getId());
            if (definition == null) {
                continue;
            }
            ElementReaction reaction = ElementReaction.of(skill.getEmbryo(index), definition.element());
            total += definition.magicDamage(reaction);
        }
        return total;
    }

    private int segmentManaCost(List<Technique> techniques, ChainSegment segment) {
        int cost = techniques.get(segment.actionIndex()).manaCost();
        for (int index : segment.enchantIndexes()) {
            cost += techniques.get(index).manaCost();
        }
        return cost;
    }

    public static void register(SimpleChannel channel, int id) {
        channel.registerMessage(id,
                SkillUsePacket.class,
                SkillUsePacket::encode,
                SkillUsePacket::decode,
                SkillUsePacket::handle);
    }
}
