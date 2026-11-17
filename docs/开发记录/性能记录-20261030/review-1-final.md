我先通读这份任务说明，按它授权的范围来做，再给出完整报告。任务是只读复核：先读设计决定、A/B/C 终稿和证据，再对照本批源码与使用说明。设计与三份终稿已对齐。接下来对照当前源码、使用说明和各作者改前快照核对契约。公共接口已读完。接下来核对生命周期细节，并逐项对照 B/C 的插桩和改前快照。本批可以通过只读复核，不需要返修。没有阻塞问题，也没有指定返修作者或文件。

这是实现阶段的集中只读复核。没有再委托代理，没有改源码、文档或表格，没有编译、测试、运行游戏或打包。不能从本报告推断性能变好，也不能把任何读数当成已经实测。

## 审查范围

对照 `design-decision.md`、A/B/C 终稿和三份 `evidence.json`，审了本批 22 个源码文件和 `docs/性能记录使用说明.md`。本批变化用 `before-A`、`before-B`、`before-C` 快照区分；快照里已有的玩法保持原样。报告里的行号与当前源码一致。没有按相对 HEAD 重审整仓。

作者 A：`PerformanceRecorder.java`、`PerfMetric.java`、`PerfCounter.java`、`PerformanceCommands.java`、`PerformanceEvents.java`、`ModCommands.java`，以及使用说明。`ModCommands.java` 相对快照只增加了 `PerformanceCommands` 的 import 和注册调用。

作者 B：`SkillUsePacket.java`、`SkillSegmentRunner.java`、`TechniqueChain.java`、`ChainScheduler.java`、`LockTargetService.java`、`ReleaseBatch.java`、`CastFrame.java`、`ContactEnchantState.java`。

作者 C：`TechniqueHitEntity.java`、`SwordEntity.java`、`SwordEntityMotion.java`、`TechniqueHitSettle.java`、`TechniqueHitSpec.java`、`TechniqueHits.java`、`PhantomSwordEffect.java`、`HitVolumes.java`。

## 1. 公共契约、线程与完整 tick

`PerfMetric` 22 项、`PerfCounter` 19 项与冻结名单一致。B/C 只调用 `isRecording`、`begin`、`end`、`add`。

关闭时这四个方法在读到门控为假后返回，不读 `nanoTime`，方法体不分配。`begin` 在未采样或线程不是绑定的服务端线程时返回 0；有效且读到 0 时返回 1。`end` 在开始时间为 0、枚举为空、门控关闭或线程不符时返回，之后才读 `nanoTime`。`add` 只接受正增量。

录制态要求 `Thread.currentThread()` 等于 `ServerStartedEvent` 绑定的那台服务器的运行线程，tick 事件再用服务器对象身份核对，不用线程名。订阅器在 Forge 总线，没有 `Dist` 限制，也没有客户端类。权限 2 的命令同样没有客户端类型。

1.20.1 映射里，`MinecraftServer` 具备 `getRunningThread()`、`File getServerDirectory()`、`isRunning()`、`isStopped()`、`isDedicatedServer()`。`TickEvent.ServerTickEvent.getServer()` 已在改前的 `ChainScheduler` 等服务端代码里使用。`sendSuccess` 使用 1.20 的 `Supplier` 形式。

状态机是 `IDLE → ARMED → RECORDING → STOP_PENDING → STOPPED`。`start` 默认 1200、范围 1 到 6000，只进入待命；下一个 START 才打开第一格，当前残缺 tick 不计未完整。忙碌时拒绝且不清数据；`STOPPED` 后再 `start` 替换内存并提示先导出。`stop` 在待命时留下 0 个完整 tick；录制中只标 `STOP_PENDING`，开着的一格到 END 归并后停。同一格既到目标又收到停止时，原因是 `target_reached`。缺 END 或重复 START 时整格丢弃并增加 `incomplete_ticks`。满请求格数或 6000 即停，不环形覆盖。`ServerStartedEvent` 清空旧会话；`ServerStoppingEvent` 按服务器身份丢弃未完成 tick，不写盘。同一次 JVM 再次进世界不会继承上次记录。

## 2. 原返回、短路、finally 与调用次数

与三份快照对照，插桩是 `long` 加 `try/finally`，开关没有包住玩法。

`getEntitiesOfClass` 仍是原来的 10 处：锁定、准星、锚点首次搜索、接触附魔、陷阱、普通命中、弧带、引爆、剑 `hurtAlong`、HitVolumes 体积与光束。谓词原文未改，也没有放进谓词里计数。锚点缓存和不需要锚点的眼前 4 格回退不计查询。`addFreshEntity`、`target.hurt`、三处 `motion.tick`、两处 `action.run` 都只出现原来的次数。幻剑 `behind` 的两次 `nextDouble`，以及 `aimFor` 里的随机方向，仍各走原分支一次。

`SkillSegmentRunner` 仍先 `PartingOffers.clear` 与 `CastFrame.unbind`，再恢复重水和宿主组。接触附魔奇数 tick 仍直接返回；偶数 tick 仍先 `settlePending` 再结束计时。剑窄判定仍是线段 `clip` 为空才求值终点盒。命中去重、`pendingActivation` 早退、无敌时间先清后按原 `finally` 恢复、潮汐 `finish`，顺序都还在。`hurtConfirmed` 的返回值仍是 `precise`。当场 `action.run` 的异常仍向外抛；到期任务仍由外层 `catch` 记录后吞掉这一处任务异常。

## 3. 覆盖、计数与重叠

契约列出的入口都有对应插桩。时间在 `end` 里按纳秒累加；报表里的毫秒由该纳秒换算。查询次数按实际返回的 `getEntitiesOfClass` 加一，候选数是该列表的 `size`，空列表的次数仍加一。owned 扫描每次加一，`anyOwnedBy` 命中后返回，只计已经访问的实体。生成三计数在循环正常结束后各加一次：计划数沿用原来的 `entityCount`，构造数是成功 `new` 的次数，接受数是 `addFreshEntity` 返回 true 的次数。窄判定先保存同一次布尔结果，再决定通过数和原来的命中。`DAMAGE_ATTEMPTS` 只在 `target.hurt` 返回后加一，返回 true 才加 `DAMAGE_ACCEPTED`；金额或目标无效的早退不计尝试。

`GEOMETRY` 在 `forAttack` 里会再进入 `volumePoint` 和 `aimFor`。`SEGMENT_EXECUTE` 包含角色扫描。`HIT_SETTLE` 的 `tryHit` 进入持续接触时会再进入 `settleFlow`。这些都是 inclusive，摘要和使用说明都写了禁止相加、禁止互斥百分比、禁止从观测 tick 里逐项相减。tick 的 P95 与方法平均值分开。`EXPLICIT_SEND` 只包 `TechniqueHitSettle` 的事件、声音和粒子三次调用；`entityData.set`、`setPos`、`ModNetwork` 和 HitVolumes 的火焰粒子没有算进网络。实体盒 `clip` 没有算进 `BLOCK_CLIP`；准星射线留在 `TARGET_QUERY`。

## 4. P95、容量、status 与导出

P95 只对已完成 tick 的纳秒副本做 nearest-rank，下标 `(95*N+99)/100-1`，即 `ceil(0.95*N)-1`。N 为 1、20、6000 时下标分别是 0、18、5699。N 为 0 时平均值、P95 和峰值写 N/A。存储上限 6000，按请求的 tick 数一次分配。`status` 只拼状态、完成/目标、未完整 tick 和最近成功导出路径。`export` 只在 `STOPPED`、门控已关且服务器仍在运行时执行；写入在命令线程同步进行，目录是 `<游戏工作目录>/logs/xueximod-performance/<UTC时间戳>-<序号>/`，会话目录和两个文件都用创建新文件的方式，不覆盖。两个文件都成功才记下路径；失败保留内存。排序和写盘发生在停止之后，不打开采样门控。

## 5. 使用说明

命令、默认 1200、范围 1 到 6000、权限 2、状态机、6000 上限、观测 MSPT 的边界、P95 算法、CSV 列、inclusive 禁令、owned 扫描与查询分列、更新次数的含义，以及裁决第六节的未覆盖项，都与代码和 `summary.txt` 一致。说明写明不承诺帧数或耗时变好，也没有把读数写成已经实测。

## 返修

需要返修的作者：无。没有对应文件。

## 静态核对不能确认的部分

没有运行 `compileJava`，所以没有编译器输出。没有启动集成服或专用服，因此没有核对真实落盘、权限手感、同优先级监听器的实际先后、时钟回绕，以及关采样时的实际分配。类第一次加载会分配一组可复用数组；关闭后的单次调用本身不再分配。循环中途抛出时，已经入世界的实体仍留在世界里，当次生成三计数不刷入记录器；正常结束的循环会刷。这些都不改变玩法返回和调用次数。游戏效果仍须用户实测。
