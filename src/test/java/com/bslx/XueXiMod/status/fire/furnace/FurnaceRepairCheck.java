package com.bslx.XueXiMod.status.fire.furnace;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** 炉心返修：静态玩家取值和线程拒绝。不启动游戏。 */
public final class FurnaceRepairCheck {
    private static final Path ROOT = Path.of("D:/alianximod");
    private static int assertionCount;
    private static int failures;

    private FurnaceRepairCheck() {
    }

    public static void main(String[] arguments) throws Exception {
        section("线程", FurnaceRepairCheck::thread);
        section("玩家取值", FurnaceRepairCheck::players);
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        System.out.println("未执行：真实服务端线程、效果和属性写入。没有启动游戏。");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void thread() throws Exception {
        check(!FurnaceMath.allowsServerWork(false, false), "没有服务端时拒绝");
        check(!FurnaceMath.allowsServerWork(false, true), "空服务端即使标记同线程也拒绝");
        check(!FurnaceMath.allowsServerWork(true, false), "服务端存在但不是当前线程时拒绝");
        check(FurnaceMath.allowsServerWork(true, true), "服务端主线程允许");
        check(!FurnaceMath.Outcome.REJECT_THREAD.succeeded(), "线程拒绝不是成功转换");
        String service = read("src/main/java/com/bslx/XueXiMod/status/fire/furnace/FurnaceService.java");
        String runtime = read("src/main/java/com/bslx/XueXiMod/status/fire/furnace/FurnaceRuntime.java");
        String gate = read("src/main/java/com/bslx/XueXiMod/status/fire/furnace/FurnaceThreadGate.java");
        int snapshotStart = service.indexOf("Snapshot serverSnapshot");
        String snapshotBody = snapshotStart < 0 ? "" : service.substring(snapshotStart);
        int snapshotGate = snapshotBody.indexOf("FurnaceThreadGate.onServerThread");
        int snapshotReconcile = snapshotBody.indexOf("FurnaceRuntime.reconcile");
        check(snapshotGate >= 0 && snapshotReconcile > snapshotGate, "查询先看线程，再 reconcile");
        check(service.contains("REJECT_THREAD"), "转换入口返回线程拒绝");
        check(!service.contains("execute(") && !runtime.contains("execute("), "不把异步改动排队后当成成功");
        int reconcile = runtime.indexOf("void reconcile");
        int advance = runtime.indexOf("void advance");
        check(runtime.indexOf("FurnaceThreadGate.onServerThread", reconcile) < runtime.indexOf("FurnaceStore", reconcile),
                "reconcile 写存档前先看线程");
        check(runtime.indexOf("FurnaceThreadGate.onServerThread", advance) < runtime.indexOf("FurnaceStore", advance),
                "advance 写热量前先看线程");
        check(gate.contains("host != null && host.isSameThread()") && gate.contains("allowsServerWork"),
                "线程门同时要求服务端存在且为当前线程");
    }

    private static void players() throws Exception {
        String events = read("src/main/java/com/bslx/XueXiMod/status/fire/furnace/FurnaceEvents.java");
        String tick = between(events, "void onPlayerTick", "void onLogin");
        String login = between(events, "void onLogin", "void onClone");
        String clone = between(events, "void onClone", "void onDeath");
        check(tick.contains("Player player = event.player") && !tick.contains("instanceof"), "tick 直接取玩家");
        check(login.contains("Player player = event.getEntity()") && !login.contains("instanceof"), "登录直接取玩家");
        check(clone.contains("Player successor = event.getEntity()") && !clone.contains("instanceof"), "克隆直接取玩家");
        check(between(events, "void onDeath", "void onHeal").contains("instanceof Player"), "死亡事件的实体仍要判断是不是玩家");
    }

    private static String read(String relative) throws Exception {
        return Files.readString(ROOT.resolve(relative), StandardCharsets.UTF_8);
    }

    private static String between(String text, String start, String end) {
        int from = text.indexOf(start);
        int stop = text.indexOf(end, from + start.length());
        if (from < 0 || stop < 0) {
            return "";
        }
        return text.substring(from, stop);
    }

    private static void section(String title, Checked body) throws Exception {
        int before = failures;
        body.run();
        System.out.println(title + (failures == before ? " 通过" : " 失败"));
    }

    @FunctionalInterface
    private interface Checked {
        void run() throws Exception;
    }

    private static void check(boolean condition, String label) {
        assertionCount += 1;
        if (!condition) {
            failures += 1;
            System.out.println("失败 " + label);
        }
    }
}
