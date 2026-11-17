先读返修任务说明，再只按它划定的范围改。问题 1 要分开两件事：能不能接触看原始加数，打出去的金额仍用已经配置的值；合法的 0 贡献按「成立但无正伤」处理。先保存这次修改前的字节。返修 1 已按问题 1 收口。没有另作架构或正式复审，也没有把这一单计成新的复核轮次。未编译、未启动游戏、未提交。写入已收束，所有权已释放。

## 0 贡献怎么走

接触资格改看原始加数。`ContactCast.refusal` 的显式接触宿主和普通接触宿主都改为 `EnchantmentSnapshot.rawDamageOf(skill, indexes) <= 0` 才拒绝。幽冥领域例外仍在。倍率收成 0、但原始加数大于 0 时，这次释放不再在扣费前被取消。

`snapshot` 仍用 `EnchantmentSnapshot.from` 的结果。`contactDamage` 保持已配置金额，可以是 0。条数、类型和幽冥状态留在这份快照里。只有原始加数也不是正数、并且不是带类型的幽冥例外时，才返回空。实际伤害仍是已配置的 `contactDamage`，没有改回原始合计。

`TechniqueHits.hurt` 和经它实现的 `hurtRoot`：金额小于等于 0 时不再和空目标揉在一起直接失败，也不进入原版 `hurt`，不改无敌帧。`DamagePermission.allows` 不通过（空目标、死亡、旁观、自己、无敌、原版不允许伤害的玩家）返回失败。通过则与 `classifyRespecting` 相同，记为成立但无正伤。正数金额仍走原来的无敌帧旁路或原版无敌帧。

`classifyRespecting` 在金额小于等于 0 时调用同一判断。原先只要目标还活着就会记成成立；现在无敌或权限不通过记为无效，不再把受保护目标当成成立。

这次没有新增公共接口。资格判断使用 C 约定的 `public static float EnchantmentSnapshot.rawDamageOf(Skill skill, List<Integer> indexes)`。没有改 C 的文件。

## 修改位置

- `network/ContactCast.java`：拒绝在第 36–47 行，快照资格在第 112–118 行。
- `shu/technique/release/TechniqueHits.java`：`hurt` 第 52–57 行，`classifyRespecting` 第 99–100 行，`classifyNonPositive` 第 148–153 行。

修前字节在 `docs/开发记录/配置平衡-20261101/pre-repair-1/A/`，没有覆盖原来的 `pre-A`。`author-A-manifest.json` 已记下这次返修。

## 仍要知道的调用方耦合

`hurt` / `hurtRoot` 对合法 0 贡献返回的布尔值是「可以接续」，不是「原版已经打出正伤」。这些不在本次可写范围里的调用方会把 `true` 当成正伤落地：`ContactEnchantState`、`SwordEntityMotion`、`HitVolumes`，以及 `TechniqueHitSettle` 和 `ReleaseBatch` 里不走 `classifyRespecting` 的分支。它们因此可能产种。`hurtConfirmed` 仍在金额小于等于 0 时返回 `false`，冲刷路径还是记成无正伤。

没有无法完成的阻塞。统一编译依赖 C 已经提供 `rawDamageOf`。

## 命名门禁

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 1。只有未改的 `OriginalHumanoid.java` 第 165 行 `i` 和第 202 行 `to`。本次文件没有新的违规。

待统一编译。游戏未测。
