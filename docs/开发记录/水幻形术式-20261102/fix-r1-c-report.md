# 水幻形 C 组第 1 轮返修报告

日期：2026-11-02。只修复核问题 3。没有复审，没有编译，没有进游戏。本报告写完后停止写源码。

## 改动

| 路径 | 行号 | 作用 |
|---|---|---|
| `shu/technique/release/capture/CaptureAcceptanceLedger.java` | 全文，20–103 | 当前服务器、当前 tick 的成功部署账 |
| `shu/technique/release/capture/ActorCaptureDispatcher.java` | 49–51 | 同窗口再次成功才拒绝 |
| `shu/technique/release/capture/ActorCaptureDispatcher.java` | 168 | 仅 `spawnCaptured` 接受后入账 |

静态 `ACCEPTED_TICK` 已从分发器删除。没有改 A/B/D，没有改 `XueXiMod` 的事件注册，没有改载体、装配、注册和资源。

命名门禁：`powershell -NoProfile -File tools/check-name-gate.ps1`，退出码 0。

## 节流语义

入账只有一处：`CloneService.spawnCaptured` 返回已接受之后（分发器 168 行）。报价失败、魔力不足、名额已满和其余拒绝都不写入。普通链不进入 `tryExecute`，账保持空，tick 监听见空窗口直接返回。

窗口要同时满足两件事才算「这一刻已经部署过」：`boundServer ==` 这次的 `MinecraftServer` 实例，并且 `boundTick ==` 这次的 `serverGameTick`。只比较 tick 数字不够，换世界后 tick 从 0 再涨不会误拒。同一窗口里第二次成功仍返回 `CAPTURE_TICK_LIMIT`，第一次的记录留着。账里只放主人 UUID，不留玩家对象。

## 停止、换 tick、登出

订阅在 `CaptureAcceptanceLedger` 自己的 `@Mod.EventBusSubscriber` 上，不经过公共生命周期类。

- 服务器 tick：空窗口不做事。START 或 END 上，服务器实例变了，或 `getTickCount()` 已不是入账 tick，就清空。本 tick 的 END 在玩法处理之后清掉这一窗口，下一 tick 不再留着上一 tick 的主人。
- `ServerStoppingEvent`（69–72 行）：停服清空，不把上一局带进同一进程里的新服务器。
- `PlayerLoggedOutEvent`（75–83 行）：按 UUID 去掉这名主人。集合空了就放开服务器引用。

三条都会把 `boundServer` 置空。下一局要等新的成功部署才重新绑定。

## 未跑

未运行 Gradle，未 `compileJava`，未打包，未进游戏。没有改制作表，没有动美术。C 组到此停止写入。
