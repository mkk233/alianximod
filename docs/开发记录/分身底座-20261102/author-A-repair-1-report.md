# 作者 A 第 1 轮返修

只改了真实源码 `src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/CloneService.java`。没有改候选副本，没有改其他文件。

## 修改

`settleCommitted` 在普通分身从 `COMMITTING` 收到 `COOLDOWN` 时，把演员身上的 `state.commitLine` 还原为 false。位置是第 591–598 行，新增第 594 行：

```591:598:src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/CloneService.java
        if (state.response == CloneResponseState.COMMITTING) {
            state.response = CloneResponseState.COOLDOWN;
            state.pending = null;
            state.commitLine = false;
            if (state.actor != null) {
                state.actor.mirror(state);
            }
        }
```

这次成功提交仍先把 `attempt.commitLine` 和 `state.commitLine` 设为 true（第 508–509 行）。`finally` 仍看 `attempt.commitLine`：过线之后不退款、不走 `restoreReady`。普通分身收束后演员标记已清掉，所以下一次提交前失败可以 `restoreReady`，过期可以 `cancelQueued`，结束时 `cancelPending` 也不再被上一次成功挡住。

幻影分支仍在第 582–589 行先写 `SPENT` 再 `end(BUDGET_SPENT)`，不改 `commitLine`，结束顺序不变。`settleCommitted` 开头仍是生命不是 `ACTIVE` 就返回，`finally` 不会把 `ENDING` / `ENDED` 拉回 `READY`。

## 未做

没有改 `restoreReady`、`cancelQueued`、`CloneRuntime.cancelPending`。没有编译，没有打包，没有启动游戏，没有独立测试，没有提交，没有改表，没有做新一轮正式复审。

## 命名门禁

已真实执行：`powershell -NoProfile -File tools/check-name-gate.ps1`，工作目录 `D:\alianximod`。退出码 1。输出：

```
源码命名可读性检查失败:
D:\alianximod\src\main\java\com\bslx\XueXiMod\animation\runtime\OriginalHumanoid.java:165:18 i: 变量名不得少于 3 个字符
D:\alianximod\src\main\java\com\bslx\XueXiMod\animation\runtime\OriginalHumanoid.java:202:17 to: 变量名不得少于 3 个字符
D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\clone\core\ClonePositionPolicy.java:13:33 MIN_SEPARATION: 禁止含糊缩写 "min"
共 3 处
```

这三处都不在本次唯一允许修改的 `CloneService.java` 里，没有改。不能把这次门禁说成通过。

## 停笔

作者 A 第 1 轮返修到此停止写入。待统一编译。游戏未测。
