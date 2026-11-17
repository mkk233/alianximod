你是主控调度的deepseek-flash专项只读读取代理。唯一GPT-6-astra已先给出调查切块，报告齐后再统一裁决。下面是已提供的带行号源码，只分析材料，禁止工具、禁止改文件、禁止编译、禁止另派代理。用户要求聚能只作用元素术式，剑术即使带元素也不允许；能量源输入量提高应带动范围/特效大小/持续时间，未聚能基线不变。不要按water_ring或water_mist id建议打补丁。区分确定事实/猜测/缺失材料，精确路径行号、调用链、字段公式、最早丢失倍率点、建议后续核实文件。不要输出长源码，不做最终设计，中文报告1000-1800字。根README不存在，不补造依据。
专项A：聚能输入底座。追踪装配到ChargeGate、SkillUsePacket/Plan/Runner、CastFrame/ReleaseDefinition。能量输入字段单位默认上下限，聚能怎样影响它，复制/快照是否丢失，伤害/几何/寿命倍率区别，普通与聚能具体数值路径，领域/护体调用是否把frame传给消费者。资格条件也记录给C块对照。

<source_file path="docs/逻辑修正.txt">
1: 落点指定术式应该是一个与锁定目标相同的修改锁定方式的术式而不是修改碰撞，落点锁定准确的说应该叫做空间锁定，具体表现为锁定准星最近实体周围3*3格子的空间，并在空间内随机位置生成装配链条上的下一个攻击术式
2: 
3: 这样拼接 落点指定 → 一分为三 → 三维交错 → 点火 → 空间切割后就更应该正确在锁定目标出生成三道可以点燃别人的空间切割，
4: 我们将术式详细分类一下
5: 索敌逻辑修改术式
6: 弹道逻辑修改术式
7: 规则逻辑修改术式
8: 动作术式
9: 附魔术式
10: 攻击术式
11: 分为这六种类型
12: 接下来我将说明这些术式类型的职责和具体表现
13: 
14: 索敌逻辑修改术式为术式添加索敌方式，优先锁定准星附近的优先锁定血最少的优先锁定与玩家距离最近的，锁定一片空间，锁定一个方块，等等效果这类术式不会改变攻击术式的弹道逻辑原本这个术式是什么样的发射方式在适用索敌后依然保持什么样的发射方式，但是会根据锁定的目标位置去发射而不是根据玩家的准星方向发射。落点指定与锁定目标都是这个分类
15: 
16: 弹道逻辑修改术式为术式修改弹道，攻击术式本身就拥有弹道这种术式会覆盖攻击术式原本的弹道并用新规定的弹道作为代替，部分剑术攻击的弹道无法修改，这会写在攻击术式上我们应该为攻击术式增加一个属性来判断是否可以修改弹道，弹道底座应该支持各类函数曲线与直线，三维的不同方向，例如螺旋弹道，抛物线弹道，直线弹道，垂直下落弹道，垂直上升弹道，旋转类弹道等。垂落术式和三维交错都是这个分类
17: 
18: 规则逻辑修改术式是为术式的释放新增一种规则，这类术式的功能更像是代码语法终端if for这类，他们可以互相嵌套不会相互影响同时作用也不会影响其后方拼接的其他术式效果，而且一起带给遇到的第一个攻击术式，一分为三，双重释放是这个分类
19: 
20: 动作术式的功能是完成一个动作并打出下一个遇到的攻击术式，如果前方有规则类术式则会改变这个逻辑变成完成一个动作动作过程中跟踪动作释放
21: 
22: 附魔术式的功能是为攻击附魔，如果只装配附魔术式那么将会直接给武器附魔让下一次攻击获得附魔属性加成以及效果，如果附魔后又攻击术式则会让附魔效果添加在攻击术式上，当有攻击术式存在时附魔术式不算做被释放而是被继承，当没有攻击术式时附魔术式会算作会被释放然后将效果继承在武器上
23: 
24: 攻击术式是最直接是生成一个碰撞盒加特效，有多个参数可调整之前我详细说过此处不再赘述你可以在代码中回忆一下，现在的攻击术式底座应该是正确的才对
25: 
26: 他们互相组合拼接组成技能就是我们这个模组的核心玩法了目前的拼接逻辑明显存在问题
27: 在设计上前三种逻辑修改术式应该可以互相嵌套无论摆放顺序如何最终结果都应该一致，三种逻辑修改应该同时生效并且互相影响无论玩家装配顺序如何这三种逻辑的互相影响的方案都统一为索敌逻辑影响弹道逻辑出现的方式规则逻辑记录并让收到索敌逻辑影响的弹道同步给收到规则影响的术式上链条上玩家可以随意摆放位置但一定要在后续有可释放的攻击术式情况下才结算，结算链条为索敌逻辑→弹道逻辑→规则逻辑→攻击术式，只有附魔术式的情况下就仅结算规则逻辑和附魔术式不结算索敌与弹道逻辑，同时出现附魔术式+加攻击术式的情况按照附魔术式的继承效果结算攻击术式。
28: 你给出的17条装配组合我一个个拆解
29: 先看第一个
30: 1.落点指定 → 一分为三 → 三维交错 → 点火 → 空间切割 这段装配的正确效果应该是，指定落点遇到一分为三生效让后续被分为三份的攻击术式按照落点指定的范围内随机生成，一分为三遇到三维交错调整了后续被分为三份的攻击术式的弹道，在三维空间随机角度随机高度出现分布范围依然遵守落点指定，遇到点火记录点火的数值和效果进入检测如果后续没有攻击术式则仅释放三次点火在武器上，实际计算结果如下三次点火叠加1*1.3*1.3并缩减0.3+0.3总计60%的冷却，由于产生了元素叠加反应所以附加的附魔伤害2点不会生效三次而是让武器基础攻击力计算一次N*1*1.3*1.3，再让附加的2点附魔伤害计算一次2*1*1.3*1.3,最后同时提升剑原本的攻击力和附魔产生的额外伤害后相加最终数值同时造成5秒燃烧，燃烧时间不叠加，这个效果会记录在释放时玩家手持的武器上，释放完后整个链条进入CD，此时被记录的落点指定和三维交错不会生效因为没有可以生效的对象他们必须要有攻击术式作为承载对象才会生效，记录在剑上的效果在玩家攻击后被消耗，玩家攻击前会一直存在剑上，此时如果释放其他附魔效果会覆盖掉原有的附魔数值与属性重新计算不会叠加。如果后续有攻击术式则完全不一样，点火会继承在攻击术式上让攻击术式获得2.附加伤害和燃烧效果因为不产生元素叠加反应因为三次点火分开使用到三道攻击术式上没有产生叠加的效果所以最终伤害计算就是(攻击术式伤害+附魔额外伤害)*3此时落点指定+一分为三+三维交错同时生效，弹道生成在指定落点周围3*3*3的空间内并根据交错的算法随机分布，总计出现三道攻击术式；同时你要记得就算玩家没有这样组合这些术式单独使用或者和其他术式组合也该正确生效
31: 后续的装配组合按照这个为基础逻辑参考你重新输出一份推演给我看看
</source_file>

<source_file path="docs/底座功能点.md">
1: # 底座功能点
2: 
3: 本文描述当前代码的拼接行为。新术式登记 `ChainRole`，走同一套收集和结算。不按术式 id 单独接落点、弹道、规则或附魔。`ChainRole` 只有这 8 个值，不新增。
4: 
5: 结算顺序是索敌 → 弹道 → 规则 → 攻击。修饰术式自己不造成这次攻击，必须挂在后面的攻击术式上才生效。离攻击更近的同类修饰覆盖更远的那条。
6: 
7: 魔力花费是 `ceil(基础伤害 × 2)`。规则术式花费为 0。附魔另按附魔定义计算。
8: 
9: ## 角色
10: 
11: | 角色 | 已登记术式 | 作用 |
12: | --- | --- | --- |
13: | `ATTACK` | 下面的攻击列表 | 这次释放的载体 |
14: | `ACTION` | 冲刺 | 动作。后面的攻击由它带出 |
15: | `SEEK_SPACE` | 落点指定 | 决定生成位置，不改弹道种类 |
16: | `SEEK_ENTITY` | 锁定实体 | 标记生物。可变弹道会朝标记飞 |
17: | `TRAJECTORY_DROP` | 垂落术式、风暴轨迹 | 覆盖可变弹道的曲线。两者靠 `Trajectory` 区分，不另开角色 |
18: | `TRAJECTORY_SCATTER` | 三维交错 | 改弹道散布，不做索敌 |
19: | `RULE_TRIPLE` | 一分为三 | 把后面第一个攻击复制三份，伤害汇总 |
20: | `RULE_DUAL` | 双重释放 | 带上下一个，再加后面两个；实际攻击按顺序放 |
21: 
22: `trajectoryMutable == false` 的斩击只改生成位置，形状不变。`trajectoryMutable == true` 的攻击可以改曲线。
23: 
24: ## 索敌
25: 
26: **锁定实体。** 准星方向 20 格内、离视线 8 格内最近的生物。找到则标记并发光，找不到则清标记。可变弹道朝该生物飞行。生成位置不因这次索敌改变。
27: 
28: **落点指定，用在可变弹道上。** 边长 3 格的立方体里均匀取点。中心是准星射线打到的方块，射线最长 32 格；方块前 1.5 格内有生物时，改用该生物中心。攻击自己的发射方式保持不变，只是从这些点发出。
29: 
30: **落点指定，用在斩击上。** 仍用斩击锚点：视线 8 格内离准星线段最近的生物中心，没有生物则是眼前 4 格。刀面的高度、倾角和旋转仍走原来的斩击散布。次元斩走这条，不走弹丸的立方体。
31: 
32: ## 弹道
33: 
34: 同一条链上有多条弹道术式时，离攻击更近的那条生效。
35: 
36: **垂落。** 只改高度，水平位置留在已经选定的点上。从该点上方 8 格出现，再按 0.08 格/tick² 自由落体。不沿准星平移，也不把所有弹丸收到同一个水平坐标。
37: 
38: **风暴轨迹。** 龙卷风。落点是准星打到的位置；光线上有生物时用该生物中心。没有装配落点指定时也会取这个落点。弹丸从落点上方 12 格、半径 5 格的圆周出发，转 3 圈并收到落点。多发按黄金角错开，铺在同一条漏斗上。寿命的前三分之二用来旋转，之后停在落点，直到寿命结束。持续术式跟着这条路径移动，到达后留在落点继续生效。速度为 0 的持续术式也会转。生成点在螺旋顶部，不先放在落点上，避免起飞前打中目标。
39: 
40: **三维交错。** 只改弹道。前面没有其他索敌时，在玩家身体周围 3 格立方体内取点，低于脚底的点抬到脚底，再沿随机三维方向射出。和落点指定一起用时，位置留在落点立方体里，方向改为随机。斩击不套这套随机向量。
41: 
42: 立方体取点先把序号打散再抽样。连续序号不能直接交给 `RandomSource`，否则第一轴几乎不动，点会排成一条线。
43: 
44: ## 规则与附魔
45: 
46: **一分为三。** 后面第一个攻击执行三次。三份共用一本账，大约 6 tick 后一次结算，伤害乘命中份数。不改无敌帧。
47: 
48: **双重释放。** 带上下一个术式，再加它后面两个。有伤害的术式按装配顺序逐个释放，间隔 10 tick。修饰不占这一拍。只有一个有伤害的术式时不多放一份。同组仍共用这次的弹道，下一发不出生在上一发的命中点。魔力按每个实际放出的攻击自己计算，不再把同一个攻击的波次乘 2。
49: 
50: **聚能。** 规则术式，自己不攻击。把后面这次攻击的能量乘 2，碰撞体积跟着变大。热光束的截面按同一倍率放大，能量为 1 时仍是信标粗细；水带宽度也乘这个倍率。多挂一条再乘一次。双重释放带出的几发攻击共用这一次加成。伤害、冷却、持续、距离、搜索和速度不乘。不另开 `ChainRole`，加成写在 `chainEnergyScale()` 上。
51: 
52: **附魔。** 不占规则释放位，记在后面第一个攻击上。没有攻击时记在主手，下一次普通攻击消耗。点火是附魔，不是爆炸。
53: 
54: ## 命中
55: 
56: 攻击体积由 `TechniqueHitSpec.spawn` 生成，命中在 `TechniqueHitEntity` 上判定。每发各自记录已经打中的目标，同一发不会对同一目标重复结算。
57: 
58: 能量层是所有术式共用的来源大小，写在释放定义的 `energyScale()` 上，默认 1。链上的聚能再乘 `chainEnergyScale()`。碰撞体积的倍率是能量乘上附魔里已经含有元素反应的半径。长、宽、高和弹丸模型一起乘。倍率为 1 时尺寸不变。爆燃的爆炸半径用同一个倍率，默认仍是 3 格。伤害、冷却、持续时间、距离、搜索和速度不乘能量。
59: 
60: 潮湿是第一种属性状态药水。精确值 0 到 100，由术式调用 `WetnessState.grant` 累加。1 到 25、26 到 50、51 到 75、76 到 100 四档，每次成功施加都刷新 50 秒。到期或效果被移除时数值归零。站在水里不会自动挂上。现有攻击没有调用它，所以旧术式的命中不变。
61: 
62: 龙卷风的落点如果在目标身上，不能把实体先放在落点再拉到天上。那样会在飞行前结算，并把真正落下时的命中用掉。
63: 
64: 水势记在玩家身上，和魔力分开，0 到 100，多出来的丢掉。出手时若这发声明了消耗，先扣预留；没打中也扣。实际打出的伤害和实际受到的伤害随后再加。同一发不能花自己刚加上的水势。蓄力被打断时预留退回，不放收招。只有声明了蓄力拍数的术式才等待，旧术式仍是立刻放出。
65: 
66: 魔力仍按伤害向上取整乘 2。释放定义若写了 `explicitManaCost()`，这发改用那个数，防御和治疗不会因为伤害是 0 就变成免费。
67: 
68: 场地是高级附魔，可以单独放。锚点钉在施法时的身体位置，索敌和弹道不挪这个点，只记在场地上。一分为三直接生成三块，生成费先按块数收齐，维持费各自再扣；扣失败、死亡、下线或换维度，这块就拆掉。之后在场地里放的术式，伤害按身体同时被几块包住来乘：一块是 1 倍，两块是 2 倍。索敌、弹道、治疗、牵引、潮湿、灭火各只套用一份。灭火只熄灭生物身上的火。凝结障碍走原来的命中体，挡住移动，不改方块和流体。幻影只有生成契约，没有分身。
69: 
70: 旧术式不填这些字段时，消耗、弹道和伤害算法保持原样。人站进场地后再放，才会吃到上面的场地规则。
71: 
72: 留下的水系术式是水刃、水幕、水雾、水环、深海重压、让锋、缺月、垂丝，复杂度都是 0，登录就会学会。水刃、水幕、水雾、水环、深海重压走 `WaterFormEffect`。缺月走函数曲线。垂丝是水帘，命中盒子只覆盖当前还在的那段水，聚能只加宽加厚。让锋和缺月是水属性剑术，只能装进剑技，法杖不能装配也不能释放。让锋先在身前铺开水面，再从中间劈开；水分开没有伤害，只有中间那一刀，伤害 40，冷却 600 tick。水幕会在身前放一块会挡人的水墙，并削去站在场地里受到的一部分伤害，削掉的数值记进水势。深海重压保持删掉前的效果：半径 5、持续 100 tick、减速、潮湿、压强，到期中心爆发 14。潮印仍是灭火附魔，断潮仍是不可变的水切。缠绕也还没有。
73: 
74: `/shu entities <数量>` 只放大攻击体积和幻剑的生成次数，范围 1 到 4096，默认 1。不放大魔力、冲刺、剑来和万剑归宗。额外副本使用不同的结算序号，避免被汇总账当成同一发。
75: 
76: ## 攻击载体
77: 
78: 可变弹道：奥术飞弹、热光束、持续试射、金针、寄生种子、幻剑术、火球、爆燃。
79: 
80: 不可变弹道：刺击、横劈、竖劈、空间切割、断潮、岩槌。
81: 
82: 操纵剑实体：剑来、万剑归宗。万剑归宗拒绝和其它术式混在同一条链里。幻剑仍要手持剑才生成。
83: 
84: 火属性零件是火球、爆燃、风暴轨迹。下面这些名字不注册，由零件组合出来：
85: 
86: | 想要的效果 | 装配 |
87: | --- | --- |
88: | 火焰风暴 | 风暴轨迹 + 火球 |
89: | 火雨 | 垂落 + 落点指定 + 一分为三和/或三维交错 + 火球 |
90: | 陨石 | 垂落 + 爆燃 |
91: | 爆炸 | 爆燃 |
92: 
93: 火球：伤害 5，速度 1.2，40 tick，命中点燃 4 秒。爆燃：伤害 8，速度 0.6，40 tick；撞到生物或方块立刻爆炸，否则到期爆炸。半径 3 格，只爆一次，点燃 4 秒。
94: 
95: 次元斩仍是：落点指定 → 一分为三 → 三维交错 → 空间切割。需要点火时把点火挂在空间切割上。
96: 
97: ## 测试指令
98: 
99: 权限 2。
100: 
101: - `/shu mana <上限>`：设置魔力上限并补满。法杖加成仍额外加上。
102: - `/shu entities <数量>`：技能生成实体的全局倍率。
103: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/network/SkillUsePacket.java">
1: package com.bslx.XueXiMod.network;
2: 
3: import com.bslx.XueXiMod.debug.DebugLog;
4: import com.bslx.XueXiMod.item.SkillCarrier;
5: import com.bslx.XueXiMod.item.StaffData;
6: import com.bslx.XueXiMod.item.StaffItem;
7: import com.bslx.XueXiMod.item.StaffSkills;
8: import com.bslx.XueXiMod.player.MagicPool;
9: import com.bslx.XueXiMod.player.ManaPaymentReceipt;
10: import com.bslx.XueXiMod.player.PlayerBoundSkills;
11: import com.bslx.XueXiMod.player.PlayerShuData;
12: import com.bslx.XueXiMod.shu.ElementReaction;
13: import com.bslx.XueXiMod.shu.Technique;
14: import com.bslx.XueXiMod.shu.TechniqueType;
15: import com.bslx.XueXiMod.shu.technique.EnchantmentAccumulator;
16: import com.bslx.XueXiMod.shu.technique.EnchantmentDefinition;
17: import com.bslx.XueXiMod.shu.technique.EnchantmentRegistry;
18: import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
19: import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
20: import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
21: import com.bslx.XueXiMod.shu.technique.TechniqueEffectRegistry;
22: import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
23: import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
24: import com.bslx.XueXiMod.shu.technique.chain.TechniqueChain;
25: import com.bslx.XueXiMod.shu.technique.effects.LockTargetService;
26: import com.bslx.XueXiMod.shu.technique.enchant.EnchantBonus;
27: import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
28: import com.bslx.XueXiMod.shu.technique.enchant.WeaponEnchant;
29: import com.bslx.XueXiMod.shu.technique.release.CastFrame;
30: import com.bslx.XueXiMod.shu.technique.release.ChainRole;
31: import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
32: import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
33: import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
34: import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
35: import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalIntent;
36: import com.bslx.XueXiMod.shu.technique.release.entity.HostCast;
37: import com.bslx.XueXiMod.shu.technique.release.water.CastSource;
38: import com.bslx.XueXiMod.shu.technique.release.water.WaterBeatBridge;
39: import com.bslx.XueXiMod.shu.technique.release.entity.HostGroupCommit;
40: import com.bslx.XueXiMod.shu.technique.release.field.FieldSpawner;
41: import com.bslx.XueXiMod.shu.technique.release.guard.GuardStates;
42: import com.bslx.XueXiMod.shu.technique.release.momentum.ChargeGate;
43: import com.bslx.XueXiMod.shu.technique.release.momentum.WaterMomentum;
44: import com.bslx.XueXiMod.shu.technique.sword.SwordEntity;
45: import com.bslx.XueXiMod.shu.technique.release.ReleaseTag;
46: import com.bslx.XueXiMod.skill.Skill;
47: import net.minecraft.network.FriendlyByteBuf;
48: import net.minecraft.network.chat.Component;
49: import net.minecraft.server.level.ServerLevel;
50: import net.minecraft.server.level.ServerPlayer;
51: import net.minecraft.world.InteractionHand;
52: import net.minecraft.world.item.ItemStack;
53: import net.minecraft.world.item.SwordItem;
54: import net.minecraft.world.phys.Vec3;
55: import net.minecraftforge.network.NetworkEvent;
56: import net.minecraftforge.network.simple.SimpleChannel;
57: 
58: import net.minecraft.world.effect.MobEffectInstance;
59: import net.minecraft.world.effect.MobEffects;
60: import net.minecraft.world.entity.LivingEntity;
61: import net.minecraft.world.entity.ai.attributes.Attributes;
62: 
63: import java.util.ArrayList;
64: import java.util.HashSet;
65: import java.util.List;
66: import java.util.Set;
67: import java.util.UUID;
68: import java.util.concurrent.atomic.AtomicBoolean;
69: import java.util.concurrent.atomic.AtomicInteger;
70: import java.util.concurrent.atomic.AtomicReference;
71: import java.util.function.Consumer;
72: import java.util.function.Supplier;
73: 
74: public class SkillUsePacket {
75:     private final int slotIndex;
76: 
77:     public SkillUsePacket(int slotIndex) {
78:         this.slotIndex = slotIndex;
79:     }
80: 
81:     public void encode(FriendlyByteBuf buf) {
82:         buf.writeInt(slotIndex);
83:     }
84: 
85:     public static SkillUsePacket decode(FriendlyByteBuf buf) {
86:         return new SkillUsePacket(buf.readInt());
87:     }
88: 
89:     public void handle(Supplier<NetworkEvent.Context> context) {
90:         context.get().enqueueWork(() -> {
91:             ServerPlayer player = context.get().getSender();
92:             if (player == null) return;
93: 
94:             ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
95:             boolean staff = StaffItem.holds(mainHand);
96:             double cooldownReduction = 0.0;
97:             Double attributeDamageBonus = null;
98:             if (staff) {
99:                 if (slotIndex != 0) return;
100:                 StaffData data = ((StaffItem) mainHand.getItem()).data();
101:                 cooldownReduction = data.cooldownReduction();
102:                 attributeDamageBonus = data.attributeDamageBonus();
103:             } else {
104:                 if (slotIndex < 0 || slotIndex > 3) return;
105:                 if (!SkillCarrier.canRelease(mainHand)) {
106:                     Skill peek = PlayerBoundSkills.get(player, slotIndex);
107:                     if (peek == null || !SkillSegmentMath.allowsEmptyHandSwordCast(player, peek)) {
108:                         return;
109:                     }
110:                 }
111:             }
112: 
113:             Skill skill = staff ? StaffSkills.asSkill(mainHand) : PlayerBoundSkills.get(player, slotIndex);
114:             if (skill == null) {
115:                 player.sendSystemMessage(Component.literal("§c这个槽位还没有装配剑技"));
116:                 return;
117:             }
118:             String heldSwordRefusal = HeldSwordGate.refusal(mainHand, skill.getTechniques());
119:             if (heldSwordRefusal != null) {
120:                 HeldSwordGate.tellLimited(player, heldSwordRefusal);
121:                 return;
122:             }
123:             if (staff) {
124:                 for (Technique listed : skill.getTechniques()) {
125:                     if (listed != null && listed.getType() == TechniqueType.SWORD) {
126:                         player.sendSystemMessage(Component.literal("§c剑术不能由法杖释放"));
127:                         return;
128:                     }
129:                 }
130:             }
131: 
132:             if (CastCooldown.busy(player, mainHand, slotIndex)) {
133:                 DebugLog.log("释放中断：槽" + (slotIndex + 1) + " 链条尚未结束");
134:                 return;
135:             }
136:             DebugLog.log("释放槽" + (slotIndex + 1) + " " + skill.getName());
137: 
138:             CastTiming timing = executeSkill(player, skill, mainHand, slotIndex, cooldownReduction, attributeDamageBonus);
139:             if (!timing.accepted()) {
140:                 return;
141:             }
142:             if (!timing.settlesLater()) {
143:                 CastCooldown.mark(player, mainHand, slotIndex, timing.replayTick());
144:                 DebugLog.log("释放时刻 开始=" + timing.startedTick() + " 末段=" + timing.finishedTick() + " 重放=" + timing.replayTick());
145:             } else {
146:                 DebugLog.log("双重释放已接受，冷却等到这一发结束");
147:             }
148: 
149:             PlayerShuData data = PlayerShuData.get(player);
150:             boolean leveledUp = false;
151:             for (Technique technique : timing.fired()) {
152:                 data.incrementUsage(technique.getId());
153:                 if (data.addUsageAndTryLevelUp(technique.getType(), player)) {
154:                     leveledUp = true;
155:                 }
156:             }
157:             PlayerShuData.save(player, data);
158: 
159:             if (leveledUp) {
160:                 com.bslx.XueXiMod.player.DomainLearning.offerReady(player);
161:                 com.bslx.XueXiMod.network.ModNetwork.sendToPlayer(
162:                     player, new com.bslx.XueXiMod.network.ShuSyncPacket(data));
163:             }
164:         });
165:         context.get().setPacketHandled(true);
166:     }
167: 
168:     private CastTiming executeSkill(ServerPlayer player, Skill skill, ItemStack weapon, int slot,
169:                              double cooldownReduction, Double attributeDamageBonus) {
170:         double weaponDamage = player.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
171:         float weaponBonus = (float) Math.max(0, weaponDamage - 1.0);
172: 
173:         List<Technique> assembled = skill.getTechniques();
174:         if (SkillSegmentMath.refusesMixedChain(assembled)) {
175:             player.sendSystemMessage(Component.literal("§c这个操纵术式不能和其他术式装配"));
176:             return CastTiming.rejected();
177:         }
178:         String heldSwordRefusal = HeldSwordGate.refusal(weapon, assembled);
179:         if (heldSwordRefusal != null) {
180:             HeldSwordGate.tellLimited(player, heldSwordRefusal);
181:             return CastTiming.rejected();
182:         }
183:         CastSource castSource = WaterBeatBridge.capture(player, slot);
184: 
185:         List<List<Integer>> chainSteps = ChainSteps.list(skill);
186:         boolean stepped = !chainSteps.isEmpty();
187:         int stepCooldown = 0;
188:         String stepSignature = "";
189:         int stepCount = chainSteps.size();
190:         if (stepped) {
191:             stepSignature = ChainSteps.signature(skill);
192:             int cursor = ChainProgress.current(player, weapon, slot, stepSignature, stepCount);
193:             List<Integer> stepIndexes = chainSteps.get(cursor);
194:             stepCooldown = ChainSteps.cooldown(skill, stepIndexes, cooldownReduction);
195:             skill = ChainSteps.slice(skill, stepIndexes);
196:         }
197:         final int frozenStepCooldown = stepCooldown;
198: 
199:         List<Technique> techniques = skill.getTechniques();
200:         final Skill replaySkill = skill;
201:         List<ChainSegment> segments = TechniqueChain.split(skill);
202: 
203:         boolean hasPlayable = false;
204:         for (ChainSegment segment : segments) {
205:             Technique listed = techniques.get(segment.actionIndex());
206:             if (SkillSegmentMath.isRemovedRule(listed.getId()) || SkillSegmentMath.isChainModifier(listed)) {
207:                 continue;
208:             }
209:             hasPlayable = true;
210:             break;
211:         }
212:         if (!stepped && segments.isEmpty() && legalEnchantChain(techniques)) {
213:             return imprintEnchantChain(player, skill, techniques, cooldownReduction);
214:         }
215:         boolean hasSwordManip = SkillSegmentMath.hasSwordManipulation(techniques);
216:         if (!hasPlayable) {
217:             player.sendSystemMessage(Component.literal("§c当前剑技没有可释放的伤害术式"));
218:             return CastTiming.rejected();
219:         }
220:         if (hasSwordManip) {
221:             boolean holdingSword = weapon.getItem() instanceof SwordItem;
222:             boolean worldSword = SkillSegmentMath.hasPlantedWorldSword(player);
223:             if (!holdingSword && !worldSword) {
224:                 player.sendSystemMessage(Component.literal("§c需要手持剑"));
225:                 return CastTiming.rejected();
226:             }
227:         }
228:         String contactRefusal = ContactCast.refusal(skill, segments);
229:         if (contactRefusal != null) {
230:             player.sendSystemMessage(Component.literal("§c" + contactRefusal));
231:             return CastTiming.rejected();
232:         }
233:         PlayerShuData casterData = PlayerShuData.get(player);
234:         for (Technique listed : techniques) {
235:             ReleaseDefinition listedDefinition = ReleaseRegistry.get(listed.getId());
236:             if (listedDefinition != null && listedDefinition.domain() != null && !casterData.isUnlocked(listed)) {
237:                 player.sendSystemMessage(Component.literal("§c还没有学会释放" + listed.getName()));
238:                 return CastTiming.rejected();
239:             }
240:         }
241: 
242:         CompiledReleaseStep dualPlan = CompiledReleaseStep.compile(skill, techniques, segments, cooldownReduction);
243:         if (dualPlan != null) {
244:             if (dualPlan.chargeRefusal != null) {
245:                 player.sendSystemMessage(Component.literal("§c" + dualPlan.chargeRefusal));
246:                 return CastTiming.rejected();
247:             }
248:             return CastExecutionSession.open(player, weapon, slot, castSource, skill, techniques, segments, dualPlan, weaponBonus,
249:                     stepSignature, stepCount);
250:         }
251: 
252:         final boolean conditionalChain = chainHasConditional(techniques, segments);
253:         final String conditionalLockKey = conditionalChain ? CastCooldown.carrierKey(player, weapon, slot) : null;
254:         final AtomicBoolean conditionalLockReleased = new AtomicBoolean(!conditionalChain);
255:         if (conditionalChain && !CastCooldown.acquire(conditionalLockKey)) {
256:             return CastTiming.rejected();
257:         }
258: 
259:         int cost = SkillChainPlan.pressCommitCost(techniques, segments);
260:         if (!MagicPool.trySpend(player, cost)) {
261:             releaseConditionalLock(conditionalLockKey, conditionalLockReleased);
262:             return CastTiming.insufficient();
263:         }
264:         ManaPaymentReceipt pressReceipt = MagicPool.lastReceipt(player.getUUID());
265:         if (cost > 0) {
266:             MagicPool.showBalance(player);
267:         }
268: 
269:         final HostCast conditionalCast;
270:         try {
271:             conditionalCast = openConditionalCast(player, techniques, segments, castSource);
272:         } catch (RuntimeException failure) {
273:             releaseConditionalLock(conditionalLockKey, conditionalLockReleased);
274:             throw failure;
275:         }
276:         final AtomicInteger pendingConditional = new AtomicInteger();
277:         final AtomicInteger conditionalCommitted = new AtomicInteger();
278:         final AtomicBoolean conditionalFailed = new AtomicBoolean();
279:         final AtomicBoolean conditionalAborted = new AtomicBoolean();
280:         final AtomicBoolean handedOff = new AtomicBoolean();
281:         final AtomicBoolean parentSettled = new AtomicBoolean();
282:         final AtomicInteger succeededConditionalScaled = new AtomicInteger();
283:         final AtomicInteger capturedLastStart = new AtomicInteger();
284:         final AtomicReference<Technique> capturedTail = new AtomicReference<>();
285:         final AtomicBoolean enchantApplied = new AtomicBoolean();
286:         final Set<String> recordedUsage = new HashSet<>();
287:         final Set<String> succeededConditionalIds = new HashSet<>();
288:         final List<Technique> ordinarySucceeded = new ArrayList<>();
289:         final List<UncommittedShare> conditionalClaims = new ArrayList<>();
290:         final String frozenSignature = stepSignature;
291:         final int openedTick = player.getServer().getTickCount();
292:         int lastStartTick = openedTick;
293:         Technique tailTechnique = null;
294:         int failedFromIndex = -1;
295:         try {
296:         ReleaseBatch.open(player, skill);
297:         int startTick = openedTick;
298:         lastStartTick = startTick;
299:         ChainSegment previous = null;
300:         boolean removedRuleWarned = false;
301:         boolean enchantImprinted = false;
302:         boolean stepFailed = false;
303: 
304:         int segmentIndex = 0;
305:         while (segmentIndex < segments.size()) {
306:             ChainSegment segment = segments.get(segmentIndex);
307:             Technique action = techniques.get(segment.actionIndex());
308: 
309:             int gap = 0;
310:             if (previous != null && !stepped) {
311:                 gap = SkillSegmentMath.scaleCooldown(TechniqueChain.gapBefore(segment, previous, skill), cooldownReduction);
312:                 startTick += gap;
313:             }
314:             lastStartTick = startTick;
315:             tailTechnique = action;
316: 
317:             if (SkillSegmentMath.isRemovedRule(action.getId())) {
318:                 if (!removedRuleWarned) {
319:                     removedRuleWarned = true;
320:                     player.sendSystemMessage(Component.literal("§c链里有已移除的术式，请重新装配"));
321:                 }
322:                 segmentIndex += 1;
323:                 previous = segment;
324:                 continue;
325:             }
326: 
327:             if (action.isEnchantment()) {
328:                 if (!enchantImprinted) {
329:                     SkillChainPlan.imprintEnchant(player, skill, techniques, 1);
330:                     enchantImprinted = true;
331:                     enchantApplied.set(true);
332:                 }
333:                 tailTechnique = action;
334:                 segmentIndex += 1;
335:                 previous = segment;
336:                 continue;
337:             }
338: 
339:             ChainRole role = SkillSegmentMath.roleOf(action);
340:             ReleaseDefinition actionDef = ReleaseRegistry.get(action.getId());
341:             if (actionDef != null && actionDef.manipulatesSwordEntities()) {
342:                 ChainScheduler.schedule(player.getServer(), startTick,
343:                         SkillSegmentRunner.buildSegmentRunner(player, action, techniques, segment.actionIndex(), weapon, 0f, false,
344:                                 EnchantmentResolution.empty(), null, false, false));
345:                 if (conditionalChain) {
346:                     rememberOrdinarySuccess(ordinarySucceeded, action);
347:                 }
348:                 tailTechnique = action;
349:                 segmentIndex += 1;
350:                 previous = segment;
351:                 continue;
352:             }
353:             if (role == ChainRole.ACTION) {
354:                 SkillChainPlan.ModifierPlan plan = SkillChainPlan.collectModifiers(segments, techniques, segmentIndex + 1);
355:                 if (plan.attackSegment >= 0) {
356:                     SkillChainPlan.ModifierPlan captured = plan;
357:                     ServerLevel castLevel = player.serverLevel();
358:                     int castEpoch = ChargeGate.epoch(player.getUUID());
359:                     final Skill castSkill = skill;
360:                     if (captured.dualRuleSegment < 0) {
361:                         int attackShare = SkillSegmentMath.segmentManaCost(techniques, segments.get(captured.attackSegment));
362:                         int memberCount = Math.max(0, captured.volleys) * Math.max(0, captured.copies);
363:                         UncommittedShare.Pool reservedShares = UncommittedShare.reserve(pressReceipt, attackShare, memberCount);
364:                         AttackBudgetWindow window = AttackBudgetWindow.open(player, castLevel, castEpoch, reservedShares,
365:                                 castSkill, techniques, segments, captured, weaponBonus, castSource);
366:                         ChainScheduler.schedule(player.getServer(), startTick, () -> {
367:                             if (!window.ownerPresent() || !HeldSwordGate.allows(action, player.getMainHandItem())) {
368:                                 window.finish();
369:                                 return;
370:                             }
371:                             if (!SkillSegmentRunner.playAction(player, action, techniques, segment.actionIndex(), weapon, window)) {
372:                                 window.finish();
373:                             }
374:                         });
375:                     } else {
376:                         ChainScheduler.schedule(player.getServer(), startTick,
377:                                 SkillSegmentRunner.buildSegmentRunner(player, action, techniques, segment.actionIndex(), weapon, 0f, false,
378:                                         EnchantmentResolution.empty(), position -> {
379:                                             if (!player.isAlive() || player.level() != castLevel
380:                                                     || !ChargeGate.same(player.getUUID(), castEpoch)) {
381:                                                 return;
382:                                             }
383:                                             SkillChainPlan.firePlan(player, castSkill, techniques, segments, captured, weaponBonus,
384:                                                     player.getServer().getTickCount(), pressReceipt, castSource);
385:                                         },
386:                                         false, false));
387:                     }
388:                     if (conditionalChain) {
389:                         rememberOrdinarySuccess(ordinarySucceeded, action);
390:                     }
391:                     tailTechnique = action;
392:                     segmentIndex = plan.resume;
393:                     previous = segment;
394:                     continue;
395:                 }
396:             }
397:             if (SkillSegmentMath.isChainModifier(action)) {
398:                 SkillChainPlan.ModifierPlan plan = SkillChainPlan.collectModifiers(segments, techniques, segmentIndex);
399:                 if (plan.attackSegment < 0) {
400:                     if (plan.dualRuleSegment >= 0) {
401:                         int firedUntil = SkillChainPlan.fireDualSequence(player, skill, techniques, segments, plan, weaponBonus, startTick, castSource);
402:                         if (firedUntil < 0) {
403:                             stepFailed = true;
404:                             failedFromIndex = segmentIndex;
405:                             break;
406:                         }
407:                         if (firedUntil > lastStartTick) {
408:                             lastStartTick = firedUntil;
409:                         }
410:                     } else if (!ContactCast.keptForContact(techniques, segments, segmentIndex)) {
411:                         SkillChainPlan.imprintEnchant(player, skill, techniques, plan.copies);
412:                         enchantImprinted = true;
413:                         enchantApplied.set(true);
414:                     }
415:                     segmentIndex = Math.max(plan.resume, segmentIndex + 1);
416:                     previous = segment;
417:                     continue;
418:                 }
419:                     int firedUntil = SkillChainPlan.firePlan(player, skill, techniques, segments, plan, weaponBonus, startTick, pressReceipt, castSource);
420:                 if (firedUntil < 0) {
421:                     stepFailed = true;
422:                     failedFromIndex = segmentIndex;
423:                     break;
424:                 }
425:                 if (firedUntil > lastStartTick) {
426:                     lastStartTick = firedUntil;
427:                 }
428:                 Technique closing = SkillChainPlan.closingAttack(techniques, segments, plan);
429:                 if (closing != null) {
430:                     tailTechnique = closing;
431:                     if (conditionalChain) {
432:                         rememberOrdinarySuccess(ordinarySucceeded, closing);
433:                     }
434:                 }
435:                 segmentIndex = plan.resume;
436:                 previous = segments.get(plan.attackSegment);
437:                 continue;
438:             }
439: 
440:             if (actionDef != null && actionDef.domain() != null && action.getBaseDamage() <= 0) {
441:                 int domainCopies = SkillChainPlan.copiesBefore(segments, techniques, segmentIndex);
442:                 EnchantmentSnapshot domainSnapshot = ContactCast.snapshot(skill, segment, player);
443:                 if (!com.bslx.XueXiMod.shu.technique.release.domain.DomainService.tryWave(
444:                         player, action, actionDef.domain(), domainCopies, domainSnapshot)) {
445:                     stepFailed = true;
446:                     failedFromIndex = segmentIndex;
447:                     break;
448:                 }
449:                 if (conditionalChain) {
450:                     rememberOrdinarySuccess(ordinarySucceeded, action);
451:                 }
452:                 tailTechnique = action;
453:                 segmentIndex += 1;
454:                 previous = segment;
455:                 continue;
456:             }
457: 
458:             if (actionDef != null && actionDef.barrier() != null && action.getBaseDamage() <= 0) {
459:                 int barrierCopies = SkillChainPlan.copiesBefore(segments, techniques, segmentIndex);
460:                 EnchantmentSnapshot barrierSnapshot = ContactCast.snapshot(skill, segment, player);
461:                 if (!com.bslx.XueXiMod.shu.technique.release.barrier.BarrierService.tryPlace(
462:                         player, actionDef.barrier(), barrierCopies, barrierSnapshot)) {
463:                     stepFailed = true;
464:                     failedFromIndex = segmentIndex;
465:                     break;
466:                 }
467:                 if (conditionalChain) {
468:                     rememberOrdinarySuccess(ordinarySucceeded, action);
469:                 }
470:                 tailTechnique = action;
471:                 segmentIndex += 1;
472:                 previous = segment;
473:                 continue;
474:             }
475: 
476:             if (actionDef != null && actionDef.guard() != null && action.getBaseDamage() <= 0) {
477:                 long waveKey = ((long) openedTick << 8) + segmentIndex;
478:                 if (!GuardStates.schedule(player, actionDef.guard(), startTick, waveKey)) {
479:                     stepFailed = true;
480:                     failedFromIndex = segmentIndex;
481:                     break;
482:                 }
483:                 if (conditionalChain) {
484:                     rememberOrdinarySuccess(ordinarySucceeded, action);
485:                 }
486:                 EnchantmentSnapshot guardSnapshot = ContactCast.snapshot(skill, segment, player);
487:                 if (guardSnapshot != null) {
488:                     GuardStates.bindContact(player, guardSnapshot);
489:                 }
490:                 tailTechnique = action;
491:                 segmentIndex += 1;
492:                 previous = segment;
493:                 continue;
494:             }
495: 
496:             if (actionDef != null && action.getBaseDamage() <= 0f && actionDef.conditionalEffect() != null) {
497:                 int segmentShare = SkillSegmentMath.segmentManaCost(techniques, segment);
498:                 EnchantmentSnapshot conditionalSnapshot = ContactCast.snapshot(skill, segment, player);
499:                 UUID operationId = conditionalCast == null
500:                         ? null
501:                         : ConditionalIntent.operationId(conditionalCast.tokenId(), segmentIndex, 0, null);
502:                 UncommittedShare claim = UncommittedShare.open(pressReceipt, segmentShare);
503:                 conditionalClaims.add(claim);
504:                 final int scaledCooldown = SkillSegmentMath.scaleCooldown(action.getBaseCooldown(), cooldownReduction);
505:                 if (startTick <= openedTick) {
506:                     ReleaseOutcome outcome = SkillSegmentRunner.commitConditional(player, actionDef, operationId, conditionalSnapshot);
507:                     if (!outcome.committed()) {
508:                         claim.cancel(player);
509:                         int laterCost = SkillChainPlan.pressCommitCost(techniques, segments, segmentIndex + 1);
510:                         if (laterCost > 0 && pressReceipt != null) {
511:                             pressReceipt.refundShare(player, laterCost);
512:                         }
513:                         if (!outcome.reason().isEmpty()) {
514:                             player.sendSystemMessage(Component.literal("§c" + outcome.reason()));
515:                         }
516:                         conditionalFailed.set(true);
517:                         break;
518:                     }
519:                     claim.commitRetained();
520:                     conditionalCommitted.incrementAndGet();
521:                     succeededConditionalIds.add(action.getId());
522:                     succeededConditionalScaled.updateAndGet(alreadyScaled -> alreadyScaled > scaledCooldown ? alreadyScaled : scaledCooldown);
523:                 } else {
524:                     pendingConditional.incrementAndGet();
525:                     if (conditionalCast != null) {
526:                         conditionalCast.hold();
527:                     }
528:                     ServerLevel conditionalLevel = player.serverLevel();
529:                     int conditionalEpoch = ChargeGate.epoch(player.getUUID());
530:                     ReleaseDefinition scheduledDefinition = actionDef;
531:                     Technique scheduledTechnique = action;
532:                     AtomicBoolean entered = new AtomicBoolean();
533:                     ChainScheduler.schedule(player.getServer(), startTick, () -> {
534:                         if (!entered.compareAndSet(false, true)) {
535:                             return;
536:                         }
537:                         boolean committedNow = false;
538:                         try {
539:                             if (conditionalAborted.get() || conditionalFailed.get() || !player.isAlive()
540:                                     || player.level() != conditionalLevel || !ChargeGate.same(player.getUUID(), conditionalEpoch)) {
541:                                 if (!claim.kept()) {
542:                                     claim.cancel(player);
543:                                 }
544:                                 return;
545:                             }
546:                             ReleaseOutcome outcome = SkillSegmentRunner.commitConditional(
547:                                     player, scheduledDefinition, operationId, conditionalSnapshot);
548:                             if (outcome.committed()) {
549:                                 claim.commitRetained();
550:                                 committedNow = true;
551:                                 conditionalCommitted.incrementAndGet();
552:                                 succeededConditionalIds.add(scheduledTechnique.getId());
553:                                 succeededConditionalScaled.updateAndGet(alreadyScaled -> alreadyScaled > scaledCooldown ? alreadyScaled : scaledCooldown);
554:                             } else {
555:                                 claim.cancel(player);
556:                                 conditionalFailed.set(true);
557:                                 if (!outcome.reason().isEmpty()) {
558:                                     player.sendSystemMessage(Component.literal("§c" + outcome.reason()));
559:                                 }
560:                             }
561:                         } catch (RuntimeException failure) {
562:                             if (!committedNow && !claim.kept()) {
563:                                 claim.cancel(player);
564:                                 conditionalFailed.set(true);
565:                             }
566:                             DebugLog.log("条件释放失败：" + failure);
567:                         } finally {
568:                             try {
569:                                 if (pendingConditional.decrementAndGet() == 0) {
570:                                     settleConditionalParent(player, weapon, slot, stepped, openedTick, frozenStepCooldown,
571:                                             capturedLastStart.get(), capturedTail.get(), cooldownReduction, replaySkill, techniques,
572:                                             frozenSignature, stepCount, conditionalCommitted, succeededConditionalScaled,
573:                                             ordinarySucceeded, succeededConditionalIds, enchantApplied, recordedUsage, parentSettled, true);
574:                                 }
575:                             } finally {
576:                                 if (pendingConditional.get() <= 0) {
577:                                     releaseConditionalLock(conditionalLockKey, conditionalLockReleased);
578:                                 }
579:                                 if (conditionalCast != null) {
580:                                     conditionalCast.release();
581:                                 }
582:                             }
583:                         }
584:                     });
585:                 }
586:                 tailTechnique = action;
587:                 segmentIndex += 1;
588:                 previous = segment;
589:                 continue;
590:             }
591: 
592:             if (actionDef != null && actionDef.fieldRadius() > 0f && action.getBaseDamage() <= 0) {
593:                 int fieldCopies = SkillChainPlan.copiesBefore(segments, techniques, segmentIndex);
594:                 int epoch = ChargeGate.epoch(player.getUUID());
595:                 WaterMomentum.Hold hold = WaterMomentum.reserve(player, actionDef.momentumSpend());
596:                 int fireTick = startTick + Math.max(0, actionDef.chargeTicks());
597:                 EnchantmentSnapshot fieldSnapshot = ContactCast.snapshot(skill, segment, player);
598:                 ChainScheduler.schedule(player.getServer(), fireTick, () -> {
599:                     if (!ChargeGate.same(player.getUUID(), epoch) || !player.isAlive()) {
600:                         WaterMomentum.release(hold);
601:                         return;
602:                     }
603:                     WaterMomentum.commit(player, hold);
604:                     int createdTick = player.getServer().getTickCount();
605:                     FieldSpawner.releaseFromChain(player, techniques, segment.actionIndex(), actionDef, fieldCopies);
606:                     if (fieldSnapshot != null) {
607:                         com.bslx.XueXiMod.shu.technique.release.field.FieldZones.bindBorn(player.getUUID(), fieldSnapshot, createdTick);
608:                     }
609:                     TechniqueEffect fieldEffect = TechniqueEffectRegistry.get(action.getId());
610:                     if (fieldEffect != null) {
611:                         fieldEffect.execute(player, action, weapon, new TechniqueCastContext(0f, player.getLookAngle(), player.position()));
612:                     }
613:                 });
614:                 if (conditionalChain) {
615:                     rememberOrdinarySuccess(ordinarySucceeded, action);
616:                 }
617:                 segmentIndex += 1;
618:                 previous = segment;
619:                 continue;
620:             }
621: 
622:             EnchantmentResolution enchantments = SkillSegmentMath.resolveSegmentEnchantments(skill, segment);
623:             int magicDamage = SkillSegmentMath.segmentMagicDamage(skill, segment);
624:             EnchantBonus bonus = EnchantBonus.resolve(skill, segment.enchantIndexes(), action.getBaseDamage(), 1);
625: 
626:             final boolean carries = action.getBaseDamage() > 0 && !segment.enchantIndexes().isEmpty();
627:             final float finalDamage = segment.enchantIndexes().isEmpty()
628:                     ? SkillSegmentMath.segmentDamage(action, segment, weaponBonus, magicDamage, attributeDamageBonus)
629:                     : bonus.carriedDamage(weaponBonus);
630:             if (!segment.enchantIndexes().isEmpty() && bonus.cooldownMultiplier > 0f && bonus.cooldownMultiplier < 0.999f && gap > 0) {
631:                 int adjusted = Math.max(1, Math.round(gap * bonus.cooldownMultiplier));
632:                 startTick += adjusted - gap;
633:                 lastStartTick = startTick;
634:             }
635:             DebugLog.log("链段 " + action.getId() + " 魔法伤害=" + magicDamage + " 间隔=" + gap);
636: 
637:             final int currentTick = startTick;
638: 
639:             ChainSegment nextSegment = (segmentIndex + 1 < segments.size()) ? segments.get(segmentIndex + 1) : null;
640:             boolean chainedToTrigger = nextSegment != null
641:                     && SkillSegmentMath.releaseKind(action.getId()) == ReleaseKind.ACTION
642:                     && SkillSegmentMath.releaseKind(techniques.get(nextSegment.actionIndex()).getId()) != ReleaseKind.RULE
643:                     && SkillSegmentMath.isTriggerRelease(techniques.get(nextSegment.actionIndex()).getId())
644:                     && !SkillSegmentMath.manipulatesSwordEntities(techniques.get(nextSegment.actionIndex()).getId());
645: 
646:             if (chainedToTrigger) {
647:                 Technique nextAction = techniques.get(nextSegment.actionIndex());
648:                 EnchantmentResolution nextEnchantments = SkillSegmentMath.resolveSegmentEnchantments(skill, nextSegment);
649:                 int nextMagicDamage = SkillSegmentMath.segmentMagicDamage(skill, nextSegment);
650:                 EnchantBonus nextBonus = EnchantBonus.resolve(skill, nextSegment.enchantIndexes(), nextAction.getBaseDamage(), 1);
651:                 boolean nextCarries = nextAction.getBaseDamage() > 0 && !nextSegment.enchantIndexes().isEmpty();
652:                 float nextDamage = nextSegment.enchantIndexes().isEmpty()
653:                         ? SkillSegmentMath.segmentDamage(nextAction, nextSegment, weaponBonus, nextMagicDamage, attributeDamageBonus)
654:                         : nextBonus.carriedDamage(weaponBonus);
655:                 DebugLog.log("链段 " + nextAction.getId() + " 由冲刺命中触发");
656:                 Runnable runNext = () -> {
657:                     if (!HeldSwordGate.allows(nextAction, player.getMainHandItem())) {
658:                         return;
659:                     }
660:                     int nextCost = SkillSegmentMath.segmentManaCost(techniques, nextSegment);
661:                     if (!MagicPool.trySpend(player, nextCost)) {
662:                         return;
663:                     }
664:                     if (nextCost > 0) {
665:                         MagicPool.showBalance(player);
666:                     }
667:                     SkillSegmentRunner.buildSegmentRunner(player, nextAction, techniques, nextSegment.actionIndex(), weapon, nextDamage, nextCarries, nextEnchantments, null,
668:                             SkillChainPlan.lockThisCast(techniques, nextSegment.actionIndex()), SkillChainPlan.dropThisCast(techniques, segments, nextSegment.actionIndex()),
669:                             null, 0, 1, ReleaseBatch.current(), nextBonus, 0, castSource).run();
670:                 };
671:                 ChainScheduler.schedule(player.getServer(), currentTick,
672:                         SkillSegmentRunner.buildSegmentRunner(player, action, techniques, segment.actionIndex(), weapon, finalDamage, carries, enchantments, position -> runNext.run(),
673:                                 SkillChainPlan.lockThisCast(techniques, segment.actionIndex()), SkillChainPlan.dropThisCast(techniques, segments, segment.actionIndex()),
674:                                 null, 0, 1, ReleaseBatch.current(), bonus, 0, null));
675:                 tailTechnique = action;
676:                 segmentIndex += 2;
677:             } else {
678:                 int segmentShare = SkillSegmentMath.segmentManaCost(techniques, segment);
679:                 ReleaseDefinition segmentDefinition = ReleaseRegistry.get(action.getId());
680:                 if (segmentDefinition != null && segmentDefinition.submitsHostGroup()) {
681:                     ServerLevel castLevel = player.serverLevel();
682:                     int castEpoch = ChargeGate.epoch(player.getUUID());
683:                     HostCast hostCast = HostCast.open(player.getUUID(), castSource);
684:                     hostCast.hold();
685:                     UUID hostGroup = hostCast.hostId(0, 0);
686:                     UncommittedShare claim = UncommittedShare.open(pressReceipt, segmentShare);
687:                     AtomicBoolean once = new AtomicBoolean(false);
688:                     AtomicBoolean released = new AtomicBoolean(false);
689:                     Runnable releaseCast = () -> {
690:                         if (released.compareAndSet(false, true)) {
691:                             hostCast.release();
692:                         }
693:                     };
694:                     try {
695:                         ChainScheduler.schedule(player.getServer(), currentTick, () -> {
696:                             if (!once.compareAndSet(false, true)) {
697:                                 return;
698:                             }
699:                             try {
700:                                 int nowTick = player.getServer() == null ? currentTick : player.getServer().getTickCount();
701:                                 if (!player.isAlive() || player.level() != castLevel || !ChargeGate.same(player.getUUID(), castEpoch)
702:                                         || !HeldSwordGate.allows(action, player.getMainHandItem())) {
703:                                     HostGroupCommit.cancel(hostCast, hostGroup, nowTick, "施法已经中断");
704:                                     claim.cancel(player);
705:                                     return;
706:                                 }
707:                                 ReleaseOutcome outcome = SkillSegmentRunner.commitHostGroup(player, action, techniques, segment.actionIndex(), weapon,
708:                                         finalDamage, carries, enchantments,
709:                                         SkillChainPlan.lockThisCast(techniques, segment.actionIndex()),
710:                                         SkillChainPlan.dropThisCast(techniques, segments, segment.actionIndex()),
711:                                         null, 1, ReleaseBatch.current(), bonus, 0, null, null, null, null, hostCast, hostGroup, null);
712:                                 if (outcome.committed()) {
713:                                     if (conditionalChain) {
714:                                         claim.commitRetained();
715:                                         rememberOrdinarySuccess(ordinarySucceeded, action);
716:                                     } else {
717:                                         claim.commit();
718:                                     }
719:                                 } else {
720:                                     claim.cancel(player);
721:                                 }
722:                             } finally {
723:                                 releaseCast.run();
724:                             }
725:                         });
726:                     } catch (RuntimeException failure) {
727:                         releaseCast.run();
728:                         throw failure;
729:                     }
730:                 } else {
731:                     Runnable keptHook = conditionalChain
732:                             ? () -> rememberOrdinarySuccess(ordinarySucceeded, action)
733:                             : null;
734:                     ChainScheduler.schedule(player.getServer(), currentTick, refundIfBlocked(player, action, pressReceipt, segmentShare,
735:                             SkillSegmentRunner.buildSegmentRunner(player, action, techniques, segment.actionIndex(), weapon, finalDamage, carries, enchantments, null,
736:                                     SkillChainPlan.lockThisCast(techniques, segment.actionIndex()), SkillChainPlan.dropThisCast(techniques, segments, segment.actionIndex()),
737:                                     null, 0, 1, ReleaseBatch.current(), bonus, 0, castSource), keptHook));
738:                 }
739:                 tailTechnique = action;
740:                 segmentIndex += 1;
741:             }
742: 
743:             previous = segment;
744:         }
745: 
746:         if (stepFailed && !conditionalChain) {
747:             if (pressReceipt != null) {
748:                 pressReceipt.refund(player);
749:             }
750:             return CastTiming.insufficient();
751:         }
752:         if (stepFailed) {
753:             conditionalAborted.set(true);
754:             cancelUnkept(conditionalClaims, player);
755:             if (pressReceipt != null && failedFromIndex >= 0) {
756:                 int tailCost = SkillChainPlan.pressCommitCost(techniques, segments, failedFromIndex);
757:                 if (tailCost > 0) {
758:                     pressReceipt.refundShare(player, tailCost);
759:                 }
760:             }
761:         }
762: 
763:         capturedLastStart.set(lastStartTick);
764:         capturedTail.set(tailTechnique);
765:         if (conditionalChain) {
766:             if (pendingConditional.get() > 0) {
767:                 handedOff.set(true);
768:                 return CastTiming.rejected();
769:             }
770:             int settledReplay = settleConditionalParent(player, weapon, slot, stepped, openedTick, frozenStepCooldown,
771:                     lastStartTick, tailTechnique, cooldownReduction, replaySkill, techniques, frozenSignature, stepCount,
772:                     conditionalCommitted, succeededConditionalScaled, ordinarySucceeded, succeededConditionalIds,
773:                     enchantApplied, recordedUsage, parentSettled, false);
774:             if (settledReplay < 0) {
775:                 return CastTiming.insufficient();
776:             }
777:             return CastTiming.accepted(openedTick, lastStartTick, settledReplay, List.of());
778:         }
779: 
780:         int replayTick = conditionalReplayTick(stepped, openedTick, stepCooldown, lastStartTick, tailTechnique, cooldownReduction);
781:         if (stepped) {
782:             ChainProgress.advance(player, weapon, slot, stepSignature, stepCount);
783:         }
784:         return CastTiming.accepted(openedTick, lastStartTick, replayTick, techniques);
785:         } catch (RuntimeException failure) {
786:             conditionalAborted.set(true);
787:             cancelUnkept(conditionalClaims, player);
788:             capturedLastStart.set(lastStartTick);
789:             capturedTail.set(tailTechnique);
790:             if (pendingConditional.get() > 0) {
791:                 handedOff.set(true);
792:             } else {
793:                 settleConditionalParent(player, weapon, slot, stepped, openedTick, frozenStepCooldown,
794:                         lastStartTick, tailTechnique, cooldownReduction, replaySkill, techniques, frozenSignature, stepCount,
795:                         conditionalCommitted, succeededConditionalScaled, ordinarySucceeded, succeededConditionalIds,
796:                         enchantApplied, recordedUsage, parentSettled, true);
797:             }
798:             throw failure;
799:         } finally {
800:             ReleaseBatch.unbind();
801:             if (conditionalCast != null) {
802:                 conditionalCast.release();
803:             }
804:             if (!handedOff.get()) {
805:                 releaseConditionalLock(conditionalLockKey, conditionalLockReleased);
806:             }
807:         }
808:     }
809: 
810:     private static HostCast openConditionalCast(ServerPlayer player, List<Technique> techniques,
811:                                                 List<ChainSegment> segments, CastSource castSource) {
812:         for (ChainSegment listedSegment : segments) {
813:             Technique listedTechnique = techniques.get(listedSegment.actionIndex());
814:             ReleaseDefinition listedDefinition = ReleaseRegistry.get(listedTechnique.getId());
815:             if (listedTechnique.getBaseDamage() <= 0f && listedDefinition != null
816:                     && listedDefinition.conditionalEffect() != null) {
817:                 HostCast opened = HostCast.open(player.getUUID(), castSource);
818:                 opened.hold();
819:                 return opened;
820:             }
821:         }
822:         return null;
823:     }
824: 
825:     private static int conditionalReplayTick(boolean stepped, int openedTick, int stepCooldown, int lastStartTick,
826:                                              Technique tailTechnique, double cooldownReduction) {
827:         if (stepped) {
828:             return openedTick + Math.max(stepCooldown, 1);
829:         }
830:         return lastStartTick + Math.max(tailCooldownOf(tailTechnique, cooldownReduction), 1);
831:     }
832: 
833:     private static int tailCooldownOf(Technique tailTechnique, double cooldownReduction) {
834:         if (tailTechnique == null) {
835:             return 0;
836:         }
837:         ReleaseDefinition tailDefinition = ReleaseRegistry.get(tailTechnique.getId());
838:         boolean conditionalTail = tailTechnique.getBaseDamage() <= 0f
839:                 && tailDefinition != null && tailDefinition.conditionalEffect() != null;
840:         if (tailTechnique.getBaseDamage() > 0f || conditionalTail) {
841:             return SkillSegmentMath.scaleCooldown(tailTechnique.getBaseCooldown(), cooldownReduction);
842:         }
843:         return 0;
844:     }
845: 
846:     private static Runnable refundIfBlocked(ServerPlayer player, Technique action, ManaPaymentReceipt receipt, int share,
847:                                             Runnable body, Runnable keptHook) {
848:         ServerLevel castLevel = player.serverLevel();
849:         int castEpoch = ChargeGate.epoch(player.getUUID());
850:         UncommittedShare claim = UncommittedShare.open(receipt, share);
851:         AtomicBoolean once = new AtomicBoolean(false);
852:         return () -> {
853:             if (!once.compareAndSet(false, true)) {
854:                 return;
855:             }
856:             if (!player.isAlive() || player.level() != castLevel || !ChargeGate.same(player.getUUID(), castEpoch)
857:                     || !HeldSwordGate.allows(action, player.getMainHandItem())) {
858:                 claim.cancel(player);
859:                 return;
860:             }
861:             body.run();
862:             if (keptHook == null) {
863:                 claim.commit();
864:                 return;
865:             }
866:             claim.commitRetained();
867:             keptHook.run();
868:         };
869:     }
870: 
871:     static void recordTechniqueUse(ServerPlayer player, Technique technique, Set<String> recorded) {
872:         if (player == null || technique == null || recorded == null || technique.getId() == null) {
873:             return;
874:         }
875:         if (!recorded.add(technique.getId())) {
876:             return;
877:         }
878:         PlayerShuData data = PlayerShuData.get(player);
879:         data.incrementUsage(technique.getId());
880:         boolean leveledUp = data.addUsageAndTryLevelUp(technique.getType(), player);
881:         PlayerShuData.save(player, data);
882:         if (leveledUp) {
883:             com.bslx.XueXiMod.player.DomainLearning.offerReady(player);
884:             com.bslx.XueXiMod.network.ModNetwork.sendToPlayer(player, new com.bslx.XueXiMod.network.ShuSyncPacket(data));
885:         }
886:     }
887: 
888:     private static void rememberOrdinarySuccess(List<Technique> succeeded, Technique technique) {
889:         if (technique == null || technique.getId() == null) {
890:             return;
891:         }
892:         for (Technique existing : succeeded) {
893:             if (technique.getId().equals(existing.getId())) {
894:                 return;
895:             }
896:         }
897:         succeeded.add(technique);
898:     }
899: 
900:     private static void cancelUnkept(List<UncommittedShare> claims, ServerPlayer player) {
901:         for (UncommittedShare claim : claims) {
902:             if (!claim.kept()) {
903:                 claim.cancel(player);
904:             }
905:         }
906:     }
907: 
908:     private static void releaseConditionalLock(String carrierKey, AtomicBoolean released) {
909:         if (carrierKey == null || released == null) {
910:             return;
911:         }
912:         if (released.compareAndSet(false, true)) {
913:             CastCooldown.release(carrierKey);
914:         }
915:     }
916: 
917:     private static boolean isZeroDamageConditional(Technique technique) {
918:         if (technique == null || technique.getBaseDamage() > 0f) {
919:             return false;
920:         }
921:         ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
922:         return definition != null && definition.conditionalEffect() != null;
923:     }
924: 
925:     private static boolean chainHasConditional(List<Technique> techniques, List<ChainSegment> segments) {
926:         for (ChainSegment listedSegment : segments) {
927:             if (isZeroDamageConditional(techniques.get(listedSegment.actionIndex()))) {
928:                 return true;
929:             }
930:         }
931:         return false;
932:     }
933: 
934:     private static int settleConditionalParent(ServerPlayer player, ItemStack weapon, int slot, boolean stepped,
935:                                                int openedTick, int frozenStepCooldown, int lastStartTick,
936:                                                Technique tailTechnique, double cooldownReduction, Skill replaySkill,
937:                                                List<Technique> techniques, String stepSignature, int stepCount,
938:                                                AtomicInteger conditionalCommitted, AtomicInteger succeededConditionalScaled,
939:                                                List<Technique> ordinarySucceeded, Set<String> succeededConditionalIds,
940:                                                AtomicBoolean enchantApplied, Set<String> recordedUsage,
941:                                                AtomicBoolean parentSettled, boolean markCooldown) {
942:         if (!parentSettled.compareAndSet(false, true)) {
943:             return -1;
944:         }
945:         boolean conditionalSuccess = conditionalCommitted.get() > 0;
946:         boolean ordinarySuccess = !ordinarySucceeded.isEmpty();
947:         if (!conditionalSuccess && !ordinarySuccess) {
948:             return -1;
949:         }
950:         for (Technique technique : ordinarySucceeded) {
951:             recordTechniqueUse(player, technique, recordedUsage);
952:         }
953:         if (enchantApplied.get()) {
954:             for (Technique technique : techniques) {
955:                 if (technique.isEnchantment()) {
956:                     recordTechniqueUse(player, technique, recordedUsage);
957:                 }
958:             }
959:         }
960:         if (conditionalSuccess) {
961:             for (Technique technique : techniques) {
962:                 if (isZeroDamageConditional(technique) && succeededConditionalIds.contains(technique.getId())) {
963:                     recordTechniqueUse(player, technique, recordedUsage);
964:                 }
965:             }
966:         }
967:         int replayTick = conditionalParentReplay(stepped, openedTick, frozenStepCooldown, lastStartTick, tailTechnique,
968:                 cooldownReduction, replaySkill, techniques, conditionalCommitted.get(), succeededConditionalScaled.get());
969:         if (markCooldown) {
970:             CastCooldown.mark(player, weapon, slot, replayTick);
971:         }
972:         if (stepped) {
973:             ChainProgress.advance(player, weapon, slot, stepSignature, stepCount);
974:         }
975:         return replayTick;
976:     }
977: 
978:     private static int conditionalParentReplay(boolean stepped, int openedTick, int frozenStepCooldown, int lastStartTick,
979:                                                Technique tailTechnique, double cooldownReduction, Skill replaySkill,
980:                                                List<Technique> techniques, int conditionalCommittedCount,
981:                                                int succeededConditionalScaled) {
982:         if (stepped) {
983:             int cooldown = frozenStepCooldown;
984:             if (conditionalCommittedCount <= 0) {
985:                 cooldown = steppedReplayCooldown(replaySkill, techniques, cooldownReduction);
986:             }
987:             return openedTick + Math.max(cooldown, 1);
988:         }
989:         int tailPart = tailCooldownOf(tailTechnique, cooldownReduction);
990:         if (conditionalCommittedCount <= 0 && isZeroDamageConditional(tailTechnique)) {
991:             tailPart = 0;
992:         }
993:         int conditionalPart = conditionalCommittedCount > 0 ? succeededConditionalScaled : 0;
994:         int piece = tailPart > conditionalPart ? tailPart : conditionalPart;
995:         return lastStartTick + Math.max(piece, 1);
996:     }
997: 
998:     private static int steppedReplayCooldown(Skill replaySkill, List<Technique> techniques, double cooldownReduction) {
999:         List<Integer> keptIndexes = new ArrayList<>();
1000:         for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
1001:             if (!isZeroDamageConditional(techniques.get(techniqueIndex))) {
1002:                 keptIndexes.add(techniqueIndex);
1003:             }
1004:         }
1005:         if (keptIndexes.isEmpty()) {
1006:             return 0;
1007:         }
1008:         return ChainSteps.cooldown(replaySkill, keptIndexes, cooldownReduction);
1009:     }
1010: 
1011:     private CastTiming imprintEnchantChain(ServerPlayer player, Skill skill, List<Technique> techniques, double cooldownReduction) {
1012:         int enchantCost = 0;
1013:         int cooldownSum = 0;
1014:         for (Technique technique : techniques) {
1015:             enchantCost += technique.manaCost();
1016:             cooldownSum += SkillSegmentMath.scaleCooldown(technique.getBaseCooldown(), cooldownReduction);
1017:         }
1018:         if (!MagicPool.trySpend(player, enchantCost)) {
1019:             return CastTiming.insufficient();
1020:         }
1021:         if (enchantCost > 0) {
1022:             MagicPool.showBalance(player);
1023:         }
1024:         SkillChainPlan.imprintEnchant(player, skill, techniques, 1);
1025:         int nowTick = player.getServer().getTickCount();
1026:         return CastTiming.accepted(nowTick, nowTick, nowTick + Math.max(cooldownSum, 1), techniques);
1027:     }
1028: 
1029:     private static boolean legalEnchantChain(List<Technique> techniques) {
1030:         if (techniques.isEmpty()) {
1031:             return false;
1032:         }
1033:         for (Technique technique : techniques) {
1034:             if (!technique.isEnchantment()) {
1035:                 return false;
1036:             }
1037:             ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
1038:             if (definition != null && definition.fieldRadius() > 0f) {
1039:                 return false;
1040:             }
1041:         }
1042:         return true;
1043:     }
1044: 
1045:     public static void register(SimpleChannel channel, int messageId) {
1046:         channel.registerMessage(messageId,
1047:                 SkillUsePacket.class,
1048:                 SkillUsePacket::encode,
1049:                 SkillUsePacket::decode,
1050:                 SkillUsePacket::handle);
1051:     }
1052: }
1053: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/network/SkillSegmentRunner.java">
1: package com.bslx.XueXiMod.network;
2: 
3: import com.bslx.XueXiMod.debug.DebugLog;
4: import com.bslx.XueXiMod.shu.Technique;
5: import com.bslx.XueXiMod.shu.TechniqueType;
6: import com.bslx.XueXiMod.shu.technique.ActionWindow;
7: import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
8: import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
9: import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
10: import com.bslx.XueXiMod.shu.technique.TechniqueEffectRegistry;
11: import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
12: import com.bslx.XueXiMod.shu.technique.effects.LockTargetService;
13: import com.bslx.XueXiMod.shu.technique.enchant.EnchantBonus;
14: import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
15: import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
16: import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalResult;
17: import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
18: import com.bslx.XueXiMod.shu.technique.release.parting.PartingOffers;
19: import com.bslx.XueXiMod.shu.technique.release.parting.PartingOperationId;
20: import com.bslx.XueXiMod.shu.technique.release.CastFrame;
21: import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
22: import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
23: import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
24: import com.bslx.XueXiMod.shu.technique.release.Trajectory;
25: import com.bslx.XueXiMod.shu.technique.release.field.FieldInfluence;
26: import com.bslx.XueXiMod.shu.technique.release.field.FieldSpawner;
27: import com.bslx.XueXiMod.shu.technique.release.field.FieldZones;
28: import com.bslx.XueXiMod.command.SkillEntityCount;
29: import com.bslx.XueXiMod.shu.technique.release.momentum.ChargeGate;
30: import com.bslx.XueXiMod.shu.technique.release.momentum.HeavyWater;
31: import com.bslx.XueXiMod.shu.technique.release.momentum.WaterMomentum;
32: import com.bslx.XueXiMod.shu.technique.release.entity.HostBeatHooks;
33: import com.bslx.XueXiMod.shu.technique.release.entity.HostCast;
34: import com.bslx.XueXiMod.shu.technique.release.entity.HostGroupCommit;
35: import com.bslx.XueXiMod.shu.technique.release.entity.HostGroupContext;
36: import com.bslx.XueXiMod.shu.technique.release.entity.HostGroupListener;
37: import com.bslx.XueXiMod.shu.technique.release.entity.HostGroupReceipt;
38: import com.bslx.XueXiMod.shu.technique.release.water.CastSource;
39: import com.bslx.XueXiMod.shu.technique.release.payload.HitPayloads;
40: import net.minecraft.server.MinecraftServer;
41: import net.minecraft.server.level.ServerPlayer;
42: import net.minecraft.world.InteractionHand;
43: import net.minecraft.world.effect.MobEffectInstance;
44: import net.minecraft.world.effect.MobEffects;
45: import net.minecraft.world.entity.LivingEntity;
46: import net.minecraft.world.item.ItemStack;
47: import net.minecraft.world.phys.Vec3;
48: 
49: import java.util.List;
50: import java.util.UUID;
51: import java.util.function.Consumer;
52: 
53: final class SkillSegmentRunner {
54:     static Runnable buildSegmentRunner(ServerPlayer player, Technique action, List<Technique> techniques,
55:                                         int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
56:                                         EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
57:                                         boolean lockThisCast, boolean dropThisCast) {
58:         return buildSegmentRunner(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
59:                 onFirstTrigger, lockThisCast, dropThisCast, null, 0, 1, ReleaseBatch.current(), EnchantBonus.NONE, 0, null);
60:     }
61: 
62:     static Runnable buildSegmentRunner(ServerPlayer player, Technique action, List<Technique> techniques,
63:                                         int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
64:                                         EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
65:                                         boolean lockThisCast, boolean dropThisCast, CastSource castSource) {
66:         return buildSegmentRunner(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
67:                 onFirstTrigger, lockThisCast, dropThisCast, null, 0, 1, ReleaseBatch.current(), EnchantBonus.NONE, 0, castSource);
68:     }
69: 
70:     static Runnable buildSegmentRunner(ServerPlayer player, Technique action, List<Technique> techniques,
71:                                         int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
72:                                         EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
73:                                         boolean lockThisCast, boolean dropThisCast, ReleaseBatch batch) {
74:         return buildSegmentRunner(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
75:                 onFirstTrigger, lockThisCast, dropThisCast, null, 0, 1, batch, EnchantBonus.NONE, 0, null);
76:     }
77: 
78:     static Runnable buildSegmentRunner(ServerPlayer player, Technique action, List<Technique> techniques,
79:                                         int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
80:                                         EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
81:                                         boolean lockThisCast, boolean dropThisCast,
82:                                         ReleaseBatch.Tally tally, int copyIndex, int copyCount, ReleaseBatch batch,
83:                                         EnchantBonus bonus, int volley, CastSource castSource) {
84:         return () -> {
85:             if (!player.isAlive()) {
86:                 return;
87:             }
88:             if (!HeldSwordGate.allows(action, player.getMainHandItem())) {
89:                 return;
90:             }
91:             Vec3 lookVec = player.getLookAngle();
92:             Vec3 position = player.position();
93:             if (commitLooseHost(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
94:                     onFirstTrigger, lockThisCast, dropThisCast, tally, copyIndex, copyCount, batch, bonus, volley, lookVec, position, castSource)) {
95:                 return;
96:             }
97:             if (action.getType() == TechniqueType.SWORD) {
98:                 player.swing(InteractionHand.MAIN_HAND, true);
99:             }
100:             runSegment(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
101:                     onFirstTrigger, lockThisCast, dropThisCast, tally, copyIndex, copyCount, batch, lookVec, position,
102:                     bonus, volley, null, null, null);
103:         };
104:     }
105: 
106:     static Runnable buildSegmentRunnerAt(ServerPlayer player, Technique action, List<Technique> techniques,
107:                                           int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
108:                                           EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
109:                                           Vec3 lookVec, Vec3 position, boolean lockThisCast, boolean dropThisCast) {
110:         return buildSegmentRunnerAt(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
111:                 onFirstTrigger, lookVec, position, lockThisCast, dropThisCast, null, 0, 1, ReleaseBatch.current(), EnchantBonus.NONE, 0, null);
112:     }
113: 
114:     static Runnable buildSegmentRunnerAt(ServerPlayer player, Technique action, List<Technique> techniques,
115:                                           int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
116:                                           EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
117:                                           Vec3 lookVec, Vec3 position, boolean lockThisCast, boolean dropThisCast,
118:                                           ReleaseBatch batch) {
119:         return buildSegmentRunnerAt(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
120:                 onFirstTrigger, lookVec, position, lockThisCast, dropThisCast, null, 0, 1, batch, EnchantBonus.NONE, 0, null);
121:     }
122: 
123:     static Runnable buildSegmentRunnerAt(ServerPlayer player, Technique action, List<Technique> techniques,
124:                                           int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
125:                                           EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
126:                                           Vec3 lookVec, Vec3 position, boolean lockThisCast, boolean dropThisCast,
127:                                           ReleaseBatch.Tally tally, int copyIndex, int copyCount, ReleaseBatch batch,
128:                                           EnchantBonus bonus, int volley, CastSource castSource) {
129:         return () -> {
130:             if (!player.isAlive()) {
131:                 return;
132:             }
133:             if (!HeldSwordGate.allows(action, player.getMainHandItem())) {
134:                 return;
135:             }
136:             if (commitLooseHost(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
137:                     onFirstTrigger, lockThisCast, dropThisCast, tally, copyIndex, copyCount, batch, bonus, volley, lookVec, position, castSource)) {
138:                 return;
139:             }
140:             if (action.getType() == TechniqueType.SWORD) {
141:                 player.swing(InteractionHand.MAIN_HAND, true);
142:             }
143:             runSegment(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
144:                     onFirstTrigger, lockThisCast, dropThisCast, tally, copyIndex, copyCount, batch, lookVec, position,
145:                     bonus, volley, null, null, null);
146:         };
147:     }
148: 
149:     /** 马上提交。蓄力宿主在这里直接拒绝，不排进以后的 tick。 */
150:     static ReleaseOutcome commitNow(ServerPlayer player, Technique action, List<Technique> techniques,
151:                                      int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
152:                                      EnchantmentResolution enchantments, boolean lockThisCast, boolean dropThisCast,
153:                                      ReleaseBatch.Tally tally, int copyIndex, int copyCount, ReleaseBatch batch,
154:                                      EnchantBonus bonus, int volley, Vec3 lookVec, Vec3 position) {
155:         int nowTick = player.getServer() == null ? 0 : player.getServer().getTickCount();
156:         if (!player.isAlive()) {
157:             return ReleaseOutcome.cancelled(nowTick, "施法者已经离开");
158:         }
159:         if (!HeldSwordGate.allows(action, player.getMainHandItem())) {
160:             return ReleaseOutcome.rejected(nowTick, "");
161:         }
162:         ReleaseDefinition definition = ReleaseRegistry.get(action.getId());
163:         if (action.getBaseDamage() <= 0f && definition != null && definition.conditionalEffect() != null) {
164:             return ReleaseOutcome.rejected(nowTick, "条件释放没有按回执提交");
165:         }
166:         if (TechniqueEffectRegistry.get(action.getId()) == null) {
167:             return ReleaseOutcome.rejected(nowTick, "没有可提交的释放");
168:         }
169:         int chargeTicks = definition == null ? 0 : definition.chargeTicks();
170:         if (chargeTicks > 0 && !ChargeGate.armed(player.getUUID())) {
171:             return ReleaseOutcome.rejected(nowTick, "蓄力宿主不能在这一拍提交");
172:         }
173:         runSegment(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
174:                 null, lockThisCast, dropThisCast, tally, copyIndex, copyCount, batch, lookVec, position,
175:                 bonus, volley, position, lookVec, null);
176:         return ReleaseOutcome.committed(player.getServer().getTickCount());
177:     }
178: 
179:     /** 接入宿主组的术式走这里。效果返回不算提交，整组生成成功才提交一次。 */
180:     static ReleaseOutcome commitHostGroup(ServerPlayer player, Technique action, List<Technique> techniques,
181:                                            int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
182:                                            EnchantmentResolution enchantments, boolean lockThisCast, boolean dropThisCast,
183:                                            ReleaseBatch.Tally tally, int copyCount, ReleaseBatch batch,
184:                                            EnchantBonus bonus, int volley, Vec3 lookVec, Vec3 position,
185:                                            Vec3 contactOrigin, Vec3 contactLook,
186:                                            HostCast hostCast, UUID groupId, HostGroupListener listener) {
187:         return commitHostGroup(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
188:                 null, lockThisCast, dropThisCast, tally, copyCount, batch, bonus, volley, lookVec, position,
189:                 contactOrigin, contactLook, hostCast, groupId, listener);
190:     }
191: 
192:     static ReleaseOutcome commitHostGroup(ServerPlayer player, Technique action, List<Technique> techniques,
193:                                            int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
194:                                            EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
195:                                            boolean lockThisCast, boolean dropThisCast,
196:                                            ReleaseBatch.Tally tally, int copyCount, ReleaseBatch batch,
197:                                            EnchantBonus bonus, int volley, Vec3 lookVec, Vec3 position,
198:                                            Vec3 contactOrigin, Vec3 contactLook,
199:                                            HostCast hostCast, UUID groupId, HostGroupListener listener) {
200:         int nowTick = player.getServer() == null ? 0 : player.getServer().getTickCount();
201:         if (hostCast == null) {
202:             return ReleaseOutcome.rejected(nowTick, "宿主组没有父释放");
203:         }
204:         HostGroupReceipt prior = HostGroupCommit.find(hostCast, groupId);
205:         if (prior != null) {
206:             return outcomeOf(prior);
207:         }
208:         if (hostCast.closed()) {
209:             return outcomeOf(HostGroupCommit.cancel(hostCast, groupId, nowTick, "宿主组已经结束"));
210:         }
211:         if (!player.isAlive()) {
212:             return outcomeOf(HostGroupCommit.cancel(hostCast, groupId, nowTick, "施法者已经离开"));
213:         }
214:         if (!HeldSwordGate.allows(action, player.getMainHandItem())) {
215:             return outcomeOf(HostGroupCommit.reject(hostCast, groupId, nowTick, ""));
216:         }
217:         if (TechniqueEffectRegistry.get(action.getId()) == null) {
218:             return outcomeOf(HostGroupCommit.reject(hostCast, groupId, nowTick, "没有可提交的释放"));
219:         }
220:         ReleaseDefinition definition = ReleaseRegistry.get(action.getId());
221:         int chargeTicks = definition == null ? 0 : definition.chargeTicks();
222:         if (chargeTicks > 0 && !ChargeGate.armed(player.getUUID())) {
223:             return outcomeOf(HostGroupCommit.reject(hostCast, groupId, nowTick, "蓄力宿主不能在这一拍提交"));
224:         }
225:         int spawnCount = Math.max(1, SkillEntityCount.entityCount());
226:         int safeCopies = Math.max(1, copyCount);
227:         Vec3 sharedLook = lookVec != null ? lookVec : player.getLookAngle();
228:         Vec3 sharedPlace = position != null ? position : player.position();
229:         HostGroupReceipt receipt = HostGroupCommit.commitOnce(hostCast, player, groupId, safeCopies * spawnCount, spawnCount, listener,
230:                 () -> {
231:                     if (definition != null && definition.tracksBeatCycle()) {
232:                         HostBeatHooks.beforeCopies(player, HostGroupContext.current());
233:                     }
234:                     for (int copyIndex = 0; copyIndex < safeCopies; copyIndex++) {
235:                         HostGroupContext hostGroup = HostGroupContext.current();
236:                         if (hostGroup != null && hostGroup.failed()) {
237:                             break;
238:                         }
239:                         runSegment(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
240:                                 onFirstTrigger, lockThisCast, dropThisCast, tally, copyIndex, safeCopies, batch, sharedLook, sharedPlace,
241:                                 bonus, volley, contactOrigin, contactLook, null);
242:                     }
243:                 });
244:         return outcomeOf(receipt);
245:     }
246: 
247:     private static ReleaseOutcome outcomeOf(HostGroupReceipt receipt) {
248:         if (receipt == null) {
249:             return ReleaseOutcome.rejected(0, "宿主组没有回执");
250:         }
251:         return switch (receipt.kind()) {
252:             case COMMITTED -> ReleaseOutcome.committed(receipt.tick());
253:             case CANCELLED -> ReleaseOutcome.cancelled(receipt.tick(), receipt.reason());
254:             case REJECTED -> ReleaseOutcome.rejected(receipt.tick(), receipt.reason());
255:         };
256:     }
257: 
258:     static boolean playAction(ServerPlayer player, Technique action, List<Technique> techniques, int actionIndex,
259:                               ItemStack weapon, ActionWindow.Sink sink) {
260:         if (player == null || !player.isAlive() || sink == null || TechniqueEffectRegistry.get(action.getId()) == null) {
261:             return false;
262:         }
263:         runSegment(player, action, techniques, actionIndex, weapon, 0f, false, EnchantmentResolution.empty(),
264:                 null, false, false, null, 0, 1, null, player.getLookAngle(), player.position(),
265:                 EnchantBonus.NONE, 0, null, null, sink);
266:         return true;
267:     }
268: 
269:     /** 条件释放的真实回执。排进调度器本身不算提交。 */
270:     static ReleaseOutcome commitConditional(ServerPlayer player, ReleaseDefinition definition,
271:                                             UUID operationId, EnchantmentSnapshot contact) {
272:         int nowTick = player == null || player.getServer() == null ? 0 : player.getServer().getTickCount();
273:         if (player == null || !player.isAlive()) {
274:             return ReleaseOutcome.cancelled(nowTick, "施法者已经离开");
275:         }
276:         if (definition == null || definition.conditionalEffect() == null || operationId == null) {
277:             return ReleaseOutcome.rejected(nowTick, "没有条件释放");
278:         }
279:         try {
280:             ConditionalResult result = definition.conditionalEffect().attempt(player, operationId, contact);
281:             if (result != null && result.isCommitted()) {
282:                 return ReleaseOutcome.committed(nowTick);
283:             }
284:             String because = result == null || result.reason() == null ? "" : result.reason();
285:             return ReleaseOutcome.rejected(nowTick, because);
286:         } catch (RuntimeException failure) {
287:             DebugLog.log("条件释放失败：" + failure);
288:             return ReleaseOutcome.rejected(nowTick, "这次没有释放");
289:         }
290:     }
291: 
292:     static void runSegment(ServerPlayer player, Technique action, List<Technique> techniques, int actionIndex,
293:                             ItemStack weapon, float finalDamage, boolean carries,
294:                             EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
295:                             boolean lockThisCast, boolean dropThisCast,
296:                             ReleaseBatch.Tally tally, int copyIndex, int copyCount,
297:                             ReleaseBatch batch, Vec3 lookVec, Vec3 position,
298:                             EnchantBonus bonus, int volley,
299:                             Vec3 contactOrigin, Vec3 contactLook, ActionWindow.Sink actionSink) {
300:         ReleaseDefinition conditionalDefinition = ReleaseRegistry.get(action.getId());
301:         if (action.getBaseDamage() <= 0f && conditionalDefinition != null && conditionalDefinition.conditionalEffect() != null) {
302:             return;
303:         }
304:         if (contactOrigin != null) {
305:             position = contactOrigin;
306:         }
307:         if (contactLook != null && contactLook.lengthSqr() > 1.0E-8) {
308:             lookVec = contactLook.normalize();
309:         }
310:         final Vec3 scheduledLook = lookVec;
311:         final Vec3 scheduledPosition = position;
312:         TechniqueEffect effect = TechniqueEffectRegistry.get(action.getId());
313:         if (effect == null) {
314:             return;
315:         }
316:         ReleaseDefinition attackDefinition = ReleaseRegistry.get(action.getId());
317:         int chargeTicks = attackDefinition == null ? 0 : attackDefinition.chargeTicks();
318:         if (chargeTicks > 0 && !ChargeGate.armed(player.getUUID())) {
319:             int epoch = ChargeGate.epoch(player.getUUID());
320:             WaterMomentum.Hold hold = WaterMomentum.reserve(player, attackDefinition.momentumSpend());
321:             MinecraftServer server = player.getServer();
322:             if (server == null) {
323:                 WaterMomentum.release(hold);
324:                 return;
325:             }
326:             ChainScheduler.schedule(server, server.getTickCount() + chargeTicks, () -> {
327:                 if (!ChargeGate.same(player.getUUID(), epoch) || !player.isAlive()) {
328:                     WaterMomentum.release(hold);
329:                     return;
330:                 }
331:                 WaterMomentum.commit(player, hold);
332:                 ChargeGate.arm(player.getUUID());
333:                 try {
334:                     runSegment(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
335:                             onFirstTrigger, lockThisCast, dropThisCast, tally, copyIndex, copyCount, batch, scheduledLook,
336:                             scheduledPosition, bonus, volley, contactOrigin, contactLook, actionSink);
337:                 } finally {
338:                     ChargeGate.disarm(player.getUUID());
339:                 }
340:             });
341:             return;
342:         }
343:         if (chargeTicks <= 0 && attackDefinition != null) {
344:             WaterMomentum.commit(player, WaterMomentum.reserve(player, attackDefinition.momentumSpend()));
345:         }
346:         HeavyWater.Session heavySession = null;
347:         HostGroupContext openedGroup = HostGroupContext.current();
348:         if (attackDefinition != null && attackDefinition.heavyMomentumPerCopy() > 0f) {
349:             int multiplier = openedGroup == null ? SkillEntityCount.entityCount() : openedGroup.spawnCount();
350:             int plannedCopies = Math.max(1, copyCount) * multiplier;
351:             String groupKey = openedGroup == null
352:                     ? player.getUUID() + "/" + action.getId() + "/" + volley + "/"
353:                     + System.identityHashCode(batch) + "/" + player.serverLevel().getGameTime()
354:                     : player.getUUID() + "/host/" + openedGroup.groupId();
355:             heavySession = HeavyWater.enter(player, groupKey, attackDefinition.heavyMomentumPerCopy(), plannedCopies);
356:         }
357:         ReleaseBatch outer = ReleaseBatch.current();
358:         if (batch != null) {
359:             ReleaseBatch.bind(batch);
360:         }
361:         try {
362:             EnchantmentResolution applied = carries ? enchantments : EnchantmentResolution.empty();
363:             boolean spaceLock = false;
364:             boolean entityLock = false;
365:             boolean scatter = false;
366:             boolean tracking = false;
367:             Trajectory pathOverride = null;
368:             for (int earlierIndex = actionIndex - 1; earlierIndex >= 0; earlierIndex--) {
369:                 Technique earlier = techniques.get(earlierIndex);
370:                 if (earlier.getBaseDamage() > 0 && !earlier.isEnchantment()) {
371:                     if (SkillSegmentMath.sharesDualRelease(techniques, earlierIndex, actionIndex)) {
372:                         continue;
373:                     }
374:                     break;
375:                 }
376:                 switch (SkillSegmentMath.roleOf(earlier)) {
377:                     case SEEK_SPACE -> spaceLock = true;
378:                     case SEEK_ENTITY -> entityLock = true;
379:                     case TRAJECTORY_TRACKING -> tracking = true;
380:                     case TRAJECTORY_SCATTER -> scatter = true;
381:                     case TRAJECTORY_DROP -> {
382:                         if (pathOverride == null) {
383:                             ReleaseDefinition pathDefinition = ReleaseRegistry.get(earlier.getId());
384:                             if (pathDefinition != null) {
385:                                 pathOverride = pathDefinition.trajectory();
386:                             }
387:                         }
388:                     }
389:                     default -> {
390:                     }
391:                 }
392:             }
393:             boolean chainLock = entityLock;
394:             Trajectory chainPath = pathOverride;
395:             boolean chainTracking = tracking;
396:             FieldInfluence influence = FieldZones.influence(player);
397:             float usedDamage = finalDamage * influence.damageScale();
398:             tracking |= influence.attachedTracking;
399:             if (!entityLock && influence.attachedLock) {
400:                 entityLock = true;
401:             }
402:             if (pathOverride == null && influence.attachedPath != null) {
403:                 pathOverride = influence.attachedPath;
404:             }
405:             HostGroupContext hostGroup = HostGroupContext.current();
406:             boolean useFrozen = hostGroup != null && hostGroup.hasFrozenLook();
407:             LivingEntity lockedTarget = null;
408:             if (useFrozen) {
409:                 lookVec = hostGroup.frozenLook();
410:             } else if (entityLock) {
411:                 lockedTarget = LockTargetService.resolve(player);
412:                 if (lockedTarget != null) {
413:                     lockedTarget.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
414:                 }
415:             }
416:             if (attackDefinition != null && attackDefinition.fieldRadius() > 0f) {
417:                 FieldSpawner.releaseAttached(player, attackDefinition, 1, chainLock, chainPath, chainTracking);
418:             }
419:             boolean mutable = attackDefinition != null && attackDefinition.trajectoryMutable();
420:             boolean dedicatedAim = attackDefinition != null && attackDefinition.dedicatedInitialAim();
421:             boolean frameLock = entityLock && mutable ? true : lockThisCast;
422:             if (spaceLock) {
423:                 frameLock = entityLock && mutable;
424:             }
425:             if (dedicatedAim) {
426:                 frameLock = false;
427:                 if (!useFrozen && entityLock && lockedTarget != null) {
428:                     Vec3 fromPoint = contactOrigin != null ? contactOrigin : player.getEyePosition();
429:                     Vec3 towardTarget = lockedTarget.getBoundingBox().getCenter().subtract(fromPoint);
430:                     if (towardTarget.lengthSqr() > 1.0E-8) {
431:                         lookVec = towardTarget.normalize();
432:                     }
433:                 }
434:             }
435:             if (hostGroup != null && !useFrozen) {
436:                 hostGroup.freezeLook(lookVec);
437:             }
438:             Vec3 center;
439:             double groundLevel = Double.NEGATIVE_INFINITY;
440:             if (mutable && batch != null && (spaceLock || pathOverride == Trajectory.VORTEX)) {
441:                 center = batch.crosshairCenter(player);
442:             } else if (mutable && scatter && !entityLock) {
443:                 center = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
444:                 groundLevel = player.getY();
445:             } else if (spaceLock && batch != null) {
446:                 center = batch.resolveAnchor(player);
447:             } else {
448:                 center = player.getEyePosition();
449:             }
450:             long seed = player.getUUID().getLeastSignificantBits()
451:                     ^ player.level().getGameTime()
452:                     ^ action.getId().hashCode()
453:                     ^ ((long) volley << 16);
454:             CastFrame frame = CastFrame.forAttack(usedDamage, applied, tally, copyIndex, copyCount,
455:                     center, lookVec, spaceLock, scatter, entityLock, pathOverride, mutable, seed, groundLevel);
456:             frame.tracking = tracking;
457:             frame.radiusMultiplier = bonus.radiusMultiplier;
458:             float ownEnergy = attackDefinition != null ? attackDefinition.energyScale() : 1f;
459:             float chainEnergy = SkillChainPlan.chainEnergyBefore(techniques, actionIndex);
460:             float combinedEnergy = ownEnergy * chainEnergy;
461:             frame.energyScale = Float.isFinite(combinedEnergy) && combinedEnergy > 0f ? combinedEnergy : 1f;
462:             frame.burnSeconds = bonus.burnSeconds;
463:             frame.controlTicks = bonus.controlTicks;
464:             frame.extinguish = bonus.extinguish;
465:             frame.knockback = bonus.knockback;
466:             frame.speedMultiplier = bonus.speedMultiplier;
467:             frame.tideSuccess = bonus == null ? TideMarkSuccess.NONE : bonus.tideSuccess(player.getUUID());
468:             frame.fieldDamageScale = influence.damageScale();
469:             frame.healAmount = pickedAmount(attackDefinition == null ? 0f : attackDefinition.healAmount(), influence.healAmount);
470:             frame.healCaster = attackDefinition != null && attackDefinition.healCaster();
471:             frame.pullStrength = pickedAmount(attackDefinition == null ? 0f : attackDefinition.pullStrength(), influence.pullStrength);
472:             frame.wetnessGrant = pickedGrant(attackDefinition == null ? 0 : attackDefinition.wetnessGrant(), influence.wetnessGrant);
473:             frame.pullAnchor = influence.pullAnchor != null ? influence.pullAnchor : player.position();
474:             frame.blocksMovement = attackDefinition != null && attackDefinition.blocksMovement();
475:             if ((attackDefinition != null && attackDefinition.purifyOnHit()) || influence.purify) {
476:                 frame.extinguish = true;
477:             }
478:             if (attackDefinition != null && attackDefinition.knockbackStrength() > frame.knockback) {
479:                 frame.knockback = attackDefinition.knockbackStrength();
480:             }
481:             if (attackDefinition != null) {
482:                 frame.liftStrength = attackDefinition.liftStrength();
483:             }
484:             if (heavySession != null) {
485:                 frame.heavyWater = heavySession.heavy();
486:                 frame.releaseGroup = heavySession.groupId();
487:             }
488:             if (contactOrigin != null) {
489:                 frame.overrideGeometry = true;
490:                 frame.origin = position;
491:                 frame.anchor = position;
492:                 frame.look = lookVec;
493:                 double flatReach = Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z);
494:                 frame.planeNormal = flatReach < 1.0E-4
495:                         ? new Vec3(0.0, 0.0, 1.0)
496:                         : new Vec3(lookVec.x / flatReach, 0.0, lookVec.z / flatReach);
497:                 frame.roll = 0.0F;
498:                 frame.pose3d = false;
499:                 frame.adoptSharedAnchor();
500:             }
501:             String batchMark = batch == null
502:                     ? "loose-" + player.serverLevel().getGameTime()
503:                     : Integer.toUnsignedString(System.identityHashCode(batch));
504:             PartingOffers.present(PartingOperationId.derive(player.getUUID(), batchMark, action.getId(), actionIndex, volley));
505:             CastFrame.bind(frame);
506:             try {
507:                 if (hostGroup != null) {
508:                     hostGroup.noteCaster(player, frame);
509:                 } else {
510:                     HitPayloads.applyCaster(player, frame);
511:                 }
512:                 TechniqueCastContext castContext = new TechniqueCastContext(usedDamage, lookVec, position, applied,
513:                         onFirstTrigger, frameLock, dropThisCast, actionSink);
514:                 effect.execute(player, action, weapon, castContext);
515:             } finally {
516:                 PartingOffers.clear();
517:                 CastFrame.unbind();
518:             }
519:         } finally {
520:             boolean holdHeavy = openedGroup != null && heavySession != null;
521:             if (holdHeavy) {
522:                 openedGroup.keepHeavy(heavySession);
523:             } else if (heavySession != null && copyIndex >= copyCount - 1) {
524:                 HeavyWater.finish(heavySession);
525:             }
526:             if (!holdHeavy) {
527:                 HeavyWater.clearCurrent();
528:             }
529:             if (batch != null) {
530:                 ReleaseBatch.bind(outer);
531:             }
532:         }
533:     }
534: 
535:     /**
536:      * 还没进宿主组的拍号术式，由第 0 份打开自己的父释放并一次提交全部复制。
537:      * 后一份直接交回，避免各自推进。已经在宿主组里的调用不重复包一层。
538:      */
539:     private static boolean commitLooseHost(ServerPlayer player, Technique action, List<Technique> techniques,
540:                                             int actionIndex, ItemStack weapon, float finalDamage, boolean carries,
541:                                             EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger,
542:                                             boolean lockThisCast, boolean dropThisCast,
543:                                             ReleaseBatch.Tally tally, int copyIndex, int copyCount, ReleaseBatch batch,
544:                                             EnchantBonus bonus, int volley, Vec3 lookVec, Vec3 position, CastSource castSource) {
545:         ReleaseDefinition definition = ReleaseRegistry.get(action.getId());
546:         if (definition == null || !definition.submitsHostGroup() || HostGroupContext.current() != null) {
547:             return false;
548:         }
549:         if (copyIndex > 0) {
550:             return true;
551:         }
552:         if (action.getType() == TechniqueType.SWORD) {
553:             player.swing(InteractionHand.MAIN_HAND, true);
554:         }
555:         HostCast hostCast = HostCast.open(player.getUUID(), castSource);
556:         hostCast.hold();
557:         try {
558:             UUID hostGroup = hostCast.hostId(volley, 0);
559:             commitHostGroup(player, action, techniques, actionIndex, weapon, finalDamage, carries, enchantments,
560:                     onFirstTrigger, lockThisCast, dropThisCast, tally, copyCount, batch, bonus, volley, lookVec, position,
561:                     null, null, hostCast, hostGroup, null);
562:         } finally {
563:             hostCast.release();
564:         }
565:         return true;
566:     }
567: 
568:     /** 术式自己的一份和场地的一份不叠加，取更大的那份。 */
569:     private static float pickedAmount(float fromDefinition, float fromField) {
570:         return fromDefinition >= fromField ? fromDefinition : fromField;
571:     }
572: 
573:     private static int pickedGrant(int fromDefinition, int fromField) {
574:         return fromDefinition >= fromField ? fromDefinition : fromField;
575:     }
576: }
577: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/network/SkillChainPlan.java">
1: package com.bslx.XueXiMod.network;
2: 
3: import com.bslx.XueXiMod.debug.DebugLog;
4: import com.bslx.XueXiMod.player.MagicPool;
5: import com.bslx.XueXiMod.player.ManaPaymentReceipt;
6: import com.bslx.XueXiMod.shu.Technique;
7: import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
8: import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
9: import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
10: import com.bslx.XueXiMod.shu.technique.enchant.EnchantBonus;
11: import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
12: import com.bslx.XueXiMod.shu.technique.enchant.WeaponEnchant;
13: import com.bslx.XueXiMod.shu.technique.release.ChainRole;
14: import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
15: import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
16: import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
17: import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
18: import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalEffects;
19: import com.bslx.XueXiMod.shu.technique.release.entity.HostCast;
20: import com.bslx.XueXiMod.shu.technique.release.water.CastSource;
21: import com.bslx.XueXiMod.shu.technique.release.entity.HostGroupCommit;
22: import com.bslx.XueXiMod.shu.technique.release.Trajectory;
23: import com.bslx.XueXiMod.shu.technique.release.guard.GuardStates;
24: import com.bslx.XueXiMod.shu.technique.release.momentum.ChargeGate;
25: import com.bslx.XueXiMod.skill.Skill;
26: import net.minecraft.server.level.ServerLevel;
27: import net.minecraft.server.level.ServerPlayer;
28: import net.minecraft.world.entity.ai.attributes.Attributes;
29: import net.minecraft.world.item.ItemStack;
30: import net.minecraft.world.phys.Vec3;
31: 
32: import java.util.ArrayList;
33: import java.util.List;
34: import java.util.UUID;
35: import java.util.concurrent.atomic.AtomicBoolean;
36: import java.util.function.Consumer;
37: 
38: final class SkillChainPlan {
39:     static int pressCommitCost(List<Technique> techniques, List<ChainSegment> segments) {
40:         return pressCommitCost(techniques, segments, 0);
41:     }
42: 
43:     static int pressCommitCost(List<Technique> techniques, List<ChainSegment> segments, int startIndex) {
44:         int cost = 0;
45:         int segmentIndex = Math.max(0, startIndex);
46:         while (segmentIndex < segments.size()) {
47:             ChainSegment segment = segments.get(segmentIndex);
48:             Technique action = techniques.get(segment.actionIndex());
49: 
50:             if (SkillSegmentMath.isRemovedRule(action.getId())) {
51:                 segmentIndex += 1;
52:                 continue;
53:             }
54:             ChainRole role = SkillSegmentMath.roleOf(action);
55:             if (role == ChainRole.ACTION) {
56:                 SkillChainPlan.ModifierPlan plan = collectModifiers(segments, techniques, segmentIndex + 1);
57:                 cost += SkillSegmentMath.segmentManaCost(techniques, segment);
58:                 if (plan.dualRuleSegment >= 0) {
59:                     cost += dualSequenceCost(techniques, segments, plan);
60:                     segmentIndex = plan.resume;
61:                 } else if (plan.attackSegment >= 0) {
62:                     int attackCopies = pricedCopies(techniques.get(segments.get(plan.attackSegment).actionIndex()), plan.copies);
63:                     cost += SkillSegmentMath.segmentManaCost(techniques, segments.get(plan.attackSegment)) * attackCopies * plan.volleys;
64:                     cost += fieldSpawnExtra(techniques, segments.get(plan.attackSegment).actionIndex(), attackCopies);
65:                     segmentIndex = plan.resume;
66:                 } else {
67:                     segmentIndex += 1;
68:                 }
69:                 continue;
70:             }
71:             if (SkillSegmentMath.isChainModifier(action)) {
72:                 SkillChainPlan.ModifierPlan plan = collectModifiers(segments, techniques, segmentIndex);
73:                 if (plan.dualRuleSegment >= 0) {
74:                     cost += dualSequenceCost(techniques, segments, plan);
75:                 } else if (plan.attackSegment >= 0) {
76:                     int attackCopies = pricedCopies(techniques.get(segments.get(plan.attackSegment).actionIndex()), plan.copies);
77:                     cost += SkillSegmentMath.segmentManaCost(techniques, segments.get(plan.attackSegment)) * attackCopies * plan.volleys;
78:                     cost += fieldSpawnExtra(techniques, segments.get(plan.attackSegment).actionIndex(), attackCopies);
79:                 }
80:                 segmentIndex = Math.max(plan.resume, segmentIndex + 1);
81:                 continue;
82:             }
83: 
84:             ChainSegment nextSegment = (segmentIndex + 1 < segments.size()) ? segments.get(segmentIndex + 1) : null;
85:             boolean chainedToTrigger = nextSegment != null
86:                     && SkillSegmentMath.releaseKind(action.getId()) == ReleaseKind.ACTION
87:                     && SkillSegmentMath.releaseKind(techniques.get(nextSegment.actionIndex()).getId()) != ReleaseKind.RULE
88:                     && SkillSegmentMath.isTriggerRelease(techniques.get(nextSegment.actionIndex()).getId())
89:                     && !SkillSegmentMath.manipulatesSwordEntities(techniques.get(nextSegment.actionIndex()).getId());
90: 
91:             if (chainedToTrigger) {
92:                 cost += SkillSegmentMath.segmentManaCost(techniques, segment);
93:                 segmentIndex += 2;
94:                 continue;
95:             }
96: 
97:             cost += SkillSegmentMath.segmentManaCost(techniques, segment);
98:             cost += fieldSpawnExtra(techniques, segment.actionIndex(), copiesBefore(segments, techniques, segmentIndex));
99:             segmentIndex += 1;
100:         }
101:         return cost;
102:     }
103: 
104:     static SkillChainPlan.ModifierPlan collectModifiers(List<ChainSegment> segments, List<Technique> techniques, int from) {
105:         SkillChainPlan.ModifierPlan plan = new SkillChainPlan.ModifierPlan();
106:         int segmentIndex = from;
107:         while (segmentIndex < segments.size()) {
108:             Technique technique = techniques.get(segments.get(segmentIndex).actionIndex());
109:             if (SkillSegmentMath.isRemovedRule(technique.getId())) {
110:                 segmentIndex++;
111:                 continue;
112:             }
113:             ChainRole role = SkillSegmentMath.roleOf(technique);
114:             if (technique.getBaseDamage() > 0 && !technique.isEnchantment()) {
115:                 plan.attackSegment = segmentIndex;
116:                 segmentIndex++;
117:                 break;
118:             }
119:             if (!SkillSegmentMath.isChainModifier(technique)) {
120:                 break;
121:             }
122:             switch (role) {
123:                 case SEEK_SPACE -> plan.spaceLock = true;
124:                 case SEEK_ENTITY -> plan.entityLock = true;
125:                 case TRAJECTORY_DROP -> {
126:                     ReleaseDefinition pathDefinition = ReleaseRegistry.get(technique.getId());
127:                     plan.pathOverride = pathDefinition != null ? pathDefinition.trajectory() : null;
128:                     plan.drop = plan.pathOverride == Trajectory.DROP;
129:                 }
130:                 case TRAJECTORY_TRACKING -> plan.tracking = true;
131:                 case TRAJECTORY_SCATTER -> plan.scatter = true;
132:                 case RULE_TRIPLE -> plan.copies *= 3;
133:                 case RULE_DUAL -> plan.dualRuleSegment = segmentIndex;
134:                 default -> {
135:                 }
136:             }
137:             segmentIndex++;
138:         }
139:         plan.resume = segmentIndex;
140:         if (plan.dualRuleSegment >= 0) {
141:             List<ChainSegment> released = releasedSegments(segments, plan.dualRuleSegment, techniques);
142:             int lastSegment = plan.dualRuleSegment;
143:             for (ChainSegment member : released) {
144:                 int memberIndex = segments.indexOf(member);
145:                 if (memberIndex > lastSegment) {
146:                     lastSegment = memberIndex;
147:                 }
148:             }
149:             plan.resume = Math.max(plan.resume, lastSegment + 1);
150:         }
151:         return plan;
152:     }
153: 
154:     static int firePlan(ServerPlayer player, Skill skill, List<Technique> techniques, List<ChainSegment> segments,
155:                           SkillChainPlan.ModifierPlan plan, float weaponBonus, int startTick, ManaPaymentReceipt receipt,
156:                           CastSource castSource) {
157:         if (plan.dualRuleSegment >= 0) {
158:             return fireDualSequence(player, skill, techniques, segments, plan, weaponBonus, startTick, castSource);
159:         }
160:         int attackShare = SkillSegmentMath.segmentManaCost(techniques, segments.get(plan.attackSegment));
161:         int memberCount = Math.max(0, plan.volleys) * Math.max(0, plan.copies);
162:         return firePaidPlan(player, skill, techniques, segments, plan, weaponBonus, startTick,
163:                 UncommittedShare.reserve(receipt, attackShare, memberCount), castSource);
164:     }
165: 
166:     static int firePaidPlan(ServerPlayer player, Skill skill, List<Technique> techniques, List<ChainSegment> segments,
167:                              SkillChainPlan.ModifierPlan plan, float weaponBonus, int startTick, UncommittedShare.Pool shares,
168:                              CastSource castSource) {
169:         ChainSegment attackSeg = segments.get(plan.attackSegment);
170:         Technique attack = techniques.get(attackSeg.actionIndex());
171:         EnchantmentResolution enchantments = SkillSegmentMath.resolveSegmentEnchantments(skill, attackSeg);
172:         EnchantBonus bonus = EnchantBonus.resolve(skill, attackSeg.enchantIndexes(), attack.getBaseDamage(), 1);
173:         float damage = attackSeg.enchantIndexes().isEmpty()
174:                 ? SkillSegmentMath.segmentDamage(attack, attackSeg, weaponBonus, SkillSegmentMath.segmentMagicDamage(skill, attackSeg), null)
175:                 : bonus.carriedDamage(weaponBonus);
176:         boolean carries = !attackSeg.enchantIndexes().isEmpty();
177:         ReleaseDefinition definition = ReleaseRegistry.get(attack.getId());
178:         boolean mutable = definition != null && definition.trajectoryMutable();
179:         boolean dedicatedAim = definition != null && definition.dedicatedInitialAim();
180:         ReleaseBatch batch = ReleaseBatch.current();
181:         ItemStack weapon = player.getMainHandItem();
182:         ServerLevel castLevel = player.serverLevel();
183:         int castEpoch = ChargeGate.epoch(player.getUUID());
184:         int finishedTick = startTick + Math.max(0, plan.volleys - 1) * DUAL_STEP_TICKS;
185:         if (shares == null || shares.isClosed()) {
186:             return finishedTick;
187:         }
188:         if (definition != null && definition.submitsHostGroup()) {
189:             HostCast hostCast = HostCast.open(player.getUUID(), castSource);
190:             hostCast.hold();
191:             try {
192:                 for (int volley = 0; volley < plan.volleys; volley++) {
193:                     int volleyIndex = volley;
194:                     List<UncommittedShare> hostClaims = new ArrayList<>();
195:                     boolean completeHost = true;
196:                     for (int copy = 0; copy < plan.copies; copy++) {
197:                         UncommittedShare claim = shares.take();
198:                         if (claim == null) {
199:                             completeHost = false;
200:                             break;
201:                         }
202:                         hostClaims.add(claim);
203:                     }
204:                     if (!completeHost) {
205:                         for (UncommittedShare claim : hostClaims) {
206:                             claim.cancel(player);
207:                         }
208:                         return finishedTick;
209:                     }
210:                     ReleaseBatch.Tally tally = plan.copies > 1 && batch != null ? batch.openGroupTally() : null;
211:                     int tick = startTick + volleyIndex * DUAL_STEP_TICKS;
212:                     hostCast.hold();
213:                     UUID hostGroup = hostCast.hostId(0, volleyIndex);
214:                     AtomicBoolean once = new AtomicBoolean(false);
215:                     AtomicBoolean released = new AtomicBoolean(false);
216:                     Runnable releaseVolley = () -> {
217:                         if (released.compareAndSet(false, true)) {
218:                             hostCast.release();
219:                         }
220:                     };
221:                     try {
222:                         ChainScheduler.schedule(player.getServer(), tick, () -> {
223:                             if (!once.compareAndSet(false, true)) {
224:                                 return;
225:                             }
226:                             try {
227:                                 int nowTick = player.getServer() == null ? tick : player.getServer().getTickCount();
228:                                 if (!castContinues(player, castLevel, castEpoch) || !HeldSwordGate.allows(attack, player.getMainHandItem())) {
229:                                     HostGroupCommit.cancel(hostCast, hostGroup, nowTick, "施法已经中断");
230:                                     for (UncommittedShare claim : hostClaims) {
231:                                         claim.cancel(player);
232:                                     }
233:                                     return;
234:                                 }
235:                                 ReleaseOutcome outcome = SkillSegmentRunner.commitHostGroup(player, attack, techniques, attackSeg.actionIndex(), weapon,
236:                                         damage, carries, enchantments,
237:                                         plan.entityLock && (mutable || dedicatedAim), plan.drop && mutable,
238:                                         tally, plan.copies, batch, bonus, volleyIndex, null, null, null, null, hostCast, hostGroup, null);
239:                                 if (outcome.committed()) {
240:                                     for (UncommittedShare claim : hostClaims) {
241:                                         claim.commit();
242:                                     }
243:                                 } else {
244:                                     for (UncommittedShare claim : hostClaims) {
245:                                         claim.cancel(player);
246:                                     }
247:                                 }
248:                             } finally {
249:                                 releaseVolley.run();
250:                             }
251:                         });
252:                     } catch (RuntimeException failure) {
253:                         releaseVolley.run();
254:                         throw failure;
255:                     }
256:                 }
257:                 return finishedTick;
258:             } finally {
259:                 hostCast.release();
260:             }
261:         }
262:         for (int volley = 0; volley < plan.volleys; volley++) {
263:             ReleaseBatch.Tally tally = plan.copies > 1 && batch != null ? batch.openGroupTally() : null;
264:             int tick = startTick + volley * DUAL_STEP_TICKS;
265:             for (int copy = 0; copy < plan.copies; copy++) {
266:                 int copyIndex = copy;
267:                 int volleyIndex = volley;
268:                 UncommittedShare claim = shares.take();
269:                 if (claim == null) {
270:                     return finishedTick;
271:                 }
272:                 AtomicBoolean once = new AtomicBoolean(false);
273:                 Runnable runner = SkillSegmentRunner.buildSegmentRunner(player, attack, techniques, attackSeg.actionIndex(), weapon,
274:                         damage, carries, enchantments, null,
275:                         plan.entityLock && (mutable || dedicatedAim), plan.drop && mutable,
276:                         tally, copyIndex, plan.copies, batch,
277:                         bonus, volleyIndex, castSource);
278:                 ChainScheduler.schedule(player.getServer(), tick, () -> {
279:                     if (!once.compareAndSet(false, true)) {
280:                         return;
281:                     }
282:                     if (!castContinues(player, castLevel, castEpoch) || !HeldSwordGate.allows(attack, player.getMainHandItem())) {
283:                         claim.cancel(player);
284:                         return;
285:                     }
286:                     runner.run();
287:                     claim.commit();
288:                 });
289:             }
290:         }
291:         return finishedTick;
292:     }
293: 
294:     static int fireDualSequence(ServerPlayer player, Skill skill, List<Technique> techniques, List<ChainSegment> segments,
295:                                   SkillChainPlan.ModifierPlan plan, float weaponBonus, int startTick, CastSource castSource) {
296:         List<ChainSegment> released = releasedSegments(segments, plan.dualRuleSegment, techniques);
297:         int lastTick = startTick;
298:         int attackOrdinal = 0;
299:         ReleaseBatch batch = ReleaseBatch.current();
300:         ItemStack weapon = player.getMainHandItem();
301:         boolean dualEnchantImprinted = false;
302:         for (ChainSegment member : released) {
303:             Technique memberTechnique = techniques.get(member.actionIndex());
304:             ReleaseDefinition memberDefinition = ReleaseRegistry.get(memberTechnique.getId());
305:             if (memberTechnique.getBaseDamage() <= 0f && memberDefinition != null && memberDefinition.domain() != null) {
306:                 int sequenceTick = startTick + attackOrdinal * DUAL_STEP_TICKS;
307:                 int memberCopies = copiesBefore(segments, techniques, segments.indexOf(member));
308:                 EnchantmentSnapshot domainSnapshot = ContactCast.snapshot(skill, member, player);
309:                 boolean placedNow = sequenceTick <= player.getServer().getTickCount();
310:                 ServerLevel domainLevel = player.serverLevel();
311:                 int domainEpoch = ChargeGate.epoch(player.getUUID());
312:                 if (placedNow && !com.bslx.XueXiMod.shu.technique.release.domain.DomainService.tryWave(
313:                         player, memberTechnique, memberDefinition.domain(), memberCopies, domainSnapshot)) {
314:                     if (attackOrdinal == 0) {
315:                         return -1;
316:                     }
317:                     continue;
318:                 }
319:                 if (!placedNow) {
320:                     ChainScheduler.schedule(player.getServer(), sequenceTick, () -> {
321:                         if (!castContinues(player, domainLevel, domainEpoch)) {
322:                             return;
323:                         }
324:                         com.bslx.XueXiMod.shu.technique.release.domain.DomainService.tryWave(
325:                                 player, memberTechnique, memberDefinition.domain(), memberCopies, domainSnapshot);
326:                     });
327:                 }
328:                 lastTick = sequenceTick;
329:                 attackOrdinal++;
330:                 continue;
331:             }
332:             if (memberTechnique.getBaseDamage() <= 0f && memberDefinition != null && memberDefinition.barrier() != null) {
333:                 int sequenceTick = startTick + attackOrdinal * DUAL_STEP_TICKS;
334:                 int memberCopies = copiesBefore(segments, techniques, segments.indexOf(member));
335:                 EnchantmentSnapshot barrierSnapshot = ContactCast.snapshot(skill, member, player);
336:                 boolean placedNow = sequenceTick <= player.getServer().getTickCount();
337:                 ServerLevel barrierLevel = player.serverLevel();
338:                 int barrierEpoch = ChargeGate.epoch(player.getUUID());
339:                 if (placedNow && !com.bslx.XueXiMod.shu.technique.release.barrier.BarrierService.tryPlace(
340:                         player, memberDefinition.barrier(), memberCopies, barrierSnapshot)) {
341:                     if (attackOrdinal == 0) {
342:                         return -1;
343:                     }
344:                     continue;
345:                 }
346:                 if (!placedNow) {
347:                     ChainScheduler.schedule(player.getServer(), sequenceTick, () -> {
348:                         if (!castContinues(player, barrierLevel, barrierEpoch)) {
349:                             return;
350:                         }
351:                         com.bslx.XueXiMod.shu.technique.release.barrier.BarrierService.tryPlace(
352:                                 player, memberDefinition.barrier(), memberCopies, barrierSnapshot);
353:                     });
354:                 }
355:                 lastTick = sequenceTick;
356:                 attackOrdinal++;
357:                 continue;
358:             }
359:             if (memberTechnique.getBaseDamage() <= 0f && memberDefinition != null && memberDefinition.guard() != null) {
360:                 int sequenceTick = startTick + attackOrdinal * DUAL_STEP_TICKS;
361:                 long waveKey = ((long) startTick << 8) + member.actionIndex() + attackOrdinal * 32L;
362:                 if (!GuardStates.schedule(player, memberDefinition.guard(), sequenceTick, waveKey)) {
363:                     if (attackOrdinal == 0) {
364:                         return -1;
365:                     }
366:                     continue;
367:                 }
368:                 EnchantmentSnapshot guardSnapshot = ContactCast.snapshot(skill, member, player);
369:                 ServerLevel guardLevel = player.serverLevel();
370:                 int guardEpoch = ChargeGate.epoch(player.getUUID());
371:                 if (guardSnapshot != null) {
372:                     ChainScheduler.schedule(player.getServer(), sequenceTick, () -> {
373:                         if (!castContinues(player, guardLevel, guardEpoch)) {
374:                             return;
375:                         }
376:                         GuardStates.bindContact(player, guardSnapshot);
377:                     });
378:                 }
379:                 lastTick = sequenceTick;
380:                 attackOrdinal++;
381:                 continue;
382:             }
383:             if (memberTechnique.isEnchantment()) {
384:                 if (!dualEnchantImprinted) {
385:                     int memberCopies = copiesBefore(segments, techniques, segments.indexOf(member));
386:                     java.util.ArrayList<Integer> loose = new java.util.ArrayList<>();
387:                     for (ChainSegment releasedMember : released) {
388:                         if (techniques.get(releasedMember.actionIndex()).isEnchantment()) {
389:                             loose.add(releasedMember.actionIndex());
390:                         }
391:                     }
392:                     imprintSelection(player, skill, loose, Math.max(1, memberCopies));
393:                     dualEnchantImprinted = true;
394:                 }
395:                 lastTick = startTick + attackOrdinal * DUAL_STEP_TICKS;
396:                 attackOrdinal++;
397:                 continue;
398:             }
399:             int memberIndex = segments.indexOf(member);
400:             int memberCopies = copiesBefore(segments, techniques, memberIndex);
401:             EnchantmentResolution enchantments = SkillSegmentMath.resolveSegmentEnchantments(skill, member);
402:             EnchantBonus bonus = EnchantBonus.resolve(skill, member.enchantIndexes(), memberTechnique.getBaseDamage(), 1);
403:             float damage = member.enchantIndexes().isEmpty()
404:                     ? SkillSegmentMath.segmentDamage(memberTechnique, member, weaponBonus, SkillSegmentMath.segmentMagicDamage(skill, member), null)
405:                     : bonus.carriedDamage(weaponBonus);
406:             ReleaseDefinition definition = ReleaseRegistry.get(memberTechnique.getId());
407:             EnchantmentSnapshot fieldSnapshot = memberTechnique.getBaseDamage() <= 0f
408:                     && definition != null && definition.fieldRadius() > 0f
409:                     ? ContactCast.snapshot(skill, member, player) : null;
410:             boolean carries = fieldSnapshot == null && memberTechnique.getBaseDamage() > 0f && !member.enchantIndexes().isEmpty();
411:             boolean mutable = definition != null && definition.trajectoryMutable();
412:             boolean lockCast = (plan.entityLock && mutable) || lockThisCast(techniques, member.actionIndex());
413:             boolean dropCast = (plan.drop && mutable) || dropThisCast(techniques, segments, member.actionIndex());
414:             int sequenceTick = startTick + attackOrdinal * DUAL_STEP_TICKS;
415:             lastTick = sequenceTick;
416:             ReleaseBatch.Tally tally = memberCopies > 1 && batch != null ? batch.openGroupTally() : null;
417:             ServerLevel castLevel = player.serverLevel();
418:             for (int copy = 0; copy < memberCopies; copy++) {
419:                 int copyIndex = copy;
420:                 int volleyIndex = attackOrdinal;
421:                 Runnable runner = SkillSegmentRunner.buildSegmentRunner(player, memberTechnique, techniques, member.actionIndex(), weapon,
422:                         damage, carries, enchantments, null,
423:                         lockCast, dropCast,
424:                         tally, copyIndex, memberCopies, batch,
425:                         bonus, volleyIndex, castSource);
426:                 ChainScheduler.schedule(player.getServer(), sequenceTick, () -> {
427:                     if (!player.isAlive() || player.level() != castLevel) {
428:                         return;
429:                     }
430:                     runner.run();
431:                     if (fieldSnapshot != null && copyIndex == 0) {
432:                         com.bslx.XueXiMod.shu.technique.release.field.FieldZones.bindBorn(
433:                                 player.getUUID(), fieldSnapshot, player.getServer().getTickCount());
434:                     }
435:                 });
436:             }
437:             attackOrdinal++;
438:         }
439:         return lastTick;
440:     }
441: 
442:     static int dualSequenceCost(List<Technique> techniques, List<ChainSegment> segments, SkillChainPlan.ModifierPlan plan) {
443:         int cost = 0;
444:         List<ChainSegment> released = releasedSegments(segments, plan.dualRuleSegment, techniques);
445:         for (ChainSegment member : released) {
446:             Technique memberTechnique = techniques.get(member.actionIndex());
447:             ReleaseDefinition memberDefinition = ReleaseRegistry.get(memberTechnique.getId());
448:             if (memberTechnique.getBaseDamage() <= 0f && memberDefinition != null
449:                     && (memberDefinition.guard() != null || memberDefinition.barrier() != null || memberDefinition.domain() != null
450:                     || ConditionalEffects.copiesCoalesce(memberDefinition))) {
451:                 cost += SkillSegmentMath.segmentManaCost(techniques, member);
452:                 continue;
453:             }
454:             int memberCopies = copiesBefore(segments, techniques, segments.indexOf(member));
455:             cost += SkillSegmentMath.segmentManaCost(techniques, member) * memberCopies;
456:             if (memberDefinition != null && memberDefinition.fieldRadius() > 0f) {
457:                 cost += fieldSpawnExtra(techniques, member.actionIndex(), memberCopies);
458:             }
459:         }
460:         return cost;
461:     }
462: 
463:     private static int pricedCopies(Technique technique, int copies) {
464:         if (technique != null && technique.getBaseDamage() <= 0f
465:                 && ConditionalEffects.copiesCoalesce(ReleaseRegistry.get(technique.getId()))) {
466:             return 1;
467:         }
468:         return copies;
469:     }
470: 
471:     private static int fieldSpawnExtra(List<Technique> techniques, int actionIndex, int copies) {
472:         ReleaseDefinition definition = ReleaseRegistry.get(techniques.get(actionIndex).getId());
473:         if (definition == null || definition.fieldRadius() <= 0f || definition.fieldSpawnCost() <= 0 || copies <= 0) {
474:             return 0;
475:         }
476:         return definition.fieldSpawnCost() * copies;
477:     }
478: 
479:     static int copiesBefore(List<ChainSegment> segments, List<Technique> techniques, int segmentIndex) {
480:         int copies = 1;
481:         for (int earlier = segmentIndex - 1; earlier >= 0; earlier--) {
482:             Technique earlierTechnique = techniques.get(segments.get(earlier).actionIndex());
483:             if (earlierTechnique.getBaseDamage() > 0 && !earlierTechnique.isEnchantment()) {
484:                 break;
485:             }
486:             if (SkillSegmentMath.roleOf(earlierTechnique) == ChainRole.RULE_TRIPLE) {
487:                 copies *= 3;
488:             }
489:         }
490:         return copies;
491:     }
492: 
493:     static float chainEnergyBefore(List<Technique> techniques, int actionIndex) {
494:         float gathered = 1f;
495:         for (int earlier = actionIndex - 1; earlier >= 0; earlier--) {
496:             Technique earlierTechnique = techniques.get(earlier);
497:             if (earlierTechnique.getBaseDamage() > 0f && !earlierTechnique.isEnchantment()) {
498:                 if (SkillSegmentMath.sharesDualRelease(techniques, earlier, actionIndex)) {
499:                     continue;
500:                 }
501:                 break;
502:             }
503:             ReleaseDefinition definition = ReleaseRegistry.get(earlierTechnique.getId());
504:             if (definition == null) {
505:                 continue;
506:             }
507:             float supplied = definition.chainEnergyScale();
508:             if (!Float.isFinite(supplied) || supplied <= 0f || Math.abs(supplied - 1f) <= 0.001f) {
509:                 continue;
510:             }
511:             gathered *= supplied;
512:         }
513:         return gathered;
514:     }
515: 
516:     static Technique closingAttack(List<Technique> techniques, List<ChainSegment> segments, ModifierPlan plan) {
517:         if (plan.dualRuleSegment >= 0) {
518:             Technique closing = null;
519:             for (ChainSegment member : releasedSegments(segments, plan.dualRuleSegment, techniques)) {
520:                 Technique memberTechnique = techniques.get(member.actionIndex());
521:                 if (memberTechnique.getBaseDamage() > 0f && !memberTechnique.isEnchantment()) {
522:                     closing = memberTechnique;
523:                 }
524:             }
525:             return closing;
526:         }
527:         if (plan.attackSegment < 0) {
528:             return null;
529:         }
530:         return techniques.get(segments.get(plan.attackSegment).actionIndex());
531:     }
532: 
533:     static void imprintEnchant(ServerPlayer player, Skill skill, List<Technique> techniques, int layers) {
534:         java.util.ArrayList<Integer> indexes = new java.util.ArrayList<>();
535:         for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
536:             if (techniques.get(techniqueIndex).isEnchantment()) {
537:                 indexes.add(techniqueIndex);
538:             }
539:         }
540:         imprintSelection(player, skill, indexes, layers);
541:     }
542: 
543:     static void imprintSelection(ServerPlayer player, Skill skill, List<Integer> selected, int layers) {
544:         java.util.ArrayList<Integer> indexes = new java.util.ArrayList<>();
545:         List<Technique> techniques = skill.getTechniques();
546:         for (int techniqueIndex : selected) {
547:             if (techniqueIndex < 0 || techniqueIndex >= techniques.size() || !techniques.get(techniqueIndex).isEnchantment()) {
548:                 continue;
549:             }
550:             ReleaseDefinition definition = ReleaseRegistry.get(techniques.get(techniqueIndex).getId());
551:             if (definition != null && definition.fieldRadius() > 0f) {
552:                 continue;
553:             }
554:             indexes.add(techniqueIndex);
555:         }
556:         if (indexes.isEmpty()) {
557:             return;
558:         }
559:         float weaponAttack = (float) player.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
560:         EnchantBonus bonus = EnchantBonus.resolve(skill, indexes, weaponAttack, Math.max(1, layers));
561:         WeaponEnchant.imprint(player, player.getMainHandItem(), bonus);
562:     }
563: 
564:     private static boolean castContinues(ServerPlayer player, ServerLevel castLevel, int castEpoch) {
565:         return player.isAlive() && player.level() == castLevel && ChargeGate.same(player.getUUID(), castEpoch);
566:     }
567: 
568:     static int findTripleAttack(List<ChainSegment> segments, int ruleIndex, List<Technique> techniques) {
569:         for (int segmentIndex = ruleIndex + 1; segmentIndex < segments.size(); segmentIndex++) {
570:             ChainSegment seg = segments.get(segmentIndex);
571:             Technique action = techniques.get(seg.actionIndex());
572:             if (SkillSegmentMath.releaseKind(action.getId()) == ReleaseKind.RULE) {
573:                 DebugLog.log("rule_triple_cast 先遇到其它规则术式，停止");
574:                 return -1;
575:             }
576:             if (action.getBaseDamage() > 0 && !action.isEnchantment()) {
577:                 return segmentIndex;
578:             }
579:         }
580:         return -1;
581:     }
582: 
583:     static boolean lockThisCast(List<Technique> techniques, int actionIndex) {
584:         ReleaseDefinition definition = ReleaseRegistry.get(techniques.get(actionIndex).getId());
585:         if (definition == null || !definition.acceptsInitialAim()) {
586:             return false;
587:         }
588:         for (int earlierIndex = actionIndex - 1; earlierIndex >= 0; earlierIndex--) {
589:             ReleaseDefinition earlier = ReleaseRegistry.get(techniques.get(earlierIndex).getId());
590:             if (earlier == null) {
591:                 continue;
592:             }
593:             if (earlier.setsTargetForNextDirectional()) {
594:                 return true;
595:             }
596:             if (earlier.acceptsTargetOverride() || earlier.acceptsInitialAim()) {
597:                 if (SkillSegmentMath.sharesDualRelease(techniques, earlierIndex, actionIndex)) {
598:                     continue;
599:                 }
600:                 return false;
601:             }
602:         }
603:         return false;
604:     }
605: 
606:     static boolean dropThisCast(List<Technique> techniques, List<ChainSegment> segments, int actionIndex) {
607:         ReleaseDefinition definition = ReleaseRegistry.get(techniques.get(actionIndex).getId());
608:         if (definition == null || !definition.acceptsPathOverride()) {
609:             return false;
610:         }
611:         List<Integer> sameRelease = sameReleaseGroup(segments, actionIndex, techniques);
612:         for (int earlierIndex = actionIndex - 1; earlierIndex >= 0; earlierIndex--) {
613:             ReleaseDefinition earlier = ReleaseRegistry.get(techniques.get(earlierIndex).getId());
614:             if (earlier == null) {
615:                 continue;
616:             }
617:             if (earlier.setsDropForNextDirectional()) {
618:                 return true;
619:             }
620:             if (earlier.acceptsPathOverride()) {
621:                 if (sameRelease != null && sameRelease.contains(earlierIndex)) {
622:                     continue;
623:                 }
624:                 return false;
625:             }
626:         }
627:         return false;
628:     }
629: 
630:     static List<Integer> sameReleaseGroup(List<ChainSegment> segments, int actionIndex, List<Technique> techniques) {
631:         for (int ruleIndex = 0; ruleIndex < segments.size(); ruleIndex++) {
632:             ChainSegment ruleSegment = segments.get(ruleIndex);
633:             if (SkillSegmentMath.releaseKind(techniques.get(ruleSegment.actionIndex()).getId()) != ReleaseKind.RULE) {
634:                 continue;
635:             }
636:             List<ChainSegment> released = releasedSegments(segments, ruleIndex, techniques);
637:             boolean found = false;
638:             for (ChainSegment seg : released) {
639:                 if (seg.actionIndex() == actionIndex) {
640:                     found = true;
641:                     break;
642:                 }
643:             }
644:             if (!found) {
645:                 continue;
646:             }
647:             List<Integer> group = new ArrayList<>();
648:             for (ChainSegment seg : released) {
649:                 group.add(seg.actionIndex());
650:             }
651:             return group;
652:         }
653:         return null;
654:     }
655: 
656:     static int ruleConsumedCount(List<ChainSegment> segments, int ruleIndex, List<Technique> techniques) {
657:         int count = 1;
658:         if (ruleIndex + 1 >= segments.size()) {
659:             return count;
660:         }
661:         count += 1;
662:         ReleaseDefinition rule = ReleaseRegistry.get(techniques.get(segments.get(ruleIndex).actionIndex()).getId());
663:         int pullAfterA = rule != null ? rule.pullAfterA() : 0;
664:         count += pulledCount(pullAfterA, segments, ruleIndex);
665:         return count;
666:     }
667: 
668:     static int pulledCount(int pullAfterA, List<ChainSegment> segments, int ruleIndex) {
669:         int available = Math.max(0, segments.size() - (ruleIndex + 2));
670:         return pullAfterA < 0 ? available : Math.min(pullAfterA, available);
671:     }
672: 
673:     static List<ChainSegment> releasedSegments(List<ChainSegment> segments, int ruleIndex, List<Technique> techniques) {
674:         List<ChainSegment> released = new ArrayList<>();
675:         if (ruleIndex < 0 || ruleIndex + 1 >= segments.size()) {
676:             return released;
677:         }
678:         ReleaseDefinition rule = ReleaseRegistry.get(techniques.get(segments.get(ruleIndex).actionIndex()).getId());
679:         int pull = rule != null ? rule.pullAfterA() : 0;
680:         int room = pull < 0 ? 64 : pull + 1;
681:         for (int followIndex = ruleIndex + 1; followIndex < segments.size() && released.size() < room; followIndex++) {
682:             ChainSegment candidate = segments.get(followIndex);
683:             Technique technique = techniques.get(candidate.actionIndex());
684:             if (SkillSegmentMath.isRemovedRule(technique.getId()) || SkillSegmentMath.isChainModifier(technique)) {
685:                 continue;
686:             }
687:             released.add(candidate);
688:         }
689:         return released;
690:     }
691: 
692:     static final class ModifierPlan {
693:         boolean spaceLock;
694:         boolean entityLock;
695:         boolean drop;
696:         Trajectory pathOverride;
697:         boolean tracking;
698:         boolean scatter;
699:         int copies = 1;
700:         int volleys = 1;
701:         int dualRuleSegment = -1;
702:         int attackSegment = -1;
703:         int resume;
704:     }
705: 
706:     static final int DUAL_STEP_TICKS = 5;
707: }
708: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/network/CastExecutionSession.java">
1: package com.bslx.XueXiMod.network;
2: 
3: import com.bslx.XueXiMod.debug.DebugLog;
4: import com.bslx.XueXiMod.player.ManaPaymentReceipt;
5: import com.bslx.XueXiMod.shu.Technique;
6: import com.bslx.XueXiMod.shu.technique.ActionWindow;
7: import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
8: import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
9: import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
10: import com.bslx.XueXiMod.shu.technique.enchant.EnchantBonus;
11: import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
12: import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
13: import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
14: import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
15: import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalIntent;
16: import com.bslx.XueXiMod.shu.technique.release.entity.HostCast;
17: import com.bslx.XueXiMod.shu.technique.release.water.CastSource;
18: import com.bslx.XueXiMod.shu.technique.release.entity.HostGroupIds;
19: import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierService;
20: import com.bslx.XueXiMod.shu.technique.release.domain.DomainService;
21: import com.bslx.XueXiMod.shu.technique.release.field.FieldZones;
22: import com.bslx.XueXiMod.shu.technique.release.guard.GuardStates;
23: import com.bslx.XueXiMod.shu.technique.release.momentum.ChargeGate;
24: import com.bslx.XueXiMod.skill.Skill;
25: import net.minecraft.network.chat.Component;
26: import net.minecraft.server.level.ServerLevel;
27: import net.minecraft.server.level.ServerPlayer;
28: import net.minecraft.world.entity.Entity;
29: import net.minecraft.world.entity.LivingEntity;
30: import net.minecraft.world.item.ItemStack;
31: import net.minecraft.world.phys.Vec3;
32: 
33: import java.util.ArrayList;
34: import java.util.HashSet;
35: import java.util.List;
36: import java.util.Set;
37: import java.util.UUID;
38: import java.util.concurrent.ConcurrentHashMap;
39: 
40: /**
41:  * 双重释放的父会话。执行锁和正式冷却分开。游标和冷却只在终结时各做一次。
42:  */
43: public final class CastExecutionSession implements ActionWindow.Sink {
44:     static final int STEP_GAP = 5;
45: 
46:     private static final ConcurrentHashMap<String, CastExecutionSession> OPEN = new ConcurrentHashMap<>();
47: 
48:     private final ServerPlayer owner;
49:     private final ItemStack carrier;
50:     private final int slot;
51:     private final CastSource castSource;
52:     private final String carrierKey;
53:     private final Skill skill;
54:     private final List<Technique> techniques;
55:     private final List<ChainSegment> segments;
56:     private final CompiledReleaseStep plan;
57:     private final float weaponBonus;
58:     private final String signature;
59:     private final int stepCount;
60:     private final ServerLevel castLevel;
61:     private final int castEpoch;
62:     private final Set<UUID> processedTargets = new HashSet<>();
63:     private final List<Group> groups = new ArrayList<>();
64: 
65:     private ManaPaymentReceipt actionReceipt;
66:     private boolean accepted;
67:     private boolean actionEnded;
68:     private boolean legalCollision;
69:     private boolean terminalAlready;
70:     private boolean closed;
71:     private boolean reserveFailed;
72:     private int pending;
73:     private HostCast hostCast;
74:     private final Set<String> conditionalUsage = new HashSet<>();
75: 
76:     private CastExecutionSession(ServerPlayer owner, ItemStack carrier, int slot, CastSource castSource, String carrierKey, Skill skill,
77:                                  List<Technique> techniques, List<ChainSegment> segments, CompiledReleaseStep plan,
78:                                  float weaponBonus, String signature, int stepCount) {
79:         this.owner = owner;
80:         this.carrier = carrier;
81:         this.slot = slot;
82:         this.castSource = castSource;
83:         this.carrierKey = carrierKey;
84:         this.skill = skill;
85:         this.techniques = techniques;
86:         this.segments = segments;
87:         this.plan = plan;
88:         this.weaponBonus = weaponBonus;
89:         this.signature = signature;
90:         this.stepCount = stepCount;
91:         this.castLevel = owner.serverLevel();
92:         this.castEpoch = ChargeGate.epoch(owner.getUUID());
93:     }
94: 
95:     static CastTiming open(ServerPlayer owner, ItemStack carrier, int slot, CastSource castSource, Skill skill, List<Technique> techniques,
96:                            List<ChainSegment> segments, CompiledReleaseStep plan, float weaponBonus,
97:                            String signature, int stepCount) {
98:         if (plan.members.isEmpty()) {
99:             owner.sendSystemMessage(Component.literal("§c双重释放后面没有可放的术式，这次没有释放"));
100:             return CastTiming.rejected();
101:         }
102:         String carrierKey = CastCooldown.carrierKey(owner, carrier, slot);
103:         if (!CastCooldown.acquire(carrierKey)) {
104:             return CastTiming.rejected();
105:         }
106:         CastExecutionSession session = new CastExecutionSession(owner, carrier, slot, castSource, carrierKey, skill, techniques,
107:                 segments, plan, weaponBonus, signature, stepCount);
108:         OPEN.put(carrierKey, session);
109:         if (plan.actionLed) {
110:             return session.startAction();
111:         }
112:         return session.startOrdinary();
113:     }
114: 
115:     public static void cancelOwned(ServerPlayer owner) {
116:         if (owner == null) {
117:             return;
118:         }
119:         List<CastExecutionSession> living = new ArrayList<>(OPEN.values());
120:         for (CastExecutionSession session : living) {
121:             if (owner.getUUID().equals(session.owner.getUUID())) {
122:                 session.cancel();
123:             }
124:         }
125:     }
126: 
127:     public static void cancelAll() {
128:         List<CastExecutionSession> living = new ArrayList<>(OPEN.values());
129:         for (CastExecutionSession session : living) {
130:             session.cancel();
131:         }
132:     }
133: 
134:     private CastTiming startAction() {
135:         actionReceipt = ManaPaymentReceipt.charge(owner, plan.actionShare, true);
136:         if (actionReceipt == null) {
137:             dropUnaccepted();
138:             return CastTiming.insufficient();
139:         }
140:         ChainSegment action = segments.get(plan.actionSegment);
141:         Technique technique = techniques.get(action.actionIndex());
142:         if (!SkillSegmentRunner.playAction(owner, technique, techniques, action.actionIndex(), carrier, this)) {
143:             dropUnaccepted();
144:             owner.sendSystemMessage(Component.literal("§c动作没有开始，这次没有释放"));
145:             return CastTiming.rejected();
146:         }
147:         accepted = true;
148:         return CastTiming.deferred(owner.getServer().getTickCount(), proficiencyTechniques(techniques));
149:     }
150: 
151:     private CastTiming startOrdinary() {
152:         int nowTick = owner.getServer().getTickCount();
153:         UUID groupId = UUID.randomUUID();
154:         ActionContact contact = new ActionContact(owner.getUUID(), nowTick, owner.position(), owner.getLookAngle(), groupId);
155:         beginGroup(contact);
156:         if (!accepted) {
157:             boolean unpaid = reserveFailed;
158:             dropUnaccepted();
159:             return unpaid ? CastTiming.insufficient() : CastTiming.rejected();
160:         }
161:         return CastTiming.deferred(nowTick, proficiencyTechniques(techniques));
162:     }
163: 
164:     @Override
165:     public void onContact(UUID targetId, Vec3 point, Vec3 direction) {
166:         if (closed || terminalAlready || !plan.actionLed || actionEnded) {
167:             return;
168:         }
169:         Entity found = castLevel.getEntity(targetId);
170:         if (!(found instanceof LivingEntity living) || !ActionWindow.hostileTo(owner, living)) {
171:             return;
172:         }
173:         if (!processedTargets.add(targetId)) {
174:             return;
175:         }
176:         legalCollision = true;
177:         Vec3 aimed = direction == null || direction.lengthSqr() < 1.0E-8 ? owner.getLookAngle() : direction.normalize();
178:         Vec3 anchor = point == null ? living.position() : point;
179:         beginGroup(new ActionContact(targetId, owner.getServer().getTickCount(), anchor, aimed, UUID.randomUUID()));
180:     }
181: 
182:     @Override
183:     public void onEnded() {
184:         if (actionEnded || terminalAlready) {
185:             return;
186:         }
187:         actionEnded = true;
188:         tryFinish();
189:     }
190: 
191:     @Override
192:     public boolean closed() {
193:         return closed || terminalAlready;
194:     }
195: 
196:     private void cancel() {
197:         if (terminalAlready) {
198:             return;
199:         }
200:         closed = true;
201:         if (!accepted) {
202:             dropUnaccepted();
203:             return;
204:         }
205:         actionEnded = true;
206:         List<Group> pendingGroups = new ArrayList<>(groups);
207:         for (Group group : pendingGroups) {
208:             finishGroup(group);
209:         }
210:         tryFinish();
211:         closeHostLedger();
212:     }
213: 
214:     private boolean beginGroup(ActionContact contact) {
215:         if (closed || terminalAlready) {
216:             return false;
217:         }
218:         Group group = new Group(contact);
219:         groups.add(group);
220:         pending++;
221:         GroupBudget budget = GroupBudget.reserve(owner, plan.groupBill());
222:         if (budget == null) {
223:             reserveFailed = true;
224:             finishGroup(group);
225:             return false;
226:         }
227:         group.budget = budget;
228:         group.batch = ReleaseBatch.open(owner, skill);
229:         ReleaseBatch.unbind();
230:         ReleaseOutcome first = commitMember(group, 0);
231:         if (!first.committed()) {
232:             if (!first.reason().isEmpty()) {
233:                 owner.sendSystemMessage(Component.literal("§c" + first.reason()));
234:             }
235:             finishGroup(group);
236:             return plan.actionLed && accepted;
237:         }
238:         budget.commit(plan.ruleShare + plan.memberShares.get(0));
239:         accepted = true;
240:         if (!plan.actionLed) {
241:             legalCollision = true;
242:             actionEnded = true;
243:         }
244:         if (plan.members.size() < 2) {
245:             finishGroup(group);
246:             return true;
247:         }
248:         int secondTick = first.tick() + STEP_GAP;
249:         ChainScheduler.schedule(owner.getServer(), secondTick, () -> runSecond(group));
250:         return true;
251:     }
252: 
253:     private void runSecond(Group group) {
254:         if (group.closed || terminalAlready) {
255:             return;
256:         }
257:         if (closed || !continues()) {
258:             finishGroup(group);
259:             return;
260:         }
261:         ReleaseOutcome second = commitMember(group, 1);
262:         if (second.committed() && group.budget != null) {
263:             group.budget.commit(plan.memberShares.get(1));
264:         }
265:         finishGroup(group);
266:     }
267: 
268:     private ReleaseOutcome commitMember(Group group, int memberIndex) {
269:         int nowTick = owner.getServer() == null ? 0 : owner.getServer().getTickCount();
270:         if (!continues()) {
271:             return ReleaseOutcome.cancelled(nowTick, "施法已经中断");
272:         }
273:         ChainSegment member = plan.members.get(memberIndex);
274:         Technique technique = techniques.get(member.actionIndex());
275:         Vec3 point = group.contact.point();
276:         Vec3 look = group.contact.direction();
277:         if (technique.isEnchantment()) {
278:             SkillChainPlan.imprintSelection(owner, skill, List.of(member.actionIndex()), 1);
279:             return ReleaseOutcome.committed(nowTick);
280:         }
281:         ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
282:         int copies = Math.max(1, SkillChainPlan.copiesBefore(segments, techniques, segments.indexOf(member)));
283:         EnchantmentSnapshot snapshot = ContactCast.snapshot(skill, member, owner);
284:         String claimSuffix = group.contact.groupId().toString();
285:         if (definition != null && technique.getBaseDamage() <= 0f && definition.conditionalEffect() != null) {
286:             HostCast parentCast = hostLedger();
287:             UUID operationId = ConditionalIntent.operationId(parentCast.tokenId(), memberIndex, 0, group.contact.groupId());
288:             ReleaseOutcome outcome = SkillSegmentRunner.commitConditional(owner, definition, operationId, snapshot);
289:             if (outcome.committed()) {
290:                 recordConditionalUse(technique);
291:             }
292:             return outcome;
293:         }
294:         if (definition != null && technique.getBaseDamage() <= 0f && definition.domain() != null) {
295:             boolean placed = DomainService.tryWaveReserved(owner, technique, definition.domain(), copies, snapshot, point, claimSuffix);
296:             return placed ? ReleaseOutcome.committed(nowTick) : ReleaseOutcome.rejected(nowTick, "");
297:         }
298:         if (definition != null && technique.getBaseDamage() <= 0f && definition.barrier() != null) {
299:             boolean placed = BarrierService.tryPlaceReserved(owner, definition.barrier(), copies, snapshot, point, look, claimSuffix);
300:             return placed ? ReleaseOutcome.committed(nowTick) : ReleaseOutcome.rejected(nowTick, "");
301:         }
302:         if (definition != null && technique.getBaseDamage() <= 0f && definition.guard() != null) {
303:             long waveKey = group.contact.groupId().getLeastSignificantBits() ^ member.actionIndex();
304:             boolean placed = GuardStates.activateReserved(owner, definition.guard(), waveKey);
305:             if (placed && snapshot != null) {
306:                 GuardStates.bindContact(owner, snapshot);
307:             }
308:             return placed ? ReleaseOutcome.committed(nowTick) : ReleaseOutcome.rejected(nowTick, "");
309:         }
310:         ReleaseBatch previous = ReleaseBatch.current();
311:         ReleaseBatch.bind(group.batch);
312:         try {
313:             if (definition != null && definition.submitsHostGroup()) {
314:                 ReleaseBatch.Tally tally = copies > 1 ? group.batch.openGroupTally() : null;
315:                 HostCast parentCast = hostLedger();
316:                 UUID hostGroup = HostGroupIds.sessionHost(group.contact.groupId(), memberIndex);
317:                 EnchantmentResolution enchantments = SkillSegmentMath.resolveSegmentEnchantments(skill, member);
318:                 EnchantBonus bonus = EnchantBonus.resolve(skill, member.enchantIndexes(), technique.getBaseDamage(), 1);
319:                 float damage = member.enchantIndexes().isEmpty()
320:                         ? SkillSegmentMath.segmentDamage(technique, member, weaponBonus, SkillSegmentMath.segmentMagicDamage(skill, member), null)
321:                         : bonus.carriedDamage(weaponBonus);
322:                 boolean fieldHost = definition.fieldRadius() > 0f;
323:                 boolean carries = !fieldHost && technique.getBaseDamage() > 0f && !member.enchantIndexes().isEmpty();
324:                 ReleaseOutcome outcome = SkillSegmentRunner.commitHostGroup(owner, technique, techniques, member.actionIndex(), carrier,
325:                         damage, carries, enchantments,
326:                         SkillChainPlan.lockThisCast(techniques, member.actionIndex()),
327:                         SkillChainPlan.dropThisCast(techniques, segments, member.actionIndex()),
328:                         tally, copies, group.batch, bonus, memberIndex, look, point, point, look, parentCast, hostGroup, null);
329:                 if (outcome.committed() && fieldHost && snapshot != null) {
330:                     FieldZones.bindBorn(owner.getUUID(), snapshot, outcome.tick());
331:                 }
332:                 return outcome;
333:             }
334:             ReleaseBatch.Tally tally = copies > 1 ? group.batch.openGroupTally() : null;
335:             EnchantmentResolution enchantments = SkillSegmentMath.resolveSegmentEnchantments(skill, member);
336:             EnchantBonus bonus = EnchantBonus.resolve(skill, member.enchantIndexes(), technique.getBaseDamage(), 1);
337:             float damage = member.enchantIndexes().isEmpty()
338:                     ? SkillSegmentMath.segmentDamage(technique, member, weaponBonus, SkillSegmentMath.segmentMagicDamage(skill, member), null)
339:                     : bonus.carriedDamage(weaponBonus);
340:             boolean fieldHost = definition != null && definition.fieldRadius() > 0f;
341:             boolean carries = !fieldHost && technique.getBaseDamage() > 0f && !member.enchantIndexes().isEmpty();
342:             boolean anyCommitted = false;
343:             int commitTick = nowTick;
344:             for (int copyIndex = 0; copyIndex < copies; copyIndex++) {
345:                 ReleaseOutcome outcome = SkillSegmentRunner.commitNow(owner, technique, techniques, member.actionIndex(), carrier,
346:                         damage, carries, enchantments,
347:                         SkillChainPlan.lockThisCast(techniques, member.actionIndex()),
348:                         SkillChainPlan.dropThisCast(techniques, segments, member.actionIndex()),
349:                         tally, copyIndex, copies, group.batch, bonus, memberIndex, look, point);
350:                 if (outcome.committed()) {
351:                     anyCommitted = true;
352:                     commitTick = outcome.tick();
353:                 }
354:             }
355:             if (!anyCommitted) {
356:                 return ReleaseOutcome.rejected(nowTick, "没有可提交的释放");
357:             }
358:             if (fieldHost && snapshot != null) {
359:                 FieldZones.bindBorn(owner.getUUID(), snapshot, commitTick);
360:             }
361:             return ReleaseOutcome.committed(commitTick);
362:         } finally {
363:             ReleaseBatch.bind(previous);
364:         }
365:     }
366: 
367:     private void finishGroup(Group group) {
368:         if (group.closed) {
369:             return;
370:         }
371:         group.closed = true;
372:         if (group.budget != null) {
373:             group.budget.settle(owner);
374:         }
375:         pending = Math.max(0, pending - 1);
376:         tryFinish();
377:     }
378: 
379:     private void tryFinish() {
380:         if (!actionEnded || pending != 0 || terminalAlready || !accepted) {
381:             return;
382:         }
383:         terminalAlready = true;
384:         closed = true;
385:         int nowTick = owner.getServer().getTickCount();
386:         int cooldown = legalCollision ? plan.fullCooldown : plan.actionCooldown;
387:         int readyTick = nowTick + Math.max(cooldown, 1);
388:         CastCooldown.mark(owner, carrier, slot, readyTick);
389:         if (stepCount > 0) {
390:             ChainProgress.advance(owner, carrier, slot, signature, stepCount);
391:         }
392:         CastCooldown.release(carrierKey);
393:         OPEN.remove(carrierKey);
394:         closeHostLedger();
395:         DebugLog.log("双重释放结算 碰撞=" + legalCollision + " 冷却=" + cooldown + " 重放=" + readyTick);
396:     }
397: 
398:     private void dropUnaccepted() {
399:         if (terminalAlready) {
400:             return;
401:         }
402:         terminalAlready = true;
403:         closed = true;
404:         for (Group group : groups) {
405:             if (!group.closed && group.budget != null) {
406:                 group.budget.settle(owner);
407:             }
408:             group.closed = true;
409:         }
410:         if (actionReceipt != null) {
411:             actionReceipt.refund(owner);
412:         }
413:         CastCooldown.release(carrierKey);
414:         OPEN.remove(carrierKey);
415:         closeHostLedger();
416:     }
417: 
418:     private HostCast hostLedger() {
419:         if (hostCast == null) {
420:             hostCast = HostCast.open(owner.getUUID(), castSource);
421:         }
422:         return hostCast;
423:     }
424: 
425:     private void closeHostLedger() {
426:         HostCast current = hostCast;
427:         if (current != null) {
428:             current.close();
429:         }
430:     }
431: 
432:     private boolean continues() {
433:         return owner.isAlive() && owner.level() == castLevel && ChargeGate.same(owner.getUUID(), castEpoch);
434:     }
435: 
436:     private void recordConditionalUse(Technique technique) {
437:         SkillUsePacket.recordTechniqueUse(owner, technique, conditionalUsage);
438:     }
439: 
440:     private static boolean isConditionalTechnique(Technique technique) {
441:         if (technique == null || technique.getBaseDamage() > 0f) {
442:             return false;
443:         }
444:         ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
445:         return definition != null && definition.conditionalEffect() != null;
446:     }
447: 
448:     /** 没有条件术式时原样交回，延后释放的熟练度名单不变。 */
449:     private static List<Technique> proficiencyTechniques(List<Technique> techniques) {
450:         boolean conditionalPresent = false;
451:         for (Technique technique : techniques) {
452:             if (isConditionalTechnique(technique)) {
453:                 conditionalPresent = true;
454:                 break;
455:             }
456:         }
457:         if (!conditionalPresent) {
458:             return techniques;
459:         }
460:         List<Technique> visible = new ArrayList<>();
461:         for (Technique technique : techniques) {
462:             if (!isConditionalTechnique(technique)) {
463:                 visible.add(technique);
464:             }
465:         }
466:         return visible;
467:     }
468: 
469:     private static final class Group {
470:         final ActionContact contact;
471:         GroupBudget budget;
472:         ReleaseBatch batch;
473:         boolean closed;
474: 
475:         Group(ActionContact contact) {
476:             this.contact = contact;
477:         }
478:     }
479: 
480:     private static final class GroupBudget {
481:         final ManaPaymentReceipt receipt;
482:         int openShare;
483:         boolean settled;
484: 
485:         GroupBudget(ManaPaymentReceipt receipt, int openShare) {
486:             this.receipt = receipt;
487:             this.openShare = openShare;
488:         }
489: 
490:         static GroupBudget reserve(ServerPlayer owner, int cost) {
491:             ManaPaymentReceipt receipt = ManaPaymentReceipt.charge(owner, Math.max(0, cost), true);
492:             if (receipt == null) {
493:                 return null;
494:             }
495:             return new GroupBudget(receipt, Math.max(0, cost));
496:         }
497: 
498:         void commit(int share) {
499:             if (settled || share <= 0) {
500:                 return;
501:             }
502:             int keeping = Math.min(share, openShare);
503:             openShare -= keeping;
504:             if (keeping > 0 && receipt != null) {
505:                 receipt.retainShare(keeping);
506:             }
507:         }
508: 
509:         void settle(ServerPlayer owner) {
510:             if (settled) {
511:                 return;
512:             }
513:             settled = true;
514:             receipt.refundShare(owner, openShare);
515:             openShare = 0;
516:         }
517:     }
518: }
519: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/network/CompiledReleaseStep.java">
1: package com.bslx.XueXiMod.network;
2: 
3: import com.bslx.XueXiMod.shu.Technique;
4: import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
5: import com.bslx.XueXiMod.shu.technique.release.ChainRole;
6: import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
7: import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
8: import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalEffects;
9: import com.bslx.XueXiMod.skill.Skill;
10: 
11: import java.util.ArrayList;
12: import java.util.List;
13: 
14: /**
15:  * 同一次按键的选段、费用和冷却都读这一份。双重只编译两个终结成员，C 留到下一步。
16:  */
17: final class CompiledReleaseStep {
18:     final int actionSegment;
19:     final int dualSegment;
20:     final List<ChainSegment> members;
21:     final List<Integer> memberShares;
22:     final int ruleShare;
23:     final int actionShare;
24:     final int fullCooldown;
25:     final int actionCooldown;
26:     final boolean actionLed;
27:     final String chargeRefusal;
28: 
29:     private CompiledReleaseStep(int actionSegment, int dualSegment, List<ChainSegment> members,
30:                                 List<Integer> memberShares, int ruleShare, int actionShare,
31:                                 int fullCooldown, int actionCooldown, boolean actionLed, String chargeRefusal) {
32:         this.actionSegment = actionSegment;
33:         this.dualSegment = dualSegment;
34:         this.members = members;
35:         this.memberShares = memberShares;
36:         this.ruleShare = ruleShare;
37:         this.actionShare = actionShare;
38:         this.fullCooldown = fullCooldown;
39:         this.actionCooldown = actionCooldown;
40:         this.actionLed = actionLed;
41:         this.chargeRefusal = chargeRefusal;
42:     }
43: 
44:     static CompiledReleaseStep compile(Skill skill, List<Technique> techniques, List<ChainSegment> segments,
45:                                        double cooldownReduction) {
46:         int dualSegment = -1;
47:         int actionSegment = -1;
48:         for (int segmentIndex = 0; segmentIndex < segments.size(); segmentIndex++) {
49:             Technique technique = techniques.get(segments.get(segmentIndex).actionIndex());
50:             ChainRole role = SkillSegmentMath.roleOf(technique);
51:             if (role == ChainRole.RULE_DUAL && dualSegment < 0) {
52:                 dualSegment = segmentIndex;
53:             }
54:             if (role == ChainRole.ACTION && actionSegment < 0 && dualSegment < 0) {
55:                 actionSegment = segmentIndex;
56:             }
57:         }
58:         if (dualSegment < 0) {
59:             return actionConditional(skill, techniques, segments, cooldownReduction, actionSegment);
60:         }
61:         List<ChainSegment> released = SkillChainPlan.releasedSegments(segments, dualSegment, techniques);
62:         List<ChainSegment> members = new ArrayList<>();
63:         for (int memberIndex = 0; memberIndex < released.size() && members.size() < 2; memberIndex++) {
64:             members.add(released.get(memberIndex));
65:         }
66:         List<Integer> memberShares = new ArrayList<>();
67:         for (ChainSegment member : members) {
68:             memberShares.add(memberShare(techniques, segments, member));
69:         }
70:         int ruleShare = SkillSegmentMath.segmentManaCost(techniques, segments.get(dualSegment));
71:         int actionShare = actionSegment < 0
72:                 ? 0
73:                 : SkillSegmentMath.segmentManaCost(techniques, segments.get(actionSegment));
74:         List<Integer> everyIndex = new ArrayList<>();
75:         for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
76:             everyIndex.add(techniqueIndex);
77:         }
78:         int fullCooldown = ChainSteps.cooldown(skill, everyIndex, cooldownReduction);
79:         int actionCooldown = 0;
80:         if (actionSegment >= 0) {
81:             ChainSegment action = segments.get(actionSegment);
82:             List<Integer> actionIndexes = new ArrayList<>();
83:             actionIndexes.add(action.actionIndex());
84:             actionIndexes.addAll(action.enchantIndexes());
85:             actionCooldown = ChainSteps.cooldown(skill, actionIndexes, cooldownReduction);
86:         }
87:         String chargeRefusal = chargeRefusal(techniques, members);
88:         return new CompiledReleaseStep(actionSegment, dualSegment, members, memberShares, ruleShare, actionShare,
89:                 fullCooldown, actionCooldown, actionSegment >= 0, chargeRefusal);
90:     }
91: 
92:     /**
93:      * 没有双重、动作后面只有一个条件释放时，沿用动作会话。
94:      * 前面或后面还有别的可释放段时返回空，留给原来的按键路径。
95:      */
96:     private static CompiledReleaseStep actionConditional(Skill skill, List<Technique> techniques,
97:                                                          List<ChainSegment> segments, double cooldownReduction,
98:                                                          int actionSegment) {
99:         if (actionSegment < 0) {
100:             return null;
101:         }
102:         for (int earlierIndex = 0; earlierIndex < actionSegment; earlierIndex++) {
103:             Technique earlier = techniques.get(segments.get(earlierIndex).actionIndex());
104:             if (SkillSegmentMath.isRemovedRule(earlier.getId()) || SkillSegmentMath.isChainModifier(earlier)) {
105:                 continue;
106:             }
107:             return null;
108:         }
109:         ChainSegment conditionalMember = null;
110:         for (int segmentIndex = actionSegment + 1; segmentIndex < segments.size(); segmentIndex++) {
111:             ChainSegment candidate = segments.get(segmentIndex);
112:             Technique technique = techniques.get(candidate.actionIndex());
113:             if (SkillSegmentMath.isRemovedRule(technique.getId()) || SkillSegmentMath.isChainModifier(technique)) {
114:                 continue;
115:             }
116:             ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
117:             if (technique.getBaseDamage() > 0f || definition == null || definition.conditionalEffect() == null) {
118:                 return null;
119:             }
120:             if (conditionalMember != null) {
121:                 return null;
122:             }
123:             conditionalMember = candidate;
124:         }
125:         if (conditionalMember == null) {
126:             return null;
127:         }
128:         List<ChainSegment> members = new ArrayList<>();
129:         members.add(conditionalMember);
130:         List<Integer> memberShares = new ArrayList<>();
131:         memberShares.add(memberShare(techniques, segments, conditionalMember));
132:         int actionShare = SkillSegmentMath.segmentManaCost(techniques, segments.get(actionSegment));
133:         List<Integer> everyIndex = new ArrayList<>();
134:         for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
135:             everyIndex.add(techniqueIndex);
136:         }
137:         int fullCooldown = ChainSteps.cooldown(skill, everyIndex, cooldownReduction);
138:         ChainSegment action = segments.get(actionSegment);
139:         List<Integer> actionIndexes = new ArrayList<>();
140:         actionIndexes.add(action.actionIndex());
141:         actionIndexes.addAll(action.enchantIndexes());
142:         int actionCooldown = ChainSteps.cooldown(skill, actionIndexes, cooldownReduction);
143:         String refusal = chargeRefusal(techniques, members);
144:         return new CompiledReleaseStep(actionSegment, -1, members, memberShares, 0, actionShare,
145:                 fullCooldown, actionCooldown, true, refusal);
146:     }
147: 
148:     private static String chargeRefusal(List<Technique> techniques, List<ChainSegment> members) {
149:         for (ChainSegment member : members) {
150:             Technique technique = techniques.get(member.actionIndex());
151:             ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
152:             if (definition != null && definition.chargeTicks() > 0) {
153:                 return "这次组合里有还不能按时提交的蓄力，没有释放";
154:             }
155:         }
156:         return null;
157:     }
158: 
159:     int groupBill() {
160:         int bill = Math.max(0, ruleShare);
161:         for (int share : memberShares) {
162:             bill += Math.max(0, share);
163:         }
164:         return bill;
165:     }
166: 
167:     private static int memberShare(List<Technique> techniques, List<ChainSegment> segments, ChainSegment member) {
168:         int memberIndex = segments.indexOf(member);
169:         Technique technique = techniques.get(member.actionIndex());
170:         int copies = Math.max(1, SkillChainPlan.copiesBefore(segments, techniques, memberIndex));
171:         ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
172:         int enchantAndHost = SkillSegmentMath.segmentManaCost(techniques, member);
173:         if (definition != null && technique.getBaseDamage() <= 0f && definition.domain() != null) {
174:             return enchantAndHost + definition.domain().initialCost() * copies;
175:         }
176:         if (definition != null && technique.getBaseDamage() <= 0f && definition.barrier() != null) {
177:             return enchantAndHost + definition.barrier().spawnCost() * copies;
178:         }
179:         if (definition != null && technique.getBaseDamage() <= 0f && definition.guard() != null) {
180:             return enchantAndHost + definition.guard().spawnCost() * copies;
181:         }
182:         if (technique.getBaseDamage() <= 0f && ConditionalEffects.copiesCoalesce(definition)) {
183:             return enchantAndHost;
184:         }
185:         int share = enchantAndHost * copies;
186:         if (definition != null && definition.fieldRadius() > 0f && definition.fieldSpawnCost() > 0) {
187:             share += definition.fieldSpawnCost() * copies;
188:         }
189:         return share;
190:     }
191: }
192: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/network/AttackBudgetWindow.java">
1: package com.bslx.XueXiMod.network;
2: 
3: import com.bslx.XueXiMod.shu.Technique;
4: import com.bslx.XueXiMod.shu.technique.ActionWindow;
5: import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
6: import com.bslx.XueXiMod.shu.technique.release.momentum.ChargeGate;
7: import com.bslx.XueXiMod.shu.technique.release.water.CastSource;
8: import com.bslx.XueXiMod.skill.Skill;
9: import net.minecraft.server.level.ServerLevel;
10: import net.minecraft.server.level.ServerPlayer;
11: import net.minecraft.world.phys.Vec3;
12: 
13: import java.util.List;
14: import java.util.UUID;
15: 
16: /** 动作窗口上的攻击预算。窗口结束、人或会话失效时只退还没取走的份额。 */
17: final class AttackBudgetWindow implements ActionWindow.Sink {
18:     private final ServerPlayer player;
19:     private final ServerLevel castLevel;
20:     private final int castEpoch;
21:     private final UncommittedShare.Pool shares;
22:     private final Skill skill;
23:     private final List<Technique> techniques;
24:     private final List<ChainSegment> segments;
25:     private final SkillChainPlan.ModifierPlan plan;
26:     private final float weaponBonus;
27:     private final CastSource castSource;
28:     private boolean finished;
29: 
30:     private AttackBudgetWindow(ServerPlayer player, ServerLevel castLevel, int castEpoch, UncommittedShare.Pool shares,
31:                                Skill skill, List<Technique> techniques, List<ChainSegment> segments,
32:                                SkillChainPlan.ModifierPlan plan, float weaponBonus, CastSource castSource) {
33:         this.player = player;
34:         this.castLevel = castLevel;
35:         this.castEpoch = castEpoch;
36:         this.shares = shares;
37:         this.skill = skill;
38:         this.techniques = techniques;
39:         this.segments = segments;
40:         this.plan = plan;
41:         this.weaponBonus = weaponBonus;
42:         this.castSource = castSource;
43:     }
44: 
45:     static AttackBudgetWindow open(ServerPlayer player, ServerLevel castLevel, int castEpoch, UncommittedShare.Pool shares,
46:                                    Skill skill, List<Technique> techniques, List<ChainSegment> segments,
47:                                    SkillChainPlan.ModifierPlan plan, float weaponBonus, CastSource castSource) {
48:         return new AttackBudgetWindow(player, castLevel, castEpoch, shares, skill, techniques, segments, plan, weaponBonus, castSource);
49:     }
50: 
51:     boolean ownerPresent() {
52:         return player != null && player.isAlive() && !player.isRemoved() && !player.hasDisconnected()
53:                 && player.level() == castLevel && ChargeGate.same(player.getUUID(), castEpoch);
54:     }
55: 
56:     void finish() {
57:         if (finished) {
58:             return;
59:         }
60:         finished = true;
61:         if (shares != null) {
62:             shares.close(player);
63:         }
64:     }
65: 
66:     @Override
67:     public void onContact(UUID targetId, Vec3 point, Vec3 direction) {
68:         if (closed() || player.getServer() == null) {
69:             return;
70:         }
71:         SkillChainPlan.firePaidPlan(player, skill, techniques, segments, plan, weaponBonus,
72:                 player.getServer().getTickCount(), shares, castSource);
73:     }
74: 
75:     @Override
76:     public void onEnded() {
77:         finish();
78:     }
79: 
80:     @Override
81:     public void onFinished() {
82:         finish();
83:     }
84: 
85:     @Override
86:     public boolean closed() {
87:         return finished || !ownerPresent();
88:     }
89: }
90: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/momentum/ChargeGate.java">
1: package com.bslx.XueXiMod.shu.technique.release.momentum;
2: 
3: import java.util.Set;
4: import java.util.UUID;
5: import java.util.concurrent.ConcurrentHashMap;
6: 
7: /** 蓄力会话。死亡、下线、换维度和术式自己的打断都走 {@link #interrupt}。 */
8: public final class ChargeGate {
9:     private static final ConcurrentHashMap<UUID, Integer> EPOCH = new ConcurrentHashMap<>();
10:     private static final Set<UUID> ARMED = ConcurrentHashMap.newKeySet();
11: 
12:     private ChargeGate() {
13:     }
14: 
15:     public static int epoch(UUID playerId) {
16:         return EPOCH.getOrDefault(playerId, 0);
17:     }
18: 
19:     public static boolean same(UUID playerId, int epoch) {
20:         return epoch(playerId) == epoch;
21:     }
22: 
23:     public static boolean armed(UUID playerId) {
24:         return ARMED.contains(playerId);
25:     }
26: 
27:     public static void arm(UUID playerId) {
28:         ARMED.add(playerId);
29:     }
30: 
31:     public static void disarm(UUID playerId) {
32:         ARMED.remove(playerId);
33:     }
34: 
35:     public static void interrupt(UUID playerId) {
36:         EPOCH.merge(playerId, 1, Integer::sum);
37:         ARMED.remove(playerId);
38:         WaterMomentum.releaseAll(playerId);
39:     }
40: }
41: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/CastFrame.java">
1: package com.bslx.XueXiMod.shu.technique.release;
2: 
3: import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
4: import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
5: import net.minecraft.util.RandomSource;
6: import net.minecraft.world.phys.Vec3;
7: 
8: public final class CastFrame {
9:     private static final ThreadLocal<CastFrame> CURRENT = new ThreadLocal<>();
10: 
11:     public float damage;
12:     public EnchantmentResolution enchantments;
13:     public ReleaseBatch.Tally tally;
14:     public int copyIndex;
15:     public Vec3 anchor;
16:     public Vec3 origin;
17:     public Vec3 look;
18:     public Vec3 planeNormal;
19:     public float roll;
20:     public boolean pose3d;
21:     public boolean overrideGeometry;
22:     public boolean drop;
23:     /** 独立追踪修饰，不替换基础路径或生成几何。 */
24:     public boolean tracking;
25:     public Trajectory pathOverride;
26:     public boolean trajectoryMutable;
27:     public float radiusMultiplier = 1f;
28:     public float energyScale = 1f;
29:     public int burnSeconds;
30:     public int controlTicks;
31:     public boolean extinguish;
32:     public float knockback;
33:     public float speedMultiplier = 1f;
34:     public TideMarkSuccess tideSuccess = TideMarkSuccess.NONE;
35:     /** 玩家身体当前被几块场地包住。0 和 1 都不改伤害。 */
36:     public float fieldDamageScale = 1f;
37:     public float healAmount;
38:     public boolean healCaster;
39:     public float pullStrength;
40:     public int wetnessGrant;
41:     public boolean blocksMovement;
42:     public Vec3 pullAnchor;
43:     public float liftStrength;
44:     public double volumeHalf;
45:     public double groundLevel = Double.NEGATIVE_INFINITY;
46:     public long sampleSeed;
47:     public boolean scatterAim;
48:     public int copyCount = 1;
49:     /** 这一份的生成几何已经按复制序号散开。只给视觉读，释放计算不读。 */
50:     public boolean cohortDispersed;
51:     public boolean heavyWater;
52:     public java.util.UUID releaseGroup;
53: 
54:     public static CastFrame current() {
55:         return CURRENT.get();
56:     }
57: 
58:     /** 能量倍率乘上已经含有元素反应的半径倍率。无效值按 1。 */
59:     public float volumeScale() {
60:         float scale = this.energyScale * this.radiusMultiplier;
61:         if (!Float.isFinite(scale) || scale <= 0f) {
62:             return 1f;
63:         }
64:         return scale;
65:     }
66: 
67:     public static void bind(CastFrame frame) {
68:         if (frame == null) {
69:             CURRENT.remove();
70:         } else {
71:             CURRENT.set(frame);
72:         }
73:     }
74: 
75:     public static void unbind() {
76:         CURRENT.remove();
77:     }
78: 
79:     /** 垂落从落点上方出现，再自由落体。 */
80:     private static final double DROP_LIFT = 8.0;
81:     private static final double GOLDEN_TURN = Math.PI * (3.0 - Math.sqrt(5.0));
82:     private static final double VOLUME_HALF = 1.5;
83: 
84:     public static CastFrame forAttack(float damage, EnchantmentResolution enchantments, ReleaseBatch.Tally tally,
85:                                       int copyIndex, int copyCount, Vec3 center, Vec3 baseLook,
86:                                       boolean spaceLock, boolean scatter, boolean entityLock, Trajectory pathOverride,
87:                                       boolean trajectoryMutable, long seed, double groundLevel) {
88:         CastFrame frame = new CastFrame();
89:         frame.damage = damage;
90:         frame.enchantments = enchantments != null ? enchantments : EnchantmentResolution.empty();
91:         frame.tally = tally;
92:         frame.copyIndex = copyIndex;
93:         frame.copyCount = copyCount < 1 ? 1 : copyCount;
94:         frame.pathOverride = pathOverride;
95:         frame.drop = pathOverride == Trajectory.DROP;
96:         frame.trajectoryMutable = trajectoryMutable;
97:         boolean replacesPath = trajectoryMutable && pathOverride != null && pathOverride != Trajectory.STRAIGHT;
98: 
99:         if (!scatter && !spaceLock && !replacesPath) {
100:             frame.overrideGeometry = false;
101:             return frame;
102:         }
103: 
104:         frame.overrideGeometry = true;
105:         if (!trajectoryMutable) {
106:             return slashGeometry(frame, center, baseLook, spaceLock, scatter, copyIndex, copyCount, seed);
107:         }
108:         return projectileGeometry(frame, center, baseLook, spaceLock, scatter, entityLock, pathOverride, seed, groundLevel);
109:     }
110: 
111:     /** 斩击保持原有落点和刀面散布，不改成弹丸的随机向量。 */
112:     private static CastFrame slashGeometry(CastFrame frame, Vec3 center, Vec3 baseLook, boolean spaceLock, boolean scatter,
113:                                            int copyIndex, int copyCount, long seed) {
114:         double half = spaceLock ? 1.5 : 1.0;
115:         Vec3 origin = (spaceLock || scatter) ? jitter(center, half, seed, copyIndex) : center;
116:         frame.origin = origin;
117:         frame.anchor = spaceLock ? center : null;
118:         frame.look = baseLook.normalize();
119: 
120:         if (!scatter) {
121:             frame.planeNormal = horizontal(baseLook);
122:             frame.roll = 0.0F;
123:             frame.pose3d = false;
124:             frame.cohortDispersed = spaceLock;
125:             return frame;
126:         }
127: 
128:         RandomSource random = RandomSource.create(seed ^ copyIndex);
129:         Vec3 flatLook = horizontal(baseLook);
130:         Vec3 flatRight = new Vec3(-flatLook.z, 0.0, flatLook.x);
131:         double radius = 0.35 + random.nextDouble() * 0.9;
132:         double azimuth = random.nextDouble() * Math.PI * 2.0;
133:         double along = (random.nextDouble() * 2.0 - 1.0) * 0.75;
134:         double verticalOffset = (random.nextDouble() * 2.0 - 1.0) * 0.75;
135:         Vec3 scattered = center
136:                 .add(flatRight.scale(Math.cos(azimuth) * radius))
137:                 .add(flatLook.scale(Math.sin(azimuth) * radius))
138:                 .add(flatLook.scale(along))
139:                 .add(0.0, verticalOffset, 0.0);
140: 
141:         frame.origin = clampToBox(scattered, center, spaceLock ? 1.5 : 1.0);
142:         frame.anchor = spaceLock ? center : null;
143:         frame.look = baseLook.normalize();
144:         frame.roll = (float) ((random.nextDouble() * 2.0 - 1.0) * 35.0);
145:         frame.planeNormal = deflectNormal(random, baseLook, copyIndex, copyCount);
146:         frame.pose3d = Math.abs(frame.roll) > 1.0F || Math.abs(frame.planeNormal.y) > 0.2;
147:         frame.cohortDispersed = true;
148:         return frame;
149:     }
150: 
151:     /** 落点指定只撒生成点。三维交错在没有其他索敌时围着玩家撒点，并改成随机三维方向。 */
152:     private static CastFrame projectileGeometry(CastFrame frame, Vec3 center, Vec3 baseLook, boolean spaceLock, boolean scatter,
153:                                                 boolean entityLock, Trajectory pathOverride, long seed, double groundLevel) {
154:         if (pathOverride == Trajectory.VORTEX) {
155:             frame.anchor = center;
156:             frame.volumeHalf = 0.0;
157:             frame.origin = center;
158:             frame.scatterAim = false;
159:             frame.look = frame.vortexPhase(0);
160:             frame.planeNormal = horizontal(frame.look);
161:             frame.roll = 0.0F;
162:             frame.pose3d = false;
163:             frame.cohortDispersed = true;
164:             return frame;
165:         }
166:         boolean cube = spaceLock || (scatter && !entityLock);
167:         frame.anchor = cube ? center : null;
168:         frame.volumeHalf = cube ? VOLUME_HALF : 0.0;
169:         frame.sampleSeed = seed;
170:         frame.groundLevel = groundLevel;
171:         frame.scatterAim = scatter && pathOverride != Trajectory.DROP;
172:         frame.look = baseLook.normalize();
173:         frame.origin = cube ? frame.volumePoint(0) : (pathOverride == Trajectory.DROP ? center.add(0.0, DROP_LIFT, 0.0) : center);
174:         frame.look = frame.aimFor(0);
175:         frame.planeNormal = horizontal(frame.look);
176:         frame.roll = 0.0F;
177:         frame.pose3d = false;
178:         frame.cohortDispersed = cube || frame.scatterAim;
179:         return frame;
180:     }
181: 
182:     /** 接触点把每一份收到同一个锚点上之后，视觉不能再把覆写当成已经散开。 */
183:     public void adoptSharedAnchor() {
184:         this.cohortDispersed = false;
185:     }
186: 
187:     /** 同一条龙卷风上的起始角。序号连续时仍均匀铺在圆周上。 */
188:     public Vec3 vortexPhase(int spawnIndex) {
189:         double phase = (this.copyIndex * 1024 + spawnIndex) * GOLDEN_TURN;
190:         return new Vec3(Math.cos(phase), 0.0, Math.sin(phase));
191:     }
192:     public Vec3 volumePoint(int spawnIndex) {
193:         Vec3 point = jitter(this.anchor, this.volumeHalf, this.sampleSeed, this.copyIndex * 1024 + spawnIndex);
194:         if (point.y < this.groundLevel) {
195:             point = new Vec3(point.x, this.groundLevel, point.z);
196:         }
197:         if (this.drop) {
198:             point = point.add(0.0, DROP_LIFT, 0.0);
199:         }
200:         return point;
201:     }
202: 
203:     public Vec3 aimFor(int spawnIndex) {
204:         if (this.drop) {
205:             return new Vec3(0.0, -1.0, 0.0);
206:         }
207:         if (this.scatterAim) {
208:             return randomDirection(this.sampleSeed, this.copyIndex * 1024 + spawnIndex);
209:         }
210:         return this.look == null ? new Vec3(0.0, 0.0, 1.0) : this.look;
211:     }
212: 
213:     private static Vec3 randomDirection(long seed, int copyIndex) {
214:         RandomSource random = RandomSource.create(seed ^ (copyIndex * 31L) ^ 0x6C62272EL);
215:         double rise = random.nextDouble() * 2.0 - 1.0;
216:         double spin = random.nextDouble() * Math.PI * 2.0;
217:         double ring = Math.sqrt(Math.max(0.0, 1.0 - rise * rise));
218:         return new Vec3(ring * Math.cos(spin), rise, ring * Math.sin(spin));
219:     }
220: 
221:     private static Vec3 deflectNormal(RandomSource random, Vec3 baseLook, int copyIndex, int copyCount) {
222:         if (copyCount <= 1) {
223:             return yawPitch(baseLook,
224:                     (random.nextDouble() * 2.0 - 1.0) * 25.0,
225:                     (random.nextDouble() * 2.0 - 1.0) * 15.0);
226:         }
227:         double baseYaw = copyIndex * 120.0;
228:         double yaw = baseYaw;
229:         double pitch = (copyIndex - 1) * 15.0;
230:         for (int attempt = 0; attempt < 8; attempt++) {
231:             double jitter = (random.nextDouble() * 2.0 - 1.0) * 25.0;
232:             if (Math.abs(jitter) >= 12.0) {
233:                 yaw = baseYaw + jitter;
234:                 pitch = (random.nextDouble() * 2.0 - 1.0) * 15.0;
235:                 break;
236:             }
237:         }
238:         return yawPitch(baseLook, yaw, pitch);
239:     }
240: 
241:     private static Vec3 yawPitch(Vec3 base, double yawDegrees, double pitchDegrees) {
242:         Vec3 flat = horizontal(base);
243:         Vec3 flatRight = new Vec3(-flat.z, 0.0, flat.x);
244:         double yaw = Math.toRadians(yawDegrees);
245:         double pitch = Math.toRadians(pitchDegrees);
246:         Vec3 yawed = flat.scale(Math.cos(yaw)).add(flatRight.scale(Math.sin(yaw)));
247:         double cosPitch = Math.cos(pitch);
248:         double sinPitch = Math.sin(pitch);
249:         return new Vec3(yawed.x * cosPitch, sinPitch, yawed.z * cosPitch).normalize();
250:     }
251: 
252:     private static Vec3 jitter(Vec3 center, double half, long seed, int sampleIndex) {
253:         long mixed = mix64(seed ^ (sampleIndex * 0x9E3779B97F4A7C15L));
254:         double east = (unit(mixed ^ 0x9E3779B97F4A7C15L) * 2.0 - 1.0) * half;
255:         double rise = (unit(mixed ^ 0xBF58476D1CE4E5B9L) * 2.0 - 1.0) * half;
256:         double south = (unit(mixed ^ 0x94D049BB133111EBL) * 2.0 - 1.0) * half;
257:         return center.add(east, rise, south);
258:     }
259: 
260:     /** 连续序号不能直接丢给 RandomSource，否则第一轴几乎不动，点会排成一条线。 */
261:     private static long mix64(long value) {
262:         long mixed = value;
263:         mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
264:         mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
265:         return mixed ^ (mixed >>> 31);
266:     }
267: 
268:     private static double unit(long mixed) {
269:         return (mix64(mixed) >>> 11) * 0x1.0p-53;
270:     }
271: 
272:     private static Vec3 clampToBox(Vec3 point, Vec3 center, double half) {
273:         return new Vec3(
274:                 clamp(point.x, center.x - half, center.x + half),
275:                 clamp(point.y, center.y - half, center.y + half),
276:                 clamp(point.z, center.z - half, center.z + half));
277:     }
278: 
279:     private static double clamp(double value, double lowerBound, double upperBound) {
280:         return Math.max(lowerBound, Math.min(upperBound, value));
281:     }
282: 
283:     private static Vec3 horizontal(Vec3 vector) {
284:         double length = Math.sqrt(vector.x * vector.x + vector.z * vector.z);
285:         if (length < 1.0E-4) {
286:             return new Vec3(0.0, 0.0, 1.0);
287:         }
288:         return new Vec3(vector.x / length, 0.0, vector.z / length);
289:     }
290: }
291: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseDefinition.java">
1: package com.bslx.XueXiMod.shu.technique.release;
2: 
3: import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierDefinition;
4: import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalEffect;
5: import com.bslx.XueXiMod.shu.technique.release.domain.DomainDefinition;
6: import com.bslx.XueXiMod.shu.technique.release.guard.GuardDefinition;
7: 
8: public interface ReleaseDefinition {
9:     String id();
10: 
11:     ReleaseKind kind();
12: 
13:     ReleaseTag tag();
14: 
15:     Trajectory trajectory();
16: 
17:     boolean moving();
18: 
19:     int lifetimeTicks();
20: 
21:     float speed();
22: 
23:     float thickness();
24: 
25:     default int pullAfterA() {
26:         return 0;
27:     }
28: 
29:     default boolean acceptsTargetOverride() {
30:         return false;
31:     }
32: 
33:     /**
34:      * 出手瞬间可以把瞄准收成当时的目标中心方向，不因此改生成点，也不因此持续追踪。
35:      * 默认与 {@link #acceptsTargetOverride()} 相同，旧术式行为不变。
36:      */
37:     default boolean acceptsInitialAim() {
38:         return acceptsTargetOverride();
39:     }
40: 
41:     /**
42:      * 出手时把方向收成当时的目标中心，之后不再跟着目标转。
43:      * 三者同时成立才算：接受初瞄、不接受持续索敌覆盖、弹道本身不可改。
44:      */
45:     default boolean dedicatedInitialAim() {
46:         return acceptsInitialAim() && !acceptsTargetOverride() && !trajectoryMutable();
47:     }
48: 
49:     /** 为真时，主手必须是剑。默认不额外要求，斧、镐、工具和三叉戟上的旧剑技保持原样。 */
50:     default boolean requiresHeldSword() {
51:         return false;
52:     }
53: 
54:     /**
55:      * 为真时，同一次宿主里的复制份和全局实体倍率必须整组真实生成成功后才提交一次。
56:      * 按登记表：只有 {@link ReleaseRegistry#submitsHostGroup(String)} 列出的术式打开。
57:      * 公共释放路径只读这个开关，不按术式编号分支。
58:      */
59:     default boolean submitsHostGroup() {
60:         return ReleaseRegistry.submitsHostGroup(id());
61:     }
62: 
63:     /** 为真时，这一发用冻结进度弧，而不是移动盒子。默认关闭，公共路径不按术式编号分支。 */
64:     default boolean usesArcBand() {
65:         return false;
66:     }
67: 
68:     /**
69:      * 为真时，宿主组在复制前准备一次拍号，只在整组真实提交后推进。
70:      * 默认关闭。公共链只读这个开关，不按术式编号分支。
71:      */
72:     default boolean tracksBeatCycle() {
73:         return false;
74:     }
75: 
76:     /** 未缩放的弧带半径。最终尺寸只在快照冻结时乘一次合成倍率。 */
77:     default float arcBandRadius() {
78:         return 2.4f;
79:     }
80: 
81:     /** 未缩放的弧带半宽。 */
82:     default float arcBandHalfWidth() {
83:         return 0.16f;
84:     }
85: 
86:     default boolean acceptsPathOverride() {
87:         return false;
88:     }
89: 
90:     default boolean setsTargetForNextDirectional() {
91:         return false;
92:     }
93: 
94:     default boolean setsDropForNextDirectional() {
95:         return false;
96:     }
97: 
98:     default ChainRole role() {
99:         return ChainRole.ATTACK;
100:     }
101: 
102:     /** 弹道修改能否改掉这条攻击原来的路径。不能改的只允许索敌改生成位置。 */
103:     default boolean trajectoryMutable() {
104:         return false;
105:     }
106: 
107:     /**
108:      * 是否为「操纵世界剑实体」类术式（幻剑术、后续召回/御剑等）。
109:      * 装配链含此类术式时：手持剑可放；空手仅当世界中存在该玩家的剑实体时可放。
110:      */
111:     default boolean manipulatesSwordEntities() {
112:         return false;
113:     }
114: 
115:     /** 这条操纵术式不能和链上的其他段一起释放。 */
116:     default boolean refusesMixedChain() {
117:         return false;
118:     }
119: 
120:     /** 来源有多大。所有属性共用，1 表示不改碰撞体积。 */
121:     default float energyScale() {
122:         return 1f;
123:     }
124: 
125:     /** 作为规则挂到后面这次攻击上的能量。1 表示这条不提供。多条相乘。 */
126:     default float chainEnergyScale() {
127:         return 1f;
128:     }
129: 
130:     /** 小于 0 时仍按伤害计算魔力。防御和治疗填自己的消耗。 */
131:     default int explicitManaCost() {
132:         return -1;
133:     }
134: 
135:     /** 大于 0 才进入蓄力。打断只清场，不放收招。 */
136:     default int chargeTicks() {
137:         return 0;
138:     }
139: 
140:     /** 出手时先扣的水势。0 表示这发不扣。 */
141:     default float momentumSpend() {
142:         return 0f;
143:     }
144: 
145:     /** 场地半径。0 表示这发不是场地。 */
146:     default float fieldRadius() {
147:         return 0f;
148:     }
149: 
150:     /** 随身护体。没有声明时保持原来的攻击或场地。 */
151:     default GuardDefinition guard() {
152:         return null;
153:     }
154: 
155:     /** 固定屏障。没有声明时保持原来的攻击、场地或护体。 */
156:     default BarrierDefinition barrier() {
157:         return null;
158:     }
159: 
160:     /** 固定领域。没有声明时保持原来的攻击、场地、护体或屏障。 */
161:     default DomainDefinition domain() {
162:         return null;
163:     }
164: 
165:     /**
166:      * 零伤条件释放。没有声明时保持原来的攻击、场地、护体、屏障或领域。
167:      * 公共链只读这个能力，不按术式编号分支。
168:      */
169:     default ConditionalEffect conditionalEffect() {
170:         return null;
171:     }
172: 
173:     /**
174:      * 显式承接来袭弹。默认关闭。
175:      * 水幕、水环和三拍水弧在各自工厂打开。别的屏障、护体和径向斩击不会因为类型相同就接上。
176:      */
177:     default boolean deflectsProjectiles() {
178:         return false;
179:     }
180: 
181:     /**
182:      * 持续接触先记下载荷，等这一下确实打出正伤再提交。默认关闭。
183:      * 热光束已有命中间隔，不能靠间隔大于 0 打开这项。公共结算只读这个开关。
184:      */
185:     default boolean persistentFlowSettlement() {
186:         return false;
187:     }
188: 
189:     default int fieldSpawnCost() {
190:         return 0;
191:     }
192: 
193:     default int fieldUpkeepCost() {
194:         return 0;
195:     }
196: 
197:     default int fieldUpkeepPeriod() {
198:         return 20;
199:     }
200: 
201:     default int fieldLifetimeTicks() {
202:         return 200;
203:     }
204: 
205:     default float healAmount() {
206:         return 0f;
207:     }
208: 
209:     /** 为真时治疗打在施法者身上，不打在被命中的目标上。 */
210:     default boolean healCaster() {
211:         return false;
212:     }
213: 
214:     default float pullStrength() {
215:         return 0f;
216:     }
217: 
218:     default int wetnessGrant() {
219:         return 0;
220:     }
221: 
222:     /** 命中时熄灭目标身上的火。不改世界里的火方块。 */
223:     default boolean purifyOnHit() {
224:         return false;
225:     }
226: 
227:     /** 生成的命中体挡住移动。不改方块，也不改流体。 */
228:     default boolean blocksMovement() {
229:         return false;
230:     }
231: 
232:     default float pressurePulse() {
233:         return 0f;
234:     }
235: 
236:     default float erosionPulse() {
237:         return 0f;
238:     }
239: 
240:     /** 命中时的水平击退。0 表示不从定义里加。 */
241:     default float knockbackStrength() {
242:         return 0f;
243:     }
244: 
245:     /** 命中时向上抛。0 表示不抛。 */
246:     default float liftStrength() {
247:         return 0f;
248:     }
249: 
250:     /** 人站在这块场地里时削掉的伤害比例。0 到 1，多块场地取最大的一份。 */
251:     default float unloadRatio() {
252:         return 0f;
253:     }
254: 
255:     /** 场地里每拍刷新的减速时长。0 表示不减速。 */
256:     default int slowTicks() {
257:         return 0;
258:     }
259: 
260:     /** 场地正常到期时在中心打一次。被打断或维持失败不打。 */
261:     default float endBurstDamage() {
262:         return 0f;
263:     }
264: 
265:     /** 0 表示沿用效果自己的外观。 */
266:     default int bodyVisual() {
267:         return 0;
268:     }
269: 
270:     default String functionId() {
271:         return "";
272:     }
273: 
274:     default ReleaseShape shape() {
275:         return ReleaseShape.BOX;
276:     }
277: 
278:     /** 宽度相对厚度的倍数。 */
279:     default float spanScale() {
280:         return 1f;
281:     }
282: 
283:     /** 长度相对厚度的倍数。 */
284:     default float lengthScale() {
285:         return 1f;
286:     }
287: 
288:     /** 能量是否放大这条攻击的长度。默认和宽、高一起放大。 */
289:     default boolean energyScalesLength() {
290:         return true;
291:     }
292: 
293:     /** 能量是否放大宽度。 */
294:     default boolean energyScalesWidth() {
295:         return true;
296:     }
297: 
298:     /** 能量是否放大高度。 */
299:     default boolean energyScalesHeight() {
300:         return true;
301:     }
302: 
303:     /** 能量是否放大模型倍率。画面若另读宽高，术式可以关掉，避免乘两次。 */
304:     default boolean energyScalesModel() {
305:         return true;
306:     }
307: 
308:     /**
309:      * 为真时，由命中实体承载的剑术视觉在同锚点多份时分道。
310:      * 默认关闭。法术、幻剑和操纵已有剑的术式保持原来的画面。
311:      */
312:     default boolean separatesCohortVisuals() {
313:         return false;
314:     }
315: 
316:     /** 每一份有效攻击要预留的重水水势。0 表示这发不走重水。 */
317:     default float heavyMomentumPerCopy() {
318:         return 0f;
319:     }
320: 
321:     /**
322:      * 重压门槛。0 表示不挂重压。
323:      * 命中前必须已经有潮湿效果，并且潮湿值达到这个数（26 起是二档）。
324:      */
325:     default int pressureWetnessMinimum() {
326:         return 0;
327:     }
328: }
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseBatch.java">
1: package com.bslx.XueXiMod.shu.technique.release;
2: 
3: import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
4: import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
5: import com.bslx.XueXiMod.shu.technique.release.domain.AttackEnchantmentContext;
6: import com.bslx.XueXiMod.shu.technique.release.domain.DomainAuraResolver;
7: import com.bslx.XueXiMod.shu.technique.release.payload.FlowSuccessPayload;
8: import com.bslx.XueXiMod.skill.Skill;
9: import net.minecraft.server.level.ServerPlayer;
10: import net.minecraft.world.entity.LivingEntity;
11: import net.minecraft.world.level.ClipContext;
12: import net.minecraft.world.phys.AABB;
13: import net.minecraft.world.phys.BlockHitResult;
14: import net.minecraft.world.phys.HitResult;
15: import net.minecraft.world.phys.Vec3;
16: 
17: import java.util.HashMap;
18: import java.util.List;
19: import java.util.Map;
20: import java.util.UUID;
21: 
22: public final class ReleaseBatch {
23:     private static final ThreadLocal<ReleaseBatch> CURRENT = new ThreadLocal<>();
24:     private static final double ANCHOR_RANGE = 8.0;
25:     private static final double ANCHOR_FALLBACK = 4.0;
26: 
27:     private final ServerPlayer player;
28:     private Vec3 anchor;
29:     private boolean wantAnchor;
30: 
31:     private ReleaseBatch(ServerPlayer player) {
32:         this.player = player;
33:     }
34: 
35:     public static ReleaseBatch open(ServerPlayer player, Skill skill) {
36:         ReleaseBatch batch = new ReleaseBatch(player);
37:         if (skill != null) {
38:             for (var technique : skill.getTechniques()) {
39:                 var definition = ReleaseRegistry.get(technique.getId());
40:                 if (definition != null && definition.role() == ChainRole.SEEK_SPACE) {
41:                     batch.wantAnchor = true;
42:                 }
43:             }
44:         }
45:         CURRENT.set(batch);
46:         return batch;
47:     }
48: 
49:     public static ReleaseBatch current() {
50:         return CURRENT.get();
51:     }
52: 
53:     public static void bind(ReleaseBatch batch) {
54:         if (batch == null) {
55:             CURRENT.remove();
56:         } else {
57:             CURRENT.set(batch);
58:         }
59:     }
60: 
61:     public static void unbind() {
62:         CURRENT.remove();
63:     }
64: 
65:     /** 弹丸的落点中心：准星打到的方块，光线上有生物则改用生物中心。不写入斩击锚点。 */
66:     public Vec3 crosshairCenter(ServerPlayer caster) {
67:         Vec3 eye = caster.getEyePosition();
68:         Vec3 look = caster.getLookAngle();
69:         Vec3 far = eye.add(look.scale(32.0));
70:         BlockHitResult blockHit = caster.serverLevel().clip(new ClipContext(
71:                 eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
72:         Vec3 limit = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : far;
73:         AABB search = new AABB(eye, limit).inflate(1.5);
74:         List<LivingEntity> entities = caster.serverLevel().getEntitiesOfClass(LivingEntity.class, search,
75:                 entity -> entity != caster && entity.isAlive() && !entity.isSpectator());
76:         LivingEntity nearest = null;
77:         double bestDistance = 1.5;
78:         for (LivingEntity entity : entities) {
79:             double distance = distanceToSegment(entity.getBoundingBox().getCenter(), eye, limit);
80:             if (distance <= bestDistance) {
81:                 bestDistance = distance;
82:                 nearest = entity;
83:             }
84:         }
85:         return nearest != null ? nearest.getBoundingBox().getCenter() : limit;
86:     }
87: 
88:     public Vec3 resolveAnchor(ServerPlayer caster) {
89:         if (anchor != null) {
90:             return anchor;
91:         }
92:         Vec3 eye = caster.getEyePosition();
93:         Vec3 look = caster.getLookAngle();
94:         if (!wantAnchor) {
95:             anchor = eye.add(look.scale(ANCHOR_FALLBACK));
96:             return anchor;
97:         }
98:         Vec3 end = eye.add(look.scale(ANCHOR_RANGE));
99:         AABB search = new AABB(eye, end).inflate(1.5);
100:         List<LivingEntity> entities = caster.serverLevel().getEntitiesOfClass(LivingEntity.class, search,
101:                 entity -> entity != caster && entity.isAlive() && !entity.isSpectator());
102:         LivingEntity nearest = null;
103:         double best = Double.MAX_VALUE;
104:         for (LivingEntity entity : entities) {
105:             double distance = distanceToSegment(entity.getBoundingBox().getCenter(), eye, end);
106:             if (distance < best) {
107:                 best = distance;
108:                 nearest = entity;
109:             }
110:         }
111:         anchor = nearest != null ? nearest.getBoundingBox().getCenter() : eye.add(look.scale(ANCHOR_FALLBACK));
112:         return anchor;
113:     }
114: 
115:     public Tally openGroupTally() {
116:         return new Tally();
117:     }
118: 
119:     public final class Tally {
120:         private final FlowRoundLedger flowLedger = new FlowRoundLedger();
121:         private final Map<UUID, LivingEntity> targets = new HashMap<>();
122:         private int originTick;
123:         private boolean originReady;
124:         private int flushTick = Integer.MAX_VALUE;
125:         private boolean flushPending;
126: 
127:         private Tally() {
128:         }
129: 
130:         public void record(LivingEntity target, float damage, int copyIndex, int hitInterval) {
131:             record(target, damage, copyIndex, hitInterval, TideMarkSuccess.NONE);
132:         }
133: 
134:         public void record(LivingEntity target, float damage, int copyIndex, int hitInterval, TideMarkSuccess tide) {
135:             record(target, damage, copyIndex, hitInterval, tide, null);
136:         }
137: 
138:         public void record(LivingEntity target, float damage, int copyIndex, int hitInterval, TideMarkSuccess tide,
139:                            FlowSuccessPayload flowPayload) {
140:             int roundIndex = 0;
141:             if (hitInterval > 0 && player.getServer() != null) {
142:                 int nowTick = player.getServer().getTickCount();
143:                 if (!originReady) {
144:                     originTick = nowTick;
145:                     originReady = true;
146:                 }
147:                 roundIndex = FlowRoundLedger.roundIndex(hitInterval, originTick, nowTick);
148:             }
149:             int marks = tide == null ? 0 : tide.marks();
150:             if (!this.flowLedger.accept(target.getUUID(), roundIndex, copyIndex, damage, marks, flowPayload)) {
151:                 return;
152:             }
153:             targets.put(target.getUUID(), target);
154:             if (player.getServer() == null) {
155:                 return;
156:             }
157:             int now = player.getServer().getTickCount();
158:             if (flushPending && flushTick >= now) {
159:                 return;
160:             }
161:             flushTick = now + FlowRoundLedger.FLUSH_DELAY;
162:             flushPending = true;
163:             ChainScheduler.schedule(player.getServer(), flushTick, this::flush);
164:         }
165: 
166:         public int flushTick() {
167:             return flushTick;
168:         }
169: 
170:         private boolean ownerReady() {
171:             return player != null && player.isAlive() && !player.isRemoved();
172:         }
173: 
174:         private void flush() {
175:             if (!flushPending) {
176:                 return;
177:             }
178:             flushPending = false;
179:             for (UUID targetId : this.flowLedger.targets()) {
180:                 int marks = this.flowLedger.takeMarks(targetId);
181:                 LivingEntity target = targets.get(targetId);
182:                 Float damage = this.flowLedger.damage(targetId);
183:                 if (target == null || !target.isAlive() || damage == null) {
184:                     continue;
185:                 }
186:                 float total = this.flowLedger.aggregatedDamage(targetId);
187:                 TideMarkSuccess tide = TideMarkSuccess.of(marks, player.getUUID());
188:                 FlowSuccessPayload flowPayload = this.flowLedger.payload(targetId);
189:                 if (flowPayload != null) {
190:                     if (!this.ownerReady()) {
191:                         continue;
192:                     }
193:                     float mergedAmount = DomainAuraResolver.merge(total, player,
194:                             AttackEnchantmentContext.Kind.ROOT_TECHNIQUE);
195:                     boolean precise = TechniqueHits.hurtConfirmed(target, player, mergedAmount,
196:                             TechniqueHits.Landing.RESPECT_INVULNERABILITY, tide);
197:                     if (precise) {
198:                         flowPayload.apply(target);
199:                     }
200:                     com.bslx.XueXiMod.shu.technique.release.payload.WetPressure.afterFlushed(this, target, precise);
201:                 } else {
202:                     boolean landed = TechniqueHits.hurtRoot(target, player, total, tide);
203:                     com.bslx.XueXiMod.shu.technique.release.payload.WetPressure.afterFlushed(this, target, landed);
204:                 }
205:             }
206:             this.flowLedger.clearSettled();
207:             targets.clear();
208:         }
209:     }
210: 
211:     private static double distanceToSegment(Vec3 point, Vec3 start, Vec3 end) {
212:         Vec3 span = end.subtract(start);
213:         double length2 = span.lengthSqr();
214:         if (length2 < 1.0E-6) {
215:             return point.distanceTo(start);
216:         }
217:         double scaleFactor = Math.max(0.0, Math.min(1.0, point.subtract(start).dot(span) / length2));
218:         return point.distanceTo(start.add(span.scale(scaleFactor)));
219:     }
220: }
221: 
</source_file>
