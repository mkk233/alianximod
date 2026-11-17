# 水幻形非视觉实现任务

阶段：实现。固定 Cursor CLI `--model grok-4.7-high-fast`，不降档、不spawn其它代理。主会话只等待最终反馈，不实时跟进或逐项复审。

完整阅读：AGENTS.md、.grok/skills/max-dispatch/SKILL.md、docs/统一开发与术式批量制作流程.md、docs/配置平衡指南.md、docs/水幻形术式设计与实现指导.md，及 docs/开发记录/水幻形术式-20261102/ 下 implementation-contract.md、design-decision.md、用户决定.md。本批设计1/2已定稿，实现复核0/3；实现作者本次不自行开独立正式审查。按合同落实，不自行重裁决。

最新用户视觉：v1圆润人形已否决。真实Blockbench直接在标准Steve六方体上画透明水线v2，用户明确覆盖二维先行。当前v2尚未视觉确认。不得引用v1轮廓、尺寸或帧率；不操作美术MCP、不读取艺术实时中间稿；不得把候选资源写入src，也不得挂接water_phantom正式PLAYER注册和专项声明。先实施设计15.1的独立逻辑，具体视觉和正式登记由后续唯一整合者完成。

写入遵守本任务分区与 implementation-contract.md 的真实路径，冲突以先报告为准。每文件只有一个作者，不补写其它组缺文件，不替其它组修代码。不读取其它组进行中的实现判断正确性；消费的是冻结合同签名，各自最终反馈后由整合者处理接线。可只读相关既有源码，不能整仓通读或复审旧批次。保留全部既有未提交修改，禁止reset/clean/checkout覆盖、提交/打包/游戏/制作表改写。

本次无Gradle授权；完成写“待统一编译”。命名门禁可执行 `powershell -NoProfile -File tools/check-name-gate.ps1`，仅修自己的改动，报告同期其它组尚在写入。不要创建镜像实现的无意义测试。最终一次性报告实际改动清单和行号、合同实现状态、依赖/阻塞、配置消费、未跑项、已停止写入；明示未编译未游戏。最终写入交回后不再改源码。

## B：断潮actor适配、冻结报价、独立攻击体积、显式能力
本任务唯一作者范围按 implementation-contract.md B分区：clone/action最小扩展、clone/capture新适配和独立体积包。报告只写本批 implementation-b-report.md。不得改A核心、C公共入口/定义/配置注册、D网络渲染。

实现设计3/5/8/9/10和15.1的B范围。只有tide_sever进入显式适配表；按部署时真实现有配置冻结ConfiguredDamage、费用/冷却、尺寸/寿命和既有正式视觉键，不复制常量覆盖配置，不二次乘全局/局部倍率。缺月等其它水系一律不伪装支持。

独立静止体积按合同原点/OBB几何/方块阻挡/实际伤害权限与去重，每tick重新开启来源隔离，classifyRespecting(NONE)，保持无敌帧，不含旧领域/主手/火种/潮印副作用。严禁旧effect.execute、spawnReported、tryHit或HostGroup/Tally复用，不修改旧四水系/TechniqueHitSpec/TechniqueHitSettle/TechniqueHits。每次资格检查排除所有CloneEntity，取消/适配版本变化/过期都释放同一租约。B不扣钱、不写玩家冷却、不结束身体，跨组操作只走冻结接口。

CloneActionRegistry从instanceof脉冲硬编码改为显式能力登记，原脉冲原能力保留，未知动作默认拒绝。网络/视觉只是无实体引用的只读描述，D负责呈现。实际tick和生命周期按合同绑定，不静默遗留未调用runtime。未归本组的挂接由报告给指定作者。

