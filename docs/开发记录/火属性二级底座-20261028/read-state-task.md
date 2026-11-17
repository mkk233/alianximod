# 只读信息采集：火属性底层状态与资源接点

当前模型必须是 Cursor CLI `--model grok-4.7-high-fast`。本单只读，不改任何文件、不编译、不额外启动 agent。用户授权补齐火属性二级底层设计并实现底层，禁止制作任何术式。

请只定位读取 D:/alianximod/src/main/java/com/bslx/XueXiMod/status、该项目 registry、player 生命周期相关类，以及 statusquery/ResourceStatuses/ResourceView、必要 mixin 和注册入口。读取相关已存在测试与 build.gradle 的测试配置；不要通读整仓。

返回简洁、完整的事实报告（不要做正式设计复审或提改平衡参数）：
1. 实体持久数据、资源查询、MobEffect 注册、玩家登录登出/死亡/跨维度、牛奶移除与治疗处理已有接点（路径、类/方法、行号）。
2. 所有已存在 fire_seed/furnace_core 实现或进行中记录（如无明确无）。
3. 火种 ENTITY int0..100，炉心 player 状态 amp0/1 持久爬坡，需要复用哪些具体API。已有属性modifier/mixin能力，是否有可定向复用的抗性减伤钩子。
4. 当前测试基础设施、可离线执行测试的方式与现存命名风格。
5. 建议可独占的新增状态/资源目录，必须由整合者单写的文件清单。仅是依赖和归属信息，不进行架构审批。

读取是信息采集，不占 formal review 轮次。最终反馈后主会话才继续，不实时跟进。无权限改表、Java、文档、全局git配置。已有所有未提交改动必须保留。报告控制在2500中文字符左右，必要代码签名可附。
