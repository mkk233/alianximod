# 火属性二级底座：实现集中复审第1轮

结论：本轮不通过，返修以下6项后再进行统一编译。本轮为实现1/2，不重审已确认设计、不制作术式，不作测试阶段验收。取证使用Cursor CLI grok-4.7-high-fast，只读会话f9ff5677-737d-4aea-a5cd-e99eae6a7d8c，success/exit0；依据review-evidence-final.md、公共规格/契约及compile-first-final.md。三作者收束后取证，期间源码冻结。本人未直接通读Java、未改源码、未运行Gradle/游戏。

## 必须修复

### R1 [P1] 炉心事件存在3处实际编译错误
路径：src/main/java/com/bslx/XueXiMod/status/fire/furnace/FurnaceEvents.java:27、41、50。
TickEvent.PlayerTickEvent.player、PlayerLoggedInEvent.getEntity()、PlayerEvent.Clone.getEntity()静态类型已经是Player，却使用instanceof Player player。JDK17拒绝编译。
最小修法：直接取得Player局部变量，保留原服务端、事件阶段与死亡/存活分支条件，不改生命周期语义。实际compileJava退出1；这不是命名门禁或环境猜测。

### R2 [P1] 专用火伤仍改写普通攻击的冷却与伤害差额
路径：src/main/resources/data/minecraft/tags/damage_type/bypasses_cooldown.json；src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/FireDamage.java:48–49。根因在本地Forge LivingEntity.hurt:1161–1174。
fire_resource带BYPASSES_COOLDOWN会全额进入else，但该分支先执行lastHurt=pAmount、invulnerableTime=20。火伤2之后立即普通攻击6只进入差额4；普通攻击1会被拒绝。4tick火伤不断刷新这个状态，也可能把前一记较大普通攻击的lastHurt改小。这是影响普通攻击的确定副作用，不能以“没有手动清零”视为满足隔离。
最小修法：保留专用标签，只对xueximod:fire_resource在hurt的else分支跳过lastHurt和invulnerableTime的两次字段写入，保留原值，不清零、不全局放行、不绕过actuallyHurt及防御。使用定向PUTFIELD混入并准确约束方法/分支/ordinal/require；不改if分支的普通差额更新，不改handleDamageEvent。优先在字段重定向中直接取得当前hurt的DamageSource参数，不用一个会被嵌套伤害覆盖的单值HEAD缓存。注册新mixin。
验收定向场景：普通先命中后火伤、火伤先命中后普通、两种伤害交错；专用伤害持续落地但不改原普通冷却和lastHurt。

### R3 [P2] 元素作用域会被未声明元素的嵌套伤害借用
路径：src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/FireRuntimeEvents.java:31–42；ElementDamageScope.current/open*；FireDamage.java:48–49；SteamBurst.release。
帧只有线程栈，没有目标、DamageSource或单次hurt身份。外层openWater/openIce期间事件同步触发另一记未开帧的hurt，内层会使用外层标志，可能把易伤加在错误目标并先消耗vulnerabilityApplied；外层随后反而不加。未结算火帧同样可能把外层B×F加给内层。蒸汽不开帧并不等于隔离。当前本批资源火伤均使用已结算帧，因此不能夸称这些现有调用已经普遍重复增伤；但所交付的显式元素公共接口存在确定串用路径。
最小修法：元素上下文绑定目标、DamageSource及单次hurt调用；事件只读取本次调用拥有的帧。嵌套的未标记hurt必须得到无元素上下文，不能继承外层；嵌套的显式hurt建立新帧，结束后恢复外层。可统一由包装入口和hurt调用栈标识实现；仅靠fire_resource类型过滤不够，因为明确水/冰接口也需可用。蒸汽保证明确无元素。不要接入或改写旧术式。

### R4 [P2] 燃烧tick末尾旧账本覆盖同步回调的新状态
路径：src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/BurnCarrier.java:22–47。
当前是read→advance→逐段hurt→write旧内存账本。hurt中的正常同步监听若schedule新燃烧，新轨道会被最后write盖掉；若discard清空且目标仍活着，旧账本会被写回，恢复本应取消的燃烧。这不要求planner违反纯函数约定，是伤害事件自身允许的重入。
最小修法：先把本tick推进后的账本提交，再分发伤害，之后禁止以旧快照覆盖持久状态；重入schedule以已提交账本为准。discard/死亡发生后不得由剩余批处理复活计划，必要时以世代/修订号识别清空。保留首段延迟、失败也推进、不离线补打、同tick不偷跑新轨道。添加覆盖“伤害回调schedule”和“伤害回调discard”的有意义检查。

### R5 [P2] 炉心入口缺服务端主线程门
路径：src/main/java/com/bslx/XueXiMod/status/fire/furnace/FurnaceService.java的gate/reconcile及四个公共转换入口；准确方法定位由返修者在该文件确认，取证报告未提供该项行号。
gate只拒绝null和客户端，查询也可通过reconcile修改NBT。异步调用enter/cool等会改效果、属性、存档；cool还可能进入伤害与实体查询。火种与消费模块已有同线程检查，炉心没有，跨模块安全契约不一致。
最小修法：所有可能写炉心状态的公共路径要求server!=null且server.isSameThread()。转换返回明确拒绝结果，查询在不安全线程不得reconcile写入，采用无状态默认或明确拒绝；禁止自动异步调度后伪报成功。保持正常主线程事件行为。

### R6 [P2] 新伤害类型缺死亡翻译
路径：src/main/resources/assets/xueximod/lang/zh_cn.json、en_us.json；对应data/xueximod/damage_type/fire_resource.json与steam_burst.json的message_id。
两份语言补齐death.attack.fire_resource、.player、.item，以及death.attack.steam_burst、.player、.item共6键/语言，按Minecraft死亡消息参数语义填写。当前缺项会暴露原始键；.item需覆盖有自定义名称武器的来源。只补这些键，保留炉心及既有语言。

## 本轮不要求扩修的事项

- FireConsumption.schedule:23–70与detonate:73–103在契约内纯planner、同一主线程条件下，先preview并验证计划再consume、挂载；规划器主动改余额/轨道导致先扣后失败属于违反纯函数契约。本轮不把它升级成必须支持任意副作用回调的事务系统。保持并清楚标注纯函数约定；不要用无条件退款掩盖错误。
- 同一castId的新FireCastContext不共用账本是已明确的“同逻辑施法复用同一对象”契约，不要求全局永久map。调用方须遵守。
- 火种门禁/去重、封存伤害B×(1+A+F)、NBT保存已扣快照、不恢复后重扣，在取证范围内没有发现必须另返修的矛盾。burn事件重入按R4处理。
- 炉心forceAddEffect升级没有Remove事件，保留F；命令归一无限并清hidden链、空治愈列表抗牛奶、死亡/存活clone分流均有源码依据。注册入口和mixin声明存在；不据此宣称运行时织入成功。
- Resistance重定向确实指向自身getDamageAfterMagicAbsorb唯一hasEffect，保护附魔后续仍在。既有防御绕过标签未把fire_resource/steam_burst加入；两份新增标签replace=false。
- 旧未显式元素标注的playerAttack不自动接入，是已批准范围，不返修旧术式。生物转化未复制燃烧不是本规格明确要求的存活换维，暂不扩大范围。
- OriginalHumanoid的i/to是历史命名问题，不是本次compileJava三处错误来源，不在本批修。

## 验证边界与交回

统一编译者实际compileJava --offline --no-daemon退出1，compileTestJava未跑。6个纯逻辑/结构main检查508断言0失败，只证明这些检查本身，无法覆盖本轮编译错误、Mixin织入、NBT实体集成或真实hurt行为。本轮不作测试阶段验收。
返修应由Cursor唯一整合者完成上述跨模块项，避免多个作者抢mixins.json和语言文件；写入全部收束后再串行编译。下一次正式实现复审若启动算实现2/2，不属于本轮延续。
仍需游戏验证：mixin实际织入、4tick真实扣血及与普通命中交错、抗火/护甲/保护/抗性、牛奶、隐藏effect/命令、死亡/存活clone、卸载NBT恢复与蒸汽约3格。未运行游戏，未确认上述效果。
