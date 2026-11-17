package com.bslx.XueXiMod.shu.technique.release.selfstatus;

import com.bslx.XueXiMod.shu.technique.release.fire.FireVisualRelay;
import com.bslx.XueXiMod.status.fire.furnace.FurnaceChange;
import com.bslx.XueXiMod.status.fire.furnace.FurnaceMath;
import com.bslx.XueXiMod.status.fire.furnace.FurnaceService;
import com.bslx.XueXiMod.visual.fire.FireVisualLayout;
import net.minecraft.server.level.ServerPlayer;

/**
 * 炉心三式的自身状态适配。只调用原来的预览和三个服务动作。
 * 热量、蒸汽、被动结束和效果规范化仍在炉心服务里，这里不改那些规则。
 */
public final class FurnaceSelfStatusCast implements SelfStatusCast {
    public static final String UNCHANGED_TEXT = "§c炉心状态没有改变";

    private final FurnaceMath.Action furnaceAction;

    private FurnaceSelfStatusCast(FurnaceMath.Action furnaceAction) {
        this.furnaceAction = furnaceAction;
    }

    public static FurnaceSelfStatusCast enterNormal() {
        return new FurnaceSelfStatusCast(FurnaceMath.Action.ENTER_NORMAL);
    }

    public static FurnaceSelfStatusCast enterOverdrive() {
        return new FurnaceSelfStatusCast(FurnaceMath.Action.ENTER_OVERDRIVE);
    }

    public static FurnaceSelfStatusCast cool() {
        return new FurnaceSelfStatusCast(FurnaceMath.Action.COOL);
    }

    public FurnaceMath.Action furnaceAction() {
        return this.furnaceAction;
    }

    @Override
    public FurnaceMath.Action action() {
        return this.furnaceAction;
    }

    @Override
    public StatusPreparation prepare(ServerPlayer player) {
        if (player == null || this.furnaceAction == null || this.furnaceAction == FurnaceMath.Action.END) {
            return StatusPreparation.reject(UNCHANGED_TEXT);
        }
        FurnaceChange preview = FurnaceService.preview(player, this.furnaceAction);
        FurnaceMath.Outcome outcome = preview == null ? null : preview.outcome();
        if (outcome != null && outcome.succeeded()) {
            return StatusPreparation.ready(new FurnacePrepared(this.furnaceAction));
        }
        if (currentStateInapplicable(outcome)) {
            return StatusPreparation.notApplicable(UNCHANGED_TEXT);
        }
        return StatusPreparation.reject(UNCHANGED_TEXT);
    }

    /** 只认这一次预览的四个状态结果。空结果、闸门拒绝和安装失败都不在内。 */
    private static boolean currentStateInapplicable(FurnaceMath.Outcome outcome) {
        return outcome == FurnaceMath.Outcome.REJECT_INACTIVE
                || outcome == FurnaceMath.Outcome.REJECT_ALREADY_NORMAL
                || outcome == FurnaceMath.Outcome.REJECT_ALREADY_OVERDRIVE
                || outcome == FurnaceMath.Outcome.REJECT_CANNOT_DOWNGRADE;
    }

    private static String previewRejection(ServerPlayer player, FurnaceMath.Action furnaceAction) {
        if (player == null || furnaceAction == null || furnaceAction == FurnaceMath.Action.END) {
            return UNCHANGED_TEXT;
        }
        FurnaceChange preview = FurnaceService.preview(player, furnaceAction);
        if (preview == null || !preview.succeeded()) {
            return UNCHANGED_TEXT;
        }
        return null;
    }

    private static FurnaceChange install(ServerPlayer player, FurnaceMath.Action furnaceAction) {
        return switch (furnaceAction) {
            case ENTER_NORMAL -> FurnaceService.enterNormal(player);
            case ENTER_OVERDRIVE -> FurnaceService.enterOverdrive(player);
            case COOL -> FurnaceService.cool(player);
            case END -> new FurnaceChange(FurnaceMath.Outcome.REJECT_BLOCKED);
        };
    }

    private static final class FurnacePrepared implements PreparedStatusChange {
        private final FurnaceMath.Action furnaceAction;
        private FurnaceMath.Outcome installedOutcome;

        private FurnacePrepared(FurnaceMath.Action furnaceAction) {
            this.furnaceAction = furnaceAction;
        }

        @Override
        public String recheck(ServerPlayer player) {
            return previewRejection(player, this.furnaceAction);
        }

        @Override
        public boolean commit(ServerPlayer player) {
            FurnaceChange committed = install(player, this.furnaceAction);
            if (committed == null || !committed.succeeded()) {
                return false;
            }
            this.installedOutcome = committed.outcome();
            return true;
        }

        @Override
        public void afterCommit(ServerPlayer player) {
            if (this.installedOutcome == null) {
                return;
            }
            int furnaceCue = switch (this.installedOutcome) {
                case ENTERED_NORMAL -> FireVisualLayout.HEAT_LOOP;
                case ENTERED_OVERDRIVE, UPGRADED -> FireVisualLayout.OVERDRIVE_LOOP;
                case COOLED -> FireVisualLayout.COOL_ONCE;
                default -> 0;
            };
            if (furnaceCue != 0) {
                FireVisualRelay.furnace(player, furnaceCue);
            }
        }

        @Override
        public String failureText() {
            return UNCHANGED_TEXT;
        }
    }
}
