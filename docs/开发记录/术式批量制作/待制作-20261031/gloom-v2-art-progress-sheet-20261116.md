# 幽冥 v2 领域候选进度写表回执（2026-11-16，gloom-record-02）

本回只登记已收束的美术候选和只读事实，并追加制作中备注。设计 `2/2`、实现 `3/3` 已用完。本阶段额度已用完，返修结果未追加复审。不追加正式复审，不重置计数。未派下属。

## 身份

- batchId `待制作-20261031`，taskId `fire_nether_gloom_v2_progress`，attemptId `20261116-gloom-record-02`。
- 记录/写表者 `01a1e08b-09e0-7bb3-b2ce-d5e430f241ac`。直接父级为当前主会话 `01a1ce06-b681-7c90-b11d-260d3c169911`。
- 模型 `cursor/grok-4.7@500k`，high。Fast 沿用客户端 `service_tier="priority"`，工具无 tier 参数，不声称后端已验证。
- 本轮唯一写表窗口。未改源码、配置、美术目录或美术回执，未编译、未打包、未启动游戏、未制作美术。

## 已接收

- Avicenna `01a1e3a5-bf00-7532-978b-a0d7284fcc0f`，attempt `20261116-art-gloom-03`，已 final 并关闭，文件写入收束。请求模型 `gpt-6-astra` max。`antigravity/gemini-4-carbon` 与 `antigravity/gemini-4-argon` 均实际返回 Unknown model，无代理、无产物，随后按流程第 11.1 节降级。旧异步截图无法写文件，见美术回执。
- 父方已读 `gloom-v2-domain-art-preview-20261116.md`、`visual-handoff.md`、`manifest.json`，并查看 7 张真实核心静帧和 GIF。这只是接收，不是新增正式复审，也不是用户确认。
- Galileo `01a1e39e-5360-7021-be0e-30e60ac7040b`，task `fire_nether_gloom_current_inputs`，attempt `20261116-gloom-facts-02`，观察 `2026-11-16 17:20:58 +08`，已 final 并关闭，只读无写。

## 候选事实

- 版本 `gloom-v2-domain-current-49-20261116`，状态 `candidate_awaiting_user_same_version_confirmation`。
- 目录 `D:/alianximod/docs/资源库/过程归档/幽冥_v2领域定稿预览_20261116/`。7×7×8 格，魂火底面 +0.15 格，固定 49 簇代表样本，人物 1.8 格。
- 四类 7 张真实 Blockbench 静帧；GIF 4.8 秒、96 帧真实视口。bbmodel 已原生保存并复开。
- 模型 SHA256 `DB3F4335002E3AA5D8A58EA1EECD88CCFF58504EA3C1F62B894AD549D348A2E2`。GIF SHA256 `2CE10A628E87540DB1A694DE09F9C5573D8275A575D7A05FB77254E418C39A19`。
- 现行 v2 像素未重画。100ms Aseprite 只对齐运行时 2 tick/帧，导出编码不同、像素一致。示意 alpha210 材质不能当运行资源。
- 默认领域全尺寸 7/7/8、200 tick。底部魂火按 UUID 可为 36/49/64 簇；本候选只展示固定 49 簇。旧 84 簇、约 10 格直径示意保留为历史。
- 现行运行 `textures/effect/nether_gloom_v2/nether_gloom.png` 保留。本次未正式归档、未接入、未编译、未游戏实测。因此写“本次未正式接入”，不是完全没有 v2 运行引用。

## 周期差异

只记录，不宣判配置错误，不改数值，不追加复审：

- 现行 `GLOOM_PERIOD_TICKS`、`EnchantmentDefinitions` 默认、`NetherContract.periodTicks` 经 `BalanceService`、`NetherDomainCycle` 均为 5。
- `run/defaultconfigs/xueximod-balance.json` 与 `run/saves/新的世界/serverconfig/xueximod-balance.json` 的对应周期均为 5。
- `approved-v1.md` 第 17、71 行，`gloom-standalone-repair-report.md` 第 87 行，`final.md` 第 89 行批准仍是 20。
- 批次 Markdown 未找到晚于该批准、把周期改为 5 的记录。

## 制作表

- 提交前基线 SHA256 `3A744629AC2BAB1CB6740F7BAD2BAA4A716BDE4B708ABBF008D5C65B212E4BCA`，188140 字节，mtime `2026-11-16 16:55:24.779`，无 Excel 锁。提交前重读仍一致。
- 独占写入窗口内提交。提交后 SHA256 `698EA06F90F4EDDAB7A395A6F0D21EB19699E9406B8CC6AFD398BA2ADDB9601A`，189447 字节，mtime `2026-11-16 18:04:04.458`。
- 备份 `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/备份/gloom-v2-art-术式制作表-20261116-180354-1.xlsx`。
- 候选 `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/候选/gloom-v2-art-术式制作表-20261116-180354-1.xlsx`，与提交后源表逐字节相同。
- 只改制作中第 10 行 `fire_nether_gloom` 的 DV。旧文 1199 字完整保留，新文 1945 字。N、O、EK 逐字未变，其他列和术式未改，未迁页。
- 重开四页：已完成 37、待制作 0、制作中 21、待验收 20，合计 78，B 唯一。27 个 ZIP 条目保留。四张状态页各自仍是 20 条验证、8 个合并区。

## 其他写入

- 批次记录 `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/记录.md` 只追加。追加前 `E6A2E06769819110262796762D4CC43E2BB0F266BD263C7B219B8ECC199D6E08`，161205 字节。
- 资源索引 `D:/alianximod/docs/资源库/README.md` 只在文末追加候选状态。追加前 `70D59EE377E408D56BC2DB6A8C432F14C58FE9F272C336B77263B3049ABF977B`，47066 字节；追加后 `AA2EABAE4D2FEA007E768742230AA11074B7D842482BF91462E208937C1545C5`，48805 字节。前缀哈希未变，CRLF 保持。旧版历史未删。

## 分列状态

- 功能：规则未改。周期 5 与批准 20 的差异仍待对账。
- 美术：动态三维候选已完成，未正式采用。
- 用户确认：未取得。
- 正式接入：本次未做。现行运行 v2 保留，不是本次确认版接入。
- 表状态：`fire_nether_gloom` 留在制作中第 10 行。未迁待验收或已完成。
- 编译与游戏：均未运行。不能称为复审通过或验收通过。

## 收束

写入已收束，无后台、无下属。下一步等用户同版确认；确认前不归档、不接入、不迁页。
