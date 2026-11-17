# 幽冥固定根部摇曳候选进度写表回执（2026-11-16，record-gloom-sway-candidate-04）

本回只登记已收束的摇曳候选，并追加制作中备注。设计 `2/2`、实现 `3/3` 已用完。本阶段额度已用完，返修结果未追加复审。不追加正式复审，不重置计数。未派下属。

## 身份

- batchId `待制作-20261031`，taskId `fire_nether_gloom_v2_domain_preview`，attemptId `20261116-record-gloom-sway-candidate-04`。
- 记录/写表者 `01a1e08b-09e0-7bb3-b2ce-d5e430f241ac`。直接父级为当前主会话 `01a1ce06-b681-7c90-b11d-260d3c169911`。
- 模型 `cursor/grok-4.7@500k`，high。Fast 沿用客户端 `service_tier="priority"`，工具无 tier 参数，不声称后端已验证。
- 本轮唯一写表窗口。未改美术目录、源码、Java 或正式资源，未编译、未启动游戏。

## 已接收

- Avicenna `01a1e3a5-bf00-7532-978b-a0d7284fcc0f`，attempt `20261116-art-gloom-sway-04`，已 final 并关闭。文件写入收束，无下属、无活动写请求。
- 父方已读交接和 `gloom-v2-sway-batch-preview-20261116.md`，并查看全景 GIF、近景 GIF 和侧面 PNG。这只是接收，不是新增正式复审，也不是用户确认。
- 请求模型 `gpt-6-astra` max。`antigravity/gemini-4-carbon` 与 `antigravity/gemini-4-argon` 均实际返回 Unknown model，无代理、无产物，随后按流程第 11.1 节降级。
- sway-03 的实时 Aseprite Lua 超时并断联，没有美术产物。旧目录 `幽冥_魂火摇曳_v2修订2_20261116` 保留为阻塞记录，晚到执行未知。父方已用官方 MCP 只读验证 `backend=batch` 成功；Aseprite 允许 batch，但不能冒充 GUI 绘画。sway-04 使用独立 `_batch_` 目录隔离旧的晚到写入。
- sway-04 是 Aseprite 1.3.17 batch 制作并复开，`ui=false`。Blockbench 桌面 5.2.2 / plugin 1.10.1 在既有真实 GUI 保存、复开并取主视口帧。没有更换账户或服务，没有 computer-use，也没有用 headless 代替 Blockbench。

## 候选事实

- 版本 `gloom-v2-fixed-root-local-sway-batch-20261116`，状态 `candidate_awaiting_user_same_version_confirmation`。
- 目录 `D:/alianximod/docs/资源库/过程归档/幽冥_魂火摇曳_v2修订2_batch_20261116/`。
- Aseprite 64×64、16 帧、5 层、100ms。底部 20 行根部固定，上部局部火舌错相摇曳。
- 真实 Blockbench 删除 49 条整簇 scale 曲线。49 组各帧变换不变。49 簇、范围、低焰、朝向和天空代理保持。
- 全景 `inside-sway-3p2s.gif`，SHA256 `B92B910CE7DCE0116EA75F670F44446812126273EC05E37DFF7E111769E22308`。近景 `close-sway-3p2s.gif`，SHA256 `A8C514EAE8E8E862FC154C472CE2E2246FF518695C75100F72C5F06488631CB8`。各 3.2 秒。
- 模型 SHA256 `350EF6D81BC057E9C577B09F04F42225009DF19EA2B59CE0C4AD4B52DD7309C9`。manifest SHA256 `4841D6409142A91D625B934C241AA4C1202023EFE6EA24E0DAFB419B1601168B`。本次磁盘复核与回执一致。
- 用户原话：“火焰特效一直在变化大小的这个效果有点蠢幽冥的火焰应该摇曳而不是变大”。上一条 DV 已逐字保留，本轮不改写。
- 运行 Java 的旧整簇脉冲未改。天空和雾仍是预览代理。未接入、未编译、未游戏实测。

## 制作表

- 提交前基线 SHA256 `3090920F15F1E21A63C01F3E83BB835D11309F00B8491090AEA769C1AAD9497F`，190508 字节，mtime `2026-11-16 20:20:58.009`，无 Excel 锁。提交前重读仍一致。
- 独占写入窗口内提交。提交后 SHA256 `D3C102BC1C6A3E668256DA1A7966DB18F2D341F8296B56DD78803842B3E223B2`，190869 字节，mtime `2026-11-16 20:48:39.313`。
- 备份 `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/备份/gloom-v2-sway-candidate-术式制作表-20261116-204832-1.xlsx`。
- 候选 `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/候选/gloom-v2-sway-candidate-术式制作表-20261116-204832-1.xlsx`，与提交后源表逐字节相同。
- 只改制作中第 10 行 `fire_nether_gloom` 的 DV。提交前 3034 字完整保留，提交后 3557 字。N、O、EK 逐字未变，其他列、术式、验证、样式和资源明细未改。
- 未迁页。原因是用户尚未同版确认，新候选不是正式资源，运行脉冲和天空/雾代码未接入，也未编译。
- 重开四页：已完成 37、待制作 0、制作中 21、待验收 20，合计 78，B 唯一。27 个 ZIP 条目保留。四张状态页各自仍是 20 条验证、8 个合并区。

## 其他写入

- 批次记录 `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/记录.md` 只追加第 799–812 行。追加前 `D74FAC39CD1D8C245EEAE0524E0C1D2BAE377E789D0E8E322E248151DD2BCA4D`，174830 字节。追加后 `DCF5E3922E281FD24319C7891D91FD8EB1D63A9E7D553E8E6EA21F629EB355D3`，178435 字节，mtime `2026-11-16 20:49:10.426`，812 行，LF。前缀哈希未变。
- 资源索引 `D:/alianximod/docs/资源库/README.md` 只在文末追加本版候选，并写明未采用。追加前 `DB17C1AB028698E0A92208378F4FF5F9F8647BA5D8404F1FFDAB30E980D7A719`，50800 字节。追加后 `DAEDEF228E93D12EBC130AEADCA48F5354FBCAAC8C141FEC64CBA9E508FE28F5`，52605 字节，mtime `2026-11-16 20:49:10.417`，246 行，CRLF。前缀哈希未变。旧候选保留。

## 分列状态

- 功能：未改。产种周期现行 5 与旧批准 20 的差异仍未决。
- 美术：固定根部局部火舌摇曳候选已完成。
- 用户确认：未取得。
- 正式接入：未做。运行脉冲和天空/雾代码未改。
- 表状态：`fire_nether_gloom` 留在制作中第 10 行。
- 编译与游戏：均未运行。不能称为复审通过或验收通过。

## 收束

写入已收束，无后台、无下属。下一步等用户同版确认；确认前不归档、不接入、不迁页。
