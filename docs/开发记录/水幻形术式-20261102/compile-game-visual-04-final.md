我先完整读取这份返修编译任务说明，再按当前授权范围执行。这是缺臂修复后的统一编译。我先读 04b 报告，并确认没有别的 Gradle 在跑。04b 已停写：诊断类已删，绘制顺序已改。没有正在进行的 Gradle 编译。开始保存源码快照。缺臂代码修复的 `compileJava` 已通过。这还不是游戏实测通过。`.\gradlew.bat compileJava --offline --no-daemon` 于 2026-11-03 00:52:25–00:52:54 实际执行，退出码 **0**，`BUILD SUCCESSFUL in 28s`。`:compileJava` 不是 UP-TO-DATE。javac 汇总 **41 个警告、0 个错误**。

`src` 共 1263 个文件，编译前后 SHA256 差异 0。最后写入是 `SteveWaterLineVisual.java`，时间 00:48:19。`SteveWaterLineVisual`、`SteveWaterLineMesh`、`CloneRenderer` 三个类已在 00:52:48 前后写入 `build/classes`。诊断源文件已不在 `src` 里，`build` 下也没有 `WaterPhantomVisualDiagnostics` 输出。命名门禁退出码 0。贴图和 `lines.bin` 与 `src` 哈希相同，没有跑 `processResources`。03 的诊断记录没有覆盖。

作者报告的根因是皮肤顶点写进了水花贴图：水花缓冲取得太早，空的皮肤批次先被提交。修复是先写完皮肤再取水花缓冲。

请完整重启当前开发客户端。F3+T 或只重进世界换不上这些类。重启后看双臂是否都在、双腿是否都是水线颜色而不是一侧发白，并再看一次散水。未打包，没有操作游戏，未追加正式复审。报告在 `docs/开发记录/水幻形术式-20261102/compile-game-visual-04-report.md`。
