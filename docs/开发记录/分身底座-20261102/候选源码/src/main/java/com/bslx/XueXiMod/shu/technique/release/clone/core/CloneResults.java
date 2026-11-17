package com.bslx.XueXiMod.shu.technique.release.clone.core;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class CloneResults {
    public static final int REPEAT_CHARGES = -1;

    private CloneResults() {
    }

    public enum RejectReason {
        WRONG_OWNER,
        WRONG_SERVER,
        WRONG_DIMENSION,
        ACTOR_GONE,
        OWNER_UNAVAILABLE,
        BUSY,
        COOLDOWN,
        BUDGET_SPENT,
        QUEUE_FULL,
        CAP_REACHED,
        NO_TARGET,
        BLOCKED,
        FORBIDDEN_TARGET,
        INSUFFICIENT_MANA,
        UNSUPPORTED_ACTION,
        UNSUPPORTED_CHAIN,
        DEVELOPMENT_ENTRY_ONLY,
        INVALID_GEOMETRY,
        INVALID_QUOTE
    }

    public record SpawnResult(
            boolean accepted,
            @Nullable RejectReason reason,
            String detail,
            @Nullable CloneHandle handle,
            @Nullable CloneKind kind,
            int chargedMana,
            int responsesRemaining,
            int cooldownTicksRemaining) {
    }

    public record RequestResult(
            boolean accepted,
            @Nullable RejectReason reason,
            String detail,
            boolean scheduled) {
    }

    public record Status(
            UUID actorId,
            CloneKind kind,
            CloneLifeState life,
            CloneResponseState response,
            Vec3 actorPosition,
            double healthNow,
            double bodyHealth,
            int responsesRemaining,
            int cooldownTicksRemaining) {

        public String commandLine() {
            String charges = this.responsesRemaining == REPEAT_CHARGES
                    ? "repeat"
                    : Integer.toString(this.responsesRemaining);
            return "uuid=" + this.actorId
                    + " pos=" + this.actorPosition.x + " " + this.actorPosition.y + " " + this.actorPosition.z
                    + " health=" + this.healthNow + "/" + this.bodyHealth
                    + " kind=" + this.kind
                    + " life=" + this.life
                    + " response=" + this.response
                    + " charges=" + charges
                    + " cooldown=" + this.cooldownTicksRemaining;
        }
    }

    public static String text(RejectReason reason) {
        return switch (reason) {
            case WRONG_OWNER -> "这具分身不属于你";
            case WRONG_SERVER -> "不在这台服务器上，没有处理";
            case WRONG_DIMENSION -> "你和分身不在同一维度";
            case ACTOR_GONE -> "这具分身已经不在";
            case OWNER_UNAVAILABLE -> "主人不在线或已经死亡";
            case BUSY -> "这具分身已有一次尚未完成的响应";
            case COOLDOWN -> "冷却还没有结束";
            case BUDGET_SPENT -> "幻影的一次响应已经用掉";
            case QUEUE_FULL -> "服务器响应队列已满";
            case CAP_REACHED -> "分身数量已经到达上限";
            case NO_TARGET -> "这个方向上没有可攻击的目标";
            case BLOCKED -> "目标和分身之间被挡住了";
            case FORBIDDEN_TARGET -> "这个目标不能由分身攻击";
            case INSUFFICIENT_MANA -> "魔力不足";
            case UNSUPPORTED_ACTION -> "这个动作没有登记";
            case UNSUPPORTED_CHAIN -> "这个动作不能由分身释放";
            case DEVELOPMENT_ENTRY_ONLY -> "该术式只属于开发入口";
            case INVALID_GEOMETRY -> "这个位置或方向不能使用";
            case INVALID_QUOTE -> "这次的费用或伤害不在可用范围内";
        };
    }

    public static SpawnResult spawnRejected(RejectReason reason, String detail) {
        return new SpawnResult(false, reason, detail, null, null, 0, 0, 0);
    }

    public static SpawnResult spawnRejected(RejectReason reason) {
        return spawnRejected(reason, text(reason));
    }

    public static RequestResult requestRejected(RejectReason reason, String detail) {
        return new RequestResult(false, reason, detail, false);
    }

    public static RequestResult requestRejected(RejectReason reason) {
        return requestRejected(reason, text(reason));
    }

    public static RequestResult requestAccepted() {
        return new RequestResult(true, null, "请求已接受", true);
    }
}
