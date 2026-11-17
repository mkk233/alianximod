工作区D:/alianximod；固定Cursor CLI --model grok-4.7-high-fast，不换模型，不spawn。完整读取本TaskFile、PromptFile、AGENTS.md、.grok/skills/max-dispatch/SKILL.md、docs/统一开发与术式批量制作流程.md。本次已获用户开工授权，困难方案由唯一Astra在设计第1轮定稿，不再重新设计/正式复审。批次设计1/2已结束，实现0/3；你不改额度或批次记录，也不自行启动审查。

你是唯一共享集成作者I。当前阶段仅落盘可供消费者稳定引用的公共接口/不可变DTO/能力定义/注册接口，并交付精确接口清单；绝不提前修改旧共享源码或接入业务。必须先读 D:/alianximod/docs/开发记录/协同回旋斩击-20261103/implementation-contract.md 全文、design-round-1.md 和必要事实facts-*.final-*.md。合同语义与接口/文件单作者为硬边界。新增类的实际包名/字段可按仓库Java17和既有类型合理落实，但不能缺失事务/付款/取消/身份/演员/配置数据；若名字冲突，本阶段集中给统一映射，后续消费者不得自行改签名。

允许写路径仅TaskFile的writePaths；整个公共api包归你。TechniqueExecutionCapabilities、ExecutionFamilyPlan、ActorAttackAdapterRegistry可放上面路径。如必要有公共基础纯类型，优先落api包；不要改其它目录以满足接口，不建立依赖尚未实现的C/E/V具体类。接口本身不调用世界/charge/hurt/客户端；不可变集合及只读装备快照，不能把可变Skill/NBT主手引用跨tick存入plan。CloneSpawnQuote/既有载体付款类型按真实来源引用或提供明确适配口。SettlementEnvelope包含合同所需载荷的类型安全边界，别用一个Object掩盖未定义接口。ReleaseGate和生命周期、CooperationActors、reservation、部署DTO、ShortReposition公共运动计划口、ActorActionPublisher及ActionPlayback、攻击prepared类型/几何、计划费用份额和低层原子发布端口都需一次明确，否则后续作者无从闭合。本单是接口层，不生成伪实现/成功占位去宣称业务完成。

现有Java只定向查合同及事实所指直接依赖，保留全部已有未提交修改，禁止reset/clean/checkout覆盖；本阶段无其它源码作者在写。不得gradlew/build/jar/reobf/runClient，不启动/操作游戏，不修改制作表、启动器、.grok运行回执、其它文档或已确认水幻形美术。未确认斩光/动画资产一律不进src/resources。本单不编写/运行独立测试。

完成后只写 D:/alianximod/docs/开发记录/协同回旋斩击-20261103/interfaces-delivery.md：每个公共类型的绝对路径、精确签名、字段语义及真正使用的既有类型，C/E/V各应实现/调用什么，哪些端口I最终整合负责，最终类/工厂接入方式，是否还有阻塞。逐一核对源码类型与该清单一致属于实现者自检，不是新复审。本次不编译，明确待统一编译。若无法闭合接口，report blocked/needs_input并具体给出缺项，不擅自改设计。

最终全文报告返回changedFiles和必要接口摘要，并以唯一机器回执结束，标记独占一行：
<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"接口层交付，业务实现和统一编译尚未进行","changedFiles":[],"checks":[],"notRun":["未编译","未游戏验证","未接入未确认美术","未正式实现复审"],"writesSettled":true,"blocker":null,"nextAction":"主会话读取接口交付清单后派C/E/V，I等待它们收束后续跑原会话整合"}
<<<END_CURSOR_TASK_REPORT>>>
只填真实结果，不写task/attempt/session/inputHash（由启动器添加）。退出0不是业务完成，必须回执完整；有阻塞则使用正确workStatus，不谎报done。
