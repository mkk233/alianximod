# B 实现最终报告

- 作者：B
- 模型：grok-4.7-high-fast
- 阶段：实现。设计 2/2 已用尽，不追加。实现复审 0/3，本单不复审。
- released：true
- 编译：未跑。待统一编译。
- 命名门：未跑。Shell 被拒绝。
- 测试、打包、游戏、提交、制作表：未做。

## 写入

新增 `src/main/java/com/bslx/XueXiMod/shu/technique/release/aim/LaunchAim.java`。

改过：

- `release/CastFrame.java`：`execution`、`launchAim`。`volumePoint` 在垂落抬高之后，自动来源再 `volume().clampPoint`。
- `release/entity/TechniqueHitSpec.java`：`sourceOrigin`、`sourceDirection`、`localTargetId`、`automaticLifetime`、`launchAim`。每个真正生成前查 `execution.token`。二次取样和发射点偏移后再钳。跟随清空起点之前先记住源点。
- `release/entity/TechniqueHitEntity.java`：实体持有完整 `LaunchAim`。自动持续不把 `releaseSlot < 0`、松键或换手当成结束。自然寿命仍在。运动拍内把局部瞄准交给 `TrackingSupport`，拍末清掉。
- `release/motion/ProjectileMotion.java`：锁定且自动时只 `findTarget`，不读玩家标记。没有目标就沿本轮方向。
- `release/motion/FollowCasterMotion.java`：自动跟随钉在该次源点；目标有效就转向，失效留最后合法方向。垂落仍向下。手动仍跟眼睛和准星。
- `release/motion/TrackingSupport.java`：保留 `LivingCandidateQueries` 近距离查询。只有当前拍绑了自动瞄准时，才只重验局部目标，不另搜 5 格。
- `effects/SpaceCutEffect.java`：自动路径不调用 `resolveAnchor`，锚点取帧上显式点再钳制。手动仍走准星锚点。
- `effects/LiaoliEffect.java`：出手时用 `HeldSwordGate` 看当前主手。自动且令牌无效则不生成。刺击轴在没有前向时，自动来源用发射方向。
- `effects/PhantomSwordEffect.java`：`SkillCarrier.acceptsSwordSkill` 同时检查本轮武器副本和当前主手，再加 `HeldSwordGate`。伤害仍用传入副本。额外竖向偏移后再钳制。每把剑生成前查令牌。
- `sword/SwordEntity.java`：悬停用局部目标，起飞后不再改 `flyDirection`。万剑归宗在令牌无效时不进入风暴。控制锚点是钳过的显式点，不把既存剑瞬移到采样点上。
- `sword/SwordVortex.java`：自动发射用局部目标或备用方向，不读玩家标记和准星。轨道中心用控制锚点。追踪只重验局部目标。

`StillAimMotion.java` 未改。自动静止斩在运动前已经写入非空 `aim` 和 `anchor`。

未创建 bind 占位类型。`ReleaseExecutionContext`、`ReleaseInputMode`、`TargetCheck`、`LifetimeToken` 按契约引用，包当前还不在工作区。

## 实时输入替换

- 燎里：自动 `automaticLifetime` 为真时，`releaseStopped` 直接返回假，因此 `releaseSlot == -1`、松键、换手和施法者倒下都不提前结束。寿命仍按年龄。跟随原点是该次 `LaunchAim.origin`，方向朝重验后的局部目标，失效用最后合法方向。
- 幻剑：悬停每拍 `findTarget`。有目标朝目标中心并记住方向；没有就用备用方向，之后保留已瞄准方向。满 10 拍仍切到飞行，方向不再跟准星。资格看当前主手，数值用本轮副本。
- 万剑归宗：不新造剑。没有既存剑仍由 `SwordReturnAllEffect` 空放。令牌失效时 `beginVortex` 在改状态之前返回。轨道中心是显式锚点。发射方向是局部目标，否则备用方向。飞行中的追踪不跨域重选。
- 空间切割：自动分支不读准星。
- 直线锁定：自动时目标点只来自 `findTarget`。空目标沿已写方向。
- 追踪：自动拍内 `resolveTarget` 不再做 5 格查询。`TrackingMotion` 未改；自动且带追踪时实体 `lockThisCast` 被清掉，避免它去读玩家标记。转向仍是原来的有界转角。
- 垂落：方向保持向下。带体积的二次 `volumePoint` 在抬高 8 格之后钳回领域。垂丝唇口不在这里钳。
- 斩击和函数轨迹：不改成飞弹或垂落。生成点钳制，后续飞行和帘口抬升不硬钳。
- 无局部目标：不取消生成，沿本轮方向放。

手动路径：`execution` 为空或模式不是 `DOMAIN_AUTOMATIC` 时，准星、玩家标记、松手、换手和幻剑眼后布点保持原样。

## 备份

Shell 的复制和哈希命令被拒绝，没有换一种命令再跑。SHA-256 和 mtime 都是 null。

改前全文已写入 `backup-B` 的有：`ProjectileMotion`、`FollowCasterMotion`、`TrackingSupport`、`SpaceCutEffect`、`PhantomSwordEffect`。

`CastFrame`、`TechniqueHitSpec`、`TechniqueHitEntity`、`LiaoliEffect`、`SwordEntity`、`SwordVortex` 在改之前读过全文，但复制命令被拒绝后没有再落成独立备份文件。这六份的改前字节备份缺失。

## 待接点

- A 的 `bind` 包和 `ReleaseExecutionContext.automatic` 尚未出现在源码树。B 只按契约调用，没有建占位。统一编译要等 A 写入这些类型。
- A 必须在调用效果前写好 `frame.execution`，以及 `overrideGeometry`、`origin`、`look`、`anchor`。B 不写这些赋值。
- `TrackingMotion` 仍有直线锁定时读 `LockTargetService` 的代码。B 对自动追踪关掉 `lockThisCast`，并用 `TrackingSupport` 的当前拍绑定替换选敌。文件本身不在本次写权限。
- `DropMotion` 仍会在 `lockThisCast` 时读玩家标记。自动垂落会关掉这个标志；采样本身也不使用目标点。文件不在本次写权限。
- `WeepingCurtainEffect` 在没有覆盖几何时仍会读准星。自动且未覆盖时，生成器会改用执行上下文的源点再钳制，效果里的那次读取不会成为实体起点。唇口抬升不钳。文件不在本次写权限。
- `SwordReturnAllEffect` 在剑列表为空时已经直接返回，不会伪造剑。令牌失效时 `beginVortex` 静默返回，该效果没有把失败交回提交结果。文件不在本次写权限。
- 剑实体读档后没有活的 `TargetCheck`，只保留方向，不再按 UUID 追踪。命中体 `shouldBeSaved` 为假，不持久化这份瞄准。
- 主人死亡时，万剑归宗原有的主人检查仍会结束风暴剑。这不是领域取消令牌。领域到期而主人仍在时，已进入风暴的剑不会被拆掉。

## 未验证

未编译，未跑命名门，未启动游戏，未打包。待统一编译。
