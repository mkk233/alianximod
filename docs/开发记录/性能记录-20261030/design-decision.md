# 性能记录首版设计裁决（2026-10-30）

本文件为本次唯一困难设计裁决，计设计 1/2，实现仍为 0/3；不是实现复审。依据是本目录 read-release-final.md 与 read-entities-final.md 两份已收束的 Cursor 最终定位报告，以及现行调度流程。没有额外读取 Java，没有实现、编译或启动游戏。用户授权直接开始性能记录，仅观测，不实施缓存、查询复用、异步、JNI 或玩法修改。以下决定可直接交三个固定 `--model grok-4.7-high-fast` Cursor 作者实施；普通 API 拼接和准确文件路径由各作者在所有权范围内确认，不需要主会话再做一轮设计判断。

## 一、首版范围与裁剪

固定枚举分类，服务端线程采样，默认关闭。计时使用显式 long start 与 try/finally end；不建立 Scope、lambda 包装器、调用树、ThreadLocal 栈或每次调用对象。不做 exclusive time，不做按术式 ID、玩家或实体 UUID 的维度，不新增事件逐条日志。所有耗时都是 inclusive，可能嵌套，也可能同枚举重入，报告禁止相加解释为总 CPU 或 tick 总耗时。同类重入次数照实记录，累计值可以超过实际墙钟时间。

覆盖释放入口、链解析和角色扫描、调度回调、段执行、目标查询、几何、攻击体/剑更新、运动、候选查询、精确碰撞、攻击体生成、幻剑生成、命中处理、Tally 入账与结算、实际伤害入口，以及命中反馈的显式发送调用。不是全模组 profiler，也不是原版追踪器 profiler。

## 二、固定公共接口（A 独占定义；B/C 直接按此使用）

新增包 `com.bslx.XueXiMod.debug.performance`，公共类 `PerformanceRecorder`、`PerfMetric`、`PerfCounter`。以下方法均 static public，枚举见后文：

```java
boolean isRecording();
long begin();
void end(PerfMetric metric, long startedNanos);
void add(PerfCounter counter, long delta);
```

`isRecording()` 只有在本次完整 tick 正在采样且 `Thread.currentThread()` 等于本服务端采样线程时为 true。关闭时只读一次 volatile 状态并返回 false；不读 nanoTime、不分配、不查询服务器/世界、不打日志。开启时进行线程一致性检查，客户端/其他线程始终 no-op。`begin()` 先同样检查，未采样返回 0；有效时返回 nanoTime（零值规避为 1）。`end` 先检查 startedNanos != 0 和当前采样线程/状态，才读 nanoTime 并累加当前 tick 对应枚举的次数、总 ns、最大 ns。`add` 同样门控，只允许非负 delta，零无作用。关闭时调用接口本身不得分配。

使用 `long perfStart = PerformanceRecorder.begin(); try { 原逻辑 } finally { PerformanceRecorder.end(PerfMetric.X, perfStart); }`，包括早退和原异常路径。只包原业务范围，不改变原 finally 恢复顺序。异常照常传播；不 catch 后吞掉玩法异常。计数的加法不得再执行一次业务表达式。关闭时不得因插桩额外计算列表大小以外的复杂量；对需要求值的统计使用 isRecording 门控。严禁把开关包在玩法逻辑外导致关闭时跳过玩法。

PerfMetric 固定枚举：
- RELEASE_REQUEST：SkillUsePacket 的 enqueueWork 中业务体（仅同步按下处理）。
- CHAIN_SPLIT：TechniqueChain.split。
- CHAIN_ROLE_SCAN：SkillSegmentRunner.runSegment 中角色扫描原区间。
- SCHEDULED_ACTION：ChainScheduler 两条真正 action.run 调用；不包 schedule 整体。
- SEGMENT_EXECUTE：SkillSegmentRunner.runSegment 服务端执行体，含其子项和早退。
- TARGET_QUERY：LockTargetService.findTarget、ReleaseBatch.crosshairCenter 的世界查询区间、resolveAnchor 首次世界查询区间；不含锚点缓存返回。
- GEOMETRY：CastFrame.forAttack 以及 volumePoint、aimFor 的计算体（重叠照实注明）。
- CONTACT_ENCHANT_TICK：ContactEnchantState.tick 服务端处理体。
- HIT_ENTITY_UPDATE：TechniqueHitEntity.tick 客户端返回后的整个服务端体，含 pendingActivation 早退。
- SWORD_UPDATE：SwordEntity.tick 客户端返回后的整个服务端体。
- MOTION：TechniqueHitEntity 中已有 motion.tick 的两处调用（包含 applySpec 初始调用），SwordEntityMotion.flyStep 与 SwordEntity.returnTick；弧带更新不是普通 motion，留在 HIT_ENTITY_UPDATE。
- ENTITY_QUERY：C 所有权文件中本文件第三节列出的实体列表查询调用；只包原查询表达式，不包消费列表的循环。
- SWORD_OWNED_SCAN：SwordEntity.anyOwnedBy / ownedBy 原扫描体；与空间查询分列。
- NARROW_PHASE：TechniqueHitEntity 普通 contains 与弧带 intersectsSlice 的实际调用，以及 SwordEntityMotion.hurtAlong 的原窄判定表达式。
- BLOCK_CLIP：命中体和剑步进的世界 level.clip 调用（不把实体 AABB.clip 混入）。
- HIT_SPAWN：TechniqueHitSpec.spawnReported 整个调用，含验证/准备/生成。
- PHANTOM_SWORD_SPAWN：PhantomSwordEffect 原生成执行体。
- HIT_SETTLE：TechniqueHitSettle.tryHit、settleFlow（后者若调用前者仍按 inclusive 解释），不在 settle 再重复包同枚举。
- TALLY_RECORD：ReleaseBatch.Tally.record。
- TALLY_FLUSH：ReleaseBatch.Tally.flush。
- DAMAGE_APPLY：TechniqueHits.hurt 的最内层实现与 hurtConfirmed 原实现，不包 hurtRoot 再重复；剑的直接 magic hurt 分支也按此记录。
- EXPLICIT_SEND：TechniqueHitSettle 中 broadcastEntityEvent、playSound、sendParticles 每个原调用各计一次。

PerfCounter 固定枚举：
- TARGET_QUERY_CALLS、ENTITY_QUERY_CALLS：实际执行一次 getEntitiesOfClass 增一，缓存命中不增；两个枚举互斥分类。
- TARGET_CANDIDATES、ENTITY_CANDIDATES：上述原调用返回列表的 size 累计，含原谓词已过滤结果；不是空间索引内部扫描量，不在 predicate 内计数，不为了计数改谓词或重查。
- SWORD_OWNED_SCAN_CALLS、SWORD_OWNED_SCANNED：每次 owned 扫描与原循环实际访问实体个数；anyOwnedBy 早退只计已访问的实体，不能预先扫全世界。
- HIT_ENTITY_UPDATES、SWORD_UPDATES：上述服务端 tick 入口调用次数，含休眠/等待激活实例；不是全世界实时活跃实例数，也不是一局去重实体数。
- HIT_SPAWN_REQUESTED：spawnReported 原循环计划次数（与现有 requested 口径一致，只累加一次）。
- HIT_SPAWN_CONSTRUCTED：实际成功 new TechniqueHitEntity 后加一。
- HIT_SPAWN_ACCEPTED：addFreshEntity 返回 true 后加一，不重复调用该方法。
- SWORD_SPAWN_REQUESTED、SWORD_SPAWN_CONSTRUCTED、SWORD_SPAWN_ACCEPTED：仅 PhantomSwordEffect 对应三种口径。
- NARROW_PHASE_TESTS、NARROW_PHASE_PASSED：实际窄判定的次数与通过次数；保留原短路逻辑，不为收集两端结果而改变求值。
- DAMAGE_ATTEMPTS、DAMAGE_ACCEPTED：已插桩的实际 target.hurt 调用次数与原布尔返回 true 次数；不是 tryHit 次数、不是 Tally.record 次数、也不是扣血量。
- EXPLICIT_SEND_CALLS：上述三个业务发送/声音调用次数；不是数据包数或字节数。

如原目标查询含多个不同世界调用，计时 TARGET_QUERY 可包该搜索整体，QUERY_CALLS 仍只按实际 getEntitiesOfClass 次数；射线 clip 时间包含在 TARGET_QUERY 内，不混进 BLOCK_CLIP。不得因接口不方便修改玩法签名。插桩过程中若某报告描述与实际方法不同，以保留原语义为先，报告具体阻塞，不自作架构变更。

## 三、作者唯一文件所有权及插桩位置

源码统一位于 `src/main/java/com/bslx/XueXiMod/` 下。下列已有文件以两份报告给出的文件名/类定位，作者用精确文件名确认子目录；同名不唯一则先报告。只能修改列明文件，不扩展到整个目录。各作者另可写本目录自己的 implementation-A/B/C-final.md；不能修改本裁决或彼此报告。

A：新增 debug/performance/PerformanceRecorder.java、PerfMetric.java、PerfCounter.java、PerformanceCommands.java、PerformanceEvents.java；已有 ModCommands.java（只追加注册）；新增 docs/性能记录使用说明.md。不改 XueXiMod.java、ModEvents.java 或已有其他生命周期类，新的 Forge 订阅器自行接入公共总线且不得限制 Dist.CLIENT。A 负责全部公共接口、生命周期、命令、统计输出与文档，不修改 B/C 源码。

B：SkillUsePacket.java、TechniqueChain.java、SkillSegmentRunner.java、ChainScheduler.java、LockTargetService.java、ReleaseBatch.java、CastFrame.java、ContactEnchantState.java。仅添加本裁决的计时/计数。ContactEnchantState 原 getEntitiesOfClass 归 ENTITY_QUERY/ENTITY_QUERY_CALLS/ENTITY_CANDIDATES，属于 B 文件所以由 B 插。不要修改 CastExecutionSession、SkillChainPlan 或额外做会话层计时：首版依靠真实 runSegment 与 scheduler 回调覆盖，避免扩大补丁。

C：TechniqueHitEntity.java、SwordEntity.java、SwordEntityMotion.java、TechniqueHitSettle.java、TechniqueHitSpec.java、TechniqueHits.java、PhantomSwordEffect.java、HitVolumes.java。候选查询覆盖：普通命中、陷阱踏板、弧带、引爆、剑 hurtAlong、HitVolumes END 的已有 getEntitiesOfClass。HitVolumes 只插查询，不改状态维护或视觉逻辑。不用修改 SizedBoxHitbox、ProgressArcBand、各 motion 实现、SwordVortex：在已定位调用侧记录，避免重复和作者越界。C 不改 ReleaseBatch。

A 公共接口与文件契约已在本文件冻结，B/C 可并行写引用，不必等待 A 代码落地。三个作者均无编译授权；全部返回最终反馈、所有权和写入收束后，由主会话指定一个 Cursor 执行最终统一编译。已有未提交改动全部保留，禁止覆盖工作区、提交、打包或启动游戏。

## 四、完整 tick 会话与生命周期

采样状态机 IDLE → ARMED → RECORDING → STOP_PENDING → STOPPED。只保留一个正在录制或最近停止的 session。`/shuperf start [ticks]`，默认 1200 完整 ticks，范围 1..6000；以 tick 数限定容量，非真实秒数承诺。重复 start 在 ARMED/RECORDING/STOP_PENDING 时拒绝且不清空已有数据；STOPPED 后 start 明确替换上次内存结果，命令反馈提醒先 export。

每次 ServerStartedEvent 绑定这一个 MinecraftServer 的 identity 与当前服务端 Thread；强制默认关闭，清除旧内存和旧引用。ServerStoppingEvent 立即禁用采样，丢弃未完成当前 tick，释放 session 与 server/thread 引用，不自动写磁盘。下一次同 JVM 集成服不得继承上次记录。服务端事件身份不匹配时 no-op。生命周期事件注册不用客户端类。

ServerTickEvent START 使用 HIGHEST 优先级打开 tick；END 使用 LOWEST 优先级关闭 tick，独立两个处理函数。用事件的 server identity 校验，不能只看线程名字。START 时间戳在启用 tick 门控之前取得，END 时间戳在统计归并/排序/导出之前取得，随后先关本 tick 门控，再归并。同优先级监听器顺序不保证，不能声称覆盖同优先级所有监听器。该时间代表 Forge START(HIGHEST) 到 END(LOWEST) 之间的服务器 tick 墙钟，报告命名 `observed_server_tick_ms`，可中文标“观测 MSPT”；不包括 tick 间 sleep/等待以及事件钩子外工作，不等同原版完整 MSPT，也不是纯 CPU 时间，包含区间内 GC/抢占及采样开销。

start 命令只置 ARMED，在下一个 START 开始第一个完整 tick，不录当前残缺 tick。stop 在 ARMED 时取消且保留零完整 tick 的停止结果；在 RECORDING 时置 STOP_PENDING，到当前完整 tick 的 END 归并后停止。达到请求 ticks 后同样在 END 自动停止。即便 stop 命令运行于事件区间外，也只在下一个合法边界停，不截断现有时长。没有 wall-clock 定时线程，不在服务端卡住时承诺自动超时；“超时”就是达到目标完整 tick 数。

所有计时和计数先写当前 tick 的固定数组；只有匹配的 END 才归并 session。退出、重复 START、缺失 END 等未完整 tick 丢弃其全部细项，报告计 incomplete_ticks；不能留下细项而没有 tick 分母。没有活动 tick 的命令/网络任务不计入首版，并明确标注这项覆盖边界。同步调用不跨服务端 tick；无需 Scope token/世代分配，清理发生后 end 通过门控直接 no-op。

## 五、有界数据、命令与导出

所有命令 require permission level 2，服务端注册，不要求客户端组件：`/shuperf start [ticks]`、`/shuperf stop`、`/shuperf status`、`/shuperf export`。status 只报状态、已完成/目标 ticks、未完整 ticks、最近导出路径，不排序、不导出、不开启采样。未知参数沿 Brigadier 常规错误返回。

固定数组保存最多 6000 个完整 tick：tick 序号、tick ns、目标查询次数、实体查询次数、候选目标总数（同时导出两类分列）、攻击体更新次数、剑更新次数。无需保存每 tick 每 metric 的矩阵。全 session 每 metric 只保留 call count / total ns / max ns；每 counter 保留 total。每 tick 临时 metric/counter 数组 START 清零，END 原始类型归并。容量到即停止，无环形覆盖、无随机抽样。分配发生在 start/START 首次会话准备或导出，不发生每实体/每查询；最好 start 一次性分配容量。禁用态事件处理也不读 nanoTime。

服务器 tick 平均值、P95、峰值针对全部完成 tick，P95 用排序副本 nearest-rank：ceil(0.95*N)-1；N=0 标 N/A。每 metric 平均值用 total/calls；首版不提供调用耗时 P95。报表中不得把 tick P95 冒充方法 P95。每 tick 查询总数 = TARGET_QUERY_CALLS + ENTITY_QUERY_CALLS；owned 全实体扫描另列，不能丢失也不混称空间查询。候选数重复访问照实累加，不是唯一目标数。

export 仅 STOPPED 且服务器未退出时允许；录制/待停/待启动时拒绝并提示先 stop、等待停止后 export。不为了导出自动截断当前 tick。同步在服务端命令线程写 UTF-8：固定目录 `<游戏工作目录>/logs/xueximod-performance/`，自动 session 时间戳+唯一后缀，CREATE_NEW 防覆盖，无用户路径参数。输出 summary.txt（口径/元数据/聚合值/未覆盖项）与 ticks.csv（最多 6000 行）。写文件失败保留内存，返回实际错误；不影响玩法状态。不提供自动定期导出，不用异步线程；排序和磁盘 I/O 都在采样停止后，所以不进入该 session。导出会暂时占用服务器线程，文档建议结束实测后操作；不声称毫秒级无感。重复 export 可生成新唯一目录。

元数据至少包含格式版本、session 时间与服务器类型（集成/专用）、目标/完成/未完整 tick 数、停止原因、Java 版本、模组可读取版本、采样边界、容量和 P95 算法。不记录玩家隐私、UUID或路径以外不必要机器信息。调试日志会影响数据，文档要求对比时保持 /shulog 与其他设置一致；不自动切换 DebugLog、不把其客户端状态猜成服务端状态。

## 六、明确未覆盖与验收边界

报告和使用文档必须逐项说明：
- 原版实体追踪、真实网络发送完成/字节数/延迟、客户端解码/渲染/FPS 均未覆盖。EXPLICIT_SEND 是业务调用分发/入队墙钟，连包数都不能据此推算。entityData.set、setPos 不当作网络发送。
- ModNetwork.sendToPlayer、FireVisualRelay、全模组其他发包位置首版未插；显式同步统计只覆盖 TechniqueHitSettle 的事件、声音与粒子调用。
- 真剑抛出生成、HitVolumes 视觉体生成不计入两类生成数；攻击体生成统计仅 TechniqueHitSpec、幻剑仅 PhantomSwordEffect。
- 活跃实体实时总量、休眠与活跃分离、每术式/每玩家明细、内存/分配/GC 独立耗时、内部空间索引候选访问量、未列出的全模组查询和碰撞均未覆盖。更新次数不可标成活跃实体总数。
- 录制只能看到事件边界内工作；ARMED 前、停止后、服务端退出的残缺 tick 不算。延迟结算仅在其实际执行 tick 落在窗口内时计入，不能按施放时间回填。
- 所有业务项 inclusive 且允许重入，不得加总、作互斥百分比饼图或从总 tick 中逐项相减。

实现阶段集中复核应检查的语义边界（不另设独立测试任务）：关闭不读 nanoTime/不分配；客户端和非服务端线程 no-op；早退/异常原样；查询谓词与随机次数不变；addFreshEntity/target.hurt 只执行原次数且返回值不变；伤害/计费/命中去重与 pendingActivation 不变；scheduler 当场/延后/同步重入仅各真实 action.run 包一次；Tally.record 与 flush 时间分离；计数每完整 tick 归并；stop 不截半 tick；同 JVM 重进世界清理；权限2与专用服无客户端依赖；导出不进入采样且文件不可覆盖。编译只能在三个作者写入收束后由指定 Cursor 执行 `./gradlew.bat compileJava --offline --no-daemon`，主会话不执行。

用户游戏实测再覆盖：关采样正常释放；开采样空场基线；相同实体数量的攻击/一分为三/双重/幻剑与剑来；停止导出；重复开启与重新进入世界；比较相同配置报告。这里只给步骤方向，不宣称游戏验证已经完成。本次非按表制作，不擅自领取或迁移制作表。

最终裁决：按本文件实施三作者并行最小观测版本。首版获得可比较的完整 tick 均值/P95/峰值、主要业务 inclusive 耗时、查询/候选数、生成与更新计数，明确统计边界；不借记录任务实施任何优化。后续发现真实热点，再由用户决定是否进入优化任务。
