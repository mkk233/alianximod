# C：领域绑定周期运行

固定 Cursor CLI --model grok-4.7-high-fast，新可写会话，接续已完成C注册作者角色。读本目录implementation-contract.md（正式契约）、design-decision-1.md、implementation-C-registration-final.md、记录.md、read-domain-final.md。设计2/2已用尽，不追加设计审查，实现0/3等全体写入收束后集中审。

完整实现正式契约中C的运行职责：每份领域安装冻结BoundReleasePlan，first=startedAt+20、每20tick固定无目标也触发；活跃域快照按ownerUUID/instanceUUID稳定顺序逐轮付费执行；真实域内取随机源点，域内完整包含的最近合法目标，等距UUID，无目标均匀球面方向；本轮武器数值副本、世界/主人/寿命token；未完成RoundHandle集合每tick清理，close同步cancel并清空，已生成攻击不受关域清除。

独占源码：D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainInstance.java、DomainService.java、新增DomainBindingRuntime.java（或正式契约等价类）；原五文件ChainRole、ModTechniques、ReleaseDefinitions、DomainGeometry、AxisBoxGeometry仍由你独占，当前注册和几何已经完成，只有必要契约对齐才修改。ReleaseDefinition能力声明确有需要归你，但不凭空添加。本单不改DomainPulses、WaterReleaseDefinition、WaterTechniques、ReleaseBaseEvents、UI、资源、旧接触计费或mixin。

使用A的bind公共契约，不能domain→network依赖。C不能安装执行桥，按DomainService正式重载传入boundPlan与claimSuffix；旧无绑定重载/去重完全保留，新同声明后缀防止不同绑定误合并。开域只保留既有起手费，每轮费用由A执行器负责，C不另charge重复收款。TargetCheck冻结几何/世界/关系，不能绑定active使已生成实体在关域后无端失效；LifetimeToken才含active、expiresAt、主人在线存活同维。每轮保留自己的context和handle，不调用按键包、玩家准星、锁定标记或slot。引用现有关系策略，不顺手改变盟友语义。

A/B并行按implementation-contract.md实现，不读写他们的中间源码、不创建占位类；直接按契约写，等主会话统一集成。外部候选查询A已released，可只读其最终接口；本次不改其query/mixin、ReleaseBatch、TrackingSupport等别人的文件。外部仅PerfMetric/PerformanceRecorder注释收尾，与本任务无写重叠。

你前单报告所有Shell被允许列表Rejected。本单不绕过拒绝或改审批：源码和完整改前内容备份使用获准的原生文件工具。所有拟改旧文件先完整保存backup-C-runtime/相对路径，不能把上期逆补丁冒称完整备份；无法保留内容就停该文件返回。禁止reset/clean/checkout，保留其他未提交改动。文档只写implementation-C-runtime-final.md、evidence-C-runtime.json与自己备份，hash/mtime无系统读数就null。

禁止编译、测试、打包、游戏、提交、改表、改权限或模型。现有命名门Shell获准才跑，否则如实未跑。全部写入收束后给最终报告并released=true，列周期/关闭/目标/费用衔接事实、改动文件、已完成注册语义和未验证项，待统一编译。不要因为主会话后续还会检查就省略必要实现。
