# 作者 A：缓存计划与当次上下文实际接线

当前阶段是已批准设计的实现，不是额外设计/审查。固定 Cursor CLI `--model grok-4.7-high-fast`，禁止切换模型、再派代理或其他 CLI。工作区 D:/alianximod。用户已授权第二步。请完整读本目录 design-decision.md 和 read-structure-final.md、read-invalidation-final.md、read-mutable-definition-final.md；落实设计中的作者 A 任务，不另做架构裁决。累计设计2/2已用完，实现1/3。你不启动正式复审。

你是下面文件的唯一源码作者：新增 src/main/java/com/bslx/XueXiMod/network/CompiledCastPlan.java、CastPlanContext.java、CastPlanCache.java、CastPlanCacheEvents.java；修改同目录 ChainSteps.java、CompiledReleaseStep.java、SkillChainPlan.java、SkillUsePacket.java、SkillSegmentRunner.java、CastExecutionSession.java、AttackBudgetWindow.java；必要时仅为共享原解析核心修改 src/main/java/com/bslx/XueXiMod 下唯一 TechniqueChain.java（用 rg 定位确切路径）。可以写 docs/术式链计划缓存说明.md、本目录 implementation-A-final.md 和自己的 backup-A/、evidence-A.json。不得写作者B文件、构建配置、制作表或其他源码；如遇范围外必需变更，最终反馈明确阻塞，不擅自扩权。

开工先保留所有未提交改动，并将每个将改的现有文件按项目相对路径复制到本目录 backup-A/，记录SHA256和UTC修改时间，确认备份已落盘后再改。禁止 reset/clean/checkout 覆盖，不回退上一性能插桩。读取仅限上述源文件及它们为理解签名实际必需的直接依赖（包括 ChainSegment、Skill、ReleaseInput、SkillSegmentMath、当前性能记录 API、服务端事件API）；不得通读整仓。

另一作者 B 同时实现冻结公共契约：shu.technique.ChainStructureEpoch.current()/changed()，ReleaseDefinition.supportsStructuralPlanCache() 默认false，合格内置定义 opt-in、注册器及Water fluent变更触发epoch；PerfCounter.CAST_PLAN_HITS/MISSES/COMPILES/BYPASSES；PerfMetric.CAST_PLAN_COMPILE。可按这些签名引用，无需等B完成。不要改B文件或用其他临时签名绕开。

必须完成设计第1、3、4、5节全部实际消费路径，不能交付只缓存 split 或未使用的骨架。复用原核心算法为 fallback/编译共用；静态缓存图仅不可变数据，成员身份保持同一segments；按次context显式传递、ModifierPlan逐次新建；延迟失配只回退原动态读取，不更换当前segments图。保留所有原分支、费用/舍入顺序、伤害/接触读取、5tick/6tick排程、随机调用。不得缓存费用、伤害、缩放冷却，不提前锁定运行期行为对象。未知定义先判资格再编译；限64链长、64条目和16384单元预算，在分配大列表前守预算，超限回旧路径但不拒绝释放。进服绑定正确服务器线程、停服清空引用。原progressSignature语义不变。

保留旧无context API作为null fallback；不给旧未知调用加入 ThreadLocal。现有性能记录仅加入真实编译和真实角色扫描计时，命中不能冒充split/scan；lookup hit/miss/bypass互斥，成功入缓存才compiles。新用户说明写清此时统计口径、缓存生命周期、动态边界、如何用既有/shuperf观察及游戏未实测。

本任务无任何编译、打包、启动游戏、测试、git提交授权；不要运行gradlew。等待所有作者完成后主会话会安排集中只读复核和唯一串行编译。实现阶段内必要的编辑自查属于你的实现，不追加正式审查任务。命名遵循现有 AGENTS/流程，避免新增单字母或含糊名称。

最终必须写完整 implementation-A-final.md：改动文件、路径行号、各接线点、复用旧语义与fallback说明、尚未完成/风险、未运行验证、逐文件最终SHA256/UTC mtime；evidence-A.json记录开始/结束UTC、修改文件、新文件、备份路径、最终hash/mtime，以及明确 released=true。最终回复确认已停止全部写入并释放文件，任何任务不完整不要报告成功。所有源码与文档写入完成后才返回最终报告。
