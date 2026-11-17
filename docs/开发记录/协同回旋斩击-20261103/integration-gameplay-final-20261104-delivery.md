# 协同距离声明与指定世界（2026-11-04）

代码默认和「新的世界」的两个显式键已经是 24/32。执行族仍只预约一次。没有编译，没有进游戏，也没有把这份说明当成实测通过。

## 代码

`CooperativeContent` 里 `rule_cooperative` 的现有键：

| 键 | 代码默认 | 范围 |
| --- | --- | --- |
| `selectionRadius` | 24 | 0 到 48 |
| `moveLimit` | 32 | 0 到 64 |
| `sideOffset` | 1.25 | 0.75 到 2，未改 |

缺键用新默认。没有改 schema，没有新键，也没有改费用、冷却、命中或复制规则。

`CooperativeChainCompiler` 本来就在准备执行族时用 `BalanceService.number` 读这三个键并冻结。`CooperationSettings` 已经能接受 48 和 64，所以编译器没有再改。

`FamilyExecutionService` 仍只调用 `tryReserve` / `advance`，不调用 `RemoteRecall`。分身核心的同一个 `CloneServerPorts` 已经实现远距端口，不需要再接线，也没有改它的文件。

## 指定世界

文件：`D:/alianximod/run/saves/新的世界/serverconfig/xueximod-balance.json`

改前没有 `rule_cooperative`。最后写入时间是 2026-11-02 02:08:15（UTC `2026-11-01T18:08:15.1358058Z`）。SHA-256：`E9B4B2F451B083368AAB915552A986958C42C1B2859616B549C9D0C47F915FC0`。

备份：`docs/开发记录/协同回旋斩击-20261103/world-config-backup/xueximod-balance-20261104-024625.json`。备份 SHA-256 与改前相同。写入前再次核对原文件哈希，没有被别人改过。

改后只新增 `rule_cooperative.selectionRadius = 24.0` 和 `moveLimit = 32.0`。该对象没有其他键。原有术式、全局倍率和 `schemaVersion` 重新解析后语义一致，差异数为 0。改后 SHA-256：`29D2AD0A240CF784FD7E340590739426FF7CBED4D424C22EBE5E1407C071D9EF`。

其他世界和全局模板没有改。重开这个世界或重启服务器后，这场测试冻结的是这两个显式值。本任务没有启动游戏。

## 文档

配置指南的协同段写了上述默认、范围、消费者和生效时机。水流和其他术式没有重写。游戏实测说明和整合说明把旧的 8/4，以及“新斩光尚未确认”，标成历史。当前资源仍是 v1 人物动作加已确认的 v2 薄壳。

## 给 C / V 的待办

没有必须再改的项。远距端口已在核心预约内。动作开始刻已用世界年龄。发布器缓存和追踪包本来就用世界年龄，没有新的视觉时钟待办。

## 自检与实测入口

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 0。

未统一编译，未打包，未进游戏，未写制作表，没有追加复审。

请重新启动开发客户端后进入「新的世界」。只按 F3+T 不会加载这次 Java 变更。配置本身在重开世界后生效。建议看这四项：回旋是否转完整圈，确认挥砍是否出现，第一人称斜斩薄壳是否能看见，以及超过 8 格、不超过 32 格的普通分身是否一次到位而不是穿墙滑行。这些都还没有在本任务里验收。
