固定Cursor CLI --model grok-4.7-high-fast，禁止换模型/降档/spawn。工作区D:/alianximod。完整读取TaskFile、PromptFile、AGENTS.md、max-dispatch技能、新流程，以及本批implementation-contract.md、interfaces-delivery.md、记录.md当前状态。用户已授权制作；Astra设计第1轮定稿，不重新设计、不增加正式复审轮次。批次设计1/2、实现0/3。你不改批次记录、TaskFile、启动器或.grok回执。

前置：本单只允许主会话在I接口阶段final回执valid/reported_done/writesSettled=true后Start。公共api/计划/能力/注册纯类型全部归I；你可以读取但绝不能编辑。本文引用的新增接口精确签名以interfaces-delivery.md和对应已收束源码为准。若公共接口缺少合同必需信息，报告具体阻塞交I集中修订，不能复制另一个同名DTO、强转Object、反射/线程局部/可变全局假装兼容。

只修改TaskFile writePaths范围，相关旧Java可定向阅读，禁止通读整仓/碰其它作者。保持既有未提交改动，禁止reset/clean/checkout。不得gradlew/build/jar/reobf/runClient，不启动/操作游戏，不动制作表。不启动正式复审或独立测试任务。完成作者必要自检即可，最后待全批集中复核及统一编译。未确认美术不复制入src、不登记正式加载路径、不用同名旧资源假装新效果。

本次无实时跟踪；遇到阻塞完成可完成部分，立即交最终回执说明边界，不在中间向未授权聊天发消息。共享整合由I在C/E/V全部停写后完成，因此只需交付真实实现与精确接入工厂，不要擅自注册自己。
最终返回文件/关键行号、实际完成和未完成、I需要调用的精确工厂/签名、已做自检及风险；你的交付报告写到本Task指定路径。唯一机器回执末尾严格：
<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"实际工作结果","changedFiles":[],"checks":[],"notRun":["未编译","未游戏验证","未正式实现复审"],"writesSettled":true,"blocker":null,"nextAction":"主会话收齐当前阶段最终回执后交I共享整合"}
<<<END_CURSOR_TASK_REPORT>>>
字段填真实值；workStatus可reported_done/blocked/needs_input/failed，不掩盖未完成。不要填task/attempt/session/inputHash，由启动器添加。

你是V，客户端动作/协议作者。本次只做不依赖美术批准的框架，写visual-framework-delivery.md；视觉同版用户确认仍待主会话后续明确通知，绝不把草稿或沉默当批准。先读合同第8/9/10以及公共接口交付。
- 为玩家与普通分身共用一个ActorActionPublisher / ActionPlayback传输缓存/采样/渲染架构；s2c服务端时间，serverSession+dimension+family/member+actorUUID/entityID/世代+单调sequence+resourceID+startTick+windup/recovery+visualYaw+mirrored+有界渲染武器快照全部保留验证。
- 每actor只留最新，旧stop不能清新，迟到按服务端时间快进，tracking新观众只补当前未结束快照；断线/换维/卸载/世代变化/结束清理，有限缓存；start/stop/tracking才发包不每tick广播。由真实服务端事件结束，客户端不能提交伤害。
- 客户端姿态框架支持实际演员身体/四肢及持物，玩家相机/逻辑yaw pitch/服务端碰撞盒保持原值；默认单斩立即出刀8tick收势，回旋6+6tick，开始回旋后出刀不能被solo动作覆盖。时长来自冻结DTO。代码框架可完成，正式动作关键帧等批准后才加载。
- 给I明确服务端publisher工厂、事件/registrar挂接；XueXiMod等公共主入口由I接，不越权。CloneEntity字段由C独占，不能改，使用公共DTO/现有身份；需要额外数据报告C/I，不能另写服务端CloneEntity。
- ordinary分身显示武器只是render副本不生成item、不伪造装备参与伤害。不能把青灰示意假人替换为运行模型；既有normal外观沿用，水幻形仍走已确认Steve水线。特别保留CloneRenderer调用和SteveWaterLineVisual先skin后flakes提交修复，不触碰water png/bin或scatter路径。
- 允许对列出的现有动作/渲染钩子作必要最小扩展，保留原space_cut行为。不改旧spacecut资源、不会把任何旧效果复用当作新动作假完成。若需要本单之外的共享文件或新mixin配置，交付精确接点由I统一处理，不自己写。
- 当前writePaths不含src/main/resources，禁止新素材/动作JSON、lang/mixin配置写入。不要硬编码还未确认的骨骼曲线；完成协议、采样器能力和清理框架，真实美术待批准后原task/session续接。报告要明确“框架完成、视觉尚待确认接入”，不能称游戏可见效果已完成。

