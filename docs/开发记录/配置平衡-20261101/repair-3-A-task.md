固定Cursor CLI --model grok-4.7-high-fast。原作者A落实review-3-final.md问题1、2全部明确修复。设计2/2、实现复核3/3已用尽；你只执行既定返修，禁止追加正确性复审/另派代理，不新架构裁决、不测试/编译/游戏/改表/提交。最终只交工作结果，不能声称已经额外复审通过。
完整读review-3-final.md后按最小修复落实：
1. SkillChainPlan.pressCommitCost被吃掉的每个链上修饰段自身segmentManaCost加一次，不乘后续份数，不在出手再扣；默认0仍0，领域/屏障/护环占位不变。覆盖本次按键既有计划消费到的各修饰，单独索敌也进入标准一次预扣，不仅第一段。
2. 所列普通报价求和和份数相乘用与现有批量编译一致的精确运算/拒绝语义：pressCommitCost各累加、enchantOnlyCost、SkillFollowCast、SkillUsePacket.imprintEnchantChain、dualSequenceCost，以及AutomaticRound调用边界。溢出必须在本次释放发生/付款前明确拒绝，不能回绕/Math.max归零/负数流入MagicPool，也不要仅改成抛异常却无调用边界处理。沿用现有失败反馈与退款收据，避免引入第二次计费。相关路径必要的调用端处理属于本次明确实现范围，不扩检查无关代码。
独占范围为你原A路径，加review3明确的SkillChainPlan/SkillFollowCast/SkillUsePacket/AutomaticRound等实际路径及完成报价拒绝所必需的紧邻调用文件；AutomaticRound若原在B目录，现单独转归你处理。B只改SwordEntityMotion.java，不冲突。不得动B幻剑、C火伤、文档指南。公共核心无需新模型决策。
保存本次修前字节pre-repair-3/A/，保留已有基线及返修备份，更新author-A-manifest.json。可跑既定变量命名门禁，OriginalHumanoid两处既有问题不扩修。最终一次完整报告修改、溢出拒绝落在哪些调用边界、原计费次数/收据如何保留、任何未完成/编译阻塞，并明确所有写入释放。后续只统一编译，无第4轮复审。
