# 水幻形：先冻结并行作者接口

阶段：已批准设计后的实现准备。固定 Cursor CLI `--model grok-4.7-high-fast`。本任务不作额外正式复审，不启动子代理，不降档。

完整读取 `D:/alianximod/docs/水幻形术式设计与实现指导.md`、本批 `design-decision.md`、`用户决定.md`、相关最终事实报告、AGENTS.md、`.grok/skills/max-dispatch/SKILL.md` 和现行配置/开发流程。按设计已裁决合同执行，不再自行另选机制。

最新视觉决定优先：v1圆润人形被用户否决，正在由独立Cursor在真实Blockbench直接画标准Steve六方块模型的v2。用户明确允许不先做二维。v2也尚未确认。不得读取美术作者的实时工作，不操作美术MCP，不使用v1轮廓，不接入任何候选资源。具体外观、water_phantom正式注册/专项声明仍等待确认。

目标：为设计第15节A/B/C/D多作者独立实施，先冻结**与现有代码真实签名相容**的精确方法、字段、记录和跨组调用协议。主会话稍后按你的最终合同并行派发A/B/C；你本次只读相关代码，写一份合同文档，不修改源码。

允许读取：项目内与捕获/分身/断潮/付款/配置/释放输入/生命周期/表现协议直接相关的源码和文档。不要整仓通读，不复审旧批次，不修改已有未提交改动。禁止Gradle/打包/运行游戏/改表/提交或重置。

唯一写入：`D:/alianximod/docs/开发记录/水幻形术式-20261102/implementation-contract.md` 与 `implementation-contract-report.md`。不能改详细设计、其它报告或任何src文件。

合同应可直接给各作者实现，至少包括：

1. A核心、B载荷体积、C装配入口、D表现协议的准确现有/新建路径与唯一作者。列出需要先交回再改的共享入口。A负责整个clone/core包，B负责clone/action和clone/capture，C负责release/capture与clone/water及公共释放入口。实体实际路径若不在core，明确归A。D客户端/网络独占。
2. `CapturedAttackSnapshot`、`PreparedCapture`、`DetachedActorFrame`、`DeploymentLease`、`CaptureQuoteContext/Quote`、`ActorVolumeSpec`、`CapturedStructurePlan/Input`等精确构造参数/访问器/不可变规则。避免跨包读取私有状态、循环初始化、缺class/占位成功代码。
3. A→B行动执行及租约唯一转移、B→A结束释放与owner/session校验；A→D只读事件发布接口；C→B报价/C→A生成/C冷却的准确签名和返回错误。不让B扣款、不让C复制A事务。配置最终声明尚未调用时，类可以存在但不能以假注册/假seal绕过。
4. 每tick入口和服务器/世界/玩家生命周期谁接，D完成前A用类型化无操作表现接收器的边界（仅无注册路径，不冒充正式外观）；B独立体积渲染怎样与D分离。说明公共注册最终由谁统一接线。
5. 保留设计第7节致死hurt六步原意；本任务不重裁决。给A标出现存落点名即可。未知事实确实不满足既定合同则交回具体阻塞，不自行改架构。
6. 分别给出A/B/C能立即实施范围和仍需等待的文件/调用。接口先落为文档，不让一个作者临时写另一个作者的文件；后续各自从本文按同一签名实现。

不需要输出冗长旧源码；只交精确合同和最少事实依据。最终一次性返回路径、真实阻塞（若无写无）、停止写入声明。主会话只等待最终反馈，不要中途要求实时审查。
