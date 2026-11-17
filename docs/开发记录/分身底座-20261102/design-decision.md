# 分身底座设计裁决

- 日期：2026-11-02。
- 批次：分身底座-20261102；非制作表批次，不领取、不迁页。
- 本轮：设计 1/2；实现 0/3。执行者为本批唯一 gpt-6-astra。补充只读事实采集归本次设计轮内，不是新一轮正式复审。没有独立测试阶段。
- 输入：architecture-brief.md；read-casting-final.md；read-performance-config-final.md；read-damage-followup-final.md；read-config-followup-final.md；read-availability-final.md；read-registration-followup-final.md；任务书指定的配置/优化/调度/统一流程文档。各 Cursor 报告均完成并停止写入。
- 用户授权：从最基础制作共用分身底座，先给详细设计与实施指导，按现有配置和优化方案兼容。
- 产物：`D:/alianximod/docs/分身底座设计与实现指导.md`，v1 实施合同。

## 裁决

按 v1 合同实施，不需要再为常规可逆实现选择征求用户许可。未发现必须补问用户的设计歧义。实施前共享文件的写入窗口由主会话跨聊天协调，不能依据旧文件时间戳推断其他作者已停止。

本次必须完成：

1. 共享实体/运行态底座，两类驻留分身；幻影仅一次已提交响应，普通可多次。存在、生命、触发、响应额度、冷却和结束策略独立。
2. 权限 2 的 `/shu clone spawn/list/respond/dismiss` 实际入口。真实登记 clone_phantom、clone_body、clone_response_pulse，命令通过正式服务真实消费配置/魔力/响应，不是无业务占位。
3. 单目标 actor 原点的基础脉冲，主动与非致死受击两种触发；有限原版僵尸/骷髅诱敌。原版人形模型仅为验证外观，不制作新美术。
4. 开发专用能力过滤：Technique 默认保持 PLAYER，新项 DEVELOPMENT_ONLY。getAll 仍供完整配置封存；显示、自动学习、learn/stone/book、刻印、装配和最终施术入口通用过滤，不按三个 id 在旧链里特判。NBT 强塞整链收费前拒绝，不能给一个先收费后静默无效的术式。
5. 服务端身份/代次、原子响应消费、实际资金收据、权限、生命周期、有界队列/数量、性能计数及最终统一 compileJava。

首版不含普通玩家装配入口、任意旧链、完整水幻形、同步/配合规则、持久化读档恢复、新美术、复杂移动 AI。默认 lifetimeTicks=0 是当前加载会话内无限自然期限；离线/死亡/换维/卸载/停服结束，不强制加载区块。

## 关键依据与风险边界

- 当前 ServerPlayer 同时承担 owner 与身体；commitNow 只是接触几何覆盖。不能把它、领域自动上下文或 FakePlayer 当 actor 接口。禁止临时移动/转动主人。
- classifyRoot 会加领域并绕无敌帧，明确禁用。基础脉冲先做 DamagePermission.allows 及友方检查，再 classifyRespecting(target, owner, configured.amount(), TideMarkSuccess.NONE)。不共享 Tally，不排 ChainScheduler，不产火种/潮印/主手附魔。
- 当前 playerAttack 的直接实体和致伤实体都是 owner。首版几何来自 actor，但原版 hurt/仇恨/击退可能仍依据 owner；文档和实测必须披露，不能声称完整 actor DamageSource 已实现。
- ManaPaymentReceipt.charge/refund 是原池/原杖退款边界；MagicPool.restore 不适合作通用退款。已提交动作不因 miss、免疫或事件取消退费/退次数；提交前失败按收据退款一次。
- 配置 seal 要求真实 Technique，无非术式 service 槽。三个真实可执行开发动作承载自己的平衡字段，硬容量/去重/次数/ChainRole 不开放配置；不改旧全局倍率算法。
- 旧 ChainScheduler 跨服不隔离，本批自有 session-bound 有界队列，不修旧队列。所有清理走幂等 end，noSave，不扫全实体恢复 owner 索引。
- 现有候选查询、结构缓存、纯计算和性能记录按各自合同使用；不把实际目标/费用塞进结构缓存，不包重复查询计时，不凑 JNI 批量，不宣称 TPS/FPS 收益。
- 另一聊天仍在处理旧链落点/编号/共享锚点问题，本轮不复审、不修复、不批准这些旧路径可供分身使用。

## 文件分工

详细逐文件清单在设计文档第 14.1 节，主会话派发前按绝对路径确认归属：

- A：新 clone/core 包，实体、状态机、服务、索引、队列、生命周期、触发/站位、诱敌、来源作用域。
- B：新 clone/action 包及 CloneBalance.java，单动作与能力注册、配置声明/读取。
- C：开发能力与玩家入口过滤、三个真实 Technique 和释放定义注册、命令、实体/属性/渲染挂接、性能枚举与语言提示。所有既有共享文件只归 C，不能与其他聊天同时写。
- DTO/接口按文档先冻结，A/B 有依赖时先交接再改，不同时写同一文件。原 TechniqueHits、MagicPool、候选缓存、计划缓存、ChainScheduler、JNI、旧 phantom 包默认不改。

全部源码实现、修复、定向检查与编译显式使用 Cursor CLI `--model grok-4.7-high-fast`。本设计者只写本记录及详细设计文档，未读取源码内容（只列过相关文件名路径）、未写 Java、未编译、未启动游戏、未改表。

## 交付状态

设计已定稿，可按边界派发实施。实现尚未开始；没有编译结果或游戏验证结果。实现完成后等相关作者最终反馈并停止写入，额度内集中复核；最终编译覆盖收束源码。实现最多 3 轮，返修复审累计，不自动追加。

本轮停止写入声明：详细设计文档和本记录已交回，设计作者停止写入；后续修改需主会话重新协调文档所有权或交回本作者，不与 Cursor 并写。用户实测清单已经列入设计文档第 15 节，尚未执行。
