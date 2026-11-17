我先读返修任务说明，按里面写明的范围改。返修只补执行返回和 `trackRound` 的交接：实例已关闭、到期或主人失效时立刻取消句柄，不再挂回。我先把这两个文件的现行全文备份下来。现行全文已经读完。接下来把返修前的两份源码写进备份目录，核对落盘后再改。# C 返修报告：关闭与轮次句柄交接

作者 C。模型 grok-4.7-high-fast。实现第 1/3 轮之后的授权返修。本单不另作正式复审。设计 2/2 已用尽，没有重做设计。

`released=true`

只修裁决 R3 里 C 的句柄交接。预算、收费、周期、采样、选敌和注册没有改。

## 接点

`BoundReleases.executeAutomatic` 返回之后，`DomainBindingRuntime.releaseOne` 立刻用本拍的 `now` 调用 `DomainInstance.roundStillHosted`。条件是：

- 实例 `active`
- `ReleaseInput.hasExpired(now, expiresAt)` 为假
- 宿主世界能取到服务器
- 主人能按 UUID 找到、`isAlive`，并且 `owner.level()` 就是这份宿主世界

未完成句柄在这些条件已经失败时马上 `cancel()`。已经 `done` 的句柄不再取消。取消后的句柄仍返回给调用方，但下一步不会把它挂进已失效实例。

`fireDue` 把同一个 `now` 传给 `trackRound`。`trackRound` 再查一次 `roundStillHosted`：

- 失效：未完成则再 `cancel()`，然后返回，不放入 `openRounds`
- 仍有效且未 `done`：才放入 `openRounds`
- 空句柄或已 `done`：不放入

因此执行栈里已经关闭、到期，或主人下线、死亡、离开宿主维度时，新句柄不会留到以后的延迟回调才发现令牌失效。`cancel` 按 `RoundHandle` 现有接口调用，幂等由 A 的实现负责。在途发布的结清仍由 A 的提交保护处理，C 不改预算。

## 保留的关闭路径

`close` 仍先把 `active` 写成 false，再 `cancelOpenRounds`，并在 `finally` 里关闭接触状态。`cancelOpenRounds` 仍先复制并清空集合，逐个取消未完成句柄，收齐第一份异常后再抛出。这些方法本单没有改。

## 改动文件

| 文件 | 改动 | 返修前备份 |
|---|---|---|
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainBindingRuntime.java` | `executeAutomatic` 返回后按本拍条件立即取消 | `backup-C-repair-1/` 下同相对路径 |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainInstance.java` | `roundStillHosted` 与 `trackRound(handle, now)` | `backup-C-repair-1/` 下同相对路径 |

备份是本轮返修前的现行全文，不是首次实施前的版本。写入后已读回：`DomainInstance` 备份仍是只看 `active` 的 `trackRound(RoundHandle)`，`DomainBindingRuntime` 备份仍是 `executeAutomatic` 后直接返回。SHA-256 与 mtime 没有文件系统读数，记为 null。证据：`docs/开发记录/领域绑定规则-20261030/evidence-C-repair-1.json`。

## 未验证

- 待统一编译。本单没有跑 `compileJava`，没有测试，没有打包，没有启动游戏。
- 命名门没有跑。对 `tools/check-name-gate.ps1` 的调用被拒绝，原因是本单禁止编译；没有绕过，也没有改审批。
- 没有游戏内核对：执行中关域、到期、主人下线或跨维时，返回的句柄是否立即取消且不留在实例上。
- A 的在途发布结清、`done()` 何时变真，以及 `cancel` 是否幂等，本单没有改，也没有编译验证。
