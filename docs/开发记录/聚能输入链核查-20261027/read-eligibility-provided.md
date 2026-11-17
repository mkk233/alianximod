你是主控调度的deepseek-flash专项只读读取代理。唯一GPT-6-astra已先给出调查切块，报告齐后再统一裁决。下面是已提供的带行号源码，只分析材料，禁止工具、禁止改文件、禁止编译、禁止另派代理。用户要求聚能只作用元素术式，剑术即使带元素也不允许；能量源输入量提高应带动范围/特效大小/持续时间，未聚能基线不变。不要按water_ring或water_mist id建议打补丁。区分确定事实/猜测/缺失材料，精确路径行号、调用链、字段公式、最早丢失倍率点、建议后续核实文件。不要输出长源码，不做最终设计，中文报告1000-1800字。根README不存在，不补造依据。
专项C：资格与其他消费者。TechniqueType/元素属性/ChainRole/ReleaseKind用途，聚能装配资格和元素剑术区别。飞弹/水幕怎样按输入量放大判定/视觉/持续时间，是否仅实体消费者处理了放大而领域护体零伤害载体漏掉。查注册定义的公共接口，列出适用载体与剑术排除依据，勿自创分类。

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

<source_file path="src/main/java/com/bslx/XueXiMod/shu/TechniqueType.java">
1: package com.bslx.XueXiMod.shu;
2: 
3: /**
4:  * 术类型枚举
5:  * SWORD - 剑术，只能用于剑技
6:  * MOVEMENT - 动作，通用
7:  * MAGIC - 法术，只能用于法技
8:  * UNIVERSAL - 通用，可用于剑技和法技
9:  */
10: public enum TechniqueType {
11:     SWORD,
12:     MOVEMENT,
13:     MAGIC,
14:     UNIVERSAL;
15: 
16:     /**
17:      * 判断该类型是否可以用于剑技组合
18:      */
19:     public boolean canUseForSwordSkill() {
20:         return this == SWORD || this == UNIVERSAL;
21:     }
22: 
23:     /**
24:      * 判断该类型是否可以用于法技组合
25:      */
26:     public boolean canUseForMagicSkill() {
27:         return this == MAGIC || this == UNIVERSAL;
28:     }
29: }
30: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/Technique.java">
1: package com.bslx.XueXiMod.shu;
2: 
3: import com.bslx.XueXiMod.shu.technique.EnchantmentDefinition;
4: import com.bslx.XueXiMod.shu.technique.EnchantmentRegistry;
5: import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
6: import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
7: import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
8: import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalEffects;
9: 
10: /**
11:  * 术 - 最小的单一功能单元
12:  *
13:  * complexity: 复杂度。剑术/法术总等级 >= complexity 才解锁；0=默认解锁。
14:  * 动作/通用始终解锁。
15:  */
16: public class Technique {
17:     private final String techniqueId;
18:     private final String name;
19:     private final TechniqueType type;
20:     private final float baseDamage;
21:     private final int baseCooldown;
22:     private final int levelCap;
23:     private final String description;
24:     private final int complexity;
25:     private final ShuAttribute attribute;
26:     private final float projectileSpeed;
27:     private final boolean enchantment;
28: 
29:     public Technique(String techniqueId, String name, TechniqueType type,
30:                      float baseDamage, int baseCooldown, int levelCap, String description) {
31:         this(techniqueId, name, type, baseDamage, baseCooldown, levelCap, description, 0, ShuAttribute.NONE, 1.0f);
32:     }
33: 
34:     public Technique(String techniqueId, String name, TechniqueType type,
35:                      float baseDamage, int baseCooldown, int levelCap, String description,
36:                      int complexity) {
37:         this(techniqueId, name, type, baseDamage, baseCooldown, levelCap, description, complexity, ShuAttribute.NONE, 1.0f);
38:     }
39: 
40:     public Technique(String techniqueId, String name, TechniqueType type,
41:                      float baseDamage, int baseCooldown, int levelCap, String description,
42:                      int complexity, ShuAttribute attribute, float projectileSpeed) {
43:         this(techniqueId, name, type, baseDamage, baseCooldown, levelCap, description, complexity, attribute, projectileSpeed, false);
44:     }
45: 
46:     public Technique(String techniqueId, String name, TechniqueType type,
47:                      float baseDamage, int baseCooldown, int levelCap, String description,
48:                      int complexity, ShuAttribute attribute, float projectileSpeed, boolean enchantment) {
49:         this.techniqueId = techniqueId;
50:         this.name = name;
51:         this.type = type;
52:         this.baseDamage = baseDamage;
53:         this.baseCooldown = baseCooldown;
54:         this.levelCap = levelCap;
55:         this.description = description;
56:         this.complexity = complexity;
57:         this.attribute = attribute;
58:         this.projectileSpeed = projectileSpeed;
59:         this.enchantment = enchantment;
60:     }
61: 
62:     public String getId() { return techniqueId; }
63:     public String getName() { return name; }
64:     public TechniqueType getType() { return type; }
65:     public float getBaseDamage() { return baseDamage; }
66:     public int getBaseCooldown() { return baseCooldown; }
67:     public int getMaxLevel() { return levelCap; }
68:     public String getDescription() { return description; }
69:     public int getComplexity() { return complexity; }
70: 
71:     @Deprecated
72:     public int getUnlockLevel() { return complexity; }
73:     public ShuAttribute getAttribute() { return attribute; }
74:     public float getProjectileSpeed() { return projectileSpeed; }
75:     public boolean isEnchantment() { return enchantment; }
76:     public boolean canReceiveEnchantment() {
77:         if (baseDamage > 0f) {
78:             return true;
79:         }
80:         ReleaseDefinition release = ReleaseRegistry.get(techniqueId);
81:         return release != null && (release.domain() != null || release.barrier() != null
82:                 || release.fieldRadius() > 0f || release.guard() != null
83:                 || ConditionalEffects.supportsContact(release));
84:     }
85: 
86:     public int assemblyCost() {
87:         if (isEnchantment()) return 2;
88:         if (getType() == TechniqueType.MOVEMENT) return 0;
89:         if (getBaseDamage() > 0f) return 1;
90:         return 0;
91:     }
92: 
93:     public int manaCost() {
94:         if (isEnchantment()) {
95:             EnchantmentDefinition definition = EnchantmentRegistry.get(techniqueId);
96:             if (definition == null) return 1;
97:             ElementReaction reaction = ElementReaction.of(ShuAttribute.NONE, ShuAttribute.NONE);
98:             return Math.max(1, definition.magicDamage(reaction));
99:         }
100:         ReleaseDefinition release = ReleaseRegistry.get(techniqueId);
101:         if (release != null && release.conditionalEffect() != null && release.explicitManaCost() >= 0) {
102:             return release.explicitManaCost();
103:         }
104:         if (release != null && release.domain() != null) {
105:             return Math.max(0, release.domain().initialCost());
106:         }
107:         if (release != null && release.kind() == ReleaseKind.RULE) return 0;
108:         if (release != null && release.explicitManaCost() >= 0) {
109:             return release.explicitManaCost();
110:         }
111:         return Math.max(0, (int) Math.ceil(baseDamage * 2.0));
112:     }
113: 
114:     @Override
115:     public boolean equals(Object candidate) {
116:         if (this == candidate) return true;
117:         if (!(candidate instanceof Technique other)) return false;
118:         return techniqueId.equals(other.techniqueId);
119:     }
120: 
121:     @Override
122:     public int hashCode() {
123:         return techniqueId.hashCode();
124:     }
125: }
126: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/TechniqueManager.java">
1: package com.bslx.XueXiMod.shu;
2: 
3: import java.util.*;
4: 
5: /**
6:  * 术管理器 - 静态注册表
7:  * 存存所有已注册的术，提供查询方法
8:  */
9: public class TechniqueManager {
10:     private static final Map<String, Technique> TECHNIQUES = new LinkedHashMap<>();
11: 
12:     public static void register(Technique technique) {
13:         TECHNIQUES.put(technique.getId(), technique);
14:     }
15: 
16:     public static Technique getById(String techniqueId) {
17:         if (techniqueId == null) {
18:             return null;
19:         }
20:         return TECHNIQUES.get(TechniqueIds.canonicalId(techniqueId));
21:     }
22: 
23:     public static Collection<Technique> getAll() {
24:         return Collections.unmodifiableCollection(TECHNIQUES.values());
25:     }
26: 
27:     public static List<Technique> getByType(TechniqueType type) {
28:         List<Technique> result = new ArrayList<>();
29:         for (Technique technique : TECHNIQUES.values()) {
30:             if (technique.getType() == type) result.add(technique);
31:         }
32:         return result;
33:     }
34: 
35:     /**
36:      * 获取可用于剑技组合的术（剑术 + 通用）
37:      */
38:     public static List<Technique> getSwordCompatible() {
39:         List<Technique> result = new ArrayList<>();
40:         for (Technique technique : TECHNIQUES.values()) {
41:             if (technique.getType().canUseForSwordSkill()) result.add(technique);
42:         }
43:         return result;
44:     }
45: 
46:     /**
47:      * 获取可用于法技组合的术（法术 + 通用）
48:      */
49:     public static List<Technique> getMagicCompatible() {
50:         List<Technique> result = new ArrayList<>();
51:         for (Technique technique : TECHNIQUES.values()) {
52:             if (technique.getType().canUseForMagicSkill()) result.add(technique);
53:         }
54:         return result;
55:     }
56: }
57: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/ModTechniques.java">
1: package com.bslx.XueXiMod.shu;
2: 
3: import com.bslx.XueXiMod.shu.technique.EnchantmentDefinitions;
4: import com.bslx.XueXiMod.shu.technique.TechniqueEffectRegistry;
5: import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinitions;
6: import com.bslx.XueXiMod.shu.technique.release.WeepingSettings;
7: import com.bslx.XueXiMod.shu.technique.effects.CastAnchorEffect;
8: import com.bslx.XueXiMod.shu.technique.effects.FireBurstEffect;
9: import com.bslx.XueXiMod.shu.technique.effects.FireOrbEffect;
10: import com.bslx.XueXiMod.shu.technique.effects.HeatBeamEffect;
11: import com.bslx.XueXiMod.shu.technique.effects.Scatter3dEffect;
12: import com.bslx.XueXiMod.shu.technique.effects.SpaceCutEffect;
13: import com.bslx.XueXiMod.shu.technique.effects.TripleCastEffect;
14: import com.bslx.XueXiMod.shu.technique.effects.MagicArcaneMissileEffect;
15: import com.bslx.XueXiMod.shu.technique.effects.MagicCreateWaterEffect;
16: import com.bslx.XueXiMod.shu.technique.effects.MagicIgniteEffect;
17: import com.bslx.XueXiMod.shu.technique.effects.MovementDashEffect;
18: import com.bslx.XueXiMod.shu.technique.effects.MovementLeapEffect;
19: import com.bslx.XueXiMod.shu.technique.effects.SwordHorizontalSlashEffect;
20: import com.bslx.XueXiMod.shu.technique.effects.SwordThrustEffect;
21: import com.bslx.XueXiMod.shu.technique.effects.SwordVerticalSlashEffect;
22: import com.bslx.XueXiMod.shu.technique.effects.TestSustainedEffect;
23: import com.bslx.XueXiMod.shu.technique.effects.MetalNeedleEffect;
24: import com.bslx.XueXiMod.shu.technique.effects.PhantomSwordEffect;
25: import com.bslx.XueXiMod.shu.technique.effects.ParasiteSeedEffect;
26: import com.bslx.XueXiMod.shu.technique.effects.SwordComeEffect;
27: import com.bslx.XueXiMod.shu.technique.effects.SwordReturnAllEffect;
28: import com.bslx.XueXiMod.shu.technique.effects.StoneHammerEffect;
29: import com.bslx.XueXiMod.shu.technique.effects.TideSeverEffect;
30: import com.bslx.XueXiMod.shu.technique.effects.UniversalLockTargetEffect;
31: import com.bslx.XueXiMod.shu.technique.effects.WeepingCurtainEffect;
32: import com.bslx.XueXiMod.shu.technique.effects.WaterYieldingEffect;
33: import com.bslx.XueXiMod.shu.technique.release.WaterCurveFunctions;
34: 
35: /**
36:  * 内置术注册
37:  * 注册所有预设的术到 TechniqueManager
38:  */
39: public class ModTechniques {
40: 
41:     // === 剑术 ===
42:     public static final Technique SWORD_THRUST = new Technique(
43:             "sword_thrust", "刺击", TechniqueType.SWORD,
44:             6.0f, 15, 20, "正前方单体突刺"
45:     );
46:     public static final Technique SWORD_HORIZONTAL_SLASH = new Technique(
47:             "sword_horizontal_slash", "横劈", TechniqueType.SWORD,
48:             4.0f, 20, 20, "正前方扇形AOE"
49:     );
50:     public static final Technique SWORD_VERTICAL_SLASH = new Technique(
51:             "sword_vertical_slash", "竖劈", TechniqueType.SWORD,
52:             8.0f, 25, 20, "高倍率单体重击"
53:     );
54: 
55:     // === 动作 ===
56:     public static final Technique MOVEMENT_DASH = new Technique(
57:             "movement_dash", "冲刺", TechniqueType.MOVEMENT,
58:             2.0f, 30, 20, "向bind前方高速移动，碰撞伤害"
59:     );
60:     public static final Technique MOVEMENT_LEAP = new Technique(
61:             "movement_leap", "跳跃", TechniqueType.MOVEMENT,
62:             0f, 40, 20, "向bind上方弹射"
63:     );
64: 
65:     // === 法术 ===
66:     public static final Technique MAGIC_ARCANE_MISSILE = new Technique(
67:             "magic_arcane_missile", "奥术飞弹", TechniqueType.MAGIC,
68:             5.0f, 20, 20, "发射追踪弹幕"
69:     );
70:     public static final Technique MAGIC_CREATE_WATER = new Technique(
71:             "magic_create_water", "生成水", TechniqueType.MAGIC,
72:             0f, 60, 20, "在目标位置放置水源"
73:     );
74:     public static final Technique TEST_SUSTAINED = new Technique(
75:             "test_sustained", "持续试射", TechniqueType.MAGIC,
76:             1.0f, 10, 20, "测试用持续弹", 0
77:     );
78:     public static final Technique HEAT_BEAM = new Technique(
79:             "heat_beam", "热光束", TechniqueType.MAGIC,
80:             3.0f, 20, 20, "持续释放一束随准星转向的火属性光束，对接触的生物造成伤害", 0, ShuAttribute.FIRE, 1.0f
81:     );
82: 
83:     // === 通用（剑术书和法术书都能写入）===
84:     public static final Technique MAGIC_IGNITE = new Technique(
85:             "magic_ignite", "点火", TechniqueType.UNIVERSAL,
86:             0f, 40, 20, "点燃目标/方块", 0, ShuAttribute.FIRE, 1.0f, true
87:     );
88:     public static final Technique UNIVERSAL_LOCK_TARGET = new Technique(
89:             "universal_lock_target", "锁定实体", TechniqueType.UNIVERSAL,
90:             0f, 50, 20, "对最近实体施加发光效果"
91:     );
92:     public static final Technique RULE_DUAL_RELEASE = new Technique(
93:             "rule_dual_release", "双重释放", TechniqueType.UNIVERSAL,
94:             0f, 20, 20, "按顺序释放下一个术式，再隔 10 tick 带上后面两个"
95:     );
96:     public static final Technique TRAJECTORY_DROP = new Technique(
97:             "trajectory_drop", "垂落术式", TechniqueType.UNIVERSAL,
98:             0f, 20, 20, "将下一个指向性术式的这次路径改为垂落"
99:     );
100:     public static final Technique SPACE_CUT = new Technique(
101:             "space_cut", "空间切割", TechniqueType.SWORD,
102:             4f, 20, 20, "在落点留下一片静止的切割"
103:     );
104:     public static final Technique RULE_CAST_ANCHOR = new Technique(
105:             "rule_cast_anchor", "落点指定", TechniqueType.UNIVERSAL,
106:             0f, 0, 20, "有怪则落在怪处，否则落在准星前方"
107:     );
108:     public static final Technique RULE_TRIPLE_CAST = new Technique(
109:             "rule_triple_cast", "一分为三", TechniqueType.UNIVERSAL,
110:             0f, 0, 20, "复制后面第一个攻击术式三份并汇总伤害"
111:     );
112:     public static final Technique RULE_GATHER_ENERGY = new Technique(
113:             "rule_gather_energy", "聚能", TechniqueType.UNIVERSAL,
114:             0f, 0, 20, "把后面这次攻击的能量乘 2，碰撞体积跟着变大。多挂一条再乘一次"
115:     );
116:     public static final Technique TRAJECTORY_3D_INTERLEAVE = new Technique(
117:             "trajectory_3d_interleave", "三维交错", TechniqueType.UNIVERSAL,
118:             0f, 0, 20, "让每一份在落点周围错开"
119:     );
120:     public static final Technique VINE_MARK = new Technique(
121:             "vine_mark", "缠枝", TechniqueType.UNIVERSAL,
122:             0f, 40, 20, "命中后短暂缠住目标", 0, ShuAttribute.WOOD, 1.0f, true
123:     );
124:     public static final Technique PARASITE_SEED = new Technique(
125:             "parasite_seed", "寄生种子", TechniqueType.MAGIC,
126:             4f, 20, 20, "撒出三颗种子，落地成陷阱，踩中爆炸并持续吸取生命", 0, ShuAttribute.WOOD, 1.0f
127:     );
128:     public static final Technique TIDE_MARK = new Technique(
129:             "tide_mark", "潮印", TechniqueType.UNIVERSAL,
130:             0f, 40, 20, "每条在命中成功后附加2点伤害，目标潮湿+5，施术者水势+2", 0, ShuAttribute.WATER, 1.0f, true
131:     );
132:     public static final Technique TIDE_SEVER = new Technique(
133:             "tide_sever", "断潮", TechniqueType.SWORD,
134:             6f, 20, 20, "一道薄而长的水切", 0, ShuAttribute.WATER, 1.0f
135:     );
136:     public static final Technique SINKING_EDGE = new Technique(
137:             "sinking_edge", "沉锋", TechniqueType.UNIVERSAL,
138:             0f, 40, 20, "给攻击加上重量、范围和短促击退", 0, ShuAttribute.EARTH, 1.0f, true
139:     );
140:     public static final Technique STONE_HAMMER = new Technique(
141:             "stone_hammer", "岩槌", TechniqueType.SWORD,
142:             8f, 25, 20, "厚重的落点重击", 0, ShuAttribute.EARTH, 1.0f
143:     );
144:     public static final Technique KEEN_MARK = new Technique(
145:             "keen_mark", "锐锋印", TechniqueType.UNIVERSAL,
146:             0f, 40, 20, "给攻击加上金属性伤害并缩短冷却", 0, ShuAttribute.METAL, 1.0f, true
147:     );
148:     public static final Technique PHANTOM_SWORD = new Technique(
149:             "phantom_sword", "幻剑术", TechniqueType.SWORD,
150:             2f, 40, 20, "在身后凝出复制主手剑的幻剑，停滞后沿准星飞出并插地"
151:     );
152:     public static final Technique SWORD_COME = new Technique(
153:             "sword_come", "剑来", TechniqueType.SWORD,
154:             0f, 20, 20, "召回周围的剑"
155:     );
156:     public static final Technique SWORD_RETURN_ALL = new Technique(
157:             "sword_return_all", "万剑归宗", TechniqueType.SWORD,
158:             8f, 200, 20, "把已放出的幻剑和真剑收到身边四面环绕，再逐把飞向锁定目标"
159:     );
160:     public static final Technique METAL_NEEDLE = new Technique(
161:             "metal_needle", "金针", TechniqueType.MAGIC,
162:             5f, 20, 20, "飞出的金属细针", 0, ShuAttribute.METAL, 1.5f
163:     );
164:     public static final Technique FIRE_ORB = new Technique(
165:             "fire_orb", "火球", TechniqueType.MAGIC,
166:             5f, 20, 20, "飞出火球，命中造成伤害并点燃。弹道可被改写", 0, ShuAttribute.FIRE, 1.2f
167:     );
168:     public static final Technique FIRE_BURST = new Technique(
169:             "fire_burst", "爆燃", TechniqueType.MAGIC,
170:             8f, 40, 20, "撞上生物或方块，或两秒后在三格内爆炸并点燃", 0, ShuAttribute.FIRE, 0.6f
171:     );
172:     public static final Technique TRAJECTORY_TRACKING = new Technique(
173:             "trajectory_tracking", "追踪弹道", TechniqueType.UNIVERSAL,
174:             0f, 20, 20, "飞行弹丸以自身5格搜索并持续转向"
175:     );
176:     public static final Technique TRAJECTORY_STORM = new Technique(
177:             "trajectory_storm", "风暴轨迹", TechniqueType.UNIVERSAL,
178:             0f, 20, 20, "把后面的可变弹道改成龙卷风：从落点上方旋入准星落点，持续术式到达后停在那里"
179:     );
180:     public static final Technique WATER_YIELDING_RIBBON = new Technique(
181:             "water_yielding_ribbon", "让锋", TechniqueType.SWORD,
182:             40f, 600, 20, "先铺开水面，再从中间劈开。水分开没有伤害，只有中间那一刀", 0, ShuAttribute.WATER, 0f
183:     );
184:     public static final Technique WATER_WEEPING_ARC = new Technique(
185:             "water_weeping_arc", "垂丝", TechniqueType.MAGIC,
186:             WeepingSettings.BASE_DAMAGE, 20, 20, "前上方垂下一截水帘。只有当前还在的那段水能打到人，聚能只加宽加厚", 0, ShuAttribute.WATER, 0f
187:     );
188: 
189:     /**
190:      * 注册所有术到管理器
191:      */
192:     public static void registerAll() {
193:         WaterCurveFunctions.register();
194:         TechniqueManager.register(SWORD_THRUST);
195:         TechniqueManager.register(SWORD_HORIZONTAL_SLASH);
196:         TechniqueManager.register(SWORD_VERTICAL_SLASH);
197:         TechniqueManager.register(MOVEMENT_DASH);
198:         TechniqueManager.register(MOVEMENT_LEAP);
199:         TechniqueManager.register(MAGIC_ARCANE_MISSILE);
200:         TechniqueManager.register(MAGIC_IGNITE);
201:         TechniqueManager.register(MAGIC_CREATE_WATER);
202:         TechniqueManager.register(TEST_SUSTAINED);
203:         TechniqueManager.register(HEAT_BEAM);
204:         TechniqueManager.register(UNIVERSAL_LOCK_TARGET);
205:         TechniqueManager.register(RULE_DUAL_RELEASE);
206:         TechniqueManager.register(TRAJECTORY_DROP);
207:         TechniqueManager.register(SPACE_CUT);
208:         TechniqueManager.register(RULE_CAST_ANCHOR);
209:         TechniqueManager.register(RULE_TRIPLE_CAST);
210:         TechniqueManager.register(RULE_GATHER_ENERGY);
211:         TechniqueManager.register(TRAJECTORY_3D_INTERLEAVE);
212:         TechniqueManager.register(VINE_MARK);
213:         TechniqueManager.register(PARASITE_SEED);
214:         TechniqueManager.register(TIDE_MARK);
215:         TechniqueManager.register(TIDE_SEVER);
216:         TechniqueManager.register(SINKING_EDGE);
217:         TechniqueManager.register(STONE_HAMMER);
218:         TechniqueManager.register(KEEN_MARK);
219:         TechniqueManager.register(METAL_NEEDLE);
220:         TechniqueManager.register(FIRE_ORB);
221:         TechniqueManager.register(FIRE_BURST);
222:         TechniqueManager.register(TRAJECTORY_TRACKING);
223:         TechniqueManager.register(TRAJECTORY_STORM);
224:         TechniqueManager.register(WATER_YIELDING_RIBBON);
225:         TechniqueManager.register(WATER_WEEPING_ARC);
226:         TechniqueManager.register(PHANTOM_SWORD);
227:         TechniqueManager.register(SWORD_COME);
228:         TechniqueManager.register(SWORD_RETURN_ALL);
229: 
230:         TechniqueEffectRegistry.register("sword_thrust", new SwordThrustEffect());
231:         TechniqueEffectRegistry.register("sword_horizontal_slash", new SwordHorizontalSlashEffect());
232:         TechniqueEffectRegistry.register("sword_vertical_slash", new SwordVerticalSlashEffect());
233:         TechniqueEffectRegistry.register("movement_dash", new MovementDashEffect());
234:         TechniqueEffectRegistry.register("movement_leap", new MovementLeapEffect());
235:         TechniqueEffectRegistry.register("magic_arcane_missile", new MagicArcaneMissileEffect());
236:         TechniqueEffectRegistry.register("magic_ignite", new MagicIgniteEffect());
237:         TechniqueEffectRegistry.register("magic_create_water", new MagicCreateWaterEffect());
238:         TechniqueEffectRegistry.register("test_sustained", new TestSustainedEffect());
239:         TechniqueEffectRegistry.register("heat_beam", new HeatBeamEffect());
240:         TechniqueEffectRegistry.register("universal_lock_target", new UniversalLockTargetEffect());
241:         TechniqueEffectRegistry.register("rule_dual_release", (player, technique, weapon, cast) -> {});
242:         TechniqueEffectRegistry.register("trajectory_drop", (player, technique, weapon, cast) -> {});
243:         TechniqueEffectRegistry.register("space_cut", new SpaceCutEffect());
244:         TechniqueEffectRegistry.register("rule_cast_anchor", new CastAnchorEffect());
245:         TechniqueEffectRegistry.register("rule_triple_cast", new TripleCastEffect());
246:         TechniqueEffectRegistry.register("rule_gather_energy", (player, technique, weapon, cast) -> {});
247:         TechniqueEffectRegistry.register("trajectory_3d_interleave", new Scatter3dEffect());
248:         TechniqueEffectRegistry.register("parasite_seed", new ParasiteSeedEffect());
249:         TechniqueEffectRegistry.register("tide_sever", new TideSeverEffect());
250:         TechniqueEffectRegistry.register("stone_hammer", new StoneHammerEffect());
251:         TechniqueEffectRegistry.register("metal_needle", new MetalNeedleEffect());
252:         TechniqueEffectRegistry.register("fire_orb", new FireOrbEffect());
253:         TechniqueEffectRegistry.register("fire_burst", new FireBurstEffect());
254:         TechniqueEffectRegistry.register("trajectory_tracking", (player, technique, weapon, cast) -> {});
255:         TechniqueEffectRegistry.register("trajectory_storm", (player, technique, weapon, cast) -> {});
256:         TechniqueEffectRegistry.register("water_yielding_ribbon", new WaterYieldingEffect());
257:         TechniqueEffectRegistry.register("water_weeping_arc", new WeepingCurtainEffect());
258:         TechniqueEffectRegistry.register("phantom_sword", new PhantomSwordEffect());
259:         TechniqueEffectRegistry.register("sword_come", new SwordComeEffect());
260:         TechniqueEffectRegistry.register("sword_return_all", new SwordReturnAllEffect());
261: 
262:         EnchantmentDefinitions.registerAll();
263:         ReleaseDefinitions.registerAll();
264:         WaterTechniques.register();
265:     }
266: }
267: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/WaterTechniques.java">
1: package com.bslx.XueXiMod.shu;
2: 
3: import com.bslx.XueXiMod.shu.technique.TechniqueEffectRegistry;
4: import com.bslx.XueXiMod.shu.technique.effects.WaterFormEffect;
5: import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
6: import com.bslx.XueXiMod.shu.technique.release.Trajectory;
7: import com.bslx.XueXiMod.shu.technique.release.WaterReleaseDefinition;
8: import com.bslx.XueXiMod.shu.technique.release.conditional.WaterPrisonEffect;
9: import com.bslx.XueXiMod.shu.technique.release.water.WaterBeatBridge;
10: import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierDefinition;
11: import com.bslx.XueXiMod.shu.technique.release.domain.DomainDefinition;
12: import com.bslx.XueXiMod.shu.technique.release.guard.GuardDefinition;
13: 
14: /** 留下的水系成品：缺月、水幕、水雾、水环、深海重压、水牢。 */
15: public final class WaterTechniques {
16:     private static final WaterFormEffect FORM = new WaterFormEffect();
17: 
18:     private WaterTechniques() {
19:     }
20: 
21:     public static void register() {
22:         WaterBeatBridge.install();
23:         WaterReleaseDefinition blade = WaterReleaseDefinition.shot("water_blade", Trajectory.STRAIGHT, 10, 1.3f, 0.35f)
24:                 .along(3.2f).span(0.7f).energy(1.35f).wetness(6)
25:                 .swordForm();
26:         Technique bladeTechnique = new Technique("water_blade", "缺月", TechniqueType.SWORD, 7f, 14, 20,
27:                 "随挥剑扫出月牙形水流弧斩，可连续衔接", 0, ShuAttribute.WATER, blade.speed());
28:         TechniqueManager.register(bladeTechnique);
29:         ReleaseRegistry.register(blade);
30:         TechniqueEffectRegistry.register("water_blade", FORM);
31:         barrier("water_curtain", "水幕", 40, 0, "身前立起一道固定的薄水墙，穿过它的箭伤害减半",
32:                 BarrierDefinition.curtain());
33:         domain("water_mist", "雾雨", 80, 10, "固定的雨雾领域。完整进入的敌人出不去，并会变潮湿",
34:                 DomainDefinition.mistRain());
35:         guard("water_ring", "水环", 40, 0, "环绕自身的水环，受到的伤害削去两成",
36:                 GuardDefinition.personalRing());
37:         domain("water_deep_pressure", "深海重压", 80, 10, "低矮的重压领域。里面的人变慢、变潮湿，氧气消耗加快",
38:                 DomainDefinition.deepPressure());
39:         prison();
40:     }
41: 
42:     /**
43:      * 不登记水系命中效果。那条生成会打出命中体，水牢的伤害必须保持为 0。
44:      */
45:     private static void prison() {
46:         WaterReleaseDefinition definition = WaterReleaseDefinition.conditional("water_prison", 40, WaterPrisonEffect.INSTANCE);
47:         Technique technique = new Technique("water_prison", "水牢", TechniqueType.MAGIC, 0f, 400, 20,
48:                 "对准二十格内一个已经潮湿的生物，把它关进水牢。关不进去就不耗魔力。",
49:                 0, ShuAttribute.WATER, 0f);
50:         TechniqueManager.register(technique);
51:         ReleaseRegistry.register(definition);
52:     }
53: 
54:     private static void shot(String releaseId, String name, float damage, int cooldown, int complexity,
55:                              String description, WaterReleaseDefinition definition) {
56:         Technique technique = new Technique(releaseId, name, TechniqueType.MAGIC, damage, cooldown, 20,
57:                 description, complexity, ShuAttribute.WATER, definition.speed());
58:         TechniqueManager.register(technique);
59:         ReleaseRegistry.register(definition);
60:         TechniqueEffectRegistry.register(releaseId, FORM);
61:     }
62: 
63:     private static void zone(String releaseId, String name, int complexity, String description,
64:                              WaterReleaseDefinition definition) {
65:         zone(releaseId, name, 40, complexity, description, definition);
66:     }
67: 
68:     private static void zone(String releaseId, String name, int cooldown, int complexity, String description,
69:                              WaterReleaseDefinition definition) {
70:         Technique technique = new Technique(releaseId, name, TechniqueType.MAGIC, 0f, cooldown, 20,
71:                 description, complexity, ShuAttribute.WATER, 0f);
72:         TechniqueManager.register(technique);
73:         ReleaseRegistry.register(definition);
74:         TechniqueEffectRegistry.register(releaseId, FORM);
75:     }
76: 
77:     private static void domain(String releaseId, String name, int cooldown, int complexity, String description,
78:                                DomainDefinition domain) {
79:         Technique technique = new Technique(releaseId, name, TechniqueType.MAGIC, 0f, cooldown, 20,
80:                 description, complexity, ShuAttribute.WATER, 0f);
81:         TechniqueManager.register(technique);
82:         ReleaseRegistry.register(WaterReleaseDefinition.domain(releaseId, domain));
83:         TechniqueEffectRegistry.register(releaseId, FORM);
84:     }
85: 
86:     private static void barrier(String releaseId, String name, int cooldown, int complexity, String description,
87:                                 BarrierDefinition barrier) {
88:         Technique technique = new Technique(releaseId, name, TechniqueType.MAGIC, 0f, cooldown, 20,
89:                 description, complexity, ShuAttribute.WATER, 0f);
90:         TechniqueManager.register(technique);
91:         ReleaseRegistry.register(WaterReleaseDefinition.barrier(releaseId, barrier));
92:         TechniqueEffectRegistry.register(releaseId, FORM);
93:     }
94: 
95:     private static void guard(String releaseId, String name, int cooldown, int complexity, String description,
96:                               GuardDefinition guard) {
97:         Technique technique = new Technique(releaseId, name, TechniqueType.MAGIC, 0f, cooldown, 20,
98:                 description, complexity, ShuAttribute.WATER, 0f);
99:         TechniqueManager.register(technique);
100:         ReleaseRegistry.register(WaterReleaseDefinition.bodyGuard(releaseId, guard));
101:         TechniqueEffectRegistry.register(releaseId, FORM);
102:     }
103: }
104: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseRegistry.java">
1: package com.bslx.XueXiMod.shu.technique.release;
2: 
3: import com.bslx.XueXiMod.shu.TechniqueIds;
4: import org.jetbrains.annotations.Nullable;
5: 
6: import java.util.LinkedHashMap;
7: import java.util.Map;
8: 
9: public final class ReleaseRegistry {
10:     private static final Map<String, ReleaseDefinition> DEFINITIONS = new LinkedHashMap<>();
11:     private static final java.util.Set<String> HOST_GROUP = java.util.Set.of("space_cut");
12: 
13:     private ReleaseRegistry() {}
14: 
15:     public static void register(ReleaseDefinition definition) {
16:         DEFINITIONS.put(definition.id(), definition);
17:     }
18: 
19:     @Nullable
20:     public static ReleaseDefinition get(String identifier) {
21:         if (identifier == null) {
22:             return null;
23:         }
24:         return DEFINITIONS.get(TechniqueIds.canonicalId(identifier));
25:     }
26: 
27:     /** 这条术式是否走宿主组提交。未登记的术式保持原来的即时释放。 */
28:     public static boolean submitsHostGroup(String identifier) {
29:         if (identifier == null) {
30:             return false;
31:         }
32:         return HOST_GROUP.contains(TechniqueIds.canonicalId(identifier));
33:     }
34: }
35: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseDefinitions.java">
1: package com.bslx.XueXiMod.shu.technique.release;
2: 
3: public final class ReleaseDefinitions {
4:     private ReleaseDefinitions() {}
5: 
6:     public static void registerAll() {
7:         ReleaseRegistry.register(define("movement_dash", ReleaseKind.ACTION, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 10, 2f, 0f, 0, false, false, false, false, ChainRole.ACTION, false, false));
8:         ReleaseRegistry.register(cohortSword("sword_horizontal_slash", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 4, 0f, 0.5f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
9:         ReleaseRegistry.register(cohortSword("sword_vertical_slash", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 4, 0f, 0.5f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
10:         ReleaseRegistry.register(cohortSword("sword_thrust", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 4, 0f, 0.5f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
11:         ReleaseRegistry.register(define("magic_arcane_missile", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, true, 40, 1.5f, 0.1f, 0, true, true, false, false, ChainRole.ATTACK, true, false));
12:         ReleaseRegistry.register(define("test_sustained", ReleaseKind.HIT_VOLUME, ReleaseTag.SUSTAINED, Trajectory.DROP, true, 20, 0.4f, 0.1f, 0, false, true, false, false, ChainRole.ATTACK, true, false));
13:         ReleaseRegistry.register(define("heat_beam", ReleaseKind.HIT_VOLUME, ReleaseTag.SUSTAINED, Trajectory.FOLLOW_LOOK, false, 40, 0f, 1f, 0, true, true, false, false, ChainRole.ATTACK, true, false));
14:         ReleaseRegistry.register(define("universal_lock_target", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, true, false, ChainRole.SEEK_ENTITY, false, false));
15:         ReleaseRegistry.register(define("rule_dual_release", ReleaseKind.RULE, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 1, false, false, false, false, ChainRole.RULE_DUAL, false, false));
16:         ReleaseRegistry.register(define("trajectory_drop", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.DROP, false, 1, 0f, 0f, 0, false, false, false, true, ChainRole.TRAJECTORY_DROP, false, false));
17:         ReleaseRegistry.register(define("trajectory_tracking", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.TRAJECTORY_TRACKING, false, false));
18:         ReleaseRegistry.register(define("trajectory_storm", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.VORTEX, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.TRAJECTORY_DROP, false, false));
19:         ReleaseRegistry.register(define("fire_orb", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, true, 40, 1.2f, 0.4f, 0, true, true, false, false, ChainRole.ATTACK, true, false));
20:         ReleaseRegistry.register(define("fire_burst", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, true, 40, 0.6f, 0.8f, 0, true, true, false, false, ChainRole.ATTACK, true, false));
21:         ReleaseRegistry.register(cohortSword("water_yielding_ribbon", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 32, 0f, 0.55f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
22:         ReleaseRegistry.register(define("water_weeping_arc", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.FUNCTION, true, WeepingSettings.LIFETIME, 0f, 0.35f, 0, true, true, false, false, ChainRole.ATTACK, false, false, false, 1f, 1f, true, true, false, false, 0f, 0, WeepingSettings.WETNESS, false, true, true, WeepingSettings.RETAINED_MANA));
23:         ReleaseRegistry.register(cohortSword("space_cut", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 10, 0f, 0f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
24:         ReleaseRegistry.register(define("rule_cast_anchor", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, true, false, ChainRole.SEEK_SPACE, false, false));
25:         ReleaseRegistry.register(define("rule_triple_cast", ReleaseKind.RULE, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.RULE_TRIPLE, false, false));
26:         ReleaseRegistry.register(define("rule_gather_energy", ReleaseKind.RULE, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.ATTACK, false, false, false, 1f, 2f));
27:         ReleaseRegistry.register(define("trajectory_3d_interleave", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 1, 0f, 0f, 0, false, false, false, false, ChainRole.TRAJECTORY_SCATTER, false, false));
28:         ReleaseRegistry.register(define("parasite_seed", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.DROP, true, 160, 0f, 0.5f, 0, false, true, false, false, ChainRole.ATTACK, true, false));
29:         ReleaseRegistry.register(cohortSword("tide_sever", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 6, 0f, 0.4f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
30:         ReleaseRegistry.register(cohortSword("stone_hammer", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 10, 0f, 1f, 0, false, false, false, false, ChainRole.ATTACK, false, false));
31:         ReleaseRegistry.register(define("metal_needle", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, true, 15, 1.5f, 0.2f, 0, true, true, false, false, ChainRole.ATTACK, true, false));
32:         ReleaseRegistry.register(define("phantom_sword", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, true, 80, 2.5f, 0.5f, 0, true, true, false, false, ChainRole.ATTACK, true, false));
33:         ReleaseRegistry.register(define("sword_come", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 20, 0f, 0.5f, 0, false, false, false, false, ChainRole.ATTACK, false, true, true));
34:         ReleaseRegistry.register(define("sword_return_all", ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, Trajectory.STRAIGHT, false, 40, 0f, 0.5f, 0, false, false, false, false, ChainRole.ATTACK, false, true, true));
35:     }
36: 
37:     /** 命中实体上的剑术斩面、水弧或水带。幻剑和召回不走这里。 */
38:     private static ReleaseDefinition cohortSword(String releaseId, ReleaseKind kind, ReleaseTag tag, Trajectory trajectory,
39:                                                   boolean moving, int lifetimeTicks, float speed, float thickness, int pullAfterA,
40:                                                   boolean acceptsTargetOverride, boolean acceptsPathOverride,
41:                                                   boolean setsTargetForNextDirectional, boolean setsDropForNextDirectional,
42:                                                   ChainRole role, boolean trajectoryMutable, boolean manipulatesSwordEntities) {
43:         return define(releaseId, kind, tag, trajectory, moving, lifetimeTicks, speed, thickness, pullAfterA,
44:                 acceptsTargetOverride, acceptsPathOverride, setsTargetForNextDirectional, setsDropForNextDirectional,
45:                 role, trajectoryMutable, manipulatesSwordEntities, false, 1f, 1f,
46:                 true, true, true, true, 0f, 0, 0, true, false, false, -1);
47:     }
48: 
49:     private static ReleaseDefinition define(String releaseId, ReleaseKind kind, ReleaseTag tag, Trajectory trajectory,
50:                                             boolean moving, int lifetimeTicks, float speed, float thickness, int pullAfterA,
51:                                             boolean acceptsTargetOverride, boolean acceptsPathOverride,
52:                                             boolean setsTargetForNextDirectional, boolean setsDropForNextDirectional,
53:                                             ChainRole role, boolean trajectoryMutable, boolean manipulatesSwordEntities) {
54:         return define(releaseId, kind, tag, trajectory, moving, lifetimeTicks, speed, thickness, pullAfterA,
55:                 acceptsTargetOverride, acceptsPathOverride, setsTargetForNextDirectional, setsDropForNextDirectional,
56:                 role, trajectoryMutable, manipulatesSwordEntities, false, 1f);
57:     }
58: 
59:     private static ReleaseDefinition define(String releaseId, ReleaseKind kind, ReleaseTag tag, Trajectory trajectory,
60:                                             boolean moving, int lifetimeTicks, float speed, float thickness, int pullAfterA,
61:                                             boolean acceptsTargetOverride, boolean acceptsPathOverride,
62:                                             boolean setsTargetForNextDirectional, boolean setsDropForNextDirectional,
63:                                             ChainRole role, boolean trajectoryMutable, boolean manipulatesSwordEntities,
64:                                             boolean refusesMixedChain) {
65:         return define(releaseId, kind, tag, trajectory, moving, lifetimeTicks, speed, thickness, pullAfterA,
66:                 acceptsTargetOverride, acceptsPathOverride, setsTargetForNextDirectional, setsDropForNextDirectional,
67:                 role, trajectoryMutable, manipulatesSwordEntities, refusesMixedChain, 1f, 1f,
68:                 true, true, true, true, 0f, 0, 0, false, false, false, -1);
69:     }
70: 
71:     private static ReleaseDefinition define(String releaseId, ReleaseKind kind, ReleaseTag tag, Trajectory trajectory,
72:                                             boolean moving, int lifetimeTicks, float speed, float thickness, int pullAfterA,
73:                                             boolean acceptsTargetOverride, boolean acceptsPathOverride,
74:                                             boolean setsTargetForNextDirectional, boolean setsDropForNextDirectional,
75:                                             ChainRole role, boolean trajectoryMutable, boolean manipulatesSwordEntities,
76:                                             boolean refusesMixedChain, float energyScale) {
77:         return define(releaseId, kind, tag, trajectory, moving, lifetimeTicks, speed, thickness, pullAfterA,
78:                 acceptsTargetOverride, acceptsPathOverride, setsTargetForNextDirectional, setsDropForNextDirectional,
79:                 role, trajectoryMutable, manipulatesSwordEntities, refusesMixedChain, energyScale, 1f,
80:                 true, true, true, true, 0f, 0, 0, false, false, false, -1);
81:     }
82: 
83:     private static ReleaseDefinition define(String releaseId, ReleaseKind kind, ReleaseTag tag, Trajectory trajectory,
84:                                             boolean moving, int lifetimeTicks, float speed, float thickness, int pullAfterA,
85:                                             boolean acceptsTargetOverride, boolean acceptsPathOverride,
86:                                             boolean setsTargetForNextDirectional, boolean setsDropForNextDirectional,
87:                                             ChainRole role, boolean trajectoryMutable, boolean manipulatesSwordEntities,
88:                                             boolean refusesMixedChain, float energyScale, float chainEnergyScale) {
89:         return define(releaseId, kind, tag, trajectory, moving, lifetimeTicks, speed, thickness, pullAfterA,
90:                 acceptsTargetOverride, acceptsPathOverride, setsTargetForNextDirectional, setsDropForNextDirectional,
91:                 role, trajectoryMutable, manipulatesSwordEntities, refusesMixedChain, energyScale, chainEnergyScale,
92:                 true, true, true, true, 0f, 0, 0, false, false, false, -1);
93:     }
94: 
95:     private static ReleaseDefinition define(String releaseId, ReleaseKind kind, ReleaseTag tag, Trajectory trajectory,
96:                                             boolean moving, int lifetimeTicks, float speed, float thickness, int pullAfterA,
97:                                             boolean acceptsTargetOverride, boolean acceptsPathOverride,
98:                                             boolean setsTargetForNextDirectional, boolean setsDropForNextDirectional,
99:                                             ChainRole role, boolean trajectoryMutable, boolean manipulatesSwordEntities,
100:                                             boolean refusesMixedChain, float energyScale, float chainEnergyScale,
101:                                             boolean scaleLength, boolean scaleWidth, boolean scaleHeight, boolean scaleModel,
102:                                             float heavyPerCopy, int pressureMinimum, int wetnessAdded,
103:                                             boolean cohortVisual, boolean persistentFlow, boolean deflectIncoming,
104:                                             int explicitCost) {
105:         return new ReleaseDefinition() {
106:             @Override
107:             public String id() {
108:                 return releaseId;
109:             }
110: 
111:             @Override
112:             public ReleaseKind kind() {
113:                 return kind;
114:             }
115: 
116:             @Override
117:             public ReleaseTag tag() {
118:                 return tag;
119:             }
120: 
121:             @Override
122:             public Trajectory trajectory() {
123:                 return trajectory;
124:             }
125: 
126:             @Override
127:             public boolean moving() {
128:                 return moving;
129:             }
130: 
131:             @Override
132:             public int lifetimeTicks() {
133:                 return lifetimeTicks;
134:             }
135: 
136:             @Override
137:             public float speed() {
138:                 return speed;
139:             }
140: 
141:             @Override
142:             public float thickness() {
143:                 return thickness;
144:             }
145: 
146:             @Override
147:             public int pullAfterA() {
148:                 return pullAfterA;
149:             }
150: 
151:             @Override
152:             public boolean acceptsTargetOverride() {
153:                 return acceptsTargetOverride;
154:             }
155: 
156:             @Override
157:             public boolean acceptsPathOverride() {
158:                 return acceptsPathOverride;
159:             }
160: 
161:             @Override
162:             public boolean setsTargetForNextDirectional() {
163:                 return setsTargetForNextDirectional;
164:             }
165: 
166:             @Override
167:             public boolean setsDropForNextDirectional() {
168:                 return setsDropForNextDirectional;
169:             }
170: 
171:             @Override
172:             public ChainRole role() {
173:                 return role;
174:             }
175: 
176:             @Override
177:             public boolean trajectoryMutable() {
178:                 return trajectoryMutable;
179:             }
180: 
181:             @Override
182:             public boolean manipulatesSwordEntities() {
183:                 return manipulatesSwordEntities;
184:             }
185: 
186:             @Override
187:             public boolean refusesMixedChain() {
188:                 return refusesMixedChain;
189:             }
190: 
191:             @Override
192:             public float energyScale() {
193:                 return energyScale;
194:             }
195: 
196:             @Override
197:             public float chainEnergyScale() {
198:                 return chainEnergyScale;
199:             }
200: 
201:             @Override
202:             public boolean energyScalesLength() {
203:                 return scaleLength;
204:             }
205: 
206:             @Override
207:             public boolean energyScalesWidth() {
208:                 return scaleWidth;
209:             }
210: 
211:             @Override
212:             public boolean energyScalesHeight() {
213:                 return scaleHeight;
214:             }
215: 
216:             @Override
217:             public boolean energyScalesModel() {
218:                 return scaleModel;
219:             }
220: 
221:             @Override
222:             public float heavyMomentumPerCopy() {
223:                 return heavyPerCopy;
224:             }
225: 
226:             @Override
227:             public int pressureWetnessMinimum() {
228:                 return pressureMinimum;
229:             }
230: 
231:             @Override
232:             public int wetnessGrant() {
233:                 return wetnessAdded;
234:             }
235: 
236:             @Override
237:             public boolean separatesCohortVisuals() {
238:                 return cohortVisual;
239:             }
240: 
241:             @Override
242:             public boolean persistentFlowSettlement() {
243:                 return persistentFlow;
244:             }
245: 
246:             @Override
247:             public boolean deflectsProjectiles() {
248:                 return deflectIncoming;
249:             }
250: 
251:             @Override
252:             public int explicitManaCost() {
253:                 return explicitCost;
254:             }
255:         };
256:     }
257: }
258: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/WaterReleaseDefinition.java">
1: package com.bslx.XueXiMod.shu.technique.release;
2: 
3: import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierDefinition;
4: import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalEffect;
5: import com.bslx.XueXiMod.shu.technique.release.domain.DomainDefinition;
6: import com.bslx.XueXiMod.shu.technique.release.guard.GuardDefinition;
7: 
8: /**
9:  * 水系成品的释放数据。结算不看术式编号，只读这些字段。
10:  */
11: public final class WaterReleaseDefinition implements ReleaseDefinition {
12:     private final String releaseId;
13:     private final Trajectory trajectory;
14:     private final boolean moving;
15:     private final int lifetimeTicks;
16:     private final float speed;
17:     private final float thickness;
18:     private boolean trajectoryMutable;
19:     private boolean acceptInitialAim;
20:     private boolean heldSword;
21:     private boolean hostGroupSubmit;
22:     private boolean arcBand;
23:     private boolean beatCycle;
24:     private boolean cohortVisual;
25:     private float energyScale = 1f;
26:     private int explicitCost = -1;
27:     private float fieldRadius;
28:     private int upkeepCost;
29:     private int fieldLife = 80;
30:     private float pullStrength;
31:     private int wetnessGrant;
32:     private float heavyPerCopy;
33:     private int pressureMinimum;
34:     private boolean purify;
35:     private boolean blocksMovement;
36:     private float pressurePulse;
37:     private float knockbackStrength;
38:     private float liftStrength;
39:     private float unloadRatio;
40:     private int slowTicks;
41:     private float endBurstDamage;
42:     private int bodyVisual = 5;
43:     private String curveFunction = "";
44:     private ReleaseShape shape = ReleaseShape.BOX;
45:     private float spanScale = 1f;
46:     private float lengthScale = 1f;
47:     private GuardDefinition guard;
48:     private BarrierDefinition barrier;
49:     private DomainDefinition domain;
50:     private ConditionalEffect conditionalEffect;
51:     private boolean deflectsIncoming;
52: 
53:     private WaterReleaseDefinition(String releaseId, Trajectory trajectory, boolean moving, int lifetimeTicks,
54:                                    float speed, float thickness, boolean trajectoryMutable) {
55:         this.releaseId = releaseId;
56:         this.trajectory = trajectory;
57:         this.moving = moving;
58:         this.lifetimeTicks = lifetimeTicks;
59:         this.speed = speed;
60:         this.thickness = thickness;
61:         this.trajectoryMutable = trajectoryMutable;
62:         this.acceptInitialAim = trajectoryMutable;
63:         this.heldSword = false;
64:     }
65: 
66:     /** 弹道锁定，仍接受出手瞬间的目标方向，并且必须手持剑。不改其他水系射击的默认值。 */
67:     public WaterReleaseDefinition swordForm() {
68:         this.trajectoryMutable = false;
69:         this.acceptInitialAim = true;
70:         this.heldSword = true;
71:         this.hostGroupSubmit = true;
72:         this.arcBand = true;
73:         this.beatCycle = true;
74:         this.cohortVisual = true;
75:         this.deflectsIncoming = true;
76:         return this;
77:     }
78: 
79:     public static WaterReleaseDefinition shot(String releaseId, Trajectory trajectory, int lifetimeTicks,
80:                                               float speed, float thickness) {
81:         return new WaterReleaseDefinition(releaseId, trajectory, true, lifetimeTicks, speed, thickness, true);
82:     }
83: 
84:     public static WaterReleaseDefinition zone(String releaseId, float radius, int lifetimeTicks, int manaCost, int upkeepCost) {
85:         WaterReleaseDefinition definition = new WaterReleaseDefinition(releaseId, Trajectory.STRAIGHT, false, lifetimeTicks, 0f, radius, false);
86:         definition.fieldRadius = radius;
87:         definition.fieldLife = lifetimeTicks;
88:         definition.explicitCost = manaCost;
89:         definition.upkeepCost = upkeepCost;
90:         definition.bodyVisual = 1;
91:         return definition;
92:     }
93: 
94:     public static WaterReleaseDefinition barrier(String releaseId, BarrierDefinition barrier) {
95:         WaterReleaseDefinition definition = new WaterReleaseDefinition(releaseId, Trajectory.STRAIGHT, false,
96:                 barrier.lifetimeTicks(), 0f, 0f, false);
97:         definition.explicitCost = 0;
98:         definition.barrier = barrier;
99:         definition.fieldLife = barrier.lifetimeTicks();
100:         definition.upkeepCost = barrier.upkeepCost();
101:         return definition;
102:     }
103: 
104:     public static WaterReleaseDefinition domain(String releaseId, DomainDefinition domain) {
105:         WaterReleaseDefinition definition = new WaterReleaseDefinition(releaseId, Trajectory.STRAIGHT, false,
106:                 domain.lifetimeTicks(), 0f, 0f, false);
107:         definition.explicitCost = 0;
108:         definition.domain = domain;
109:         definition.fieldLife = domain.lifetimeTicks();
110:         definition.upkeepCost = domain.upkeepCost();
111:         return definition;
112:     }
113: 
114:     public static WaterReleaseDefinition bodyGuard(String releaseId, GuardDefinition guard) {
115:         WaterReleaseDefinition definition = new WaterReleaseDefinition(releaseId, Trajectory.STRAIGHT, false,
116:                 guard.lifetimeTicks(), 0f, 0f, false);
117:         definition.explicitCost = 0;
118:         definition.guard = guard;
119:         return definition;
120:     }
121: 
122:     /** 零伤条件释放。显式魔力只在非负时写下，不顺手变成领域、屏障或护体。 */
123:     public static WaterReleaseDefinition conditional(String releaseId, int manaCost, ConditionalEffect effect) {
124:         WaterReleaseDefinition definition = new WaterReleaseDefinition(releaseId, Trajectory.STRAIGHT, false,
125:                 1, 0f, 0f, false);
126:         if (manaCost >= 0) {
127:             definition.explicitCost = manaCost;
128:         }
129:         definition.conditionalEffect = effect;
130:         return definition;
131:     }
132: 
133:     public WaterReleaseDefinition energy(float scale) {
134:         this.energyScale = scale;
135:         return this;
136:     }
137: 
138:     public WaterReleaseDefinition wetness(int amount) {
139:         this.wetnessGrant = amount;
140:         return this;
141:     }
142: 
143:     /** 每一份有效攻击预留的重水。非有限或负数留下原值。0 表示不走重水。 */
144:     public WaterReleaseDefinition heavyMomentum(float perCopy) {
145:         if (Float.isFinite(perCopy) && perCopy >= 0f) {
146:             this.heavyPerCopy = perCopy;
147:         }
148:         return this;
149:     }
150: 
151:     /** 重压的潮湿门槛，只接受 0 到 100。越界留下原值。0 表示不挂重压。 */
152:     public WaterReleaseDefinition pressureWetness(int minimum) {
153:         if (minimum >= 0 && minimum <= 100) {
154:             this.pressureMinimum = minimum;
155:         }
156:         return this;
157:     }
158: 
159:     public WaterReleaseDefinition pull(float strength) {
160:         this.pullStrength = strength;
161:         return this;
162:     }
163: 
164:     public WaterReleaseDefinition pressure(float amount) {
165:         this.pressurePulse = amount;
166:         return this;
167:     }
168: 
169:     public WaterReleaseDefinition knock(float strength) {
170:         this.knockbackStrength = strength;
171:         return this;
172:     }
173: 
174:     public WaterReleaseDefinition lift(float strength) {
175:         this.liftStrength = strength;
176:         return this;
177:     }
178: 
179:     public WaterReleaseDefinition unload(float ratio) {
180:         this.unloadRatio = ratio;
181:         return this;
182:     }
183: 
184:     public WaterReleaseDefinition slow(int ticks) {
185:         this.slowTicks = ticks;
186:         return this;
187:     }
188: 
189:     public WaterReleaseDefinition burst(float damage) {
190:         this.endBurstDamage = damage;
191:         return this;
192:     }
193: 
194:     public WaterReleaseDefinition purify() {
195:         this.purify = true;
196:         return this;
197:     }
198: 
199:     public WaterReleaseDefinition wall() {
200:         this.blocksMovement = true;
201:         return this;
202:     }
203: 
204:     public WaterReleaseDefinition visual(int visual) {
205:         this.bodyVisual = visual;
206:         return this;
207:     }
208: 
209:     public WaterReleaseDefinition curve(String functionId) {
210:         this.curveFunction = functionId == null ? "" : functionId;
211:         return this;
212:     }
213: 
214:     public WaterReleaseDefinition shape(ReleaseShape shape) {
215:         this.shape = shape;
216:         return this;
217:     }
218: 
219:     public WaterReleaseDefinition span(float scale) {
220:         this.spanScale = scale;
221:         return this;
222:     }
223: 
224:     public WaterReleaseDefinition along(float scale) {
225:         this.lengthScale = scale;
226:         return this;
227:     }
228: 
229:     @Override
230:     public String id() {
231:         return releaseId;
232:     }
233: 
234:     @Override
235:     public ReleaseKind kind() {
236:         return ReleaseKind.HIT_VOLUME;
237:     }
238: 
239:     @Override
240:     public ReleaseTag tag() {
241:         return ReleaseTag.TRIGGER;
242:     }
243: 
244:     @Override
245:     public Trajectory trajectory() {
246:         return trajectory;
247:     }
248: 
249:     @Override
250:     public boolean moving() {
251:         return moving;
252:     }
253: 
254:     @Override
255:     public int lifetimeTicks() {
256:         return lifetimeTicks;
257:     }
258: 
259:     @Override
260:     public float speed() {
261:         return speed;
262:     }
263: 
264:     @Override
265:     public float thickness() {
266:         return thickness;
267:     }
268: 
269:     @Override
270:     public boolean acceptsTargetOverride() {
271:         return trajectoryMutable;
272:     }
273: 
274:     @Override
275:     public boolean acceptsInitialAim() {
276:         return acceptInitialAim;
277:     }
278: 
279:     @Override
280:     public boolean requiresHeldSword() {
281:         return heldSword;
282:     }
283: 
284:     @Override
285:     public boolean submitsHostGroup() {
286:         return hostGroupSubmit;
287:     }
288: 
289:     @Override
290:     public boolean usesArcBand() {
291:         return arcBand;
292:     }
293: 
294:     @Override
295:     public boolean tracksBeatCycle() {
296:         return beatCycle;
297:     }
298: 
299:     @Override
300:     public boolean deflectsProjectiles() {
301:         if (deflectsIncoming) {
302:             return true;
303:         }
304:         if (barrier != null && barrier.deflectsProjectiles()) {
305:             return true;
306:         }
307:         return guard != null && guard.deflectsProjectiles();
308:     }
309: 
310:     @Override
311:     public boolean separatesCohortVisuals() {
312:         return cohortVisual;
313:     }
314: 
315:     @Override
316:     public boolean acceptsPathOverride() {
317:         return trajectoryMutable;
318:     }
319: 
320:     @Override
321:     public ChainRole role() {
322:         return ChainRole.ATTACK;
323:     }
324: 
325:     @Override
326:     public boolean trajectoryMutable() {
327:         return trajectoryMutable;
328:     }
329: 
330:     @Override
331:     public float energyScale() {
332:         return energyScale;
333:     }
334: 
335:     @Override
336:     public int explicitManaCost() {
337:         return explicitCost;
338:     }
339: 
340:     @Override
341:     public float fieldRadius() {
342:         return fieldRadius;
343:     }
344: 
345:     @Override
346:     public GuardDefinition guard() {
347:         return guard;
348:     }
349: 
350:     @Override
351:     public BarrierDefinition barrier() {
352:         return barrier;
353:     }
354: 
355:     @Override
356:     public DomainDefinition domain() {
357:         return domain;
358:     }
359: 
360:     @Override
361:     public ConditionalEffect conditionalEffect() {
362:         return conditionalEffect;
363:     }
364: 
365:     @Override
366:     public int fieldUpkeepCost() {
367:         return upkeepCost;
368:     }
369: 
370:     @Override
371:     public int fieldLifetimeTicks() {
372:         return fieldLife;
373:     }
374: 
375:     @Override
376:     public float pullStrength() {
377:         return pullStrength;
378:     }
379: 
380:     @Override
381:     public int wetnessGrant() {
382:         return wetnessGrant;
383:     }
384: 
385:     @Override
386:     public float heavyMomentumPerCopy() {
387:         return heavyPerCopy;
388:     }
389: 
390:     @Override
391:     public int pressureWetnessMinimum() {
392:         return pressureMinimum;
393:     }
394: 
395:     @Override
396:     public boolean purifyOnHit() {
397:         return purify;
398:     }
399: 
400:     @Override
401:     public boolean blocksMovement() {
402:         return blocksMovement;
403:     }
404: 
405:     @Override
406:     public float pressurePulse() {
407:         return pressurePulse;
408:     }
409: 
410:     @Override
411:     public float knockbackStrength() {
412:         return knockbackStrength;
413:     }
414: 
415:     @Override
416:     public float liftStrength() {
417:         return liftStrength;
418:     }
419: 
420:     @Override
421:     public float unloadRatio() {
422:         return unloadRatio;
423:     }
424: 
425:     @Override
426:     public int slowTicks() {
427:         return slowTicks;
428:     }
429: 
430:     @Override
431:     public float endBurstDamage() {
432:         return endBurstDamage;
433:     }
434: 
435:     @Override
436:     public int bodyVisual() {
437:         return bodyVisual;
438:     }
439: 
440:     @Override
441:     public String functionId() {
442:         return curveFunction;
443:     }
444: 
445:     @Override
446:     public ReleaseShape shape() {
447:         return shape;
448:     }
449: 
450:     @Override
451:     public float spanScale() {
452:         return spanScale;
453:     }
454: 
455:     @Override
456:     public float lengthScale() {
457:         return lengthScale;
458:     }
459: }
460: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitSpec.java">
1: package com.bslx.XueXiMod.shu.technique.release.entity;
2: 
3: import com.bslx.XueXiMod.command.SkillEntityCount;
4: import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
5: import com.bslx.XueXiMod.shu.technique.release.CastFrame;
6: import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
7: import com.bslx.XueXiMod.shu.technique.release.Trajectory;
8: import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
9: import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
10: import com.bslx.XueXiMod.shu.technique.release.momentum.HeavyWater;
11: import com.bslx.XueXiMod.shu.technique.release.payload.HitPayloads;
12: import com.bslx.XueXiMod.shu.technique.release.payload.WetPressure;
13: import com.bslx.XueXiMod.shu.technique.release.water.ArcBandDraft;
14: import com.bslx.XueXiMod.shu.technique.release.water.ArcBandSnapshot;
15: import com.bslx.XueXiMod.shu.technique.release.water.ArcVec;
16: import com.bslx.XueXiMod.shu.technique.release.water.RadialCopyPlane;
17: import net.minecraft.server.level.ServerLevel;
18: import net.minecraft.server.level.ServerPlayer;
19: import net.minecraft.world.entity.LivingEntity;
20: import net.minecraft.world.phys.Vec3;
21: 
22: import java.util.function.Consumer;
23: 
24: public final class TechniqueHitSpec {
25: 
26:     public enum Motion {
27:         SWING,
28:         PROJECTILE,
29:         DROP,
30:         FOLLOW
31:     }
32: 
33:     public enum Hitbox {
34:         ARC,
35:         OBB,
36:         BOX,
37:         SPHERE
38:     }
39: 
40:     public enum Path {
41:         STILL_AIM,
42:         STRAIGHT,
43:         DROP,
44:         FOLLOW,
45:         VORTEX,
46:         FUNCTION
47:     }
48: 
49:     public interface HitListener {
50:         void onHit(LivingEntity target);
51: 
52:         void onExpire(Vec3 position);
53:     }
54: 
55:     public interface HitListenerFactory {
56:         HitListener create();
57:     }
58: 
59:     public String techniqueId = "";
60:     public Motion motion = Motion.PROJECTILE;
61:     public Hitbox hitbox = Hitbox.BOX;
62:     public int lifetime = 20;
63:     public double speed = 1.0;
64:     public boolean lockThisCast;
65:     public boolean dropThisCast;
66:     public boolean tracking;
67:     public double arcRadius = 2.0;
68:     public double arcDegrees = 120.0;
69:     public double arcHeight = 2.0;
70:     public Vec3 boxSize = new Vec3(1.0, 1.0, 1.0);
71:     public int hitInterval;
72:     /** 服务器年龄达到这个值才开始命中。0 表示生成后就能打到。 */
73:     public int hitReadyAge;
74:     public int visual;
75:     public float modelScale = 1.0F;
76:     public Vec3 start;
77:     public Vec3 anchor;
78:     public Vec3 aim;
79:     public Path path;
80:     public String functionId = "";
81:     public double length = 1.0;
82:     public double width = 1.0;
83:     public double height = 1.0;
84:     public String texture;
85:     public boolean centered;
86:     public boolean pose3d;
87:     public float roll;
88:     public HitListener listener;
89:     public HitListenerFactory listenerFactory;
90:     public float settleDamage = Float.NaN;
91:     public EnchantmentResolution settleEnchantments;
92:     public ReleaseBatch.Tally settleTally;
93:     public int settleCopyIndex;
94:     public int settleBurnSeconds;
95:     public int settleControlTicks;
96:     public boolean settleExtinguish;
97:     public float settleKnockback;
98:     public com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess settleTide =
99:             com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess.NONE;
100:     public Vec3 localOffset;
101:     public boolean trap;
102:     public boolean burstOnImpact;
103:     public boolean blocksMovement;
104:     /** 空表示仍走旧的盒子或弹道。只有声明了弧带的释放会填。 */
105:     public ArcBandDraft arcDraft;
106:     /** 这一份实体冻结后的弧。生成循环里逐份写入，不在份与份之间共用可变对象。 */
107:     public ArcBandSnapshot frozenArc;
108:     /** 这一次生成要抄到实体上的显示序号。循环内逐份换成新记录，结束后回到单份。 */
109:     public CohortVisual cohortVisual = CohortVisual.SINGLE;
110:     /** 静止冻结斩面的单次贴图。默认关闭，调用方显式打开，不在这里识别术式名字。 */
111:     public StillSheetVisual stillSheet = StillSheetVisual.NONE;
112:     /** 实体真正进入世界后通知调用方。不在这里识别术式名字。 */
113:     public Consumer<TechniqueHitEntity> publishedHook;
114: 
115:     public static final class SpawnReport {
116:         private final int requested;
117:         private final int accepted;
118: 
119:         private SpawnReport(int requested, int accepted) {
120:             this.requested = requested;
121:             this.accepted = accepted;
122:         }
123: 
124:         public int requested() {
125:             return requested;
126:         }
127: 
128:         public int accepted() {
129:             return accepted;
130:         }
131: 
132:         public boolean complete() {
133:             return requested > 0 && accepted == requested;
134:         }
135:     }
136: 
137:     public static void spawn(ServerLevel level, ServerPlayer caster, TechniqueHitSpec spec) {
138:         spawnReported(level, caster, spec);
139:     }
140: 
141:     /** 返回这一次调用里实际要求生成和真正进入世界的数量。 */
142:     public static SpawnReport spawnReported(ServerLevel level, ServerPlayer caster, TechniqueHitSpec spec) {
143:         CastFrame frame = CastFrame.current();
144:         boolean arcBand = spec.arcDraft != null;
145:         if (frame != null) {
146:             spec.tracking = frame.tracking && frame.trajectoryMutable;
147:             spec.settleDamage = frame.damage;
148:             spec.settleEnchantments = frame.enchantments;
149:             spec.settleTally = frame.tally;
150:             spec.settleCopyIndex = frame.copyIndex;
151:             float volumeScale = frame.volumeScale();
152:             ReleaseDefinition definition = ReleaseRegistry.get(spec.techniqueId);
153:             boolean scaleLength = definition == null || definition.energyScalesLength();
154:             boolean scaleWidth = definition == null || definition.energyScalesWidth();
155:             boolean scaleHeight = definition == null || definition.energyScalesHeight();
156:             boolean scaleModel = definition == null || definition.energyScalesModel();
157:             if (!arcBand && Math.abs(volumeScale - 1f) > 0.001F) {
158:                 if (scaleLength) {
159:                     spec.length *= volumeScale;
160:                 }
161:                 if (scaleWidth) {
162:                     spec.width *= volumeScale;
163:                 }
164:                 if (scaleHeight) {
165:                     spec.height *= volumeScale;
166:                 }
167:                 if (scaleModel) {
168:                     spec.modelScale *= volumeScale;
169:                 }
170:             }
171:             spec.settleBurnSeconds = frame.burnSeconds;
172:             spec.settleControlTicks = frame.controlTicks;
173:             spec.settleExtinguish = frame.extinguish;
174:             spec.settleKnockback = frame.knockback;
175:             spec.settleTide = frame.tideSuccess == null
176:                     ? com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess.NONE
177:                     : frame.tideSuccess;
178:             if (frame.blocksMovement) {
179:                 spec.blocksMovement = true;
180:             }
181:             if (frame.speedMultiplier > 1.001F) {
182:                 spec.speed *= frame.speedMultiplier;
183:             }
184:             if (!arcBand && frame.overrideGeometry) {
185:                 Path path = spec.path;
186:                 boolean still = path == Path.STILL_AIM || spec.centered || !frame.trajectoryMutable;
187:                 if (frame.trajectoryMutable && frame.pathOverride != null && frame.pathOverride != Trajectory.STRAIGHT) {
188:                     spec.dropThisCast = frame.pathOverride == Trajectory.DROP;
189:                 }
190:                 if (frame.trajectoryMutable && frame.pathOverride == Trajectory.VORTEX) {
191:                     spec.path = Path.VORTEX;
192:                     spec.start = frame.origin != null ? frame.origin : caster.getEyePosition();
193:                     spec.aim = frame.look;
194:                 } else if (frame.drop && frame.trajectoryMutable) {
195:                     spec.path = Path.DROP;
196:                     spec.start = frame.origin != null ? frame.origin : caster.getEyePosition();
197:                     spec.aim = frame.look;
198:                 } else if (path == Path.FUNCTION) {
199:                     // 不可改弹道的水帘也要吃落点和交错，只搬生成位置，不改成斩击锚点。
200:                     Vec3 locked = frame.origin != null ? frame.origin : frame.anchor;
201:                     spec.start = locked != null ? locked : caster.getEyePosition();
202:                     if (frame.look != null) {
203:                         spec.aim = frame.look;
204:                     }
205:                 } else if (still) {
206:                     spec.centered = true;
207:                     spec.anchor = frame.origin != null ? frame.origin : frame.anchor;
208:                     spec.aim = frame.planeNormal != null ? frame.planeNormal : frame.look;
209:                     spec.roll = frame.roll;
210:                     Vec3 normal = frame.planeNormal != null ? frame.planeNormal : frame.look;
211:                     spec.pose3d = frame.pose3d
212:                             || (normal != null && Math.abs(normal.y) > 0.2)
213:                             || Math.abs(frame.roll) > 1.0;
214:                 } else if (path == Path.STRAIGHT || path == Path.DROP || path == Path.FOLLOW) {
215:                     spec.start = frame.origin != null ? frame.origin : caster.getEyePosition();
216:                     if (frame.look != null) {
217:                         spec.aim = frame.look;
218:                     }
219:                 }
220:             }
221:         }
222:         if (spec.localOffset != null) {
223:             if (spec.start != null) {
224:                 spec.start = spec.start.add(spec.localOffset);
225:             } else if (spec.anchor != null) {
226:                 spec.anchor = spec.anchor.add(spec.localOffset);
227:             } else {
228:                 spec.start = caster.getEyePosition().add(spec.localOffset);
229:             }
230:         }
231:         int baseCopy = spec.settleCopyIndex;
232:         HostGroupContext hostGroup = HostGroupContext.current();
233:         int entityCount = hostGroup == null ? SkillEntityCount.entityCount() : hostGroup.spawnCount();
234:         ReleaseDefinition cohortDefinition = ReleaseRegistry.get(spec.techniqueId);
235:         boolean swordVisual = cohortDefinition != null && cohortDefinition.separatesCohortVisuals();
236:         Vec3 bakedStart = spec.start;
237:         Vec3 bakedAim = spec.aim;
238:         CastFrame placedFrame = frame;
239:         Vec3 sampledEye = arcBand ? caster.getEyePosition() : null;
240:         Vec3 sampledAim = arcBand ? caster.getLookAngle() : null;
241:         boolean planeSpread = radialPlaneSpread(spec.arcDraft, placedFrame);
242:         int accepted = 0;
243:         for (int spawnIndex = 0; spawnIndex < entityCount; spawnIndex++) {
244:             if (spec.listenerFactory != null) {
245:                 spec.listener = spec.listenerFactory.create();
246:             }
247:             spec.settleCopyIndex = CohortVisual.settleEncoding(baseCopy, spawnIndex);
248:             assignCohort(spec, placedFrame, spawnIndex, entityCount, swordVisual, planeSpread);
249:             if (placedFrame != null && placedFrame.pathOverride == Trajectory.VORTEX && placedFrame.trajectoryMutable) {
250:                 spec.path = Path.VORTEX;
251:                 spec.dropThisCast = false;
252:                 spec.start = placedFrame.anchor != null ? placedFrame.anchor : placedFrame.origin;
253:                 spec.aim = placedFrame.vortexPhase(spawnIndex);
254:             } else if (placedFrame != null && placedFrame.volumeHalf > 0.0 && placedFrame.trajectoryMutable && placedFrame.anchor != null) {
255:                 spec.start = placedFrame.volumePoint(spawnIndex);
256:                 if (spec.localOffset != null) {
257:                     spec.start = spec.start.add(spec.localOffset);
258:                 }
259:                 spec.aim = placedFrame.aimFor(spawnIndex);
260:                 if (placedFrame.drop) {
261:                     spec.path = Path.DROP;
262:                     spec.dropThisCast = true;
263:                 }
264:             }
265:             if (arcBand) {
266:                 spec.frozenArc = bakeArc(spec, placedFrame, sampledEye, sampledAim);
267:                 if (spec.frozenArc == null) {
268:                     if (hostGroup != null) {
269:                         hostGroup.fail();
270:                     }
271:                     continue;
272:                 }
273:             }
274:             TechniqueHitEntity entity = new TechniqueHitEntity(ReleaseEntities.TECHNIQUE_HIT.get(), level);
275:             if (hostGroup != null) {
276:                 entity.holdActivation();
277:             }
278:             try {
279:                 entity.applySpec(caster, spec);
280:             } catch (RuntimeException failure) {
281:                 if (hostGroup != null) {
282:                     hostGroup.fail();
283:                 }
284:                 HitPayloads.clear(entity);
285:                 WetPressure.clear(entity);
286:                 continue;
287:             }
288:             HitPayloads.attach(entity, placedFrame);
289:             ReleaseDefinition spawnedDefinition = ReleaseRegistry.get(spec.techniqueId);
290:             if (spawnedDefinition != null && spawnedDefinition.pressureWetnessMinimum() > 0 && placedFrame != null) {
291:                 WetPressure.attach(entity, placedFrame.releaseGroup, placedFrame.heavyWater,
292:                         spawnedDefinition.pressureWetnessMinimum());
293:             }
294:             boolean published = level.addFreshEntity(entity);
295:             if (!published) {
296:                 HitPayloads.clear(entity);
297:                 WetPressure.clear(entity);
298:                 if (hostGroup != null) {
299:                     hostGroup.fail();
300:                 }
301:                 continue;
302:             }
303:             accepted++;
304:             if (spec.publishedHook != null) {
305:                 spec.publishedHook.accept(entity);
306:             }
307:             if (hostGroup != null) {
308:                 hostGroup.stagePublished(entity);
309:             } else {
310:                 entity.stampStillBirth();
311:                 HeavyWater.commitFirst(caster);
312:             }
313:         }
314:         spec.settleCopyIndex = baseCopy;
315:         spec.start = bakedStart;
316:         spec.aim = bakedAim;
317:         spec.frozenArc = null;
318:         releaseCohort(spec);
319:         return new SpawnReport(entityCount, accepted);
320:     }
321: 
322:     /** 按这一帧实际采用的几何，给当前生成号做一份不可变显示记录。 */
323:     public static CohortVisual cohortFor(CastFrame frame, int spawnIndex, int spawnCount, boolean swordVisual) {
324:         return cohortFor(frame, spawnIndex, spawnCount, swordVisual, false);
325:     }
326: 
327:     /**
328:      * {@code planeSpread} 只写进这一份的视觉记录，不改共享帧。
329:      * 复制轴散开后，同一复制号里的生成倍率仍按生成号分槽。
330:      */
331:     public static CohortVisual cohortFor(CastFrame frame, int spawnIndex, int spawnCount, boolean swordVisual,
332:                                          boolean planeSpread) {
333:         int copyCount = 1;
334:         int copyIndex = 0;
335:         if (frame != null) {
336:             copyCount = frame.copyCount < 1 ? 1 : frame.copyCount;
337:             copyIndex = frame.copyIndex;
338:         }
339:         return CohortVisual.capture(copyIndex, copyCount, spawnIndex, spawnCount, swordVisual,
340:                 copyDispersed(frame) || planeSpread, spawnDispersed(frame));
341:     }
342: 
343:     /** 这一份的生成点或刀面已经按复制号分开。接触点改回同一锚点后不再算。 */
344:     public static boolean copyDispersed(CastFrame frame) {
345:         if (frame == null) {
346:             return false;
347:         }
348:         if (spawnDispersed(frame)) {
349:             return true;
350:         }
351:         return frame.cohortDispersed;
352:     }
353: 
354:     /** 只看生成循环里真正按生成号改位置或方向的分支，不从复制散开标志推断。 */
355:     public static boolean spawnDispersed(CastFrame frame) {
356:         if (frame == null) {
357:             return false;
358:         }
359:         if (frame.pathOverride == Trajectory.VORTEX && frame.trajectoryMutable) {
360:             return true;
361:         }
362:         return frame.volumeHalf > 0.0 && frame.trajectoryMutable && frame.anchor != null;
363:     }
364: 
365:     public static void assignCohort(TechniqueHitSpec spec, CastFrame frame, int spawnIndex, int spawnCount, boolean swordVisual) {
366:         assignCohort(spec, frame, spawnIndex, spawnCount, swordVisual, false);
367:     }
368: 
369:     public static void assignCohort(TechniqueHitSpec spec, CastFrame frame, int spawnIndex, int spawnCount,
370:                                     boolean swordVisual, boolean planeSpread) {
371:         spec.cohortVisual = cohortFor(frame, spawnIndex, spawnCount, swordVisual, planeSpread);
372:     }
373: 
374:     /** 交错、落点和已经改过刀面的路径保持原轨迹。只搬锚点的覆写不算已经散开。 */
375:     static boolean radialCopiesAlreadySpread(CastFrame frame) {
376:         if (frame == null) {
377:             return false;
378:         }
379:         if (copyDispersed(frame)) {
380:             return true;
381:         }
382:         return frame.pose3d || Math.abs(frame.roll) > 1.0F;
383:     }
384: 
385:     static boolean radialPlaneSpread(ArcBandDraft draft, CastFrame frame) {
386:         if (draft == null || frame == null) {
387:             return false;
388:         }
389:         return RadialCopyPlane.spreads(draft.mode(), frame.copyCount, radialCopiesAlreadySpread(frame));
390:     }
391: 
392:     public static void releaseCohort(TechniqueHitSpec spec) {
393:         spec.cohortVisual = CohortVisual.SINGLE;
394:     }
395: 
396:     /** 覆写过的原点和瞄准原样进入快照。没有覆写时，眼位和朝向只用循环外的那一次采样。 */
397:     static ArcBandSnapshot bakeArc(TechniqueHitSpec spec, CastFrame frame, Vec3 sampledEye, Vec3 sampledAim) {
398:         boolean framedOrigin = frame != null && frame.overrideGeometry && frame.origin != null;
399:         Vec3 origin = framedOrigin ? frame.origin : (spec.start != null ? spec.start : sampledEye);
400:         if (origin == null) {
401:             return null;
402:         }
403:         if (framedOrigin && spec.localOffset != null) {
404:             origin = origin.add(spec.localOffset);
405:         }
406:         boolean scatterFacing = frame != null && (frame.pose3d || Math.abs(frame.roll) > 1.0F);
407:         Vec3 facing = scatterFacing && frame.planeNormal != null && frame.planeNormal.lengthSqr() > 1.0E-8
408:                 ? frame.planeNormal
409:                 : (frame != null && frame.look != null && frame.look.lengthSqr() > 1.0E-8 ? frame.look : spec.aim);
410:         if (facing == null || facing.lengthSqr() <= 1.0E-8) {
411:             facing = sampledAim;
412:         }
413:         if (facing == null) {
414:             return null;
415:         }
416:         double extraRoll = frame == null ? 0.0 : frame.roll;
417:         double scale = frame == null ? 1.0 : frame.volumeScale();
418:         int beat = spec.arcDraft.beat();
419:         ArcVec frozenOrigin = new ArcVec(origin.x, origin.y, origin.z);
420:         ArcVec frozenFacing = new ArcVec(facing.x, facing.y, facing.z);
421:         if (frame != null && frame.copyIndex > 0 && radialPlaneSpread(spec.arcDraft, frame)) {
422:             RadialCopyPlane.Placement placement = RadialCopyPlane.place(frame.copyIndex, frame.copyCount, beat, extraRoll);
423:             return ArcBandSnapshot.freezeLook(
424:                     frozenOrigin,
425:                     frozenFacing,
426:                     null,
427:                     false,
428:                     placement.extraRollDegrees(beat),
429:                     beat,
430:                     spec.arcDraft.baseRadius(),
431:                     spec.arcDraft.baseHalfWidth(),
432:                     scale,
433:                     spec.arcDraft.mode(),
434:                     placement.sectorStartDegrees(),
435:                     placement.sectorSweepDegrees());
436:         }
437:         return ArcBandSnapshot.freezeLook(
438:                 frozenOrigin,
439:                 frozenFacing,
440:                 null,
441:                 false,
442:                 extraRoll,
443:                 beat,
444:                 spec.arcDraft.baseRadius(),
445:                 spec.arcDraft.baseHalfWidth(),
446:                 scale,
447:                 spec.arcDraft.mode());
448:     }
449: }
450: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/WeepingCurtain.java">
1: package com.bslx.XueXiMod.shu.technique.release;
2: 
3: import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
4: import net.minecraft.core.BlockPos;
5: import net.minecraft.core.particles.ParticleTypes;
6: import net.minecraft.server.level.ServerLevel;
7: import net.minecraft.util.Mth;
8: import net.minecraft.world.level.block.state.BlockState;
9: import net.minecraft.world.phys.AABB;
10: import net.minecraft.world.phys.Vec3;
11: import net.minecraft.world.phys.shapes.CollisionContext;
12: import net.minecraft.world.phys.shapes.VoxelShape;
13: 
14: import java.util.ArrayList;
15: import java.util.Collections;
16: import java.util.List;
17: import java.util.Map;
18: import java.util.WeakHashMap;
19: 
20: /**
21:  * 垂丝这一帘水。唇口固定在源点正上方，整段沿世界竖直轴下落。
22:  * 起流从上沿向下长，满流保持整段，排空时上沿往下收。本拍结果留给 {@link #active}。
23:  */
24: public final class WeepingCurtain {
25:     public static final String FUNCTION_ID = "water_weeping";
26:     /** 出水口在释放点上方。水从这里往下倒。 */
27:     public static final double LIP_LIFT = 4.0;
28:     /**
29:      * 足迹覆盖到的方块格上限。超出就整段留空并记下这个边界，不把采样间距拉大。
30:      */
31:     public static final int FOOTPRINT_CELL_BUDGET = 4096;
32: 
33:     private static final double REFERENCE_WIDTH = 0.5;
34:     private static final double BASE_THICK = 0.35;
35:     private static final double NARROW_SCALE = 0.2;
36:     private static final double PLANE_SLOP = 1.0E-4;
37:     private static final double SHAPE_SLOP = 1.0E-4;
38:     private static final double EDGE_SLOP = 1.0E-7;
39: 
40:     /** 弱键只留水段值，不把世界留在缓存里。 */
41:     private static final Map<TechniqueHitEntity, WaterSegment> CACHED_SEGMENTS =
42:             Collections.synchronizedMap(new WeakHashMap<>());
43:     private static volatile long notedCells;
44:     private static volatile boolean notedOverBudget;
45: 
46:     private WeepingCurtain() {
47:     }
48: 
49:     public static void register() {
50:         TrajectorySampler.registerVolume(FUNCTION_ID, WeepingCurtain::fit);
51:     }
52: 
53:     /**
54:      * 按这一拍的实体源点和朝向写下水段。
55:      * {@code groundPoint} 是旧弹道在释放点正下方的射线，实体这时已经被挪走，不用它改足端。
56:      */
57:     public static void fit(TechniqueHitEntity entity, ServerLevel level, Vec3 groundPoint) {
58:         if (entity == null) {
59:             return;
60:         }
61:         Vec3 lip = lipOf(entity.origin());
62:         double across = sheetAcross(entity.width());
63:         double thick = sheetThick(entity.width());
64:         FootClip clip = probeTerrain(level, lip, entity.forward(), across, thick);
65:         WaterSegment segment = segmentFor(clip, lip, entity.forward(), across, thick, entity.age());
66:         CACHED_SEGMENTS.put(entity, segment);
67:         entity.placeAt(segment.center(), segment.ahead());
68:         entity.resizeVolume(segment.thick(), segment.across(), segment.activeHeight());
69:         if (level != null && splashes(entity.age(), clip, segment)) {
70:             Vec3 splashAt = footPoint(segment);
71:             level.sendParticles(ParticleTypes.SPLASH, splashAt.x, splashAt.y, splashAt.z, 6, 0.15, 0.05, 0.15, 0.02);
72:         }
73:     }
74: 
75:     /** 返回本拍 {@link #fit} 已经算好的水段。没有缓存时给有限空段，不再探地。 */
76:     public static WaterSegment active(TechniqueHitEntity entity) {
77:         if (entity == null) {
78:             return WaterSegment.EMPTY;
79:         }
80:         WaterSegment cached = CACHED_SEGMENTS.get(entity);
81:         if (cached == null) {
82:             return WaterSegment.EMPTY;
83:         }
84:         return cached;
85:     }
86: 
87:     /** 实体移除时丢掉缓存。 */
88:     public static void forget(TechniqueHitEntity entity) {
89:         if (entity == null) {
90:             return;
91:         }
92:         CACHED_SEGMENTS.remove(entity);
93:     }
94: 
95:     public static Vec3 lipOf(Vec3 origin) {
96:         if (origin == null || !Double.isFinite(origin.x) || !Double.isFinite(origin.y) || !Double.isFinite(origin.z)) {
97:             return new Vec3(0.0, LIP_LIFT, 0.0);
98:         }
99:         return new Vec3(origin.x, origin.y + LIP_LIFT, origin.z);
100:     }
101: 
102:     /** 当前实体宽度，含聚能。非法宽度回到基准 0.5。 */
103:     public static double sheetAcross(double entityWidth) {
104:         if (!Double.isFinite(entityWidth) || entityWidth <= 0.0) {
105:             return REFERENCE_WIDTH;
106:         }
107:         return entityWidth;
108:     }
109: 
110:     /** 厚度仍按宽度相对 0.5 缩放。过窄或非法时用基准厚度，不把宽度一起改掉。 */
111:     public static double sheetThick(double entityWidth) {
112:         double crossScale = entityWidth / REFERENCE_WIDTH;
113:         if (!Double.isFinite(crossScale) || crossScale < NARROW_SCALE) {
114:             crossScale = 1.0;
115:         }
116:         return BASE_THICK * crossScale;
117:     }
118: 
119:     /** 已知足端后的相位。不探地。 */
120:     public static WaterSegment flowSegment(Vec3 lip, Vec3 look, double footDrop, boolean floorFoot,
121:                                            double across, double thick, int age) {
122:         Vec3 mouth = lip == null ? Vec3.ZERO : lip;
123:         int fillTicks = WeepingSettings.FILL_TICKS;
124:         if (fillTicks < 1) {
125:             fillTicks = 1;
126:         }
127:         int lifeTicks = WeepingSettings.LIFETIME;
128:         int drainStart = WeepingSettings.DRAIN_START;
129:         int drainWindow = lifeTicks - drainStart;
130:         if (drainWindow < 1) {
131:             drainWindow = 1;
132:         }
133:         int lived = Math.max(age, 0);
134:         double head = lived >= fillTicks ? 1.0 : lived / (double) fillTicks;
135:         double tail = lived <= drainStart ? 0.0 : (lived - drainStart) / (double) drainWindow;
136:         if (tail > 1.0) {
137:             tail = 1.0;
138:         }
139:         if (tail > head) {
140:             tail = head;
141:         }
142:         double safeFoot = footDrop;
143:         if (!Double.isFinite(safeFoot) || safeFoot < 0.0) {
144:             safeFoot = 0.0;
145:         }
146:         double upperDrop = safeFoot * tail;
147:         double lowerDrop = safeFoot * head;
148:         double openSpan = lowerDrop - upperDrop;
149:         boolean flowing = openSpan > WaterSegment.PRESENT_GAP;
150:         double middleDrop = (upperDrop + lowerDrop) * 0.5;
151:         Vec3 center = new Vec3(mouth.x, mouth.y - middleDrop, mouth.z);
152:         return new WaterSegment(mouth, center, look, upperDrop, lowerDrop, safeFoot, across, thick, flowing, floorFoot);
153:     }
154: 
155:     /** 源被堵住或预算不够时不给水体。其余走相位。 */
156:     public static WaterSegment segmentFor(FootClip clip, Vec3 lip, Vec3 look, double across, double thick, int age) {
157:         if (clip == null || clip.sourceInside() || clip.overBudget() || !Double.isFinite(clip.footDrop())) {
158:             return WaterSegment.absent(lip, look, across, thick);
159:         }
160:         return flowSegment(lip, look, clip.footDrop(), clip.floorFoot(), across, thick, age);
161:     }
162: 
163:     /** 只有水头到达真实朝上地面时才溅水。 */
164:     public static boolean splashes(int age, FootClip clip, WaterSegment segment) {
165:         return clip != null
166:                 && segment != null
167:                 && !clip.sourceInside()
168:                 && !clip.overBudget()
169:                 && clip.floorFoot()
170:                 && segment.present()
171:                 && age == WeepingSettings.FILL_TICKS;
172:     }
173: 
174:     /** 足端在唇口正下方，不往前倾。 */
175:     public static Vec3 footPoint(WaterSegment segment) {
176:         if (segment == null) {
177:             return Vec3.ZERO;
178:         }
179:         return new Vec3(segment.lip().x, segment.lip().y - segment.footDrop(), segment.lip().z);
180:     }
181: 
182:     public static long footprintCellCount(Vec3 lip, Vec3 look, double across, double thick) {
183:         PreparedColumn prepared = prepare(lip, look, across, thick);
184:         if (prepared == null) {
185:             return 0L;
186:         }
187:         return prepared.cellCount();
188:     }
189: 
190:     public static long notedFootprintCells() {
191:         return notedCells;
192:     }
193: 
194:     public static boolean footprintBudgetRejected() {
195:         return notedOverBudget;
196:     }
197: 
198:     /** 用已经取到的碰撞盒裁切。盒子要是世界坐标。超预算时不读这些盒子。 */
199:     public static FootClip clipFootprint(Vec3 lip, Vec3 look, double across, double thick, List<AABB> boxes) {
200:         PreparedColumn prepared = prepare(lip, look, across, thick);
201:         if (prepared == null) {
202:             noteFootprint(0L, true);
203:             return FootClip.budgetStop(0L);
204:         }
205:         if (prepared.cellCount() > FOOTPRINT_CELL_BUDGET) {
206:             noteFootprint(prepared.cellCount(), true);
207:             return FootClip.budgetStop(prepared.cellCount());
208:         }
209:         noteFootprint(prepared.cellCount(), false);
210:         return clipBoxes(prepared, boxes);
211:     }
212: 
213:     /**
214:      * 从世界读取碰撞形状后再裁切。形状以方块自己的格为原点，这里平移一次。
215:      * 只读方块，不改地形。没有世界或格子超预算时不扫描。
216:      */
217:     public static FootClip probeTerrain(ServerLevel level, Vec3 lip, Vec3 look, double across, double thick) {
218:         PreparedColumn prepared = prepare(lip, look, across, thick);
219:         if (prepared == null || level == null || prepared.cellCount() > FOOTPRINT_CELL_BUDGET) {
220:             long cellCount = prepared == null ? 0L : prepared.cellCount();
221:             noteFootprint(cellCount, true);
222:             return FootClip.budgetStop(cellCount);
223:         }
224:         noteFootprint(prepared.cellCount(), false);
225:         return clipBoxes(prepared, gatherTerrain(level, prepared.range()));
226:     }
227: 
228:     private static PreparedColumn prepare(Vec3 lip, Vec3 look, double across, double thick) {
229:         double limit = WeepingSettings.FALL_DISTANCE_LIMIT;
230:         if (!Double.isFinite(limit) || limit <= 0.0) {
231:             return null;
232:         }
233:         Vec3 mouth = lip;
234:         if (mouth == null || !Double.isFinite(mouth.x) || !Double.isFinite(mouth.y) || !Double.isFinite(mouth.z)) {
235:             mouth = Vec3.ZERO;
236:         }
237:         double fullAcross = across;
238:         double fullThick = thick;
239:         if (!Double.isFinite(fullAcross) || fullAcross < 0.0) {
240:             fullAcross = 0.0;
241:         }
242:         if (!Double.isFinite(fullThick) || fullThick < 0.0) {
243:             fullThick = 0.0;
244:         }
245:         FlatFrame frame = FlatFrame.of(look);
246:         AABB bounds = columnBounds(mouth, frame, fullAcross * 0.5, fullThick * 0.5, limit);
247:         BlockRange range = BlockRange.of(bounds);
248:         return new PreparedColumn(mouth, frame, fullAcross * 0.5, fullThick * 0.5, limit, range, range.cellCount());
249:     }
250: 
251:     private static FootClip clipBoxes(PreparedColumn prepared, List<AABB> boxes) {
252:         if (boxes == null || boxes.isEmpty()) {
253:             return FootClip.openAir(prepared.dropLimit(), prepared.cellCount());
254:         }
255:         boolean buried = false;
256:         boolean foundTop = false;
257:         double highestTop = Double.NEGATIVE_INFINITY;
258:         for (AABB box : boxes) {
259:             if (!boxUsable(box) || !overlapsFootprint(prepared, box)) {
260:                 continue;
261:             }
262:             if (box.minY < prepared.mouth().y - PLANE_SLOP && box.maxY > prepared.mouth().y + PLANE_SLOP) {
263:                 buried = true;
264:                 break;
265:             }
266:             if (box.maxY <= prepared.mouth().y + PLANE_SLOP
267:                     && box.maxY > prepared.mouth().y - prepared.dropLimit() + PLANE_SLOP) {
268:                 if (!foundTop || box.maxY > highestTop) {
269:                     highestTop = box.maxY;
270:                     foundTop = true;
271:                 }
272:             }
273:         }
274:         if (buried) {
275:             return FootClip.blockedSource(prepared.cellCount());
276:         }
277:         if (!foundTop) {
278:             return FootClip.openAir(prepared.dropLimit(), prepared.cellCount());
279:         }
280:         double drop = prepared.mouth().y - highestTop;
281:         if (!Double.isFinite(drop) || drop < 0.0) {
282:             drop = 0.0;
283:         }
284:         if (drop > prepared.dropLimit()) {
285:             return FootClip.openAir(prepared.dropLimit(), prepared.cellCount());
286:         }
287:         return new FootClip(drop, true, false, false, prepared.cellCount());
288:     }
289: 
290:     private static boolean overlapsFootprint(PreparedColumn prepared, AABB box) {
291:         Vec3 ahead = prepared.frame().ahead();
292:         Vec3 right = prepared.frame().right();
293:         double centerX = (box.minX + box.maxX) * 0.5;
294:         double centerZ = (box.minZ + box.maxZ) * 0.5;
295:         double halfBoxX = (box.maxX - box.minX) * 0.5;
296:         double halfBoxZ = (box.maxZ - box.minZ) * 0.5;
297:         double offsetX = centerX - prepared.mouth().x;
298:         double offsetZ = centerZ - prepared.mouth().z;
299:         double curtainRadiusX = Math.abs(ahead.x) * prepared.halfThick() + Math.abs(right.x) * prepared.halfAcross();
300:         if (Math.abs(offsetX) > halfBoxX + curtainRadiusX - SHAPE_SLOP) {
301:             return false;
302:         }
303:         double curtainRadiusZ = Math.abs(ahead.z) * prepared.halfThick() + Math.abs(right.z) * prepared.halfAcross();
304:         if (Math.abs(offsetZ) > halfBoxZ + curtainRadiusZ - SHAPE_SLOP) {
305:             return false;
306:         }
307:         double aheadOffset = offsetX * ahead.x + offsetZ * ahead.z;
308:         double boxOnAhead = halfBoxX * Math.abs(ahead.x) + halfBoxZ * Math.abs(ahead.z);
309:         if (Math.abs(aheadOffset) > prepared.halfThick() + boxOnAhead - SHAPE_SLOP) {
310:             return false;
311:         }
312:         double rightOffset = offsetX * right.x + offsetZ * right.z;
313:         double boxOnRight = halfBoxX * Math.abs(right.x) + halfBoxZ * Math.abs(right.z);
314:         if (Math.abs(rightOffset) > prepared.halfAcross() + boxOnRight - SHAPE_SLOP) {
315:             return false;
316:         }
317:         return true;
318:     }
319: 
320:     private static List<AABB> gatherTerrain(ServerLevel level, BlockRange range) {
321:         List<AABB> gathered = new ArrayList<>();
322:         BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
323:         CollisionContext emptyContext = CollisionContext.empty();
324:         for (int columnX = range.westEdge(); columnX <= range.eastEdge(); columnX++) {
325:             for (int layerY = range.lowEdge(); layerY <= range.highEdge(); layerY++) {
326:                 for (int columnZ = range.northEdge(); columnZ <= range.southEdge(); columnZ++) {
327:                     cursor.set(columnX, layerY, columnZ);
328:                     BlockState state = level.getBlockState(cursor);
329:                     if (state.isAir()) {
330:                         continue;
331:                     }
332:                     VoxelShape shape = state.getCollisionShape(level, cursor, emptyContext);
333:                     if (shape == null || shape.isEmpty()) {
334:                         continue;
335:                     }
336:                     VoxelShape moved = shape.move(columnX, layerY, columnZ);
337:                     for (AABB part : moved.toAabbs()) {
338:                         if (boxUsable(part)) {
339:                             gathered.add(part);
340:                         }
341:                     }
342:                 }
343:             }
344:         }
345:         return gathered;
346:     }
347: 
348:     private static AABB columnBounds(Vec3 mouth, FlatFrame frame, double halfAcross, double halfThick, double dropLimit) {
349:         Vec3 ahead = frame.ahead();
350:         Vec3 right = frame.right();
351:         double west = mouth.x;
352:         double east = mouth.x;
353:         double north = mouth.z;
354:         double south = mouth.z;
355:         double[] cornerAlong = new double[] {halfThick, halfThick, -halfThick, -halfThick};
356:         double[] cornerAcross = new double[] {halfAcross, -halfAcross, halfAcross, -halfAcross};
357:         for (int cornerIndex = 0; cornerIndex < cornerAlong.length; cornerIndex++) {
358:             double sampleX = mouth.x + ahead.x * cornerAlong[cornerIndex] + right.x * cornerAcross[cornerIndex];
359:             double sampleZ = mouth.z + ahead.z * cornerAlong[cornerIndex] + right.z * cornerAcross[cornerIndex];
360:             west = Math.min(west, sampleX);
361:             east = Math.max(east, sampleX);
362:             north = Math.min(north, sampleZ);
363:             south = Math.max(south, sampleZ);
364:         }
365:         return new AABB(west, mouth.y - dropLimit, north, east, mouth.y, south);
366:     }
367: 
368:     private static boolean boxUsable(AABB box) {
369:         return box != null
370:                 && Double.isFinite(box.minX) && Double.isFinite(box.minY) && Double.isFinite(box.minZ)
371:                 && Double.isFinite(box.maxX) && Double.isFinite(box.maxY) && Double.isFinite(box.maxZ)
372:                 && box.maxX >= box.minX && box.maxY >= box.minY && box.maxZ >= box.minZ;
373:     }
374: 
375:     private static void noteFootprint(long cellCount, boolean rejected) {
376:         notedCells = cellCount;
377:         notedOverBudget = rejected;
378:     }
379: 
380:     /**
381:      * 一列水的裁切结果。{@code floorFoot} 只在落到朝上的盒顶，并且落差小于最大落差时为真。
382:      */
383:     public record FootClip(double footDrop, boolean floorFoot, boolean sourceInside, boolean overBudget, long cellCount) {
384:         public static FootClip openAir(double footDrop, long cellCount) {
385:             double drop = footDrop;
386:             if (!Double.isFinite(drop) || drop < 0.0) {
387:                 drop = 0.0;
388:             }
389:             return new FootClip(drop, false, false, false, cellCount);
390:         }
391: 
392:         public static FootClip blockedSource(long cellCount) {
393:             return new FootClip(0.0, false, true, false, cellCount);
394:         }
395: 
396:         public static FootClip budgetStop(long cellCount) {
397:             return new FootClip(0.0, false, false, true, cellCount);
398:         }
399:     }
400: 
401:     private record FlatFrame(Vec3 ahead, Vec3 right) {
402:         private static FlatFrame of(Vec3 look) {
403:             Vec3 ahead = WaterSegment.horizontalAhead(look);
404:             return new FlatFrame(ahead, new Vec3(ahead.z, 0.0, -ahead.x));
405:         }
406:     }
407: 
408:     private record PreparedColumn(Vec3 mouth, FlatFrame frame, double halfAcross, double halfThick,
409:                                   double dropLimit, BlockRange range, long cellCount) {
410:     }
411: 
412:     private record BlockRange(int westEdge, int eastEdge, int lowEdge, int highEdge,
413:                               int northEdge, int southEdge, long cellCount) {
414:         private static BlockRange of(AABB bounds) {
415:             int westEdge = Mth.floor(bounds.minX);
416:             int eastEdge = Mth.floor(bounds.maxX - EDGE_SLOP);
417:             int lowEdge = Mth.floor(bounds.minY);
418:             int highEdge = Mth.floor(bounds.maxY - EDGE_SLOP);
419:             int northEdge = Mth.floor(bounds.minZ);
420:             int southEdge = Mth.floor(bounds.maxZ - EDGE_SLOP);
421:             if (eastEdge < westEdge) {
422:                 eastEdge = westEdge;
423:             }
424:             if (highEdge < lowEdge) {
425:                 highEdge = lowEdge;
426:             }
427:             if (southEdge < northEdge) {
428:                 southEdge = northEdge;
429:             }
430:             long spanX = (long) eastEdge - westEdge + 1L;
431:             long spanY = (long) highEdge - lowEdge + 1L;
432:             long spanZ = (long) southEdge - northEdge + 1L;
433:             long cellCount;
434:             if (spanX > FOOTPRINT_CELL_BUDGET || spanY > FOOTPRINT_CELL_BUDGET || spanZ > FOOTPRINT_CELL_BUDGET) {
435:                 cellCount = (long) FOOTPRINT_CELL_BUDGET + 1L;
436:             } else {
437:                 cellCount = spanX * spanY * spanZ;
438:             }
439:             return new BlockRange(westEdge, eastEdge, lowEdge, highEdge, northEdge, southEdge, cellCount);
440:         }
441:     }
442: }
443: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/TechniqueCastContext.java">
1: package com.bslx.XueXiMod.shu.technique;
2: 
3: import net.minecraft.world.phys.Vec3;
4: 
5: import java.util.function.Consumer;
6: 
7: public class TechniqueCastContext {
8:     private final float damage;
9:     private final Vec3 look;
10:     private final Vec3 position;
11:     private final EnchantmentResolution enchantments;
12:     private final Consumer<Vec3> onFirstTrigger;
13:     private final boolean lockThisCast;
14:     private final boolean dropThisCast;
15:     private final ActionWindow.Sink actionSink;
16: 
17:     public TechniqueCastContext(float damage, Vec3 look, Vec3 position) {
18:         this(damage, look, position, EnchantmentResolution.empty(), null, false, false, null);
19:     }
20: 
21:     public TechniqueCastContext(float damage, Vec3 look, Vec3 position, EnchantmentResolution enchantments) {
22:         this(damage, look, position, enchantments, null, false, false, null);
23:     }
24: 
25:     public TechniqueCastContext(float damage, Vec3 look, Vec3 position, EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger) {
26:         this(damage, look, position, enchantments, onFirstTrigger, false, false, null);
27:     }
28: 
29:     public TechniqueCastContext(float damage, Vec3 look, Vec3 position, EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger, boolean lockThisCast, boolean dropThisCast) {
30:         this(damage, look, position, enchantments, onFirstTrigger, lockThisCast, dropThisCast, null);
31:     }
32: 
33:     public TechniqueCastContext(float damage, Vec3 look, Vec3 position, EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger, boolean lockThisCast, boolean dropThisCast, ActionWindow.Sink actionSink) {
34:         this.damage = damage;
35:         this.look = look;
36:         this.position = position;
37:         this.enchantments = enchantments;
38:         this.onFirstTrigger = onFirstTrigger;
39:         this.lockThisCast = lockThisCast;
40:         this.dropThisCast = dropThisCast;
41:         this.actionSink = actionSink;
42:     }
43: 
44:     public boolean lockThisCast() {
45:         return lockThisCast;
46:     }
47: 
48:     public boolean dropThisCast() {
49:         return dropThisCast;
50:     }
51: 
52:     public float getDamage() {
53:         return damage;
54:     }
55: 
56:     public Vec3 getLook() {
57:         return look;
58:     }
59: 
60:     public Vec3 getPos() {
61:         return position;
62:     }
63: 
64:     public EnchantmentResolution enchantments() {
65:         return enchantments;
66:     }
67: 
68:     public Consumer<Vec3> onFirstTrigger() {
69:         return onFirstTrigger;
70:     }
71: 
72:     public ActionWindow.Sink actionSink() {
73:         return actionSink;
74:     }
75: }
76: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/network/SkillAssemblyPacket.java">
1: package com.bslx.XueXiMod.network;
2: 
3: import com.bslx.XueXiMod.gui.SkillAssemblyMenu;
4: import com.bslx.XueXiMod.item.SkillCarrier;
5: import com.bslx.XueXiMod.item.SkillStorageData;
6: import com.bslx.XueXiMod.item.SkillStorageItem;
7: import com.bslx.XueXiMod.item.StaffItem;
8: import com.bslx.XueXiMod.item.StaffSkills;
9: import com.bslx.XueXiMod.player.PlayerBoundSkills;
10: import com.bslx.XueXiMod.player.PlayerShuData;
11: import com.bslx.XueXiMod.skill.Skill;
12: import com.bslx.XueXiMod.skill.SkillType;
13: import com.bslx.XueXiMod.shu.ShuAttribute;
14: import com.bslx.XueXiMod.shu.Technique;
15: import com.bslx.XueXiMod.shu.TechniqueManager;
16: import com.bslx.XueXiMod.shu.TechniqueType;
17: import net.minecraft.network.FriendlyByteBuf;
18: import net.minecraft.network.chat.Component;
19: import net.minecraft.server.level.ServerPlayer;
20: import net.minecraft.world.InteractionHand;
21: import net.minecraft.world.MenuProvider;
22: import net.minecraft.world.entity.player.Inventory;
23: import net.minecraft.world.entity.player.Player;
24: import net.minecraft.world.inventory.AbstractContainerMenu;
25: import net.minecraft.world.item.ItemStack;
26: import net.minecraftforge.network.NetworkHooks;
27: import net.minecraftforge.network.NetworkEvent;
28: import net.minecraftforge.network.simple.SimpleChannel;
29: 
30: import java.util.ArrayList;
31: import java.util.List;
32: import java.util.function.Supplier;
33: 
34: public class SkillAssemblyPacket {
35: 
36:     public static final int ACTION_OPEN = 0;
37:     public static final int ACTION_PUT = 1;
38:     public static final int ACTION_REMOVE = 2;
39:     public static final int ACTION_MOVE_LEFT = 3;
40:     public static final int ACTION_MOVE_RIGHT = 4;
41: 
42:     private final int action;
43:     private final int slot;
44:     private final int storageIndex;
45:     private final int techniqueIndex;
46: 
47:     public SkillAssemblyPacket(int action, int slot, int storageIndex, int techniqueIndex) {
48:         this.action = action;
49:         this.slot = slot;
50:         this.storageIndex = storageIndex;
51:         this.techniqueIndex = techniqueIndex;
52:     }
53: 
54:     public void encode(FriendlyByteBuf buf) {
55:         buf.writeInt(action);
56:         buf.writeInt(slot);
57:         buf.writeInt(storageIndex);
58:         buf.writeInt(techniqueIndex);
59:     }
60: 
61:     public static SkillAssemblyPacket decode(FriendlyByteBuf buf) {
62:         return new SkillAssemblyPacket(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
63:     }
64: 
65:     public void handle(Supplier<NetworkEvent.Context> context) {
66:         context.get().enqueueWork(() -> {
67:             ServerPlayer player = context.get().getSender();
68:             if (player == null) return;
69:             switch (action) {
70:                 case ACTION_OPEN -> open(player);
71:                 case ACTION_PUT -> put(player);
72:                 case ACTION_REMOVE -> remove(player);
73:                 case ACTION_MOVE_LEFT -> move(player, -1);
74:                 case ACTION_MOVE_RIGHT -> move(player, 1);
75:                 default -> { }
76:             }
77:         });
78:         context.get().setPacketHandled(true);
79:     }
80: 
81:     private void open(ServerPlayer player) {
82:         ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
83:         if (!SkillCarrier.canRelease(mainHand)) return;
84:         if (!StaffItem.holds(mainHand)) {
85:             BoundSkillSyncPacket.send(player);
86:         }
87:         boolean holdingStaff = StaffItem.holds(mainHand);
88:         NetworkHooks.openScreen(player, new MenuProvider() {
89:             @Override
90:             public Component getDisplayName() {
91:                 return Component.literal(holdingStaff ? "法杖装配" : "剑技装配");
92:             }
93: 
94:             @Override
95:             public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
96:                 return new com.bslx.XueXiMod.gui.SpellAssemblyMenu(containerId, playerInv);
97:             }
98:         });
99:     }
100: 
101:     private void put(ServerPlayer player) {
102:         ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
103:         if (!SkillCarrier.canRelease(mainHand)) return;
104:         boolean staff = StaffItem.holds(mainHand);
105: 
106:         ItemStack storage = SkillStorageItem.find(player);
107:         if (storage.isEmpty()) return;
108:         List<SkillStorageData.Entry> entries = SkillStorageData.entries(storage);
109:         if (storageIndex < 0 || storageIndex >= entries.size()) return;
110: 
111:         SkillStorageData.Entry entry = entries.get(storageIndex);
112:         String spellId = entry.spellId();
113:         String embryoId = entry.embryoId();
114:         Technique technique = TechniqueManager.getById(spellId);
115:         if (technique == null) return;
116: 
117:         if (staff) {
118:             if (technique.getType() == TechniqueType.SWORD) {
119:                 player.displayClientMessage(Component.literal("无法装配：剑术不能装入法杖"), false);
120:                 return;
121:             }
122:             if (StaffSkills.count(mainHand) >= ((StaffItem) mainHand.getItem()).data().slotCount()) {
123:                 player.displayClientMessage(Component.literal("无法装配：法杖槽位已满"), false);
124:                 return;
125:             }
126:         } else {
127:             if (slot < 0 || slot >= 4) return;
128:             if (technique.getType() == TechniqueType.MAGIC && !SkillCarrier.allowsMagic(mainHand)) {
129:                 player.displayClientMessage(Component.literal("无法装配：法术不能装入剑技槽"), false);
130:                 return;
131:             }
132: 
133:             int swordLevel = PlayerShuData.get(player).getSwordLevel();
134:             int capacity = 10 + (Math.max(swordLevel, 1) - 1) * 5;
135:             if (assemblyCostOfSlot(player, slot) + technique.assemblyCost() > capacity) {
136:                 player.displayClientMessage(Component.literal("无法装配：超出剑技容量"), false);
137:                 return;
138:             }
139:             Skill existing = PlayerBoundSkills.get(player, slot);
140:             int chainLength = existing == null ? 0 : existing.getTechniques().size();
141:             if (chainLength >= com.bslx.XueXiMod.skill.Skill.CHAIN_LIMIT) {
142:                 player.displayClientMessage(Component.literal("无法装配：一条剑技最多 8 段"), false);
143:                 return;
144:             }
145:         }
146: 
147:         if (!staff) {
148:             Skill existing = PlayerBoundSkills.get(player, slot);
149:             List<Technique> proposed = new ArrayList<>();
150:             if (existing != null) proposed.addAll(existing.getTechniques());
151:             proposed.add(technique);
152:             if (SkillSegmentMath.refusesMixedChain(proposed)) {
153:                 player.displayClientMessage(Component.literal("无法装配：操纵术式不能和其他术式装配"), false);
154:                 return;
155:             }
156:         }
157: 
158:         if (!SkillStorageData.consume(storage, spellId, embryoId, 1)) {
159:             player.displayClientMessage(Component.literal("无法装配：匣子里没有这个术式"), false);
160:             return;
161:         }
162:         if (staff) {
163:             StaffSkills.add(mainHand, spellId, ShuAttribute.fromId(embryoId));
164:         } else {
165:             appendToSlot(player, slot, technique, embryoId);
166:             BoundSkillSyncPacket.send(player);
167:         }
168:         syncInventory(player);
169:     }
170: 
171:     private void remove(ServerPlayer player) {
172:         ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
173:         if (!SkillCarrier.canRelease(mainHand)) return;
174:         boolean staff = StaffItem.holds(mainHand);
175:         if (staff) {
176:             removeFromStaff(player, mainHand);
177:             return;
178:         }
179: 
180:         if (slot < 0 || slot >= 4) return;
181:         Skill skill = PlayerBoundSkills.get(player, slot);
182:         if (skill == null) return;
183:         if (techniqueIndex < 0 || techniqueIndex >= skill.getTechniques().size()) return;
184: 
185:         ItemStack storage = SkillStorageItem.getOrCreate(player);
186:         if (storage.isEmpty()) {
187:             player.displayClientMessage(Component.literal("背包已满，无法收入术式匣"), false);
188:             return;
189:         }
190: 
191:         Technique removed = skill.getTechniques().remove(techniqueIndex);
192:         ShuAttribute embryo = skill.getEmbryos().size() > techniqueIndex
193:                 ? skill.getEmbryos().remove(techniqueIndex) : ShuAttribute.NONE;
194: 
195:         SkillStorageData.add(storage, removed.getId(), embryo.getId(), 1);
196: 
197:         if (skill.getTechniques().isEmpty()) {
198:             PlayerBoundSkills.set(player, slot, null);
199:         } else {
200:             PlayerBoundSkills.set(player, slot, skill);
201:         }
202:         BoundSkillSyncPacket.send(player);
203:         syncInventory(player);
204:     }
205: 
206:     private void removeFromStaff(ServerPlayer player, ItemStack mainHand) {
207:         if (slot < 0 || slot >= StaffSkills.count(mainHand)) return;
208:         ItemStack storage = SkillStorageItem.getOrCreate(player);
209:         if (storage.isEmpty()) {
210:             player.displayClientMessage(Component.literal("背包已满，无法收入术式匣"), false);
211:             return;
212:         }
213:         var entry = StaffSkills.entries(mainHand).get(slot);
214:         SkillStorageData.add(storage, entry.techniqueId,
215:                 entry.embryo != null ? entry.embryo.getId() : ShuAttribute.NONE.getId(), 1);
216:         StaffSkills.remove(mainHand, slot);
217:         syncInventory(player);
218:     }
219: 
220:     private void move(ServerPlayer player, int direction) {
221:         ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
222:         if (!SkillCarrier.canRelease(mainHand)) return;
223:         boolean staff = StaffItem.holds(mainHand);
224:         if (staff) {
225:             int index = slot;
226:             int target = index + direction;
227:             if (index < 0 || index >= StaffSkills.count(mainHand)
228:                     || target < 0 || target >= StaffSkills.count(mainHand)) return;
229:             StaffSkills.move(mainHand, index, direction);
230:             syncInventory(player);
231:             return;
232:         }
233: 
234:         if (slot < 0 || slot >= 4) return;
235:         Skill skill = PlayerBoundSkills.get(player, slot);
236:         if (skill == null) return;
237: 
238:         List<Technique> techs = skill.getTechniques();
239:         List<ShuAttribute> embryos = skill.getEmbryos();
240:         int fromIndex = techniqueIndex;
241:         int toIndex = fromIndex + direction;
242:         if (fromIndex < 0 || fromIndex >= techs.size() || toIndex < 0 || toIndex >= techs.size() || toIndex >= embryos.size()) return;
243: 
244:         java.util.Collections.swap(techs, fromIndex, toIndex);
245:         java.util.Collections.swap(embryos, fromIndex, toIndex);
246:         PlayerBoundSkills.set(player, slot, skill);
247:         BoundSkillSyncPacket.send(player);
248:     }
249: 
250:     private void appendToSlot(ServerPlayer player, int slot, Technique technique, String embryoId) {
251:         Skill skill = PlayerBoundSkills.get(player, slot);
252:         if (skill == null) {
253:             skill = new Skill(technique.getName(), SkillType.SWORD, new ArrayList<>());
254:         }
255:         skill.getTechniques().add(technique);
256:         skill.getEmbryos().add(ShuAttribute.fromId(embryoId));
257:         PlayerBoundSkills.set(player, slot, skill);
258:     }
259: 
260:     private int assemblyCostOfSlot(ServerPlayer player, int slot) {
261:         Skill skill = PlayerBoundSkills.get(player, slot);
262:         if (skill == null) return 0;
263:         int sum = 0;
264:         for (Technique technique : skill.getTechniques()) {
265:             sum += technique.assemblyCost();
266:         }
267:         return sum;
268:     }
269: 
270:     private static void syncInventory(ServerPlayer player) {
271:         player.getInventory().setChanged();
272:         player.inventoryMenu.broadcastChanges();
273:     }
274: 
275:     public static void register(SimpleChannel channel, int messageIndex) {
276:         channel.registerMessage(messageIndex,
277:                 SkillAssemblyPacket.class,
278:                 SkillAssemblyPacket::encode,
279:                 SkillAssemblyPacket::decode,
280:                 SkillAssemblyPacket::handle);
281:     }
282: 
283:     public static class Registration implements NetworkPacket {
284:         @Override
285:         public void register(SimpleChannel channel, int messageIndex) {
286:             SkillAssemblyPacket.register(channel, messageIndex);
287:         }
288:     }
289: }
290: 
</source_file>

<partial_source path="src/main/java/com/bslx/XueXiMod/gui/SkillAssemblyScreen.java" selected="matching params and 10 adjacent lines; omissions explicit">
6: import com.bslx.XueXiMod.item.StaffItem;
7: import com.bslx.XueXiMod.item.StaffSkills;
8: import com.bslx.XueXiMod.network.BoundSkillSyncPacket;
9: import com.bslx.XueXiMod.network.ModNetwork;
10: import com.bslx.XueXiMod.network.SkillAssemblyPacket;
11: import com.bslx.XueXiMod.network.ShuSyncPacket;
12: import com.bslx.XueXiMod.skill.Skill;
13: import com.bslx.XueXiMod.shu.ShuAttribute;
14: import com.bslx.XueXiMod.shu.Technique;
15: import com.bslx.XueXiMod.shu.TechniqueManager;
16: import com.bslx.XueXiMod.shu.TechniqueType;
17: import net.minecraft.client.Minecraft;
18: import net.minecraft.client.gui.GuiGraphics;
19: import net.minecraft.client.gui.components.Button;
20: import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
21: import net.minecraft.resources.ResourceLocation;
22: import net.minecraft.network.chat.Component;
23: import net.minecraft.world.entity.player.Inventory;
24: import net.minecraft.world.item.ItemStack;
25: 
26: import java.util.List;
...OMITTED...
69: 
70:     private int slotCount = 4;
71:     private int currentSlot = 0;
72:     private int selectedStorage = -1;
73:     private int selectedTech = -1;
74:     private int detailMode = 0;
75:     private int listScroll = 0;
76: 
77:     public SkillAssemblyScreen(SkillAssemblyMenu menu, Inventory inv, Component title) {
78:         super(menu, inv, title);
79:         this.imageWidth = 256;
80:         this.imageHeight = 196;
81:     }
82: 
83:     @Override
84:     protected void init() {
85:         this.slotCount = computeSlotCount();
86:         if (this.slotCount > 4) {
87:             this.imageHeight = 200;
88:         }
89:         if (isStaffMode()) {
90:             this.imageWidth = Math.max(256, STAFF_GRID_X + this.slotCount * (CELL + CELL_GAP) + STAFF_GRID_MARGIN);
91:         }
92:         super.init();
93:         int screenX = leftPos;
94:         int screenY = topPos;
95:         int buttonY = this.slotCount > 4 ? 174 : BUTTON_Y;
96:         addRenderableWidget(Button.builder(Component.literal("放入当前槽"), ignored -> put())
97:                 .pos(screenX + 6, screenY + buttonY).size(70, 20).build());
98:         addRenderableWidget(Button.builder(Component.literal("取出"), ignored -> remove())
99:                 .pos(screenX + 81, screenY + buttonY).size(40, 20).build());
100:         addRenderableWidget(Button.builder(Component.literal("左移"), ignored -> moveLeft())
...OMITTED...
194:                 return staffSlotTechnique(currentSlot);
195:             }
196:             Skill skill = slotSkill(currentSlot);
197:             if (skill != null && selectedTech >= 0 && selectedTech < skill.getTechniques().size()) {
198:                 return skill.getTechniques().get(selectedTech);
199:             }
200:         }
201:         return null;
202:     }
203: 
204:     private static String typeName(TechniqueType type) {
205:         return switch (type) {
206:             case SWORD -> "剑术";
207:             case MOVEMENT -> "动作";
208:             case MAGIC -> "法术";
209:             case UNIVERSAL -> "通用";
210:         };
211:     }
212: 
213:     private static String attributeName(ShuAttribute attr) {
214:         return switch (attr) {
215:             case NONE -> "无";
216:             case WOOD -> "木";
217:             case FIRE -> "火";
218:             case WATER -> "水";
219:             case EARTH -> "土";
220:             case METAL -> "金";
221:         };
222:     }
223: 
224:     private static ResourceLocation techniqueIcon(Technique technique) {
225:         String kind = technique.getType() == TechniqueType.SWORD ? "attack" : "other";
226:         ShuAttribute attr = technique.getAttribute();
227:         String textureKey = attr == null ? "none" : attr.getId();
228:         return ResourceLocation.fromNamespaceAndPath(XueXiMod.MOD_ID, "textures/gui/technique/" + kind + "_" + textureKey + ".png");
229:     }
230: 
231:     private static void blitTechniqueIcon(GuiGraphics graphics, Technique technique, int drawX, int drawY, int size) {
232:         graphics.blit(techniqueIcon(technique), drawX, drawY, size, size, 0.0F, 0.0F, 32, 32, 32, 32);
233:     }
234: 
235:     private int visibleListRows() {
...OMITTED...
245:     }
246: 
247:     private void clampListScroll(int size) {
248:         int upperBound = Math.max(0, size - visibleListRows());
249:         if (listScroll < 0) listScroll = 0;
250:         if (listScroll > upperBound) listScroll = upperBound;
251:     }
252: 
253:     @Override
254:     protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
255:         graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
256:         graphics.fill(leftPos + LEFT_X, topPos + COLUMN_TOP, leftPos + LEFT_X + LEFT_W, topPos + COLUMN_BOTTOM, LEFT_BG);
257:     }
258: 
259:     @Override
260:     protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
261:         graphics.drawCenteredString(font, isStaffMode() ? "法杖装配" : "剑技装配", imageWidth / 2, TITLE_Y, 0xFFFFFF);
262:         graphics.drawString(font, "×", CLOSE_X + 4, CLOSE_Y + 1, 0xFFFFFF, false);
263: 
264:         List<SkillStorageData.Entry> entries = storageEntries();
265:         clampListScroll(entries.size());
266:         graphics.drawString(font, "匣子成品", LEFT_X, LIST_HEADER_Y, 0xFFFF55, false);
267:         if (entries.isEmpty()) {
268:             graphics.drawString(font, "（空）", LEFT_X, LIST_ROWS_Y, 0xAAAAAA, false);
269:         } else {
270:             for (int rowIndex = 0; rowIndex < visibleListRows(); rowIndex++) {
271:                 int index = listScroll + rowIndex;
...OMITTED...
317:             }
318:         }
319: 
320:         Technique detail = detailTechnique();
321:         if (detail == null) {
322:             graphics.drawString(font, "未选择术式", LEFT_X, DETAIL_Y, 0xAAAAAA, false);
323:         } else {
324:             String typeText = typeName(detail.getType());
325:             String attrText = attributeName(detail.getAttribute());
326:             graphics.drawString(font, "类型：", LEFT_X, DETAIL_Y, 0xFFFFFF, false);
327:             graphics.drawString(font, typeText, LEFT_X + font.width("类型："), DETAIL_Y, CYAN, false);
328:             int attributeX = LEFT_X + font.width("类型：" + typeText) + 10;
329:             graphics.drawString(font, "属性：", attributeX, DETAIL_Y, 0xFFFFFF, false);
330:             graphics.drawString(font, attrText, attributeX + font.width("属性："), DETAIL_Y, CYAN, false);
331: 
332:             graphics.drawString(font, "描述：", LEFT_X, DETAIL_Y + 12, 0xFFFFFF, false);
333:             graphics.drawString(font, detail.getDescription(), LEFT_X + font.width("描述："), DETAIL_Y + 12, CYAN, false);
334: 
335:             graphics.drawString(font, "装配复杂度：", LEFT_X, DETAIL_Y + 24, 0xFFFFFF, false);
336:             graphics.drawString(font, String.valueOf(detail.assemblyCost()),
337:                     LEFT_X + font.width("装配复杂度："), DETAIL_Y + 24, CYAN, false);
338:         }
339:     }
340: 
341:     @Override
342:     public boolean mouseClicked(double mouseX, double mouseY, int button) {
343:         int panelX = (int) mouseX - leftPos;
344:         int panelY = (int) mouseY - topPos;
345: 
346:         if (button == 0) {
347:             if (panelX >= CLOSE_X && panelX < CLOSE_X + CLOSE_W && panelY >= CLOSE_Y && panelY < CLOSE_Y + CLOSE_H) {
</partial_source>

<partial_source path="src/main/java/com/bslx/XueXiMod/gui/SpellAssemblyScreen.java" selected="matching params and 10 adjacent lines; omissions explicit">
1: package com.bslx.XueXiMod.gui;
2: 
3: import com.bslx.XueXiMod.item.SkillStoneItem;
4: import com.bslx.XueXiMod.shu.ShuAttribute;
5: import com.bslx.XueXiMod.shu.SkillStoneData;
6: import com.bslx.XueXiMod.shu.Technique;
7: import com.bslx.XueXiMod.shu.TechniqueType;
8: import com.bslx.XueXiMod.network.HeldSwordGate;
9: import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
10: import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
11: import net.minecraft.client.gui.GuiGraphics;
12: import net.minecraft.client.gui.components.Button;
13: import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
14: import net.minecraft.network.chat.Component;
15: import net.minecraft.world.entity.player.Inventory;
16: import net.minecraft.world.inventory.Slot;
17: import net.minecraft.world.item.ItemStack;
...OMITTED...
40:     private static final int ROW2_Y = 22;
41:     private static final int BTN_NORMAL = 0xFF3A3A3A;
42:     private static final int BTN_SELECTED = 0xFF6B9E3E;
43:     private static final int RETRIEVE_W = 36;
44:     private static final int RETRIEVE_H = 14;
45: 
46:     private Button retrieveButton;
47:     private int selectedGear = -1;
48: 
49:     private static final String[] TYPE_LABELS = {"全部", "剑术", "法术", "动作", "常用"};
50:     private static final TechniqueType[] TYPE_VALUES =
51:             {null, TechniqueType.SWORD, TechniqueType.MAGIC, TechniqueType.MOVEMENT, TechniqueType.UNIVERSAL};
52:     private static final String[] ATTR_LABELS = {"无", "木", "火", "水", "土", "金"};
53:     private static final ShuAttribute[] ATTR_VALUES =
54:             {ShuAttribute.NONE, ShuAttribute.WOOD, ShuAttribute.FIRE, ShuAttribute.WATER, ShuAttribute.EARTH, ShuAttribute.METAL};
55: 
56:     public SpellAssemblyScreen(SpellAssemblyMenu menu, Inventory inventory, Component title) {
57:         super(menu, inventory, title);
58:         this.imageWidth = SpellAssemblyMenu.GUI_W;
59:         this.imageHeight = SpellAssemblyMenu.GUI_H;
60:         this.titleLabelY = -999;
61:         this.inventoryLabelY = -999;
62:     }
63: 
64:     @Override
65:     protected void init() {
66:         super.init();
67:         this.retrieveButton = Button.builder(Component.literal("取回"), pressed -> {
68:             if (this.selectedGear >= 0 && this.minecraft != null && this.minecraft.gameMode != null) {
69:                 this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, this.selectedGear);
...OMITTED...
77:     @Override
78:     public void removed() {
79:         this.selectedGear = -1;
80:         super.removed();
81:     }
82: 
83:     @Override
84:     protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
85:         int left = this.leftPos;
86:         int top = this.topPos;
87:         int width = SpellAssemblyMenu.GUI_W;
88:         int height = SpellAssemblyMenu.GUI_H;
89:         graphics.fill(left, top, left + width, top + height, PANEL);
90:         graphics.fill(left, top, left + width, top + 2, FRAME);
91:         graphics.fill(left, top + height - 2, left + width, top + height, FRAME);
92:         graphics.fill(left, top, left + 2, top + height, FRAME);
93:         graphics.fill(left + width - 2, top, left + width, top + height, FRAME);
94:         graphics.fill(left + 3, top + 3, left + SpellAssemblyMenu.GEAR_X - 2, top + SpellAssemblyMenu.LIST_Y - 1, FILTER_BG);
95:         graphics.fill(left + 3, top + SpellAssemblyMenu.LIST_Y - 1, left + SpellAssemblyMenu.GEAR_X - 2, top + SpellAssemblyMenu.BOTTOM_Y - 1, LIST_BG);
96:         graphics.fill(left + 3, top + SpellAssemblyMenu.BOTTOM_Y - 1, left + SpellAssemblyMenu.GEAR_X - 2, top + height - 3, INV_BG);
97:         graphics.fill(left + SpellAssemblyMenu.GEAR_X - 2, top + SpellAssemblyMenu.BOTTOM_Y - 1,
98:                 left + SpellAssemblyMenu.PREVIEW_X - 2, top + height - 3, GEAR_BG);
99:         graphics.fill(left + SpellAssemblyMenu.PREVIEW_X - 2, top + 3, left + width - 3, top + height - 3, PREVIEW_BG);
100:         for (Slot slot : this.menu.slots) {
101:             drawSlotBox(graphics, left + slot.x, top + slot.y);
102:         }
103:     }
104: 
105:     @Override
106:     public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
107:         syncRetrieveButton();
108:         super.render(graphics, mouseX, mouseY, partialTick);
109:         renderFilterButtons(graphics);
...OMITTED...
139:                     this.selectedGear = gearIndex;
140:                     return true;
141:                 }
142:             }
143:         }
144:         return super.mouseClicked(mouseX, mouseY, button);
145:     }
146: 
147:     private void renderFilterButtons(GuiGraphics graphics) {
148:         int typeSel = -1;
149:         TechniqueType type = this.menu.currentType();
150:         if (type == null) {
151:             typeSel = 0;
152:         } else {
153:             for (int typeIndex = 1; typeIndex < TYPE_VALUES.length; typeIndex++) {
154:                 if (TYPE_VALUES[typeIndex] == type) {
155:                     typeSel = typeIndex;
156:                     break;
157:                 }
158:             }
159:         }
...OMITTED...
168:             }
169:         }
170:         for (int typeIndex = 0; typeIndex < TYPE_LABELS.length; typeIndex++) {
171:             drawFilterButton(graphics, typeIndex, ROW1_Y, TYPE_LABELS[typeIndex], typeIndex == typeSel, BTN_STEP, BTN_W);
172:         }
173:         for (int attrIndex = 0; attrIndex < ATTR_LABELS.length; attrIndex++) {
174:             drawFilterButton(graphics, attrIndex, ROW2_Y, ATTR_LABELS[attrIndex], attrIndex == attrSel, ATTR_STEP, ATTR_W);
175:         }
176:     }
177: 
178:     private void drawFilterButton(GuiGraphics graphics, int index, int rowY, String label, boolean selected, int step, int width) {
179:         int buttonX = this.leftPos + BTN_LEFT + index * step;
180:         int buttonY = this.topPos + rowY;
181:         graphics.fill(buttonX, buttonY, buttonX + width, buttonY + BTN_H, selected ? BTN_SELECTED : BTN_NORMAL);
182:         int textX = buttonX + (width - this.font.width(label)) / 2;
183:         int textY = buttonY + (BTN_H - this.font.lineHeight) / 2;
184:         graphics.drawString(this.font, label, textX, textY, selected ? 0xFFFFFF : 0xDDDDDD, false);
185:     }
186: 
187:     private int buttonAt(double mouseX, double mouseY, int count, int rowY, int step, int width) {
188:         for (int buttonIndex = 0; buttonIndex < count; buttonIndex++) {
189:             int buttonX = this.leftPos + BTN_LEFT + buttonIndex * step;
190:             int buttonY = this.topPos + rowY;
191:             if (mouseX >= buttonX && mouseX < buttonX + width && mouseY >= buttonY && mouseY < buttonY + BTN_H) {
192:                 return buttonIndex;
193:             }
194:         }
195:         return -1;
196:     }
197: 
198:     private void drawSlotBox(GuiGraphics graphics, int slotX, int slotY) {
199:         graphics.fill(slotX, slotY, slotX + 16, slotY + 16, SLOT_FILL);
200:         graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY, SLOT_BORDER);
201:         graphics.fill(slotX - 1, slotY + 16, slotX + 17, slotY + 17, SLOT_BORDER);
...OMITTED...
228:         if (data == null || data.getTechnique() == null) {
229:             return;
230:         }
231:         Technique technique = data.getTechnique();
232:         int textX = this.leftPos + PREVIEW_X;
233:         int textY = this.topPos + PREVIEW_Y + 78;
234:         int iconX = this.leftPos + PREVIEW_X + 28;
235:         int iconY = this.topPos + PREVIEW_Y + 28;
236:         graphics.pose().pushPose();
237:         graphics.pose().translate(iconX, iconY, 0.0F);
238:         graphics.pose().scale(2.0F, 2.0F, 1.0F);
239:         graphics.renderItem(hovered.getItem(), 0, 0);
240:         graphics.pose().popPose();
241:         graphics.drawString(this.font, "名称 " + technique.getName(), textX, textY, 0xFFFFFF, false);
242:         graphics.drawString(this.font, "类型 " + typeName(technique), textX, textY + 12, 0xFFFFFF, false);
243:         graphics.drawString(this.font, "属性 " + attrName(technique.getAttribute()), textX, textY + 24, 0xFFFFFF, false);
244:         graphics.drawString(this.font, "伤害 " + trim(technique.getBaseDamage()), textX, textY + 36, 0xFFFFFF, false);
245:         graphics.drawString(this.font, "冷却 " + technique.getBaseCooldown(), textX, textY + 48, 0xFFFFFF, false);
246:         graphics.drawString(this.font, "魔力消耗 " + technique.manaCost(), textX, textY + 60, 0xFFFFFF, false);
247:         graphics.drawString(this.font, technique.getDescription(), textX, textY + 78, 0xDDDDDD, false);
248:     }
</partial_source>

<partial_source path="src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitEntity.java" selected="matching params and 10 adjacent lines; omissions explicit">
41: import java.util.List;
42: import java.util.Map;
43: import java.util.Set;
44: import java.util.UUID;
45: 
46: public class TechniqueHitEntity extends Entity {
47:     private static final EntityDataAccessor<String> DATA_TECHNIQUE_ID =
48:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.STRING);
49:     private static final EntityDataAccessor<Integer> DATA_VISUAL =
50:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
51:     private static final EntityDataAccessor<Float> DATA_MODEL_SCALE =
52:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);
53:     private static final EntityDataAccessor<String> DATA_TEXTURE =
54:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.STRING);
55:     private static final EntityDataAccessor<Float> DATA_LENGTH =
56:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);
57:     private static final EntityDataAccessor<Float> DATA_WIDTH =
58:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);
59:     private static final EntityDataAccessor<Float> DATA_HEIGHT =
60:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);
61:     private static final EntityDataAccessor<Float> DATA_ROLL =
62:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);
63:     private static final EntityDataAccessor<String> DATA_ARC_SNAPSHOT =
64:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.STRING);
65:     private static final EntityDataAccessor<Integer> DATA_DISPLAY_INDEX =
66:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
67:     private static final EntityDataAccessor<Integer> DATA_DISPLAY_COUNT =
68:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
69:     private static final EntityDataAccessor<Boolean> DATA_DISPLAY_LANE =
...OMITTED...
82:             SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.BOOLEAN);
83: 
84:     @Nullable
85:     private UUID casterUuid;
86:     private boolean deflectsIncoming;
87:     /** 持续接触结算。只从释放定义抄来，客户端不登记瀑布宿主。 */
88:     boolean persistentFlow;
89:     private ArcDeflectionHost arcDeflection;
90:     private TechniqueMotion motion;
91:     private TechniqueHitbox hitbox;
92:     private int lifetime;
93:     private double speed;
94:     private boolean lockThisCast;
95:     private boolean dropThisCast;
96:     private boolean centered;
97:     private boolean pose3d;
98:     private boolean beam;
99:     private boolean pinned;
100:     private boolean trap;
101:     boolean burstOnImpact;
102:     private boolean planted;
...OMITTED...
106:     private boolean pendingActivation;
107:     @Nullable
108:     private ArcBandSnapshot arcSnapshot;
109:     private double lastProcessedProgress;
110:     private boolean arcDamageClosed;
111:     boolean detonated;
112:     private final ClientTrack clientTrack = new ClientTrack();
113:     int settleControlTicks;
114:     boolean settleExtinguish;
115:     float settleKnockback;
116:     private double arcRadius;
117:     private double arcDegrees;
118:     private double arcHeight;
119:     private Vec3 boxSize = Vec3.ZERO;
120:     private double length = 1.0;
121:     private double width = 1.0;
122:     private double height = 1.0;
123:     int hitInterval;
124:     int hitReadyAge;
125:     @Nullable
126:     TechniqueHitSpec.HitListener listener;
127:     private int age;
128:     /** 就绪年龄只通知一次。不在这里识别术式名字。 */
129:     private boolean partingNotified;
130:     private Vec3 origin = Vec3.ZERO;
131:     Vec3 forward = new Vec3(0.0, 0.0, 1.0);
132:     private String functionId = "";
...OMITTED...
148:     public TechniqueHitEntity(EntityType<? extends TechniqueHitEntity> type, Level level) {
149:         super(type, level);
150:         this.noPhysics = true;
151:         this.setNoGravity(true);
152:     }
153: 
154:     @Override
155:     protected void defineSynchedData() {
156:         this.entityData.define(DATA_TECHNIQUE_ID, "");
157:         this.entityData.define(DATA_VISUAL, 0);
158:         this.entityData.define(DATA_MODEL_SCALE, 1.0F);
159:         this.entityData.define(DATA_TEXTURE, "");
160:         this.entityData.define(DATA_LENGTH, 1.0F);
161:         this.entityData.define(DATA_WIDTH, 1.0F);
162:         this.entityData.define(DATA_HEIGHT, 1.0F);
163:         this.entityData.define(DATA_ROLL, 0.0F);
164:         this.entityData.define(DATA_ARC_SNAPSHOT, "");
165:         this.entityData.define(DATA_DISPLAY_INDEX, 0);
166:         this.entityData.define(DATA_DISPLAY_COUNT, 1);
167:         this.entityData.define(DATA_DISPLAY_LANE, false);
168:         this.entityData.define(DATA_LANE_INDEX, 0);
169:         this.entityData.define(DATA_LANE_COUNT, 1);
170:         this.entityData.define(DATA_STILL_FRAMES, 0);
171:         this.entityData.define(DATA_STILL_FRAME_TICKS, 1);
172:         this.entityData.define(DATA_STILL_BIRTH, StillSheetVisual.UNBORN);
...OMITTED...
300:             BlockHitResult blockHit = level.clip(new ClipContext(
301:                     this.previousPosition, this.position(),
302:                     ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
303:             if (blockHit.getType() == HitResult.Type.BLOCK) {
304:                 Vec3 hitPoint = blockHit.getLocation();
305:                 this.setPos(hitPoint.x, hitPoint.y, hitPoint.z);
306:                 TechniqueHitSettle.onExpire(this, level);
307:                 return;
308:             }
309:         }
310:         if (this.age >= this.lifetime) {
311:             TechniqueHitSettle.onExpire(this, level);
312:         }
313:     }
314: 
315:     /** 粒子只在客户端逻辑拍生成，避免渲染帧里按光束长度狂撒。 */
316:     private void clientDecor() {
317:         int visual = this.getVisual();
318:         if (visual == 4) {
319:             this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY() + 0.2, this.getZ(), 0.0, 0.01, 0.0);
320:         } else if (visual == 3 && (this.tickCount & 7) == 0) {
...OMITTED...
357:         CohortVisual shown = spec.cohortVisual == null ? CohortVisual.SINGLE : spec.cohortVisual.stored();
358:         this.entityData.set(DATA_DISPLAY_INDEX, shown.displayIndex());
359:         this.entityData.set(DATA_DISPLAY_COUNT, shown.displayCount());
360:         this.entityData.set(DATA_DISPLAY_LANE, shown.laneSplit());
361:         this.entityData.set(DATA_LANE_INDEX, shown.laneIndex());
362:         this.entityData.set(DATA_LANE_COUNT, shown.laneCount());
363:         this.casterUuid = caster.getUUID();
364:         ReleaseDefinition boundDefinition = ReleaseRegistry.get(spec.techniqueId);
365:         this.deflectsIncoming = boundDefinition != null && boundDefinition.deflectsProjectiles();
366:         this.persistentFlow = boundDefinition != null && boundDefinition.persistentFlowSettlement();
367:         this.lifetime = spec.lifetime;
368:         this.speed = spec.speed;
369:         this.lockThisCast = spec.lockThisCast;
370:         this.dropThisCast = spec.dropThisCast;
371:         this.centered = spec.centered;
372:         this.pose3d = spec.pose3d;
373:         this.arcRadius = spec.arcRadius;
374:         this.arcDegrees = spec.arcDegrees;
375:         this.arcHeight = spec.arcHeight;
376:         this.boxSize = spec.boxSize == null ? Vec3.ZERO : spec.boxSize;
377:         this.hitInterval = spec.hitInterval;
378:         this.hitReadyAge = Math.max(0, spec.hitReadyAge);
379:         this.listener = spec.listener;
380:         this.settleDamage = spec.settleDamage;
381:         this.settleEnchantments = spec.settleEnchantments;
382:         this.settleTally = spec.settleTally;
383:         this.settleCopyIndex = spec.settleCopyIndex;
384:         this.settleBurnSeconds = spec.settleBurnSeconds;
385:         this.settleControlTicks = spec.settleControlTicks;
386:         this.settleExtinguish = spec.settleExtinguish;
387:         this.settleKnockback = spec.settleKnockback;
388:         this.settleTide = spec.settleTide == null
389:                 ? com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess.NONE
390:                 : spec.settleTide;
391:         this.trap = spec.trap;
392:         this.burstOnImpact = spec.burstOnImpact;
393:         if (spec.blocksMovement) ObstacleBodies.track(this, spec.length, spec.width, spec.height);
394:         this.anchor = spec.anchor;
395:         this.aim = spec.aim;
396:         this.length = spec.length;
397:         this.width = spec.width;
398:         this.height = spec.height;
399:         this.functionId = spec.functionId == null ? "" : spec.functionId;
400:         if (spec.frozenArc != null) {
401:             this.installArc(spec.frozenArc);
402:             return;
403:         }
404:         this.origin = spec.start != null ? spec.start : caster.getEyePosition();
405:         this.forward = spec.aim != null && spec.aim.lengthSqr() > 1.0E-8
406:                 ? spec.aim.normalize()
407:                 : caster.getLookAngle();
408:         TechniqueHitSpec.Path path = spec.path;
...OMITTED...
414:                 case PROJECTILE -> TechniqueHitSpec.Path.STRAIGHT;
415:             };
416:         }
417:         if (spec.dropThisCast) {
418:             path = TechniqueHitSpec.Path.DROP;
419:         }
420:         this.beam = path == TechniqueHitSpec.Path.FOLLOW;
421:         this.pinned = this.beam && spec.start != null;
422:         this.entityData.set(DATA_TECHNIQUE_ID, spec.techniqueId == null ? "" : spec.techniqueId);
423:         this.entityData.set(DATA_VISUAL, spec.visual);
424:         this.entityData.set(DATA_MODEL_SCALE, spec.modelScale);
425:         this.entityData.set(DATA_TEXTURE, spec.texture == null ? "" : spec.texture);
426:         StillSheetVisual stillSheet = spec.stillSheet == null ? StillSheetVisual.NONE : spec.stillSheet;
427:         this.entityData.set(DATA_STILL_FRAMES, stillSheet.enabled() ? stillSheet.frameCount() : 0);
428:         this.entityData.set(DATA_STILL_FRAME_TICKS, stillSheet.enabled() ? stillSheet.ticksPerFrame() : 1);
429:         this.entityData.set(DATA_LENGTH, (float) spec.length);
430:         this.entityData.set(DATA_WIDTH, (float) spec.width);
431:         this.entityData.set(DATA_HEIGHT, (float) spec.height);
432:         this.entityData.set(DATA_ROLL, spec.roll);
433:         this.motion = spec.trap ? new com.bslx.XueXiMod.shu.technique.release.motion.TrapFallMotion() : TechniqueHitKinds.motion(path);
434:         if (spec.tracking && !spec.trap && (path == TechniqueHitSpec.Path.STRAIGHT
435:                 || path == TechniqueHitSpec.Path.DROP || path == TechniqueHitSpec.Path.VORTEX)) {
436:             this.motion = new com.bslx.XueXiMod.shu.technique.release.motion.TrackingMotion(path);
437:         }
438:         this.hitbox = spec.path == null && spec.texture == null ? TechniqueHitKinds.hitbox(spec.hitbox) : new SizedBoxHitbox();
439:         if (path == TechniqueHitSpec.Path.STILL_AIM && this.motion != null && caster.level() instanceof ServerLevel serverLevel) {
440:             this.motion.tick(this, serverLevel);
441:             this.previousPosition = this.position();
...OMITTED...
470:         if (server == null) {
471:             return null;
472:         }
473:         return server.getPlayerList().getPlayer(this.casterUuid);
474:     }
475: 
476:     public int age() {
477:         return this.age;
478:     }
479: 
480:     public int lifetime() {
481:         return this.lifetime;
482:     }
483: 
484:     public double speed() {
485:         return this.speed;
486:     }
487: 
488:     public boolean lockThisCast() {
489:         return this.lockThisCast;
490:     }
491: 
...OMITTED...
514:     }
515: 
516:     public void setPlanted(boolean planted) {
517:         this.planted = planted;
518:     }
519: 
520:     public float roll() {
521:         return this.entityData.get(DATA_ROLL);
522:     }
523: 
524:     public double arcRadius() {
525:         return this.arcRadius;
526:     }
527: 
528:     public double arcDegrees() {
529:         return this.arcDegrees;
530:     }
531: 
532:     public double arcHeight() {
533:         return this.arcHeight;
534:     }
535: 
536:     public Vec3 boxSize() {
537:         return this.boxSize;
538:     }
539: 
540:     public Vec3 origin() {
541:         return this.origin;
542:     }
543: 
...OMITTED...
633: 
634:     public int displayLaneCount() {
635:         return this.entityData.get(DATA_LANE_COUNT);
636:     }
637: 
638:     public CohortVisual cohortVisual() {
639:         return CohortVisual.read(this.displayCopyIndex(), this.displayCopyCount(),
640:                 this.displayLaneIndex(), this.displayLaneCount(), this.displayLaneSplit());
641:     }
642: 
643:     public float getModelScale() {
644:         return this.entityData.get(DATA_MODEL_SCALE);
645:     }
646: 
647:     public String texture() {
648:         return this.entityData.get(DATA_TEXTURE);
649:     }
650: 
651:     public StillSheetVisual stillSheet() {
652:         return StillSheetVisual.once(this.texture(), this.entityData.get(DATA_STILL_FRAMES),
653:                 this.entityData.get(DATA_STILL_FRAME_TICKS));
654:     }
...OMITTED...
677:         if (this.level().isClientSide()) {
678:             return;
679:         }
680:         this.entityData.set(DATA_STILL_CANCELLED, true);
681:     }
682: 
683:     public double length() {
684:         return this.level().isClientSide() ? this.entityData.get(DATA_LENGTH) : this.length;
685:     }
686: 
687:     public double width() {
688:         return this.level().isClientSide() ? this.entityData.get(DATA_WIDTH) : this.width;
689:     }
690: 
691:     public double height() {
692:         return this.level().isClientSide() ? this.entityData.get(DATA_HEIGHT) : this.height;
693:     }
694: 
695:     public boolean exactVolume() {
696:         return this.exactVolume;
697:     }
698: 
699:     public double hitHeight() {
700:         if (this.exactVolume) {
701:             return this.height() > 0.0 ? this.height() : 0.0;
702:         }
703:         if (this.height() <= 0.05) {
704:             return Math.max(this.length(), 1.0);
705:         }
706:         return Math.max(this.height(), 1.0);
707:     }
708: 
709:     public void resizeVolume(double along, double across, double vertical) {
710:         this.exactVolume = true;
711:         this.length = along;
712:         this.width = across;
713:         this.height = vertical;
714:         this.entityData.set(DATA_LENGTH, (float) along);
715:         this.entityData.set(DATA_WIDTH, (float) across);
716:         this.entityData.set(DATA_HEIGHT, (float) vertical);
717:         double reach = Math.max(along, Math.max(across, vertical)) * 0.5;
718:         this.setBoundingBox(new AABB(
719:                 this.getX() - reach, this.getY() - reach, this.getZ() - reach,
720:                 this.getX() + reach, this.getY() + reach, this.getZ() + reach));
721:     }
722: 
723:     private void installArc(ArcBandSnapshot snapshot) {
724:         this.motion = null;
725:         this.hitbox = null;
726:         this.centered = false;
727:         this.adoptArc(snapshot);
728:         this.forward = new Vec3(snapshot.forwardAxis().east(), snapshot.forwardAxis().above(), snapshot.forwardAxis().south());
729:         this.origin = new Vec3(snapshot.origin().east(), snapshot.origin().above(), snapshot.origin().south());
730:         this.placeAt(this.origin, this.forward);
731:         this.lifetime = snapshot.lifeTicks();
732:     }
733: 
734:     private void adoptArc(ArcBandSnapshot snapshot) {
735:         this.arcSnapshot = snapshot;
736:         this.entityData.set(DATA_ARC_SNAPSHOT, snapshot.pack());
737:     }
738: 
739:     private void tickArcBand(ServerLevel level) {
740:         if (this.arcSnapshot.birthTick() < 0L) {
741:             return;
</partial_source>
