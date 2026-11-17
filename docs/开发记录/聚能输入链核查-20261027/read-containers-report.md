## 专项B：领域 / 护体容器读取报告

**结论（确定事实）**
- 现有领域、护体容器都没有能量、倍率入口。范围、寿命、效果强度全是定义里的字面量或固定策略常量。
- 聚能倍率若从释放链传入，最早在容器入口丢失；领域在几何生成前，护体在 `GuardDefinition` 构造前。
- 领域尺寸与寿命的客户端同源链路基本完整（同步 AABB、`visualEnd`）；护体半径同源，band 只进判定、不进渲染，法线有三套来源。
- 材料无法判定领域、护体是否属于元素术式；元素标记不在给定文件内。

**领域链**
`DomainDefinition.java:19-38` 只保存 placer、费用、寿命、原生策略、aura、边界、视觉；`mistRain()` 40-47 写死 7×7×8、40/8/20、200 tick；`deepPressure()` 90-97 写死 7×7×3、同样费用和寿命。`place(player)` 49-51 与 `Placer` 99-102 只接收玩家，没有倍率参数。

`DomainService.java:48-88` 是唯一发布路径：bill=初始费×份数（71），几何由 `definition.place(player)` 生成（78），可选按接触点平移整盒（79-82），再 `new DomainInstance`（87）。份数只重复同一个盒子，没有分散（77-88）。`tick` 108-133 只做到期、主人死亡/换维度关闭、维护扣费；没有重放或改尺寸。

`DomainInstance.java:29-40` 在构造时锁定 `expiresAt=startedAt+lifetimeTicks`（35）、`visualOrigin/visualEnd`（36-37），几何字段 final。`touches` 用 `geometry.intersects`（134-136），`queryBox` 外扩 1 格（144-146）。`auraDamage` 直取定义（88-90），无缩放。

`DomainPulses.java:28-52` 按 `DomainNativePolicy` 的固定值发潮湿、减速、水势：`grantWet` 54-67、减速 90-116、`momentumInside` 118-141，全部是常量，没有能量参数。策略参数的具体字段映射需 `DomainNativePolicy` 源码，材料未给。

`DomainSyncPacket.java:116-122` 只发 AABB、视觉/边界序号、`visualOrigin/visualEnd`、`animSeed`，无能量或 aura 半径字段；`broadcast` 66-74、`sendSnapshot` 76-86。`DomainClientState.java:22-36` 只给本地玩家/同乘载具做移动预测，`active` 57-61 用 `gameTime < visualEnd`。三个领域渲染器只给了匹配参数的空片段，无法核实体积、雾雨、环的具体尺寸来源。

**护体链**
`GuardDefinition.java:20-44` 全部是固定参数：`personalRing` 为 200 tick、生成 8、维护 2/20t、`unloadRatio=0.20`、半径 0.9、band 0.12、`bodyHeight=0.55`、倾角 0、可偏转。没有能量字段或由能量派生半径的构造。

`GuardStates.java:38-57` 排程只带 fireTick、waveKey；`activate` 68-111 先按 waveKey 去重（72），扣生成费（75）。已存在同类护体时走 83-93：重置 `expiresAt`、`revision+1`、维度，重新挂偏转，`guard` 定义不替换，`animationStart` 和 `nextUpkeepAt` 不重置，客户端不会重启动画（168-192）。新建走 94-110。

寿命与维护在 `tick` 137-172：到期、主人死亡/换维度、维护失败都直接移除。`unloadRatio` 113-123 把非正/非有限值归零，上限 0.6。

实际偏转几何由 `attachDeflection` 312-329 创建：`RingDeflectionHost` 用 `guard.radius()` 与 `band()*0.5`。`LiveRing` 343-401 中心为 `owner.position + bbHeight*bodyHeight`（369-375），法线用 tick 插值（377-384）。接触几何 `GuardTouch` 516-602 用 `towardNormal` 直接归一化（556-560），半径、`band*0.5+slop`（562-563），`queryBox` 为 `radius+band+2`（592-593）。`GuardPreview` 16-20 只做预警抬环，射程 8、候选 16、预判 4 tick，明确不改箭碰撞。

`GuardSyncPacket.java:427-449` 的 FULL/POSE 已带 `baseTilt/radius/band/bodyHeight` 和姿态插值参数，未带能量；`sendAround` 133-142 限 128 格。`GuardRingRenderer.java:54-84` 用 `radius` 缩放、`bodyHeight` 定中心；`band` 未参与渲染；法线在 86-97 用 `System.currentTimeMillis()` 墙钟插值。

**倍率最早丢失点**
- 领域：`DomainDefinition.Placer`/`place`（49-51、99-102）与 `DomainService.releaseWave`（48-88）没有能量参数，几何直接取 40-47、90-97 的字面量，这是第一断点。寿命第二断点在 `DomainInstance` 构造（35）且字段 final。尺寸在客户端不是断点，AABB 已同步；但若特效尺寸来自 `auraSpec` 而非盒子，`packetFor` 116-122 没有对应字段，属确定缺失。
- 护体：`GuardDefinition` 构造（20-39）与 `GuardStates.schedule/activate`（38、59-68）没有能量参数，`BodyGuard` 只持有共享定义（490-514），无实例缩放字段，这是第一断点。半径若只改实例值，`fullPacket` 427-437 能同步，但 `GuardTouch` 562 仍读定义半径，接触与渲染会分叉。

**刷新与能量消费**
领域每次释放都新建实例，同 tick 去重键为“玩家+类型+初始费+寿命+后缀”（64-70），不含能量与几何；不同能量、同费用的两次释放会互相吞掉。护体重放只续命、升 revision，不换参数，重复收生成费（75）。两者都只扣魔力：领域 40×份数 + 8/20t，护体 8 + 2/20t，没有独立能量池；`unloadRatio` 是比例而非能量输入。

**同源性**
中心公式三处一致：`GuardRingRenderer.java:59-61`、`LiveRing` 369-375、`GuardTouch` 561。半径经同步包同源；band 只在服务端接触/偏转使用。法线三源不一致：服务端 tick 插值（383）、接触直接 `towardNormal`（556）、客户端墙钟插值（86-97），姿态过渡期间判定与画面会短暂分叉。

**缺失材料 / 待核实**
- `DomainNativePolicy`、`DomainAuraSpec`、`AxisBoxGeometry`、`DomainVisual/Boundary`、`DomainSightStore`、三个领域渲染器完整源码。
- `RingDeflectionHost`、`RingBandGeometry`、`DeflectionHosts`、`ContactEnchantState`、`GuardPose`、`GuardClientLifecycle`、`WaterRingV8Math/Mesh`。
- 调用入口：谁调用 `DomainService.tryWave/tryWaveReserved`、`GuardStates.schedule/activateReserved/bindContact`；`ReleaseDefinitions` 中领域/护体是否登记 `energyScale`、是否带元素标记；`chainEnergyScale()` 的实际传播路径。材料没有这些，不能断言聚能当前是否已到达容器。

**与需求的冲突**
`docs/底座功能点.md:50` 与 `:58` 明确写“伤害、冷却、持续、距离、搜索和速度不乘能量”。用户新要求“持续时间随能量提高”，与现文档相反，需先裁决。未聚能基线应由现有常量保持：领域 7/7/8、7/7/3、200 tick；护体 200 tick、0.9/0.12/0.55、8 与 2/20t。

**需公共参数化的候选字段（仅列字段）**
领域：盒宽/深/高、`lifetimeTicks`、`initialCost/upkeepCost` 的计价基准、`DomainNativePolicy` 的量/周期/持续、`auraSpec` 的半径与持续时间。护体：`radius`、`band`、`bodyHeight`、`lifetimeTicks`、`unloadRatio`、维护周期。同步包需能区分同一术式的不同能量版本。