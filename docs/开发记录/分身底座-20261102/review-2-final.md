我先完整读第 2 轮复核任务，确认范围和停写前提后再审当前正式源码。返修报告说只清了普通分身的提交标记，并做了同义重命名。我对照正式源码里这几处路径核对。针对这次返修未发现阻断。普通分身成功提交后，演员身上的提交标记会随本次收束清掉；下一次在提交前失败或过期可以回到可用状态。退款界限、幻影耗尽和 `ENDING` 优先都还在。未编译，未进游戏。

`settleCommitted` 只在普通分身从 `COMMITTING` 收到 `COOLDOWN` 时把 `state.commitLine` 设回 false，位置是 `CloneService.java` 第 591–598 行，清标记在第 594 行。本次过线仍先写 `attempt.commitLine` 和 `state.commitLine`（第 508–509 行）。`finally` 仍看 `attempt.commitLine`：过线后不退款、不走 `restoreReady`（第 533–551 行）。

因此下一次尝试开始时演员标记已是 false。提交前失败会进入 `restoreReady`（第 601–611 行），把 `RESERVED` 收回 `READY` 并清掉 `pending`。排队过期会进入 `cancelQueued`（第 394–411 行），同一条预约被取消。撤回时 `CloneRuntime.cancelPending`（第 371–383 行）也不再被上一次成功挡住。这三个方法没有改，它们仍用 `state.commitLine` 表示「当前这次已经过线、尚未收成冷却」。

幻影仍走第 582–589 行：先写 `SPENT`，再 `end(BUDGET_SPENT)`，不改 `commitLine`。`settleCommitted` 开头仍是生命不是 `ACTIVE` 就返回。已结束的状态不会被 `finally` 拉回 `READY`。同一次尝试若在收束前结束，`cancelPending` 仍会看到 `executing` 或尚未清掉的 `commitLine`，不会把 `ENDING` 写成 `READY`。

命名补修只在 `ClonePositionPolicy.java`。第 13 行常量是 `MINIMUM_SEPARATION`，值仍是 `1.0E-8D`。第 46 行仍用它比较 `delta.lengthSqr()`。`src/main` 里已没有 `MIN_SEPARATION`。比较、提前返回和组帧都没有变。

核对范围就是上述收束、恢复、过期取消、结束时取消预约，以及这一处重命名。`restoreReady` 相对上一轮只因插入第 594 行而下移一行。没有把本轮扩成全仓审查。
