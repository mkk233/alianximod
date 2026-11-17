# 水幻形非视觉实现任务

阶段：实现。固定 Cursor CLI `--model grok-4.7-high-fast`，不降档、不spawn其它代理。主会话只等待最终反馈，不实时跟进或逐项复审。

完整阅读：AGENTS.md、.grok/skills/max-dispatch/SKILL.md、docs/统一开发与术式批量制作流程.md、docs/配置平衡指南.md、docs/水幻形术式设计与实现指导.md，及 docs/开发记录/水幻形术式-20261102/ 下 implementation-contract.md、design-decision.md、用户决定.md。本批设计1/2已定稿，实现复核0/3；实现作者本次不自行开独立正式审查。按合同落实，不自行重裁决。

最新用户视觉：v1圆润人形已否决。真实Blockbench直接在标准Steve六方体上画透明水线v2，用户明确覆盖二维先行。当前v2尚未视觉确认。不得引用v1轮廓、尺寸或帧率；不操作美术MCP、不读取艺术实时中间稿；不得把候选资源写入src，也不得挂接water_phantom正式PLAYER注册和专项声明。先实施设计15.1的独立逻辑，具体视觉和正式登记由后续唯一整合者完成。

写入遵守本任务分区与 implementation-contract.md 的真实路径，冲突以先报告为准。每文件只有一个作者，不补写其它组缺文件，不替其它组修代码。不读取其它组进行中的实现判断正确性；消费的是冻结合同签名，各自最终反馈后由整合者处理接线。可只读相关既有源码，不能整仓通读或复审旧批次。保留全部既有未提交修改，禁止reset/clean/checkout覆盖、提交/打包/游戏/制作表改写。

本次无Gradle授权；完成写“待统一编译”。命名门禁可执行 `powershell -NoProfile -File tools/check-name-gate.ps1`，仅修自己的改动，报告同期其它组尚在写入。不要创建镜像实现的无意义测试。最终一次性报告实际改动清单和行号、合同实现状态、依赖/阻塞、配置消费、未跑项、已停止写入；明示未编译未游戏。最终写入交回后不再改源码。

## 设计第2/2轮最终补充（最新优先）

唯一设计者已完成并停止写入。必须完整读取本批 `design-round-2-carrier.md`、已更新的 `implementation-contract.md` 第9节及详细设计。设计额度已2/2用完，不再请求新设计复审。此补充属于已批准实现，A/B/D冻结接口不变。

批准仅法杖槽0完整捕获计划的被消费载荷下标获局部SWORD类型例外；水幻形仍MAGIC、断潮仍SWORD，普通剑技及裸剑术法杖门不变。写入范围追加合同精确授权的 `item/StaffSkills.java`、共享载体检查器、捕获相关的原子写回和装配候选构造。你独占这些新增C文件；不得改A/B/D文件。

必须覆盖StaffSkills.castRefusal的前置类型门、offerNow第二门、实际dispatch防御、SkillAssemblyPacket末尾追加候选、SpellAssemblyMenu按非空槽紧凑序的插入/替换完整候选、删宿主/移动后的半链编辑策略、捕获相关失败不吞物品不丢原胚不截断。严格按第2轮已裁决细节，不以全局允许法杖SWORD或按术式ID特判代替。其它未适配载荷依然收费前拒绝。water_phantom正式注册与专项声明仍等视觉确认，不能提前接线。

现在可实施所有C非视觉范围。最终报告应说明每道门与共享检查器的接线、准入/编辑/付款前拒绝行为；编译仍待统一。

## C：整链捕获、公共入口、装配与配置定义
本任务唯一作者范围按 implementation-contract.md C分区：release/capture新包、clone/water新定义与配置类、SkillUsePacket、SkillAssemblyPacket、SpellAssemblyMenu、ReleaseDefinition/ReleaseDefinitions、必要HoldRepeat/玩家冷却和合同明确归C的公共文件。本次不得执行water_phantom实际注册/专项声明调用；ModTechniques最终接线等待确认。报告只写本批 implementation-c-report.md。

实现设计第2/4/5/11与15.1的C范围。声明式能力+显式适配表识别，两格水幻形→断潮才PLAN；任何前后缀、非默认原胚、未支持载荷在所有付款/冷却/游标之前拒绝。普通链NOT_APPLICABLE直返原流程。捕获整链在BatchReleaseCompiler之前统一路由，成功/拒绝均不再落回主人循环。使用原下标且消费两格，不能二次收费/放攻击/获统计。不能散落按术式id特判。

服务端本次输入冻结潜行/视线/载体，长按只处理一次并解除捕获HoldRepeat；普通旧链长按保持。C调用B报价、A付款发布，不复制支付事务；冷却成功一次合计处理，默认40tick，溢出拒绝，失败不冷却。装配和服务端释放共用结构校验，错误清楚中文；仅半链保存编辑并不授予释放许可。

新增WaterPhantomDefinition/Balance类可以存在，但**不正式注册术式，不声明未注册术式平衡键，不假装DEVELOPMENT_ONLY**。配置仅manaCost/cooldown/lifetimeTicks/decoyRadius等合同规定实用字段，伤害0不参与DAMAGE_HOSTS，旧配置未知键保存。最终正式注册和视觉由后续整合任务接线。

原文设计与implementation-contract若个别签名不一致以合同真实签名、设计语义为准；重大不可满足项报告，不擅自改变设计。

