我先读取任务说明和相关执行规范，确认授权范围后再动手。任务授权是实现 B：自动发射源与攻击实体适配。我先读正式接口和相关设计记录，再按独占文件动手。契约已明确：B 负责显式发射源和实体适配。我先定位独占源码和 A/C 已落地的接口，再备份后改。改代码前先把拟改文件的完整原文备份到本任务目录。接着再接入显式发射源。Shell 备份被拒绝，我改用原生写入保存改前全文。先补齐大文件内容，再落备份。复制命令被拒绝了。我改用原生写入保存改前全文，并先加上 `LaunchAim`。B 的实现已写入并交回。`released` 为 true。编译未跑，待统一编译。

## 写入

新增 `release/aim/LaunchAim.java`。自动构造必须带上 `TargetCheck`；五参数重载不能用于自动来源。`findTarget` 只重验冻结的那个目标，世界不一致、死亡、出界或关系不符都返回空。不重新选目标，不读玩家标记，也不保存领域取消令牌。

改过这些文件：

- `CastFrame`：增加 `execution` 和 `launchAim`。`volumePoint` 在垂落抬高之后，自动来源再钳回领域真实范围。
- `TechniqueHitSpec`：增加 `sourceOrigin`、`sourceDirection`、`localTargetId`、`automaticLifetime`、`launchAim`。每个真正生成前检查 `execution.token`。二次取样和发射点偏移后再钳。
- `TechniqueHitEntity`：实体持有完整 `LaunchAim`。自动持续不再把槽位 `-1`、松键或换手当成结束。自然寿命仍在。
- `ProjectileMotion`：自动锁定只用局部目标。没有目标就沿本轮方向。
- `FollowCasterMotion`：自动跟随钉在该次源点。目标有效就转向，失效留最后合法方向。垂落仍向下。
- `TrackingSupport`：保留原有近距离查询。只有当前拍绑了自动瞄准时，才只重验局部目标。
- `SpaceCutEffect`：自动路径不调用准星锚点。
- `LiaoliEffect`：出手时用共享持剑判断看当前主手。
- `PhantomSwordEffect`：当前主手和本轮武器副本都要过剑技资格，伤害仍用副本。竖向偏移后再钳制。
- `SwordEntity`、`SwordVortex`：幻剑悬停用局部目标，起飞后冻结方向。万剑归宗继续操纵既存剑，轨道中心用显式锚点，不把剑瞬移到采样点，也没有剑时不伪造。

`StillAimMotion` 未改。自动静止斩在运动前已经写上非空瞄准和锚点。

没有创建 bind 占位类型。契约里的执行上下文目前还不在源码树里，统一编译要等 A 写入。

## 手动路径

`execution` 为空，或模式不是领域自动时，准星、玩家标记、松手、换手和幻剑眼后布点保持原样。

## 备份与验证

复制和命名门的 Shell 都被拒绝，没有换命令重跑。哈希和时间都是 null。

改前全文已备份的有：`ProjectileMotion`、`FollowCasterMotion`、`TrackingSupport`、`SpaceCutEffect`、`PhantomSwordEffect`。

`CastFrame`、`TechniqueHitSpec`、`TechniqueHitEntity`、`LiaoliEffect`、`SwordEntity`、`SwordVortex` 改前读过全文，但没有落成独立备份文件。

未编译，未跑命名门，未启动游戏，未打包，未改制作表。

## 待接点

- A 必须在调用效果前写好 `frame.execution`，以及覆盖几何、原点、朝向和锚点。
- `TrackingMotion` 未改。自动追踪会关掉实体上的玩家锁定标志，选敌改走当前拍的局部目标。
- `DropMotion` 未改。自动垂落会关掉该标志，采样本身也不使用目标点。
- `WeepingCurtainEffect` 未改。没有覆盖几何时它仍会读准星；自动生成会改用执行上下文的源点，唇口抬升不钳。
- `SwordReturnAllEffect` 未改。没有剑时它已经直接返回。令牌失效时风暴入口静默返回，这个失败没有交回提交结果。
- 剑读档后没有活的目标检查，只保留方向。命中体不存档。主人死亡时，万剑归宗原有的主人检查仍会结束风暴剑；领域到期而主人还在时，已经进入风暴的剑不会被拆掉。

完整条目在 `docs/开发记录/领域绑定规则-20261030/implementation-B-final.md` 和 `evidence-B.json`。
