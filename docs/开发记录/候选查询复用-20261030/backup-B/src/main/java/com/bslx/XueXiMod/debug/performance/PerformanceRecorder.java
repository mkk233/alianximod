package com.bslx.XueXiMod.debug.performance;

import com.bslx.XueXiMod.XueXiMod;
import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.ModList;

/**
 * 默认关闭的服务端性能记录。热路径只有 {@link #isRecording()}、{@link #begin()}、{@link #end(PerfMetric, long)}、{@link #add(PerfCounter, long)}。
 * 关闭时这些方法不读 nanoTime，也不分配。业务耗时都是 inclusive，禁止在报表里加总。
 */
public final class PerformanceRecorder {
    static final int DEFAULT_TARGET_TICKS = 1200;
    static final int CAPACITY_TICKS = 6000;
    private static final int FORMAT_VERSION = 1;

    private static final PerfMetric[] METRIC_VALUES = PerfMetric.values();
    private static final PerfCounter[] COUNTER_VALUES = PerfCounter.values();
    private static final int METRIC_COUNT = METRIC_VALUES.length;
    private static final int COUNTER_COUNT = COUNTER_VALUES.length;

    private static final long[] scratchMetricNanos = new long[METRIC_COUNT];
    private static final long[] scratchPeakNanos = new long[METRIC_COUNT];
    private static final long[] scratchMetricCalls = new long[METRIC_COUNT];
    private static final long[] scratchCounters = new long[COUNTER_COUNT];

    private static volatile boolean tickGate;
    private static volatile Phase phase = Phase.IDLE;
    private static volatile Thread boundThread;
    private static volatile MinecraftServer boundServer;

    private static long tickStartNanos;
    private static int requestedTicks;
    private static int completedTicks;
    private static int incompleteTicks;
    private static long peakTickNanos;
    private static long totalTickNanos;
    private static long armedEpochMillis;
    private static long recordingEpochMillis;
    private static long stoppedEpochMillis;
    private static StopReason stopReason = StopReason.NONE;
    private static String lastExportPath;
    private static long exportSerial;

    private static long[] storedTickNanos;
    private static int[] storedTickOrdinal;
    private static long[] storedTargetQueryCalls;
    private static long[] storedEntityQueryCalls;
    private static long[] storedTargetCandidates;
    private static long[] storedEntityCandidates;
    private static long[] storedHitUpdates;
    private static long[] storedSwordUpdates;
    private static long[] sessionMetricNanos;
    private static long[] sessionPeakNanos;
    private static long[] sessionMetricCalls;
    private static long[] sessionCounters;

    private PerformanceRecorder() {
    }

    public static boolean isRecording() {
        if (!tickGate) {
            return false;
        }
        return Thread.currentThread() == boundThread;
    }

    public static long begin() {
        if (!tickGate) {
            return 0L;
        }
        if (Thread.currentThread() != boundThread) {
            return 0L;
        }
        long started = System.nanoTime();
        if (started == 0L) {
            return 1L;
        }
        return started;
    }

    public static void end(PerfMetric metric, long startedNanos) {
        if (startedNanos == 0L || metric == null) {
            return;
        }
        if (!tickGate || Thread.currentThread() != boundThread) {
            return;
        }
        int slot = metric.ordinal();
        if (slot < 0 || slot >= METRIC_COUNT) {
            return;
        }
        long finished = System.nanoTime();
        long elapsed = finished - startedNanos;
        if (elapsed < 0L) {
            elapsed = 0L;
        }
        scratchMetricCalls[slot] = scratchMetricCalls[slot] + 1L;
        scratchMetricNanos[slot] = scratchMetricNanos[slot] + elapsed;
        long peak = scratchPeakNanos[slot];
        if (elapsed > peak) {
            scratchPeakNanos[slot] = elapsed;
        }
    }

    public static void add(PerfCounter counter, long delta) {
        if (!tickGate) {
            return;
        }
        if (Thread.currentThread() != boundThread || counter == null || delta <= 0L) {
            return;
        }
        int slot = counter.ordinal();
        if (slot < 0 || slot >= COUNTER_COUNT) {
            return;
        }
        scratchCounters[slot] = scratchCounters[slot] + delta;
    }

    static boolean sameServer(MinecraftServer server) {
        return server != null && server == boundServer;
    }

    static void bindFreshServer(MinecraftServer server, Thread serverThread) {
        tickGate = false;
        phase = Phase.IDLE;
        releaseMemory();
        boundThread = serverThread;
        boundServer = server;
        phase = Phase.IDLE;
    }

    static void releaseServer(MinecraftServer server) {
        if (server == null || server != boundServer) {
            return;
        }
        tickGate = false;
        phase = Phase.IDLE;
        releaseMemory();
        boundServer = null;
        boundThread = null;
    }

    static void onTickStart() {
        Phase current = phase;
        if (current != Phase.ARMED && current != Phase.RECORDING && current != Phase.STOP_PENDING) {
            return;
        }
        if (Thread.currentThread() != boundThread) {
            return;
        }
        if (current == Phase.STOP_PENDING && !tickGate) {
            finishStopped(StopReason.STOP_COMMAND);
            return;
        }
        if (tickGate) {
            tickGate = false;
            tickStartNanos = 0L;
            incompleteTicks++;
            if (phase == Phase.STOP_PENDING) {
                finishStopped(StopReason.STOP_COMMAND);
                return;
            }
        }
        if (storedTickNanos == null || sessionMetricCalls == null || sessionCounters == null || requestedTicks <= 0) {
            finishStopped(StopReason.CAPACITY_REACHED);
            return;
        }
        if (completedTicks >= storedTickNanos.length || completedTicks >= CAPACITY_TICKS) {
            finishStopped(StopReason.CAPACITY_REACHED);
            return;
        }
        if (completedTicks >= requestedTicks) {
            finishStopped(StopReason.TARGET_REACHED);
            return;
        }
        if (phase == Phase.ARMED) {
            recordingEpochMillis = System.currentTimeMillis();
            phase = Phase.RECORDING;
        }
        if (phase != Phase.RECORDING) {
            return;
        }
        clearScratch();
        long started = System.nanoTime();
        tickStartNanos = started;
        tickGate = true;
    }

    static void onTickEnd() {
        if (!tickGate) {
            return;
        }
        if (Thread.currentThread() != boundThread) {
            return;
        }
        long ended = System.nanoTime();
        long elapsed = ended - tickStartNanos;
        if (elapsed < 0L) {
            elapsed = 0L;
        }
        tickGate = false;
        tickStartNanos = 0L;
        mergeCompletedTick(elapsed);
    }

    static CommandResult commandStart(MinecraftServer server, int targetTicks) {
        CommandResult rejected = rejectUnlessRunning(server);
        if (rejected != null) {
            return rejected;
        }
        if (targetTicks < 1 || targetTicks > CAPACITY_TICKS) {
            return CommandResult.of(CommandOutcome.REJECTED_RANGE);
        }
        Phase current = phase;
        if (current == Phase.ARMED || current == Phase.RECORDING || current == Phase.STOP_PENDING) {
            return CommandResult.of(CommandOutcome.REJECTED_BUSY);
        }
        boolean replacing = current == Phase.STOPPED;
        String previousExport = lastExportPath;
        try {
            installSession(targetTicks);
        } catch (OutOfMemoryError exhaustion) {
            return CommandResult.of(CommandOutcome.REJECTED_MEMORY);
        }
        requestedTicks = targetTicks;
        completedTicks = 0;
        incompleteTicks = 0;
        totalTickNanos = 0L;
        peakTickNanos = 0L;
        recordingEpochMillis = 0L;
        stoppedEpochMillis = 0L;
        stopReason = StopReason.NONE;
        tickStartNanos = 0L;
        tickGate = false;
        clearScratch();
        armedEpochMillis = System.currentTimeMillis();
        phase = Phase.ARMED;
        if (replacing) {
            return new CommandResult(CommandOutcome.STARTED_REPLACED, previousExport);
        }
        return CommandResult.of(CommandOutcome.STARTED);
    }

    static CommandResult commandStop(MinecraftServer server) {
        CommandResult rejected = rejectUnlessRunning(server);
        if (rejected != null) {
            return rejected;
        }
        if (phase == Phase.IDLE) {
            return CommandResult.of(CommandOutcome.STOP_NONE);
        }
        if (phase == Phase.STOPPED) {
            return CommandResult.of(CommandOutcome.STOP_ALREADY);
        }
        if (phase == Phase.STOP_PENDING) {
            return CommandResult.of(CommandOutcome.STOP_ALREADY_PENDING);
        }
        if (phase == Phase.ARMED) {
            completedTicks = 0;
            incompleteTicks = 0;
            totalTickNanos = 0L;
            peakTickNanos = 0L;
            clearScratch();
            finishStopped(StopReason.ARMED_CANCELLED);
            return CommandResult.of(CommandOutcome.STOP_CANCELLED_ARMED);
        }
        phase = Phase.STOP_PENDING;
        return CommandResult.of(CommandOutcome.STOP_MARKED_PENDING);
    }

    static CommandResult commandStatus(MinecraftServer server) {
        if (server == null || server != boundServer) {
            return CommandResult.of(CommandOutcome.REJECTED_UNBOUND);
        }
        return new CommandResult(CommandOutcome.STATUS_REPORTED, statusLine());
    }

    static CommandResult commandExport(MinecraftServer server) {
        CommandResult rejected = rejectUnlessRunning(server);
        if (rejected != null) {
            return new CommandResult(CommandOutcome.EXPORT_REJECTED_SERVER, rejected.detail);
        }
        if (tickGate || phase != Phase.STOPPED) {
            return CommandResult.of(CommandOutcome.EXPORT_REJECTED_STATE);
        }
        Path folder = null;
        try {
            folder = createSessionDirectory(server);
            String summaryBody = buildSummary(server, folder);
            String csvBody = buildTicksCsv();
            Files.writeString(folder.resolve("summary.txt"), summaryBody, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            Files.writeString(folder.resolve("ticks.csv"), csvBody, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            lastExportPath = folder.toString();
            return new CommandResult(CommandOutcome.EXPORT_WRITTEN, lastExportPath);
        } catch (Exception failure) {
            String location = folder == null ? "尚未创建目录" : folder.toString();
            return new CommandResult(CommandOutcome.EXPORT_FAILED, location + "；" + failure);
        }
    }

    private static void mergeCompletedTick(long elapsed) {
        if (storedTickNanos == null || storedTickOrdinal == null
                || storedTargetQueryCalls == null || storedEntityQueryCalls == null
                || storedTargetCandidates == null || storedEntityCandidates == null
                || storedHitUpdates == null || storedSwordUpdates == null
                || sessionMetricCalls == null || sessionMetricNanos == null
                || sessionPeakNanos == null || sessionCounters == null
                || completedTicks < 0 || completedTicks >= storedTickNanos.length) {
            incompleteTicks++;
            finishStopped(StopReason.CAPACITY_REACHED);
            return;
        }
        int row = completedTicks;
        storedTickOrdinal[row] = row + 1;
        storedTickNanos[row] = elapsed;
        storedTargetQueryCalls[row] = scratchCounters[PerfCounter.TARGET_QUERY_CALLS.ordinal()];
        storedEntityQueryCalls[row] = scratchCounters[PerfCounter.ENTITY_QUERY_CALLS.ordinal()];
        storedTargetCandidates[row] = scratchCounters[PerfCounter.TARGET_CANDIDATES.ordinal()];
        storedEntityCandidates[row] = scratchCounters[PerfCounter.ENTITY_CANDIDATES.ordinal()];
        storedHitUpdates[row] = scratchCounters[PerfCounter.HIT_ENTITY_UPDATES.ordinal()];
        storedSwordUpdates[row] = scratchCounters[PerfCounter.SWORD_UPDATES.ordinal()];
        for (int slot = 0; slot < METRIC_COUNT; slot++) {
            sessionMetricCalls[slot] = sessionMetricCalls[slot] + scratchMetricCalls[slot];
            sessionMetricNanos[slot] = sessionMetricNanos[slot] + scratchMetricNanos[slot];
            long peak = scratchPeakNanos[slot];
            if (peak > sessionPeakNanos[slot]) {
                sessionPeakNanos[slot] = peak;
            }
        }
        for (int slot = 0; slot < COUNTER_COUNT; slot++) {
            sessionCounters[slot] = sessionCounters[slot] + scratchCounters[slot];
        }
        totalTickNanos = totalTickNanos + elapsed;
        if (elapsed > peakTickNanos) {
            peakTickNanos = elapsed;
        }
        completedTicks = row + 1;
        clearScratch();
        if (completedTicks >= requestedTicks) {
            finishStopped(StopReason.TARGET_REACHED);
            return;
        }
        if (completedTicks >= CAPACITY_TICKS) {
            finishStopped(StopReason.CAPACITY_REACHED);
            return;
        }
        if (phase == Phase.STOP_PENDING) {
            finishStopped(StopReason.STOP_COMMAND);
        }
    }

    private static void installSession(int targetTicks) {
        long[] tickNanos = new long[targetTicks];
        int[] tickOrdinals = new int[targetTicks];
        long[] targetQueries = new long[targetTicks];
        long[] entityQueries = new long[targetTicks];
        long[] targetCandidates = new long[targetTicks];
        long[] entityCandidates = new long[targetTicks];
        long[] hitUpdates = new long[targetTicks];
        long[] swordUpdates = new long[targetTicks];
        long[] metricNanos = new long[METRIC_COUNT];
        long[] metricPeaks = new long[METRIC_COUNT];
        long[] metricCalls = new long[METRIC_COUNT];
        long[] counterTotals = new long[COUNTER_COUNT];
        storedTickNanos = tickNanos;
        storedTickOrdinal = tickOrdinals;
        storedTargetQueryCalls = targetQueries;
        storedEntityQueryCalls = entityQueries;
        storedTargetCandidates = targetCandidates;
        storedEntityCandidates = entityCandidates;
        storedHitUpdates = hitUpdates;
        storedSwordUpdates = swordUpdates;
        sessionMetricNanos = metricNanos;
        sessionPeakNanos = metricPeaks;
        sessionMetricCalls = metricCalls;
        sessionCounters = counterTotals;
    }

    private static void releaseMemory() {
        tickGate = false;
        tickStartNanos = 0L;
        requestedTicks = 0;
        completedTicks = 0;
        incompleteTicks = 0;
        peakTickNanos = 0L;
        totalTickNanos = 0L;
        armedEpochMillis = 0L;
        recordingEpochMillis = 0L;
        stoppedEpochMillis = 0L;
        stopReason = StopReason.NONE;
        lastExportPath = null;
        storedTickNanos = null;
        storedTickOrdinal = null;
        storedTargetQueryCalls = null;
        storedEntityQueryCalls = null;
        storedTargetCandidates = null;
        storedEntityCandidates = null;
        storedHitUpdates = null;
        storedSwordUpdates = null;
        sessionMetricNanos = null;
        sessionPeakNanos = null;
        sessionMetricCalls = null;
        sessionCounters = null;
        clearScratch();
    }

    private static void finishStopped(StopReason reason) {
        tickGate = false;
        tickStartNanos = 0L;
        if (phase == Phase.STOPPED) {
            return;
        }
        stopReason = reason;
        stoppedEpochMillis = System.currentTimeMillis();
        phase = Phase.STOPPED;
    }

    private static void clearScratch() {
        Arrays.fill(scratchMetricNanos, 0L);
        Arrays.fill(scratchPeakNanos, 0L);
        Arrays.fill(scratchMetricCalls, 0L);
        Arrays.fill(scratchCounters, 0L);
    }

    private static CommandResult rejectUnlessRunning(MinecraftServer server) {
        if (server == null || server != boundServer) {
            return CommandResult.of(CommandOutcome.REJECTED_UNBOUND);
        }
        if (!server.isRunning() || server.isStopped()) {
            return CommandResult.of(CommandOutcome.REJECTED_SERVER_EXITED);
        }
        return null;
    }

    private static String statusLine() {
        String exported = lastExportPath == null ? "无" : lastExportPath;
        return "状态: " + phase.name()
                + "；完成/目标: " + completedTicks + "/" + requestedTicks
                + "；未完整: " + incompleteTicks
                + "；最近导出: " + exported;
    }

    private static Path createSessionDirectory(MinecraftServer server) throws IOException {
        File gameDirectory = server.getServerDirectory();
        if (gameDirectory == null) {
            throw new IOException("服务器工作目录不可用");
        }
        Path root = gameDirectory.toPath().resolve("logs").resolve("xueximod-performance");
        Files.createDirectories(root);
        long stampMillis = recordingEpochMillis;
        if (stampMillis <= 0L) {
            stampMillis = armedEpochMillis;
        }
        if (stampMillis <= 0L) {
            stampMillis = stoppedEpochMillis > 0L ? stoppedEpochMillis : System.currentTimeMillis();
        }
        DateTimeFormatter folderFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssSSS'Z'", Locale.ROOT)
                .withZone(ZoneOffset.UTC);
        String stamp = folderFormat.format(Instant.ofEpochMilli(stampMillis));
        FileAlreadyExistsException occupied = null;
        for (int attempt = 0; attempt < 10000; attempt++) {
            exportSerial++;
            if (exportSerial <= 0L) {
                exportSerial = 1L;
            }
            Path folder = root.resolve(stamp + "-" + exportSerial);
            try {
                return Files.createDirectory(folder);
            } catch (FileAlreadyExistsException existingFolder) {
                occupied = existingFolder;
            }
        }
        throw new IOException("无法创建不覆盖的唯一目录", occupied);
    }

    private static String buildSummary(MinecraftServer server, Path folder) throws IOException {
        if (storedTickNanos == null || sessionMetricCalls == null || sessionCounters == null) {
            throw new IOException("停止结果不在内存中");
        }
        int sampleCount = completedTicks;
        if (sampleCount < 0) {
            sampleCount = 0;
        }
        if (sampleCount > storedTickNanos.length) {
            sampleCount = storedTickNanos.length;
        }
        StringBuilder builder = new StringBuilder(4096);
        appendKey(builder, "format_version", FORMAT_VERSION);
        appendKey(builder, "mod_id", XueXiMod.MOD_ID);
        appendKey(builder, "mod_version", modVersion());
        appendKey(builder, "java_version", javaVersion());
        boolean dedicated = server.isDedicatedServer();
        appendKey(builder, "server_type", dedicated ? "dedicated" : "integrated");
        appendKey(builder, "server_type_label", dedicated ? "专用" : "集成");
        appendKey(builder, "session_armed_utc", formatUtc(armedEpochMillis));
        appendKey(builder, "session_recording_started_utc", formatUtc(recordingEpochMillis));
        appendKey(builder, "session_stopped_utc", formatUtc(stoppedEpochMillis));
        appendKey(builder, "stop_reason", stopReasonToken(stopReason));
        appendKey(builder, "stop_reason_label", stopReasonLabel(stopReason));
        appendKey(builder, "target_ticks", requestedTicks);
        appendKey(builder, "completed_ticks", sampleCount);
        appendKey(builder, "incomplete_ticks", incompleteTicks);
        appendKey(builder, "capacity_ticks", CAPACITY_TICKS);
        appendKey(builder, "buffer_ticks", storedTickNanos.length);
        appendKey(builder, "p95_algorithm", "nearest-rank");
        appendKey(builder, "p95_index_formula", "ceil(0.95*N)-1");
        appendKey(builder, "p95_index_integer", "(95*N+99)/100-1");
        appendKey(builder, "p95_applies_to", "completed observed server ticks only");
        appendKey(builder, "metric_call_p95", "not_provided");
        appendKey(builder, "export_directory", folder.toString());
        appendKey(builder, "sampling_boundary",
                "Forge ServerTickEvent START(HIGHEST) 到 END(LOWEST) 的服务器 tick 墙钟。中文称观测 MSPT，字段名为 observed_server_tick_ms。不包括 tick 间 sleep 或等待，也不包括事件钩子外的工作。不是原版完整 MSPT，也不是纯 CPU 时间。区间内包含 GC、抢占和采样开销。同优先级监听器顺序不保证。");
        builder.append('\n');
        if (sampleCount == 0) {
            appendKey(builder, "observed_server_tick_count", 0);
            appendKey(builder, "observed_server_tick_total_ns", 0);
            appendKey(builder, "observed_server_tick_average_ns", "N/A");
            appendKey(builder, "observed_server_tick_average_ms", "N/A");
            appendKey(builder, "observed_server_tick_p95_ns", "N/A");
            appendKey(builder, "observed_server_tick_p95_ms", "N/A");
            appendKey(builder, "observed_server_tick_p95_index", "N/A");
            appendKey(builder, "observed_server_tick_peak_ns", "N/A");
            appendKey(builder, "observed_server_tick_peak_ms", "N/A");
        } else {
            int rankIndex = nearestRankIndex(sampleCount);
            long p95Nanos = percentileNearestRank(storedTickNanos, sampleCount, rankIndex);
            appendKey(builder, "observed_server_tick_count", sampleCount);
            appendKey(builder, "observed_server_tick_total_ns", totalTickNanos);
            appendKey(builder, "observed_server_tick_average_ns", formatAverage(totalTickNanos, sampleCount));
            appendKey(builder, "observed_server_tick_average_ms", formatMillisFromAverage(totalTickNanos, sampleCount));
            appendKey(builder, "observed_server_tick_p95_ns", p95Nanos);
            appendKey(builder, "observed_server_tick_p95_ms", formatObservedMillis(p95Nanos));
            appendKey(builder, "observed_server_tick_p95_index", rankIndex);
            appendKey(builder, "observed_server_tick_peak_ns", peakTickNanos);
            appendKey(builder, "observed_server_tick_peak_ms", formatObservedMillis(peakTickNanos));
        }
        builder.append('\n');
        appendKey(builder, "inclusive_policy", "do_not_sum");
        builder.append("note_inclusive: 下列业务项都是 inclusive，允许嵌套和同枚举重入，累计可以超过墙钟。禁止把它们相加，禁止画成互斥百分比，禁止从 observed tick 里逐项相减。这里的 P95、平均值和峰值只描述完整 tick 的观测墙钟，不是单次调用耗时的 P95。\n");
        for (int slot = 0; slot < METRIC_COUNT; slot++) {
            long calls = sessionMetricCalls[slot];
            long totalNanos = sessionMetricNanos[slot];
            long peakNanos = sessionPeakNanos[slot];
            builder.append("metric ");
            builder.append(METRIC_VALUES[slot].name());
            builder.append(" calls=");
            builder.append(calls);
            builder.append(" total_ns=");
            builder.append(totalNanos);
            builder.append(" average_ns=");
            builder.append(formatAverage(totalNanos, calls));
            builder.append(" peak_ns=");
            builder.append(calls <= 0L ? "N/A" : Long.toString(peakNanos));
            builder.append('\n');
        }
        builder.append('\n');
        builder.append("note_query_total: 每 tick 查询次数 = TARGET_QUERY_CALLS + ENTITY_QUERY_CALLS。两类调用互斥，不能再把同一调用算进两边。\n");
        builder.append("note_candidates: 候选数把重复访问照实累加，不是唯一目标数，也不是空间索引内部扫描量。\n");
        builder.append("note_owned_scan: SWORD_OWNED_SCAN_CALLS 与 SWORD_OWNED_SCANNED 是全实体扫描，单独列出，不是空间查询，不计入 query_calls_total。\n");
        builder.append("note_updates: HIT_ENTITY_UPDATES 与 SWORD_UPDATES 是服务端 tick 入口次数，含休眠和等待激活，不是实时活跃实体总数。\n");
        for (int slot = 0; slot < COUNTER_COUNT; slot++) {
            builder.append("counter ");
            builder.append(COUNTER_VALUES[slot].name());
            builder.append(" total=");
            builder.append(sessionCounters[slot]);
            builder.append('\n');
        }
        builder.append('\n');
        appendKey(builder, "csv_file", "ticks.csv");
        appendKey(builder, "csv_columns", "tick_ordinal,tick_ns,observed_server_tick_ms,target_query_calls,entity_query_calls,query_calls_total,target_candidates,entity_candidates,candidate_total,hit_entity_updates,sword_updates");
        appendKey(builder, "csv_row_limit", CAPACITY_TICKS);
        appendKey(builder, "csv_order", "tick_ordinal ascending");
        appendKey(builder, "csv_encoding", "UTF-8 without BOM, LF");
        builder.append("note_outside_tick: 没有打开采样 tick 时的命令和网络任务不计入首版。同步调用不跨服务端 tick；门控关闭后的 end 直接忽略。\n");
        builder.append("note_delayed_settle: 延迟结算只在实际执行的 tick 落在窗口内时计入，不按施放时间回填。\n");
        builder.append("note_debug_log: 调试日志会影响读数。对比时保持 /shulog 与其他设置一致。本记录器不读取、不切换 DebugLog，也不把客户端调试状态当作服务端状态。\n");
        builder.append("note_export: 排序和写盘只发生在 STOPPED 之后的导出命令里，不进入该次采样。导出同步占用服务器线程，应在实测结束后执行。\n");
        builder.append("note_restart: 同一次 JVM 里下一次服务器启动会清空内存和服务器引用，不继承上次记录，也不在退出时自动写盘。\n");
        builder.append('\n');
        builder.append("uncovered: 原版实体追踪、真实网络发送完成、字节数、延迟、客户端解码、渲染和 FPS。\n");
        builder.append("uncovered: EXPLICIT_SEND 只是业务调用分发或入队的墙钟，不能据此推算包数。entityData.set 和 setPos 不算网络发送。\n");
        builder.append("uncovered: ModNetwork.sendToPlayer、FireVisualRelay，以及全模组其他发包位置。显式同步只覆盖 TechniqueHitSettle 的事件、声音和粒子调用。\n");
        builder.append("uncovered: 真剑抛出生成，以及 HitVolumes 视觉体生成。攻击体生成只计 TechniqueHitSpec，幻剑生成只计 PhantomSwordEffect。\n");
        builder.append("uncovered: 活跃实体实时总量、休眠与活跃分离、每术式或每玩家明细、内存分配和 GC 的独立耗时、空间索引内部候选访问量，以及未列出的查询和碰撞。\n");
        builder.append("uncovered: 更新次数不能读成活跃实体总数。\n");
        builder.append("uncovered: ARMED 之前、停止之后，以及服务端退出时的残缺 tick。退出时未完成 tick 随 session 丢弃，不写盘。\n");
        builder.append("uncovered: 本记录不是全模组 profiler，也不是原版追踪器 profiler。\n");
        return builder.toString();
    }

    private static String buildTicksCsv() throws IOException {
        if (storedTickNanos == null) {
            throw new IOException("停止结果不在内存中");
        }
        int sampleCount = completedTicks;
        if (sampleCount < 0) {
            sampleCount = 0;
        }
        if (sampleCount > storedTickNanos.length) {
            sampleCount = storedTickNanos.length;
        }
        StringBuilder builder = new StringBuilder(sampleCount * 96 + 160);
        builder.append("tick_ordinal,tick_ns,observed_server_tick_ms,target_query_calls,entity_query_calls,query_calls_total,target_candidates,entity_candidates,candidate_total,hit_entity_updates,sword_updates\n");
        for (int row = 0; row < sampleCount; row++) {
            long queryTotal = storedTargetQueryCalls[row] + storedEntityQueryCalls[row];
            long candidateTotal = storedTargetCandidates[row] + storedEntityCandidates[row];
            builder.append(storedTickOrdinal[row]);
            builder.append(',');
            builder.append(storedTickNanos[row]);
            builder.append(',');
            builder.append(formatObservedMillis(storedTickNanos[row]));
            builder.append(',');
            builder.append(storedTargetQueryCalls[row]);
            builder.append(',');
            builder.append(storedEntityQueryCalls[row]);
            builder.append(',');
            builder.append(queryTotal);
            builder.append(',');
            builder.append(storedTargetCandidates[row]);
            builder.append(',');
            builder.append(storedEntityCandidates[row]);
            builder.append(',');
            builder.append(candidateTotal);
            builder.append(',');
            builder.append(storedHitUpdates[row]);
            builder.append(',');
            builder.append(storedSwordUpdates[row]);
            builder.append('\n');
        }
        return builder.toString();
    }

    private static int nearestRankIndex(int sampleCount) {
        int rankIndex = (95 * sampleCount + 99) / 100 - 1;
        if (rankIndex < 0) {
            return 0;
        }
        if (rankIndex >= sampleCount) {
            return sampleCount - 1;
        }
        return rankIndex;
    }

    private static long percentileNearestRank(long[] samples, int sampleCount, int rankIndex) {
        long[] sortedNanos = Arrays.copyOf(samples, sampleCount);
        Arrays.sort(sortedNanos);
        return sortedNanos[rankIndex];
    }

    private static String formatUtc(long epochMillis) {
        if (epochMillis <= 0L) {
            return "N/A";
        }
        return DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(epochMillis));
    }

    private static String formatAverage(long totalNanos, long calls) {
        if (calls <= 0L) {
            return "N/A";
        }
        return String.format(Locale.ROOT, "%.3f", (double) totalNanos / (double) calls);
    }

    private static String formatMillisFromAverage(long totalNanos, int sampleCount) {
        if (sampleCount <= 0) {
            return "N/A";
        }
        double averageNanos = (double) totalNanos / (double) sampleCount;
        return String.format(Locale.ROOT, "%.6f", averageNanos / 1_000_000.0);
    }

    private static String formatObservedMillis(long nanos) {
        return String.format(Locale.ROOT, "%.6f", nanos / 1_000_000.0);
    }

    private static String javaVersion() {
        String versionText = System.getProperty("java.version");
        if (versionText == null || versionText.isEmpty()) {
            return "unknown";
        }
        return versionText;
    }

    private static String modVersion() {
        try {
            return ModList.get().getModContainerById(XueXiMod.MOD_ID)
                    .map(container -> {
                        if (container.getModInfo() == null || container.getModInfo().getVersion() == null) {
                            return "missing";
                        }
                        return container.getModInfo().getVersion().toString();
                    })
                    .orElse("missing");
        } catch (Exception failure) {
            return "unavailable";
        }
    }

    private static String stopReasonToken(StopReason reason) {
        return switch (reason) {
            case ARMED_CANCELLED -> "armed_cancelled";
            case STOP_COMMAND -> "stop_command";
            case TARGET_REACHED -> "target_reached";
            case CAPACITY_REACHED -> "capacity_reached";
            case NONE -> "none";
        };
    }

    private static String stopReasonLabel(StopReason reason) {
        return switch (reason) {
            case ARMED_CANCELLED -> "待命被取消，完整 tick 为 0";
            case STOP_COMMAND -> "命令停止，已结束的完整 tick 保留";
            case TARGET_REACHED -> "达到请求的完整 tick 数";
            case CAPACITY_REACHED -> "达到 6000 条完整 tick 容量";
            case NONE -> "无";
        };
    }

    private static void appendKey(StringBuilder builder, String key, String value) {
        builder.append(key);
        builder.append(": ");
        builder.append(value);
        builder.append('\n');
    }

    private static void appendKey(StringBuilder builder, String key, long value) {
        builder.append(key);
        builder.append(": ");
        builder.append(value);
        builder.append('\n');
    }

    private enum Phase {
        IDLE,
        ARMED,
        RECORDING,
        STOP_PENDING,
        STOPPED
    }

    private enum StopReason {
        NONE,
        ARMED_CANCELLED,
        STOP_COMMAND,
        TARGET_REACHED,
        CAPACITY_REACHED
    }

    enum CommandOutcome {
        STARTED,
        STARTED_REPLACED,
        REJECTED_BUSY,
        REJECTED_UNBOUND,
        REJECTED_SERVER_EXITED,
        REJECTED_RANGE,
        REJECTED_MEMORY,
        STOP_CANCELLED_ARMED,
        STOP_MARKED_PENDING,
        STOP_ALREADY_PENDING,
        STOP_NONE,
        STOP_ALREADY,
        STATUS_REPORTED,
        EXPORT_WRITTEN,
        EXPORT_REJECTED_STATE,
        EXPORT_REJECTED_SERVER,
        EXPORT_FAILED
    }

    static final class CommandResult {
        final CommandOutcome outcome;
        final String detail;

        private CommandResult(CommandOutcome outcome, String detail) {
            this.outcome = outcome;
            this.detail = detail;
        }

        static CommandResult of(CommandOutcome outcome) {
            return new CommandResult(outcome, null);
        }
    }
}
