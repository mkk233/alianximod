我先通读这份任务说明，确认授权范围后再按其中的要求执行。任务是只读定位。我先读确认版交接、分身所有权和源码入口，再沿引用把释放链路和渲染接口对齐。确认版和释放链路已经对上。接下来只读工作簿、bbmodel 元数据和当前 Gradle 进程，不改任何文件。只读定位已完成。没有改源码、资源、工作簿或任何文件，没有编译、打包或启动游戏。设计额度 2/2 已用完，这次只采集接口，实现复核仍是 0/3。

确认版是 `docs/资源库/过程归档/拔刀斩_v5-20261102/`。`animation.iaido_single_v5_left_waist`，1.6 秒，once，0.01 秒一帧，161 帧。bbmodel 为 free、format 5.0，20 个立方体，11 个组：`ActorRoot`、`Body`、`Head`、`ArmRight`、`ForearmRight`、`SwordHand`、`ArmLeft`、`ForearmLeft`、`LegRight`、`LegLeft`、`示意辅助_地面`。内嵌贴图 `iaido_atlas` 为 64×64。刀骨 `SwordHand` 的父级是 `ForearmRight`。关键帧文件里的 9 根骨是躯干、头、左右上臂、左右前臂、刀和两条腿；`Body` 与两条腿带位移，其余只有旋转。bbmodel 动画器还包含 `ActorRoot` 和地面参照。`manifest.json` 里 `status` 仍是 `preview_pending_user_confirmation`，`runtime.integrated` 为 false；当前授权以任务说明里的用户确认为准。

## 1. 稳定 ID 与释放链路

稳定 ID 是 `space_cut`，动作资源 ID 是 `xueximod:space_cut`。

- 术式：`ModTechniques.java` 105–108 行，`TechniqueType.SWORD`，基础伤害 4、冷却 20 tick、等级上限 20；354 行登记 `SpaceCutEffect`。
- 释放定义：`ReleaseDefinitions.java` 29 行，`cohortSword`，`HIT_VOLUME` / `TRIGGER` / `STRAIGHT`，寿命 10 tick，速度 0，厚度 0，`ChainRole.ATTACK`，`trajectoryMutable` 为 false。358 行登记寿命配置，381 行调用 `SpaceCutEffect.declareBalance()`。
- 宿主组：`ReleaseRegistry.java` 12 行，`HOST_GROUP` 只有 `space_cut`。`submitsHostGroup` 因此为 true。
- 平衡：`SpaceCutEffect.java` 32–36 行声明 `boxDepth` 默认 3、`boxWidth` 默认 5。`BalanceCatalog.java` 35 行把 `space_cut` 放进 `DAMAGE_HOSTS`。
- 玩家触发：`SkillUsePacket.java` 520–522 行把攻击段交给 `SkillSegmentRunner.buildSegmentRunner`。`SkillSegmentRunner.java` 148–150 行进入 `commitLooseHost`；803–818 行确认宿主组后，剑术类型先 `swing(MAIN_HAND)`，再 `commitHostGroup`。
- 组内：`SkillSegmentRunner.java` 388–403 行按复制份数循环 `runSegment`。`SpaceCutEffect.java` 41–107 行生成静止斩面，70–72 行 `registerAction(SPACE_CUT_ID)`。
- 动作只发一次：`HostGroupContext.java` 98–102 行，同组动作先到先得。`HostGroupCommit.java` 77–85 行在提交成功后 `ActionRuntime.notifyAttackGroup`。`ActionRuntime.java` 42–52 行以速度 1.0、不循环打开会话，266–271 行用 `ActionSyncS2CPacket` 发给本人和同维度 128 格内的观察者。包注释写明该包不驱动伤害。

## 2. 时序、特效和现有动作设施

玩法时钟与 1.6 秒示意是分开的。

- `ReleaseDefinition.chargeTicks()` 默认 0。空间切割的匿名定义没有覆盖它。工作簿 `AQ` 也是 0。没有蓄力门。
- `SpaceCutEffect` 不写 `hitReadyAge`。`TechniqueHitSpec.java` 83 行默认 0，含义是激活后就能打到。实体在 `releaseActivation` 之后才进入命中 tick，下一拍 `age` 增到 1 即可结算。没有 0.48 秒伤害延迟。
- 命中体寿命来自定义，默认 10 tick，配置键 `lifetimeTicks`。冷却是术式上的 20 tick。
- 斩击特效在效果执行时挂上：贴图 `xueximod:textures/entity/xueximod/dimension_willow_v8.png`，`StillSheetVisual.once(..., 20, 1)`，即 20 帧、每帧 1 tick。资源文件在 `src/main/resources/assets/xueximod/textures/entity/xueximod/dimension_willow_v8.png`。`TechniqueHitEntity.releaseActivation` 给静止斩面盖出生时间；`StillSheetPlayback` 在客户端按实体快照画，实体提前移除后快照继续播完。`TechniqueHitRenderer` 对已启用的静止斩面直接返回，不走通用命中体模型。
- 现成玩家动作是 `OriginalHumanoid.registerSpaceCut`，86–117 行。时长 1.0 秒，只带动 `Root`、`Body`、`Head`、`ArmRight`、`ArmLeft`，通道只有旋转，混合 0.06 秒。0.62 秒的 `draw-release` 只进视觉通知，没有伤害消费者。骨架在 243–257 行，每侧一条手臂，没有前臂、手或刀骨。
- 重定向 `RetargetProfile.originalHumanoid` 把 `ArmRight` / `ArmLeft` 收成一条臂旋转，手骨和武器骨都是空。`ThirdPersonPoseWriter.applyUpperBody` 写原版 `PlayerModel` 的 `body`、`head`、`rightArm`、`leftArm`，腿保持原版。`UPPER_BODY_SLASH` 不使用挥斩对齐矩阵。
- 已发布皮肤网格按 20 根授权骨核对。`SkinSession.chooseThird` 在骨架不一致时改走刚性 `PlayerModel`。第一人称 `FirstPersonSkinDraw` 失败后，`ItemInHandRendererActionMixin` 仍画原版单节手；接管成功时把挥砍进度清零，再乘一个手臂四元数。
- 持物是主手或副手的 `SwordItem`，画在这一节手臂上。v5 的 `SwordHand` 没有运行时骨骼。

## 3. 要改的文件，以及和分身所有权的关系

只替换现有五骨剪辑时，改 `OriginalHumanoid.java` 即可。动作 ID 保持 `xueximod:space_cut` 时，`SpaceCutEffect` 的登记和柳叶斩贴图可以不动。这一改法表达不了前臂、刀轴和躯干位移。

要让确认版的双节臂和刀骨按原层级出现，现有单节 `PlayerModel` 没有对应部件。需要动的是自有动画与客户端姿态文件：`OriginalHumanoid.java`、`RetargetProfile.java`、`RetargetedPose.java`、`RigidPoseRetarget.java`、`ThirdPersonPoseWriter.java`，以及第一人称持物路径 `ActionRenderQueries.java`、`FirstPersonHandBasis.java`、`ItemInHandRendererActionMixin.java`。这些文件不在分身合同的所有权清单里，正式源码里也各只有一份。

分身 A/B/C 正在写 `docs/开发记录/分身底座-20261102/候选源码/`。正式目录 `src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/` 还不存在。合同里 C 独占、候选目录里已有副本、本项不能抢写的正式挂接文件包括：`XueXiMod.java`、`Technique.java`、`TechniqueManager.java`、`ModTechniques.java`、`BalanceCatalog.java`、`ReleaseDefinitions.java`、`ShuCommands.java`、`PlayerConnectionHandler.java`、刻印与装配菜单、`SkillAssemblyPacket.java`、`SkillUsePacket.java`、`StaffSkills.java`、`PerfMetric.java`、`PerfCounter.java`、`PlayerShuData.java`、两份语言文件。A 的 `clone/core/`、B 的 `clone/action/` 与 `CloneBalance.java`，以及 C 的 `CloneRenderer.java` 等新文件同样不写。

伤害、范围、规则链和配置键不需要改上述释放文件。

## 4. 现有接口能覆盖的部分，以及必须由你选的两点

同一条 `ActionSyncS2CPacket` 覆盖本人和 128 格内的其他玩家。第三人称由 `PlayerModelActionMixin` 写姿态；观察者收到同一动作 ID 后走同一套刚性写入。护甲跟随原版单节手臂。连续释放共用每个玩家一个会话，后一次 `replace` 前一次姿态。播放期间没有额外的移动锁或施法锁。

一分为三保持现有次数和可见性。`commitLooseHost` 在 `copyIndex > 0` 时直接交回，不另开宿主组。组内仍按复制份数各跑一次 `runSegment`，每份各生成命中体。`separatesCohortVisuals` 为 true。没有落点指定、没有三维交错时，各份几何叠在同一锚点，`CohortLaneLayout` 按份把斩面分成独立槽。有落点指定时，`CastFrame.slashGeometry` 按复制号抖动斩击原点。玩家挥刀动作每组只登记一次。

第一人称只有主手物品是剑，并且目录认定该手时，才会接管。`ActionVisualCatalog` 176 行把 `space_cut` 标成右手、非左手、`UPPER_BODY_SLASH`，没有三点采样，因此没有斩击平面。角色右臂对应 `model.rightArm`。左手主手时，剑在左手，第一人称主手不会被这个动作接管，仍走原版挥手；第三人称仍播放写在右臂上的剪辑。

必须二选一的只有这两处：

1. 视觉时长。现有剪辑是 1.0 秒。确认版采样是 1.6 秒。把它写入 `ActionDefinition.durationSeconds` 只改变姿态播放时钟。蓄力 0、命中就绪 0、寿命 10 tick、冷却 20 tick 保持原值。1.6 秒大于 20 tick 冷却时，下一次释放会换掉尚未播完的姿态。
2. 双节与刀。现有接口只能把一条旋转写进原版单节手臂，主手剑跟着那一节走。v5 的上臂、前臂各 6 单位，刀在 `ForearmRight/SwordHand`，躯干和腿还有位移。要原样出现，需要新增双节与刀骨的渲染部件。这一步不改伤害和规则。

## 5. 工作簿

`docs/术式制作表.xlsx` 当前 SHA256 是 `06a616fbf4484b38d101be875d6f6d0293676052f0f65747e74893ad79c3b179`，110918 字节。

四张状态页里，只有「已完成」第 6 行命中。待制作、制作中、待验收都没有这一行。该行保留在已完成，没有迁移。

- B：`space_cut`
- N（功能描述）：在落点留下一片静止的切割
- O（特效）：`SpaceCutEffect`
- DV（备注）：空

表头没有「依赖」或「前置」列。同行的「关联说明」「迁移来源」也是空的。同行已填的结构格是：属性无，基础伤害 4，基础冷却 20，寿命 10，飞行速度 0，蓄力 0，必须手持剑为否，弹道可修改为否，同锚点分道显示为是。表上「整组提交」为否；运行时 `ReleaseRegistry.HOST_GROUP` 只含 `space_cut`，实际会走宿主组。

## 6. Gradle 与需要等待的写入

当前没有 `runClient`，也没有 `GradleWrapperMain` 或 Gradle worker。有一个 Gradle 8.8 daemon，pid 66116，父进程是已打开 `D:\alianximod` 的 IntelliJ IDEA（pid 57868）。`gradle.properties` 仍是 `org.gradle.jvmargs=-Xmx3G` 和 `org.gradle.daemon=false`。这次没有编译。

本项若只改动画与客户端姿态文件，不必等分身候选目录收束。若实现要碰到上一节列出的 C 独占正式文件，就要等候选目录写入结束并完成正式挂接之后再动。分身正式 `clone` 包尚未进入 `src`。
