# C：领域绑定身份注册与几何接口

固定 Cursor CLI --model grok-4.7-high-fast。本阶段实现，设计已由本目录 design-decision-1.md（新增功能设计1/2）批准，实现复核0/3，等待所有作者收束再集中审查。本单不复审旧接触计费，不编译、不跑测试、不改制作表、不提交、不打包、不启动游戏。先读定案和记录。

你是C，唯一领域/注册作者。此单先实现不依赖A接口的注册和几何：
- 在真实路径ChainRole新增 RULE_DOMAIN_BIND。
- ModTechniques和ReleaseDefinitions注册通用无属性规则 rule_domain_bind，名称领域绑定；描述准确说明在领域后绑定后续攻击链、每20tick自动释放；0规则魔力、0装配消耗，沿用other_none图标，无独立美术。效果可空，但定义不是攻击/聚能边界。
- DomainGeometry新增 net.minecraft.world.phys.Vec3 samplePoint(net.minecraft.util.RandomSource random)、Vec3 clampPoint(Vec3 point) 两个接口。AxisBoxGeometry实现真实bounds内均匀采样和边缘内缩约束；不使用外扩queryBox，不改原生边界/体积判定语义。

精确写权限：D:/alianximod/src/main/java/com/bslx/XueXiMod 下上述五个类，先rg查真实唯一文件位置再编辑；文档仅本目录 evidence-C-registration.json、backup-C-registration/ 和 implementation-C-registration-final.md。拟改旧文件先按相对路径备份并记录hash/mtime，保留所有既有未提交改动，不reset/clean/checkout。不要写A的network/TechniqueChain和B的frame/实体。此单暂不修改DomainInstance/DomainService；A正在固定这些接口，下一单由你继续实现。

共享工作区另一候选查询作者仍拥有TechniqueHitEntity/Settle、SwordEntityMotion、HitVolumes、TrackingSupport、LockTargetService、ReleaseBatch、ContactEnchantState、query/和其mixins，禁止写或读取其进行中版本；不跑Gradle。

必要编辑自查可用现有命名门（不编译），不新增镜像实现的测试。完成所有本单写入后交最终报告：确切注册/类型/消耗/图标事实、几何接口签名和边缘行为、文件列表与最终hash/mtime、未验证项；写明待统一编译，交还文件写权限。你仍是这些文件后续唯一作者，主会话会继续派领域运行单。不得宣布整个领域绑定功能完成。
