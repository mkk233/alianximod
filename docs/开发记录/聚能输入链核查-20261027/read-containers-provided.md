你是主控调度的deepseek-flash专项只读读取代理。唯一GPT-6-astra已先给出调查切块，报告齐后再统一裁决。下面是已提供的带行号源码，只分析材料，禁止工具、禁止改文件、禁止编译、禁止另派代理。用户要求聚能只作用元素术式，剑术即使带元素也不允许；能量源输入量提高应带动范围/特效大小/持续时间，未聚能基线不变。不要按water_ring或water_mist id建议打补丁。区分确定事实/猜测/缺失材料，精确路径行号、调用链、字段公式、最早丢失倍率点、建议后续核实文件。不要输出长源码，不做最终设计，中文报告1000-1800字。根README不存在，不补造依据。
专项B：领域/护体容器。DomainDefinition→Service→Instance→Geometry/寿命→Sync→Client，以及GuardDefinition→States→实际偏转/接触几何/寿命→Sync→Renderer。找常量与实例参数、刷新/覆盖行为、输入能量是否消费、渲染是否同源。列出缺失的调用入口但不要推测，把需要公共参数化的字段与最小文件说明清楚。

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

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainDefinition.java">
1: package com.bslx.XueXiMod.shu.technique.release.domain;
2: 
3: import net.minecraft.server.level.ServerPlayer;
4: import net.minecraft.world.phys.Vec3;
5: 
6: /** 一份领域声明。盒子尺寸由各术式传入，公共代码不写死雾雨。 */
7: public final class DomainDefinition {
8:     private final Placer placer;
9:     private final int initialCost;
10:     private final int upkeepCost;
11:     private final int upkeepPeriod;
12:     private final int lifetimeTicks;
13:     private final DomainNativePolicy nativePolicy;
14:     private final DomainAuraSpec auraSpec;
15:     private final boolean learnsWhenReady;
16:     private final DomainBoundary boundary;
17:     private final DomainVisual visual;
18: 
19:     public DomainDefinition(Placer placer, int initialCost, int upkeepCost, int upkeepPeriod, int lifetimeTicks,
20:                             DomainNativePolicy nativePolicy, DomainAuraSpec auraSpec, boolean learnsWhenReady) {
21:         this(placer, initialCost, upkeepCost, upkeepPeriod, lifetimeTicks, nativePolicy, auraSpec, learnsWhenReady,
22:                 DomainBoundary.SEAL, DomainVisual.VOLUME);
23:     }
24: 
25:     public DomainDefinition(Placer placer, int initialCost, int upkeepCost, int upkeepPeriod, int lifetimeTicks,
26:                             DomainNativePolicy nativePolicy, DomainAuraSpec auraSpec, boolean learnsWhenReady,
27:                             DomainBoundary boundary, DomainVisual visual) {
28:         this.placer = placer;
29:         this.initialCost = initialCost;
30:         this.upkeepCost = upkeepCost;
31:         this.upkeepPeriod = upkeepPeriod;
32:         this.lifetimeTicks = lifetimeTicks;
33:         this.nativePolicy = nativePolicy;
34:         this.auraSpec = auraSpec;
35:         this.learnsWhenReady = learnsWhenReady;
36:         this.boundary = boundary == null ? DomainBoundary.SEAL : boundary;
37:         this.visual = visual == null ? DomainVisual.VOLUME : visual;
38:     }
39: 
40:     public static DomainDefinition mistRain() {
41:         return new DomainDefinition(
42:                 player -> AxisBoxGeometry.atFeet(player.position(), 7.0, 7.0, 8.0),
43:                 40, 8, 20, 200,
44:                 new DomainNativePolicy("mist_rain", 10, 2, 10, 20, 2, 20),
45:                 DomainAuraSpec.tideNeutral(),
46:                 true);
47:     }
48: 
49:     public DomainGeometry place(ServerPlayer player) {
50:         return placer.place(player);
51:     }
52: 
53:     public int initialCost() {
54:         return initialCost;
55:     }
56: 
57:     public int upkeepCost() {
58:         return upkeepCost;
59:     }
60: 
61:     public int upkeepPeriod() {
62:         return upkeepPeriod;
63:     }
64: 
65:     public int lifetimeTicks() {
66:         return lifetimeTicks;
67:     }
68: 
69:     public DomainNativePolicy nativePolicy() {
70:         return nativePolicy;
71:     }
72: 
73:     public DomainAuraSpec auraSpec() {
74:         return auraSpec;
75:     }
76: 
77:     public boolean learnsWhenReady() {
78:         return learnsWhenReady;
79:     }
80: 
81:     public DomainBoundary boundary() {
82:         return boundary;
83:     }
84: 
85:     public DomainVisual visual() {
86:         return visual;
87:     }
88: 
89:     /** 7×7×3，六面开放。减速、潮湿和加倍耗氧，不产原生水势，也不封人。 */
90:     public static DomainDefinition deepPressure() {
91:         return new DomainDefinition(
92:                 player -> AxisBoxGeometry.atFeet(player.position(), 7.0, 7.0, 3.0),
93:                 40, 8, 20, 200,
94:                 new DomainNativePolicy("deep_pressure", 10, 4, 10, 20, 0, 20, 0, 20, 10, 10, 2f),
95:                 DomainAuraSpec.tideNeutral(), true,
96:                 DomainBoundary.OPEN, DomainVisual.PRESSURE);
97:     }
98: 
99:     @FunctionalInterface
100:     public interface Placer {
101:         DomainGeometry place(ServerPlayer player);
102:     }
103: 
104:     public static DomainGeometry feetBox(Vec3 feet, double width, double depth, double height) {
105:         return AxisBoxGeometry.atFeet(feet, width, depth, height);
106:     }
107: }
108: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainService.java">
1: package com.bslx.XueXiMod.shu.technique.release.domain;
2: 
3: import com.bslx.XueXiMod.network.DomainSyncPacket;
4: import com.bslx.XueXiMod.player.MagicPool;
5: import com.bslx.XueXiMod.player.ManaPaymentReceipt;
6: import com.bslx.XueXiMod.player.PlayerShuData;
7: import com.bslx.XueXiMod.shu.Technique;
8: import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
9: import com.bslx.XueXiMod.shu.technique.release.field.FieldZones;
10: import net.minecraft.server.MinecraftServer;
11: import net.minecraft.server.level.ServerLevel;
12: import net.minecraft.server.level.ServerPlayer;
13: import net.minecraft.world.phys.Vec3;
14: 
15: import java.util.ArrayList;
16: import java.util.HashSet;
17: import java.util.List;
18: 
19: /** 同一波先预检再一起发布。维护失败或到期就整份关掉。 */
20: public final class DomainService {
21:     private static final ArrayList<DomainInstance> ACTIVE = new ArrayList<>();
22:     private static final HashSet<String> PLACED_THIS_TICK = new HashSet<>();
23:     private static int placedTick = Integer.MIN_VALUE;
24:     private static boolean legacySwept;
25: 
26:     private DomainService() {
27:     }
28: 
29:     public static void onServerStarted() {
30:         if (legacySwept) {
31:             return;
32:         }
33:         legacySwept = true;
34:         FieldZones.retireLegacyMist();
35:         FieldZones.retireLegacyPressure();
36:     }
37: 
38:     public static boolean tryWave(ServerPlayer player, Technique source, DomainDefinition definition, int copies, EnchantmentSnapshot snapshot) {
39:         return releaseWave(player, source, definition, copies, snapshot, null, false, null);
40:     }
41: 
42:     /** 费用已经由父会话预留。feet 不为空时整盒平移到接触点，不再跟施法者脚下。 */
43:     public static boolean tryWaveReserved(ServerPlayer player, Technique source, DomainDefinition definition, int copies,
44:                                           EnchantmentSnapshot snapshot, Vec3 feet, String claimSuffix) {
45:         return releaseWave(player, source, definition, copies, snapshot, feet, true, claimSuffix);
46:     }
47: 
48:     private static boolean releaseWave(ServerPlayer player, Technique source, DomainDefinition definition, int copies,
49:                                        EnchantmentSnapshot snapshot, Vec3 feet, boolean reserved, String claimSuffix) {
50:         if (player == null || source == null || definition == null || !(player.level() instanceof ServerLevel level)) {
51:             return false;
52:         }
53:         if (!PlayerShuData.get(player).isUnlocked(source)) {
54:             player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c还没有学会释放" + source.getName()));
55:             return false;
56:         }
57:         int count = Math.max(1, copies);
58:         int now = level.getServer().getTickCount();
59:         if (placedTick != now) {
60:             PLACED_THIS_TICK.clear();
61:             placedTick = now;
62:         }
63:         String kind = definition.nativePolicy() == null ? "plain" : definition.nativePolicy().effectKey();
64:         String claim = player.getUUID() + ":" + kind + ":" + definition.initialCost() + ":" + definition.lifetimeTicks();
65:         if (claimSuffix != null) {
66:             claim = claim + ":" + claimSuffix;
67:         }
68:         if (PLACED_THIS_TICK.contains(claim)) {
69:             return true;
70:         }
71:         int bill = definition.initialCost() * count;
72:         if (!reserved && !MagicPool.canAfford(player, bill)) {
73:             player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c魔力不足"));
74:             return false;
75:         }
76:         List<DomainInstance> pending = new ArrayList<>();
77:         for (int copy = 0; copy < count; copy++) {
78:             DomainGeometry geometry = definition.place(player);
79:             if (geometry != null && feet != null) {
80:                 Vec3 delta = feet.subtract(player.position());
81:                 geometry = new AxisBoxGeometry(geometry.bounds().move(delta.x, delta.y, delta.z));
82:             }
83:             if (geometry == null) {
84:                 discard(pending);
85:                 return false;
86:             }
87:             pending.add(new DomainInstance(player.getUUID(), level, geometry, definition, now));
88:         }
89:         if (!reserved) {
90:             ManaPaymentReceipt receipt = ManaPaymentReceipt.charge(player, bill, true);
91:             if (receipt == null) {
92:                 discard(pending);
93:                 return false;
94:             }
95:         }
96:         for (DomainInstance domain : pending) {
97:             domain.bindContact(snapshot);
98:             ACTIVE.add(domain);
99:             DomainSyncPacket.broadcast(domain, false);
100:         }
101:         if (!reserved && bill > 0) {
102:             MagicPool.showBalance(player);
103:         }
104:         PLACED_THIS_TICK.add(claim);
105:         return true;
106:     }
107: 
108:     public static void tick(MinecraftServer server) {
109:         if (server == null) {
110:             return;
111:         }
112:         int now = server.getTickCount();
113:         for (int index = ACTIVE.size() - 1; index >= 0; index--) {
114:             DomainInstance domain = ACTIVE.get(index);
115:             if (!domain.active()) {
116:                 ACTIVE.remove(index);
117:                 continue;
118:             }
119:             ServerPlayer owner = server.getPlayerList().getPlayer(domain.ownerId());
120:             if (owner == null || !owner.isAlive() || owner.level() != domain.hostLevel() || now >= domain.expiresAt()) {
121:                 closeAt(index, domain);
122:                 continue;
123:             }
124:             if (now >= domain.nextUpkeepAt()) {
125:                 if (!MagicPool.trySpend(owner, domain.definition().upkeepCost(), false)) {
126:                     closeAt(index, domain);
127:                     continue;
128:                 }
129:                 domain.setNextUpkeepAt(now + domain.definition().upkeepPeriod());
130:             }
131:         }
132:         DomainPulses.fire(ACTIVE, now);
133:     }
134: 
135:     public static void closeOwned(ServerPlayer player) {
136:         if (player == null) {
137:             return;
138:         }
139:         for (int index = ACTIVE.size() - 1; index >= 0; index--) {
140:             DomainInstance domain = ACTIVE.get(index);
141:             if (player.getUUID().equals(domain.ownerId())) {
142:                 closeAt(index, domain);
143:             }
144:         }
145:     }
146: 
147:     public static List<DomainInstance> active() {
148:         return ACTIVE;
149:     }
150: 
151:     private static void closeAt(int index, DomainInstance domain) {
152:         domain.close();
153:         ACTIVE.remove(index);
154:         DomainSyncPacket.broadcast(domain, true);
155:     }
156: 
157:     private static void discard(List<DomainInstance> pending) {
158:         for (DomainInstance domain : pending) {
159:             domain.close();
160:         }
161:     }
162: }
163: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainInstance.java">
1: package com.bslx.XueXiMod.shu.technique.release.domain;
2: 
3: import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
4: import com.bslx.XueXiMod.shu.technique.release.contact.ContactCarrier;
5: import com.bslx.XueXiMod.shu.technique.release.contact.ContactEnchantState;
6: import net.minecraft.server.level.ServerLevel;
7: import net.minecraft.world.entity.LivingEntity;
8: import net.minecraft.world.phys.AABB;
9: 
10: import java.util.UUID;
11: 
12: /** 一个已经落下的领域。几何固定，供给不写成玩家身上的永久状态。 */
13: public final class DomainInstance implements ContactCarrier, DomainBound {
14:     private final UUID instanceId = UUID.randomUUID();
15:     private final UUID ownerId;
16:     private final ServerLevel level;
17:     private final DomainGeometry geometry;
18:     private final DomainDefinition definition;
19:     private final int revision = 1;
20:     private final int startedAt;
21:     private final int expiresAt;
22:     private final long visualOrigin;
23:     private final long visualEnd;
24:     private final int animSeed;
25:     private int nextUpkeepAt;
26:     private boolean active = true;
27:     private ContactEnchantState contact;
28: 
29:     DomainInstance(UUID ownerId, ServerLevel level, DomainGeometry geometry, DomainDefinition definition, int startedAt) {
30:         this.ownerId = ownerId;
31:         this.level = level;
32:         this.geometry = geometry;
33:         this.definition = definition;
34:         this.startedAt = startedAt;
35:         this.expiresAt = startedAt + definition.lifetimeTicks();
36:         this.visualOrigin = level.getGameTime();
37:         this.visualEnd = this.visualOrigin + definition.lifetimeTicks();
38:         this.animSeed = Math.floorMod(instanceId.hashCode(), 16);
39:         this.nextUpkeepAt = startedAt + definition.upkeepPeriod();
40:     }
41: 
42:     void bindContact(EnchantmentSnapshot snapshot) {
43:         if (snapshot == null) {
44:             return;
45:         }
46:         this.contact = ContactEnchantState.open(this, snapshot);
47:     }
48: 
49:     void close() {
50:         if (!active) {
51:             return;
52:         }
53:         active = false;
54:         if (contact != null) {
55:             contact.close();
56:             contact = null;
57:         }
58:     }
59: 
60:     public UUID instanceId() {
61:         return instanceId;
62:     }
63: 
64:     public DomainGeometry geometry() {
65:         return geometry;
66:     }
67: 
68:     public DomainDefinition definition() {
69:         return definition;
70:     }
71: 
72:     public int startedAt() {
73:         return startedAt;
74:     }
75: 
76:     public int expiresAt() {
77:         return expiresAt;
78:     }
79: 
80:     public int nextUpkeepAt() {
81:         return nextUpkeepAt;
82:     }
83: 
84:     void setNextUpkeepAt(int tick) {
85:         this.nextUpkeepAt = tick;
86:     }
87: 
88:     public float auraDamage() {
89:         return definition.auraSpec() == null ? 0f : definition.auraSpec().bonusDamage();
90:     }
91: 
92:     @Override
93:     public UUID hostId() {
94:         return instanceId;
95:     }
96: 
97:     @Override
98:     public int revision() {
99:         return revision;
100:     }
101: 
102:     @Override
103:     public DomainBoundary boundary() {
104:         return definition.boundary();
105:     }
106: 
107:     public DomainVisual visual() {
108:         return definition.visual();
109:     }
110: 
111:     public long visualOrigin() {
112:         return visualOrigin;
113:     }
114: 
115:     public long visualEnd() {
116:         return visualEnd;
117:     }
118: 
119:     public int animSeed() {
120:         return animSeed;
121:     }
122: 
123:     @Override
124:     public UUID ownerId() {
125:         return ownerId;
126:     }
127: 
128:     @Override
129:     public boolean active() {
130:         return active;
131:     }
132: 
133:     @Override
134:     public boolean touches(LivingEntity target) {
135:         return target != null && geometry.intersects(target.getBoundingBox());
136:     }
137: 
138:     @Override
139:     public ServerLevel hostLevel() {
140:         return level;
141:     }
142: 
143:     @Override
144:     public AABB queryBox() {
145:         return geometry.bounds().inflate(1.0);
146:     }
147: }
148: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainGeometry.java">
1: package com.bslx.XueXiMod.shu.technique.release.domain;
2: 
3: import net.minecraft.world.phys.AABB;
4: import net.minecraft.world.phys.Vec3;
5: 
6: /** 领域的固定范围。完整包含和体积相交是两件事。 */
7: public interface DomainGeometry {
8:     AABB bounds();
9: 
10:     boolean fullyContains(AABB body);
11: 
12:     boolean intersects(AABB body);
13: 
14:     /** 位移过程中身体完整位于域内的参数区间。放不进去时为空。 */
15:     double[] fullInterval(AABB body, Vec3 displacement);
16: 
17:     /** 已完全进入后，只缩短会穿出边界的轴，其余方向保持原位移。 */
18:     default Vec3 clipInside(AABB body, Vec3 movement) {
19:         return movement;
20:     }
21: 
22:     boolean fits(AABB body);
23: }
24: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainBound.java">
1: package com.bslx.XueXiMod.shu.technique.release.domain;
2: 
3: import com.bslx.XueXiMod.shu.technique.release.relations.Relations;
4: import net.minecraft.world.entity.Entity;
5: 
6: import java.util.UUID;
7: 
8: /** 服务端实例和客户端快照都能拿来裁切移动。 */
9: public interface DomainBound {
10:     UUID ownerId();
11: 
12:     DomainGeometry geometry();
13: 
14:     boolean active();
15: 
16:     default DomainBoundary boundary() {
17:         return DomainBoundary.SEAL;
18:     }
19: 
20:     default boolean passes(Entity member) {
21:         return Relations.permits(ownerId(), member) || Relations.ally(ownerId(), member);
22:     }
23: }
24: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainPulses.java">
1: package com.bslx.XueXiMod.shu.technique.release.domain;
2: 
3: import com.bslx.XueXiMod.shu.technique.release.momentum.WaterMomentum;
4: import com.bslx.XueXiMod.shu.technique.release.relations.Relations;
5: import com.bslx.XueXiMod.status.WetnessState;
6: import net.minecraft.server.level.ServerPlayer;
7: import net.minecraft.world.effect.MobEffectInstance;
8: import net.minecraft.world.effect.MobEffects;
9: import net.minecraft.world.entity.LivingEntity;
10: import net.minecraft.world.phys.AABB;
11: 
12: import java.util.HashMap;
13: import java.util.UUID;
14: 
15: /** 同一轮先收齐候选再给一次。潮湿取最大，减速不盖过更强或更长的效果。 */
16: public final class DomainPulses {
17:     private static final HashMap<UUID, WetWindow> WET_WINDOWS = new HashMap<>();
18:     private static final HashMap<UUID, Integer> NEXT_MOMENTUM = new HashMap<>();
19: 
20:     private DomainPulses() {
21:     }
22: 
23:     public static void clear() {
24:         WET_WINDOWS.clear();
25:         NEXT_MOMENTUM.clear();
26:     }
27: 
28:     public static void fire(Iterable<DomainInstance> domains, int now) {
29:         HashMap<UUID, WetOffer> wetOffers = new HashMap<>();
30:         HashMap<UUID, SlowOffer> slowOffers = new HashMap<>();
31:         for (DomainInstance domain : domains) {
32:             if (!domain.active() || domain.definition().nativePolicy() == null) {
33:                 continue;
34:             }
35:             DomainNativePolicy policy = domain.definition().nativePolicy();
36:             if (policy.wetAmount() > 0 && due(now, domain.startedAt(), policy.wetDelay(), policy.wetPeriod())) {
37:                 collectWet(domain, policy, wetOffers);
38:             }
39:             if (policy.slowAmplifier() >= 0 && due(now, domain.startedAt(), policy.slowDelay(), policy.slowPeriod())) {
40:                 collectSlow(domain, policy, slowOffers);
41:             }
42:             if (policy.momentumAmount() > 0 && due(now, domain.startedAt(), policy.momentumDelay(), policy.momentumPeriod())) {
43:                 momentumInside(domain, policy, now);
44:             }
45:         }
46:         for (WetOffer offer : wetOffers.values()) {
47:             grantWet(offer, now);
48:         }
49:         for (SlowOffer offer : slowOffers.values()) {
50:             applySlow(offer);
51:         }
52:     }
53: 
54:     private static void grantWet(WetOffer offer, int now) {
55:         UUID targetId = offer.living.getUUID();
56:         WetWindow window = WET_WINDOWS.get(targetId);
57:         if (window == null || now >= window.untilTick) {
58:             WetnessState.grant(offer.living, offer.amount);
59:             WET_WINDOWS.put(targetId, new WetWindow(now + offer.period, offer.amount));
60:             return;
61:         }
62:         if (offer.amount <= window.grantedAmount) {
63:             return;
64:         }
65:         WetnessState.grant(offer.living, offer.amount - window.grantedAmount);
66:         window.grantedAmount = offer.amount;
67:     }
68: 
69:     private static boolean due(int now, int startedAt, int delay, int period) {
70:         if (period <= 0 || now < startedAt + delay) {
71:             return false;
72:         }
73:         return (now - startedAt) % period == 0;
74:     }
75: 
76:     private static void collectWet(DomainInstance domain, DomainNativePolicy policy, HashMap<UUID, WetOffer> offers) {
77:         AABB area = domain.geometry().bounds();
78:         for (LivingEntity living : domain.hostLevel().getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
79:             if (!DomainNative.hostileInside(domain, living)) {
80:                 continue;
81:             }
82:             WetOffer current = offers.get(living.getUUID());
83:             if (current != null && current.amount >= policy.wetAmount()) {
84:                 continue;
85:             }
86:             offers.put(living.getUUID(), new WetOffer(living, policy.wetAmount(), policy.wetPeriod()));
87:         }
88:     }
89: 
90:     private static void collectSlow(DomainInstance domain, DomainNativePolicy policy, HashMap<UUID, SlowOffer> offers) {
91:         AABB area = domain.geometry().bounds();
92:         for (LivingEntity living : domain.hostLevel().getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
93:             if (!DomainNative.hostileInside(domain, living)) {
94:                 continue;
95:             }
96:             SlowOffer current = offers.get(living.getUUID());
97:             if (current != null && current.amplifier > policy.slowAmplifier()) {
98:                 continue;
99:             }
100:             if (current != null && current.amplifier == policy.slowAmplifier() && current.duration >= policy.slowDuration()) {
101:                 continue;
102:             }
103:             offers.put(living.getUUID(), new SlowOffer(living, policy.slowAmplifier(), policy.slowDuration()));
104:         }
105:     }
106: 
107:     private static void applySlow(SlowOffer offer) {
108:         MobEffectInstance current = offer.living.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
109:         if (current != null && current.getAmplifier() > offer.amplifier) {
110:             return;
111:         }
112:         if (current != null && current.getAmplifier() == offer.amplifier && current.getDuration() >= offer.duration) {
113:             return;
114:         }
115:         offer.living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, offer.duration, offer.amplifier));
116:     }
117: 
118:     private static void momentumInside(DomainInstance domain, DomainNativePolicy policy, int now) {
119:         AABB area = domain.geometry().bounds().inflate(1.0);
120:         for (ServerPlayer player : domain.hostLevel().players()) {
121:             if (!player.isAlive() || player.isSpectator()) {
122:                 continue;
123:             }
124:             if (!Relations.permits(domain.ownerId(), player) && !Relations.ally(domain.ownerId(), player)) {
125:                 continue;
126:             }
127:             if (!domain.geometry().fits(player.getBoundingBox())
128:                     || !domain.geometry().fullyContains(player.getBoundingBox())) {
129:                 continue;
130:             }
131:             if (!area.intersects(player.getBoundingBox())) {
132:                 continue;
133:             }
134:             int ready = NEXT_MOMENTUM.getOrDefault(player.getUUID(), 0);
135:             if (now < ready) {
136:                 continue;
137:             }
138:             WaterMomentum.credit(player, policy.momentumAmount());
139:             NEXT_MOMENTUM.put(player.getUUID(), now + policy.momentumPeriod());
140:         }
141:     }
142: 
143:     private static final class WetWindow {
144:         private final int untilTick;
145:         private int grantedAmount;
146: 
147:         private WetWindow(int untilTick, int grantedAmount) {
148:             this.untilTick = untilTick;
149:             this.grantedAmount = grantedAmount;
150:         }
151:     }
152: 
153:     private static final class WetOffer {
154:         private final LivingEntity living;
155:         private final int amount;
156:         private final int period;
157: 
158:         private WetOffer(LivingEntity living, int amount, int period) {
159:             this.living = living;
160:             this.amount = amount;
161:             this.period = period;
162:         }
163:     }
164: 
165:     private static final class SlowOffer {
166:         private final LivingEntity living;
167:         private final int amplifier;
168:         private final int duration;
169: 
170:         private SlowOffer(LivingEntity living, int amplifier, int duration) {
171:             this.living = living;
172:             this.amplifier = amplifier;
173:             this.duration = duration;
174:         }
175:     }
176: }
177: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/guard/GuardDefinition.java">
1: package com.bslx.XueXiMod.shu.technique.release.guard;
2: 
3: /**
4:  * 随身护体的参数。没有声明护体的术式保持原样。
5:  * 类型键用来认同一类护体，公共入口不按术式编号分支。
6:  */
7: public final class GuardDefinition {
8:     private final String typeKey;
9:     private final int lifetimeTicks;
10:     private final int spawnCost;
11:     private final int upkeepCost;
12:     private final int upkeepPeriod;
13:     private final float unloadRatio;
14:     private final double radius;
15:     private final double band;
16:     private final double bodyHeight;
17:     private final float baseTilt;
18:     private final boolean deflectsProjectiles;
19: 
20:     public GuardDefinition(String typeKey, int lifetimeTicks, int spawnCost, int upkeepCost, int upkeepPeriod,
21:                            float unloadRatio, double radius, double band, double bodyHeight, float baseTilt) {
22:         this(typeKey, lifetimeTicks, spawnCost, upkeepCost, upkeepPeriod, unloadRatio, radius, band, bodyHeight, baseTilt, false);
23:     }
24: 
25:     public GuardDefinition(String typeKey, int lifetimeTicks, int spawnCost, int upkeepCost, int upkeepPeriod,
26:                            float unloadRatio, double radius, double band, double bodyHeight, float baseTilt,
27:                            boolean deflectsProjectiles) {
28:         this.typeKey = typeKey;
29:         this.lifetimeTicks = lifetimeTicks;
30:         this.spawnCost = spawnCost;
31:         this.upkeepCost = upkeepCost;
32:         this.upkeepPeriod = upkeepPeriod;
33:         this.unloadRatio = unloadRatio;
34:         this.radius = radius;
35:         this.band = band;
36:         this.bodyHeight = bodyHeight;
37:         this.baseTilt = baseTilt;
38:         this.deflectsProjectiles = deflectsProjectiles;
39:     }
40: 
41:     /** 水环显式承接来袭弹。其它护体构造保持关闭。 */
42:     public static GuardDefinition personalRing() {
43:         return new GuardDefinition("personal_ring", 200, 8, 2, 20, 0.20f, 0.9, 0.12, 0.55, 0f, true);
44:     }
45: 
46:     public String typeKey() {
47:         return typeKey;
48:     }
49: 
50:     public int lifetimeTicks() {
51:         return lifetimeTicks;
52:     }
53: 
54:     public int spawnCost() {
55:         return spawnCost;
56:     }
57: 
58:     public int upkeepCost() {
59:         return upkeepCost;
60:     }
61: 
62:     public int upkeepPeriod() {
63:         return upkeepPeriod;
64:     }
65: 
66:     public float unloadRatio() {
67:         return unloadRatio;
68:     }
69: 
70:     public double radius() {
71:         return radius;
72:     }
73: 
74:     public double band() {
75:         return band;
76:     }
77: 
78:     public double bodyHeight() {
79:         return bodyHeight;
80:     }
81: 
82:     public float baseTilt() {
83:         return baseTilt;
84:     }
85: 
86:     public boolean deflectsProjectiles() {
87:         return deflectsProjectiles;
88:     }
89: }
90: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/guard/GuardStates.java">
1: package com.bslx.XueXiMod.shu.technique.release.guard;
2: 
3: import com.bslx.XueXiMod.network.GuardSyncPacket;
4: import com.bslx.XueXiMod.network.ModNetwork;
5: import com.bslx.XueXiMod.player.MagicPool;
6: import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHosts;
7: import com.bslx.XueXiMod.shu.technique.release.deflection.hosts.RingBandGeometry;
8: import com.bslx.XueXiMod.shu.technique.release.deflection.hosts.RingDeflectionHost;
9: import com.bslx.XueXiMod.shu.technique.release.deflection.hosts.RingFollow;
10: import com.bslx.XueXiMod.shu.technique.release.momentum.ChargeGate;
11: import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
12: import com.bslx.XueXiMod.shu.technique.release.contact.ContactCarrier;
13: import com.bslx.XueXiMod.shu.technique.release.contact.ContactEnchantState;
14: import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
15: import net.minecraft.server.MinecraftServer;
16: import net.minecraft.server.level.ServerLevel;
17: import net.minecraft.server.level.ServerPlayer;
18: import net.minecraft.world.entity.LivingEntity;
19: import net.minecraft.world.phys.AABB;
20: import net.minecraft.world.phys.Vec3;
21: import net.minecraft.resources.ResourceKey;
22: import net.minecraft.world.level.Level;
23: 
24: import java.util.ArrayList;
25: import java.util.Iterator;
26: import java.util.UUID;
27: import java.util.concurrent.ConcurrentHashMap;
28: 
29: /** 每人每类护体最多一个。不进场地表，也不增加场地层数。 */
30: public final class GuardStates {
31:     private static final ArrayList<BodyGuard> ACTIVE = new ArrayList<>();
32:     private static final ConcurrentHashMap<UUID, ConcurrentHashMap<Long, Integer>> WAVES = new ConcurrentHashMap<>();
33: 
34:     private GuardStates() {
35:     }
36: 
37:     /** 当下就能生成时返回是否生成成功。排到以后的，只表示已经排上。 */
38:     public static boolean schedule(ServerPlayer player, GuardDefinition guard, int fireTick, long waveKey) {
39:         if (player == null || guard == null || player.getServer() == null) {
40:             return false;
41:         }
42:         UUID playerId = player.getUUID();
43:         int epoch = ChargeGate.epoch(playerId);
44:         if (fireTick <= player.getServer().getTickCount()) {
45:             if (!ChargeGate.same(playerId, epoch) || !player.isAlive()) {
46:                 return false;
47:             }
48:             return activate(player, guard, waveKey);
49:         }
50:         ChainScheduler.schedule(player.getServer(), fireTick, () -> {
51:             if (!ChargeGate.same(playerId, epoch) || !player.isAlive()) {
52:                 return;
53:             }
54:             activate(player, guard, waveKey);
55:         });
56:         return true;
57:     }
58: 
59:     public static boolean activate(ServerPlayer player, GuardDefinition guard, long waveKey) {
60:         return activate(player, guard, waveKey, false);
61:     }
62: 
63:     /** 生成费已由父会话预留。 */
64:     public static boolean activateReserved(ServerPlayer player, GuardDefinition guard, long waveKey) {
65:         return activate(player, guard, waveKey, true);
66:     }
67: 
68:     private static boolean activate(ServerPlayer player, GuardDefinition guard, long waveKey, boolean reserved) {
69:         if (player == null || guard == null || player.getServer() == null) {
70:             return false;
71:         }
72:         if (!claimWave(player.getUUID(), waveKey, player.getServer().getTickCount())) {
73:             return true;
74:         }
75:         if (!reserved && !MagicPool.trySpend(player, guard.spawnCost(), true)) {
76:             forgetWave(player.getUUID(), waveKey);
77:             return false;
78:         }
79:         if (!reserved && guard.spawnCost() > 0) {
80:             MagicPool.showBalance(player);
81:         }
82:         int now = player.getServer().getTickCount();
83:         BodyGuard existing = find(player.getUUID(), guard.typeKey());
84:         if (existing != null) {
85:             detachDeflection(existing);
86:             existing.expiresAt = now + guard.lifetimeTicks();
87:             existing.revision += 1;
88:             existing.dimension = player.level().dimension();
89:             existing.serverLevel = player.serverLevel();
90:             attachDeflection(existing);
91:             broadcastFull(player, existing);
92:             return true;
93:         }
94:         BodyGuard created = new BodyGuard();
95:         created.ownerId = player.getUUID();
96:         created.dimension = player.level().dimension();
97:         created.serverLevel = player.serverLevel();
98:         created.guard = guard;
99:         created.expiresAt = now + guard.lifetimeTicks();
100:         created.nextUpkeepAt = now + guard.upkeepPeriod();
101:         created.animationStart = now;
102:         created.revision = 1;
103:         created.fromNormal = GuardPose.baseNormal(guard.baseTilt());
104:         created.towardNormal = created.fromNormal;
105:         created.phaseStart = now;
106:         created.phaseEnd = now;
107:         ACTIVE.add(created);
108:         attachDeflection(created);
109:         broadcastFull(player, created);
110:         return true;
111:     }
112: 
113:     public static float unloadRatio(ServerPlayer player) {
114:         BodyGuard guard = living(player);
115:         if (guard == null) {
116:             return 0f;
117:         }
118:         float ratio = guard.guard.unloadRatio();
119:         if (!Float.isFinite(ratio) || ratio <= 0f) {
120:             return 0f;
121:         }
122:         return ratio > 0.6f ? 0.6f : ratio;
123:     }
124: 
125:     public static void strike(ServerPlayer player, int poseKind, Vec3 incoming) {
126:         BodyGuard guard = living(player);
127:         if (guard == null || poseKind == GuardPose.NONE) {
128:             return;
129:         }
130:         int now = player.getServer().getTickCount();
131:         Vec3 target = poseKind == GuardPose.MELEE ? GuardPose.worldLevel() : GuardPose.leanToward(incoming);
132:         beginMove(player, guard, target, GuardPose.CAUSE_HIT, GuardPose.ENTER_TICKS, now);
133:         guard.holdUntil = now + GuardPose.ENTER_TICKS + GuardPose.HOLD_TICKS;
134:         guard.returning = false;
135:     }
136: 
137:     public static void tick(MinecraftServer server) {
138:         int now = server.getTickCount();
139:         Iterator<BodyGuard> iterator = ACTIVE.iterator();
140:         while (iterator.hasNext()) {
141:             BodyGuard guard = iterator.next();
142:             ServerPlayer owner = server.getPlayerList().getPlayer(guard.ownerId);
143:             if (owner == null || !owner.isAlive() || owner.level().dimension() != guard.dimension) {
144:                 drop(guard);
145:                 iterator.remove();
146:                 if (owner != null) {
147:                     broadcastRemove(owner, guard);
148:                 }
149:                 continue;
150:             }
151:             if (now >= guard.expiresAt) {
152:                 drop(guard);
153:                 iterator.remove();
154:                 broadcastRemove(owner, guard);
155:                 continue;
156:             }
157:             advancePose(owner, guard, now);
158:             if (now >= guard.nextUpkeepAt) {
159:                 if (!MagicPool.trySpend(owner, guard.guard.upkeepCost(), false)) {
160:                     drop(guard);
161:                     iterator.remove();
162:                     broadcastRemove(owner, guard);
163:                     continue;
164:                 }
165:                 guard.nextUpkeepAt += guard.guard.upkeepPeriod();
166:             }
167:             if ((now & 1) == 0) {
168:                 GuardPreview.look(owner, guard, now);
169:             }
170:         }
171:         forgetOldWaves(now);
172:     }
173: 
174:     public static void closeOwned(ServerPlayer player) {
175:         if (player == null) {
176:             return;
177:         }
178:         UUID playerId = player.getUUID();
179:         Iterator<BodyGuard> iterator = ACTIVE.iterator();
180:         while (iterator.hasNext()) {
181:             BodyGuard guard = iterator.next();
182:             if (!guard.ownerId.equals(playerId)) {
183:                 continue;
184:             }
185:             iterator.remove();
186:             drop(guard);
187:             broadcastRemove(player, guard);
188:         }
189:         WAVES.remove(playerId);
190:     }
191: 
192:     public static GuardSyncPacket snapshot(UUID ownerId) {
193:         for (BodyGuard guard : ACTIVE) {
194:             if (guard.ownerId.equals(ownerId)) {
195:                 return fullPacket(guard);
196:             }
197:         }
198:         return null;
199:     }
200: 
201:     static void preview(ServerPlayer owner, BodyGuard guard, Vec3 incoming, int entityId, int now) {
202:         if (guard.poseCause == GuardPose.CAUSE_HIT && now < guard.holdUntil) {
203:             return;
204:         }
205:         if (guard.previewEntityId == entityId && guard.previewDirection != null
206:                 && guard.previewDirection.distanceToSqr(incoming) < 0.04) {
207:             return;
208:         }
209:         guard.previewEntityId = entityId;
210:         guard.previewDirection = incoming;
211:         beginMove(owner, guard, GuardPose.leanToward(incoming), GuardPose.CAUSE_PREVIEW, GuardPose.ENTER_TICKS, now);
212:         guard.holdUntil = now + GuardPose.ENTER_TICKS + 4;
213:         guard.returning = false;
214:     }
215: 
216:     private static void advancePose(ServerPlayer owner, BodyGuard guard, int now) {
217:         if (guard.poseCause == GuardPose.CAUSE_NONE || now < guard.phaseEnd || now < guard.holdUntil || guard.returning) {
218:             if (guard.returning && now >= guard.phaseEnd) {
219:                 guard.poseCause = GuardPose.CAUSE_NONE;
220:                 guard.returning = false;
221:                 guard.previewEntityId = Integer.MIN_VALUE;
222:             }
223:             return;
224:         }
225:         Vec3 base = GuardPose.baseNormal(guard.guard.baseTilt());
226:         beginMove(owner, guard, base, guard.poseCause, GuardPose.RETURN_TICKS, now);
227:         guard.returning = true;
228:         guard.holdUntil = now;
229:     }
230: 
231:     private static void beginMove(ServerPlayer owner, BodyGuard guard, Vec3 target, int cause, int ticks, int now) {
232:         guard.fromNormal = guard.towardNormal == null ? GuardPose.baseNormal(guard.guard.baseTilt()) : guard.towardNormal;
233:         guard.towardNormal = target;
234:         guard.poseCause = cause;
235:         guard.phaseStart = now;
236:         guard.phaseEnd = now + Math.max(1, ticks);
237:         guard.sequence += 1;
238:         GuardSyncPacket.sendAround(owner, posePacket(guard, ticks));
239:     }
240: 
241:     private static BodyGuard living(ServerPlayer player) {
242:         if (player == null || player.getServer() == null) {
243:             return null;
244:         }
245:         BodyGuard guard = find(player.getUUID(), null);
246:         if (guard == null || player.getServer().getTickCount() >= guard.expiresAt) {
247:             return null;
248:         }
249:         if (player.level().dimension() != guard.dimension) {
250:             return null;
251:         }
252:         return guard;
253:     }
254: 
255:     private static BodyGuard find(UUID ownerId, String typeKey) {
256:         for (BodyGuard guard : ACTIVE) {
257:             if (!guard.ownerId.equals(ownerId)) {
258:                 continue;
259:             }
260:             if (typeKey == null || guard.guard.typeKey().equals(typeKey)) {
261:                 return guard;
262:             }
263:         }
264:         return null;
265:     }
266: 
267:     private static boolean claimWave(UUID playerId, long waveKey, int now) {
268:         ConcurrentHashMap<Long, Integer> waves = WAVES.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>());
269:         Integer previous = waves.putIfAbsent(waveKey, now);
270:         return previous == null;
271:     }
272: 
273:     private static void forgetWave(UUID playerId, long waveKey) {
274:         ConcurrentHashMap<Long, Integer> waves = WAVES.get(playerId);
275:         if (waves != null) {
276:             waves.remove(waveKey);
277:         }
278:     }
279: 
280:     private static void forgetOldWaves(int now) {
281:         for (ConcurrentHashMap<Long, Integer> waves : WAVES.values()) {
282:             waves.entrySet().removeIf(entry -> now - entry.getValue() > 400);
283:         }
284:     }
285: 
286:     private static void broadcastFull(ServerPlayer owner, BodyGuard guard) {
287:         GuardSyncPacket.sendAround(owner, fullPacket(guard));
288:     }
289: 
290:     public static void bindContact(ServerPlayer player, EnchantmentSnapshot snapshot) {
291:         BodyGuard guard = living(player);
292:         if (guard == null || snapshot == null) {
293:             return;
294:         }
295:         if (guard.contact != null) {
296:             guard.contact.close();
297:             guard.contact = null;
298:         }
299:         guard.contactRevision += 1;
300:         guard.contact = ContactEnchantState.open(new GuardTouch(guard), snapshot);
301:     }
302: 
303:     private static void drop(BodyGuard guard) {
304:         guard.retired = true;
305:         detachDeflection(guard);
306:         if (guard.contact != null) {
307:             guard.contact.close();
308:             guard.contact = null;
309:         }
310:     }
311: 
312:     private static void attachDeflection(BodyGuard guard) {
313:         detachDeflection(guard);
314:         if (guard.retired || guard.guard == null || !guard.guard.deflectsProjectiles() || guard.serverLevel == null) {
315:             return;
316:         }
317:         if (guard.ownerId == null || guard.hostId == null) {
318:             return;
319:         }
320:         guard.deflectionHost = new RingDeflectionHost(
321:                 new LiveRing(guard, guard.revision),
322:                 guard.ownerId,
323:                 guard.hostId,
324:                 guard.revision,
325:                 guard.serverLevel,
326:                 guard.guard.radius(),
327:                 guard.guard.band() * 0.5);
328:         DeflectionHosts.register(guard.deflectionHost);
329:     }
330: 
331:     private static void detachDeflection(BodyGuard guard) {
332:         if (guard.deflectionHost == null) {
333:             return;
334:         }
335:         DeflectionHosts.unregister(guard.deflectionHost.key());
336:         guard.deflectionHost = null;
337:     }
338: 
339:     /**
340:      * 偏转环面跟当前主人，法线用姿态插值。插值公式在 {@link RingBandGeometry#blendNormal}。
341:      * 主人不在本维度或这一代 revision 已经换掉时不再跟踪。
342:      */
343:     private static final class LiveRing implements RingFollow {
344:         private final BodyGuard guard;
345:         private final int boundRevision;
346: 
347:         private LiveRing(BodyGuard guard, int boundRevision) {
348:             this.guard = guard;
349:             this.boundRevision = boundRevision;
350:         }
351: 
352:         @Override
353:         public boolean live() {
354:             if (guard.retired || guard.revision != boundRevision || guard.serverLevel == null || guard.guard == null) {
355:                 return false;
356:             }
357:             if (guard.serverLevel.getServer() == null) {
358:                 return false;
359:             }
360:             int now = guard.serverLevel.getServer().getTickCount();
361:             if (now >= guard.expiresAt) {
362:                 return false;
363:             }
364:             ServerPlayer owner = guard.serverLevel.getServer().getPlayerList().getPlayer(guard.ownerId);
365:             return owner != null && owner.isAlive() && owner.level() == guard.serverLevel;
366:         }
367: 
368:         @Override
369:         public Vec3 center() {
370:             ServerPlayer owner = owner();
371:             if (owner == null || guard.guard == null) {
372:                 return Vec3.ZERO;
373:             }
374:             return owner.position().add(0.0, owner.getBbHeight() * guard.guard.bodyHeight(), 0.0);
375:         }
376: 
377:         @Override
378:         public Vec3 planeNormal() {
379:             int now = 0;
380:             if (guard.serverLevel != null && guard.serverLevel.getServer() != null) {
381:                 now = guard.serverLevel.getServer().getTickCount();
382:             }
383:             return RingBandGeometry.blendNormal(guard.fromNormal, guard.towardNormal, guard.phaseStart, guard.phaseEnd, now);
384:         }
385: 
386:         @Override
387:         public Vec3 ownerAnchor() {
388:             ServerPlayer owner = owner();
389:             if (owner == null) {
390:                 return center();
391:             }
392:             return owner.position();
393:         }
394: 
395:         private ServerPlayer owner() {
396:             if (guard.serverLevel == null || guard.serverLevel.getServer() == null || guard.ownerId == null) {
397:                 return null;
398:             }
399:             return guard.serverLevel.getServer().getPlayerList().getPlayer(guard.ownerId);
400:         }
401:     }
402: 
403:     private static void broadcastRemove(ServerPlayer owner, BodyGuard guard) {
404:         GuardSyncPacket packet = removePacket(guard);
405:         GuardSyncPacket.sendAround(owner, packet);
406:         ServerLevel savedLevel = guard.serverLevel;
407:         if (owner == null || savedLevel == null || savedLevel == owner.serverLevel()) {
408:             return;
409:         }
410:         for (ServerPlayer watcher : savedLevel.players()) {
411:             if (watcher == null || watcher == owner) {
412:                 continue;
413:             }
414:             ModNetwork.sendToPlayer(watcher, packet);
415:         }
416:     }
417: 
418:     private static GuardSyncPacket removePacket(BodyGuard guard) {
419:         int serverTick = serverTick(guard);
420:         return new GuardSyncPacket(GuardSyncPacket.REMOVE, guard.ownerId, guard.revision, guard.sequence,
421:                 0, 0, guard.animationStart, animationAgeTicks(serverTick, guard.animationStart),
422:                 0f, 0f, 0f, 0f,
423:                 0f, 0f, 0f,
424:                 0f, 0f, 0f, 0);
425:     }
426: 
427:     private static GuardSyncPacket fullPacket(BodyGuard guard) {
428:         int serverTick = serverTick(guard);
429:         SnapshotPose pose = snapshotPose(guard.fromNormal, guard.towardNormal, guard.phaseStart, guard.phaseEnd, serverTick);
430:         return new GuardSyncPacket(GuardSyncPacket.FULL, guard.ownerId, guard.revision, guard.sequence,
431:                 guard.expiresAt, guard.nextUpkeepAt, guard.animationStart, animationAgeTicks(serverTick, guard.animationStart),
432:                 guard.guard.baseTilt(),
433:                 (float) guard.guard.radius(), (float) guard.guard.band(), (float) guard.guard.bodyHeight(),
434:                 (float) pose.from.x, (float) pose.from.y, (float) pose.from.z,
435:                 (float) pose.toward.x, (float) pose.toward.y, (float) pose.toward.z,
436:                 pose.poseTicks);
437:     }
438: 
439:     private static GuardSyncPacket posePacket(BodyGuard guard, int ticks) {
440:         int serverTick = serverTick(guard);
441:         Vec3 from = guard.fromNormal;
442:         Vec3 toward = guard.towardNormal;
443:         return new GuardSyncPacket(GuardSyncPacket.POSE, guard.ownerId, guard.revision, guard.sequence,
444:                 guard.expiresAt, guard.nextUpkeepAt, guard.animationStart, animationAgeTicks(serverTick, guard.animationStart),
445:                 guard.guard.baseTilt(),
446:                 (float) guard.guard.radius(), (float) guard.guard.band(), (float) guard.guard.bodyHeight(),
447:                 (float) from.x, (float) from.y, (float) from.z,
448:                 (float) toward.x, (float) toward.y, (float) toward.z, ticks);
449:     }
450: 
451:     private static int serverTick(BodyGuard guard) {
452:         if (guard.serverLevel == null || guard.serverLevel.getServer() == null) {
453:             return 0;
454:         }
455:         return guard.serverLevel.getServer().getTickCount();
456:     }
457: 
458:     static int animationAgeTicks(int serverTick, int animationStart) {
459:         int elapsed = serverTick - animationStart;
460:         if (elapsed < 0) {
461:             return 0;
462:         }
463:         return elapsed;
464:     }
465: 
466:     static SnapshotPose snapshotPose(Vec3 fromNormal, Vec3 towardNormal, int phaseStart, int phaseEnd, int serverTick) {
467:         Vec3 target = towardNormal == null ? GuardPose.worldLevel() : towardNormal;
468:         boolean settled = phaseEnd <= phaseStart || serverTick >= phaseEnd;
469:         if (settled) {
470:             return new SnapshotPose(target, target, 0);
471:         }
472:         Vec3 storedFrom = fromNormal == null ? target : fromNormal;
473:         Vec3 blended = RingBandGeometry.blendNormal(storedFrom, target, phaseStart, phaseEnd, serverTick);
474:         int remaining = phaseEnd - serverTick;
475:         return new SnapshotPose(blended, target, remaining);
476:     }
477: 
478:     static final class SnapshotPose {
479:         final Vec3 from;
480:         final Vec3 toward;
481:         final int poseTicks;
482: 
483:         private SnapshotPose(Vec3 from, Vec3 toward, int poseTicks) {
484:             this.from = from;
485:             this.toward = toward;
486:             this.poseTicks = poseTicks;
487:         }
488:     }
489: 
490:     static final class BodyGuard {
491:         private UUID ownerId;
492:         private ResourceKey<Level> dimension;
493:         private GuardDefinition guard;
494:         private int expiresAt;
495:         private int nextUpkeepAt;
496:         private int animationStart;
497:         private int revision;
498:         private int sequence;
499:         private int poseCause;
500:         private int phaseStart;
501:         private int phaseEnd;
502:         private int holdUntil;
503:         private boolean returning;
504:         private Vec3 fromNormal = GuardPose.worldLevel();
505:         private Vec3 towardNormal = GuardPose.worldLevel();
506:         private Vec3 previewDirection;
507:         private int previewEntityId = Integer.MIN_VALUE;
508:         private UUID hostId = UUID.randomUUID();
509:         private ContactEnchantState contact;
510:         private int contactRevision = 1;
511:         private boolean retired;
512:         private ServerLevel serverLevel;
513:         private RingDeflectionHost deflectionHost;
514:     }
515: 
516:     private static final class GuardTouch implements ContactCarrier {
517:         private final BodyGuard guard;
518: 
519:         private GuardTouch(BodyGuard guard) {
520:             this.guard = guard;
521:         }
522: 
523:         @Override
524:         public UUID hostId() {
525:             return guard.hostId;
526:         }
527: 
528:         @Override
529:         public int revision() {
530:             return guard.contactRevision;
531:         }
532: 
533:         @Override
534:         public UUID ownerId() {
535:             return guard.ownerId;
536:         }
537: 
538:         @Override
539:         public boolean active() {
540:             return !guard.retired;
541:         }
542: 
543:         @Override
544:         public boolean touches(LivingEntity target) {
545:             if (target == null || guard.guard == null) {
546:                 return false;
547:             }
548:             ServerLevel level = hostLevel();
549:             if (level == null) {
550:                 return false;
551:             }
552:             ServerPlayer owner = level.getServer().getPlayerList().getPlayer(guard.ownerId);
553:             if (owner == null) {
554:                 return false;
555:             }
556:             Vec3 normal = guard.towardNormal == null ? GuardPose.worldLevel() : guard.towardNormal;
557:             if (normal.lengthSqr() < 1.0E-6) {
558:                 normal = GuardPose.worldLevel();
559:             }
560:             normal = normal.normalize();
561:             Vec3 center = owner.position().add(0.0, owner.getBbHeight() * guard.guard.bodyHeight(), 0.0);
562:             double radius = guard.guard.radius();
563:             double reach = guard.guard.band() * 0.5 + ContactEnchantState.SURFACE_SLOP;
564:             AABB body = target.getBoundingBox();
565:             Vec3 nearest = nearest(body, center);
566:             Vec3 relative = nearest.subtract(center);
567:             double along = relative.dot(normal);
568:             Vec3 planar = relative.subtract(normal.scale(along));
569:             double radial = planar.length();
570:             Vec3 ringPoint = radial < 1.0E-4
571:                     ? center
572:                     : center.add(planar.scale(radius / radial));
573:             Vec3 closest = nearest(body, ringPoint);
574:             return closest.distanceToSqr(ringPoint) <= reach * reach;
575:         }
576: 
577:         @Override
578:         public ServerLevel hostLevel() {
579:             return guard.serverLevel;
580:         }
581: 
582:         @Override
583:         public AABB queryBox() {
584:             ServerLevel level = hostLevel();
585:             if (level == null || guard.guard == null) {
586:                 return new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
587:             }
588:             ServerPlayer owner = level.getServer().getPlayerList().getPlayer(guard.ownerId);
589:             if (owner == null) {
590:                 return new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
591:             }
592:             double reach = guard.guard.radius() + guard.guard.band() + 2.0;
593:             return owner.getBoundingBox().inflate(reach);
594:         }
595: 
596:         private static Vec3 nearest(AABB body, Vec3 point) {
597:             double clampedX = point.x < body.minX ? body.minX : (point.x > body.maxX ? body.maxX : point.x);
598:             double clampedY = point.y < body.minY ? body.minY : (point.y > body.maxY ? body.maxY : point.y);
599:             double clampedZ = point.z < body.minZ ? body.minZ : (point.z > body.maxZ ? body.maxZ : point.z);
600:             return new Vec3(clampedX, clampedY, clampedZ);
601:         }
602:     }
603: }
604: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/guard/GuardPreview.java">
1: package com.bslx.XueXiMod.shu.technique.release.guard;
2: 
3: import net.minecraft.server.level.ServerLevel;
4: import net.minecraft.server.level.ServerPlayer;
5: import net.minecraft.world.entity.Entity;
6: import net.minecraft.world.entity.projectile.Projectile;
7: import net.minecraft.world.level.ClipContext;
8: import net.minecraft.world.phys.AABB;
9: import net.minecraft.world.phys.BlockHitResult;
10: import net.minecraft.world.phys.HitResult;
11: import net.minecraft.world.phys.Vec3;
12: 
13: import java.util.Comparator;
14: import java.util.List;
15: 
16: /** 箭还没打到时先把环抬起来。不改箭的碰撞，也不记水势。 */
17: final class GuardPreview {
18:     private static final double REACH = 8.0;
19:     private static final int CANDIDATE_CAP = 16;
20:     private static final double LOOKAHEAD = 4.0;
21: 
22:     private GuardPreview() {
23:     }
24: 
25:     static void look(ServerPlayer owner, GuardStates.BodyGuard guard, int now) {
26:         if (!(owner.level() instanceof ServerLevel level)) {
27:             return;
28:         }
29:         AABB area = owner.getBoundingBox().inflate(REACH);
30:         List<Projectile> found = level.getEntitiesOfClass(Projectile.class, area, Projectile::isAlive);
31:         found.sort(Comparator.comparingDouble(projectile -> projectile.distanceToSqr(owner)));
32:         int scanned = Math.min(CANDIDATE_CAP, found.size());
33:         Projectile chosen = null;
34:         double soonest = LOOKAHEAD + 1.0;
35:         Vec3 chosenDirection = null;
36:         for (int candidateIndex = 0; candidateIndex < scanned; candidateIndex++) {
37:             Projectile projectile = found.get(candidateIndex);
38:             if (projectile.onGround() || projectile.getDeltaMovement().lengthSqr() < 1.0E-4) {
39:                 continue;
40:             }
41:             Vec3 relative = projectile.position().subtract(owner.position());
42:             Vec3 velocity = projectile.getDeltaMovement();
43:             if (relative.dot(velocity) > 0.0 && relative.lengthSqr() > 1.0) {
44:                 continue;
45:             }
46:             double speedSqr = velocity.lengthSqr();
47:             double approach = speedSqr < 1.0E-6 ? 0.0 : -relative.dot(velocity) / speedSqr;
48:             if (approach < 0.0) {
49:                 approach = 0.0;
50:             }
51:             if (approach > LOOKAHEAD) {
52:                 continue;
53:             }
54:             Vec3 future = projectile.position().add(velocity.scale(approach));
55:             if (future.distanceToSqr(owner.getBoundingBox().getCenter()) > 2.25) {
56:                 continue;
57:             }
58:             if (blocked(level, projectile, owner, future)) {
59:                 continue;
60:             }
61:             if (approach < soonest) {
62:                 soonest = approach;
63:                 chosen = projectile;
64:                 chosenDirection = owner.position().subtract(projectile.position());
65:             }
66:         }
67:         if (chosen == null || chosenDirection == null) {
68:             return;
69:         }
70:         GuardStates.preview(owner, guard, chosenDirection, chosen.getId(), now);
71:     }
72: 
73:     private static boolean blocked(ServerLevel level, Projectile projectile, ServerPlayer owner, Vec3 future) {
74:         BlockHitResult hit = level.clip(new ClipContext(projectile.position(), future,
75:                 ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, projectile));
76:         if (hit.getType() == HitResult.Type.BLOCK) {
77:             return true;
78:         }
79:         AABB swept = new AABB(projectile.position(), future).inflate(0.3);
80:         List<Entity> between = level.getEntities(projectile, swept, entity -> entity != owner && entity.isAlive());
81:         return !between.isEmpty();
82:     }
83: }
84: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/network/DomainSyncPacket.java">
1: package com.bslx.XueXiMod.network;
2: 
3: import com.bslx.XueXiMod.shu.technique.release.domain.DomainBoundary;
4: import com.bslx.XueXiMod.shu.technique.release.domain.DomainInstance;
5: import com.bslx.XueXiMod.shu.technique.release.domain.DomainSightStore;
6: import com.bslx.XueXiMod.shu.technique.release.domain.DomainVisual;
7: import com.bslx.XueXiMod.shu.technique.release.relations.Relations;
8: import net.minecraft.network.FriendlyByteBuf;
9: import net.minecraft.server.level.ServerPlayer;
10: import net.minecraft.world.phys.AABB;
11: import net.minecraftforge.api.distmarker.Dist;
12: import net.minecraftforge.fml.DistExecutor;
13: import net.minecraftforge.network.NetworkEvent;
14: import net.minecraftforge.network.simple.SimpleChannel;
15: 
16: import java.util.UUID;
17: import java.util.function.Supplier;
18: 
19: /** 把固定领域盒子和这位观察者能否通过发给客户端。 */
20: public class DomainSyncPacket {
21:     private final boolean removed;
22:     private final UUID instanceId;
23:     private final int revision;
24:     private final UUID ownerId;
25:     private final double lowX;
26:     private final double lowY;
27:     private final double lowZ;
28:     private final double highX;
29:     private final double highY;
30:     private final double highZ;
31:     private final boolean viewerMayPass;
32:     private final int permitRevision;
33:     private final int visualKind;
34:     private final int boundaryKind;
35:     private final long visualOrigin;
36:     private final long visualEnd;
37:     private final int animSeed;
38: 
39:     public DomainSyncPacket(boolean removed, UUID instanceId, int revision, UUID ownerId,
40:                             double lowX, double lowY, double lowZ, double highX, double highY, double highZ,
41:                             boolean viewerMayPass, int permitRevision,
42:                             int visualKind, int boundaryKind, long visualOrigin, long visualEnd, int animSeed) {
43:         this.removed = removed;
44:         this.instanceId = instanceId;
45:         this.revision = revision;
46:         this.ownerId = ownerId;
47:         this.lowX = lowX;
48:         this.lowY = lowY;
49:         this.lowZ = lowZ;
50:         this.highX = highX;
51:         this.highY = highY;
52:         this.highZ = highZ;
53:         this.viewerMayPass = viewerMayPass;
54:         this.permitRevision = permitRevision;
55:         this.visualKind = visualKind;
56:         this.boundaryKind = boundaryKind;
57:         this.visualOrigin = visualOrigin;
58:         this.visualEnd = visualEnd;
59:         this.animSeed = animSeed;
60:     }
61: 
62:     public static void register(SimpleChannel channel, int packetId) {
63:         channel.registerMessage(packetId, DomainSyncPacket.class, DomainSyncPacket::encode, DomainSyncPacket::decode, DomainSyncPacket::handle);
64:     }
65: 
66:     public static void broadcast(DomainInstance domain, boolean removed) {
67:         if (domain == null || domain.hostLevel() == null) {
68:             return;
69:         }
70:         for (ServerPlayer watcher : domain.hostLevel().players()) {
71:             boolean pass = Relations.permits(domain.ownerId(), watcher) || Relations.ally(domain.ownerId(), watcher);
72:             ModNetwork.sendToPlayer(watcher, packetFor(domain, removed, pass));
73:         }
74:     }
75: 
76:     public static void sendSnapshot(ServerPlayer watcher) {
77:         if (watcher == null) {
78:             return;
79:         }
80:         for (DomainInstance domain : com.bslx.XueXiMod.shu.technique.release.domain.DomainService.active()) {
81:             if (domain.active() && domain.hostLevel() == watcher.level()) {
82:                 boolean pass = Relations.permits(domain.ownerId(), watcher) || Relations.ally(domain.ownerId(), watcher);
83:                 ModNetwork.sendToPlayer(watcher, packetFor(domain, false, pass));
84:             }
85:         }
86:     }
87: 
88:     public void encode(FriendlyByteBuf buffer) {
89:         buffer.writeBoolean(removed);
90:         buffer.writeUUID(instanceId);
91:         buffer.writeVarInt(revision);
92:         buffer.writeUUID(ownerId);
93:         buffer.writeDouble(lowX);
94:         buffer.writeDouble(lowY);
95:         buffer.writeDouble(lowZ);
96:         buffer.writeDouble(highX);
97:         buffer.writeDouble(highY);
98:         buffer.writeDouble(highZ);
99:         buffer.writeBoolean(viewerMayPass);
100:         buffer.writeVarInt(permitRevision);
101:         buffer.writeVarInt(visualKind);
102:         buffer.writeVarInt(boundaryKind);
103:         buffer.writeLong(visualOrigin);
104:         buffer.writeLong(visualEnd);
105:         buffer.writeVarInt(animSeed);
106:     }
107: 
108:     public static DomainSyncPacket decode(FriendlyByteBuf buffer) {
109:         return new DomainSyncPacket(buffer.readBoolean(), buffer.readUUID(), buffer.readVarInt(), buffer.readUUID(),
110:                 buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
111:                 buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
112:                 buffer.readBoolean(), buffer.readVarInt(),
113:                 buffer.readVarInt(), buffer.readVarInt(), buffer.readLong(), buffer.readLong(), buffer.readVarInt());
114:     }
115: 
116:     private static DomainSyncPacket packetFor(DomainInstance domain, boolean removed, boolean pass) {
117:         AABB bounds = domain.geometry().bounds();
118:         return new DomainSyncPacket(removed, domain.instanceId(), domain.revision(), domain.ownerId(),
119:                 bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ,
120:                 pass, domain.revision(), domain.visual().ordinal(), domain.boundary().ordinal(),
121:                 domain.visualOrigin(), domain.visualEnd(), domain.animSeed());
122:     }
123: 
124:     public static void handle(DomainSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
125:         contextSupplier.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DomainSightStore.accept(packet)));
126:         contextSupplier.get().setPacketHandled(true);
127:     }
128: 
129:     public boolean removed() {
130:         return removed;
131:     }
132: 
133:     public UUID instanceId() {
134:         return instanceId;
135:     }
136: 
137:     public int revision() {
138:         return revision;
139:     }
140: 
141:     public UUID ownerId() {
142:         return ownerId;
143:     }
144: 
145:     public double lowX() {
146:         return lowX;
147:     }
148: 
149:     public double lowY() {
150:         return lowY;
151:     }
152: 
153:     public double lowZ() {
154:         return lowZ;
155:     }
156: 
157:     public double highX() {
158:         return highX;
159:     }
160: 
161:     public double highY() {
162:         return highY;
163:     }
164: 
165:     public double highZ() {
166:         return highZ;
167:     }
168: 
169:     public boolean viewerMayPass() {
170:         return viewerMayPass;
171:     }
172: 
173:     public int permitRevision() {
174:         return permitRevision;
175:     }
176: 
177:     public DomainVisual visual() {
178:         DomainVisual[] kinds = DomainVisual.values();
179:         if (visualKind < 0 || visualKind >= kinds.length) {
180:             return DomainVisual.VOLUME;
181:         }
182:         return kinds[visualKind];
183:     }
184: 
185:     public DomainBoundary boundary() {
186:         DomainBoundary[] kinds = DomainBoundary.values();
187:         if (boundaryKind < 0 || boundaryKind >= kinds.length) {
188:             return DomainBoundary.SEAL;
189:         }
190:         return kinds[boundaryKind];
191:     }
192: 
193:     public long visualOrigin() {
194:         return visualOrigin;
195:     }
196: 
197:     public long visualEnd() {
198:         return visualEnd;
199:     }
200: 
201:     public int animSeed() {
202:         return animSeed;
203:     }
204: }
205: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/network/GuardSyncPacket.java">
1: package com.bslx.XueXiMod.network;
2: 
3: import com.bslx.XueXiMod.client.GuardClientLifecycle;
4: import com.bslx.XueXiMod.shu.technique.release.guard.GuardStates;
5: import net.minecraft.network.FriendlyByteBuf;
6: import net.minecraft.server.level.ServerPlayer;
7: import net.minecraftforge.api.distmarker.Dist;
8: import net.minecraftforge.fml.DistExecutor;
9: import net.minecraftforge.network.NetworkEvent;
10: import net.minecraftforge.network.simple.SimpleChannel;
11: 
12: import java.util.ArrayList;
13: import java.util.List;
14: import java.util.UUID;
15: import java.util.function.Supplier;
16: 
17: /** 服务端把护体快照和姿态发给能看见这位玩家的人。坐标不逐拍发送。动画相位用年龄，不用墙钟。 */
18: public class GuardSyncPacket {
19:     public static final int FULL = 0;
20:     public static final int POSE = 1;
21:     public static final int REMOVE = 2;
22: 
23:     private final int kind;
24:     private final UUID ownerId;
25:     private final int revision;
26:     private final int sequence;
27:     private final int expiresAt;
28:     private final int upkeepAt;
29:     private final int animationStart;
30:     private final int animationAgeTicks;
31:     private final float baseTilt;
32:     private final float radius;
33:     private final float band;
34:     private final float bodyHeight;
35:     private final float fromX;
36:     private final float fromY;
37:     private final float fromZ;
38:     private final float towardX;
39:     private final float towardY;
40:     private final float towardZ;
41:     private final int poseTicks;
42: 
43:     public GuardSyncPacket(int kind, UUID ownerId, int revision, int sequence,
44:                            int expiresAt, int upkeepAt, int animationStart, int animationAgeTicks,
45:                            float baseTilt, float radius, float band, float bodyHeight,
46:                            float fromX, float fromY, float fromZ,
47:                            float towardX, float towardY, float towardZ, int poseTicks) {
48:         this.kind = kind;
49:         this.ownerId = ownerId;
50:         this.revision = revision;
51:         this.sequence = sequence;
52:         this.expiresAt = expiresAt;
53:         this.upkeepAt = upkeepAt;
54:         this.animationStart = animationStart;
55:         this.animationAgeTicks = animationAgeTicks;
56:         this.baseTilt = baseTilt;
57:         this.radius = radius;
58:         this.band = band;
59:         this.bodyHeight = bodyHeight;
60:         this.fromX = fromX;
61:         this.fromY = fromY;
62:         this.fromZ = fromZ;
63:         this.towardX = towardX;
64:         this.towardY = towardY;
65:         this.towardZ = towardZ;
66:         this.poseTicks = poseTicks;
67:     }
68: 
69:     public void encode(FriendlyByteBuf buffer) {
70:         buffer.writeByte(kind);
71:         buffer.writeUUID(ownerId);
72:         buffer.writeVarInt(revision);
73:         buffer.writeVarInt(sequence);
74:         buffer.writeVarInt(expiresAt);
75:         buffer.writeVarInt(upkeepAt);
76:         buffer.writeVarInt(animationStart);
77:         buffer.writeVarInt(animationAgeTicks);
78:         buffer.writeFloat(baseTilt);
79:         buffer.writeFloat(radius);
80:         buffer.writeFloat(band);
81:         buffer.writeFloat(bodyHeight);
82:         buffer.writeFloat(fromX);
83:         buffer.writeFloat(fromY);
84:         buffer.writeFloat(fromZ);
85:         buffer.writeFloat(towardX);
86:         buffer.writeFloat(towardY);
87:         buffer.writeFloat(towardZ);
88:         buffer.writeVarInt(poseTicks);
89:     }
90: 
91:     public static GuardSyncPacket decode(FriendlyByteBuf buffer) {
92:         return new GuardSyncPacket(buffer.readByte(), buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt(),
93:                 buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
94:                 buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
95:                 buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
96:                 buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
97:                 buffer.readVarInt());
98:     }
99: 
100:     boolean matches(GuardSyncPacket other) {
101:         return other != null
102:                 && this.kind == other.kind
103:                 && this.ownerId.equals(other.ownerId)
104:                 && this.revision == other.revision
105:                 && this.sequence == other.sequence
106:                 && this.expiresAt == other.expiresAt
107:                 && this.upkeepAt == other.upkeepAt
108:                 && this.animationStart == other.animationStart
109:                 && this.animationAgeTicks == other.animationAgeTicks
110:                 && this.baseTilt == other.baseTilt
111:                 && this.radius == other.radius
112:                 && this.band == other.band
113:                 && this.bodyHeight == other.bodyHeight
114:                 && this.fromX == other.fromX
115:                 && this.fromY == other.fromY
116:                 && this.fromZ == other.fromZ
117:                 && this.towardX == other.towardX
118:                 && this.towardY == other.towardY
119:                 && this.towardZ == other.towardZ
120:                 && this.poseTicks == other.poseTicks;
121:     }
122: 
123:     public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
124:         GuardSyncPacket packet = this;
125:         contextSupplier.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientRings.accept(packet)));
126:         contextSupplier.get().setPacketHandled(true);
127:     }
128: 
129:     public static void register(SimpleChannel channel, int messageId) {
130:         channel.registerMessage(messageId, GuardSyncPacket.class, GuardSyncPacket::encode, GuardSyncPacket::decode, GuardSyncPacket::handle);
131:     }
132: 
133:     public static void sendAround(ServerPlayer owner, GuardSyncPacket packet) {
134:         if (owner == null || owner.getServer() == null) {
135:             return;
136:         }
137:         for (ServerPlayer watcher : owner.serverLevel().players()) {
138:             if (watcher == owner || watcher.distanceTo(owner) <= 128f) {
139:                 ModNetwork.sendToPlayer(watcher, packet);
140:             }
141:         }
142:     }
143: 
144:     public static void sendSnapshot(ServerPlayer watcher, ServerPlayer owner) {
145:         GuardSyncPacket packet = GuardStates.snapshot(owner.getUUID());
146:         if (packet != null && watcher != null) {
147:             ModNetwork.sendToPlayer(watcher, packet);
148:         }
149:     }
150: 
151:     public static final class ClientRings {
152:         private static final ArrayList<View> VIEWS = new ArrayList<>();
153: 
154:         private ClientRings() {
155:         }
156: 
157:         public static void accept(GuardSyncPacket packet) {
158:             accept(packet, GuardClientLifecycle.clientGameTick());
159:         }
160: 
161:         static void accept(GuardSyncPacket packet, long clientGameTick) {
162:             if (packet.kind == REMOVE) {
163:                 VIEWS.removeIf(view -> removes(view, packet));
164:                 return;
165:             }
166:             View view = find(packet.ownerId);
167:             if (packet.kind == FULL) {
168:                 if (view != null && packet.animationStart < view.animationStart) {
169:                     return;
170:                 }
171:                 if (view != null && packet.animationStart == view.animationStart && packetLags(view, packet)) {
172:                     return;
173:                 }
174:                 boolean freshActivation = view == null || packet.animationStart != view.animationStart;
175:                 boolean sameRevision = !freshActivation && packet.revision == view.revision;
176:                 boolean sameSequence = sameRevision && packet.sequence == view.sequence;
177:                 if (view == null) {
178:                     view = new View();
179:                     view.ownerId = packet.ownerId;
180:                     VIEWS.add(view);
181:                 }
182:                 view.revision = packet.revision;
183:                 view.sequence = packet.sequence;
184:                 view.baseTilt = packet.baseTilt;
185:                 view.radius = packet.radius;
186:                 view.band = packet.band;
187:                 view.bodyHeight = packet.bodyHeight;
188:                 view.animationStart = packet.animationStart;
189:                 anchorAge(view, packet.animationAgeTicks, clientGameTick, freshActivation);
190:                 if (freshActivation || !sameSequence) {
191:                     applyPose(view, packet);
192:                 }
193:                 return;
194:             }
195:             if (packet.kind != POSE || view == null || packet.animationStart != view.animationStart
196:                     || packet.revision != view.revision || packetLags(view, packet)) {
197:                 return;
198:             }
199:             boolean freshActivation = packet.animationStart != view.animationStart;
200:             boolean sameRevision = packet.revision == view.revision;
201:             boolean sameSequence = sameRevision && packet.sequence == view.sequence;
202:             view.sequence = packet.sequence;
203:             anchorAge(view, packet.animationAgeTicks, clientGameTick, freshActivation);
204:             if (freshActivation || !sameSequence) {
205:                 applyPose(view, packet);
206:             }
207:         }
208: 
209:         public static void clear() {
210:             VIEWS.clear();
211:         }
212: 
213:         public static List<View> all() {
214:             return VIEWS;
215:         }
216: 
217:         private static boolean removes(View view, GuardSyncPacket packet) {
218:             if (view.ownerId == null || !view.ownerId.equals(packet.ownerId)) {
219:                 return false;
220:             }
221:             if (view.animationStart != packet.animationStart) {
222:                 return false;
223:             }
224:             return !packetLags(view, packet);
225:         }
226: 
227:         private static boolean packetLags(View view, GuardSyncPacket packet) {
228:             if (packet.revision < view.revision) {
229:                 return true;
230:             }
231:             if (packet.revision > view.revision) {
232:                 return false;
233:             }
234:             return packet.sequence < view.sequence;
235:         }
236: 
237:         private static void anchorAge(View view, int packetAge, long clientGameTick, boolean freshActivation) {
238:             int safeAge = Math.max(0, packetAge);
239:             if (freshActivation) {
240:                 view.anchoredAgeTicks = safeAge;
241:                 view.anchorClientTick = clientGameTick;
242:                 return;
243:             }
244:             double localAge = view.animationAge(clientGameTick, 0f);
245:             if (safeAge > localAge) {
246:                 view.anchoredAgeTicks = safeAge;
247:                 view.anchorClientTick = clientGameTick;
248:             }
249:         }
250: 
251:         private static View find(UUID ownerId) {
252:             for (View view : VIEWS) {
253:                 if (view.ownerId.equals(ownerId)) {
254:                     return view;
255:                 }
256:             }
257:             return null;
258:         }
259: 
260:         private static void applyPose(View view, GuardSyncPacket packet) {
261:             view.fromX = packet.fromX;
262:             view.fromY = packet.fromY;
263:             view.fromZ = packet.fromZ;
264:             view.towardX = packet.towardX;
265:             view.towardY = packet.towardY;
266:             view.towardZ = packet.towardZ;
267:             view.poseStartMillis = System.currentTimeMillis();
268:             view.poseTicks = Math.max(1, packet.poseTicks);
269:         }
270: 
271:         public static final class View {
272:             public UUID ownerId;
273:             public int revision;
274:             public int sequence;
275:             public float baseTilt;
276:             public float radius = 0.9f;
277:             public float band = 0.12f;
278:             public float bodyHeight = 0.55f;
279:             public int animationStart;
280:             public float fromX;
281:             public float fromY = 1f;
282:             public float fromZ;
283:             public float towardX;
284:             public float towardY = 1f;
285:             public float towardZ;
286:             public long poseStartMillis;
287:             public int poseTicks = 1;
288:             private int anchoredAgeTicks;
289:             private long anchorClientTick;
290: 
291:             public double animationAge(long clientGameTick, float partialTick) {
292:                 long elapsedTicks = clientGameTick - this.anchorClientTick;
293:                 if (elapsedTicks < 0L) {
294:                     elapsedTicks = 0L;
295:                 }
296:                 float partial = partialTick;
297:                 if (!Float.isFinite(partial) || partial < 0f) {
298:                     partial = 0f;
299:                 }
300:                 double age = this.anchoredAgeTicks + (double) elapsedTicks + partial;
301:                 if (age < 0.0) {
302:                     return 0.0;
303:                 }
304:                 return age;
305:             }
306:         }
307:     }
308: }
309: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/client/DomainClientState.java">
1: package com.bslx.XueXiMod.client;
2: 
3: import com.bslx.XueXiMod.shu.technique.release.domain.DomainBound;
4: import com.bslx.XueXiMod.shu.technique.release.domain.DomainClientBridge;
5: import com.bslx.XueXiMod.shu.technique.release.domain.DomainSightStore;
6: import com.bslx.XueXiMod.shu.technique.release.relations.Relations;
7: import net.minecraft.client.Minecraft;
8: import net.minecraft.world.entity.Entity;
9: 
10: import java.util.ArrayList;
11: import java.util.List;
12: 
13: /** 只给本地玩家做移动预测。其他实体等服务端裁决。 */
14: public final class DomainClientState {
15:     private DomainClientState() {
16:     }
17: 
18:     public static void bind() {
19:         DomainClientBridge.bind(DomainClientState::targets);
20:     }
21: 
22:     private static List<DomainBound> targets(Entity mover) {
23:         ArrayList<DomainBound> found = new ArrayList<>();
24:         Minecraft minecraft = Minecraft.getInstance();
25:         if (minecraft.player == null || mover == null) {
26:             return found;
27:         }
28:         Entity viewer = minecraft.player;
29:         if (mover != viewer && mover.getRootVehicle() != viewer.getRootVehicle()) {
30:             return found;
31:         }
32:         for (DomainSightStore.Sight sight : DomainSightStore.all()) {
33:             found.add(new ViewerSight(sight, viewer));
34:         }
35:         return found;
36:     }
37: 
38:     private static final class ViewerSight implements DomainBound {
39:         private final DomainSightStore.Sight sight;
40:         private final Entity viewer;
41: 
42:         private ViewerSight(DomainSightStore.Sight sight, Entity viewer) {
43:             this.sight = sight;
44:             this.viewer = viewer;
45:         }
46: 
47:         @Override
48:         public java.util.UUID ownerId() {
49:             return sight.ownerId();
50:         }
51: 
52:         @Override
53:         public com.bslx.XueXiMod.shu.technique.release.domain.DomainGeometry geometry() {
54:             return sight.geometry();
55:         }
56: 
57:         @Override
58:         public boolean active() {
59:             Minecraft minecraft = Minecraft.getInstance();
60:             return minecraft.level != null && minecraft.level.getGameTime() < sight.visualEnd();
61:         }
62: 
63:         @Override
64:         public com.bslx.XueXiMod.shu.technique.release.domain.DomainBoundary boundary() {
65:             return sight.boundary();
66:         }
67: 
68:         @Override
69:         public boolean passes(Entity member) {
70:             if (sight.viewerMayPass() && member != null && member.getUUID().equals(viewer.getUUID())) {
71:                 return true;
72:             }
73:             return Relations.permits(sight.ownerId(), member) || Relations.ally(sight.ownerId(), member);
74:         }
75:     }
76: }
77: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/client/GuardRingRenderer.java">
1: package com.bslx.XueXiMod.client;
2: 
3: import com.bslx.XueXiMod.XueXiMod;
4: import com.bslx.XueXiMod.network.GuardSyncPacket;
5: import com.mojang.blaze3d.vertex.PoseStack;
6: import com.mojang.blaze3d.vertex.VertexConsumer;
7: import net.minecraft.client.Minecraft;
8: import net.minecraft.client.renderer.MultiBufferSource;
9: import net.minecraft.client.renderer.RenderType;
10: import net.minecraft.util.Mth;
11: import net.minecraft.world.entity.player.Player;
12: import net.minecraft.world.phys.Vec3;
13: import net.minecraftforge.api.distmarker.Dist;
14: import net.minecraftforge.client.event.RenderLevelStageEvent;
15: import net.minecraftforge.eventbus.api.SubscribeEvent;
16: import net.minecraftforge.fml.common.Mod;
17: import org.joml.Matrix4f;
18: 
19: /** 水环护体的 v8 水膜。环心和姿态仍跟同步视图，网格三层共用一次变换。 */
20: @Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, value = Dist.CLIENT)
21: public final class GuardRingRenderer {
22:     private GuardRingRenderer() {
23:     }
24: 
25:     @SubscribeEvent
26:     public static void onRender(RenderLevelStageEvent event) {
27:         if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
28:             return;
29:         }
30:         Minecraft minecraft = Minecraft.getInstance();
31:         if (minecraft.level == null || minecraft.player == null) {
32:             return;
33:         }
34:         if (GuardSyncPacket.ClientRings.all().isEmpty()) {
35:             return;
36:         }
37:         WaterRingV8Mesh mesh = WaterRingV8Mesh.current();
38:         if (mesh == null) {
39:             return;
40:         }
41:         float partialTick = event.getPartialTick();
42:         long gameTime = minecraft.level.getGameTime();
43:         Vec3 camera = event.getCamera().getPosition();
44:         boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
45:         PoseStack pose = event.getPoseStack();
46:         MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
47:         RenderType meshType = WaterRingV8Material.mesh();
48:         VertexConsumer consumer = buffer.getBuffer(meshType);
49:         double[] placedPoint = new double[3];
50:         double[] offsetX = new double[4];
51:         double[] offsetY = new double[4];
52:         double[] offsetZ = new double[4];
53:         float[] atlasUv = new float[2];
54:         for (GuardSyncPacket.ClientRings.View view : GuardSyncPacket.ClientRings.all()) {
55:             Player wearer = minecraft.level.getPlayerByUUID(view.ownerId);
56:             if (wearer == null || !Double.isFinite(view.radius) || view.radius <= 0.0f) {
57:                 continue;
58:             }
59:             double centerX = Mth.lerp(partialTick, wearer.xo, wearer.getX());
60:             double centerY = Mth.lerp(partialTick, wearer.yo, wearer.getY()) + wearer.getBbHeight() * view.bodyHeight;
61:             double centerZ = Mth.lerp(partialTick, wearer.zo, wearer.getZ());
62:             if (!Double.isFinite(centerX) || !Double.isFinite(centerY) || !Double.isFinite(centerZ)) {
63:                 continue;
64:             }
65:             Vec3 normal = blendedNormal(view);
66:             double age = view.animationAge(gameTime, partialTick);
67:             WaterRingV8Math.PlacedFrame placed = WaterRingV8Math.PlacedFrame.prepare(
68:                     mesh.tiltDegrees,
69:                     WaterRingV8Math.spinRadians(age),
70:                     view.radius / WaterRingV8Math.MODEL_RADIUS,
71:                     normal.x, normal.y, normal.z);
72:             boolean fadeNearEye = firstPerson && wearer == minecraft.player;
73:             pose.pushPose();
74:             pose.translate(
75:                     WaterRingV8Math.relativeToCamera(centerX, camera.x),
76:                     WaterRingV8Math.relativeToCamera(centerY, camera.y),
77:                     WaterRingV8Math.relativeToCamera(centerZ, camera.z));
78:             drawRing(consumer, pose.last().pose(), mesh, placed, WaterRingV8Math.frameIndex(age),
79:                     centerX, centerY, centerZ, camera, fadeNearEye,
80:                     placedPoint, offsetX, offsetY, offsetZ, atlasUv);
81:             pose.popPose();
82:         }
83:         buffer.endBatch(meshType);
84:     }
85: 
86:     private static Vec3 blendedNormal(GuardSyncPacket.ClientRings.View view) {
87:         float span = Math.max(1, view.poseTicks) * 50f;
88:         float progress = Mth.clamp((System.currentTimeMillis() - view.poseStartMillis) / span, 0f, 1f);
89:         double normalX = Mth.lerp(progress, view.fromX, view.towardX);
90:         double normalY = Mth.lerp(progress, view.fromY, view.towardY);
91:         double normalZ = Mth.lerp(progress, view.fromZ, view.towardZ);
92:         Vec3 normal = new Vec3(normalX, normalY, normalZ);
93:         if (normal.lengthSqr() < 1.0E-6) {
94:             return new Vec3(0.0, 1.0, 0.0);
95:         }
96:         return normal.normalize();
97:     }
98: 
99:     private static void drawRing(VertexConsumer consumer, Matrix4f matrix, WaterRingV8Mesh mesh,
100:                                   WaterRingV8Math.PlacedFrame placed, int frame,
101:                                   double centerX, double centerY, double centerZ, Vec3 camera, boolean fadeNearEye,
102:                                   double[] placedPoint, double[] offsetX, double[] offsetY, double[] offsetZ,
103:                                   float[] atlasUv) {
104:         for (WaterRingV8Mesh.Layer layer : mesh.layers) {
105:             for (int quadIndex = 0; quadIndex < layer.quadCount; quadIndex++) {
106:                 for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
107:                     int corner = quadIndex * 4 + vertexIndex;
108:                     placed.apply(layer.localX[corner], layer.localY[corner], layer.localZ[corner], placedPoint);
109:                     offsetX[vertexIndex] = placedPoint[0];
110:                     offsetY[vertexIndex] = placedPoint[1];
111:                     offsetZ[vertexIndex] = placedPoint[2];
112:                 }
113:                 int alpha = faceAlpha(fadeNearEye, centerX, centerY, centerZ, camera, offsetX, offsetY, offsetZ);
114:                 if (alpha <= 0) {
115:                     continue;
116:                 }
117:                 for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
118:                     int corner = quadIndex * 4 + vertexIndex;
119:                     WaterRingV8Math.mapUv(WaterRingV8Math.ATLAS_WIDTH, WaterRingV8Math.ATLAS_HEIGHT,
120:                             WaterRingV8Math.LOGICAL_WIDTH, layer.atlasOriginX, layer.frameHeight, frame,
121:                             layer.sourceU[corner], layer.sourceV[corner], atlasUv);
122:                     consumer.vertex(matrix, (float) offsetX[vertexIndex], (float) offsetY[vertexIndex], (float) offsetZ[vertexIndex])
123:                             .color(255, 255, 255, alpha)
124:                             .uv(atlasUv[0], atlasUv[1])
125:                             .endVertex();
126:                 }
127:             }
128:         }
129:     }
130: 
131:     private static int faceAlpha(boolean fadeNearEye, double centerX, double centerY, double centerZ, Vec3 camera,
132:                                   double[] offsetX, double[] offsetY, double[] offsetZ) {
133:         double midX = centerX + (offsetX[0] + offsetX[1] + offsetX[2] + offsetX[3]) * 0.25;
134:         double midY = centerY + (offsetY[0] + offsetY[1] + offsetY[2] + offsetY[3]) * 0.25;
135:         double midZ = centerZ + (offsetZ[0] + offsetZ[1] + offsetZ[2] + offsetZ[3]) * 0.25;
136:         double deltaX = midX - camera.x;
137:         double deltaY = midY - camera.y;
138:         double deltaZ = midZ - camera.z;
139:         double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
140:         return WaterRingV8Math.nearEyeAlpha(fadeNearEye, distance);
141:     }
142: }
143: 
</source_file>

<partial_source path="src/main/java/com/bslx/XueXiMod/client/DomainMistRainRenderer.java" selected="matching params and 10 adjacent lines; omissions explicit">

</partial_source>

<partial_source path="src/main/java/com/bslx/XueXiMod/client/DomainMistRingRenderer.java" selected="matching params and 10 adjacent lines; omissions explicit">

</partial_source>

<partial_source path="src/main/java/com/bslx/XueXiMod/client/DomainWorldRenderer.java" selected="matching params and 10 adjacent lines; omissions explicit">

</partial_source>
