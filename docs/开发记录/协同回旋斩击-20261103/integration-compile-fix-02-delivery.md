# 编译访问修正

`compileJava` 的两个错误都是 `network.CooperativeCastRouter` 不能读取 `FamilyExecutionService` 的包内字段 `FORCED_ANCHOR`。

字段改为私有。预付释放改走 `beginForcedAnchor` 和 `endForcedAnchor`。设置仍在准备和打开执行族之前，`finally` 里移除，不留到下一刻。接触点仍优先于这个锚点，落点射线的条件没有改。

没有重新跑 Gradle，也没有进游戏。45 条 removal 警告没有处理。
