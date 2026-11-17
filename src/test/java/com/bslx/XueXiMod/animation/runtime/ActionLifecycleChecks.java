package com.bslx.XueXiMod.animation.runtime;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bslx.XueXiMod.animation.core.AnimationClip;
import com.bslx.XueXiMod.animation.core.JointTransform;
import com.bslx.XueXiMod.animation.core.Keyframe;
import com.bslx.XueXiMod.animation.core.TransformSheet;
import com.bslx.XueXiMod.network.ActionSyncS2CPacket;

import net.minecraft.resources.ResourceLocation;

/** 调用生产准入、许可和防重账本。无头可跑。 */
public final class ActionLifecycleChecks {
    private static int checks;

    private ActionLifecycleChecks() {
    }

    public static void main(String[] programArguments) {
        GroupReplayLedger.clearAll();
        try {
            checkCursorGate();
            checkPermitMismatchLeavesPermit();
            checkFallbackSelectsOnce();
            checkThousandGroupsStayBounded();
            checkSpentPermitAfterEviction();
            checkTerminalRecordStays();
            System.out.println("ActionLifecycleChecks: " + checks + " assertions");
        } finally {
            GroupReplayLedger.clearAll();
        }
    }

    private static void checkCursorGate() {
        Object firstLink = new Object();
        Object secondLink = new Object();
        Object firstLevel = new Object();
        Object secondLevel = new Object();
        yes(!ActionPlaybackGate.sameLink(secondLink, firstLink), "old connection enqueue is discarded");
        yes(ActionPlaybackGate.sameLink(secondLink, secondLink), "live connection matches itself");
        yes(ActionPlaybackGate.replaceWorld(firstLevel, secondLevel, firstLink, secondLink), "new level instance replaces");
        yes(!ActionPlaybackGate.replaceWorld(secondLevel, secondLevel, secondLink, secondLink), "same level stays");
        yes(ActionPlaybackGate.admit(false, 9L, 40L, 4, true, false, 1L, 1L, 1, ActionSyncS2CPacket.START),
                "new connection allows epoch 1 sequence 1");
        yes(!ActionPlaybackGate.admit(true, 4L, 8L, 3, true, false, 4L, 8L, 4, ActionSyncS2CPacket.START),
                "same connection terminal rejects old start");
        yes(!ActionPlaybackGate.admit(true, 4L, 8L, 3, true, false, 4L, 7L, 1, ActionSyncS2CPacket.RESUME),
                "same connection rejects older sequence");
        yes(!ActionPlaybackGate.admit(true, 4L, 8L, 3, true, false, 4L, 8L, 4, ActionSyncS2CPacket.RESUME),
                "terminal session rejects resume");
        UUID ownerId = UUID.randomUUID();
        yes(ActionPlaybackGate.entityApplies(ownerId, 12, ownerId, 12, true), "matching entity applies");
        yes(!ActionPlaybackGate.entityApplies(ownerId, 12, ownerId, 19, true), "same uuid new entity id does not apply");
    }

    private static void checkPermitMismatchLeavesPermit() {
        UUID ownerId = UUID.randomUUID();
        UUID otherOwner = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID otherGroup = UUID.randomUUID();
        ResourceLocation actionId = ResourceLocation.fromNamespaceAndPath("xuexicheck", "mismatch");
        CommitPermit permit = new CommitPermit(ownerId, groupId);
        ActionCommit commit = ActionCommit.group(groupId, actionId);
        ActionRuntime.PermitPreparation wrongOwner = ActionRuntime.preparePermit(otherOwner, permit, commit, 0L);
        yes(wrongOwner.outcome() == CommitAuthorization.Outcome.MISMATCH, "owner mismatch");
        yes(!permit.consumed(), "owner mismatch does not consume");
        ActionCommit wrongGroup = ActionCommit.group(otherGroup, actionId);
        ActionRuntime.PermitPreparation wrongGroupResult = ActionRuntime.preparePermit(ownerId, permit, wrongGroup, 0L);
        yes(wrongGroupResult.outcome() == CommitAuthorization.Outcome.MISMATCH, "group mismatch");
        yes(!permit.consumed(), "group mismatch does not consume");
        ActionRuntime.PermitPreparation ready = ActionRuntime.preparePermit(ownerId, permit, commit, 0L);
        yes(ready.outcome() == CommitAuthorization.Outcome.READY, "matching permit becomes ready");
        yes(permit.consumed(), "matching permit consumes once");
    }

    private static void checkFallbackSelectsOnce() {
        ResourceLocation requestedId = ResourceLocation.fromNamespaceAndPath("xuexicheck", "dev_missing");
        ResourceLocation fallbackId = ResourceLocation.fromNamespaceAndPath("xuexicheck", "fallback_pose");
        registerPlayable(fallbackId);
        registerBlocked(requestedId);
        UUID ownerId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        CommitPermit permit = new CommitPermit(ownerId, groupId);
        ActionCommit commit = ActionCommit.group(groupId, requestedId).withFallback(fallbackId);
        yes(ActionRuntime.selectPlayable(commit).equals(fallbackId), "missing dev selects fallback");
        yes(ActionRuntime.selectPlayable(commit).equals(fallbackId), "second select stays on the same fallback");
        ActionRuntime.PermitPreparation first = ActionRuntime.preparePermit(ownerId, permit, commit, 10L);
        yes(first.outcome() == CommitAuthorization.Outcome.READY, "fallback preparation ready");
        yes(fallbackId.equals(first.actionId()), "preparation plays fallback once");
        ActionRuntime.PermitPreparation second = ActionRuntime.preparePermit(ownerId, permit, commit, 10L);
        yes(second.actionId() == null, "consumed permit does not play again");
        yes(second.outcome() == CommitAuthorization.Outcome.DUPLICATE || second.outcome() == CommitAuthorization.Outcome.SPENT,
                "second preparation is not another play");
        registerPlayable(requestedId);
        yes(requestedId.equals(ActionRuntime.selectPlayable(commit)), "playable dev is the only selection");
    }

    private static void checkThousandGroupsStayBounded() {
        UUID ownerId = UUID.randomUUID();
        for (int groupIndex = 0; groupIndex < 1000; groupIndex++) {
            GroupReplayLedger.remember(ownerId, UUID.randomUUID(), 0L, false);
        }
        yes(GroupReplayLedger.OWNER_LIMIT <= 256, "owner cap is 256");
        yes(GroupReplayLedger.retained(ownerId) <= 256, "one thousand groups stay within 256");
        yes(GroupReplayLedger.retained(ownerId) == GroupReplayLedger.OWNER_LIMIT, "cap is filled");
    }

    private static void checkSpentPermitAfterEviction() {
        UUID ownerId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        ResourceLocation actionId = ResourceLocation.fromNamespaceAndPath("xuexicheck", "evicted");
        CommitPermit permit = new CommitPermit(ownerId, groupId);
        ActionCommit commit = ActionCommit.group(groupId, actionId);
        ActionRuntime.PermitPreparation first = ActionRuntime.preparePermit(ownerId, permit, commit, 0L);
        yes(first.outcome() == CommitAuthorization.Outcome.READY, "first permit ready");
        yes(permit.consumed(), "first permit consumed");
        GroupReplayLedger.expireAll(GroupReplayLedger.RETAIN_TICKS);
        yes(!GroupReplayLedger.covers(ownerId, groupId, GroupReplayLedger.RETAIN_TICKS), "record expired");
        ActionRuntime.PermitPreparation second = ActionRuntime.preparePermit(ownerId, permit, commit, GroupReplayLedger.RETAIN_TICKS);
        yes(second.outcome() == CommitAuthorization.Outcome.SPENT, "evicted permit still rejected");
        yes(second.actionId() == null, "evicted permit does not select a play");
    }

    private static void checkTerminalRecordStays() {
        UUID ownerId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        GroupReplayLedger.remember(ownerId, groupId, 0L, false);
        GroupReplayLedger.markTerminal(ownerId, groupId);
        yes(GroupReplayLedger.retained(ownerId) == 1, "terminal record is kept");
        yes(GroupReplayLedger.covers(ownerId, groupId, 1L), "terminal record still covers the group");
        GroupReplayLedger.clearOwner(ownerId);
        yes(GroupReplayLedger.retained(ownerId) == 0, "owner cleanup removes records");
    }

    private static void registerPlayable(ResourceLocation actionId) {
        AnimationClip clip = new AnimationClip();
        clip.setClipTime(0.4F);
        clip.addJointTransform("Root", new TransformSheet(List.of(
                new Keyframe(0.0F, JointTransform.empty()),
                new Keyframe(0.4F, JointTransform.empty()))));
        ActionDefinition definition = new ActionDefinition(
                actionId,
                "xueximod:target/humanoid",
                0.4F,
                false,
                0.0F,
                Set.of(),
                EnumSet.allOf(ActionDefinition.Channel.class),
                List.of(),
                "lifecycle check");
        ActionRegistry.registerPlayable(definition, clip, status(actionId, ActionStatus.Availability.ORIGINAL));
    }

    private static void registerBlocked(ResourceLocation actionId) {
        ActionRegistry.registerBlocked(status(actionId, ActionStatus.Availability.REJECTED));
    }

    private static ActionStatus status(ResourceLocation actionId, ActionStatus.Availability availability) {
        return new ActionStatus(actionId, availability, "lifecycle check", "xueximod:target/humanoid", "", "", "", "", 0.4F, "", "", false);
    }

    private static void yes(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
