# 协同距离公共端口（2026-11-04）

本单只交公共分界、远距端口和执行族的单一预约调用。远距身体扫掠、DDA 和真正搬移留给分身核心。没有编译，没有进游戏，没有改世界配置，也不把这份说明当成实测通过。

## 代码默认和现存覆盖

两边不是同一件事：

| 位置 | `selectionRadius` | `moveLimit` | `sideOffset` |
| --- | --- | --- | --- |
| 设计目标 | 默认 24，范围 [0,48] | 默认 32，范围 [0,64] | 默认 1.25，范围 [0.75,2]，不变 |
| 现行声明 `CooperativeContent` | 默认仍是 **8**，范围仍是 **[0,16]** | 默认仍是 **4**，范围仍是 **[0,8]** | 默认 1.25，范围 [0.75,2] |
| 本单结构上限 | 记录可接受 0 到 48 | 记录可接受 0 到 64 | 仍是 0.75 到 2 |
| 世界存档 | 未读、未改 | 未读、未改 | 未读、未改 |

`network/CooperativeChainCompiler` 仍在执行族准备时用 `BalanceService.number(BalanceSide.SERVER, "rule_cooperative", key)` 冻结这三个键，构造签名没变。声明上限没放宽之前，冻结值不会变成 24/32。缺键继续落到声明里的 8/4。已经写进世界的 8/4，包括恰好等于旧默认的值，都还在原文件里。0 仍然只允许原地，远距分支不会把 0 变成不限距离。

生效时机沿原配置：重开世界或重启服务器。本单不声称热更新。

`docs/配置平衡指南.md` 不在本单写入范围，没有改。指南目前没有把 24/32 写成已经生效的代码默认。

## 当前开发世界

项目里的现有日志不能唯一指出开发世界，所以没有改任何存档：

- `D:\alianximod\logs\latest.log` 是空文件。
- `D:\alianximod\logs\debug.log` 没有存档路径或世界名。
- `D:\alianximod\run-data\logs\latest.log` 是 `forgedatauserdev` 数据生成，`--gameDir` 为 `.`，没有加载世界。
- 项目下没有 `server.properties`，也没有 `run/**/level.dat`。

没有遍历用户的其他世界。全局模板也没有改。

## 已接上的调用

`FamilyExecutionService.beginGate` 仍只调用 `CooperationActors.tryReserve`，然后在还没有 READY 时调用 `advance`。费用、预约、READY 之后的动作、双人失败改单人，都沿原路径。执行族不调用 `ShortReposition`，也不调用 `RemoteRecall`，避免和核心各搬一次。

`CooperationActors` 新增默认方法，现有实现类不用改就能编译：

```java
default RecallKind recallKind(double travelBlocks, CooperationSettings settings)
```

它转给 `RecallRoute.kindFor(travelBlocks, moveLimit)`。

## 给分身核心的接点

包都在 `com.bslx.XueXiMod.shu.technique.release.cooperative.api`。没有新的实例工厂。远距端口应加在现有 `CloneServerPorts` 这一个对象上，不要再造第二个归位服务。`CloneServerPorts` 本单没有改。

分界：

```java
public enum RecallKind { SHORT, REMOTE_RECALL, OUT_OF_RANGE }

public final class RecallRoute {
    public static RecallKind kindFor(double travelBlocks, double moveLimit);
    public static double shortSweepLimit(double travelBlocks, double moveLimit);
}
```

- 实际位移不超过 8 格，并且不超过冻结 `moveLimit`：`SHORT`。`shortSweepLimit` 返回这个实际位移，交给原来的 `ShortReposition.prepare`。不要传入冻结上限本身。
- 大于 8 格且不超过冻结上限：`REMOTE_RECALL`。
- 超过冻结上限、非有限，或上限为 0 但身体不在原地：`OUT_OF_RANGE`，当次单人。

短距合同没放大。`REPOSITION_DISTANCE_CAP` 仍是 8。`AcceptedReposition` 仍拒绝大于 8 的距离。`SWEEP_SAMPLE_CAP`、准备 2 刻、每刻 32 个工作单位都没加。

远距端口：

```java
public interface RemoteRecall {
    RecallAttempt recall(ServerLevel level, CloneHandle handle, Vec3 destination, double frozenMoveLimit);
}

public sealed interface RecallAttempt permits AcceptedRemoteRecall, RejectedRecall {}

public record AcceptedRemoteRecall(UUID planId, CloneHandle handle, Vec3 destination,
                                    double travelBlocks, double frozenMoveLimit, RemoteSpaceGrant grant)
        implements RecallAttempt {}

public record RejectedRecall(CancelReason reason) implements RecallAttempt {}
```

`recall` 必须在同一次服务端调用里做完有界检查并搬到目的站位。成功后核心再标 READY。执行族若发现已经 READY，就不会再 `advance` 短距计划。失败返回 `RejectedRecall`，不造替身，不消耗响应，不把没查完的路径当成安全。验证结论不跨 tick 保存；若核心必须分刻，搬移那一刻要重新走完整检查。

选择顺序仍是核心现有的距离、UUID 和左右站位。最多 4 具身体、每具 2 个站位做廉价端点检查。选定第一条端点合法的候选后，只尝试这一条远距通道，失败不再换身体做长路径。

额度：

```java
public enum RemoteCheckPhase { ENDPOINT, CHANNEL }

public final class RemoteRecallMeter {
    public static Optional<RemoteSpaceGrant> take(long serverTick, RemoteCheckPhase phase,
            int cellCount, int stepCount, int entityCount, int chunkCount);
}
```

- 端点：`ENDPOINT`，方块 1 到 128，步进、实体、区块都必须是 0。同一次协同的端点与支撑合计也不得超过 128，这个合计由核心自己累加。
- 通道：`CHANNEL`，方块 1 到 512，中心线推进不超过 128，实体候选不超过 64，区块不超过 16。`AcceptedRemoteRecall` 只接受通道额度，端点额度不能当成已经搬移。
- 全服每刻合用 2048 个方块单元和 256 个实体候选。换刻清零。超出返回空，不截断。
- 实际搬移和完整路径检查仍消耗既有 `COOPERATION_PREPARE_PER_TICK_CAP`（32）。本计量器没有第二份 32。预算不够就当次单人，不排到第三刻。

`RemoteSpaceGrant` 的构造对包外不可见。核心不能自己 new 一个许可来跳过计量。

现行 `CloneServerPorts.stanceFor` 仍把 `settings.moveLimit()` 传进短距 `pathProblem`。那个函数在上限大于 8 时直接拒绝，连近处身体也会失败。所以在核心改成按 `recallKind` 分支之前，不能把 `CooperativeContent` 的默认或上限改成 24/32。本单也没有改那个声明。

## 开放键

没有新键。`sideOffset` 和前距 0.5 不变，斩击盒、伤害、费用、冷却都没动。

| 键 | 单位 | 设计范围与默认 | 本单之后实际冻结来源 | 消费时机 |
| --- | --- | --- | --- | --- |
| `selectionRadius` | 格 | 默认 24，[0,48] | 声明仍是默认 8，[0,16] | 执行族准备时冻结，核心按主人脚点到身体脚点的三维距离过滤，最多 4 具 |
| `moveLimit` | 格 | 默认 32，[0,64] | 声明仍是默认 4，[0,8] | 同一次冻结。表示身体到候选站位的最大三维位移，不增加攻击距离 |
| `sideOffset` | 格 | 默认 1.25，[0.75,2] | 与声明一致 | 站位，脚点加前向 0.5 再加减侧距 |

准备刻数仍是结构常数 2，不是平衡键。

## 本单没改的文件

这些文件要么不在写入名单，要么真实路径和名单不一致。没有自行扩大授权：

- `src/main/java/com/bslx/XueXiMod/shu/technique/release/cooperative/CooperativeContent.java`：两个距离键的声明、默认和 `BalanceBound` 仍是 8/[0,16]、4/[0,8]。要让 24/32 进入冻结值，必须改这里，并且要排在核心分支之后。
- `src/main/java/com/bslx/XueXiMod/network/CooperativeChainCompiler.java`：名单写的是 `cooperative/CooperativeChainCompiler.java`，真实文件在 `network`。构造调用已经能传入放宽后的记录，本单不改。
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/`：远距实现未写。
- `cooperative/attack`、`cooperative/action`、客户端、网络包、`BalanceCatalog`、语言、`docs/配置平衡指南.md`：未改。
- `api/FamilyExecutionRuntime.java` 的方法签名未改。名单里的 `cooperative/FamilyExecutionRuntime.java` 和 `cooperative/CooperationRuleEffect.java` 不存在，没有新建。

## 自检

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 0。

未做统一编译、打包、游戏、提交、制作表写回，也没有正式复审。实现额度已用完，本单不追加复审。
