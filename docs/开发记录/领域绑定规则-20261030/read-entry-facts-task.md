# 同轮补充入口事实

继续你的链路只读会话，固定 Cursor CLI --model grok-4.7-high-fast。本阶段是设计第1/2轮需要的信息补齐，不作架构裁决，不实现，不编译，不修改文件。无需重读已有长文档；遵守此前约束。

只在 D:/alianximod/src/main/java/com/bslx/XueXiMod/network/ 下既有 ChainSteps、SkillUsePacket、SkillChainPlan、CompiledReleaseStep、CastExecutionSession、SkillSegmentRunner 及其直接依赖，和 shu/technique/release/chain/ChainScheduler、ReleaseBaseEvents 与领域 tick 调用关系里查下列事实：

1. ChainSteps→普通按键、双重编译会话、接触开域三种入口分别怎样切步/消费原始下标？领域后段在哪个入口会同次执行？另一份报告说开域后链继续当次放攻击，而你说领域结束 step 下次按键，请用最小调用链区分，给例子“雾雨→一分为三→垂丝”和“双重→雾雨→垂丝”。
2. 自动子释放能复用的 CompiledReleaseStep/CastExecutionSession 预算和执行入口是否能传独立取消条件、slot=-1、固定武器快照、显式几何？若不能，指出具体硬编码参数/职责耦合位置，不最终设计新架构。
3. ChainScheduler 是否先于 DomainService.tick 执行？给同 tick 到期的延迟子动作须额外检查寿命的事实。

用户已确认每20tick无人也放，域内随机点朝最近有效目标（无目标随机方向），双重可延伸捕获第二攻击并保持原序。开域冻结子链、每份独立收费，首发满20tick后。本次不重新审旧的接触计费。返回精简事实报告，建议2500汉字内，行号必须准确；不列大表、不输出思考过程。原源码未提交改动保持。
