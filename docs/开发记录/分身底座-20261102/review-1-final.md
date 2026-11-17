我先完整读这份任务说明，再按其中的范围执行。任务是第 1 轮只读复核：先核对合同、作者报告和设计第 14.1 节，确认真实接入已经停写后再审代码。三方已停写，正式源码在 `src/main`。接下来按合同八项对照真实接入，不把候选目录当完成。实现第 1/3 轮只读复核已完成。三方最终反馈都已停写，正式树在 `src/main`，本轮只审这棵树。发现 1 个阻断问题。

## 阻断

**严重度：高。** 普通分身第一次成功提交之后，下一次只要在提交前失败或过期，这具分身就永久停在 `RESERVED`。

`CloneRuntimeState.commitLine` 在跨过提交线时被设为 true，之后没有任何路径把它清回 false。

```507:509:src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/CloneService.java
            crossCommitLine(runtime, state, actor, actorFrame, token, attempt.prepared.quotedCooldownTicks());
            attempt.commitLine = true;
            state.commitLine = true;
```

成功收束只把响应写成 `COOLDOWN` 并清掉 `pending`，不碰这个标记：

```591:597:src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/CloneService.java
        if (state.response == CloneResponseState.COMMITTING) {
            state.response = CloneResponseState.COOLDOWN;
            state.pending = null;
            if (state.actor != null) {
                state.actor.mirror(state);
            }
        }
```

下一次失败走 `restoreReady`，一看到上次留下的 `state.commitLine` 就返回，`RESERVED` 和 `pending` 都留着：

```600:605:src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/CloneService.java
    private static void restoreReady(@Nullable CloneRuntimeState state) {
        if (state == null || state.commitLine || state.life != CloneLifeState.ACTIVE) {
            return;
        }
        if (state.response == CloneResponseState.RESERVED) {
```

过期取消同样被这个标记挡住，队列项被摘掉，预约却不还：

```394:397:src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/CloneService.java
    static void cancelQueued(CloneRuntime runtime, CloneActionRequest request) {
        CloneRuntimeState state = runtime.find(request.handle().actorId());
        if (state == null || state.commitLine) {
            return;
```

`CloneRuntime.cancelPending`（约 371–374 行）用的是同一个标记。

**触发：** 普通分身有一次成功脉冲，冷却结束后再排队。下一次没目标、被挡、魔力不足、报价非法，或在 2 tick 内没轮到执行。幻影提交后就结束，碰不到这条。连续成功也不会暴露。

**影响：** 响应停在 `RESERVED`。新的主动和受击请求都是 `BUSY`。冷却刷新只处理 `COOLDOWN`，不会把它拉回 `READY`。这具分身仍占主人名额，只能撤回后重召。同一次尝试的退款门闩是 `Attempt.commitLine`，演员身上的标记不该活过这次收束。

**最小修法：** 在 `settleCommitted` 里，普通分身写入 `COOLDOWN` 之后把 `state.commitLine` 设回 false。`restoreReady`、`cancelQueued`、`cancelPending` 只跳过当前这次已经过线的尝试。

## 作者声明和代码

代次读取口已经在 `CloneEntity.generation()`。`ClonePositionPolicy.frame` 组帧时会读它。B 的准备仍只拒绝帧上代次小于 1；帧是执行当刻从实体现造的，没有看到因此打到旧代次的路径。`/shu clone respond` 已走 `CloneService.handleFor`，找不到就回「不在本人索引」。渲染是 `HumanoidModel` 加上原版僵尸层和 `textures/entity/zombie/zombie.png`，设计允许这条替代。命名门禁按接入记录没有跑成。

## 未验证

未编译，未跑命名门禁，未进游戏。接入后的正式文件哈希没有重算；结论来自当前 `src/main`。冷却在 `ServerTickEvent.END` 才从 `COOLDOWN` 回到 `READY`，到期那一 tick 里、END 之前的命令仍会看到冷却。冷却 0 因此最快隔一个完整命令窗口才能再排队，仍保证每个演员每个 tick 最多提交一次。和主人精确重叠时，原版互推在水平间距小于 0.01 时不动；走开之后会不会轻推，没有运行证据。区块卸载、同 JVM 重开、诱敌在原版 AI 上是否留目标，都依赖事件顺序，没有实测。

## 审查覆盖

八项里，除上面这条状态门闩外，其余对照当前正式源码没有第二处阻断：

1. 旧构造默认 `PLAYER`，`getAll()` 仍是全量。学习、自动学习、列表、刻印、书、装配和法杖拒绝都走玩家目录或 `allowsPlayerUse`。`SkillUsePacket` 在四种收费分支之前整链拒绝，失败返回 false 并松开按住。
2. 三个术式和释放定义已登记；脉冲在 `DAMAGE_HOSTS`，两个生成术式不在。专项键没有再声明魔力、冷却和伤害倍率。读取都带 `BalanceSide.SERVER`，伤害、魔力和冷却各经 `BalanceService` 缩放一次。静态初始化没有读快照。
3. 出生票在 `addFreshEntity` 前绑定；重叠不挡生成，实心、边界和未加载会拒绝。`noSave`、读档和没有票的召唤不能进入 `ACTIVE`。登出、死亡、换维、离开世界、世界卸载和停服都进同一个 `end`。重力还在，经验及掉落关掉，`removeWhenFarAway` 为 false。
4. 预约不扣费；额度、冷却和序号在提交线写入。提交后不退款。`CloneSourceScope` 包住 `commit`，关闭时回到上一层。环境伤不反击，致死不反击，主人和友方在 `super.hurt` 之前拒绝。
5. 射线从分身眼睛出发，主动方向用请求里冻结的视线。反击按 UUID 重取包围盒中心。`DamagePermission.allows` 在 `classifyRespecting` 之前。没有 `classifyRoot`、火种、潮印、`Tally` 或 `ChainScheduler`。0 点走既有的无正伤裁决。
6. 扣款用 `ManaPaymentReceipt.charge` 的返回值。失败退款有一次性标记，并且在提交线之前。生成预留在失败时释放。
7. 诱敌只在有演员、半径大于 0、轮到且未超过每 tick 4 次时查询。白名单是 `Enemy` 且为僵尸或骷髅族。结束时只清仍指向自己的租约。
8. 实体、属性和渲染按 Forge 注册；渲染只在客户端事件里。同步字段有变化才写。命令权限 2，只操作本人。性能枚举加在末尾，`ticks.csv` 表头仍是原来的 11 列。采样关闭时既有 `begin` 不读 `nanoTime`。
