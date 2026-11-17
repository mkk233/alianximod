我先通读这份任务说明，确认授权范围后再按它收集并回报。任务是只读定位现有纯数值内核，我按授权范围查几何、瞄准、运动、命中和性能计数相关代码。这是步骤 4 的只读事实报告，不是架构批准，也不是正确性审查。授权范围内没有已接入热点的数值数组批处理，也没有 native 调用。下面的最小集合是现有热点已经在消费、并且可以在新文件里实现后由现有方法转调的纯数值操作。

## 现有公共计算、数组批处理和 native

`src/main/java` 下没有 `native` 方法，也没有 `System.load`。仓库里还没有 `shu/compute` 或 `native/compute`。

已有的纯 Java 公共数值函数都是单次调用，输入输出是 `Vec3`、`ArcVec` 或少量 `double`，不是一批样本的数组：

- `TrajectorySampler.sample`：一条轨迹一个 `Sample(position, direction)`。
- `TrackingSupport.turnToward`、`SwordTrackingStep.plan`：一把剑或一个攻击体一步。
- `MeleeCorridorMath.overlaps`、`contactDistance`、`blocked`：一个目标盒一次。
- `ProgressArcBand.sliceBroadBounds`、`point`、`segmentCount`：一条弧、一段进度。
- `SizedBoxHitbox.holdsPoint`、`acceptContact`、`sweptSegmentHits`：一个点或一条线段。
- `ArcVec`：东、上、南三个 `double`，对应世界 X、Y、Z。
- `CohortLaneLayout`：视觉分槽，调用方都在客户端渲染。
- `AxisBoxGeometry.fullInterval` 返回长度 2 的 `double[]`；`samplePoint` 会推进 `RandomSource`。

内部小数组只服务一次求交，例如弧带的 6 元包围、`[0,1]` 裁剪窗口。没有把 N 个实体的轨迹或粗筛打成一块数组再算的入口。

`PerfMetric` / `PerfCounter` 只命名热点，本身不算数值。和这份报告直接对应的是 `GEOMETRY`、`MOTION`、`NARROW_PHASE`、`PHANTOM_SWORD_SPAWN`、`HIT_ENTITY_UPDATE`、`SWORD_UPDATE`。`GEOMETRY` 指向 `CastFrame.forAttack`、`volumePoint`、`aimFor`，该文件不在本次可读范围。

## 可在独占文件接线的最小真实操作

新实现放在尚未存在的独占计算文件里。现有方法保留签名并转调，热点调用方可以不改。空接口不算接入。

### 1. 闭合轨迹采样

路径：`shu/technique/release/TrajectorySampler.java` 的 `sample`。

输入：轨迹枚举、起点、前向、速度、寿命 tick、年龄 tick、可选目标点、`currentLook`、`functionId`。另有一个 `followCurrentLook` 参数，方法体没有读取它。输出：位置 3 元、方向 3 元。

`normalized` 在 `null` 或 `lengthSqr < 1.0E-8` 时失败，失败前向变成 `(0,0,1)`。这里不检查非有限数。`progress` 只在 `FUNCTION` 使用：寿命 `<= 0` 则为 0，否则夹到 `[0,1]`。

- `STRAIGHT`：有目标点时方向是 `normalize(目标点 - 起点)`，归一失败就用前向。位置是 `起点 + 方向 * speed * ageTicks`。没有目标点时用归一前向，同一公式。
- `DROP`：`fall = 0.5 * 0.08 * ageTicks * ageTicks`。位置只改 Y，X/Z 留在起点。方向固定 `(0,-1,0)`。目标点、速度、前向都不参与。
- `FOLLOW_LOOK`：先归一 `currentLook`，失败用前向。有目标点时再改成 `normalize(目标点 - 起点)`；这一步失败则保留非空的 `currentLook`（不再归一），两者都没有才用前向。位置始终是起点。
- `VORTEX`：`mouthRadius=5`，`columnHeight=12`，`turnCount=3`，`spiralTicks = max(8, lifetimeTicks * 2 / 3)`。`ageTicks >= spiralTicks` 时停在起点，方向是水平前向，水平长度失败则为 `(0,0,1)`。否则 `phase = atan2(前向.z, 前向.x)`，`angle = phase + progress * turnCount * 2π`，半径和高度随进度收到 0。切向由角速度、缩径率和下降率组成，归一失败则方向为 `(0,-1,0)`。速度不参与。
- `FUNCTION`：查表。已注册的 `water_weeping` 直接返回起点。方向用当前进度和 `progress - 0.04`（不低于 0）两个位置的差，差为零则用前向。未注册时退回直线公式。`fitVolume` 会把实体和 `ServerLevel` 交给 `WeepingCurtain::fit`，这一步不是纯计算。

没有 RNG。批量大小是每个攻击体每个服务端 tick 一次，不是数组。`TechniqueHitEntity.tick` 先把 `age` 加一，再调用 `motion.tick`。

活对象在调用方，不在采样公式里。`ProjectileMotion`、`DropMotion`、`FollowCasterMotion` 可能先读锁定目标或施法者视线，再把数字传入。`FunctionMotion.groundBelow` 用 `level.clip` 向下 48 格；没打到方块，或落点比起点低不足 0.05，就传 `null`。`TrackingMotion` 在尚未咬住目标时把采样位置直接 `placeAt`；咬住之后只用相邻两次采样位置的距离当步长，沿转向后的方向走，不回到采样曲线。

当前调用：

- `ProjectileMotion` → `STRAIGHT`
- `DropMotion` → `DROP`（`FollowCasterMotion` 在垂落时转调它）
- `VortexMotion` → `VORTEX`
- `FunctionMotion` → `FUNCTION`，随后 `fitVolume`
- `TrackingMotion`：路径 `DROP`、`VORTEX` 保持原样，其余当 `STRAIGHT`；内部采样不传目标点

装配在 `TechniqueHitEntity`：陷阱用 `TrapFallMotion`；否则 `TechniqueHitKinds.motion`。`tracking` 且路径是直线、垂落或涡旋时换成 `TrackingMotion`。`TechniqueHitKinds` 把 `SWING` 配成 `StillAimMotion`。`AttachedSwingMotion` 没有构造调用。`HitVolumes` 里也有 `sample`，但全仓没有 `HitVolumes.staticBox`、`movingBox`、`beam` 的调用方，不能算现有热点。

必须改的活跃共享文件只有 `TrajectorySampler.java` 这一处转调。`FUNCTION` 回调和 `fitVolume` 留在原处。

### 2. 有界转向和追踪步长

路径：`TrackingSupport.turnToward`，以及 `SwordTrackingStep.plan`。

`turnToward` 输入两个向量，输出单位方向。非有限或长度 `< 1.0E-8` 视为无效。当前方向无效时用目标方向，目标也无效则 `(0,0,1)`。点积夹到 `[-1,1]`。夹角 `<= 12°`（含相等）时直接返回目标方向。否则沿切向转满 12°。切向退化时，`|current.y| < 0.9` 用世界上方向，否则用东向，再叉乘。

`plan` 再限制步长。基础速度非有限或 `<= 0` 则速度为 0。目标向量无效、长度 `< 1.0E-8`，或转向后点积非有限，步长为 0，方向仍给出。剩余夹角 `>= 90°` 时步长为 0。剩余夹角 `<= 1.0E-4` 时步长为 `min(速度, 距离)`。否则步长为 `min(速度, 0.8 * 距离 * sin(12°)) * max(cos(剩余角), 0)`，再夹到 `[0, 速度]`。

没有 RNG。一只追踪攻击体或一把剑每个 tick 一次。

`TrackingSupport.resolveTarget` 不是纯计算：自动瞄准只重验冻结目标，失败就返回空，不再空间搜索；否则先认 UUID，再在 5 格膨胀盒里查询，并要求中心距离平方 `<= 25`。距离相同留 `Entity.getId` 更小的一个。查询和这个同值规则必须留在 Java。

调用方：`TrackingMotion` 直接调用 `turnToward`；`SwordVortex.steer` 调用 `plan`。剑的基础速度在 `SwordEntityMotion.flyStep`：幻剑 2.5，真剑 1.2。

转调文件：`TrackingSupport.java`、`SwordTrackingStep.java`。

### 3. 攻击体粗筛

三处都是单目标或单弧，外面的候选列表来自 `LivingCandidateQueries`，不能并进纯计算。

`SizedBoxHitbox.broadPhase`：`reach = max(length, width) + 1`，用上一位置到当前位置的盒，按 `(reach, max(hitHeight, 1), reach)` 膨胀。每个非陷阱、已到 `hitReadyAge` 的攻击体 tick 调用一次，接着做实体查询。窄判定仍走 `contains`。

`MeleeCorridorMath` 已是纯 `double` 函数。`overlaps` 把目标盒八个角投影成沿前向、横向、竖向的区间；前向长度 `< 1.0E-8`、长度 `<= 0` 或宽高为负则不相交。沿前向碰到 `[0, length]`，横向、竖向落在半宽半高加投影余量内才相交。`contactDistance` 取靠近射线的一端，负值收成 0。`blocked` 在接触距离非有限或为负时挡住；障碍距离非有限时不挡；`障碍 + 1.0E-4 < 接触` 才挡。`MeleeCorridor.hits` 在数学通过后仍做 `level.clip`，这段留在 Java。入口是光束且 `clipsSolids` 的 `SizedBoxHitbox.contains`。

`ProgressArcBand.sliceBroadBounds`：由弧快照和进度区间得到 6 元轴对齐界（低东、低上、低南、高东、高上、高南）。`TechniqueHitEntity.tickArcBand` 在进度有增长时用它做查询盒，之后才对每个候选调用 `intersectsSlice`。段数先按弓高估计，再至少覆盖 `π/6`，最后不超过 256。径向刃片走另一套界。`advance` 用 `min(1, age / max(1, activeTicks))`，没有进度增长则 `swept=false`，不产生这次命中。窄判定和 `RadialBladeVolume` 留在原方法。

没有 RNG。批量就是这一拍活着的攻击体，外加每条弧最多 256 段。

转调文件：`SizedBoxHitbox.java`、`MeleeCorridor.java`（只转调已有数学，`clip` 不动）、`ProgressArcBand.java` 的粗界部分。`TechniqueHitEntity.java` 可以不改。

其余命中盒也是单目标，且读取实体或施法者：`BoxHitbox` 轴对齐相交；`SphereHitbox` 半径至少 0.3，粗盒再加 2，窄判定看膨胀盒是否含端点或切线段；`ObbHitbox` 只绕 Y 轴；`ArcHitbox` 用水平半径、高度和半角，水平距离 `< 1.0E-4` 时不再比角度。它们可以沿用同一转调方式，但不是比上面三项更大的批量热点。

### 4. 剑的位移数字

这些被 `SWORD_UPDATE` 包住的 `flyStep` 和 `returnTick` 消费。世界查询、伤害和物品回收留在原方法。

`SwordEntityMotion.approachPoint`：水平距离 `< 1.0E-4` 时，角度取 `(sword.getId() & 7) * π/4`，不是随机。否则沿当前水平方向。落点在眼睛高度、水平外侧 1 格。`startReturning` 写一次；`returnTick` 在主人仍在时每拍重算。

`returnTick` 的步长：距离 `<= 0.4` 时直接放到落点。否则步长 `min(1.5, 距离)`，方向是当前位置到落点的整段向量，含垂直分量。代码里没有单独的先抬高阶段。

`plantTipDirection`：来向长度平方 `< 1.0E-6` 时用朝内法线。与法线点积 `< 0` 才反向，点积为 0 保持。`plant` 把中心放在表面上（尖端内嵌 0.5，中心到尖端 0.5）。

`SwordEntityMotion.aim`：长度平方 `< 1.0E-6` 则不改朝向。偏航是 `atan2(z, x) * 180/π - 90`，俯仰是 `-atan2(y, 水平长) * 180/π`。

`SwordVortex.assignOrbit` 按这个顺序消费 `sword.random.nextDouble()` 四次：仰角扰动、方位扰动、半径、起伏相位。仰角基值在一把剑时为 0，多把时为 `1 - 2*slot/(count-1)`，再加 `(roll-0.5)*0.45`，夹到 `[-1,1]`。方位是 `slot * π * (3-sqrt(5))` 再加 `(roll-0.5)*1.1`。半径 `4 + roll*1.4`。相位 `roll*2π`。

`place` 不再取随机：每 tick 转 12°，水平半径 `4 + (orbitRadius-4)*(0.42+0.58*ringFactor)`，`ringFactor = sqrt(max(0, 1-elevation²))`，起伏 `sin(age*0.18+phase)*0.35`，高度为锚点半身附近 `+1 + elevation*1.8 + bob`。锚点空时用主人脚位置加半个身高。

`flyStep` 在未追踪时沿 `flyDirection * speed`。真剑在非追踪、非归航且飞行 tick `>= 40` 后每拍给速度加 `(0,-0.08,0)`。幻剑飞行 tick `>= 80` 或路程 `>= 64` 时移除。这些阈值和重力必须留在原调用顺序里。`hurtAlong` 的窄判定是：线段未切到盒子，并且目标盒膨胀 0.2 后不含终点，才算未中；命中人数到 5 停止。伤害和查询不是纯计算。

`planWave` 按 `vortexSlot` 升序取前若干把。100 把以内每 20 把把同时发射数翻倍，超过后每 100 把再翻倍，组数不超过 16，结果为 `1 << groups` 再不超过剩余剑数；组数 `<= 0` 时为 1。任一剩余剑的 `vortexCount >= 100` 时，波次不低于 `simultaneousCount(99)`。这是实体挑选，不是位置公式。

转调文件：`SwordEntityMotion.java`、`SwordVortex.java`。`SwordEntity.returnTick` 只保留回收、伤害和 `setPos`。

## 必要文件

独占新文件之外，转调需要：

- `TrajectorySampler.java`
- `TrackingSupport.java`
- `SwordTrackingStep.java`
- `SizedBoxHitbox.java`
- `MeleeCorridor.java`
- `MeleeCorridorMath.java`（已是纯实现，可成为 Java 后端本体）
- `ProgressArcBand.java`
- `ArcVec.java`
- `SwordEntityMotion.java`
- `SwordVortex.java`

保持调用、本次不必为了接线去改的消费方：`ProjectileMotion.java`、`DropMotion.java`、`VortexMotion.java`、`FunctionMotion.java`、`FollowCasterMotion.java`、`TrackingMotion.java`、`TechniqueHitKinds.java`、`TechniqueHitEntity.java`、`SwordEntity.java` 的 `fly` 和 `returnTick` 外壳。

`WaterCurveFunctions.java` 的 `water_weeping` 必须继续返回起点。`WeepingCurtain` 的体积贴合不进入纯计算。

## 会与多重释放底座冲突的文件

这些文件带有网络链、释放运行、发布回执或领域捕获，本次不能靠改它们来接线：

- `CastFrame.java`：`forAttack`、`volumePoint`、`aimFor`、`vortexPhase`，以及自动源点钳制
- `PublicationAttempt.java` 和 `bind/` 下的发布、绑定
- `DomainService.java`、`DomainBindingRuntime.java`，以及会把几何交回这两处的领域调用
- `BarrierService.java`、`GuardStates.java`
- 网络包和释放运行链

同一方法里已经夹着这些调用、改了就会碰到底座的文件：

- `PhantomSwordEffect.java`：生成循环里调用 `PublicationAttempt` 和 `CastFrame`
- `SwordEntity.java` 的 `beginVortex`：同时读 `CastFrame.current()` 和 `PublicationAttempt`
- `TechniqueHitSpec.java`：生成时调用 `aimFor(spawnIndex)`
- `TechniqueHitEntity.java`：若为了批量收集去改 tick，而不是在下游数学方法转调

`AxisBoxGeometry` 的调用链进入 `DomainService` 和 `DomainBindingRuntime`。它的 `samplePoint` 按 X、Y、Z 顺序各调用一次 `nextDouble`。这次不把它放进最小接线集合。

## 热点上的其余行为事实

幻剑生成数量来自 `SkillEntityCount.entityCount()`，默认 1，上限 4096。非自动且没有强制原点时，每把剑调用 `behind`：先 `nextDouble` 得到左右 `[-3,3]`，再 `nextDouble` 得到眼高以上 `[0,4]`，深度固定在眼后 2 格。水平视线长度 `< 1.0E-4` 时平面前向取 `(0,0,1)`。这是连续抽样，不是 7 个固定列。自动或强制原点时不走这组随机；`spawnIndex > 0` 时 Y 加 `(spawnIndex % 7) * 0.35 - 1.0`，自动路径随后还要钳制源点。索引 0 不加这项。

`CohortLaneLayout` 只被客户端画面调用。`DRAW_FRACTION = 0.6`。上升跨度大于横向跨度加 `1.0E-6` 时左右分，相等或更宽则上下分。它不在服务端 `MOTION` 或 `GEOMETRY` 计数里。

`StillAimMotion`、`TrapFallMotion` 直接改实体位置。前者读施法者或锚点；后者读方块碰撞。`TrapFallMotion` 每拍下降 0.35，落地时方向改为向上并标记插地。

## 尚缺

- `CastFrame.volumePoint`、`aimFor`、`vortexPhase` 的完整分支、随机消费顺序、`copyIndex` 与 `spawnIndex` 的混合方式。符号只确认方法存在并被生成调用。这是 `GEOMETRY` 计数的本体，也是三维交错和一分为三的生成布局入口，本次按授权没有通读。
- `TechniqueHitSpec` 的实际生成个数，以及交错、一分为三如何把序号送进上述方法。`MulticastParser` 只被符号检索碰到 `TRAJECTORY_SCATTER`，方法体未读。
- 另一写入会话是否已经改动 `TechniqueHitEntity`、`CastFrame` 或 `TechniqueHitSpec`。冲突名单来自授权边界和调用关系，不是对方差异的核对结果。
- 运行中的攻击体、剑和弧段数量。代码只给出每实例一次、弧段上限 256、测试倍率上限 4096。
- `WeepingCurtain.fit` 的方法体。已确认它接收实体、世界和地面点，所以不能进纯计算。
- `HitVolumes` 若只通过反射或尚未编译的入口被调用，这次静态调用图看不到。当前 Java 源码没有直接调用。

把召回改成先垂直抬高再水平飞回，或把幻剑门平面改成 7 个固定列，都会改变现有玩法。当前召回是朝眼睛高度外侧 1 格的单段位移，上限 1.5；幻剑随机位置是连续区间。
