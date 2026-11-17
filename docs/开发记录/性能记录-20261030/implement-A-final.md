我先读取任务说明和执行流程，确认授权范围后再动手。任务已授权由我单独实现公共设施。我先读冻结的设计裁决，再定位现有命令注册点。设计边界已经冻结。接下来只定位命令注册、模组版本和 Forge 事件接入方式，不读 B/C 正在改的文件。公共接口要和已经在用的调用方式一致。我先备份现有的 `ModCommands.java`，再写入 A 独占的新文件。命名检查里，本次改过的文件没有新的违规。接下来写入最终报告和证据，并记下文件哈希。# A 实现最终报告：性能记录公共设施

实现已写入并收束。本阶段是实现，不是正式复审。设计裁决仍为 1/2，实现复审仍为 0/3。本次没有另开正式复审，也没有建立独立测试。未运行 `gradlew`、`compileJava`、`build`、`jar`、`reobf`、`runClient`，没有启动游戏。不能从这份报告推断性能变好，也不能把任何读数当成用户已经实测。

代码快照 UTC：`2026-10-29T20:21:07.186Z`。报告文件 `docs/开发记录/性能记录-20261030/implementation-A-final.md` 的 sha256 是 `4FB557CBC2F6175A3C42AF309B411F47DC9D38D94003C094B3890F49DABA1438`（11631 字节，哈希时间 `2026-10-29T20:22:15.836Z`）。证据文件 `implementation-A-evidence.json` 的 sha256 是 `31068546426EBFDE09CE531349021E6E7484C38DA086EB5C2F2C55923BEBE091`（2657 字节，`2026-10-29T20:22:32.011Z`）。证据文件记录了报告哈希，自身哈希不写回文件，以免改写已记录内容。

## 结论

默认关闭的服务端性能记录公共设施已经按冻结裁决落地。B/C 可直接调用的公开面是 `PerformanceRecorder.isRecording`、`begin`、`end`、`add`，以及枚举 `PerfMetric`、`PerfCounter`。命令是权限 2 的 `/shuperf start|stop|status|export`。采样只在绑定服务器的服务端线程、并且当前完整 tick 的门控打开时生效。

## 改动路径与 sha256

| 路径 | 字节 | sha256 |
|---|---:|---|
| `src/main/java/com/bslx/XueXiMod/debug/performance/PerfMetric.java` | 2412 | `1358A74F7BE55499E693CB34F4372AEACC1006CD794F46E12A7AA3E7E06ED5CD` |
| `src/main/java/com/bslx/XueXiMod/debug/performance/PerfCounter.java` | 2271 | `50ACD8C5C17C03649D14EA48DF78633A936F47E941269806C452307669477F55` |
| `src/main/java/com/bslx/XueXiMod/debug/performance/PerformanceRecorder.java` | 34455 | `897A55048AE9C1AAA6CF8FE8D47E8FE8A53C9C73240421555D79244082473065` |
| `src/main/java/com/bslx/XueXiMod/debug/performance/PerformanceCommands.java` | 6422 | `CC12749E5C6B298F480935697D8828800B5859EBCDDA23F1A4326A8E3C7D7B49` |
| `src/main/java/com/bslx/XueXiMod/debug/performance/PerformanceEvents.java` | 2160 | `024620501187984A9A4E2847BC1DD9F21F2864880E7DC3E97C577543FA03914A` |
| `src/main/java/com/bslx/XueXiMod/command/ModCommands.java` | 1024 | `F3049296125FFC5E9327045884933742A005C07DC8696D55570B4E6FE2D47220` |
| `docs/性能记录使用说明.md` | 6950 | `13274A7CB49F76BC3869415EA6231FD1259DCD1084280DA2A89CB3264A506DB3` |
| `docs/开发记录/性能记录-20261030/before-A/ModCommands.java` | 899 | `37B49C896B5E73DE2BDD6AC2F342E7F9FF5C0AF323AACC65DE53305909150EBA` |

`ModCommands.java` 的改前哈希与备份相同。现文件只多了 `PerformanceCommands` 的 import，以及 `onRegisterCommands` 里的一行注册。没有 reset、clean、checkout、提交或改制作表。B/C 的源码没有改。

## 关键位置

公开热路径在 `PerformanceRecorder.java`：`isRecording` 72–77 行，`begin` 79–91 行，`end` 93–115 行，`add` 117–129 行。

生命周期在 `PerformanceEvents.java`：`ServerStartedEvent` 21–32 行绑定 `MinecraftServer.getRunningThread()`；`ServerStoppingEvent` 34–37 行按服务器对象身份释放；START 用 `EventPriority.HIGHEST`，39–48 行；END 用 `EventPriority.LOWEST`，50–59 行。两个处理方法分开，先对阶段和服务器对象身份过滤。订阅器在公共 Forge 总线，没有 `Dist` 限制，也没有客户端类。

命令注册在 `PerformanceCommands.java` 16–29 行，根字面量 `shuperf` 要求 `hasPermission(2)`。`ModCommands.java` 25 行只追加这次注册。

会话开关在 `PerformanceRecorder.java`：`bindFreshServer` 135–142 行，`releaseServer` 144–153 行，`onTickStart` 155–199 行，`onTickEnd` 201–216 行，`commandStart` 218–254 行，`commandStop` 256–281 行，`commandExport` 290–313 行。归并从 `mergeCompletedTick` 315 行起。P95 在 `nearestRankIndex` 647 行和 `percentileNearestRank` 658 行，只在导出时对已完成 tick 的副本排序。

## 实现行为

状态是 `IDLE → ARMED → RECORDING → STOP_PENDING → STOPPED`。同时只保留一份正在录或最近停止的会话。

`ServerStartedEvent` 绑定这一台服务器对象和它的运行线程，强制回到 `IDLE`，清掉旧数组、旧导出路径和旧引用。`ServerStoppingEvent` 若服务器对象不一致则直接返回；一致时先关 tick 门控，再丢掉整个会话，不写盘。同一次 JVM 里下一次服务器不会继承上次记录。tick 事件用 `event.getServer() == boundServer` 判断，不用线程名字。

`/shuperf start` 默认 1200，允许 1 到 6000。它只进入 `ARMED`，并按请求的 tick 数一次分配每 tick 数组和会话聚合数组。下一个 START 才打开第一格。下达命令时正在走的那一格不录，也不算未完整 tick。`ARMED`、`RECORDING`、`STOP_PENDING` 上再次 start 会拒绝，不清数据。`STOPPED` 上 start 会换掉内存结果，并提示应先 export；磁盘上已导出的目录还留着。

`stop` 在 `ARMED` 时立刻取消，留下 0 个完整 tick 的 `STOPPED` 结果。在 `RECORDING` 时只改为 `STOP_PENDING`：门控还开着就等这一格 END 归并后再停，不截断；门控没开就在下一个 START 停止，不再多录一格。同一格 END 上若已经达到目标 tick，停止原因记为 `target_reached`，优先于命令停止。没有墙钟定时线程。满 6000 或满请求格数就停，不环形覆盖，不抽样。

START 在打开门控前清零当前 tick 的原始数组，然后取 `nanoTime`，再打开门控。END 先取 `nanoTime`，再关门控，再把原始值归并进会话。重复 START 或缺失 END 时，已打开但没有配上 END 的那一格整格丢弃，只增加 `incomplete_ticks`，不留下没有 tick 分母的细项。服务器退出时未完成的一格随会话一起丢掉，不保留这个计数，因为不会自动导出。

`isRecording` 在门控关闭时只读一次 volatile 就返回 false。`begin` 在未采样或线程不是绑定的服务端线程时返回 0，不读 `nanoTime`；有效时返回 `nanoTime`，若读到 0 则返回 1。`end` 先看 `startedNanos != 0`、枚举非空、门控和线程，之后才读 `nanoTime`，并累加当前 tick 的次数、总纳秒和单次峰值。`add` 同样先看门控；只接受正增量，零和负数无作用。这四个方法不创建对象、字符串或 lambda。负的时间差钳成 0，这一次调用或这一格 tick 仍算完成。类加载时会分配一次可复用的当前 tick 临时数组；关闭后的调用本身不再分配。

每 tick 保存序号、tick 纳秒、目标查询次数、实体查询次数、两类候选、攻击体更新次数、剑更新次数。全会话每个 `PerfMetric` 保留次数、总纳秒、单次峰值；每个 `PerfCounter` 保留合计。不保存每 tick 的 metric 矩阵。业务项不加成一个总耗时。

`status` 只报状态、完成/目标、未完整 tick 和最近成功导出路径。`export` 只在 `STOPPED`、门控已关、服务器仍在运行且未退出时执行。录制、待命、待停都拒绝，不为导出截断当前 tick。写入在命令线程同步进行，UTF-8 无 BOM，目录是 `<游戏工作目录>/logs/xueximod-performance/<UTC时间戳>-<序号>/`，`CREATE_NEW` 写 `summary.txt` 与 `ticks.csv`。两个文件都成功才记下导出路径。失败时内存保留，命令返回异常文本；若目录已创建但文件没写完，该目录可能不完整。再次 export 使用新序号和新目录。排序和写盘都在停止之后，不进入该次采样。

观测 tick 的平均值、P95、峰值只针对已完成 tick。P95 是排序副本的 nearest-rank，下标 `(95*N+99)/100-1`，也就是 `ceil(0.95*N)-1`。N 为 0 写 N/A。手算核对：N=1 下标 0；N=20 下标 18；N=6000 下标 5699。这是 tick 墙钟的 P95，不是方法调用 P95。每 tick 查询次数只等于两类 `getEntitiesOfClass` 次数之和。owned 全实体扫描在摘要里单独列出，不并进查询次数。候选数是重复访问的累加。

报表元数据含格式版本 1、会话 UTC 时间、集成或专用、目标/完成/未完整 tick、停止原因、Java 版本、模组容器可读版本、采样边界、容量和 P95 算法。不记录玩家名、UUID，也不记录路径以外的机器信息。不读取、不切换 DebugLog。

## 定向完整性核对

这次只核对了上述边界，没有跑游戏，也没有把它记成实现复审。

- 关闭路径在读到门控为假后返回，不读 `nanoTime`，四个公开方法不分配。
- 非绑定线程即使门控开着也直接返回。
- 公开方法不吞异常；早退和异常由调用方的 `try/finally` 自己走到 `end`。
- 计数只加调用方已经算好的增量。
- 归并只发生在配上的 END。`stop` 不把正在开着的一格截断。
- 新的 `ServerStartedEvent` 清掉上一台服务器的会话。
- 命令权限是 2，注册类不依赖客户端。
- 导出在 `STOPPED` 之后，文件用 `CREATE_NEW`，失败不改玩法状态，也不把内存清掉。

## 未覆盖

裁决第六节的边界都写进了使用说明和 `summary.txt`：

- 原版实体追踪，真实网络完成、字节数、延迟，客户端解码、渲染、FPS。
- `EXPLICIT_SEND` 只是业务调用墙钟，不能换成包数。`entityData.set` 和 `setPos` 不算网络发送。
- `ModNetwork.sendToPlayer`、`FireVisualRelay` 以及其他发包位置。显式同步只覆盖 `TechniqueHitSettle` 的事件、声音和粒子。
- 真剑抛出生成和 `HitVolumes` 视觉体生成。攻击体只计 `TechniqueHitSpec`，幻剑只计 `PhantomSwordEffect`。
- 活跃实体实时总量、休眠与活跃分离、每术式或每玩家明细、分配和 GC 的独立耗时、索引内部候选量，以及未列入的查询和碰撞。更新次数不是活跃实体总数。
- 待命前、停止后、服务器退出时的残缺 tick。延迟结算不按施放时间回填。
- 没有活动采样 tick 的命令和网络任务。
- 不是全模组 profiler，也不是原版追踪器 profiler。
- 没有方法调用耗时 P95，没有 exclusive time，没有按术式、玩家或 UUID 的维度。
- 同优先级监听器的顺序不确定，观测窗口不包含钩子外工作和 tick 间等待。

另外：`nanoTime` 回绕成负数时该次时长记 0；`begin` 读到 0 时返回 1。导出若在写完摘要前失败，可能留下空目录或不完整目录。服务器退出会丢掉尚未导出的内存。命名门禁仍有两处旧违规，在未改动的 `animation/runtime/OriginalHumanoid.java` 165 行 `i`、202 行 `to`。本次没有顺手改那个文件。

## 编译与实测

待统一编译。三个作者的写入都收束后，才能由指定的 Cursor 在项目根执行 `.\gradlew.bat compileJava --offline --no-daemon`。本次没有编译，没有游戏实测，也不声称采样开销或玩法耗时有改善。

## 所有权

A 对上表源码、使用说明、`before-A/`、`implementation-A-final.md` 和 `implementation-A-evidence.json` 的写入已经结束，所有权释放。
