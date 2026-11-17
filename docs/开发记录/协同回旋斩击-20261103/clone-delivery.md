# 分身核心交付（C）

本单只写 `shu/technique/release/clone/core/` 和这份说明。没有改公共 api、`CloneBalance`、注册器、命令、语言、网络、客户端，也没有改水幻形 capture/action 包。没有编译，没有游戏验证，没有正式实现复审。

## 给 I 的工厂

```java
CloneServerPorts ports = CloneServerPorts.create(sessionLookup);
```

`D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/CloneServerPorts.java` 第 51 行。同一个实例实现三个端口，不要各 new 一份：

- `CooperationActors`
- `ShortReposition`
- `CloneStandaloneDeployment`

`ServerSessionLookup.sessionOf(server)` 返回这台服务器的协同会话 UUID。`cloneSessionToken(该 UUID)` 必须等于 `CloneRuntime.get(server).sessionToken()`。对不上时 `tryReserve` 返回无演员预约，不占锁。C 不发明会话 UUID，也不把 long 令牌哈希成 UUID。

## 普通部署

`deployStandalone(ServerPlayer, CloneDeploymentInput)` 在第 382 行，转到 `CloneService.deployStandaloneBody`（第 137 行）。

- 来源不是 `PLAYER_STANDALONE` 时返回 `PERMISSION`，开发命令不能借这个方法绕权限。
- 先走无副作用的 `inspectStandalone`（第 93 行）：主人、报价范围、主手栈引用和选中槽、安全生成点、名额和生成冷却。通过之后才 `launchWithQuote`（第 168 行）占名额、`ManaPaymentReceipt.charge(quote.manaCost())`、发布、失败则 `receipt.refund`、成功才装生成冷却。
- 报价已含一次载体折扣。这里不再读 `CloneBalance.readSpawn`，也不再打第二轮折扣。
- `dismissStandalone(owner, NORMAL)` 第 387 行，只结束该主人 `externalCooperation` 开启的普通身体及其预约。返回数量。不扣钱，不重置已有生成冷却。水幻形和开发幻影规格是关闭的，不会被撤回。开发命令生成的普通分身规格是开启的，潜行撤回会一起结束；`/clone dismiss` 仍走原来的 `CloneService.dismiss`。

旧 `/clone spawn` 仍调用 `CloneService.spawn`。它和玩家部署共用 `launchWithQuote`。开发幻影仍要求身体和脉冲都是 `DEVELOPMENT_ONLY`（`developmentOnly` 第 826 行）。普通分身不再因为身体可用性改成玩家就拒绝开发命令。命令权限 2 仍在 I 的命令入口，C 没有改命令。

## 规格

`CloneSpec` 增加组件 `ExternalCooperation externalCooperation`。14 参旧构造默认 `CLOSED`，水幻形 `CapturedCasts` 不用改。`CloneService.specFor` 第 817 行：普通分身 `OPEN`，幻影 `CLOSED`。I 若自己 new 普通分身规格，必须传 `ExternalCooperation.OPEN`，不要再造一份关闭的普通身体。

## 协同预约

`tryReserve` 第 59 行。找不到演员时返回 `actorPresent=false` 的预约，不留锁。

- 只查该主人索引，最多 4 具。按到玩家脚点的距离平方，再按身体 UUID 排序。
- 左右站位是脚点 + 冻结水平前向 × 0.5 ± 右侧 × `sideOffset`。移动距离更短的一边入选；相等固定右侧。近垂直瞄准用 `visualYaw` 的水平前向。
- 水幻形、开发幻影、关闭外部协同、非 `REPEAT`、已有 pending、捕获租约、`BREAK_ON_TRIGGER`、`CloneSourceScope` 响应来源都跳过，不报错。
- 与旧手动/受击共用 `READY→RESERVED`。预约期间 `CloneTriggerPolicy` 返回 `BUSY`，不扣脉冲费、不花次数。
- 预约记下 family、token、身体世代、会话、维度和到期刻。到期不超过 80 刻，准备窗口 2 刻。
- 全服预约不超过 128。新预约和每次实际归位步计入原每 tick 32 次工作预算，不另开 32。预算不够返回无演员或 `UNAVAILABLE`，供单人回退。
- `advance` 第 115 行：`PREPARING` / `READY` / `UNAVAILABLE`。归位失败停在最后一个安全点，不传回旧点，并清未提交预约。
- `readyFrame` 给出协同演员：序号 1，`mirrored=true`，瞄准是预约时冻结的共同方向。C 不调用 `ActorActionPublisher`，也不引用客户端类。姿态由 I 用这个帧和已有 `ActionPlayback` 发布。
- `validate` 第 199 行用当前玩家脚点加冻结前向重算站位。偏差超过 1 格、身体不安全或凭据失效时返回 false，不拉玩家。
- `markCommitted` 第 242 行按成员序号幂等。第一次成功后不可撤销。
- `finish` 第 262 行和 `cancel` 第 267 行只看 C 自己记下的提交标记。调用方传 false 不能取消已提交。已提交则进入部署时冻结的内部响应冷却一次，`REPEAT` 不改成一次性。未提交则回到 `READY`，不记冷却。
- 未提交预约过期或两刻仍未到位时，`CloneRuntime.expireCooperation` 第 680 行清锁。已提交的预约保持到 `finish`/`cancel` 或身体结束，不自动退伤害。
- 登出、死亡、复制、换维、世界卸载、停服仍走原生命周期；结束身体时丢掉预约。软失败只放未提交预约。已发布伤害不在这里倒扣。

## 短距移动

`ShortReposition.prepare` 第 272 行，`advance` 第 321 行，`cancel` 第 370 行。只接受可动普通身体（普通、外部协同开启、非捕获）。时长 1 或 2 刻，距离 0 到 8 格。每步最多 8 个采样，并用起点盒和终点盒的并集做保守扫掠。检查完整身体盒、方块和实体、世界边界、建造高度、已加载区块、脚下方块的上表面支撑，以及流体、火、岩浆、仙人掌、岩浆块、营火、甜浆果、凋零玫瑰、细雪、滴水石锥。不加载区块，不上下搜索落点。`cancel` 可重复，不把身体拉回起点。

## 自检

命名门禁 `tools/check-name-gate.ps1` 退出码 0。没有跑 `compileJava`，没有进游戏，没有正式复审。

## 风险

- 支撑判断用 `isFaceSturdy` 的完整面。台阶、楼梯一类上表面可能被当成不能站，该侧站位失败后回退单人。
- 方块碰撞只在 `hasChunkAt` 为真之后读取。邻块形状是否会间接加载区块，要等统一编译和游戏验证。
- 载体检查比对冻结的主手栈引用和选中槽，不调用 `CarrierWatch.observe`，避免清掉水刃拍号。同一栈换槽再换回来时，观察世代变了，但栈引用仍在，部署检查会通过。
- 会话对照没接上时，协同全部表现为没有演员，玩家部署本身不受影响。
- 克隆提交队列和捕获票已经占满当 tick 的 32 次时，新的协同准备会软失败。
