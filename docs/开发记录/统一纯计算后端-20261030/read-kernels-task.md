# 步骤4/5：现有纯数值内核与接点事实

固定Cursor CLI --model grok-4.7-high-fast，只读定位。主会话已读AGENTS和max-dispatch；整体设计2/2、实现3/3已耗尽。这里只获取实现输入事实，不作架构批准/正确性审查，不另开代理，不实现/编译/测试。

用户已授权统一纯计算接口（Java默认实现+C可选实现）。纯计算仅输入数字数组/标量和输出计算结果；世界/实体/附魔/NBT/伤害/同步仍由Java原线程在原时点处理，保留玩法、顺序、随机消费、浮点比较与费用。需要实际被现有热点消费，不能只搭未使用空接口。

允许只读范围：D:/alianximod/AGENTS.md、.grok/skills/max-dispatch/SKILL.md，docs/候选查询复用说明.md、docs/性能记录使用说明.md、本目录记录.md；src/main/java/com/bslx/XueXiMod/shu/technique/release 下的几何、aim、motion、命中几何类，shu/technique/sword 的计算方法，effects/PhantomSwordEffect.java、HitVolumes.java（可以rg --files确认准确子路径），debug/performance/PerfCounter.java和PerfMetric.java。只在这些范围内按必要符号寻找依赖，不通读整仓。不要读取正在由另一个任务改动的network链/释放运行、bind/领域捕获/PublicationAttempt/DomainService/BarrierService/GuardStates文件；需此依赖时只列阻塞。

目标：列出现有可抽取的真实批量数值计算，优先轨迹/生成布局/距离向量或攻击体几何粗筛，报告每个候选的确切路径和方法、输入/输出数字布局、原有运算与分支/同值取舍/边界、原RNG及调用顺序、批量大小来源、读取活对象或产生副作用的位置、当前调用方、是否必须改活跃共享文件。用事实指出当前是否已有纯Java公共计算函数、数值数组批处理、native依赖；不要评价另一个任务或重复审查步骤1/2/3。

交付完整最终报告，包含可在独占文件接线的最小真实操作集合及它们的当前行为事实、必要文件列表、会与多重释放底座冲突的文件、尚缺信息。不是正式设计裁决。保持原逻辑的实施细节由后续原作者在已授权需求内处理，需求外玩法变化必须报告。只返回最终文本，不写源码/文件；外层会保存最终报告。不要运行gradle/native编译器/游戏，不联网安装、不打包、不提交。
