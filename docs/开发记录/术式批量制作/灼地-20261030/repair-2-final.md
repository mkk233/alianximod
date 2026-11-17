我先读返修 2 的授权范围，再按其中的裁决改代码。结束包目前不带原到期时间，迟到分片还能把画面装回来。我只补版本顺序和结束标记，旧的燎原路径不动。灼地返修 2 已写完并停止写入。设计仍是 2/2，实现复审仍是 2/3。这次没有新开复审。

## 版本分支

比较只发生在同一维度、同一宿主编号上。`FireVisualClient` 第 144–146 行只有画面维度和来包维度一致时，才把已显示版本传进组装。

`SurfaceSnapshotAssembly.accept` 第 47–70 行：

- 低于已显示版本，或低于同维度正在组装的版本：丢掉来包，不清除、不替换现有组装。
- 等于已显示版本：忽略，不新建组装。
- 等于正在组装的版本：继续收片。重复下标直接忽略，不改已收片数、创建时刻和 80 刻等待截止。分片总数或出生、到期、原点、样式不一致时拒绝来包，保留现有组装。
- 高于同维度正在组装的版本：换掉未完成组装。只有分片收齐才把结果交给画面。等待期间旧画面还在。
- 来包维度和正在组装的维度不同：忽略来包，不清除那份组装。

画面侧在第 152–158 行：已有画面时，只有同维度且版本更高才换切片和版本，出生与到期不动。第一次出现的画面仍沿用原来的时钟对齐。

## 结束与维度

结束包没有新增字段。`FireVisualPacket.surfaceEnd` 第 110–113 行把维度、出生和到期写进协议里原有的 `dimensionId`、`birthTick`、`expireTick`。`SurfaceFireHosts` 第 427 行发送宿主自己的维度、`birthGameTime` 和 `expireGameTime`。固定头布局不变，`GROUND` 结束包仍写空维度和 0 时刻。

`acceptSurfaceEnd` 第 178–203 行删掉该宿主画面。组装器第 74–79 行删掉未完成组装，并记下结束标记直到原来的到期时刻。标记还在时，这个宿主编号的迟到分片不能再装出画面；显示表按宿主编号，所以其它维度字符串同样不能把这个槽装回来。世界时间为到期之后的下一刻，标记在 `expireEnded` 第 97–107 行去掉。渲染第 261 行每帧清过期标记。

结束包若没有可用到期时刻，先用已有画面的到期，否则用未完成组装的到期。两边都没有时，才用当前时刻加 100 刻。正常服务端路径会带上原来的出生和到期，不会走这档。

客户端世界为空、登出，以及客户端维度卸载，都走原来的 `forget`。第 92–94 行同时清掉未完成组装和结束标记。

有界校验、空几何、40 刻同版本补发都还在。`GROUND`、燃烧和炉心路径没有改。

## 修改文件

| 文件 | LastWriteTime |
| --- | --- |
| `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\SurfaceSnapshotAssembly.java` | 2026-10-30 21:40:59 |
| `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\FireVisualClient.java` | 2026-10-30 21:41:16 |
| `D:\alianximod\src\main\java\com\bslx\XueXiMod\network\FireVisualPacket.java` | 2026-10-30 21:41:16 |
| `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\firecast\SurfaceFireHosts.java` | 2026-10-30 21:41:17 |

## 命名门禁

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 1。本次文件没有命中。剩下的是未改文件里的旧违规：`OriginalHumanoid.java:165` 的 `i`，以及 `:202` 的 `to`。

没有编译，没有进游戏。统一编译仍待主会话执行 `.\gradlew.bat compileJava --offline --no-daemon`。
