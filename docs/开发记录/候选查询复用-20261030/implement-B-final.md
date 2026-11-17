我先读取任务说明和相关执行规范，确认授权范围后再动手。任务范围已确认：只加候选查询统计和说明，先读契约与现有性能代码，再备份后改。契约已对齐。我先按相对路径备份现有文件并记下哈希与 UTC 时间，再只改统计常量和说明。说明文字已经写入。我核对导出注记和 CSV 是否只动了允许的部分，并跑名称门禁。导出注记和 CSV 列已核对，名称门禁只剩未改文件里的旧问题。正在写入最终报告和证据。作者 B 的写入已经收束并释放。设计仍是 2/2，实现仍是 2/3，没有正式复审，也没有编译或游戏实测。完整记录在 `docs/开发记录/候选查询复用-20261030/implementation-B-final.md`。

## 新增常量

现有枚举没有重排，也没有在中间插入。ordinal 仍是声明顺序。本作者只追加枚举和说明，没有在查询调用点执行 `add`、`begin` 或 `end`。作者 A 接入之前，这些新计数保持 0。

`PerfCounter.java` 里，`CAST_PLAN_BYPASSES` 仍在第 53 行，ordinal 22。其后追加：

| 常量 | 行 | ordinal | 口径 |
| --- | --- | --- | --- |
| `CANDIDATE_QUERY_REQUESTS` | 55 | 23 | 接入候选查询 helper 的一次逻辑请求。同一次请求的 reuse、build、bypass 最多一个终态。 |
| `CANDIDATE_QUERY_REUSES` | 57 | 24 | 复用已有候选，且没有实际底层世界查询。 |
| `CANDIDATE_QUERY_BUILDS` | 59 | 25 | 真实取得候选并成功放入复用存储。异常或构建放弃不能先记此项。 |
| `CANDIDATE_QUERY_BYPASSES` | 61 | 26 | 本次走原 API，例如超限、不安全或未绑定。 |
| `CANDIDATE_QUERY_INVALIDATIONS` | 63 | 27 | 候选空间变化时真正清空已有缓存。没有清空已有缓存则不加。 |

前四项只改了注释：`TARGET_QUERY_CALLS` 第 9 行、`ENTITY_QUERY_CALLS` 第 11 行只计实际 `getEntitiesOfClass`，复用命中不加，两类互斥。`TrackingSupport` 原先不计数，接入后归 `ENTITY_QUERY_CALLS`，新旧报告口径扩大。`TARGET_CANDIDATES` 第 13 行、`ENTITY_CANDIDATES` 第 15 行按每次请求返回给原调用者的过滤后列表 `size` 累计，包含复用；不是唯一目标，也不是扩大范围的内部扫描量。剑的 owned 扫描、窄判定、更新和伤害计数的声明未改。

`PerfMetric.java` 里，`CAST_PLAN_COMPILE` 仍在第 56 行，ordinal 22。末尾追加 `CANDIDATE_QUERY_LOOKUP`，第 61 行，ordinal 23。它是 helper 的包含式耗时，含 miss 或 bypass 里的实际世界查询，不可与嵌套的 `TARGET_QUERY`、`ENTITY_QUERY` 相加。`TARGET_QUERY` 第 22 行、`ENTITY_QUERY` 第 34 行的注释改为只计实际底层查询，复用命中不加；射线仍留在 `TARGET_QUERY`。`TrackingSupport` 接入后归 `ENTITY_QUERY`。

`query_calls_total` 仍是 CSV 字段，等于 `TARGET_QUERY_CALLS + ENTITY_QUERY_CALLS`，不是新枚举。

## 说明变更

`PerformanceRecorder.java` 只改了注释和 `note_*`。存储分配、计时、启停、线程门控和 CSV 列未改。类注释在第 22 行。`note_query_total` 第 572 行、`note_candidates` 第 573 行、`note_owned_scan` 第 574 行已按上面的口径改写。`note_updates` 第 575 行原文保留。新的 `note_candidate_query` 在第 576 行，接在原有四条 note 之后。一次请求若放弃构建后再走原 API，两次真实查询都计入。

会话合计仍由原有 `metric ` / `counter ` 循环按 `values()` 写出，所以新枚举会以原格式出现在 `summary.txt`。`scratch*` 与 `session*` 的长度本来就取 `values().length`，分配代码未改。逐 tick 数组和 CSV 列未增加。`csv_columns` 仍在第 586 行，表头仍在第 619 行：

`tick_ordinal,tick_ns,observed_server_tick_ms,target_query_calls,entity_query_calls,query_calls_total,target_candidates,entity_candidates,candidate_total,hit_entity_updates,sword_updates`

`docs/性能记录使用说明.md` 没有重做全文。第 3 行指向 `docs/候选查询复用说明.md`，并写明本文只记口径。第 55 行说明新计数不进 CSV。第 61 行更新查询次数和候选数口径。第 65–85 行新增「候选查询计数」。操作仍是 `/shuperf`。本文不声称已经编译、已经实测，也不写成 tick 耗时收益。

## 哈希、备份与未跑的验证

修改前备份在 `docs/开发记录/候选查询复用-20261030/backup-B/`，相对路径与源文件相同。SHA-256 如下。

| 路径 | 修改前 | 最终 | 最终 mtime |
| --- | --- | --- | --- |
| `PerfCounter.java` | `93e2366c5e81f4b62a949bdfea5bfb456747d37ad2efd380208f5ab2d3ef13f9` | `29e8aa32f8a408ebca80b77edceb4abc3639be38a16bb469b269e971d2ff4464` | `2026-10-30T03:56:53.026Z` |
| `PerfMetric.java` | `14ebf0fb6d3502dab9bfecdea40273d71bb2c332b2a0166f43b09c5338683bca` | `dcefb7fd6e9aa59ebeacdaf91838881dca268627e80f7f972decdc2c346a5c04` | `2026-10-30T03:56:55.403Z` |
| `PerformanceRecorder.java` | `375d26b74f181922f4bac10fb47364880a62c811ecb903881035dd8105d430da` | `e4feead9103572e3b29885220a577f3434d2a9df6751f9e681ad85bf4192de30` | `2026-10-30T03:57:27.074Z` |
| `docs/性能记录使用说明.md` | `13274a7cb49f76bc3869415ea6231fd1259dcd1084280da2a89cb3264a506db3` | `f9b705eca48ab00de131ba08b7020b164001e6a1eb4aa8dbced59f732f12b476` | `2026-10-30T03:57:06.785Z` |

修改前 mtime 依次是 `2026-10-30T02:38:34.040Z`、`2026-10-30T02:38:35.783Z`、`2026-10-29T20:58:25.984Z`、`2026-10-29T20:18:53.449Z`。备份哈希与修改前一致。

`implementation-B-final.md` 的 SHA-256 是 `6b21355e69a89265926036402afbf4083e4325fa327f4c4a6ddbb3ffb0ee075d`，mtime `2026-10-30T03:59:02.039Z`。`evidence-B.json` 的 SHA-256 是 `c4763f7d8f4587b560e31a9764d5cbdb5da79d3a9145ac66e16d5ae9a2c7fe58`，mtime `2026-10-30T03:59:20.153Z`。证据里 `startedUtc` 是 `2026-10-30T03:56:19.813Z`，`endedUtc` 是 `2026-10-30T03:59:10.504Z`，`released` 是 `true`。

名称门禁退出码 1。两处都在未修改的 `OriginalHumanoid.java`：第 165 行 `i`，第 202 行 `to`。本次三个 Java 文件没有报错，所以没有改那个旧文件。这不是模组编译。

未运行 `compileJava`、测试、打包和游戏。没有新的导出样本，也没有耗时对比。未读作者 A 的中间源码，未核对接线是否已经按终态记账。
