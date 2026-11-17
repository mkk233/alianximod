先读完整任务说明，再按其中授权范围执行。日志确认 78 和 115 行是 JDK 17 的模式类型错误：`getLevel` 的静态类型已经是 `ServerLevel`。这两处改成局部赋值和空检查，空、线程和维度判断保持原样。`ClonePresentation.java` 里两处 JDK 17 模式类型错误已经改成局部赋值和空检查。

`MinecraftServer.getLevel` 的静态类型本来就是 `ServerLevel`，原来的 `instanceof ServerLevel` 不能通过编译。非空时它恒成立，实际挡住的是未加载维度返回的 `null`。

- `emit` 78–82 行：取出 `state.dimension` 对应的关卡，为空则摘掉发布键并返回。
- `emitVolume` 116–120 行：对票据维度做同样的空检查。

同线程、会话、维度键、观众判断和通知字段都保持原样。命名门禁退出码 0。未编译。`SkillUsePacket.java` 和 `ActorCaptureDispatcher.java` 的另外两处错误不在本次文件内，没有改。报告在 `docs/开发记录/水幻形术式-20261102/fix-compile-a-report.md`。写入到此停止。
