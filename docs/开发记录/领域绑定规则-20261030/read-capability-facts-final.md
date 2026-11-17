我先读这份任务说明，确认授权范围后再按它采集事实并交完整报告。用户已确认随机点、无目标朝向和开域冻结。我按能力查正伤攻击里仍会读准星、主手或按住的入口。只读补齐。未改文件，未做架构裁决，也未复审接触计费。正伤且非附魔，指 `Technique.baseDamage > 0` 且 `enchantment == false`，释放角色为攻击。冲刺有伤害但是动作角色，不计入这组。

## 已有声明字段

`Technique` 只有伤害、类型、属性、弹速、是否附魔，没有朝向、持剑、按住或蓄力字段。

`ReleaseDefinition` 上和这次有关的是：`acceptsTargetOverride`、`acceptsInitialAim`、`dedicatedInitialAim`、`acceptsPathOverride`、`trajectoryMutable`、`requiresHeldSword`、`manipulatesSwordEntities`、`requiresFreshPress`、`endsWhenReleased`、`chargeTicks`、`usesArcBand`。默认持剑为假、蓄力为 0、松手结束为假。全库没有任何实现把 `chargeTicks()` 返回成大于 0。

`CastFrame` 能携带 `origin`、`look`、`anchor`、`overrideGeometry`、`planeNormal`、`pathOverride`、`drop`、`tracking`。没有持剑、按住、蓄力或飞剑方向字段。

`TechniqueHitSpec` 有 `start`、`aim`、`anchor`、`lockThisCast`、`dropThisCast`、`keepFollowing`、`endsWhenReleased`、`releaseSlot`。`releaseSlot` 默认 -1。

## 准星

`SkillSegmentRunner.buildSegmentRunner` 在真正执行时取 `getLookAngle()` 和 `position()`（119–120 行）。`buildSegmentRunnerAt` 可以改传已经算好的方向和位置。即便如此，`runSegment` 在子链有实体锁定时仍调用 `LockTargetService.resolve(player)`（542–546 行），落点或龙卷风用 `crosshairCenter`，不可改弹道的落点用 `resolveAnchor`（572–580 行）。这三处都读实时准星。追踪规则本身不读准星：`TrackingSupport.resolveTarget` 只在落点 5 格内选最近生物（26–41 行）。

生成器只在 `overrideGeometry` 时采用帧上的点（`TechniqueHitSpec` 213–248 行）。静止斩、函数轨迹、直线、垂落、跟随都能被 `origin`/`look` 盖住。`applySpec` 仅在 `start` 或 `aim` 为空时才回眼睛和准星（518–521 行）。`StillAimMotion` 同样只在锚点或朝向为空时回退（23–24 行）。因此横竖劈、刺击、断潮、岩槌、让锋、垂丝、飞弹、金针、火球、爆燃、寄生种子、持续试射，只要调用前写上覆盖几何和域内方向，随后不再读准星。垂丝只在没有覆盖时自己读准星（`WeepingCurtainEffect` 42–55 行）。焚天、燎原已经优先用 `frame.origin`（`FentianEffect` 36–38 行，`LiaoyuanEffect` 29–31 行）。缺月弧的 `bakeArc` 优先 `frame.look`，没有才用采样准星（464–478 行）；`forAttack` 在无落点、无交错时不写 `frame.look`（107–109 行），这时靠 `cast.getLook()`。

盖不住的回退有四处。燎里把 `keepFollowing` 设为真（`LiaoliEffect` 52 行），生成器随后清掉 `start` 和 `anchor`（309–313 行），`FollowCasterMotion` 每拍改回眼睛和准星（23–30 行）。热光束不设 `keepFollowing`，覆盖后 `start` 非空就会 `pinned`，停在冻结点（15–17 行）。幻剑悬停每拍把 `flyDirection` 写成主人准星；`lockAim` 只在已有标记时改向标记，没有标记仍是准星（`SwordEntity` 363–376 行）。`phantom()` 不保存 `frame.look`。万剑归宗没有标记时，`launchDirection` 用主人准星（`SwordVortex` 183–194 行）。空间切割无条件 `resolveAnchor`（`SpaceCutEffect` 32–34 行）；覆盖几何能改写最后锚点，但这次准星读取仍会发生。

## 主手、按住、蓄力、操纵剑

持剑不能用帧或规格覆盖。`requiresHeldSword` 为真的正伤只有燎里，以及 `swordForm()` 的缺月（`WaterReleaseDefinition` 68–71 行，`WaterTechniques` 23–26 行）。`buildSegmentRunner` 116 行和 `commitNow` 233 行读的是当时主手，不是参数里的 `weapon`。燎里存续中再读一次主手（`TechniqueHitEntity` 877–879 行）。幻剑读的是传入的 `weapon`（`PhantomSwordEffect` 21 行），调用方可以传冻结副本；这不是定义字段。

按住同样不能用帧关掉。正伤里只有燎里 `endsWhenReleased` 和 `requiresFreshPress` 为真。效果把 `batch.sustainSlot()` 写入 `releaseSlot`（57 行）。`releaseSlot < 0` 会被当成已经松开并立刻结束（881 行）。`requiresFreshPress` 只影响 `HoldRepeat` 的连发（86–98 行），不阻止别人直接调用 `runSegment`。

蓄力的接点只有 `definition.chargeTicks()`（`SkillSegmentRunner` 473、243 行）。`CastFrame` 没有对应字段。当前没有正伤攻击进入这段。

操纵剑没有可冻结方向的共享字段。正伤且 `manipulatesSwordEntities` 为真的是万剑归宗。幻剑该标志为假，但悬停仍走上面的准星重写。

双重释放的两段都进入同一个 `runSegment`，没有第二段专用的准星豁免。顺序在链的调度，不在效果里。

## 直接实现时要动的入口

不读准星、并让域内目标或随机方向生效，现有字段盖不住、因此要改的是：`SkillSegmentRunner` 119–120 行和 542–580 行；`SpaceCutEffect` 32–34 行；`LiaoliEffect` 52 行、`TechniqueHitSpec` 309–313 行、`FollowCasterMotion` 23–30 行；`SwordEntity` 363–376 行；`SwordVortex` 183–194 行。其余正伤命中体只要在调用前写好 `overrideGeometry`、`origin` 和 `look`，就不会再回准星。

持剑和松手结束没有可覆盖字段。沿用定义的话，燎里和缺月仍看触发当时的主手；燎里没有按住槽会立刻结束。蓄力目前没有正伤声明需要改。
