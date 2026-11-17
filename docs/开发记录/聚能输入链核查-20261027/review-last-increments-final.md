我先完整读取这份增量复审任务，再只按文件里的范围核对。两个补丁分开核对：先读菜单和边界报告，再只对照对应的改前文件和当前实现。PASS。两个补丁都落在裁决范围内。菜单没有打开过，这次也没有进游戏，所以不能把界面记成实测通过。

剑技会话在构造时把 `staff` 固定下来。`staff == false` 时，`rejectReason` 遇到 `TechniqueType.MAGIC` 直接返回「无法装配：法术不能装入剑技槽」，`writeGear` 直接跳过，两处都不再读当前主手的 `allowsMagic`。齿轮格的 `mayPlace` 和 `set` 仍走 `rejectReason`。菜单开着时把主手换成法杖，`staff` 仍是 false，聚能进不了剑技槽，也不会被写成 `SkillType.SWORD`。`staff == true` 的法杖分支仍只拒绝剑术并检查槽位；UNIVERSAL、MOVEMENT 的剑技容量路径没有改。

`chainEnergyBefore` 只把「非附魔、定义非空、`HIT_VOLUME`、`role == ATTACK`」当作释放边界。同一组 `sharesDualRelease` 为真则 `continue`，否则 `break`。未注册定义只跳过，不截断。聚能是 `RULE` 加攻击职责，动作宿主是 `ACTION`，都不会被当成边界。资格仍由 `consumesElementMagic` 单独判断，所以无属性法术和剑术自己的倍率是 1，同时挡住后面的元素法术。`findTripleAttack` 和索敌扫描仍用原来的条件，份数和附魔没有改。

`RouteReleaseInputCheck` 调用生产的 `frozenChainInput` 和 `CompiledReleaseStep.compile`。水环、雾雨用的是 `WaterReleaseDefinition.bodyGuard` / `domain`，伤害为 0，种类是 `HIT_VOLUME`，职责是 `ATTACK`，不是把伤害改成正数。已有日志 `check-route-boundary.log` 为 68 条断言、0 失败，和生产语义一致：水环后雾雨是 2/1，正伤后雾雨是 2/1，无属性和剑术挡住后续，两段聚能分别是 2/2，穿过动作宿主是 4，双重组内共享 2、组外是 1。这次没有重跑该测试。