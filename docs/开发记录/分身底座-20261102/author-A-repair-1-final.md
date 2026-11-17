先读完这份返修任务说明，再只按它写明的范围动手。命名门禁已实际跑过，失败项都不在这次允许改的文件里。正在把返修结果写成报告。普通分身第一次成功提交后，演员身上的 `commitLine` 会留下 `true`，下一次提交前失败或过期就无法回到 `READY`。已经按复核给出的修法改好。

只改了 `src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/CloneService.java` 第 594 行。`settleCommitted` 在普通分身从 `COMMITTING` 收到 `COOLDOWN` 时，把 `state.commitLine` 设回 `false`。同一次尝试的退款仍看 `Attempt.commitLine`。幻影仍是先 `SPENT` 再按额度结束。生命已进入 `ENDING` 或 `ENDED` 时不会被拉回 `READY`。

命名门禁已执行：`powershell -NoProfile -File tools/check-name-gate.ps1`，退出码 1。三处都不是这次改的文件：

- `OriginalHumanoid.java:165` 的 `i`
- `OriginalHumanoid.java:202` 的 `to`
- `ClonePositionPolicy.java:13` 的 `MIN_SEPARATION`

这三处没有改。没有编译。

报告在 `docs/开发记录/分身底座-20261102/author-A-repair-1-report.md`。作者 A 第 1 轮返修到此停止写入。待统一编译。游戏未测。
