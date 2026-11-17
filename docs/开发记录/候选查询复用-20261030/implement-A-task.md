# 作者A：步骤3候选查询复用、失效与全部查询接线

固定Cursor CLI --model grok-4.7-high-fast，工作区D:/alianximod。不换模型、不派子代理/其他CLI。用户已授权继续步骤3“减少重复候选实体查询”；先完整读本目录记录.md、read-hit-queries-final.md、read-query-lifecycle-final.md。设计额度累计2/2已用完，不追加设计审查/架构裁决；这里是已授权方向的实际实现，常规实现选择由你在下列范围和行为约束内完成。实现复核仍为2/3，等所有写入收束后主会话统一安排剩余一轮；你不另开正式复审。无法在该范围内实现时返回真实阻塞，不扩张到C或异步。

目标是实际减少重复LivingEntity空间候选查询及临时工作：密集攻击体/飞剑在附近、同tick且世界候选结构未变化时应能复用底层候选，而每次查询仍按当前精确AABB和原谓词过滤，命中仍走原实时窄判定/有效性/伤害流程。不能只包一层未消费工具类、只复用空列表、只改计数或把跨tick陈旧结果称作完成。缓存只保存可再过滤的候选集合，不能缓存最终目标、canHit结果、伤害、锁定选择或其他玩法判断。

唯一源码作者范围（都位于src/main/java/com/bslx/XueXiMod）：可在shu/technique/query/新增候选查询服务及必要数据/生命周期类型；可在mixin/下新增本功能必要的公共实体/section失效mixin，修改src/main/resources/xueximod.mixins.json仅注册自己新增项（保留全部现有项顺序/配置）。可修改已定位的TechniqueHitEntity.java、TechniqueHitSettle.java、SwordEntityMotion.java、HitVolumes.java、TrackingSupport.java、LockTargetService.java、ReleaseBatch.java、ContactEnchantState.java，仅替换候选获取和对应计数计时边界。用rg确认这些唯一类的准确路径。必要的现有公共mixin只读，默认用独立新增mixin避免改其玩法行为。不要修改SwordEntity所属扫描/LAUNCHING算法、第二步network链结构缓存、世界伤害规则或未列出的术式实现。可读这些文件的直接依赖、当前性能API、现有生命周期/mixin格式，以及两报告所引用的本机Forge/Minecraft sources jar中所需方法；不通读整仓、不下载依赖。

文档写入权：docs/候选查询复用说明.md；本目录implementation-A-final.md、evidence-A.json、backup-A/及必要的自己证据文件。开始前将所有拟改的现有文件按项目相对路径备份backup-A/并记录原hash/UTC mtime，确保保存成功再写；已完成的前两步和其他未提交改动必须保留，禁止reset/clean/checkout覆盖。

行为与范围约束：
- 主线程访问世界，不异步、不线程池、不复制活世界对象到后台。必须绑定正确MinecraftServer identity/线程、按世界区分，停服和世界卸载清理引用；tick边界清理/过期，不跨服复用；缓存条目、覆盖区域、候选总量和超大查询均有硬上限，超限回原API，不截断正常释放/候选。
- 同tick并不代表候选未变。复用有效性必须涵盖LivingEntity位置/仅包围盒变化、同section移动、跨section、加入移除、section可访问性切换和加载卸载；只订阅Join/Leave或onMove不足。依据已读真实源码选择最小必要失效接点，必须照顾内部写入顺序和可重入回调，避免新实体/新移入目标漏查。不能用最终filter作为新增候选的替代。检测挂点未就绪/不支持的情况须保守走原API。
- 缓存候选覆盖范围必须包含原AABB的原版完整查询集合，不能从小盒结果推大盒，也不能依赖实体中心一定在盒中。扩展区域结果过滤回小盒时必须保留原版section/实体相对顺序，包括负坐标、边界和同距目标选择。不要排序、更换实体身份或去重改变原行为。候选空间变化后及时失效；活性/旁观者/所属/已命中过的过滤仍按原时点逐请求计算。
- 列表供每次消费时独立，不让调用方改共享候选；支持查询嵌套/重入，不共享可变scratch列表。查询的谓词按原AABB相交检查之后执行，不对扩大区域外的实体多执行原谓词。不吞异常或重复执行有副作用的谓词来“重试”。只有能保留语义的既有调用方接入；不能泛化接管未知全局Level查询/全部mod调用。不能改变原查询之后逐个目标处理时已经取得的这一次列表语义。
- 不把所有非Living攻击体的常规移动都当作Living空间改变，避免幻剑/攻击体移动把复用完全冲掉；同时不能漏掉Living视觉载体/新加入Living造成的真实变化。不要加入每请求全世界扫描来验证缓存，也不要用重复查询全世界作“验证”。大型/稀疏/不支持请求保守fallback。
- 保留陷阱、引爆、弧带、普通命中、剑命中五个上限、所有回调/附魔/无敌帧/费用/5tick和6tick时序、随机调用次序。锁定每次重新选目标，ReleaseBatch.anchor继续原冻结语义。原有getAllEntities剑所属扫描本步保持。

作者B并行独占PerfCounter.java、PerfMetric.java和PerformanceRecorder.java的说明文字，你不要修改这些文件。冻结统计常量：PerfCounter新增CANDIDATE_QUERY_REQUESTS、CANDIDATE_QUERY_REUSES、CANDIDATE_QUERY_BUILDS、CANDIDATE_QUERY_BYPASSES、CANDIDATE_QUERY_INVALIDATIONS；PerfMetric新增CANDIDATE_QUERY_LOOKUP。请求=接入helper的一次逻辑候选请求；reuse=无需实际世界查询而用已有候选；build=实际取候选并成功放入复用存储；bypass=本次直接原API（超限/不安全/未绑定等）；invalidations=因候选结构变化真正清掉已有缓存的次数。每次请求reuse/build/bypass最多一终态，真正构建失败且fallback不能先报build。计数关闭快路径沿用PerformanceRecorder门控，不拼接日志。

原TARGET_QUERY_CALLS/ENTITY_QUERY_CALLS和其TARGET_QUERY/ENTITY_QUERY计时只统计真实底层空间查询，复用命中不能增加；候选访问数仍按每次请求返回、交调用者消费的过滤后列表size累计（含复用），不是去重目标或扩大区域内部扫过数。如果一次请求因预算放弃构建而再次原API，真实两次查询必须都计数。helper的CANDIDATE_QUERY_LOOKUP覆盖请求自身含miss查询，仍为包含式计时不可与内部相加。不要调用方和helper双重记账；新增接入的TrackingSupport原本未计数，接入后归ENTITY_QUERY并在文档标注口径扩大。原NARROW_PHASE/更新/伤害计数不改。

本任务无编译/测试/打包/runClient/游戏/提交授权，不运行gradlew。必要编辑自查属于实施，但不组织新设计或正式复审。所有写入完结才返回最终报告。报告需列实际接入入口、没接入的入口和原因、缓存上界/键/失效真实方法行号、原查询顺序与副作用保留事实、原API回退、计数边界、改动文件/最终hash/UTCmtime、新增文件/备份、未验证项和风险，说明未编译未游戏实测。evidence-A.json含startedUtc/endedUtc/released=true、每文件路径hash/mtime和备份。没有完成不得报成功。
