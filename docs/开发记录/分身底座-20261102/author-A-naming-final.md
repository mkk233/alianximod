先读完这份命名任务说明，再只按它写明的范围改。门禁里的本批缩写已经消失。正在写命名报告。`ClonePositionPolicy` 里的 `MIN_SEPARATION` 已改成 `MINIMUM_SEPARATION`。数值仍是 `1.0E-8D`，仍是私有常量。声明在第 13 行，比较在第 46 行。

`src/main` 里只有这两处引用，都在这个文件中，没有跨到 B 或 C。`CloneService` 第 594 行的 `state.commitLine = false` 还在。

命名门禁已执行：`powershell -NoProfile -File tools/check-name-gate.ps1`，退出码 1。本批的 `min` 缩写已经不在结果里。剩下两处是 `OriginalHumanoid.java` 的 `i` 和 `to`，这次没有改。

报告在 `docs/开发记录/分身底座-20261102/author-A-naming-report.md`。作者 A 命名补修到此停止写入。待统一编译。游戏未测。
