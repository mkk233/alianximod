# 作者 B 最终报告：候选查询复用统计与现有性能说明

作者 B。模型固定 Cursor CLI grok-4.7-high-fast。未换模型，未派子代理或其他 CLI。设计累计 2/2，实现累计 2/3，本任务不重置、不追加正式复审。未读作者 A 的中间源码。未跑 `gradlew`、`compileJava`、测试、打包、`runClient` 或游戏，未提交。既有未提交改动和前两步性能功能保留，没有 reset、clean、checkout。

本作者只追加统计枚举和导出/文档口径。查询调用点、helper、失效和 mixin 仍由作者 A 接入。本作者没有在任何调用点执行 `add`、`begin` 或 `end`。写入已经收束，下列四个既有文件的所有权释放。`released=true`。等作者 A 结束后才统一复核和编译。

## 新增常量

现有枚举项没有重排，也没有在中间插入。ordinal 仍是声明顺序。

### PerfCounter

文件 `src/main/java/com/bslx/XueXiMod/debug/performance/PerfCounter.java`。`CAST_PLAN_BYPASSES` 仍在第 53 行，ordinal 22。其后追加：

| 常量 | 行 | ordinal | 口径 |
| --- | --- | --- | --- |
| `CANDIDATE_QUERY_REQUESTS` | 55 | 23 | 接入候选查询 helper 的一次逻辑请求。同一次请求的 reuse、build、bypass 最多一个终态。 |
| `CANDIDATE_QUERY_REUSES` | 57 | 24 | 复用已有候选，且没有实际底层世界查询。 |
| `CANDIDATE_QUERY_BUILDS` | 59 | 25 | 真实取得候选并成功放入复用存储。异常或构建放弃不能先记此项。 |
| `CANDIDATE_QUERY_BYPASSES` | 61 | 26 | 本次走原 API，例如超限、不安全或未绑定。 |
| `CANDIDATE_QUERY_INVALIDATIONS` | 63 | 27 | 候选空间变化时真正清空已有缓存。没有清空已有缓存则不加。 |

既有项注释只改了前四项口径，名字和顺序未动：

- `TARGET_QUERY_CALLS`，第 9 行，ordinal 0。只计实际执行的目标类 `getEntitiesOfClass`。复用命中不加。与 `ENTITY_QUERY_CALLS` 互斥。
- `ENTITY_QUERY_CALLS`，第 11 行，ordinal 1。只计实际执行的实体类 `getEntitiesOfClass`。复用命中不加。`TrackingSupport` 原先不计数，接入后归此项，新旧报告口径扩大。
- `TARGET_CANDIDATES`，第 13 行，ordinal 2。每次目标请求返回、供原调用者消费的过滤后列表 `size`，包含复用。不是唯一目标，也不是扩大范围的内部扫描量。
- `ENTITY_CANDIDATES`，第 15 行，ordinal 3。每次实体请求返回、供原调用者消费的过滤后列表 `size`，包含复用。重复访问照实相加。

`SWORD_OWNED_SCAN_CALLS` 与 `SWORD_OWNED_SCANNED` 的声明未改。窄判定、更新和伤害计数的声明未改。

### PerfMetric

文件 `src/main/java/com/bslx/XueXiMod/debug/performance/PerfMetric.java`。`CAST_PLAN_COMPILE` 仍在第 56 行，ordinal 22。末尾追加：

| 常量 | 行 | ordinal | 口径 |
| --- | --- | --- | --- |
| `CANDIDATE_QUERY_LOOKUP` | 61 | 23 | helper 的包含式耗时。覆盖该次逻辑请求，含 miss 或 bypass 里的实际世界查询。不可与嵌套的 `TARGET_QUERY`、`ENTITY_QUERY` 相加。 |

既有耗时项顺序未动。注释只改了两项：

- `TARGET_QUERY`，第 22 行，ordinal 5。锁定、准星和锚点的实际底层世界查询区间。复用命中不加。射线时间仍留在此项，不进 `BLOCK_CLIP`。
- `ENTITY_QUERY`，第 34 行，ordinal 11。已列明的实际底层 `getEntitiesOfClass`。复用命中不加。仍只包查询表达式，不包随后的消费循环。`TrackingSupport` 原先不计入，接入后归此项，新旧报告口径扩大。

`SWORD_OWNED_SCAN`、`NARROW_PHASE` 及其他既有项的声明未改。

没有引入不存在的别名。报告里的 `query_calls_total` 仍是 CSV 字段名，等于 `TARGET_QUERY_CALLS + ENTITY_QUERY_CALLS`，不是新的枚举。

## 导出说明

`PerformanceRecorder.java` 只改了注释和 `note_*` 文字。存储数组、计时、启停、线程门控、CSV 列和 `csv_columns` 键没有改。

- 类注释第 22 行：写明 `CANDIDATE_QUERY_LOOKUP` 含 miss 或 bypass 中的实际世界查询，不可与嵌套的 `TARGET_QUERY`、`ENTITY_QUERY` 相加。
- `note_query_total`，第 572 行。`query_calls_total` 仍是两种实际底层查询之和。复用命中不加。两类互斥。一次请求因预算放弃构建后再走原 API 时，两次真实查询都计入。
- `note_candidates`，第 573 行。点名 `TARGET_CANDIDATES` 与 `ENTITY_CANDIDATES`：每次请求返回给原调用者的过滤后列表 `size`，包含复用；不是唯一目标，也不是扩大范围的内部扫描量。
- `note_owned_scan`，第 574 行。剑的 owned 扫描仍单独列出，不进 `query_calls_total`，也不进候选查询复用计数。
- `note_updates`，第 575 行。原文未改。
- `note_candidate_query`，第 576 行。新说明，接在原有四条 note 之后。内容覆盖请求、三个互斥终态、失效、包含式 `CANDIDATE_QUERY_LOOKUP`，以及 `TrackingSupport` 接入后实体查询口径扩大。

会话合计仍由原有 `metric ` / `counter ` 循环按 `values()` 顺序写出。因此新枚举会以原字段格式出现在 `summary.txt`，例如 `counter CANDIDATE_QUERY_REQUESTS total=` 和 `metric CANDIDATE_QUERY_LOOKUP calls=`。这不需要新的存储字段。`scratch*` 与 `session*` 的长度本来就取 `values().length`，分配代码未改；新 ordinal 只是沿这条既有长度多出一个槽。逐 tick 数组仍只保存原来的目标查询、实体查询、两类候选、攻击体更新和剑更新。

CSV 未改。`csv_columns` 仍在第 586 行，表头仍在第 619 行：

`tick_ordinal,tick_ns,observed_server_tick_ms,target_query_calls,entity_query_calls,query_calls_total,target_candidates,entity_candidates,candidate_total,hit_entity_updates,sword_updates`

命令仍是 `/shuperf`。启停状态机未改。

在作者 A 接入之前，这些新计数保持 0。本作者没有检查 A 是否已经引用它们。

## 使用说明

`docs/性能记录使用说明.md` 没有重做全文，也没有新界面。命令、状态机和导出目录仍是原来的 `/shuperf`。

- 第 3 行：指向 `docs/候选查询复用说明.md`，并写明本文只记报告口径。实现策略不在本文。
- 第 55 行：写明新计数和 `CANDIDATE_QUERY_LOOKUP` 只在 `summary.txt` 的会话合计，不增加 CSV 列。
- 第 61 行：`query_calls_total`、复用命中不加、候选数包含复用。
- 第 65–85 行：新增「候选查询计数」。写明请求与三个终态、失效、底层查询与候选数、包含式 LOOKUP、`TrackingSupport` 口径扩大、剑扫描独立，以及窄判定、更新和伤害口径不变。

本文不声称已经编译、已经实测，也不把这些计数写成 tick 耗时或帧数收益。

## 备份与哈希

修改前已按项目相对路径复制到 `docs/开发记录/候选查询复用-20261030/backup-B/`。备份内容的 SHA-256 与修改前源文件一致，备份的 UTC mtime 设回修改前的 mtime。算法 SHA-256，时间是 UTC。

| 路径 | 修改前 SHA-256 | 修改前 mtime | 最终 SHA-256 | 最终 mtime |
| --- | --- | --- | --- | --- |
| `src/main/java/com/bslx/XueXiMod/debug/performance/PerfCounter.java` | `93e2366c5e81f4b62a949bdfea5bfb456747d37ad2efd380208f5ab2d3ef13f9` | `2026-10-30T02:38:34.040Z` | `29e8aa32f8a408ebca80b77edceb4abc3639be38a16bb469b269e971d2ff4464` | `2026-10-30T03:56:53.026Z` |
| `src/main/java/com/bslx/XueXiMod/debug/performance/PerfMetric.java` | `14ebf0fb6d3502dab9bfecdea40273d71bb2c332b2a0166f43b09c5338683bca` | `2026-10-30T02:38:35.783Z` | `dcefb7fd6e9aa59ebeacdaf91838881dca268627e80f7f972decdc2c346a5c04` | `2026-10-30T03:56:55.403Z` |
| `src/main/java/com/bslx/XueXiMod/debug/performance/PerformanceRecorder.java` | `375d26b74f181922f4bac10fb47364880a62c811ecb903881035dd8105d430da` | `2026-10-29T20:58:25.984Z` | `e4feead9103572e3b29885220a577f3434d2a9df6751f9e681ad85bf4192de30` | `2026-10-30T03:57:27.074Z` |
| `docs/性能记录使用说明.md` | `13274a7cb49f76bc3869415ea6231fd1259dcd1084280da2a89cb3264a506db3` | `2026-10-29T20:18:53.449Z` | `f9b705eca48ab00de131ba08b7020b164001e6a1eb4aa8dbced59f732f12b476` | `2026-10-30T03:57:06.785Z` |

备份根目录：`docs/开发记录/候选查询复用-20261030/backup-B/`。四个备份路径与上表相对路径相同。

## 名称门禁

改完 Java 后在项目根执行了 `powershell -NoProfile -File tools/check-name-gate.ps1`。这不是 `gradlew`，也不是模组编译。退出码 1。报出的两处都在本次未修改的 `OriginalHumanoid.java`：第 165 行 `i`，第 202 行 `to`。本次改过的三个 Java 文件没有出现在结果里，因此没有顺手改那个旧文件。

## 未运行的验证

- 未运行 `.\gradlew.bat compileJava`，也没有其他 Gradle 任务。枚举能否通过编译、作者 A 的引用是否匹配，都还没有编译器结果。
- 未做测试、未打包、未启动客户端或服务器。
- 未做游戏内采样，没有 `summary.txt` 或 `ticks.csv` 的新导出样本，没有 TPS 或耗时对比。
- 未做正式实现复审。实现额度仍是 2/3。
- 未核对作者 A 的接线是否已经按上述终态记账。
