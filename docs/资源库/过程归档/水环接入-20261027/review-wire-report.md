我先把任务说明全文读完，再按里面的范围和约束执行。任务是只读核对水环同步接入。我先读本轮规格和报告，再对照备份看这四个文件的改动。规格里的门控和快照还要对着 `GuardStates` 与改前备份核对。我只看构包、删除通知和协议相关差异。同激活、同 revision、同 sequence 的 `FULL`/`POSE` 会重摆姿态；更高 revision 的 `POSE` 会抢走当前环。其余核对项与规格一致。

**1. 同代重复包重摆姿态**

`src/main/java/com/bslx/XueXiMod/network/GuardSyncPacket.java` 第 174–188 行、第 191–197 行，`applyPose` 在第 258 行写 `poseStartMillis`。

`FULL` 只在 `animationStart` 更旧，或同身份且 `packetLags` 时返回。同 revision、同 sequence 不算落后，仍会覆盖字段并调用 `applyPose`。`POSE` 在通过同一道门后同样总是 `applyPose`。

触发：视图已是 `animationStart=100`、`revision=2`、`sequence=5`，姿态时钟为 T0。再来一包同身份、同 revision、同 sequence 的 `FULL` 或 `POSE`。旋转年龄不会归零，但 `poseStartMillis` 变成当前墙钟，姿态从这一拍重播。`POSE` 带的是动作起点和整段 tick，重放会把倾斜拨回起点。

最小修法：写 `revision`/`sequence` 之前先算 `freshActivation`、`sameRevision`、`sameSequence`。`anchorAge` 仍只向前校准；`FULL` 仍可更新尺寸。仅新激活，或 revision/sequence 确实更新时才 `applyPose`。不要把 `packetLags` 的 `<` 改成 `<=`。在 `GuardSyncWireCheck` 补：同 revision 同 sequence 的重复 `FULL`/`POSE` 保持 `poseStartMillis`；同 revision、更大 sequence 的 `POSE` 仍更新姿态。

**2. 更高 revision 的 POSE 抢代**

同文件第 191–194 行。`packetLags`（第 218–225 行）在 `packet.revision > view.revision` 时返回 false。

触发：视图为 `revision=2`、`sequence=5`。同 `animationStart` 的 `POSE` 以 `revision=9`、`sequence=1` 到达。视图被改成 revision 9、sequence 1，姿态被换掉。之后真正的 `FULL` revision 3 会被当成旧包丢掉。

`POSE` 必须 `packet.revision == view.revision`，只推进同代 sequence。新 revision 只由 `FULL` 建立。更高或更低 revision 的 `POSE` 在改任何字段之前返回。`REMOVE` 仍用现在的 `packetLags`，同 sequence 的删除要保留。回归：高 revision `POSE` 不改变 `revision`、`sequence`、`fromX`、`poseStartMillis`。