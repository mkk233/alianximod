# 作者 B：结构资格、失效代际与公共指标

当前阶段为已批准设计的实现，不是额外设计/审查。固定 Cursor CLI `--model grok-4.7-high-fast`，禁止切模型、其他CLI或再派代理。工作区 D:/alianximod；用户已授权第二步。完整读本目录 design-decision.md、read-invalidation-final.md、read-mutable-definition-final.md，落实第2和6节作者B任务。累计设计2/2已用完，实现1/3，不启动正式复审。

你是以下文件唯一作者：新增 src/main/java/com/bslx/XueXiMod/shu/technique/ChainStructureEpoch.java；修改 src/main/java/com/bslx/XueXiMod 下唯一 ReleaseDefinition.java、ReleaseDefinitions.java、WaterReleaseDefinition.java、SelfStatusDefinition.java、TechniqueManager.java、ReleaseRegistry.java、TechniqueEffectRegistry.java、EnchantmentRegistry.java（用rg确定各绝对路径）；修改 debug/performance/PerfCounter.java、PerfMetric.java。可以写本目录 implementation-B-final.md、backup-B/、evidence-B.json。不得修改network、PerformanceRecorder/PerformanceEvents、注册事件拒绝逻辑、构建配置、其他源码或制作表。

开工先将每个拟修改现有文件按项目相对路径备份本目录backup-B/，记录原hash/UTC mtime后再写。保留所有已有未提交改动和性能插桩；禁止reset/clean/checkout覆盖。读取只限上述文件和签名实际依赖，不读整仓。

冻结对外API：public final工具 ChainStructureEpoch，public static long current()，public static void changed()，AtomicLong单调代际。ReleaseDefinition新增default boolean supportsStructuralPlanCache(){return false;}，在契约文档写明结构getter纯配置和变化完成后changed义务；未知实现默认不缓存。

内置ReleaseDefinitions闭包opt-in；SelfStatusDefinition和WaterReleaseDefinition本类opt-in，若可继承须精确运行类限制防止未知子类自动继承资格。Water全部公开fluent修改方法在原赋值完成后changed，不只挑选当前影响结构的方法；不得冻结对象/改效果/改旧setter签名。四注册器原成功put之后changed，保留原覆盖、拒绝、异常行为。登记前builder多次递增无害，私有构造未逃逸赋值无需逐字段bump。不直接操作作者A的LRU，不声称改善旧注册表并发。

增加PerfCounter.CAST_PLAN_HITS、CAST_PLAN_MISSES、CAST_PLAN_COMPILES、CAST_PLAN_BYPASSES；增加PerfMetric.CAST_PLAN_COMPILE。沿用现有enum风格、名称/标签，现有编号语义不被误用；CHAIN_ROLE_SCAN说明改为实际角色扫描（编译与fallback实际工作，命中不扫描）。不改report格式和启停逻辑。A将消费这些确切枚举与API。

本任务禁止gradlew/编译、打包、测试、游戏、提交。A同时写全部network接线，所有源码写入收束后主会话统一安排集中复核和串行编译。不要审查作者A中间工作。设计明确以外有阻塞，保留现场最终报告，不自行扩展设计。

最终写完整implementation-B-final.md：每文件行号、变化、枚举/API、所有Water修改口列表、保留行为、未运行验证、逐文件最终SHA256/UTC mtime；evidence-B.json记录开始/结束UTC、修改/新文件、备份路径、最终hash/mtime和released=true。完成全部写入后返回最终报告，并明确已停止写入且释放文件。未完成内容如实说明。
