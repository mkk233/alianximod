先读这次授权的任务说明，再按其中的范围处理吸收量窗口。裁决要求把偏移 56 那次真实减少的吸收算进异常提交。我先核对两个 `actuallyHurt` 里吸收写入的位置和前后值。偏移 56 的 `setAbsorptionAmount` 在 `setHealth` 之前返回。接下来只在这次写入的前后值上判断是否真实减少，并保留原来的扣血信号。吸收实际减少已经纳入同一跟踪帧。`setHealth` 返回后的提交信号还在。

确认点是匹配本次目标与来源的那一帧里，`setAbsorptionAmount` 返回后吸收值比该次调用前更小。等值、变大或调用没返回，都不记。生物 `LivingEntity.actuallyHurt` 有两处 `LivingEntity.setAbsorptionAmount`：偏移 56 在 `onLivingDamage` 和 `setHealth`（147）之前；偏移 157 在 `setHealth` 返回之后。玩家 `Player.actuallyHurt` 不走生物版，只有偏移 56 的 `Player.setAbsorptionAmount`，位于 `onLivingDamage`（62）和 `setHealth`（133）之前。生物吸收是字段写入后立刻返回；玩家经 `SynchedEntityData.set` 后返回，返回时值已经在。比较只包这一次调用，不取整段 `hurt` 的差值。

四分支都是对照本地字节码的静态结果，没有执行：

- 吸收已减少且该次写入已返回后抛出：提交一次。
- 吸收未减少（含零、等值、变大、写入未返回）且生命尚未提交就抛出：退款一次。
- `setHealth` 已返回后抛出：提交一次。生命与吸收同时成立也只 `retainShare` 一次。
- 正常返回且最终零伤：仍只看 `hurtConfirmed`，为假则退款。吸收标记不改变这条路径。

异常继续原样抛出，不发成功载荷，不补生命、不回填吸收。`RuntimeException` 与 `Error` 保持原类型。

改动文件与末次写入：`TechniqueHits.java` 23:11:01，`DomainContactBilling.java` 23:10:23（异常分支 108 行），`LivingDomainDamageCommitMixin.java` 23:10:25（吸收注入 53–77 行），`PlayerDomainDamageCommitMixin.java` 23:10:27（31–55 行）。`actuallyHurt` 仍只调用一次。未改 `xueximod.mixins.json`。基线在 `contact-evidence/repair-absorption/before/`，依据在同目录 `bytecode-absorption.txt`。

`name-gate` 退出码 1，只有未改的 `OriginalHumanoid.java:165 i` 和 `:202 to`。未编译、未跑测试、未启动游戏。不是新的复审通过。

写入已收束，待统一编译。
