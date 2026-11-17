我先读这份任务说明，确认授权范围后再按它执行。任务是已批准方案的实现，我先读方案和相关源码，再按文件所有权改领域停维护。基线已对齐：领域存续扣款只在 `DomainService.tick`，工厂仍把 8 和 20 当作维护。我先把三份原文件快照进证据目录，再停掉这套维护。命名检查里，我改过的三个文件没有新问题。接着把文件清单写入证据目录。领域存续维护已停。雾雨和深海重压仍按现有生成费放出，到期以及主人死亡、下线、跨维时关闭；存在期间不再扣维护，也不再因付不起维护而关闭。写入已收束，待统一编译。未经新派发，不再写这些文件。

## 改过路径

- `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\domain\DomainDefinition.java`，LastWriteTime `2026-10-29 22:00:22.612`
- `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\domain\DomainService.java`，LastWriteTime `2026-10-29 22:00:23.918`
- `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\WaterReleaseDefinition.java`，LastWriteTime `2026-10-29 22:00:45.242`

## 基线快照

改前全文在 `D:\alianximod\docs\开发记录\领域附魔底座实施-20261029\lifecycle-evidence\before\`，相对结构与源码一致。清单在同目录 `file-list.txt`。

| 快照 | 原文件时间 | 字节 |
|---|---|---|
| `...\before\...\domain\DomainDefinition.java` | 2026-10-25 05:40:21.334 | 3747 |
| `...\before\...\domain\DomainService.java` | 2026-10-27 04:39:02.204 | 8602 |
| `...\before\...\release\WaterReleaseDefinition.java` | 2026-10-26 20:28:28.847 | 12192 |

## 具体变化

`DomainService.tick`（118–135 行）在主人无效或寿命到期时仍先 `closeAt`，然后才 `DomainPulses.fire`。原来的维护扣款和魔力不足关闭分支已删除。本文件不再调用 `nextUpkeepAt`、`setNextUpkeepAt`、`upkeepCost` 或 `upkeepPeriod`。`closeAt`（183–187 行）仍是关闭接触、移出列表、广播结束。生成账单仍是 `initialCost * count`（85–104 行）；预留路径、同 tick 声明串、份数、几何、寿命缩放都未改。

`DomainDefinition` 增加不含维护参数的构造（21–31 行）。`mistRain`（62–69 行）和 `deepPressure`（120–127 行）改走这条构造，生成费仍是 40，寿命仍是 200，盒子和原生周期未改。带维护费、维护周期的旧构造（37–60 行）保留，范围外调用仍能编译；这两个参数只被保存和读出，不再作为存续条件。`upkeepCost`（83–85 行）和 `upkeepPeriod`（91–93 行）标为弃用。两个工厂不再传入 8 和 20，因此现行领域这两个读数是 0，同时 tick 里已经没有到点扣费。

`WaterReleaseDefinition.domain`（104–112 行）不再把 `domain.upkeepCost()` 写入场地维护，该字段保持默认 0。显式魔力仍是 0，寿命和 `fieldLife` 仍抄领域寿命。`zone`（84–91 行）和 `barrier`（94–101 行）仍写各自的维护费。

`DomainInstance` 仍由另一作者持有。它现有的 `nextUpkeepAt` 初始化还在调用已弃用的 `upkeepPeriod()`；本侧没有新增它必须改用的接口。

## name-gate

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 1。本次改过的三个文件没有命中。未改文件里的旧诊断只记录，没有改名：

- `src\main\java\com\bslx\XueXiMod\animation\runtime\OriginalHumanoid.java:165`，`i` 少于 3 个字符
- 同文件 202 行，`to` 少于 3 个字符

静态对照还看到 `src\test\java\com\bslx\XueXiMod\shu\technique\release\ReleaseContainerEnergyCheck.java` 第 81 行仍断言雾雨 `upkeepCost()==8` 且 `upkeepPeriod()==20`。测试文件不在本次可写范围，没有改。

## 未跑验证

未执行 `gradlew`、`compileJava`、测试、打包和游戏。护体、屏障、旧场地维护、接触计费、制作表均未改。

写入已收束，待统一编译。
