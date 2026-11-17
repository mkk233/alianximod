固定Cursor CLI --model grok-4.7-high-fast，Ask只读，不spawn，不改源码、资源、文档、表或启动器记录，不运行Gradle，不启动游戏。

这是一轮集中实现复核，只有主会话在记录.md明确登记实现第2/3轮并确认所有源码写入收束后才Start。本批设计1/2、此前接口文档实现1/3，不因新task重置；本次返回一份完整结论，不自动循环复审或修复。只读范围为本批合同涉及的新增能力/运行时/分身/攻击/动作/客户端及其直接旧共享接点，不通读整仓，不重新审水幻形旧批已关闭问题。

先读AGENTS.md、max-dispatch、统一开发流程，以及本批implementation-contract.md、interfaces-delivery.md、clone-delivery.md、effects-delivery.md、visual-delivery.md、integration-delivery.md。实际实现是否存在以代码和调用链为证，不把作者文字说明当代码证据。用户批准的视觉是docs/资源库/动画/剑术/协同回旋斩击_v1/，不拿未对齐草稿作为标准。

重点给出真实可达路径、反例及精确文件行号：
1. 玩家可获取装配三个术式与普通clone_body，斩击单装、回旋→斩击/横劈、协同→回旋→这两攻击；未适配攻击收费前拒绝。定义、准入、能力、配置、所有工厂真实注册，正常加载不会duplicate key或抛异常，旧execute异常路径不能被新功能走到。普通分身仅法杖槽0独立部署、唯一付款，潜行撤回范围正确。
2. 新能力实际穿过手动编译、CONTACT冲刺、领域绑定/自动轮三入口，原链索引、索敌/弹道/附魔、三分/双重/错时/接续语义保留。无新能力旧路径稳定。零伤害仍结构ATTACK。领域合法能力不能丢，既有动作禁用规则按原结构保留。
3. family/member/copy/spawn/actor身份，Tally按member独立，actor不进入伤害去重，密集ordinal及全局实体倍率只一次；单copy也强制Tally，六刻尾部仍可取消/复验权限，seen不复活。来源深度1仅最终结算同栈，无跨tick ThreadLocal。
4. 全几何先暂存再发布、失败无半提交，双人降单人不重复发玩家体积；共享实体预算、目标上限、TTL/队列有界。actor眼点偏移仅一次；新斩击厚度真实碰撞；静止方向和近垂直瞄准与合同吻合，不把站位fallback擅自应用到三维刀面。
5. 动作费、攻击份额、可选协同费、冷却各有真实提交线；没分身/合作余额不够可正常单人；取消未提交份额退原收据原池，配置倍率不二套，领域不重扣手动费用。
6. C普通响应与协作共用唯一pending，预约互斥并共用旧32tick预算。候选owner索引≤4，站位/2tick swept AABB路径/危险地面/实体/区块边界，实际写位前检查，不操作玩家。撤回、死亡、世代、维度/卸载/stop清理，普通READY恢复且只一次响应冷却，无追加脉冲伤害。
7. 服务端gate唯一，回旋时间与动作包冻结一致，prepare不双叠；客户端开始/停止/跟踪、旧序号、不同session、换维、缓存清理；普通玩家/分身均能播真实轨道，不转镜头。批准PNG一致；实际emit驱动斩光，游戏几何与显示位置/roll/尺度吻合；专服不触发客户端类加载。旧水幻形顺序仅作为本批触点回归边界，不重开旧批审查。
8. 值真实读自BalanceService并冻结，无伪键/未消费计数；配置指南与接入一致。核心契约是否留下TODO/throw/空方法/未挂工具类必须明确。

交付要求：按严重度列具体缺陷及复现路径、文件绝对路径和行号、应归I/C/E/V哪一作者。没有证据不猜缺陷。标注哪些合同点确实查过/哪些无法代码静态确认，编译和游戏都尚未执行，不写已通过。报告结尾给本轮结论（通过或必须返修）与最小修复清单；如果未完成审查如实blocked，不把过程摘要当结束。

最终正文直接返回供主会话通过Result保存，不写报告文件。末尾唯一有效机器JSON：

主会话现已收到I/C/E/V全部valid/reported_done/writesSettled=true且登记实现第2/3轮。I完整Result为integration-final.final.md。其报告明确start(plan)拒绝、三入口改用open；协同费先扣再预约失败退款；规则ReleaseKind=SELF_STATUS；近垂直方向在准备前收成水平；配置采用moveLimit。请把这些实际边界按合同核实到代码和可达行为，不视作者文字为自动免责或正确，也不要仅因名字不同判错。最终报告某些changedFiles写entity/而真实符号可能位于shu/technique/release/entity/，先按符号确定实际源码路径，不假设同名文件就是作者所改。

<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"本轮只读复核的实际结论","changedFiles":[],"checks":[],"notRun":["未编译","未游戏验证"],"writesSettled":true,"blocker":null,"nextAction":"由主会话按本轮缺陷交回原作者，不自动开始返修后复审"}
<<<END_CURSOR_TASK_REPORT>>>
