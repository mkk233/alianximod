我先读完这份返修任务说明，再按它划定的范围改。这单只写协调契约，不改源码。我先读返修裁决，再对照自己的预算和执行实现来定接口。裁决要求按现有上下文接线。我只读自己的预算、自动轮和上下文实现，用来定 B 要调用的签名。现有实现把 `effect.execute` 的正常返回当成成功，并且用同一个先置位的完成标记挡住随后的回执。契约会把收费改成按计划复制份的真实发布记录，并和在途结清分开。发布观察契约已经写入 `docs/开发记录/领域绑定规则-20261030/repair-commit-contract.md`。本单没有改源码，没有编译，没有跑命名门。

接口在 `com.bslx.XueXiMod.shu.technique.release.bind.PublicationAttempt`，挂在现有的 `ReleaseExecutionContext` 上。没有第二套账本，没有全局实体事务，没有新的 Mixin，也不按术式 id 分支。手动路径栈为空，`current()` 为 null，B 保持原发布行为。

## B 调用的三个方法

`current()` 返回当前线程栈顶尝试；栈空时返回 null。

`allowsPublication()` 返回 true 只表示还允许尝试，不表示已经收费。返回 false 时不得调用 `addFreshEntity`，不得进入剑控制状态，也不得调用 `reportAccepted()`。真正发布前 B 仍要自己核验令牌。令牌无效或 `allowsPublication()` 为 false，都不发布、不报告。

`reportAccepted()` 只在 `addFreshEntity` 已返回 true，或既存剑已经实际进入该次有效控制状态之后调用。

- 返回 true：这一计划复制份已记成功。同份再调用仍返回 true，费用只留一次。返回 true 之后再抛异常，这一份费用仍然保留。
- 返回 false：这次通知不进松散账，预算不因此提交松散费用。不得据此删除已发布实体，也不得改报成成功。宿主组作用域固定返回 false。

空剑列表、`addFreshEntity` 返回 false、控制状态没变、幻剑的冻结武器或当前资格不满足而早退，都不调用 `reportAccepted()`。两道幻剑检查都保留。同一计划份里多个实体可以多次报告，只收这一份的价，不按实体个数加价。

## A 独占的打开与关闭

只有已安装的自动轮可以调用：

- `openLoose(UUID roundId, int memberIndex, int copyIndex)`
- `openHostGroup(UUID roundId, int memberIndex)`
- `close()`
- 另有 `roundId()`、`memberIndex()`、`copyIndex()`、`kind()`、`accepted()`，以及 `Kind.LOOSE_COPY`、`Kind.HOST_GROUP`

`roundId` 用 `ReleaseExecutionContext.roundId()`。`memberIndex` 是计划成员序号。松散 `copyIndex` 从 0 起，宿主组的 `copyIndex()` 固定为 -1。

打开压入独立线程栈，`close()` 弹出并恢复外层。同一句柄第二次 `close()` 无作用。不是栈顶就抛 `IllegalStateException`，且不把本轮结清成成功。已请求取消或已结清时打开返回 null，这一份不执行。

松散路径在每一份 `runSegment` 外打开。`runSegment` 自己不打开尝试，也不再把 `effect.execute` 正常返回当成成功。宿主组在整次 `commitHostGroup` 外打开；组内不再开松散尝试。整组只在既有回执为 `COMMITTED` 时保留整组价，其他回执整组不留。

## 费用

一轮预留总额不变。双重仍是 `groupBill()`，非双重仍是 `pressCommitCost`，自动仍 `reserve(..., false)`。手动预留和提示不变。

双重的复制池是各 `memberShares`，不含 `ruleShare`。`ruleShare` 只归属成员 0，结算一次。非双重的复制池就是那次 `pressCommitCost`，不再拆出新规则价。

份数是发射时的 `max(1, copiesBefore)`。分解的是已经预留的池，不重新计价。第 i 份价格是 `pool / copyCount`，余数 `pool % copyCount` 分给较小序号，各份之和等于该池。

松散成功份在自己的 `close()` 时提交自己的价格。成员 0 的规则份额仅在至少一份松散成功时整笔保留一次。宿主组不逐份计价：`COMMITTED` 时一次保留复制池，成员 0 再加整笔规则份额。`commit` 必须在 `settle` 之前。`settle` 只退剩余未提交份额，并且只结清一次。

## 取消与 done

取消请求、在途次数、已经结清分开记录。不再用同一个先置位的完成标记挡住随后的成功回执。

`cancel()` 幂等。没有在途时，返回前就结清，`done()` 为 true。有在途时不退款，`cancelled()` 为 true，`done()` 仍为 false。`done()` 只在预算实际结清之后为 true。

同线程重入取消会立刻禁止新尝试、后续发布和未开始的延迟成员。已经打开的那一次不提前退款。若发布已经成功，随后的 `reportAccepted()` 仍返回 true，并在 `close()` 时保留该份。`close()` 先按真实回执提交，再减在途；在途回到 0 且已取消或成员都结束时才退剩余金额。异常走完 `close()` 后继续抛出。

`addFreshEntity` 或控制状态失败：不报告，不收费。报告返回 true：该计划份收费，之后的异常不倒退。报告返回 false：不收松散份。复制中途异常：已成功的份保留，未成功的份不收费。

`executeAutomatic` 返回后，若实例已在这次栈里关闭或到期，调用方立刻 `cancel()` 返回的句柄，并且不把未完成句柄挂回去。没有在途时当场结清。延迟成员见到取消后不得再生成。
