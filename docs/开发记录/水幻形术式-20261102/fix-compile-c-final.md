先通读编译返修任务全文，再按其中的授权范围改。两处 javac 错误已按日志改完。报告在 `docs/开发记录/水幻形术式-20261102/fix-compile-c-report.md`。

`SkillUsePacket.executeSkill` 里，本批捕获路由的局部变量改名为 `captureTiming`（335–337 行）。非空仍直接返回这次施放结果，空则继续原来的批量编译。632 行旧的 `SkillChainPlan.ModifierPlan captured` 保持原名。

`ActorCaptureDispatcher` 补回了 `java.util.UUID`（第 30 行）。`UUID.randomUUID()` 仍在快照构造里，节流和会话判断没有改。

命名门禁退出码是 0。没有编译，没有进游戏。源码到此停止。
