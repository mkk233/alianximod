# 弹道规则单独预览实现回执

- batchId: `待制作-20261031`（历史标识，未重置额度）
- taskId: `trajectory_rule_standalone_preview`
- attemptId: `20261114-impl-trajectory-preview-01`
- stage: `implementation`
- 执行者: Grok 4.6（原生作者）
- 直接父代理: 本次调用的主会话（未提供 UUID，不编造）
- 嵌套只读: 未派。本任务相关入口已由作者定向读取。
- 复审: 设计 2/2、实现 3/3 均已用完。本阶段额度已用完，返修结果未追加复审。下文只是定向静态核对，不是正式复审结论。
- 编译: 本单无编译权限，未跑 `gradlew` / `build` / `jar` / `runClient` / 测试，也未启动游戏。待统一编译。
- 游戏实测: 未做。

## 改动

- `src/main/java/com/bslx/XueXiMod/shu/technique/release/ChainRole.java`
  - `modifiesFollowingTrajectory()` 只包含 `TRAJECTORY_UPWELL`、`TRAJECTORY_DROP`、`TRAJECTORY_PARALLEL`、`TRAJECTORY_TRACKING`、`TRAJECTORY_SCATTER`。
  - 不包含 `RULE_STAGGER`、`RULE_HIT_CONTINUE`、其他普通规则、索敌、攻击和 `TRAJECTORY_MANIPULATION`。
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/TrajectoryRulePreview.java`（新）
  - 单独组判定、现有报价/冷却和有限粒子预览。
- `src/main/java/com/bslx/XueXiMod/network/SkillUsePacket.java`
  - 协同路由之后、批量编译和普通链拆段之前接入。
  - 不是单独弹道规则时返回空，缺宿主拒绝仍在后面的原路径。

## 单独组口径

判定的是本次实际选择并传入 `executeSkill` 的完整 `Skill`，不是拆段、过滤攻击/附魔/索敌之后的子段，也不看其他未选槽位。

有效实际装配项恰好一个，且该项已登记、不是已移除规则、角色 `modifiesFollowingTrajectory()` 为真，才算单独。空项、未知术式、两份相同规则、规则加任何其他有效项，都不算单独，也不被静默滤掉。

现有六个登记都落在这五个角色上，没有按术式编号、名称前缀、`ReleaseKind.RULE` 或 `trajectoryMutable` 白名单：

- `fire_upwell` → `TRAJECTORY_UPWELL`
- `trajectory_drop` → `TRAJECTORY_DROP`，弹道 `DROP`
- `trajectory_storm` → `TRAJECTORY_DROP`，弹道 `VORTEX`
- `trajectory_parallel` → `TRAJECTORY_PARALLEL`
- `trajectory_tracking` → `TRAJECTORY_TRACKING`，弹道 `STRAIGHT`
- `trajectory_3d_interleave` → `TRAJECTORY_SCATTER`，弹道 `STRAIGHT`

## 预览路径

起点是服务端当时眼睛位置，方向是当时视线。没有目标、锁定发光、命中体、伤害、火种、附魔、命中接续、领域事件或方块变更。每条预览按年龄排进现有 `ChainScheduler`，玩家离开或换世界后剩余帧不再发粒子。寿命最长 100 刻。

- 喷涌: `MotionProfile.approvedUpwell()` 的速度、重力、锥角、时长和水平上限；单份初速用 `TrajectorySampler.upwellVelocity`；点用 `sampleBallistic`。粒子 `FLAME`。
- 垂落: 眼睛上方 8 格，这是现有垂落抬升；速度取登记速度，非正时用术式弹速，仍非正时用 1；点用 `TrajectorySampler.sample(DROP)`，该公式里的 `DROP_LEADING * DROP_FACTOR`。粒子 `END_ROD`。
- 风暴: 角色仍是垂落，但登记弹道是 `VORTEX`，所以走 `TrajectorySampler.sample(VORTEX)`，不画成垂直落下。起点是眼睛。登记寿命是 1 刻，公式会立刻停在落点；占位因螺旋下限 8 刻再多留 4 刻，共 12 刻，仍受 100 刻上限。粒子 `CLOUD`。
- 平行: 视线的水平投影，再走直线采样。速度同上；登记寿命 1 刻时预览 20 刻。粒子 `END_ROD`。
- 追踪: 没有目标时沿当前视线直线采样，不查找、不标记、不发光。速度和寿命口径同平行。粒子 `END_ROD`。
- 三维交错: 起点在视线水平面内、1.5 格半径随机偏移，方向是随机三维单位向量，然后直线采样 20 刻。不画成索敌标记。粒子 `CRIT`。

费用用 `SkillSegmentMath.quotedMana`，再交 `MagicPool.trySpend`。法力不足时不播放，沿用现有不足提示。冷却用 `BalanceDisplay.cooldown` 加 `SkillSegmentMath.scaleCooldown`，再写入现有释放冷却。权限、持有和装配核对仍在这次分支之前的原入口完成。

## 组合与例外

组合不进入预览。后续仍走原批量编译或原链：攻击照常生成，弹道规则继续改后续弹道，不再单独播放自身预览。

`RULE_STAGGER`、`RULE_HIT_CONTINUE`、其他纯规则、缺宿主、未知装配仍被原路径拒绝。本次没有放开全部 `missing_parent` 或纯规则链。地表定位和索敌锁定没有加隐藏门控，也没有改它们的现有视觉。

## 美术

功能已接入，美术占位未完成。占位只用原版 `FLAME`、`END_ROD`、`CLOUD`、`CRIT`，没有新贴图、没有绘画或建模，也没有复制未确认的喷涌示意素材。正式预览美术仍待单独确认。

## 静态核对

六项都能由角色加登记弹道进入对应采样。风暴不会因为角色是垂落就画成垂落；追踪没有目标时不会画成索敌标记。

名称门禁对全部 `src` 返回非零，报告的是既有短名字和缩写。过滤后，本次三个文件没有命中。未改既有违例。

## 源码指纹

指纹在本文件写入前计算，不包含本回执：

- `ChainRole.java` SHA256 `15186d387014ca5aba074654e29e80d0020a1dd5b318a12f4d55c53f52c04cc6`
- `TrajectoryRulePreview.java` SHA256 `3d99fca7de38358ee8eb3bd32a677c8ac5c009730008fe9a57173e93b62a0db4`
- `SkillUsePacket.java` SHA256 `53a8ecf45fc3def113f05d56de6d5bb5b648a2c0aa175061a7bd0d7c0c70535d`
