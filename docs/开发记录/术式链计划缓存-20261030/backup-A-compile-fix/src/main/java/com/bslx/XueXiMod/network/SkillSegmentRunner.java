package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.debug.DebugLog;
import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.shu.technique.ActionWindow;
import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.TechniqueEffectRegistry;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.effects.LockTargetService;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantBonus;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalResult;
import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
import com.bslx.XueXiMod.shu.technique.release.parting.PartingOffers;
import com.bslx.XueXiMod.shu.technique.release.parting.PartingOperationId;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.field.FieldInfluence;
import com.bslx.XueXiMod.shu.technique.release.field.FieldSpawner;
import com.bslx.XueXiMod.shu.technique.release.field.FieldZones;
import com.bslx.XueXiMod.command.SkillEntityCount;
import com.bslx.XueXiMod.shu.technique.release.momentum.ChargeGate;
import com.bslx.XueXiMod.shu.technique.release.momentum.HeavyWater;
import com.bslx.XueXiMod.shu.technique.release.momentum.WaterMomentum;
import com.bslx.XueXiMod.shu.technique.release.entity.HostBeatHooks;
import com.bslx.XueXiMod.shu.technique.release.entity.HostCast;
import com.bslx.XueXiMod.shu.technique.release.entity.HostGroupCommit;
import com.bslx.XueXiMod.shu.technique.release.entity.HostGroupContext;
import com.bslx.XueXiMod.shu.technique.release.entity.HostGroupListener;
import com.bslx.XueXiMod.shu.technique.release.entity.HostGroupReceipt;
import com.bslx.XueXiMod.shu.technique.release.water.CastSource;
import com.bslx.XueXiMod.shu.technique.release.payload.HitPayloads;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

final class SkillSegmentRunner {
    static Runnable buildSegmentRunner(ServerPlayer player, Technique action, List<Technique> techniques,
                                        int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                        EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                        boolean lockThisCast, boolean dropThisCast) {
        return buildSegmentRunner(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                onFirstTrigger, lockThisCast, dropThisCast, null, 0, 1, ReleaseBatch.current(), EnchantBonus.NONE, 0, null);
    }

    static Runnable buildSegmentRunner(ServerPlayer player, Technique action, List<Technique> techniques,
                                        int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                        EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                        boolean lockThisCast, boolean dropThisCast, CastSource castSource) {
        return buildSegmentRunner(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                onFirstTrigger, lockThisCast, dropThisCast, null, 0, 1, ReleaseBatch.current(), EnchantBonus.NONE, 0, castSource);
    }

    static Runnable buildSegmentRunner(ServerPlayer player, Technique action, List<Technique> techniques,
                                        int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                        EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                        boolean lockThisCast, boolean dropThisCast, ReleaseBatch batch) {
        return buildSegmentRunner(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                onFirstTrigger, lockThisCast, dropThisCast, null, 0, 1, batch, EnchantBonus.NONE, 0, null);
    }

    static Runnable buildSegmentRunner(ServerPlayer player, Technique action, List<Technique> techniques,
                                        int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                        EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                        boolean lockThisCast, boolean dropThisCast,
                                        ReleaseBatch.Tally tally, int copyIndex, int copyCount, ReleaseBatch batch,
                                        EnchantBonus bonus, int volley, CastSource castSource) {
        return buildSegmentRunner(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                onFirstTrigger, lockThisCast, dropThisCast, tally, copyIndex, copyCount, batch, bonus, volley, castSource,
                SkillChainPlan.frozenChainInput(null, techniques, actionIndex));
    }

    static Runnable buildSegmentRunner(ServerPlayer player, Technique action, List<Technique> techniques,
                                        int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                        EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                        boolean lockThisCast, boolean dropThisCast,
                                        ReleaseBatch.Tally tally, int copyIndex, int copyCount, ReleaseBatch batch,
                                        EnchantBonus bonus, int volley, CastSource castSource, ReleaseInput chainInput) {
        return buildSegmentRunner(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                onFirstTrigger, lockThisCast, dropThisCast, tally, copyIndex, copyCount, batch, bonus, volley, castSource,
                chainInput, null);
    }

    static Runnable buildSegmentRunner(ServerPlayer player, Technique action, List<Technique> techniques,
                                        int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                        EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                        boolean lockThisCast, boolean dropThisCast,
                                        ReleaseBatch.Tally tally, int copyIndex, int copyCount, ReleaseBatch batch,
                                        EnchantBonus bonus, int volley, CastSource castSource, ReleaseInput chainInput,
                                        CastPlanContext planContext) {
        ReleaseInput frozenInput = chainInput == null ? ReleaseInput.BASE : chainInput;
        return () -> {
            if (!player.isAlive()) {
                return;
            }
            if (!HeldSwordGate.allows(action, player.getMainHandItem())) {
                return;
            }
            Vec3 lookVec = player.getLookAngle();
            Vec3 position = player.position();
            if (commitLooseHost(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                    onFirstTrigger, lockThisCast, dropThisCast, tally, copyIndex, copyCount, batch, bonus, volley, lookVec, position, castSource,
                    frozenInput, planContext)) {
                return;
            }
            if (action.getType() == TechniqueType.SWORD) {
                player.swing(InteractionHand.MAIN_HAND, true);
            }
            runSegment(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                    onFirstTrigger, lockThisCast, dropThisCast, tally, copyIndex, copyCount, batch, lookVec, position,
                    bonus, volley, null, null, null, frozenInput, planContext);
        };
    }

    static Runnable buildSegmentRunnerAt(ServerPlayer player, Technique action, List<Technique> techniques,
                                          int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                          EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                          Vec3 lookVec, Vec3 position, boolean lockThisCast, boolean dropThisCast) {
        return buildSegmentRunnerAt(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                onFirstTrigger, lookVec, position, lockThisCast, dropThisCast, null, 0, 1, ReleaseBatch.current(), EnchantBonus.NONE, 0, null);
    }

    static Runnable buildSegmentRunnerAt(ServerPlayer player, Technique action, List<Technique> techniques,
                                          int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                          EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                          Vec3 lookVec, Vec3 position, boolean lockThisCast, boolean dropThisCast,
                                          ReleaseBatch batch) {
        return buildSegmentRunnerAt(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                onFirstTrigger, lookVec, position, lockThisCast, dropThisCast, null, 0, 1, batch, EnchantBonus.NONE, 0, null);
    }

    static Runnable buildSegmentRunnerAt(ServerPlayer player, Technique action, List<Technique> techniques,
                                          int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                          EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                          Vec3 lookVec, Vec3 position, boolean lockThisCast, boolean dropThisCast,
                                          ReleaseBatch.Tally tally, int copyIndex, int copyCount, ReleaseBatch batch,
                                          EnchantBonus bonus, int volley, CastSource castSource) {
        return buildSegmentRunnerAt(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                onFirstTrigger, lookVec, position, lockThisCast, dropThisCast, tally, copyIndex, copyCount, batch, bonus, volley, castSource,
                SkillChainPlan.frozenChainInput(null, techniques, actionIndex));
    }

    static Runnable buildSegmentRunnerAt(ServerPlayer player, Technique action, List<Technique> techniques,
                                          int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                          EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                          Vec3 lookVec, Vec3 position, boolean lockThisCast, boolean dropThisCast,
                                          ReleaseBatch.Tally tally, int copyIndex, int copyCount, ReleaseBatch batch,
                                          EnchantBonus bonus, int volley, CastSource castSource, ReleaseInput chainInput) {
        return buildSegmentRunnerAt(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                onFirstTrigger, lookVec, position, lockThisCast, dropThisCast, tally, copyIndex, copyCount, batch, bonus, volley, castSource,
                chainInput, null);
    }

    static Runnable buildSegmentRunnerAt(ServerPlayer player, Technique action, List<Technique> techniques,
                                          int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                          EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                          Vec3 lookVec, Vec3 position, boolean lockThisCast, boolean dropThisCast,
                                          ReleaseBatch.Tally tally, int copyIndex, int copyCount, ReleaseBatch batch,
                                          EnchantBonus bonus, int volley, CastSource castSource, ReleaseInput chainInput,
                                          CastPlanContext planContext) {
        ReleaseInput frozenInput = chainInput == null ? ReleaseInput.BASE : chainInput;
        return () -> {
            if (!player.isAlive()) {
                return;
            }
            if (!HeldSwordGate.allows(action, player.getMainHandItem())) {
                return;
            }
            if (commitLooseHost(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                    onFirstTrigger, lockThisCast, dropThisCast, tally, copyIndex, copyCount, batch, bonus, volley, lookVec, position, castSource,
                    frozenInput, planContext)) {
                return;
            }
            if (action.getType() == TechniqueType.SWORD) {
                player.swing(InteractionHand.MAIN_HAND, true);
            }
            runSegment(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                    onFirstTrigger, lockThisCast, dropThisCast, tally, copyIndex, copyCount, batch, lookVec, position,
                    bonus, volley, null, null, null, frozenInput, planContext);
        };
    }

    /** 马上提交。蓄力宿主在这里直接拒绝，不排进以后的 tick。 */
    static ReleaseOutcome commitNow(ServerPlayer player, Technique action, List<Technique> techniques,
                                     int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                     EnchantmentResolution enchantments, boolean lockThisCast, boolean dropThisCast,
                                     ReleaseBatch.Tally tally, int copyIndex, int copyCount, ReleaseBatch batch,
                                     EnchantBonus bonus, int volley, Vec3 lookVec, Vec3 position) {
        return commitNow(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                lockThisCast, dropThisCast, tally, copyIndex, copyCount, batch, bonus, volley, lookVec, position,
                SkillChainPlan.frozenChainInput(null, techniques, actionIndex));
    }

    static ReleaseOutcome commitNow(ServerPlayer player, Technique action, List<Technique> techniques,
                                     int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                     EnchantmentResolution enchantments, boolean lockThisCast, boolean dropThisCast,
                                     ReleaseBatch.Tally tally, int copyIndex, int copyCount, ReleaseBatch batch,
                                     EnchantBonus bonus, int volley, Vec3 lookVec, Vec3 position, ReleaseInput chainInput) {
        return commitNow(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                lockThisCast, dropThisCast, tally, copyIndex, copyCount, batch, bonus, volley, lookVec, position, chainInput, null);
    }

    static ReleaseOutcome commitNow(ServerPlayer player, Technique action, List<Technique> techniques,
                                     int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                     EnchantmentResolution enchantments, boolean lockThisCast, boolean dropThisCast,
                                     ReleaseBatch.Tally tally, int copyIndex, int copyCount, ReleaseBatch batch,
                                     EnchantBonus bonus, int volley, Vec3 lookVec, Vec3 position, ReleaseInput chainInput,
                                     CastPlanContext planContext) {
        int nowTick = player.getServer() == null ? 0 : player.getServer().getTickCount();
        if (!player.isAlive()) {
            return ReleaseOutcome.cancelled(nowTick, "施法者已经离开");
        }
        if (!HeldSwordGate.allows(action, player.getMainHandItem())) {
            return ReleaseOutcome.rejected(nowTick, "");
        }
        ReleaseDefinition definition = ReleaseRegistry.get(action.getId());
        if (action.getBaseDamage() <= 0f && definition != null && definition.conditionalEffect() != null) {
            return ReleaseOutcome.rejected(nowTick, "条件释放没有按回执提交");
        }
        if (TechniqueEffectRegistry.get(action.getId()) == null) {
            return ReleaseOutcome.rejected(nowTick, "没有可提交的释放");
        }
        int chargeTicks = definition == null ? 0 : definition.chargeTicks();
        if (chargeTicks > 0 && !ChargeGate.armed(player.getUUID())) {
            return ReleaseOutcome.rejected(nowTick, "蓄力宿主不能在这一拍提交");
        }
        runSegment(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                null, lockThisCast, dropThisCast, tally, copyIndex, copyCount, batch, lookVec, position,
                bonus, volley, position, lookVec, null, chainInput, planContext);
        return ReleaseOutcome.committed(player.getServer().getTickCount());
    }

    /** 接入宿主组的术式走这里。效果返回不算提交，整组生成成功才提交一次。 */
    static ReleaseOutcome commitHostGroup(ServerPlayer player, Technique action, List<Technique> techniques,
                                           int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                           EnchantmentResolution enchantments, boolean lockThisCast, boolean dropThisCast,
                                           ReleaseBatch.Tally tally, int copyCount, ReleaseBatch batch,
                                           EnchantBonus bonus, int volley, Vec3 lookVec, Vec3 position,
                                           Vec3 contactOrigin, Vec3 contactLook,
                                           HostCast hostCast, UUID groupId, HostGroupListener listener) {
        return commitHostGroup(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                lockThisCast, dropThisCast, tally, copyCount, batch, bonus, volley, lookVec, position,
                contactOrigin, contactLook, hostCast, groupId, listener,
                SkillChainPlan.frozenChainInput(null, techniques, actionIndex));
    }

    static ReleaseOutcome commitHostGroup(ServerPlayer player, Technique action, List<Technique> techniques,
                                           int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                           EnchantmentResolution enchantments, boolean lockThisCast, boolean dropThisCast,
                                           ReleaseBatch.Tally tally, int copyCount, ReleaseBatch batch,
                                           EnchantBonus bonus, int volley, Vec3 lookVec, Vec3 position,
                                           Vec3 contactOrigin, Vec3 contactLook,
                                           HostCast hostCast, UUID groupId, HostGroupListener listener,
                                           ReleaseInput chainInput) {
        return commitHostGroup(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                null, lockThisCast, dropThisCast, tally, copyCount, batch, bonus, volley, lookVec, position,
                contactOrigin, contactLook, hostCast, groupId, listener, chainInput);
    }

    static ReleaseOutcome commitHostGroup(ServerPlayer player, Technique action, List<Technique> techniques,
                                           int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                           EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                           boolean lockThisCast, boolean dropThisCast,
                                           ReleaseBatch.Tally tally, int copyCount, ReleaseBatch batch,
                                           EnchantBonus bonus, int volley, Vec3 lookVec, Vec3 position,
                                           Vec3 contactOrigin, Vec3 contactLook,
                                           HostCast hostCast, UUID groupId, HostGroupListener listener, ReleaseInput chainInput) {
        return commitHostGroup(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                onFirstTrigger, lockThisCast, dropThisCast, tally, copyCount, batch, bonus, volley, lookVec, position,
                contactOrigin, contactLook, hostCast, groupId, listener, chainInput, null);
    }

    static ReleaseOutcome commitHostGroup(ServerPlayer player, Technique action, List<Technique> techniques,
                                           int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                           EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                           boolean lockThisCast, boolean dropThisCast,
                                           ReleaseBatch.Tally tally, int copyCount, ReleaseBatch batch,
                                           EnchantBonus bonus, int volley, Vec3 lookVec, Vec3 position,
                                           Vec3 contactOrigin, Vec3 contactLook,
                                           HostCast hostCast, UUID groupId, HostGroupListener listener, ReleaseInput chainInput,
                                           CastPlanContext planContext) {
        ReleaseInput frozenInput = chainInput == null ? ReleaseInput.BASE : chainInput;
        int nowTick = player.getServer() == null ? 0 : player.getServer().getTickCount();
        if (hostCast == null) {
            return ReleaseOutcome.rejected(nowTick, "宿主组没有父释放");
        }
        HostGroupReceipt prior = HostGroupCommit.find(hostCast, groupId);
        if (prior != null) {
            return outcomeOf(prior);
        }
        if (hostCast.closed()) {
            return outcomeOf(HostGroupCommit.cancel(hostCast, groupId, nowTick, "宿主组已经结束"));
        }
        if (!player.isAlive()) {
            return outcomeOf(HostGroupCommit.cancel(hostCast, groupId, nowTick, "施法者已经离开"));
        }
        if (!HeldSwordGate.allows(action, player.getMainHandItem())) {
            return outcomeOf(HostGroupCommit.reject(hostCast, groupId, nowTick, ""));
        }
        if (TechniqueEffectRegistry.get(action.getId()) == null) {
            return outcomeOf(HostGroupCommit.reject(hostCast, groupId, nowTick, "没有可提交的释放"));
        }
        ReleaseDefinition definition = ReleaseRegistry.get(action.getId());
        int chargeTicks = definition == null ? 0 : definition.chargeTicks();
        if (chargeTicks > 0 && !ChargeGate.armed(player.getUUID())) {
            return outcomeOf(HostGroupCommit.reject(hostCast, groupId, nowTick, "蓄力宿主不能在这一拍提交"));
        }
        int spawnCount = Math.max(1, SkillEntityCount.entityCount());
        int safeCopies = Math.max(1, copyCount);
        Vec3 sharedLook = lookVec != null ? lookVec : player.getLookAngle();
        Vec3 sharedPlace = position != null ? position : player.position();
        HostGroupReceipt receipt = HostGroupCommit.commitOnce(hostCast, player, groupId, safeCopies * spawnCount, spawnCount, listener,
                () -> {
                    if (definition != null && definition.tracksBeatCycle()) {
                        HostBeatHooks.beforeCopies(player, HostGroupContext.current());
                    }
                    for (int copyIndex = 0; copyIndex < safeCopies; copyIndex++) {
                        HostGroupContext hostGroup = HostGroupContext.current();
                        if (hostGroup != null && hostGroup.failed()) {
                            break;
                        }
                        runSegment(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                                onFirstTrigger, lockThisCast, dropThisCast, tally, copyIndex, safeCopies, batch, sharedLook, sharedPlace,
                                bonus, volley, contactOrigin, contactLook, null, frozenInput, planContext);
                    }
                });
        return outcomeOf(receipt);
    }

    private static ReleaseOutcome outcomeOf(HostGroupReceipt receipt) {
        if (receipt == null) {
            return ReleaseOutcome.rejected(0, "宿主组没有回执");
        }
        return switch (receipt.kind()) {
            case COMMITTED -> ReleaseOutcome.committed(receipt.tick());
            case CANCELLED -> ReleaseOutcome.cancelled(receipt.tick(), receipt.reason());
            case REJECTED -> ReleaseOutcome.rejected(receipt.tick(), receipt.reason());
        };
    }

    static boolean playAction(ServerPlayer player, Technique action, List<Technique> techniques, int actionIndex,
                              ItemStack weapon, ActionWindow.Sink sink) {
        return playAction(player, action, techniques, actionIndex, weapon, sink,
                SkillChainPlan.frozenChainInput(null, techniques, actionIndex));
    }

    static boolean playAction(ServerPlayer player, Technique action, List<Technique> techniques, int actionIndex,
                              ItemStack weapon, ActionWindow.Sink sink, ReleaseInput chainInput) {
        return playAction(player, action, techniques, actionIndex, weapon, sink, chainInput, null);
    }

    static boolean playAction(ServerPlayer player, Technique action, List<Technique> techniques, int actionIndex,
                              ItemStack weapon, ActionWindow.Sink sink, ReleaseInput chainInput, CastPlanContext planContext) {
        if (player == null || !player.isAlive() || sink == null || TechniqueEffectRegistry.get(action.getId()) == null) {
            return false;
        }
        runSegment(player, action, techniques, actionIndex, weapon, 0f, false, EnchantmentResolution.empty(),
                null, false, false, null, 0, 1, null, player.getLookAngle(), player.position(),
                EnchantBonus.NONE, 0, null, null, sink, chainInput, planContext);
        return true;
    }

    /** 条件释放的真实回执。排进调度器本身不算提交。 */
    static ReleaseOutcome commitConditional(ServerPlayer player, ReleaseDefinition definition,
                                            UUID operationId, EnchantmentSnapshot contact) {
        return commitConditional(player, definition, operationId, contact, ReleaseInput.BASE);
    }

    static ReleaseOutcome commitConditional(ServerPlayer player, ReleaseDefinition definition,
                                            UUID operationId, EnchantmentSnapshot contact, ReleaseInput chainInput) {
        return commitConditional(player, definition, operationId, contact, chainInput, null);
    }

    static ReleaseOutcome commitConditional(ServerPlayer player, ReleaseDefinition definition,
                                            UUID operationId, EnchantmentSnapshot contact, ReleaseInput chainInput,
                                            ManaPaymentReceipt receipt) {
        int nowTick = player == null || player.getServer() == null ? 0 : player.getServer().getTickCount();
        if (player == null || !player.isAlive()) {
            return ReleaseOutcome.cancelled(nowTick, "施法者已经离开");
        }
        if (definition == null || definition.conditionalEffect() == null || operationId == null) {
            return ReleaseOutcome.rejected(nowTick, "没有条件释放");
        }
        try {
            ReleaseInput frozenInput = chainInput == null ? ReleaseInput.BASE : chainInput;
            ConditionalResult result = definition.conditionalEffect().attempt(player, operationId, contact, frozenInput, receipt);
            if (result != null && result.isCommitted()) {
                return ReleaseOutcome.committed(nowTick);
            }
            String because = result == null || result.reason() == null ? "" : result.reason();
            return ReleaseOutcome.rejected(nowTick, because);
        } catch (RuntimeException failure) {
            DebugLog.log("条件释放失败：" + failure);
            return ReleaseOutcome.rejected(nowTick, "这次没有释放");
        }
    }

    static void runSegment(ServerPlayer player, Technique action, List<Technique> techniques, int actionIndex,
                            ItemStack weapon, float finalDamage, boolean carries,
                            EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                            boolean lockThisCast, boolean dropThisCast,
                            ReleaseBatch.Tally tally, int copyIndex, int copyCount,
                            ReleaseBatch batch, Vec3 lookVec, Vec3 position,
                            EnchantBonus bonus, int volley,
                            Vec3 contactOrigin, Vec3 contactLook, ActionWindow.Sink actionSink, ReleaseInput chainInput) {
        runSegment(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                onFirstTrigger, lockThisCast, dropThisCast, tally, copyIndex, copyCount, batch, lookVec, position,
                bonus, volley, contactOrigin, contactLook, actionSink, chainInput, null);
    }

    static void runSegment(ServerPlayer player, Technique action, List<Technique> techniques, int actionIndex,
                            ItemStack weapon, float finalDamage, boolean carries,
                            EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                            boolean lockThisCast, boolean dropThisCast,
                            ReleaseBatch.Tally tally, int copyIndex, int copyCount,
                            ReleaseBatch batch, Vec3 lookVec, Vec3 position,
                            EnchantBonus bonus, int volley,
                            Vec3 contactOrigin, Vec3 contactLook, ActionWindow.Sink actionSink, ReleaseInput chainInput,
                            CastPlanContext planContext) {
        long segmentStart = PerformanceRecorder.begin();
        try {
        ReleaseInput frozenInput = chainInput == null ? ReleaseInput.BASE : chainInput;
        ReleaseDefinition conditionalDefinition = ReleaseRegistry.get(action.getId());
        if (action.getBaseDamage() <= 0f && conditionalDefinition != null && conditionalDefinition.conditionalEffect() != null) {
            return;
        }
        if (contactOrigin != null) {
            position = contactOrigin;
        }
        if (contactLook != null && contactLook.lengthSqr() > 1.0E-8) {
            lookVec = contactLook.normalize();
        }
        final Vec3 scheduledLook = lookVec;
        final Vec3 scheduledPosition = position;
        TechniqueEffect effect = TechniqueEffectRegistry.get(action.getId());
        if (effect == null) {
            return;
        }
        ReleaseDefinition attackDefinition = ReleaseRegistry.get(action.getId());
        int chargeTicks = attackDefinition == null ? 0 : attackDefinition.chargeTicks();
        if (chargeTicks > 0 && !ChargeGate.armed(player.getUUID())) {
            int epoch = ChargeGate.epoch(player.getUUID());
            WaterMomentum.Hold hold = WaterMomentum.reserve(player, attackDefinition.momentumSpend());
            MinecraftServer server = player.getServer();
            if (server == null) {
                WaterMomentum.release(hold);
                return;
            }
            ChainScheduler.schedule(server, server.getTickCount() + chargeTicks, () -> {
                if (!ChargeGate.same(player.getUUID(), epoch) || !player.isAlive()) {
                    WaterMomentum.release(hold);
                    return;
                }
                WaterMomentum.commit(player, hold);
                ChargeGate.arm(player.getUUID());
                try {
                    runSegment(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                            onFirstTrigger, lockThisCast, dropThisCast, tally, copyIndex, copyCount, batch, scheduledLook,
                            scheduledPosition, bonus, volley, contactOrigin, contactLook, actionSink, frozenInput, planContext);
                } finally {
                    ChargeGate.disarm(player.getUUID());
                }
            });
            return;
        }
        if (chargeTicks <= 0 && attackDefinition != null) {
            WaterMomentum.commit(player, WaterMomentum.reserve(player, attackDefinition.momentumSpend()));
        }
        HeavyWater.Session heavySession = null;
        HostGroupContext openedGroup = HostGroupContext.current();
        if (attackDefinition != null && attackDefinition.heavyMomentumPerCopy() > 0f) {
            int multiplier = openedGroup == null ? SkillEntityCount.entityCount() : openedGroup.spawnCount();
            int plannedCopies = Math.max(1, copyCount) * multiplier;
            String groupKey = openedGroup == null
                    ? player.getUUID() + "/" + action.getId() + "/" + volley + "/"
                    + System.identityHashCode(batch) + "/" + player.serverLevel().getGameTime()
                    : player.getUUID() + "/host/" + openedGroup.groupId();
            heavySession = HeavyWater.enter(player, groupKey, attackDefinition.heavyMomentumPerCopy(), plannedCopies);
        }
        ReleaseBatch outer = ReleaseBatch.current();
        if (batch != null) {
            ReleaseBatch.bind(batch);
        }
        try {
            EnchantmentResolution applied = carries ? enchantments : EnchantmentResolution.empty();
            RoleScan roleScan = resolveRoles(techniques, actionIndex, planContext);
            boolean spaceLock = roleScan.spaceLock;
            boolean entityLock = roleScan.entityLock;
            boolean scatter = roleScan.scatter;
            boolean tracking = roleScan.tracking;
            Trajectory pathOverride = roleScan.pathOverride;
            boolean chainLock = entityLock;
            Trajectory chainPath = pathOverride;
            boolean chainTracking = tracking;
            FieldInfluence influence = FieldZones.influence(player);
            float usedDamage = finalDamage * influence.damageScale();
            tracking |= influence.attachedTracking;
            if (!entityLock && influence.attachedLock) {
                entityLock = true;
            }
            if (pathOverride == null && influence.attachedPath != null) {
                pathOverride = influence.attachedPath;
            }
            HostGroupContext hostGroup = HostGroupContext.current();
            boolean useFrozen = hostGroup != null && hostGroup.hasFrozenLook();
            LivingEntity lockedTarget = null;
            if (useFrozen) {
                lookVec = hostGroup.frozenLook();
            } else if (entityLock) {
                lockedTarget = LockTargetService.resolve(player);
                if (lockedTarget != null) {
                    lockedTarget.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                }
            }
            if (attackDefinition != null && attackDefinition.fieldRadius() > 0f) {
                FieldSpawner.releaseAttached(player, attackDefinition, 1, chainLock, chainPath, chainTracking, frozenInput);
            }
            boolean mutable = attackDefinition != null && attackDefinition.trajectoryMutable();
            boolean dedicatedAim = attackDefinition != null && attackDefinition.dedicatedInitialAim();
            boolean frameLock = entityLock && mutable ? true : lockThisCast;
            if (spaceLock) {
                frameLock = entityLock && mutable;
            }
            if (dedicatedAim) {
                frameLock = false;
                if (!useFrozen && entityLock && lockedTarget != null) {
                    Vec3 fromPoint = contactOrigin != null ? contactOrigin : player.getEyePosition();
                    Vec3 towardTarget = lockedTarget.getBoundingBox().getCenter().subtract(fromPoint);
                    if (towardTarget.lengthSqr() > 1.0E-8) {
                        lookVec = towardTarget.normalize();
                    }
                }
            }
            if (hostGroup != null && !useFrozen) {
                hostGroup.freezeLook(lookVec);
            }
            Vec3 center;
            double groundLevel = Double.NEGATIVE_INFINITY;
            if (mutable && batch != null && (spaceLock || pathOverride == Trajectory.VORTEX)) {
                center = batch.crosshairCenter(player);
            } else if (mutable && scatter && !entityLock) {
                center = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
                groundLevel = player.getY();
            } else if (spaceLock && batch != null) {
                center = batch.resolveAnchor(player);
            } else {
                center = player.getEyePosition();
            }
            long seed = player.getUUID().getLeastSignificantBits()
                    ^ player.level().getGameTime()
                    ^ action.getId().hashCode()
                    ^ ((long) volley << 16);
            CastFrame frame = CastFrame.forAttack(usedDamage, applied, tally, copyIndex, copyCount,
                    center, lookVec, spaceLock, scatter, entityLock, pathOverride, mutable, seed, groundLevel);
            frame.tracking = tracking;
            frame.radiusMultiplier = bonus.radiusMultiplier;
            frame.releaseInput = frozenInput;
            float ownEnergy = attackDefinition != null ? attackDefinition.energyScale() : 1f;
            frame.energyScale = SkillChainPlan.geometryEnergy(ownEnergy, frozenInput);
            frame.burnSeconds = bonus.burnSeconds;
            frame.seedPoints = bonus.seedPoints;
            frame.controlTicks = bonus.controlTicks;
            frame.extinguish = bonus.extinguish;
            frame.knockback = bonus.knockback;
            frame.speedMultiplier = bonus.speedMultiplier;
            frame.tideSuccess = bonus == null ? TideMarkSuccess.NONE : bonus.tideSuccess(player.getUUID());
            frame.fieldDamageScale = influence.damageScale();
            frame.healAmount = pickedAmount(attackDefinition == null ? 0f : attackDefinition.healAmount(), influence.healAmount);
            frame.healCaster = attackDefinition != null && attackDefinition.healCaster();
            frame.pullStrength = pickedAmount(attackDefinition == null ? 0f : attackDefinition.pullStrength(), influence.pullStrength);
            frame.wetnessGrant = pickedGrant(attackDefinition == null ? 0 : attackDefinition.wetnessGrant(), influence.wetnessGrant);
            frame.pullAnchor = influence.pullAnchor != null ? influence.pullAnchor : player.position();
            frame.blocksMovement = attackDefinition != null && attackDefinition.blocksMovement();
            if ((attackDefinition != null && attackDefinition.purifyOnHit()) || influence.purify) {
                frame.extinguish = true;
            }
            if (attackDefinition != null && attackDefinition.knockbackStrength() > frame.knockback) {
                frame.knockback = attackDefinition.knockbackStrength();
            }
            if (attackDefinition != null) {
                frame.liftStrength = attackDefinition.liftStrength();
            }
            if (heavySession != null) {
                frame.heavyWater = heavySession.heavy();
                frame.releaseGroup = heavySession.groupId();
            }
            if (contactOrigin != null) {
                frame.overrideGeometry = true;
                frame.origin = position;
                frame.anchor = position;
                frame.look = lookVec;
                double flatReach = Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z);
                frame.planeNormal = flatReach < 1.0E-4
                        ? new Vec3(0.0, 0.0, 1.0)
                        : new Vec3(lookVec.x / flatReach, 0.0, lookVec.z / flatReach);
                frame.roll = 0.0F;
                frame.pose3d = false;
                frame.adoptSharedAnchor();
            }
            String batchMark = batch == null
                    ? "loose-" + player.serverLevel().getGameTime()
                    : Integer.toUnsignedString(System.identityHashCode(batch));
            PartingOffers.present(PartingOperationId.derive(player.getUUID(), batchMark, action.getId(), actionIndex, volley));
            CastFrame.bind(frame);
            try {
                if (hostGroup != null) {
                    hostGroup.noteCaster(player, frame);
                } else {
                    HitPayloads.applyCaster(player, frame);
                }
                TechniqueCastContext castContext = new TechniqueCastContext(usedDamage, lookVec, position, applied,
                        onFirstTrigger, frameLock, dropThisCast, actionSink);
                effect.execute(player, action, weapon, castContext);
            } finally {
                PartingOffers.clear();
                CastFrame.unbind();
            }
        } finally {
            boolean holdHeavy = openedGroup != null && heavySession != null;
            if (holdHeavy) {
                openedGroup.keepHeavy(heavySession);
            } else if (heavySession != null && copyIndex >= copyCount - 1) {
                HeavyWater.finish(heavySession);
            }
            if (!holdHeavy) {
                HeavyWater.clearCurrent();
            }
            if (batch != null) {
                ReleaseBatch.bind(outer);
            }
        }
        } finally {
            PerformanceRecorder.end(PerfMetric.SEGMENT_EXECUTE, segmentStart);
        }
    }

    /**
     * 还没进宿主组的拍号术式，由第 0 份打开自己的父释放并一次提交全部复制。
     * 后一份直接交回，避免各自推进。已经在宿主组里的调用不重复包一层。
     */
    private static boolean commitLooseHost(ServerPlayer player, Technique action, List<Technique> techniques,
                                            int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                            EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                            boolean lockThisCast, boolean dropThisCast,
                                            ReleaseBatch.Tally tally, int copyIndex, int copyCount, ReleaseBatch batch,
                                            EnchantBonus bonus, int volley, Vec3 lookVec, Vec3 position, CastSource castSource,
                                            ReleaseInput chainInput, CastPlanContext planContext) {
        ReleaseInput frozenInput = chainInput == null ? ReleaseInput.BASE : chainInput;
        ReleaseDefinition definition = ReleaseRegistry.get(action.getId());
        if (definition == null || !definition.submitsHostGroup() || HostGroupContext.current() != null) {
            return false;
        }
        if (copyIndex > 0) {
            return true;
        }
        if (action.getType() == TechniqueType.SWORD) {
            player.swing(InteractionHand.MAIN_HAND, true);
        }
        HostCast hostCast = HostCast.open(player.getUUID(), castSource);
        hostCast.hold();
        try {
            UUID hostGroup = hostCast.hostId(volley, 0);
            commitHostGroup(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                    onFirstTrigger, lockThisCast, dropThisCast, tally, copyCount, batch, bonus, volley, lookVec, position,
                    null, null, hostCast, hostGroup, null, frozenInput, planContext);
        } finally {
            hostCast.release();
        }
        return true;
    }

    static RoleScan scanRoles(List<Technique> techniques, int actionIndex) {
        long roleScanStart = PerformanceRecorder.begin();
        try {
            boolean spaceLock = false;
            boolean entityLock = false;
            boolean scatter = false;
            boolean tracking = false;
            Trajectory pathOverride = null;
            for (int earlierIndex = actionIndex - 1; earlierIndex >= 0; earlierIndex--) {
                Technique earlier = techniques.get(earlierIndex);
                if (earlier.getBaseDamage() > 0 && !earlier.isEnchantment()) {
                    if (SkillSegmentMath.sharesDualRelease(techniques, earlierIndex, actionIndex)) {
                        continue;
                    }
                    break;
                }
                switch (SkillSegmentMath.roleOf(earlier)) {
                    case SEEK_SPACE -> spaceLock = true;
                    case SEEK_ENTITY -> entityLock = true;
                    case TRAJECTORY_TRACKING -> tracking = true;
                    case TRAJECTORY_SCATTER -> scatter = true;
                    case TRAJECTORY_DROP -> {
                        if (pathOverride == null) {
                            ReleaseDefinition pathDefinition = ReleaseRegistry.get(earlier.getId());
                            if (pathDefinition != null) {
                                pathOverride = pathDefinition.trajectory();
                            }
                        }
                    }
                    default -> {
                    }
                }
            }
            return new RoleScan(spaceLock, entityLock, scatter, tracking, pathOverride);
        } finally {
            PerformanceRecorder.end(PerfMetric.CHAIN_ROLE_SCAN, roleScanStart);
        }
    }

    private static RoleScan resolveRoles(List<Technique> techniques, int actionIndex, CastPlanContext planContext) {
        if (planContext != null && planContext.canReuseStructure()) {
            RoleScan cached = planContext.roleScan(actionIndex);
            if (cached != null) {
                return cached;
            }
        }
        return scanRoles(techniques, actionIndex);
    }

    static final class RoleScan {
        final boolean spaceLock;
        final boolean entityLock;
        final boolean scatter;
        final boolean tracking;
        final Trajectory pathOverride;

        RoleScan(boolean spaceLock, boolean entityLock, boolean scatter, boolean tracking, Trajectory pathOverride) {
            this.spaceLock = spaceLock;
            this.entityLock = entityLock;
            this.scatter = scatter;
            this.tracking = tracking;
            this.pathOverride = pathOverride;
        }
    }

    /** 术式自己的一份和场地的一份不叠加，取更大的那份。 */
    private static float pickedAmount(float fromDefinition, float fromField) {
        return fromDefinition >= fromField ? fromDefinition : fromField;
    }

    private static int pickedGrant(int fromDefinition, int fromField) {
        return fromDefinition >= fromField ? fromDefinition : fromField;
    }
}
