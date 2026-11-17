本轮定位结果：**空中组合失败有明确的源码缺陷：造水采用“完整内含格”，组合识别却向下采样，非整数底高时两者错层。地面入卷即溃败已追到具体关闭分支，但缺少当次余额、有效配置和实体碰撞盒，不能认定实际触发的是资源不足还是空间失败，也没有证据证明“三重必然有错”。**

本轮仅使用 `read_files/search`，未写源码、未编译、未启动游戏、未写表。前次配置读取中断属于不存在文件导致的读取失败。

**已证实的源码事实如下。**

1. **空中临时水可以放置，问题出在生成水层与识别水层不一致。**

   [StillWaterLease.java](/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/motion/StillWaterLease.java:132) 的 `contained()` 要求整个方块落在领域 AABB 内；`placeIfAir()` 没有“下方必须有实体方块”的条件。[StillDomainFluid.java](/D:/alianximod/src/main/java/com/bslx/XueXiMod/block/StillDomainFluid.java:39) 禁止流动，有效源水租约不会因为悬空而自行消失。

   但 [VolumeMotionRuntime.java](/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/motion/VolumeMotionRuntime.java:601) 的 `baseWater()`，以及同文件第 945 行的 `waterShared()`，都只检查 `floor(bottom)` 和下一层 `floor(bottom)-1`。

   以三个领域共同底高 **64.4**、深海默认高度 **3** 为例：

   | 项目 | 实际高度 |
   |---|---|
   | 深海领域 | `[64.4, 67.4]` |
   | 完整内含、允许生成水的方块层 | `65、66` |
   | 气泡与旋流检查的水层 | `64、63` |
   | 无天然水补充时的结果 | 挂载检查失败，不能形成水龙卷 |

   底高为整数 **64** 时，造水包含第 64 层，识别也检查第 64 层，因此能组合。这个差异直接对应“地面正常、空中不组合”，同样可能影响半砖等非整数高度位置。**缺陷与施法是否使用三重无必然关系。**

2. **用户列出的纯四格链，三成员共用冻结位置；不能把空中下落造成的位置漂移当作既定原因。**

   [CastExecutionSession.java](/D:/alianximod/src/main/java/com/bslx/XueXiMod/network/CastExecutionSession.java:176) 在普通释放开始时，把 `owner.position()` 和 `owner.getLookAngle()` 保存到同一个 `ActionContact`。成员执行时读取同一份 `point/direction`，领域分支将该点传给 `domainSpace()`。

   [DomainCastContext.java](/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/motion/DomainCastContext.java:54) 在没有空间索敌修饰时返回这个非空 `contactPoint`。[DomainService.java](/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainService.java:201) 再将当前玩家脚底生成的盒子平移到该点，抵消成员延迟期间玩家的位移。

   对所报链条，按现有执行路径：

   | 成员 | 计划释放时刻 | origin | aim 的实际作用 |
   |---|---:|---|---|
   | 深海重压 | `t₀` | 冻结脚底点 P | 领域保持竖直，不按视线旋转 |
   | 上升气泡 | `t₀+3` | 同一个 P | 同上 |
   | 旋流 | `t₀+6` | 同一个 P | 同上 |

   会话保存的方向相同，但领域几何不消费该方向；不能将其描述为三份各自重新瞄准。

   **另发现一个仅在带空间索敌时成立的问题：**领域分支在绑定 `group.batch` 之前就返回，而 `DomainCastContext.origin()` 的空间索敌分支会调用 `ReleaseBatch.sharedLanding()`。没有绑定批次时，它临时创建解析对象，读取当时的玩家位置、视线和目标，三个延迟成员可能得到不同落点。此缺口确实存在，但用户列出的链没有空间索敌，不能作为本次纯四格链的直接解释。

3. **气泡建立和旋流识别没有“必须同 tick 出生”的限制。**

   深海在 `DomainService.releaseWave()` 发布时同步调用 `StillWaterLease.fill()`；气泡领域创建不以当前有水为前置条件。每次运动处理的顺序是：

   `matchMounts → refreshColumn → processHost → 未挂载气泡独立上升`

   因此，在区块已加载、三个成员成功创建且支撑水检查成立时，先深海、后气泡、再旋流可以正常识别；运行器每 tick 重试匹配。调度器与领域 tick 的事件先后可能使识别晚一个处理周期，不能解释持续不组合。上述错层则会让每次重试都失败。

4. **水势来自施法者的持久钱包，释放名册只负责去重，没有给旋流另建初始库存。**

   [PlayerShuData.java](/D:/alianximod/src/main/java/com/bslx/XueXiMod/player/PlayerShuData.java:47) 的 `waterMomentum` 新数据默认为 **0**，既有玩家则读取持久值，范围为 0–100。三个术式没有开场赠送水势；深海的原生 `momentumAmount` 明确为 0。开域魔力付款不会变成水势。

   [ReleaseMotionLedger.java](/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/motion/ReleaseMotionLedger.java:63) 按 `releaseGroup + declarationSlot` 共用已结算、已抛出和控制权记录。同一旋流声明的复制份共享记录；不同声明不会仅因术式 ID 相同而合账。实际金额统一通过领域 `ownerId` 找到施法者后结算。

   [WaterMomentum.java](/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/momentum/WaterMomentum.java:119) 先检查可用额——存储余额减未提交预留——再一次写入：

   `最终余额 = 原余额 − 毛费用 + 返还`

   **没有先把余额写为 0、随后才返还的中间状态。**余额 10、三级目标收费 10 返 13，会直接得到 13；其他预留占用了可用额时，仍可能支付失败。

5. **地面入卷即溃败的直接代码入口已明确，但各入口目前共用关闭结果。**

   `VolumeMotionRuntime` 的关键分支是：

   | 分支 | 关闭条件 |
   |---|---|
   | `processHost()`，第 235 行 | 施法者存储水势不大于 0，空旋流也关闭 |
   | `steerGroup()` | 接触本体但身体尺寸放不下；或已受控身体发生相应阻塞 |
   | `acquire()`，第 388 行起 | 累计体积超限、找不到合法座位、支付失败，或完整结算后余额为 0 |
   | `deflectProjectile()` | 弹射物偏转支付失败，或支付后耗尽 |

   失败路径最终都调用 `DomainService.closeInstance(host)`，因此单凭“卷入便散”的画面不能区分原因。关闭旋流会同时终止其已挂载气泡，深海水域保留自己的生命周期。

   默认生物收费来自 [DomainDefinition.java](/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainDefinition.java:291)：

   | 入卷时状态 | 扣费 | 返还 |
   |---|---:|---:|
   | 独立旋流，任意湿度 | 10 | 0 |
   | 水龙卷，0 档 | 10 | 0 |
   | 水龙卷，1 档 | 8 | 0 |
   | 水龙卷，2 档 | 5 | 0 |
   | 水龙卷，3 档 | 10 | 13 |
   | 水龙卷，4 档 | 10 | 16 |

   扣费发生在**完整进入本体且通过占位检查后**，外围吸引不逐 tick 收入卷费；成功结算后的同一实体不重复扣返。

   深海默认从出生后第 **10 tick** 才开始原生施湿，每次 **4 点**、周期 **10 tick**；旋流计划在第 **6 tick** 出现。因此，原本干燥的实体若立即被接纳，不能保证已经享受湿度折扣，更不能保证达到三级返还。该时点安排本身没有违反所读批准文档。

6. **默认容量没有形成“一只普通人形实体必然超载”的数学结果。**

   默认本体半径 **2.5**，干地高度 **3**，组合高度 **8**，占用体积上限 **12**。体积算法是各边至少 0.25，再各加 0.1 间隙后相乘。

   若实体实际 AABB 为 `0.6×1.8×0.6`，计费体积为：

   `0.7×1.9×0.7 = 0.931 < 12`

   其尺寸也能通过默认柱体尺寸检查。漏斗窄底只是优先轨道，失败后仍尝试圆柱同心环及中心位置，不直接以漏斗外观宽度判超载。

   这不排除实际地形无合法座位、骑乘组并集过大、已有其他占位或配置降低容量。掉落物不扣水势但占体积；骑乘组按成员合计费用。运行器还排除施法者所在骑乘组、旁观和无物理等对象，普通其他玩家、生物及盟友没有统一敌对过滤。

   普通实体的气泡上升经 `FlowMotionIntents.compose()` 保证竖直位移下限；`WetnessFlight` 检查有效上升后跳过潮湿下压。未发现普通实体因“不会游泳”而被取消上升的条件，也没有下压直接关闭旋流的分支。

**尚需游戏验证的推断与证据边界。**

- **空中现象：高可信对应上述错层。**需要当次 P.y、三个领域 AABB 和支撑采样格的实际流体状态，才能证明用户那一次确实命中了该条件。非整数高度、仅靠本次生成水且底部无天然水时，源码链条已能确定失败。
- **地面现象：资源不足是明确可达解释，尚未证实是当次原因。**例如无其他预留时，余额 10 遇干燥实体会结算成 0 并立即关闭；余额 8 遇一档实体同样关闭。若余额原本就是 0，旋流应在首次处理时关闭，不必等待实体，因此不能用“默认 0”直接解释一个已经稳定存在、入卷才散的实例。
- 若当次余额充足且结算后仍为正，就应追查实际碰撞盒、骑乘成员、累计体积和选座失败，不能继续归因水势。
- `run/config` 定向搜索未找到相关术式、费用或容量键。前次已确认 `run/config/xueximod-balance.json` 不存在；**这不证明运行时使用默认值。**[BalanceDocuments.java](/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/balance/BalanceDocuments.java:31) 与 [BalanceLifecycle.java](/D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/balance/BalanceLifecycle.java:24) 表明，实际读取的是“当前世界根目录/serverconfig/xueximod-balance.json”，模板位于游戏目录的 `defaultconfigs`。本轮未取得测试世界的准确路径或有效配置快照，不能声称已核实实际覆盖。

**最小修复建议与精确文件路径。**

| 范围 | 建议 | 文件 |
|---|---|---|
| 已证实的空中错层 | 将 `waterShared()` 与 `baseWater()` 统一为基部支撑区域内的实际流体相交检查，覆盖非整数底高对应的首个内含水层；保留已有下方水支撑语义。只检查基部，不扫描整根柱体或扩大领域。 | `D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/motion/VolumeMotionRuntime.java` |
| 带空间索敌的通用落点缺口 | 在领域空间解析与发布期间绑定本组 `ReleaseBatch`，用 `finally` 恢复外层绑定，使延迟成员复用同组已解析落点。此项与用户当前纯四格链的错层原因分开处理。 | `D:/alianximod/src/main/java/com/bslx/XueXiMod/network/CastExecutionSession.java` |
| 地面入卷溃败的必要取证 | 给现有失败出口记录具体原因及结算/占位数据，优先在 `acquire()` 丢失失败原因之前记录；取得证据后再修实际误判。 | `D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/motion/VolumeMotionRuntime.java` |

空中修复无需改变 `StillWaterLease` 的完整内含放置规则，也无需统一贴地。当前证据不支持修改 `MulticastCompileAdapter.java`、增大范围、提高默认容量、降低收费，或更换水势钱包。

地面问题还精确缺少以下信息，建议一次失败记录同时采集：

- 旋流实例、释放组与声明槽位；关闭原因及对应 tick。
- 扣费前存储水势、可用水势、毛费用、返还、支付结果和最终余额。
- 入卷目标类型、湿度值/档位、骑乘成员、各 AABB、并集 AABB、已有占用体积、入卷体积和有效上限。
- 三个领域的实际 bounds、挂载对象、组合状态，以及测试世界生效的三术式配置值。

[既有设计裁决](/D:/alianximod/docs/开发记录/术式批量制作/水流组合-20261103/设计裁决-1.md) 明确要求毛费用付得起、最终余额为 0 关闭、真实空间不足立即溃败。若取证证明命中的就是这些条件，那么“空钱包也能持续卷入”“开场自动补势”或“入卷前立即升到高湿返还档”属于设计或平衡变化，不能作为此次故障修复擅自加入。