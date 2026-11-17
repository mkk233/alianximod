我先读这份复核说明，再按它的范围核对修正是否落地。上次指出的两处要复审。我只看 `accept` 的门控、新增回归断言，以及报告里对应的修订。通过。

同身份、同 revision、同 sequence 的 `FULL` 在覆盖字段前算出 `sameSequence`，不调用 `applyPose`，尺寸仍会更新，`anchorAge` 仍可向前。`POSE` 先要求 `packet.revision == view.revision`，同 sequence 不重摆，只有 sequence 变大才 `applyPose`。更高 revision 的 `POSE` 在改任何字段之前返回。`packetLags` 仍是小于判断，同 sequence 的 `REMOVE` 会删除。

`duplicatePoseClockStays` 已从 `main` 调用。它把 `poseStartMillis` 设成 `11L` 再断言重复 `FULL`/`POSE` 仍是 `11L`，并检查高 revision `POSE` 之后的时钟、法线、代际、半径和年龄都不变。`wire-report.md` 的「姿态时钟修正」与上述行为一致。