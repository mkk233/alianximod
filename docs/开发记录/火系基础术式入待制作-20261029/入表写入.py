# -*- coding: utf-8 -*-
"""Insert 13 fire basic techniques into 待制作. Preserves the rest of the workbook."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

root = Path(r"D:\alianximod")
batch = root / "docs" / "开发记录" / "火系基础术式入待制作-20261029"
live = root / "docs" / "术式制作表.xlsx"
expected = "50877EAF6C0D34DAAA49465DE447F8600073BE19348D3C90B96B2F0A1D4985E7"
footer = (
    "本次录入2026-10-29。来源docs/火属性基础术式与组合设计.md，并按用户最新决定替换魂焰、采用共享燃料。"
    "只进入待制作，尚未领取，不开始实现。"
    "魔力、冷却、复杂度、装配消耗、等级上限和等级成长未定，对应数值格留空，不用0代替。"
)
rows = [
    {
        "name": "焰流", "id": "fire_flame_flow",
        "C": "法术", "D": "火", "E": 2, "J": "否", "M": "是", "U": 60,
        "P": "命中体", "Q": "持续", "R": "直线", "S": "攻击", "AH": "是", "AK": "是", "AB": "否",
        "n": "生成一束有实际宽度、缓慢向前推进的持续火焰。默认沿释放方向运动，持续60tick，在出生后的第20、40、60tick各有一次基础2点火伤。允许修改弹道。风暴轨迹使火体沿旋转路径扫过，喷涌使火体向上抛起；改变路径不增加伤害段数。只在实际火体及其连续扫掠范围内判定，视觉火星和漏斗外框不单独造成伤害。每个脉冲对同一目标至多命中一次。种火、引燃和引爆按整次释放对同一目标去重，不能每个脉冲重触发。未附加种火时，持续火伤不生产火种。\n火焰风暴组合为落点指定、风暴轨迹、一分为三、种火、焰流，不另建组合行。种火可换成引燃或引爆。旋转轨迹不等于整个漏斗都有伤害，也不默认吸附或禁锢。",
        "o": "有宽度的橙红火束，火焰前沿可辨认，能看出正在扫到的位置。",
        "dv": "推进速度、火束宽度和风暴轨迹的具体覆盖尺寸未定。风暴轨迹现表已有，编号trajectory_storm，本行只依赖它。火球、爆燃、落点指定、垂落、一分为三、聚能都是既有术式，不在本次新增。" + footer,
    },
    {
        "name": "灼地", "id": "fire_scorch_field",
        "C": "法术", "D": "火", "E": 2, "J": "否", "M": "是", "AB": "否",
        "P": "命中体", "Q": "持续", "S": "攻击", "AH": "否", "AK": "否",
        "AS": 2, "BV": 20, "BW": 120,
        "n": "在合法地表形成固定火场。基础半径2格，持续120tick，每20tick造成一次基础2点火伤，首次在第20tick。只有接触火场实际低矮判定体积的目标受影响，不自动攻击整根竖直柱。单独使用时默认脚前合法地表；装配地表定位后可投到远处。火场位置固定，不被弹道改成飞行火场。不放置原版火或岩浆，不蔓延、不破坏方块、不烧掉落物，与陨岩留下真实方块不同。火场到期清理；已经挂到目标上的资源燃烧按自己的剩余时长继续。\n灵魂火地带组合改为地表定位、幽冥、灼地，不另建组合行。灼地作为领域载体。幽冥把火场表现为蓝火；蓝火不改变伤害归属或免疫，不给减速，不放置灵魂沙或原版灵魂火方块。",
        "o": "边界和贴地高度清楚的低矮火场。装上幽冥后表现为蓝色魂火。",
        "dv": "魂焰的40tick、20%减速已被用户否决，不写入本行。火场半径、寿命和伤害间隔是来源稿的基础参数。" + footer,
    },
    {
        "name": "陨岩", "id": "fire_meteorite",
        "C": "法术", "D": "火", "E": 24, "J": "否", "M": "是", "AB": "否",
        "P": "命中体", "Q": "触发", "S": "攻击", "AH": "是", "AK": "是",
        "n": "生成有实体碰撞的炽热岩体，标准本体按一个方块的体量。单独使用时向前抛出并受重力作用，允许垂落、喷涌等弹道修改。第一次合法实体碰撞或首次地形碰撞进入撞击阶段，对中心做一次基础24点物理冲击。实体直接碰撞已经包含在这次中心判定里，不再另算直击，也不叠加原版落物伤害。中心基础水平半径1.5格。与震环组合时两者使用同一垂直判定带，首版总高3格，以撞击点为中心。跨越两区的大体型目标优先归入中心，只进其中一组。\n每个真实陨岩最终留下一个可保存、可采掘的陨石方块，有碰撞和世界存档，不是纯特效。聚能不把一个方块变成一整座山。本体不自动消费火种，也不自动破坏周围地形。中心消费由引爆提供，挖坑由崩地提供，留下自身方块始终是本体功能。\n先撞到空中目标时，撞击和命中接续在该碰撞点完成，随后残骸无伤坠落。接地只处理崩地和方块安放，不再产生中心伤害、震环或资源消费。全局术式地形破坏开关不能关闭陨石残留。关闭破坏时在合法空位放置，不覆盖已有地形。极端没有合法位置时保留无伤待安放残骸，不丢弃，待有空位再变成方块。火种为0也不能让岩体消失。方块不自动散发燃烧、不循环产种、不构成第三轮攻击。\n陨石雨主段依赖落点指定、垂落、一分为三、错时、崩地、引爆和本行；火山喷发把垂落换成地表定位和喷涌。命中接续子段是引燃、震环。引爆和引燃分别附着父子段，不内置成两次免费消费。这些组合不另建行。",
        "o": "一个方块大小的炽热岩体，外层岩壳，裂缝透出熔热。落地后成为可碰撞、可保存、可采掘的陨石方块。",
        "dv": "默认抛射速度未定，弹道格留空，不填成直线或垂落。方块硬度、采掘工具、掉落物、用途和空位搜索边界待定。不沿用旧稿环带总32点或引燃14段28点。连续多组陨石雨需要真实的后续释放和费用，费用未定。" + footer,
    },
    {
        "name": "震环", "id": "fire_shock_ring",
        "C": "法术", "D": "火", "E": 4, "J": "否", "M": "是", "AB": "否",
        "P": "命中体", "Q": "触发", "S": "攻击",
        "n": "在释放锚点周围产生一次环形冲击，基础物理伤害4点。基础内半径1.5格、外半径5格。单独使用时以施法位置为中心，接续时使用父攻击给出的事件点。中间为空，不填成圆盘。与陨岩配套时，中心目标名单作为本次排除名单，避免目标被第一击推入环带后再受一次直接伤害。只有一次范围命中，扩散动画不按每一帧重新造成伤害。可以携带种火、引燃、引爆或幽冥的表现，但不自动附送这些功能。击退不是已定功能。\n作为陨岩命中接续的子段时，只继承子段自己的引燃，不继承中心引爆。引燃按共享逐段燃料草案挂载，不按旧预付承诺28点。",
        "o": "一次向外扩散的环形冲击，中间留空。动画不代表每一帧都在命中。",
        "dv": "击退未定，不填写击退数值。不把震环当成免费视觉附件。不内置火种消费。" + footer,
    },
    {
        "name": "地表定位", "id": "fire_surface_anchor",
        "C": "通用", "D": "火", "J": "否", "AB": "否",
        "P": "命中体", "Q": "触发",
        "n": "沿准星选择有遮挡约束的有效地表，给出表面上方能容纳完整攻击本体的锚点。它明确选地面，不同于在目标附近空间取点的落点指定。不把弹丸变成落物，不代替垂落或喷涌。没有合法表面或足够空间时拒绝该次释放，不隔墙找点，不把攻击塞进实心方块。用于向上喷发时，锚点留出本体与支撑面的间距，避免出生就撞地。出生时的支撑关系不是一次新的命中接续。\n灼地可改用这个锚点。火山喷发以它作为喷口。不另建组合行。",
        "o": "准星落在可见地表上的锚点标记，位置在表面上方，能看出不是隔墙选点。",
        "dv": "现有链上角色只有「落点指定」，没有「地表定位」。链上角色和弹道格留空，不把本行填成落点指定或垂落。" + footer,
    },
    {
        "name": "喷涌", "id": "fire_upwell",
        "C": "通用", "D": "火", "J": "否", "AB": "否",
        "P": "命中体", "Q": "触发",
        "n": "把攻击改成向上抛出、再受重力落回的完整抛物线。单份向上抛出；多份在向上的锥形范围内分配不同初始方向。只控制运动，不增加份数，不自动生成弹体、火柱、爆炸或落地火场。适用于陨岩、火球和其他允许修改弹道的载体。一条攻击分支选择一种完整运动路径。喷涌、垂落和风暴轨迹不因书写顺序叠成未定义的多套运动。\n火山喷发使用地表定位、喷涌、一分为三、错时、崩地、引爆、陨岩，不另建组合行。喷口火光不自动造成额外伤害。需要有伤害的中央火柱时另装焰流并计费。",
        "o": "攻击向上抛起再落下。多份时呈向上散开的锥形，不额外生出火柱。",
        "dv": "上抛速度、锥角和飞行距离未定，数值格留空。现有弹道名单没有喷涌，链上角色也没有对应项，这两格留空，不改记成垂落。" + footer,
    },
    {
        "name": "错时", "id": "fire_stagger",
        "C": "通用", "D": "火", "J": "否", "AB": "否",
        "P": "规则", "Q": "触发",
        "n": "把已经存在的多个副本错开出手。基础三份示例是相对本组开始的第0、10、20tick。只有一份时立即出手，不凭空增加后续攻击。一分为三负责数量，本行负责时间。两者要在通用释放计划里分开表达，不能因落进同一个旧规则槽就互相覆盖，也不能把错时实现成再复制三份。整组默认冻结释放时的目标与锚点。每个实际副本从自身出生时开始计算寿命。不重新瞄准，不补算尚未出生时的命中，不为每个波次创建新的火种上下文。错开后的真实后续攻击必须纳入费用和取消规则。\n陨石雨、火山喷发和烈焰人连射使用本行安排时序，不另建组合行。烈焰人连射的载体仍是已有火球。",
        "o": "多份攻击依次出现，间隔看得出，不额外变出新的弹体。",
        "dv": "现有链上角色没有「错时」，不填成「一分为三」。0、10、20tick只是三份示例。后续波次的费用未定。" + footer,
    },
    {
        "name": "命中接续", "id": "fire_hit_continue",
        "C": "通用", "D": "火", "J": "否", "AB": "否",
        "P": "规则", "Q": "触发",
        "n": "绑定一个父攻击和一个明确的后续攻击分支，只取最先成立的一次：父攻击首次命中通过关系与过滤的合法实体；此前没有实体命中时，首次碰撞有效地形，包括触地和撞墙。两种事件共享一次机会。命中实体后再触地不再触发。同一tick多个碰撞只接受运动路径上最早的一个。纯特效接触、被过滤的友方、父攻击尚未激活时的接触都不算。实体命中指攻击已经成立的命中事件，不以最终扣血大于0为条件。减伤或免疫不把一次接触变成无限等待。被取消或未成立的事件不触发。超时消失且没有有效命中时不默认触发。强制清理、主人失效或维度切换也不伪装成触地。\n先完成父攻击本次自身结算，再从固定接触点释放子攻击，继承必要的方向、归属和同一次施法上下文，不重新读取准星。不自动复制父攻击，不改写焰流等其他载体的后续寿命。附魔只作用于所属分支。三颗陨岩最多三个震环；每个震环只一份，不再次继承已经执行过的一分为三而变成九份。子分支若另装复制，必须显式计费。父子共用火种账本，但各自保留命中记录。不能按陨岩或震环的编号特判。本定义取代旧提案里「命中后继续复制父攻击」。",
        "o": "在父攻击的接触点接出明确的子攻击，不把父攻击再复制一遍。",
        "dv": "现有链上角色没有命中接续，本行留空，不填成落点指定、一分为三或其他角色。父子附魔范围和接续去重还是后续要补的公共能力，不假装表内枚举已经表达。" + footer,
    },
    {
        "name": "种火", "id": "fire_seed_sow",
        "C": "通用", "D": "火", "J": "是", "AB": "否",
        "P": "规则", "Q": "触发",
        "DW": "是", "DX": "火种", "DZ": "增加", "EA": "命中目标", "EB": "命中成功", "EC": "10",
        "EI": "每目标每次施法", "EL": "N/DV", "EM": "不改点火、热光束、火球已有产种",
        "n": "有效伤害命中后生产火种。基础10点，实际请求为向下取整后的10乘炉心产火种倍率，并按目标0至100上限钳制。无炉心为10点。同一次真实释放的多发、脉冲和接续对同一目标只生产一次；失败或被取消的伤害不生产。不改变基础攻击伤害，也不附送引燃或引爆。单独装配时交给下一次有效普通攻击，费用沿用统一附魔，不另建火系武器账本。\n原版着火、站在火场内和炉心运行时长不自动产种。正在燃烧的目标仍可获得这次新增火种，进入同一余额后由尚未结束的燃烧消耗。现有点火额外2、热光束每次接受命中1、火球每个实际目标3不改成这10点。\n火焰风暴、烈焰人连射和火山的铺火种变体可以装配本行，不另建组合行。",
        "o": "命中处短暂火星，表示火种挂上，不另做一段持续燃烧。",
        "dv": "普通炉心随热量约为11至16点、过载约为22点，这是草稿估算，待定稿，不写入数值格。本行是新的种火附魔，不是已有点火。" + footer,
        "ej": "有效伤害被接受才生产。失败或取消不生产。",
        "ek": "基础10，再乘炉心产火种倍率后向下取整，上限100。同一次真实释放对同一目标只生产一次。",
    },
    {
        "name": "引燃", "id": "fire_kindle",
        "C": "通用", "D": "火", "J": "是", "AB": "否",
        "P": "规则", "Q": "触发",
        "DW": "是", "DX": "火种", "DZ": "消费", "EA": "命中目标", "EB": "燃烧结算", "EC": "10",
        "EG": 10, "EI": "每次触发", "EL": "N/DV", "EM": "不产火种、不施加炉心",
        "n": "火种持续伤害附魔。本次录入的基础参数草案：启动时读取n=floor(当时余额/10)和炉心F，不把整笔余额预付或复制成私有燃料。每个完整段伤害为2×(1+F)，间隔10tick，最长40+10×n tick。每个完整段向共享余额请求10点火种；实付不足时按实付/请求缩放该段伤害，没有燃料则该段不造成资源伤害。100点火种且没有补种时，最多供10个完整的2点段，不承诺14段或28点。n小于1时不挂燃烧，载体自己的直接伤害照常处理。\n不同真实释放可以叠层。同一次真实释放、同一目标、同一效果不重复挂层，也不为每一段伪造新的施法上下文。不同效果争用同一余额。余额到0则结束，已经终止的层不因后来补种复活。尚未结束的层可以消耗新进入同一余额的火种，但不刷新启动倍率和时长。同tick到期的层按需求比例分配，不能只让第一层拿满。没有「正在燃烧就禁止引爆或禁止补种」的总锁，也没有同一次施法引燃后永远禁止引爆的跨效果锁。原版着火和载体基础伤害不自动变成资源燃烧。离开火场不解除已挂燃烧。\n陨石组合里，引燃只附着震环子段，不附着陨岩中心，也不把旧环带32点预算写成本行保证值。",
        "o": "目标身上的持续火焰，按段亮一下，燃料耗尽或期限到了就熄灭。",
        "dv": "上述间隔、时长和单段2×(1+F)是录入用的基础参数草案，待制作前定稿。不填写旧规则「启动消费全部火种、按S保证段数」。不采用100点14段、28点或环带总32点。" + footer,
        "ej": "同一次真实释放、同一目标、同一效果不重复挂层。不同真实释放可以叠层。满8条计划在扣火种之前拒绝。",
        "ek": "完整段请求10。单段2×(1+F)，间隔10tick，最长40+10×n tick。实付不足按比例缩放。同tick按需求比例分配。不预扣全部，不保证段数。",
    },
    {
        "name": "引爆", "id": "fire_detonate",
        "C": "通用", "D": "火", "J": "是", "AB": "否",
        "P": "规则", "Q": "触发",
        "DW": "是", "DX": "火种", "DZ": "消费", "EA": "命中目标", "EB": "命中成功", "EC": "全部",
        "EI": "每目标每次施法", "EL": "N/DV", "EM": "不是燎里，不用0.36或焚天指数",
        "n": "立即消费结算当时的剩余火种S，资源火伤为0.2×S×(1+F)。S是当时共享余额，不是预付私有池。余额为0时没有资源火伤，载体直接伤害照常处理。不含炉心和防御时，10点为2点，50点为10点，100点为20点。1至9点按实际剩余结算。只作用于本次载体命中的目标，每个目标消费自己的余额，不能把一个目标的火种复制成整片范围的额外伤害。\n与燃烧争用同一余额。燃烧先扣则本行只消费剩余；本行先清空则燃烧失去后续燃料。没有燃烧期间禁止引爆或禁止补种的总锁。同一次真实释放对同一目标最多一次。不是燎里的0.36×S，也不套焚天的1.2指数，不叠加旧BURST每档0.04。\n陨石组合里只绑定中心父段，不自动进震环。恶魂炮击使用已有聚能、本行和已有爆燃，不另建组合行，也不留下陨石方块。",
        "o": "命中点一次短促火团，强度随当时花掉的火种增加。没有余额时只有载体自己的命中。",
        "dv": "0.2×S×(1+F)是引爆草稿，待制作前仍以这份草稿录入，不是燎里。费用未定。" + footer,
        "ej": "同一次真实释放对同一目标最多一次。余额为0不扣资源，载体伤害仍在。",
        "ek": "消费当时剩余全部。火伤0.2×S×(1+F)。与燃烧按发生顺序争用同一余额，不建第二套燃料池。",
    },
    {
        "name": "幽冥", "id": "fire_nether_gloom",
        "C": "通用", "D": "火", "J": "是", "AB": "否",
        "P": "规则", "Q": "持续",
        "DW": "是", "DX": "火种", "DZ": "增加", "EA": "领域内敌对活物", "EB": "领域周期",
        "EI": "每次触发", "EL": "N/DV", "EM": "不自建领域，不减速，不另建魂系资源",
        "n": "领域附魔，替换已被否决的魂焰减速。领域内不断冒出魂火，给领域内敌对生物增加火种。领域内的火系术式表现为蓝火。不给减速，不新增魂系资源，不附带免费额外伤害。它依附领域载体，范围和时长继承载体，不自己凭空创建领域。灵魂火地带的载体是灼地，组合为地表定位、幽冥、灼地，不另建组合行。蓝火只是表现变化，不改变伤害归属或免疫。\n正在燃烧的目标也可以获得新增火种。火种进入同一个余额后，尚未结束的燃烧可以消耗它，不因此刷新燃烧的启动倍率或时长。产种量、周期和重叠策略用户尚未最终确认。",
        "o": "领域内不断冒出蓝色魂火。领域里的火系术式表现为蓝火，不出现减速痕迹，不放置灵魂沙或原版灵魂火。",
        "dv": "曾建议每20tick基础加5、重叠取强，尚待定稿。产种量和周期没有写入数值格。魂焰的40tick、20%减速已否决，不登记魂焰。" + footer,
        "ej": "只作用于领域载体内的敌对生物。范围和时长继承载体。",
        "ek": "产种量、周期和重叠策略待定。新增火种进入同一余额，存活燃烧可以消耗。不减速，不另建魂系资源，不附带免费伤害。",
    },
    {
        "name": "崩地", "id": "fire_break_ground",
        "C": "通用", "D": "火", "J": "是", "AB": "否",
        "P": "规则", "Q": "触发",
        "n": "让适用攻击在有效地形碰撞时执行一次方块破坏，形成撞击坑。不额外生成生物伤害，不通过世界爆炸再补一轮伤害。遵守服务器统一的术式地形破坏开关和不可破坏方块规则。附在陨岩上时，每颗实际陨岩只执行一次；先撞空中实体的残骸，等真正接地后再执行。没有装崩地的陨岩仍有中心攻击和方块残留，但不挖坑。关闭统一开关后停止破坏已有方块，不关闭陨岩本体、命中接续、伤害或陨石残留。开关首版默认开启。两轮生物判定不在挖坑之后扩大到原本被挡住的目标。",
        "o": "撞击处出现一次坑，不另起爆炸，也不额外打出一轮伤害。",
        "dv": "坑形、基础半径、深度，以及和通用体积增幅的关系都未定，数值格留空。统一开关以后要核对既有术式是否都接入，本行不假装已经接通所有旧术式。" + footer,
    },
]


def fail(message):
    raise SystemExit(message)


def sha256(blob):
    return hashlib.sha256(blob).hexdigest().upper()


def col_index(letters):
    index = 0
    for char in letters:
        index = index * 26 + ord(char) - 64
    return index


def xml_text(value):
    return html.escape(str(value), quote=False)


def cell_xml(column, number, kind, value):
    ref = f"{column}{number}"
    if kind == "n":
        return f'<c r="{ref}" s="15" t="n"><v>{value}</v></c>'
    if kind == "i":
        space = ' xml:space="preserve"' if "\n" in str(value) else ""
        return f'<c r="{ref}" s="15" t="inlineStr"><is><t{space}>{xml_text(value)}</t></is></c>'
    style = "16" if column in ("B", "N", "O", "DV") else "15"
    space = ' xml:space="preserve"' if "\n" in str(value) else ""
    return f'<c r="{ref}" s="{style}" t="str"><v{space}>{xml_text(value)}</v></c>'


def row_xml(number, spec):
    values = {
        "A": ("s", spec["name"]),
        "B": ("s", spec["id"]),
        "N": ("s", spec["n"]),
        "O": ("s", spec["o"]),
        "DV": ("s", spec["dv"]),
    }
    for column in (
        "C", "D", "J", "M", "P", "Q", "R", "S", "AB", "AH", "AK",
        "DW", "DX", "DZ", "EA", "EB", "EC", "EE", "EI", "EJ", "EK", "EL", "EM",
    ):
        if column in spec:
            kind = "i" if column >= "DW" else "s"
            values[column] = (kind, spec[column])
    if "ej" in spec:
        values["EJ"] = ("i", spec["ej"])
    if "ek" in spec:
        values["EK"] = ("i", spec["ek"])
    for column in ("E", "U", "AS", "BV", "BW", "EG"):
        if column in spec:
            values[column] = ("n", spec[column])
    parts = [cell_xml(column, number, kind, value) for column, (kind, value) in sorted(values.items(), key=lambda item: col_index(item[0]))]
    return f'<row r="{number}" ht="225" customHeight="1">{"".join(parts)}</row>'


def rows_of(sheet_xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", sheet_xml)


def text_of(xml, column, number):
    match = re.search(rf'<c r="{column}{number}"[^>]*>([\s\S]*?)</c>', xml)
    if not match:
        return None
    inner = match.group(1)
    inline = re.search(r"<t(?: [^>]*)?>([\s\S]*)</t>", inner)
    if inline:
        return html.unescape(inline.group(1))
    value = re.search(r"<v(?: [^>]*)?>([\s\S]*)</v>", inner)
    return html.unescape(value.group(1)) if value else None


if len(rows) != 13 or len({item["id"] for item in rows}) != 13:
    fail("Row spec is not 13 unique ids")
live_bytes = live.read_bytes()
if sha256(live_bytes) != expected:
    fail(f"Live hash changed: {sha256(live_bytes)}")
stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
backup = batch / "备份" / f"入表-术式制作表-{stamp}-1.xlsx"
output = batch / "入表-candidate-preserved.xlsx"
backup.parent.mkdir(exist_ok=True)
backup.write_bytes(live_bytes)
(batch / "入表-rows.json").write_text(json.dumps(rows, ensure_ascii=False, indent=2), encoding="utf-8")

with zipfile.ZipFile(backup) as archive:
    order = archive.infolist()
    original = {info.filename: archive.read(info.filename) for info in order}
sheet2 = original["xl/worksheets/sheet2.xml"].decode("utf-8")
existing = rows_of(sheet2)
if [int(re.search(r'\br="(\d+)"', row).group(1)) for row in existing] != [1, 2]:
    fail("待制作 is not empty")
built = [row_xml(index + 3, spec) for index, spec in enumerate(rows)]
match = re.search(r"(<sheetData>)([\s\S]*)(</sheetData>)", sheet2)
sheet2_out = sheet2[:match.start(2)] + "".join(existing + built) + sheet2[match.end(2):]
if sheet2_out.count('ref="A1:EN2"') != 1:
    fail("Dimension anchor missing")
sheet2_out = sheet2_out.replace('ref="A1:EN2"', 'ref="A1:EN15"', 1)
uncovered = []
for validation in re.findall(r'<dataValidation\b[^>]*sqref="([^"]+)"', sheet2_out):
    for ref in validation.split():
        bounds = re.match(r"([A-Z]+)(\d+):([A-Z]+)(\d+)$", ref)
        if bounds and int(bounds.group(2)) <= 15 and int(bounds.group(4)) < 15:
            uncovered.append(ref)
if uncovered:
    fail(f"Validation does not cover new rows: {uncovered}")
changed = {"xl/worksheets/sheet2.xml": sheet2_out.encode("utf-8")}
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output, "w") as dst:
    for info in order:
        dst.writestr(info, changed.get(info.filename, src.read(info.filename)))
with zipfile.ZipFile(backup) as src, zipfile.ZipFile(output) as dst:
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != ["xl/worksheets/sheet2.xml"]:
        fail(f"Unexpected parts: {different}")
    pending = dst.read("xl/worksheets/sheet2.xml").decode("utf-8")
    for index, spec in enumerate(rows):
        number = index + 3
        if text_of(pending, "A", number) != spec["name"] or text_of(pending, "B", number) != spec["id"]:
            fail(f"Identity mismatch at {number}")
        if text_of(pending, "N", number) != spec["n"] or text_of(pending, "O", number) != spec["o"]:
            fail(f"N/O mismatch at {number}")
        if spec.get("E") is not None and text_of(pending, "E", number) != str(spec["E"]):
            fail(f"Damage mismatch at {number}")
        if spec["id"] == "fire_nether_gloom" and text_of(pending, "EC", number) is not None:
            fail("幽冥 amount was filled")
        if spec["id"] == "fire_kindle" and text_of(pending, "EG", number) != "10":
            fail("引燃 period missing")
    ids = []
    counts = {}
    for title, part in (
        ("已完成", "xl/worksheets/sheet1.xml"),
        ("待制作", "xl/worksheets/sheet2.xml"),
        ("制作中", "xl/worksheets/sheet3.xml"),
        ("待验收", "xl/worksheets/sheet4.xml"),
    ):
        xml = dst.read(part).decode("utf-8")
        page = []
        for row in rows_of(xml):
            number = int(re.search(r'\br="(\d+)"', row).group(1))
            if number >= 3:
                code = text_of(row, "B", number)
                if code:
                    page.append(code)
        counts[title] = len(page)
        ids.extend(page)
    if counts != {"已完成": 40, "待制作": 13, "制作中": 4, "待验收": 2}:
        fail(f"Counts {counts}")
    if len(ids) != 59 or len(set(ids)) != 59:
        fail(f"Ids {len(ids)} unique {len(set(ids))}")
    names = [text_of(pending, "A", index + 3) for index in range(13)]
    if "魂焰" in names:
        fail("魂焰 was entered")
summary = {
    "submitted": False,
    "baselineSha256": expected,
    "backup": str(backup),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "counts": counts,
    "rows": {spec["id"]: index + 3 for index, spec in enumerate(rows)},
}
(batch / "入表-summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"backup": backup.name, "candidateSha256": summary["candidateSha256"], "counts": counts}, ensure_ascii=False))
