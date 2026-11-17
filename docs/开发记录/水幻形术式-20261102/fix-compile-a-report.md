# 水幻形 A 组 javac 修复报告

只修 `ClonePresentation.java` 的两处 JDK 17 模式类型错误。没有新开设计或实现复审。设计额度仍是 2/2，实现额度仍是 2/3。

**未编译。** 本报告写完后停止写入。

命名门禁已执行 `powershell -NoProfile -File tools/check-name-gate.ps1`，退出码 0。

## 日志定位

`docs/开发记录/水幻形术式-20261102/compile-attempt-1/compile-nonvisual-report.md` 与 `compile-nonvisual-log-utf8.txt` 第 128、131 行：

- `ClonePresentation.java:78`：模式类型 `ServerLevel` 是表达式类型 `ServerLevel` 的子类型。
- `ClonePresentation.java:115`：同一错误。

`MinecraftServer.getLevel` 的静态返回类型已经是 `ServerLevel`。原 `instanceof ServerLevel` 在非空时恒成立，真正挡住的是未加载维度返回的 `null`。

## 变更

只改 `src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/ClonePresentation.java`。

- `emit`：78–82 行。`ServerLevel level = server.getLevel(state.dimension);`，`level == null` 时摘掉本次发布键并返回。
- `emitVolume`：116–120 行。`ServerLevel level = server.getLevel(dimension);`，同样只在 `null` 时摘键返回。

前面的空服务器、空状态或空票据、同线程、有限坐标、单位视线、发布去重和 `CloneRuntime.get(server)` 仍在。维度键仍取自原状态或票据。观众、序号和通知字段未改，发布仍在这些检查之后同步进行。

另外两处 javac 错误在 `SkillUsePacket.java` 与 `ActorCaptureDispatcher.java`，不在本次文件内，没有改。
