# 发布观察契约（A 交给 B，C 只使用句柄语义）

本文件是实现第 1/3 轮返修里 R1、R2、R3 的唯一发布观察接口。只固定签名、返回值和调用时点。本单不改源码。

包名：`com.bslx.XueXiMod.shu.technique.release.bind`

类名：`PublicationAttempt`

它挂在已经存在的 `ReleaseExecutionContext` 上：自动轮在绑定该上下文的同一段 `try/finally` 里打开和关闭尝试。没有第二套收费账本，没有全局实体事务，没有新的 Mixin，不按术式 id 分支。

手动路径没有尝试入栈。`current()` 为 null。B 保持原来的发布、提示、几何和控制，不调用本接口。

## B 调用的方法

```java
@Nullable
public static PublicationAttempt current()

public boolean allowsPublication()

public boolean reportAccepted()
```

`current()` 返回当前线程栈顶尝试。栈空时返回 null。

`allowsPublication()` 只回答这一次还能否发布。返回 true 表示允许尝试，不表示已经收费。返回 false 时，B 不得调用 `addFreshEntity`，不得进入剑控制状态，不得调用 `reportAccepted()`。

`reportAccepted()` 只在下面两件事已经发生之后调用，二选一：

- `addFreshEntity` 已经返回 true。
- 操纵既存剑的控制方法已经实际进入该次有效控制状态。

返回值：

- true：这一计划复制份已经记成成功。同一份之后再调用仍然返回 true，费用只留一次。返回 true 之后，即便后续抛出异常，这一份费用仍然保留。
- false：这次通知不进入松散复制账。预算不因为这次调用提交松散费用。B 不得据此删掉已经发布的实体，也不得把 false 改报成成功。

空剑列表、`addFreshEntity` 返回 false、控制状态没有改变、幻剑资格或冻结武器不满足而早退：都不调用 `reportAccepted()`。这些情况是失败，对应费用保持未提交。幻剑的冻结武器检查和当前资格检查都保留。

B 在真正发布前仍然自己核验令牌。令牌无效时不发布、不报告。`allowsPublication()` 为 false 时同样不发布、不报告。两个条件都要过。

一次计划复制份里合法生成多个实体时，可以多次调用 `reportAccepted()`。第一次成功记下这一份，后面几次只返回 true，不按实体个数加价。构造了实体、调用了 execute、数了调用次数，都不是收费依据。

## A 打开和关闭的方法

只有已安装的自动轮执行器可以调用。B 不调用。

```java
public enum Kind {
    LOOSE_COPY,
    HOST_GROUP
}

@Nullable
public static PublicationAttempt openLoose(UUID roundId, int memberIndex, int copyIndex)

@Nullable
public static PublicationAttempt openHostGroup(UUID roundId, int memberIndex)

public void close()

public UUID roundId()
public int memberIndex()
public int copyIndex()
public Kind kind()
public boolean accepted()
```

`openLoose` 的 `copyIndex` 是该成员 `runSegment` 的复制序号，从 0 起。`openHostGroup` 的 `copyIndex()` 固定为 -1，避免和松散的第 0 份串账。

打开时压入独立于 `ReleaseExecutionContext` 的线程栈。`close()` 弹出这一层并恢复外层。同一句柄的第二次 `close()` 无作用。`close()` 必须对当前栈顶调用；不是栈顶就抛 `IllegalStateException`，并且不把本轮结清成成功。执行器用 `try/finally` 保证本层先关闭。

已经请求取消或预算已经结清时，打开返回 null。调用方不执行这一份，也不对 null 调用 `close()`。新的尝试因此不再开始。

`roundId` 使用 `ReleaseExecutionContext.roundId()`。`memberIndex` 是 `BoundReleasePlan` 的成员序号，双重释放是 0 然后 1。

松散路径：自动轮在每一份 `runSegment` 外包一层 `openLoose` / `close`。`SkillSegmentRunner` 不再用 `effect.execute` 正常返回来设置成功标记。`runSegment` 自己不打开松散尝试。

宿主组路径：自动轮在整次 `commitHostGroup` 外包一层 `openHostGroup` / `close`。组内的 `runSegment` 不再打开松散尝试。组内的 `reportAccepted()` 一律返回 false，不把暂存发布记成松散成功。整组是否收费只看这次 `commitHostGroup` 结束后的既有回执：`HostGroupReceipt` 为 `COMMITTED`（`ReleaseOutcome.committed()`）才保留该成员的整组价；其他回执整组不留。不把宿主组拆成按份或按实体计价。

嵌套时内层先关闭，外层尝试保持原样。延迟回调重新进入时自己打开，结束时弹出，不留到下一拍。

## 费用怎么分解

一轮预留的总额不变。双重仍是 `CompiledReleaseStep.groupBill()`：`ruleShare` 加各 `memberShares`。非双重仍是冻结链的 `pressCommitCost`。自动预留仍是 `GroupBudget.reserve(owner, cost, false)`。手动 `reserve(owner, cost)` 仍提示玩家，不走本分解。

内部分开，不再把规则份额加进成员 0 再整段提交：

- 双重：成员 i 的复制池是 `max(0, memberShares.get(i))`，不含 `ruleShare`。`ruleShare = max(0, compiled.ruleShare)`，只归属成员 0，结算一次。
- 非双重：成员 0 的复制池就是那次 `pressCommitCost`，规则份额按 0。不再从中拆出新的规则价。

计划复制份数是该成员发射时的 `max(1, copiesBefore(...))`。被分解的是预留时已经确定的复制池，不按发射重新计价，不新增费率。

对复制池 `pool` 和份数 `copyCount`：

- `base = pool / copyCount`（向零的整数除法，价格非负）
- `remainder = pool % copyCount`
- 第 i 份价格 = `base + (i < remainder ? 1 : 0)`，i 从 0 起

各份之和等于该成员复制池。余数分给较小的序号。禁止逐份四舍五入。

松散成功的一份在其 `close()` 时提交自己的价格。同一份多次 `reportAccepted()` 只提交一次。没报告成功的份保持未提交。成员 0 的规则份额只在该成员至少有一份松散成功时整笔保留一次；一份都没有就随未提交金额退回。规则份额不按份拆开，也不按份重复收。

宿主组不使用上面的逐份价格。`COMMITTED` 时一次保留 `复制池 + （成员 0 的规则份额，否则 0）`。未 `COMMITTED` 时这一成员连同规则份额都不留。组内实体个数不改变这笔组价。

`GroupBudget.commit` 仍是保留原语，只保存原来的收据。`commit` 必须发生在 `settle` 之前。`settle` 仍只退还剩余未提交份额，并且只结清一次。

## 取消、在途和 done

取消请求、在途次数、已经结清是三个独立状态。禁止再用同一个先置位的 `AtomicBoolean` 同时表示取消和完成，以免随后的成功回执被挡掉。

`RoundHandle` 的可见语义：

- `cancel()`：幂等。置取消请求。在途次数为 0 时，返回前就结清，随后 `done()` 为 true。在途次数大于 0 时，不退款，`cancelled()` 为 true，`done()` 仍为 false。
- `cancelled()`：已经请求取消。可以在 `done()` 仍为 false 时为 true。
- `done()`：只有预算实际 `settle` 完成之后才为 true。

同线程重入 `cancel()` 立即禁止新的尝试、后续发布和尚未开始的延迟成员。已经打开且尚未 `close()` 的那一次不提前退款。`allowsPublication()` 随取消请求变为 false。这次若已经通过允许检查并且 `addFreshEntity` 或控制状态已经成功，接着调用的 `reportAccepted()` 仍然返回 true，并在 `close()` 时保留该份价格。

`close()` 的顺序固定：先按本层真实回执 `commit`，再弹出，再减少在途次数。在途回到 0，且已经请求取消或全部成员都已结束时，才 `settle` 剩余未提交份额，然后 `done()` 为 true。异常先走完这个 `close()`，再继续向外抛。不吞异常，不补造成功，不删除已经正常生成的实体。

没有在途发布时，`cancel()` 立即退款，不把普通关闭拖到以后的 tick。

四种结果：

- `addFreshEntity` 返回 false，或控制状态没有提交：不调用 `reportAccepted()`。该份不收费。
- 返回 true 或控制状态已经提交，并且 `reportAccepted()` 返回 true：该计划份收费。之后的异常不倒退这一份。
- `reportAccepted()` 返回 false：不提交松散份。宿主组作用域就是这个结果；组价另看 `COMMITTED`。
- 复制循环中途异常：已经报告成功的份保留，尚未成功的份不收费，异常继续抛出。

`executeAutomatic` 返回后，若领域实例已在这次调用栈里关闭或到期，调用方立刻 `cancel()` 返回的句柄，并且不把未完成句柄挂回该实例。同一线程上尚未关闭的尝试由上面的在途规则结清，不等待未来回调。正常已经关闭的实例不再持有这个新句柄。延迟成员见到取消请求后不得再生成。

当前没有正蓄力攻击。本契约不新增蓄力玩法。将来若仍走现有延迟回调，尝试的打开和关闭放在回调之内，规则与上面相同。
