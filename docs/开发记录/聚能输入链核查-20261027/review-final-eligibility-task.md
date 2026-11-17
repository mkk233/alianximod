继续独立Grok4.7复审，只读不改代码。前轮你对公共路由发现两项：现行菜单漏挡聚能、两条BASE对象身份测试错误。已按GPT-6-astra最终裁决修复，生产/测试改动快照在 eligibility-review-snapshot/，改前在before-eligibility/。作者同时编译，后续如发生源文件差异主控会核对。本轮只审最终资格/注册/对应测试这项，勿重复通读所有路由与其他消费者。

最新范围已确定：按用户原话“聚能只对元素术式生效，剑术无法使用聚能”的较窄口径执行，NONE不纳入。这不是用户回答过先前选项，也没有待答阻塞。工厂必须同时满足skill MAGIC、member MAGIC且属性非null非NONE、definition HIT_VOLUME、非附魔；任何缺失/越界/剑技槽/SWORD成员/ACTION/RULE/UNIVERSAL/MOVEMENT/NONE都BASE。不要过滤baseDamage>0：雾雨/水环等零伤MAGIC+WATER、WaterReleaseDefinition.kind HIT_VOLUME必须2/4。ownEnergy1.35仍保留，不能被资格BASE覆盖。

聚能自身注册类型改MAGIC使当前SpellAssemblyMenu.rejectReason/mayPlace/set现有类型检查拒绝装剑技，而非只依赖旧SkillAssemblyPacket.put。核实TechniqueItem/Data.getTechnique如何读取类型，旧物品按id取新注册类型还是缓存UNIVERSAL；检查实际服务端菜单入口确实拦住，新注册不改变普通规则动作兼容。既存已组装剑技释放由工厂BASE兜住。不要求按id分支，也不要为了更改做无关架构重写。
两条原BASE身份断言应改为数值1及不复用前成员倍率2；不要改变生产工厂对象策略以迎合测试。新RouteReleaseInputCheck确实调用生产工厂/计划，增加上述资格matrix。
SkillFollowCast已由GPT-6-astra全量符号查证src/main/java无外部调用，是未使用遗留入口，本次不改也不重启，不再当运行漏接。资格与dead-entry说明以本条为准，不重复旧报告未答状态。
给PASS或可复现新缺陷的路径行号/触发/修复建议。不要跑Gradle/游戏，不写文件，不调度brain/subagent。