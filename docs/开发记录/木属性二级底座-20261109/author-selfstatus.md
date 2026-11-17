这是独立的可写实现工作项，已由统一启动器显式 Start -WriteMode。旧只读检查会话已经结束并移交所有权，没有源码写入；不继承旧任务的只读限制。用户实现授权与架构裁决未变，仍在同一个 batchId，校验轮次未重置。下面原实现单中有关沿用旧 session 的字样由本段取代：从本次新建的实现会话起，后续返修统一 Resume 本会话。完整读取当前 TaskFile 和架构裁决、对应事实报告。效果作者另读伤害包装补充事实.md。不要派子代理、不要修改启动器或旧运行记录。

# 整合作者 C 第一阶段 自身状态入口

沿用inspect-furnace原task和Cursor session，固定 --model grok-4.7-high-fast。完整读取author-integration.json、本文件、架构裁决.md第4和6节。你是公共接线唯一作者，但本次只做不依赖A/B的SELF_STATUS；其他两位正在新目录实现资源和通用效果。不要在他们写入期间读他们中间源码或提前整合。

本次唯一写入：`shu/technique/release/selfstatus/`、必要的`shu/technique/release/ReleaseDefinition.java`、`network/SkillUsePacket.java`、`tools/wood-base-checks/selfstatus/`、本目录`状态入口作者交付.md`。所有路径以D:/alianximod为根。禁止改FurnaceService/Math/MobEffect/事件、网络注册、资源查询、公共伤害包装、其他作者目录或制作表；下一次收到A/B完成回执后才扩大整合权限。

当前已有文件很多未提交/未跟踪，写前保留基线，只做定点修改。禁止reset/clean/checkout/提交。禁止编译或运行测试（其他作者源码尚在写入），可准备必要定向检查材料。不启动子代理、不再设计新玩法。

按架构裁决将SelfStatusCast从FurnaceMath.Action解耦为prepare/recheck/commit/afterCommit的小型事务契约，炉心三式用适配器保持原有语义。保持原ID、耗魔、冷却、混链/新按键、升级保热、蒸汽和视觉；失败不收费，安装失败退款，成功后视觉错误不得退款或让用户重复购买已生效状态。

新增FiniteMobEffectCast，接受任意已注册有限MobEffect，正时长/合法等级/简单REJECT_IF_PRESENT或REFRESH_SAME_LEVEL策略；拒绝无限/无变化，addEffect失败不算成功。不注册六种花谱效果、不新增花谱术式、不把600tick写成全局唯一时长。

SkillUsePacket失败消息交提交结果提供，炉心原文保留；协同前置路由和现有报价/绑定不改。SELF_STATUS但selfStatus=null不能误调用炉心。SelfStatusOrder现有检查若依赖旧接口，保持兼容或在自己范围内更新。

交付准确API、修改文件、待跑炉心回归点、当前与其他作者互不冲突范围，在`状态入口作者交付.md`写明。最后返回标记JSON，writesSettled=true并停止写入。当前只算本阶段作者交回，待A/B最终回执后继续原会话整合，不自行看运行中的其他源码。

