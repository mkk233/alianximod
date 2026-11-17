模型：Cursor CLI --model grok-4.7-high-fast（不得改模型）。本阶段：只读事实定位，不是正式复审。只输出最终事实报告，不改任何文件，不编译、不测试、不启动游戏、不改表、不提交。不要运行其他代理。读 AGENTS.md 和 .grok/skills/max-dispatch/SKILL.md。允许只读 D:/alianximod/src/main/java、src/main/resources、docs 中对应文档及项目构建配置；不要通读整仓，只查下列相关系统。已有大量工作区修改必须保留。git 若报 dubious ownership，可单次 git -c safe.directory=D:/alianximod，不改全局配置。报告写出准确路径、行号、真实签名、调用关系和已知未决事项，区分当前代码事实与建议。不看其他进行中会话的日志和中间代码。最终报告控制在 4500 中文字左右，足够给架构裁决；如不能满足请明确缺失。
用户本次已授权：从最基础开始制作共用分身底座，先给详细设计文档与实现指导，再实施；兼容既有优化和统一平衡配置。两类都可长期存在，幻影只能一次响应，普通分身可多次，需能诱敌并自身位置施术，支持主动/受击触发；未来规则术式安排同步或配合，包括独立站位、交替施术、分合。当前不是按表批量任务，不领取或迁移制作表，不直接制作水幻形整术式或新增美术。基础范围待架构裁决；你只提供事实，不自行设计最终方案。

调查 A：施术与实体接入。
1. SkillUsePacket、CastFrame、ReleaseDefinitions、ChainRole、链计划编译/缓存、TechniqueHitSpec/Entity/Settle、ReleaseBatch.Tally 及普通动作/法杖路径。找出 ServerPlayer 绑定、位置/朝向/锁定/武器快照/费用/附魔/领域/伤害归属/延迟结算入口，哪些能传 actor origin 哪些仍直接从 owner 读；不要凭类名假设。
2. 现有实体登记、LivingEntity/Mob 基础实体、客户端 renderer（可复用原版人形，不制作美术）、NBT/lifecycle/网络登记、命令入口；是否已有 clone/decoy/分身，找到即可报告。
3. 火种、潮印、附魔、真剑与幻剑等高风险专属路径对分身通用执行的阻碍；哪些适合第一版显式 capability 拒绝而保留原玩家路径。
4. 指出可最小改动接入的文件边界，列现有兼容不变量与实际扩展入口。特别检查不得 FakePlayer/移动本体伪装施术，分身的事件不得递归生成另一轮响应。
