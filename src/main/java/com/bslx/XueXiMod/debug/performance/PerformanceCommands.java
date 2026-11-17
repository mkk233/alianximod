package com.bslx.XueXiMod.debug.performance;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * 服务端 /shuperf。权限等级 2。没有客户端命令组件。
 */
public final class PerformanceCommands {
    private PerformanceCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shuperf")
                .requires(commandSource -> commandSource.hasPermission(2))
                .then(Commands.literal("start")
                        .executes(context -> start(context.getSource(), PerformanceRecorder.DEFAULT_TARGET_TICKS))
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1, PerformanceRecorder.CAPACITY_TICKS))
                                .executes(context -> start(context.getSource(), IntegerArgumentType.getInteger(context, "ticks")))))
                .then(Commands.literal("stop")
                        .executes(context -> stop(context.getSource())))
                .then(Commands.literal("status")
                        .executes(context -> status(context.getSource())))
                .then(Commands.literal("export")
                        .executes(context -> export(context.getSource()))));
    }

    private static int start(CommandSourceStack commandSource, int targetTicks) {
        PerformanceRecorder.CommandResult result = PerformanceRecorder.commandStart(commandSource.getServer(), targetTicks);
        return switch (result.outcome) {
            case STARTED -> succeed(commandSource, "性能记录已待命。目标 " + targetTicks
                    + " 个完整 tick。从下一个服务端 tick 的 START 开始，当前这段不计入。");
            case STARTED_REPLACED -> succeed(commandSource, replacedStartText(targetTicks, result.detail));
            case REJECTED_BUSY -> fail(commandSource, "正在待命、录制或等待停止，已拒绝开始。已有数据未清空。");
            case REJECTED_UNBOUND -> fail(commandSource, "性能记录没有绑定这台服务器，已拒绝。");
            case REJECTED_SERVER_EXITED -> fail(commandSource, "服务器已退出或正在停止，已拒绝。");
            case REJECTED_RANGE -> fail(commandSource, "tick 数必须在 1 到 " + PerformanceRecorder.CAPACITY_TICKS + " 之间。");
            case REJECTED_MEMORY -> fail(commandSource, "无法分配记录缓冲，已拒绝。原有内存结果未替换。");
            default -> fail(commandSource, "无法开始性能记录。");
        };
    }

    private static int stop(CommandSourceStack commandSource) {
        PerformanceRecorder.CommandResult result = PerformanceRecorder.commandStop(commandSource.getServer());
        return switch (result.outcome) {
            case STOP_CANCELLED_ARMED -> succeed(commandSource, "已取消待命。完整 tick 为 0。这是一次已停止结果，可用 /shuperf export 导出。");
            case STOP_MARKED_PENDING -> succeed(commandSource, "已请求停止。当前若有尚未结束的完整 tick，会在它的 END 归并后停止，不会截断。当前若没有打开的 tick，会在下一个 START 边界停止，不再多录一个 tick。");
            case STOP_ALREADY_PENDING -> fail(commandSource, "已经在等待停止，数据未改。");
            case STOP_NONE -> fail(commandSource, "当前没有录制。");
            case STOP_ALREADY -> fail(commandSource, "已经停止。可用 /shuperf status 或 /shuperf export。");
            case REJECTED_UNBOUND -> fail(commandSource, "性能记录没有绑定这台服务器，已拒绝。");
            case REJECTED_SERVER_EXITED -> fail(commandSource, "服务器已退出或正在停止，已拒绝。");
            default -> fail(commandSource, "无法停止性能记录。");
        };
    }

    private static int status(CommandSourceStack commandSource) {
        PerformanceRecorder.CommandResult result = PerformanceRecorder.commandStatus(commandSource.getServer());
        return switch (result.outcome) {
            case STATUS_REPORTED -> succeed(commandSource, result.detail == null ? "性能记录没有状态。" : result.detail);
            case REJECTED_UNBOUND -> fail(commandSource, "性能记录没有绑定这台服务器。");
            default -> fail(commandSource, "无法读取性能记录状态。");
        };
    }

    private static int export(CommandSourceStack commandSource) {
        PerformanceRecorder.CommandResult result = PerformanceRecorder.commandExport(commandSource.getServer());
        return switch (result.outcome) {
            case EXPORT_WRITTEN -> succeed(commandSource, "已导出到新目录：" + result.detail + "。写入在服务端命令线程同步完成。");
            case EXPORT_REJECTED_STATE -> fail(commandSource, "现在不能导出。请先 /shuperf stop，等到状态为 STOPPED 后再导出。不会为了导出截断当前 tick。");
            case EXPORT_REJECTED_SERVER -> fail(commandSource, "服务器未绑定或已退出，不能导出。未自动写盘。");
            case EXPORT_FAILED -> fail(commandSource, "导出失败，内存结果仍保留。" + (result.detail == null ? "" : result.detail));
            default -> fail(commandSource, "无法导出性能记录。");
        };
    }

    private static String replacedStartText(int targetTicks, String previousExport) {
        String text = "已替换内存中的上次停止结果，并重新待命。目标 " + targetTicks
                + " 个完整 tick。从下一个完整 tick 开始。上次结果若未导出，现已不在内存中；以后请先 /shuperf export 再 start。";
        if (previousExport == null || previousExport.isEmpty()) {
            return text;
        }
        return text + "上次成功导出的目录仍在：" + previousExport;
    }

    private static int succeed(CommandSourceStack commandSource, String text) {
        commandSource.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int fail(CommandSourceStack commandSource, String text) {
        commandSource.sendFailure(Component.literal(text));
        return 0;
    }
}
