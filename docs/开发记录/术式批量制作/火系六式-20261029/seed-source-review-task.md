# 火种来源实现集中只读复核（实现累计第3轮）

模型固定 Cursor CLI --model grok-4.7-high-fast，独立只读会话。本单必须在源码作者 seed-source-implement 最终交回并停写后才由主会话启动。你不参与实现，不写文件、不跑Gradle、不启动游戏、不改制作表。

先读最新 AGENTS/max-dispatch/统一流程。设计既有2/2，实现既有2次；本单是实现累计3/3，不能在结束后自动追加复审，也不要再调用其他模型/会话复审。旧测试记录保持历史，无独立测试阶段。本次只读正式实现核对，主会话仅转达你的结论，不独立再做一轮。

工作区 D:/alianximod。输入本目录 seed-source-implement-task.md、seed-source-implementation-report.md/json、seed-source-current-requirements.json、seed-source-read-report.md、seed-source-intake-report.md。用户直接授权既有点火 magic_ignite 额外+2、热光束 heat_beam 每次命中+1、火球 fire_orb 命中+3，可相加3/5。不把三式历史在已完成误作新增功能已验收。读取最终代码和仅本次改动/其必要调用链；不通读整仓，不重新审无关六式旧缺陷。

作者最终产物是 seed-source-implementation-report.md、seed-source-implementation.json、seed-source-implementation-evidence.json；以这些实际文件名为准。作者明确18个文件均停止写入，未发现同路径其他作者，未编译。特别覆盖仅附点火、没有其他额外伤害附魔的普通武器成功攻击应当+2，不应要求附魔自身有额外伤害才能挂种。

集中检查本次生产实现：
1. 具体附魔/宿主数值能否从生产定义沿实际调用链完整传到真实伤害成功路径，而非只改未调用效果类；点火术式和武器、潮印路径完整。
2. 1/2/3固定，额外可相加，100上限与原有火种生命周期保留。无无关伤害/点燃/冷却/印记次数/轨迹/语言信息删减。
3. 热光束现有10tick门每个实际命中仍能继续产种；批次once上下文不能把后续命中吞掉。火球沿现有每目标hitOnce/穿透生物，命中各目标加3。
4. 空挥/友伤拒绝/免疫/客户端/自身/死亡不产种；不要让武器额外火伤、原版燃烧tick或其他派生伤害重复触发。伤害被取消/零伤害时不预支挂种。
5. 三份/Tally聚合/重复callback不乘火种或重复生产；额外2与原生1/3只加一次；不破坏消费者FireCastContext/FireSeedAccount已有一次消费和生产契约。必要上下文生命周期、线程/跨目标隔离与NBT兼容。
6. 使用公共能力/载荷接线，没有在SkillUsePacket或release/fire按这三项id硬分支，也没有为简单来源加第二套伤害底座。

只读审查不得修改任何文件。最终反馈直接返回，由外层wrapper存 seed-source-review-final.md。内容：明确通过/发现问题；每个真实可定位问题的优先级、绝对文件路径/行号、触发例与必要修复；本轮实际读的文件/源码hash是否匹配作者final；未验证边界（不把只读审查称为运行/游戏测试）；不能判定则明确缺证据。不要把假设性重构偏好或缺少新测试当bug。原已完成缺历史快照只属于表迁移约束，不是源码功能缺陷。最后确认只读、未写源码并结束本轮，不自行复审修复。
