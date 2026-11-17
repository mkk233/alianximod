# 引燃原版动画输入记录

- 批次：`待制作-20261031`
- 任务：`fire_kindle_v2_progress`
- 尝试：`20261116-doc-kindle-vanilla-input-13`
- 日期：`2026-11-16`
- 记录者：`01a1e08b-09e0-7bb3-b2ce-d5e430f241ac`
- 直接父级：主会话 `01a1ce06-b681-7c90-b11d-260d3c169911`
- 模型：`cursor/grok-4.7@500k`，high；客户端 priority 已配置，工具无 tier 参数，不声称后端已验证
- 额度：设计 `2/2`、实现 `3/3`。本阶段额度已用完，返修结果未追加复审。

## 用户原话

引燃的效果就采用原本燃烧动画就好伤害依然用引燃本身计算特效不再单独制作

这覆盖此前等待引燃后述新设计的决定。主会话的执行理解是复用原版 Minecraft 实体着火动画，并把显示与伤害分开，不再制作引燃专属美术。该理解不是用户原话。

焚天 v4.1 的观感不因本轮记成新确认。焚天玩法公式差异也不在本次范围。

## 只读表

- 源表未由本轮写入。最初无锁，187607 字节，SHA256 `C8B27C3D1AA6AEAB1EA0607D4D6CEE7817435323AFB418F17422E9AE8EDD4D2E`，mtime `2026-11-16 13:05:13.701`。
- 文档追加后再次读取，字节和 mtime 未变，SHA256 变为 `159879C3EE8650462379A7074D2B6A9C6CE8DA6FEC8CDDE604C706A2B2D87B7B`。它与 `候选/fentian-burnout-final-术式制作表-20261116-130513-1.xlsx` 逐字节相同。相对备份只增加制作中第 28 行 `fire_fentian` 的 DV，前缀保留，6188 字增至 6898 字。本轮未覆盖。
- 四页为已完成 37、待制作 0、制作中 23、待验收 18，合计 78，编号唯一。
- `fire_kindle` 在制作中第 29 行。`fire_fentian` 在制作中第 28 行，未改。
- 现行 N 483 字，O 29 字，DV 3284 字，EK 77 字。
- 单元格 SHA256：N `75F6ED304CD49A509224F4E9D117DBDE581D415A30ED54D0FAF0ED8E656DB8C1`，O `933CC168D8E153AA87437E5CFDBD52C6030A05FB4392CD85379E66285F6F9F08`，DV `2DC82E674099E7185C45809FC41CF784A4B263AEC0DC780405446CDE6E3E767D`，EK `67BEA7E220606E3F5BB5871FB5C756C7822B63008F510D67AF720012E68C0769`。
- 完整单元格在 `kindle-vanilla-input-snapshot-20261116.json`。快照同时保留最初读取哈希和后来观察到的真实哈希。
- 待制作原始数据第 19 行的 N/O 与现行单元格逐字相同，DV 不同。档案不是本次依据。
- 资源状态明细没有 `fire_kindle`。资源与状态组仍是火种消费，不是贴图。
- 表内 DV 仍停在等待后述设计，新原话尚未写入单元格。

## 已接收事实

Ampere `01a1e2fe-8c12-7b82-bdcd-2f43c5c13a1f`，attempt `20261116-read-kindle-vanilla-01`，已 final 并关闭，只读无写。他没有取得最新代码哈希，因此不能把事实包当成哈希已验证。本记录者也没有重读正在修改的源码。

- 当前显示是 `FireVisualClient` 的 BURNS 加专用 `kindle_v4` 持续/余烬 billboard。
- 服务端伤害由 KindlePlan、FireConsumption、BurnLedger、BurnCarrier、FireDamage 结算。
- 当前未点燃原版着火状态。
- 原版 `EntityRenderDispatcher` 的 `displayFireAnimation` 到 `renderFlame` 可独立显示。
- 既有 `EntityRenderDispatcherSoulFireMixin` 可复用。

## 所有权与状态

- 本记录者只写批次记录、完整快照和本回执。
- Sartre `01a1e246-a7e1-7861-acdf-2569d7b9ecee`，attempt `20261116-impl-kindle-vanilla-01`，仍在写 `FireVisualClient.java`、`mixin/client/EntityRenderDispatcherSoulFireMixin.java` 和 `kindle-vanilla-visual-impl-20261116.md`。进行中内容未读。
- 没有其他本批源码作者，也没有 Gradle 授权。
- 引燃的新显示决定已记录，但尚未接入、尚未编译，不能写成完成。
- 不再单独制作引燃专属美术；原版动画本身尚未接到运行显示。
- 索引和实测清单本轮未改。
- 两项都留在制作中，未游戏实测，不称正式复审通过。

## 缺项

- 实现 final 与写入收束。
- 最终编译。
- 表内 DV 追加。
- 资源索引和实测清单同步。
- 游戏实测。

写入已收束。无下属。下一步交回主会话。
