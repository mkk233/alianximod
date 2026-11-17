# A：固定实施接口并交接

固定 Cursor CLI --model grok-4.7-high-fast。执行阶段：已批准设计的实现准备，不另作正式设计审查，不编译、不测试、不改表、不启动游戏、不提交。完整读本目录 design-decision-1.md、记录.md 及自己的已完成链路报告。当前新增功能设计1/2已经定案，实现0/3；不重新审旧接触计费。

你是本功能唯一链作者A，负责 design-decision-1.md第6节A列出的network/TechniqueChain与新增链契约。此子任务先只写 docs/开发记录/领域绑定规则-20261030/implementation-contract.md，锁定三个作者共用的精确Java包、类名、构造器/方法/字段签名、所有权和调用关系，返回完成反馈；暂不改源码（下一单按同一会话完整实现A）。这不是让你重新发明架构，而是把已批准的职责转成能并行编程的契约。

必须给：BindingLayout及原始索引传递、BoundReleasePlan的冻结与访问、ReleaseExecutionContext及自动/手动分支入口、自动执行入口返回RoundHandle、有效性token、C的DomainService所有tryWave/tryWaveReserved兼容重载与bound plan参数、C按每轮需要调用A的精确入口。B需要的跨tick自动发射源/局部目标类及CastFrame/HitSpec新增字段由B所有，但签名在这里统一；A构造context时能传什么，B如何访问/把生成点约束到几何中，C如何提供采样和令牌，要明确。避免双方互造同名类和循环编译依赖。

你可以只读自己现有network/TechniqueChain代码以适配当前接口，不读取另一候选查询任务正在写的文件。作者C会先实现新ChainRole RULE_DOMAIN_BIND、ModTechniques/ReleaseDefinitions注册和DomainGeometry/AxisBoxGeometry采样/约束；请与固定基础契约对齐：DomainGeometry新增 Vec3 samplePoint(RandomSource random)、Vec3 clampPoint(Vec3 point)，均真实bounds内，AxisBox实现；其余领域服务重载等由你在契约定义，C随后实现。新标识rule_domain_bind，名称领域绑定，普通无属性规则，既有other_none图标，0魔力0装配消耗，无新素材。

B将有权修改CastFrame、TechniqueHitSpec、ProjectileMotion、FollowCasterMotion、SpaceCutEffect、LiaoliEffect、PhantomSwordEffect、SwordEntity、SwordVortex和新增跨tick发射源类型；TechniqueHitEntity/TrackingSupport等外部占用文件暂禁止，待主会话交接再放行。你不要读取它们的中间版本，不写A范围外文件，不建占位源码冒充实现。

契约应充分复用现有计划、报价和预算，不在领域端复制扫描计价。能按设计明确的常规技术选择你自己定；确有无法实现的接口冲突就列事实阻塞，不换语义。输出精确可派发文本后结束本单，不在等待其它作者时驻留或轮询。主会话收到此最终反馈后立即派A完整实现及B/C使用这份契约。
