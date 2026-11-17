我先读任务说明，按其中授权范围执行。任务范围已明确。我接着读本目录三份设计文档，只按第 2、6 节作者 B 的内容落地。设计已对齐。接下来定位授权文件、记下原哈希和时间，再按契约改资格、代际和指标。公共接口和注册点已经对上。我再核对指标枚举的序号用法，避免新常量挪动现有编号。源码里还没有这些接口。我先按相对路径备份现有文件并记下原哈希，然后再改。备份已记下。开始写入代际工具、资格契约、注册失效和性能枚举。注册和枚举已经接上。接下来给水系每个公开 fluent 写入口补上代际，并打开本类资格。源码已改完，本次动过的文件没有命名门禁问题。我现在写终稿和证据，写完就停。作者 B 的授权范围已经写完。结束 UTC 是 `2026-10-30T02:41:33.257Z`，`evidence-B.json` 的 `released` 为 `true`。源码和报告都已停止写入，这些文件现已释放。

完整记录在 `docs/开发记录/术式链计划缓存-20261030/implementation-B-final.md`。开工 UTC 是 `2026-10-30T02:38:07.044Z`。写源码前，10 个既有文件已按项目相对路径备份到 `backup-B/`，原哈希与 UTC mtime 在 `backup-B/original-metadata.json`。未跑 `gradlew`、编译、打包、测试、游戏或提交，也没有启动正式复审。

## 冻结 API

`ChainStructureEpoch` 是 `public final` 工具类。`current()` 读取 `AtomicLong`，初值 0，重进服务端不归零。`changed()` 使用 `incrementAndGet()`，只加不减。本类不操作计划缓存。

`ReleaseDefinition.supportsStructuralPlanCache()` 默认返回 `false`（第 352–362 行）。注释约定：结构 getter 是与世界、玩家、时钟无关的纯配置；分类或结构配置变化完成后必须调用 `changed()`；行为对象只在运行期使用，不能放进共享计划。未知实现不进缓存。

新枚举都追加在末尾，原 ordinal 不变：

| 枚举 | 名称 | ordinal | 口径 |
| --- | --- | --- | --- |
| `PerfCounter` | `CAST_PLAN_HITS` | 19 | 命中已有条目，一次 lookup 的一个终态 |
| `PerfCounter` | `CAST_PLAN_MISSES` | 20 | 合格且可入缓存，但没有条目 |
| `PerfCounter` | `CAST_PLAN_COMPILES` | 21 | 成功完整编译并放入缓存，与 miss 分开累加 |
| `PerfCounter` | `CAST_PLAN_BYPASSES` | 22 | 无资格、过长、超预算、未绑定或线程不符；预算超限不先记 miss |
| `PerfMetric` | `CAST_PLAN_COMPILE` | 22 | 只包真实结构编译 |

`CHAIN_ROLE_SCAN` 仍是 ordinal 2。说明改为实际角色扫描：编译和失配 fallback 的实际工作计入，命中后只读标志不计。口径从原来的执行期 `runSegment` 回扫扩大到全部实际扫描。`CHAIN_SPLIT` 未改。计数调用留给作者 A。`PerformanceRecorder`、`PerformanceEvents`、报表格式和启停都未改。

## 资格与代际

`ReleaseDefinitions` 的构造参数闭包在第 283–287 行返回 `true`。短重载和 `cohortSword` 都进入这个闭包。

`SelfStatusDefinition` 与 `WaterReleaseDefinition` 都是 `public final`，未知子类无法声明，因此没有再做运行类比较，本类直接返回 `true`。自身状态字段在构造时写完，本类没有 setter，也不调用 `changed()`。

水系 19 个公开 fluent 方法都在原赋值完成后调用 `changed()`：`swordForm` 77、`energy` 137、`wetness` 143、`heavyMomentum` 151、`pressureWetness` 160、`pull` 167、`pressure` 173、`knock` 179、`lift` 185、`unload` 191、`slow` 197、`burst` 203、`purify` 209、`wall` 215、`visual` 221、`curve` 227、`shape` 233、`span` 239、`along` 245。`heavyMomentum` 和 `pressureWetness` 只在原赋值真正发生后递增；非法值仍留下原值。`shot`、`zone`、`barrier`、`domain`、`bodyGuard`、`conditional` 和私有构造不逐字段递增。对象未冻结，效果和旧签名未改。

四个注册器都在原 `put` 成功返回后调用 `changed()`：`TechniqueManager` 第 15 行、`ReleaseRegistry` 第 18 行、`TechniqueEffectRegistry` 第 14 行、`EnchantmentRegistry` 第 15 行。`put` 前或 `put` 时抛出则不会递增。覆盖仍是一次成功写入后再递增。事件拒绝逻辑未改。没有声称修了 `LinkedHashMap` 的并发。

## 最终哈希

| 文件 | 最终 SHA256 | UTC mtime |
| --- | --- | --- |
| `ChainStructureEpoch.java`（新） | `1d0f1f1c1c0685b4ff03728fb428ebe4b3fc72da9a0797fa785719826e212074` | `2026-10-30T02:38:28.838Z` |
| `ReleaseDefinition.java` | `bddbfb077219596ba533717ad50c615294b41335830b50831c8ee2804e2f8e05` | `2026-10-30T02:38:29.038Z` |
| `ReleaseDefinitions.java` | `1b6ff094512e87144f761d83f63807696aaf7e3411f6542661e16b62a30f6c4d` | `2026-10-30T02:38:29.610Z` |
| `SelfStatusDefinition.java` | `610bdde78ce3ee648094ff1ae8454c8a840c723ce341c5f80a094990f7bd3679` | `2026-10-30T02:38:29.897Z` |
| `WaterReleaseDefinition.java` | `dae15fee635fa8f3e3af37ff73d7faad9b57b5e1291c7b32becf9a06a7992abe` | `2026-10-30T02:38:59.882Z` |
| `TechniqueManager.java` | `eaa6b09b84628b3d81f455a5ff795db329c383491a1b863768975649836539a6` | `2026-10-30T02:38:31.610Z` |
| `ReleaseRegistry.java` | `fbc07c2b8f5cecea8f5fb19902f1cc0ba4b0125812f895bd17dd96c8f730df33` | `2026-10-30T02:38:32.785Z` |
| `TechniqueEffectRegistry.java` | `2e529c7222a40ded473f15da1f6d085e8813d971947f9466e29cd7575b548cac` | `2026-10-30T02:38:32.658Z` |
| `EnchantmentRegistry.java` | `657c255e2f64e3c118dc7124832462c5ddad0285086d078a1f707831a35001a7` | `2026-10-30T02:38:33.248Z` |
| `PerfCounter.java` | `93e2366c5e81f4b62a949bdfea5bfb456747d37ad2efd380208f5ab2d3ef13f9` | `2026-10-30T02:38:34.040Z` |
| `PerfMetric.java` | `14ebf0fb6d3502dab9bfecdea40273d71bb2c332b2a0166f43b09c5338683bca` | `2026-10-30T02:38:35.783Z` |

报告文件 SHA256 是 `7a5e8a5e12d34486b0e62f58bcf195d3345e0a8f4a0df7485b58624889845399`。

## 未验证与未完成

命名门禁退出码 1。两处都在未改的 `OriginalHumanoid.java` 第 165 行 `i`、第 202 行 `to`，没有顺手改名。本次改过的文件没有被点名。

授权范围内没有未完成的代码项。编译要等网络侧写入收束后统一进行。作者 B 已停止写入并释放上述文件。
