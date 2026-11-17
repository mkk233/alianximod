# 幽冥魂谷摇曳采用与正式归档回执（2026-11-16，record-gloom-sway-adoption-01）

本回登记用户正式采用、完成正式归档，并记录接入进行中。设计 `2/2`、实现 `3/3` 已用完。本阶段额度已用完，返修结果未追加复审。不追加正式复审，不重置计数。未派下属。

## 身份

- batchId `待制作-20261031`，taskId `fire_nether_gloom_v2_domain_preview`，attemptId `20261116-record-gloom-sway-adoption-01`。
- 记录/写表者 `01a1e08b-09e0-7bb3-b2ce-d5e430f241ac`。直接父级为当前主会话 `01a1ce06-b681-7c90-b11d-260d3c169911`。
- 模型 `cursor/grok-4.7@500k`，high。Fast 沿用客户端 `service_tier="priority"`，工具无 tier 参数，不声称后端已验证。
- 本轮唯一写表窗口。未改源码、Java、旧过程目录或实现者正在写的范围，未编译、未启动游戏。

## 采用

用户原话：“按这版接入”。

确认对象是 `D:/alianximod/docs/资源库/过程归档/幽冥_魂火摇曳_v2修订2_batch_20261116/` 的 `close-sway-3p2s.gif` 与 `inside-sway-3p2s.gif`，并绑定同版 Aseprite、原尺寸 PNG、bbmodel 和交接。这是正式采用和接入授权，不再标为未确认。

接入范围包括固定根部的局部火舌摇曳，以及此前已展示的领域内魂谷天空和远雾氛围。游戏效果尚未实测。

## 作者与所有权

- 美术作者 `01a1e3a5-bf00-7532-978b-a0d7284fcc0f` 已关闭，写入已收束。
- 实现作者 Archimedes `01a1e46a-caa0-7d00-ba8c-1b04b467005a`，模型 `cursor/grok-4.7@500k` high。独占幽冥客户端视觉、必要天空雾事件和注册、运行 `nether_gloom_v2` 资源，以及 `gloom-v2-sway-integration-20261116.md`。本回未读其实时代码或中间产物。

## 正式目录

目录 `D:/alianximod/docs/资源库/特效/火焰/幽冥_魂谷摇曳_v2/` 原先不存在。已按字节复制 12 份确认文件，没有重画：

- Aseprite `9D7D6867771F216A3CA7552844FB66D15D764DF9A3FAE4737114FF39C5000032`，16091 字节。
- 原尺寸 PNG `7699297D207AC3CCACEC25E9CF7226C5B3765C0E16F620F3E1D717C1E23AD0FB`，5310 字节。
- 模型 `350EF6D81BC057E9C577B09F04F42225009DF19EA2B59CE0C4AD4B52DD7309C9`，319122 字节。
- 近景 GIF `A8C514EAE8E8E862FC154C472CE2E2246FF518695C75100F72C5F06488631CB8`。
- 全景 GIF `B92B910CE7DCE0116EA75F670F44446812126273EC05E37DFF7E111769E22308`。
- 交接 `1BA364C752FDD27504A74C7D980708DD76AC871663742F2BC2B7BF8DA035B01E`。
- 过程 manifest 的逐字节副本 `来源-manifest.json`，SHA256 `4841D6409142A91D625B934C241AA4C1202023EFE6EA24E0DAFB419B1601168B`。

新 `manifest.json` 只是正式路径索引，不替代、也不改写过程 manifest 哈希。模型内相对贴图路径仍指向原过程目录的上一层；原过程目录未移动，从原目录打开时回链仍有效。交接里指向 `evidence/` 和 `frames/` 的相对链接也以原过程目录为准。

计划运行映射是把上述 PNG 复制到 `assets/xueximod/textures/effect/nether_gloom_v2/nether_gloom.png`。实现完成前只是计划，不称已替换。

## 功能事实

功能交付证据不因本次视觉采用而齐全：

- 天空和雾代码未完成。
- 运行时整簇宽高脉冲尚未取消。
- 编译未完成。
- 游戏实测未完成。

产种周期仍未决。最近批准记录是每 20 tick 基础 5：

- `approved-v1.md` 第 17、71 行。
- `gloom-standalone-repair-report.md` 第 87 行。
- `gloom-standalone-repair-final.md` 第 89 行。

先前只读事实记录现行代码和两份存档配置为 5。批次 Markdown 没有更晚把 20 改成 5 的批准。本轮不审代码、不改玩法，也不把视觉采用判成功能通过。

## 制作表

- 提交前基线 SHA256 `D3C102BC1C6A3E668256DA1A7966DB18F2D341F8296B56DD78803842B3E223B2`，190869 字节，mtime `2026-11-16 20:48:39.313`，无 Excel 锁。提交前重读仍一致。
- 独占写入窗口内提交。提交后 SHA256 `A52D332FE5FB916B1BD510D61D7CDD69AE7C3094B953537028C83752B92D51DD`，191214 字节，mtime `2026-11-16 20:59:06.182`。
- 备份 `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/备份/gloom-v2-sway-adoption-术式制作表-20261116-205857-1.xlsx`。
- 候选 `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/候选/gloom-v2-sway-adoption-术式制作表-20261116-205857-1.xlsx`，与提交后源表逐字节相同。
- 只改制作中第 10 行 `fire_nether_gloom` 的 DV。提交前 3557 字完整保留，提交后 4082 字。N 236 字、O 47 字、EK 74 字逐字未变。其他列、术式、验证、样式和资源明细未改。
- 未迁页。原因是接入、编译和游戏实测未完成，周期差异也未解决。
- 重开四页：已完成 37、待制作 0、制作中 21、待验收 20，合计 78，B 唯一。27 个 ZIP 条目保留。四张状态页各自仍是 20 条验证、8 个合并区。

完整 N/O/DV 快照：`D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/gloom-v2-sway-adoption-requirements-20261116.json`。

## 其他写入

- 批次记录 `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/记录.md` 只追加第 813–826 行。追加前 `DCF5E3922E281FD24319C7891D91FD8EB1D63A9E7D553E8E6EA21F629EB355D3`，178435 字节。追加后 `158DD7A3CDA2026018CEFD9C2801B8A8FB7DDBAD39EFFE0C9AC4D2C0CB3D48BB`，182406 字节，mtime `2026-11-16 21:00:06.520`，826 行，LF。前缀哈希未变。
- 资源索引 `D:/alianximod/docs/资源库/README.md` 只在文末追加采用状态。追加前 `DAEDEF228E93D12EBC130AEADCA48F5354FBCAAC8C141FEC64CBA9E508FE28F5`，52605 字节。追加后 `02190946FDDFD34A4F708645E2B0006A8E4147E1EEE0AE3FBA93E7518711DAE6`，53964 字节，mtime `2026-11-16 21:00:06.447`，255 行，CRLF。前缀哈希未变。旧条目保留。

## 分列状态

- 功能：规则未改。周期 5 与批准 20 的差异仍未决。
- 美术：确认版已正式归档。
- 用户确认：已采用，不再等待确认。
- 正式接入：进行中，不是完成。
- 表状态：`fire_nether_gloom` 留在制作中第 10 行。
- 编译与游戏：均未运行。不能称为复审通过或验收通过。

## 收束

写入已收束，无后台、无下属。本回不等待实现。下一步等实现和编译完成后，再由另一次续派登记终态并评估能否迁待验收。
