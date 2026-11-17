package com.bslx.XueXiMod.shu.technique.release.water;

import com.bslx.XueXiMod.animation.runtime.ActionCommit;
import com.bslx.XueXiMod.animation.runtime.ActionDefinition;
import com.bslx.XueXiMod.animation.runtime.ActionRegistry;
import com.bslx.XueXiMod.animation.runtime.ActionRuntime;
import com.bslx.XueXiMod.animation.runtime.ActionVisualFallback;
import com.bslx.XueXiMod.animation.runtime.CommitPermit;
import com.bslx.XueXiMod.animation.runtime.WaterBladeActionMap;
import com.bslx.XueXiMod.debug.DebugLog;
import com.bslx.XueXiMod.shu.technique.release.entity.HostBeatHooks;
import com.bslx.XueXiMod.shu.technique.release.entity.HostCast;
import com.bslx.XueXiMod.shu.technique.release.entity.HostGroupContext;
import com.bslx.XueXiMod.shu.technique.release.entity.HostGroupReceipt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** 水刃拍号和成功宿主组的动作提交。失败只丢掉候选，不刷新窗口，也不回退已提交的攻击。 */
public final class WaterBeatBridge {
    private static final HostBeatHooks.Cycle CYCLE = new HostBeatHooks.Cycle() {
        @Override
        public void beforeCopies(ServerPlayer player, HostGroupContext context) {
            prepare(player, context);
        }

        @Override
        public void beforeActivation(ServerPlayer player, HostGroupContext context) {
            rejectIfCarrierMoved(player, context);
        }

        @Override
        public void onCommitted(ServerPlayer player, HostGroupContext context, HostGroupReceipt receipt) {
            publish(player, context, receipt);
        }

        @Override
        public void onAbandoned(HostGroupContext context) {
            drop(context);
        }
    };

    private WaterBeatBridge() {
    }

    public static void install() {
        HostBeatHooks.install(CYCLE);
    }

    public static CastSource capture(ServerPlayer player, int skillSlot) {
        if (player == null) {
            return null;
        }
        ItemStack stack = player.getMainHandItem();
        int selectedSlot = player.getInventory().selected;
        int generation = CarrierWatch.observe(player.getUUID(), stack, selectedSlot);
        return new CastSource(skillSlot, stack, selectedSlot, generation);
    }

    private static void prepare(ServerPlayer player, HostGroupContext context) {
        if (player == null || context == null || context.hasBeatCandidate()) {
            return;
        }
        if (!carrierMatches(player, context)) {
            context.fail();
            return;
        }
        CastSource source = context.hostCast().source();
        long gameTime = player.serverLevel().getGameTime();
        int beat = WaterBeatLedger.prepareHeld(player.getUUID(), source, context.groupId(), gameTime);
        context.armBeat(beat);
    }

    private static void rejectIfCarrierMoved(ServerPlayer player, HostGroupContext context) {
        if (player == null || context == null || !context.hasBeatCandidate()) {
            return;
        }
        if (!carrierMatches(player, context)) {
            context.fail();
        }
    }

    private static boolean carrierMatches(ServerPlayer player, HostGroupContext context) {
        HostCast hostCast = context.hostCast();
        CastSource source = hostCast == null ? null : hostCast.source();
        CarrierWatch.observe(player.getUUID(), player.getMainHandItem(), player.getInventory().selected);
        return CarrierWatch.allows(player.getUUID(), source);
    }

    private static void publish(ServerPlayer player, HostGroupContext context, HostGroupReceipt receipt) {
        if (player == null || context == null || receipt == null || !context.hasBeatCandidate() || !receipt.committed()) {
            return;
        }
        long gameTime = player.serverLevel().getGameTime();
        if (!WaterBeatLedger.commitHeld(context.groupId(), gameTime)) {
            return;
        }
        ArcBandSnapshot snapshot = context.committedArc();
        if (snapshot == null) {
            return;
        }
        try {
            ActionCommit commit = framedCommit(receipt.groupId(), context.beatCandidate(), snapshot);
            if (commit == null) {
                return;
            }
            CommitPermit permit = receipt.commitPermit(player.getUUID());
            if (permit == null) {
                return;
            }
            ActionRuntime.notifyAttackGroup(player, permit, commit);
        } catch (RuntimeException visualFailure) {
            DebugLog.log("水刃姿态没有播成，攻击和拍号保持提交: " + visualFailure.getClass().getSimpleName());
        }
    }

    private static void drop(HostGroupContext context) {
        if (context == null || !context.hasBeatCandidate()) {
            return;
        }
        WaterBeatLedger.abandonHeld(context.groupId());
        context.clearBeat();
    }

    private static ActionCommit framedCommit(UUID groupId, int beatIndex, ArcBandSnapshot snapshot) {
        WaterBeatFrame copied = WaterBeatFrame.copy(snapshot);
        if (copied == null || groupId == null) {
            return null;
        }
        WaterBladeActionMap.Resolution resolution = WaterBladeActionMap.resolve(beatIndex);
        ResourceLocation actionId = resolution.actionId();
        ResourceLocation fallbackId = ActionVisualFallback.fallbackFor(actionId);
        float duration = authoritativeSeconds(actionId, fallbackId, beatIndex);
        return ActionCommit.framed(groupId, actionId, resolution.beatIndex(),
                (float) copied.forwardEast(), (float) copied.forwardAbove(), (float) copied.forwardSouth(),
                (float) copied.normalEast(), (float) copied.normalAbove(), (float) copied.normalSouth())
                .withFallback(fallbackId)
                .withAuthoritativeDuration(duration);
    }

    private static float authoritativeSeconds(ResourceLocation actionId, ResourceLocation fallbackId, int beatIndex) {
        ActionDefinition played = ActionRegistry.definition(actionId);
        if (played == null) {
            played = ActionRegistry.definition(fallbackId);
        }
        if (played != null && Float.isFinite(played.durationSeconds()) && played.durationSeconds() > 0.0F) {
            return played.durationSeconds();
        }
        return Math.floorMod(beatIndex, 3) == 2 ? 0.48F : 0.42F;
    }
}
