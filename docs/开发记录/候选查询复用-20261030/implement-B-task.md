# 作者B：候选查询复用统计与现有性能说明

固定Cursor CLI --model grok-4.7-high-fast，工作区D:/alianximod，不换模型、不派其他代理/CLI。读本目录记录.md、两份read-*-final.md中性能相关段、implement-A-task.md的统计契约。只实现下述指标和说明，不做新设计/正式复审。设计累计2/2、实现2/3，不重置；等作者A结束后才统一复核/编译。

唯一允许修改：src/main/java/com/bslx/XueXiMod/debug/performance/PerfCounter.java、PerfMetric.java、PerformanceRecorder.java（只导出说明/注释，不改存储、计时/启停和线程逻辑）；docs/性能记录使用说明.md。可写本目录implementation-B-final.md、evidence-B.json、backup-B/。其他源码/配置/表格不要动。修改前按相对路径备份所有现有文件到backup-B/并记录hash/UTCmtime；保留所有原未提交改动及两步性能功能。

冻结API：在末尾追加PerfCounter.CANDIDATE_QUERY_REQUESTS、CANDIDATE_QUERY_REUSES、CANDIDATE_QUERY_BUILDS、CANDIDATE_QUERY_BYPASSES、CANDIDATE_QUERY_INVALIDATIONS，保持现有ordinal。PerfMetric末尾追加CANDIDATE_QUERY_LOOKUP，保持既有项顺序。

语义：requests为一次接入helper的逻辑请求；reuse为复用候选且没有实际底层查询；build为真实取得候选并成功入复用存储；bypass为本次走原API；invalidations为候选空间变化时真正清空已有缓存。每次请求reuse/build/bypass最多一个终态；异常/构建放弃不能先虚报build。LOOKUP是helper包含式耗时，包含miss或bypass里的实际世界查询，不可与嵌套QUERY相加。

现有TARGET_QUERY_CALLS/ENTITY_QUERY_CALLS与TARGET_QUERY/ENTITY_QUERY只算实际底层世界查询，复用命中不加；query_calls_total仍是这两种实际调用总和。TARGET_CANDIDATES/ENTITY_CANDIDATES按每次请求返回、供原调用者消费的过滤后列表size累计，包含复用（不是唯一目标，也不是扩大范围内部扫描数）。根据实际现有常量名核对，勿引入不存在的别名。TrackingSupport此前没计数，这次接入归ENTITY，说明新旧报告口径扩大；Sword owned扫描仍独立。保留原汇出字段格式，可调整note_*文字解释上述区别；不要改CSV列或启停机制。

文档只写指标与统计口径、操作仍为原/shuperf，引用docs/候选查询复用说明.md由作者A负责实际实现策略。不提前声称已编译/已实测/TPS收益。无需重设计整份文档或新界面。不读/审A中间源码。

无任何gradlew/编译、测试、打包、启动游戏或提交授权。最终完整报告新增常量、说明变更、具体文件行号、最终hash/UTCmtime/备份、未运行验证；evidence-B.json含startedUtc/endedUtc/released=true和每文件证据。所有写入收束后返回并释放。
