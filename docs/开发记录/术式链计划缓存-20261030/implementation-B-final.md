# 作者 B 最终报告：结构资格、失效代际与公共指标

日期：2026-10-30。实现阶段，不启动正式复审。设计额度 2/2 已用完，实现额度仍按任务记为 1/3，本报告不是实现复审结论。未跑 `gradlew`、未编译、未打包、未测试、未启动游戏、未提交。源码写入已经收束。本文件之后只写 `evidence-B.json`，然后停止并释放文件。

开工 UTC：`2026-10-30T02:38:07.044Z`。写源码之前已把 10 个既有文件按项目相对路径复制到 `docs/开发记录/术式链计划缓存-20261030/backup-B/`，原 SHA256 与 UTC mtime 记在 `backup-B/original-metadata.json`。备份与当时源码哈希一致。已有未提交改动和性能插桩保留，没有 reset、clean、checkout。

## 冻结 API

`com.bslx.XueXiMod.shu.technique.ChainStructureEpoch`，`public final` 工具类，私有构造。进程内一个 `AtomicLong`，构造默认初值 0，重进服务端不归零。

- `public static long current()`：返回当前代际。
- `public static void changed()`：`incrementAndGet()`，单调加一。

`ReleaseDefinition` 新增：

```java
default boolean supportsStructuralPlanCache() { return false; }
```

契约写在该方法注释（`ReleaseDefinition.java` 第 352–362 行）：结构解析所读 getter 是无副作用纯配置，与世界、玩家、时钟无关；分类或结构配置变化完成后必须调用 `ChainStructureEpoch.changed()`；getter 返回的行为对象只在运行期使用，不能放入共享计划。未知实现保持 false，整条链不进缓存。不是术式 id 白名单。

作者 A 应消费的枚举，都追加在原枚举末尾，既有 ordinal 不变：

| 枚举 | 名称 | 新 ordinal | 说明 |
| --- | --- | --- | --- |
| `PerfCounter` | `CAST_PLAN_HITS` | 19 | 查找命中已有条目。一次 lookup 的一个终态。 |
| `PerfCounter` | `CAST_PLAN_MISSES` | 20 | 合格且可以入缓存，但没有条目。一次 lookup 的一个终态。 |
| `PerfCounter` | `CAST_PLAN_COMPILES` | 21 | 成功完整编译并放入缓存。与本次 miss 分开累加。 |
| `PerfCounter` | `CAST_PLAN_BYPASSES` | 22 | 无资格、过长、超预算、未绑定或线程不符。预算超限只记此项，不先记 miss。 |
| `PerfMetric` | `CAST_PLAN_COMPILE` | 22 | 只包真实结构编译。命中读取和旧路径不进此项。 |

`PerfMetric.CHAIN_ROLE_SCAN` 仍是 ordinal 2。注释改为实际角色扫描：编译与失配 fallback 的实际扫描都计入，命中后只读标志不计。口径由原先「`SkillSegmentRunner.runSegment` 中的角色扫描原区间 / 执行期回扫」扩大到全部实际扫描。新旧 `/shuperf` 报告按这个新口径读。`CHAIN_SPLIT` 注释和位置未改。

一次 lookup 只有 hit、miss、bypass 中的一个终态。miss 后再成功入缓存，另计 compiles。本作者没有调用 `PerformanceRecorder.add`，也没有包 `begin` / `end`。计数落点属于作者 A。

## 逐文件

### 新增 `src/main/java/com/bslx/XueXiMod/shu/technique/ChainStructureEpoch.java`

第 9–21 行。第 10 行 `AtomicLong EPOCH`。第 15–17 行 `current()`。第 19–21 行 `changed()`。不操作 LRU，不在停服或开服时归零。

无备份。最终 SHA256 `1d0f1f1c1c0685b4ff03728fb428ebe4b3fc72da9a0797fa785719826e212074`，UTC mtime `2026-10-30T02:38:28.838Z`。

### `src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseDefinition.java`

第 352–362 行新增默认方法 `supportsStructuralPlanCache()`，返回 false，并写上纯配置 getter 与变化完成后 `changed()` 的义务。其余方法未改。

备份：`docs/开发记录/术式链计划缓存-20261030/backup-B/src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseDefinition.java`。

原 SHA256 `e6a143ab58e39d722ab6613a00f861efe341afbf7bf96b1b965f89d3c592c036`，原 UTC mtime `2026-10-28T18:47:30.360Z`。

最终 SHA256 `bddbfb077219596ba533717ad50c615294b41335830b50831c8ee2804e2f8e05`，UTC mtime `2026-10-30T02:38:29.038Z`。

### `src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseDefinitions.java`

唯一的构造参数闭包在第 283–287 行覆写 `supportsStructuralPlanCache()` 并返回 true。短重载和 `cohortSword` 都进这个闭包，所以内置登记实现一起 opt-in。闭包 getter 仍只返回构造参数。没有在闭包里逐字段调用 `changed()`；登记 `put` 会递增。

备份：`docs/开发记录/术式链计划缓存-20261030/backup-B/src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseDefinitions.java`。

原 SHA256 `2979b0584625307dc454a65351990453d9cae226ce0c885ff5bb9137675c5f03`，原 UTC mtime `2026-10-28T18:56:08.915Z`。

最终 SHA256 `1b6ff094512e87144f761d83f63807696aaf7e3411f6542661e16b62a30f6c4d`，UTC mtime `2026-10-30T02:38:29.610Z`。

### `src/main/java/com/bslx/XueXiMod/shu/technique/release/selfstatus/SelfStatusDefinition.java`

类仍是 `public final`（第 12 行），未知子类不能声明，因此没有再加运行类比较。第 109–115 行本类 opt-in，返回 true。字段仍是构造时写入的 `final`，没有 setter，本类不调用 `changed()`。

备份：`docs/开发记录/术式链计划缓存-20261030/backup-B/src/main/java/com/bslx/XueXiMod/shu/technique/release/selfstatus/SelfStatusDefinition.java`。

原 SHA256 `ad2eabadb23787b078cb818bc040d1de5e93f2c37fedd2325bfdb889086a2a80`，原 UTC mtime `2026-10-28T18:58:10.124Z`。

最终 SHA256 `610bdde78ce3ee648094ff1ae8454c8a840c723ce341c5f80a094990f7bd3679`，UTC mtime `2026-10-30T02:38:29.897Z`。

### `src/main/java/com/bslx/XueXiMod/shu/technique/release/WaterReleaseDefinition.java`

类仍是 `public final`（第 12 行），同样直接本类 opt-in。第 3 行导入 `ChainStructureEpoch`。第 249–255 行 `supportsStructuralPlanCache()` 返回 true。

对象没有冻结，效果逻辑没有改，公开方法签名没有改。私有构造第 54–65 行以及尚未逃逸的工厂赋值不逐字段递增。登记前 fluent 多次递增无害。

公开 fluent 修改口，都在原赋值完成之后 `changed()`：

| 方法 | 方法行 | `changed()` 行 | 写入条件 |
| --- | --- | --- | --- |
| `swordForm()` | 68–79 | 77 | 八个字段都写完 |
| `energy(float)` | 135–139 | 137 | 赋值后 |
| `wetness(int)` | 141–145 | 143 | 赋值后 |
| `heavyMomentum(float)` | 148–154 | 151 | 仅有限且非负、原赋值发生后 |
| `pressureWetness(int)` | 157–163 | 160 | 仅 0 到 100、原赋值发生后 |
| `pull(float)` | 165–169 | 167 | 赋值后 |
| `pressure(float)` | 171–175 | 173 | 赋值后 |
| `knock(float)` | 177–181 | 179 | 赋值后 |
| `lift(float)` | 183–187 | 185 | 赋值后 |
| `unload(float)` | 189–193 | 191 | 赋值后 |
| `slow(int)` | 195–199 | 197 | 赋值后 |
| `burst(float)` | 201–205 | 203 | 赋值后 |
| `purify()` | 207–211 | 209 | 赋值后 |
| `wall()` | 213–217 | 215 | 赋值后 |
| `visual(int)` | 219–223 | 221 | 赋值后 |
| `curve(String)` | 225–229 | 227 | 赋值后，null 仍写成空串 |
| `shape(ReleaseShape)` | 231–235 | 233 | 赋值后 |
| `span(float)` | 237–241 | 239 | 赋值后 |
| `along(float)` | 243–247 | 245 | 赋值后 |

不递增的工厂：`shot` 第 81–84 行，`zone` 第 86–94 行，`barrier` 第 96–104 行，`domain` 第 106–114 行，`bodyGuard` 第 116–122 行，`conditional` 第 125–133 行。越界的 `heavyMomentum` / `pressureWetness` 仍留下原值，并且因为没有赋值而不递增。

备份：`docs/开发记录/术式链计划缓存-20261030/backup-B/src/main/java/com/bslx/XueXiMod/shu/technique/release/WaterReleaseDefinition.java`。

原 SHA256 `291c38879ff91d2626c681d0eb960e308daf6cca3f0cc463bf00bdce16591b39`，原 UTC mtime `2026-10-29T14:00:45.242Z`。

最终 SHA256 `dae15fee635fa8f3e3af37ff73d7faad9b57b5e1291c7b32becf9a06a7992abe`，UTC mtime `2026-10-30T02:38:59.882Z`。

### 四个注册器

都在原 `put` 正常返回之后调用 `changed()`。`put` 之前的空指针，或 `put` 自身抛出，都不会执行到 `changed()`。覆盖仍是一次成功 `put`，然后递增。没有改拒绝分支，没有改事件层，没有改 `LinkedHashMap`，也不声称它并发安全。

| 文件 | 变化行 | 备份相对路径 |
| --- | --- | --- |
| `src/main/java/com/bslx/XueXiMod/shu/TechniqueManager.java` | 第 3 行导入；第 13–16 行，`put` 后第 15 行 `changed()` | `backup-B/src/main/java/com/bslx/XueXiMod/shu/TechniqueManager.java` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseRegistry.java` | 第 4 行导入；第 16–19 行，`put` 后第 18 行 `changed()` | `backup-B/src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseRegistry.java` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/TechniqueEffectRegistry.java` | 同包，无导入；第 12–15 行，`put` 后第 14 行 `changed()` | `backup-B/src/main/java/com/bslx/XueXiMod/shu/technique/TechniqueEffectRegistry.java` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/EnchantmentRegistry.java` | 同包，无导入；第 13–16 行，`put` 后第 15 行 `changed()` | `backup-B/src/main/java/com/bslx/XueXiMod/shu/technique/EnchantmentRegistry.java` |

备份目录前缀都是 `docs/开发记录/术式链计划缓存-20261030/`。

| 文件 | 原 SHA256 | 原 UTC mtime | 最终 SHA256 | 最终 UTC mtime |
| --- | --- | --- | --- | --- |
| `TechniqueManager.java` | `9452f43e6b3f3c6c35f16ed5f0949d743521651e4fe0a97929e88b1d5458bb47` | `2026-10-25T08:15:12.241Z` | `eaa6b09b84628b3d81f455a5ff795db329c383491a1b863768975649836539a6` | `2026-10-30T02:38:31.610Z` |
| `ReleaseRegistry.java` | `067baeaf3e70458adcb619fa2692e405f43eb14d95950593adb3a403a83dd581` | `2026-10-25T11:20:51.282Z` | `fbc07c2b8f5cecea8f5fb19902f1cc0ba4b0125812f895bd17dd96c8f730df33` | `2026-10-30T02:38:32.785Z` |
| `TechniqueEffectRegistry.java` | `9de4d252f8c5357f8f14ef3294138d06e03fb889751ee75ea986d7fc0d8d11fb` | `2026-10-25T08:15:15.031Z` | `2e529c7222a40ded473f15da1f6d085e8813d971947f9466e29cd7575b548cac` | `2026-10-30T02:38:32.658Z` |
| `EnchantmentRegistry.java` | `edbfa5289eb5395abbff0d786cf9588db00b75cf2a182724b899af759775dd10` | `2026-10-19T17:50:12.577Z` | `657c255e2f64e3c118dc7124832462c5ddad0285086d078a1f707831a35001a7` | `2026-10-30T02:38:33.248Z` |

### `src/main/java/com/bslx/XueXiMod/debug/performance/PerfCounter.java`

第 45 行原最后一项改为带逗号。第 46–53 行追加四个 `CAST_PLAN_*`。ordinal 0–18 与原来的 `TARGET_QUERY_CALLS` 到 `EXPLICIT_SEND_CALLS` 相同。记录器按 `values().length` 分配槽位，所以没有改 `PerformanceRecorder`。

备份：`docs/开发记录/术式链计划缓存-20261030/backup-B/src/main/java/com/bslx/XueXiMod/debug/performance/PerfCounter.java`。

原 SHA256 `50acd8c5c17c03649d14ea48df78633a936f47e941269806c452307669477f55`，原 UTC mtime `2026-10-29T20:14:47.515Z`。

最终 SHA256 `93e2366c5e81f4b62a949bdfea5bfb456747d37ad2efd380208f5ab2d3ef13f9`，UTC mtime `2026-10-30T02:38:34.040Z`。

### `src/main/java/com/bslx/XueXiMod/debug/performance/PerfMetric.java`

第 12–16 行只改 `CHAIN_ROLE_SCAN` 的说明，常量顺序不变。第 54 行原最后一项改为带逗号。第 55–56 行追加 `CAST_PLAN_COMPILE`。没有改报表格式、启停、`ticks.csv` 或 `PerformanceEvents`。现有导出名循环若按 `values()` 输出，新常量会多一行名称；那是原循环，不是本次改报表。

备份：`docs/开发记录/术式链计划缓存-20261030/backup-B/src/main/java/com/bslx/XueXiMod/debug/performance/PerfMetric.java`。

原 SHA256 `1358a74f7be55499e693cb34f4372aeacc1006cd794f46e12a7aa3e7e06ed5cd`，原 UTC mtime `2026-10-29T20:14:43.705Z`。

最终 SHA256 `14ebf0fb6d3502dab9bfecdea40273d71bb2c332b2a0166f43b09c5338683bca`，UTC mtime `2026-10-30T02:38:35.783Z`。

## 保留行为

- 未知 `ReleaseDefinition` 仍默认不可缓存。
- `SelfStatusDefinition` 与 `WaterReleaseDefinition` 都是 `public final`。任务要求仅在可继承时做精确运行类限制；当前不能派生子类，所以本类直接返回 true。
- 水系工厂和私有构造在对象逃逸前不逐字段递增。
- 水系非法重水、非法重压门槛仍拒绝写入。
- 四张表仍覆盖同键。事件拒绝登记的代码未改，因此拒绝路径不会走到成功 `put`。
- 直接再调用 `register` 仍会覆盖，并在成功写入后递增代际。
- 效果和附魔覆盖也递增，即使它们不是数值缓存来源。
- 没有改网络、`PerformanceRecorder`、`PerformanceEvents`、构建配置、制作表或其他源码。
- 没有操作作者 A 的 LRU，也没有声称改善原注册表并发。

## 未运行的验证

- 未运行 `gradlew`、`compileJava`、打包、单元测试、游戏或客户端。
- 未做正式实现复审。
- 命名门禁 `powershell -NoProfile -File tools/check-name-gate.ps1` 已跑，退出码 1。报出的两处都在未改文件 `src/main/java/com/bslx/XueXiMod/animation/runtime/OriginalHumanoid.java` 第 165 行 `i`、第 202 行 `to`。按门禁规则只列出，没有顺手改名。本次改过的文件没有被点名。
- 编译要等作者 A 的网络写入收束后，由主会话统一安排。本作者没有编译授权。

## 完成情况

授权范围内的新增类、资格、水系 fluent、四注册器、两个性能枚举和本报告都已写入。没有未完成的作者 B 代码项。`evidence-B.json` 在本文件之后写入，`released` 以该文件为准。

作者 B 写完证据文件后停止写入，并释放上述源码与本目录报告文件。
