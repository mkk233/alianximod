# 领域绑定：运行时事实采集

模型固定 Cursor CLI --model grok-4.7-high-fast。本阶段只读信息采集，不是正式正确性审查，不修改任何文件，不编译，不写制作表，不启动游戏，不提交。

先读 D:/alianximod/AGENTS.md、D:/alianximod/.grok/skills/max-dispatch/SKILL.md、本目录记录.md；可读前一期 D:/alianximod/docs/开发记录/领域附魔底座实施-20261029/记录.md 了解已交付边界，不重新审查旧计费实现。

代码读取范围：D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/ 下现行领域存续/实例/几何；同级 release 下 CastFrame、ReleaseBatch、支付/延迟执行的直接接口；D:/alianximod/src/main/java/com/bslx/XueXiMod/player/ManaPaymentReceipt.java；D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/effects 下与垂丝、飞弹、领域创建直接相关的实现；定义依赖按需查单个符号。可 rg --files 找确切文件，不通读无关模块。

用户新增需求：领域后放绑定规则，捕获后续规则到第一个可用攻击术式，并每 20 tick 固定自动释放一次，没有目标也放。生成点/目标点在领域内随机，不读玩家准星；具体随机几何待用户答复。需要沿用绑定子链规则与正常费用。暂定开域时冻结绑定内容。

只返回事实和候选约束，不最终裁决架构，不复审前期已耗尽额度的接触伤害计费：
1. DomainInstance/Service 创建、复制、tick、关闭/销毁、归属与几何（形状/包围盒/实际域）接口及行号；适合承载冻结子链和 20tick 触发输入的数据位置。
2. 怎样获得真实领域内部随机点，能否复用形状接口；现有方向/落点/锁定目标如何分别传给攻击。垂丝等 trajectoryMutable=false 的实际生成行为。
3. 绑定触发的当前可用魔力扣款、法杖/玩家退款接口、延迟分支取消事实；领域主人换武器/离线/跨维/关闭时的接口。哪些取消/重入能力尚不存在。
4. 多份领域同时触发、每份独立收费和稳定顺序的既有设施；领域每20tick无人也生成对原有类型的限制需要何种能力判断。
5. 返回可能最小改动的具体文件清单及与链作者共享文件点，区分代码事实、建议和待确认。

全程不写文件，最终反馈由调度器保存。建议不超过 5500 汉字。禁止 reset/clean/checkout，保留所有已存在未提交改动。
