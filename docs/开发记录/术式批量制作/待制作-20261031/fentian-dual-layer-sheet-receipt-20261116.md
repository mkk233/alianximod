# 焚天双层表提交回执（2026-11-16）

batchId=`待制作-20261031`；taskId=`fire_kindle_v2_progress`；attemptId=`20261116-sheet-fentian-dual-layer-09`。
写表者线程 `01a1e08b-09e0-7bb3-b2ce-d5e430f241ac`。直接父级主会话 `01a1ce06-b681-7c90-b11d-260d3c169911`。
模型 `cursor/grok-4.7@500k`，high。Fast 沿用客户端 `service_tier="priority"`，工具无 tier 参数，不声称后端已验证。
设计 2/2、实现 3/3。本阶段额度已用完，返修结果未追加复审。未派下属。未改 Java、美术、资源库索引或实测清单。

## 提交

- 提交前源表 185085 字节，SHA256 `53365FCEEB3D54441438593BA9F4DA29B1DDCE1BAF54768F699983B7B3247453`，mtime `2026-11-16 11:30:39.447`。无 `~$` 锁。
- 原样提交候选 `docs/开发记录/术式批量制作/待制作-20261031/候选/fentian-dual-layer-术式制作表-20261116-115405-1.xlsx`，186519 字节，SHA256 `2D839D407D792B9C0798723E8E7887819FC05808F7FB4312000F8257F9C5D70C`。没有重建工作簿，也没有改单元格。
- 提交后源表同为 186519 字节、SHA256 `2D839D407D792B9C0798723E8E7887819FC05808F7FB4312000F8257F9C5D70C`。复制先保留了候选时间；随后只把 LastWriteTime 改为 `2026-11-16 12:02:36.350`，字节和哈希未变。
- 重开：27 个 ZIP 条目。B 计数已完成 37、待制作 0、制作中 23、待验收 18，合计 78，四页 B 唯一。制作中仍有原空行 14，所以 XML 数据行是 24，不是新增术式。制作中范围 `A1:EN29`，待验收范围 `A1:EN25`。两张状态表验证仍各 20 条。
- `fire_kindle` 在制作中第 29 行。N 483 字，恢复为「火种持续伤害附魔……」详细需求；O 29 字，为「目标身上的持续火焰，按段亮一下，燃料耗尽或期限到了就熄灭。」待验收第 26 行已无该编号。
- `fire_fentian` 仍在制作中第 28 行。N 594 字、O 425 字、EK 原文未改。DV 增至 5194 字，只在原前缀后追加。不迁待验收，不迁已完成。
- 候选单元格仍写引燃「未编译」、焚天「待统一编译」。这是已封存候选的原文；本次按指定哈希提交，没有为后到的编译回执改格。

## 另登已收束编译

已读完成回执 `fentian-dual-layer-compile-20261116.md`，4403 字节，SHA256 `D77F920B63CC75DD7063D5DBE8D2D16E2B557F7BDF8EE16F5C7FFE61888D968D`，mtime `2026-11-16 11:58:46.060`。未读进行中的 attempt03，未重算 `src/main`，未改该回执。

- Sartre attempt `20261116-compile-fentian-dual-layer-02`。命令一次 `compileJava --offline --no-daemon`，`2026-11-16 11:53:34.142`–`11:54:01.180`，退出码 0，`BUILD SUCCESSFUL in 26s`。`compileJava` 实际执行，不是 `UP-TO-DATE`。
- 54 条既有 javac `[removal]` 警告，另有两条过时 API 注记，无错误。`FireVisualLayout.java` 无警告。指向 `FireVisualClient.java:879` 的一条是原有 `sheet` 助手，不是本次新调用。
- 两触点编译前后未变：`FireVisualClient.java` 52998 字节、`EEE7620485A157F67FAB14C3B55E84C7BEF60067168AA2739D38111633E33A2E`；`FireVisualLayout.java` 10934 字节、`9C57329F4057CE564DC3D2B8E1856559B4F4F33CAF9FAEFAE46945E892678284`。对应 class 写在本次窗口内。
- 限度：回执更正句称真实编译前聚合是 `AE3C3A58...`，但同文编译前子弹仍印着与编译后相同的 `AE3C3A58BB1680064E8E5665FD0A2A35F4804126549E6747B9A12F86562A2009`。按父方与回执结论登记为前后聚合不同、没有逐文件清单，不能声称整棵 `src/main` 字节一致。本记录不裁决哪个聚合值才是原值。
- attempt03 正在补串行编译和前后逐文件清单。结果未到，不宣称。

## 未完成

美术仍是 `prepared_waiting_gui`。Planck 已关闭，等蓝火释放同一 MCP 后续做真实预览。不写成美术完成。玩法公式差异未改、未对账。没有游戏实测，也没有新增复审。父方快照只转记：`01a1c131` 与 `01a1cd95` idle，`01a1c9b3` 在做空间预览、未运行接入、未声明写表；本写表者未读这些会话。

写入已收束。源表已替换为上述候选。下一步交回主会话，不等美术，不等 attempt03。


## 提交后补登（不改表）

Sartre attempt `20261116-compile-fentian-dual-layer-03` 在源表替换之后收束。回执 `fentian-dual-layer-compile-stable-20261116.md`，4306 字节，SHA256 `AD876550203BAAB40E91E3DB526211EE260BD87102720DA54FDC80A17CCC81F4`。本记录者重算前后 JSON：384471 / 384470 字节，哈希 `F91BE5C63688E0A0ABDCF4DC5944B3510C5157B744BFFF1412296351C1B9549D` / `74A8948F6DBD0FEBF9192616D4695FB25A39A094AA42A6B636EB47E2E610B283`，各 1488 个文件，树摘要都是 `AE3C3A58BB1680064E8E5665FD0A2A35F4804126549E6747B9A12F86562A2009`。

命令一次 `compileJava --offline --no-daemon`，`2026-11-16 12:01:11.573`–`12:01:23.910`，退出码 0，`BUILD SUCCESSFUL in 11s`。`:compileJava` 为 `UP-TO-DATE`。回执称新增、删除、内容、长度和仅 mtime 变化都是 0。两触点哈希仍与实现回执相同，现有 class 仍覆盖它们。这次不回溯定位上次 `AE3C3A58...`。

源表当时已经按候选哈希提交，DV 仍是提交前的「未编译 / 待统一编译」原文。按父方指示不再覆盖源表，也不改 DV。美术仍未完成。
