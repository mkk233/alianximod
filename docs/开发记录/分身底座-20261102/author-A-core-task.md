模型：Cursor CLI --model grok-4.7-high-fast（显式固定，不换模型）。阶段：实现，无编译授权。设计第1/2轮已定稿，实现正式复核尚0/3；本单为作者实现/必要定向自检，不另开独立测试或正式复审。必须完整读 AGENTS.md、.grok/skills/max-dispatch/SKILL.md、docs/统一开发与术式批量制作流程.md、docs/分身底座设计与实现指导.md，以及本批 implementation-contract.md。按两份合同实施，不自行扩大范围或重审旧批次。

用户授权基础分身底座，先设计再实现。真实开发闭环：两类驻留分身、主动/受击基础脉冲、有限诱敌、生命周期、配置消费者、权限2命令与普通入口过滤。新视觉只复用原版模型/贴图，不制作新美术。不开放任意现有链/完整水幻形/同步配合AI；不更改旧全局平衡算法/伤害规则，不用 FakePlayer，不挪动主人，不向旧 ChainScheduler/Tally 塞分身任务。

保留所有未提交修改；不得 reset/clean/checkout、提交、改表、打包、build/jar/reobf/runClient、启动游戏、跑 gradlew 或新增独立测试。git使用单次 -c safe.directory=D:/alianximod。不要派其他代理。派发期间主会话只收最终结果，不读实时内容。

严格单文件单作者。依赖其他作者时只看已冻结 implementation-contract.md，不读取其他作者进行中的候选/中间代码来猜接口。确有合同冲突或需要范围外文件，停止那一小部分，最终报告清楚交回，不越权修共享文件。常规实现选择按合同自主完成。

报告要求：本次改动的准确路径、真实完成能力、接口依赖、配置实际消费者、必要自检与未做项、哈希与最后写入时间，明确“待统一编译”“游戏未测”；最后停止写入并声明释放。不要把候选源当成已经接入运行源码。

你是核心作者 A。本单只写不参与项目编译的候选源码：
D:/alianximod/docs/开发记录/分身底座-20261102/候选源码/src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/**
及本批 author-A-core-report.md / author-A-core-evidence.json。实际 D:/alianximod/src/main 和其他候选目录一律不写。实现后这些文件会由整合者按原字节接入；包名仍采用正式生产包，不含候选目录名。

按设计第4—8、11—13节和实现合同完整实现归属A的实体、状态/DTO、生成/请求事务、owner索引、独立有界队列、次数/冷却、生命周期、诱敌、来源作用域、权限与去重。所有新增DTO/接口必须真实可供本次B/C消费，不留下空方法或TODO替代核心。
- owner/actor/session/generation/action/root event边界清楚，引用只活当前server，停服和卸载幂等清理，不强制加载或全实体恢复。
- 幻影一次提交后结束；普通多次；受击只在非致死已接受之后请求，环境/主人/友方/其他分身来源不形成递归。
- 请求先预约且不扣钱；prepare/receipt/commit线与finally终止优先完全按合同；生成ticket与容量提交要可撤销，不泄漏owner记录。
- 诱敌必须有实际消费者和错开/预算，不只可hurt；只按明确白名单和目标关系。
- 与B/C使用冻结接口；B负责Action/CloneBalance，C负责EntityTypes/TechniqueRegistry/性能枚举/命令渲染，不得越界写那些文件。
- 如implementation-contract确有机械签名缺口，你可以在本单owned报告里提出精确补充，不与B/C改同一合同，也不能改变设计语义。
本单禁止编译。命名门禁若仅扫描真实src，候选的统一门禁留接入后执行，报告如实注明。
