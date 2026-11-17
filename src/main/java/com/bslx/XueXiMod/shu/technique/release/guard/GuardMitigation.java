package com.bslx.XueXiMod.shu.technique.release.guard;

import com.bslx.XueXiMod.debug.DebugLog;
import com.bslx.XueXiMod.shu.technique.release.deflection.MitigationCreditScope;
import com.bslx.XueXiMod.shu.technique.release.momentum.MomentumCreditPolicy;
import com.bslx.XueXiMod.shu.technique.release.momentum.WaterMomentum;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.ArrayList;
import java.util.UUID;

/**
 * 只服务护体这次卸力。场地卸力仍在受伤当下入账。
 * 同一条受伤事件确认没被取消、数额仍为正，才记水势并播受击姿态。
 * 这是暂存结算服务；领域不记卸力水势，球形场地不延迟入账。
 */
public final class GuardMitigation {
    private static final int CAPACITY = 64;
    private static final ArrayList<Note> NOTES = new ArrayList<>();
    private static long lastOverflowLog;

    private GuardMitigation() {
    }

    /** 暂存一笔护体卸力。事件或玩家为空时忽略；记录满了只丢弃新笔。 */
    public static void offer(LivingHurtEvent event, ServerPlayer player, float credit, int poseKind, Vec3 incoming) {
        if (event == null || player == null) {
            return;
        }
        if (NOTES.size() >= CAPACITY) {
            long now = System.currentTimeMillis();
            if (now - lastOverflowLog > 5000L) {
                lastOverflowLog = now;
                DebugLog.log("护体卸力记录已满，这笔水势不发");
            }
            return;
        }
        Note note = new Note();
        note.event = event;
        note.playerId = player.getUUID();
        note.dimension = player.level().dimension();
        note.credit = credit;
        boolean drowningSuppress = MomentumCreditPolicy.suppresses(event.getEntity(), event.getSource());
        boolean partialSuppress = MitigationCreditScope.suppressesMitigated(event.getEntity(), event.getSource());
        note.suppressCredit = drowningSuppress || partialSuppress;
        note.deflectionSuppress = partialSuppress;
        note.poseKind = poseKind;
        note.incoming = incoming == null ? Vec3.ZERO : incoming;
        note.hurtAmount = event.getAmount();
        note.order = NOTES.size();
        NOTES.add(note);
    }

    /** 确认并结算暂存卸力。事件被取消、数额非正或玩家失效时不记水势。 */
    public static void flush() {
        if (NOTES.isEmpty()) {
            return;
        }
        ArrayList<Note> batch = new ArrayList<>(NOTES);
        NOTES.clear();
        for (int playerSlot = 0; playerSlot < batch.size(); playerSlot++) {
            Note first = batch.get(playerSlot);
            if (first.consumed) {
                continue;
            }
            Note chosenPose = null;
            for (Note note : batch) {
                if (!note.playerId.equals(first.playerId) || note.consumed) {
                    continue;
                }
                note.consumed = true;
                note.event = confirm(note) ? note.event : null;
                if (note.event == null) {
                    continue;
                }
                if (MitigationCreditScope.allowsMitigatedCredit(note.credit, note.suppressCredit, note.deflectionSuppress)) {
                    ServerPlayer player = note.event.getEntity() instanceof ServerPlayer owner ? owner : null;
                    if (player != null) {
                        WaterMomentum.creditMitigated(player, note.credit);
                    }
                }
                chosenPose = prefer(chosenPose, note);
            }
            if (chosenPose != null && chosenPose.event.getEntity() instanceof ServerPlayer owner) {
                GuardStates.strike(owner, chosenPose.poseKind, chosenPose.incoming);
            }
        }
        for (Note note : batch) {
            note.event = null;
        }
    }

    /** 清空未结算记录并松开事件引用。不补记水势。 */
    public static void clear() {
        for (Note note : NOTES) {
            note.event = null;
        }
        NOTES.clear();
    }

    private static boolean confirm(Note note) {
        LivingHurtEvent event = note.event;
        if (event == null || event.isCanceled() || !Float.isFinite(event.getAmount()) || event.getAmount() <= 0f) {
            return false;
        }
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isRemoved()) {
            return false;
        }
        if (!player.getUUID().equals(note.playerId) || player.level().dimension() != note.dimension) {
            return false;
        }
        return true;
    }

    /** 同一次结算里，近战压过箭；多支箭取卸力更大的一发。 */
    private static Note prefer(Note current, Note candidate) {
        if (candidate.poseKind == GuardPose.NONE) {
            return current;
        }
        if (current == null) {
            return candidate;
        }
        if (candidate.poseKind == GuardPose.MELEE && current.poseKind != GuardPose.MELEE) {
            return candidate;
        }
        if (current.poseKind == GuardPose.MELEE && candidate.poseKind != GuardPose.MELEE) {
            return current;
        }
        if (candidate.hurtAmount > current.hurtAmount + 0.001f) {
            return candidate;
        }
        if (current.hurtAmount > candidate.hurtAmount + 0.001f) {
            return current;
        }
        return candidate.order < current.order ? current : candidate;
    }

    private static final class Note {
        private LivingHurtEvent event;
        private UUID playerId;
        private ResourceKey<Level> dimension;
        private float credit;
        private int poseKind;
        private Vec3 incoming = Vec3.ZERO;
        private float hurtAmount;
        private int order;
        private boolean consumed;
        private boolean suppressCredit;
        private boolean deflectionSuppress;
    }
}
