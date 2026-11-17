我先读取这份任务说明，确认授权范围后再按其中的要求执行。任务已授权由我直接实现，不另派代理。我先读本目录记录和客户端输入，再定位可写源码与调用者。调用方只读取返回列表，不改它。我先按包路径备份待改文件的当前字节，再做空结果短路。命名检查里，本次改动的文件没有新违规。历史两处不在本次范围内，保持不动。正在写最终报告和证据。# 实现 C 最终报告：领域移动空结果

实现者：Cursor CLI grok-4.7-high-fast。本单是已批准设计的实现，不是独立复审，也不构成实现阶段的正式正确性结论。设计 1/2 已批准；实现轮次仍由主会话记账。未创建或运行测试，未编译，未打包，未启动或操控游戏，未提交。已有未提交修改均保留。

## 结论

客户端在领域集合为空，或移动者不符合原有本地预测条件时，返回共享空列表 `List.of()`，不再分配空 `ArrayList`。`DomainMotion.relevant` 在客户端见到空结果时直接返回同一类共享空列表，不再 `addAll` 复制。非空桥接列表的直接调用者只读不改，因此客户端非空结果直接返回桥接列表，不再做第二份复制。该非空列表每次调用现建，不跨 tick 保存。

服务器分支、已有领域的按轴裁切、本地玩家与根载具条件、`ALLOWED` 移动前后配对、领域进入离开的当次读取，以及同维复活行为，都保持原样。

## 实际修改的文件与行号

1. `src/main/java/com/bslx/XueXiMod/client/DomainClientState.java`
   - 第 11 行：增加 `Collection` 导入。
   - 第 16 行：新增字段 `SHARED_EMPTY`。
   - 第 25–43 行：`targets`。本地玩家缺失、移动者为空、移动者不是本地玩家且根载具不同，或 `DomainSightStore.all()` 为空时，返回 `SHARED_EMPTY`。通过上述条件且集合非空时，仍对这一次的 `all()` 视图新建 `ArrayList` 并包装 `ViewerSight`。
2. `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainMotion.java`
   - 第 15 行：新增字段 `SHARED_EMPTY`。
   - 第 106–121 行：`relevant`。客户端第 107–112 行读取桥接列表，空则返回 `SHARED_EMPTY`，非空则返回该列表本身。第 114–120 行服务器分支仍新建列表并筛选同世界的活动领域。
   - `clip`（第 20–49 行）与 `finish`（第 51–74 行）的判断和写回未改语义；仅因新增字段，行号比修改前各后移 1 行。

未改源码：

- `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainClientBridge.java`。它只转发 `source.targets`，默认源已是 `List.of()`。空结果短路和免复制放在实际分配的两处即可，桥接层再包一层没有必要。
- `EntityMoveMixin.java` 及其他源码。只读确认了移动前后调用关系：`move` 头部调用 `DomainMotion.clip`，返回时调用 `DomainMotion.finish`。

备份（修改前字节，包路径保留在 `before-c/` 下）：

- `docs/开发记录/空闲与复活性能优化-20261030/before-c/com/bslx/XueXiMod/client/DomainClientState.java`
- `docs/开发记录/空闲与复活性能优化-20261030/before-c/com/bslx/XueXiMod/shu/technique/release/domain/DomainMotion.java`

## 前后 SHA256 与最后写入时间

`DomainClientState.java`

- 修改前 / 备份：SHA256 `177ECEB2281AC0034DF30D56FD1F8BBB615476DAC152B87A1EBC632F67CCD3F4`，2584 字节，2026-10-23 17:26:15.797。备份与修改前一致。
- 修改后：SHA256 `355D612C449FD7E4981A3FC2C24B05F336749615416BF97A632C03BBAC7E319D`，2832 字节，2026-10-30 01:46:03.809。

`DomainMotion.java`

- 修改前 / 备份：SHA256 `BF7FEDB0B5A1C2C539C78D865902675A7D7DD6A1ED35991B4DEBAB326E342359`，3837 字节，2026-10-27 03:59:41.607。备份与修改前一致。
- 修改后：SHA256 `4B74D8D1F87298217AEF6FC1ED8F0BADB3599088E3314476CF960C19F94C2AE5`，4028 字节，2026-10-30 01:46:01.417。

`DomainClientBridge.java`（未改，修改前后相同）

- SHA256 `CACABD091FC44A61A729E3DB3B2D887FECD6007B854E7186022F47DDCA376476`，649 字节，2026-10-23 14:14:07.510。

## 列表是否被修改

直接返回桥接列表的前提成立：调用者不修改该列表。

全 `src` 中，`DomainClientBridge.targets` 只有 `DomainMotion.relevant` 第 108 行这一处调用。客户端分支只调用 `isEmpty()`，空则另返共享空列表，非空则返回原引用；没有 `add`、`addAll`、`remove`、`clear`、`set`、`retainAll`、`removeIf`、`replaceAll` 或 `sort`。

`relevant` 的结果只有两处使用：

- `clip` 第 27–44 行：`isEmpty()` 后按元素只读迭代，并调用领域上的几何判断。不改列表结构。
- `finish` 第 62–69 行：只读迭代。而且第 55–58 行在 `ALLOWED` 没有记录时已经返回，不会在空预测路径上再取一次列表。

服务器第 117 行的 `found.add` 写的是该方法自己新建的列表，与桥接列表不是同一对象。

因此客户端非空路径可以按已批准设计直接返回桥接列表。空路径即使桥接层交来一份新建空表，也不会再复制。当前 `DomainClientState` 在空路径上交来的就是共享空列表。

`SHARED_EMPTY` 是 `List.of()`，JDK 的不可变空列表。现有调用只做空判断和迭代，不会向它写入。

## 服务器未变与状态配对保持的理由

服务器路径仍在 `mover.level().isClientSide()` 为假时进入第 114–120 行：每次新建 `ArrayList`，遍历 `DomainService.active()`，只收活动且与移动者同一世界的领域。没有把共享空列表套到服务器，也没有改筛选条件。

`clip` 仍先排除乘客、旁观、无物理和近似零位移，再取领域。有领域时仍按原顺序做开启边界、通行、是否装得下、是否完全包含，以及 `clipInside` 按轴裁切。只有 `held` 时才 `ALLOWED.put`。`finish` 仍先 `ALLOWED.remove`；没有记录就返回；有记录才再次 `relevant`，发现身体已不在封闭领域内才 `setPos` 回允许位置。两次查询各自现读，不共用上一次的非空列表，所以领域进入或离开会在下一次 `relevant` 生效。`forget` 未改。

本地预测条件仍是：需要 `minecraft.player`，且移动者就是该玩家，或二者 `getRootVehicle()` 相同。其他实体继续不参与客户端预测。

同维复活未改。没有触碰 `ModEvents`、`DomainSightStore.clear` 或复活克隆路径。`DomainSightStore.all()` 仍是地图的当前视图；有内容的包装列表不放入静态字段，所以不会把旧领域列表留到后续 tick，也不会为了帧数清掉同维复活后仍应存在的领域快照。

## 命名检查

已运行 `powershell -NoProfile -File tools/check-name-gate.ps1`，输出在 `name-gate-c.log`，退出码 1。本次改动的文件没有违规。全仓仅有的 2 处是既有问题，未改：

- `src/main/java/com/bslx/XueXiMod/animation/runtime/OriginalHumanoid.java:165` 变量 `i`
- 同文件第 202 行变量 `to`

## 未完成项

- 统一编译未运行。本任务禁止编译；`记录.md` 还要求等双方源码停写后再由指定编译者编译。
- 游戏内效果未验证。
- 不估计 FPS，也不把这次分配减少说成帧数收益。
- 未改 `DomainClientBridge.java`，原因见上。
- 未改历史命名违规。
- 未创建或运行测试。

## 停写

`DomainMotion.java` 与 `DomainClientState.java` 已写完。本会话不再写入任何源码，并释放这两份文件的占用。待统一编译。未做游戏验证。

归档：`docs/开发记录/空闲与复活性能优化-20261030/implement-c-report.md` 与 `implement-c-evidence.json`。报告写完后已再核过两份源码哈希，与上文修改后 SHA256 一致。
