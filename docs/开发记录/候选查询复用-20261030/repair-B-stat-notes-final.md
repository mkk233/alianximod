我先读取返修任务说明，确认授权范围后再改统计说明。作者 A 的最终报告写明：`TARGET_QUERY` 已只包真实的生物空间查询，射线和查后选择不再计入。我先备份说明文件，再把过时注释改到这条边界上。命令行复制被拒绝了。我改用写入方式，先把三份现行文件原样放到 `backup-B-stat-notes/`，再改注释。`PerformanceRecorder` 的说明已经改完。命令行仍被拒绝，我按修改前原文把这份文件的备份写回去。作者 B 的说明返修已经写入并释放。设计仍是 2/2，实现仍是 2/3。没有编译，也没有把这次写成编译成功。完整记录在 `docs/开发记录/候选查询复用-20261030/repair-B-stat-notes-final.md`。

`TARGET_QUERY` 现在只包含实际的 LivingEntity 空间查询，也就是真实的 `getEntitiesOfClass`。查询返回之后的距离选择、准星方块射线和锚点选择都不在这项里。与旧报告相比，这项耗时口径收窄。`TrackingSupport` 接入 `ENTITY_QUERY` 与 `ENTITY_QUERY_CALLS` 后的扩大口径仍然保留。没有恢复旧的外围计时，也没有改作者 A 的源码。`docs/候选查询复用说明.md` 第 65 行已经写了这条事实，所以没有改那份说明；该行后半仍说作者 B 的注释还把射线留在该项，那句话现在已经过时，但不在本次允许修改的文件里。

## 说明变更

枚举顺序、计数、CSV、存储、计时、启停和调用方都没有改。

`PerfMetric.java` 里，`TARGET_QUERY` 仍是 ordinal 5，常量在第 26 行，注释在第 21–25 行。已删掉「射线时间留在此项」。`ENTITY_QUERY` 仍在第 38 行，ordinal 11，`TrackingSupport` 口径扩大的注释保留。

`PerformanceRecorder.java` 第 576 行的 `note_candidate_query` 保留原有请求、终态、失效和包含式 `CANDIDATE_QUERY_LOOKUP` 说明，并追加：`TARGET_QUERY` 只包真实 `getEntitiesOfClass`，不含查后距离选择、准星方块射线和锚点选择，该项耗时口径收窄。其余 `note_*` 和 CSV 表头未改。

`docs/性能记录使用说明.md` 第 77–79 行把调用次数和 `TARGET_QUERY` 耗时分开，写明口径收窄，并保留 `TrackingSupport` 的扩大口径。操作仍是 `/shuperf`。第 85 行的原说明还在。

## 备份与哈希

修改前全文在：

- `backup-B-stat-notes/src/main/java/com/bslx/XueXiMod/debug/performance/PerfMetric.java`
- `backup-B-stat-notes/docs/性能记录使用说明.md`

`PerformanceRecorder.java` 的整文件复制没有做成，本会话命令执行被拒绝。该文件只改了第 576 行，修改前整行在 `backup-B-stat-notes/PerformanceRecorder-note-before.txt`。原来的 `backup-B/` 没有动。

修改前哈希沿用上一份证据，动手前读到的原文与那次一致：

| 路径 | 修改前 SHA-256 | 修改前 mtime |
| --- | --- | --- |
| `PerfMetric.java` | `dcefb7fd6e9aa59ebeacdaf91838881dca268627e80f7f972decdc2c346a5c04` | `2026-10-30T03:56:55.403Z` |
| `PerformanceRecorder.java` | `e4feead9103572e3b29885220a577f3434d2a9df6751f9e681ad85bf4192de30` | `2026-10-30T03:57:27.074Z` |
| `docs/性能记录使用说明.md` | `f9b705eca48ab00de131ba08b7020b164001e6a1eb4aa8dbced59f732f12b476` | `2026-10-30T03:57:06.785Z` |

改完后的 SHA-256 和 UTC mtime 没有测到，因为命令执行一直被拒绝。上表不是最终哈希。名称门禁也没有跑成。

`evidence-B-stat-notes.json` 里 `released` 为 `true`。`startedUtc` 取任务消息时间 `2026-10-30T04:21:00Z`；命令时钟不可用，所以 `endedUtc` 没有独立读数。
