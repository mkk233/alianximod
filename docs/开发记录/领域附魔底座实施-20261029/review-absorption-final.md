我先通读这份吸收值复核说明，确认只读范围后再核对它点名的调用顺序。裁决和返修终稿先对齐。接着只看这 4 个文件相对吸收返修基线的差异，以及前置吸收扣除的提交证据。终稿说吸收减少已经记进同一帧。我对照基线和现行 4 个文件，核对前后值、完成信号和异常时只提交一次。原 P2 已按静态证据补上。前置吸收确实减少并在该次写入返回之后，异常会提交一次；生命提交与吸收减少同时成立也只提交一次。未发现新的必修项。

证据在同一次跟踪帧里。`observeAbsorptionBefore`（`TechniqueHits` 178–186 行）把 `getAbsorptionAmount()` 记在当前帧；`observeAbsorptionAfter`（192–206 行）只在 `setAbsorptionAmount` 返回后比较，新值更小才把 `absorptionReduced` 设为真。等值、变大、尚未返回都不记。`matchedWatch`（208–221 行）仍要求栈顶帧、绑定帧、目标和来源一致，前后值放在这一帧上，不取整段 `hurt` 的差额。

生物 `actuallyHurt` 的两处 `LivingEntity.setAbsorptionAmount`（偏移 56 与 157）由 mixin 53–77 行以 `require = 2` 包住，56 在 `onLivingDamage` 和 `setHealth` 之前。玩家不走生物版，mixin 31–55 行以 `require = 1` 只包偏移 56 的 `Player.setAbsorptionAmount`，位于 `onLivingDamage`（62）和 `setHealth`（133）之前。生物吸收是字段写入后返回；玩家经 `SynchedEntityData.set` 返回时值已在。`setHealth` 返回后的 `noteHealthCommitted`（169–175 行）仍在。

`DomainContactBilling.apply` 107–116 行在异常里看 `healthApplied() || absorptionReduced()`，成立则 `retainShare` 一次并继续抛出原异常；否则 `finally` 退款一次。`deliverPayload` 只在正常确认之后。`Error` 与 `RuntimeException` 保持原类型。正常返回仍只看 `hurtConfirmed`：最终零伤、取消、无敌帧退款，吸收标记不参与。`actuallyHurt` 仍由原重定向调用一次。没有监视时吸收钩子直接返回。

`xueximod.mixins.json` 33–34 行仍注册这两个类，本次未改。注入目标与本地字节码一致，但是否在运行时接上尚未验证。未编译、未跑测试、未启动游戏。
