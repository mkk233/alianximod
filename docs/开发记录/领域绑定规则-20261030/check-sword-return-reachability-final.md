先读这份可达性核查的任务说明，再按它划定的范围核对源码并给出完整报告。撤回上一份报告里的那条反例。`SwordComeEffect` 到 `startReturning` 当前不能进入合法的 `BoundReleasePlan`，不能算这次自动绑定的实际漏洞。

剑来只有一条注册。术式 id 是 `sword_come`，`Technique.baseDamage` 为 `0f`（`ModTechniques.java` 第 159–161 行）。释放定义 id 同为 `sword_come`，`kind` 为 `HIT_VOLUME`，`role` 为 `ATTACK`，`trajectoryMutable` 为 false，`manipulatesSwordEntities` 为 true，`refusesMixedChain` 为 true（`ReleaseDefinitions.java` 第 36 行，对应第 66–74 行那条重载）。效果只在 `ModTechniques.java` 第 304 行登记到这个 id。`startReturning` 的唯一调用点是 `SwordComeEffect.java` 第 22 行，定义在 `SwordEntity.java` 第 260 行。没有别的术式复用 `SwordComeEffect` 或 `startReturning`。

捕获谓词是 `BindingStructureCompiler.qualifiedAttack`（第 245–256 行）。第 246 行要求 `getBaseDamage() > 0f`，剑来在这里直接失败。非双重在第 121–123 行、双重在第 147–150 行都会因此产生结构错误，不会写入成员。成员只来自已通过该谓词的 `hostIndexes`（第 173–178 行）。它排在合格攻击后面时，捕获区间在合格攻击处结束，也不会被收进冻结子链。

万剑归宗是另一条。id 为 `sword_return_all`，`baseDamage` 为 `8f`（`ModTechniques.java` 第 163–165 行）。释放定义同样是 `HIT_VOLUME`、`ATTACK`，`manipulatesSwordEntities` 为 true（`ReleaseDefinitions.java` 第 37 行）。正伤且操纵剑实体的是它，不是剑来。它走 `SwordReturnAllEffect` 的 `beginVortex`，不走 `startReturning`。
