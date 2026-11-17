# 术式链计划缓存：最终开工设计裁决

日期：2026-10-30。整体性能改造累计设计 2/2（本轮），实现仍为 1/3。本文件不是实现验收；设计额度已用完，不再自动追加设计重审。依据仅为本目录三份已收束 Cursor 只读报告和既有流程。未读 Java、未改 Java、未编译。已授权第二步限于结构计划与当次数据分离，不包括查询复用、异步、JNI 或玩法改变。

## 1. 决定与实际交付范围

采用 `CompiledCastPlan`（整条已解析 Skill 的不可变结构计划）、其不可变 `Slice`（一次步进切片/非步进整链）和按次创建的 `CastPlanContext`。继续使用现有 `ChainSegment`、`CompiledReleaseStep`、`SkillChainPlan.ModifierPlan`、CastExecutionSession、AttackBudgetWindow、ReleaseBatch、CastFrame 等运行容器。

复用现有解析算法，抽出纯结构核心，不另写一套角色或双重规则解析器。缓存必须实际替代以下重复结构工作：ChainSteps.list 与对应切片下标；TechniqueChain.split；CompiledReleaseStep 的普通/双重/动作单条件路线判定和成员选择；collectModifiers 的结构扫描与 releasedSegments/copiesBefore 等结构结果；runSegment 的角色向前扫描及其中 sharesDualRelease 的列表重建；冻结链能量输入的纯结构计算。不能只缓存 split 就报告第二步完成。

不缓存任何费用、附魔伤害或冷却数值计算结果。冷却/费用的结构索引可以复用，但原数值帮助函数必须在原分支、原时点调用。这样无需假定护体/屏障/领域定义内部配置不变，也不把法杖 CD 缩减烤进计划。这里的“静态参数”只包括经过资格契约保证的分类、挂载索引、份数规则、结构布尔和 ReleaseInput 的链能量倍率；不是所有配置 getter 的返回值。

## 2. 定义资格与代际

新增公共工具 `com.bslx.XueXiMod.shu.technique.ChainStructureEpoch`：

```java
public static long current();
public static void changed();
```

用 AtomicLong 维护进程内单调代际，构造后初值即可，不在重进服务端时归零。四个注册器 TechniqueManager、ReleaseRegistry、TechniqueEffectRegistry、EnchantmentRegistry 在原有成功 put 之后调用 changed；保留原拒绝/覆盖/异常行为，事件层拒绝注册不得触发成功写入。虽然效果/附魔不是缓存数值来源，覆盖时保守使结构计划失效，避免未来遗漏。不新增热重载系统，不改变原注册线程规则，也不声称修复原 LinkedHashMap 的并发安全。

在 ReleaseDefinition 增加默认方法：

```java
default boolean supportsStructuralPlanCache() { return false; }
```

该能力的契约：现有结构解析所需 getter 是无副作用、与世界/玩家/时钟无关的配置读取；分类或结构配置变化必须在变化完成后调用 ChainStructureEpoch.changed。getter 返回的行为对象仍只在运行期使用，不能放入共享计划。未知第三方实现默认 false，整条链回退原路径；愿意显式 opt-in 的扩展承担此契约。不是按术式 id 白名单。

本仓 ReleaseDefinitions 的构造参数闭包实现 opt-in。SelfStatusDefinition 的本类实现 opt-in；若该类允许继承，使用本类精确运行类限制，避免未知子类继承资格却改写动态 getter。WaterReleaseDefinition 同样本类 opt-in；所有公开 fluent 配置修改方法在完成原写入后 changed（不限于目前判定会影响结构的几个，防遗漏；登记前调用多增代际无害）。不冻结水系对象、不禁止原 setter，也不改其效果。工厂私有赋值在尚未逃逸时不需要逐字段 bump，登记的 put 最终会 bump。未来新增 setter 需遵守同一契约。

资格判断只检查本次已解析链所涉及的 ReleaseDefinition；无定义时沿用现有缺省分支，可缓存该“缺失”事实，后续登记会变代。Technique 的 final 基础字段可用于结构，附魔计算器及护体/屏障/领域/条件行为对象不提取数值或存入计划。资格检查属于每次 lookup 的 O(n) 小检查，不为了缓存省掉必要的内容解析。

## 3. 键、容量与生命周期

键采用值相等：SkillType + 按原顺序的每项术式 id 与原胚规范值 + ChainStructureEpoch。保留重复项和顺序；不能只用 hash 判相等，hash 冲突必须完整比较。不包含技能名称、玩家 UUID、槽位、手持对象、游标、CD 缩减、武器/属性加成。不序列化 NBT；直接从本次已解析 Skill 的类型、列表与对应原胚取值，使用与现有 slice 同样的缺省原胚规则。未知 id 已被原读取流程丢弃，键基于真实参与释放的解析结果。

每次 offer 仍从 NBT 得到新的 Skill，不能缓存 Skill.fromNbt、StaffSkills.asSkill 的结果，也不改装配和迁移逻辑。装配、换法杖、复制/读档内容只要参与结构的值变化，下一次内容键自然不同；不需要改每个写入入口。新旧物品同结构可共享计划，动态属性仍按次读。

仅服务端线程访问单个 access-order LinkedHashMap，最多 64 个整链条目；只缓存长度不超过 64 的链，超长链直接旧路径（不拒绝释放）。每个条目包含非步进整链或全部合法步进切片的结构，不按玩家建 map。每切片只保留有限索引/标志数组、每起始位置一个必要修饰模板，禁止缓存任意参数组合；规模以 64 链长和 64 条目为硬上界（允许 O(n²) 索引，但不能指数展开复制体）。复制次数保存为数值，不物化大量份数节点。结构产物若超出每条目 16384 个索引/标志/模板单元，则该条目不入缓存并回退旧路径；编译过程必须在构造大列表前按索引范围检查预算，超预算不能先把指数产物造完。预算检查不改变玩法能否释放。

每次 lookup 发现 epoch 与缓存已知 epoch 不同，清缓存再处理；注册器不跨线程直接操作 LRU。专用新 `CastPlanCacheEvents` 在 ServerStarted 绑定服务端 identity/线程并清空，在 ServerStopping 清空并释放引用；不改性能采样事件、不碰 ChainProgress 的历史生命周期。未绑定、错误服务器/线程直接无缓存回退。服务端重开不沿用计划，代际仍不归零。

## 4. 不可变图与当次对象

允许直接共享现有不可变 ChainSegment：List.copyOf 包装 split 的列表，成员必须永远引用该 Slice.segments 中的同一实例。禁止为 members 重新 split、单独 new ChainSegment 或与另一切片的列表拼用。所有现有 indexOf(member) 保持能返回正确局部下标。更易审查的模板成员字段保存 int 下标，实例化 CompiledReleaseStep 时再从同一 Slice.segments 取对象。CompiledReleaseStep.members 改为 List.copyOf，不保留输入 ArrayList。

CompiledCastPlan 及全部 Slice/模板只保存枚举、标量、String、不可变索引集合、ChainSegment、不可变 ReleaseInput。不保存 Skill、Technique/ReleaseDefinition 对象、活列表、玩家、Level、实体、ItemStack、NBT、账本、资源回执、行为对象、随机结果、闭包或附魔累加器。

CastPlanContext 每次 executeSkill 新建，持有本次 Skill、选中 Slice、当前代际及必要当次关联；本次 Skill 是原读取所得，绝不反写进缓存。选择步进后仍调用既有 slice 逻辑生成当次 Skill（切片依据使用已缓存布局），以保留局部下标和原胚语义。上下文不要求把所有动态值搬成新字段：现有会话、ModifierPlan、回执、ReleaseBatch、CastFrame 和闭包继续在原位置创建及保存动态数据。ModifierPlan 每次消费模板都新建并复制可变字段/集合，不跨次共享。

运行中遇 epoch 改变：正在使用的不可变 segments 对象图保持有效，不替换整图或重置正在进行的释放。原先已冻结的数据仍按原语义冻结；原先在延迟/接触/段执行时重新读取的结构与定义，先检查 context 当前代际，失配则在当前这份当次 Skill + 同一 segments 上调用原动态帮助函数。失配后不再给该 context 安装新计划，不混用新成员与旧图。其他行为/效果 registry 查询继续在原读取点执行，不把定义实例提前绑定。下一次 offer 使用新代际计划。

若某旧 API 没有 context（例如未确认调用的 SkillFollowCast 路径），保持它的旧实现作为 fallback；不能通过 ThreadLocal 猜“当前计划”。缓存只在显式传播 context 的调用链上使用。

## 5. 冻结跨作者接口与解析接线

以下新类型放 `com.bslx.XueXiMod.network`，可采用包内可见类/方法；跨作者唯一公共依赖是第二节的 epoch/资格方法，网络内部由同一作者统一调整。

```java
// 无资格、过长、超预算、未绑定或线程不符返回 null。
static CompiledCastPlan CastPlanCache.lookup(MinecraftServer server, Skill currentSkill);

// plan 不保存传入的 currentSkill。
int CompiledCastPlan.stepCount();
String CompiledCastPlan.progressSignature();
CompiledCastPlan.Slice CompiledCastPlan.slice(int stepIndex); // -1 表示非步进
Skill CompiledCastPlan.sliceSkill(Skill currentSkill, int stepIndex);

// 构造器包内即可，Skill 与 Slice 属于本次选择。
CastPlanContext(Skill currentSkill, CompiledCastPlan.Slice slice);
boolean CastPlanContext.canReuseStructure(); // 当前 epoch 与 slice.epoch 相同
List<ChainSegment> CastPlanContext.segments();
```

Slice 的结构访问器由网络作者围绕原实际字段定义，不要求扩展成外部 API。至少包含 segments、按段角色结果、按修饰起点模板、成员/份数/挂载关系、会话路线模板、静态 ReleaseInput。progressSignature 必须来自原 ChainSteps.signature 算法、保持仅 id 的原语义；内容缓存键不能传给 ChainProgress。若无步进，stepCount 返回 0、slice(-1) 是整链；有步进只取原 current 游标选中的 slice，保留原范围归一化/推进逻辑。

具体接线：
1. offerNow 保留手持检查、法杖两项加成、Skill 读取以及 offer/重复按住入口。executeSkill 在原结构解析区调用 lookup；命中时使用已缓存步进布局及签名，仍在原位置读取/推进 ChainProgress，仍按旧数值表达式计算 ChainSteps.cooldown 与 CD 缩减。fallback 完整走旧步骤。
2. TechniqueChain.split 仍是唯一分段算法，编译 Slice 时真实调用一次，命中不调用。不要为每次分发重新复制或 split segments。
3. CompiledReleaseStep 抽出结构选择核心，原 compile 与缓存编译共同调用它；缓存存模板，按次 compile 的消费端根据模板在原时点计算原 fullCooldown/actionCooldown、费用和拒绝消息等数值，再创建当次 CompiledReleaseStep。不通过 cooldownReduction=0 先编一次再比例缩放整数；那会改变舍入。所有乘法、求和、强转/ceil 的原顺序不动。
4. SkillChainPlan 的 collectModifiers 抽取唯一结构扫描核心，产生不可变模板；原 fallback 和缓存编译共用。运行期命中模板后创建新的 ModifierPlan。`skill == null` 的 pressCommitCost 路径继续使用 ReleaseInput.BASE，不能误取真实 Skill 对应倍率；实际发射保留原 frozenChainInput。copiesBefore、releasedSegments、sameReleaseGroup、锁定/垂落等帮助函数增加可选 context 的重载/参数，缓存可用时用同一 Slice 的索引结果，旧重载保留原行为。数字费用帮助函数不缓存。
5. SkillUsePacket 普通段循环、动作接触、修饰分支、条件/领域/屏障/护体/力场、纯附魔及预扣费用消费同一当次 context。保留所有原分支顺序，不趁机删除报告认为不可达的逻辑。
6. CastExecutionSession 与 AttackBudgetWindow 保存的是当次 context；第二成员/接触/蓄力完成继续在原时点算伤害、主手或载体、快照、份额与费用。调用 SkillChainPlan 和 SkillSegmentRunner 时显式传递 context；不能复制一份模板以外的共享可变计划。
7. SkillSegmentRunner 的 buildSegmentRunner/buildSegmentRunnerAt/commitNow/commitHostGroup/playAction/runSegment 等实际路径显式传 context（保留旧重载转 null）。角色扫描提取为一个原语义 helper，编译模板和 fallback 都复用它。有效 context 直接取对应 segment 的角色结果；代际失配或 context=null 才在原执行点扫描。蓄力重入继续携带同一当次 context，不提前读位置/视线。
8. HoldRepeat 不需改：它再次调用 offer，会得到新的当次 context，内容相同可命中。ChainSteps 的缓存适配器复用原 slice/list/signature，不改变 ChainProgress；不改 PlayerBoundSkills、StaffSkills、装配包或菜单。

明确仍在原时点计算：魔力/水势/费用退款，领域学习状态、当前 tick/纪元，普通攻击力和法杖属性伤害，接触时再次取得的主手，双重会话已冻结的载体/weaponBonus，附魔反应与快照，锁定/落点/场地查询，动量提交，几何、种子和随机数。5 tick 双重、6 tick Tally、蓄力排程均不改。getter 结构计算提前仅适用于已 opt-in 的纯配置；未知定义走旧路径，不能先编译再发现不安全。

## 6. 观测与文件作者

PerfCounter 增加 `CAST_PLAN_HITS`、`CAST_PLAN_MISSES`、`CAST_PLAN_COMPILES`、`CAST_PLAN_BYPASSES`。一次 lookup 只有一个终态 hit/miss/bypass；miss 指合格且可入缓存但没有条目的查找，成功完整编译并入缓存另增 compiles；预算超限最终归 bypass，不先增 miss。编译异常原样暴露或按原异常处理，不能 catch 全部异常伪装 bypass 继续重复执行副作用。编译过程只有纯结构读取，仍不得默默吞掉异常。

添加 `PerfMetric.CAST_PLAN_COMPILE` 只包真实结构编译；CHAIN_SPLIT 仍只在真正 split 时记，CHAIN_ROLE_SCAN 移到唯一角色扫描 helper 包住实际循环（包括编译时扫描与失配 fallback），命中读取标志不计时。更新指标注释：ROLE_SCAN 从“执行期回扫”扩大为“实际角色扫描”，新旧报告必须注明这一口径变化。继续使用现有 /shuperf，不改 ticks.csv、不开新面板，不承诺命中必然降低 MSPT。

采用两个互斥作者，避免强耦合网络文件拆给多个人。下列已有文件按报告中的类名在 src/main/java/com/bslx/XueXiMod 下定位，禁止扩大到全目录；新类明确落点如上。

作者 A（结构缓存和全部调用接线）：新增 network/CompiledCastPlan.java、CastPlanContext.java、CastPlanCache.java、CastPlanCacheEvents.java；修改 ChainSteps.java、CompiledReleaseStep.java、SkillChainPlan.java、SkillUsePacket.java、SkillSegmentRunner.java、CastExecutionSession.java、AttackBudgetWindow.java；必要时仅为共享原解析核心修改 TechniqueChain.java。可读 ChainSegment/Skill/ReleaseInput/SkillSegmentMath，但不修改这些类型。A 可写本目录 implementation-A-final.md 和 docs/术式链计划缓存说明.md。所有网络内部接口调整归 A，不让另一作者同时写调用方。

作者 B（定义资格、代际和公共指标）：新增 shu/technique/ChainStructureEpoch.java；修改 ReleaseDefinition.java、ReleaseDefinitions.java、WaterReleaseDefinition.java、SelfStatusDefinition.java、TechniqueManager.java、ReleaseRegistry.java、TechniqueEffectRegistry.java、EnchantmentRegistry.java；修改 debug/performance/PerfCounter.java、PerfMetric.java。可写本目录 implementation-B-final.md。B 不修改任何网络调用方、不改原注册事件拒绝逻辑、不改 PerformanceRecorder/PerformanceEvents。

A/B 可依冻结契约并行；A 可先引用 B 的公共接口与枚举。B 完成不意味着可提前编译；两作者都写入收束并返回最终反馈后再统一编译。只允许固定 Cursor CLI `--model grok-4.7-high-fast`；无自动模型替代。两作者实施阶段无编译授权。已有性能插桩及所有未提交改动必须保留。

## 7. 实现验收和用户实测边界

实现阶段剩余自动复核额度为 2 轮（累计最多 3/3），父子共享；收束后集中进行，不按作者重置。不设独立测试任务。不追加设计复审。遇已批准方案内无法解决的真实阻塞，保留现场报告；不要擅自扩展范围或降模型。

静态集中复核重点：缓存真被全部列明入口消费；冷却表达式与费用时点一致；每个 members 身份来自同一 segments；ModifierPlan 不共享；缓存无活对象/闭包；未知定义整体 bypass；水系 fluent 与注册代际完整；迟到回调失配时只回退原动态读取不换图；装配原胚变动使缓存失效但不改变原游标签名；容量上界和重进服清理；性能计数/实际扫描口径正确。编译必须在最终相关源码收束后，由主会话指定的固定 Cursor 执行 `./gradlew.bat compileJava --offline --no-daemon`；主会话不实现、不编译。编译不等于游戏效果验证。

交用户实测重点：相同链重复按下/按住出现 hit；换顺序/原胚/SkillType 后 miss 或新编译；法杖同结构不同 CD/属性加成仍各自正确；步进只改原胚不擅自归零；普通/纯附魔/一分为三/双重/动作接触条件各原时序；延迟期间换武器保留原读取时点；重进世界从空缓存开始；用相同场景 /shuperf 比较扫描次数、编译次数和观测 MSPT。第三方未 opt-in 应保持功能、记录 bypass，不能据此报缓存错误。用户才进行游戏验证，当前不打包、不启动游戏、不迁制作表。

最终决定：按以上两作者实施，复用原解析器形成真正消费的结构缓存，运行期数据继续留在原容器和原读取时点。此轮设计裁决已经完成并耗尽自动设计额度；主会话可直接转交，不再另做一轮独立设计判断。
