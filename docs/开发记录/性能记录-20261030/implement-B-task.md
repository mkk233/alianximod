# B：释放链性能插桩

你是唯一 B 作者，Cursor CLI --model grok-4.7-high-fast；不委托子代理。用户只授权性能记录。本阶段实现，设计裁决1/2已完成，实现正式复审0/3，不自行追加复审或测试。先完整读取本目录 design-decision.md 和 read-release-final.md。

唯一可写源码均在 D:/alianximod/src/main/java/com/bslx/XueXiMod 下，按精确文件名确认唯一位置：SkillUsePacket.java、TechniqueChain.java、SkillSegmentRunner.java、ChainScheduler.java、LockTargetService.java、ReleaseBatch.java、CastFrame.java、ContactEnchantState.java。仅八个文件。另可写本目录 implementation-B-final.md、implementation-B-evidence.json、before-B/ 备份。其他源码只读；A正在实现debug/performance公共接口，C负责实体文件，禁止修改他们文件或读取其未完成工作。

完整执行 design-decision.md 指定的本组计时/计数范围和精确API，不改公共接口：PerformanceRecorder.begin()/end(PerfMetric,long)/add(PerfCounter,long)/isRecording()，包com.bslx.XueXiMod.debug.performance。先声明long，再原逻辑try/finally；不能改变原异常/返回/原finally恢复顺序。开关不能控制业务本体。A文件暂时不存在也照冻结契约引用，不自行补建。

保留原来每次查询、随机取数、资源扣除、调度和玩法时序。只在两个真实action.run调用处测SCHEDULED_ACTION，不包整个schedule。TARGET_QUERY跳过resolveAnchor缓存命中；候选计数为原getEntitiesOfClass返回size，不能重查或在predicate计数。角色扫描与段执行明确嵌套。Tally.record/flush分开。ContactEnchantState只有本作者插ENTITY_QUERY。当前源码的双重释放间隔等与旧文档差异不属于本任务，不修改。

先备份八个现有源文件到before-B/并记录原hash，保留所有既有未提交改动；禁止reset/clean/checkout、提交、制作表操作、打包、游戏；git单次-c safe.directory=D:/alianximod。只读按具体依赖定位，不整仓通读。

无编译授权，不运行gradlew或独立测试，不更改配置。只做实现必要定向完整性检查。难点超出冻结设计时报告，不擅自架构变更或降模型。

最终写implementation-B-final.md与evidence.json：修改路径/行号、覆盖的指标与计数、保留语义的处理、未覆盖/阻塞、待统一编译和未游戏实测、各文件最后写入UTC时间/hash，明确本作者全部写入结束并释放八个文件。只返回一次完整最终结果，主会话不会实时读取工作过程。
