独立Grok4.7只读审查公共能量输入接线。架构已经GPT-6-astra裁决，你不要调用其他代理/brain，不写代码，不运行游戏或Gradle。
先读本任务目录shared-contract.md、route-report.md。基线before/是本次开始快照，git HEAD含很多旧变更不可当本次全部。聚焦下列本次修改：
network/SkillChainPlan.java、CompiledReleaseStep.java、SkillSegmentRunner.java、CastExecutionSession.java、SkillUsePacket.java、SkillAssemblyPacket.java，及实际新增的路线辅助文件和RouteReleaseInputCheck.java。ReleaseInput本身已独立审查且77断言通过，不重复做全量审查。B/C消费者只需核对调用签名与传参，别重做它们的独立审查。
检查每个实际成员在计划建立时冻结自身合资格链输入，延迟/双重/一分为三/接触/预约/条件/宿主/附属场地同一份沿用，执行时不重新扫描装配。剑术成员和剑技槽整条链输入BASE；定义自身1.35等仍保留，附魔半径和寿命不混入链输入。新装配不能往剑技槽放聚能，历史链服务端也不加成，不按ID特判。费用/单次伤害/冷却/速度/索敌/退款身份不能变化。
已知待补：SkillFollowCast旧入口缺技能槽上下文，主控会让A补显式快照，这个已知点不作为新发现但列待复核。无属性MAGIC/NONE资格仍待用户，当前保留既有扫描属于明确未完成，不能你替用户决定，也不重复报为未知缺陷。整体修复不能称最终资格完整。
测试检查应验证真实生产计划并说明未执行，不把手工构造伪成完整游戏执行。只输出可复现有证据的缺陷，路径行号、触发条件、修复建议；没有新缺陷则写有上述待办的PASS。按diff精确定位相关方法，不重新通读整仓。