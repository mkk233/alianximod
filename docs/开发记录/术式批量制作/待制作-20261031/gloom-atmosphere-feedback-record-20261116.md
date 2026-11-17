# 幽冥天空返修反馈记录（2026-11-16，gloom-atmosphere-record-03）

本回只追加批次记录。不写制作表、资源索引或源码，不读正在制作的美术目录，不编译，不启动游戏。设计 `2/2`、实现 `3/3` 已用完。本阶段额度已用完，返修结果未追加复审。不追加正式复审，不重置计数。

## 身份

- batchId `待制作-20261031`，taskId `fire_nether_gloom_v2_progress`，attemptId `20261116-gloom-atmosphere-record-03`。
- 记录者 `01a1e08b-09e0-7bb3-b2ce-d5e430f241ac`。直接父级为当前主会话 `01a1ce06-b681-7c90-b11d-260d3c169911`。
- 模型 `cursor/grok-4.7@500k`，high。Fast 沿用客户端 `service_tier="priority"`；工具无 tier 参数，不声称后端已验证。
- 无下属。未调用美术 MCP。

## 写入

- 批次记录 `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/记录.md` 只追加第 760–774 行。
- 追加前 SHA256 `786EB73E11BFE207D1B080C38FF9DB12071775794E9BCB635139A842730F8505`，165013 字节，759 行，LF。
- 追加后 SHA256 `2C369D05C2E3C8D13FD27900BF3AE66559B26CFAEB15DA3E50DA304E819C86C7`，169183 字节，mtime `2026-11-16 18:38:00.972`，774 行，LF，CR 仍为 0。前 165013 字节哈希与追加前一致。
- 本回执本文件。未写 `docs/术式制作表.xlsx`、`docs/资源库/README.md`、`src` 和美术过程目录。

## 用户原话与授权

用户原话：“幽冥现在表现力不足，地面烧起低矮火焰的思路没什么问题但是现在确实九幽地狱的感觉，尝试一下让领域内天空色调变得像是灵魂沙峡谷，并让火焰看上去更危险”。

这是返修授权。前一版 `gloom-v2-domain-current-49-20261116` 未获得同版采用确认。已有交付和写表记录保留，不改写成已确认。

入域渐入、出域渐退只是主会话提出的接入建议，不是用户逐字给出的数值。新天空色调和更危险的火焰目前只是待制作、待接入方案。

## 已接收事实

- 美术已续派但未交付：Avicenna `01a1e3a5-bf00-7532-978b-a0d7284fcc0f`，attempt `20261116-art-gloom-rev-01`。独占 `docs/资源库/过程归档/幽冥_魂谷天空危险魂火_v2修订_20261116/` 与 `gloom-v2-atmosphere-revision-preview-20261116.md`。Carbon 与 Argon 实际返回 Unknown model 后沿用 Astra max；不重试未知模型，不记成额度问题。本回未读其正在写的目录或回执。
- 父方已实查 Aseprite 1.3.17（batch ready，`live=false`）和 Blockbench 桌面 5.2.2 / plugin 1.10.1。旧工程 UUID `0ff57af5-a0c2-9110-a30c-aa29bf43c939`。
- 只读 Carver `01a1e3e8-d4a5-7d02-b766-e54eb4fa09a2`，task `fire_nether_gloom_atmosphere_facts`，attempt `20261116-read-gloom-atmosphere-01`，观察 `2026-11-16 18:31–18:33 +08`，已 final 并关闭，零写入，无下属。
- `NetherDomainSight.probe` 第 64–76 行公开；`Sight.covers` 第 143–157 行按逐片 `[min,max)` 点包含。`NetherDomainTint.gloom` 第 42–44 行是公开布尔入口。外包盒不能用于切片判定。live 为同维度且 `gameTime<visualEnd`，removal 立即删除。客户端集合只含幽冥，没有独立域类型或主人存活字段。
- `src/main/java` 与 `src/main/resources` 搜索 `ViewportEvent`、`ComputeFogColor`、`RenderFogEvent`、`FogRenderer`、`DimensionSpecialEffects`、`RenderSkyEvent`、`fogColor`、`skyColor` 均无命中。
- 现行魂火只在 `AFTER_ENTITIES` 绘制：挂点 `minY+0.15`，双面十字、随机 yaw，宽 0.69–1.06、高 0.50–1.06，宽脉冲 0.8–1、高脉冲 0.45–1，alpha 210、全亮，16 帧每 2 tick，脉动 3.84 秒，到期直接停画、无淡出。视觉参数硬编码。
- 源 SHA256：`NetherDomainWisps` `5435DD96C5E9126726AC7E23B2D42C110BB53EBC3D4E1723648F9FC26CCE4791`；`NetherDomainSight` `BBED914E521B6ADC28B98FDD86C6860476C6A41FF4DEDB4DE6E114B423F0C06E`；`NetherDomainSync` `C4CB0878D08A1E60204EA459BF4BD54EBD0B2C18ADC311E65E898B19AE47AA55`；`DomainDefinition` `93379888C0E2C3CF21E866001BD4B78509DBF954B20C9122F0380BA32F3911D6`。

## 制作表只读

- 无 Excel 锁。`FileShare.None` 只读打开成功。
- 189447 字节，SHA256 `698EA06F90F4EDDAB7A395A6F0D21EB19699E9406B8CC6AFD398BA2ADDB9601A`，mtime `2026-11-16 18:04:04.458`。
- 与上一轮提交后基线一致。本轮没有写表，没有重开四页，也没有改 DV。

## 分列状态

- 功能：规则未改。产种周期现行 5 与旧批准 20 的差异仍未决，不借这次视觉返修修改。
- 美术：魂谷天空和危险魂火返修进行中，不是完成。
- 用户确认：新版尚未确认；前一版也未获得同版采用确认。
- 正式接入：未做。新天空和雾不是现行接入。
- 表状态：仍留制作中。未迁待验收或已完成。
- 编译与游戏：均未运行。不能称为复审通过或验收通过。

## 收束

写入已收束，无后台、无下属。本回不等待美术。下一步由父方接收美术 final 后，再另续派最终 DV 和索引候选进度写回。
