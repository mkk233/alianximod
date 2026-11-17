我先读完这份返修任务，再只在它授权的范围内改。契约要求先把拟改文件的现行全文备份出来，读回确认后才改。我先把 `AutomaticRound` 和 `SkillSegmentRunner` 完整读出来。备份先落盘。`AutomaticRound` 和 `SkillSegmentRunner` 都按现行全文写入 `backup-A-repair-1`，读回对上之后才改。`AutomaticRound` 的备份已经写入。接下来把 `SkillSegmentRunner` 的现行全文按同样路径存好，并读回备份确认。成功标记已经从执行返回上拿掉。接下来按契约改自动轮：按计划复制份记账，并把取消、在途和结清拆开。released=true

真实提交和部分退款已经按发布观察契约接到自动轮上。统一编译没跑，费用窗口还没有验证。

## 接口

B 仍然只调用这三个方法，签名没改：

- `PublicationAttempt.current()`：栈空时返回 null。手动路径不入栈。
- `allowsPublication()`：返回 true 只表示还允许发布。取消、令牌失效、轮次不符或尝试已关闭时返回 false。
- `reportAccepted()`：只对松散计划份。第一次成功记下该份并返回 true，同份再调用仍返回 true。宿主组固定返回 false。返回 true 之后，即使重入取消，这一份仍然算成功。

执行器使用 `openLoose`、`openHostGroup`、`close` 和 `noteHostCommitted`。账本是 `PublicationAttempt.Ledger`，由自动轮实现。bind 包不引用 network。`ReleaseExecutionContext` 没有改；尝试用自己的线程栈，并读取当前上下文核对自动模式和 `roundId`。

`effect.execute` 正常返回不再写成成功。`GroupBudget.commit` / `settle` 没改，自动轮在 `close()` 里先提交，再在在途归零且本轮该结束时 `settle`。

## 收费单位

一轮预留总额不变。双重是各 `memberShares` 加一次 `ruleShare`，规则份额不再混进成员 0 的复制池。非双重复制池就是 `pressCommitCost`。自动预留仍是 `reserve(..., false)`。

份数是该成员发射时的 `max(1, copiesBefore)`。第 i 份是池的整数商，余数分给较小序号，各份之和等于该池。松散成功只在自己的 `close()` 提交这一份，不按实体个数加价。成员 0 的规则份额只在至少一份松散成功时整笔保留一次。宿主组不拆份：`commitHostGroup` 结果为 `committed()` 时一次留下整池，成员 0 再加整笔规则份额；否则整组不留。

当前资格失败、没有效果、持剑门拒绝、令牌失效、尝试打不开，都不记成功。

## 四条路径

取消请求、在途次数和真正结清是分开的。`done()` 只在 `budget.settle` 完成之后为 true。没有在途时 `cancel()` 当场退款；有在途时只禁止新的发布和后续成员，不提前退款。

- 发布或控制没成功：不报告，`close()` 不提交该份。空效果在 `effect.execute` 之前返回。
- `reportAccepted()` 已经返回 true 之后抛异常：该份先 `commit`，异常继续向外抛。没报告的份不收费。
- 复制循环中途停下：已成功的份保留，后面没打开的份不收费。
- 重入取消或第二成员：`allowsPublication()` 立刻变 false，新尝试打不开，已调度的第二成员不再生成。已记下的成功份先保留，然后才退剩余未提交份额。

成员正常失败但没有抛异常时，另一成员仍会发射。异常离开本轮前会先保住已成功份额，再结清尚未提交的份额，所以还没开始的后续成员不再生成。

## 备份、未完成与编译

改前全文在 `backup-A-repair-1` 下的 `AutomaticRound.java` 和 `SkillSegmentRunner.java`，读回确认后才改。`GroupBudget`、`ReleaseExecutionContext`、`ReleaseBatch` 没改。

没有改 B、C 的源码。B 还没把真实 `addFreshEntity` 和剑控制提交接到这三个方法上。垂丝横排和自动无剑提示不在本单。

命名门已跑，退出码 1。这次改过的文件没有违规。旧违规两处在 `OriginalHumanoid.java`，没有顺手改。

没有跑 Gradle、`compileJava`、测试、打包或游戏。待统一编译。
