编译回执（task compile-final，batch release-variants-20261109）

- 真实命令：`.\gradlew.bat compileJava --offline --no-daemon`
- 开始：2026-11-09T21:57:03.654779+08:00
- 结束：2026-11-09T21:57:30.372400+08:00
- 退出码：0
- BUILD SUCCESSFUL（约 25s；4 个任务：2 执行，2 UP-TO-DATE）
- 输入未变化：`changedInputs` 为空（inputCount 1335）
- 尚未游戏验证
- 第二现象（旋流溃败）仍未确诊

stderr 仅为既有 API 过时/removal 警告，未导致失败。未改源码，未复跑。