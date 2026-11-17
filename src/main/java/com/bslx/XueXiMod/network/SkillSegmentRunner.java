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
import com.bslx.XueXiMod.shu.technique.release.domain.motion.DomainCastContext;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.bind.LifetimeToken;
import com.bslx.XueXiMod.shu.technique.release.bind.PublicationAttempt;
import com.bslx.XueXiMod.shu.technique.release.bind.ReleaseExecutionContext;
import com.bslx.XueXiMod.shu.technique.release.bind.ReleaseInputMode;
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

/**
 * 单段释放的服务端执行器。调用方传入已重编的术式、伤害和冻结链输入；这里重查存活、持剑、蓄力和服务端视线。
 * 不信任客户端选择的目标、落点或效果结果。排队任务遇到失效条件静默返回；立即提交则返回拒绝或取消原因。
 * 扣费由外层份额负责，真正的术式效果见被调用方。
 */
final class SkillSegmentRunner {
    private static final ThreadLocal<Boolean> chargeReady = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<MemberCastView> boundMemberView = new ThreadLocal<>();

    /** 把当前线程绑定到一名多重成员视图。传入空时清除绑定，不执行释放。 */
    static void bindMemberView(MemberCastView view) {
        if (view == null) {
            boundMemberView.remove();
            return;
        }
        boundMemberView.set(view);
    }

    /** 清除当前线程的多重成员视图。 */
    static void clearMemberView() {
        boundMemberView.remove();
    }

    /** 标记当前线程的蓄力延迟已经完成，允许再次进入同一段执行。 */
    static void openChargeBypass() {
        chargeReady.set(Boolean.TRUE);
    }

    /** 关闭当前线程的蓄力完成标记。 */
    static void closeChargeBypass() {
        chargeReady.set(Boolean.FALSE);
    }

    /** 组装使用当前批次、当前视线和位置的延迟段任务。 */
    static Runnable buildSegmentRunner(ServerPlayer player, Technique action, List<Technique> techniques,
                                        int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                        EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                        boolean lockThisCast, boolean dropThisCast) {
        return buildSegmentRunner(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                onFirstTrigger, lockThisCast, dropThisCast, null, 0, 1, ReleaseBatch.current(), EnchantBonus.NONE, 0, null);
    }

    /** 组装带施法来源的延迟段任务，其余输入使用当前状态。 */
    static Runnable buildSegmentRunner(ServerPlayer player, Technique action, List<Technique> techniques,
                                        int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                        EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                        boolean lockThisCast, boolean dropThisCast, CastSource castSource) {
        return buildSegmentRunner(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                onFirstTrigger, lockThisCast, dropThisCast, null, 0, 1, ReleaseBatch.current(), EnchantBonus.NONE, 0, castSource);
    }

    /** 组装使用调用方指定批次的延迟段任务。 */
    static Runnable buildSegmentRunner(ServerPlayer player, Technique action, List<Technique> techniques,
                                        int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                        EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                        boolean lockThisCast, boolean dropThisCast, ReleaseBatch batch) {
        return buildSegmentRunner(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                onFirstTrigger, lockThisCast, dropThisCast, null, 0, 1, batch, EnchantBonus.NONE, 0, null);
    }

    /** 组装带复制序号、附魔和施法来源的延迟段任务，并按宿主现算冻结链输入。 */
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

    /** 组装使用调用方冻结链输入的延迟段任务。 */
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

    /**
     * 组装最终延迟任务。执行时施法者离场或持剑条件失效就静默返回；
     * 宿主组由loose提交一次，剑术会挥动主手，然后进入段执行。
     */
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

    /** 组装使用预定视线和位置的延迟段任务。 */
    static Runnable buildSegmentRunnerAt(ServerPlayer player, Technique action, List<Technique> techniques,
                                          int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                          EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                          Vec3 lookVec, Vec3 position, boolean lockThisCast, boolean dropThisCast) {
        return buildSegmentRunnerAt(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                onFirstTrigger, lookVec, position, lockThisCast, dropThisCast, null, 0, 1, ReleaseBatch.current(), EnchantBonus.NONE, 0, null);
    }

    /** 组装使用预定几何和调用方批次的延迟段任务。 */
    static Runnable buildSegmentRunnerAt(ServerPlayer player, Technique action, List<Technique> techniques,
                                          int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                          EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
                                          Vec3 lookVec, Vec3 position, boolean lockThisCast, boolean dropThisCast,
                                          ReleaseBatch batch) {
        return buildSegmentRunnerAt(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                onFirstTrigger, lookVec, position, lockThisCast, dropThisCast, null, 0, 1, batch, EnchantBonus.NONE, 0, null);
    }

    /** 组装带复制信息、预定几何并按宿主现算链输入的延迟段任务。 */
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

    /** 组装使用预定几何和调用方冻结链输入的延迟段任务。 */
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

    /**
     * 组装最终的定点延迟任务。离场或持剑失效时静默返回；
     * 不在执行时重新读取视线和位置。宿主组、挥剑和段执行见被调用方。
     */
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

    /** 马上提交，并按宿主现算冻结链输入。拒绝边界见完整入口。 */
    static ReleaseOutcome commitNow(ServerPlayer player, Technique action, List<Technique> techniques,
                                     int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                     EnchantmentResolution enchantments, boolean lockThisCast, boolean dropThisCast,
                                     ReleaseBatch.Tally tally, int copyIndex, int copyCount, ReleaseBatch batch,
                                     EnchantBonus bonus, int volley, Vec3 lookVec, Vec3 position) {
        return commitNow(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                lockThisCast, dropThisCast, tally, copyIndex, copyCount, batch, bonus, volley, lookVec, position,
                SkillChainPlan.frozenChainInput(null, techniques, actionIndex));
    }

    /** 马上提交一份调用方冻结的链输入。拒绝边界见完整入口。 */
    static ReleaseOutcome commitNow(ServerPlayer player, Technique action, List<Technique> techniques,
                                     int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                     EnchantmentResolution enchantments, boolean lockThisCast, boolean dropThisCast,
                                     ReleaseBatch.Tally tally, int copyIndex, int copyCount, ReleaseBatch batch,
                                     EnchantBonus bonus, int volley, Vec3 lookVec, Vec3 position, ReleaseInput chainInput) {
        return commitNow(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                lockThisCast, dropThisCast, tally, copyIndex, copyCount, batch, bonus, volley, lookVec, position, chainInput, null);
    }

    /**
     * 马上提交。蓄力宿主、离场、自动释放失效、持剑不符、条件段或缺少效果都直接拒绝，不排进以后的 tick。
     * 通过后才进入段执行。
     */
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
        if (automaticBlocked(player)) {
            return ReleaseOutcome.rejected(nowTick, "");
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

    /** 按宿主现算链输入后提交宿主组。回执语义见完整入口。 */
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

    /** 使用调用方链输入提交宿主组，不绑定首次触发回调。 */
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

    /** 使用调用方链输入和计划上下文提交宿主组。回执语义见完整入口。 */
    static ReleaseOutcome commitHostGroup(ServerPlayer player, Technique action, List<Technique> techniques,
                                           int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
                                           EnchantmentResolution enchantments, boolean lockThisCast, boolean dropThisCast,
                                           ReleaseBatch.Tally tally, int copyCount, ReleaseBatch batch,
                                           EnchantBonus bonus, int volley, Vec3 lookVec, Vec3 position,
                                           Vec3 contactOrigin, Vec3 contactLook,
                                           HostCast hostCast, UUID groupId, HostGroupListener listener,
                                           ReleaseInput chainInput, CastPlanContext planContext) {
        return commitHostGroup(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                null, lockThisCast, dropThisCast, tally, copyCount, batch, bonus, volley, lookVec, position,
                contactOrigin, contactLook, hostCast, groupId, listener, chainInput, planContext);
    }

    /** 带首次触发回调提交宿主组，不使用计划上下文。 */
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

    /**
     * 接入宿主组的术式走这里。效果返回不算提交，整组生成成功才提交一次。
     * 已有回执直接复用；父释放缺失、结束、离场、蓄力或持剑失效时返回对应拒绝或取消。
     */
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
        if (automaticBlocked(player)) {
            return outcomeOf(HostGroupCommit.reject(hostCast, groupId, nowTick, ""));
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
        ReleaseExecutionContext automaticContext = domainContext();
        Vec3 sharedLook;
        Vec3 sharedPlace;
        if (automaticContext != null) {
            sharedLook = lookVec != null ? lookVec : automaticContext.baseDirection();
            sharedPlace = position != null ? position : automaticContext.origin();
        } else {
            sharedLook = lookVec != null ? lookVec : player.getLookAngle();
            sharedPlace = position != null ? position : player.position();
        }
        HostGroupReceipt receipt;
        try {
            receipt = HostGroupCommit.commitOnce(hostCast, player, groupId, safeCopies * spawnCount, spawnCount, listener,
                    () -> {
                        if (definition != null && definition.tracksBeatCycle()) {
                            HostBeatHooks.beforeCopies(player, HostGroupContext.current());
                        }
                        for (int copyIndex = 0; copyIndex < safeCopies; copyIndex++) {
                            if (automaticBlocked(player)) {
                                break;
                            }
                            HostGroupContext hostGroup = HostGroupContext.current();
                            if (hostGroup != null && hostGroup.failed()) {
                                break;
                            }
                            runSegment(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                                    onFirstTrigger, lockThisCast, dropThisCast, tally, copyIndex, safeCopies, batch, sharedLook, sharedPlace,
                                    bonus, volley, contactOrigin, contactLook, null, frozenInput, planContext);
                        }
                    });
        } catch (Throwable thrown) {
            DebugLog.log("宿主组发布异常：" + thrown);
            receipt = HostGroupCommit.find(hostCast, groupId);
        }
        return outcomeOf(receipt);
    }

    /** 把宿主组回执翻译成分段提交结果。没有回执时拒绝。 */
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

    /** 播放动作宿主，并按动作现算链输入。 */
    static boolean playAction(ServerPlayer player, Technique action, List<Technique> techniques, int actionIndex,
                              ItemStack weapon, ActionWindow.Sink sink) {
        return playAction(player, action, techniques, actionIndex, weapon, sink,
                SkillChainPlan.frozenChainInput(null, techniques, actionIndex));
    }

    /** 播放动作宿主并使用调用方链输入。 */
    static boolean playAction(ServerPlayer player, Technique action, List<Technique> techniques, int actionIndex,
                              ItemStack weapon, ActionWindow.Sink sink, ReleaseInput chainInput) {
        return playAction(player, action, techniques, actionIndex, weapon, sink, chainInput, null);
    }

    /**
     * 播放动作宿主。施法者离场、缺少动作接收器或没有效果时返回假；
     * 成功只表示已经进入段执行，不表示命中。
     */
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

    /** 条件释放的真实回执。排进调度器本身不算提交；这份入口使用基准链输入，不携带魔力回执。 */
    static ReleaseOutcome commitConditional(ServerPlayer player, ReleaseDefinition definition,
                                            UUID operationId, EnchantmentSnapshot contact) {
        return commitConditional(player, definition, operationId, contact, ReleaseInput.BASE);
    }

    /**
     * 用调用方链输入提交条件效果，不携带魔力回执。施法者离场时取消；定义、效果或操作编号缺失时拒绝。
     * 效果抛出异常时记录并拒绝，不在这里扣费。排进调度器本身不算提交。
     */
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

    /** 执行一段释放，不使用计划上下文。完整的失效和蓄力边界见两份入口中的后一份。 */
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

    /**
     * 执行一段已通过入口校验的释放。条件段在此静默跳过；缺少效果或自动释放令牌失效也静默返回。
     * 未完成蓄力时改排到蓄力结束。这里绑定释放帧并调用效果，不直接扣费或结算命中。
     */
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
        if (chargeTicks > 0 && !ChargeGate.armed(player.getUUID()) && !Boolean.TRUE.equals(chargeReady.get())) {
            ReleaseExecutionContext scheduledContext = domainContext();
            if (scheduledContext != null) {
                LifetimeToken scheduledToken = scheduledContext.token();
                WaterMomentum.Hold momentumHold = WaterMomentum.reserve(player, attackDefinition.momentumSpend());
                MinecraftServer automaticServer = player.getServer();
                if (automaticServer == null) {
                    WaterMomentum.release(momentumHold);
                    return;
                }
                ChainScheduler.schedule(automaticServer, automaticServer.getTickCount() + chargeTicks, () -> {
                    int firedTick = automaticServer.getTickCount();
                    if (!player.isAlive() || scheduledToken == null || !scheduledToken.valid(firedTick)) {
                        WaterMomentum.release(momentumHold);
                        return;
                    }
                    ReleaseExecutionContext.bind(scheduledContext);
                    chargeReady.set(Boolean.TRUE);
                    try {
                        WaterMomentum.commit(player, momentumHold);
                        runSegment(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
                                onFirstTrigger, lockThisCast, dropThisCast, tally, copyIndex, copyCount, batch, scheduledLook,
                                scheduledPosition, bonus, volley, contactOrigin, contactLook, actionSink, frozenInput, planContext);
                    } finally {
                        chargeReady.set(Boolean.FALSE);
                        ReleaseExecutionContext.unbind();
                    }
                });
                return;
            }
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
        ReleaseBatch outer = ReleaseBatch.current();
        boolean geometryOnly = batch != null
                && tally == null
                && contactOrigin == null
                && com.bslx.XueXiMod.shu.technique.release.batch.BirthLayout.current() != null;
        ReleaseBatch selectionBatch = batch;
        ReleaseBatch settlementBatch = geometryOnly ? null : batch;
        if (attackDefinition != null && attackDefinition.heavyMomentumPerCopy() > 0f) {
            int multiplier = openedGroup == null ? SkillEntityCount.entityCount() : openedGroup.spawnCount();
            int plannedCopies = Math.max(1, copyCount) * multiplier;
            String groupKey = openedGroup == null
                    ? player.getUUID() + "/" + action.getId() + "/" + volley + "/"
                    + System.identityHashCode(settlementBatch) + "/" + player.serverLevel().getGameTime()
                    : player.getUUID() + "/host/" + openedGroup.groupId();
            heavySession = HeavyWater.enter(player, groupKey, attackDefinition.heavyMomentumPerCopy(), plannedCopies);
        }
        if (settlementBatch != null) {
            ReleaseBatch.bind(settlementBatch);
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
            ReleaseExecutionContext automaticContext = domainContext();
            boolean domainAutomatic = automaticContext != null;
            LivingEntity lockedTarget = null;
            if (useFrozen) {
                lookVec = hostGroup.frozenLook();
            } else if (domainAutomatic) {
                lockedTarget = automaticContext.targetCheck().stillValid(automaticContext.localTargetId());
                if (lockedTarget == null) {
                    lookVec = automaticContext.baseDirection();
                }
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
                    Vec3 fromPoint = domainAutomatic
                            ? automaticContext.origin()
                            : (contactOrigin != null ? contactOrigin : player.getEyePosition());
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
            if (domainAutomatic) {
                center = automaticContext.origin();
            } else if (mutable && selectionBatch != null && (spaceLock || pathOverride == Trajectory.VORTEX)) {
                center = selectionBatch.crosshairCenter(player);
            } else if (mutable && scatter && !entityLock) {
                center = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
                groundLevel = player.getY();
            } else if (spaceLock && selectionBatch != null) {
                center = selectionBatch.resolveAnchor(player);
            } else {
                center = player.getEyePosition();
            }
            long seed = domainAutomatic
                    ? automaticContext.roundSeed() ^ ((long) volley << 16)
                    : player.getUUID().getLeastSignificantBits()
                    ^ player.level().getGameTime()
                    ^ action.getId().hashCode()
                    ^ ((long) volley << 16);
            int geometryIndex = copyIndex;
            int geometryCount = copyCount;
            com.bslx.XueXiMod.shu.technique.release.batch.BirthLayout.Slot birthSlot =
                    com.bslx.XueXiMod.shu.technique.release.batch.BirthLayout.current();
            if (birthSlot != null) {
                geometryIndex = birthSlot.copyOrdinal;
                geometryCount = Math.max(1, birthSlot.spreadCount);
                if (domainAutomatic) {
                    com.bslx.XueXiMod.shu.technique.release.domain.DomainSpawnInheritance.install(
                            birthSlot, automaticContext, action);
                }
            }
            Vec3 castLook = lookVec;
            if (mutable && pathOverride == Trajectory.PARALLEL) {
                castLook = horizontalCastHeading(lookVec, player.getYRot());
            }
            CastFrame frame = CastFrame.forAttack(usedDamage, applied, tally, geometryIndex, geometryCount,
                    center, castLook, spaceLock, scatter, entityLock, pathOverride, mutable, seed, groundLevel);
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
            if (domainAutomatic && influence.pullAnchor == null) {
                frame.pullAnchor = automaticContext.origin();
            } else {
                frame.pullAnchor = influence.pullAnchor != null ? influence.pullAnchor : player.position();
            }
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
            if (domainAutomatic) {
                applyDomainFrame(frame, automaticContext, lookVec);
                int effectTick = player.getServer() == null ? 0 : player.getServer().getTickCount();
                LifetimeToken effectToken = automaticContext.token();
                if (effectToken == null || !effectToken.valid(effectTick)) {
                    return;
                }
            }
            if (frame.parallel && mutable) {
                frame.look = castLook;
                frame.planeNormal = castLook;
            }
            String batchMark = settlementBatch == null
                    ? "loose-" + player.serverLevel().getGameTime()
                    : Integer.toUnsignedString(System.identityHashCode(settlementBatch));
            PartingOffers.present(PartingOperationId.derive(player.getUUID(), batchMark, action.getId(), actionIndex, volley));
            ReleaseBatch.bindActiveOffer(settlementBatch == null ? null : settlementBatch.explicitOffer(actionIndex));
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
                ReleaseBatch.bindActiveOffer(null);
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
            if (settlementBatch != null) {
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

    /**
     * 领域两条开域路径共用的空间身份。锁定实体不挪原点，垂落不放倒竖轴。
     * 没有施法者时抛出状态异常；有成员视图时使用视图中的角色，否则现场扫描。
     */
    static DomainCastContext domainSpace(ServerPlayer player, List<Technique> techniques, int actionIndex,
                                         UUID groupId, boolean explicitContact, Vec3 contactPoint,
                                         MemberCastView view) {
        if (player == null) {
            throw new IllegalStateException("领域释放缺少施法者");
        }
        RoleScan roles = view == null
                ? scanRoles(techniques, actionIndex)
                : new RoleScan(view.spaceLock, view.entityLock, view.scatter, view.tracking, view.pathOverride);
        boolean dropPath = view != null ? view.drop : roles.pathOverride == Trajectory.DROP;
        return new DomainCastContext(groupId, actionIndex, roles.spaceLock, roles.scatter, roles.entityLock,
                dropPath, explicitContact, contactPoint);
    }

    /**
     * 向前扫描落点、锁定、交错、跟随和路径覆盖。遇到另一段正伤害攻击即停止，同一次多重成员除外。
     */
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
                if (SkillSegmentMath.isOperationHost(earlier)
                        || earlier.getBaseDamage() > 0 && !earlier.isEnchantment()) {
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
                    case TRAJECTORY_DROP, TRAJECTORY_PARALLEL -> {
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

    /** 选择角色扫描结果，依次使用线程成员视图、计划缓存和现场扫描。 */
    private static RoleScan resolveRoles(List<Technique> techniques, int actionIndex, CastPlanContext planContext) {
        MemberCastView boundView = boundMemberView.get();
        if (boundView != null) {
            return new RoleScan(boundView.spaceLock, boundView.entityLock, boundView.scatter,
                    boundView.tracking, boundView.pathOverride);
        }
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

    /** 在术式和场地给出的整数授予中取较大值。 */
    private static int pickedGrant(int fromDefinition, int fromField) {
        return fromDefinition >= fromField ? fromDefinition : fromField;
    }

    /** 只返回当前处于领域自动模式的执行上下文，其他模式视为没有。 */
    private static ReleaseExecutionContext domainContext() {
        ReleaseExecutionContext currentContext = ReleaseExecutionContext.current();
        if (currentContext == null || currentContext.mode() != ReleaseInputMode.DOMAIN_AUTOMATIC) {
            return null;
        }
        return currentContext;
    }

    /** 判断领域自动释放当前是否因发布许可或生命令牌失效而被挡住。非自动模式不挡。 */
    private static boolean automaticBlocked(ServerPlayer player) {
        ReleaseExecutionContext currentContext = domainContext();
        if (currentContext == null) {
            return false;
        }
        PublicationAttempt attempt = PublicationAttempt.current();
        if (attempt != null && !attempt.allowsPublication()) {
            return true;
        }
        int nowTick = player == null || player.getServer() == null ? 0 : player.getServer().getTickCount();
        LifetimeToken token = currentContext.token();
        return token == null || !token.valid(nowTick);
    }

    /**
     * 规则几何已经写完后，把发射源收回本轮体积。缺原点时垂落只抬一次。
     * 这里的正下方只是生成前的占位，每一发的斜向在生成口覆盖它。
     */
    private static Vec3 horizontalCastHeading(Vec3 look, float yawDegrees) {
        if (look != null) {
            double horizontalLength = Math.sqrt(look.x * look.x + look.z * look.z);
            if (horizontalLength >= 1.0E-4) {
                return new Vec3(look.x / horizontalLength, 0.0, look.z / horizontalLength);
            }
        }
        double yawRadians = Math.toRadians(yawDegrees);
        return new Vec3(-Math.sin(yawRadians), 0.0, Math.cos(yawRadians));
    }

    private static void applyDomainFrame(CastFrame frame, ReleaseExecutionContext automaticContext, Vec3 lookVec) {
        frame.execution = automaticContext;
        boolean missingOrigin = frame.origin == null;
        Vec3 sourceOrigin = missingOrigin ? automaticContext.origin() : frame.origin;
        if (missingOrigin && frame.drop) {
            sourceOrigin = sourceOrigin.add(0.0, 8.0, 0.0);
        }
        Vec3 sourceAnchor = frame.anchor == null ? automaticContext.origin() : frame.anchor;
        frame.origin = automaticContext.volume().clampPoint(sourceOrigin);
        frame.anchor = automaticContext.volume().clampPoint(sourceAnchor);
        frame.overrideGeometry = true;
        if (frame.drop) {
            frame.look = new Vec3(0.0, -1.0, 0.0);
        } else if (frame.look == null) {
            frame.look = lookVec == null ? automaticContext.baseDirection() : lookVec;
        }
    }
}
